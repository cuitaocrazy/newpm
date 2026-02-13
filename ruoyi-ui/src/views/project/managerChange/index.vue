<template>
  <div class="app-container manager-change-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="项目名称" prop="projectId">
        <el-select
          v-model="queryParams.projectId"
          placeholder="请选择或搜索项目"
          filterable
          remote
          reserve-keyword
          clearable
          :remote-method="remoteSearchProject"
          :loading="projectSearchLoading"
          style="width: 280px"
          @visible-change="handleProjectSelectVisible"
        >
          <el-option
            v-for="project in projectOptions"
            :key="project.projectId"
            :label="project.projectName"
            :value="project.projectId"
          >
            <div>
              <span>{{ project.projectName }}</span>
              <span style="float: right; color: #8492a6; font-size: 12px;">{{ project.projectCode }}</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="当前经理" prop="currentManagerId">
        <el-select v-model="queryParams.currentManagerId" placeholder="请选择当前项目经理" filterable clearable style="width: 200px">
          <el-option
            v-for="user in managerList"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="变更时间" style="width: 308px">
        <el-date-picker
          v-model="daterangeChangeTime"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Edit"
          :disabled="multiple"
          @click="handleBatchChange"
          v-hasPermi="['project:managerChange:batchChange']"
        >批量变更</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="projectList"
      :height="tableHeight"
      @selection-change="handleSelectionChange"
      border
      stripe
      style="width: 100%">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" type="index" width="55" align="center" />
      <el-table-column label="项目名称" align="left" prop="projectName" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <el-button link type="primary" @click="handleDetail(scope.row)">
            {{ scope.row.projectName }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="项目编号" align="center" prop="projectCode" width="150" />
      <el-table-column label="立项时间" align="center" prop="projectCreateTime" width="110" />
      <el-table-column label="当前项目经理" align="center" prop="currentManagerName" width="120" />
      <el-table-column label="原项目经理" align="center" prop="oldManagerName" width="120">
        <template #default="scope">
          <span v-if="scope.row.oldManagerName">{{ scope.row.oldManagerName }}</span>
          <span v-else style="color: #ccc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="变更原因" align="left" prop="changeReason" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.changeReason">{{ scope.row.changeReason }}</span>
          <span v-else style="color: #ccc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="变更人" align="center" prop="changeByName" width="100">
        <template #default="scope">
          <span v-if="scope.row.changeByName">{{ scope.row.changeByName }}</span>
          <span v-else style="color: #ccc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="变更时间" align="center" prop="changeTime" width="160">
        <template #default="scope">
          <span v-if="scope.row.changeTime">{{ scope.row.changeTime }}</span>
          <span v-else style="color: #ccc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" @click="handleDetail(scope.row)" v-hasPermi="['project:managerChange:query']">
            详情
          </el-button>
          <el-button link type="primary" @click="handleChange(scope.row)" v-hasPermi="['project:managerChange:change']">
            变更
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 变更对话框（单个/批量共用） -->
    <el-dialog :title="changeTitle" v-model="changeDialogOpen" width="600px" append-to-body>
      <el-form ref="changeFormRef" :model="changeForm" :rules="changeRules" label-width="120px">
        <el-form-item label="项目" v-if="!isBatchChange">
          <el-tag type="info">{{ currentProject.projectName }}</el-tag>
        </el-form-item>
        <el-form-item label="选中项目" v-if="isBatchChange">
          <el-tag v-for="item in selectedProjects" :key="item.projectId" style="margin-right: 5px;">
            {{ item.projectName }}
          </el-tag>
        </el-form-item>
        <el-form-item label="当前项目经理" v-if="!isBatchChange">
          <el-input v-model="currentProject.currentManagerName" disabled />
        </el-form-item>
        <el-form-item label="新项目经理" prop="newManagerId">
          <el-select v-model="changeForm.newManagerId" placeholder="请选择新项目经理" filterable style="width: 100%">
            <el-option
              v-for="user in managerList"
              :key="user.userId"
              :label="user.nickName"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因" prop="changeReason">
          <el-input v-model="changeForm.changeReason" type="textarea" :rows="4" placeholder="请输入变更原因（选填，最多500字）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitChange">确 定</el-button>
          <el-button @click="cancelChange">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog title="项目经理变更详情" v-model="detailDialogOpen" width="700px" append-to-body>
      <div class="detail-container">
        <div class="project-info">
          <h3>项目信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目名称">{{ currentProject.projectName }}</el-descriptions-item>
            <el-descriptions-item label="项目编号">{{ currentProject.projectCode }}</el-descriptions-item>
            <el-descriptions-item label="当前项目经理">{{ currentProject.currentManagerName }}</el-descriptions-item>
            <el-descriptions-item label="立项时间">{{ currentProject.projectCreateTime }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="change-history">
          <h3>变更历史</h3>
          <el-empty v-if="changeHistoryList.length === 0" description="暂无变更记录" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="item in changeHistoryList"
              :key="item.changeId"
              :timestamp="item.createTime"
              placement="top"
            >
              <el-card>
                <h4>项目经理变更: {{ item.oldManagerName || '无' }} → {{ item.newManagerName }}</h4>
                <p v-if="item.changeReason">变更原因: {{ item.changeReason }}</p>
                <p style="color: #999; font-size: 12px;">变更人: {{ item.createBy }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ManagerChange">
import { listManagerChange, getProjectChangeHistory, changeManager, batchChangeManager } from "@/api/project/managerChange"
import { listUserByPost } from "@/api/system/user"
import request from '@/utils/request'

const { proxy } = getCurrentInstance()

const projectList = ref([])
const managerList = ref([])
const changeHistoryList = ref([])
const projectOptions = ref([])
const projectSearchLoading = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const selectedProjects = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const daterangeChangeTime = ref([])
const tableHeight = ref(600)

const changeDialogOpen = ref(false)
const detailDialogOpen = ref(false)
const isBatchChange = ref(false)
const changeTitle = ref("")
const currentProject = ref({})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: null,
    currentManagerId: null,
    changeTime: null
  },
  changeForm: {
    projectId: null,
    newManagerId: null,
    changeReason: null
  },
  changeRules: {
    newManagerId: [
      { required: true, message: "新项目经理不能为空", trigger: "change" }
    ]
  }
})

