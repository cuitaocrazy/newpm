# Implementation Plan: 非批次任务问题单及缺陷

**Branch**: `011-nobatch-problem-defect` | **Date**: 2026-06-14 | **Spec**: [spec.md](./spec.md)

## Summary
独立表 `pm_nobatch_prolist_defect`(任务字段冗余手填，无 task_id FK)。结构镜像 ④ 但砍掉任务库联动：任务号/任务名文本手填，二级产品/排期状态字典下拉，三测试日期手填。投产年份→批次→计划投产日期仍联动。附件复用 pm_attachment(nobatch_prolist)。4级独立权限。

## 字段（pm_nobatch_prolist_defect）
与 pm_prolist_defect 主表字段相同 + **额外冗余手填任务字段**：
| 字段 | 类型 | 说明 |
|---|---|---|
| problem_id | bigint PK | |
| task_no | varchar(64) | 软件中心任务号(手填) |
| task_name | varchar(255) | 任务名称(手填) |
| product | varchar(64) | 二级产品(字典sys_product手选) |
| internal_closure_date | date | 提交内部测试B包日期(手填) |
| functional_test_date | date | 提交功能测试版本日期(手填) |
| production_version_date | date | 提交生产版本日期(手填) |
| schedule_status | varchar(32) | 排期状态(字典sys_pqzt手选) |
| production_year | varchar(20) | 投产年份(sys_ndgl) |
| batch_id | bigint | 投产批次(FK pm_production_batch，联动计划投产日期) |
| dept_id | bigint | 项目组(部门,手选,**不派生**——非批次无真实任务) |
| problem_no varchar(160) 唯一 / problem_level / current_status / submit_date / settle_date / verify_date |
| whether_defect/overtime/pro_recurrence/att_required/update_version / solution_time_over_one_day |
| defect_desc varchar(128) / remarks varchar(2048) / del_flag / 审计 |

**关联实时取(JOIN)**：batch_no/plan_production_date(pm_production_batch)、dept_name(sys_dept)、create_by_name/update_by_name(sys_user 加COLLATE)。任务字段直接读实存列(无JOIN pm_task)。

## 与 ④ 的实现差异
- domain：去掉 ④ 的"关联展示字段(taskName/product/各日期/scheduleStatus 从JOIN)"，改为**实存字段**(同名但直接持久化)。taskCode→task_no。
- mapper：list 不 JOIN pm_task(任务字段读本表)；**无 taskOptions/taskInfo/selectTaskProjectDept**；保留 batchByYear/tcDate/checkProblemNo。
- service：**无 syncDeptIdFromTask**(dept_id 直接存手选值)；保留派生算法+查重。
- controller：CRUD+导出+3联动端点(batchByYear/tcDate/checkProblemNo)，去掉 taskOptions/taskInfo。
- 前端 add：任务号/任务名 el-input；二级产品 dict-select sys_product；排期状态 dict-select sys_pqzt；三测试日期 el-date-picker；年份→批次→计划投产日期联动(不联动任务)。
- 附件：AttachmentServiceImpl.getBusinessFolder 加 `nobatch_prolist` 分支(反查 pm_nobatch_prolist_defect)；AttachmentController 白名单加 nobatchProlist 权限。

## Constitution Check
- 权限4级✅ / startPage+TableDataInfo✅ / JOIN加COLLATE✅ / 部门走ancestors(查询)✅ / dict-select✅

## Project Structure
```
后端: domain/NobatchProlistDefect + mapper(+xml) + service(+impl) + controller NobatchProlistDefectController(/project/nobatchProlist)
     AttachmentController/ServiceImpl 加 nobatch_prolist 分支
前端: views/project/nobatchProlist/{index,add,edit,detail} + api/project/nobatchProlist.ts + router
数据库: 00_tables_ddl 建表; 02_menu_data 二级菜单+4权限; migration_011 (复用已有3字典,不新增)
```

## Structure Decision
独立表镜像④，砍任务联动、任务字段实存手填。dept_id 不派生(非批次无真实任务，存手选)。
