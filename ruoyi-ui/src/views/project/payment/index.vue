<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="120px">
      <!-- 核心条件（5个） -->
      <el-form-item label="合同选择" prop="contractId">
        <el-autocomplete
          v-model="contractSearchText"
          :fetch-suggestions="searchContract"
          placeholder="请输入合同名称或编号搜索"
          clearable
          @select="handleContractSelect"
          @clear="handleContractClear"
          style="width: 240px"
        >
          <template #default="{ item }">
            <div>{{ item.label }}</div>
          </template>
        </el-autocomplete>
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
      <el-form-item label="款项确认年份" prop="confirmYear">
        <el-select v-model="queryParams.confirmYear" placeholder="请选择款项确认年份" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_ndgl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>

      <!-- 更多条件（折叠区域） -->
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="text" @click="showMoreSearch = !showMoreSearch">
          {{ showMoreSearch ? '收起' : '更多条件' }}
          <el-icon><component :is="showMoreSearch ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
        </el-button>
      </el-form-item>

      <!-- 次要条件（折叠显示） -->
      <template v-if="showMoreSearch">
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
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="paymentList">
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column label="里程碑名称" align="center" prop="paymentMethodName" width="180" show-overflow-tooltip />
      <el-table-column label="关联合同" align="center" prop="contractName" width="180" show-overflow-tooltip />
      <el-table-column label="付款金额（元）" align="center" prop="paymentAmount" width="130">
        <template #default="scope">
          {{ formatMoney(scope.row.paymentAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="是否涉及违约扣款" align="center" prop="hasPenalty" width="140">
        <template #default="scope">
          <el-tag :type="scope.row.hasPenalty === '1' ? 'danger' : 'success'">
            {{ scope.row.hasPenalty === '1' ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="扣款金额（元）" align="center" prop="penaltyAmount" width="130">
        <template #default="scope">
          {{ scope.row.hasPenalty === '1' ? formatMoney(scope.row.penaltyAmount) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="付款状态" align="center" prop="paymentStatus" width="100">
        <template #default="scope">
          <dict-tag :options="sys_fkzt" :value="scope.row.paymentStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="预计回款季度" align="center" prop="expectedQuarter" width="120">
        <template #default="scope">
          <dict-tag :options="sys_jdgl" :value="scope.row.expectedQuarter"/>
        </template>
      </el-table-column>
      <el-table-column label="实际回款季度" align="center" prop="actualQuarter" width="120">
        <template #default="scope">
          <dict-tag v-if="scope.row.actualQuarter" :options="sys_jdgl" :value="scope.row.actualQuarter"/>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="款项确认年份" align="center" prop="confirmYear" width="120">
        <template #default="scope">
          <dict-tag :options="sys_ndgl" :value="scope.row.confirmYear"/>
        </template>
      </el-table-column>
      <el-table-column label="实际回款日期" align="center" prop="actualPaymentDate" width="120">
        <template #default="scope">
          <span>{{ scope.row.actualPaymentDate ? parseTime(scope.row.actualPaymentDate, '{y}-{m}-{d}') : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:payment:edit']">编辑</el-button>
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['project:payment:query']">详情</el-button>
          <el-button link type="primary" icon="Paperclip" @click="handleAttachment(scope.row)" v-hasPermi="['project:payment:query']">附件</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:payment:remove']">删除</el-button>
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
import { listPayment, delPayment, checkAttachments } from "@/api/project/payment"
import { searchContracts } from "@/api/project/contract"
import { watch } from 'vue'
import { useRoute } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const { sys_ndgl, sys_jdgl, sys_fkzt, sys_htzt } = proxy.useDict('sys_ndgl', 'sys_jdgl', 'sys_fkzt', 'sys_htzt')

const paymentList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const showMoreSearch = ref(false)
const total = ref(0)
const contractSearchText = ref('')
const actualPaymentDateRange = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    contractId: null,
    contractStatus: null,
    paymentMethodName: null,
    hasPenalty: null,
    paymentStatus: null,
    expectedQuarter: null,
    actualQuarter: null,
    confirmYear: null,
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

  listPayment(queryParams.value).then(response => {
    paymentList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 合同搜索 */
function searchContract(queryString, cb) {
  if (!queryString) {
    cb([])
    return
  }
  searchContracts({ keyword: queryString }).then(response => {
    const results = response.data.map(item => ({
      value: item.contractName,
      contractId: item.contractId,
      contractCode: item.contractCode,
      label: `${item.contractName} (${item.contractCode})`
    }))
    cb(results)
  })
}

/** 合同选择 */
function handleContractSelect(item) {
  queryParams.value.contractId = item.contractId
  contractSearchText.value = item.label
}

/** 合同清空 */
function handleContractClear() {
  queryParams.value.contractId = null
  contractSearchText.value = ''
}

/** 金额格式化（千分位） */
function formatMoney(amount) {
  if (amount === null || amount === undefined || amount === '') {
    return '0.00'
  }
  const num = Number(amount).toFixed(2)
  return num.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  contractSearchText.value = ''
  actualPaymentDateRange.value = []
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 新增按钮操作 */
function handleAdd() {
  router.push('/project/payment/add')
}

/** 编辑按钮操作 */
function handleUpdate(row) {
  router.push('/project/payment/edit/' + row.paymentId)
}

/** 详情按钮操作 */
function handleDetail(row) {
  router.push('/project/payment/detail/' + row.paymentId)
}

/** 附件按钮操作 */
function handleAttachment(row) {
  router.push('/project/payment/attachment/' + row.paymentId)
}

/** 删除按钮操作 */
function handleDelete(row) {
  const paymentId = row.paymentId
  const paymentName = row.paymentMethodName
  const paymentAmount = row.paymentAmount

  // 检查是否有附件
  checkAttachments(paymentId).then(response => {
    const hasAttachments = response.data > 0
    let message = `是否确认删除里程碑【${paymentName}】？\n付款金额：${formatMoney(paymentAmount)} 元`

    if (hasAttachments) {
      message += '\n\n注意：该里程碑下已经上传附件，删除后附件数据将一并删除且无法恢复！'
    }

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
  proxy.download('project/payment/export', {
    ...queryParams.value
  }, `payment_${new Date().getTime()}.xlsx`)
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
