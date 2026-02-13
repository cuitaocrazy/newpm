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
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['project:projectManagerChange:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['project:projectManagerChange:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['project:projectManagerChange:remove']"
        >删除</el-button>
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

    <!-- 添加或修改项目经理变更对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="projectManagerChangeRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="项目ID" prop="projectId">
          <el-input v-model="form.projectId" placeholder="请输入项目ID" />
        </el-form-item>
        <el-form-item label="变更原因" prop="changeReason">
          <el-input v-model="form.changeReason" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="删除标志" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="请输入删除标志" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ProjectManagerChange">
import { listProjectManagerChange, getProjectManagerChange, delProjectManagerChange, addProjectManagerChange, updateProjectManagerChange, searchProjects } from "@/api/project/projectManagerChange"
import { listUser } from "@/api/system/user"

const { proxy } = getCurrentInstance()

const projectManagerChangeList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const daterangeChangeTime = ref([])
const userList = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    currentManagerId: null,
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
  }
})

const { queryParams, form, rules } = toRefs(data)

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
  single.value = selection.length != 1
  multiple.value = !selection.length
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

getList()
getUserList()
</script>
