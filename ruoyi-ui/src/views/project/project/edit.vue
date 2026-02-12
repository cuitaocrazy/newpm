<template>
  <div class="app-container project-edit">
    <!-- 页面标题 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <h2>编辑项目</h2>
        <p class="tips">修改项目信息，保存后将更新项目数据</p>
      </div>
    </el-card>

    <!-- 已拒绝状态提示 -->
    <el-alert
      v-if="form.approvalStatus === '2'"
      title="审核已拒绝"
      type="warning"
      :closable="false"
      show-icon
      class="mb-4"
      style="margin-top: 20px;"
    >
      <template #default>
        <p style="margin: 0;"><strong>拒绝原因：</strong>{{ form.approvalReason || '无' }}</p>
        <p style="margin: 8px 0 0 0;">请根据审核意见修改后重新提交</p>
      </template>
    </el-alert>

    <!-- 表单主体 -->
    <el-form ref="editFormRef" :model="form" :rules="rules" label-width="140px" v-loading="loading">
      <el-collapse v-model="activeNames" class="apply-collapse">
        
        <!-- 面板1：项目基本信息 -->
        <el-collapse-item name="1" title="一、项目基本信息">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目编号">
                <el-input v-model="form.projectCode" disabled
                  style="background-color: #f5f7fa;" />
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
              <el-form-item label="行业" prop="industry" data-prop="industry">
                <dict-select
                  v-model="form.industry"
                  dict-type="industry"
                  placeholder="请选择行业"
                  clearable
                  @blur="validateOnBlur('industry')"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="一级区域" prop="region" data-prop="region">
                <dict-select
                  v-model="form.region"
                  dict-type="sys_yjqy"
                  placeholder="请选择一级区域"
                  clearable
                  @change="handleRegionChange"
                  @blur="validateOnBlur('region')"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="二级区域" prop="regionCode" data-prop="regionCode">
                <el-select
                  v-model="form.regionCode"
                  placeholder="请选择二级区域"
                  clearable
                  :disabled="!form.region"
                  @change="handleSecondaryRegionChange"
                  @blur="validateOnBlur('regionCode')"
                >
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
              <el-form-item label="项目简称" prop="shortName" data-prop="shortName">
                <el-input v-model="form.shortName" placeholder="请输入项目简称" @blur="validateOnBlur('shortName')" />
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
                  clearable
                  @change="generateProjectCode"
                  @blur="validateOnBlur('establishedYear')"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目编号" prop="projectCode" data-prop="projectCode">
                <el-input v-model="form.projectCode" placeholder="自动生成" disabled />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="项目名称" prop="projectName" data-prop="projectName">
                <el-input v-model="form.projectName" placeholder="请输入项目名称" @blur="validateOnBlur('projectName')" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目分类" prop="projectCategory" data-prop="projectCategory">
                <dict-select
                  v-model="form.projectCategory"
                  dict-type="sys_xmfl"
                  placeholder="请选择项目分类"
                  clearable
                  @blur="validateOnBlur('projectCategory')"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目部门" prop="projectDept" data-prop="projectDept">
                <project-dept-select
                  v-model="form.projectDept"
                  @blur="validateOnBlur('projectDept')"
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
                  clearable
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
                  clearable
                  @blur="validateOnBlur('acceptanceStatus')"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="预估工作量" prop="estimatedWorkload" data-prop="estimatedWorkload">
                <el-input v-model="form.estimatedWorkload" placeholder="请输入预估工作量" @blur="validateOnBlur('estimatedWorkload')">
                  <template #append>人天</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="实际工作量" prop="actualWorkload" data-prop="actualWorkload">
                <el-input v-model="form.actualWorkload" placeholder="请输入实际工作量" @blur="validateOnBlur('actualWorkload')">
                  <template #append>人天</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目地址" prop="projectAddress" data-prop="projectAddress">
                <el-input v-model="form.projectAddress" placeholder="请输入项目地址"  @blur="validateOnBlur('projectAddress')" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="项目计划" prop="projectPlan" data-prop="projectPlan">
                <el-input v-model="form.projectPlan" type="textarea" :rows="3"
                  placeholder="请输入项目计划" @blur="validateOnBlur('projectPlan')" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="项目描述" prop="projectDescription" data-prop="projectDescription">
                <el-input v-model="form.projectDescription" type="textarea" :rows="3"
                  placeholder="请输入项目描述" @blur="validateOnBlur('projectDescription')" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="审核状态">
                <dict-tag :options="sys_spzt" :value="form.approvalStatus" v-if="form.approvalStatus" />
                <span v-else>-</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="审核意见">
                <el-input v-model="form.approvalReason" disabled
                  placeholder="暂无审核意见" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板2：人员配置 -->
        <el-collapse-item name="2" title="二、人员配置">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目经理" prop="projectManagerId" data-prop="projectManagerId">
                <user-select
                  v-model="form.projectManagerId"
                  post-code="pm"
                  placeholder="请选择项目经理"
                  filterable
                  clearable
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
                  clearable
                  @blur="validateOnBlur('marketManagerId')"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="参与人员" prop="participants" data-prop="participants">
                <!-- 已选人员标签展示 -->
                <div class="selected-participants" v-if="selectedParticipants.length > 0" style="margin-bottom: 10px;">
                  <el-tag v-for="user in selectedParticipants" :key="user.userId"
                    closable @close="removeParticipant(user.userId)"
                    type="info" class="participant-tag" style="margin-right: 8px; margin-bottom: 8px;">
                    {{ user.nickName }}
                  </el-tag>
                </div>

                <!-- 多选下拉框 -->
                <el-select
                  v-model="form.participants"
                  placeholder="请选择参与人员（可多选）"
                  multiple
                  filterable
                  clearable
                  collapse-tags
                  collapse-tags-tooltip
                  :max-collapse-tags="3"
                  @change="handleParticipantsChange"
                  style="width: 100%">
                  <el-option v-for="user in allUsers" :key="user.userId"
                    :label="user.nickName"
                    :value="user.userId">
                    <span>{{ user.nickName }}</span>
                    <span style="color: #8492a6; font-size: 13px; margin-left: 8px;">（{{ user.userName }}）</span>
                  </el-option>
                </el-select>
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
                  clearable
                  @change="handleSalesManagerChange"
                  @blur="validateOnBlur('salesManagerId')"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="销售联系方式" prop="salesContact" data-prop="salesContact">
                <el-input v-model="form.salesContact" placeholder="选择销售负责人后自动显示"                  readonly disabled />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板3：客户信息 -->
        <el-collapse-item name="3" title="三、客户信息">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="客户名称" prop="customerId" data-prop="customerId">
                <el-select v-model="form.customerId" placeholder="请选择客户"                  filterable clearable @change="handleCustomerChange" @blur="validateOnBlur('customerId')">
                  <el-option v-for="customer in customerOptions" :key="customer.customerId"
                    :label="customer.customerSimpleName" :value="customer.customerId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="客户联系人" prop="customerContactId" data-prop="customerContactId">
                <el-select v-model="form.customerContactId" placeholder="请选择客户联系人"                  filterable clearable :disabled="!form.customerId" @change="handleContactChange" @blur="validateOnBlur('customerContactId')">
                  <el-option v-for="contact in contactOptions" :key="contact.contactId"
                    :label="contact.contactName" :value="contact.contactId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="客户联系方式">
                <el-input v-model="customerContactPhone" placeholder="自动带出" readonly />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="商户联系人">
                <el-input v-model="form.merchantContact" placeholder="请输入商户联系人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="商户联系方式">
                <el-input v-model="form.merchantPhone" placeholder="请输入商户联系方式" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板4：时间规划 -->
        <el-collapse-item name="4" title="四、时间规划">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="启动日期">
                <el-date-picker v-model="form.startDate" type="date" 
                  placeholder="请选择启动日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束日期">
                <el-date-picker v-model="form.endDate" type="date" 
                  placeholder="请选择结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="投产日期">
                <el-date-picker v-model="form.productionDate" type="date" 
                  placeholder="请选择投产日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="验收日期">
                <el-date-picker v-model="form.acceptanceDate" type="date" 
                  placeholder="请选择验收日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板5：成本预算 -->
        <el-collapse-item name="5" title="五、成本预算">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目预算" prop="projectBudget" data-prop="projectBudget">
                <el-input v-model="form.projectBudget" placeholder="请输入项目预算" @blur="validateOnBlur('projectBudget')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目费用">
                <el-input v-model="form.projectCost" placeholder="请输入项目费用">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="费用预算">
                <el-input v-model="form.costBudget" placeholder="请输入费用预算">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="成本预算">
                <el-input v-model="form.budgetCost" placeholder="请输入成本预算">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="人力费用">
                <el-input v-model="form.laborCost" placeholder="请输入人力费用">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="采购成本">
                <el-input v-model="form.purchaseCost" placeholder="请输入采购成本">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板6：备注 -->
        <el-collapse-item name="6" title="六、备注">
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="4"
                  placeholder="请输入备注信息" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

      </el-collapse>
    </el-form>

    <!-- 底部操作按钮 -->
    <div class="form-footer">
      <el-button
        type="primary"
        size="large"
        :icon="submitButtonIcon"
        @click="submitForm"
        :loading="submitLoading"
      >
        {{ submitButtonText }}
      </el-button>
      <el-button size="large" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script setup name="ProjectApply">
