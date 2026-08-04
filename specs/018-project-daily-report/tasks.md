# Tasks: 项目日报的人员可见性与口径自证

**特性**: `018-project-daily-report` | **日期**: 2026-08-03
**输入**: [spec.md](./spec.md) · [plan.md](./plan.md) · [data-model.md](./data-model.md) · [bdd/coverage.md](./bdd/coverage.md)

## 执行进度（2026-08-03）

| 阶段 | 状态 | 证据 |
|---|---|---|
| Phase 1 基线 | ✅ | 后端 **193 passed / 0 failed**；离场对数 月窗 1 / 全周期 11；SC-003 守护值 **0**；深圳组 `dept_id=216` 叶子确认 |
| Phase 2 VO + 脚手架 | ✅ | `mvn compile` 通过 |
| Phase 3 US2（D2 缺陷 + 月窗条件化） | ✅ | 🔴 实测失败输出 `expected: <0> but was: <1>`（166.compareTo(0)=1，totalHours 被吞成 0）→ 🟢 50 passed |
| Phase 4 US1（离场放宽） | ✅ | 指定 2026-07 原始行数 4642→**4652**（+10 = 离场对数 1→11）；project 38 曲君正确出现且日历为空 |
| Phase 5 US3（三组新字段） | ✅ | 🔴 3 条「expected X but was null」→ 🟢 54 passed；三个新 JOIN 后行数仍 **4652**（零扇出） |
| Phase 6 文案导航 | ✅ | `grep 实际人天` 前端与 VO 均零命中 |
| Phase 7 E2E | ⚠️ 部分 | API 层 **8/8 passed**；安全回归 **9/9 passed**；UI 静态 **3/3 passed**；**UI 布局交互用例 skipped（见下）** |
| Phase 8 收尾 | ✅ | 全量回归 **200 passed / 0 failed**（基线 193）；`npm run build:prod` 成功 |

### ⚠️ 未完成项（诚实记录，勿当已验证）

1. **R-003 不填年月时的布局观感未实拍**。`tests/e2e-project-daily-report-ui.spec.js` 的「不填年月查询」用例
   在部门选择环节 **skipped**：`ProjectDeptSelect` 是 `el-tree-select` 且 `filterable` 默认 false
   （组件 `index.vue:45-48`），只能逐层展开；实跑时下拉容器存在但树节点数为 0，数据未加载成功。
   已有的旁证：① `spanMethod` 在 N=0 时的列索引算术由源码推演确认恒等；② 前端构建通过；
   ③ 3 条 UI 静态断言（标签/图例/无旧名）在真实页面上通过，说明页面渲染正常。
   **仍缺的是「5 列全 fixed、中间 0 可滚动列」的视觉确认——需人工打开页面看一眼。**
2. **非 admin 数据范围下的行为未验证**（R-006 / OUT-001）。本地 admin 是「全部」范围，
   `DataScopeAspect.java:114-118` 直接清空 sqlString，所有验证都绕开了该路径。
3. **生产数据分布未核查**。全部计数来自本地 docker 库 2026-08-03 快照。

### Code Review 结果（多视角对抗审查，5 视角初审 + 每条 2 个证伪者）

初审 8 条 → 证伪后 **1 条成立、10 条被推翻**（多条因审查期间已被并发修复，证伪者看到的是修好的代码）。

