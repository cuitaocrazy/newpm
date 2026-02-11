<template>
  <div class="app-container payment-form">
    <el-form ref="paymentFormRef" :model="form" :rules="rules" label-width="140px" v-loading="formLoading">
      <!-- 第一部分：关联合同 -->
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">关联合同</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="关联合同" prop="contractId">
              <el-select
                v-model="form.contractId"
                filterable
                remote
                reserve-keyword
                placeholder="请输入合同名称或编号搜索"
                :remote-method="searchContract"
                :loading="contractLoading"
                clearable
                @change="handleContractChange"
                @focus="handleContractFocus"
                :disabled="isEdit || isPresetContract"
                style="width: 100%"
              >
                <el-option
                  v-for="item in contractOptions"
                  :key="item.contractId"
                  :label="item.label"
                  :value="item.contractId"
                >
                  <span>{{ item.label }}</span>
                </el-option>
              </el-select>

              <!-- 合同详细信息 -->
              <div v-if="selectedContract.contractId" class="contract-detail">
                <el-divider content-position="left">
                  <span style="font-weight: 600; color: #303133;">合同基本信息</span>
                </el-divider>
                <el-descriptions :column="3" border size="small">
                  <el-descriptions-item label="合同名称" :span="3">
                    {{ selectedContract.contractName || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="合同编号">
                    {{ selectedContract.contractCode || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="免维期（月）">
                    {{ selectedContract.freeMaintenancePeriod || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="部门名称">
                    {{ selectedContract.deptName || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="客户名称">
                    {{ selectedContract.customerName || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="合同类型">
                    <dict-tag v-if="selectedContract.contractType" :options="sys_htlx" :value="selectedContract.contractType" />
                    <span v-else>-</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="合同状态">
                    <dict-tag v-if="selectedContract.contractStatus" :options="sys_htzt" :value="selectedContract.contractStatus" />
                    <span v-else>-</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="合同金额（含税）">
                    {{ formatMoney(selectedContract.contractAmount) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="合同确认金额">
                    {{ formatMoney(selectedContract.confirmAmount) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="确认年份">
                    {{ selectedContract.confirmYear || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="签订日期">
                    {{ selectedContract.contractSignDate || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="合同周期（月）">
                    {{ selectedContract.contractPeriod || '-' }}
                  </el-descriptions-item>
                </el-descriptions>

                <!-- 已付款里程碑列表 -->
                <el-divider content-position="left" style="margin-top: 20px;">
                  <span style="font-weight: 600; color: #303133;">已付款里程碑</span>
                </el-divider>
                <el-table
                  :data="paymentMilestones"
                  border
                  size="small"
                  style="width: 100%"
                  max-height="300"
                  v-loading="milestonesLoading"
                >
                  <el-table-column type="index" label="序号" width="60" align="center" />
                  <el-table-column label="里程碑名称" prop="paymentMethodName" min-width="150" show-overflow-tooltip />
                  <el-table-column label="付款金额（元）" prop="paymentAmount" width="120" align="right">
                    <template #default="scope">
                      {{ formatMoney(scope.row.paymentAmount) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="付款状态" prop="paymentStatus" width="100" align="center">
                    <template #default="scope">
                      <dict-tag :options="sys_fkzt" :value="scope.row.paymentStatus" />
                    </template>
                  </el-table-column>
                  <el-table-column label="预计回款季度" prop="expectedQuarterName" width="120" align="center" />
                  <el-table-column label="开票日期" prop="submitAcceptanceDate" width="110" align="center" />
                  <el-table-column label="实际回款季度" prop="actualQuarterName" width="120" align="center" />
                  <el-table-column label="实际回款日期" prop="actualPaymentDate" width="110" align="center" />
                  <el-table-column label="确认年份" prop="confirmYear" width="100" align="center" />
                  <el-table-column label="是否违约扣款" prop="hasPenalty" width="120" align="center">
                    <template #default="scope">
                      {{ scope.row.hasPenalty === '1' ? '是' : '否' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="扣款金额（元）" prop="penaltyAmount" width="120" align="right">
                    <template #default="scope">
                      {{ scope.row.hasPenalty === '1' ? formatMoney(scope.row.penaltyAmount) : '-' }}
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 第二部分：付款信息 -->
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">付款信息</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="付款状态" prop="paymentStatus">
              <el-select v-model="form.paymentStatus" placeholder="请选择付款状态" clearable style="width: 100%">
                <el-option
                  v-for="dict in sys_fkzt"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款金额（元）" prop="paymentAmount">
              <el-input-number
                v-model="form.paymentAmount"
                :precision="2"
                :step="0.01"
                :min="0"
                :max="999999999.99"
                controls-position="right"
                placeholder="请输入付款金额"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="是否涉及违约扣款" prop="hasPenalty">
              <el-radio-group v-model="form.hasPenalty" @change="handlePenaltyChange">
                <el-radio label="0">否</el-radio>
                <el-radio label="1">是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.hasPenalty === '1'">
            <el-form-item label="扣款金额（元）" prop="penaltyAmount">
              <el-input-number
                v-model="form.penaltyAmount"
                :precision="2"
                :step="0.01"
                :min="0"
                :max="999999999.99"
                controls-position="right"
                placeholder="请输入扣款金额"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 第三部分：回款信息 -->
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">回款信息</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="里程碑名称" prop="paymentMethodName">
              <el-input
                v-model="form.paymentMethodName"
                placeholder="请输入里程碑名称"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="款项确认年份" prop="confirmYear">
              <el-select v-model="form.confirmYear" placeholder="请选择款项确认年份" clearable style="width: 100%">
                <el-option
                  v-for="dict in sys_ndgl"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预计回款季度" prop="expectedQuarter">
              <el-select v-model="form.expectedQuarter" placeholder="请选择预计回款季度" clearable style="width: 100%">
                <el-option
                  v-for="dict in sys_jdgl"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际回款季度" prop="actualQuarter">
              <el-select v-model="form.actualQuarter" placeholder="请选择实际回款季度" clearable style="width: 100%">
                <el-option
                  v-for="dict in sys_jdgl"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="提交验收日期" prop="submitAcceptanceDate">
              <el-date-picker
                v-model="form.submitAcceptanceDate"
                type="date"
                placeholder="请选择提交验收日期"
                value-format="YYYY-MM-DD"
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际回款日期" prop="actualPaymentDate">
              <el-date-picker
                v-model="form.actualPaymentDate"
                type="date"
                placeholder="请选择实际回款日期"
                value-format="YYYY-MM-DD"
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="3"
                placeholder="请输入备注"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>
    </el-form>

    <!-- 底部操作按钮 -->
    <div class="form-footer">
      <el-button type="primary" size="large" @click="handleSubmit" :loading="submitLoading">提交</el-button>
      <el-button size="large" @click="handleCancel">取消</el-button>
    </div>
  </div>
</template>

<script setup name="PaymentForm">
import { ref, reactive, onMounted, getCurrentInstance, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPayment, addPayment, updatePayment } from '@/api/project/payment'
import { searchContracts, getContract } from '@/api/project/contract'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()
const { sys_ndgl, sys_jdgl, sys_fkzt, sys_htlx, sys_htzt } = proxy.useDict('sys_ndgl', 'sys_jdgl', 'sys_fkzt', 'sys_htlx', 'sys_htzt')

// 表单数据
const paymentFormRef = ref(null)
const formLoading = ref(false)
const submitLoading = ref(false)
const contractLoading = ref(false)
const milestonesLoading = ref(false)
const contractOptions = ref([])
const selectedContract = ref({})
const paymentMilestones = ref([])

const form = reactive({
  paymentId: null,
  contractId: null,
  paymentMethodName: '',
  paymentAmount: null,
  hasPenalty: '0',
  penaltyAmount: 0.00,
  paymentStatus: '',
  expectedQuarter: '',
  actualQuarter: '',
  submitAcceptanceDate: '',
  actualPaymentDate: '',
  confirmYear: '',
  remark: ''
})

// 表单验证规则
const rules = {
  contractId: [
    { required: true, message: '请选择关联合同', trigger: 'change' }
  ],
  paymentMethodName: [
    { required: true, message: '请输入里程碑名称', trigger: 'blur' }
  ],
  paymentAmount: [
    { required: true, message: '请输入付款金额', trigger: 'blur' }
  ],
  hasPenalty: [
    { required: true, message: '请选择是否涉及违约扣款', trigger: 'change' }
  ],
  penaltyAmount: [
    { required: true, message: '请输入扣款金额', trigger: 'blur' }
  ],
  paymentStatus: [
    { required: true, message: '请选择付款状态', trigger: 'change' }
  ]
}

// 是否编辑模式
const isEdit = computed(() => {
  return !!route.params.paymentId
})

// 是否预设合同（从列表页跳转过来）
const isPresetContract = computed(() => {
  return !!route.query.contractId
})

// 页面标题
const pageTitle = computed(() => {
  return isEdit.value ? '编辑付款里程碑' : '新增付款里程碑'
})

/** 金额格式化（千分位） */
function formatMoney(amount) {
  if (amount === null || amount === undefined || amount === '') {
    return '0.00'
  }
  const num = Number(amount).toFixed(2)
  return num.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/** 合同搜索 */
function searchContract(queryString) {
  contractLoading.value = true
  const keyword = queryString || ''
  searchContracts({ keyword: keyword }).then(response => {
    contractOptions.value = response.data.map(item => {
      // 格式：合同编号-合同名称，如果合同编号为空则只显示合同名称
      const displayLabel = item.contractCode
        ? `${item.contractCode}-${item.contractName}`
        : item.contractName

      return {
        contractId: item.contractId,
        contractCode: item.contractCode,
        contractName: item.contractName,
        contractAmount: item.contractAmount,
        label: displayLabel
      }
    })
    contractLoading.value = false
  }).catch(() => {
    contractLoading.value = false
  })
}

/** 合同下拉框获得焦点 */
function handleContractFocus() {
  // 如果选项列表为空，加载初始数据
  if (contractOptions.value.length === 0) {
    searchContract('')
  }
}

/** 合同选择变化 */
function handleContractChange(contractId) {
  if (!contractId) {
    selectedContract.value = {}
    paymentMilestones.value = []
    return
  }

  // 加载合同详细信息
  loadContractDetail(contractId)

  // 加载已付款里程碑
  loadPaymentMilestones(contractId)
}

/** 加载合同详细信息 */
function loadContractDetail(contractId) {
  getContract(contractId).then(response => {
    const contract = response.data

    selectedContract.value = {
      contractId: contract.contractId,
      contractName: contract.contractName,
      contractCode: contract.contractCode,
      contractAmount: contract.contractAmount,
      freeMaintenancePeriod: contract.freeMaintenancePeriod,
      deptName: contract.deptName,
      customerName: contract.customerName,
      contractType: contract.contractType,
      contractStatus: contract.contractStatus,
      confirmAmount: contract.confirmAmount,
      confirmYear: contract.confirmYear,
      contractSignDate: contract.contractSignDate,
      contractPeriod: contract.contractPeriod
    }
  })
}

/** 加载已付款里程碑 */
function loadPaymentMilestones(contractId) {
  milestonesLoading.value = true
  import('@/api/project/payment').then(module => {
    module.listPayment({ contractId: contractId, pageNum: 1, pageSize: 1000 }).then(response => {
      const list = response.rows || []

      // 处理季度字典标签（表格中直接显示文本）
      paymentMilestones.value = list.map(item => {
        return {
          ...item,
          expectedQuarterName: getDictLabel(sys_jdgl.value, item.expectedQuarter),
          actualQuarterName: getDictLabel(sys_jdgl.value, item.actualQuarter)
        }
      })

      milestonesLoading.value = false
    }).catch(() => {
      milestonesLoading.value = false
    })
  })
}

/** 获取字典标签 */
function getDictLabel(dictData, dictValue) {
  if (!dictData || !dictValue) return '-'
  const dict = dictData.find(item => item.value === dictValue)
  return dict ? dict.label : dictValue
}

/** 违约扣款变化 */
function handlePenaltyChange(value) {
  if (value === '0') {
    form.penaltyAmount = 0.00
  }
}

/** 加载付款里程碑详情 */
function loadPaymentDetail() {
  if (!route.params.paymentId) return

  formLoading.value = true
  getPayment(route.params.paymentId).then(response => {
    Object.assign(form, response.data)

    // 加载关联合同信息
    if (form.contractId) {
      // 加载合同详细信息
      loadContractDetail(form.contractId)

      // 加载已付款里程碑
      loadPaymentMilestones(form.contractId)

      // 加载合同选项（用于下拉框显示）
      getContract(form.contractId).then(contractResponse => {
        const contract = contractResponse.data
        // 格式：合同编号-合同名称，如果合同编号为空则只显示合同名称
        const displayLabel = contract.contractCode
          ? `${contract.contractCode}-${contract.contractName}`
          : contract.contractName

        // 将当前合同添加到选项列表中，以便下拉框显示
        contractOptions.value = [{
          contractId: contract.contractId,
          contractCode: contract.contractCode,
          contractName: contract.contractName,
          contractAmount: contract.contractAmount,
          label: displayLabel
        }]
      })
    }

    formLoading.value = false
  }).catch(() => {
    formLoading.value = false
  })
}

/** 提交表单 */
function handleSubmit() {
  paymentFormRef.value.validate(valid => {
    if (valid) {
      // 编辑模式下二次确认
      if (isEdit.value) {
        proxy.$modal.confirm('是否确认编辑该付款里程碑？').then(() => {
          submitForm()
        }).catch(() => {})
      } else {
        submitForm()
      }
    }
  })
}

/** 提交表单数据 */
function submitForm() {
  submitLoading.value = true

  const submitData = { ...form }

  const apiCall = isEdit.value ? updatePayment(submitData) : addPayment(submitData)

  apiCall.then(response => {
    proxy.$modal.msgSuccess(isEdit.value ? '修改成功' : '新增成功')
    // 跳转到付款里程碑查询页面并触发查询
    router.push({ path: '/htkx/payment', query: { t: Date.now() } })
  }).finally(() => {
    submitLoading.value = false
  })
}

/** 重置表单 */
function handleReset() {
  proxy.$modal.confirm('确认重置已经填写好的付款里程碑信息吗？重置后将清空填写的所有信息！').then(() => {
    // 重置表单数据
    Object.assign(form, {
      paymentId: null,
      contractId: null,
      paymentMethodName: '',
      paymentAmount: null,
      hasPenalty: '0',
      penaltyAmount: 0.00,
      paymentStatus: '',
      expectedQuarter: '',
      actualQuarter: '',
      submitAcceptanceDate: '',
      actualPaymentDate: '',
      confirmYear: '',
      remark: ''
    })

    // 清空合同相关数据
    selectedContract.value = {}
    paymentMilestones.value = []
    contractOptions.value = []

    // 清空表单验证
    paymentFormRef.value.clearValidate()

    proxy.$modal.msgSuccess('重置成功')
  }).catch(() => {})
}

/** 取消操作 */
function handleCancel() {
  router.push({ path: '/htkx/payment', query: { t: Date.now() } })
}

// 初始化
onMounted(() => {
  if (isEdit.value) {
    loadPaymentDetail()
  } else if (isPresetContract.value) {
    // 预设合同模式：从 URL 参数获取合同信息
    const contractId = parseInt(route.query.contractId)
    const contractName = route.query.contractName

    if (contractId) {
      form.contractId = contractId

      // 设置合同选项（用于下拉框显示）
      contractOptions.value = [{
        contractId: contractId,
        contractName: contractName,
        label: contractName
      }]

      // 加载合同详细信息和已有付款里程碑
      loadContractDetail(contractId)
      loadPaymentMilestones(contractId)
    }
  }
})
</script>

<style scoped lang="scss">
.payment-form {
  .form-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        padding-left: 10px;
        border-left: 3px solid #409eff;
      }
    }
  }

  .contract-detail {
    width: 100%;
    margin-top: 15px;

    .el-divider {
      margin: 15px 0;
    }

    .el-table {
      margin-top: 10px;
    }
  }

  // 描述列表样式
  :deep(.el-descriptions__label) {
    width: 140px;
    background-color: #fafafa;
    font-weight: 500;
  }

  :deep(.el-descriptions__content) {
    color: #303133;
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
