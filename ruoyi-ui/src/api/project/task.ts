import request from '@/utils/request'

export function listTask(query) {
  return request({ url: '/project/task/list', method: 'get', params: query })
}

export function getTask(taskId) {
  return request({ url: `/project/task/${taskId}`, method: 'get' })
}

export function addTask(data) {
  return request({ url: '/project/task', method: 'post', data })
}

export function updateTask(data) {
  return request({ url: '/project/task', method: 'put', data })
}

export function delTask(taskIds) {
  return request({ url: `/project/task/${taskIds}`, method: 'delete' })
}

export function getTaskOptions(projectId) {
  return request({ url: '/project/task/options', method: 'get', params: { projectId } })
}

export function getProjectsHasTasks(projectIds) {
  return request({ url: '/project/task/projectsHasTasks', method: 'get', params: { projectIds } })
}

export function searchTaskCode(taskCode) {
  return request({ url: '/project/task/searchTaskCode', method: 'get', params: { taskCode } })
}

export function searchTaskName(taskName) {
  return request({ url: '/project/task/searchTaskName', method: 'get', params: { taskName } })
}
