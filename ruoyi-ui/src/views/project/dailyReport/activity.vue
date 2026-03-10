<template>
  <div class="app-container">
    <!-- 查询栏 -->
    <el-form :inline="true" :model="queryParams" class="query-form">
      <el-form-item label="姓名">
        <el-select v-model="queryParams.userId" filterable clearable
          placeholder="全部人员" style="width: 180px;" @change="handleQuery">
          <el-option label="全部人员（团队概览）" value="" />
          <el-option v-for="u in userList" :key="u.userId"
            :label="u.nickName" :value="u.userId">
            <span>{{ u.nickName }}</span>
            <span style="color: #909399; margin-left: 8px; font-size: 12px;">{{ u.deptName }}</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="项目部门">
        <project-dept-select
          ref="projectDeptSelectRef"
          v-model="queryParams.deptId"
          style="width: 220px;"
          @change="handleQuery"
        />
      </el-form-item>
      <el-form-item label="项目名称">
        <el-autocomplete
          v-model="queryParams.projectName"
          :fetch-suggestions="fetchProjectSuggestions"
          placeholder="输入关键字搜索"
          clearable
          style="width: 200px;"
          value-key="projectName"
          @select="handleProjectSelect"
          @clear="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 信息栏：团队模式 or 个人模式 -->
    <el-card v-if="!queryParams.userId" shadow="hover" class="info-bar">
      <div style="display: flex; align-items: center; gap: 16px;">
        <el-avatar :size="44" style="background: linear-gradient(135deg, #409eff, #66b1ff);">
          <el-icon :size="24"><User /></el-icon>
        </el-avatar>
        <div style="flex: 1;">
          <div style="font-size: 17px; font-weight: 700;">{{ currentDeptName || '全部部门' }}</div>
          <div style="font-size: 13px; color: #909399;">点击日历格子中的人员可查看个人详情</div>
        </div>
        <div style="display: flex; gap: 24px;">
          <div class="stat-box"><div class="stat-num">{{ teamStats.total }}</div><div class="stat-label">团队人数</div></div>
          <div class="stat-box"><div class="stat-num">{{ teamStats.reported }}</div><div class="stat-label">今日已填</div></div>
          <div class="stat-box"><div class="stat-num">{{ teamStats.unreported }}</div><div class="stat-label">今日未填</div></div>
          <div class="stat-box"><div class="stat-num">{{ teamStats.avgHours }}h</div><div class="stat-label">今日人均工时</div></div>
        </div>
      </div>
    </el-card>

    <el-card v-else shadow="hover" class="info-bar">
      <div style="display: flex; align-items: center; gap: 16px;">
        <el-avatar :size="44" :style="{ background: selectedUserColor }">
          {{ selectedUserName?.charAt(0) }}
        </el-avatar>
        <div style="flex: 1;">
          <div style="font-size: 17px; font-weight: 700;">{{ selectedUserName }}</div>
          <div style="font-size: 13px; color: #909399;">{{ selectedUserDept }}</div>
        </div>
        <div style="display: flex; gap: 24px;">
          <div class="stat-box"><div class="stat-num">{{ personStats.days }}</div><div class="stat-label">已填报天</div></div>
          <div class="stat-box"><div class="stat-num">{{ personStats.totalHours }}h</div><div class="stat-label">月累计工时</div></div>
          <div class="stat-box"><div class="stat-num">{{ personStats.avgHours }}h</div><div class="stat-label">日均工时</div></div>
          <div class="stat-box"><div class="stat-num">{{ personStats.fullDays }}/{{ personStats.days }}</div><div class="stat-label">满勤(>=8h)</div></div>
        </div>
        <el-button text type="primary" @click="clearPerson">返回团队概览</el-button>
      </div>
    </el-card>

    <!-- 日历 -->
    <el-card v-if="!listMode" shadow="hover">
      <MonthCalendar
        v-model="selectedDate"
        :work-calendar-map="workCalendarMap"
        @month-change="handleMonthChange"
        @year-change="handleYearChange"
      >
        <template #default="{ day, dateStr, isToday, dayType, wcItem }">
          <div class="activity-cell" :class="getActivityCellClass(dayType)" @click="handleCellClick(dateStr)">
            <div class="cell-top">
              <span class="mc-day-num" :class="getDayBadgeClass(dayType, isToday)">{{ day }}</span>
              <span v-if="getCalendarTag(dateStr, wcItem)" class="cell-tag" :class="getCalendarTagClass(wcItem)">
                {{ getCalendarTag(dateStr, wcItem) }}
              </span>
              <span v-if="getCellSummary(dateStr)" class="cell-summary">
                {{ getCellSummary(dateStr) }}
              </span>
            </div>
            <!-- 团队模式：显示人员列表 -->
            <div v-if="!queryParams.userId" class="cell-people">
              <div v-for="person in getCellPeople(dateStr).slice(0, 4)" :key="person.userId"
                class="person-chip" :class="getChipClass(person.totalWorkHours)"
                @click.stop="openDrawer(dateStr)">
                <span class="chip-name">{{ person.nickName }}</span>
                <span class="chip-hours">{{ person.totalWorkHours }}h</span>
              </div>
              <div v-if="getCellPeople(dateStr).length > 4" class="more-chip"
                @click.stop="openDrawer(dateStr)">
                +{{ getCellPeople(dateStr).length - 4 }}人
              </div>
            </div>
            <!-- 个人模式：显示项目详情 -->
            <div v-else class="cell-projects">
              <div v-for="detail in getCellDetails(dateStr)" :key="detail.detailId" class="cell-prj">
                <div class="cell-prj-bar" :style="{ background: getProjectColor(detail.projectId) }"></div>
                <div class="cell-prj-info">
                  <div class="cell-prj-top">
                    <span class="cell-prj-name" :title="detail.projectName">{{ detail.projectName }}</span>
                    <el-tag size="small" type="info">{{ detail.projectStageName }}</el-tag>
                    <span class="cell-prj-h">{{ detail.workHours }}h</span>
                  </div>
                  <div class="cell-prj-time">更新 {{ formatTime(detail.updateTime) }}</div>
                </div>
              </div>
              <div v-if="getCellDetails(dateStr).length === 0 && isWorkday(dateStr, dayType)" class="cell-empty">
                未填报
              </div>
              <!-- 假期行 -->
              <div v-for="leave in getCellLeaves(dateStr)" :key="leave.detailId || leave.entryType"
                class="cell-leave">
                <span class="cell-leave-dot" :style="{ background: LEAVE_TYPE_COLOR[leave.entryType] }"></span>
                <span class="cell-leave-label" :style="{ color: LEAVE_TYPE_COLOR[leave.entryType] }">
                  {{ LEAVE_TYPE_LABEL[leave.entryType] }}
                </span>
                <span class="cell-leave-h">{{ leave.leaveHours || leave.workHours }}h</span>
              </div>
            </div>
          </div>
        </template>
      </MonthCalendar>

      <div class="legend">
        <span class="legend-item"><span class="legend-dot" style="background:#67c23a"></span>项目满勤(≥8h)</span>
        <span class="legend-item"><span class="legend-dot" style="background:#e6a23c"></span>项目不满(&lt;8h)</span>
        <span class="legend-item"><span class="legend-dot" style="background:#409eff"></span>项目超时(&gt;8h)</span>
        <span class="legend-item"><span class="legend-dot" style="background:#f56c6c"></span>请假</span>
        <span class="legend-item"><span class="legend-dot" style="background:#b37feb"></span>倒休</span>
        <span class="legend-item"><span class="legend-dot" style="background:#36cfc9"></span>年假</span>
      </div>
    </el-card>

    <!-- 列表模式：选了人 + 日期范围时显示 -->
    <template v-if="listMode">
      <div v-if="sortedReportList.length === 0" style="text-align:center;color:#909399;padding:40px 0;">
        该时间段内无日报记录
      </div>
      <el-card v-for="report in sortedReportList" :key="report.reportId"
        shadow="hover" style="margin-bottom: 12px;">
        <template #header>
          <div style="display: flex; align-items: center; gap: 12px;">
            <span style="font-weight: 700; font-size: 15px;">{{ report.reportDate }}</span>
            <span style="color: #909399; font-size: 13px;">{{ weekdayLabel(report.reportDate) }}</span>
            <el-tag :type="report.totalWorkHours >= 8 ? 'success' : 'warning'" size="small" round>
              {{ report.totalWorkHours }}h
            </el-tag>
            <span style="margin-left: auto; font-size: 12px; color: #c0c4cc;">
              更新 {{ formatTime(report.updateTime) }}
            </span>
          </div>
        </template>
        <div v-for="detail in report.detailList.filter(d => !d.entryType || d.entryType === 'work')"
          :key="detail.detailId" class="drawer-prj">
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 2px;">
            <el-tag v-if="detail.revenueConfirmYear" size="small" type="warning" :style="yearTagStyle(detail.revenueConfirmYear)">
              {{ getDictLabel(sys_ndgl, detail.revenueConfirmYear) }}
            </el-tag>
            <el-tag size="small" type="primary">{{ detail.projectName }}</el-tag>
            <el-tag size="small" type="info">{{ detail.projectStageName }}</el-tag>
            <span style="margin-left: auto; font-weight: 700;">{{ detail.workHours }}h</span>
          </div>
          <div v-if="detail.subProjectName || detail.workCategory" style="display: flex; align-items: center; gap: 6px; margin-bottom: 4px; padding-left: 2px;">
            <el-tag v-if="detail.subProjectName" size="small" type="success">{{ detail.subProjectName }}</el-tag>
            <dict-tag v-if="detail.workCategory" :options="sys_gzlb" :value="detail.workCategory" />
          </div>
          <div style="font-size: 12px; color: #909399; margin-bottom: 4px;">
            预计人天：<strong>{{ detail.estimatedWorkload != null ? detail.estimatedWorkload : '-' }}</strong>
            &nbsp;&nbsp;已花人天：<strong>{{ detail.actualWorkload != null ? Number(detail.actualWorkload).toFixed(3) : '-' }}</strong>
          </div>
          <div style="font-size: 13px; color: #606266; line-height: 1.6; padding-left: 8px; border-left: 2px solid #e4e7ed;" v-html="formatWorkContent(detail.workContent)"></div>
          <div style="font-size: 11px; color: #c0c4cc; margin-top: 3px;">
            更新于 {{ formatTime(detail.updateTime) }}
          </div>
        </div>
        <div v-for="leave in report.detailList.filter(d => d.entryType && d.entryType !== 'work')"
          :key="'leave-' + leave.detailId" class="drawer-leave">
          <span class="cell-leave-dot" :style="{ background: LEAVE_TYPE_COLOR[leave.entryType] }"></span>
          <span :style="{ color: LEAVE_TYPE_COLOR[leave.entryType], fontWeight: 600 }">
            {{ LEAVE_TYPE_LABEL[leave.entryType] }}
          </span>
          <span style="margin-left: auto; font-weight: 700;">{{ leave.leaveHours || leave.workHours }}h</span>
          <span v-if="leave.remark" style="font-size: 12px; color: #909399; margin-left: 8px;">{{ leave.remark }}</span>
        </div>
      </el-card>
    </template>

    <!-- 抽屉（团队模式查看某天详情） -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="780px">
      <div style="display: flex; gap: 24px; padding: 14px 16px; background: #f5f7fa; border-radius: 8px; margin-bottom: 16px;">
        <div class="stat-box"><div class="stat-num">{{ drawerStats.count }}</div><div class="stat-label">已提交</div></div>
        <div class="stat-box"><div class="stat-num">{{ drawerStats.totalHours }}h</div><div class="stat-label">总工时</div></div>
        <div class="stat-box"><div class="stat-num">{{ drawerStats.fullCount }}/{{ drawerStats.count }}</div><div class="stat-label">满勤(>=8h)</div></div>
      </div>
      <el-card v-for="person in drawerPeople" :key="person.userId" shadow="hover" style="margin-bottom: 12px;">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <el-avatar :size="34" :style="{ background: getPersonColor(person.userId) }">
                {{ person.nickName?.charAt(0) }}
              </el-avatar>
              <div>
                <div style="font-weight: 600;">{{ person.nickName }}</div>
                <div style="font-size: 12px; color: #909399;">{{ person.deptName }}</div>
              </div>
            </div>
            <el-tag :type="person.totalWorkHours >= 8 ? 'success' : 'warning'" size="large" round>
              {{ person.totalWorkHours }}h
            </el-tag>
          </div>
        </template>
        <div v-for="detail in person.detailList.filter(d => !d.entryType || d.entryType === 'work')" :key="detail.detailId" class="drawer-prj">
          <!-- 项目行：项目经理 | 年度 | 项目名 | 预计人天 | 已花人天 | 项目阶段 | 工时 -->
          <div class="detail-project-row">
            <span class="detail-row-prefix">项目：</span>
            <span v-if="detail.projectManagerName" class="detail-manager">{{ detail.projectManagerName }}</span>
            <el-tag v-if="detail.revenueConfirmYear" size="small" type="warning" :style="yearTagStyle(detail.revenueConfirmYear)">
              {{ getDictLabel(sys_ndgl, detail.revenueConfirmYear) }}
            </el-tag>
            <el-tag size="small" type="primary">{{ detail.projectName }}</el-tag>
            <span class="detail-workload">预计 <strong>{{ detail.estimatedWorkload != null ? detail.estimatedWorkload : '-' }}</strong> 天</span>
            <span class="detail-workload">已花 <strong>{{ detail.actualWorkload != null ? Number(detail.actualWorkload).toFixed(3) : '-' }}</strong> 天</span>
            <el-tag size="small" type="info">{{ detail.projectStageName }}</el-tag>
            <span class="detail-hours">{{ detail.workHours }}h</span>
          </div>
          <!-- 任务行：仅有 subProjectId 时显示 -->
          <div v-if="detail.subProjectId" class="detail-task-row">
            <span class="detail-row-prefix">任务：</span>
            <el-tag size="small" type="success">{{ detail.subProjectName }}</el-tag>
            <el-tag v-if="detail.subProjectStage" size="small" type="warning">{{ getStageName(detail.subProjectStage) }}</el-tag>
            <span v-if="detail.subProjectManagerName" class="detail-manager">负责人：{{ detail.subProjectManagerName }}</span>
            <dict-tag v-if="detail.workCategory" :options="sys_gzlb" :value="detail.workCategory" />
          </div>
          <!-- 工作内容 -->
          <div class="detail-content" v-html="formatWorkContent(detail.workContent)"></div>
          <div style="font-size:11px;color:#c0c4cc;margin-top:3px;">
            更新于 {{ formatTime(detail.updateTime) }}
          </div>
        </div>
        <!-- 假期行 -->
        <div v-for="leave in person.detailList.filter(d => d.entryType && d.entryType !== 'work')"
          :key="'leave-' + leave.detailId" class="drawer-leave">
          <span class="cell-leave-dot" :style="{ background: LEAVE_TYPE_COLOR[leave.entryType] }"></span>
          <span :style="{ color: LEAVE_TYPE_COLOR[leave.entryType], fontWeight: 600 }">
            {{ LEAVE_TYPE_LABEL[leave.entryType] }}
          </span>
          <span style="margin-left: auto; font-weight: 700;">{{ leave.leaveHours || leave.workHours }}h</span>
          <span v-if="leave.remark" style="font-size: 12px; color: #909399; margin-left: 8px;">{{ leave.remark }}</span>
        </div>
      </el-card>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { User } from '@element-plus/icons-vue'
