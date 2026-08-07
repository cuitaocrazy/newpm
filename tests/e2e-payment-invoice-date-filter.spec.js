/**
 * Issue #36 — 付款里程碑查询区新增「开票日期」区间条件（BDD / E2E）
 *
 * ── 一句话业务 ──
 * 财务在「合同款项 → 付款里程碑」页按发票开出的日期核对台账：给定一段开票期间，
 * 只看这段期间内开出的发票，列表、底部合计金额、导出三处口径必须一致。
 *
 * ── 关键事实（实现约定，测试按此断言，不要"顺手改名"）──
 * 1. 「开票日期」的物理列是 pm_payment.submit_acceptance_date
 *    （历史列名「提交验收日期」，业务含义改为开票日期但列名未改；
 *     pm-sql/init/00_tables_ddl.sql:714 该列的 COMMENT 就是「开票日期」）。
 *    库里不存在 invoice_date 之类的列，本需求零 DDL 变更。
 * 2. 前后端契约参数名固定为 submitAcceptanceDateStart / submitAcceptanceDateEnd。
 * 3. 受影响的三个后端出口：
 *      GET  /project/payment/listWithContracts   （列表）
 *      GET  /project/payment/sumPaymentAmount    （底部合计，最易漏实现的一处）
 *      POST /project/payment/export              （导出）
 *    以及扁平款项清单 GET /project/payment/list（走 params[...] 通道）。
 *
 * ── 数据策略：全部自造、跑完自删，不依赖库里已有数据的内容 ──
 * beforeAll 建 1 个带唯一标记的合同 + 6 条付款里程碑（金额取 2 的幂，任意子集和唯一，
 * 因此"合计金额"能反推出命中集合），其中一条 submit_acceptance_date 为 NULL（尚未开票）。
 * 所有断言都只针对这 6 条自造数据（查询一律带 contractName=<唯一合同名> 收敛范围），
 * 绝不断言全库行数。afterAll 逐条删付款里程碑 + 删合同。
 *
 * ── 运行前置 ──
 * - 后端 + 前端（默认 http://localhost:80，可用 E2E_BASE_URL 覆盖）均已启动
 * - UI 用例需要临时关闭登录验证码（sys.account.captchaEnabled=false），与本仓库其他 UI 套件一致
 * - 【禁止 import.meta】：根 package.json 无 type:module，Playwright 对 .js 走 ESM→CJS 转译，
 *   含 import.meta 的文件无法转译，会报误导性的 "require is not defined in ES module scope"
 */

import { test, expect, request as playwrightRequest } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:80';

// ─────────────────────────────────────────────────────────────
// 造数定义
// ─────────────────────────────────────────────────────────────

/** 本次运行的唯一标记：合同名 / 合同编号 / 付款方式名称都带上它，保证与库里既有数据不串 */
const TAG = `INV${Date.now()}`;
const CONTRACT_NAME = `E2E开票日期合同_${TAG}`;
const CONTRACT_CODE = `E2E-INV-${TAG}`;

/** 被测区间：2026 年 3 月（财务按月核对的典型口径） */
const MARCH_START = '2026-03-01';
const MARCH_END = '2026-03-31';

/**
 * 6 条付款里程碑。
 * 金额刻意取 2 的幂 —— 任意子集之和唯一，sumPaymentAmount 的结果能反推命中集合，
 * 不需要额外查询就能证明"合计跟着筛选条件一起变"。
 */
const SEED_PAYMENTS = [
  { key: 'A', invoiceDate: '2026-03-01', actualPaymentDate: null, amount: 100, desc: '开票日 == 区间起始日（左端点）' },
  { key: 'B', invoiceDate: '2026-03-15', actualPaymentDate: null, amount: 200, desc: '开票日在区间正中' },
  { key: 'C', invoiceDate: '2026-03-31', actualPaymentDate: null, amount: 400, desc: '开票日 == 区间结束日（右端点）' },
  { key: 'D', invoiceDate: '2026-04-01', actualPaymentDate: null, amount: 800, desc: '开票日在区间外（右侧相邻一天）' },
  { key: 'E', invoiceDate: null, actualPaymentDate: null, amount: 1600, desc: '尚未开票（submit_acceptance_date 为 NULL）' },
  { key: 'F', invoiceDate: '2026-03-15', actualPaymentDate: '2026-05-20', amount: 3200, desc: '3月开票 + 5月回款，用于验证与实际回款日期的 AND' },
];

const AMOUNT_OF = Object.fromEntries(SEED_PAYMENTS.map(p => [p.key, p.amount]));
const ALL_KEYS = SEED_PAYMENTS.map(p => p.key).sort();
/** 命中集合 → 期望合计金额 */
const sumOf = (keys) => keys.reduce((acc, k) => acc + AMOUNT_OF[k], 0);

/** 付款方式名称：`${TAG}-A` … `${TAG}-F`，反解出 key 用于集合断言 */
const nameOf = (key) => `${TAG}-${key}`;

let api;                       // 已登录的 API 客户端（tests/helpers/api-client.js）
let rawCtx;                    // 裸 APIRequestContext：导出返回二进制，不能走 api.post 的 resp.json()
let contractId = null;
let contractDeptId = null;     // 合同所属团队（组合筛选用；为空则相关用例显式 skip）
let paymentStatusValue = null; // 取自字典 sys_fkzt 的真实值（trim 后使用，避开脏数据坑）
const createdPaymentIds = [];

// ─────────────────────────────────────────────────────────────
// 查询工具：所有查询都带 contractName，把范围收敛到自造数据
// ─────────────────────────────────────────────────────────────

