/**
 * 005-cleanup-project-task-fields 验证测试
 * 验证：清理 pm_project.yml 废弃字段后，项目管理和任务管理页面仍正常工作
 * 使用侧边栏菜单导航（避免 page.goto 导致动态路由 404）
 */

import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:80';

async function login(page) {
  await page.goto(BASE_URL);
  await page.fill('input[placeholder="账号"]', 'admin');
  await page.fill('input[placeholder="密码"]', '123456789');
  await page.locator('button.el-button--primary').click();
  await expect(page.locator('.sidebar-container')).toBeVisible({ timeout: 20000 });
  const cancelBtn = page.locator('.el-message-box .el-button--default');
  if (await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    await cancelBtn.click();
  }
}

/** 通过侧边栏菜单导航并验证 API 正常 */
async function navigateAndVerify(page, { menuName, subMenuName, apiPattern }) {
  // 展开一级菜单（点击一级菜单的标题区域）
  const parentMenu = page.locator(`.sidebar-container .el-sub-menu`).filter({ hasText: menuName }).first();
  await parentMenu.locator('.el-sub-menu__title').click();
  await page.waitForTimeout(500);

  // 注册响应监听
  const responsePromise = page.waitForResponse(
    res => res.url().includes(apiPattern) && res.request().method() === 'GET',
    { timeout: 20000 }
  );

  // 点击二级菜单项（el-menu-item 不含子菜单的叶子节点）
  const subItem = parentMenu.locator('.el-menu-item').filter({ hasText: subMenuName }).first();
  await subItem.click();

  const response = await responsePromise;
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.code).toBe(200);

  // 表格渲染
  await expect(page.locator('.el-table__body-wrapper')).toBeVisible({ timeout: 10000 });
  return body;
}

test.describe('005 清理验证 — 项目与任务页面正常', () => {

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('项目列表页 — 菜单导航 + API 正常', async ({ page }) => {
    const body = await navigateAndVerify(page, {
      menuName: '项目管理',
      subMenuName: '项目管理',
      apiPattern: '/project/project/list'
    });
    expect(body.total).toBeGreaterThanOrEqual(0);
  });

  test('任务列表页 — 菜单导航 + API 正常', async ({ page }) => {
    const body = await navigateAndVerify(page, {
      menuName: '任务管理',
      subMenuName: '任务管理',
      apiPattern: '/project/task/list'
    });
    expect(body.total).toBeGreaterThanOrEqual(0);
  });

  test('合同列表页 — 菜单导航 + API 正常', async ({ page }) => {
    const body = await navigateAndVerify(page, {
      menuName: '合同款项',
      subMenuName: '合同管理',
      apiPattern: '/project/contract/list'
    });
    expect(body.total).toBeGreaterThanOrEqual(0);
  });
});
