<template>
  <div class="app-container">
    <!-- 查询栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="项目所属部门" prop="deptId">
        <project-dept-select
          v-model="queryParams.deptId"
          placeholder="请选择项目所属部门"
          style="width: 220px"
          @change="handleDeptChange"
        />
      </el-form-item>
      <el-form-item label="项目名称" prop="projectId">
        <el-autocomplete
          v-model="projectKeyword"
          :fetch-suggestions="fetchProjectSuggestions"
          placeholder="输入关键字筛选项目"
          clearable
          style="width: 240px"
          value-key="projectName"
          :trigger-on-focus="true"
          @select="handleProjectSelect"
          @clear="handleProjectClear"
        />
      </el-form-item>
      <el-form-item label="确认年度" prop="revenueConfirmYears">
        <el-select
          v-model="queryParams.revenueConfirmYears"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
          placeholder="收入确认年度"
          style="width: 220px"
        >
          <el-option
            v-for="dict in sys_ndgl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="年月" prop="yearMonth">
        <!-- 018 FR-006：年月非必填。清空后查询即为全周期口径（日期列不显示，
             个人人天与项目累计人天同为全周期）。默认仍填当月，避免首屏就打全周期查询。 -->
        <el-date-picker
          v-model="queryParams.yearMonth"
          type="month"
          value-format="YYYY-MM"
          placeholder="不选则查全周期"
          clearable
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" :loading="loading" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" :disabled="loading" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 图例说明 -->
    <div class="legend-bar">
      <span class="legend-item">
        <span class="legend-dot contract-dot"></span><span class="contract-label" style="display:inline">项目名称为绿色</span> = 项目有关联合同（可能带来收入）
      </span>
      <span class="legend-item">
        <span class="legend-dot warn-dot"></span><span class="warn-text" style="display:inline">项目累计人天为红色</span> = 项目累计人天已超预算的 50%
      </span>
      <span class="legend-item">
        <span class="legend-dot former-dot"></span><span class="former-member" style="display:inline">人员为灰色</span> = 有工时但已不在项目成员名单，工时仍计入项目累计人天
      </span>
      <span class="legend-item">
        <span class="legend-dot formula-dot"></span><span class="formula-text">项目累计人天 = 项目日报小时 ÷ 8 + 补正天数</span>（全周期累计，不随查询年月变化）
      </span>
    </div>

    <!-- 主体表格 -->
    <div v-loading="loading" :style="loading && tableData.length === 0 ? 'min-height: 200px' : ''">
      <el-empty v-if="!loading && tableData.length === 0" description="暂无日报数据" />

      <template v-else-if="tableData.length > 0">
        <el-table
          :data="flatRows"
          border
          style="width: 100%"
          :span-method="spanMethod"
          :row-class-name="({ row }: any) => row.stripe === 0 ? 'stripe-even' : 'stripe-odd'"
          :row-style="{ height: 'auto' }"
        >
          <!-- 固定列：项目信息（多行展示）。
               【018】用 min-width 而非 width：不填年月时日期列为 0，其余 4 列都是数字定宽，
               若本列也定宽，表格总宽会塌到 706px，右侧留出大片空白而边框仍画到容器右缘。
               Element Plus 要求至少一列使用 min-width 才会把剩余宽度分配出去。
               列数不变，故 spanMethod 的列索引算术不受影响。 -->
          <el-table-column label="项目" prop="projectName" fixed min-width="280">
            <template #default="{ row }">
              <div class="project-cell">
                <div :class="row.hasContract ? 'contract-label' : 'project-label'">
                  <el-icon v-if="row.hasContract" color="#67c23a"><CircleCheck /></el-icon>
                  <!-- 018 FR-019：必须是带真实 href 的 <a>，浏览器右键菜单才会出现
                       「在新标签页打开」；@click.prevent 保证左键仍走前端路由不整页刷新。
                       写法与 stats.vue:35-40 一致。 -->
                  <a
                    :href="projectHref(row.projectId)"
                    class="project-link"
                    @click="onProjectClick($event, row.projectId)"
                  >{{ row.projectName }}</a>
                </div>
                <div class="project-meta">
                  <dict-tag :options="sys_xmjd" :value="row.projectStage" />
                  <span v-if="row.revenueConfirmYear" class="meta-sep">·</span>
                  <dict-tag v-if="row.revenueConfirmYear" :options="sys_ndgl" :value="row.revenueConfirmYear" />
                  <span v-if="row.revenueConfirmStatus" class="meta-sep">·</span>
                  <dict-tag v-if="row.revenueConfirmStatus" :options="sys_qrzt" :value="row.revenueConfirmStatus" />
                </div>
                <div class="project-amounts">
                  <span v-if="row.projectBudget" class="amount-line">预算 {{ formatAmount(row.projectBudget) }}</span>
                  <span v-if="row.contractAmount" class="amount-line">合同 {{ formatAmount(row.contractAmount) }}</span>
                  <span v-if="row.confirmAmount" class="amount-line">确认 {{ formatAmount(row.confirmAmount) }}</span>
                  <!-- 018 FR-013：机构分组 = 项目所属部门自身名称（不是路径、不是 ancestors 末段，
                       也不是成员本人部门）。位置在「确认」金额下方，格式与上面三行保持「标签 值」一致。 -->
                  <span v-if="row.projectDeptName" class="amount-line">机构 {{ row.projectDeptName }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <!-- 固定列：人员（已离场成员灰显并标注，其工时仍计入项目累计人天） -->
          <el-table-column label="人员" prop="nickName" fixed width="146">
            <template #default="{ row }">
              <!-- 018 FR-011/012：昵称（角色）。角色反推不出时不显示空括号。 -->
              <div :class="row.isFormer ? 'former-member' : ''">
                {{ row.nickName }}<span v-if="row.roleLabel" class="role-label">（{{ row.roleLabel }}）</span>
              </div>
              <div v-if="row.isFormer" class="former-tag">已离场</div>
              <!-- 018 FR-017：参与本项目的时间。主源=本项目日报首末日（与工时同源），
                   无日报时回退成员表在册区间。两行展示以免撑宽固定列。 -->
              <div v-if="row.spanStart" class="participation-span">
                {{ row.spanStart }}<br />~ {{ row.spanEnd }}
              </div>
            </template>
          </el-table-column>

          <!-- 动态日期列 -->
          <el-table-column
            v-for="day in dayColumns"
            :key="day"
            :label="day.slice(8)"
            :prop="day"
            width="46"
            align="center"
          >
            <template #default="{ row }">
              <span v-if="row.dailyHours[day]" class="hours-badge">
                {{ row.dailyHours[day] }}
              </span>
            </template>
          </el-table-column>

          <!-- 个人人天（不合并） -->
          <el-table-column label="个人人天" fixed="right" width="80" align="center">
            <template #default="{ row }">
              <span v-if="row.totalHours">{{ formatDays(Number(row.totalHours) / 8) }}</span>
              <span v-else>—</span>
            </template>
          </el-table-column>

          <!-- 固定右列：项目累计人天（项目汇总） / 预算人天 —— 两列均按项目合并 -->
          <el-table-column label="项目累计人天" fixed="right" width="110" align="center">
            <template #default="{ row }">
              <template v-if="row.memberIndex === 0">
                <span
                  :class="row.estimatedWorkload > 0 && row.projectActualDays > row.estimatedWorkload * 0.5 ? 'warn-text' : ''"
                >{{ formatDays(row.projectActualDays) }}</span>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="预算人天" fixed="right" width="90" align="center">
            <template #default="{ row }">
              <template v-if="row.memberIndex === 0">
                <span v-if="row.estimatedWorkload > 0">{{ formatDays(row.estimatedWorkload) }}</span>
                <span v-else>—</span>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts" name="TeamDailyReport">
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import { getTeamMonthly, getTeamProjectOptions } from '@/api/project/dailyReport'
import dayjs from 'dayjs'

const { proxy } = getCurrentInstance() as any
const router = useRouter()

/**
 * 018 FR-019：解析项目详情页的真实 URL，供项目名的 <a href> 使用。
 * 有真实 href 浏览器右键菜单才会出现「在新标签页打开」，写法同 stats.vue:129-131。
 */
function projectHref(projectId: number | undefined) {
  return projectId ? router.resolve(`/project/list/detail/${projectId}`).href : undefined
}

/**
 * 018 FR-019：项目名点击处理。
 * ⚠️ 不能无条件 `@click.prevent` —— 那会把 Cmd/Ctrl+左键、Shift+左键、中键这些
 * 「用户明确想开新标签/新窗口」的操作一并拦下，反而变成在当前页跳走。
 * 只接管普通左键，其余交还浏览器原生行为。
 */
function onProjectClick(e: MouseEvent, projectId: number | undefined) {
  if (!projectId) return
  if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey || e.button !== 0) return
  e.preventDefault()
  router.push(`/project/list/detail/${projectId}`)
}

