<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="项目名称" prop="projectName">
        <el-autocomplete
          v-model="queryParams.projectName"
          :fetch-suggestions="fetchProjectSuggestions"
          placeholder="输入关键字搜索，或直接选择下拉数据"
          clearable
          style="width: 300px"
          value-key="projectName"
          @select="handleProjectSelect"
          @clear="handleSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 统计表格 -->
    <el-table
      v-loading="loading"
      :data="tableRows"
      :height="tableHeight"
      border
      style="width: 100%"
    >
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column label="项目名称" align="left" header-align="center" prop="projectName" width="220">
        <template #default="scope">
          <div class="project-name-cell">
            <a
              :href="projectHref(scope.row.projectId)"
              class="el-link el-link--primary"
              style="text-decoration: none; white-space: normal; word-break: break-all; line-height: 1.5;"
              @click.prevent="router.push(`/project/list/detail/${scope.row.projectId}`)"
            >{{ scope.row.projectName }}</a>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="项目经理" prop="projectManagerName" align="center" />
      <el-table-column label="预估工作量(人天)" prop="estimatedWorkload" align="center" />
      <el-table-column label="实际人天" prop="actualDays" align="center" />
      <el-table-column label="日报人天(合计)" prop="totalActualDays" align="center" />
      <el-table-column label="调整人天" prop="adjustWorkload" align="center" />
      <el-table-column label="操作" width="130" align="center">
        <template #default="scope">
          <el-button type="primary" link @click="openCorrectDialog(scope.row)">补正</el-button>
          <el-button type="info" link @click="openLogDialog(scope.row)">日志</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 补正对话框 -->
    <el-dialog v-model="correctDialog.visible" title="人天补正" width="480px" @close="resetCorrectForm">
      <el-form :model="correctForm" ref="correctFormRef" :rules="correctRules" label-width="80px">
        <el-form-item label="调整人天" prop="delta">
          <div style="display: flex; align-items: center; gap: 8px">
            <el-select v-model="correctForm.direction" style="width: 90px">
              <el-option label="增加" :value="0" />
              <el-option label="减少" :value="1" />
            </el-select>
            <el-input-number
              v-model="correctForm.delta"
              :min="0"
              :precision="2"
              :step="1"
              style="width: 150px"
            />
            <span>人天</span>
          </div>
        </el-form-item>
        <el-form-item label="理由" prop="reason">
          <el-input v-model="correctForm.reason" placeholder="请输入补正理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="correctDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="correctDialog.saving" @click="saveCorrect">保存</el-button>
      </template>
    </el-dialog>

    <!-- 补正日志对话框 -->
    <el-dialog v-model="logDialog.visible" :title="`补正日志 — ${logDialog.projectName}`" width="780px">
      <el-table :data="logDialog.list" v-loading="logDialog.loading" border stripe size="small">
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column label="操作时间" prop="createTime" width="160" align="center" />
        <el-table-column label="操作人" prop="createByName" width="100" align="center" />
        <el-table-column label="方向" width="70" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.direction === 0 ? 'success' : 'danger'" size="small">
              {{ scope.row.direction === 0 ? '增加' : '减少' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="调整数" prop="delta" width="90" align="center" />
        <el-table-column label="调整前" prop="beforeAdjust" width="90" align="center" />
        <el-table-column label="调整后" prop="afterAdjust" width="90" align="center" />
        <el-table-column label="理由" prop="reason" min-width="150" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!logDialog.loading && logDialog.list.length === 0" description="暂无补正记录" />
      <template #footer>
        <el-button @click="logDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ProjectStats">
import { ref, nextTick, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { getProjectStats, getProjectNameSuggestions, correctAdjustWorkload, getCorrectLogs } from '@/api/project/dailyReport'

const { proxy } = getCurrentInstance()
const router = useRouter()

function projectHref(projectId) {
  return projectId ? router.resolve(`/project/list/detail/${projectId}`).href : undefined
}

const loading = ref(false)
const tableRows = ref([])
const total = ref(0)
const tableHeight = ref(600)

const queryParams = ref({
  projectName: '',
  pageNum: 1,
  pageSize: 10
})

// 补正对话框
const correctDialog = ref({ visible: false, saving: false, row: null })
const correctFormRef = ref(null)
const correctForm = ref({ direction: 0, delta: 0, reason: '' })
const correctRules = {
  delta: [{ required: true, message: '请输入调整人天数', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入补正理由', trigger: 'blur' }]
}

// 日志对话框
const logDialog = ref({ visible: false, loading: false, list: [], projectName: '' })

function calcTableHeight() {
  nextTick(() => {
    const searchHeight = 50
    const paginationHeight = 65
    const padding = 120
    tableHeight.value = window.innerHeight - searchHeight - paginationHeight - padding
  })
}

onMounted(() => {
  calcTableHeight()
  window.addEventListener('resize', calcTableHeight)
})

onUnmounted(() => {
  window.removeEventListener('resize', calcTableHeight)
})

async function getList() {
  loading.value = true
  try {
    const res = await getProjectStats(
      queryParams.value.projectName || undefined,
      queryParams.value.pageNum,
      queryParams.value.pageSize
    )
    const data = res.data || {}
    total.value = data.total || 0
    buildTableRows(data.rows || [])
  } finally {
    loading.value = false
  }
}

/** 将项目列表构建为表格行（每项目一行） */
function buildTableRows(list) {
  tableRows.value = list.map(project => ({
    projectId: project.projectId,
    projectName: project.projectName,
    projectManagerName: project.projectManagerName,
    estimatedWorkload: project.estimatedWorkload,
    actualDays: project.actualDays,
    totalActualDays: project.totalActualDays,
    adjustWorkload: project.adjustWorkload
  }))
}

function openCorrectDialog(row) {
  correctDialog.value.row = row
  correctForm.value = { direction: 0, delta: 0, reason: '' }
  correctDialog.value.visible = true
}

function resetCorrectForm() {
  correctFormRef.value?.resetFields()
}

async function saveCorrect() {
  const valid = await correctFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const row = correctDialog.value.row
  const current = row.adjustWorkload || 0
  const delta = correctForm.value.delta || 0
  const afterAdjust = correctForm.value.direction === 0
    ? +((current + delta).toFixed(2))
    : +((current - delta).toFixed(2))

  correctDialog.value.saving = true
  try {
    await correctAdjustWorkload(row.projectId, {
      direction: correctForm.value.direction,
      delta,
      afterAdjust,
      reason: correctForm.value.reason
    })
    // 同步前端所有同项目行
    tableRows.value.forEach(r => {
      if (r.projectId === row.projectId) {
        r.adjustWorkload = afterAdjust
        r.actualDays = +((r.totalActualDays || 0) + afterAdjust).toFixed(2)
      }
    })
    proxy.$modal.msgSuccess('补正已保存')
    correctDialog.value.visible = false
  } catch {
    proxy.$modal.msgError('保存失败，请重试')
  } finally {
    correctDialog.value.saving = false
  }
}

async function openLogDialog(row) {
  logDialog.value.projectName = row.projectName
  logDialog.value.list = []
  logDialog.value.visible = true
  logDialog.value.loading = true
  try {
    const res = await getCorrectLogs(row.projectId)
    logDialog.value.list = res.data || []
  } finally {
    logDialog.value.loading = false
  }
}

async function fetchProjectSuggestions(keyword, callback) {
  try {
    const res = await getProjectNameSuggestions(keyword || undefined)
    callback(res.data || [])
  } catch { callback([]) }
}

function handleProjectSelect(item) {
  queryParams.value.projectName = item.projectName
  queryParams.value.pageNum = 1
  getList()
}

function handleSearch() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value.projectName = ''
  queryParams.value.pageNum = 1
  getList()
}

getList()
</script>

<style scoped>
:deep(.el-pagination) {
  margin-top: 15px;
  text-align: right;
}

.project-name-cell {
  word-break: break-all;
  white-space: normal;
  line-height: 1.5;
  text-align: left;
}
</style>