import { getMonthlyReports, getProjectNameSuggestions } from '@/api/project/dailyReport'
import { getWorkCalendarByYear } from '@/api/project/workCalendar'
import request from '@/utils/request'
import MonthCalendar from '@/components/MonthCalendar/index.vue'

const { proxy } = getCurrentInstance()
const { sys_ndgl, sys_gzlb, sys_xmjd } = proxy.useDict('sys_ndgl', 'sys_gzlb', 'sys_xmjd')

const todayStr = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})()

const selectedDate = ref(todayStr)
const currentYearMonth = ref((() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
})())
const queryParams = ref({ userId: '', deptId: null, projectName: null })
const reportData = ref([]) // 月度完整数据
const userList = ref([])
const projectDeptSelectRef = ref(null)
const workCalendarMap = ref({}) // { 'yyyy-MM-dd': { dayType, dayName } }
const drawerVisible = ref(false)
const drawerTitle = ref('')
const drawerPeople = ref([])
const drawerStats = ref({ count: 0, totalHours: 0, fullCount: 0 })

const projectColors = ['#409eff', '#67c23a', '#f56c6c', '#e6a23c', '#b37feb', '#00b894', '#fdcb6e', '#909399']
const currentYear = new Date().getFullYear().toString()
function yearTagStyle(year) {
  return parseInt(year) < parseInt(currentYear)
    ? { backgroundColor: '#f5222d', borderColor: '#f5222d', color: '#fff' }
    : {}
}
const LEAVE_TYPE_COLOR = { leave: '#f56c6c', comp: '#b37feb', annual: '#36cfc9' }
const LEAVE_TYPE_LABEL = { leave: '请假', comp: '倒休', annual: '年假' }
const personColorMap = {}
let colorIndex = 0

