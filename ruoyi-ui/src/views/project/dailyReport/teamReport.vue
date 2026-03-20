<template>
  <div class="app-container">
    <!-- 查询栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="项目部门" prop="deptId">
        <project-dept-select
          v-model="queryParams.deptId"
          placeholder="请选择三级部门"
          style="width: 220px"
          @change="handleDeptChange"
        />
      </el-form-item>
      <el-form-item label="项目名称" prop="projectId">
        <el-autocomplete
          v-model="projectKeyword"
          :fetch-suggestions="fetchProjectSuggestions"
          placeholder="输入关键字筛选项目"
          clearable
          style="width: 240px"
          value-key="projectName"
          :trigger-on-focus="false"
          @select="handleProjectSelect"
          @clear="handleProjectClear"
        />
      </el-form-item>
      <el-form-item label="年月" prop="yearMonth">
        <el-date-picker
          v-model="queryParams.yearMonth"
          type="month"
          value-format="YYYY-MM"
          placeholder="选择年月"
          style="width: 140px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 图例说明 -->
    <div class="legend-bar">
      <span class="legend-item">
        <span class="legend-dot contract-dot"></span>绿色行 = 项目有关联合同（可能带来收入）
      </span>
      <span class="legend-item">
        <span class="legend-dot warn-dot"></span>红色 ⚠ = 实际人天已超预算的 50%
      </span>
    </div>

    <!-- 主体表格 -->
    <div v-loading="loading">
      <el-empty v-if="!loading && tableData.length === 0" description="暂无日报数据" />

      <template v-else>
        <el-table
          :data="flatRows"
          border
          style="width: 100%"
          :span-method="spanMethod"
          :row-class-name="rowClassName"
          :row-style="{ height: 'auto' }"
        >
          <!-- 固定列：项目名称（固定宽度，支持换行） -->
          <el-table-column label="项目" prop="projectName" fixed width="180">
            <template #default="{ row }">
              <span :class="row.hasContract ? 'contract-label' : 'project-label'">
                <el-icon v-if="row.hasContract" color="#67c23a"><CircleCheck /></el-icon>
                {{ row.projectName }}
              </span>
            </template>
          </el-table-column>
          <!-- 固定列：人员 -->
          <el-table-column label="人员" prop="nickName" fixed width="90" />

          <!-- 动态日期列 -->
          <el-table-column
            v-for="day in dayColumns"
            :key="day"
            :label="day.slice(8)"
            :prop="day"
            width="46"
            align="center"
          >
            <template #default="{ row }">
              <span v-if="row.dailyHours[day]" class="hours-badge">
                {{ row.dailyHours[day] }}
              </span>
            </template>
          </el-table-column>

          <!-- 个人实际人天（不合并） -->
          <el-table-column label="个人人天" fixed="right" width="80" align="center">
            <template #default="{ row }">
              <span v-if="row.totalHours">{{ formatDays(Number(row.totalHours) / 8) }}</span>
              <span v-else>—</span>
            </template>
          </el-table-column>

          <!-- 固定右列：实际人天（项目汇总） / 预算人天 —— 两列均按项目合并 -->
          <el-table-column label="实际人天" fixed="right" width="90" align="center">
            <template #default="{ row }">
              <template v-if="row.memberIndex === 0">
                <span
                  v-if="row.estimatedWorkload > 0 && row.projectActualDays > row.estimatedWorkload * 0.5"
                  class="warn-text"
                >
                  <el-icon><Warning /></el-icon>{{ formatDays(row.projectActualDays) }}
                </span>
                <span v-else>{{ formatDays(row.projectActualDays) }}</span>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="预算人天" fixed="right" width="90" align="center">
            <template #default="{ row }">
              <template v-if="row.memberIndex === 0">
                <span v-if="row.estimatedWorkload > 0">{{ formatDays(row.estimatedWorkload) }}</span>
                <span v-else>—</span>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Warning } from '@element-plus/icons-vue'
import { getTeamMonthly, getTeamProjectOptions } from '@/api/project/dailyReport'
import dayjs from 'dayjs'

// --- 查询参数 ---
const queryRef = ref()
const loading = ref(false)
const projectKeyword = ref('')

const queryParams = ref({
  deptId: undefined as number | undefined,
  projectId: undefined as number | undefined,
  yearMonth: dayjs().format('YYYY-MM')
})

// --- 数据 ---
const tableData = ref<any[]>([])

// --- 日期列（当月所有日期） ---
const dayColumns = computed(() => {
  if (!queryParams.value.yearMonth) return []
  const base = dayjs(queryParams.value.yearMonth + '-01')
  const daysInMonth = base.daysInMonth()
  const cols: string[] = []
  for (let d = 1; d <= daysInMonth; d++) {
    cols.push(base.date(d).format('YYYY-MM-DD'))
  }
  return cols
})

