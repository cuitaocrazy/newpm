<template>
  <div class="app-container" v-loading="loading">
    <h2 style="margin: 0 0 12px 0; font-weight: bold;">编辑批次版本</h2>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
      <!-- 一、版本归属 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">一、版本归属</span></template>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="投产年份" prop="productionYear">
              <dict-select v-model="form.productionYear" dict-type="sys_ndgl" placeholder="请选择投产年份" @change="onYearChange" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="投产批次" prop="batchId">
              <el-select v-model="form.batchId" placeholder="请选择批次" filterable style="width:100%" @change="onBatchChange">
                <el-option v-for="b in batchOptions" :key="b.batchId" :label="b.batchNo" :value="b.batchId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="版本投产日期">
              <el-input v-model="form.versionPDate" placeholder="选批次后自动带出" readonly />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="产品" prop="product">
              <dict-select v-model="form.product" dict-type="sys_product" placeholder="请选择产品" @change="onProductChange" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="提交人员" prop="commName">
              <user-select v-model="form.commName" placeholder="提交人员" filterable />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 二、版本信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">二、版本信息</span></template>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="子系统" prop="sysName">
              <el-select v-model="form.sysName" placeholder="请先选产品" filterable style="width:100%" @change="onSysNameChange">
                <el-option v-for="s in sysNameOptions" :key="s.id" :label="s.sysName" :value="s.sysName" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="基准版本号">
              <el-input v-model="form.baseVersionCode" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="版本类型" prop="versionType">
              <dict-select v-model="form.versionType" dict-type="sys_version_type" placeholder="请选择版本类型" @change="onVersionTypeChange" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="isUpgrade">
          <el-col :span="8">
            <el-form-item label="升级包初级版本号" prop="outVersion">
              <el-select v-model="form.outVersion" placeholder="请选择初级版本号" filterable clearable style="width:100%" @change="tryGenerate">
                <el-option v-for="o in outVersionOptions" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="出入库版本号">
              <el-input v-model="form.outLibVersion" readonly placeholder="自动生成">
                <template #append><el-tag type="success" size="small">自动</el-tag></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="组包方式" prop="packageMode">
              <dict-select v-model="form.packageMode" dict-type="sys_package_mode" placeholder="请选择组包方式" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="版本状态" prop="versionStatus">
              <dict-select v-model="form.versionStatus" dict-type="sys_version_status" placeholder="可选（待生产数据）" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="涉及TWS改造" prop="isInvolved">
              <el-radio-group v-model="form.isInvolved"><el-radio value="0">是</el-radio><el-radio value="1">否</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数据库是否修改" prop="dbUpdate">
              <el-radio-group v-model="form.dbUpdate"><el-radio value="0">是</el-radio><el-radio value="1">否</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="接口是否修改" prop="usbUpdate">
              <el-radio-group v-model="form.usbUpdate"><el-radio value="0">是</el-radio><el-radio value="1">否</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="版本简介" prop="versionBrief">
              <el-input v-model="form.versionBrief" placeholder="请输入版本简介" maxlength="512" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="版本说明" prop="versionDescr">
              <el-input v-model="form.versionDescr" type="textarea" :rows="2" placeholder="请输入版本说明" maxlength="512" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 三、关联任务 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header>
          <span style="font-size: 16px; font-weight: bold;">三、关联任务</span>
          <el-button type="primary" link style="float:right" @click="addTaskRow" :disabled="!taskOptionsReady">+ 添加任务</el-button>
        </template>
        <el-alert v-if="!taskOptionsReady" type="info" :closable="false" show-icon
          title="请先选择 投产年份 + 投产批次 + 产品，任务号下拉才有数据" style="margin-bottom: 10px;" />
        <el-table :data="form.taskList" border size="small">
          <el-table-column label="软件中心任务号" min-width="200">
            <template #default="{ row }">
              <el-select v-model="row.taskId" placeholder="请选择任务号" filterable style="width:100%" @change="onTaskSelect(row)">
                <el-option v-for="t in taskOptions" :key="t.taskId" :label="t.taskNo" :value="t.taskId" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="任务名称" prop="taskName" min-width="160" show-overflow-tooltip />
          <el-table-column label="项目名称" prop="prjName" min-width="160" show-overflow-tooltip />
          <el-table-column label="需求名称" prop="demandName" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="80">
            <template #default="{ $index }">
              <el-button type="danger" link @click="removeTaskRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <div style="text-align:center; margin: 20px 0;">
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button @click="cancel">取消</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup name="VersionOutEdit">
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  getVersionOut, updateVersionOut, generateOutLibVersion, getBatchByYear,
  getSysNameByProduct, getOutVersionOptions, getTaskOptions
} from '@/api/project/versionOut'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const formRef = ref()
const loading = ref(false)
const batchOptions = ref([])
const sysNameOptions = ref([])
const outVersionOptions = ref([])
const taskOptions = ref([])
// 记录原始关键字段，重算时传给后端
const original = ref({ sysName: null, versionType: null, subVersionCode: null })

const form = ref({ taskList: [] })

