# 表单验证增强实现文档

## 概述

为立项申请（apply.vue）和项目编辑（edit.vue）页面添加实时验证和错误定位功能。

## 已完成

### 1. 创建 Composable (`useFormValidation.ts`)

位置：`ruoyi-ui/src/composables/useFormValidation.ts`

功能：
- ✅ `validateOnBlur(prop)` - 单字段实时验证
- ✅ `validateAndScroll(callback)` - 提交验证 + 自动滚动到错误位置
- ✅ `scrollToError(prop)` - 滚动到指定错误字段
- ✅ 自动展开折叠面板
- ✅ 错误字段高亮动画

### 2. 创建样式文件 (`form-validation.scss`)

位置：`ruoyi-ui/src/assets/styles/form-validation.scss`

功能：
- ✅ 抖动动画 (`shake-error`)
- ✅ 错误边框高亮
- ✅ 验证成功视觉反馈（可选）

### 3. 修改 apply.vue

已完成的修改：
- ✅ 引入 `useFormValidation` composable
- ✅ 所有 `el-card` 添加 `data-panel` 属性
- ✅ 所有 `el-form-item` 添加 `data-prop` 属性
- ✅ 所有表单控件添加 `@blur="validateOnBlur('fieldName')"` 事件
- ✅ 修改 `submitForm()` 使用 `validateAndScroll()`
- ✅ 引入样式文件

## 待完成

### 修改 edit.vue

需要进行相同的修改：

1. **引入 composable**
```javascript
import { useFormValidation } from '@/composables/useFormValidation'
const { validateOnBlur, validateAndScroll } = useFormValidation(editFormRef, activeNames)
```

2. **添加 data 属性**
- 所有 `<el-card>` 添加 `data-panel="X"`
- 所有 `<el-form-item prop="xxx">` 添加 `data-prop="xxx"`

3. **添加 blur 事件**
所有表单控件添加：
```vue
@blur="validateOnBlur('fieldName')"
```

4. **修改提交函数**
```javascript
function submitForm() {
  validateAndScroll(() => {
    // 原有的提交逻辑
  })
}
```

5. **引入样式**
```vue
<style scoped src="@/assets/styles/form-validation.scss"></style>
```

## 使用方法

### 用户体验

1. **实时验证**
   - 用户填写完字段后，光标离开（blur）时自动验证
   - 立即显示错误提示，无需等待提交

2. **提交时定位**
   - 点击提交按钮时，如果有验证错误
   - 自动展开包含错误的折叠面板
   - 平滑滚动到第一个错误字段
   - 错误字段抖动高亮，吸引注意力

3. **视觉反馈**
   - 错误字段：红色边框 + 抖动动画
   - 错误提示：Element Plus 原生提示
   - 面板自动展开：确保错误可见

## 技术细节

### 字段定位机制

通过 `data-prop` 属性定位字段：
```vue
<el-form-item prop="projectName" data-prop="projectName">
```

查找逻辑：
```javascript
const element = document.querySelector(`[data-prop="${prop}"]`)
```

### 面板展开机制

通过 `data-panel` 属性识别面板：
```vue
<el-card data-panel="1">
```

展开逻辑：
```javascript
const panel = element.closest('[data-panel]')
const panelIndex = panel?.getAttribute('data-panel')
if (!activeNames.value.includes(panelIndex)) {
  activeNames.value.push(panelIndex)
}
```

### 滚动时机

```javascript
setTimeout(() => {
  element.scrollIntoView({ behavior: 'smooth', block: 'center' })
}, 300) // 等待面板展开动画完成
```

## 快速应用到 edit.vue

由于 edit.vue 和 apply.vue 结构相似，可以使用以下步骤快速应用：

### 方法一：手动修改（推荐）

1. 复制 apply.vue 的 script setup 开头部分（引入 composable）
2. 批量替换：在 edit.vue 中查找所有 `<el-form-item prop="xxx">` 并添加 `data-prop="xxx"`
3. 批量替换：在所有表单控件上添加 `@blur="validateOnBlur('xxx')"`
4. 修改 submitForm 函数
5. 添加样式引入

### 方法二：使用脚本（快速）

创建一个 Node.js 脚本自动处理（见下方）

## 自动化脚本

```javascript
// scripts/add-form-validation.js
const fs = require('fs')

function addValidationToFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8')

  // 1. 添加 composable 引入
  content = content.replace(
    /import { updateProject } from/,
    `import { useFormValidation } from '@/composables/useFormValidation'\nimport { updateProject } from`
  )

  // 2. 添加 composable 使用
  content = content.replace(
    /const editFormRef = ref\(\)/,
    `const editFormRef = ref()\nconst { validateOnBlur, validateAndScroll } = useFormValidation(editFormRef, activeNames)`
  )

  // 3. 添加 data-prop 属性
  content = content.replace(
    /<el-form-item ([^>]*?)prop="([^"]+)"/g,
    '<el-form-item $1prop="$2" data-prop="$2"'
  )

  // 4. 添加 data-panel 属性
  const panels = ['1', '2', '3', '4', '5', '6', '7']
  panels.forEach((panel, index) => {
    content = content.replace(
      new RegExp(`(<el-card[^>]*?)(>.*?面板${index + 1})`, 's'),
      `$1 data-panel="${panel}"$2`
    )
  })

  // 5. 添加 blur 事件（需要更复杂的逻辑）
  // ... 省略

  fs.writeFileSync(filePath, content, 'utf-8')
}

addValidationToFile('./ruoyi-ui/src/views/project/project/edit.vue')
```

## 测试清单

- [ ] apply.vue - 实时验证工作正常
- [ ] apply.vue - 提交时滚动到错误字段
- [ ] apply.vue - 折叠面板自动展开
- [ ] apply.vue - 错误字段抖动高亮
- [ ] edit.vue - 实时验证工作正常
- [ ] edit.vue - 提交时滚动到错误字段
- [ ] edit.vue - 折叠面板自动展开
- [ ] edit.vue - 错误字段抖动高亮
- [ ] 所有必填字段都有 blur 验证
- [ ] 样式在不同浏览器下正常显示

## 注意事项

1. **el-select 的 blur 事件**
   - Element Plus 的 select 组件 blur 事件可能不触发
   - 如果遇到问题，可以使用 `@change` 代替

2. **el-input-number 的 blur 事件**
   - 需要确保 blur 事件正确绑定
   - 测试时注意验证触发时机

3. **el-date-picker 的 blur 事件**
   - 日期选择器的 blur 可能需要特殊处理
   - 建议使用 `@change` 事件

4. **性能考虑**
   - 实时验证会增加一些性能开销
   - 对于35个字段的表单，影响可以忽略不计

## 后续优化建议

1. **错误汇总面板**（可选）
   - 在页面顶部显示所有错误列表
   - 点击错误项跳转到对应字段

2. **进度指示器**（可选）
   - 显示各面板的验证状态
   - 快速定位未完成的部分

3. **防抖优化**（可选）
   - 对于频繁触发的验证，添加防抖
   - 减少不必要的验证调用

4. **自定义验证规则**
   - 添加跨字段验证（如结束日期必须大于开始日期）
   - 添加异步验证（如项目编号唯一性检查）
