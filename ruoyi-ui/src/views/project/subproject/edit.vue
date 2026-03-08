<template>
  <div class="app-container">
    <h2 style="margin: 0 0 6px 0; font-weight: bold;">编辑子项目</h2>

    <el-alert v-if="parentProject" type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>
        所属主项目：{{ parentProject.projectName }}（{{ parentProject.projectCode }}）
      </template>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" v-loading="loading">
      <div style="text-align: right; margin-bottom: 10px;">
        <el-link type="primary" @click="expandAll" style="margin-right: 10px;">全部展开</el-link>
        <el-link type="primary" @click="collapseAll">全部折叠</el-link>
      </div>

      <!-- 一、项目基本信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <div @click="togglePanel('1')" style="cursor: pointer; user-select: none; display: flex; align-items: center;">
            <i :class="activeNames.includes('1') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">一、项目基本信息</span>
          </div>
        </template>
        <div v-show="activeNames.includes('1')">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="子项目编号" prop="taskCode" data-prop="taskCode">
                <el-input v-model="form.taskCode" placeholder="如：01、用户系统（父项目内唯一标识）" @blur="validateOnBlur('taskCode')" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="子项目名称" prop="projectName" data-prop="projectName">
                <el-input v-model="form.projectName" placeholder="请输入子项目名称" @blur="validateOnBlur('projectName')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="行业" prop="industry" data-prop="industry">
                <el-select v-model="form.industry" placeholder="请选择行业" @change="generateProjectCode" @blur="validateOnBlur('industry')">
                  <el-option v-for="dict in industry" :key="dict.value" :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="一级区域" prop="region" data-prop="region">
                <dict-select v-model="form.region" dict-type="sys_yjqy" placeholder="请选择一级区域"
                  @change="handleRegionChange" @blur="validateOnBlur('region')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="二级区域" prop="regionCode" data-prop="regionCode">
                <el-select v-model="form.regionCode" placeholder="请选择二级区域" :disabled="!form.region"
                  @change="handleSecondaryRegionChange" @blur="validateOnBlur('regionCode')">
                  <el-option v-for="item in secondaryRegionOptions" :key="item.regionCode" :label="item.regionName" :value="item.regionCode" />
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
                <dict-select v-model="form.establishedYear" dict-type="sys_ndgl" placeholder="请选择立项年度"
                  @change="generateProjectCode" @blur="validateOnBlur('establishedYear')" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目编号" prop="projectCode" data-prop="projectCode">
                <el-input v-model="form.projectCode" placeholder="自动生成" readonly />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目分类" prop="projectCategory" data-prop="projectCategory">
                <dict-select v-model="form.projectCategory" dict-type="sys_xmfl" placeholder="请选择项目分类" @blur="validateOnBlur('projectCategory')" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目部门" prop="projectDept" data-prop="projectDept">
                <project-dept-select v-model="form.projectDept" @blur="validateOnBlur('projectDept')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目状态" prop="projectStatus" data-prop="projectStatus">
                <dict-select v-model="form.projectStatus" dict-type="sys_xmzt" placeholder="请选择项目状态" clearable @blur="validateOnBlur('projectStatus')" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目阶段" prop="projectStage" data-prop="projectStage">
                <dict-select v-model="form.projectStage" dict-type="sys_xmjd" placeholder="请选择项目阶段" @blur="validateOnBlur('projectStage')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="验收状态" prop="acceptanceStatus" data-prop="acceptanceStatus">
                <dict-select v-model="form.acceptanceStatus" dict-type="sys_yszt" placeholder="请选择验收状态" @blur="validateOnBlur('acceptanceStatus')" />
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
            <el-col :span="12">
              <el-form-item label="项目预算" prop="projectBudget" data-prop="projectBudget">
                <el-input v-model="form.projectBudget" placeholder="请输入金额"
                  @input="handleAmountInput('projectBudget', $event)" @blur="handleAmountBlur('projectBudget')">
                  <template #append>元</template>
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
                <el-input v-model="form.projectDescription" type="textarea" :rows="3" placeholder="请输入项目描述" @blur="validateOnBlur('projectDescription')" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 二、人员配置 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <div @click="togglePanel('3')" style="cursor: pointer; user-select: none; display: flex; align-items: center;">
            <i :class="activeNames.includes('3') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">二、人员配置</span>
          </div>
        </template>
        <div v-show="activeNames.includes('3')">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目经理" prop="projectManagerId" data-prop="projectManagerId">
                <user-select v-model="form.projectManagerId" post-code="pm" placeholder="请选择项目经理" filterable @blur="validateOnBlur('projectManagerId')" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="市场经理" prop="marketManagerId" data-prop="marketManagerId">
                <user-select v-model="form.marketManagerId" post-code="scjl" placeholder="请选择市场经理" filterable @blur="validateOnBlur('marketManagerId')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="销售负责人" prop="salesManagerId" data-prop="salesManagerId">
                <user-select v-model="form.salesManagerId" post-code="xsfzr" placeholder="请选择销售负责人" filterable
                  @change="handleSalesManagerChange" @blur="validateOnBlur('salesManagerId')" />
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
                <org-user-select v-model="participantIds" placeholder="请选择参与人员" style="width: 100%" :no-data-scope="true" @blur="validateOnBlur('participants')" />
                <div v-if="participantIds.length > 0" style="margin-top: 6px; color: #606266; font-size: 13px;">已选 {{ participantIds.length }} 人</div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 三、客户信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <div @click="togglePanel('4')" style="cursor: pointer; user-select: none; display: flex; align-items: center;">
            <i :class="activeNames.includes('4') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">三、客户信息</span>
          </div>
        </template>
        <div v-show="activeNames.includes('4')">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="客户名称" prop="customerId" data-prop="customerId">
                <el-select v-model="form.customerId" placeholder="请选择客户" filterable @change="handleCustomerChange" @blur="validateOnBlur('customerId')">
                  <el-option v-for="c in customerOptions" :key="c.customerId" :label="c.customerSimpleName" :value="c.customerId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户联系人" prop="customerContactId" data-prop="customerContactId">
                <el-select v-model="form.customerContactId" placeholder="请选择客户联系人" :disabled="!form.customerId" @change="handleContactChange" @blur="validateOnBlur('customerContactId')">
                  <el-option v-for="c in contactOptions" :key="c.contactId" :label="c.contactName" :value="c.contactId" />
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
        </div>
      </el-card>

      <!-- 四、时间规划 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <div @click="togglePanel('5')" style="cursor: pointer; user-select: none; display: flex; align-items: center;">
            <i :class="activeNames.includes('5') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">四、时间规划</span>
          </div>
        </template>
        <div v-show="activeNames.includes('5')">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="启动日期">
                <el-date-picker v-model="form.startDate" type="date" placeholder="选择启动日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束日期">
                <el-date-picker v-model="form.endDate" type="date" placeholder="选择结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="投产日期">
                <el-date-picker v-model="form.productionDate" type="date" placeholder="选择投产日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="验收日期">
                <el-date-picker v-model="form.acceptanceDate" type="date" placeholder="选择验收日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 五、成本预算 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <div @click="togglePanel('6')" style="cursor: pointer; user-select: none; display: flex; align-items: center;">
            <i :class="activeNames.includes('6') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">五、成本预算</span>
          </div>
        </template>
        <div v-show="activeNames.includes('6')">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目费用">
                <el-input v-model="form.projectCost" placeholder="请输入项目费用"
                  @input="handleAmountInput('projectCost', $event)" @blur="handleAmountBlur('projectCost')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="费用预算">
                <el-input v-model="form.expenseBudget" placeholder="请输入费用预算"
                  @input="handleAmountInput('expenseBudget', $event)" @blur="handleAmountBlur('expenseBudget')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="成本预算">
                <el-input v-model="form.costBudget" placeholder="请输入成本预算"
                  @input="handleAmountInput('costBudget', $event)" @blur="handleAmountBlur('costBudget')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="人力费用">
                <el-input v-model="form.laborCost" placeholder="请输入人力费用"
                  @input="handleAmountInput('laborCost', $event)" @blur="handleAmountBlur('laborCost')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="采购成本">
                <el-input v-model="form.purchaseCost" placeholder="请输入采购成本"
                  @input="handleAmountInput('purchaseCost', $event)" @blur="handleAmountBlur('purchaseCost')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 六、备注 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <div @click="togglePanel('7')" style="cursor: pointer; user-select: none; display: flex; align-items: center;">
            <i :class="activeNames.includes('7') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">六、备注</span>
          </div>
        </template>
        <div v-show="activeNames.includes('7')">
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </el-form>

    <!-- 底部操作按钮 -->
    <div class="form-footer">
      <el-button type="primary" size="large" @click="submitForm">保存</el-button>
      <el-button size="large" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script setup name="SubprojectEdit">
