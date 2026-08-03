# Tasks: 日报保存的工时保护与项目归属校验

**Input**: Design documents from `/specs/015-daily-report-ownership-check/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, bdd/

**Tests**: **TDD 模式（用户明确要求）** —— 每个行为改动都先写失败测试、看着它以正确的理由失败，再写最小实现让它变绿。

**Organization**: 按 user story 分阶段，每个阶段可独立实现与验证。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件、无未完成依赖）
- **[Story]**: US1 / US2 / US3 / US4，对应 spec.md 的 user story

---

## 执行进度（2026-08-03 更新）

**已完成：Phase 1 – 7 —— User Story 1 / 2 / 3 / 4 的服务层逻辑与全部 SQL 均已落地**

| 项 | 状态 |
|---|---|
| 单测数量 | 33（基线）→ **44**，全绿 |
| 全模块单测 | **190 个全绿**，改动未波及其他服务 |
| 完成的红-绿循环 | **5 对**：保存范围收窄 / 删除范围收窄 / 主记录去留 / V1 曾是成员 / V2+V3 结项与任务归属 |
| 完成的回归护栏 | **6 个**：可见工时清零仍可删 / 无残留时正常删主记录 / 结项工时不提交则保留 / 离场成员可维护 / 假期跳过校验 / 既有 rollup 行为不变 |
| 变异测试 | ✅ 已验证「校验前置」这一最高风险项——把校验移到删除之后，测试立刻变红 |
| 已写 SQL | 7 条全部完成，接口 ↔ XML 一一对应校验通过 |

**Phase 8（e2e）也已完成** —— 用隔离 mysqld(3307) + redis(6380) 实例跑通：

| 项 | 结果 |
|---|---|
| e2e 用例 | **10 passed**（`tests/e2e-daily-report-ownership.spec.js`） |
| 对账 SC-008 | ✅ 无不符项目 |
| 对账 SC-010 | ✅ 孤立明细 0、主记录汇总全部相符 |
| 单测总数 | 33 → **46**（全模块 192 全绿） |

**e2e 抓出 2 个单测完全抓不到的缺陷（已修 + 已加回归）**：

1. **删除后返回 500 但数据已正确处理** —— 明细全被保留时无主记录可删，`rows=0`
   被 `toAjax` 判为失败，填报人会看到错误提示并重复点击。
   改为返回「本次处理的日报条数」。
2. **保存后当日汇总工时偏小** —— `totalWorkHours` 只累加提交内容，不含被保留的不可见明细
   （实测明细和 5.00、主记录写 3.00），违反 SC-010。改为保存后按剩余明细重算。

两者的共同点：单测验证了 Mapper 被正确调用，却没验证**语义后果**。凡是断言
「某个值没有改变」或「返回值的业务含义」的，单测通常无能为力。

**未做**：既有日报 e2e 套件的回归（`e2e-daily-report-*.spec.js`）—— 它们经 `/dev-api`
依赖前端 vite 代理、且依赖长期库的数据形态，隔离实例上跑不了。回归保障由全模块 192 个单测承担。

**待办**：提交前的安全审查（T050）。

**执行中发现的三处计划修正**（原计划有误，已按实际调整）：

1. **护栏顺序后移**：原计划 T009/T010 在改造前写、应立刻通过。但删除方法签名从
   `deleteByReportId(id)` 变为 `deleteByReportIdInScope(id, scope)`，而 Mockito 单测只能验证
   方法调用、验证不了最终状态——任何护栏都得等新签名就位才写得出来。护栏改到循环 1 之后写。
2. **T010 / T015 / T016 移交 e2e**：「假期未提交即删除」（靠 SQL 的 `project_id is null` 匹配）与
   「保留字段逐字不变」（靠"范围外明细压根没被 touch"）都是**数据库层行为**，单测把 Mapper 全 mock
   掉了，根本验证不了。已并入 Phase 8 的 e2e。
3. **被打破的既有测试只有 1 个，不是 2 个**：`saveDailyReport_update_oldDetailsAlsoRecomputed`
   并未断言 `deleteByReportId`，毫发无损。实际只需修 `saveDailyReport_update_whenExistingReport`。

**另外两项计划外的必要改动**：

- `DailyReportServiceImpl` 新增私有方法 `resolveVisibleProjectIds(userId)`——与 `selectMyProjects()`
  同口径但不做 hasSubProject 附加查询（界定范围只需项目ID）
- `ProjectMemberMapper` 的注入推迟到 Phase 5 真正需要时再加，避免提前引入用不上的依赖

**待办**：Phase 5（US2 归属校验）→ Phase 6（US3 结项拒绝）→ Phase 7（US4 离场成员）→ Phase 8（e2e + Polish）

---

## TDD 执行纪律（每个 🔴/🟢 任务对必读）

本特性的测试分两类，**处理方式不同，不可混为一谈**：

| 类型 | 标记 | 判据 | 若结果不符 |
|---|---|---|---|
| **红-绿循环** | 🔴 → 🟢 | 测试**必须先失败**，且失败原因是「功能缺失」而非拼写/编译错误 | 测试立刻通过 = 测错了东西，重写测试 |
| **回归保护** | 🛡️ | 测试**应当立刻通过**（描述的是现有正确行为，改造后不得被破坏） | 测试失败 = 已经踩坏了现状，立刻停下 |

**铁律**：🟢 任务不得在对应的 🔴 任务确认失败之前开始。跳过 Verify RED 等于没做 TDD。

**单测运行命令**（无需 MySQL/Redis）：

```bash
mvn test -pl ruoyi-project -am -Dtest=DailyReportServiceImplTest#<方法名>   # 单个方法
mvn test -pl ruoyi-project -am -Dtest=DailyReportServiceImplTest            # 整个类
```

**两个高风险项**（由 BDD 缺口 3、4 推演得出，实现时须始终成立）：

1. **全部校验必须前置于任何写操作** —— 否则会出现「校验失败了、但工时已经被删了」，比现状更糟
2. **FR-013 与 FR-014 必须成对实现** —— 只保留明细却软删主记录，明细将无法被任何业务查询到达，等于换一种方式把工时弄丢

---

## Phase 1: Setup

- [ ] T001 记录测试基线：运行 `mvn test -pl ruoyi-project -am -Dtest=DailyReportServiceImplTest` 并把 34 个方法的通过情况存档到 `/tmp/tdd-baseline.txt`，确认全绿后再动任何代码
- [ ] T002 在 `ruoyi-project/src/test/java/com/ruoyi/project/service/impl/DailyReportServiceImplTest.java` 顶部补充类级注释，说明本轮新增测试的两种类型（红-绿循环 / 回归保护）及其判据，供后续维护者区分

---

## Phase 2: Foundational（阻塞所有 user story）

**目的**：仅添加 Mapper **接口方法签名**，让后续测试能够编译并 mock。

> ⚠️ **此阶段不写任何 XML SQL**。接口签名是「wished-for API」的类型声明，不是业务逻辑；
> SQL 实现属生产代码，必须等到对应测试变红之后才写（见各 story 阶段）。

- [ ] T003 [P] 在 `ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportDetailMapper.java` 新增三个方法签名：`int deleteByReportIdInScope(@Param("reportId") Long reportId, @Param("visibleProjectIds") Collection<Long> visibleProjectIds)`、`int countByReportId(@Param("reportId") Long reportId)`、`BigDecimal sumWorkHoursByReportId(@Param("reportId") Long reportId)`，每个方法带 javadoc 说明语义
- [ ] T004 [P] 在 `ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportMapper.java` 新增 `int updateTotalWorkHours(@Param("reportId") Long reportId, @Param("hours") BigDecimal hours)`，javadoc 注明「SQL 须带 `update_time = update_time` 以免触碰审计字段」
- [ ] T005 [P] 在 `ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMemberMapper.java` 新增 `List<Long> selectEverMemberProjectIds(@Param("userId") Long userId, @Param("projectIds") Collection<Long> projectIds)`，javadoc 必须写明「**不限** `is_active` / `del_flag` —— 离场成员仍是『曾参与』的凭据（FR-006 / US4）」
- [ ] T006 [P] 在 `ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java` 新增 `List<Map<String, Object>> selectProjectStatesIn(@Param("projectIds") Collection<Long> projectIds)`，返回 `projectId` / `projectName` / `projectStage` 三列
- [ ] T007 [P] 在 `ruoyi-project/src/main/java/com/ruoyi/project/mapper/TaskMapper.java` 新增 `List<Map<String, Object>> selectTaskProjectPairs(@Param("taskIds") Collection<Long> taskIds)`，返回 `taskId` / `projectId` 两列
- [ ] T008 运行 `mvn clean compile -pl ruoyi-project -am` 确认五个 Mapper 接口改动编译通过（此时无对应 XML，编译期不校验，属预期）

**Checkpoint**: 接口签名就位，测试可以 mock 这些方法了。

---

## Phase 3: User Story 1a — 保存日报时不丢失不可见工时 (P1) 🎯 MVP

**Goal**: 保存日报时，作用范围外的既有明细原样保留。

**Independent Test**: 构造某天含「可见项目 + 不可见项目」两条工时，仅提交可见项目 → 不可见项目的工时仍在，其项目实际人天不变。

### 🛡️ 先立回归护栏（这些测试应当立刻通过）

- [ ] T009 [P] [US1] 在 `DailyReportServiceImplTest.java` 新增 🛡️ `saveDailyReport_visibleProjectClearedToZero_isDeleted()`：填报人把可见项目工时清零并保存，断言该明细被删除（INV-4 / FR-002 / spec US1 场景 3）。**运行后应当立刻通过**——它锁定的是现有正确行为
- [ ] T010 [P] [US1] 新增 🛡️ `saveDailyReport_leaveEntryNotSubmitted_isDeleted()`：假期记录未出现在提交中时被删除（FR-005 修正后的措辞——假期在所有入口均可见，未提交即视为主动删除）。**应当立刻通过**

### 🔴 → 🟢 循环 1：作用范围内删除（核心改造点）

- [ ] T011 [US1] 🔴 新增失败测试 `saveDailyReport_invisibleProjectHours_arePreserved()`：mock `selectMyProjects` 只返回项目 A，该日既有明细含项目 A 4h 与项目 B（不可见）2h，提交仅含 A 6h。断言 `detailMapper.deleteByReportIdInScope(reportId, 含A不含B的集合)` 被调用，且 **`deleteByReportId` 从未被调用**。运行 `mvn test -pl ruoyi-project -am -Dtest=DailyReportServiceImplTest#saveDailyReport_invisibleProjectHours_arePreserved`，**确认失败且失败原因是「deleteByReportId 仍被调用」而非编译错误**
- [ ] T012 [US1] 🟢 在 `DailyReportServiceImpl.java` 第 209 行处，把 `detailMapper.deleteByReportId(existingReportId)` 替换为先计算 `visibleProjectIds`（从既有 `selectMyProjects()` 提取 projectId 集合）再调用 `deleteByReportIdInScope(existingReportId, visibleProjectIds)`。写最小实现，重跑 T011 的测试确认变绿

