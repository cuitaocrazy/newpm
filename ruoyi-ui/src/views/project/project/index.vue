<template>
  <div class="app-container">
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
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['project:project:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['project:project:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="projectList" @selection-change="handleSelectionChange"
              :row-class-name="tableRowClassName">
      <el-table-column type="selection" width="55" align="center" :selectable="checkSelectable" />
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
          <span v-if="!scope.row.isSummaryRow">{{ getUserName(scope.row.projectManagerId, projectManagerList) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目分类" align="center" prop="projectCategory" min-width="120">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_xmfl" :value="scope.row.projectCategory" />
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
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_htzt" :value="scope.row.contractStatus" />
        </template>
      </el-table-column>
      <el-table-column label="收入确认状态" align="center" prop="revenueConfirmStatus" min-width="120">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_srqrzt" :value="scope.row.revenueConfirmStatus" />
        </template>
      </el-table-column>
      <el-table-column label="确认金额" align="center" prop="confirmAmount" min-width="120">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.confirmAmount }}</span>
          <span v-else>{{ scope.row.confirmAmount }}</span>
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
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_spzt" :value="scope.row.approvalStatus" />
        </template>
      </el-table-column>
      <el-table-column label="项目阶段" align="center" prop="projectStage" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_xmjd" :value="scope.row.projectStage" />
        </template>
      </el-table-column>
      <el-table-column label="验收状态" align="center" prop="acceptanceStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummaryRow" :options="sys_yszt" :value="scope.row.acceptanceStatus" />
        </template>
      </el-table-column>
      <el-table-column label="更新人" align="center" prop="updateBy" min-width="100" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="300">
        <template #default="scope">
          <template v-if="!scope.row.isSummaryRow">
            <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['project:project:query']">详情</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:project:edit']">编辑</el-button>

            <!-- 收入确认/查看按钮 -->
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

            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:project:remove']">删除</el-button>
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

    <!-- 审核对话框 -->
    <el-dialog title="项目审核" v-model="approvalDialogVisible" width="600px" append-to-body>
      <el-form :model="approvalForm" :rules="approvalRules" ref="approvalFormRef" label-width="100px">
        <el-form-item label="项目名称">
          <el-input v-model="currentProject.projectName" disabled />
        </el-form-item>
        <el-form-item label="项目编号">
          <el-input v-model="currentProject.projectCode" disabled />
        </el-form-item>
        <el-form-item label="审核结果" prop="approvalStatus">
          <el-radio-group v-model="approvalForm.approvalStatus">
            <el-radio label="1">通过</el-radio>
            <el-radio label="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" prop="approvalReason">
          <el-input
            v-model="approvalForm.approvalReason"
            type="textarea"
            :rows="4"
            :placeholder="approvalForm.approvalStatus === '2' ? '请填写拒绝原因（必填）' : '审核意见（可选）'"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="approvalDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitApproval">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审核历史对话框 -->
    <el-dialog title="审核历史" v-model="historyDialogVisible" width="800px" append-to-body>
      <el-timeline v-if="approvalHistory.length > 0">
        <el-timeline-item
          v-for="item in approvalHistory"
          :key="item.approvalId"
          :timestamp="item.createTime"
          placement="top"
        >
          <el-card>
            <template #header>
              <span>审核结果：</span>
              <dict-tag :options="sys_spzt" :value="item.approvalStatus" />
            </template>
            <p>审核时间：{{ item.approvalTime }}</p>
            <p>审核人ID：{{ item.approverId }}</p>
            <p>审核意见：{{ item.approvalReason || '无' }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审核记录" />
    </el-dialog>
  </div>
</template>

<script setup name="Project">
import { listProject, delProject, getUsersByPost, getSecondaryRegions, getDeptTree } from "@/api/project/project"
import { approveProject, getApprovalHistory } from "@/api/project/approval"
import { listUser } from "@/api/system/user"
import { useRouter } from 'vue-router'
import { handleTree } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()
const router = useRouter()
const { industry, sys_yjqy, sys_ndgl, sys_xmfl, sys_xmjd, sys_xmzt, sys_yszt, sys_spzt, sys_srqrzt, sys_htzt } = proxy.useDict('industry', 'sys_yjqy', 'sys_ndgl', 'sys_xmfl', 'sys_xmjd', 'sys_xmzt', 'sys_yszt', 'sys_spzt', 'sys_srqrzt', 'sys_htzt')

const projectList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)

// 审核对话框
const approvalDialogVisible = ref(false)
const currentProject = ref({})
const approvalForm = ref({
  projectId: null,
  approvalStatus: '1',
  approvalReason: ''
})
const approvalRules = {
  approvalStatus: [
    { required: true, message: '请选择审核结果', trigger: 'change' }
  ]
}

// 审核历史对话框
const historyDialogVisible = ref(false)
const approvalHistory = ref([])

// 辅助数据源
const deptTree = ref([])
const deptFlatList = ref([])  // 扁平部门列表，用于快速查找
const provinceList = ref([])
const projectManagerList = ref([])
const marketManagerList = ref([])
const allUsersList = ref([])  // 所有用户列表，用于显示参与人员

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
    acceptanceStatus: null
  }
})

const { queryParams } = toRefs(data)

