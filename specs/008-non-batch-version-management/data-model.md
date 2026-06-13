# Phase 1 Data Model: 非批次版本管理

## 既有表变更：`pm_version_out` 加 2 列

| 列 | 类型 | 说明 |
|---|---|---|
| manual_task_no | varchar(64) | 软件中心任务号（**非批次手填**，批次为空） |
| manual_task_name | varchar(255) | 任务名称（**非批次手填**，批次为空） |

> 非批次记录：`manual_input='1'`，任务信息存这两列。
> 批次记录：`manual_input='0'`，任务走 `pm_version_out_task` 关联表，这两列空。

## 复用字段（pm_version_out 已有，非批次用到）

production_year、batch_id、pro_batch_no、sub_version_code(产品)、product、version_type、
sys_name、base_version_code、out_lib_version、version_code、out_version(类型5/6)、
comm_name、version_p_date、is_involved、db_update、usb_update、version_descr、remarks、
manual_input、del_flag、create_by/time、update_by/time。

## 非批次**不使用**的字段（批次专用）

version_brief（版本简介）、package_mode（组包方式）、version_status（版本状态）。
→ 非批次新增/编辑不填这些，前端不显示。

## 复用表（不变）

- `pm_sys_name`：产品→子系统→基准版本号。
- `pm_production_batch`：批次号、投产日期。
- 字典：`sys_ndgl`(年份)、`sys_product`(产品)、`sys_version_type`(版本类型)。
- `sys_user`：提交人员/创建/修改人昵称（JOIN 加 COLLATE）。

## 字段必填（来自源码核实）

新增必填★：投产年份、投产批次、产品、子系统、软件中心任务号(手填)、任务名称(手填)、
版本类型、(类型5/6)初级版本号、是否涉及TWS改造、数据库是否修改、接口是否修改、版本说明。
只读：出入库版本号(自动)、版本投产日期(批次带出)、提交人员(当前用户)。

## ER

```
pm_production_batch 1──* pm_version_out(manual_input=1)
pm_sys_name 1──* pm_version_out
字典/sys_user ──> pm_version_out
（非批次不关联 pm_version_out_task / pm_task）
```
