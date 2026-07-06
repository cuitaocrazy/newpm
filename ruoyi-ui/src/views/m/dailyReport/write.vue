<template>
  <div class="mdr">
    <van-nav-bar title="日报填写" fixed placeholder safe-area-inset-top />

    <!-- 白名单豁免（FR-009）：不渲染表单与保存栏 -->
    <van-empty v-if="isWhitelisted" image="search" description="您在日报豁免名单中，无需填写日报" />

    <template v-else>
      <!-- 本周日期切换条（US3）：仅提供本周一~周日入口 -->
      <div class="mdr-week">
        <div
          v-for="d in weekDays"
          :key="d.date"
          class="mdr-week__day"
          :class="{ 'is-active': d.date === selectedDate, 'is-today': d.isToday }"
          @click="selectedDate = d.date"
        >
          <span class="mdr-week__label">{{ d.label }}</span>
          <span class="mdr-week__num">{{ d.dayNum }}</span>
          <span class="mdr-week__dot" :class="{ 'is-filled': d.hasReport }"></span>
        </div>
      </div>

      <!-- 只读提示（US3/FR-009）：防 URL 直达越界日期 -->
      <van-notice-bar
        v-if="!isEditable"
        wrapable
        :scrollable="false"
        text="仅可填写本周（周一至周日）的日报，当前所选日期为只读"
      />

      <!-- 汇总条（FR-008）：工作 + 假期合计，≥8h 达标 -->
      <div class="mdr-summary">
        <span class="mdr-summary__date">{{ selectedDateText }}</span>
        <van-tag :type="totalHours >= 8 ? 'success' : 'warning'" size="large" v-if="totalHours > 0">
          合计 {{ totalHours }}h（工时 {{ totalWorkHours }}h<template v-if="totalLeaveHours > 0"> + 假期 {{ totalLeaveHours }}h</template>）
        </van-tag>
        <van-tag type="default" size="large" v-else>尚未填写</van-tag>
      </div>

      <!-- 项目名过滤（FR-004） -->
      <van-search v-model="filterKeyword" placeholder="搜索项目名称" />

      <van-loading v-if="loading" class="mdr-loading" vertical>加载中...</van-loading>

      <template v-else>
        <!-- 空态 -->
        <van-empty v-if="formList.length === 0" description="暂无参与的项目，请联系项目经理" />
        <van-empty v-else-if="filteredList.length === 0" description="没有匹配的项目" />

        <!-- 项目卡片（FR-005） -->
        <div v-for="item in filteredList" :key="item.projectId" class="mdr-card">
          <div class="mdr-card__header">
            <span class="mdr-card__title">{{ item.projectName }}</span>
            <van-tag plain type="primary" v-if="item.projectStageName">{{ item.projectStageName }}</van-tag>
          </div>

          <!-- 普通项目：单行工时 + 类别 + 内容 -->
          <template v-if="!item.hasSubProject">
            <van-cell-group :border="false">
              <van-cell title="工时" class="mdr-stepper-cell">
                <van-stepper
                  v-model="item.workHours"
                  :min="0" :max="24" :step="0.5"
                  :decimal-length="1"
                  input-width="48px" button-size="28px"
                  :disabled="!isEditable"
                />
              </van-cell>
              <van-field
                :model-value="categoryText(item.workCategory)"
                is-link readonly
                label="类别"
                placeholder="请选择工作任务类型（必填）"
                @click="isEditable && openCategoryPicker(item)"
              />
              <van-field
                v-model="item.workContent"
                type="textarea" rows="2" autosize
                label="内容"
                placeholder="今天做了什么…"
                maxlength="500" show-word-limit
                :disabled="!isEditable"
              />
            </van-cell-group>
          </template>

          <!-- 含子任务项目：任务分行（急加载，与桌面一致） -->
          <template v-else>
            <van-loading v-if="item.taskRows === null" size="18" class="mdr-card__tasks-loading">任务加载中...</van-loading>
            <van-collapse v-else v-model="expandedCards" :border="false">
              <van-collapse-item
                :name="String(item.projectId)"
                :title="`任务（${item.taskRows.length}）`"
                :value="taskFilledSummary(item)"
              >
                <div v-for="t in item.taskRows" :key="t.subProjectId" class="mdr-task">
                  <div class="mdr-task__name">{{ t.taskName }}</div>
                  <van-cell-group :border="false">
                    <van-cell title="工时" class="mdr-stepper-cell">
                      <van-stepper
                        v-model="t.workHours"
                        :min="0" :max="24" :step="0.5"
                        :decimal-length="1"
                        input-width="48px" button-size="28px"
                        :disabled="!isEditable"
                      />
                    </van-cell>
                    <van-field
                      :model-value="categoryText(t.workCategory)"
                      is-link readonly
                      label="类别"
                      placeholder="请选择工作任务类别（必填）"
                      @click="isEditable && openCategoryPicker(t)"
                    />
                    <van-field
                      v-model="t.workContent"
                      type="textarea" rows="2" autosize
                      label="内容"
                      placeholder="该任务做了什么…"
                      maxlength="500" show-word-limit
                      :disabled="!isEditable"
                    />
                  </van-cell-group>
                </div>
              </van-collapse-item>
            </van-collapse>
          </template>
        </div>

        <!-- 假期区（US2/FR-006） -->
        <div class="mdr-card">
          <div class="mdr-card__header">
            <span class="mdr-card__title">假期记录</span>
            <van-button
              size="small" plain type="primary" icon="plus"
              :disabled="!isEditable"
              @click="leaveList.push({ entryType: 'leave', leaveHours: 1, remark: '' })"
            >添加假期</van-button>
          </div>
          <van-empty v-if="leaveList.length === 0" image-size="48" description="今日无假期记录" />
          <div v-for="(l, idx) in leaveList" :key="idx" class="mdr-leave">
            <van-cell-group :border="false">
              <van-field
                :model-value="leaveTypeLabel(l.entryType)"
                is-link readonly
                label="类型"
                placeholder="请选择假期类型"
                @click="isEditable && openLeaveTypePicker(idx)"
              >
                <template #button>
                  <van-icon name="delete-o" class="mdr-leave__del" @click.stop="isEditable && leaveList.splice(idx, 1)" />
                </template>
              </van-field>
              <van-cell title="小时" class="mdr-stepper-cell">
                <van-stepper
                  v-model="l.leaveHours"
                  :min="0" :max="24" :step="0.5"
                  :decimal-length="1"
                  input-width="48px" button-size="28px"
                  :disabled="!isEditable"
                />
              </van-cell>
              <van-field
                v-model="l.remark"
                label="备注"
                placeholder="可选"
                maxlength="100"
                :disabled="!isEditable"
              />
            </van-cell-group>
          </div>
        </div>

        <!-- 底部占位，避免内容被固定保存栏遮挡 -->
        <div class="mdr-bottom-spacer"></div>
      </template>

      <!-- 固定底部保存栏 -->
      <div class="mdr-savebar">
        <van-button
          round block type="primary"
          :loading="saving" loading-text="保存中..."
          :disabled="!isEditable"
          @click="handleSave"
        >保存日报</van-button>
      </div>
    </template>

    <!-- 工作类别多选 Popup（数据源 sys_gzlb，Constitution VI：useDict 取数不硬编码） -->
    <van-popup v-model:show="categoryPicker.show" position="bottom" round safe-area-inset-bottom>
      <div class="mdr-popup__bar">
        <span class="mdr-popup__btn" @click="categoryPicker.show = false">取消</span>
        <span class="mdr-popup__title">工作任务类别（可多选）</span>
        <span class="mdr-popup__btn is-confirm" @click="confirmCategory">确定</span>
      </div>
      <van-checkbox-group v-model="categoryPicker.selected" class="mdr-popup__body">
        <van-cell-group :border="false">
          <van-cell
            v-for="d in sys_gzlb"
            :key="d.value"
            :title="d.label"
            clickable
            @click="toggleCategory(d.value)"
          >
            <template #right-icon>
              <van-checkbox :name="d.value" shape="square" @click.stop />
            </template>
          </van-cell>
        </van-cell-group>
      </van-checkbox-group>
    </van-popup>

    <!-- 假期类型 Picker（数据源 sys_rbtype 非 work 项） -->
    <van-popup v-model:show="leaveTypePicker.show" position="bottom" round safe-area-inset-bottom>
      <van-picker
        :columns="leaveTypeColumns"
        title="假期类型"
        @confirm="onLeaveTypeConfirm"
        @cancel="leaveTypePicker.show = false"
      />
    </van-popup>
  </div>
