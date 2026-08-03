<template>
  <!-- 必须保持单根节点：v-hasPermi 靠删除根 DOM 元素实现权限控制 -->
  <el-button
    link
    type="primary"
    :tag="href ? 'a' : 'button'"
    :href="href"
    :icon="icon"
    :disabled="disabled"
    draggable="false"
    @click="onClick"
  >
    <slot>{{ label }}</slot>
  </el-button>
</template>

<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'

interface Props {
  /** 目标路由，支持字符串路径与 { path, query } 对象两种形态 */
  to: string | RouteLocationRaw
  /** 按钮文字，也可用默认插槽覆盖（插槽优先） */
  label?: string
  /** 图标名，透传给 el-button。EP 图标由 main.ts 的 app.use(elementIcons) 全量全局注册，故字符串可用 */
  icon?: string
  /** 禁用态不生成 href，避免 el-button 在 tag='a' 时丢失 disabled 拦截导致整页跳转 */
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  label: '',
  icon: undefined,
  disabled: false
})

const emit = defineEmits<{
  (e: 'navigate', event: MouseEvent): void
}>()

const router = useRouter()
const instance = getCurrentInstance()

// 生成真实 href，让浏览器原生的「右键新标签 / 中键 / Ctrl+点击」能力生效
const href = computed<string | undefined>(() => {
  if (props.disabled) return undefined
  try {
    return router.resolve(props.to as RouteLocationRaw).href
  } catch {
    // 路由解析失败（如 name 不存在）时退化成普通 button，功能不丢
    return undefined
  }
})

function onClick(e: MouseEvent) {
  if (e.defaultPrevented) return
  // 带修饰键或非左键：不拦截，交还浏览器打开新标签/新窗口
  if (e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return
  e.preventDefault()
  // 调用方绑了 @navigate 则交给它（navigate 已被 defineEmits 声明，不会出现在 attrs 里）
  if (instance?.vnode.props?.onNavigate) {
    emit('navigate', e)
  } else {
    router.push(props.to as RouteLocationRaw)
  }
}
</script>
