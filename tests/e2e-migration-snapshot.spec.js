// 数据迁移(012) 快照显示 E2E —— 验证迁移历史数据 + COALESCE 兜底 + 字典扩展 + 加后缀保留
// 依赖: ry-vue 已灌迁移数据(create_by='yadapm-migrate'), 后端8085, 前端8090
// 运行: E2E_BASE_URL=http://localhost:8090 npx playwright test e2e-migration-snapshot.spec.js --reporter=list
const { test, expect } = require('@playwright/test')
const { setupApi } = require('./helpers/api-client')

let api
test.beforeAll(async () => { api = await setupApi() })
test.afterAll(async () => { await api.dispose() })

test('④ 列表含迁移历史数据(total≥2958)', async () => {
  const r = await api.get('/project/prolistDefect/list', { pageSize: 1 })
  expect(r.code).toBe(200)
  expect(r.total).toBeGreaterThanOrEqual(2958)
})

test('④ 历史快照行: task_id 为空但任务信息(快照列)能显示', async () => {
  const r = await api.get('/project/prolistDefect/list', { pageSize: 300 })
  const snap = r.rows.filter(x => !x.taskId && x.taskName)
  expect(snap.length).toBeGreaterThan(0)            // 有快照行
  const s = snap[0]
  expect(s.taskName).toBeTruthy()                   // COALESCE 兜底: 任务名来自快照
  expect(s.problemNo).toBeTruthy()
})

test('④ FK行: task_id 有值, 任务信息来自 JOIN', async () => {
  const r = await api.get('/project/prolistDefect/list', { pageSize: 300 })
  const fk = r.rows.filter(x => x.taskId)
  expect(fk.length).toBeGreaterThan(0)              // 重叠任务挂上FK
  expect(fk[0].taskName).toBeTruthy()
})

test('④ 扩展状态(7/9)能查询(字典6→16)', async () => {
  const r7 = await api.get('/project/prolistDefect/list', { currentStatus: '7', pageSize: 1 })
  expect(r7.code).toBe(200)
  expect(r7.total).toBeGreaterThan(0)               // 状态7(已转至外部系统处理)有数据
})

test('④ 加后缀保留: 同编号两条都在(原件 + _R2)', async () => {
  const r = await api.get('/project/prolistDefect/list', { problemNo: 'TPR-信用卡微信公众号-账单20230322-002', pageSize: 10 })
  expect(r.code).toBe(200)
  expect(r.total).toBeGreaterThanOrEqual(2)         // 原件 + _R2 都迁入
  const nos = r.rows.map(x => x.problemNo)
  expect(nos.some(n => n.endsWith('_R2'))).toBeTruthy()
})

test('④ 补迁字段: 项目组/计划投产日期/创建人(姓名) 历史行能显示', async () => {
  const r = await api.get('/project/prolistDefect/list', { pageSize: 50 })
  expect(r.rows.filter(x => x.deptName).length).toBeGreaterThan(0)            // 项目组(subtask_team快照)
  expect(r.rows.filter(x => x.planProductionDate).length).toBeGreaterThan(0)  // 计划投产日期(快照)
  const withCreator = r.rows.filter(x => x.createByName)
  expect(withCreator.length).toBeGreaterThan(0)                               // 创建人(老id映射姓名)
  expect(/^\d+$/.test(withCreator[0].createByName)).toBeFalsy()               // 是姓名不是纯数字id
})

test('⑤ 列表含迁移历史 + 批次号快照显示', async () => {
  const r = await api.get('/project/nobatchProlist/list', { pageSize: 5 })
  expect(r.code).toBe(200)
  expect(r.total).toBeGreaterThanOrEqual(299)
  const withBatch = r.rows.find(x => x.batchNo)
  expect(withBatch).toBeTruthy()                    // pro_batch_no 快照经 COALESCE 显示为 batchNo
})

test('③ 旧数据查询含迁移归档(1231)', async () => {
  const r = await api.get('/project/oldVersionOut/list', { pageSize: 1 })
  expect(r.code).toBe(200)
  expect(r.total).toBeGreaterThanOrEqual(1231)
})

test('①② 版本管理含迁移历史(批次+非批次)', async () => {
  const r = await api.get('/project/versionOut/list', { pageSize: 1 })
  expect(r.code).toBe(200)
  expect(r.total).toBeGreaterThan(1000)             // 批次版本历史
})
