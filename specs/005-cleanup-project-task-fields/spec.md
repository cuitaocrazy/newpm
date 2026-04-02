# Feature Specification: 清理 pm_project 废弃任务字段

**Feature Branch**: `005-cleanup-project-task-fields`  
**Created**: 2026-04-01  
**Status**: Draft  
**Input**: 清理 pm_project 中已迁移到 pm_task 的废弃字段，包括 Java Entity 和 Spec 文件

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 清理 Spec 文件中的废弃字段定义 (Priority: P1)

作为开发者，我需要从 `pm_project.yml` spec 文件中移除 21 个标记为 DEPRECATED 的已迁移任务字段定义，完成迁移收尾工作，防止代码生成工具误将这些字段重新生成到 pm_project 模块。

**Why this priority**: 这是核心目标。经研究确认 Java Entity、Mapper XML、DDL 均已在此前迁移中清理完毕，pm_project.yml 是唯一残留。spec 文件是代码生成蓝图，残留废弃字段有被误生成的风险。

**Independent Test**: 清理后在 yml 文件中搜索 "DEPRECATED" 关键词，零匹配。

**Acceptance Scenarios**:

1. **Given** pm_project.yml 中 21 个 DEPRECATED 字段块已删除, **When** 搜索 "DEPRECATED" 或 "migrated to pm_task", **Then** 零匹配
2. **Given** pm_project.yml 已清理, **When** 检查 `batchNo` 和 `planProductionDate` 字段, **Then** 仍然保留（未标记 DEPRECATED）
3. **Given** pm_project.yml 已清理, **When** YAML 格式校验, **Then** 解析成功无错误

---

### User Story 2 - Spec 文件清理以防止误生成 (Priority: P2)

作为开发者，我需要从 `pm_project.yml` spec 文件中移除所有标记为 DEPRECATED 的字段块，确保未来使用代码生成工具时不会把这些字段重新生成到 pm_project 模块中。

**Why this priority**: spec 文件是代码生成的蓝图，残留的废弃字段有被误生成的风险，但短期内不会造成运行时问题。

**Independent Test**: 检查 pm_project.yml 中不再包含任何 "DEPRECATED" 或 "migrated to pm_task" 标记。

**Acceptance Scenarios**:

1. **Given** pm_project.yml 已清理, **When** 搜索 "DEPRECATED" 关键词, **Then** 无匹配结果
2. **Given** pm_project.yml 已清理, **When** 查看 detail 页面的 UI 描述部分, **Then** 引用已迁移字段的 UI 描述同步更新（如卡片6的任务关联列表描述应引用 pm_task 字段）

---

### User Story 3 - 确保无残留引用 (Priority: P1)

作为开发者，我需要确认代码库中没有其他文件（Controller、Service、前端组件等）仍在引用这些废弃字段，避免删除后出现编译错误或运行时异常。

**Why this priority**: 如果有残留引用但未处理，删除字段会直接导致编译失败或运行时错误。

**Independent Test**: 全局搜索废弃字段名，确认除 spec 文件和计划文档外无其他引用。

**Acceptance Scenarios**:

1. **Given** 废弃字段已从 Project.java 删除, **When** 全局搜索 `taskCode`/`batchId`/`productionYear` 等字段名, **Then** 仅在 pm_task 相关文件和文档中出现，不在 pm_project 相关代码中出现
2. **Given** 所有清理完成, **When** 运行 `mvn clean compile -pl ruoyi-project -am`, **Then** 编译成功

---

### Edge Cases

- 如果 `batchNo` 和 `planProductionDate`（非 DB 字段，未标记 DEPRECATED）仍被 TaskMapper 用于查询结果映射到 Project 对象，则不应删除
- 如果 `toString()` 方法引用了废弃字段，需同步清理
- 如果 Excel 导出注解（`@Excel`）标注在废弃字段上，需确认导出模板是否受影响

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 必须从 `pm_project.yml` 中删除所有标记为 "DEPRECATED: migrated to pm_task" 的字段块（共 21 个：15 个 DB 列 + 6 个非 DB 字段）
- **FR-002**: 必须删除相关分隔注释行（如 `# ========== 任务相关字段...`、`# --- Task 来源的审计字段...`）
- **FR-003**: 不得删除 `batchNo` 和 `planProductionDate` 两个非 DB 字段（未标记 DEPRECATED，仍被 TaskMapper 使用）
- **FR-004**: 清理后 YAML 格式必须仍可正确解析
- ~~**FR-005 (已完成)**: Project.java 中废弃字段已在此前迁移中清理完毕，无需再次操作~~
- ~~**FR-006 (已完成)**: ProjectMapper.xml 和 DDL 已在此前迁移中清理完毕，无需再次操作~~

### Key Entities

- **Project** (`pm_project`): 项目管理主实体，删除 21 个废弃字段后保留所有活跃业务字段
- **Task** (`pm_task`): 任务管理实体，这 21 个字段的新归属地，不受本次变更影响

## Assumptions

- DDL (`00_tables_ddl.sql`) 中 pm_project 表已不包含这些列（已确认）
- `ProjectMapper.xml` 已不引用这些字段（已确认）
- 生产数据库 pm_project 表中这些列可能仍然存在（ALTER TABLE DROP COLUMN 不在本次范围内，属于后续 DBA 操作）
- `batchNo` 和 `planProductionDate` 的保留/删除需在实施阶段通过代码搜索确认

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `pm_project.yml` 中不再包含任何 "DEPRECATED" 标记的字段（grep 计数 = 0）
- **SC-002**: `pm_project.yml` YAML 格式校验通过
- **SC-003**: `batchNo` 和 `planProductionDate` 字段保留完好
