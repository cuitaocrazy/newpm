<template>
  <div class="app-container contract-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="合同名称" prop="contractName">
        <el-autocomplete
          v-model="queryParams.contractName"
          :fetch-suggestions="fetchContractNameSuggestions"
          placeholder="请输入合同名称"
          clearable
          value-key="contractName"
          style="width: 200px"
          @select="handleQuery"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="合同所属部门" prop="deptId">
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
      <el-form-item label="关联客户" prop="customerId">
        <el-select v-model="queryParams.customerId" placeholder="请选择关联客户" clearable filterable style="width: 200px">
          <el-option
            v-for="customer in customerOptions"
            :key="customer.customerId"
            :label="customer.customerSimpleName"
            :value="customer.customerId"
          />
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
          <el-autocomplete
            v-model="queryParams.contractCode"
            :fetch-suggestions="fetchContractCodeSuggestions"
            placeholder="请输入合同编号"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
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
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
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
        >新增合同</el-button>
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
      ref="contractTableRef"
      v-loading="loading"
      :data="tableDataWithSummary"
      :height="tableHeight"
      :span-method="spanMethod"
      :default-sort="defaultSort"
      @sort-change="handleSortChange"
      border
      stripe
      style="width: 100%">
      <el-table-column label="序号" width="60" align="center" fixed="left" prop="index" v-if="columns.index.visible">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold;">合计</span>
          <span v-else-if="scope.row._isFirstRow">{{ scope.row._contractSeq }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同名称" align="left" header-align="center" prop="contractName" min-width="260" fixed="left" v-if="columns.contractName.visible">
        <template #default="scope">
          <div v-if="!scope.row.isSummary" class="contract-name-cell">
            <el-link
              v-if="checkPermi(['project:contract:query'])"
              class="contract-name-link"
              type="primary"
              :href="`/htkx/contract/detail/${scope.row.contractId}`"
              @click.prevent="handleView(scope.row)">
              {{ scope.row.contractName }}
            </el-link>
            <span v-else>{{ scope.row.contractName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="合同编号" align="center" prop="contractCode" width="160" show-overflow-tooltip v-if="columns.contractCode.visible" sortable="custom" />
      <el-table-column label="合同所属部门" align="center" prop="deptId" min-width="120" show-overflow-tooltip v-if="columns.deptId.visible">
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
      <el-table-column label="合同签订日期" align="center" prop="contractSignDate" width="130" v-if="columns.contractSignDate.visible" sortable="custom" >
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ parseTime(scope.row.contractSignDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同周期(月)" align="center" prop="contractPeriod" width="110" v-if="columns.contractPeriod.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.contractPeriod }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同金额(元)" align="right" prop="contractAmount" min-width="130" v-if="columns.contractAmount.visible" sortable="custom" >
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
      <el-table-column label="关联项目" align="left" prop="projectList" min-width="160" v-if="columns.projectList.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">
            {{ scope.row._project ? scope.row._project.projectName : '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="项目所属部门" align="center" prop="projectDept" min-width="120" show-overflow-tooltip v-if="columns.projectDept.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row._project?.deptName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目阶段" align="center" prop="projectStage" min-width="120" v-if="columns.projectStage.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">
            <dict-tag v-if="scope.row._project?.projectStage" :options="sys_xmjd" :value="scope.row._project.projectStage" />
            <span v-else>-</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="项目经理" align="center" prop="projectManager" min-width="100" v-if="columns.projectManager.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row._project?.projectManagerName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="二级区域" align="center" prop="secondaryRegion" min-width="100" v-if="columns.secondaryRegion.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row._project?.regionName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目预算(元)" align="right" prop="projectBudget" min-width="130" sortable="custom" v-if="columns.projectBudget.visible">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold; color: #409EFF;">{{ formatAmount(scope.row.projectBudgetTotal) }}</span>
          <span v-else>{{ scope.row._project ? formatAmount(scope.row._project.projectBudget) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预估工作量(人天)" align="right" prop="estimatedWorkload" min-width="140" sortable="custom" v-if="columns.estimatedWorkload.visible">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold; color: #409EFF;">{{ scope.row.estimatedWorkloadTotal }}</span>
          <span v-else>{{ scope.row._project?.estimatedWorkload != null ? scope.row._project.estimatedWorkload : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实际人天" align="right" prop="actualWorkload" min-width="110" sortable="custom" v-if="columns.actualWorkload.visible">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold; color: #409EFF;">{{ scope.row.actualWorkloadTotal }}</span>
          <span v-else>{{ scope.row._project?.actualWorkload != null ? toPersonDays(scope.row._project.actualWorkload, scope.row._project.adjustWorkload) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="公司收入确认年度" align="center" prop="revenueConfirmYear" min-width="140" v-if="columns.revenueConfirmYear.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">
            <dict-tag v-if="scope.row._project?.revenueConfirmYear" :options="sys_ndgl" :value="scope.row._project.revenueConfirmYear" />
            <span v-else>-</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="公司收入确认金额(元)" align="right" prop="revenueConfirmAmount" min-width="170" sortable="custom" v-if="columns.revenueConfirmAmount.visible">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold; color: #409EFF;">{{ formatAmount(scope.row.revenueConfirmAmountTotal) }}</span>
          <span v-else>{{ scope.row._project ? formatAmount(scope.row._project.confirmAmount) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收入确认状态" align="center" prop="revenueConfirmStatus" min-width="120" v-if="columns.revenueConfirmStatus.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">
            <dict-tag v-if="scope.row._project?.revenueConfirmStatus" :options="sys_qrzt" :value="scope.row._project.revenueConfirmStatus" />
            <span v-else>-</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="客户名称" align="center" prop="customerName" min-width="140" show-overflow-tooltip v-if="columns.customerId.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.customerName || '-' }}</span>
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
            <row-link-button :to="`/htkx/contract/detail/${scope.row.contractId}`" icon="View" label="详情" @navigate="handleView(scope.row)" v-hasPermi="['project:contract:query']" />
            <row-link-button :to="`/htkx/contract/edit/${scope.row.contractId}`" icon="Edit" label="编辑" @navigate="handleUpdate(scope.row)" v-hasPermi="['project:contract:edit']" />
            <el-button link type="primary" icon="Paperclip" @click="handleAttachment(scope.row)" v-hasPermi="['project:contract:attachment']">附件</el-button>
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
import { onBeforeRouteLeave } from "vue-router"
import { listContract, getContract, delContract, searchContracts } from "@/api/project/contract"
import { getDeptTree as fetchDeptTree } from "@/api/project/project"
import { listAllCustomer } from "@/api/project/customer"
import { listProject } from "@/api/project/project"
import { listAttachment, uploadAttachment, downloadAttachment, delAttachment, listAttachmentLog } from "@/api/project/attachment"
import { getToken } from "@/utils/auth"
import { checkPermi } from "@/utils/permission"
import { toPersonDays } from '@/utils/workload'

const { proxy } = getCurrentInstance()
const { sys_ndgl, sys_htlx, sys_htzt, sys_wdlx, sys_xmjd, sys_qrzt } = proxy.useDict('sys_ndgl', 'sys_htlx', 'sys_htzt', 'sys_wdlx', 'sys_xmjd', 'sys_qrzt')
const router = useRouter()
const route = useRoute()

const contractList = ref([])
const summaryData = ref({})

// 项目级字段列表（需要按项目拆行的列 prop）
const projectLevelProps = new Set([
  'projectList', 'projectDept', 'projectStage', 'projectManager',
  'secondaryRegion', 'projectBudget', 'estimatedWorkload', 'actualWorkload',
  'revenueConfirmYear', 'revenueConfirmAmount', 'revenueConfirmStatus'
])

const tableDataWithSummary = computed(() => {
  if (contractList.value.length === 0) {
    return []
  }

  // 使用后端返回的总计数据
  const summary = {
    isSummary: true,
    _isFirstRow: true,
    _rowSpan: 1,
    contractAmount: Number(summaryData.value.contractAmountSum || 0).toFixed(2),
    amountNoTax: Number(summaryData.value.amountNoTaxSum || 0).toFixed(2),
    projectBudgetTotal: Number(summaryData.value.projectBudgetSum || 0).toFixed(2),
    estimatedWorkloadTotal: Math.round(Number(summaryData.value.estimatedWorkloadSum || 0)),
    actualWorkloadTotal: toPersonDays(summaryData.value.actualWorkloadSum, summaryData.value.adjustWorkloadSum),
    revenueConfirmAmountTotal: Number(summaryData.value.revenueConfirmAmountSum || 0).toFixed(2),
  }

  // 将合同按 projectList 展开为多行
  const rows = []
  contractList.value.forEach((contract, contractIdx) => {
    const projects = contract.projectList && contract.projectList.length > 0
      ? contract.projectList
      : [null] // 无项目时也保留一行
    const seq = (queryParams.value.pageNum - 1) * queryParams.value.pageSize + contractIdx + 1
    projects.forEach((project, idx) => {
      rows.push({
        ...contract,
        _project: project,
        _isFirstRow: idx === 0,
        _rowSpan: idx === 0 ? projects.length : 0,
        _projectIndex: idx,
        _contractSeq: seq
      })
    })
  })

  return [summary, ...rows]
})

// 表格合并方法：合同级列做 rowSpan 合并
const mergeColumns = [
  'index', 'contractName', 'contractCode', 'deptId', 'contractType', 'contractStatus',
  'contractSignDate', 'contractPeriod', 'contractAmount', 'amountNoTax',
  'freeMaintenancePeriod', 'customerId', 'remark', 'createTime', 'createByName',
  'updateTime', 'updateByName'
]
function spanMethod({ row, column }) {
  if (row.isSummary) {
    return { rowspan: 1, colspan: 1 }
  }
  if (mergeColumns.includes(column.property)) {
    if (row._isFirstRow) {
      return { rowspan: row._rowSpan, colspan: 1 }
    } else {
      return { rowspan: 0, colspan: 0 }
    }
  }
}

const attachmentOpen = ref(false)
const logOpen = ref(false)
const loading = ref(true)
const attachmentLoading = ref(false)
const logLoading = ref(false)
const showSearch = ref(true)
const showMoreQuery = ref(false)
const total = ref(0)
const tableHeight = ref(600)
const contractTableRef = ref(null)
// el-table 的 default-sort 不是响应式属性：table-header 在挂载后的第 2 个 nextTick
// 解构读一次就不再看（table-header/index.mjs:76-81），之后再改都不会反映到表头箭头上。
// 因此恢复排序必须在本页 onMounted 里"同步"赋值——放到任何 await 之后箭头都不会出现。
// 空 prop 是安全的 no-op（store 内部有 if (prop) 守卫）。
// 已知降级：若被恢复的列已被用户在列显隐里隐藏，恢复会静默失效——数据仍有序、表头无箭头，不是缺陷。
const defaultSort = ref({ prop: '', order: '' })
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
  contractCode: { label: '合同编号', visible: true },
  deptId: { label: '合同所属部门', visible: true },
  contractType: { label: '合同类型', visible: true },
  contractStatus: { label: '合同状态', visible: true },
  contractSignDate: { label: '合同签订日期', visible: true },
  contractPeriod: { label: '合同周期', visible: true },
  contractAmount: { label: '合同金额', visible: true },
  amountNoTax: { label: '不含税金额', visible: true },
  freeMaintenancePeriod: { label: '免维期', visible: true },
  projectList: { label: '关联项目', visible: true },
  projectDept: { label: '项目所属部门', visible: true },
  projectStage: { label: '项目阶段', visible: true },
  projectManager: { label: '项目经理', visible: true },
  secondaryRegion: { label: '二级区域', visible: true },
  projectBudget: { label: '项目预算', visible: true },
  estimatedWorkload: { label: '预估工作量', visible: true },
  actualWorkload: { label: '实际人天', visible: true },
  revenueConfirmYear: { label: '公司收入确认年度', visible: true },
  revenueConfirmAmount: { label: '公司收入确认金额', visible: true },
  revenueConfirmStatus: { label: '收入确认状态', visible: true },
  customerId: { label: '客户名称', visible: true },
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
    contractList.value = response.rows
    total.value = response.total
    // 获取后端返回的总计数据（从 extra 中获取）
    summaryData.value = (response.extra && response.extra.summary) || {}
    loading.value = false
  })
}

// 排序处理
const handleSortChange = ({ column, prop, order }) => {
  if (!order) {
    // 取消排序。这里必须写 null 而不是 undefined：JSON.stringify 会丢掉值为 undefined 的键，
    // 而 restoreSearchState 用 Object.assign 合并，丢键就等于"取消排序"这个状态还原不回来。
    // tansParams(utils/ruoyi.ts) 对 null 与 undefined 一视同仁地不拼进 query string，上行报文不变。
    queryParams.value.orderByColumn = null
    queryParams.value.isAsc = null
    defaultSort.value = { prop: '', order: '' }
  } else {
    // 设置排序字段和排序方式
    // 将驼峰命名转换为下划线命名（后端数据库字段格式）
    const columnMap = {
      'contractCode': 'contract_code',
      'contractSignDate': 'contract_sign_date',
      'contractAmount': 'contract_amount',
      'projectBudget': 'project_budget_total',
      'estimatedWorkload': 'estimated_workload_total',
      'actualWorkload': 'actual_workload_total',
      'revenueConfirmAmount': 'revenue_confirm_amount_total',
    }
    const dbColumn = columnMap[prop] || prop
    const dir = order === 'ascending' ? 'asc' : 'desc'
    // 追加唯一次级排序键，使排序成为全序（Issue #16）：
    // contract_code / contract_sign_date / contract_amount 均存在大量并列值，
    // 排序键不唯一时 MySQL 对并列行的顺序不保证，每页 limit 独立排序会导致翻页重复与遗漏
    queryParams.value.orderByColumn = `${dbColumn} ${dir}, c.contract_id`
    queryParams.value.isAsc = dir
    // orderByColumn 是含唯一次级键的 SQL 片段，不可解析回推；el-table 需要的是 Vue 侧的 prop 名，
    // 所以另存一份给 default-sort 用，两者各管各的，避免任何一方被改写。
    defaultSort.value = { prop, order }
  }
  handleQuery()
}

const SEARCH_STATE_KEY = 'contract_search_state'

function saveSearchState() {
  // 本页两个异步下拉（deptOptions / customerOptions）在 setup 顶层无条件重载，
  // 组件每次创建都会重新拉取，故不入缓存；若将来任一下拉改成条件加载（如按年份级联），
  // 必须把它的 options 一并存进来，否则还原后下拉是空的。
  // （projectOptions 只服务于列表内的 getProjectName() 查表，不是查询条件，与缓存无关。）
  //
  // 必须 try/catch：本函数挂在 onBeforeRouteLeave 上，setItem 在配额满 / 隐私模式 /
  // 站点数据被禁时会抛异常，异常从路由守卫冒出去会让 vue-router 取消导航——
  // 实测表现是详情、编辑、侧边栏菜单全部点不动，用户被困在列表页。
  // 缓存失败应当退化为「这次不缓存」，绝不能升级为「走不掉」。
  try {
    sessionStorage.setItem(SEARCH_STATE_KEY, JSON.stringify({
      queryParams: { ...queryParams.value },
      sort: { ...defaultSort.value },
      showMoreQuery: showMoreQuery.value
    }))
  } catch {
    // 忽略：缓存不可用不应影响页面跳转
  }
}

function restoreSearchState() {
  try {
    const raw = sessionStorage.getItem(SEARCH_STATE_KEY)
    if (!raw) return false
    const state = JSON.parse(raw)
    Object.assign(queryParams.value, state.queryParams)
    defaultSort.value = state.sort?.prop ? { ...state.sort } : { prop: '', order: '' }
    // 「更多」区是 v-if，展开态不还原的话，恢复出来的 4 个条件在界面上完全看不见，
    // 用户只会看到"结果被筛过但找不到筛选条件"。
    showMoreQuery.value = !!state.showMoreQuery
    sessionStorage.removeItem(SEARCH_STATE_KEY)
    return true
  } catch {
    // 条目结构不兼容（如跨版本发布后的旧结构残留）时，一并清掉坏条目，
    // 否则它会一直躺在 sessionStorage 里，之后每次重建都白跑一次解析。
    try { sessionStorage.removeItem(SEARCH_STATE_KEY) } catch { /* 存储不可用，忽略 */ }
    return false
  }
}

// 用块体而非箭头隐式返回：守卫的返回值对 vue-router 有语义（false = 取消导航），
// 隐式返回会把 saveSearchState 未来可能的返回值意外变成导航开关。
onBeforeRouteLeave(() => {
  saveSearchState()
})

/** 查询部门下拉树结构 */
function getDeptTree() {
  fetchDeptTree().then(response => {
    // 与项目列表保持一致：用 ancestors 判断层级，只保留三级及以下机构
    const level3AndBelowDepts = (response.data || []).filter(dept => {
      if (!dept.ancestors) return false
      return dept.ancestors.split(',').length >= 3
    })
    const deptData = level3AndBelowDepts.map((dept) => ({
      ...dept,
      id: dept.deptId,
      label: dept.deptName
    }))
    deptOptions.value = proxy.handleTree(deptData, "id", "parentId")
  })
}

/** 查询客户列表 */
function getCustomerList() {
  listAllCustomer().then(response => {
    customerOptions.value = response.data
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
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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

/** 合同名称 autocomplete */
function fetchContractNameSuggestions(queryStr, cb) {
  searchContracts({ keyword: queryStr }).then(res => {
    cb(res.data || [])
  }).catch(() => cb([]))
}

/** 合同编号 autocomplete */
function fetchContractCodeSuggestions(queryStr, cb) {
  searchContracts({ keyword: queryStr }).then(res => {
    cb((res.data || []).map(c => ({ value: c.contractCode })).filter(c => c.value))
  }).catch(() => cb([]))
}

/** 重置按钮操作 */
function resetQuery() {
  sessionStorage.removeItem(SEARCH_STATE_KEY)
  proxy.resetForm("queryRef")
  // 「更多」区的 form-item 是 v-if 挂载的：若本次是从缓存恢复来的，这些 form-item 在恢复之后才挂载，
  // el-form 捕获到的 initialValue 就是恢复值，resetFields 会把它们"重置"回恢复值而不是清空，必须显式置空。
  queryParams.value.contractCode = null
  queryParams.value.contractSignDate = null
  queryParams.value.confirmAmount = null
  queryParams.value.confirmYear = null
  // orderByColumn / isAsc 不是 el-form 字段，resetForm 碰不到它们；
  // 而只清参数不清 el-table 内部的 sorting state，表头箭头也不会复位，两处都要清。
  queryParams.value.orderByColumn = null
  queryParams.value.isAsc = null
  defaultSort.value = { prop: '', order: '' }
  contractTableRef.value?.clearSort?.()
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
  // restoreSearchState 必须放在这里的第一行，三个时序约束的唯一交集：
  // ① 晚于子 form-item 的 onMounted（子先于父），否则「重置」会退回到恢复值而不是清空；
  // ② 早于首次 getList()，否则首屏拿到的是未排序数据、且不会补发请求；
  // ③ defaultSort 必须在本页 onMounted 内同步赋值，table-header 才读得到。
  restoreSearchState()
  calcTableHeight()
  window.addEventListener('resize', calcTableHeight)
  getList()
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

getDeptTree()
getCustomerList()
getProjectList()
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

  .contract-name-cell {
    .contract-name-link {
      word-break: break-all;
      white-space: normal;
      line-height: 1.5;
      text-align: left;
      display: inline-block;
    }
  }
}
</style>
