# Feature Specification: 非批次版本管理（出入库版本·手动输入）

**Feature Branch**: `008-non-batch-version-management`
**Created**: 2026-06-13
**Status**: Draft
**Input**: 迁移自老 yadapm「出入库版本管理 → 非批次版本管理」(`/storageManual`)。与批次版本管理共用主表 `pm_version_out`（`manual_input='1'` 区分），任务信息手填、不走任务库联动，砍掉组包方式/版本状态/版本简介/多任务。

> 需求来源：`docs/pm/yadapm需求-02-非批次版本管理.md` + 旧系统源码 `storageManual/*`（已逐文件核实，2026-06-13）。
> 蓝本：批次版本管理 spec 007（复用 `pm_version_out`、`VersionNumberGenerator`、`pm_sys_name`、版本类型/组包方式字典）。

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 新增非批次版本并自动生成出入库版本号 (Priority: P1)

版本管理人员对**不走正式批次任务流程**的版本（临时/插单/特殊任务）登记一条记录：选年份+批次+产品+子系统+版本类型，**手填**软件中心任务号与任务名称，系统按版本类型自动生成唯一出入库版本号并保存。

**Why this priority**: 这是本模块核心价值。版本号生成复用批次的 `VersionNumberGenerator`（逻辑相同），但任务信息改手填、字段裁剪是非批次的本质特征。

**Independent Test**: 录入一条非批次版本（手填任务号/任务名），确认版本号正确生成、`manual_input='1'`、与批次数据互不串扰，即交付价值。

**Acceptance Scenarios**:
1. **Given** 选定子系统+版本类型"SP升级包"，**When** 提交新增，**Then** 生成 `{基准版本号}_SP{序号}` 版本号，记录 `manual_input='1'`。
2. **Given** 手填软件中心任务号和任务名称，**When** 保存，**Then** 两值存入 `manual_task_no`/`manual_task_name`（不进 `pm_version_out_task`）。
3. **Given** 选定投产批次，**When** 批次确定，**Then** 版本投产日期自动带出且只读。
4. **Given** 当前登录用户，**When** 打开新增页，**Then** 提交人员只读显示当前用户昵称、不可改。

### User Story 2 - 查询、筛选与查看非批次版本列表 (Priority: P2)

按多条件检索非批次版本并查看详情，**只看非批次数据**（不混批次）。

**Why this priority**: 登记后必须可查可看，依赖 P1 数据。

**Independent Test**: 按年份/批次/任务号/版本类型/提交人员等筛选，列表只返回 `manual_input='1'` 记录，可进详情。

**Acceptance Scenarios**:
1. **Given** 库中有批次与非批次记录，**When** 进入非批次列表，**Then** 只显示非批次（manual_input='1'）记录。
2. **Given** 列表记录，**When** 点详情，**Then** 展示全字段（含手填任务号/任务名、审计字段）。

### User Story 3 - 编辑与删除非批次版本 (Priority: P3)

修订（关键字段变更重算版本号）或软删除非批次版本。

**Why this priority**: 完整 CRUD 的补充能力。

**Independent Test**: 改备注号不变、改子系统/版本类型号重算；删除留审计。

**Acceptance Scenarios**:
1. **Given** 一条非批次版本，**When** 改非关键字段保存，**Then** 出入库版本号不变。
2. **Given** 改子系统或版本类型，**When** 保存，**Then** 按规则重算版本号。
3. **Given** 删除，**When** 确认，**Then** 软删除（del_flag='1'）+ 审计日志。

### User Story 4 - 导出非批次版本为 Excel (Priority: P3)

把当前查询结果导出 Excel。

**Independent Test**: 有结果时导出，含列表全字段（任务号/任务名为手填值，字典转中文）。

**Acceptance Scenarios**:
1. **Given** 查询结果，**When** 导出，**Then** 生成含全部列的 Excel。

### Edge Cases

