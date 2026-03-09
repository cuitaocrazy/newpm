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
                <span v-if="getDateWorkHours(dateStr)" class="cal-hours" :class="getHoursClass(dateStr)">
                  {{ getDateWorkHours(dateStr) }}h
                </span>
                <span v-for="le in getDateLeaveEntries(dateStr)" :key="le.type"
                  class="cal-leave-badge"
                  :style="{ color: LEAVE_TYPE_COLOR[le.type] }">
                  {{ LEAVE_TYPE_LABEL[le.type] }}{{ le.hours }}h
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
              <div style="display: flex; gap: 8px; align-items: center;">
                <el-button type="primary" @click="handleSave" :disabled="saving || !isEditable">保存日报</el-button>
                <el-button type="danger" plain @click="handleDelete" :disabled="!currentReportId">删除日报</el-button>
                <span v-if="!isEditable" style="font-size: 12px; color: #f56c6c;">仅限本周（周一至周日）可录入</span>
              </div>
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
            <!-- 假期记录区块 -->
            <div class="leave-section">
              <div class="leave-header">
                <span class="leave-title">假期记录</span>
                <el-button
                  type="primary" size="small" plain
                  :disabled="!isEditable"
                  @click="leaveList.push({ entryType: 'leave', leaveHours: 1, remark: '' })"
                >+ 添加假期</el-button>
              </div>
              <div v-if="leaveList.length === 0" class="leave-empty">暂无假期记录</div>
              <div v-for="(item, idx) in leaveList" :key="idx" class="leave-item">
                <span class="leave-color-dot" :style="{ background: LEAVE_TYPE_COLOR[item.entryType] || '#ccc' }"></span>
                <el-select v-model="item.entryType" size="small" style="width: 90px;" :disabled="!isEditable">
                  <el-option label="请假" value="leave" />
                  <el-option label="倒休" value="comp" />
                  <el-option label="年假" value="annual" />
                </el-select>
                <el-input-number
                  v-model="item.leaveHours"
                  :min="0.5" :max="24" :step="0.5" :precision="1"
                  size="small" style="width: 120px;"
                  :disabled="!isEditable"
                />
                <span style="color: #909399; font-size: 13px;">小时</span>
                <el-input
                  v-model="item.remark"
                  placeholder="备注(选填)"
                  size="small"
                  style="flex: 1;"
                  :disabled="!isEditable"
                />
                <el-button link type="danger" icon="Delete" :disabled="!isEditable"
                  @click="leaveList.splice(idx, 1)" />
              </div>
            </div>

            <div v-for="(item, index) in formList" :key="item.projectId" class="project-item">
              <!-- 第一行：项目名 + 阶段 -->
              <div class="prj-header">
                <div class="prj-color-bar" :style="{ background: getColor(index) }"></div>
                <el-tag v-if="item.revenueConfirmYear" size="small" type="warning" style="flex-shrink:0;">
                  {{ getDictLabel(sys_ndgl, item.revenueConfirmYear) }}
                </el-tag>
                <span class="prj-name">{{ item.projectName }}</span>
                <span class="prj-workload-info">
                  预计人天：<strong>{{ item.estimatedWorkload != null ? item.estimatedWorkload : '-' }}</strong>
                  &nbsp;&nbsp;已花人天：<strong>{{ item.actualWorkload != null ? Number(item.actualWorkload).toFixed(3) : '-' }}</strong>
                </span>
                <el-tag size="small" type="info" style="margin-left: auto; flex-shrink: 0;">{{ item.projectStageName || '未设置' }}</el-tag>
              </div>

              <!-- 第二行：任务 + 工作任务类别（仅有子项目的主项目显示） -->
              <div v-if="item.hasSubProject" class="prj-sub-row">
                <span class="sub-label">任务:</span>
                <el-select
                  v-model="item.subProjectId"
                  placeholder="请选择任务（可选）"
                  clearable
                  size="small"
                  style="flex: 1; min-width: 0;"
                  :disabled="!isEditable"
                  @focus="loadSubProjectOptions(item)"
                >
                  <el-option
                    v-for="sp in (item.subProjectOptions || [])"
                    :key="sp.projectId"
                    :label="sp.taskCode ? `[${sp.taskCode}] ${sp.projectName}` : sp.projectName"
                    :value="sp.projectId"
                  />
                </el-select>
                <span class="sub-label" style="margin-left: 12px;">工作任务类别:</span>
                <el-select
                  v-model="item.workCategory"
                  placeholder="请选择"
                  clearable
                  size="small"
                  style="width: 140px;"
                  :disabled="!isEditable"
                >
                  <el-option v-for="d in sys_gzlb" :key="d.value" :label="d.label" :value="d.value" />
                </el-select>
              </div>

              <!-- 第三行：工时（slider + 输入框） -->
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

              <!-- 第四行：工作内容 -->
              <div class="prj-content-row">
                <el-input
                  v-model="item.workContent"
                  type="textarea"
                  :rows="5"
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
import { ref, onMounted, computed, watch, getCurrentInstance } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getMyReport, getMyProjects, saveDailyReport, listDailyReport, delDailyReport } from '@/api/project/dailyReport'
import { getWorkCalendarByYear } from '@/api/project/workCalendar'
import MonthCalendar from '@/components/MonthCalendar/index.vue'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const { sys_ndgl, sys_gzlb } = proxy.useDict('sys_ndgl', 'sys_gzlb')

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
const currentReportId = ref(null)
const loading = ref(false)
const saving = ref(false)
const monthReports = ref({}) // { 'yyyy-MM-dd': { workHours, leaveSummary } }
const workCalendarMap = ref({}) // { 'yyyy-MM-dd': { dayType, dayName } }

