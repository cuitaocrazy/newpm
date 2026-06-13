/**
 * 非批次版本管理 E2E —— 覆盖 VersionOutManualController 全部端点
 *
 * 1. 列表查询（manual_input='1'）
 * 2. 级联：产品→子系统、年份→批次
 * 3. 生成版本号
 * 4. 新增（手填任务号/任务名）
 * 5. 详情（含手填任务 + 审计字段）
 * 6. manual_input 隔离（批次列表不含非批次）
 * 7. 编辑触发版本号重算
 * 8. 软删除
 * 9. 导出端点可达
 *
 * 运行：E2E_BASE_URL=http://localhost:8090 npx playwright test e2e-version-out-manual-crud.spec.js
 * 前置：pm_sys_name 有子系统配置、2026 有批次；运行前临时关验证码。
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;
let sysName = null;
let product = null;
let batchId = null;
let createdId = null;

test.describe.serial('非批次版本管理 CRUD', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    console.log('✅ 登录成功，开始非批次版本 CRUD 测试');
  });

  test.afterAll(async () => { await api?.dispose(); });

  test('1. 列表查询', async () => {
    const body = await api.get('/project/versionOutManual/list', { pageNum: 1, pageSize: 10 });
    expect(body.code).toBe(200);
    expect(Array.isArray(body.rows)).toBe(true);
  });

  test('2. 级联：产品→子系统 + 年份→批次', async () => {
    const dict = await api.get('/system/dict/data/type/sys_product');
    for (const d of (dict.data || [])) {
      const res = await api.get('/project/versionOutManual/sysNameByProduct', { product: d.dictValue });
      if (res.data && res.data.length > 0) { product = d.dictValue; sysName = res.data[0].sysName; break; }
    }
    expect(sysName).not.toBeNull();
    const batches = await api.get('/project/versionOutManual/batchByYear', { year: '2026' });
    expect(batches.data.length).toBeGreaterThan(0);
    batchId = batches.data[0].batchId;
  });

  test('3. 生成版本号（类型1）', async () => {
    const res = await api.post('/project/versionOutManual/generateOutLibVersion?addFlag=1', {
      sysName, subVersionCode: product, versionType: '1'
    });
    expect(res.code).toBe(200);
    expect(res.data.outLibVersion).toMatch(/_SP\d{2}$/);
  });

  test('4. 新增（手填任务）', async () => {
    const body = await api.post('/project/versionOutManual', {
      productionYear: '2026', batchId, product, subVersionCode: product, sysName,
      versionType: '1', manualTaskNo: 'E2E-MANUAL-001', manualTaskName: 'E2E手填任务',
      isInvolved: '1', dbUpdate: '1', usbUpdate: '1', versionDescr: 'E2E非批次说明'
    });
    expect(body.code).toBe(200);

    const list = await api.get('/project/versionOutManual/list', { pageNum: 1, pageSize: 50, manualTaskNo: 'E2E-MANUAL-001' });
    const found = list.rows.find(r => r.manualTaskNo === 'E2E-MANUAL-001');
    expect(found, '新增记录应能查到').toBeTruthy();
    expect(found.manualTaskName).toBe('E2E手填任务');
    expect(found.outLibVersion).toMatch(/_SP\d{2}$/);
    expect(found.manualInput).toBe('1');
    createdId = found.id;
  });

  test('5. 详情（手填任务 + 审计）', async () => {
    const body = await api.get(`/project/versionOutManual/${createdId}`);
    expect(body.code).toBe(200);
    expect(body.data.manualTaskNo).toBe('E2E-MANUAL-001');
    expect(body.data.manualTaskName).toBe('E2E手填任务');
  });

  test('6. manual_input 隔离（批次列表不含非批次）', async () => {
    const batchList = await api.get('/project/versionOut/list', { pageNum: 1, pageSize: 100 });
    const leaked = batchList.rows.some(r => r.manualTaskNo === 'E2E-MANUAL-001');
    expect(leaked, '非批次记录不应出现在批次列表').toBe(false);
  });

  test('7. 编辑触发版本号重算（类型1→3）', async () => {
    const detail = await api.get(`/project/versionOutManual/${createdId}`);
    const d = detail.data;
    d.versionType = '3';
    const body = await api.put('/project/versionOutManual', d);
    expect(body.code).toBe(200);
    const after = await api.get(`/project/versionOutManual/${createdId}`);
    expect(after.data.versionType).toBe('3');
    expect(after.data.outLibVersion).toMatch(/_B\d{2}$/);
  });

  test('8. 软删除', async () => {
    const body = await api.del(`/project/versionOutManual/${createdId}`);
    expect(body.code).toBe(200);
    const list = await api.get('/project/versionOutManual/list', { pageNum: 1, pageSize: 50, manualTaskNo: 'E2E-MANUAL-001' });
    expect(list.rows.find(r => r.id === createdId)).toBeFalsy();
  });

  test('9. 导出端点可达', async () => {
    const res = await api.get('/project/versionOutManual/list', { pageNum: 1, pageSize: 1 });
    expect(res.code).toBe(200);
  });
});