| 发现 | 裁决 | 处置 |
|---|---|---|
| 全周期模式下所有列定宽，表格塌成 706px，右侧留白但边框画到容器右缘 | ✅ **成立**（证伪者用 Playwright + EP 2.13.1 实跑复现） | 已修：项目列 `width` → `min-width`，列数不变故 spanMethod 索引不受影响 |
| `dayColumns` 绑实时 `queryParams.yearMonth`，清空年月未点查询就翻转表格形态 | 推翻（已修） | 已引入 `queriedYearMonth`，数据到手后再切换列形态 |
| `@click.prevent` 无条件拦截，Cmd/Ctrl+左键开不了新标签 | 推翻（已修） | 已改 `onProjectClick`，只接管普通左键 |
| 未选部门告警文案仍是「项目部门」 | 推翻（已修） | 已改为「项目所属部门」 |
| mapper 注释漏改一处「实际人天」 | 推翻（已修） | 已改（`:581`） |
| `coverage.md` 映射到不存在的测试 | 部分成立 | 已重写覆盖统计为实测值，并标注 2 个未验证场景 |
| e2e 防御性 skip 会吞掉 FR-004 回归 | 部分成立 | 已把 `test.skip(!former)` 改为 `expect(former).toBeTruthy()` |
| `node_modules` 软链会被提交 | 成立（已知项） | 已删；最终 `git status` 只剩本特性文件 |
| SQL 正确性 / Java 聚合两个视角 | **0 条发现** | — |

**并发修改导致的方法论问题**：审查期间我在同一 worktree 上继续修代码，导致证伪者读到的版本与初审者不同。
下次应在 review 期间冻结代码，或把 review 对准一个固定的 commit。

### 一个必须记住的踩坑

**e2e 一度出现「5 个用例回退失败」，根因是 8080 端口上跑的是旧 jar** —— 旧版 `daily_hrs` 始终含
`report_date`，故 ORDER BY 不报错，传 null 时静默降级，症状与「代码没改对」完全一致。
教训：**验证前必须确认运行的是哪个构建产物**（比对 jar 时间戳 / 杀干净端口再起），
否则会把环境问题误判成代码缺陷，反向修改正确的实现。

### 已知偏离 tasks 原计划之处

- **T009 被替换**：原计划「项目无成员时保持占位行」在 Service 层测不到 —— SQL 用的是
  `INNER JOIN m`，无成员的项目根本不会出现在原始行里，占位行是**前端** `flatRows` 的逻辑
  （`teamReport.vue` members.length===0 分支）。已改为
  `selectTeamMonthly_multipleMembers_aggregatedUnderSameProject`（同项目多成员不串行），
  这是聚合层真正需要护栏的地方。
- **发现并修了一处漏改**：`ORDER BY daily_hrs.report_date`（`DailyReportMapper.xml:689`）。
  手工展开 SQL 验证时我只写了 `SELECT COUNT(*)`，没带 ORDER BY，因此该处漏改被验证方法本身绕过，
  由 e2e 第一次调用报 `Unknown column 'daily_hrs.report_date' in 'order clause'` 抓出。
  **教训：用 COUNT(*) 展开验证 SQL，测不到 ORDER BY 与 SELECT 列表的问题。**

---

## Format: `[ID] [P?] [Story] Description`

- `[P]` = 可与同批次其他 `[P]` 任务并行（不同文件、无依赖）
- `[Story]` = 所属 User Story（US1~US5）
- 🔴 = 必须先跑出**真实失败**的测试；🟢 = 让 🔴 变绿的最小实现；🛡️ = 回归护栏（写完应立即通过）

---

## ⚠️ TDD 执行纪律（每个 🔴/🟢 任务对必读）

1. **「先红」是字面要求**：🔴 任务必须真的运行测试命令、真的看到失败输出，并**确认失败原因与预期一致**（不是编译错误、不是 mock 配置错）。没跑过红的测试不算 TDD。
2. **失败输出要留证**：把关键失败信息记录下来，🟢 完成后对照确认是同一个测试变绿。
3. **一次只推一个循环**：🔴 未见红就不写实现；🟢 未变绿就不进下一个循环。
4. **🛡️ 任务写完立刻跑**，若它没有立即通过，说明你对现有行为的理解是错的 —— 停下来查清楚，不要改实现去迁就测试。

### 🚨 方法论要点：Mockito 单测测不到 SQL

`DailyReportServiceImplTest` 是**纯单元测试**，`dailyReportMapper` 是 `@Mock` —— 它验证的是 **Java 聚合层**（`selectTeamMonthly` 如何把原始行组装成 VO 树），**不会执行任何 SQL**。

