/**
 * E2E：日报保存的工时保护与项目归属校验（specs/015-daily-report-ownership-check）
 *
 * 为什么必须有 e2e：本特性有 4 条不变式是纯数据库层的——
 * 「被保留的明细字段逐字未变」靠的是「范围外的行压根没被 touch」，
 * 而服务层单测把 Mapper 全 mock 掉了，无从验证。凡是断言「某个东西没有被改动」的，
 * 单测通常无能为力。返回码语义（见「删除后仍返回成功」一例）同样只有真实调用才暴露得出来。
 *
 * ⚠️⚠️ 本套件是【破坏性】的：它会重写 admin 当日日报、删除日报主记录与明细。
 * 只能打在专用造数库上，绝不能打在真实开发库/生产库上。
 * beforeAll 里的 assertFixtureDatabase() 就是为此设的闸门，见其注释。
 *
 * 前置条件：
 *   1. 后端已启动，且【连的是专用造数库】（用 E2E_API_URL 指定，默认 http://localhost:8085）
 *   2. 已执行造数：tests/fixtures/015-daily-report-ownership-seed.sql
 *   3. 验证码已关闭：UPDATE sys_config SET config_value='false'
 *                    WHERE config_key='sys.account.captchaEnabled';
 *   4. admin 密码用 E2E_ADMIN_PASSWORD 指定（全新导入的库是 admin123，
 *      长期库通常是 123456789）
 *
 * 造一个专用库（docker 本地，不碰 dev 库 ry-vue）——三步，完整命令见
 * specs/015-daily-report-ownership-check/quickstart.md：
 *   1. mysqldump --no-data 导出 ry-vue 的纯结构到新库 ry_vue_e2e
 *      （用真实库结构而非 pm-sql/init，后者与线上有 schema 漂移）
 *   2. 只导 sys_* 基准数据 + 工作日历 + 白名单，业务表全部留空
 *      —— 业务表留空，fixture 的 SIGNAL 闸门才会放行
 *   3. 用 --spring.config.additional-location 起一个专跑 e2e 的后端指向该库
 *      （命令行 --spring.datasource.druid.master.url 在本项目的 DruidConfig 上不生效）
 *
 * 运行：
 *   E2E_API_URL=http://localhost:8087 E2E_ADMIN_PASSWORD=<造数库的 admin 密码> \
 *     npx playwright test e2e-daily-report-ownership.spec.js
 *
 * ⚠️ 本 spec 直连后端、认的是 E2E_API_URL——不是 E2E_BASE_URL。
 * 仓库里三套 daily-report e2e 用了三种约定（本文件 = E2E_API_URL 直连后端；
 * e2e-daily-report / e2e-team-daily-workload 走 helpers/api-client.js 的
 * E2E_BASE_URL + /dev-api 前缀；e2e-daily-report-write-filter = E2E_BASE_URL）。
 * 设错变量时本文件会静默回落到默认的 8085，也就是开发者自己的 dev 后端——
 * 那背后是真实库。设错变量已经真的发生过一次，所以才有下面的闸门。
 *
 * 本 spec 不使用 tests/helpers/api-client.js：后者带 /dev-api 前缀，依赖 vite 代理。
 */

import { test, expect, request as playwrightRequest } from '@playwright/test';

const API = process.env.E2E_API_URL || 'http://localhost:8085';
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD || '123456789';

// 造数约定的固定 ID，见 tests/fixtures/015-daily-report-ownership-seed.sql
const P_ACTIVE = 990100;    // 在建 + admin 是成员    → 可见，作用范围内
const P_CLOSED = 990200;    // 已结项 + admin 是成员  → 不可见，作用范围外
const P_STRANGER = 990300;  // 在建，admin 从未参与   → 归属校验须拒绝
const P_FORMER = 990400;    // 在建，admin 已离场     → 归属校验须放行
const P_ROLE_ONLY = 990500; // 在建，admin 是市场经理但成员表无行 → 归属校验须放行（读写同源）

let ctx;
let headers;

/** 读取某日日报的全部明细（后端不做可见性过滤，含填报人看不到的行） */
async function readDay(date) {
  const resp = await ctx.get(`/project/dailyReport/my/${date}`, { headers });
  const body = await resp.json();
  return body.data || {};
}

