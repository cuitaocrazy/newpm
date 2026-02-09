import request from '@/utils/request'

// 查询待审核项目列表
export function listReview(query) {
  return request({
    url: '/project/review/list',
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
