import request from '@/utils/request'

// 查询日报列表（概览）
export function listDailyReport(query) {
  return request({
    url: '/project/dailyReport/list',
    method: 'get',
    params: query
  })
}

// 查询某月全部日报（含明细，工作日报动态用）
export function getMonthlyReports(query) {
  return request({
    url: '/project/dailyReport/monthly',
    method: 'get',
    params: query
  })
}

// 查询日报详情
export function getDailyReport(reportId) {
  return request({
    url: '/project/dailyReport/' + reportId,
    method: 'get'
  })
}

// 查询我的某日日报
export function getMyReport(reportDate) {
  return request({
    url: '/project/dailyReport/my/' + reportDate,
    method: 'get'
  })
}

// 查询当前用户参与的项目列表
export function getMyProjects() {
  return request({
    url: '/project/dailyReport/myProjects',
    method: 'get'
  })
}

// 保存日报（新增或修改）
export function saveDailyReport(data) {
  return request({
    url: '/project/dailyReport',
    method: 'post',
    data: data,
    headers: { repeatSubmit: false }
  })
}

// 删除日报
export function delDailyReport(reportIds) {
  return request({
    url: '/project/dailyReport/' + reportIds,
    method: 'delete'
  })
}

// 项目人天统计（服务端分页）
export function getProjectStats(projectName, pageNum, pageSize) {
  return request({
    url: '/project/dailyReport/projectStats',
    method: 'get',
    params: { projectName, pageNum, pageSize }
  })
}

// 人天补正（更新调整人天并记录日志）
export function correctAdjustWorkload(projectId, data) {
  return request({
    url: `/project/dailyReport/projectStats/${projectId}/correct`,
    method: 'post',
    data
  })
}

// 查询项目补正日志
export function getCorrectLogs(projectId) {
  return request({
    url: `/project/dailyReport/projectStats/${projectId}/correctLog`,
    method: 'get'
  })
}

// 项目名称 autocomplete
export function getProjectNameSuggestions(keyword) {
  return request({
    url: '/project/dailyReport/projectNameSuggestions',
    method: 'get',
    params: { keyword }
  })
}
