<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="140px">
      <!-- 主要查询条件 -->
      <el-form-item label="合同名称" prop="contractName">
        <el-input
          v-model="queryParams.contractName"
          placeholder="请输入合同名称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="合同编号" prop="contractCode">
        <el-input
          v-model="queryParams.contractCode"
          placeholder="请输入合同编号"
          clearable
          @keyup.enter="handleQuery"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="合同状态" prop="contractStatus">
        <el-select v-model="queryParams.contractStatus" placeholder="请选择合同状态" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_htzt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="客户名称" prop="customerName">
        <el-input
          v-model="queryParams.customerName"
          placeholder="请输入客户名称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="付款状态" prop="paymentStatus">
        <el-select v-model="queryParams.paymentStatus" placeholder="请选择付款状态" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_fkzt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>

      <!-- 折叠的查询条件（在按钮上方） -->
      <template v-if="showMoreSearch">
        <el-form-item label="预计回款季度" prop="expectedQuarter">
          <el-select v-model="queryParams.expectedQuarter" placeholder="请选择预计回款季度" clearable style="width: 200px">
            <el-option
              v-for="dict in sys_jdgl"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="款项确认年份" prop="paymentConfirmYear">
          <el-select v-model="queryParams.paymentConfirmYear" placeholder="请选择款项确认年份" clearable style="width: 200px">
            <el-option
              v-for="dict in sys_ndgl"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="付款方式名称" prop="paymentMethodName">
          <el-input
            v-model="queryParams.paymentMethodName"
            placeholder="请输入付款方式名称"
            clearable
            @keyup.enter="handleQuery"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="是否涉及违约扣款" prop="hasPenalty">
          <el-select v-model="queryParams.hasPenalty" placeholder="请选择" clearable style="width: 200px">
            <el-option label="是" value="1" />
            <el-option label="否" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="实际回款季度" prop="actualQuarter">
          <el-select v-model="queryParams.actualQuarter" placeholder="请选择实际回款季度" clearable style="width: 200px">
            <el-option
              v-for="dict in sys_jdgl"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="实际回款日期" prop="actualPaymentDateRange">
          <el-date-picker
            v-model="actualPaymentDateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
      </template>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="text" @click="showMoreSearch = !showMoreSearch">
          {{ showMoreSearch ? '收起' : '更多' }}
          <el-icon><component :is="showMoreSearch ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
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
          v-hasPermi="['project:payment:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['project:payment:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="tableDataWithSummary"
      border
      stripe
      :span-method="spanMethod"
      style="width: 100%">
      <el-table-column label="序号" width="60" align="center" fixed="left" v-if="columns.index.visible">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold;">合计</span>
          <span v-else>{{ scope.$index }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同名称" header-align="center" align="left" prop="contractName" min-width="260" fixed="left" v-if="columns.contractName.visible">
        <template #default="scope">
          <div v-if="!scope.row.isSummary" class="contract-name-cell">
            <el-link
              type="primary"
              :underline="false"
              @click="handleViewContract(scope.row)"
              class="contract-name-link"
              :title="scope.row.contractName">
              {{ scope.row.contractName }}
            </el-link>
            <el-button
              v-if="scope.row.isFirstRow"
              link
              type="success"
              size="small"
              icon="Plus"
              @click="handleAddPaymentForContract(scope.row)"
              v-hasPermi="['project:payment:add']"
              class="add-payment-btn">
              新增付款里程碑
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="合同状态" align="center" prop="contractStatus" width="100" v-if="columns.contractStatus.visible">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummary" :options="sys_htzt" :value="scope.row.contractStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="客户名称" align="center" prop="customerName" min-width="140" show-overflow-tooltip v-if="columns.customerName.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.customerName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同金额（元）" align="right" prop="contractAmount" min-width="130" v-if="columns.contractAmount.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ formatMoney(scope.row.contractAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同签订日期" align="center" prop="contractSignDate" width="120" v-if="columns.contractSignDate.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.contractSignDate ? parseTime(scope.row.contractSignDate, '{y}-{m}-{d}') : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="免维期（月）" align="center" prop="freeMaintenancePeriod" width="100" v-if="columns.freeMaintenancePeriod.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.freeMaintenancePeriod || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同所属团队" align="center" prop="deptName" min-width="120" show-overflow-tooltip v-if="columns.deptName.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.deptName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="付款里程碑名称" align="center" prop="paymentMethodName" min-width="180" show-overflow-tooltip v-if="columns.paymentMethodName.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.paymentMethodName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="付款状态" align="center" prop="paymentStatus" width="100" v-if="columns.paymentStatus.visible">
        <template #default="scope">
          <template v-if="!scope.row.isSummary">
            <dict-tag v-if="scope.row.paymentStatus" :options="sys_fkzt" :value="scope.row.paymentStatus"/>
            <span v-else>-</span>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="预计回款季度" align="center" prop="expectedQuarter" width="120" v-if="columns.expectedQuarter.visible">
        <template #default="scope">
          <template v-if="!scope.row.isSummary">
            <dict-tag v-if="scope.row.expectedQuarter" :options="sys_jdgl" :value="scope.row.expectedQuarter"/>
            <span v-else>-</span>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="实际回款季度" align="center" prop="actualQuarter" width="120" v-if="columns.actualQuarter.visible">
        <template #default="scope">
          <template v-if="!scope.row.isSummary">
            <dict-tag v-if="scope.row.actualQuarter" :options="sys_jdgl" :value="scope.row.actualQuarter"/>
            <span v-else>-</span>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="实际回款日期" align="center" prop="actualPaymentDate" width="120" v-if="columns.actualPaymentDate.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.actualPaymentDate ? parseTime(scope.row.actualPaymentDate, '{y}-{m}-{d}') : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="里程碑确认年份" align="center" prop="confirmYear" width="130" v-if="columns.confirmYear.visible">
        <template #default="scope">
          <template v-if="!scope.row.isSummary">
            <dict-tag v-if="scope.row.confirmYear" :options="sys_ndgl" :value="scope.row.confirmYear"/>
            <span v-else>-</span>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="付款金额（元）" align="right" prop="paymentAmount" min-width="130" v-if="columns.paymentAmount.visible">
        <template #default="scope">
          <span :style="scope.row.isSummary ? 'font-weight: bold; color: #409EFF;' : ''">
            {{ scope.row.paymentAmount ? formatMoney(scope.row.paymentAmount) : '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="更新人" align="center" prop="updateByName" width="100" v-if="columns.updateByName.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ getDisplayUpdateBy(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="160" v-if="columns.updateTime.visible">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ getDisplayUpdateTime(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" fixed="right" class-name="small-padding fixed-width" v-if="columns.actions.visible">
        <template #default="scope">
          <template v-if="!scope.row.isSummary">
            <template v-if="scope.row.paymentId">
              <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['project:payment:query']">详情</el-button>
              <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:payment:edit']">编辑</el-button>
              <el-button link type="primary" icon="Paperclip" @click="handleAttachment(scope.row)" v-hasPermi="['project:payment:query']">附件</el-button>
              <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:payment:remove']">删除</el-button>
            </template>
            <template v-else>
              <span style="color: #909399;">暂无付款记录</span>
            </template>
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
  </div>
</template>

<script setup name="Payment">
import { listPaymentWithContracts, delPayment, checkAttachments, sumPaymentAmount } from "@/api/project/payment"
import { watch } from 'vue'
import { useRoute } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const { sys_ndgl, sys_jdgl, sys_fkzt, sys_htzt } = proxy.useDict('sys_ndgl', 'sys_jdgl', 'sys_fkzt', 'sys_htzt')

const contractList = ref([])
const displayList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const showMoreSearch = ref(false)
const total = ref(0)
const totalAmount = ref(0)
const tableDataWithSummary = computed(() => {
  if (displayList.value.length === 0) {
    return []
  }

  // 合计行数据
  const summary = {
    isSummary: true,
    paymentAmount: Number(totalAmount.value || 0).toFixed(2)
  }

  // 将合计行放在第一行
  return [summary, ...displayList.value]
})
const actualPaymentDateRange = ref([])

// 列显隐信息
const columns = ref({
  index: { label: '序号', visible: true },
  contractName: { label: '合同名称', visible: true },
  contractStatus: { label: '合同状态', visible: true },
  customerName: { label: '客户名称', visible: true },
  contractAmount: { label: '合同金额', visible: true },
  contractSignDate: { label: '合同签订日期', visible: true },
  freeMaintenancePeriod: { label: '免维期', visible: true },
  deptName: { label: '合同所属团队', visible: true },
  paymentMethodName: { label: '付款里程碑名称', visible: true },
  paymentStatus: { label: '付款状态', visible: true },
  expectedQuarter: { label: '预计回款季度', visible: true },
  actualQuarter: { label: '实际回款季度', visible: true },
  actualPaymentDate: { label: '实际回款日期', visible: true },
  confirmYear: { label: '里程碑确认年份', visible: true },
  paymentAmount: { label: '付款金额', visible: true },
  updateByName: { label: '更新人', visible: true },
  updateTime: { label: '更新时间', visible: true },
  actions: { label: '操作', visible: true }
})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    contractName: null,
    contractCode: null,
    contractStatus: null,
    customerName: null,
    paymentMethodName: null,
    hasPenalty: null,
    paymentStatus: null,
    expectedQuarter: null,
    actualQuarter: null,
    paymentConfirmYear: null,
    actualPaymentDateStart: null,
    actualPaymentDateEnd: null,
  }
})