function getDictLabel(dictList, value) {
  return dictList?.find(d => d.value == value)?.label || value
}

function getStageName(val) {
  return getDictLabel(sys_xmjd.value, val)
}

function getPersonColor(userId) {
  if (!personColorMap[userId]) {
    personColorMap[userId] = projectColors[colorIndex % projectColors.length]
    colorIndex++
  }
  return personColorMap[userId]
}

function getProjectColor(projectId) {
  return projectColors[(projectId || 0) % projectColors.length]
}

const currentDeptName = computed(() => {
  if (!queryParams.value.deptId) return ''
  const findLabel = (nodes, id) => {
    for (const node of nodes) {
      if (node.id === id) return node.label
      if (node.children?.length) {
        const found = findLabel(node.children, id)
        if (found) return found
      }
    }
    return ''
  }
  return findLabel(projectDeptSelectRef.value?.deptOptions || [], queryParams.value.deptId) || ''
})

const selectedUserName = computed(() => {
  const u = userList.value.find(x => x.userId === queryParams.value.userId)
  return u?.nickName || ''
})

const selectedUserDept = computed(() => {
  const u = userList.value.find(x => x.userId === queryParams.value.userId)
  return u?.deptName || ''
})

const selectedUserColor = computed(() => getPersonColor(queryParams.value.userId))

