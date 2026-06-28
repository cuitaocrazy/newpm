<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="120px">
      <el-form-item label="投产年份" prop="productionYear">
        <dict-select v-model="queryParams.productionYear" dict-type="sys_ndgl" placeholder="全部" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="投产批次号" prop="proBatchNo">
        <el-input v-model="queryParams.proBatchNo" placeholder="批次号" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="软件中心任务号" prop="manualTaskNo">
        <el-input v-model="queryParams.manualTaskNo" placeholder="任务号" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="产品" prop="product">
        <dict-select v-model="queryParams.product" dict-type="sys_product" placeholder="全部" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="版本类型" prop="versionType">
        <dict-select v-model="queryParams.versionType" dict-type="sys_version_type" placeholder="全部" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="基准版本号" prop="baseVersionCode">
        <el-input v-model="queryParams.baseVersionCode" placeholder="基准版本号" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="出入库版本号" prop="outLibVersion">
        <el-input v-model="queryParams.outLibVersion" placeholder="模糊匹配" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="投产日期" prop="versionPDate">
        <el-date-picker v-model="queryParams.versionPDate" type="date" value-format="YYYY-MM-DD" placeholder="投产日期" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['project:versionOutManual:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['project:versionOutManual:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="versionList" border @sort-change="handleSortChange">
      <el-table-column type="index" label="序号" width="55" align="center" fixed="left" />
      <el-table-column label="出入库版本号" prop="outLibVersion" min-width="150" show-overflow-tooltip fixed="left" />
      <el-table-column label="投产年份" prop="productionYear" width="90" align="center" />
      <el-table-column label="投产批次号" prop="proBatchNo" width="110" align="center" />
      <el-table-column label="产品" prop="product" width="110" align="center" />
      <el-table-column label="软件中心任务号" prop="manualTaskNo" min-width="140" show-overflow-tooltip />
      <el-table-column label="任务名称" prop="manualTaskName" min-width="160" show-overflow-tooltip />
      <el-table-column label="基准版本号" prop="baseVersionCode" width="120" align="center" />
      <el-table-column label="版本编号" prop="versionCode" width="90" align="center" />
      <el-table-column label="版本类型" prop="versionType" width="110" align="center">
        <template #default="{ row }"><dict-tag :options="sys_version_type" :value="row.versionType" /></template>
      </el-table-column>
      <el-table-column label="提交人员" prop="userName" width="100" align="center" />
      <el-table-column label="投产日期" prop="versionPDate" width="110" align="center" />
      <el-table-column label="涉及TWS改造" width="100" align="center">
        <template #default="{ row }">{{ row.isInvolved === '0' ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="数据库是否修改" width="110" align="center">
        <template #default="{ row }">{{ row.dbUpdate === '0' ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="接口是否修改" width="110" align="center">
        <template #default="{ row }">{{ row.usbUpdate === '0' ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="版本说明" prop="versionDescr" min-width="140" show-overflow-tooltip />
      <el-table-column label="创建时间" prop="createTime" width="160" align="center" sortable="custom" />
      <el-table-column label="修改时间" prop="updateTime" width="160" align="center" sortable="custom" />
      <el-table-column label="备注" prop="remarks" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link icon="View" @click="handleDetail(row)" v-hasPermi="['project:versionOutManual:query']">详情</el-button>
          <el-button type="primary" link icon="Edit" @click="handleEdit(row)" v-hasPermi="['project:versionOutManual:edit']">编辑</el-button>
          <el-button type="danger" link icon="Delete" @click="handleDelete(row)" v-hasPermi="['project:versionOutManual:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="VersionOutManual">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { listVersionOutManual, delVersionOutManual } from '@/api/project/versionOutManual'

const { proxy } = getCurrentInstance()
const router = useRouter()
const { sys_version_type } = proxy.useDict('sys_version_type')

const SEARCH_STATE_KEY = 'version_out_manual_search_state'

const versionList = ref([])
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1, pageSize: 10,
    productionYear: null, proBatchNo: null, manualTaskNo: null, product: null,
    versionType: null, baseVersionCode: null, outLibVersion: null, versionPDate: null,
    orderByColumn: null, isAsc: null
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listVersionOutManual(queryParams.value).then(res => {
    versionList.value = res.rows
    total.value = res.total
    loading.value = false
  }).catch(() => { loading.value = false })
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }

function resetQuery() {
  sessionStorage.removeItem(SEARCH_STATE_KEY)
  proxy.resetForm('queryRef')
  queryParams.value.orderByColumn = null; queryParams.value.isAsc = null
  handleQuery()
}

function handleSortChange({ prop, order }) {
  queryParams.value.orderByColumn = prop
  queryParams.value.isAsc = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : null
  handleQuery()
}

function handleAdd() { router.push('/project/versionOutManual/add') }
function handleDetail(row) { router.push(`/project/versionOutManual/detail/${row.id}`) }
function handleEdit(row) { router.push(`/project/versionOutManual/edit/${row.id}`) }

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除出入库版本号「${row.outLibVersion}」？`).then(() => {
    return delVersionOutManual(row.id)
  }).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

function handleExport() {
  proxy.download('/project/versionOutManual/export', { ...queryParams.value }, `非批次版本_${new Date().getTime()}.xlsx`)
}

function saveSearchState() { sessionStorage.setItem(SEARCH_STATE_KEY, JSON.stringify({ queryParams: queryParams.value })) }
function restoreSearchState() {
  try {
    const raw = sessionStorage.getItem(SEARCH_STATE_KEY)
    if (!raw) return
    Object.assign(queryParams.value, JSON.parse(raw).queryParams)
    sessionStorage.removeItem(SEARCH_STATE_KEY)
  } catch {}
}

onBeforeRouteLeave(() => saveSearchState())
onMounted(() => { restoreSearchState(); getList() })
</script>
