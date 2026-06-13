# yadapm 迁移需求整理：出入库版本管理 + 项目质量管理

> 来源：旧系统 yadapm（Scala + Spring Boot + Thymeleaf + Oracle + Shiro）
> 源码路径：`/Users/kongli/Documents/companyProjectDoc/ConfigDoc/ConfigItemDocuments/一部项目管理源码和资料-华冰提供/yadapm`
> 目标：迁移到 newpm（RuoYi-Vue：Spring Boot 3 + MyBatis + MySQL + Vue3 + Spring Security）
> 整理日期：2026-06-10

本文档枚举 5 个二级菜单**全部功能**（不止落地页），并给出落地 newpm 的映射方案，作为 `/speckit-specify` 的输入。

---

## 实施进度

| 菜单 | 状态 | 说明 |
|---|---|---|
| **批次版本管理** | ✅ **已完成**（spec 007，2026-06-11） | US1-US4 功能全交付，详见 `specs/007-batch-version-management/` |
| 非批次版本管理 | ⏳ 未开始 | 与批次共用 `pm_version_out`（manual_input=1），可复用大部分 |
| 旧数据查询 | ⏳ 未开始 | 只读，依赖数据迁移 |
| 批次任务问题单及缺陷 | ⏳ 未开始 | |
| 非批次任务问题单及缺陷 | ⏳ 未开始 | |
| 数据迁移（全部菜单存量） | ⏸️ 延后 | 等旧库最新数据到位 |

### 批次版本管理 与老系统的差异（迁移决策）

1. **并发防撞**：老系统"查最大+1"裸奔有竞态；newpm 加 `uk_sys_type_outlib` 唯一键 + 生成失败重试(≤3)。
2. **软删除**：老系统物理删除；newpm 用 `del_flag='1'` 软删除（符合宪法）。
3. **子系统配置**：`T_C_SYS_NAME` → 独立配置表 `pm_sys_name`（非字典，因含基准版本号/产品多属性）。
4. **任务关联**：老系统逗号分隔 taskNo → 子表 `pm_version_out_task`（仅存 task_id，回显 JOIN pm_task/pm_project）。
5. **需求名称**：`pm_task` 新增 `demand_name` 列（老系统任务表无此字段）。
6. **版本状态字典**：`sys_version_status` 建空字典，枚举值待数据迁移填充（源码无 seed）。
7. **稽核**：老系统 `T_B_OPERATION_LOG` → newpm `@Log` 注解 + `sys_oper_log`。
8. **提交人员**：存 `user_id`，列表/导出 JOIN 显示 `nick_name`（姓名）。

---

## 一、菜单结构总览

```
出入库版本管理 (一级菜单)
├── 批次版本管理      /storage/*          表 T_B_VERSION_OUT (manualInput=批次)
├── 非批次版本管理    /storageManual/*    表 T_B_VERSION_OUT (manualInput=非批次)
└── 旧数据查询        /oldStorage/*       表 T_B_OLD_VERSION_OUT (只读)

项目质量管理 (一级菜单)
├── 批次任务问题单及缺陷    /proListAndDefect/*         表 T_B_PROLIST_AND_DEFECT (+附件)
└── 非批次任务问题单及缺陷  /proNoBatchListAndDefect/*  表 T_B_NOBATCH_PROLIST_AND_DEFECT (+附件)
```

老系统权限（Shiro）：
- 出入库：`storageManagementQuery / Edit / Delete`、`oldStorageManagementQuery`
- 质量：`proListAndDefect{Query,Edit,Delete,File}`、`proNoBatchListAndDefect{Query,Edit,Delete,File}`

---

## 二、出入库版本管理

### 2.1 批次版本管理（`/storage/*`）

**全部功能端点：**

