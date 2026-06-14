import request from '@/utils/request'

// 列表
export function listNobatchProlist(query: any) {
  return request({ url: '/project/nobatchProlist/list', method: 'get', params: query })
}

// 详情
export function getNobatchProlist(problemId: number | string) {
  return request({ url: `/project/nobatchProlist/${problemId}`, method: 'get' })
}

// 新增
export function addNobatchProlist(data: any) {
  return request({ url: '/project/nobatchProlist', method: 'post', data })
}

// 修改
export function updateNobatchProlist(data: any) {
  return request({ url: '/project/nobatchProlist', method: 'put', data })
}

// 删除
export function delNobatchProlist(ids: number | string | (number | string)[]) {
  return request({ url: `/project/nobatchProlist/${ids}`, method: 'delete' })
}

// 年份→批次下拉
export function getBatchByYear(year: string) {
  return request({ url: '/project/nobatchProlist/batchByYear', method: 'get', params: { year } })
}

// 批次→计划投产日期
export function getTcDate(batchId: number | string) {
  return request({ url: '/project/nobatchProlist/tcDate', method: 'get', params: { batchId } })
}

// 问题单编号查重（problemId 非空时排除自己）
export function checkProblemNo(problemNo: string, problemId?: number | string) {
  return request({ url: '/project/nobatchProlist/checkProblemNo', method: 'get', params: { problemNo, problemId } })
}
