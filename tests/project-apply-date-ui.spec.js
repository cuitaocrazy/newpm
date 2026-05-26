/**
 * 立项申请日期(applyDate) —— UI 渲染端到端测试（驱动浏览器）
 *
 * 与 project-print-proposal.spec.js（纯 API 取数）互补：
 *   - API 测试证明"后端正确存取 applyDate"
 *   - 本测试证明"前端三个页面真的把 applyDate 画到了界面上"
 *
 * 分工：数据准备走 API（快且稳），断言走浏览器（看真实渲染）。
 *
 * 覆盖 applyDate 在三处的渲染路径（各不相同，需逐一断言）：
 *   1. 详情页  /project/list/detail/{id}  —— el-descriptions-item「立项日期」文本
 *   2. 编辑页  /project/list/edit/{id}    —— el-date-picker 的 input 回填值
 *   3. 打印立项申请书（列表行「打印立项申请」→ 预览弹窗）—— #print-content-area 单元格
 *
 * 前置：本仓库所有浏览器测试均假设登录验证码已关闭（login() 不填验证码）。
 *       运行前请确保 sys.account.captchaEnabled=false（跑完可恢复）。
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

const BASE_URL = 'http://localhost';
const APPLY_DATE = '2026-05-26';

let api;
let projectId = null;
let projectName = '';
let projectCode = '';

/** UI 登录（沿用 query-smoke 的方式，依赖验证码关闭） */
async function login(page) {
  await page.goto(BASE_URL);
  await page.fill('input[placeholder="账号"]', 'admin');
  await page.fill('input[placeholder="密码"]', '123456789');
  await page.locator('button.el-button--primary').click();
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 });
  // 关闭可能出现的"修改密码"提示弹窗
  const cancelBtn = page.locator('.el-message-box .el-button--default');
  if (await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    await cancelBtn.click();
  }
}

test.describe.serial('立项申请日期 - UI 渲染', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    const stamp = Date.now();
    projectName = `E2E立项日期UI测试_${stamp}`;
    projectCode = `ZH-BJ-11-UIDATE-2026-${stamp}`;
    const r = await api.post('/project/project', {
      projectName,
      industry: 'ZH',
      region: 'BJ',
      regionId: '11',
      shortName: 'UIDATE',
      establishedYear: '2026',
      projectCode,
      projectCategory: 'RJKF',
      projectDept: '216',
      projectStatus: '1',
      acceptanceStatus: '0',
      estimatedWorkload: '10',
      projectBudget: '100000',
      projectManagerId: '1',
      projectDescription: 'UI 渲染测试',
      applyDate: APPLY_DATE
    });
    expect(r.code, '创建测试项目应返回200').toBe(200);
    const list = await api.get('/project/project/list', { projectCode, pageNum: 1, pageSize: 1 });
    expect(list.total, '应能查到刚创建的项目').toBeGreaterThanOrEqual(1);
    projectId = list.rows[0].projectId;
    console.log(`测试项目已建：id=${projectId}, applyDate=${APPLY_DATE}`);
  });

  test.afterAll(async () => {
    if (api) {
      if (projectId) await api.del(`/project/project/${projectId}`);
      await api.dispose();
    }
  });

  // ─────────────────────────────────────────────
  // 1. 详情页：项目信息模块显示「立项日期」及值
  // ─────────────────────────────────────────────
  test('详情页显示「立项日期」标签及值', async ({ page }) => {
    await login(page);
    await page.goto(`${BASE_URL}/project/list/detail/${projectId}`);
    // 立项日期 标签（exact 避免与「立项年度」等混淆）
    await expect(page.getByText('立项日期', { exact: true })).toBeVisible({ timeout: 10000 });
    // 该值应出现在页面上
    await expect(page.getByText(APPLY_DATE).first()).toBeVisible();
    console.log('详情页 OK：立项日期 = ' + APPLY_DATE);
  });

  // ─────────────────────────────────────────────
  // 2. 编辑页：日期选择器回填 applyDate
  // ─────────────────────────────────────────────
  test('编辑页日期选择器回填立项日期', async ({ page }) => {
    await login(page);
    await page.goto(`${BASE_URL}/project/list/edit/${projectId}`);
    // 定位「立项日期」表单项内的日期输入框，断言其值
    const input = page.locator('.el-form-item:has(.el-form-item__label:text-is("立项日期")) input').first();
    await expect(input).toHaveValue(APPLY_DATE, { timeout: 10000 });
    console.log('编辑页 OK：date-picker 回填 = ' + APPLY_DATE);
  });

  // ─────────────────────────────────────────────
  // 3. 打印立项申请书：预览弹窗显示「立项日期」行及值
  // ─────────────────────────────────────────────
  test('打印立项申请书预览显示「立项日期」及值', async ({ page }) => {
    await login(page);
    await page.goto(`${BASE_URL}/project/list`);

    // 按项目名称过滤。该 placeholder 被项目名称/合同编号/合同名称三个框共用，按 label 定位
    // el-autocomplete 填入会弹出建议浮层，按 Escape 关掉再点搜索
    const nameInput = page.getByRole('textbox', { name: '项目名称' });
    await nameInput.fill(projectName);
    await page.keyboard.press('Escape');
    await page.getByRole('button', { name: '搜索' }).click();

    // 显式等待数据行（项目名链接）渲染出来
    await expect(page.getByText(projectName).first()).toBeVisible({ timeout: 10000 });

    // 过滤后唯一数据行，点击「打印立项申请」（页面级，避免固定列分表的行 scope 问题）
    await page.getByRole('button', { name: '打印立项申请' }).first().click();

    // 确认弹窗 → 确定
    await page.locator('.el-message-box__btns .el-button--primary').click();

    // 预览弹窗出现，#print-content-area 内应含「立项日期」行及值
    const printArea = page.locator('#print-content-area');
    await expect(printArea).toBeVisible({ timeout: 10000 });
    await expect(printArea).toContainText('立项日期');
    await expect(printArea).toContainText(APPLY_DATE);
    console.log('打印预览 OK：立项日期 = ' + APPLY_DATE);
  });
});
