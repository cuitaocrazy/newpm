<template>
  <div class="app-container customer-container">
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
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
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
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['project:customer:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="customerList" :height="tableHeight" border>
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
      <el-table-column label="销售负责人" align="center" prop="salesManagerName">
        <template #default="scope">
          {{ scope.row.salesManagerName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="办公地址" align="center" prop="officeAddress" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['project:customer:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:customer:edit']">编辑</el-button>
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
          <el-col :span="12">
            <el-form-item label="销售负责人" prop="salesManagerId">
              <user-select
                v-model="form.salesManagerId"
                post-code="xsfzr"
                placeholder="请选择销售负责人"
                clearable
                filterable
                style="width: 100%"
              />
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
        </el-row>
        <el-table :data="customerContactList" :row-class-name="rowCustomerContactIndex">
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

    <!-- 客户详情对话框 -->
    <el-dialog title="客户详情" v-model="openDetail" width="900px" append-to-body>
      <div class="detail-section">
        <div class="detail-section-title">客户基本信息</div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="客户简称">{{ detailData.customerSimpleName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="客户全称">{{ detailData.customerAllName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属行业">
            <dict-tag :options="industry" :value="detailData.industry" />
          </el-descriptions-item>
          <el-descriptions-item label="所属区域">
            <dict-tag :options="sys_yjqy" :value="detailData.region" />
          </el-descriptions-item>
          <el-descriptions-item label="销售负责人">{{ detailData.salesManagerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="办公地址">{{ detailData.officeAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建日期">{{ parseTime(detailData.createTime, '{y}-{m}-{d} {h}:{i}:{s}') || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新日期">{{ parseTime(detailData.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="detail-section" style="margin-top: 20px;">
        <div class="detail-section-title">联系人信息</div>
        <el-table :data="detailData.customerContactList || []" border>
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="联系人姓名" prop="contactName" />
          <el-table-column label="联系人电话" prop="contactPhone" />
          <el-table-column label="联系人标签" prop="contactTag">
            <template #default="scope">
              <dict-tag :options="contact_tag" :value="scope.row.contactTag" />
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" />
          <el-table-column label="创建时间" prop="createTime" width="180">
            <template #default="scope">
              {{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handleDetailEdit">编辑</el-button>
          <el-button @click="openDetail = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Customer">
import { listCustomer, getCustomer, delCustomer, addCustomer, updateCustomer, checkCustomerSimpleNameUnique } from "@/api/project/customer"
import { getUsersByPost } from "@/api/project/project"

const { proxy } = getCurrentInstance()
const { contact_tag, sys_yjqy, industry } = proxy.useDict('contact_tag', 'sys_yjqy', 'industry')

const customerList = ref([])
const customerContactList = ref([])
const salesManagerList = ref([])
const customerSimpleNameList = ref([])
const open = ref(false)
const openDetail = ref(false)
const detailData = ref({})
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const tableHeight = ref(600)
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

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加客户管理"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _customerId = row.customerId
  getCustomer(_customerId).then(response => {
    form.value = response.data
    customerContactList.value = response.data.customerContactList
    open.value = true
    title.value = "编辑客户信息"
  })
}

/** 详情按钮操作 */
function handleDetail(row) {
  getCustomer(row.customerId).then(response => {
    detailData.value = response.data
    openDetail.value = true
  })
}

/** 详情弹窗点击编辑 */
function handleDetailEdit() {
  openDetail.value = false
  handleUpdate(detailData.value)
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
  proxy.$modal.confirm(`是否确认删除客户简称为"${row.customerSimpleName}"的数据项及其所有关联数据，且无法恢复！是否继续？`).then(function() {
    return delCustomer(row.customerId)
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

/** 客户联系人行内删除操作 */
function handleDeleteSingleContact(index) {
  proxy.$modal.confirm('是否确认删除该联系人？').then(() => {
    customerContactList.value[index].delFlag = '1'
    customerContactList.value.splice(index, 1)
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('project/customer/export', {
    ...queryParams.value
  }, `客户信息_${new Date().getTime()}.xlsx`)
}

/** 加载销售负责人列表 */
function loadSalesManagers() {
  getUsersByPost('xsfzr').then(response => {
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
  loadSalesManagers()
  loadCustomerSimpleNames()
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
.customer-container {
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

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
</style>
