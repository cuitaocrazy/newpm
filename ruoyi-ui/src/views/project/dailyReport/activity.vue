<template>
  <div class="app-container">
    <!-- 查询栏 -->
    <el-form :inline="true" :model="queryParams" class="query-form">
      <el-form-item label="姓名">
        <el-select v-model="queryParams.userId" filterable clearable
          placeholder="全部人员" style="width: 180px;" @change="handleQuery">
          <el-option label="全部人员（团队概览）" :value="null" />
          <el-option v-for="u in userList" :key="u.userId"
            :label="u.nickName" :value="u.userId">
            <span>{{ u.nickName }}</span>
            <span style="color: #909399; margin-left: 8px; font-size: 12px;">{{ u.deptName }}</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="项目部门">
        <el-tree-select
          v-model="queryParams.deptId"
          :data="deptTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          placeholder="请选择项目部门"
          check-strictly
          clearable
          filterable
          style="width: 220px;"
          @change="handleQuery"
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
                @click.stop="selectPerson(person.userId)">
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
            </div>
          </div>
        </template>
      </MonthCalendar>

      <div class="legend">
        <span class="legend-item"><span class="legend-dot" style="background:#67c23a"></span> >=8h</span>
        <span class="legend-item"><span class="legend-dot" style="background:#e6a23c"></span> &lt;8h</span>
        <span class="legend-item"><span class="legend-dot" style="background:#409eff"></span> &gt;8h</span>
      </div>
    </el-card>

    <!-- 抽屉（团队模式查看某天详情） -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="560px">
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
        <div v-for="detail in person.detailList" :key="detail.detailId" class="drawer-prj">
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px;">
            <el-tag size="small" type="primary">{{ detail.projectName }}</el-tag>
            <el-tag size="small" type="info">{{ detail.projectStageName }}</el-tag>
            <span style="margin-left: auto; font-weight: 700;">{{ detail.workHours }}h</span>
          </div>
          <div style="font-size: 13px; color: #606266; line-height: 1.6; padding-left: 8px; border-left: 2px solid #e4e7ed;">
            {{ detail.workContent }}
          </div>
          <div style="font-size: 11px; color: #c0c4cc; margin-top: 3px;">
            更新于 {{ formatTime(detail.updateTime) }}
          </div>
        </div>
      </el-card>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { User } from '@element-plus/icons-vue'
import { getMonthlyReports } from '@/api/project/dailyReport'
import { getWorkCalendarByYear } from '@/api/project/workCalendar'
import { getDeptTree } from '@/api/project/project'
import { handleTree } from '@/utils/ruoyi'
import request from '@/utils/request'
import MonthCalendar from '@/components/MonthCalendar/index.vue'

const todayStr = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})()

const selectedDate = ref(todayStr)
const currentYearMonth = ref((() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
})())
const queryParams = ref({ userId: null, deptId: null })
const reportData = ref([]) // 月度完整数据
const userList = ref([])
const deptTree = ref([])
const deptFlatList = ref([])
const workCalendarMap = ref({}) // { 'yyyy-MM-dd': { dayType, dayName } }
const drawerVisible = ref(false)
const drawerTitle = ref('')
const drawerPeople = ref([])
const drawerStats = ref({ count: 0, totalHours: 0, fullCount: 0 })

const projectColors = ['#409eff', '#67c23a', '#f56c6c', '#e6a23c', '#b37feb', '#00b894', '#fdcb6e', '#909399']
const personColorMap = {}
let colorIndex = 0

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
  if (!queryParams.value.deptId) return '全部部门'
  const d = deptFlatList.value.find(x => x.deptId === queryParams.value.deptId)
  return d?.deptName || ''
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

// 按日期分组数据
const dataByDate = computed(() => {
  const map = {}
  reportData.value.forEach(r => {
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
  return {
    total: userList.value.length,
    reported: todayData.length,
    unreported: userList.value.length - todayData.length,
    avgHours: avgH
  }
})

// 个人统计
const personStats = computed(() => {
  const userReports = reportData.value.filter(r => r.userId === queryParams.value.userId)
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
  return report?.detailList || []
}

function selectPerson(userId) {
  queryParams.value.userId = userId
}

function clearPerson() {
  queryParams.value.userId = null
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
  }
}

async function loadData() {
  const params = { yearMonth: currentYearMonth.value }
  if (queryParams.value.userId) params.userId = queryParams.value.userId
  if (queryParams.value.deptId) params.deptId = queryParams.value.deptId
  const res = await getMonthlyReports(params)
  reportData.value = res.data || []
}

async function loadUsers() {
  const res = await request({ url: '/system/user/list', method: 'get', params: { pageSize: 500 } })
  userList.value = (res.rows || []).map(u => ({
    userId: u.userId,
    nickName: u.nickName,
    deptName: u.dept?.deptName || ''
  }))
}

async function loadDeptTree() {
  const response = await getDeptTree()
  deptFlatList.value = response.data
  // 过滤三级及以下部门
  const level3AndBelow = response.data.filter(dept => {
    if (!dept.ancestors) return false
    const level = dept.ancestors.split(',').length + 1
    return level >= 3
  })
  const deptData = level3AndBelow.map(dept => ({
    deptId: dept.deptId,
    parentId: dept.parentId,
    value: dept.deptId,
    label: dept.deptName
  }))
  deptTree.value = handleTree(deptData, 'deptId', 'parentId', 'children')
}

function handleQuery() { loadData() }

function handleReset() {
  queryParams.value = { userId: null, deptId: null }
  loadData()
}

// MonthCalendar 事件
function handleMonthChange({ yearMonth }) {
  currentYearMonth.value = yearMonth
  loadData()
}

function handleYearChange(year) {
  loadWorkCalendar(year)
}

onMounted(() => {
  loadWorkCalendar()
  loadUsers()
  loadDeptTree()
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
</style>
