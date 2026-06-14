import request from '@/utils/request'

// 旧版本归档列表（只读）
export function listOldVersionOut(query: any) {
  return request({ url: '/project/oldVersionOut/list', method: 'get', params: query })
}

// 投产批次号下拉选项
export function getProBatchNoOptions() {
  return request({ url: '/project/oldVersionOut/proBatchNoOptions', method: 'get' })
}

// 子产品下拉选项
export function getProductOptions() {
  return request({ url: '/project/oldVersionOut/productOptions', method: 'get' })
}

// 版本类型下拉选项
export function getVersionTypeOptions() {
  return request({ url: '/project/oldVersionOut/versionTypeOptions', method: 'get' })
}
