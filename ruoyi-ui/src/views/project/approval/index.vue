<template>
  <div class="app-container approval-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="项目ID" prop="projectId">
        <el-input
          v-model="queryParams.projectId"
          placeholder="请输入项目ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="审核状态" prop="approvalStatus">
        <el-select v-model="queryParams.approvalStatus" placeholder="请选择审核状态" clearable>
          <el-option
            v-for="dict in sys_spzt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审核人ID" prop="approverId">
        <el-input
          v-model="queryParams.approverId"
          placeholder="请输入审核人ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="审核时间" prop="approvalTime">
        <el-date-picker clearable
          v-model="queryParams.approvalTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择审核时间">
        </el-date-picker>
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
          v-hasPermi="['project:approval:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['project:approval:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['project:approval:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['project:approval:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="approvalList" :height="tableHeight" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="审核主键ID" align="center" prop="approvalId" />
      <el-table-column label="项目ID" align="center" prop="projectId" />
      <el-table-column label="审核状态(0-待审核、1-通过、2-不通过)" align="center" prop="approvalStatus">
        <template #default="scope">
          <dict-tag :options="sys_spzt" :value="scope.row.approvalStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="审核原因/意见" align="center" prop="approvalReason" />
      <el-table-column label="审核人ID" align="center" prop="approverId" />
      <el-table-column label="审核时间" align="center" prop="approvalTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.approvalTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:approval:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:approval:remove']">删除</el-button>
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

    <!-- 添加或修改立项审核对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="approvalRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="项目ID" prop="projectId">
          <el-input v-model="form.projectId" placeholder="请输入项目ID" />
        </el-form-item>
        <el-form-item label="审核状态" prop="approvalStatus">
          <el-radio-group v-model="form.approvalStatus">
            <el-radio
              v-for="dict in sys_spzt"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核原因/意见" prop="approvalReason">
          <el-input v-model="form.approvalReason" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="审核人ID" prop="approverId">
          <el-input v-model="form.approverId" placeholder="请输入审核人ID" />
        </el-form-item>
        <el-form-item label="审核时间" prop="approvalTime">
          <el-date-picker clearable
            v-model="form.approvalTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择审核时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="删除标志(0正常 1删除)" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="请输入删除标志(0正常 1删除)" />
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

<script setup name="Approval">
import { listApproval, getApproval, delApproval, addApproval, updateApproval } from "@/api/project/approval"

const { proxy } = getCurrentInstance()
const { sys_spzt } = proxy.useDict('sys_spzt')

const approvalList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const tableHeight = ref(600)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: null,
    approvalStatus: null,
    approvalReason: null,
    approverId: null,
    approvalTime: null,
  },
  rules: {
    approvalId: [
      { required: true, message: "审核主键ID不能为空", trigger: "blur" }
    ],
    projectId: [
      { required: true, message: "项目ID不能为空", trigger: "blur" }
    ],
    approvalStatus: [
      { required: true, message: "审核状态(0-待审核、1-通过、2-不通过)不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询立项审核列表 */
function getList() {
  loading.value = true
  listApproval(queryParams.value).then(response => {
    approvalList.value = response.rows
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
    approvalId: null,
    projectId: null,
    approvalStatus: null,
    approvalReason: null,
    approverId: null,
    approvalTime: null,
    delFlag: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("approvalRef")
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

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.approvalId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加立项审核"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _approvalId = row.approvalId || ids.value
  getApproval(_approvalId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改立项审核"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["approvalRef"].validate(valid => {
    if (valid) {
      if (form.value.approvalId != null) {
        updateApproval(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addApproval(form.value).then(response => {
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
  const _approvalIds = row.approvalId || ids.value
  proxy.$modal.confirm('是否确认删除立项审核编号为"' + _approvalIds + '"的数据项？').then(function() {
    return delApproval(_approvalIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('project/approval/export', {
    ...queryParams.value
  }, `approval_${new Date().getTime()}.xlsx`)
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
})

onUnmounted(() => {
  window.removeEventListener('resize', calcTableHeight)
})

// 监听搜索框显示/隐藏
watch(showSearch, () => {
  calcTableHeight()
})

getList()
</script>

<style scoped lang="scss">
.approval-container {
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
</style>
