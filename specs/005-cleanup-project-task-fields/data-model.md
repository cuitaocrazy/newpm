# Data Model: 清理 pm_project 废弃任务字段

**Date**: 2026-04-01

## Entities Affected

### pm_project (Project)

**变更类型**: 无代码变更（Java/Mapper/DDL 已清理完毕）

**Spec 文件清理**: 从 `docs/gen-specs/pm_project.yml` 中移除以下 21 个 DEPRECATED 字段定义：

#### 15 个 DB 列字段（已迁移到 pm_task）

| 字段 (snake_case) | Java 字段 | 说明 |
|---|---|---|
| `parent_id` | parentId | 父项目ID |
| `project_level` | projectLevel | 项目层级 |
| `task_code` | taskCode | 子项目编号 |
| `batch_id` | batchId | 投产批次ID |
| `production_year` | productionYear | 投产年份 |
| `bank_demand_no` | bankDemandNo | 总行需求号 |
| `software_demand_no` | softwareDemandNo | 软件中心需求编号 |
| `product` | product | 二级产品 |
| `schedule_status` | scheduleStatus | 排期状态 |
| `internal_closure_date` | internalClosureDate | 内部B包日期 |
| `functional_test_date` | functionalTestDate | 功能测试版本日期 |
| `production_version_date` | productionVersionDate | 生产版本日期 |
| `actual_production_date` | actualProductionDate | 实际投产日期 |
| `function_description` | functionDescription | 功能点说明 |
| `implementation_plan` | implementationPlan | 实施计划 |

#### 6 个非 DB 字段（TaskMapper 代理返回字段）

| Java 字段 | 说明 |
|---|---|
| `taskCreateBy` | 任务创建人 |
| `taskCreateTime` | 任务创建时间 |
| `taskUpdateBy` | 任务更新人 |
| `taskUpdateTime` | 任务更新时间 |
| `taskCreateByName` | 任务创建人昵称 |
| `taskUpdateByName` | 任务更新人昵称 |

### pm_task (Task) — 不受影响

这些字段的正确归属地，不做任何变更。

## 不在范围内

- `batchNo`、`planProductionDate` — 未标记 DEPRECATED，不清理
- 生产数据库 ALTER TABLE DROP COLUMN — 属于 DBA 后续操作
- `pm_project.yml` 中 UI 描述（detail 页面卡片6引用的 task 字段）— 这些描述引用的是 Task 实体字段，属于正确引用
