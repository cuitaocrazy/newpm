/**
 * E2E：日报保存的工时保护与项目归属校验（specs/015-daily-report-ownership-check）
 *
 * 为什么必须有 e2e：本特性有 4 条不变式是纯数据库层的——
 * 「被保留的明细字段逐字未变」靠的是「范围外的行压根没被 touch」，
 * 而服务层单测把 Mapper 全 mock 掉了，无从验证。凡是断言「某个东西没有被改动」的，
 * 单测通常无能为力。返回码语义（见「删除后仍返回成功」一例）同样只有真实调用才暴露得出来。
 *
 * 前置条件：
 *   1. 后端已启动（默认 http://localhost:8085，用 E2E_API_URL 覆盖）
 *   2. 已执行造数：tests/fixtures/015-daily-report-ownership-seed.sql
 *   3. 验证码已关闭：UPDATE sys_config SET config_value='false'
 *                    WHERE config_key='sys.account.captchaEnabled';
 *   4. admin 密码用 E2E_ADMIN_PASSWORD 指定（全新导入的库是 admin123，
 *      长期库通常是 123456789）
 *
 * 运行：
 *   E2E_API_URL=http://localhost:8085 E2E_ADMIN_PASSWORD=admin123 \
 *     npx playwright test e2e-daily-report-ownership.spec.js
 *
 * 本 spec 直连后端，不经前端 dev server —— 因此不使用 tests/helpers/api-client.js
 * （后者带 /dev-api 前缀，依赖 vite 代理）。
 */

import { test, expect, request as playwrightRequest } from '@playwright/test';

const API = process.env.E2E_API_URL || 'http://localhost:8085';
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD || '123456789';

// 造数约定的固定 ID，见 tests/fixtures/015-daily-report-ownership-seed.sql
const P_ACTIVE = 100;    // 在建 + admin 是成员    → 可见，作用范围内
const P_CLOSED = 200;    // 已结项 + admin 是成员  → 不可见，作用范围外
const P_STRANGER = 300;  // 在建，admin 从未参与   → 归属校验须拒绝
const P_FORMER = 400;    // 在建，admin 已离场     → 归属校验须放行

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
// 对账（SC-008 / SC-010）不在本 spec 内
// ---------------------------------------------------------------------------
// 「所有项目实际人天 == 其明细汇总」与「不存在孤立明细 / 汇总与明细不符」这两条，
// 需要跨全表聚合，HTTP 接口给不出来。以 SQL 形式记录在
// specs/015-daily-report-ownership-check/quickstart.md，跑完本 spec 后直连库执行。
// 此处不写占位用例——一个永远通过的断言只会制造覆盖率的错觉。
// ===========================================================================
