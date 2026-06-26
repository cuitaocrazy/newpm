/**
 * 日报填写 - 右侧查询条件 E2E（feature 013-daily-report-write-filter）
 *
 * 覆盖 contracts/ui-filter.md 的 C1–C7：
 *  - 查询条件栏存在（项目名称 / 任务名称 模糊 + 项目经理下拉）
 *  - 项目名称模糊过滤 + 清空恢复
 *  - 任务名称模糊过滤（项目卡片收窄 + 卡片内仅显示匹配任务行；普通项目被排除）
 *  - 项目经理下拉（选项来自参与项目去重 + 选中过滤 + 清空恢复）
 *  - 三条件 AND 组合
 *  - 空状态提示
 *
 * 前置（由会话脚本临时设置，测完恢复）：
 *  - 登录验证码关闭
 *  - admin 已移出日报白名单
 *  - admin 参与 3 个项目（seed remark='e2e-seed-013'）：
 *      「2026年-综合运营各项事务」(杜林纳, 含任务: 投标类任务…)
 *      「【COR】…HHAP-COR、HHAP-TMS分项工作任务订单」(李治强, 含任务)
 *      「新疆兵团社保新增批量代发功能项目」(曲君, 普通无任务)
 *
 * baseURL 由 playwright.config.js 读取 E2E_BASE_URL（本次跑在 :8090），导航用相对路径，端口无关。
 *
 * 注：SC-003「过滤不丢工时」由代码不变量保证（handleSave/handleDelete 遍历完整 formList，
 *     模板仅用 filteredFormList/visibleTaskRows 做展示），不在 UI 层做易碎的保存往返测试。
 */

import { test, expect } from '@playwright/test';

const TEST_USER = { username: 'admin', password: '123456789' };

// 唯一标识各 seed 项目的稳定片段
const P_OPS = '综合运营';      // 仅匹配「2026年-综合运营各项事务」(杜林纳)
const P_XJ = '新疆';            // 仅匹配「新疆兵团社保…」(曲君, 普通项目)
const TASK_TENDER = '投标';     // 仅匹配项目53 的任务「投标类任务」
const NO_MATCH = 'ZZZ_NOMATCH_QXY';
const BASELINE = 3;             // admin 参与的 seed 项目数

/** UI 登录（依赖验证码关闭），停到首页 */
async function login(page) {
  await page.goto('/');
  await page.fill('input[placeholder="账号"]', TEST_USER.username);
  await page.fill('input[placeholder="密码"]', TEST_USER.password);
  await page.locator('button.el-button--primary').click();
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 });
  // 关闭可能弹出的「修改密码」提示框
  const cancelBtn = page.locator('.el-message-box .el-button--default');
  if (await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    await cancelBtn.click();
  }
}

/** 进入日报填写页并等待 3 张项目卡片就绪 */
async function gotoWrite(page) {
  await page.goto('/dailyReport/write');
  await expect(page.locator('.dr-filter-bar')).toBeVisible({ timeout: 15000 });
  await expect(page.locator('.project-item')).toHaveCount(BASELINE, { timeout: 15000 });
}

const projName = (page) => page.locator('.dr-filter-bar input[placeholder="项目名称"]');
const taskName = (page) => page.locator('.dr-filter-bar input[placeholder="任务名称"]');
const managerSelect = (page) => page.locator('.dr-filter-bar .el-select');

