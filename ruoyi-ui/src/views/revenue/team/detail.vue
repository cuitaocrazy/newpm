<template>
  <div class="app-container">
    <el-page-header @back="goBack" title="团队收入确认详情" />

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 左侧：项目详情（60%） -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: bold;">项目详情</span>
          </template>

          <!-- 项目基本信息 -->
          <div class="section-title">项目基本信息</div>
          <el-descriptions :column="2" border label-width="120px">
            <el-descriptions-item label="行业"><dict-tag :options="industry" :value="form.industry" /></el-descriptions-item>
            <el-descriptions-item label="一级区域">{{ getDictLabel(sys_yjqy, form.region) }}</el-descriptions-item>
            <el-descriptions-item label="二级区域">{{ form.regionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="立项年度"><dict-tag :options="sys_ndgl" :value="form.establishedYear" /></el-descriptions-item>
            <el-descriptions-item label="项目编号">{{ form.projectCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目预算">{{ formatAmount(form.projectBudget) }} 元</el-descriptions-item>
            <el-descriptions-item label="项目名称" :span="2">{{ form.projectName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目分类">{{ getDictLabel(sys_xmfl, form.projectCategory) }}</el-descriptions-item>
            <el-descriptions-item label="项目部门">{{ deptPath || form.deptName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="预估工作量">{{ form.estimatedWorkload != null ? form.estimatedWorkload + ' 人天' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="实际人天">{{ form.actualWorkload != null ? toPersonDays(form.actualWorkload, form.adjustWorkload) + ' 人天' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目状态">{{ getDictLabel(sys_xmzt, form.projectStatus) }}</el-descriptions-item>
            <el-descriptions-item label="验收状态"><dict-tag :options="sys_yszt" :value="form.acceptanceStatus" /></el-descriptions-item>
            <el-descriptions-item label="合同金额">{{ formatAmount(form.contractAmount) }} 元</el-descriptions-item>
            <el-descriptions-item label="审核状态">{{ getDictLabel(sys_spzt, form.approvalStatus) }}</el-descriptions-item>
            <el-descriptions-item label="合同状态"><dict-tag :options="sys_htzt" :value="form.contractStatus" /></el-descriptions-item>
            <el-descriptions-item label="合同名称"><span style="white-space: pre-line;">{{ form.contractName || '-' }}</span></el-descriptions-item>
            <el-descriptions-item label="收入确认状态"><dict-tag :options="sys_qrzt" :value="form.revenueConfirmStatus" /></el-descriptions-item>
            <el-descriptions-item label="收入确认年度">{{ form.revenueConfirmYear || '-' }}</el-descriptions-item>
            <el-descriptions-item label="确认金额（含税）">{{ formatAmount(form.confirmAmount) }} 元</el-descriptions-item>
            <el-descriptions-item label="税率">{{ form.taxRate != null ? form.taxRate + '%' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="税后金额" :span="2">{{ formatAmount(form.afterTaxAmount) }} 元</el-descriptions-item>
            <el-descriptions-item label="项目地址" :span="2">{{ form.projectAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目计划" :span="2"><span style="white-space: pre-line;">{{ form.projectPlan || '-' }}</span></el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="2"><span style="white-space: pre-line;">{{ form.projectDescription || '-' }}</span></el-descriptions-item>
            <el-descriptions-item label="审核意见" :span="2">{{ form.approvalReason || '-' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 人员配置 -->
          <div class="section-title">人员配置</div>
          <el-descriptions :column="2" border label-width="120px">
            <el-descriptions-item label="项目经理">{{ form.projectManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="市场经理">{{ form.marketManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="销售负责人" :span="2">{{ form.salesManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="销售联系方式" :span="2">{{ form.salesContact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="参与人员" :span="2">{{ participantsDisplay }}</el-descriptions-item>
          </el-descriptions>

          <!-- 客户信息 -->
          <div class="section-title">客户信息</div>
          <el-descriptions :column="2" border label-width="120px">
            <el-descriptions-item label="客户名称" :span="2">{{ form.customerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户联系人">{{ form.customerContactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户联系方式">{{ customerContactPhone || form.customerContactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商户联系人">{{ form.merchantContact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商户联系方式">{{ form.merchantPhone || '-' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 时间规划 -->
          <div class="section-title">时间规划</div>
          <el-descriptions :column="2" border label-width="120px">
            <el-descriptions-item label="启动日期">{{ parseTime(form.startDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结束日期">{{ parseTime(form.endDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="实施年度" :span="2">{{ form.reservedField1 || '-' }}</el-descriptions-item>
            <el-descriptions-item label="投产日期">{{ parseTime(form.productionDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="验收日期">{{ parseTime(form.acceptanceDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 成本预算 -->
          <div class="section-title">成本预算</div>
          <el-descriptions :column="2" border label-width="120px">
            <el-descriptions-item label="项目费用">{{ form.projectCost != null ? formatAmount(form.projectCost) + ' 元' : '- 元' }}</el-descriptions-item>
            <el-descriptions-item label="费用预算">{{ form.expenseBudget != null ? formatAmount(form.expenseBudget) + ' 元' : '- 元' }}</el-descriptions-item>
            <el-descriptions-item label="成本预算">{{ form.costBudget != null ? formatAmount(form.costBudget) + ' 元' : '- 元' }}</el-descriptions-item>
            <el-descriptions-item label="人力费用">{{ form.laborCost != null ? formatAmount(form.laborCost) + ' 元' : '- 元' }}</el-descriptions-item>
            <el-descriptions-item label="采购成本">{{ form.purchaseCost != null ? formatAmount(form.purchaseCost) + ' 元' : '- 元' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 其他信息 -->
          <div class="section-title">其他信息</div>
          <el-descriptions :column="1" border label-width="120px">
            <el-descriptions-item label="备注"><span style="white-space: pre-line;">{{ form.remark || '-' }}</span></el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧（40%） -->
      <el-col :span="10">
        <!-- 公司收入确认信息 -->
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: bold;">公司收入确认信息</span>
          </template>

          <!-- 状态横幅 -->
          <div class="status-banner" :class="getStatusBannerClass(form.revenueConfirmStatus)">
            {{ getDictLabel(sys_qrzt, form.revenueConfirmStatus) }}
          </div>

          <!-- 验收状态 -->
          <div class="info-row">
            <div class="info-label">验收状态</div>
            <dict-tag :options="sys_yszt" :value="form.acceptanceStatus" />
          </div>

          <!-- 确认年度 -->
          <div class="info-row">
            <div class="info-label"><el-icon><Calendar /></el-icon> 确认年度</div>
            <div class="info-value">{{ form.revenueConfirmYear || '-' }}</div>
          </div>

          <!-- 确认金额 -->
          <div class="amount-box">
            <div class="amount-box-label"><el-icon><Tickets /></el-icon> 确认金额(含税)</div>
            <div class="amount-value-red">¥ {{ formatAmount(form.confirmAmount) }}</div>
          </div>

          <!-- 税后金额 -->
          <div class="info-row">
            <div class="info-label">税后金额</div>
            <div class="info-value amount-blue-bold">¥ {{ formatAmount(form.afterTaxAmount) }}</div>
          </div>

          <!-- 税率 -->
          <div class="info-row">
            <div class="info-label">税率</div>
            <div class="info-value">{{ form.taxRate != null ? form.taxRate + '%' : '-' }}</div>
          </div>

          <el-divider style="margin: 16px 0;" />

          <!-- 确认人 -->
          <div class="info-row">
            <div class="info-label"><el-icon><User /></el-icon> 确认人</div>
            <div class="info-value">{{ form.companyRevenueConfirmedBy || '-' }}</div>
          </div>

          <!-- 确认时间 -->
          <div class="info-row">
            <div class="info-label"><el-icon><Clock /></el-icon> 确认时间</div>
            <div class="info-value">{{ parseTime(form.companyRevenueConfirmedTime, '{y}-{m}-{d}') || '-' }}</div>
          </div>

          <div class="action-buttons">
            <el-button @click="goBack" style="width: 100%;">返回列表</el-button>
          </div>
        </el-card>

        <!-- 团队收入确认明细 -->
        <el-card shadow="never" style="margin-top: 16px;">
          <template #header>
            <span style="font-weight: bold;">团队收入确认明细</span>
          </template>
          <el-table :data="detailList" border size="small" empty-text="暂无团队收入确认数据">
            <el-table-column label="部门名称" prop="deptName" min-width="120" show-overflow-tooltip />
            <el-table-column label="确认金额" prop="confirmAmount" min-width="110" align="right">
              <template #default="{ row }">{{ formatAmount(row.confirmAmount) }} 元</template>
            </el-table-column>
            <el-table-column label="确认时间" prop="confirmTime" min-width="100">
              <template #default="{ row }">{{ parseTime(row.confirmTime, '{y}-{m}-{d}') }}</template>
            </el-table-column>
            <el-table-column label="确认人" prop="confirmUserName" min-width="80" show-overflow-tooltip />
            <el-table-column label="备注" prop="remark" min-width="100" show-overflow-tooltip />
          </el-table>
          <div v-if="detailList.length === 0" style="text-align: center; color: #909399; padding: 20px 0;">
            暂无团队收入确认数据
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="TeamRevenueDetail">
import { ref, computed, onMounted, onActivated, watch, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getTeamRevenue } from '@/api/revenue/team'
import { getDeptTree, getUsersByPost } from '@/api/project/project'
import request from '@/utils/request'
import { toPersonDays } from '@/utils/workload'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()

const { industry, sys_yjqy, sys_xmfl, sys_spzt, sys_yszt, sys_qrzt, sys_ndgl, sys_xmzt, sys_htzt } = proxy.useDict(
  'industry', 'sys_yjqy', 'sys_xmfl', 'sys_spzt', 'sys_yszt', 'sys_qrzt', 'sys_ndgl', 'sys_xmzt', 'sys_htzt'
)

const form = ref({})
const detailList = ref([])
const allUsers = ref([])
const selectedParticipants = ref([])
const customerContactPhone = ref('')
const deptPath = ref('')

/** 参与人员显示：优先用后端返回的名称字符串，其次用已解析的 tag 列表 */
const participantsDisplay = computed(() => {
  if (form.value.participantsNames) return form.value.participantsNames
  if (selectedParticipants.value.length > 0) return selectedParticipants.value.map(u => u.nickName).join('、')
  return '-'
})

/** 根据字典数据获取标签文本 */
function getDictLabel(dicts, value) {
  if (value === null || value === undefined || value === '') return '-'
  const dictArr = Array.isArray(dicts) ? dicts : (dicts?.value || [])
  const dict = dictArr.find(d => String(d.value) === String(value))
  return dict ? dict.label : (value || '-')
}

/** 根据收入确认状态返回横幅样式类 */
function getStatusBannerClass(status) {
  const dictArr = sys_qrzt.value || []
  const dict = dictArr.find(d => String(d.value) === String(status))
  const elTagType = dict?.elTagType || 'default'
  const typeMap = {
    'success': 'status-success',
    'warning': 'status-warning',
    'danger':  'status-danger',
    'primary': 'status-primary',
    'info':    'status-info',
    'default': 'status-default',
    '':        'status-default',
  }
  return typeMap[elTagType] || 'status-default'
}

/** 格式化金额为千分位 */
function formatAmount(amount) {
  if (amount === null || amount === undefined || amount === '') return '-'
  const num = parseFloat(amount)
  if (isNaN(num)) return '-'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 返回 */
function goBack() {
  router.back()
}

/** 加载所有用户（用于参与人员显示） */
function loadAllUsers() {
  getUsersByPost().then(response => {
    allUsers.value = response.data || []
  })
}

/** 加载客户联系人电话 */
function loadCustomerContactPhone(customerId, contactId) {
  if (!customerId || !contactId) return
  request({
    url: '/project/customer/contact/listByCustomer',
    method: 'get',
    params: { customerId }
  }).then(response => {
    if (response.data && response.data.length > 0) {
      const contact = response.data.find(c => c.contactId === contactId)
      if (contact) customerContactPhone.value = contact.contactPhone || ''
    }
  }).catch(() => {
    customerContactPhone.value = ''
  })
}

/** 构建部门路径（三级及以下机构，用 - 隔开） */
function buildDeptPath(deptId) {
  if (!deptId) { deptPath.value = ''; return }
  getDeptTree().then(response => {
    const allDepts = response.data || []
    const currentDept = allDepts.find(d => d.deptId === deptId)
    if (!currentDept) { deptPath.value = ''; return }
    const ancestorIds = (currentDept.ancestors || '').split(',').filter(id => id && id !== '0')
    const level = ancestorIds.length + 1
    if (level < 3) { deptPath.value = ''; return }
    const pathParts = []
    ancestorIds.forEach(ancestorId => {
      const dept = allDepts.find(d => d.deptId === parseInt(ancestorId))
      if (dept) {
        const ancestorLevel = dept.ancestors ? dept.ancestors.split(',').filter(id => id && id !== '0').length + 1 : 1
        if (ancestorLevel >= 3) pathParts.push(dept.deptName)
      }
    })
    pathParts.push(currentDept.deptName)
    deptPath.value = pathParts.join('-')
  }).catch(() => { deptPath.value = '' })
}

/** 获取详情 */
function getDetail() {
  const projectId = route.params.projectId
  getTeamRevenue(projectId).then(response => {
    const projectData = response.data.project || {}
    if (projectData.participants) {
      projectData.participants = projectData.participants.split(',').map(Number)
    }
    form.value = projectData
    detailList.value = response.data.detailList || []

    if (projectData.customerId && projectData.customerContactId) {
      loadCustomerContactPhone(projectData.customerId, projectData.customerContactId)
    }
    if (projectData.projectDept) {
      buildDeptPath(parseInt(projectData.projectDept))
    }

    setTimeout(() => {
      if (form.value.participants && form.value.participants.length > 0) {
        selectedParticipants.value = allUsers.value.filter(u =>
          form.value.participants.includes(u.userId)
        )
      }
    }, 500)
  })
}

onMounted(() => {
  loadAllUsers()
  getDetail()
})

onActivated(() => {
  getDetail()
})

watch(
  () => route.params.projectId,
  (newId, oldId) => {
    if (newId && newId !== oldId) getDetail()
  }
)
</script>

<style scoped>
/* 固定左侧所有 descriptions 的 label 列宽 */
:deep(.el-descriptions__label) {
  width: 120px;
  min-width: 120px;
  max-width: 120px;
  white-space: nowrap;
}

:deep(.el-descriptions__content) {
  min-width: 0;
}

:deep(.el-descriptions__body table) {
  table-layout: fixed;
  width: 100%;
}

.section-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-top: 20px;
  margin-bottom: 10px;
  padding-left: 10px;
  border-left: 3px solid #409EFF;
}

.section-title:first-child {
  margin-top: 0;
}

/* 右侧查看模式 */
.status-banner {
  text-align: center;
  font-size: 16px;
  font-weight: bold;
  padding: 12px 0;
  border-radius: 6px;
  margin-bottom: 20px;
}
.status-success { background: #f0f9eb; color: #67c23a; }
.status-warning { background: #fdf6ec; color: #e6a23c; }
.status-danger  { background: #fef0f0; color: #f56c6c; }
.status-primary { background: #ecf5ff; color: #409eff; }
.status-info    { background: #f4f4f5; color: #909399; }
.status-default { background: #f4f4f5; color: #909399; }

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 14px;
  color: #303133;
}
.info-label {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
  font-size: 13px;
  flex-shrink: 0;
}
.info-value {
  font-weight: 500;
  color: #303133;
}

.amount-box {
  border: 1px dashed #f56c6c;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 14px;
  background: #fff8f8;
}
.amount-box-label {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
  font-size: 13px;
  margin-bottom: 6px;
}
.amount-value-red {
  font-size: 22px;
  font-weight: bold;
  color: #f56c6c;
}

.amount-blue-bold {
  font-size: 16px;
  font-weight: bold;
  color: #409eff;
}

.action-buttons {
  margin-top: 20px;
}
</style>
