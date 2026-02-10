<template>
  <div class="app-container">
    <!-- 第一部分：合同基本信息 -->
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span style="font-size: 16px; font-weight: bold;">合同基本信息</span>
          <el-button style="float: right; padding: 3px 0" type="text" icon="Back" @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="合同名称" :span="2" label-class-name="label-bold">
          {{ detailData.contractName }}
        </el-descriptions-item>
        <el-descriptions-item label="合同编号" label-class-name="label-bold">
          {{ detailData.contractCode }}
        </el-descriptions-item>
        <el-descriptions-item label="部门" label-class-name="label-bold">
          {{ getDeptName(detailData.deptId) }}
        </el-descriptions-item>
        <el-descriptions-item label="关联客户" label-class-name="label-bold">
          {{ getCustomerName(detailData.customerId) }}
        </el-descriptions-item>
        <el-descriptions-item label="合同类型" label-class-name="label-bold">
          <dict-tag :options="sys_htlx" :value="detailData.contractType"/>
        </el-descriptions-item>
        <el-descriptions-item label="合同状态" :span="2" label-class-name="label-bold">
          <dict-tag :options="sys_htzt" :value="detailData.contractStatus"/>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 第二部分：合同时间与周期 -->
    <el-card class="box-card" style="margin-top: 20px;">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">合同时间与周期</span>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="合同签订日期" label-class-name="label-bold">
          {{ parseTime(detailData.contractSignDate, '{y}-{m}-{d}') }}
        </el-descriptions-item>
        <el-descriptions-item label="合同周期" label-class-name="label-bold">
          {{ detailData.contractPeriod }} 月
        </el-descriptions-item>
        <el-descriptions-item label="免维期" label-class-name="label-bold">
          {{ detailData.freeMaintenancePeriod }} 月
        </el-descriptions-item>
        <el-descriptions-item></el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 第三部分：合同金额 -->
    <el-card class="box-card" style="margin-top: 20px;">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">合同金额</span>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="合同金额（含税）" label-class-name="label-bold">
          <span style="color: #409EFF; font-weight: bold; font-size: 16px;">{{ formatAmount(detailData.contractAmount) }} 元</span>
        </el-descriptions-item>
        <el-descriptions-item label="税率" label-class-name="label-bold">
          {{ detailData.taxRate }} %
        </el-descriptions-item>
        <el-descriptions-item label="不含税金额" label-class-name="label-bold">
          <span style="color: #67C23A; font-weight: bold; font-size: 16px;">{{ formatAmount(detailData.amountNoTax) }} 元</span>
        </el-descriptions-item>
        <el-descriptions-item label="税金" label-class-name="label-bold">
          {{ formatAmount(detailData.taxAmount) }} 元
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 第四部分：付款里程碑信息 -->
    <el-card class="box-card" style="margin-top: 20px;">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">付款里程碑信息</span>
      </template>
      <el-table :data="paymentListWithSummary" border>
        <el-table-column label="序号" width="60" align="center">
          <template #default="scope">
            <span v-if="scope.row.isSummary" style="font-weight: bold;">合计</span>
            <span v-else>{{ scope.$index }}</span>
          </template>
        </el-table-column>
        <el-table-column label="里程碑名称" align="center" prop="paymentMethodName" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="!scope.row.isSummary">{{ scope.row.paymentMethodName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="付款金额（元）" align="center" prop="paymentAmount" width="130">
          <template #default="scope">
            <span :style="scope.row.isSummary ? 'font-weight: bold; color: #409EFF;' : ''">
              {{ formatAmount(scope.row.paymentAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="付款状态" align="center" prop="paymentStatus" width="100">
          <template #default="scope">
            <dict-tag v-if="!scope.row.isSummary" :options="sys_fkzt" :value="scope.row.paymentStatus"/>
          </template>
        </el-table-column>
        <el-table-column label="预计回款季度" align="center" prop="expectedQuarter" width="120">
          <template #default="scope">
            <span v-if="!scope.row.isSummary">{{ scope.row.expectedQuarter }}</span>
          </template>
        </el-table-column>
        <el-table-column label="实际回款季度" align="center" prop="actualQuarter" width="120">
          <template #default="scope">
            <span v-if="!scope.row.isSummary">{{ scope.row.actualQuarter || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="实际回款日期" align="center" prop="actualPaymentDate" width="120">
          <template #default="scope">
            <span v-if="!scope.row.isSummary">{{ scope.row.actualPaymentDate ? parseTime(scope.row.actualPaymentDate, '{y}-{m}-{d}') : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="确认年份" align="center" prop="confirmYear" width="100">
          <template #default="scope">
            <dict-tag v-if="!scope.row.isSummary" :options="sys_ndgl" :value="scope.row.confirmYear"/>
          </template>
        </el-table-column>
        <el-table-column label="是否违约扣款" align="center" prop="hasPenalty" width="120">
          <template #default="scope">
            <span v-if="!scope.row.isSummary">{{ scope.row.hasPenalty === '1' ? '是' : '否' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="扣款金额（元）" align="center" prop="penaltyAmount" width="120">
          <template #default="scope">
            <span :style="scope.row.isSummary ? 'font-weight: bold; color: #F56C6C;' : ''">
              {{ scope.row.penaltyAmount ? formatAmount(scope.row.penaltyAmount) : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="!scope.row.isSummary">{{ scope.row.remark || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 第五部分：合同附件列表 -->
    <el-card class="box-card" style="margin-top: 20px;">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">合同附件列表</span>
      </template>
      <el-table :data="attachmentList" border>
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="文档类型" align="center" prop="documentType" width="120">
          <template #default="scope">
            <dict-tag :options="sys_wdlx" :value="scope.row.documentType"/>
          </template>
        </el-table-column>
        <el-table-column label="文件名称" align="center" prop="fileName" show-overflow-tooltip>
          <template #default="scope">
            <el-link type="primary" @click="handleDownload(scope.row)">
              {{ scope.row.fileName }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="文件说明" align="center" prop="fileDescription" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.fileDescription || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="上传人员" align="center" prop="createByName" width="100" />
        <el-table-column label="上传时间" align="center" prop="createTime" width="180">
          <template #default="scope">
            {{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="100">
          <template #default="scope">
            <el-button link type="primary" icon="Download" @click="handleDownload(scope.row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 第六部分：关联项目列表 -->
    <el-card class="box-card" style="margin-top: 20px;" v-if="projectList.length > 0">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">关联项目列表</span>
      </template>
      <el-table :data="projectList" border>
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="项目名称" align="center" prop="projectName" show-overflow-tooltip>
          <template #default="scope">
            <el-link type="primary" @click="handleViewProject(scope.row.projectId)">
              {{ scope.row.projectName }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="预算金额（元）" align="center" prop="projectBudget" width="150">
          <template #default="scope">
            {{ formatAmount(scope.row.projectBudget) }}
          </template>
        </el-table-column>
        <el-table-column label="预估工作量（人天）" align="center" prop="estimatedWorkload" width="160">
          <template #default="scope">
            {{ scope.row.estimatedWorkload }}
          </template>
        </el-table-column>
        <el-table-column label="实际人天" align="center" prop="actualWorkload" width="150">
          <template #default="scope">
            {{ scope.row.actualWorkload || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 第七部分：备注与审计信息 -->
    <el-card class="box-card" style="margin-top: 20px;">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">备注与审计信息</span>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="备注" :span="2" label-class-name="label-bold">
          {{ detailData.remark || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建人" label-class-name="label-bold">
          {{ detailData.createByName || detailData.createBy }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" label-class-name="label-bold">
          {{ parseTime(detailData.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
        </el-descriptions-item>
        <el-descriptions-item label="更新人" label-class-name="label-bold">
          {{ detailData.updateByName || detailData.updateBy }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间" label-class-name="label-bold">
          {{ parseTime(detailData.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup name="ContractDetail">
import { getContract } from "@/api/project/contract"
import { listPayment } from "@/api/project/payment"
import { listAttachment, downloadAttachment } from "@/api/project/attachment"
import { deptTreeSelect } from "@/api/system/user"
import { listCustomer } from "@/api/project/customer"
import { listProject } from "@/api/project/project"
import { saveAs } from 'file-saver'

const { proxy } = getCurrentInstance()
const { sys_htlx, sys_htzt, sys_ndgl, sys_fkzt, sys_wdlx } = proxy.useDict('sys_htlx', 'sys_htzt', 'sys_ndgl', 'sys_fkzt', 'sys_wdlx')
const route = useRoute()
const router = useRouter()

const detailData = ref({})
const deptOptions = ref([])
const customerOptions = ref([])
const projectOptions = ref([])
const projectList = ref([])
const paymentList = ref([])
const attachmentList = ref([])

/** 付款里程碑列表（带合计行） */
const paymentListWithSummary = computed(() => {
  if (paymentList.value.length === 0) {
    return []
  }

  // 计算合计
  const summary = {
    isSummary: true,
    paymentAmount: 0,
    penaltyAmount: 0
  }

  paymentList.value.forEach(item => {
    summary.paymentAmount += Number(item.paymentAmount) || 0
    summary.penaltyAmount += Number(item.penaltyAmount) || 0
  })

  // 保留两位小数
  summary.paymentAmount = summary.paymentAmount.toFixed(2)
  summary.penaltyAmount = summary.penaltyAmount.toFixed(2)

  // 将合计行放在第一行
  return [summary, ...paymentList.value]
})

/** 查询部门下拉树结构 */
function getDeptTree() {
  deptTreeSelect().then(response => {
    deptOptions.value = response.data
  })
}

/** 查询客户列表 */
function getCustomerList() {
  listCustomer().then(response => {
    customerOptions.value = response.rows
  })
}

/** 查询项目列表 */
function getProjectList() {
  listProject().then(response => {
    projectOptions.value = response.rows
  })
}

/** 根据客户ID获取客户名称 */
function getCustomerName(customerId) {
  if (!customerId) return ''
  const customer = customerOptions.value.find(item => item.customerId === customerId)
  return customer ? customer.customerSimpleName : customerId
}

/** 根据部门ID获取部门名称 */
function getDeptName(deptId) {
  if (!deptId) return ''
  // 递归查找部门名称
  const findDept = (depts, id) => {
    if (!depts || !Array.isArray(depts)) return null
    for (const dept of depts) {
      if (dept.id === id) return dept.label
      if (dept.children && dept.children.length > 0) {
        const found = findDept(dept.children, id)
        if (found) return found
      }
    }
    return null
  }
  const name = findDept(deptOptions.value, deptId)
  return name || deptId
}

/** 根据项目ID获取项目名称 */
function getProjectName(projectId) {
  if (!projectId) return ''
  const project = projectOptions.value.find(item => item.projectId === projectId)
  return project ? project.projectName : projectId
}

/** 格式化金额，保留2位小数 */
function formatAmount(amount) {
  if (amount === null || amount === undefined || amount === '') return ''
  const num = parseFloat(amount)
  if (isNaN(num)) return amount
  return num.toFixed(2)
}

/** 返回按钮 */
function handleBack() {
  router.back()
}

/** 查看项目详情 */
function handleViewProject(projectId) {
  router.push({ path: `/project/project/detail/${projectId}` })
}

/** 下载附件 */
function handleDownload(row) {
  proxy.$modal.loading('正在下载文件，请稍候...')
  downloadAttachment(row.attachmentId).then(response => {
    const blob = new Blob([response])
    saveAs(blob, row.fileName)
    proxy.$modal.closeLoading()
    proxy.$modal.msgSuccess('下载成功')
  }).catch(() => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgError('下载失败')
  })
}

/** 查询付款里程碑列表 */
function getPaymentList(contractId) {
  listPayment({ contractId: contractId }).then(response => {
    paymentList.value = response.rows || []
  })
}

/** 查询附件列表 */
function getAttachmentList(contractId) {
  listAttachment({
    businessType: 'contract',
    businessId: contractId
  }).then(response => {
    attachmentList.value = response.data || []
  })
}

/** 初始化数据 */
function init() {
  getDeptTree()
  getCustomerList()
  getProjectList()

  const contractId = route.params.contractId
  if (contractId) {
    // 查询合同详情
    getContract(contractId).then(response => {
      detailData.value = response.data

      // 查询关联项目列表
      if (response.data.projectList && response.data.projectList.length > 0) {
        projectList.value = response.data.projectList
      }
    })

    // 查询付款里程碑列表
    getPaymentList(contractId)

    // 查询附件列表
    getAttachmentList(contractId)
  }
}

init()
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.label-bold) {
  font-weight: bold;
  background-color: #f5f7fa;
}
</style>