import { ref, watch, getCurrentInstance, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject, updateProject, checkProjectCode } from '@/api/project/project'
import { ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useFormValidation } from '@/composables/useFormValidation'

let codeGenSeq = 0

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const { industry, sys_xmfl, sys_ndgl, sys_yjqy, sys_xmjd, sys_yszt, sys_xmzt } = proxy.useDict('industry', 'sys_xmfl', 'sys_ndgl', 'sys_yjqy', 'sys_xmjd', 'sys_yszt', 'sys_xmzt')

const formRef = ref()
const loading = ref(false)
const activeNames = ref(['1', '3', '4', '5', '6', '7'])
const allPanelNames = ['1', '3', '4', '5', '6', '7']
const parentProject = ref(null)

const { validateOnBlur, validateAndScroll } = useFormValidation(formRef, activeNames)

const form = ref({
  projectId: null,
  parentId: null,
  projectLevel: 1,
  taskCode: null,
  projectName: null,
  industry: null,
  region: null,
  regionCode: null,
  regionId: null,
  shortName: null,
  establishedYear: null,
  projectCode: null,
  projectCategory: null,
  projectDept: null,
  projectStatus: null,
  projectStage: null,
  acceptanceStatus: null,
  estimatedWorkload: null,
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
  merchantContact: null,
  merchantPhone: null,
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
  projectName: [{ required: true, message: '子项目名称不能为空', trigger: 'blur' }],
  industry: [{ required: true, message: '行业不能为空', trigger: 'change' }],
  region: [{ required: true, message: '一级区域不能为空', trigger: 'change' }],
  regionCode: [{ required: true, message: '二级区域不能为空', trigger: 'change' }],
  shortName: [{ required: true, message: '简称不能为空', trigger: 'blur' }],
  establishedYear: [{ required: true, message: '立项年度不能为空', trigger: 'change' }],
  projectCategory: [{ required: true, message: '项目分类不能为空', trigger: 'change' }],
  projectDept: [{ required: true, message: '项目部门不能为空', trigger: 'change' }],
  projectStatus: [{ required: true, message: '项目状态不能为空', trigger: 'change' }],
  projectStage: [{ required: true, message: '项目阶段不能为空', trigger: 'change' }],
  acceptanceStatus: [{ required: true, message: '验收状态不能为空', trigger: 'change' }],
  estimatedWorkload: [{ required: true, message: '预估工作量不能为空', trigger: 'blur' }],
  projectBudget: [{ required: true, message: '项目预算不能为空', trigger: 'blur' }],
  projectAddress: [{ required: true, message: '项目地址不能为空', trigger: 'blur' }],
  projectPlan: [{ required: true, message: '项目计划不能为空', trigger: 'blur' }],
  projectDescription: [{ required: true, message: '项目描述不能为空', trigger: 'blur' }],
  projectManagerId: [{ required: true, message: '项目经理不能为空', trigger: 'change' }],
  customerId: [{ required: true, message: '客户名称不能为空', trigger: 'change' }],
  customerContactId: [{ required: true, message: '客户联系人不能为空', trigger: 'change' }]
})

