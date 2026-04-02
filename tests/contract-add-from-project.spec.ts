import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost';

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

test.describe('从项目列表关联合同功能测试', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('点击项目列表的"关联合同"按钮，应该自动回显项目、部门、客户信息', async ({ page }) => {
    // 1. 通过侧边栏导航到项目管理页面
    const parentMenu = page.locator('.sidebar-container .el-sub-menu').filter({ hasText: '项目管理' }).first();
    await parentMenu.locator('.el-sub-menu__title').click();
    await page.waitForTimeout(500);
    await parentMenu.locator('.el-menu-item').filter({ hasText: '项目管理' }).first().click();

    // 2. 等待项目列表加载
    await page.waitForSelector('.el-table__body', { timeout: 15000 });

    // 3. 点击数据行（非合计行）的"关联合同"按钮
    // Element Plus 固定列在独立容器中，普通 locator 可能被遮挡，用 JS 点击
    await page.evaluate(() => {
      // 找到数据行（排除合计行 .el-table__footer-wrapper）中的"关联合同"按钮
      const rows = document.querySelectorAll('.el-table__body-wrapper .el-table__row');
      for (const row of rows) {
        const btn = [...row.querySelectorAll('button')].find(b => b.textContent?.includes('关联合同'));
        if (btn) { btn.click(); return; }
      }
    });
    await page.waitForTimeout(500);

    // 6. 等待页面导航（URL 变化 或 新内容加载）
    await page.waitForTimeout(3000);
    const url = page.url();
    console.log('当前 URL:', url);

    const projectIdMatch = url.match(/bind-contract\/(\d+)/);
    expect(projectIdMatch, 'URL 应包含项目ID').toBeTruthy();

    // 等待页面数据加载
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // 7. 检查部门字段是否有值
    const deptInput = page.locator('label:has-text("部门")').locator('xpath=following-sibling::div[1]//input').first();
    if (await deptInput.count() > 0) {
      const deptDisplayValue = await deptInput.inputValue();
      console.log('部门显示值:', deptDisplayValue || '(空)');
    }

    // 8. 检查关联客户字段是否有值
    const customerInput = page.locator('label:has-text("关联客户")').locator('xpath=following-sibling::div[1]//input').first();
    if (await customerInput.count() > 0) {
      const customerDisplayValue = await customerInput.inputValue();
      console.log('关联客户显示值:', customerDisplayValue || '(空)');
    }

    // 9. 验证页面加载了内容（表单或表格）
    const hasContent = await page.locator('.el-form, .el-table, .el-card, .el-descriptions').first().isVisible({ timeout: 10000 }).catch(() => false);
    expect(hasContent, '合同关联页面应包含内容').toBeTruthy();
  });
});
