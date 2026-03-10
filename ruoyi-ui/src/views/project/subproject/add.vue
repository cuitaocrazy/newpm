<template>
  <div class="app-container">
    <h2 style="margin: 0 0 6px 0; font-weight: bold;">新增任务</h2>

    <el-alert v-if="parentProject" type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>
        所属项目：{{ parentProject.projectName }}（{{ parentProject.projectCode }}）
      </template>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="160px">

      <!-- 一、基本信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">一、基本信息</span></template>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务负责人" prop="projectManagerId">
              <user-select v-model="form.projectManagerId" post-code="pm" placeholder="请选择任务负责人" filterable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务名称" prop="projectName">
              <el-input v-model="form.projectName" placeholder="请输入任务名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务编号" prop="taskCode">
              <el-input v-model="form.taskCode" placeholder="如：01、用户系统（同一主项目下需唯一）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务阶段" prop="projectStage">
              <dict-select v-model="form.projectStage" dict-type="sys_xmjd" placeholder="请选择任务阶段" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预估工作量" prop="estimatedWorkload">
              <el-input v-model="form.estimatedWorkload" placeholder="请输入">
                <template #append>人天</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际工作量">
              <el-input :value="'0.000'" disabled>
                <template #append>人天</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务预算">
              <el-input v-model="form.projectBudget" placeholder="请输入金额">
                <template #append>元</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="投产批次">
              <dict-select v-model="form.productionBatch" dict-type="sys_tcpc" placeholder="请选择投产批次" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="参与人员">
              <div v-if="parentParticipants.length > 0">
                <el-tag v-for="u in parentParticipants" :key="u.userId"
                  type="info" style="margin: 0 4px 4px 0">{{ u.nickName }}</el-tag>
              </div>
              <span v-else style="color: #909399;">主项目暂无参与人员</span>
              <div style="color: #909399; font-size: 12px; margin-top: 4px;">继承自主项目，不可修改</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="任务计划">
              <el-input v-model="form.projectPlan" type="textarea" :rows="3" placeholder="请输入任务计划" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="任务描述">
              <el-input v-model="form.projectDescription" type="textarea" :rows="3" placeholder="请输入任务描述" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 二、时间规划 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">二、时间规划</span></template>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="启动日期" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" placeholder="选择启动日期"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" placeholder="选择结束日期"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="投产时间">
              <el-date-picker v-model="form.productionDate" type="date" placeholder="选填"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内部闭包日期">
              <el-date-picker v-model="form.internalClosureDate" type="date" placeholder="选填"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="功能测试版本日期">
              <el-date-picker v-model="form.functionalTestDate" type="date" placeholder="选填"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 三、备注 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">三、备注</span></template>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注（选填）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>
    </el-form>

    <div class="form-footer">
      <el-button type="primary" size="large" @click="submitForm">保存</el-button>
      <el-button size="large" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script setup name="TaskAdd">
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { addProject, getProject, getUsersByPost } from '@/api/project/project'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const formRef = ref()
const parentProject = ref(null)
const parentParticipants = ref([])

const form = ref({
  taskCode: null, projectName: null, projectManagerId: null, projectStage: null,
  estimatedWorkload: null, projectBudget: null, productionBatch: null,
  projectPlan: null, projectDescription: null,
  startDate: null, endDate: null, productionDate: null,
  internalClosureDate: null, functionalTestDate: null, remark: null
})

const rules = ref({
  taskCode:          [{ required: true, message: '任务编号不能为空（同一主项目下需唯一，用于生成项目编号）', trigger: 'blur' }],
  projectManagerId:  [{ required: true, message: '任务负责人不能为空', trigger: 'change' }],
  projectName:       [{ required: true, message: '任务名称不能为空',   trigger: 'blur'   }],
  projectStage:      [{ required: true, message: '任务阶段不能为空',   trigger: 'change' }],
  estimatedWorkload: [{ required: true, message: '预估工作量不能为空', trigger: 'blur'   }],
  startDate:         [{ required: true, message: '启动日期不能为空',   trigger: 'change' }],
  endDate:           [{ required: true, message: '结束日期不能为空',   trigger: 'change' }]
})

async function loadParentProject(parentId) {
  try {
    const [projectRes, usersRes] = await Promise.all([
      getProject(parentId),
      getUsersByPost()
    ])
    parentProject.value = projectRes.data
    const ids = (projectRes.data.participants || '').split(',').map(Number).filter(Boolean)
    parentParticipants.value = (usersRes.data || []).filter(u => ids.includes(u.userId))
  } catch (e) {
    console.error('加载主项目信息失败', e)
  }
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    const parentId = route.query.parentId ? Number(route.query.parentId) : null
    const p = parentProject.value || {}
    const taskCode = form.value.taskCode
    const projectCode = p.projectCode ? `${p.projectCode}-${taskCode}` : null
    const submitData = {
      ...form.value,
      parentId,
      projectStatus: '0',
      projectLevel: 1,
      projectCode,
      projectDept: p.projectDept || null,
      industry: p.industry || null,
      region: p.region || null,
      regionId: p.regionId || null,
      regionCode: p.regionCode || null,
      shortName: p.shortName || null,
      establishedYear: p.establishedYear || null,
      projectCategory: p.projectCategory || null,
      projectBudget: form.value.projectBudget ? parseFloat(String(form.value.projectBudget).replace(/,/g, '')) : null,
      estimatedWorkload: form.value.estimatedWorkload ? parseFloat(form.value.estimatedWorkload) : null
    }
    addProject(submitData).then(() => {
      proxy.$modal.msgSuccess('新增成功')
      router.push({ path: '/project/subproject', query: { parentId } })
    })
  })
}

function cancel() {
  const parentId = route.query.parentId ? Number(route.query.parentId) : null
  router.push({ path: '/project/subproject', query: { parentId } })
}

onMounted(() => {
  const parentId = route.query.parentId
  if (parentId) loadParentProject(Number(parentId))
})
</script>

<style scoped>
.app-container { padding-bottom: 80px; }
.form-footer {
  position: sticky; bottom: 0; padding: 20px;
  background-color: #fff; border-top: 1px solid #dcdfe6;
  text-align: center; z-index: 10;
}
.form-footer .el-button { min-width: 120px; margin: 0 10px; }
</style>
