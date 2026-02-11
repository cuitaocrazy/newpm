<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <h2 style="margin: 0 0 6px 0; font-weight: bold;">立项申请</h2>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
      <!-- 折叠控制工具栏 -->
      <div style="text-align: right; margin-bottom: 10px;">
        <el-link type="primary" @click="expandAll" style="margin-right: 10px;">全部展开</el-link>
        <el-link type="primary" @click="collapseAll">全部折叠</el-link>
      </div>

      <!-- 一、项目基本信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="1">
        <template #header>
          <div style="display: flex; align-items: center;">
            <div @click="togglePanel('1')" style="cursor: pointer; user-select: none; flex: 1;">
              <i :class="activeNames.includes('1') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
              <span style="font-size: 16px; font-weight: bold;">一、项目基本信息</span>
            </div>
          </div>
        </template>
        <div v-show="activeNames.includes('1')">

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="行业" prop="industry" data-prop="industry">
            <el-select v-model="form.industry" placeholder="请选择行业" @change="generateProjectCode" @blur="validateOnBlur('industry')">
              <el-option
                v-for="dict in industry"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="一级区域" prop="region" data-prop="region">
            <el-select v-model="form.region" placeholder="请选择一级区域" @change="handleRegionChange" @blur="validateOnBlur('region')">
              <el-option
                v-for="dict in sys_yjqy"
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
          <el-form-item label="二级区域" prop="regionCode" data-prop="regionCode">
            <el-select v-model="form.regionCode" placeholder="请选择二级区域" :disabled="!form.region" @change="handleSecondaryRegionChange" @blur="validateOnBlur('regionCode')">
              <el-option
                v-for="item in secondaryRegionOptions"
                :key="item.regionCode"
                :label="item.regionName"
                :value="item.regionCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="简称" prop="shortName" data-prop="shortName">
            <el-input v-model="form.shortName" placeholder="请输入简称" @input="generateProjectCode" @blur="validateOnBlur('shortName')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="立项年度" prop="establishedYear" data-prop="establishedYear">
            <el-select v-model="form.establishedYear" placeholder="请选择立项年度" @change="generateProjectCode" @blur="validateOnBlur('establishedYear')">
              <el-option
                v-for="dict in sys_ndgl"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="项目名称" prop="projectName" data-prop="projectName">
            <el-input v-model="form.projectName" placeholder="请输入项目名称" @blur="validateOnBlur('projectName')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="项目编号" prop="projectCode" data-prop="projectCode">
            <el-input v-model="form.projectCode" placeholder="自动生成" readonly />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="项目分类" prop="projectCategory" data-prop="projectCategory">
            <el-select v-model="form.projectCategory" placeholder="请选择项目分类" @blur="validateOnBlur('projectCategory')">
              <el-option
                v-for="dict in sys_xmfl"
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
          <el-form-item label="项目部门" prop="projectDept" data-prop="projectDept">
            <project-dept-select
              v-model="form.projectDept"
              @blur="validateOnBlur('projectDept')"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="项目阶段" prop="projectStage" data-prop="projectStage">
            <el-select v-model="form.projectStage" placeholder="请选择项目阶段" @blur="validateOnBlur('projectStage')">
              <el-option
                v-for="dict in sys_xmjd"
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
          <el-form-item label="验收状态" prop="acceptanceStatus" data-prop="acceptanceStatus">
            <el-select v-model="form.acceptanceStatus" placeholder="请选择验收状态" @blur="validateOnBlur('acceptanceStatus')">
              <el-option
                v-for="dict in sys_yszt"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="预估工作量(人天)" prop="estimatedWorkload" data-prop="estimatedWorkload">
            <el-input-number v-model="form.estimatedWorkload" :min="0" :precision="2" placeholder="请输入预估工作量" style="width: 100%" @blur="validateOnBlur('estimatedWorkload')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="项目预算(元)" prop="projectBudget" data-prop="projectBudget">
            <el-input-number v-model="form.projectBudget" :min="0" :precision="2" placeholder="请输入项目预算" style="width: 100%" @blur="validateOnBlur('projectBudget')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="项目地址" prop="projectAddress" data-prop="projectAddress">
            <el-input v-model="form.projectAddress" placeholder="请输入项目地址" @blur="validateOnBlur('projectAddress')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="项目计划" prop="projectPlan" data-prop="projectPlan">
            <el-input v-model="form.projectPlan" type="textarea" :rows="3" placeholder="请输入项目计划" @blur="validateOnBlur('projectPlan')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="项目描述" prop="projectDescription" data-prop="projectDescription">
            <el-input v-model="form.projectDescription" type="textarea" :rows="3" placeholder="请输入项目描述（用于项目阶段为服务类别，在此添加巡检相关信息）" @blur="validateOnBlur('projectDescription')" />
          </el-form-item>
        </el-col>
      </el-row>
        </div>
      </el-card>

      <!-- 二、人员配置 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="3">
        <template #header>
          <div @click="togglePanel('3')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('3') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">二、人员配置</span>
          </div>
        </template>
        <div v-show="activeNames.includes('3')">

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="项目经理" prop="projectManagerId" data-prop="projectManagerId">
            <el-select v-model="form.projectManagerId" placeholder="请选择项目经理" filterable @blur="validateOnBlur('projectManagerId')">
              <el-option
                v-for="user in projectManagerOptions"
                :key="user.userId"
                :label="user.nickName"
                :value="user.userId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="市场经理" prop="marketManagerId" data-prop="marketManagerId">
            <el-select v-model="form.marketManagerId" placeholder="请选择市场经理" filterable @blur="validateOnBlur('marketManagerId')">
              <el-option
                v-for="user in marketManagerOptions"
                :key="user.userId"
                :label="user.nickName"
                :value="user.userId"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="销售负责人" prop="salesManagerId" data-prop="salesManagerId">
            <el-select v-model="form.salesManagerId" placeholder="请选择销售负责人" filterable @change="handleSalesManagerChange" @blur="validateOnBlur('salesManagerId')">
              <el-option
                v-for="user in salesManagerOptions"
                :key="user.userId"
                :label="user.nickName"
                :value="user.userId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="销售联系方式" prop="salesContact" data-prop="salesContact">
            <el-input v-model="form.salesContact" placeholder="自动带出" readonly />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="参与人员" prop="participants" data-prop="participants">
            <el-select v-model="participantIds" placeholder="请选择参与人员" multiple filterable style="width: 100%" @blur="validateOnBlur('participants')">
              <el-option
                v-for="user in allUserOptions"
                :key="user.userId"
                :label="user.nickName"
                :value="user.userId"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
        </div>
      </el-card>

      <!-- 三、客户信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="4">
        <template #header>
          <div @click="togglePanel('4')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('4') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">三、客户信息</span>
          </div>
        </template>
        <div v-show="activeNames.includes('4')">

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="客户名称" prop="customerId" data-prop="customerId">
            <el-select v-model="form.customerId" placeholder="请选择客户" filterable @change="handleCustomerChange" @blur="validateOnBlur('customerId')">
              <el-option
                v-for="customer in customerOptions"
                :key="customer.customerId"
                :label="customer.customerSimpleName"
                :value="customer.customerId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="客户联系人" prop="customerContactId" data-prop="customerContactId">
            <el-select v-model="form.customerContactId" placeholder="请选择客户联系人" :disabled="!form.customerId" @change="handleContactChange" @blur="validateOnBlur('customerContactId')">
              <el-option
                v-for="contact in contactOptions"
                :key="contact.contactId"
                :label="contact.contactName"
                :value="contact.contactId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="客户联系方式">
            <el-input v-model="customerContactPhone" placeholder="自动带出" readonly />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="商户联系人" prop="merchantContact" data-prop="merchantContact">
            <el-input v-model="form.merchantContact" placeholder="请输入商户联系人" @blur="validateOnBlur('merchantContact')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="商户联系方式">
            <el-input v-model="form.merchantPhone" placeholder="请输入商户联系方式" />
          </el-form-item>
        </el-col>
      </el-row>
        </div>
      </el-card>

      <!-- 四、时间规划 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="5">
        <template #header>
          <div @click="togglePanel('5')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('5') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">四、时间规划</span>
          </div>
        </template>
        <div v-show="activeNames.includes('5')">

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="启动日期" prop="startDate" data-prop="startDate">
            <el-date-picker
              v-model="form.startDate"
              type="date"
              placeholder="选择启动日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              @blur="validateOnBlur('startDate')"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结束日期" prop="endDate" data-prop="endDate">
            <el-date-picker
              v-model="form.endDate"
              type="date"
              placeholder="选择结束日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              @blur="validateOnBlur('endDate')"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="投产日期" prop="productionDate" data-prop="productionDate">
            <el-date-picker
              v-model="form.productionDate"
              type="date"
              placeholder="选择投产日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              @blur="validateOnBlur('productionDate')"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="验收日期" prop="acceptanceDate" data-prop="acceptanceDate">
            <el-date-picker
              v-model="form.acceptanceDate"
              type="date"
              placeholder="选择验收日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              @blur="validateOnBlur('acceptanceDate')"
            />
          </el-form-item>
        </el-col>
      </el-row>
        </div>
      </el-card>

      <!-- 五、成本预算 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="6">
        <template #header>
          <div @click="togglePanel('6')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('6') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">五、成本预算</span>
          </div>
        </template>
        <div v-show="activeNames.includes('6')">

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="项目费用(元)" prop="projectCost" data-prop="projectCost">
            <el-input-number v-model="form.projectCost" :min="0" :precision="2" placeholder="请输入项目费用" style="width: 100%" @blur="validateOnBlur('projectCost')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="费用预算(元)" prop="expenseBudget" data-prop="expenseBudget">
            <el-input-number v-model="form.expenseBudget" :min="0" :precision="2" placeholder="请输入费用预算" style="width: 100%" @blur="validateOnBlur('expenseBudget')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="成本预算(元)" prop="costBudget" data-prop="costBudget">
            <el-input-number v-model="form.costBudget" :min="0" :precision="2" placeholder="请输入成本预算" style="width: 100%" @blur="validateOnBlur('costBudget')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="人力费用(元)" prop="laborCost" data-prop="laborCost">
            <el-input-number v-model="form.laborCost" :min="0" :precision="2" placeholder="请输入人力费用" style="width: 100%" @blur="validateOnBlur('laborCost')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="采购成本(元)" prop="purchaseCost" data-prop="purchaseCost">
            <el-input-number v-model="form.purchaseCost" :min="0" :precision="2" placeholder="请输入采购成本" style="width: 100%" @blur="validateOnBlur('purchaseCost')" />
          </el-form-item>
        </el-col>
      </el-row>
        </div>
      </el-card>

      <!-- 六、备注 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="7">
        <template #header>
          <div @click="togglePanel('7')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('7') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">六、备注</span>
          </div>
        </template>
        <div v-show="activeNames.includes('7')">

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="备注" prop="remark" data-prop="remark">
            <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" @blur="validateOnBlur('remark')" />
          </el-form-item>
        </el-col>
      </el-row>
        </div>
      </el-card>
    </el-form>

    <!-- 底部操作按钮 -->
    <div class="form-footer">
      <el-button type="primary" size="large" @click="submitForm">提交</el-button>
      <el-button size="large" @click="resetForm">重置</el-button>
      <el-button size="large" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<style scoped>