// --- 平铺行（项目行展开成多个成员行，供 span-method 合并项目列） ---
const flatRows = computed(() => {
  const rows: any[] = []
  for (const project of tableData.value) {
    const members = project.members || []
    // 汇总项目组所有成员工时 → 项目实际人天
    const totalProjectHours = members.reduce((sum: number, m: any) => sum + Number(m.totalHours || 0), 0)
    const projectActualDays = totalProjectHours / 8
    const estimatedWorkload = Number(project.estimatedWorkload || 0)

    members.forEach((member: any, idx: number) => {
      rows.push({
        projectId: project.projectId,
        projectName: project.projectName,
        hasContract: project.hasContract,
        estimatedWorkload,
        projectActualDays,
        memberCount: members.length,
        memberIndex: idx,
        userId: member.userId,
        nickName: member.nickName,
        deptName: member.deptName,
        dailyHours: member.dailyHours || {},
        totalHours: member.totalHours
      })
    })
    // 若项目无成员也显示占位行
    if (members.length === 0) {
      rows.push({
        projectId: project.projectId,
        projectName: project.projectName,
        hasContract: project.hasContract,
        estimatedWorkload,
        projectActualDays: 0,
        memberCount: 0,
        memberIndex: 0,
        nickName: '—',
        dailyHours: {},
        totalHours: null
      })
    }
  }
  return rows
})

// --- 合并项目列 ---
// 列索引：0=项目名, 1=人员, 2..N+1=日期, N+2=个人人天, N+3=实际人天(汇总), N+4=预算人天
function spanMethod({ row, columnIndex }: any) {
  const actualColIndex = 2 + dayColumns.value.length + 1
  const budgetColIndex = actualColIndex + 1
  if (columnIndex === 0 || columnIndex === actualColIndex || columnIndex === budgetColIndex) {
    if (row.memberIndex === 0) {
      return { rowspan: Math.max(row.memberCount, 1), colspan: 1 }
    } else {
      return { rowspan: 0, colspan: 0 }
    }
  }
}

// --- 行样式 ---
function rowClassName({ row }: any) {
  return row.hasContract ? 'contract-row' : ''
}

// --- 格式化人天 ---
function formatDays(val: any) {
  if (val == null) return '—'
  return Number(val).toFixed(2) + 'd'
}

// --- 查询 ---
async function handleQuery() {
  if (!queryParams.value.deptId) {
    ElMessage.warning('请先选择项目部门')
    return
  }
  if (!queryParams.value.yearMonth) {
    ElMessage.warning('请选择年月')
    return
  }
  loading.value = true
  try {
    const res = await getTeamMonthly(queryParams.value)
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

// --- 部门切换时重置项目筛选 ---
function handleDeptChange() {
  projectKeyword.value = ''
  queryParams.value.projectId = undefined
}

// --- 项目 autocomplete ---
let debounceTimer: any = null
function fetchProjectSuggestions(query: string, cb: Function) {
  if (!query) { cb([]); return }
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    try {
      const res = await getTeamProjectOptions({
        deptId: queryParams.value.deptId,
        projectName: query
      })
      cb((res.data || []).map((item: any) => ({ ...item, value: item.projectName })))
    } catch {
      cb([])
    }
  }, 300)
}

function handleProjectSelect(item: any) {
  queryParams.value.projectId = item.projectId
  handleQuery()
}

function handleProjectClear() {
  queryParams.value.projectId = undefined
  handleQuery()
}

// --- 重置 ---
function resetQuery() {
  queryRef.value?.resetFields()
  projectKeyword.value = ''
  queryParams.value.deptId = undefined
  queryParams.value.projectId = undefined
  queryParams.value.yearMonth = dayjs().format('YYYY-MM')
  tableData.value = []
}

onMounted(() => {
  // 默认不自动查询，等用户选择部门
})
</script>

<style scoped>
.legend-bar {
  display: flex;
  gap: 24px;
  align-items: center;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.legend-dot {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 3px;
  flex-shrink: 0;
}
.contract-dot { background: #95d475; }
.warn-dot     { background: #f56c6c; }

.contract-row {
  background-color: #f0f9eb !important;
}
.contract-label,
.project-label {
  display: block;
  white-space: normal;
  word-break: break-all;
  line-height: 1.4;
}
.contract-label {
  color: #67c23a;
  font-weight: 500;
}
.hours-badge {
  display: inline-block;
  font-size: 12px;
  color: #606266;
}
.warn-text {
  color: #f56c6c;
  display: flex;
  align-items: center;
  gap: 2px;
}
</style>
