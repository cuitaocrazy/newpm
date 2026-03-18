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
          <el-popover
            v-model:visible="userPickerVisible"
            placement="bottom-start"
            :width="320"
            trigger="click"
          >
            <template #reference>
              <div class="user-picker-trigger">
                <span v-if="!form.userName" class="user-picker-placeholder">请选择人员</span>
                <span v-else class="user-picker-selected">{{ form.userName }}</span>
                <el-icon class="user-picker-arrow"><ArrowDown /></el-icon>
              </div>
            </template>
            <div style="padding: 4px 0">
              <el-input
                v-model="userFilterText"
                placeholder="搜索姓名"
                clearable
                size="small"
                style="margin-bottom: 8px"
              />
              <el-scrollbar max-height="300px">
                <el-tree
                  ref="userTreeRef"
                  :data="userTreeData"
                  node-key="nodeId"
                  :filter-node-method="filterUserNode"
                  :props="{ label: 'label', children: 'children', isLeaf: 'isLeaf' }"
                  highlight-current
                  :current-node-key="form.userId"
                  @node-click="handleUserNodeClick"
                />
              </el-scrollbar>
            </div>
          </el-popover>
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
import { ref, reactive, watch, onMounted } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWhitelist, addWhitelist, removeWhitelist } from '@/api/project/whitelist'
import { getUsersByPost } from '@/api/project/project'
import { handleTree } from '@/utils/ruoyi'
import request from '@/utils/request'

const loading = ref(false)
const total = ref(0)
const whitelistList = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const queryRef = ref()
const formRef = ref()

// 人员树
const userPickerVisible = ref(false)
const userFilterText = ref('')
const userTreeRef = ref()
const userTreeData = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const form = reactive({
  userId: undefined as number | undefined,
  userName: '',
  reason: ''
})

const rules = {
  userId: [{ required: true, message: '请选择人员', trigger: 'change' }],
  reason: [{ required: true, message: '请输入加入原因', trigger: 'blur' }]
}

/** 加载部门+用户树（level3+部门，用户为叶节点） */
async function loadUserTree() {
  if (userTreeData.value.length > 0) return
  const [deptRes, userRes] = await Promise.all([
    request({ url: '/project/project/deptTree', method: 'get' }),
    getUsersByPost(null)
  ])
  const allDepts = deptRes.data || []
  const allUsers = userRes.data || []
  const validDepts = allDepts.filter((d: any) => d.ancestors && d.ancestors.split(',').length >= 3)
  const deptTree = handleTree(
    validDepts.map((d: any) => ({ ...d, nodeId: `d_${d.deptId}`, label: d.deptName })),
    'deptId', 'parentId'
  )
  function appendUsers(nodes: any[]) {
    nodes.forEach(node => {
      const deptUsers = allUsers
        .filter((u: any) => u.deptId === node.deptId)
        .map((u: any) => ({ nodeId: u.userId, label: u.nickName, userId: u.userId, isLeaf: true, isUser: true }))
      node.children = [...(node.children || []), ...deptUsers]
      if (node.children.length > 0) appendUsers(node.children.filter((c: any) => !c.isUser))
    })
  }
  appendUsers(deptTree)
  userTreeData.value = deptTree
}

function filterUserNode(value: string, data: any) {
  if (!value) return true
  return data.label.includes(value)
}

watch(userFilterText, val => userTreeRef.value?.filter(val))

watch(userPickerVisible, async val => {
  if (val) await loadUserTree()
})

function handleUserNodeClick(data: any) {
  if (!data.isUser) return
  form.userId = data.userId
  form.userName = data.label
  userPickerVisible.value = false
  formRef.value?.validateField('userId')
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
  form.userName = ''
  form.reason = ''
  userFilterText.value = ''
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

<style scoped>
.user-picker-trigger {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 32px;
  padding: 0 11px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: #fff;
  cursor: pointer;
  box-sizing: border-box;
  font-size: 14px;
  transition: border-color 0.2s;
}
.user-picker-trigger:hover {
  border-color: var(--el-color-primary);
}
.user-picker-placeholder {
  flex: 1;
  color: var(--el-text-color-placeholder);
}
.user-picker-selected {
  flex: 1;
  color: var(--el-text-color-regular);
}
.user-picker-arrow {
  color: var(--el-text-color-placeholder);
  margin-left: 4px;
}
</style>
