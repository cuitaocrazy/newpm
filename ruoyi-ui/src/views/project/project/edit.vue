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
              <el-form-item label="项目名称" prop="projectName">
                <el-input v-model="form.projectName" placeholder="请输入项目名称" />
              </el-form-item>
            </el-col>
          </el-row>

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

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="审核状态">
                <el-tag v-if="form.approvalStatus === '0'" type="warning">待审核</el-tag>
                <el-tag v-else-if="form.approvalStatus === '1'" type="success">审核通过</el-tag>
                <el-tag v-else-if="form.approvalStatus === '2'" type="danger">审核拒绝</el-tag>
                <span v-else>-</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="审核原因">
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
                <el-input v-model="form.salesContact" placeholder="选择销售负责人后自动显示"
                  readonly disabled />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 面板3：客户信息 -->
        <el-collapse-item name="3" title="三、客户信息">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="客户ID" prop="customerId">
                <el-input v-model="form.customerId" placeholder="请输入客户ID" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户联系人ID" prop="customerContactId">
                <el-input v-model="form.customerContactId" placeholder="请输入客户联系人ID" clearable />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="客户联系方式">
                <el-input v-model="form.customerPhone" placeholder="请输入客户联系方式" />
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
import { ref, reactive, toRefs, watch, getCurrentInstance, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject, updateProject } from '@/api/project/project'
import { listUser, listUserByPost } from '@/api/system/user'
import { listDept } from '@/api/system/dept'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const { proxy } = getCurrentInstance()
const { sys_xmfl, sys_xmjd, sys_yszt, sys_shzt, sys_qrzt, industry, sys_yjqy, sys_jdgl } = 
  proxy.useDict('sys_xmfl', 'sys_xmjd', 'sys_yszt', 'sys_shzt', 'sys_qrzt', 'industry', 'sys_yjqy', 'sys_jdgl')

// 折叠面板激活状态
const activeNames = ref(['1', '2', '3', '4', '5', '6'])

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
const deptOptions = ref([])
const projectManagers = ref([])
const marketManagers = ref([])
const salesManagers = ref([])
const allUsers = ref([])
const selectedParticipants = ref([])
const participantInput = ref('')
const participantAutocomplete = ref(null)

// 项目编号自动生成
watch([() => form.value.industry, () => form.value.region, 
       () => form.value.shortName, () => form.value.year],
  ([industry, region, shortName, year]) => {
    if (industry && region && shortName && year) {
      form.value.projectCode = `${industry}-${region}-${shortName}-${year}`
    }
  }
)

// 监听销售负责人变化，自动填充手机号
watch(() => form.value.salesManagerId, (userId) => {
  if (userId) {
    const salesManager = salesManagers.value.find(u => u.userId === userId)
    if (salesManager) {
      form.value.salesContact = salesManager.phonenumber || ''
    }
  } else {
    form.value.salesContact = ''
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
  proxy.$refs['editFormRef'].validate(valid => {
    if (valid) {
      proxy.$modal.confirm('确认保存项目信息？').then(() => {
        submitLoading.value = true
        // 深拷贝表单数据，避免修改原始数据
        const submitData = { ...form.value }
        // 将参与人员数组转换为逗号分隔的字符串
        if (Array.isArray(submitData.participants)) {
          submitData.participants = submitData.participants.join(',')
        }
        updateProject(submitData).then(response => {
          proxy.$modal.msgSuccess('保存成功')
          router.push('/project/project')
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
  proxy.$modal.confirm('确认取消编辑？未保存的数据将丢失').then(() => {
    router.push('/project/project')
  })
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
    // 填充表单
    Object.assign(form.value, data)
  }).catch(() => {
    proxy.$modal.msgError('加载项目数据失败')
    router.push('/project/project')
  }).finally(() => {
    loading.value = false
  })
}

// 页面加载时初始化
onMounted(() => {
  getDeptTree()
  loadProjectManagers()
  loadMarketManagers()
  loadSalesManagers()
  loadAllUsers()
  loadProjectData()
})
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