### 🔴 → 🟢 循环 2：修复被签名变更打破的既有测试

- [ ] T013 [US1] 运行整类测试 `mvn test -pl ruoyi-project -am -Dtest=DailyReportServiceImplTest`，定位因 `deleteByReportId` → `deleteByReportIdInScope` 而失败的既有测试（预期为 `saveDailyReport_update_whenExistingReport` 第 158 行与 `saveDailyReport_update_oldDetailsAlsoRecomputed` 第 358 行），把失败清单与 T001 的基线对比确认无其他连带破坏
- [ ] T014 [US1] 更新 T013 中定位到的既有测试：改为断言 `deleteByReportIdInScope` 被调用（而非删除全部），并为其 mock `selectMyProjects` 返回值。重跑整类测试确认全绿

### 🔴 → 🟢 循环 3：保留字段逐字不变

- [ ] T015 [US1] 🔴 新增失败测试 `saveDailyReport_preservedDetail_fieldsUnchanged()`：验证被保留明细的 `workHours` / `workContent` / `subProjectId` / `workCategory` 在保存前后完全一致（INV-3 / FR-003）。确认其**先失败**
- [ ] T016 [US1] 🟢 让 T015 变绿（若循环 1 的实现已天然满足——因为范围外明细根本没被 touch——则记录「实现已覆盖」，但仍须先看到 T015 失败才算数）