// 列表模式：选了特定用户时展示所有日报列表
const listMode = computed(() => !!queryParams.value.userId)

// 列表模式下按日期升序排列的报告列表
const sortedReportList = computed(() => {
  if (!listMode.value) return []
  return [...displayReportData.value]
    .filter(r => r.userId === queryParams.value.userId)
    .sort((a, b) => {
      const d = (a.reportDate || '').localeCompare(b.reportDate || '')
      if (d !== 0) return d
      return (a.updateTime || '').localeCompare(b.updateTime || '')
    })
})

const weekdayLabel = (dateStr) => {
  if (!dateStr) return ''
  const labels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return labels[new Date(dateStr).getDay()]
}


const displayReportData = computed(() => {
  if (!queryParams.value.projectName) return reportData.value
  const keyword = queryParams.value.projectName.toLowerCase()
  return reportData.value.map(r => {
    const filteredDetails = (r.detailList || []).filter(d =>
      d.projectName?.toLowerCase().includes(keyword)
    )
    const filteredHours = filteredDetails.reduce((s, d) => s + Number(d.workHours || 0), 0)
    return { ...r, detailList: filteredDetails, totalWorkHours: filteredHours }
  })
})

// 按日期分组数据
const dataByDate = computed(() => {
  const map = {}
  displayReportData.value.forEach(r => {
    const day = r.reportDate?.substring(0, 10)
    if (!day) return
    if (!map[day]) map[day] = []
    map[day].push(r)
  })
  return map
})

