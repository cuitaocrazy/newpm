<template>
  <div class="app-container project-edit">
    <!-- 页面标题 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <h2>编辑项目</h2>
        <p class="tips">修改项目信息，保存后将更新项目数据</p>
      </div>
    </el-card>

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
                <el-select v-model="form.industry" placeholder="请选择行业" clearable @blur="validateOnBlur('industry')">
                  <el-option v-for="dict in industry" :key="dict.value"
                    :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="一级区域" prop="region" data-prop="region">
                <el-select v-model="form.region" placeholder="请选择一级区域" clearable @change="handleRegionChange" @blur="validateOnBlur('region')">
                  <el-option v-for="dict in sys_yjqy" :key="dict.value"
                    :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="二级区域" prop="provinceCode" data-prop="provinceCode">
                <el-select v-model="form.provinceCode" placeholder="请选择二级区域" clearable                  :disabled="!form.region" @change="handleSecondaryRegionChange" @blur="validateOnBlur('provinceCode')">
                  <el-option
                    v-for="item in secondaryRegionOptions"
                    :key="item.provinceCode"
                    :label="item.provinceName"
                    :value="item.provinceCode"
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
              <el-form-item label="立项年份" prop="establishedYear" data-prop="establishedYear">
                <el-select v-model="form.establishedYear" placeholder="请选择立项年份" clearable @blur="validateOnBlur('establishedYear')">
                  <el-option
                    v-for="year in yearOptions"
                    :key="year"
                    :label="year"
                    :value="year"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目编号" prop="projectCode" data-prop="projectCode">
                <el-input v-model="form.projectCode" placeholder="自动生成" disabled />
                <div style="color: #909399; font-size: 12px; margin-top: 5px;">
                  格式：{行业代码}-{一级区域代码}-{二级区域代码}-{简称}-{年份}
                </div>
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
                <el-select v-model="form.projectCategory" placeholder="请选择项目分类" clearable @blur="validateOnBlur('projectCategory')">
                  <el-option v-for="dict in sys_xmfl" :key="dict.value" 
                    :label="dict.label" :value="dict.value" />
                </el-select>
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
              <el-form-item label="项目状态" prop="projectStatus" data-prop="projectStatus">
                <el-select v-model="form.projectStatus" placeholder="请选择项目状态" clearable @blur="validateOnBlur('projectStatus')">
                  <el-option v-for="dict in sys_xmjd" :key="dict.value" 
                    :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="验收状态" prop="acceptanceStatus" data-prop="acceptanceStatus">
                <el-select v-model="form.acceptanceStatus" placeholder="请选择验收状态" clearable @blur="validateOnBlur('acceptanceStatus')">
                  <el-option v-for="dict in sys_yszt" :key="dict.value" 
                    :label="dict.label" :value="dict.value" />
                </el-select>
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
                <el-select v-model="form.projectManagerId" placeholder="请选择项目经理"                  filterable clearable @blur="validateOnBlur('projectManagerId')">
                  <el-option v-for="user in projectManagers" :key="user.userId" 
                    :label="user.nickName" :value="user.userId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="市场经理" prop="marketManagerId" data-prop="marketManagerId">
                <el-select v-model="form.marketManagerId" placeholder="请选择市场经理"                  filterable clearable @blur="validateOnBlur('marketManagerId')">
                  <el-option v-for="user in marketManagers" :key="user.userId" 
                    :label="user.nickName" :value="user.userId" />
                </el-select>
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
                <el-select v-model="form.salesManagerId" placeholder="请选择销售负责人"                  filterable clearable @blur="validateOnBlur('salesManagerId')">
                  <el-option v-for="user in salesManagers" :key="user.userId" 
                    :label="user.nickName" :value="user.userId" />
                </el-select>
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
      <el-button type="primary" size="large" @click="submitForm" :loading="submitLoading">
        保存
      </el-button>
      <el-button size="large" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script setup name="ProjectApply">
import { ref, reactive, toRefs, watch, getCurrentInstance, onMounted, nextTick } from 'vue'
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
const { sys_xmfl, sys_xmjd, sys_yszt, sys_shzt, sys_qrzt, industry, sys_yjqy, sys_jdgl, sys_spzt } =
  proxy.useDict('sys_xmfl', 'sys_xmjd', 'sys_yszt', 'sys_shzt', 'sys_qrzt', 'industry', 'sys_yjqy', 'sys_jdgl', 'sys_spzt')

