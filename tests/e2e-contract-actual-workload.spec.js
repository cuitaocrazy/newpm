/**
 * 合同详情 - 关联项目列表「实际人天」换算回归测试
 *
 * 背景：pm_project.actual_workload 在库中存储为「小时」，展示层需换算为「人天」
 *      （人天 = 小时/8 + adjust_workload，见 src/utils/workload.ts:toPersonDays）。
 *      合同详情页「关联项目列表」此前直接显示原始小时，本用例锁定修复后行为。
 *
 * 数据驱动：从 /project/contract/list 找一条「关联项目含实际工时」的合同作为样本，
 *          不硬编码 ID，避免测试数据变动导致失效。
 *
 * 注意：与套件内其他用例一致，运行前需关闭登录验证码（sys.account.captchaEnabled=false）。
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:80';

/** 与 src/utils/workload.ts 的 toPersonDays 保持一致 */
function toPersonDays(hours, adjustDays = 0) {
  const h = parseFloat(String(hours ?? 0)) || 0;
  const a = parseFloat(String(adjustDays ?? 0)) || 0;
  return (h / 8 + a).toFixed(3);
}

/** UI 登录（依赖验证码关闭，约定同 contract-filter.spec.js） */
async function login(page, username = 'admin', password = '123456789') {
  await page.goto(BASE_URL);
  await page.fill('input[placeholder="账号"]', username);
  await page.fill('input[placeholder="密码"]', password);
  await page.locator('button.el-button--primary').click();
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 });
  const cancelBtn = page.locator('.el-message-box .el-button--default');
  if (await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    await cancelBtn.click();
  }
}

test.describe('合同详情 - 关联项目列表 实际人天', () => {
  let api;
  /** { contractId, projectName, rawHours, adjust, expected } */
  let sample;

  test.beforeAll(async () => {
    api = await setupApi();
    const res = await api.get('/project/contract/list', { pageNum: 1, pageSize: 300 });
    expect(res.code, '合同列表应返回 200').toBe(200);
    const rows = res.rows || [];
    for (const c of rows) {
      const proj = (c.projectList || []).find(p => Number(p.actualWorkload) > 0);
      if (proj) {
        sample = {
          contractId: c.contractId,
          projectName: proj.projectName,
          rawHours: Number(proj.actualWorkload),
          adjust: Number(proj.adjustWorkload || 0),
          expected: toPersonDays(proj.actualWorkload, proj.adjustWorkload),
        };
        break;
      }
    }
    expect(sample, '需要一条「关联项目含实际工时」的合同作为样本').toBeTruthy();
    // eslint-disable-next-line no-console
    console.log('[sample]', JSON.stringify(sample));
  });

  test.afterAll(async () => {
    if (api) await api.dispose();
  });

  test('API: 列表接口返回的是原始小时（换算由前端负责）', () => {
    // 原始小时应明显大于换算后的人天（÷8），确认后端未做换算
    expect(sample.rawHours).toBeGreaterThan(parseFloat(sample.expected));
  });

  test('UI: 实际人天列显示换算后的人天，而非原始小时', async ({ page }) => {
    await login(page);
    await page.goto(`${BASE_URL}/htkx/contract/detail/${sample.contractId}`);

    // 定位「关联项目列表」卡片，避免与付款/任务等其他表格混淆
    const card = page.locator('.el-card', { has: page.getByText('关联项目列表', { exact: true }) });
    await expect(card).toBeVisible({ timeout: 15000 });

    // 目标项目所在行（按项目名匹配）
    const row = card.locator('.el-table__body tbody tr', { hasText: sample.projectName }).first();
    await expect(row).toBeVisible({ timeout: 15000 });

    // 「实际人天」为第 5 列（序号/项目名称/预算金额/预估工作量/实际人天）→ td 索引 4
    const cell = row.locator('td').nth(4);
    await expect(cell).toHaveText(sample.expected);

    // 防回归：单元格不应再显示原始小时整数部分
    const rawInt = String(Math.trunc(sample.rawHours));
    await expect(cell).not.toHaveText(new RegExp(`^\\s*${rawInt}(\\.0+)?\\s*$`));
  });
});

test.describe('付款详情 - 关联项目列表 实际人天', () => {
  let api;
  /** { paymentId, contractId, projectName, rawHours, adjust, expected } */
  let sample;

  test.beforeAll(async () => {
    api = await setupApi();
    // 1) 收集「关联项目含实际工时」的合同（付款详情的项目列表同样来自 getContract().projectList）
    const cres = await api.get('/project/contract/list', { pageNum: 1, pageSize: 300 });
    expect(cres.code, '合同列表应返回 200').toBe(200);
    const byContract = new Map();
    for (const c of cres.rows || []) {
      const proj = (c.projectList || []).find(p => Number(p.actualWorkload) > 0);
      if (proj && !byContract.has(c.contractId)) {
        byContract.set(c.contractId, {
          projectName: proj.projectName,
          rawHours: Number(proj.actualWorkload),
          adjust: Number(proj.adjustWorkload || 0),
          expected: toPersonDays(proj.actualWorkload, proj.adjustWorkload),
        });
      }
    }
    // 2) 找一条 contractId 命中上述合同的付款里程碑
    const pres = await api.get('/project/payment/list', { pageNum: 1, pageSize: 300 });
    expect(pres.code, '付款列表应返回 200').toBe(200);
    for (const pay of pres.rows || []) {
      if (byContract.has(pay.contractId)) {
        sample = { paymentId: pay.paymentId, contractId: pay.contractId, ...byContract.get(pay.contractId) };
        break;
      }
    }
    expect(sample, '需要一条「合同含实际工时关联项目」的付款里程碑作为样本').toBeTruthy();
    // eslint-disable-next-line no-console
    console.log('[payment-sample]', JSON.stringify(sample));
  });

  test.afterAll(async () => {
    if (api) await api.dispose();
  });

  test('UI: 实际人天列显示换算后的人天，而非原始小时', async ({ page }) => {
    await login(page);
    await page.goto(`${BASE_URL}/htkx/payment/detail/${sample.paymentId}`);

    const card = page.locator('.el-card', { has: page.getByText('关联项目列表', { exact: true }) });
    await expect(card).toBeVisible({ timeout: 15000 });

    const row = card.locator('.el-table__body tbody tr', { hasText: sample.projectName }).first();
    await expect(row).toBeVisible({ timeout: 15000 });

    // 付款详情列序：序号/项目名称/当前阶段/预算金额/预估工作量/实际人天 → td 索引 5
    const cell = row.locator('td').nth(5);
    await expect(cell).toHaveText(sample.expected);

    const rawInt = String(Math.trunc(sample.rawHours));
    await expect(cell).not.toHaveText(new RegExp(`^\\s*${rawInt}(\\.0+)?\\s*$`));
  });
});
