<template>
  <div class="app-container daily-report-page">
    <el-row :gutter="16">
      <!-- 左侧日历 -->
      <el-col :span="7">
        <el-card shadow="hover">
          <MonthCalendar
            v-model="selectedDate"
            :work-calendar-map="workCalendarMap"
            @month-change="handleMonthChange"
            @year-change="handleYearChange"
          >
            <template #default="{ day, dateStr, isToday, dayType }">
              <div class="cal-cell">
                <span class="mc-day-num" :class="getDayBadgeClass(dateStr, dayType, isToday)">{{ day }}</span>
                <span v-if="getDateHours(dateStr)" class="cal-hours" :class="getHoursClass(dateStr)">
                  {{ getDateHours(dateStr) }}h
                </span>
              </div>
            </template>
          </MonthCalendar>
        </el-card>
      </el-col>

      <!-- 右侧编辑区 -->
      <el-col :span="17">
        <el-card shadow="hover">
          <template #header>
            <div style="display: flex; align-items: center; justify-content: space-between;">
              <div>
                <span style="font-size: 16px; font-weight: bold;">{{ formatDate(selectedDate) }}</span>
                <el-tag v-if="totalHours > 0" :type="totalHours >= 8 ? 'success' : 'warning'" style="margin-left: 12px;">
                  总工时: {{ totalHours }}h
                </el-tag>
              </div>
              <el-button type="primary" @click="handleSave" :loading="saving">保存日报</el-button>
            </div>
          </template>

          <div v-if="loading" style="text-align: center; padding: 40px;">
            <el-icon class="is-loading" :size="24"><Loading /></el-icon>
            <p style="color: #909399; margin-top: 8px;">加载中...</p>
          </div>

          <div v-else-if="projects.length === 0" style="text-align: center; padding: 60px; color: #909399;">
            <p>暂无参与的项目</p>
            <p style="font-size: 12px;">请联系项目经理将您添加为项目成员</p>
          </div>

          <div v-else class="project-list">
            <div v-for="(item, index) in formList" :key="item.projectId" class="project-item">
              <!-- 第一行：项目名 + 阶段 -->
              <div class="prj-header">
                <div class="prj-color-bar" :style="{ background: getColor(index) }"></div>
                <span class="prj-name">{{ item.projectName }}</span>
                <el-tag size="small" type="info">{{ item.projectStageName || '未设置' }}</el-tag>
              </div>

              <!-- 第二行：工时（slider + 输入框） -->
              <div class="prj-hours-row">
                <span class="hours-label">工时:</span>
                <el-slider
                  v-model="item.workHours"
                  :min="0" :max="24" :step="1"
                  :marks="{ 0: '0', 8: '8h', 16: '16h', 24: '24h' }"
                  style="flex: 1; margin: 0 16px;"
                />
                <el-input-number
                  v-model="item.workHours"
                  :min="0" :max="24" :step="1" :precision="0"
                  size="small" style="width: 100px;"
                />
                <span style="margin-left: 4px; color: #909399;">h</span>
              </div>

              <!-- 第三行：工作内容 -->
              <div class="prj-content-row">
                <el-input
                  v-model="item.workContent"
                  type="textarea"
                  :rows="2"
                  :placeholder="'填写 ' + item.projectName + ' 的工作内容...'"
                  maxlength="2000"
                  show-word-limit
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getMyReport, getMyProjects, saveDailyReport, listDailyReport } from '@/api/project/dailyReport'
import { getWorkCalendarByYear } from '@/api/project/workCalendar'
import MonthCalendar from '@/components/MonthCalendar/index.vue'
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()

function formatDateStr(date) {
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const selectedDate = ref(formatDateStr(new Date()))
const currentYearMonth = ref((() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
})())
const projects = ref([])
const formList = ref([])
const loading = ref(false)
const saving = ref(false)
const monthReports = ref({}) // { 'yyyy-MM-dd': totalHours }
const workCalendarMap = ref({}) // { 'yyyy-MM-dd': { dayType, dayName } }

const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb', '#00b894', '#fdcb6e']

const totalHours = computed(() => {
  return formList.value.reduce((sum, item) => sum + (item.workHours || 0), 0)
})

