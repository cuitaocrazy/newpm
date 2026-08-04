# Implementation Plan: 项目日报的人员可见性与口径自证

**特性分支**: `018-project-daily-report` | **日期**: 2026-08-03 | **Spec**: [spec.md](./spec.md)
**基线**: `015-daily-report-ownership-check` @ `d269536`

## Summary

一句话：**把「按月可见」放宽为「全周期可见」，并让页面上的每个数字都能自证来源。**

改动集中在一条 SQL（`selectTeamMonthlyRaw`）、一段 Java 聚合（`selectTeamMonthly`）、两个 VO、一个 Vue 文件。**无 schema 变更、无新增接口、无新增依赖。**

技术上有四件事：

1. **月窗条件化** —— `#{yearMonth}` 的 4 处引用加 `<if>` 守卫，且 `daily_hrs` 子查询要有两种形态（带 `report_date` 分组 / 不带）。
2. **三个新字段进 SQL** —— 角色（查询期 CASE 反推）、机构分组（LEFT JOIN `sys_dept`）、参与时间（两个 LEFT JOIN：日报首末日 + 成员表兜底）。
3. **一处 Java 聚合缺陷修复** —— 现有 null 守卫会在无年月形态下吞掉个人人天，必须拆开（见「关键决策 D2」，这是本方案唯一的隐蔽坑）。
4. **前端 11 处改动** —— 全部在 `teamReport.vue` 内，无新增组件。

---

## Technical Context

| 项 | 值 |
|---|---|
| **语言/版本** | Java 17 / TypeScript 5.6 |
| **主要依赖** | Spring Boot 3.5.8、MyBatis、Vue 3.5、Element Plus 2.13、dayjs（均为既有，无新增） |
| **存储** | MySQL 8.x (`ry-vue`)。涉及表：`pm_project`、`pm_project_member`、`pm_daily_report`、`pm_daily_report_detail`、`sys_dept`、`sys_user`。**全部只读，无 DDL** |
| **测试** | JUnit 5 + Mockito（`ruoyi-project`）、Playwright（根 `tests/`） |
| **目标平台** | 既有 K3s 部署，无变化 |
| **性能目标** | 不填年月时查询原始行数 ≤ 2200 行量级（现状带月窗 4642 行）；**禁止**出现 16111 行形态 |
| **约束** | `DailyReportMapper.xml:566-569` 的 `EXISTS` 安全约束一行不动；e2e 安全用例保持绿 |

---

## Constitution Check

*GATE: Phase 0 前必须通过，Phase 1 设计后复查。*

| 原则 | 相关要求 | 本特性符合性 |
|---|---|---|
| **I. 业务完整性优先** | mutating 接口 MUST 加 `@Log` | ⚪ 不适用：本特性**全部为只读查询**，不新增任何 mutating 接口 |
| | 硬删除例外表约定 | ⚪ 不适用：不涉及删除 |
| **II. 权限驱动访问控制** | Controller MUST 有 `@PreAuthorize` | ✅ 无新增 Controller；`/teamMonthly` 既有权限注解不变 |
| | 部门数据隔离用 `@DataScope` | ⚠️ **保持现状不动**（OUT-001）。现 `@DataScope(deptAlias="d", userAlias="u")` 的 `d` 绑成员本人部门而非项目部门，语义存疑但**属既有行为**，改它是权限模型变更，不夹带进本 UI 特性。已在 spec「已知限制」登记并另开 Issue |
| **III. API 与代码一致性** | 业务异常抛 `ServiceException` | ⚪ 不适用：只读查询无业务异常路径 |
| | Service 方法命名约定 | ✅ 不新增 Service 方法，仅修改 `selectTeamMonthly` 内部聚合逻辑 |
| **IV. 任务与项目解耦** | 禁止向 `pm_project` 加任务字段 | ✅ **无 schema 变更**；角色反推读的是 `pm_project` 既有的 4 个 manager 字段 |
| | `pm_project.update_by/update_time` 只能由项目编辑更新 | ✅ 本特性不写 `pm_project` |
| **V. 数据库规范** | PM 表与系统表 JOIN 须加 `COLLATE` | ⚠️ **本特性新增一处跨字符集 JOIN**：`pm_project`(utf8mb4_0900_ai_ci) ↔ `sys_dept`(utf8mb4_unicode_ci)。JOIN 条件本身是数值比较（`dept_id` = `project_dept`）不涉 collation，但 **SELECT 出的 `dept_name` MUST 加 `COLLATE utf8mb4_unicode_ci`**，与既有 `:527-528` 写法一致 |
| | 人天公式须含补正量 | ✅ `actualPersonDays` 公式不动（`ROUND(actual_workload/8,3) + COALESCE(adjust_workload,0)`），且本特性正是要把该公式**显式写到界面上**（FR-016） |
| | Schema 变更策略 | ✅ 无 schema 变更，不需要 `fix_*.sql`。**唯一可能的 SQL 是菜单改名**（假设 7），须按 `component` 定位、禁止硬编码 `menu_id` |
| **VI. 前端组件与字典规范** | 字典用 `<dict-select>`、禁止硬编码 | ✅ 新增的「角色」不是字典项（数据库中不存在角色表，见 OUT-003），是查询期反推的展示标签，不适用字典规范。既有 `dict-tag` 用法不变 |

