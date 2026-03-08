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
          @clear="onSearchClear"
        />

        <!-- 搜索结果（有关键字时） -->
        <template v-if="searchText">
          <div v-if="searchLoading" class="hint-loading">搜索中...</div>
          <div v-else-if="searchResults.length === 0" class="hint-empty">无匹配合同</div>
          <el-scrollbar v-else max-height="320px">
            <div
              v-for="item in searchResults"
              :key="item.contractId"
              class="search-item"
              :class="{ 'is-selected': selectedContract && selectedContract.contractId === item.contractId }"
              @click="selectContract(item)"
            >
              <div class="search-item__name">{{ item.contractName }}</div>
              <div class="search-item__meta">{{ item.contractCode || '' }}</div>
            </div>
          </el-scrollbar>
        </template>

        <!-- 部门树（无关键字时） -->
        <el-scrollbar v-else max-height="320px">
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
import { listContractsByDept, searchContracts } from '@/api/project/contract'

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

// 搜索模式
const searchText = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
let searchTimer = null

// 部门数据仅请求一次，缓存在此
let allValidDepts = []

watch(() => props.modelValue, (val) => {
  selectedContract.value = val || null
  currentNodeKey.value = val ? `c_${val.contractId}` : null
})

// ─── 搜索相关 ────────────────────────────────────────────

function onSearchInput(val) {
  clearTimeout(searchTimer)
  if (!val) return
  searchLoading.value = true
  searchTimer = setTimeout(() => {
    searchContracts({ keyword: val }).then(res => {
      searchResults.value = res.data || []
    }).finally(() => {
      searchLoading.value = false
    })
  }, 300)
}

function onSearchClear() {
  searchResults.value = []
}

function selectContract(item) {
  selectedContract.value = makeContractNode(item)
  currentNodeKey.value = `c_${item.contractId}`
  emit('update:modelValue', selectedContract.value)
  emit('change', selectedContract.value)
  visible.value = false
}

// ─── 部门树懒加载 ─────────────────────────────────────────

function loadNode(node, resolve) {
  if (node.level === 0) {
    getDeptTree().then(res => {
      const all = res.data || []
      allValidDepts = all.filter(d => d.ancestors && d.ancestors.split(',').length >= 3)
      const validIds = new Set(allValidDepts.map(d => d.deptId))
      const roots = allValidDepts
        .filter(d => !validIds.has(d.parentId))
        .map(d => makeDeptNode(d))
      resolve(roots)
    }).catch(() => resolve([]))
    return
  }

  if (node.data.isDept) {
    const deptId = node.data.deptId
    const subDepts = allValidDepts
      .filter(d => d.parentId === deptId)
      .map(d => makeDeptNode(d))
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
    isLeaf: false
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
  searchText.value = ''
  searchResults.value = []
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

.hint-loading,
.hint-empty {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
  text-align: center;
  padding: 16px 0;
}

.search-item {
  padding: 7px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}

.search-item:hover {
  background-color: var(--el-fill-color-light);
}

.search-item.is-selected {
  background-color: var(--el-color-primary-light-9);
}

.search-item__name {
  font-size: 13px;
  color: var(--el-text-color-primary);
  line-height: 1.4;
}

.search-item__meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
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
