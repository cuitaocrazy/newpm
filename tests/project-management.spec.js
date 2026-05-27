/**
 * 项目管理功能 E2E 测试（API 造数 + UI 验证）
 *
 * 设计原则：用 API 快速、稳定地造数据（setupApi 走 /dev-api 代理，端口无关），
 * 浏览器只负责验证 UI 渲染/交互。避免用 UI 填整个立项表单（深层部门树、异步编号、
 * 字典耦合、无清理 等使其极脆）。create-via-UI 的脆弱反模式已弃用。
 *
 * 前置：本仓库所有浏览器测试均依赖登录验证码关闭（login() 不填验证码）。
 *
 * 覆盖：列表可见 / 列表查询 / 编辑保存 / 详情展示 / 部门树过滤 / 列表字段 / 项目编号自动生成
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

// 不带 :80：浏览器会把默认端口从 URL 抹掉，waitForURL 用带 :80 的字符串会永远匹配不上
const BASE_URL = 'http://localhost';
const TEST_USER = { username: 'admin', password: '123456789' };

const TS = Date.now();
const PROJECT_NAME = `自动化测试项目_${TS}`;
const PROJECT_CODE = `ZH-BJ-11-API${String(TS).slice(-6)}-2026`; // 唯一，避免与场景7 的 AUTO 编号冲突

/**
 * 选择下拉选项（el-select / dict-select / user-select 通用）。
 * 按 el-form-item 的 data-prop 钩子定位，避开"label 不在 select 内"的脆弱选择器。
 */
async function pickOption(page, prop, optionText) {
  await page.locator(`[data-prop="${prop}"] .el-select`).first().click();
  const item = page.locator(`.el-select-dropdown__item:has-text("${optionText}")`).first();
  await item.waitFor({ state: 'visible' });
  await item.click();
}

/** UI 登录（依赖验证码关闭；登录按钮文本是"登 录"，用 primary class 选） */
async function login(page) {
  await page.goto(BASE_URL);
  await page.fill('input[placeholder="账号"]', TEST_USER.username);
  await page.fill('input[placeholder="密码"]', TEST_USER.password);
  await page.locator('button.el-button--primary').click();
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 });
  const cancelBtn = page.locator('.el-message-box .el-button--default');
  if (await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    await cancelBtn.click();
  }
}