.app-container {
  padding-bottom: 80px;
}

/* 禁用 label 点击触发表单控件的默认行为 */
:deep(.el-form-item__label) {
  pointer-events: none;
}

/* 底部按钮 */
.form-footer {
  position: sticky;
  bottom: 0;
  padding: 20px;
  background-color: #fff;
  border-top: 1px solid #dcdfe6;
  text-align: center;
  z-index: 10;
}

.form-footer .el-button {
  min-width: 120px;
  margin: 0 10px;
}
</style>

<style scoped src="@/assets/styles/form-validation.scss"></style>

<script setup name="ProjectApply">
import { addProject } from "@/api/project/project"
import request from '@/utils/request'
import { useFormValidation } from '@/composables/useFormValidation'

const { proxy } = getCurrentInstance()
const router = useRouter()
const { industry, sys_xmfl, sys_ndgl, sys_yjqy, sys_xmjd, sys_yszt } = proxy.useDict('industry', 'sys_xmfl', 'sys_ndgl', 'sys_yjqy', 'sys_xmjd', 'sys_yszt')

const formRef = ref()
const activeNames = ref(['1', '3', '4', '5', '6', '7']) // 默认展开所有折叠面板
const allPanelNames = ['1', '3', '4', '5', '6', '7'] // 所有面板的name