### SQL 实现与真实验证

- [ ] T017 [US1] 在 `ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml` 实现 `deleteByReportIdInScope`：`delete from pm_daily_report_detail where report_id = #{reportId} and (project_id is null <if test="visibleProjectIds != null and visibleProjectIds.size() > 0"> or project_id in <foreach .../></if>)`。**必须带 `<if>` 空集守卫**——集合为空时退化为仅删非项目工时，绝不能生成 `in ()` 语法错误
- [ ] T018 [US1] 在 `tests/e2e-daily-report-ownership.spec.js` 新建 e2e 用例「保存时不可见工时被保留」：造数（某天含在建项目 + 已结项项目工时）→ 调保存接口只提交在建项目 → 查库断言已结项项目工时仍在且项目实际人天未变（SC-001）

**Checkpoint**: MVP 达成——正在发生的数据丢失被止住（保存路径）。此时可独立交付。

---

## Phase 4: User Story 1b — 删除整条日报时不丢失不可见工时 (P1)

**Goal**: 删除日报时同样按作用范围删除；有残留则保留主记录并重算汇总工时。

**Independent Test**: 某天含「可见 4h + 不可见 2h」，删除整条日报 → 不可见的 2h 仍在，主记录保留且汇总工时为 2h。

> ⚠️ **FR-013 与 FR-014 必须成对实现**：只做 T020（保留明细）而不做 T022（保留主记录），
> 被保留的明细将无法通过任何业务查询到达——等于换一种方式把工时弄丢。

