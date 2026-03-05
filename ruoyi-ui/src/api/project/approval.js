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

// 审核项目
export function approveProject(data) {
  return request({
    url: '/project/approval/approve',
    method: 'post',
    data: data
  })
}

// 查询审核历史
export function getApprovalHistory(projectId) {
  return request({
    url: '/project/approval/history/' + projectId,
    method: 'get'
  })
}

// 立项审核专用项目列表
export function getApprovalProjectList(query) {
  return request({
    url: '/project/approval/projectList',
    method: 'get',
    params: query
  })
}

// 立项审核项目预算合计
export function getApprovalProjectSummary(query) {
  return request({
    url: '/project/approval/projectSummary',
    method: 'get',
    params: query
  })
}

// 退回审核通过的项目
export function rollbackProject(data) {
  return request({
    url: '/project/approval/rollback',
    method: 'post',
    data: data
  })
}
