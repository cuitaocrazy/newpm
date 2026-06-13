# yadapm 需求梳理 ④ 项目质量管理 → 批次任务问题单及缺陷

> 来源：逐文件精读老系统源码（ProListAndDefectController/Service/Dao/Model/Query + proListAndDefect/listDetail·create·edit·showDetail·proListFile.html）
> 模块：项目质量管理（一级，**新的一级菜单**）→ 批次任务问题单及缺陷（二级），老系统 URL 前缀 `/proListAndDefect`
> 整理日期：2026-06-11　｜　状态：需求梳理（不开发），供评审

---

## 一、模块概述

针对**批次任务**登记"问题单及缺陷"记录：选定 年份+批次+项目组 下的某个任务，登记一条问题单（含一组缺陷/超时/重现等布尔标记），并可挂附件。

- **与版本管理无数据关联**，但**共用任务库地基**（`T_B_TASK` + 产品/年份/批次）。
- 权限（Shiro，**4 级**）：
  - `proListAndDefectQuery` —— 查询、详情
  - `proListAndDefectEdit` —— 新增、编辑、导出
  - `proListAndDefectDelete` —— 删除
  - `proListAndDefectFile` —— 附件上传/下载/删除
- 关键概念：**"问题单"和"缺陷"不是两张表/两条记录，而是同一条记录**（一行 = 一个问题单，其"是否缺陷/是否超时/…"是该行上的一组布尔标记）。

## 二、页面与接口清单

| 端点 | 方法 | 权限 | 作用 |
|---|---|---|---|
| `/proListAndDefect/listDetail` | GET | Query | 列表页（初始化下拉） |
| `/proListAndDefect/queryPage` | GET/POST | Query | 分页查询 |
| `/proListAndDefect/create` | GET | Edit | 新增页 |
| `/proListAndDefect/save` | POST | Edit | 保存新增（成功跳详情） |
| `/proListAndDefect/edit` | GET | Edit | 编辑页 |
| `/proListAndDefect/update` | POST | Edit | 保存编辑 |
| `/proListAndDefect/showDetail` | GET | Query | 详情（含附件列表 + 附件操作日志） |
| `/proListAndDefect/delete` | GET | Delete | 删除 |
| `/proListAndDefect/exportDetail` | GET | Edit | 导出 Excel |
| `/proListAndDefect/proListFile` | GET | File | 附件管理页 |
| `/proListAndDefect/uploadFile` | POST | File | 上传附件 |
| `/proListAndDefect/downloadFile` | GET | File | 下载附件 |
| `/proListAndDefect/deleteFile` | GET | File | 删除附件 |

**联动/校验 AJAX：**
| 接口 | 入参 | 作用 |
|---|---|---|
| `ajax_getBatchNoByBatchYear` | batchYear | 年份→批次 |
| `ajax_getProductByPidArr` | productTop(一级产品,可多选) | 一级产品→二级产品（查询用） |
| `ajax_getTaskNoByBatchYearBatchNoAndtaskTeam` | batchYear, batchId, subtaskTeam | **年份+批次+项目组 → 软件中心任务号下拉** |
| `ajax_getTaskInfoById` | taskId | 任务→回显（任务名/二级产品/各测试日期/审核状态等） |
| `ajax_getTcDate` | batchId | 批次→计划投产日期 |
| `ajax_checkRepeat` | problemNo | **问题单编号查重** |
| `ajax_checkFile` | problemId, file | **附件查重**（同问题单同类型是否已传） |

## 三、新增/编辑页 —— 字段（23 项）