function formatDate(dateStr) {
  const d = new Date(dateStr)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${dateStr} ${weekdays[d.getDay()]}`
}

function getColor(index) {
  return colors[index % colors.length]
}

// 日期徽章 class（结合工作日历 + 日报数据）
function getDayBadgeClass(dateStr, dayType, isToday) {
  if (isToday) return 'mc-day-today'

  const hours = monthReports.value[dateStr]
  const wc = workCalendarMap.value[dateStr]

  if (wc?.dayType === 'holiday') {
    return hours ? 'badge-ok' : 'mc-day-holiday'
  }
  if (wc?.dayType === 'workday') {
    if (hours >= 8) return 'badge-ok'
    if (hours > 0) return 'badge-warn'
    return 'mc-day-workday'
  }
  if (dayType === 'weekend') {
    return hours ? 'badge-ok' : 'mc-day-weekend'
  }
  // 普通工作日
  if (!hours) return 'mc-day-normal'
  if (hours >= 8) return 'badge-ok'
  return 'badge-warn'
}

function getDateHours(dateStr) {
  return monthReports.value[dateStr]
}

function getHoursClass(dateStr) {
  const hours = monthReports.value[dateStr]
  if (!hours) return ''
  if (hours > 8) return 'hours-over'
  if (hours >= 8) return 'hours-ok'
  return 'hours-warn'
}

// 加载参与的项目列表
async function loadProjects() {
  const res = await getMyProjects()
  projects.value = res.data || []
}

// 加载某日的日报数据
async function loadDayReport(dateStr) {
  loading.value = true
  try {
    const res = await getMyReport(dateStr)
    const report = res.data

    // 构建表单：所有项目列出，已有数据的填充
    formList.value = projects.value.map(p => {
      const detail = report?.detailList?.find(d => d.projectId === p.projectId)
      return {
        projectId: p.projectId,
        projectName: p.projectName,
        projectCode: p.projectCode,
        projectStage: p.projectStage,
        projectStageName: p.projectStageName,
        workHours: detail ? Number(detail.workHours) : 0,
        workContent: detail ? detail.workContent : ''
      }
    })
  } finally {
    loading.value = false
  }
}

// 加载月度概览（日历标注用）
async function loadMonthOverview() {
  const res = await listDailyReport({ yearMonth: currentYearMonth.value, userId: userStore.id })
  const map = {}
  if (res.rows) {
    res.rows.forEach(r => {
      const day = r.reportDate?.substring(0, 10)
      if (day) map[day] = Number(r.totalWorkHours)
    })
  }
  monthReports.value = map
}

// 加载工作日历
async function loadWorkCalendar(year) {
  const y = year || Number(currentYearMonth.value.split('-')[0])
  const res = await getWorkCalendarByYear(y)
  const map = {}
  if (res.data) {
    res.data.forEach(item => {
      const dateStr = item.calendarDate?.substring(0, 10)
      if (dateStr) map[dateStr] = item
    })
  }
  workCalendarMap.value = map
}

// MonthCalendar 事件
function handleMonthChange({ yearMonth }) {
  currentYearMonth.value = yearMonth
  loadMonthOverview()
}

function handleYearChange(year) {
  loadWorkCalendar(year)
}

// 选择日期变化时加载日报
watch(selectedDate, (newVal) => {
  loadDayReport(newVal)
})

async function handleSave() {
  // 过滤有效条目（工时>0 且内容非空）
  const details = formList.value
    .filter(f => f.workHours > 0 && f.workContent && f.workContent.trim())
    .map(f => ({
      projectId: f.projectId,
      projectStage: f.projectStage,
      workHours: f.workHours,
      workContent: f.workContent
    }))

  if (details.length === 0) {
    ElMessage.warning('请至少填写一个项目的工时和工作内容')
    return
  }

  saving.value = true
  try {
    await saveDailyReport({
      reportDate: selectedDate.value,
      detailList: details
    })
    ElMessage.success('日报保存成功')
    loadMonthOverview()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadWorkCalendar()
  await loadProjects()
  await loadDayReport(selectedDate.value)
  await loadMonthOverview()
})
</script>

<style scoped>
.daily-report-page {
  height: calc(100vh - 84px);
}

/* ===== 日历格子内容 ===== */
.cal-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2px 0;
}

/* 已填报徽章（自定义颜色，覆盖全局 mc-day-* ） */
.badge-ok { color: #fff; background: #00b42a; }
.badge-warn { color: #fff; background: #ff7d00; }

/* 工时标注 */
.cal-hours { font-size: 11px; font-weight: 700; margin-top: 3px; }
.cal-hours.hours-ok { color: #00b42a; }
.cal-hours.hours-warn { color: #ff7d00; }
.cal-hours.hours-over { color: #409eff; }

/* 项目列表 */
.project-list { display: flex; flex-direction: column; gap: 16px; }

.project-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 16px;
  transition: box-shadow 0.2s;
}
.project-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }

.prj-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.prj-color-bar { width: 4px; height: 20px; border-radius: 2px; }
.prj-name { font-size: 15px; font-weight: 600; }

.prj-hours-row {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.hours-label { font-size: 13px; color: #606266; white-space: nowrap; }

.prj-content-row {}
</style>
