<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="项目名称" prop="projectName">
        <el-autocomplete
          v-model="queryParams.projectName"
          :fetch-suggestions="queryProjectNames"
          placeholder="请输入项目名称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 200px"
        />
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
      <el-form-item label="收入确认年度" prop="revenueConfirmYear">
        <el-select v-model="queryParams.revenueConfirmYear" placeholder="请选择收入确认年度" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_ndgl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
      <el-form-item label="二级区域" prop="regionCode">
        <el-select v-model="queryParams.regionCode" placeholder="请选择二级区域" clearable :disabled="!queryParams.region" style="width: 200px">
          <el-option
            v-for="item in secondaryRegionOptions"
            :key="item.provinceCode"
            :label="item.provinceName"
            :value="item.provinceCode"
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
      <el-form-item label="立项年度" prop="establishedYear">
        <el-select v-model="queryParams.establishedYear" placeholder="请选择立项年度" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_ndgl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目阶段" prop="projectStatus">
        <el-select v-model="queryParams.projectStatus" placeholder="请选择项目阶段" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_xmjd"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
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
      <el-form-item label="验收状态" prop="acceptanceStatus">
        <el-select v-model="queryParams.acceptanceStatus" placeholder="请选择验收状态" clearable style="width: 200px">
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
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['project:project:add']"
        >新增</el-button>
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

    <el-table v-loading="loading" :data="projectList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" type="index" width="55" align="center" />
      <el-table-column label="项目名称" align="center" prop="projectName" min-width="150" show-overflow-tooltip />
      <el-table-column label="项目部门" align="center" prop="projectDeptName" min-width="120" show-overflow-tooltip />
      <el-table-column label="项目经理" align="center" prop="projectManagerName" min-width="100" />
      <el-table-column label="项目分类" align="center" prop="projectCategory" width="100">
        <template #default="scope">
          <dict-tag :options="sys_xmfl" :value="scope.row.projectCategory"/>
        </template>
      </el-table-column>
      <el-table-column label="项目预算" align="center" prop="projectBudget" width="120" />
      <el-table-column label="预估工作量" align="center" prop="estimatedWorkload" width="100" />
      <el-table-column label="实际人天" align="center" prop="actualWorkload" width="100" />
      <el-table-column label="合同金额" align="center" prop="contractAmount" width="120" />
      <el-table-column label="收入确认年度" align="center" prop="revenueConfirmYear" width="120">
        <template #default="scope">
          <dict-tag :options="sys_ndgl" :value="scope.row.revenueConfirmYear"/>
        </template>
      </el-table-column>
      <el-table-column label="合同状态" align="center" prop="contractStatus" width="100" />
      <el-table-column label="收入确认状态" align="center" prop="revenueConfirmStatus" width="120" />
      <el-table-column label="确认金额" align="center" prop="confirmAmount" width="120" />
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
      <el-table-column label="操作" align="center" width="150" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:project:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:project:remove']">删除</el-button>
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
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目全称" prop="projectFullName">
          <el-input v-model="form.projectFullName" placeholder="请输入项目全称" />
        </el-form-item>
        <el-form-item label="行业" prop="industry">
          <el-select v-model="form.industry" placeholder="请选择行业">
            <el-option
              v-for="dict in industry"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="一级区域" prop="region">
          <el-select v-model="form.region" placeholder="请选择一级区域">
            <el-option
              v-for="dict in sys_yjqy"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="简称" prop="shortName">
          <el-input v-model="form.shortName" placeholder="请输入简称" />
        </el-form-item>
        <el-form-item label="立项年份" prop="establishedYear">
          <el-select v-model="form.establishedYear" placeholder="请选择立项年份">
            <el-option
              v-for="dict in sys_ndgl"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="项目分类" prop="projectCategory">
          <el-select v-model="form.projectCategory" placeholder="请选择项目分类">
            <el-option
              v-for="dict in sys_xmfl"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="项目状态" prop="projectStatus">
          <el-select v-model="form.projectStatus" placeholder="请选择项目状态">
            <el-option
              v-for="dict in sys_xmzt"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="验收状态" prop="acceptanceStatus">
          <el-select v-model="form.acceptanceStatus" placeholder="请选择验收状态">
            <el-option
              v-for="dict in sys_yszt"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="预估工作量(人天)" prop="estimatedWorkload">
          <el-input v-model="form.estimatedWorkload" placeholder="请输入预估工作量(人天)" />
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
        <el-form-item label="团队负责人ID" prop="teamLeaderId">
          <el-input v-model="form.teamLeaderId" placeholder="请输入团队负责人ID" />
        </el-form-item>
        <el-form-item label="商户联系人" prop="merchantContact">
          <el-input v-model="form.merchantContact" placeholder="请输入商户联系人" />
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
        <el-form-item label="项目预算(元)" prop="projectBudget">
          <el-input v-model="form.projectBudget" placeholder="请输入项目预算(元)" />
        </el-form-item>
        <el-form-item label="项目费用(元)" prop="projectCost">
          <el-input v-model="form.projectCost" placeholder="请输入项目费用(元)" />
        </el-form-item>
        <el-form-item label="费用预算(元)" prop="expenseBudget">
          <el-input v-model="form.expenseBudget" placeholder="请输入费用预算(元)" />
        </el-form-item>
        <el-form-item label="成本预算(元)" prop="costBudget">
          <el-input v-model="form.costBudget" placeholder="请输入成本预算(元)" />
        </el-form-item>
        <el-form-item label="人力费用(元)" prop="laborCost">
          <el-input v-model="form.laborCost" placeholder="请输入人力费用(元)" />
        </el-form-item>
        <el-form-item label="采购成本" prop="purchaseCost">
          <el-input v-model="form.purchaseCost" placeholder="请输入采购成本" />
        </el-form-item>
        <el-form-item label="审批状态" prop="approvalStatus">
          <el-select v-model="form.approvalStatus" placeholder="请选择审批状态">
            <el-option
              v-for="dict in sys_spzt"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="审批意见" prop="approvalReason">
          <el-input v-model="form.approvalReason" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="行业代码" prop="industryCode">
          <el-input v-model="form.industryCode" placeholder="请输入行业代码" />
        </el-form-item>
        <el-form-item label="区域代码" prop="regionCode">
          <el-select v-model="form.regionCode" placeholder="请选择区域代码">
            <el-option
              v-for="dict in sys_yjqy"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
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
        <el-form-item label="收入确认年度" prop="revenueConfirmYear">
          <el-select v-model="form.revenueConfirmYear" placeholder="请选择收入确认年度">
            <el-option
              v-for="dict in sys_ndgl"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="确认金额(含税)" prop="confirmAmount">
          <el-input v-model="form.confirmAmount" placeholder="请输入确认金额(含税)" />
        </el-form-item>
        <el-form-item label="税后金额" prop="afterTaxAmount">
          <el-input v-model="form.afterTaxAmount" placeholder="请输入税后金额" />
        </el-form-item>
        <el-form-item label="公司收入确认人姓名" prop="companyRevenueConfirmedByName">
          <el-input v-model="form.companyRevenueConfirmedByName" placeholder="请输入公司收入确认人姓名" />
        </el-form-item>
        <el-divider content-position="center">项目审核信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="Plus" @click="handleAddProjectApproval">添加</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" icon="Delete" @click="handleDeleteProjectApproval">删除</el-button>
          </el-col>
        </el-row>
        <el-table :data="projectApprovalList" :row-class-name="rowProjectApprovalIndex" @selection-change="handleProjectApprovalSelectionChange" ref="projectApproval">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="序号" align="center" prop="index" width="50"/>
          <el-table-column label="审核状态" prop="approvalStatus" width="150">
            <template #default="scope">
              <el-select v-model="scope.row.approvalStatus" placeholder="请选择审核状态">
                <el-option
                  v-for="dict in sys_spzt"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="审核人ID" prop="approverId" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.approverId" placeholder="请输入审核人ID" />
            </template>
          </el-table-column>
          <el-table-column label="审核时间" prop="approvalTime" width="240">
            <template #default="scope">
              <el-date-picker clearable
                v-model="scope.row.approvalTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择审核时间">
              </el-date-picker>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="240">
            <template #default="scope">
              <el-date-picker clearable
                v-model="scope.row.createTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择创建时间">
              </el-date-picker>
            </template>
          </el-table-column>
        </el-table>
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
import { listProject, getProject, delProject, addProject, updateProject } from "@/api/project/project"
import request from '@/utils/request'