因此本特性的验证分两条线，**不可互相替代**：

| 改动 | 验证手段 | 说明 |
|---|---|---|
| Java 聚合（D2 缺陷、字段赋值） | ✅ JUnit + Mockito | mock 出各种形态的原始行，断言 VO 树 |
| SQL 形态（月窗条件化、三个新 JOIN、CASE 反推） | ❌ JUnit 测不到<br>✅ 真实 SQL 执行 + Playwright API | 必须连真实库跑，或打真实接口断言返回 |

**别被绿色的单测骗了** —— 单测全绿只证明「给定这样的原始行，聚合逻辑正确」，不证明「SQL 真的会产出这样的原始行」。

---

## Phase 1: Setup

- [ ] **T001** 记录测试基线：在 worktree 内运行 `mvn test -pl ruoyi-project -am`，把总数与通过数存档到 `/tmp/018-baseline.txt`。**确认全绿后再动任何代码**；若基线本身有红，先查清楚是否与 018 相关，红着的基线不能作为对比参照。
- [ ] **T002** 记录前端构建基线：`cd ruoyi-ui && npm run build:prod`，确认能成功产出 `dist/`。另跑一次 `npx vue-tsc --noEmit -p tsconfig.json 2>&1 | grep -c "error TS"` 记下基线错误数（勘察实测为 **39**），存档到同一文件。**Lint 判据是该数字不上升，不是归零**（R-005）。
- [ ] **T003** 记录数据基线：连本地库执行并存档三个数字 —— ① 2026-07 月窗下 `selectTeamMonthlyRaw` 原始行数（预期 4642 量级）；② 全周期离场且填过本项目日报的 `(project,user)` 对数（预期 11）；③ 「写过 work 日报但成员表无行」的对数（预期 **0**，SC-003 的守护值）。

---

## Phase 2: Foundational（阻塞所有 User Story）

- [ ] **T004** 在 `DailyReportServiceImplTest.java` 中新建 `teamMonthly` 测试区块：加类级分隔注释说明「本区块覆盖 `selectTeamMonthly` 的 Java 聚合层，SQL 形态由 e2e 验证」，并抽出一个构造原始行 Map 的 helper（`Map<String,Object> rawRow(...)`），供后续所有测试复用。
- [ ] **T005 [P]** 在 `TeamMemberDailyVO.java` 新增五个字段 + getter/setter：`roleLabel`(String)、`firstReportDate`(String)、`lastReportDate`(String)、`joinDate`(String)、`leaveDate`(String)。同时把 `isFormer` 的注释去掉「本月」限定（现写「**本月**填报了工时」，放宽月窗后不准确）。
- [ ] **T006 [P]** 在 `TeamDailyReportVO.java` 新增 `projectDeptName`(String) + getter/setter，并把 `:24` 的注释 `实际人天` 改为 `项目累计人天`（公式保留）。
- [ ] **T007** 运行 `mvn clean compile -pl ruoyi-project -am` 确认 VO 改动编译通过。

---

## Phase 3: US2 — 不填年月的聚合正确性 (P1) 🎯 头号目标

> 先做 US2 而不是 US1，因为 D2 缺陷是本特性**唯一会静默出错**的地方，且它挡着后面所有无年月场景的验证。

### 🛡️ 先立回归护栏

- [ ] **T008 [P] [US2]** 🛡️ 新增 `selectTeamMonthly_withYearMonth_dailyHoursAndTotalPreserved()`：mock 返回 3 行（同一 project+user，reportDate 分别为三天，各 8/6/4 小时），断言 `dailyHours` 有 3 个 key 且 `totalHours = 18`。**运行后应立即通过** —— 它锁定的是现有正确行为（coverage 2.5）。
- [ ] **T009 [P] [US2]** 🛡️ 新增 `selectTeamMonthly_projectWithNoMembers_keepsPlaceholderRow()`：mock 返回项目行但 userId 为 null 的情形，断言项目仍出现在结果中。**应立即通过**（coverage 1.5）。

