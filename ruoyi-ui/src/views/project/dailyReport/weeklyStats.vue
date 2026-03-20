<template>
  <div class="app-container">
    <!-- 查询栏 -->
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="月份">
        <el-date-picker
          v-model="queryParams.yearMonth"
          type="month"
          value-format="YYYY-MM"
          placeholder="选择月份"
          style="width: 150px"
          @change="handleMonthChange"
        />
      </el-form-item>
      <el-form-item label="周次">
        <el-select
          v-model="selectedWeek"
          style="width: 200px"
          placeholder="全部周次"
          clearable
          @change="handleWeekChange"
        >
          <el-option
            v-for="w in weekOptions"
            :key="w.value"
            :label="w.label"
            :value="w.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="部门">
        <el-tree-select
          v-model="queryParams.deptId"
          :data="deptOptions"
          :props="treeProps"
          check-strictly
          clearable
          placeholder="全部部门"
          style="width: 220px"
          node-key="deptId"
          :render-after-expand="false"
          @change="handleQuery"
        />
      </el-form-item>
      <el-form-item label="项目名称">
        <el-autocomplete
          v-model="queryParams.projectName"
          :fetch-suggestions="fetchProjectSuggestions"
          placeholder="输入关键字搜索，或直接选择下拉数据"
          :trigger-on-focus="true"
          clearable
          style="width: 200px"
          @select="handleQuery"
          @clear="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button
          v-hasPermi="['project:dailyReport:weeklyStatsExport']"
          type="success"
          icon="Download"
          :loading="exporting"
          @click="handleExport"
        >导出 Excel</el-button>
      </el-form-item>
    </el-form>

    <!-- 总人数提示 -->
    <div v-if="!loading" class="stats-summary">
      需要提交日报人数：<strong>{{ totalUsers }}</strong> 人
    </div>

    <!-- 双列周卡片网格（倒序：最新周在前） -->
    <div v-loading="loading" class="week-grid">
      <div
        v-for="(week, wi) in displayedWeeks"
        :key="wi"
        class="week-card"
        :class="{ 'current-week': week.isCurrent }"
      >
        <div class="week-card-header">
          <span class="week-title">
            第 {{ week.weekNum }} 周
            <span class="week-range">{{ week.startDate }} ～ {{ week.endDate }}</span>
          </span>
          <el-tag v-if="week.isCurrent" size="small" type="warning">本周</el-tag>
        </div>
        <el-table :data="week.days" border size="small" class="week-table">
          <el-table-column label="日期" prop="reportDate" width="100" />
          <el-table-column label="周" prop="dayOfWeek" width="46" align="center" />
          <el-table-column label="已提交" width="82" align="center">
            <template #default="{ row }">
              <span v-if="row.isFuture" class="dash">—</span>
              <el-button
                v-else-if="row.submittedCount > 0"
                link type="primary"
                @click="openDetail(row.reportDate, 'submitted')"
              >{{ row.submittedCount }} 人</el-button>
              <span v-else class="text-muted">0 人</span>
            </template>
          </el-table-column>
          <el-table-column label="未提交" align="center">
            <template #default="{ row }">
              <span v-if="!row.isWorkday" class="non-workday">非工作日</span>
              <span v-else-if="row.isFuture" class="dash">—</span>
              <el-button
                v-else-if="row.unsubmittedCount > 0"
                link type="danger"
                @click="openDetail(row.reportDate, 'unsubmitted')"
              >{{ row.unsubmittedCount }} 人</el-button>
              <span v-else class="all-submitted">全部已填</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-if="!loading && displayedWeeks.length === 0" description="暂无数据" class="grid-empty" />
    </div>

    <!-- 人员明细弹框 -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="900px" append-to-body>
      <el-table :data="detailList" v-loading="detailLoading" border size="small">
        <el-table-column label="姓名" prop="nickName" min-width="90" />
        <el-table-column label="部门" prop="deptName" min-width="130" />
        <template v-if="detailType === 'submitted'">
          <el-table-column label="项目名称" prop="projectNames" min-width="160">
            <template #default="{ row }">
              <div v-for="(name, i) in (row.projectNames || '').split('、').filter(Boolean)" :key="i" class="multiline-item">{{ name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="工时(h)" prop="totalWorkHours" width="82" align="center">
            <template #default="{ row }">
              <span :class="hoursClass(row.totalWorkHours)">{{ row.totalWorkHours }}</span>
            </template>
          </el-table-column>
          <el-table-column label="工作内容摘要" prop="workContentSummary" min-width="200">
            <template #default="{ row }">
              <div v-for="(line, i) in (row.workContentSummary || '').split('；').filter(Boolean)" :key="i" class="multiline-item">{{ line }}</div>
            </template>
          </el-table-column>
        </template>
      </el-table>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import isoWeek from 'dayjs/plugin/isoWeek'
import { ElMessage } from 'element-plus'
import { handleTree } from '@/utils/ruoyi'
import {
  getWeeklyStats,
  getWeeklyStatsDetail,
  exportWeeklyStats,
  getWeeklyStatsDeptTree
} from '@/api/project/dailyReport'
import { searchProjects } from '@/api/project/project'

dayjs.extend(isoWeek)

interface DailySubmissionStat {
  reportDate: string
  dayOfWeek: string
  isWorkday: boolean
  isFuture: boolean
  submittedCount: number | null
  unsubmittedCount: number | null
}

interface WeekOption {
  value: number
  label: string
  startDate: string
  endDate: string
}

interface WeekGroup {
  weekNum: number
  startDate: string
  endDate: string
  isCurrent: boolean
  days: DailySubmissionStat[]
}

const loading = ref(false)
const exporting = ref(false)
const statList = ref<DailySubmissionStat[]>([])
const totalUsers = ref(0)
const deptOptions = ref<any[]>([])
const treeProps = { label: 'deptName', value: 'deptId', children: 'children' }

const queryParams = reactive({
  yearMonth: dayjs().format('YYYY-MM'),
  deptId: undefined as number | undefined,
  projectName: ''
})

const selectedWeek = ref<number | null>(null)
const weekOptions = ref<WeekOption[]>([])

function computeWeekOptions(yearMonth: string): WeekOption[] {
  const firstDay = dayjs(yearMonth + '-01')
  const lastDay = firstDay.endOf('month')
  const today = dayjs()
  const weeks: WeekOption[] = []
  let current = firstDay
  let weekNum = 1
  while (current.isBefore(lastDay) || current.isSame(lastDay, 'day')) {
    const weekStart = current.startOf('isoWeek')
    const weekEnd = current.endOf('isoWeek')
    const clampedStart = weekStart.isBefore(firstDay) ? firstDay : weekStart
    const clampedEnd = weekEnd.isAfter(lastDay) ? lastDay : weekEnd
    // 只加入已开始的周
    if (!today.isBefore(dayjs(clampedStart.format('YYYY-MM-DD')))) {
      weeks.push({
        value: weekNum,
        label: `第${weekNum}周（${clampedStart.format('MM-DD')}～${clampedEnd.format('MM-DD')}）`,
        startDate: clampedStart.format('YYYY-MM-DD'),
        endDate: clampedEnd.format('YYYY-MM-DD')
      })
    }
    current = weekEnd.add(1, 'day')
    weekNum++
  }
  return weeks
}

function groupByWeeks(stats: DailySubmissionStat[], options: WeekOption[]): WeekGroup[] {
  const today = dayjs()
  return options.map(opt => ({
    weekNum: opt.value,
    startDate: opt.startDate,
    endDate: opt.endDate,
    isCurrent: !today.isBefore(dayjs(opt.startDate)) && !today.isAfter(dayjs(opt.endDate)),
    days: stats.filter(s => s.reportDate >= opt.startDate && s.reportDate <= opt.endDate)
  }))
}

const allWeeks = computed<WeekGroup[]>(() => groupByWeeks(statList.value, weekOptions.value))

// 只保留已开始的周（startDate <= 今天）
const startedWeeks = computed<WeekGroup[]>(() => {
  const today = dayjs().format('YYYY-MM-DD')
  return allWeeks.value.filter(w => w.startDate <= today)
})

// 倒序展示（最新周在前）；按周筛选时保持正序
const displayedWeeks = computed<WeekGroup[]>(() => {
  if (selectedWeek.value) return startedWeeks.value.filter(w => w.weekNum === selectedWeek.value)
  return [...startedWeeks.value].reverse()
})

async function loadStats() {
  loading.value = true
  try {
    const params: any = {
      yearMonth: queryParams.yearMonth,
      projectName: queryParams.projectName || undefined,
      deptId: queryParams.deptId || undefined
    }
    const res = await getWeeklyStats(params)
    statList.value = res.data?.list || []
    totalUsers.value = res.data?.totalUsers ?? 0
  } finally {
    loading.value = false
  }
}

async function loadDeptTree() {
  const res = await getWeeklyStatsDeptTree()
  deptOptions.value = handleTree(res.data || [], 'deptId', 'parentId')
}

async function fetchProjectSuggestions(keyword: string, cb: (suggestions: any[]) => void) {
  try {
    const res = await searchProjects(keyword || '')
    cb((res.data || []).map((p: any) => ({ value: p.projectName })))
  } catch {
    cb([])
  }
}

function handleMonthChange() {
  weekOptions.value = computeWeekOptions(queryParams.yearMonth)
  selectedWeek.value = null
  loadStats()
}

function handleWeekChange() { /* 纯前端过滤 */ }

function handleQuery() { loadStats() }

function resetQuery() {
  queryParams.yearMonth = dayjs().format('YYYY-MM')
  queryParams.deptId = undefined
  queryParams.projectName = ''
  selectedWeek.value = null
  weekOptions.value = computeWeekOptions(queryParams.yearMonth)
  loadStats()
}

// 工时颜色：< 8h 淡红色背景，>= 8h 默认样式
function hoursClass(hours: number | null): string {
  if (hours == null) return ''
  const h = Number(hours)
  if (h < 8) return 'hours-low'
  return ''
}

// 明细弹框
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailTitle = ref('')
const detailType = ref<'submitted' | 'unsubmitted'>('submitted')
const detailList = ref<any[]>([])

async function openDetail(date: string, type: 'submitted' | 'unsubmitted') {
  detailType.value = type
  detailTitle.value = `${date} ${type === 'submitted' ? '已提交' : '未提交'}人员明细`
  detailVisible.value = true
  detailLoading.value = true
  detailList.value = []
  try {
    const params: any = {
      reportDate: date,
      type,
      deptId: queryParams.deptId || undefined,
      projectName: queryParams.projectName || undefined
    }
    const res = await getWeeklyStatsDetail(params)
    detailList.value = res.data || []
  } finally {
    detailLoading.value = false
  }
}

// 导出
async function handleExport() {
  exporting.value = true
  try {
    const params: any = {
      yearMonth: queryParams.yearMonth,
      projectName: queryParams.projectName || undefined,
      deptId: queryParams.deptId || undefined
    }
    const res = await exportWeeklyStats(params)
    const url = URL.createObjectURL(new Blob([res]))
    const a = document.createElement('a')
    a.href = url
    a.download = `日报统计报表_${queryParams.yearMonth}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  weekOptions.value = computeWeekOptions(queryParams.yearMonth)
  loadDeptTree()
  loadStats()
})
</script>

<style scoped>
.multiline-item {
  line-height: 1.5;
  padding: 1px 0;
  word-break: break-all;
}
.multiline-item + .multiline-item {
  border-top: 1px dashed #e4e7ed;
  margin-top: 3px;
  padding-top: 3px;
}
/* 总人数统计行 */
.stats-summary {
  margin-bottom: 10px;
  font-size: 13px;
  color: #606266;
}
.stats-summary strong {
  color: #303133;
  font-size: 15px;
}

/* 双列网格 */
.week-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.grid-empty {
  grid-column: 1 / -1;
}

/* 周卡片 */
.week-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}
.current-week {
  border-color: #e6a23c;
  box-shadow: 0 0 0 2px rgba(230, 162, 60, 0.15);
}

/* 卡片标题 */
.week-card-header {
  background: #f5f7fa;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.current-week .week-card-header {
  background: #fdf6ec;
}
.week-title {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
}
.current-week .week-title {
  color: #e6a23c;
}
.week-range {
  font-weight: 400;
  font-size: 12px;
  color: #909399;
  margin-left: 6px;
}

.week-table { width: 100%; }

/* 状态文字 */
.non-workday { color: #c0c4cc; font-size: 12px; }
.dash        { color: #c0c4cc; font-size: 14px; }
.text-muted  { color: #c0c4cc; }
.all-submitted { color: #67c23a; font-size: 12px; }

/* 工时颜色 */
.hours-low  { background-color: #ffecec; border-radius: 4px; padding: 2px 6px; }
</style>
