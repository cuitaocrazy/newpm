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
          @clear="() => { queryParams.projectId = null; handleQuery() }"
        />
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker v-model="queryParams.reportDateStart" type="date"
          placeholder="开始日期" value-format="YYYY-MM-DD" style="width: 140px;" clearable />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker v-model="queryParams.reportDateEnd" type="date"
          placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 140px;" clearable />
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
          <div style="font-size: 13px; color: #909399;">点击日历格子中的人员或"+N人"可查看日报详情；点击日期格可切换已填写/未填写汇总，点击数字可查看人员明细</div>
        </div>
        <div style="display: flex; gap: 24px;">
          <div class="stat-box"><div class="stat-num">{{ teamStats.total }}</div><div class="stat-label">团队人数</div></div>
          <div class="stat-box stat-clickable" @click="openStatsDialog('filled')">
            <div class="stat-num" style="color: #67c23a;">{{ teamStats.reported }}</div>
            <div class="stat-label">已填写 · {{ focusedDateLabel }}</div>
          </div>
          <div class="stat-box stat-clickable" @click="openStatsDialog('unfilled')">
            <div class="stat-num" style="color: #f56c6c;">{{ teamStats.unreported }}</div>
            <div class="stat-label">未填写 · {{ focusedDateLabel }}</div>
          </div>
          <div class="stat-box"><div class="stat-num">{{ teamStats.avgHours }}h</div><div class="stat-label">{{ focusedDateLabel }} 人均</div></div>
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
        <el-button text type="primary" @click="personDrawerVisible = true">查看全部日报</el-button>
        <el-button text type="primary" @click="clearPerson">返回团队概览</el-button>
      </div>
    </el-card>

    <!-- 日历 -->
    <el-card shadow="hover">
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

    <!-- 已填/未填人员详情弹框 -->
    <el-dialog v-model="statsDialogVisible" :title="statsDialogTitle" width="680px" destroy-on-close>
      <el-table :data="statsDialogRows" border stripe size="small" max-height="500">
        <el-table-column label="用户昵称" prop="nickName" width="110" />
        <el-table-column label="所属机构" prop="deptName" width="180" />
        <el-table-column label="实际工作量（小时）" width="140" align="center">
          <template #default="{ row }">
            <span v-if="row.totalWorkHours != null" :style="{ color: row.totalWorkHours >= 8 ? '#67c23a' : '#e6a23c', fontWeight: 700 }">
              {{ row.totalWorkHours }}h
            </span>
            <span v-else style="color: #c0c4cc;">—</span>
          </template>
        </el-table-column>
        <el-table-column label="关联项目" min-width="220">
          <template #default="{ row }">
            <template v-if="row.projects && row.projects.length">
              <el-tag v-for="p in row.projects" :key="p" size="small" type="info" style="margin: 2px 2px 2px 0;">{{ p }}</el-tag>
            </template>
            <span v-else style="color: #c0c4cc;">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 抽屉（团队模式查看某天详情） -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="1000px">
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
          <div class="detail-project-row">
            <el-tag v-if="detail.revenueConfirmYear" size="small" type="warning" :style="yearTagStyle(detail.revenueConfirmYear)">
              {{ getDictLabel(sys_ndgl, detail.revenueConfirmYear) }}
            </el-tag>
            <span v-if="detail.projectManagerName" class="detail-manager">{{ detail.projectManagerName }}</span>
            <el-link type="primary" class="detail-link" @click="goToProject(detail.projectId)">{{ detail.projectName }}</el-link>
            <el-tag size="small" type="info">{{ detail.projectStageName }}</el-tag>
            <span class="detail-workload">预计 <strong>{{ detail.estimatedWorkload != null ? detail.estimatedWorkload : '-' }}</strong> 天</span>
            <span class="detail-workload">已花 <strong>{{ detail.actualWorkload != null ? Number(detail.actualWorkload).toFixed(3) : '-' }}</strong> 天</span>
            <template v-if="detail.workCategory">
              <dict-tag v-for="cat in detail.workCategory.split(',')" :key="cat" :options="sys_gzlb" :value="cat.trim()" />
            </template>
            <span class="detail-hours">{{ detail.workHours }}h</span>
          </div>
          <!-- 任务行：仅有 subProjectId 时显示 -->
          <div v-if="detail.subProjectId" class="detail-task-row">
            <el-tag v-if="detail.subProjectBatchNo" size="small" type="info">{{ detail.subProjectBatchNo }}</el-tag>
            <span v-if="detail.subProjectManagerName" class="detail-manager">{{ detail.subProjectManagerName }}</span>
            <el-link type="success" class="detail-link" @click="goToSubproject(detail.subProjectId)">{{ detail.subProjectName }}</el-link>
            <el-tag v-if="detail.subProjectStage" size="small" type="warning">{{ getStageName(detail.subProjectStage) }}</el-tag>
            <span class="detail-workload">预计 <strong>{{ detail.subProjectEstimatedWorkload != null ? detail.subProjectEstimatedWorkload : '-' }}</strong> 天</span>
            <span class="detail-workload">已花 <strong>{{ detail.subProjectActualWorkload != null ? Number(detail.subProjectActualWorkload).toFixed(3) : '-' }}</strong> 天</span>
            <dict-tag v-for="cat in detail.workCategory.split(',')" :key="cat" :options="sys_gzlb" :value="cat.trim()" v-if="detail.workCategory" />
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

    <!-- 个人日报抽屉（选人后打开） -->
    <el-drawer v-model="personDrawerVisible" :title="personDrawerTitle" size="1000px">
      <div style="display: flex; gap: 24px; padding: 14px 16px; background: #f5f7fa; border-radius: 8px; margin-bottom: 16px;">
        <div class="stat-box"><div class="stat-num">{{ personStats.days }}</div><div class="stat-label">已填报天</div></div>
        <div class="stat-box"><div class="stat-num">{{ personStats.totalHours }}h</div><div class="stat-label">累计工时</div></div>
        <div class="stat-box"><div class="stat-num">{{ personStats.avgHours }}h</div><div class="stat-label">日均工时</div></div>
        <div class="stat-box"><div class="stat-num">{{ personStats.fullDays }}/{{ personStats.days }}</div><div class="stat-label">满勤(>=8h)</div></div>
      </div>
      <div v-if="sortedReportList.length === 0" style="text-align:center;color:#909399;padding:40px 0;">
        暂无日报记录
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
          <div class="detail-project-row">
            <el-tag v-if="detail.revenueConfirmYear" size="small" type="warning" :style="yearTagStyle(detail.revenueConfirmYear)">
              {{ getDictLabel(sys_ndgl, detail.revenueConfirmYear) }}
            </el-tag>
            <span v-if="detail.projectManagerName" class="detail-manager">{{ detail.projectManagerName }}</span>
            <el-link type="primary" class="detail-link" @click="goToProject(detail.projectId)">{{ detail.projectName }}</el-link>
            <el-tag size="small" type="info">{{ detail.projectStageName }}</el-tag>
            <span class="detail-workload">预计 <strong>{{ detail.estimatedWorkload != null ? detail.estimatedWorkload : '-' }}</strong> 天</span>
            <span class="detail-workload">已花 <strong>{{ detail.actualWorkload != null ? Number(detail.actualWorkload).toFixed(3) : '-' }}</strong> 天</span>
            <template v-if="detail.workCategory">
              <dict-tag v-for="cat in detail.workCategory.split(',')" :key="cat" :options="sys_gzlb" :value="cat.trim()" />
            </template>
            <span class="detail-hours">{{ detail.workHours }}h</span>
          </div>
          <div v-if="detail.subProjectId" class="detail-task-row">
            <el-tag v-if="detail.subProjectBatchNo" size="small" type="info">{{ detail.subProjectBatchNo }}</el-tag>
            <span v-if="detail.subProjectManagerName" class="detail-manager">{{ detail.subProjectManagerName }}</span>
            <el-link type="success" class="detail-link" @click="goToSubproject(detail.subProjectId)">{{ detail.subProjectName }}</el-link>
            <el-tag v-if="detail.subProjectStage" size="small" type="warning">{{ getStageName(detail.subProjectStage) }}</el-tag>
            <span class="detail-workload">预计 <strong>{{ detail.subProjectEstimatedWorkload != null ? detail.subProjectEstimatedWorkload : '-' }}</strong> 天</span>
            <span class="detail-workload">已花 <strong>{{ detail.subProjectActualWorkload != null ? Number(detail.subProjectActualWorkload).toFixed(3) : '-' }}</strong> 天</span>
            <dict-tag v-for="cat in detail.workCategory.split(',')" :key="cat" :options="sys_gzlb" :value="cat.trim()" v-if="detail.workCategory" />
          </div>
          <div class="detail-content" v-html="formatWorkContent(detail.workContent)"></div>
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
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { User } from '@element-plus/icons-vue'
import { getMonthlyReports, getProjectNameSuggestions } from '@/api/project/dailyReport'
import { getWorkCalendarByYear } from '@/api/project/workCalendar'
import request from '@/utils/request'
import MonthCalendar from '@/components/MonthCalendar/index.vue'