// 使用表单验证增强
const { validateOnBlur, validateAndScroll } = useFormValidation(formRef, activeNames)
const form = ref({
  industry: null,
  region: null,
  regionCode: null,
  regionId: null,
  shortName: null,
  establishedYear: null,
  projectCode: null,
  projectName: null,
  projectCategory: null,
  projectDept: null,
  projectStage: null,
  acceptanceStatus: null,
  estimatedWorkload: null,
  actualWorkload: null,
  projectBudget: null,
  projectAddress: null,
  projectPlan: null,
  projectDescription: null,
  projectManagerId: null,
  marketManagerId: null,
  participants: null,
  salesManagerId: null,
  salesContact: null,
  customerId: null,
  customerContactId: null,
  merchantPhone: null,
  merchantContact: null,
  startDate: null,
  endDate: null,
  productionDate: null,
  acceptanceDate: null,
  projectCost: null,
  expenseBudget: null,
  costBudget: null,
  laborCost: null,
  purchaseCost: null,
  remark: null
})

const rules = ref({
  industry: [{ required: true, message: "行业不能为空", trigger: "change" }],
  region: [{ required: true, message: "一级区域不能为空", trigger: "change" }],
  regionCode: [{ required: true, message: "二级区域不能为空", trigger: "change" }],
  establishedYear: [{ required: true, message: "立项年度不能为空", trigger: "change" }],
  projectName: [{ required: true, message: "项目名称不能为空", trigger: "blur" }],
  projectCategory: [{ required: true, message: "项目分类不能为空", trigger: "change" }],
  projectDept: [{ required: true, message: "项目部门不能为空", trigger: "change" }],
  projectStage: [{ required: true, message: "项目阶段不能为空", trigger: "change" }],
  acceptanceStatus: [{ required: true, message: "验收状态不能为空", trigger: "change" }],
  estimatedWorkload: [{ required: true, message: "预估工作量不能为空", trigger: "blur" }],
  projectBudget: [{ required: true, message: "项目预算不能为空", trigger: "blur" }],
  projectAddress: [{ required: true, message: "项目地址不能为空", trigger: "blur" }],
  projectPlan: [{ required: true, message: "项目计划不能为空", trigger: "blur" }],
  projectDescription: [{ required: true, message: "项目描述不能为空", trigger: "blur" }],
  projectManagerId: [{ required: true, message: "项目经理不能为空", trigger: "change" }],
  marketManagerId: [{ required: true, message: "市场经理不能为空", trigger: "change" }],
  participants: [{ required: true, message: "参与人员不能为空", trigger: "change" }],
  salesManagerId: [{ required: true, message: "销售负责人不能为空", trigger: "change" }],
  salesContact: [{ required: true, message: "销售联系方式不能为空", trigger: "blur" }],
  customerId: [{ required: true, message: "客户名称不能为空", trigger: "change" }],
  customerContactId: [{ required: true, message: "客户联系人不能为空", trigger: "change" }]
})

