<template>
  <div>
    <!-- 第一步：选择部门 -->
    <el-tree-select
      v-model="selectedDeptId"
      :data="deptOptions"
      :props="{ value: 'id', label: 'label', children: 'children' }"
      value-key="id"
      placeholder="第一步：选择部门"
      check-strictly
      clearable
      filterable
      style="width: 100%; margin-bottom: 8px;"
      @change="handleDeptChange"
    />
    <!-- 第二步：选择项目 -->
    <el-select
      v-model="selectedProjectIds"
      placeholder="第二步：选择项目（可多选）"
      multiple
      filterable
      clearable
      collapse-tags
      collapse-tags-tooltip
      :max-collapse-tags="3"
      :disabled="!selectedDeptId"
      style="width: 100%"
      @change="handleProjectChange"
    >
      <el-option
        v-for="project in projectOptions"
        :key="project.projectId"
        :label="project.projectName"
        :value="project.projectId"
      />
    </el-select>
  </div>
</template>

<script setup name="DeptProjectSelect">
import { ref, watch, onMounted } from 'vue'
import { getDeptTree, listProjectByDept } from '@/api/project/project'
import { handleTree } from '@/utils/ruoyi'

const props = defineProps({
  // 已选项目ID列表（v-model）
  modelValue: { type: Array, default: () => [] },
  // 编辑合同时传入，排除当前合同已关联的项目（使其可重新选择）
  excludeContractId: { type: [Number, String], default: null },
  // 初始部门（回显用）
  initDeptId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:modelValue', 'projects-change'])

const deptOptions = ref([])
const projectOptions = ref([])
const selectedDeptId = ref(null)
const selectedProjectIds = ref([])

onMounted(() => {
  loadDeptTree()
  if (props.initDeptId) {
    selectedDeptId.value = props.initDeptId
    loadProjects(props.initDeptId)
  }
})

// 回显：父组件传入初始值
watch(() => props.modelValue, (val) => {
  if (val && val.length && !selectedProjectIds.value.length) {
    selectedProjectIds.value = [...val]
  }
}, { immediate: true })

watch(() => props.initDeptId, (val) => {
  if (val && !selectedDeptId.value) {
    selectedDeptId.value = val
    loadProjects(val)
  }
})

function loadDeptTree() {
  getDeptTree().then(res => {
    const flat = res.data || []
    const tree = handleTree(flat.map(d => ({ ...d, id: d.deptId, label: d.deptName })), 'id')
    deptOptions.value = tree
  })
}

function loadProjects(deptId) {
  listProjectByDept(deptId, props.excludeContractId).then(res => {
    projectOptions.value = res.data || []
  })
}

function handleDeptChange(deptId) {
  selectedProjectIds.value = []
  projectOptions.value = []
  emit('update:modelValue', [])
  emit('projects-change', [])
  if (deptId) loadProjects(deptId)
}

function handleProjectChange(ids) {
  emit('update:modelValue', ids)
  const selected = projectOptions.value.filter(p => ids.includes(p.projectId))
  emit('projects-change', selected)
}

// 暴露给父组件：重置
defineExpose({
  reset() {
    selectedDeptId.value = null
    selectedProjectIds.value = []
    projectOptions.value = []
  }
})
</script>
