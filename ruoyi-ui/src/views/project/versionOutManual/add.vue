<template>
  <div class="app-container">
    <h2 style="margin: 0 0 12px 0; font-weight: bold;">新增非批次版本</h2>

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
            <el-form-item label="提交人员">
              <el-input :value="userStore.nickName" readonly placeholder="当前登录用户" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 二、任务信息（非批次手填） -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">二、任务信息（手填）</span></template>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="软件中心任务号" prop="manualTaskNo">
              <el-input v-model="form.manualTaskNo" placeholder="请输入软件中心任务号" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="任务名称" prop="manualTaskName">
              <el-input v-model="form.manualTaskName" placeholder="请输入任务名称" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 三、版本信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">三、版本信息</span></template>
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
              <el-input v-model="form.baseVersionCode" readonly placeholder="选子系统后带出" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="版本类型" prop="versionType">
              <dict-select v-model="form.versionType" dict-type="sys_version_type" placeholder="请选择版本类型" @change="onVersionTypeChange" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="6" v-if="isUpgrade">
            <el-form-item label="升级包初级版本号" prop="outVersion">
              <el-select v-model="form.outVersion" placeholder="请选择初级版本号" filterable clearable style="width:100%" @change="tryGenerate">
                <el-option v-for="o in outVersionOptions" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="isUpgrade ? 6 : 12">
            <el-form-item label="出入库版本号">
              <el-input v-model="form.outLibVersion" readonly placeholder="自动生成">
                <template #append><el-tag type="success" size="small">自动</el-tag></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="isUpgrade ? 6 : 6">
            <el-form-item label="涉及TWS改造" prop="isInvolved">
              <el-radio-group v-model="form.isInvolved"><el-radio value="0">是</el-radio><el-radio value="1">否</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="6" v-if="isUpgrade">
            <el-form-item label="数据库是否修改" prop="dbUpdate">
              <el-radio-group v-model="form.dbUpdate"><el-radio value="0">是</el-radio><el-radio value="1">否</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="!isUpgrade">
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
        <el-row :gutter="20" v-if="isUpgrade">
          <el-col :span="8">
            <el-form-item label="接口是否修改" prop="usbUpdate">
              <el-radio-group v-model="form.usbUpdate"><el-radio value="0">是</el-radio><el-radio value="1">否</el-radio></el-radio-group>
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

      <div style="text-align:center; margin: 20px 0;">
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button @click="cancel">取消</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup name="VersionOutManualAdd">
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import {
  addVersionOutManual, generateOutLibVersion, getBatchByYear, getSysNameByProduct,
  getOutVersionOptions
} from '@/api/project/versionOutManual'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const batchOptions = ref([])
const sysNameOptions = ref([])
const outVersionOptions = ref([])

const form = ref({
  productionYear: null, batchId: null, proBatchNo: null, versionPDate: null,
  product: null, subVersionCode: null,
  manualTaskNo: null, manualTaskName: null,
  sysName: null, baseVersionCode: null, versionType: null, outVersion: null,
  outLibVersion: null, versionCode: null,
  isInvolved: '1', dbUpdate: '1', usbUpdate: '1',
  versionDescr: null, remarks: null
})

const rules = ref({
  productionYear: [{ required: true, message: '投产年份不能为空', trigger: 'change' }],
  batchId:        [{ required: true, message: '投产批次不能为空', trigger: 'change' }],
  product:        [{ required: true, message: '产品不能为空', trigger: 'change' }],
  manualTaskNo:   [{ required: true, message: '软件中心任务号不能为空', trigger: 'blur' }],
  manualTaskName: [{ required: true, message: '任务名称不能为空', trigger: 'blur' }],
  sysName:        [{ required: true, message: '子系统不能为空', trigger: 'change' }],
  versionType:    [{ required: true, message: '版本类型不能为空', trigger: 'change' }],
  versionDescr:   [{ required: true, message: '版本说明不能为空', trigger: 'blur' }],
  isInvolved:     [{ required: true, message: '请选择是否涉及TWS改造', trigger: 'change' }],
  dbUpdate:       [{ required: true, message: '请选择数据库是否修改', trigger: 'change' }],
  usbUpdate:      [{ required: true, message: '请选择接口是否修改', trigger: 'change' }]
})

const isUpgrade = computed(() => form.value.versionType === '5' || form.value.versionType === '6')

async function onYearChange(year) {
  form.value.batchId = null; form.value.versionPDate = null; form.value.proBatchNo = null
  batchOptions.value = []
  if (!year) return
  const res = await getBatchByYear(year)
  batchOptions.value = res.data || []
}

function onBatchChange(batchId) {
  const found = batchOptions.value.find(b => b.batchId === batchId)
  form.value.proBatchNo = found ? found.batchNo : null
  form.value.versionPDate = found && found.planProductionDate ? found.planProductionDate.substring(0, 10) : null
}

async function onProductChange(product) {
  form.value.subVersionCode = product  // 产品即 subVersionCode（与批次一致）
  form.value.sysName = null; form.value.baseVersionCode = null
  sysNameOptions.value = []
  if (!product) return
  const res = await getSysNameByProduct(product)
  sysNameOptions.value = res.data || []
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

async function tryGenerate() {
  const f = form.value
  if (!f.sysName || !f.versionType) return
  if (isUpgrade.value && !f.outVersion) return
  try {
    const res = await generateOutLibVersion(
      { sysName: f.sysName, subVersionCode: f.subVersionCode, versionType: f.versionType, outVersion: f.outVersion },
      { addFlag: '1' }
    )
    f.outLibVersion = res.data?.outLibVersion || ''
    f.versionCode = res.data?.versionCode || ''
  } catch (e) { console.error('生成版本号失败', e) }
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    if (!form.value.outLibVersion) {
      proxy.$modal.msgError('出入库版本号未生成，请检查子系统/版本类型')
      return
    }
    addVersionOutManual(form.value).then(() => {
      proxy.$modal.msgSuccess('新增成功')
      router.back()
    })
  })
}

function cancel() { router.back() }
</script>