import { ref, reactive, computed, toRefs, watch, getCurrentInstance, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject, updateProject } from '@/api/project/project'
import { listUser, listUserByPost } from '@/api/system/user'
import { listDept } from '@/api/system/dept'
import request from '@/utils/request'
import { useFormValidation } from '@/composables/useFormValidation'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const { proxy } = getCurrentInstance()
const { sys_xmfl, sys_xmjd, sys_yszt, sys_shzt, sys_qrzt, industry, sys_yjqy, sys_jdgl, sys_spzt, sys_ndgl } =
  proxy.useDict('sys_xmfl', 'sys_xmjd', 'sys_yszt', 'sys_shzt', 'sys_qrzt', 'industry', 'sys_yjqy', 'sys_jdgl', 'sys_spzt', 'sys_ndgl')

// 折叠面板激活状态
const activeNames = ref(['1', '2', '3', '4', '5', '6'])

// 表单引用
const editFormRef = ref()

// 使用表单验证增强
const { validateOnBlur, validateAndScroll } = useFormValidation(editFormRef, activeNames)

// 动态按钮文字和图标
const submitButtonText = computed(() => {
  return form.value.approvalStatus === '2' ? '重新提交审核' : '保存'
})

const submitButtonIcon = computed(() => {
  return form.value.approvalStatus === '2' ? 'RefreshRight' : 'Select'
})

