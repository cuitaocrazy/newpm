/**
 * 项目立项申请书打印 —— 数据取数回归测试
 * API-based，无浏览器。
 *
 * 打印功能在「项目管理」列表操作列（按钮文案「打印立项申请」），前端纯组装：
 *   getProject(projectId) —— 项目主体，提供打印所需的全部字段
 *
 * 打印版式（2026-05 改造后）：
 *   - 「立项日期」取 project.applyDate（原取 create_time，已改）；为空前端显示 '-'
 *   - 「所属部门」取 project.projectDept（标签由「所属团队」改为「所属部门」，取数字段不变）
 *   - 「项目概况」「项目计划」取 projectDescription / projectPlan，支持换行
 *   - 已去掉「任务编号」「任务名称」两行 —— 打印不再调用 listTask
 *
 * 本测试锁死：
 *  1. getProject 自足提供打印主体字段（projectName/projectCode/projectDept），无需 listTask
 *  2. applyDate 往返：创建时写入 → getProject 读回相等（验证 Mapper insert + resultMap 三处镜像）
 *  3. applyDate 可更新：编辑改值 → getProject 读回为新值（验证 Mapper update 镜像）
 *  4. 旧项目 applyDate 为空时字段仍存在（前端 printProject.applyDate 引用，需保证 property 在）
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;
let createdProjectId = null;

const APPLY_DATE_INIT = '2026-05-26';
const APPLY_DATE_UPDATED = '2026-06-01';

test.describe.serial('项目立项申请书打印 - 取数', () => {

  test.beforeAll(async () => {
    api = await setupApi();

    // 自建测试项目（带 applyDate），不依赖库中既有数据
    const stamp = Date.now();
    const createRes = await api.post('/project/project', {
      projectName: `E2E打印立项日期测试_${stamp}`,
      industry: 'ZH',
      region: 'BJ',
      regionId: '11',
      shortName: 'PRINT',
      establishedYear: '2026',
      projectCode: `ZH-BJ-11-PRINT-2026-${stamp}`,
      projectCategory: 'RJKF',
      projectDept: '216',
      projectStatus: '1',
      acceptanceStatus: '0',
      estimatedWorkload: '10',
      projectBudget: '100000',
      projectManagerId: '1',
      projectDescription: '打印取数测试\n第二行：验证项目概况换行',
      projectPlan: '计划第一行\n计划第二行',
      applyDate: APPLY_DATE_INIT
    });
    expect(createRes.code, '创建测试项目应返回200').toBe(200);

    // 按编号反查 projectId
    const listRes = await api.get('/project/project/list', { projectCode: `ZH-BJ-11-PRINT-2026-${stamp}`, pageNum: 1, pageSize: 1 });
    expect(listRes.total, '应能查到刚创建的项目').toBeGreaterThanOrEqual(1);
    createdProjectId = listRes.rows[0].projectId;
    console.log(`测试项目已创建：id=${createdProjectId}, applyDate=${APPLY_DATE_INIT}`);
  });

  test.afterAll(async () => {
    if (api) {
      if (createdProjectId) await api.del(`/project/project/${createdProjectId}`);
      await api.dispose();
    }
  });

  // ─────────────────────────────────────────────
  // 1. getProject 自足提供打印主体字段（无需 listTask）
  // ─────────────────────────────────────────────
  test('getProject 自足返回打印主体字段（项目名称/编号/所属部门）', async () => {
    const res = await api.get(`/project/project/${createdProjectId}`);
    expect(res.code, 'getProject 应返回200').toBe(200);
    expect(res.data, '应返回项目对象').toBeTruthy();
    expect(res.data.projectName, '项目名称应存在').toBeTruthy();
    expect(res.data.projectCode, '项目编号应存在').toBeTruthy();
    // 所属部门 = 项目部门：前端用 getDeptName(projectDept) 渲染
    expect(res.data, 'projectDept 字段应存在（所属部门来源）').toHaveProperty('projectDept');
    // 概况/计划：打印用 white-space:pre-line 渲染换行，字段需带回原始换行
    expect(res.data.projectDescription, '项目概况应保留换行').toContain('\n');
    expect(res.data.projectPlan, '项目计划应保留换行').toContain('\n');
    console.log(`打印主体 OK：${res.data.projectName} / ${res.data.projectCode} / dept=${res.data.projectDept}`);
  });

  // ─────────────────────────────────────────────
  // 2. 立项日期取数：applyDate 往返相等（insert + resultMap）
  // ─────────────────────────────────────────────
  test('立项日期取数：getProject 返回 applyDate 且等于创建时所填值', async () => {
    const res = await api.get(`/project/project/${createdProjectId}`);
    expect(res.code).toBe(200);
    expect(res.data, 'applyDate 字段应存在（打印立项日期来源）').toHaveProperty('applyDate');
    // @JsonFormat(pattern="yyyy-MM-dd") 序列化，可直接前缀比对
    expect(String(res.data.applyDate).substring(0, 10),
      'applyDate 应等于创建时写入的值（验证 Mapper insert/resultMap）').toBe(APPLY_DATE_INIT);
    console.log(`立项日期取数 OK：applyDate=${res.data.applyDate}`);
  });

  // ─────────────────────────────────────────────
  // 3. applyDate 可更新（update 镜像）
  // ─────────────────────────────────────────────
  test('编辑修改 applyDate 后 getProject 读回为新值', async () => {
    const cur = (await api.get(`/project/project/${createdProjectId}`)).data;
    const updRes = await api.put('/project/project', { ...cur, applyDate: APPLY_DATE_UPDATED });
    expect(updRes.code, '编辑应返回200').toBe(200);

    const after = (await api.get(`/project/project/${createdProjectId}`)).data;
    expect(String(after.applyDate).substring(0, 10),
      'applyDate 应更新为新值（验证 Mapper update 镜像）').toBe(APPLY_DATE_UPDATED);
    console.log(`applyDate 更新 OK：${APPLY_DATE_INIT} -> ${after.applyDate}`);
  });

  // ─────────────────────────────────────────────
  // 4. applyDate 为空的项目，字段仍存在（前端引用需 property 在）
  // ─────────────────────────────────────────────
  test('applyDate 为空时字段仍随 getProject 返回（前端引用安全）', async () => {
    // 库中可能存在改造前的老项目（apply_date 为 NULL），抽样校验字段恒存在
    const list = await api.get('/project/project/list', { pageNum: 1, pageSize: 50 });
    expect(list.code).toBe(200);
    const sample = (list.rows || []).find(r => r.projectId !== createdProjectId) || list.rows[0];
    if (!sample) test.skip(true, '库中无其它项目可抽样');
    const res = await api.get(`/project/project/${sample.projectId}`);
    expect(res.code).toBe(200);
    // 不论有无值，property 必须存在，否则前端 printProject.applyDate 取到 undefined（仍安全）
    expect(res.data, 'applyDate property 应恒存在').toHaveProperty('applyDate');
    console.log(`抽样项目 ${sample.projectId} applyDate=${res.data.applyDate ?? '(空)'}`);
  });
});
