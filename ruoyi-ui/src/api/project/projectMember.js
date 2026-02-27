import request from '@/utils/request'

// 查询项目人员列表
export function listProjectMember(query) {
  return request({
    url: '/project/member/list',
    method: 'get',
    params: query
  })
}

// 查询项目成员详情
export function getProjectMemberDetail(projectId) {
  return request({
    url: '/project/member/' + projectId,
    method: 'get'
  })
}

// 更新项目成员
export function updateProjectMembers(data) {
  return request({
    url: '/project/member',
    method: 'put',
    data: data
  })
}
