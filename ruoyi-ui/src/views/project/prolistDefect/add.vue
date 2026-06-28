<template>
  <div class="app-container">
    <h2 style="margin: 0 0 12px 0; font-weight: bold;">{{ isEdit ? '编辑问题单及缺陷' : '新增问题单及缺陷' }}</h2>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
      <!-- 一、任务归属 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">一、任务归属</span></template>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="投产年份" prop="productionYear">
              <dict-select v-model="form.productionYear" dict-type="sys_ndgl" placeholder="请选择投产年份" @change="onYearChange" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="投产批次号" prop="batchId">
              <el-select v-model="form.batchId" placeholder="请选择批次" filterable style="width:100%" @change="onBatchChange">
                <el-option v-for="b in batchOptions" :key="b.batchId" :label="b.batchNo" :value="b.batchId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="项目组(部门)" prop="deptId">
              <project-dept-select v-model="form.deptId" placeholder="请选择项目组" filterable @change="onDeptChange" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="软件中心任务号" prop="taskId">
              <el-select v-model="form.taskId" placeholder="请先选年份+批次" filterable style="width:100%" @change="onTaskSelect">
                <el-option v-for="t in taskOptions" :key="t.taskId" :label="t.taskCode" :value="t.taskId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert v-if="!taskOptionsReady" type="info" :closable="false" show-icon
          title="请先选择 投产年份 + 投产批次，任务号下拉才有数据（选项目组可进一步过滤）" style="margin-top: 5px;" />
      </el-card>

      <!-- 二、任务信息（只读，选任务号后回显） -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">二、任务信息（只读）</span></template>
        <el-row :gutter="20">
          <el-col :span="8"><el-form-item label="任务名称"><el-input v-model="form.taskName" readonly /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="二级产品"><el-input v-model="form.product" readonly /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="提交内部测试B包日期"><el-input v-model="form.internalClosureDate" readonly /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8"><el-form-item label="提交功能测试版本日期"><el-input v-model="form.functionalTestDate" readonly /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="提交生产版本日期"><el-input v-model="form.productionVersionDate" readonly /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="计划投产日期"><el-input v-model="form.planProductionDate" readonly placeholder="选批次后自动带出" /></el-form-item></el-col>
        </el-row>
      </el-card>

      <!-- 三、问题单及缺陷 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">三、问题单及缺陷</span></template>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="问题单编号" prop="problemNo">
              <el-input v-model="form.problemNo" placeholder="请输入问题单编号" maxlength="128" @blur="onProblemNoBlur" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="问题单级别" prop="problemLevel">
              <dict-select v-model="form.problemLevel" dict-type="sys_problem_level" placeholder="请选择级别" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="提交日期" prop="submitDate">
              <el-date-picker v-model="form.submitDate" type="date" value-format="YYYY-MM-DD" placeholder="提交日期" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="问题单关闭日期">
              <el-date-picker v-model="form.settleDate" type="date" value-format="YYYY-MM-DD" placeholder="解决/关闭日期(可空)" clearable style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否缺陷" prop="whetherDefect">
              <el-radio-group v-model="form.whetherDefect"><el-radio value="1">是</el-radio><el-radio value="0">否</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否超时" prop="whetherOvertime">
              <el-radio-group v-model="form.whetherOvertime"><el-radio value="1">是</el-radio><el-radio value="0">否</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="是否问题重现" prop="whetherProRecurrence">
              <el-radio-group v-model="form.whetherProRecurrence"><el-radio value="1">是</el-radio><el-radio value="0">否</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否须关注" prop="whetherAttRequired">
              <el-radio-group v-model="form.whetherAttRequired"><el-radio value="1">是</el-radio><el-radio value="0">否</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="缺陷说明/超时说明" prop="defectDesc">
              <el-input v-model="form.defectDesc" placeholder="请输入缺陷说明/超时说明" maxlength="128" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="是否更新版本" prop="whetherUpdateVersion">
              <el-radio-group v-model="form.whetherUpdateVersion"><el-radio value="1">是</el-radio><el-radio value="0">否</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="当前状态" prop="currentStatus">
              <dict-select v-model="form.currentStatus" dict-type="sys_problem_state" placeholder="请选择当前状态" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="核查日期" prop="verifyDate">
              <el-date-picker v-model="form.verifyDate" type="date" value-format="YYYY-MM-DD" placeholder="核查日期" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="请输入备注" maxlength="2048" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <div style="text-align:center; margin: 20px 0;">
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button @click="cancel">取消</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup name="ProlistDefectAdd">
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addProlistDefect, updateProlistDefect, getProlistDefect,
  getBatchByYear, getTcDate, getTaskOptions, getTaskInfo, checkProblemNo
} from '@/api/project/prolistDefect'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const formRef = ref()
const batchOptions = ref([])
const taskOptions = ref([])

const isEdit = computed(() => !!route.query.problemId)

