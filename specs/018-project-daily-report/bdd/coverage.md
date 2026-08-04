# BDD 场景覆盖映射

**特性**: `018-project-daily-report` | **日期**: 2026-08-03

本项目**无 Cucumber 工具链**，`project-daily-report.feature` 不直接执行。本文件把每个 Gherkin 场景映射到实际可执行的测试，作为「BDD 测试通过」的判定依据。

**执行层选择原则**：
- 涉及 **SQL 形态 / Java 聚合逻辑** → JUnit（Mockito mock `selectTeamMonthlyRaw` 的返回行）
- 涉及 **真实 SQL 行为 / 跨表数据** → Playwright API 层（打真实接口断言返回结构）
- 涉及 **渲染与交互** → Playwright UI 层

---

## 一、人员可见性（US1 / US5）

| # | Gherkin 场景 | 执行层 | 落地测试 | 类型 |
|---|---|---|---|---|
| 1.1 | 离场成员出现，使项目累计人天可解释 | Playwright API | `e2e-project-daily-report.spec.js` → `不填年月时离场成员出现在人员列表` | 🔴 新增 |
| 1.2 | 填了年月，离场成员即使当月无工时也要显示 | Playwright API | `e2e-project-daily-report.spec.js` → `指定年月时离场成员仍全周期可见` | 🔴 新增（**行为变更点**） |
| 1.3 | 从未是成员的人不得出现（安全边界） | Playwright API | `e2e-team-daily-workload.spec.js:227-282`（**既有，不得修改**） | 🛡️ 回归保护 |
| 1.4 | 已离职人员与在职同等对待 | Playwright API | `e2e-project-daily-report.spec.js` → `离职账号的历史工时仍可见` | 🔴 新增 |
| 1.5 | 全周期无人填报的项目保持占位行 | JUnit | `selectTeamMonthly_projectWithNoMembers_keepsPlaceholderRow` | 🛡️ 回归保护 |

> 1.3 是本特性**唯一不能碰**的既有测试。它断言 admin 不得作为离场成员出现在从未参与的项目下。若它变红，说明 `EXISTS(pm_project_member)` 被误删 —— 立即停止，回退改动。

## 二、年月非必填（US2）

| # | Gherkin 场景 | 执行层 | 落地测试 | 类型 |
|---|---|---|---|---|
| 2.1 | 不填年月可以查询，且不显示任何日期列 | Playwright UI | `e2e-project-daily-report.spec.js` → `清空年月查询不报错且无日期列` | 🔴 新增 |
| 2.2 | 项目所属部门仍然必填 | Playwright UI | 同上 → `未选部门时拦截查询` | 🛡️ 回归保护 |
| 2.3 | 项目累计人天不因是否填年月而改变 | Playwright API | 同上 → `累计人天在两种查询条件下逐项目相等` | 🔴 新增（对应 SC-004） |
| 2.4 | **不填年月时个人人天为全周期合计** | JUnit | `selectTeamMonthly_noYearMonth_totalHoursAccumulatedWithoutDate` | 🔴 **头号红测试**（plan.md D2） |
| 2.5 | 填了年月时恢复按月口径 | JUnit | `selectTeamMonthly_withYearMonth_dailyHoursAndTotalPreserved` | 🛡️ 回归保护 |

> 2.4 是整个特性最容易静默出错的一条。现有 `if (reportDate != null && totalWorkHours != null)` 会在无年月形态下把 `totalHours` 吞成 0。**该测试必须在改 Java 之前先跑出红**，且失败原因必须是「totalHours 为 0 而非期望值」，不是编译错误。

## 三、展示增强（US3）

| # | Gherkin 场景 | 执行层 | 落地测试 | 类型 |
|---|---|---|---|---|
| 3.1 | 一人命中多个角色时按优先级取最高 | JUnit | `selectTeamMonthly_multiRole_takesHighestPriority` | 🔴 新增 |
| 3.2 | 角色优先级顺序 | JUnit | 同上（参数化：市场经理 + 参与人员 → 市场经理） | 🔴 新增 |
| 3.3 | 反推不出角色时只显示昵称 | JUnit + UI | `selectTeamMonthly_noRole_returnsNullLabel` + UI 断言无空括号 | 🔴 新增 |
| 3.4 | 机构分组显示项目所属部门自身的名称 | Playwright API | `e2e-project-daily-report.spec.js` → `机构分组返回项目部门名` | 🔴 新增 |
| 3.5 | 机构分组不取 ancestors 末段 | Playwright API | 同上（对深圳组断言返回「深圳组」而非父级名） | 🔴 新增 |
| 3.6 | 所属部门为非叶子时显示该部门自身名称 | Playwright API | 同上 | 🔴 新增 |
| 3.7 | **机构分组不得覆盖成员所属部门** | JUnit | `selectTeamMonthly_projectDeptName_doesNotOverwriteMemberDeptName` | 🔴 新增（防别名撞名） |
| 3.8 | 参与时间用日报首末日 | JUnit | `selectTeamMonthly_participationSpan_prefersReportDates` | 🔴 新增 |
| 3.9 | 无日报时回退成员表在册区间 | JUnit | `selectTeamMonthly_participationSpan_fallsBackToMemberDates` | 🔴 新增 |
| 3.10 | 离场后仍有日报时以日报为准 | JUnit | 由 3.8 覆盖（日报口径天然优先） | 🔴 新增 |