// 团队统计
const teamStats = computed(() => {
  const todayData = dataByDate.value[todayStr] || []
  const todayTotalH = todayData.reduce((s, r) => s + Number(r.totalWorkHours || 0), 0)
  const avgH = todayData.length > 0 ? (todayTotalH / todayData.length).toFixed(1) : '0.0'
  // 有项目筛选时：团队人数 = 本月内有该项目填报记录的不重复用户数
  // 无筛选时：团队人数 = 数据权限范围内的全部用户数
  const total = queryParams.value.projectName
    ? new Set(displayReportData.value.map(r => r.userId)).size
    : userList.value.length
  return {
    total,
    reported: todayData.length,
    unreported: total - todayData.length,
    avgHours: avgH
  }
})

// 个人统计
const personStats = computed(() => {
  const userReports = displayReportData.value.filter(r => r.userId === queryParams.value.userId)
  const totalH = userReports.reduce((s, r) => s + Number(r.totalWorkHours || 0), 0)
  const fullDays = userReports.filter(r => Number(r.totalWorkHours) >= 8).length
  const avgH = userReports.length > 0 ? (totalH / userReports.length).toFixed(1) : '0.0'
  return {
    days: userReports.length,
    totalHours: totalH.toFixed(1),
    fullDays,
    avgHours: avgH
  }
})

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.substring(11, 16)
}