const form = ref({
  problemId: null,
  productionYear: null, batchId: null, planProductionDate: null, deptId: null, taskId: null,
  taskName: null, product: null, internalClosureDate: null, functionalTestDate: null, productionVersionDate: null,
  problemNo: null, problemLevel: null, currentStatus: null,
  submitDate: null, settleDate: null, verifyDate: null,
  whetherDefect: '0', whetherOvertime: '0', whetherProRecurrence: '0', whetherAttRequired: '0', whetherUpdateVersion: '0',
  defectDesc: null, remarks: null
})

const rules = ref({
  productionYear: [{ required: true, message: '投产年份不能为空', trigger: 'change' }],
  batchId:        [{ required: true, message: '投产批次不能为空', trigger: 'change' }],
  deptId:         [{ required: true, message: '项目组不能为空', trigger: 'change' }],
  taskId:         [{ required: true, message: '软件中心任务号不能为空', trigger: 'change' }],
  problemNo:      [{ required: true, message: '问题单编号不能为空', trigger: 'blur' }],
  problemLevel:   [{ required: true, message: '问题单级别不能为空', trigger: 'change' }],
  currentStatus:  [{ required: true, message: '当前状态不能为空', trigger: 'change' }],
  submitDate:     [{ required: true, message: '提交日期不能为空', trigger: 'change' }],
  verifyDate:     [{ required: true, message: '核查日期不能为空', trigger: 'change' }],
  whetherDefect:  [{ required: true, message: '请选择是否缺陷', trigger: 'change' }],
  whetherOvertime:[{ required: true, message: '请选择是否超时', trigger: 'change' }],
  whetherProRecurrence:[{ required: true, message: '请选择是否问题重现', trigger: 'change' }],
  whetherAttRequired:[{ required: true, message: '请选择是否须关注', trigger: 'change' }],
  whetherUpdateVersion:[{ required: true, message: '请选择是否更新版本', trigger: 'change' }],
  defectDesc:     [{ required: true, message: '缺陷说明不能为空', trigger: 'blur' }]
})

const taskOptionsReady = computed(() => !!(form.value.productionYear && form.value.batchId))

async function onYearChange(year) {
  form.value.batchId = null
  form.value.planProductionDate = null
  resetTask()
  batchOptions.value = []
  if (!year) return
  const res = await getBatchByYear(year)
  batchOptions.value = res.data || []
}

async function onBatchChange(batchId) {
  resetTask()
  if (!batchId) { form.value.planProductionDate = null; return }
  const res = await getTcDate(batchId)
  form.value.planProductionDate = res.data || null
  loadTaskOptions()
}

function onDeptChange() {
  resetTask()
  loadTaskOptions()
}

function resetTask() {
  form.value.taskId = null
  form.value.taskName = null
  form.value.product = null
  form.value.internalClosureDate = null
  form.value.functionalTestDate = null
  form.value.productionVersionDate = null
  taskOptions.value = []
}

async function loadTaskOptions() {
  const f = form.value
  if (!(f.productionYear && f.batchId)) return
  const res = await getTaskOptions(f.productionYear, f.batchId, f.deptId)
  taskOptions.value = res.data || []
}

async function onTaskSelect(taskId) {
  if (!taskId) return
  const res = await getTaskInfo(taskId)
  const t = res.data || {}
  form.value.taskName = t.taskName
  form.value.product = t.product
  form.value.internalClosureDate = t.internalClosureDate
  form.value.functionalTestDate = t.functionalTestDate
  form.value.productionVersionDate = t.productionVersionDate
}

// 问题单编号查重（失焦）
async function onProblemNoBlur() {
  if (!form.value.problemNo) return
  const res = await checkProblemNo(form.value.problemNo, form.value.problemId)
  if (res.data === false) {
    proxy.$modal.msgError('问题单编号已存在：' + form.value.problemNo)
  }
}

function submitForm() {
  formRef.value.validate(async valid => {
    if (!valid) return
    // 保存前再查重一次
    const chk = await checkProblemNo(form.value.problemNo, form.value.problemId)
    if (chk.data === false) {
      proxy.$modal.msgError('问题单编号已存在：' + form.value.problemNo)
      return
    }
    const action = isEdit.value ? updateProlistDefect : addProlistDefect
    action(form.value).then(() => {
      proxy.$modal.msgSuccess(isEdit.value ? '修改成功' : '新增成功')
      router.back()
    })
  })
}

function cancel() {
  router.back()
}

async function loadForEdit() {
  const res = await getProlistDefect(route.query.problemId)
  const d = res.data
  Object.assign(form.value, d)
  // 编辑时回填联动下拉数据源
  if (d.productionYear) {
    const b = await getBatchByYear(d.productionYear)
    batchOptions.value = b.data || []
  }
  if (d.productionYear && d.batchId) {
    const t = await getTaskOptions(d.productionYear, d.batchId, d.deptId)
    taskOptions.value = t.data || []
  }
}

onMounted(() => {
  if (isEdit.value) loadForEdit()
})
</script>
