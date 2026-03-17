import request from '@/utils/request'

// 白名单列表（管理员）
export function listWhitelist(query) {
  return request({ url: '/project/whitelist/list', method: 'get', params: query })
}

// 添加白名单
export function addWhitelist(data) {
  return request({ url: '/project/whitelist', method: 'post', data })
}

// 移除白名单
export function removeWhitelist(id) {
  return request({ url: '/project/whitelist/' + id, method: 'delete' })
}

// 检查当前用户是否在白名单中
export function checkSelfInWhitelist() {
  return request({ url: '/project/whitelist/checkSelf', method: 'get' })
}
