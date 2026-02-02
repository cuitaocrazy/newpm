import request from '@/utils/request'

// 查询项目管理列表
export function listProject(query) {
  return request({
    url: '/project/project/list',
    method: 'get',
    params: query
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

// 获取项目名称列表（用于智能提示）
export function getProjectNameList(projectName) {
  return request({
    url: '/project/project/nameList',
    method: 'get',
    params: { projectName }
  })
}

// 获取项目编号列表（用于智能提示）
export function getProjectCodeList(projectCode) {
  return request({
    url: '/project/project/codeList',
    method: 'get',
    params: { projectCode }
  })
}

// 获取金额汇总
export function getProjectSummary(query) {
  return request({
    url: '/project/project/summary',
    method: 'get',
    params: query
  })
}
