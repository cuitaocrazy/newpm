<template>
  <div class="app-container contract-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="合同名称" prop="contractName">
        <el-input
          v-model="queryParams.contractName"
          placeholder="请输入合同名称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 200px"
        />
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
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-select v-model="queryParams.customerId" placeholder="请选择客户" clearable filterable style="width: 200px">
          <el-option
            v-for="customer in customerOptions"
            :key="customer.customerId"
            :label="customer.customerSimpleName"
            :value="customer.customerId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="合同类型" prop="contractType">
        <el-select v-model="queryParams.contractType" placeholder="请选择合同类型" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_htlx"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="合同状态" prop="contractStatus">
        <el-select v-model="queryParams.contractStatus" placeholder="请选择合同状态" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_htzt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          ></el-option>
        </el-select>
      </el-form-item>
      <template v-if="showMoreQuery">
        <el-form-item label="合同编号" prop="contractCode">
          <el-input
            v-model="queryParams.contractCode"
            placeholder="请输入合同编号"
            clearable
            @keyup.enter="handleQuery"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="合同签订日期" prop="contractSignDate">
          <el-date-picker clearable
            v-model="queryParams.contractSignDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择合同签订日期"
            style="width: 200px">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="合同确认金额" prop="confirmAmount">
          <el-input
            v-model="queryParams.confirmAmount"
            placeholder="请输入合同确认金额"
            clearable
            @keyup.enter="handleQuery"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="确认年份" prop="confirmYear">
          <el-select v-model="queryParams.confirmYear" placeholder="请选择确认年份" clearable style="width: 200px">
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
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="tableDataWithSummary"
      :height="tableHeight"
      border
      stripe
      style="width: 100%">
      <el-table-column label="序号" width="60" align="center" fixed="left" v-if="columns.index.visible">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold;">合计</span>
          <span v-else>{{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同名称" align="center" prop="contractName" min-width="180" show-overflow-tooltip fixed="left" v-if="columns.contractName.visible">
        <template #default="scope">
          <el-link
            v-if="!scope.row.isSummary"
            type="primary"
            @click="handleView(scope.row)"
            @contextmenu.prevent="handleContextMenu($event, scope.row)">
            {{ scope.row.contractName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="关联项目" align="left" prop="projectList" min-width="160" v-if="columns.projectList.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">
            <template v-if="scope.row.projectList && scope.row.projectList.length > 0">
              <div v-for="(project, index) in scope.row.projectList" :key="project.projectId">
                {{ index + 1 }}、{{ project.projectName }}
              </div>
            </template>
            <span v-else>-</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="客户名称" align="center" prop="customerId" min-width="140" show-overflow-tooltip v-if="columns.customerId.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ getCustomerName(scope.row.customerId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="部门" align="center" prop="deptId" min-width="120" show-overflow-tooltip v-if="columns.deptId.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ getDeptName(scope.row.deptId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同类型" align="center" prop="contractType" width="100" v-if="columns.contractType.visible">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummary" :options="sys_htlx" :value="scope.row.contractType"/>
        </template>
      </el-table-column>
      <el-table-column label="合同状态" align="center" prop="contractStatus" width="100" v-if="columns.contractStatus.visible">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummary" :options="sys_htzt" :value="scope.row.contractStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="合同签订日期" align="center" prop="contractSignDate" width="120" v-if="columns.contractSignDate.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ parseTime(scope.row.contractSignDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同周期(月)" align="center" prop="contractPeriod" width="110" v-if="columns.contractPeriod.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.contractPeriod }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同金额(元)" align="right" prop="contractAmount" min-width="130" v-if="columns.contractAmount.visible">
        <template #default="scope">
          <span :style="scope.row.isSummary ? 'font-weight: bold; color: #409EFF;' : ''">
            {{ formatAmount(scope.row.contractAmount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="不含税金额(元)" align="right" prop="amountNoTax" min-width="140" v-if="columns.amountNoTax.visible">
        <template #default="scope">
          <span :style="scope.row.isSummary ? 'font-weight: bold; color: #67C23A;' : ''">
            {{ formatAmount(scope.row.amountNoTax) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="免维期(月)" align="center" prop="freeMaintenancePeriod" width="100" v-if="columns.freeMaintenancePeriod.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.freeMaintenancePeriod }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="150" show-overflow-tooltip v-if="columns.remark.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.remark }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建日期" align="center" prop="createTime" width="160" v-if="columns.createTime.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建人" align="center" prop="createByName" width="100" v-if="columns.createByName.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.createByName || scope.row.createBy }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最后更新日期" align="center" prop="updateTime" width="160" v-if="columns.updateTime.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最后更新人" align="center" prop="updateByName" width="100" v-if="columns.updateByName.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.updateByName || scope.row.updateBy }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" fixed="right" class-name="small-padding fixed-width" v-if="columns.actions.visible">
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
import { listAttachment, uploadAttachment, downloadAttachment, delAttachment, listAttachmentLog } from "@/api/project/attachment"
import { getToken } from "@/utils/auth"

const { proxy } = getCurrentInstance()
const { sys_ndgl, sys_htlx, sys_htzt, sys_wdlx } = proxy.useDict('sys_ndgl', 'sys_htlx', 'sys_htzt', 'sys_wdlx')
const router = useRouter()
const route = useRoute()

const contractList = ref([])
const summaryData = ref({})
const tableDataWithSummary = computed(() => {
  if (contractList.value.length === 0) {
    return []
  }

  // 使用后端返回的总计数据
  const summary = {
    isSummary: true,
    contractAmount: Number(summaryData.value.contractAmountSum || 0).toFixed(2),
    amountNoTax: Number(summaryData.value.amountNoTaxSum || 0).toFixed(2)
  }

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
const tableHeight = ref(600)
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

// 列显隐信息
const columns = ref({
  index: { label: '序号', visible: true },
  contractName: { label: '合同名称', visible: true },
  projectList: { label: '关联项目', visible: true },
  customerId: { label: '客户名称', visible: true },
  deptId: { label: '部门', visible: true },
  contractType: { label: '合同类型', visible: true },
  contractStatus: { label: '合同状态', visible: true },
  contractSignDate: { label: '合同签订日期', visible: true },
  contractPeriod: { label: '合同周期', visible: true },
  contractAmount: { label: '合同金额', visible: true },
  amountNoTax: { label: '不含税金额', visible: true },
  freeMaintenancePeriod: { label: '免维期', visible: true },
  remark: { label: '备注', visible: true },
  createTime: { label: '创建日期', visible: true },
  createByName: { label: '创建人', visible: true },
  updateTime: { label: '最后更新日期', visible: true },
  updateByName: { label: '最后更新人', visible: true },
  actions: { label: '操作', visible: true }
})

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
    console.log('合同列表响应数据:', response)
    contractList.value = response.rows
    total.value = response.total
    // 获取后端返回的总计数据（从 extra 中获取）
    summaryData.value = (response.extra && response.extra.summary) || {}
    console.log('总计数据:', summaryData.value)
    loading.value = false
  })
}

/** 查询部门下拉树结构 */
function getDeptTree() {
  deptTreeSelect().then(response => {
    // 过滤部门树，只保留第三级及以下的部门
    deptOptions.value = filterDeptFromLevel3(response.data)
  })
}

/** 过滤部门树，从第三级开始展示 */
function filterDeptFromLevel3(deptTree, level = 1) {
  if (!deptTree || !Array.isArray(deptTree)) {
    return []
  }

  const result = []

  for (const dept of deptTree) {
    if (level >= 3) {
      // 第三级及以下，保留该节点
      const newDept = { ...dept }
      if (dept.children && dept.children.length > 0) {
        newDept.children = filterDeptFromLevel3(dept.children, level + 1)
      }
      result.push(newDept)
    } else {
      // 第一级和第二级，只递归处理子节点
      if (dept.children && dept.children.length > 0) {
        const childrenResult = filterDeptFromLevel3(dept.children, level + 1)
        result.push(...childrenResult)
      }
    }
  }

  return result
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
  // 跳过合计行，从实际数据开始计算
  if (index < 0) return ''

  const dataList = contractList.value
  let contractIndex = 0
  let currentIndex = 0

  // 遍历数据，找到当前行对应的合同序号
  for (let i = 0; i < dataList.length; i++) {
    if (currentIndex === index) {
      // 如果是第一行，显示序号
      if (dataList[i].isFirstRow) {
        return (queryParams.value.pageNum - 1) * queryParams.value.pageSize + contractIndex + 1
      } else {
        return '' // 非第一行不显示序号
      }
    }

    // 如果是第一行，增加合同计数
    if (dataList[i].isFirstRow) {
      contractIndex++
    }
    currentIndex++
  }

  return ''
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
  router.push({ path: '/htkx/contract/add' })
}

/** 查看详情按钮操作 */
function handleView(row) {
  router.push({ path: `/htkx/contract/detail/${row.contractId}` })
}

/** 右键菜单 - 在新标签页打开 */
function handleContextMenu(event, row) {
  event.preventDefault()
  const url = router.resolve({ path: `/htkx/contract/detail/${row.contractId}` }).href
  window.open(url, '_blank')
}

/** 修改按钮操作 */
function handleUpdate(row) {
  router.push({ path: `/htkx/contract/edit/${row.contractId}` })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const contractName = row.contractName || '该合同'
  proxy.$modal.confirm(`此操作将永久删除合同【${contractName}】及其所有关联数据，且无法恢复！是否继续？`).then(function() {
    return delContract(row.contractId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch((error) => {
    // 如果后端返回错误信息，会自动显示
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  })
}

/** 导出按钮操作 */
function handleExport() {
  const timestamp = proxy.parseTime(new Date(), '{y}{m}{d}{h}{i}{s}')
  proxy.download('project/contract/export', {
    ...queryParams.value
  }, `合同管理_${timestamp}.xlsx`)
}

/** 附件按钮操作 */
function handleAttachment(row) {
  router.push({ path: `/htkx/contract/attachment/${row.contractId}` })
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
  listAttachmentLog('contract', currentContractId.value).then(response => {
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

/** 计算表格高度 */
function calcTableHeight() {
  nextTick(() => {
    const windowHeight = window.innerHeight
    const searchHeight = showSearch.value ? (showMoreQuery.value ? 160 : 100) : 0
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

// 监听更多查询展开/收起
watch(showMoreQuery, () => {
  calcTableHeight()
})

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

<style scoped lang="scss">
.contract-container {
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
