/**
 * 公司收入确认 —— 编辑权限前端拦截 E2E（Issue #4 回归）
 *
 * 背景：无 revenue:company:edit 权限的用户（团队经理角色，如姜凤鸣）
 *   - 有 revenue:company:query → 能看列表、能进详情查看态
 *   - 无 revenue:company:edit → 不应看到「编辑收入确认」/「提交」按钮，后端 PUT 也 403
 *
 * 前置：验证码已关闭；姜凤鸣密码临时改为 123456789；后端 8085 + 前端(E2E_BASE_URL)。
 * 只读验证，不修改任何业务数据。
 */
import { test, expect } from '@playwright/test';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:80';

// 团队经理角色(role_id=100)：有 query 无 edit —— 1:1 复现生产报错主体
const NOEDIT_USER = { username: 'fengming.jiang', password: '123456789' };
// 超级管理员：有全部权限，作对照
const ADMIN_USER = { username: 'admin', password: '123456789' };
// 姜凤鸣数据权限内可见的公司收入确认项目
const PROJECT_ID = 263;

async function login(page, user) {
  await page.goto(BASE_URL);
  await page.fill('input[placeholder="账号"]', user.username);
  await page.fill('input[placeholder="密码"]', user.password);
  await page.locator('button.el-button--primary').click();
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 });
  // 关闭可能出现的「修改初始密码」等弹窗
  const cancelBtn = page.locator('.el-message-box .el-button--default');
  if (await cancelBtn.count() > 0) {
    await cancelBtn.first().click().catch(() => {});
  }
}

test.describe.serial('公司收入确认 - 编辑权限前端拦截 (Issue #4)', () => {

  test('无 edit 权(姜凤鸣): 详情查看态 —— 看不到「编辑收入确认」按钮', async ({ page }) => {
    await login(page, NOEDIT_USER);
    await page.goto(`${BASE_URL}/revenue/company/detail/${PROJECT_ID}`);

    // 详情右侧标题出现，代表页面已正确加载（query 权限放行）
    await expect(page.getByText('公司收入确认信息')).toBeVisible({ timeout: 10000 });

    // 核心断言：v-hasPermi 生效 —— 无 edit 权，编辑入口不渲染
    await expect(page.locator('button:has-text("编辑收入确认")')).toHaveCount(0);
  });

  test('无 edit 权(姜凤鸣): 强行进编辑态 —— 进得去但看不到「提交」按钮', async ({ page }) => {
    await login(page, NOEDIT_USER);
    // 直接带 ?mode=edit 强行进编辑态（模拟手改 URL 绕过）
    await page.goto(`${BASE_URL}/revenue/company/detail/${PROJECT_ID}?mode=edit`);

    await expect(page.getByText('公司收入确认信息')).toBeVisible({ timeout: 10000 });
    // 「编辑中」标记出现 → 确实进了编辑态（isViewMode=false）
    await expect(page.getByText('编辑中')).toBeVisible();
    // 但提交按钮被 v-hasPermi 拦截 —— 无法提交
    await expect(page.locator('button:has-text("提交")')).toHaveCount(0);
  });

  test('对照 - admin(有 edit): 详情查看态 —— 能看到「编辑收入确认」按钮', async ({ page }) => {
    await login(page, ADMIN_USER);
    await page.goto(`${BASE_URL}/revenue/company/detail/${PROJECT_ID}`);

    await expect(page.getByText('公司收入确认信息')).toBeVisible({ timeout: 10000 });
    // 有 edit 权 → 编辑入口正常渲染，证明按钮本身可见、只受权限控制
    await expect(page.locator('button:has-text("编辑收入确认")')).toHaveCount(1);
  });
});