const { queryParams } = toRefs(data)

/** 查询款项管理列表 */
function getList() {
  loading.value = true

  // 处理日期范围
  if (actualPaymentDateRange.value && actualPaymentDateRange.value.length === 2) {
    queryParams.value.actualPaymentDateStart = actualPaymentDateRange.value[0]
    queryParams.value.actualPaymentDateEnd = actualPaymentDateRange.value[1]
  } else {
    queryParams.value.actualPaymentDateStart = null
    queryParams.value.actualPaymentDateEnd = null
  }

  // 查询列表数据
  listPaymentWithContracts(queryParams.value).then(response => {
    contractList.value = response.rows
    total.value = response.total

    // 处理数据：将合同和付款里程碑展开为平铺列表
    processDisplayList()

    loading.value = false
  })

  // 查询总金额
  sumPaymentAmount(queryParams.value).then(response => {
    totalAmount.value = response.data || 0
  })
}

/** 处理展示列表：将合同和付款里程碑展开 */
function processDisplayList() {
  const list = []

  contractList.value.forEach(contract => {
    if (contract.paymentList && contract.paymentList.length > 0) {
      // 有付款里程碑的合同：每个付款里程碑一行
      contract.paymentList.forEach((payment, index) => {
        list.push({
          // 合同信息
          contractId: contract.contractId,
          contractCode: contract.contractCode,
          contractName: contract.contractName,
          contractStatus: contract.contractStatus,
          customerName: contract.customerName,
          contractAmount: contract.contractAmount,
          contractSignDate: contract.contractSignDate,
          freeMaintenancePeriod: contract.freeMaintenancePeriod,
          deptName: contract.deptName,
          // 付款里程碑信息
          paymentId: payment.paymentId,
          paymentMethodName: payment.paymentMethodName,
          paymentAmount: payment.paymentAmount,
          hasPenalty: payment.hasPenalty,
          penaltyAmount: payment.penaltyAmount,
          paymentStatus: payment.paymentStatus,
          expectedQuarter: payment.expectedQuarter,
          actualQuarter: payment.actualQuarter,
          submitAcceptanceDate: payment.submitAcceptanceDate,
          actualPaymentDate: payment.actualPaymentDate,
          confirmYear: payment.confirmYear,
          createBy: payment.createBy,
          createTime: payment.createTime,
          updateBy: payment.updateBy,
          updateTime: payment.updateTime,
          createByName: payment.createByName,
          updateByName: payment.updateByName,
          // 合并单元格标记
          isFirstRow: index === 0,
          rowSpan: index === 0 ? contract.paymentList.length : 0
        })
      })
    } else {
      // 没有付款里程碑的合同：显示一行，操作栏显示"暂无付款记录"
      list.push({
        // 合同信息
        contractId: contract.contractId,
        contractCode: contract.contractCode,
        contractName: contract.contractName,
        contractStatus: contract.contractStatus,
        customerName: contract.customerName,
        contractAmount: contract.contractAmount,
        contractSignDate: contract.contractSignDate,
        freeMaintenancePeriod: contract.freeMaintenancePeriod,
        deptName: contract.deptName,
        // 没有付款里程碑
        paymentId: null,
        paymentMethodName: null,
        paymentAmount: null,
        paymentStatus: null,
        expectedQuarter: null,
        actualQuarter: null,
        actualPaymentDate: null,
        confirmYear: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        createByName: null,
        updateByName: null,
        // 合并单元格标记
        isFirstRow: true,
        rowSpan: 1
      })
    }
  })

  displayList.value = list
}