**结论**：通过。两处 ⚠️ 均为**显式登记的已知项**而非违规：`@DataScope` 是保持现状 + 另开 Issue，collation 是新增 JOIN 但已给出强制写法。无需 Complexity Tracking。

---

## 关键决策

### D1：月窗条件化 —— `daily_hrs` 必须有两种形态，不能只删 WHERE

**为什么不能简单删掉月窗**：实测数据说话。

| 形态 | 主查询原始行数 | 说明 |
|---|---|---|
| 现状（带月窗） | 4,642 | 基线 |
| 去月窗但保留 `report_date` 分组 | **16,111**（3.5x） | ❌ 且日期 key 从 31 个变成 **181 个**（全库 distinct `report_date`，跨度 2025-12-24~2026-08-15），前端会尝试渲染 181 个日期列 —— 与 FR-008「日期列不显示」直接矛盾 |
| 去月窗且 `daily_hrs` 退化为无 `report_date` 的 SUM | **2,199** | ✅ 目标形态 |

**改法**：`daily_hrs` 子查询的 `report_date` 在 SELECT / GROUP BY 中同时条件化：

```xml
LEFT JOIN (
    SELECT r.user_id,
           <if test="yearMonth != null and yearMonth != ''">r.report_date,</if>
           rd.project_id,
           SUM(rd.work_hours) AS projectWorkHours
    FROM pm_daily_report r
    JOIN pm_daily_report_detail rd ON rd.report_id = r.report_id
        AND rd.entry_type = 'work' AND rd.del_flag = '0'
    WHERE r.del_flag = '0'
      <if test="yearMonth != null and yearMonth != ''">
      AND r.report_date &gt;= STR_TO_DATE(CONCAT(#{yearMonth}, '-01'), '%Y-%m-%d')
      AND r.report_date &lt;  DATE_ADD(STR_TO_DATE(CONCAT(#{yearMonth}, '-01'), '%Y-%m-%d'), INTERVAL 1 MONTH)
      </if>
    GROUP BY r.user_id, <if test="yearMonth != null and yearMonth != ''">r.report_date,</if> rd.project_id
) daily_hrs ON daily_hrs.user_id = m.user_id AND daily_hrs.project_id = p.project_id
```

主 SELECT 中的 `reportDate` 列同步条件化（无年月时输出 `NULL AS reportDate`）：

```xml
<choose>
    <when test="yearMonth != null and yearMonth != ''">daily_hrs.report_date</when>
    <otherwise>NULL</otherwise>
</choose> AS reportDate,
```

离场分支（`:564-565`）的月窗同样包 `<if>`。

> ⛔ **`EXISTS`(:566-569) 与 `NOT EXISTS`(:570-574) 必须留在 `<if>` 之外。** 它们不是时间条件，是准入判据（FR-002）。把它们卷进 `<if>` 会在无年月时直接开出越权读取通道。

### D2：Java 聚合的隐蔽缺陷 —— 现有 null 守卫会吞掉个人人天 ⚠️

这是本方案**唯一不改就会静默出错**的地方。`DailyReportServiceImpl.java:786-796` 现为：

```java
Object reportDate = row.get("reportDate");
Object totalWorkHours = row.get("totalWorkHours");
if (reportDate != null && totalWorkHours != null)   // ← 两个条件是「与」
{
    String dateStr = reportDate.toString().substring(0, 10);
    BigDecimal hours = toBigDecimal(totalWorkHours);
    member.getDailyHours().merge(dateStr, hours, BigDecimal::add);
    member.setTotalHours(member.getTotalHours().add(hours));   // ← 被 reportDate 连坐
}
```

