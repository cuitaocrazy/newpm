<template>
  <div class="app-container daily-report-page">
    <!-- 白名单用户提示 -->
    <el-alert
      v-if="isWhitelisted"
      title="您已被设置为无需填写日报"
      description="如有疑问，请联系系统管理员。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px;"
    />
    <el-row v-if="!isWhitelisted" :gutter="16">
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
                  :style="{ color: leaveColorMap[le.type] }">
                  {{ getDictLabel(sys_rbtype, le.type) }}{{ le.hours }}h
                </span>
              </div>
            </template>
          </MonthCalendar>
        </el-card>
        <div class="calendar-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>项目状态<b>非开启</b>或项目阶段属于<b>「已结项」</b>，整个项目不可写日报；若仅某个任务的状态属于<b>「已结项」</b>，则该任务不显示。</span>
        </div>
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
                <el-button type="primary" @click="handleSave" :disabled="saving || (!isEditable && leaveList.length === 0 && !currentReportId)">保存日报</el-button>
                <el-button type="danger" plain @click="handleDelete" :disabled="!currentReportId">删除日报</el-button>
                <span v-if="!isEditable" style="font-size: 12px; color: #f56c6c;">仅限本周（周一至周日）可录入日报；非本周日期仅支持修改假期记录</span>
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
                  :disabled="!isLeaveEditable"
                  @click="leaveList.push({ entryType: 'leave', leaveHours: 1, remark: '' })"
                >+ 添加假期</el-button>
                <el-button
                  type="warning" size="small" plain
                  @click="batchLeaveVisible = true"
                >批量填假期</el-button>
              </div>
              <div v-if="leaveList.length === 0" class="leave-empty">暂无假期记录</div>
              <div v-for="(item, idx) in leaveList" :key="idx" class="leave-item">
                <span class="leave-color-dot" :style="{ background: leaveColorMap[item.entryType] || '#ccc' }"></span>
                <el-select v-model="item.entryType" size="small" style="width: 90px;" :disabled="!isLeaveEditable">
                  <el-option v-for="d in sys_rbtype.filter(d => d.value !== 'work')" :key="d.value" :label="d.label" :value="d.value" />
                </el-select>
                <el-input-number
                  v-model="item.leaveHours"
                  :min="0.5" :max="24" :step="0.5" :precision="1"
                  size="small" style="width: 120px;"
                  :disabled="!isLeaveEditable"
                />
                <span style="color: #909399; font-size: 13px;">小时</span>
                <el-input
                  v-model="item.remark"
                  placeholder="备注(选填)"
                  size="small"
                  style="flex: 1;"
                  :disabled="!isLeaveEditable"
                />
                <el-button link type="danger" icon="Delete" :disabled="!isLeaveEditable"
                  @click="leaveList.splice(idx, 1)" />
              </div>
            </div>

            <div v-for="(item, index) in formList" :key="item.projectId" class="project-item">
              <!-- 第一行：项目经理 + 年度 + 项目名 + 阶段 -->
              <div class="prj-header">
                <div class="prj-color-bar" :style="{ background: getColor(index) }"></div>
                <el-tag v-if="item.revenueConfirmYear" size="small" type="warning" :style="yearTagStyle(item.revenueConfirmYear)" style="flex-shrink:0;">
                  {{ getDictLabel(sys_ndgl, item.revenueConfirmYear) }}
                </el-tag>
                <span class="prj-manager-label"><b>{{ item.projectManagerName || '-' }}</b></span>
                <span class="prj-name">{{ item.projectName }}</span>
                <span class="prj-workload-info">
                  预计人天：<strong>{{ item.estimatedWorkload != null ? item.estimatedWorkload : '-' }}</strong>
                  &nbsp;&nbsp;已花人天：<strong>{{ item.actualWorkload != null ? Number(item.actualWorkload).toFixed(3) : '-' }}</strong>
                </span>
                <el-tag size="small" type="info" style="margin-left: auto; flex-shrink: 0;">{{ item.projectStageName || '未设置' }}</el-tag>
              </div>

              <!-- 无子任务：原有单行逻辑 -->
              <template v-if="!item.hasSubProject">
                <!-- 工作任务类型 -->
                <div class="prj-category-row">
                  <span class="hours-label">工作任务类型: <span style="color:#f56c6c;">*</span></span>
                  <el-select v-model="item.workCategory" placeholder="请选择工作任务类型（必填）" clearable
                    multiple collapse-tags collapse-tags-tooltip
                    size="small" style="width: 260px;" :disabled="!isEditable"
                    :class="{ 'is-required-error': item.workHours > 0 && !item.workCategory?.length }">
                    <el-option v-for="d in sys_gzlb" :key="d.value" :label="d.label" :value="d.value" />
                  </el-select>
                </div>

                <!-- 工时（slider + 输入框） -->
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

                <!-- 工作内容 -->
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
              </template>

              <!-- 有子任务：多任务行 -->
              <template v-else>
                <!-- 合计工时（只读 slider，由各任务工时汇总，紧跟项目信息下方） -->
                <div class="prj-hours-row">
                  <span class="hours-label">合计工时：</span>
                  <el-slider
                    :model-value="item.taskRows ? item.taskRows.reduce((s, t) => s + (t.workHours || 0), 0) : 0"
                    :min="0" :max="24" :step="1"
                    :marks="{ 0: '0', 8: '8h', 16: '16h', 24: '24h' }"
                    style="flex: 1; margin: 0 16px;"
                    disabled
                  />
                  <el-input-number
                    :model-value="item.taskRows ? item.taskRows.reduce((s, t) => s + (t.workHours || 0), 0) : 0"
                    :min="0" :max="24" :step="1" :precision="0"
                    size="small" style="width: 100px;"
                    disabled
                  />
                  <span style="margin-left: 4px; color: #909399;">h</span>
                </div>

                <div v-if="!item.taskRows" class="task-loading" @click="loadTaskRows(item)">
                  <el-button size="small" type="primary" plain>加载任务列表</el-button>
                </div>
                <div v-else class="task-rows-container">
                  <div v-if="item.taskRows.length === 0" style="font-size:13px;color:#c0c4cc;padding:8px 0;">暂无任务</div>
                  <div v-for="task in item.taskRows" :key="task.subProjectId" class="task-row">
                    <!-- 任务头（只读信息） -->
                    <div class="task-row-header">
                      <el-tag v-if="task.batchNo" size="small" type="info">{{ task.batchNo }}</el-tag>
                      <span class="task-manager">{{ task.taskManagerName }}</span>
                      <span class="task-name">{{ task.taskName }}</span>
                      <el-tag v-if="task.taskStage" size="small" type="info">{{ getStageName(task.taskStage) }}</el-tag>
                      <span class="task-workload-info">
                        预计：<strong>{{ task.estimatedWorkload != null ? task.estimatedWorkload : '-' }}</strong>天
                        &nbsp;实际：<strong>{{ task.actualWorkload != null ? Number(task.actualWorkload).toFixed(3) : '-' }}</strong>天
                      </span>
                      <el-select v-model="task.workCategory" placeholder="工作任务类别（必填）" clearable
                        multiple collapse-tags collapse-tags-tooltip
                        size="small" style="width: 200px; margin-left: 8px;" :disabled="!isEditable"
                        :class="{ 'is-required-error': task.workHours > 0 && !task.workCategory?.length }">
                        <el-option v-for="d in sys_gzlb" :key="d.value" :label="d.label" :value="d.value" />
                      </el-select>
                    </div>
                    <!-- 工时 -->
                    <div class="task-row-inputs">
                      <span class="hours-label">工时：</span>
                      <el-input-number v-model="task.workHours" :min="0" :max="24" :step="0.5"
                        size="small" style="width: 110px;" :disabled="!isEditable" />
                      <span style="font-size:12px;color:#909399;">小时</span>
                    </div>
                    <!-- 工作内容 -->
                    <el-input v-model="task.workContent" type="textarea" :rows="2"
                      :placeholder="'填写 ' + task.taskName + ' 的工作内容...'"
                      :disabled="!isEditable"
                      style="margin-top:4px;" />
                  </div>
                </div>
              </template>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 批量填假期弹窗 -->
    <el-dialog v-model="batchLeaveVisible" title="批量填假期" width="460px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="假期类型">
          <el-select v-model="batchLeaveForm.entryType" style="width: 160px;">
            <el-option
              v-for="d in sys_rbtype.filter(d => d.value !== 'work')"
              :key="d.value" :label="d.label" :value="d.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="batchLeaveForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 280px;"
          />
        </el-form-item>
        <el-form-item label="每日时长">
          <el-input-number
            v-model="batchLeaveForm.leaveHoursPerDay"
            :min="0.5" :max="24" :step="0.5" :precision="1"
            style="width: 120px;"
          />
          <span style="margin-left: 8px; color: #909399; font-size: 13px;">小时/天</span>
        </el-form-item>
        <el-form-item label="冲突处理">
          <el-radio-group v-model="batchLeaveForm.conflictStrategy">
            <el-radio value="skip">跳过已有记录</el-radio>
            <el-radio value="overwrite">覆盖已有记录</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchLeaveVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLeaving" @click="handleBatchLeave">确认填写</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DailyReportWrite">
