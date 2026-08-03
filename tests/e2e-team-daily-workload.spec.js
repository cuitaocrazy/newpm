/**
 * 团队日报工时缺陷修复 E2E（Issue #5）
 *
 * 覆盖 5 个修复点：
 *   ① 项目有任务时，直挂父项目的工时不得被 actual_workload 吞掉
 *   ② pm_project_member 唯一约束 + 团队日报成员去重（工时不再翻倍）
 *   ③ 已离场填报人以 isFormer=true 呈现，工时不再整行消失
 *   ④ 项目编辑不能改 adjust_workload（必须走补正接口留痕）
 *   ⑤ syncMembers 软离场 + 重新加入时激活回 is_active='1'
 *
 * 纯 API 测试，自造自清：创建独立项目/任务/日报，afterAll 全部删除。
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;
let projectId = null;
let taskId = null;
let reportId = null;
let projectDept = null;
let otherUserId = null;

const ADMIN_USER_ID = 1;
const STAMP = Date.now();

// 使用当月一个固定日期（避免跨月边界）；后端 saveDailyReport 无周次限制
const now = new Date();
const yearMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
const reportDate = `${yearMonth}-15`;

/** 从 teamMonthly 结果里挑出目标项目 */
function findProject(rows) {
  return (rows || []).find((p) => Number(p.projectId) === Number(projectId));
}

/** 从项目里挑出某个成员行 */
function findMember(project, userId) {
  return (project?.members || []).find((m) => Number(m.userId) === Number(userId));
}

