<template>
  <div class="app-container revenue-company-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="120px">
      <el-form-item label="项目名称" prop="projectName">
        <el-autocomplete
          v-model="queryParams.projectName"
          :fetch-suggestions="queryProjectNames"
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
        <dict-select
          v-model="queryParams.revenueConfirmYear"
          dict-type="sys_ndgl"
          placeholder="请选择收入确认年度"
          clearable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="项目分类" prop="projectCategory">
        <dict-select
          v-model="queryParams.projectCategory"
          dict-type="sys_xmfl"
          placeholder="请选择项目分类"
          clearable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="一级区域" prop="region">
        <dict-select
          v-model="queryParams.region"
          dict-type="sys_yjqy"
          placeholder="请选择一级区域"
          clearable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="二级区域" prop="regionId">
        <secondary-region-select
          v-model="queryParams.regionId"
          :region-dict-value="queryParams.region"
          placeholder="请选择二级区域"
          clearable
          filterable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="项目经理" prop="projectManagerId">
        <user-select
          ref="projectManagerSelectRef"
          v-model="queryParams.projectManagerId"
          post-code="pm"
          placeholder="请选择项目经理"
          clearable
          filterable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="市场经理" prop="marketManagerId">
        <user-select
          ref="marketManagerSelectRef"
          v-model="queryParams.marketManagerId"
          post-code="scjl"
          placeholder="请选择市场经理"
          clearable
          filterable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="立项年度" prop="establishedYear">
        <dict-select
          v-model="queryParams.establishedYear"
          dict-type="sys_ndgl"
          placeholder="请选择立项年度"
          clearable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="项目阶段" prop="projectStage">
        <dict-select
          v-model="queryParams.projectStage"
          dict-type="sys_xmjd"
          placeholder="请选择项目阶段"
          clearable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="审核状态" prop="approvalStatus">
        <dict-select
          v-model="queryParams.approvalStatus"
          dict-type="sys_spzt"
          placeholder="请选择审核状态"
          clearable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="验收状态" prop="acceptanceStatus">
        <dict-select
          v-model="queryParams.acceptanceStatus"
          dict-type="sys_yszt"
          placeholder="请选择验收状态"
          clearable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="收入确认状态" prop="revenueConfirmStatus">
        <dict-select
          v-model="queryParams.revenueConfirmStatus"
          dict-type="sys_srqrzt"
          placeholder="请选择收入确认状态"
          clearable
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 隐藏的 UserSelect 组件用于加载所有用户（显示参与人员需要） -->
    <user-select
      ref="allUsersSelectRef"
      v-model="hiddenAllUsersValue"
      style="display: none"
    />

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

    <el-table v-loading="loading" :data="revenueList" :height="tableHeight">
      <el-table-column label="序号" width="55" align="center">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">合计</span>
          <span v-else>{{ scope.$index }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目名称" align="center" prop="projectName" min-width="150" show-overflow-tooltip />
      <el-table-column label="项目部门" align="center" prop="projectDept" min-width="120" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ getDeptName(scope.row.projectDept) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目经理" align="center" prop="projectManagerId" min-width="100">
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ getUserName(scope.row.projectManagerId, projectManagerSelectRef?.userOptions) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目分类" align="center" prop="projectCategory" min-width="120">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_xmfl" :value="scope.row.projectCategory"/>
        </template>
      </el-table-column>
      <el-table-column label="二级区域" align="center" prop="regionName" min-width="120" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ scope.row.regionName || '-' }}</span>
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
      <el-table-column label="收入确认年度" align="center" prop="revenueConfirmYear" min-width="120" />
      <el-table-column label="合同状态" align="center" prop="contractStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_htzt" :value="scope.row.contractStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="收入确认状态" align="center" prop="revenueConfirmStatus" min-width="120">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_srqrzt" :value="scope.row.revenueConfirmStatus"/>
        </template>
      </el-table-column>
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
      <el-table-column label="参与人员" align="center" prop="participants" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ getParticipantsNames(scope.row.participants) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="启动日期" align="center" prop="startDate" width="100" />
      <el-table-column label="结束日期" align="center" prop="endDate" width="100" />
      <el-table-column label="验收日期" align="center" prop="acceptanceDate" width="100" />
      <el-table-column label="审核状态" align="center" prop="approvalStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_spzt" :value="scope.row.approvalStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="项目阶段" align="center" prop="projectStage" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_xmjd" :value="scope.row.projectStage"/>
        </template>
      </el-table-column>
      <el-table-column label="验收状态" align="center" prop="acceptanceStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_yszt" :value="scope.row.acceptanceStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="更新人" align="center" prop="updateBy" min-width="100" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="160" />
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
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { listRevenue, exportRevenue } from "@/api/revenue/company"
import { getDeptTree } from "@/api/project/project"
import { handleTree } from '@/utils/ruoyi'
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
const tableHeight = ref(600)

