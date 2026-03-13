<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="130px" v-show="showSearch">
      <el-form-item label="项目部门" prop="projectDept">
        <project-dept-select v-model="queryParams.projectDept" @change="onDeptChange" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="项目名称" prop="parentProjectName">
        <el-autocomplete
          v-model="parentProjectKeyword"
          :fetch-suggestions="fetchParentProjectSuggestions"
          placeholder="请输入项目名称"
          value-key="projectName"
          style="width: 180px"
          :debounce="300"
          clearable
          @select="onParentProjectSelect"
          @clear="onParentProjectClear"
          @input="onParentProjectInput"
        >
          <template #default="{ item }">
            <span>{{ item.projectName }}</span>
            <span style="color: #909399; font-size: 12px; margin-left: 6px;">{{ item.projectCode }}</span>
          </template>
        </el-autocomplete>
      </el-form-item>
      <el-form-item label="任务名称" prop="taskName">
        <el-autocomplete
          v-model="queryParams.taskName"
          :fetch-suggestions="fetchTaskNameSuggestions"
          placeholder="请输入任务名称"
          style="width: 180px"
          :debounce="300"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="任务编号" prop="taskCode">
        <el-autocomplete
          v-model="queryParams.taskCode"
          :fetch-suggestions="fetchTaskCodeSuggestions"
          placeholder="请输入任务编号"
          style="width: 180px"
          :debounce="300"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="投产年份" prop="productionYear">
        <el-select v-model="queryParams.productionYear" placeholder="请选择" clearable style="width: 180px"
          @change="onQueryYearChange">
          <el-option v-for="d in sys_ndgl" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="批次号" prop="batchId">
        <el-select v-model="queryParams.batchId" placeholder="请先选年份" :disabled="!queryParams.productionYear"
          clearable style="width: 180px">
          <el-option v-for="b in queryBatchOptions" :key="b.batchId" :label="b.batchNo" :value="b.batchId" />
        </el-select>
      </el-form-item>
      <el-form-item label="项目确认年份" prop="parentRevenueConfirmYear">
        <el-select v-model="queryParams.parentRevenueConfirmYear" placeholder="请选择" clearable style="width: 180px">
          <el-option v-for="d in sys_ndgl" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="产品" prop="product">
        <el-select v-model="queryParams.product" placeholder="请选择" clearable style="width: 180px">
          <el-option v-for="d in sys_product" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务负责人" prop="taskManagerId">
        <el-select v-model="queryParams.taskManagerId" placeholder="请选择" clearable filterable style="width: 180px">
          <el-option v-for="u in managerOptions" :key="u.userId" :label="u.nickName" :value="u.userId" />
        </el-select>
      </el-form-item>
      <el-form-item label="软件中心需求编号" prop="softwareDemandNo">
        <el-autocomplete
          v-model="queryParams.softwareDemandNo"
          :fetch-suggestions="fetchSoftwareDemandNoSuggestions"
          placeholder="请输入"
          style="width: 180px"
          :debounce="300"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="排期状态" prop="scheduleStatus">
        <el-select v-model="queryParams.scheduleStatus" placeholder="请选择" clearable style="width: 180px">
          <el-option v-for="d in sys_pqzt" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务当前阶段" prop="taskStage">
        <el-select v-model="queryParams.taskStage" placeholder="请选择" clearable style="width: 180px">
          <el-option v-for="d in sys_xmjd" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns" />
    </el-row>

    <el-table v-loading="loading" :data="displayList" border :row-style="{ height: 'auto' }"
      :cell-style="{ verticalAlign: 'middle' }" :span-method="spanMethod" style="width: 100%"
      :row-class-name="getRowClassName"
      @sort-change="handleSortChange">
      <el-table-column label="序号" width="60" align="center" fixed="left" v-if="columns.index.visible">
        <template #default="scope">
          <strong v-if="scope.row.isSummaryRow">合计</strong>
          <template v-else>{{ scope.$index }}</template>
        </template>
      </el-table-column>

      <!-- 项目信息组 -->
      <el-table-column label="项目信息" align="center" class-name="project-col-group" label-class-name="project-group-header">
        <el-table-column label="收入确认年度" prop="parentRevenueConfirmYear" width="120" align="center"
          fixed="left" v-if="columns.parentRevenueConfirmYear.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow">
              <dict-tag :options="sys_ndgl" :value="scope.row.parentRevenueConfirmYear" v-if="scope.row.parentRevenueConfirmYear" />
              <span v-else>-</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="项目名称" prop="parentProjectName" min-width="180" fixed="left"
          v-if="columns.parentProjectName.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow">
              <el-link v-if="scope.row.projectId" type="primary"
                :href="`/project/list/detail/${scope.row.projectId}`"
                @click.prevent="$router.push(`/project/list/detail/${scope.row.projectId}`)"
                style="white-space: normal; word-break: break-all; line-height: 1.4;">
                {{ scope.row.parentProjectName }}
              </el-link>
              <span v-else>{{ scope.row.parentProjectName || '-' }}</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="项目所属部门" prop="projectDeptName" min-width="150" align="center"
          show-overflow-tooltip v-if="columns.projectDeptName.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow">{{ getDeptPath(scope.row.projectDept) }}</template>
          </template>
        </el-table-column>
        <el-table-column label="项目经理" prop="projectManagerName" width="100" align="center"
          v-if="columns.projectManagerName.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow">{{ scope.row.projectManagerName }}</template>
          </template>
        </el-table-column>
        <el-table-column label="项目预算(元)" prop="parentProjectBudget" width="130" align="right"
          sortable="custom" v-if="columns.parentProjectBudget.visible">
          <template #default="scope">
            <strong v-if="scope.row.isSummaryRow">{{ Number(scope.row.totalProjectBudget || 0).toLocaleString() }}</strong>
            <template v-else>
              {{ scope.row.parentProjectBudget != null ? Number(scope.row.parentProjectBudget).toLocaleString() : '-' }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="项目预估人天" prop="parentEstimatedWorkload" width="110" align="right"
          sortable="custom" v-if="columns.parentEstimatedWorkload.visible">
          <template #default="scope">
            <strong v-if="scope.row.isSummaryRow">{{ scope.row.totalProjectEstimatedWorkload || 0 }}</strong>
            <template v-else>
              {{ scope.row.parentEstimatedWorkload != null ? scope.row.parentEstimatedWorkload : '-' }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="项目实际人天" prop="parentActualWorkload" width="110" align="right"
          sortable="custom" v-if="columns.parentActualWorkload.visible">
          <template #default="scope">
            <strong v-if="scope.row.isSummaryRow">{{ scope.row.totalProjectActualWorkload || 0 }}</strong>
            <template v-else>
              {{ scope.row.parentActualWorkload != null
                ? parseFloat((scope.row.parentActualWorkload / 8 + (scope.row.parentAdjustWorkload || 0)).toFixed(3))
                : '-' }}
            </template>
          </template>
        </el-table-column>
      </el-table-column>

      <!-- 任务信息组 -->
      <el-table-column label="任务信息" align="center" label-class-name="task-group-header">
        <el-table-column label="任务编号" prop="taskCode" width="130" v-if="columns.taskCode.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow">{{ scope.row.taskCode }}</template>
          </template>
        </el-table-column>
        <el-table-column label="任务名称" prop="taskName" min-width="200" v-if="columns.taskName.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow && scope.row.taskId">
              <el-link type="primary" :href="`/task/subproject/detail/${scope.row.taskId}`"
                @click.prevent="handleDetail(scope.row)"
                style="white-space: normal; word-break: break-all; line-height: 1.4;"
                v-if="checkPermi(['project:task:query'])">{{ scope.row.taskName }}</el-link>
              <span v-else style="white-space: normal; word-break: break-all; line-height: 1.4;">{{ scope.row.taskName }}</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="投产批次" prop="batchNo" width="120" align="center" v-if="columns.batchNo.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow">{{ scope.row.batchNo }}</template>
          </template>
        </el-table-column>
        <el-table-column label="任务预算(元)" prop="taskBudget" width="140" align="right"
          sortable="custom" v-if="columns.taskBudget.visible">
          <template #default="scope">
            <strong v-if="scope.row.isSummaryRow">{{ Number(scope.row.totalBudget).toLocaleString() }}</strong>
            <template v-else>
              {{ scope.row.taskBudget != null ? Number(scope.row.taskBudget).toLocaleString() : '-' }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="产品" prop="product" width="100" align="center" v-if="columns.product.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow">
              <dict-tag :options="sys_product" :value="scope.row.product" v-if="scope.row.product" />
              <span v-else>-</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="提交功能测试版本日期" prop="functionalTestDate" width="170" align="center"
          v-if="columns.functionalTestDate.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow">
              {{ scope.row.functionalTestDate ? parseTime(scope.row.functionalTestDate, '{y}-{m}-{d}') : '-' }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="投产版本日期" prop="productionVersionDate" width="120" align="center"
          v-if="columns.productionVersionDate.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow">
              {{ scope.row.productionVersionDate ? parseTime(scope.row.productionVersionDate, '{y}-{m}-{d}') : '-' }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="计划投产日期" prop="productionDate" width="120" align="center"
          v-if="columns.productionDate.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow">
              {{ scope.row.productionDate ? parseTime(scope.row.productionDate, '{y}-{m}-{d}') : '-' }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="排期状态" prop="scheduleStatus" width="100" align="center"
          v-if="columns.scheduleStatus.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow">
              <dict-tag :options="sys_pqzt" :value="scope.row.scheduleStatus" v-if="scope.row.scheduleStatus" />
              <span v-else>-</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="任务预估人天" prop="estimatedWorkload" width="115" align="right"
          sortable="custom" v-if="columns.estimatedWorkload.visible">
          <template #default="scope">
            <strong v-if="scope.row.isSummaryRow">{{ scope.row.totalEstimatedWorkload }}</strong>
            <template v-else>
              {{ scope.row.estimatedWorkload != null ? scope.row.estimatedWorkload : '-' }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="任务实际人天" prop="actualWorkload" width="115" align="right"
          sortable="custom" v-if="columns.actualWorkload.visible">
          <template #default="scope">
            <strong v-if="scope.row.isSummaryRow">{{ parseFloat((scope.row.totalActualWorkload / 8).toFixed(3)) }}</strong>
            <template v-else>
              {{ scope.row.actualWorkload != null ? parseFloat((scope.row.actualWorkload / 8).toFixed(3)) : '-' }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right" v-if="columns.actions.visible">
          <template #default="scope">
            <template v-if="!scope.row.isSummaryRow && scope.row.taskId">
              <el-button link type="primary" icon="View" v-hasPermi="['project:task:query']"
                @click="handleDetail(scope.row)">详情</el-button>
              <el-button link type="primary" icon="Edit" v-hasPermi="['project:task:edit']"
                @click="handleEdit(scope.row)">编辑</el-button>
              <el-button link type="danger" icon="Delete" v-hasPermi="['project:task:remove']"
                @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      @pagination="getList" />
  </div>
</template>

<script setup name="SubprojectIndex">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { listTask, delTask } from '@/api/project/task'
import { getUsersByPost } from '@/api/project/project'
import { checkPermi } from '@/utils/permission'
import { parseTime } from '@/utils/ruoyi'
import request from '@/utils/request'

const router = useRouter()
const { proxy } = getCurrentInstance()
const { sys_xmjd, sys_ndgl, sys_product, sys_pqzt } = proxy.useDict('sys_xmjd', 'sys_ndgl', 'sys_product', 'sys_pqzt')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const displayList = ref([])
const queryBatchOptions = ref([])
const managerOptions = ref([])
const parentProjectKeyword = ref('')
const deptFlatList = ref([])

function getDeptPath(deptId) {
  if (!deptId) return '-'
  const numId = typeof deptId === 'string' ? parseInt(deptId) : deptId
  const dept = deptFlatList.value.find(d => d.deptId === numId)
  if (!dept) return '-'
  // ancestors 形如 "0,100,101,102"，过滤掉 0，从第3个节点（index=2）开始
  const ancestorIds = dept.ancestors ? dept.ancestors.split(',').filter(id => id && id !== '0') : []
  const pathDepts = []
  for (let i = 2; i < ancestorIds.length; i++) {
    const ancestor = deptFlatList.value.find(d => d.deptId === parseInt(ancestorIds[i]))
    if (ancestor) pathDepts.push(ancestor.deptName)
  }
  pathDepts.push(dept.deptName)
  return pathDepts.join('-')
}

const columns = ref({
  index:                    { label: '序号',           visible: true },
  parentRevenueConfirmYear: { label: '收入确认年度',   visible: true },
  parentProjectName:        { label: '项目名称',       visible: true },
  projectDeptName:          { label: '项目所属部门',   visible: true },
  projectManagerName:       { label: '项目经理',       visible: true },
  parentProjectBudget:      { label: '项目预算(元)',   visible: true },
  parentEstimatedWorkload:  { label: '项目预估人天',   visible: true },
  parentActualWorkload:     { label: '项目实际人天',   visible: true },
  taskCode:                 { label: '任务编号',       visible: true },
  taskName:                 { label: '任务名称',       visible: true },
  batchNo:                  { label: '投产批次',       visible: true },
  taskBudget:               { label: '任务预算(元)',   visible: true },
  product:                  { label: '产品',           visible: true },
  functionalTestDate:       { label: '功能测试版本日期', visible: true },
  productionVersionDate:    { label: '投产版本日期',   visible: true },
  productionDate:           { label: '计划投产日期',   visible: true },
  scheduleStatus:           { label: '排期状态',       visible: true },
  estimatedWorkload:        { label: '任务预估人天',   visible: true },
  actualWorkload:           { label: '任务实际人天',   visible: true },
  actions:                  { label: '操作',           visible: true }
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  projectDept: null,
  parentId: null,
  parentProjectName: '',
  taskName: '',
  taskCode: '',
  taskStage: '',
  productionYear: null,
  batchId: null,
  parentRevenueConfirmYear: null,
  product: null,
  taskManagerId: null,
  softwareDemandNo: '',
  scheduleStatus: null,
  orderByColumn: null,
  isAsc: null
})

// 需要合并的项目列
const mergeColumns = [
  'parentRevenueConfirmYear', 'parentProjectName', 'projectDeptName',
  'projectManagerName', 'parentProjectBudget', 'parentEstimatedWorkload', 'parentActualWorkload'
]

function processDisplayList(rows, summaryData) {
  const list = []
  const grouped = new Map()
  const order = []

  for (const row of rows) {
    const pid = row.projectId
    if (!grouped.has(pid)) {
      grouped.set(pid, [])
      order.push(pid)
    }
    grouped.get(pid).push(row)
  }

  for (const pid of order) {
    const tasks = grouped.get(pid)
    const first = tasks[0]
    tasks.forEach((row, index) => {
      list.push({
        // 项目字段（合并行用）
        projectId: first.projectId,
        parentProjectName: first.parentProjectName,
        parentRevenueConfirmYear: first.parentRevenueConfirmYear,
        projectDeptName: first.projectDeptName,
        projectDept: first.projectDept,
        projectManagerName: first.projectManagerName,
        parentProjectBudget: first.parentProjectBudget,
        parentEstimatedWorkload: first.parentEstimatedWorkload,
        parentActualWorkload: first.parentActualWorkload,
        parentAdjustWorkload: first.parentAdjustWorkload,
        projectStatus: first.projectStatus,
        // 任务字段（无任务时为 null）
        taskId: row.taskId,
        taskCode: row.taskCode,
        taskName: row.taskName,
        taskStage: row.taskStage,
        product: row.product,
        functionalTestDate: row.functionalTestDate,
        productionVersionDate: row.productionVersionDate,
        productionDate: row.productionDate,
        scheduleStatus: row.scheduleStatus,
        estimatedWorkload: row.estimatedWorkload,
        actualWorkload: row.actualWorkload,
        taskBudget: row.taskBudget,
        batchNo: row.batchNo,
        taskManagerName: row.taskManagerName,
        // 合并标记
        isFirstRow: index === 0,
        rowSpan: index === 0 ? tasks.length : 0
      })
    })
  }

  // 合计行插在第一条数据前
  displayList.value = [
    { isSummaryRow: true, ...summaryData },
    ...list
  ]
}

function spanMethod({ row, columnIndex }) {
  // 合计行：不合并任何单元格
  if (row.isSummaryRow) {
    return { rowspan: 1, colspan: 1 }
  }
  // 普通行：按 property 合并项目列
  const column = arguments[0].column
  if (mergeColumns.includes(column.property)) {
    if (row.isFirstRow) {
      return { rowspan: row.rowSpan, colspan: 1 }
    } else {
      return { rowspan: 0, colspan: 0 }
    }
  }
}

function getRowClassName({ row }) {
  return row.isSummaryRow ? 'summary-row' : ''
}

function onDeptChange() {
  parentProjectKeyword.value = ''
  queryParams.parentId = null
}

function fetchParentProjectSuggestions(keyword, cb) {
  request({
    url: '/project/project/search',
    method: 'get',
    params: { projectName: keyword }
  }).then(res => cb(res.data || [])).catch(() => cb([]))
}

function onParentProjectSelect(item) {
  queryParams.parentId = item.projectId
  queryParams.parentProjectName = ''
  parentProjectKeyword.value = item.projectName
  handleQuery()
}

function onParentProjectClear() {
  queryParams.parentId = null
  queryParams.parentProjectName = ''
}

function onParentProjectInput(val) {
  queryParams.parentId = null
  queryParams.parentProjectName = val || ''
}

function fetchTaskCodeSuggestions(keyword, cb) {
  request({
    url: '/project/task/searchTaskCode',
    method: 'get',
    params: { taskCode: keyword }
  }).then(res => {
    cb((res.data || []).map(v => ({ value: v })))
  }).catch(() => cb([]))
}

function fetchTaskNameSuggestions(keyword, cb) {
  request({
    url: '/project/task/searchTaskName',
    method: 'get',
    params: { taskName: keyword }
  }).then(res => {
    cb((res.data || []).map(v => ({ value: v })))
  }).catch(() => cb([]))
}

function fetchSoftwareDemandNoSuggestions(keyword, cb) {
  request({
    url: '/project/task/searchSoftwareDemandNo',
    method: 'get',
    params: { softwareDemandNo: keyword }
  }).then(res => {
    cb((res.data || []).map(v => ({ value: v })))
  }).catch(() => cb([]))
}

async function onQueryYearChange(year) {
  queryParams.batchId = null
  queryBatchOptions.value = []
  if (!year) return
  try {
    const res = await request({
      url: '/project/productionBatch/byYear',
      method: 'get',
      params: { productionYear: year }
    })
    queryBatchOptions.value = res.data || []
  } catch (e) {
    console.error('加载批次失败', e)
  }
}

function getList() {
  loading.value = true
  Promise.all([
    listTask(queryParams),
    request({ url: '/project/task/summary', method: 'get', params: queryParams })
  ]).then(([listRes, summaryRes]) => {
    total.value = listRes.total
    processDisplayList(listRes.rows, summaryRes.data || {})
  }).finally(() => { loading.value = false })
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() {
  proxy.$refs['queryRef'].resetFields()
  parentProjectKeyword.value = ''
  queryParams.parentId = null
  queryParams.parentProjectName = ''
  queryBatchOptions.value = []
  queryParams.orderByColumn = null
  queryParams.isAsc = null
  handleQuery()
}

const sortColumnMap = {
  taskBudget:              't.task_budget',
  parentProjectBudget:     'p.project_budget',
  parentEstimatedWorkload: 'p.estimated_workload',
  parentActualWorkload:    'p.actual_workload',
  estimatedWorkload:       't.estimated_workload',
  actualWorkload:          't.actual_workload'
}

function handleSortChange({ prop, order }) {
  queryParams.orderByColumn = order ? (sortColumnMap[prop] || null) : null
  queryParams.isAsc = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : null
  getList()
}

function handleEdit(row) {
  router.push(`/task/subproject/edit/${row.taskId}`)
}
function handleDetail(row) {
  router.push(`/task/subproject/detail/${row.taskId}`)
}
function handleDelete(row) {
  proxy.$modal.confirm(`确认删除任务「${row.taskName}」？`).then(() => {
    return delTask(row.taskId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

onMounted(async () => {
  // 加载部门树（用于部门路径显示）
  request({ url: '/project/project/deptTreeAll', method: 'get' })
    .then(res => { deptFlatList.value = res.data || [] })
    .catch(() => {})
  try {
    const res = await getUsersByPost()
    managerOptions.value = res.data || []
  } catch (e) {
    console.error('加载负责人列表失败', e)
  }
  getList()
})
</script>

<style scoped>
:deep(.project-group-header) {
  background-color: #ecf5ff !important;
  color: #409eff !important;
  font-weight: bold;
}
:deep(.task-group-header) {
  background-color: #f0f9eb !important;
  color: #67c23a !important;
  font-weight: bold;
}
:deep(.summary-row td) {
  background-color: #fdf6ec !important;
  font-weight: bold;
  color: #303133;
}
</style>