/** 列表（合同+里程碑聚合）。返回命中的里程碑 key 集合（已排序） */
async function listKeys(extra = {}) {
  const res = await api.get('/project/payment/listWithContracts', {
    pageNum: 1, pageSize: 100, contractName: CONTRACT_NAME, ...extra,
  });
  expect(res.code, `listWithContracts 应返回 200，实际：${JSON.stringify(res).slice(0, 300)}`).toBe(200);
  expect(res.rows, 'rows 应为数组').toBeInstanceOf(Array);
  const keys = [];
  for (const row of res.rows) {
    for (const p of row.paymentList || []) {
      const name = p.paymentMethodName || '';
      if (name.startsWith(`${TAG}-`)) keys.push(name.slice(TAG.length + 1));
    }
  }
  return keys.sort();
}

/** 底部合计金额 */
async function sumAmount(extra = {}) {
  const res = await api.get('/project/payment/sumPaymentAmount', { contractName: CONTRACT_NAME, ...extra });
  expect(res.code, `sumPaymentAmount 应返回 200，实际：${JSON.stringify(res).slice(0, 300)}`).toBe(200);
  return Number(res.data);
}

/**
 * 扁平款项清单（另一条出口，SQL 在 PaymentMapper.selectPaymentList，
 * 日期条件走 BaseEntity.params 通道 → 参数形如 params[submitAcceptanceDateStart]）
 */
async function flatListKeys(params = {}) {
  const query = { pageNum: 1, pageSize: 100, contractId };
  for (const [k, v] of Object.entries(params)) query[`params[${k}]`] = v;
  const res = await api.get('/project/payment/list', query);
  expect(res.code, `/project/payment/list 应返回 200，实际：${JSON.stringify(res).slice(0, 300)}`).toBe(200);
  return (res.rows || [])
    .map(p => p.paymentMethodName || '')
    .filter(n => n.startsWith(`${TAG}-`))
    .map(n => n.slice(TAG.length + 1))
    .sort();
}

/** 导出（form-urlencoded，与前端 proxy.download 的发法一致），返回 {status, contentType, bytes} */
async function exportBytes(extra = {}) {
  const resp = await rawCtx.post('/dev-api/project/payment/export', {
    headers: { Authorization: `Bearer ${api.token}` },
    form: { contractName: CONTRACT_NAME, ...extra },
  });
  const body = await resp.body();
  return { status: resp.status(), contentType: resp.headers()['content-type'] || '', bytes: body.length };
}

// ─────────────────────────────────────────────────────────────
// UI 工具（手法照抄 tests/e2e-sort-state-persist.spec.js，保持同一套约定）
// ─────────────────────────────────────────────────────────────

const PAYMENT_PATH = '/htkx/payment';
const LIST_URL = '/project/payment/listWithContracts';
const STORAGE_KEY = 'payment_search_state';

async function login(page, username = 'admin', password = '123456789') {
  await page.goto(BASE_URL);
  await page.fill('input[placeholder="账号"]', username);
  await page.fill('input[placeholder="密码"]', password);
  await page.locator('button.el-button--primary').click();
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 });
  const cancelBtn = page.locator('.el-message-box .el-button--default');
  if (await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    await cancelBtn.click();
  }
}

/** 收集列表请求 URL（已 decode），用于断言"条件真的上行了" */
function trackRequests(page, urlFragment) {
  const urls = [];
  page.on('request', r => {
    const u = r.url();
    if (u.includes(urlFragment)) urls.push(decodeURIComponent(u));
  });
  return {
    last: () => (urls.length ? urls[urls.length - 1] : null),
    clear: () => { urls.length = 0; },
  };
}

/** 取单个查询参数；取不到返回 null（前端 tansParams 会丢掉 null/空串，故"取不到"即"没传"） */
function paramOf(url, name) {
  if (!url) return null;
  const m = decodeURIComponent(url).match(new RegExp(`[?&]${name}=([^&]*)`));
  return m ? m[1] : null;
}

const formItem = (page, label) =>
  page.locator('.el-form-item').filter({ hasText: label }).first();

async function waitTableReady(page) {
  await page.waitForSelector('.el-table', { timeout: 25000 });
  await page.waitForFunction(
    () => document.querySelectorAll('.el-table__body tr').length > 0,
    undefined,
    { timeout: 25000 }
  );
}

async function gotoList(page) {
  await page.goto(`${BASE_URL}${PAYMENT_PATH}`);
  await waitTableReady(page);
}

/** 展开「更多」折叠区（开票日期与实际回款日期都在里面） */
async function expandMore(page) {
  const moreBtn = page.locator('.el-form button', { hasText: '更多' }).first();
  if (await moreBtn.isVisible().catch(() => false)) {
    await moreBtn.click();
  }
  await expect(formItem(page, '开票日期'), '点「更多」后折叠区应展开并出现「开票日期」').toBeVisible();
}

/** 填某个 daterange 控件（value-format YYYY-MM-DD） */
async function fillDateRange(page, label, start, end) {
  const inputs = formItem(page, label).locator('.el-range-input');
  await inputs.nth(0).fill(start);
  await inputs.nth(0).press('Enter');
  await inputs.nth(1).fill(end);
  await inputs.nth(1).press('Enter');
  await page.keyboard.press('Escape');
}

/** 点「查询」并等这次列表请求回来 */
async function runQuery(page) {
  const resp = page.waitForResponse(
    r => r.url().includes(LIST_URL) && r.request().method() === 'GET', { timeout: 25000 });
  await page.locator('.el-form button', { hasText: '查询' }).first().click();
  await resp;
  await waitTableReady(page);
}

// ─────────────────────────────────────────────────────────────
// 造数 / 清理
// ─────────────────────────────────────────────────────────────

