# Tasks: 清理 pm_project 废弃任务字段

**Input**: Design documents from `/specs/005-cleanup-project-task-fields/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup

**Purpose**: 无需项目初始化——本次变更仅编辑 1 个已有文件

（无任务）

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 确认清理范围，避免误删活跃字段

- [x] T001 确认 `batchNo` 和 `planProductionDate` 在 `docs/gen-specs/pm_project.yml` 中未标记 DEPRECATED，记录其行号以便后续保留验证

**Checkpoint**: 已确认保护字段位置，可以开始清理

---

## Phase 3: User Story 1 - 删除 DB 列字段块 (Priority: P1) 🎯 MVP

**Goal**: 从 pm_project.yml 中删除 15 个已迁移到 pm_task 的 DB 列字段定义块

**Independent Test**: `grep -c "DEPRECATED" docs/gen-specs/pm_project.yml` 返回 0 后部分完成；YAML 可解析

### Implementation for User Story 1

- [x] T002 [US1] 删除 `docs/gen-specs/pm_project.yml` 中的分隔注释行 `# ========== 任务相关字段（从子项目迁移至 pm_task...） ==========`
- [x] T003 [US1] 删除 `docs/gen-specs/pm_project.yml` 中以下 15 个 DEPRECATED DB 列字段块（每块包含 `# DEPRECATED` 注释 + 字段定义 + `notes`）：`parent_id`, `project_level`, `task_code`, `batch_id`, `production_year`, `bank_demand_no`, `software_demand_no`, `product`, `schedule_status`, `internal_closure_date`, `functional_test_date`, `production_version_date`, `actual_production_date`, `function_description`, `implementation_plan`
- [x] T004 [US1] 删除 `docs/gen-specs/pm_project.yml` 中分组注释 `# --- Task 来源的审计字段...---` 及 6 个非 DB 字段块：`taskCreateBy`, `taskCreateTime`, `taskUpdateBy`, `taskUpdateTime`, `taskCreateByName`, `taskUpdateByName`

**Checkpoint**: 所有 21 个 DEPRECATED 字段块已删除

---

## Phase 4: User Story 2 - UI 描述同步更新 (Priority: P2)

**Goal**: 检查并更新 pm_project.yml 中 detail 页面 UI 描述中对已删除字段的引用

**Independent Test**: pm_project.yml 中 UI 描述部分不再引用已删除的字段名作为 Project 属性

### Implementation for User Story 2

- [x] T005 [US2] 检查 `docs/gen-specs/pm_project.yml` 中 detail 页面 UI 描述（如卡片6"项目关联任务列表"），确认引用的 `taskCode`、`batchNo` 等字段注明来源为 pm_task 而非 pm_project

**Checkpoint**: UI 描述与数据模型一致

---

## Phase 5: User Story 3 - 验证无残留 (Priority: P1)

**Goal**: 全面验证清理结果，确保无遗漏

**Independent Test**: grep 零匹配 + YAML 解析通过 + 保护字段保留

### Implementation for User Story 3

- [x] T006 [US3] 执行 `grep -c "DEPRECATED" docs/gen-specs/pm_project.yml` 验证返回 0
- [x] T007 [P] [US3] 执行 `python3 -c "import yaml; yaml.safe_load(open('docs/gen-specs/pm_project.yml'))"` 验证 YAML 格式正确
- [x] T008 [P] [US3] 确认 `batchNo` 和 `planProductionDate` 字段在 `docs/gen-specs/pm_project.yml` 中仍然存在且完整

**Checkpoint**: 所有验证通过，清理完成

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 收尾工作

- [x] T009 清理 `docs/gen-specs/pm_project.yml` 中因删除字段块产生的多余空行（保持文件整洁）

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: 无前置依赖，立即开始
- **User Story 1 (Phase 3)**: 依赖 T001（确认保护字段）
- **User Story 2 (Phase 4)**: 依赖 Phase 3 完成（字段删除后才能检查 UI 描述一致性）
- **User Story 3 (Phase 5)**: 依赖 Phase 3 + Phase 4 完成
- **Polish (Phase 6)**: 依赖 Phase 5 验证通过

### User Story Dependencies

- **User Story 1 (P1)**: 核心清理，必须最先完成
- **User Story 2 (P2)**: 依赖 US1 完成
- **User Story 3 (P1)**: 验证性质，依赖 US1 + US2 完成

### Parallel Opportunities

- T006、T007、T008 可并行执行（都是独立的验证命令）
- T002、T003、T004 理论上操作同一文件，建议顺序执行以避免编辑冲突

---

## Parallel Example: User Story 3 (Verification)

```bash
# 三个验证任务可同时执行：
Task T006: "grep -c DEPRECATED docs/gen-specs/pm_project.yml"
Task T007: "python3 -c 'import yaml; yaml.safe_load(open(\"docs/gen-specs/pm_project.yml\"))'"
Task T008: "grep 'batchNo\|planProductionDate' docs/gen-specs/pm_project.yml"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete T001: 确认保护字段
2. Complete T002-T004: 删除 21 个 DEPRECATED 字段块
3. **STOP and VALIDATE**: 运行 T006-T008 快速验证
4. 如果通过，MVP 即完成

### Full Delivery

1. T001 → T002-T004 → T005 → T006-T008 → T009
2. 整个流程预计可在单次会话中完成

---

## Notes

- 本次变更仅涉及 1 个文件：`docs/gen-specs/pm_project.yml`
- 无代码编译风险（spec 文件不参与编译）
- 无运行时风险（spec 文件不参与运行时）
- Commit after T004 (core cleanup) and after T009 (polish)
