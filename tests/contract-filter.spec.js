/**
 * 合同编号 / 合同名称 查询条件测试
 *
 * 覆盖范围：
 *  - 后端 /project/contract/searchForFilter（autoComplete 数据权限建议）
 *  - 项目管理列表（/project/project/list）按合同编号/名称过滤
 *  - 公司收入确认列表（/project/project/revenue/list）按合同编号/名称过滤
 *  - 列表 + 合计（summary）查询均接受合同过滤参数（验证 selectProjectSummary 的
 *    ca 子查询已补齐 contract_code/contract_name 两列）
 *  - 两个页面的 UI autoComplete 下拉与过滤联动
 *
 * 注意：与套件内其他用例一致，运行前需关闭登录验证码（sys.account.captchaEnabled=false）。
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

const BASE_URL = 'http://localhost:80';

/** UI 登录（与 query-smoke.spec.js 约定一致，依赖验证码关闭） */
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
// 后端 API 测试
// ─────────────────────────────────────────────────────────────
test.describe('合同过滤 - 后端接口', () => {
  let api;
  /** 一个真实存在、且当前用户数据权限内可见的合同（数据驱动，避免硬编码） */
  let sampleContract;

  test.beforeAll(async () => {
    api = await setupApi();
    // 不带关键词 → 返回数据权限内最近 20 条合同，取首条作为样本
    const res = await api.get('/project/contract/searchForFilter', {});
    expect(res.code, 'searchForFilter 应返回 200').toBe(200);
    expect(Array.isArray(res.data), 'searchForFilter data 应为数组').toBe(true);
    sampleContract = (res.data || []).find(c => c.contractCode || c.contractName);
  });

  test.afterAll(async () => {
    if (api) await api.dispose();
  });

  test('searchForFilter 返回精简字段且条数受限（<=20）', async () => {
    const res = await api.get('/project/contract/searchForFilter', {});
    expect(res.code).toBe(200);
    expect(res.data.length, '应受 limit 20 约束').toBeLessThanOrEqual(20);
    if (res.data.length) {
      const first = res.data[0];
      expect(first, '应包含 contractId').toHaveProperty('contractId');
      expect(first, '应包含 contractCode').toHaveProperty('contractCode');
      expect(first, '应包含 contractName').toHaveProperty('contractName');
    }
  });

  test('searchForFilter 按合同编号模糊匹配', async () => {
    test.skip(!sampleContract || !sampleContract.contractCode, '无可用合同编号样本');
    const kw = sampleContract.contractCode.slice(0, 3);
    const res = await api.get('/project/contract/searchForFilter', { contractCode: kw });
    expect(res.code).toBe(200);
    for (const c of res.data) {
      expect(c.contractCode, `编号「${c.contractCode}」应包含关键词「${kw}」`).toContain(kw);
    }
  });

  test('searchForFilter 按合同名称模糊匹配', async () => {
    test.skip(!sampleContract || !sampleContract.contractName, '无可用合同名称样本');
    const kw = sampleContract.contractName.slice(0, 2);
    const res = await api.get('/project/contract/searchForFilter', { contractName: kw });
    expect(res.code).toBe(200);
    for (const c of res.data) {
      expect(c.contractName, `名称「${c.contractName}」应包含关键词「${kw}」`).toContain(kw);
    }
  });

  test('项目列表按合同编号过滤：结果行的合同编号列均命中', async () => {
    test.skip(!sampleContract || !sampleContract.contractCode, '无可用合同编号样本');
    const code = sampleContract.contractCode;
    const res = await api.get('/project/project/list', { contractCode: code, pageSize: 100 });
    expect(res.code).toBe(200);
    for (const row of res.rows || []) {
      // 项目可能关联多份合同（GROUP_CONCAT 串），命中其一即可
      expect(row.contractCode || '', `项目「${row.projectName}」合同编号「${row.contractCode}」应包含「${code}」`).toContain(code);
    }
  });

  test('项目列表按合同名称过滤：结果行的合同名称列均命中', async () => {
    test.skip(!sampleContract || !sampleContract.contractName, '无可用合同名称样本');
    const name = sampleContract.contractName;
    const res = await api.get('/project/project/list', { contractName: name, pageSize: 100 });
    expect(res.code).toBe(200);
    for (const row of res.rows || []) {
      expect(row.contractName || '', `项目「${row.projectName}」合同名称应包含过滤值`).toContain(name);
    }
  });

  test('公司收入确认列表按合同编号过滤返回 200 且命中', async () => {
    test.skip(!sampleContract || !sampleContract.contractCode, '无可用合同编号样本');
    const code = sampleContract.contractCode;
    const res = await api.get('/project/project/revenue/list', { contractCode: code, pageSize: 100 });
    expect(res.code).toBe(200);
    for (const row of res.rows || []) {
      expect(row.contractCode || '', `收入确认行合同编号应包含「${code}」`).toContain(code);
    }
  });

  test('合计（summary）查询接受合同过滤参数（验证 ca 子查询补列）', async () => {
    test.skip(!sampleContract || !sampleContract.contractCode, '无可用合同编号样本');
    const code = sampleContract.contractCode;
    // 项目管理合计
    const projSummary = await api.get('/project/project/summary', { contractCode: code });
    expect(projSummary.code, '项目管理 summary 应 200（selectProjectSummary 含 ca.contract_code）').toBe(200);
    // 公司收入确认合计
    const revSummary = await api.get('/project/project/revenue/summary', { contractCode: code });
    expect(revSummary.code, '公司收入确认 summary 应 200（selectRevenueSummary 含 ca.contract_code）').toBe(200);
  });

  test('合同编号过滤后列表条数 <= 全量条数（过滤确实生效）', async () => {
    test.skip(!sampleContract || !sampleContract.contractCode, '无可用合同编号样本');
    const all = await api.get('/project/project/list', { pageSize: 1 });
    const filtered = await api.get('/project/project/list', { contractCode: sampleContract.contractCode, pageSize: 1 });
    expect(all.code).toBe(200);
    expect(filtered.code).toBe(200);
    expect(filtered.total, '过滤后 total 不应超过全量 total').toBeLessThanOrEqual(all.total);
  });
});

