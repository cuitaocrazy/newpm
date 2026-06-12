<template>
  <div class="app-container" v-loading="loading">
    <h2 style="margin: 0 0 12px 0; font-weight: bold;">批次版本详情</h2>

    <el-card shadow="hover" style="margin-bottom: 15px;">
      <template #header><span style="font-size: 16px; font-weight: bold;">版本信息</span></template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="出入库版本号">
          <el-tag type="success">{{ form.outLibVersion }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="版本编号">{{ form.versionCode }}</el-descriptions-item>
        <el-descriptions-item label="版本类型"><dict-tag :options="sys_version_type" :value="form.versionType" /></el-descriptions-item>
        <el-descriptions-item label="投产年份">{{ form.productionYear }}</el-descriptions-item>
        <el-descriptions-item label="投产批次号">{{ form.proBatchNo }}</el-descriptions-item>
        <el-descriptions-item label="版本投产日期">{{ form.versionPDate }}</el-descriptions-item>
        <el-descriptions-item label="产品">{{ form.product }}</el-descriptions-item>
        <el-descriptions-item label="子系统">{{ form.sysName }}</el-descriptions-item>
        <el-descriptions-item label="基准版本号">{{ form.baseVersionCode }}</el-descriptions-item>
        <el-descriptions-item label="升级包初级版本号">{{ form.outVersion }}</el-descriptions-item>
        <el-descriptions-item label="组包方式"><dict-tag :options="sys_package_mode" :value="form.packageMode" /></el-descriptions-item>
        <el-descriptions-item label="版本状态"><dict-tag :options="sys_version_status" :value="form.versionStatus" /></el-descriptions-item>
        <el-descriptions-item label="提交人员">{{ form.userName }}</el-descriptions-item>
        <el-descriptions-item label="涉及TWS改造">{{ form.isInvolved === '0' ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="数据库是否修改">{{ form.dbUpdate === '0' ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="接口是否修改">{{ form.usbUpdate === '0' ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="创建人员">{{ form.createByName || form.createBy }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ form.createTime }}</el-descriptions-item>
        <el-descriptions-item label="修改人员">{{ form.updateByName || form.updateBy }}</el-descriptions-item>
        <el-descriptions-item label="最后修改时间">{{ form.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="版本简介" :span="3">{{ form.versionBrief }}</el-descriptions-item>
        <el-descriptions-item label="版本说明" :span="3">{{ form.versionDescr }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ form.remarks }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="hover" style="margin-bottom: 15px;">
      <template #header><span style="font-size: 16px; font-weight: bold;">关联任务</span></template>
      <el-table :data="form.taskList || []" border size="small">
        <el-table-column label="软件中心任务号" prop="taskNo" min-width="160" />
        <el-table-column label="任务名称" prop="taskName" min-width="200" show-overflow-tooltip />
        <el-table-column label="项目名称" prop="prjName" min-width="200" show-overflow-tooltip />
        <el-table-column label="需求名称" prop="demandName" min-width="160" show-overflow-tooltip />
        <template #empty>暂无关联任务</template>
      </el-table>
    </el-card>

    <div style="text-align:center; margin: 20px 0;">
      <el-button @click="$router.back()">返回</el-button>
    </div>
  </div>
</template>

<script setup name="VersionOutDetail">
import { ref, getCurrentInstance, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getVersionOut } from '@/api/project/versionOut'

const { proxy } = getCurrentInstance()
const route = useRoute()
const { sys_version_type, sys_package_mode, sys_version_status } =
  proxy.useDict('sys_version_type', 'sys_package_mode', 'sys_version_status')

const form = ref({})
const loading = ref(false)

onMounted(() => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  getVersionOut(id).then(res => {
    form.value = res.data || {}
    loading.value = false
  }).catch(() => { loading.value = false })
})
</script>
