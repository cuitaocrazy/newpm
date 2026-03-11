<template>
  <div class="app-container">
    <h2 style="margin: 0 0 16px 0; font-weight: bold;">任务详情</h2>

    <div v-loading="loading">
      <!-- 一、所属项目 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">一、选择所属项目</span></template>
        <el-row :gutter="20" style="margin-bottom: 12px;">
          <el-col :span="8">
            <el-form-item label="所属机构" label-width="100px">
              <project-dept-select :model-value="parentProjectDept" disabled style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="项目名称" label-width="100px">
              <el-input :model-value="form.parentProjectName" disabled style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <div v-if="selectedProject">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="行业">
              <dict-tag :options="industry" :value="selectedProject.industry" />
            </el-descriptions-item>
            <el-descriptions-item label="一级区域">
              <dict-tag :options="sys_yjqy" :value="selectedProject.region" />
            </el-descriptions-item>
            <el-descriptions-item label="二级区域">{{ selectedProject.regionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="简称">{{ selectedProject.shortName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="立项年份">
              <dict-tag :options="sys_ndgl" :value="selectedProject.establishedYear" />
            </el-descriptions-item>
            <el-descriptions-item label="项目ID">{{ selectedProject.projectCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目名称" :span="3">{{ selectedProject.projectName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目分类">
              <dict-tag :options="sys_xmfl" :value="selectedProject.projectCategory" />
            </el-descriptions-item>
            <el-descriptions-item label="项目部门">{{ getDeptName(selectedProject.projectDept) }}</el-descriptions-item>
            <el-descriptions-item label="预估工作量">{{ selectedProject.estimatedWorkload || 0 }} 人天</el-descriptions-item>
            <el-descriptions-item label="项目状态">
              <dict-tag :options="sys_xmzt" :value="selectedProject.projectStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="验收状态">
              <dict-tag :options="sys_yszt" :value="selectedProject.acceptanceStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="实际人天">
              {{ selectedProject.actualWorkload != null ? parseFloat(selectedProject.actualWorkload).toFixed(3) : '0.000' }} 人天
            </el-descriptions-item>
            <el-descriptions-item label="审核状态">
              <dict-tag :options="sys_spzt" :value="selectedProject.approvalStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="项目预算">{{ formatAmount(selectedProject.projectBudget) }} 元</el-descriptions-item>
            <el-descriptions-item label="合同状态">
              <dict-tag :options="sys_htzt" :value="selectedProject.contractStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="合同名称" :span="2">{{ selectedProject.contractName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="合同编号">{{ selectedProject.contractCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="合同金额">
              {{ selectedProject.contractAmount != null ? formatAmount(selectedProject.contractAmount) + ' 元' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="收入确认年度">
              <dict-tag :options="sys_ndgl" :value="selectedProject.revenueConfirmYear" />
            </el-descriptions-item>
            <el-descriptions-item label="收入确认状态">
              <dict-tag :options="sys_qrzt" :value="selectedProject.revenueConfirmStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="项目经理">{{ selectedProject.projectManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="市场经理">{{ selectedProject.marketManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户名称" :span="2">{{ projectCustomerName || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>

      <!-- 二、任务信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">二、任务信息</span></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务名称">{{ form.projectName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务负责人">{{ form.projectManagerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务编号">{{ form.taskCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务阶段">
            <dict-tag :options="sys_xmjd" :value="form.projectStage" />
          </el-descriptions-item>
          <el-descriptions-item label="总行需求号">{{ form.bankDemandNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="软件中心需求编号">{{ form.softwareDemandNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="产品" :span="2">
            <dict-tag :options="sys_product" :value="form.product" />
          </el-descriptions-item>
          <el-descriptions-item label="预估工作量">
            {{ form.estimatedWorkload != null ? form.estimatedWorkload + ' 人天' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="实际工作量">
            <el-input :model-value="form.actualWorkload != null ? parseFloat(form.actualWorkload).toFixed(3) + ' 人天' : '-'" disabled />
          </el-descriptions-item>
          <el-descriptions-item label="任务预算">
            {{ form.projectBudget != null ? formatAmount(form.projectBudget) + ' 元' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="投产年度">
            <dict-tag :options="sys_ndgl" :value="form.productionYear" />
          </el-descriptions-item>
          <el-descriptions-item label="投产批次">{{ form.batchNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="计划投产日期">
            {{ form.planProductionDate ? form.planProductionDate.substring(0, 10) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="启动日期">{{ form.startDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ form.endDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内部B包日期">{{ form.internalClosureDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="功能测试版本日期">{{ form.functionalTestDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="生产版本日期">{{ form.productionVersionDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            <div class="text-content">{{ form.remark || '-' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 系统信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">系统信息</span></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="创建人">{{ form.taskCreateByName || form.taskCreateBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ form.taskCreateTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新人">{{ form.taskUpdateByName || form.taskUpdateBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ form.taskUpdateTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <div class="form-footer">
      <el-button size="large" @click="goBack">返回</el-button>
    </div>
  </div>
</template>

<script setup name="SubprojectDetail">
import { ref, getCurrentInstance, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject } from '@/api/project/project'
import request from '@/utils/request'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const { industry, sys_yjqy, sys_xmfl, sys_xmzt, sys_yszt, sys_spzt, sys_htzt, sys_ndgl, sys_qrzt, sys_xmjd, sys_product } =
  proxy.useDict('industry', 'sys_yjqy', 'sys_xmfl', 'sys_xmzt', 'sys_yszt', 'sys_spzt', 'sys_htzt', 'sys_ndgl', 'sys_qrzt', 'sys_xmjd', 'sys_product')

const loading = ref(false)
const form = ref({})
const selectedProject = ref(null)
const projectCustomerName = ref('')
const parentProjectDept = ref(null)
const deptFlatList = ref([])

function getDeptName(deptId) {
  if (!deptId) return '-'
  const numId = typeof deptId === 'string' ? parseInt(deptId) : deptId
  const dept = deptFlatList.value.find(d => d.deptId === numId)
  if (!dept) return '-'
  const ancestorIds = dept.ancestors ? dept.ancestors.split(',').filter(id => id && id !== '0') : []
  const pathDepts = []
  if (ancestorIds.length >= 2) {
    for (let i = 2; i < ancestorIds.length; i++) {
      const ancestor = deptFlatList.value.find(d => d.deptId === parseInt(ancestorIds[i]))
      if (ancestor) pathDepts.push(ancestor.deptName)
    }
  }
  pathDepts.push(dept.deptName)
  return pathDepts.join('-')
}

function formatAmount(value) {
  if (value === null || value === undefined || value === '') return '-'
  const num = parseFloat(String(value).replace(/,/g, ''))
  if (isNaN(num)) return '-'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function goBack() {
  router.push({ path: '/task/subproject', query: { parentId: form.value.parentId } })
}

onMounted(async () => {
  request({ url: '/project/project/deptTreeAll', method: 'get' })
    .then(res => { deptFlatList.value = res.data || [] })
    .catch(() => {})

  const projectId = route.params.projectId
  if (!projectId) return
  loading.value = true
  try {
    const res = await getProject(Number(projectId))
    form.value = res.data || {}
    if (form.value.parentId) {
      const parentRes = await getProject(form.value.parentId)
      selectedProject.value = parentRes.data
      parentProjectDept.value = parentRes.data.projectDept ? Number(parentRes.data.projectDept) : null
      if (parentRes.data.customerId) {
        request({ url: `/project/customer/${parentRes.data.customerId}`, method: 'get' })
          .then(r => { projectCustomerName.value = r.data.customerSimpleName || r.data.customerFullName || '' })
          .catch(() => {})
      }
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.app-container { padding-bottom: 80px; }
.text-content { white-space: pre-wrap; word-break: break-all; }
.form-footer {
  position: sticky; bottom: 0; padding: 20px;
  background-color: #fff; border-top: 1px solid #dcdfe6;
  text-align: center; z-index: 10;
}
</style>
