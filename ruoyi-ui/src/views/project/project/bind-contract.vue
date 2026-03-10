<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span style="font-size: 16px; font-weight: 600;">关联合同</span>
          <el-button link type="primary" icon="Back" @click="goBack">返回</el-button>
        </div>
      </template>

      <!-- 项目基本信息 -->
      <div class="section-title" style="margin-bottom: 10px;">项目基本信息</div>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="行业">
          <dict-tag :options="industry" :value="projectInfo?.industry" />
        </el-descriptions-item>
        <el-descriptions-item label="一级区域">
          <dict-tag :options="sys_yjqy" :value="projectInfo?.region" />
        </el-descriptions-item>
        <el-descriptions-item label="二级区域">
          {{ projectInfo?.regionName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="简称">
          {{ projectInfo?.shortName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="立项年份">
          <dict-tag :options="sys_ndgl" :value="projectInfo?.establishedYear" />
        </el-descriptions-item>
        <el-descriptions-item label="项目ID">
          {{ projectInfo?.projectCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="项目名称" :span="3">
          {{ projectInfo?.projectName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="项目分类">
          <dict-tag :options="sys_xmfl" :value="projectInfo?.projectCategory" />
        </el-descriptions-item>
        <el-descriptions-item label="项目部门">
          {{ getDeptName(projectInfo?.projectDept) }}
        </el-descriptions-item>
        <el-descriptions-item label="预估工作量">
          {{ projectInfo?.estimatedWorkload || 0 }} 人天
        </el-descriptions-item>
        <el-descriptions-item label="项目状态">
          <dict-tag :options="sys_xmzt" :value="projectInfo?.projectStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="验收状态">
          <dict-tag :options="sys_yszt" :value="projectInfo?.acceptanceStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="实际人天">
          {{ projectInfo?.actualWorkload != null ? parseFloat(projectInfo.actualWorkload).toFixed(3) : '0.000' }} 人天
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <dict-tag :options="sys_spzt" :value="projectInfo?.approvalStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="项目预算">
          {{ formatAmount(projectInfo?.projectBudget) }} 元
        </el-descriptions-item>
        <el-descriptions-item label="合同状态">
          <dict-tag :options="sys_htzt" :value="projectInfo?.contractStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="收入确认年度">
          <dict-tag :options="sys_ndgl" :value="projectInfo?.revenueConfirmYear" />
        </el-descriptions-item>
        <el-descriptions-item label="收入确认状态">
          <dict-tag :options="sys_qrzt" :value="projectInfo?.revenueConfirmStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="收入确认金额">
          {{ projectInfo?.confirmAmount != null ? formatAmount(projectInfo.confirmAmount) + ' 元' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="项目阶段" :span="2">
          <dict-tag :options="sys_xmjd" :value="projectInfo?.projectStage" />
        </el-descriptions-item>
        <el-descriptions-item label="项目地址" :span="3">
          {{ projectInfo?.projectAddress || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="项目计划" :span="3">
          <div class="text-content">{{ projectInfo?.projectPlan || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="项目描述" :span="3">
          <div class="text-content">{{ projectInfo?.projectDescription || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="3">
          {{ projectInfo?.approvalReason || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 部门 + 合同 联动选择 -->
      <el-row :gutter="16" style="margin-top: 16px;">
        <el-col :span="12">
          <div class="selector-label">选择部门 <span class="required">*</span></div>
          <el-tree-select
            v-model="selectedDeptId"
            :data="deptTreeData"
            :props="{ label: 'label', value: 'deptId', children: 'children' }"
            placeholder="请选择部门"
            filterable
            clearable
            style="width: 100%;"
            check-strictly
            @change="onDeptChange"
          />
        </el-col>
        <el-col :span="12">
          <div class="selector-label">选择合同 <span class="required">*</span></div>
          <el-select
            v-model="selectedContractId"
            placeholder="请先选择部门"
            :disabled="!selectedDeptId"
            :loading="contractLoading"
            filterable
            clearable
            style="width: 100%;"
            @change="onContractChange"
          >
            <el-option
              v-for="c in contractOptions"
              :key="c.contractId"
              :label="c.contractName"
              :value="c.contractId"
            >
              <span>{{ c.contractName }}</span>
              <span style="float: right; color: #909399; font-size: 12px; margin-left: 8px;">{{ c.contractCode || '' }}</span>
            </el-option>
          </el-select>
        </el-col>
      </el-row>

      <!-- 合同详情预览：前4部分 -->
      <template v-if="contractDetail">
        <el-divider style="margin: 20px 0 16px;" />

        <!-- 1. 合同基本信息 -->
        <div class="section-title">合同基本信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="合同名称" :span="2" label-class-name="desc-label">
            {{ contractDetail.contractName }}
          </el-descriptions-item>
          <el-descriptions-item label="合同编号" label-class-name="desc-label">
            {{ contractDetail.contractCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="部门" label-class-name="desc-label">
            {{ contractDetail.deptName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="关联客户" label-class-name="desc-label">
            {{ contractDetail.customerName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="合同类型" label-class-name="desc-label">
            <dict-tag :options="sys_htlx" :value="contractDetail.contractType" />
          </el-descriptions-item>
          <el-descriptions-item label="合同状态" :span="2" label-class-name="desc-label">
            <dict-tag :options="sys_htzt" :value="contractDetail.contractStatus" />
          </el-descriptions-item>
        </el-descriptions>

        <!-- 2. 合同时间与周期 -->
        <div class="section-title" style="margin-top: 16px;">合同时间与周期</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="合同签订日期" label-class-name="desc-label">
            {{ parseTime(contractDetail.contractSignDate, '{y}-{m}-{d}') || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="合同周期" label-class-name="desc-label">
            {{ contractDetail.contractPeriod != null ? contractDetail.contractPeriod + ' 月' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="免维期" label-class-name="desc-label">
            {{ contractDetail.freeMaintenancePeriod != null ? contractDetail.freeMaintenancePeriod + ' 月' : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 3. 合同金额 -->
        <div class="section-title" style="margin-top: 16px;">合同金额</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="合同金额（含税）" label-class-name="desc-label">
            <span style="color: #409EFF; font-weight: bold;">{{ formatAmount(contractDetail.contractAmount) }} 元</span>
          </el-descriptions-item>
          <el-descriptions-item label="税率" label-class-name="desc-label">
            {{ contractDetail.taxRate != null ? contractDetail.taxRate + ' %' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="不含税金额" label-class-name="desc-label">
            <span style="color: #67C23A; font-weight: bold;">{{ formatAmount(contractDetail.amountNoTax) }} 元</span>
          </el-descriptions-item>
          <el-descriptions-item label="税金" label-class-name="desc-label">
            {{ formatAmount(contractDetail.taxAmount) }} 元
          </el-descriptions-item>
        </el-descriptions>

        <!-- 4. 付款里程碑信息 -->
        <div class="section-title" style="margin-top: 16px;">付款里程碑信息</div>
        <el-table :data="paymentListWithSummary" border size="small">
          <el-table-column label="序号" width="55" align="center">
            <template #default="scope">
              <span v-if="scope.row.isSummary" style="font-weight: bold;">合计</span>
              <span v-else>{{ scope.$index }}</span>
            </template>
          </el-table-column>
          <el-table-column label="里程碑名称" prop="paymentMethodName" show-overflow-tooltip min-width="120">
            <template #default="scope">
              <span v-if="!scope.row.isSummary">{{ scope.row.paymentMethodName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="付款金额（元）" align="right" prop="paymentAmount" width="130">
            <template #default="scope">
              <span :style="scope.row.isSummary ? 'font-weight:bold;color:#409EFF;' : ''">
                {{ formatAmount(scope.row.paymentAmount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="付款状态" align="center" prop="paymentStatus" width="100">
            <template #default="scope">
              <dict-tag v-if="!scope.row.isSummary" :options="sys_fkzt" :value="scope.row.paymentStatus" />
            </template>
          </el-table-column>
          <el-table-column label="预计回款季度" align="center" prop="expectedQuarter" width="120">
            <template #default="scope">
              <dict-tag v-if="!scope.row.isSummary && scope.row.expectedQuarter" :options="sys_jdgl" :value="scope.row.expectedQuarter" />
              <span v-else-if="!scope.row.isSummary">-</span>
            </template>
          </el-table-column>
          <el-table-column label="实际回款日期" align="center" prop="actualPaymentDate" width="120">
            <template #default="scope">
              <span v-if="!scope.row.isSummary">{{ scope.row.actualPaymentDate ? parseTime(scope.row.actualPaymentDate, '{y}-{m}-{d}') : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" show-overflow-tooltip min-width="100">
            <template #default="scope">
              <span v-if="!scope.row.isSummary">{{ scope.row.remark || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="paymentList.length === 0" style="text-align:center;color:#909399;padding:16px 0;font-size:13px;">暂无付款里程碑</div>
      </template>

      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" :disabled="!selectedContractId" @click="submitBind">
          <el-icon><Check /></el-icon>&nbsp;确认关联
        </el-button>
        <el-button @click="goBack">取 消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="BindContract">
import { getProject, getDeptTree, bindContractToProject, getContractByProjectId } from '@/api/project/project'
import { listContractsByDept, getContract } from '@/api/project/contract'
import { listPayment } from '@/api/project/payment'
import { handleTree } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const { sys_htlx, sys_htzt, sys_fkzt, sys_jdgl, industry, sys_yjqy, sys_xmfl, sys_xmjd, sys_xmzt, sys_yszt, sys_spzt, sys_ndgl, sys_qrzt } = proxy.useDict(
  'sys_htlx', 'sys_htzt', 'sys_fkzt', 'sys_jdgl',
  'industry', 'sys_yjqy', 'sys_xmfl', 'sys_xmjd', 'sys_xmzt', 'sys_yszt', 'sys_spzt', 'sys_ndgl', 'sys_qrzt'
)

const projectId = computed(() => Number(route.params.projectId))
const projectInfo = ref(null)

const deptFlatList = ref([])
const deptTreeData = ref([])
const selectedDeptId = ref(null)
const contractOptions = ref([])
const contractLoading = ref(false)
const selectedContractId = ref(null)
const contractDetail = ref(null)
const paymentList = ref([])

const paymentListWithSummary = computed(() => {
  if (paymentList.value.length === 0) return []
  const summary = { isSummary: true, paymentAmount: 0, penaltyAmount: 0 }
  paymentList.value.forEach(item => {
    summary.paymentAmount += Number(item.paymentAmount) || 0
  })
  summary.paymentAmount = summary.paymentAmount.toFixed(2)
  return [summary, ...paymentList.value]
})

function formatAmount(amount) {
  if (amount === null || amount === undefined || amount === '') return '-'
  const num = parseFloat(amount)
  if (isNaN(num)) return '-'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 加载部门树（只显示第3级及以下） */
async function loadDeptTree() {
  const res = await getDeptTree()
  const allDepts = res.data || []
  deptFlatList.value = allDepts
  const validDepts = allDepts.filter(d => d.ancestors && d.ancestors.split(',').length >= 3)
  const nodes = validDepts.map(d => ({ deptId: d.deptId, label: d.deptName, parentId: d.parentId }))
  deptTreeData.value = handleTree(nodes, 'deptId')
}

/** 根据部门ID获取部门名称 */
function getDeptName(deptId) {
  if (!deptId) return '-'
  const numId = typeof deptId === 'string' ? parseInt(deptId) : deptId
  const dept = deptFlatList.value.find(d => d.deptId === numId)
  return dept ? dept.deptName : '-'
}

/** 切换部门 */
function onDeptChange(deptId) {
  selectedContractId.value = null
  contractDetail.value = null
  paymentList.value = []
  contractOptions.value = []
  if (!deptId) return
  contractLoading.value = true
  listContractsByDept(deptId, '').then(res => {
    contractOptions.value = res.data || []
  }).finally(() => {
    contractLoading.value = false
  })
}

/** 切换合同 */
function onContractChange(contractId) {
  contractDetail.value = null
  paymentList.value = []
  if (!contractId) return
  getContract(contractId).then(res => {
    contractDetail.value = res.data
  })
  listPayment({ contractId }).then(res => {
    paymentList.value = res.rows || []
  })
}

/** 提交关联 */
function submitBind() {
  if (!selectedContractId.value) return
  bindContractToProject(projectId.value, selectedContractId.value).then(() => {
    proxy.$modal.msgSuccess('关联合同成功')
    goBack()
  })
}

function goBack() {
  router.push('/project/list')
}

/** 初始化：加载项目信息 + 部门树 + 回显已有关联合同 */
async function init() {
  // 项目信息
  getProject(projectId.value).then(res => {
    projectInfo.value = res.data || null
  })

  await loadDeptTree()

  // 回显已有关联合同
  getContractByProjectId(projectId.value).then(res => {
    if (!res.data) return
    const c = res.data
    if (c.deptId) {
      selectedDeptId.value = c.deptId
      contractLoading.value = true
      listContractsByDept(c.deptId, '').then(listRes => {
        contractOptions.value = listRes.data || []
        selectedContractId.value = c.contractId
        getContract(c.contractId).then(detailRes => {
          contractDetail.value = detailRes.data
        })
        listPayment({ contractId: c.contractId }).then(pmtRes => {
          paymentList.value = pmtRes.rows || []
        })
      }).finally(() => {
        contractLoading.value = false
      })
    }
  })
}

init()
</script>

<style scoped>
.selector-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}
.required {
  color: #f56c6c;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding-left: 10px;
  border-left: 3px solid #409eff;
  margin-bottom: 10px;
}
.action-bar {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 12px;
}
:deep(.desc-label) {
  font-weight: 600;
  background: #f5f7fa;
}
.text-content {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