| 端点 | 方法 | 作用 |
|---|---|---|
| `/storage/list` | GET | 列表页初始化（加载下拉框） |
| `/storage/query` | GET | 分页查询 |
| `/storage/create` | GET | 新增页 |
| `/storage/save` | POST | 保存新增 |
| `/storage/edit` | GET | 编辑页（回显） |
| `/storage/update` | POST | 保存编辑 |
| `/storage/show` | GET | 详情页 |
| `/storage/delete` | GET | 删除 |
| `/storage/exportDetail` | GET | 导出 Excel |
| `/storage/ajax_getBatchNoByBatchYear` | POST | 联动：年份→批次列表 |
| `/storage/ajax_getCenterTaskNo` | POST | 联动：批次+产品+年份→任务列表 |
| `/storage/ajax_getSysNameByProduct` | POST | 联动：产品→子系统列表 |
| `/storage/ajax_getOutVersion` | POST | 联动：子系统+版本类型→升级包初级版本号 |
| `/storage/ajax_getOutLibVersion` | POST | **自动生成出入库版本号（核心算法）** |
| `/storage/ajax_getVersionPDate` | POST | 联动：批次→投产日期 |
| `/storage/ajax_getPrjDemandBatchNo` | POST | 联动：任务号→项目名/需求名回显 |

**主表 `T_B_VERSION_OUT` 字段（来自 Storage.scala，权威）：**

| 字段 | 含义 | 备注 |
|---|---|---|
| id | 版本信息ID | 主键 |
| proYear / yearDes | 投产年份id / 年份显示 | → newpm `sys_ndgl` |
| batchId / proBatchNo | 投产批次id / 批次号 | → newpm `pm_production_batch` |
| subVersionCode | 子产品ID | → `sys_product` |
| product | 产品 | |
| taskNo / taskNoArr / taskIdArr | 软件中心任务号（可多个，逗号分隔） | → `pm_task.softwareDemandNo` |
| taskName | 任务名称 | 冗余 |
| versionType / versionTypeName | 版本类型id / 名称 | 枚举 1-6，见下 |
| sysName | 子系统名称 | 配置表 T_C_SYS_NAME |
| baseVersioncode | 基准版本号 | 来自子系统配置 |
| outLibVersion | **出入库版本号** | 自动生成 |
| versionCode | 版本编号 | 自动生成，拼接用 |
| outVersion | 升级包初级版本号 | 版本类型 5/6 用 |
| prjName / demandName | 项目名 / 需求名 | 任务号回显 |
| commName / userName | 提交人员id / 姓名 | → `sys_user` |
| versionPDate | 版本投产日期 | 批次自动带出 |
| isInvolved | 是否涉及TWS改造 | 0=是 1=否 |
| dbUpdate | 数据库是否修改 | 0=是 1=否 |
| usbUpdate | 接口是否修改 | 0=是 1=否 |
| packageMode | 组包方式 | 枚举 1-6（A1/A2/B1/B2/C1/C2） |
| versionStatus | 版本状态 | 配置 |
| versionDescr | 版本说明 | |
| remarks | 备注 | |
| localSysDate | 提交日期 | YYYYMMDD |
| manualInput | 是否手动输入 | **区分批次/非批次的标志位** |
| creator/modifier/creationDate/lastModificationDate | 审计字段 | |

**版本类型枚举（T_C_VERSION_TYPE）：** 1=SP升级包 2=PTF补丁包 3=B测试包 4=临时版本包 5=B包升级包 6=SP包升级包

**组包方式枚举（T_C_PACKAGE_MODE）：** 1=A1本批次全量 2=A2本批次增量 3=B1单任务全量 4=B2单任务增量 5=C1多任务全量 6=C2多任务增量

**核心业务规则 —— 出入库版本号自动生成算法（最复杂，迁移重点）：**
- 类型1 SP升级包：`{基准版本号}_SP{序号}`，序号=同子系统同类型最大序号+1，2位补零
- 类型2 PTF：`{基准版本号}_PTF{序号}`
- 类型3 B测试包：`{基准版本号}_B{序号}`
- 类型4 临时版本包：`T_{当前年份}_{序号3位}_{产品}`，序号按子产品+年份维度
- 类型5 B包升级包：`{基准版本号}_B{主}.{次}`，依赖 outVersion，已存在则次+1 否则次=01
- 类型6 SP包升级包：`{基准版本号}_SP{主}.{次}`，规则同类型5

**其它规则：**
- taskNo 支持多任务（逗号分隔），前端动态增删行
- 投产日期由批次自动带出，用户不可改
- 每次增删改写稽核日志（老系统 T_B_OPERATION_LOG）→ newpm 用 `@Log` 注解替代