const secondaryRegionOptions = ref([])
const projectManagerOptions = ref([])
const marketManagerOptions = ref([])
const salesManagerOptions = ref([])
const allUserOptions = ref([])
const customerOptions = ref([])
const contactOptions = ref([])
const participantIds = ref([])
const customerContactPhone = ref('')

// 监听参与人员变化，同步到表单
watch(participantIds, (newVal) => {
  form.value.participants = newVal.join(',')
})

/** 过滤部门树，只保留三级及以下机构 */
/** 获取项目经理列表 */
function getProjectManagers() {
  request({
    url: '/system/user/listByPost',
    method: 'get',
    params: { postCode: 'pm' }
  }).then(response => {
    projectManagerOptions.value = response.data || []
  })
}

/** 获取市场经理列表 */
function getMarketManagers() {
  request({
    url: '/system/user/listByPost',
    method: 'get',
    params: { postCode: 'scjl' }
  }).then(response => {
    marketManagerOptions.value = response.data || []
  })
}

/** 获取销售负责人列表 */
function getSalesManagers() {
  request({
    url: '/system/user/listByPost',
    method: 'get',
    params: { postCode: 'xsfzr' }
  }).then(response => {
    salesManagerOptions.value = response.data || []
  })
}

/** 获取所有用户列表 */
function getAllUsers() {
  request({
    url: '/system/user/list',
    method: 'get',
    params: { pageNum: 1, pageSize: 1000 }
  }).then(response => {
    allUserOptions.value = response.rows || []
  })
}

