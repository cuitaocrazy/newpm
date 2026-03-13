<template>
  <div class="app-container">
    <h2 style="margin: 0 0 16px 0; font-weight: bold;">任务详情</h2>

    <div v-loading="loading">
      <!-- 一、所属项目 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">一、选择所属项目</span></template>
        <el-row :gutter="20" style="margin-bottom: 12px;">
          <el-col :span="8">
            <el-form-item label="所属机构" label-width="100px">
              <project-dept-select :model-value="parentProjectDept" disabled style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="项目名称" label-width="100px">
              <el-input :model-value="form.parentProjectName" disabled style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <div v-if="selectedProject">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="行业">
              <dict-tag :options="industry" :value="selectedProject.industry" />
            </el-descriptions-item>
            <el-descriptions-item label="一级区域">
              <dict-tag :options="sys_yjqy" :value="selectedProject.region" />
            </el-descriptions-item>
            <el-descriptions-item label="二级区域">{{ selectedProject.regionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="简称">{{ selectedProject.shortName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="立项年份">
              <dict-tag :options="sys_ndgl" :value="selectedProject.establishedYear" />
            </el-descriptions-item>
            <el-descriptions-item label="项目ID">{{ selectedProject.projectCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目名称" :span="3">{{ selectedProject.projectName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目分类">
              <dict-tag :options="sys_xmfl" :value="selectedProject.projectCategory" />
            </el-descriptions-item>
            <el-descriptions-item label="项目部门">{{ getDeptName(selectedProject.projectDept) }}</el-descriptions-item>
            <el-descriptions-item label="预估工作量">{{ selectedProject.estimatedWorkload || 0 }} 人天</el-descriptions-item>
            <el-descriptions-item label="项目状态">
              <dict-tag :options="sys_xmzt" :value="selectedProject.projectStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="验收状态">
              <dict-tag :options="sys_yszt" :value="selectedProject.acceptanceStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="实际人天">
              {{ selectedProject.actualWorkload != null ? parseFloat(selectedProject.actualWorkload).toFixed(3) : '0.000' }} 人天
            </el-descriptions-item>
            <el-descriptions-item label="审核状态">
              <dict-tag :options="sys_spzt" :value="selectedProject.approvalStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="项目预算">{{ formatAmount(selectedProject.projectBudget) }} 元</el-descriptions-item>
            <el-descriptions-item label="合同状态">
              <dict-tag :options="sys_htzt" :value="selectedProject.contractStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="合同名称" :span="2">{{ selectedProject.contractName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="合同编号">{{ selectedProject.contractCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="合同金额">
              {{ selectedProject.contractAmount != null ? formatAmount(selectedProject.contractAmount) + ' 元' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="收入确认年度">
              <dict-tag :options="sys_ndgl" :value="selectedProject.revenueConfirmYear" />
            </el-descriptions-item>
            <el-descriptions-item label="收入确认状态">
              <dict-tag :options="sys_qrzt" :value="selectedProject.revenueConfirmStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="项目经理">{{ selectedProject.projectManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="市场经理">{{ selectedProject.marketManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户名称" :span="2">{{ projectCustomerName || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>

      <!-- 本项目已分解的任务 -->
      <el-card v-if="selectedProject" shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">本项目已分解的任务</span></template>
        <div v-if="siblingTasks.length > 0">
          <el-table :data="siblingTasks" border size="small" style="width: 100%">
            <el-table-column type="index" label="序号" width="55" align="center" />
            <el-table-column label="投产批次" prop="batchNo" width="120" align="center" />
            <el-table-column label="任务编号" prop="taskCode" width="130" />
            <el-table-column label="产品" prop="product" width="120">
              <template #default="scope">
                <dict-tag :options="sys_product" :value="scope.row.product" />
              </template>
            </el-table-column>
            <el-table-column label="任务名称" prop="taskName" min-width="160" show-overflow-tooltip />
            <el-table-column label="排期状态" prop="scheduleStatus" width="110" align="center">
              <template #default="scope">
                <dict-tag :options="sys_pqzt" :value="scope.row.scheduleStatus" />
              </template>
            </el-table-column>
            <el-table-column label="预估工作量" prop="estimatedWorkload" width="100" align="right" />
            <el-table-column label="功能测试版本日期" prop="functionalTestDate" width="140" align="center">
              <template #default="scope">{{ formatDate(scope.row.functionalTestDate) }}</template>
            </el-table-column>
            <el-table-column label="计划投产日期" prop="planProductionDate" width="120" align="center">
              <template #default="scope">{{ formatDate(scope.row.planProductionDate) }}</template>
            </el-table-column>
            <el-table-column label="任务负责人" prop="taskManagerName" width="100" align="center" />
          </el-table>
        </div>
        <el-empty v-else description="本项目暂无已分解项目" :image-size="80" />
      </el-card>

      <!-- 二、任务信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">二、任务信息</span></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务名称">{{ form.taskName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务负责人">{{ form.taskManagerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务编号">{{ form.taskCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务阶段">
            <dict-tag :options="sys_xmjd" :value="form.taskStage" />
          </el-descriptions-item>
          <el-descriptions-item label="总行需求号">{{ form.bankDemandNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="软件中心需求编号">{{ form.softwareDemandNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="产品">
            <dict-tag :options="sys_product" :value="form.product" />
          </el-descriptions-item>
          <el-descriptions-item label="排期状态">
            <dict-tag :options="sys_pqzt" :value="form.scheduleStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="预估工作量">
            {{ form.estimatedWorkload != null ? form.estimatedWorkload + ' 人天' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="实际工作量">
            <el-input :model-value="form.actualWorkload != null ? parseFloat(form.actualWorkload).toFixed(3) + ' 人天' : '-'" disabled />
          </el-descriptions-item>
          <el-descriptions-item label="任务预算">
            {{ form.taskBudget != null ? formatAmount(form.taskBudget) + ' 元' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="投产年度">
            <dict-tag :options="sys_ndgl" :value="form.productionYear" />
          </el-descriptions-item>
          <el-descriptions-item label="投产批次">{{ form.batchNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="计划投产日期">
            {{ form.planProductionDate ? form.planProductionDate.substring(0, 10) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="启动日期">{{ form.startDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ form.endDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内部B包日期">{{ formatDate(form.internalClosureDate) }}</el-descriptions-item>
          <el-descriptions-item label="功能测试版本日期">{{ formatDate(form.functionalTestDate) }}</el-descriptions-item>
          <el-descriptions-item label="生产版本日期">{{ formatDate(form.productionVersionDate) }}</el-descriptions-item>
          <el-descriptions-item label="实际投产日期">{{ formatDate(form.actualProductionDate) }}</el-descriptions-item>
          <el-descriptions-item label="功能点说明" :span="2">
            <div class="text-content">{{ form.functionDescription || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="实施计划" :span="2">
            <div class="text-content">{{ form.implementationPlan || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            <div class="text-content">{{ form.remark || '-' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 三、合同信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">三、合同信息</span></template>
        <div v-if="contractInfo">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="合同名称" :span="3">{{ contractInfo.contractName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="合同状态">
              <dict-tag :options="sys_htzt" :value="contractInfo.contractStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="合同类型">
              <dict-tag :options="sys_htlx" :value="contractInfo.contractType" />
            </el-descriptions-item>
            <el-descriptions-item label="合同签订日期">{{ contractInfo.contractSignDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="合同周期">
              {{ contractInfo.contractPeriod ? contractInfo.contractPeriod + ' 月' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="免维期">
              {{ contractInfo.freeMaintenancePeriod ? contractInfo.freeMaintenancePeriod + ' 月' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="税率">
              {{ contractInfo.taxRate ? contractInfo.taxRate + ' %' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="合同金额">
              {{ contractInfo.contractAmount != null ? formatAmount(contractInfo.contractAmount) + ' 元' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="不含税金额">
              {{ contractInfo.amountNoTax != null ? formatAmount(contractInfo.amountNoTax) + ' 元' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="操作" :span="3">
              <el-button type="primary" link @click="viewContract(contractInfo.contractId)"
                v-if="checkPermi(['project:contract:query'])">查看详情</el-button>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <el-empty v-else description="暂无关联合同" :image-size="80" />
      </el-card>

      <!-- 四、付款里程碑信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">四、付款里程碑信息</span></template>
        <el-table :data="paymentListWithSummary" border size="small" style="width: 100%">
          <el-table-column label="序号" width="60" align="center">
            <template #default="scope">
              <span v-if="scope.row.isSummary" style="font-weight: bold;">合计</span>
              <span v-else>{{ scope.$index }}</span>
            </template>
          </el-table-column>
          <el-table-column label="里程碑名称" prop="paymentMethodName" min-width="160" show-overflow-tooltip>
            <template #default="scope">
              <span v-if="!scope.row.isSummary">{{ scope.row.paymentMethodName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="付款金额（元）" prop="paymentAmount" width="140" align="right">
            <template #default="scope">
              <span :style="scope.row.isSummary ? 'font-weight: bold; color: #409EFF;' : ''">
                {{ scope.row.paymentAmount != null ? formatAmount(scope.row.paymentAmount) : '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="付款状态" prop="paymentStatus" width="100" align="center">
            <template #default="scope">
              <dict-tag v-if="!scope.row.isSummary" :options="sys_fkzt" :value="scope.row.paymentStatus" />
            </template>
          </el-table-column>
          <el-table-column label="预计回款季度" prop="expectedQuarter" width="120" align="center">
            <template #default="scope">
              <template v-if="!scope.row.isSummary">
                <dict-tag v-if="scope.row.expectedQuarter" :options="sys_jdgl" :value="scope.row.expectedQuarter" />
                <span v-else>-</span>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="实际回款季度" prop="actualQuarter" width="120" align="center">
            <template #default="scope">
              <template v-if="!scope.row.isSummary">
                <dict-tag v-if="scope.row.actualQuarter" :options="sys_jdgl" :value="scope.row.actualQuarter" />
                <span v-else>-</span>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="实际回款日期" prop="actualPaymentDate" width="120" align="center">
            <template #default="scope">
              <span v-if="!scope.row.isSummary">
                {{ scope.row.actualPaymentDate ? formatDate(scope.row.actualPaymentDate) : '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="确认年份" prop="confirmYear" width="100" align="center">
            <template #default="scope">
              <dict-tag v-if="!scope.row.isSummary && scope.row.confirmYear" :options="sys_ndgl" :value="scope.row.confirmYear" />
            </template>
          </el-table-column>
          <el-table-column label="是否违约扣款" prop="hasPenalty" width="120" align="center">
            <template #default="scope">
              <span v-if="!scope.row.isSummary">{{ scope.row.hasPenalty === '1' ? '是' : '否' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="扣款金额（元）" prop="penaltyAmount" width="130" align="right">
            <template #default="scope">
              <span :style="scope.row.isSummary ? 'font-weight: bold; color: #F56C6C;' : ''">
                {{ scope.row.penaltyAmount ? formatAmount(scope.row.penaltyAmount) : '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip>
            <template #default="scope">
              <span v-if="!scope.row.isSummary">{{ scope.row.remark || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 系统信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">系统信息</span></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="创建人">{{ form.createByName || form.createBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ form.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新人">{{ form.updateByName || form.updateBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ form.updateTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <div class="form-footer">
      <el-button size="large" @click="goBack">返回</el-button>
    </div>
  </div>
</template>

<script setup name="SubprojectDetail">
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getTask } from '@/api/project/task'
import { getProject, getContractByProjectId } from '@/api/project/project'
import { listPayment } from '@/api/project/payment'
import { checkPermi } from '@/utils/permission'
import request from '@/utils/request'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const { industry, sys_yjqy, sys_xmfl, sys_xmzt, sys_yszt, sys_spzt, sys_htzt, sys_htlx, sys_ndgl, sys_qrzt, sys_xmjd, sys_product, sys_pqzt, sys_fkzt, sys_jdgl } =
  proxy.useDict('industry', 'sys_yjqy', 'sys_xmfl', 'sys_xmzt', 'sys_yszt', 'sys_spzt', 'sys_htzt', 'sys_htlx', 'sys_ndgl', 'sys_qrzt', 'sys_xmjd', 'sys_product', 'sys_pqzt', 'sys_fkzt', 'sys_jdgl')

const loading = ref(false)
const form = ref({})
const selectedProject = ref(null)
const projectCustomerName = ref('')
const parentProjectDept = ref(null)
const deptFlatList = ref([])
const siblingTasks = ref([])
const contractInfo = ref(null)
const paymentList = ref([])

const paymentListWithSummary = computed(() => {
  if (paymentList.value.length === 0) return []
  const summary = { isSummary: true, paymentAmount: 0, penaltyAmount: 0 }
  paymentList.value.forEach(item => {
    summary.paymentAmount += Number(item.paymentAmount) || 0
    summary.penaltyAmount += Number(item.penaltyAmount) || 0
  })
  summary.paymentAmount = summary.paymentAmount.toFixed(2)
  summary.penaltyAmount = summary.penaltyAmount.toFixed(2)
  return [summary, ...paymentList.value]
})

function getDeptName(deptId) {
  if (!deptId) return '-'
  const numId = typeof deptId === 'string' ? parseInt(deptId) : deptId
  const dept = deptFlatList.value.find(d => d.deptId === numId)
  if (!dept) return '-'
  const ancestorIds = dept.ancestors ? dept.ancestors.split(',').filter(id => id && id !== '0') : []
  const pathDepts = []
  if (ancestorIds.length >= 2) {
    for (let i = 2; i < ancestorIds.length; i++) {
      const ancestor = deptFlatList.value.find(d => d.deptId === parseInt(ancestorIds[i]))
      if (ancestor) pathDepts.push(ancestor.deptName)
    }
  }
  pathDepts.push(dept.deptName)
  return pathDepts.join('-')
}

function formatAmount(value) {
  if (value === null || value === undefined || value === '') return '-'
  const num = parseFloat(String(value).replace(/,/g, ''))
  if (isNaN(num)) return '-'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(val) {
  if (!val) return '-'
  return String(val).substring(0, 10)
}

function viewContract(contractId) {
  router.push(`/htkx/contract/detail/${contractId}`)
}

function goBack() {
  router.push({ path: '/task/subproject', query: { parentId: form.value.projectId } })
}

onMounted(async () => {
  request({ url: '/project/project/deptTreeAll', method: 'get' })
    .then(res => { deptFlatList.value = res.data || [] })
    .catch(() => {})

  const projectId = route.params.taskId || route.params.projectId
  if (!projectId) return
  loading.value = true
  try {
    const res = await getTask(Number(projectId))
    form.value = res.data || {}
    if (form.value.projectId) {
      const parentRes = await getProject(form.value.projectId)
      selectedProject.value = parentRes.data
      parentProjectDept.value = parentRes.data.projectDept ? Number(parentRes.data.projectDept) : null
      request({ url: '/project/task/list', method: 'get', params: { projectId: form.value.projectId } })
        .then(r => { siblingTasks.value = r.rows || [] })
        .catch(() => {})
      if (parentRes.data.customerId) {
        request({ url: `/project/customer/${parentRes.data.customerId}`, method: 'get' })
          .then(r => { projectCustomerName.value = r.data.customerSimpleName || r.data.customerFullName || '' })
          .catch(() => {})
      }
      // 加载合同信息及付款里程碑
      getContractByProjectId(form.value.projectId).then(res => {
        contractInfo.value = res.data || null
        if (contractInfo.value && contractInfo.value.contractId) {
          listPayment({ contractId: contractInfo.value.contractId }).then(r => {
            paymentList.value = r.rows || []
          }).catch(() => {})
        }
      }).catch(() => { contractInfo.value = null })
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.app-container { padding-bottom: 80px; }
.text-content { white-space: pre-wrap; word-break: break-all; }
.form-footer {
  position: sticky; bottom: 0; padding: 20px;
  background-color: #fff; border-top: 1px solid #dcdfe6;
  text-align: center; z-index: 10;
}
</style>