### 2.2 非批次版本管理（`/storageManual/*`）

- **同一张主表 `T_B_VERSION_OUT`**，用 `manualInput` 区分。
- 端点同批次版（list/query/create/save/edit/update/show/delete/exportDetail + 部分 ajax 联动）。
- **差异**：投产日期用户手填（不绑批次自动带出）；查询页更简洁。
- 版本号生成算法、权限、稽核完全相同（稽核描述前缀为"非批次版本管理-"）。

### 2.3 旧数据查询（`/oldStorage/*`）

- **只读**，仅 `/oldStorage/list`（初始化）+ `/oldStorage/query`（分页查询），无增删改。
- 主表 `T_B_OLD_VERSION_OUT`（来自 OldStorage.scala）字段：id、sysName(子系统)、product(子产品)、baseVersioncode(基准版本号)、outLibVersion(出入库版本号)、versionType(版本类型)、versionCode(版本编号)、commName(提交人员)、versionPDate(版本投产日期)、versionDescr(版本说明)、remarks(备注)、taskNo(任务编号)、taskName(任务名称)、proYear(投产年份)、proBatchNo(投产批次号)、isInvolved/dbUpdate/usbUpdate、sequenceNo(顺序号)。
- 查询条件：任务编号、投产批次号、子产品、版本类型。
- 列表 18 列全展示，仅查询/重置按钮。
- **本质是历史归档数据展示** → 需要数据迁移把老 Oracle 数据导入。

---

## 三、项目质量管理

### 3.1 批次任务问题单及缺陷（`/proListAndDefect/*`）

**全部功能端点：**

| 端点 | 方法 | 作用 |
|---|---|---|
| `/proListAndDefect/listDetail` | GET | 列表页 |
| `/proListAndDefect/queryPage` | GET/POST | 分页查询 |
| `/proListAndDefect/create` | GET | 新增页 |
| `/proListAndDefect/save` | POST | 保存新增 |
| `/proListAndDefect/edit` | GET | 编辑页 |
| `/proListAndDefect/update` | POST | 保存编辑 |
| `/proListAndDefect/showDetail` | GET | 详情页 |
| `/proListAndDefect/delete` | POST | 删除 |
| `/proListAndDefect/exportDetail` | POST | 导出 Excel |
| `/proListAndDefect/proListFile` | GET | 附件管理页 |
| `/proListAndDefect/uploadFile` | POST | 上传附件 |
| `/proListAndDefect/downloadFile` | GET | 下载附件 |
| `/proListAndDefect/deleteFile` | POST | 删除附件 |
| `/proListAndDefect/ajax_getBatchNoByBatchYear` | POST | 联动：年份→批次 |
| `/proListAndDefect/ajax_getProductByPidArr` | POST | 联动：一级产品→二级产品 |
| `/proListAndDefect/ajax_getTaskNoByBatchYearBatchNoAndtaskTeam` | POST | 联动：年份+批次+项目组→任务号 |
| `/proListAndDefect/ajax_getTaskInfoById` | POST | 联动：任务→任务详情自动填充 |
| `/proListAndDefect/ajax_getTcDate` | POST | 联动：批次→计划投产日期 |
| `/proListAndDefect/ajax_checkFile` | POST | 校验文件是否已上传 |
| `/proListAndDefect/ajax_checkRepeat` | POST | 校验问题单编号重复 |

**主表 `T_B_PROLIST_AND_DEFECT` 字段（来自真实 DDL）：**