/** 合并单元格方法 */
function spanMethod({ row, column, rowIndex, columnIndex }) {
  // 合计行不进行单元格合并
  if (row.isSummary) {
    return {
      rowspan: 1,
      colspan: 1
    }
  }

  // 需要合并的列：合同名称、合同状态、客户名称、合同金额、合同签订日期、免维期、合同所属团队
  const mergeColumns = ['contractName', 'contractStatus', 'customerName', 'contractAmount', 'contractSignDate', 'freeMaintenancePeriod', 'deptName']
  const columnProperty = column.property

  if (mergeColumns.includes(columnProperty)) {
    if (row.isFirstRow) {
      return {
        rowspan: row.rowSpan,
        colspan: 1
      }
    } else {
      return {
        rowspan: 0,
        colspan: 0
      }
    }
  }
}

/** 金额格式化（千分位） */
function formatMoney(amount) {
  if (amount === null || amount === undefined || amount === '') {
    return '0.00'
  }
  const num = Number(amount).toFixed(2)
  return num.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/** 获取显示的更新人 */
function getDisplayUpdateBy(row) {
  // 优先显示更新人姓名，如果为空则显示创建人姓名
  if (row.updateByName) return row.updateByName
  if (row.createByName) return row.createByName
  if (row.updateBy) return row.updateBy
  if (row.createBy) return row.createBy
  return '-'
}

/** 获取显示的更新时间 */
function getDisplayUpdateTime(row) {
  // 优先显示更新时间，如果为空则显示创建时间
  const time = row.updateBy ? row.updateTime : row.createTime
  return time ? proxy.parseTime(time, '{y}-{m}-{d} {h}:{i}:{s}') : '-'
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  actualPaymentDateRange.value = []
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 新增按钮操作 */
function handleAdd() {
  router.push('/htkx/payment/add')
}

/** 查看合同详情 */
function handleViewContract(row) {
  router.push({
    path: '/htkx/contract/detail/' + row.contractId,
    query: {
      from: 'payment'
    }
  })
}

/** 为指定合同新增付款里程碑 */
function handleAddPaymentForContract(row) {
  router.push({
    path: '/htkx/payment/add',
    query: {
      contractId: row.contractId,
      contractName: row.contractName
    }
  })
}

/** 编辑按钮操作 */
function handleUpdate(row) {
  router.push('/htkx/payment/edit/' + row.paymentId)
}

/** 详情按钮操作 */
function handleDetail(row) {
  router.push('/htkx/payment/detail/' + row.paymentId)
}

/** 附件按钮操作 */
function handleAttachment(row) {
  router.push('/htkx/payment/attachment/' + row.paymentId)
}

/** 删除按钮操作 */
function handleDelete(row) {
  const paymentId = row.paymentId
  const paymentName = row.paymentMethodName
  const contractName = row.contractName

  // 检查是否有附件
  checkAttachments(paymentId).then(response => {
    const hasAttachments = response.data > 0

    if (hasAttachments) {
      // 如果有附件，不允许删除
      proxy.$modal.msgError(`合同【${contractName}】下付款里程碑【${paymentName}】已上传附件，无法进行删除操作！`)
      return
    }

    // 没有附件，可以删除
    const message = `此操作将永久删除付款里程碑【${paymentName}】，且无法恢复！是否继续？`
    proxy.$modal.confirm(message).then(() => {
      return delPayment(paymentId)
    }).then(() => {
      getList()
      proxy.$modal.msgSuccess("删除成功")
    }).catch(() => {})
  })
}

/** 导出按钮操作 */
function handleExport() {
  const timestamp = proxy.parseTime(new Date(), '{y}{m}{d}{h}{i}{s}')
  proxy.download('project/payment/export', {
    ...queryParams.value
  }, `付款里程碑_${timestamp}.xlsx`)
}

// 初始化
getList()

// 监听路由变化，刷新列表
watch(() => route.query.t, (newVal) => {
  if (newVal) {
    getList()
  }
})
</script>

<style scoped lang="scss">
.app-container {
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

    // 合同名称单元格样式
    .contract-name-cell {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;
      padding: 4px 0;
      width: 100%;

      .contract-name-link {
        font-weight: 500;
        cursor: pointer;
        word-break: break-all;
        white-space: normal;
        line-height: 1.5;
        text-align: left;
        display: inline-block;

        &:hover {
          color: #409eff;
        }
      }

      .add-payment-btn {
        font-size: 12px;
        padding: 0;
        height: auto;
        align-self: flex-start;
        margin-left: 0;
      }
    }
  }

  :deep(.el-pagination) {
    margin-top: 15px;
    text-align: right;
  }
}
</style>
