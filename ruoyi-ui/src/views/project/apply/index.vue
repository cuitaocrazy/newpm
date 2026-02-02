<template>
  <div class="app-container project-apply">
    <!-- 页面标题 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <h2>项目立项申请</h2>
        <p class="tips">请完整填写项目信息，提交后将进入审核流程</p>
      </div>
    </el-card>

    <!-- 表单主体 -->
    <el-form ref="applyFormRef" :model="form" :rules="rules" label-width="140px">
      <el-collapse v-model="activeNames" class="apply-collapse">
        
        <!-- 面板1：项目基本信息 -->
        <el-collapse-item name="1" title="一、项目基本信息">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="行业" prop="industry">
                <el-select v-model="form.industry" placeholder="请选择行业" clearable>
                  <el-option v-for="dict in industry" :key="dict.value" 
                    :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="区域" prop="region">
                <el-select v-model="form.region" placeholder="请选择区域" clearable>
                  <el-option v-for="dict in sys_yjqy" :key="dict.value" 
                    :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目简称" prop="shortName">
                <el-input v-model="form.shortName" placeholder="请输入项目简称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="年份" prop="year">
                <el-date-picker v-model="form.year" type="year" 
                  placeholder="请选择年份" value-format="YYYY" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="项目编号" prop="projectCode">
                <el-input v-model="form.projectCode" placeholder="自动生成" :disabled="true" />
                <span class="form-tip">格式：{行业代码}-{区域代码}-{简称}-{年份}</span>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="项目名称" prop="projectName">
                <el-input v-model="form.projectName" placeholder="请输入项目名称" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目分类" prop="projectCategory">
                <el-select v-model="form.projectCategory" placeholder="请选择项目分类" clearable>
                  <el-option v-for="dict in sys_xmfl" :key="dict.value" 
                    :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目部门" prop="projectDept">
                <el-tree-select v-model="form.projectDept" :data="deptOptions"
                  :props="{ value: 'id', label: 'label', children: 'children' }"
                  value-key="id" placeholder="请选择项目部门" check-strictly clearable />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目状态" prop="projectStatus">
                <el-select v-model="form.projectStatus" placeholder="请选择项目状态" clearable>
                  <el-option v-for="dict in sys_xmjd" :key="dict.value" 
                    :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="验收状态" prop="acceptanceStatus">
                <el-select v-model="form.acceptanceStatus" placeholder="请选择验收状态" clearable>
                  <el-option v-for="dict in sys_yszt" :key="dict.value" 
                    :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="预估工作量" prop="estimatedWorkload">
                <el-input v-model="form.estimatedWorkload" placeholder="请输入预估工作量">
                  <template #append>人天</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目地址" prop="projectAddress">
                <el-input v-model="form.projectAddress" placeholder="请输入项目地址" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="项目计划" prop="projectPlan">
                <el-input v-model="form.projectPlan" type="textarea" :rows="3" 
                  placeholder="请输入项目计划" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="项目描述" prop="projectDescription">
                <el-input v-model="form.projectDescription" type="textarea" :rows="3" 
                  placeholder="请输入项目描述" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板2：合同信息 -->
        <el-collapse-item name="2" title="二、合同信息">
          <el-alert type="info" :closable="false" show-icon>
            <template #title>
              合同信息为关联数据，如需修改请前往合同管理模块
            </template>
          </el-alert>
          
          <el-row :gutter="20" class="readonly-section">
            <el-col :span="12">
              <div class="info-item">
                <span class="label">合同名称：</span>
                <span class="value">{{ contractInfo.name || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="label">合同状态：</span>
                <span class="value">{{ contractInfo.status || '-' }}</span>
              </div>
            </el-col>
          </el-row>
          
          <el-row :gutter="20" class="readonly-section">
            <el-col :span="12">
              <div class="info-item">
                <span class="label">合同金额：</span>
                <span class="value">{{ contractInfo.amount || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="label">免费维护期：</span>
                <span class="value">{{ contractInfo.maintenancePeriod || '-' }} 月</span>
              </div>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板3：人员配置 -->
        <el-collapse-item name="3" title="三、人员配置">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目经理" prop="projectManagerId">
                <el-select v-model="form.projectManagerId" placeholder="请选择项目经理" 
                  filterable clearable>
                  <el-option v-for="user in projectManagers" :key="user.userId" 
                    :label="user.nickName" :value="user.userId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="市场经理" prop="marketManagerId">
                <el-select v-model="form.marketManagerId" placeholder="请选择市场经理" 
                  filterable clearable>
                  <el-option v-for="user in marketManagers" :key="user.userId" 
                    :label="user.nickName" :value="user.userId" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="参与人员" prop="participants">
                <!-- 已选人员标签展示 -->
                <div class="selected-participants" v-if="selectedParticipants.length > 0">
                  <el-tag v-for="user in selectedParticipants" :key="user.userId" 
                    closable @close="removeParticipant(user.userId)" 
                    type="info" class="participant-tag">
                    {{ user.nickName }}
                  </el-tag>
                </div>
                
                <!-- 智能提示输入框 -->
                <el-autocomplete v-model="participantInput" 
                  :fetch-suggestions="queryParticipants"
                  placeholder="输入姓名搜索并选择参与人员"
                  @select="handleSelectParticipant"
                  clearable
                  style="width: 100%">
                  <template #default="{ item }">
                    <div class="autocomplete-item">
                      <span class="name">{{ item.nickName }}</span>
                      <span class="username">（{{ item.userName }}）</span>
                    </div>
                  </template>
                </el-autocomplete>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="销售负责人" prop="salesManagerId">
                <el-select v-model="form.salesManagerId" placeholder="请选择销售负责人" 
                  filterable clearable>
                  <el-option v-for="user in salesManagers" :key="user.userId" 
                    :label="user.nickName" :value="user.userId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="销售联系方式" prop="salesContact">
                <el-input v-model="form.salesContact" placeholder="请输入销售联系方式" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板4：客户信息 -->
        <el-collapse-item name="4" title="四、客户信息">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="客户名称" prop="customerId">
                <el-select v-model="form.customerId" placeholder="请选择客户" 
                  filterable clearable>
                  <el-option v-for="customer in customers" :key="customer.customerId" 
                    :label="customer.customerName" :value="customer.customerId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户联系人" prop="customerContactId">
                <el-select v-model="form.customerContactId" placeholder="请先选择客户" 
                  filterable clearable :disabled="!form.customerId">
                  <el-option v-for="contact in customerContacts" :key="contact.contactId" 
                    :label="contact.contactName" :value="contact.contactId" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="客户联系方式">
                <el-input v-model="form.customerPhone" placeholder="选择联系人后自动填充" 
                  :disabled="true" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="商户联系人">
                <el-input v-model="form.merchantContact" placeholder="请输入商户联系人" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="商户联系方式">
                <el-input v-model="form.merchantPhone" placeholder="请输入商户联系方式" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板5：时间规划 -->
        <el-collapse-item name="5" title="五、时间规划">
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
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="实施年度">
                <el-date-picker v-model="form.implementationYear" type="year" 
                  placeholder="请选择实施年度" value-format="YYYY" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板6：成本预算 -->
        <el-collapse-item name="6" title="六、成本预算">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目预算" prop="projectBudget">
                <el-input v-model="form.projectBudget" placeholder="请输入项目预算">
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

        <!-- 面板7：备注 -->
        <el-collapse-item name="7" title="七、备注">
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
        提交申请
      </el-button>
      <el-button size="large" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script setup name="ProjectApply">
import { ref, reactive, toRefs, watch, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { addProject } from '@/api/project/project'
import { listUser } from '@/api/system/user'
import { listDept } from '@/api/system/dept'
import { listCustomer } from '@/api/project/customer'
import { listContact } from '@/api/project/contact'

const router = useRouter()
const { proxy } = getCurrentInstance()
const { sys_xmfl, sys_xmjd, sys_yszt, sys_shzt, sys_qrzt, industry, sys_yjqy, sys_jdgl } = 
  proxy.useDict('sys_xmfl', 'sys_xmjd', 'sys_yszt', 'sys_shzt', 'sys_qrzt', 'industry', 'sys_yjqy', 'sys_jdgl')

// 折叠面板激活状态
const activeNames = ref(['1', '2', '3', '4', '5', '6', '7'])

// 表单数据
const data = reactive({
  form: {
    industry: '',
    region: '',
    shortName: '',
    year: '',
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
    region: [{ required: true, message: '请选择区域', trigger: 'change' }],
    shortName: [{ required: true, message: '请输入项目简称', trigger: 'blur' }],
    year: [{ required: true, message: '请选择年份', trigger: 'change' }],
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
    customerId: [{ required: true, message: '请选择客户名称', trigger: 'change' }],
    customerContactId: [{ required: true, message: '请选择客户联系人', trigger: 'change' }],
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
const deptOptions = ref([])
const projectManagers = ref([])
const marketManagers = ref([])
const salesManagers = ref([])
const customers = ref([])
const customerContacts = ref([])
const selectedParticipants = ref([])
const participantInput = ref('')

// 项目编号自动生成
watch([() => form.value.industry, () => form.value.region, 
       () => form.value.shortName, () => form.value.year],
  ([industry, region, shortName, year]) => {
    if (industry && region && shortName && year) {
      form.value.projectCode = `${industry}-${region}-${shortName}-${year}`
    }
  }
)

// 客户联系人联动
watch(() => form.value.customerId, (customerId) => {
  if (customerId) {
    form.value.customerContactId = ''
    form.value.customerPhone = ''
    loadCustomerContacts(customerId)
  }
})

watch(() => form.value.customerContactId, (contactId) => {
  if (contactId) {
    const contact = customerContacts.value.find(c => c.contactId === contactId)
    if (contact) {
      form.value.customerPhone = contact.phone
    }
  }
})

// 加载部门树
function getDeptTree() {
  listDept().then(response => {
    const deptData = response.data.map(dept => ({
      ...dept,
      id: dept.deptId,
      label: dept.deptName
    }))
    deptOptions.value = proxy.handleTree(deptData, "id")
  })
}

// 加载项目经理列表
function loadProjectManagers() {
  proxy.$http.get('/system/user/listByPost', { params: { postCode: 'pm' } }).then(response => {
    projectManagers.value = response.data || []
  })
}

// 加载市场经理列表
function loadMarketManagers() {
  proxy.$http.get('/system/user/listByPost', { params: { postCode: 'scjl' } }).then(response => {
    marketManagers.value = response.data || []
  })
}

// 加载销售负责人列表
function loadSalesManagers() {
  proxy.$http.get('/system/user/listByPost', { params: { postCode: 'xsfzr' } }).then(response => {
    salesManagers.value = response.data || []
  })
}

// 加载客户列表
function loadCustomers() {
  listCustomer({}).then(response => {
    customers.value = response.rows || []
  })
}

// 加载客户联系人列表
function loadCustomerContacts(customerId) {
  listContact({ customerId }).then(response => {
    customerContacts.value = response.rows || []
  })
}

// 参与人员智能提示
function queryParticipants(queryString, cb) {
  if (!queryString) {
    cb([])
    return
  }
  listUser({ userName: queryString }).then(response => {
    const results = response.rows.map(user => ({
      value: user.nickName,
      userId: user.userId,
      userName: user.userName,
      nickName: user.nickName
    }))
    cb(results)
  })
}

// 选中参与人员
function handleSelectParticipant(item) {
  if (!selectedParticipants.value.find(p => p.userId === item.userId)) {
    selectedParticipants.value.push(item)
    form.value.participants = selectedParticipants.value.map(p => p.userId)
  }
  participantInput.value = ''
}

// 删除参与人员
function removeParticipant(userId) {
  selectedParticipants.value = selectedParticipants.value.filter(p => p.userId !== userId)
  form.value.participants = selectedParticipants.value.map(p => p.userId)
}

// 提交表单
function submitForm() {
  proxy.$refs['applyFormRef'].validate(valid => {
    if (valid) {
      proxy.$modal.confirm('确认提交立项申请？').then(() => {
        submitLoading.value = true
        addProject(form.value).then(response => {
          proxy.$modal.msgSuccess('立项申请提交成功')
          router.push('/project/list')
        }).finally(() => {
          submitLoading.value = false
        })
      })
    } else {
      proxy.$modal.msgError('请完善必填信息')
    }
  })
}

// 取消
function cancel() {
  proxy.$modal.confirm('确认取消立项申请？未保存的数据将丢失').then(() => {
    router.push('/project/list')
  })
}

// 初始化
getDeptTree()
loadProjectManagers()
loadMarketManagers()
loadSalesManagers()
loadCustomers()
</script>

<style scoped lang="scss">
.project-apply {
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
    }
  }
}
</style>
