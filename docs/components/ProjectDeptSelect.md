# ProjectDeptSelect 项目部门选择器

## 组件说明

项目部门选择器是一个封装了部门树选择逻辑的公共组件，专门用于项目管理模块中的部门选择功能。

### 特性

- 自动加载部门树数据
- 自动过滤前两级部门，只显示三级及以下机构
- 支持任意层级选择（checkStrictly）
- 支持清空、禁用、过滤等常用功能
- 全局注册，无需手动导入

## 使用方法

### 基础用法

```vue
<template>
  <el-form-item label="项目部门" prop="projectDept">
    <project-dept-select v-model="form.projectDept" />
  </el-form-item>
</template>

<script setup>
import { ref } from 'vue'

const form = ref({
  projectDept: null
})
</script>
```

### 自定义宽度

```vue
<project-dept-select
  v-model="form.projectDept"
  width="300px"
/>
```

### 禁用状态

```vue
<project-dept-select
  v-model="form.projectDept"
  :disabled="true"
/>
```

### 支持过滤

```vue
<project-dept-select
  v-model="form.projectDept"
  :filterable="true"
/>
```

### 监听事件

```vue
<project-dept-select
  v-model="form.projectDept"
  @change="handleDeptChange"
  @blur="handleBlur"
/>
```

### 手动加载数据

```vue
<template>
  <project-dept-select
    v-model="form.projectDept"
    :auto-load="false"
    ref="deptSelectRef"
  />
  <el-button @click="loadData">加载数据</el-button>
</template>

<script setup>
import { ref } from 'vue'

const deptSelectRef = ref()

function loadData() {
  deptSelectRef.value.loadDeptTree()
}
</script>
```

## Props

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| modelValue | 绑定值 | String / Number | — | null |
| placeholder | 占位文本 | String | — | 请选择项目部门 |
| checkStrictly | 是否严格的遵守父子节点不互相关联 | Boolean | — | true |
| clearable | 是否可清空 | Boolean | — | true |
| disabled | 是否禁用 | Boolean | — | false |
| filterable | 是否可搜索 | Boolean | — | false |
| width | 组件宽度 | String | — | 100% |
| autoLoad | 是否自动加载数据 | Boolean | — | true |

## Events

| 事件名 | 说明 | 回调参数 |
|--------|------|----------|
| update:modelValue | 绑定值变化时触发 | (value: String \| Number) |
| change | 选中值发生变化时触发 | (value: String \| Number) |
| blur | 失去焦点时触发 | (event: Event) |

## Methods

| 方法名 | 说明 | 参数 |
|--------|------|------|
| loadDeptTree | 手动加载部门树数据 | — |

## 实现原理

### 数据过滤逻辑

组件内部实现了 `filterDeptTree` 函数，递归过滤部门树：

1. 跳过第1、2级部门节点
2. 从第3级开始保留节点及其所有子节点
3. 保持树形结构的层级关系

```javascript
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
```

### 数据加载

组件通过 `/system/dept/treeselect` 接口获取部门树数据，该接口返回完整的部门树结构。

## 使用场景

### 1. 查询条件（index.vue）

```vue
<el-form-item label="项目部门" prop="projectDept">
  <project-dept-select
    v-model="queryParams.projectDept"
    style="width: 200px"
  />
</el-form-item>
```

### 2. 立项申请（apply.vue）

```vue
<el-form-item label="项目部门" prop="projectDept" data-prop="projectDept">
  <project-dept-select
    v-model="form.projectDept"
    @blur="validateOnBlur('projectDept')"
  />
</el-form-item>
```

### 3. 编辑项目（edit.vue）

```vue
<el-form-item label="项目部门" prop="projectDept" data-prop="projectDept">
  <project-dept-select
    v-model="form.projectDept"
    @blur="validateOnBlur('projectDept')"
  />
</el-form-item>
```

## 注意事项

1. **全局注册**：组件已在 `main.ts` 中全局注册，无需在页面中手动导入
2. **自动加载**：默认情况下组件会在挂载时自动加载数据，如需手动控制请设置 `:auto-load="false"`
3. **数据格式**：组件期望的数据格式为 `{ id, label, children }` 结构
4. **过滤逻辑**：组件会自动过滤前两级部门，只显示三级及以下机构
5. **表单验证**：配合 el-form 使用时，需要在 form-item 上设置 prop 属性

## 迁移指南

### 从旧代码迁移

**旧代码：**

```vue
<template>
  <el-tree-select
    v-model="form.projectDept"
    :data="deptOptions"
    :props="{ value: 'id', label: 'label', children: 'children' }"
    value-key="id"
    placeholder="请选择项目部门"
    check-strictly
    clearable
  />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const deptOptions = ref([])

function filterDeptTree(depts, level = 1) {
  // ... 过滤逻辑
}

function getDeptTree() {
  request({
    url: '/system/dept/treeselect',
    method: 'get'
  }).then(response => {
    const allDepts = response.data || []
    deptOptions.value = filterDeptTree(allDepts)
  })
}

onMounted(() => {
  getDeptTree()
})
</script>
```

**新代码：**

```vue
<template>
  <project-dept-select v-model="form.projectDept" />
</template>

<script setup>
import { ref } from 'vue'

const form = ref({
  projectDept: null
})
</script>
```

### 迁移步骤

1. 删除 `deptOptions` 变量定义
2. 删除 `filterDeptTree` 函数
3. 删除 `getDeptTree` 函数
4. 删除 `onMounted` 中的 `getDeptTree()` 调用
5. 将 `<el-tree-select>` 替换为 `<project-dept-select>`
6. 移除不需要的 props（data, props, value-key 等）

## 相关文件

- 组件源码：`ruoyi-ui/src/components/ProjectDeptSelect/index.vue`
- 全局注册：`ruoyi-ui/src/main.ts`
- 使用示例：
  - `ruoyi-ui/src/views/project/project/index.vue`
  - `ruoyi-ui/src/views/project/project/apply.vue`
  - `ruoyi-ui/src/views/project/project/edit.vue`
