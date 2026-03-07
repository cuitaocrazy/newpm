<template>
  <div class="app-container revenue-team-container">
    <!-- 查询表单 -->
    <el-form :model="queryParams" ref="queryRef" v-show="showSearch" label-width="120px">
      <el-row :gutter="10">
        <el-col :span="6">
          <el-form-item label="项目名称" prop="projectName">
            <el-autocomplete
              v-model="queryParams.projectName"
              :fetch-suggestions="remoteSearchProjects"
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
          <el-form-item label="确认团队" prop="confirmDeptId">
            <el-tree-select
              v-model="queryParams.confirmDeptId"
              :data="confirmDeptTree"
              :props="{ label: 'label', value: 'value', children: 'children' }"
              placeholder="请选择确认团队"
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
      </el-row>
      <el-row :gutter="10">
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
      </el-row>
      <el-row :gutter="10">
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
            <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['revenue:team:export']">导出</el-button>
            <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['revenue:team:add']">新增</el-button>
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
      <right-toolbar v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 列表表格 -->
    <el-table v-loading="loading" :data="teamRevenueList" :height="tableHeight" :row-class-name="tableRowClassName" border>
      <el-table-column label="序号" width="55" align="center" fixed="left">
        <template #default="scope">
          <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">合计</span>
          <span v-else>{{ scope.$index }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[0].visible" label="项目名称" align="left" header-align="center" prop="projectName" min-width="220" fixed="left">
        <template #default="scope">
          <el-link
            v-if="isDataRow(scope.row)"
            type="primary"
            class="project-name-cell"
            @click="handleDetail(scope.row)"
          >{{ scope.row.projectName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[1].visible" label="项目阶段" align="center" prop="projectStage" min-width="100">
        <template #default="scope">
          <dict-tag v-if="isDataRow(scope.row)" :options="sys_xmjd" :value="scope.row.projectStage"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[2].visible" label="项目部门" align="center" prop="projectDept" min-width="120" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="isDataRow(scope.row)">{{ getDeptName(scope.row.projectDept) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[3].visible" label="项目经理" align="center" prop="projectManagerName" min-width="100">
        <template #default="scope">
          <span v-if="isDataRow(scope.row)">{{ scope.row.projectManagerName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[4].visible" label="项目分类" align="center" prop="projectCategory" min-width="120">
        <template #default="scope">
          <dict-tag v-if="isDataRow(scope.row)" :options="sys_xmfl" :value="scope.row.projectCategory"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[5].visible" label="二级区域" align="center" prop="regionName" min-width="120" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="isDataRow(scope.row)">{{ scope.row.regionName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[6].visible" label="项目预算(元)" align="right" prop="projectBudget" min-width="130">
        <template #default="scope">
          <span :style="scope.row.isSummaryRow ? 'font-weight: bold; color: #409EFF;' : ''">{{ formatAmount(scope.row.projectBudget) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[7].visible" label="预估工作量（人天）" align="center" prop="estimatedWorkload" min-width="130">
        <template #default="scope">
          <span :style="scope.row.isSummaryRow ? 'font-weight: bold; color: #409EFF;' : ''">{{ formatWorkload(scope.row.estimatedWorkload) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[8].visible" label="实际人天" align="center" prop="actualWorkload" min-width="100">
        <template #default="scope">
          <span :style="scope.row.isSummaryRow ? 'font-weight: bold; color: #409EFF;' : ''">{{ formatWorkload(scope.row.actualWorkload) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[9].visible" label="合同名称" align="left" header-align="center" prop="contractName" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="isDataRow(scope.row)" style="white-space: pre-line;">{{ scope.row.contractName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[10].visible" label="合同金额(元)" align="right" prop="contractAmount" min-width="130">
        <template #default="scope">
          <span :style="scope.row.isSummaryRow ? 'font-weight: bold; color: #409EFF;' : ''">{{ formatAmount(scope.row.contractAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[11].visible" label="合同状态" align="center" prop="contractStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="isDataRow(scope.row)" :options="sys_htzt" :value="scope.row.contractStatus"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[12].visible" label="收入确认年度" align="center" prop="revenueConfirmYear" min-width="120">
        <template #default="scope">
          <span v-if="isDataRow(scope.row)">{{ scope.row.revenueConfirmYear }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[13].visible" label="收入确认状态" align="center" prop="revenueConfirmStatus" min-width="120">
        <template #default="scope">
          <dict-tag v-if="isDataRow(scope.row)" :options="sys_qrzt" :value="scope.row.revenueConfirmStatus"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[14].visible" label="确认金额(元)" align="right" prop="confirmAmount" min-width="130">
        <template #default="scope">
          <span :style="scope.row.isSummaryRow ? 'font-weight: bold; color: #409EFF;' : ''">{{ formatAmount(scope.row.confirmAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[15].visible" label="确认团队" align="center" prop="teamDeptName" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="isDataRow(scope.row)">{{ scope.row.teamDeptName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[16].visible" label="团队确认收入(元)" align="right" prop="teamConfirmAmount" min-width="150">
        <template #default="scope">
          <span :style="scope.row.isSummaryRow ? 'font-weight: bold; color: #67C23A;' : 'color: #67C23A;'">{{ formatAmount(scope.row.teamConfirmAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[17].visible" label="参与人员" align="center" prop="participants" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="isDataRow(scope.row)">{{ getParticipantsNames(scope.row.participants) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[18].visible" label="启动日期" align="center" prop="startDate" width="100">
        <template #default="scope">
          <span v-if="isDataRow(scope.row)">{{ scope.row.startDate }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[19].visible" label="结束日期" align="center" prop="endDate" width="100">
        <template #default="scope">
          <span v-if="isDataRow(scope.row)">{{ scope.row.endDate }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[20].visible" label="验收日期" align="center" prop="acceptanceDate" width="100">
        <template #default="scope">
          <span v-if="isDataRow(scope.row)">{{ scope.row.acceptanceDate }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[21].visible" label="审核状态" align="center" prop="approvalStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="isDataRow(scope.row)" :options="sys_spzt" :value="scope.row.approvalStatus"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[22].visible" label="项目状态" align="center" prop="projectStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="isDataRow(scope.row)" :options="sys_xmzt" :value="scope.row.projectStatus"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[23].visible" label="验收状态" align="center" prop="acceptanceStatus" min-width="100">
        <template #default="scope">
          <dict-tag v-if="isDataRow(scope.row)" :options="sys_yszt" :value="scope.row.acceptanceStatus"/>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[24].visible" label="更新人" align="center" prop="updateByName" min-width="100">
        <template #default="scope">
          <span v-if="isDataRow(scope.row)">{{ scope.row.updateByName }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="columns[25].visible" label="更新时间" align="center" prop="updateTime" width="160">
        <template #default="scope">
          <span v-if="isDataRow(scope.row)">{{ scope.row.updateTime }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120" fixed="right">
        <template #default="scope">
          <template v-if="isDataRow(scope.row)">
            <el-button
              link
              type="primary"
              icon="View"
              @click="handleDetail(scope.row)"
              v-hasPermi="['revenue:team:query']"
            >查看</el-button>
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['revenue:team:edit']"
            >编辑</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" v-model="open" width="960px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <!-- 项目（全宽） -->
        <el-row>
          <el-col :span="24">
            <el-form-item label="项目" prop="projectId">
              <project-select
                ref="projectSelectRef"
                v-model="form.projectId"
                @change="handleProjectChange"
                placeholder="请选择项目"
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 预算金额 | 合同金额 -->
        <el-row>
          <el-col :span="12">
            <el-form-item label="预算金额(元)">
              <el-input :model-value="formatAmountDisplay(form.projectBudget)" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同金额(元)">
              <el-input :model-value="formatAmountDisplay(form.contractAmount)" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 公司确认金额 | 确认年度 -->
        <el-row>
          <el-col :span="12">
            <el-form-item label="公司确认金额(元)">
              <el-input :model-value="formatAmountDisplay(form.confirmAmount)" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认年度">
              <el-input :model-value="form.revenueConfirmYear ? form.revenueConfirmYear + '年' : '-'" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 计划参与人员 | 实际参与人员 -->
        <el-row>
          <el-col :span="12">
            <el-form-item label="计划参与人员">
              <span class="participants-text">{{ form.participantsNames || '暂无' }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际参与人员">
              <span class="participants-text">暂无</span>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 参与人员工时（折叠） -->
        <div class="worktime-section">
          <div class="worktime-header" @click="worktimeExpanded = !worktimeExpanded">
            <span class="worktime-title">参与人员工时</span>
            <span class="worktime-stats">
              <span class="stat-people">{{ participantCount }} 人</span>
              <span class="stat-hours">总工时 0.0 小时</span>
              <span>合计 {{ form.actualWorkload != null ? parseFloat(form.actualWorkload).toFixed(3) : '0.000' }} 人天</span>
            </span>
            <el-icon :class="['worktime-arrow', { expanded: worktimeExpanded }]"><ArrowDown /></el-icon>
          </div>
          <div v-show="worktimeExpanded" class="worktime-body">
            <div class="worktime-empty">
              <el-empty description="暂无工时记录" :image-size="60" />
            </div>
          </div>
        </div>

        <!-- 确认明细标题行 -->
        <div class="detail-header">
          <span class="detail-header-label">确认明细</span>
          <el-button type="primary" size="small" @click="handleAddDetail">+ 添加确认记录</el-button>
        </div>

        <el-table :data="form.detailList" border style="width: 100%">
          <el-table-column label="序号" type="index" width="55" align="center" />
          <el-table-column label="部门" prop="deptId" min-width="180">
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
          <el-table-column label="确认金额(元)" prop="confirmAmount" width="160">
            <template #default="scope">
              <el-input-number
                v-model="scope.row.confirmAmount"
                :precision="2"
                :min="0"
                controls-position="right"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="确认人" prop="confirmUserName" width="100" align="center">
            <template #default="scope">
              <span class="confirm-user-name">{{ scope.row.confirmUserName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="150">
            <template #default="scope">
              <el-input v-model="scope.row.remark" placeholder="如：预付款、中期款、尾款等" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="scope">
              <el-button link type="danger" icon="Delete" @click="handleDeleteDetail(scope.$index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 合计金额 -->
        <div class="total-amount-row">
          合计金额：¥ {{ totalAmount }}
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="RevenueTeam">
import { ref, reactive, toRefs, getCurrentInstance, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { listTeamRevenue, getTeamRevenueSummary, getTeamRevenue, getProjectInfo, addTeamRevenue, updateTeamRevenue, delTeamRevenue, exportTeamRevenue } from "@/api/revenue/team"
import { getDeptTree, searchProjects , listProjectByName} from "@/api/project/project"
import { handleTree } from '@/utils/ruoyi'
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const { proxy } = getCurrentInstance()
const userStore = useUserStore()

const { sys_xmfl, sys_yjqy, sys_xmjd, sys_spzt, sys_yszt, sys_qrzt, sys_ndgl, sys_htzt, sys_xmzt } = proxy.useDict(
  'sys_xmfl', 'sys_yjqy', 'sys_xmjd', 'sys_spzt', 'sys_yszt', 'sys_qrzt', 'sys_ndgl', 'sys_htzt', 'sys_xmzt'
)

const teamRevenueList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const tableHeight = ref(600)
const open = ref(false)
const title = ref("")

const deptTree = ref([])
const deptFlatList = ref([])
const confirmDeptTree = ref([])     // 确认团队树形选项（三级及以下部门）
const projectManagerSelectRef = ref(null)
const marketManagerSelectRef = ref(null)
const allUsersSelectRef = ref(null)
const hiddenAllUsersValue = ref(null)
const projectSelectRef = ref(null)
const worktimeExpanded = ref(false)

// 显隐列配置
const columns = ref([
  { key: 0,  label: '项目名称',          visible: true  },
  { key: 1,  label: '项目阶段',          visible: true  },
  { key: 2,  label: '项目部门',          visible: true  },
  { key: 3,  label: '项目经理',          visible: true  },
  { key: 4,  label: '项目分类',          visible: true  },
  { key: 5,  label: '二级区域',          visible: true  },
  { key: 6,  label: '项目预算(元)',      visible: true  },
  { key: 7,  label: '预估工作量（人天）', visible: true  },
  { key: 8,  label: '实际人天',          visible: true  },
  { key: 9,  label: '合同名称',          visible: true  },
  { key: 10, label: '合同金额(元)',      visible: true  },
  { key: 11, label: '合同状态',          visible: true  },
  { key: 12, label: '收入确认年度',      visible: true  },
  { key: 13, label: '收入确认状态',      visible: true  },
  { key: 14, label: '确认金额(元)',      visible: true  },
  { key: 15, label: '确认团队',          visible: true  },
  { key: 16, label: '团队确认收入(元)',  visible: true  },
  { key: 17, label: '参与人员',          visible: true  },
  { key: 18, label: '启动日期',          visible: true  },
  { key: 19, label: '结束日期',          visible: true  },
  { key: 20, label: '验收日期',          visible: true  },
  { key: 21, label: '审核状态',          visible: true  },
  { key: 22, label: '项目状态',          visible: true  },
  { key: 23, label: '验收状态',          visible: true  },
  { key: 24, label: '更新人',            visible: true  },
  { key: 25, label: '更新时间',          visible: true  },
])

const data = reactive({
  form: {
    projectId: null,
    projectName: null,
    projectBudget: null,
    contractAmount: null,
    confirmAmount: null,
    revenueConfirmYear: null,
    participantsNames: null,
    participants: null,
    actualWorkload: null,
    detailList: []
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    projectDept: null,
    confirmDeptId: null,
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
  },
  rules: {
    projectId: [
      { required: true, message: "请选择项目", trigger: "change" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

// 计算弹窗明细合计金额
const totalAmount = computed(() => {
  if (!form.value.detailList || form.value.detailList.length === 0) {
    return '0.00'
  }
  const t = form.value.detailList.reduce((sum, item) => {
    return sum + (parseFloat(item.confirmAmount) || 0)
  }, 0)
  return t.toFixed(2)
})

/** 对话框标题（动态拼入项目名） */
const dialogTitle = computed(() => {
  if (form.value.projectName) return `项目【${form.value.projectName}】收入确认管理`
  return '团队收入确认管理'
})

/** 计划参与人员数量 */
const participantCount = computed(() => {
  if (!form.value.participants) return 0
  const ids = typeof form.value.participants === 'string'
    ? form.value.participants.split(',').filter(id => id.trim())
    : (Array.isArray(form.value.participants) ? form.value.participants : [])
  return ids.length
})

/** 格式化金额为千分位，保留2位小数 */
function formatAmount(amount) {
  if (amount === null || amount === undefined || amount === '') return '0.00'
  const num = parseFloat(amount)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 对话框禁用 input 的金额展示（含 ¥ 前缀） */
function formatAmountDisplay(amount) {
  if (amount === null || amount === undefined || amount === '') return '-'
  const num = parseFloat(amount)
  if (isNaN(num)) return '-'
  return '¥' + num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 格式化工作量（取整，不显示小数） */
function formatWorkload(value) {
  if (value === null || value === undefined || value === '') return '-'
  const num = parseFloat(value)
  return isNaN(num) ? '-' : num.toFixed(3)
}

/** 判断是否为普通数据行（排除合计行） */
function isDataRow(row) {
  return !row.isSummaryRow
}

/** 查询列表（列表 + 全量合计并行请求） */
function getList() {
  loading.value = true
  const params = queryParams.value
  Promise.all([
    listTeamRevenue(params),
    getTeamRevenueSummary(params)
  ]).then(([listRes, summaryRes]) => {
    const rows = listRes.rows || []
    total.value = listRes.total
    if (rows.length > 0) {
      const s = summaryRes.data || {}
      const summaryRow = {
        isSummaryRow: true,
        projectBudget: Number(s.projectBudget || 0).toFixed(2),
        estimatedWorkload: Number(s.estimatedWorkload || 0),
        actualWorkload: Number(s.actualWorkload || 0).toFixed(3),
        contractAmount: Number(s.contractAmount || 0).toFixed(2),
        confirmAmount: Number(s.confirmAmount || 0).toFixed(2),
        teamConfirmAmount: Number(s.teamConfirmAmount || 0).toFixed(2),
      }
      teamRevenueList.value = [summaryRow, ...rows]
    } else {
      teamRevenueList.value = []
    }
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 表格行样式 */
function tableRowClassName({ row }) {
  return row.isSummaryRow ? 'summary-row' : ''
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

/** 项目名称远程搜索 */
function remoteSearchProjects(query, callback) {
  searchProjects(query || '').then(response => {
    callback((response.data || []).map(p => ({ value: p.projectName })))
  }).catch(() => callback([]))
}

/** 加载部门树 */
function loadDeptTree() {
  getDeptTree().then(response => {
    deptFlatList.value = response.data

    const level3AndBelowDepts = response.data.filter(dept => {
      if (!dept.ancestors) return false
      const level = dept.ancestors.split(',').length
      return level >= 3
    })

    const deptData = level3AndBelowDepts.map(dept => ({
      ...dept,
      value: dept.deptId,
      label: dept.deptName
    }))

    const builtTree = handleTree(deptData, 'deptId', 'parentId', 'children')
    deptTree.value = builtTree
    // 确认团队树：首位插入"无确认团队"虚拟节点
    confirmDeptTree.value = [
      { label: '无确认团队', value: -1, children: [] },
      ...builtTree
    ]
  })
}

/** 根据部门ID获取部门名称（显示第三级及以下机构的完整路径） */
function getDeptName(deptId) {
  if (!deptId) return '-'
  const numDeptId = typeof deptId === 'string' ? parseInt(deptId) : deptId
  const dept = deptFlatList.value.find(d => d.deptId === numDeptId)
  if (!dept) return '-'

  const ancestorIds = dept.ancestors ? dept.ancestors.split(',').filter(id => id && id !== '0') : []
  const pathDepts = []
  if (ancestorIds.length >= 2) {
    for (let i = 2; i < ancestorIds.length; i++) {
      const ancestorDept = deptFlatList.value.find(d => d.deptId === parseInt(ancestorIds[i]))
      if (ancestorDept) pathDepts.push(ancestorDept.deptName)
    }
  }
  pathDepts.push(dept.deptName)
  return pathDepts.length > 0 ? pathDepts.join('-') : dept.deptName
}

/** 根据用户ID获取用户名称 */
function getUserName(userId, userList) {
  if (!userId) return '-'
  const list = userList?.value || userList || []
  const user = list.find(u => u.userId === userId)
  return user ? user.nickName : '-'
}

/** 根据参与人员ID列表获取名称（逗号分隔） */
function getParticipantsNames(participants) {
  if (!participants) return '-'
  const participantIds = typeof participants === 'string'
    ? participants.split(',').map(id => parseInt(id.trim()))
    : participants
  if (!participantIds || participantIds.length === 0) return '-'
  const allUsers = allUsersSelectRef.value?.userOptions || []
  if (allUsers.length === 0) return '-'
  const names = participantIds
    .map(id => {
      const user = allUsers.find(u => u.userId === id)
      return user ? user.nickName : null
    })
    .filter(name => name !== null)
  return names.length > 0 ? names.join(', ') : '-'
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
      projectName: project.projectName,
      projectBudget: project.projectBudget,
      contractAmount: project.contractAmount,
      confirmAmount: project.confirmAmount,
      revenueConfirmYear: project.revenueConfirmYear,
      participantsNames: project.participantsNames,
      participants: project.participants,
      actualWorkload: project.actualWorkload,
      detailList: detailList.map(item => ({
        teamConfirmId: item.teamConfirmId,
        deptId: item.deptId,
        confirmAmount: item.confirmAmount,
        confirmUserId: item.confirmUserId,
        confirmUserName: item.confirmUserName,
        remark: item.remark
      }))
    }

    open.value = true
    title.value = "修改团队收入确认"

    nextTick(() => {
      if (projectSelectRef.value && project.projectId && project.projectName) {
        projectSelectRef.value.projectOptions.push({
          projectId: project.projectId,
          projectName: project.projectName
        })
      }
    })
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
    form.value.projectName = response.data.projectName
    form.value.participantsNames = response.data.participantsNames
    form.value.participants = response.data.participants
    form.value.actualWorkload = response.data.actualWorkload
  })
}

/** 添加明细行 */
function handleAddDetail() {
  form.value.detailList.push({
    deptId: null,
    confirmAmount: 0,
    confirmUserId: userStore.id,
    confirmUserName: userStore.nickName,
    remark: ''
  })
}

/** 删除明细行 */
function handleDeleteDetail(index) {
  form.value.detailList.splice(index, 1)
  calculateTotalAmount()
}

/** 计算合计金额（由 computed totalAmount 自动处理，此处保留以兼容 @change） */
function calculateTotalAmount() {}

/** 表单重置 */
function reset() {
  form.value = {
    projectId: null,
    projectName: null,
    projectBudget: null,
    contractAmount: null,
    confirmAmount: null,
    revenueConfirmYear: null,
    participantsNames: null,
    participants: null,
    actualWorkload: null,
    detailList: []
  }
  worktimeExpanded.value = false
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

      const submitData = {
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
        updateTeamRevenue(submitData).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addTeamRevenue(submitData).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
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

onMounted(() => {
  calcTableHeight()
  window.addEventListener('resize', calcTableHeight)
})

onUnmounted(() => {
  window.removeEventListener('resize', calcTableHeight)
})

watch(showSearch, () => {
  calcTableHeight()
})

// 初始化
loadDeptTree()
getList()
</script>

<style scoped lang="scss">
.revenue-team-container {
  height: 100%;
  display: flex;
  flex-direction: column;

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

  .project-name-cell {
    word-break: break-all;
    white-space: normal;
    line-height: 1.5;
    text-align: left;
  }
}

::v-deep .summary-row {
  background-color: #f5f7fa;
  font-weight: bold;
}

/* 弹窗内部样式 */
.participants-text {
  font-size: 14px;
  color: #606266;
}

.worktime-section {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  margin-bottom: 16px;
}
.worktime-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  background: #f5f7fa;
  cursor: pointer;
  user-select: none;
  border-radius: 4px;
}
.worktime-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  flex-shrink: 0;
}
.worktime-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #606266;
  flex: 1;
}
.stat-people {
  color: #409eff;
  font-weight: 600;
}
.stat-hours {
  color: #e6a23c;
  font-weight: 600;
}
.worktime-arrow {
  margin-left: auto;
  transition: transform 0.2s;
  flex-shrink: 0;
}
.worktime-arrow.expanded {
  transform: rotate(180deg);
}
.worktime-body {
  padding: 0 14px 10px;
}
.worktime-empty {
  display: flex;
  justify-content: center;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.detail-header-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.confirm-user-name {
  color: #67c23a;
  font-weight: 500;
}

.total-amount-row {
  margin-top: 12px;
  text-align: right;
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}
</style>