| 字段 | 含义 | 备注 |
|---|---|---|
| PROBLEM_ID | 问题单ID | 主键 |
| TASK_ID | 任务ID | → newpm `pm_task.task_id` |
| PROBLEM_NO | 问题单编号 | VARCHAR2(128)，需唯一性校验 |
| SUBMIT_DATE | 提交日期 | YYYYMMDD |
| SETTLE_DATE | 解决/关闭日期 | YYYYMMDD |
| DEFECT_DESC | 缺陷说明/超时说明 | VARCHAR2(128) |
| VERIFY_DATE | 核查日期 | YYYYMMDD |
| WHETHER_DEFECT | 是否缺陷 | 0否 1是 |
| WHETHER_OVERTIME | 是否超时 | 0否 1是 |
| WHETHER_PRO_RECURRENCE | 是否问题重现 | 0否 1是 |
| WHETHER_ATT_REQUIRED | 是否须关注 | 0否 1是 |
| SOLUTINO_TIME_OVER_ONE_DAY | 解决时间超一天 | 由 SETTLE-SUBMIT 计算，SETTLE 空则用当天 |
| YEAR_ID | 投产年份id | → `sys_ndgl` |
| BATCH_NO | 批次号 | → `pm_production_batch` |
| SUBTASK_TEAM | 项目组 | |
| CURRENT_STATUS_ID | 当前状态id | 配置 |
| PROBLEM_LEVEL_ID | 问题单级别id | 配置 T_C_PROBLEM_LEVEL |
| UPDATE_VERSION | 是否更新版本 | 0否 1是 |
| REMARKS | 备注 | VARCHAR2(2048) |
| CREATOR/MODIFIER/CREATION_DATE/LAST_MODIFICATION_DATE | 审计 | |

**从 Task 关联带出的只读显示字段：** batchYear、centerTaskNo(软件中心任务号)、taskName、product/productOne、checkStatus、testSubBDate(内测B包日期)、testVersionDate(功能测试版本日期)、proVersionDate(生产版本日期)、tcDate(计划投产日期)、schedulingStatus(排期状态)、currentStatus、problemLevel。

**附件表 `T_B_PROLIST_FILE`：** ID、PROBLEM_ID、FILE_TYPE、FILE_URL、UPLOADER、UPLOAD_DATE、FILE_DES。
**附件类型表 `T_C_PROLISTFILE_TYPE`：** ID、FILE_TYPE、SEQUENCE，预置 `('1','相关材料','1')`。

**核心业务规则：**
- "问题单"与"缺陷"**不是两张表**：缺陷是问题单记录上的一组布尔标签（是否缺陷/超时/重现/须关注），一条记录即一个问题单。
- 必须绑定一个正式任务（TASK_ID），年份+批次+项目组+任务号级联选择。
- 问题单编号唯一性校验（ajax_checkRepeat）。
- 列表带颜色标记（缺陷/超时等字段为"是"时高亮）。
- 列表约 30 列，含从任务带出的日期/状态列。
- 与出入库版本（storage）**无数据关联**。

### 3.2 非批次任务问题单及缺陷（`/proNoBatchListAndDefect/*`）

- 端点与批次版一一对应（前缀换成 `/proNoBatchListAndDefect`）。
- 主表 `T_B_NOBATCH_PROLIST_AND_DEFECT`：在批次版字段基础上，**额外冗余了任务信息字段**（因为不强制关联任务表）：TASK_NO(手输软件中心任务号)、TASK_NAME(手输任务名)、PRODUCT(手选产品)、TEST_SUB_B_DATE、TEST_VERSION_DATE、PRO_VERSION_DATE、SCHEDULING_STATUS、BATCH_ID。
- 附件表 `T_B_NOBATCH_PROLIST_FILE`（结构同批次版）。
- **核心差异**：任务号/任务名/产品/测试日期等**全部手输**，不级联任务库；TASK_ID 可空。用于插单/临时/特殊任务。

**批次 vs 非批次对照：**

| 维度 | 批次 | 非批次 |
|---|---|---|
| 任务来源 | 强制关联 pm_task | 手输，不强制关联 |
| 任务号/名称/产品/测试日期 | 任务自动带出（只读） | 手输 |
| 使用场景 | 正常批次流程 | 插单/临时/特殊 |

---

## 四、落地 newpm 的映射方案

### 4.1 概念映射（复用现有资产）

| yadapm 概念 | newpm 现成对应 | 说明 |
|---|---|---|
| T_TASK 任务 | `pm_task` | 批次问题单/批次版本直接 FK `task_id` |
| T_BATCH 投产批次 | `pm_production_batch` | 批次号、投产日期来源 |
| T_YEAR 投产年份 | 字典 `sys_ndgl` | productionYear |
| 产品 | 字典 `sys_product` | |
| 排期状态 | 字典 `sys_pqzt` | scheduleStatus |
| 软件中心任务号 | `pm_task.software_demand_no` | |
| Shiro 权限 | Spring Security `@ss.hasPermi('project:xxx:action')` | |
| T_B_OPERATION_LOG 稽核 | `@Log(title,businessType)` 注解 | 异步审计 |
| Thymeleaf 模板 | Vue3 + Element Plus 页面 | |
| Oracle 序列/VARCHAR2/NUMBER | MySQL AUTO_INCREMENT/varchar/数值 | 字符集 utf8mb4 |

