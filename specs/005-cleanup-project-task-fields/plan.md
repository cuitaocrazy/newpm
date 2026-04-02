# Implementation Plan: 清理 pm_project 废弃任务字段

**Branch**: `005-cleanup-project-task-fields` | **Date**: 2026-04-01 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-cleanup-project-task-fields/spec.md`

## Summary

从 `docs/gen-specs/pm_project.yml` 中移除 21 个标记为 DEPRECATED 的已迁移任务字段定义。研究表明 Java Entity、Mapper XML、DDL 均已在此前迁移中清理完毕，本次仅需清理 spec 文件。

## Technical Context

**Language/Version**: YAML (spec 文件，非运行时代码)  
**Primary Dependencies**: 无（文档文件编辑）  
**Storage**: N/A  
**Testing**: grep 验证 + YAML 格式校验  
**Target Platform**: N/A（开发文档）  
**Project Type**: 技术债清理（文档层面）  
**Performance Goals**: N/A  
**Constraints**: 不得误删活跃字段（`batchNo`、`planProductionDate`）  
**Scale/Scope**: 1 个文件，删除约 21 个字段块 + 分隔注释

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. 业务完整性优先 | PASS | 不涉及 mutating 接口，无需 @Log |
| II. 权限驱动访问控制 | PASS | 不涉及 Controller/API 变更 |
| III. API 与代码结构一致性 | PASS | 不涉及代码变更 |
| **IV. 关注点分离：任务与项目解耦** | **PASS — 正是此原则的落地** | 清理 pm_project spec 中的任务字段残留，完成迁移收尾 |
| V. 数据库规范 | PASS | DDL 已清理，不涉及 SQL 变更 |
| VI. 前端组件与字典规范 | PASS | 不涉及前端变更 |

**Post-design re-check**: 所有门禁通过。本次变更完全符合宪法 Principle IV 的要求。

## Project Structure

### Documentation (this feature)

```text
specs/005-cleanup-project-task-fields/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: research findings
├── data-model.md        # Phase 1: affected entities
├── quickstart.md        # Phase 1: implementation quickstart
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (by /speckit.tasks)
```

### Source Code (repository root)

```text
docs/gen-specs/
└── pm_project.yml       # 唯一需要编辑的文件：删除 21 个 DEPRECATED 字段块
```

**Structure Decision**: 纯文档清理，不涉及 `src/` 目录下任何代码文件变更。

## Research Findings Summary

详见 [research.md](research.md)。关键发现：

| 层 | 状态 | 需要变更？ |
|---|---|---|
| Project.java | 已清理 | 否 |
| ProjectMapper.xml | 已清理 | 否 |
| DDL (pm_project) | 已清理 | 否 |
| 前端 Vue/API | 无残留引用 | 否 |
| **pm_project.yml** | **21 个 DEPRECATED 字段残留** | **是** |

## Implementation Steps

### Step 1: 删除 pm_project.yml 中的 DEPRECATED 字段块

从 `docs/gen-specs/pm_project.yml` 中删除：

1. **15 个 DB 列字段块**（`parent_id` ~ `implementation_plan`），每个包含字段注释 `# DEPRECATED: migrated to pm_task` + 完整的 YAML 定义块 + `notes` 说明
2. **分隔注释行**：`# ========== 任务相关字段（从子项目迁移至 pm_task...） ==========`
3. **6 个非 DB 字段块**（`taskCreateBy` ~ `taskUpdateByName`），位于 `nonDbFields` 或类似区域
4. **相关分组注释**：`# --- Task 来源的审计字段...---`

### Step 2: 保留验证

确认以下字段未被删除：
- `batchNo` — 批次号关联字段
- `planProductionDate` — 计划投产日期关联字段

### Step 3: 格式验证

- `grep -c "DEPRECATED" docs/gen-specs/pm_project.yml` → 期望 0
- YAML 格式校验通过

### Step 4: 更新 spec.md

将 spec 中 User Story 1 的范围从"Project.java 清理"修正为"仅 pm_project.yml 清理"（反映研究发现）。

## Complexity Tracking

无宪法违规，无需记录。
