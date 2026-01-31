<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="客户ID" prop="customerId">
        <el-input
          v-model="queryParams.customerId"
          placeholder="请输入客户ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="联系人姓名" prop="contactName">
        <el-input
          v-model="queryParams.contactName"
          placeholder="请输入联系人姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="联系人电话" prop="contactPhone">
        <el-input
          v-model="queryParams.contactPhone"
          placeholder="请输入联系人电话"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="联系人标签" prop="contactTag">
        <el-select v-model="queryParams.contactTag" placeholder="请选择联系人标签(字典表，字典类型contact_tag)" clearable>
          <el-option
            v-for="dict in contact_tag"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
          v-hasPermi="['project:contact:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['project:contact:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['project:contact:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['project:contact:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="contactList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="联系人主键ID" align="center" prop="contactId" />
      <el-table-column label="客户ID" align="center" prop="customerId" />
      <el-table-column label="联系人姓名" align="center" prop="contactName" />
      <el-table-column label="联系人电话" align="center" prop="contactPhone" />
      <el-table-column label="联系人标签" align="center" prop="contactTag">
        <template #default="scope">
          <dict-tag :options="contact_tag" :value="scope.row.contactTag"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:contact:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:contact:remove']">删除</el-button>
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

    <!-- 添加或修改客户联系人对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="contactRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="客户ID" prop="customerId">
          <el-input v-model="form.customerId" placeholder="请输入客户ID" />
        </el-form-item>
        <el-form-item label="联系人姓名" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系人电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系人电话" />
        </el-form-item>
        <el-form-item label="联系人标签" prop="contactTag">
          <el-select v-model="form.contactTag" placeholder="请选择联系人标签(字典表，字典类型contact_tag)">
            <el-option
              v-for="dict in contact_tag"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
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

<script setup name="Contact">
import { listContact, getContact, delContact, addContact, updateContact } from "@/api/project/contact"

const { proxy } = getCurrentInstance()
const { contact_tag } = proxy.useDict('contact_tag')

const contactList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    customerId: null,
    contactName: null,
    contactPhone: null,
    contactTag: null,
  },
  rules: {
    contactId: [
      { required: true, message: "联系人主键ID不能为空", trigger: "blur" }
    ],
    customerId: [
      { required: true, message: "客户ID不能为空", trigger: "blur" }
    ],
    contactName: [
      { required: true, message: "联系人姓名不能为空", trigger: "blur" }
    ],
    contactPhone: [
      { required: true, message: "联系人电话不能为空", trigger: "blur" }
    ],
    contactTag: [
      { required: true, message: "联系人标签(字典表，字典类型contact_tag)不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询客户联系人列表 */
function getList() {
  loading.value = true
  listContact(queryParams.value).then(response => {
    contactList.value = response.rows
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
    contactId: null,
    customerId: null,
    contactName: null,
    contactPhone: null,
    contactTag: null,
    delFlag: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("contactRef")
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
  ids.value = selection.map(item => item.contactId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加客户联系人"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _contactId = row.contactId || ids.value
  getContact(_contactId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改客户联系人"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["contactRef"].validate(valid => {
    if (valid) {
      if (form.value.contactId != null) {
        updateContact(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addContact(form.value).then(response => {
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
  const _contactIds = row.contactId || ids.value
  proxy.$modal.confirm('是否确认删除客户联系人编号为"' + _contactIds + '"的数据项？').then(function() {
    return delContact(_contactIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('project/contact/export', {
    ...queryParams.value
  }, `contact_${new Date().getTime()}.xlsx`)
}

getList()
</script>
