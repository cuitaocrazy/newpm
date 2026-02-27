import request from '@/utils/request'

// 按年份查询工作日历
export function getWorkCalendarByYear(year) {
  return request({
    url: '/project/workCalendar/year/' + year,
    method: 'get'
  })
}

// 查询列表（分页）
export function listWorkCalendar(query) {
  return request({
    url: '/project/workCalendar/list',
    method: 'get',
    params: query
  })
}

// 查询详情
export function getWorkCalendar(id) {
  return request({
    url: '/project/workCalendar/' + id,
    method: 'get'
  })
}

// 新增
export function addWorkCalendar(data) {
  return request({
    url: '/project/workCalendar',
    method: 'post',
    data: data
  })
}

// 修改
export function updateWorkCalendar(data) {
  return request({
    url: '/project/workCalendar',
    method: 'put',
    data: data
  })
}

// 删除
export function delWorkCalendar(ids) {
  return request({
    url: '/project/workCalendar/' + ids,
    method: 'delete'
  })
}
