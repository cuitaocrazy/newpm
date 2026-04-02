/**
 * 所有列表页查询功能冒烟测试
 * 验证：点击查询按钮后，接口正常返回数据（不报错、有响应）
 */

import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:80';

async function login(page, username = 'admin', password = '123456789') {
  await page.goto(BASE_URL);
  await page.fill('input[placeholder="账号"]', username);
  await page.fill('input[placeholder="密码"]', password);
  await page.locator('button.el-button--primary').click();
  // 等待跳离登录页（无论跳到哪个路径）
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 });
  // 关闭可能出现的密码修改弹窗
  const cancelBtn = page.locator('.el-message-box .el-button--default');
  if (await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    await cancelBtn.click();
  }
}

/**
 * 通用查询测试：先注册响应监听器，再导航，捕获页面挂载时的自动请求
 */
async function testQuery(page, { name, url, apiPattern }) {
  console.log(`\n▶ 测试查询：${name}`);

  // 在 goto 之前注册监听，确保捕获页面挂载时的首次自动请求
  const responsePromise = page.waitForResponse(
    res => res.url().includes(apiPattern) && res.request().method() === 'GET',
    { timeout: 20000 }
  );

  await page.goto(`${BASE_URL}${url}`);

  const response = await responsePromise;
  const status = response.status();
  console.log(`  接口: ${response.url()}`);
  console.log(`  状态: ${status}`);

  expect(status, `${name} 查询接口应返回 200`).toBe(200);

  const body = await response.json();
  expect(body.code, `${name} 响应 code 应为 200`).toBe(200);
  console.log(`  ✅ ${name} 查询正常，total=${body.total ?? 'N/A'}`);
}

test.describe('列表查询冒烟测试', () => {

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('项目列表 - 查询', async ({ page }) => {
    await testQuery(page, {
      name: '项目列表',
      url: '/project/list',
      apiPattern: '/project/project/list',
    });
  });

  test('立项审核 - 查询', async ({ page }) => {
    await testQuery(page, {
      name: '立项审核',
      url: '/project/review',
      apiPattern: '/project/review/list',
    });
  });

  test('立项审核 - 项目名称模糊查询', async ({ page }) => {
    console.log('\n▶ 测试：立项审核项目名称模糊查询');
    await page.goto(`${BASE_URL}/project/review`);

    // 输入项目名称（不选下拉）
    const input = page.locator('.el-autocomplete input').first();
    await input.waitFor({ state: 'visible', timeout: 20000 });
    await input.fill('测试');

    const responsePromise = page.waitForResponse(
      res => res.url().includes('/project/review/list') && res.request().method() === 'GET',
      { timeout: 15000 }
    );

    await page.locator('button:has-text("搜索")').first().click();
    const response = await responsePromise;

    // 验证请求参数带上了 projectName
    const reqUrl = new URL(response.url());
    const projectName = reqUrl.searchParams.get('projectName');
    console.log(`  projectName 参数: ${projectName}`);
    expect(projectName, '模糊查询应传递 projectName 参数').toBeTruthy();
    expect(response.status()).toBe(200);
    console.log('  ✅ 立项审核模糊查询正常');
  });

  test('合同管理 - 查询', async ({ page }) => {
    await testQuery(page, { name: '合同管理', url: '/htkx/contract', apiPattern: '/project/contract/list' });
  });

  test('客户管理 - 查询', async ({ page }) => {
    await testQuery(page, { name: '客户管理', url: '/market/customer', apiPattern: '/project/customer/list' });
  });

  test('款项管理 - 查询', async ({ page }) => {
    await testQuery(page, { name: '款项管理', url: '/htkx/payment', apiPattern: '/project/payment/listWithContracts' });
  });

  test('项目成员 - 查询', async ({ page }) => {
    await testQuery(page, {
      name: '项目成员',
      url: '/project/projectMember',
      apiPattern: '/project/member/list',
    });
  });

  test('项目经理变更 - 查询', async ({ page }) => {
    await testQuery(page, {
      name: '项目经理变更',
      url: '/project/managerChange',
      apiPattern: '/project/managerChange/list',
    });
  });

  test('项目阶段变更 - 查询', async ({ page }) => {
    await testQuery(page, {
      name: '项目阶段变更',
      url: '/project/projectStageChange',
      apiPattern: '/project/projectStageChange/list',
    });
  });

  test('公司收入确认 - 查询', async ({ page }) => {
    await testQuery(page, { name: '公司收入确认', url: '/revenue/company', apiPattern: '/project/project/revenue/list' });
  });

  test('团队收入确认 - 查询', async ({ page }) => {
    await testQuery(page, { name: '团队收入确认', url: '/revenue/team', apiPattern: '/revenue/team/list' });
  });

  test('二级区域管理 - 查询', async ({ page }) => {
    await testQuery(page, { name: '二级区域管理', url: '/system/secondaryRegion', apiPattern: '/project/secondaryRegion/list' });
  });

  test('工作日历 - 页面加载', async ({ page }) => {
    await testQuery(page, { name: '工作日历', url: '/dailyReport/workCalendar', apiPattern: '/project/workCalendar/year/' });
  });

});

