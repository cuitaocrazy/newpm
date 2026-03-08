<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="所属主项目" prop="parentId">
        <el-select
          v-model="queryParams.parentId"
          placeholder="请选择主项目"
          filterable
          clearable
          style="width: 260px"
          @change="handleQuery"
        >
          <el-option
            v-for="p in mainProjectOptions"
            :key="p.projectId"
            :label="`${p.projectName}（${p.projectCode}）`"
            :value="p.projectId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="子项目名称" prop="projectName">
        <el-input v-model="queryParams.projectName" placeholder="请输入子项目名称"
          clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="当前阶段" prop="projectStage">
        <el-select v-model="queryParams.projectStage" placeholder="请选择" clearable style="width: 160px">
          <el-option v-for="d in sys_xmjd" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" v-hasPermi="['project:subproject:add']"
          :disabled="!queryParams.parentId" @click="handleAdd">新增子项目</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="subprojectList" border>
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column label="子项目编号" prop="taskCode" width="130" />
      <el-table-column label="子项目名称" prop="projectName" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          <el-link type="primary" @click="handleDetail(scope.row)"
            v-if="checkPermi(['project:subproject:query'])">{{ scope.row.projectName }}</el-link>
          <span v-else>{{ scope.row.projectName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前阶段" prop="projectStage" width="130">
        <template #default="scope">
          <dict-tag :options="sys_xmjd" :value="scope.row.projectStage" />
        </template>
      </el-table-column>
      <el-table-column label="项目状态" prop="projectStatus" width="100" align="center">
        <template #default="scope">
          <dict-tag :options="sys_xmzt" :value="scope.row.projectStatus" />
        </template>
      </el-table-column>
      <el-table-column label="项目经理" prop="projectManagerName" width="100" align="center" />
      <el-table-column label="预计人天" prop="estimatedWorkload" width="100" align="right" />
      <el-table-column label="实际人天" prop="actualWorkload" width="100" align="right">
        <template #default="scope">
          {{ scope.row.actualWorkload != null ? parseFloat(scope.row.actualWorkload).toFixed(3) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" v-hasPermi="['project:subproject:query']"
            @click="handleDetail(scope.row)">详情</el-button>
          <el-button link type="primary" icon="Edit" v-hasPermi="['project:subproject:edit']"
            @click="handleEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" icon="Delete" v-hasPermi="['project:subproject:remove']"
            @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      @pagination="getList" />
  </div>
</template>

<script setup name="SubprojectIndex">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { listSubProject, listProject, delProject } from '@/api/project/project'
import { checkPermi } from '@/utils/permission'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()
const { sys_xmjd, sys_xmzt } = proxy.useDict('sys_xmjd', 'sys_xmzt')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const subprojectList = ref([])
const mainProjectOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  parentId: null,
  projectName: '',
  projectStage: ''
})

/** 加载主项目下拉选项 */
function loadMainProjectOptions() {
  listProject({ pageNum: 1, pageSize: 999 }).then(res => {
    mainProjectOptions.value = res.rows || []
  })
}

/** 查询子项目列表 */
function getList() {
  if (!queryParams.parentId) {
    subprojectList.value = []
    total.value = 0
    return
  }
  loading.value = true
  listSubProject(queryParams).then(res => {
    subprojectList.value = res.rows
    total.value = res.total
  }).finally(() => { loading.value = false })
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery()  { proxy.$refs['queryRef'].resetFields(); handleQuery() }

function handleAdd() {
  router.push({ path: '/project/subproject/add', query: { parentId: queryParams.parentId } })
}
function handleEdit(row) {
  router.push(`/project/subproject/edit/${row.projectId}`)
}
function handleDetail(row) {
  router.push(`/project/subproject/detail/${row.projectId}`)
}
function handleDelete(row) {
  proxy.$modal.confirm(`确认删除子项目「${row.projectName}」？`).then(() => {
    return delProject(row.projectId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

onMounted(() => {
  loadMainProjectOptions()
  // 若从主项目列表跳转过来，直接带入 parentId
  if (route.query.parentId) {
    queryParams.parentId = Number(route.query.parentId)
    getList()
  }
})
</script>
