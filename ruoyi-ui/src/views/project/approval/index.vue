<template>
  <div class="app-container approval-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="queryParams.projectName" placeholder="请输入项目名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核状态" prop="approvalStatus">
        <el-select v-model="queryParams.approvalStatus" placeholder="请选择" clearable>
          <el-option v-for="dict in sys_spzt" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="projectList" :height="tableHeight">
      <el-table-column label="序号" width="55" align="center">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold; color: #606266;">合计</span>
          <span v-else>{{ scope.$index }}</span>        </template>
      </el-table-column>
      <el-table-column label="项目名称" prop="projectName" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ scope.row.projectName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目ID" prop="projectId" width="80" align="center" />
      <el-table-column label="项目分类" prop="projectCategory" align="center" width="110">
        <template #default="scope">
          <dict-tag :options="sys_xmfl" :value="scope.row.projectCategory" />
        </template>
      </el-table-column>
      <el-table-column label="行业" prop="industry" align="center" width="100">
        <template #default="scope">
          <dict-tag :options="industry" :value="scope.row.industry" />
        </template>
      </el-table-column>
      <el-table-column label="区域" prop="region" align="center" width="100">
        <template #default="scope">
          <dict-tag :options="sys_yjqy" :value="scope.row.region" />
        </template>
      </el-table-column>
      <el-table-column label="项目状态" prop="projectStatus" align="center" width="90">
        <template #default="scope">
          <dict-tag :options="sys_xmzt" :value="scope.row.projectStatus" />
        </template>
      </el-table-column>
      <el-table-column label="审核状态" prop="approvalStatus" align="center" width="90">
        <template #default="scope">
          <dict-tag :options="sys_spzt" :value="scope.row.approvalStatus" />
        </template>
      </el-table-column>
      <el-table-column label="项目部门" align="center" min-width="130" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ getDeptName(scope.row.projectDept) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目经理" prop="projectManagerName" align="center" width="90" />
      <el-table-column label="申请人" prop="createBy" align="center" width="90" />
      <el-table-column label="申请时间" prop="createTime" align="center" width="110">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目预算(元)" align="right" min-width="130">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.projectBudget }}</span>
          <span v-else>{{ scope.row.projectBudget != null ? formatAmount(scope.row.projectBudget) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="80" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleApprove(scope.row)" v-hasPermi="['project:approval:approve']">审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 审核对话框 -->
    <el-dialog title="项目审核" v-model="approveOpen" width="480px" append-to-body>
      <div class="approve-info">
        <p><strong>项目名称：</strong>{{ currentProject.projectName }}</p>
        <p><strong>项目ID：</strong>{{ currentProject.projectId }}</p>
      </div>
      <el-form ref="approveRef" :model="approveForm" :rules="approveRules" label-width="90px" style="margin-top: 16px;">
        <el-form-item label="审核结果" prop="approvalStatus">
          <el-radio-group v-model="approveForm.approvalStatus">
            <el-radio label="1">通过</el-radio>
            <el-radio label="2">不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" prop="approvalReason">
          <el-input v-model="approveForm.approvalReason" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitApprove">确 定</el-button>
          <el-button @click="approveOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Approval">
import { listProject, getProjectSummary, getDeptTree } from "@/api/project/project"
import { approveProject } from "@/api/project/approval"
import { parseTime } from "@/utils/ruoyi"

const { proxy } = getCurrentInstance()
const { sys_spzt, sys_xmfl, industry, sys_yjqy, sys_xmzt } = proxy.useDict('sys_spzt', 'sys_xmfl', 'industry', 'sys_yjqy', 'sys_xmzt')

const projectList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const tableHeight = ref(600)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  projectName: null,
  approvalStatus: null,
})

// 部门相关
const deptFlatList = ref([])

/** 加载部门列表（用于解析部门名） */
function loadDeptTree() {
  getDeptTree().then(response => {
    deptFlatList.value = response.data || []
  })
}

/** 根据部门ID获取部门名称 */
function getDeptName(deptId) {
  if (!deptId) return '-'
  const numDeptId = parseInt(deptId)
  const dept = deptFlatList.value.find(d => d.deptId === numDeptId)
  if (!dept) return '-'

  const ancestorIds = dept.ancestors ? dept.ancestors.split(',').filter(id => id && id !== '0') : []
  const pathDepts = []
  // 从第三级开始（跳过根节点0和一级节点）
  for (let i = 1; i < ancestorIds.length; i++) {
    const ancestorDept = deptFlatList.value.find(d => d.deptId === parseInt(ancestorIds[i]))
    if (ancestorDept) pathDepts.push(ancestorDept.deptName)
  }
  pathDepts.push(dept.deptName)
  return pathDepts.join('-')
}

/** 千分位格式，保留2位小数 */
function formatAmount(val) {
  if (val == null) return '-'
  return Number(val).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/** 查询列表 */
function getList() {
  loading.value = true
  Promise.all([listProject(queryParams.value), getProjectSummary(queryParams.value)])
    .then(([listRes, summaryRes]) => {
      const rows = listRes.rows || []
      total.value = listRes.total
      const s = summaryRes.data || {}
      if (rows.length > 0) {
        const summaryRow = {
          isSummaryRow: true,
          projectBudget: formatAmount(Number(s.projectBudget || 0))
        }
        projectList.value = [summaryRow, ...rows]
      } else {
        projectList.value = []
      }
      loading.value = false
    })
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 审核相关
const approveOpen = ref(false)
const currentProject = ref({})
const approveForm = ref({ approvalStatus: '1', approvalReason: '' })
const approveRules = {
  approvalStatus: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  approvalReason: [
    {
      validator: (rule, value, callback) => {
        if (approveForm.value.approvalStatus === '2' && (!value || !value.trim())) {
          callback(new Error('不通过时必须填写审核意见'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
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

/** 计算表格高度 */
function calcTableHeight() {
  nextTick(() => {
    const windowHeight = window.innerHeight
    const searchHeight = showSearch.value ? 100 : 0
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

.approve-info {
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