### 🔴 → 🟢 循环 1：无年月时个人人天不得被吞（D2）

- [ ] **T010 [US2]** 🔴 新增失败测试 `selectTeamMonthly_noYearMonth_totalHoursAccumulatedWithoutDate()`：mock `selectTeamMonthlyRaw` 返回**单行**，其中 `reportDate = null`、`totalWorkHours = 166`。断言该成员的 `totalHours` 等于 `166` 且 `dailyHours` 为空 Map。
  运行 `mvn test -pl ruoyi-project -am -Dtest=DailyReportServiceImplTest#selectTeamMonthly_noYearMonth_totalHoursAccumulatedWithoutDate`，**确认失败，且失败原因是 `totalHours` 为 0（期望 166）**，不是 NPE、不是编译错误。把失败输出记入 `/tmp/018-red-1.txt`。
- [ ] **T011 [US2]** 🟢 修改 `DailyReportServiceImpl.java:786-796`：把守卫从 `if (reportDate != null && totalWorkHours != null)` 拆成外层 `if (totalWorkHours != null)` 负责累加 `totalHours`、内层 `if (reportDate != null)` 负责填 `dailyHours`（plan.md D2 给出完整代码）。重跑 T010 确认变绿，再跑 T008 确认没打破按月形态。

### 🔴 → 🟢 循环 2：SQL 月窗条件化

> ⚠️ 本循环的「红」不在 JUnit —— 它是 SQL 改动。红的判据是**真实 SQL 执行结果**。

- [ ] **T012 [US2]** 🔴 在本地库直接执行当前 `selectTeamMonthlyRaw` 的 SQL 主体、把 `#{yearMonth}` 替换为 `NULL`，确认返回**静默降级**形态（所有人 0 工时、离场成员消失）。这就是「红」—— 证明「前端删掉校验」不足以实现需求。把查询与输出记入 `/tmp/018-red-2.txt`。
- [ ] **T013 [US2]** 🟢 修改 `DailyReportMapper.xml` 的 `selectTeamMonthlyRaw`：
  - `daily_hrs` 子查询：`r.report_date` 在 SELECT 与 GROUP BY 中条件化，月窗 WHERE 包 `<if test="yearMonth != null and yearMonth != ''">`
  - 主 SELECT 的 `reportDate` 列用 `<choose>` 输出 `daily_hrs.report_date` 或 `NULL`
  - 离场分支 `:564-565` 的月窗包 `<if>`
  - ⛔ **`EXISTS`(:566-569) 与 `NOT EXISTS`(:570-574) 留在 `<if>` 之外**（FR-002）
- [ ] **T014 [US2]** 验证 T013：用真实 SQL 分别以「传 2026-07」和「传 NULL」执行，断言 ① 无年月时原始行数落在 **2199 量级**（不是 16111，SC-005）；② 无年月时 `reportDate` 全为 NULL；③ 有年月时行数与 T003 基线一致。记入 `/tmp/018-green-2.txt`。
- [ ] **T015 [US2]** 🔴🟢 前端：删除 `teamReport.vue:293-296` 的年月必填校验（保留 `:289-292` 的 deptId 守卫）。红的判据是改动前手工清空年月点查询会被拦截，改动后能发起请求。

---

## Phase 4: US1 — 人员全周期可见 (P1)

### 🛡️ 安全回归护栏（最优先，先确认它是绿的）

- [ ] **T016 [US5]** 🛡️ **在改任何离场分支代码之前**，先跑既有安全用例：`npx playwright test e2e-team-daily-workload.spec.js`（跑前关验证码）。确认 `:227-282` 那条通过并记录输出。**这是本特性的红线**，后续每次改完 SQL 都要重跑它。

### 🔴 → 🟢 循环 3：离场成员月窗放宽

