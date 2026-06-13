import request from '@/utils/request'

// 非批次版本列表
export function listVersionOutManual(query: any) {
  return request({ url: '/project/versionOutManual/list', method: 'get', params: query })
}

// 详情
export function getVersionOutManual(id: number | string) {
  return request({ url: `/project/versionOutManual/${id}`, method: 'get' })
}

// 新增
export function addVersionOutManual(data: any) {
  return request({ url: '/project/versionOutManual', method: 'post', data })
}

// 修改
export function updateVersionOutManual(data: any) {
  return request({ url: '/project/versionOutManual', method: 'put', data })
}

// 删除
export function delVersionOutManual(ids: number | string | (number | string)[]) {
  return request({ url: `/project/versionOutManual/${ids}`, method: 'delete' })
}

// 实时生成出入库版本号
export function generateOutLibVersion(data: any, params?: any) {
  return request({ url: '/project/versionOutManual/generateOutLibVersion', method: 'post', data, params })
}

// 年份→批次列表
export function getBatchByYear(year: string) {
  return request({ url: '/project/versionOutManual/batchByYear', method: 'get', params: { year } })
}

// 产品→子系统列表
export function getSysNameByProduct(product: string) {
  return request({ url: '/project/versionOutManual/sysNameByProduct', method: 'get', params: { product } })
}

// 子系统+版本类型→升级包初级版本号候选
export function getOutVersionOptions(sysName: string, versionType: string) {
  return request({ url: '/project/versionOutManual/outVersionOptions', method: 'get', params: { sysName, versionType } })
}

// 批次→投产日期
export function getVersionPDate(batchId: number | string) {
  return request({ url: '/project/versionOutManual/versionPDate', method: 'get', params: { batchId } })
}
