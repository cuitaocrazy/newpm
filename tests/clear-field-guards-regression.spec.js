/**
 * 跨模块「可选字段能被清空」回归测试 —— <if> 守卫与全量表单提交语义错配
 *
 * 背景（与 tests/payment-clear-field-regression.spec.js 同源）：
 *   代码生成器模板 mapper.xml.vm 对每一列无差别生成 <if test="xxx != null"> 守卫。
 *   该守卫是「部分更新」语义，但所有 Controller 的 edit 都是「全量表单提交」语义。
 *   用户在表单上清空一个可选字段并保存：
 *     el-select / el-date-picker 清空 → v-model = undefined → JSON.stringify 丢弃该 key
 *     → 后端收到 null → 被 <if> 守卫拦下 → SQL 里不生成该列赋值 → 数据库保留旧值。
 *   接口返回 200、前端提示「修改成功」，失败完全静默。
 *
 * 本用例对 7 个模块的主 CRUD update 锁定两条路径：
 *   A. payload 中 key 缺失（前端 undefined 被 JSON.stringify 丢弃的真实形态）
 *   B. payload 中 key 显式为 null（前端归一化后的形态）
 *   并反向断言：必填字段 / 本次未解放的字段，其守卫仍生效（key 缺失时保留原值），
 *   以及有值提交时不得被误清空。
 *
 * 覆盖范围（15 个已解放字段）：
 *   ProjectMapper.updateProject            applyDate/startDate/endDate/acceptanceDate/productionDate
 *   TaskMapper.updateTask                  internalClosureDate/functionalTestDate/productionDate/
 *                                          productionVersionDate/actualProductionDate/taskBudget
 *   VersionOutMapper.updateVersionOut      outVersion/versionStatus
 *   ContractMapper.updateContract          contractSignDate
 *   CustomerMapper.updateCustomer          salesManagerId
 *
 * 另有两块验证「守卫兜底有效」（这两处 deptId 是必填字段，守卫保留是正确设计）：
 *   ProlistDefectMapper.updateProlistDefect / NobatchProlistDefectMapper.updateNobatchProlistDefect
 *
 * 注：修复只在后端。已实测验证 @RequestBody 绑定 Java Bean 时，
 *    「payload 中 key 缺失」与「显式传 null」效果完全相同，故前端无需做 undefined→null 归一。
 *    A / B 两条路径都保留，用于锁定这两种形态都能落库。
 *
 * 造数原则：所有依赖数据（项目/客户/部门/批次/子系统）一律运行时查询，查不到就 skip 该块，
 *          不硬编码任何 ID；每个块 afterAll 自清理。
 *
 * 运行：npx playwright test clear-field-guards-regression.spec.js --reporter=list
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;

// 本套件显式关闭重试（Issue #20）。
// 每个块都是「造数 → 断言 → 自清理」的 describe.serial：中途失败时清理用例不会执行，
// 残留记录会让重试的造数撞上 Service 层查重（如 problemNo），失败原因变成
// 「问题单编号已存在」，掩盖真实失败。这类用例失败就该直接暴露，不应重试。
test.describe.configure({ retries: 0 });

/** 造数唯一后缀：时间戳 + 随机量，降低跨进程／跨次运行撞名概率 */
const TS = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 5)}`;

test.beforeAll(async () => {
  api = await setupApi();
  console.log('✅ 登录成功，开始「可选字段能被清空」跨模块回归测试');
});

test.afterAll(async () => {
  await api?.dispose();
});

// ─────────────────────────────────────────────────────────────
// 公共断言 / payload 变换工具
// ─────────────────────────────────────────────────────────────

/**
 * 断言字段已被清空。
 * 只接受 null / 空串 —— 不接受 undefined：详情接口对 NULL 列会显式返回 null（已实测），
 * 若拿到 undefined 说明字段名拼错或接口没返回该列，那是用例自身的缺陷，必须暴露出来，
 * 否则整组「清空」断言会静默假绿。
 */
function expectCleared(obj, field) {
  const value = obj?.[field];
  expect(
    value !== undefined,
    `字段 ${field} 在详情响应中不存在 —— 字段名拼错或接口未返回该列，用例无效`
  ).toBe(true);
  expect(
    value === null || value === '',
    `字段 ${field} 应已被清空，实际为「${value}」`
  ).toBe(true);
}

/**
 * 断言字段仍保留原值（不得被静默清空 / 篡改）。
 * 用严格相等而非子串匹配 —— toContain 会让 '1' 匹配上 '10'/'11'，100 匹配上 1001，
 * 证不出「原值未被篡改」。
 */
function expectKept(obj, field, expected) {
  const value = obj?.[field];
  expect(
    value !== null && value !== undefined && value !== '',
    `字段 ${field} 不得被清空，实际为「${value}」`
  ).toBe(true);
  expect(String(value), `字段 ${field} 应严格保留原值 ${expected}`).toBe(String(expected));
}

/** 模拟前端 clearable 清空：v-model=undefined → JSON.stringify 直接丢弃该 key */
function omitKeys(source, fields) {
  const copy = { ...source };
  for (const f of fields) delete copy[f];
  return copy;
}

/** 模拟前端归一化：清空后显式传 null */
function nullKeys(source, fields) {
  const copy = { ...source };
  for (const f of fields) copy[f] = null;
  return copy;
}

// ═════════════════════════════════════════════════════════════
// 1. 项目管理 —— ProjectMapper.updateProject
//    /project/project  （PUT 全量表单提交，前端 project/edit.vue）
// ═════════════════════════════════════════════════════════════
test.describe.serial('项目管理 · updateProject 清空可选日期', () => {
  /** 前端非必填、可清空 —— 修复后必须无条件更新 */
  const CLEARABLE = {
    applyDate: '2026-01-05',
    startDate: '2026-02-06',
    endDate: '2026-11-07',
    acceptanceDate: '2026-12-08',
    productionDate: '2026-10-09'
  };
  /** 必填 / 本次未解放 —— 守卫仍生效，key 缺失时必须保留原值 */
  const GUARDED = {
    projectName: `E2E清空守卫_项目_${TS}`,
    projectCode: `E2E-CLR-P-${TS}`,
    projectStatus: '1'
  };

  let projectId = null;

  test.afterAll(async () => {
    if (projectId) {
      try {
        await api.del(`/project/project/${projectId}`);
        console.log(`🧹 afterAll 清理：已删除项目 ${projectId}`);
      } catch { /* 可能已清理 */ }
      projectId = null;
    }
  });

  test('前置：创建带完整可选日期的项目', async () => {
    // 依赖数据：借一条既有项目做行业/区域/部门模板，避免硬编码字典与部门 ID
    const listRes = await api.get('/project/project/list', { pageNum: 1, pageSize: 1 });
    expect(listRes.code, '项目列表应返回200').toBe(200);
    if (!listRes.rows || listRes.rows.length === 0) {
      console.log('⏭️ 库中无既有项目可作模板，跳过「项目管理」块');
      test.skip();
      return;
    }
    const tpl = listRes.rows[0];

    const addRes = await api.post('/project/project', {
      ...GUARDED,
      industry: tpl.industry,
      region: tpl.region,
      regionId: tpl.regionId,
      regionCode: tpl.regionCode,
      shortName: 'CLRGUARD',
      establishedYear: tpl.establishedYear,
      projectCategory: tpl.projectCategory,
      projectDept: tpl.projectDept,
      projectStage: '0',
      acceptanceStatus: '0',
      estimatedWorkload: 10,
      projectBudget: 100000,
      projectManagerId: tpl.projectManagerId,
      projectDescription: 'E2E清空守卫回归测试项目',
      ...CLEARABLE
    });
    expect(addRes.code, '新增项目应成功').toBe(200);

    const found = await api.get('/project/project/list', {
      pageNum: 1,
      pageSize: 10,
      projectName: GUARDED.projectName
    });
    expect(found.code).toBe(200);
    expect(found.rows.length, '应能按名称检索到刚创建的项目').toBeGreaterThan(0);
    projectId = found.rows[0].projectId;

    const detail = await api.get(`/project/project/${projectId}`);
    expect(detail.code).toBe(200);
    for (const [f, v] of Object.entries(CLEARABLE)) {
      expectKept(detail.data, f, v);
    }
    console.log(`📌 前置项目已创建，projectId=${projectId}，5 个可选日期均已赋值`);
  });

  test('A. key 缺失：可选日期应被真正清空，必填字段保留', async () => {
    if (!projectId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const detail = await api.get(`/project/project/${projectId}`);
    expect(detail.code).toBe(200);

    const submit = omitKeys(detail.data, [...Object.keys(CLEARABLE), ...Object.keys(GUARDED)]);
    const putRes = await api.put('/project/project', submit);
    expect(putRes.code, '修改项目应成功').toBe(200);

    const after = await api.get(`/project/project/${projectId}`);
    expect(after.code).toBe(200);
    for (const f of Object.keys(CLEARABLE)) expectCleared(after.data, f);
    for (const [f, v] of Object.entries(GUARDED)) expectKept(after.data, f, v);
    console.log('✅ 项目：key 缺失场景断言完成');
  });

  test('B. 显式 null：可选日期应被真正清空', async () => {
    if (!projectId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    // 先回填，构造可清空的初始状态
    const before = await api.get(`/project/project/${projectId}`);
    const refill = await api.put('/project/project', { ...before.data, ...CLEARABLE });
    expect(refill.code, '回填日期应成功').toBe(200);

    const refilled = await api.get(`/project/project/${projectId}`);
    for (const [f, v] of Object.entries(CLEARABLE)) expectKept(refilled.data, f, v);

    const putRes = await api.put('/project/project', nullKeys(refilled.data, Object.keys(CLEARABLE)));
    expect(putRes.code, '修改项目应成功').toBe(200);

    const after = await api.get(`/project/project/${projectId}`);
    for (const f of Object.keys(CLEARABLE)) expectCleared(after.data, f);
    console.log('✅ 项目：显式 null 场景断言完成');
  });

  test('C. 反向保护：有值提交时不得被误清空', async () => {
    if (!projectId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/project/${projectId}`);
    const putRes = await api.put('/project/project', {
      ...before.data,
      ...CLEARABLE,
      ...GUARDED
    });
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/project/${projectId}`);
    for (const [f, v] of Object.entries({ ...CLEARABLE, ...GUARDED })) {
      expectKept(after.data, f, v);
    }
    console.log('✅ 项目：反向保护断言完成');
  });

  test('清理：删除测试项目', async () => {
    if (!projectId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }
    const res = await api.del(`/project/project/${projectId}`);
    expect(res.code, '删除项目应成功').toBe(200);
    projectId = null;
  });
});

// ═════════════════════════════════════════════════════════════
// 2. 任务管理 —— TaskMapper.updateTask
//    /project/task  （PUT 全量表单提交，前端 subproject/edit.vue）
// ═════════════════════════════════════════════════════════════
test.describe.serial('任务管理 · updateTask 清空可选日期', () => {
  const CLEARABLE = {
    internalClosureDate: '2026-03-11',
    functionalTestDate: '2026-04-12',
    productionVersionDate: '2026-06-14',
    actualProductionDate: '2026-07-15',
    // taskBudget 是普通 el-input（无 clearable 属性），但用户手动删空后
    // subproject/edit.vue 会显式提交 null —— 同样被守卫吞掉。
    // 它是「可清空 ≠ 控件带 clearable」的典型反例。
    taskBudget: '88888.88'
  };
  /**
   * taskCode/taskName 是必填；startDate/endDate 前端 rules 必填 —— 守卫保留。
   *
   * productionDate 也在这里，原因与其它三个不同，务必看清：
   * 任务编辑页 subproject/edit.vue 上**根本没有这个字段**（计划投产日期由所选批次派生、
   * 只读展示），PUT 请求体里不带 productionDate。它一旦被解放守卫，每一次任务编辑
   * （哪怕只改任务名称）都会把它写成 NULL。所以它必须保留守卫，本块验证守卫兜住了。
   * 这也是本套件「用详情接口构造 payload」这一手法的盲区：payload 里有没有某个 key
   * 取决于真实表单，而不是详情接口返回什么。
   */
  const GUARDED = {
    taskCode: `E2E-CLR-T-${TS}`,
    taskName: `E2E清空守卫_任务_${TS}`,
    startDate: '2026-01-02',
    endDate: '2026-12-30',
    productionDate: '2026-05-13'
  };

  let taskId = null;

  test.afterAll(async () => {
    if (taskId) {
      try {
        await api.del(`/project/task/${taskId}`);
        console.log(`🧹 afterAll 清理：已删除任务 ${taskId}`);
      } catch { /* 可能已清理 */ }
      taskId = null;
    }
  });

  test('前置：创建带完整可选日期的任务', async () => {
    const projectRes = await api.get('/project/project/list', { pageNum: 1, pageSize: 1 });
    expect(projectRes.code).toBe(200);
    if (!projectRes.rows || projectRes.rows.length === 0) {
      console.log('⏭️ 库中无项目可挂任务，跳过「任务管理」块');
      test.skip();
      return;
    }
    const projectId = projectRes.rows[0].projectId;

    const addRes = await api.post('/project/task', {
      projectId,
      ...GUARDED,
      ...CLEARABLE
    });
    expect(addRes.code, '新增任务应成功').toBe(200);

    const found = await api.get('/project/task/list', {
      pageNum: 1,
      pageSize: 20,
      taskCode: GUARDED.taskCode
    });
    expect(found.code).toBe(200);
    const row = (found.rows || []).find(r => r.taskCode === GUARDED.taskCode);
    expect(row, '应能按编号检索到刚创建的任务').toBeTruthy();
    taskId = row.taskId;

    const detail = await api.get(`/project/task/${taskId}`);
    expect(detail.code).toBe(200);
    for (const [f, v] of Object.entries(CLEARABLE)) expectKept(detail.data, f, v);
    console.log(`📌 前置任务已创建，taskId=${taskId}，5 个可选日期均已赋值`);
  });

  test('A. key 缺失：可选日期应被真正清空，必填字段保留', async () => {
    if (!taskId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const detail = await api.get(`/project/task/${taskId}`);
    const submit = omitKeys(detail.data, [...Object.keys(CLEARABLE), ...Object.keys(GUARDED)]);
    const putRes = await api.put('/project/task', submit);
    expect(putRes.code, '修改任务应成功').toBe(200);

    const after = await api.get(`/project/task/${taskId}`);
    for (const f of Object.keys(CLEARABLE)) expectCleared(after.data, f);
    for (const [f, v] of Object.entries(GUARDED)) expectKept(after.data, f, v);
    console.log('✅ 任务：key 缺失场景断言完成');
  });

  test('B. 显式 null：可选日期应被真正清空', async () => {
    if (!taskId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/task/${taskId}`);
    const refill = await api.put('/project/task', { ...before.data, ...CLEARABLE });
    expect(refill.code, '回填日期应成功').toBe(200);

    const refilled = await api.get(`/project/task/${taskId}`);
    for (const [f, v] of Object.entries(CLEARABLE)) expectKept(refilled.data, f, v);

    const putRes = await api.put('/project/task', nullKeys(refilled.data, Object.keys(CLEARABLE)));
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/task/${taskId}`);
    for (const f of Object.keys(CLEARABLE)) expectCleared(after.data, f);
    console.log('✅ 任务：显式 null 场景断言完成');
  });

  test('C. 反向保护：有值提交时不得被误清空', async () => {
    if (!taskId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/task/${taskId}`);
    const putRes = await api.put('/project/task', { ...before.data, ...CLEARABLE, ...GUARDED });
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/task/${taskId}`);
    for (const [f, v] of Object.entries({ ...CLEARABLE, ...GUARDED })) {
      expectKept(after.data, f, v);
    }
    console.log('✅ 任务：反向保护断言完成');
  });

  test('清理：删除测试任务', async () => {
    if (!taskId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }
    const res = await api.del(`/project/task/${taskId}`);
    expect(res.code, '删除任务应成功').toBe(200);
    taskId = null;
  });
});

// ═════════════════════════════════════════════════════════════
// 3. 批次版本管理 —— VersionOutMapper.updateVersionOut
//    /project/versionOut  （PUT 全量表单提交，前端 versionOut/edit.vue）
//
//    真实触发路径：版本类型由 5/6（升级包）改成 1/2/3 时，
//    onVersionTypeChange() 会把 form.outVersion 置 null；版本状态是可选下拉，可直接清空。
//    这里用类型 1 建数（版本号按 maxCode 自增，可重复跑），只针对 mapper 守卫做断言。
// ═════════════════════════════════════════════════════════════
test.describe.serial('批次版本管理 · updateVersionOut 清空可选字段', () => {
  const CLEARABLE = {
    outVersion: `E2EOV${TS}`,
    versionStatus: 'E2E01'   // sys_version_status 是空字典，用字面量占位（varchar(20)）
  };
  /** 本次未解放 —— 守卫仍生效，key 缺失时必须保留原值 */
  const GUARDED = {
    remarks: `E2E清空守卫_备注_${TS}`
  };
  /** @Validated 必填，不能从 payload 里删；断言其在清空操作后未被篡改 */
  const REQUIRED_KEEP = ['sysName', 'versionType', 'versionBrief', 'packageMode'];

  const versionBrief = `E2E清空守卫_版本_${TS}`;
  let versionId = null;
  let sysName = null;
  let product = null;

  test.afterAll(async () => {
    if (versionId) {
      try {
        await api.del(`/project/versionOut/${versionId}`);
        console.log(`🧹 afterAll 清理：已删除批次版本 ${versionId}`);
      } catch { /* 可能已清理 */ }
      versionId = null;
    }
  });

  test('前置：创建带 outVersion / versionStatus 的批次版本', async () => {
    // 依赖 1：产品 → 子系统（遍历 sys_product 字典找一个真有子系统配置的产品）
    const dict = await api.get('/system/dict/data/type/sys_product');
    for (const d of (dict.data || [])) {
      const res = await api.get('/project/versionOut/sysNameByProduct', { product: d.dictValue });
      if (res.data && res.data.length > 0) {
        product = d.dictValue;
        sysName = res.data[0].sysName;
        break;
      }
    }
    // 依赖 2：投产批次
    const batchRes = await api.get('/project/productionBatch/list', { pageNum: 1, pageSize: 1 });
    const batch = batchRes.rows && batchRes.rows.length > 0 ? batchRes.rows[0] : null;

    if (!sysName || !batch) {
      console.log('⏭️ 无子系统配置(pm_sys_name)或无投产批次(pm_production_batch)，跳过「批次版本」块');
      test.skip();
      return;
    }

    const addRes = await api.post('/project/versionOut', {
      productionYear: batch.productionYear,
      batchId: batch.batchId,
      product,
      subVersionCode: product,
      sysName,
      versionType: '1',
      packageMode: '1',
      isInvolved: '0',
      dbUpdate: '0',
      usbUpdate: '0',
      versionBrief,
      versionDescr: 'E2E清空守卫回归',
      taskList: [],
      ...GUARDED,
      ...CLEARABLE
    });
    expect(addRes.code, '新增批次版本应成功').toBe(200);

    const list = await api.get('/project/versionOut/list', { pageNum: 1, pageSize: 50, sysName });
    expect(list.code).toBe(200);
    const row = (list.rows || []).find(r => r.versionBrief === versionBrief);
    expect(row, '应能检索到刚创建的批次版本').toBeTruthy();
    versionId = row.id;

    const detail = await api.get(`/project/versionOut/${versionId}`);
    expect(detail.code).toBe(200);
    for (const [f, v] of Object.entries({ ...CLEARABLE, ...GUARDED })) {
      expectKept(detail.data, f, v);
    }
    console.log(`📌 前置批次版本已创建，id=${versionId}，outVersion / versionStatus 均已赋值`);
  });

  test('A. key 缺失：outVersion / versionStatus 应被真正清空', async () => {
    if (!versionId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const detail = await api.get(`/project/versionOut/${versionId}`);
    const snapshot = detail.data;

    // @Validated 的必填字段必须留在 payload 里，否则请求在校验层就被拒
    const submit = omitKeys(snapshot, [...Object.keys(CLEARABLE), ...Object.keys(GUARDED)]);
    const putRes = await api.put('/project/versionOut', submit);
    expect(putRes.code, '修改批次版本应成功').toBe(200);

    const after = await api.get(`/project/versionOut/${versionId}`);
    for (const f of Object.keys(CLEARABLE)) expectCleared(after.data, f);
    for (const [f, v] of Object.entries(GUARDED)) expectKept(after.data, f, v);
    for (const f of REQUIRED_KEEP) expectKept(after.data, f, snapshot[f]);
    console.log('✅ 批次版本：key 缺失场景断言完成');
  });

  test('B. 显式 null：outVersion / versionStatus 应被真正清空', async () => {
    if (!versionId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/versionOut/${versionId}`);
    const refill = await api.put('/project/versionOut', { ...before.data, ...CLEARABLE });
    expect(refill.code, '回填字段应成功').toBe(200);

    const refilled = await api.get(`/project/versionOut/${versionId}`);
    for (const [f, v] of Object.entries(CLEARABLE)) expectKept(refilled.data, f, v);

    const putRes = await api.put('/project/versionOut', nullKeys(refilled.data, Object.keys(CLEARABLE)));
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/versionOut/${versionId}`);
    for (const f of Object.keys(CLEARABLE)) expectCleared(after.data, f);
    console.log('✅ 批次版本：显式 null 场景断言完成');
  });

  test('C. 反向保护：有值提交时不得被误清空', async () => {
    if (!versionId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/versionOut/${versionId}`);
    const putRes = await api.put('/project/versionOut', { ...before.data, ...CLEARABLE, ...GUARDED });
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/versionOut/${versionId}`);
    for (const [f, v] of Object.entries({ ...CLEARABLE, ...GUARDED })) {
      expectKept(after.data, f, v);
    }
    console.log('✅ 批次版本：反向保护断言完成');
  });

  test('清理：删除测试批次版本', async () => {
    if (!versionId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }
    const res = await api.del(`/project/versionOut/${versionId}`);
    expect(res.code, '删除批次版本应成功').toBe(200);
    versionId = null;
  });
});

// ═════════════════════════════════════════════════════════════
// 4. 合同管理 —— ContractMapper.updateContract
//    /project/contract  （PUT 全量表单提交，前端 contract/edit.vue）
// ═════════════════════════════════════════════════════════════
test.describe.serial('合同管理 · updateContract 清空合同签订日期', () => {
  const CLEARABLE = {
    contractSignDate: '2026-03-18'
  };
  const GUARDED = {
    contractName: `E2E清空守卫_合同_${TS}`,
    contractType: '1',
    contractAmount: 123456.78
  };

  let contractId = null;

  test.afterAll(async () => {
    if (contractId) {
      try {
        await api.del(`/project/contract/${contractId}`);
        console.log(`🧹 afterAll 清理：已删除合同 ${contractId}`);
      } catch { /* 可能已清理 */ }
      contractId = null;
    }
  });

  test('前置：创建带签订日期的合同', async () => {
    const customerRes = await api.get('/project/customer/list', { pageNum: 1, pageSize: 1 });
    expect(customerRes.code).toBe(200);
    if (!customerRes.rows || customerRes.rows.length === 0) {
      console.log('⏭️ 库中无客户，无法建合同，跳过「合同管理」块');
      test.skip();
      return;
    }
    const customerId = customerRes.rows[0].customerId;

    const deptRes = await api.get('/project/project/deptTree');
    const deptId = (deptRes.data || []).length > 0 ? deptRes.data[0].deptId : null;

    const addRes = await api.post('/project/contract', {
      ...GUARDED,
      contractCode: `E2E-CLR-C-${TS}`,
      contractStatus: '0',
      customerId,
      deptId,
      projectIds: [],
      ...CLEARABLE
    });
    expect(addRes.code, '新增合同应成功').toBe(200);

    const found = await api.get('/project/contract/list', {
      pageNum: 1,
      pageSize: 20,
      contractName: GUARDED.contractName
    });
    expect(found.code).toBe(200);
    const row = (found.rows || []).find(r => r.contractName === GUARDED.contractName);
    expect(row, '应能按名称检索到刚创建的合同').toBeTruthy();
    contractId = row.contractId;

    const detail = await api.get(`/project/contract/${contractId}`);
    expect(detail.code).toBe(200);
    for (const [f, v] of Object.entries(CLEARABLE)) expectKept(detail.data, f, v);
    console.log(`📌 前置合同已创建，contractId=${contractId}，contractSignDate 已赋值`);
  });

  test('A. key 缺失：contractSignDate 应被真正清空，必填字段保留', async () => {
    if (!contractId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const detail = await api.get(`/project/contract/${contractId}`);
    const submit = omitKeys(detail.data, [...Object.keys(CLEARABLE), ...Object.keys(GUARDED)]);
    const putRes = await api.put('/project/contract', submit);
    expect(putRes.code, '修改合同应成功').toBe(200);

    const after = await api.get(`/project/contract/${contractId}`);
    for (const f of Object.keys(CLEARABLE)) expectCleared(after.data, f);
    for (const [f, v] of Object.entries(GUARDED)) expectKept(after.data, f, v);
    console.log('✅ 合同：key 缺失场景断言完成');
  });

  test('B. 显式 null：contractSignDate 应被真正清空', async () => {
    if (!contractId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/contract/${contractId}`);
    const refill = await api.put('/project/contract', { ...before.data, ...CLEARABLE });
    expect(refill.code, '回填签订日期应成功').toBe(200);

    const refilled = await api.get(`/project/contract/${contractId}`);
    for (const [f, v] of Object.entries(CLEARABLE)) expectKept(refilled.data, f, v);

    const putRes = await api.put('/project/contract', nullKeys(refilled.data, Object.keys(CLEARABLE)));
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/contract/${contractId}`);
    for (const f of Object.keys(CLEARABLE)) expectCleared(after.data, f);
    console.log('✅ 合同：显式 null 场景断言完成');
  });

  test('C. 反向保护：有值提交时不得被误清空', async () => {
    if (!contractId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/contract/${contractId}`);
    const putRes = await api.put('/project/contract', { ...before.data, ...CLEARABLE, ...GUARDED });
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/contract/${contractId}`);
    for (const [f, v] of Object.entries({ ...CLEARABLE, ...GUARDED })) {
      expectKept(after.data, f, v);
    }
    console.log('✅ 合同：反向保护断言完成');
  });

  test('清理：删除测试合同', async () => {
    if (!contractId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }
    const res = await api.del(`/project/contract/${contractId}`);
    expect(res.code, '删除合同应成功').toBe(200);
    contractId = null;
  });
});

// ═════════════════════════════════════════════════════════════
// 5. 客户管理 —— CustomerMapper.updateCustomer
//    /project/customer  （PUT 全量表单提交，前端 customer/index.vue 弹窗表单）
// ═════════════════════════════════════════════════════════════
test.describe.serial('客户管理 · updateCustomer 清空销售负责人', () => {
  /** salesManagerId：前端 el-select clearable、rules 非必填 */
  const CLEARABLE = {};   // 运行时填入（销售负责人取真实用户 ID，不硬编码）
  /** 本次未解放 / 必填 —— 守卫仍生效 */
  const GUARDED = {
    customerAllName: `E2E清空守卫_客户全称_${TS}`,
    officeAddress: `E2E清空守卫_办公地址_${TS}`
  };
  const customerSimpleName = `E2E清空守卫_客户_${TS}`;

  let customerId = null;

  test.afterAll(async () => {
    if (customerId) {
      try {
        await api.del(`/project/customer/${customerId}`);
        console.log(`🧹 afterAll 清理：已删除客户 ${customerId}`);
      } catch { /* 可能已清理 */ }
      customerId = null;
    }
  });

  test('前置：创建带销售负责人的客户', async () => {
    const userRes = await api.get('/project/project/users');
    expect(userRes.code).toBe(200);
    if (!userRes.data || userRes.data.length === 0) {
      console.log('⏭️ 无可用用户作销售负责人，跳过「客户管理」块');
      test.skip();
      return;
    }
    CLEARABLE.salesManagerId = userRes.data[0].userId;

    const addRes = await api.post('/project/customer', {
      customerSimpleName,
      ...GUARDED,
      ...CLEARABLE,
      customerContactList: []
    });
    expect(addRes.code, '新增客户应成功').toBe(200);

    const found = await api.get('/project/customer/list', {
      pageNum: 1,
      pageSize: 20,
      customerSimpleName
    });
    expect(found.code).toBe(200);
    const row = (found.rows || []).find(c => c.customerSimpleName === customerSimpleName);
    expect(row, '应能按简称检索到刚创建的客户').toBeTruthy();
    customerId = row.customerId;

    const detail = await api.get(`/project/customer/${customerId}`);
    expect(detail.code).toBe(200);
    for (const [f, v] of Object.entries({ ...CLEARABLE, ...GUARDED })) {
      expectKept(detail.data, f, v);
    }
    console.log(`📌 前置客户已创建，customerId=${customerId}，salesManagerId=${CLEARABLE.salesManagerId}`);
  });

  test('A. key 缺失：salesManagerId 应被真正清空，其余字段保留', async () => {
    if (!customerId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const detail = await api.get(`/project/customer/${customerId}`);
    // customerSimpleName 必须留在 payload：Service 会用它做唯一性校验，缺失会直接抛异常
    const submit = omitKeys(detail.data, [...Object.keys(CLEARABLE), ...Object.keys(GUARDED)]);
    const putRes = await api.put('/project/customer', submit);
    expect(putRes.code, '修改客户应成功').toBe(200);

    const after = await api.get(`/project/customer/${customerId}`);
    for (const f of Object.keys(CLEARABLE)) expectCleared(after.data, f);
    for (const [f, v] of Object.entries(GUARDED)) expectKept(after.data, f, v);
    console.log('✅ 客户：key 缺失场景断言完成');
  });

  test('B. 显式 null：salesManagerId 应被真正清空', async () => {
    if (!customerId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/customer/${customerId}`);
    const refill = await api.put('/project/customer', { ...before.data, ...CLEARABLE });
    expect(refill.code, '回填销售负责人应成功').toBe(200);

    const refilled = await api.get(`/project/customer/${customerId}`);
    for (const [f, v] of Object.entries(CLEARABLE)) expectKept(refilled.data, f, v);

    const putRes = await api.put('/project/customer', nullKeys(refilled.data, Object.keys(CLEARABLE)));
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/customer/${customerId}`);
    for (const f of Object.keys(CLEARABLE)) expectCleared(after.data, f);
    console.log('✅ 客户：显式 null 场景断言完成');
  });

  test('C. 反向保护：有值提交时不得被误清空', async () => {
    if (!customerId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/customer/${customerId}`);
    const putRes = await api.put('/project/customer', { ...before.data, ...CLEARABLE, ...GUARDED });
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/customer/${customerId}`);
    for (const [f, v] of Object.entries({ ...CLEARABLE, ...GUARDED })) {
      expectKept(after.data, f, v);
    }
    console.log('✅ 客户：反向保护断言完成');
  });

  test('清理：删除测试客户', async () => {
    if (!customerId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }
    const res = await api.del(`/project/customer/${customerId}`);
    expect(res.code, '删除客户应成功').toBe(200);
    customerId = null;
  });
});

// ═════════════════════════════════════════════════════════════
// 6. 批次问题单及缺陷 —— ProlistDefectMapper.updateProlistDefect
//    /project/prolistDefect  （PUT 全量表单提交，前端 prolistDefect/edit.vue）
//
//    ⚠️ 造数刻意不带 taskId：ProlistDefectServiceImpl.updateProlistDefect 里的
//       syncDeptIdFromTask() 会在 taskId 非空时用任务所属项目的 project_dept
//       覆盖 deptId —— 那条路径下 deptId 永远不可能为空。
//       deptId 的清空语义只在「无任务关联（迁移形态）」的记录上成立，故此处按该形态造数。
// ═════════════════════════════════════════════════════════════
test.describe.serial('批次问题单 · updateProlistDefect 项目组(部门)守卫回归', () => {
  // deptId 在两个编辑表单里都是必填（rules 有「项目组不能为空」校验器，
// project-dept-select 也未开 clearable），故 mapper 守卫保留是正确设计。
// 本块验证守卫确实兜住了，防止将来有人连它一起解放。
const GUARDED_DEPT = {};   // 运行时填入 deptId（取真实部门，不硬编码）
  const GUARDED = {
    problemLevel: '1',
    currentStatus: '2',
    submitDate: '2026-06-01',
    verifyDate: '2026-06-06'
  };
  const problemNo = `E2E-CLR-PD-${TS}`;

  let problemId = null;

  test.afterAll(async () => {
    if (problemId) {
      try {
        await api.del(`/project/prolistDefect/${problemId}`);
        console.log(`🧹 afterAll 清理：已删除批次问题单 ${problemId}`);
      } catch { /* 可能已清理 */ }
      problemId = null;
    }
  });

  test('前置：创建带项目组的批次问题单（无任务关联）', async () => {
    const batchRes = await api.get('/project/productionBatch/list', { pageNum: 1, pageSize: 1 });
    const batch = batchRes.rows && batchRes.rows.length > 0 ? batchRes.rows[0] : null;
    const deptRes = await api.get('/project/project/deptTree');
    const dept = (deptRes.data || []).length > 0 ? deptRes.data[0] : null;

    if (!batch || !dept) {
      console.log('⏭️ 无投产批次或无可用部门，跳过「批次问题单」块');
      test.skip();
      return;
    }
    GUARDED_DEPT.deptId = dept.deptId;

    const addRes = await api.post('/project/prolistDefect', {
      problemNo,
      productionYear: batch.productionYear,
      batchId: batch.batchId,
      ...GUARDED,
      ...GUARDED_DEPT,
      whetherDefect: '1',
      whetherOvertime: '0',
      whetherProRecurrence: '0',
      whetherAttRequired: '1',
      whetherUpdateVersion: '0',
      defectDesc: 'E2E清空守卫回归',
      remarks: 'E2E'
    });
    expect(addRes.code, '新增批次问题单应成功').toBe(200);

    const found = await api.get('/project/prolistDefect/list', { pageNum: 1, pageSize: 20, problemNo });
    expect(found.code).toBe(200);
    const row = (found.rows || []).find(r => r.problemNo === problemNo);
    expect(row, '应能按编号检索到刚创建的问题单').toBeTruthy();
    problemId = row.problemId;

    const detail = await api.get(`/project/prolistDefect/${problemId}`);
    expect(detail.code).toBe(200);
    expect(detail.data.taskId, '造数应无任务关联，否则 deptId 会被 syncDeptIdFromTask 回填').toBeFalsy();
    for (const [f, v] of Object.entries({ ...GUARDED_DEPT, ...GUARDED })) {
      expectKept(detail.data, f, v);
    }
    console.log(`📌 前置批次问题单已创建，problemId=${problemId}，deptId=${GUARDED_DEPT.deptId}`);
  });

  test('A. key 缺失：deptId 属必填，守卫应保住原值', async () => {
    if (!problemId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const detail = await api.get(`/project/prolistDefect/${problemId}`);
    // problemNo 必须留在 payload：Service 用它做编号查重，缺失会直接抛异常
    const submit = omitKeys(detail.data, [...Object.keys(GUARDED_DEPT), ...Object.keys(GUARDED)]);
    const putRes = await api.put('/project/prolistDefect', submit);
    expect(putRes.code, '修改批次问题单应成功').toBe(200);

    const after = await api.get(`/project/prolistDefect/${problemId}`);
    for (const f of Object.keys(GUARDED_DEPT)) expectKept(after.data, f, GUARDED_DEPT[f]);
    for (const [f, v] of Object.entries(GUARDED)) expectKept(after.data, f, v);
    console.log('✅ 批次问题单：key 缺失场景断言完成');
  });

  test('B. 显式 null：deptId 属必填，守卫应保住原值', async () => {
    if (!problemId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/prolistDefect/${problemId}`);
    const refill = await api.put('/project/prolistDefect', { ...before.data, ...GUARDED_DEPT });
    expect(refill.code, '回填项目组应成功').toBe(200);

    const refilled = await api.get(`/project/prolistDefect/${problemId}`);
    for (const [f, v] of Object.entries(GUARDED_DEPT)) expectKept(refilled.data, f, v);

    const putRes = await api.put('/project/prolistDefect', nullKeys(refilled.data, Object.keys(GUARDED_DEPT)));
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/prolistDefect/${problemId}`);
    for (const f of Object.keys(GUARDED_DEPT)) expectKept(after.data, f, GUARDED_DEPT[f]);
    console.log('✅ 批次问题单：显式 null 场景断言完成');
  });

  test('C. 反向保护：有值提交时不得被误清空', async () => {
    if (!problemId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/prolistDefect/${problemId}`);
    const putRes = await api.put('/project/prolistDefect', { ...before.data, ...GUARDED_DEPT, ...GUARDED });
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/prolistDefect/${problemId}`);
    for (const [f, v] of Object.entries({ ...GUARDED_DEPT, ...GUARDED })) {
      expectKept(after.data, f, v);
    }
    console.log('✅ 批次问题单：反向保护断言完成');
  });

  test('清理：删除测试批次问题单', async () => {
    if (!problemId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }
    const res = await api.del(`/project/prolistDefect/${problemId}`);
    expect(res.code, '删除批次问题单应成功').toBe(200);
    problemId = null;
  });
});

// ═════════════════════════════════════════════════════════════
// 7. 非批次问题单及缺陷 —— NobatchProlistDefectMapper.updateNobatchProlistDefect
//    /project/nobatchProlist  （PUT 全量表单提交，前端 nobatchProlist/edit.vue）
// ═════════════════════════════════════════════════════════════
test.describe.serial('非批次问题单 · updateNobatchProlistDefect 项目组(部门)守卫回归', () => {
  // deptId 属必填字段，守卫保留是正确设计，本块验证其兜底有效。
const GUARDED_DEPT = {};   // 运行时填入 deptId
  const GUARDED = {
    problemLevel: '1',
    currentStatus: '2',
    submitDate: '2026-06-01',
    verifyDate: '2026-06-06'
  };
  const problemNo = `E2E-CLR-NB-${TS}`;

  let problemId = null;

  test.afterAll(async () => {
    if (problemId) {
      try {
        await api.del(`/project/nobatchProlist/${problemId}`);
        console.log(`🧹 afterAll 清理：已删除非批次问题单 ${problemId}`);
      } catch { /* 可能已清理 */ }
      problemId = null;
    }
  });

  test('前置：创建带项目组的非批次问题单', async () => {
    const batchRes = await api.get('/project/productionBatch/list', { pageNum: 1, pageSize: 1 });
    const batch = batchRes.rows && batchRes.rows.length > 0 ? batchRes.rows[0] : null;
    const deptRes = await api.get('/project/project/deptTree');
    const dept = (deptRes.data || []).length > 0 ? deptRes.data[0] : null;

    if (!batch || !dept) {
      console.log('⏭️ 无投产批次或无可用部门，跳过「非批次问题单」块');
      test.skip();
      return;
    }
    GUARDED_DEPT.deptId = dept.deptId;

    const addRes = await api.post('/project/nobatchProlist', {
      problemNo,
      productionYear: batch.productionYear,
      batchId: batch.batchId,
      taskNo: `E2E-CLR-NBT-${TS}`,
      taskName: 'E2E清空守卫_手填任务',
      ...GUARDED,
      ...GUARDED_DEPT,
      whetherDefect: '1',
      whetherOvertime: '0',
      whetherProRecurrence: '0',
      whetherAttRequired: '1',
      whetherUpdateVersion: '0',
      defectDesc: 'E2E清空守卫回归',
      remarks: 'E2E'
    });
    expect(addRes.code, '新增非批次问题单应成功').toBe(200);

    const found = await api.get('/project/nobatchProlist/list', { pageNum: 1, pageSize: 20, problemNo });
    expect(found.code).toBe(200);
    const row = (found.rows || []).find(r => r.problemNo === problemNo);
    expect(row, '应能按编号检索到刚创建的问题单').toBeTruthy();
    problemId = row.problemId;

    const detail = await api.get(`/project/nobatchProlist/${problemId}`);
    expect(detail.code).toBe(200);
    for (const [f, v] of Object.entries({ ...GUARDED_DEPT, ...GUARDED })) {
      expectKept(detail.data, f, v);
    }
    console.log(`📌 前置非批次问题单已创建，problemId=${problemId}，deptId=${GUARDED_DEPT.deptId}`);
  });

  test('A. key 缺失：deptId 属必填，守卫应保住原值', async () => {
    if (!problemId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const detail = await api.get(`/project/nobatchProlist/${problemId}`);
    // problemNo 必须留在 payload：Service 用它做编号查重，缺失会直接抛异常
    const submit = omitKeys(detail.data, [...Object.keys(GUARDED_DEPT), ...Object.keys(GUARDED)]);
    const putRes = await api.put('/project/nobatchProlist', submit);
    expect(putRes.code, '修改非批次问题单应成功').toBe(200);

    const after = await api.get(`/project/nobatchProlist/${problemId}`);
    for (const f of Object.keys(GUARDED_DEPT)) expectKept(after.data, f, GUARDED_DEPT[f]);
    for (const [f, v] of Object.entries(GUARDED)) expectKept(after.data, f, v);
    console.log('✅ 非批次问题单：key 缺失场景断言完成');
  });

  test('B. 显式 null：deptId 属必填，守卫应保住原值', async () => {
    if (!problemId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/nobatchProlist/${problemId}`);
    const refill = await api.put('/project/nobatchProlist', { ...before.data, ...GUARDED_DEPT });
    expect(refill.code, '回填项目组应成功').toBe(200);

    const refilled = await api.get(`/project/nobatchProlist/${problemId}`);
    for (const [f, v] of Object.entries(GUARDED_DEPT)) expectKept(refilled.data, f, v);

    const putRes = await api.put('/project/nobatchProlist', nullKeys(refilled.data, Object.keys(GUARDED_DEPT)));
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/nobatchProlist/${problemId}`);
    for (const f of Object.keys(GUARDED_DEPT)) expectKept(after.data, f, GUARDED_DEPT[f]);
    console.log('✅ 非批次问题单：显式 null 场景断言完成');
  });

  test('C. 反向保护：有值提交时不得被误清空', async () => {
    if (!problemId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const before = await api.get(`/project/nobatchProlist/${problemId}`);
    const putRes = await api.put('/project/nobatchProlist', { ...before.data, ...GUARDED_DEPT, ...GUARDED });
    expect(putRes.code).toBe(200);

    const after = await api.get(`/project/nobatchProlist/${problemId}`);
    for (const [f, v] of Object.entries({ ...GUARDED_DEPT, ...GUARDED })) {
      expectKept(after.data, f, v);
    }
    console.log('✅ 非批次问题单：反向保护断言完成');
  });

  test('清理：删除测试非批次问题单', async () => {
    if (!problemId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }
    const res = await api.del(`/project/nobatchProlist/${problemId}`);
    expect(res.code, '删除非批次问题单应成功').toBe(200);
    problemId = null;
  });
});

// ═════════════════════════════════════════════════════════════
// 8. 收入确认 —— updateProject 的第二个 Controller 入口
//    PUT /project/project/revenue （前端 revenue/company/detail.vue）
//
//    该入口与项目编辑表单共用 ProjectMapper.updateProject，而本次已解放
//    5 个日期字段的守卫。它当前之所以安全，靠的是
//    GET /project/project/revenue/{id} 返回完整实体（实测 96 字段，
//    与 GET /project/project/{id} 一致），前端 form.value = response.data
//    后整体回传，字段齐全。
//
//    这个安全性依赖「查询 SQL 返回全字段」这一实现细节，没有任何测试锁定它。
//    若将来有人优化该查询只 select 需要的列，5 个日期会在每次收入确认时被
//    静默清空 —— 与本文件所修复的缺陷完全同型。本块锁定该前提。（Issue #19）
// ═════════════════════════════════════════════════════════════
test.describe.serial('收入确认 · PUT /project/project/revenue 不得清空项目日期', () => {
  /** 已解放守卫的 5 个日期 —— 收入确认操作绝不能碰它们 */
  const DATES = {
    applyDate: '2026-01-21',
    startDate: '2026-02-22',
    endDate: '2026-11-23',
    acceptanceDate: '2026-12-24',
    productionDate: '2026-10-25'
  };
  const projectName = `E2E收入确认守卫_项目_${TS}`;
  const projectCode = `E2E-REV-P-${TS}`;

  let projectId = null;

  test.afterAll(async () => {
    if (projectId) {
      try {
        await api.del(`/project/project/${projectId}`);
        console.log(`🧹 afterAll 清理：已删除项目 ${projectId}`);
      } catch { /* 可能已清理 */ }
      projectId = null;
    }
  });

  test('前置：创建带完整日期的项目', async () => {
    const listRes = await api.get('/project/project/list', { pageNum: 1, pageSize: 1 });
    expect(listRes.code).toBe(200);
    if (!listRes.rows || listRes.rows.length === 0) {
      console.log('⏭️ 库中无既有项目可作模板，跳过「收入确认」块');
      test.skip();
      return;
    }
    const tpl = listRes.rows[0];

    const addRes = await api.post('/project/project', {
      projectName,
      projectCode,
      projectStatus: '1',
      industry: tpl.industry,
      region: tpl.region,
      regionId: tpl.regionId,
      regionCode: tpl.regionCode,
      shortName: 'REVGUARD',
      establishedYear: tpl.establishedYear,
      projectCategory: tpl.projectCategory,
      projectDept: tpl.projectDept,
      projectStage: '0',
      acceptanceStatus: '0',
      estimatedWorkload: 10,
      projectBudget: 100000,
      projectManagerId: tpl.projectManagerId,
      projectDescription: 'E2E收入确认守卫回归测试项目',
      ...DATES
    });
    expect(addRes.code, '新增项目应成功').toBe(200);

    const found = await api.get('/project/project/list', { pageNum: 1, pageSize: 10, projectName });
    expect(found.rows.length, '应能按名称检索到刚创建的项目').toBeGreaterThan(0);
    projectId = found.rows[0].projectId;
    console.log(`📌 前置项目已创建，projectId=${projectId}`);
  });

  test('A. 契约锁定：收入确认详情接口必须返回全部 5 个日期', async () => {
    if (!projectId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const rev = await api.get(`/project/project/revenue/${projectId}`);
    expect(rev.code, '收入确认详情应返回200').toBe(200);

    // 前端 detail.vue 是 form.value = response.data 后整体回传，
    // 该接口一旦漏返回任何一个日期，回传时就是 null，守卫已去掉 → 直接被清空
    for (const [f, v] of Object.entries(DATES)) {
      expectKept(rev.data, f, v);
    }
    console.log('✅ 收入确认：详情接口字段完整性契约成立');
  });

  test('B. 收入确认操作后，5 个日期必须保持原值', async () => {
    if (!projectId) { console.log('⏭️ 前置数据缺失，跳过'); test.skip(); return; }

    const rev = await api.get(`/project/project/revenue/${projectId}`);
    expect(rev.code).toBe(200);

    // 模拟 revenue/company/detail.vue 的提交：整体回传 + 填入收入确认必填项
    const putRes = await api.put('/project/project/revenue', {
      ...rev.data,
      revenueConfirmStatus: '1',
      revenueConfirmYear: String(new Date().getFullYear()),
      confirmAmount: 50000,
      taxRate: 6
    });
    expect(putRes.code, '收入确认应成功').toBe(200);

    const after = await api.get(`/project/project/${projectId}`);
    expect(after.code).toBe(200);
    for (const [f, v] of Object.entries(DATES)) {
      expectKept(after.data, f, v);
    }
    // 确认收入确认本身生效了，否则上面的断言可能是「什么都没发生」的假绿
    expect(String(after.data.revenueConfirmStatus), '收入确认状态应已更新').toBe('1');
    console.log('✅ 收入确认：5 个日期未被误清空，且确认操作确实生效');
  });

  test('清理：删除测试项目', async () => {
    if (!projectId) { test.skip(); return; }
    const res = await api.del(`/project/project/${projectId}`);
    expect(res.code, '删除项目应成功').toBe(200);
    projectId = null;
    console.log('🧹 测试项目已删除');
  });
});
