<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="客户简称" prop="customerSimpleName">
        <el-autocomplete
          v-model="queryParams.customerSimpleName"
          :fetch-suggestions="querySimpleNameSearch"
          placeholder="请输入客户简称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="客户全称" prop="customerAllName">
        <el-autocomplete
          v-model="queryParams.customerAllName"
          :fetch-suggestions="queryAllNameSearch"
          placeholder="请输入客户全称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="所属行业" prop="industry">
        <el-select v-model="queryParams.industry" placeholder="请选择所属行业(字典表 字典类型industry)" clearable>
          <el-option
            v-for="dict in industry"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="所属区域" prop="region">
        <el-select v-model="queryParams.region" placeholder="请选择所属区域(字典表 : 字典类型sys_yjqy)" clearable>
          <el-option
            v-for="dict in sys_yjqy"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="销售负责人ID" prop="salesManagerId">
        <el-input
          v-model="queryParams.salesManagerId"
          placeholder="请输入销售负责人ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="销售负责人姓名" prop="salesManagerName">
        <el-input
          v-model="queryParams.salesManagerName"
          placeholder="请输入销售负责人姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker clearable
          v-model="queryParams.createTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择创建时间">
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
          v-hasPermi="['project:customer:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['project:customer:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['project:customer:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['project:customer:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="customerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="客户主键ID" align="center" prop="customerId" />
      <el-table-column label="客户简称" align="center" prop="customerSimpleName" />
      <el-table-column label="客户全称" align="center" prop="customerAllName" />
      <el-table-column label="所属行业" align="center" prop="industry">
        <template #default="scope">
          <dict-tag :options="industry" :value="scope.row.industry"/>
        </template>
      </el-table-column>
      <el-table-column label="所属区域" align="center" prop="region">
        <template #default="scope">
          <dict-tag :options="sys_yjqy" :value="scope.row.region"/>
        </template>
      </el-table-column>
      <el-table-column label="销售负责人姓名" align="center" prop="salesManagerName" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:customer:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:customer:remove']">删除</el-button>
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

    <!-- 添加或修改客户管理对话框 -->
    <el-dialog :title="title" v-model="open" width="900px" append-to-body>
      <el-form ref="customerRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="客户简称" prop="customerSimpleName">
          <el-input v-model="form.customerSimpleName" placeholder="请输入客户简称" />
        </el-form-item>
        <el-form-item label="客户全称" prop="customerAllName">
          <el-input v-model="form.customerAllName" placeholder="请输入客户全称" />
        </el-form-item>
        <el-form-item label="所属行业" prop="industry">
          <el-select v-model="form.industry" placeholder="请选择所属行业(字典表 字典类型industry)">
            <el-option
              v-for="dict in industry"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="所属区域" prop="region">
          <el-select v-model="form.region" placeholder="请选择所属区域(字典表 : 字典类型sys_yjqy)">
            <el-option
              v-for="dict in sys_yjqy"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="销售负责人ID" prop="salesManagerId">
          <el-input v-model="form.salesManagerId" placeholder="请输入销售负责人ID" />
        </el-form-item>
        <el-form-item label="销售负责人姓名" prop="salesManagerName">
          <el-input v-model="form.salesManagerName" placeholder="请输入销售负责人姓名" />
        </el-form-item>
        <el-form-item label="办公地址" prop="officeAddress">
          <el-input v-model="form.officeAddress" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-divider content-position="left">联系人信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="Plus" size="small" @click="handleAddContact">添加联系人</el-button>
          </el-col>
        </el-row>
        <el-table :data="form.contactList" style="width: 100%" max-height="300">
          <el-table-column label="联系人姓名" width="120">
            <template #default="scope">
              <el-input v-model="scope.row.contactName" placeholder="请输入姓名" />
            </template>
          </el-table-column>
          <el-table-column label="联系人电话" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.contactPhone" placeholder="请输入电话" />
            </template>
          </el-table-column>
          <el-table-column label="联系人标签" width="150">
            <template #default="scope">
              <el-select v-model="scope.row.contactTag" placeholder="请选择标签">
                <el-option
                  v-for="dict in contact_tag"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="120">
            <template #default="scope">
              <el-input v-model="scope.row.remark" placeholder="请输入备注" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="scope">
              <el-button link type="danger" icon="Delete" @click="handleDeleteContact(scope.$index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
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

<script setup name="Customer">
import { listCustomer, getCustomer, delCustomer, addCustomer, updateCustomer } from "@/api/project/customer"

const { proxy } = getCurrentInstance()
const { sys_yjqy, industry, contact_tag } = proxy.useDict('sys_yjqy', 'industry', 'contact_tag')

