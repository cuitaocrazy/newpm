# Phase 1 Data Model: 批次版本管理

字段映射来源：`Storage.scala` + `T_B_VERSION_OUT` DDL。命名转为 newpm 规范（snake_case 列、BaseEntity 审计字段、del_flag 软删除）。字符集 utf8mb4_0900_ai_ci。

---

## 表 1：`pm_version_out`（出入库版本主表）

| 列 | 类型 | 说明 | 老系统对应 | 映射 |
|---|---|---|---|---|
| id | bigint PK auto_increment | 主键 | id | |
| production_year | varchar(20) | 投产年份 | proYear | 字典 sys_ndgl |
| batch_id | bigint | 投产批次id | batchId | FK pm_production_batch |
| pro_batch_no | varchar(64) | 投产批次号（冗余展示） | proBatchNo | |
| sub_version_code | varchar(64) | 子产品ID | subVersionCode | 字典 sys_product；类型4 版本号按"子产品+年份"维度取序号 |
| product | varchar(64) | 产品（冗余展示，来源 pm_sys_name.product） | product | 选定子系统后带出，非独立维护 |
| version_type | varchar(8) | 版本类型 1-6 | versionType | 字典 sys_version_type |
| sys_name | varchar(64) | 子系统名称 | sysName | FK pm_sys_name.sys_name |
| base_version_code | varchar(64) | 基准版本号 | baseVersioncode | 录入时从子系统带出 |
| out_lib_version | varchar(128) | **出入库版本号（自动生成、只读）** | outLibVersion | 算法生成 |
| version_code | varchar(64) | 版本编号（生成中间值） | versionCode | 算法生成 |
| out_version | varchar(64) | 升级包初级版本号 | outVersion | 类型 5/6 用 |
| comm_name | varchar(64) | 提交人员 | commName | FK sys_user，默认当前用户 |
| version_p_date | varchar(20) | 版本投产日期（只读，批次带出） | versionPDate | from pm_production_batch |
| is_involved | char(1) | 是否涉及TWS改造 0是1否 | isInvolved | |
| db_update | char(1) | 数据库是否修改 0是1否 | dbUpdate | |
| usb_update | char(1) | 接口是否修改 0是1否 | usbUpdate | |
| package_mode | varchar(8) | 组包方式 1-6 | packageMode | 字典 sys_package_mode |
| version_status | varchar(20) | 版本状态 | versionStatus | 字典 sys_version_status（**值待生产数据，初始空字典，管理员可维护；本轮可选**） |
| version_descr | varchar(512) | 版本说明 | versionDescr | |
| remarks | varchar(2048) | 备注 | remarks | |
| manual_input | char(1) | 批次标志 0=批次 1=非批次 | manualInput | **本 spec 仅 0** |
| del_flag | char(1) default '0' | 软删除 0正常 1删除 | — | newpm 约定 |
| create_by/create_time/update_by/update_time/remark | BaseEntity | 审计 | creator/modifier/... | |

**索引**：`idx_year_batch(production_year,batch_id)`、`idx_sys_type(sys_name,version_type)`、唯一键 `uk_sys_type_outlib(sys_name,version_type,out_lib_version)`（保证版本号唯一 + 并发防撞）。

**验证规则（来自 FR）**：
- 必填：production_year、batch_id、sub_version_code、sys_name、version_type、package_mode、is_involved、db_update、usb_update（FR-001）。
- **version_status 本轮为可选**：枚举值来自生产库（源码无 seed），待迁移数据到位后灌入字典并可收紧为必填。
- out_lib_version 由系统生成，禁止前端传入覆盖（FR-002）。
- version_p_date 只读，由 batch_id 带出（FR-005）。
- 类型 5/6 必须提供 out_version，否则阻止生成（Edge Case）。
- task_id 可空：录入的软件中心任务号在 pm_task 找不到对应时，仅存 task_no 文本，不强制关联（Edge Case）。

## 表 2：`pm_version_out_task`（版本-任务关联，主从子表）

| 列 | 类型 | 说明 |
|---|---|---|
| id | bigint PK auto_increment | 主键 |
| version_id | bigint | FK pm_version_out.id |
| task_id | bigint | FK pm_task.task_id（可空，老数据可能无对应） |
| task_no | varchar(64) | 软件中心任务号（software_demand_no） |
| task_name | varchar(255) | 任务名称（冗余回显） |
| prj_name | varchar(255) | 项目名称（回显） |
| demand_name | varchar(255) | 需求名称（回显） |

- 主从级联：保存版本时整体替换其任务行；删除版本时级联删除（`@Transactional`）。
- resultMap 用 `<collection>` 装配 taskList。

## 表 3：`pm_sys_name`（子系统配置表）

| 列 | 类型 | 说明 | 老系统 |
|---|---|---|---|
| id | bigint PK auto_increment | 主键 | T_C_SYS_NAME.id |
| sys_name | varchar(64) | 子系统名称 | sys_name |
| base_version_code | varchar(64) | 基准版本号 | base_version_code |
| p_id | varchar(32) | 一级产品ID | pId |
| product | varchar(64) | 产品名称 | product |
| del_flag | char(1) default '0' | 软删除 | |
| create_*/update_* | BaseEntity | 审计 | |

- 联动：`产品 → 子系统列表`（按 `product` 过滤 pm_sys_name）；选中子系统带出 `base_version_code` 与 `product`。
- `product`/`p_id` 是子系统的属性：主表 `pm_version_out.product` 由此带出（冗余展示），类型4 版本号后缀 `T_{年}_{NNN}_{product}` 取自此处。

## 字典数据（sys_dict_data）

| 字典类型 | 值-标签 |
|---|---|
| sys_version_type | 1=SP升级包 2=PTF补丁包 3=B测试包 4=临时版本包 5=B包升级包 6=SP包升级包 |
| sys_package_mode | 1=A1-本批次全量 2=A2-本批次增量 3=B1-单任务全量 4=B2-单任务增量 5=C1-多任务全量 6=C2-多任务增量 |
| sys_version_status | （迁移时从老 T_C_VERSION_STATUS 取真实枚举，暂以占位，plan→tasks 阶段确认） |

> 插入字典后须 `DEL sys_dict:<type>` 刷 Redis 缓存。

## 实体关系

```
pm_production_batch 1──* pm_version_out *──* pm_task   (经 pm_version_out_task)
pm_sys_name 1──* pm_version_out
sys_dict_data(sys_version_type/sys_package_mode/sys_version_status/sys_ndgl/sys_product) ──> pm_version_out (字典值)
sys_user ──> pm_version_out.comm_name (提交人员)
```

## 状态/生命周期

本实体无审批状态机；version_status 是描述性字典，非流转状态。CRUD + 软删除即全部生命周期。