/**
 * 018 FR-017：计算某成员参与本项目的时间区间。
 * 主源为本项目日报首末日（与工时同源，能解释「工时为什么落在这段时间」）；
 * 从未填报时回退成员表在册区间（join_date 是系统录入日，实测 41% 早于首次日报，故仅作兜底）。
 */
function computeSpan(member: any): { start: string; end: string } | null {
  if (member.firstReportDate) {
    return { start: member.firstReportDate, end: member.lastReportDate || member.firstReportDate }
  }
  if (member.joinDate) {
    return { start: member.joinDate, end: member.leaveDate || '至今' }
  }
  return null
}

// --- 字典数据 ---
const { sys_xmjd, sys_ndgl, sys_qrzt } = proxy.useDict('sys_xmjd', 'sys_ndgl', 'sys_qrzt')

function formatAmount(val: any): string {
  if (val == null || val === '' || Number(val) === 0) return '—'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// --- 查询参数 ---
const queryRef = ref()
const loading = ref(false)
const projectKeyword = ref('')

const queryParams = ref({
  deptId: undefined as number | undefined,
  projectId: undefined as number | undefined,
  revenueConfirmYears: [] as string[],
  yearMonth: dayjs().format('YYYY-MM')
})

// --- 数据 ---
const tableData = ref<any[]>([])

/**
 * 【018】上一次**实际发起查询**时所用的年月。
 * 日期列必须跟着它走，而不是跟着 queryParams.yearMonth ——
 * 后者是输入框的实时值，用户刚清空年月、还没点查询，表格就会先塌成全周期形态，
 * 与此刻仍在展示的按月数据不符（日历格数据还在，列却没了）。
 */
const queriedYearMonth = ref<string | null>(null)

// --- 日期列（上次查询所属月份的所有日期；未指定年月时为空数组） ---
const dayColumns = computed(() => {
  if (!queriedYearMonth.value) return []
  const base = dayjs(queriedYearMonth.value + '-01')
  const daysInMonth = base.daysInMonth()
  const cols: string[] = []
  for (let d = 1; d <= daysInMonth; d++) {
    cols.push(base.date(d).format('YYYY-MM-DD'))
  }
  return cols
})

// --- 平铺行（项目行展开成多个成员行，供 span-method 合并项目列） ---
const flatRows = computed(() => {
  const rows: any[] = []
  for (const project of tableData.value) {
    const members = project.members || []
    // 使用后端返回的项目累计人天（已含调整人天）
    const projectActualDays = Number(project.actualPersonDays || 0)
    const estimatedWorkload = Number(project.estimatedWorkload || 0)
    const stripe = tableData.value.indexOf(project) % 2

    const projectExtra = {
      projectStage: project.projectStage,
      revenueConfirmYear: project.revenueConfirmYear,
      confirmAmount: project.confirmAmount,
      revenueConfirmStatus: project.revenueConfirmStatus,
      projectBudget: project.projectBudget,
      contractAmount: project.contractAmount,
      // 018 FR-013：项目所属机构分组（≠ 成员本人部门 deptName）
      projectDeptName: project.projectDeptName
    }

    members.forEach((member: any, idx: number) => {
      const span = computeSpan(member)
      rows.push({
        stripe,
        projectId: project.projectId,
        projectName: project.projectName,
        hasContract: project.hasContract,
        estimatedWorkload,
        projectActualDays,
        ...projectExtra,
        memberCount: members.length,
        memberIndex: idx,
        userId: member.userId,
        nickName: member.nickName,
        deptName: member.deptName,
        isFormer: member.isFormer === true,
        // 018 FR-011：项目内角色，为空时前端不渲染括号
        roleLabel: member.roleLabel,
        // 018 FR-017：参与时间区间，预先算好避免模板里重复调用
        spanStart: span?.start,
        spanEnd: span?.end,
        dailyHours: member.dailyHours || {},
        totalHours: member.totalHours
      })
    })
    // 若项目无成员也显示占位行
    if (members.length === 0) {
      rows.push({
        stripe,
        projectId: project.projectId,
        projectName: project.projectName,
        hasContract: project.hasContract,
        estimatedWorkload,
        projectActualDays: 0,
        ...projectExtra,
        memberCount: 0,
        memberIndex: 0,
        nickName: '—',
        dailyHours: {},
        totalHours: null
      })
    }
  }
  return rows
})

// --- 合并项目列 ---
// 列索引：0=项目名, 1=人员, 2..N+1=日期, N+2=个人人天, N+3=项目累计人天(汇总), N+4=预算人天
// 【018】未指定年月时 N=0，列表退化为 [0 项目, 1 人员, 2 个人人天, 3 项目累计人天, 4 预算人天]，
// 下面的公式 actualColIndex=2+N+1、budgetColIndex=N+4 对任意 N≥0 恒成立，无需加分支。
function spanMethod({ row, columnIndex }: any) {
  const actualColIndex = 2 + dayColumns.value.length + 1
  const budgetColIndex = actualColIndex + 1
  if (columnIndex === 0 || columnIndex === actualColIndex || columnIndex === budgetColIndex) {
    if (row.memberIndex === 0) {
      return { rowspan: Math.max(row.memberCount, 1), colspan: 1 }
    } else {
      return { rowspan: 0, colspan: 0 }
    }
  }
}

// --- 格式化人天 ---
function formatDays(val: any) {
  if (val == null) return '—'
  return Number(val).toFixed(2) + 'd'
}

// --- 查询 ---
async function handleQuery() {
  if (!queryParams.value.deptId) {
    ElMessage.warning('请先选择项目所属部门')
    return
  }
  // 018 FR-006：年月已改为非必填——不填即查全周期，此处不再拦截。
  // 项目所属部门仍必填（上方守卫），否则一次查询会拉全公司的项目。
  loading.value = true
  try {
    const params = {
      ...queryParams.value,
      revenueConfirmYears: queryParams.value.revenueConfirmYears.length > 0
        ? queryParams.value.revenueConfirmYears.join(',') : undefined
    }
    const res = await getTeamMonthly(params)
    tableData.value = res.data || []
    // 数据到手后再切换日期列形态，保证列与数据始终同源（见 queriedYearMonth 注释）
    queriedYearMonth.value = queryParams.value.yearMonth || null
  } finally {
    loading.value = false
  }
}

// --- 部门切换时重置项目筛选 ---
function handleDeptChange() {
  projectKeyword.value = ''
  queryParams.value.projectId = undefined
}

// --- 项目 autocomplete ---
let debounceTimer: any = null
function fetchProjectSuggestions(query: string, cb: Function) {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    try {
      const res = await getTeamProjectOptions({
        deptId: queryParams.value.deptId,
        projectName: query
      })
      cb((res.data || []).map((item: any) => ({ ...item, value: item.projectName })))
    } catch {
      cb([])
    }
  }, 300)
}

