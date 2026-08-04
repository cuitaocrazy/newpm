/**
 * 项目日报的人员可见性与口径自证 E2E（specs/018-project-daily-report）
 *
 * 覆盖 bdd/coverage.md 中的 API 层场景：
 *   ① 年月非必填：不填年月查询成功、无日期数据、个人人天走全周期
 *   ② 项目累计人天不因是否填年月而改变（SC-004）
 *   ③ 离场成员全周期可见（填了年月也要显示，Q2 拍板）
 *   ④ 机构分组返回项目所属部门名，且不覆盖成员本人部门
 *   ⑤ 角色标签按优先级返回单个值
 *   ⑥ 参与时间：日报首末日优先，无日报回退成员表区间
 *
 * 取数策略：以只读方式断言**真实业务数据**，不自造项目。
 * 理由：离场成员场景需要「曾是成员 + 已被移出 + 有历史工时」三个条件同时成立，
 * 自造要跑一遍完整的成员增删流程，而本地库已有 11 对天然样本（详见 spec.md SC-002）。
 * 每个用例都带防御性 skip：目标数据不存在时跳过而非失败，避免库变动导致假红。
 *
 * 安全边界由 e2e-team-daily-workload.spec.js:227-282 守护，本文件不重复。
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;

/** 已知样本：project 38「兵团人社全程电子化项目」，部门 215（新疆组） */
const SAMPLE_PROJECT_ID = 38;
const SAMPLE_DEPT_ID = 215;
/** 曲君：已被移出该项目（is_active=0），全周期工时 166h，日报 2026-03-02 ~ 2026-04-16 */
const FORMER_USER_ID = 250;
/** 与曲君历史工时无关的月份，用于验证「填了年月、当月无工时的离场成员仍可见」 */
const UNRELATED_MONTH = '2026-07';
/** 曲君在本项目确有工时的月份，用于验证按月口径未被全周期改动波及 */
const MONTH_WITH_HOURS = '2026-03';

function findProject(rows, projectId = SAMPLE_PROJECT_ID) {
  return (rows || []).find((p) => Number(p.projectId) === Number(projectId));
}

function findMember(project, userId) {
  return (project?.members || []).find((m) => Number(m.userId) === Number(userId));
}

test.beforeAll(async () => {
  api = await setupApi();
});

test.afterAll(async () => {
  if (api) await api.dispose();
});

