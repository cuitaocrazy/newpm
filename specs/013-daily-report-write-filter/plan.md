# Implementation Plan: 日报填写右侧查询条件

**Branch**: `013-daily-report-write-filter` | **Date**: 2026-06-26 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/013-daily-report-write-filter/spec.md`

## Summary

在「日报填写」页面（`ruoyi-ui/src/views/project/dailyReport/write.vue`）右侧编辑区顶部，新增一行查询条件栏：项目名称模糊输入、任务名称模糊输入、项目经理下拉。三者以 AND 关系对**已加载**的项目/任务列表做纯前端实时过滤（显示/隐藏语义，不丢已录入工时）。无需任何后端改动——所需字段（`projectName`、`projectManagerName`、`hasSubProject`、任务的 `taskName`）均已由 `/project/dailyReport/myProjects` 与 `/project/task/options` 接口返回。

## Technical Context

**Language/Version**: TypeScript 5.6 / Vue 3.5（前端 only）
**Primary Dependencies**: Element Plus 2.13（`el-input` / `el-select` / `el-option`），既有 `formList` 响应式数据
**Storage**: N/A（无持久化；查询条件为会话内 reactive 视图状态）
**Testing**: Playwright（UI 级 e2e，`tests/` 目录，chromium，baseURL `http://localhost:80`）
**Target Platform**: 浏览器（桌面，zh-CN）
**Project Type**: Web application（仅触及 frontend）
**Performance Goals**: 过滤为内存计算，结果实时（<1s，无明显延迟），无额外网络请求
**Constraints**: 不修改后端接口；不破坏既有保存/删除/假期记录逻辑；过滤不得丢失已录入数据
**Scale/Scope**: 单个 `.vue` 文件改动 + 1 个新 e2e spec；典型用户参与项目数 5–20+，任务数十级

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. 业务完整性优先**：本特性不涉及财务/合同/款项的 mutating 操作，纯展示过滤，不触发审计日志需求。保存逻辑保持不变（仍 `@Log` 于 `DailyReportController`）。✅ 不适用 / 无违反
- **II. 权限驱动访问控制**：无新增接口；复用既有 `getMyProjects` / `getTaskOptions`（已带权限）。前端不直接调用 `system/` 接口。✅ 通过
- **III. API 与代码结构一致性**：无后端改动；前端继续用 `import request`、`res.data`/`res.rows` 既有约定（本特性甚至不发新请求）。✅ 通过
- **IV. 任务与项目解耦**：过滤逻辑读取 `pm_task` 来源的任务字段（`taskName`），不向 `pm_project` 添加任何字段；不改 `update_by/update_time`。✅ 通过
- **V. 数据库规范**：无 SQL / schema 改动。✅ 不适用
- **VI. 前端组件与字典规范**：项目经理下拉的选项来自已加载数据的去重集合（非字典值，不需 `dict-select`）；不直接调 `system/user`。普通文本/姓名过滤无字典依赖。✅ 通过

**结论**：无宪法违反项，Complexity Tracking 留空。

## Project Structure

### Documentation (this feature)

```text
specs/013-daily-report-write-filter/
├── plan.md              # This file
├── spec.md              # Feature spec
├── research.md          # Phase 0 output（设计取舍）
├── data-model.md        # Phase 1 output（前端视图模型 & 过滤规则）
├── quickstart.md        # Phase 1 output（本地验证步骤）
├── contracts/
│   └── ui-filter.md     # 前端 UI 过滤“契约”（输入→可见结果映射）
└── checklists/
    └── requirements.md  # 已有：规格质量校验
```

### Source Code (repository root)

```text
ruoyi-ui/
└── src/
    └── views/
        └── project/
            └── dailyReport/
                └── write.vue        # 唯一改动文件：新增查询条件栏 + computed 过滤

tests/
└── e2e-daily-report-write-filter.spec.js   # 新增：UI 级 e2e（项目名/任务名/经理过滤）
```

**Structure Decision**: Web application 结构，本特性仅触及前端单文件 `write.vue` 与一个新增 Playwright spec。无后端、无数据库、无新 API。

## 实现要点（技术方案）

1. **查询条件状态**：在 `write.vue` `<script setup>` 内新增三个 ref：`filterProjectName`（string）、`filterTaskName`（string）、`filterManager`（string，项目经理姓名）。它们是会话内视图状态，不随 `selectedDate` 切换被清空（FR-010）。

2. **项目经理下拉选项**：新增 `computed` `managerOptions`，从 `formList`（或 `projects`）去重收集非空 `projectManagerName`，供 `el-select` 渲染（FR-004 / SC-004）。

3. **过滤计算**：新增 `computed` `filteredFormList`，对 `formList` 派生：
   - 项目名称：`item.projectName` 不区分大小写子串匹配 `filterProjectName`。
   - 项目经理：`item.projectManagerName === filterManager`（未选则不约束）。
   - 任务名称：若 `filterTaskName` 非空 → 仅保留 `hasSubProject` 且 `taskRows` 中存在 `taskName` 匹配的项目；对保留项目，渲染时只展示匹配的任务行（通过派生的 `visibleTaskRows` 或模板内 `taskRows.filter(...)`，**不修改原 `taskRows` 数据**，保证 FR-007 / 保存不丢工时）。
   - 三条件 AND 组合（FR-005）。
   - 模板把 `v-for="(item, index) in formList"` 改为遍历 `filteredFormList`；任务行 `v-for` 套一层任务名过滤（仅展示层）。

4. **空状态**：当 `filteredFormList.length === 0` 且存在过滤条件时，展示「没有符合筛选条件的项目」提示（FR-009）。区分于既有「暂无参与的项目」（`projects.length === 0`）。

5. **保存不受影响**：`handleSave` 继续遍历 `formList`（完整数据），不读 `filteredFormList`，确保被过滤隐藏的工时仍保存（FR-007 / SC-003）。这是关键不变量。

6. **可见性约束**：查询条件栏仅在 `!isWhitelisted` 且右侧编辑区渲染时出现（FR-011）。

## Complexity Tracking

> 无宪法违反项，无需填写。