async function detailOf(date, projectId) {
  const day = await readDay(date);
  return (day.detailList || []).find((d) => Number(d.projectId) === projectId);
}

async function saveDay(date, detailList) {
  const resp = await ctx.post('/project/dailyReport', {
    headers,
    data: { reportDate: date, detailList },
  });
  return resp.json();
}

async function actualWorkloadOf(projectId) {
  const resp = await ctx.get(`/project/project/${projectId}`, { headers });
  const body = await resp.json();
  return Number((body.data || {}).actualWorkload ?? 0);
}

/**
 * 造数库闸门 —— 必须在任何用例之前跑，失败即整套中止。
 *
 * 为什么需要它：本套件是破坏性的（重写当日日报、删主记录与明细），而 API 地址是个
 * 有默认值的环境变量。默认值 8085 恰好就是开发者自己的 dev 后端，背后是真实库；
 * 变量名又与另两套 daily-report e2e 不同（E2E_API_URL vs E2E_BASE_URL），
 * 极易设错。设错时没有任何征兆：登录会成功、请求会成功，用例只是断言失败，
 * 而破坏性写入已经落到真实库了 —— 这已经真实发生过一次
 * （空 detailList 的保存把一条真实日报的明细清空、汇总归零）。
 *
 * 判据用「有没有非造数项目」而不是「有没有 990100」：后者只能证明造数已灌，
 * 证不了这个库【只有】造数——而真实库里 admin 往往也挂着真实项目，
 * 那些项目当天的工时会被本套件的 saveDay 连带重写。
 *
 * 这是 API 侧闸门；DB 侧的对应物是 fixture 里的 SIGNAL 闸门
 * （有效项目数 > 20 即判定真实库、拒绝造数）。两道各自独立、缺一不可：
 * fixture 拦「往真实库灌造数」，本闸门拦「拿真实库跑用例」。
 */
async function assertFixtureDatabase() {
  const resp = await ctx.get('/project/dailyReport/myProjects', { headers });
  const body = await resp.json();
  const ids = (body.data || body.rows || []).map((p) => Number(p.projectId));
  const foreign = ids.filter((id) => id < 990000);

  if (!ids.includes(P_ACTIVE)) {
    throw new Error(
      `[造数库闸门] 拒绝执行：找不到造数项目 ${P_ACTIVE}。\n` +
        `当前 API = ${API}，myProjects = [${ids}]。\n` +
        `请先执行 tests/fixtures/015-daily-report-ownership-seed.sql，` +
        `并确认 E2E_API_URL 指向连着该造数库的后端。`
    );
  }
  if (foreign.length > 0) {
    throw new Error(
      `[造数库闸门] 拒绝执行：${API} 背后的库里 admin 还挂着非造数项目 [${foreign}]，` +
        `判定为真实库。\n` +
        `本套件是破坏性的（会重写 admin 当日日报、删除日报主记录与明细），` +
        `只能打在专用造数库上。\n` +
        `请用 E2E_API_URL 指向专跑 e2e 的后端（造库步骤见本文件头部注释）。`
    );
  }
}

test.beforeAll(async () => {
  ctx = await playwrightRequest.newContext({ baseURL: API });
  const resp = await ctx.post('/login', {
    data: { username: 'admin', password: ADMIN_PASSWORD },
  });
  const body = await resp.json();
  if (body.code !== 200) {
    throw new Error(`登录失败：${body.msg}（密码用 E2E_ADMIN_PASSWORD 指定）`);
  }
  headers = { Authorization: `Bearer ${body.token}` };

  // ⚠️ 顺序不可调：闸门必须在任何破坏性用例之前，且必须在拿到 headers 之后。
  await assertFixtureDatabase();
});

test.afterAll(async () => {
  await ctx.dispose();
});

// ===========================================================================
// 前置：确认「可见 / 不可见」的边界确实成立，否则后续用例全部失去意义
// ===========================================================================

