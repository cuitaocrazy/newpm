import request from '@/utils/request'

// 查询项目列表（带最新变更信息）
export function listManagerChange(query) {
  return request({
    url: '/project/managerChange/list',
    method: 'get',
    params: query
  })
}

// 查询项目变更历史
export function getProjectChangeHistory(projectId) {
  return request({
    url: '/project/managerChange/history/' + projectId,
    method: 'get'
  })
}

// 单个项目变更经理
export function changeManager(data) {
  return request({
    url: '/project/managerChange/change',
    method: 'post',
    data: data
  })
}

// 批量变更项目经理
export function batchChangeManager(projectIds, newManagerId, changeReason) {
  return request({
    url: '/project/managerChange/batchChange',
    method: 'post',
    params: {
      projectIds: projectIds,
      newManagerId: newManagerId,
      changeReason: changeReason
    }
  })
}

// ========== 以下是原有的变更记录管理接口（保留用于后台管理） ==========

// 查询项目经理变更详细
export function getManagerChange(changeId) {
  return request({
    url: '/project/managerChange/' + changeId,
    method: 'get'
  })
}

// 新增项目经理变更
export function addManagerChange(data) {
  return request({
    url: '/project/managerChange',
    method: 'post',
    data: data
  })
}

// 修改项目经理变更
export function updateManagerChange(data) {
  return request({
    url: '/project/managerChange',
    method: 'put',
    data: data
  })
}

// 删除项目经理变更
export function delManagerChange(changeId) {
  return request({
    url: '/project/managerChange/' + changeId,
    method: 'delete'
  })
}
