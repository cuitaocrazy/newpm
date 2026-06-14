# Feature Specification: 旧数据查询（出入库版本历史归档）

**Branch**: `009-old-version-query` | **Created**: 2026-06-14 | **Status**: Draft
**Input**: 迁移自老 yadapm「出入库版本管理 → 旧数据查询」(`/oldStorage`)。纯只读历史归档，独立表 `pm_old_version_out`，无增删改导、无级联。
**需求**: `docs/pm/yadapm需求-03-旧数据查询.md`（已源码核实）

## User Scenarios & Testing

### User Story 1 - 查询旧版本历史数据 (Priority: P1)
版本管理人员检索从旧系统迁移来的"出入库版本"历史记录，按任务编号/投产批次号/子产品/版本类型筛选，分页查看。

**Why this priority**: 这是本模块唯一价值——只读检索历史。
**Independent Test**: 库中有历史数据时，按各条件筛选，列表正确分页过滤。

**Acceptance Scenarios**:
1. **Given** 库中有历史记录，**When** 进入页面，**Then** 分页展示全部历史记录（18 列）。
2. **Given** 输入任务编号（文本），**When** 查询，**Then** 模糊匹配过滤。
3. **Given** 选投产批次号/子产品/版本类型下拉，**When** 查询，**Then** 精确过滤；下拉选项来自历史数据 distinct。

### Edge Cases
- 数据未迁移时页面为空（功能可先上线，数据后补）。

## Requirements

- **FR-001**: 系统 MUST 提供旧版本历史数据的分页查询，条件：任务编号(模糊)、投产批次号、子产品、版本类型。
- **FR-002**: 系统 MUST 展示 18 列（见数据模型），**无操作列**（纯只读）。
- **FR-003**: 系统 MUST 提供 投产批次号/子产品/版本类型 三个下拉的选项（来自 `pm_old_version_out` distinct）。
- **FR-004**: 系统 MUST 用权限 `project:oldVersionOut:list` 保护查询接口。
- **FR-005**: 系统 MUST NOT 提供新增/编辑/删除/导出（旧系统无，纯归档只读）。

## Key Entities
- **旧版本归档**：独立表 `pm_old_version_out`，扁平快照。所有字段为迁移定格的文本/值，**不与任务库/项目库/字典实时关联**（提交人员、版本类型均存文本）。

## Success Criteria
- **SC-001**: 按任一条件筛选，结果准确率 100%。
- **SC-002**: 与 pm_version_out（批次/非批次）零关联、零串扰——独立表独立查询。

## Assumptions
- **独立只读表**：新建 `pm_old_version_out`，不复用 pm_version_out（结构语义都不同）。
- **下拉从表 distinct 取**：版本类型等存历史文本，不接字典。
- **数据迁移延后**：历史数据导入（T_B_OLD_VERSION_OUT → pm_old_version_out）属独立后续任务，功能先建空表。
- **字段保真**：提交人员等存文本快照，不映射 sys_user。
