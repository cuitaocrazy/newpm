/**
 * 运行时守卫一致性检查（Issue #27 / #29）
 *
 * 规则：mapper 主 CRUD update 中**无 <if> 守卫**（无条件 `col = #{field}`）的字段，
 *       在编辑页上「什么都不改地保存一次」后，值必须原样保留。
 *       否则说明该字段没随表单提交，后端收到 null 无条件写回 —— 每次编辑都损坏这一列。
 *
 * 判据换过三次，前两次都被实验证伪，别再走回去：
 *
 *  ① 静态分析「字段名在 .vue 里出现过」（scripts/check-guard-consistency.mjs，未提交）
 *     → 保持 productionDate 缺陷不动，只加一行**被注释掉的**
 *       `// form.productionDate = data.productionDate`，检查器就从 FAIL 1 变成 PASS 45。
 *
 *  ② 运行时查「PUT payload 里有没有这个 key」
 *     → 编辑页若用 `form.value = response.data` 或 `Object.assign(form.value, data)` 整体回填，
 *       详情接口的所有 key 都会并进 form，断言**恒真**。实证：project/edit.vue 用 Object.assign，
 *       把 approvalTime / approverId（该页 0 匹配）的守卫去掉后旧判据仍全绿。
 *       10 个提交点里 8 个属于这种情况。且「key 在但值恒为 null」造成同样的 NULL 写入，它也免疫。
 *
 *  ③ 现判据「空保存后值不变」—— 直接观测伤害本身，与回填方式无关。
 *
 * 两个实现要点（都踩过）：
 *  - 必须 `route.continue()` 真写库，不能 `route.fulfill` 伪造 200，否则观测不到任何变化
 *  - 必须等 PUT 的**响应**回来再取 after 快照。只等请求被拦截会拿到旧值 →
 *    缺陷真实发生了测试却报绿（实测：注入 productionDate 缺陷后 2 passed，事后查库该字段确已为 NULL）
 *
 * 覆盖：9 条主 CRUD 语句 / 10 个提交点（ProjectMapper.updateProject 有两个提交点）。
 *
 * 运行前提：后端 + vite 均已启动。
 *   PROTO_BASE_URL=http://localhost:5174 npx playwright test guard-payload-runtime.spec.js
 */

import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

const BASE = process.env.PROTO_BASE_URL || 'http://localhost:5174';
const MAPPER_DIR = 'ruoyi-project/src/main/resources/mapper/project';

/**
 * 从 mapper XML 中解析指定 update 语句内**无 <if> 守卫**的字段。
 * 关键处理（均为实测踩过的坑）：
 *  - 先剥离 XML 注释：7 个 mapper 的注释里含字面量 <if>，裸正则会把注释当守卫
 *  - 按 \r?\n 切分：9 个 mapper 是 CRLF，按 \n 切会让字段名尾巴挂上 \r
 *  - <set> 与 <trim prefix="SET"> 两种包裹都要认
 */
