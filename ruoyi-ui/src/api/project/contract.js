import request from '@/utils/request'

// 查询合同管理列表
export function listContract(query) {
  return request({
    url: '/project/contract/list',
    method: 'get',
    params: query
  })
}

// 查询合同管理详细
export function getContract(contractId) {
  return request({
    url: '/project/contract/' + contractId,
    method: 'get'
  })
}

// 新增合同管理
export function addContract(data) {
  return request({
    url: '/project/contract',
    method: 'post',
    data: data
  })
}

// 修改合同管理
export function updateContract(data) {
  return request({
    url: '/project/contract',
    method: 'put',
    data: data
  })
}

// 删除合同管理
export function delContract(contractId) {
  return request({
    url: '/project/contract/' + contractId,
    method: 'delete'
  })
}

// 搜索合同
export function searchContracts(query) {
  return request({
    url: '/project/contract/search',
    method: 'get',
    params: query
  })
}

// 检查合同名称是否唯一
export function checkContractNameUnique(contractName, contractId) {
  return request({
    url: '/project/contract/checkContractNameUnique',
    method: 'get',
    params: {
      contractName,
      contractId
    }
  })
}
