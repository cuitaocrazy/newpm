# Phase 0 Research: 日报保存的工时保护与项目归属校验

**Feature**: 015-daily-report-ownership-check
**Date**: 2026-07-30

## 核心问题

`DailyReportServiceImpl.saveDailyReport()` 采用「整天替换」：

```java
detailMapper.deleteByReportId(existingReportId);   // 清空该日全部明细
detailMapper.batchInsert(detailList);              // 按提交内容重建
```

该实现隐含一个**未被声明的契约**：客户端提交的明细集合 = 该日应有的全集。

而前端 `write.vue` 的表单行由「当前可填项目」驱动：

```javascript
formList.value = projects.value.map(p => {...})   // projects = GET /myProjects
for (const item of formList.value) { details.push({...}) }   // 保存只遍历 formList
```

`myProjects` 排除了已结项项目（`project_stage='11'`）等状态，因此一旦某项目变为不可填，
前端提交的就是**子集**，差集落入 `DELETE` 范围而 `INSERT` 不补回 → 静默丢失。

**本轮研究要解决的关键未知**：服务端如何判定「哪些既有明细属于本次提交的作用范围之外」，
从而只删该删的、保留该留的。

---

## Decision 1: 作用范围的判定方式

**Decision**: 服务端在保存时自行计算「可填项目集合」，复用 `selectMyProjects()` 的同一口径，
以该集合作为本次提交的作用范围。范围内的既有明细按提交内容替换；范围外的既有明细原样保留。

**Rationale**:

- 前端 `formList` 本就由 `GET /project/dailyReport/myProjects` 驱动，服务端复用同一 Service 方法，
  两侧对「可见」的定义是**同一份代码**，不存在口径漂移
- 前端零改动，符合 spec Assumptions（不改变填写页展示规则）
- 所有写入入口（桌面填写页、移动端 H5、任何未来入口）自动获得保护，满足 FR-004
- 判定成本极低：单日报明细 1–6 条、涉及项目最多 6 个（生产实测），`selectMyProjects()` 每次保存
  调用一次即可

**Alternatives considered**:

| 方案 | 否决原因 |
|---|---|
| 前端在请求中显式声明「本次覆盖了哪些项目」 | 需改桌面版 + 移动端 H5 两处；漏改一处仍会丢；且新增入口时会再次遗漏 |
| 前端回传每条明细的主键，服务端按主键差量 | 同样需改两处前端；且明细主键暴露给前端并无必要 |
| 后端改为「按明细逐条 upsert」，完全不删 | 无法表达「填报人主动清零工时」的删除意图（FR-002 要求保留该能力） |
| 让 `myProjects` 包含已结项项目、前端置灰只读 | 属体验增强，可作为后续独立改进；但仍需前端配合，无法覆盖所有入口，不能替代服务端兜底 |

**风险与缓解**：若未来前端改为「只提交被修改过的行」（而非可见行全量），则服务端的范围假设会偏大，
导致未提交但可见的明细被误删。缓解：在 `write.vue` 的 `handleSave` 与服务端方法上互相注明该契约，
并由 e2e 用例锁定「可见项目工时清零后被删除」的行为（FR-002 / SC-004）。

---

## Decision 2: 删除范围的收窄方式

**Decision**: 新增 `deleteByReportIdInScope(reportId, visibleProjectIds)`，SQL 条件为：

```sql
delete from pm_daily_report_detail
where report_id = #{reportId}
  and ( project_id is null
        <if test="visibleProjectIds != null and visibleProjectIds.size() > 0">
        or project_id in (...)
        </if> )
```

原 `deleteByReportId` 不再有调用方——**整条删除日报的路径同样改用按范围删除**（见 Decision 9）。
保留该方法定义但标注禁用说明，防止后来者误用而重新引入丢失。

**Rationale**:

- `project_id is null` 覆盖全部非项目工时（生产实测：`work` 类型 15661 条 `project_id` 全部有值；
  其余 7 种类型共 1282 条 `project_id` 全部为 NULL），它们在填写页始终可见，应按提交内容处理
- 可见集合为空时（该用户当前无可填项目），`<if>` 使条件退化为仅删非项目工时，语法安全
- 不改动原方法签名，避免影响整条删除日报的既有行为

**Alternatives considered**: 在 Java 侧先查出旧明细、逐条判断后按主键删除——多一次查询且需处理主键集合，
SQL 侧一次性表达更简洁，且天然在同一事务内。

---

## Decision 3: 「曾是成员」的判据与批量校验

**Decision**: 归属校验以 `pm_project_member` 中存在该 `(project_id, user_id)` 行为判据，
**不限 `is_active` / `del_flag`**（即"曾以任意身份参与过"）。新增批量查询：

```
selectEverMemberProjectIds(userId, projectIds) → 该用户曾参与的项目 id 子集
```

服务端取提交明细中的 `projectId` 去重集合，一次查询得到合法子集，差集即为非法项目。

**Rationale**:

- 成员行只能由持项目编辑权限者写入，填报人**无法自助获得**，是不可伪造的凭据
  （与 Issue #5 给团队日报离场分支加的安全约束是同一维度，读写两侧门槛统一）
- 不能改用 `selectMyProjects()` 做判据：它混入了项目生命周期状态
  （`approval_status='1'` / `project_status='0'` / `project_stage!='11'`），
  生产实测会误拒 **126 组 (人,项目) / 11213.5 小时**的历史日报编辑，
  其中 92 组是"项目已结项"——那部分该由 Decision 4 的结项校验单独处理，语义更清晰
- 批量规模极小：单日报涉及项目最多 6 个，一次 `IN` 查询即可

**Alternatives considered**:

| 方案 | 否决原因 |
|---|---|
| 用 `selectMyProjects()` 判据 | 误拒 126 组历史组合；把「身份关系」与「状态时效」两个正交概念混为一谈 |
| 只校验 `is_active='1'` 的在册成员 | 会让离场成员无法维护历史工时，违反 FR-006 与 User Story 4；生产有 8 组此类残留 |
| 加外键约束由数据库保证 | 外键只能保证项目存在，无法表达「该人与该项目有关系」 |

---

## Decision 4: 结项校验的落点

**Decision**: 对提交明细中的项目，若 `project_stage='11'`（项目结项），拒绝整次保存并提示项目已结项。
批量查询 `selectClosedProjectIds(projectIds)` 一次完成。

**Rationale**:

- 满足 FR-010（拒绝新增）与 FR-011（拒绝修改）：由于是"整天替换"语义，
  任何**出现在提交中**的已结项项目明细都意味着新增或修改，统一拒绝即可，无需区分两者
- 与 Decision 1 的作用范围形成互补：已结项项目**不在**可见集合中 → 前端不会提交它 →
  其既有工时被保留（FR-001）；若有人绕过前端直接提交 → 被本校验拒绝（FR-010/011）。
  两条规则各管一侧，不会相互抵消（spec Edge Cases 明确要求）
- 「结项」以 `project_stage` 为唯一判据，不扩大到 `approval_status` / `project_status`
  （spec Assumptions 已声明沿用现状，避免影响面扩大）

**Alternatives considered**: 用 `pm_project_stage_change` 判断"结项时间是否早于本次保存"——
无必要且不可靠：生产 104 个已结项项目中仅 41 个有阶段变更记录（39%）。当前状态就是唯一可信判据。

---

## Decision 5: 任务归属校验

**Decision**: 对 `subProjectId` 非空的明细，校验该任务的 `project_id` 等于明细声明的 `projectId`。
批量查询 `selectTaskProjectPairs(taskIds)` 得到任务→项目映射后在 Java 侧比对。

**Rationale**: 满足 FR-007。防止「项目 A 合法 + 任务属于项目 B」的组合绕过项目级校验。
生产实测 `sub_project_id` 指向已删除任务的明细为 **0 条**，无历史脏数据负担。

---

## Decision 6: 与工时汇总（rollup）的配合

**Decision**: 被保留的明细所属项目**一并纳入** `affectedProjectIds` 参与重算。

**Rationale**:

- 保留的明细仍在库中，而 rollup 口径是
  `SUM(pm_daily_report_detail.work_hours) WHERE project_id = 本项目`（Issue #5 已改为按明细全量汇总），
  因此保留项目的汇总值本就不会变化——纳入重算是幂等的
- 纳入的好处是防御性的：若并发场景下该项目的其他明细同时被改动，重算能保证一致性
- 满足 SC-008（不得引入新的对账偏差）

**Alternatives considered**: 不纳入（值未变，省一次 UPDATE）——省下的开销可忽略（单日报最多 6 个项目），
而一致性保障更重要。

---

## Decision 7: 非项目工时的识别

**Decision**: 以 `entry_type = 'work'` 判定「项目工时」，其余类型一律视为非项目工时，
不适用归属校验（FR-007），但同样受作用范围保护（FR-005，通过 `project_id is null` 条件覆盖）。

**Rationale**: `entry_type` 的取值域由字典 `sys_rbtype` 定义，共 8 项（均为启用状态）：

| dict_value | 含义 | 生产明细数 | project_id |
|---|---|---|---|
| `work` | 项目工时 | 15661 | 全部有值 |
| `leave` | 请事假 | 92 | 全部 NULL |
| `comp` | 倒休 | 911 | 全部 NULL |
| `annual` | 年假 | 128 | 全部 NULL |
| `marriage` | 婚假 | 40 | 全部 NULL |
| `maternity` | 产假 | 53 | 全部 NULL |
| `bereavement` | 丧假 | 9 | 全部 NULL |
| `8` | **出差途中** | 49 | 全部 NULL |

注意 `'8'` 是**合法字典值**（「出差途中」），只是取值风格与其余英文标识不一致，容易被误判为脏数据。
这正说明**不能靠枚举假期类型来识别非项目工时**——字典可随时新增取值，且取值风格无保证。
以 `entry_type='work'` 作正向判据、其余一律视为非项目工时，才不会因字典变动而失效。

