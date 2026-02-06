<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="客户简称" prop="customerSimpleName">
        <el-autocomplete
          v-model="queryParams.customerSimpleName"
          :fetch-suggestions="querySearchCustomer"
          placeholder="请输入客户简称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="所属行业" prop="industry">
        <el-select v-model="queryParams.industry" placeholder="请选择所属行业" clearable>
          <el-option
            v-for="dict in industry"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="所属区域" prop="region">
        <el-select v-model="queryParams.region" placeholder="请选择所属区域" clearable>
          <el-option
            v-for="dict in sys_yjqy"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="销售负责人" prop="salesManagerId">
        <el-select v-model="queryParams.salesManagerId" placeholder="请选择销售负责人" clearable filterable>
          <el-option
            v-for="user in salesManagerList"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
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
      <el-table-column type="index" label="序号" width="55" align="center" />
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
      <el-table-column label="销售负责人" align="center" prop="salesManagerName" />
      <el-table-column label="办公地址" align="center" prop="officeAddress" show-overflow-tooltip />
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
      <el-table-column type="expand">
        <template #default="props">
          <div v-if="props.row.customerContactList && props.row.customerContactList.length > 0" style="padding: 0 50px;">
            <el-table :data="props.row.customerContactList" border>
              <el-table-column label="联系人姓名" prop="contactName" />
              <el-table-column label="联系人电话" prop="contactPhone" />
              <el-table-column label="联系人标签" prop="contactTag">
                <template #default="scope">
                  <dict-tag :options="contact_tag" :value="scope.row.contactTag"/>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-else style="padding: 20px 50px; color: #909399;">暂无联系人信息</div>
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
    <el-dialog :title="title" v-model="open" width="1100px" append-to-body>
      <el-form ref="customerRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户简称" prop="customerSimpleName">
              <el-input v-model="form.customerSimpleName" placeholder="请输入客户简称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户全称" prop="customerAllName">
              <el-input v-model="form.customerAllName" placeholder="请输入客户全称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属行业" prop="industry">
              <el-select v-model="form.industry" placeholder="请选择所属行业" style="width: 100%">
                <el-option
                  v-for="dict in industry"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属区域" prop="region">
              <el-select v-model="form.region" placeholder="请选择所属区域" style="width: 100%">
                <el-option
                  v-for="dict in sys_yjqy"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="办公地址" prop="officeAddress">
              <el-input v-model="form.officeAddress" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="center">客户联系人信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="Plus" @click="handleAddCustomerContact">添加</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" icon="Delete" @click="handleDeleteCustomerContact">删除</el-button>
          </el-col>
        </el-row>
        <el-table :data="customerContactList" :row-class-name="rowCustomerContactIndex" @selection-change="handleCustomerContactSelectionChange" ref="customerContact">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="序号" align="center" prop="index" width="70"/>
          <el-table-column label="联系人姓名" prop="contactName" width="160">
            <template #default="scope">
              <el-input v-model="scope.row.contactName" placeholder="请输入联系人姓名" />
            </template>
          </el-table-column>
          <el-table-column label="联系人电话" prop="contactPhone" width="260">
            <template #default="scope">
              <el-input v-model="scope.row.contactPhone" placeholder="如：13800138000/010-88888888" />
            </template>
          </el-table-column>
          <el-table-column label="联系人标签" prop="contactTag" width="150">
            <template #default="scope">
              <el-select v-model="scope.row.contactTag" placeholder="请选择联系人标签">
                <el-option
                  v-for="dict in contact_tag"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="120">
            <template #default="scope">
              <el-input v-model="scope.row.remark" type="textarea" placeholder="请输入备注" />
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="80">
            <template #default="scope">
              <el-button link type="danger" @click="handleDeleteSingleContact(scope.$index)">删除</el-button>
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
import { listCustomer, getCustomer, delCustomer, addCustomer, updateCustomer, checkCustomerSimpleNameUnique } from "@/api/project/customer"
import { listUserByPost } from "@/api/system/user"

const { proxy } = getCurrentInstance()
const { contact_tag, sys_yjqy, industry } = proxy.useDict('contact_tag', 'sys_yjqy', 'industry')

