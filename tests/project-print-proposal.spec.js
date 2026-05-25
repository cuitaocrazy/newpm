/**
 * 项目立项申请书打印 —— 数据取数回归测试
 * API-based，无浏览器。
 *
 * 打印功能在「项目管理」列表操作列，前端纯组装：
 *   getProject(projectId)  —— 项目主体（项目名称/编号/客户/所属团队 projectDept 等）
 *   listTask({ projectId }) —— 该项目下"全部"任务，渲染任务编号/任务名称（多任务逐行）
 *
 * 本次修复的核心 bug：最初用 /project/task/options 取任务，该接口是给日报下拉用的，
 * 带 `task_stage != '11'` 过滤，会藏掉已结项任务（如 stage=11），导致打印少显示任务。
 * 改用 /project/task/list（任务管理同款，无阶段过滤）。
 *
 * 测试项：
 *  1. 打印取数：getProject 返回项目主体且含 projectDept（所属团队来源）
 *  2. 打印取数：listTask 返回该项目全部任务，且含 taskCode/taskName 两列
 *  3. 回归：含已结项任务(stage=11)的项目，list 取得到、options 取不到
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;

/** 含任务的 projectId（任务 taskId 非空） */
let projectIdWithTasks = null;
/** 含已结项任务(stage=11)的项目及该任务（用于回归对比） */
let projectIdWithClosedTask = null;
let closedTask = null;

test.describe.serial('项目立项申请书打印 - 取数', () => {

  test.beforeAll(async () => {
    api = await setupApi();

    // 全量扫一遍任务，定位"含任务的项目"和"含结项任务的项目"
    const all = await api.get('/project/task/list', { pageNum: 1, pageSize: 1000 });
    expect(all.code, '任务列表应返回200').toBe(200);
    const validTasks = (all.rows || []).filter(r => r.taskId != null);

    // 优先挑"编号和名称都非空"的任务所在项目，让列内容断言更有意义；否则退回任意含任务项目
    const richTask = validTasks.find(r => r.taskCode && r.taskName);
    if (richTask) {
      projectIdWithTasks = richTask.projectId;
    } else if (validTasks.length > 0) {
      projectIdWithTasks = validTasks[0].projectId;
    }
    closedTask = validTasks.find(r => String(r.taskStage) === '11') || null;
    if (closedTask) {
      projectIdWithClosedTask = closedTask.projectId;
    }
    console.log(`含任务项目=${projectIdWithTasks}，含结项任务项目=${projectIdWithClosedTask}` +
      (closedTask ? `（任务 ${closedTask.taskCode} / ${closedTask.taskName}）` : '（无 stage=11 任务）'));
  });

  test.afterAll(async () => {
    if (api) await api.dispose();
  });

  // ─────────────────────────────────────────────
  // 1. getProject 返回项目主体且含 projectDept（所属团队来源）
  // ─────────────────────────────────────────────
  test('getProject 返回项目主体，含 projectDept（所属团队）', async () => {
    test.skip(projectIdWithTasks == null, '当前库中无含任务的项目，跳过');

    const res = await api.get(`/project/project/${projectIdWithTasks}`);
    expect(res.code, 'getProject 应返回200').toBe(200);
    expect(res.data, '应返回项目对象').toBeTruthy();
    expect(res.data.projectName, '项目名称应存在').toBeTruthy();
    expect(res.data.projectCode, '项目编号应存在').toBeTruthy();
    // 所属团队 = 项目部门：前端用 getDeptName(projectDept) 渲染，需保证字段存在
    expect(res.data, 'projectDept 字段应存在（所属团队来源）').toHaveProperty('projectDept');
    console.log(`项目主体 OK：${res.data.projectName} / ${res.data.projectCode} / dept=${res.data.projectDept}`);
  });

  // ─────────────────────────────────────────────
  // 2. listTask 返回该项目全部任务，且含 taskCode / taskName
  // ─────────────────────────────────────────────
  test('listTask 返回该项目全部任务，含 taskCode/taskName 两列', async () => {
    test.skip(projectIdWithTasks == null, '当前库中无含任务的项目，跳过');

    const res = await api.get('/project/task/list', { projectId: projectIdWithTasks, pageNum: 1, pageSize: 1000 });
    expect(res.code, 'listTask 应返回200').toBe(200);
    const tasks = (res.rows || []).filter(r => r.taskId != null);
    expect(tasks.length, '该项目应至少有1个任务').toBeGreaterThan(0);
    // 打印的两列：任务编号 / 任务名称 —— 字段必须存在
    for (const t of tasks) {
      expect(t, '任务应含 taskCode').toHaveProperty('taskCode');
      expect(t, '任务应含 taskName').toHaveProperty('taskName');
    }
    // 前端 computed 用 .filter(Boolean) 拼接，至少应有一个任务名称非空，否则两列全空无意义
    const nonEmptyNames = tasks.map(t => t.taskName).filter(Boolean);
    expect(nonEmptyNames.length, '至少应有1个非空任务名称用于渲染').toBeGreaterThan(0);
    console.log(`listTask 取到 ${tasks.length} 个任务：${tasks.map(t => t.taskCode || '(无编号)').join(', ')}`);
  });

  // ─────────────────────────────────────────────
  // 3. 回归：含结项任务(stage=11)的项目，list 取得到、options 取不到
  //    —— 锁死"打印必须用 listTask 而非 options"的修复
  // ─────────────────────────────────────────────
  test('回归：已结项任务(stage=11) list 能取到、options 被过滤', async () => {
    test.skip(closedTask == null, '当前库中无 stage=11 任务，跳过此回归用例');

    // listTask（打印用）应包含该结项任务
    const listRes = await api.get('/project/task/list', { projectId: projectIdWithClosedTask, pageNum: 1, pageSize: 1000 });
    expect(listRes.code).toBe(200);
    const listTasks = (listRes.rows || []).filter(r => r.taskId != null);
    const listCodes = listTasks.map(t => t.taskCode);
    expect(listCodes, `listTask 应包含结项任务 ${closedTask.taskCode}`).toContain(closedTask.taskCode);

    // options（日报下拉用）应过滤掉该结项任务
    const optRes = await api.get('/project/task/options', { projectId: projectIdWithClosedTask });
    expect(optRes.code).toBe(200);
    const optCodes = (optRes.data || []).map(t => t.taskCode);
    expect(optCodes, `options 不应包含结项任务 ${closedTask.taskCode}`).not.toContain(closedTask.taskCode);

    // 因此 list 的任务数应严格多于 options（至少多出这个结项任务）
    expect(listTasks.length, 'list 任务数应多于 options（结项任务被 options 滤掉）')
      .toBeGreaterThan((optRes.data || []).length);

    console.log(`回归通过：结项任务 ${closedTask.taskCode} —— list=${listTasks.length} 个，options=${(optRes.data || []).length} 个`);
  });
});