### 🛡️ 回归护栏

- [ ] T019 [P] [US1] 新增 🛡️ `deleteDailyReport_allVisible_behaviorUnchanged()`：该日报全部明细均在可见范围内时，行为与改造前完全一致（全部删除 + 软删主记录）。**应当立刻通过**（INV-D5 / spec US1 场景 6）

### 🔴 → 🟢 循环 4：按范围删除明细

- [ ] T020 [US1] 🔴 新增失败测试 `deleteDailyReport_invisibleHours_arePreserved()`：断言调用的是 `deleteByReportIdInScope` 而非 `deleteByReportIds`。运行确认**失败原因是仍在调用 `deleteByReportIds`**
- [ ] T021 [US1] 🟢 在 `DailyReportServiceImpl.java` 第 311 行处，把 `detailMapper.deleteByReportIds(reportIds)` 改为对每个 reportId 调用 `deleteByReportIdInScope(reportId, visibleProjectIds)`。重跑确认变绿

### 🔴 → 🟢 循环 5：主记录去留与汇总重算（不可与循环 4 分开交付）

- [ ] T022 [US1] 🔴 新增失败测试 `deleteDailyReport_withRemainingDetails_keepsMasterRecord()`：删除后 `countByReportId` 返回 > 0 时，断言 **主记录未被软删**、且 `updateTotalWorkHours(reportId, 剩余work工时和)` 被调用（FR-014 / INV-D1 / INV-D2）。确认**先失败**
- [ ] T023 [US1] 🔴 新增失败测试 `deleteDailyReport_noRemainingDetails_deletesMasterRecord()`：`countByReportId` 返回 0 时主记录被软删（既有行为的边界侧）。确认**先失败**（因为此时删除路径尚未引入 count 判断，主记录被无条件软删——该测试断言的是「经过判断后才删」）
- [ ] T024 [US1] 🟢 在 `deleteDailyReportByIds` 中实现残留判断：删明细后调 `countByReportId`，>0 则跳过主记录软删并调 `sumWorkHoursByReportId` + `updateTotalWorkHours`，=0 则走既有软删。重跑 T022/T023 确认双绿

