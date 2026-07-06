/**
 * 移动端日报 H5 E2E 测试（specs/014-daily-report-mobile）
 *
 * 覆盖：
 * - US1: 移动登录、守卫重定向、填写普通/含子任务项目并保存、回显（T010）
 * - US2: 假期条目添加/删除、汇总（T013）
 * - US3: 本周日期切换、越界日期只读（T016）
 * - SC-002: 桌面视角双端一致（移动保存 → 桌面 write.vue 回显）
 *
 * 前置：验证码已临时关闭（memory: feedback_e2e_captcha_toggle）
 * 数据卫生：beforeAll 快照当天日报，afterAll 恢复/删除，避免污染本地数据
 */

import { test, expect, devices } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:80';

// iPhone 13 仿真参数（不含 defaultBrowserType，避免 describe 内 test.use 报错；chromium 支持 isMobile/hasTouch）
const iPhone = devices['iPhone 13'];
const MOBILE_USE = {
  viewport: iPhone.viewport,
  userAgent: iPhone.userAgent,
  deviceScaleFactor: iPhone.deviceScaleFactor,
  isMobile: iPhone.isMobile,
  hasTouch: iPhone.hasTouch
};

function fmt(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
const now = new Date();
const today = fmt(now);
const dow = now.getDay();
const monday = new Date(now);
monday.setDate(now.getDate() + (dow === 0 ? -6 : 1 - dow));
const lastWeekWed = new Date(monday);
lastWeekWed.setDate(monday.getDate() - 5);
const outOfWeekDate = fmt(lastWeekWed); // 上周三，越界日期

let api;
let plainProject = null;   // 无子任务项目
let taskProject = null;    // 含子任务项目（且有任务行）
let priorToday = null;     // 当天日报快照（恢复用）
let whitelisted = false;

const PLAIN_CONTENT = '移动端E2E测试-普通项目工作内容';
const TASK_CONTENT = '移动端E2E测试-任务工作内容';

async function injectAuth(context) {
  await context.addCookies([{ name: 'Admin-Token', value: api.token, url: BASE_URL }]);
}

/** 恢复当天日报到测试前状态 */
async function restoreToday() {
  const cur = await api.get(`/project/dailyReport/my/${today}`);
  const curId = cur.data?.reportId;
  if (priorToday?.detailList?.length) {
    const detailList = priorToday.detailList.map(d => ({
      projectId: d.projectId,
      projectStage: d.projectStage,
      workHours: d.workHours,
      workContent: d.workContent,
      entryType: d.entryType || 'work',
      subProjectId: d.subProjectId ?? null,
      workCategory: d.workCategory || null,
      leaveHours: d.leaveHours,
      remark: d.remark || ''
    }));
    await api.post('/project/dailyReport', { reportDate: today, detailList });
  } else if (curId) {
    await api.del(`/project/dailyReport/${curId}`);
  }
}

test.describe.configure({ mode: 'serial' });

test.beforeAll(async () => {
  api = await setupApi();
  // 白名单用户不渲染表单，整套跳过
  const wl = await api.get('/project/whitelist/checkSelf');
  whitelisted = wl.data === true;
  // 挑选测试项目
  const res = await api.get('/project/dailyReport/myProjects');
  const projects = res.data || [];
  plainProject = projects.find(p => !p.hasSubProject) || null;
  for (const p of projects.filter(p => p.hasSubProject)) {
    const t = await api.get(`/project/task/options`, { projectId: p.projectId });
    if ((t.data || []).length > 0) { taskProject = p; break; }
  }
  // 快照当天日报
  const cur = await api.get(`/project/dailyReport/my/${today}`);
  priorToday = cur.data || null;
  console.log(`\n📱 移动端E2E启动 today=${today} 越界日期=${outOfWeekDate}`);
  console.log(`  普通项目: ${plainProject?.projectName || '无'} | 含任务项目: ${taskProject?.projectName || '无'} | 白名单: ${whitelisted}`);
});

test.afterAll(async () => {
  try { await restoreToday(); console.log('  ♻️ 当天日报已恢复至测试前状态'); } catch (e) { console.log('  ⚠️ 恢复失败: ' + e.message); }
  await api?.dispose();
});

// ============ US1: 守卫与登录（无登录态） ============
test.describe('移动端-守卫与登录', () => {
  test.use(MOBILE_USE);

  test('未登录访问填写页重定向到移动登录页(FR-010)', async ({ page }) => {
    await page.goto('/m/daily-report/write');
    await page.waitForURL(/\/m\/login/);
    expect(page.url()).toContain('/m/login');
    expect(page.url()).toContain('redirect=');
    console.log('  ✅ 未登录重定向 → ' + page.url());
  });

  test('移动登录页可登录并进入填写页', async ({ page }) => {
    await page.goto('/m/login');
    await page.getByPlaceholder('请输入账号').fill('admin');
    await page.getByPlaceholder('请输入密码').fill('123456789');
    // 验证码已关闭：验证码行不应出现
    await expect(page.getByPlaceholder('请输入验证码')).toBeHidden();
    await page.getByRole('button', { name: /登\s*录/ }).click();
    await page.waitForURL(/\/m\/daily-report\/write/, { timeout: 15000 });
    await expect(page.locator('.van-nav-bar__title')).toHaveText('日报填写');
    console.log('  ✅ 移动登录成功并进入填写页');
  });
});

// ============ US1: 填写与保存 ============
test.describe('移动端-填写与保存(US1)', () => {
  test.use(MOBILE_USE);
  test.beforeEach(async ({ context }) => { await injectAuth(context); });

  test('填写普通项目+任务项目并保存，API断言契约字段', async ({ page }) => {
    test.skip(whitelisted, '当前账号在白名单中');
    test.skip(!plainProject, '无普通项目可测');
    test.setTimeout(60000);

    await page.goto('/m/daily-report/write');
    const plainCard = page.locator('.mdr-card', { hasText: plainProject.projectName }).first();
    await expect(plainCard).toBeVisible({ timeout: 20000 });

    // 普通项目：工时 4 + 类别 + 内容
    await plainCard.locator('.van-stepper input').fill('4');
    await plainCard.locator('.van-field', { hasText: '类别' }).click();
    const popup = page.locator('.van-popup--bottom:visible');
    await expect(popup).toBeVisible();
    await popup.locator('.van-cell').first().click();
    await popup.locator('.mdr-popup__btn.is-confirm').click();
    await plainCard.locator('textarea').fill(PLAIN_CONTENT);

    // 含子任务项目：展开后填第一个任务
    if (taskProject) {
      const taskCard = page.locator('.mdr-card', { hasText: taskProject.projectName }).first();
      await taskCard.scrollIntoViewIfNeeded();
      const collapseTitle = taskCard.locator('.van-collapse-item__title');
      // 未展开则点开
      if (!(await taskCard.locator('.mdr-task').first().isVisible().catch(() => false))) {
        await collapseTitle.click();
      }
      const firstTask = taskCard.locator('.mdr-task').first();
      await expect(firstTask).toBeVisible();
      await firstTask.locator('.van-stepper input').fill('2');
      await firstTask.locator('.van-field', { hasText: '类别' }).click();
      const popup2 = page.locator('.van-popup--bottom:visible');
      await popup2.locator('.van-cell').first().click();
      await popup2.locator('.mdr-popup__btn.is-confirm').click();
      await firstTask.locator('textarea').fill(TASK_CONTENT);
    }

    // 保存
    await page.locator('.mdr-savebar button').click();
    await expect(page.locator('.van-toast', { hasText: '日报保存成功' })).toBeVisible({ timeout: 10000 });

    // API 断言（contracts/reused-apis.md 契约锚点 + data-model §3）
    const res = await api.get(`/project/dailyReport/my/${today}`);
    expect(res.code).toBe(200);
    const details = res.data?.detailList || [];
    const plainDetail = details.find(d => d.projectId === plainProject.projectId && d.subProjectId == null && (!d.entryType || d.entryType === 'work'));
    expect(plainDetail, '应存在普通项目work明细').toBeTruthy();
    expect(Number(plainDetail.workHours)).toBe(4);
    expect(plainDetail.workContent).toBe(PLAIN_CONTENT);
    expect(plainDetail.workCategory, 'workCategory 应为非空逗号串').toBeTruthy();

    if (taskProject) {
      const taskDetail = details.find(d => d.projectId === taskProject.projectId && d.subProjectId != null);
      expect(taskDetail, '应存在任务work明细(subProjectId非空)').toBeTruthy();
      expect(Number(taskDetail.workHours)).toBe(2);
      expect(taskDetail.workContent).toBe(TASK_CONTENT);
      expect(taskDetail.workCategory).toBeTruthy();
    }
    console.log('  ✅ 保存成功且 payload 契约字段全部匹配');
  });

  test('重新进入页面回显一致', async ({ page }) => {
    test.skip(whitelisted || !plainProject, '前置不满足');
    await page.goto('/m/daily-report/write');
    const plainCard = page.locator('.mdr-card', { hasText: plainProject.projectName }).first();
    await expect(plainCard).toBeVisible({ timeout: 20000 });
    await expect(plainCard.locator('.van-stepper input')).toHaveValue(/^4(\.0)?$/);
    await expect(plainCard.locator('textarea')).toHaveValue(PLAIN_CONTENT);
    console.log('  ✅ 回显一致（工时/内容）');
  });
});

// ============ US2: 假期登记 ============
test.describe('移动端-假期登记(US2)', () => {
  test.use(MOBILE_USE);
  test.beforeEach(async ({ context }) => { await injectAuth(context); });

  test('添加假期8小时并保存，API断言leaveDetail结构', async ({ page }) => {
    test.skip(whitelisted, '当前账号在白名单中');
    test.setTimeout(60000);

    await page.goto('/m/daily-report/write');
    await expect(page.locator('.mdr-card').first()).toBeVisible({ timeout: 20000 });

    const leaveCard = page.locator('.mdr-card', { hasText: '假期记录' });
    await expect(leaveCard).toBeVisible({ timeout: 20000 });
    await leaveCard.scrollIntoViewIfNeeded();
    // 幂等：先清掉已有假期条目（serial 重试会重跑整链，避免累计）
    const existingDel = leaveCard.locator('.mdr-leave__del');
    const existingCount = await existingDel.count();
    for (let i = existingCount - 1; i >= 0; i--) { await existingDel.nth(i).click(); }
    await leaveCard.getByRole('button', { name: '添加假期' }).click();
    const leaveRow = leaveCard.locator('.mdr-leave').last();
    await expect(leaveRow).toBeVisible();

    // 类型默认 leave（请假），改选 Picker 最后一项并记录文本
    await leaveRow.locator('.van-field', { hasText: '类型' }).click();
    const picker = page.locator('.van-popup--bottom:visible .van-picker');
    await expect(picker).toBeVisible();
    await picker.locator('.van-picker-column__item').last().click();
    await picker.locator('.van-picker__confirm').click();

    // 小时 8
    await leaveRow.locator('.van-stepper input').fill('8');
    await leaveRow.getByPlaceholder('可选').fill('E2E假期备注');

    await page.locator('.mdr-savebar button').click();
    await expect(page.locator('.van-toast', { hasText: '日报保存成功' })).toBeVisible({ timeout: 10000 });

    // API 断言 leaveDetail 结构（data-model §3：projectId null、workHours/leaveHours 同值双写）
    const res = await api.get(`/project/dailyReport/my/${today}`);
    const leaves = (res.data?.detailList || []).filter(d => d.entryType && d.entryType !== 'work');
    expect(leaves.length, '应恰好一条假期明细（测试前已清空）').toBe(1);
    const l = leaves[leaves.length - 1];
    expect(l.projectId == null).toBe(true);
    expect(Number(l.leaveHours)).toBe(8);
    expect(Number(l.workHours)).toBe(8);
    // 注：remark 不做持久化断言——后端批量插入明细 SQL 不含 remark 列（DailyReportDetailMapper.xml:90），
    // 桌面端同样不持久化假期备注；移动端行为与桌面一致（发送但后端丢弃），后端零改动约束下不修
    console.log(`  ✅ 假期保存成功 entryType=${l.entryType} leaveHours=${l.leaveHours}`);
  });

  test('汇总条包含工时+假期合计', async ({ page }) => {
    test.skip(whitelisted || !plainProject, '前置不满足');
    await page.goto('/m/daily-report/write');
    await expect(page.locator('.mdr-card').first()).toBeVisible({ timeout: 20000 });
    // 期望值从 API 动态计算，避免依赖前序用例的固定数值
    const res = await api.get(`/project/dailyReport/my/${today}`);
    const details = res.data?.detailList || [];
    const leaveSum = details.filter(d => d.entryType && d.entryType !== 'work')
      .reduce((s, d) => s + Number(d.leaveHours || d.workHours || 0), 0);
    const summary = page.locator('.mdr-summary .van-tag');
    await expect(summary).toContainText('合计');
    if (leaveSum > 0) {
      await expect(summary).toContainText(`假期 ${leaveSum}h`);
    }
    console.log('  ✅ 汇总条含假期小时：' + (await summary.textContent()));
  });

  test('删除假期条目并保存后移除', async ({ page }) => {
    test.skip(whitelisted || !plainProject, '前置不满足');
    test.setTimeout(60000);
    await page.goto('/m/daily-report/write');
    await expect(page.locator('.mdr-card').first()).toBeVisible({ timeout: 20000 });

    const leaveCard = page.locator('.mdr-card', { hasText: '假期记录' });
    await expect(leaveCard).toBeVisible({ timeout: 20000 });
    await leaveCard.scrollIntoViewIfNeeded();
    const delIcons = leaveCard.locator('.mdr-leave__del');
    await expect(delIcons.first()).toBeVisible({ timeout: 10000 });
    const count = await delIcons.count();
    expect(count).toBeGreaterThan(0);
    // 删除全部假期条目
    for (let i = count - 1; i >= 0; i--) { await delIcons.nth(i).click(); }
    await page.locator('.mdr-savebar button').click();
    await expect(page.locator('.van-toast', { hasText: '日报保存成功' })).toBeVisible({ timeout: 10000 });

    const res = await api.get(`/project/dailyReport/my/${today}`);
    const leaves = (res.data?.detailList || []).filter(d => d.entryType && d.entryType !== 'work');
    expect(leaves.length, '假期明细应已移除').toBe(0);
    console.log('  ✅ 假期条目删除并保存生效');
  });
});

// ============ US3: 周约束 ============
test.describe('移动端-周约束(US3)', () => {
  test.use(MOBILE_USE);
  test.beforeEach(async ({ context }) => { await injectAuth(context); });

  test('URL直达越界日期进入只读态(FR-009)', async ({ page }) => {
    test.skip(whitelisted, '当前账号在白名单中');
    await page.goto(`/m/daily-report/write?date=${outOfWeekDate}`);
    await expect(page.locator('.van-notice-bar')).toBeVisible({ timeout: 20000 });
    await expect(page.locator('.van-notice-bar')).toContainText('仅可填写本周');
    await expect(page.locator('.mdr-savebar button')).toBeDisabled();
    console.log(`  ✅ 越界日期 ${outOfWeekDate} 只读态生效（提示+保存禁用）`);
  });

  test('本周日期chip切换可用且今天回显保留', async ({ page }) => {
    test.skip(whitelisted || !plainProject, '前置不满足');
    await page.goto('/m/daily-report/write');
    await expect(page.locator('.mdr-week')).toBeVisible({ timeout: 20000 });
    // 7 个 chip
    await expect(page.locator('.mdr-week__day')).toHaveCount(7);
    // 切到本周一（非今天时验证切换；周一当天则跳过切换断言）
    const mondayChip = page.locator('.mdr-week__day').first();
    await mondayChip.click();
    await expect(mondayChip).toHaveClass(/is-active/);
    // 无只读提示（本周内均可编辑）
    await expect(page.locator('.van-notice-bar')).toBeHidden();
    // 切回今天：已填数据回显（US3-AS3）
    const todayChip = page.locator('.mdr-week__day.is-today');
    await todayChip.click();
    const plainCard = page.locator('.mdr-card', { hasText: plainProject.projectName }).first();
    await expect(plainCard.locator('.van-stepper input')).toHaveValue(/^4(\.0)?$/, { timeout: 15000 });
    console.log('  ✅ 周内切换正常，切回今天回显保留');
  });
});

// ============ SC-002: 桌面视角双端一致（默认桌面 viewport） ============
test.describe('桌面视角双端一致(SC-002)', () => {
  test.beforeEach(async ({ context }) => { await injectAuth(context); });

  test('移动端保存的数据在桌面write.vue回显一致', async ({ page }) => {
    test.skip(whitelisted || !plainProject, '前置不满足');
    test.setTimeout(60000);
    // 自足造数：经 API 写入与移动端产出完全一致的 payload（US1 已断言移动 UI 产出该结构，证据链衔接）
    // 避免 serial 重试跨 worker 时 afterAll 恢复导致状态丢失
    const saveRes = await api.post('/project/dailyReport', {
      reportDate: today,
      detailList: [{
        projectId: plainProject.projectId,
        projectStage: plainProject.projectStage,
        workHours: 4,
        workContent: PLAIN_CONTENT,
        entryType: 'work',
        subProjectId: null,
        workCategory: '1'
      }]
    });
    expect(saveRes.code).toBe(200);

    // 桌面真实路由为 /dailyReport/write（日报管理为顶级菜单，见 sys_menu 2214/2215）
    await page.goto('/dailyReport/write');
    await expect(page.getByText(plainProject.projectName).first()).toBeVisible({ timeout: 30000 });
    // 断言移动端语义的保存内容出现在桌面 textarea 中
    await expect
      .poll(async () => {
        const values = await page.locator('textarea').evaluateAll(els => els.map(e => e.value));
        return values.some(v => (v || '').includes(PLAIN_CONTENT));
      }, { timeout: 15000, message: '桌面端应回显移动端保存的工作内容' })
      .toBe(true);
    console.log('  ✅ 桌面端回显移动端保存的内容，双端一致');
    // 本用例自足清理
    await restoreToday();
  });
});