| # | 字段 | 控件 | 必填 | 来源/联动 |
|---|---|---|---|---|
| 1 | 投产年份 | 下拉 | ★ | onchange→批次 |
| 2 | 投产批次号 | 下拉 | ★ | onchange→计划投产日期 + 任务号 |
| 3 | 项目组 | 下拉 | ★ | onchange→任务号（subtaskTeam） |
| 4 | 软件中心任务号 | **下拉** | ★ | 年份+批次+项目组联动；onchange→回显任务信息 |
| 5 | 任务名称 | 文本只读 | - | 选任务号回显 |
| 6 | 二级产品 | 文本只读 | - | 选任务号回显 |
| 7 | 提交内部测试B包日期 | 文本只读 | - | 选任务号回显 |
| 8 | 提交功能测试版本日期 | 文本只读 | - | 选任务号回显 |
| 9 | 提交生产版本日期 | 文本只读 | - | 选任务号回显 |
| 10 | 计划投产日期 | 文本只读 | - | 选批次回显（getTcDate） |
| 11 | 问题单编号 | 文本 | ★ | 手填，**查重 ajax_checkRepeat** |
| 12 | 问题单级别 | 下拉 | ★ | 字典 T_C_PROBLEM_LEVEL |
| 13 | 提交日期 | 日期 | ★ | |
| 14 | 问题单关闭日期 | 日期 | - | 即解决日期 settleDate |
| 15 | 是否缺陷 | 下拉(是/否) | ★ | |
| 16 | 是否超时 | 下拉(是/否) | ★ | |
| 17 | 是否问题重现 | 下拉(是/否) | ★ | |
| 18 | 是否须关注 | 下拉(是/否) | ★ | |
| 19 | 缺陷说明/超时说明 | 文本 | ★ | defectDesc，最长128 |
| 20 | 是否更新版本 | 下拉(是/否) | ★ | whetherUpdateVersion |
| 21 | 当前状态 | 下拉 | ★ | 配置 T_C_CURRENT_STATE |
| 22 | 核查日期 | 日期 | ★ | verifyDate |
| 23 | 备注 | 文本域 | - | remarksShow，最长1024 |

**字段联动链：**
- 投产年份 → 批次（getBatchNoList）
- 投产批次号 → 计划投产日期（getTcDate）+ 任务号（getTaskNoList）
- 项目组 → 任务号（getTaskNoList，**任务号下拉按 年份+批次+项目组 过滤**）
- 软件中心任务号 → 回显 任务名称/二级产品/提交内部测试B包日期/提交功能测试版本日期/提交生产版本日期 + 任务审核状态 checkStatus

**派生字段（不在表单录入、系统算/展示）：**
- 解决时间超一天 `solutionTimeOverOneDay`：由 解决日期(settleDate) − 提交日期(submitDate) > 1 天 计算；settleDate 为空则用当天。
- 排期状态 `schedulingStatus`：从任务带出。
- 颜色字段 `webDefectColor / webOvertimeColor / …`：列表里按布尔值给"是否缺陷/超时/…"上不同颜色。

## 四、列表页

**查询条件（基础 + "查看更多"展开，moreValue 控制）：**
投产年份、批次号、一级产品、二级产品（一级→二级联动）、软件中心任务号、任务名称、是否缺陷、是否超时、是否问题重现、是否须关注、解决时间是否超一天、当前状态、项目组、问题单编号、创建人员、备注、创建日期(范围)、提交日期(范围)。

**列表列（约 30 列）：** 序号、任务名称、软件中心任务号、项目组、投产年份、批次号、二级产品、提交内部测试B包日期、提交功能测试版本日期、提供生产版本日期、计划投产日期、当前状态、排期状态、问题单编号、问题单级别、提交日期、问题单关闭日期、核查日期、是否缺陷、是否超时、是否问题重现、是否须关注、解决时间超一天、缺陷说明/超时说明、是否更新版本、创建人员、修改人员、创建日期、最后修改日期、操作（详情/编辑/删除/附件）。

> 列表里"是否缺陷/超时/重现/须关注/解决超一天"按值带颜色高亮（webXxxColor）。

## 五、详情页

含表单全部字段 + 创建/修改人员与时间 + **附件列表**（文档类型/文件名称/文件说明/上传人员/上传时间/下载/删除）+ **附件操作日志**（ProListFile 模块审计）。

## 六、附件功能（proListFile.html + Controller）

- **附件管理页**展示：任务名称(只读)、软件中心任务号(只读)、文档类型(下拉，来自 `T_C_PROLISTFILE_TYPE`，预置"相关材料")、*上传文件(file)、文件说明(最长512)。
- **上传**：选文档类型 + 文件 + 说明 → uploadFile；上传前 **ajax_checkFile** 校验"同问题单+同类型"是否已传。
- **下载**：downloadFile（返回字节流）。
- **删除**：deleteFile。
- **审计**：上传/删除写操作日志（老系统 `OperationLogService`，moduleName=`ProListFile`），详情页可看日志。
- 附件表 `T_B_PROLIST_FILE`：ID、PROBLEM_ID、FILE_TYPE、FILE_URL、UPLOADER、UPLOAD_DATE、FILE_DES。
- 附件类型表 `T_C_PROLISTFILE_TYPE`：ID、FILE_TYPE、SEQUENCE，预置 `(1,'相关材料',1)`。

