/**
 * 客户管理 CRUD E2E 测试
 * 覆盖 CustomerController 全部端点：列表、详情、新增、修改、删除、唯一性校验、联系人查询
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;
const UNIQUE_SUFFIX = Date.now();

/** 测试用客户数据 */
const TEST_CUSTOMER = {
  customerAllName: `E2E测试客户全称_${UNIQUE_SUFFIX}`,
  customerSimpleName: `E2E简称_${UNIQUE_SUFFIX}`,
  customerContactList: [
    { contactName: '张三', contactPhone: '13800000001', contactTag: '技术负责人' },
    { contactName: '李四', contactPhone: '13800000002', contactTag: '商务对接' }
  ]
};

let createdCustomerId = null;

test.describe.serial('客户管理 CRUD', () => {
  test.beforeAll(async () => {
    api = await setupApi();
  });

  // 1. 客户列表查询
  test('客户列表查询', async () => {
    console.log('>>> 查询客户分页列表');
    const res = await api.get('/project/customer/list', { pageNum: 1, pageSize: 10 });
    expect(res.code, '返回 code 应为 200').toBe(200);
    expect(Array.isArray(res.rows), 'rows 应为数组').toBe(true);
    expect(typeof res.total, 'total 应为数字').toBe('number');
    expect(res.total).toBeGreaterThanOrEqual(0);
    console.log(`<<< 客户总数: ${res.total}, 当前页: ${res.rows.length}`);
  });

  // 2. 全部客户查询
  test('全部客户查询', async () => {
    console.log('>>> 查询全部客户(不分页)');
    const res = await api.get('/project/customer/listAll');
    expect(res.code).toBe(200);
    expect(Array.isArray(res.data), 'data 应为数组').toBe(true);
    console.log(`<<< 全部客户数量: ${res.data.length}`);
  });

  // 3. 客户简称唯一性 — 已存在
  test('客户简称唯一性 — 已存在的简称返回 not unique', async () => {
    console.log('>>> 获取已有客户简称，检测唯一性');
    const listRes = await api.get('/project/customer/list', { pageNum: 1, pageSize: 1 });
    expect(listRes.code).toBe(200);

    if (listRes.rows.length === 0) {
      console.log('<<< 无客户数据，跳过');
      test.skip();
      return;
    }

    const existingName = listRes.rows[0].customerSimpleName;
    console.log(`>>> 校验已存在简称: "${existingName}"`);
    const res = await api.get('/project/customer/checkSimpleNameUnique', {
      customerSimpleName: existingName
    });
    expect(res.code).toBe(200);
    // false 表示不唯一（已存在）
    expect(res.data, '已存在的简称应返回 false (不唯一)').toBe(false);
    console.log('<<< 已存在简称校验通过: 返回 not unique');
  });

  // 4. 客户简称唯一性 — 新名称
  test('客户简称唯一性 — 不存在的简称返回 unique', async () => {
    const fakeName = `E2E不存在的客户名_${Date.now()}`;
    console.log(`>>> 校验新简称: "${fakeName}"`);
    const res = await api.get('/project/customer/checkSimpleNameUnique', {
      customerSimpleName: fakeName
    });
    expect(res.code).toBe(200);
    // true 表示唯一（不存在）
    expect(res.data, '不存在的简称应返回 true (唯一)').toBe(true);
    console.log('<<< 新简称校验通过: 返回 unique');
  });

  // 5. 新增客户(含联系人)
  test('新增客户(含联系人)', async () => {
    console.log(`>>> 新增客户: "${TEST_CUSTOMER.customerSimpleName}"`);
    const res = await api.post('/project/customer', TEST_CUSTOMER);
    expect(res.code, '新增应返回 200').toBe(200);
    expect(res.msg).toBe('操作成功');
    console.log('<<< 新增客户成功');
  });

  // 6. 查询新增客户详情
  test('查询新增客户详情', async () => {
    console.log('>>> 查找刚新增的客户');
    const listRes = await api.get('/project/customer/list', {
      pageNum: 1,
      pageSize: 50,
      customerSimpleName: TEST_CUSTOMER.customerSimpleName
    });
    expect(listRes.code).toBe(200);

    const found = listRes.rows.find(c => c.customerSimpleName === TEST_CUSTOMER.customerSimpleName);
    expect(found, '应能在列表中找到新增客户').toBeTruthy();
    createdCustomerId = found.customerId;
    console.log(`>>> 找到客户 ID: ${createdCustomerId}, 查询详情`);

    const detailRes = await api.get(`/project/customer/${createdCustomerId}`);
    expect(detailRes.code).toBe(200);
    expect(detailRes.data).toBeTruthy();
    expect(detailRes.data.customerSimpleName).toBe(TEST_CUSTOMER.customerSimpleName);
    console.log(`<<< 客户详情确认: ${detailRes.data.customerSimpleName}`);
  });

  // 7. 查询客户联系人
  test('查询客户联系人', async () => {
    expect(createdCustomerId, '需要先创建客户').toBeTruthy();
    console.log(`>>> 查询客户 ${createdCustomerId} 的联系人`);
    const res = await api.get('/project/customer/contact/listByCustomer', {
      customerId: createdCustomerId
    });
    expect(res.code).toBe(200);
    expect(Array.isArray(res.data), '联系人 data 应为数组').toBe(true);
    expect(res.data.length, '应有 2 个联系人').toBe(2);

    const names = res.data.map(c => c.contactName);
    expect(names).toContain('张三');
    expect(names).toContain('李四');
    console.log(`<<< 联系人列表: ${names.join(', ')}`);
  });

  // 8. 修改客户
  test('修改客户', async () => {
    expect(createdCustomerId, '需要先创建客户').toBeTruthy();
    const updatedName = `E2E测试客户_已修改_${UNIQUE_SUFFIX}`;
    console.log(`>>> 修改客户名称为: "${updatedName}"`);

    const res = await api.put('/project/customer', {
      customerId: createdCustomerId,
      customerAllName: updatedName,
      customerSimpleName: TEST_CUSTOMER.customerSimpleName,
      customerContactList: TEST_CUSTOMER.customerContactList
    });
    expect(res.code, '修改应返回 200').toBe(200);
    expect(res.msg).toBe('操作成功');

    // 验证修改结果
    const detailRes = await api.get(`/project/customer/${createdCustomerId}`);
    expect(detailRes.code).toBe(200);
    expect(detailRes.data.customerAllName).toBe(updatedName);
    console.log('<<< 修改客户成功并已验证');
  });

  // 9. 删除客户
  test('删除客户', async () => {
    expect(createdCustomerId, '需要先创建客户').toBeTruthy();
    console.log(`>>> 删除客户 ID: ${createdCustomerId}`);

    const res = await api.del(`/project/customer/${createdCustomerId}`);
    expect(res.code, '删除应返回 200').toBe(200);
    expect(res.msg).toBe('操作成功');

    // 验证已删除
    const listRes = await api.get('/project/customer/list', {
      pageNum: 1,
      pageSize: 50,
      customerSimpleName: TEST_CUSTOMER.customerSimpleName
    });
    const stillExists = listRes.rows.find(c => c.customerId === createdCustomerId);
    expect(stillExists, '已删除的客户不应出现在列表中').toBeFalsy();
    console.log('<<< 删除客户成功并已验证');
    createdCustomerId = null; // 标记已清理
  });

  // 10. 删除被项目引用的客户应拒绝
  test('删除被项目引用的客户应拒绝', async () => {
    console.log('>>> 查找被项目引用的客户');

    // 从项目列表中找一个有 customerId 的项目
    const projRes = await api.get('/project/project/list', { pageNum: 1, pageSize: 50 });
    expect(projRes.code).toBe(200);

    const projectWithCustomer = projRes.rows.find(p => p.customerId);
    if (!projectWithCustomer) {
      console.log('<<< 没有找到关联客户的项目，跳过测试');
      test.skip();
      return;
    }

    const customerId = projectWithCustomer.customerId;
    console.log(`>>> 尝试删除被项目引用的客户 ID: ${customerId} (项目: ${projectWithCustomer.projectName})`);

    const res = await api.del(`/project/customer/${customerId}`);
    expect(res.code, '删除被引用客户应返回 500').toBe(500);
    expect(res.msg, '错误信息应包含 "已关联项目"').toContain('已关联项目');
    console.log(`<<< 删除被拒绝，错误信息: ${res.msg}`);
  });

  // 清理：如果测试中途失败，确保测试客户被删除
  test.afterAll(async () => {
    if (createdCustomerId) {
      console.log(`>>> 清理: 删除残留客户 ID: ${createdCustomerId}`);
      try {
        await api.del(`/project/customer/${createdCustomerId}`);
        console.log('<<< 清理完成');
      } catch (e) {
        console.log(`<<< 清理失败: ${e.message}`);
      }
    }
    await api?.dispose();
  });
});
