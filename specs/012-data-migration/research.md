# 数据迁移 — 真实数据勘察结果（导入 Oracle 后实测，2026-06-14）

> 用 Docker Oracle XE 11 `imp` 导入 `yadapm_backup_20260611.dmp` 后，实测真实数据。这些是**需求文档/源码都看不出、只有真实数据才暴露**的关键事实，是 plan 的依据。

## ★ 业务决策（与同事沟通确认，2026-06-22）

1. **5 个功能的「投产批次号」都指向「投产批次管理」(`pm_production_batch`)**——批次是 5 功能共享维度，迁移时批次引用统一解析到此表。
2. **新旧系统任务号/批次号数据有冲突时，以新系统为准**。具体落到本次重叠报告：
   - **任务号重叠 7 个**：按 task_code 匹配的，历史记录的任务 FK 挂到**新系统对应任务**；属性冲突取新系统值。
     - 2 个编号撞车任务（M-202510-00065、M-202511-00610，均属谈旭/TMS组）：以**新系统那条任务为准**（即历史问题单挂到新系统同号任务，显示新任务名）。
   - **批次号重叠 5 个**：挂到投产批次管理对应批次；`26年6月独立`计划投产日期冲突（老06-25 vs 新06-26）→ **取新系统 06-26**。
3. **【已定·领导确认 2026-06-23】采用 A 反范式化（文本快照）**：老数据引用但新系统不存在的 195 个任务 / 213 个批次，历史记录**自带任务名/批次号文本快照、task_id/batch_id 置空**，不挂 FK、不注入 pm_task/pm_production_batch。
   - **不碰 pm_task / pm_production_batch**（任务管理/投产批次管理台账保持干净，旧任务不出现在日常菜单）。
   - 显示逻辑兜底：有 FK(新数据)→JOIN 取；无 FK(历史快照)→读本表文本列。新数据关联新任务的现有功能完全不变。
   - 需加快照列：④ pm_prolist_defect 加 `task_no/task_name/product/internal_closure_date/functional_test_date/production_version_date/schedule_status/pro_batch_no`(8列)；⑤ pm_nobatch_prolist_defect 加 `pro_batch_no`(1列)；①②③ 已足够文本化无需加。
   - 重叠的 7 任务/5 批次仍按"以新系统为准"挂 FK；其余走快照。

## 一、真实行数（迁移工作量）

| 源表 | 行数 | 目标 | 功能 |
|---|---|---|---|
| T_B_VERSION_OUT | 2679 (批次1612 / 非批次1067) | pm_version_out | ①② |
| T_B_OLD_VERSION_OUT | 1231 | pm_old_version_out | ③ |
| T_B_PROLIST_AND_DEFECT | 2958 | pm_prolist_defect | ④ |
| T_B_NOBATCH_PROLIST_AND_DEFECT | 299 | pm_nobatch_prolist_defect | ⑤ |
| T_B_PROLIST_FILE | 47 | pm_attachment(prolist) | ④附件 |
| T_B_NOBATCH_PROLIST_FILE | **0** | — | ⑤附件(无数据) |
| **合计业务行** | **~7214** | | |
| T_B_TASK(参照) | 1367 | 匹配 pm_task | FK |
| T_C_BATCH_STATUS(参照) | 218 | 匹配 pm_production_batch | FK |

## 二、关键数据坑（实测发现）

### 坑A：当前状态字典 6 → 必须扩到 16（影响 ④⑤ 224+ 条）
- `T_C_CURRENT_STATE` 生产有 **16 个有名状态**(id1-16)+1个空(id17)，但我们 `sys_problem_state` 只照搬源码预置的 **6 个**。
- 批次问题单实际引用 status_id 分布：`5(2695) / 7(223) / 4(27) / 2(12) / 9(1)`——**id 7、9 共 224 条用到了字典外的值**，前端 dict-tag 会显示空白。
- id 7-16 是转派类状态：已转至外部系统处理 / 已转至HHAP-CFS排查 / HHAP-TMS / HHAP-ICA / HHAP-CSKJ / COR-商户服务组 / COR-特色业务组 / COR-快捷业务组 / 转出至HHAP / 转出至WCB。
- **对策**：迁移前先把 `sys_problem_state` 字典从 6 扩到 16（值=老 id，标签=老 CURRENT_STATUS）。

### 坑B：comm_name / creator 存的是老系统 user_id（数字），非姓名
- `T_B_VERSION_OUT.COMM_NAME='382'/'261'`、`T_B_PROLIST_AND_DEFECT.CREATOR='103'` ——都是老 yadapm 的**用户 id 数字**。
- 这些 id 与 newpm `sys_user.user_id` **不是同一套**，直接搬过去前端会显示数字。
- **待决策**：①保留数字文本(最省，显示数字)；②另迁老用户表做 id→姓名 映射后存姓名文本；③留空。建议先①，姓名映射作为后续优化。

