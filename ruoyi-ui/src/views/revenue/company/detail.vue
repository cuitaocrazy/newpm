<template>
  <div class="app-container">
    <el-page-header @back="goBack" :title="pageTitle" />

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 左侧：项目详情（60%） -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: bold;">项目详情</span>
          </template>

          <!-- 项目基本信息 -->
          <div class="section-title">项目基本信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="行业"><dict-tag :options="industry" :value="form.industry"/></el-descriptions-item>
            <el-descriptions-item label="一级区域">{{ getDictLabel(sys_yjqy, form.region) }}</el-descriptions-item>
            <el-descriptions-item label="二级区域">{{ form.regionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="立项年度"><dict-tag :options="sys_ndgl" :value="form.establishedYear"/></el-descriptions-item>
            <el-descriptions-item label="项目编号">{{ form.projectCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目预算">{{ formatAmount(form.projectBudget) }} 元</el-descriptions-item>
            <el-descriptions-item label="项目名称" :span="2">{{ form.projectName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目分类">{{ getDictLabel(sys_xmfl, form.projectCategory) }}</el-descriptions-item>
            <el-descriptions-item label="项目部门">{{ form.deptName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="预估工作量">{{ form.estimatedWorkload != null ? form.estimatedWorkload + ' 人天' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="实际人天">{{ form.actualWorkload != null ? form.actualWorkload + ' 人天' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目状态">{{ getDictLabel(sys_xmzt, form.projectStatus) }}</el-descriptions-item>
            <el-descriptions-item label="验收状态"><dict-tag :options="sys_yszt" :value="form.acceptanceStatus"/></el-descriptions-item>
            <el-descriptions-item label="合同金额">{{ formatAmount(form.contractAmount) }} 元</el-descriptions-item>
            <el-descriptions-item label="审核状态">{{ getDictLabel(sys_spzt, form.approvalStatus) }}</el-descriptions-item>
            <el-descriptions-item label="合同状态"><dict-tag :options="sys_htzt" :value="form.contractStatus"/></el-descriptions-item>
            <el-descriptions-item label="合同名称"><span style="white-space: pre-line;">{{ form.contractName || '-' }}</span></el-descriptions-item>
            <el-descriptions-item label="收入确认状态"><dict-tag :options="sys_qrzt" :value="form.revenueConfirmStatus"/></el-descriptions-item>
            <el-descriptions-item label="收入确认年度">{{ form.revenueConfirmYear || '-' }}</el-descriptions-item>
            <el-descriptions-item label="确认金额（含税）">{{ formatAmount(form.confirmAmount) }} 元</el-descriptions-item>
            <el-descriptions-item label="税率">{{ form.taxRate != null ? form.taxRate + '%' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="税后金额" :span="2">{{ formatAmount(form.afterTaxAmount) }} 元</el-descriptions-item>
            <el-descriptions-item label="项目地址" :span="2">{{ form.projectAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目计划" :span="2">{{ form.projectPlan || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="2">{{ form.projectDescription || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核意见" :span="2">{{ form.approvalReason || '-' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 人员配置 -->
          <div class="section-title">人员配置</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目经理">{{ form.projectManagerName }}</el-descriptions-item>
            <el-descriptions-item label="市场经理">{{ form.marketManagerName }}</el-descriptions-item>
            <el-descriptions-item label="销售负责人" :span="2">{{ form.salesManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="销售联系方式" :span="2">{{ form.salesContact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="参与人员" :span="2">{{ form.participantsNames }}</el-descriptions-item>
          </el-descriptions>

          <!-- 客户信息 -->
          <div class="section-title">客户信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="客户名称" :span="2">{{ form.customerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户联系人">{{ form.customerContactName }}</el-descriptions-item>
            <el-descriptions-item label="客户联系方式">{{ form.customerContactPhone }}</el-descriptions-item>
            <el-descriptions-item label="商户联系人">{{ form.merchantContact }}</el-descriptions-item>
            <el-descriptions-item label="商户联系方式">{{ form.merchantPhone }}</el-descriptions-item>
          </el-descriptions>

          <!-- 时间规划 -->
          <div class="section-title">时间规划</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="启动日期">{{ parseTime(form.startDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结束日期">{{ parseTime(form.endDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="实施年度" :span="2">{{ form.reservedField1 || '-' }}</el-descriptions-item>
            <el-descriptions-item label="投产日期">{{ parseTime(form.productionDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="验收日期">{{ parseTime(form.acceptanceDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 成本预算 -->
          <div class="section-title">成本预算</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目费用">{{ form.projectCost != null ? formatAmount(form.projectCost) + ' 元' : '- 元' }}</el-descriptions-item>
            <el-descriptions-item label="费用预算">{{ form.expenseBudget != null ? formatAmount(form.expenseBudget) + ' 元' : '- 元' }}</el-descriptions-item>
            <el-descriptions-item label="成本预算">{{ form.costBudget != null ? formatAmount(form.costBudget) + ' 元' : '- 元' }}</el-descriptions-item>
            <el-descriptions-item label="人力费用">{{ form.laborCost != null ? formatAmount(form.laborCost) + ' 元' : '- 元' }}</el-descriptions-item>
            <el-descriptions-item label="采购成本">{{ form.purchaseCost != null ? formatAmount(form.purchaseCost) + ' 元' : '- 元' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 其他信息 -->
          <div class="section-title">其他信息</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="备注"><span style="white-space: pre-line;">{{ form.remark || '-' }}</span></el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧（40%） -->
      <el-col :span="10">
        <!-- 公司收入确认信息 -->
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; align-items: center; justify-content: space-between;">
              <span style="font-weight: bold;">公司收入确认信息</span>
              <el-tag v-if="!isViewMode" type="warning" size="small">编辑中</el-tag>
            </div>
          </template>

          <!-- 查看模式：只读展示 -->
          <template v-if="isViewMode">
            <!-- 状态横幅 -->
            <div class="status-banner" :class="getStatusBannerClass(form.revenueConfirmStatus)">
              {{ getDictLabel(sys_qrzt, form.revenueConfirmStatus) }}
            </div>

            <!-- 验收状态 -->
            <div class="info-row">
              <div class="info-label">验收状态</div>
              <dict-tag :options="sys_yszt" :value="form.acceptanceStatus"/>
            </div>

            <!-- 确认年度 -->
            <div class="info-row">
              <div class="info-label"><el-icon><Calendar /></el-icon> 确认年度</div>
              <div class="info-value">{{ form.revenueConfirmYear || '-' }}</div>
            </div>

            <!-- 确认金额 -->
            <div class="amount-box">
              <div class="amount-box-label"><el-icon><Tickets /></el-icon> 确认金额(含税)</div>
              <div class="amount-value-red">¥ {{ formatAmount(form.confirmAmount) }}</div>
            </div>

            <!-- 税后金额 -->
            <div class="info-row">
              <div class="info-label">税后金额</div>
              <div class="info-value amount-blue-bold">¥ {{ formatAmount(form.afterTaxAmount) }}</div>
            </div>

            <!-- 税率 -->
            <div class="info-row">
              <div class="info-label">税率</div>
              <div class="info-value">{{ form.taxRate != null ? form.taxRate + '%' : '-' }}</div>
            </div>

            <el-divider style="margin: 16px 0;" />

            <!-- 确认人 -->
            <div class="info-row">
              <div class="info-label"><el-icon><User /></el-icon> 确认人</div>
              <div class="info-value">{{ form.companyRevenueConfirmedBy || '-' }}</div>
            </div>

            <!-- 确认时间 -->
            <div class="info-row">
              <div class="info-label"><el-icon><Clock /></el-icon> 确认时间</div>
              <div class="info-value">{{ parseTime(form.companyRevenueConfirmedTime, '{y}-{m}-{d}') || '-' }}</div>
            </div>

            <!-- 操作按钮 -->
            <div class="action-buttons">
              <el-button type="primary" @click="goEdit" style="width: 100%;">
                <el-icon><Edit /></el-icon>&nbsp;编辑收入确认
              </el-button>
              <el-button @click="goBack" style="width: 100%; margin-top: 8px; margin-left: 0;">返回列表</el-button>
            </div>
          </template>

          <!-- 编辑模式：表单 -->
          <template v-else>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
              <!-- 收入确认状态（必填，可编辑） -->
              <el-form-item label="收入确认状态" prop="revenueConfirmStatus">
                <el-select v-model="form.revenueConfirmStatus" placeholder="请选择收入确认状态" style="width: 100%;">
                  <el-option
                    v-for="dict in sys_qrzt"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>

              <!-- 验收状态（可编辑） -->
              <el-form-item label="验收状态">
                <el-select v-model="form.acceptanceStatus" placeholder="请选择验收状态" style="width: 100%;">
                  <el-option
                    v-for="dict in sys_yszt"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>

              <!-- 收入确认年度（非必填） -->
              <el-form-item label="收入确认年度">
                <el-select v-model="form.revenueConfirmYear" placeholder="请选择收入确认年度" clearable style="width: 100%;">
                  <el-option
                    v-for="dict in sys_ndgl"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>

              <!-- 确认金额（非必填） -->
              <el-form-item label="确认金额(含税)">
                <el-input
                  v-model="form.confirmAmount"
                  type="number"
                  placeholder="请输入确认金额"
                  style="width: 100%;"
                  @blur="formatConfirmAmount"
                >
                  <template #append>元</template>
                </el-input>
              </el-form-item>

              <!-- 税率（非必填） -->
              <el-form-item label="税率">
                <el-input
                  v-model="form.taxRate"
                  type="number"
                  placeholder="请输入税率"
                  style="width: 100%;"
                  @blur="formatTaxRate"
                >
                  <template #append>%</template>
                </el-input>
                <div class="tax-quick-btns">
                  <span class="tax-quick-label">常用税率：</span>
                  <el-button size="small" @click="form.taxRate = 6">6%</el-button>
                  <el-button size="small" @click="form.taxRate = 13">13%</el-button>
                </div>
              </el-form-item>

              <!-- 税后金额（自动计算，非必填） -->
              <el-form-item label="税后金额">
                <el-input v-model="form.afterTaxAmount" disabled style="width: 100%;">
                  <template #prefix>¥</template>
                </el-input>
                <div class="calc-tip">
                  <el-icon style="flex-shrink:0;margin-top:1px;"><InfoFilled /></el-icon>
                  根据确认金额和税率自动计算：税后金额 = 确认金额 / (1 + 税率/100)
                </div>
              </el-form-item>

              <!-- 备注（非必填） -->
              <el-form-item label="备注">
                <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入备注信息"
                  maxlength="500"
                  show-word-limit
                  style="width: 100%;"
                />
              </el-form-item>

              <!-- 按钮区：每个按钮单独一行 -->
              <el-form-item>
                <el-button type="primary" @click="submitForm" style="width: 100%;">
                  <el-icon><Check /></el-icon>&nbsp;提交
                </el-button>
              </el-form-item>
              <el-form-item>
                <el-button @click="resetFormData" style="width: 100%;">重置</el-button>
              </el-form-item>
              <el-form-item>
                <el-button @click="goBack" style="width: 100%;">取消</el-button>
              </el-form-item>
            </el-form>
          </template>
        </el-card>

        <!-- 团队收入确认明细 -->
        <el-card shadow="never" style="margin-top: 16px;">
          <template #header>
            <span style="font-weight: bold;">团队收入确认明细</span>
          </template>
          <el-table :data="teamList" border size="small">
            <el-table-column label="部门名称" prop="deptName" min-width="120" show-overflow-tooltip />
            <el-table-column label="确认金额" prop="confirmAmount" min-width="110" align="right">
              <template #default="{ row }">{{ formatAmount(row.confirmAmount) }} 元</template>
            </el-table-column>
            <el-table-column label="确认时间" prop="confirmTime" min-width="100">
              <template #default="{ row }">{{ parseTime(row.confirmTime, '{y}-{m}-{d}') }}</template>
            </el-table-column>
            <el-table-column label="确认人" prop="confirmUserName" min-width="90" show-overflow-tooltip />
            <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
          </el-table>
          <div v-if="teamList.length === 0" style="text-align: center; color: #909399; padding: 20px 0;">
            暂无团队收入确认数据
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="RevenueCompanyDetail">
import { getRevenue, updateRevenue } from "@/api/revenue/company"
import { getTeamRevenue } from "@/api/revenue/team"

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const { sys_qrzt, sys_ndgl, sys_xmfl, sys_yjqy, sys_xmzt, sys_yszt, sys_spzt, sys_htzt, industry } = proxy.useDict(
  'sys_qrzt', 'sys_ndgl', 'sys_xmfl', 'sys_yjqy', 'sys_xmzt', 'sys_yszt', 'sys_spzt', 'sys_htzt', 'industry'
)

const projectId = computed(() => route.params.projectId)
const mode = computed(() => route.query.mode || 'view')
const isViewMode = computed(() => mode.value === 'view')
const pageTitle = computed(() => isViewMode.value ? '收入确认详情' : '收入确认编辑')

const teamList = ref([])
const formOrigin = ref({})

const data = reactive({
  form: {},
  rules: {
    revenueConfirmStatus: [
      { required: true, message: "收入确认状态不能为空", trigger: "change" }
    ]
  }
})

const { form, rules } = toRefs(data)

/** 根据字典数据获取标签 */
function getDictLabel(dicts, value) {
  if (value === null || value === undefined || value === '') return '-'
  const dictArr = Array.isArray(dicts) ? dicts : (dicts?.value || [])
  const dict = dictArr.find(d => String(d.value) === String(value))
  return dict ? dict.label : (value || '-')
}

/** 根据收入确认状态返回横幅样式类（从字典 elTagType 动态读取，与 dict-tag 颜色保持一致） */
function getStatusBannerClass(status) {
  const dictArr = sys_qrzt.value || []
  const dict = dictArr.find(d => String(d.value) === String(status))
  const elTagType = dict?.elTagType || 'default'
  const typeMap = {
    'success': 'status-success',
    'warning': 'status-warning',
    'danger':  'status-danger',
    'primary': 'status-primary',
    'info':    'status-info',
    'default': 'status-default',
    '':        'status-default',
  }
  return typeMap[elTagType] || 'status-default'
}

/** 格式化金额为千分位 */
function formatAmount(amount) {
  if (amount === null || amount === undefined || amount === '') return '-'
  const num = parseFloat(amount)
  if (isNaN(num)) return '-'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 获取详情 */
function getDetail() {
  getRevenue(projectId.value).then(response => {
    form.value = response.data
    formOrigin.value = { ...response.data }
    if (!form.value.taxRate) {
      initTaxRate(form.value.projectCategory)
    }
  })
}

/** 获取团队收入确认明细 */
function getTeamDetail() {
  getTeamRevenue(projectId.value).then(response => {
    teamList.value = response.data?.detailList || []
  })
}

/** 根据项目分类初始化税率 */
function initTaxRate(projectCategory) {
  if (projectCategory === '1') {
    form.value.taxRate = 6
  } else if (projectCategory === '2') {
    form.value.taxRate = 13
  }
}

/** 格式化确认金额，保留2位小数 */
function formatConfirmAmount() {
  if (form.value.confirmAmount !== null && form.value.confirmAmount !== '') {
    const value = parseFloat(form.value.confirmAmount)
    if (!isNaN(value) && value >= 0) {
      form.value.confirmAmount = value.toFixed(2)
    } else {
      form.value.confirmAmount = null
    }
  }
}

/** 格式化税率，保留2位小数，限制范围0-100 */
function formatTaxRate() {
  if (form.value.taxRate !== null && form.value.taxRate !== '') {
    let value = parseFloat(form.value.taxRate)
    if (!isNaN(value)) {
      value = Math.max(0, Math.min(100, value))
      form.value.taxRate = value.toFixed(2)
    } else {
      form.value.taxRate = null
    }
  }
}

/** 计算税后金额 */
function calculateAfterTax() {
  const amount = parseFloat(form.value.confirmAmount)
  const rate = parseFloat(form.value.taxRate)
  if (!isNaN(amount) && !isNaN(rate) && amount > 0 && rate >= 0) {
    const afterTax = amount / (1 + rate / 100)
    form.value.afterTaxAmount = afterTax.toFixed(2)
  } else {
    form.value.afterTaxAmount = null
  }
}

watch(
  () => [form.value.confirmAmount, form.value.taxRate],
  () => {
    calculateAfterTax()
  }
)

/** 提交表单 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      updateRevenue(form.value).then(() => {
        proxy.$modal.msgSuccess("提交成功")
        goBack()
      })
    }
  })
}

/** 重置表单 */
function resetFormData() {
  form.value = { ...formOrigin.value }
  proxy.$refs["formRef"]?.clearValidate()
}

/** 返回列表 */
function goBack() {
  router.push("/revenue/company")
}

/** 进入编辑模式 */
function goEdit() {
  router.push(`/revenue/company/detail/${projectId.value}?mode=edit`)
}

// 初始化
getDetail()
getTeamDetail()

// 路由 query/params 变化时重新加载（组件复用场景）
watch(() => route.params.projectId, (newId) => {
  if (newId) {
    getDetail()
    getTeamDetail()
  }
})

// 同步面包屑 tab 标签标题（模式切换时更新，守卫：仅在详情路由上执行）
watch(pageTitle, (title) => {
  if (!route.path.includes('/company/detail')) return
  nextTick(() => {
    proxy.$tab.updatePage({
      path: route.path,
      title,
      meta: { ...route.meta, title }
    })
  })
}, { immediate: true })
</script>

<style scoped>
.section-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-top: 20px;
  margin-bottom: 10px;
  padding-left: 10px;
  border-left: 3px solid #409EFF;
}

.section-title:first-child {
  margin-top: 0;
}

/* 右侧查看模式 */
.status-banner {
  text-align: center;
  font-size: 16px;
  font-weight: bold;
  padding: 12px 0;
  border-radius: 6px;
  margin-bottom: 20px;
}
.status-success { background: #f0f9eb; color: #67c23a; }
.status-warning { background: #fdf6ec; color: #e6a23c; }
.status-danger  { background: #fef0f0; color: #f56c6c; }
.status-primary { background: #ecf5ff; color: #409eff; }
.status-info    { background: #f4f4f5; color: #909399; }
.status-default { background: #f4f4f5; color: #909399; }

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 14px;
  color: #303133;
}
.info-label {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
  font-size: 13px;
  flex-shrink: 0;
}
.info-value {
  font-weight: 500;
  color: #303133;
}

.amount-box {
  border: 1px dashed #f56c6c;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 14px;
  background: #fff8f8;
}
.amount-box-label {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
  font-size: 13px;
  margin-bottom: 6px;
}
.amount-value-red {
  font-size: 22px;
  font-weight: bold;
  color: #f56c6c;
}

.amount-blue-bold {
  font-size: 16px;
  font-weight: bold;
  color: #409eff;
}

.action-buttons {
  margin-top: 20px;
}

/* 编辑模式 */
.tax-quick-btns {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  width: 100%;
}
.tax-quick-label {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.calc-tip {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  margin-top: 6px;
  width: 100%;
  font-size: 12px;
  color: #fff;
  background: #409eff;
  border-radius: 4px;
  padding: 6px 10px;
  line-height: 1.5;
  box-sizing: border-box;
}

</style>