D1 的无年月形态下 `reportDate` 恒为 `NULL` 而 `totalWorkHours` 有值（全周期 SUM）。当前守卫会让**整个分支不执行**，`totalHours` 停在 `BigDecimal.ZERO` —— 表现为「个人人天全部为 0」，而右侧项目累计人天正常。这正是 FR-009 要避免的。

**改法**：把「累加总工时」与「填充日历格」拆开，前者只依赖 `totalWorkHours`：

```java
Object reportDate = row.get("reportDate");
Object totalWorkHours = row.get("totalWorkHours");
if (totalWorkHours != null)
{
    BigDecimal hours = toBigDecimal(totalWorkHours);
    member.setTotalHours(member.getTotalHours().add(hours));   // 无条件累加
    if (reportDate != null)                                     // 仅日历格依赖日期
    {
        String dateStr = reportDate.toString().substring(0, 10);
        member.getDailyHours().merge(dateStr, hours, BigDecimal::add);
    }
}
```

**这是必须先写红测试的头号目标**（见 tasks.md 循环 1）。

### D3：角色反推 —— 查询期 CASE，不落库

角色在数据库中**不存在**：`pm_project_member` 无角色列，且 `ProjectServiceImpl.java:548-596` 的 `syncProjectMembers` 把 5 类人倒进一个 `Set<Long>` 后统一同步，身份在这一步被抹平。

`p` 别名已在 FROM 中，无需新 JOIN：

```sql
CASE
    WHEN p.project_manager_id = u.user_id                 THEN '项目经理'
    WHEN p.market_manager_id  = u.user_id                 THEN '市场经理'
    WHEN p.sales_manager_id   = u.user_id                 THEN '销售负责人'
    WHEN FIND_IN_SET(u.user_id, p.participants) > 0       THEN '参与人员'
    ELSE NULL
END AS roleLabel
```

三条设计约束，每条都有依据：

1. **`team_leader_id` 不入链** —— 实测 284 个有效项目中 100% 为 `NULL`，永远不命中（假设 3）。
2. **优先级顺序不可调换** —— 实测 21.8%（473/2189 行）命中 2 个以上角色，根因是 `ProjectMemberServiceImpl.java:73-79` 把整个成员集合回写 `pm_project.participants`，导致经理们也进了 `participants`。**把经理排在参与人员之前正是为了抵消这个副作用**，实现时须加注释说明，防止后续有人把它当脏数据去「修」。
3. **叫「销售负责人」不叫「销售经理」** —— 依据 DB 注释与前端 label（`project/edit.vue:285`）。

### D4：参与时间 —— 主源日报首末日，成员表兜底

两个 LEFT JOIN，**都不受 `yearMonth` 影响**（参与时间是全周期属性，不是「本月工时」）：

```sql
-- 主源：该人在本项目的日报首末日
LEFT JOIN (
    SELECT r.user_id, rd.project_id,
           MIN(r.report_date) AS firstReportDate,
           MAX(r.report_date) AS lastReportDate
    FROM pm_daily_report r
    JOIN pm_daily_report_detail rd ON rd.report_id = r.report_id
        AND rd.entry_type = 'work' AND rd.del_flag = '0'
    WHERE r.del_flag = '0'
    GROUP BY r.user_id, rd.project_id
) span ON span.user_id = m.user_id AND span.project_id = p.project_id

-- 兜底：成员表的在册区间。不限 del_flag/is_active，与 EXISTS(pm3) 判据对称
LEFT JOIN (
    SELECT project_id, user_id,
           MIN(join_date)  AS joinDate,
           MAX(leave_date) AS leaveDate
    FROM pm_project_member
    GROUP BY project_id, user_id
) mb ON mb.project_id = p.project_id AND mb.user_id = m.user_id
```

**为什么兜底源用独立 LEFT JOIN 而不是改 UNION 两支**：UNION 子查询紧邻 `EXISTS` 安全约束，改动面越小越安全；且 `pm_project_member` 历史上缺 `(project_id,user_id)` 唯一约束（`:542-543` 注释已记载），直接 JOIN 会扇出，必须先聚合 —— 独立 JOIN 里做 `GROUP BY` 天然解决。

**为什么主源是日报首末日**（澄清 Q3 已拍板）：`join_date` 实为系统录入日（`ProjectMemberServiceImpl.java:117` `Date now = new Date()`），实测 934 对有日报的成员中 **383 对（41%）首次日报早于 `join_date`**，仅 130 对相等，且有批量录入痕迹（2026-03-11 一天录入 295 行）。本页是「日报人天」视图，与工时同源的首末日才有解释力。

