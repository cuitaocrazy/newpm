<template>
  <div class="dept-contract-select" :style="{ width }">
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
          class="dept-contract-select__trigger"
          :class="{ 'is-disabled': disabled }"
        >
          <span v-if="!selectedContract" class="placeholder">{{ placeholder }}</span>
          <span v-else class="selected-text">{{ selectedContract.contractName }}</span>
          <el-icon class="arrow-icon"><ArrowDown /></el-icon>
        </div>
      </template>

      <!-- 弹出内容 -->
      <div class="dept-contract-select__panel">
        <!-- 搜索框 -->
        <el-input
          v-model="searchText"
          placeholder="搜索合同名称"
          clearable
          size="small"
          style="margin-bottom: 8px"
          @input="onSearchInput"
        />

        <!-- 部门+合同树（始终显示） -->
        <div v-if="treeLoading" class="hint-loading">加载中...</div>
        <el-scrollbar v-else max-height="320px">
          <el-tree
            ref="treeRef"
            :data="treeData"
            node-key="nodeId"
            :filter-node-method="filterNode"
            :props="treeProps"
            highlight-current
            :current-node-key="currentNodeKey"
            default-expand-all
            @node-click="handleNodeClick"
          />
        </el-scrollbar>

        <div class="panel-footer">
          <el-button link size="small" @click="clearSelection">清空</el-button>
          <span class="hint-text">{{ selectedContract ? selectedContract.contractName : '未选择' }}</span>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup name="DeptContractSelect">
import { ref, watch } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { getDeptTree } from '@/api/project/project'
import { listContractsByDept } from '@/api/project/contract'

const props = defineProps({
  modelValue: {
    type: Object,
    default: null
  },
  placeholder: {
    type: String,
    default: '请选择合同'
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
    default: 380
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'blur'])

const treeProps = { label: 'label', children: 'children', isLeaf: 'isLeaf' }

const visible = ref(false)
const treeRef = ref(null)
const treeData = ref([])
const treeLoading = ref(false)
const searchText = ref('')
const selectedContract = ref(props.modelValue || null)
const currentNodeKey = ref(null)

let dataLoaded = false

watch(() => props.modelValue, (val) => {
  selectedContract.value = val || null
  currentNodeKey.value = val ? `c_${val.contractId}` : null
})

// 首次打开时加载完整树数据
watch(visible, (val) => {
  if (val && !dataLoaded) {
    loadAll()
  }
})

/** 一次性加载所有部门（三级及以下）及其合同，拼成完整树 */
async function loadAll() {
  treeLoading.value = true
  try {
    const deptRes = await getDeptTree()
    const allDepts = deptRes.data || []
    const validDepts = allDepts.filter(d => d.ancestors && d.ancestors.split(',').length >= 3)
    const validIds = new Set(validDepts.map(d => d.deptId))

    // 根部门（parentId 不在 validDepts 内的）
    const roots = validDepts.filter(d => !validIds.has(d.parentId))

    // 按部门并行拉取合同
    const contractResults = await Promise.all(
      validDepts.map(d =>
        listContractsByDept(d.deptId, '').then(res => ({
          deptId: d.deptId,
          contracts: res.data || []
        })).catch(() => ({ deptId: d.deptId, contracts: [] }))
      )
    )
    // deptId → contracts 映射
    const contractMap = {}
    contractResults.forEach(({ deptId, contracts }) => {
      contractMap[deptId] = contracts
    })

    // 递归构建树
    function buildTree(depts) {
      return depts.map(d => {
        const subDepts = validDepts.filter(sub => sub.parentId === d.deptId)
        const contracts = (contractMap[d.deptId] || []).map(c => makeContractNode(c))
        const children = [...buildTree(subDepts), ...contracts]
        return {
          nodeId: `d_${d.deptId}`,
          label: d.deptName,
          deptId: d.deptId,
          isDept: true,
          isLeaf: false,
          children
        }
      })
    }

    treeData.value = buildTree(roots)
    dataLoaded = true
  } finally {
    treeLoading.value = false
  }
}

function makeContractNode(c) {
  return {
    nodeId: `c_${c.contractId}`,
    label: c.contractName,
    contractId: c.contractId,
    contractCode: c.contractCode,
    contractName: c.contractName,
    contractAmount: c.contractAmount,
    contractStatus: c.contractStatus,
    isLeaf: true,
    isContract: true
  }
}

// ─── 搜索过滤 ────────────────────────────────────────────

function filterNode(value, data) {
  if (!value) return true
  // 合同节点：按名称匹配
  if (data.isContract) return data.contractName.includes(value)
  // 部门节点：返回 false 让 el-tree 根据子节点决定是否显示
  return false
}

function onSearchInput(val) {
  treeRef.value?.filter(val)
}

// ─── 选中 ────────────────────────────────────────────────

function handleNodeClick(data) {
  if (!data.isContract) return
  selectedContract.value = data
  currentNodeKey.value = data.nodeId
  emit('update:modelValue', data)
  emit('change', data)
  visible.value = false
}

function clearSelection() {
  selectedContract.value = null
  currentNodeKey.value = null
  searchText.value = ''
  treeRef.value?.filter('')
  treeRef.value?.setCurrentKey(null)
  emit('update:modelValue', null)
  emit('change', null)
}

function onHide() {
  searchText.value = ''
  treeRef.value?.filter('')
  emit('blur')
}
</script>

<style scoped>
.dept-contract-select {
  display: inline-block;
}

.dept-contract-select__trigger {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 4px 11px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: #fff;
  cursor: pointer;
  box-sizing: border-box;
  transition: border-color 0.2s;
  font-size: 14px;
}

.dept-contract-select__trigger:hover {
  border-color: var(--el-color-primary);
}

.dept-contract-select__trigger.is-disabled {
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.arrow-icon {
  color: var(--el-text-color-placeholder);
  margin-left: 4px;
  flex-shrink: 0;
  font-size: 12px;
}

.dept-contract-select__panel {
  padding: 8px;
}

.hint-loading {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
  text-align: center;
  padding: 20px 0;
}

.panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--el-border-color-lighter);
  margin-top: 8px;
  padding-top: 8px;
}

.hint-text {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
