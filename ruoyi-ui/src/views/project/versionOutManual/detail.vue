<template>
  <div class="app-container" v-loading="loading">
    <h2 style="margin: 0 0 12px 0; font-weight: bold;">非批次版本详情</h2>

    <el-card shadow="hover" style="margin-bottom: 15px;">
      <template #header><span style="font-size: 16px; font-weight: bold;">版本信息</span></template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="投产年份">{{ form.productionYear }}</el-descriptions-item>
        <el-descriptions-item label="投产批次号">{{ form.proBatchNo }}</el-descriptions-item>
        <el-descriptions-item label="产品">{{ form.product }}</el-descriptions-item>
        <el-descriptions-item label="子系统">{{ form.sysName }}</el-descriptions-item>
        <el-descriptions-item label="基准版本号">{{ form.baseVersionCode }}</el-descriptions-item>
        <el-descriptions-item label="软件中心任务号">{{ form.manualTaskNo }}</el-descriptions-item>
        <el-descriptions-item label="任务名称" :span="3">{{ form.manualTaskName }}</el-descriptions-item>
        <el-descriptions-item label="版本类型"><dict-tag :options="sys_version_type" :value="form.versionType" /></el-descriptions-item>
        <el-descriptions-item label="出入库版本号"><el-tag type="success">{{ form.outLibVersion }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="版本编号">{{ form.versionCode }}</el-descriptions-item>
        <el-descriptions-item label="升级包初级版本号">{{ form.outVersion }}</el-descriptions-item>
        <el-descriptions-item label="提交人员">{{ form.userName }}</el-descriptions-item>
        <el-descriptions-item label="版本投产日期">{{ form.versionPDate }}</el-descriptions-item>
        <el-descriptions-item label="涉及TWS改造">{{ form.isInvolved === '0' ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="数据库是否修改">{{ form.dbUpdate === '0' ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="接口是否修改">{{ form.usbUpdate === '0' ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="版本说明" :span="3">{{ form.versionDescr }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ form.remarks }}</el-descriptions-item>
        <el-descriptions-item label="创建人员">{{ displayUser(form.createByName, form.createBy) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ form.createTime }}</el-descriptions-item>
        <el-descriptions-item label="修改人员">{{ displayUser(form.updateByName, form.updateBy) }}</el-descriptions-item>
        <el-descriptions-item label="最后修改时间">{{ form.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <div style="text-align:center; margin: 20px 0;">
      <el-button @click="$router.back()">返回</el-button>
    </div>
  </div>
</template>

<script setup name="VersionOutManualDetail">
import { ref, getCurrentInstance, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getVersionOutManual } from '@/api/project/versionOutManual'

const { proxy } = getCurrentInstance()
const route = useRoute()
const { sys_version_type } = proxy.useDict('sys_version_type')

const form = ref({})
const loading = ref(false)

// 创建人/修改人显示：优先解析出的姓名(nick_name或迁移快照)，过滤掉迁移标记 yadapm-migrate
function displayUser(name, by) {
  if (name) return name
  if (!by || by === 'yadapm-migrate') return ''
  return by
}

onMounted(() => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  getVersionOutManual(id).then(res => {
    form.value = res.data || {}
    loading.value = false
  }).catch(() => { loading.value = false })
})
</script>
