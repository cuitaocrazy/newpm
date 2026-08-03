/**
 * 合同编号列排序测试（Issue #9）+ 分页排序稳定性回归（Issue #16）
 *
 * 覆盖范围：
 *  - 合同管理列表 /project/contract/list 支持 orderByColumn=contract_code
 *  - 付款里程碑列表 /project/payment/listWithContracts 支持 orderByColumn=contract_code
 *    （该查询是「合同 1:N 付款」的嵌套 resultMap，PageHelper 会替换 mapper 里原有的
 *     order by；本用例守住两点：SQL 能被解析执行、排序不会把同一合同的多条付款拆散）
 *  - 两个页面 UI 上「合同编号」表头出现排序图标，点击可升序 / 降序 / 取消
 *  - 排序键存在并列值时翻页不重复、不遗漏（Issue #16 回归：contract_code 有 143 行并列，
 *    contract_sign_date 133 行、contract_amount 107 行，缺少唯一次级键会导致跨页重复）
 *
 * 注意：与套件内其他用例一致，运行前需关闭登录验证码（sys.account.captchaEnabled=false）。
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:80';

/**
 * 构造排序参数，与前端 handleSortChange 的实现保持一致：
 * 排序列后追加唯一次级键 c.contract_id，使排序成为全序（Issue #16）。
 * 两个列表的 SQL 主表别名均为 c；付款里程碑查询中 pm_payment 也有 contract_id 列，
 * 必须限定别名否则字段歧义。
 */
const sortParams = (dbColumn, dir) => ({
  orderByColumn: `${dbColumn} ${dir}, c.contract_id`,
  isAsc: dir,
});

/** UI 登录（与 contract-filter.spec.js 约定一致，依赖验证码关闭） */
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

