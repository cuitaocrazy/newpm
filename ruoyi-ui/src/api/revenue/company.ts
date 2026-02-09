import request from '@/utils/request'

// 查询公司收入确认列表
export function listRevenueCompany(query: any) {
  return request({
    url: '/project/project/revenueList',
    method: 'get',
    params: query
  })
}

// 查询公司收入确认详细
export function getRevenueCompany(projectId: number) {
  return request({
    url: '/project/project/revenue/' + projectId,
    method: 'get'
  })
}

// 更新公司收入确认信息
export function updateRevenueConfirm(data: any) {
  return request({
    url: '/project/project/revenueConfirm',
    method: 'put',
    data: data
  })
}

// 导出公司收入确认列表
export function exportRevenueCompany(query: any) {
  return request({
    url: '/project/project/revenueExport',
    method: 'post',
    params: query
  })
}
