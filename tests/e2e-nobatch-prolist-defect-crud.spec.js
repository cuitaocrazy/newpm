// 非批次任务问题单及缺陷 (011) E2E —— 全端点 + 联动(仅批次→计划投产日期) + 查重 + 软删坑13 + 派生
// 运行：E2E_BASE_URL=http://localhost:8090 npx playwright test e2e-nobatch-prolist-defect-crud.spec.js --reporter=list
const { test, expect } = require('@playwright/test')
const { setupApi } = require('./helpers/api-client')

const BASE = '/project/nobatchProlist'
const YEAR = '2026'
const BATCH_ID = 1
const DEPT_ID = 201

let api
const created = []
test.beforeAll(async () => { api = await setupApi() })
test.afterAll(async () => {
  for (const id of created) { try { await api.del(`${BASE}/${id}`) } catch {} }
  await api.dispose()
})

function payload(problemNo, overrides = {}) {
  return {
    taskNo: 'NB-E2E-TASK', taskName: '手填任务', product: 'HHAP-COR',
    internalClosureDate: '2026-05-01', functionalTestDate: '2026-05-10', productionVersionDate: '2026-05-20',
    scheduleStatus: '1', productionYear: YEAR, batchId: BATCH_ID, deptId: DEPT_ID,
    problemNo, problemLevel: '1', currentStatus: '2',
    submitDate: '2026-06-01', settleDate: '2026-06-05', verifyDate: '2026-06-06',
    whetherDefect: '1', whetherOvertime: '0', whetherProRecurrence: '0',
    whetherAttRequired: '1', whetherUpdateVersion: '0',
    defectDesc: 'E2E非批次缺陷', remarks: 'E2E', ...overrides
  }
}

test('联动 batchByYear', async () => {
  const r = await api.get(`${BASE}/batchByYear`, { year: YEAR })
  expect(r.code).toBe(200)
  expect(r.data.some(b => b.batchId === BATCH_ID)).toBeTruthy()
})

test('联动 tcDate 进data', async () => {
  const r = await api.get(`${BASE}/tcDate`, { batchId: BATCH_ID })
  expect(r.code).toBe(200)
  expect(r.data).toBeTruthy()
})

test('查重 不存在→true', async () => {
  const r = await api.get(`${BASE}/checkProblemNo`, { problemNo: 'NB-NOTEXIST-XYZ' })
  expect(r.data).toBe(true)
})

test('新增 + 列表(手填任务字段实存 + JOIN + 派生)', async () => {
  const add = await api.post(BASE, payload('NB-E2E-001'))
  expect(add.code).toBe(200)
  const list = await api.get(`${BASE}/list`, { problemNo: 'NB-E2E-001' })
  expect(list.total).toBeGreaterThanOrEqual(1)
  const row = list.rows[0]
  created.push(row.problemId)
  expect(row.taskNo).toBe('NB-E2E-TASK')   // 手填实存
  expect(row.taskName).toBe('手填任务')
  expect(row.product).toBe('HHAP-COR')
  expect(row.batchNo).toBeTruthy()         // JOIN pm_production_batch
  expect(row.deptName).toBeTruthy()        // JOIN sys_dept
  expect(row.createByName).toBeTruthy()    // JOIN sys_user COLLATE
  expect(row.solutionTimeOverOneDay).toBe('1')
})

test('查重 已存在→false', async () => {
  const r = await api.get(`${BASE}/checkProblemNo`, { problemNo: 'NB-E2E-001' })
  expect(r.data).toBe(false)
})

test('新增重复编号→拒绝(500)', async () => {
  const r = await api.post(BASE, payload('NB-E2E-001'))
  expect(r.code).toBe(500)
})

test('详情', async () => {
  const r = await api.get(`${BASE}/${created[0]}`)
  expect(r.code).toBe(200)
  expect(r.data.taskNo).toBe('NB-E2E-TASK')
})

test('编辑: 同号排除自己 + settle空派生用当天', async () => {
  const id = created[0]
  const r = await api.put(BASE, payload('NB-E2E-001', { problemId: id, settleDate: null, currentStatus: '5' }))
  expect(r.code).toBe(200)
  const d = await api.get(`${BASE}/${id}`)
  expect(d.data.currentStatus).toBe('5')
  expect(d.data.settleDate).toBeFalsy()
  expect(d.data.solutionTimeOverOneDay).toBe('1')
})

test('软删除 + 重建同号(坑13)', async () => {
  const id = created[0]
  expect((await api.del(`${BASE}/${id}`)).code).toBe(200)
  created.length = 0
  const list = await api.get(`${BASE}/list`, { problemNo: 'NB-E2E-001' })
  expect(list.rows.every(r => r.problemNo !== 'NB-E2E-001')).toBeTruthy()
  const re = await api.post(BASE, payload('NB-E2E-001'))
  expect(re.code).toBe(200)
  const relist = await api.get(`${BASE}/list`, { problemNo: 'NB-E2E-001' })
  created.push(relist.rows[0].problemId)
})

test('多维查询过滤', async () => {
  const r = await api.get(`${BASE}/list`, { productionYear: YEAR, batchId: BATCH_ID, whetherDefect: '1', scheduleStatus: '1' })
  expect(r.code).toBe(200)
  expect(r.rows.every(x => x.whetherDefect === '1' && x.scheduleStatus === '1')).toBeTruthy()
})