/** 查询项目管理列表 */
function getList() {
  loading.value = true
  listProject(queryParams.value).then(response => {
    const rows = response.rows || []

    // 计算合计行
    if (rows.length > 0) {
      const summaryRow = {
        isSummaryRow: true,
        projectBudget: rows.reduce((sum, item) => sum + (Number(item.projectBudget) || 0), 0).toFixed(2),
        estimatedWorkload: rows.reduce((sum, item) => sum + (Number(item.estimatedWorkload) || 0), 0).toFixed(2),
        actualWorkload: rows.reduce((sum, item) => sum + (Number(item.actualWorkload) || 0), 0).toFixed(2),
        contractAmount: rows.reduce((sum, item) => sum + (Number(item.contractAmount) || 0), 0).toFixed(2),
        confirmAmount: rows.reduce((sum, item) => sum + (Number(item.confirmAmount) || 0), 0).toFixed(2)
      }
      // 将合计行插入到列表第一行
      projectList.value = [summaryRow, ...rows]
    } else {
      projectList.value = rows
    }

    total.value = response.total
    loading.value = false
  })
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
  const user = userList.find(u => u.userId === userId)
  return user ? user.nickName : '-'
}

/** 根据参与人员ID列表获取人员名称列表 */
function getParticipantsNames(participants) {
  if (!participants) return '-'
  const userIds = participants.split(',').map(id => parseInt(id.trim()))
  const names = userIds.map(userId => {
    const user = allUsersList.value.find(u => u.userId === userId)
    return user ? user.nickName : userId
  }).filter(name => name)
  return names.length > 0 ? names.join(', ') : '-'
}

/** 加载项目经理列表 */
function loadProjectManagers() {
  getUsersByPost('pm').then(response => {
    projectManagerList.value = response.data
  })
}

/** 加载市场经理列表 */
function loadMarketManagers() {
  getUsersByPost('scjl').then(response => {
    marketManagerList.value = response.data
  })
}

/** 加载所有用户（用于显示参与人员） */
function loadAllUsers() {
  listUser({}).then(response => {
    allUsersList.value = response.rows || []
  })
}

/** 一级区域变化时加载二级区域 */
function handleRegionChange(regionCode) {
  // 清空二级区域选择
  queryParams.value.regionId = null
  provinceList.value = []

  if (regionCode) {
    getSecondaryRegions(regionCode).then(response => {
      provinceList.value = response.data || []
    })
  }
}

/** 项目名称自动完成 */
function queryProjectNames(queryString, callback) {
  if (!queryString) {
    callback([])
    return
  }

  // 从当前项目列表中过滤匹配的项目名称
  const results = projectList.value
    .filter(project => project.projectName && project.projectName.toLowerCase().includes(queryString.toLowerCase()))
    .map(project => ({ value: project.projectName }))

  callback(results)
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
  // 过滤掉合计行
  const validSelection = selection.filter(item => !item.isSummaryRow)
  ids.value = validSelection.map(item => item.projectId)
  single.value = validSelection.length != 1
  multiple.value = !validSelection.length
}

/** 判断行是否可选 */
function checkSelectable(row) {
  return !row.isSummaryRow
}

/** 表格行类名 */
function tableRowClassName({ row }) {
  if (row.isSummaryRow) {
    return 'summary-row'
  }
  return ''
}

/** 详情按钮操作 */
function handleDetail(row) {
  const projectId = row.projectId
  router.push(`/project/list/detail/${projectId}`)
}

/** 编辑按钮操作 */
function handleUpdate(row) {
  const projectId = row.projectId || ids.value[0]
  router.push(`/project/list/edit/${projectId}`)
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

/** 删除按钮操作 */
function handleDelete(row) {
  const projectIds = row.projectId || ids.value
  proxy.$modal.confirm('是否确认删除项目管理编号为"' + projectIds + '"的数据项？').then(function() {
    return delProject(projectIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('project/project/export', {
    ...queryParams.value
  }, `project_${new Date().getTime()}.xlsx`)
}

/** 审核按钮操作 */
function handleApprove(row) {
  currentProject.value = { ...row }
  approvalForm.value = {
    projectId: row.projectId,
    approvalStatus: '1',
    approvalReason: ''
  }
  approvalDialogVisible.value = true
}

/** 提交审核 */
function submitApproval() {
  proxy.$refs.approvalFormRef.validate(valid => {
    if (valid) {
      // 拒绝时必须填写原因
      if (approvalForm.value.approvalStatus === '2' && !approvalForm.value.approvalReason) {
        proxy.$modal.msgWarning('拒绝时必须填写拒绝原因')
        return
      }

      approveProject(approvalForm.value).then(response => {
        proxy.$modal.msgSuccess('审核成功')
        approvalDialogVisible.value = false
        getList()
      })
    }
  })
}

/** 查看审核历史 */
function handleHistory(row) {
  getApprovalHistory(row.projectId).then(response => {
    approvalHistory.value = response.data
    historyDialogVisible.value = true
  })
}

getList()
loadDeptTree()
loadProjectManagers()
loadMarketManagers()
loadAllUsers()
</script>

<style scoped>
::v-deep .summary-row {
  background-color: #f5f7fa;
  font-weight: bold;
}

::v-deep .summary-row:hover > td {
  background-color: #f5f7fa !important;
}
</style>