const { queryParams, changeForm, changeRules } = toRefs(data)

/** 查询项目列表 */
function getList() {
  loading.value = true
  queryParams.value.params = {}
  if (daterangeChangeTime.value && daterangeChangeTime.value.length === 2) {
    queryParams.value.params["beginChangeTime"] = daterangeChangeTime.value[0]
    queryParams.value.params["endChangeTime"] = daterangeChangeTime.value[1]
  }
  listManagerChange(queryParams.value).then(response => {
    projectList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 加载项目经理列表 */
function loadManagerList() {
  listUserByPost('pm').then(response => {
    managerList.value = response.data
  })
}

/** 加载所有项目列表（初始） */
function loadProjectOptions() {
  projectSearchLoading.value = true
  request({
    url: '/project/project/search',
    method: 'get',
    params: { projectName: '' }
  }).then(response => {
    projectOptions.value = response.data || []
    projectSearchLoading.value = false
  }).catch(() => {
    projectSearchLoading.value = false
  })
}

/** 项目远程搜索 */
function remoteSearchProject(query) {
  if (query) {
    projectSearchLoading.value = true
    request({
      url: '/project/project/search',
      method: 'get',
      params: { projectName: query }
    }).then(response => {
      projectOptions.value = response.data || []
      projectSearchLoading.value = false
    }).catch(() => {
      projectSearchLoading.value = false
    })
  } else {
    projectOptions.value = []
  }
}

/** 下拉框展开时加载全部项目 */
function handleProjectSelectVisible(visible) {
  if (visible && projectOptions.value.length === 0) {
    loadProjectOptions()
  }
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  daterangeChangeTime.value = []
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.projectId)
  selectedProjects.value = selection
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 单个项目变更 */
function handleChange(row) {
  currentProject.value = { ...row }
  changeForm.value = {
    projectId: row.projectId,
    newManagerId: null,
    changeReason: null
  }
  isBatchChange.value = false
  changeTitle.value = "变更项目经理"
  changeDialogOpen.value = true
}

/** 批量变更 */
function handleBatchChange() {
  if (ids.value.length === 0) {
    proxy.$modal.msgError("请选择要变更的项目")
    return
  }
  changeForm.value = {
    projectId: null,
    newManagerId: null,
    changeReason: null
  }
  isBatchChange.value = true
  changeTitle.value = "批量变更项目经理"
  changeDialogOpen.value = true
}

/** 提交变更 */
function submitChange() {
  proxy.$refs["changeFormRef"].validate(valid => {
    if (valid) {
      if (isBatchChange.value) {
        // 批量变更
        batchChangeManager(ids.value, changeForm.value.newManagerId, changeForm.value.changeReason).then(response => {
          proxy.$modal.msgSuccess("批量变更成功")
          changeDialogOpen.value = false
          getList()
        })
      } else {
        // 单个变更
        changeManager(changeForm.value).then(response => {
          proxy.$modal.msgSuccess("变更成功")
          changeDialogOpen.value = false
          getList()
        })
      }
    }
  })
}

/** 取消变更 */
function cancelChange() {
  changeDialogOpen.value = false
  proxy.resetForm("changeFormRef")
}

/** 查看详情 */
function handleDetail(row) {
  currentProject.value = { ...row }
  detailDialogOpen.value = true
  // 加载变更历史
  getProjectChangeHistory(row.projectId).then(response => {
    changeHistoryList.value = response.data || []
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

// 监听窗口大小变化
onMounted(() => {
  calcTableHeight()
  window.addEventListener('resize', calcTableHeight)
  loadManagerList()
  getList()
})

onUnmounted(() => {
  window.removeEventListener('resize', calcTableHeight)
})

// 监听搜索框显示/隐藏
watch(showSearch, () => {
  calcTableHeight()
})
</script>

<style scoped lang="scss">
.manager-change-container {
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

.project-info {
  margin-bottom: 20px;
}

.project-info h3,
.change-history h3 {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: bold;
}
</style>
