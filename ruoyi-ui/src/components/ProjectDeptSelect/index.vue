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
import { handleTree } from '@/utils/ruoyi'

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

/** 获取部门树 */
function loadDeptTree() {
  request({
    url: '/project/project/deptTree',
    method: 'get'
  }).then(response => {
    // 与 index.vue 保持一致：用 ancestors 判断层级，只保留三级及以下机构
    const level3AndBelowDepts = (response.data || []).filter(dept => {
      if (!dept.ancestors) return false
      return dept.ancestors.split(',').length >= 3
    })
    const flatList = level3AndBelowDepts.map((d) => ({
      id: d.deptId,
      label: d.deptName,
      parentId: d.parentId
    }))
    deptOptions.value = handleTree(flatList, 'id', 'parentId')
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
