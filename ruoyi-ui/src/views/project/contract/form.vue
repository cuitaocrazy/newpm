<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">{{ title }}</span>
          <el-button style="float: right; padding: 3px 0" type="text" icon="Back" @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-form ref="contractRef" :model="form" :rules="rules" label-width="140px">
        <!-- 第一部分：合同基本信息 -->
        <el-divider content-position="left">
          <span style="font-size: 16px; font-weight: bold; color: #409EFF;">合同基本信息</span>
        </el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="合同名称" prop="contractName">
              <el-input v-model="form.contractName" placeholder="请输入合同名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同编号" prop="contractCode">
              <el-input v-model="form.contractCode" placeholder="请输入合同编号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="部门" prop="deptId">
              <el-tree-select
                v-model="form.deptId"
                :data="filteredDeptOptions"
                :props="{ value: 'id', label: 'label', children: 'children' }"
                value-key="id"
                placeholder="请选择部门"
                clearable
                check-strictly
                style="width: 100%"
                @change="handleDeptChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联客户" prop="customerId">
              <el-select v-model="form.customerId" placeholder="请选择客户" clearable filterable style="width: 100%">
                <el-option
                  v-for="customer in customerOptions"
                  :key="customer.customerId"
                  :label="customer.customerSimpleName"
                  :value="customer.customerId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="合同类型" prop="contractType">
              <el-select v-model="form.contractType" placeholder="请选择合同类型" style="width: 100%">
                <el-option
                  v-for="dict in sys_htlx"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同状态" prop="contractStatus">
              <el-select v-model="form.contractStatus" placeholder="请选择合同状态" style="width: 100%">
                <el-option
                  v-for="dict in sys_htzt"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="关联项目" prop="projectIds">
              <el-select
                v-model="selectedProjectId"
                :placeholder="projectPlaceholder"
                clearable
                filterable
                style="width: 100%"
                :disabled="!form.deptId"
                @change="handleProjectSelect"
              >
                <el-option
                  v-for="project in availableProjectOptions"
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
            <el-form-item label=" " label-width="140px">
              <el-table :data="selectedProjects" border style="width: 100%">
                <el-table-column type="index" label="序号" width="60" align="center" />
                <el-table-column prop="projectName" label="项目名称" min-width="200" />
                <el-table-column prop="projectBudget" label="预算金额（元）" width="150" align="right">
                  <template #default="scope">
                    {{ scope.row.projectBudget ? Number(scope.row.projectBudget).toFixed(2) : '0.00' }}
                  </template>
                </el-table-column>
                <el-table-column prop="estimatedWorkload" label="预估工作量（人天）" width="160" align="right">
                  <template #default="scope">
                    {{ scope.row.estimatedWorkload ? Number(scope.row.estimatedWorkload).toFixed(2) : '0.00' }}
                  </template>
                </el-table-column>
                <el-table-column prop="actualWorkload" label="实际工作量（人天）" width="160" align="right">
                  <template #default="scope">
                    {{ scope.row.actualWorkload ? Number(scope.row.actualWorkload).toFixed(2) : '0.00' }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center">
                  <template #default="scope">
                    <el-button type="text" icon="Delete" @click="handleRemoveProject(scope.$index)">移除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第二部分：合同时间与周期 -->
        <el-divider content-position="left">
          <span style="font-size: 16px; font-weight: bold; color: #409EFF;">合同时间与周期</span>
        </el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="合同签订日期" prop="contractSignDate">
              <el-date-picker clearable
                v-model="form.contractSignDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择合同签订日期"
                style="width: 100%">
              </el-date-picker>
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

        <!-- 第三部分：合同金额 -->
        <el-divider content-position="left">
          <span style="font-size: 16px; font-weight: bold; color: #409EFF;">合同金额</span>
        </el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="合同金额(含税)" prop="contractAmount">
              <el-input v-model="form.contractAmount" placeholder="请输入合同金额(含税)" @input="calculateAmounts">
                <template #append>元</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税率" prop="taxRate">
              <el-input v-model="form.taxRate" placeholder="请输入税率" @input="calculateAmounts">
                <template #append>%</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="不含税金额" prop="amountNoTax">
              <el-input v-model="form.amountNoTax" placeholder="自动计算" :disabled="true">
                <template #append>元</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税金" prop="taxAmount">
              <el-input v-model="form.taxAmount" placeholder="自动计算" :disabled="true">
                <template #append>元</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第四部分：备注 -->
        <el-divider content-position="left">
          <span style="font-size: 16px; font-weight: bold; color: #409EFF;">备注</span>
        </el-divider>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注内容" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第五部分：审计信息（仅编辑模式显示） -->
        <template v-if="form.contractId">
          <el-divider content-position="left">
            <span style="font-size: 16px; font-weight: bold; color: #409EFF;">审计信息</span>
          </el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="创建人">
                <el-input v-model="form.createByName" :placeholder="form.createBy" :disabled="true" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="创建时间">
                <el-input :value="parseTime(form.createTime, '{y}-{m}-{d} {h}:{i}:{s}')" :disabled="true" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="更新人">
                <el-input v-model="form.updateByName" :placeholder="form.updateBy" :disabled="true" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="更新时间">
                <el-input :value="parseTime(form.updateTime, '{y}-{m}-{d} {h}:{i}:{s}')" :disabled="true" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- 操作按钮 -->
        <el-row :gutter="20">
          <el-col :span="24" style="text-align: center; margin-top: 20px;">
            <el-button type="primary" @click="submitForm" :loading="submitLoading">提交</el-button>
            <el-button @click="handleBack">取消</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>
</template>

<script setup name="ContractForm">
import { getContract, addContract, updateContract, checkContractNameUnique } from "@/api/project/contract"
import { deptTreeSelect } from "@/api/system/user"
import { listCustomer } from "@/api/project/customer"
import { listProjectByDept } from "@/api/project/project"

const { proxy } = getCurrentInstance()
const { sys_htlx, sys_htzt } = proxy.useDict('sys_htlx', 'sys_htzt')
const route = useRoute()
const router = useRouter()

const title = ref("合同新增")
const deptOptions = ref([])
const filteredDeptOptions = ref([])
const customerOptions = ref([])
const submitLoading = ref(false)
const filteredProjectOptions = ref([])
const availableProjectOptions = ref([])
const projectPlaceholder = ref("请先选择部门")
const selectedProjectId = ref(null)
const selectedProjects = ref([])

const data = reactive({
  form: {
    contractId: null,
    contractCode: null,
    contractName: null,
    customerId: null,
    deptId: null,
    contractType: null,
    contractStatus: null,
    contractSignDate: null,
    contractPeriod: null,
    contractAmount: null,
    taxRate: null,
    amountNoTax: null,
    taxAmount: null,
    confirmAmount: null,
    confirmYear: null,
    freeMaintenancePeriod: null,
    createBy: null,
    createByName: null,
    createTime: null,
    updateBy: null,
    updateByName: null,
    updateTime: null,
    remark: null,
    projectIds: []
  },
  rules: {
    contractName: [
      { required: true, message: "合同名称不能为空", trigger: "blur" },
      { validator: validateContractName, trigger: "blur" }
    ],
    deptId: [
      { required: true, message: "部门不能为空", trigger: "change" }
    ],
    customerId: [
      { required: true, message: "关联客户不能为空", trigger: "change" }
    ],
    contractType: [
      { required: true, message: "合同类型不能为空", trigger: "change" }
    ],
    contractSignDate: [
      { required: true, message: "合同签订日期不能为空", trigger: "change" }
    ],
    contractAmount: [
      { required: true, message: "合同金额(含税)不能为空", trigger: "blur" }
    ],
  }
})

const { form, rules } = toRefs(data)

/** 合同名称唯一性校验 */
async function validateContractName(rule, value, callback) {
  if (!value || value.trim() === '') {
    callback()
    return
  }

  try {
    const response = await checkContractNameUnique(value.trim(), form.value.contractId)
    if (response.data === true) {
      callback()
    } else {
      callback(new Error('合同名称已存在，请使用其他名称'))
    }
  } catch (error) {
    console.error('校验合同名称唯一性失败:', error)
    callback()
  }
}

/** 查询部门下拉树结构 */
function getDeptTree() {
  deptTreeSelect().then(response => {
    deptOptions.value = response.data
    // 过滤出第三级及以下部门
    filteredDeptOptions.value = filterDeptFromLevel3(response.data)
  })
}

/** 过滤部门树，只保留第三级及以下部门 */
function filterDeptFromLevel3(deptTree, level = 1) {
  if (!deptTree || !Array.isArray(deptTree)) return []

  const result = []
  for (const dept of deptTree) {
    if (level >= 3) {
      // 第三级及以下，保留该节点
      const newDept = { ...dept }
      if (dept.children && dept.children.length > 0) {
        newDept.children = filterDeptFromLevel3(dept.children, level + 1)
      }
      result.push(newDept)
    } else {
      // 第一、二级，只递归处理子节点
      if (dept.children && dept.children.length > 0) {
        const childrenResult = filterDeptFromLevel3(dept.children, level + 1)
        result.push(...childrenResult)
      }
    }
  }
  return result
}

/** 查询客户列表 */
function getCustomerList() {
  listCustomer().then(response => {
    customerOptions.value = response.rows
  })
}

/** 查询项目列表 */
function getProjectList() {
  // 不再在初始化时查询所有项目，而是根据部门动态查询
}

/** 部门变化时过滤项目 */
function handleDeptChange(deptId) {
  // 清空已选项目
  selectedProjects.value = []
  selectedProjectId.value = null

  if (!deptId) {
    filteredProjectOptions.value = []
    availableProjectOptions.value = []
    projectPlaceholder.value = "请先选择部门"
    return
  }

  // 根据部门查询项目（排除已被其他合同关联的项目）
  const excludeContractId = form.value.contractId || null
  listProjectByDept(deptId, excludeContractId).then(response => {
    filteredProjectOptions.value = response.data || []

    // 更新可选项目列表
    updateAvailableProjects()

    // 更新提示文本
    if (filteredProjectOptions.value.length === 0) {
      projectPlaceholder.value = "该部门暂无可关联的项目"
    } else {
      projectPlaceholder.value = "请选择关联项目"
    }
  }).catch(error => {
    console.error('查询项目列表失败:', error)
    filteredProjectOptions.value = []
    availableProjectOptions.value = []
    projectPlaceholder.value = "查询项目失败"
  })
}

/** 更新可选项目列表（排除已选项目） */
function updateAvailableProjects() {
  const selectedIds = selectedProjects.value.map(p => p.projectId)
  availableProjectOptions.value = filteredProjectOptions.value.filter(project => {
    return !selectedIds.includes(project.projectId)
  })
}

/** 项目选择变化时自动添加到列表 */
function handleProjectSelect(projectId) {
  if (!projectId) return

  const project = filteredProjectOptions.value.find(p => p.projectId === projectId)
  if (project) {
    selectedProjects.value.push({ ...project })
    // 清空选择，允许继续选择其他项目
    selectedProjectId.value = null
    updateAvailableProjects()
  }
}

/** 从已选列表移除项目 */
function handleRemoveProject(index) {
  selectedProjects.value.splice(index, 1)
  updateAvailableProjects()
}

// 计算不含税金额和税金
function calculateAmounts() {
  const amount = parseFloat(form.value.contractAmount)
  const rate = parseFloat(form.value.taxRate)

  if (!isNaN(amount) && !isNaN(rate) && rate >= 0) {
    // 不含税金额 = 合同金额 / (1 + 税率/100)
    const amountNoTax = amount / (1 + rate / 100)
    form.value.amountNoTax = amountNoTax.toFixed(2)

    // 税金 = 合同金额 - 不含税金额
    const taxAmount = amount - amountNoTax
    form.value.taxAmount = taxAmount.toFixed(2)
  } else if (!isNaN(amount) && (isNaN(rate) || rate === 0)) {
    // 如果没有税率或税率为0，不含税金额等于合同金额
    form.value.amountNoTax = amount.toFixed(2)
    form.value.taxAmount = '0.00'
  } else {
    form.value.amountNoTax = null
    form.value.taxAmount = null
  }
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["contractRef"].validate(valid => {
    if (valid) {
      submitLoading.value = true
      // 将已选项目ID列表赋值给form
      form.value.projectIds = selectedProjects.value.map(p => p.projectId)

      if (form.value.contractId != null) {
        updateContract(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          router.back()
        }).finally(() => {
          submitLoading.value = false
        })
      } else {
        addContract(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          router.back()
        }).finally(() => {
          submitLoading.value = false
        })
      }
    }
  })
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

  // 如果有contractId参数，则是编辑模式
  const contractId = route.params.contractId
  if (contractId) {
    title.value = "合同编辑"
    // 动态修改路由的 meta.title 以更新面包屑
    if (route.meta) {
      route.meta.title = "合同编辑"
    }
    getContract(contractId).then(response => {
      form.value = response.data
      // 编辑模式下，根据已选部门过滤项目
      if (form.value.deptId) {
        // 延迟执行，确保部门数据已加载
        setTimeout(() => {
          // 查询该部门的项目（包含当前合同已关联的项目）
          listProjectByDept(form.value.deptId, contractId).then(projectResponse => {
            filteredProjectOptions.value = projectResponse.data || []

            // 恢复已选项目列表
            const projectIds = response.data.projectIds || []
            selectedProjects.value = filteredProjectOptions.value.filter(project => {
              return projectIds.includes(project.projectId)
            })

            // 更新可选项目列表
            updateAvailableProjects()

            // 更新提示文本
            if (filteredProjectOptions.value.length === 0) {
              projectPlaceholder.value = "该部门暂无可关联的项目"
            } else {
              projectPlaceholder.value = "请选择关联项目"
            }
          }).catch(error => {
            console.error('查询项目列表失败:', error)
          })
        }, 100)
      }
    })
  } else {
    // 新增模式
    title.value = "合同新增"
    if (route.meta) {
      route.meta.title = "合同新增"
    }
    // 重置表单数据
    resetForm()
  }
}

/** 重置表单数据 */
function resetForm() {
  form.value = {
    contractId: null,
    contractCode: null,
    contractName: null,
    customerId: null,
    deptId: null,
    contractType: null,
    contractStatus: null,
    contractSignDate: null,
    contractPeriod: null,
    contractAmount: null,
    taxRate: null,
    amountNoTax: null,
    taxAmount: null,
    confirmAmount: null,
    confirmYear: null,
    freeMaintenancePeriod: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null,
    projectIds: []
  }
  filteredProjectOptions.value = []
  availableProjectOptions.value = []
  selectedProjects.value = []
  selectedProjectId.value = null
  projectPlaceholder.value = "请先选择部门"
}

// 监听路由参数变化，当 contractId 变化时重新加载数据
watch(() => route.params.contractId, (newId, oldId) => {
  // 只有当 contractId 真正变化时才重新加载
  if (newId !== oldId) {
    init()
  }
})

// 组件激活时（从缓存中恢复）重新加载数据
onActivated(() => {
  init()
})

init()
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
