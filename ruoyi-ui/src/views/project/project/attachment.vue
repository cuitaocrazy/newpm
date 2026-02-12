<template>
  <div class="app-container project-attachment">
    <!-- 页面标题 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <h2>项目附件管理</h2>
        <p class="tips">管理项目相关附件文档</p>
      </div>
    </el-card>

    <!-- 项目基本信息 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">项目基本信息</span>
      </template>
      <el-descriptions :column="3" border v-loading="projectLoading">
        <el-descriptions-item label="行业">
          <dict-tag :options="industry" :value="project.industry" />
        </el-descriptions-item>
        <el-descriptions-item label="一级区域">
          <dict-tag :options="sys_yjqy" :value="project.region" />
        </el-descriptions-item>
        <el-descriptions-item label="二级区域">
          {{ project.regionName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="简称">
          {{ project.shortName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="立项年份">
          {{ project.establishedYear || '-' }} 年
        </el-descriptions-item>
        <el-descriptions-item label="项目编号" :span="2">
          {{ project.projectCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="项目名称" :span="3">
          {{ project.projectName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="项目分类">
          <dict-tag :options="sys_xmfl" :value="project.projectCategory" />
        </el-descriptions-item>
        <el-descriptions-item label="项目部门">
          {{ project.projectDept || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="预估工作量">
          {{ project.estimatedWorkload || 0 }} 人天
        </el-descriptions-item>
        <el-descriptions-item label="项目预算">
          {{ project.projectBudget || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="实际工作量">
          {{ project.actualWorkload || 0 }} 人天
        </el-descriptions-item>
        <el-descriptions-item label="项目阶段">
          <dict-tag :options="sys_xmjd" :value="project.projectStage" />
        </el-descriptions-item>
        <el-descriptions-item label="验收状态">
          <dict-tag :options="sys_yszt" :value="project.acceptanceStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <dict-tag :options="sys_spzt" :value="project.approvalStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="项目地址" :span="3">
          {{ project.projectAddress || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="项目计划" :span="3">
          <div class="text-content">{{ project.projectPlan || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="项目描述" :span="3">
          <div class="text-content">{{ project.projectDescription || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="3">
          {{ project.approvalReason || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 人员配置 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">人员配置</span>
      </template>
      <el-descriptions :column="3" border v-loading="projectLoading">
        <el-descriptions-item label="项目经理">
          {{ project.projectManagerName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="市场经理">
          {{ project.marketManagerName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="销售负责人">
          {{ project.salesManagerName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="销售联系方式" :span="3">
          {{ project.salesContact || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="参与人员" :span="3">
          <div v-if="selectedParticipants.length > 0" class="selected-participants">
            <el-tag v-for="user in selectedParticipants" :key="user.userId"
              type="info" class="participant-tag">
              {{ user.nickName }}
            </el-tag>
          </div>
          <span v-else style="color: #909399;">暂无参与人员</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 客户信息 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">客户信息</span>
      </template>
      <el-descriptions :column="3" border v-loading="projectLoading">
        <el-descriptions-item label="客户名称">
          {{ project.customerName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="客户联系人">
          {{ project.customerContactName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="客户联系方式">
          {{ customerContactPhone || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="商户联系人">
          {{ project.merchantContact || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="商户联系方式" :span="2">
          {{ project.merchantPhone || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 时间规划 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">时间规划</span>
      </template>
      <el-descriptions :column="3" border v-loading="projectLoading">
        <el-descriptions-item label="启动日期">
          {{ project.startDate || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="结束日期">
          {{ project.endDate || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="投产日期">
          {{ project.productionDate || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="验收日期" :span="3">
          {{ project.acceptanceDate || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 成本预算 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">成本预算</span>
      </template>
      <el-descriptions :column="3" border v-loading="projectLoading">
        <el-descriptions-item label="项目预算">
          {{ project.projectBudget || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="项目费用">
          {{ project.projectCost || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="费用预算">
          {{ project.costBudget || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="成本预算">
          {{ project.budgetCost || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="人力费用">
          {{ project.laborCost || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="采购成本">
          {{ project.purchaseCost || 0 }} 元
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 备注 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">备注</span>
      </template>
      <el-descriptions :column="1" border v-loading="projectLoading">
        <el-descriptions-item label="备注">
          <div class="text-content">{{ project.remark || '-' }}</div>
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
                上传
              </el-button>
              <el-button @click="resetUploadForm">重置</el-button>
              <el-button type="primary" @click="handleShowLog">
                <el-icon><Document /></el-icon>
                日志
              </el-button>
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
        </div>
      </template>
      <el-table v-loading="listLoading" :data="attachmentList" border>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="文档类型" prop="documentType" width="150" align="center">
          <template #default="scope">
            <dict-tag :options="sys_wdlx" :value="scope.row.documentType"/>
          </template>
        </el-table-column>
        <el-table-column label="文件名称" prop="fileName" min-width="200" show-overflow-tooltip />
        <el-table-column label="文件大小" prop="fileSize" width="120" align="center">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="文件说明" prop="fileDescription" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.fileDescription || '-' }}
          </template>
        </el-table-column>
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

<script setup name="ProjectAttachment">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject } from '@/api/project/project'
import { listUser } from '@/api/system/user'
import { listContactByCustomer } from '@/api/project/contact'
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
const { sys_xmfl, sys_xmjd, sys_yszt, sys_spzt, industry, sys_yjqy, sys_wdlx } =
  proxy.useDict('sys_xmfl', 'sys_xmjd', 'sys_yszt', 'sys_spzt', 'industry', 'sys_yjqy', 'sys_wdlx')

// 项目ID
const projectId = ref(null)

// 项目信息
const projectLoading = ref(false)
const project = ref({})

// 参与人员
const allUsers = ref([])
const selectedParticipants = ref([])
const customerContactPhone = ref('')

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

// 格式化文件大小
const formatFileSize = (size) => {
  if (!size || size === 0) return '-'
  const kb = size / 1024
  if (kb < 1024) {
    return kb.toFixed(2) + ' KB'
  }
  const mb = kb / 1024
  return mb.toFixed(2) + ' MB'
}

// 加载所有用户（用于参与人员显示）
function loadAllUsers() {
  listUser({}).then(response => {
    allUsers.value = response.rows || []
  })
}

// 加载客户联系人电话
function loadCustomerContactPhone(customerId, contactId) {
  listContactByCustomer(customerId).then(response => {
    const contacts = response.data || []
    const contact = contacts.find(c => c.contactId === contactId)
    if (contact) {
      customerContactPhone.value = contact.contactPhone || '-'
    }
  }).catch(() => {
    customerContactPhone.value = '-'
  })
}

// 加载项目信息
const loadProjectInfo = () => {
  if (!projectId.value) {
    proxy.$modal.msgError('项目ID不能为空')
    handleBack()
    return
  }

  projectLoading.value = true
  getProject(projectId.value).then(response => {
    const data = response.data || {}
    project.value = data

    // 处理参与人员
    if (data.participants) {
      const participantIds = data.participants.split(',').map(Number)
      selectedParticipants.value = allUsers.value.filter(u => participantIds.includes(u.userId))
    }

    // 加载客户联系人电话
    if (data.customerId && data.customerContactId) {
      loadCustomerContactPhone(data.customerId, data.customerContactId)
    }

    projectLoading.value = false
  }).catch(() => {
    projectLoading.value = false
  })
}

// 加载附件列表
const loadAttachmentList = () => {
  if (!projectId.value) return

  listLoading.value = true
  listAttachment({
    businessType: 'project',
    businessId: projectId.value
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
      formData.append('businessType', 'project')
      formData.append('businessId', projectId.value)
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
  const message = `确认删除附件【${row.fileName}】吗？`
  proxy.$modal.confirm(message).then(() => {
    return delAttachment(row.attachmentId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    loadAttachmentList()
  }).catch(() => {})
}

// 显示操作日志
const handleShowLog = () => {
  if (!projectId.value) return

  logDialogVisible.value = true
  logLoading.value = true
  listAttachmentLog('project', projectId.value).then(response => {
    logList.value = response.data || []
    logLoading.value = false
  }).catch(() => {
    logLoading.value = false
  })
}

// 返回
const handleBack = () => {
  router.push({ path: '/project/list', query: { t: Date.now() } })
}

// 初始化
onMounted(() => {
  projectId.value = route.params.projectId
  if (projectId.value) {
    loadAllUsers()
    loadProjectInfo()
    loadAttachmentList()
  } else {
    proxy.$modal.msgError('项目ID不能为空')
    handleBack()
  }
})
</script>

<style scoped lang="scss">
.project-attachment {
  // 页面标题
  .page-header {
    margin-bottom: 20px;

    .header-content {
      h2 {
        margin: 0 0 8px 0;
        font-size: 22px;
        font-weight: 600;
        color: #303133;
      }

      .tips {
        margin: 0;
        font-size: 14px;
        color: #909399;
      }
    }
  }

  // 详情卡片、上传卡片、附件列表卡片
  .detail-card,
  .upload-card,
  .attachment-list-card {
    margin-bottom: 20px;

    .card-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      padding-left: 10px;
      border-left: 3px solid #409eff;
    }

    // 文本内容区域
    .text-content {
      white-space: pre-wrap;
      word-break: break-word;
      line-height: 1.8;
    }

    // 参与人员标签
    .selected-participants {
      .participant-tag {
        margin-right: 8px;
        margin-bottom: 4px;
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
