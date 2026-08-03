/**
 * 合同编号列排序测试（Issue #9）
 *
 * 覆盖范围：
 *  - 合同管理列表 /project/contract/list 支持 orderByColumn=contract_code
 *  - 付款里程碑列表 /project/payment/listWithContracts 支持 orderByColumn=contract_code
 *    （该查询是「合同 1:N 付款」的嵌套 resultMap，PageHelper 会替换 mapper 里原有的
 *     order by；本用例守住两点：SQL 能被解析执行、排序不会把同一合同的多条付款拆散）
 *  - 两个页面 UI 上「合同编号」表头出现排序图标，点击可升序 / 降序 / 取消
 *
 * 注意：与套件内其他用例一致，运行前需关闭登录验证码（sys.account.captchaEnabled=false）。
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:80';

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
      api.get('/project/contract/list', { pageNum: 1, pageSize: 10, orderByColumn: 'contract_code', isAsc: order });

    const [asc, desc] = await Promise.all([query('asc'), query('desc')]);

    expect(asc.code).toBe(200);
    expect(desc.code).toBe(200);
    expect(asc.rows.length).toBeGreaterThan(0);
    // 升序与降序的首条不应相同，否则说明 orderByColumn 被忽略了
    expect(asc.rows[0].contractId).not.toBe(desc.rows[0].contractId);
  });

  test('付款里程碑列表按 contract_code 排序且不拆散合同分组', async () => {
    const query = order =>
      api.get('/project/payment/listWithContracts', { pageNum: 1, pageSize: 10, orderByColumn: 'contract_code', isAsc: order });

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

      // 排序请求必须带上后端可识别的下划线字段名
      expect(sortRequests.some(u => u.includes('orderByColumn=contract_code&isAsc=asc'))).toBe(true);
      expect(sortRequests.some(u => u.includes('orderByColumn=contract_code&isAsc=desc'))).toBe(true);
    });
  }
});
