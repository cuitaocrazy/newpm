<template>
  <div class="app-container">

    <!-- 顶部：左右布局 -->
    <el-row :gutter="16">
      <!-- 左侧：合同基本信息 -->
      <el-col :span="14">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span style="font-size: 16px; font-weight: bold;">合同基本信息</span>
              <el-button style="float: right; padding: 3px 0" link type="primary" icon="Back" @click="handleBack">返回</el-button>
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
      </el-col>

      <!-- 右侧：合同时间与周期 -->
      <el-col :span="10">
        <!-- 合同时间与周期 -->
        <el-card class="box-card">
          <template #header>
            <span style="font-size: 16px; font-weight: bold;">合同时间与周期</span>
          </template>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="合同签订日期" label-class-name="label-bold">
              {{ parseTime(detailData.contractSignDate, '{y}-{m}-{d}') }}
            </el-descriptions-item>
            <el-descriptions-item label="合同周期" label-class-name="label-bold">
              {{ detailData.contractPeriod }} 月
            </el-descriptions-item>
            <el-descriptions-item label="免维期" label-class-name="label-bold">
              {{ detailData.freeMaintenancePeriod }} 月
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三部分：合同金额 -->
    <el-card class="box-card" style="margin-top: 16px;">
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
    <el-card class="box-card" style="margin-top: 16px;">
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
            <a
              v-if="!scope.row.isSummary"
              :href="`/htkx/payment/detail/${scope.row.paymentId}`"
              class="el-link el-link--primary"
              style="text-decoration: none;"
              @click.prevent="router.push(`/htkx/payment/detail/${scope.row.paymentId}`)"
            >{{ scope.row.paymentMethodName }}</a>
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
            <dict-tag v-if="!scope.row.isSummary && scope.row.expectedQuarter" :options="sys_jdgl" :value="scope.row.expectedQuarter"/>
            <span v-else-if="!scope.row.isSummary">-</span>
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
    <el-card class="box-card" style="margin-top: 16px;">
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
    <el-card class="box-card" style="margin-top: 16px;">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">关联项目列表</span>
      </template>
      <el-empty v-if="projectList.length === 0" description="该合同暂无关联项目" :image-size="80" />
      <el-table v-else :data="projectList" border>
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="项目名称" align="left" prop="projectName" width="220" class-name="col-wrap">
          <template #default="scope">
            <a
              :href="`/project/list/detail/${scope.row.projectId}?from=contract&contractId=${detailData.contractId}`"
              class="el-link el-link--primary"
              style="text-decoration: none;"
              @click.prevent="handleViewProject(scope.row.projectId)"
            >{{ scope.row.projectName }}</a>
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
        <el-table-column label="实际人天" align="center" prop="actualWorkload" width="110">
          <template #default="scope">
            {{ scope.row.actualWorkload || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="收入确认年度" align="center" width="120">
          <template #default="scope">
            <dict-tag v-if="scope.row.revenueConfirmYear" :options="sys_ndgl" :value="scope.row.revenueConfirmYear" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="确认状态" align="center" width="110">
          <template #default="scope">
            <dict-tag v-if="scope.row.revenueConfirmStatus != null" :options="sys_qrzt" :value="scope.row.revenueConfirmStatus" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="收入确认团队" align="center" min-width="160" show-overflow-tooltip>
          <template #default="scope">{{ formatDeptPath(scope.row.projectDept) }}</template>
        </el-table-column>
        <el-table-column label="确认金额（元）" align="center" width="130">
          <template #default="scope">{{ scope.row.confirmAmount != null ? formatAmount(scope.row.confirmAmount) : '-' }}</template>
        </el-table-column>
        <el-table-column label="确认日期" align="center" width="110">
          <template #default="scope">{{ scope.row.revenueConfirmDate ? parseTime(scope.row.revenueConfirmDate, '{y}-{m}-{d}') : '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 第七部分：项目分解任务列表 -->
    <el-card class="box-card" style="margin-top: 16px;">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">项目分解任务列表</span>
      </template>
      <el-table :data="contractTaskList" border>
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="投产批次" align="center" prop="batchNo" width="120" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.batchNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="任务编号" align="center" prop="taskCode" width="120" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.taskCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="产品" align="center" prop="product" width="120" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="sys_product" :value="scope.row.product" />
          </template>
        </el-table-column>
        <el-table-column label="任务名称" align="left" prop="taskName" show-overflow-tooltip>
          <template #default="scope">
            <el-link type="primary" @click="router.push(`/project/subproject/detail/${scope.row.taskId}`)">
              {{ scope.row.taskName }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="排期状态" align="center" prop="scheduleStatus" width="110">
          <template #default="scope">
            <dict-tag :options="sys_pqzt" :value="scope.row.scheduleStatus" />
          </template>
        </el-table-column>
        <el-table-column label="预估工作量(人天)" align="center" prop="estimatedWorkload" width="140">
          <template #default="scope">{{ scope.row.estimatedWorkload || '-' }}</template>
        </el-table-column>
        <el-table-column label="实际工作量(人天)" align="center" prop="actualWorkload" width="140">
          <template #default="scope">
            {{ scope.row.actualWorkload ? toPersonDays(scope.row.actualWorkload, scope.row.adjustWorkload) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="功能测试版本日期" align="center" prop="functionalTestDate" width="150">
          <template #default="scope">
            {{ scope.row.functionalTestDate ? parseTime(scope.row.functionalTestDate, '{y}-{m}-{d}') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="计划投产日期" align="center" prop="productionDate" width="130">
          <template #default="scope">
            {{ scope.row.productionDate ? parseTime(scope.row.productionDate, '{y}-{m}-{d}') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="任务负责人" align="center" prop="taskManagerName" width="110">
          <template #default="scope">{{ scope.row.taskManagerName || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 第八部分：备注与审计信息 -->
    <el-card class="box-card" style="margin-top: 16px;">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">备注与审计信息</span>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="备注" :span="2" label-class-name="label-bold">
          {{ detailData.remark || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建人" label-class-name="label-bold">
          {{ detailData.createByName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" label-class-name="label-bold">
          {{ parseTime(detailData.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
        </el-descriptions-item>
        <el-descriptions-item label="更新人" label-class-name="label-bold">
          {{ detailData.updateByName || detailData.createByName }}
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
import { listCustomer } from "@/api/project/customer"
import { listProject, getDeptTree as fetchDeptTree } from '@/api/project/project'
import { saveAs } from 'file-saver'
import request from '@/utils/request'
import { toPersonDays } from '@/utils/workload'

const { proxy } = getCurrentInstance()
const { sys_htlx, sys_htzt, sys_ndgl, sys_fkzt, sys_wdlx, sys_jdgl, sys_product, sys_pqzt, sys_qrzt } = proxy.useDict('sys_htlx', 'sys_htzt', 'sys_ndgl', 'sys_fkzt', 'sys_wdlx', 'sys_jdgl', 'sys_product', 'sys_pqzt', 'sys_qrzt')
const route = useRoute()
const router = useRouter()

const detailData = ref({})
const deptOptions = ref([])
const deptFlatList = ref([])
const customerOptions = ref([])
const projectOptions = ref([])
const projectList = ref([])
const contractTaskList = ref([])
const paymentList = ref([])
const attachmentList = ref([])

/** 关联项目表格展开行（每条团队收入确认一行） */
const projectTableRows = computed(() => {
  const rows = []
  projectList.value.forEach(project => {
    const confirms = project.teamConfirmList || []
    if (confirms.length === 0) {
      rows.push({ ...project, _confirmIdx: 0, _confirmCount: 1, _deptName: null, _confirmAmount: null, _confirmTime: null, _confirmYear: project.revenueConfirmYear })
    } else {
      confirms.forEach((c, i) => {
        rows.push({
          ...project,
          _confirmIdx: i,
          _confirmCount: confirms.length,
          _deptName: c.deptName,
          _confirmAmount: c.confirmAmount,
          _confirmTime: c.confirmTime,
          _confirmYear: c.revenueConfirmYear ?? project.revenueConfirmYear
        })
      })
    }
  })
  return rows
})

/** span-method：前9列均为项目级字段，按团队确认条数合并 */
function projectSpanMethod({ row, columnIndex }) {
  if (columnIndex < 9) {
    return row._confirmIdx === 0 ? [row._confirmCount, 1] : [0, 0]
  }
  return [1, 1]
}

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
  fetchDeptTree().then(response => {
    deptFlatList.value = response.data || []
    const deptData = response.data.map((dept) => ({
      ...dept,
      id: dept.deptId,
      label: dept.deptName
    }))
    deptOptions.value = proxy.handleTree(deptData, "id")
  })
}

/** 格式化部门路径（三级及以下，以 - 分隔） */
function formatDeptPath(deptId) {
  if (!deptId) return '-'
  const id = parseInt(deptId)
  if (!id) return String(deptId)
  const dept = deptFlatList.value.find(d => d.deptId === id)
  if (!dept) return String(deptId)
  const ancestorIds = (dept.ancestors || '').split(',').map(Number).filter(n => n > 0)
  const fullPath = [...ancestorIds, id]
  const displayIds = fullPath.length > 2 ? fullPath.slice(2) : fullPath
  const flatMap = {}
  deptFlatList.value.forEach(d => { flatMap[d.deptId] = d })
  return displayIds.map(did => flatMap[did]?.deptName || String(did)).join(' - ')
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
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 返回按钮 */
function handleBack() {
  router.back()
}

/** 查看项目详情 */
function handleViewProject(projectId) {
  router.push({
    path: `/project/list/detail/${projectId}`,
    query: { from: 'contract', contractId: detailData.value.contractId }
  })
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
        // 加载所有关联项目的任务列表
        const taskPromises = response.data.projectList.map(p =>
          request({ url: '/project/task/list', method: 'get', params: { projectId: p.projectId } })
            .then(r => r.rows || [])
            .catch(() => [])
        )
        Promise.all(taskPromises).then(results => {
          contractTaskList.value = results.flat().filter(t => t.taskId != null)
        })
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
:deep(.col-wrap .cell) {
  white-space: normal;
  word-break: break-word;
  line-height: 1.6;
}
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
