# Feature Specification: yadapm 5 功能历史数据迁移

**Branch**: `012-data-migration` | **Created**: 2026-06-14 | **Status**: Draft
**Input**: 将老 yadapm Oracle 库（`exp` 备份 `yadapm_backup_20260611.dmp`，schema `YADAPM`）中 5 个已迁移功能涉及的历史数据，导入 newpm MySQL 库对应表。
**源数据**: `/Users/kongli/Documents/.../最新oracle数据库-20260611/yadapm_backup_20260611.dmp`（Oracle 经典 `exp` V11.02 导出，308MB）
**前置**: 功能代码已迁移完成（spec 007~011，已合入 main）；目标表已建空表。本特性只做**数据搬运**，不改功能。

## 背景与关键约束

- **源是 Oracle `exp` 私有二进制**：DDL 明文可读，但行数据是列流二进制，**取数基本只能用 Oracle `imp`/实例**。这是迁移路径的决定性约束（见待澄清 Q1）。
- **5 功能涉及的源表（已在 dump 中确认存在，DDL 已读取）**：
  | 老表 | 新表 | 功能 |
  |---|---|---|
  | `T_B_VERSION_OUT` (MANUAL_INPUT=0) | `pm_version_out` (manual_input=0) | ① 批次版本管理 |
  | `T_B_VERSION_OUT` (MANUAL_INPUT=1) | `pm_version_out` (manual_input=1) | ② 非批次版本管理 |
  | `T_B_OLD_VERSION_OUT` | `pm_old_version_out` | ③ 旧数据查询 |
  | `T_B_PROLIST_AND_DEFECT` | `pm_prolist_defect` | ④ 批次问题单及缺陷 |
  | `T_B_NOBATCH_PROLIST_AND_DEFECT` | `pm_nobatch_prolist_defect` | ⑤ 非批次问题单及缺陷 |
  | `T_B_PROLIST_FILE` | `pm_attachment`(business_type=prolist) | ④ 附件 |
  | `T_B_NOBATCH_PROLIST_FILE` | `pm_attachment`(business_type=nobatch_prolist) | ⑤ 附件 |
  - **辅助/参照表**（用于 FK/ID 重映射，不直接迁移成业务记录）：`T_B_TASK`、`T_C_BATCH_STATUS`(批次)、`T_C_CURRENT_STATE`、`T_C_PROBLEM_LEVEL`、`T_C_PROLISTFILE_TYPE`、`T_TEAM`(项目组)。

## User Scenarios & Testing

### User Story 1 - 版本数据迁移（①②③，无附件无复杂FK） (Priority: P1)
将 `T_B_VERSION_OUT`(批次/非批次)、`T_B_OLD_VERSION_OUT` 三类版本记录搬到 newpm，字段一一映射、类型转换（yyyymmdd 文本→date、产品取 SUB_VERSION_CODE 等），批次/非批次按 MANUAL_INPUT 落到同一 `pm_version_out`。旧数据查询表是扁平快照，直接搬入 `pm_old_version_out`。

**Why P1**: 这三类无附件、FK 简单（仅批次/年份），是迁移的低风险打底，先跑通验证管道。
**Independent Test**: 迁移后在 newpm 三个列表页能查到老数据，关键字段（版本号/投产日期/产品/任务号）与老库一致，条数吻合。

**Acceptance Scenarios**:
1. **Given** 老库 T_B_VERSION_OUT 有 N 条，**When** 迁移，**Then** pm_version_out 新增 N 条（manual_input 正确区分批次/非批次），版本号/产品/批次/日期映射正确。
2. **Given** 老库 T_B_OLD_VERSION_OUT 有 M 条，**When** 迁移，**Then** pm_old_version_out 有 M 条，18 列值与老库一致。
3. **Given** 日期字段老存 yyyymmdd 文本，**When** 迁移，**Then** 新库 date 列正确（非法/空值落 NULL 不报错）。

