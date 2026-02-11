import request from '@/utils/request'

// 查询收入确认列表
export function listRevenue(query) {
  return request({
    url: '/project/project/revenue/list',
    method: 'get',
    params: query
  })
}

// 获取收入确认详情
export function getRevenue(projectId) {
  return request({
    url: '/project/project/revenue/' + projectId,
    method: 'get'
  })
}

// 保存收入确认信息
export function updateRevenue(data) {
  return request({
    url: '/project/project/revenue',
    method: 'put',
    data: data
  })
}

// 导出收入确认
export function exportRevenue(query) {
  return request({
    url: '/project/project/revenue/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  })
}
