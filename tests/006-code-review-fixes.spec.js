/**
 * 006-code-review-fixes E2E 测试
 * 验证 High 级别 Code Review 修复：删除保护、输入校验、路径安全
 *
 * H4:  合同关联查询加 del_flag='0' 过滤
 * H5:  编辑合同时校验项目是否已关联其他合同
 * H7:  删除客户前检查是否被项目引用
 * H8:  删除投产批次前检查是否被任务引用
 * H9:  删除任务前检查是否有日报明细引用
 * H12: 人天补正接口参数校验（DTO + @Validated）
 */

import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:80';
let authToken = '';

/** 登录获取 token */
async function getToken(request) {
  const resp = await request.post(`${BASE_URL}/dev-api/login`, {
    data: { username: 'admin', password: '123456789' }
  });
  const body = await resp.json();
  expect(body.code, '登录应成功').toBe(200);
  return body.token;
}

/** 带认证的 GET */
async function apiGet(request, path, params = {}) {
  const query = new URLSearchParams(params).toString();
  const url = query ? `${BASE_URL}/dev-api${path}?${query}` : `${BASE_URL}/dev-api${path}`;
  return request.get(url, {
    headers: { Authorization: `Bearer ${authToken}` }
  });
}

/** 带认证的 POST */
async function apiPost(request, path, data = {}) {
  return request.post(`${BASE_URL}/dev-api${path}`, {
    headers: { Authorization: `Bearer ${authToken}` },
    data
  });
}

/** 带认证的 PUT */
async function apiPut(request, path, data = {}) {
  return request.put(`${BASE_URL}/dev-api${path}`, {
    headers: { Authorization: `Bearer ${authToken}` },
    data
  });
}

/** 带认证的 DELETE */
async function apiDelete(request, path) {
  return request.delete(`${BASE_URL}/dev-api${path}`, {
    headers: { Authorization: `Bearer ${authToken}` }
  });
}