### User Story 2 - 问题单数据迁移（④⑤，含FK重映射） (Priority: P2)
迁移批次/非批次问题单主表，处理 ID/FK 重映射：老 `TASK_ID`→新 `pm_task.task_id`、老 `BATCH_ID`→新 `pm_production_batch.batch_id`、老 `CURRENT_STATUS_ID`/`PROBLEM_LEVEL_ID`→新字典值、老 `SUBTASK_TEAM`(项目组名)→新 `dept_id`、`SOLUTINO_TIME_OVER_ONE_DAY` 字段名 typo 修正。

**Acceptance Scenarios**:
1. **Given** 老问题单引用 TASK_ID=x，**When** 迁移，**Then** 新记录 task_id 指向 newpm 中对应任务（按业务键匹配），匹配不上的按约定处理（见 Q2）。
2. **Given** 老 CURRENT_STATUS_ID=2，**When** 迁移，**Then** 新 current_status= 对应字典值（已定位/待验证/…按 T_C_CURRENT_STATE 顺序映射）。
3. **Given** 老 SUBTASK_TEAM='COR项目组'，**When** 迁移，**Then** 新 dept_id= 对应部门（按 Q2 映射规则）。

### User Story 3 - 附件数据迁移（④⑤附件） (Priority: P3)
迁移 `T_B_PROLIST_FILE`/`T_B_NOBATCH_PROLIST_FILE` 到 `pm_attachment`，business_type 区分，business_id 指向迁移后的问题单 id，file_type→文档类型字典，并处理物理文件（见 Q3）。

**Acceptance Scenarios**:
1. **Given** 老附件记录指向问题单 PROBLEM_ID=p，**When** 迁移，**Then** pm_attachment 新记录 business_id= 迁移后该问题单的新 id。
2. **Given** 物理文件按 Q3 决策处理，**Then** 详情页附件列表能展示（可下载 / 仅元数据 按 Q3）。

### Edge Cases
- 日期文本非法（如空串、'00000000'）→ 落 NULL。
- FK 匹配不上的任务/批次/部门 → 按 Q2 约定（跳过/置空/记录到差异报告）。
- 问题单编号在新库已存在（理论上空库不会）→ 记冲突，不覆盖。
- 迁移可重复执行（幂等）：重跑不产生重复（按业务键去重或先清后插）。

## Requirements

- **FR-001**: 系统 MUST 从 Oracle `exp` dump 提取 5 功能 7 张源表的全部数据行（提取方式见 Q1）。
- **FR-002**: 系统 MUST 将源数据按字段映射表转换并写入对应 newpm MySQL 表，含类型转换（yyyymmdd→date、NUMBER→bigint、NVARCHAR2→utf8mb4）。
- **FR-003**: 系统 MUST 对问题单的 TASK_ID/BATCH_ID/状态/级别/项目组做 FK/ID 重映射（规则见 Q2）。
- **FR-004**: 系统 MUST 迁移附件元数据到 pm_attachment（物理文件处理见 Q3）。
- **FR-005**: 迁移 MUST 可重复执行（幂等），重跑不产生重复数据。
- **FR-006**: 系统 MUST 产出迁移报告：每表源条数/成功条数/失败或跳过条数 + 差异明细（FK 未匹配等）。
- **FR-007**: 迁移 MUST 可校验：迁移后各表条数、关键字段抽样与老库一致。
- **FR-008**: 迁移 MUST NOT 影响 newpm 现有数据/功能（只新增历史数据，不动既有记录）。

## Key Entities
- **版本记录**：T_B_VERSION_OUT/T_B_OLD_VERSION_OUT → pm_version_out/pm_old_version_out。
- **问题单及缺陷**：T_B_(NOBATCH_)PROLIST_AND_DEFECT → pm_(nobatch_)prolist_defect，需 FK 重映射。
- **附件**：T_B_(NOBATCH_)PROLIST_FILE → pm_attachment（+ 物理文件）。
- **映射参照**：T_B_TASK/T_C_BATCH_STATUS/T_C_*/T_TEAM ↔ pm_task/pm_production_batch/字典/sys_dept。