// 折叠面板激活状态
const activeNames = ref(['1', '2', '3', '4', '5', '6'])

// 表单引用
const editFormRef = ref()

// 使用表单验证增强
const { validateOnBlur, validateAndScroll } = useFormValidation(editFormRef, activeNames)

// 表单数据
const data = reactive({
  form: {
    industry: '',
    region: '',
    provinceCode: '',
    provinceId: null,
    shortName: '',
    establishedYear: null,
    projectCode: '',
    projectName: '',
    projectCategory: '',
    projectDept: '',
    projectStatus: '',
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
    provinceCode: [{ required: true, message: '请选择二级区域', trigger: 'change' }],
    shortName: [{ required: true, message: '请输入项目简称', trigger: 'blur' }],
    establishedYear: [{ required: true, message: '请选择立项年份', trigger: 'change' }],
    projectCode: [{ required: true, message: '项目编号不能为空', trigger: 'blur' }],
    projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
    projectCategory: [{ required: true, message: '请选择项目分类', trigger: 'change' }],
    projectDept: [{ required: true, message: '请选择项目部门', trigger: 'change' }],
    projectStatus: [{ required: true, message: '请选择项目状态', trigger: 'change' }],
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
const yearOptions = ref([])
const projectManagers = ref([])
const marketManagers = ref([])
const salesManagers = ref([])
const allUsers = ref([])
const selectedParticipants = ref([])
const participantInput = ref('')
const participantAutocomplete = ref(null)
const customerOptions = ref([])
const contactOptions = ref([])
const customerContactPhone = ref('')

// 项目编号自动生成
watch([() => form.value.industry, () => form.value.region,
       () => form.value.provinceCode, () => form.value.shortName, () => form.value.establishedYear],
  ([industry, region, provinceCode, shortName, establishedYear]) => {
    if (industry && region && provinceCode && shortName && establishedYear) {
      form.value.projectCode = `${industry}-${region}-${provinceCode}-${shortName}-${establishedYear}`
    }
  }
)

// 监听销售负责人变化，自动填充手机号
watch(() => form.value.salesManagerId, (userId) => {
  if (userId) {
    const salesManager = salesManagers.value.find(u => u.userId === userId)
    if (salesManager) {
      form.value.salesContact = salesManager.phonenumber || ''
      // 自动填充后立即触发验证，清除错误提示
      nextTick(() => {
        validateOnBlur('salesContact')
      })
    }
  } else {
    form.value.salesContact = ''
  }
})

// 监听区域变化，重新加载客户列表
watch(() => form.value.region, (newRegion) => {
  if (newRegion) {
    loadCustomers()
  }
})

/** 过滤部门树，只保留三级及以下机构 */
// 加载项目经理列表
function loadProjectManagers() {
  listUserByPost('pm').then(response => {
    projectManagers.value = response.data || []
  })
}

// 加载市场经理列表
function loadMarketManagers() {
  listUserByPost('scjl').then(response => {
    marketManagers.value = response.data || []
  })
}

// 加载销售负责人列表
function loadSalesManagers() {
  listUserByPost('xsfzr').then(response => {
    salesManagers.value = response.data || []
  })
}

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
      // 前端 provinceCode 映射到后端 regionCode
      submitData.regionCode = submitData.provinceCode
      delete submitData.provinceCode
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
    router.push('/project/project')
  })
}

/** 初始化年份选项 */
function initYearOptions() {
  const currentYear = new Date().getFullYear()
  const years = []
  for (let i = currentYear; i >= currentYear - 10; i--) {
    years.push(i)
  }
  yearOptions.value = years
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
  form.value.provinceCode = null
  form.value.provinceId = null
  getSecondaryRegions(value)
}

/** 二级区域变化处理 */
function handleSecondaryRegionChange(provinceCode) {
  if (!provinceCode) {
    form.value.provinceId = null
    return
  }
  // 根据选中的provinceCode找到对应的provinceId
  const selectedRegion = secondaryRegionOptions.value.find(item => item.provinceCode === provinceCode)
  if (selectedRegion) {
    form.value.provinceId = selectedRegion.provinceId
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
    // 后端 regionCode 映射到前端 provinceCode
    if (data.regionCode) {
      data.provinceCode = data.regionCode
    }
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
  initYearOptions()
  loadProjectManagers()
  loadMarketManagers()
  loadSalesManagers()
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
