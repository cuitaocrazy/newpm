/**
 * 合同管理 CRUD E2E 测试
 * 验证 ContractController 所有主要接口的正确性
 *
 * 测试项：
 * 1. 合同列表查询
 * 2. 合同详情查询
 * 3. 合同搜索
 * 4. 合同编号唯一性检查
 * 5. 新增合同
 * 6. 修改合同
 * 7. 删除合同
 * 8. 删除不存在的合同
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;
const TIMESTAMP = Date.now();
let createdContractId = null;

test.describe.serial('合同管理 CRUD', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    console.log('✅ 登录成功，开始合同 CRUD 测试');
  });

  test.afterAll(async () => {
    await api?.dispose();
  });

  // ─────────────────────────────────────────────
  // 1. 合同列表查询
  // ─────────────────────────────────────────────
  test('合同列表查询', async () => {
    console.log('\n▶ 测试：合同列表查询');

    const body = await api.get('/project/contract/list', { pageNum: 1, pageSize: 10 });

    expect(body.code, '响应 code 应为 200').toBe(200);
    expect(Array.isArray(body.rows), 'rows 应为数组').toBe(true);
    expect(typeof body.total, 'total 应为数字').toBe('number');
    expect(body.total).toBeGreaterThanOrEqual(0);

    console.log(`  ✅ 列表查询成功，total=${body.total}, 返回 ${body.rows.length} 条`);
  });

  // ─────────────────────────────────────────────
  // 2. 合同详情查询
  // ─────────────────────────────────────────────
  test('合同详情查询', async () => {
    console.log('\n▶ 测试：合同详情查询');

    // 先获取列表中第一条合同
    const listBody = await api.get('/project/contract/list', { pageNum: 1, pageSize: 1 });
    expect(listBody.code).toBe(200);

    if (!listBody.rows || listBody.rows.length === 0) {
      console.log('  ⏭ 列表为空，跳过详情查询测试');
      test.skip();
      return;
    }

    const firstContract = listBody.rows[0];
    const contractId = firstContract.contractId;
    console.log(`  查询合同详情，contractId=${contractId}`);

    const body = await api.get(`/project/contract/${contractId}`);

    expect(body.code, '响应 code 应为 200').toBe(200);
    expect(body.data, 'data 不应为空').toBeTruthy();
    expect(body.data.contractId, 'contractId 应匹配').toBe(contractId);

    console.log(`  ✅ 详情查询成功，合同名称: ${body.data.contractName}`);
  });

  // ─────────────────────────────────────────────
  // 3. 合同搜索
  // ─────────────────────────────────────────────
  test('合同搜索', async () => {
    console.log('\n▶ 测试：合同搜索');

    // 先获取一个已有合同名称作为搜索关键词
    const listBody = await api.get('/project/contract/list', { pageNum: 1, pageSize: 1 });
    expect(listBody.code).toBe(200);

    if (!listBody.rows || listBody.rows.length === 0) {
      console.log('  ⏭ 列表为空，跳过搜索测试');
      test.skip();
      return;
    }

    // 取合同名称的前两个字符作为关键词
    const keyword = listBody.rows[0].contractName.substring(0, 2);
    console.log(`  搜索关键词: "${keyword}"`);

    const body = await api.get('/project/contract/search', { keyword });

    expect(body.code, '响应 code 应为 200').toBe(200);
    expect(Array.isArray(body.data), '搜索结果应为数组').toBe(true);

    console.log(`  ✅ 搜索成功，匹配 ${body.data.length} 条`);
  });

  // ─────────────────────────────────────────────
  // 4. 合同编号唯一性检查
  // ─────────────────────────────────────────────
  test('合同编号唯一性检查', async () => {
    console.log('\n▶ 测试：合同编号唯一性检查');

    // 获取一个已有合同编号
    const listBody = await api.get('/project/contract/list', { pageNum: 1, pageSize: 1 });
    expect(listBody.code).toBe(200);

    if (!listBody.rows || listBody.rows.length === 0) {
      console.log('  ⏭ 列表为空，跳过唯一性检查测试');
      test.skip();
      return;
    }

    const existingCode = listBody.rows[0].contractCode;
    console.log(`  检查已有编号: "${existingCode}"`);

    const body = await api.get('/project/contract/checkContractCodeUnique', {
      contractCode: existingCode
    });

    expect(body.code, '响应 code 应为 200').toBe(200);
    // 已存在的编号应返回 false（不唯一）
    expect(body.data, '已存在的编号应返回 false（不唯一）').toBe(false);

    console.log(`  ✅ 唯一性检查成功，已有编号返回: ${body.data}`);
  });

  // ─────────────────────────────────────────────
  // 5. 新增合同
  // ─────────────────────────────────────────────
  test('新增合同', async () => {
    console.log('\n▶ 测试：新增合同');

    // 先获取一个有效的 customerId
    const customerList = await api.get('/project/customer/list', { pageNum: 1, pageSize: 1 });
    expect(customerList.code).toBe(200);
    const customerId = customerList.rows.length > 0 ? customerList.rows[0].customerId : null;

    const contractData = {
      contractName: `E2E测试合同_${TIMESTAMP}`,
      contractCode: `E2E-${TIMESTAMP}`,
      contractType: '1',
      contractStatus: '0',
      contractAmount: 10000.00,
      customerId: customerId,
      projectIds: []
    };

    console.log(`  新增合同: ${contractData.contractName}, customerId=${customerId}`);

    const body = await api.post('/project/contract', contractData);

    expect(body.code, '新增应返回 code 200').toBe(200);
    expect(body.msg).toBe('操作成功');

    // 通过列表查询找到刚创建的合同，获取 ID
    const listBody = await api.get('/project/contract/list', {
      pageNum: 1,
      pageSize: 10,
      contractName: `E2E测试合同_${TIMESTAMP}`
    });
    expect(listBody.code).toBe(200);
    expect(listBody.rows.length, '应能查到刚创建的合同').toBeGreaterThanOrEqual(1);

    const created = listBody.rows.find(r => r.contractCode === `E2E-${TIMESTAMP}`);
    expect(created, '应通过编号找到创建的合同').toBeTruthy();

    createdContractId = created.contractId;
    console.log(`  ✅ 新增成功，contractId=${createdContractId}`);
  });

  // ─────────────────────────────────────────────
  // 6. 修改合同
  // ─────────────────────────────────────────────
  test('修改合同', async () => {
    console.log('\n▶ 测试：修改合同');

    expect(createdContractId, '需要先创建合同').toBeTruthy();

    // 先查询完整数据
    const detailBody = await api.get(`/project/contract/${createdContractId}`);
    expect(detailBody.code).toBe(200);

    const updatedName = `E2E测试合同_修改_${TIMESTAMP}`;
    const updateData = {
      ...detailBody.data,
      contractName: updatedName
    };

    console.log(`  修改合同名称为: ${updatedName}`);

    const body = await api.put('/project/contract', updateData);

    expect(body.code, '修改应返回 code 200').toBe(200);
    expect(body.msg).toBe('操作成功');

    // 验证修改生效
    const verifyBody = await api.get(`/project/contract/${createdContractId}`);
    expect(verifyBody.code).toBe(200);
    expect(verifyBody.data.contractName, '合同名称应已更新').toBe(updatedName);

    console.log(`  ✅ 修改成功，名称已更新`);
  });

  // ─────────────────────────────────────────────
  // 7. 删除合同
  // ─────────────────────────────────────────────
  test('删除合同', async () => {
    console.log('\n▶ 测试：删除合同');

    expect(createdContractId, '需要先创建合同').toBeTruthy();

    console.log(`  删除合同 contractId=${createdContractId}`);

    const body = await api.del(`/project/contract/${createdContractId}`);

    expect(body.code, '删除应返回 code 200').toBe(200);
    expect(body.msg).toBe('操作成功');

    // 验证删除生效：再次查询应找不到（软删除，列表不显示）
    const listBody = await api.get('/project/contract/list', {
      pageNum: 1,
      pageSize: 10,
      contractName: `E2E测试合同_修改_${TIMESTAMP}`
    });
    expect(listBody.code).toBe(200);
    const found = listBody.rows.find(r => r.contractId === createdContractId);
    expect(found, '已删除的合同不应出现在列表中').toBeFalsy();

    console.log(`  ✅ 删除成功，列表中已无该合同`);

    // 清理引用，避免后续测试干扰
    createdContractId = null;
  });

  // ─────────────────────────────────────────────
  // 8. 删除不存在的合同
  // ─────────────────────────────────────────────
  test('删除不存在的合同', async () => {
    console.log('\n▶ 测试：删除不存在的合同');

    const body = await api.del('/project/contract/99999999');

    // 删除不存在的记录：可能返回 200（静默成功）或 500（报错）
    // 无论哪种，接口不应崩溃
    expect([200, 500], '应返回有效的响应码').toContain(body.code);

    console.log(`  ✅ 删除不存在合同返回 code=${body.code}, msg="${body.msg}"`);
  });
});
