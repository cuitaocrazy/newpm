// 批次任务问题单及缺陷 (010) E2E —— 覆盖 Controller 全端点 + 联动 + 查重 + 软删坑13 + 派生算法
// 运行：E2E_BASE_URL=http://localhost:8090 npx playwright test e2e-prolist-defect-crud.spec.js --reporter=list
const { test, expect } = require('@playwright/test')
const { setupApi } = require('./helpers/api-client')

const BASE = '/project/prolistDefect'
// 测试数据依赖：pm_task 中 任务239(年2026/批1/部门201)
const TASK_ID = 239
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

function buildPayload(problemNo, overrides = {}) {
  return {
    taskId: TASK_ID, productionYear: YEAR, batchId: BATCH_ID, deptId: DEPT_ID,
    problemNo, problemLevel: '1', currentStatus: '2',
    submitDate: '2026-06-01', settleDate: '2026-06-05', verifyDate: '2026-06-06',
    whetherDefect: '1', whetherOvertime: '0', whetherProRecurrence: '0',
    whetherAttRequired: '1', whetherUpdateVersion: '0',
    defectDesc: 'E2E缺陷', remarks: 'E2E', ...overrides
  }
}

test('联动 batchByYear: 年份→批次', async () => {
  const r = await api.get(`${BASE}/batchByYear`, { year: YEAR })
  expect(r.code).toBe(200)
  expect(Array.isArray(r.data)).toBeTruthy()
  expect(r.data.some(b => b.batchId === BATCH_ID)).toBeTruthy()
})

test('联动 tcDate: 批次→计划投产日期(进data非msg)', async () => {
  const r = await api.get(`${BASE}/tcDate`, { batchId: BATCH_ID })
  expect(r.code).toBe(200)
  expect(r.data).toBeTruthy() // 修复"success(String)塞msg"坑后，日期在data
})

test('联动 taskOptions: 年份+批次+部门→任务号', async () => {
  const r = await api.get(`${BASE}/taskOptions`, { productionYear: YEAR, batchId: BATCH_ID, deptId: DEPT_ID })
  expect(r.code).toBe(200)
  expect(r.data.some(t => t.taskId === TASK_ID)).toBeTruthy()
})

test('联动 taskInfo: 任务回显', async () => {
  const r = await api.get(`${BASE}/taskInfo`, { taskId: TASK_ID })
  expect(r.code).toBe(200)
  expect(r.data.taskName).toBeTruthy()
  expect(r.data.product).toBeTruthy()
})

test('查重 checkProblemNo: 不存在→true', async () => {
  const r = await api.get(`${BASE}/checkProblemNo`, { problemNo: 'E2E-NOTEXIST-XYZ' })
  expect(r.code).toBe(200)
  expect(r.data).toBe(true)
})

test('新增 + 列表JOIN字段 + 派生算法', async () => {
  const add = await api.post(BASE, buildPayload('E2E-PD-001'))
  expect(add.code).toBe(200)
  const list = await api.get(`${BASE}/list`, { problemNo: 'E2E-PD-001' })
  expect(list.total).toBeGreaterThanOrEqual(1)
  const row = list.rows[0]
  created.push(row.problemId)
  expect(row.taskName).toBeTruthy()      // JOIN pm_task
  expect(row.deptName).toBeTruthy()      // JOIN sys_dept
  expect(row.createByName).toBeTruthy()  // JOIN sys_user (COLLATE)
  expect(row.solutionTimeOverOneDay).toBe('1') // 6-05减6-01=4天>1
})

test('查重 checkProblemNo: 已存在→false', async () => {
  const r = await api.get(`${BASE}/checkProblemNo`, { problemNo: 'E2E-PD-001' })
  expect(r.data).toBe(false)
})

test('新增重复编号→拒绝(500)', async () => {
  const r = await api.post(BASE, buildPayload('E2E-PD-001'))
  expect(r.code).toBe(500)
})

test('详情', async () => {
  const id = created[0]
  const r = await api.get(`${BASE}/${id}`)
  expect(r.code).toBe(200)
  expect(r.data.problemNo).toBe('E2E-PD-001')
})

test('编辑: 同编号查重排除自己 + settle空派生用当天', async () => {
  const id = created[0]
  const r = await api.put(BASE, buildPayload('E2E-PD-001', { problemId: id, settleDate: null, currentStatus: '5' }))
  expect(r.code).toBe(200)
  const detail = await api.get(`${BASE}/${id}`)
  expect(detail.data.currentStatus).toBe('5')
  expect(detail.data.settleDate).toBeFalsy()
  expect(detail.data.solutionTimeOverOneDay).toBe('1') // 提交2026-06-01距今>1天
})

test('checkProblemNo 编辑排除自己→true', async () => {
  const id = created[0]
  const r = await api.get(`${BASE}/checkProblemNo`, { problemNo: 'E2E-PD-001', problemId: id })
  expect(r.data).toBe(true)
})

test('软删除 + 删后重建同号(坑13)', async () => {
  const id = created[0]
  const del = await api.del(`${BASE}/${id}`)
  expect(del.code).toBe(200)
  created.length = 0
  // 删后列表不含
  const list = await api.get(`${BASE}/list`, { problemNo: 'E2E-PD-001' })
  expect(list.rows.every(r => r.problemNo !== 'E2E-PD-001')).toBeTruthy()
  // 重建同号应成功（软删行 problem_no 已加 _DEL_ 后缀腾位）
  const re = await api.post(BASE, buildPayload('E2E-PD-001'))
  expect(re.code).toBe(200)
  const relist = await api.get(`${BASE}/list`, { problemNo: 'E2E-PD-001' })
  created.push(relist.rows[0].problemId)
})

test('多维查询过滤', async () => {
  const r = await api.get(`${BASE}/list`, { productionYear: YEAR, batchId: BATCH_ID, whetherDefect: '1', deptId: DEPT_ID })
  expect(r.code).toBe(200)
  expect(r.rows.every(x => x.whetherDefect === '1')).toBeTruthy()
})