const secondaryRegionOptions = ref([])
const customerOptions = ref([])
const contactOptions = ref([])
const participantIds = ref([])
const customerContactPhone = ref('')

watch(participantIds, (newVal) => {
  form.value.participants = newVal.join(',')
})

function getCustomers() {
  request({ url: '/project/customer/list', method: 'get', params: { pageNum: 1, pageSize: 1000 } })
    .then(response => { customerOptions.value = response.rows || [] })
}

function getSecondaryRegions(regionDictValue) {
  if (!regionDictValue) { secondaryRegionOptions.value = []; return }
  request({ url: '/project/secondaryRegion/listByRegion', method: 'get', params: { regionDictValue } })
    .then(response => { secondaryRegionOptions.value = response.data || [] })
}

function handleRegionChange(value) {
  form.value.regionCode = null
  form.value.regionId = null
  getSecondaryRegions(value)
  generateProjectCode()
}

function handleSecondaryRegionChange(regionCode) {
  if (!regionCode) { form.value.regionId = null; generateProjectCode(); return }
  const sel = secondaryRegionOptions.value.find(i => i.regionCode === regionCode)
  if (sel) form.value.regionId = sel.regionId
  generateProjectCode()
}

function handleSalesManagerChange(userId, user) {
  if (!userId) { form.value.salesContact = null; return }
  if (user) {
    form.value.salesContact = user.phonenumber || ''
    nextTick(() => validateOnBlur('salesContact'))
  }
}