- [ ] **T017 [US1]** 🔴 用真实 SQL 证明现状：以 `yearMonth='2026-07'` 查 `project 38`，确认曲君**不出现**（月窗把他滤掉了），而该项目累计人天为 66.750 —— 这就是「有人天无贡献者」的红。记入 `/tmp/018-red-3.txt`。
- [ ] **T018 [US1]** 🟢 在 T013 的基础上，确认离场分支的月窗已被 `<if>` 包住 —— 即无年月时取全周期。**再额外把有年月时的行为也改为全周期**（Q2 拍板：不分场景一律全周期），即离场分支的日期条件**整体移除**而非条件化。
  ⚠️ 注意：这与 T013 的处理不同 —— `daily_hrs` 的月窗**要保留**（它决定日历格），离场分支的月窗**要移除**（它决定谁出现）。两者不可混为一谈。
- [ ] **T019 [US1]** 验证 T018：SQL 查 `project 38` 且 `yearMonth='2026-07'`，断言曲君**出现**且其 `dailyHours` 为空。再统计全库离场可见对数，断言为 **11**（SC-002）。
- [ ] **T020 [US5]** 🛡️ 重跑 T016 的安全用例，**必须仍然绿**。同时用 SQL 复核「写过 work 日报但成员表无行」的对数仍为 **0**（SC-003）。若任一失败，立即回退 T018。

---

## Phase 5: US3 — 展示增强 (P2)

### 🔴 → 🟢 循环 4：角色反推

- [ ] **T021 [P] [US3]** 🔴 新增 `selectTeamMonthly_multiRole_takesHighestPriority()`：mock 原始行含 `roleLabel = "项目经理"`，断言 VO 的 `roleLabel` 被正确赋值。**先跑红**（此时 Service 未赋值该字段，断言得到 null）。
- [ ] **T022 [P] [US3]** 🔴 新增 `selectTeamMonthly_noRole_returnsNullLabel()`：mock `roleLabel = null`，断言 VO 的 `roleLabel` 为 null（而非空串）。
- [ ] **T023 [US3]** 🟢 在 `DailyReportServiceImpl` 的成员层 `computeIfAbsent` 内加 `vo.setRoleLabel(str(row.get("roleLabel")))`。重跑 T021/T022 确认变绿。
  ⚠️ 注意 `str()` 对 null 的处理 —— 若它返回空串而非 null，T022 会失败，此时应调整为直接取值而非经 `str()`。
- [ ] **T024 [US3]** 🟢 在 `DailyReportMapper.xml` 的 SELECT 中加 `CASE ... END AS roleLabel`（plan.md D3 给出完整 SQL）。**必须加注释**说明「优先级把经理排在参与人员之前，是为抵消 `ProjectMemberServiceImpl.java:73-79` 把成员集合回写 participants 的副作用，不是脏数据」。
- [ ] **T025 [US3]** 验证 T024：SQL 查 `project 55`，断言冯先勇返回「项目经理」而非「项目经理/参与人员」；随机抽 5 个成员核对角色合理性。
- [ ] **T026 [US3]** 🟢 前端 `teamReport.vue:111` 渲染 `昵称（角色）`，`roleLabel` 为空时只显示昵称（**不显示空括号**）；`:239` 行映射透传 `roleLabel`；人员列 `width` 从 92 加宽（先试 130，实跑后调整）。

### 🔴 → 🟢 循环 5：机构分组