## Success Criteria
- **SC-001**: 5 功能列表页能查到迁移来的历史数据，条数与老库逐表吻合（误差 0，或差异有报告解释）。
- **SC-002**: 关键字段（版本号/问题单编号/投产日期/任务号/产品）抽样比对，与老库一致率 100%。
- **SC-003**: 问题单 FK 重映射后，详情页任务/批次/部门/状态/级别正确显示（匹配不上的按报告可追溯）。
- **SC-004**: 迁移可重跑且幂等，重跑后条数不翻倍。
- **SC-005**: newpm 既有数据零改动、5 功能页面零报错。

## Assumptions
- 目标表（pm_version_out/pm_old_version_out/pm_prolist_defect/pm_nobatch_prolist_defect/pm_attachment + 3 字典 + 菜单）均已建好（spec 007~011）。
- 迁移先在**本地 Docker MySQL** 演练验证，确认无误后再在**生产 K3s** 执行（生产需先跑 migration_007~011 建表）。
- 提交人员(comm_name)、问题单创建人(creator) 等是老库存的**用户名/人名文本**，迁移按文本保留（不强行映射到 sys_user，匹配不上不阻断）。
- 老库日期为 yyyymmdd/yyyymmddhhmmss 文本，按格式解析为 MySQL date/datetime，非法值落 NULL。

## 已澄清的迁移方案决策（2026-06-14，用户拍板）

- **Q1 取数方式 → Docker 起 Oracle 导入**：本地 Docker 起 Oracle 实例，用 `imp` 导入 `yadapm_backup_20260611.dmp`（exp V11.02），再把 7 张源表导出为 CSV/SQL，转换后灌入 MySQL。全流程由我搭建。
  - 注意：exp dump 需 `imp`（非 `impdp`）；Oracle 镜像需兼容 V11.02 导出（11g 或更高 `imp` 通常向下兼容）。
- **Q2 FK 重映射 → 用户提供映射表**：老 `TASK_ID→新 task_id`、老 `BATCH_ID→新 batch_id`、老 `SUBTASK_TEAM(项目组名)→新 dept_id` 的对照由用户提供（或用户把老 T_B_TASK/T_C_BATCH_STATUS/T_TEAM 一并迁出供匹配）。迁移脚本消费该映射表做精确重映射；映射表未提供前，问题单(④⑤)迁移阻塞，版本数据(①②③)可先行。
  - 配置 ID（CURRENT_STATUS_ID/PROBLEM_LEVEL_ID/FILE_TYPE）→ 字典值：可由 `T_C_CURRENT_STATE`/`T_C_PROBLEM_LEVEL`/`T_C_PROLISTFILE_TYPE` 的 ID 顺序直接推导（新字典值按老 ID 建），无需用户额外提供。
- **Q3 附件 → 只迁元数据（先）**：`T_B_(NOBATCH_)PROLIST_FILE` 只迁 DB 记录到 `pm_attachment`（文件名/类型/说明/上传人/时间），详情页可见附件列表但暂不可下载（物理文件 file_path 置为原 file_url 或标记缺失）。物理文件搬运作为独立后续任务。

## 实施顺序（按依赖与风险）
1. **环境**：Docker Oracle + imp 导入 dump（一次性）；本地 MySQL 演练。
2. **US1 版本数据**（①②③）：无 FK，先跑通管道。
3. **US2 问题单**（④⑤）：依赖用户映射表（Q2）。
4. **US3 附件元数据**（④⑤附件）：依赖 US2 完成（business_id 指向迁移后问题单）。
5. 校验报告 + 生产执行（生产先建表）。
