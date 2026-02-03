<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">附件管理</span>
          <el-button style="float: right; padding: 3px 0" type="text" icon="Back" @click="handleBack">返回</el-button>
        </div>
      </template>

      <!-- 合同基本信息 -->
      <el-divider content-position="left">
        <span style="font-size: 16px; font-weight: bold; color: #409EFF;">合同基本信息</span>
      </el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="合同名称" :span="2" label-class-name="label-bold">
          {{ contractInfo.contractName }}
        </el-descriptions-item>
        <el-descriptions-item label="合同编号" label-class-name="label-bold">
          {{ contractInfo.contractCode }}
        </el-descriptions-item>
        <el-descriptions-item label="客户名称" label-class-name="label-bold">
          {{ getCustomerName(contractInfo.customerId) }}
        </el-descriptions-item>
        <el-descriptions-item label="合同类型" label-class-name="label-bold">
          <dict-tag :options="sys_htlx" :value="contractInfo.contractType"/>
        </el-descriptions-item>
        <el-descriptions-item label="合同金额(元)" label-class-name="label-bold">
          <span style="color: #409EFF; font-weight: bold;">{{ formatAmount(contractInfo.contractAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="合同签订日期" label-class-name="label-bold">
          {{ parseTime(contractInfo.contractSignDate, '{y}-{m}-{d}') }}
        </el-descriptions-item>
        <el-descriptions-item label="所属部门" label-class-name="label-bold">
          {{ getDeptName(contractInfo.deptId) }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 附件上传表单 -->
      <el-divider content-position="left">
        <span style="font-size: 16px; font-weight: bold; color: #409EFF;">上传附件</span>
      </el-divider>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="文档类型" prop="documentType">
              <el-select v-model="uploadForm.documentType" placeholder="请选择文档类型" style="width: 100%">
                <el-option
                  v-for="dict in sys_wdlx"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="选择文件" prop="file">
              <el-upload
                ref="uploadRef"
                :auto-upload="false"
                :limit="1"
                :on-change="handleFileChange"
                :on-exceed="handleExceed"
                :before-upload="beforeUpload"
                :file-list="fileList"
                accept=".doc,.docx,.xls,.xlsx,.pdf,.csv,.png,.jpg,.txt,.7z,.zip,.gz"
              >
                <template #trigger>
                  <el-button type="primary" icon="Upload">选择文件</el-button>
                </template>
                <template #tip>
                  <div class="el-upload__tip">
                    每次只能选择一个文件，支持格式：doc、docx、xls、xlsx、pdf、csv、png、jpg、txt、7z、zip、gz，单个文件不超过50MB
                  </div>
                </template>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="文件说明" prop="fileDescription">
              <el-input
                v-model="uploadForm.fileDescription"
                type="textarea"
                :rows="4"
                placeholder="请输入文件说明"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24" style="text-align: center;">
            <el-button type="primary" @click="submitUpload" :loading="uploading">上传</el-button>
            <el-button @click="resetUploadForm">重置</el-button>
            <el-button type="info" icon="View" @click="handleViewAllLogs">日志</el-button>
          </el-col>
        </el-row>
      </el-form>

      <!-- 附件列表 -->
      <el-divider content-position="left">
        <span style="font-size: 16px; font-weight: bold; color: #409EFF;">附件列表</span>
      </el-divider>
      <el-table :data="attachmentList" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="fileName" label="文件名称" min-width="200" />
        <el-table-column label="文档类型" width="120" align="center">
          <template #default="scope">
            <dict-tag :options="sys_wdlx" :value="scope.row.documentType"/>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="文件大小" width="120" align="center">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="fileDescription" label="文件说明" min-width="150" show-overflow-tooltip />
        <el-table-column prop="uploadBy" label="上传人" width="120" align="center" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" align="center">
          <template #default="scope">
            {{ parseTime(scope.row.uploadTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="scope">
            <el-button type="text" icon="Download" @click="handleDownload(scope.row)">下载</el-button>
            <el-button type="text" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:attachment:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 操作日志对话框 -->
      <el-dialog v-model="logDialogVisible" title="附件操作日志" width="800px" append-to-body>
        <el-table :data="logList" border>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="operationType" label="操作类型" width="100" align="center" />
          <el-table-column prop="operationBy" label="操作人" width="120" align="center" />
          <el-table-column prop="operationTime" label="操作时间" width="180" align="center">
            <template #default="scope">
              {{ parseTime(scope.row.operationTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
            </template>
          </el-table-column>
          <el-table-column prop="operationRemark" label="操作说明" min-width="150" show-overflow-tooltip />
        </el-table>
        <template #footer>
          <el-button @click="logDialogVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup name="ContractAttachment">
import { getContract } from "@/api/project/contract"
import { listAttachment, uploadAttachment, downloadAttachment, delAttachment, getAttachmentLog } from "@/api/project/attachment"
import { deptTreeSelect } from "@/api/system/user"
import { listCustomer } from "@/api/project/customer"

const { proxy } = getCurrentInstance()
const { sys_htlx, sys_wdlx } = proxy.useDict('sys_htlx', 'sys_wdlx')
const route = useRoute()
const router = useRouter()

const contractInfo = ref({})
const deptOptions = ref([])
const customerOptions = ref([])
const attachmentList = ref([])
const fileList = ref([])
const uploading = ref(false)
const logDialogVisible = ref(false)
const logList = ref([])

const uploadForm = ref({
  documentType: null,
  file: null,
  fileDescription: ''
})

const uploadRules = {
  documentType: [
    { required: true, message: "文档类型不能为空", trigger: "change" }
  ],
  file: [
    { required: true, message: "请选择文件", trigger: "change" }
  ]
}

/** 查询部门下拉树结构 */
function getDeptTree() {
  deptTreeSelect().then(response => {
    deptOptions.value = response.data
  })
}

/** 查询客户列表 */
function getCustomerList() {
  listCustomer().then(response => {
    customerOptions.value = response.rows
  })
}

/** 根据客户ID获取客户名称 */
function getCustomerName(customerId) {
  if (!customerId) return ''
  const customer = customerOptions.value.find(item => item.customerId === customerId)
  return customer ? customer.customerSimpleName : customerId
}

/** 根据部门ID获取部门名称 */
function getDeptName(deptId) {
  if (!deptId) return ''
  const findDept = (depts, id) => {
    if (!depts || !Array.isArray(depts)) return null
    for (const dept of depts) {
      if (dept.id === id) return dept.label
      if (dept.children && dept.children.length > 0) {
        const found = findDept(dept.children, id)
        if (found) return found
      }
    }
    return null
  }
  const name = findDept(deptOptions.value, deptId)
  return name || deptId
}

/** 格式化金额 */
function formatAmount(amount) {
  if (amount === null || amount === undefined || amount === '') return ''
  const num = parseFloat(amount)
  if (isNaN(num)) return amount
  return num.toFixed(2)
}

/** 格式化文件大小 */
function formatFileSize(size) {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let index = 0
  let fileSize = parseFloat(size)
  while (fileSize >= 1024 && index < units.length - 1) {
    fileSize /= 1024
    index++
  }
  return fileSize.toFixed(2) + ' ' + units[index]
}

/** 文件选择变化 */
function handleFileChange(file, files) {
  uploadForm.value.file = file.raw
  fileList.value = files
}

/** 文件超出限制 */
function handleExceed() {
  proxy.$modal.msgWarning('每次只能上传一个文件')
}

/** 上传前校验 */
function beforeUpload(file) {
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    proxy.$modal.msgError('上传文件大小不能超过 50MB!')
    return false
  }
  const allowedTypes = ['.doc', '.docx', '.xls', '.xlsx', '.pdf', '.csv', '.png', '.jpg', '.txt', '.7z', '.zip', '.gz']
  const fileName = file.name.toLowerCase()
  const isAllowedType = allowedTypes.some(type => fileName.endsWith(type))
  if (!isAllowedType) {
    proxy.$modal.msgError('不支持的文件格式!')
    return false
  }
  return true
}

/** 提交上传 */
function submitUpload() {
  proxy.$refs.uploadFormRef.validate(valid => {
    if (valid) {
      if (!uploadForm.value.file) {
        proxy.$modal.msgWarning('请选择文件')
        return
      }

      uploading.value = true
      const formData = new FormData()
      formData.append('file', uploadForm.value.file)
      formData.append('contractId', route.query.contractId)
      formData.append('documentType', uploadForm.value.documentType)
      formData.append('fileDescription', uploadForm.value.fileDescription || '')

      uploadAttachment(formData).then(response => {
        proxy.$modal.msgSuccess('上传成功')
        resetUploadForm()
        getAttachmentList()
      }).finally(() => {
        uploading.value = false
      })
    }
  })
}

/** 重置上传表单 */
function resetUploadForm() {
  uploadForm.value = {
    documentType: null,
    file: null,
    fileDescription: ''
  }
  fileList.value = []
  proxy.$refs.uploadFormRef?.resetFields()
}

/** 查询附件列表 */
function getAttachmentList() {
  const contractId = route.query.contractId
  if (contractId) {
    listAttachment({ contractId: contractId }).then(response => {
      attachmentList.value = response.rows
    })
  }
}

/** 下载附件 */
function handleDownload(row) {
  downloadAttachment(row.attachmentId).then(response => {
    const blob = new Blob([response])
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = row.fileName
    link.click()
    window.URL.revokeObjectURL(link.href)
  })
}

/** 查看所有附件操作日志 */
function handleViewAllLogs() {
  const contractId = route.query.contractId
  if (!contractId) {
    proxy.$modal.msgWarning('合同ID不存在')
    return
  }

  // 查询该合同下所有附件的操作日志
  // 这里假设后端提供了按合同ID查询所有附件日志的接口
  // 如果没有，可以遍历所有附件获取日志
  if (attachmentList.value.length === 0) {
    proxy.$modal.msgWarning('暂无附件')
    return
  }

  // 收集所有附件的日志
  const logPromises = attachmentList.value.map(attachment =>
    getAttachmentLog(attachment.attachmentId).then(response => response.data || [])
  )

  Promise.all(logPromises).then(results => {
    // 合并所有日志并按时间排序
    logList.value = results.flat().sort((a, b) => {
      return new Date(b.operationTime) - new Date(a.operationTime)
    })
    logDialogVisible.value = true
  }).catch(() => {
    proxy.$modal.msgError('获取操作日志失败')
  })
}

/** 删除附件 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除该附件？').then(() => {
    return delAttachment(row.attachmentId)
  }).then(() => {
    getAttachmentList()
    proxy.$modal.msgSuccess('删除成功')
  })
}

/** 返回按钮 */
function handleBack() {
  router.back()
}

/** 初始化数据 */
function init() {
  getDeptTree()
  getCustomerList()

  const contractId = route.query.contractId
  if (contractId) {
    // 查询合同信息
    getContract(contractId).then(response => {
      contractInfo.value = response.data
    })
    // 查询附件列表
    getAttachmentList()
  }
}

init()
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.label-bold) {
  font-weight: bold;
  background-color: #f5f7fa;
}

:deep(.el-upload__tip) {
  color: #909399;
  font-size: 12px;
  margin-top: 7px;
}
</style>
