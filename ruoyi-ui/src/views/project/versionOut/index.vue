<template>
  <div class="app-container">
    <!-- 查询表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="120px">
      <el-form-item label="投产年份" prop="productionYear">
        <dict-select v-model="queryParams.productionYear" dict-type="sys_ndgl" placeholder="全部" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="产品" prop="product">
        <dict-select v-model="queryParams.product" dict-type="sys_product" placeholder="全部" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="版本类型" prop="versionType">
        <dict-select v-model="queryParams.versionType" dict-type="sys_version_type" placeholder="全部" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="子系统" prop="sysName">
        <el-input v-model="queryParams.sysName" placeholder="子系统名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="出入库版本号" prop="outLibVersion">
        <el-input v-model="queryParams.outLibVersion" placeholder="模糊匹配" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="组包方式" prop="packageMode">
        <dict-select v-model="queryParams.packageMode" dict-type="sys_package_mode" placeholder="全部" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="投产批次号" prop="proBatchNo">
        <el-input v-model="queryParams.proBatchNo" placeholder="批次号" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="软件中心任务号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" placeholder="任务号" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="基准版本号" prop="baseVersionCode">
        <el-input v-model="queryParams.baseVersionCode" placeholder="基准版本号" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="版本状态" prop="versionStatus">
        <dict-select v-model="queryParams.versionStatus" dict-type="sys_version_status" placeholder="全部" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="投产日期" prop="versionPDate">
        <el-date-picker v-model="queryParams.versionPDate" type="date" value-format="YYYY-MM-DD" placeholder="投产日期" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item label="提交人员" prop="commName">
        <user-select v-model="queryParams.commName" placeholder="提交人员" clearable filterable width="240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['project:versionOut:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['project:versionOut:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="versionList" border @sort-change="handleSortChange">
      <el-table-column type="index" label="序号" width="55" align="center" fixed="left" />
      <el-table-column label="出入库版本号" prop="outLibVersion" min-width="150" show-overflow-tooltip fixed="left" />
      <el-table-column label="软件中心任务号" prop="taskNos" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <template v-for="(t, i) in taskCellTokens(row)" :key="i">
            <span v-if="i > 0">, </span>
            <router-link v-if="t.type === 'L'" class="task-link" :to="`/task/subproject/detail/${t.id}`">{{ t.no }}</router-link>
            <router-link v-else-if="t.type === 'S'" class="task-link" :to="`/project/versionOut/taskSnapshot?legacyId=${t.id}`">{{ t.no }}</router-link>
            <span v-else>{{ t.no }}</span>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="投产年份" prop="productionYear" width="90" align="center" />
      <el-table-column label="投产批次号" prop="proBatchNo" width="110" align="center" />
      <el-table-column label="产品" prop="product" width="110" align="center" />
      <el-table-column label="子系统" prop="sysName" min-width="120" show-overflow-tooltip />
      <el-table-column label="版本简介" prop="versionBrief" min-width="140" show-overflow-tooltip />
      <el-table-column label="基准版本号" prop="baseVersionCode" width="120" align="center" />
      <el-table-column label="版本编号" prop="versionCode" width="100" align="center" />
      <el-table-column label="版本类型" prop="versionType" width="110" align="center">
        <template #default="{ row }"><dict-tag :options="sys_version_type" :value="row.versionType" /></template>
      </el-table-column>
      <el-table-column label="组包方式" prop="packageMode" width="150" align="center">
        <template #default="{ row }"><dict-tag :options="sys_package_mode" :value="row.packageMode" /></template>
      </el-table-column>
      <el-table-column label="版本状态" prop="versionStatus" width="100" align="center">
        <template #default="{ row }"><dict-tag :options="sys_version_status" :value="row.versionStatus" /></template>
      </el-table-column>
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
      <el-table-column label="提交人员" prop="userName" width="100" align="center" />
      <el-table-column label="版本说明" prop="versionDescr" min-width="140" show-overflow-tooltip />
      <el-table-column label="创建时间" prop="createTime" width="160" align="center" sortable="custom" />
      <el-table-column label="修改时间" prop="updateTime" width="160" align="center" sortable="custom" />
      <el-table-column label="备注" prop="remarks" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link icon="View" @click="handleDetail(row)" v-hasPermi="['project:versionOut:query']">详情</el-button>
          <el-button type="primary" link icon="Edit" @click="handleEdit(row)" v-hasPermi="['project:versionOut:edit']">编辑</el-button>
          <el-button type="danger" link icon="Delete" @click="handleDelete(row)" v-hasPermi="['project:versionOut:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="VersionOut">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { onBeforeRouteLeave } from 'vue-router'