function handleCustomerChange(customerId) {
  form.value.customerContactId = null
  customerContactPhone.value = ''
  if (!customerId) { contactOptions.value = []; return }
  request({ url: '/project/customer/contact/listByCustomer', method: 'get', params: { customerId } })
    .then(response => { contactOptions.value = response.data || [] })
}

function handleContactChange(contactId) {
  if (!contactId) { customerContactPhone.value = ''; return }
  const contact = contactOptions.value.find(c => c.contactId === contactId)
  if (contact) customerContactPhone.value = contact.contactPhone || ''
}

async function generateProjectCode() {
  const { industry: ind, region, regionCode, shortName, establishedYear } = form.value
  if (!(ind && region && regionCode && shortName && establishedYear)) return
  const mySeq = ++codeGenSeq
  const baseCode = `${ind}-${region}-${regionCode}-${shortName}-${establishedYear}`
  const res = await checkProjectCode(baseCode, form.value.projectId)
  if (mySeq !== codeGenSeq) return
  const { exists, existingProject, suggestedCode } = res.data
  if (!exists) { form.value.projectCode = baseCode; return }
  try {
    await ElMessageBox.confirm(
      `该项目编号【${existingProject.projectCode}】已被项目【${existingProject.projectName}】使用，请确认是否继续？`,
      '项目编号重复提示',
      { confirmButtonText: '继续', cancelButtonText: '取消', type: 'warning' }
    )
    if (mySeq !== codeGenSeq) return
    form.value.projectCode = suggestedCode
  } catch {
    if (mySeq !== codeGenSeq) return
    form.value.projectCode = ''
    form.value.shortName = ''
  }
}

function handleAmountInput(field, value) {
  let cleaned = String(value).replace(/[^\d.]/g, '')
  const dotIndex = cleaned.indexOf('.')
  if (dotIndex !== -1) cleaned = cleaned.substring(0, dotIndex + 1) + cleaned.substring(dotIndex + 1).replace(/\./g, '')
  const parts = cleaned.split('.')
  if (parts.length === 2 && parts[1].length > 2) cleaned = parts[0] + '.' + parts[1].substring(0, 2)
  form.value[field] = cleaned
}

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