test.describe.serial('团队日报工时缺陷修复（Issue #5）', () => {
  test.beforeAll(async () => {
    api = await setupApi();

    // 取一个非 admin 的用户，用于把 admin 挤出成员名单
    const users = await api.get('/project/project/users');
    const candidate = (users.data || []).find((u) => Number(u.userId) !== ADMIN_USER_ID);
    otherUserId = candidate ? candidate.userId : null;
    console.log(`\n📋 Issue #5 E2E 启动，yearMonth=${yearMonth}, reportDate=${reportDate}, otherUserId=${otherUserId}`);
  });

  test.afterAll(async () => {
    if (!api) return;
    // 逆序清理：日报 → 任务 → 项目
    if (reportId) await api.del(`/project/dailyReport/${reportId}`);
    if (taskId) await api.del(`/project/task/${taskId}`);
    if (projectId) await api.del(`/project/project/${projectId}`);
    await api.dispose();
  });

  test('准备：创建测试项目（admin 为项目经理）', async () => {
    const projectName = `E2E工时缺陷_${STAMP}`;
    const createRes = await api.post('/project/project', {
      projectName,
      industry: 'ZH',
      region: 'BJ',
      regionId: '11',
      shortName: 'WLFIX',
      establishedYear: '2026',
      projectCode: `ZH-BJ-11-WLFIX-2026-${STAMP}`,
      projectCategory: 'RJKF',
      projectDept: '216',
      projectStatus: '1',
      acceptanceStatus: '0',
      estimatedWorkload: '10',
      projectBudget: '100000',
      projectManagerId: String(ADMIN_USER_ID),
      projectDescription: 'Issue #5 工时缺陷回归'
    });
    expect(createRes.code, '创建项目应成功').toBe(200);

    const list = await api.get('/project/project/list', { projectName, pageNum: 1, pageSize: 1 });
    expect(list.total, '应能查到刚建的项目').toBeGreaterThanOrEqual(1);
    projectId = list.rows[0].projectId;
    projectDept = list.rows[0].projectDept;
    console.log(`  ✅ 项目已创建 projectId=${projectId}, projectDept=${projectDept}`);

    // 项目经理应被同步进成员表（/project/member/{projectId} 返回在册成员明细，已过滤 is_active='1'）
    const members = (await api.get(`/project/member/${projectId}`)).data;
    const admin = (members || []).find((m) => Number(m.userId) === ADMIN_USER_ID);
    expect(admin, 'admin 作为项目经理应在成员表中').toBeTruthy();
  });

  test('①-前置：仅直挂父项目填报 8h，actual_workload 应为 8', async () => {
    const saveRes = await api.post('/project/dailyReport', {
      reportDate,
      detailList: [
        { projectId, subProjectId: null, entryType: 'work', workHours: 8, workContent: 'E2E 直挂父项目' }
      ]
    });
    expect(saveRes.code, '保存日报应成功').toBe(200);

    const detail = (await api.get(`/project/project/${projectId}`)).data;
    expect(Number(detail.actualWorkload), '直挂工时应计入 actual_workload').toBe(8);
    console.log(`  ✅ 直挂 8h 已计入，actualWorkload=${detail.actualWorkload}`);
  });

  test('① 核心回归：项目建任务后，直挂父项目的历史工时不得被抹掉', async () => {
    // 建任务
    const taskRes = await api.post('/project/task', {
      projectId,
      taskName: `E2E任务_${STAMP}`,
      taskCode: `E2ET-${STAMP}`,
      taskStage: '1'
    });
    expect(taskRes.code, '创建任务应成功').toBe(200);

    const taskList = await api.get('/project/task/list', { projectId, pageNum: 1, pageSize: 10 });
    const task = (taskList.rows || []).find((t) => t.taskId);
    expect(task, '应能查到刚建的任务').toBeTruthy();
    taskId = task.taskId;
    console.log(`  任务已创建 taskId=${taskId}`);

    // 同一天日报改为：直挂父项目 8h + 挂在任务上 4h（覆盖保存，走 update 分支）
    const saveRes = await api.post('/project/dailyReport', {
      reportDate,
      detailList: [
        { projectId, subProjectId: null, entryType: 'work', workHours: 8, workContent: 'E2E 直挂父项目' },
        { projectId, subProjectId: taskId, entryType: 'work', workHours: 4, workContent: 'E2E 任务工时', workCategory: '1' }
      ]
    });
    expect(saveRes.code, '保存含任务的日报应成功').toBe(200);

    const detail = (await api.get(`/project/project/${projectId}`)).data;
    // 修复前：actual_workload 会被 SUM(pm_task)=4 覆盖，直挂的 8h 永久丢失
    expect(Number(detail.actualWorkload), '父项目应为 直挂8 + 任务4 = 12，而非只有任务的 4')
      .toBe(12);
    console.log(`  ✅ ① 通过：actualWorkload=${detail.actualWorkload}（修复前会是 4）`);

    // 任务自身仍按 sub_project_id 汇总
    const taskDetail = (await api.get(`/project/task/${taskId}`)).data;
    expect(Number(taskDetail.actualWorkload), '任务实际工时应为 4').toBe(4);
  });

  test('② 团队日报成员不重复、工时不翻倍', async () => {
    const res = await api.get('/project/dailyReport/teamMonthly', { deptId: projectDept, yearMonth });
    expect(res.code).toBe(200);

    const project = findProject(res.data);
    expect(project, '团队日报应包含测试项目').toBeTruthy();

    const adminRows = (project.members || []).filter((m) => Number(m.userId) === ADMIN_USER_ID);
    expect(adminRows.length, 'admin 在同一项目下只应出现一行').toBe(1);
    expect(Number(adminRows[0].totalHours), '当月工时应为 12，不得翻倍').toBe(12);
    console.log(`  ✅ ② 通过：admin 单行，totalHours=${adminRows[0].totalHours}`);
  });

  test('③⑤ 移出成员后：软离场保留记录，工时仍以「已离场」呈现', async () => {
    test.skip(!otherUserId, '环境中没有第二个用户，跳过');

    // 把项目经理换成别人 → admin 不再属于任何成员角色
    const project = (await api.get(`/project/project/${projectId}`)).data;
    const putRes = await api.put('/project/project', {
      ...project,
      projectManagerId: otherUserId,
      participants: ''
    });
    expect(putRes.code, '编辑项目应成功').toBe(200);

    // 在册名单里不应再有 admin（说明已置 is_active='0'）
    const members = (await api.get(`/project/member/${projectId}`)).data;
    const stillActive = (members || []).find((m) => Number(m.userId) === ADMIN_USER_ID);
    expect(stillActive, 'admin 应已不在在册成员名单').toBeFalsy();

    // 但团队日报仍应显示他，且标记为已离场、工时不丢
    const res = await api.get('/project/dailyReport/teamMonthly', { deptId: projectDept, yearMonth });
    const teamProject = findProject(res.data);
    expect(teamProject, '团队日报仍应包含该项目').toBeTruthy();

    const formerAdmin = findMember(teamProject, ADMIN_USER_ID);
    expect(formerAdmin, '已离场但本月有工时的人必须显示，否则个人人天与实际人天对不上账').toBeTruthy();
    expect(formerAdmin.isFormer, 'isFormer 应为 true').toBe(true);
    expect(Number(formerAdmin.totalHours), '离场后工时仍应为 12').toBe(12);
    console.log(`  ✅ ③⑤ 通过：admin 以 isFormer=true 呈现，totalHours=${formerAdmin.totalHours}`);
  });

  test('⑤ 重新加回项目：应被激活回在册状态', async () => {
    test.skip(!otherUserId, '环境中没有第二个用户，跳过');

    const project = (await api.get(`/project/project/${projectId}`)).data;
    const putRes = await api.put('/project/project', {
      ...project,
      projectManagerId: String(ADMIN_USER_ID)
    });
    expect(putRes.code).toBe(200);

    // 修复前：is_active 会永久停在 '0'，admin 再也回不到在册名单
    const members = (await api.get(`/project/member/${projectId}`)).data;
    const backActive = (members || []).find((m) => Number(m.userId) === ADMIN_USER_ID);
    expect(backActive, '重新加入项目后应回到在册名单').toBeTruthy();
    expect(backActive.isActive, 'is_active 应被激活回 1').toBe('1');

    const res = await api.get('/project/dailyReport/teamMonthly', { deptId: projectDept, yearMonth });
    const admin = findMember(findProject(res.data), ADMIN_USER_ID);
    expect(admin, 'admin 应在团队日报中').toBeTruthy();
    expect(admin.isFormer, '回归后 isFormer 应为 false').toBe(false);
    console.log(`  ✅ ⑤ 通过：admin 已激活回在册，isFormer=${admin.isFormer}`);
  });

  test('④ 项目编辑不得改写 adjust_workload（必须走补正接口）', async () => {
    const before = (await api.get(`/project/project/${projectId}`)).data;
    const beforeAdjust = Number(before.adjustWorkload || 0);

    const putRes = await api.put('/project/project', {
      ...before,
      adjustWorkload: 99.5
    });
    expect(putRes.code, '编辑请求本身应成功（字段被静默忽略）').toBe(200);

    const after = (await api.get(`/project/project/${projectId}`)).data;
    expect(Number(after.adjustWorkload || 0), '调整人天不应被项目编辑改写')
      .toBe(beforeAdjust);
    console.log(`  ✅ ④ 通过：adjustWorkload 保持 ${after.adjustWorkload || 0}，未被 99.5 覆盖`);
  });

  test('安全回归：给「从未参与过的项目」注入工时，不得因此出现在团队日报中', async () => {
    test.skip(!otherUserId, '环境中没有第二个用户，跳过');

    // 造一个 admin 完全不沾的项目：项目经理是别人，参与人为空 → admin 无任何成员行
    const outsiderName = `E2E越权探针_${STAMP}`;
    const createRes = await api.post('/project/project', {
      projectName: outsiderName,
      industry: 'ZH', region: 'BJ', regionId: '11', shortName: 'PROBE',
      establishedYear: '2026',
      projectCode: `ZH-BJ-11-PROBE-2026-${STAMP}`,
      projectCategory: 'RJKF', projectDept: '216',
      projectStatus: '1', acceptanceStatus: '0',
      estimatedWorkload: '10', projectBudget: '999999',
      projectManagerId: otherUserId,
      projectDescription: '越权探针：admin 不是任何角色'
    });
    expect(createRes.code).toBe(200);

    const list = await api.get('/project/project/list', { projectName: outsiderName, pageNum: 1, pageSize: 1 });
    const outsiderId = list.rows[0].projectId;

    // admin 确实不在该项目在册名单中
    const members = (await api.get(`/project/member/${outsiderId}`)).data;
    expect((members || []).some((m) => Number(m.userId) === ADMIN_USER_ID), 'admin 不应是该项目成员').toBe(false);

    // 攻击动作：往「从未参与过的项目」注入工时
    // 【015 起】写入侧已加项目归属校验（specs/015-daily-report-ownership-check），
    // 这一步现在会被直接拒绝——防线从「读取侧过滤」前移到「写入侧拒绝」。
    // 修复前此处断言的是 toBe(200) 并注明「属既有问题」，那正是 015 修掉的缺口。
    const probeDate = `${yearMonth}-16`;
    const inject = await api.post('/project/dailyReport', {
      reportDate: probeDate,
      detailList: [{ projectId: outsiderId, entryType: 'work', workHours: 0.01, workContent: 'probe' }]
    });
    expect(inject.code, '第一道防线：写入侧应拒绝向从未参与的项目记录工时').toBe(500);
    expect(inject.msg, '拒绝提示须指明被拒项目').toContain('不在您参与的项目范围内');

    // 第二道防线（纵深防御，即使将来写入侧被绕过也须成立）：
    // 不得因任何注入而获得「离场成员」身份读到该项目
    // 团队日报的离场分支要求「曾是该项目成员」，成员行只能由持项目编辑权者写入，无法自助伪造。
    // 若有人删掉该 EXISTS 约束，此处会失败——那意味着任何账号可越权读取任意项目的
    // 预算 / 合同金额 / 收入确认金额。
    const res = await api.get('/project/dailyReport/teamMonthly', { deptId: projectDept, yearMonth });
    const probed = (res.data || []).find((p) => Number(p.projectId) === Number(outsiderId));
    if (probed) {
      const adminRow = (probed.members || []).find((m) => Number(m.userId) === ADMIN_USER_ID);
      expect(adminRow, 'admin 不得作为离场成员出现在从未参与的项目下').toBeFalsy();
    }
    console.log('  ✅ 安全回归通过：注入工时未换来越权可见性');

    // 清理探针数据
    const reports = await api.get('/project/dailyReport/list', { yearMonth, pageNum: 1, pageSize: 50 });
    const probeReport = (reports.rows || []).find((r) => String(r.reportDate || '').startsWith(probeDate));
    if (probeReport) await api.del(`/project/dailyReport/${probeReport.reportId}`);
    await api.del(`/project/project/${outsiderId}`);
  });

  test('清理前置：记录待删日报ID', async () => {
    const list = await api.get('/project/dailyReport/list', { yearMonth, pageNum: 1, pageSize: 50 });
    const mine = (list.rows || []).find((r) => String(r.reportDate || '').startsWith(reportDate));
    if (mine) {
      reportId = mine.reportId;
      console.log(`  待清理 reportId=${reportId}`);
    }
    expect(true).toBe(true);
  });
});
