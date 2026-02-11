import request from '@/utils/request'

// 查询立项审核列表
export function listApproval(query) {
  return request({
    url: '/project/approval/list',
    method: 'get',
    params: query
  })
}

// 查询立项审核详细
export function getApproval(approvalId) {
  return request({
    url: '/project/approval/' + approvalId,
    method: 'get'
  })
}

// 新增立项审核
export function addApproval(data) {
  return request({
    url: '/project/approval',
    method: 'post',
    data: data
  })
}

// 修改立项审核
export function updateApproval(data) {
  return request({
    url: '/project/approval',
    method: 'put',
    data: data
  })
}

// 删除立项审核
export function delApproval(approvalId) {
  return request({
    url: '/project/approval/' + approvalId,
    method: 'delete'
  })
}