function handleAmountBlur(field) {
  finalizeAmountFormat(field)
  validateOnBlur(field)
}

function parseAmount(value) {
  if (value === null || value === undefined || value === '') return null
  const num = parseFloat(String(value).replace(/,/g, ''))
  return isNaN(num) ? null : num
}

function formatAmountForDisplay(value) {
  if (value === null || value === undefined || value === '') return null
  const num = parseFloat(String(value))
  if (isNaN(num)) return null
  const parts = num.toFixed(2).split('.')
  const intPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return `${intPart}.${parts[1]}`
}

async function loadSubproject(projectId) {
  loading.value = true
  try {
    const res = await getProject(projectId)
    const data = res.data
    // Populate form
    Object.keys(form.value).forEach(key => {
      if (data[key] !== undefined) form.value[key] = data[key]
    })
    // Format amount fields for display
    const amountFields = ['projectBudget', 'projectCost', 'expenseBudget', 'costBudget', 'laborCost', 'purchaseCost']
    amountFields.forEach(f => { form.value[f] = formatAmountForDisplay(data[f]) })
    // Restore participants
    if (data.participants) {
      participantIds.value = data.participants.split(',').filter(Boolean).map(Number)
    }
    // Load secondary regions for current region
    if (data.region) {
      await new Promise(resolve => {
        request({ url: '/project/secondaryRegion/listByRegion', method: 'get', params: { regionDictValue: data.region } })
          .then(r => { secondaryRegionOptions.value = r.data || []; resolve() })
      })
    }
    // Load contacts for current customer
    if (data.customerId) {
      request({ url: '/project/customer/contact/listByCustomer', method: 'get', params: { customerId: data.customerId } })
        .then(r => {
          contactOptions.value = r.data || []
          // Set contact phone display
          if (data.customerContactId) {
            const contact = contactOptions.value.find(c => c.contactId === data.customerContactId)
            if (contact) customerContactPhone.value = contact.contactPhone || ''
          }
        })
    }
    // Load parent project info
    if (data.parentId) {
      getProject(data.parentId).then(r => { parentProject.value = r.data })
    }
  } finally {
    loading.value = false
  }
}

function submitForm() {
  validateAndScroll(() => {
    const submitData = {
      ...form.value,
      projectBudget: parseAmount(form.value.projectBudget),
      projectCost: parseAmount(form.value.projectCost),
      expenseBudget: parseAmount(form.value.expenseBudget),
      costBudget: parseAmount(form.value.costBudget),
      laborCost: parseAmount(form.value.laborCost),
      purchaseCost: parseAmount(form.value.purchaseCost)
    }
    updateProject(submitData).then(() => {
      proxy.$modal.msgSuccess('保存成功')
      router.push({ path: '/project/subproject', query: { parentId: form.value.parentId } })
    })
  })
}

function cancel() {
  router.push({ path: '/project/subproject', query: { parentId: form.value.parentId } })
}

function expandAll()  { activeNames.value = [...allPanelNames] }
function collapseAll() { activeNames.value = [] }
function togglePanel(name) {
  const idx = activeNames.value.indexOf(name)
  if (idx > -1) activeNames.value.splice(idx, 1)
  else activeNames.value.push(name)
}

onMounted(() => {
  getCustomers()
  const projectId = route.params.projectId
  if (projectId) loadSubproject(Number(projectId))
})
</script>

<style scoped>
.app-container { padding-bottom: 80px; }
:deep(.el-form-item__label) { pointer-events: none; }
.form-footer {
  position: sticky; bottom: 0; padding: 20px;
  background-color: #fff; border-top: 1px solid #dcdfe6;
  text-align: center; z-index: 10;
}
.form-footer .el-button { min-width: 120px; margin: 0 10px; }
</style>

<style scoped src="@/assets/styles/form-validation.scss"></style>
