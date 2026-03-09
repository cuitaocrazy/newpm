<template>
  <div class="app-container project-detail">
    <!-- 页面标题 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <h2>项目详情</h2>
        <p class="tips">查看项目完整信息</p>
      </div>
    </el-card>

    <!-- 左右布局：项目基本信息 + 右侧辅助信息 -->
    <el-row :gutter="16" style="margin-bottom: 0;">
      <!-- 左侧：项目基本信息 -->
      <el-col :span="15">
        <el-card shadow="never" class="detail-card">
          <template #header>
            <span class="card-title">项目基本信息</span>
          </template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="行业">
              <dict-tag :options="industry" :value="form.industry" />
            </el-descriptions-item>
            <el-descriptions-item label="一级区域">
              <dict-tag :options="sys_yjqy" :value="form.region" />
            </el-descriptions-item>
            <el-descriptions-item label="二级区域">
              {{ form.regionName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="简称">
              {{ form.shortName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="立项年份">
              <dict-tag :options="sys_ndgl" :value="form.establishedYear" />
            </el-descriptions-item>
            <el-descriptions-item label="项目ID">
              {{ form.projectCode || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="项目名称" :span="3">
              {{ form.projectName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="项目分类">
              <dict-tag :options="sys_xmfl" :value="form.projectCategory" />
            </el-descriptions-item>
            <el-descriptions-item label="项目部门">
              {{ getDeptName(form.projectDept) }}
            </el-descriptions-item>
            <el-descriptions-item label="预估工作量">
              {{ form.estimatedWorkload || 0 }} 人天
            </el-descriptions-item>
            <el-descriptions-item label="项目状态">
              <dict-tag :options="sys_xmzt" :value="form.projectStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="验收状态">
              <dict-tag :options="sys_yszt" :value="form.acceptanceStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="实际人天">
              {{ form.actualWorkload != null ? parseFloat(form.actualWorkload).toFixed(3) : '0.000' }} 人天
            </el-descriptions-item>
            <el-descriptions-item label="审核状态">
              <dict-tag :options="sys_spzt" :value="form.approvalStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="项目预算">
              {{ formatAmount(form.projectBudget) }} 元
            </el-descriptions-item>
            <el-descriptions-item label="合同状态">
              <dict-tag :options="sys_htzt" :value="form.contractStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="合同名称" :span="2">
              {{ form.contractName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="合同编号">
              {{ form.contractCode || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="合同金额">
              {{ form.contractAmount != null ? formatAmount(form.contractAmount) + ' 元' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="收入确认年度">
              <dict-tag :options="sys_ndgl" :value="form.revenueConfirmYear" />
            </el-descriptions-item>
            <el-descriptions-item label="收入确认状态">
              <dict-tag :options="sys_qrzt" :value="form.revenueConfirmStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="收入确认金额">
              {{ form.confirmAmount != null ? formatAmount(form.confirmAmount) + ' 元' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="项目阶段" :span="2">
              <dict-tag :options="sys_xmjd" :value="form.projectStage" />
            </el-descriptions-item>
            <el-descriptions-item label="项目地址" :span="3">
              {{ form.projectAddress || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="项目计划" :span="3">
              <div class="text-content">{{ form.projectPlan || '-' }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="3">
              <div class="text-content">{{ form.projectDescription || '-' }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="审核意见" :span="3">
              {{ form.approvalReason || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧：人员配置 + 客户信息 + 时间规划 -->
      <el-col :span="9">
        <!-- 人员配置 -->
        <el-card shadow="never" class="detail-card">
          <template #header>
            <span class="card-title">人员配置</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目经理">
              {{ form.projectManagerName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="市场经理">
              {{ form.marketManagerName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="销售负责人">
              {{ form.salesManagerName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="销售联系方式">
              {{ form.salesContact || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="参与人员" :span="2">
              <div v-if="selectedParticipants.length > 0" class="selected-participants">
                <el-tag v-for="user in selectedParticipants" :key="user.userId"
                  type="info" class="participant-tag">
                  {{ user.nickName }}
                </el-tag>
              </div>
              <span v-else style="color: #909399;">暂无参与人员</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 客户信息 -->
        <el-card shadow="never" class="detail-card">
          <template #header>
            <span class="card-title">客户信息</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="客户名称" :span="2">
              {{ form.customerName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="客户联系人">
              {{ form.customerContactName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="客户联系方式">
              {{ customerContactPhone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="商户联系人">
              {{ form.merchantContact || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="商户联系方式">
              {{ form.merchantPhone || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 时间规划 -->
        <el-card shadow="never" class="detail-card">
          <template #header>
            <span class="card-title">时间规划</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="启动日期">
              {{ form.startDate || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="结束日期">
              {{ form.endDate || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="投产日期">
              {{ form.productionDate || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="验收日期">
              {{ form.acceptanceDate || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <!-- 成本预算 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">成本预算</span>
      </template>
      <el-descriptions :column="5" border>
        <el-descriptions-item label="项目预算">
          {{ formatAmount(form.projectBudget) }} 元
        </el-descriptions-item>
        <el-descriptions-item label="费用预算">
          {{ formatAmount(form.costBudget) }} 元
        </el-descriptions-item>
        <el-descriptions-item label="成本预算">
          {{ formatAmount(form.budgetCost) }} 元
        </el-descriptions-item>
        <el-descriptions-item label="人力费用">
          {{ formatAmount(form.laborCost) }} 元
        </el-descriptions-item>
        <el-descriptions-item label="采购成本">
          {{ formatAmount(form.purchaseCost) }} 元
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 合同信息 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">合同信息</span>
      </template>
      <div v-if="contractInfo">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="合同名称" :span="3">
            {{ contractInfo.contractName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="合同状态">
            <dict-tag :options="sys_htzt" :value="contractInfo.contractStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="合同类型">
            <dict-tag :options="sys_htlx" :value="contractInfo.contractType" />
          </el-descriptions-item>
          <el-descriptions-item label="合同签订日期">
            {{ contractInfo.contractSignDate || '-' }}
          </el-descriptions-item>
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
            {{ contractInfo.contractAmount !== null && contractInfo.contractAmount !== undefined ? formatAmount(contractInfo.contractAmount) + ' 元' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="不含税金额">
            {{ contractInfo.amountNoTax !== null && contractInfo.amountNoTax !== undefined ? formatAmount(contractInfo.amountNoTax) + ' 元' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建人">
            {{ contractInfo.createByName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ contractInfo.createTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="更新人">
            {{ contractInfo.updateByName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="最后更新时间" :span="2">
            {{ contractInfo.updateTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="操作">
            <el-button v-if="checkPermi(['project:contract:query'])" type="primary" link @click="viewContract">查看详情</el-button>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else description="暂无关联合同" :image-size="80" />
    </el-card>

    <!-- 付款里程碑信息 -->
    <el-card shadow="never" class="detail-card" v-if="contractInfo && paymentList.length > 0">
      <template #header>
        <span class="card-title">付款里程碑信息</span>
      </template>
      <el-table :data="paymentListWithSummary" border>
        <el-table-column label="序号" type="index" width="60" align="center">
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
        <el-table-column label="付款金额（元）" align="center" prop="paymentAmount" width="150">
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
            <span v-if="!scope.row.isSummary">{{ scope.row.actualPaymentDate || '-' }}</span>
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

    <!-- 项目附件列表 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">项目附件列表</span>
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
        <el-table-column label="文件大小" align="center" prop="fileSize" width="120">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="文件说明" align="center" prop="fileDescription" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.fileDescription || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="上传人" align="center" prop="createByName" width="100" />
        <el-table-column label="上传时间" align="center" prop="createTime" width="180" />
      </el-table>
    </el-card>

    <!-- 其他信息 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">其他信息</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="备注" :span="2">
          <div class="text-content">{{ form.remark || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="创建人">
          {{ getUserNickName(form.createBy) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ form.createTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="更新人">
          {{ getUserNickName(form.updateBy || form.createBy) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ form.updateTime || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 底部操作按钮 -->
    <div class="form-footer">
      <el-button size="large" @click="goBack">返回</el-button>
    </div>
  </div>
</template>

<script setup name="ProjectDetail">
import { ref, reactive, toRefs, computed, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject, getContractByProjectId, getDeptTree, getUsersByPost } from '@/api/project/project'
import { listPayment } from '@/api/project/payment'
import { listAttachment, downloadAttachment } from '@/api/project/attachment'
import { saveAs } from 'file-saver'
import request from '@/utils/request'
import { checkPermi } from "@/utils/permission"

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const { proxy } = getCurrentInstance()
const { sys_xmfl, sys_xmjd, sys_xmzt, sys_yszt, sys_spzt, industry, sys_yjqy, sys_htlx, sys_htzt, sys_fkzt, sys_ndgl, sys_wdlx, sys_jdgl, sys_qrzt } =
  proxy.useDict('sys_xmfl', 'sys_xmjd', 'sys_xmzt', 'sys_yszt', 'sys_spzt', 'industry', 'sys_yjqy', 'sys_htlx', 'sys_htzt', 'sys_fkzt', 'sys_ndgl', 'sys_wdlx', 'sys_jdgl', 'sys_qrzt')

// 表单数据
const data = reactive({
  form: {
    industry: '',
    region: '',
    shortName: '',
    establishedYear: '',
    projectCode: '',
    projectName: '',
    projectCategory: '',
    projectDept: '',
    projectStage: '',
    acceptanceStatus: '',
    estimatedWorkload: '',
    actualWorkload: '',
    projectPlan: '',
    projectDescription: '',
    projectAddress: '',
    projectManagerId: '',
    projectManagerName: '',
    marketManagerId: '',
    marketManagerName: '',
    participants: [],
    salesManagerId: '',
    salesManagerName: '',
    salesContact: '',
    customerId: '',
    customerName: '',
    customerContactId: '',
    customerContactName: '',
    merchantContact: '',
    merchantPhone: '',
    startDate: '',
    endDate: '',
    productionDate: '',
    acceptanceDate: '',
    projectCost: '',
    projectBudget: '',
    costBudget: '',
    budgetCost: '',
    laborCost: '',
    purchaseCost: '',
    remark: '',
    approvalStatus: '0',
    approvalReason: ''
  }
})

const { form } = toRefs(data)

// 其他数据
const allUsers = ref([])
const selectedParticipants = ref([])
const customerContactPhone = ref('')
const contractInfo = ref(null)
const paymentList = ref([])
const attachmentList = ref([])
const deptFlatList = ref([])

// 付款里程碑列表（带合计行）
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

// 加载所有用户（用于参与人员显示）
function loadAllUsers() {
  getUsersByPost().then(response => {
    allUsers.value = response.data || []
  })
}

// 根据登录名获取昵称
function getUserNickName(loginName) {
  if (!loginName) return '-'
  const user = allUsers.value.find(u => u.userName === loginName)
  return user ? user.nickName : loginName
}

// 返回列表
function goBack() {
  if (route.query.from === 'contract' && route.query.contractId) {
    router.push(`/htkx/contract/detail/${route.query.contractId}`)
  } else {
    router.push('/project/list')
  }
}

// 查看合同详情
function viewContract() {
  if (contractInfo.value && contractInfo.value.contractId) {
    router.push(`/htkx/contract/detail/${contractInfo.value.contractId}`)
  }
}

// 加载项目数据
function loadProjectData() {
  const projectId = route.params.projectId
  if (!projectId) {
    proxy.$modal.msgError('缺少项目ID参数')
    router.push('/project/list')
    return
  }

  loading.value = true
  getProject(projectId).then(response => {
    const data = response.data
    // 转换参与人员格式：字符串 → 数组
    if (data.participants) {
      data.participants = data.participants.split(',').map(Number)
    }
    // 填充表单
    Object.assign(form.value, data)

    // 加载关联的名称数据
    loadRelatedNames(data)

    // 如果有客户ID，加载联系人列表以获取联系方式
    if (data.customerId && data.customerContactId) {
      loadCustomerContactPhone(data.customerId, data.customerContactId)
    }

    // 加载关联的合同信息
    loadContractInfo(projectId)

    // 加载项目附件列表
    loadAttachmentList(projectId)

    // 等待所有用户加载完成后，同步参与人员显示
    setTimeout(() => {
      if (form.value.participants && form.value.participants.length > 0) {
        selectedParticipants.value = allUsers.value.filter(user =>
          form.value.participants.includes(user.userId)
        )
      }
    }, 500)
  }).catch(() => {
    proxy.$modal.msgError('加载项目数据失败')
    router.push('/project/list')
  }).finally(() => {
    loading.value = false
  })
}

// 加载关联的名称数据（projectManagerName/marketManagerName/salesManagerName 由后端 JOIN 返回，无需单独请求）
function loadRelatedNames(projectData) {
  // 加载客户名称
  if (projectData.customerId) {
    request({
      url: `/project/customer/${projectData.customerId}`,
      method: 'get'
    }).then(res => {
      form.value.customerName = res.data.customerSimpleName || res.data.customerFullName || ''
    }).catch(() => {})
  }

  // 加载客户联系人名称
  if (projectData.customerId && projectData.customerContactId) {
    request({
      url: '/project/customer/contact/listByCustomer',
      method: 'get',
      params: { customerId: projectData.customerId }
    }).then(res => {
      const contact = res.data.find(c => c.contactId === projectData.customerContactId)
      if (contact) {
        form.value.customerContactName = contact.contactName || ''
      }
    }).catch(() => {})
  }
}

// 加载客户联系人电话
function loadCustomerContactPhone(customerId, contactId) {
  request({
    url: '/project/customer/contact/listByCustomer',
    method: 'get',
    params: { customerId: customerId }
  }).then(response => {
    if (response.data && response.data.length > 0) {
      // 从联系人列表中找到对应的联系人
      const contact = response.data.find(c => c.contactId === contactId)
      if (contact) {
        customerContactPhone.value = contact.contactPhone || ''
      }
    }
  }).catch(() => {
    // 如果加载失败，不影响页面显示
    customerContactPhone.value = ''
  })
}

// 加载合同信息
function loadContractInfo(projectId) {
  getContractByProjectId(projectId).then(response => {
    contractInfo.value = response.data
    // 如果有合同信息，加载付款里程碑
    if (contractInfo.value && contractInfo.value.contractId) {
      getPaymentList(contractInfo.value.contractId)
    }
  }).catch(() => {
    // 如果加载失败或没有关联合同，不影响页面显示
    contractInfo.value = null
  })
}

// 查询付款里程碑列表
function getPaymentList(contractId) {
  listPayment({ contractId: contractId }).then(response => {
    paymentList.value = response.rows || []
  })
}

// 加载部门树（用于部门名称显示）
function loadDeptTree() {
  getDeptTree().then(response => {
    deptFlatList.value = response.data
  })
}

// 根据部门ID获取部门名称（显示第三级及以下完整路径，用 - 隔开）
function getDeptName(deptId) {
  if (!deptId) return '-'
  const numDeptId = typeof deptId === 'string' ? parseInt(deptId) : deptId
  const dept = deptFlatList.value.find(d => d.deptId === numDeptId)
  if (!dept) return '-'
  const ancestorIds = dept.ancestors ? dept.ancestors.split(',').filter(id => id && id !== '0') : []
  const pathDepts = []
  if (ancestorIds.length >= 2) {
    for (let i = 2; i < ancestorIds.length; i++) {
      const ancestorDept = deptFlatList.value.find(d => d.deptId === parseInt(ancestorIds[i]))
      if (ancestorDept) pathDepts.push(ancestorDept.deptName)
    }
  }
  pathDepts.push(dept.deptName)
  return pathDepts.join('-')
}

// 格式化金额（千分位，保留两位小数）；null/undefined/空字符串/0 均显示 '-'
function formatAmount(amount) {
  if (amount === null || amount === undefined || amount === '' || amount === 0) return '-'
  const num = Number(amount)
  if (isNaN(num) || num === 0) return '-'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化文件大小
function formatFileSize(size) {
  if (!size || size === 0) return '-'
  const kb = size / 1024
  if (kb < 1024) {
    return kb.toFixed(2) + ' KB'
  }
  const mb = kb / 1024
  return mb.toFixed(2) + ' MB'
}

// 查询附件列表
function loadAttachmentList(projectId) {
  listAttachment({
    businessType: 'project',
    businessId: projectId
  }).then(response => {
    attachmentList.value = response.data || []
  })
}

// 下载附件
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

// 初始化
loadAllUsers()
loadDeptTree()
loadProjectData()
</script>

<style scoped lang="scss">
.project-detail {
  .page-header {
    margin-bottom: 20px;

    .header-content {
      h2 {
        margin: 0 0 8px 0;
        font-size: 20px;
        color: #303133;
      }

      .tips {
        margin: 0;
        font-size: 14px;
        color: #909399;
      }
    }
  }

  .detail-card {
    margin-bottom: 20px;

    .card-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .text-content {
    white-space: pre-wrap;
    word-break: break-all;
    line-height: 1.6;
  }

  // 参与人员标签
  .selected-participants {
    .participant-tag {
      margin-right: 8px;
      margin-bottom: 8px;
    }
  }

  // 底部按钮
  .form-footer {
    position: sticky;
    bottom: 0;
    padding: 20px;
    background-color: #fff;
    border-top: 1px solid #dcdfe6;
    text-align: center;
    z-index: 10;

    .el-button {
      min-width: 120px;
    }
  }
}
</style>
