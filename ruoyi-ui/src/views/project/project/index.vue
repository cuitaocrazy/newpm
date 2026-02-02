<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="项目名称" prop="projectName">
        <el-autocomplete
          v-model="queryParams.projectName"
          :fetch-suggestions="queryProjectNames"
          placeholder="请输入项目名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="项目编号" prop="projectCode">
        <el-autocomplete
          v-model="queryParams.projectCode"
          :fetch-suggestions="queryProjectCodes"
          placeholder="请输入项目编号"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="行业" prop="industry">
        <el-select v-model="queryParams.industry" placeholder="请选择行业" clearable style="width: 200px">
          <el-option
            v-for="dict in industry"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="区域" prop="region">
        <el-select v-model="queryParams.region" placeholder="请选择区域" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_yjqy"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目状态" prop="projectStatus">
        <el-select v-model="queryParams.projectStatus" placeholder="请选择项目状态" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_xmjd"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目部门" prop="projectDept">
        <el-tree-select
          v-model="queryParams.projectDept"
          :data="deptOptions"
          :props="{ value: 'id', label: 'label', children: 'children' }"
          value-key="id"
          placeholder="请选择项目部门"
          check-strictly
          clearable
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="确认年度" prop="confirmYear">
        <el-select v-model="queryParams.confirmYear" placeholder="请选择确认年度" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_jdgl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态" prop="approvalStatus">
        <el-select v-model="queryParams.approvalStatus" placeholder="请选择审核状态" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_shzt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="立项时间" prop="startDateRange">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
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
          @click="handleApply"
          v-hasPermi="['project:project:add']"
        >立项申请</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['project:project:edit']"
        >修改</el-button>
      </el-col>
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

    <el-table v-loading="loading" :data="projectList" @selection-change="handleSelectionChange" :row-class-name="tableRowClassName">
      <el-table-column type="selection" width="55" align="center">
        <template #default="scope">
          <el-checkbox v-if="!scope.row.isSummary" v-model="scope.row.selected" @change="handleSelectionChange" />
        </template>
      </el-table-column>
      <el-table-column label="序号" type="index" width="60" align="center">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.$index }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目名称" align="center" prop="projectName" width="180" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.isSummary" style="font-weight: bold;">汇总</span>
          <span v-else>{{ scope.row.projectName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目编号" align="center" prop="projectCode" width="150" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.projectCode }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="项目经理" align="center" prop="projectManagerName" width="100">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.projectManagerName }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="市场经理" align="center" prop="marketManagerName" width="100">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.marketManagerName }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="实施年份" align="center" prop="implementationYear" width="100">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ scope.row.implementationYear }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" align="center" prop="approvalStatus" width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummary" :options="sys_shzt" :value="scope.row.approvalStatus"/>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="项目状态" align="center" prop="projectStatus" width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummary" :options="sys_xmjd" :value="scope.row.projectStatus"/>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="验收状态" align="center" prop="acceptanceStatus" width="100">
        <template #default="scope">
          <dict-tag v-if="!scope.row.isSummary" :options="sys_yszt" :value="scope.row.acceptanceStatus"/>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="项目预算(万元)" align="center" prop="projectBudget" width="130">
        <template #default="scope">
          <span :style="scope.row.isSummary ? 'font-weight: bold;' : ''">{{ formatAmountToWan(scope.row.projectBudget) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="立项时间" align="center" prop="startDate" width="110">
        <template #default="scope">
          <span v-if="!scope.row.isSummary">{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <template v-if="!scope.row.isSummary">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:project:edit']">修改</el-button>
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

    <!-- 添加或修改项目管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="projectRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="项目编号(格式:行业-区域-简称-年份)" prop="projectCode">
          <el-input v-model="form.projectCode" placeholder="请输入项目编号(格式:行业-区域-简称-年份)" />
        </el-form-item>
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目全称" prop="projectFullName">
          <el-input v-model="form.projectFullName" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="行业 字典表 字典类型industry" prop="industry">
          <el-input v-model="form.industry" placeholder="请输入行业 字典表 字典类型industry" />
        </el-form-item>
        <el-form-item label="区域 字典表 字典类型sys_yjqy" prop="region">
          <el-input v-model="form.region" placeholder="请输入区域 字典表 字典类型sys_yjqy" />
        </el-form-item>
        <el-form-item label="简称" prop="shortName">
          <el-input v-model="form.shortName" placeholder="请输入简称" />
        </el-form-item>
        <el-form-item label="年份" prop="year">
          <el-input v-model="form.year" placeholder="请输入年份" />
        </el-form-item>
        <el-form-item label="项目分类(字典表 字典类型sys_xmfl)" prop="projectCategory">
          <el-input v-model="form.projectCategory" placeholder="请输入项目分类(字典表 字典类型sys_xmfl)" />
        </el-form-item>
        <el-form-item label="项目部门" prop="projectDept">
          <el-input v-model="form.projectDept" placeholder="请输入项目部门" />
        </el-form-item>
        <el-form-item label="项目阶段(字典表 字典类型sys_xmjd)" prop="projectStatus">
          <el-radio-group v-model="form.projectStatus">
            <el-radio
              v-for="dict in sys_xmjd"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="验收状态(字典表 字典类型sys_yszt)" prop="acceptanceStatus">
          <el-radio-group v-model="form.acceptanceStatus">
            <el-radio
              v-for="dict in sys_yszt"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="预估工作量(人天)" prop="estimatedWorkload">
          <el-input v-model="form.estimatedWorkload" placeholder="请输入预估工作量(人天)" />
        </el-form-item>
        <el-form-item label="实际工作量(人天)" prop="actualWorkload">
          <el-input v-model="form.actualWorkload" placeholder="请输入实际工作量(人天)" />
        </el-form-item>
        <el-form-item label="项目地址" prop="projectAddress">
          <el-input v-model="form.projectAddress" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="项目计划" prop="projectPlan">
          <el-input v-model="form.projectPlan" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="项目描述" prop="projectDescription">
          <el-input v-model="form.projectDescription" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="项目经理ID" prop="projectManagerId">
          <el-input v-model="form.projectManagerId" placeholder="请输入项目经理ID" />
        </el-form-item>
        <el-form-item label="市场经理ID" prop="marketManagerId">
          <el-input v-model="form.marketManagerId" placeholder="请输入市场经理ID" />
        </el-form-item>
        <el-form-item label="参与人员ID列表(逗号分隔)" prop="participants">
          <el-input v-model="form.participants" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="销售负责人ID" prop="salesManagerId">
          <el-input v-model="form.salesManagerId" placeholder="请输入销售负责人ID" />
        </el-form-item>
        <el-form-item label="销售联系方式" prop="salesContact">
          <el-input v-model="form.salesContact" placeholder="请输入销售联系方式" />
        </el-form-item>
        <el-form-item label="团队负责人ID" prop="teamLeaderId">
          <el-input v-model="form.teamLeaderId" placeholder="请输入团队负责人ID" />
        </el-form-item>
        <el-form-item label="客户ID" prop="customerId">
          <el-input v-model="form.customerId" placeholder="请输入客户ID" />
        </el-form-item>
        <el-form-item label="客户联系人ID" prop="customerContactId">
          <el-input v-model="form.customerContactId" placeholder="请输入客户联系人ID" />
        </el-form-item>
        <el-form-item label="商户联系人" prop="merchantContact">
          <el-input v-model="form.merchantContact" placeholder="请输入商户联系人" />
        </el-form-item>
        <el-form-item label="商户联系方式" prop="merchantPhone">
          <el-input v-model="form.merchantPhone" placeholder="请输入商户联系方式" />
        </el-form-item>
        <el-form-item label="启动日期" prop="startDate">
          <el-date-picker clearable
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择启动日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker clearable
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择结束日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="投产日期" prop="productionDate">
          <el-date-picker clearable
            v-model="form.productionDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择投产日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="验收日期" prop="acceptanceDate">
          <el-date-picker clearable
            v-model="form.acceptanceDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择验收日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="实施年度" prop="implementationYear">
          <el-input v-model="form.implementationYear" placeholder="请输入实施年度" />
        </el-form-item>
        <el-form-item label="项目预算(元)" prop="projectBudget">
          <el-input v-model="form.projectBudget" placeholder="请输入项目预算(元)" />
        </el-form-item>
        <el-form-item label="项目费用(元)" prop="projectCost">
          <el-input v-model="form.projectCost" placeholder="请输入项目费用(元)" />
        </el-form-item>
        <el-form-item label="费用预算(元)" prop="costBudget">
          <el-input v-model="form.costBudget" placeholder="请输入费用预算(元)" />
        </el-form-item>
        <el-form-item label="成本预算(元)" prop="budgetCost">
          <el-input v-model="form.budgetCost" placeholder="请输入成本预算(元)" />
        </el-form-item>
        <el-form-item label="人力费用(元)" prop="laborCost">
          <el-input v-model="form.laborCost" placeholder="请输入人力费用(元)" />
        </el-form-item>
        <el-form-item label="采购成本" prop="purchaseCost">
          <el-input v-model="form.purchaseCost" placeholder="请输入采购成本" />
        </el-form-item>
        <el-form-item label="审批状态(待审核/已通过/已拒绝)" prop="approvalStatus">
          <el-radio-group v-model="form.approvalStatus">
            <el-radio
              v-for="dict in sys_shzt"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" prop="approvalReason">
          <el-input v-model="form.approvalReason" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="行业代码" prop="industryCode">
          <el-input v-model="form.industryCode" placeholder="请输入行业代码" />
        </el-form-item>
        <el-form-item label="区域代码(字典:sys_yjqy)" prop="regionCode">
          <el-input v-model="form.regionCode" placeholder="请输入区域代码(字典:sys_yjqy)" />
        </el-form-item>
        <el-form-item label="审批时间" prop="approvalTime">
          <el-date-picker clearable
            v-model="form.approvalTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择审批时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="审批人" prop="approverId">
          <el-input v-model="form.approverId" placeholder="请输入审批人" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="税率(%)" prop="taxRate">
          <el-input v-model="form.taxRate" placeholder="请输入税率(%)" />
        </el-form-item>
        <el-form-item label="确认人ID" prop="confirmUserId">
          <el-input v-model="form.confirmUserId" placeholder="请输入确认人ID" />
        </el-form-item>
        <el-form-item label="确认时间" prop="confirmTime">
          <el-date-picker clearable
            v-model="form.confirmTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择确认时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="备用域1" prop="reservedField1">
          <el-input v-model="form.reservedField1" placeholder="请输入备用域1" />
        </el-form-item>
        <el-form-item label="备用域2" prop="reservedField2">
          <el-input v-model="form.reservedField2" placeholder="请输入备用域2" />
        </el-form-item>
        <el-form-item label="备用域3" prop="reservedField3">
          <el-input v-model="form.reservedField3" placeholder="请输入备用域3" />
        </el-form-item>
        <el-form-item label="备用域4" prop="reservedField4">
          <el-input v-model="form.reservedField4" placeholder="请输入备用域4" />
        </el-form-item>
        <el-form-item label="备用域5" prop="reservedField5">
          <el-input v-model="form.reservedField5" placeholder="请输入备用域5" />
        </el-form-item>
        <el-form-item label="删除标志(0正常 1删除)" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="请输入删除标志(0正常 1删除)" />
        </el-form-item>
        <el-form-item label="确认状态" prop="confirmStatus">
          <el-radio-group v-model="form.confirmStatus">
            <el-radio
              v-for="dict in sys_qrzt"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="确认季度(字典表 字典类型 sys_jdgl)" prop="confirmQuarter">
          <el-input v-model="form.confirmQuarter" placeholder="请输入确认季度(字典表 字典类型 sys_jdgl)" />
        </el-form-item>
        <el-form-item label="确认金额" prop="confirmAmount">
          <el-input v-model="form.confirmAmount" placeholder="请输入确认金额" />
        </el-form-item>
        <el-form-item label="税后金额" prop="afterTaxAmount">
          <el-input v-model="form.afterTaxAmount" placeholder="请输入税后金额" />
        </el-form-item>
        <el-form-item label="确认人姓名" prop="confirmUserName">
          <el-input v-model="form.confirmUserName" placeholder="请输入确认人姓名" />
        </el-form-item>
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

<script setup name="Project">
import { listProject, getProject, delProject, addProject, updateProject, getProjectNameList, getProjectCodeList, getProjectSummary } from "@/api/project/project"
import { listDept } from "@/api/system/dept"
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()
const { sys_qrzt, sys_shzt, sys_xmjd, sys_yszt, industry, sys_yjqy, sys_jdgl, sys_htzt } = proxy.useDict('sys_qrzt', 'sys_shzt', 'sys_xmjd', 'sys_yszt', 'industry', 'sys_yjqy', 'sys_jdgl', 'sys_htzt')

const projectList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const dateRange = ref([])
const deptOptions = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    projectCode: null,
    industry: null,
    region: null,
    projectStatus: null,
    projectDept: null,
    confirmYear: new Date().getFullYear().toString(), // 默认当年
    approvalStatus: null
  },
  rules: {
    projectId: [
      { required: true, message: "项目ID不能为空", trigger: "blur" }
    ],
    projectCode: [
      { required: true, message: "项目编号(格式:行业-区域-简称-年份)不能为空", trigger: "blur" }
    ],
    projectName: [
      { required: true, message: "项目名称不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询项目管理列表 */
function getList() {
  loading.value = true
  const params = proxy.addDateRange(queryParams.value, dateRange.value, 'StartDate')

  // 先获取汇总数据
  getProjectSummary(params).then(summaryResponse => {
    const summaryData = summaryResponse.data || { projectBudgetSum: 0 }

    // 再获取列表数据
    listProject(params).then(response => {
      // 创建汇总行
      const summaryRow = {
        isSummary: true,
        projectName: '汇总',
        projectBudget: summaryData.projectBudgetSum || 0
      }

      // 将汇总行插入到列表第一条
      projectList.value = [summaryRow, ...response.rows]
      total.value = response.total
      loading.value = false
    })
  })
}

/** 获取部门树 */
function getDeptTree() {
  listDept().then(response => {
    // 手动转换数据结构，确保 label 字段正确
    const deptData = response.data.map(dept => ({
      ...dept,
      id: dept.deptId,
      label: dept.deptName
    }))
    // 使用 handleTree 构建树形结构
    deptOptions.value = proxy.handleTree(deptData, "id")
  })
}

/** 项目名称智能提示 */
function queryProjectNames(queryString, cb) {
  // 如果没有输入内容，传空字符串给后端获取所有数据
  getProjectNameList(queryString || '').then(response => {
    const results = response.data.map(name => ({ value: name }))
    cb(results)
  })
}

/** 项目编号智能提示 */
function queryProjectCodes(queryString, cb) {
  // 如果没有输入内容，传空字符串给后端获取所有数据
  getProjectCodeList(queryString || '').then(response => {
    const results = response.data.map(code => ({ value: code }))
    cb(results)
  })
}

/** 金额格式化为万元 */
function formatAmountToWan(amount) {
  if (amount == null || amount === '') return '0.00'
  return (Number(amount) / 10000).toFixed(2)
}

/** 表格行样式 */
function tableRowClassName({ row }) {
  if (row.isSummary) {
    return 'summary-row'
  }
  return ''
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    projectId: null,
    projectCode: null,
    projectName: null,
    projectFullName: null,
    industry: null,
    region: null,
    shortName: null,
    year: null,
    projectCategory: null,
    projectDept: null,
    projectStatus: null,
    acceptanceStatus: null,
    estimatedWorkload: null,
    actualWorkload: null,
    projectAddress: null,
    projectPlan: null,
    projectDescription: null,
    projectManagerId: null,
    marketManagerId: null,
    participants: null,
    salesManagerId: null,
    salesContact: null,
    teamLeaderId: null,
    customerId: null,
    customerContactId: null,
    merchantContact: null,
    merchantPhone: null,
    startDate: null,
    endDate: null,
    productionDate: null,
    acceptanceDate: null,
    implementationYear: null,
    projectBudget: null,
    projectCost: null,
    costBudget: null,
    budgetCost: null,
    laborCost: null,
    purchaseCost: null,
    approvalStatus: null,
    approvalReason: null,
    industryCode: null,
    regionCode: null,
    approvalTime: null,
    approverId: null,
    remark: null,
    taxRate: null,
    confirmUserId: null,
    confirmTime: null,
    reservedField1: null,
    reservedField2: null,
    reservedField3: null,
    reservedField4: null,
    reservedField5: null,
    delFlag: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    confirmStatus: null,
    confirmQuarter: null,
    confirmAmount: null,
    afterTaxAmount: null,
    confirmUserName: null
  }
  proxy.resetForm("projectRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = []
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.projectId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加项目管理"
}

/** 立项申请按钮操作 */
function handleApply() {
  proxy.$router.push('/project/apply')
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _projectId = row.projectId || ids.value
  getProject(_projectId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改项目管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["projectRef"].validate(valid => {
    if (valid) {
      if (form.value.projectId != null) {
        updateProject(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addProject(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _projectIds = row.projectId || ids.value
  proxy.$modal.confirm('是否确认删除项目管理编号为"' + _projectIds + '"的数据项？').then(function() {
    return delProject(_projectIds)
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

// 初始化
getDeptTree()
getList()
</script>

<style scoped>
.summary-row {
  background-color: #f5f7fa;
  font-weight: bold;
}
</style>
