import request from '@/utils/request'

// 查询客户管理列表
export function listCustomer(query) {
  return request({
    url: '/project/customer/list',
    method: 'get',
    params: query
  })
}

// 查询客户管理详细
export function getCustomer(customerId) {
  return request({
    url: '/project/customer/' + customerId,
    method: 'get'
  })
}

// 新增客户管理
export function addCustomer(data) {
  return request({
    url: '/project/customer',
    method: 'post',
    data: data
  })
}

// 修改客户管理
export function updateCustomer(data) {
  return request({
    url: '/project/customer',
    method: 'put',
    data: data
  })
}

// 删除客户管理
export function delCustomer(customerId) {
  return request({
    url: '/project/customer/' + customerId,
    method: 'delete'
  })
}

// 检查客户简称是否唯一
export function checkCustomerSimpleNameUnique(customerSimpleName, customerId) {
  return request({
    url: '/project/customer/checkSimpleNameUnique',
    method: 'get',
    params: { customerSimpleName, customerId }
  })
}
