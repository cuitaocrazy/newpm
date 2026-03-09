<template>
  <div class="app-container">
    <h2 style="margin: 0 0 6px 0; font-weight: bold;">任务详情</h2>

    <el-alert v-if="form.parentProjectName" type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>
        所属主项目：{{ form.parentProjectName }}
      </template>
    </el-alert>

    <div v-loading="loading">
      <el-card shadow="never" class="detail-card">
        <template #header><span class="card-title">基本信息</span></template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="任务编号">
            {{ form.taskCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="任务名称" :span="2">
            {{ form.projectName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="项目编号" :span="3">
            {{ form.projectCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="行业">
            <dict-tag :options="industry" :value="form.industry" />
          </el-descriptions-item>
          <el-descriptions-item label="一级区域">
            <dict-tag :options="sys_yjqy" :value="form.region" />
          </el-descriptions-item>
          <el-descriptions-item label="二级区域">
            {{ form.regionName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="简称">
            {{ form.shortName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="立项年份">
            <dict-tag :options="sys_ndgl" :value="form.establishedYear" />
          </el-descriptions-item>
          <el-descriptions-item label="项目分类">
            <dict-tag :options="sys_xmfl" :value="form.projectCategory" />
          </el-descriptions-item>
          <el-descriptions-item label="项目部门">
            {{ form.projectDeptName || form.projectDept || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="项目状态">
            <dict-tag :options="sys_xmzt" :value="form.projectStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="任务阶段">
            <dict-tag :options="sys_xmjd" :value="form.projectStage" />
          </el-descriptions-item>
          <el-descriptions-item label="验收状态">
            <dict-tag :options="sys_yszt" :value="form.acceptanceStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="预估工作量">
            {{ form.estimatedWorkload != null ? form.estimatedWorkload + ' 人天' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="实际人天">
            {{ form.actualWorkload != null ? parseFloat(form.actualWorkload).toFixed(3) + ' 人天' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="任务预算">
            {{ form.projectBudget != null ? formatAmount(form.projectBudget) + ' 元' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="投产批次">
            <dict-tag :options="sys_tcpc" :value="form.productionBatch" />
          </el-descriptions-item>
          <el-descriptions-item label="项目地址" :span="3">
            {{ form.projectAddress || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="任务计划" :span="3">
            <div class="text-content">{{ form.projectPlan || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="任务描述" :span="3">
            <div class="text-content">{{ form.projectDescription || '-' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 时间规划 -->
      <el-card shadow="never" class="detail-card">
        <template #header><span class="card-title">时间规划</span></template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="启动日期">{{ form.startDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ form.endDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="投产日期">{{ form.productionDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内部闭包日期">{{ form.internalClosureDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="功能测试版本日期">{{ form.functionalTestDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="验收日期">{{ form.acceptanceDate || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 其他信息 -->
      <el-card shadow="never" class="detail-card">
        <template #header><span class="card-title">其他信息</span></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="备注" :span="2">
            <div class="text-content">{{ form.remark || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="创建人">{{ form.createBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ form.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新人">{{ form.updateBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ form.updateTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <!-- 底部操作按钮 -->
    <div class="form-footer">
      <el-button size="large" @click="goBack">返回</el-button>
    </div>
  </div>
</template>

<script setup name="SubprojectDetail">
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject } from '@/api/project/project'
import request from '@/utils/request'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const { industry, sys_xmfl, sys_ndgl, sys_yjqy, sys_xmjd, sys_yszt, sys_xmzt, sys_tcpc } = proxy.useDict('industry', 'sys_xmfl', 'sys_ndgl', 'sys_yjqy', 'sys_xmjd', 'sys_yszt', 'sys_xmzt', 'sys_tcpc')

const loading = ref(false)
const form = ref({})
const participantNames = ref([])

function formatAmount(value) {
  if (value === null || value === undefined || value === '') return '-'
  const num = parseFloat(String(value).replace(/,/g, ''))
  if (isNaN(num)) return '-'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadSubproject(projectId) {
  loading.value = true
  try {
    const res = await getProject(projectId)
    form.value = res.data || {}
    // Load participant names
    if (form.value.participants) {
      const ids = form.value.participants.split(',').filter(Boolean)
      if (ids.length > 0) {
        request({ url: '/project/project/users', method: 'get' }).then(r => {
          const allUsers = r.data || []
          const idSet = new Set(ids.map(Number))
          participantNames.value = allUsers
            .filter(u => idSet.has(u.userId))
            .map(u => u.nickName || u.userName)
        })
      }
    }
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push({ path: '/project/subproject', query: { parentId: form.value.parentId } })
}

onMounted(() => {
  const projectId = route.params.projectId
  if (projectId) loadSubproject(Number(projectId))
})
</script>

<style scoped>
.app-container { padding-bottom: 80px; }
.detail-card { margin-top: 16px; }
.card-title { font-size: 15px; font-weight: bold; }
.text-content { white-space: pre-wrap; word-break: break-all; }
.form-footer {
  position: sticky; bottom: 0; padding: 20px;
  background-color: #fff; border-top: 1px solid #dcdfe6;
  text-align: center; z-index: 10;
}
</style>
