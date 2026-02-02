<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="合同编号" prop="contractCode">
        <el-input
          v-model="queryParams.contractCode"
          placeholder="请输入合同编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="合同名称" prop="contractName">
        <el-input
          v-model="queryParams.contractName"
          placeholder="请输入合同名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-select v-model="queryParams.customerId" placeholder="请选择客户" clearable filterable>
          <el-option
            v-for="customer in customerOptions"
            :key="customer.customerId"
            :label="customer.customerSimpleName"
            :value="customer.customerId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="部门" prop="deptId">
        <el-tree-select
          v-model="queryParams.deptId"
          :data="deptOptions"
          :props="{ value: 'id', label: 'label', children: 'children' }"
          value-key="id"
          placeholder="请选择部门"
          clearable
          check-strictly
        />
      </el-form-item>
      <el-form-item label="合同类型" prop="contractType">
        <el-select v-model="queryParams.contractType" placeholder="请选择合同类型" clearable>
          <el-option
            v-for="dict in sys_htlx"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="合同状态" prop="contractStatus">
        <el-select v-model="queryParams.contractStatus" placeholder="请选择合同状态" clearable>
          <el-option
            v-for="dict in sys_htzt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <template v-if="showMoreQuery">
        <el-form-item label="合同签订日期" prop="contractSignDate">
          <el-date-picker clearable
            v-model="queryParams.contractSignDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择合同签订日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="合同确认金额" prop="confirmAmount">
          <el-input
            v-model="queryParams.confirmAmount"
            placeholder="请输入合同确认金额"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="确认年份" prop="confirmYear">
          <el-select v-model="queryParams.confirmYear" placeholder="请选择确认年份" clearable>
            <el-option
              v-for="dict in sys_ndgl"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </template>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button
          :icon="showMoreQuery ? 'ArrowUp' : 'ArrowDown'"
          @click="showMoreQuery = !showMoreQuery"
        >
          {{ showMoreQuery ? '收起' : '更多' }}
        </el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['project:contract:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['project:contract:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="tableDataWithSummary">
      <el-table-column label="序号" width="60" align="center">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold;">合计</span>
          <span v-else>{{ indexMethod(scope.$index - 1) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同编号" align="center" prop="contractCode" width="150">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.contractCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同名称" align="center" prop="contractName" width="200" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.contractName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="客户" align="center" prop="customerId" width="180" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ getCustomerName(scope.row.customerId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="部门" align="center" prop="deptId" width="150" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ getDeptName(scope.row.deptId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同类型" align="center" prop="contractType" width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummary" :options="sys_htlx" :value="scope.row.contractType"/>
        </template>
      </el-table-column>
      <el-table-column label="合同状态" align="center" prop="contractStatus" width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummary" :options="sys_htzt" :value="scope.row.contractStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="合同签订日期" align="center" prop="contractSignDate" width="120">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ parseTime(scope.row.contractSignDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同周期(月)" align="center" prop="contractPeriod" width="110">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.contractPeriod }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同金额(含税)(元)" align="center" prop="contractAmount" width="160">
        <template #default="scope">
          <span :style="scope.row.isSummary ? 'font-weight: bold; color: #409EFF;' : ''">
            {{ formatAmount(scope.row.contractAmount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="税率(%)" align="center" prop="taxRate" width="100">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.taxRate }}</span>
        </template>
      </el-table-column>
      <el-table-column label="不含税金额(元)" align="center" prop="amountNoTax" width="140">
        <template #default="scope">
          <span :style="scope.row.isSummary ? 'font-weight: bold; color: #67C23A;' : ''">
            {{ formatAmount(scope.row.amountNoTax) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="税金(元)" align="center" prop="taxAmount" width="120">
        <template #default="scope">
          <span :style="scope.row.isSummary ? 'font-weight: bold; color: #E6A23C;' : ''">
            {{ formatAmount(scope.row.taxAmount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="合同确认金额(元)" align="center" prop="confirmAmount" width="150">
        <template #default="scope">
          <span :style="scope.row.isSummary ? 'font-weight: bold; color: #F56C6C;' : ''">
            {{ formatAmount(scope.row.confirmAmount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="确认年份" align="center" prop="confirmYear" width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummary" :options="sys_ndgl" :value="scope.row.confirmYear"/>
        </template>
      </el-table-column>
      <el-table-column label="免维期(月)" align="center" prop="freeMaintenancePeriod" width="110">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.freeMaintenancePeriod }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" width="150" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.remark }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="250" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <template v-if="!scope.row.isSummary">
            <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['project:contract:query']">详情</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:contract:edit']">编辑</el-button>
            <el-button link type="primary" icon="Paperclip" @click="handleAttachment(scope.row)" v-hasPermi="['project:attachment:list']">附件</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:contract:remove']">删除</el-button>
          </template>
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

    <!-- 附件管理对话框 -->
    <el-dialog title="附件管理" v-model="attachmentOpen" width="1000px" append-to-body>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-upload
            ref="uploadRef"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :data="uploadData"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :show-file-list="false"
            :limit="1"
          >
            <el-button type="primary" icon="Upload" v-hasPermi="['project:attachment:upload']">上传附件</el-button>
          </el-upload>
        </el-col>
      </el-row>

      <el-table v-loading="attachmentLoading" :data="attachmentList">
        <el-table-column label="文件名" align="center" prop="fileOriginalName" show-overflow-tooltip />
        <el-table-column label="文件类型" align="center" prop="fileType" width="100" />
        <el-table-column label="文件大小" align="center" prop="fileSize" width="120">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="文档类型" align="center" prop="documentType" width="120">
          <template #default="scope">
            <dict-tag :options="sys_wdlx" :value="scope.row.documentType"/>
          </template>
        </el-table-column>
        <el-table-column label="上传人" align="center" prop="uploadUserName" width="100" />
        <el-table-column label="下载次数" align="center" prop="downloadCount" width="100" />
        <el-table-column label="上传时间" align="center" prop="createTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="Download" @click="handleDownload(scope.row)" v-hasPermi="['project:attachment:download']">下载</el-button>
            <el-button link type="primary" icon="View" @click="handleViewLog(scope.row)" v-hasPermi="['project:attachment:log']">日志</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDeleteAttachment(scope.row)" v-hasPermi="['project:attachment:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="attachmentOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 操作日志对话框 -->
    <el-dialog title="操作日志" v-model="logOpen" width="900px" append-to-body>
      <el-table v-loading="logLoading" :data="logList">
        <el-table-column label="操作描述" align="center" prop="operationDesc" min-width="200" show-overflow-tooltip />
        <el-table-column label="文档类型" align="center" prop="documentType" width="120">
          <template #default="scope">
            <dict-tag :options="sys_wdlx" :value="scope.row.documentType"/>
          </template>
        </el-table-column>
        <el-table-column label="操作时间" align="center" prop="operationTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.operationTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作人" align="center" prop="operationUserName" width="120" />
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="logOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Contract">
import { listContract, getContract, delContract } from "@/api/project/contract"
import { deptTreeSelect } from "@/api/system/user"
import { listCustomer } from "@/api/project/customer"
import { listProject } from "@/api/project/project"
import { listAttachment, uploadAttachment, downloadAttachment, delAttachment, getAttachmentLog } from "@/api/project/attachment"
import { getToken } from "@/utils/auth"

const { proxy } = getCurrentInstance()
const { sys_ndgl, sys_htlx, sys_htzt, sys_wdlx } = proxy.useDict('sys_ndgl', 'sys_htlx', 'sys_htzt', 'sys_wdlx')
const router = useRouter()
const route = useRoute()

const contractList = ref([])
const tableDataWithSummary = computed(() => {
  if (contractList.value.length === 0) {
    return []
  }

  // 计算合计
  const summary = {
    isSummary: true,
    contractAmount: 0,
    amountNoTax: 0,
    taxAmount: 0,
    confirmAmount: 0
  }

  contractList.value.forEach(item => {
    summary.contractAmount += Number(item.contractAmount) || 0
    summary.amountNoTax += Number(item.amountNoTax) || 0
    summary.taxAmount += Number(item.taxAmount) || 0
    summary.confirmAmount += Number(item.confirmAmount) || 0
  })

  // 保留两位小数
  summary.contractAmount = summary.contractAmount.toFixed(2)
  summary.amountNoTax = summary.amountNoTax.toFixed(2)
  summary.taxAmount = summary.taxAmount.toFixed(2)
  summary.confirmAmount = summary.confirmAmount.toFixed(2)

  // 将合计行放在第一行
  return [summary, ...contractList.value]
})

const attachmentOpen = ref(false)
const logOpen = ref(false)
const loading = ref(true)
const attachmentLoading = ref(false)
const logLoading = ref(false)
const showSearch = ref(true)
const showMoreQuery = ref(false)
const total = ref(0)
const deptOptions = ref([])
const customerOptions = ref([])
const projectOptions = ref([])
const attachmentList = ref([])
const logList = ref([])
const currentContractId = ref(null)
const uploadUrl = ref(import.meta.env.VITE_APP_BASE_API + '/project/attachment/upload')
const uploadHeaders = ref({ Authorization: 'Bearer ' + getToken() })
const uploadData = ref({})
const maxFileSize = ref(50) // MB

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    contractCode: null,
    contractName: null,
    customerId: null,
    deptId: null,
    contractType: null,
    contractStatus: null,
    contractSignDate: null,
    confirmAmount: null,
    confirmYear: null
  }
})

const { queryParams } = toRefs(data)

/** 查询合同管理列表 */
function getList() {
  loading.value = true
  listContract(queryParams.value).then(response => {
    contractList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 查询部门下拉树结构 */
function getDeptTree() {
  deptTreeSelect().then(response => {
    deptOptions.value = response.data
  })
}

/** 查询客户列表 */
function getCustomerList() {
  listCustomer().then(response => {
    customerOptions.value = response.rows
  })
}

/** 查询项目列表 */
function getProjectList() {
  listProject().then(response => {
    projectOptions.value = response.rows
  })
}

/** 根据客户ID获取客户名称 */
function getCustomerName(customerId) {
  if (!customerId) return ''
  const customer = customerOptions.value.find(item => item.customerId === customerId)
  return customer ? customer.customerSimpleName : customerId
}

/** 根据部门ID获取部门名称 */
function getDeptName(deptId) {
  if (!deptId) return ''
  // 递归查找部门名称
  const findDept = (depts, id) => {
    if (!depts || !Array.isArray(depts)) return null
    for (const dept of depts) {
      if (dept.id === id) return dept.label
      if (dept.children && dept.children.length > 0) {
        const found = findDept(dept.children, id)
        if (found) return found
      }
    }
    return null
  }
  const name = findDept(deptOptions.value, deptId)
  return name || deptId
}

/** 根据项目ID获取项目名称 */
function getProjectName(projectId) {
  if (!projectId) return ''
  const project = projectOptions.value.find(item => item.projectId === projectId)
  return project ? project.projectName : projectId
}

/** 格式化金额，保留2位小数 */
function formatAmount(amount) {
  if (amount === null || amount === undefined || amount === '') return ''
  const num = parseFloat(amount)
  if (isNaN(num)) return amount
  return num.toFixed(2)
}

/** 序号计算方法 */
function indexMethod(index) {
  return (queryParams.value.pageNum - 1) * queryParams.value.pageSize + index + 1
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
  router.push({ path: '/project/contract/form' })
}

/** 查看详情按钮操作 */
function handleView(row) {
  router.push({ path: '/project/contract/detail', query: { contractId: row.contractId } })
}

/** 修改按钮操作 */
function handleUpdate(row) {
  router.push({ path: '/project/contract/form', query: { contractId: row.contractId } })
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除该合同数据？').then(function() {
    return delContract(row.contractId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('project/contract/export', {
    ...queryParams.value
  }, `contract_${new Date().getTime()}.xlsx`)
}

/** 附件按钮操作 */
function handleAttachment(row) {
  currentContractId.value = row.contractId
  uploadData.value = {
    businessType: 'contract',
    businessId: row.contractId
  }
  getAttachmentList()
  attachmentOpen.value = true
}

/** 查询附件列表 */
function getAttachmentList() {
  attachmentLoading.value = true
  const query = {
    businessType: 'contract',
    businessId: currentContractId.value
  }
  listAttachment(query).then(response => {
    attachmentList.value = response.rows
    attachmentLoading.value = false
  })
}

/** 上传前校验 */
function beforeUpload(file) {
  const fileSizeInMB = file.size / 1024 / 1024
  if (fileSizeInMB > maxFileSize.value) {
    proxy.$modal.msgError(`文件大小不能超过 ${maxFileSize.value}MB`)
    return false
  }

  const extension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
  const allowedExtensions = ['.doc', '.docx', '.pdf', '.xls', '.xlsx', '.txt', '.png', '.jpg', '.gz', '.zip', '.csv']
  if (!allowedExtensions.includes(extension)) {
    proxy.$modal.msgError('不支持的文件类型：' + extension)
    return false
  }

  return true
}

/** 上传成功回调 */
function handleUploadSuccess(response) {
  if (response.code === 200) {
    proxy.$modal.msgSuccess('上传成功')
    getAttachmentList()
  } else {
    proxy.$modal.msgError(response.msg || '上传失败')
  }
}

/** 上传失败回调 */
function handleUploadError() {
  proxy.$modal.msgError('上传失败')
}

/** 下载附件 */
function handleDownload(row) {
  proxy.$modal.confirm('是否确认下载附件"' + row.fileOriginalName + '"？').then(() => {
    return downloadAttachment(row.attachmentId)
  }).then(response => {
    const blob = new Blob([response])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = row.fileOriginalName
    link.click()
    URL.revokeObjectURL(link.href)
    proxy.$modal.msgSuccess('下载成功')
    getAttachmentList()
  }).catch(() => {})
}

/** 删除附件 */
function handleDeleteAttachment(row) {
  proxy.$modal.confirm('是否确认删除附件"' + row.fileOriginalName + '"？').then(() => {
    return delAttachment(row.attachmentId)
  }).then(() => {
    getAttachmentList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

/** 查看操作日志 */
function handleViewLog(row) {
  logLoading.value = true
  getAttachmentLog(row.attachmentId).then(response => {
    logList.value = response.rows
    logLoading.value = false
    logOpen.value = true
  })
}

/** 格式化文件大小 */
function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 监听路由变化，当从表单页面返回时自动查询
watch(() => route.query.t, (newVal) => {
  if (newVal) {
    getList()
  }
})

// 页面激活时刷新数据（从其他页面返回时）
onActivated(() => {
  getList()
})

getDeptTree()
getCustomerList()
getProjectList()
getList()
</script>
