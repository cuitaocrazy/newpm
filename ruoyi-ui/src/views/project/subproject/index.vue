<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="项目部门" prop="projectDept">
        <project-dept-select v-model="queryParams.projectDept" @change="handleDeptChange" clearable style="width: 300px" />
      </el-form-item>
      <el-form-item label="所属主项目" prop="parentId">
        <el-select
          v-model="queryParams.parentId"
          placeholder="请先选择部门"
          filterable
          clearable
          style="width: 260px"
          :disabled="!queryParams.projectDept"
          @change="handleQuery"
        >
          <el-option
            v-for="p in parentProjectOptions"
            :key="p.projectId"
            :label="`${p.projectName}（${p.projectCode}）`"
            :value="p.projectId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="任务名称" prop="projectName">
        <el-input v-model="queryParams.projectName" placeholder="请输入任务名称"
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
          :disabled="!queryParams.parentId" @click="handleAdd">新增任务</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="subprojectList" border>
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column label="任务编号" prop="taskCode" width="130" />
      <el-table-column label="任务名称" prop="projectName" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          <el-link type="primary" @click="handleDetail(scope.row)"
            v-if="checkPermi(['project:subproject:query'])">{{ scope.row.projectName }}</el-link>
          <span v-else>{{ scope.row.projectName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="所属主项目" prop="parentProjectName" min-width="160" show-overflow-tooltip />
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
      <el-table-column label="任务负责人" prop="projectManagerName" width="100" align="center" />
      <el-table-column label="预计人天" prop="estimatedWorkload" width="100" align="right" />
      <el-table-column label="实际人天" prop="actualWorkload" width="100" align="right">
        <template #default="scope">
          {{ scope.row.actualWorkload != null ? parseFloat(scope.row.actualWorkload).toFixed(3) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="投产批次" prop="productionBatch" width="120" align="center">
        <template #default="scope">
          <dict-tag :options="sys_tcpc" :value="scope.row.productionBatch" />
        </template>
      </el-table-column>
      <el-table-column label="启动日期" prop="startDate" width="120" align="center" />
      <el-table-column label="结束日期" prop="endDate" width="120" align="center" />
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
import { listSubProject, delProject, getProject } from '@/api/project/project'
import { checkPermi } from '@/utils/permission'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()
const { sys_xmjd, sys_xmzt, sys_tcpc } = proxy.useDict('sys_xmjd', 'sys_xmzt', 'sys_tcpc')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const subprojectList = ref([])
const parentProjectOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  projectDept: null,
  parentId: null,
  projectName: '',
  projectStage: ''
})

/** 部门变更：清空主项目选择，重新加载该部门下的主项目列表 */
async function handleDeptChange(deptId) {
  queryParams.parentId = null
  parentProjectOptions.value = []
  if (!deptId) { subprojectList.value = []; total.value = 0; return }
  const res = await request({
    url: '/project/project/list',
    method: 'get',
    params: { projectDept: deptId, projectLevel: 0, pageNum: 1, pageSize: 200 }
  })
  parentProjectOptions.value = res.rows || []
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
function resetQuery()  { proxy.$refs['queryRef'].resetFields(); parentProjectOptions.value = []; handleQuery() }

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
  proxy.$modal.confirm(`确认删除任务「${row.projectName}」？`).then(() => {
    return delProject(row.projectId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

onMounted(async () => {
  if (route.query.parentId) {
    const parentId = Number(route.query.parentId)
    queryParams.parentId = parentId
    // 获取父项目信息，用于填充部门和下拉
    try {
      const res = await getProject(parentId)
      if (res.data) {
        queryParams.projectDept = res.data.projectDept || null
        parentProjectOptions.value = [res.data]
      }
    } catch (e) {
      console.error('加载父项目信息失败', e)
    }
    getList()
  } else {
    getList()
  }
})
</script>
