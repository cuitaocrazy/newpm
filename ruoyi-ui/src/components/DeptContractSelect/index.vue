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
        <el-scrollbar max-height="360px">
          <el-tree
            ref="treeRef"
            node-key="nodeId"
            lazy
            :load="loadNode"
            :props="treeProps"
            highlight-current
            :current-node-key="currentNodeKey"
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
const selectedContract = ref(props.modelValue || null)
const currentNodeKey = ref(null)

// 部门数据仅请求一次，缓存在此
let allValidDepts = []

watch(() => props.modelValue, (val) => {
  selectedContract.value = val || null
  currentNodeKey.value = val ? `c_${val.contractId}` : null
})

/**
 * el-tree lazy loadNode：
 *   level 0 → 加载三级及以下根部门（从服务端拿全量部门后过滤+缓存）
 *   isDept  → 同时返回该部门的子部门 + 该部门下的合同
 */
function loadNode(node, resolve) {
  if (node.level === 0) {
    getDeptTree().then(res => {
      const all = res.data || []
      // 三级及以下部门（ancestors 逗号数 >= 2，即 "0,100,101..."）
      allValidDepts = all.filter(d => d.ancestors && d.ancestors.split(',').length >= 3)
      const validIds = new Set(allValidDepts.map(d => d.deptId))
      // 根节点：parentId 不在 validDepts 范围内
      const roots = allValidDepts
        .filter(d => !validIds.has(d.parentId))
        .map(d => makeDeptNode(d))
      resolve(roots)
    }).catch(() => resolve([]))
    return
  }

  if (node.data.isDept) {
    const deptId = node.data.deptId
    // 直接子部门（从缓存中查，无需再请求）
    const subDepts = allValidDepts
      .filter(d => d.parentId === deptId)
      .map(d => makeDeptNode(d))
    // 该部门下的合同（异步加载）
    listContractsByDept(deptId, '').then(res => {
      const contracts = (res.data || []).map(c => makeContractNode(c))
      resolve([...subDepts, ...contracts])
    }).catch(() => resolve(subDepts))
    return
  }

  resolve([])
}

function makeDeptNode(d) {
  return {
    nodeId: `d_${d.deptId}`,
    label: d.deptName,
    deptId: d.deptId,
    parentId: d.parentId,
    isDept: true,
    isLeaf: false   // 保持可展开，展开时懒加载合同
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
  treeRef.value?.setCurrentKey(null)
  emit('update:modelValue', null)
  emit('change', null)
}

function onHide() {
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
