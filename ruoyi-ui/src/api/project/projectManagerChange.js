import request from '@/utils/request'

// 查询项目经理变更列表
export function listProjectManagerChange(query) {
  return request({
    url: '/project/projectManagerChange/list',
    method: 'get',
    params: query
  })
}

// 查询项目经理变更详细
export function getProjectManagerChange(changeId) {
  return request({
    url: '/project/projectManagerChange/' + changeId,
    method: 'get'
  })
}

// 新增项目经理变更
export function addProjectManagerChange(data) {
  return request({
    url: '/project/projectManagerChange',
    method: 'post',
    data: data
  })
}

// 修改项目经理变更
export function updateProjectManagerChange(data) {
  return request({
    url: '/project/projectManagerChange',
    method: 'put',
    data: data
  })
}

// 删除项目经理变更
export function delProjectManagerChange(changeId) {
  return request({
    url: '/project/projectManagerChange/' + changeId,
    method: 'delete'
  })
}

// 项目名称自动补全搜索
export function searchProjects(keyword) {
  return request({
    url: '/project/projectManagerChange/searchProjects',
    method: 'get',
    params: { keyword }
  })
}

// 变更项目经理
export function changeManager(data) {
  return request({
    url: '/project/projectManagerChange/change',
    method: 'post',
    data: data
  })
}

// 批量变更项目经理
export function batchChangeManager(data) {
  return request({
    url: '/project/projectManagerChange/batchChange',
    method: 'post',
    data: data
  })
}

// 获取项目经理变更详情
export function getChangeDetail(projectId) {
  return request({
    url: '/project/projectManagerChange/detail/' + projectId,
    method: 'get'
  })
}
