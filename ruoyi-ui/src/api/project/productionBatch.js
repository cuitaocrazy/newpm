import request from '@/utils/request'

// 查询投产批次管理列表
export function listProductionBatch(query) {
  return request({
    url: '/project/productionBatch/list',
    method: 'get',
    params: query
  })
}

// 查询投产批次管理详细
export function getProductionBatch(batchId) {
  return request({
    url: '/project/productionBatch/' + batchId,
    method: 'get'
  })
}

// 新增投产批次管理
export function addProductionBatch(data) {
  return request({
    url: '/project/productionBatch',
    method: 'post',
    data: data
  })
}

// 修改投产批次管理
export function updateProductionBatch(data) {
  return request({
    url: '/project/productionBatch',
    method: 'put',
    data: data
  })
}

// 删除投产批次管理
export function delProductionBatch(batchId) {
  return request({
    url: '/project/productionBatch/' + batchId,
    method: 'delete'
  })
}

// 获取批次号下拉选项
export function getBatchNoOptions() {
  return request({
    url: '/project/productionBatch/batchNoOptions',
    method: 'get'
  })
}
