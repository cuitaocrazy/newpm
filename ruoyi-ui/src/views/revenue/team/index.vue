<template>
  <div class="app-container">
    <!-- 查询表单 - 复用公司收入确认的查询条件 -->
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

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['revenue:team:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['revenue:team:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 列表表格 - 复用公司收入确认的列表字段 -->
    <el-table v-loading="loading" :data="teamRevenueList">
      <el-table-column label="序号" width="55" align="center" type="index" />
      <el-table-column label="项目名称" align="center" prop="projectName" min-width="150" show-overflow-tooltip />
      <el-table-column label="项目部门" align="center" prop="projectDept" min-width="120" show-overflow-tooltip />
      <el-table-column label="项目经理" align="center" prop="projectManagerId" min-width="100" />
      <el-table-column label="市场经理" align="center" prop="marketManagerId" min-width="100" />
      <el-table-column label="项目分类" align="center" prop="projectCategory" width="120">
        <template #default="scope">
          <dict-tag :options="sys_xmfl" :value="scope.row.projectCategory"/>
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
      <el-table-column label="确认金额(含税)" align="center" prop="confirmAmount" min-width="120" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['revenue:team:edit']"
          >编辑</el-button>
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleDetail(scope.row)"
            v-hasPermi="['revenue:team:query']"
          >详情</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="title" v-model="open" width="900px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <!-- 主表信息 -->
        <el-divider content-position="left">项目信息</el-divider>
        <el-row>
          <el-col :span="12">
            <el-form-item label="项目" prop="projectId">
              <project-select
                v-model="form.projectId"
                @change="handleProjectChange"
                placeholder="请选择项目"
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预算金额(元)">
              <el-input v-model="form.projectBudget" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="合同金额(元)">
              <el-input v-model="form.contractAmount" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公司确认金额(元)">
              <el-input v-model="form.confirmAmount" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="确认年度">
              <el-input v-model="form.revenueConfirmYear" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 明细列表 -->
        <el-divider content-position="left">团队确认明细</el-divider>
        <el-button type="primary" size="small" icon="Plus" @click="handleAddDetail" style="margin-bottom: 10px">添加明细</el-button>
        <el-table :data="form.detailList" border style="width: 100%">
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="部门" prop="deptId" width="200">
            <template #default="scope">
              <el-tree-select
                v-model="scope.row.deptId"
                :data="deptTree"
                :props="{ label: 'label', value: 'value', children: 'children' }"
                placeholder="请选择部门"
                check-strictly
                clearable
                filterable
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="确认金额(元)" prop="confirmAmount" width="150">
            <template #default="scope">
              <el-input-number
                v-model="scope.row.confirmAmount"
                :precision="2"
                :min="0"
                controls-position="right"
                @change="calculateTotalAmount"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="确认人" prop="confirmUserName" width="100">
            <template #default="scope">
              <span>{{ scope.row.confirmUserName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="150">
            <template #default="scope">
              <el-input v-model="scope.row.remark" placeholder="请输入备注" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="scope">
              <el-button
                link
                type="danger"
                icon="Delete"
                @click="handleDeleteDetail(scope.$index)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 合计 -->
        <div style="margin-top: 10px; text-align: right; font-size: 14px; font-weight: bold;">
          合计金额：{{ totalAmount }} 元
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="RevenueTeam">
import { ref, reactive, toRefs, getCurrentInstance, computed } from 'vue'
import { listTeamRevenue, getTeamRevenue, getProjectInfo, addTeamRevenue, updateTeamRevenue, exportTeamRevenue } from "@/api/revenue/team"
import { getDeptTree, listProjectByName } from "@/api/project/project"
import { handleTree } from '@/utils/ruoyi'
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const { proxy } = getCurrentInstance()
const userStore = useUserStore()

const { sys_xmfl, sys_yjqy, sys_xmjd, sys_spzt, sys_yszt, sys_srqrzt, sys_ndgl } = proxy.useDict(
  'sys_xmfl', 'sys_yjqy', 'sys_xmjd', 'sys_spzt', 'sys_yszt', 'sys_srqrzt', 'sys_ndgl'
)

const teamRevenueList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref("")

const deptTree = ref([])
const projectManagerSelectRef = ref(null)
const marketManagerSelectRef = ref(null)

const data = reactive({
  form: {
    projectId: null,
    projectBudget: null,
    contractAmount: null,
    confirmAmount: null,
    revenueConfirmYear: null,
    detailList: []
  },
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
  },
  rules: {
    projectId: [
      { required: true, message: "请选择项目", trigger: "change" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

// 计算合计金额
const totalAmount = computed(() => {
  if (!form.value.detailList || form.value.detailList.length === 0) {
    return '0.00'
  }
  const total = form.value.detailList.reduce((sum, item) => {
    return sum + (parseFloat(item.confirmAmount) || 0)
  }, 0)
  return total.toFixed(2)
})

/** 查询列表 */
function getList() {
  loading.value = true
  listTeamRevenue(queryParams.value).then(response => {
    teamRevenueList.value = response.rows
    total.value = response.total
    loading.value = false
  })
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
  proxy.download('/revenue/team/export', {
    ...queryParams.value
  }, `团队收入确认_${new Date().getTime()}.xlsx`)
}

/** 项目名称autocomplete */
function queryProjectNames(queryString, cb) {
  if (!queryString) {
    cb([])
    return
  }
  listProjectByName({ projectName: queryString }).then(response => {
    const results = response.data.map(item => ({
      value: item.projectName,
      projectId: item.projectId
    }))
    cb(results)
  })
}

/** 加载部门树 */
function loadDeptTree() {
  getDeptTree().then(response => {
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

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "新增团队收入确认"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const projectId = row.projectId
  getTeamRevenue(projectId).then(response => {
    const project = response.data.project
    const detailList = response.data.detailList || []

    form.value = {
      projectId: project.projectId,
      projectBudget: project.projectBudget,
      contractAmount: project.contractAmount,
      confirmAmount: project.confirmAmount,
      revenueConfirmYear: project.revenueConfirmYear,
      detailList: detailList.map(item => ({
        teamConfirmId: item.teamConfirmId,
        deptId: item.deptId,
        confirmAmount: item.confirmAmount,
        confirmUserId: item.confirmUserId,
        confirmUserName: item.confirmUserName, // 确认人姓名
        remark: item.remark
      }))
    }

    open.value = true
    title.value = "修改团队收入确认"
  })
}

/** 详情按钮操作 */
function handleDetail(row) {
  router.push({
    path: '/revenue/team/detail/' + row.projectId
  })
}

/** 项目选择变化 */
function handleProjectChange(projectId) {
  if (!projectId) {
    form.value.projectBudget = null
    form.value.contractAmount = null
    form.value.confirmAmount = null
    form.value.revenueConfirmYear = null
    return
  }
  
  getProjectInfo(projectId).then(response => {
    form.value.projectBudget = response.data.projectBudget
    form.value.contractAmount = response.data.contractAmount
    form.value.confirmAmount = response.data.confirmAmount
    form.value.revenueConfirmYear = response.data.revenueConfirmYear
  })
}

/** 添加明细行 */
function handleAddDetail() {
  form.value.detailList.push({
    deptId: null,
    confirmAmount: 0,
    confirmUserId: userStore.id, // 默认当前用户ID（注意：userStore中是id不是userId）
    confirmUserName: userStore.nickName, // 默认当前用户名
    remark: ''
  })
}

/** 删除明细行 */
function handleDeleteDetail(index) {
  form.value.detailList.splice(index, 1)
  calculateTotalAmount()
}

/** 计算合计金额 */
function calculateTotalAmount() {
  // 使用 computed 自动计算，这里不需要手动操作
}

/** 表单重置 */
function reset() {
  form.value = {
    projectId: null,
    projectBudget: null,
    contractAmount: null,
    confirmAmount: null,
    revenueConfirmYear: null,
    detailList: []
  }
  proxy.resetForm("formRef")
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      if (form.value.detailList.length === 0) {
        proxy.$modal.msgError("请至少添加一条确认明细")
        return
      }

      // 验证明细列表中的必填字段
      for (let i = 0; i < form.value.detailList.length; i++) {
        const detail = form.value.detailList[i]
        if (!detail.deptId) {
          proxy.$modal.msgError(`第 ${i + 1} 条明细的部门不能为空`)
          return
        }
        if (detail.confirmAmount === null || detail.confirmAmount === undefined) {
          proxy.$modal.msgError(`第 ${i + 1} 条明细的确认金额不能为空`)
          return
        }
        if (!detail.confirmUserId) {
          proxy.$modal.msgError(`第 ${i + 1} 条明细的确认人不能为空`)
          return
        }
      }

      // 过滤掉 confirmUserName 字段（后端不需要）
      const data = {
        projectId: form.value.projectId,
        detailList: form.value.detailList.map(item => ({
          teamConfirmId: item.teamConfirmId,
          deptId: item.deptId,
          confirmAmount: item.confirmAmount,
          confirmUserId: item.confirmUserId,
          remark: item.remark
        }))
      }

      if (form.value.detailList.some(item => item.teamConfirmId)) {
        // 编辑
        updateTeamRevenue(data).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        // 新增
        addTeamRevenue(data).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

// 初始化
loadDeptTree()
getList()
</script>