function parseUnguardedFields(mapperFile, updateId) {
  const raw = fs.readFileSync(path.resolve(MAPPER_DIR, mapperFile), 'utf-8');
  const stripped = raw.replace(/<!--[\s\S]*?-->/g, '');
  const m = stripped.match(new RegExp(`<update id="${updateId}"[\\s\\S]*?</update>`));
  if (!m) throw new Error(`未找到 update 语句：${mapperFile} → ${updateId}`);

  const out = [];
  for (const line of m[0].split(/\r?\n/)) {
    const t = line.trim();
    if (!t || t.startsWith('<if') || t.startsWith('</if')) continue;
    // 形如  col_name = #{javaField},
    const hit = t.match(/^([a-z_]+)\s*=\s*#\{(\w+)\}\s*,?$/);
    if (hit) out.push({ column: hit[1], javaField: hit[2] });
  }
  return out;
}

/** 审计列由后端赋值，不经表单提交，不属本规则管辖 */
const AUDIT_FIELDS = new Set(['createBy', 'createTime', 'updateBy', 'updateTime', 'delFlag']);

test.describe.configure({ retries: 0 });

test.describe.serial('运行时守卫一致性 · 任务编辑页（原型）', () => {
  let token = null;
  let taskId = null;
  let unguarded = [];

  test('前置：解析 mapper 并取一条真实任务', async ({ request }) => {
    unguarded = parseUnguardedFields('TaskMapper.xml', 'updateTask')
      .filter(f => !AUDIT_FIELDS.has(f.javaField));
    console.log(`📋 TaskMapper.updateTask 无守卫字段（${unguarded.length}）：` +
      unguarded.map(f => f.javaField).join(', '));
    expect(unguarded.length, 'mapper 里应至少有一个无守卫字段，否则本用例无意义').toBeGreaterThan(0);

    const login = await request.post(`${BASE}/dev-api/login`, {
      data: { username: 'admin', password: '123456789' }
    });
    const body = await login.json();
    expect(body.code, `登录失败：${body.msg}`).toBe(200);
    token = body.token;

    // 注意：selectTaskList 查的是 pm_project LEFT JOIN pm_task，
    // 列表里混着「没有任务的项目」（taskId 为 null），必须筛出真有 taskId 的行
    const listRes = await request.get(`${BASE}/dev-api/project/task/list?pageNum=1&pageSize=50`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    const list = await listRes.json();
    const withTask = (list.rows || []).filter(r => r.taskId);
    if (withTask.length === 0) {
      console.log('⏭️ 库中无真实任务记录（列表全是无任务的项目），跳过');
      test.skip();
      return;
    }
    taskId = withTask[0].taskId;
    console.log(`📌 用于验证的任务 taskId=${taskId}（列表 ${list.rows.length} 行中有任务的 ${withTask.length} 行）`);
  });

  test('什么都不改地保存一次后，无守卫字段的值必须原样保留', async ({ page, context, request }) => {
    if (!taskId) { console.log('⏭️ 前置数据缺失'); test.skip(); return; }

    // 造值：把无守卫字段全部填上非空值。
    // 不做这一步的话，字段保存前就是 null 的部分**无法验证**（区分不了「本来就 null」
    // 与「被写成 null」）—— 实测这条任务上 5 个字段有 4 个是 null，只造值 1 个可验证。
    const seedRes = await request.get(`${BASE}/dev-api/project/task/${taskId}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    const seeded = { ...(await seedRes.json()).data };
    for (const f of unguarded) {
      if (seeded[f.javaField] === null || seeded[f.javaField] === undefined || seeded[f.javaField] === '') {
        seeded[f.javaField] = /Date$/i.test(f.javaField) ? '2026-06-15' : 12345.67;
      }
    }
    const seedPut = await request.put(`${BASE}/dev-api/project/task`, {
      headers: { Authorization: `Bearer ${token}` }, data: seeded
    });
    expect((await seedPut.json()).code, '造值失败').toBe(200);

    // 保存前快照 —— 判据是「值不变」，必须先拿到基线
    const beforeRes = await request.get(`${BASE}/dev-api/project/task/${taskId}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    const before = (await beforeRes.json()).data;
    console.log(`📸 保存前：` + unguarded.map(f =>
      `${f.javaField}=${JSON.stringify(before?.[f.javaField])}`).join(', '));

    await context.addCookies([{
      name: 'Admin-Token', value: token, url: BASE
    }]);

    // 拦截真实的 PUT，取实际 payload —— 这是本方案的核心：观测事实而非猜测代码
    let payload = null;
    await page.route('**/dev-api/project/task', async route => {
      if (route.request().method() === 'PUT') payload = route.request().postDataJSON();
      await route.continue();
    });

    // 路由定义是 path 参数 `/task/subproject/edit/:taskId(\d+)`（router/index.ts:295-305），
    // 不是 query。用 ?taskId= 只能匹配到父路由 Layout，子路由不匹配 → 编辑表单根本不渲染。
    await page.goto(`${BASE}/task/subproject/edit/${taskId}`, { waitUntil: 'networkidle' });
    await expect(page.getByRole('button', { name: '保存' })).toBeVisible({ timeout: 15000 });

    // ⚠️ 必须等 PUT 的**响应**回来，不能只等请求被拦截。
    // expect.poll(() => payload !== null) 只保证请求发出去了，此时后端可能还没落库，
    // 之后取的 after 快照会拿到旧值 → 缺陷真实发生了，测试却报绿（实测踩过：
    // 注入 productionDate 缺陷后 2 passed，而事后查库该字段确已被写成 NULL）。
    const putDone = page.waitForResponse(r =>
      r.request().method() === 'PUT' && new URL(r.url()).pathname === '/dev-api/project/task',
      { timeout: 15000 });
    await page.getByRole('button', { name: '保存' }).click();
    const putRes = await putDone;
    expect(putRes.status(), 'PUT 应返回 200').toBe(200);
    expect(payload, '未捕获到 PUT payload').not.toBeNull();

    console.log(`📦 实际 PUT payload 含 ${Object.keys(payload || {}).length} 个 key`);

    // 保存已真实写库（本 block 用 route.continue 放行），再取一次详情与 before 比对。
    // 判据是「值不变」而非「key 存在」—— 详见 assertUnchanged 的注释。
    const afterRes = await request.get(`${BASE}/dev-api/project/task/${taskId}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    const after = (await afterRes.json()).data;

    assertUnchanged(before, after, unguarded, {
      statement: 'TaskMapper.updateTask',
      editPage: 'views/project/subproject/edit.vue'
    });
  });
});

/* ═══════════════════════════════════════════════════════════════════════════
 * 其余 8 个模块（9 个提交点）。
 *
 * ProjectMapper.updateProject 有**两个提交点**（project/edit.vue 与
 * revenue/company/detail.vue），只测一个等于另一条路径免检。
 *
 * 全部 block 一律 route.continue 真写库 —— 判据是「空保存后值不变」，不落库就观测不到变化。
 * 「空保存」的副作用限于刷新 update_by / update_time，可接受。
 * ═══════════════════════════════════════════════════════════════════════════ */

/** JS 空值语义：只有 null / undefined / '' 算空（0、false、0.0 不算，否则会误杀金额为 0 的行） */
const nb = v => v !== null && v !== undefined && v !== '';

/** 登录取 token —— 供 Cookie 注入与列表取数共用 */
async function login(request) {
  const res = await request.post(`${BASE}/dev-api/login`, {
    data: { username: 'admin', password: '123456789' }
  });
  const body = await res.json();
  expect(body.code, `登录失败：${body.msg}`).toBe(200);
  return body.token;
}

/** 带鉴权拉列表，返回 rows */
async function fetchRows(request, token, url) {
  const res = await request.get(`${BASE}/dev-api/${url}`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  const body = await res.json();
  expect(body.code, `取数失败：${url} → ${body.msg}`).toBe(200);
  return body.rows || [];
}

/**
 * 拦截指定 pathname 上的 PUT，取真实 payload。
 *
 * 用**函数谓词做 pathname 全等比较**，不用 glob：
 *   - 'versionOut' 是 'versionOutManual' 的字符串前缀，glob 稍宽两个模块就互相抢请求；
 *   - 同 pathname 前缀下还有 GET 详情（/project/contract/339）、查重（checkProblemNo）等，
 *     宽 glob 会把它们一起拦下。
 * 非 PUT（同 URL 的 GET / POST 新增）一律 continue 放行。
 */
async function capturePut(page, pathname) {
  const box = { payload: null };
  await page.route(
    url => {
      const u = typeof url === 'string' ? new URL(url) : url;
      return u.pathname === pathname;
    },
    async route => {
      if (route.request().method() !== 'PUT') { await route.continue(); return; }
      box.payload = route.request().postDataJSON();
      // ⚠️ 必须 continue 真发到后端，不能 fulfill 假造 200。
      // 本套用例的判据是「空保存后值不变」，不真写库就观测不到任何变化 —— 恒绿。
      // 「空保存」对业务数据的副作用仅限刷新 update_by / update_time，可接受。
      await route.continue();
    }
  );
  return box;
}

/** 等某个 GET 详情回来 —— 数据没加载完就点保存，必然因必填为空而校验失败、根本不发 PUT */
function waitDetail(page, pathname) {
  return page.waitForResponse(
    r => r.request().method() === 'GET' && new URL(r.url()).pathname === pathname,
    { timeout: 25000 }
  );
}

/**
 * 点掉 $modal.confirm 弹出的二次确认框。
 * 按钮文案是「确定」（无空格，plugins/modal.ts 显式指定 confirmButtonText）。
 * 弹框迟迟不出现 = 表单校验没过（validate 失败时既不弹框也不发请求），把校验错误一并报出来。
 */
async function clickMessageBoxConfirm(page, label) {
  const btn = page.locator('.el-message-box__btns .el-button--primary');
  try {
    await expect(btn).toBeVisible({ timeout: 10000 });
  } catch {
    const errs = await page.locator('.el-form-item__error').allTextContents();
    throw new Error(
      `${label}：点击保存后二次确认框没有出现。\n` +
      (errs.length
        ? `原因是表单校验未通过（${errs.length} 项），提交被前端静默拦下：\n  - ${errs.join('\n  - ')}`
        : `页面上没有校验错误提示 —— 请检查保存按钮定位是否正确。`)
    );
  }
  await btn.click();
}

/** 等 PUT 被拦到；等不到时把「为什么没发出去」说清楚，而不是干巴巴一个 timeout */
async function waitForPayload(page, box, label) {
  try {
    await expect.poll(() => box.payload !== null, { timeout: 15000 }).toBe(true);
  } catch {
    const errs = await page.locator('.el-form-item__error').allTextContents();
    throw new Error(
      `未捕获到 PUT ${label} 请求。\n` +
      (errs.length
        ? `原因大概率是表单校验未通过（${errs.length} 项），提交被前端静默拦下：\n  - ${errs.join('\n  - ')}`
        : `页面上没有校验错误提示。排查方向：保存按钮没点到 / 二次确认框未处理 / ` +
          `前端防重复提交拦截器（utils/request.ts:40-67，同 url+同 body 且间隔<1s 直接 reject）把请求挡在了网络层之外。`)
    );
  }
  return box.payload;
}

/**
 * 「空保存」检查的通用流程 —— 9 个模块共用。
 *
 * 三步：① 先把无守卫字段全部造上非空值（否则保存前为 null 的字段无法验证）
 *      ② 在 UI 上什么都不改地保存一次，**等 PUT 响应回来**（不等就会拿到旧值，实测踩过）
 *      ③ 再取详情与 before 比对，值必须原样保留
 *
 * @param openAndSave 由各 block 提供：打开编辑页 → 点保存（含二次确认），不负责等响应
 */
async function blankSaveCheck({ request, token, page, detailPath, putPath, unguarded, ctx, openAndSave }) {
  const H = { Authorization: `Bearer ${token}` };
  const getDetail = async () => {
    const r = await request.get(`${BASE}/dev-api${detailPath}`, { headers: H });
    const b = await r.json();
    expect(b.code, `取详情失败：${detailPath} → ${b.msg}`).toBe(200);
    return b.data;
  };

  // ① 造值：把无守卫字段填满，让每一个都可验证
  const cur = await getDetail();
  const seeded = { ...cur };
  const filled = [];
  for (const f of unguarded) {
    if (!nb(seeded[f.javaField])) {
      seeded[f.javaField] = /Date$/i.test(f.javaField) ? '2026-06-15' : 12345.67;
      filled.push(f.javaField);
    }
  }
  if (filled.length > 0) {
    const r = await request.put(`${BASE}/dev-api${putPath}`, { headers: H, data: seeded });
    const b = await r.json();
    if (b.code !== 200) {
      console.log(`⚠️  造值未成功（${b.msg}），这些字段将无法验证：${filled.join(', ')}`);
    }
  }

  // ② before 基线
  const before = await getDetail();
  console.log(`📸 保存前：` + unguarded.map(f =>
    `${f.javaField}=${JSON.stringify(before?.[f.javaField])}`).join(', '));

  // ③ UI 空保存，等 PUT 响应真正回来
  const putDone = page.waitForResponse(
    r => r.request().method() === 'PUT' && new URL(r.url()).pathname === `/dev-api${putPath}`,
    { timeout: 25000 });
  await openAndSave();
  const putRes = await putDone;
  expect(putRes.status(), 'PUT 应返回 200').toBe(200);

  const after = await getDetail();
  assertUnchanged(before, after, unguarded, ctx);
}

/**
 * 断言「什么都不改地保存一次后，无守卫字段的值必须原样保留」。
 *
 * 为什么不是查 payload 里有没有这个 key（前一版判据，已被证伪）：
 *   规则要防的伤害是「每次编辑把该列写成 NULL」，「key 缺失」只是造成它的**其中一条路径**。
 *   而当编辑页用 `form.value = response.data` 或 `Object.assign(form.value, data)` 整体回填时，
 *   详情接口返回的所有 key 都会并进 form —— payload 里什么 key 都有，key 存在性断言**恒真**。
 *   实证：project/edit.vue 用 Object.assign 回填，把 approvalTime / approverId（在该页 0 匹配）
 *   的守卫去掉后，旧判据仍然全绿。10 个提交点里 8 个属于这种情况。
 *   此外「key 在但值恒为 null」造成同样的 NULL 写入，旧判据对它也完全免疫。
 *
 * 本判据直接观测伤害本身：值变了就是出事了，与回填方式无关。
 *
 * 注意：保存前就是空的字段**无法验证**（区分不了「本来就 null」与「被写成 null」），
 * 必须显式报出，不能计入通过 —— 静默放过正是本项目历次事故的共同成因。
 */
function assertUnchanged(before, after, unguarded, { statement, editPage }) {
  const hasValue = v => v !== null && v !== undefined && v !== '';
  const testable = unguarded.filter(f => hasValue(before?.[f.javaField]));
  const untestable = unguarded.filter(f => !hasValue(before?.[f.javaField]));

  if (untestable.length > 0) {
    console.log(`⚠️  无法验证（该记录上这些字段保存前就是空）：${untestable.map(f => f.javaField).join(', ')}`);
  }
  if (testable.length === 0) {
    throw new Error(
      `${statement}：该记录上 ${unguarded.length} 个无守卫字段**全部为空**，本次断言无法证明任何事。\n` +
      `需要换一条这些字段有值的记录，或先造数把值填上，否则这个 block 是同义反复的绿。\n` +
      `字段：${unguarded.map(f => f.javaField).join(', ')}`
    );
  }

  const changed = testable.filter(f => String(after?.[f.javaField]) !== String(before[f.javaField]));
  if (changed.length > 0) {
    const detail = changed.map(f =>
      `  • ${statement}.${f.javaField}（列 ${f.column}）在 mapper 里无 <if> 守卫（无条件写回），\n` +
      `    在编辑页 ${editPage} 上「什么都不改地保存一次」后，值发生了变化：\n` +
      `    保存前：${JSON.stringify(before[f.javaField])}  →  保存后：${JSON.stringify(after?.[f.javaField])}\n` +
      `    → 该字段没有随表单提交，后端收到 null 后无条件写回，每次编辑都会损坏这一列`
    ).join('\n');
    throw new Error(
      `发现 ${changed.length} 个字段违反规则（Issue #27）：\n${detail}\n\n` +
      `修法二选一：给该字段恢复 <if> 守卫，或让编辑页把它放进提交的 form 里。`
    );
  }
  console.log(`✅ ${testable.length} 个无守卫字段在「空保存」后值原样保留` +
    (untestable.length ? `（另有 ${untestable.length} 个因保存前为空而无法验证）` : ''));
}


/* ──────────────────────────────────────────────────────────────────────────
 * ① ProjectMapper.updateProject —— 提交点 1/2：项目编辑页 project/edit.vue
 * ────────────────────────────────────────────────────────────────────────── */

// edit.vue:548-580 的 24 条必填。任一为空 → validateAndScroll 只滚动到错误字段，
// 不弹确认框也不发请求，测试只会莫名超时。取数阶段先筛掉。
const PROJECT_REQUIRED = [
  'industry', 'region', 'regionCode', 'shortName', 'establishedYear', 'projectCode',
  'projectName', 'projectCategory', 'projectDept', 'projectStatus', 'projectStage',
  'acceptanceStatus', 'estimatedWorkload', 'projectPlan', 'projectDescription',
  'projectAddress', 'projectManagerId', 'marketManagerId', 'participants',
  'salesManagerId', 'salesContact', 'customerId', 'customerContactId', 'projectBudget'
];

test.describe.serial('运行时守卫一致性 · 项目编辑页（ProjectMapper.updateProject 提交点 1/2）', () => {
  let token = null, projectId = null, unguarded = [];

  test('前置：解析 mapper 并取一条可编辑项目', async ({ request }) => {
    unguarded = parseUnguardedFields('ProjectMapper.xml', 'updateProject')
      .filter(f => !AUDIT_FIELDS.has(f.javaField));
    console.log(`📋 ProjectMapper.updateProject 无守卫字段（${unguarded.length}）：` +
      unguarded.map(f => f.javaField).join(', '));
    expect(unguarded.length, 'mapper 里应至少有一个无守卫字段，否则本用例无意义').toBeGreaterThan(0);

    token = await login(request);
    const rows = await fetchRows(request, token, 'project/project/list?pageNum=1&pageSize=50');
    // approvalStatus==='2'（审核拒绝）的项目保存按钮变成「重新提交审核」，
    // 且后端会改状态 + 插审核记录，副作用比普通编辑大 —— 一并滤掉
    const usable = rows.filter(r =>
      r.projectId && PROJECT_REQUIRED.every(f => nb(r[f])) && r.approvalStatus !== '2');
    if (usable.length === 0) {
      console.log(`⏭️ 跳过：${rows.length} 行项目中无一条满足 edit.vue 的 24 条必填校验（校验不过就不会发 PUT）`);
      test.skip();
      return;
    }
    projectId = usable[0].projectId;
    console.log(`📌 用于验证的项目 projectId=${projectId}（${rows.length} 行中可编辑 ${usable.length} 行）`);
  });

  test('什么都不改地保存一次后，无守卫字段的值必须原样保留', async ({ page, context, request }) => {
    if (!projectId) { console.log('⏭️ 前置数据缺失'); test.skip(); return; }
    await context.addCookies([{ name: 'Admin-Token', value: token, url: BASE }]);
    await capturePut(page, '/dev-api/project/project');

    await blankSaveCheck({
      request, token, page,
      detailPath: `/project/project/${projectId}`,
      putPath: '/project/project',
      unguarded,
      ctx: {
        statement: 'ProjectMapper.updateProject',
        editPage: 'ruoyi-ui/src/views/project/project/edit.vue'
      },
      openAndSave: async () => {
        const detail = waitDetail(page, `/dev-api/project/project/${projectId}`);
        // 路由是 path 参数：父 '/project/list/edit' + 子 ':projectId(\d+)'（router/index.ts:121-133）
        await page.goto(`${BASE}/project/list/edit/${projectId}`, { waitUntil: 'networkidle' });
        await detail;
        // 保存按钮文案是动态计算的（submitButtonText），用 primary 按钮定位规避文案漂移
        const saveBtn = page.locator('.form-footer .el-button--primary').first();
        await expect(saveBtn).toBeVisible({ timeout: 15000 });
        await saveBtn.click();
        await clickMessageBoxConfirm(page, '项目编辑页');
      }
    });
  });
});

/* ──────────────────────────────────────────────────────────────────────────
 * ② ProjectMapper.updateProject —— 提交点 2/2：公司收入确认 revenue/company/detail.vue
 *    同一条 update 的第二个 Java 调用方（ProjectController.updateRevenue）。
 *    该页 form.value = response.data（**整体替换**），payload 的 key 集合 100% 等于
 *    GET 响应的 key 集合 —— 后端 select 少查一列就立刻缺 key，比提交点 1 脆弱得多。
 * ────────────────────────────────────────────────────────────────────────── */

test.describe.serial('运行时守卫一致性 · 公司收入确认编辑页（ProjectMapper.updateProject 提交点 2/2）', () => {
  let token = null, projectId = null, unguarded = [];

  test('前置：解析 mapper 并取一条可提交的收入确认记录', async ({ request }) => {
    unguarded = parseUnguardedFields('ProjectMapper.xml', 'updateProject')
      .filter(f => !AUDIT_FIELDS.has(f.javaField));
    console.log(`📋 ProjectMapper.updateProject 无守卫字段（${unguarded.length}）：` +
      unguarded.map(f => f.javaField).join(', '));
    expect(unguarded.length).toBeGreaterThan(0);

    token = await login(request);
    const rows = await fetchRows(request, token, 'project/project/revenue/list?pageNum=1&pageSize=50');
    // 前端必填只有 revenueConfirmStatus；后端另有 4 条必填（Status/Year/confirmAmount/taxRate）
    const usable = rows.filter(r =>
      r.projectId && nb(r.revenueConfirmStatus) && nb(r.revenueConfirmYear) &&
      r.confirmAmount !== null && r.taxRate !== null);
    if (usable.length === 0) {
      console.log(`⏭️ 跳过：${rows.length} 行收入确认记录中无一条满足 detail.vue 的必填校验`);
      test.skip();
      return;
    }
    projectId = usable[0].projectId;
    console.log(`📌 用于验证的项目 projectId=${projectId}（${rows.length} 行中可提交 ${usable.length} 行）`);
  });

  test('什么都不改地保存一次后，无守卫字段的值必须原样保留', async ({ page, context, request }) => {
    if (!projectId) { console.log('⏭️ 前置数据缺失'); test.skip(); return; }
    await context.addCookies([{ name: 'Admin-Token', value: token, url: BASE }]);
    // 注意：拦的是 /project/project/revenue，与提交点 1 的 /project/project 是两条不同路径
    await capturePut(page, '/dev-api/project/project/revenue');

    await blankSaveCheck({
      request, token, page,
      detailPath: `/project/project/revenue/${projectId}`,
      putPath: '/project/project/revenue',
      unguarded,
      openAndSave: async () => {
        const detail = waitDetail(page, `/dev-api/project/project/revenue/${projectId}`);
        // path 参数 + query 混合：不带 ?mode=edit 进去是只读模式，根本没有表单
        await page.goto(`${BASE}/revenue/company/detail/${projectId}?mode=edit`, { waitUntil: 'networkidle' });
        await detail;
        // 按钮文案是「提交」，且前面挂了一个 &nbsp;(U+00A0)，用正则规避归一化差异
        const submitBtn = page.getByRole('button', { name: /提交/ });
        await expect(submitBtn).toBeVisible({ timeout: 15000 });
        await submitBtn.click();   // 此页无二次确认，直接发 PUT
      },
      ctx: {
        statement: 'ProjectMapper.updateProject',
        editPage: 'ruoyi-ui/src/views/revenue/company/detail.vue（?mode=edit）'
      }
    });
  });
});

/* ──────────────────────────────────────────────────────────────────────────
 * ③ PaymentMapper.updatePayment —— payment/form.vue（新增/编辑共用，靠 isEdit 分流）
 * ────────────────────────────────────────────────────────────────────────── */

test.describe.serial('运行时守卫一致性 · 款项编辑页（PaymentMapper.updatePayment）', () => {
  let token = null, paymentId = null, unguarded = [];

  test('前置：解析 mapper 并取一条可编辑款项', async ({ request }) => {
    unguarded = parseUnguardedFields('PaymentMapper.xml', 'updatePayment')
      .filter(f => !AUDIT_FIELDS.has(f.javaField));
    console.log(`📋 PaymentMapper.updatePayment 无守卫字段（${unguarded.length}）：` +
      unguarded.map(f => f.javaField).join(', '));
    expect(unguarded.length).toBeGreaterThan(0);

    token = await login(request);
    const rows = await fetchRows(request, token, 'project/payment/list?pageNum=1&pageSize=50');
    const usable = rows.filter(r =>
      r.paymentId && r.contractId && nb(r.paymentMethodName) && r.paymentAmount !== null &&
      nb(r.hasPenalty) && r.penaltyAmount !== null && nb(r.paymentStatus));
    if (usable.length === 0) {
      console.log(`⏭️ 跳过：${rows.length} 行款项中无一条满足 form.vue 的 6 条必填校验`);
      test.skip();
      return;
    }
    paymentId = usable[0].paymentId;
    console.log(`📌 用于验证的款项 paymentId=${paymentId}（${rows.length} 行中可编辑 ${usable.length} 行）`);
  });

  test('什么都不改地保存一次后，无守卫字段的值必须原样保留', async ({ page, context, request }) => {
    if (!paymentId) { console.log('⏭️ 前置数据缺失'); test.skip(); return; }
    await context.addCookies([{ name: 'Admin-Token', value: token, url: BASE }]);
    await capturePut(page, '/dev-api/project/payment');

    await blankSaveCheck({
      request, token, page,
      detailPath: `/project/payment/${paymentId}`,
      putPath: '/project/payment',
      unguarded,
      openAndSave: async () => {
        const detail = waitDetail(page, `/dev-api/project/payment/${paymentId}`);
        // path 参数，父路径是 /htkx/payment/edit（不是 /project/payment）；
        // 走 /htkx/payment/add 只会发 POST，抓不到 PUT
        await page.goto(`${BASE}/htkx/payment/edit/${paymentId}`, { waitUntil: 'networkidle' });
        await detail;
        const saveBtn = page.locator('.form-footer .el-button--primary').first();
        await expect(saveBtn).toBeVisible({ timeout: 15000 });
        await saveBtn.click();
        await clickMessageBoxConfirm(page, '款项编辑页');   // 编辑模式才有二次确认
      },
      ctx: {
        statement: 'PaymentMapper.updatePayment',
        editPage: 'ruoyi-ui/src/views/project/payment/form.vue（isEdit 分支）'
      }
    });
  });
});

/* ──────────────────────────────────────────────────────────────────────────
 * ④ ContractMapper.updateContract —— contract/edit.vue
 * ────────────────────────────────────────────────────────────────────────── */

test.describe.serial('运行时守卫一致性 · 合同编辑页（ContractMapper.updateContract）', () => {
  let token = null, contractId = null, unguarded = [];

  test('前置：解析 mapper 并取一条可编辑合同', async ({ request }) => {
    unguarded = parseUnguardedFields('ContractMapper.xml', 'updateContract')
      .filter(f => !AUDIT_FIELDS.has(f.javaField));
    console.log(`📋 ContractMapper.updateContract 无守卫字段（${unguarded.length}）：` +
      unguarded.map(f => f.javaField).join(', '));
    expect(unguarded.length).toBeGreaterThan(0);

    token = await login(request);
    const rows = await fetchRows(request, token, 'project/contract/list?pageNum=1&pageSize=50');
    // 只取「没有关联项目」的合同：关联项目是二段式回填（getContract 回来后才发 listProjectByDept），
    // 抢在第二个请求前提交 → ContractServiceImpl 先删后建会把关联物理删光。
    // 本用例另用 route.fulfill 让 PUT 根本不落库，这里是第二道防线。
    const usable = rows.filter(r => r.contractId && (r.projectList || []).length === 0);
    if (usable.length === 0) {
      console.log(`⏭️ 跳过：${rows.length} 行合同中没有「未关联项目」的行（关联行有删关联的风险，不取）`);
      test.skip();
      return;
    }
    contractId = usable[0].contractId;
    console.log(`📌 用于验证的合同 contractId=${contractId}（${rows.length} 行中未关联项目的 ${usable.length} 行）`);
  });

  test('什么都不改地保存一次后，无守卫字段的值必须原样保留', async ({ page, context, request }) => {
    if (!contractId) { console.log('⏭️ 前置数据缺失'); test.skip(); return; }
    await context.addCookies([{ name: 'Admin-Token', value: token, url: BASE }]);
    await capturePut(page, '/dev-api/project/contract');

    await blankSaveCheck({
      request, token, page,
      detailPath: `/project/contract/${contractId}`,
      putPath: '/project/contract',
      unguarded,
      openAndSave: async () => {
        const detail = waitDetail(page, `/dev-api/project/contract/${contractId}`);
        await page.goto(`${BASE}/htkx/contract/edit/${contractId}`, { waitUntil: 'networkidle' });
        await detail;
        const saveBtn = page.locator('.form-footer .el-button--primary').first();
        await expect(saveBtn).toBeVisible({ timeout: 15000 });
        await saveBtn.click();
        // 合同名唯一性是**异步** validator，validate() 要等它回来才 resolve，确认框是延迟出现的
        await clickMessageBoxConfirm(page, '合同编辑页');
      },
      ctx: {
        statement: 'ContractMapper.updateContract',
        editPage: 'ruoyi-ui/src/views/project/contract/edit.vue'
      }
    });
  });
});

/* ──────────────────────────────────────────────────────────────────────────
 * ⑤ CustomerMapper.updateCustomer —— customer/index.vue（列表页内嵌 dialog）
 *    唯一一个没有独立 edit.vue 的模块：要先进列表 → 点行内「编辑」开弹窗。
 * ────────────────────────────────────────────────────────────────────────── */

test.describe.serial('运行时守卫一致性 · 客户编辑弹窗（CustomerMapper.updateCustomer）', () => {
  let token = null, customer = null, unguarded = [];

  test('前置：解析 mapper 并取一条可编辑客户', async ({ request }) => {
    // CustomerMapper.xml 是 CRLF —— parseUnguardedFields 的 split(/\r?\n/) 在这里是必需的，
    // 按 \n 切会让 javaField 变成 'salesManagerId\r'，与 payload key 永远比不上（假红灯）
    unguarded = parseUnguardedFields('CustomerMapper.xml', 'updateCustomer')
      .filter(f => !AUDIT_FIELDS.has(f.javaField));
    console.log(`📋 CustomerMapper.updateCustomer 无守卫字段（${unguarded.length}）：` +
      unguarded.map(f => f.javaField).join(', '));
    expect(unguarded.length).toBeGreaterThan(0);

    token = await login(request);
    const rows = await fetchRows(request, token, 'project/customer/list?pageNum=1&pageSize=100');
    // 取 0 联系人的客户：submitForm 会逐个校验联系人必填，任一为空只弹 msgError 不发 PUT；
    // 且 CustomerServiceImpl 是「先全删联系人再重插」，0 联系人把风险归零
    const usable = rows.filter(r =>
      r.customerId && nb(r.customerSimpleName) && (r.customerContactList || []).length === 0);
    if (usable.length === 0) {
      console.log(`⏭️ 跳过：${rows.length} 行客户中没有「零联系人」的行`);
      test.skip();
      return;
    }
    customer = usable[0];
    console.log(`📌 用于验证的客户 customerId=${customer.customerId}「${customer.customerSimpleName}」` +
      `（${rows.length} 行中零联系人 ${usable.length} 行）`);
  });

  test('什么都不改地保存一次后，无守卫字段的值必须原样保留', async ({ page, context, request }) => {
    if (!customer) { console.log('⏭️ 前置数据缺失'); test.skip(); return; }
    await context.addCookies([{ name: 'Admin-Token', value: token, url: BASE }]);
    await capturePut(page, '/dev-api/project/customer');

    await blankSaveCheck({
      request, token, page,
      detailPath: `/project/customer/${customer.customerId}`,
      putPath: '/project/customer',
      unguarded,
      openAndSave: async () => {
        // /market/customer 是后端菜单驱动的**动态路由**（router/index.ts 里搜不到），
        // 首次导航要等 permission.ts 走完 getInfo + generateRoutes
        await page.goto(`${BASE}/market/customer`, { waitUntil: 'networkidle' });
        await expect(page.locator('.el-table__row').first()).toBeVisible({ timeout: 20000 });

        // 列表 ORDER BY update_time DESC，行序在多次运行间不稳定 → 按客户简称文本定位，别用下标
        const row = page.locator('.el-table__row').filter({ hasText: customer.customerSimpleName }).first();
        await expect(row).toBeVisible({ timeout: 15000 });

        const detail = waitDetail(page, `/dev-api/project/customer/${customer.customerId}`);
        // 行内按钮文案是「编辑」（不是「修改」）
        await row.getByRole('button', { name: '编辑' }).click();
        await detail;

        // 弹窗 append-to-body，且页面里还有第二个「客户详情」弹窗 → 必须 scope 到可见弹窗。
        // 确定按钮文案是「确 定」——**两个汉字中间有一个空格**，getByRole({name:'确定'}) 匹配不到
        const dialog = page.locator('.el-dialog:visible');
        await expect(dialog).toBeVisible({ timeout: 10000 });
        await dialog.getByRole('button', { name: /确\s*定/ }).click();
      },
      ctx: {
        statement: 'CustomerMapper.updateCustomer',
        editPage: 'ruoyi-ui/src/views/project/customer/index.vue（列表内嵌编辑弹窗）'
      }
    });
  });
});

/* ──────────────────────────────────────────────────────────────────────────
 * ⑥ VersionOutMapper.updateVersionOut —— versionOut/edit.vue（批次版本）
 * ────────────────────────────────────────────────────────────────────────── */

test.describe.serial('运行时守卫一致性 · 批次版本编辑页（VersionOutMapper.updateVersionOut）', () => {
  let token = null, versionId = null, unguarded = [];

  test('前置：解析 mapper 并取一条可编辑批次版本', async ({ request }) => {
    // 同一个 mapper 文件里有两条主 CRUD：updateVersionOutManual(166) 排在 updateVersionOut(222) **之前**。
    // parseUnguardedFields 的正则带闭合双引号，能正确区分，别把它改成 id="updateVersionOut[^"]*"
    unguarded = parseUnguardedFields('VersionOutMapper.xml', 'updateVersionOut')
      .filter(f => !AUDIT_FIELDS.has(f.javaField));
    console.log(`📋 VersionOutMapper.updateVersionOut 无守卫字段（${unguarded.length}）：` +
      unguarded.map(f => f.javaField).join(', '));
    expect(unguarded.length).toBeGreaterThan(0);

    token = await login(request);
    const rows = await fetchRows(request, token, 'project/versionOut/list?pageNum=1&pageSize=200');
    // batchId 为空的是迁移行（只有 proBatchNo 快照），后端 @NotNull 会拒（500「投产批次不能为空」）
    const usable = rows.filter(r => r.id && r.batchId);
    if (usable.length === 0) {
      console.log(`⏭️ 跳过：${rows.length} 行批次版本全是 batchId 为空的迁移行，保存必被后端拒`);
      test.skip();
      return;
    }
    versionId = usable[0].id;
    console.log(`📌 用于验证的批次版本 id=${versionId}（${rows.length} 行中非迁移行 ${usable.length} 行）`);
  });

  test('什么都不改地保存一次后，无守卫字段的值必须原样保留', async ({ page, context, request }) => {
    if (!versionId) { console.log('⏭️ 前置数据缺失'); test.skip(); return; }
    await context.addCookies([{ name: 'Admin-Token', value: token, url: BASE }]);

    // pathname 全等比较：/dev-api/project/versionOut 不会误捕 /versionOutManual，也不会误捕详情 GET
    await capturePut(page, '/dev-api/project/versionOut');

    await blankSaveCheck({
      request, token, page,
      detailPath: `/project/versionOut/${versionId}`,
      putPath: '/project/versionOut',
      unguarded,
      openAndSave: async () => {
        const detail = waitDetail(page, `/dev-api/project/versionOut/${versionId}`);
        await page.goto(`${BASE}/project/versionOut/edit/${versionId}`, { waitUntil: 'networkidle' });
        await detail;
        const saveBtn = page.getByRole('button', { name: '保存' });
        await expect(saveBtn).toBeVisible({ timeout: 15000 });
        await saveBtn.click();   // 无弹窗、无二次确认
      },
      ctx: {
        statement: 'VersionOutMapper.updateVersionOut',
        editPage: 'ruoyi-ui/src/views/project/versionOut/edit.vue'
      }
    });
  });
});

/* ──────────────────────────────────────────────────────────────────────────
 * ⑦ VersionOutMapper.updateVersionOutManual —— versionOutManual/edit.vue（非批次版本）
 *    同文件第二条主 CRUD。它的无守卫字段只有 outVersion 一个，
 *    versionStatus / packageMode / versionBrief 在这条 SQL 里压根不出现 —— 别套批次那条的结论。
 * ────────────────────────────────────────────────────────────────────────── */

test.describe.serial('运行时守卫一致性 · 非批次版本编辑页（VersionOutMapper.updateVersionOutManual）', () => {
  let token = null, versionId = null, unguarded = [];

  test('前置：解析 mapper 并取一条可编辑非批次版本', async ({ request }) => {
    unguarded = parseUnguardedFields('VersionOutMapper.xml', 'updateVersionOutManual')
      .filter(f => !AUDIT_FIELDS.has(f.javaField));
    console.log(`📋 VersionOutMapper.updateVersionOutManual 无守卫字段（${unguarded.length}）：` +
      unguarded.map(f => f.javaField).join(', '));
    expect(unguarded.length).toBeGreaterThan(0);

    token = await login(request);
    const rows = await fetchRows(request, token, 'project/versionOutManual/list?pageNum=1&pageSize=200');
    // ① batchId 为空的迁移行会被 validateManual 拒（非批次这边占比过半）
    // ② versionType 5/6（升级包）额外要求 outVersion 非空，避开更稳
    const usable = rows.filter(r => r.id && r.batchId && !['5', '6'].includes(String(r.versionType)));
    if (usable.length === 0) {
      console.log(`⏭️ 跳过：${rows.length} 行非批次版本中无一条同时满足「有 batchId」且「非升级包」`);
      test.skip();
      return;
    }
    versionId = usable[0].id;
    console.log(`📌 用于验证的非批次版本 id=${versionId}（${rows.length} 行中可保存 ${usable.length} 行）`);
  });

  test('什么都不改地保存一次后，无守卫字段的值必须原样保留', async ({ page, context, request }) => {
    if (!versionId) { console.log('⏭️ 前置数据缺失'); test.skip(); return; }
    await context.addCookies([{ name: 'Admin-Token', value: token, url: BASE }]);
    await capturePut(page, '/dev-api/project/versionOutManual');

    await blankSaveCheck({
      request, token, page,
      detailPath: `/project/versionOutManual/${versionId}`,
      putPath: '/project/versionOutManual',
      unguarded,
      openAndSave: async () => {
        const detail = waitDetail(page, `/dev-api/project/versionOutManual/${versionId}`);
        await page.goto(`${BASE}/project/versionOutManual/edit/${versionId}`, { waitUntil: 'networkidle' });
        await detail;
        const saveBtn = page.getByRole('button', { name: '保存' });
        await expect(saveBtn).toBeVisible({ timeout: 15000 });
        await saveBtn.click();
      },
      ctx: {
        statement: 'VersionOutMapper.updateVersionOutManual',
        editPage: 'ruoyi-ui/src/views/project/versionOutManual/edit.vue'
      }
    });
  });
});

/* ──────────────────────────────────────────────────────────────────────────
 * ⑧ ProlistDefectMapper.updateProlistDefect —— prolistDefect/add.vue（add.vue 兼编辑）
 * ────────────────────────────────────────────────────────────────────────── */

// add.vue:180-196 的 required 规则
const PROLIST_REQUIRED = [
  'productionYear', 'problemNo', 'problemLevel', 'currentStatus', 'submitDate', 'verifyDate',
  'whetherDefect', 'whetherOvertime', 'whetherProRecurrence', 'whetherAttRequired',
  'whetherUpdateVersion', 'defectDesc'
];

test.describe.serial('运行时守卫一致性 · 批次问题单编辑页（ProlistDefectMapper.updateProlistDefect）', () => {
  let token = null, problemId = null, unguarded = [];

  test('前置：解析 mapper 并取一条可编辑问题单', async ({ request }) => {
    unguarded = parseUnguardedFields('ProlistDefectMapper.xml', 'updateProlistDefect')
      .filter(f => !AUDIT_FIELDS.has(f.javaField));
    // 说明：solutionTimeOverOneDay 是服务端派生字段（ProlistDefectServiceImpl 在调 mapper 前
    // 无条件覆盖入参），这一列其实不可能被「缺 key」写成 NULL。此处**仍然纳入断言**——
    // 它当前确实在 payload 里（靠 loadForEdit 的 Object.assign 整体回灌 GET 详情），
    // 纳入是对规则的忠实执行。若它将来变红，先核对 ServiceImpl 再决定是改前端还是加豁免。
    console.log(`📋 ProlistDefectMapper.updateProlistDefect 无守卫字段（${unguarded.length}）：` +
      unguarded.map(f => f.javaField).join(', '));
    expect(unguarded.length).toBeGreaterThan(0);

    token = await login(request);
    const rows = await fetchRows(request, token, 'project/prolistDefect/list?pageNum=1&pageSize=100');
    // 批次/部门/任务号是 validator 规则：FK 或快照文本二者有其一即可（迁移记录靠快照放行）
    const usable = rows.filter(r =>
      r.problemId && PROLIST_REQUIRED.every(f => nb(r[f])) &&
      (r.batchId || nb(r.batchNo)) && (r.deptId || nb(r.deptName)) && (r.taskId || nb(r.taskCode)));
    if (usable.length === 0) {
      console.log(`⏭️ 跳过：${rows.length} 行问题单中无一条满足 add.vue 的必填校验`);
      test.skip();
      return;
    }
    problemId = usable[0].problemId;
    console.log(`📌 用于验证的问题单 problemId=${problemId}（${rows.length} 行中可编辑 ${usable.length} 行）`);
  });

  test('什么都不改地保存一次后，无守卫字段的值必须原样保留', async ({ page, context, request }) => {
    if (!problemId) { console.log('⏭️ 前置数据缺失'); test.skip(); return; }
    await context.addCookies([{ name: 'Admin-Token', value: token, url: BASE }]);

    await capturePut(page, '/dev-api/project/prolistDefect');

    await blankSaveCheck({
      request, token, page,
      detailPath: `/project/prolistDefect/${problemId}`,
      putPath: '/project/prolistDefect',
      unguarded,
      openAndSave: async () => {
        const detail = waitDetail(page, `/dev-api/project/prolistDefect/${problemId}`);
        // 这个模块的路由是 **query 参数**（子路由 path: ''），不是 path 参数
        await page.goto(`${BASE}/project/prolistDefect/edit?problemId=${problemId}`, { waitUntil: 'networkidle' });
        await detail;
        const saveBtn = page.getByRole('button', { name: '保存' });
        await expect(saveBtn).toBeVisible({ timeout: 15000 });
        // 点保存后还会先发一次 checkProblemNo（GET，不同 pathname，不会被拦），返回 false 才 return
        await saveBtn.click();
      },
      ctx: {
        statement: 'ProlistDefectMapper.updateProlistDefect',
        editPage: 'ruoyi-ui/src/views/project/prolistDefect/add.vue（edit.vue 只是壳）'
      }
    });
  });
});

/* ──────────────────────────────────────────────────────────────────────────
 * ⑨ NobatchProlistDefectMapper.updateNobatchProlistDefect —— nobatchProlist/add.vue
 *    16 个无守卫字段，是本文件里最多的一条。
 * ────────────────────────────────────────────────────────────────────────── */

test.describe.serial('运行时守卫一致性 · 非批次问题单编辑页（NobatchProlistDefectMapper.updateNobatchProlistDefect）', () => {
  let token = null, problemId = null, unguarded = [];

  test('前置：解析 mapper 并取一条可编辑非批次问题单', async ({ request }) => {
    unguarded = parseUnguardedFields('NobatchProlistDefectMapper.xml', 'updateNobatchProlistDefect')
      .filter(f => !AUDIT_FIELDS.has(f.javaField));
    console.log(`📋 NobatchProlistDefectMapper.updateNobatchProlistDefect 无守卫字段（${unguarded.length}）：` +
      unguarded.map(f => f.javaField).join(', '));
    expect(unguarded.length).toBeGreaterThan(0);

    token = await login(request);
    const rows = await fetchRows(request, token, 'project/nobatchProlist/list?pageNum=1&pageSize=100');
    // 与批次版差异：任务号是纯文本 taskNo（required），不是 FK taskId
    const usable = rows.filter(r =>
      r.problemId && PROLIST_REQUIRED.every(f => nb(r[f])) && nb(r.taskNo) &&
      (r.batchId || nb(r.batchNo)) && (r.deptId || nb(r.deptName)));
    if (usable.length === 0) {
      console.log(`⏭️ 跳过：${rows.length} 行非批次问题单中无一条满足 add.vue 的必填校验`);
      test.skip();
      return;
    }
    problemId = usable[0].problemId;
    console.log(`📌 用于验证的非批次问题单 problemId=${problemId}（${rows.length} 行中可编辑 ${usable.length} 行）`);
  });

  test('什么都不改地保存一次后，无守卫字段的值必须原样保留', async ({ page, context, request }) => {
    if (!problemId) { console.log('⏭️ 前置数据缺失'); test.skip(); return; }
    await context.addCookies([{ name: 'Admin-Token', value: token, url: BASE }]);

    await capturePut(page, '/dev-api/project/nobatchProlist');

    // ⚠️ 本模块判据强度弱于其它 block，如实标注：
    // 该模块的 GET 详情对 task_name / product / internal_closure_date 等 6 列做了
    // COALESCE 快照兜底（本表为空时回落到关联任务的值）。这 6 列在 update 里无条件写回，
    // 于是「空保存」会把快照派生值物化进本表 —— 而 GET 前后都返回同一个快照值，
    // 「值不变」断言对这 6 列**观测不到差异**，仅对其余 10 列有效。
    // 要覆盖这 6 列需要直接查库（绕过 COALESCE），属后续增强。
    await blankSaveCheck({
      request, token, page,
      detailPath: `/project/nobatchProlist/${problemId}`,
      putPath: '/project/nobatchProlist',
      unguarded,
      openAndSave: async () => {
        const detail = waitDetail(page, `/dev-api/project/nobatchProlist/${problemId}`);
        await page.goto(`${BASE}/project/nobatchProlist/edit?problemId=${problemId}`, { waitUntil: 'networkidle' });
        await detail;
        const saveBtn = page.getByRole('button', { name: '保存' });
        await expect(saveBtn).toBeVisible({ timeout: 15000 });
        await saveBtn.click();
      },
      ctx: {
        statement: 'NobatchProlistDefectMapper.updateNobatchProlistDefect',
        editPage: 'ruoyi-ui/src/views/project/nobatchProlist/add.vue（edit.vue 只是壳）'
      }
    });
  });
});
