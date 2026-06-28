<template>
  <div class="app-container" v-loading="loading">
    <h2 style="margin: 0 0 12px 0; font-weight: bold;">
      历史任务快照
      <el-tag type="warning" effect="dark" size="small" style="margin-left: 8px; vertical-align: middle;">历史快照 · 只读</el-tag>
    </h2>
    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 15px;"
      title="本任务数据迁移自旧系统，为历史定格快照，不与现任务库/项目库/字典实时关联。" />

    <template v-if="hasData">
      <!-- 一、任务基本信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">一、任务基本信息</span></template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="软件中心任务号">{{ form.taskNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务名称" :span="2">{{ form.taskName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="产品">{{ form.product || '-' }}</el-descriptions-item>
          <el-descriptions-item label="子任务团队">{{ form.subtaskTeam || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务负责人">{{ form.taskHoldersName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属项目" :span="3">{{ form.parentProjectName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="投产年度">{{ form.productionYear || '-' }}</el-descriptions-item>
          <el-descriptions-item label="投产批次号">{{ form.batchNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="排期状态">
            <dict-tag v-if="isDictVal(form.scheduleStatus)" :options="sys_pqzt" :value="form.scheduleStatus" />
            <span v-else>{{ form.scheduleStatus || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="预估工作量">{{ form.taskManday != null ? form.taskManday + ' 人天' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="立项号">{{ form.lxNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内部子任务号">{{ form.insideSubtaskNo || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 二、需求信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">二、需求信息</span></template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="总行需求号">{{ form.bankDemandNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需求名称" :span="2">{{ form.demandName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需求联系人">{{ form.demandContacts || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ form.contactsTel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系手机">{{ form.contactsMobile || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 三、进度/日期 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">三、进度 / 日期</span></template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="内部B包日期">{{ fmtDate(form.internalClosureDate) }}</el-descriptions-item>
          <el-descriptions-item label="功能测试版本日期">{{ fmtDate(form.functionalTestDate) }}</el-descriptions-item>
          <el-descriptions-item label="生产版本日期">{{ fmtDate(form.productionVersionDate) }}</el-descriptions-item>
          <el-descriptions-item label="实际投产日期">{{ fmtDate(form.actualProductionDate) }}</el-descriptions-item>
          <el-descriptions-item label="投产报告状态">{{ form.productReportStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="投产报告日期">{{ fmtDate(form.productReportDate) }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">{{ form.checkStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ form.reviewerName || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 四、说明 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">四、说明</span></template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="功能点说明"><div class="text-content">{{ form.functionDescription || '-' }}</div></el-descriptions-item>
          <el-descriptions-item label="实施计划"><div class="text-content">{{ form.taskPlan || '-' }}</div></el-descriptions-item>
          <el-descriptions-item label="备注"><div class="text-content">{{ form.remarks || '-' }}</div></el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 系统信息(老) -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">系统信息（旧系统）</span></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="创建人">{{ form.creatorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ form.legacyCreationDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="修改人">{{ form.modifierName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后修改时间">{{ form.legacyModificationDate || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </template>

    <el-empty v-else-if="!loading" description="未找到该历史任务快照" />

    <div style="text-align:center; margin: 20px 0;">
      <el-button @click="$router.back()">返回</el-button>
    </div>
  </div>
</template>

<script setup name="VersionOutTaskSnapshot">
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTaskSnapshot } from '@/api/project/versionOut'

const { proxy } = getCurrentInstance()
const route = useRoute()
const { sys_pqzt } = proxy.useDict('sys_pqzt')

const form = ref({})
const loading = ref(false)
const hasData = computed(() => form.value && Object.keys(form.value).length > 0)

function isDictVal(v) { return v != null && /^\d+$/.test(String(v)) }
function fmtDate(v) { return v ? String(v).substring(0, 10) : '-' }

onMounted(() => {
  const legacyId = route.query.legacyId
  if (!legacyId) return
  loading.value = true
  getTaskSnapshot(legacyId).then(res => {
    form.value = res.data || {}
    loading.value = false
  }).catch(() => { loading.value = false })
})
</script>

<style scoped>
.text-content { white-space: pre-wrap; word-break: break-all; }
</style>