// ─────────────────────────────────────────────────────────────
// 前端 UI 测试
// ─────────────────────────────────────────────────────────────
test.describe('合同过滤 - 项目管理页 UI', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('合同编号 autoComplete 触发 searchForFilter 并按编号过滤列表', async ({ page }) => {
    await page.goto(`${BASE_URL}/project/list`);
    const codeInput = page.locator('.el-form-item').filter({ hasText: '合同编号' }).locator('input').first();
    await codeInput.waitFor({ state: 'visible', timeout: 20000 });

    // 输入触发 autoComplete 建议接口
    const suggestPromise = page.waitForResponse(
      res => res.url().includes('/project/contract/searchForFilter') && res.url().includes('contractCode'),
      { timeout: 15000 }
    );
    await codeInput.fill('F');
    const suggestResp = await suggestPromise;
    expect(suggestResp.status(), 'searchForFilter 应 200').toBe(200);

    // 搜索时列表接口应带 contractCode 参数
    const listPromise = page.waitForResponse(
      res => res.url().includes('/project/project/list') && res.url().includes('contractCode') && res.request().method() === 'GET',
      { timeout: 15000 }
    );
    await page.locator('button:has-text("搜索")').first().click();
    const listResp = await listPromise;
    const param = new URL(listResp.url()).searchParams.get('contractCode');
    expect(param, '列表请求应携带 contractCode 参数').toBeTruthy();
    expect(listResp.status()).toBe(200);
  });

  test('合同名称 autoComplete 触发 searchForFilter（contractName 参数）', async ({ page }) => {
    await page.goto(`${BASE_URL}/project/list`);
    const nameInput = page.locator('.el-form-item').filter({ hasText: '合同名称' }).locator('input').first();
    await nameInput.waitFor({ state: 'visible', timeout: 20000 });

    const suggestPromise = page.waitForResponse(
      res => res.url().includes('/project/contract/searchForFilter') && res.url().includes('contractName'),
      { timeout: 15000 }
    );
    await nameInput.fill('建');
    const suggestResp = await suggestPromise;
    expect(suggestResp.status()).toBe(200);
  });
});

test.describe('合同过滤 - 公司收入确认页 UI', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('合同编号过滤：列表接口带 contractCode 且 200', async ({ page }) => {
    await page.goto(`${BASE_URL}/revenue/company`);
    const codeInput = page.locator('.el-form-item').filter({ hasText: '合同编号' }).locator('input').first();
    await codeInput.waitFor({ state: 'visible', timeout: 20000 });

    const suggestPromise = page.waitForResponse(
      res => res.url().includes('/project/contract/searchForFilter'),
      { timeout: 15000 }
    );
    await codeInput.fill('F');
    await suggestPromise;

    const listPromise = page.waitForResponse(
      res => res.url().includes('/project/project/revenue/list') && res.url().includes('contractCode') && res.request().method() === 'GET',
      { timeout: 15000 }
    );
    await page.locator('button:has-text("查询")').first().click();
    const listResp = await listPromise;
    expect(new URL(listResp.url()).searchParams.get('contractCode'), '应携带 contractCode').toBeTruthy();
    expect(listResp.status()).toBe(200);
  });

  test('查询表单每行 4 个查询要素（最后一行为操作按钮）', async ({ page }) => {
    await page.goto(`${BASE_URL}/revenue/company`);
    await page.locator('.el-form-item').filter({ hasText: '合同编号' }).first().waitFor({ state: 'visible', timeout: 20000 });
    // 16 个查询字段，每个 el-col span=6 → 每行 4 个
    const cols = await page.locator('form .el-row .el-col[class*="is-span-6"], form .el-row .el-col').evaluateAll(
      els => els.filter(e => /el-col-6\b/.test(e.className)).length
    );
    expect(cols, '应为 16 个 span=6 的查询字段列').toBe(16);
  });
});