function formatWorkContent(content) {
  if (!content) return ''
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\r\n/g, '<br>')
    .replace(/\n/g, '<br>')
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

// 判断是否为工作日
function isWorkday(dateStr, dayType) {
  return dayType === 'normal' || dayType === 'workday'
}

// 日历格子整体 class
function getActivityCellClass(dayType) {
  if (dayType === 'holiday') return 'cell-holiday'
  if (dayType === 'workday') return 'cell-makeup-workday'
  if (dayType === 'weekend') return 'cell-weekend'
  return ''
}

// 日期徽章 class
function getDayBadgeClass(dayType, isToday) {
  if (isToday) return 'mc-day-today'
  return `mc-day-${dayType}`
}

// 日历标签（节假日名称 / 调休）
function getCalendarTag(dateStr, wcItem) {
  if (wcItem?.dayName) return wcItem.dayName
  if (wcItem?.dayType === 'holiday') return '假'
  if (wcItem?.dayType === 'workday') return '班'
  return null
}

function getCalendarTagClass(wcItem) {
  if (wcItem?.dayType === 'holiday') return 'tag-holiday'
  if (wcItem?.dayType === 'workday') return 'tag-workday'
  return ''
}

function getChipClass(hours) {
  if (hours > 8) return 'chip-over'
  if (hours >= 8) return 'chip-ok'
  return 'chip-warn'
}

function getCellSummary(dateStr) {
  const people = dataByDate.value[dateStr]
  if (!people || people.length === 0) return null
  if (!queryParams.value.userId) {
    const totalH = people.reduce((s, r) => s + Number(r.totalWorkHours), 0)
    return `${people.length}人 ${totalH.toFixed(1)}h`
  } else {
    const report = people.find(r => r.userId === queryParams.value.userId)
    if (!report) return null
    const h = Number(report.totalWorkHours)
    return `${h}h`
  }
}

