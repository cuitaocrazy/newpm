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
        <project-dept-select
          v-model="queryParams.deptId"
          style="width: 200px"
          placeholder="全部部门"
          clearable
          @change="handleQuery"
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

    <!-- 按周分组表格 -->
    <div v-loading="loading">
      <template v-for="(week, wi) in displayedWeeks" :key="wi">
        <div class="week-header">
          第 {{ week.weekNum }} 周（{{ week.startDate }} ～ {{ week.endDate }}）
        </div>
        <el-table :data="week.days" border style="width: 100%; margin-bottom: 16px">
          <el-table-column label="日期" prop="reportDate" width="120" />
          <el-table-column label="星期" prop="dayOfWeek" width="80" />
          <el-table-column label="已提交" width="120">
            <template #default="{ row }">
              <el-button
                v-if="row.submittedCount > 0"
                link
                type="primary"
                @click="openDetail(row.reportDate, 'submitted')"
              >{{ row.submittedCount }} 人</el-button>
              <span v-else class="text-muted">0 人</span>
            </template>
          </el-table-column>
          <el-table-column label="未提交">
            <template #default="{ row }">
              <template v-if="!row.isWorkday">
                <span class="non-workday-label">非工作日</span>
              </template>
              <template v-else>
                <el-button
                  v-if="row.unsubmittedCount > 0"
                  link
                  type="danger"
                  @click="openDetail(row.reportDate, 'unsubmitted')"
                >{{ row.unsubmittedCount }} 人</el-button>
                <span v-else class="text-muted">0 人</span>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-if="!loading && displayedWeeks.length === 0" description="暂无数据" />
    </div>

    <!-- 人员明细弹框 -->
    <el-dialog
      v-model="detailVisible"
      :title="detailTitle"
      width="600px"
      append-to-body
    >
      <el-table :data="detailList" v-loading="detailLoading" border>
        <el-table-column label="姓名" prop="nickName" min-width="100" />
        <el-table-column label="部门" prop="deptName" min-width="150" />
        <template v-if="detailType === 'submitted'">
          <el-table-column label="工时(小时)" prop="totalWorkHours" width="100" />
          <el-table-column label="工作内容摘要" prop="workContentSummary" min-width="200" show-overflow-tooltip />
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
import { getWeeklyStats, getWeeklyStatsDetail, exportWeeklyStats } from '@/api/project/dailyReport'

dayjs.extend(isoWeek)

interface DailySubmissionStat {
  reportDate: string
  dayOfWeek: string
  isWorkday: boolean
  submittedCount: number
  unsubmittedCount: number
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
  days: DailySubmissionStat[]
}

const loading = ref(false)
const exporting = ref(false)
const statList = ref<DailySubmissionStat[]>([])

const queryParams = reactive({
  yearMonth: dayjs().format('YYYY-MM'),
  deptId: null as number | null
})

// 周次选项
const selectedWeek = ref<number | null>(null)
const weekOptions = ref<WeekOption[]>([])

function computeWeekOptions(yearMonth: string): WeekOption[] {
  const firstDay = dayjs(yearMonth + '-01')
  const lastDay = firstDay.endOf('month')
  const weeks: WeekOption[] = []
  let current = firstDay
  let weekNum = 1
  while (current.isBefore(lastDay) || current.isSame(lastDay, 'day')) {
    const weekStart = current.startOf('isoWeek')
    const weekEnd = current.endOf('isoWeek')
    const clampedStart = weekStart.isBefore(firstDay) ? firstDay : weekStart
    const clampedEnd = weekEnd.isAfter(lastDay) ? lastDay : weekEnd
    weeks.push({
      value: weekNum,
      label: `第${weekNum}周（${clampedStart.format('MM-DD')}～${clampedEnd.format('MM-DD')}）`,
      startDate: clampedStart.format('YYYY-MM-DD'),
      endDate: clampedEnd.format('YYYY-MM-DD')
    })
    current = weekEnd.add(1, 'day')
    weekNum++
  }
  return weeks
}

// 将后端数组按周分组
function groupByWeeks(stats: DailySubmissionStat[], options: WeekOption[]): WeekGroup[] {
  return options.map(opt => ({
    weekNum: opt.value,
    startDate: opt.startDate,
    endDate: opt.endDate,
    days: stats.filter(s => s.reportDate >= opt.startDate && s.reportDate <= opt.endDate)
  }))
}

const allWeeks = computed<WeekGroup[]>(() =>
  groupByWeeks(statList.value, weekOptions.value)
)

const displayedWeeks = computed<WeekGroup[]>(() => {
  if (!selectedWeek.value) return allWeeks.value
  return allWeeks.value.filter(w => w.weekNum === selectedWeek.value)
})

async function loadStats() {
  loading.value = true
  try {
    const res = await getWeeklyStats(queryParams)
    statList.value = res.data || []
  } finally {
    loading.value = false
  }
}

function handleMonthChange() {
  weekOptions.value = computeWeekOptions(queryParams.yearMonth)
  selectedWeek.value = null
  loadStats()
}

function handleWeekChange() {
  // 纯前端过滤，无需重新请求
}

function handleQuery() {
  loadStats()
}

function resetQuery() {
  queryParams.yearMonth = dayjs().format('YYYY-MM')
  queryParams.deptId = null
  selectedWeek.value = null
  weekOptions.value = computeWeekOptions(queryParams.yearMonth)
  loadStats()
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
    const res = await getWeeklyStatsDetail({
      reportDate: date,
      type,
      deptId: queryParams.deptId
    })
    detailList.value = res.data || []
  } finally {
    detailLoading.value = false
  }
}

// 导出
async function handleExport() {
  exporting.value = true
  try {
    const res = await exportWeeklyStats(queryParams)
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
  loadStats()
})
</script>

<style scoped>
.week-header {
  font-weight: 600;
  font-size: 14px;
  padding: 6px 0 4px;
  color: #333;
}
.non-workday-label {
  color: #aaa;
  font-size: 12px;
}
.text-muted {
  color: #ccc;
}
:deep(.el-table tr.non-workday td) {
  background: #f5f5f5;
  color: #aaa;
}
</style>
