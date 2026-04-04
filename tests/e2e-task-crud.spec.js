/**
 * 任务管理 (TaskController) E2E 测试
 * API-based，无浏览器，覆盖任务 CRUD 全流程
 *
 * 测试项：
 *  1. 任务列表查询
 *  2. 任务汇总查询
 *  3. 任务搜索(编号)
 *  4. 任务搜索(名称)
 *  5. 任务选项列表
 *  6. 批量检查项目是否有任务
 *  7. 新增任务
 *  8. 查询新增任务详情
 *  9. 修改任务
 * 10. 删除任务
 * 11. 删除有日报的任务应拒绝
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;
const TS = Date.now();
const TEST_TASK_NAME = `E2E测试任务_${TS}`;
const TEST_TASK_CODE = `E2E-T-${TS}`;

/** 创建过程中产生的 taskId，用于后续查询/修改/删除 */
let createdTaskId = null;

/** 从列表中取到的已有任务（用于搜索测试） */
let existingTask = null;

/** 有任务的 projectId（用于 options / projectsHasTasks） */
let projectIdWithTasks = null;

test.describe.serial('任务管理 CRUD', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    console.log('登录成功，开始任务管理测试');
  });

  // ─────────────────────────────────────────────
  // 1. 任务列表查询
  // ─────────────────────────────────────────────
  test('任务列表查询', async () => {
    const res = await api.get('/project/task/list', { pageNum: 1, pageSize: 10 });
    expect(res.code, '列表查询应返回200').toBe(200);
    expect(res.total, '总数应为数字').toBeGreaterThanOrEqual(0);
    expect(Array.isArray(res.rows), 'rows 应为数组').toBe(true);
    console.log(`任务列表查询成功，共 ${res.total} 条记录`);

    // 找一条 taskId 不为 null 的行（LEFT JOIN 结果里有些行是纯项目无任务）
    if (res.rows.length > 0) {
      existingTask = res.rows.find(r => r.taskId != null) || null;
      if (existingTask) {
        projectIdWithTasks = existingTask.projectId;
        console.log(`已有任务示例: taskId=${existingTask.taskId}, taskCode=${existingTask.taskCode}, taskName=${existingTask.taskName}`);
      } else {
        console.log('列表中无有效任务（taskId 全为 null），后续任务搜索测试将跳过');
      }
    }
  });

  // ─────────────────────────────────────────────
  // 2. 任务汇总查询
  // ─────────────────────────────────────────────
  test('任务汇总查询', async () => {
    const res = await api.get('/project/task/summary', { pageNum: 1, pageSize: 10 });
    expect(res.code, '汇总查询应返回200').toBe(200);
    expect(res.data, '应返回汇总数据').toBeDefined();
    console.log('任务汇总查询成功:', JSON.stringify(res.data));
  });

  // ─────────────────────────────────────────────
  // 3. 任务搜索(编号)
  // ─────────────────────────────────────────────
  test('任务搜索(编号)', async () => {
    if (!existingTask?.taskCode) {
      console.log('无已有任务编号，跳过');
      test.skip();
      return;
    }
    // 取编号前几个字符做模糊搜索
    const partial = existingTask.taskCode.substring(0, Math.min(4, existingTask.taskCode.length));
    const res = await api.get('/project/task/searchTaskCode', { taskCode: partial });
    expect(res.code, '搜索编号应返回200').toBe(200);
    expect(Array.isArray(res.data), '返回建议列表应为数组').toBe(true);
    console.log(`按编号 "${partial}" 搜索到 ${res.data.length} 条建议`);
  });

  // ─────────────────────────────────────────────
  // 4. 任务搜索(名称)
  // ─────────────────────────────────────────────
  test('任务搜索(名称)', async () => {
    if (!existingTask?.taskName) {
      console.log('无已有任务名称，跳过');
      test.skip();
      return;
    }
    const partial = existingTask.taskName.substring(0, Math.min(4, existingTask.taskName.length));
    const res = await api.get('/project/task/searchTaskName', { taskName: partial });
    expect(res.code, '搜索名称应返回200').toBe(200);
    expect(Array.isArray(res.data), '返回建议列表应为数组').toBe(true);
    console.log(`按名称 "${partial}" 搜索到 ${res.data.length} 条建议`);
  });

  // ─────────────────────────────────────────────
  // 5. 任务选项列表
  // ─────────────────────────────────────────────
  test('任务选项列表', async () => {
    if (!projectIdWithTasks) {
      console.log('无含任务的项目，跳过');
      test.skip();
      return;
    }
    const res = await api.get('/project/task/options', { projectId: projectIdWithTasks });
    expect(res.code, '选项列表应返回200').toBe(200);
    expect(Array.isArray(res.data), '选项数据应为数组').toBe(true);
    expect(res.data.length, '应有至少一个选项').toBeGreaterThan(0);
    console.log(`项目 ${projectIdWithTasks} 的任务选项共 ${res.data.length} 条`);
  });

  // ─────────────────────────────────────────────
  // 6. 批量检查项目是否有任务
  // ─────────────────────────────────────────────
  test('批量检查项目是否有任务', async () => {
    // 先取几个项目 ID
    const projRes = await api.get('/project/project/list', { pageNum: 1, pageSize: 5 });
    expect(projRes.code).toBe(200);
    const projectIds = projRes.rows.map(p => p.projectId).filter(Boolean);
    if (projectIds.length === 0) {
      console.log('无项目数据，跳过');
      test.skip();
      return;
    }

    const res = await api.get('/project/task/projectsHasTasks', { projectIds: projectIds.join(',') });
    expect(res.code, '批量检查应返回200').toBe(200);
    expect(res.data, '应返回映射数据').toBeDefined();
    console.log('批量检查结果:', JSON.stringify(res.data));
  });

  // ─────────────────────────────────────────────
  // 7. 新增任务
  // ─────────────────────────────────────────────
  test('新增任务', async () => {
    // 先获取一个有效的项目 ID
    const projRes = await api.get('/project/project/list', { pageNum: 1, pageSize: 5 });
    expect(projRes.code).toBe(200);
    expect(projRes.rows.length, '需要至少一个项目').toBeGreaterThan(0);
    const projectId = projRes.rows[0].projectId;

    const res = await api.post('/project/task', {
      projectId,
      taskName: TEST_TASK_NAME,
      taskCode: TEST_TASK_CODE,
    });
    expect(res.code, '新增任务应返回200').toBe(200);
    expect(res.msg).toBe('操作成功');

    // 查询列表获取刚创建的任务 ID
    const listRes = await api.get('/project/task/list', {
      pageNum: 1,
      pageSize: 10,
      taskCode: TEST_TASK_CODE,
    });
    expect(listRes.code).toBe(200);
    expect(listRes.rows.length, '应能查到刚创建的任务').toBeGreaterThan(0);
    const found = listRes.rows.find(t => t.taskCode === TEST_TASK_CODE);
    expect(found, '应匹配到测试任务编号').toBeTruthy();
    createdTaskId = found.taskId;
    console.log(`新增任务成功: taskId=${createdTaskId}, taskCode=${TEST_TASK_CODE}`);
  });

  // ─────────────────────────────────────────────
  // 8. 查询新增任务详情
  // ─────────────────────────────────────────────
  test('查询新增任务详情', async () => {
    expect(createdTaskId, '需要先新增任务').toBeTruthy();
    const res = await api.get(`/project/task/${createdTaskId}`);
    expect(res.code, '详情查询应返回200').toBe(200);
    expect(res.data, '应返回任务数据').toBeDefined();
    expect(res.data.taskName).toBe(TEST_TASK_NAME);
    expect(res.data.taskCode).toBe(TEST_TASK_CODE);
    console.log(`任务详情查询成功: taskId=${createdTaskId}, taskName=${res.data.taskName}`);
  });

  // ─────────────────────────────────────────────
  // 9. 修改任务
  // ─────────────────────────────────────────────
  test('修改任务', async () => {
    expect(createdTaskId, '需要先新增任务').toBeTruthy();
    const updatedName = `${TEST_TASK_NAME}_已修改`;
    // 先获取完整数据
    const detail = await api.get(`/project/task/${createdTaskId}`);
    expect(detail.code).toBe(200);

    const res = await api.put('/project/task', {
      ...detail.data,
      taskId: createdTaskId,
      taskName: updatedName,
    });
    expect(res.code, '修改任务应返回200').toBe(200);
    expect(res.msg).toBe('操作成功');

    // 验证修改结果
    const verify = await api.get(`/project/task/${createdTaskId}`);
    expect(verify.data.taskName).toBe(updatedName);
    console.log(`修改任务成功: taskName 已更新为 "${updatedName}"`);
  });

  // ─────────────────────────────────────────────
  // 10. 删除任务
  // ─────────────────────────────────────────────
  test('删除任务', async () => {
    expect(createdTaskId, '需要先新增任务').toBeTruthy();
    const res = await api.del(`/project/task/${createdTaskId}`);
    expect(res.code, '删除任务应返回200').toBe(200);
    expect(res.msg).toBe('操作成功');

    // 验证删除后查不到
    const listRes = await api.get('/project/task/list', {
      pageNum: 1,
      pageSize: 10,
      taskCode: TEST_TASK_CODE,
    });
    const stillExists = listRes.rows?.find(t => t.taskId === createdTaskId);
    expect(stillExists, '删除后不应再查到该任务').toBeFalsy();
    console.log(`删除任务成功: taskId=${createdTaskId}`);
    createdTaskId = null; // 标记已清理
  });

  // ─────────────────────────────────────────────
  // 11. 删除有日报的任务应拒绝
  // ─────────────────────────────────────────────
  test('删除有日报的任务应拒绝', async () => {
    // 查找一个 actualWorkload > 0 的任务（说明有日报记录）
    const listRes = await api.get('/project/task/list', { pageNum: 1, pageSize: 100 });
    expect(listRes.code).toBe(200);

    const taskWithReport = listRes.rows.find(t => t.actualWorkload && t.actualWorkload > 0);
    if (!taskWithReport) {
      console.log('无有日报记录的任务，跳过');
      test.skip();
      return;
    }

    console.log(`尝试删除有日报的任务: taskId=${taskWithReport.taskId}, actualWorkload=${taskWithReport.actualWorkload}`);
    const res = await api.del(`/project/task/${taskWithReport.taskId}`);
    expect(res.code, '删除有日报的任务应被拒绝').toBe(500);
    expect(res.msg, '应提示已有日报记录').toContain('已有日报记录');
    console.log(`删除被正确拒绝: ${res.msg}`);
  });

  // ─────────────────────────────────────────────
  // 清理：确保测试任务被删除
  // ─────────────────────────────────────────────
  test.afterAll(async () => {
    if (createdTaskId) {
      console.log(`清理残留测试任务: taskId=${createdTaskId}`);
      try {
        await api.del(`/project/task/${createdTaskId}`);
        console.log('清理完成');
      } catch (e) {
        console.log(`清理失败（可忽略）: ${e.message}`);
      }
    }
    await api?.dispose();
  });
});
