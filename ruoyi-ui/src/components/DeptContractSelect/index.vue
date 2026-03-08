<template>
  <div class="dept-contract-select" :style="{ width }">
    <el-popover
      v-model:visible="visible"
      placement="bottom-start"
      :width="520"
      trigger="click"
      :disabled="disabled"
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
        <!-- 第一步：选择部门 -->
        <div class="panel-section">
          <div class="section-label">选择部门</div>
          <el-tree-select
            v-model="selectedDeptId"
            :data="deptTreeData"
            :props="{ label: 'label', value: 'value', children: 'children' }"
            placeholder="请选择部门"
            clearable
            filterable
            style="width: 100%"
            @change="onDeptChange"
          />
        </div>

        <!-- 第二步：选择合同 -->
        <div class="panel-section" style="margin-top: 12px;">
          <div class="section-label">选择合同</div>
          <el-input
            v-model="contractKeyword"
            placeholder="输入关键字筛选合同"
            clearable
            size="small"
            style="margin-bottom: 8px"
            :disabled="!selectedDeptId"
            @input="onKeywordInput"
          />
          <div v-if="!selectedDeptId" class="hint-text">请先选择部门</div>
          <div v-else-if="contractLoading" class="hint-text">
            <el-icon class="is-loading"><Loading /></el-icon> 加载中...
          </div>
          <div v-else-if="contractList.length === 0" class="hint-text">暂无合同数据</div>
          <el-scrollbar v-else max-height="240px">
            <div
              v-for="item in contractList"
              :key="item.contractId"
              class="contract-item"
              :class="{ 'is-selected': selectedContract && selectedContract.contractId === item.contractId }"
              @click="selectContract(item)"
            >
              <div class="contract-name">{{ item.contractName }}</div>
              <div class="contract-meta">
                <span>{{ item.contractCode || '-' }}</span>
                <span v-if="item.contractAmount" style="margin-left: 8px;">
                  {{ Number(item.contractAmount).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }} 元
                </span>
              </div>
            </div>
          </el-scrollbar>
        </div>

        <!-- 底部操作 -->
        <div class="panel-footer">
          <el-button link size="small" @click="clearSelection">清空</el-button>
          <el-button size="small" type="primary" @click="visible = false">关闭</el-button>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup name="DeptContractSelect">
import { ref, watch, onMounted } from 'vue'
import { ArrowDown, Loading } from '@element-plus/icons-vue'
import { getDeptTree } from '@/api/project/project'
import { listContractsByDept } from '@/api/project/contract'
import { handleTree } from '@/utils/ruoyi'

const props = defineProps({
  modelValue: {
    type: Object,
    default: null
  },
  placeholder: {
    type: String,
    default: '请选择部门和合同'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  width: {
    type: String,
    default: '100%'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const visible = ref(false)
const deptTreeData = ref([])
const selectedDeptId = ref(null)
const contractKeyword = ref('')
const contractList = ref([])
const contractLoading = ref(false)
const selectedContract = ref(props.modelValue || null)

let keywordTimer = null

onMounted(() => {
  loadDeptTree()
})

watch(() => props.modelValue, (val) => {
  selectedContract.value = val || null
})

function loadDeptTree() {
  getDeptTree().then(response => {
    const flatList = response.data || []
    // 只保留三级及以下部门（ancestors 中逗号数 >= 2，即 "0,100,101" 这种格式表示第三级）
    const filtered = flatList.filter(dept => {
      if (!dept.ancestors) return false
      return dept.ancestors.split(',').length >= 3
    })
    const mapped = filtered.map(dept => ({
      ...dept,
      value: dept.deptId,
      label: dept.deptName
    }))
    deptTreeData.value = handleTree(mapped, 'deptId', 'parentId', 'children')
  })
}

function onDeptChange(deptId) {
  contractList.value = []
  contractKeyword.value = ''
  if (deptId) {
    loadContracts(deptId, '')
  }
}

function onKeywordInput() {
  clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    if (selectedDeptId.value) {
      loadContracts(selectedDeptId.value, contractKeyword.value)
    }
  }, 300)
}

function loadContracts(deptId, keyword) {
  contractLoading.value = true
  listContractsByDept(deptId, keyword).then(res => {
    contractList.value = res.data || []
  }).finally(() => {
    contractLoading.value = false
  })
}

function selectContract(item) {
  selectedContract.value = item
  emit('update:modelValue', item)
  emit('change', item)
  visible.value = false
}

function clearSelection() {
  selectedContract.value = null
  selectedDeptId.value = null
  contractList.value = []
  contractKeyword.value = ''
  emit('update:modelValue', null)
  emit('change', null)
}
</script>

<style scoped>
.dept-contract-select {
  display: inline-block;
}

.dept-contract-select__trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  height: 32px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-blank);
  cursor: pointer;
  font-size: 14px;
  color: var(--el-text-color-regular);
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.dept-contract-select__trigger:hover {
  border-color: var(--el-color-primary);
}

.dept-contract-select__trigger.is-disabled {
  background-color: var(--el-disabled-bg-color);
  cursor: not-allowed;
  color: var(--el-disabled-text-color);
}

.placeholder {
  color: var(--el-text-color-placeholder);
}

.arrow-icon {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  flex-shrink: 0;
}

.dept-contract-select__panel {
  padding: 4px 0;
}

.panel-section .section-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
  font-weight: 500;
}

.hint-text {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
  text-align: center;
  padding: 16px 0;
}

.contract-item {
  padding: 8px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}

.contract-item:hover {
  background-color: var(--el-fill-color-light);
}

.contract-item.is-selected {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.contract-name {
  font-size: 13px;
  line-height: 1.4;
}

.contract-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.contract-item.is-selected .contract-meta {
  color: var(--el-color-primary-light-3);
}

.panel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
