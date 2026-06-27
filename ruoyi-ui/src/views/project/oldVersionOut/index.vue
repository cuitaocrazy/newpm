<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="任务编号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" placeholder="模糊匹配" clearable style="width:160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="投产批次号" prop="proBatchNo">
        <el-select v-model="queryParams.proBatchNo" placeholder="全部" clearable filterable style="width:160px">
          <el-option v-for="v in proBatchNoOptions" :key="v" :label="v" :value="v" />
        </el-select>
      </el-form-item>
      <el-form-item label="子产品" prop="product">
        <el-select v-model="queryParams.product" placeholder="全部" clearable filterable style="width:160px">
          <el-option v-for="v in productOptions" :key="v" :label="v" :value="v" />
        </el-select>
      </el-form-item>
      <el-form-item label="版本类型" prop="versionType">
        <el-select v-model="queryParams.versionType" placeholder="全部" clearable filterable style="width:160px">
          <el-option v-for="v in versionTypeOptions" :key="v" :label="v" :value="v" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="子系统名称" prop="sysName" min-width="130" show-overflow-tooltip fixed="left" />
      <el-table-column label="子产品名称" prop="product" width="110" align="center" />
      <el-table-column label="基准版本号" prop="baseVersionCode" width="120" align="center" />
      <el-table-column label="出入库版本号" prop="outLibVersion" min-width="150" show-overflow-tooltip />
      <el-table-column label="版本类型" prop="versionType" width="110" align="center" />
      <el-table-column label="版本编号" prop="versionCode" width="90" align="center" />
      <el-table-column label="提交人员" prop="commName" width="100" align="center" />
      <el-table-column label="版本投产日期" prop="versionPDate" width="120" align="center" />
      <el-table-column label="版本说明" prop="versionDescr" min-width="140" show-overflow-tooltip />
      <el-table-column label="备注" prop="remarks" min-width="120" show-overflow-tooltip />
      <el-table-column label="任务编号" prop="taskNo" min-width="120" show-overflow-tooltip />
      <el-table-column label="任务名称" prop="taskName" min-width="160" show-overflow-tooltip />
      <el-table-column label="投产年份" prop="proYear" width="90" align="center" />
      <el-table-column label="投产批次号" prop="proBatchNo" width="110" align="center" />
      <el-table-column label="是否涉及TWS改造" prop="isInvolved" width="130" align="center">
        <template #default="{ row }">{{ row.isInvolved === '0' ? '是' : row.isInvolved === '1' ? '否' : '' }}</template>
      </el-table-column>
      <el-table-column label="数据库是否修改" prop="dbUpdate" width="120" align="center">
        <template #default="{ row }">{{ row.dbUpdate === '0' ? '是' : row.dbUpdate === '1' ? '否' : '' }}</template>
      </el-table-column>
      <el-table-column label="接口是否修改" prop="usbUpdate" width="110" align="center">
        <template #default="{ row }">{{ row.usbUpdate === '0' ? '是' : row.usbUpdate === '1' ? '否' : '' }}</template>
      </el-table-column>
      <el-table-column label="顺序号" prop="sequenceNo" width="80" align="center" />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="OldVersionOut">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from 'vue'
import { listOldVersionOut, getProBatchNoOptions, getProductOptions, getVersionTypeOptions } from '@/api/project/oldVersionOut'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const dataList = ref([])
const proBatchNoOptions = ref([])
const productOptions = ref([])
const versionTypeOptions = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    taskNo: undefined,
    proBatchNo: undefined,
    product: undefined,
    versionType: undefined
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listOldVersionOut(queryParams.value).then(res => {
    dataList.value = res.rows
    total.value = res.total
  }).finally(() => {
    loading.value = false
  })
}

function loadOptions() {
  getProBatchNoOptions().then(res => { proBatchNoOptions.value = res.data || [] })
  getProductOptions().then(res => { productOptions.value = res.data || [] })
  getVersionTypeOptions().then(res => { versionTypeOptions.value = res.data || [] })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

onMounted(() => {
  loadOptions()
  getList()
})
</script>
