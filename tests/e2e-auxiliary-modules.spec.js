/**
 * 辅助模块 E2E 测试
 * 覆盖 4 个辅助 Controller：投产批次、二级区域、工作日历、日报白名单
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;

// ============================================================
// 1. 投产批次 (ProductionBatchController) — 完整 CRUD
// ============================================================
test.describe.serial('投产批次管理 CRUD', () => {
  const BATCH_NO = 'E2E-B-' + Date.now();
  let createdBatchId = null;

  test.beforeAll(async () => {
    api = await setupApi();
  });

  test('列表查询', async () => {
    console.log('>>> 查询投产批次分页列表');
    const res = await api.get('/project/productionBatch/list', { pageNum: 1, pageSize: 10 });
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(Array.isArray(res.rows), 'rows 应为数组').toBe(true);
    expect(typeof res.total, 'total 应为数字').toBe('number');
    console.log(`<<< 批次总数: ${res.total}, 当前页: ${res.rows.length}`);
  });

  test('批次选项查询 (batchNoOptions)', async () => {
    console.log('>>> 查询批次下拉选项');
    const res = await api.get('/project/productionBatch/batchNoOptions');
    expect(res.code).toBe(200);
    expect(Array.isArray(res.data), 'data 应为数组').toBe(true);
    console.log(`<<< 批次选项数量: ${res.data.length}`);
  });

  test('按年度查询 (byYear)', async () => {
    console.log('>>> 按年度查询投产批次');
    const res = await api.get('/project/productionBatch/byYear', { productionYear: '2026' });
    expect(res.code).toBe(200);
    expect(Array.isArray(res.data), 'data 应为数组').toBe(true);
    console.log(`<<< 2026年批次数量: ${res.data.length}`);
  });

  test('新增批次', async () => {
    console.log(`>>> 新增投产批次: "${BATCH_NO}"`);
    const res = await api.post('/project/productionBatch', {
      batchNo: BATCH_NO,
      productionYear: '2026',
      sortOrder: 999,
      planProductionDate: '2026-12-31'
    });
    expect(res.code, '新增应返回 200').toBe(200);
    expect(res.msg).toBe('操作成功');
    console.log('<<< 新增批次成功');
  });

  test('详情查询', async () => {
    console.log('>>> 查找刚新增的批次');
    const listRes = await api.get('/project/productionBatch/list', {
      pageNum: 1,
      pageSize: 50,
      batchNo: BATCH_NO
    });
    expect(listRes.code).toBe(200);

    const found = listRes.rows.find(b => b.batchNo === BATCH_NO);
    expect(found, '应能在列表中找到新增批次').toBeTruthy();
    createdBatchId = found.batchId;
    console.log(`>>> 找到批次 ID: ${createdBatchId}, 查询详情`);

    const detailRes = await api.get(`/project/productionBatch/${createdBatchId}`);
    expect(detailRes.code).toBe(200);
    expect(detailRes.data).toBeTruthy();
    expect(detailRes.data.batchNo).toBe(BATCH_NO);
    console.log(`<<< 批次详情确认: ${detailRes.data.batchNo}`);
  });

  test('修改批次', async () => {
    expect(createdBatchId, '需要先创建批次').toBeTruthy();
    console.log(`>>> 修改批次排序为 888`);

    const res = await api.put('/project/productionBatch', {
      batchId: createdBatchId,
      batchNo: BATCH_NO,
      productionYear: '2026',
      sortOrder: 888
    });
    expect(res.code, '修改应返回 200').toBe(200);
    expect(res.msg).toBe('操作成功');

    // 验证修改结果
    const detailRes = await api.get(`/project/productionBatch/${createdBatchId}`);
    expect(detailRes.code).toBe(200);
    expect(detailRes.data.sortOrder).toBe(888);
    console.log('<<< 修改批次成功并已验证');
  });

  test('删除批次', async () => {
    expect(createdBatchId, '需要先创建批次').toBeTruthy();
    console.log(`>>> 删除批次 ID: ${createdBatchId}`);

    const res = await api.del(`/project/productionBatch/${createdBatchId}`);
    expect(res.code, '删除应返回 200').toBe(200);
    expect(res.msg).toBe('操作成功');

    // 验证已删除
    const listRes = await api.get('/project/productionBatch/list', {
      pageNum: 1,
      pageSize: 50,
      batchNo: BATCH_NO
    });
    const stillExists = listRes.rows.find(b => b.batchId === createdBatchId);
    expect(stillExists, '已删除的批次不应出现在列表中').toBeFalsy();
    console.log('<<< 删除批次成功并已验证');
    createdBatchId = null;
  });

  // 清理：如果测试中途失败，确保测试批次被删除
  test.afterAll(async () => {
    if (createdBatchId) {
      console.log(`>>> 清理: 删除残留批次 ID: ${createdBatchId}`);
      try {
        await api.del(`/project/productionBatch/${createdBatchId}`);
        console.log('<<< 清理完成');
      } catch (e) {
        console.log(`<<< 清理失败: ${e.message}`);
      }
    }
    await api?.dispose();
  });
});

// ============================================================
// 2. 二级区域 (SecondaryRegionController) — 只读测试
// ============================================================
test.describe.serial('二级区域查询', () => {
  test.beforeAll(async () => {
    api = await setupApi();
  });

  test('列表查询', async () => {
    console.log('>>> 查询二级区域分页列表');
    const res = await api.get('/project/secondaryRegion/list', { pageNum: 1, pageSize: 10 });
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(Array.isArray(res.rows), 'rows 应为数组').toBe(true);
    expect(typeof res.total, 'total 应为数字').toBe('number');
    console.log(`<<< 二级区域总数: ${res.total}, 当前页: ${res.rows.length}`);
  });

  test('按区域查询 (listByRegion)', async () => {
    console.log('>>> 先获取已有区域的 regionDictValue');
    const listRes = await api.get('/project/secondaryRegion/list', { pageNum: 1, pageSize: 1 });
    expect(listRes.code).toBe(200);

    if (listRes.rows.length === 0) {
      console.log('<<< 无二级区域数据，跳过');
      test.skip();
      return;
    }

    const regionDictValue = listRes.rows[0].parentRegionDictValue || listRes.rows[0].regionDictValue;
    console.log(`>>> 按区域查询: regionDictValue=${regionDictValue}`);
    const res = await api.get('/project/secondaryRegion/listByRegion', { regionDictValue });
    expect(res.code).toBe(200);
    expect(Array.isArray(res.data), 'data 应为数组').toBe(true);
    console.log(`<<< 该区域下二级区域数量: ${res.data.length}`);
  });

  test.afterAll(async () => {
    await api?.dispose();
  });
});

// ============================================================
// 3. 工作日历 (WorkCalendarController) — 只读测试
// ============================================================
test.describe.serial('工作日历查询', () => {
  test.beforeAll(async () => {
    api = await setupApi();
  });

  test('列表查询', async () => {
    console.log('>>> 查询工作日历分页列表');
    const res = await api.get('/project/workCalendar/list', { pageNum: 1, pageSize: 10 });
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(Array.isArray(res.rows), 'rows 应为数组').toBe(true);
    expect(typeof res.total, 'total 应为数字').toBe('number');
    console.log(`<<< 工作日历总数: ${res.total}, 当前页: ${res.rows.length}`);
  });

  test('按年度查询 (year/2026)', async () => {
    console.log('>>> 查询2026年工作日历');
    const res = await api.get('/project/workCalendar/year/2026');
    expect(res.code).toBe(200);
    expect(Array.isArray(res.data), 'data 应为数组').toBe(true);
    console.log(`<<< 2026年日历条目数: ${res.data.length}`);
  });

  test.afterAll(async () => {
    await api?.dispose();
  });
});

// ============================================================
// 4. 日报白名单 (DailyReportWhitelistController) — 只读测试
// ============================================================
test.describe.serial('日报白名单查询', () => {
  test.beforeAll(async () => {
    api = await setupApi();
  });

  test('列表查询', async () => {
    console.log('>>> 查询日报白名单分页列表');
    const res = await api.get('/project/whitelist/list', { pageNum: 1, pageSize: 10 });
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(Array.isArray(res.rows), 'rows 应为数组').toBe(true);
    expect(typeof res.total, 'total 应为数字').toBe('number');
    console.log(`<<< 白名单总数: ${res.total}, 当前页: ${res.rows.length}`);
  });

  test('自查是否在白名单 (checkSelf)', async () => {
    console.log('>>> 检查当前用户是否在白名单');
    const res = await api.get('/project/whitelist/checkSelf');
    expect(res.code).toBe(200);
    expect(typeof res.data, 'data 应为布尔值').toBe('boolean');
    console.log(`<<< 当前用户在白名单中: ${res.data}`);
  });

  test.afterAll(async () => {
    await api?.dispose();
  });
});