// ─────────────────────────────────────────────────────────────
// 后端接口
// ─────────────────────────────────────────────────────────────
test.describe('合同编号排序 - 后端接口', () => {
  let api;

  test.beforeAll(async () => { api = await setupApi(); });
  test.afterAll(async () => { await api.dispose(); });

  test('合同列表按 contract_code 升序 / 降序返回不同结果', async () => {
    const query = order =>
      api.get('/project/contract/list', { pageNum: 1, pageSize: 10, ...sortParams('contract_code', order) });

    const [asc, desc] = await Promise.all([query('asc'), query('desc')]);

    expect(asc.code).toBe(200);
    expect(desc.code).toBe(200);
    expect(asc.rows.length).toBeGreaterThan(0);
    // 升序与降序的首条不应相同，否则说明 orderByColumn 被忽略了
    expect(asc.rows[0].contractId).not.toBe(desc.rows[0].contractId);
  });

  test('付款里程碑列表按 contract_code 排序且不拆散合同分组', async () => {
    const query = order =>
      api.get('/project/payment/listWithContracts', { pageNum: 1, pageSize: 10, ...sortParams('contract_code', order) });

    for (const order of ['asc', 'desc']) {
      const body = await query(order);

      expect(body.code, `${order} 排序不应报错（PageHelper 需能解析这条含 COLLATE 的 SQL）`).toBe(200);
      expect(body.rows.length).toBeGreaterThan(0);

      // 同一合同必须聚成一条记录（付款挂在 paymentList 里），
      // 出现重复 contractId 说明排序把同一合同的多行拆散了，前端 spanMethod 合并会错乱
      const ids = body.rows.map(r => r.contractId);
      expect(new Set(ids).size, `${order} 排序后不应出现被拆散的重复合同`).toBe(ids.length);
    }

    const [ascBody, descBody] = await Promise.all([query('asc'), query('desc')]);
    expect(ascBody.rows[0].contractId).not.toBe(descBody.rows[0].contractId);
  });

  // BDD: 排序作用于全部数据而非当前页
  test('排序是后端全局排序：第一页与第二页记录互不重复', async () => {
    const page = n =>
      api.get('/project/contract/list', { pageNum: n, pageSize: 10, ...sortParams('contract_code', 'asc') });

    const [p1, p2] = await Promise.all([page(1), page(2)]);
    expect(p1.code).toBe(200);
    expect(p2.total).toBeGreaterThan(10); // 需要至少两页数据，否则本用例无意义

    const ids1 = p1.rows.map(r => r.contractId);
    const ids2 = p2.rows.map(r => r.contractId);
    const overlap = ids2.filter(id => ids1.includes(id));
    // 前端当页排序不会改变分页边界；后端全局排序下两页内容必然互斥。
    // 出现重叠说明排序键不稳定导致分页错位（跨页重复 = 同时也意味着有记录被遗漏）
    expect(overlap, `第 1、2 页出现重复记录：${overlap}`).toHaveLength(0);
  });

  // BDD: Issue #16 回归——并列值更多的既有排序列同样不得跨页重复
  for (const col of ['contract_sign_date', 'contract_amount', 'project_budget_total']) {
    test(`按 ${col} 排序时翻页不重复不遗漏`, async () => {
      const page = n =>
        api.get('/project/contract/list', { pageNum: n, pageSize: 10, ...sortParams(col, 'desc') });

      const [p1, p2] = await Promise.all([page(1), page(2)]);
      expect(p1.code, `${col} 排序不应报错`).toBe(200);
      expect(p2.code).toBe(200);
      expect(p2.total).toBeGreaterThan(10);

      const ids1 = p1.rows.map(r => r.contractId);
      const overlap = p2.rows.map(r => r.contractId).filter(id => ids1.includes(id));
      expect(overlap, `${col} 排序时第 1、2 页重复：${overlap}`).toHaveLength(0);
    });
  }

  // BDD: 合同编号为空的记录参与排序而不被丢弃
  test('空合同编号在升序时排最前且仍出现在列表中', async () => {
    const asc = await api.get('/project/contract/list',
      { pageNum: 1, pageSize: 10, ...sortParams('contract_code', 'asc') });
    expect(asc.code).toBe(200);

    const isBlank = v => v === null || v === undefined || v === '';
    if (!isBlank(asc.rows[0].contractCode)) {
      test.skip(true, '当前库中不存在合同编号为空的合同，该场景无数据可验证');
    }

    // 空编号连续排在最前，且这些记录本身完整返回（未被排序丢弃）
    const blanks = asc.rows.filter(r => isBlank(r.contractCode));
    expect(blanks.length).toBeGreaterThan(0);
    blanks.forEach(r => expect(r.contractId).toBeTruthy());
    const firstNonBlank = asc.rows.findIndex(r => !isBlank(r.contractCode));
    if (firstNonBlank !== -1) {
      // 非空编号之后不应再出现空编号（空值必须聚在最前，而不是散落各处）
      expect(asc.rows.slice(firstNonBlank).some(r => isBlank(r.contractCode))).toBe(false);
    }
  });

  // BDD: 排序与查询条件叠加生效
  test('排序与筛选条件叠加：筛选范围与总数不因排序改变', async () => {
    const base = await api.get('/project/contract/list', { pageNum: 1, pageSize: 10 });
    expect(base.code).toBe(200);
    const sampleStatus = base.rows.map(r => r.contractStatus).find(s => s !== null && s !== undefined && s !== '');
    expect(sampleStatus, '样本数据中应存在带合同状态的合同').toBeTruthy();

    const filtered = await api.get('/project/contract/list',
      { pageNum: 1, pageSize: 10, contractStatus: sampleStatus });
    const filteredSorted = await api.get('/project/contract/list',
      { pageNum: 1, pageSize: 10, contractStatus: sampleStatus, ...sortParams('contract_code', 'desc') });

    expect(filteredSorted.code).toBe(200);
    // 排序只改变顺序，不得改变筛选出的数据范围
    expect(filteredSorted.total).toBe(filtered.total);
    filteredSorted.rows.forEach(r =>
      expect(r.contractStatus, '排序后仍须全部满足筛选条件').toBe(sampleStatus));
  });

  // BDD: 拒绝伪造的排序字段（INV-3）
  test('伪造的排序字段被拒绝，白名单外的字符不放行', async () => {
    const malicious = await api.get('/project/payment/listWithContracts',
      { pageNum: 1, pageSize: 2, orderByColumn: "contract_code;DROP TABLE pm_contract--", isAsc: 'asc' });

    expect(malicious.code, '含分号的排序字段必须被拒绝').not.toBe(200);
    expect(malicious.msg).toContain('参数不符合规范');

    // 合法字段名照常放行，确认拒绝的是非法字符而非整个排序能力
    const ok = await api.get('/project/payment/listWithContracts',
      { pageNum: 1, pageSize: 2, ...sortParams('contract_code', 'asc') });
    expect(ok.code).toBe(200);
  });
});

