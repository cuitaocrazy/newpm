/**
 * 付款里程碑「清空可空字段」回归测试 —— Issue #7
 *
 * 背景：
 *   编辑页把「实际回款季度」清空并保存，提示成功但列表仍显示原值。
 *   根因两处叠加：
 *     1. 前端 el-select clearable 清空时 emit undefined，JSON.stringify 直接丢弃该 key；
 *     2. 后端 PaymentMapper.updatePayment 的 <if test="xxx != null"> 守卫拦下 null，
 *        SQL 中不生成该列的赋值语句 → 数据库保留旧值。
 *
 * 本用例锁定两条路径都能真正清空：
 *   A. payload 中该 key 缺失（前端 undefined 被丢弃的真实形态）
 *   B. payload 中该 key 显式为 null
 *
 * PaymentController: /project/payment
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;

/** 测试过程中创建的款项 ID，用于清理 */
let createdPaymentId = null;

/**
 * 表单上的可选字段 —— 用户可以清空，清空后必须落库。
 * 与 PaymentMapper.updatePayment 中无条件更新的字段一一对应。
 */
const CLEARABLE_FIELDS = {
  expectedQuarter: '202603',
  actualQuarter: '202602',
  submitAcceptanceDate: '2025-12-09',
  actualPaymentDate: '2026-01-15',
  confirmYear: '2026',
  remark: 'E2E清空回归-初始备注'
};

/**
 * 表单上的必填字段 —— 前端 rules 拦住清空，mapper 保留 <if> 守卫作为兜底。
 * 即使 payload 中 key 缺失也必须保留原值，不得被静默清空。
 */
const GUARDED_FIELDS = {
  paymentStatus: 'YTJFP',
  penaltyAmount: 100
};

/** 断言字段已被清空（null 或空串都算清空） */
function expectCleared(payment, field) {
  const value = payment[field];
  expect(
    value === null || value === undefined || value === '',
    `字段 ${field} 应已被清空，实际为「${value}」`
  ).toBe(true);
}

