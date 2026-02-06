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
              <el-autocomplete
                v-model="contractSearchText"
                :fetch-suggestions="searchContract"
                placeholder="请输入合同名称或编号搜索"
                clearable
                @select="handleContractSelect"
                :disabled="isEdit"
                style="width: 100%"
              >
                <template #default="{ item }">
                  <div>{{ item.label }}</div>
                </template>
              </el-autocomplete>
              <div v-if="selectedContract.contractId" class="contract-info">
                <el-descriptions :column="2" border size="small" style="margin-top: 10px">
                  <el-descriptions-item label="合同名称" :span="2">
                    {{ selectedContract.contractName }}
                  </el-descriptions-item>
                  <el-descriptions-item label="合同编号">
                    {{ selectedContract.contractCode }}
                  </el-descriptions-item>
                  <el-descriptions-item label="合同金额（元）">
                    {{ formatMoney(selectedContract.contractAmount) }}
                  </el-descriptions-item>
                </el-descriptions>
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
              <el-select v-model="form.paymentStatus" placeholder="请选择付款状态" style="width: 100%">
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
              <el-select v-model="form.confirmYear" placeholder="请选择款项确认年份" style="width: 100%">
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
              <el-select v-model="form.expectedQuarter" placeholder="请选择预计回款季度" style="width: 100%">
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
      <el-button type="primary" size="large" @click="handleSubmit" :loading="submitLoading">保存</el-button>
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
const { sys_ndgl, sys_jdgl, sys_fkzt } = proxy.useDict('sys_ndgl', 'sys_jdgl', 'sys_fkzt')

// 表单数据
const paymentFormRef = ref(null)
const formLoading = ref(false)
const submitLoading = ref(false)
const contractSearchText = ref('')
const selectedContract = ref({})

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
  return !!route.params.id
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
function searchContract(queryString, cb) {
  if (!queryString) {
    cb([])
    return
  }
  searchContracts({ keyword: queryString }).then(response => {
    const results = response.data.map(item => ({
      value: item.contractName,
      contractId: item.contractId,
      contractCode: item.contractCode,
      contractName: item.contractName,
      contractAmount: item.contractAmount,
      label: `${item.contractName} (${item.contractCode})`
    }))
    cb(results)
  })
}

/** 合同选择 */
function handleContractSelect(item) {
  form.contractId = item.contractId
  contractSearchText.value = item.label
  selectedContract.value = {
    contractId: item.contractId,
    contractName: item.contractName,
    contractCode: item.contractCode,
    contractAmount: item.contractAmount
  }
}

/** 违约扣款变化 */
function handlePenaltyChange(value) {
  if (value === '0') {
    form.penaltyAmount = 0.00
  }
}

/** 加载付款里程碑详情 */
function loadPaymentDetail() {
  if (!route.params.id) return

  formLoading.value = true
  getPayment(route.params.id).then(response => {
    Object.assign(form, response.data)

    // 加载关联合同信息
    if (form.contractId) {
      getContract(form.contractId).then(contractResponse => {
        const contract = contractResponse.data
        selectedContract.value = {
          contractId: contract.contractId,
          contractName: contract.contractName,
          contractCode: contract.contractCode,
          contractAmount: contract.contractAmount
        }
        contractSearchText.value = `${contract.contractName} (${contract.contractCode})`
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
    router.push({ path: '/project/payment', query: { t: Date.now() } })
  }).finally(() => {
    submitLoading.value = false
  })
}

/** 取消操作 */
function handleCancel() {
  router.push({ path: '/project/payment', query: { t: Date.now() } })
}

// 初始化
onMounted(() => {
  if (isEdit.value) {
    loadPaymentDetail()
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

  .contract-info {
    width: 100%;
  }

  // 描述列表样式
  :deep(.el-descriptions__label) {
    width: 120px;
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
