<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="96px">
      <el-form-item label="投产年份" prop="productionYear">
        <dict-select v-model="queryParams.productionYear" dict-type="sys_ndgl" placeholder="全部" clearable style="width:140px" @change="onQueryYearChange" />
      </el-form-item>
      <el-form-item label="投产批次号" prop="batchId">
        <el-select v-model="queryParams.batchId" placeholder="全部" clearable filterable style="width:140px">
          <el-option v-for="b in queryBatchOptions" :key="b.batchId" :label="b.batchNo" :value="b.batchId" />
        </el-select>
      </el-form-item>
      <el-form-item label="二级产品" prop="product">
        <dict-select v-model="queryParams.product" dict-type="sys_product" placeholder="全部" clearable style="width:140px" />
      </el-form-item>
      <el-form-item label="软件中心任务号" prop="taskCode">
        <el-input v-model="queryParams.taskCode" placeholder="任务号" clearable style="width:140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="任务名称" prop="taskName">
        <el-input v-model="queryParams.taskName" placeholder="任务名称" clearable style="width:140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="问题单编号" prop="problemNo">
        <el-input v-model="queryParams.problemNo" placeholder="编号" clearable style="width:140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <template v-if="moreOpen">
        <el-form-item label="问题单级别" prop="problemLevel">
          <dict-select v-model="queryParams.problemLevel" dict-type="sys_problem_level" placeholder="全部" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="当前状态" prop="currentStatus">
          <dict-select v-model="queryParams.currentStatus" dict-type="sys_problem_state" placeholder="全部" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="项目组(部门)" prop="deptId">
          <project-dept-select v-model="queryParams.deptId" placeholder="全部" filterable />
        </el-form-item>
        <el-form-item label="是否缺陷" prop="whetherDefect">
          <el-select v-model="queryParams.whetherDefect" placeholder="全部" clearable style="width:140px"><el-option label="是" value="1" /><el-option label="否" value="0" /></el-select>
        </el-form-item>
        <el-form-item label="是否超时" prop="whetherOvertime">
          <el-select v-model="queryParams.whetherOvertime" placeholder="全部" clearable style="width:140px"><el-option label="是" value="1" /><el-option label="否" value="0" /></el-select>
        </el-form-item>
        <el-form-item label="是否问题重现" prop="whetherProRecurrence">
          <el-select v-model="queryParams.whetherProRecurrence" placeholder="全部" clearable style="width:140px"><el-option label="是" value="1" /><el-option label="否" value="0" /></el-select>
        </el-form-item>
        <el-form-item label="是否须关注" prop="whetherAttRequired">
          <el-select v-model="queryParams.whetherAttRequired" placeholder="全部" clearable style="width:140px"><el-option label="是" value="1" /><el-option label="否" value="0" /></el-select>
        </el-form-item>
        <el-form-item label="解决超一天" prop="solutionTimeOverOneDay">
          <el-select v-model="queryParams.solutionTimeOverOneDay" placeholder="全部" clearable style="width:140px"><el-option label="是" value="1" /><el-option label="否" value="0" /></el-select>
        </el-form-item>
        <el-form-item label="创建人员" prop="creatorName">
          <el-input v-model="queryParams.creatorName" placeholder="创建人" clearable style="width:140px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="提交日期" prop="submitDateStart">
          <el-date-picker v-model="submitDateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="~" start-placeholder="开始" end-placeholder="结束" style="width:230px" />
        </el-form-item>
        <el-form-item label="创建日期" prop="createDateStart">
          <el-date-picker v-model="createDateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="~" start-placeholder="开始" end-placeholder="结束" style="width:230px" />
        </el-form-item>
      </template>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button link type="primary" @click="moreOpen = !moreOpen">{{ moreOpen ? '收起' : '查看更多' }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['project:prolistDefect:edit']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['project:prolistDefect:edit']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column type="index" label="序号" width="55" align="center" fixed="left" />
      <el-table-column label="任务名称" prop="taskName" min-width="150" show-overflow-tooltip fixed="left" />
      <el-table-column label="软件中心任务号" prop="taskCode" min-width="140" show-overflow-tooltip />
      <el-table-column label="项目组" prop="deptName" width="110" align="center" show-overflow-tooltip />
      <el-table-column label="投产年份" prop="productionYear" width="90" align="center" />
      <el-table-column label="批次号" prop="batchNo" width="100" align="center" />
      <el-table-column label="二级产品" prop="product" width="100" align="center" />
      <el-table-column label="提交内部测试B包日期" prop="internalClosureDate" width="160" align="center" />
      <el-table-column label="提交功能测试版本日期" prop="functionalTestDate" width="160" align="center" />
      <el-table-column label="提交生产版本日期" prop="productionVersionDate" width="140" align="center" />
      <el-table-column label="计划投产日期" prop="planProductionDate" width="120" align="center" />
      <el-table-column label="当前状态" prop="currentStatus" width="100" align="center">
        <template #default="{ row }"><dict-tag :options="sys_problem_state" :value="row.currentStatus" /></template>
      </el-table-column>
      <el-table-column label="排期状态" prop="scheduleStatus" width="120" align="center">
        <template #default="{ row }">
          <dict-tag v-if="/^\d+$/.test(row.scheduleStatus)" :options="sys_pqzt" :value="row.scheduleStatus" />
          <span v-else>{{ row.scheduleStatus }}</span>
        </template>
      </el-table-column>
      <el-table-column label="问题单编号" prop="problemNo" min-width="130" show-overflow-tooltip />
      <el-table-column label="问题单级别" prop="problemLevel" width="140" align="center">
        <template #default="{ row }"><dict-tag :options="sys_problem_level" :value="row.problemLevel" /></template>
      </el-table-column>
      <el-table-column label="提交日期" prop="submitDate" width="110" align="center" />
      <el-table-column label="问题单关闭日期" prop="settleDate" width="120" align="center" />
      <el-table-column label="核查日期" prop="verifyDate" width="110" align="center" />
      <el-table-column label="是否缺陷" width="90" align="center">
        <template #default="{ row }"><el-tag v-if="row.whetherDefect==='1'" type="danger">是</el-tag><span v-else>否</span></template>
      </el-table-column>
      <el-table-column label="是否超时" width="90" align="center">
        <template #default="{ row }"><el-tag v-if="row.whetherOvertime==='1'" type="danger">是</el-tag><span v-else>否</span></template>
      </el-table-column>
      <el-table-column label="是否问题重现" width="100" align="center">
        <template #default="{ row }"><el-tag v-if="row.whetherProRecurrence==='1'" type="warning">是</el-tag><span v-else>否</span></template>
      </el-table-column>
      <el-table-column label="是否须关注" width="100" align="center">
        <template #default="{ row }"><el-tag v-if="row.whetherAttRequired==='1'" type="warning">是</el-tag><span v-else>否</span></template>
      </el-table-column>
      <el-table-column label="解决超一天" width="100" align="center">
        <template #default="{ row }"><el-tag v-if="row.solutionTimeOverOneDay==='1'" type="danger">是</el-tag><span v-else>否</span></template>
      </el-table-column>
      <el-table-column label="缺陷说明" prop="defectDesc" min-width="140" show-overflow-tooltip />
      <el-table-column label="是否更新版本" width="100" align="center">
        <template #default="{ row }">{{ row.whetherUpdateVersion === '1' ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="创建人员" prop="createByName" width="100" align="center" />
      <el-table-column label="修改人员" prop="updateByName" width="100" align="center" />
      <el-table-column label="创建日期" prop="createTime" width="160" align="center" />
      <el-table-column label="最后修改日期" prop="updateTime" width="160" align="center" />
      <el-table-column label="操作" width="210" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link icon="View" @click="handleDetail(row)" v-hasPermi="['project:prolistDefect:query']">详情</el-button>
          <el-button type="primary" link icon="Edit" @click="handleEdit(row)" v-hasPermi="['project:prolistDefect:edit']">编辑</el-button>
          <el-button type="danger" link icon="Delete" @click="handleDelete(row)" v-hasPermi="['project:prolistDefect:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="ProlistDefect">
import { ref, reactive, toRefs, watch, getCurrentInstance, onMounted } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { listProlistDefect, delProlistDefect, getBatchByYear } from '@/api/project/prolistDefect'

const { proxy } = getCurrentInstance()
const router = useRouter()
const { sys_problem_state, sys_problem_level, sys_pqzt } = proxy.useDict('sys_problem_state', 'sys_problem_level', 'sys_pqzt')

const SEARCH_STATE_KEY = 'prolist_defect_search_state'

const loading = ref(false)
const showSearch = ref(true)
const moreOpen = ref(false)
const total = ref(0)
const dataList = ref([])
const queryBatchOptions = ref([])
const submitDateRange = ref([])
const createDateRange = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1, pageSize: 10,
    productionYear: undefined, batchId: undefined, product: undefined,
    taskCode: undefined, taskName: undefined, problemNo: undefined,
    problemLevel: undefined, currentStatus: undefined, deptId: undefined,
    whetherDefect: undefined, whetherOvertime: undefined, whetherProRecurrence: undefined,
    whetherAttRequired: undefined, solutionTimeOverOneDay: undefined, creatorName: undefined,
    submitDateStart: undefined, submitDateEnd: undefined, createDateStart: undefined, createDateEnd: undefined
  }
})
const { queryParams } = toRefs(data)

