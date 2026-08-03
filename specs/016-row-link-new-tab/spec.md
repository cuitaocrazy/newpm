# 016 — 列表页操作列「详情/编辑」支持右键新标签打开

- **GitHub Issue**：[#12](https://github.com/cuitaocrazy/newpm/issues/12)
- **分支**：`016-row-link-new-tab`
- **类型**：前端 only（后端零改动、无 schema 变更）

## 1. 业务描述

各查询（列表）页面的「操作」列提供 **详情** / **编辑** 两个入口，是用户从列表进入单条记录的主路径。

**谁在用**：项目经理、市场/销售经理、财务与管理层——凡是要在列表里逐条核对数据的角色。

**期望的正确行为**：列表是"工作台"，常见动作是**一次打开多条记录横向比对**（核对 3 个项目的预算、比对多份合同的金额）。这要求详情/编辑入口具备浏览器原生的链接语义：

| 操作 | 期望结果 |
|---|---|
| 左键点击 | 当前标签内 SPA 跳转，**不整页刷新**（保持现有行为） |
| 右键 → 在新标签页中打开链接 | 新标签打开该记录 |
| Ctrl/⌘ + 左键 | 新标签打开 |
| 鼠标中键点击 | 新标签打开 |
| 悬停 / 复制链接地址 | 状态栏显示目标 URL，可复制分享 |

## 2. 现象

上述"新标签"类操作**全部无效**：右键菜单没有"在新标签页中打开链接"项；Ctrl/⌘+点击等同普通点击，仍在当前标签跳转并**丢失原列表的查询条件与滚动位置**；中键无反应；无法复制记录直达链接。

用户被迫「进详情 → 返回 → 记住刚才查到哪 → 再进下一条」。

## 3. 根因

**不是逻辑 bug，是 DOM 元素语义缺失。**

操作列渲染为 `<el-button>`，最终 DOM 是 `<button>`，跳转完全由 JS 事件驱动（`ruoyi-ui/src/views/project/project/index.vue:307-308` + `:801-810`）。浏览器的"新标签打开链接 / Ctrl+Click / 中键"三种行为**只对带 `href` 的 `<a>` 生效**；`<button>` + `router.push()` 对浏览器而言不存在"链接"概念，目标 URL 只活在 JS 闭包里，DOM 上无任何痕迹。

同仓库存在可工作的对照：`ruoyi-ui/src/views/project/project/index.vue:191` 项目名称列用 `<el-link :href @click.prevent>`，该列右键**可以**打开新标签——证明是操作列写法漏了这个模式，而非框架限制。

路由为 `createWebHistory()`（`ruoyi-ui/src/router/index.ts:571`），`href` 即真实可访问 URL。

## 4. 范围

### 4.1 纳入（9 个页面 / 17 个入口）

| 页面 | 详情目标 | 编辑目标 |
|---|---|---|
| `project/project/index.vue` | `/project/list/detail/{projectId}` | `/project/list/edit/{projectId}` |
| `project/contract/index.vue` | `/htkx/contract/detail/{contractId}` | `/htkx/contract/edit/{contractId}` |
| `project/payment/index.vue` | `/htkx/payment/detail/{paymentId}` | `/htkx/payment/edit/{paymentId}` |
| `project/subproject/index.vue` | `/task/subproject/detail/{taskId}` | `/task/subproject/edit/{taskId}` |
| `project/versionOut/index.vue` | `/project/versionOut/detail/{id}` | `/project/versionOut/edit/{id}` |
| `project/versionOutManual/index.vue` | `/project/versionOutManual/detail/{id}` | `/project/versionOutManual/edit/{id}` |
| `project/prolistDefect/index.vue` | `?problemId=` 形态 | `?problemId=` 形态 |
| `project/nobatchProlist/index.vue` | `?problemId=` 形态 | `?problemId=` 形态 |
| `revenue/team/index.vue` | `/revenue/team/detail/{projectId}` | —（弹窗，见 4.2） |

### 4.2 明确排除（无独立路由，右键新标签无意义）

以下页面的详情/编辑是 **`el-dialog` 弹窗**，不存在可 `href` 的 URL。强行支持需要为其新建路由页面，属独立改造：

`project/customer`（详情+编辑均弹窗）、`project/productionBatch`、`project/secondaryRegion`、`project/managerChange`、`project/projectMember`、`revenue/team` 的**编辑**、`system/*` 全系列。

判定依据是**读 handler 实现**：`router.push(...)` → 纳入；`open.value = true` → 排除。

`project/review`、`revenue/company`、`project/oldVersionOut`、`project/dailyReport/stats` 经确认**无操作列**，不在范围内。

## 5. 回归红线

- 列表页**查询条件缓存**（`sessionStorage` + `onBeforeRouteLeave`）在左键跳转时照常生效
- `v-hasPermi` 权限控制不失效（服务端权限本就独立校验，href 不构成绕过）
- 操作列**视觉零变化**：按钮尺寸、图标、颜色、间距与改造前一致
- 同一操作列的其他按钮（附件、查看合同、删除、分解任务等）不动

## 6. 验收标准

- [x] 操作列详情/编辑的 DOM 是 `<a>` 且 `href` 指向正确路由
- [x] 右键出现"在新标签页中打开链接"
- [x] Ctrl/⌘ + 左键、鼠标中键 → 新标签打开，**且当前页 URL 不变**
- [x] 普通左键 → 当前标签 SPA 跳转，不整页刷新
- [x] 操作列视觉与改造前一致
- [x] BDD 场景已产出并落地映射（[bdd/row-link-new-tab.feature](./bdd/row-link-new-tab.feature) + [bdd/coverage.md](./bdd/coverage.md)）
- [x] E2E **有头模式** 17/17 通过、类型检查零新增错误、生产构建成功、相关模块回归无新增失败
