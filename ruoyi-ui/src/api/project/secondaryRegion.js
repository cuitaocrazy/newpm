import request from '@/utils/request'

// 查询省级区域列表
export function listSecondaryRegion(query) {
  return request({
    url: '/project/secondaryRegion/list',
    method: 'get',
    params: query
  })
}

// 查询省级区域详细
export function getSecondaryRegion(provinceId) {
  return request({
    url: '/project/secondaryRegion/' + provinceId,
    method: 'get'
  })
}

// 新增省级区域
export function addSecondaryRegion(data) {
  return request({
    url: '/project/secondaryRegion',
    method: 'post',
    data: data
  })
}

// 修改省级区域
export function updateSecondaryRegion(data) {
  return request({
    url: '/project/secondaryRegion',
    method: 'put',
    data: data
  })
}

// 删除省级区域
export function delSecondaryRegion(provinceId) {
  return request({
    url: '/project/secondaryRegion/' + provinceId,
    method: 'delete'
  })
}
