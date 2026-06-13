# yadapm 需求梳理 ⑤ 项目质量管理 → 非批次任务问题单及缺陷

> 来源：逐文件精读老系统源码（ProNoBatchListAndDefectController/Service/Dao/Model/Query + proNoBatchListAndDefect/*.html）
> 模块：项目质量管理（一级）→ 非批次任务问题单及缺陷（二级），老系统 URL 前缀 `/proNoBatchListAndDefect`
> 整理日期：2026-06-11　｜　状态：需求梳理（不开发），供评审
> **共用部分见 `yadapm需求-04-批次任务问题单及缺陷.md`**；本文只写差异与本模块要点。

---

## 一、模块概述

与批次任务问题单及缺陷**业务相同**（登记问题单/缺陷 + 附件），区别同"非批次版本管理"那套：**任务信息全靠手填/手选，不走任务库联动**。独立主表 `T_B_NOBATCH_PROLIST_AND_DEFECT`（任务相关字段冗余存）。

- 权限（Shiro，**4 级**，独立于批次）：`proNoBatchListAndDefect{Query,Edit,Delete,File}`。
- 关键概念同批次：**问题单 = 缺陷（同一条记录）**，挂一组布尔标记。

## 二、页面与接口

页面与端点**与批次一一对应**（listDetail/queryPage/create/save/edit/update/showDetail/delete/exportDetail/proListFile/uploadFile/downloadFile/deleteFile），前缀换成 `/proNoBatchListAndDefect`。

AJAX 端点也基本相同（getBatchNoByBatchYear/getProductByPidArr/getTaskNoByBatchYearBatchNoAndtaskTeam/getTaskInfoById/getTcDate/checkFile）。
> ⚠️ 注意：虽然 `getTaskNoByBatchYearBatchNoAndtaskTeam`、`getTaskInfoById` 端点仍在，但**非批次表单里的"软件中心任务号"是文本手填**，不调这俩做任务下拉/回显（属遗留端点，前端未用）。

## 三、新增/编辑页 —— 字段（与批次的差异在"任务信息手填/手选"）

| # | 字段 | 批次 | **非批次** |
|---|---|---|---|
| 投产年份 | 下拉★ | 下拉★ | 同 |
| 投产批次号 | 下拉★（联动任务+投产日期）| 下拉★（**只联动计划投产日期，不联动任务**）| 差异 |
| 项目组 | 下拉★（联动任务）| 下拉★（**不联动任务**）| 差异 |
| **软件中心任务号** | 下拉(选 taskId) | **文本手填**(taskNo) | **差异** |
| **任务名称** | 只读回显 | **文本手填** | **差异** |
| **二级产品** | 只读回显 | **下拉手选** | **差异** |
| **提交内部测试B包日期** | 只读回显 | **日期手填** | **差异** |
| **提交功能测试版本日期** | 只读回显 | **日期手填** | **差异** |
| **提交生产版本日期** | 只读回显 | **日期手填** | **差异** |
| 计划投产日期 | 批次带出只读 | 批次带出只读 | 同 |
| 问题单编号 | 文本★(查重) | 文本★(查重) | 同 |
| 问题单级别 | 下拉★ | 下拉★ | 同 |
| 提交日期 | 日期★ | 日期★ | 同 |
| 问题单关闭日期 | 日期 | 日期 | 同 |
| 是否缺陷/超时/问题重现/须关注 | 下拉★ | 下拉★ | 同 |
| 缺陷说明/超时说明 | 文本★ | 文本★ | 同 |
| 是否更新版本 | 下拉★ | 下拉★ | 同 |
| 当前状态 | 下拉★ | 下拉★ | 同 |
| 核查日期 | 日期★ | 日期★ | 同 |
| **排期状态** | 无（从任务带）| **下拉手选**（schedulingStatus）| **差异** |
| 备注 | 文本域 | 文本域 | 同 |

**一句话差异**：非批次把"软件中心任务号/任务名称/二级产品/三个测试日期/排期状态"从"任务库联动/只读回显"改成**手填/手选**，且批次号/项目组不再联动任务下拉。

## 四、列表页 / 详情页 / 附件

- **列表查询条件、列表列、详情字段**：与批次基本一致（任务相关列改为展示手填值）。
- **附件功能**：与批次完全相同（上传/下载/删除/查重/审计），附件表为 `T_B_NOBATCH_PROLIST_FILE`（结构同 `T_B_PROLIST_FILE`）。

## 五、核心业务规则

- 同批次：问题单=缺陷同记录、问题单编号查重、解决时间超一天计算、当前状态/问题单级别配置、增删改+附件审计。
- **无任务库联动、无任务审核校验**（任务信息纯手填；批次版本身也不校验审核，见文档 04 第七节.3）。

## 六、数据表

**主表 `T_B_NOBATCH_PROLIST_AND_DEFECT`**：在批次主表字段基础上，**额外冗余手填的任务字段** —— TASK_NO(软件中心任务号)、TASK_NAME(任务名称)、PRODUCT(二级产品)、TEST_SUB_B_DATE、TEST_VERSION_DATE、PRO_VERSION_DATE、SCHEDULING_STATUS(排期状态)、BATCH_ID。其余布尔标记/状态/级别/审计字段同批次主表。

**附件表 `T_B_NOBATCH_PROLIST_FILE`**：结构同 `T_B_PROLIST_FILE`。

## 七、落地 newpm 的设计考量（开发阶段，先记录）

1. **新建独立表 `pm_nobatch_prolist_defect`**（不与批次问题单共表 —— 与老系统一致，两张物理表），任务字段冗余手填，**无 task_id FK**（或可空）。
2. **附件复用 `pm_attachment`**（业务类型 `nobatch_prolist`）。
3. **权限独立 4 级**：`project:nobatchProlist:{query,edit,remove,file}`。
4. **字段裁剪/改造**：任务相关字段全部改手填/手选；二级产品/排期状态用字典下拉。
5. 当前状态/问题单级别/附件类型 → 字典（同批次问题单复用同一套字典）。

## 八、项目质量管理两件套差异速查

| 维度 | 批次问题单 | 非批次问题单 |
|---|---|---|
| 主表 | T_B_PROLIST_AND_DEFECT | T_B_NOBATCH_PROLIST_AND_DEFECT |
| 软件中心任务号 | 下拉(taskId,联动) | 文本手填 |
| 任务名称/二级产品/测试日期 | 任务库回显(只读) | 手填/手选 |
| 排期状态 | 从任务带 | 下拉手选 |
| 批次号/项目组 联动任务 | 是 | 否 |
| 附件/布尔标记/状态/级别/查重 | —— | 与批次相同 |
| 权限 | proListAndDefect* | proNoBatchListAndDefect* |
