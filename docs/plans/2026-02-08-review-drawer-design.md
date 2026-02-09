# 立项审核页面改为抽屉样式设计

**设计日期：** 2026-02-08
**设计人：** Claude Sonnet 4.5

## 背景

当前立项审核功能使用 `el-dialog` 对话框展示审核详情，用户反馈不喜欢这种方式，希望改为抽屉（Drawer）样式，体验更好。

## 目标

将审核详情页面从对话框改为抽屉样式，提升用户体验。

## 方案对比

### 方案1：对话框（Dialog）- 当前方式
**优点：**
- 居中显示，聚焦性强
- 适合简单表单

**缺点：**
- 空间受限，内容多时需要滚动
- 遮挡背景，无法对比查看
- 用户体验不佳

### 方案2：抽屉（Drawer）- 选定方案 ✅
**优点：**
- 从右侧滑出，空间更大（占据整个屏幕高度）
- 宽度可设置为 80%，足够展示详细内容
- 不完全遮挡背景，可以看到列表
- 视觉体验更现代、流畅
- Element Plus 的 `el-drawer` 组件支持完善

**缺点：**
- 无明显缺点

**适合场景：**
- ✅ 内容较多的详情展示（审核页面有7个折叠面板）
- ✅ 需要查看详细信息的场景

### 方案3：新页面（独立路由）
**优点：**
- 空间最大，完全独立
- 可以有独立的 URL

**缺点：**
- 需要页面跳转，操作流程变长
- 需要配置路由和菜单
- 过于重量级

## 设计方案

### 整体架构

**改动范围：**
只需要修改前端文件：`ruoyi-ui/src/views/project/review/index.vue`

**核心改动：**
将 `el-dialog` 组件替换为 `el-drawer` 组件，保持所有业务逻辑不变。

### 组件配置

```vue
<el-drawer
  v-model="reviewOpen"
  :title="'审核项目 - ' + reviewForm.projectName"
  direction="rtl"
  size="80%"
  :close-on-click-modal="true"
  :close-on-press-escape="false"
>
  <!-- 内容区域 -->
  <template #default>
    <!-- 项目详情（7个折叠面板） -->
    <el-collapse v-model="activeNames">
      <!-- 基本信息 -->
      <el-collapse-item title="基本信息" name="1">...</el-collapse-item>
      <!-- 人员信息 -->
      <el-collapse-item title="人员信息" name="2">...</el-collapse-item>
      <!-- 预算信息 -->
      <el-collapse-item title="预算信息" name="3">...</el-collapse-item>
      <!-- 工作量信息 -->
      <el-collapse-item title="工作量信息" name="4">...</el-collapse-item>
      <!-- 时间信息 -->
      <el-collapse-item title="时间信息" name="5">...</el-collapse-item>
      <!-- 项目描述 -->
      <el-collapse-item title="项目描述" name="6">...</el-collapse-item>
      <!-- 客户信息 -->
      <el-collapse-item title="客户信息" name="7">...</el-collapse-item>
    </el-collapse>

    <!-- 审核表单 -->
    <el-divider />
    <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="100px">
      <el-form-item label="审核意见" prop="approvalReason">
        <el-input v-model="reviewForm.approvalReason" type="textarea" :rows="3" placeholder="请输入审核意见（拒绝时必填）" />
      </el-form-item>
    </el-form>
  </template>

  <!-- 底部按钮固定 -->
  <template #footer>
    <div style="display: flex; justify-content: flex-end; gap: 10px;">
      <el-button type="success" @click="submitApprove('1')">通过</el-button>
      <el-button type="danger" @click="submitApprove('2')">拒绝</el-button>
      <el-button @click="cancelReview">取消</el-button>
    </div>
  </template>
</el-drawer>
```

### 配置说明

| 属性 | 值 | 说明 |
|------|-----|------|
| `v-model` | `reviewOpen` | 控制抽屉显示/隐藏 |
| `title` | `'审核项目 - ' + reviewForm.projectName` | 动态显示项目名称 |
| `direction` | `"rtl"` | 从右侧滑出 |
| `size` | `"80%"` | 占据屏幕宽度的 80% |
| `close-on-click-modal` | `true` | 点击遮罩层关闭 |
| `close-on-press-escape` | `false` | 禁用 ESC 键关闭 |

### 样式和交互细节

**抽屉标题：**
- 动态显示项目名称，用户一眼就知道在审核哪个项目

**内容区域布局：**
- 折叠面板：保持现有的7个 `el-collapse-item`，默认全部展开
- 审核意见表单：放在折叠面板下方，用 `el-divider` 分隔
- 内容滚动：抽屉内容区域可滚动，底部按钮固定不动

**底部按钮样式：**
- 按钮右对齐，间距统一（`gap: 10px`）
- 三个按钮：通过（绿色）、拒绝（红色）、取消（默认）

**关闭行为：**
- 点击遮罩层：关闭抽屉
- 点击右上角 X：关闭抽屉
- 点击取消按钮：关闭抽屉
- 审核成功后：自动关闭抽屉并刷新列表

### 保持不变的部分

- 所有业务逻辑（`handleReview`、`submitApprove`、`cancelReview` 等方法）
- 数据结构（`reviewForm`、`reviewOpen`、`activeNames` 等）
- 7个折叠面板的内容和布局
- 审核表单验证规则
- 所有 API 调用和数据处理逻辑

## 实现步骤

### 1. 替换组件标签
将 `<el-dialog>` 改为 `<el-drawer>`

### 2. 调整属性
- **移除**：`width`、`append-to-body`
- **新增**：`direction="rtl"`、`size="80%"`
- **保留**：`v-model`、`:title`

### 3. 调整插槽
- `#default` → 内容区域（折叠面板 + 表单）
- `#footer` → 底部按钮区域

### 4. 测试验证
- ✅ 点击"审核"按钮，抽屉从右侧滑出
- ✅ 抽屉宽度占据 80% 屏幕
- ✅ 项目详情正确显示（7个折叠面板）
- ✅ 底部按钮固定，始终可见
- ✅ 点击遮罩层/X按钮/取消按钮，抽屉关闭
- ✅ 审核通过/拒绝后，抽屉关闭并刷新列表

## 预计工作量

- 代码修改：5-10分钟
- 测试验证：5分钟
- 总计：约15分钟

## 技术栈

- **前端框架**：Vue 3.5
- **UI 组件库**：Element Plus 2.13
- **组件**：`el-drawer`

## 验收标准

- [ ] 抽屉从右侧滑出，宽度为 80%
- [ ] 抽屉标题显示项目名称
- [ ] 7个折叠面板正确显示，默认全部展开
- [ ] 审核意见表单正常工作
- [ ] 底部按钮固定在底部，始终可见
- [ ] 点击遮罩层/X按钮/取消按钮可以关闭抽屉
- [ ] 审核通过/拒绝功能正常，审核后自动关闭抽屉并刷新列表
- [ ] 所有业务逻辑保持不变

## 后续优化（可选）

- 抽屉打开/关闭时添加动画效果（Element Plus 默认已有）
- 考虑添加快捷键支持（如 Ctrl+Enter 快速通过）
- 考虑添加审核历史记录展示