### SQL 实现与真实验证

- [ ] T025 [P] [US1] 在 `DailyReportDetailMapper.xml` 实现 `countByReportId` 与 `sumWorkHoursByReportId`；后者口径须为 `SUM(work_hours) WHERE report_id=? AND entry_type='work' AND del_flag='0'`（与 saveDailyReport 计算 totalWorkHours 的口径一致，只累加 work 类型）
- [ ] T026 [P] [US1] 在 `ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml` 实现 `updateTotalWorkHours`，**必须带 `update_time = update_time`** 以免触碰审计字段（宪法 IV）
- [ ] T027 [US1] 在 `tests/e2e-daily-report-ownership.spec.js` 新增 e2e 用例「删除时不可见工时被保留」：删除含已结项工时的整条日报 → 断言已结项工时仍在、主记录仍在、主记录汇总工时等于剩余明细之和（SC-009 / SC-010）

**Checkpoint**: US1 完整达成——保存与删除两条路径都不再丢失不可见工时。

---

## Phase 5: User Story 2 — 阻止把工时填到无关项目上 (P1)

**Goal**: 校验每条项目工时的项目归属，不通过则拒绝整次保存。

**Independent Test**: 用从未参与的项目提交 → 被拒且该项目实际人天不变；用正常参与的项目提交 → 成功。

> ⚠️ **校验必须前置于任何写操作**（BDD 缺口 3）。T030 的测试专门守这条。

### 🔴 → 🟢 循环 6：V1「曾是成员」校验

- [ ] T028 [US2] 🔴 新增失败测试 `saveDailyReport_neverMemberProject_isRejected()`：mock `selectEverMemberProjectIds` 返回空集，断言抛出 `ServiceException` **且异常消息包含项目名称**（FR-008），同时断言 `batchInsert` 与任何 delete 方法**均未被调用**（FR-009）。确认**先失败**（当前无任何校验，会正常保存）
- [ ] T029 [US2] 🟢 在 `saveDailyReport` 的白名单校验之后、任何写操作之前，插入校验段：提取 `entryType='work'` 且 `projectId != null` 的明细 → 取 projectId 去重集 → 调 `selectProjectStatesIn` 与 `selectEverMemberProjectIds` → 非成员项目抛 `ServiceException("项目《" + name + "》不在您参与的项目范围内")`。重跑确认变绿

### 🔴 → 🟢 循环 7：拒绝路径不得触发任何写入（最高风险项）

- [ ] T030 [US2] 🔴 新增失败测试 `saveDailyReport_rejected_invisibleHoursStillExist()`：一次提交同时含「不可见的已有合法工时」与「无关项目的非法工时」，断言拒绝后 **`deleteByReportIdInScope` 未被调用**、`updateActualWorkload` 未被调用（spec US1 场景 7 / INV-1 / BDD 缺口 3）。若 T029 已把校验放在写操作之前，此测试应当通过——**但仍须先运行确认它能捕捉到「校验后置」这一错误**：临时把校验段移到删除之后，看测试变红，再移回来看它变绿
- [ ] T031 [US2] 🟢 确认校验段位置正确（在 `selectMyProjects` 与所有 delete/insert 之前），T030 稳定通过

