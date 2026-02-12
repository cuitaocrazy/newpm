<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :filterable="filterable"
    :disabled="isInternalDisabled"
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
      v-for="region in regionOptions"
      :key="region.regionId"
      :label="region.regionName"
      :value="region.regionId"
    />
  </el-select>
</template>

<script setup name="SecondaryRegionSelect">
import { ref, watch, onMounted, computed } from 'vue'
import { listSecondaryRegion } from '@/api/project/secondaryRegion'

// Props
const props = defineProps({
  modelValue: {
    type: [Number, Array],
    default: null
  },
  regionDictValue: {
    type: String,
    default: null
  },
  placeholder: {
    type: String,
    default: '请选择二级区域'
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
  autoDisabled: {
    type: Boolean,
    default: true
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'change', 'blur', 'clear', 'visible-change', 'remove-tag', 'focus'])

// Data
const regionOptions = ref([])

/** 计算是否禁用（未选一级区域时自动禁用） */
const isInternalDisabled = computed(() => {
  return props.disabled || (props.autoDisabled && !props.regionDictValue)
})

/** 加载二级区域列表 */
async function loadRegions() {
  if (!props.regionDictValue) {
    regionOptions.value = []
    return
  }

  try {
    const response = await listSecondaryRegion({ regionDictValue: props.regionDictValue })
    regionOptions.value = response.rows || []
  } catch (error) {
    console.error('加载二级区域失败:', error)
    regionOptions.value = []
  }
}

/** 处理值变化 */
function handleChange(value) {
  emit('update:modelValue', value)
}

/** 处理 change 事件（增强版，返回完整区域对象） */
function handleChangeEvent(value) {
  let region = null
  if (props.multiple && Array.isArray(value)) {
    region = regionOptions.value.filter(r => value.includes(r.regionId))
  } else {
    region = regionOptions.value.find(r => r.regionId === value)
  }
  emit('change', value, region)
}

/** 处理失焦事件 */
function handleBlur(event) {
  emit('blur', event)
}

/** 处理清空事件 */
function handleClear() {
  emit('clear')
}

/** 处理下拉显示隐藏 */
function handleVisibleChange(visible) {
  emit('visible-change', visible)
}

/** 处理移除标签 */
function handleRemoveTag(value) {
  emit('remove-tag', value)
}

/** 处理聚焦事件 */
function handleFocus(event) {
  emit('focus', event)
}

/** 根据ID查找区域 */
function getRegionById(regionId) {
  return regionOptions.value.find(r => r.regionId === regionId)
}

/** 根据ID获取区域名称 */
function getRegionName(regionId) {
  const region = getRegionById(regionId)
  return region ? region.regionName : ''
}

/** 清空选择和选项 */
function clearOptions() {
  emit('update:modelValue', props.multiple ? [] : null)
  regionOptions.value = []
}

/** 清空选择（保留选项） */
function clear() {
  emit('update:modelValue', props.multiple ? [] : null)
}

/** 监听一级区域变化 */
watch(() => props.regionDictValue, async (newVal, oldVal) => {
  // 一级区域变化时，清空选择
  if (newVal !== oldVal) {
    emit('update:modelValue', props.multiple ? [] : null)
    regionOptions.value = []
  }

  // 加载新数据
  if (newVal) {
    await loadRegions()
  }
}, { immediate: false })

/** 暴露方法供外部调用 */
defineExpose({
  loadRegions,
  regionOptions,
  getRegionById,
  getRegionName,
  clearOptions,
  clear
})

// 组件挂载时，如果已经有一级区域值，则加载数据
onMounted(() => {
  if (props.regionDictValue) {
    loadRegions()
  }
})
</script>

<style scoped>
/* 组件样式 */
</style>
