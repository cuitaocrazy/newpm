<template>
  <div class="app-container payment-detail">
    <el-form :model="form" label-width="140px" v-loading="formLoading">
      <!-- 第一部分：关联合同 -->
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">关联合同</span>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="合同名称" :span="2">
            {{ selectedContract.contractName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="合同编号">
            {{ selectedContract.contractCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="合同金额（元）">
            {{ formatMoney(selectedContract.contractAmount) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 第二部分：付款信息 -->
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">付款信息</span>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="付款状态">
            <dict-tag :options="sys_fkzt" :value="form.paymentStatus"/>
          </el-descriptions-item>
          <el-descriptions-item label="付款金额（元）">
            {{ formatMoney(form.paymentAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="是否涉及违约扣款">
            <el-tag :type="form.hasPenalty === '1' ? 'danger' : 'success'">
              {{ form.hasPenalty === '1' ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="扣款金额（元）">
            {{ form.hasPenalty === '1' ? formatMoney(form.penaltyAmount) : '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 第三部分：回款信息 -->
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">回款信息</span>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="里程碑名称" :span="2">
            {{ form.paymentMethodName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="款项确认年份">
            <dict-tag :options="sys_ndgl" :value="form.confirmYear"/>
          </el-descriptions-item>
          <el-descriptions-item label="预计回款季度">
            <dict-tag :options="sys_jdgl" :value="form.expectedQuarter"/>
          </el-descriptions-item>
          <el-descriptions-item label="实际回款季度">
            <dict-tag v-if="form.actualQuarter" :options="sys_jdgl" :value="form.actualQuarter"/>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交验收日期">
            {{ form.submitAcceptanceDate ? parseTime(form.submitAcceptanceDate, '{y}-{m}-{d}') : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="实际回款日期" :span="2">
            {{ form.actualPaymentDate ? parseTime(form.actualPaymentDate, '{y}-{m}-{d}') : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            {{ form.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 第四部分：系统信息 -->
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">系统信息</span>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="款项主键ID">
            {{ form.paymentId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建人">
            {{ form.createBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ form.createTime ? parseTime(form.createTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="更新人">
            {{ form.updateBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间" :span="2">
            {{ form.updateTime ? parseTime(form.updateTime) : '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </el-form>

    <!-- 底部操作按钮 -->
    <div class="form-footer">
      <el-button size="large" @click="handleBack">返回</el-button>
    </div>
  </div>
</template>

<script setup name="PaymentDetail">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPayment } from '@/api/project/payment'
import { getContract } from '@/api/project/contract'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()
const { sys_ndgl, sys_jdgl, sys_fkzt } = proxy.useDict('sys_ndgl', 'sys_jdgl', 'sys_fkzt')

// 表单数据
const formLoading = ref(false)
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
  remark: '',
  createBy: '',
  createTime: '',
  updateBy: '',
  updateTime: ''
})

/** 金额格式化（千分位） */
function formatMoney(amount) {
  if (amount === null || amount === undefined || amount === '') {
    return '0.00'
  }
  const num = Number(amount).toFixed(2)
  return num.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/** 加载付款里程碑详情 */
function loadPaymentDetail() {
  if (!route.params.id) {
    proxy.$modal.msgError('付款里程碑ID不能为空')
    handleBack()
    return
  }

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
      })
    }

    formLoading.value = false
  }).catch(() => {
    formLoading.value = false
  })
}

/** 返回 */
function handleBack() {
  router.push({ path: '/project/payment', query: { t: Date.now() } })
}

// 初始化
onMounted(() => {
  loadPaymentDetail()
})
</script>

<style scoped lang="scss">
.payment-detail {
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
