<template>
  <div class="app-container">
    <div style="margin-bottom: 12px;">
      <el-button icon="Back" @click="goBack">返回</el-button>
      <span style="font-size: 18px; font-weight: bold; margin-left: 12px;">问题单及缺陷详情</span>
    </div>

    <el-card shadow="hover" style="margin-bottom: 15px;">
      <template #header><span style="font-weight: bold;">任务信息</span></template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="任务名称">{{ form.taskName }}</el-descriptions-item>
        <el-descriptions-item label="软件中心任务号">{{ form.taskCode }}</el-descriptions-item>
        <el-descriptions-item label="项目组">{{ form.deptName }}</el-descriptions-item>
        <el-descriptions-item label="二级产品">{{ form.product }}</el-descriptions-item>
        <el-descriptions-item label="投产年份">{{ form.productionYear }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ form.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="计划投产日期">{{ form.planProductionDate }}</el-descriptions-item>
        <el-descriptions-item label="提交内部测试B包日期">{{ form.internalClosureDate }}</el-descriptions-item>
        <el-descriptions-item label="提交功能测试版本日期">{{ form.functionalTestDate }}</el-descriptions-item>
        <el-descriptions-item label="提交生产版本日期">{{ form.productionVersionDate }}</el-descriptions-item>
        <el-descriptions-item label="排期状态"><dict-tag :options="sys_pqzt" :value="form.scheduleStatus" /></el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="hover" style="margin-bottom: 15px;">
      <template #header><span style="font-weight: bold;">问题单及缺陷</span></template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="问题单编号">{{ form.problemNo }}</el-descriptions-item>
        <el-descriptions-item label="问题单级别"><dict-tag :options="sys_problem_level" :value="form.problemLevel" /></el-descriptions-item>
        <el-descriptions-item label="当前状态"><dict-tag :options="sys_problem_state" :value="form.currentStatus" /></el-descriptions-item>
        <el-descriptions-item label="提交日期">{{ form.submitDate }}</el-descriptions-item>
        <el-descriptions-item label="问题单关闭日期">{{ form.settleDate }}</el-descriptions-item>
        <el-descriptions-item label="核查日期">{{ form.verifyDate }}</el-descriptions-item>
        <el-descriptions-item label="是否缺陷">{{ yn(form.whetherDefect) }}</el-descriptions-item>
        <el-descriptions-item label="是否超时">{{ yn(form.whetherOvertime) }}</el-descriptions-item>
        <el-descriptions-item label="是否问题重现">{{ yn(form.whetherProRecurrence) }}</el-descriptions-item>
        <el-descriptions-item label="是否须关注">{{ yn(form.whetherAttRequired) }}</el-descriptions-item>
        <el-descriptions-item label="解决时间超一天">{{ yn(form.solutionTimeOverOneDay) }}</el-descriptions-item>
        <el-descriptions-item label="是否更新版本">{{ yn(form.whetherUpdateVersion) }}</el-descriptions-item>
        <el-descriptions-item label="缺陷说明/超时说明" :span="3">{{ form.defectDesc }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ form.remarks }}</el-descriptions-item>
        <el-descriptions-item label="创建人员">{{ form.createByName }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ form.createTime }}</el-descriptions-item>
        <el-descriptions-item label="修改人员">{{ form.updateByName }}</el-descriptions-item>
        <el-descriptions-item label="最后修改时间">{{ form.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 附件区（复用通用附件体系 business_type=prolist） -->
    <el-card shadow="hover" style="margin-bottom: 15px;">
      <template #header>
        <span style="font-weight: bold;">附件列表</span>
        <el-button v-hasPermi="['project:prolistDefect:file']" type="primary" size="small" icon="Upload" style="float:right" @click="openUpload">上传附件</el-button>
      </template>
      <el-table :data="attachmentList" border v-loading="attachmentLoading">
        <el-table-column label="文档类型" prop="documentType" width="120" align="center">
          <template #default="{ row }"><dict-tag :options="sys_prolist_file_type" :value="row.documentType" /></template>
        </el-table-column>
        <el-table-column label="文件名称" prop="fileName" min-width="200" show-overflow-tooltip />
        <el-table-column label="文件说明" prop="fileDescription" min-width="160" show-overflow-tooltip />
        <el-table-column label="上传人员" prop="createBy" width="120" align="center" />
        <el-table-column label="上传时间" prop="createTime" width="160" align="center" />
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button type="primary" link icon="Download" @click="handleDownload(row)" v-hasPermi="['project:prolistDefect:file','project:prolistDefect:query']">下载</el-button>
            <el-button type="danger" link icon="Delete" @click="handleDeleteFile(row)" v-hasPermi="['project:prolistDefect:file']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="hover">
      <template #header><span style="font-weight: bold;">附件操作日志</span></template>
      <el-table :data="attachmentLogs" border v-loading="logLoading">
        <el-table-column label="操作类型" prop="operationType" width="120" align="center" />
        <el-table-column label="文件名称" prop="fileName" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作人" prop="operatorName" width="120" align="center" />
        <el-table-column label="操作时间" prop="operationTime" width="160" align="center" />
      </el-table>
    </el-card>

    <!-- 上传弹窗 -->
    <el-dialog v-model="uploadOpen" title="上传附件" width="500px" append-to-body>
      <el-form :model="uploadForm" label-width="90px">
        <el-form-item label="文档类型" required>
          <dict-select v-model="uploadForm.documentType" dict-type="sys_prolist_file_type" placeholder="请选择文档类型" />
        </el-form-item>
        <el-form-item label="文件说明">
          <el-input v-model="uploadForm.fileDescription" placeholder="请输入文件说明" maxlength="512" />
        </el-form-item>
        <el-form-item label="选择文件" required>
          <el-upload ref="uploadRef" :auto-upload="false" :limit="1" :on-change="onFileChange" :on-exceed="onExceed">
            <el-button type="primary">选取文件</el-button>
            <template #tip><div class="el-upload__tip">支持 doc/docx/xls/xlsx/pdf/png/jpg/zip 等，≤30MB</div></template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadOpen = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">确定上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ProlistDefectDetail">
import { ref, getCurrentInstance, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProlistDefect } from '@/api/project/prolistDefect'
import { listAttachment, uploadAttachment, downloadAttachment, delAttachment, listAttachmentLog } from '@/api/project/attachment'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const { sys_problem_state, sys_problem_level, sys_pqzt, sys_prolist_file_type } =
  proxy.useDict('sys_problem_state', 'sys_problem_level', 'sys_pqzt', 'sys_prolist_file_type')

const BIZ = 'prolist'
const problemId = route.query.problemId

const form = ref({})
const attachmentList = ref([])
const attachmentLogs = ref([])
const attachmentLoading = ref(false)
const logLoading = ref(false)

const uploadOpen = ref(false)
const uploading = ref(false)
const uploadRef = ref()
const uploadForm = ref({ documentType: undefined, fileDescription: '', file: null })

function yn(v) { return v === '1' ? '是' : '否' }

function loadDetail() {
  getProlistDefect(problemId).then(res => { form.value = res.data || {} })
}

function loadAttachments() {
  attachmentLoading.value = true
  listAttachment({ businessType: BIZ, businessId: problemId }).then(res => {
    attachmentList.value = res.rows || res.data || []
  }).finally(() => { attachmentLoading.value = false })
}

function loadLogs() {
  logLoading.value = true
  listAttachmentLog(BIZ, problemId).then(res => {
    attachmentLogs.value = res.rows || res.data || []
  }).finally(() => { logLoading.value = false })
}

function openUpload() {
  uploadForm.value = { documentType: undefined, fileDescription: '', file: null }
  proxy.$nextTick(() => uploadRef.value?.clearFiles())
  uploadOpen.value = true
}

function onFileChange(file) { uploadForm.value.file = file.raw }
function onExceed(files) {
  uploadRef.value.clearFiles()
  uploadRef.value.handleStart(files[0])
  uploadForm.value.file = files[0]
}

function submitUpload() {
  if (!uploadForm.value.documentType) { proxy.$modal.msgError('请选择文档类型'); return }
  if (!uploadForm.value.file) { proxy.$modal.msgError('请选择文件'); return }
  const fd = new FormData()
  fd.append('businessType', BIZ)
  fd.append('businessId', problemId)
  fd.append('documentType', uploadForm.value.documentType)
  fd.append('fileDescription', uploadForm.value.fileDescription || '')
  fd.append('file', uploadForm.value.file)
  uploading.value = true
  uploadAttachment(fd).then(() => {
    proxy.$modal.msgSuccess('上传成功')
    uploadOpen.value = false
    loadAttachments()
    loadLogs()
  }).finally(() => { uploading.value = false })
}

function handleDownload(row) {
  downloadAttachment(row.attachmentId).then(res => {
    const blob = new Blob([res])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.fileName
    a.click()
    window.URL.revokeObjectURL(url)
    loadLogs()
  })
}

function handleDeleteFile(row) {
  proxy.$modal.confirm(`确认删除附件「${row.fileName}」？`).then(() => delAttachment(row.attachmentId)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    loadAttachments()
    loadLogs()
  }).catch(() => {})
}

function goBack() { router.back() }

onMounted(() => {
  loadDetail()
  loadAttachments()
  loadLogs()
})
</script>