async function seed() {
  // 合同所属团队：取当前登录用户的部门，让"开票期间 + 合同所属团队"组合筛选有真实取值
  let deptId = null;
  try {
    const info = await api.get('/getInfo');
    deptId = info?.user?.deptId ?? null;
  } catch { /* 拿不到就算了，相关用例会 skip */ }

  const customerList = await api.get('/project/customer/list', { pageNum: 1, pageSize: 1 });
  const customerId = customerList.rows?.length ? customerList.rows[0].customerId : null;

  const addRes = await api.post('/project/contract', {
    contractName: CONTRACT_NAME,
    contractCode: CONTRACT_CODE,
    contractType: '1',
    contractStatus: '0',
    contractAmount: sumOf(ALL_KEYS),
    customerId,
    deptId,
    projectIds: [],
  });
  expect(addRes.code, `造数：新增合同应成功，实际：${JSON.stringify(addRes)}`).toBe(200);

  const listRes = await api.get('/project/contract/list', { pageNum: 1, pageSize: 10, contractName: CONTRACT_NAME });
  const created = (listRes.rows || []).find(r => r.contractCode === CONTRACT_CODE);
  expect(created, `造数：应能按编号 ${CONTRACT_CODE} 找回刚建的合同`).toBeTruthy();
  contractId = created.contractId;
  contractDeptId = created.deptId ?? null;

  // 付款状态取自字典 sys_fkzt。
  // 【坑】库里历史脏数据让部分状态值带 Tab（见 tests/payment-status-filter-regression.spec.js），
  // 而查询参数会被 BaseController 的 StringTrimmerEditor trim 掉 —— 造数时就写 trim 后的值，
  // 保证"写进去的"和"查得到的"是同一个串，本用例才不会被那条历史脏数据带偏。
  try {
    const dict = await api.get('/system/dict/data/type/sys_fkzt');
    const first = (dict.data || []).map(d => (d.dictValue || '').trim()).find(v => v);
    if (first) paymentStatusValue = first;
  } catch { /* 字典取不到，相关用例会 skip */ }

  for (const p of SEED_PAYMENTS) {
    const payload = {
      contractId,
      paymentMethodName: nameOf(p.key),
      paymentAmount: p.amount,
      submitAcceptanceDate: p.invoiceDate,     // null 即"尚未开票"
      actualPaymentDate: p.actualPaymentDate,
      remark: p.desc,
    };
    // B（区间内）与 D（区间外）带上付款状态，用于"开票期间 + 付款状态"组合筛选
    if (paymentStatusValue && (p.key === 'B' || p.key === 'D')) {
      payload.paymentStatus = paymentStatusValue;
    }
    const res = await api.post('/project/payment', payload);
    expect(res.code, `造数：新增里程碑 ${nameOf(p.key)} 应成功，实际：${JSON.stringify(res)}`).toBe(200);
  }

  // 回读拿 paymentId（清理用），同时校验开票日期确实落库
  const flat = await api.get('/project/payment/list', { pageNum: 1, pageSize: 100, contractId });
  expect(flat.code).toBe(200);
  const mine = (flat.rows || []).filter(r => (r.paymentMethodName || '').startsWith(`${TAG}-`));
  expect(mine.length, `造数：应能回读到 ${SEED_PAYMENTS.length} 条自造里程碑，实际 ${mine.length} 条`)
    .toBe(SEED_PAYMENTS.length);
  for (const r of mine) createdPaymentIds.push(r.paymentId);

  const byKey = Object.fromEntries(mine.map(r => [r.paymentMethodName.slice(TAG.length + 1), r]));
  for (const p of SEED_PAYMENTS) {
    const got = byKey[p.key];
    const gotDate = got.submitAcceptanceDate ? String(got.submitAcceptanceDate).slice(0, 10) : null;
    expect(gotDate,
      `造数前提：${nameOf(p.key)} 的开票日期应为 ${p.invoiceDate}（${p.desc}），实际 ${gotDate}`)
      .toBe(p.invoiceDate);
  }
  console.log(`🌱 造数完成：合同 ${contractId}（${CONTRACT_NAME}），里程碑 ${createdPaymentIds.join(',')}`);
}

async function cleanup() {
  for (const id of createdPaymentIds) {
    try {
      // 【注意】DELETE /project/payment/{paymentIds:\d+} 的路径正则是 \d+，逗号串不匹配该映射，
      // 只能逐条删；批量删会 404 并静默留下脏数据。
      await api.del(`/project/payment/${id}`);
    } catch { /* 已删或不可达，忽略 */ }
  }
  createdPaymentIds.length = 0;
  if (contractId) {
    try { await api.del(`/project/contract/${contractId}`); } catch { /* 忽略 */ }
    contractId = null;
  }
  console.log('🧹 清理完成：自造的合同与付款里程碑已删除');
}