### 🔴 → 🟢 循环 8：V3 任务归属校验

- [ ] T032 [US2] 🔴 新增失败测试 `saveDailyReport_taskNotBelongingToProject_isRejected()`：明细的 `subProjectId` 指向的任务其 `projectId` 与明细声明的不一致 → 拒绝（FR-007）。确认**先失败**
- [ ] T033 [US2] 🟢 在校验段中加入 V3：取非空 `subProjectId` 去重集 → 调 `selectTaskProjectPairs` → 映射缺失或不匹配则抛 `ServiceException("任务与所选项目不匹配")`。重跑确认变绿

### 🛡️ 回归护栏

- [ ] T034 [P] [US2] 新增 🛡️ `saveDailyReport_leaveEntries_skipOwnershipCheck()`：请假/倒休/年假记录（`projectId` 为 null）不触发归属校验（FR-005 / spec US2 场景 4）。**应当立刻通过**

### SQL 实现与真实验证

- [ ] T035 [P] [US2] 在 `ruoyi-project/src/main/resources/mapper/project/ProjectMemberMapper.xml` 实现 `selectEverMemberProjectIds`——**不带** `is_active` / `del_flag` 过滤（这是 US4 成立的关键），带 `<foreach>` 与空集守卫
- [ ] T036 [P] [US2] 在 `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml` 实现 `selectProjectStatesIn`；在 `ruoyi-project/src/main/resources/mapper/project/TaskMapper.xml` 实现 `selectTaskProjectPairs`。两者均为 PM 表内查询，无需 `COLLATE`
- [ ] T037 [US2] 在 `tests/e2e-daily-report-ownership.spec.js` 新增 e2e 用例「跨项目提交被拒绝」：用 admin 之外的账号提交一个从未参与项目的工时 → 断言返回 `code:500`、消息含项目名、目标项目实际人天未变（SC-002）
- [ ] T038 [US2] 验证 FR-012 可追溯性：执行一次会被拒绝的保存后，查询 `sys_oper_log` 确认存在含操作人、完整请求参数、失败状态与拒绝原因的记录（依托既有 `@Log` 注解，无需新增代码；本任务是验证而非实现）

**Checkpoint**: 越权写入缺口关闭。US1 + US2 共同构成完整防线。

---

## Phase 6: User Story 3 — 结项后不再接受新增或变更工时 (P2)

**Goal**: 已结项项目（`project_stage='11'`）拒绝新增与修改工时。

**Independent Test**: 为已结项项目新增工时 → 拒绝；仅修改同一天在建项目工时 → 成功且已结项工时原样保留。

> 本 story 复用 US2 已建立的校验框架与 `selectProjectStatesIn` 查询，**依赖 Phase 5 完成**。

### 🔴 → 🟢 循环 9：V2 结项校验

- [ ] T039 [US3] 🔴 新增失败测试 `saveDailyReport_closedProject_isRejected()`：mock `selectProjectStatesIn` 返回 `projectStage='11'`，断言抛出 `ServiceException` 且消息为「项目《XXX》已结项，不能新增或修改其工时」（FR-010 / FR-011）。确认**先失败**
- [ ] T040 [US3] 🟢 在校验段中加入 V2：`selectProjectStatesIn` 结果中 `projectStage='11'` 的项目直接拒绝。重跑确认变绿

### 🛡️ 关键交叉护栏（「保护」与「拒绝」不得互相抵消）

- [ ] T041 [US3] 🛡️ 新增 `saveDailyReport_closedProjectNotSubmitted_isPreservedNotRejected()`：同一天含已结项项目工时，提交内容**不含**它、仅改在建项目 → **保存成功**（不因该日存在已结项工时而被拒），且已结项工时原样保留（spec US3 场景 3）。这条同时守着 US1 与 US3 的边界，**必须通过**——若失败说明校验误把「既有的」当成了「新增的」
- [ ] T042 [US3] 在 `tests/e2e-daily-report-ownership.spec.js` 新增 e2e 用例「结项项目拒绝 + 既有工时保留」：覆盖 T039 与 T041 两侧行为，断言 SC-003（结项后实际人天不再因任何保存操作变化）

