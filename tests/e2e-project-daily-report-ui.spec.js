/**
 * 项目日报 UI 层 E2E（specs/018-project-daily-report）
 *
 * 覆盖 bdd/coverage.md 的 UI 层场景：
 *   4.1 图例含累计人天公式          5.1 查询条件标签措辞
 *   4.2 表头与图例同名、全页无旧名   5.2 项目名是带 href 的真实链接
 *   2.1 不填年月查询成功、无日期列   （附）R-003 布局实拍：5 列全 fixed、中间 0 列
 *
 * 登录方式：用 API 拿 token 后注入 Admin-Token cookie，绕开登录页（验证码状态与本用例无关）。
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:80';
const PAGE_PATH = '/dailyReport/teamReport';
/** 新疆组：project 38 所属部门，确保查询有数据 */
const SAMPLE_DEPT_ID = 215;

let api;

test.beforeAll(async () => {
  api = await setupApi();
});

test.afterAll(async () => {
  if (api) await api.dispose();
});

test.beforeEach(async ({ context, page }) => {
  await context.addCookies([{ name: 'Admin-Token', value: api.token, url: BASE_URL }]);
  await page.goto(PAGE_PATH);
  await page.waitForLoadState('networkidle');
});

test.describe('018 项目日报 UI', () => {
  test('查询条件标签为「项目所属部门」（FR-018）', async ({ page }) => {
    await expect(page.locator('.el-form-item__label', { hasText: '项目所属部门' })).toBeVisible();
    // 旧措辞不得残留
    const oldLabel = await page.locator('.el-form-item__label', { hasText: /^项目部门/ }).count();
    expect(oldLabel, '不应再出现「项目部门」这个旧标签').toBe(0);
  });

  test('图例包含累计人天的算法说明（FR-016）', async ({ page }) => {
    const legend = page.locator('.legend-bar');
    await expect(legend).toBeVisible();
    await expect(legend).toContainText('项目累计人天 = 项目日报小时 ÷ 8 + 补正天数');
  });

  test('全页不出现旧名「实际人天」，表头与图例同名（FR-015 / SC-008）', async ({ page }) => {
    const bodyText = await page.locator('body').innerText();
    expect(bodyText, '页面上不应再有「实际人天」字样').not.toContain('实际人天');
    // 图例里用的就是新名
    await expect(page.locator('.legend-bar')).toContainText('项目累计人天为红色');
  });

  test('不填年月查询：成功返回、无日期列、项目名是可右键新开的真实链接', async ({ page }) => {
    // 选择项目所属部门。注意点 .el-select__wrapper 而非内部 input——
    // Element Plus 的 placeholder span 覆盖在 input 之上会拦截 pointer events。
    await page.locator('.el-form-item', { hasText: '项目所属部门' })
      .locator('.el-select__wrapper').first().click();
    await page.waitForTimeout(800);

    // ProjectDeptSelect 是 el-tree-select 且 filterable 默认 false（组件 index.vue:45-48），
    // 无法输入过滤，只能逐层展开。目标节点在第 5 层（新疆组 ancestors=0,100,101,105,...）。
    const targetLabel = page.locator('.el-tree-node__label', { hasText: '新疆组' });
    for (let depth = 0; depth < 6; depth++) {
      if (await targetLabel.count() > 0) break;
      const collapsed = page.locator('.el-tree-node__expand-icon:not(.is-leaf)')
        .locator(':not(.expanded)');
      const arrows = page.locator('.el-tree-node__expand-icon:not(.is-leaf)');
      const n = await arrows.count();
      if (n === 0) break;
      for (let i = 0; i < n; i++) {
        const arrow = arrows.nth(i);
        const cls = await arrow.getAttribute('class');
        if (cls && !cls.includes('expanded')) {
          await arrow.click({ timeout: 3000 }).catch(() => {});
          await page.waitForTimeout(120);
        }
      }
    }
    if (await targetLabel.count() === 0) {
      const allLabels = await page.locator('.el-tree-node__label').allInnerTexts();
      const dropdownCount = await page.locator('.el-select-dropdown').count();
      console.log(`  ⚠️ 未找到「新疆组」。下拉容器数=${dropdownCount}，`
        + `可见节点(${allLabels.length})=${allLabels.slice(0, 30).join(' / ')}`);
      test.skip(true, '部门树中找不到「新疆组」，跳过（部门数据可能已变）');
    }
    await targetLabel.first().click();
    await page.waitForTimeout(300);

    // 清空年月
    const monthInput = page.locator('.el-form-item', { hasText: '年月' }).locator('input').first();
    await monthInput.hover();
    const clearIcon = page.locator('.el-form-item', { hasText: '年月' }).locator('.el-input__clear').first();
    if (await clearIcon.count() > 0) {
      await clearIcon.click();
    } else {
      await monthInput.fill('');
      await page.keyboard.press('Escape');
    }

    await page.getByRole('button', { name: '查询' }).click();
    await page.waitForLoadState('networkidle');

    // 不应弹出「请选择年月」
    const warned = await page.locator('.el-message--warning', { hasText: '请选择年月' }).count();
    expect(warned, '年月已非必填，不应再拦截').toBe(0);

    // 表格出现且没有日期列（日期列表头是两位数字，如 "01".."31"）
    await expect(page.locator('.el-table')).toBeVisible();
    const headers = await page.locator('.el-table__header th .cell').allInnerTexts();
    const dayCols = headers.filter((h) => /^\d{2}$/.test(h.trim()));
    expect(dayCols.length, `不填年月时不应有日期列，实际出现：${dayCols.join(',')}`).toBe(0);
    console.log(`  ℹ️ 表头：${headers.map((h) => h.trim()).filter(Boolean).join(' | ')}`);

    // FR-019：项目名必须是带有效 href 的 <a>——这是浏览器右键出现「在新标签页打开」的充要条件
    const firstLink = page.locator('.project-link').first();
    await expect(firstLink).toBeVisible();
    const href = await firstLink.getAttribute('href');
    expect(href, '项目名必须有真实 href').toBeTruthy();
    expect(href).toContain('/project/list/detail/');
    console.log(`  ℹ️ 项目名链接 href=${href}`);

    // R-003 布局实拍：5 列全 fixed（2 左 + 3 右）、中间 0 可滚动列
    await page.screenshot({ path: 'test-results/018-no-month-layout.png', fullPage: false });

    // 页面本身不得横向溢出
    const overflow = await page.evaluate(() =>
      document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow, `页面横向溢出 ${overflow}px，布局异常`).toBeLessThanOrEqual(1);
  });
});