function handleProjectSelect(item: any) {
  queryParams.value.projectId = item.projectId
  handleQuery()
}

function handleProjectClear() {
  queryParams.value.projectId = undefined
  handleQuery()
}

// --- 重置 ---
function resetQuery() {
  queryRef.value?.resetFields()
  projectKeyword.value = ''
  queryParams.value.deptId = undefined
  queryParams.value.projectId = undefined
  queryParams.value.revenueConfirmYears = []
  queryParams.value.yearMonth = dayjs().format('YYYY-MM')
  tableData.value = []
  // 表格已清空，日期列形态一并归零，避免残留上次查询的列
  queriedYearMonth.value = null
}

onMounted(() => {
  // 默认不自动查询，等用户选择部门
})
</script>

<style scoped>
.legend-bar {
  display: flex;
  gap: 24px;
  align-items: center;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.legend-dot {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 3px;
  flex-shrink: 0;
}
.contract-dot { background: #95d475; }
.warn-dot     { background: #f56c6c; }
.former-dot   { background: #c0c4cc; }
.formula-dot  { background: #909399; }

/* 018 FR-016：累计人天算法说明 */
.formula-text {
  color: #606266;
  font-weight: 500;
}

/* 018 FR-019：项目名链接。颜色继承父级（有合同为绿、无合同为默认），
   仅在 hover 时给下划线，视觉上与改造前保持一致。 */
.project-link {
  color: inherit;
  text-decoration: none;
  cursor: pointer;
}
.project-link:hover {
  text-decoration: underline;
}

/* 018 FR-011：人员列的角色后缀 */
.role-label {
  color: #909399;
  font-size: 12px;
}

/* 018 FR-017：参与本项目的时间区间，两行小字以免撑宽固定列 */
.participation-span {
  margin-top: 2px;
  font-size: 11px;
  line-height: 1.3;
  color: #a8abb2;
  white-space: nowrap;
}

/* 已离场成员：有工时但已不在项目成员名单，工时仍计入项目累计人天 */
.former-member {
  color: #909399;
}
.former-tag {
  display: inline-block;
  margin-top: 2px;
  padding: 0 3px;
  font-size: 11px;
  line-height: 1.5;
  color: #909399;
  border: 1px solid #dcdfe6;
  border-radius: 2px;
}

.contract-label,
.project-label {
  display: block;
  white-space: normal;
  word-break: break-all;
  line-height: 1.4;
}
.contract-label {
  color: #67c23a;
  font-weight: 500;
}
:deep(.stripe-even > td) { background-color: #ffffff !important; }
:deep(.stripe-odd  > td) { background-color: #f5f7fa !important; }
:deep(.stripe-even:hover > td) { background-color: #eef1f6 !important; }
:deep(.stripe-odd:hover  > td) { background-color: #e8ecf2 !important; }

.hours-badge {
  display: inline-block;
  font-size: 12px;
  color: #606266;
}
.warn-text {
  color: #f56c6c;
  font-weight: 500;
}

.project-cell {
  line-height: 1.5;
  padding: 2px 0;
}
.project-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  margin-top: 4px;
  font-size: 12px;
}
.meta-sep {
  color: #c0c4cc;
  margin: 0 2px;
}
.project-amounts {
  display: flex;
  flex-direction: column;
  gap: 1px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.amount-line {
  white-space: nowrap;
}
</style>
