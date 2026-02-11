<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="120px">
      <el-form-item label="项目名称" prop="projectName">
        <el-input
          v-model="queryParams.projectName"
          placeholder="请输入项目名称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="项目部门" prop="projectDept">
        <el-tree-select
          v-model="queryParams.projectDept"
          :data="deptTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          placeholder="请选择项目部门"
          check-strictly
          clearable
          filterable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="收入确认年度" prop="revenueConfirmYear">
        <el-select v-model="queryParams.revenueConfirmYear" placeholder="请选择收入确认年度" clearable style="width: 240px">
          <el-option
            v-for="dict in sys_ndgl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目分类" prop="projectCategory">
        <el-select v-model="queryParams.projectCategory" placeholder="请选择项目分类" clearable style="width: 240px">
          <el-option
            v-for="dict in sys_xmfl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="一级区域" prop="region">
        <el-select v-model="queryParams.region" placeholder="请选择一级区域" clearable @change="handleRegionChange" style="width: 240px">
          <el-option
            v-for="dict in sys_yjqy"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="二级区域" prop="regionId">
        <el-select v-model="queryParams.regionId" placeholder="请选择二级区域" clearable filterable style="width: 240px">
          <el-option
            v-for="item in provinceList"
            :key="item.regionId"
            :label="item.regionName"
            :value="item.regionId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目经理" prop="projectManagerId">
        <el-select v-model="queryParams.projectManagerId" placeholder="请选择项目经理" clearable filterable style="width: 240px">
          <el-option
            v-for="user in projectManagerList"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="市场经理" prop="marketManagerId">
        <el-select v-model="queryParams.marketManagerId" placeholder="请选择市场经理" clearable filterable style="width: 240px">
          <el-option
            v-for="user in marketManagerList"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="立项年度" prop="establishedYear">
        <el-select v-model="queryParams.establishedYear" placeholder="请选择立项年度" clearable style="width: 240px">
          <el-option
            v-for="dict in sys_ndgl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目阶段" prop="projectStage">
        <el-select v-model="queryParams.projectStage" placeholder="请选择项目阶段" clearable style="width: 240px">
          <el-option
            v-for="dict in sys_xmjd"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态" prop="approvalStatus">
        <el-select v-model="queryParams.approvalStatus" placeholder="请选择审核状态" clearable style="width: 240px">
          <el-option
            v-for="dict in sys_spzt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="验收状态" prop="acceptanceStatus">
        <el-select v-model="queryParams.acceptanceStatus" placeholder="请选择验收状态" clearable style="width: 240px">
          <el-option
            v-for="dict in sys_yszt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="收入确认状态" prop="revenueConfirmStatus">
        <el-select v-model="queryParams.revenueConfirmStatus" placeholder="请选择收入确认状态" clearable style="width: 240px">
          <el-option
            v-for="dict in sys_srqrzt"
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
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['revenue:company:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="revenueList">
      <el-table-column label="序号" width="55" align="center">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">合计</span>
          <span v-else>{{ scope.$index }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目名称" align="center" prop="projectName" min-width="150" show-overflow-tooltip />
      <el-table-column label="项目编码" align="center" prop="projectCode" min-width="150" show-overflow-tooltip />
      <el-table-column label="项目部门" align="center" prop="projectDept" min-width="120" show-overflow-tooltip />
      <el-table-column label="项目经理" align="center" prop="projectManagerName" min-width="100" />
      <el-table-column label="市场经理" align="center" prop="marketManagerName" min-width="100" />
      <el-table-column label="项目分类" align="center" prop="projectCategory" width="120">
        <template #default="scope">
          <dict-tag :options="sys_xmfl" :value="scope.row.projectCategory"/>
        </template>
      </el-table-column>
      <el-table-column label="一级区域" align="center" prop="region" width="100">
        <template #default="scope">
          <dict-tag :options="sys_yjqy" :value="scope.row.region"/>
        </template>
      </el-table-column>
      <el-table-column label="二级区域" align="center" prop="regionName" width="100" />
      <el-table-column label="项目阶段" align="center" prop="projectStage" width="100">
        <template #default="scope">
          <dict-tag :options="sys_xmjd" :value="scope.row.projectStage"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收入确认状态" align="center" prop="revenueConfirmStatus" min-width="120">
        <template #default="scope">
          <dict-tag :options="sys_srqrzt" :value="scope.row.revenueConfirmStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="收入确认年度" align="center" prop="revenueConfirmYear" min-width="120" />
      <el-table-column label="确认金额(含税)" align="center" prop="confirmAmount" min-width="120">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.confirmAmount }}</span>
          <span v-else>{{ scope.row.confirmAmount }}</span>
        </template>
      </el-table-column>

      <el-table-column label="税后金额" align="center" prop="afterTaxAmount" min-width="120">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.afterTaxAmount }}</span>
          <span v-else>{{ scope.row.afterTaxAmount }}</span>
        </template>
      </el-table-column>

      <el-table-column label="项目预算" align="center" prop="projectBudget" min-width="120">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.projectBudget }}</span>
          <span v-else>{{ scope.row.projectBudget }}</span>
        </template>
      </el-table-column>

      <el-table-column label="预估工作量" align="center" prop="estimatedWorkload" min-width="100">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.estimatedWorkload }}</span>
          <span v-else>{{ scope.row.estimatedWorkload }}</span>
        </template>
      </el-table-column>

      <el-table-column label="实际人天" align="center" prop="actualWorkload" min-width="100">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.actualWorkload }}</span>
          <span v-else>{{ scope.row.actualWorkload }}</span>
        </template>
      </el-table-column>

      <el-table-column label="合同金额" align="center" prop="contractAmount" min-width="120">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.contractAmount }}</span>
          <span v-else>{{ scope.row.contractAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150" fixed="right">
        <template #default="scope">
          <template v-if="!scope.row.isSummaryRow">
            <el-button
              v-if="scope.row.revenueConfirmStatus === '0' || !scope.row.revenueConfirmStatus"
              link
              type="success"
              icon="Money"
              @click="handleRevenue(scope.row)"
              v-hasPermi="['revenue:company:edit']"
            >收入确认</el-button>
            <el-button
              v-else-if="scope.row.revenueConfirmStatus === '1'"
              link
              type="primary"
              icon="View"
              @click="handleRevenueView(scope.row)"
              v-hasPermi="['revenue:company:view']"
            >收入查看</el-button>
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

<script setup name="RevenueCompany">
import { listRevenue, exportRevenue } from "@/api/revenue/company"
import { listSecondaryRegion } from "@/api/project/secondaryRegion"
import { listUserByPost } from "@/api/system/user"
import { listDept } from "@/api/system/dept"
import { useRouter } from 'vue-router'

const router = useRouter()

const { proxy } = getCurrentInstance()

const { sys_xmfl, sys_yjqy, sys_xmjd, sys_spzt, sys_yszt, sys_srqrzt, sys_ndgl, sys_htzt } = proxy.useDict(
  'sys_xmfl', 'sys_yjqy', 'sys_xmjd', 'sys_spzt', 'sys_yszt', 'sys_srqrzt', 'sys_ndgl', 'sys_htzt'
)

const revenueList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const deptTree = ref([])
const provinceList = ref([])
const projectManagerList = ref([])
const marketManagerList = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    projectDept: null,
    revenueConfirmYear: null,
    projectCategory: null,
    region: null,
    regionId: null,
    projectManagerId: null,
    marketManagerId: null,
    establishedYear: null,
    projectStage: null,
    approvalStatus: null,
    acceptanceStatus: null,
    revenueConfirmStatus: null
  }
})

