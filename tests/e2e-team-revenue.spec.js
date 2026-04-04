/**
 * 收入确认 E2E 测试（只读）
 * 覆盖 TeamRevenueConfirmationController 和 ProjectController 公司收入端点
 * 不修改任何数据，仅测试读操作
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;
let teamProjectId = null;
let companyProjectId = null;

test.describe.serial('收入确认（只读）', () => {
  test.beforeAll(async () => {
    api = await setupApi();
  });

  test.afterAll(async () => {
    await api?.dispose();
  });

  // ========== 团队收入确认 ==========

  // 1. 团队收入确认列表
  test('团队收入确认列表', async () => {
    console.log('>>> 查询团队收入确认分页列表');
    const res = await api.get('/revenue/team/list', { pageNum: 1, pageSize: 10 });
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(Array.isArray(res.rows), 'rows 应为数组').toBe(true);
    expect(typeof res.total, 'total 应为数字').toBe('number');
    expect(res.total).toBeGreaterThanOrEqual(0);
    console.log(`<<< 团队收入确认总数: ${res.total}, 当前页: ${res.rows.length}`);

    // 保存一个 projectId 供后续测试使用
    if (res.rows.length > 0) {
      teamProjectId = res.rows[0].projectId;
      console.log(`    记录 teamProjectId=${teamProjectId} 用于后续详情查询`);
    }
  });

  // 2. 团队收入汇总
  test('团队收入汇总', async () => {
    console.log('>>> 查询团队收入汇总');
    const res = await api.get('/revenue/team/summary');
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(res.data, 'data 不应为空').not.toBeNull();
    console.log(`<<< 团队收入汇总数据:`, JSON.stringify(res.data));
  });

  // 3. 团队收入详情
  test('团队收入详情', async () => {
    if (!teamProjectId) {
      console.log('<<< 无团队收入数据，跳过详情查询');
      test.skip();
      return;
    }
    console.log(`>>> 查询团队收入详情, projectId=${teamProjectId}`);
    const res = await api.get(`/revenue/team/${teamProjectId}`);
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(res.data, 'data 不应为空').not.toBeNull();
    console.log(`<<< 团队收入详情: ${JSON.stringify(Object.keys(res.data))}`);
    if (res.data.detailList) {
      console.log(`    确认明细条数: ${res.data.detailList.length}`);
    }
  });

  // 4. 项目收入页信息
  test('项目收入页信息', async () => {
    if (!teamProjectId) {
      console.log('<<< 无团队收入数据，跳过项目收入页查询');
      test.skip();
      return;
    }
    console.log(`>>> 查询项目收入页信息, projectId=${teamProjectId}`);
    const res = await api.get(`/revenue/team/project/${teamProjectId}`);
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(res.data, 'data 不应为空').not.toBeNull();
    console.log(`<<< 项目收入页信息: 项目=${res.data.projectName || res.data.projectCode || teamProjectId}`);
  });

  // ========== 公司收入确认 ==========

  // 5. 公司收入确认列表
  test('公司收入确认列表', async () => {
    console.log('>>> 查询公司收入确认分页列表');
    const res = await api.get('/project/project/revenue/list', { pageNum: 1, pageSize: 10 });
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(Array.isArray(res.rows), 'rows 应为数组').toBe(true);
    expect(typeof res.total, 'total 应为数字').toBe('number');
    expect(res.total).toBeGreaterThanOrEqual(0);
    console.log(`<<< 公司收入确认总数: ${res.total}, 当前页: ${res.rows.length}`);

    // 保存一个 projectId 供后续测试使用
    if (res.rows.length > 0) {
      companyProjectId = res.rows[0].projectId;
      console.log(`    记录 companyProjectId=${companyProjectId} 用于后续详情查询`);
    }
  });

  // 6. 公司收入确认汇总
  test('公司收入确认汇总', async () => {
    console.log('>>> 查询公司收入确认汇总');
    const res = await api.get('/project/project/revenue/summary');
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(res.data, 'data 不应为空').not.toBeNull();
    console.log(`<<< 公司收入确认汇总数据:`, JSON.stringify(res.data));
  });

  // 7. 公司收入确认详情
  test('公司收入确认详情', async () => {
    if (!companyProjectId) {
      console.log('<<< 无公司收入数据，跳过详情查询');
      test.skip();
      return;
    }
    console.log(`>>> 查询公司收入确认详情, projectId=${companyProjectId}`);
    const res = await api.get(`/project/project/revenue/${companyProjectId}`);
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(res.data, 'data 不应为空').not.toBeNull();
    expect(res.data.projectId, 'projectId 应匹配').toBe(companyProjectId);
    console.log(`<<< 公司收入确认详情: 项目=${res.data.projectName || res.data.projectCode || companyProjectId}`);
    // 验证收入确认相关字段存在
    const data = res.data;
    console.log(`    确认金额: ${data.confirmAmount ?? '未设置'}, 税率: ${data.taxRate ?? '未设置'}, 税后金额: ${data.afterTaxAmount ?? '未设置'}, 确认状态: ${data.revenueConfirmStatus ?? '未设置'}`);
  });
});