test.describe('018 项目日报', () => {
  // ═══════════════ ① 年月非必填 ═══════════════

  test('不填年月时查询成功，且不返回任何日期维度数据（FR-007/008）', async () => {
    const res = await api.get('/project/dailyReport/teamMonthly', { deptId: SAMPLE_DEPT_ID });
    expect(res.code, '不填年月不应报错').toBe(200);
    expect(Array.isArray(res.data), '应返回项目数组').toBe(true);

    const withDates = (res.data || []).flatMap((p) => p.members || [])
      .filter((m) => Object.keys(m.dailyHours || {}).length > 0);
    expect(withDates.length, '不填年月时不应有任何日历格数据（否则前端会渲染 181 个日期列）').toBe(0);
  });

  test('不填年月时个人人天取全周期合计，不被吞成 0（FR-009）', async () => {
    // 这条守护 plan.md D2 那个缺陷：SQL 输出 NULL AS reportDate 时，
    // 旧的 `reportDate != null && totalWorkHours != null` 与条件会让 totalHours 停在 0。
    const res = await api.get('/project/dailyReport/teamMonthly', { deptId: SAMPLE_DEPT_ID });
    const project = findProject(res.data);
    // 只对「数据环境本身变了」做 skip；一旦项目还在，成员就必须在——
    // 否则「离场成员消失」这个正是本特性要防的回归会被 skip 悄悄吞掉。
    test.skip(!project, `样本项目 ${SAMPLE_PROJECT_ID} 不在部门 ${SAMPLE_DEPT_ID} 下，跳过`);

    const former = findMember(project, FORMER_USER_ID);
    expect(former, `离场成员 ${FORMER_USER_ID} 必须出现在全周期查询中（FR-001/FR-004）`).toBeTruthy();

    expect(Number(former.totalHours), '离场成员的全周期工时必须大于 0').toBeGreaterThan(0);
    expect(Object.keys(former.dailyHours || {}).length, '不填年月时该成员不应有日历格').toBe(0);
  });

  test('项目累计人天不因是否填年月而改变（SC-004）', async () => {
    const withMonth = await api.get('/project/dailyReport/teamMonthly', {
      deptId: SAMPLE_DEPT_ID, yearMonth: UNRELATED_MONTH
    });
    const without = await api.get('/project/dailyReport/teamMonthly', { deptId: SAMPLE_DEPT_ID });

    const a = findProject(withMonth.data);
    const b = findProject(without.data);
    test.skip(!a || !b, '样本项目缺失，跳过');

    expect(Number(b.actualPersonDays), '该列取自 pm_project.actual_workload，与 yearMonth 无关')
      .toBe(Number(a.actualPersonDays));
  });

  // ═══════════════ ③ 离场成员全周期可见 ═══════════════

  test('指定年月时，离场成员即使当月无工时也要显示（FR-004，Q2）', async () => {
    const res = await api.get('/project/dailyReport/teamMonthly', {
      deptId: SAMPLE_DEPT_ID, yearMonth: UNRELATED_MONTH
    });
    const project = findProject(res.data);
    test.skip(!project, '样本项目缺失，跳过');

    const former = findMember(project, FORMER_USER_ID);
    expect(former, `离场成员 ${FORMER_USER_ID} 在 ${UNRELATED_MONTH} 无工时，但仍须出现——`
      + '否则「项目累计人天」没有人员承载行，页面对不上账').toBeTruthy();
    expect(former.isFormer, '应标记为已离场').toBe(true);
    expect(Object.keys(former.dailyHours || {}).length, '该月无工时，日历应为空').toBe(0);
  });

  // ═══════════════ ④ 机构分组 ═══════════════

  test('项目层返回机构分组，且不覆盖成员本人部门（FR-013/014）', async () => {
    const res = await api.get('/project/dailyReport/teamMonthly', { deptId: SAMPLE_DEPT_ID });
    const project = findProject(res.data);
    test.skip(!project, '样本项目缺失，跳过');

    expect(project.projectDeptName, '项目层应有机构分组名').toBeTruthy();

    // 关键：成员本人部门是独立字段，实测 38% 的成员行与项目部门不同
    const members = project.members || [];
    expect(members.length, '样本项目应有成员').toBeGreaterThan(0);
    members.forEach((m) => {
      expect(m.deptName, `成员 ${m.nickName} 的部门不应为空（说明被项目部门覆盖或漏取）`).toBeTruthy();
    });

    const differing = members.filter((m) => m.deptName !== project.projectDeptName);
    console.log(`  ℹ️ 机构分组=${project.projectDeptName}，`
      + `其中 ${differing.length}/${members.length} 个成员的本人部门与之不同`);
  });

  // ═══════════════ ⑤ 角色标签 ═══════════════

  test('角色标签为单值且取自约定取值域（FR-011/012）', async () => {
    const ALLOWED = ['项目经理', '市场经理', '销售负责人', '参与人员'];
    const res = await api.get('/project/dailyReport/teamMonthly', { deptId: SAMPLE_DEPT_ID });
    const members = (res.data || []).flatMap((p) => p.members || []);
    test.skip(members.length === 0, '无成员数据，跳过');

    const labelled = members.filter((m) => m.roleLabel != null);
    expect(labelled.length, '应至少有部分成员能反推出角色').toBeGreaterThan(0);

    labelled.forEach((m) => {
      expect(ALLOWED, `角色「${m.roleLabel}」不在约定取值域内`).toContain(m.roleLabel);
      expect(m.roleLabel, '多角色须按优先级取一，不得拼接').not.toContain('/');
    });

    // 反推不出角色时必须是 null，不能是空串——否则前端会渲染出空括号
    const unlabelled = members.filter((m) => m.roleLabel == null);
    unlabelled.forEach((m) => {
      expect(m.roleLabel, '无角色时须为 null 而非空串').toBeNull();
    });
    console.log(`  ℹ️ 角色覆盖 ${labelled.length}/${members.length}`);
  });

  // ═══════════════ ⑥ 参与时间 ═══════════════

  test('参与时间：有日报用首末日，无日报回退成员表区间（FR-017）', async () => {
    const res = await api.get('/project/dailyReport/teamMonthly', { deptId: SAMPLE_DEPT_ID });
    const project = findProject(res.data);
    test.skip(!project, '样本项目缺失，跳过');

    const former = findMember(project, FORMER_USER_ID);
    expect(former, `离场成员 ${FORMER_USER_ID} 必须可见（FR-001）`).toBeTruthy();

    // 有工时的人必须有日报首末日
    expect(former.firstReportDate, '有工时的成员必须有日报首日').toBeTruthy();
    expect(former.lastReportDate, '有工时的成员必须有日报末日').toBeTruthy();
    expect(former.lastReportDate >= former.firstReportDate, '末日不得早于首日').toBe(true);
    expect(former.firstReportDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);

    // 从未填报的在册成员应有 joinDate 兜底
    const neverReported = (project.members || []).filter((m) => !m.firstReportDate);
    neverReported.forEach((m) => {
      expect(m.joinDate, `成员 ${m.nickName} 无日报时须有 joinDate 作为参与时间兜底`).toBeTruthy();
    });
    console.log(`  ℹ️ ${neverReported.length} 个成员走兜底（无日报），`
      + `${(project.members || []).length - neverReported.length} 个走日报首末日`);
  });

  // ═══════════════ 回归：既有字段未被破坏 ═══════════════

  test('回归：指定年月时日历格与个人人天仍按月口径（不得被全周期改动波及）', async () => {
    const res = await api.get('/project/dailyReport/teamMonthly', {
      deptId: SAMPLE_DEPT_ID, yearMonth: MONTH_WITH_HOURS
    });
    expect(res.code).toBe(200);

    const membersWithHours = (res.data || []).flatMap((p) => p.members || [])
      .filter((m) => Number(m.totalHours) > 0);

    // 这条断言必须真的跑到——用一个确有工时的月份，否则 forEach 空转、测试等于没写
    expect(membersWithHours.length,
      `${MONTH_WITH_HOURS} 应有工时数据，否则本用例是空跑`).toBeGreaterThan(0);

    membersWithHours.forEach((m) => {
      const dates = Object.keys(m.dailyHours || {});
      expect(dates.length, `${m.nickName} 有工时就应有日历格`).toBeGreaterThan(0);
      dates.forEach((d) => {
        expect(d.startsWith(MONTH_WITH_HOURS),
          `指定 ${MONTH_WITH_HOURS} 时不应出现其他月份的日历格：${d}`).toBe(true);
      });

      // 按月口径校验：个人人天须等于该月日历格之和 ÷ 8
      const sum = Object.values(m.dailyHours).reduce((a, b) => a + Number(b), 0);
      expect(Number(m.totalHours), `${m.nickName} 的个人工时应等于当月日历格之和`).toBeCloseTo(sum, 2);
    });
    console.log(`  ℹ️ ${MONTH_WITH_HOURS} 有工时成员 ${membersWithHours.length} 人，`
      + '日历格均落在本月且与个人工时对账一致');
  });
});
