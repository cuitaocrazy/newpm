<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="姓名或部门"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
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
          @click="handleAdd"
          v-hasPermi="['project:whitelist:add']"
        >添加</el-button>
      </el-col>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="whitelistList">
      <el-table-column label="姓名" prop="nickName" min-width="120" />
      <el-table-column label="部门" prop="deptName" min-width="150" />
      <el-table-column label="加入原因" prop="reason" min-width="200" show-overflow-tooltip />
      <el-table-column label="添加时间" prop="createTime" width="160" />
      <el-table-column label="操作人" prop="createBy" width="120" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleRemove(row)"
            v-hasPermi="['project:whitelist:remove']"
          >移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加弹框 -->
    <el-dialog title="添加白名单人员" v-model="dialogVisible" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="人员" prop="userId">
          <user-select
            v-model="form.userId"
            post-code="pm"
            placeholder="请选择人员"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="加入原因" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入加入原因（必填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWhitelist, addWhitelist, removeWhitelist } from '@/api/project/whitelist'

const loading = ref(false)
const total = ref(0)
const whitelistList = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const queryRef = ref()
const formRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const form = reactive({
  userId: undefined as number | undefined,
  reason: ''
})

const rules = {
  userId: [{ required: true, message: '请选择人员', trigger: 'change' }],
  reason: [{ required: true, message: '请输入加入原因', trigger: 'blur' }]
}

async function getList() {
  loading.value = true
  try {
    const res = await listWhitelist(queryParams)
    whitelistList.value = res.rows
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryRef.value?.resetFields()
  handleQuery()
}

function handleAdd() {
  form.userId = undefined
  form.reason = ''
  formRef.value?.resetFields()
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await addWhitelist({ userId: form.userId, reason: form.reason })
    ElMessage.success('添加成功')
    dialogVisible.value = false
    getList()
  } finally {
    submitting.value = false
  }
}

function handleRemove(row: any) {
  ElMessageBox.confirm(`确定要将 ${row.nickName} 从白名单中移除吗？`, '确认移除', {
    type: 'warning'
  }).then(async () => {
    await removeWhitelist(row.id)
    ElMessage.success('移除成功')
    getList()
  }).catch(() => {})
}

onMounted(() => {
  getList()
})
</script>