test('前置｜可填项目列表不含已结项项目与从未参与的项目', async () => {
  const resp = await ctx.get('/project/dailyReport/myProjects', { headers });
  const body = await resp.json();
  const ids = (body.data || body.rows || []).map((p) => Number(p.projectId));

  expect(ids).toContain(P_ACTIVE);
  expect(ids).not.toContain(P_CLOSED);   // 已结项 → 填写页看不到 → 作用范围外
  expect(ids).not.toContain(P_STRANGER); // 非成员 → 看不到
});

// ===========================================================================
// User Story 1a：保存日报时不丢失填报人看不见的工时
// ===========================================================================

test('US1a｜只提交可见项目时，不可见项目的工时原样保留（含字段逐字不变）', async () => {
  const DATE = '2026-07-20';
  const before = await detailOf(DATE, P_CLOSED);
  expect(before, '造数应包含一条不可见工时').toBeTruthy();

  const closedWorkloadBefore = await actualWorkloadOf(P_CLOSED);

  // 模拟填写页：它只能提交自己显示得出来的行
  const res = await saveDay(DATE, [
    { projectId: P_ACTIVE, subProjectId: null, entryType: 'work', workHours: 6, workContent: 'e2e-改为6小时' },
  ]);
  expect(res.code).toBe(200);

  const after = await detailOf(DATE, P_CLOSED);
  expect(after, '不可见工时不得被删除（FR-001，本特性的核心）').toBeTruthy();
  expect(Number(after.workHours)).toBe(Number(before.workHours));
  // INV-3：字段逐字不变——这条单测验证不了，只有真实数据库能证实
  expect(after.workContent).toBe(before.workContent);
  expect(after.subProjectId ?? null).toBe(before.subProjectId ?? null);
  expect(after.workCategory ?? null).toBe(before.workCategory ?? null);

  // 提交的那条按新值更新
  const active = await detailOf(DATE, P_ACTIVE);
  expect(Number(active.workHours)).toBe(6);

  // 不可见项目的实际人天不受影响
  expect(await actualWorkloadOf(P_CLOSED)).toBe(closedWorkloadBefore);
});

test('US1a｜护栏：可见项目的工时被清零后仍应删除（防丢失不得堵死正常删除）', async () => {
  const DATE = '2026-07-20';

  // 提交里不含 P_ACTIVE，等价于填报人把它清零
  const res = await saveDay(DATE, []);
  expect(res.code).toBe(200);

  expect(await detailOf(DATE, P_ACTIVE), '可见项目未提交即应删除（FR-002 / INV-4）').toBeFalsy();
  expect(await detailOf(DATE, P_CLOSED), '不可见的仍须保留').toBeTruthy();
});

// ===========================================================================
// User Story 1b：删除整条日报时不丢失填报人看不见的工时
// ===========================================================================

test('US1b｜删除整条日报时，不可见工时保留、主记录保留、当日汇总重算', async () => {
  const DATE = '2026-07-21';
  const day = await readDay(DATE);
  const reportId = day.reportId;
  expect(reportId, '造数应存在该日日报').toBeTruthy();

  const resp = await ctx.delete(`/project/dailyReport/${reportId}`, { headers });
  expect((await resp.json()).code).toBe(200);

  const after = await readDay(DATE);
  expect(after.reportId, '仍有工时被保留时主记录必须保留（FR-014 / INV-D1）').toBeTruthy();

  const details = after.detailList || [];
  expect(details.length, '只应剩下不可见的那条').toBe(1);
  expect(Number(details[0].projectId)).toBe(P_CLOSED);
  expect(Number(details[0].workHours)).toBe(2);

  // INV-D2：主记录汇总须按剩余 work 明细重算
  expect(Number(after.totalWorkHours)).toBe(2);
});

test('US1b｜回归：明细全部被保留、无主记录可删时，仍须返回成功', async () => {
  // 这个缺陷是 e2e 实测暴露的（2026-08-03）：当时数据层面三条断言全对，
  // 但接口返回 500「操作失败」，因为没有主记录可删导致 rows=0 被 toAjax 判为失败。
  // 填报人会看到失败提示并重复点击。
  const DATE = '2026-07-24';
  const day = await readDay(DATE);
  const reportId = day.reportId;
  expect(reportId).toBeTruthy();

  const resp = await ctx.delete(`/project/dailyReport/${reportId}`, { headers });
  const body = await resp.json();
  expect(body.code, '数据层面操作已成功，不得返回失败').toBe(200);

  const after = await readDay(DATE);
  expect((after.detailList || []).length, '不可见工时应全部保留').toBe(1);
});

