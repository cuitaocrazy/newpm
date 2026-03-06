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
            <dict-select
              v-model="form.region"
              dict-type="sys_yjqy"
              placeholder="请选择一级区域"
              @change="handleRegionChange"
              @blur="validateOnBlur('region')"
            />
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
            <dict-select
              v-model="form.establishedYear"
              dict-type="sys_ndgl"
              placeholder="请选择立项年度"
              @change="generateProjectCode"
              @blur="validateOnBlur('establishedYear')"
            />
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
            <dict-select
              v-model="form.projectCategory"
              dict-type="sys_xmfl"
              placeholder="请选择项目分类"
              @blur="validateOnBlur('projectCategory')"
            />
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
          <el-form-item label="项目状态" prop="projectStatus" data-prop="projectStatus">
            <dict-select
              v-model="form.projectStatus"
              dict-type="sys_xmzt"
              placeholder="请选择项目状态"
              clearable
              @blur="validateOnBlur('projectStatus')"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="项目阶段" prop="projectStage" data-prop="projectStage">
            <dict-select
              v-model="form.projectStage"
              dict-type="sys_xmjd"
              placeholder="请选择项目阶段"
              @blur="validateOnBlur('projectStage')"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="验收状态" prop="acceptanceStatus" data-prop="acceptanceStatus">
            <dict-select
              v-model="form.acceptanceStatus"
              dict-type="sys_yszt"
              placeholder="请选择验收状态"
              @blur="validateOnBlur('acceptanceStatus')"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="项目预算" prop="projectBudget" data-prop="projectBudget">
            <el-input v-model="form.projectBudget" placeholder="请输入金额"
              @input="handleAmountInput('projectBudget', $event)"
              @blur="handleAmountBlur('projectBudget')">
              <template #append>元</template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="预估工作量" prop="estimatedWorkload" data-prop="estimatedWorkload">
            <el-input v-model="form.estimatedWorkload" placeholder="请输入" @blur="validateOnBlur('estimatedWorkload')">
              <template #append>人天</template>
            </el-input>
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
            <user-select
              v-model="form.projectManagerId"
              post-code="pm"
              placeholder="请选择项目经理"
              filterable
              @blur="validateOnBlur('projectManagerId')"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="市场经理" prop="marketManagerId" data-prop="marketManagerId">
            <user-select
              v-model="form.marketManagerId"
              post-code="scjl"
              placeholder="请选择市场经理"
              filterable
              @blur="validateOnBlur('marketManagerId')"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="销售负责人" prop="salesManagerId" data-prop="salesManagerId">
            <user-select
              v-model="form.salesManagerId"
              post-code="xsfzr"
              placeholder="请选择销售负责人"
              filterable
              @change="handleSalesManagerChange"
              @blur="validateOnBlur('salesManagerId')"
            />
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
            <org-user-select
              v-model="participantIds"
              placeholder="请选择参与人员"
              style="width: 100%"
              :no-data-scope="true"
              @blur="validateOnBlur('participants')"
            />
            <div v-if="participantIds.length > 0" style="margin-top: 6px; color: #606266; font-size: 13px;">已选 {{ participantIds.length }} 人</div>
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
          <el-form-item label="项目费用" prop="projectCost" data-prop="projectCost">
            <el-input v-model="form.projectCost" placeholder="请输入项目费用" style="width: 100%"
              @input="handleAmountInput('projectCost', $event)"
              @blur="handleAmountBlur('projectCost')">
              <template #append>元</template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="费用预算" prop="expenseBudget" data-prop="expenseBudget">
            <el-input v-model="form.expenseBudget" placeholder="请输入费用预算" style="width: 100%"
              @input="handleAmountInput('expenseBudget', $event)"
              @blur="handleAmountBlur('expenseBudget')">
              <template #append>元</template>
            </el-input>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="成本预算" prop="costBudget" data-prop="costBudget">
            <el-input v-model="form.costBudget" placeholder="请输入成本预算" style="width: 100%"
              @input="handleAmountInput('costBudget', $event)"
              @blur="handleAmountBlur('costBudget')">
              <template #append>元</template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="人力费用" prop="laborCost" data-prop="laborCost">
            <el-input v-model="form.laborCost" placeholder="请输入人力费用" style="width: 100%"
              @input="handleAmountInput('laborCost', $event)"
              @blur="handleAmountBlur('laborCost')">
              <template #append>元</template>
            </el-input>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="采购成本" prop="purchaseCost" data-prop="purchaseCost">
            <el-input v-model="form.purchaseCost" placeholder="请输入采购成本" style="width: 100%"
              @input="handleAmountInput('purchaseCost', $event)"
              @blur="handleAmountBlur('purchaseCost')">
              <template #append>元</template>
            </el-input>
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
const { industry, sys_xmfl, sys_ndgl, sys_yjqy, sys_xmjd, sys_yszt, sys_xmzt } = proxy.useDict('industry', 'sys_xmfl', 'sys_ndgl', 'sys_yjqy', 'sys_xmjd', 'sys_yszt', 'sys_xmzt')

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
  projectStatus: null,
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
  projectStatus: [{ required: true, message: "项目状态不能为空", trigger: "change" }],
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
const customerOptions = ref([])
const contactOptions = ref([])
const participantIds = ref([])
const customerContactPhone = ref('')

