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
      v-for="user in userOptions"
      :key="user.userId"
      :label="user.nickName"
      :value="user.userId"
      :disabled="user.status === '1'"
    />
  </el-select>
</template>

<script setup name="UserSelect">
import { ref, watch, onMounted } from 'vue'
import { listUserByPost } from '@/api/system/user'
import { listUser } from '@/api/system/user'

// Props
const props = defineProps({
  modelValue: {
    type: [Number, Array],
    default: null
  },
  postCode: {
    type: String,
    default: null
  },
  placeholder: {
    type: String,
    default: '请选择用户'
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
    default: true
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'change', 'blur', 'clear', 'visible-change', 'remove-tag', 'focus'])

// Data
const userOptions = ref([])

/** 加载用户列表 */
async function loadUsers() {
  try {
    let response
    if (props.postCode) {
      // 按岗位筛选
      response = await listUserByPost(props.postCode)
      userOptions.value = response.data || []
    } else {
      // 查询所有用户
      response = await listUser({})
      userOptions.value = response.rows || []
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    userOptions.value = []
  }
}

/** 处理值变化 */
function handleChange(value) {
  emit('update:modelValue', value)
}

/** 处理 change 事件（增强版，返回完整用户对象） */
function handleChangeEvent(value) {
  let user = null
  if (props.multiple && Array.isArray(value)) {
    user = userOptions.value.filter(u => value.includes(u.userId))
  } else {
    user = userOptions.value.find(u => u.userId === value)
  }
  emit('change', value, user)
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

/** 根据ID查找用户 */
function getUserById(userId) {
  return userOptions.value.find(u => u.userId === userId)
}

/** 根据ID获取用户昵称 */
function getUserName(userId) {
  const user = getUserById(userId)
  return user ? user.nickName : ''
}

/** 清空选择 */
function clear() {
  emit('update:modelValue', props.multiple ? [] : null)
}

/** 监听 postCode 变化，自动重新加载 */
watch(() => props.postCode, () => {
  if (props.autoLoad) {
    loadUsers()
  }
})

/** 暴露方法供外部调用 */
defineExpose({
  loadUsers,
  userOptions,
  getUserById,
  getUserName,
  clear
})

// 组件挂载时自动加载数据
onMounted(() => {
  if (props.autoLoad) {
    loadUsers()
  }
})
</script>

<style scoped>
/* 组件样式 */
</style>
