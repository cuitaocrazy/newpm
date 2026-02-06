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
      <el-card shadow="hover" style="margin-bottom: 15px;">
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
        <el-col :span="8">
          <el-form-item label="行业" prop="industry">
            <el-select v-model="form.industry" placeholder="请选择行业" @change="generateProjectCode">
              <el-option
                v-for="dict in industry"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="一级区域" prop="region">
            <el-select v-model="form.region" placeholder="请选择一级区域" @change="handleRegionChange">
              <el-option
                v-for="dict in sys_yjqy"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="二级区域" prop="regionCode">
            <el-select v-model="form.regionCode" placeholder="请选择二级区域" :disabled="!form.region" @change="generateProjectCode">
              <el-option
                v-for="item in secondaryRegionOptions"
                :key="item.provinceCode"
                :label="item.provinceName"
                :value="item.provinceCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="简称" prop="shortName">
            <el-input v-model="form.shortName" placeholder="请输入简称" @input="generateProjectCode" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="立项年度" prop="establishedYear">
            <el-select v-model="form.establishedYear" placeholder="请选择立项年度" @change="generateProjectCode">
              <el-option
                v-for="dict in sys_ndgl"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="项目编号" prop="projectCode">
            <el-input v-model="form.projectCode" placeholder="自动生成" readonly />
            <div style="color: #909399; font-size: 12px; margin-top: 5px;">
              格式：{行业代码}-{一级区域代码}-{二级区域代码}-{简称}-{年份}
            </div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="项目名称" prop="projectName">
            <el-input v-model="form.projectName" placeholder="请输入项目名称" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="项目分类" prop="projectCategory">
            <el-select v-model="form.projectCategory" placeholder="请选择项目分类">
              <el-option
                v-for="dict in sys_xmfl"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="项目部门" prop="projectDept">
            <el-tree-select
              v-model="form.projectDept"
              :data="deptOptions"
              :props="{ value: 'id', label: 'label', children: 'children' }"
              value-key="id"
              placeholder="请选择项目部门"
              check-strictly
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="项目状态" prop="projectStatus">
            <el-select v-model="form.projectStatus" placeholder="请选择项目状态">
              <el-option
                v-for="dict in sys_xmjd"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="验收状态" prop="acceptanceStatus">
            <el-select v-model="form.acceptanceStatus" placeholder="请选择验收状态">
              <el-option
                v-for="dict in sys_yszt"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="预估工作量(人天)" prop="estimatedWorkload">
            <el-input-number v-model="form.estimatedWorkload" :min="0" :precision="2" placeholder="请输入预估工作量" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="实际工作量(人天)" prop="actualWorkload">
            <el-input-number v-model="form.actualWorkload" :min="0" :precision="2" placeholder="请输入实际工作量" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="项目预算(元)" prop="projectBudget">
            <el-input-number v-model="form.projectBudget" :min="0" :precision="2" placeholder="请输入项目预算" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="项目地址" prop="projectAddress">
            <el-input v-model="form.projectAddress" placeholder="请输入项目地址" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="项目计划" prop="projectPlan">
            <el-input v-model="form.projectPlan" type="textarea" :rows="3" placeholder="请输入项目计划" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="项目描述" prop="projectDescription">
            <el-input v-model="form.projectDescription" type="textarea" :rows="3" placeholder="请输入项目描述（用于项目状态为服务类别，在此添加巡检相关信息）" />
          </el-form-item>
        </el-col>
      </el-row>
        </div>
      </el-card>

      <!-- 二、人员配置 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <div @click="togglePanel('3')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('3') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">二、人员配置</span>
          </div>
        </template>
        <div v-show="activeNames.includes('3')">

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="项目经理" prop="projectManagerId">
            <el-select v-model="form.projectManagerId" placeholder="请选择项目经理" filterable>
              <el-option
                v-for="user in projectManagerOptions"
                :key="user.userId"
                :label="user.nickName"
                :value="user.userId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="市场经理" prop="marketManagerId">
            <el-select v-model="form.marketManagerId" placeholder="请选择市场经理" filterable>
              <el-option
                v-for="user in marketManagerOptions"
                :key="user.userId"
                :label="user.nickName"
                :value="user.userId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="销售负责人" prop="salesManagerId">
            <el-select v-model="form.salesManagerId" placeholder="请选择销售负责人" filterable @change="handleSalesManagerChange">
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

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="销售联系方式" prop="salesContact">
            <el-input v-model="form.salesContact" placeholder="自动带出" readonly />
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item label="参与人员" prop="participants">
            <el-select v-model="participantIds" placeholder="请选择参与人员" multiple filterable style="width: 100%">
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
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <div @click="togglePanel('4')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('4') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">三、客户信息</span>
          </div>
        </template>
        <div v-show="activeNames.includes('4')">

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="客户名称" prop="customerId">
            <el-select v-model="form.customerId" placeholder="请选择客户" filterable @change="handleCustomerChange">
              <el-option
                v-for="customer in customerOptions"
                :key="customer.customerId"
                :label="customer.customerSimpleName"
                :value="customer.customerId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="客户联系人" prop="customerContactId">
            <el-select v-model="form.customerContactId" placeholder="请选择客户联系人" :disabled="!form.customerId" @change="handleContactChange">
              <el-option
                v-for="contact in contactOptions"
                :key="contact.contactId"
                :label="contact.contactName"
                :value="contact.contactId"
              />
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
        <el-col :span="8">
          <el-form-item label="商户联系人" prop="merchantContact">
            <el-input v-model="form.merchantContact" placeholder="请输入商户联系人" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="商户联系方式">
            <el-input v-model="form.merchantPhone" placeholder="请输入商户联系方式" />
          </el-form-item>
        </el-col>
      </el-row>
        </div>
      </el-card>

      <!-- 四、时间规划 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <div @click="togglePanel('5')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('5') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">四、时间规划</span>
          </div>
        </template>
        <div v-show="activeNames.includes('5')">

      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="启动日期" prop="startDate">
            <el-date-picker
              v-model="form.startDate"
              type="date"
              placeholder="选择启动日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="结束日期" prop="endDate">
            <el-date-picker
              v-model="form.endDate"
              type="date"
              placeholder="选择结束日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="投产日期" prop="productionDate">
            <el-date-picker
              v-model="form.productionDate"
              type="date"
              placeholder="选择投产日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="验收日期" prop="acceptanceDate">
            <el-date-picker
              v-model="form.acceptanceDate"
              type="date"
              placeholder="选择验收日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
        </div>
      </el-card>

      <!-- 五、成本预算 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <div @click="togglePanel('6')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('6') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">五、成本预算</span>
          </div>
        </template>
        <div v-show="activeNames.includes('6')">

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="项目费用(元)" prop="projectCost">
            <el-input-number v-model="form.projectCost" :min="0" :precision="2" placeholder="请输入项目费用" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="费用预算(元)" prop="expenseBudget">
            <el-input-number v-model="form.expenseBudget" :min="0" :precision="2" placeholder="请输入费用预算" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="成本预算(元)" prop="costBudget">
            <el-input-number v-model="form.costBudget" :min="0" :precision="2" placeholder="请输入成本预算" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="人力费用(元)" prop="laborCost">
            <el-input-number v-model="form.laborCost" :min="0" :precision="2" placeholder="请输入人力费用" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="采购成本(元)" prop="purchaseCost">
            <el-input-number v-model="form.purchaseCost" :min="0" :precision="2" placeholder="请输入采购成本" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
        </div>
      </el-card>

      <!-- 六、备注 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <div @click="togglePanel('7')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('7') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">六、备注</span>
          </div>
        </template>
        <div v-show="activeNames.includes('7')">

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
          </el-form-item>
        </el-col>
      </el-row>
        </div>
      </el-card>

      <!-- 按钮 -->
      <el-row>
        <el-col :span="24" style="text-align: center; margin-top: 20px; margin-bottom: 60px;">
          <el-button type="primary" @click="submitForm">提交</el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button @click="cancel">取消</el-button>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<style scoped>
