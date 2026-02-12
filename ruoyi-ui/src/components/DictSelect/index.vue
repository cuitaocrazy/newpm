<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :filterable="filterable"
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
      v-for="dict in dictOptions"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
      :disabled="dict.disabled"
    />
  </el-select>
</template>

<script setup name="DictSelect">
import { ref, watch, onMounted, getCurrentInstance } from 'vue'

// Props
const props = defineProps({
  modelValue: {
    type: [String, Number, Array],
    default: null
  },
  dictType: {
    type: String,
    required: true
  },
  placeholder: {
    type: String,
    default: '请选择'
  },
  clearable: {
    type: Boolean,
    default: true
  },
  filterable: {
    type: Boolean,
    default: false
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
    default: true
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'change', 'blur', 'clear', 'visible-change', 'remove-tag', 'focus'])

// Data
const { proxy } = getCurrentInstance()
const dictOptions = ref([])

/** 加载字典数据 */
function loadDict() {
  if (!props.dictType) {
    console.warn('DictSelect: dictType is required')
    return
  }

  const { [props.dictType]: dict } = proxy.useDict(props.dictType)

  // 监听字典变化
  watch(dict, (newVal) => {
    dictOptions.value = newVal || []
  }, { immediate: true })
}

/** 处理值变化 */
function handleChange(value) {
  emit('update:modelValue', value)
}

/** 处理 change 事件 */
function handleChangeEvent(value) {
  emit('change', value)
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

/** 清空选择 */
function clear() {
  emit('update:modelValue', props.multiple ? [] : null)
}

/** 暴露方法供外部调用 */
defineExpose({
  loadDict,
  dictOptions,
  clear
})

// 组件挂载时自动加载数据
onMounted(() => {
  if (props.autoLoad) {
    loadDict()
  }
})
</script>

<style scoped>
/* 组件样式 */
</style>