// 表单数据
const data = reactive({
  form: {
    industry: '',
    region: '',
    regionCode: '',
    regionId: null,
    shortName: '',
    establishedYear: null,
    projectCode: '',
    projectName: '',
    projectCategory: '',
    projectDept: '',
    projectStage: '',
    acceptanceStatus: '',
    estimatedWorkload: '',
    projectPlan: '',
    projectDescription: '',
    projectAddress: '',
    projectManagerId: '',
    marketManagerId: '',
    participants: [],
    salesManagerId: '',
    salesContact: '',
    customerId: '',
    customerContactId: '',
    customerPhone: '',
    merchantContact: '',
    merchantPhone: '',
    startDate: '',
    endDate: '',
    productionDate: '',
    acceptanceDate: '',
    implementationYear: '',
    projectCost: '',
    projectBudget: '',
    costBudget: '',
    budgetCost: '',
    laborCost: '',
    purchaseCost: '',
    remark: '',
    approvalStatus: '0'
  },
  rules: {
    industry: [{ required: true, message: '请选择行业', trigger: 'change' }],
    region: [{ required: true, message: '请选择一级区域', trigger: 'change' }],
    regionCode: [{ required: true, message: '请选择二级区域', trigger: 'change' }],
    shortName: [{ required: true, message: '请输入项目简称', trigger: 'blur' }],
    establishedYear: [{ required: true, message: '请选择立项年度', trigger: 'change' }],
    projectCode: [{ required: true, message: '项目编号不能为空', trigger: 'blur' }],
    projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
    projectCategory: [{ required: true, message: '请选择项目分类', trigger: 'change' }],
    projectDept: [{ required: true, message: '请选择项目部门', trigger: 'change' }],
    projectStage: [{ required: true, message: '请选择项目阶段', trigger: 'change' }],
    acceptanceStatus: [{ required: true, message: '请选择验收状态', trigger: 'change' }],
    estimatedWorkload: [
      { required: true, message: '请输入预估工作量', trigger: 'blur' },
      { pattern: /^\d+(\.\d+)?$/, message: '请输入有效的数字', trigger: 'blur' }
    ],
    projectPlan: [{ required: true, message: '请输入项目计划', trigger: 'blur' }],
    projectDescription: [{ required: true, message: '请输入项目描述', trigger: 'blur' }],
    projectAddress: [{ required: true, message: '请输入项目地址', trigger: 'blur' }],
    projectManagerId: [{ required: true, message: '请选择项目经理', trigger: 'change' }],
    marketManagerId: [{ required: true, message: '请选择市场经理', trigger: 'change' }],
    participants: [
      { required: true, message: '请选择参与人员', trigger: 'change' },
      { type: 'array', min: 1, message: '至少选择一名参与人员', trigger: 'change' }
    ],
    salesManagerId: [{ required: true, message: '请选择销售负责人', trigger: 'change' }],
    salesContact: [{ required: true, message: '请输入销售联系方式', trigger: 'blur' }],
    customerId: [{ required: true, message: '请输入客户ID', trigger: 'blur' }],
    customerContactId: [{ required: true, message: '请输入客户联系人ID', trigger: 'blur' }],
    projectBudget: [
      { required: true, message: '请输入项目预算', trigger: 'blur' },
      { pattern: /^\d+(\.\d+)?$/, message: '请输入有效的数字', trigger: 'blur' }
    ]
  }
})