**Checkpoint**: 「结项后不能填日报」这条既有业务规则从「靠前端不展示」升级为服务端强制。

---

## Phase 7: User Story 4 — 离场成员的历史日报仍可维护 (P3)

**Goal**: 曾参与但已离场的成员，仍可维护其历史工时。

**Independent Test**: 「曾是成员但已离场」的在建项目 → 允许；「从未参与」的项目 → 拒绝。两者结果必须不同。

> 本 story 不新增逻辑——它验证的是 T035 中「`selectEverMemberProjectIds` 不过滤 `is_active`」这一判据选择是否正确。

- [ ] T043 [US4] 🛡️ 新增 `saveDailyReport_formerMember_canMaintainHistory()`：mock `selectEverMemberProjectIds` 对已离场成员仍返回该项目 id → 保存成功（FR-006 / SC-007）。**若失败说明 T035 的 SQL 误加了 `is_active` 过滤**
- [ ] T044 [US4] 在 `tests/e2e-daily-report-ownership.spec.js` 新增 e2e 用例「离场成员可维护 vs 从未参与被拒」：用真实数据构造两个对照项目，断言两者结果不同——这是 research.md Decision 3 的实证（用现役成员判据会误拒 126 组历史组合）

**Checkpoint**: 全部四个 user story 完成。

---

## Phase 8: Polish & 跨切面验证

- [ ] T045 运行完整单测 `mvn test -pl ruoyi-project -am` 确认全绿，并与 T001 的基线对比——原有 34 个方法必须**一个不少地仍然通过**
- [ ] T046 运行完整 e2e：`npx playwright test e2e-daily-report-ownership.spec.js`（需先启动后端 8085 与前端，并**临时关闭验证码**，跑完恢复）
- [ ] T047 跑既有日报相关 e2e 回归：`npx playwright test e2e-daily-report-*.spec.js e2e-mobile-daily-report.spec.js`，确认桌面与移动端两个入口的正常填报流程零感知（SC-005 / FR-004）
- [ ] T048 对账验证 SC-008 与 SC-010：在本地库执行 SQL，确认「所有项目 `actual_workload` 严格等于其明细汇总」且「不存在孤立明细、不存在主记录汇总与明细不符」，结果记录到 `specs/015-daily-report-ownership-check/quickstart.md`
- [ ] T049 更新 `specs/015-daily-report-ownership-check/bdd/coverage.md`，把每个 BDD 场景映射到实际实现它的测试方法名（feature 文件里的 28 个场景 → 单测方法 / e2e 用例），形成可追溯的双向映射
- [ ] T050 提交前安全审查（用户全局规范要求）：执行 `/security-review` 检查本次改动，重点核对越权校验是否可绕过、SQL 注入（`<foreach>` 参数化）、错误消息是否泄露内部标识（FR-008 要求只暴露项目名不暴露 ID）

---

## Dependencies

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational: Mapper 接口签名) ← 阻塞所有 story
    ↓
    ├─→ Phase 3 (US1a 保存防丢失) ────┐   🎯 MVP，可独立交付
    │        ↓                        │
    │   Phase 4 (US1b 删除防丢失)      │   依赖 Phase 3 的 deleteByReportIdInScope
    │                                 │
    └─→ Phase 5 (US2 归属校验) ───────┤   与 Phase 3/4 逻辑独立，但共享同一方法体
             ↓                        │
        Phase 6 (US3 结项拒绝)         │   依赖 Phase 5 的校验框架与 selectProjectStatesIn
             ↓                        │
        Phase 7 (US4 离场成员)         │   验证 Phase 5 的判据选择，不新增逻辑
                                      ↓
                              Phase 8 (Polish)
