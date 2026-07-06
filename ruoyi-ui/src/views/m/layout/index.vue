<template>
  <div class="m-layout">
    <router-view />
  </div>
</template>

<script setup name="MobileLayout">
// 移动端布局壳：全量引入 Vant 样式（随移动懒加载 chunk 下发，桌面端不受影响，见 specs/014 research R2）
import 'vant/lib/index.css'
</script>

<style scoped>
.m-layout {
  min-height: 100vh;
  background: #f7f8fa;
  /* 显式文字基线色：防深色模式 WebView 强制改写继承色（白底白字） */
  color: #323233;
  /* iPhone 刘海屏底部安全区，避免固定底栏被 Home 指示条遮挡 */
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