// ═════════════════════════════════════════════════════════════
// 用例
// ═════════════════════════════════════════════════════════════
test.describe.serial('Issue #36 付款里程碑「开票日期」区间筛选', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    rawCtx = await playwrightRequest.newContext({ baseURL: BASE_URL });
    await seed();
  });

  test.afterAll(async () => {
    await cleanup();
    await rawCtx?.dispose();
    await api?.dispose();
  });

  // ───────────────────────────────────────────
  // 基线：不填开票日期，台账与改造前一模一样
  // BDD: 不填开票日期时，台账与本次改造前一模一样（SC-012 / FR-013）
  // ───────────────────────────────────────────
  test('不填开票日期：6 条里程碑（含未开票那条）全部在列表里，合计为全量', async () => {
    expect(await listKeys(), '不带任何开票日期条件时，自造的 6 条里程碑应全部出现')
      .toEqual(ALL_KEYS);
    expect(await sumAmount(), '不带条件时合计应为 6 条之和').toBeCloseTo(sumOf(ALL_KEYS), 2);
  });

  test('开票日期传空串等同于不传：条件不生效，台账保持全量', async () => {
    // 后端 <if test="... != null and ... != ''"> 的第二个判断守的就是这条：
    // 前端把区间清空后可能传空串，不能因此把台账筛成 0 条。
    expect(await listKeys({ submitAcceptanceDateStart: '', submitAcceptanceDateEnd: '' }),
      '空串条件不应参与过滤').toEqual(ALL_KEYS);
    expect(await sumAmount({ submitAcceptanceDateStart: '', submitAcceptanceDateEnd: '' }),
      '空串条件下合计应仍为全量').toBeCloseTo(sumOf(ALL_KEYS), 2);
  });

  // ───────────────────────────────────────────
  // 完整区间 / 区间外
  // BDD: 财务按月核对：只看 2026 年 3 月开出的发票（SC-003 / FR-003+FR-004）
  // ───────────────────────────────────────────
  test('完整区间命中：2026-03-01 ~ 2026-03-31 只筛出 3 月开票的 A/B/C/F', async () => {
    const keys = await listKeys({
      submitAcceptanceDateStart: MARCH_START,
      submitAcceptanceDateEnd: MARCH_END,
    });
    expect(keys,
      `3 月区间应命中 A(3/1)、B(3/15)、C(3/31)、F(3/15)；` +
      `D(4/1 区间外) 与 E(未开票) 不得出现。实际命中：${JSON.stringify(keys)}`)
      .toEqual(['A', 'B', 'C', 'F']);
  });

  test('区间外的里程碑不命中：4 月 1 日开票的 D 被 3 月区间挡在外面', async () => {
    const keys = await listKeys({
      submitAcceptanceDateStart: MARCH_START,
      submitAcceptanceDateEnd: MARCH_END,
    });
    expect(keys, 'D 的开票日是 2026-04-01，只差一天也必须被排除').not.toContain('D');

    // 反向再证一次：把区间挪到 4 月，只剩 D
    const aprilKeys = await listKeys({
      submitAcceptanceDateStart: '2026-04-01',
      submitAcceptanceDateEnd: '2026-04-30',
    });
    expect(aprilKeys, '4 月区间应只命中 D').toEqual(['D']);
  });

  // ───────────────────────────────────────────
  // 端点包含（场景大纲 2 例）
  // BDD: 区间两端都算数——恰好等于起止日期的发票必须被查出来（SC-009 / FR-003）
  // ───────────────────────────────────────────
  for (const c of [
    { title: '恰好等于起始日', start: MARCH_START, end: MARCH_END, mustHave: 'A', why: 'A 的开票日 == 起始日 2026-03-01' },
    { title: '恰好等于结束日', start: MARCH_START, end: MARCH_END, mustHave: 'C', why: 'C 的开票日 == 结束日 2026-03-31' },
  ]) {
    test(`区间两端都算数（${c.title}）：${c.mustHave} 必须被查出来`, async () => {
      const keys = await listKeys({ submitAcceptanceDateStart: c.start, submitAcceptanceDateEnd: c.end });
      expect(keys,
        `${c.why}；区间必须是闭区间（>= start 且 <= end），实际命中：${JSON.stringify(keys)}`)
        .toContain(c.mustHave);
    });
  }

  // ───────────────────────────────────────────
  // 单边条件
  // BDD: 只填开始日期（SC-006 / FR-008）、只填结束日期（SC-007 / FR-009）
  // ───────────────────────────────────────────
  test('只填开始日期：2026-03-31 起开出的发票全都要（C、D），往后不设限', async () => {
    const keys = await listKeys({ submitAcceptanceDateStart: '2026-03-31' });
    expect(keys,
      `只给起始日时不应有右边界：C(3/31，含端点) 与 D(4/1) 都要在内，` +
      `A(3/1)、B(3/15) 在起始日之前要排除。实际命中：${JSON.stringify(keys)}`)
      .toEqual(['C', 'D']);
    expect(await sumAmount({ submitAcceptanceDateStart: '2026-03-31' }),
      '合计应等于 C+D').toBeCloseTo(sumOf(['C', 'D']), 2);
  });

  test('只填结束日期：2026-03-01 之前开出的发票全都要（A），往前不设限', async () => {
    const keys = await listKeys({ submitAcceptanceDateEnd: '2026-03-01' });
    expect(keys,
      `只给结束日时不应有左边界：A(3/1，含端点) 在内，其余更晚的都排除。` +
      `实际命中：${JSON.stringify(keys)}`)
      .toEqual(['A']);
    expect(await sumAmount({ submitAcceptanceDateEnd: '2026-03-01' }),
      '合计应等于 A').toBeCloseTo(sumOf(['A']), 2);
  });

  // ───────────────────────────────────────────
  // BDD: 起止是同一天：精确筛出这一天开出的发票（SC-008 / FR-010）
  // ───────────────────────────────────────────
  test('起止同一天（2026-03-15）：精确筛出这天开票的 B 与 F', async () => {
    const keys = await listKeys({
      submitAcceptanceDateStart: '2026-03-15',
      submitAcceptanceDateEnd: '2026-03-15',
    });
    expect(keys,
      `同一天的区间应是"恰好这一天"，B 与 F 都是 3/15。实际命中：${JSON.stringify(keys)}`)
      .toEqual(['B', 'F']);
    expect(await sumAmount({ submitAcceptanceDateStart: '2026-03-15', submitAcceptanceDateEnd: '2026-03-15' }),
      '合计应等于 B+F').toBeCloseTo(sumOf(['B', 'F']), 2);
  });

  // ───────────────────────────────────────────
  // 未开票（NULL）在任何开票期间条件下都不出现（场景大纲 4 例）
  // BDD: 尚未开票的里程碑，在任何开票期间查询里都不出现（SC-011 / FR-012）
  // ───────────────────────────────────────────
  for (const c of [
    { title: '完整区间', params: { submitAcceptanceDateStart: MARCH_START, submitAcceptanceDateEnd: MARCH_END } },
    { title: '仅开始日期', params: { submitAcceptanceDateStart: '2000-01-01' } },
    { title: '仅结束日期', params: { submitAcceptanceDateEnd: '2099-12-31' } },
    { title: '同一天', params: { submitAcceptanceDateStart: '2026-03-15', submitAcceptanceDateEnd: '2026-03-15' } },
  ]) {
    test(`尚未开票的里程碑不出现（${c.title}）`, async () => {
      const keys = await listKeys(c.params);
      expect(keys,
        `E 的 submit_acceptance_date 为 NULL（尚未开票），任何开票期间条件下都不应出现。` +
        `条件=${JSON.stringify(c.params)}，实际命中：${JSON.stringify(keys)}`)
        .not.toContain('E');
      // 同时确认这个条件本身是"能筛出东西"的，否则 not.toContain 会因为空集而假绿
      expect(keys.length, `条件 ${JSON.stringify(c.params)} 至少应命中一条已开票的里程碑`)
        .toBeGreaterThan(0);
    });
  }

  test('未开票的里程碑在"不带开票条件"时仍然可见（证明它没被造数漏掉）', async () => {
    // 这条是上面 4 例的对照组：E 不是"根本不存在"，而是"被开票期间条件排除"
    expect(await listKeys(), '不带开票条件时 E 应可见').toContain('E');
  });

  // ───────────────────────────────────────────
  // BDD: 起始日期晚于结束日期：查不到东西，但不能报错（SC-010 / FR-011）
  // ───────────────────────────────────────────
  test('起始晚于结束（2026-04-01 ~ 2026-03-01）：返回空集且接口不报错', async () => {
    const res = await api.get('/project/payment/listWithContracts', {
      pageNum: 1, pageSize: 100, contractName: CONTRACT_NAME,
      submitAcceptanceDateStart: '2026-04-01',
      submitAcceptanceDateEnd: '2026-03-01',
    });
    expect(res.code, '倒置区间不得抛异常，应正常返回 200').toBe(200);

    const keys = await listKeys({
      submitAcceptanceDateStart: '2026-04-01',
      submitAcceptanceDateEnd: '2026-03-01',
    });
    expect(keys, `不存在同时 >=4/1 且 <=3/1 的开票日，应为空集。实际：${JSON.stringify(keys)}`)
      .toEqual([]);

    const sum = await sumAmount({ submitAcceptanceDateStart: '2026-04-01', submitAcceptanceDateEnd: '2026-03-01' });
    expect(sum, '空集的合计应为 0，而不是回落成全量').toBeCloseTo(0, 2);
  });

  // ───────────────────────────────────────────
  // 合计金额（最易漏实现的一处）
  // BDD: 底部合计金额跟着开票日期一起变，不再是全量（SC-004 / FR-005）
  // ───────────────────────────────────────────
  test('底部合计金额跟着开票日期一起变，不是全量', async () => {
    const all = await sumAmount();
    const march = await sumAmount({ submitAcceptanceDateStart: MARCH_START, submitAcceptanceDateEnd: MARCH_END });

    expect(all, '不带条件时合计 = A+B+C+D+E+F').toBeCloseTo(sumOf(ALL_KEYS), 2);
    expect(march,
      `3 月区间的合计应等于 A+B+C+F = ${sumOf(['A', 'B', 'C', 'F'])}；` +
      `若 sumPaymentAmount 漏加开票日期条件，这里会等于全量 ${sumOf(ALL_KEYS)}——` +
      `即"列表筛过了、底部合计还是全量"。实际 ${march}`)
      .toBeCloseTo(sumOf(['A', 'B', 'C', 'F']), 2);
    expect(march, '筛过之后的合计必须小于全量').toBeLessThan(all);
  });

  test('合计与列表口径完全一致：命中集合的金额之和 == sumPaymentAmount', async () => {
    // 金额取 2 的幂 → 子集和唯一，两边对得上就说明用的是同一套过滤条件
    for (const params of [
      { submitAcceptanceDateStart: MARCH_START, submitAcceptanceDateEnd: MARCH_END },
      { submitAcceptanceDateStart: '2026-03-31' },
      { submitAcceptanceDateEnd: '2026-03-01' },
      { submitAcceptanceDateStart: '2026-03-15', submitAcceptanceDateEnd: '2026-03-15' },
    ]) {
      const keys = await listKeys(params);
      const sum = await sumAmount(params);
      expect(sum,
        `条件 ${JSON.stringify(params)}：列表命中 ${JSON.stringify(keys)}，金额和应为 ${sumOf(keys)}，` +
        `合计接口却返回 ${sum} —— 两处 SQL 的过滤条件不一致`)
        .toBeCloseTo(sumOf(keys), 2);
    }
  });

  // ───────────────────────────────────────────
  // BDD: 导出拿到的和列表看到的是同一批数据（SC-005 / FR-006）
  // ───────────────────────────────────────────
  test('导出口径与列表一致：接口不报错且受开票日期条件影响', async () => {
    const all = await exportBytes();
    const march = await exportBytes({ submitAcceptanceDateStart: MARCH_START, submitAcceptanceDateEnd: MARCH_END });
    const none = await exportBytes({ submitAcceptanceDateStart: '2026-04-01', submitAcceptanceDateEnd: '2026-03-01' });

    for (const [label, r] of [['全量', all], ['3月区间', march], ['空集', none]]) {
      expect(r.status, `导出(${label}) 应返回 200，实际 ${r.status}`).toBe(200);
      expect(r.contentType,
        `导出(${label}) 应返回 Excel 二进制，实际 content-type=${r.contentType}`)
        .toMatch(/spreadsheetml|octet-stream|vnd\.ms-excel/);
      expect(r.bytes, `导出(${label}) 不应是空响应`).toBeGreaterThan(0);
    }

    // 无法在不引入 xlsx 解析库的前提下逐行比对，改用"行数越多字节越多"的单调性：
    // 空集 < 3月区间(4 条) < 全量(6 条)。若导出漏加开票日期条件，三者会完全相同。
    expect(march.bytes,
      `导出应受开票日期条件影响：空集(${none.bytes}B) 应小于 3 月区间(${march.bytes}B)；` +
      `相等说明 export 没把 submitAcceptanceDate* 传进查询`)
      .toBeGreaterThan(none.bytes);
    expect(all.bytes,
      `全量(${all.bytes}B) 应大于 3 月区间(${march.bytes}B)`)
      .toBeGreaterThan(march.bytes);
  });

  // ───────────────────────────────────────────
  // BDD: 系统里另一条款项清单出口，也按同一个开票期间口径过滤（FR-007）
  // ───────────────────────────────────────────
  test('扁平款项清单 /project/payment/list 也按同一口径过滤', async () => {
    expect(await flatListKeys(), '不带条件时应能看到全部 6 条').toEqual(ALL_KEYS);

    expect(await flatListKeys({ submitAcceptanceDateStart: MARCH_START, submitAcceptanceDateEnd: MARCH_END }),
      '扁平清单的 3 月区间应与聚合列表一致：A/B/C/F').toEqual(['A', 'B', 'C', 'F']);

    expect(await flatListKeys({ submitAcceptanceDateStart: '2026-03-31' }),
      '扁平清单只填开始日期应命中 C/D').toEqual(['C', 'D']);

    expect(await flatListKeys({ submitAcceptanceDateEnd: '2026-03-01' }),
      '扁平清单只填结束日期应命中 A').toEqual(['A']);

    expect(await flatListKeys({ submitAcceptanceDateStart: MARCH_START, submitAcceptanceDateEnd: MARCH_END }),
      '未开票的 E 在扁平清单里同样不得出现').not.toContain('E');
  });

  // ───────────────────────────────────────────
  // 组合条件
  // BDD: 开票期间与回款期间同时填：两个条件都得满足（SC-013 / FR-014 + INV-1）
  // ───────────────────────────────────────────
  test('开票期间 + 实际回款期间：两个条件是 AND，不是 OR', async () => {
    const both = await listKeys({
      submitAcceptanceDateStart: MARCH_START,
      submitAcceptanceDateEnd: MARCH_END,
      actualPaymentDateStart: '2026-05-01',
      actualPaymentDateEnd: '2026-05-31',
    });
    expect(both,
      `只有 F 同时满足"3 月开票"与"5 月回款"。若命中 A/B/C（只满足开票）说明两条件成了 OR，` +
      `或回款条件被开票条件覆盖。实际命中：${JSON.stringify(both)}`)
      .toEqual(['F']);

    // 交集为空的组合同样必须为空
    const empty = await listKeys({
      submitAcceptanceDateStart: '2026-04-01',
      submitAcceptanceDateEnd: '2026-04-30',
      actualPaymentDateStart: '2026-05-01',
      actualPaymentDateEnd: '2026-05-31',
    });
    expect(empty, '4 月开票 ∩ 5 月回款为空集').toEqual([]);

    expect(await sumAmount({
      submitAcceptanceDateStart: MARCH_START,
      submitAcceptanceDateEnd: MARCH_END,
      actualPaymentDateStart: '2026-05-01',
      actualPaymentDateEnd: '2026-05-31',
    }), '组合条件下的合计应只剩 F').toBeCloseTo(sumOf(['F']), 2);
  });

  // BDD: 开票期间 + 付款状态（SC-013 扩展 / FR-014）
  test('开票期间 + 付款状态：两个条件叠加生效', async () => {
    test.skip(!paymentStatusValue, '字典 sys_fkzt 取不到可用值，跳过"开票期间 + 付款状态"组合');

    const keys = await listKeys({
      submitAcceptanceDateStart: MARCH_START,
      submitAcceptanceDateEnd: MARCH_END,
      paymentStatuses: paymentStatusValue,
    });
    expect(keys,
      `造数时只有 B(3/15) 与 D(4/1) 带状态 ${paymentStatusValue}，` +
      `叠加 3 月区间后应只剩 B。实际命中：${JSON.stringify(keys)}`)
      .toEqual(['B']);

    // 对照：同一状态不加开票区间，B 与 D 都在
    const noDate = await listKeys({ paymentStatuses: paymentStatusValue });
    expect(noDate, '不加开票区间时该状态应命中 B 与 D').toEqual(['B', 'D']);
  });

  // BDD: 开票期间 + 合同所属团队（FR-014 / INV-7）
  test('开票期间 + 合同所属团队：两个条件叠加生效', async () => {
    test.skip(!contractDeptId, '自造合同没有 deptId，跳过"开票期间 + 合同所属团队"组合');

    const hit = await listKeys({
      submitAcceptanceDateStart: MARCH_START,
      submitAcceptanceDateEnd: MARCH_END,
      deptIds: contractDeptId,
    });
    expect(hit,
      `合同属于团队 ${contractDeptId}，叠加 3 月区间后应仍是 A/B/C/F。实际：${JSON.stringify(hit)}`)
      .toEqual(['A', 'B', 'C', 'F']);

    const miss = await listKeys({
      submitAcceptanceDateStart: MARCH_START,
      submitAcceptanceDateEnd: MARCH_END,
      deptIds: 99999999,
    });
    expect(miss, '换成一个不存在的团队后应为空集（证明团队条件确实参与了过滤）').toEqual([]);
  });

  // BDD: 开票期间 + 合同名称（SC-013 扩展 / FR-014）
  test('开票期间 + 合同名称：两个条件叠加生效', async () => {
    // 本套件所有查询都带 contractName=<唯一合同名>，这里正面证明它与开票区间是叠加关系
    const res = await api.get('/project/payment/listWithContracts', {
      pageNum: 1, pageSize: 100,
      contractName: `${CONTRACT_NAME}_不存在的后缀`,
      submitAcceptanceDateStart: MARCH_START,
      submitAcceptanceDateEnd: MARCH_END,
    });
    expect(res.code).toBe(200);
    const keys = (res.rows || []).flatMap(r => (r.paymentList || []))
      .map(p => p.paymentMethodName || '')
      .filter(n => n.startsWith(`${TAG}-`));
    expect(keys, '合同名对不上时，即使开票区间命中也不应返回自造数据').toEqual([]);

    // 合同名对得上时才有数据
    expect(await listKeys({ submitAcceptanceDateStart: MARCH_START, submitAcceptanceDateEnd: MARCH_END }),
      '合同名对得上 + 3 月区间 → A/B/C/F').toEqual(['A', 'B', 'C', 'F']);
  });

  // ═══════════════════════════════════════════
  // UI 用例（需要前端已启动 + 登录验证码已关闭）
  // ═══════════════════════════════════════════
  test.describe('查询区交互', () => {
    test.beforeEach(async ({ page }) => { await login(page); });

    // BDD: 「开票日期」条件与「实际回款日期」并排出现在「更多」里（SC-001 / FR-001）
    test('「开票日期」与「实际回款日期」并排出现在「更多」折叠区里', async ({ page }) => {
      await gotoList(page);

      // 未展开时，两个条件都藏在 v-if 里（注意：用 form-item 的存在性断言，
      // 不要用占位符文本 —— placeholder 是属性不是文本，hasText 匹配不到它，那样写会永远为 0 而假绿）
      await expect(page.locator('.el-form-item').filter({ hasText: '开票日期' }),
        '未点「更多」时「开票日期」不应出现在查询区').toHaveCount(0);
      await expect(page.locator('.el-form-item').filter({ hasText: '实际回款日期' }),
        '对照：「实际回款日期」同样藏在「更多」里').toHaveCount(0);

      await expandMore(page);

      await expect(formItem(page, '实际回款日期'), '「实际回款日期」应可见').toBeVisible();
      await expect(formItem(page, '开票日期'), '「开票日期」应与它并排可见').toBeVisible();

      const inputs = formItem(page, '开票日期').locator('.el-range-input');
      await expect(inputs, '开票日期应是"开始日期 + 结束日期"的区间控件（两个输入框）').toHaveCount(2);
      await expect(inputs.nth(0), '起始输入框占位符').toHaveAttribute('placeholder', '开始日期');
      await expect(inputs.nth(1), '结束输入框占位符').toHaveAttribute('placeholder', '结束日期');
    });

    // BDD: 填好开票日期点查询，条件真的传到了后台（SC-002 / FR-002）
    test('填好开票日期点查询，条件按契约参数名上行到后台', async ({ page }) => {
      const listReqs = trackRequests(page, LIST_URL);

      await gotoList(page);
      await expandMore(page);
      await fillDateRange(page, '开票日期', MARCH_START, MARCH_END);
      listReqs.clear();
      await runQuery(page);

      const req = listReqs.last();
      expect(paramOf(req, 'submitAcceptanceDateStart'),
        `查询请求应带 submitAcceptanceDateStart（前后端契约参数名，不得改名）。实际请求：${req}`)
        .toBe(MARCH_START);
      expect(paramOf(req, 'submitAcceptanceDateEnd'),
        `查询请求应带 submitAcceptanceDateEnd。实际请求：${req}`).toBe(MARCH_END);

      // 自造数据保证 3 月区间一定有行，可放心断言"筛完还有数据"
      expect(await page.locator('.el-table__body a.el-button', { hasText: '详情' }).count(),
        '3 月区间下应至少有一行（本套件已造 4 条 3 月开票的里程碑）').toBeGreaterThan(0);
    });

    // BDD: 「重置」把开票日期条件清干净（SC-015 / FR-016）
    test('「重置」把开票日期条件清干净，下一次请求不再带该参数', async ({ page }) => {
      const listReqs = trackRequests(page, LIST_URL);

      await gotoList(page);
      await expandMore(page);
      await fillDateRange(page, '开票日期', MARCH_START, MARCH_END);
      await runQuery(page);
      expect(paramOf(listReqs.last(), 'submitAcceptanceDateStart'),
        `重置前请求应带该条件。实际请求：${listReqs.last()}`).toBe(MARCH_START);

      // 点「重置」
      const resp = page.waitForResponse(
        r => r.url().includes(LIST_URL) && r.request().method() === 'GET', { timeout: 25000 });
      listReqs.clear();
      await page.locator('.el-form button', { hasText: '重置' }).first().click();
      await resp;
      await waitTableReady(page);

      // ① 控件上看不到值了
      const inputs = formItem(page, '开票日期').locator('.el-range-input');
      await expect(inputs.nth(0), '重置后开票日期起始应为空').toHaveValue('');
      await expect(inputs.nth(1), '重置后开票日期截止应为空').toHaveValue('');

      // ② 请求里也不再带该参数
      //    （开票日期绑的是独立 ref，不是 queryParams 字段，el-form 的 resetFields 碰不到它，
      //      必须在 resetQuery 里显式清空 —— 漏了的话控件看着空、请求里却仍带旧值）
      const req = listReqs.last();
      expect(paramOf(req, 'submitAcceptanceDateStart'),
        `重置后请求不应再带 submitAcceptanceDateStart。实际请求：${req}`).toBeNull();
      expect(paramOf(req, 'submitAcceptanceDateEnd'),
        `重置后请求不应再带 submitAcceptanceDateEnd。实际请求：${req}`).toBeNull();
    });

    // BDD: 进详情再回列表，开票日期条件还在，并且下一次查询仍然生效（SC-014 / FR-015）
    test('进详情再回列表：开票日期条件保留，且重建后的请求仍带该参数', async ({ page }) => {
      const listReqs = trackRequests(page, LIST_URL);

      await gotoList(page);
      await expandMore(page);
      await fillDateRange(page, '开票日期', MARCH_START, MARCH_END);
      await runQuery(page);

      const beforeReq = listReqs.last();
      expect(paramOf(beforeReq, 'submitAcceptanceDateStart'), `实际请求：${beforeReq}`).toBe(MARCH_START);
      expect(paramOf(beforeReq, 'submitAcceptanceDateEnd'), `实际请求：${beforeReq}`).toBe(MARCH_END);

      // 进详情（RowLinkButton 渲染成真 <a>，左键走 router.push）
      const link = page.locator('.el-table__body a.el-button', { hasText: '详情' }).first();
      await expect(link, '列表应至少有一行带「详情」入口').toHaveCount(1);
      await link.click();
      await page.waitForURL(u => new URL(u).pathname.startsWith(`${PAYMENT_PATH}/detail/`), { timeout: 20000 });

      // 缓存里必须存下 submitAcceptanceDateRange 这个 ref。
      // 【坑】getList() 每次都用该 ref 反向覆写 submitAcceptanceDateStart/End，
      // 只还原 queryParams 而不还原 ref，还原后第一次 getList 会当场把条件抹成 null。
      const cached = await page.evaluate(k => sessionStorage.getItem(k), STORAGE_KEY);
      expect(cached, `离开列表页应把查询状态写入 sessionStorage['${STORAGE_KEY}']`).toBeTruthy();
      const state = JSON.parse(cached);
      expect(state.submitAcceptanceDateRange,
        `缓存必须包含 submitAcceptanceDateRange（区间绑的是独立 ref），实际缓存：${cached}`)
        .toEqual([MARCH_START, MARCH_END]);

      // 关掉列表页签销毁 keep-alive 实例，再返回 —— 这才是真正会丢状态的路径
      const tag = page.locator(`.tags-view-item[data-path="${PAYMENT_PATH}"]`);
      await expect(tag, `tagsView 中应存在付款里程碑列表页签（${PAYMENT_PATH}）`).toHaveCount(1);
      await tag.locator('.el-icon-close').click();
      await expect(tag, '关闭后列表页签应从 tagsView 移除（缓存实例随之销毁）').toHaveCount(0);

      listReqs.clear();
      const resp = page.waitForResponse(
        r => r.url().includes(LIST_URL) && r.request().method() === 'GET', { timeout: 25000 });
      await page.getByRole('button', { name: '返回' }).first().click();
      await page.waitForURL(u => new URL(u).pathname === PAYMENT_PATH, { timeout: 20000 });
      await resp;
      await waitTableReady(page);

      // ① 「更多」区要一起恢复，否则条件筛过了但界面上看不见
      await expect(formItem(page, '开票日期'),
        '开票日期藏在「更多」的 v-if 里，恢复了值却不恢复展开态 = 筛选条件不可见').toBeVisible();

      // ② 控件上看得见原值
      const inputs = formItem(page, '开票日期').locator('.el-range-input');
      await expect(inputs.nth(0), '开票日期起始应恢复').toHaveValue(MARCH_START);
      await expect(inputs.nth(1), '开票日期截止应恢复').toHaveValue(MARCH_END);

      // ③ 真判据：重建后的请求仍带该参数
      const afterReq = listReqs.last();
      expect(paramOf(afterReq, 'submitAcceptanceDateStart'),
        `开票日期条件必须恢复；只还原 queryParams 而不还原 submitAcceptanceDateRange 时，` +
        `getList() 会把它反写成 null。实际请求：${afterReq}`).toBe(MARCH_START);
      expect(paramOf(afterReq, 'submitAcceptanceDateEnd'),
        `实际请求：${afterReq}`).toBe(MARCH_END);
    });

    // BDD（缺陷回归）：选了日期但【不点查询】直接点导出，日期条件不得被静默丢弃
    //
    // 【缺陷背景】两个日期区间绑的是独立 ref（actualPaymentDateRange /
    // submitAcceptanceDateRange），queryParams 里的 xxxDateStart/End 只是派生快照，
    // 历史实现里只有 getList() 会刷新它。handleExport 直接展开 queryParams，
    // 于是「选日期 → 不点查询 → 直接导出」这条路径导出的是全量台账，
    // 界面上却明明摆着筛选条件 —— 财务据此对账会得到错误的口径。
    // 修法：抽出 syncDateRangesToQuery()，getList 与 handleExport 消费 queryParams 前都调用它。
    test('选了日期不点查询直接导出：导出请求仍带开票日期与实际回款日期条件', async ({ page }) => {
      const EXPORT_URL = '/project/payment/export';
      const PAYMENT_MAY_START = '2026-05-01';
      const PAYMENT_MAY_END = '2026-05-31';

      await gotoList(page);
      await expandMore(page);

      // 两个区间都填上，然后【故意不点「查询」】—— 这就是缺陷路径本身
      await fillDateRange(page, '开票日期', MARCH_START, MARCH_END);
      await fillDateRange(page, '实际回款日期', PAYMENT_MAY_START, PAYMENT_MAY_END);

      const exportReqPromise = page.waitForRequest(
        r => r.url().includes(EXPORT_URL) && r.method() === 'POST', { timeout: 25000 });
      await page.locator('button', { hasText: '导出' }).first().click();
      const exportReq = await exportReqPromise;

      // download() 用 tansParams 发 form-urlencoded；paramOf 需要前导 ? 或 &，故补一个 ?
      const body = `?${exportReq.postData() || ''}`;

      expect(paramOf(body, 'submitAcceptanceDateStart'),
        `未点查询直接导出时，导出请求必须带 submitAcceptanceDateStart（handleExport 漏调 ` +
        `syncDateRangesToQuery 就会导出全量）。实际请求体：${decodeURIComponent(body)}`)
        .toBe(MARCH_START);
      expect(paramOf(body, 'submitAcceptanceDateEnd'),
        `实际请求体：${decodeURIComponent(body)}`).toBe(MARCH_END);

      // 实际回款日期是同一个双副本状态，同样中招，必须一并守住
      expect(paramOf(body, 'actualPaymentDateStart'),
        `实际回款日期与开票日期是同一种「独立 ref + 派生快照」结构，导出同样必须带上它。` +
        `实际请求体：${decodeURIComponent(body)}`).toBe(PAYMENT_MAY_START);
      expect(paramOf(body, 'actualPaymentDateEnd'),
        `实际请求体：${decodeURIComponent(body)}`).toBe(PAYMENT_MAY_END);
    });
  });
});
