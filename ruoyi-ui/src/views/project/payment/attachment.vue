<template>
  <div class="app-container payment-attachment">
    <!-- 第一部分：付款里程碑信息（只读） -->
    <el-card class="payment-info-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">付款里程碑基本信息</span>
        </div>
      </template>
      <el-descriptions :column="2" border v-loading="paymentLoading">
        <!-- 第一行：合同名称 -->
        <el-descriptions-item label="合同名称" :span="2">
          {{ payment.contractName || '-' }}
        </el-descriptions-item>

        <!-- 第二行：合同编号、合同金额 -->
        <el-descriptions-item label="合同编号">
          {{ payment.contractCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="合同金额">
          {{ formatAmount(payment.contractAmount) }} 元
        </el-descriptions-item>

        <!-- 第三行：付款方式名称、付款状态 -->
        <el-descriptions-item label="付款方式名称">
          {{ payment.paymentMethodName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="付款状态">
          <dict-tag v-if="payment.paymentStatus" :options="sys_fkzt" :value="payment.paymentStatus"/>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 第二部分：附件上传表单 -->
    <el-card class="upload-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">上传附件</span>
        </div>
      </template>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="文档类型" prop="documentType">
              <el-select v-model="uploadForm.documentType" placeholder="请选择文档类型" clearable style="width: 100%">
                <el-option v-for="dict in sys_wdlx" :key="dict.value"
                  :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="上传附件" prop="file">
              <el-upload
                ref="uploadRef"
                :auto-upload="false"
                :limit="1"
                :on-change="handleFileChange"
                :on-exceed="handleExceed"
                :before-remove="handleBeforeRemove"
                :file-list="fileList"
                accept=".doc,.docx,.xls,.xlsx,.pdf,.csv,.png,.jpg,.gif,.txt,.7z,.zip,.gz"
              >
                <el-button type="primary" icon="Upload">选择文件</el-button>
                <template #tip>
                  <div class="el-upload__tip">
                    支持格式：doc、docx、xls、xlsx、pdf、csv、png、jpg、gif、txt、7z、zip、gz，单个文件不超过30MB
                  </div>
                </template>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="文档说明">
              <el-input v-model="uploadForm.fileDescription" type="textarea" :rows="3"
                placeholder="请输入文档说明（选填）" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item>
              <el-button type="primary" @click="handleUpload" :loading="uploading"
                :disabled="fileList.length === 0">
                <el-icon><Upload /></el-icon>
                上传附件
              </el-button>
              <el-button @click="resetUploadForm">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 第三部分：附件列表 -->
    <el-card class="attachment-list-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">附件列表</span>
          <el-button type="primary" link @click="handleShowLog">
            <el-icon><Document /></el-icon>
            操作日志
          </el-button>
        </div>
      </template>
      <el-table v-loading="listLoading" :data="attachmentList" border>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="文档类型" prop="documentTypeName" width="120" align="center" />
        <el-table-column label="文件名称" prop="fileName" min-width="200" show-overflow-tooltip />
        <el-table-column label="文件说明" prop="fileDescription" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.fileDescription || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="上传人员" prop="createByName" width="120" align="center" />
        <el-table-column label="上传时间" prop="createTime" width="160" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleDownload(scope.row)">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 操作日志弹窗 -->
    <el-dialog v-model="logDialogVisible" title="附件操作日志" width="900px" append-to-body>
      <el-table v-loading="logLoading" :data="logList" border max-height="500">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="调整说明" prop="operationDesc" min-width="200" show-overflow-tooltip />
        <el-table-column label="文档类型" prop="documentTypeName" width="120" align="center" />
        <el-table-column label="调整人" prop="operatorName" width="120" align="center" />
        <el-table-column label="调整时间" prop="createTime" width="160" align="center" />
      </el-table>
      <template #footer>
        <el-button @click="logDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 底部操作按钮 -->
    <div class="form-footer">
      <el-button size="large" @click="handleBack">返回</el-button>
    </div>
  </div>
</template>

<script setup name="PaymentAttachment">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPayment } from '@/api/project/payment'
import {
  listAttachment,
  uploadAttachment,
  downloadAttachment,
  delAttachment,
  listAttachmentLog
} from '@/api/project/attachment'
import { saveAs } from 'file-saver'
import { Upload, Download, Delete, Document } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()
const { sys_wdlx, sys_fkzt, sys_ndgl } = proxy.useDict('sys_wdlx', 'sys_fkzt', 'sys_ndgl')

// 付款里程碑ID
const paymentId = ref(null)

// 付款里程碑信息
const paymentLoading = ref(false)
const payment = ref({})

// 上传表单
const uploadFormRef = ref(null)
const uploadRef = ref(null)
const uploading = ref(false)
const fileList = ref([])
const uploadForm = reactive({
  documentType: '',
  file: null,
  fileDescription: ''
})

const uploadRules = {
  documentType: [
    { required: true, message: '请选择文档类型', trigger: 'change' }
  ],
  file: [
    { required: true, message: '请选择要上传的文件', trigger: 'change' }
  ]
}

// 附件列表
const listLoading = ref(false)
const attachmentList = ref([])

// 操作日志
const logDialogVisible = ref(false)
const logLoading = ref(false)
const logList = ref([])

// 格式化金额
const formatAmount = (amount) => {
  if (amount === null || amount === undefined || amount === '') {
    return '0.00'
  }
  return Number(amount).toFixed(2)
}

// 加载付款里程碑信息
const loadPaymentInfo = () => {
  if (!paymentId.value) {
    proxy.$modal.msgError('付款里程碑ID不能为空')
    handleBack()
    return
  }

  paymentLoading.value = true
  getPayment(paymentId.value).then(response => {
    payment.value = response.data || {}
    paymentLoading.value = false
  }).catch(() => {
    paymentLoading.value = false
  })
}

// 加载附件列表
const loadAttachmentList = () => {
  if (!paymentId.value) return

  listLoading.value = true
  listAttachment({
    businessType: 'payment',
    businessId: paymentId.value
  }).then(response => {
    attachmentList.value = response.data || []
    listLoading.value = false
  }).catch(() => {
    listLoading.value = false
  })
}

// 文件选择变化
const handleFileChange = (file, files) => {
  // 文件大小校验（30MB）
  const isLt30M = file.size / 1024 / 1024 < 30
  if (!isLt30M) {
    proxy.$modal.msgError('上传文件大小不能超过 30MB!')
    files.pop()
    return false
  }

  // 文件格式校验
  const allowedExtensions = ['.doc', '.docx', '.xls', '.xlsx', '.pdf', '.csv', '.png', '.jpg', '.gif', '.txt', '.7z', '.zip', '.gz']
  const fileName = file.name
  const extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()

  if (!allowedExtensions.includes(extension)) {
    proxy.$modal.msgError('不支持的文件格式，请上传：doc、docx、xls、xlsx、pdf、csv、png、jpg、gif、txt、7z、zip、gz 格式的文件')
    files.pop()
    return false
  }

  uploadForm.file = file.raw
  fileList.value = files
}

// 文件超出限制
const handleExceed = () => {
  proxy.$modal.msgWarning('只能上传一个文件，请先删除已选文件')
}

// 删除文件前确认
const handleBeforeRemove = (file, files) => {
  uploadForm.file = null
  return true
}

// 上传附件
const handleUpload = () => {
  uploadFormRef.value.validate(valid => {
    if (valid) {
      if (!uploadForm.file) {
        proxy.$modal.msgError('请选择要上传的文件')
        return
      }

      // 构建FormData
      const formData = new FormData()
      formData.append('businessType', 'payment')
      formData.append('businessId', paymentId.value)
      formData.append('documentType', uploadForm.documentType)
      formData.append('file', uploadForm.file)
      if (uploadForm.fileDescription) {
        formData.append('fileDescription', uploadForm.fileDescription)
      }

      uploading.value = true
      uploadAttachment(formData).then(response => {
        proxy.$modal.msgSuccess('上传成功')
        resetUploadForm()
        loadAttachmentList()
      }).finally(() => {
        uploading.value = false
      })
    }
  })
}

// 重置上传表单
const resetUploadForm = () => {
  uploadForm.documentType = ''
  uploadForm.file = null
  uploadForm.fileDescription = ''
  fileList.value = []
  uploadFormRef.value?.resetFields()
}

// 下载附件
const handleDownload = (row) => {
  proxy.$modal.loading('正在下载文件，请稍候...')
  downloadAttachment(row.attachmentId).then(response => {
    const blob = new Blob([response])
    saveAs(blob, row.fileName)
    proxy.$modal.closeLoading()
    proxy.$modal.msgSuccess('下载成功')
  }).catch(() => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgError('下载失败')
  })
}

// 删除附件
const handleDelete = (row) => {
  const message = `确认删除【${payment.value.paymentMethodName}】下的【${row.fileName}】附件吗？`
  proxy.$modal.confirm(message).then(() => {
    return delAttachment(row.attachmentId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    loadAttachmentList()
  }).catch(() => {})
}

// 显示操作日志
const handleShowLog = () => {
  if (!paymentId.value) return

  logDialogVisible.value = true
  logLoading.value = true
  listAttachmentLog('payment', paymentId.value).then(response => {
    logList.value = response.data || []
    logLoading.value = false
  }).catch(() => {
    logLoading.value = false
  })
}

// 返回
const handleBack = () => {
  router.push({ path: '/htkx/payment', query: { t: Date.now() } })
}

// 初始化
onMounted(() => {
  paymentId.value = route.params.paymentId
  if (paymentId.value) {
    loadPaymentInfo()
    loadAttachmentList()
  } else {
    proxy.$modal.msgError('付款里程碑ID不能为空')
    handleBack()
  }
})
</script>

<style scoped lang="scss">
.payment-attachment {
  .payment-info-card,
  .upload-card,
  .attachment-list-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        padding-left: 10px;
        border-left: 3px solid #409eff;
      }
    }
  }

  // 描述列表样式
  :deep(.el-descriptions__label) {
    width: 120px;
    background-color: #fafafa;
    font-weight: 500;
  }

  :deep(.el-descriptions__content) {
    color: #303133;
  }

  // 上传组件样式
  .el-upload__tip {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
  }

  // 表格样式
  :deep(.el-table) {
    .el-button + .el-button {
      margin-left: 0;
    }
  }

  // 底部按钮
  .form-footer {
    position: sticky;
    bottom: 0;
    padding: 20px;
    background-color: #fff;
    border-top: 1px solid #dcdfe6;
    text-align: center;
    z-index: 10;

    .el-button {
      min-width: 120px;
    }
  }
}
</style>
