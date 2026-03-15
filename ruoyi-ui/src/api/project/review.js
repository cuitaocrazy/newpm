import request from '@/utils/request'

// 查询立项审核列表
export function listReview(query) {
  return request({
    url: '/project/review/list',
    method: 'get',
    params: query
  })
}

// 查询立项审核列表预算合计
export function getReviewSummary(query) {
  return request({
    url: '/project/review/summary',
    method: 'get',
    params: query
  })
}

// 查询项目详细信息
export function getReview(projectId) {
  return request({
    url: '/project/review/' + projectId,
    method: 'get'
  })
}

// 审核项目
export function approveProject(data) {
  return request({
    url: '/project/review/approve',
    method: 'post',
    data: data
  })
}

// 退回审核通过的项目（改为退回待审核）
export function rollbackProject(data) {
  return request({
    url: '/project/review/rollback',
    method: 'post',
    data: data
  })
}

// 查询审核历史（project/project/index.vue 使用）
export function getApprovalHistory(projectId) {
  return request({
    url: '/project/approval/history/' + projectId,
    method: 'get'
  })
}