### 坑C：日期格式跨表不一致
- `T_B_VERSION_OUT.VERSION_P_DATE='20211105'`、`T_B_PROLIST_*.SUBMIT_DATE='20220427'`、`CREATION_DATE='20220427171903'` —— **yyyymmdd / yyyymmddhhmmss**（无分隔）。
- `T_B_OLD_VERSION_OUT.VERSION_P_DATE='2016-02-20'` —— **yyyy-mm-dd**（带横杠）！与上面不同。
- **对策**：转换函数要**同时兼容** yyyymmdd、yyyymmddhhmmss、yyyy-mm-dd；非法/空值(如 '空'、''、'00000000')落 NULL。

### 坑D：sub_version_code(产品) 跨表语义不一致
- `T_B_VERSION_OUT.SUB_VERSION_CODE='19'/'26'`（**数字编码**）。
- `T_B_OLD_VERSION_OUT.SUB_VERSION_CODE='HHAP-CFS'/'空'`（**文本产品名**，'空'是字面值）。
- 而 newpm `product` 走字典 `sys_product`(值如 HHAP-COR)。版本管理的产品 '19'/'26' 是数字，**需查老产品字典表解码**或从 out_lib_version 前缀(HHAP-CFS_...)解析。**待 plan 细化**。

### 坑E：项目组(subtask_team) 10 个真实组名，需映射表(Q2)
- 非批次问题单 subtask_team 实际值：`CFS批次组(109) / COR特色组(66) / TMS批次组(32) / CFS信创组(22) / COR快捷组(17) / COR信创组(17) / CFS项目组(14) / WCB项目组(14) / TMS项目组(4) / COR商服组(4)`。
- 与 newpm 部门(开发一组（COR项目组）/开发二组（CFS项目组）/…)**非 1:1**，必须靠用户提供的映射表(Q2)。

### 坑F：⑤非批次附件 0 条、④批次附件仅 47 条
- T_B_NOBATCH_PROLIST_FILE 空表——⑤附件迁移无数据可迁。
- T_B_PROLIST_FILE 仅 47 条，且 Q3 决定只迁元数据。

### 坑G（最关键）：老数据引用的任务/批次，newpm 几乎没有 → FK 重映射前提崩塌
- newpm `pm_task` 仅 **~38 个任务(2025-2026)**；老库被问题单引用的任务跨 **2021-2026 共 ~2900 个**(2021:779/2022:279/2023:627/2024:498/2025:709/2026:2)。
- newpm `pm_production_batch` 仅 **17 个 2026 批次(2601批次…)**；老批次是 `企架1.1批次` 等完全不同的历史批次。
- **结论**：历史数据引用的任务/批次绝大多数在 newpm 不存在 → Q2 的"老id→新id 映射表"前提不成立（新 id 根本没有）。必须改策略。
- 各目标表对 task/batch 的存储能力：
  - ③ pm_old_version_out：全文本无 FK → **可直接迁**。
  - ①② pm_version_out：有 `pro_batch_no`(文本)、`product`(文本)、`manual_task_no/name`(文本)，batch_id 可空 → 大体可迁；①批次的任务关联走 `pm_version_out_task`(FK) 需另议。
  - ⑤ pm_nobatch_prolist_defect：任务字段全文本(self-contained)，但 batch_id/dept_id 是 FK 且无 batch_no 文本列。
  - ④ pm_prolist_defect：**纯 FK**(task_id/batch_id)，任务名/产品/测试日期/批次号/计划投产日期全靠 JOIN pm_task/pm_production_batch → 历史记录这些列**全空白**。
- **待用户决策（见报告 A/B/C）**：反范式化 / 注入老任务批次 / 缩范围。

## 三、取数管道（已跑通，可复用）
1. `docker run -d --name yadapm-oracle -p 1521:1521 -e ORACLE_PASSWORD=oracle -v <dumpdir>:/dump:ro gvenzl/oracle-xe:11-slim`
2. 建用户：`CREATE USER YADAPM IDENTIFIED BY yadapm; GRANT CONNECT,RESOURCE,DBA TO YADAPM;`
3. 导入：`imp system/oracle@localhost/XE file=/dump/yadapm_backup_20260611.dmp fromuser=YADAPM touser=YADAPM grants=n statistics=none ignore=y`
4. 连接查询：`sqlplus -S YADAPM/yadapm@localhost/XE`
- 导出到 MySQL 的机制(plan 定)：建议 Python `oracledb`(thin,免客户端) 读 Oracle + `pymysql` 写 MySQL，做类型转换/FK重映射/幂等，比 CSV 中转更稳(中文+逗号+长文本)。
