<template>
  <el-tree-select
    :model-value="modelValue"
    :data="deptOptions"
    :props="{ value: 'id', label: 'label', children: 'children' }"
    value-key="id"
    :placeholder="placeholder"
    :check-strictly="checkStrictly"
    :clearable="clearable"
    :disabled="disabled"
    :filterable="filterable"
    :style="{ width: width }"
    @update:model-value="handleChange"
    @blur="handleBlur"
  />
</template>

<script setup name="ProjectDeptSelect">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

// Props
const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: null
  },
  placeholder: {
    type: String,
    default: '请选择项目部门'
  },
  checkStrictly: {
    type: Boolean,
    default: true
  },
  clearable: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  filterable: {
    type: Boolean,
    default: false
  },
  width: {
    type: String,
    default: '100%'
  },
  // 是否自动加载数据
  autoLoad: {
    type: Boolean,
    default: true
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'change', 'blur'])

// Data
const deptOptions = ref([])

/** 过滤部门树，只保留三级及以下机构 */
function filterDeptTree(depts, level = 1) {
  if (!depts || depts.length === 0) return []

  // 如果当前是第1或第2级，继续递归子节点
  if (level < 3) {
    let result = []
    depts.forEach(dept => {
      if (dept.children && dept.children.length > 0) {
        const filtered = filterDeptTree(dept.children, level + 1)
        result = result.concat(filtered)
      }
    })
    return result
  }

  // 第3级及以下，保留当前节点及其子节点
  return depts.map(dept => ({
    ...dept,
    children: dept.children && dept.children.length > 0
      ? filterDeptTree(dept.children, level + 1)
      : undefined
  }))
}

/** 获取部门树 */
function loadDeptTree() {
  request({
    url: '/system/dept/treeselect',
    method: 'get'
  }).then(response => {
    const allDepts = response.data || []
    // 过滤掉前两级，只显示三级及以下机构
    deptOptions.value = filterDeptTree(allDepts)
  })
}

/** 处理值变化 */
function handleChange(value) {
  emit('update:modelValue', value)
  emit('change', value)
}

/** 处理失焦事件 */
function handleBlur(event) {
  emit('blur', event)
}

/** 暴露方法供外部调用 */
defineExpose({
  loadDeptTree,
  deptOptions
})

// 组件挂载时自动加载数据
onMounted(() => {
  if (props.autoLoad) {
    loadDeptTree()
  }
})
</script>

<style scoped>
/* 组件样式 */
</style>
