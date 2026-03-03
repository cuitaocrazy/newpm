<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="项目名称" prop="projectName">
        <el-autocomplete
          v-model="queryParams.projectName"
          :fetch-suggestions="fetchProjectSuggestions"
          placeholder="请选择或搜索项目"
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
      :span-method="spanMethod"
      :height="tableHeight"
      border
      style="width: 100%"
    >
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column label="项目名称" align="left" header-align="center" prop="projectName" width="220">
        <template #default="scope">
          <div class="project-name-cell">{{ scope.row.projectName }}</div>
        </template>
      </el-table-column>
      <el-table-column label="项目经理" prop="projectManagerName" align="center" />
      <el-table-column label="预估工作量(人天)" prop="estimatedWorkload" align="center" />
      <el-table-column label="实际人天" prop="actualDays" align="center" />
      <el-table-column label="日报人天(合计)" prop="totalActualDays" align="center" />
      <el-table-column label="调整人天" min-width="150" align="center">
        <template #default="scope">
          <el-input-number
            v-model="scope.row.adjustWorkload"
            :precision="2"
            :step="1"
            style="width: 138px"
            @change="(val) => handleAdjustChange(scope.row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="项目阶段" prop="projectStage" align="center">
        <template #default="scope">
          <dict-tag :options="sys_xmjd" :value="scope.row.projectStage" />
        </template>
      </el-table-column>
      <el-table-column label="日报人天" prop="stageDays" align="center" />
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="ProjectStats">
import { ref, nextTick, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { getProjectStats, getProjectNameSuggestions, updateAdjustWorkload } from '@/api/project/dailyReport'

const { proxy } = getCurrentInstance()
const { sys_xmjd } = proxy.useDict('sys_xmjd')

const loading = ref(false)
const tableRows = ref([])
const spanMap = ref([])
const total = ref(0)
const tableHeight = ref(600)

const queryParams = ref({
  projectName: '',
  pageNum: 1,
  pageSize: 10
})

function calcTableHeight() {
  nextTick(() => {
    const searchHeight = 50
    const paginationHeight = 65  // 50px 高度 + 15px margin-top
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

/** 将项目列表展开为平铺行，计算 rowspan */
function buildTableRows(list) {
  const rows = []
  const map = []

  for (const project of list) {
    const stages = project.stages && project.stages.length > 0
      ? project.stages
      : [{ projectStage: null, stageDays: null }]
    const rowspan = stages.length

    stages.forEach((stage, idx) => {
      rows.push({
        projectId: project.projectId,
        projectName: project.projectName,
        projectManagerName: project.projectManagerName,
        estimatedWorkload: project.estimatedWorkload,
        actualDays: project.actualDays,
        totalActualDays: project.totalActualDays,
        adjustWorkload: project.adjustWorkload,
        projectStage: stage.projectStage,
        stageDays: stage.stageDays
      })
      map.push(idx === 0 ? rowspan : 0)
    })
  }

  tableRows.value = rows
  spanMap.value = map
}

/** 前7列按项目合并，后2列（项目阶段/日报人天）不合并 */
function spanMethod({ rowIndex, columnIndex }) {
  if (columnIndex <= 6) {
    const s = spanMap.value[rowIndex]
    return s > 0 ? { rowspan: s, colspan: 1 } : { rowspan: 0, colspan: 0 }
  }
  return { rowspan: 1, colspan: 1 }
}

/** 调整人天修改：同步前端、自动保存 */
async function handleAdjustChange(row, val) {
  const adjust = val ?? 0
  // 同步同一项目的所有行（actualDays = totalActualDays + adjustWorkload）
  tableRows.value.forEach(r => {
    if (r.projectId === row.projectId) {
      r.adjustWorkload = adjust
      r.actualDays = +((r.totalActualDays || 0) + adjust).toFixed(2)
    }
  })
  try {
    await updateAdjustWorkload(row.projectId, adjust)
    proxy.$modal.msgSuccess('调整人天已保存')
  } catch {
    proxy.$modal.msgError('保存失败，请重试')
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
