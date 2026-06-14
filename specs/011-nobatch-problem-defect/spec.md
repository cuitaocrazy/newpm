# Feature Specification: 非批次任务问题单及缺陷

**Branch**: `011-nobatch-problem-defect` | **Created**: 2026-06-14 | **Status**: Draft
**Input**: 迁移自老 yadapm「项目质量管理→非批次任务问题单及缺陷」(`/proNoBatchListAndDefect`)。「项目质量管理」第二个二级功能。与 ④ 批次问题单业务相同，**区别：任务信息全手填/手选，不走任务库联动**。
**需求**: `docs/pm/yadapm需求-05-非批次任务问题单及缺陷.md`（已源码核实）
**蓝本**: ④ spec 010（批次问题单）。共用业务规则见 010。

## 与 ④ 批次问题单的差异（源码核实，2026-06-14）

| 维度 | ④批次 | ⑤非批次 |
|---|---|---|
| 主表 | pm_prolist_defect | **pm_nobatch_prolist_defect（独立表）** |
| 软件中心任务号 | 下拉(taskId,联动) | **文本手填(taskNo)** |
| 任务名称 | 任务库回显只读 | **文本手填** |
| 二级产品 | 任务库回显只读 | **字典下拉手选(sys_product)** |
| 三个测试日期 | 任务库回显只读 | **日期手填** |
| 排期状态 | 从任务带 | **字典下拉手选(sys_pqzt)** |
| 批次号/项目组 联动任务 | 是 | **否（仅批次号联动计划投产日期）** |
| 任务字段存储 | JOIN pm_task 取 | **冗余实存主表** |
| 权限 | project:prolistDefect:* | **project:nobatchProlist:*** |

**相同点**：问题单=缺陷同记录、5布尔标记、问题单编号查重(编辑排除自己)、solutionTimeOverOneDay派生算法、当前状态/级别字典、附件复用 pm_attachment、软删除_DEL_腾位、投产年份→批次→计划投产日期联动。

## User Scenarios & Testing

### User Story 1 - 登记非批次问题单 (P1, MVP)
质量管理人员手填任务信息(任务号/任务名/二级产品/测试日期/排期状态)，选投产年份+批次(带出计划投产日期)+项目组，登记问题单。

**Acceptance**:
1. 选年份→批次下拉刷新；选批次→带出计划投产日期(不联动任务)。
2. 软件中心任务号/任务名称文本手填；二级产品/排期状态字典下拉手选；三测试日期手填。
3. 问题单编号查重；保存计算 solutionTimeOverOneDay。

### User Story 2 - 查询列表 (P2)
多维查询 + 列表(任务列展示手填值，布尔颜色高亮)。

### User Story 3 - 改/删/导出 (P2)
编辑(查重排除自己)、软删除、导出。

### User Story 4 - 附件 (P3)
上传/下载/删除，复用 pm_attachment(business_type=`nobatch_prolist`)。

## Requirements
- **FR-001**: 登记问题单，任务信息全手填/手选(任务号文本、任务名文本、二级产品sys_product、三测试日期、排期状态sys_pqzt)。
- **FR-002**: 投产年份→批次→计划投产日期联动；**不联动任务**。
- **FR-003**: 问题单编号唯一，查重编辑排除自己。
- **FR-004**: 派生 solutionTimeOverOneDay(同④)。
- **FR-005**: 多维分页查询 + 列表布尔颜色高亮。
- **FR-006**: 编辑/软删除/导出。
- **FR-007**: 附件复用 pm_attachment(business_type=nobatch_prolist，文档类型字典sys_prolist_file_type)。
- **FR-008**: 4级权限 project:nobatchProlist:list/query/edit/remove/file。

## Key Entities
- **非批次问题单及缺陷**：独立表 `pm_nobatch_prolist_defect`，任务字段(task_no/task_name/product/三测试日期/schedule_status)冗余实存，无 task_id FK。批次/部门/计划投产日期 JOIN pm_production_batch/sys_dept 取展示。
- **附件**：复用 pm_attachment(nobatch_prolist)+pm_attachment_log。

## Success Criteria
- **SC-001**: 任务信息手填保存/回显正确。
- **SC-002**: solutionTimeOverOneDay 与老系统口径一致。
- **SC-003**: 与批次问题单零关联零串扰（独立表独立权限）。

## Assumptions
- 独立表，任务字段冗余手填，复用③④建的3个字典。
- 二级产品 sys_product、排期状态 sys_pqzt 字典下拉。
- 数据迁移延后，先建空表上线。