const rules = ref({
  productionYear: [{ required: true, message: '投产年份不能为空', trigger: 'change' }],
  batchId:        [{ required: true, message: '投产批次不能为空', trigger: 'change' }],
  product:        [{ required: true, message: '产品不能为空', trigger: 'change' }],
  sysName:        [{ required: true, message: '子系统不能为空', trigger: 'change' }],
  versionType:    [{ required: true, message: '版本类型不能为空', trigger: 'change' }],
  packageMode:    [{ required: true, message: '组包方式不能为空', trigger: 'change' }],
  versionBrief:   [{ required: true, message: '版本简介不能为空', trigger: 'blur' }],
  versionDescr:   [{ required: true, message: '版本说明不能为空', trigger: 'blur' }],
  isInvolved:     [{ required: true, message: '请选择是否涉及TWS改造', trigger: 'change' }],
  dbUpdate:       [{ required: true, message: '请选择数据库是否修改', trigger: 'change' }],
  usbUpdate:      [{ required: true, message: '请选择接口是否修改', trigger: 'change' }]
})

const isUpgrade = computed(() => form.value.versionType === '5' || form.value.versionType === '6')
const taskOptionsReady = computed(() => !!(form.value.productionYear && form.value.batchId && form.value.product))

async function onYearChange(year) {
  form.value.batchId = null; form.value.versionPDate = null; form.value.proBatchNo = null
  batchOptions.value = []
  resetTaskRows()
  if (!year) return
  const res = await getBatchByYear(year)
  batchOptions.value = res.data || []
}

function onBatchChange(batchId) {
  const found = batchOptions.value.find(b => b.batchId === batchId)
  form.value.proBatchNo = found ? found.batchNo : null
  // 版本投产日期 = 批次的计划投产日期（直接取自批次选项，与任务管理一致）
  form.value.versionPDate = found && found.planProductionDate ? found.planProductionDate.substring(0, 10) : null
  resetTaskRows()
  if (batchId) loadTaskOptions()
}

async function onProductChange(product) {
  form.value.subVersionCode = product  // 产品即 subVersionCode，两列同值
  form.value.sysName = null; form.value.baseVersionCode = null
  sysNameOptions.value = []
  resetTaskRows()
  if (!product) return
  const res = await getSysNameByProduct(product)
  sysNameOptions.value = res.data || []
  loadTaskOptions()
}

function resetTaskRows() {
  form.value.taskList = []
  taskOptions.value = []
}

async function loadTaskOptions() {
  const f = form.value
  if (!(f.productionYear && f.batchId && f.product)) return
  const res = await getTaskOptions(f.productionYear, f.batchId, f.product)
  taskOptions.value = res.data || []
}

function onSysNameChange(sysName) {
  const found = sysNameOptions.value.find(s => s.sysName === sysName)
  form.value.baseVersionCode = found ? found.baseVersionCode : null
  tryGenerate()
}

async function onVersionTypeChange() {
  form.value.outVersion = null
  outVersionOptions.value = []
  if (isUpgrade.value && form.value.sysName) {
    const res = await getOutVersionOptions(form.value.sysName, form.value.versionType)
    outVersionOptions.value = res.data || []
  }
  tryGenerate()
}

// 编辑：关键字段未变沿用原号，变了用 addFlag=2 预览重算
async function tryGenerate() {
  const f = form.value
  if (!f.sysName || !f.versionType) return
  if (isUpgrade.value && !f.outVersion) return
  const keyChanged = f.sysName !== original.value.sysName
    || f.versionType !== original.value.versionType
    || f.subVersionCode !== original.value.subVersionCode
  if (!keyChanged) return
  try {
    const res = await generateOutLibVersion(
      { id: f.id, sysName: f.sysName, subVersionCode: f.subVersionCode, versionType: f.versionType, outVersion: f.outVersion },
      { addFlag: '2', oldSubVersionCode: original.value.subVersionCode, oldVersionType: original.value.versionType }
    )
    f.outLibVersion = res.data?.outLibVersion || f.outLibVersion
    f.versionCode = res.data?.versionCode || f.versionCode
  } catch (e) { console.error('重算版本号失败', e) }
}

function addTaskRow() { form.value.taskList.push({ taskNo: '', taskId: null, taskName: '', prjName: '', demandName: '' }) }
function removeTaskRow(index) { form.value.taskList.splice(index, 1) }

function onTaskSelect(row) {
  const opt = taskOptions.value.find(t => t.taskId === row.taskId)
  if (opt) {
    row.taskNo = opt.taskNo; row.taskName = opt.taskName
    row.prjName = opt.prjName; row.demandName = opt.demandName
  }
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    updateVersionOut(form.value).then(() => {
      proxy.$modal.msgSuccess('修改成功')
      router.back()
    })
  })
}

function cancel() { router.back() }

onMounted(async () => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    const res = await getVersionOut(id)
    const d = res.data || {}
    if (!d.taskList) d.taskList = []
    if (d.commName) d.commName = Number(d.commName)  // user-select 需数字 userId 才能解析姓名
    form.value = d
    original.value = { sysName: d.sysName, versionType: d.versionType, subVersionCode: d.subVersionCode }
    // 重建级联下拉选项
    if (d.productionYear) { const r = await getBatchByYear(d.productionYear); batchOptions.value = r.data || [] }
    if (d.product) { const r = await getSysNameByProduct(d.product); sysNameOptions.value = r.data || [] }
    if ((d.versionType === '5' || d.versionType === '6') && d.sysName) {
      const r = await getOutVersionOptions(d.sysName, d.versionType); outVersionOptions.value = r.data || []
    }
    await loadTaskOptions()  // 让已关联任务行的下拉能正确显示
  } finally { loading.value = false }
})
</script>
