<template>
  <div class="app-container work-calendar-page">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <div class="year-nav">
        <el-button :icon="ArrowLeft" text @click="changeYear(-1)" />
        <span class="year-title">{{ currentYear }}年工作日历</span>
        <el-button :icon="ArrowRight" text @click="changeYear(1)" />
      </div>
      <div class="legend">
        <span class="legend-item"><span class="legend-dot dot-holiday"></span> 节假日</span>
        <span class="legend-item"><span class="legend-dot dot-workday"></span> 调休上班日</span>
        <span class="legend-item"><span class="legend-dot dot-weekend"></span> 周末</span>
      </div>
      <div class="top-actions">
        <el-dropdown trigger="click" @command="handleCommand">
          <el-button type="primary">
            工作日历 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="addHoliday">新增节假日</el-dropdown-item>
              <el-dropdown-item command="addWorkday">新增调休上班日</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 12个月日历网格 -->
    <div class="months-grid">
      <div v-for="month in 12" :key="month" class="month-card">
        <div class="month-title">{{ month }}月</div>
        <table class="mini-cal">
          <thead>
            <tr>
              <th v-for="w in weekHeaders" :key="w" :class="{ 'wk-head': w === '六' || w === '日' }">{{ w }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(week, wi) in getMonthWeeks(month)" :key="wi">
              <td v-for="(day, di) in week" :key="di"
                :class="getDayClass(day, month)"
                @click="day ? handleDayClick(day, month) : null">
                <span v-if="day" class="day-num">{{ day }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px" append-to-body>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="日期" prop="calendarDate">
          <el-date-picker v-model="form.calendarDate" type="date" value-format="YYYY-MM-DD"
            placeholder="选择日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="类型" prop="dayType">
          <el-radio-group v-model="form.dayType">
            <el-radio value="holiday">节假日</el-radio>
            <el-radio value="workday">调休上班日</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="dayName">
          <el-input v-model="form.dayName" placeholder="如：春节、元旦调休" maxlength="50" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="form.id" type="danger" plain @click="handleDelete">删除</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WorkCalendar">
import { ref, onMounted, watch, nextTick } from 'vue'
import { ArrowLeft, ArrowRight, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkCalendarByYear, addWorkCalendar, updateWorkCalendar, delWorkCalendar } from '@/api/project/workCalendar'

const currentYear = ref(new Date().getFullYear())
const calendarData = ref([]) // 当年所有特殊日期
const weekHeaders = ['日', '一', '二', '三', '四', '五', '六']

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const form = ref({
  id: null,
  calendarDate: '',
  dayType: 'holiday',
  dayName: '',
  remark: ''
})
const rules = {
  calendarDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  dayType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

// 构建日期 → 数据的映射
function buildDateMap() {
  const map = {}
  calendarData.value.forEach(item => {
    const dateStr = item.calendarDate?.substring(0, 10)
    if (dateStr) map[dateStr] = item
  })
  return map
}

// 获取某月的周数据（二维数组）
function getMonthWeeks(month) {
  const firstDay = new Date(currentYear.value, month - 1, 1)
  const lastDay = new Date(currentYear.value, month, 0)
  const startDow = firstDay.getDay() // 0=周日
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

// 获取日期的 CSS class
function getDayClass(day, month) {
  if (!day) return 'empty'

  const dateStr = `${currentYear.value}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  const dateMap = buildDateMap()
  const item = dateMap[dateStr]

  const date = new Date(currentYear.value, month - 1, day)
  const dow = date.getDay()
  const isWeekend = dow === 0 || dow === 6

  if (item) {
    if (item.dayType === 'holiday') return 'day-holiday'
    if (item.dayType === 'workday') return 'day-workday'
  }

  if (isWeekend) return 'day-weekend'
  return 'day-normal'
}

// 点击日期
function handleDayClick(day, month) {
  const dateStr = `${currentYear.value}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  const dateMap = buildDateMap()
  const existing = dateMap[dateStr]

  if (existing) {
    // 编辑已有记录
    dialogTitle.value = '编辑工作日历'
    form.value = {
      id: existing.id,
      calendarDate: existing.calendarDate?.substring(0, 10),
      dayType: existing.dayType,
      dayName: existing.dayName || '',
      remark: existing.remark || ''
    }
  } else {
    // 新增
    dialogTitle.value = '新增工作日历'
    form.value = {
      id: null,
      calendarDate: dateStr,
      dayType: 'holiday',
      dayName: '',
      remark: ''
    }
  }
  dialogVisible.value = true
}

// 下拉菜单命令
function handleCommand(cmd) {
  if (cmd === 'addHoliday') {
    dialogTitle.value = '新增节假日'
    form.value = { id: null, calendarDate: '', dayType: 'holiday', dayName: '', remark: '' }
    dialogVisible.value = true
  } else if (cmd === 'addWorkday') {
    dialogTitle.value = '新增调休上班日'
    form.value = { id: null, calendarDate: '', dayType: 'workday', dayName: '', remark: '' }
    dialogVisible.value = true
  }
}

// 提交
async function handleSubmit() {
  await formRef.value.validate()
  if (form.value.id) {
    await updateWorkCalendar(form.value)
    ElMessage.success('修改成功')
  } else {
    await addWorkCalendar(form.value)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

// 删除
async function handleDelete() {
  await ElMessageBox.confirm('确认删除该日期的工作日历记录？', '提示', { type: 'warning' })
  await delWorkCalendar(form.value.id)
  ElMessage.success('删除成功')
  dialogVisible.value = false
  loadData()
}

// 切换年份
function changeYear(delta) {
  currentYear.value += delta
}

// 加载数据
async function loadData() {
  const res = await getWorkCalendarByYear(currentYear.value)
  calendarData.value = res.data || []
}

watch(currentYear, () => loadData())

onMounted(() => loadData())
</script>

<style scoped>
.work-calendar-page {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.year-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}
.year-title {
  font-size: 18px;
  font-weight: 700;
  min-width: 140px;
  text-align: center;
}

.legend {
  display: flex;
  gap: 20px;
  align-items: center;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}
.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}
.dot-holiday { background: #fde2e2; border: 1px solid #f56c6c; }
.dot-workday { background: #d9ecff; border: 1px solid #409eff; }
.dot-weekend { background: #f5f7fa; border: 1px solid #dcdfe6; }

/* 12个月网格 */
.months-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.month-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
}

.month-title {
  font-size: 15px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 8px;
  color: #303133;
}

/* 迷你日历表格 */
.mini-cal {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}
.mini-cal th {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  padding: 4px 0;
  text-align: center;
}
.mini-cal th.wk-head {
  color: #f56c6c;
}
.mini-cal td {
  text-align: center;
  padding: 3px 0;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s;
}
.mini-cal td:hover:not(.empty) {
  outline: 2px solid #409eff;
}

.day-num {
  display: inline-block;
  width: 26px;
  height: 26px;
  line-height: 26px;
  font-size: 13px;
  border-radius: 4px;
}

/* 日期类型样式 */
.day-normal .day-num {
  color: #303133;
}
.day-weekend .day-num {
  color: #909399;
  background: #f5f7fa;
}
.day-holiday .day-num {
  color: #f56c6c;
  background: #fef0f0;
  font-weight: 600;
}
.day-workday .day-num {
  color: #409eff;
  background: #ecf5ff;
  font-weight: 600;
}
.empty {
  cursor: default;
}

/* 响应式：小屏幕2列 */
@media (max-width: 1200px) {
  .months-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 900px) {
  .months-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
