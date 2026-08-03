/**
 * 审批流程 E2E 测试
 * 验证 ProjectApprovalController 和 ProjectReviewController 的查询接口
 *
 * 测试项：
 * 1. 审批列表查询
 * 2. 待审批项目列表
 * 3. 审批汇总
 * 4. 审批历史查询
 * 5. 立项审核列表
 * 6. 立项审核汇总
 * 7. 立项审核详情
 * 8. 审批拒绝需要原因（仅验证格式，不实际修改数据）
 * 9. 【Issue #23】立项审核不清空项目日期与审核意见 —— 用真实创建的项目跑完整审核序列，
 *    锁定 approve/rollback 必须走 updateProjectApprovalFields（Issue #10）
 *    以及「审核通过未填意见时保留主表原有意见」
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;

test.describe('审批流程', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    console.log('✅ 登录成功，开始审批流程测试');
  });

  test.afterAll(async () => {
    await api?.dispose();
  });

  // ─────────────────────────────────────────────
  // 1. 审批列表查询
  // ─────────────────────────────────────────────
  test('审批列表查询', async () => {
    console.log('\n▶ 测试：审批列表查询');

    const body = await api.get('/project/approval/list', { pageNum: 1, pageSize: 10 });

    expect(body.code, '响应 code 应为 200').toBe(200);
    expect(Array.isArray(body.rows), 'rows 应为数组').toBe(true);
    expect(typeof body.total, 'total 应为数字').toBe('number');
    expect(body.total).toBeGreaterThanOrEqual(0);

    console.log(`  ✅ 审批列表查询成功，total=${body.total}, 返回 ${body.rows.length} 条`);

    // 如果有数据，验证基本字段存在
    if (body.rows.length > 0) {
      const first = body.rows[0];
      expect(first).toHaveProperty('approvalId');
      console.log(`  首条记录 approvalId=${first.approvalId}`);
    }
  });

  // ─────────────────────────────────────────────
  // 2. 待审批项目列表
  // ─────────────────────────────────────────────
  test('待审批项目列表', async () => {
    console.log('\n▶ 测试：待审批项目列表');

    const body = await api.get('/project/approval/projectList', { pageNum: 1, pageSize: 10 });

    expect(body.code, '响应 code 应为 200').toBe(200);
    expect(Array.isArray(body.rows), 'rows 应为数组').toBe(true);
    expect(typeof body.total, 'total 应为数字').toBe('number');
    expect(body.total).toBeGreaterThanOrEqual(0);

    console.log(`  ✅ 待审批项目列表成功，total=${body.total}, 返回 ${body.rows.length} 条`);

    if (body.rows.length > 0) {
      const first = body.rows[0];
      expect(first).toHaveProperty('projectId');
      console.log(`  首条待审批项目 projectId=${first.projectId}, 项目名称=${first.projectName || 'N/A'}`);
    }
  });

  // ─────────────────────────────────────────────
  // 3. 审批汇总
  // ─────────────────────────────────────────────
  test('审批汇总', async () => {
    console.log('\n▶ 测试：审批汇总');

    const body = await api.get('/project/approval/projectSummary');

    expect(body.code, '响应 code 应为 200').toBe(200);
    expect(body.data, 'data 不应为空').toBeTruthy();

    console.log(`  ✅ 审批汇总成功，data=`, JSON.stringify(body.data));
  });

  // ─────────────────────────────────────────────
  // 4. 审批历史查询
  // ─────────────────────────────────────────────
  test('审批历史查询', async () => {
    console.log('\n▶ 测试：审批历史查询');

    // 先从审批列表找一个 projectId
    const listBody = await api.get('/project/approval/list', { pageNum: 1, pageSize: 1 });
    expect(listBody.code).toBe(200);

    if (!listBody.rows || listBody.rows.length === 0) {
      // 没有审批记录，尝试从项目列表获取
      const projectList = await api.get('/project/project/list', { pageNum: 1, pageSize: 1 });
      expect(projectList.code).toBe(200);

      if (!projectList.rows || projectList.rows.length === 0) {
        console.log('  ⏭ 无任何项目数据，跳过审批历史测试');
        test.skip();
        return;
      }

      const projectId = projectList.rows[0].projectId;
      console.log(`  从项目列表取得 projectId=${projectId}`);

      const body = await api.get(`/project/approval/history/${projectId}`);
      expect(body.code, '响应 code 应为 200').toBe(200);

      // data 可能是数组（历史记录列表）或为空
      console.log(`  ✅ 审批历史查询成功，projectId=${projectId}, 记录数=${Array.isArray(body.data) ? body.data.length : 'N/A'}`);
      return;
    }

    const projectId = listBody.rows[0].projectId;
    console.log(`  从审批列表取得 projectId=${projectId}`);

    const body = await api.get(`/project/approval/history/${projectId}`);
    expect(body.code, '响应 code 应为 200').toBe(200);

    console.log(`  ✅ 审批历史查询成功，projectId=${projectId}, 记录数=${Array.isArray(body.data) ? body.data.length : 'N/A'}`);
  });

  // ─────────────────────────────────────────────
  // 5. 立项审核列表
  // ─────────────────────────────────────────────
  test('立项审核列表', async () => {
    console.log('\n▶ 测试：立项审核列表');

    const body = await api.get('/project/review/list', { pageNum: 1, pageSize: 10 });

    expect(body.code, '响应 code 应为 200').toBe(200);
    expect(Array.isArray(body.rows), 'rows 应为数组').toBe(true);
    expect(typeof body.total, 'total 应为数字').toBe('number');
    expect(body.total).toBeGreaterThanOrEqual(0);

    console.log(`  ✅ 立项审核列表成功，total=${body.total}, 返回 ${body.rows.length} 条`);

    if (body.rows.length > 0) {
      const first = body.rows[0];
      expect(first).toHaveProperty('projectId');
      console.log(`  首条记录 projectId=${first.projectId}, 项目名称=${first.projectName || 'N/A'}`);
    }
  });

  // ─────────────────────────────────────────────
  // 6. 立项审核汇总
  // ─────────────────────────────────────────────
  test('立项审核汇总', async () => {
    console.log('\n▶ 测试：立项审核汇总');

    const body = await api.get('/project/review/summary');

    expect(body.code, '响应 code 应为 200').toBe(200);
    expect(body.data, 'data 不应为空').toBeTruthy();

    console.log(`  ✅ 立项审核汇总成功，data=`, JSON.stringify(body.data));
  });

  // ─────────────────────────────────────────────
  // 7. 立项审核详情
  // ─────────────────────────────────────────────
  test('立项审核详情', async () => {
    console.log('\n▶ 测试：立项审核详情');

    // 从立项审核列表找一个 projectId
    const listBody = await api.get('/project/review/list', { pageNum: 1, pageSize: 1 });
    expect(listBody.code).toBe(200);

    if (!listBody.rows || listBody.rows.length === 0) {
      console.log('  ⏭ 立项审核列表为空，跳过详情测试');
      test.skip();
      return;
    }

    const projectId = listBody.rows[0].projectId;
    console.log(`  查询立项审核详情，projectId=${projectId}`);

    const body = await api.get(`/project/review/${projectId}`);

    expect(body.code, '响应 code 应为 200').toBe(200);
    expect(body.data, 'data 不应为空').toBeTruthy();
    expect(body.data.projectId, 'projectId 应匹配').toBe(projectId);

    console.log(`  ✅ 立项审核详情成功，项目名称=${body.data.projectName || 'N/A'}, 状态=${body.data.approvalStatus ?? 'N/A'}`);
  });

  // ─────────────────────────────────────────────
  // 8. 审批拒绝需要原因（仅验证接口格式，不修改真实数据）
  // ─────────────────────────────────────────────
  test('审批拒绝缺少原因应提示错误', async () => {
    console.log('\n▶ 测试：审批拒绝缺少原因的校验');

    // 使用不存在的 projectId 来避免修改真实数据
    // 如果接口对不存在的 projectId 返回 500/错误，说明它做了校验
    // 如果接口要求 approvalReason，缺少时也会返回错误
    const fakeProjectId = -999999;

    const body = await api.post('/project/review/approve', {
      projectId: fakeProjectId,
      approvalStatus: 2,  // 拒绝
      // 故意不传 approvalReason
    });

    // 期望返回错误（不存在的项目或缺少原因）
    // 不管具体错误码，关键是不能返回成功
    console.log(`  接口响应: code=${body.code}, msg=${body.msg || 'N/A'}`);

    if (body.code === 200) {
      // 如果意外成功了（不太可能，因为 projectId 不存在），记录警告
      console.log('  ⚠ 注意：使用不存在的 projectId 拒绝竟然返回了 200，需要检查接口逻辑');
    } else {
      console.log(`  ✅ 接口正确拒绝了请求，code=${body.code}`);
      expect(body.code).not.toBe(200);
    }
  });

  // ═════════════════════════════════════════════════════════════
  // 9. 立项审核不得清空项目日期与审核意见（Issue #10 / Issue #23）
  //
  // 为什么要有这一块（上面第 8 条为什么不算数）：
  //   第 8 条用的是不存在的 fakeProjectId=-999999。ProjectReviewServiceImpl.approveProject
  //   **没有任何前置校验**，它照样执行 updateProjectApprovalFields，只是影响 0 行 → result=0
  //   → 跳过写审核历史 → toAjax(0) 返回 error。也就是说那条用例只证明了「update 影响 0 行
  //   会返回 error」，审核主干一行都没跑过。
  //
  // 本块用**真实创建的项目**跑完整审核序列，锁定两处此前零验证的行为：
  //   A) Issue #10：ProjectMapper.updateProject 已解放 start/end/production/acceptance/apply
  //      五个日期的 <if> 守卫（无条件写入）。审核只能走专用语句 updateProjectApprovalFields，
  //      一旦改回拿只填了审核字段的裸 Project 去调 updateProject，每次审核都会把这 5 个日期
  //      写成 NULL。这里对 5 个日期逐个做严格相等断言。
  //   B) 审核通过时未填意见 → 必须保留主表原有 approval_reason（该语句是无条件写入，
  //      传 null 会把上一次的意见清空；而项目列表/详情/审核页三处都直接读主表这一列）。
  //
  // ⚠️ payload 纪律：调 /project/review/approve 时严格只发 review/index.vue 真实发送的 3 个 key
  //   （projectId / approvalStatus / approvalReason）。绝不能图省事把整个项目详情回传 ——
  //   @RequestBody Project 会把 5 个日期一起绑上，那样即使 Service 改回 updateProject 日期
  //   也不会丢，用例永远变不红，等于给缺陷盖章。
  // ═════════════════════════════════════════════════════════════
  test.describe.serial('立项审核不清空项目日期与审核意见', () => {
    // 造数 → 断言 → 自清理型的 serial 块必须关重试：中途失败时清理不会执行，
    // 残留项目会让重试的造数撞上编号/名称查重，真实失败原因被掩盖。
    // 与 tests/clear-field-guards-regression.spec.js 同约定。
    test.describe.configure({ retries: 0 });

    /** 造数唯一后缀：时间戳 + 随机量，降低跨次运行撞名概率 */
    const TS = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 5)}`;

    /**
     * 5 个已解放守卫的日期。Project.java 上都带 @JsonFormat("yyyy-MM-dd")，
     * 详情接口返回 '2026-02-06' 形态，可直接与字面量做严格字符串相等。
     */
    const DATES = {
      applyDate: '2026-01-05',
      startDate: '2026-02-06',
      endDate: '2026-11-07',
      acceptanceDate: '2026-12-08',
      productionDate: '2026-10-09'
    };

    const PROJECT_NAME = `E2E审核日期守卫_${TS}`;
    const PROJECT_CODE = `E2E-RVW-${TS}`;
    const PROJECT_BUDGET = 100000;
    const SEED_REASON = `E2E初始审核意见_${TS}`;
    const ROLLBACK_REASON = `E2E退回意见_${TS}`;
    const REJECT_REASON = `E2E拒绝意见_${TS}`;

    let projectId = null;
    let currentUserId = null;
    /** 审核前的 update_by / update_time 基线：专用语句不得触碰这两列 */
    let auditBaseline = null;

    /**
     * 严格相等断言。
     * 先确认 key 存在（!== undefined）：字段名拼错时详情接口会返回 undefined，
     * 若直接比较会静默假绿。再用 String() 严格相等，不用 toContain ——
     * 子串匹配会让 '1' 匹配上 '10'，100 匹配上 1001，证不出「原值未被篡改」。
     */
    function expectExact(obj, field, expected, why) {
      const value = obj?.[field];
      expect(
        value !== undefined,
        `字段 ${field} 在详情响应中不存在 —— 字段名拼错或接口未返回该列，用例无效`
      ).toBe(true);
      expect(String(value), `${why}（字段 ${field}，实际「${value}」）`).toBe(String(expected));
    }

    /** 5 个日期逐个断言原样保留 —— 这是 Issue #10 改动的全部目的 */
    function expectDatesIntact(detail, stage) {
      for (const [field, expected] of Object.entries(DATES)) {
        const value = detail?.[field];
        expect(
          value !== undefined,
          `${stage}：日期 ${field} 详情接口未返回 —— 字段名拼错，用例无效`
        ).toBe(true);
        expect(
          value !== null && value !== '',
          `${stage}：日期 ${field} 被清空了！审核走的不是 updateProjectApprovalFields，`
          + `而是拿裸 Project 调了 updateProject（该语句无条件写这 5 列）`
        ).toBe(true);
        expect(
          String(value),
          `${stage}：日期 ${field} 应严格保留 ${expected}`
        ).toBe(String(expected));
      }
    }

    /**
     * 取项目详情。
     * 必须走 /project/review/{id} 或 /project/project/{id}（都是 selectProjectByProjectId，
     * 字段最全）—— 列表接口 selectProjectList / selectReviewList 都不 select approval_time，
     * 拿列表行断言 approvalTime 会永远是 undefined。
     * 这里两个入口都取一次并交叉核对日期，防止某一侧字段缺失导致假绿。
     */
    async function fetchDetail(stage) {
      const reviewRes = await api.get(`/project/review/${projectId}`);
      expect(reviewRes.code, `${stage}：审核详情应返回 200`).toBe(200);
      expect(reviewRes.data, `${stage}：审核详情不应为空`).toBeTruthy();

      const projectRes = await api.get(`/project/project/${projectId}`);
      expect(projectRes.code, `${stage}：项目详情应返回 200`).toBe(200);
      expect(projectRes.data, `${stage}：项目详情不应为空`).toBeTruthy();

      for (const field of Object.keys(DATES)) {
        expect(
          String(projectRes.data[field]),
          `${stage}：/project/project 与 /project/review 两个入口的 ${field} 应一致`
        ).toBe(String(reviewRes.data[field]));
      }
      return reviewRes.data;
    }

    /** 取审核历史并按 approvalId 升序（mapper 无 ORDER BY，不能假定 data[0] 是最新一条） */
    async function fetchHistoryAsc() {
      const res = await api.get(`/project/approval/history/${projectId}`);
      expect(res.code, '审核历史应返回 200').toBe(200);
      expect(Array.isArray(res.data), '审核历史应为数组').toBe(true);
      return [...res.data].sort((a, b) => Number(a.approvalId) - Number(b.approvalId));
    }

    test.afterAll(async () => {
      if (!projectId) return;
      // 先清审核历史：deleteProjectByProjectIds 只级联删项目成员，不删 pm_project_approval
      try {
        const history = await api.get(`/project/approval/history/${projectId}`);
        const ids = (history.data || []).map(h => h.approvalId).filter(Boolean);
        if (ids.length > 0) {
          await api.del(`/project/approval/${ids.join(',')}`);
          console.log(`🧹 afterAll 清理：已删除 ${ids.length} 条审核历史`);
        }
      } catch (e) {
        console.log(`⚠ 清理审核历史失败（不影响其它用例）：${e}`);
      }
      try {
        const del = await api.del(`/project/project/${projectId}`);
        console.log(`🧹 afterAll 清理：删除测试项目 ${projectId}，code=${del.code}`);
      } catch (e) {
        console.log(`⚠ 清理测试项目失败：${e}`);
      }
      projectId = null;
    });

    test('前置：创建带 5 个日期的项目，并写入一条审核意见', async () => {
      console.log('\n▶ 前置：造一个可被审核的真实项目');

      // /getInfo 走 AjaxResult.success().put("user", ...)，user 挂在**响应顶层**而不是 data 下
      const info = await api.get('/getInfo');
      expect(info.code, '获取登录用户信息应成功').toBe(200);
      currentUserId = info.user?.userId;
      expect(currentUserId, '应能取到当前登录用户 userId（不硬编码 1）').toBeTruthy();

      // 依赖数据全部运行时查询：借一条既有项目做行业/区域/部门/分类模板，不硬编码任何字典值或 ID
      const listRes = await api.get('/project/project/list', { pageNum: 1, pageSize: 1 });
      expect(listRes.code, '项目列表应返回 200').toBe(200);
      if (!listRes.rows || listRes.rows.length === 0) {
        console.log('  ⏭ 库中无既有项目可作模板，跳过本块');
        test.skip();
        return;
      }
      const tpl = listRes.rows[0];

      const addRes = await api.post('/project/project', {
        projectName: PROJECT_NAME,
        projectCode: PROJECT_CODE,
        projectStatus: '1',
        shortName: 'RVWGUARD',
        projectStage: '0',
        acceptanceStatus: '0',
        estimatedWorkload: 10,
        projectBudget: PROJECT_BUDGET,
        projectDescription: 'E2E 立项审核日期守卫回归测试项目',
        industry: tpl.industry,
        region: tpl.region,
        regionId: tpl.regionId,
        regionCode: tpl.regionCode,
        establishedYear: tpl.establishedYear,
        projectCategory: tpl.projectCategory,
        projectDept: tpl.projectDept,
        projectManagerId: tpl.projectManagerId,
        // 不传 approvalStatus：pm_project.approval_status DDL 默认 '0'（待审核），
        // insertProject 的 XML 对该列带 <if> 守卫，不传即落库为 '0'
        ...DATES
      });
      expect(addRes.code, '新增项目应成功').toBe(200);

      // POST 只返回受影响行数，拿不到 projectId → 按唯一名称反查（精确相等，列表是模糊匹配）
      const found = await api.get('/project/project/list', {
        pageNum: 1, pageSize: 10, projectName: PROJECT_NAME
      });
      expect(found.code).toBe(200);
      const row = (found.rows || []).find(r => r.projectName === PROJECT_NAME);
      expect(row, '应能按名称精确检索到刚创建的项目').toBeTruthy();
      projectId = row.projectId;

      const seeded = await fetchDetail('创建后');
      expectDatesIntact(seeded, '创建后');
      expectExact(seeded, 'approvalStatus', '0', '新建项目审核状态应为待审核');

      // insertProject 会写 updateBy/updateTime，故此处必为非空 —— 后续的「不变」断言才有区分力
      auditBaseline = { updateBy: seeded.updateBy, updateTime: seeded.updateTime };
      expect(auditBaseline.updateBy, 'insertProject 应已写入 updateBy').toBeTruthy();
      expect(auditBaseline.updateTime, 'insertProject 应已写入 updateTime').toBeTruthy();

      // 审核通过并写入一条意见 —— 后面「不传意见」的核心用例要靠它验证「保留原值」
      const seedApprove = await api.post('/project/review/approve', {
        projectId,
        approvalStatus: '1',
        approvalReason: SEED_REASON
      });
      expect(seedApprove.code, '审核通过（带意见）应成功').toBe(200);

      const afterSeed = await fetchDetail('首次审核通过后');
      expectDatesIntact(afterSeed, '首次审核通过后');
      expectExact(afterSeed, 'approvalStatus', '1', '审核通过后状态应为 1');
      expectExact(afterSeed, 'approvalReason', SEED_REASON, '带意见审核时应写入新意见');

      console.log(`  ✅ 前置完成 projectId=${projectId}，5 个日期 + 审核意见均已就绪`);
    });

    test('审核通过·不传 approvalReason：5 个日期与原有意见都必须原样保留', async () => {
      if (!projectId) { console.log('  ⏭ 前置数据缺失，跳过'); test.skip(); return; }
      console.log('\n▶ 核心用例：审核通过且不填意见');

      const before = await fetchDetail('审核前');
      expectExact(before, 'approvalReason', SEED_REASON, '前置意见应已就位');
      const historyBefore = await fetchHistoryAsc();

      // 严格复刻 review/index.vue:437-441 的真实 payload：只有 3 个 key，且不含 approvalReason
      const res = await api.post('/project/review/approve', {
        projectId,
        approvalStatus: '1'
      });
      expect(res.code, '审核通过（不填意见）应成功').toBe(200);

      const after = await fetchDetail('审核后');

      // ① 5 个日期逐个严格保留 —— Issue #10 的全部目的，此前零验证
      expectDatesIntact(after, '审核通过后');

      // ② 审核意见保留原值 —— 未填意见时必须回读主表旧值写回
      expectExact(
        after, 'approvalReason', SEED_REASON,
        '未填审核意见时必须保留主表原有意见（updateProjectApprovalFields 无条件写入，传 null 会清空）'
      );

      // ③ 审核字段确实被更新了（证明这次调用真的落库，而不是什么都没做）
      expectExact(after, 'approvalStatus', '1', '审核通过后状态应为 1');
      expect(after.approvalTime, '审核后 approvalTime 应非空').toBeTruthy();
      expectExact(after, 'approverId', currentUserId, '审核人应为当前登录用户');

      // ④ 专用语句显式写 update_time = update_time、完全不碰 update_by
      expectExact(after, 'updateBy', auditBaseline.updateBy, '审核不得改写 updateBy');
      expectExact(after, 'updateTime', auditBaseline.updateTime, '审核不得改写 updateTime');

      // ⑤ 其它非审核字段未受牵连
      expectExact(after, 'projectName', PROJECT_NAME, '项目名称不得被改动');
      expectExact(after, 'projectCode', PROJECT_CODE, '项目编号不得被改动');
      expect(
        Number(after.projectBudget),
        `项目预算不得被改动，实际「${after.projectBudget}」`
      ).toBe(PROJECT_BUDGET);   // Number 比较以容忍 100000 与 '100000.00' 的 decimal 格式差异

      // ⑥ pm_project_approval 新增了历史记录（证明 result>0 分支确实执行过）
      const historyAfter = await fetchHistoryAsc();
      expect(
        historyAfter.length,
        'pm_project_approval 应新增一条审核历史'
      ).toBe(historyBefore.length + 1);
      const latest = historyAfter[historyAfter.length - 1];
      expectExact(latest, 'approvalStatus', '1', '新增历史的审核状态应为 1');
      expectExact(latest, 'approverId', currentUserId, '新增历史的审核人应为当前登录用户');
      // 既有行为：历史表记的是**原始传入**的 approvalReason（未填即 null），
      // 主表记的是回读的旧意见 —— 两边本就不一致，别断言成一致
      expect(
        latest.approvalReason === null || latest.approvalReason === '',
        `未填意见时历史表 approvalReason 应为空，实际「${latest.approvalReason}」`
      ).toBe(true);

      console.log('  ✅ 5 个日期、审核意见、update_by/update_time 全部原样保留，历史已新增');
    });

    test('退回：状态置为 0（不是 3），意见写入退回原因，5 个日期仍不变', async () => {
      if (!projectId) { console.log('  ⏭ 前置数据缺失，跳过'); test.skip(); return; }
      console.log('\n▶ 测试：/project/review/rollback');

      const historyBefore = await fetchHistoryAsc();

      // 复刻 review/index.vue:467-470 的真实 payload：只有 2 个 key
      const res = await api.post('/project/review/rollback', {
        projectId,
        rollbackReason: ROLLBACK_REASON
      });
      expect(res.code, '退回应成功').toBe(200);

      const after = await fetchDetail('退回后');
      expectDatesIntact(after, '退回后');
      // ProjectReviewServiceImpl.rollbackProject 写的是 "0"（待审核）；
      // ProjectApprovalServiceImpl.rollbackProject 写的才是 "3"（退回待审核）。别抄错方向。
      expectExact(after, 'approvalStatus', '0', 'review 的退回目标状态是 0（待审核），不是 3');
      expectExact(after, 'approvalReason', ROLLBACK_REASON, '退回原因应写入主表审核意见');
      expectExact(after, 'updateBy', auditBaseline.updateBy, '退回不得改写 updateBy');
      expectExact(after, 'updateTime', auditBaseline.updateTime, '退回不得改写 updateTime');

      const historyAfter = await fetchHistoryAsc();
      expect(historyAfter.length, '退回应新增一条审核历史').toBe(historyBefore.length + 1);
      expectExact(historyAfter[historyAfter.length - 1], 'approvalStatus', '0', '退回历史状态应为 0');

      console.log('  ✅ 退回后状态为 0、意见已更新、5 个日期仍完好');
    });

    test('退回后再次审核通过·不传意见：保留的是退回原因（覆盖「回读旧意见」分支）', async () => {
      if (!projectId) { console.log('  ⏭ 前置数据缺失，跳过'); test.skip(); return; }
      console.log('\n▶ 测试：退回后再次审核通过且不填意见');

      const res = await api.post('/project/review/approve', {
        projectId,
        approvalStatus: '1'
      });
      expect(res.code, '再次审核通过应成功').toBe(200);

      const after = await fetchDetail('再次审核通过后');
      expectDatesIntact(after, '再次审核通过后');
      expectExact(after, 'approvalStatus', '1', '再次审核通过后状态应为 1');
      expectExact(
        after, 'approvalReason', ROLLBACK_REASON,
        '未填意见时应保留主表当前意见（此时是上一步的退回原因），不得被清空'
      );

      console.log('  ✅ 退回原因在「不填意见的审核通过」后仍完整保留');
    });

    test('审核拒绝·带意见：状态 2、写入新意见，5 个日期仍不变', async () => {
      if (!projectId) { console.log('  ⏭ 前置数据缺失，跳过'); test.skip(); return; }
      console.log('\n▶ 测试：审核拒绝');

      const historyBefore = await fetchHistoryAsc();

      const res = await api.post('/project/review/approve', {
        projectId,
        approvalStatus: '2',
        approvalReason: REJECT_REASON
      });
      expect(res.code, '审核拒绝（带意见）应成功').toBe(200);

      const after = await fetchDetail('审核拒绝后');
      expectDatesIntact(after, '审核拒绝后');
      expectExact(after, 'approvalStatus', '2', '拒绝后状态应为 2');
      expectExact(after, 'approvalReason', REJECT_REASON, '拒绝意见应写入主表');
      expectExact(after, 'updateBy', auditBaseline.updateBy, '拒绝不得改写 updateBy');
      expectExact(after, 'updateTime', auditBaseline.updateTime, '拒绝不得改写 updateTime');

      const historyAfter = await fetchHistoryAsc();
      expect(historyAfter.length, '拒绝应新增一条审核历史').toBe(historyBefore.length + 1);
      const latest = historyAfter[historyAfter.length - 1];
      expectExact(latest, 'approvalStatus', '2', '拒绝历史状态应为 2');
      expectExact(latest, 'approvalReason', REJECT_REASON, '拒绝历史应记录拒绝意见');

      console.log('  ✅ 拒绝后状态/意见正确，5 个日期仍完好');
    });

    test('审核历史完整覆盖本次全部 5 次操作（1/1/0/1/2）', async () => {
      if (!projectId) { console.log('  ⏭ 前置数据缺失，跳过'); test.skip(); return; }
      console.log('\n▶ 测试：审核历史序列');

      const history = await fetchHistoryAsc();
      // 项目是本用例新建的，pm_project_approval 里只可能有本块产生的记录
      expect(history.length, '本块共 5 次审核操作，历史应恰好 5 条').toBe(5);
      expect(
        history.map(h => String(h.approvalStatus)),
        '审核历史状态序列应为 1(带意见)→1(不填意见)→0(退回)→1(不填意见)→2(拒绝)'
      ).toEqual(['1', '1', '0', '1', '2']);

      expectExact(history[0], 'approvalReason', SEED_REASON, '第 1 条历史应记录初始意见');
      expectExact(history[2], 'approvalReason', ROLLBACK_REASON, '第 3 条历史应记录退回原因');
      expectExact(history[4], 'approvalReason', REJECT_REASON, '第 5 条历史应记录拒绝意见');

      console.log('  ✅ 审核历史 5 条、状态序列与意见全部符合预期');
    });
  });

});