const { proxy } = getCurrentInstance()
const { sys_ndgl, sys_gzlb, sys_xmjd } = proxy.useDict('sys_ndgl', 'sys_gzlb', 'sys_xmjd')
const router = useRouter()
function goToProject(projectId) { if (projectId) router.push(`/project/list/detail/${projectId}`) }
function goToSubproject(subProjectId) { if (subProjectId) router.push(`/task/subproject/detail/${subProjectId}`) }

const todayStr = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})()

const selectedDate = ref(todayStr)
const currentYearMonth = ref((() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
})())
const queryParams = ref({ userId: '', deptId: null, projectName: null, projectId: null, reportDateStart: null, reportDateEnd: null })
const reportData = ref([]) // 月度完整数据
const userList = ref([])
const projectDeptSelectRef = ref(null)
const workCalendarMap = ref({}) // { 'yyyy-MM-dd': { dayType, dayName } }
const drawerVisible = ref(false)
const drawerTitle = ref('')
const drawerPeople = ref([])
const drawerStats = ref({ count: 0, totalHours: 0, fullCount: 0 })
const personDrawerVisible = ref(false)
const personDrawerTitle = computed(() => selectedUserName.value ? `${selectedUserName.value} 的全部日报` : '')

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

// 当前聚焦日期（驱动已填/未填统计，默认今天）
const focusedDate = ref(todayStr)
const statsDialogVisible = ref(false)
const statsDialogType = ref('filled') // 'filled' | 'unfilled'
const statsDialogRows = ref([])

