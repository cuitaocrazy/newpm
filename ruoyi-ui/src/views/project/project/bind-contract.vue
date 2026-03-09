<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span style="font-size: 16px; font-weight: 600;">关联合同</span>
          <el-button link type="primary" icon="Back" @click="goBack">返回</el-button>
        </div>
      </template>

      <!-- 项目名称 -->
      <div class="project-name-bar">
        <span class="label">项目：</span>
        <span class="value">{{ projectName || '-' }}</span>
      </div>

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
const { sys_htlx, sys_htzt, sys_fkzt, sys_jdgl } = proxy.useDict('sys_htlx', 'sys_htzt', 'sys_fkzt', 'sys_jdgl')

const projectId = computed(() => Number(route.params.projectId))
const projectName = ref('')

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
  const validDepts = allDepts.filter(d => d.ancestors && d.ancestors.split(',').length >= 3)
  const nodes = validDepts.map(d => ({ deptId: d.deptId, label: d.deptName, parentId: d.parentId }))
  deptTreeData.value = handleTree(nodes, 'deptId')
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

/** 初始化：加载项目名称 + 部门树 + 回显已有关联合同 */
async function init() {
  // 项目名称
  getProject(projectId.value).then(res => {
    projectName.value = res.data?.projectName || ''
  })

  await loadDeptTree()

  // 回显已有关联合同
  getContractByProjectId(projectId.value).then(res => {
    if (!res.data) return
    const c = res.data
    // 找到合同对应的部门，选中部门后加载合同列表
    if (c.deptId) {
      selectedDeptId.value = c.deptId
      contractLoading.value = true
      listContractsByDept(c.deptId, '').then(listRes => {
        contractOptions.value = listRes.data || []
        selectedContractId.value = c.contractId
        // 加载合同详情
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
.project-name-bar {
  display: flex;
  align-items: center;
  font-size: 14px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}
.project-name-bar .label {
  color: #606266;
  flex-shrink: 0;
}
.project-name-bar .value {
  color: #303133;
  font-weight: 500;
}
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
</style>
