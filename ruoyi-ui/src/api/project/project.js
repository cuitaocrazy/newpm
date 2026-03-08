import request from '@/utils/request'

// 查询项目管理列表
export function listProject(query) {
  return request({
    url: '/project/project/list',
    method: 'get',
    params: query
  })
}

// 查询项目合计（全量，不分页）
export function getProjectSummary(query) {
  return request({
    url: '/project/project/summary',
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

// 检查项目编号是否与已有项目冲突，返回建议编号
// excludeProjectId: 编辑时传入当前项目ID以排除自身，新增时不传
export function checkProjectCode(projectCode, excludeProjectId) {
  return request({
    url: '/project/project/checkCode',
    method: 'get',
    params: { projectCode, excludeProjectId }
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

// 根据项目名称搜索（用于 autocomplete）
export function listProjectByName(query) {
  return request({
    url: '/project/project/listByName',
    method: 'get',
    params: query
  })
}

// 轻量项目搜索（只返回 projectId/projectName/projectCode，支持 revenue 权限）
export function searchProjects(projectName) {
  return request({
    url: '/project/project/search',
    method: 'get',
    params: { projectName }
  })
}

// 根据项目ID查询关联的合同信息
export function getContractByProjectId(projectId) {
  return request({
    url: `/project/project/${projectId}/contract`,
    method: 'get'
  })
}

// 关联合同到项目
export function bindContractToProject(projectId, contractId) {
  return request({
    url: `/project/project/${projectId}/bindContract`,
    method: 'post',
    params: { contractId }
  })
}
