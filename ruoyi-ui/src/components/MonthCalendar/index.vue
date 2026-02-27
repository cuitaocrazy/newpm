<template>
  <div class="month-calendar">
    <div class="mc-header">
      <div class="mc-nav">
        <el-button :icon="ArrowLeft" text size="small" @click="prevMonth" />
        <span class="mc-title">{{ displayYear }}年{{ displayMonth }}月</span>
        <el-button :icon="ArrowRight" text size="small" @click="nextMonth" />
      </div>
      <el-button text size="small" type="primary" @click="goToday">今天</el-button>
    </div>
    <table class="mc-table">
      <thead>
        <tr>
          <th v-for="w in weekHeaders" :key="w" :class="{ 'mc-wk-end': w === '六' || w === '日' }">{{ w }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(week, wi) in weeksData" :key="wi">
          <td v-for="(cell, di) in week" :key="di"
            :class="cell.tdClass"
            @click="cell.day ? onDayClick(cell) : null">
            <slot v-if="cell.day" v-bind="cell">
              <span class="mc-day-num" :class="cell.badgeClass">{{ cell.day }}</span>
            </slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  workCalendarMap: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'month-change', 'year-change'])

const todayStr = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})()

function parseYM(dateStr) {
  if (dateStr) {
    const parts = dateStr.split('-')
    if (parts.length >= 2) return { y: Number(parts[0]), m: Number(parts[1]) }
  }
  const d = new Date()
  return { y: d.getFullYear(), m: d.getMonth() + 1 }
}

const initial = parseYM(props.modelValue || todayStr)
const displayYear = ref(initial.y)
const displayMonth = ref(initial.m)

const weekHeaders = ['日', '一', '二', '三', '四', '五', '六']

function getMonthWeeks(year, month) {
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const startDow = firstDay.getDay()
  const totalDays = lastDay.getDate()
  const weeks = []
  let week = new Array(7).fill(null)
  for (let d = 1; d <= totalDays; d++) {
    const dow = (startDow + d - 1) % 7
    if (d > 1 && dow === 0) {
      weeks.push(week)
      week = new Array(7).fill(null)
    }
    week[dow] = d
  }
  weeks.push(week)
  return weeks
}

function makeDateStr(day) {
  return `${displayYear.value}-${String(displayMonth.value).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function getDayType(dateStr) {
  const wc = props.workCalendarMap[dateStr]
  if (wc) {
    if (wc.dayType === 'holiday') return 'holiday'
    if (wc.dayType === 'workday') return 'workday'
  }
  const d = new Date(dateStr)
  const dow = d.getDay()
  if (dow === 0 || dow === 6) return 'weekend'
  return 'normal'
}

const weeksData = computed(() => {
  const rawWeeks = getMonthWeeks(displayYear.value, displayMonth.value)
  return rawWeeks.map(week =>
    week.map(day => {
      if (!day) return { day: null, tdClass: 'mc-empty' }
      const dateStr = makeDateStr(day)
      const dayType = getDayType(dateStr)
      const isToday = dateStr === todayStr
      const isSelected = dateStr === props.modelValue
      const wcItem = props.workCalendarMap[dateStr] || null

      const tdClasses = [`mc-${dayType}`]
      if (isToday) tdClasses.push('mc-today')
      if (isSelected) tdClasses.push('mc-selected')

      const badgeClasses = ['mc-day-num', `mc-day-${dayType}`]
      if (isToday) badgeClasses.push('mc-day-today')

      return { day, dateStr, isToday, isSelected, dayType, wcItem, tdClass: tdClasses.join(' '), badgeClass: badgeClasses.join(' ') }
    })
  )
})

function prevMonth() {
  const oldYear = displayYear.value
  if (displayMonth.value === 1) {
    displayMonth.value = 12
    displayYear.value--
  } else {
    displayMonth.value--
  }
  if (displayYear.value !== oldYear) emit('year-change', displayYear.value)
  emitMonthChange()
}

function nextMonth() {
  const oldYear = displayYear.value
  if (displayMonth.value === 12) {
    displayMonth.value = 1
    displayYear.value++
  } else {
    displayMonth.value++
  }
  if (displayYear.value !== oldYear) emit('year-change', displayYear.value)
  emitMonthChange()
}

function goToday() {
  const d = new Date()
  const y = d.getFullYear()
  const m = d.getMonth() + 1
  const yearChanged = y !== displayYear.value
  const monthChanged = m !== displayMonth.value
  displayYear.value = y
  displayMonth.value = m
  if (yearChanged) emit('year-change', y)
  if (monthChanged || yearChanged) emitMonthChange()
  emit('update:modelValue', todayStr)
}

function emitMonthChange() {
  emit('month-change', {
    year: displayYear.value,
    month: displayMonth.value,
    yearMonth: `${displayYear.value}-${String(displayMonth.value).padStart(2, '0')}`
  })
}

function onDayClick(cell) {
  emit('update:modelValue', cell.dateStr)
}

// Sync displayed month when modelValue changes externally
watch(() => props.modelValue, (val) => {
  if (!val) return
  const { y, m } = parseYM(val)
  if (y !== displayYear.value || m !== displayMonth.value) {
    displayYear.value = y
    displayMonth.value = m
  }
})
</script>

<!-- Unscoped: day badge styles usable by parent slot content -->
<style>
.mc-day-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 4px;
  flex-shrink: 0;
}
.mc-day-normal { color: #303133; }
.mc-day-weekend { color: #909399; background: #f5f7fa; font-weight: 500; }
.mc-day-holiday { color: #f56c6c; background: #fef0f0; }
.mc-day-workday { color: #409eff; background: #ecf5ff; }
.mc-day-today { color: #fff !important; background: #409eff !important; font-weight: 700; }
</style>

<style scoped>
.mc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
  margin-bottom: 4px;
  border-bottom: 1px solid #f2f3f5;
}
.mc-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}
.mc-title {
  font-size: 15px;
  font-weight: 700;
  min-width: 100px;
  text-align: center;
  color: #1d2129;
}

.mc-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}
.mc-table th {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  padding: 6px 0;
  text-align: center;
}
.mc-table th.mc-wk-end {
  color: #f56c6c;
}
.mc-table td {
  text-align: center;
  padding: 3px 0;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s;
  vertical-align: top;
}
.mc-table td:hover:not(.mc-empty) {
  outline: 2px solid #409eff;
}
.mc-empty {
  cursor: default;
}
.mc-today {
  background: #f0f7ff;
}
.mc-selected {
  background: #ecf5ff;
  box-shadow: inset 0 0 0 2px #409eff;
}
</style>