const { proxy } = getCurrentInstance()
const { sys_xmfl, sys_ndgl, sys_yjqy, sys_spzt, sys_xmjd, sys_yszt } = proxy.useDict('sys_xmfl', 'sys_ndgl', 'sys_yjqy', 'sys_spzt', 'sys_xmjd', 'sys_yszt')

const projectList = ref([])
const projectApprovalList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const checkedProjectApproval = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

// 数据源选项
const deptOptions = ref([])
const projectManagerOptions = ref([])
const marketManagerOptions = ref([])
const secondaryRegionOptions = ref([])
const projectNameList = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    projectDept: null,
    revenueConfirmYear: null,
    projectCategory: null,
    region: null,
    regionCode: null,
    projectManagerId: null,
    marketManagerId: null,
    establishedYear: null,
    projectStatus: null,
    approvalStatus: null,
    acceptanceStatus: null,
  },
  rules: {
    projectCode: [
      { required: true, message: "项目编号不能为空", trigger: "blur" }
    ],
    projectName: [
      { required: true, message: "项目名称不能为空", trigger: "blur" }
    ],
    industry: [
      { required: true, message: "行业不能为空", trigger: "change" }
    ],
    region: [
      { required: true, message: "区域不能为空", trigger: "change" }
    ],
    establishedYear: [
      { required: true, message: "立项年份不能为空", trigger: "change" }
    ],
    projectCategory: [
      { required: true, message: "项目分类不能为空", trigger: "change" }
    ],
    projectDept: [
      { required: true, message: "项目部门不能为空", trigger: "change" }
    ],
    projectStatus: [
      { required: true, message: "项目状态不能为空", trigger: "change" }
    ],
    acceptanceStatus: [
      { required: true, message: "验收状态不能为空", trigger: "change" }
    ],
    estimatedWorkload: [
      { required: true, message: "预估工作量(人天)不能为空", trigger: "blur" }
    ],
    projectAddress: [
      { required: true, message: "项目地址不能为空", trigger: "blur" }
    ],
    projectPlan: [
      { required: true, message: "项目计划不能为空", trigger: "blur" }
    ],
    projectDescription: [
      { required: true, message: "项目描述不能为空", trigger: "blur" }
    ],
    projectManagerId: [
      { required: true, message: "项目经理ID不能为空", trigger: "change" }
    ],
    marketManagerId: [
      { required: true, message: "市场经理ID不能为空", trigger: "change" }
    ],
    participants: [
      { required: true, message: "参与人员ID列表不能为空", trigger: "change" }
    ],
    salesManagerId: [
      { required: true, message: "销售负责人ID不能为空", trigger: "change" }
    ],
    salesContact: [
      { required: true, message: "销售联系方式不能为空", trigger: "blur" }
    ],
    customerId: [
      { required: true, message: "客户ID不能为空", trigger: "change" }
    ],
    customerContactId: [
      { required: true, message: "客户联系人ID不能为空", trigger: "change" }
    ],
    projectBudget: [
      { required: true, message: "项目预算(元)不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 加载部门树 */
function getDeptTree() {
  request({
    url: '/system/dept/treeselect',
    method: 'get'
  }).then(response => {
    deptOptions.value = response.data
  })
}

/** 加载项目经理列表 */
function getProjectManagers() {
  request({
    url: '/system/user/listByPost',
    method: 'get',
    params: { postCode: 'pm' }
  }).then(response => {
    projectManagerOptions.value = response.data
  })
}

/** 加载市场经理列表 */
function getMarketManagers() {
  request({
    url: '/system/user/listByPost',
    method: 'get',
    params: { postCode: 'scjl' }
  }).then(response => {
    marketManagerOptions.value = response.data
  })
}

/** 加载二级区域列表 */
function getSecondaryRegions(regionDictValue) {
  if (!regionDictValue) {
    secondaryRegionOptions.value = []
    return
  }
  request({
    url: '/project/secondaryRegion/listByRegion',
    method: 'get',
    params: { regionDictValue: regionDictValue }
  }).then(response => {
    secondaryRegionOptions.value = response.data || []
  })
}

/** 一级区域变化处理 */
function handleRegionChange(value) {
  queryParams.value.regionCode = null
  getSecondaryRegions(value)
}

/** 项目名称自动补全查询 */
function queryProjectNames(queryString, cb) {
  const results = queryString
    ? projectNameList.value.filter(item => item.value.toLowerCase().includes(queryString.toLowerCase()))
    : projectNameList.value
  cb(results)
}

/** 查询项目管理列表 */
function getList() {
  loading.value = true
  listProject(queryParams.value).then(response => {
    projectList.value = response.rows
    total.value = response.total
    loading.value = false

    // 提取项目名称用于自动补全
    const names = response.rows.map(item => ({ value: item.projectName }))
    projectNameList.value = [...new Set(names.map(item => item.value))].map(name => ({ value: name }))
  })
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
    establishedYear: null,
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
    projectBudget: null,
    projectCost: null,
    expenseBudget: null,
    costBudget: null,
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
    revenueConfirmStatus: null,
    revenueConfirmYear: null,
    confirmAmount: null,
    afterTaxAmount: null,
    companyRevenueConfirmedByName: null
  }
  projectApprovalList.value = []
  proxy.resetForm("projectRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  queryParams.value.regionCode = null
  secondaryRegionOptions.value = []
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

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _projectId = row.projectId || ids.value
  getProject(_projectId).then(response => {
    form.value = response.data
    projectApprovalList.value = response.data.projectApprovalList
    open.value = true
    title.value = "修改项目管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["projectRef"].validate(valid => {
    if (valid) {
      form.value.projectApprovalList = projectApprovalList.value
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

/** 项目审核序号 */
function rowProjectApprovalIndex({ row, rowIndex }) {
  row.index = rowIndex + 1
}

/** 项目审核添加按钮操作 */
function handleAddProjectApproval() {
  let obj = {}
  obj.approvalStatus = ""
  obj.approvalReason = ""
  obj.approverId = ""
  obj.approvalTime = ""
  obj.createTime = ""
  projectApprovalList.value.push(obj)
}

/** 项目审核删除按钮操作 */
function handleDeleteProjectApproval() {
  if (checkedProjectApproval.value.length == 0) {
    proxy.$modal.msgError("请先选择要删除的项目审核数据")
  } else {
    const projectApprovals = projectApprovalList.value
    const checkedProjectApprovals = checkedProjectApproval.value
    projectApprovalList.value = projectApprovals.filter(function(item) {
      return checkedProjectApprovals.indexOf(item.index) == -1
    })
  }
}

/** 复选框选中数据 */
function handleProjectApprovalSelectionChange(selection) {
  checkedProjectApproval.value = selection.map(item => item.index)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('project/project/export', {
    ...queryParams.value
  }, `project_${new Date().getTime()}.xlsx`)
}

// 初始化数据
getDeptTree()
getProjectManagers()
getMarketManagers()
getList()
</script>
