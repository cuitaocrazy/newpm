# Feature Specification: 批次任务问题单及缺陷

**Branch**: `010-batch-problem-defect` | **Created**: 2026-06-14 | **Status**: Draft
**Input**: 迁移自老 yadapm「项目质量管理 → 批次任务问题单及缺陷」(`/proListAndDefect`)。新一级菜单「项目质量管理」下首个二级功能。含附件、4级权限、任务联动。
**需求**: `docs/pm/yadapm需求-04-批次任务问题单及缺陷.md`（已源码核实）
**源码核实**: ProListAndDefect{Controller,Service,Dao,Model} + proListAndDefect/{listDetail,create,edit,showDetail,proListFile}.html

## 已确认的架构决策（2026-06-14，用户拍板）

1. **项目组(subtaskTeam) → 部门(dept)**：老系统按 `年份+批次+项目组` 过滤任务号；newpm 改为 `年份+批次+部门`。部门取任务所属项目的 `project_dept`，过滤走 ancestors 层级（匹配自身或子部门）。问题单存 `dept_id`。
2. **配置项全部用字典**：当前状态→`sys_problem_state`、问题单级别→`sys_problem_level`、附件文档类型→`sys_prolist_file_type`（均照搬老配置表预置值）。
3. **附件复用 pm_attachment + pm_attachment_log**：新增业务类型 `prolist`，复用现成 AttachmentController（上传/下载/删除/审计已具备），不建独立附件表。

## User Scenarios & Testing

### User Story 1 - 登记问题单及缺陷 (Priority: P1, MVP)
质量管理人员选定 年份+批次+部门 下的某个任务，登记一条问题单：填问题单编号(查重)、级别、提交/核查日期、一组布尔标记(是否缺陷/超时/问题重现/须关注/更新版本)、缺陷说明、当前状态、备注。

**Why P1**: 核心录入能力，没有它整个模块无意义。
**Independent Test**: 选任务→填字段→保存→列表可见、详情正确。

**Acceptance Scenarios**:
1. **Given** 选年份，**When** 联动，**Then** 批次下拉刷新；选批次→带出计划投产日期 + 部门下拉；选部门→任务号下拉按 年份+批次+部门 过滤。
2. **Given** 选任务号，**When** 回显，**Then** 任务名称/二级产品/提交内部测试B包日期/提交功能测试版本日期/提交生产版本日期 只读带出。
3. **Given** 输入已存在的问题单编号，**When** 失焦查重，**Then** 提示重复、禁止保存。
4. **Given** 必填项齐全，**When** 保存，**Then** 入库并计算 solutionTimeOverOneDay（解决日期−提交日期>1天，解决日期空用当天）。

### User Story 2 - 查询与列表 (Priority: P2)
按多维条件（年份/批次/产品/任务/各布尔标记/当前状态/部门/问题单编号/创建人/日期范围）筛选问题单，列表展示约30列，布尔标记带颜色高亮。

**Acceptance Scenarios**:
1. **Given** 设置查询条件，**When** 查询，**Then** 分页过滤正确；一级产品→二级产品联动。
2. **Given** 列表行，**Then** 是否缺陷/超时/重现/须关注/解决超一天 按值高亮(是=警示色)。

### User Story 3 - 编辑/删除/导出 (Priority: P2)
编辑问题单(问题单编号查重排除自己——修复老系统bug)、删除、导出Excel。

**Acceptance Scenarios**:
1. **Given** 编辑现有问题单，**When** 保留原问题单编号，**Then** 查重不卡自己。
2. **Given** 删除，**Then** 软删除(del_flag)。
3. **Given** 导出，**Then** 生成含关联展示字段的 Excel。

### User Story 4 - 附件管理 (Priority: P3)
对问题单上传/下载/删除附件（文档类型字典、文件说明），复用通用附件体系，操作记审计日志。

**Acceptance Scenarios**:
1. **Given** 问题单，**When** 上传附件(选文档类型+文件+说明)，**Then** 入 pm_attachment(业务类型prolist)、记日志。
2. **Given** 已有附件，**When** 下载/删除，**Then** 正常且记日志。

### Edge Cases
- 任务审核状态**不校验**（老系统核实：任何任务都可登记问题单，与版本管理不同）。
- settleDate(解决日期)可空；空时 solutionTimeOverOneDay 用当天计算。
- 部门过滤需走 ancestors 层级。

## Requirements

- **FR-001**: 系统 MUST 支持登记问题单：年份/批次/部门/任务号(下拉联动) + 问题单编号(查重) + 级别 + 提交日期 + 核查日期 + 5个布尔标记 + 缺陷说明 + 当前状态 + 备注。
- **FR-002**: 任务号下拉 MUST 按 年份+批次+部门 过滤 `pm_task`（部门走 ancestors）；选任务回显只读任务信息。
- **FR-003**: 问题单编号 MUST 唯一，查重编辑时排除自己（修复老系统bug）。
- **FR-004**: 系统 MUST 计算派生字段 solutionTimeOverOneDay（解决日期−提交日期>1天=是，解决日期空用当天）。
- **FR-005**: 系统 MUST 提供多维分页查询 + 约30列列表，布尔标记带颜色高亮。
- **FR-006**: 系统 MUST 支持编辑/软删除/Excel导出。
- **FR-007**: 系统 MUST 支持附件上传/下载/删除（复用 pm_attachment，业务类型 prolist，文档类型字典 sys_prolist_file_type），记审计日志。
- **FR-008**: 4级权限：`project:prolistDefect:list/query`(查询详情)、`:edit`(增改导)、`:remove`(删)、`:file`(附件)。
- **FR-009**: 任务审核状态 MUST NOT 校验（与版本管理不同）。

## Key Entities
- **问题单及缺陷**：主表 `pm_prolist_defect`，FK `task_id`→`pm_task`。任务名/二级产品/各测试日期/计划投产日期/排期状态/状态名/级别名 关联实时取，不冗余。布尔标记和派生字段存主表。
- **附件**：复用 `pm_attachment`(业务类型 prolist)+`pm_attachment_log`。

## Success Criteria
- **SC-001**: 选任务后只读信息回显准确率100%；问题单编号查重(含编辑排除自己)正确。
- **SC-002**: solutionTimeOverOneDay 计算与老系统口径一致。
- **SC-003**: 部门过滤任务下拉与老系统"项目组过滤"语义对齐(年份+批次+部门)。
- **SC-004**: 附件上传/下载/删除复用通用体系，与合同/项目附件行为一致。

## Assumptions
- 部门映射：任务所属项目的 project_dept，过滤走 ancestors。
- 字典预置照搬老配置表：当前状态(已定位/待验证/未受理/已受理/问题已解决/问题再现)、级别(高/中/低优先级)、附件类型(相关材料)。
- 日期字段在 newpm 用标准 date 类型（老系统存 yyyymmdd 文本，迁移时转换）。
- 数据迁移延后，功能先建空表上线。