watch(submitDateRange, (v) => {
  queryParams.value.submitDateStart = v && v[0] ? v[0] : undefined
  queryParams.value.submitDateEnd = v && v[1] ? v[1] : undefined
})
watch(createDateRange, (v) => {
  queryParams.value.createDateStart = v && v[0] ? v[0] : undefined
  queryParams.value.createDateEnd = v && v[1] ? v[1] : undefined
})

async function onQueryYearChange(year) {
  queryParams.value.batchId = undefined
  queryBatchOptions.value = []
  if (!year) return
  const res = await getBatchByYear(year)
  queryBatchOptions.value = res.data || []
}

function getList() {
  loading.value = true
  listProlistDefect(queryParams.value).then(res => {
    dataList.value = res.rows
    total.value = res.total
  }).finally(() => { loading.value = false })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  sessionStorage.removeItem(SEARCH_STATE_KEY)
  submitDateRange.value = []
  createDateRange.value = []
  proxy.resetForm('queryRef')
  queryBatchOptions.value = []
  handleQuery()
}

function handleAdd() { router.push('/project/prolistDefect/add') }
function handleEdit(row) { router.push({ path: '/project/prolistDefect/edit', query: { problemId: row.problemId } }) }
function handleDetail(row) { router.push({ path: '/project/prolistDefect/detail', query: { problemId: row.problemId } }) }

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除问题单编号「${row.problemNo}」的记录？`).then(() => {
    return delProlistDefect(row.problemId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('/project/prolistDefect/export', { ...queryParams.value }, `问题单及缺陷_${new Date().getTime()}.xlsx`)
}

function saveSearchState() {
  sessionStorage.setItem(SEARCH_STATE_KEY, JSON.stringify({
    queryParams: { ...queryParams.value },
    submitDateRange: submitDateRange.value,
    createDateRange: createDateRange.value,
    queryBatchOptions: queryBatchOptions.value,
    moreOpen: moreOpen.value
  }))
}

function restoreSearchState() {
  try {
    const raw = sessionStorage.getItem(SEARCH_STATE_KEY)
    if (!raw) return false
    const s = JSON.parse(raw)
    Object.assign(queryParams.value, s.queryParams)
    submitDateRange.value = s.submitDateRange || []
    createDateRange.value = s.createDateRange || []
    queryBatchOptions.value = s.queryBatchOptions || []
    moreOpen.value = s.moreOpen || false
    sessionStorage.removeItem(SEARCH_STATE_KEY)
    return true
  } catch { return false }
}

onBeforeRouteLeave(() => saveSearchState())

onMounted(() => {
  restoreSearchState()
  getList()
})
</script>
