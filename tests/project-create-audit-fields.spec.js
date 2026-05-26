/**
 * 验证立项申请和编辑时 create_by / update_by 字段正确设置
 * 修复：ProjectServiceImpl.insertProject 补 createBy，Controller.edit 补 updateBy/updateTime
 *
 * 走 setupApi() helper（localhost:80/dev-api 代理），端口无关：
 * 不硬编码后端端口，本地(8085)/k3s(8080) 均可，由前端代理转发。
 */
import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;
let createdProjectId = null;

test.describe.serial('项目 create_by / update_by 审计字段验证', () => {

  test.beforeAll(async () => {
    api = await setupApi();
  });

  test.afterAll(async () => {
    if (api) {
      if (createdProjectId) await api.del(`/project/project/${createdProjectId}`);
      await api.dispose();
    }
  });

  test('立项申请后 create_by 应有值', async () => {
    const stamp = Date.now();
    const projectName = `E2E审计字段测试_${stamp}`;
    const createRes = await api.post('/project/project', {
      projectName,
      industry: 'ZH',
      region: 'BJ',
      regionId: '11',
      shortName: 'AUDIT',
      establishedYear: '2026',
      projectCode: `ZH-BJ-11-AUDIT-2026-${stamp}`,
      projectCategory: 'RJKF',
      projectDept: '216',
      projectStatus: '1',
      acceptanceStatus: '0',
      estimatedWorkload: '10',
      projectBudget: '100000',
      projectManagerId: '1',
      projectDescription: 'E2E测试：验证create_by字段'
    });
    expect(createRes.code).toBe(200);

    const listData = await api.get('/project/project/list', { projectName, pageNum: 1, pageSize: 1 });
    expect(listData.total).toBeGreaterThanOrEqual(1);
    createdProjectId = listData.rows[0].projectId;

    const detail = (await api.get(`/project/project/${createdProjectId}`)).data;
    console.log(`创建人: ${detail.createBy}, 创建时间: ${detail.createTime}`);
    expect(detail.createBy).toBe('admin');
    expect(detail.createTime).toBeTruthy();
    console.log('✅ 立项申请 create_by 验证通过');
  });

  test('编辑保存后 update_by / update_time 应有值', async () => {
    expect(createdProjectId).toBeTruthy();

    const project = (await api.get(`/project/project/${createdProjectId}`)).data;
    const originalUpdateTime = project.updateTime;

    await api.put('/project/project', {
      ...project,
      projectDescription: 'E2E测试：验证update_by字段（已编辑）'
    });

    const updated = (await api.get(`/project/project/${createdProjectId}`)).data;
    console.log(`更新人: ${updated.updateBy}, 更新时间: ${updated.updateTime}`);
    expect(updated.updateBy).toBe('admin');
    expect(updated.updateTime).toBeTruthy();
    if (originalUpdateTime) {
      expect(new Date(updated.updateTime).getTime()).toBeGreaterThanOrEqual(new Date(originalUpdateTime).getTime());
    }
    console.log('✅ 编辑保存 update_by / update_time 验证通过');
  });

  test('非编辑操作不应改变 update_by / update_time', async () => {
    expect(createdProjectId).toBeTruthy();

    const before = (await api.get(`/project/project/${createdProjectId}`)).data;
    const beforeUpdateBy = before.updateBy;
    const beforeUpdateTime = before.updateTime;

    // 收入确认（非编辑操作，不应更新 update_by/update_time）
    await api.put('/project/project/revenue', {
      projectId: createdProjectId,
      revenueConfirmStatus: '1',
      revenueConfirmYear: '2026',
      confirmAmount: '50000',
      taxRate: '6'
    });

    const after = (await api.get(`/project/project/${createdProjectId}`)).data;
    console.log(`收入确认前 update_by: ${beforeUpdateBy}, update_time: ${beforeUpdateTime}`);
    console.log(`收入确认后 update_by: ${after.updateBy}, update_time: ${after.updateTime}`);

    expect(after.updateBy).toBe(beforeUpdateBy);
    expect(new Date(after.updateTime).getTime()).toBe(new Date(beforeUpdateTime).getTime());
    console.log('✅ 非编辑操作未改变 update_by / update_time 验证通过');
  });
});
