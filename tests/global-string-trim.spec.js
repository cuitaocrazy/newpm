/**
 * 全局字符串 trim E2E 测试
 *
 * 验证后端全局机制：输入数据在「查询 / 新建 / 编辑 / 提交」时自动去除首尾空格。
 * 两条入站链路：
 *   - GET 查询参数  → BaseController.@InitBinder + StringTrimmerEditor
 *   - @RequestBody  → ApplicationConfig 注册的 TrimStringJsonDeserializer
 *
 * 覆盖模块：项目管理 / 合同管理 / 付款里程碑管理，以及黑名单（密码不 trim）验证。
 *
 * 设计文档：docs/plans/2026-05-25-global-string-trim.md
 */

import { test, expect, request as playwrightRequest } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

const BASE_URL = 'http://localhost:80';
const TS = Date.now();

let api;
let createdContractId = null;
let createdPaymentId = null;
let createdProjectId = null;

test.describe.serial('全局字符串 trim', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    console.log('✅ 登录成功，开始全局 trim 测试');
  });

  test.afterAll(async () => {
    // 自清理：逆序删除创建的测试数据
    if (createdPaymentId) {
      try { await api.del(`/project/payment/${createdPaymentId}`); } catch { /* ignore */ }
    }
    if (createdProjectId) {
      try { await api.del(`/project/project/${createdProjectId}`); } catch { /* ignore */ }
    }
    if (createdContractId) {
      try { await api.del(`/project/contract/${createdContractId}`); } catch { /* ignore */ }
    }
    await api?.dispose();
  });

  // ─────────────────────────────────────────────
  // 合同管理：新建 trim + 查询 trim
  // ─────────────────────────────────────────────
  test('合同新建：首尾空格应被去除', async () => {
    const cleanName = `E2E_TRIM合同_${TS}`;
    const cleanCode = `E2E-TRIM-${TS}`;

    const customerList = await api.get('/project/customer/list', { pageNum: 1, pageSize: 1 });
    const customerId = customerList.rows.length > 0 ? customerList.rows[0].customerId : null;

    // 故意在名称、编号首尾加空格
    const res = await api.post('/project/contract', {
      contractName: `  ${cleanName}  `,
      contractCode: `\t${cleanCode} `,
      contractType: '1',
      contractStatus: '0',
      contractAmount: 10000.00,
      customerId,
      projectIds: []
    });
    expect(res.code, '新增合同应成功').toBe(200);

    // 用「干净值」精确查到它，说明入库已 trim（否则按干净值查不到）
    const listRes = await api.get('/project/contract/list', {
      pageNum: 1, pageSize: 10, contractName: cleanName
    });
    const created = listRes.rows.find(r => r.contractCode === cleanCode);
    expect(created, '应能按去空格后的编号查到合同').toBeTruthy();
    createdContractId = created.contractId;

    // 详情字段不应残留首尾空格
    const detail = await api.get(`/project/contract/${createdContractId}`);
    expect(detail.data.contractName).toBe(cleanName);
    expect(detail.data.contractCode).toBe(cleanCode);
    console.log(`✅ 合同新建 trim 通过：name="${detail.data.contractName}" code="${detail.data.contractCode}"`);
  });

  test('合同查询：查询条件首尾空格应被忽略', async () => {
    expect(createdContractId, '需先创建合同').toBeTruthy();
    const cleanName = `E2E_TRIM合同_${TS}`;

    // 查询条件带首尾空格，仍应命中（证明 GET @InitBinder trim 生效）
    const res = await api.get('/project/contract/list', {
      pageNum: 1, pageSize: 10, contractName: `   ${cleanName}   `
    });
    expect(res.code).toBe(200);
    const hit = res.rows.find(r => r.contractId === createdContractId);
    expect(hit, '带空格的查询条件应能命中合同').toBeTruthy();
    console.log('✅ 合同查询 trim 通过：带空格条件成功命中');
  });

  // ─────────────────────────────────────────────
  // 付款里程碑：新建 trim
  // ─────────────────────────────────────────────
  test('款项新建：首尾空格应被去除', async () => {
    expect(createdContractId, '需先创建合同作为款项归属').toBeTruthy();
    const cleanName = `E2E_TRIM款项_${TS}`;

    const res = await api.post('/project/payment', {
      contractId: createdContractId,
      paymentMethodName: `  ${cleanName}  `,
      paymentAmount: 5000
    });
    expect(res.code, '新增款项应成功').toBe(200);

    const listRes = await api.get('/project/payment/list', {
      pageNum: 1, pageSize: 50, paymentMethodName: cleanName
    });
    const created = listRes.rows.find(r => r.paymentMethodName === cleanName);
    expect(created, '应能按去空格后的名称查到款项').toBeTruthy();
    createdPaymentId = created.paymentId;

    const detail = await api.get(`/project/payment/${createdPaymentId}`);
    expect(detail.data.paymentMethodName).toBe(cleanName);
    console.log(`✅ 款项新建 trim 通过：name="${detail.data.paymentMethodName}"`);
  });

  // ─────────────────────────────────────────────
  // 项目管理：新建 trim + 查询 trim
  // ─────────────────────────────────────────────
  test('项目新建：首尾空格应被去除', async () => {
    const cleanName = `E2E_TRIM项目_${TS}`;
    const cleanCode = `ZH-BJ-11-TRIM-2026-${TS}`;

    const res = await api.post('/project/project', {
      projectName: `  ${cleanName}  `,
      projectCode: ` ${cleanCode} `,
      industry: 'ZH',
      region: 'BJ',
      regionId: '11',
      shortName: 'TRIM',
      establishedYear: '2026',
      projectCategory: 'RJKF',
      projectDept: '216',
      projectStatus: '1',
      acceptanceStatus: '0',
      estimatedWorkload: '10',
      projectBudget: '100000',
      projectManagerId: '1',
      projectDescription: '  E2E全局trim测试  '
    });
    expect(res.code, '新增项目应成功').toBe(200);

    // 用干净名称查询（同时验证查询条件 trim）
    const listRes = await api.get('/project/project/list', {
      pageNum: 1, pageSize: 10, projectName: `  ${cleanName}  `
    });
    expect(listRes.code).toBe(200);
    const created = listRes.rows.find(r => r.projectCode === cleanCode);
    expect(created, '应能按去空格后的编号查到项目（且查询条件带空格仍命中）').toBeTruthy();
    createdProjectId = created.projectId;

    const detail = await api.get(`/project/project/${createdProjectId}`);
    expect(detail.data.projectName).toBe(cleanName);
    expect(detail.data.projectCode).toBe(cleanCode);
    console.log(`✅ 项目新建+查询 trim 通过：name="${detail.data.projectName}" code="${detail.data.projectCode}"`);
  });

  // ─────────────────────────────────────────────
  // 黑名单验证：用户名 trim、密码不 trim
  // ─────────────────────────────────────────────
  test('登录：用户名首尾空格应被去除（仍可登录）', async () => {
    const ctx = await playwrightRequest.newContext({ baseURL: BASE_URL });
    try {
      const resp = await ctx.post('/dev-api/login', {
        data: { username: '  admin  ', password: '123456789' }
      });
      const body = await resp.json();
      expect(body.code, '用户名带空格应被 trim 后正常登录').toBe(200);
      expect(body.token, '应返回 token').toBeTruthy();
      console.log('✅ 用户名 trim 生效：带空格用户名成功登录');
    } finally {
      await ctx.dispose();
    }
  });

  test('登录：密码不应被 trim（带尾空格密码登录失败）', async () => {
    const ctx = await playwrightRequest.newContext({ baseURL: BASE_URL });
    try {
      const resp = await ctx.post('/dev-api/login', {
        data: { username: 'admin', password: '123456789 ' } // 尾部有空格
      });
      const body = await resp.json();
      // 密码在黑名单中不被 trim → 与真实密码不符 → 登录失败
      expect(body.code, '密码带空格不应被 trim，登录应失败').not.toBe(200);
      // 排除"验证码错误"等无关原因，确保是密码不匹配导致的失败
      expect(body.msg || '', '失败应源于密码不匹配，而非验证码等其它原因')
        .not.toContain('验证码');
      console.log(`✅ 密码黑名单生效：带空格密码登录被拒，msg="${body.msg}"`);
    } finally {
      await ctx.dispose();
    }
  });
});