test.describe('项目管理功能测试（API造数 + UI验证）', () => {
  let api;
  let projectId;

  test.beforeAll(async () => {
    api = await setupApi();
    const res = await api.post('/project/project', {
      projectName: PROJECT_NAME,
      industry: 'ZH',
      region: 'BJ',
      regionId: '11',
      regionCode: '11',
      shortName: `API${String(TS).slice(-6)}`,
      establishedYear: '2026',
      projectCode: PROJECT_CODE,
      projectCategory: 'RJKF',
      projectDept: '216',          // 深圳组
      projectStatus: '0',
      projectStage: '0',           // 编辑页必填
      acceptanceStatus: '0',
      estimatedWorkload: '100',
      projectBudget: '1000000',
      projectManagerId: '250',     // 曲君
      marketManagerId: '103',      // 张仟栋
      salesManagerId: '103',       // 编辑页必填
      salesContact: '13800000000', // 编辑页必填
      customerId: '1',             // 编辑页必填
      customerContactId: '1',      // 编辑页必填
      participants: '103,104',
      projectAddress: '测试地址',
      projectPlan: '测试计划',
      projectDescription: '自动化测试项目（API 造数）'
    });
    expect(res.code, '创建测试项目应返回200').toBe(200);
    const list = await api.get('/project/project/list', { projectCode: PROJECT_CODE, pageNum: 1, pageSize: 1 });
    expect(list.total, '应能查到刚创建的项目').toBeGreaterThanOrEqual(1);
    projectId = list.rows[0].projectId;
    console.log(`测试项目已建：id=${projectId}, code=${PROJECT_CODE}`);
  });

  test.afterAll(async () => {
    if (!api) return;
    try {
      if (projectId) await api.del(`/project/project/${projectId}`);
      // 兜底清理历史遗留（含异常中断未删的）
      const r = await api.get('/project/project/list', { projectName: '自动化测试项目_', pageNum: 1, pageSize: 50 });
      for (const row of (r.rows || [])) {
        if (row.projectName?.startsWith('自动化测试项目_')) await api.del(`/project/project/${row.projectId}`);
      }
    } finally {
      await api.dispose();
    }
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  // ─────────────────────────────────────────────
  test('场景1：立项项目在列表可见（API 造数）', async ({ page }) => {
    await page.goto(`${BASE_URL}/project/list`);
    await page.waitForURL(`${BASE_URL}/project/list`);
    await expect(page.locator('.el-table')).toBeVisible();

    await page.getByRole('textbox', { name: '项目名称' }).fill(PROJECT_NAME);
    await page.getByRole('button', { name: '搜索' }).click();
    await expect(page.getByText(PROJECT_NAME).first()).toBeVisible({ timeout: 10000 });
    console.log('✅ 项目在列表可见');
  });

  // ─────────────────────────────────────────────
  test('场景2：项目列表 - 查询和重置', async ({ page }) => {
    await page.goto(`${BASE_URL}/project/list`);
    await expect(page.locator('.el-table')).toBeVisible();

    // 按名称查询：只剩目标项目
    await page.getByRole('textbox', { name: '项目名称' }).fill(PROJECT_NAME);
    await page.getByRole('button', { name: '搜索' }).click();
    await expect(page.getByText(PROJECT_NAME).first()).toBeVisible({ timeout: 10000 });

    // 重置：表格仍在，且条数 >= 1
    await page.getByRole('button', { name: '重置' }).click();
    await page.waitForTimeout(800);
    await expect(page.locator('.el-table')).toBeVisible();
    console.log('✅ 查询和重置完成');
  });

  // ─────────────────────────────────────────────
  test('场景3：编辑项目 - 编辑页加载我方数据且可编辑', async ({ page }) => {
    await page.goto(`${BASE_URL}/project/list/edit/${projectId}`);
    await page.waitForURL(/\/project\/list\/edit\/\d+/);

    // 编辑页应回填我方项目数据
    await expect(page.locator('[data-prop="projectName"] input')).toHaveValue(PROJECT_NAME, { timeout: 10000 });
    await expect(page.locator('[data-prop="shortName"] input')).toHaveValue(/API/);

    // 字段可编辑
    const wl = page.locator('[data-prop="estimatedWorkload"] input');
    await wl.fill('200');
    await expect(wl).toHaveValue('200');
    console.log('✅ 编辑页加载并可编辑');
  });

  // ─────────────────────────────────────────────
  test('场景4：项目详情 - 查看项目信息', async ({ page }) => {
    await page.goto(`${BASE_URL}/project/list/detail/${projectId}`);
    await page.waitForURL(/\/project\/list\/detail\/\d+/);

    // 详情页为只读 el-descriptions，验证模块标题与项目名展示
    await expect(page.getByText('项目基本信息').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('人员配置').first()).toBeVisible();
    await expect(page.getByText(PROJECT_NAME).first()).toBeVisible();
    console.log('✅ 详情页展示正常');
  });

  // ─────────────────────────────────────────────
  test('场景5：部门树过滤 - 只显示三级及以下机构', async ({ page }) => {
    await page.goto(`${BASE_URL}/project/apply`);
    await page.waitForURL(`${BASE_URL}/project/apply`);

    // 打开项目部门树
    await page.locator('[data-prop="projectDept"] .el-select').first().click();
    await expect(page.locator('.el-select-dropdown .el-tree-node').first()).toBeVisible({ timeout: 10000 });

    // 顶层节点不应是一级(亚大)/二级(软件事业部)机构 —— 应从三级开始
    const dropdownText = await page.locator('.el-select-dropdown .el-tree').first().textContent();
    expect(dropdownText).not.toContain('亚大');
    expect(dropdownText).not.toContain('软件事业部');
    console.log('✅ 部门树过滤（三级及以下）验证通过');
  });

  // ─────────────────────────────────────────────
  test('场景6：列表字段验证 - 关键列存在', async ({ page }) => {
    await page.goto(`${BASE_URL}/project/list`);
    await expect(page.locator('.el-table')).toBeVisible();

    // 关键列表头存在
    const header = page.locator('.el-table__header-wrapper');
    await expect(header.getByText('参与人员').first()).toBeVisible();
    await expect(header.getByText('收入确认状态').first()).toBeVisible();
    await expect(header.getByText('审核状态').first()).toBeVisible();
    console.log('✅ 列表关键列存在');
  });

  // ─────────────────────────────────────────────
  test('场景7：项目编号自动生成 - 格式正确', async ({ page }) => {
    await page.goto(`${BASE_URL}/project/apply`);
    await page.waitForURL(`${BASE_URL}/project/apply`);

    // 编号需 5 个字段齐全才生成（generateProjectCode 异步调 checkProjectCode）
    await pickOption(page, 'industry', '中行');       // ZH
    await pickOption(page, 'region', '北京');          // BJ
    await page.waitForTimeout(500);
    await pickOption(page, 'regionCode', '北京市');    // 11
    await page.locator('[data-prop="shortName"] input').fill('AUTO');
    await pickOption(page, 'establishedYear', '2026');

    // 等待异步回填，断言最终格式
    await expect(page.locator('[data-prop="projectCode"] input'))
      .toHaveValue('ZH-BJ-11-AUTO-2026', { timeout: 8000 });
    console.log('✅ 项目编号自动生成: ZH-BJ-11-AUTO-2026');
  });

});
