<template>
  <div class="app-container">
    <el-row :gutter="16">
      <!-- 左侧：项目信息 -->
      <el-col :span="14">
        <!-- 项目基本信息 -->
        <el-card shadow="never" style="margin-bottom: 15px;">
          <template #header>
            <span style="font-weight: bold;">项目基本信息</span>
          </template>
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="行业">
              <dict-tag :options="industry" :value="form.industry" />
            </el-descriptions-item>
            <el-descriptions-item label="一级区域">
              <dict-tag :options="sys_yjqy" :value="form.region" />
            </el-descriptions-item>
            <el-descriptions-item label="二级区域">{{ form.regionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="立项年度">
              <dict-tag :options="sys_ndgl" :value="String(form.establishedYear || '')" />
            </el-descriptions-item>
            <el-descriptions-item label="项目编号">{{ form.projectCode }}</el-descriptions-item>
            <el-descriptions-item label="项目预算">{{ form.projectBudget != null ? form.projectBudget + ' 元' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目名称" :span="2">{{ form.projectName }}</el-descriptions-item>
            <el-descriptions-item label="项目分类">
              <dict-tag :options="sys_xmfl" :value="form.projectCategory" />
            </el-descriptions-item>
            <el-descriptions-item label="项目部门">{{ form.projectDeptName }}</el-descriptions-item>
            <el-descriptions-item label="预估工作量">{{ form.estimatedWorkload != null ? form.estimatedWorkload + ' 人天' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="实际人天">{{ form.actualWorkload != null ? form.actualWorkload + ' 人天' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目状态">
              <dict-tag :options="sys_xmzt" :value="form.projectStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="验收状态">
              <dict-tag :options="sys_yszt" :value="form.acceptanceStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="合同金额">{{ form.contractAmount != null ? form.contractAmount + ' 元' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核状态">
              <dict-tag :options="sys_spzt" :value="form.approvalStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="合同状态">
              <dict-tag :options="sys_htzt" :value="form.contractStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="合同名称">{{ form.contractName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目地址" :span="2">{{ form.projectAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目计划" :span="2">{{ form.projectPlan || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="2">{{ form.projectDescription || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 人员配置 -->
        <el-card shadow="never" style="margin-bottom: 15px;">
          <template #header>
            <span style="font-weight: bold;">人员配置</span>
          </template>
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="项目经理">{{ form.projectManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="市场经理">{{ form.marketManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="销售负责人">{{ form.salesManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="销售联系方式">{{ form.salesContact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="参与人员" :span="2">{{ form.participantsNames || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 客户信息 -->
        <el-card shadow="never" style="margin-bottom: 15px;">
          <template #header>
            <span style="font-weight: bold;">客户信息</span>
          </template>
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="客户名称">{{ form.customerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户联系人">{{ form.customerContactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户联系方式">{{ form.customerContactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商户联系人">{{ form.merchantContact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商户联系方式" :span="2">{{ form.merchantPhone || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 时间规划 -->
        <el-card shadow="never" style="margin-bottom: 15px;">
          <template #header>
            <span style="font-weight: bold;">时间规划</span>
          </template>
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="启动日期">{{ parseTime(form.startDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结束日期">{{ parseTime(form.endDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上线日期">{{ parseTime(form.productionDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="验收日期">{{ parseTime(form.acceptanceDate, '{y}-{m}-{d}') || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 成本预算 -->
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: bold;">成本预算</span>
          </template>
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="项目预算">{{ form.projectBudget != null ? form.projectBudget + ' 元' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目费用">{{ form.projectCost != null ? form.projectCost + ' 元' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="成本预算">{{ form.budgetCost != null ? form.budgetCost + ' 元' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="人工成本">{{ form.laborCost != null ? form.laborCost + ' 元' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="采购成本" :span="2">{{ form.purchaseCost != null ? form.purchaseCost + ' 元' : '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧：收入确认表单 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span style="font-weight: bold;">公司收入确认信息</span>
              <el-tag v-if="!isViewMode" type="primary" size="default">编辑中</el-tag>
            </div>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
            <el-form-item label="收入确认状态" prop="revenueConfirmStatus">
              <el-select v-model="form.revenueConfirmStatus" placeholder="请选择" style="width: 100%;" :disabled="isViewMode">
                <el-option v-for="dict in sys_srqrzt" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>

            <el-form-item label="验收状态">
              <dict-tag :options="sys_yszt" :value="form.acceptanceStatus" />
            </el-form-item>

            <el-form-item label="收入确认年">
              <el-select v-model="form.revenueConfirmYear" placeholder="请选择收入确认年" style="width: 100%;" :disabled="isViewMode">
                <el-option v-for="dict in sys_ndgl" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>

            <el-form-item label="确认金额(含税)">
              <el-input
                v-model="form.confirmAmount"
                type="number"
                placeholder="请输入金额"
                :disabled="isViewMode"
                style="width: 100%;"
              >
                <template #append>元</template>
              </el-input>
            </el-form-item>

            <el-form-item label="税率">
              <el-input
                v-model="form.taxRate"
                type="number"
                placeholder="请输入税率"
                :disabled="isViewMode"
                style="width: 100%;"
              >
                <template #append>%</template>
              </el-input>
              <div style="margin-top: 6px; line-height: 1;">
                <span style="color: #909399; font-size: 13px; margin-right: 6px;">常用税率：</span>
                <span class="rate-btn" :class="{ disabled: isViewMode }" @click="setTaxRate(6)">6%</span>
                <span class="rate-btn" :class="{ disabled: isViewMode }" @click="setTaxRate(13)">13%</span>
              </div>
            </el-form-item>

            <el-form-item label="税后金额" style="margin-bottom: 0;">
              <span class="after-tax-amount">¥ {{ afterTaxDisplay }}</span>
            </el-form-item>
            <div class="tax-tip" style="padding-left: 130px; margin-top: -10px; margin-bottom: 18px;">
              ⓘ 根据确认金额和税率自动计算：税后金额 = 确认金额 / (1 + 税率/100)
            </div>

            <el-form-item label="备注">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="4"
                :maxlength="500"
                show-word-limit
                placeholder="请输入备注信息"
                :disabled="isViewMode"
              />
            </el-form-item>

            <el-form-item>
              <template v-if="!isViewMode">
                <el-button type="primary" @click="submitForm">✓ 提交</el-button>
                <el-button @click="resetForm">重置</el-button>
              </template>
              <el-button v-if="isViewMode" type="primary" @click="toggleEditMode">编辑</el-button>
              <el-button @click="goBack">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="RevenueCompanyDetail">
import { getRevenue, updateRevenue } from "@/api/revenue/company"
import { parseTime } from "@/utils/ruoyi"

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const {
  sys_srqrzt, sys_ndgl, sys_xmfl, sys_xmzt, sys_yszt, sys_spzt, sys_htzt, industry, sys_yjqy
} = proxy.useDict('sys_srqrzt', 'sys_ndgl', 'sys_xmfl', 'sys_xmzt', 'sys_yszt', 'sys_spzt', 'sys_htzt', 'industry', 'sys_yjqy')

const projectId = ref(route.params.projectId)
const mode = ref(route.query.mode || 'edit')
const isViewMode = computed(() => mode.value === 'view')

const data = reactive({
  form: {},
  rules: {
    revenueConfirmStatus: [
      { required: true, message: "收入确认状态不能为空", trigger: "change" }
    ]
  }
})

const { form, rules } = toRefs(data)

/** 税后金额展示（实时计算） */
const afterTaxDisplay = computed(() => {
  const amount = parseFloat(form.value.confirmAmount)
  const rate = parseFloat(form.value.taxRate)
  if (!isNaN(amount) && !isNaN(rate) && amount >= 0 && rate >= 0) {
    return (amount / (1 + rate / 100)).toFixed(2)
  }
  return '0.00'
})

/** 获取详情 */
function getDetail() {
  getRevenue(projectId.value).then(response => {
    form.value = response.data
    if (!form.value.taxRate && form.value.taxRate !== 0) {
      initTaxRate(form.value.projectCategory)
    }
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

/** 设置常用税率 */
function setTaxRate(rate) {
  if (isViewMode.value) return
  form.value.taxRate = rate
}

/** 重置表单（重新加载数据） */
function resetForm() {
  getDetail()
}

/** 提交 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      updateRevenue(form.value).then(() => {
        proxy.$modal.msgSuccess("保存成功")
        goBack()
      })
    }
  })
}

/** 返回 */
function goBack() {
  router.push("/revenue/company")
}

/** 切换编辑模式 */
function toggleEditMode() {
  mode.value = 'edit'
}

getDetail()
</script>

<style scoped>
.after-tax-amount {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.tax-tip {
  margin-top: 6px;
  color: #409EFF;
  font-size: 12px;
  line-height: 1.5;
}

.rate-btn {
  display: inline-block;
  padding: 2px 10px;
  border: 1px solid #409EFF;
  border-radius: 12px;
  color: #409EFF;
  font-size: 12px;
  cursor: pointer;
  margin-right: 6px;
  transition: background 0.2s;
  user-select: none;
}

.rate-btn:hover {
  background-color: #ecf5ff;
}

.rate-btn.disabled {
  border-color: #c0c4cc;
  color: #c0c4cc;
  cursor: not-allowed;
}

.rate-btn.disabled:hover {
  background-color: transparent;
}
</style>
