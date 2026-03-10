<template>
  <div class="app-container revenue-company-container">
    <el-form :model="queryParams" ref="queryRef" v-show="showSearch" label-width="120px">
      <el-row :gutter="10">
        <el-col :span="6">
          <el-form-item label="项目名称" prop="projectName">
            <el-autocomplete
              v-model="queryParams.projectName"
              :fetch-suggestions="queryProjectNames"
              clearable
              placeholder="输入关键字搜索，或直接选择下拉数据"
              style="width: 100%"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="项目部门" prop="projectDept">
            <el-tree-select
              v-model="queryParams.projectDept"
              :data="deptTree"
              :props="{ label: 'label', value: 'value', children: 'children' }"
              placeholder="请选择项目部门"
              check-strictly
              clearable
              filterable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="收入确认年度" prop="revenueConfirmYear">
            <dict-select
              v-model="queryParams.revenueConfirmYear"
              dict-type="sys_ndgl"
              placeholder="请选择收入确认年度"
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="项目分类" prop="projectCategory">
            <dict-select
              v-model="queryParams.projectCategory"
              dict-type="sys_xmfl"
              placeholder="请选择项目分类"
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="10">
        <el-col :span="6">
          <el-form-item label="一级区域" prop="region">
            <dict-select
              v-model="queryParams.region"
              dict-type="sys_yjqy"
              placeholder="请选择一级区域"
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="二级区域" prop="regionId">
            <secondary-region-select
              v-model="queryParams.regionId"
              :region-dict-value="queryParams.region"
              placeholder="请选择二级区域"
              clearable
              filterable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="项目经理" prop="projectManagerId">
            <user-select
              ref="projectManagerSelectRef"
              v-model="queryParams.projectManagerId"
              post-code="pm"
              placeholder="请选择项目经理"
              clearable
              filterable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="市场经理" prop="marketManagerId">
            <user-select
              ref="marketManagerSelectRef"
              v-model="queryParams.marketManagerId"
              post-code="scjl"
              placeholder="请选择市场经理"
              clearable
              filterable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="10">
        <el-col :span="6">
          <el-form-item label="立项年度" prop="establishedYear">
            <dict-select
              v-model="queryParams.establishedYear"
              dict-type="sys_ndgl"
              placeholder="请选择立项年度"
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="审核状态" prop="approvalStatus">
            <dict-select
              v-model="queryParams.approvalStatus"
              dict-type="sys_spzt"
              placeholder="请选择审核状态"
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="验收状态" prop="acceptanceStatus">
            <dict-select
              v-model="queryParams.acceptanceStatus"
              dict-type="sys_yszt"
              placeholder="请选择验收状态"
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="项目阶段" prop="projectStage">
            <dict-select
              v-model="queryParams.projectStage"
              dict-type="sys_xmjd"
              placeholder="请选择项目阶段"
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="10">
        <el-col :span="6">
          <el-form-item label="合同状态" prop="contractStatus">
            <dict-select
              v-model="queryParams.contractStatus"
              dict-type="sys_htzt"
              placeholder="请选择合同状态"
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="收入确认状态" prop="revenueConfirmStatus">
            <dict-select
              v-model="queryParams.revenueConfirmStatus"
              dict-type="sys_qrzt"
              placeholder="请选择收入确认状态"
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="10">
        <el-col :span="24">
          <el-form-item label-width="0">
            <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['revenue:company:export']">导出</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <!-- 隐藏的 UserSelect 组件用于加载所有用户（显示参与人员需要） -->
    <user-select
      ref="allUsersSelectRef"
      v-model="hiddenAllUsersValue"
      style="display: none"
    />

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="revenueList" :height="tableHeight" border :row-class-name="getRowClass">
      <el-table-column label="序号" width="55" align="center" fixed="left">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">合计</span>
          <span v-else>{{ scope.$index }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目名称" align="left" header-align="center" prop="projectName" min-width="220" fixed="left">
        <template #default="scope">
          <el-link
            v-if="!scope.row.isSummaryRow"
            type="primary"
            class="project-name-cell"
            @click="handleRevenueView(scope.row)"
          >{{ scope.row.projectName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[0].visible" label="项目阶段" align="center" prop="projectStage" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_xmjd" :value="scope.row.projectStage"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[1].visible" label="项目部门" align="center" prop="projectDept" min-width="120" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ getDeptName(scope.row.projectDept) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[2].visible" label="项目经理" align="center" prop="projectManagerId" min-width="100">
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ getUserName(scope.row.projectManagerId, projectManagerSelectRef?.userOptions) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[3].visible" label="项目分类" align="center" prop="projectCategory" min-width="120">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_xmfl" :value="scope.row.projectCategory"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[4].visible" label="二级区域" align="center" prop="regionName" min-width="120" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ scope.row.regionName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[5].visible" label="项目预算(元)" align="right" prop="projectBudget" min-width="120">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow">{{ formatAmount(scope.row.projectBudget) }}</span>
          <span v-else>{{ formatAmount(scope.row.projectBudget) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[6].visible" label="预估工作量（人天）" align="center" prop="estimatedWorkload" min-width="130">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow">{{ scope.row.estimatedWorkload }}</span>
          <span v-else>{{ scope.row.estimatedWorkload }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[7].visible" label="实际人天" align="center" prop="actualWorkload" min-width="100">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow">{{ scope.row.actualWorkload }}</span>
          <span v-else>{{ scope.row.actualWorkload != null ? parseFloat(scope.row.actualWorkload).toFixed(3) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[8].visible" label="合同名称" align="left" header-align="center" prop="contractName" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow" style="white-space: pre-line;">{{ scope.row.contractName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[9].visible" label="合同金额(元)" align="right" prop="contractAmount" min-width="120">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow">{{ formatAmount(scope.row.contractAmount) }}</span>
          <span v-else>{{ formatAmount(scope.row.contractAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[10].visible" label="合同状态" align="center" prop="contractStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_htzt" :value="scope.row.contractStatus"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[11].visible" label="收入确认年度" align="center" prop="revenueConfirmYear" min-width="120">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_ndgl" :value="scope.row.revenueConfirmYear"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[12].visible" label="确认日期" align="center" prop="revenueConfirmDate" width="110">
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ scope.row.revenueConfirmDate || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[13].visible" label="收入确认状态" align="center" prop="revenueConfirmStatus" min-width="120">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_qrzt" :value="scope.row.revenueConfirmStatus"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[14].visible" label="验收状态" align="center" prop="acceptanceStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_yszt" :value="scope.row.acceptanceStatus"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[15].visible" label="确认金额（元）" align="right" prop="confirmAmount" min-width="130">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow">{{ formatAmount(scope.row.confirmAmount) }}</span>
          <span v-else>{{ formatAmount(scope.row.confirmAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[16].visible" label="参与人员" align="center" prop="participants" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummaryRow">{{ getParticipantsNames(scope.row.participants) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[17].visible" label="启动日期" align="center" prop="startDate" width="100" />
      <el-table-column v-if="columns[18].visible" label="结束日期" align="center" prop="endDate" width="100" />
      <el-table-column v-if="columns[19].visible" label="验收日期" align="center" prop="acceptanceDate" width="100" />
      <el-table-column v-if="columns[20].visible" label="审核状态" align="center" prop="approvalStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_spzt" :value="scope.row.approvalStatus"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[21].visible" label="项目状态" align="center" prop="projectStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_xmzt" :value="scope.row.projectStatus"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[22].visible" label="更新人" align="center" prop="updateByName" min-width="100" />
      <el-table-column v-if="columns[23].visible" label="更新时间" align="center" prop="updateTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200" fixed="right">
        <template #default="scope">
          <template v-if="!scope.row.isSummaryRow">
            <el-button
              link
              type="primary"
              icon="View"
              @click="handleRevenueView(scope.row)"
              v-hasPermi="['revenue:company:view']"
            >收入查看</el-button>
            <el-button
              link
              type="success"
              icon="Money"
              @click="handleRevenue(scope.row)"
              v-hasPermi="['revenue:company:edit']"
            >收入确认</el-button>
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
import { ref, reactive, toRefs, getCurrentInstance, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { listRevenue, getRevenueSummary, exportRevenue } from "@/api/revenue/company"
import { getDeptTree, searchProjects } from "@/api/project/project"
import { handleTree } from '@/utils/ruoyi'
import { useRouter } from 'vue-router'

const router = useRouter()

const { proxy } = getCurrentInstance()

const { sys_xmfl, sys_yjqy, sys_xmjd, sys_spzt, sys_yszt, sys_qrzt, sys_ndgl, sys_htzt, sys_xmzt } = proxy.useDict(
  'sys_xmfl', 'sys_yjqy', 'sys_xmjd', 'sys_spzt', 'sys_yszt', 'sys_qrzt', 'sys_ndgl', 'sys_htzt', 'sys_xmzt'
)

const revenueList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const tableHeight = ref(600)

const deptTree = ref([])
const deptFlatList = ref([])

// 列显隐配置
const columns = ref([
  { key: 0,  label: '项目阶段',        visible: true  },
  { key: 1,  label: '项目部门',        visible: true  },
  { key: 2,  label: '项目经理',        visible: true  },
  { key: 3,  label: '项目分类',        visible: true  },
  { key: 4,  label: '二级区域',        visible: true  },
  { key: 5,  label: '项目预算(元)',     visible: true  },
  { key: 6,  label: '预估工作量(人天)', visible: true  },
  { key: 7,  label: '实际人天',        visible: true  },
  { key: 8,  label: '合同名称',        visible: true  },
  { key: 9,  label: '合同金额(元)',     visible: true  },
  { key: 10, label: '合同状态',        visible: true  },
  { key: 11, label: '收入确认年度',    visible: true  },
  { key: 12, label: '确认日期',        visible: true  },
  { key: 13, label: '收入确认状态',    visible: true  },
  { key: 14, label: '验收状态',        visible: true  },
  { key: 15, label: '确认金额(元)',     visible: true  },
  { key: 16, label: '参与人员',        visible: true  },
  { key: 17, label: '启动日期',        visible: true  },
  { key: 18, label: '结束日期',        visible: true  },
  { key: 19, label: '验收日期',        visible: true  },
  { key: 20, label: '审核状态',        visible: true  },
  { key: 21, label: '项目状态',        visible: true  },
  { key: 22, label: '更新人',          visible: true  },
  { key: 23, label: '更新时间',        visible: true  },
])

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
    contractStatus: null,
    revenueConfirmStatus: null
  }
})

const { queryParams } = toRefs(data)

/** 查询列表 */
function getList() {
  loading.value = true
  Promise.all([
    listRevenue(queryParams.value),
    getRevenueSummary(queryParams.value)
  ]).then(([listRes, summaryRes]) => {
    const summary = {
      isSummaryRow: true,
      projectBudget: Number(summaryRes.data?.projectBudget || 0).toFixed(2),
      estimatedWorkload: Number(summaryRes.data?.estimatedWorkload || 0).toFixed(2),
      actualWorkload: Number(summaryRes.data?.actualWorkload || 0).toFixed(3),
      contractAmount: Number(summaryRes.data?.contractAmount || 0).toFixed(2),
      confirmAmount: Number(summaryRes.data?.confirmAmount || 0).toFixed(2)
    }
    if (listRes.rows && listRes.rows.length > 0) {
      revenueList.value = [summary, ...listRes.rows]
    } else {
      revenueList.value = []
    }
    total.value = listRes.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 格式化金额为千分位，保留2位小数；无值时显示 0.00 */
function formatAmount(amount) {
  if (amount === null || amount === undefined || amount === '') return '0.00'
  const num = parseFloat(amount)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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
  proxy.download('project/project/revenue/export', {
    ...queryParams.value
  }, `收入确认_${new Date().getTime()}.xlsx`)
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

/** 加载项目名称下拉选项（支持模糊查询） */
function queryProjectNames(queryString, cb) {
  searchProjects(queryString || '').then(response => {
    cb((response.data || []).map(p => ({ value: p.projectName })))
  }).catch(() => cb([]))
}

/** 初始化下拉选项 */
function initOptions() {
  // 加载部门树
  loadDeptTree()
}

/** 合计行样式 */
function getRowClass({ row }) {
  return row.isSummaryRow ? 'summary-row' : ''
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
    const searchHeight = showSearch.value ? 255 : 0
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

  :deep(.el-form .el-form-item) {
    margin-bottom: 12px;
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

  :deep(.el-table .summary-row td) {
    color: #409eff;
    font-weight: bold;
  }

  :deep(.el-pagination) {
    margin-top: 15px;
    text-align: right;
  }

  .project-name-cell {
    word-break: break-all;
    white-space: normal;
    line-height: 1.5;
    text-align: left;
  }
}
</style>
