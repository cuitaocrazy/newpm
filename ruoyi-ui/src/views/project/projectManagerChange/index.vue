<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="项目名称" prop="projectName">
        <el-autocomplete
          v-model="queryParams.projectName"
          :fetch-suggestions="querySearchAsync"
          placeholder="请输入项目名称（至少2个字符）"
          :debounce="300"
          :trigger-on-focus="false"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="当前项目经理" prop="currentManagerId">
        <el-select v-model="queryParams.currentManagerId" placeholder="请选择项目经理" clearable style="width: 200px">
          <el-option
            v-for="user in userList"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="变更日期" style="width: 308px">
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
          type="success"
          plain
          icon="Edit"
          :disabled="multiple"
          @click="handleBatchChange"
          v-hasPermi="['project:projectManagerChange:batchChange']"
        >批量变更</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['project:projectManagerChange:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="projectManagerChangeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="项目编号" align="center" prop="projectCode" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="项目名称" align="center" prop="projectName" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="当前项目经理" align="center" prop="currentManagerName" width="120" />
      <el-table-column label="原项目经理" align="center" prop="oldManagerName" width="120" />
      <el-table-column label="变更原因" align="center" prop="changeReason" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="变更人" align="center" prop="changeByName" width="100" />
      <el-table-column label="变更时间" align="center" prop="changeTime" width="160" />
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['project:projectManagerChange:detail']">详情</el-button>
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

    <!-- 变更项目经理对话框 -->
    <el-dialog :title="changeDialogTitle" v-model="changeDialogVisible" width="500px" append-to-body>
      <el-form ref="changeFormRef" :model="changeForm" :rules="changeRules" label-width="120px">
        <el-form-item label="新项目经理" prop="newManagerId">
          <el-select v-model="changeForm.newManagerId" placeholder="请选择新项目经理" clearable style="width: 100%">
            <el-option
              v-for="user in userList"
              :key="user.userId"
              :label="user.nickName"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因" prop="changeReason">
          <el-input
            v-model="changeForm.changeReason"
            type="textarea"
            :rows="4"
            placeholder="请输入变更原因"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-alert
          v-if="isBatchChange"
          title="批量变更提示"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 15px"
        >
          <template #default>
            <div>将批量变更 <strong>{{ changeForm.projectIds.length }}</strong> 个项目的项目经理</div>
            <div style="font-size: 12px; margin-top: 5px">如果某个项目的新旧经理相同，该项目将被跳过</div>
          </template>
        </el-alert>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitChange">确 定</el-button>
          <el-button @click="cancelChange">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog title="项目经理变更详情" v-model="detailDialogVisible" width="700px" append-to-body>
      <div v-loading="detailLoading">
        <!-- 项目信息 -->
        <el-descriptions title="项目信息" :column="2" border>
          <el-descriptions-item label="项目编号">{{ detailProject.projectCode }}</el-descriptions-item>
          <el-descriptions-item label="项目名称">{{ detailProject.projectName }}</el-descriptions-item>
          <el-descriptions-item label="当前项目经理" :span="2">
            <el-tag type="success">{{ detailProject.projectManagerName || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ parseTime(detailProject.createTime, '{y}-{m}-{d}') }}
          </el-descriptions-item>
          <el-descriptions-item label="项目状态">
            <dict-tag :options="dict.type.sys_xmzt" :value="detailProject.projectStatus"/>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 变更历史 -->
        <el-divider content-position="left">变更历史</el-divider>
        <el-empty v-if="detailChanges.length === 0" description="暂无变更记录" :image-size="100" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="(change, index) in detailChanges"
            :key="change.changeId"
            :timestamp="parseTime(change.createTime)"
            placement="top"
            :color="index === 0 ? '#67C23A' : '#409EFF'"
          >
            <el-card>
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span style="font-weight: bold;">
                    变更 #{{ detailChanges.length - index }}
                  </span>
                  <el-tag :type="index === 0 ? 'success' : 'info'" size="small">
                    {{ index === 0 ? '最新' : '历史' }}
                  </el-tag>
                </div>
              </template>
              <div style="line-height: 28px;">
                <div>
                  <el-icon style="vertical-align: middle;"><User /></el-icon>
                  <span style="margin-left: 8px;">
                    <strong>原经理：</strong>
                    <el-tag size="small" type="info">{{ change.oldManagerName || '-' }}</el-tag>
                    <el-icon style="margin: 0 8px;"><Right /></el-icon>
                    <strong>新经理：</strong>
                    <el-tag size="small" type="success">{{ change.newManagerName || '-' }}</el-tag>
                  </span>
                </div>
                <div v-if="change.changeReason" style="margin-top: 8px;">
                  <el-icon style="vertical-align: middle;"><Document /></el-icon>
                  <span style="margin-left: 8px;">
                    <strong>变更原因：</strong>{{ change.changeReason }}
                  </span>
                </div>
                <div style="margin-top: 8px; color: #909399; font-size: 12px;">
                  <el-icon style="vertical-align: middle;"><Clock /></el-icon>
                  <span style="margin-left: 8px;">
                    变更人：{{ change.createBy || '-' }} · {{ parseTime(change.createTime) }}
                  </span>
                </div>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDetail">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ProjectManagerChange">
import { User, Right, Document, Clock } from '@element-plus/icons-vue'
import { listProjectManagerChange, getProjectManagerChange, delProjectManagerChange, addProjectManagerChange, updateProjectManagerChange, searchProjects, changeManager, batchChangeManager, getChangeDetail } from "@/api/project/projectManagerChange"
import { listUser } from "@/api/system/user"

const { proxy } = getCurrentInstance()
const { dict } = proxy.useDict('sys_xmzt')

const projectManagerChangeList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const projectIds = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const daterangeChangeTime = ref([])
const userList = ref([])

// 变更对话框状态
const changeDialogVisible = ref(false)
const changeDialogTitle = ref("")
const isBatchChange = ref(false)

// 详情对话框状态
const detailDialogVisible = ref(false)
const detailProject = ref({})
const detailChanges = ref([])
const detailLoading = ref(false)

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    currentManagerId: null,
  },
  // 变更表单
  changeForm: {
    projectId: null,
    projectIds: [],
    newManagerId: null,
    changeReason: null
  },
  rules: {
    changeId: [
      { required: true, message: "变更主键ID不能为空", trigger: "blur" }
    ],
    projectId: [
      { required: true, message: "项目ID不能为空", trigger: "blur" }
    ],
    newManagerId: [
      { required: true, message: "新项目经理ID不能为空", trigger: "change" }
    ],
  },
  // 变更表单验证规则
  changeRules: {
    newManagerId: [
      { required: true, message: "请选择新项目经理", trigger: "change" }
    ],
    changeReason: [
      { required: true, message: "请输入变更原因", trigger: "blur" }
    ]
  }
})