const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb', '#00b894', '#fdcb6e']

// 假期类型颜色（不与现有绿/橙冲突）
const LEAVE_TYPE_COLOR = { leave: '#f56c6c', comp: '#b37feb', annual: '#36cfc9' }
const LEAVE_TYPE_LABEL = { leave: '请假', comp: '倒休', annual: '年假' }

// 假期记录列表（当天）
const leaveList = ref([])

const totalHours = computed(() => {
  return formList.value.reduce((sum, item) => sum + (item.workHours || 0), 0)
})

// 本周范围（周一至周日），仅本周日期可保存
const weekBounds = (() => {
  const today = new Date()
  const dow = today.getDay() // 0=周日,1=周一,...,6=周六
  const diffToMonday = dow === 0 ? -6 : 1 - dow
  const monday = new Date(today)
  monday.setDate(today.getDate() + diffToMonday)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return { start: formatDateStr(monday), end: formatDateStr(sunday) }
})()

const isEditable = computed(() => {
  return selectedDate.value >= weekBounds.start && selectedDate.value <= weekBounds.end
})

function formatDate(dateStr) {
  const d = new Date(dateStr)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${dateStr} ${weekdays[d.getDay()]}`
}

function getColor(index) {
  return colors[index % colors.length]
}

function getDictLabel(dictList, value) {
  return dictList?.find(d => d.value == value)?.label || value
}

// 兼容旧格式（纯数字）和新格式（对象）获取工时
function getDateWorkHours(dateStr) {
  const data = monthReports.value[dateStr]
  if (!data) return 0
  return typeof data === 'object' ? (data.workHours || 0) : (Number(data) || 0)
}

// 解析 leaveSummary 为数组：[{type:'leave', hours:2}, ...]
function getDateLeaveEntries(dateStr) {
  const data = monthReports.value[dateStr]
  const summary = typeof data === 'object' ? data.leaveSummary : ''
  if (!summary) return []
  return summary.split(',').map(seg => {
    const [type, hours] = seg.split(':')
    return { type, hours: parseFloat(hours) || 0 }
  }).filter(e => e.type && e.hours > 0)
}

// 日期徽章 class（结合工作日历 + 日报数据）
function getDayBadgeClass(dateStr, dayType, isToday) {
  if (isToday) return 'mc-day-today'

  const hours = getDateWorkHours(dateStr)
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

function getHoursClass(dateStr) {
  const hours = getDateWorkHours(dateStr)
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

// 按需加载子项目选项（首次展开下拉时，预置的单条记录视为未完整加载）
async function loadSubProjectOptions(item) {
  if (item.subProjectOptions && item.subProjectOptions.length > 1) return
  const res = await import('@/api/project/project').then(m => m.getSubProjectOptions(item.projectId))
  item.subProjectOptions = res.data || []
}

// 加载某日的日报数据
async function loadDayReport(dateStr) {
  loading.value = true
  try {
    const res = await getMyReport(dateStr)
    const report = res.data
    currentReportId.value = report?.reportId || null

    // 构建表单：所有项目列出，已有数据的填充
    formList.value = projects.value.map(p => {
      const detail = report?.detailList?.find(d => d.projectId === p.projectId && (!d.entryType || d.entryType === 'work'))
      return {
        projectId: p.projectId,
        projectName: p.projectName,
        projectCode: p.projectCode,
        projectStage: p.projectStage,
        projectStageName: p.projectStageName,
        estimatedWorkload: p.estimatedWorkload != null ? p.estimatedWorkload : null,
        actualWorkload: p.actualWorkload != null ? p.actualWorkload : null,
        revenueConfirmYear: p.revenueConfirmYear || null,
        hasSubProject: !!p.hasSubProject,
        subProjectOptions: (detail?.subProjectId && detail?.subProjectName)
          ? [{ projectId: detail.subProjectId, projectName: detail.subProjectName, taskCode: null }]
          : null,
        workHours: detail ? Number(detail.workHours) : 0,
        workContent: detail ? detail.workContent : '',
        subProjectId: detail ? (detail.subProjectId || null) : null,
        workCategory: detail ? (detail.workCategory || null) : null
      }
    })

    leaveList.value = (report?.detailList || [])
      .filter(d => d.entryType && d.entryType !== 'work')
      .map(d => ({
        detailId: d.detailId,
        entryType: d.entryType,
        leaveHours: parseFloat(d.leaveHours || d.workHours) || 0,
        remark: d.remark || ''
      }))
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
      if (day) map[day] = {
        workHours: Number(r.totalWorkHours),
        leaveSummary: r.leaveSummary || ''
      }
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
      workContent: f.workContent,
      entryType: 'work',
      subProjectId: f.subProjectId || null,
      workCategory: f.workCategory || null
    }))

  // 追加假期行
  const leaveDetails = leaveList.value
    .filter(l => l.entryType && l.leaveHours > 0)
    .map(l => ({
      projectId: null,
      workHours: l.leaveHours,
      workContent: '',
      entryType: l.entryType,
      leaveHours: l.leaveHours,
      remark: l.remark || ''
    }))

  const allDetails = [...details, ...leaveDetails]

  if (allDetails.length === 0) {
    ElMessage.warning('请至少填写一个项目的工时或假期记录')
    return
  }

  saving.value = true
  try {
    await saveDailyReport({
      reportDate: selectedDate.value,
      detailList: allDetails
    })
    ElMessage.success('日报保存成功')
    loadMonthOverview()
    // 重新加载当日日报以更新 currentReportId，使删除按钮可用
    const res2 = await getMyReport(selectedDate.value)
    currentReportId.value = res2.data?.reportId || null
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  await ElMessageBox.confirm(`确认删除 ${formatDate(selectedDate.value)} 的日报？`, '提示', {
    type: 'warning',
    confirmButtonText: '确认删除',
    confirmButtonClass: 'el-button--danger'
  })
  await delDailyReport(currentReportId.value)
  ElMessage.success('日报已删除')
  currentReportId.value = null
  formList.value.forEach(item => { item.workHours = 0; item.workContent = '' })
  leaveList.value = []
  loadMonthOverview()
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

/* 假期类型角标 */
.cal-leave-badge {
  font-size: 10px;
  font-weight: 600;
  margin-top: 1px;
  line-height: 1.2;
  display: block;
}

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

.prj-workload-info {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.prj-hours-row {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.hours-label { font-size: 13px; color: #606266; white-space: nowrap; }

.prj-sub-row {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px; background: #f9fafb; border-radius: 4px; margin-bottom: 6px;
}
.sub-label { font-size: 13px; color: #606266; white-space: nowrap; flex-shrink: 0; }

.prj-content-row {}

.leave-section {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 16px;
  margin-top: 8px;
}
.leave-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.leave-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.leave-empty {
  font-size: 13px;
  color: #c0c4cc;
  text-align: center;
  padding: 8px 0;
}
.leave-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.leave-color-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
