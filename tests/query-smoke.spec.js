/**
 * 所有列表页查询功能冒烟测试
 * 验证：点击查询按钮后，接口正常返回数据（不报错、有响应）
 */

import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:80';

async function login(page) {
  await page.goto(BASE_URL);
  await page.fill('input[placeholder="账号"]', 'admin');
  await page.fill('input[placeholder="密码"]', '123456789');
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
 * 通用查询测试：导航到页面，等待列表加载，点击查询按钮，验证接口响应
 */
async function testQuery(page, { name, url, apiPattern, searchBtn = '搜索' }) {
  console.log(`\n▶ 测试查询：${name}`);

  const responsePromise = page.waitForResponse(
    res => res.url().includes(apiPattern) && res.request().method() === 'GET',
    { timeout: 15000 }
  );

  await page.goto(`${BASE_URL}${url}`);
  await page.waitForLoadState('networkidle', { timeout: 15000 });

  // 点击查询按钮
  const btn = page.locator(`button:has-text("${searchBtn}")`).first();
  await btn.waitFor({ state: 'visible', timeout: 10000 });
  await btn.click();

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
      url: '/project/project',
      apiPattern: '/project/project/list',
    });
  });

  test('立项审核 - 查询', async ({ page }) => {
    await testQuery(page, {
      name: '立项审核',
      url: '/project/approval',
      apiPattern: '/project/approval/projectList',
    });
  });

  test('立项审核 - 项目名称模糊查询', async ({ page }) => {
    console.log('\n▶ 测试：立项审核项目名称模糊查询');
    await page.goto(`${BASE_URL}/project/approval`);
    await page.waitForLoadState('networkidle', { timeout: 15000 });

    // 输入项目名称（不选下拉）
    const input = page.locator('.el-autocomplete input').first();
    await input.waitFor({ state: 'visible', timeout: 10000 });
    await input.fill('测试');

    const responsePromise = page.waitForResponse(
      res => res.url().includes('/project/approval/projectList') && res.request().method() === 'GET',
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
    await testQuery(page, {
      name: '合同管理',
      url: '/project/contract',
      apiPattern: '/project/contract/list',
    });
  });

  test('客户管理 - 查询', async ({ page }) => {
    await testQuery(page, {
      name: '客户管理',
      url: '/project/customer',
      apiPattern: '/project/customer/list',
    });
  });

  test('款项管理 - 查询', async ({ page }) => {
    await testQuery(page, {
      name: '款项管理',
      url: '/project/payment',
      apiPattern: '/project/payment/list',
    });
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
    await testQuery(page, {
      name: '公司收入确认',
      url: '/revenue/company',
      apiPattern: '/project/review/list',
    });
  });

  test('团队收入确认 - 查询', async ({ page }) => {
    await testQuery(page, {
      name: '团队收入确认',
      url: '/revenue/team',
      apiPattern: '/revenue/team/list',
    });
  });

  test('二级区域管理 - 查询', async ({ page }) => {
    await testQuery(page, {
      name: '二级区域管理',
      url: '/project/secondaryRegion',
      apiPattern: '/project/secondaryRegion/list',
    });
  });

  test('工作日历 - 查询', async ({ page }) => {
    await testQuery(page, {
      name: '工作日历',
      url: '/project/workCalendar',
      apiPattern: '/project/workCalendar/list',
    });
  });

});