### 4.2 新建表（MySQL utf8mb4，沿用 newpm 命名规范）

1. `pm_version_out` —— 出入库版本（批次+非批次共用，`manual_input` 区分）
2. `pm_old_version_out` —— 旧版本归档（只读）
3. `pm_prolist_defect` —— 批次问题单及缺陷（FK `task_id`→`pm_task`）
4. `pm_prolist_file` —— 批次问题单附件（或直接复用 newpm 现有 `pm_attachment`，业务类型新增）
5. `pm_nobatch_prolist_defect` —— 非批次问题单及缺陷（冗余任务字段）
6. `pm_nobatch_prolist_file` —— 非批次问题单附件（同上，可考虑复用 pm_attachment）

> 附件建议：newpm 已有统一 `pm_attachment` + `pm_attachment_log`（业务类型 project/contract/payment）。可新增业务类型 `prolist`/`nobatch_prolist` 复用，避免重复造附件表。

### 4.3 新增字典 / 配置

需要新增字典类型（对应老系统配置表）：
- 版本类型（6 项）→ 新字典如 `sys_version_type`
- 组包方式（6 项）→ `sys_package_mode`
- 版本状态 → `sys_version_status`
- 问题单级别（T_C_PROBLEM_LEVEL）→ `sys_problem_level`
- 问题单当前状态（T_C_CURRENT_STATUS）→ `sys_problem_status`
- 子系统名称+基准版本号（T_C_SYS_NAME，含 base_version_code/product 关联）→ **字段多，建议建配置表 `pm_sys_name` 而非字典**

### 4.4 后端落地（RuoYi 模式）

- 6 个 Controller 落 `ruoyi-project`，URL 前缀建议：
  - `/project/versionOut/**`（批次+非批次共用，或拆 `/versionOut` `/versionOutManual`）
  - `/project/oldVersionOut/**`
  - `/project/prolistDefect/**`、`/project/nobatchProlistDefect/**`
- 继承 `BaseController`，list 首行 `startPage()`，`@PreAuthorize` + `@Log`。
- **出入库版本号生成算法**是迁移最高风险点，需单独 service 方法 + 字符集测试 + 特征测试锁定。
- 各种 ajax 联动 → 改为 REST 端点供前端 Vue 调用。
- Excel 导出用 `@Excel` + `exportExcel`。

### 4.5 前端落地（Vue3 + TS）

- 页面落 `ruoyi-ui/src/views/project/` 下，每个二级菜单一套 index/add/edit/detail。
- 动态增删任务行 → Vue 响应式数组。
- 级联下拉 → 复用 `DictSelect`/`UserSelect`/`ProjectDeptSelect` + 自定义联动请求。
- 查询状态缓存复用 newpm `sessionStorage` 模式。

### 4.6 数据迁移

- "旧数据查询"页依赖存量数据 → 需要 Oracle → MySQL 的 ETL 脚本（`T_B_OLD_VERSION_OUT` → `pm_old_version_out`）。
- 是否迁移现有 `T_B_VERSION_OUT` / 问题单存量数据需确认。

---

## 五、待确认决策点

1. **数据迁移范围**：是否迁移老 Oracle 存量数据？还是新系统从零开始（仅旧数据查询页迁历史）？
2. **拆分粒度**：做成 1 个大 spec，还是按一级菜单拆成 2 个 spec（出入库 / 质量管理）？建议拆 2 个。
3. **附件方案**：复用 newpm 现有 `pm_attachment`，还是照搬独立附件表？建议复用。
4. **子系统/版本类型等配置**：字典 vs 配置表？子系统名称字段多，建议配置表；其余用字典。
5. **菜单权限粒度**：是否保留老系统"查询/编辑/删除/附件"四级权限拆分？