// ===========================================================================
// Issue #13：删除日报的归属校验（只能删自己的）
//
// 为什么必须有 e2e：user_id 限定落在 SQL 里，而服务层单测把 Mapper 全 mock 了，
// 原理上验证不了「SQL 真的没删到他人的行」。这两条用例是归属限定的唯一实证。
// ===========================================================================

/** 通过月度接口读取任意用户的日报（admin 数据权限为全量），含明细 */
async function readAnyReport(yearMonth, reportId) {
  const resp = await ctx.get(`/project/dailyReport/monthly?yearMonth=${yearMonth}`, { headers });
  const body = await resp.json();
  return (body.data || []).find((r) => Number(r.reportId) === reportId);
}

test('Issue#13｜删除他人的日报被拒绝，该日报与其全部明细原样存活', async () => {
  const OTHERS_REPORT = 991003;   // user_id=2 'ry' 的日报，见造数脚本

  const before = await readAnyReport('2026-07', OTHERS_REPORT);
  expect(before, '造数应包含一条属于他人（user_id=2）的日报').toBeTruthy();
  const detailsBefore = (before.detailList || []).length;
  expect(detailsBefore, '他人日报应有 2 条明细').toBe(2);

  const resp = await ctx.delete(`/project/dailyReport/${OTHERS_REPORT}`, { headers });
  const body = await resp.json();
  expect(body.code, '删他人日报必须被拒绝（Issue #13）').toBe(500);
  expect(body.msg, `提示应说明只能删本人的，实际：${body.msg}`).toContain('本人');

  const after = await readAnyReport('2026-07', OTHERS_REPORT);
  expect(after, '主记录必须原样存活（pm_daily_report 是硬删除，删掉就只能靠付费解冻备份）').toBeTruthy();
  expect((after.detailList || []).length,
    '两条明细都在 admin 的作用范围内——仅靠 015 的范围裁剪会被删掉，只有 user_id 限定拦得住').toBe(detailsBefore);
  expect(Number(after.totalWorkHours), '他人日报的汇总工时不得被改写').toBe(Number(before.totalWorkHours));
});

test('Issue#13｜混合批次（自己的 + 他人的）整批拒绝，自己的那条也不得被删', async () => {
  const MY_REPORT = 991000;       // admin 自己的（2026-07-20）
  const OTHERS_REPORT = 991003;   // 他人的

  const myBefore = await readDay('2026-07-20');
  expect(myBefore.reportId, '自己的日报应存在').toBeTruthy();
  const myDetailsBefore = (myBefore.detailList || []).length;

  // Spring 会把 @PathVariable Long[] 按逗号拆开——攻击可以混在一次合法自删里
  const resp = await ctx.delete(`/project/dailyReport/${MY_REPORT},${OTHERS_REPORT}`, { headers });
  const body = await resp.json();
  expect(body.code, '混合批次必须整批拒绝').toBe(500);

  const myAfter = await readDay('2026-07-20');
  expect(myAfter.reportId, '整批拒绝 = 不得部分执行，自己的那条也应保持原样').toBeTruthy();
  expect((myAfter.detailList || []).length).toBe(myDetailsBefore);
  expect(await readAnyReport('2026-07', OTHERS_REPORT), '他人的那条更不能动').toBeTruthy();
});

test('Issue#13｜删除不存在的 reportId 按幂等放行（重复点击 / 过期页面不得报错）', async () => {
  // 这条守着归属校验刻意选择的失败模式：「查不到」= no-op 成功，「查得到但属他人」= 500。
  // 若有人把 selectReportIdsNotOwnedBy 改成 not exists / left join 之类让「查不到」也算越权，
  // 每一次重复点击删除都会变成 500「只能删除本人的日报」，而单测（Mapper 全 mock）发现不了。
  const GONE = 991900;   // 造数号段内，但从不创建

  const resp = await ctx.delete(`/project/dailyReport/${GONE}`, { headers });
  const body = await resp.json();
  expect(body.code, '不存在的日报应按幂等 no-op 返回成功，而不是当成越权').toBe(200);
});

