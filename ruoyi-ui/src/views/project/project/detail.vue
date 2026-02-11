<template>
  <div class="app-container project-detail">
    <!-- 页面标题 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <h2>项目详情</h2>
        <p class="tips">查看项目完整信息</p>
      </div>
    </el-card>

    <!-- 项目基本信息 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">项目基本信息</span>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="行业">
          <dict-tag :options="industry" :value="form.industry" />
        </el-descriptions-item>
        <el-descriptions-item label="区域">
          <dict-tag :options="sys_yjqy" :value="form.region" />
        </el-descriptions-item>
        <el-descriptions-item label="简称">
          {{ form.shortName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="立项年份">
          {{ form.establishedYear || '-' }} 年
        </el-descriptions-item>
        <el-descriptions-item label="项目编号" :span="2">
          {{ form.projectCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="项目名称" :span="3">
          {{ form.projectName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="项目分类">
          <dict-tag :options="sys_xmfl" :value="form.projectCategory" />
        </el-descriptions-item>
        <el-descriptions-item label="项目部门">
          {{ form.projectDept || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="预估工作量">
          {{ form.estimatedWorkload || 0 }} 人天
        </el-descriptions-item>
        <el-descriptions-item label="项目预算">
          {{ form.projectBudget || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="实际工作量">
          {{ form.actualWorkload || 0 }} 人天
        </el-descriptions-item>
        <el-descriptions-item label="项目阶段">
          <dict-tag :options="sys_xmjd" :value="form.projectStage" />
        </el-descriptions-item>
        <el-descriptions-item label="验收状态">
          <dict-tag :options="sys_yszt" :value="form.acceptanceStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <dict-tag :options="sys_spzt" :value="form.approvalStatus" />
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

    <!-- 人员配置 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">人员配置</span>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="项目经理">
          {{ form.projectManagerName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="市场经理">
          {{ form.marketManagerName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="销售负责人">
          {{ form.salesManagerName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="销售联系方式" :span="3">
          {{ form.salesContact || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="参与人员" :span="3">
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
      <el-descriptions :column="3" border>
        <el-descriptions-item label="客户名称">
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
        <el-descriptions-item label="商户联系方式" :span="2">
          {{ form.merchantPhone || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 时间规划 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">时间规划</span>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="启动日期">
          {{ form.startDate || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="结束日期">
          {{ form.endDate || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="投产日期">
          {{ form.productionDate || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="验收日期" :span="3">
          {{ form.acceptanceDate || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 成本预算 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">成本预算</span>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="项目预算">
          {{ form.projectBudget || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="项目费用">
          {{ form.projectCost || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="费用预算">
          {{ form.costBudget || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="成本预算">
          {{ form.budgetCost || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="人力费用">
          {{ form.laborCost || 0 }} 元
        </el-descriptions-item>
        <el-descriptions-item label="采购成本">
          {{ form.purchaseCost || 0 }} 元
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 备注 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <span class="card-title">备注</span>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="备注">
          <div class="text-content">{{ form.remark || '-' }}</div>
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
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject } from '@/api/project/project'
import { listUser } from '@/api/system/user'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const { proxy } = getCurrentInstance()
const { sys_xmfl, sys_xmjd, sys_yszt, sys_spzt, industry, sys_yjqy } =
  proxy.useDict('sys_xmfl', 'sys_xmjd', 'sys_yszt', 'sys_spzt', 'industry', 'sys_yjqy')

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

// 加载所有用户（用于参与人员显示）
function loadAllUsers() {
  listUser({}).then(response => {
    allUsers.value = response.rows || []
  })
}

// 返回列表
function goBack() {
  router.push('/project/list')
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

// 加载关联的名称数据
function loadRelatedNames(projectData) {
  // 加载项目经理名称
  if (projectData.projectManagerId) {
    request({
      url: `/system/user/${projectData.projectManagerId}`,
      method: 'get'
    }).then(res => {
      form.value.projectManagerName = res.data.nickName || ''
    }).catch(() => {})
  }

  // 加载市场经理名称
  if (projectData.marketManagerId) {
    request({
      url: `/system/user/${projectData.marketManagerId}`,
      method: 'get'
    }).then(res => {
      form.value.marketManagerName = res.data.nickName || ''
    }).catch(() => {})
  }

  // 加载销售负责人名称
  if (projectData.salesManagerId) {
    request({
      url: `/system/user/${projectData.salesManagerId}`,
      method: 'get'
    }).then(res => {
      form.value.salesManagerName = res.data.nickName || ''
    }).catch(() => {})
  }

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

// 初始化
loadAllUsers()
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
