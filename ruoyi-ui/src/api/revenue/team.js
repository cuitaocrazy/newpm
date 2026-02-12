import request from '@/utils/request'

// 查询团队收入确认列表（项目列表）
export function listTeamRevenue(query) {
  return request({
    url: '/revenue/team/list',
    method: 'get',
    params: query
  })
}

// 查询团队收入确认详细（项目详情 + 团队确认明细列表）
export function getTeamRevenue(projectId) {
  return request({
    url: '/revenue/team/' + projectId,
    method: 'get'
  })
}

// 根据项目ID获取项目基本信息（用于新增页面自动带出字段）
export function getProjectInfo(projectId) {
  return request({
    url: '/revenue/team/project/' + projectId,
    method: 'get'
  })
}

// 新增团队收入确认（批量保存明细）
export function addTeamRevenue(data) {
  return request({
    url: '/revenue/team',
    method: 'post',
    data: data
  })
}

// 修改团队收入确认（批量更新明细）
export function updateTeamRevenue(data) {
  return request({
    url: '/revenue/team',
    method: 'put',
    data: data
  })
}

// 删除团队收入确认明细
export function delTeamRevenue(teamConfirmIds) {
  return request({
    url: '/revenue/team/' + teamConfirmIds,
    method: 'delete'
  })
}

// 导出团队收入确认
export function exportTeamRevenue(query) {
  return request({
    url: '/revenue/team/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  })
}