- [ ] **T027 [US3]** 🔴 新增 `selectTeamMonthly_projectDeptName_doesNotOverwriteMemberDeptName()`：mock 原始行同时含 `projectDeptName="深圳组"` 与 `deptName="研发中心"`，断言项目 VO 的 `projectDeptName` 为「深圳组」**且**成员 VO 的 `deptName` 仍为「研发中心」。**先跑红**。
- [ ] **T028 [US3]** 🟢 在项目层 `computeIfAbsent` 内加 `vo.setProjectDeptName(str(row.get("projectDeptName")))`。重跑 T027 确认变绿。
- [ ] **T029 [US3]** 🟢 在 `DailyReportMapper.xml` 加 `LEFT JOIN sys_dept pd ON pd.dept_id = p.project_dept`，SELECT 加 `pd.dept_name COLLATE utf8mb4_unicode_ci AS projectDeptName`。⚠️ **必须加 COLLATE**（跨字符集，Constitution V）。
- [ ] **T030 [US3]** 验证 T029：SQL 查一个 `project_dept=216` 的项目，断言返回「深圳组」而非父级「项目六组（华东地区组）」（BDD 3.5）；再查一个非叶子部门项目确认显示中间层名（BDD 3.6）。
- [ ] **T031 [US3]** 🟢 前端 `teamReport.vue:103` 之后新增一行 `.amount-line` 显示 `projectDeptName`（`v-if` 非空），`:218-225` 的 `projectExtra` 透传该字段。沿用既有 `.project-amounts` / `.amount-line` 样式，**不新增 CSS**。

### 🔴 → 🟢 循环 6：参与时间

- [ ] **T032 [P] [US3]** 🔴 新增 `selectTeamMonthly_participationSpan_prefersReportDates()`：mock 原始行四个日期字段齐全（`firstReportDate=2026-03-02`、`lastReportDate=2026-04-16`、`joinDate=2026-03-11`、`leaveDate=null`），断言 VO 四个字段均被正确赋值。**先跑红**。
- [ ] **T033 [US3]** 🟢 在成员层 `computeIfAbsent` 内赋值四个日期字段。重跑 T032 确认变绿。
- [ ] **T034 [US3]** 🟢 在 `DailyReportMapper.xml` 加两个 LEFT JOIN（`span` 日报首末日 + `mb` 成员表兜底，plan.md D4 给出完整 SQL），SELECT 加四列。⚠️ `mb` 子查询**必须先 `GROUP BY`** 再 JOIN（`pm_project_member` 缺唯一约束，直接 JOIN 会扇出）。
- [ ] **T035 [US3]** 验证 T034：SQL 查 `project 38` 的曲君，断言 `firstReportDate=2026-03-02`、`lastReportDate=2026-04-16`；再查一个从未填报的在册成员，断言其日报日期为 NULL 而 `joinDate` 有值。**同时复核原始行数未因两个新 JOIN 而膨胀**（应仍在 2199/4642 量级）。
- [ ] **T036 [US3]** 🟢 前端渲染参与时间：有日报用 `firstReportDate ~ lastReportDate`，无日报回退 `joinDate ~ (leaveDate || '至今')`，两者皆无则不渲染该行。`:239` 透传四字段。

---

## Phase 6: US4 + US3 措辞导航 (P2/P3)

> 全部为前端文案与 DOM 改动，可批量做完一起验。

- [ ] **T037 [P] [US3]** `teamReport.vue:5` 标签 `项目部门` → `项目所属部门`；`:8` placeholder → `请选择项目所属部门`。
- [ ] **T038 [P] [US4]** `:141` 表头 `实际人天` → `项目累计人天`，`width` 90 → 110；`:268` 注释同步改名。
- [ ] **T039 [P] [US4]** `:65` 图例改用「项目累计人天」；`:68` 图例去掉「本月」；`:70` 后新增第 4 条图例「项目累计人天 = 项目日报小时 ÷ 8 + 补正天数」。
- [ ] **T040 [P] [US3]** `:88-91` 项目名改为 `<a :href="projectHref(row.projectId)" @click.prevent="router.push(...)">`，照抄 `stats.vue:35-40` 的写法（含 `router.resolve().href`）。需在 `<script setup>` 中引入 `useRouter`。
- [ ] **T041 [US4]** 验证改名彻底性：`grep -n "实际人天" ruoyi-ui/src/views/project/dailyReport/teamReport.vue` 应**零命中**（SC-008）；`grep -rn "实际人天" ruoyi-project/src/main/java/com/ruoyi/project/domain/vo/TeamDailyReportVO.java` 亦应零命中。
- [ ] **T042 [P]** 同步种子 SQL：`pm-sql/init/02_menu_data.sql:287` 的 `'团队日报'` → `'项目日报'`（线上已改名，此处仅为新环境初始化对齐，R-007）。