// ─────────────────────────────────────────────────────────────
// 前端页面
// ─────────────────────────────────────────────────────────────
test.describe('合同编号排序 - 页面交互', () => {
  test.beforeEach(async ({ page }) => { await login(page); });

  for (const [name, path] of [['合同管理', '/htkx/contract'], ['付款里程碑', '/htkx/payment']]) {
    test(`${name}列表的合同编号表头可排序`, async ({ page }) => {
      const sortRequests = [];
      page.on('request', r => {
        if (r.url().includes('orderByColumn')) sortRequests.push(r.url());
      });

      await page.goto(`${BASE_URL}${path}`);
      await page.waitForSelector('.el-table', { timeout: 20000 });

      const header = page.locator('.el-table th').filter({ hasText: '合同编号' }).first();
      await expect(header).toHaveClass(/is-sortable/);
      await expect(header.locator('.caret-wrapper')).toHaveCount(1);

      // 点击三次：升序 → 降序 → 取消
      await header.click();
      await expect(header).toHaveClass(/ascending/);

      await header.click();
      await expect(header).toHaveClass(/descending/);

      await header.click();
      await expect(header).not.toHaveClass(/ascending|descending/);

      // 排序请求必须带上后端可识别的下划线字段名，且附带唯一次级键（Issue #16）
      const decoded = sortRequests.map(u => decodeURIComponent(u));
      expect(decoded.some(u => u.includes('orderByColumn=contract_code asc, c.contract_id') && u.includes('isAsc=asc'))).toBe(true);
      expect(decoded.some(u => u.includes('orderByColumn=contract_code desc, c.contract_id') && u.includes('isAsc=desc'))).toBe(true);
    });

    // BDD: 合计行始终固定在首行且不参与排序（INV-2）
    test(`${name}列表排序后合计行仍在首行`, async ({ page }) => {
      await page.goto(`${BASE_URL}${path}`);
      await page.waitForSelector('.el-table__body tr', { timeout: 20000 });

      const firstRow = page.locator('.el-table__body tr').first();
      await expect(firstRow, '排序前合计行应在首行').toContainText('合计');

      const header = page.locator('.el-table th').filter({ hasText: '合同编号' }).first();
      for (const expectClass of [/ascending/, /descending/]) {
        await header.click();
        await expect(header).toHaveClass(expectClass);
        await expect(page.locator('.el-table__body tr').first(),
          '合计行由前端拼在数据最前，不应被后端排序带走').toContainText('合计');
      }
    });
  }

  // BDD: 翻页时保持当前排序
  test('合同管理列表翻页后仍保持排序', async ({ page }) => {
    const listRequests = [];
    page.on('request', r => {
      if (r.url().includes('/project/contract/list')) listRequests.push(r.url());
    });

    await page.goto(`${BASE_URL}/htkx/contract`);
    await page.waitForSelector('.el-table', { timeout: 20000 });

    const header = page.locator('.el-table th').filter({ hasText: '合同编号' }).first();
    await header.click();
    await header.click();
    await expect(header).toHaveClass(/descending/);

    const nextPage = page.locator('.el-pagination button.btn-next');
    await expect(nextPage).toBeEnabled();
    await nextPage.click();
    await page.waitForTimeout(2000);

    const lastRequest = decodeURIComponent(listRequests[listRequests.length - 1]);
    expect(lastRequest, `翻页请求应带上排序参数，实际：${lastRequest}`)
      .toContain('orderByColumn=contract_code desc, c.contract_id');
    expect(lastRequest).toContain('isAsc=desc');
    expect(lastRequest).toContain('pageNum=2');
    // 翻页后表头排序状态不应丢失
    await expect(header).toHaveClass(/descending/);
  });
});
