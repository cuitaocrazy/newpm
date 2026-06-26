---
description: "Task list for 日报填写右侧查询条件"
---

# Tasks: 日报填写右侧查询条件

**Input**: Design documents from `/specs/013-daily-report-write-filter/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-filter.md, quickstart.md

**Tests**: 用户明确要求"本地做 e2e 测试"，故包含 Playwright e2e 任务。

**Organization**: 按用户故事分组（US1 项目名 / US2 任务名 / US3 经理）。本特性所有实现都集中在单文件 `ruoyi-ui/src/views/project/dailyReport/write.vue`，故同文件的实现任务不可并行（无 [P]）；e2e 测试为独立新文件，可并行编写。

## Path Conventions

- 前端：`ruoyi-ui/src/views/project/dailyReport/write.vue`（唯一改动文件）
- 测试：`tests/e2e-daily-report-write-filter.spec.js`（新增）

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 无新依赖、无脚手架；仅确认基线。

- [x] T001 确认本地环境就绪：后端 8085、前端 80（`npm run dev`）、MySQL/Redis 运行，账号 `admin`/`123456789` 可登录（参见 `specs/013-daily-report-write-filter/quickstart.md`）

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 所有用户故事共用的查询条件栏骨架与状态/派生基础设施，必须先于 US1/US2/US3 完成。

- [x] T002 在 `ruoyi-ui/src/views/project/dailyReport/write.vue` 的 `<script setup>` 中新增三个 reactive ref：`filterProjectName=''`、`filterTaskName=''`、`filterManager=''`（会话内视图状态，切换日期不重置）
- [x] T003 在 `write.vue` 右侧 `el-col(span=17)` 卡片内、项目/任务列表（含假期区块）上方新增查询条件栏容器（一行三控件：项目名 `el-input` clearable、任务名 `el-input` clearable、项目经理 `el-select` clearable），仅在 `!isWhitelisted` 时渲染（FR-001 / FR-011），样式与现有卡片协调
- [x] T004 在 `write.vue` 新增 `computed filteredFormList`，对 `formList` 派生只读可见列表（暂时直通返回 `formList`，为 US1–US3 填充过滤条件预留），并把模板里项目卡片 `v-for` 由 `formList` 改为遍历 `filteredFormList`（保持 `handleSave`/`handleDelete` 仍遍历 `formList` 不变——关键不变量 I1/I2）

**Checkpoint**: 条件栏可见、列表照常渲染、保存/删除逻辑未受影响。

---

## Phase 3: User Story 1 - 按项目名称模糊查找 (Priority: P1) 🎯 MVP

**Goal**: 输入项目名片段，右侧只剩名称匹配的项目卡片（不区分大小写）；清空恢复。

**Independent Test**: 用参与多项目账号，项目名输入框键入片段→列表实时收窄；清空→恢复全部（spec 验收 US1）。

### Implementation

- [x] T005 [US1] 在 `write.vue` `filteredFormList` 中加入项目名条件：`filterProjectName` 非空时，按 `item.projectName.toLowerCase().includes(kw)` 过滤（FR-002）
- [x] T006 [US1] 绑定项目名 `el-input` 的 `v-model` 到 `filterProjectName`，确认输入实时驱动 `filteredFormList`（无需查询按钮，FR-006/SC-002）
- [x] T007 [US1] 在 `write.vue` 新增空状态提示：当 `filteredFormList.length===0` 且存在任一过滤条件时显示「没有符合筛选条件的项目」，区别于既有 `projects.length===0` 的「暂无参与的项目」（FR-009）

**Checkpoint**: US1 可独立演示——项目名过滤 + 清空恢复 + 空状态。

---

## Phase 4: User Story 2 - 按任务名称模糊查找 (Priority: P2)

**Goal**: 输入任务名片段，仅显示含匹配任务的项目，且卡片内仅展示匹配任务行；普通项目（无子任务）被排除。

**Independent Test**: 含多子任务项目账号，任务名输入片段→只剩含匹配任务的项目，卡片内只显示匹配任务行（spec 验收 US2）。

### Implementation

- [x] T008 [US2] 在 `write.vue` `filteredFormList` 中加入任务名条件：`filterTaskName` 非空时，仅保留 `item.hasSubProject` 且 `(item.taskRows||[]).some(t => t.taskName.toLowerCase().includes(taskKw))` 的项目；普通项目被排除（FR-003 / 验收2）
- [x] T009 [US2] 在含子任务项目的任务行 `v-for` 上套展示层过滤：`filterTaskName` 非空时仅渲染 `taskName` 匹配的任务行；**不修改原 `item.taskRows`**（保证保存不丢工时，FR-007 / 不变量 I2）
- [x] T010 [US2] 绑定任务名 `el-input` 的 `v-model` 到 `filterTaskName`，确认与项目名条件 AND 组合（FR-005）

**Checkpoint**: US2 可独立演示——任务名过滤收窄到项目+任务行，且与 US1 联合生效。

---

## Phase 5: User Story 3 - 按项目经理下拉筛选 (Priority: P3)

**Goal**: 下拉选项=参与项目经理去重集合；选一个→只剩该经理项目；清空→恢复。

**Independent Test**: 参与多经理项目账号，下拉选项正确、选中过滤、清空恢复（spec 验收 US3）。

### Implementation

- [x] T011 [US3] 在 `write.vue` 新增 `computed managerOptions`：从 `formList` 收集非空 `projectManagerName` 去重（Set）并排序，作为经理 `el-select` 选项（FR-004 / SC-004）
- [x] T012 [US3] 在 `filteredFormList` 中加入经理条件：`filterManager` 非空时按 `item.projectManagerName === filterManager` 过滤；绑定 `el-select` 的 `v-model` 与 `:options`，确认三条件 AND 组合（FR-005）

**Checkpoint**: US3 可独立演示——经理下拉过滤，三条件可叠加。

---

## Phase 6: E2E 测试与本地验证

**Purpose**: 用户要求"本地做 e2e 测试，测试通过后看结果"。覆盖 contracts/ui-filter.md 的 C1–C7。

- [x] T013 [P] 新增 `tests/e2e-daily-report-write-filter.spec.js`，参考 `tests/project-management.spec.js` 的 UI 登录模式：表单登录→导航 `/project/dailyReport/write`→断言三个查询控件存在
- [x] T014 [P] 在该 spec 中补充用例：项目名过滤（可见项目卡片数收窄）、清空恢复（C2/C7）；任务名过滤（项目+任务行收窄、普通项目排除，C3）；经理下拉（选项来自参与项目、选中过滤、清空恢复，C4）；空状态（C6）；保存不丢工时（在过滤隐藏某行后保存→重载校验工时仍在）
- [x] T015 临时关闭登录验证码（用户记忆 `feedback_e2e_captcha_toggle`），确保前端(80)+后端(8085)在跑，执行 `npx playwright test e2e-daily-report-write-filter.spec.js`；跑完恢复验证码配置
- [x] T016 `npx playwright show-report` 收集结果，确认全部通过（retries=2, workers=1）

---

## Phase 7: Polish & Cross-Cutting

- [x] T017 自查不变量：`handleSave`/`handleDelete` 仍遍历 `formList`（非 `filteredFormList`），过滤逻辑全程未写入 `formList`/`taskRows`（不变量 I1/I2/I3）
- [x] T018 [P] 视觉/交互打磨：条件栏在窄屏不换乱、clearable 图标可用、与「保存日报」按钮区不冲突；切换日期后条件保留（FR-010）核对

---

## Dependencies & Execution Order

- **Phase 1 (Setup)** → **Phase 2 (Foundational)** 必须先完成（条件栏骨架 + `filteredFormList` 直通）。
- **US1 / US2 / US3** 均依赖 Phase 2，但彼此逻辑独立（都往同一个 `filteredFormList` 叠加条件 + 各自绑定控件）。因同改 `write.vue`，实现任务**顺序执行**（无 [P]）；建议按 P1→P2→P3。
- **Phase 6 (E2E)** 依赖 US1–US3 实现完成；spec 文件编写（T013/T014）可与实现并行起草，但运行（T015/T016）需实现就绪。
- **Phase 7** 收尾。

## Implementation Strategy

- **MVP = Phase 1 + 2 + US1**：仅项目名过滤即可交付可用价值（用户核心痛点）。
- 增量叠加 US2（任务名）、US3（经理），每步可独立验证。
- 最后统一跑 e2e（Phase 6）并人工核对不变量（Phase 7）。

## Format Validation

- 全部任务含 `- [ ]` + `T0xx` + 描述 + 文件路径；用户故事任务含 `[US1]/[US2]/[US3]`；Setup/Foundational/E2E/Polish 无故事标签；`[P]` 仅用于独立文件任务（T013/T014/T018）。✅