const { form, rules } = toRefs(data)

// 其他数据
const submitLoading = ref(false)
const contractInfo = ref({})
const secondaryRegionOptions = ref([])
const allUsers = ref([])
const selectedParticipants = ref([])
const participantInput = ref('')
const participantAutocomplete = ref(null)
const customerOptions = ref([])
const contactOptions = ref([])
const customerContactPhone = ref('')

// 项目编号自动生成
watch([() => form.value.industry, () => form.value.region,
       () => form.value.regionCode, () => form.value.shortName, () => form.value.establishedYear],
  ([industry, region, regionCode, shortName, establishedYear]) => {
    if (industry && region && regionCode && shortName && establishedYear) {
      form.value.projectCode = `${industry}-${region}-${regionCode}-${shortName}-${establishedYear}`
    }
  }
)

// 监听区域变化，重新加载客户列表
watch(() => form.value.region, (newRegion) => {
  if (newRegion) {
    loadCustomers()
  }
})

// 处理销售负责人变化，自动填充手机号
function handleSalesManagerChange(userId, user) {
  if (user) {
    form.value.salesContact = user.phonenumber || ''
    // 自动填充后立即触发验证，清除错误提示
    nextTick(() => {
      validateOnBlur('salesContact')
    })
  } else {
    form.value.salesContact = ''
  }
}

/** 过滤部门树，只保留三级及以下机构 */
// 加载所有用户（用于参与人员选择）
function loadAllUsers() {
  listUser({}).then(response => {
    allUsers.value = response.rows || []
  })
}

// 加载客户列表（根据区域过滤）
function loadCustomers() {
  const params = {}
  // 如果已选择区域，则按区域过滤客户
  if (form.value.region) {
    params.region = form.value.region
  }
  request({
    url: '/project/customer/list',
    method: 'get',
    params: params
  }).then(response => {
    customerOptions.value = response.rows || []
  })
}

// 客户变化时加载联系人
function handleCustomerChange(customerId) {
  // 清空联系人和联系方式
  form.value.customerContactId = null
  customerContactPhone.value = ''
  contactOptions.value = []

  if (!customerId) return

  // 加载该客户的联系人列表
  request({
    url: '/project/customer/contact/listByCustomer',
    method: 'get',
    params: { customerId: customerId }
  }).then(response => {
    contactOptions.value = response.data || []
  })
}

// 联系人变化时自动填充联系方式
function handleContactChange(contactId) {
  if (!contactId) {
    customerContactPhone.value = ''
    return
  }

  // 从联系人列表中找到对应的联系人
  const contact = contactOptions.value.find(c => c.contactId === contactId)
  if (contact) {
    customerContactPhone.value = contact.contactPhone || ''
  }
}

// 参与人员变化时同步更新已选人员列表
function handleParticipantsChange(userIds) {
  selectedParticipants.value = allUsers.value.filter(user => userIds.includes(user.userId))
}

// 删除参与人员（从标签删除）
function removeParticipant(userId) {
  form.value.participants = form.value.participants.filter(id => id !== userId)
  selectedParticipants.value = selectedParticipants.value.filter(p => p.userId !== userId)
}

