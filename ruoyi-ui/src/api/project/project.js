import request from '@/utils/request'

// 查询项目管理列表
export function listProject(query) {
  return request({
    url: '/project/project/list',
    method: 'get',
    params: query
  })
}

// 根据部门查询项目列表（用于合同关联项目选择）
export function listProjectByDept(deptId, excludeContractId) {
  return request({
    url: '/project/project/listByDept',
    method: 'get',
    params: {
      deptId,
      excludeContractId
    }
  })
}

// 查询项目管理详细
export function getProject(projectId) {
  return request({
    url: '/project/project/' + projectId,
    method: 'get'
  })
}

// 新增项目管理
export function addProject(data) {
  return request({
    url: '/project/project',
    method: 'post',
    data: data
  })
}

// 修改项目管理
export function updateProject(data) {
  return request({
    url: '/project/project',
    method: 'put',
    data: data
  })
}

// 删除项目管理
export function delProject(projectId) {
  return request({
    url: '/project/project/' + projectId,
    method: 'delete'
  })
}