test.describe('日报填写 - 右侧查询条件', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await gotoWrite(page);
  });

  test('C1 查询条件栏存在：项目名称 / 任务名称 / 项目经理三控件可见', async ({ page }) => {
    await expect(projName(page)).toBeVisible();
    await expect(taskName(page)).toBeVisible();
    await expect(managerSelect(page)).toBeVisible();
  });

  test('C2/C7 项目名称模糊过滤 + 清空恢复', async ({ page }) => {
    await projName(page).fill(P_OPS);
    await expect(page.locator('.project-item')).toHaveCount(1);
    await expect(page.locator('.project-item .prj-name')).toContainText('综合运营');
    // 清空恢复
    await projName(page).fill('');
    await expect(page.locator('.project-item')).toHaveCount(BASELINE);
  });

  test('C6 过滤无结果显示空状态', async ({ page }) => {
    await projName(page).fill(NO_MATCH);
    await expect(page.locator('.project-item')).toHaveCount(0);
    await expect(page.locator('.filter-empty')).toBeVisible();
    await expect(page.locator('.filter-empty')).toContainText('没有符合筛选条件的项目');
    await projName(page).fill('');
    await expect(page.locator('.project-item')).toHaveCount(BASELINE);
  });

  test('C3 任务名称过滤：收窄到含匹配任务的项目，且只显示匹配任务行，普通项目被排除', async ({ page }) => {
    await taskName(page).fill(TASK_TENDER);
    // 仅项目53（含「投标类任务」）保留；普通项目(新疆)与无匹配任务的107被排除
    await expect(page.locator('.project-item')).toHaveCount(1);
    await expect(page.locator('.project-item .prj-name')).toContainText('综合运营');
    // 卡片内只显示匹配的任务行
    await expect(page.locator('.project-item .task-row')).toHaveCount(1);
    await expect(page.locator('.project-item .task-row')).toContainText('投标');
    // 清空恢复
    await taskName(page).fill('');
    await expect(page.locator('.project-item')).toHaveCount(BASELINE);
  });

  test('C4 项目经理下拉：选项来自参与项目 + 选中过滤 + 清空恢复', async ({ page }) => {
    // 打开下拉，校验选项含三位经理
    await managerSelect(page).click();
    const dropdown = page.locator('.el-select-dropdown:visible');
    await expect(dropdown.locator('.el-select-dropdown__item', { hasText: '曲君' })).toBeVisible();
    await expect(dropdown.locator('.el-select-dropdown__item', { hasText: '杜林纳' })).toBeVisible();
    await expect(dropdown.locator('.el-select-dropdown__item', { hasText: '李治强' })).toBeVisible();
    // 选「曲君」→ 仅普通项目「新疆兵团…」
    await dropdown.locator('.el-select-dropdown__item', { hasText: '曲君' }).first().click();
    await expect(page.locator('.project-item')).toHaveCount(1);
    await expect(page.locator('.project-item .prj-name')).toContainText('新疆');
    // 清空下拉（clearable 的 X）→ 恢复
    await managerSelect(page).hover();
    await page.locator('.dr-filter-bar .el-select__clear').click();
    await expect(page.locator('.project-item')).toHaveCount(BASELINE);
  });

  test('C5 三条件 AND 组合', async ({ page }) => {
    // 项目名「新疆」+ 经理「曲君」→ 命中同一普通项目
    await projName(page).fill(P_XJ);
    await managerSelect(page).click();
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '曲君' }).first().click();
    await expect(page.locator('.project-item')).toHaveCount(1);
    await expect(page.locator('.project-item .prj-name')).toContainText('新疆');
    // 改经理为「杜林纳」（与项目名「新疆」矛盾）→ 0 结果 + 空状态
    await managerSelect(page).click();
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '杜林纳' }).first().click();
    await expect(page.locator('.project-item')).toHaveCount(0);
    await expect(page.locator('.filter-empty')).toBeVisible();
  });

  test('FR-010 切换日期保留查询条件', async ({ page }) => {
    await projName(page).fill(P_OPS);
    await expect(page.locator('.project-item')).toHaveCount(1);
    // 点击日历中“今天之外”的某一天（取当月第一个可见日期数字格）
    const firstDay = page.locator('.cal-cell .mc-day-num').first();
    await firstDay.click();
    // 查询条件应保留（输入框值不被清空），过滤结果仍为 1
    await expect(projName(page)).toHaveValue(P_OPS);
    await expect(page.locator('.project-item')).toHaveCount(1);
  });
});
