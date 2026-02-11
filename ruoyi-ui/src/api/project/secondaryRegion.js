import request from '@/utils/request'

// 查询二级区域列表
export function listSecondaryRegion(query) {
  return request({
    url: '/project/secondaryRegion/list',
    method: 'get',
    params: query
  })
}

// 查询二级区域详细
export function getSecondaryRegion(regionId) {
  return request({
    url: '/project/secondaryRegion/' + regionId,
    method: 'get'
  })
}

// 新增二级区域
export function addSecondaryRegion(data) {
  return request({
    url: '/project/secondaryRegion',
    method: 'post',
    data: data
  })
}

// 修改二级区域
export function updateSecondaryRegion(data) {
  return request({
    url: '/project/secondaryRegion',
    method: 'put',
    data: data
  })
}

// 删除二级区域
export function delSecondaryRegion(regionId) {
  return request({
    url: '/project/secondaryRegion/' + regionId,
    method: 'delete'
  })
}