const { queryParams, form, rules, changeForm, changeRules } = toRefs(data)

/** 自动补全查询 */
function querySearchAsync(queryString, cb) {
  if (!queryString || queryString.length < 2) {
    cb([])
    return
  }
  searchProjects(queryString).then(response => {
    cb(response.data || [])
  }).catch(() => {
    cb([])
  })
}

/** 查询用户列表 */
function getUserList() {
  listUser({ pageNum: 1, pageSize: 1000 }).then(response => {
    userList.value = response.rows || []
  })
}

/** 查询项目经理变更列表 */
function getList() {
  loading.value = true
  queryParams.value.params = {}
  if (null != daterangeChangeTime.value && '' != daterangeChangeTime.value) {
    queryParams.value.params["beginTime"] = daterangeChangeTime.value[0]
    queryParams.value.params["endTime"] = daterangeChangeTime.value[1]
  }
  listProjectManagerChange(queryParams.value).then(response => {
    projectManagerChangeList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    changeId: null,
    projectId: null,
    oldManagerId: null,
    newManagerId: null,
    changeReason: null,
    delFlag: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("projectManagerChangeRef")
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

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.changeId)
  projectIds.value = selection.map(item => item.projectId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 重置变更表单 */
function resetChangeForm() {
  changeForm.value = {
    projectId: null,
    projectIds: [],
    newManagerId: null,
    changeReason: null
  }
  proxy.resetForm("changeFormRef")
}

/** 单个变更按钮操作 */
function handleChange(row) {
  resetChangeForm()
  changeForm.value.projectId = row.projectId
  changeDialogTitle.value = `变更项目经理 - ${row.projectName}`
  isBatchChange.value = false
  changeDialogVisible.value = true
}

/** 批量变更按钮操作 */
function handleBatchChange() {
  if (projectIds.value.length === 0) {
    proxy.$modal.msgWarning("请至少选择一条记录")
    return
  }
  resetChangeForm()
  changeForm.value.projectIds = projectIds.value
  changeDialogTitle.value = `批量变更项目经理 - 已选择 ${projectIds.value.length} 个项目`
  isBatchChange.value = true
  changeDialogVisible.value = true
}

/** 取消变更 */
function cancelChange() {
  changeDialogVisible.value = false
  resetChangeForm()
}

/** 提交变更 */
function submitChange() {
  proxy.$refs["changeFormRef"].validate(valid => {
    if (valid) {
      const apiCall = isBatchChange.value
        ? batchChangeManager(changeForm.value)
        : changeManager(changeForm.value)

      apiCall.then(response => {
        proxy.$modal.msgSuccess(response.msg || "变更成功")
        changeDialogVisible.value = false
        resetChangeForm()
        getList()
      }).catch(() => {
        // 错误已由全局拦截器处理
      })
    }
  })
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加项目经理变更"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _changeId = row.changeId || ids.value
  getProjectManagerChange(_changeId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改项目经理变更"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["projectManagerChangeRef"].validate(valid => {
    if (valid) {
      if (form.value.changeId != null) {
        updateProjectManagerChange(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addProjectManagerChange(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _changeIds = row.changeId || ids.value
  proxy.$modal.confirm('是否确认删除项目经理变更编号为"' + _changeIds + '"的数据项？').then(function() {
    return delProjectManagerChange(_changeIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('project/projectManagerChange/export', {
    ...queryParams.value
  }, `projectManagerChange_${new Date().getTime()}.xlsx`)
}

/** 查看详情按钮操作 */
function handleDetail(row) {
  detailLoading.value = true
  detailDialogVisible.value = true
  detailProject.value = {}
  detailChanges.value = []

  getChangeDetail(row.projectId).then(response => {
    detailProject.value = response.data.project || {}
    detailChanges.value = response.data.changes || []
    detailLoading.value = false
  }).catch(() => {
    detailLoading.value = false
  })
}

/** 关闭详情对话框 */
function closeDetail() {
  detailDialogVisible.value = false
  detailProject.value = {}
  detailChanges.value = []
}

getList()
getUserList()
</script>