### D5：机构分组 —— `project_dept` 自身名，独立别名

```sql
LEFT JOIN sys_dept pd ON pd.dept_id = p.project_dept
...
pd.dept_name COLLATE utf8mb4_unicode_ci AS projectDeptName
```

- **别名必须是 `projectDeptName`，不能复用 `deptName`** —— 后者是成员本人部门（`:577` `INNER JOIN sys_dept d ON d.dept_id = u.dept_id`），实测 2188 条在册成员行中 **831 条（38%）两者不同**，撞名会覆盖成员部门。
- **口径是部门自身名，不是 ancestors 末段** —— 已被数据排除：对「深圳组」(`dept_id=216`) 取 ancestors 末段会得到父级「项目六组（华东地区组）」，且 216 无子部门，任何项目都不可能让该口径产出「深圳组」。
- `project_dept` 是 `varchar(100)` 存数字串，与 `dept_id`(bigint) 比较触发隐式转换。`sys_dept` 表小（百行量级），可接受；实测 284 个有效项目 0 空值 0 孤儿。

---

## VO 扩展

| VO | 新增字段 | 类型 | 说明 |
|---|---|---|---|
| `TeamDailyReportVO` | `projectDeptName` | `String` | 机构分组名（FR-013） |
| `TeamMemberDailyVO` | `roleLabel` | `String` | 角色标签，无角色时为 `null`（FR-011/012） |
| | `firstReportDate` | `String` | 日报首日 `yyyy-MM-dd`，无日报时 `null` |
| | `lastReportDate` | `String` | 日报末日 |
| | `joinDate` | `String` | 成员表在册起（兜底源） |
| | `leaveDate` | `String` | 成员表在册止，可为 `null` |

参与时间的**展示口径决策放在前端**（后端只吐原始四个日期），理由：口径若将来调整（如业务改要人事口径）只需改一个 Vue 文件，不必动 SQL 与 VO。

同时更新 `TeamDailyReportVO.java:24` 的注释：`实际人天` → `项目累计人天`（保留公式）。

---

## 前端改动清单（全部在 `teamReport.vue`）

| # | 位置 | 改动 | FR |
|---|---|---|---|
| 1 | `:5` | `label="项目部门"` → `"项目所属部门"` | FR-018 |
| 2 | `:8` | placeholder「请选择三级部门」→「请选择项目所属部门」（实测 `ProjectDeptSelect:74-75` 过滤是 `>= 3` 级，即三级**及更深**都可选，原措辞不准） | — |
| 3 | `:65` | 图例「实际人天为红色 = 实际人天已超预算的 50%」→ 改用「项目累计人天」 | FR-015 |
| 4 | `:68` | 图例「**本月**有工时但已不在项目成员名单」→ 去掉「本月」 | FR-004 |
| 5 | `:70` 后 | 新增第 4 条图例：「项目累计人天 = 项目日报小时 ÷ 8 + 补正天数」 | FR-016 |
| 6 | `:88-91` | 项目名改为 `<a :href>` + `@click.prevent`（照抄 `stats.vue:35-40`） | FR-019 |
| 7 | `:103` 后 | 新增一行 `.amount-line` 显示 `projectDeptName`，沿用既有 `.project-amounts` flex 与 `.amount-line` 样式，**无需新 CSS** | FR-013 |
| 8 | `:109-114` | 人员列：昵称后拼 `（角色）`、新增参与时间行；`width=92` → 需加宽（角色约 130px，含参与时间需实测） | FR-011/012/017 |
| 9 | `:141` | 表头 `label="实际人天"` → `"项目累计人天"`；`width=90` 可能需 100-110 | FR-015 |
| 10 | `:268` | 注释中「实际人天(汇总)」同步改名 | FR-015 |
| 11 | `:293-296` | 删除 4 行年月必填校验；`:289-292` 的 deptId 守卫保留 | FR-006 |
| 12 | `:218-225` / `:239-243` | `projectExtra` 透传 `projectDeptName`；成员行映射透传 `roleLabel` 与四个日期 | — |

**零改动确认**：
- `dayColumns`(`:197-206`) 首行即 `if (!yearMonth) return []`，FR-008 天然满足。
- `spanMethod`(`:269-279`) 的列索引算术在 `N=0` 时仍正确 —— 列表退化为 `[0 项目, 1 人员, 2 个人人天, 3 项目累计人天, 4 预算人天]`，公式 `actualColIndex = 2+N+1 = 3` ✓、`budgetColIndex = N+4 = 4` ✓，对任意 `N≥0` 恒等。**但该结论来自 Element Plus 源码推演，未实跑**，见风险 R-003。

