# Research: 清理 pm_project 废弃任务字段

**Date**: 2026-04-01

## Research Task 1: Project.java 废弃字段是否仍存在？

**Decision**: Project.java 已完全清理，无需修改

**Rationale**: 通过 grep 搜索 `taskCode`、`parentId`、`projectLevel`、`batchId`、`productionYear`、`bankDemandNo`、`softwareDemandNo`、`scheduleStatus`、`functionDescription`、`implementationPlan`、`taskCreateBy`、`taskUpdateBy` 等关键词，Project.java 中零匹配。这些字段已在此前的迁移中被删除。

**Alternatives considered**: 无需考虑替代方案——字段已不存在。

## Research Task 2: ProjectMapper.xml 是否仍引用废弃字段？

**Decision**: ProjectMapper.xml 已完全清理，无需修改

**Rationale**: grep 搜索 `task_code`、`project_level`、`bank_demand_no` 等 snake_case 字段名，零匹配。Mapper 中出现的 `parent_id` 仅用于 `sys_dept` 表的部门层级查询，与项目废弃字段无关。

## Research Task 3: DDL 中 pm_project 表是否还有废弃列？

**Decision**: `00_tables_ddl.sql` 中 pm_project 表定义已清理（行 722-794），67 个列中不含任何任务相关废弃列

**Rationale**: 直接阅读 DDL 确认。废弃字段相关的匹配（`task_code`、`bank_demand_no` 等）仅出现在 `pm_task` 表定义中（正确位置）。

## Research Task 4: 前端/API 层是否有残留引用？

**Decision**: 无残留引用

**Rationale**: `ruoyi-ui/src/views/project/subproject/` 下的 Vue 文件中出现的 `taskCode`、`functionDescription`、`implementationPlan` 等字段均属于 Task 上下文（页面标题为"新增任务"、"编辑任务"等），不是 Project 上下文的引用。

## Research Task 5: pm_project.yml spec 文件中的废弃字段

**Decision**: pm_project.yml 是唯一需要清理的文件，包含 21 个 DEPRECATED 字段块

**Rationale**: 
- 15 个 DB 列字段：`parent_id`, `project_level`, `task_code`, `batch_id`, `production_year`, `bank_demand_no`, `software_demand_no`, `product`, `schedule_status`, `internal_closure_date`, `functional_test_date`, `production_version_date`, `actual_production_date`, `function_description`, `implementation_plan`
- 6 个非 DB 字段：`taskCreateBy`, `taskCreateTime`, `taskUpdateBy`, `taskUpdateTime`, `taskCreateByName`, `taskUpdateByName`
- 另有分隔注释行（如 `# ========== 任务相关字段...`）需一并删除

**Alternatives considered**: 保留 DEPRECATED 字段作为历史文档 — 已拒绝，因为迁移已完成，保留只会增加噪音和误生成风险。

## Research Task 6: batchNo 和 planProductionDate 的状态

**Decision**: 这两个非 DB 字段未标记 DEPRECATED，不在本次清理范围内

**Rationale**: `batchNo` 和 `planProductionDate` 在 yml 中未标记废弃，它们是 TaskMapper 查询时从 `pm_production_batch` 表 JOIN 获取的关联显示字段。需单独评估是否迁移到 Task 域。

## Conclusion

**本次变更范围大幅缩小**：仅需清理 `docs/gen-specs/pm_project.yml` 中的 21 个 DEPRECATED 字段块及相关注释。Java、XML、DDL、前端代码均已在此前迁移中清理完毕。
