<template>
  <div class="app-container">
    <h2 style="margin: 0 0 16px 0; font-weight: bold;">项目分解任务</h2>

    <!-- 第一步：选择项目 -->
    <el-card shadow="hover" style="margin-bottom: 15px;">
      <template #header><span style="font-size: 16px; font-weight: bold;">一、选择所属项目</span></template>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="所属机构" label-width="100px">
            <project-dept-select v-model="searchDept" style="width: 100%" @change="onDeptChange" />
          </el-form-item>
        </el-col>
        <el-col :span="10">
          <el-form-item label="项目名称" label-width="100px">
            <el-autocomplete
              v-model="projectKeyword"
              :fetch-suggestions="fetchProjectSuggestions"
              placeholder="请输入项目名称搜索"
              value-key="projectName"
              style="width: 100%"
              :debounce="300"
              clearable
              @select="onProjectSelect"
              @clear="onProjectClear"
            >
              <template #default="{ item }">
                <span>{{ item.projectName }}</span>
                <span style="color: #909399; font-size: 12px; margin-left: 8px;">{{ item.projectCode }}</span>
              </template>
            </el-autocomplete>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 项目基本信息只读展示 -->
      <div v-if="selectedProject" style="margin-top: 12px;">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="行业">
            <dict-tag :options="industry" :value="selectedProject.industry" />
          </el-descriptions-item>
          <el-descriptions-item label="一级区域">
            <dict-tag :options="sys_yjqy" :value="selectedProject.region" />
          </el-descriptions-item>
          <el-descriptions-item label="二级区域">{{ selectedProject.regionName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="简称">{{ selectedProject.shortName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="立项年份">
            <dict-tag :options="sys_ndgl" :value="selectedProject.establishedYear" />
          </el-descriptions-item>
          <el-descriptions-item label="项目ID">{{ selectedProject.projectCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目名称" :span="3">{{ selectedProject.projectName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目分类">
            <dict-tag :options="sys_xmfl" :value="selectedProject.projectCategory" />
          </el-descriptions-item>
          <el-descriptions-item label="项目部门">{{ getDeptName(selectedProject.projectDept) }}</el-descriptions-item>
          <el-descriptions-item label="预估工作量">{{ selectedProject.estimatedWorkload || 0 }} 人天</el-descriptions-item>
          <el-descriptions-item label="项目状态">
            <dict-tag :options="sys_xmzt" :value="selectedProject.projectStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="验收状态">
            <dict-tag :options="sys_yszt" :value="selectedProject.acceptanceStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="实际人天">
            {{ selectedProject.actualWorkload != null ? parseFloat(selectedProject.actualWorkload).toFixed(3) : '0.000' }} 人天
          </el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <dict-tag :options="sys_spzt" :value="selectedProject.approvalStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="项目预算">{{ formatAmount(selectedProject.projectBudget) }} 元</el-descriptions-item>
          <el-descriptions-item label="合同状态">
            <dict-tag :options="sys_htzt" :value="selectedProject.contractStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="合同名称" :span="2">{{ selectedProject.contractName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合同编号">{{ selectedProject.contractCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合同金额">
            {{ selectedProject.contractAmount != null ? formatAmount(selectedProject.contractAmount) + ' 元' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="收入确认年度">
            <dict-tag :options="sys_ndgl" :value="selectedProject.revenueConfirmYear" />
          </el-descriptions-item>
          <el-descriptions-item label="收入确认状态">
            <dict-tag :options="sys_qrzt" :value="selectedProject.revenueConfirmStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="项目经理">{{ selectedProject.projectManagerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="市场经理">{{ selectedProject.marketManagerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="客户名称" :span="2">{{ projectCustomerName || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <!-- 本项目已分解的任务 -->
    <el-card v-if="selectedProject" shadow="hover" style="margin-bottom: 15px;">
      <template #header><span style="font-size: 16px; font-weight: bold;">本项目已分解的任务</span></template>
      <el-table :data="siblingTasks" border size="small" style="width: 100%">
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column label="投产批次" prop="batchNo" width="120" align="center" />
        <el-table-column label="任务编号" prop="taskCode" width="130" />
        <el-table-column label="产品" prop="product" width="120">
          <template #default="scope">
            <dict-tag :options="sys_product" :value="scope.row.product" />
          </template>
        </el-table-column>
        <el-table-column label="任务名称" prop="projectName" min-width="160" show-overflow-tooltip />
        <el-table-column label="预估工作量" prop="estimatedWorkload" width="100" align="right" />
        <el-table-column label="功能测试版本日期" prop="functionalTestDate" width="140" align="center">
          <template #default="scope">{{ formatDate(scope.row.functionalTestDate) }}</template>
        </el-table-column>
        <el-table-column label="计划投产日期" prop="planProductionDate" width="120" align="center">
          <template #default="scope">{{ formatDate(scope.row.planProductionDate) }}</template>
        </el-table-column>
        <el-table-column label="任务负责人" prop="projectManagerName" width="100" align="center" />
      </el-table>
    </el-card>

    <!-- 第二步：填写任务信息 -->
    <el-card v-if="selectedProject" shadow="hover" style="margin-bottom: 15px;">
      <template #header><span style="font-size: 16px; font-weight: bold;">二、任务信息</span></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务名称" prop="projectName">
              <el-input v-model="form.projectName" placeholder="请输入任务名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务负责人" prop="projectManagerId">
              <user-select v-model="form.projectManagerId" post-code="pm" placeholder="请选择任务负责人" filterable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务编号" prop="taskCode">
              <el-input v-model="form.taskCode" placeholder="请输入任务编号（选填，最多 20 位）" maxlength="20" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务阶段" prop="projectStage">
              <dict-select v-model="form.projectStage" dict-type="sys_xmjd" placeholder="请选择任务阶段" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="总行需求号">
              <el-input v-model="form.bankDemandNo" placeholder="请输入总行需求号（选填）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="软件中心需求编号">
              <el-input v-model="form.softwareDemandNo" placeholder="请输入软件中心需求编号（选填）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产品">
              <dict-select v-model="form.product" dict-type="sys_product" placeholder="请选择产品" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排期状态">
              <dict-select v-model="form.scheduleStatus" dict-type="sys_pqzt" placeholder="请选择排期状态" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="预估工作量" prop="estimatedWorkload">
              <el-input v-model="form.estimatedWorkload" placeholder="请输入">
                <template #append>人天</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="实际工作量">
              <el-input model-value="0.000" disabled>
                <template #append>人天</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="任务预算">
              <el-input v-model="form.projectBudget" placeholder="请输入金额">
                <template #append>元</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="投产年度">
              <dict-select v-model="form.productionYear" dict-type="sys_ndgl" placeholder="请选择投产年度" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="投产批次">
              <el-select v-model="form.batchId" placeholder="请先选择投产年度" :disabled="!form.productionYear"
                clearable style="width: 100%" @change="onBatchChange">
                <el-option v-for="b in batchOptions" :key="b.batchId" :label="b.batchNo" :value="b.batchId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计划投产日期">
              <span style="line-height: 32px; color: #606266;">{{ planProductionDateDisplay || '-' }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="启动日期" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" placeholder="选择启动日期"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" placeholder="选择结束日期"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="内部B包日期">
              <el-date-picker v-model="form.internalClosureDate" type="date" placeholder="选填"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="功能测试版本日期">
              <el-date-picker v-model="form.functionalTestDate" type="date" placeholder="选填"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="生产版本日期">
              <el-date-picker v-model="form.productionVersionDate" type="date" placeholder="选填"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际投产日期">
              <el-date-picker v-model="form.actualProductionDate" type="date" placeholder="选填"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="功能点说明">
              <el-input v-model="form.functionDescription" type="textarea" :rows="3" placeholder="请输入功能点说明（选填）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="实施计划">
              <el-input v-model="form.implementationPlan" type="textarea" :rows="3" placeholder="请输入实施计划（选填）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注（选填）" />
            </el-form-item>
          </el-col>
        </el-row>

      </el-form>
    </el-card>

    <div v-if="selectedProject" class="form-footer">
      <el-button type="primary" size="large" @click="submitForm">保存</el-button>
      <el-button size="large" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script setup name="TaskDecompose">
import { ref, watch, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { addProject, getProject } from '@/api/project/project'
import request from '@/utils/request'

const { proxy } = getCurrentInstance()
const router = useRouter()

const { industry, sys_yjqy, sys_xmfl, sys_xmzt, sys_yszt, sys_spzt, sys_htzt, sys_ndgl, sys_qrzt, sys_product } =
  proxy.useDict('industry', 'sys_yjqy', 'sys_xmfl', 'sys_xmzt', 'sys_yszt', 'sys_spzt', 'sys_htzt', 'sys_ndgl', 'sys_qrzt', 'sys_product')

const formRef = ref()
const searchDept = ref(null)
const projectKeyword = ref('')
const selectedProject = ref(null)
const projectCustomerName = ref('')
const deptFlatList = ref([])
const batchOptions = ref([])
const planProductionDateDisplay = ref('')
const siblingTasks = ref([])

const form = ref({
  projectName: null, projectManagerId: null, taskCode: null, projectStage: null,
  productionYear: null, batchId: null,
  bankDemandNo: null, softwareDemandNo: null, product: null,
  scheduleStatus: null, functionDescription: null, implementationPlan: null,
  estimatedWorkload: null, projectBudget: null,
  startDate: null, endDate: null,
  internalClosureDate: null, functionalTestDate: null, productionVersionDate: null,
  actualProductionDate: null,
  remark: null
})

const rules = ref({
  taskCode:          [{ max: 20, message: '任务编号最多 20 位', trigger: 'blur' }],
  projectManagerId:  [{ required: true, message: '任务负责人不能为空', trigger: 'change' }],
  projectName:       [{ required: true, message: '任务名称不能为空',   trigger: 'blur'   }],
  projectStage:      [{ required: true, message: '任务阶段不能为空',   trigger: 'change' }],
  estimatedWorkload: [{ required: true, message: '预估工作量不能为空', trigger: 'blur'   }],
  startDate:         [{ required: true, message: '启动日期不能为空',   trigger: 'change' }],
  endDate:           [{ required: true, message: '结束日期不能为空',   trigger: 'change' }]
})

function getDeptName(deptId) {
  if (!deptId) return '-'
  const numId = typeof deptId === 'string' ? parseInt(deptId) : deptId
  const dept = deptFlatList.value.find(d => d.deptId === numId)
  if (!dept) return '-'
  const ancestorIds = dept.ancestors ? dept.ancestors.split(',').filter(id => id && id !== '0') : []
  const pathDepts = []
  if (ancestorIds.length >= 2) {
    for (let i = 2; i < ancestorIds.length; i++) {
      const ancestor = deptFlatList.value.find(d => d.deptId === parseInt(ancestorIds[i]))
      if (ancestor) pathDepts.push(ancestor.deptName)
    }
  }
  pathDepts.push(dept.deptName)
  return pathDepts.join('-')
}

function formatAmount(amount) {
  if (amount === null || amount === undefined || amount === '' || amount === 0) return '-'
  const num = Number(amount)
  if (isNaN(num) || num === 0) return '-'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(val) {
  if (!val) return '-'
  return String(val).substring(0, 10)
}

function onDeptChange() {
  projectKeyword.value = ''
  selectedProject.value = null
  projectCustomerName.value = ''
  siblingTasks.value = []
  resetBatch()
}

function fetchProjectSuggestions(keyword, cb) {
  request({
    url: '/project/project/search',
    method: 'get',
    params: { projectName: keyword, projectDept: searchDept.value || undefined }
  }).then(res => cb(res.data || [])).catch(() => cb([]))
}

async function onProjectSelect(item) {
  try {
    const res = await getProject(item.projectId)
    selectedProject.value = res.data
    projectKeyword.value = item.projectName
    projectCustomerName.value = ''
    loadSiblingTasks(item.projectId)
    if (res.data.customerId) {
      request({ url: `/project/customer/${res.data.customerId}`, method: 'get' })
        .then(r => { projectCustomerName.value = r.data.customerSimpleName || r.data.customerFullName || '' })
        .catch(() => {})
    }
  } catch (e) {
    console.error('加载项目详情失败', e)
  }
}

function onProjectClear() {
  selectedProject.value = null
  projectCustomerName.value = ''
  siblingTasks.value = []
  resetBatch()
}

async function loadSiblingTasks(parentId) {
  try {
    const res = await request({ url: '/project/project/siblingTasks', method: 'get', params: { parentId } })
    siblingTasks.value = res.data || []
  } catch (e) { console.error('加载兄弟任务失败', e) }
}

watch(() => form.value.productionYear, async (year) => {
  form.value.batchId = null
  planProductionDateDisplay.value = ''
  batchOptions.value = []
  if (!year) return
  try {
    const res = await request({ url: '/project/productionBatch/byYear', method: 'get', params: { productionYear: year } })
    batchOptions.value = res.data || []
  } catch (e) { console.error('加载批次失败', e) }
})

function onBatchChange(batchId) {
  const found = batchOptions.value.find(b => b.batchId === batchId)
  planProductionDateDisplay.value = found ? (found.planProductionDate ? found.planProductionDate.substring(0, 10) : '') : ''
}

function resetBatch() {
  form.value.productionYear = null
  form.value.batchId = null
  planProductionDateDisplay.value = ''
  batchOptions.value = []
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    const p = selectedProject.value
    const submitData = {
      ...form.value,
      parentId: p.projectId,
      projectStatus: '0',
      projectLevel: 1,
      projectDept: p.projectDept || null,
      industry: p.industry || null,
      region: p.region || null,
      regionId: p.regionId || null,
      regionCode: p.regionCode || null,
      shortName: p.shortName || null,
      establishedYear: p.establishedYear || null,
      projectCategory: p.projectCategory || null,
      projectBudget: form.value.projectBudget ? parseFloat(String(form.value.projectBudget).replace(/,/g, '')) : null,
      estimatedWorkload: form.value.estimatedWorkload ? parseFloat(form.value.estimatedWorkload) : null
    }
    addProject(submitData).then(() => {
      proxy.$modal.msgSuccess('新增成功')
      router.push('/task/subproject')
    })
  })
}

function cancel() {
  router.push('/task/subproject')
}

onMounted(() => {
  request({ url: '/project/project/deptTreeAll', method: 'get' })
    .then(res => { deptFlatList.value = res.data || [] })
    .catch(() => {})
})
</script>

<style scoped>
.app-container { padding-bottom: 80px; }
.form-footer {
  position: sticky; bottom: 0; padding: 20px;
  background-color: #fff; border-top: 1px solid #dcdfe6;
  text-align: center; z-index: 10;
}
.form-footer .el-button { min-width: 120px; margin: 0 10px; }
</style>
