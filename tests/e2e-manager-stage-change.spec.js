/**
 * 项目经理变更 & 项目阶段变更 E2E 测试（只读）
 * 验证 ProjectManagerChangeController 和 ProjectStageChangeController 查询接口
 *
 * 测试项：
 * 1. 项目经理变更列表
 * 2. 项目经理变更历史
 * 3. 项目经理变更详情
 * 4. 项目阶段变更列表
 * 5. 项目阶段变更详情
 * 6. 项目阶段变更历史
 *
 * 注意：不执行任何写操作，避免影响生产数据
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;

test.describe.serial('项目经理变更 & 项目阶段变更（只读）', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    console.log('✅ 登录成功，开始经理/阶段变更查询测试');
  });

  test.afterAll(async () => {
    await api?.dispose();
  });

  // ─────────────────────────────────────────────
  // 1. 项目经理变更列表
  // ─────────────────────────────────────────────
  test('项目经理变更列表', async () => {
    console.log('\n▶ 测试：项目经理变更列表');

    const body = await api.get('/project/managerChange/list', { pageNum: 1, pageSize: 10 });

    expect(body.code, '响应 code 应为 200').toBe(200);
    expect(Array.isArray(body.rows), 'rows 应为数组').toBe(true);
    expect(typeof body.total, 'total 应为数字').toBe('number');
    expect(body.total).toBeGreaterThanOrEqual(0);

    console.log(`  ✅ 列表查询成功，total=${body.total}, 返回 ${body.rows.length} 条`);

    if (body.rows.length > 0) {
      const first = body.rows[0];
      console.log(`  📋 首条记录: changeId=${first.changeId}, projectId=${first.projectId}`);
    }
  });

  // ─────────────────────────────────────────────
  // 2. 项目经理变更历史
  // ─────────────────────────────────────────────
  test('项目经理变更历史', async () => {
    console.log('\n▶ 测试：项目经理变更历史');

    // 先从变更列表中获取一个 projectId
    const listBody = await api.get('/project/managerChange/list', { pageNum: 1, pageSize: 1 });
    expect(listBody.code).toBe(200);

    if (!listBody.rows || listBody.rows.length === 0) {
      // 没有变更记录，用项目列表中的第一个项目
      const projectList = await api.get('/project/project/list', { pageNum: 1, pageSize: 1 });
      expect(projectList.code).toBe(200);

      if (!projectList.rows || projectList.rows.length === 0) {
        console.log('  ⏭ 无项目数据，跳过历史查询测试');
        test.skip();
        return;
      }

      const projectId = projectList.rows[0].projectId;
      console.log(`  📌 使用项目 projectId=${projectId} 查询变更历史`);

      const historyBody = await api.get(`/project/managerChange/history/${projectId}`);
      expect(historyBody.code, '响应 code 应为 200').toBe(200);
      expect(historyBody.data, 'data 应存在').toBeDefined();
      console.log(`  ✅ 历史查询成功，记录数=${Array.isArray(historyBody.data) ? historyBody.data.length : 0}`);
      return;
    }

    const projectId = listBody.rows[0].projectId;
    console.log(`  📌 使用变更记录中的 projectId=${projectId} 查询历史`);

    const historyBody = await api.get(`/project/managerChange/history/${projectId}`);

    expect(historyBody.code, '响应 code 应为 200').toBe(200);
    expect(historyBody.data, 'data 应存在').toBeDefined();

    if (Array.isArray(historyBody.data)) {
      expect(historyBody.data.length).toBeGreaterThanOrEqual(0);
      console.log(`  ✅ 历史查询成功，记录数=${historyBody.data.length}`);
    } else {
      console.log(`  ✅ 历史查询成功，返回数据类型=${typeof historyBody.data}`);
    }
  });

  // ─────────────────────────────────────────────
  // 3. 项目经理变更详情
  // ─────────────────────────────────────────────
  test('项目经理变更详情', async () => {
    console.log('\n▶ 测试：项目经理变更详情');

    const listBody = await api.get('/project/managerChange/list', { pageNum: 1, pageSize: 1 });
    expect(listBody.code).toBe(200);

    if (!listBody.rows || listBody.rows.length === 0) {
      console.log('  ⏭ 变更列表为空，跳过详情测试');
      test.skip();
      return;
    }

    const row = listBody.rows[0];
    // managerChange/list 返回的是 VO 视图，可能没有 changeId
    // 尝试用 history 接口来验证详情
    const projectId = row.projectId;
    console.log(`  📌 通过 history 接口查询 projectId=${projectId} 的变更记录`);

    const historyBody = await api.get(`/project/managerChange/history/${projectId}`);
    expect(historyBody.code, '响应 code 应为 200').toBe(200);
    console.log(`  ✅ 变更历史/详情查询成功，记录数=${Array.isArray(historyBody.data) ? historyBody.data.length : 'N/A'}`);
  });

  // ─────────────────────────────────────────────
  // 4. 项目阶段变更列表
  // ─────────────────────────────────────────────
  test('项目阶段变更列表', async () => {
    console.log('\n▶ 测试：项目阶段变更列表');

    const body = await api.get('/project/projectStageChange/list', { pageNum: 1, pageSize: 10 });

    expect(body.code, '响应 code 应为 200').toBe(200);
    expect(Array.isArray(body.rows), 'rows 应为数组').toBe(true);
    expect(typeof body.total, 'total 应为数字').toBe('number');
    expect(body.total).toBeGreaterThanOrEqual(0);

    console.log(`  ✅ 列表查询成功，total=${body.total}, 返回 ${body.rows.length} 条`);

    if (body.rows.length > 0) {
      const first = body.rows[0];
      console.log(`  📋 首条记录: changeId=${first.changeId}, projectId=${first.projectId}`);
    }
  });

  // ─────────────────────────────────────────────
  // 5. 项目阶段变更详情
  // ─────────────────────────────────────────────
  test('项目阶段变更详情', async () => {
    console.log('\n▶ 测试：项目阶段变更详情');

    const listBody = await api.get('/project/projectStageChange/list', { pageNum: 1, pageSize: 1 });
    expect(listBody.code).toBe(200);

    if (!listBody.rows || listBody.rows.length === 0) {
      console.log('  ⏭ 阶段变更列表为空，跳过详情测试');
      test.skip();
      return;
    }

    const row = listBody.rows[0];
    const changeId = row.changeId;

    if (!changeId) {
      // 列表可能返回视图数据无 changeId，改用 history 接口
      const projectId = row.projectId;
      console.log(`  📌 列表无 changeId，通过 history 查询 projectId=${projectId}`);
      const historyBody = await api.get(`/project/projectStageChange/history/${projectId}`);
      expect(historyBody.code, '响应 code 应为 200').toBe(200);
      console.log(`  ✅ 阶段变更历史查询成功，记录数=${Array.isArray(historyBody.data) ? historyBody.data.length : 'N/A'}`);
      return;
    }

    console.log(`  📌 查询阶段变更详情 changeId=${changeId}`);
    const detailBody = await api.get(`/project/projectStageChange/${changeId}`);
    expect(detailBody.code, '响应 code 应为 200').toBe(200);
    expect(detailBody.data, 'data 应存在').toBeDefined();
    console.log(`  ✅ 详情查询成功，changeId=${changeId}`);
  });

  // ─────────────────────────────────────────────
  // 6. 项目阶段变更历史
  // ─────────────────────────────────────────────
  test('项目阶段变更历史', async () => {
    console.log('\n▶ 测试：项目阶段变更历史');

    // 先从阶段变更列表中获取一个 projectId
    const listBody = await api.get('/project/projectStageChange/list', { pageNum: 1, pageSize: 1 });
    expect(listBody.code).toBe(200);

    if (!listBody.rows || listBody.rows.length === 0) {
      // 没有阶段变更记录，用项目列表中的第一个项目
      const projectList = await api.get('/project/project/list', { pageNum: 1, pageSize: 1 });
      expect(projectList.code).toBe(200);

      if (!projectList.rows || projectList.rows.length === 0) {
        console.log('  ⏭ 无项目数据，跳过历史查询测试');
        test.skip();
        return;
      }

      const projectId = projectList.rows[0].projectId;
      console.log(`  📌 使用项目 projectId=${projectId} 查询阶段变更历史`);

      const historyBody = await api.get(`/project/projectStageChange/history/${projectId}`);
      expect(historyBody.code, '响应 code 应为 200').toBe(200);
      expect(historyBody.data, 'data 应存在').toBeDefined();
      console.log(`  ✅ 历史查询成功，记录数=${Array.isArray(historyBody.data) ? historyBody.data.length : 0}`);
      return;
    }

    const projectId = listBody.rows[0].projectId;
    console.log(`  📌 使用阶段变更记录中的 projectId=${projectId} 查询历史`);

    const historyBody = await api.get(`/project/projectStageChange/history/${projectId}`);

    expect(historyBody.code, '响应 code 应为 200').toBe(200);
    expect(historyBody.data, 'data 应存在').toBeDefined();

    if (Array.isArray(historyBody.data)) {
      expect(historyBody.data.length).toBeGreaterThanOrEqual(0);
      console.log(`  ✅ 历史查询成功，记录数=${historyBody.data.length}`);
    } else {
      console.log(`  ✅ 历史查询成功，返回数据类型=${typeof historyBody.data}`);
    }
  });

});
