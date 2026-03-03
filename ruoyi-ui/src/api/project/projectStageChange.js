import request from '@/utils/request'

export function listProjectStageChange(query) {
  return request({ url: '/project/projectStageChange/list', method: 'get', params: query })
}

export function historyByProject(projectId) {
  return request({ url: '/project/projectStageChange/history/' + projectId, method: 'get' })
}

export function getProjectStageChange(changeId) {
  return request({ url: '/project/projectStageChange/' + changeId, method: 'get' })
}

export function addProjectStageChange(data) {
  return request({ url: '/project/projectStageChange', method: 'post', data })
}

export function batchChangeStage(data) {
  return request({ url: '/project/projectStageChange/batchChange', method: 'post', data })
}

export function updateProjectStageChange(data) {
  return request({ url: '/project/projectStageChange', method: 'put', data })
}

export function delProjectStageChange(changeId) {
  return request({ url: '/project/projectStageChange/' + changeId, method: 'delete' })
}
