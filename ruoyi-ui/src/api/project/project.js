import request from '@/utils/request'

// 查询项目管理列表
export function listProject(query) {
  return request({
    url: '/project/project/list',
    method: 'get',
    params: query
  })
}

// 查询项目管理详细
export function getProject(projectId) {
  return request({
    url: '/project/project/' + projectId,
    method: 'get'
  })
}

// 新增项目管理
export function addProject(data) {
  return request({
    url: '/project/project',
    method: 'post',
    data: data
  })
}

// 修改项目管理
export function updateProject(data) {
  return request({
    url: '/project/project',
    method: 'put',
    data: data
  })
}

// 删除项目管理
export function delProject(projectId) {
  return request({
    url: '/project/project/' + projectId,
    method: 'delete'
  })
}

// 获取用户列表（按岗位过滤）
export function getUsersByPost(postCode) {
  return request({
    url: '/project/project/users',
    method: 'get',
    params: { postCode }
  })
}

// 获取二级区域列表（根据一级区域）
export function getSecondaryRegions(regionDictValue) {
  return request({
    url: '/project/project/secondaryRegions',
    method: 'get',
    params: { regionDictValue }
  })
}

// 获取客户列表（支持搜索）
export function getCustomers(customerSimpleName) {
  return request({
    url: '/project/project/customers',
    method: 'get',
    params: { customerSimpleName }
  })
}

// 获取客户联系人列表（根据客户ID）
export function getCustomerContacts(customerId) {
  return request({
    url: '/project/project/customerContacts',
    method: 'get',
    params: { customerId }
  })
}

// 获取部门树（三级及以下机构）
export function getDeptTree() {
  return request({
    url: '/project/project/deptTree',
    method: 'get'
  })
}

// 生成项目编号
export function generateProjectCode(data) {
  return request({
    url: '/project/project/generateCode',
    method: 'post',
    data: data
  })
}

// 根据部门查询项目列表（用于合同关联，可排除已关联的项目）
export function listProjectByDept(deptId, excludeContractId) {
  return request({
    url: '/project/project/listByDept',
    method: 'get',
    params: {
      deptId: deptId,
      excludeContractId: excludeContractId
    }
  })
}
