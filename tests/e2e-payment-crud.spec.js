/**
 * 款项管理 (Payment) CRUD E2E 测试
 *
 * PaymentController: /project/payment
 * 测试流程：列表查询 → 聚合查询 → 金额合计 → 详情 → 名称搜索
 *          → 新增 → 修改 → 附件检查 → 删除（自清理）
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;

/** 测试过程中创建的款项 ID，用于清理 */
let createdPaymentId = null;

test.describe.serial('款项管理 CRUD', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    console.log('✅ 登录成功，开始款项管理测试');
  });

  test.afterAll(async () => {
    // 自清理：如果测试中途失败，确保删除已创建的款项
    if (createdPaymentId) {
      try {
        const res = await api.del(`/project/payment/${createdPaymentId}`);
        if (res.code === 200) {
          console.log(`🧹 afterAll 清理：已删除款项 ${createdPaymentId}`);
        }
      } catch {
        // 可能已被删除测试清理，忽略
      }
      createdPaymentId = null;
    }
    await api?.dispose();
  });

  // ─────────────────────────────────────────────
  // 1. 款项列表查询
  // ─────────────────────────────────────────────
  test('款项列表查询', async () => {
    const res = await api.get('/project/payment/list', { pageNum: 1, pageSize: 10 });
    expect(res.code, '接口应返回200').toBe(200);
    expect(res.rows, '应返回数组').toBeInstanceOf(Array);
    expect(typeof res.total).toBe('number');
    console.log(`📋 款项列表查询成功，共 ${res.total} 条，本页 ${res.rows.length} 条`);
  });

  // ─────────────────────────────────────────────
  // 2. 合同+款项聚合查询
  // ─────────────────────────────────────────────
  test('合同+款项聚合查询', async () => {
    const res = await api.get('/project/payment/listWithContracts', { pageNum: 1, pageSize: 10 });
    expect(res.code, '接口应返回200').toBe(200);
    expect(res.rows, '应返回数组').toBeInstanceOf(Array);
    expect(typeof res.total).toBe('number');

    if (res.rows.length > 0) {
      const first = res.rows[0];
      // 聚合查询的行应包含合同信息
      const hasContractInfo = first.contractName !== undefined
        || first.contractCode !== undefined
        || first.contractId !== undefined;
      expect(hasContractInfo, '聚合行应包含合同信息').toBe(true);
      console.log(`📋 聚合查询成功，共 ${res.total} 条，首行含合同字段`);
    } else {
      console.log('📋 聚合查询成功，暂无数据');
    }
  });

  // ─────────────────────────────────────────────
  // 3. 款项金额合计
  // ─────────────────────────────────────────────
  test('款项金额合计', async () => {
    const res = await api.get('/project/payment/sumPaymentAmount');
    expect(res.code, '接口应返回200').toBe(200);
    expect(res.data, '应返回合计数据').toBeDefined();
    console.log('💰 款项金额合计:', JSON.stringify(res.data));
  });

  // ─────────────────────────────────────────────
  // 4. 款项详情
  // ─────────────────────────────────────────────
  test('款项详情', async () => {
    // 先获取列表中第一条
    const listRes = await api.get('/project/payment/list', { pageNum: 1, pageSize: 1 });
    expect(listRes.code).toBe(200);

    if (listRes.rows.length === 0) {
      console.log('⚠️ 无款项数据，跳过详情测试');
      test.skip();
      return;
    }

    const paymentId = listRes.rows[0].paymentId;
    const res = await api.get(`/project/payment/${paymentId}`);
    expect(res.code, '接口应返回200').toBe(200);
    expect(res.data, '应返回款项详情').toBeDefined();
    expect(res.data.paymentId).toBe(paymentId);
    console.log(`🔍 款项详情查询成功，ID=${paymentId}，名称=${res.data.paymentMethodName || '无'}`);
  });

  // ─────────────────────────────────────────────
  // 5. 款项名称搜索
  // ─────────────────────────────────────────────
  test('款项名称搜索', async () => {
    const res = await api.get('/project/payment/searchPaymentMethodNames', { keyword: '款' });
    expect(res.code, '接口应返回200').toBe(200);
    // data 应为字符串列表（可能为空）
    if (res.data) {
      expect(res.data).toBeInstanceOf(Array);
      console.log(`🔎 款项名称搜索结果：${res.data.length} 个匹配`);
    } else {
      console.log('🔎 款项名称搜索无匹配结果');
    }
  });

  // ─────────────────────────────────────────────
  // 6. 新增款项
  // ─────────────────────────────────────────────
  test('新增款项', async () => {
    // 先查找一个有效的 contractId
    const contractRes = await api.get('/project/contract/list', { pageNum: 1, pageSize: 10 });
    expect(contractRes.code, '合同列表应返回200').toBe(200);
    expect(contractRes.rows.length, '应至少有一个合同用于测试').toBeGreaterThan(0);

    const contractId = contractRes.rows[0].contractId;
    const paymentName = `E2E测试款项_${Date.now()}`;

    const res = await api.post('/project/payment', {
      contractId,
      paymentMethodName: paymentName,
      paymentAmount: 10000
    });
    expect(res.code, '新增款项应成功').toBe(200);
    console.log(`✅ 新增款项成功，合同ID=${contractId}，名称=${paymentName}`);

    // 从列表中找到刚创建的款项以获取 ID
    const listRes = await api.get('/project/payment/list', {
      pageNum: 1,
      pageSize: 50,
      paymentMethodName: paymentName
    });
    expect(listRes.code).toBe(200);

    // 如果按名称搜索不到，取最新的一条
    if (listRes.rows.length > 0) {
      createdPaymentId = listRes.rows[0].paymentId;
    } else {
      // fallback: 取全量列表第一条（按创建时间倒序通常是最新的）
      const allRes = await api.get('/project/payment/list', { pageNum: 1, pageSize: 1 });
      if (allRes.rows.length > 0) {
        createdPaymentId = allRes.rows[0].paymentId;
      }
    }

    expect(createdPaymentId, '应能获取到新创建的款项ID').toBeTruthy();
    console.log(`📌 创建的款项ID=${createdPaymentId}`);
  });

  // ─────────────────────────────────────────────
  // 7. 修改款项
  // ─────────────────────────────────────────────
  test('修改款项', async () => {
    expect(createdPaymentId, '需要先成功新增款项').toBeTruthy();

    // 先获取详情
    const detailRes = await api.get(`/project/payment/${createdPaymentId}`);
    expect(detailRes.code).toBe(200);

    const payment = detailRes.data;
    const updatedAmount = 20000;

    const res = await api.put('/project/payment', {
      ...payment,
      paymentAmount: updatedAmount
    });
    expect(res.code, '修改款项应成功').toBe(200);

    // 验证修改生效
    const verifyRes = await api.get(`/project/payment/${createdPaymentId}`);
    expect(verifyRes.code).toBe(200);
    expect(Number(verifyRes.data.paymentAmount)).toBe(updatedAmount);
    console.log(`✏️ 修改款项成功，金额从 10000 → ${updatedAmount}`);
  });

  // ─────────────────────────────────────────────
  // 8. 检查款项附件
  // ─────────────────────────────────────────────
  test('检查款项附件', async () => {
    expect(createdPaymentId, '需要先成功新增款项').toBeTruthy();

    const res = await api.get(`/project/payment/checkAttachments/${createdPaymentId}`);
    expect(res.code, '接口应返回200').toBe(200);
    expect(res.data, '应返回附件数量').toBeDefined();
    console.log(`📎 款项附件数量：${res.data}`);
  });

  // ─────────────────────────────────────────────
  // 9. 删除款项
  // ─────────────────────────────────────────────
  test('删除款项', async () => {
    expect(createdPaymentId, '需要先成功新增款项').toBeTruthy();

    const res = await api.del(`/project/payment/${createdPaymentId}`);
    expect(res.code, '删除款项应成功').toBe(200);
    console.log(`🗑️ 删除款项成功，ID=${createdPaymentId}`);

    // 验证已删除
    const verifyRes = await api.get(`/project/payment/${createdPaymentId}`);
    // 删除后查详情应返回空 data 或 500
    const isDeleted = verifyRes.data === null || verifyRes.data === undefined || verifyRes.code === 500;
    expect(isDeleted, '款项应已被删除').toBe(true);

    createdPaymentId = null; // 标记已清理，afterAll 不再重复删除
    console.log('✅ 款项删除验证通过，测试数据已清理');
  });
});