/** 获取客户列表 */
function getCustomers() {
  request({
    url: '/project/customer/list',
    method: 'get',
    params: { pageNum: 1, pageSize: 1000 }
  }).then(response => {
    customerOptions.value = response.rows || []
  })
}

/** 获取二级区域列表 */
function getSecondaryRegions(regionDictValue) {
  if (!regionDictValue) {
    secondaryRegionOptions.value = []
    return
  }
  request({
    url: '/project/secondaryRegion/listByRegion',
    method: 'get',
    params: { regionDictValue: regionDictValue }
  }).then(response => {
    secondaryRegionOptions.value = response.data || []
  })
}

/** 一级区域变化处理 */
function handleRegionChange(value) {
  form.value.regionCode = null
  form.value.regionId = null
  getSecondaryRegions(value)
  generateProjectCode()
}

/** 二级区域变化处理 */
function handleSecondaryRegionChange(regionCode) {
  if (!regionCode) {
    form.value.regionId = null
    generateProjectCode()
    return
  }
  // 根据选中的regionCode找到对应的regionId
  const selectedRegion = secondaryRegionOptions.value.find(item => item.regionCode === regionCode)
  if (selectedRegion) {
    form.value.regionId = selectedRegion.regionId
  }
  generateProjectCode()
}

/** 销售负责人变化处理 */
function handleSalesManagerChange(userId) {
  if (!userId) {
    form.value.salesContact = null
    return
  }
  const user = salesManagerOptions.value.find(u => u.userId === userId)
  if (user) {
    form.value.salesContact = user.phonenumber || ''
    // 自动填充后立即触发验证，清除错误提示
    nextTick(() => {
      validateOnBlur('salesContact')
    })
  }
}

/** 客户变化处理 */
function handleCustomerChange(customerId) {
  form.value.customerContactId = null
  customerContactPhone.value = ''
  if (!customerId) {
    contactOptions.value = []
    return
  }
  request({
    url: '/project/customer/contact/listByCustomer',
    method: 'get',
    params: { customerId: customerId }
  }).then(response => {
    contactOptions.value = response.data || []
  })
}

/** 客户联系人变化处理 */
function handleContactChange(contactId) {
  if (!contactId) {
    customerContactPhone.value = ''
    return
  }
  const contact = contactOptions.value.find(c => c.contactId === contactId)
  if (contact) {
    customerContactPhone.value = contact.contactPhone || ''
    // 注意：customerContactPhone 不是表单字段，不需要验证
  }
}

/** 生成项目编号 */
function generateProjectCode() {
  const { industry, region, regionCode, shortName, establishedYear } = form.value
  if (industry && region && regionCode && shortName && establishedYear) {
    form.value.projectCode = `${industry}-${region}-${regionCode}-${shortName}-${establishedYear}`
  }
}

/** 提交表单 */
function submitForm() {
  validateAndScroll(() => {
    // 验证通过后的提交逻辑
    const submitData = {
      ...form.value,
      establishedYear: parseInt(form.value.establishedYear)  // 转换为整数
    }

    addProject(submitData).then(response => {
      proxy.$modal.msgSuccess("提交成功")
      router.push('/project/list')
    })
  })
}

/** 重置表单 */
function resetForm() {
  formRef.value.resetFields()
  participantIds.value = []
  customerContactPhone.value = ''
  secondaryRegionOptions.value = []
  contactOptions.value = []
}

/** 取消 */
function cancel() {
  router.push('/project/project')
}

/** 全部展开 */
function expandAll() {
  activeNames.value = [...allPanelNames]
}

/** 全部折叠 */
function collapseAll() {
  activeNames.value = []
}

/** 切换面板展开/折叠 */
function togglePanel(name) {
  const index = activeNames.value.indexOf(name)
  if (index > -1) {
    activeNames.value.splice(index, 1)
  } else {
    activeNames.value.push(name)
  }
}

// 初始化数据
onMounted(() => {
  getProjectManagers()
  getMarketManagers()
  getSalesManagers()
  getAllUsers()
  getCustomers()
})
</script>

<style scoped>
.app-container {
  padding-bottom: 80px;
}
</style>
