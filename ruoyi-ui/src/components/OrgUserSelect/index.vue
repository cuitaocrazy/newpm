<template>
  <div class="org-user-select" :style="{ width }">
    <el-popover
      v-model:visible="visible"
      placement="bottom-start"
      :width="popoverWidth"
      trigger="click"
      :disabled="disabled"
      @hide="onHide"
    >
      <!-- 触发器 -->
      <template #reference>
        <div
          class="org-user-select__trigger"
          :class="{ 'is-disabled': disabled }"
        >
          <span v-if="selectedUsers.length === 0" class="placeholder">{{ placeholder }}</span>
          <span v-else class="selected-text">
            已选 {{ selectedUsers.length }} 人：{{ displayNames }}
          </span>
          <el-icon class="arrow-icon"><ArrowDown /></el-icon>
        </div>
      </template>

      <!-- 弹出内容 -->
      <div class="org-user-select__panel">
        <el-input
          v-model="filterText"
          placeholder="搜索姓名"
          clearable
          size="small"
          style="margin-bottom: 8px"
        />
        <el-scrollbar max-height="320px">
          <el-tree
            ref="treeRef"
            :data="treeData"
            node-key="nodeId"
            show-checkbox
            :check-strictly="false"
            :filter-node-method="filterNode"
            :default-checked-keys="checkedKeys"
            :props="{ label: 'label', children: 'children', isLeaf: 'isLeaf' }"
            @check="handleCheck"
          />
        </el-scrollbar>
        <div class="panel-footer">
          <el-button link size="small" @click="clearAll">清空</el-button>
          <span class="count-text">已选 {{ selectedUsers.length }} 人</span>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup name="OrgUserSelect">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getUsersByPost } from '@/api/project/project'
import { handleTree } from '@/utils/ruoyi'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '请选择参与人员'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  width: {
    type: String,
    default: '100%'
  },
  popoverWidth: {
    type: Number,
    default: 320
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'blur'])

const visible = ref(false)
const filterText = ref('')
const treeRef = ref(null)
const treeData = ref([])
// 所有用户的扁平 map，用于回显
const userMap = ref({})

// 当前选中的用户 ID 列表（同步自 modelValue）
const checkedKeys = computed(() => props.modelValue || [])

// 选中的用户对象列表
const selectedUsers = computed(() => {
  return (props.modelValue || []).map(id => userMap.value[id]).filter(Boolean)
})

// 展示文本（最多显示 3 个，超出显示 +N）
const displayNames = computed(() => {
  const names = selectedUsers.value.map(u => u.nickName)
  if (names.length <= 3) return names.join('、')
  return names.slice(0, 3).join('、') + ` 等${names.length}人`
})

/** 过滤节点 */
function filterNode(value, data) {
  if (!value) return true
  return data.label.includes(value)
}

/** 构建组织机构树（部门 + 用户） */
async function loadData() {
  const [deptRes, userRes] = await Promise.all([
    request({ url: '/project/project/deptTree', method: 'get' }),
    getUsersByPost(null)
  ])

  const allDepts = deptRes.data || []
  const allUsers = userRes.data || []

  // 建立 userId → user 的 map，用于回显
  allUsers.forEach(u => {
    userMap.value[u.userId] = u
  })

  // 只取 level3+ 的部门（与项目列表保持一致）
  const validDepts = allDepts.filter(d => d.ancestors && d.ancestors.split(',').length >= 3)

  // 先用 handleTree 建立部门层级
  const deptTree = handleTree(
    validDepts.map(d => ({ ...d, nodeId: `d_${d.deptId}` })),
    'deptId',
    'parentId'
  )

  // 递归地将用户追加到对应部门的 children 末尾
  function appendUsers(nodes) {
    nodes.forEach(node => {
      const deptUsers = allUsers
        .filter(u => u.deptId === node.deptId)
        .map(u => ({
          nodeId: u.userId,
          label: u.nickName,
          userId: u.userId,
          isLeaf: true,
          isUser: true
        }))
      const subDepts = node.children || []
      node.children = [...subDepts, ...deptUsers]
      if (subDepts.length > 0) appendUsers(subDepts)
    })
  }

  appendUsers(deptTree)
  treeData.value = deptTree
}

/** tree check 事件：只取用户叶节点 */
function handleCheck() {
  const leafChecked = treeRef.value.getCheckedNodes(true) // leafOnly=true
  const userIds = leafChecked.filter(n => n.isUser).map(n => n.userId)
  emit('update:modelValue', userIds)
  emit('change', userIds)
}

/** 清空 */
function clearAll() {
  treeRef.value?.setCheckedKeys([])
  emit('update:modelValue', [])
  emit('change', [])
}

/** 弹出层关闭时触发 blur（供表单校验） */
function onHide() {
  emit('blur')
}

/** 监听 filterText 过滤树 */
watch(filterText, val => {
  treeRef.value?.filter(val)
})

/** 当弹层打开时，同步已选中状态到树 */
watch(visible, async (val) => {
  if (val) {
    await nextTick()
    treeRef.value?.setCheckedKeys(props.modelValue || [])
  }
})

onMounted(loadData)
</script>

<style scoped>
.org-user-select__trigger {
  display: flex;
  align-items: center;
  height: 32px;
  padding: 0 11px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: #fff;
  cursor: pointer;
  box-sizing: border-box;
  transition: border-color 0.2s;
  font-size: 14px;
}
.org-user-select__trigger:hover {
  border-color: var(--el-color-primary);
}
.org-user-select__trigger.is-disabled {
  background-color: var(--el-disabled-bg-color);
  cursor: not-allowed;
}
.placeholder {
  color: var(--el-text-color-placeholder);
  flex: 1;
}
.selected-text {
  flex: 1;
  color: var(--el-text-color-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.arrow-icon {
  color: var(--el-text-color-placeholder);
  margin-left: 4px;
  flex-shrink: 0;
}
.org-user-select__panel {
  padding: 8px;
}
.panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--el-border-color-lighter);
  margin-top: 8px;
  padding-top: 8px;
}
.count-text {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