**校验点**：`work` 类型的 `project_id` 100% 有值、其余 7 种 100% 为 NULL，
证明「`project_id IS NULL` ⟺ 非项目工时」这一等价关系在现存数据上成立，
可安全用作删除范围 SQL 的条件（Decision 2）。

---

## Decision 8: 拒绝的错误呈现与可追溯性

**Decision**: 校验失败时抛 `ServiceException`，消息包含被拒项目的**名称**（非 ID）与具体原因；
由 `GlobalExceptionHandler` 统一转为 `AjaxResult.error` 返回。可追溯性依托既有的
`@Log(title="日报管理", businessType=INSERT)` 注解——`sys_oper_log` 会记录失败状态、
操作人、完整请求参数与错误消息。

**Rationale**:

- 满足 FR-008（提示指明项目名称与原因）与 FR-012（拒绝事件可追溯）
- 符合宪法 III / 开发工作流（业务异常用 `ServiceException`，由全局处理器统一处理）
- 无需新增审计表：`sys_oper_log` 的既有记录已包含判定异常尝试所需的全部字段。
  本次历史丢失取证正是依靠该表的 `oper_param` 完成的，其证据能力已被实证
- 需一次查询取项目名称：可与 Decision 3/4 的批量查询合并（返回 id + name）

**Alternatives considered**: 新增专用的拒绝审计表——`sys_oper_log` 已够用，
新增表违反宪法「不必要的复杂度」倾向且无额外收益。

---

## Decision 9: 删除整条日报时的工时保护

**Decision**: `deleteDailyReportByIds` 同样按作用范围删除明细；若该日报仍有明细残留，
则**不删除主记录**，改为重算其 `total_work_hours`；仅当无残留时才软删主记录。

**Rationale**:

- 业务方 2026-07-30 明确：项目结项后虽不再显示在日报界面中，但此前已填的数据必须保留——
  不能因为填报人在日报上执行了删除操作，就把已结项项目的工时无辜删掉（FR-013）
- 由此产生一个**结构约束**：`pm_daily_report_detail` 通过 `report_id` 归属主记录，
  若主记录被软删而明细仍在，这些明细将无法通过任何既有查询到达
  （`selectByReportId` 之外的查询都经 `pm_daily_report r ... WHERE r.del_flag='0'` 过滤），
  等于换一种方式把工时弄丢。故必须保留主记录（FR-014）
- `pm_daily_report.total_work_hours` 是当日汇总，保留主记录时必须按剩余明细重算，
  否则主记录与明细不符（SC-010）

**实现要点**：删除流程改为
`收集受影响项目 → 按范围删明细 → 查剩余明细 → 有残留则重算 total_work_hours、无残留则软删主记录 → 重算项目实际人天`。
其中「收集受影响项目」必须在删除**之前**完成（既有逻辑已如此）。

**Alternatives considered**:

| 方案 | 否决原因 |
|---|---|
| 删除时不做保护（保持现状） | 违反 FR-013，业务方已明确否决 |
| 保护明细但仍软删主记录 | 明细成为无法到达的孤立数据，等于换个方式丢失（SC-010 不成立） |
| 保留主记录但不重算汇总工时 | 主记录汇总与明细之和不符，日历卡等展示会出错 |
| 把不可见明细迁移到一条新的"归档"日报 | 凭空增加数据结构与迁移逻辑，且改变了明细的日期归属，得不偿失 |

---

## 未决事项

无。所有 spec 中的 `[NEEDS CLARIFICATION]` 已在 specify 阶段消解，本轮未产生新的未知。

## 对既有代码的影响面

| 文件 | 改动性质 |
|---|---|
| `DailyReportServiceImpl.saveDailyReport()` | 核心逻辑：新增校验段 + 删除范围收窄 |
| `DailyReportServiceImpl.deleteDailyReportByIds()` | 按范围删明细；有残留则保主记录并重算汇总工时，无残留才软删主记录（Decision 9） |
| `DailyReportDetailMapper.java` / `.xml` | 新增 `deleteByReportIdInScope`、`countByReportId`（判断是否有残留）；给 `deleteByReportId` 加禁用说明 |
| `ProjectMemberMapper.java` / `.xml` | 新增 `selectEverMemberProjectIds` |
| `ProjectMapper.java` / `.xml` | 新增 `selectClosedProjectIdsIn`（或复用轻量查询返回 id+name+stage） |
| `TaskMapper.java` / `.xml` | 新增 `selectTaskProjectPairs` |
| `DailyReportMapper.java` / `.xml` | 新增 `updateTotalWorkHours`（保留主记录时重算当日汇总） |
| `DailyReportServiceImplTest` | 更新／新增单元测试（含保存与删除两条路径的防丢失回归） |
| `tests/e2e-*.spec.js` | 新增 e2e：防丢失、越权拒绝、结项拒绝、离场可维护 |

**不改动**：任何 Controller（无新增接口、无权限变更）、任何前端文件、任何数据库 schema。
