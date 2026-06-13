<template>
  <ProlistDefectAdd />
</template>

<script setup name="ProlistDefectEdit">
// 编辑复用 add.vue（内部按 route.query.problemId 自动进入编辑模式：加载数据 + 调用 update）
import ProlistDefectAdd from './add.vue'
</script>
