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

          <!-- 基本信息 -->
          <div class="section-title">基本信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目名称">{{ form.projectName }}</el-descriptions-item>
            <el-descriptions-item label="项目编码">{{ form.projectCode }}</el-descriptions-item>
            <el-descriptions-item label="项目部门">{{ form.projectDeptName }}</el-descriptions-item>
            <el-descriptions-item label="项目分类">{{ form.projectCategory }}</el-descriptions-item>
            <el-descriptions-item label="预估工作量">{{ form.estimatedWorkload }} 人天</el-descriptions-item>
            <el-descriptions-item label="实际工作量">{{ form.actualWorkload }} 人天</el-descriptions-item>
            <el-descriptions-item label="项目阶段">{{ form.projectStage }}</el-descriptions-item>
            <el-descriptions-item label="验收状态">{{ form.acceptanceStatus }}</el-descriptions-item>
            <el-descriptions-item label="一级区域">{{ form.region }}</el-descriptions-item>
            <el-descriptions-item label="二级区域">{{ form.regionName }}</el-descriptions-item>
            <el-descriptions-item label="项目地址" :span="2">{{ form.projectAddress }}</el-descriptions-item>
            <el-descriptions-item label="项目计划" :span="2">{{ form.projectPlan }}</el-descriptions-item>
          </el-descriptions>

          <!-- 人员配置 -->
          <div class="section-title">人员配置</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目经理">{{ form.projectManagerName }}</el-descriptions-item>
            <el-descriptions-item label="市场经理">{{ form.marketManagerName }}</el-descriptions-item>
            <el-descriptions-item label="销售负责人">{{ form.salesManagerName }}</el-descriptions-item>
            <el-descriptions-item label="销售联系方式">{{ form.salesContact }}</el-descriptions-item>
            <el-descriptions-item label="参与人员" :span="2">{{ form.participantsNames }}</el-descriptions-item>
          </el-descriptions>

          <!-- 客户信息 -->
          <div class="section-title">客户信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="客户名称">{{ form.customerName }}</el-descriptions-item>
            <el-descriptions-item label="客户联系人">{{ form.customerContactName }}</el-descriptions-item>
            <el-descriptions-item label="客户联系方式">{{ form.customerContactPhone }}</el-descriptions-item>
            <el-descriptions-item label="商户联系人">{{ form.merchantContact }}</el-descriptions-item>
            <el-descriptions-item label="商户联系方式">{{ form.merchantPhone }}</el-descriptions-item>
          </el-descriptions>

          <!-- 时间规划 -->
          <div class="section-title">时间规划</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="启动日期">{{ parseTime(form.startDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="结束日期">{{ parseTime(form.endDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="上线日期">{{ parseTime(form.productionDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="验收日期">{{ parseTime(form.acceptanceDate, '{y}-{m}-{d}') }}</el-descriptions-item>
          </el-descriptions>

          <!-- 成本预算 -->
          <div class="section-title">成本预算</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目预算">{{ form.projectBudget }} 元</el-descriptions-item>
            <el-descriptions-item label="项目费用">{{ form.projectCost }} 元</el-descriptions-item>
            <el-descriptions-item label="成本预算">{{ form.costBudget }} 元</el-descriptions-item>
            <el-descriptions-item label="人工成本">{{ form.laborCost }} 元</el-descriptions-item>
            <el-descriptions-item label="采购成本">{{ form.purchaseCost }} 元</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧：收入确认表单（40%） -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: bold;">收入确认信息</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
            <!-- 表单字段将在下一步添加 -->

            <el-form-item style="margin-top: 20px;">
              <el-button type="primary" @click="submitForm">保存</el-button>
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

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const projectId = ref(route.params.projectId)
const pageTitle = ref("收入确认")

const data = reactive({
  form: {},
  rules: {}
})

const { form, rules } = toRefs(data)

/** 获取详情 */
function getDetail() {
  getRevenue(projectId.value).then(response => {
    form.value = response.data
  })
}

/** 提交表单 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      updateRevenue(form.value).then(response => {
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

// 初始化
getDetail()
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
</style>