const deptTree = ref([])
const deptFlatList = ref([])  // 扁平部门列表，用于快速查找

// 使用 UserSelect 组件的 ref 来获取用户列表
const projectManagerSelectRef = ref(null)
const marketManagerSelectRef = ref(null)
const allUsersSelectRef = ref(null)
const hiddenAllUsersValue = ref(null)

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
function handleRegionChange(regionCode) {
  queryParams.value.regionId = null
  provinceList.value = []
  if (regionCode) {
    getSecondaryRegions(regionCode).then(response => {
      provinceList.value = response.data || []
    })
  }
}

/** 加载部门树 */
function loadDeptTree() {
  getDeptTree().then(response => {
    // 保存所有部门的扁平列表用于查找（包含一、二级部门，用于显示路径）
    deptFlatList.value = response.data

    // 过滤出三级及以下的部门用于下拉选择
    // 判断规则：ancestors中逗号数量+1 >= 3（ancestors格式："0,100,101"）
    const level3AndBelowDepts = response.data.filter(dept => {
      if (!dept.ancestors) return false
      const level = dept.ancestors.split(',').length
      return level >= 3
    })

    // 转换字段名：deptId -> value, deptName -> label
    const deptData = level3AndBelowDepts.map(dept => ({
      ...dept,
      value: dept.deptId,
      label: dept.deptName
    }))

    // 将平铺数据转换为树形结构（用于下拉选择）
    deptTree.value = handleTree(deptData, 'deptId', 'parentId', 'children')
  })
}

/** 根据部门ID获取部门名称（显示第三级及以下机构的完整路径） */
function getDeptName(deptId) {
  if (!deptId) return '-'

  // 确保deptId是数字类型
  const numDeptId = typeof deptId === 'string' ? parseInt(deptId) : deptId

  const dept = deptFlatList.value.find(d => d.deptId === numDeptId)
  if (!dept) {
    // 如果找不到部门，返回横杠
    return '-'
  }

  // 解析 ancestors 获取所有祖先部门ID
  // ancestors 格式：0,100,101,102 (0是顶级，后面是各级部门)
  const ancestorIds = dept.ancestors ? dept.ancestors.split(',').filter(id => id && id !== '0') : []

  // 构建完整的部门路径（从第三级开始）
  const pathDepts = []

  // 如果祖先层级 >= 2（即第三级及以下），则从第三级开始显示
  if (ancestorIds.length >= 2) {
    // 从第3级开始（索引为2）
    for (let i = 2; i < ancestorIds.length; i++) {
      const ancestorDept = deptFlatList.value.find(d => d.deptId === parseInt(ancestorIds[i]))
      if (ancestorDept) {
        pathDepts.push(ancestorDept.deptName)
      }
    }
  }

  // 添加当前部门
  pathDepts.push(dept.deptName)

  return pathDepts.length > 0 ? pathDepts.join('-') : dept.deptName
}

/** 根据用户ID获取用户名称 */
function getUserName(userId, userList) {
  if (!userId) return '-'
  // 如果 userList 是响应式对象，取其 value
  const list = userList?.value || userList || []
  const user = list.find(u => u.userId === userId)
  return user ? user.nickName : '-'
}

/** 根据参与人员ID列表获取名称（逗号分隔） */
function getParticipantsNames(participants) {
  if (!participants) return '-'

  // participants 格式可能是字符串"1,2,3"或数组[1,2,3]
  const participantIds = typeof participants === 'string'
    ? participants.split(',').map(id => parseInt(id.trim()))
    : participants

  if (!participantIds || participantIds.length === 0) return '-'

  // 从 allUsersSelectRef 获取所有用户列表
  const allUsers = allUsersSelectRef.value?.userOptions || []
  if (allUsers.length === 0) return '-'

  // 根据ID查找用户名称
  const names = participantIds
    .map(id => {
      const user = allUsers.find(u => u.userId === id)
      return user ? user.nickName : null
    })
    .filter(name => name !== null)

  return names.length > 0 ? names.join(', ') : '-'
}

/** 项目名称自动完成 */
function queryProjectNames(queryString, callback) {
  if (!queryString) {
    callback([])
    return
  }

  // 从当前项目列表中过滤匹配的项目名称（排除合计行）
  const results = revenueList.value
    .filter(project => !project.isSummaryRow && project.projectName && project.projectName.toLowerCase().includes(queryString.toLowerCase()))
    .map(project => ({ value: project.projectName }))

  callback(results)
}

/** 初始化下拉选项 */
function initOptions() {
  // 加载部门树
  loadDeptTree()
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

/** 计算表格高度 */
function calcTableHeight() {
  nextTick(() => {
    const windowHeight = window.innerHeight
    const searchHeight = showSearch.value ? 200 : 0
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

// 初始化
initOptions()
getList()
</script>

<style scoped lang="scss">
.revenue-company-container {
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
