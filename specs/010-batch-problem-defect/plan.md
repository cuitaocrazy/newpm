# Implementation Plan: 批次任务问题单及缺陷

**Branch**: `010-batch-problem-defect` | **Date**: 2026-06-14 | **Spec**: [spec.md](./spec.md)

## Summary
新一级菜单「项目质量管理」+ 二级「批次任务问题单及缺陷」。主表 `pm_prolist_defect`(FK task_id→pm_task)，4级权限，任务三级联动(年份+批次+部门→任务号)，5个布尔标记+派生字段，附件复用 pm_attachment。问题单=缺陷同一条记录。

## Technical Context
Java 17/SpringBoot 3.5.8/MyBatis/Vue3/Element Plus。复用 007 版本管理的联动端点模式 + 通用附件体系。日期用标准 date 类型。

## Constitution Check
- II 权限：4级 @PreAuthorize ✅
- III 一致性：BaseController+startPage+TableDataInfo ✅
- V 数据库：JOIN sys_user/sys_dept/pm_task 加 COLLATE；部门过滤走 ancestors ✅
- VI 前端：四件套 + dict-select/dict-tag ✅

## 字段映射（老 T_B_PROLIST_AND_DEFECT → newpm pm_prolist_defect）

| 老字段 | 新字段 | 类型 | 说明 |
|---|---|---|---|
| PROBLEM_ID | problem_id | bigint PK | |
| TASK_ID | task_id | bigint | FK→pm_task |
| PROBLEM_NO | problem_no | varchar(128) | 问题单编号，唯一查重 |
| SUBMIT_DATE | submit_date | date | 提交日期(老yyyymmdd→date) |
| SETTLE_DATE | settle_date | date | 解决/关闭日期，可空 |
| DEFECT_DESC | defect_desc | varchar(128) | 缺陷说明(老前端64/建表128，取128) |
| VERIFY_DATE | verify_date | date | 核查日期 |
| WHETHER_DEFECT | whether_defect | char(1) | 是否缺陷 0/1 |
| WHETHER_OVERTIME | whether_overtime | char(1) | 是否超时 |
| WHETHER_PRO_RECURRENCE | whether_pro_recurrence | char(1) | 是否问题重现 |
| WHETHER_ATT_REQUIRED | whether_att_required | char(1) | 是否须关注 |
| SOLUTINO_TIME_OVER_ONE_DAY | solution_time_over_one_day | char(1) | 派生:解决超一天(修typo) |
| UPDATE_VERSION | update_version | char(1) | 是否更新版本 |
| YEAR_ID | production_year | varchar(20) | 投产年份(字典sys_ndgl) |
| BATCH_NO/BATCH_ID | batch_id | bigint | FK→pm_production_batch |
| SUBTASK_TEAM | dept_id | bigint | **项目组→部门**(FK sys_dept) |
| CURRENT_STATUS_ID | current_status | varchar(32) | 当前状态(字典sys_problem_state) |
| PROBLEM_LEVEL_ID | problem_level | varchar(32) | 级别(字典sys_problem_level) |
| REMARKS | remarks | varchar(2048) | 备注 |
| CREATOR/MODIFIER/DATE | create_by/update_by/time | (BaseEntity) | 审计 |

**关联实时取(不冗余)**：taskName/二级产品(pm_task.product)/内部B包日期(internal_closure_date)/功能测试日期(functional_test_date)/生产版本日期(production_version_date)/计划投产日期(pm_production_batch.plan_production_date)/排期状态(schedule_status)/部门名/状态名/级别名。

## 派生字段算法（照搬老系统口径）
```
solutionTimeOverOneDay:
  base = settle_date 非空 ? settle_date : 当天
  diff天数 = base - submit_date
  结果 = diff > 1 ? '1' : '0'
```
（老系统用 yyyymmdd 数值相减，newpm 用 date 的 DAYS 差，口径一致：差>1天为是。）

## 任务联动端点（按部门过滤，对照老 ajax_*）
- `GET /batchByYear?year=` 年份→批次（复用 pm_production_batch）
- `GET /tcDate?batchId=` 批次→计划投产日期
- `GET /deptOptions` 部门下拉（数据权限部门树，复用 /project/project/deptTree）
- `GET /taskOptions?productionYear=&batchId=&deptId=` **年份+批次+部门→任务号**(部门走 ancestors)
- `GET /taskInfo?taskId=` 任务回显(taskName/product/internalClosureDate/functionalTestDate/productionVersionDate/scheduleStatus)
- `GET /checkProblemNo?problemNo=&problemId=` 问题单编号查重(**编辑排除自己**，修老bug)

## Project Structure
```
后端 ruoyi-project：
- domain/ProlistDefect.java（主表字段 + 关联展示字段 + 多值查询字段）
- mapper/ProlistDefectMapper.java(+xml)：selectList(多维+部门ancestors+JOIN COLLATE) / CRUD / checkProblemNo / taskOptions / taskInfo
- service/IProlistDefectService + impl：CRUD + solutionTimeOverOneDay 计算 + 查重 + 联动转发
- controller/ProlistDefectController：/project/prolistDefect（CRUD+导出+6联动端点），4级权限
- AttachmentController：@PreAuthorize 白名单加 project:prolistDefect:query/file（复用附件）
前端 ruoyi-ui：
- views/project/prolistDefect/（index/add/edit/detail，detail含附件区）
- api/project/prolistDefect.ts
数据库：00_tables_ddl 建表；01_tables_data 加3字典；02_menu_data 一级菜单+二级+4权限；migration_010
```

## Structure Decision
主表独立，附件复用通用体系。任务联动复用 007 模式但过滤维度换部门。问题单=缺陷单表(布尔标记)。