```

**依赖说明（如实标注，不假装完全独立）**：

- **US1a → US1b**：删除路径复用保存路径建立的 `deleteByReportIdInScope`，必须先做 US1a
- **US2 → US3**：US3 只是在 US2 的校验段里多加一个判据（`projectStage='11'`），共用 `selectProjectStatesIn` 查询
- **US2 → US4**：US4 不新增代码，它验证的是 US2 中 SQL 判据的正确性
- **US1 与 US2 的交叉点**：T030（拒绝时不可见工时仍在）同时依赖两者——US2 提供拒绝、US1 提供保护，必须在两者都完成后验证

---

## Parallel Execution Opportunities

| 阶段 | 可并行任务 | 说明 |
|---|---|---|
| Phase 2 | T003–T007 | 五个不同的 Mapper 接口文件，互不影响 |
| Phase 3 | T009, T010 | 两个回归护栏测试，同文件但互不依赖，可连续写完一起跑 |
| Phase 4 | T025, T026 | 两个不同的 XML 文件 |
| Phase 5 | T035, T036 | 三个不同的 XML 文件 |

**不可并行的关键路径**：所有 🔴 → 🟢 任务对必须严格串行（红在前、绿在后），且不同循环之间也应串行——同时开两个红灯会让你分不清哪个实现让哪个测试变绿。

---

## Implementation Strategy

### MVP 范围建议：Phase 1 → 2 → 3

**只做 US1a（保存路径防丢失）就已经是可交付的价值**：它止住了**正在发生**的数据丢失——生产现存 133 条高风险日报，每一条被再次编辑时都会丢失其中已结项项目的工时。这部分改动面最小（一个方法调用 + 一条 SQL），风险最低。

### 增量交付顺序

1. **第一批（止血）**：Phase 3 —— 保存路径防丢失
2. **第二批（补全）**：Phase 4 —— 删除路径防丢失（与第一批同属 US1，但可分开验证与上线）
3. **第三批（防越权）**：Phase 5 + 6 + 7 —— 归属校验全套
4. **第四批（收尾）**：Phase 8

每批之间都有 Checkpoint，可独立跑测试、独立上线。

### 已知的验收折中

删除后若仍有不可见工时被保留，填报人重新打开那天**看到的仍是空白**，但**日历卡上该日仍会显示汇总工时**。这是已记录在 `contracts/delete-daily-report.md` 的可接受折中——相比静默丢失历史工时，宁可让填报人看到一个暂时无法解释的数字。彻底消除需走「已结项项目以只读行呈现」的体验增强路线，不在本特性范围。

---

## Task Summary

| 阶段 | 任务数 | 其中 🔴→🟢 循环 | 其中 🛡️ 回归护栏 |
|---|---|---|---|
| Phase 1 Setup | 2 | — | — |
| Phase 2 Foundational | 6 | — | — |
| Phase 3 US1a 保存防丢失 | 10 | 3 对 | 2 |
| Phase 4 US1b 删除防丢失 | 9 | 3 对 | 1 |
| Phase 5 US2 归属校验 | 11 | 3 对 | 1 |
| Phase 6 US3 结项拒绝 | 4 | 1 对 | 1 |
| Phase 7 US4 离场成员 | 2 | — | 1 |
| Phase 8 Polish | 6 | — | — |
| **合计** | **50** | **10 对** | **7** |

**测试产出**：单测新增约 17 个方法（10 个红-绿循环 + 7 个回归护栏），e2e 新增 1 个文件 6 个用例，既有单测更新 2 个。

**独立验证标准**：

- **US1**：某天含可见+不可见工时，保存与删除各验一次，不可见部分都保留
- **US2**：从未参与的项目被拒 + 正常项目可存，两者结果不同
- **US3**：已结项项目拒绝新增，但既有工时不因此被拒绝保存
- **US4**：离场成员可维护 vs 从未参与被拒，两者结果不同