---

## Project Structure

### Documentation (this feature)

```
specs/018-project-daily-report/
├── spec.md              # 需求（已完成）
├── plan.md              # 本文件
├── data-model.md        # 字段级契约：新增列的来源、类型、边界值
├── bdd/
│   ├── project-daily-report.feature   # 中文 Gherkin 场景
│   └── coverage.md                    # 场景 → JUnit/Playwright 映射
└── tasks.md             # TDD 红绿任务清单
```

### Source Code

```
ruoyi-project/src/main/
├── java/com/ruoyi/project/
│   ├── service/impl/DailyReportServiceImpl.java     # D2 聚合修复 + 3 个新字段赋值
│   └── domain/vo/
│       ├── TeamDailyReportVO.java                   # +projectDeptName，注释改名
│       └── TeamMemberDailyVO.java                   # +roleLabel +4 个日期
└── resources/mapper/project/
    └── DailyReportMapper.xml                        # selectTeamMonthlyRaw 全部 SQL 改动

ruoyi-project/src/test/java/com/ruoyi/project/service/impl/
└── DailyReportServiceImplTest.java                  # 新增 teamMonthly 区块（当前 0 覆盖）

ruoyi-ui/src/views/project/dailyReport/
└── teamReport.vue                                   # 12 处改动

tests/
└── e2e-project-daily-report.spec.js                 # 新增 e2e（既有 e2e-team-daily-workload.spec.js 保持绿）
```

---

## 风险与缓解

| ID | 风险 | 证据 | 缓解 |
|---|---|---|---|
| **R-001** | `selectTeamMonthly` 零单测覆盖，聚合层改错无护栏 | 全仓 grep 仅 5 处命中全在 `main/`，`src/test` **0 命中**；既有 47 个 `@Test` 无一条读 teamMonthly | tasks.md 的第一个循环就是补这块的红。D2 缺陷必须由测试先暴露 |
| **R-002** | 需求 3 被误实现成「删掉 EXISTS」 | `DailyReportMapper.xml:552-558` 注释 + e2e `:227-282` 双重守护 | `<if>` 只包日期条件；PR 自检清单强制 diff 检查 `EXISTS` 未被触碰 |
| **R-003** | 不填年月时 5 列全 fixed（2 左 + 3 右）、中间 0 列，布局未实跑 | 列 fixed 声明 `:86/:109/:133/:141/:150`；worktree 内无 `node_modules` 无法起前端 | 实现后**必须实跑**一次「不填年月 → 查询」看布局。异常则用 `:fixed="dayColumns.length ? 'right' : false"` |
| **R-004** | 口径改名漏改，同屏两个名字指同一列 | 5 处涉及：`:141` 表头、`:65` 图例、`:68` 图例、`:268` 注释、`TeamDailyReportVO.java:24` | SC-008 用全页搜索「实际人天」零命中作为验收 |
| **R-005** | 前端 lint 无门禁，`vue-tsc` 基线就红 | `package.json` 无 lint script、全仓零 eslint 配置；`vue-tsc --noEmit` 在**未改一行**的基线上报 **39 个 error**，其中 4 个在 `teamReport.vue` | Lint 定义为 `npm run build:prod` + `mvn compile`；`vue-tsc` 只比对错误数**不上升**，不设 0 error 门槛 |
| **R-006** | 非 admin 数据范围行为未验证 | 本地 admin 是「全部」范围，`DataScopeAspect.java:114-118` 直接清空 sqlString，所有验证都绕开了该路径 | OUT-001 明确不改；上线前用真实非 admin 账号在生产做一次对照观测 |
| **R-007** | 种子 SQL 菜单名滞后 | 用户 2026-08-03 确认生产已改名「项目日报」，本地 `menu_id=2220` 亦是新名；但 `pm-sql/init/02_menu_data.sql:287` 仍写 `VALUES ('团队日报', ...)` | 同步改种子 SQL 一处文案即可，**不需要 fix SQL**（线上两处已是新名）。路由 `path`/组件名 `teamReport` 保持不变 |

---

## 设计后复查（Phase 1 完成）

设计未引入新接口、新表、新依赖、新前端组件；新增的三处 LEFT JOIN 均为聚合子查询或小表关联；跨字符集处已强制 `COLLATE`；安全约束保持逐字不动。**Constitution 复查：仍全部通过。**