test('Issue#13 读侧｜按 ID 直读他人日报拿不到内容，读自己的不受影响', async () => {
  // GET /project/dailyReport/{reportId} 既无 user_id 也无 @DataScope（入参不是 BaseEntity），
  // 而 report_id 连续自增 —— 不限定就等于开放「按 ID 遍历全公司日报正文」。
  const OTHERS_REPORT = 991003;
  const MY_REPORT = 991000;

  const others = await (await ctx.get(`/project/dailyReport/${OTHERS_REPORT}`, { headers })).json();
  expect(others.data ?? null, '他人日报正文不得被按 ID 直读').toBeNull();

  const mine = await (await ctx.get(`/project/dailyReport/${MY_REPORT}`, { headers })).json();
  expect(mine.data, '本人日报仍应能按 ID 读到（不要过度收紧）').toBeTruthy();
  expect(Number(mine.data.reportId)).toBe(MY_REPORT);
});

// ===========================================================================
// User Story 2：阻止把工时填到无关项目上
// ===========================================================================

test('US2｜提交从未参与过的项目被拒绝，提示含项目名，且该项目实际人天不变', async () => {
  const workloadBefore = await actualWorkloadOf(P_STRANGER);

  const res = await saveDay('2026-07-26', [
    { projectId: P_STRANGER, entryType: 'work', workHours: 8, workContent: '越权尝试' },
  ]);

  expect(res.code).toBe(500);
  expect(res.msg, '提示须指明被拒项目名称，让填报人无需联系管理员（FR-008 / SC-006）')
    .toContain('015无关项目C');
  expect(res.msg).toContain('不在您参与的项目范围内');

  expect(await actualWorkloadOf(P_STRANGER), 'SC-002：跨项目提交不得改变目标项目人天')
    .toBe(workloadBefore);

  const day = await readDay('2026-07-26');
  expect((day.detailList || []).length, '不得产生任何明细').toBe(0);
});

test('US2｜混合提交（合法+非法）整次拒绝，合法部分也不得写入', async () => {
  const DATE = '2026-07-20';
  const activeBefore = await detailOf(DATE, P_ACTIVE);
  const closedBefore = await detailOf(DATE, P_CLOSED);

  const res = await saveDay(DATE, [
    { projectId: P_ACTIVE, entryType: 'work', workHours: 7, workContent: '合法部分' },
    { projectId: P_STRANGER, entryType: 'work', workHours: 1, workContent: '非法部分' },
  ]);
  expect(res.code).toBe(500);

  // FR-009 / INV-1：拒绝后数据库状态与请求前完全一致
  const activeAfter = await detailOf(DATE, P_ACTIVE);
  expect(activeAfter?.workHours ?? null).toBe(activeBefore?.workHours ?? null);

  // spec US1 场景 7：拒绝路径不得触发任何删除，不可见的合法工时必须仍然存在
  const closedAfter = await detailOf(DATE, P_CLOSED);
  expect(closedAfter, '拒绝必须发生在任何写操作之前').toBeTruthy();
  expect(Number(closedAfter.workHours)).toBe(Number(closedBefore.workHours));
});

// ===========================================================================
// User Story 3：项目结项后不再接受新增或变更工时
// ===========================================================================

test('US3｜为已结项项目新增工时被拒绝', async () => {
  const res = await saveDay('2026-07-27', [
    { projectId: P_CLOSED, entryType: 'work', workHours: 8, workContent: '给已结项项目加工时' },
  ]);

  expect(res.code).toBe(500);
  expect(res.msg).toContain('015已结项项目B');
  expect(res.msg).toContain('已结项');
});