import { listVersionOut, delVersionOut } from '@/api/project/versionOut'

const { proxy } = getCurrentInstance()
const router = useRouter()
const { sys_version_type, sys_package_mode, sys_version_status } =
  proxy.useDict('sys_version_type', 'sys_package_mode', 'sys_version_status')

const SEARCH_STATE_KEY = 'version_out_search_state'

const versionList = ref([])
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1, pageSize: 10,
    productionYear: null, product: null, versionType: null,
    sysName: null, outLibVersion: null, packageMode: null,
    proBatchNo: null, taskNo: null, baseVersionCode: null,
    versionStatus: null, versionPDate: null, commName: null,
    orderByColumn: null, isAsc: null
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listVersionOut(queryParams.value).then(res => {
    versionList.value = res.rows
    total.value = res.total
    loading.value = false
  }).catch(() => { loading.value = false })
}

// 解析后端 taskRefs("任务号::L{taskId};;任务号::S{legacyTaskId}") → { 任务号: {type,id} }
// L=新系统实时任务(跳任务管理详情) / S=迁移老任务(跳历史任务快照详情)
function parseTaskRefs(taskRefs) {
  const map = {}
  if (!taskRefs) return map
  for (const seg of taskRefs.split(';;')) {
    const idx = seg.lastIndexOf('::')
    if (idx < 0) continue
    const no = seg.slice(0, idx).trim()
    const ref = seg.slice(idx + 2).trim()
    if (!no || !ref) continue
    const type = ref.charAt(0)
    const id = ref.slice(1)
    if ((type === 'L' || type === 'S') && id) map[no] = { type, id }
  }
  return map
}

// 列表"软件中心任务号"单元格：按 taskNos 逐个任务号输出 {no, type, id}
// type=L→实时任务详情链接, S→历史快照详情链接, null→纯文本(无法定位的任务号)
function taskCellTokens(row) {
  const map = parseTaskRefs(row.taskRefs)
  const seen = new Set()
  const out = []
  for (const raw of (row.taskNos || '').split(',')) {
    const no = raw.trim()
    if (!no || seen.has(no)) continue
    seen.add(no)
    const ref = map[no]
    out.push({ no, type: ref ? ref.type : null, id: ref ? ref.id : null })
  }
  return out
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  sessionStorage.removeItem(SEARCH_STATE_KEY)
  proxy.resetForm('queryRef')
  queryParams.value.orderByColumn = null
  queryParams.value.isAsc = null
  handleQuery()
}

function handleSortChange({ prop, order }) {
  queryParams.value.orderByColumn = prop
  queryParams.value.isAsc = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : null
  handleQuery()
}

function handleAdd() {
  router.push('/project/versionOut/add')
}

function handleExport() {
  proxy.download('/project/versionOut/export', { ...queryParams.value }, `批次版本_${new Date().getTime()}.xlsx`)
}

function handleDetail(row) {
  router.push(`/project/versionOut/detail/${row.id}`)
}

function handleEdit(row) {
  router.push(`/project/versionOut/edit/${row.id}`)
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除出入库版本号「${row.outLibVersion}」？`).then(() => {
    return delVersionOut(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

// 搜索状态缓存：离开详情/新增返回时保留查询条件
function saveSearchState() {
  sessionStorage.setItem(SEARCH_STATE_KEY, JSON.stringify({ queryParams: queryParams.value }))
}

function restoreSearchState() {
  try {
    const raw = sessionStorage.getItem(SEARCH_STATE_KEY)
    if (!raw) return false
    Object.assign(queryParams.value, JSON.parse(raw).queryParams)
    sessionStorage.removeItem(SEARCH_STATE_KEY)
    return true
  } catch { return false }
}

onBeforeRouteLeave(() => saveSearchState())

onMounted(() => {
  restoreSearchState()
  getList()
})
</script>

<style scoped>
.task-link {
  color: var(--el-color-primary);
  text-decoration: none;
}
.task-link:hover {
  text-decoration: underline;
}
</style>
