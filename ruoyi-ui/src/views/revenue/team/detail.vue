<template>
  <div class="app-container">
    <el-page-header @back="goBack" title="返回" content="团队收入确认详情" />

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 左侧：项目详情（55%） -->
      <el-col :span="13">
        <!-- 项目基本信息 -->
        <el-card shadow="never" class="detail-card">
          <template #header>
            <span class="card-title">项目基本信息</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="行业">
              <dict-tag :options="industry" :value="form.industry" />
            </el-descriptions-item>
            <el-descriptions-item label="一级区域">
              <dict-tag :options="sys_yjqy" :value="form.region" />
            </el-descriptions-item>
            <el-descriptions-item label="简称">{{ form.shortName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="二级区域">{{ form.regionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="立项年份">{{ form.establishedYear || '-' }} 年</el-descriptions-item>
            <el-descriptions-item label="项目编号">{{ form.projectCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目名称" :span="2">{{ form.projectName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目分类">
              <dict-tag :options="sys_xmfl" :value="form.projectCategory" />
            </el-descriptions-item>
            <el-descriptions-item label="项目部门">{{ deptPath || '-' }}</el-descriptions-item>
            <el-descriptions-item label="预估工作量">{{ form.estimatedWorkload || 0 }} 人天</el-descriptions-item>
            <el-descriptions-item label="项目预算">{{ form.projectBudget || 0 }} 元</el-descriptions-item>
            <el-descriptions-item label="实际工作量">{{ form.actualWorkload || 0 }} 人天</el-descriptions-item>
            <el-descriptions-item label="项目阶段">
              <dict-tag :options="sys_xmjd" :value="form.projectStage" />
            </el-descriptions-item>
            <el-descriptions-item label="验收状态">
              <dict-tag :options="sys_yszt" :value="form.acceptanceStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="审核状态">
              <dict-tag :options="sys_spzt" :value="form.approvalStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="项目地址" :span="2">{{ form.projectAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目计划" :span="2">
              <div class="text-content">{{ form.projectPlan || '-' }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="2">
              <div class="text-content">{{ form.projectDescription || '-' }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="审核意见" :span="2">{{ form.approvalReason || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 人员配置 -->
        <el-card shadow="never" class="detail-card" style="margin-top: 15px;">
          <template #header>
            <span class="card-title">人员配置</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目经理">{{ form.projectManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="市场经理">{{ form.marketManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="销售负责人">{{ form.salesManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="销售联系方式">{{ form.salesContact || '-' }}</el-descriptions-item>
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
        <el-card shadow="never" class="detail-card" style="margin-top: 15px;">
          <template #header>
            <span class="card-title">客户信息</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="客户名称">{{ form.customerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户联系人">{{ form.customerContactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户联系方式">{{ customerContactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商户联系人">{{ form.merchantContact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商户联系方式" :span="2">{{ form.merchantPhone || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 时间规划 -->
        <el-card shadow="never" class="detail-card" style="margin-top: 15px;">
          <template #header>
            <span class="card-title">时间规划</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="启动日期">{{ parseTime(form.startDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="结束日期">{{ parseTime(form.endDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="投产日期">{{ parseTime(form.productionDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="验收日期">{{ parseTime(form.acceptanceDate, '{y}-{m}-{d}') }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 成本预算 -->
        <el-card shadow="never" class="detail-card" style="margin-top: 15px;">
          <template #header>
            <span class="card-title">成本预算</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目费用">{{ form.projectCost || 0 }} 元</el-descriptions-item>
            <el-descriptions-item label="费用预算">{{ form.costBudget || 0 }} 元</el-descriptions-item>
            <el-descriptions-item label="成本预算">{{ form.budgetCost || 0 }} 元</el-descriptions-item>
            <el-descriptions-item label="人力费用">{{ form.laborCost || 0 }} 元</el-descriptions-item>
            <el-descriptions-item label="采购成本" :span="2">{{ form.purchaseCost || 0 }} 元</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 备注 -->
        <el-card shadow="never" class="detail-card" style="margin-top: 15px;">
          <template #header>
            <span class="card-title">备注</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="备注">
              <div class="text-content">{{ form.remark || '-' }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧：收入确认信息（45%） -->
      <el-col :span="11">
        <!-- 公司收入确认信息 -->
        <el-card shadow="never" class="detail-card">
          <template #header>
            <span class="card-title">公司收入确认信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="收入确认状态">
              <dict-tag :options="sys_srqrzt" :value="form.revenueConfirmStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="收入确认年度">{{ form.revenueConfirmYear }}</el-descriptions-item>
            <el-descriptions-item label="合同金额(含税)">{{ form.contractAmount }} 元</el-descriptions-item>
            <el-descriptions-item label="确认金额(含税)">{{ form.confirmAmount }} 元</el-descriptions-item>
            <el-descriptions-item label="税率">{{ form.taxRate }}%</el-descriptions-item>
            <el-descriptions-item label="税后金额">{{ form.afterTaxAmount }} 元</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 团队收入确认明细 -->
        <el-card shadow="never" class="detail-card" style="margin-top: 15px;">
          <template #header>
            <span class="card-title">团队收入确认明细</span>
          </template>

          <el-table :data="detailList" border style="width: 100%;" empty-text="暂无团队确认明细">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column label="部门" prop="deptName" min-width="120" show-overflow-tooltip />
            <el-table-column label="确认金额(元)" prop="confirmAmount" width="120" align="right">
              <template #default="scope">
                {{ scope.row.confirmAmount ? scope.row.confirmAmount.toFixed(2) : '0.00' }}
              </template>
            </el-table-column>
            <el-table-column label="确认时间" prop="confirmTime" width="160" align="center">
              <template #default="scope">
                {{ parseTime(scope.row.confirmTime, '{y}-{m}-{d} {h}:{i}') }}
              </template>
            </el-table-column>
            <el-table-column label="确认人" prop="confirmUserName" width="100" align="center" />
            <el-table-column label="备注" prop="remark" min-width="150" show-overflow-tooltip />
          </el-table>

          <!-- 合计行 -->
          <div v-if="detailList && detailList.length > 0" style="margin-top: 10px; text-align: right; font-size: 14px; font-weight: bold;">
            团队确认金额合计：{{ totalTeamAmount }} 元
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
import { listUser } from '@/api/system/user'
import { getDeptTree } from '@/api/project/project'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()

// 字典
const { proxy } = getCurrentInstance()
const { industry, sys_yjqy, sys_xmfl, sys_xmjd, sys_spzt, sys_yszt, sys_srqrzt } = proxy.useDict(
  'industry', 'sys_yjqy', 'sys_xmfl', 'sys_xmjd', 'sys_spzt', 'sys_yszt', 'sys_srqrzt'
)

// 数据
const form = ref({})
const detailList = ref([])
const allUsers = ref([])
const selectedParticipants = ref([])
const customerContactPhone = ref('')
const deptPath = ref('')

// 计算团队确认金额合计
const totalTeamAmount = computed(() => {
  if (!detailList.value || detailList.value.length === 0) {
    return '0.00'
  }
  const total = detailList.value.reduce((sum, item) => {
    return sum + (parseFloat(item.confirmAmount) || 0)
  }, 0)
  return total.toFixed(2)
})

/** 返回 */
function goBack() {
  router.back()
}

/** 加载所有用户（用于参与人员显示） */
function loadAllUsers() {
  listUser({}).then(response => {
    allUsers.value = response.rows || []
  })
}

/** 加载客户联系人电话 */
function loadCustomerContactPhone(customerId, contactId) {
  if (!customerId || !contactId) return

  request({
    url: '/project/customer/contact/listByCustomer',
    method: 'get',
    params: { customerId: customerId }
  }).then(response => {
    if (response.data && response.data.length > 0) {
      const contact = response.data.find(c => c.contactId === contactId)
      if (contact) {
        customerContactPhone.value = contact.contactPhone || ''
      }
    }
  }).catch(() => {
    customerContactPhone.value = ''
  })
}

/** 构建部门路径（三级及以下机构，用-隔开） */
function buildDeptPath(deptId) {
  if (!deptId) {
    deptPath.value = ''
    return
  }

  getDeptTree().then(response => {
    const allDepts = response.data || []

    // 找到当前部门
    const currentDept = allDepts.find(d => d.deptId === deptId)
    if (!currentDept) {
      deptPath.value = ''
      return
    }

    // 解析祖先路径（格式：0,100,101,102）
    const ancestors = currentDept.ancestors || ''
    const ancestorIds = ancestors.split(',').filter(id => id && id !== '0')

    // 只保留三级及以下的部门（ancestors包含的逗号数>=2，即level>=3）
    // ancestors格式："0,100,101" -> level=3（祖先路径中有2个逗号）
    const level = ancestorIds.length + 1 // 当前部门层级

    if (level < 3) {
      // 不是三级及以下部门，不显示
      deptPath.value = ''
      return
    }

    // 构建从三级开始的完整路径
    const pathParts = []

    // 找出所有相关部门
    ancestorIds.forEach(ancestorId => {
      const dept = allDepts.find(d => d.deptId === parseInt(ancestorId))
      if (dept) {
        // 计算该祖先部门的层级
        const ancestorLevel = dept.ancestors ? dept.ancestors.split(',').filter(id => id && id !== '0').length + 1 : 1
        if (ancestorLevel >= 3) {
          pathParts.push(dept.deptName)
        }
      }
    })

    // 添加当前部门
    pathParts.push(currentDept.deptName)

    deptPath.value = pathParts.join('-')
  }).catch(() => {
    deptPath.value = ''
  })
}

/** 获取详情 */
function getDetail() {
  const projectId = route.params.projectId
  getTeamRevenue(projectId).then(response => {
    const projectData = response.data.project || {}

    // 转换参与人员格式：字符串 → 数组
    if (projectData.participants) {
      projectData.participants = projectData.participants.split(',').map(Number)
    }

    form.value = projectData
    detailList.value = response.data.detailList || []

    // 加载客户联系人电话
    if (projectData.customerId && projectData.customerContactId) {
      loadCustomerContactPhone(projectData.customerId, projectData.customerContactId)
    }

    // 构建部门路径
    if (projectData.projectDept) {
      buildDeptPath(parseInt(projectData.projectDept))
    }

    // 等待所有用户加载完成后，同步参与人员显示
    setTimeout(() => {
      if (form.value.participants && form.value.participants.length > 0) {
        selectedParticipants.value = allUsers.value.filter(user =>
          form.value.participants.includes(user.userId)
        )
      }
    }, 500)
  })
}

// 初始化
onMounted(() => {
  loadAllUsers()
  getDetail()
})

// 当组件被激活时（从缓存中恢复），重新加载数据
// 这样可以确保从公司收入确认页面返回时能看到最新数据
onActivated(() => {
  // 重新加载详情数据，获取最新的公司收入确认信息
  getDetail()
})

// 监听路由参数变化，当项目ID变化时重新加载数据
watch(
  () => route.params.projectId,
  (newProjectId, oldProjectId) => {
    if (newProjectId && newProjectId !== oldProjectId) {
      getDetail()
    }
  }
)
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.detail-card {
  margin-bottom: 0;

  :deep(.el-card__header) {
    padding: 12px 20px;
    background-color: #f5f7fa;
  }

  .card-title {
    font-size: 14px;
    font-weight: bold;
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

:deep(.el-descriptions) {
  .el-descriptions__label {
    width: 140px;
    background-color: #fafafa;
    font-weight: 500;
  }

  .el-descriptions__content {
    color: #606266;
  }
}

:deep(.el-table) {
  font-size: 13px;

  .el-table__header {
    th {
      background-color: #fafafa;
      color: #606266;
      font-weight: 500;
    }
  }
}
</style>