const { queryParams } = toRefs(data)

/** 查询列表 */
function getList() {
  loading.value = true
  listRevenue(queryParams.value).then(response => {
    const list = response.rows
    const summary = calculateSummary(list)
    revenueList.value = [summary, ...list] // 合计行在第一行
    total.value = response.total
    loading.value = false
  })
}

/** 计算合计行 */
function calculateSummary(list) {
  const summary = {
    isSummaryRow: true,
    projectBudget: 0,
    estimatedWorkload: 0,
    actualWorkload: 0,
    contractAmount: 0,
    confirmAmount: 0,
    afterTaxAmount: 0
  }

  list.forEach(item => {
    summary.projectBudget += (item.projectBudget || 0)
    summary.estimatedWorkload += (item.estimatedWorkload || 0)
    summary.actualWorkload += (item.actualWorkload || 0)
    summary.contractAmount += (item.contractAmount || 0)
    summary.confirmAmount += (item.confirmAmount || 0)
    summary.afterTaxAmount += (item.afterTaxAmount || 0)
  })

  // 格式化为两位小数
  Object.keys(summary).forEach(key => {
    if (key !== 'isSummaryRow' && summary[key]) {
      summary[key] = summary[key].toFixed(2)
    }
  })

  return summary
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

/** 导出按钮操作 */
function handleExport() {
  proxy.download('revenue/export', {
    ...queryParams.value
  }, `收入确认_${new Date().getTime()}.xlsx`)
}

/** 一级区域变化 */
function handleRegionChange(value) {
  queryParams.value.regionId = null
  provinceList.value = []
  if (value) {
    listSecondaryRegion({ regionCode: value }).then(response => {
      provinceList.value = response.rows || []
    })
  }
}

/** 初始化下拉选项 */
function initOptions() {
  // 加载部门树
  listDept().then(response => {
    deptTree.value = proxy.handleTree(response.data, "deptId")
  })

  // 加载项目经理（岗位代码：pm）
  listUserByPost('pm').then(response => {
    projectManagerList.value = response.data || []
  })

  // 加载市场经理（岗位代码：scjl）
  listUserByPost('scjl').then(response => {
    marketManagerList.value = response.data || []
  })
}

/** 收入确认按钮 */
function handleRevenue(row) {
  router.push({
    path: '/revenue/company/detail/' + row.projectId,
    query: { mode: 'edit' }
  })
}

/** 收入查看按钮 */
function handleRevenueView(row) {
  router.push({
    path: '/revenue/company/detail/' + row.projectId,
    query: { mode: 'view' }
  })
}

// 初始化
initOptions()
getList()
</script>
