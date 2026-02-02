import request from '@/utils/request'

// 查询附件管理列表
export function listAttachment(query) {
  return request({
    url: '/project/attachment/list',
    method: 'get',
    params: query
  })
}

// 上传附件
export function uploadAttachment(data) {
  return request({
    url: '/project/attachment/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 下载附件
export function downloadAttachment(attachmentId) {
  return request({
    url: '/project/attachment/download/' + attachmentId,
    method: 'get',
    responseType: 'blob'
  })
}

// 删除附件
export function delAttachment(attachmentId) {
  return request({
    url: '/project/attachment/' + attachmentId,
    method: 'delete'
  })
}

// 查询附件操作日志
export function getAttachmentLog(attachmentId) {
  return request({
    url: '/project/attachment/log/' + attachmentId,
    method: 'get'
  })
}
