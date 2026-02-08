<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="项目名称" prop="projectName">
        <el-input
          v-model="queryParams.projectName"
          placeholder="请输入项目名称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="项目部门" prop="projectDept">
        <ProjectDeptSelect
          v-model="queryParams.projectDept"
          placeholder="请选择项目部门"
          width="200px"
        />
      </el-form-item>
      <el-form-item label="项目分类" prop="projectCategory">
        <el-select v-model="queryParams.projectCategory" placeholder="请选择项目分类" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_xmfl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="一级区域" prop="region">
        <el-select v-model="queryParams.region" placeholder="请选择一级区域" clearable @change="handleRegionChange" style="width: 200px">
          <el-option
            v-for="dict in sys_yjqy"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="二级区域" prop="provinceId">
        <el-select v-model="queryParams.provinceId" placeholder="请选择二级区域" clearable :disabled="!queryParams.region" style="width: 200px">
          <el-option
            v-for="item in secondaryRegionOptions"
            :key="item.provinceId"
            :label="item.provinceName"
            :value="item.provinceId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目经理" prop="projectManagerId">
        <el-select v-model="queryParams.projectManagerId" placeholder="请选择项目经理" clearable filterable style="width: 200px">
          <el-option
            v-for="user in projectManagerOptions"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="市场经理" prop="marketManagerId">
        <el-select v-model="queryParams.marketManagerId" placeholder="请选择市场经理" clearable filterable style="width: 200px">
          <el-option
            v-for="user in marketManagerOptions"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态" prop="approvalStatus">
        <el-select v-model="queryParams.approvalStatus" placeholder="请选择审核状态" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_spzt"
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
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="reviewList">
      <el-table-column label="序号" type="index" width="55" align="center" />
      <el-table-column label="项目名称" align="center" prop="projectName" min-width="150" show-overflow-tooltip />
      <el-table-column label="项目编码" align="center" prop="projectCode" min-width="150" show-overflow-tooltip />
      <el-table-column label="项目部门" align="center" prop="deptName" min-width="120" show-overflow-tooltip />
      <el-table-column label="项目经理" align="center" prop="projectManagerName" min-width="100" />
      <el-table-column label="市场经理" align="center" prop="marketManagerName" min-width="100" />
      <el-table-column label="项目分类" align="center" prop="projectCategory" width="100">
        <template #default="scope">
          <dict-tag :options="sys_xmfl" :value="scope.row.projectCategory"/>
        </template>
      </el-table-column>
      <el-table-column label="一级区域" align="center" prop="region" width="100">
        <template #default="scope">
          <dict-tag :options="sys_yjqy" :value="scope.row.region"/>
        </template>
      </el-table-column>
      <el-table-column label="二级区域" align="center" prop="provinceName" width="100" />
      <el-table-column label="项目阶段" align="center" prop="projectStatus" width="100">
        <template #default="scope">
          <dict-tag :options="sys_xmjd" :value="scope.row.projectStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleReview(scope.row)"
            v-hasPermi="['project:review:approve']"
          >审核</el-button>
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

    <!-- 审核抽屉 -->
    <el-drawer
      v-model="reviewOpen"
      :title="'审核项目 - ' + reviewForm.projectName"
      direction="rtl"
      size="60%"
      :close-on-click-modal="true"
      :close-on-press-escape="false"
      class="review-drawer"
    >
      <template #header>
        <div class="drawer-header">
          <span class="drawer-title">审核项目 - {{ reviewForm.projectName }}</span>
          <el-link type="primary" :underline="false" @click="toggleAllCollapse">
            {{ isAllExpanded ? '全部收起' : '全部展开' }}
          </el-link>
        </div>
      </template>

      <el-collapse v-model="activeNames" style="margin-top: -10px;">
        <!-- 基本信息 -->
        <el-collapse-item title="基本信息" name="1">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目名称">{{ reviewForm.projectName }}</el-descriptions-item>
            <el-descriptions-item label="项目编码">{{ reviewForm.projectCode }}</el-descriptions-item>
            <el-descriptions-item label="项目部门">{{ reviewForm.deptName }}</el-descriptions-item>
            <el-descriptions-item label="项目分类">
              <dict-tag :options="sys_xmfl" :value="reviewForm.projectCategory"/>
            </el-descriptions-item>
            <el-descriptions-item label="预估工作量">{{ reviewForm.estimatedWorkload }} 人天</el-descriptions-item>
            <el-descriptions-item label="实际工作量">{{ reviewForm.actualWorkload }} 人天</el-descriptions-item>
            <el-descriptions-item label="项目阶段">
              <dict-tag :options="sys_xmjd" :value="reviewForm.projectStatus"/>
            </el-descriptions-item>
            <el-descriptions-item label="验收状态">
              <dict-tag :options="sys_yszt" :value="reviewForm.acceptanceStatus"/>
            </el-descriptions-item>
            <el-descriptions-item label="一级区域">
              <dict-tag :options="sys_yjqy" :value="reviewForm.region"/>
            </el-descriptions-item>
            <el-descriptions-item label="二级区域">{{ reviewForm.provinceName }}</el-descriptions-item>
            <el-descriptions-item label="项目地址" :span="2">{{ reviewForm.projectAddress }}</el-descriptions-item>
            <el-descriptions-item label="项目计划" :span="2">{{ reviewForm.projectPlan }}</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>

        <!-- 人员配置 -->
        <el-collapse-item title="人员配置" name="2">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目经理">{{ reviewForm.projectManagerName }}</el-descriptions-item>
            <el-descriptions-item label="市场经理">{{ reviewForm.marketManagerName }}</el-descriptions-item>
            <el-descriptions-item label="销售负责人">{{ reviewForm.salesManagerName }}</el-descriptions-item>
            <el-descriptions-item label="销售联系方式">{{ reviewForm.salesContact }}</el-descriptions-item>
            <el-descriptions-item label="参与人员" :span="2">{{ reviewForm.participantsNames }}</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>

        <!-- 客户信息 -->
        <el-collapse-item title="客户信息" name="3">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="客户名称">{{ reviewForm.customerName }}</el-descriptions-item>
            <el-descriptions-item label="客户联系人">{{ reviewForm.customerContactName }}</el-descriptions-item>
            <el-descriptions-item label="客户联系方式">{{ reviewForm.customerContactPhone }}</el-descriptions-item>
            <el-descriptions-item label="商户联系人">{{ reviewForm.merchantContact }}</el-descriptions-item>
            <el-descriptions-item label="商户联系方式">{{ reviewForm.merchantPhone }}</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>

        <!-- 时间规划 -->
        <el-collapse-item title="时间规划" name="4">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="启动日期">{{ parseTime(reviewForm.startDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="结束日期">{{ parseTime(reviewForm.endDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="上线日期">{{ parseTime(reviewForm.productionDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="验收日期">{{ parseTime(reviewForm.acceptanceDate, '{y}-{m}-{d}') }}</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>

        <!-- 成本预算 -->
        <el-collapse-item title="成本预算" name="5">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目预算">{{ reviewForm.projectBudget }}</el-descriptions-item>
            <el-descriptions-item label="项目费用">{{ reviewForm.projectCost }}</el-descriptions-item>
            <el-descriptions-item label="成本预算">{{ reviewForm.costBudget }}</el-descriptions-item>
            <el-descriptions-item label="人工成本">{{ reviewForm.laborCost }}</el-descriptions-item>
            <el-descriptions-item label="采购成本">{{ reviewForm.purchaseCost }}</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>
      </el-collapse>

      <!-- 审核表单 -->
      <el-divider />
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="100px">
        <el-form-item label="审核意见" prop="approvalReason">
          <el-input
            v-model="reviewForm.approvalReason"
            type="textarea"
            :rows="3"
            placeholder="请输入审核意见（拒绝时必填）"
            :class="{ 'is-error': showReasonError }"
            @input="showReasonError = false"
          />
          <transition name="fade">
            <div v-if="showReasonError" class="error-tip">
              <el-icon><WarningFilled /></el-icon>
              <span>拒绝审核时必须填写审核意见</span>
            </div>
          </transition>
        </el-form-item>
      </el-form>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 10px;">
          <el-button type="success" @click="submitApprove('1')">通过</el-button>
          <el-button type="danger" @click="submitApprove('2')">拒绝</el-button>
          <el-button @click="cancelReview">取消</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 列表和对话框将在后续步骤添加 -->
  </div>
</template>

<script setup name="Review">
import { listReview, getReview, approveProject } from "@/api/project/review"
import { listSecondaryRegion } from "@/api/project/secondaryRegion"
import { listUserByPost } from "@/api/system/user"
import request from '@/utils/request'
import ProjectDeptSelect from '@/components/ProjectDeptSelect/index.vue'
import { WarningFilled } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()
const { sys_xmfl, sys_yjqy, sys_xmjd, sys_spzt, sys_yszt } = proxy.useDict('sys_xmfl', 'sys_yjqy', 'sys_xmjd', 'sys_spzt', 'sys_yszt')

const reviewList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const secondaryRegionOptions = ref([])
const projectManagerOptions = ref([])
const marketManagerOptions = ref([])
const reviewOpen = ref(false)
const activeNames = ref(['1', '2', '3', '4', '5'])
const isAllExpanded = ref(true)
const showReasonError = ref(false)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    projectDept: null,
    projectCategory: null,
    region: null,
    provinceId: null,
    projectManagerId: null,
    marketManagerId: null,
    approvalStatus: '0'  // 默认查询待审核
  },
  reviewForm: {},
  reviewRules: {
    approvalReason: [
      { required: false, message: "审核意见不能为空", trigger: "blur" }
    ]
  }
})

const { queryParams, reviewForm, reviewRules } = toRefs(data)

/** 查询列表 */
function getList() {
  loading.value = true
  listReview(queryParams.value).then(response => {
    reviewList.value = response.rows
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
  queryParams.value.approvalStatus = '0'  // 重置后仍然默认查询待审核
  handleQuery()
}

/** 一级区域变化 */
function handleRegionChange(value) {
  queryParams.value.provinceId = null
  secondaryRegionOptions.value = []
  if (value) {
    listSecondaryRegion({ regionCode: value }).then(response => {
      secondaryRegionOptions.value = response.rows
    })
  }
}

/** 查询用户列表 */
function getUserOptions() {
  // 查询项目经理（岗位代码：pm）
  listUserByPost('pm').then(response => {
    projectManagerOptions.value = response.data || []
  })
  // 查询市场经理（岗位代码：scjl）
  listUserByPost('scjl').then(response => {
    marketManagerOptions.value = response.data || []
  })
}

/** 审核按钮操作 */
function handleReview(row) {
  reviewOpen.value = true
  const projectId = row.projectId
  getReview(projectId).then(response => {
    reviewForm.value = response.data
    reviewForm.value.approvalReason = null
    // 默认展开所有折叠面板
    activeNames.value = ['1', '2', '3', '4', '5']
    isAllExpanded.value = true
  })
}

/** 切换全部折叠/展开 */
function toggleAllCollapse() {
  if (isAllExpanded.value) {
    // 收起所有
    activeNames.value = ['1']
    isAllExpanded.value = false
  } else {
    // 展开所有
    activeNames.value = ['1', '2', '3', '4', '5']
    isAllExpanded.value = true
  }
}

/** 取消审核 */
function cancelReview() {
  reviewOpen.value = false
  showReasonError.value = false
  reset()
}

/** 表单重置 */
function reset() {
  reviewForm.value = {
    projectId: null,
    approvalReason: null
  }
  proxy.resetForm("reviewFormRef")
}

/** 提交审核 */
function submitApprove(approvalStatus) {
  // 拒绝时必须填写审核意见
  if (approvalStatus === '2' && (!reviewForm.value.approvalReason || reviewForm.value.approvalReason.trim() === '')) {
    // 显示输入框下方的错误提示
    showReasonError.value = true
    // 滚动到审核意见输入框位置
    nextTick(() => {
      const formElement = document.querySelector('.review-drawer .el-form')
      if (formElement) {
        formElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    })
    return
  }

  const statusText = approvalStatus === '1' ? '通过' : '拒绝'
  proxy.$modal.confirm('是否确认' + statusText + '该项目立项申请？').then(() => {
    const data = {
      projectId: reviewForm.value.projectId,
      approvalStatus: approvalStatus,
      approvalReason: reviewForm.value.approvalReason
    }
    return approveProject(data)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("审核" + statusText + "成功")
    reviewOpen.value = false
    showReasonError.value = false
  }).catch(() => {})
}

// 初始化
getUserOptions()
getList()
</script>

<style scoped>
.review-drawer :deep(.el-drawer__body) {
  padding-top: 10px;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.drawer-title {
  font-size: 16px;
  font-weight: 500;
}

.error-tip {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #f56c6c;
  font-size: 12px;
  margin-top: 5px;
}

.error-tip .el-icon {
  font-size: 14px;
}

.is-error :deep(.el-textarea__inner) {
  border-color: #f56c6c;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