const focusedDateLabel = computed(() => {
  if (focusedDate.value === todayStr) return '今日'
  const d = new Date(focusedDate.value + 'T00:00:00')
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getMonth() + 1}月${d.getDate()}日(周${weekdays[d.getDay()]})`
})

const statsDialogTitle = computed(() => {
  const label = focusedDateLabel.value
  const count = statsDialogRows.value.length
  return statsDialogType.value === 'filled'
    ? `${label} 已填写日报（${count} 人）`
    : `${label} 未填写日报（${count} 人）`
})

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

// 团队统计（基于 focusedDate，点日历格子时动态更新）
const teamStats = computed(() => {
  const focusedData = dataByDate.value[focusedDate.value] || []
  const focusedTotalH = focusedData.reduce((s, r) => s + Number(r.totalWorkHours || 0), 0)
  const avgH = focusedData.length > 0 ? (focusedTotalH / focusedData.length).toFixed(1) : '0.0'
  const total = queryParams.value.projectName
    ? new Set(displayReportData.value.map(r => r.userId)).size
    : userList.value.length
  return {
    total,
    reported: focusedData.length,
    unreported: total - focusedData.length,
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
  focusedDate.value = dateStr
}

function openStatsDialog(type) {
  statsDialogType.value = type
  statsDialogRows.value = getStatsDialogRows(type)
  statsDialogVisible.value = true
}

/**
 * TODO(human): 将当前 focusedDate 的已填/未填数据转换为弹框展示行
 * type: 'filled' | 'unfilled'
 * 返回: Array<{ nickName: string, deptName: string, projects: string[] }>
 *
 * 提示：
 * - 已填人员来自 dataByDate.value[focusedDate.value]，每人有 detailList
 * - 未填人员 = userList.value 中不在已填集合里的人（按 userId 判断）
 * - projects 字段：已填人员取当日 detailList 中的 projectName（去重），未填为 []
 * - 只统计 work 类型条目（entryType 为空或 'work'），排除请假等
 */
function getStatsDialogRows(type) {
  const filled = dataByDate.value[focusedDate.value] || []
  if (type === 'filled') {
    return filled.map(person => {
      const projects = [...new Set(
        (person.detailList || [])
          .filter(d => !d.entryType || d.entryType === 'work')
          .map(d => d.projectName)
          .filter(Boolean)
      )]
      return { nickName: person.nickName, deptName: person.deptName, projects, totalWorkHours: person.totalWorkHours }
    })
  } else {
    const filledIds = new Set(filled.map(r => r.userId))
    return userList.value
      .filter(u => !filledIds.has(u.userId))
      .map(u => ({ nickName: u.nickName, deptName: u.deptName, projects: [], totalWorkHours: null }))
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
  if (queryParams.value.projectId) {
    params.projectId = queryParams.value.projectId
  } else if (queryParams.value.projectName) {
    params.projectName = queryParams.value.projectName
  }

  if (listMode.value) {
    if (queryParams.value.reportDateStart) params.reportDateStart = queryParams.value.reportDateStart
    if (queryParams.value.reportDateEnd) params.reportDateEnd = queryParams.value.reportDateEnd
  } else {
    params.yearMonth = currentYearMonth.value
  }

  const res = await getMonthlyReports(params)
  reportData.value = res.data || []
}

async function loadUsers() {
  const params = {}
  if (queryParams.value.projectId) {
    params.projectId = queryParams.value.projectId
  } else {
    if (queryParams.value.deptId) params.deptId = queryParams.value.deptId
  }
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
  queryParams.value = { userId: '', deptId: null, projectName: null, projectId: null, reportDateStart: null, reportDateEnd: null }
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
  queryParams.value.projectId = item.projectId || null
  handleQuery()
}

// MonthCalendar 事件
function handleMonthChange({ yearMonth }) {
  currentYearMonth.value = yearMonth
  // 切换月份时，聚焦日期重置为：当月则今天，否则该月第一天
  const todayYM = todayStr.substring(0, 7)
  focusedDate.value = yearMonth === todayYM ? todayStr : `${yearMonth}-01`
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
.stat-clickable { cursor: pointer; border-radius: 8px; transition: background 0.15s, transform 0.1s; }
.stat-clickable:hover { background: #f0f7ff; transform: scale(1.05); }

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
  flex-wrap: nowrap;
  gap: 6px;
  margin-bottom: 4px;
  overflow: hidden;
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
.detail-link {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 300px;
  flex-shrink: 1;
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
/* 项目名 tag 可收缩截断，避免撑破行 */
.detail-project-row .el-tag:nth-child(2),
.detail-task-row .el-tag:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 300px;
  flex-shrink: 1;
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