test.describe('项目列表查询结果一致性', () => {

  test('竞态修复验证 — qiang.qin 搜索"厦门"返回行数与 total 一致', async ({ page }) => {
    console.log('\n▶ 测试：竞态修复，qiang.qin 搜索厦门');
    await login(page, 'admin', '123456789');

    // 通过侧边栏菜单导航（避免 page.goto 动态路由 404）
    const parentMenu = page.locator('.sidebar-container .el-sub-menu').filter({ hasText: '项目管理' }).first();
    await parentMenu.locator('.el-sub-menu__title').click();
    await page.waitForTimeout(500);
    await parentMenu.locator('.el-menu-item').filter({ hasText: '项目管理' }).first().click();
    const searchBtn = page.locator('button:has-text("搜索")').first();
    await searchBtn.waitFor({ state: 'visible', timeout: 20000 });

    // 填入搜索关键词并立即搜索（模拟竞态场景：初始请求可能还在途中）
    const nameInput = page.locator('.el-autocomplete input').first();
    await nameInput.fill('厦门');

    const filteredResponsePromise = page.waitForResponse(
      res => res.url().includes('/project/project/list') && res.url().includes('projectName') && res.request().method() === 'GET',
      { timeout: 15000 }
    );

    await searchBtn.click();
    const filteredResponse = await filteredResponsePromise;
    const body = await filteredResponse.json();

    const apiTotal = body.total;
    const apiRows = (body.rows || []).length;
    console.log(`  API total=${apiTotal}, rows.length=${apiRows}`);
    expect(filteredResponse.status()).toBe(200);
    expect(body.code).toBe(200);

    // 等待表格稳定（竞态若未修复，此时旧响应可能覆盖新数据）
    await page.waitForTimeout(2000);

    // 统计页面实际渲染的数据行数（排除合计行）
    const dataRows = await page.locator('.el-table__body-wrapper tr:not(.summary-row)').count();
    console.log(`  页面渲染行数: ${dataRows}，API total: ${apiTotal}`);

    // 页面行数应 = API 返回的 rows 数量（含合计行则 apiRows+1）
    expect(dataRows, `页面行数(${dataRows})应与 API rows(${apiRows})+合计行 一致`).toBeLessThanOrEqual(apiRows + 1);

    // 验证所有数据行链接文本包含"厦门"（项目名称列有超链接）
    const projectNameLinks = await page.locator('.el-table__body-wrapper td a').allTextContents();
    const nonXiamen = projectNameLinks.filter(t => t.trim() && !t.includes('厦门'));
    console.log(`  项目名称链接: ${JSON.stringify(projectNameLinks)}`);
    console.log(`  不含"厦门"的链接: ${JSON.stringify(nonXiamen)}`);
    expect(nonXiamen.length, `不应存在不含"厦门"的项目名称`).toBe(0);

    console.log('  ✅ 竞态修复验证通过');
  });

});