test.describe.serial('付款里程碑清空可空字段（Issue #7）', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    console.log('✅ 登录成功，开始付款里程碑清空回归测试');
  });

  test.afterAll(async () => {
    if (createdPaymentId) {
      try {
        await api.del(`/project/payment/${createdPaymentId}`);
        console.log(`🧹 afterAll 清理：已删除款项 ${createdPaymentId}`);
      } catch {
        // 可能已被清理，忽略
      }
      createdPaymentId = null;
    }
    await api?.dispose();
  });

  // ─────────────────────────────────────────────
  // 前置：创建一条各可空字段均有值的款项
  // ─────────────────────────────────────────────
  test('前置：创建带完整可空字段的款项', async () => {
    const contractRes = await api.get('/project/contract/list', { pageNum: 1, pageSize: 10 });
    expect(contractRes.code, '合同列表应返回200').toBe(200);
    expect(contractRes.rows.length, '应至少有一个合同用于测试').toBeGreaterThan(0);

    const contractId = contractRes.rows[0].contractId;
    const paymentName = `E2E清空回归_${Date.now()}`;

    const res = await api.post('/project/payment', {
      contractId,
      paymentMethodName: paymentName,
      paymentAmount: 10000,
      hasPenalty: '1',
      ...CLEARABLE_FIELDS,
      ...GUARDED_FIELDS
    });
    expect(res.code, '新增款项应成功').toBe(200);

    const listRes = await api.get('/project/payment/list', {
      pageNum: 1,
      pageSize: 50,
      paymentMethodName: paymentName
    });
    expect(listRes.code).toBe(200);
    expect(listRes.rows.length, '应能按名称检索到刚创建的款项').toBeGreaterThan(0);
    createdPaymentId = listRes.rows[0].paymentId;

    // 确认各字段确实写入成功，否则后面的「清空」验证没有意义
    const detailRes = await api.get(`/project/payment/${createdPaymentId}`);
    expect(detailRes.code).toBe(200);
    for (const field of [...Object.keys(CLEARABLE_FIELDS), ...Object.keys(GUARDED_FIELDS)]) {
      expect(detailRes.data[field], `前置数据字段 ${field} 应有值`).toBeTruthy();
    }
    console.log(`📌 前置款项已创建，ID=${createdPaymentId}，可选字段与必填字段均已赋值`);
  });

  // ─────────────────────────────────────────────
  // A. key 缺失场景 —— 前端 undefined 被 JSON.stringify 丢弃的真实形态
  // ─────────────────────────────────────────────
  test('清空后提交：payload 中 key 缺失，字段应被真正清空', async () => {
    expect(createdPaymentId, '需要先成功创建前置款项').toBeTruthy();

    const detailRes = await api.get(`/project/payment/${createdPaymentId}`);
    expect(detailRes.code).toBe(200);

    // 模拟前端：用户清空了这些字段 → v-model 变 undefined → JSON 序列化后 key 消失
    // 必填字段的 key 也一并删除，用于验证守卫兜底是否生效
    const submitData = { ...detailRes.data };
    for (const field of [...Object.keys(CLEARABLE_FIELDS), ...Object.keys(GUARDED_FIELDS)]) {
      delete submitData[field];
    }

    const putRes = await api.put('/project/payment', submitData);
    expect(putRes.code, '修改款项应成功').toBe(200);

    const afterRes = await api.get(`/project/payment/${createdPaymentId}`);
    expect(afterRes.code).toBe(200);

    // 可选字段：必须被清空
    for (const field of Object.keys(CLEARABLE_FIELDS)) {
      expectCleared(afterRes.data, field);
    }
    // 必填字段：守卫兜底，必须保留原值
    for (const [field, value] of Object.entries(GUARDED_FIELDS)) {
      expect(
        String(afterRes.data[field]),
        `必填字段 ${field} 不得被静默清空`
      ).toContain(String(value));
    }
    console.log('✅ key 缺失场景：可选字段已清空，必填字段保留原值');
  });

  // ─────────────────────────────────────────────
  // B. 显式 null 场景 —— 前端归一化后的形态
  // ─────────────────────────────────────────────
  test('清空后提交：payload 中 key 显式为 null，字段应被真正清空', async () => {
    expect(createdPaymentId, '需要先成功创建前置款项').toBeTruthy();

    // 先把字段重新填回去，构造可清空的初始状态
    const detailRes = await api.get(`/project/payment/${createdPaymentId}`);
    expect(detailRes.code).toBe(200);
    const refillRes = await api.put('/project/payment', {
      ...detailRes.data,
      ...CLEARABLE_FIELDS
    });
    expect(refillRes.code, '回填字段应成功').toBe(200);

    const refilled = await api.get(`/project/payment/${createdPaymentId}`);
    for (const field of Object.keys(CLEARABLE_FIELDS)) {
      expect(refilled.data[field], `回填后字段 ${field} 应有值`).toBeTruthy();
    }

    // 显式传 null 清空
    const nulled = { ...refilled.data };
    for (const field of Object.keys(CLEARABLE_FIELDS)) {
      nulled[field] = null;
    }
    const putRes = await api.put('/project/payment', nulled);
    expect(putRes.code, '修改款项应成功').toBe(200);

    const afterRes = await api.get(`/project/payment/${createdPaymentId}`);
    expect(afterRes.code).toBe(200);
    for (const field of Object.keys(CLEARABLE_FIELDS)) {
      expectCleared(afterRes.data, field);
    }
    console.log('✅ 显式 null 场景：全部可空字段已清空');
  });

  // ─────────────────────────────────────────────
  // C. 反向保护 —— 有值的字段不能被误清空
  // ─────────────────────────────────────────────
  test('反向保护：正常提交有值字段时不得被误清空', async () => {
    expect(createdPaymentId, '需要先成功创建前置款项').toBeTruthy();

    const detailRes = await api.get(`/project/payment/${createdPaymentId}`);
    const putRes = await api.put('/project/payment', {
      ...detailRes.data,
      ...CLEARABLE_FIELDS,
      paymentAmount: 20000
    });
    expect(putRes.code, '修改款项应成功').toBe(200);

    const afterRes = await api.get(`/project/payment/${createdPaymentId}`);
    expect(afterRes.code).toBe(200);
    for (const [field, value] of Object.entries(CLEARABLE_FIELDS)) {
      expect(String(afterRes.data[field]), `字段 ${field} 应保留提交值`).toContain(String(value));
    }
    expect(Number(afterRes.data.paymentAmount), '付款金额应更新为20000').toBe(20000);
    console.log('✅ 反向保护：有值字段正常保存，未被误清空');
  });

  // ─────────────────────────────────────────────
  // 清理
  // ─────────────────────────────────────────────
  test('清理：删除测试款项', async () => {
    expect(createdPaymentId, '需要先成功创建前置款项').toBeTruthy();
    const res = await api.del(`/project/payment/${createdPaymentId}`);
    expect(res.code, '删除款项应成功').toBe(200);
    createdPaymentId = null;
    console.log('🧹 测试款项已删除');
  });
});
