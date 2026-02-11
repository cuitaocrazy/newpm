<template>
  <div class="app-container payment-detail">
    <el-form :model="form" label-width="160px" v-loading="formLoading">
      <!-- 第一部分：付款里程碑基本信息 -->
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">付款里程碑基本信息</span>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <!-- 第一行：付款里程碑名称、付款状态 -->
          <el-descriptions-item label="付款里程碑名称">
            {{ form.paymentMethodName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="付款状态">
            <dict-tag :options="sys_fkzt" :value="form.paymentStatus"/>
          </el-descriptions-item>

          <!-- 第二行：付款金额、付款里程碑确认年份 -->
          <el-descriptions-item label="付款金额">
            {{ formatMoney(form.paymentAmount) }} 元
          </el-descriptions-item>
          <el-descriptions-item label="付款里程碑确认年份">
            <dict-tag :options="sys_ndgl" :value="form.confirmYear"/>
          </el-descriptions-item>

          <!-- 第三行：是否涉及违约扣款（是-还要展示扣款金额，否-不展示扣款金额且单独展示一行） -->
          <el-descriptions-item label="是否涉及违约扣款" :span="form.hasPenalty === '1' ? 1 : 2">
            <el-tag :type="form.hasPenalty === '1' ? 'danger' : 'success'">
              {{ form.hasPenalty === '1' ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="扣款金额" v-if="form.hasPenalty === '1'">
            {{ formatMoney(form.penaltyAmount) }} 元
          </el-descriptions-item>

          <!-- 第四行：预计回款所属季度、实际回款所属季度 -->
          <el-descriptions-item label="预计回款所属季度">
            <dict-tag :options="sys_jdgl" :value="form.expectedQuarter"/>
          </el-descriptions-item>
          <el-descriptions-item label="实际回款所属季度">
            <dict-tag v-if="form.actualQuarter" :options="sys_jdgl" :value="form.actualQuarter"/>
            <span v-else>-</span>
          </el-descriptions-item>

          <!-- 第五行：开票日期、实际回款日期 -->
          <el-descriptions-item label="开票日期">
            {{ form.submitAcceptanceDate ? parseTime(form.submitAcceptanceDate, '{y}-{m}-{d}') : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="实际回款日期">
            {{ form.actualPaymentDate ? parseTime(form.actualPaymentDate, '{y}-{m}-{d}') : '-' }}
          </el-descriptions-item>

          <!-- 第六行：备注（单独一行） -->
          <el-descriptions-item label="备注" :span="2">
            {{ form.remark || '-' }}
          </el-descriptions-item>

          <!-- 第七行：创建人、创建时间 -->
          <el-descriptions-item label="创建人">
            {{ form.createBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ form.createTime ? parseTime(form.createTime) : '-' }}
          </el-descriptions-item>

          <!-- 第八行：更新人、更新时间 -->
          <el-descriptions-item label="更新人">
            {{ form.updateBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ form.updateTime ? parseTime(form.updateTime) : '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 第二部分：关联合同信息 -->
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">关联合同信息</span>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <!-- 第一行：合同名称（单独一行） -->
          <el-descriptions-item label="合同名称" :span="2">
            {{ selectedContract.contractName || '-' }}
          </el-descriptions-item>

          <!-- 第二行：所属部门、合同编号 -->
          <el-descriptions-item label="所属部门">
            {{ selectedContract.deptName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="合同编号">
            {{ selectedContract.contractCode || '-' }}
          </el-descriptions-item>

          <!-- 第三行：合同状态、免维期 -->
          <el-descriptions-item label="合同状态">
            <dict-tag v-if="selectedContract.contractStatus" :options="sys_htzt" :value="selectedContract.contractStatus"/>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="免维期（月）">
            {{ selectedContract.freeMaintenancePeriod || '-' }}
          </el-descriptions-item>

          <!-- 第四行：合同金额（含税）、合同签订日期 -->
          <el-descriptions-item label="合同金额">
            {{ formatMoney(selectedContract.contractAmount) }} 元
          </el-descriptions-item>
          <el-descriptions-item label="合同签订日期">
            {{ selectedContract.contractSignDate || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 第三部分：付款里程碑附件列表 -->
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">付款里程碑附件列表</span>
          </div>
        </template>
        <el-table
          :data="attachmentList"
          border
          v-loading="attachmentLoading"
        >
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="文档类型" prop="documentType" width="120" align="center">
            <template #default="scope">
              <dict-tag :options="sys_wdlx" :value="scope.row.documentType"/>
            </template>
          </el-table-column>
          <el-table-column label="文件名称" prop="fileName" min-width="200" show-overflow-tooltip />
          <el-table-column label="文件说明" prop="fileDescription" min-width="200" show-overflow-tooltip />
          <el-table-column label="上传人员" prop="createBy" width="120" align="center" />
          <el-table-column label="上传时间" prop="createTime" width="160" align="center">
            <template #default="scope">
              {{ parseTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="150" fixed="right">
            <template #default="scope">
              <el-button link type="primary" icon="Download" @click="handleDownload(scope.row)">下载</el-button>
            </template>
          </el-table-column>
        </el-table>
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
import { listAttachment, downloadAttachment } from '@/api/project/attachment'
import { saveAs } from 'file-saver'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()
const { sys_ndgl, sys_jdgl, sys_fkzt, sys_htzt, sys_wdlx } = proxy.useDict('sys_ndgl', 'sys_jdgl', 'sys_fkzt', 'sys_htzt', 'sys_wdlx')

// 表单数据
const formLoading = ref(false)
const attachmentLoading = ref(false)
const selectedContract = ref({})
const attachmentList = ref([])

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
  if (!route.params.paymentId) {
    proxy.$modal.msgError('付款里程碑ID不能为空')
    handleBack()
    return
  }

  formLoading.value = true
  getPayment(route.params.paymentId).then(response => {
    Object.assign(form, response.data)

    // 加载关联合同信息
    if (form.contractId) {
      getContract(form.contractId).then(contractResponse => {
        const contract = contractResponse.data
        selectedContract.value = {
          contractId: contract.contractId,
          contractName: contract.contractName,
          contractCode: contract.contractCode,
          contractAmount: contract.contractAmount,
          deptName: contract.deptName,
          contractStatus: contract.contractStatus,
          freeMaintenancePeriod: contract.freeMaintenancePeriod,
          contractSignDate: contract.contractSignDate
        }
      })
    }

    // 加载附件列表
    loadAttachmentList()

    formLoading.value = false
  }).catch(() => {
    formLoading.value = false
  })
}

/** 加载附件列表 */
function loadAttachmentList() {
  attachmentLoading.value = true
  listAttachment({
    businessType: 'payment',
    businessId: route.params.paymentId
  }).then(response => {
    attachmentList.value = response.rows || []
    attachmentLoading.value = false
  }).catch(() => {
    attachmentLoading.value = false
  })
}

/** 下载附件 */
function handleDownload(row) {
  downloadAttachment(row.attachmentId).then(response => {
    saveAs(response, row.fileName)
    proxy.$modal.msgSuccess('下载成功')
  }).catch(() => {
    proxy.$modal.msgError('下载失败')
  })
}

/** 返回 */
function handleBack() {
  router.push({ path: '/htkx/payment', query: { t: Date.now() } })
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
    width: 160px;
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