test('US3｜交叉护栏：当天存在已结项工时但本次不提交它，须保存成功且原样保留', async () => {
  const DATE = '2026-07-20';
  const closedBefore = await detailOf(DATE, P_CLOSED);
  expect(closedBefore).toBeTruthy();

  // 校验只看「本次提交的内容」，不看「该日既有明细」。
  // 若误把既有的已结项工时也纳入校验，US3 会把 US1 要保护的场景整个拒掉——修复变成新 bug。
  const res = await saveDay(DATE, [
    { projectId: P_ACTIVE, entryType: 'work', workHours: 3, workContent: '只改在建项目' },
  ]);
  expect(res.code, '不得因该日存在已结项工时而拒绝整次保存').toBe(200);

  const closedAfter = await detailOf(DATE, P_CLOSED);
  expect(closedAfter).toBeTruthy();
  expect(Number(closedAfter.workHours)).toBe(Number(closedBefore.workHours));
});

// ===========================================================================
// User Story 4：离场成员的历史日报仍可维护
// ===========================================================================

test('US4｜离场成员可维护历史工时，而从未参与者被拒 —— 两者结果必须不同', async () => {
  const former = await saveDay('2026-07-25', [
    { projectId: P_FORMER, entryType: 'work', workHours: 5, workContent: '离场成员维护历史工时' },
  ]);
  expect(former.code, '若此处失败，说明 selectEverMemberProjectIds 误加了 is_active 过滤（SC-007）')
    .toBe(200);

  const stranger = await saveDay('2026-07-26', [
    { projectId: P_STRANGER, entryType: 'work', workHours: 5, workContent: '从未参与' },
  ]);
  expect(stranger.code, '「曾参与」与「从未参与」必须区别对待').toBe(500);
});

// ===========================================================================
// 读写口径同源：担任项目角色但成员表漏行
// ---------------------------------------------------------------------------
// 015 初版的 V1 只认 pm_project_member，而填报页右侧的项目列表
// （myProjects → selectProjectsByUserId）的 OR 列表还含 project_manager_id /
// market_manager_id / team_leader_id / FIND_IN_SET(participants)。
// 成员表漏同步的历史项目于是「列得出、填不了」；更糟的是填写页会把该项目当日既有工时
// 回填成 > 0 一并提交，于是该填报人当日整张日报永久保存不了。
// 修法：V1 补上凭据②（ProjectMapper.selectProjectRoleProjectIds），与读侧
// applyProjectScopeBypass（Issue #24）同源。
// ===========================================================================

test('口径同源｜担任项目角色但成员表无行，保存必须放行（读侧已放行，写侧不得自相矛盾）', async () => {
  const res = await saveDay('2026-07-28', [
    { projectId: P_ROLE_ONLY, entryType: 'work', workHours: 6, workContent: '市场经理填自己项目的工时' },
  ]);
  expect(res.code,
    '990500 上 admin 是 market_manager_id 但 pm_project_member 无行。'
    + 'myProjects 会列出它 → 写侧必须能保存，否则该账号当日整张日报永久保存不了。'
    + `实际返回：${res.code} / ${res.msg}`)
    .toBe(200);

  const detail = await detailOf('2026-07-28', P_ROLE_ONLY);
  expect(detail, '放行后明细必须真的落库').toBeTruthy();
  expect(Number(detail.workHours)).toBe(6);
});

test('口径同源｜护栏：放宽到项目角色后，「毫无关系」仍须被拒（不得顺手放开一片）', async () => {
  const res = await saveDay('2026-07-29', [
    { projectId: P_STRANGER, entryType: 'work', workHours: 6, workContent: '既非成员也无任何角色' },
  ]);
  expect(res.code,
    '990300 的 project_manager_id=2、其余角色列与 participants 全为 NULL —— '
    + '放宽 V1 后它仍必须被拒。若此处变绿，说明凭据②的 OR 列表写宽了。')
    .toBe(500);
});

// ===========================================================================
// 对账（SC-008 / SC-010）不在本 spec 内
// ---------------------------------------------------------------------------
// 「所有项目实际人天 == 其明细汇总」与「不存在孤立明细 / 汇总与明细不符」这两条，
// 需要跨全表聚合，HTTP 接口给不出来。以 SQL 形式记录在
// specs/015-daily-report-ownership-check/quickstart.md，跑完本 spec 后直连库执行。
// 此处不写占位用例——一个永远通过的断言只会制造覆盖率的错觉。
// ===========================================================================