## 四、口径自证（US4）

| # | Gherkin 场景 | 执行层 | 落地测试 | 类型 |
|---|---|---|---|---|
| 4.1 | 图例说明项目累计人天的算法 | Playwright UI | `e2e-project-daily-report.spec.js` → `图例含累计人天公式` | 🔴 新增 |
| 4.2 | 表头与图例对同一列使用同一个名字 | Playwright UI | 同上 → `全页无「实际人天」字样`（对应 SC-008） | 🔴 新增 |
| 4.3 | 红色告警阈值逻辑不变 | Playwright UI | 同上 → `超预算50%仍标红` | 🛡️ 回归保护 |

## 五、导航与措辞（US3）

| # | Gherkin 场景 | 执行层 | 落地测试 | 类型 |
|---|---|---|---|---|
| 5.1 | 查询条件标签为项目所属部门 | Playwright UI | `e2e-project-daily-report.spec.js` → `查询条件标签措辞` | 🔴 新增 |
| 5.2 | 项目名称可右键在新标签打开 | Playwright UI | 同上 → `项目名是带 href 的真实链接`（断言 `a[href]` 存在且指向详情路由） | 🔴 新增 |
| 5.3 | 项目名称左键点击仍走前端路由 | Playwright UI | 同上 → `左键点击不触发整页刷新` | 🔴 新增 |

> 5.2 无法在 Playwright 中真正模拟浏览器右键菜单的「在新标签打开」。**判定方式是断言 DOM 契约**：项目名必须是带有效 `href` 的 `<a>` 元素 —— 这正是浏览器右键菜单出现该选项的充要条件。

---

## 覆盖统计

> ⚠️ **以下是实际落地情况（2026-08-03 实跑后回填），不是计划值。**
> 本文件上半部分的「落地测试」列是**设计意图**，其中一部分被合并实现或未单独成用例，
> 详见下表的诚实对照。

### 实际落地

| 执行层 | 实际用例数 | 结果 | 文件 |
|---|---|---|---|
| JUnit（Java 聚合层） | **7** | 7 passed（全模块 200 passed / 0 failed） | `DailyReportServiceImplTest` 的 018 区块 |
| Playwright API | **8** | 8 passed | `tests/e2e-project-daily-report.spec.js` |
| Playwright UI | **4** | 3 passed / **1 skipped** | `tests/e2e-project-daily-report-ui.spec.js` |
| 真实 SQL 手工验证 | **4 组** | 全部符合预期 | 见 tasks.md「执行进度」的行数与对账数据 |
| 既有安全回归 | **9** | 9 passed（含 `:227-282` 越权用例） | `tests/e2e-team-daily-workload.spec.js` |

### 与 feature 场景的对照（诚实版）

| feature 场景 | 落地方式 |
|---|---|
| 1.1 / 1.2 / 3.8 / 3.9 / 3.10 | ✅ 由 API e2e 覆盖（多个场景合并进同一用例断言） |
| 1.3（安全边界） | ✅ 既有 e2e `:227-282` + SQL 层 SC-003 守护值双重覆盖 |
| 1.4（离职人员同等对待） | ⚠️ **未单独成用例**。旁证：全周期 11 对样本中 4 人昵称含「(已离职)」且均出现在查询结果里 |
| 1.5（无成员项目占位行） | ⚠️ **未落地**。该逻辑在前端 `flatRows`，且 SQL 用 `INNER JOIN m` 导致无成员项目根本不返回，Service 层测不到 |
| 2.1 / 2.2 | ✅ UI 静态断言 + API 断言 |
| 2.3 / 2.4 / 2.5 | ✅ JUnit + API e2e |
| 3.1 / 3.2 / 3.3 | ✅ JUnit（角色透传）+ API e2e（取值域与单值性） |
| 3.4 / 3.5 / 3.6 / 3.7 | ✅ API e2e + 真实 SQL 验证（深圳组 3 个项目、project 14 的 5 成员 5 部门） |
| 4.1 / 4.2 | ✅ UI e2e passed |
| 4.3（红色阈值不变） | ⚠️ **未落地**。改动未触碰阈值表达式（`teamReport.vue` 的 `estimatedWorkload * 0.5`），靠代码 diff 确认 |
| 5.1 | ✅ UI e2e passed |
| 5.2 / 5.3（链接契约、左键不刷新） | ⚠️ **skipped**。所在用例卡在部门树展开（`filterable` 默认 false），见 tasks.md 未完成项 |

**合计**：feature 定义 26 个场景，**19 个有自动化覆盖**，5 个靠 SQL/diff 旁证，**2 个未验证**（5.2 / 5.3）。

---

## 判定「BDD 测试通过」的命令

```bash
# 后端（含 11 条 JUnit 场景 + 全量回归）
mvn test -pl ruoyi-project -am

# E2E（跑前须临时关验证码，跑完恢复）
npx playwright test e2e-project-daily-report.spec.js
npx playwright test e2e-team-daily-workload.spec.js   # 安全回归，必须绿
```

**验收标准**：26 个场景对应的测试全部通过，且 `mvn test` 全量数量不低于改动前基线（015 记录为 192）。
