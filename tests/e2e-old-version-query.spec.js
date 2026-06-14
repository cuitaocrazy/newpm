// 旧数据查询 (009) E2E —— 纯只读：list + 3 个 distinct 下拉端点 + 各查询条件过滤
// 依赖：本地插入的测试数据（comm_name='E2E测试'，3 条）。运行：
//   E2E_BASE_URL=http://localhost:8090 npx playwright test e2e-old-version-query.spec.js --reporter=list
const { test, expect } = require('@playwright/test')
const { setupApi } = require('./helpers/api-client')

const BASE = '/project/oldVersionOut'

let api
test.beforeAll(async () => { api = await setupApi() })
test.afterAll(async () => { await api.dispose() })

test('list: 返回分页结构且含测试数据', async () => {
  const body = await api.get(`${BASE}/list`, { pageNum: 1, pageSize: 50 })
  expect(body.code).toBe(200)
  expect(Array.isArray(body.rows)).toBeTruthy()
  expect(body.total).toBeGreaterThanOrEqual(3)
  const seed = body.rows.filter(r => r.commName === 'E2E测试')
  expect(seed.length).toBeGreaterThanOrEqual(3)
})

test('list: 任务编号模糊过滤', async () => {
  const body = await api.get(`${BASE}/list`, { taskNo: 'TASK-2025-001' })
  expect(body.code).toBe(200)
  expect(body.rows.every(r => r.taskNo.includes('TASK-2025-001'))).toBeTruthy()
  expect(body.rows.length).toBeGreaterThanOrEqual(1)
})

test('list: 投产批次号精确过滤', async () => {
  const body = await api.get(`${BASE}/list`, { proBatchNo: 'B2025001', pageSize: 50 })
  expect(body.code).toBe(200)
  expect(body.rows.every(r => r.proBatchNo === 'B2025001')).toBeTruthy()
  const seed = body.rows.filter(r => r.commName === 'E2E测试')
  expect(seed.length).toBeGreaterThanOrEqual(2)
})

test('list: 子产品精确过滤', async () => {
  const body = await api.get(`${BASE}/list`, { product: '产品Y' })
  expect(body.code).toBe(200)
  expect(body.rows.every(r => r.product === '产品Y')).toBeTruthy()
})

test('list: 版本类型精确过滤', async () => {
  const body = await api.get(`${BASE}/list`, { versionType: 'SP升级包' })
  expect(body.code).toBe(200)
  expect(body.rows.every(r => r.versionType === 'SP升级包')).toBeTruthy()
})

test('list: 多条件组合过滤', async () => {
  const body = await api.get(`${BASE}/list`, { proBatchNo: 'B2025001', versionType: 'SP升级包' })
  expect(body.code).toBe(200)
  expect(body.rows.every(r => r.proBatchNo === 'B2025001' && r.versionType === 'SP升级包')).toBeTruthy()
})

test('proBatchNoOptions: distinct 非空且含测试批次', async () => {
  const body = await api.get(`${BASE}/proBatchNoOptions`)
  expect(body.code).toBe(200)
  expect(Array.isArray(body.data)).toBeTruthy()
  expect(body.data).toContain('B2025001')
  expect(body.data).toContain('B2025002')
  expect(body.data.filter(v => v === 'B2025001').length).toBe(1)
})

test('productOptions: distinct 含测试产品', async () => {
  const body = await api.get(`${BASE}/productOptions`)
  expect(body.code).toBe(200)
  expect(body.data).toContain('产品X')
  expect(body.data).toContain('产品Y')
})

test('versionTypeOptions: distinct 含测试版本类型', async () => {
  const body = await api.get(`${BASE}/versionTypeOptions`)
  expect(body.code).toBe(200)
  expect(body.data).toContain('SP升级包')
  expect(body.data).toContain('B包升级包')
})