import { ref, onMounted, computed, watch, getCurrentInstance } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, InfoFilled } from '@element-plus/icons-vue'
import { getMyReport, getMyProjects, saveDailyReport, listDailyReport, delDailyReport, batchSaveLeave } from '@/api/project/dailyReport'
import { checkSelfInWhitelist } from '@/api/project/whitelist'
import { getWorkCalendarByYear } from '@/api/project/workCalendar'
import MonthCalendar from '@/components/MonthCalendar/index.vue'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const { sys_ndgl, sys_gzlb, sys_xmjd, sys_rbtype } = proxy.useDict('sys_ndgl', 'sys_gzlb', 'sys_xmjd', 'sys_rbtype')

const userStore = useUserStore()

function formatDateStr(date) {
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const selectedDate = ref(formatDateStr(new Date()))
const isWhitelisted = ref(false)
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
const currentYear = new Date().getFullYear().toString()
function yearTagStyle(year) {
  return parseInt(year) < parseInt(currentYear)
    ? { backgroundColor: '#f5222d', borderColor: '#f5222d', color: '#fff' }
    : {}
}

// 假期类型颜色：由字典 list_class 动态驱动，不硬编码
const ELEMENT_PLUS_COLORS = {
  primary: '#409eff', success: '#67c23a',
  warning: '#e6a23c', danger: '#f56c6c', info: '#909399'
}
const leaveColorMap = computed(() => {
  return Object.fromEntries(
    (sys_rbtype.value || [])
      .filter(d => d.value !== 'work')
      .map(d => [d.value, ELEMENT_PLUS_COLORS[d.elTagType] || '#909399'])
  )
})

// 批量填假期弹窗状态
const batchLeaveVisible = ref(false)
const batchLeaving = ref(false)
const batchLeaveForm = ref({
  entryType: 'leave',
  dateRange: [],
  leaveHoursPerDay: 8,
  conflictStrategy: 'skip'
})

async function handleBatchLeave() {
  const [startDate, endDate] = batchLeaveForm.value.dateRange || []
  if (!startDate || !endDate) {
    ElMessage.warning('请选择日期范围')
    return
  }
  batchLeaving.value = true
  try {
    const res = await batchSaveLeave({
      entryType: batchLeaveForm.value.entryType,
      startDate,
      endDate,
      leaveHoursPerDay: batchLeaveForm.value.leaveHoursPerDay,
      conflictStrategy: batchLeaveForm.value.conflictStrategy
    })
    const { created, skipped, totalWorkdays } = res.data
    ElMessage.success(`批量填写完成：共 ${totalWorkdays} 个工作日，新建 ${created} 条，跳过 ${skipped} 条`)
    batchLeaveVisible.value = false
    loadMonthOverview()
  } finally {
    batchLeaving.value = false
  }
}

// 假期记录列表（当天）
const leaveList = ref([])

const totalHours = computed(() => {
  return formList.value.reduce((sum, item) => {
    if (item.hasSubProject && item.taskRows) {
      return sum + item.taskRows.reduce((s, t) => s + (t.workHours || 0), 0)
    }
    return sum + (item.workHours || 0)
  }, 0)
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

// 假期记录不受本周限制，任意日期均可修改（婚假/年假等可提前填写未来日期）
const isLeaveEditable = computed(() => true)

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

// 阶段 dict lookup（替代 Vue 2 filter）
function getStageName(val) {
  if (!val) return ''
  const found = (sys_xmjd.value || []).find(d => d.value === val)
  return found ? found.label : val
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

// 加载子任务行（有子任务的项目使用）
async function loadTaskRows(item) {
  if (item.taskRows !== null && item.taskRows !== undefined) return
  const res = await import('@/api/project/task').then(m => m.getTaskOptions(item.projectId))
  const tasks = res.data || []
  item.taskRows = tasks.map(t => {
    const existingDetail = (item._existingDetails || []).find(d => d.subProjectId === t.taskId)
    return {
      subProjectId: t.taskId,
      taskName: t.taskName,
      batchNo: t.batchNo || '',
      taskStage: t.taskStage,
      taskManagerName: t.taskManagerName || '-',
      estimatedWorkload: t.estimatedWorkload != null ? t.estimatedWorkload : null,
      actualWorkload: t.actualWorkload != null ? t.actualWorkload : null,
      workCategory: existingDetail?.workCategory
        ? existingDetail.workCategory.split(',').map(s => s.trim()).filter(Boolean)
        : [],
      workHours: existingDetail ? Number(existingDetail.workHours) : 0,
      workContent: existingDetail?.workContent || ''
    }
  })
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
      if (p.hasSubProject) {
        // 有子任务的项目：收集该项目的所有 work 条目，存入 _existingDetails，延迟加载 taskRows
        const existingDetails = (report?.detailList || []).filter(
          d => d.projectId === p.projectId && (!d.entryType || d.entryType === 'work') && d.subProjectId != null
        )
        const item = {
          projectId: p.projectId,
          projectName: p.projectName,
          projectCode: p.projectCode,
          projectStage: p.projectStage,
          projectStageName: p.projectStageName,
          projectManagerName: p.projectManagerName || '',
          estimatedWorkload: p.estimatedWorkload != null ? p.estimatedWorkload : null,
          actualWorkload: p.actualWorkload != null ? p.actualWorkload : null,
          revenueConfirmYear: p.revenueConfirmYear || null,
          hasSubProject: true,
          _existingDetails: existingDetails,
          taskRows: null  // null 表示未加载，会触发"加载任务列表"按钮
        }
        return item
      } else {
        const detail = report?.detailList?.find(d => d.projectId === p.projectId && (!d.entryType || d.entryType === 'work'))
        return {
          projectId: p.projectId,
          projectName: p.projectName,
          projectCode: p.projectCode,
          projectStage: detail?.projectStage || p.projectStage,
          projectStageName: detail?.projectStageName || p.projectStageName,
          projectManagerName: p.projectManagerName || '',
          estimatedWorkload: p.estimatedWorkload != null ? p.estimatedWorkload : null,
          actualWorkload: p.actualWorkload != null ? p.actualWorkload : null,
          revenueConfirmYear: p.revenueConfirmYear || null,
          hasSubProject: false,
          workHours: detail ? Number(detail.workHours) : 0,
          workContent: detail ? detail.workContent : '',
          workCategory: detail?.workCategory
            ? detail.workCategory.split(',').map(s => s.trim()).filter(Boolean)
            : []
        }
      }
    })

    // 对所有有子任务的项目，自动加载 taskRows（guard 已保证不重复加载）
    const subProjectItems = formList.value.filter(item => item.hasSubProject)
    await Promise.all(subProjectItems.map(item => loadTaskRows(item)))

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
  const details = []

  for (const item of formList.value) {
    if (item.hasSubProject && item.taskRows === null) {
      proxy.$modal.msgWarning(`项目"${item.projectName}"的任务列表尚未加载，请稍后再试`)
      return  // 阻止整个保存操作
    }
    if (item.hasSubProject && item.taskRows) {
      // 有子任务的项目：遍历 taskRows，工时>0 的生成 detail
      for (const t of item.taskRows.filter(t => t.workHours > 0)) {
        if (!t.workCategory?.length) {
          proxy.$modal.msgWarning(`项目"${item.projectName}"的任务"${t.taskName}"工时已填写，请选择工作任务类别`)
          return
        }
        details.push({
          projectId: item.projectId,
          projectStage: item.projectStage,
          workHours: t.workHours,
          workContent: t.workContent,
          entryType: 'work',
          subProjectId: t.subProjectId,
          workCategory: t.workCategory?.length ? t.workCategory.join(',') : null
        })
      }
    } else if (!item.hasSubProject) {
      // 无子任务的项目：工时>0 时 workCategory 必须选择
      if (item.workHours > 0) {
        if (!item.workCategory?.length) {
          proxy.$modal.msgWarning(`项目"${item.projectName}"工时已填写，请选择工作任务类型`)
          return
        }
        if (item.workContent && item.workContent.trim()) {
          details.push({
            projectId: item.projectId,
            projectStage: item.projectStage,
            workHours: item.workHours,
            workContent: item.workContent,
            entryType: 'work',
            subProjectId: null,
            workCategory: item.workCategory?.length ? item.workCategory.join(',') : null
          })
        }
      }
    }
  }

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
  formList.value.forEach(item => {
    if (item.hasSubProject && item.taskRows) {
      item.taskRows.forEach(t => { t.workHours = 0; t.workContent = '' })
    } else {
      item.workHours = 0
      item.workContent = ''
    }
  })
  leaveList.value = []
  loadMonthOverview()
}


onMounted(async () => {
  // 检查当前用户是否在日报白名单中
  const whitelistRes = await checkSelfInWhitelist().catch(() => ({ data: false }))
  isWhitelisted.value = whitelistRes.data === true
  if (isWhitelisted.value) return  // 白名单用户无需加载日报相关数据
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

.prj-category-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
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

/* 多任务行样式 */
.task-rows-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-row {
  background: #f9fafb;
  border: 1px solid #e8eaed;
  border-radius: 6px;
  padding: 10px 12px;
}

.task-row-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.task-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.task-manager {
  font-size: 12px;
  color: #909399;
}

.task-workload-info {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.task-row-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0;
}

.prj-manager-label {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
  flex-shrink: 0;
}
.prj-manager-label b { color: #303133; }

.task-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  flex-shrink: 0;
}

.task-loading {
  text-align: center;
  padding: 12px 0;
}

/* 必填校验红框 */
.is-required-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #f56c6c inset;
}

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

.calendar-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 10px;
  padding: 8px 12px;
  background: #f0f7ff;
  border: 1px solid #d0e8ff;
  border-radius: 6px;
  font-size: 12px;
  color: #4a90d9;
  line-height: 1.6;
}
.calendar-tip b {
  color: #1677ff;
  font-weight: 600;
}
.calendar-tip .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
  font-size: 13px;
}
</style>