// 电话号码验证函数（手机号或座机号）
const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback()
    return
  }
  // 手机号正则：1开头的11位数字
  const mobileReg = /^1[3-9]\d{9}$/
  // 座机号正则：区号-号码 或 号码（支持分机号）
  const telReg = /^(0\d{2,3}-?)?\d{7,8}(-\d{1,6})?$/

  if (mobileReg.test(value) || telReg.test(value)) {
    callback()
  } else {
    callback(new Error('请输入正确的手机号或座机号'))
  }
}

const customerList = ref([])
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
    customerSimpleName: null,
    customerAllName: null,
    industry: null,
    region: null,
    salesManagerId: null,
    salesManagerName: null,
    createTime: null,
  },
  rules: {
    customerId: [
      { required: true, message: "客户主键ID不能为空", trigger: "blur" }
    ],
    customerSimpleName: [
      { required: true, message: "客户简称不能为空", trigger: "blur" }
    ],
    customerAllName: [
      { required: true, message: "客户全称不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询客户管理列表 */
function getList() {
  loading.value = true
  listCustomer(queryParams.value).then(response => {
    customerList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 客户简称自动补全查询 */
function querySimpleNameSearch(queryString, cb) {
  // 获取所有客户简称的唯一值
  const results = queryString
    ? customerList.value
        .filter(item => item.customerSimpleName && item.customerSimpleName.toLowerCase().includes(queryString.toLowerCase()))
        .map(item => ({ value: item.customerSimpleName }))
        .filter((item, index, self) =>
          index === self.findIndex(t => t.value === item.value)
        )
    : customerList.value
        .filter(item => item.customerSimpleName)
        .map(item => ({ value: item.customerSimpleName }))
        .filter((item, index, self) =>
          index === self.findIndex(t => t.value === item.value)
        )
  cb(results)
}

/** 客户全称自动补全查询 */
function queryAllNameSearch(queryString, cb) {
  // 获取所有客户全称的唯一值
  const results = queryString
    ? customerList.value
        .filter(item => item.customerAllName && item.customerAllName.toLowerCase().includes(queryString.toLowerCase()))
        .map(item => ({ value: item.customerAllName }))
        .filter((item, index, self) =>
          index === self.findIndex(t => t.value === item.value)
        )
    : customerList.value
        .filter(item => item.customerAllName)
        .map(item => ({ value: item.customerAllName }))
        .filter((item, index, self) =>
          index === self.findIndex(t => t.value === item.value)
        )
  cb(results)
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    customerId: null,
    customerSimpleName: null,
    customerAllName: null,
    industry: null,
    region: null,
    salesManagerId: null,
    salesManagerName: null,
    officeAddress: null,
    delFlag: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null,
    contactList: []
  }
  proxy.resetForm("customerRef")
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
  ids.value = selection.map(item => item.customerId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加客户管理"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _customerId = row.customerId || ids.value
  getCustomer(_customerId).then(response => {
    form.value = response.data
    // 确保contactList存在
    if (!form.value.contactList) {
      form.value.contactList = []
    }
    open.value = true
    title.value = "修改客户管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["customerRef"].validate(valid => {
    if (valid) {
      // 验证联系人电话号码
      if (form.value.contactList && form.value.contactList.length > 0) {
        for (let i = 0; i < form.value.contactList.length; i++) {
          const contact = form.value.contactList[i]
          if (contact.contactPhone) {
            const mobileReg = /^1[3-9]\d{9}$/
            const telReg = /^(0\d{2,3}-?)?\d{7,8}(-\d{1,6})?$/
            if (!mobileReg.test(contact.contactPhone) && !telReg.test(contact.contactPhone)) {
              proxy.$modal.msgError(`第${i + 1}个联系人的电话号码格式不正确，请输入正确的手机号或座机号`)
              return
            }
          }
        }
      }

      if (form.value.customerId != null) {
        updateCustomer(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addCustomer(form.value).then(response => {
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
  const _customerIds = row.customerId || ids.value
  proxy.$modal.confirm('是否确认删除客户管理编号为"' + _customerIds + '"的数据项？').then(function() {
    return delCustomer(_customerIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('project/customer/export', {
    ...queryParams.value
  }, `customer_${new Date().getTime()}.xlsx`)
}

/** 添加联系人 */
function handleAddContact() {
  const newContact = {
    contactName: '',
    contactPhone: '',
    contactTag: '',
    remark: ''
  }
  if (!form.value.contactList) {
    form.value.contactList = []
  }
  form.value.contactList.push(newContact)
}

/** 删除联系人 */
function handleDeleteContact(index) {
  form.value.contactList.splice(index, 1)
}

getList()
</script>
