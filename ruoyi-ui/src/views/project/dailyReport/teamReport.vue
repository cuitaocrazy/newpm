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

    <!-- 图例说明。与表格同宽：全周期口径下表格按内容宽度收缩到 750px，
         图例若仍是全宽，两者右边缘差一倍，视觉上像「表格缩水了」而不是「这块内容就这么宽」。 -->
    <div class="legend-bar" :style="tableStyle">
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
          ref="tableRef"
          :data="flatRows"
          border
          :style="tableStyle"
          :span-method="spanMethod"
          :row-class-name="({ row }: any) => row.stripe === 0 ? 'stripe-even' : 'stripe-odd'"
          :row-style="{ height: 'auto' }"
        >
          <!-- 项目列：项目名 + 阶段/年度/确认标签 + 预算/合同/确认/机构，合并成一格。
               两种口径共用同一套列结构 —— 全周期口径与按月口径的唯一差别就是有没有日期列。
               min-width 260 让本列成为全表唯一的弹性列，吸收定宽列之外的剩余宽度，
               保证列宽之和恰好等于表宽（不横向滚动）。 -->
          <el-table-column
            key="m-project"
            label="项目"
            prop="projectName"
            fixed
            min-width="260"
            :class-name="PROJECT_LEVEL_CLASS"
          >
            <template #default="{ row }">
              <div class="project-cell">
                <div :class="row.hasContract ? 'contract-label' : 'project-label'">
                  <el-icon v-if="row.hasContract" color="#67c23a"><CircleCheck /></el-icon>
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
                  <span v-if="row.projectDeptName" class="amount-line">机构 {{ row.projectDeptName }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <!-- 人员列（已离场成员灰显并标注，其工时仍计入项目累计人天）。
               ⚠️ 展开形态下**不能**设 fixed：Element Plus 的 updateColumns() 按
               [...fixedLeft, ...notFixed, ...fixedRight] 重排，fixed 列被无条件提到最前 ——
               本列一旦 fixed 就会落到索引 1，把「项目名称」与其余 5 个项目级列劈开，
               变成「一个跨 N 行的大格 → N 个逐行变化的人员小格 → 又 5 个跨 N 行的大格」，
               项目属性被成员名单从中间截断。而展开形态下表宽恒等于列宽之和、永不横向滚动，
               fixed 本就毫无作用。按月与窄屏降级形态窄屏会滚动，故仍保留 fixed。
               【019】按月口径 160，是给 31 个日期列腾宽度后能给到的上限；
               全周期口径无此压力，给 180。
               注意 180 也并非「一定不折行」：实测「严晓（已离职）（项目经理）」
               这类「姓名+已离职+长角色」仍会折成两行，属可接受降级。 -->
          <el-table-column
            key="member"
            label="人员"
            prop="nickName"
            fixed
            width="160"
            class-name="member-cell"
          ><!-- member-cell 是 e2e 钩子（tests/e2e-team-report-layout.spec.js 用它断言
               「人员是行级列、每行都有独立单元格」），不要当死代码清掉 -->
            <template #default="{ row }">
              <!-- 018 FR-011/012：昵称（角色）。角色反推不出时不显示空括号。 -->
              <div :class="row.isFormer ? 'former-member' : ''">
                {{ row.nickName }}<span v-if="row.roleLabel" class="role-label">（{{ row.roleLabel }}）</span>
              </div>
              <div v-if="row.isFormer" class="former-tag">已离场</div>
              <!-- 018 FR-017：参与本项目的时间。主源=本项目日报首末日（与工时同源），
                   无日报时回退成员表在册区间。
                   【019】列宽放宽到 160/180 后日期区间一行放得下，去掉硬换行；
                   窄屏或超长角色时自然折行，无损降级。 -->
              <div v-if="row.spanStart" class="participation-span">
                {{ row.spanStart }} ~ {{ row.spanEnd }}
              </div>
            </template>
          </el-table-column>

          <!-- 动态日期列。
               【019】46 → 30：31 列 ×46 = 1426px，加上其余列合计 2176px 远超可用 1680px，
               整月有一半日期要横向滚动才看得到。压到 30px 后 31 列合计 930px，
               定宽合计 1356（人员160 + 日期930 + 个人人天88 + 累计88 + 预算人天90），
               1920 视口（容器 1680）下项目列弹性吃 324px，整月一屏可见。
               ⚠️ 仅在 ≥1920 视口成立：1440 视口容器 1200 < 最小合计 1616，仍需横向滚动
               （31 列是物理下限，无法在 1200px 内放下），但已远好于改动前的 2176px。
               ⚠️ 必须同时收紧单元格内边距（day-cell）：Element Plus 默认 .cell 左右各 12px，
               30 − 24 = 6px 连「7.5」都放不下，只压列宽会变成一片省略号。 -->
          <el-table-column
            v-for="day in dayColumns"
            :key="day"
            :label="day.slice(8)"
            :prop="day"
            width="30"
            align="center"
            class-name="day-cell"
            label-class-name="day-cell"
          >
            <template #default="{ row }">
              <span v-if="row.dailyHours[day]" class="hours-badge">
                {{ row.dailyHours[day] }}
              </span>
            </template>
          </el-table-column>

          <!-- 个人人天（不合并）。【019】固定 88px，与「项目累计人天」按月口径同宽。
               不是 80 —— 80px 的内容区 79px 只装得下 7 字符（523.63d），
               4 位数人天「1300.00d」需 83px 会被截断（e2e 的确定性边界探针抓到过）。
               实测业务上界：预算人天 6740.00、项目累计人天 1121.95，均为 4 位数。
               两种口径不分流，早先的 `isFullPeriod ? 80 : 80` 是死三元。 -->
          <el-table-column
            key="personDays"
            label="个人人天"
            fixed="right"
            width="88"
            align="center"
          >
            <template #default="{ row }">
              <span v-if="row.totalHours" class="days-cell">{{ formatDays(Number(row.totalHours) / 8) }}</span>
              <span v-else>—</span>
            </template>
          </el-table-column>

          <!-- 固定右列：项目累计人天（项目汇总） / 预算人天 —— 两列均按项目合并 -->
          <el-table-column
            key="projectDays"
            label="项目累计人天"
            fixed="right"
            width="88"
            align="center"
            :class-name="PROJECT_LEVEL_CLASS"
          >
            <template #default="{ row }">
              <template v-if="row.memberIndex === 0">
                <span
                  :class="row.estimatedWorkload > 0 && row.projectActualDays > row.estimatedWorkload * 0.5 ? 'warn-text' : ''"
                 class="days-cell">{{ formatDays(row.projectActualDays) }}</span>
              </template>
            </template>
          </el-table-column>
          <el-table-column
            key="budgetDays"
            label="预算人天"
            fixed="right"
            width="90"
            align="center"
            :class-name="PROJECT_LEVEL_CLASS"
          >
            <template #default="{ row }">
              <template v-if="row.memberIndex === 0">
                <span v-if="row.estimatedWorkload > 0" class="days-cell">{{ formatDays(row.estimatedWorkload) }}</span>
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
import { ref, computed, nextTick, onMounted, onUnmounted, getCurrentInstance } from 'vue'
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

/**
 * 【019】全周期口径 = 未指定年月，动态日期列为 0。
 */
const isFullPeriod = computed(() => dayColumns.value.length === 0)

const tableRef = ref()

/**
 * 【019】表格宽度。
 *
 * 两种口径共用同一套列结构，「项目」是全表唯一的弹性列，吸收「表宽 − 定宽列之和」的富余：
 *   按月　：定宽 1356（人员160 + 31×30 日期 + 88+88+90），1920 视口下项目列得到 324px；
 *   全周期：日期列归零，定宽只剩 426，若同样铺满，项目列会被撑到 1254px —— 内容只需约 324px。
 *
 * 所以全周期封顶到 750px（= 426 定宽 + 324 项目列），项目列宽度与按月口径保持一致，
 * 代价是右侧留白。这是「项目列宽必须与按月一致」这条要求的必然结果：
 * 少了 31 个日期列的宽度，总得有地方去 —— 要么灌进项目列（就是原来那个缺陷），要么留白。
 */
const FULL_PERIOD_TABLE_WIDTH = 750
const tableStyle = computed(() =>
  isFullPeriod.value
    ? { width: '100%', maxWidth: `${FULL_PERIOD_TABLE_WIDTH}px` }
    : { width: '100%' }
)

/**
 * 【019】项目级列的标记 class。带此 class 的列按项目 rowspan 合并。
 *
 * ⚠️ 不要改回「按 columnIndex 算」：Element Plus 会把 fixed 列重排到最前、
 * fixed="right" 列重排到最后，DOM 列序与模板书写顺序并不一致 ——
 * 全周期口径下「人员」（fixed）实际落在索引 1，按书写顺序推算会把它误合并成一格，
 * 只显示第一个成员，而「确认」列反倒每行重复。列宽断言看不见这种错。
 */
const PROJECT_LEVEL_CLASS = 'col-project-level'

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

// --- 合并项目级列（同一项目的多个成员行合并成一格）---
// 【019】按列自带的标记 class 判断，而不是按 columnIndex 推算（原因见 PROJECT_LEVEL_CLASS）。
function spanMethod({ row, column }: any) {
  if (!String(column?.className || '').includes(PROJECT_LEVEL_CLASS)) return
  return row.memberIndex === 0
    ? { rowspan: Math.max(row.memberCount, 1), colspan: 1 }
    : { rowspan: 0, colspan: 0 }
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
    // 【019】本次请求所用的年月必须在发请求前就快照下来。
    // 原写法在 await 之后读 queryParams.value.yearMonth —— 请求在途时用户清空年月，
    // 响应落地就会把「按月数据」渲染成「全周期形态」：日期列消失、个人人天按全周期解读，
    // 而屏幕上没有任何线索表明这批数字只属于某个月（实测延迟 4s 可稳定复现：
    // 2026-03 的 25.50d 被展示成全周期口径，真实全周期值是 123.38d）。
    const requestedYearMonth = queryParams.value.yearMonth || null
    const res = await getTeamMonthly(params)
    tableData.value = res.data || []
    // 数据到手后再切换日期列形态，保证列与数据始终同源（见 queriedYearMonth 注释）
    queriedYearMonth.value = requestedYearMonth
    // 【019】按月 ⇄ 全周期切换会同时改变列数与表格封顶宽度，
    // 需在 DOM 更新后强制重算列宽，否则残留上一形态的分配结果。
    await nextTick()
    tableRef.value?.doLayout()
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

onUnmounted(() => {
  // 组件销毁时清掉 autocomplete 的防抖定时器，避免销毁后仍发出请求
  clearTimeout(debounceTimer)
})
</script>

<style scoped>
.legend-bar {
  display: flex;
  /* 【019】允许换行：四条图例挤在一行时会被压到贴边，窄屏更是直接溢出 */
  flex-wrap: wrap;
  column-gap: 24px;
  row-gap: 6px;
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

/* 018 FR-017：参与本项目的时间区间，小字展示。
   【019】去掉 nowrap：列宽 160/180 下一行放得下，更窄时允许自然折行而不是溢出 */
.participation-span {
  margin-top: 2px;
  font-size: 11px;
  line-height: 1.3;
  color: #a8abb2;
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
/* 【019】日期列专用：收紧左右内边距。
   Element Plus 默认 .cell 左右各 12px，30px 的列减去 24px 只剩 6px，
   「11」「7.5」都会被省略号吃掉 —— 压列宽必须与压内边距成对出现。 */
:deep(.day-cell .cell) {
  padding-left: 2px;
  padding-right: 2px;
}

/* 【019】表头允许折行。按月口径下「项目累计人天」压到 88px（6 字需 78+24=102），
   会折成「项目累计」/「人天」两行 —— 这是当前唯一折行的表头，属有意取舍：
   那 18px 让给了 31 个日期列。Element Plus 默认表头不换行，不覆盖会截断成省略号。 */
:deep(.el-table__header th .cell) {
  white-space: normal;
  line-height: 1.35;
}

/* 【019】人天数值不许折行。
   Element Plus 默认 .cell 是 word-break: break-all，实拍下「308.00d」被拆成「308.00」+「d」两行。 */
.days-cell {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

/* 【019】单元格顶部对齐。
   「项目」「项目累计人天」「预算人天」三列按项目 rowspan 合并，成员多时单元格高达 400px+，
   浏览器默认的 vertical-align: middle 会把项目信息推到单元格正中，与首行成员错位。
   顶对齐后项目级信息与该项目第一位成员处在同一水平线上。 */
:deep(.el-table__body td.el-table__cell) {
  vertical-align: top;
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
.meta-sep {
  color: #c0c4cc;
  margin: 0 2px;
}
.project-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  margin-top: 4px;
  font-size: 12px;
}
/* 金额与机构竖排，每项一行。
   两种口径共用同一套列结构，项目列宽度也一致（约 324px），横排会折成不整齐的两行，
   竖排反而更好扫读。 */
.project-amounts {
  display: flex;
  flex-direction: column;
  gap: 1px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.amount-line {
  /* nowrap 不可省：Element Plus 的 .cell 默认 white-space: normal + overflow-wrap: break-word，
     超宽时会把「11,700,000.00」折成「11,700,000.」+「00」两行，读起来像两个数 */
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
</style>