/* 禁用 label 点击触发表单控件的默认行为 */
:deep(.el-form-item__label) {
  pointer-events: none;
}
</style>

<script setup name="ProjectApply">
import { addProject } from "@/api/project/project"
import request from '@/utils/request'

const { proxy } = getCurrentInstance()
const router = useRouter()
const { industry, sys_xmfl, sys_ndgl, sys_yjqy, sys_xmjd, sys_yszt } = proxy.useDict('industry', 'sys_xmfl', 'sys_ndgl', 'sys_yjqy', 'sys_xmjd', 'sys_yszt')

const formRef = ref()
const activeNames = ref(['1', '3', '4', '5', '6', '7']) // 默认展开所有折叠面板
const allPanelNames = ['1', '3', '4', '5', '6', '7'] // 所有面板的name
const form = ref({
  industry: null,
  region: null,
  regionCode: null,
  shortName: null,
  establishedYear: null,
  projectCode: null,
  projectName: null,
  projectCategory: null,
  projectDept: null,
  projectStatus: null,
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
const deptOptions = ref([])
const projectManagerOptions = ref([])
const marketManagerOptions = ref([])
const allUserOptions = ref([])
const customerOptions = ref([])
const contactOptions = ref([])
const participantIds = ref([])
const customerContactPhone = ref('')

// 监听参与人员变化，同步到表单
watch(participantIds, (newVal) => {
  form.value.participants = newVal.join(',')
})

/** 获取部门树 */
function getDeptTree() {
  request({
    url: '/system/dept/treeselect',
    method: 'get'
  }).then(response => {
    deptOptions.value = response.data || []
  })
}

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
  getSecondaryRegions(value)
  generateProjectCode()
}

/** 销售负责人变化处理 */
function handleSalesManagerChange(userId) {
  if (!userId) {
    form.value.salesContact = null
    return
  }
  const user = allUserOptions.value.find(u => u.userId === userId)
  if (user) {
    form.value.salesContact = user.phonenumber || ''
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
  formRef.value.validate(valid => {
    if (valid) {
      addProject(form.value).then(response => {
        proxy.$modal.msgSuccess("提交成功")
        router.push('/project/list')
      })
    }
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
  getDeptTree()
  getProjectManagers()
  getMarketManagers()
  getAllUsers()
  getCustomers()
})
</script>

<style scoped>
.app-container {
  padding-bottom: 80px;
}
</style>