const customerList = ref([])
const customerContactList = ref([])
const salesManagerList = ref([])
const customerSimpleNameList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const checkedCustomerContact = ref([])
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
    industry: null,
    region: null,
    salesManagerId: null,
  },
  rules: {
    customerSimpleName: [
      { required: true, message: "客户简称不能为空", trigger: "blur" },
      {
        trigger: "blur",
        validator: (rule, value, callback) => {
          if (!value || value.trim() === '') {
            callback()
            return
          }
          checkCustomerSimpleNameUnique(value, form.value.customerId).then(response => {
            if (response.data === false) {
              callback(new Error("客户简称已存在"))
            } else {
              callback()
            }
          }).catch(() => {
            callback()
          })
        }
      }
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
    officeAddress: null,
    delFlag: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  customerContactList.value = []
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
    customerContactList.value = response.data.customerContactList
    open.value = true
    title.value = "修改客户管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["customerRef"].validate(valid => {
    if (valid) {
      // 验证联系人必填字段
      for (let i = 0; i < customerContactList.value.length; i++) {
        const contact = customerContactList.value[i]
        if (!contact.contactName || contact.contactName.trim() === '') {
          proxy.$modal.msgError(`第${i + 1}个联系人的联系人姓名不能为空`)
          return
        }
        if (!contact.contactPhone || contact.contactPhone.trim() === '') {
          proxy.$modal.msgError(`第${i + 1}个联系人的联系人电话不能为空`)
          return
        }
        if (!contact.contactTag || contact.contactTag.trim() === '') {
          proxy.$modal.msgError(`第${i + 1}个联系人的联系人标签不能为空`)
          return
        }
      }

      form.value.customerContactList = customerContactList.value
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

  // 构建删除确认消息
  let confirmMessage = ''
  if (row.customerId) {
    // 单个删除：使用客户简称
    confirmMessage = `是否确认删除客户简称为"${row.customerSimpleName}"的数据项？`
  } else {
    // 批量删除：提取所有选中客户的简称
    const selectedCustomers = customerList.value.filter(item => ids.value.includes(item.customerId))
    const customerNames = selectedCustomers.map(item => item.customerSimpleName).join('、')
    confirmMessage = `是否确认删除客户简称为"${customerNames}"的数据项？`
  }

  proxy.$modal.confirm(confirmMessage).then(function() {
    return delCustomer(_customerIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 客户联系人序号 */
function rowCustomerContactIndex({ row, rowIndex }) {
  row.index = rowIndex + 1
}

/** 客户联系人添加按钮操作 */
function handleAddCustomerContact() {
  let obj = {}
  obj.contactName = ""
  obj.contactPhone = ""
  obj.contactTag = ""
  obj.remark = ""
  customerContactList.value.push(obj)
}

/** 客户联系人删除按钮操作 */
function handleDeleteCustomerContact() {
  if (checkedCustomerContact.value.length == 0) {
    proxy.$modal.msgError("请先选择要删除的客户联系人数据")
  } else {
    proxy.$modal.confirm('是否确认删除选中的联系人？').then(() => {
      const customerContacts = customerContactList.value
      const checkedCustomerContacts = checkedCustomerContact.value
      customerContactList.value = customerContacts.filter(function(item) {
        return checkedCustomerContacts.indexOf(item.index) == -1
      })
      proxy.$modal.msgSuccess("删除成功")
    }).catch(() => {})
  }
}

/** 客户联系人行内删除操作 */
function handleDeleteSingleContact(index) {
  proxy.$modal.confirm('是否确认删除该联系人？').then(() => {
    customerContactList.value[index].delFlag = '1'
    customerContactList.value.splice(index, 1)
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 复选框选中数据 */
function handleCustomerContactSelectionChange(selection) {
  checkedCustomerContact.value = selection.map(item => item.index)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('project/customer/export', {
    ...queryParams.value
  }, `customer_${new Date().getTime()}.xlsx`)
}

/** 加载销售负责人列表 */
function loadSalesManagers() {
  listUserByPost('xsfzr').then(response => {
    salesManagerList.value = response.data
  })
}

/** 加载客户简称列表 */
function loadCustomerSimpleNames() {
  listCustomer({ pageNum: 1, pageSize: 10000 }).then(response => {
    customerSimpleNameList.value = response.rows.map(item => ({
      value: item.customerSimpleName
    }))
  })
}

/** 客户简称搜索方法 */
function querySearchCustomer(queryString, cb) {
  const results = queryString
    ? customerSimpleNameList.value.filter(item =>
        item.value.toLowerCase().includes(queryString.toLowerCase())
      )
    : customerSimpleNameList.value
  cb(results)
}

onMounted(() => {
  loadSalesManagers()
  loadCustomerSimpleNames()
})

getList()
</script>
