<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :filterable="filterable"
    :remote="remote"
    :remote-method="remoteMethod"
    :loading="loading"
    :disabled="disabled"
    :multiple="multiple"
    :collapse-tags="collapseTags"
    :collapse-tags-tooltip="collapseTagsTooltip"
    :max-collapse-tags="maxCollapseTags"
    :style="{ width: width }"
    @update:model-value="handleChange"
    @change="handleChangeEvent"
    @blur="handleBlur"
    @clear="handleClear"
    @visible-change="handleVisibleChange"
    @remove-tag="handleRemoveTag"
    @focus="handleFocus"
  >
    <el-option
      v-for="project in projectOptions"
      :key="project.projectId"
      :label="project.projectName"
      :value="project.projectId"
    />
  </el-select>
</template>

<script setup name="ProjectSelect">
import { ref, watch, onMounted } from 'vue'
import { listProject, listProjectByName } from '@/api/project/project'

// Props
const props = defineProps({
  modelValue: {
    type: [Number, Array],
    default: null
  },
  // 是否启用远程搜索（适合大数据量场景）
  remote: {
    type: Boolean,
    default: true
  },
  // 部门ID过滤
  deptId: {
    type: Number,
    default: null
  },
  placeholder: {
    type: String,
    default: '请选择项目'
  },
  clearable: {
    type: Boolean,
    default: true
  },
  filterable: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  multiple: {
    type: Boolean,
    default: false
  },
  collapseTags: {
    type: Boolean,
    default: false
  },
  collapseTagsTooltip: {
    type: Boolean,
    default: false
  },
  maxCollapseTags: {
    type: Number,
    default: 1
  },
  width: {
    type: String,
    default: '100%'
  },
  autoLoad: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'change', 'blur', 'clear', 'visible-change', 'remove-tag', 'focus'])

// Data
const projectOptions = ref([])
const loading = ref(false)

/** 加载项目列表 */
async function loadProjects() {
  try {
    loading.value = true
    const params = {}
    if (props.deptId) {
      params.projectDept = props.deptId
    }
    const response = await listProject(params)
    projectOptions.value = response.rows || []
  } catch (error) {
    console.error('加载项目列表失败:', error)
    projectOptions.value = []
  } finally {
    loading.value = false
  }
}

/** 远程搜索方法 */
async function remoteMethod(query) {
  if (!query) {
    // 无搜索词时，显示所有项目
    if (projectOptions.value.length === 0) {
      await loadProjects()
    }
    return
  }

  try {
    loading.value = true
    const response = await listProjectByName({ projectName: query })
    projectOptions.value = response.data || []
  } catch (error) {
    console.error('搜索项目失败:', error)
    projectOptions.value = []
  } finally {
    loading.value = false
  }
}

/** 处理下拉框显示/隐藏 */
function handleVisibleChange(visible) {
  if (visible && projectOptions.value.length === 0) {
    // 首次打开下拉框，加载所有项目
    loadProjects()
  }
  emit('visible-change', visible)
}

/** 处理值变化 */
function handleChange(value) {
  emit('update:modelValue', value)
}

/** 处理 change 事件（增强版，返回完整项目对象） */
function handleChangeEvent(value) {
  let project = null
  if (props.multiple && Array.isArray(value)) {
    project = projectOptions.value.filter(p => value.includes(p.projectId))
  } else {
    project = projectOptions.value.find(p => p.projectId === value)
  }
  emit('change', value, project)
}

/** 处理失焦事件 */
function handleBlur(event) {
  emit('blur', event)
}

/** 处理清空事件 */
function handleClear() {
  emit('clear')
}

/** 处理移除标签 */
function handleRemoveTag(value) {
  emit('remove-tag', value)
}

/** 处理聚焦事件 */
function handleFocus(event) {
  emit('focus', event)
}

/** 根据ID查找项目 */
function getProjectById(projectId) {
  return projectOptions.value.find(p => p.projectId === projectId)
}

/** 根据ID获取项目名称 */
function getProjectName(projectId) {
  const project = getProjectById(projectId)
  return project ? project.projectName : ''
}

/** 清空选择 */
function clear() {
  emit('update:modelValue', props.multiple ? [] : null)
}

/** 监听 deptId 变化，自动重新加载 */
watch(() => props.deptId, () => {
  if (props.autoLoad && !props.remote) {
    loadProjects()
  }
})

/** 暴露方法供外部调用 */
defineExpose({
  loadProjects,
  projectOptions,
  getProjectById,
  getProjectName,
  clear
})

// 组件挂载时，如果不是远程搜索且需要自动加载，则加载数据
onMounted(() => {
  if (props.autoLoad && !props.remote) {
    loadProjects()
  }
})
</script>

<style scoped>
/* 组件样式 */
</style>