</template>

<script setup name="MobileDailyReportWrite">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  NavBar as VanNavBar,
  NoticeBar as VanNoticeBar,
  Tag as VanTag,
  Search as VanSearch,
  Loading as VanLoading,
  Empty as VanEmpty,
  Stepper as VanStepper,
  Field as VanField,
  Cell as VanCell,
  CellGroup as VanCellGroup,
  Button as VanButton,
  Popup as VanPopup,
  Picker as VanPicker,
  Checkbox as VanCheckbox,
  CheckboxGroup as VanCheckboxGroup,
  Collapse as VanCollapse,
  CollapseItem as VanCollapseItem,
  Icon as VanIcon,
  showToast,
  showSuccessToast
} from 'vant'
import { getMyReport, getMyProjects, saveDailyReport, listDailyReport } from '@/api/project/dailyReport'
import { checkSelfInWhitelist } from '@/api/project/whitelist'
import { getTaskOptions } from '@/api/project/task'
import { useDict } from '@/utils/dict'
import useUserStore from '@/store/modules/user'

const { sys_gzlb, sys_rbtype } = useDict('sys_gzlb', 'sys_rbtype')
const userStore = useUserStore()
const route = useRoute()

// ===== 日期与本周约束（对齐桌面 write.vue weekBounds 逻辑） =====
function formatDateStr(date) {
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

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

// 支持 URL query 直达指定日期（?date=YYYY-MM-DD），越界日期进入只读态
const initDate = /^\d{4}-\d{2}-\d{2}$/.test(String(route.query.date || ''))
  ? String(route.query.date)
  : formatDateStr(new Date())
const selectedDate = ref(initDate)

const isEditable = computed(() => {
  return selectedDate.value >= weekBounds.start && selectedDate.value <= weekBounds.end
})

const selectedDateText = computed(() => {
  const d = new Date(selectedDate.value)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${selectedDate.value} ${weekdays[d.getDay()]}`
})

// 本周 7 天 chip（US3）
const weekReportMap = ref({})
const weekDays = computed(() => {
  const labels = ['一', '二', '三', '四', '五', '六', '日']
  const days = []
  const start = new Date(weekBounds.start)
  const todayStr = formatDateStr(new Date())
  for (let i = 0; i < 7; i++) {
    const d = new Date(start)
    d.setDate(start.getDate() + i)
    const dateStr = formatDateStr(d)
    days.push({
      date: dateStr,
      label: labels[i],
      dayNum: d.getDate(),
      isToday: dateStr === todayStr,
      hasReport: !!weekReportMap.value[dateStr]
    })
  }
  return days
})

// ===== 状态 =====
const isWhitelisted = ref(false)
const loading = ref(false)
const saving = ref(false)
const projects = ref([])
const formList = ref([])
const leaveList = ref([])
const currentReportId = ref(null)
const filterKeyword = ref('')
const expandedCards = ref([])

const filteredList = computed(() => {
  const kw = filterKeyword.value.trim().toLowerCase()
  if (!kw) return formList.value
  return formList.value.filter(item => (item.projectName || '').toLowerCase().includes(kw))
})

// 汇总（FR-008）：工作工时 + 假期小时，≥8h 达标
// 注意：van-stepper 配 decimal-length 时 v-model 回写字符串（如 "2.0"），运算必须 Number() 强转
const totalWorkHours = computed(() => {
  return formList.value.reduce((sum, item) => {
    if (item.hasSubProject && item.taskRows) {
      return sum + item.taskRows.reduce((s, t) => s + (Number(t.workHours) || 0), 0)
    }
    return sum + (Number(item.workHours) || 0)
  }, 0)
})
const totalLeaveHours = computed(() => {
  return leaveList.value.reduce((sum, l) => sum + (l.entryType && Number(l.leaveHours) > 0 ? Number(l.leaveHours) : 0), 0)
})
const totalHours = computed(() => totalWorkHours.value + totalLeaveHours.value)

// ===== 数据加载（逻辑对齐桌面 loadDayReport / loadTaskRows） =====
async function loadProjects() {
  const res = await getMyProjects()
  projects.value = res.data || []
}

async function loadTaskRows(item) {
  if (item.taskRows !== null && item.taskRows !== undefined) return
  const res = await getTaskOptions(item.projectId)
  const tasks = res.data || []
  item.taskRows = tasks.map(t => {
    const existingDetail = (item._existingDetails || []).find(d => d.subProjectId === t.taskId)
    return {
      subProjectId: t.taskId,
      taskName: t.taskName,
      workCategory: existingDetail?.workCategory
        ? existingDetail.workCategory.split(',').map(s => s.trim()).filter(Boolean)
        : [],
      workHours: existingDetail ? Number(existingDetail.workHours) : 0,
      workContent: existingDetail?.workContent || ''
    }
  })
}

async function loadDayReport(dateStr) {
  loading.value = true
  try {
    const res = await getMyReport(dateStr)
    const report = res.data
    currentReportId.value = report?.reportId || null

    formList.value = projects.value.map(p => {
      if (p.hasSubProject) {
        const existingDetails = (report?.detailList || []).filter(
          d => d.projectId === p.projectId && (!d.entryType || d.entryType === 'work') && d.subProjectId != null
        )
        return {
          projectId: p.projectId,
          projectName: p.projectName,
          projectCode: p.projectCode,
          projectStage: p.projectStage,
          projectStageName: p.projectStageName,
          hasSubProject: true,
          _existingDetails: existingDetails,
          taskRows: null
        }
      } else {
        const detail = report?.detailList?.find(d => d.projectId === p.projectId && (!d.entryType || d.entryType === 'work'))
        return {
          projectId: p.projectId,
          projectName: p.projectName,
          projectCode: p.projectCode,
          projectStage: detail?.projectStage || p.projectStage,
          projectStageName: detail?.projectStageName || p.projectStageName,
          hasSubProject: false,
          workHours: detail ? Number(detail.workHours) : 0,
          workContent: detail ? detail.workContent : '',
          workCategory: detail?.workCategory
            ? detail.workCategory.split(',').map(s => s.trim()).filter(Boolean)
            : []
        }
      }
    })

    // 与桌面一致：对所有含子任务项目急加载 taskRows（guard 防重复）
    const subProjectItems = formList.value.filter(item => item.hasSubProject)
    await Promise.all(subProjectItems.map(item => loadTaskRows(item)))

    // 默认展开已有工时的任务卡片
    expandedCards.value = subProjectItems
      .filter(item => (item.taskRows || []).some(t => t.workHours > 0))
      .map(item => String(item.projectId))

    leaveList.value = (report?.detailList || [])
      .filter(d => d.entryType && d.entryType !== 'work')
      .map(d => ({
        entryType: d.entryType,
        leaveHours: parseFloat(d.leaveHours || d.workHours) || 0,
        remark: d.remark || ''
      }))
  } finally {
    loading.value = false
  }
}

// 本周已填标记：按周跨到的月份查询（可能跨 1~2 个月）
async function loadWeekOverview() {
  const months = [...new Set([weekBounds.start.substring(0, 7), weekBounds.end.substring(0, 7)])]
  const results = await Promise.all(
    months.map(ym => listDailyReport({ yearMonth: ym, userId: userStore.id, pageNum: 1, pageSize: 31 }).catch(() => ({ rows: [] })))
  )
  const map = {}
  results.forEach(res => {
    ;(res.rows || []).forEach(r => {
      const day = r.reportDate?.substring(0, 10)
      if (day) map[day] = true
    })
  })
  weekReportMap.value = map
}

// ===== 工作类别多选 Popup =====
const categoryPicker = reactive({ show: false, target: null, selected: [] })

function categoryText(arr) {
  if (!arr || !arr.length) return ''
  return arr
    .map(v => (sys_gzlb.value.find(d => d.value === v) || {}).label || v)
    .join('、')
}

function openCategoryPicker(target) {
  categoryPicker.target = target
  categoryPicker.selected = [...(target.workCategory || [])]
  categoryPicker.show = true
}

function toggleCategory(value) {
  const i = categoryPicker.selected.indexOf(value)
  if (i >= 0) categoryPicker.selected.splice(i, 1)
  else categoryPicker.selected.push(value)
}

function confirmCategory() {
  if (categoryPicker.target) categoryPicker.target.workCategory = [...categoryPicker.selected]
  categoryPicker.show = false
}

// ===== 假期类型 Picker =====
const leaveTypePicker = reactive({ show: false, index: -1 })

const leaveTypeColumns = computed(() =>
  sys_rbtype.value.filter(d => d.value !== 'work').map(d => ({ text: d.label, value: d.value }))
)

function leaveTypeLabel(type) {
  return (sys_rbtype.value.find(d => d.value === type) || {}).label || ''
}

function openLeaveTypePicker(idx) {
  leaveTypePicker.index = idx
  leaveTypePicker.show = true
}

function onLeaveTypeConfirm({ selectedOptions }) {
  if (leaveTypePicker.index >= 0 && selectedOptions?.[0]) {
    leaveList.value[leaveTypePicker.index].entryType = selectedOptions[0].value
  }
  leaveTypePicker.show = false
}

// 含子任务卡片的已填摘要
function taskFilledSummary(item) {
  const filled = (item.taskRows || []).filter(t => Number(t.workHours) > 0)
  if (!filled.length) return ''
  const hours = filled.reduce((s, t) => s + Number(t.workHours), 0)
  return `已填 ${filled.length} 项 / ${hours}h`
}

// ===== 保存（payload 组装与校验严格对齐桌面 handleSave，见 specs/014 data-model.md §3） =====
async function handleSave() {
  const details = []

  for (const item of formList.value) {
    if (item.hasSubProject && item.taskRows === null) {
      showToast(`项目"${item.projectName}"的任务列表尚未加载，请稍后再试`)
      return
    }
    if (item.hasSubProject && item.taskRows) {
      for (const t of item.taskRows.filter(t => Number(t.workHours) > 0)) {
        if (!t.workCategory?.length) {
          showToast(`项目"${item.projectName}"的任务"${t.taskName}"工时已填写，请选择工作任务类别`)
          return
        }
        details.push({
          projectId: item.projectId,
          projectStage: item.projectStage,
          workHours: Number(t.workHours),
          workContent: t.workContent,
          entryType: 'work',
          subProjectId: t.subProjectId,
          workCategory: t.workCategory?.length ? t.workCategory.join(',') : null
        })
      }
    } else if (!item.hasSubProject) {
      if (Number(item.workHours) > 0) {
        if (!item.workCategory?.length) {
          showToast(`项目"${item.projectName}"工时已填写，请选择工作任务类型`)
          return
        }
        if (item.workContent && item.workContent.trim()) {
          details.push({
            projectId: item.projectId,
            projectStage: item.projectStage,
            workHours: Number(item.workHours),
            workContent: item.workContent,
            entryType: 'work',
            subProjectId: null,
            workCategory: item.workCategory?.length ? item.workCategory.join(',') : null
          })
        }
      }
    }
  }

  // 追加假期行（V4：无类型或小时≤0 的静默过滤，与桌面一致）
  const leaveDetails = leaveList.value
    .filter(l => l.entryType && Number(l.leaveHours) > 0)
    .map(l => ({
      projectId: null,
      workHours: Number(l.leaveHours),
      workContent: '',
      entryType: l.entryType,
      leaveHours: Number(l.leaveHours),
      remark: l.remark || ''
    }))

  const allDetails = [...details, ...leaveDetails]

  if (allDetails.length === 0) {
    showToast('请至少填写一个项目的工时或假期记录')
    return
  }

  saving.value = true
  try {
    await saveDailyReport({
      reportDate: selectedDate.value,
      detailList: allDetails
    })
    showSuccessToast('日报保存成功')
    // 保存后刷新 reportId 与本周已填标记（保存失败时不进入此段，已填内容保留在页面）
    const res2 = await getMyReport(selectedDate.value)
    currentReportId.value = res2.data?.reportId || null
    loadWeekOverview()
  } finally {
    saving.value = false
  }
}

// ===== 初始化（对齐桌面 onMounted：白名单检查最先，命中则短路） =====
watch(selectedDate, (newVal) => {
  loadDayReport(newVal)
})

onMounted(async () => {
  const whitelistRes = await checkSelfInWhitelist().catch(() => ({ data: false }))
  isWhitelisted.value = whitelistRes.data === true
  if (isWhitelisted.value) return
  await loadProjects()
  await loadDayReport(selectedDate.value)
  await loadWeekOverview()
})
</script>

<style scoped>
.mdr {
  min-height: 100vh;
  background: #f7f8fa;
}

/* 本周日期切换条 */
.mdr-week {
  display: flex;
  background: #fff;
  padding: 8px 4px;
}
.mdr-week__day {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 0;
  border-radius: 8px;
  /* 触控目标 ≥44px（SC-005） */
  min-height: 52px;
  cursor: pointer;
}
.mdr-week__day.is-active {
  background: #1989fa;
  color: #fff;
}
.mdr-week__day.is-today:not(.is-active) .mdr-week__num {
  color: #1989fa;
  font-weight: 600;
}
.mdr-week__label {
  font-size: 12px;
  /* 显式着色：深色模式 WebView 会把未声明颜色的文字强制变浅（白底白字不可见） */
  color: #969799;
}
.mdr-week__num {
  font-size: 16px;
  font-weight: 500;
  color: #323233;
}
.mdr-week__day.is-active .mdr-week__label,
.mdr-week__day.is-active .mdr-week__num {
  color: #fff;
}
.mdr-week__dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}
.mdr-week__dot.is-filled {
  background: #07c160;
}
.mdr-week__day.is-active .mdr-week__dot.is-filled {
  background: #fff;
}

/* 汇总条 */
.mdr-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fff;
  margin-top: 8px;
}
.mdr-summary__date {
  font-size: 14px;
  color: #323233;
  font-weight: 500;
}

/* 项目卡片 */
.mdr-card {
  background: #fff;
  border-radius: 8px;
  margin: 8px 12px;
  padding: 12px 0 4px;
  overflow: hidden;
}
.mdr-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0 16px 8px;
}
.mdr-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #323233;
  flex: 1;
  min-width: 0;
}
.mdr-card__tasks-loading {
  padding: 12px 16px;
}

/* 任务小节 */
.mdr-task {
  border-top: 1px dashed #ebedf0;
  padding-top: 8px;
  margin-top: 8px;
}
.mdr-task:first-child {
  border-top: none;
  margin-top: 0;
}
.mdr-task__name {
  font-size: 14px;
  color: #576b95;
  padding: 0 16px 4px;
}

/* 假期条目 */
.mdr-leave {
  border-top: 1px dashed #ebedf0;
}
.mdr-leave__del {
  font-size: 18px;
  color: #ee0a24;
  padding: 4px;
}

/* Stepper 行触控优化（SC-005） */
.mdr-stepper-cell :deep(.van-cell__value) {
  display: flex;
  justify-content: flex-end;
}

/* 底部保存栏 */
.mdr-bottom-spacer {
  height: 76px;
}
.mdr-savebar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
  z-index: 10;
}
.mdr-savebar :deep(.van-button) {
  height: 44px;
}

/* Popup 选择器 */
.mdr-popup__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 44px;
  padding: 0 16px;
  border-bottom: 1px solid #ebedf0;
}
.mdr-popup__title {
  font-size: 15px;
  font-weight: 500;
  color: #323233;
}
.mdr-popup__btn {
  font-size: 14px;
  color: #969799;
  padding: 8px;
}
.mdr-popup__btn.is-confirm {
  color: #1989fa;
}
.mdr-popup__body {
  max-height: 50vh;
  overflow-y: auto;
}
.mdr-loading {
  padding: 40px 0;
}
</style>
