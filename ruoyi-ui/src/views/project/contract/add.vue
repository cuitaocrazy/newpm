<template>
  <div class="app-container contract-add">
    <!-- 表单主体 -->
    <el-form ref="addFormRef" :model="form" :rules="rules" label-width="140px" v-loading="loading">
      <el-collapse v-model="activeNames" class="contract-collapse">

        <!-- 面板1：合同基本信息 -->
        <el-collapse-item name="1" title="合同基本信息">
          <!-- 从项目添加时，显示项目信息提示 -->
          <el-alert
            v-if="projectInfo"
            type="info"
            :closable="false"
            style="margin-bottom: 16px"
          >
            <template #title>
              将为项目创建合同：{{ projectInfo.projectName }} ({{ projectInfo.projectCode }})
            </template>
          </el-alert>

          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="合同名称" prop="contractName">
                <el-input v-model="form.contractName" placeholder="请输入合同名称" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="合同编号" prop="contractCode">
                <el-input v-model="form.contractCode" placeholder="请输入合同编号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="部门" prop="deptId">
                <el-tree-select
                  v-model="form.deptId"
                  :data="deptOptions"
                  :props="{ value: 'id', label: 'label', children: 'children' }"
                  value-key="id"
                  placeholder="请选择部门"
                  check-strictly
                  clearable
                  @change="handleDeptChange"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="关联客户" prop="customerId">
                <el-select
                  v-model="form.customerId"
                  placeholder="请选择客户"
                  filterable
                  clearable
                  style="width: 100%"
                >
                  <el-option
                    v-for="customer in customerOptions"
                    :key="customer.customerId"
                    :label="customer.customerSimpleName"
                    :value="customer.customerId"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合同类型" prop="contractType">
                <el-select v-model="form.contractType" placeholder="请选择合同类型" clearable style="width: 100%">
                  <el-option v-for="dict in sys_htlx" :key="dict.value"
                    :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="合同状态" prop="contractStatus">
                <el-select v-model="form.contractStatus" placeholder="请选择合同状态" clearable style="width: 100%">
                  <el-option v-for="dict in sys_htzt" :key="dict.value"
                    :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 关联项目选择 -->
          <el-row :gutter="20" v-if="!projectInfo">
            <el-col :span="24">
              <el-form-item label="关联项目" prop="projectIds">
                <el-select
                  v-model="form.projectIds"
                  placeholder="请先选择部门，然后选择项目（可多选）"
                  multiple
                  filterable
                  clearable
                  collapse-tags
                  collapse-tags-tooltip
                  :max-collapse-tags="3"
                  :disabled="!form.deptId"
                  @change="handleProjectChange"
                  style="width: 100%"
                >
                  <el-option
                    v-for="project in projectOptions"
                    :key="project.projectId"
                    :label="project.projectName"
                    :value="project.projectId"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 已选项目列表 -->
          <el-row :gutter="20" v-if="selectedProjects.length > 0">
            <el-col :span="24">
              <el-form-item label="已选项目">
                <el-table :data="selectedProjects" border style="width: 100%">
                  <el-table-column type="index" label="序号" width="60" />
                  <el-table-column prop="projectName" label="项目名称" />
                  <el-table-column prop="projectBudget" label="预算金额（元）" width="150">
                    <template #default="scope">
                      {{ scope.row.projectBudget || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="estimatedWorkload" label="预估工作量（人天）" width="180">
                    <template #default="scope">
                      {{ scope.row.estimatedWorkload || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="80" v-if="!projectInfo">
                    <template #default="scope">
                      <el-button link type="danger" @click="removeProject(scope.row.projectId)">
                        删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板2：合同时间与周期 -->
        <el-collapse-item name="2" title="合同时间与周期">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="合同签订日期" prop="contractSignDate">
                <el-date-picker
                  v-model="form.contractSignDate"
                  type="date"
                  placeholder="请选择合同签订日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合同周期" prop="contractPeriod">
                <el-input v-model="form.contractPeriod" placeholder="请输入合同周期">
                  <template #append>月</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="免维期" prop="freeMaintenancePeriod">
                <el-input v-model="form.freeMaintenancePeriod" placeholder="请输入免维期">
                  <template #append>月</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板3：合同金额 -->
        <el-collapse-item name="3" title="合同金额">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="合同金额（含税）" prop="contractAmount">
                <el-input
                  v-model="form.contractAmount"
                  placeholder="请输入合同金额"
                  @input="calculateTax"
                >
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="税率" prop="taxRate">
                <el-input
                  v-model="form.taxRate"
                  placeholder="请输入税率"
                  @input="calculateTax"
                >
                  <template #append>%</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="不含税金额">
                <el-input
                  v-model="form.amountNoTax"
                  placeholder="自动计算"
                  readonly
                  disabled
                  style="background-color: #f5f7fa;"
                >
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="税金">
                <el-input
                  v-model="form.taxAmount"
                  placeholder="自动计算"
                  readonly
                  disabled
                  style="background-color: #f5f7fa;"
                >
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板4：备注 -->
        <el-collapse-item name="4" title="备注">
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入备注信息"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

      </el-collapse>
    </el-form>

    <!-- 底部操作按钮 -->
    <div class="form-footer">
      <el-button type="primary" size="large" @click="submitForm" :loading="submitLoading">
        保存
      </el-button>
      <el-button size="large" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script setup name="ContractAdd">
import { ref, reactive, toRefs, watch, getCurrentInstance, onMounted, onActivated } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { addContract } from '@/api/project/contract'
import { listProjectByDept, getProject, getDeptTree as fetchDeptTree } from '@/api/project/project'
import { listAllCustomer } from '@/api/project/customer'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const { proxy } = getCurrentInstance()
const { sys_htlx, sys_htzt } = proxy.useDict('sys_htlx', 'sys_htzt')

// 从项目列表进入时的项目信息
const projectInfo = ref(null)

// 折叠面板激活状态
const activeNames = ref(['1', '2', '3', '4'])

// 表单数据
const data = reactive({
  form: {
    contractCode: '',
    contractName: '',
    customerId: '',
    deptId: '',
    contractType: '',
    contractStatus: '',
    contractSignDate: '',
    contractPeriod: '',
    contractAmount: '',
    taxRate: '',
    amountNoTax: '',
    taxAmount: '',
    freeMaintenancePeriod: '',
    projectIds: [],
    remark: ''
  },
  rules: {
    contractName: [
      { required: true, message: '请输入合同名称', trigger: 'blur' }
    ],
    deptId: [
      { required: true, message: '请选择部门', trigger: 'change' }
    ],
    customerId: [
      { required: true, message: '请选择关联客户', trigger: 'change' }
    ],
    contractType: [
      { required: true, message: '请选择合同类型', trigger: 'change' }
    ],
    contractAmount: [
      { required: true, message: '请输入合同金额', trigger: 'blur' },
      { pattern: /^\d+(\.\d+)?$/, message: '请输入有效的数字', trigger: 'blur' }
    ]
  }
})

const { form, rules } = toRefs(data)

// 其他数据
const submitLoading = ref(false)
const deptOptions = ref([])
const customerOptions = ref([])
const projectOptions = ref([])
const selectedProjects = ref([])

// 重置表单
function resetForm() {
  form.value = {
    contractCode: '',
    contractName: '',
    customerId: '',
    deptId: '',
    contractType: '',
    contractStatus: '',
    contractSignDate: '',
    contractPeriod: '',
    contractAmount: '',
    taxRate: '',
    amountNoTax: '',
    taxAmount: '',
    freeMaintenancePeriod: '',
    projectIds: [],
    remark: ''
  }
  projectOptions.value = []
  selectedProjects.value = []
  // 清除表单验证
  if (proxy.$refs['addFormRef']) {
    proxy.$refs['addFormRef'].clearValidate()
  }
}

// 加载部门树
function getDeptTree() {
  fetchDeptTree().then(response => {
    const deptData = response.data.map((dept) => ({
      ...dept,
      id: dept.deptId,
      label: dept.deptName
    }))
    deptOptions.value = proxy.handleTree(deptData, "id")
  })
}

// 加载客户列表
function getCustomerList() {
  listAllCustomer().then(response => {
    customerOptions.value = response.data || []
  })
}

// 部门变化时加载项目列表
function handleDeptChange(deptId) {
  // 清空已选项目
  form.value.projectIds = []
  selectedProjects.value = []
  projectOptions.value = []

  if (deptId) {
    loadProjectsByDept(deptId)
  }
}

// 根据部门加载项目列表（排除已被其他合同关联的项目）
function loadProjectsByDept(deptId) {
  listProjectByDept(deptId, null).then(response => {  // 新增时传null，排除所有已关联的项目
    projectOptions.value = response.data || []
  })
}

// 项目选择变化
function handleProjectChange(projectIds) {
  selectedProjects.value = projectOptions.value.filter(project =>
    projectIds.includes(project.projectId)
  )
}

// 删除已选项目
function removeProject(projectId) {
  form.value.projectIds = form.value.projectIds.filter(id => id !== projectId)
  selectedProjects.value = selectedProjects.value.filter(p => p.projectId !== projectId)
}

// 计算税金
function calculateTax() {
  const contractAmount = parseFloat(form.value.contractAmount)
  const taxRate = parseFloat(form.value.taxRate)

  if (contractAmount && taxRate >= 0) {
    // 不含税金额 = 合同金额 / (1 + 税率/100)
    const amountNoTax = contractAmount / (1 + taxRate / 100)
    // 税金 = 合同金额 - 不含税金额
    const taxAmount = contractAmount - amountNoTax

    form.value.amountNoTax = amountNoTax.toFixed(2)
    form.value.taxAmount = taxAmount.toFixed(2)
  } else {
    form.value.amountNoTax = ''
    form.value.taxAmount = ''
  }
}

// 提交表单
function submitForm() {
  proxy.$refs['addFormRef'].validate(valid => {
    if (valid) {
      proxy.$modal.confirm('确认保存合同信息？').then(() => {
        submitLoading.value = true
        addContract(form.value).then(response => {
          proxy.$modal.msgSuccess('新增成功')
          // 跳转到合同管理页面并刷新
          router.push({ path: '/htkx/contract', query: { t: Date.now() } })
        }).finally(() => {
          submitLoading.value = false
        })
      })
    } else {
      proxy.$modal.msgError('请完善必填信息')
    }
  })
}

// 取消
function cancel() {
  proxy.$modal.confirm('确认取消新增？未保存的数据将丢失').then(() => {
    // 跳转到合同管理页面并刷新
    router.push({ path: '/htkx/contract', query: { t: Date.now() } })
  })
}

/** 加载项目信息（从项目列表进入时） */
async function loadProjectInfo(projectId) {
  try {
    console.log('🔍 开始加载项目信息, projectId:', projectId)
    const response = await getProject(projectId)
    console.log('📦 项目数据返回:', response.data)

    if (!response.data) {
      proxy.$modal.msgWarning('项目不存在')
      router.push('/htkx/contract')
      return
    }
    projectInfo.value = response.data
    console.log('✅ projectInfo已设置:', projectInfo.value)

    // 自动填充项目ID（单个项目）
    form.value.projectIds = [Number(projectId)]
    console.log('✅ form.projectIds已设置:', form.value.projectIds)

    // 设置已选项目显示（用于提示用户已选择的项目）
    selectedProjects.value = [{
      projectId: response.data.projectId,
      projectName: response.data.projectName,
      projectBudget: response.data.projectBudget,
      estimatedWorkload: response.data.estimatedWorkload
    }]
    console.log('✅ selectedProjects已设置:', selectedProjects.value)

    // 自动填充部门信息
    if (response.data.deptId) {
      form.value.deptId = response.data.deptId
      console.log('✅ form.deptId已设置:', form.value.deptId)
    } else {
      console.warn('⚠️  项目数据中没有deptId')
    }

    // 自动填充客户信息
    if (response.data.customerId) {
      form.value.customerId = response.data.customerId
      console.log('✅ form.customerId已设置:', form.value.customerId)
    } else {
      console.warn('⚠️  项目数据中没有customerId')
    }

    console.log('🎉 loadProjectInfo完成, form最终状态:', form.value)
  } catch (error) {
    proxy.$modal.msgError('加载项目信息失败')
    console.error('❌ 加载项目信息失败:', error)
  }
}

// 页面加载时初始化
onMounted(() => {
  console.log('🎬 ========== ContractAdd onMounted 开始 ==========')
  console.log('📍 当前路由:', route.path)
  console.log('📋 query参数:', route.query)
  console.log('🔑 projectId:', route.query.projectId)

  getDeptTree()
  getCustomerList()
  resetForm()

  // 检查是否从项目列表进入（带projectId参数）
  const projectId = route.query.projectId
  if (projectId) {
    console.log('🚀 onMounted: 检测到projectId, 开始加载项目信息...')
    loadProjectInfo(projectId)
  } else {
    console.log('⚠️  onMounted: 未检测到projectId参数')
  }

  console.log('🎬 ========== ContractAdd onMounted 结束 ==========')
})

// 组件激活时处理（从其他页面返回时）
onActivated(() => {
  console.log('🔄 onActivated: 组件被激活')

  // 检查是否有projectId参数
  const projectId = route.query.projectId
  if (projectId) {
    // 如果有projectId，说明是从项目列表进入，重新加载项目信息
    console.log('🚀 onActivated: 检测到projectId, 重新加载项目信息...')
    loadProjectInfo(projectId)
  } else {
    // 如果没有projectId，说明是正常的合同新增页面，重置表单
    console.log('🔄 onActivated: 无projectId, 重置表单')
    resetForm()
  }
})
</script>

<style scoped lang="scss">
.contract-add {
  .contract-collapse {
    margin-bottom: 20px;

    :deep(.el-collapse-item__header) {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      padding-left: 10px;
      border-left: 3px solid #409eff;
    }

    :deep(.el-collapse-item__content) {
      padding: 20px 15px;
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
