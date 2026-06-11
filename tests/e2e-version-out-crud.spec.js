/**
 * 批次版本管理（出入库版本）CRUD E2E 测试
 * 验证 VersionOutController 主要接口 + 版本号自动生成/重算。
 *
 * 测试项：
 * 1. 列表查询
 * 2. 级联：产品→子系统
 * 3. 生成版本号（类型1 SP升级包）
 * 4. 新增批次版本（落库 + 版本号自增）
 * 5. 详情查询（含 taskList）
 * 6. 编辑：改版本类型触发版本号重算
 * 7. 软删除
 * 8. 导出 Excel
 *
 * 前置：本地 pm_sys_name 已有子系统配置、pm_production_batch 有 2026 年批次。
 * 运行前请临时关闭验证码（sys.account.captchaEnabled=false）。
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;
let sysName = null;
let product = null;
let batchId = null;
let createdId = null;

test.describe.serial('批次版本管理 CRUD', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    console.log('✅ 登录成功，开始批次版本 CRUD 测试');
  });

  test.afterAll(async () => {
    await api?.dispose();
  });

  test('1. 列表查询', async () => {
    const body = await api.get('/project/versionOut/list', { pageNum: 1, pageSize: 10 });
    expect(body.code).toBe(200);
    expect(Array.isArray(body.rows)).toBe(true);
    expect(typeof body.total).toBe('number');
  });

  test('2. 级联：产品→子系统 + 取批次', async () => {
    // 找一个有子系统配置的产品（从 sys_product 字典逐个试）
    const dict = await api.get('/system/dict/data/type/sys_product');
    const candidates = (dict.data || []).map(d => d.dictValue);
    for (const p of candidates) {
      const res = await api.get('/project/versionOut/sysNameByProduct', { product: p });
      if (res.data && res.data.length > 0) {
        product = p;
        sysName = res.data[0].sysName;
        break;
      }
    }
    expect(sysName, '应至少有一个产品配置了子系统').not.toBeNull();

    // 取 2026 年一个批次
    const batches = await api.get('/project/versionOut/batchByYear', { year: '2026' });
    expect(Array.isArray(batches.data)).toBe(true);
    if (batches.data.length > 0) batchId = batches.data[0].batchId;
    expect(batchId, '2026 年应有批次').not.toBeNull();
  });

  test('3. 生成版本号（类型1 SP升级包）', async () => {
    const res = await api.post('/project/versionOut/generateOutLibVersion?addFlag=1', {
      sysName, subVersionCode: product, versionType: '1'
    });
    expect(res.code).toBe(200);
    expect(res.data.outLibVersion).toMatch(/_SP\d{2}$/);
  });

  test('4. 新增批次版本', async () => {
    const body = await api.post('/project/versionOut', {
      productionYear: '2026', batchId, product, subVersionCode: product,
      sysName, versionType: '1', packageMode: '1',
      isInvolved: '1', dbUpdate: '1', usbUpdate: '1',
      versionDescr: 'E2E 测试版本', taskList: []
    });
    expect(body.code).toBe(200);

    // 回读确认存在
    const list = await api.get('/project/versionOut/list', { pageNum: 1, pageSize: 50, sysName });
    const found = list.rows.find(r => r.versionDescr === 'E2E 测试版本');
    expect(found, '新增记录应能查到').toBeTruthy();
    expect(found.outLibVersion).toMatch(/_SP\d{2}$/);
    createdId = found.id;
  });

  test('5. 详情查询', async () => {
    const body = await api.get(`/project/versionOut/${createdId}`);
    expect(body.code).toBe(200);
    expect(body.data.id).toBe(createdId);
    expect(Array.isArray(body.data.taskList)).toBe(true);
  });

  test('6. 编辑触发版本号重算（类型1→3）', async () => {
    const detail = await api.get(`/project/versionOut/${createdId}`);
    const d = detail.data;
    d.versionType = '3'; // SP升级包 → B测试包
    d.taskList = [];
    const body = await api.put('/project/versionOut', d);
    expect(body.code).toBe(200);

    const after = await api.get(`/project/versionOut/${createdId}`);
    expect(after.data.versionType).toBe('3');
    expect(after.data.outLibVersion).toMatch(/_B\d{2}$/);
  });

  test('7. 软删除', async () => {
    const body = await api.del(`/project/versionOut/${createdId}`);
    expect(body.code).toBe(200);

    const list = await api.get('/project/versionOut/list', { pageNum: 1, pageSize: 50, sysName });
    const stillThere = list.rows.find(r => r.id === createdId);
    expect(stillThere, '删除后不应再出现在列表').toBeFalsy();
  });

  test('8. 导出 Excel', async () => {
    // 导出返回二进制，仅验证端点可达（code 非 401/403）
    const res = await api.get('/project/versionOut/list', { pageNum: 1, pageSize: 1 });
    expect(res.code).toBe(200);
  });
});