function getCellPeople(dateStr) {
  return dataByDate.value[dateStr] || []
}

function getCellDetails(dateStr) {
  const people = dataByDate.value[dateStr] || []
  const report = people.find(r => r.userId === queryParams.value.userId)
  return (report?.detailList || []).filter(d => !d.entryType || d.entryType === 'work')
}

function getCellLeaves(dateStr) {
  const people = dataByDate.value[dateStr] || []
  const report = people.find(r => r.userId === queryParams.value.userId)
  return (report?.detailList || []).filter(d => d.entryType && d.entryType !== 'work')
}

function selectPerson(userId) {
  queryParams.value.userId = userId
}

function clearPerson() {
  queryParams.value.userId = ''
}

function openDrawer(dateStr) {
  const people = dataByDate.value[dateStr] || []
  if (people.length === 0) return
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const d = new Date(dateStr)
  drawerTitle.value = `${dateStr} ${weekdays[d.getDay()]} 日报详情`
  drawerPeople.value = people
  const totalH = people.reduce((s, r) => s + Number(r.totalWorkHours), 0)
  const fullCount = people.filter(r => Number(r.totalWorkHours) >= 8).length
  drawerStats.value = { count: people.length, totalHours: totalH.toFixed(1), fullCount }
  drawerVisible.value = true
}

function handleCellClick(dateStr) {
  if (!queryParams.value.userId) {
    openDrawer(dateStr)
  } else {
    openPersonDrawer(dateStr)
  }
}

function openPersonDrawer(dateStr) {
  const people = dataByDate.value[dateStr] || []
  const person = people.find(r => r.userId === queryParams.value.userId)
  if (!person) return
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const d = new Date(dateStr)
  drawerTitle.value = `${selectedUserName.value} · ${dateStr} ${weekdays[d.getDay()]} 日报详情`
  drawerPeople.value = [person]
  const h = Number(person.totalWorkHours)
  drawerStats.value = { count: 1, totalHours: h.toFixed(1), fullCount: h >= 8 ? 1 : 0 }
  drawerVisible.value = true
}

async function loadData() {
  const params = {}
  if (queryParams.value.userId) params.userId = queryParams.value.userId
  if (queryParams.value.deptId) params.deptId = queryParams.value.deptId
  if (queryParams.value.projectName) params.projectName = queryParams.value.projectName

  if (!listMode.value) {
    params.yearMonth = currentYearMonth.value
  }

  const res = await getMonthlyReports(params)
  reportData.value = res.data || []
}

async function loadUsers() {
  const params = {}
  if (queryParams.value.deptId) params.deptId = queryParams.value.deptId
  const res = await request({ url: '/project/dailyReport/activityUsers', method: 'get', params })
  userList.value = (res.data || []).map(u => ({
    userId: u.userId,
    nickName: u.nickName,
    deptName: u.deptName || ''
  }))
}

async function handleQuery() {
  await loadUsers()
  loadData()
}

async function handleReset() {
  queryParams.value = { userId: '', deptId: null, projectName: null }
  await loadUsers()
  loadData()
}

async function fetchProjectSuggestions(keyword, callback) {
  try {
    const res = await getProjectNameSuggestions(keyword || undefined)
    callback(res.data || [])
  } catch { callback([]) }
}

function handleProjectSelect(item) {
  queryParams.value.projectName = item.projectName
  handleQuery()
}

// MonthCalendar 事件
function handleMonthChange({ yearMonth }) {
  currentYearMonth.value = yearMonth
  loadData()
}

function handleYearChange(year) {
  loadWorkCalendar(year)
}

onMounted(async () => {
  loadWorkCalendar()
  await loadUsers()
  loadData()
})
</script>

