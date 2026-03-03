<template>
  <div class="app-container">
    <!-- 查询区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目名称" prop="projectId">
        <el-autocomplete
          v-model="projectNameQuery"
          :fetch-suggestions="queryProjectList"
          placeholder="请输入项目名称搜索"
          clearable
          :debounce="300"
          :trigger-on-focus="false"
          @select="handleProjectSelect"
          @input="handleProjectInput"
          style="width: 220px"
        />
      </el-form-item>
      <el-form-item label="当前阶段" prop="projectStage">
        <el-select v-model="queryParams.projectStage" placeholder="请选择阶段" clearable style="width: 160px">
          <el-option v-for="dict in sys_xmjd" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Switch"
          :disabled="multiple"
          @click="handleBatchChange"
          v-hasPermi="['project:projectStageChange:add']"
        >批量变更</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="projectStageChangeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" type="index" width="55" align="center" />
      <el-table-column label="项目名称" align="center" prop="projectName" min-width="160" show-overflow-tooltip />
      <el-table-column label="收入确认年度" align="center" prop="revenueConfirmYear" width="120" />
      <el-table-column label="确认金额(元)" align="center" prop="confirmAmount" width="150">
        <template #default="scope">
          <span>{{ formatAmountYuan(scope.row.confirmAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同状态" align="center" prop="contractStatus" width="100">
        <template #default="scope">
          <dict-tag :options="sys_htzt" :value="scope.row.contractStatus" />
        </template>
      </el-table-column>
      <el-table-column label="合同金额(元)" align="center" prop="contractAmount" width="150">
        <template #default="scope">
          <span>{{ formatAmountYuan(scope.row.contractAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="确认状态" align="center" prop="revenueConfirmStatus" width="110">
        <template #default="scope">
          <dict-tag :options="sys_srqrzt" :value="scope.row.revenueConfirmStatus" />
        </template>
      </el-table-column>
      <el-table-column label="项目部门" align="center" prop="projectDept" min-width="160" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ formatDeptPath(scope.row.projectDept) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前阶段" align="center" prop="projectStage" width="110">
        <template #default="scope">
          <dict-tag :options="sys_xmjd" :value="scope.row.projectStage" />
        </template>
      </el-table-column>
      <el-table-column label="原阶段" align="center" prop="oldStage" width="110">
        <template #default="scope">
          <dict-tag :options="sys_xmjd" :value="scope.row.oldStage" />
        </template>
      </el-table-column>
      <el-table-column label="变更原因" align="center" prop="changeReason" min-width="120" show-overflow-tooltip />
      <el-table-column label="变更人" align="center" prop="changeBy" width="100" />
      <el-table-column label="变更时间" align="center" prop="changeTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleChange(scope.row)" v-hasPermi="['project:projectStageChange:add']">变更</el-button>
          <el-button link type="primary" icon="Document" @click="handleHistory(scope.row)">变更记录</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 变更 Dialog -->
    <el-dialog :title="'变更项目阶段 - ' + changeForm.projectName" v-model="changeOpen" width="520px" append-to-body>
      <el-form ref="changeFormRef" :model="changeForm" :rules="changeRules" label-width="90px">
        <el-form-item label="新阶段" prop="newStage">
          <el-select v-model="changeForm.newStage" placeholder="请选择新阶段" style="width: 100%">
            <el-option v-for="dict in sys_xmjd" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因">
          <el-input v-model="changeForm.changeReason" type="textarea" :rows="4" placeholder="请输入变更原因" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="变更项目数">
          <span style="color: #606266;">1 个项目</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitChange">确 定</el-button>
        <el-button @click="changeOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 变更记录 Dialog -->
    <el-dialog title="变更记录" v-model="historyOpen" width="820px" append-to-body>
      <div class="mb8" style="font-size: 13px; color: #606266;">
        项目：<strong>{{ currentProjectName }}</strong>
      </div>
      <el-table :data="historyList" v-loading="historyLoading" border>
        <el-table-column label="序号" type="index" width="55" align="center" />
        <el-table-column label="原阶段" align="center" prop="oldStage" width="130">
          <template #default="scope">
            <dict-tag :options="sys_xmjd" :value="scope.row.oldStage" />
          </template>
        </el-table-column>
        <el-table-column label="新阶段" align="center" prop="newStage" width="130">
          <template #default="scope">
            <dict-tag :options="sys_xmjd" :value="scope.row.newStage" />
          </template>
        </el-table-column>
        <el-table-column label="变更原因" align="center" prop="changeReason" min-width="150" show-overflow-tooltip />
        <el-table-column label="变更人" align="center" prop="createBy" width="100" />
        <el-table-column label="变更时间" align="center" prop="createTime" width="180" />
      </el-table>
    </el-dialog>

    <!-- 批量变更 Dialog -->
    <el-dialog title="批量变更项目阶段" v-model="batchOpen" width="520px" append-to-body>
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="90px">
        <el-form-item label="新阶段" prop="newStage">
          <el-select v-model="batchForm.newStage" placeholder="请选择新阶段" style="width: 100%">
            <el-option v-for="dict in sys_xmjd" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因">
          <el-input v-model="batchForm.changeReason" type="textarea" :rows="4" placeholder="请输入变更原因" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="变更项目数">
          <span style="color: #606266;">{{ selectedRows.length }} 个项目</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitBatchChange">确 定</el-button>
        <el-button @click="batchOpen = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ProjectStageChange">
import { listProjectStageChange, historyByProject, addProjectStageChange, batchChangeStage } from "@/api/project/projectStageChange"
import request from '@/utils/request'

const { proxy } = getCurrentInstance()
const { sys_xmjd, sys_htzt, sys_srqrzt } = proxy.useDict('sys_xmjd', 'sys_htzt', 'sys_srqrzt')

const projectStageChangeList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const selectedRows = ref([])
const multiple = ref(true)

const projectNameQuery = ref('')
const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: null,
    projectStage: null,
  }
})
const { queryParams } = toRefs(data)

// 变更 Dialog
const changeOpen = ref(false)
const changeForm = ref({})
const changeRules = {
  newStage: [{ required: true, message: '请选择新阶段', trigger: 'change' }]
}

// 变更记录 Dialog
const historyOpen = ref(false)
const historyLoading = ref(false)
const historyList = ref([])
const currentProjectName = ref('')

// 批量变更 Dialog
const batchOpen = ref(false)
const batchForm = ref({})
const batchRules = {
  newStage: [{ required: true, message: '请选择新阶段', trigger: 'change' }]
}

function formatAmountYuan(val) {
  if (val == null || val === '') return '-'
  return parseFloat(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 部门树
const deptMap = ref({})
function loadDepts() {
  request({ url: '/system/dept/list', method: 'get' }).then(res => {
    const map = {}
    ;(res.data || []).forEach(d => { map[d.deptId] = d })
    deptMap.value = map
  })
}
function formatDeptPath(deptId) {
  if (!deptId) return '-'
  const id = parseInt(deptId)
  if (!id) return deptId
  const dept = deptMap.value[id]
  if (!dept) return deptId
  const ancestorIds = (dept.ancestors || '').split(',').map(Number).filter(n => n > 0)
  const fullPath = [...ancestorIds, id]
  // 从三级机构（index=2）起显示，以 - 间隔
  const displayIds = fullPath.length > 2 ? fullPath.slice(2) : fullPath
  return displayIds.map(did => deptMap.value[did]?.deptName || String(did)).join(' - ')
}

function getList() {
  loading.value = true
  listProjectStageChange(queryParams.value).then(response => {
    projectStageChangeList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function queryProjectList(keyword, cb) {
  if (!keyword) { cb([]); return }
  request({
    url: '/project/project/list',
    method: 'get',
    params: { projectName: keyword, pageSize: 10, pageNum: 1 }
  }).then(res => {
    cb((res.rows || []).map(r => ({ value: r.projectName, projectId: r.projectId })))
  }).catch(() => cb([]))
}

function handleProjectSelect(item) {
  queryParams.value.projectId = item.projectId
}

function handleProjectInput() {
  queryParams.value.projectId = null
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  projectNameQuery.value = ''
  queryParams.value.projectId = null
  handleQuery()
}

function handleSelectionChange(selection) {
  selectedRows.value = selection
  multiple.value = !selection.length
}

function handleChange(row) {
  changeForm.value = {
    projectId: row.projectId,
    projectName: row.projectName,
    currentStage: row.projectStage,
    newStage: null,
    changeReason: null,
  }
  changeOpen.value = true
}

function submitChange() {
  proxy.$refs['changeFormRef'].validate(valid => {
    if (valid) {
      addProjectStageChange({
        projectId: changeForm.value.projectId,
        oldStage: changeForm.value.currentStage,
        newStage: changeForm.value.newStage,
        changeReason: changeForm.value.changeReason,
      }).then(() => {
        proxy.$modal.msgSuccess('变更成功')
        changeOpen.value = false
        getList()
      })
    }
  })
}

function handleHistory(row) {
  currentProjectName.value = row.projectName
  historyOpen.value = true
  historyLoading.value = true
  historyList.value = []
  historyByProject(row.projectId).then(res => {
    historyList.value = res.data || []
    historyLoading.value = false
  })
}

function handleBatchChange() {
  batchForm.value = { newStage: null, changeReason: null }
  batchOpen.value = true
}

function submitBatchChange() {
  proxy.$refs['batchFormRef'].validate(valid => {
    if (valid) {
      batchChangeStage({
        projectIds: selectedRows.value.map(r => r.projectId),
        newStage: batchForm.value.newStage,
        changeReason: batchForm.value.changeReason,
      }).then(() => {
        proxy.$modal.msgSuccess('批量变更成功')
        batchOpen.value = false
        getList()
      })
    }
  })
}

loadDepts()
getList()
</script>

<style scoped>
:deep(.el-pagination) {
  margin-top: 15px;
  text-align: right;
}
</style>