- 版本类型 5/6（B包/SP包升级包）必须先选初级版本号，否则阻止生成。
- 同子系统+版本类型并发新增不撞号（唯一键+重试）。
- 非批次查询必须严格 `manual_input='1'`，绝不串到批次数据。

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统 MUST 允许有 `project:versionOutManual:add` 权限的用户新增非批次版本，含投产年份、投产批次、产品、子系统、版本类型、**手填软件中心任务号、手填任务名称**、是否涉及TWS改造、数据库是否修改、接口是否修改、版本说明、备注。
- **FR-002**: 系统 MUST 复用 `VersionNumberGenerator` 按版本类型自动生成出入库版本号（6 类型规则与批次逐字相同，见需求文档02 §6.1），用户只读不可改。
- **FR-003**: 系统 MUST 在年份选定后联动批次、产品选定后**仅联动子系统**（非批次不联动任务）、子系统+类型选定后联动/计算初级版本号。
- **FR-004**: 系统 MUST 把手填的软件中心任务号、任务名称存入 `pm_version_out.manual_task_no`/`manual_task_name`（**不进 `pm_version_out_task`**）。
- **FR-005**: 系统 MUST 选批次后自动带出版本投产日期，只读。
- **FR-006**: 系统 MUST 支持按投产年份、投产批次号、软件中心任务号、版本类型、提交人员、基准版本号、投产日期、产品、出入库版本号分页查询，**硬编码 `manual_input='1'`**。
- **FR-007**: 系统 MUST 提供详情查看（全字段+审计）。
- **FR-008**: 系统 MUST 支持编辑，关键字段（子系统/版本类型/子产品）变更则重算版本号，否则保持原号。
- **FR-009**: 系统 MUST 支持软删除（del_flag='1'）。
- **FR-010**: 系统 MUST 支持查询结果导出 Excel。
- **FR-011**: 系统 MUST 对增删改记 `@Log` 审计。
- **FR-012**: 系统 MUST 用独立权限 `project:versionOutManual:{list,query,add,edit,remove,export}` 保护各接口。
- **FR-013**: 系统 MUST 保证出入库版本号在同子系统+版本类型范围唯一，并发不撞号。
- **FR-014**: 提交人员 MUST 只读固定当前登录用户（新增），编辑时只读显示记录原始提交人。

### Key Entities

- **非批次版本**：`pm_version_out` 中 `manual_input='1'` 的记录。复用批次全部字段，**额外用 `manual_task_no`/`manual_task_name` 存手填任务信息**；不使用 version_brief/package_mode/version_status（非批次无这些）。
- **子系统配置**：复用 `pm_sys_name`（产品→子系统→基准版本号）。
- **投产批次/年份/产品/版本类型**：复用 `pm_production_batch` + 字典 `sys_ndgl`/`sys_product`/`sys_version_type`。

## Success Criteria *(mandatory)*

- **SC-001**: 录入一条非批次版本（含手填任务）2 分钟内完成，版本号即时正确生成。
- **SC-002**: 6 类型版本号生成 100% 正确（复用批次算法，特征测试覆盖）。
- **SC-003**: 非批次列表零串扰——只显示 manual_input='1'，批次记录 0 误现。
- **SC-004**: 手填任务号/任务名正确存取于 manual_task_no/manual_task_name。
- **SC-005**: 增删改审计覆盖率 100%。

## Assumptions

- **复用批次资产**：版本号生成器、子系统配置表、字典、投产批次实体全部复用，不重建。
- **任务信息手填**：非批次不查任务库、不回显项目/需求名（旧系统 storageManual 无 `ajax_getCenterTaskNo`/`ajax_getPrjDemandBatchNo`，已核实）。
- **字段裁剪**：非批次无组包方式/版本状态/版本简介/多任务行（旧系统 create.html 已核实无这些）。
- **独立权限/菜单**：独立权限点 `project:versionOutManual:*` + "出入库版本管理"下新增二级菜单（用户决策 2026-06-13；旧系统实为共用 storageManagement 权限）。
- **数据迁移延后**：非批次存量数据迁移（TASK_NO→manual_task_no、TASK_NAME→manual_task_name）属独立后续任务，依赖旧库数据到位。
