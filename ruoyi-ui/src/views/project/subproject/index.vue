<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="90px" v-show="showSearch">
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
        >
          <template #default="{ item }">
            <span>{{ item.projectName }}</span>
            <span style="color: #909399; font-size: 12px; margin-left: 6px;">{{ item.projectCode }}</span>
          </template>
        </el-autocomplete>
      </el-form-item>
      <el-form-item label="任务名称" prop="projectName">
        <el-autocomplete
          v-model="queryParams.projectName"
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
      <el-form-item label="当前阶段" prop="projectStage">
        <el-select v-model="queryParams.projectStage" placeholder="请选择" clearable style="width: 180px">
          <el-option v-for="d in sys_xmjd" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
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
      <el-form-item label="任务负责人" prop="projectManagerId">
        <el-select v-model="queryParams.projectManagerId" placeholder="请选择" clearable filterable style="width: 180px">
          <el-option v-for="u in managerOptions" :key="u.userId" :label="u.nickName" :value="u.userId" />
        </el-select>
      </el-form-item>
      <el-form-item label="软件中心需求编号" prop="softwareDemandNo" label-width="120px">
        <el-input v-model="queryParams.softwareDemandNo" placeholder="请输入"
          clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns" />
    </el-row>

    <el-table v-loading="loading" :data="subprojectList" border :row-style="{ height: 'auto' }" :cell-style="{ verticalAlign: 'middle' }">
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column label="任务编号" prop="taskCode" width="130" v-if="columns.taskCode.visible" />
      <el-table-column label="任务名称" prop="projectName" width="220" v-if="columns.projectName.visible">
        <template #default="scope">
          <el-link type="primary" @click="handleDetail(scope.row)" style="white-space: normal; word-break: break-all; line-height: 1.4;"
            v-if="checkPermi(['project:subproject:query'])">{{ scope.row.projectName }}</el-link>
          <span v-else style="white-space: normal; word-break: break-all; line-height: 1.4;">{{ scope.row.projectName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="所属主项目" prop="parentProjectName" min-width="160" show-overflow-tooltip v-if="columns.parentProjectName.visible" />
      <el-table-column label="当前阶段" prop="projectStage" width="130" v-if="columns.projectStage.visible">
        <template #default="scope">
          <dict-tag :options="sys_xmjd" :value="scope.row.projectStage" />
        </template>
      </el-table-column>
      <el-table-column label="项目状态" prop="projectStatus" width="100" align="center" v-if="columns.projectStatus.visible">
        <template #default="scope">
          <dict-tag :options="sys_xmzt" :value="scope.row.projectStatus" />
        </template>
      </el-table-column>
      <el-table-column label="任务负责人" prop="projectManagerName" width="100" align="center" v-if="columns.projectManagerName.visible" />
      <el-table-column label="预计人天" prop="estimatedWorkload" width="100" align="right" v-if="columns.estimatedWorkload.visible" />
      <el-table-column label="实际人天" prop="actualWorkload" width="100" align="right" v-if="columns.actualWorkload.visible">
        <template #default="scope">
          {{ scope.row.actualWorkload != null ? parseFloat(scope.row.actualWorkload).toFixed(3) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="投产年份" prop="productionYear" width="100" align="center" v-if="columns.productionYear.visible" />
      <el-table-column label="投产批次" prop="batchNo" width="120" align="center" v-if="columns.batchNo.visible" />
      <el-table-column label="启动日期" prop="startDate" width="120" align="center" v-if="columns.startDate.visible" />
      <el-table-column label="结束日期" prop="endDate" width="120" align="center" v-if="columns.endDate.visible" />
      <el-table-column label="操作" width="150" align="center" fixed="right" v-if="columns.actions.visible">
        <template #default="scope">
          <el-button link type="primary" icon="View" v-hasPermi="['project:subproject:query']"
            @click="handleDetail(scope.row)">详情</el-button>
          <el-button link type="primary" icon="Edit" v-hasPermi="['project:subproject:edit']"
            @click="handleEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" icon="Delete" v-hasPermi="['project:subproject:remove']"
            @click="handleDelete(scope.row)">删除</el-button>
        </template>
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
import { listSubProject, delProject, getUsersByPost } from '@/api/project/project'
import { checkPermi } from '@/utils/permission'
import request from '@/utils/request'

const router = useRouter()
const { proxy } = getCurrentInstance()
const { sys_xmjd, sys_xmzt, sys_ndgl, sys_product } = proxy.useDict('sys_xmjd', 'sys_xmzt', 'sys_ndgl', 'sys_product')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const subprojectList = ref([])
const queryBatchOptions = ref([])
const managerOptions = ref([])
const parentProjectKeyword = ref('')

const columns = ref({
  taskCode:          { label: '任务编号',   visible: true },
  projectName:       { label: '任务名称',   visible: true },
  parentProjectName: { label: '所属主项目', visible: true },
  projectStage:      { label: '当前阶段',   visible: true },
  projectStatus:     { label: '项目状态',   visible: true },
  projectManagerName:{ label: '任务负责人', visible: true },
  estimatedWorkload: { label: '预计人天',   visible: true },
  actualWorkload:    { label: '实际人天',   visible: true },
  productionYear:    { label: '投产年份',   visible: true },
  batchNo:           { label: '投产批次',   visible: true },
  startDate:         { label: '启动日期',   visible: true },
  endDate:           { label: '结束日期',   visible: true },
  actions:           { label: '操作',       visible: true }
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  projectDept: null,
  parentId: null,
  projectName: '',
  taskCode: '',
  projectStage: '',
  productionYear: null,
  batchId: null,
  parentRevenueConfirmYear: null,
  product: null,
  projectManagerId: null,
  softwareDemandNo: ''
})

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
  parentProjectKeyword.value = item.projectName
  handleQuery()
}

function onParentProjectClear() {
  queryParams.parentId = null
}

function fetchTaskCodeSuggestions(keyword, cb) {
  request({
    url: '/project/project/searchTaskCode',
    method: 'get',
    params: { taskCode: keyword }
  }).then(res => {
    cb((res.data || []).map(v => ({ value: v })))
  }).catch(() => cb([]))
}

function fetchTaskNameSuggestions(keyword, cb) {
  request({
    url: '/project/project/searchTaskName',
    method: 'get',
    params: { projectName: keyword }
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
  listSubProject(queryParams).then(res => {
    subprojectList.value = res.rows
    total.value = res.total
  }).finally(() => { loading.value = false })
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() {
  proxy.$refs['queryRef'].resetFields()
  parentProjectKeyword.value = ''
  queryParams.parentId = null
  queryBatchOptions.value = []
  handleQuery()
}

function handleEdit(row) {
  router.push(`/task/subproject/edit/${row.projectId}`)
}
function handleDetail(row) {
  router.push(`/task/subproject/detail/${row.projectId}`)
}
function handleDelete(row) {
  proxy.$modal.confirm(`确认删除任务「${row.projectName}」？`).then(() => {
    return delProject(row.projectId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

onMounted(async () => {
  try {
    const res = await getUsersByPost()
    managerOptions.value = res.data || []
  } catch (e) {
    console.error('加载负责人列表失败', e)
  }
  getList()
})
</script>
