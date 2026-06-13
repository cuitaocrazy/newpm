# Implementation Plan: 非批次版本管理

**Branch**: `008-non-batch-version-management` | **Date**: 2026-06-13 | **Spec**: [spec.md](./spec.md)
**需求**: `docs/pm/yadapm需求-02-非批次版本管理.md`（已源码核实）| **蓝本**: spec 007 批次版本管理

## Summary

迁移老 yadapm `storageManual`。核心是**裁剪+区分**：与批次共用 `pm_version_out`（`manual_input='1'`）和同一个 `VersionNumberGenerator`，但任务信息改**手填**（加 `manual_task_no`/`manual_task_name` 两列）、砍掉组包/状态/简介/多任务/任务下拉，独立权限 `project:versionOutManual:*` + 独立二级菜单。

技术路线：复用批次的 domain/mapper/service/字典/子系统表；新增 `VersionOutManualController`（或在 VersionOut 体系内加 manual 端点）+ 一套前端四件套，查询硬编码 `manual_input='1'`。

## Technical Context

**Language**: Java 17 / TS 5.6 | **依赖**: Spring Boot 3.5.8、MyBatis、POI、Vue3.5、Element Plus
**Storage**: MySQL（`pm_version_out` 加 2 列）| **Testing**: JUnit5+Mockito、Playwright E2E、JaCoCo
**复用**: `VersionNumberGenerator`（版本号算法，逐字相同）、`pm_sys_name`、字典 `sys_version_type`、`pm_production_batch`

## Constitution Check

| 原则 | 合规 | 说明 |
|---|---|---|
| I 业务完整性 | ✅ | @Log 审计、软删除 del_flag |
| II 权限驱动 | ✅ | `@PreAuthorize('project:versionOutManual:*')`；用户/部门走代理API |
| III API一致性 | ✅ | BaseController、startPage、TableDataInfo/AjaxResult、命名规范 |
| IV 任务/项目解耦 | ✅ | 任务字段手填存主表 manual 列，不动 pm_project；不进 pm_task |
| V 数据库规范 | ✅ | utf8mb4；JOIN sys_user 加 COLLATE；手填任务无需 JOIN |
| VI 前端字典 | ✅ | dict-select/dict-tag；服务端排序 |

**Gate**: 无违规。复用批次唯一键+重试机制防并发。

## Project Structure

```
后端（ruoyi-project，复用 VersionOut 体系）：
- domain/VersionOut.java：加 manualTaskNo/manualTaskName 字段（批次不用、非批次用）
- mapper：复用 VersionOutMapper，加 manual 列的 insert/update + manual_input='1' 查询
  （或新增 selectManualList 等，硬编码 manual_input='1'）
- controller/VersionOutManualController.java：/project/versionOutManual/**（独立权限）
- service：复用 IVersionOutService，加 manual 专用方法 或 共用带 manualInput 参数

前端（ruoyi-ui）：
- views/project/versionOutManual/{index,add,edit,detail}.vue（裁剪版，参考 versionOut/）
- api/project/versionOutManual.ts

数据库：
- pm-sql/init/00_tables_ddl.sql：ALTER pm_version_out 加 2 列
- 02_menu_data.sql：二级菜单"非批次版本管理" + 6 权限点
- pm-sql/migration_008_non_batch_version_management.sql（可移植）
```

**Structure Decision**: 后端尽量复用 VersionOut 的 domain/mapper/service（加 manual 列和 manual_input 区分），新建独立 Controller（独立权限）。前端独立四件套（裁剪版）。避免大改批次代码。

## Complexity Tracking
无宪法违规，留空。
