<template>
  <div class="app-container approval-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="项目名称" prop="projectName">
        <el-select
          v-model="queryParams.projectName"
          filterable
          remote
          clearable
          :remote-method="remoteQueryProjectNames"
          :loading="loadingProjectNames"
          placeholder="请输入项目名称搜索"
          style="width: 200px"
        >
          <el-option v-for="item in projectNameOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="项目编号" prop="projectCode">
        <el-input v-model="queryParams.projectCode" placeholder="请输入项目编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="项目分类" prop="projectCategory">
        <dict-select v-model="queryParams.projectCategory" dict-type="sys_xmfl" placeholder="请选择项目分类" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="行业" prop="industry">
        <dict-select v-model="queryParams.industry" dict-type="industry" placeholder="请选择行业" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="区域" prop="region">
        <dict-select v-model="queryParams.region" dict-type="sys_yjqy" placeholder="请选择区域" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="项目状态" prop="projectStatus">
        <dict-select v-model="queryParams.projectStatus" dict-type="sys_xmzt" placeholder="请选择项目状态" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="审核状态" prop="approvalStatus">
        <dict-select v-model="queryParams.approvalStatus" dict-type="sys_spzt" placeholder="请选择审核状态" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="projectList"
      :height="tableHeight"
      border
      stripe
      style="width: 100%"
      :row-class-name="tableRowClassName"
    >
      <el-table-column label="序号" width="55" align="center" fixed="left">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">合计</span>
          <span v-else>{{ scope.$index }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目名称" align="left" header-align="center" prop="projectName" min-width="200" fixed="left" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ scope.row.projectName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目编号" prop="projectCode" align="center" min-width="160" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ scope.row.projectCode || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目分类" prop="projectCategory" align="center" min-width="120">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_xmfl" :value="scope.row.projectCategory" />
        </template>
      </el-table-column>
      <el-table-column label="行业" prop="industry" align="center" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="industry" :value="scope.row.industry" />
        </template>
      </el-table-column>
      <el-table-column label="区域" prop="region" align="center" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_yjqy" :value="scope.row.region" />
        </template>
      </el-table-column>
      <el-table-column label="项目状态" prop="projectStatus" align="center" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_xmzt" :value="scope.row.projectStatus" />
        </template>
      </el-table-column>
      <el-table-column label="审核状态" prop="approvalStatus" align="center" min-width="110">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_spzt" :value="scope.row.approvalStatus" />
        </template>
      </el-table-column>
      <el-table-column label="项目部门" align="center" min-width="130" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ getDeptName(scope.row.projectDept) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目经理" prop="projectManagerName" align="center" min-width="90" />
      <el-table-column label="申请人" prop="createBy" align="center" min-width="90" />
      <el-table-column label="申请时间" prop="createTime" align="center" min-width="110">
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目预算(元)" align="right" header-align="center" min-width="130">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.projectBudget }}</span>
          <span v-else>{{ formatAmount(scope.row.projectBudget) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="scope">
          <template v-if="!scope.row.isSummaryRow">
            <!-- 待审核 or 退回待审核：显示审核按钮 -->
            <el-button
              v-if="scope.row.approvalStatus === '0' || scope.row.approvalStatus === '3'"
              link
              type="primary"
              @click="handleApprove(scope.row)"
              v-hasPermi="['project:approval:approve']"
            >审核</el-button>
            <!-- 审核通过：显示退回按钮 -->
            <el-button
              v-if="scope.row.approvalStatus === '1'"
              link
              type="warning"
              @click="handleRollback(scope.row)"
              v-hasPermi="['project:approval:approve']"
            >退回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 审核对话框 -->
    <el-dialog title="项目审核" v-model="approveOpen" width="480px" append-to-body>
      <div class="dialog-project-info">
        <p><strong>项目名称：</strong>{{ currentProject.projectName }}</p>
        <p><strong>项目编号：</strong>{{ currentProject.projectCode || '-' }}</p>
      </div>
      <el-form ref="approveRef" :model="approveForm" :rules="approveRules" label-width="90px" style="margin-top: 16px;">
        <el-form-item label="审核结果" prop="approvalStatus">
          <el-radio-group v-model="approveForm.approvalStatus">
            <el-radio value="1">通过</el-radio>
            <el-radio value="2">不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" prop="approvalReason">
          <el-input
            v-model="approveForm.approvalReason"
            type="textarea"
            :rows="3"
            :placeholder="approveForm.approvalStatus === '2' ? '请填写不通过原因（必填）' : '审核意见（可选）'"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveOpen = false">取消</el-button>
        <el-button type="primary" @click="submitApprove">确定</el-button>
      </template>
    </el-dialog>

    <!-- 退回对话框 -->
    <el-dialog title="退回项目" v-model="rollbackOpen" width="480px" append-to-body>
      <div class="dialog-project-info">
        <p><strong>项目名称：</strong>{{ currentProject.projectName }}</p>
        <p><strong>项目编号：</strong>{{ currentProject.projectCode || '-' }}</p>
      </div>
      <el-form ref="rollbackRef" :model="rollbackForm" :rules="rollbackRules" label-width="90px" style="margin-top: 16px;">
        <el-form-item label="退回原因" prop="rollbackReason">
          <el-input
            v-model="rollbackForm.rollbackReason"
            type="textarea"
            :rows="3"
            placeholder="请填写退回原因"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rollbackOpen = false">取消</el-button>
        <el-button type="warning" @click="submitRollback">确认退回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Approval">
import { getApprovalProjectList, getApprovalProjectSummary, approveProject, rollbackProject } from "@/api/project/approval"
import { getDeptTree, searchProjects } from "@/api/project/project"
import { parseTime } from "@/utils/ruoyi"

const { proxy } = getCurrentInstance()
const { sys_spzt, sys_xmfl, industry, sys_yjqy, sys_xmzt } = proxy.useDict('sys_spzt', 'sys_xmfl', 'industry', 'sys_yjqy', 'sys_xmzt')

const projectList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const tableHeight = ref(600)

// 项目名称远程搜索
const projectNameOptions = ref([])
const loadingProjectNames = ref(false)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  projectName: null,
  projectCode: null,
  projectCategory: null,
  industry: null,
  region: null,
  projectStatus: null,
  approvalStatus: null,
})

const deptFlatList = ref([])

function loadDeptTree() {
  getDeptTree().then(res => {
    deptFlatList.value = res.data || []
  })
}

function getDeptName(deptId) {
  if (!deptId) return '-'
  const numDeptId = parseInt(deptId)
  const dept = deptFlatList.value.find(d => d.deptId === numDeptId)
  if (!dept) return '-'
  const ancestorIds = dept.ancestors ? dept.ancestors.split(',').filter(id => id && id !== '0') : []
  const pathDepts = []
  for (let i = 1; i < ancestorIds.length; i++) {
    const ancestorDept = deptFlatList.value.find(d => d.deptId === parseInt(ancestorIds[i]))
    if (ancestorDept) pathDepts.push(ancestorDept.deptName)
  }
  pathDepts.push(dept.deptName)
  return pathDepts.join('-')
}

function formatAmount(val) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function tableRowClassName({ row }) {
  return row.isSummaryRow ? 'summary-row' : ''
}

function remoteQueryProjectNames(query) {
  if (!query) { projectNameOptions.value = []; return }
  loadingProjectNames.value = true
  searchProjects(query).then(res => {
    projectNameOptions.value = (res.data || []).map(p => ({ value: p.projectName, label: p.projectName }))
  }).finally(() => { loadingProjectNames.value = false })
}

function getList() {
  loading.value = true
  getApprovalProjectList(queryParams.value).then(listRes => {
    const rows = listRes.rows || []
    total.value = listRes.total
    if (rows.length > 0) {
      // 先用占位数据渲染列表
      projectList.value = [{ isSummaryRow: true, projectBudget: '-' }, ...rows]
      // 再异步加载合计
      getApprovalProjectSummary(queryParams.value).then(summaryRes => {
        const s = summaryRes.data || {}
        projectList.value[0] = {
          isSummaryRow: true,
          projectBudget: formatAmount(Number(s.projectBudget || 0))
        }
      }).catch(() => {
        projectList.value[0] = { isSummaryRow: true, projectBudget: '-' }
      })
    } else {
      projectList.value = []
    }
    loading.value = false
  }).catch(() => { loading.value = false })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 审核
const approveOpen = ref(false)
const currentProject = ref({})
const approveForm = ref({ approvalStatus: '1', approvalReason: '' })
const approveRules = {
  approvalStatus: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  approvalReason: [{
    validator: (rule, value, callback) => {
      if (approveForm.value.approvalStatus === '2' && (!value || !value.trim())) {
        callback(new Error('不通过时必须填写原因'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }]
}

function handleApprove(row) {
  currentProject.value = row
  approveForm.value = { approvalStatus: '1', approvalReason: '' }
  approveOpen.value = true
}

function submitApprove() {
  proxy.$refs['approveRef'].validate(valid => {
    if (valid) {
      approveProject({
        projectId: currentProject.value.projectId,
        approvalStatus: approveForm.value.approvalStatus,
        approvalReason: approveForm.value.approvalReason
      }).then(() => {
        proxy.$modal.msgSuccess("审核成功")
        approveOpen.value = false
        getList()
      })
    }
  })
}

// 退回
const rollbackOpen = ref(false)
const rollbackForm = ref({ rollbackReason: '' })
const rollbackRules = {
  rollbackReason: [{ required: true, message: '请填写退回原因', trigger: 'blur' }]
}

function handleRollback(row) {
  currentProject.value = row
  rollbackForm.value = { rollbackReason: '' }
  rollbackOpen.value = true
}

function submitRollback() {
  proxy.$refs['rollbackRef'].validate(valid => {
    if (valid) {
      rollbackProject({
        projectId: currentProject.value.projectId,
        rollbackReason: rollbackForm.value.rollbackReason
      }).then(() => {
        proxy.$modal.msgSuccess("退回成功")
        rollbackOpen.value = false
        getList()
      })
    }
  })
}

function calcTableHeight() {
  nextTick(() => {
    const windowHeight = window.innerHeight
    const searchHeight = showSearch.value ? 160 : 0
    tableHeight.value = windowHeight - searchHeight - 50 - 50 - 120
  })
}

onMounted(() => {
  loadDeptTree()
  calcTableHeight()
  window.addEventListener('resize', calcTableHeight)
})

onUnmounted(() => {
  window.removeEventListener('resize', calcTableHeight)
})

watch(showSearch, () => calcTableHeight())

getList()
</script>

<style scoped lang="scss">
.approval-container {
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.el-form--inline .el-form-item) {
    margin-right: 20px;
    margin-bottom: 15px;
  }

  :deep(.el-table) {
    font-size: 13px;

    .el-table__header th {
      background-color: #f5f7fa;
      color: #606266;
      font-weight: 600;
    }

    .el-table__body tr:hover > td {
      background-color: #f5f7fa !important;
    }
  }

  :deep(.el-pagination) {
    margin-top: 15px;
    text-align: right;
  }
}

::v-deep .summary-row {
  background-color: #f5f7fa;
  font-weight: bold;
}

::v-deep .summary-row:hover > td {
  background-color: #f5f7fa !important;
}

.dialog-project-info {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 12px 16px;
  p {
    margin: 4px 0;
    font-size: 14px;
    color: #303133;
  }
}
</style>
