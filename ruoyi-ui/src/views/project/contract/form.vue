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
                :data="deptOptions"
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
                v-model="form.projectIds"
                :placeholder="projectPlaceholder"
                multiple
                clearable
                filterable
                style="width: 100%"
                :disabled="!form.deptId"
              >
                <el-option
                  v-for="project in filteredProjectOptions"
                  :key="project.projectId"
                  :label="project.projectName"
                  :value="project.projectId"
                />
              </el-select>
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
import { getContract, addContract, updateContract } from "@/api/project/contract"
import { deptTreeSelect } from "@/api/system/user"
import { listCustomer } from "@/api/project/customer"
import { listProject } from "@/api/project/project"

const { proxy } = getCurrentInstance()
const { sys_htlx, sys_htzt } = proxy.useDict('sys_htlx', 'sys_htzt')
const route = useRoute()
const router = useRouter()

const title = ref("合同新增")
const deptOptions = ref([])
const customerOptions = ref([])
const projectOptions = ref([])
const allProjectOptions = ref([])
const submitLoading = ref(false)
const filteredProjectOptions = ref([])
const projectPlaceholder = ref("请先选择部门")

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
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null,
    projectIds: []
  },
  rules: {
    contractName: [
      { required: true, message: "合同名称不能为空", trigger: "blur" }
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
    freeMaintenancePeriod: [
      { required: true, message: "免维期不能为空", trigger: "blur" }
    ],
    contractAmount: [
      { required: true, message: "合同金额(含税)不能为空", trigger: "blur" }
    ],
  }
})

const { form, rules } = toRefs(data)

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
    allProjectOptions.value = response.rows
    projectOptions.value = response.rows
  })
}

/** 获取部门及其所有子部门的ID列表 */
function getAllDeptIds(deptId) {
  const deptIds = [deptId]

  // 递归获取所有子部门ID
  function getChildDeptIds(depts, parentId) {
    if (!depts || !Array.isArray(depts)) return
    for (const dept of depts) {
      if (dept.id === parentId && dept.children && dept.children.length > 0) {
        for (const child of dept.children) {
          deptIds.push(child.id)
          getChildDeptIds(dept.children, child.id)
        }
      } else if (dept.children && dept.children.length > 0) {
        getChildDeptIds(dept.children, parentId)
      }
    }
  }

  getChildDeptIds(deptOptions.value, deptId)
  return deptIds
}

/** 部门变化时过滤项目 */
function handleDeptChange(deptId) {
  // 清空已选项目
  form.value.projectIds = []

  if (!deptId) {
    filteredProjectOptions.value = []
    projectPlaceholder.value = "请先选择部门"
    return
  }

  // 获取当前部门及所有子部门的ID
  const deptIds = getAllDeptIds(deptId)

  // 过滤项目：项目的部门ID在部门ID列表中
  filteredProjectOptions.value = allProjectOptions.value.filter(project => {
    return project.projectDept && deptIds.includes(parseInt(project.projectDept))
  })

  // 更新提示文本
  if (filteredProjectOptions.value.length === 0) {
    projectPlaceholder.value = "该部门暂无项目"
  } else {
    projectPlaceholder.value = "请选择关联项目"
  }
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
  const contractId = route.query.contractId
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
        // 延迟执行，确保项目列表已加载
        setTimeout(() => {
          handleDeptChange(form.value.deptId)
          // 恢复已选项目（因为handleDeptChange会清空）
          form.value.projectIds = response.data.projectIds || []
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
  projectPlaceholder.value = "请先选择部门"
}

// 监听路由参数变化，当 contractId 变化时重新加载数据
watch(() => route.query.contractId, (newId, oldId) => {
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