// 提交表单
function submitForm() {
  validateAndScroll(() => {
    proxy.$modal.confirm('确认保存项目信息？').then(() => {
      submitLoading.value = true
      // 深拷贝表单数据，避免修改原始数据
      const submitData = { ...form.value }
      // 将参与人员数组转换为逗号分隔的字符串
      if (Array.isArray(submitData.participants)) {
        submitData.participants = submitData.participants.join(',')
      }
      // 转换 establishedYear 为整数
      if (submitData.establishedYear) {
        submitData.establishedYear = parseInt(submitData.establishedYear)
      }

      updateProject(submitData).then(response => {
        proxy.$modal.msgSuccess('保存成功')
        router.push('/project/list')
      }).finally(() => {
        submitLoading.value = false
      })
    })
  })
}

// 取消
function cancel() {
  proxy.$modal.confirm('确认取消编辑？未保存的数据将丢失').then(() => {
    router.push('/project/list')
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
}

/** 二级区域变化处理 */
function handleSecondaryRegionChange(regionCode) {
  if (!regionCode) {
    form.value.regionId = null
    return
  }
  // 根据选中的regionCode找到对应的regionId
  const selectedRegion = secondaryRegionOptions.value.find(item => item.regionCode === regionCode)
  if (selectedRegion) {
    form.value.regionId = selectedRegion.regionId
  }
}

// 加载项目数据
function loadProjectData() {
  const projectId = route.params.projectId
  if (!projectId) {
    proxy.$modal.msgError('缺少项目ID参数')
    router.push('/project/project')
    return
  }

  loading.value = true
  getProject(projectId).then(response => {
    const data = response.data
    // 转换参与人员格式：字符串 → 数组
    if (data.participants) {
      data.participants = data.participants.split(',').map(Number)
    }

    // 保存客户联系人ID（因为 handleCustomerChange 会清空它）
    const savedCustomerContactId = data.customerContactId

    // 填充表单
    Object.assign(form.value, data)

    // 如果有一级区域，加载对应的二级区域列表
    if (data.region) {
      getSecondaryRegions(data.region)
    }

    // 如果有客户ID，加载对应的联系人列表
    if (data.customerId) {
      // 加载联系人列表
      request({
        url: '/project/customer/contact/listByCustomer',
        method: 'get',
        params: { customerId: data.customerId }
      }).then(res => {
        contactOptions.value = res.data || []
        // 恢复客户联系人ID
        if (savedCustomerContactId) {
          form.value.customerContactId = savedCustomerContactId
          // 触发联系人变化，显示联系方式
          handleContactChange(savedCustomerContactId)
        }
      })
    }
  }).catch(() => {
    proxy.$modal.msgError('加载项目数据失败')
    router.push('/project/project')
  }).finally(() => {
    loading.value = false
  })
}

// 页面加载时初始化
onMounted(() => {
  loadAllUsers()
  loadCustomers()
  loadProjectData()
})
</script>

<style scoped lang="scss">
/* 禁用 label 点击触发表单控件的默认行为 */
:deep(.el-form-item__label) {
  pointer-events: none;
}

.project-edit {
  .page-header {
    margin-bottom: 20px;

    .header-content {
      h2 {
        margin: 0 0 8px 0;
        font-size: 20px;
        color: #303133;
      }

      .tips {
        margin: 0;
        font-size: 14px;
        color: #909399;
      }
    }
  }

  .apply-collapse {
    margin-bottom: 20px;

    :deep(.el-collapse-item__header) {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      padding-left: 10px;
      border-left: 3px solid #409eff;
    }

    :deep(.el-collapse-item__content) {
      padding: 20px 15px;
    }
  }

  .form-tip {
    display: block;
    margin-top: 5px;
    font-size: 12px;
    color: #909399;
  }

  // 只读信息区域
  .readonly-section {
    padding: 15px;
    background-color: #f5f7fa;
    border-radius: 4px;
    margin-bottom: 15px;

    .info-item {
      line-height: 32px;

      .label {
        color: #606266;
        font-weight: 500;
      }

      .value {
        color: #303133;
      }
    }
  }

  // 参与人员标签
  .selected-participants {
    margin-bottom: 10px;

    .participant-tag {
      margin-right: 8px;
      margin-bottom: 8px;
    }
  }

  .autocomplete-item {
    display: flex;
    justify-content: space-between;

    .name {
      font-weight: 500;
    }

    .username {
      color: #909399;
      font-size: 12px;
    }
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
      margin: 0 10px;
    }
  }
}
</style>

<style scoped src="@/assets/styles/form-validation.scss"></style>