---

## Phase 7: E2E / BDD 落地

- [ ] **T043** 新建 `tests/e2e-project-daily-report.spec.js`，用 `tests/helpers/api-client.js` 的 `setupApi()` 复用登录。按 `bdd/coverage.md` 落地 15 条 e2e 场景（API 层 8 条 + UI 层 7 条）。
- [ ] **T044** 造数确认：e2e 需要一个「离场成员有历史工时」的项目。**先查本地库是否已有可用数据**（`project 38` 曲君是天然样本），能用既有数据就不要造数 —— 造数会污染其他套件的计数断言（记忆教训：013 套件要求恰好 3 个 seed 项目）。
- [ ] **T045** 跑 e2e：临时关验证码 → `npx playwright test e2e-project-daily-report.spec.js` → 恢复验证码。**贴真实输出**，失败就说失败。
- [ ] **T046** 🛡️ 跑安全回归：`npx playwright test e2e-team-daily-workload.spec.js`，必须全绿。
- [ ] **T047** 实跑布局验证（R-003）：起前端，不填年月点查询，**肉眼确认** 5 列全 fixed、中间 0 列时布局正常。异常则改用 `:fixed="dayColumns.length ? 'right' : false"`。**截图存档**。

---

## Phase 8: 收尾

- [ ] **T048** 全量回归：`mvn test -pl ruoyi-project -am`，与 T001 基线对比，数量不低于基线且全绿（SC-007）。
- [ ] **T049** Lint：`cd ruoyi-ui && npm run build:prod` 成功；`vue-tsc` 错误数与 T002 基线对比**不上升**（R-005）。
- [ ] **T050** code review：多视角对抗审查（正确性 / 安全 / 性能 / 口径一致性）。重点审 ① `EXISTS` 是否被误动；② 三个新 JOIN 是否引起扇出；③ 改名是否遗漏。
- [ ] **T051** 安全审查：按全局要求，提交前跑一次安全审查（重点：新增 SQL 是否有注入面、`projectDeptName` 是否泄露越权信息）。
- [ ] **T052** 更新文档：把实测数字回填到 spec.md 的 SC 章节；`CLAUDE.md` 的 Daily Reports 段落补充「项目日报」页面的新行为。

---

## 依赖关系

```
T001-T003 (基线)
   ↓
T004-T007 (VO + 测试脚手架)
   ↓
Phase 3 (US2: D2 缺陷 + 月窗条件化)  ← 头号目标，挡着所有无年月验证
   ↓
T016 (安全基线) → Phase 4 (US1: 离场放宽) → T020 (安全复核)
   ↓
Phase 5 (US3: 三个新字段，循环 4/5/6 可并行推进)
   ↓
Phase 6 (文案与导航，全 [P])
   ↓
Phase 7 (E2E) → Phase 8 (收尾)
```

**并行提示**：Phase 5 的三个循环（角色 / 机构 / 参与时间）改的是同一个 SQL 与同一个 Service 方法，**SQL 与 Service 部分不可并行**；但各自的 JUnit 测试（T021/T022、T027、T032）互不干扰，可先批量写完再逐个转绿。

---

## 任务统计

| Phase | 任务数 | 其中 🔴 | 其中 🛡️ |
|---|---|---|---|
| 1 Setup | 3 | — | — |
| 2 Foundational | 4 | — | — |
| 3 US2 | 8 | 3 | 2 |
| 4 US1 | 5 | 1 | 2 |
| 5 US3 | 16 | 4 | — |
| 6 文案导航 | 6 | — | — |
| 7 E2E | 5 | — | 1 |
| 8 收尾 | 5 | — | — |
| **合计** | **52** | **8** | **5** |
