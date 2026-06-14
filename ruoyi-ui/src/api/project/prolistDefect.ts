import request from '@/utils/request'

// 列表
export function listProlistDefect(query: any) {
  return request({ url: '/project/prolistDefect/list', method: 'get', params: query })
}

// 详情
export function getProlistDefect(problemId: number | string) {
  return request({ url: `/project/prolistDefect/${problemId}`, method: 'get' })
}

// 新增
export function addProlistDefect(data: any) {
  return request({ url: '/project/prolistDefect', method: 'post', data })
}

// 修改
export function updateProlistDefect(data: any) {
  return request({ url: '/project/prolistDefect', method: 'put', data })
}

// 删除
export function delProlistDefect(ids: number | string | (number | string)[]) {
  return request({ url: `/project/prolistDefect/${ids}`, method: 'delete' })
}

// 年份→批次下拉
export function getBatchByYear(year: string) {
  return request({ url: '/project/prolistDefect/batchByYear', method: 'get', params: { year } })
}

// 批次→计划投产日期
export function getTcDate(batchId: number | string) {
  return request({ url: '/project/prolistDefect/tcDate', method: 'get', params: { batchId } })
}

// 年份+批次+部门→任务号下拉
export function getTaskOptions(productionYear: string, batchId: number | string, deptId?: number | string) {
  return request({ url: '/project/prolistDefect/taskOptions', method: 'get', params: { productionYear, batchId, deptId } })
}

// 任务回显
export function getTaskInfo(taskId: number | string) {
  return request({ url: '/project/prolistDefect/taskInfo', method: 'get', params: { taskId } })
}

// 问题单编号查重（problemId 非空时排除自己）
export function checkProblemNo(problemNo: string, problemId?: number | string) {
  return request({ url: '/project/prolistDefect/checkProblemNo', method: 'get', params: { problemNo, problemId } })
}
