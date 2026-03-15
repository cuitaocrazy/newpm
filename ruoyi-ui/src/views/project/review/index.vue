<template>
  <div class="app-container review-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="120px">
      <el-form-item label="项目名称" prop="projectName">
        <el-autocomplete
          v-model="queryParams.projectName"
          :fetch-suggestions="remoteQueryProjectNames"
          clearable
          placeholder="输入关键字搜索，或直接选择下拉数据"
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="项目部门" prop="projectDept">
        <project-dept-select v-model="queryParams.projectDept" style="width: 240px" />
      </el-form-item>
      <el-form-item label="项目分类" prop="projectCategory">
        <dict-select v-model="queryParams.projectCategory" dict-type="sys_xmfl" placeholder="请选择项目分类" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="一级区域" prop="region">
        <dict-select v-model="queryParams.region" dict-type="sys_yjqy" placeholder="请选择一级区域" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="二级区域" prop="regionId">
        <secondary-region-select :region-dict-value="queryParams.region" v-model="queryParams.regionId" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="项目经理" prop="projectManagerId">
        <user-select v-model="queryParams.projectManagerId" post-code="pm" placeholder="请选择项目经理" clearable filterable style="width: 240px" />
      </el-form-item>
      <el-form-item label="市场经理" prop="marketManagerId">
        <user-select v-model="queryParams.marketManagerId" post-code="scjl" placeholder="请选择市场经理" clearable filterable style="width: 240px" />
      </el-form-item>
      <el-form-item label="审核状态" prop="approvalStatus">
        <dict-select v-model="queryParams.approvalStatus" dict-type="sys_spzt" placeholder="请选择审核状态" clearable style="width: 240px" />
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
      :data="reviewList"
      :height="tableHeight"
      border
      stripe
      style="width: 100%"
      :row-class-name="tableRowClassName"
      @sort-change="handleSortChange"
    >
      <el-table-column label="序号" width="55" align="center" fixed="left">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">合计</span>
          <span v-else>{{ scope.$index }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目名称" align="left" header-align="center" prop="projectName" width="220" fixed="left" class-name="col-wrap">
        <template #default="scope">
          <el-link v-if="!scope.row.isSummaryRow" type="primary" :href="`/project/list/detail/${scope.row.projectId}`" @click.prevent="handleReview(scope.row)" underline="never">
            {{ scope.row.projectName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="项目编号" prop="projectCode" align="center" min-width="160" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ scope.row.projectCode || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目分类" prop="projectCategory" align="center" min-width="110">
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
      <el-table-column label="审核意见" prop="approvalReason" align="left" header-align="center" min-width="160" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ scope.row.approvalReason || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目部门" align="center" min-width="130" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ scope.row.deptName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目经理" prop="projectManagerName" align="center" min-width="90" />
      <el-table-column label="申请人" prop="createByName" align="center" min-width="90" />
      <el-table-column label="申请时间" prop="createTime" align="center" min-width="110">
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目预算(元)" prop="projectBudget" align="right" header-align="center" min-width="130" sortable="custom">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.projectBudget }}</span>
          <span v-else>{{ formatAmount(scope.row.projectBudget) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="scope">
          <template v-if="!scope.row.isSummaryRow">
            <!-- 待审核(0)：显示审核按钮 -->
            <el-button
              v-if="scope.row.approvalStatus === '0'"
              link
              type="primary"
              icon="Edit"
              @click="handleReview(scope.row)"
              v-hasPermi="['project:review:approve']"
            >审核</el-button>
            <!-- 审核通过(1)：显示退回按钮 -->
            <el-button
              v-if="scope.row.approvalStatus === '1'"
              link
              type="warning"
              @click="handleRollback(scope.row)"
              v-hasPermi="['project:review:approve']"
            >退回</el-button>
          </template>
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

    <!-- 审核抽屉 -->
    <el-drawer
      v-model="reviewOpen"
      :title="'审核项目 - ' + reviewForm.projectName"
      direction="rtl"
      size="60%"
      :close-on-click-modal="true"
      :close-on-press-escape="false"
      class="review-drawer"
    >
      <template #header>
        <div class="drawer-header">
          <span class="drawer-title">审核项目 - {{ reviewForm.projectName }}</span>
          <el-link type="primary" underline="never" @click="toggleAllCollapse">
            {{ isAllExpanded ? '全部收起' : '全部展开' }}
          </el-link>
        </div>
      </template>

      <el-collapse v-model="activeNames" style="margin-top: -10px;">
        <el-collapse-item title="基本信息" name="1">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="行业">
              <dict-tag :options="industry" :value="reviewForm.industry"/>
            </el-descriptions-item>
            <el-descriptions-item label="一级区域">
              <dict-tag :options="sys_yjqy" :value="reviewForm.region"/>
            </el-descriptions-item>
            <el-descriptions-item label="二级区域">{{ reviewForm.regionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="简称">{{ reviewForm.shortName }}</el-descriptions-item>
            <el-descriptions-item label="立项年份">{{ reviewForm.establishedYear }} 年</el-descriptions-item>
            <el-descriptions-item label="项目编号">{{ reviewForm.projectCode }}</el-descriptions-item>
            <el-descriptions-item label="项目名称" :span="2">{{ reviewForm.projectName }}</el-descriptions-item>
            <el-descriptions-item label="项目分类">
              <dict-tag :options="sys_xmfl" :value="reviewForm.projectCategory"/>
            </el-descriptions-item>
            <el-descriptions-item label="项目部门">{{ reviewForm.deptName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="预估工作量">{{ reviewForm.estimatedWorkload || 0 }} 人天</el-descriptions-item>
            <el-descriptions-item label="项目预算">{{ reviewForm.projectBudget || 0 }} 元</el-descriptions-item>
            <el-descriptions-item label="实际人天">{{ reviewForm.actualWorkload != null ? parseFloat(reviewForm.actualWorkload).toFixed(3) : '0.000' }} 人天</el-descriptions-item>
            <el-descriptions-item label="项目阶段">
              <dict-tag :options="sys_xmjd" :value="reviewForm.projectStage"/>
            </el-descriptions-item>
            <el-descriptions-item label="验收状态">
              <dict-tag :options="sys_yszt" :value="reviewForm.acceptanceStatus"/>
            </el-descriptions-item>
            <el-descriptions-item label="审核状态">
              <dict-tag :options="sys_spzt" :value="reviewForm.approvalStatus"/>
            </el-descriptions-item>
            <el-descriptions-item label="项目地址" :span="2">{{ reviewForm.projectAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目计划" :span="2">{{ reviewForm.projectPlan || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="2">{{ reviewForm.projectDescription || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>

        <el-collapse-item title="人员配置" name="2">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目经理">{{ reviewForm.projectManagerName }}</el-descriptions-item>
            <el-descriptions-item label="市场经理">{{ reviewForm.marketManagerName }}</el-descriptions-item>
            <el-descriptions-item label="销售负责人">{{ reviewForm.salesManagerName }}</el-descriptions-item>
            <el-descriptions-item label="销售联系方式">{{ reviewForm.salesContact }}</el-descriptions-item>
            <el-descriptions-item label="参与人员" :span="2">{{ reviewForm.participantsNames }}</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>

        <el-collapse-item title="客户信息" name="3">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="客户名称">{{ reviewForm.customerName }}</el-descriptions-item>
            <el-descriptions-item label="客户联系人">{{ reviewForm.customerContactName }}</el-descriptions-item>
            <el-descriptions-item label="客户联系方式">{{ reviewForm.customerContactPhone }}</el-descriptions-item>
            <el-descriptions-item label="商户联系人">{{ reviewForm.merchantContact }}</el-descriptions-item>
            <el-descriptions-item label="商户联系方式">{{ reviewForm.merchantPhone }}</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>

        <el-collapse-item title="时间规划" name="4">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="启动日期">{{ parseTime(reviewForm.startDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="结束日期">{{ parseTime(reviewForm.endDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="上线日期">{{ parseTime(reviewForm.productionDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="验收日期">{{ parseTime(reviewForm.acceptanceDate, '{y}-{m}-{d}') }}</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>

        <el-collapse-item title="成本预算" name="5">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目费用">{{ reviewForm.projectCost }} 元</el-descriptions-item>
            <el-descriptions-item label="费用预算">{{ reviewForm.expenseBudget }} 元</el-descriptions-item>
            <el-descriptions-item label="成本预算">{{ reviewForm.costBudget }} 元</el-descriptions-item>
            <el-descriptions-item label="人力费用">{{ reviewForm.laborCost }} 元</el-descriptions-item>
            <el-descriptions-item label="采购成本">{{ reviewForm.purchaseCost }} 元</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>

        <el-collapse-item title="备注" name="6">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="备注">{{ reviewForm.remark }}</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>
      </el-collapse>

      <!-- 审核表单 -->
      <el-divider />
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="100px">
        <el-form-item label="审核意见" prop="approvalReason">
          <el-input
            v-model="reviewForm.approvalReason"
            type="textarea"
            :rows="3"
            placeholder="请输入审核意见（拒绝时必填）"
            :class="{ 'is-error': showReasonError }"
            @input="showReasonError = false"
          />
          <transition name="fade">
            <div v-if="showReasonError" class="error-tip">
              <el-icon><WarningFilled /></el-icon>
              <span>拒绝审核时必须填写审核意见</span>
            </div>
          </transition>
        </el-form-item>
      </el-form>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 10px;">
          <el-button type="success" @click="submitApprove('1')">通过</el-button>
          <el-button type="danger" @click="submitApprove('2')">拒绝</el-button>
          <el-button @click="cancelReview">取消</el-button>
        </div>
      </template>
    </el-drawer>

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

<script setup name="Review">
import { listReview, getReviewSummary, getReview, approveProject, rollbackProject } from "@/api/project/review"
import { searchProjects } from "@/api/project/project"
import { WarningFilled } from '@element-plus/icons-vue'
import { parseTime } from "@/utils/ruoyi"

const { proxy } = getCurrentInstance()
const { sys_xmfl, sys_yjqy, sys_xmjd, sys_spzt, sys_yszt, sys_xmzt, industry } = proxy.useDict('sys_xmfl', 'sys_yjqy', 'sys_xmjd', 'sys_spzt', 'sys_yszt', 'sys_xmzt', 'industry')

const reviewList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const tableHeight = ref(600)
const reviewOpen = ref(false)
const activeNames = ref(['1', '2', '3', '4', '5', '6'])
const isAllExpanded = ref(true)
const showReasonError = ref(false)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  projectName: null,
  projectDept: null,
  projectCategory: null,
  region: null,
  regionId: null,
  projectManagerId: null,
  marketManagerId: null,
  approvalStatus: '0',
})

const reviewForm = ref({})
const reviewRules = {
  approvalReason: [{ required: false, message: "审核意见不能为空", trigger: "blur" }]
}

function formatAmount(val) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function tableRowClassName({ row }) {
  return row.isSummaryRow ? 'summary-row' : ''
}

function getList() {
  loading.value = true
  listReview(queryParams.value).then(res => {
    const rows = res.rows || []
    total.value = res.total
    if (rows.length > 0) {
      reviewList.value = [{ isSummaryRow: true, projectBudget: '-' }, ...rows]
      getReviewSummary(queryParams.value).then(summaryRes => {
        const s = summaryRes.data || {}
        reviewList.value[0] = {
          isSummaryRow: true,
          projectBudget: formatAmount(Number(s.projectBudget || 0))
        }
      }).catch(() => {})
    } else {
      reviewList.value = []
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

function handleSortChange({ prop, order }) {
  queryParams.value.orderByColumn = order ? prop : null
  queryParams.value.isAsc = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : null
  getList()
}
function remoteQueryProjectNames(query, callback) {
  searchProjects(query || '').then(res => {
    callback((res.data || []).map(p => ({ value: p.projectName })))
  }).catch(() => callback([]))
}

function handleReview(row) {
  reviewOpen.value = true
  getReview(row.projectId).then(response => {
    reviewForm.value = response.data
    reviewForm.value.approvalReason = null
    activeNames.value = ['1', '2', '3', '4', '5', '6']
    isAllExpanded.value = true
  })
}

function toggleAllCollapse() {
  if (isAllExpanded.value) {
    activeNames.value = ['1']
    isAllExpanded.value = false
  } else {
    activeNames.value = ['1', '2', '3', '4', '5', '6']
    isAllExpanded.value = true
  }
}

function cancelReview() {
  reviewOpen.value = false
  showReasonError.value = false
  reviewForm.value = {}
}

function submitApprove(approvalStatus) {
  if (approvalStatus === '2' && (!reviewForm.value.approvalReason || reviewForm.value.approvalReason.trim() === '')) {
    showReasonError.value = true
    nextTick(() => {
      const formElement = document.querySelector('.review-drawer .el-form')
      if (formElement) formElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
    return
  }
  const statusText = approvalStatus === '1' ? '通过' : '拒绝'
  proxy.$modal.confirm('是否确认' + statusText + '该项目立项申请？').then(() => {
    return approveProject({
      projectId: reviewForm.value.projectId,
      approvalStatus: approvalStatus,
      approvalReason: reviewForm.value.approvalReason
    })
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("审核" + statusText + "成功")
    reviewOpen.value = false
    showReasonError.value = false
  }).catch(() => {})
}

// 退回
const rollbackOpen = ref(false)
const currentProject = ref({})
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
.review-container {
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.col-wrap .cell) {
    white-space: normal;
    word-break: break-word;
    line-height: 1.6;
  }

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

.review-drawer :deep(.el-drawer__body) {
  padding-top: 10px;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.drawer-title {
  font-size: 16px;
  font-weight: 500;
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

.error-tip {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #f56c6c;
  font-size: 12px;
  margin-top: 5px;
}

.error-tip .el-icon {
  font-size: 14px;
}

.is-error :deep(.el-textarea__inner) {
  border-color: #f56c6c;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
