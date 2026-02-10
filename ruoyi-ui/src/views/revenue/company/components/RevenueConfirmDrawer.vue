<template>
  <el-drawer
    :title="drawerTitle"
    v-model="visible"
    direction="rtl"
    size="80%"
    :before-close="handleClose"
  >
    <el-row :gutter="20">
      <!-- 左侧：项目信息展示（60%） -->
      <el-col :span="14">
        <div class="project-info-section">
          <!-- 1. 项目基本信息 -->
          <el-card class="info-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">项目基本信息</span>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="行业">
                <dict-tag :options="industry" :value="projectData.industry" />
              </el-descriptions-item>
              <el-descriptions-item label="区域">
                <dict-tag :options="sys_yjqy" :value="projectData.region" />
              </el-descriptions-item>
              <el-descriptions-item label="简称">{{ projectData.shortName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="立项年份">{{ projectData.establishedYear || '-' }} 年</el-descriptions-item>
              <el-descriptions-item label="项目编号" :span="2">{{ projectData.projectCode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目名称" :span="2">{{ projectData.projectName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目分类">
                <dict-tag :options="sys_xmfl" :value="projectData.projectCategory" />
              </el-descriptions-item>
              <el-descriptions-item label="项目部门">{{ projectData.deptName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="预估工作量">{{ projectData.estimatedWorkload || 0 }} 人天</el-descriptions-item>
              <el-descriptions-item label="实际工作量">{{ projectData.actualWorkload || 0 }} 人天</el-descriptions-item>
              <el-descriptions-item label="项目状态">
                <dict-tag :options="sys_xmjd" :value="projectData.projectStatus" />
              </el-descriptions-item>
              <el-descriptions-item label="验收状态">
                <dict-tag :options="sys_yszt" :value="projectData.acceptanceStatus" />
              </el-descriptions-item>
              <el-descriptions-item label="审核状态">
                <dict-tag :options="sys_spzt" :value="projectData.approvalStatus" />
              </el-descriptions-item>
              <el-descriptions-item label="项目地址" :span="2">{{ projectData.projectAddress || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目计划" :span="2">
                <div class="text-content">{{ projectData.projectPlan || '-' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="项目描述" :span="2">
                <div class="text-content">{{ projectData.projectDescription || '-' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="审核意见" :span="2">{{ projectData.approvalReason || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 2. 人员配置 -->
          <el-card class="info-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">人员配置</span>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="项目经理">{{ projectData.projectManagerName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="市场经理">{{ projectData.marketManagerName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="销售负责人">{{ projectData.salesManagerName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="销售联系方式">{{ projectData.salesContact || '-' }}</el-descriptions-item>
              <el-descriptions-item label="参与人员" :span="2">{{ projectData.participantsNames || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 3. 客户信息 -->
          <el-card class="info-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">客户信息</span>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="客户简称">{{ projectData.customerName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="客户联系人">{{ projectData.customerContactName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="客户联系方式">{{ projectData.customerContactPhone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="商户联系人">{{ projectData.merchantContact || '-' }}</el-descriptions-item>
              <el-descriptions-item label="商户联系方式" :span="2">{{ projectData.merchantPhone || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 4. 时间规划 -->
          <el-card class="info-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">时间规划</span>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="启动日期">{{ projectData.startDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="结束日期">{{ projectData.endDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="投产日期">{{ projectData.productionDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="验收日期">{{ projectData.acceptanceDate || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 5. 成本预算 -->
          <el-card class="info-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">成本预算</span>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="项目预算">{{ projectData.projectBudget || 0 }} 元</el-descriptions-item>
              <el-descriptions-item label="项目费用">{{ projectData.projectCost || 0 }} 元</el-descriptions-item>
              <el-descriptions-item label="费用预算">{{ projectData.costBudget || 0 }} 元</el-descriptions-item>
              <el-descriptions-item label="成本预算">{{ projectData.budgetCost || 0 }} 元</el-descriptions-item>
              <el-descriptions-item label="人力费用">{{ projectData.laborCost || 0 }} 元</el-descriptions-item>
              <el-descriptions-item label="采购成本">{{ projectData.purchaseCost || 0 }} 元</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 6. 备注 -->
          <el-card class="info-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">备注</span>
              </div>
            </template>
            <div class="remark-content">
              {{ projectData.remark || '无' }}
            </div>
          </el-card>
        </div>
      </el-col>

      <!-- 右侧：收入确认表单（40%） -->
      <el-col :span="10">
        <div class="revenue-form-section">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">收入确认信息</span>
                <el-button
                  v-if="mode === 'view' && projectData.revenueConfirmStatus === '1'"
                  type="primary"
                  size="small"
                  @click="switchToEditMode"
                  v-hasPermi="['revenue:company:edit']"
                >编辑</el-button>
              </div>
            </template>

            <el-form ref="formRef" :model="formData" :rules="rules" label-width="140px">
              <!-- 收入确认状态（只读显示） -->
              <el-form-item label="收入确认状态">
                <dict-tag :options="sys_srqrzt" :value="projectData.revenueConfirmStatus"/>
              </el-form-item>

              <!-- 收入确认年度（必填） -->
              <el-form-item label="收入确认年度" prop="revenueConfirmYear">
                <el-select
                  v-model="formData.revenueConfirmYear"
                  placeholder="请选择收入确认年度"
                  :disabled="mode === 'view'"
                  style="width: 100%"
                >
                  <el-option
                    v-for="dict in sys_ndgl"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>

              <!-- 确认金额（含税）（必填） -->
              <el-form-item label="确认金额（含税）" prop="confirmAmount">
                <el-input-number
                  v-model="formData.confirmAmount"
                  :precision="2"
                  :step="1000"
                  :min="0"
                  :max="999999999.99"
                  :disabled="mode === 'view'"
                  style="width: 100%"
                  @change="calculateAfterTaxAmount"
                />
              </el-form-item>

              <!-- 税率（必填，带快捷按钮） -->
              <el-form-item label="税率" prop="taxRate">
                <el-input-number
                  v-model="formData.taxRate"
                  :precision="2"
                  :step="1"
                  :min="0"
                  :max="100"
                  :disabled="mode === 'view'"
                  style="width: 100%"
                  @change="calculateAfterTaxAmount"
                />
                <div v-if="mode !== 'view'" style="margin-top: 8px;">
                  <el-button size="small" @click="setTaxRate(6)">6%</el-button>
                  <el-button size="small" @click="setTaxRate(13)">13%</el-button>
                </div>
              </el-form-item>

              <!-- 税后金额（自动计算） -->
              <el-form-item label="税后金额">
                <el-input-number
                  v-model="formData.afterTaxAmount"
                  :precision="2"
                  disabled
                  style="width: 100%"
                />
              </el-form-item>

              <!-- 备注（可选） -->
              <el-form-item label="备注" prop="remark">
                <el-input
                  v-model="formData.remark"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入备注"
                  :disabled="mode === 'view'"
                />
              </el-form-item>

              <!-- 确认人和确认时间（已确认时显示） -->
              <el-form-item v-if="projectData.revenueConfirmStatus === '1'" label="确认人">
                <span>{{ getUserName(projectData.companyRevenueConfirmedBy) }}</span>
              </el-form-item>
              <el-form-item v-if="projectData.revenueConfirmStatus === '1'" label="确认时间">
                <span>{{ projectData.companyRevenueConfirmedTime ? parseTime(projectData.companyRevenueConfirmedTime) : '-' }}</span>
              </el-form-item>

              <!-- 操作按钮 -->
              <el-form-item v-if="mode !== 'view'">
                <el-button type="primary" @click="submitForm">保存</el-button>
                <el-button @click="handleClose">取消</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </div>
      </el-col>
    </el-row>
  </el-drawer>
</template>

<script setup name="RevenueConfirmDrawer">
import { getRevenueCompany, updateRevenueConfirm } from "@/api/revenue/company";
import { listUser } from "@/api/system/user";

const { proxy } = getCurrentInstance();
const { sys_srqrzt, sys_ndgl, industry, sys_yjqy, sys_xmfl, sys_xmjd, sys_yszt, sys_spzt } = proxy.useDict('sys_srqrzt', 'sys_ndgl', 'industry', 'sys_yjqy', 'sys_xmfl', 'sys_xmjd', 'sys_yszt', 'sys_spzt');

const emit = defineEmits(['refresh']);

const visible = ref(false);
const mode = ref('edit'); // 'view' | 'edit'
const projectData = ref({});
const userMap = ref({});
const formData = ref({
  projectId: null,
  revenueConfirmYear: null,
  confirmAmount: null,
  taxRate: null,
  afterTaxAmount: null,
  remark: ''
});

const rules = {
  revenueConfirmYear: [
    { required: true, message: "收入确认年度不能为空", trigger: "change" }
  ],
  confirmAmount: [
    { required: true, message: "确认金额不能为空", trigger: "blur" }
  ],
  taxRate: [
    { required: true, message: "税率不能为空", trigger: "blur" }
  ]
};

const drawerTitle = computed(() => {
  if (mode.value === 'view') {
    return `查看收入确认 - ${projectData.value.projectName || ''}`;
  } else if (projectData.value.revenueConfirmStatus === '1') {
    return `编辑收入确认 - ${projectData.value.projectName || ''}`;
  } else {
    return `公司收入确认 - ${projectData.value.projectName || ''}`;
  }
});

/** 打开抽屉 */
function open(projectId, viewMode = false) {
  mode.value = viewMode ? 'view' : 'edit';
  // 获取用户列表
  getUserList();
  // 获取项目数据
  getRevenueCompany(projectId).then(response => {
    projectData.value = response.data;
    console.log('项目数据:', projectData.value);
    console.log('确认时间:', projectData.value.companyRevenueConfirmedTime);
    console.log('确认人ID:', projectData.value.companyRevenueConfirmedBy);
    console.log('确认状态:', projectData.value.revenueConfirmStatus);
    // 初始化表单数据
    formData.value = {
      projectId: projectData.value.projectId,
      revenueConfirmYear: projectData.value.revenueConfirmYear,
      confirmAmount: projectData.value.confirmAmount,
      taxRate: projectData.value.taxRate,
      afterTaxAmount: projectData.value.afterTaxAmount,
      remark: projectData.value.remark
    };
    visible.value = true;
  });
}

/** 获取用户列表 */
function getUserList() {
  listUser().then(response => {
    const users = response.rows;
    userMap.value = {};
    users.forEach(user => {
      userMap.value[user.userId] = user.nickName;
    });
  });
}

/** 切换到编辑模式 */
function switchToEditMode() {
  mode.value = 'edit';
}

/** 设置税率快捷按钮 */
function setTaxRate(rate) {
  formData.value.taxRate = rate;
  calculateAfterTaxAmount();
}

/** 自动计算税后金额 */
function calculateAfterTaxAmount() {
  if (formData.value.confirmAmount && formData.value.taxRate != null) {
    const confirmAmount = parseFloat(formData.value.confirmAmount);
    const taxRate = parseFloat(formData.value.taxRate);
    const afterTaxAmount = confirmAmount / (1 + taxRate / 100);
    formData.value.afterTaxAmount = parseFloat(afterTaxAmount.toFixed(2));
  } else {
    formData.value.afterTaxAmount = null;
  }
}

/** 提交表单 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      updateRevenueConfirm(formData.value).then(response => {
        proxy.$modal.msgSuccess("保存成功");
        visible.value = false;
        emit('refresh');
      });
    }
  });
}

/** 关闭抽屉 */
function handleClose() {
  visible.value = false;
  proxy.resetForm("formRef");
}

/** 格式化金额 */
function formatMoney(value) {
  if (value == null || value === '') return '-';
  return parseFloat(value).toFixed(2) + ' 元';
}

/** 获取用户名 */
function getUserName(userId) {
  return userMap.value[userId] || '-';
}

defineExpose({
  open
});
</script>

<style scoped lang="scss">
.project-info-section {
  .info-card {
    margin-bottom: 16px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-weight: bold;
        font-size: 14px;
      }
    }

    .remark-content {
      padding: 12px;
      background-color: #f5f7fa;
      border-radius: 4px;
      min-height: 60px;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }
}

.revenue-form-section {
  position: sticky;
  top: 0;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      font-weight: bold;
      font-size: 14px;
    }
  }
}
</style>
