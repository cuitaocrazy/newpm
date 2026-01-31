import request from '@/utils/request'

// 查询客户联系人列表
export function listContact(query) {
  return request({
    url: '/project/contact/list',
    method: 'get',
    params: query
  })
}

// 查询客户联系人详细
export function getContact(contactId) {
  return request({
    url: '/project/contact/' + contactId,
    method: 'get'
  })
}

// 新增客户联系人
export function addContact(data) {
  return request({
    url: '/project/contact',
    method: 'post',
    data: data
  })
}

// 修改客户联系人
export function updateContact(data) {
  return request({
    url: '/project/contact',
    method: 'put',
    data: data
  })
}

// 删除客户联系人
export function delContact(contactId) {
  return request({
    url: '/project/contact/' + contactId,
    method: 'delete'
  })
}
