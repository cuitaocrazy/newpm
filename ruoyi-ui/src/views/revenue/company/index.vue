<template>
  <div class="app-container">
    <!-- 查询表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="项目名称" prop="projectName">
        <el-autocomplete
          v-model="queryParams.projectName"
          :fetch-suggestions="queryProjectNameSuggestions"
          placeholder="请输入项目名称"
          clearable
          @keyup.enter="handleQuery"
          @select="handleQuery"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="项目部门" prop="projectDept">
        <ProjectDeptSelect
          v-model="queryParams.projectDept"
          :filterable="true"
          width="200px"
        />
      </el-form-item>
      <el-form-item label="收入确认年度" prop="revenueConfirmYear">
        <el-select v-model="queryParams.revenueConfirmYear" placeholder="请选择收入确认年度" clearable>
          <el-option
            v-for="dict in sys_ndgl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="收入确认状态" prop="revenueConfirmStatus">
        <el-select v-model="queryParams.revenueConfirmStatus" placeholder="请选择收入确认状态" clearable>
          <el-option
            v-for="dict in sys_srqrzt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目分类" prop="projectCategory">
        <el-select v-model="queryParams.projectCategory" placeholder="请选择项目分类" clearable>
          <el-option
            v-for="dict in sys_xmfl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="一级区域" prop="region">
        <el-select v-model="queryParams.region" placeholder="请选择一级区域" clearable @change="handleRegionChange">
          <el-option
            v-for="dict in sys_yjqy"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="二级区域" prop="provinceId">
        <el-select v-model="queryParams.provinceId" placeholder="请选择二级区域" clearable :disabled="!queryParams.region" filterable>
          <el-option
            v-for="item in secondaryRegionOptions"
            :key="item.provinceId"
            :label="item.provinceName"
            :value="item.provinceId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目经理" prop="projectManagerId">
        <el-select v-model="queryParams.projectManagerId" placeholder="请选择项目经理" clearable filterable>
          <el-option
            v-for="user in projectManagerList"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="市场经理" prop="marketManagerId">
        <el-select v-model="queryParams.marketManagerId" placeholder="请选择市场经理" clearable filterable>
          <el-option
            v-for="user in marketManagerList"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="立项年度" prop="establishedYear">
        <el-select v-model="queryParams.establishedYear" placeholder="请选择立项年度" clearable>
          <el-option
            v-for="dict in sys_ndgl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态" prop="approvalStatus">
        <el-select v-model="queryParams.approvalStatus" placeholder="请选择审核状态" clearable>
          <el-option
            v-for="dict in sys_spzt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="验收状态" prop="acceptanceStatus">
        <el-select v-model="queryParams.acceptanceStatus" placeholder="请选择验收状态" clearable>
          <el-option
            v-for="dict in sys_yszt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="合同状态" prop="contractStatus">
        <el-select v-model="queryParams.contractStatus" placeholder="请选择合同状态" clearable>
          <el-option
            v-for="dict in sys_htzt"
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

    <!-- 工具栏 -->
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

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="revenueListWithSummary"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" :selectable="checkSelectable" />
      <el-table-column label="序号" width="55" align="center">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold;">合计</span>
          <span v-else>{{ scope.$index }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目名称" align="center" prop="projectName" min-width="150" show-overflow-tooltip />
      <el-table-column label="项目部门" align="center" prop="deptName" min-width="120" show-overflow-tooltip />
      <el-table-column label="项目经理" align="center" prop="projectManagerName" min-width="100" />
      <el-table-column label="项目分类" align="center" prop="projectCategory" width="100">
        <template #default="scope">
          <dict-tag :options="sys_xmfl" :value="scope.row.projectCategory"/>
        </template>
      </el-table-column>
      <el-table-column label="二级区域" align="center" prop="provinceName" width="100" show-overflow-tooltip />
      <el-table-column label="项目预算（元）" align="center" prop="projectBudget" width="120">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold;">{{ scope.row.projectBudget }}</span>
          <span v-else>{{ scope.row.projectBudget ? parseFloat(scope.row.projectBudget).toLocaleString() : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预估工作量（人天）" align="center" prop="estimatedWorkload" width="140">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold;">{{ scope.row.estimatedWorkload }}</span>
          <span v-else>{{ scope.row.estimatedWorkload }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实际人天" align="center" prop="actualWorkload" width="100">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold;">{{ scope.row.actualWorkload }}</span>
          <span v-else>{{ scope.row.actualWorkload }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合同名称" align="center" prop="contractName" min-width="150" show-overflow-tooltip />
      <el-table-column label="合同金额（元）" align="center" prop="contractAmount" width="130">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold;">{{ scope.row.contractAmount }}</span>
          <span v-else>{{ scope.row.contractAmount ? parseFloat(scope.row.contractAmount).toLocaleString() : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收入确认年度" align="center" prop="revenueConfirmYear" width="120">
        <template #default="scope">
          <dict-tag :options="sys_ndgl" :value="scope.row.revenueConfirmYear"/>
        </template>
      </el-table-column>
      <el-table-column label="合同状态" align="center" prop="contractStatus" width="100" />
      <el-table-column label="收入确认状态" align="center" prop="revenueConfirmStatus" width="120">
        <template #default="scope">
          <dict-tag :options="sys_srqrzt" :value="scope.row.revenueConfirmStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="确认金额（元）" align="center" prop="confirmAmount" width="130">
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold;">{{ scope.row.confirmAmount }}</span>
          <span v-else>{{ scope.row.confirmAmount ? parseFloat(scope.row.confirmAmount).toLocaleString() : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="参与人员" align="center" prop="participantsNames" min-width="150" show-overflow-tooltip />
      <el-table-column label="启动日期" align="center" prop="startDate" width="110">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束日期" align="center" prop="endDate" width="110">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="验收日期" align="center" prop="acceptanceDate" width="110">
        <template #default="scope">
          <span>{{ parseTime(scope.row.acceptanceDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" align="center" prop="approvalStatus" width="100">
        <template #default="scope">
          <dict-tag :options="sys_spzt" :value="scope.row.approvalStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="项目状态" align="center" prop="projectStatus" width="100">
        <template #default="scope">
          <dict-tag :options="sys_xmjd" :value="scope.row.projectStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="验收状态" align="center" prop="acceptanceStatus" width="100">
        <template #default="scope">
          <dict-tag :options="sys_yszt" :value="scope.row.acceptanceStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="更新人" align="center" prop="updateBy" width="100" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="110">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150" fixed="right">
        <template #default="scope">
          <!-- 合计行不显示操作按钮 -->
          <template v-if="!scope.row.isSummary">
            <!-- 未确认：显示"收入确认"按钮 -->
            <el-button
              v-if="scope.row.revenueConfirmStatus === '0'"
              link
              type="primary"
              icon="Edit"
              @click="handleConfirm(scope.row)"
              v-hasPermi="['revenue:company:edit']"
            >收入确认</el-button>
            <!-- 已确认：显示"查看确认"按钮 -->
            <el-button
              v-if="scope.row.revenueConfirmStatus === '1'"
              link
              type="success"
              icon="View"
              @click="handleView(scope.row)"
              v-hasPermi="['revenue:company:view']"
            >查看确认</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 收入确认抽屉 -->
    <RevenueConfirmDrawer
      ref="drawerRef"
      @refresh="getList"
    />
  </div>
</template>

<script setup name="RevenueCompany">
import { listRevenueCompany, exportRevenueCompany } from "@/api/revenue/company";
import { listUser, listUserByPost } from "@/api/system/user";
import { listSecondaryRegion } from "@/api/project/secondaryRegion";
import RevenueConfirmDrawer from './components/RevenueConfirmDrawer.vue';
import ProjectDeptSelect from '@/components/ProjectDeptSelect/index.vue';

const { proxy } = getCurrentInstance();
const { sys_srqrzt, sys_ndgl, sys_xmfl, sys_yjqy, sys_spzt, sys_xmjd, sys_yszt, sys_htzt } = proxy.useDict('sys_srqrzt', 'sys_ndgl', 'sys_xmfl', 'sys_yjqy', 'sys_spzt', 'sys_xmjd', 'sys_yszt', 'sys_htzt');

const revenueList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const total = ref(0);
const userList = ref([]);
const userMap = ref({});
const drawerRef = ref(null);
const secondaryRegionOptions = ref([]);
const projectManagerList = ref([]);
const marketManagerList = ref([]);

/** 带合计行的列表数据 */
const revenueListWithSummary = computed(() => {
  if (revenueList.value.length === 0) {
    return [];
  }

  // 计算合计
  const summary = {
    isSummary: true,
    projectBudget: 0,
    estimatedWorkload: 0,
    actualWorkload: 0,
    contractAmount: 0,
    confirmAmount: 0
  };

  revenueList.value.forEach(item => {
    summary.projectBudget += Number(item.projectBudget) || 0;
    summary.estimatedWorkload += Number(item.estimatedWorkload) || 0;
    summary.actualWorkload += Number(item.actualWorkload) || 0;
    summary.contractAmount += Number(item.contractAmount) || 0;
    summary.confirmAmount += Number(item.confirmAmount) || 0;
  });

  // 格式化合计值
  summary.projectBudget = summary.projectBudget.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  summary.estimatedWorkload = summary.estimatedWorkload.toFixed(2);
  summary.actualWorkload = summary.actualWorkload.toFixed(2);
  summary.contractAmount = summary.contractAmount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  summary.confirmAmount = summary.confirmAmount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  // 将合计行插入到第一行
  return [summary, ...revenueList.value];
});

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    projectDept: null,
    revenueConfirmYear: null,
    revenueConfirmStatus: null,
    projectCategory: null,
    region: null,
    provinceId: null,
    projectManagerId: null,
    marketManagerId: null,
    establishedYear: null,
    approvalStatus: null,
    acceptanceStatus: null,
    contractStatus: null
  }
});

const { queryParams } = toRefs(data);

/** 查询公司收入确认列表 */
function getList() {
  loading.value = true;
  listRevenueCompany(queryParams.value).then(response => {
    revenueList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** 查询用户列表 */
function getUserList() {
  listUser().then(response => {
    userList.value = response.rows;
    // 构建用户ID到姓名的映射
    userList.value.forEach(user => {
      userMap.value[user.userId] = user.nickName;
    });
  });

  // 查询项目经理列表（岗位代码：pm）
  listUserByPost('pm').then(response => {
    projectManagerList.value = response.data || [];
  });

  // 查询市场经理列表（岗位代码：scjl）
  listUserByPost('scjl').then(response => {
    marketManagerList.value = response.data || [];
  });
}

/** 根据用户ID获取用户名 */
function getUserName(userId) {
  return userMap.value[userId] || userId;
}

/** 一级区域变化时加载二级区域 */
function handleRegionChange(value) {
  queryParams.value.provinceId = null;
  secondaryRegionOptions.value = [];
  if (value) {
    listSecondaryRegion({ regionDictValue: value }).then(response => {
      secondaryRegionOptions.value = response.rows || [];
    });
  }
}

/** 项目名称自动补全 */
function queryProjectNameSuggestions(queryString, cb) {
  // 如果输入为空，不显示建议
  if (!queryString) {
    cb([]);
    return;
  }

  // 从当前列表数据中提取项目名称作为建议
  const suggestions = revenueList.value
    .filter(item => item.projectName && item.projectName.toLowerCase().includes(queryString.toLowerCase()))
    .map(item => ({
      value: item.projectName
    }));

  // 去重
  const uniqueSuggestions = Array.from(
    new Map(suggestions.map(item => [item.value, item])).values()
  );

  cb(uniqueSuggestions);
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  secondaryRegionOptions.value = [];
  handleQuery();
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.projectId);
}

/** 选择框是否可选 */
function checkSelectable(row) {
  return !row.isSummary;
}

/** 收入确认按钮操作 */
function handleConfirm(row) {
  drawerRef.value.open(row.projectId, false);
}

/** 查看确认按钮操作 */
function handleView(row) {
  drawerRef.value.open(row.projectId, true);
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('project/project/revenueExport', {
    ...queryParams.value
  }, `公司收入确认_${new Date().getTime()}.xlsx`)
}

onMounted(() => {
  getList();
  getUserList();
});
</script>
