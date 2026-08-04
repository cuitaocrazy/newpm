/**
 * 运行时守卫一致性检查 —— 原型（Issue #27）
 *
 * 规则：mapper 主 CRUD update 中**无 <if> 守卫**（无条件 col = #{field}）的字段，
 *       必须真的出现在该模块编辑页实际提交的 PUT payload 里。
 *       否则 PUT 请求体没有这个 key → 后端收到 null → 每次编辑把该列写成 NULL。
 *
 * 为什么用运行时而不是静态分析：
 *   静态方案（scripts/check-guard-consistency.mjs）已被实验证伪 —— 它判定的是
 *   「字段名在 .vue 文件里出现过」，而规则要的是「payload 里有没有这个 key」。
 *   实测：保持 productionDate 缺陷不动，只在 edit.vue 加一行**被注释掉的**
 *   `// form.productionDate = data.productionDate`，检查器就从 FAIL 1 变成 PASS 45。
 *   JS 的表达力（注释 / v-if 分支 / 展开运算符 / 提交前重构 payload）远超正则，
 *   补正则永远追不上。
 *
 * 本方案直接观测事实：真实打开编辑页 → 点保存 → 拦截那个 PUT → 取 payload 的 key 集合。
 * 不受任何写法影响。
 *
 * 运行前提：后端 + vite 均已启动，用 PROTO_BASE_URL 指定前端地址。
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

  test('编辑页提交的 payload 必须含全部无守卫字段', async ({ page, context }) => {
    if (!taskId) { console.log('⏭️ 前置数据缺失'); test.skip(); return; }

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

    await page.getByRole('button', { name: '保存' }).click();
    await expect.poll(() => payload !== null, {
      message: '未捕获到 PUT /project/task 请求',
      timeout: 15000
    }).toBe(true);

    const keys = new Set(Object.keys(payload));
    console.log(`📦 实际 PUT payload 含 ${keys.size} 个 key`);

    const missing = unguarded.filter(f => !keys.has(f.javaField));
    if (missing.length > 0) {
      const detail = missing.map(f =>
        `  • TaskMapper.updateTask.${f.javaField}（列 ${f.column}）无守卫，` +
        `但编辑页提交的 payload 里没有这个 key\n` +
        `    → 每次任务编辑都会把 ${f.column} 写成 NULL`).join('\n');
      throw new Error(
        `发现 ${missing.length} 个字段违反规则（Issue #27）：\n${detail}\n\n` +
        `payload 实际 key：${[...keys].sort().join(', ')}`
      );
    }
    console.log(`✅ ${unguarded.length} 个无守卫字段全部出现在真实 payload 中`);
  });
});
