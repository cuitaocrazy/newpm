<template>
  <div class="app-container">
    <h2 style="margin: 0 0 12px 0; font-weight: bold;">{{ isEdit ? '编辑非批次问题单及缺陷' : '新增非批次问题单及缺陷' }}</h2>

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
              <el-select v-if="!isMigrated" v-model="form.batchId" placeholder="请选择批次" filterable style="width:100%" @change="onBatchChange">
                <el-option v-for="b in batchOptions" :key="b.batchId" :label="b.batchNo" :value="b.batchId" />
              </el-select>
              <el-input v-else :model-value="form.batchNo" readonly placeholder="—（迁移数据）" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计划投产日期">
              <el-input v-model="form.planProductionDate" placeholder="选批次后自动带出" readonly />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="项目组(部门)" prop="deptId">
              <project-dept-select v-if="!isMigrated" v-model="form.deptId" placeholder="请选择项目组" filterable />
              <el-input v-else :model-value="form.deptName" readonly placeholder="—（迁移数据）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 二、任务信息（全部手填/手选） -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">二、任务信息（手填）</span></template>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="软件中心任务号" prop="taskNo">
              <el-input v-model="form.taskNo" placeholder="请输入软件中心任务号" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="任务名称">
              <el-input v-model="form.taskName" placeholder="请输入任务名称" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="二级产品">
              <dict-select v-model="form.product" dict-type="sys_product" placeholder="请选择二级产品" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="提交内部测试B包日期">
              <el-date-picker v-model="form.internalClosureDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" clearable style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="提交功能测试版本日期">
              <el-date-picker v-model="form.functionalTestDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" clearable style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="提交生产版本日期">
              <el-date-picker v-model="form.productionVersionDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" clearable style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="排期状态">
              <dict-select v-model="form.scheduleStatus" dict-type="sys_pqzt" placeholder="请选择排期状态" clearable />
            </el-form-item>
          </el-col>
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

<script setup name="NobatchProlistAdd">
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addNobatchProlist, updateNobatchProlist, getNobatchProlist,
  getBatchByYear, getTcDate, checkProblemNo
} from '@/api/project/nobatchProlist'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const formRef = ref()
const batchOptions = ref([])
const isEdit = computed(() => !!route.query.problemId)
// 迁移记录: 编辑时 FK(批次/部门)为空, 仅有快照文本(batchNo/deptName)。newpm 无对应主数据无法挂FK,
// 故这2字段改快照只读回显, 校验放行可保存(软件中心任务号是纯文本不受影响)。
const isMigrated = computed(() => isEdit.value && !form.value.batchId && !form.value.deptId)

const form = ref({
  problemId: null,
  productionYear: null, batchId: null, planProductionDate: null, deptId: null,
  taskNo: null, taskName: null, product: null,
  internalClosureDate: null, functionalTestDate: null, productionVersionDate: null, scheduleStatus: null,
  problemNo: null, problemLevel: null, currentStatus: null,
  submitDate: null, settleDate: null, verifyDate: null,
  whetherDefect: '0', whetherOvertime: '0', whetherProRecurrence: '0', whetherAttRequired: '0', whetherUpdateVersion: '0',
  defectDesc: null, remarks: null
})

const rules = ref({
  productionYear: [{ required: true, message: '投产年份不能为空', trigger: 'change' }],
  batchId:        [{ validator: (r, v, cb) => (v || form.value.batchNo) ? cb() : cb(new Error('投产批次不能为空')), trigger: 'change' }],
  deptId:         [{ validator: (r, v, cb) => (v || form.value.deptName) ? cb() : cb(new Error('项目组不能为空')), trigger: 'change' }],
  taskNo:         [{ required: true, message: '软件中心任务号不能为空', trigger: 'blur' }],
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

async function onYearChange(year) {
  form.value.batchId = null
  form.value.planProductionDate = null
  batchOptions.value = []
  if (!year) return
  const res = await getBatchByYear(year)
  batchOptions.value = res.data || []
}

async function onBatchChange(batchId) {
  if (!batchId) { form.value.planProductionDate = null; return }
  const res = await getTcDate(batchId)
  form.value.planProductionDate = res.data || null
}

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
    const chk = await checkProblemNo(form.value.problemNo, form.value.problemId)
    if (chk.data === false) {
      proxy.$modal.msgError('问题单编号已存在：' + form.value.problemNo)
      return
    }
    const action = isEdit.value ? updateNobatchProlist : addNobatchProlist
    action(form.value).then(() => {
      proxy.$modal.msgSuccess(isEdit.value ? '修改成功' : '新增成功')
      router.back()
    })
  })
}

function cancel() { router.back() }

async function loadForEdit() {
  const res = await getNobatchProlist(route.query.problemId)
  Object.assign(form.value, res.data)
  if (res.data.productionYear) {
    const b = await getBatchByYear(res.data.productionYear)
    batchOptions.value = b.data || []
  }
}

onMounted(() => {
  if (isEdit.value) loadForEdit()
})
</script>
