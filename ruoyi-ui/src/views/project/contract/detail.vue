<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">合同详情</span>
          <el-button style="float: right; padding: 3px 0" type="text" icon="Back" @click="handleBack">返回</el-button>
        </div>
      </template>

      <!-- 第一部分：合同基本信息 -->
      <el-divider content-position="left">
        <span style="font-size: 16px; font-weight: bold; color: #409EFF;">合同基本信息</span>
      </el-divider>
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
        <el-descriptions-item label="合同状态" label-class-name="label-bold">
          <dict-tag :options="sys_htzt" :value="detailData.contractStatus"/>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 关联项目列表 -->
      <div style="margin-top: 20px;" v-if="relatedProjects.length > 0">
        <div style="margin-bottom: 10px; font-weight: bold; color: #606266;">关联项目</div>
        <el-table :data="relatedProjects" border style="width: 100%">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="项目名称" min-width="200">
            <template #default="scope">
              <el-link type="primary" @click="handleViewProject(scope.row.projectId)">
                {{ scope.row.projectName }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="预计金额(元)" width="150" align="right">
            <template #default="scope">
              {{ formatAmount(scope.row.projectBudget) }}
            </template>
          </el-table-column>
          <el-table-column label="预计人天(人/天)" width="150" align="center">
            <template #default="scope">
              {{ scope.row.estimatedWorkload || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="实际人天(人/天)" width="150" align="center">
            <template #default="scope">
              {{ scope.row.actualWorkload || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div style="margin-top: 20px; color: #909399;" v-else>
        <div style="margin-bottom: 10px; font-weight: bold; color: #606266;">关联项目</div>
        <div style="padding: 20px; text-align: center; background-color: #f5f7fa; border: 1px solid #e4e7ed; border-radius: 4px;">
          暂无关联项目
        </div>
      </div>

      <!-- 第二部分：合同时间与周期 -->
      <el-divider content-position="left">
        <span style="font-size: 16px; font-weight: bold; color: #409EFF;">合同时间与周期</span>
      </el-divider>
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
      </el-descriptions>

      <!-- 第三部分：合同金额 -->
      <el-divider content-position="left">
        <span style="font-size: 16px; font-weight: bold; color: #409EFF;">合同金额</span>
      </el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="合同金额(含税)" label-class-name="label-bold">
          <span style="color: #409EFF; font-weight: bold; font-size: 16px;">{{ formatAmount(detailData.contractAmount) }} 元</span>
        </el-descriptions-item>
        <el-descriptions-item label="税率" label-class-name="label-bold">
          {{ detailData.taxRate }}%
        </el-descriptions-item>
        <el-descriptions-item label="不含税金额" label-class-name="label-bold">
          <span style="color: #67C23A; font-weight: bold; font-size: 16px;">{{ formatAmount(detailData.amountNoTax) }} 元</span>
        </el-descriptions-item>
        <el-descriptions-item label="税金" label-class-name="label-bold">
          {{ formatAmount(detailData.taxAmount) }} 元
        </el-descriptions-item>
        <el-descriptions-item label="合同确认金额" label-class-name="label-bold">
          <span style="color: #E6A23C; font-weight: bold; font-size: 16px;">{{ formatAmount(detailData.confirmAmount) }} 元</span>
        </el-descriptions-item>
        <el-descriptions-item label="确认年份" label-class-name="label-bold">
          <dict-tag :options="sys_ndgl" :value="detailData.confirmYear"/>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 第四部分：其他信息 -->
      <el-divider content-position="left">
        <span style="font-size: 16px; font-weight: bold; color: #409EFF;">其他信息</span>
      </el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="创建人" label-class-name="label-bold">
          {{ detailData.createBy }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" label-class-name="label-bold">
          {{ parseTime(detailData.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
        </el-descriptions-item>
        <el-descriptions-item label="更新人" label-class-name="label-bold">
          {{ detailData.updateBy }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间" label-class-name="label-bold">
          {{ parseTime(detailData.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2" label-class-name="label-bold">
          {{ detailData.remark || '无' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 操作按钮 -->
      <el-row :gutter="20">
        <el-col :span="24" style="text-align: center; margin-top: 20px;">
          <el-button @click="handleBack">返回</el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup name="ContractDetail">
import { getContract } from "@/api/project/contract"
import { deptTreeSelect } from "@/api/system/user"
import { listCustomer } from "@/api/project/customer"
import { listProject } from "@/api/project/project"

const { proxy } = getCurrentInstance()
const { sys_htlx, sys_htzt, sys_ndgl } = proxy.useDict('sys_htlx', 'sys_htzt', 'sys_ndgl')
const route = useRoute()
const router = useRouter()

const detailData = ref({})
const deptOptions = ref([])
const customerOptions = ref([])
const projectOptions = ref([])

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

/** 计算关联项目列表 */
const relatedProjects = computed(() => {
  if (!detailData.value.projectIds || detailData.value.projectIds.length === 0) {
    return []
  }
  return projectOptions.value.filter(project =>
    detailData.value.projectIds.includes(project.projectId)
  )
})

/** 跳转到项目详情 */
function handleViewProject(projectId) {
  router.push({ path: '/project/project/detail', query: { projectId: projectId } })
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

/** 初始化数据 */
function init() {
  getDeptTree()
  getCustomerList()
  getProjectList()

  const contractId = route.query.contractId
  if (contractId) {
    getContract(contractId).then(response => {
      detailData.value = response.data
    })
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