## 七、核心业务规则

1. **问题单 = 缺陷（同一条记录）**：一行问题单记录上挂一组布尔标记（是否缺陷/超时/问题重现/须关注/是否更新版本），不是独立的缺陷子表。
2. **任务关联**：必须选自 年份+批次+项目组 下的真实任务（下拉，存 `taskId`）；选中后从任务回显一批只读信息。
3. **任务审核状态：不校验**（已核实）。模型虽带 `checkStatus`，但 create.html 的 `getTaskInfoById` 选任务时**只回显任务信息、不做审核校验**（无"未审核/已驳回不可关联"提示）。**与版本管理不同** —— 版本管理拦未审核任务，问题单**不拦**（任何任务都可登记问题单）。
4. **问题单编号唯一**：ajax_checkRepeat 查重。
5. **解决时间超一天**：系统按 解决日期−提交日期>1天 计算（settle 空用当天）。
6. **当前状态 / 问题单级别**：配置表 `T_C_CURRENT_STATE` / `T_C_PROBLEM_LEVEL`。
7. **审计**：增删改 + 附件操作均记操作日志。

## 八、数据表

**主表 `T_B_PROLIST_AND_DEFECT`**（问题单及缺陷）字段：PROBLEM_ID(主键)、TASK_ID(任务id)、PROBLEM_NO(问题单编号)、SUBMIT_DATE(提交日期)、SETTLE_DATE(解决/关闭日期)、DEFECT_DESC(缺陷说明)、VERIFY_DATE(核查日期)、WHETHER_DEFECT(是否缺陷)、WHETHER_OVERTIME(是否超时)、WHETHER_PRO_RECURRENCE(是否问题重现)、WHETHER_ATT_REQUIRED(是否须关注)、SOLUTINO_TIME_OVER_ONE_DAY(解决时间超一天)、YEAR_ID(投产年份id)、BATCH_NO(批次号)、SUBTASK_TEAM(项目组)、CURRENT_STATUS_ID(当前状态id)、PROBLEM_LEVEL_ID(问题单级别id)、UPDATE_VERSION(是否更新版本)、REMARKS(备注)、CREATOR/MODIFIER/CREATION_DATE/LAST_MODIFICATION_DATE(审计)。

> 任务名称/二级产品/各测试日期/计划投产日期/排期状态/当前状态名/级别名 等均**关联 T_B_TASK / 配置表**实时取，不冗余存主表。

**附件表 `T_B_PROLIST_FILE`** + **附件类型表 `T_C_PROLISTFILE_TYPE`**（见第六节）。

## 九、落地 newpm 的设计考量（开发阶段，先记录）

1. **新建一级菜单"项目质量管理"** + 二级"批次任务问题单及缺陷"，权限 4 级（query/edit/remove/file）。
2. **新建表**：`pm_prolist_defect`（主表，FK `task_id`→`pm_task`）+ 附件。配置：当前状态、问题单级别 → 字典（`sys_xxx`）；附件类型 → 字典或小配置表。
3. **复用任务地基**：任务号下拉端点（按 年份+批次+**项目组**过滤 `pm_task`）—— 注意与版本管理的"按产品过滤"不同，这里按**项目组(subtaskTeam)**过滤；newpm `pm_task` 是否有"项目组"概念需确认（可能映射 `task` 的某字段或部门）。
4. **附件复用 `pm_attachment` + `pm_attachment_log`**（业务类型新增 `prolist`），替代独立附件表 + 自建日志。
5. **任务审核校验**：若保留，按"批次版本管理决策 B"同口径 —— 项目级 `pm_project.approval_status='1'`（待确认是否对问题单也加此校验）。
6. **解决时间超一天**：后端按日期计算的派生字段，不入库或入库均可（老系统入库 SOLUTINO_TIME_OVER_ONE_DAY）。

## 十、待澄清
- ~~选任务时是否校验任务审核状态~~ → **已核实：不校验**（见第七节.3）。
- "项目组(subtaskTeam)"在 newpm 对应什么（pm_task 字段 / 部门 / 团队）？任务号下拉按它过滤，需先确定映射。
- 当前状态 / 问题单级别 / 附件类型 用字典还是配置表？（建议字典）