<style scoped>
.info-bar { margin-bottom: 16px; }
.stat-box { text-align: center; padding: 0 16px; border-right: 1px solid #ebeef5; }
.stat-box:last-child { border-right: none; }
.stat-num { font-size: 22px; font-weight: 700; color: #409eff; }
.stat-label { font-size: 12px; color: #909399; margin-top: 2px; }

/* 覆盖 MonthCalendar 的 td hover（活动页面用 shadow 代替 outline） */
:deep(.mc-table td:hover:not(.mc-empty)) {
  outline: none;
  background: #f8f9fb;
}
:deep(.mc-table td) {
  border-radius: 8px;
  vertical-align: top;
}

/* ===== 日历格子内容 ===== */
.activity-cell { min-height: 100px; cursor: pointer; padding: 6px 8px; }
.activity-cell.cell-holiday { background: #fff5f5; }
.activity-cell.cell-weekend { background: transparent; }
.activity-cell.cell-makeup-workday { background: #f0f7ff; }

.cell-top { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }

.cell-summary { font-size: 11px; color: #86909c; margin-left: auto; }
.cell-tag { font-size: 10px; padding: 1px 6px; border-radius: 4px; font-weight: 500; }
.tag-holiday { background: #ffece8; color: #f56c6c; }
.tag-workday { background: #e8f3ff; color: #409eff; }

/* 人员条 */
.person-chip {
  display: flex; justify-content: space-between; padding: 3px 8px;
  margin: 2px 0; border-radius: 6px; font-size: 11px; cursor: pointer;
  transition: transform 0.15s;
}
.person-chip:hover { transform: scale(1.02); filter: brightness(0.97); }
.chip-ok { background: #e8ffea; color: #00b42a; }
.chip-warn { background: #fff7e8; color: #ff7d00; }
.chip-over { background: #e8f3ff; color: #409eff; }
.chip-name { font-weight: 500; }
.chip-hours { font-weight: 700; }
.more-chip { font-size: 10px; color: #86909c; padding: 2px 6px; cursor: pointer; border-radius: 4px; }
.more-chip:hover { color: #409eff; background: #f2f3f5; }

/* 项目条 */
.cell-prj { display: flex; gap: 4px; padding: 2px 0; }
.cell-prj-bar { width: 3px; border-radius: 2px; align-self: stretch; min-height: 24px; }
.cell-prj-info { flex: 1; min-width: 0; }
.cell-prj-top { display: flex; align-items: center; gap: 4px; }
.cell-prj-name { font-size: 12px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100px; }
.cell-prj-h { font-size: 11px; font-weight: 700; color: #409eff; margin-left: auto; }
.cell-prj-time { font-size: 10px; color: #c0c4cc; }
.cell-empty { font-size: 11px; color: #c9cdd4; text-align: center; margin-top: 16px; }

/* 图例 */
.legend { display: flex; gap: 16px; justify-content: flex-end; margin-top: 8px; }
.legend-item { font-size: 12px; color: #86909c; display: flex; align-items: center; gap: 4px; }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; }

/* 抽屉内项目 */
.drawer-prj { padding: 8px 0; border-bottom: 1px solid #f5f7fa; }
.drawer-prj:last-child { border-bottom: none; }

.detail-project-row,
.detail-task-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
}
.detail-row-prefix {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
}
.detail-manager {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
}
.detail-workload {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
.detail-hours {
  margin-left: auto;
  font-weight: 700;
  font-size: 14px;
  white-space: nowrap;
}
.detail-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  padding-left: 8px;
  border-left: 2px solid #e4e7ed;
  margin-bottom: 2px;
}
.cell-leave {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 0;
  font-size: 11px;
}
.cell-leave-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.cell-leave-label { font-weight: 600; }
.cell-leave-h { font-weight: 700; margin-left: auto; }
.drawer-leave {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #f5f7fa;
  font-size: 13px;
}
</style>
