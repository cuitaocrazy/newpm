import request from '@/utils/request'

// 批次版本列表
export function listVersionOut(query: any) {
  return request({ url: '/project/versionOut/list', method: 'get', params: query })
}

// 批次版本详情
export function getVersionOut(id: number | string) {
  return request({ url: `/project/versionOut/${id}`, method: 'get' })
}

// 新增批次版本
export function addVersionOut(data: any) {
  return request({ url: '/project/versionOut', method: 'post', data })
}

// 修改批次版本
export function updateVersionOut(data: any) {
  return request({ url: '/project/versionOut', method: 'put', data })
}

// 删除批次版本
export function delVersionOut(ids: number | string | (number | string)[]) {
  return request({ url: `/project/versionOut/${ids}`, method: 'delete' })
}

// 实时生成出入库版本号
export function generateOutLibVersion(data: any, params?: any) {
  return request({ url: '/project/versionOut/generateOutLibVersion', method: 'post', data, params })
}

// 年份→批次列表
export function getBatchByYear(year: string) {
  return request({ url: '/project/versionOut/batchByYear', method: 'get', params: { year } })
}

// 产品→子系统列表
export function getSysNameByProduct(product: string) {
  return request({ url: '/project/versionOut/sysNameByProduct', method: 'get', params: { product } })
}

// 子系统+版本类型→升级包初级版本号候选
export function getOutVersionOptions(sysName: string, versionType: string) {
  return request({ url: '/project/versionOut/outVersionOptions', method: 'get', params: { sysName, versionType } })
}

// 批次→投产日期
export function getVersionPDate(batchId: number | string) {
  return request({ url: '/project/versionOut/versionPDate', method: 'get', params: { batchId } })
}

// 软件中心任务号→任务回显
export function getTaskInfo(taskNo: string) {
  return request({ url: '/project/versionOut/taskInfo', method: 'get', params: { taskNo } })
}

// 任务下拉选项：按 年份+批次+产品
export function getTaskOptions(productionYear: string, batchId: number | string, product: string) {
  return request({ url: '/project/versionOut/taskOptions', method: 'get', params: { productionYear, batchId, product } })
}