// 监听参与人员变化，同步到表单
watch(participantIds, (newVal) => {
  form.value.participants = newVal.join(',')
})

/** 过滤部门树，只保留三级及以下机构 */
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
function handleSalesManagerChange(userId, user) {
  if (!userId) {
    form.value.salesContact = null
    return
  }
  // user 参数由 UserSelect 组件的 change 事件提供
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

/** 金额输入处理 - 仅允许数字和小数点 */
function handleAmountInput(field, value) {
  let cleaned = String(value).replace(/[^\d.]/g, '')
  // 只保留第一个小数点
  const dotIndex = cleaned.indexOf('.')
  if (dotIndex !== -1) {
    cleaned = cleaned.substring(0, dotIndex + 1) + cleaned.substring(dotIndex + 1).replace(/\./g, '')
  }
  // 小数位不超过2位
  const parts = cleaned.split('.')
  if (parts.length === 2 && parts[1].length > 2) {
    cleaned = parts[0] + '.' + parts[1].substring(0, 2)
  }
  form.value[field] = cleaned
}

/** 失焦时格式化为千分位显示 */
function finalizeAmountFormat(field) {
  const value = form.value[field]
  if (!value && value !== 0) return
  const str = String(value).replace(/,/g, '')
  const num = parseFloat(str)
  if (isNaN(num)) return
  const parts = str.split('.')
  const intPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  const decPart = parts.length === 2 ? parts[1].padEnd(2, '0').substring(0, 2) : '00'
  form.value[field] = `${intPart}.${decPart}`
}

/** 金额字段失焦 - 格式化 + 验证 */
function handleAmountBlur(field) {
  finalizeAmountFormat(field)
  validateOnBlur(field)
}

/** 解析金额字符串为数字（提交前用） */
function parseAmount(value) {
  if (value === null || value === undefined || value === '') return null
  const num = parseFloat(String(value).replace(/,/g, ''))
  return isNaN(num) ? null : num
}

/** 提交表单 */
function submitForm() {
  validateAndScroll(() => {
    // 验证通过后的提交逻辑
    const submitData = {
      ...form.value,
      projectBudget: parseAmount(form.value.projectBudget),
      projectCost: parseAmount(form.value.projectCost),
      expenseBudget: parseAmount(form.value.expenseBudget),
      costBudget: parseAmount(form.value.costBudget),
      laborCost: parseAmount(form.value.laborCost),
      purchaseCost: parseAmount(form.value.purchaseCost)
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
  router.push('/project/list')
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
  getCustomers()
})
</script>

<style scoped>
.app-container {
  padding-bottom: 80px;
}
</style>
