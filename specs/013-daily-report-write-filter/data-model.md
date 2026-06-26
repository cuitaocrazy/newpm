# Data Model: 日报填写右侧查询条件

**Feature**: 013-daily-report-write-filter | **Date**: 2026-06-26

本特性无数据库实体、无后端 DTO 变更。以下为**前端视图模型**（`write.vue` 内的 reactive 状态与派生）。

## 既有数据（只读，来自接口）

### 参与项目项（`formList[]` 元素，源自 `getMyProjects`）

| 字段 | 类型 | 过滤用途 |
|---|---|---|
| `projectId` | number | 行 key |
| `projectName` | string | **项目名称模糊过滤目标** |
| `projectManagerName` | string | **项目经理下拉过滤目标 + 下拉选项来源** |
| `hasSubProject` | boolean | 决定是否参与任务名过滤（仅含子任务的项目有任务行） |
| `taskRows` | array \| null | 子任务行（含子任务项目），任务名过滤的容器 |
| `workHours` / `workContent` / `workCategory` | — | 已录入数据（过滤**不得**触碰） |

### 任务行（`item.taskRows[]` 元素，源自 `getTaskOptions`）

| 字段 | 类型 | 过滤用途 |
|---|---|---|
| `subProjectId`(=taskId) | number | 行 key |
| `taskName` | string | **任务名称模糊过滤目标** |
| `workHours` / `workContent` / `workCategory` | — | 已录入数据（过滤**不得**触碰） |

## 新增视图状态（reactive refs）

| 名称 | 类型 | 初值 | 说明 |
|---|---|---|---|
| `filterProjectName` | `Ref<string>` | `''` | 项目名称关键字 |
| `filterTaskName` | `Ref<string>` | `''` | 任务名称关键字 |
| `filterManager` | `Ref<string>` | `''` | 所选项目经理姓名（空=不约束） |

> 三者为会话内状态，切换 `selectedDate` 不重置（FR-010）。

## 新增派生（computed）

### `managerOptions: string[]`

```
from formList → map(projectManagerName) → filter(非空) → 去重(Set) → 排序
```
作为项目经理 `el-select` 的选项（FR-004 / SC-004）。

### `filteredFormList: ProjectItem[]`

对 `formList` 派生（不修改原数组）：

```
keep(item) ==
  (filterProjectName=='' || item.projectName.toLowerCase().includes(kw.toLowerCase()))
  &&
  (filterManager=='' || item.projectManagerName === filterManager)
  &&
  (filterTaskName=='' ||
     (item.hasSubProject && (item.taskRows||[]).some(t => t.taskName?.toLowerCase().includes(taskKw.toLowerCase()))))
```

- 任务名条件为空时，普通项目（无子任务）正常保留。
- 任务名条件非空时，普通项目被排除（FR-003 验收 2）。
- 三条件 AND（FR-005）。

### 任务行展示层过滤（模板内，不新增状态）

含子任务项目渲染 `taskRows` 时：
```
filterTaskName=='' ? 全部 taskRows : taskRows.filter(t => t.taskName 匹配)
```
仅影响显示，原 `taskRows` 不变 → 保存遍历 `formList` 时仍是全量（FR-007）。

## 不变量（Invariants）

- **I1**: `handleSave` / `handleDelete` 始终遍历 `formList`，绝不读 `filteredFormList`。→ 保存不丢工时（SC-003）。
- **I2**: 过滤逻辑全程不写 `formList`、`taskRows` 及其内的工时字段。
- **I3**: 全部条件为空 ⇒ `filteredFormList` 等价于 `formList`，任务行全展示（FR-008 / SC-005）。