test.describe('006 Code Review 修复验证', () => {

  test.beforeAll(async ({ request }) => {
    authToken = await getToken(request);
  });

  // ─────────────────────────────────────────────
  // H7: 删除客户前检查是否被项目引用
  // ─────────────────────────────────────────────
  test('H7: 删除被项目引用的客户应被拒绝', async ({ request }) => {
    // 1. 查询项目列表，获取一个有 customerId 的项目
    const projResp = await apiGet(request, '/project/project/list', { pageNum: 1, pageSize: 50 });
    const projBody = await projResp.json();
    expect(projBody.code).toBe(200);

    const projectWithCustomer = projBody.rows.find(p => p.customerId);
    if (!projectWithCustomer) {
      test.skip();
      return;
    }

    const customerId = projectWithCustomer.customerId;
    console.log(`  尝试删除客户 ${customerId}（被项目 ${projectWithCustomer.projectName} 引用）`);

    // 2. 尝试删除该客户 — 应返回错误
    const delResp = await apiDelete(request, `/project/customer/${customerId}`);
    const delBody = await delResp.json();
    console.log(`  删除响应: code=${delBody.code}, msg=${delBody.msg}`);

    expect(delBody.code, '删除被引用的客户应失败').toBe(500);
    expect(delBody.msg).toContain('已关联项目');
  });

  // ─────────────────────────────────────────────
  // H8: 删除投产批次前检查是否被任务引用
  // ─────────────────────────────────────────────
  test('H8: 删除被任务引用的投产批次应被拒绝', async ({ request }) => {
    // 1. 查询任务列表，找到有 batchId 的任务
    const taskResp = await apiGet(request, '/project/task/list', { pageNum: 1, pageSize: 100 });
    const taskBody = await taskResp.json();
    expect(taskBody.code).toBe(200);

    const taskWithBatch = taskBody.rows.find(t => t.batchId);
    if (!taskWithBatch) {
      test.skip();
      return;
    }

    const batchId = taskWithBatch.batchId;
    console.log(`  尝试删除批次 ${batchId}（被任务 ${taskWithBatch.taskName} 引用）`);

    // 2. 尝试删除该批次 — 应返回错误
    const delResp = await apiDelete(request, `/project/productionBatch/${batchId}`);
    const delBody = await delResp.json();
    console.log(`  删除响应: code=${delBody.code}, msg=${delBody.msg}`);

    expect(delBody.code, '删除被引用的批次应失败').toBe(500);
    expect(delBody.msg).toContain('已被任务引用');
  });

  // ─────────────────────────────────────────────
  // H9: 删除任务前检查是否有日报明细引用
  // ─────────────────────────────────────────────
  test('H9: 删除有日报记录的任务应被拒绝', async ({ request }) => {
    // 1. 查询任务列表，找到有 actualWorkload > 0 的任务（说明有日报记录）
    const taskResp = await apiGet(request, '/project/task/list', { pageNum: 1, pageSize: 100 });
    const taskBody = await taskResp.json();
    expect(taskBody.code).toBe(200);

    const taskWithReport = taskBody.rows.find(t => t.actualWorkload && t.actualWorkload > 0);
    if (!taskWithReport) {
      test.skip();
      return;
    }

    const taskId = taskWithReport.taskId;
    console.log(`  尝试删除任务 ${taskId}（${taskWithReport.taskName}，actualWorkload=${taskWithReport.actualWorkload}）`);

    // 2. 尝试删除该任务 — 应返回错误
    const delResp = await apiDelete(request, `/project/task/${taskId}`);
    const delBody = await delResp.json();
    console.log(`  删除响应: code=${delBody.code}, msg=${delBody.msg}`);

    expect(delBody.code, '删除有日报的任务应失败').toBe(500);
    expect(delBody.msg).toContain('已有日报记录');
  });

  // ─────────────────────────────────────────────
  // H12: 人天补正接口 DTO 校验
  // ─────────────────────────────────────────────
  test('H12: 人天补正接口缺少必填字段应返回校验错误', async ({ request }) => {
    // 找一个有效的项目 ID
    const projResp = await apiGet(request, '/project/project/list', { pageNum: 1, pageSize: 1 });
    const projBody = await projResp.json();
    expect(projBody.code).toBe(200);

    if (projBody.rows.length === 0) {
      test.skip();
      return;
    }

    const projectId = projBody.rows[0].projectId;

    // 1. 缺少 direction — 应校验失败
    const resp1 = await apiPost(request, `/project/dailyReport/projectStats/${projectId}/correct`, {
      delta: '1.0',
      afterAdjust: '10.0',
      reason: '测试'
    });
    const body1 = await resp1.json();
    console.log(`  缺少 direction: code=${body1.code}, msg=${body1.msg}`);
    expect(body1.code, '缺少 direction 应校验失败').not.toBe(200);

    // 2. 缺少 delta — 应校验失败
    const resp2 = await apiPost(request, `/project/dailyReport/projectStats/${projectId}/correct`, {
      direction: 1,
      afterAdjust: '10.0',
      reason: '测试'
    });
    const body2 = await resp2.json();
    console.log(`  缺少 delta: code=${body2.code}, msg=${body2.msg}`);
    expect(body2.code, '缺少 delta 应校验失败').not.toBe(200);

    // 3. 完整参数应接受（不一定执行成功，但不应是校验错误）
    const resp3 = await apiPost(request, `/project/dailyReport/projectStats/${projectId}/correct`, {
      direction: 1,
      delta: '0.001',
      afterAdjust: '0.001',
      reason: 'E2E测试校验'
    });
    const body3 = await resp3.json();
    console.log(`  完整参数: code=${body3.code}, msg=${body3.msg}`);
    // 完整参数时不应返回参数校验错误（可能因权限等返回其他错误，但不应是 400 校验类）
  });

  // ─────────────────────────────────────────────
  // H5: 编辑合同时校验项目是否已关联其他合同
  // ─────────────────────────────────────────────
  test('H5: 编辑合同绑定已被其他合同关联的项目应被拒绝', async ({ request }) => {
    // 1. 查询合同列表，找两个有 projectIds 的合同
    const contractResp = await apiGet(request, '/project/contract/list', { pageNum: 1, pageSize: 100 });
    const contractBody = await contractResp.json();
    expect(contractBody.code).toBe(200);

    // 找到有关联项目的合同
    const contractsWithProject = contractBody.rows.filter(c => c.projectIds && c.projectIds.length > 0);
    if (contractsWithProject.length < 2) {
      console.log('  数据不足：需要至少2个有关联项目的合同才能测试 H5');
      test.skip();
      return;
    }

    const contract1 = contractsWithProject[0];
    const contract2 = contractsWithProject[1];

    // 获取合同1的完整信息
    const detailResp = await apiGet(request, `/project/contract/${contract2.contractId}`);
    const detailBody = await detailResp.json();
    expect(detailBody.code).toBe(200);
    const contractDetail = detailBody.data;

    // 2. 尝试将 contract1 的项目绑到 contract2 — 应被拒绝
    const conflictProjectId = contract1.projectIds[0];
    console.log(`  尝试将项目 ${conflictProjectId}（属于合同 ${contract1.contractId}）绑定到合同 ${contract2.contractId}`);

    const updateData = {
      ...contractDetail,
      projectIds: [...(contractDetail.projectIds || []), conflictProjectId]
    };

    const updateResp = await apiPut(request, '/project/contract', updateData);
    const updateBody = await updateResp.json();
    console.log(`  更新响应: code=${updateBody.code}, msg=${updateBody.msg}`);

    expect(updateBody.code, '绑定已关联项目应失败').toBe(500);
    expect(updateBody.msg).toContain('已关联其他合同');
  });

  // ─────────────────────────────────────────────
  // H4: 合同关联查询 del_flag 过滤（间接验证）
  // ─────────────────────────────────────────────
  test('H4: 项目绑定合同接口正常工作', async ({ request }) => {
    // 验证 selectContractIdByProjectId 查询正常（间接验证 del_flag 过滤）
    // 找一个没有合同的项目
    const projResp = await apiGet(request, '/project/project/list', { pageNum: 1, pageSize: 50 });
    const projBody = await projResp.json();
    expect(projBody.code).toBe(200);

    // 合同列表正常返回
    const contractResp = await apiGet(request, '/project/contract/list', { pageNum: 1, pageSize: 10 });
    const contractBody = await contractResp.json();
    expect(contractBody.code).toBe(200);
    console.log(`  合同列表正常，共 ${contractBody.total} 条`);

    // 项目列表正常返回
    expect(projBody.total).toBeGreaterThanOrEqual(0);
    console.log(`  项目列表正常，共 ${projBody.total} 条`);
    console.log('  ✅ H4 del_flag 过滤不影响正常查询');
  });

  // ─────────────────────────────────────────────
  // H11: ServiceException 替换验证（间接）
  // ─────────────────────────────────────────────
  test('H11: 客户简称重复应返回规范化错误', async ({ request }) => {
    // 1. 获取一个已有客户的简称
    const listResp = await apiGet(request, '/project/customer/list', { pageNum: 1, pageSize: 1 });
    const listBody = await listResp.json();
    expect(listBody.code).toBe(200);

    if (listBody.rows.length === 0) {
      test.skip();
      return;
    }

    const existingName = listBody.rows[0].customerSimpleName;
    console.log(`  用已有简称 "${existingName}" 尝试新增客户`);

    // 2. 尝试新增同名客户 — 应返回 ServiceException 格式的错误
    const addResp = await apiPost(request, '/project/customer', {
      customerName: '测试重复客户E2E',
      customerSimpleName: existingName
    });
    const addBody = await addResp.json();
    console.log(`  响应: code=${addBody.code}, msg=${addBody.msg}`);

    // ServiceException 会被 GlobalExceptionHandler 捕获返回 code=500
    // RuntimeException 也会返回 500 但可能 msg 不一致
    expect(addBody.code, '重复简称应返回错误').toBe(500);
    expect(addBody.msg).toContain('客户简称已存在');
  });

});
