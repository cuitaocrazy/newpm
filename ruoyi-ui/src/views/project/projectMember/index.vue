<template>
  <div class="app-container project-member-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="项目部门" prop="deptId">
        <project-dept-select v-model="queryParams.deptId" placeholder="请选择部门" style="width: 220px" />
      </el-form-item>
      <el-form-item label="项目名称" prop="projectName">
        <el-select
          v-model="queryParams.projectName"
          filterable
          remote
          clearable
          :remote-method="remoteSearchProject"
          :loading="projectSearchLoading"
          placeholder="请选择或输入项目名称"
          style="width: 240px"
          @keyup.enter="handleQuery"
          @visible-change="(v) => v && loadProjectOptions()"
        >
          <el-option
            v-for="p in projectOptions"
            :key="p.projectId"
            :label="p.projectName"
            :value="p.projectName"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="projectList"
      :height="tableHeight"
      border
      stripe
      style="width: 100%">
      <el-table-column label="序号" type="index" width="55" align="center" />
      <el-table-column label="项目名称" align="left" header-align="center" prop="projectName" width="220">
        <template #default="scope">
          <div class="project-name-cell" @click="handleDetail(scope.row)" style="cursor: pointer; color: #409eff;">
            {{ scope.row.projectName }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="项目编号" align="center" prop="projectCode" width="160" />
      <el-table-column label="所属部门" align="center" prop="deptName" width="150" />
      <el-table-column label="参与人员" align="left" prop="memberNames" min-width="250">
        <template #default="scope">
          <template v-if="scope.row.memberNames">
            <el-tag
              v-for="name in scope.row.memberNames.split('、')"
              :key="name"
              size="small"
              style="margin: 2px 4px 2px 0;"
            >{{ name }}</el-tag>
          </template>
          <span v-else style="color: #ccc;">暂无成员</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" @click="handleDetail(scope.row)" v-hasPermi="['project:member:query']">
            详情
          </el-button>
          <el-button link type="primary" @click="handleEdit(scope.row)" v-hasPermi="['project:member:edit']">
            编辑
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 详情对话框 -->
    <el-dialog title="项目人员详情" v-model="detailDialogOpen" width="700px" append-to-body>
      <div class="detail-container">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目名称">{{ currentProject.projectName }}</el-descriptions-item>
          <el-descriptions-item label="项目编号">{{ currentProject.projectCode }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 16px 0 10px;">参与人员</h4>
        <el-table :data="memberList" border stripe size="small" v-loading="memberLoading">
          <el-table-column label="序号" type="index" width="55" align="center" />
          <el-table-column label="姓名" align="center" prop="nickName" min-width="120" />
          <el-table-column label="部门" align="center" prop="deptName" min-width="150" />
          <el-table-column label="加入时间" align="center" prop="joinDate" width="120" />
        </el-table>
        <el-empty v-if="!memberLoading && memberList.length === 0" description="暂无成员" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog title="编辑项目人员" v-model="editDialogOpen" width="600px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="项目">
          <el-tag type="info">{{ currentProject.projectName }}</el-tag>
        </el-form-item>
        <el-form-item label="参与人员" prop="userIds">
          <user-select
            v-model="editForm.userIds"
            placeholder="请选择参与人员"
            multiple
            filterable
            collapse-tags
            :collapse-tags-tooltip="true"
            :max-collapse-tags="3"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitEdit">确 定</el-button>
          <el-button @click="editDialogOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ProjectMember">
import { listProjectMember, getProjectMemberDetail, updateProjectMembers } from "@/api/project/projectMember"
import { searchProjects } from "@/api/project/project"

const { proxy } = getCurrentInstance()

const projectList = ref([])
const projectOptions = ref([])
const projectSearchLoading = ref(false)
const memberList = ref([])
const loading = ref(true)
const memberLoading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const tableHeight = ref(600)

const detailDialogOpen = ref(false)
const editDialogOpen = ref(false)
const currentProject = ref({})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    deptId: null
  },
  editForm: {
    projectId: null,
    userIds: []
  },
  editRules: {
    userIds: [
      { required: true, message: "请选择参与人员", trigger: "change" }
    ]
  }
})

const { queryParams, editForm, editRules } = toRefs(data)

/** 查询项目列表 */
function getList() {
  loading.value = true
  listProjectMember(queryParams.value).then(response => {
    projectList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 加载项目列表（支持模糊搜索） */
function loadProjectOptions(query) {
  projectSearchLoading.value = true
  searchProjects(query || '').then(res => {
    projectOptions.value = res.data || []
  }).finally(() => { projectSearchLoading.value = false })
}

function remoteSearchProject(query) {
  loadProjectOptions(query)
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 查看详情 */
function handleDetail(row) {
  currentProject.value = { ...row }
  detailDialogOpen.value = true
  memberLoading.value = true
  getProjectMemberDetail(row.projectId).then(response => {
    memberList.value = response.data || []
    memberLoading.value = false
  })
}

/** 编辑成员 */
function handleEdit(row) {
  currentProject.value = { ...row }
  editForm.value.projectId = row.projectId
  // 加载当前成员列表，预填已有成员
  getProjectMemberDetail(row.projectId).then(response => {
    const members = response.data || []
    editForm.value.userIds = members.map(m => m.userId)
    editDialogOpen.value = true
  })
}

/** 提交编辑 */
function submitEdit() {
  proxy.$refs["editFormRef"].validate(valid => {
    if (valid) {
      updateProjectMembers({
        projectId: editForm.value.projectId,
        userIds: editForm.value.userIds
      }).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        editDialogOpen.value = false
        getList()
      })
    }
  })
}

/** 计算表格高度 */
function calcTableHeight() {
  nextTick(() => {
    const windowHeight = window.innerHeight
    const searchHeight = showSearch.value ? 100 : 0
    const toolbarHeight = 50
    const paginationHeight = 50
    const padding = 120
    tableHeight.value = windowHeight - searchHeight - toolbarHeight - paginationHeight - padding
  })
}

onMounted(() => {
  calcTableHeight()
  window.addEventListener('resize', calcTableHeight)
  getList()
})

onUnmounted(() => {
  window.removeEventListener('resize', calcTableHeight)
})

watch(showSearch, () => {
  calcTableHeight()
})
</script>

<style scoped lang="scss">
.project-member-container {
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.el-form--inline .el-form-item) {
    margin-right: 20px;
    margin-bottom: 15px;
  }

  :deep(.el-table) {
    font-size: 13px;

    .el-table__header th {
      background-color: #f5f7fa;
      color: #606266;
      font-weight: 600;
    }

    .el-table__body tr:hover > td {
      background-color: #f5f7fa !important;
    }
  }

  :deep(.el-pagination) {
    margin-top: 15px;
    text-align: right;
  }
}

.detail-container {
  max-height: 500px;
  overflow-y: auto;
}

.project-name-cell {
  word-break: break-all;
  white-space: normal;
  line-height: 1.5;
  text-align: left;
}
</style>
