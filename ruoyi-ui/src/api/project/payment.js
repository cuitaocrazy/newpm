import request from '@/utils/request'

// 查询款项管理列表
export function listPayment(query) {
  return request({
    url: '/project/payment/list',
    method: 'get',
    params: query
  })
}

// 查询合同及其付款里程碑列表（用于付款里程碑查询页面）
export function listPaymentWithContracts(query) {
  return request({
    url: '/project/payment/listWithContracts',
    method: 'get',
    params: query
  })
}

// 查询款项管理详细
export function getPayment(paymentId) {
  return request({
    url: '/project/payment/' + paymentId,
    method: 'get'
  })
}

// 新增款项管理
export function addPayment(data) {
  return request({
    url: '/project/payment',
    method: 'post',
    data: data
  })
}

// 修改款项管理
export function updatePayment(data) {
  return request({
    url: '/project/payment',
    method: 'put',
    data: data
  })
}

// 删除款项管理
export function delPayment(paymentId) {
  return request({
    url: '/project/payment/' + paymentId,
    method: 'delete'
  })
}

// 检查附件数量
export function checkAttachments(paymentId) {
  return request({
    url: '/project/payment/checkAttachments/' + paymentId,
    method: 'get'
  })
}

// 统计付款里程碑总金额
export function sumPaymentAmount(query) {
  return request({
    url: '/project/payment/sumPaymentAmount',
    method: 'get',
    params: query
  })
}
