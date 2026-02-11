<template>
  <div class="app-container">
    <el-page-header @back="goBack" :title="pageTitle" />

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 左侧：项目详情（60%） -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: bold;">项目详情</span>
          </template>
          <!-- 项目详情将在下一步添加 -->
        </el-card>
      </el-col>

      <!-- 右侧：收入确认表单（40%） -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: bold;">收入确认信息</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
            <!-- 表单字段将在下一步添加 -->

            <el-form-item style="margin-top: 20px;">
              <el-button type="primary" @click="submitForm">保存</el-button>
              <el-button @click="goBack">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="RevenueCompanyDetail">
import { getRevenue, updateRevenue } from "@/api/revenue/company"

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const projectId = ref(route.params.projectId)
const pageTitle = ref("收入确认")

const data = reactive({
  form: {},
  rules: {}
})

const { form, rules } = toRefs(data)

/** 获取详情 */
function getDetail() {
  getRevenue(projectId.value).then(response => {
    form.value = response.data
  })
}

/** 提交表单 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      updateRevenue(form.value).then(response => {
        proxy.$modal.msgSuccess("保存成功")
        goBack()
      })
    }
  })
}

/** 返回 */
function goBack() {
  router.push("/revenue/company")
}

// 初始化
getDetail()
</script>
