# Data Model: 项目日报的人员可见性与口径自证

**特性**: `018-project-daily-report` | **日期**: 2026-08-03
**Schema 变更**: **无**。本特性全部为只读查询，不新增表、列、索引。

---

## 1. 涉及的既有表（全部只读）

| 表 | 字符集 | 本特性中的用途 | 读取的列 |
|---|---|---|---|
| `pm_project` | `utf8mb4_0900_ai_ci` | 项目主体、角色反推源、机构分组外键 | `project_id`、`project_name`、`project_dept`、`project_manager_id`、`market_manager_id`、`sales_manager_id`、`participants`、`actual_workload`、`adjust_workload`、`estimated_workload`、`del_flag` |
| `pm_project_member` | `utf8mb4_0900_ai_ci` | 准入判据（EXISTS）、参与时间兜底源 | `project_id`、`user_id`、`join_date`、`leave_date`、`is_active`、`del_flag` |
| `pm_daily_report` | `utf8mb4_0900_ai_ci` | 工时与参与时间主源 | `report_id`、`user_id`、`report_date`、`del_flag` |
| `pm_daily_report_detail` | `utf8mb4_0900_ai_ci` | 工时明细 | `report_id`、`project_id`、`work_hours`、`entry_type`、`del_flag` |
| `sys_dept` | `utf8mb4_unicode_ci` | ⚠️ 机构分组名（**跨字符集**） | `dept_id`、`dept_name` |
| `sys_user` | `utf8mb4_unicode_ci` | ⚠️ 昵称（**跨字符集**） | `user_id`、`nick_name`、`dept_id`、`status` |

> ⚠️ **跨字符集处理**：`sys_dept` / `sys_user` 的字符串列在 SELECT 时 MUST 加 `COLLATE utf8mb4_unicode_ci`，与既有 `DailyReportMapper.xml:527-528` 写法一致。JOIN 条件本身是数值比较（`dept_id` / `user_id`），不涉 collation。

---

## 2. 新增查询字段契约

### 2.1 `projectDeptName` — 机构分组

| 项 | 值 |
|---|---|
| **归属 VO** | `TeamDailyReportVO`（项目层） |
| **Java 类型** | `String` |
| **SQL 来源** | `pd.dept_name COLLATE utf8mb4_unicode_ci`，其中 `LEFT JOIN sys_dept pd ON pd.dept_id = p.project_dept` |
| **可空** | 是。`project_dept` 为 NULL 或指向已删部门时为 `null` |
| **前端降级** | `null` 时该行不渲染（与既有 `v-if="row.confirmAmount"` 的处理一致） |
| **实测分布** | 284 个有效项目：0 空值、0 孤儿；250 个（88%）指向叶子部门，34 个指向非叶子 |
| **边界值** | 非叶子部门显示中间层名（如「项目五组（科技项目外派）」17 个、「项目四组（分行业务组）」14 个、「项目六组（华东地区组）」2 个、「一部」1 个）—— **属预期，不是错误** |

**类型注意**：`pm_project.project_dept` 是 `varchar(100)` 存数字串，`sys_dept.dept_id` 是 `bigint`。JOIN 触发隐式转换，`sys_dept` 为百行量级小表，可接受。

**别名冲突警告**：MUST NOT 命名为 `deptName` —— 该名已被**成员本人部门**占用（`:528`，来自 `INNER JOIN sys_dept d ON d.dept_id = u.dept_id`）。实测 2188 条在册成员行中 **831 条（38%）** 成员部门 ≠ 项目部门，撞名会静默覆盖。

---

### 2.2 `roleLabel` — 角色标签

| 项 | 值 |
|---|---|
| **归属 VO** | `TeamMemberDailyVO`（成员层） |
| **Java 类型** | `String` |
| **SQL 来源** | 查询期 `CASE` 反推，无新 JOIN（`p` 别名已在 FROM） |
| **可空** | 是。四档全不命中时为 `null` |
| **取值域** | `项目经理` \| `市场经理` \| `销售负责人` \| `参与人员` \| `null` |

```sql
CASE
    WHEN p.project_manager_id = u.user_id           THEN '项目经理'
    WHEN p.market_manager_id  = u.user_id           THEN '市场经理'
    WHEN p.sales_manager_id   = u.user_id           THEN '销售负责人'
    WHEN FIND_IN_SET(u.user_id, p.participants) > 0 THEN '参与人员'
    ELSE NULL
END AS roleLabel
```

**实测覆盖率**（在册 2189 行，2026-08-03 快照）：

| 命中角色数 | 行数 | 占比 |
|---|---|---|
| 0 个 | 2 | 0.1%（均为 e2e 造数的系统管理员行 `member_id` 7538/7539） |
| 1 个 | 1,711 | 78.2% |
| 2 个 | 473 | 21.6% |
| 3 个 | 3 | 0.1% |

离场成员 16 行中：7 行可反推、9 行推不出。

**三条不可变约束**：

1. **`team_leader_id` 不入链** —— 实测 284 个有效项目 100% 为 NULL。若未来启用需回头补。
2. **优先级顺序不可调换** —— 多角色的根因是 `ProjectMemberServiceImpl.java:73-79` 把整个成员集合回写 `pm_project.participants`，使经理也进了 participants。**经理排在参与人员之前正是为抵消该副作用**，实现处 MUST 加注释，防止后续被当脏数据「修掉」。
3. **叫「销售负责人」** —— 依据 DB 注释与 `project/edit.vue:285` 的前端 label，不是「销售经理」。

**前端渲染**：`roleLabel` 非空时显示 `昵称（角色）`，为空时只显示 `昵称`（FR-012，**不显示空括号**）。

---

### 2.3 参与时间四字段

| 字段 | 归属 | 类型 | SQL 来源 | 可空 |
|---|---|---|---|---|
| `firstReportDate` | `TeamMemberDailyVO` | `String` (`yyyy-MM-dd`) | `span.firstReportDate` = `MIN(r.report_date)` | 是（从未填报时） |
| `lastReportDate` | `TeamMemberDailyVO` | `String` | `span.lastReportDate` = `MAX(r.report_date)` | 是 |
| `joinDate` | `TeamMemberDailyVO` | `String` | `mb.joinDate` = `MIN(join_date)` | 否（实测在册 2353 行全非空） |
| `leaveDate` | `TeamMemberDailyVO` | `String` | `mb.leaveDate` = `MAX(leave_date)` | 是（在册成员为 NULL） |

**两个 LEFT JOIN 均不受 `yearMonth` 影响** —— 参与时间是全周期属性。

```sql
-- 主源
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

-- 兜底源（不限 del_flag/is_active，与 EXISTS(pm3) 判据对称）
LEFT JOIN (
    SELECT project_id, user_id,
           MIN(join_date) AS joinDate, MAX(leave_date) AS leaveDate
    FROM pm_project_member
    GROUP BY project_id, user_id
) mb ON mb.project_id = p.project_id AND mb.user_id = m.user_id
```

**为什么兜底源必须先 `GROUP BY` 再 JOIN**：`pm_project_member` 历史上缺 `(project_id, user_id)` 唯一约束（`DailyReportMapper.xml:542-543` 注释已记载该问题曾导致工时成倍虚增）。直接 JOIN 会扇出。

**前端展示口径**（决策放在前端，便于将来调整而不动 SQL）：

```
有日报  →  firstReportDate ~ lastReportDate
无日报  →  joinDate ~ (leaveDate || '至今')
两者皆无 →  不渲染该行
```

**已知数据质量问题**（不阻断实现，仅作说明）：

| 现象 | 实测量 | 处理 |
|---|---|---|
| 首次日报早于 `join_date` | 934 对中 383 对（41%） | 主口径已避开 `join_date`，仅作兜底 |
| 首次日报 = `join_date` | 130 对 | — |
| `leave_date` 之后仍有日报 | 2 对（如 `project 17` 游生寿 `leave=2026-03-04`，日报到 `2026-06-24`） | 按日报首末日展示，自然呈现为「参与到 2026-06-24」，不特殊处理 |
| 「离开后加回」的人 `join_date` 停在最早那次 | 未统计 | `ProjectMemberMapper.xml:129-134` 的 `ON DUPLICATE KEY UPDATE` 不更新 `join_date`，中间区间无痕。已知限制 |

---

## 3. 受影响的既有字段

| 字段 | 变化 | 说明 |
|---|---|---|
| `reportDate`（原始行） | **无年月时恒为 `NULL`** | 由 `<choose>` 输出 `NULL AS reportDate`。Java 聚合层 MUST 相应调整（plan.md D2） |
| `totalWorkHours`（原始行） | 无年月时语义从「某人某日某项目工时」变为「某人某项目全周期工时」 | 每个 `(project, user)` 只有一行 |
| `TeamMemberDailyVO.totalHours` | 语义随之从「月累计」变为「全周期累计」（仅在无年月时） | 字段注释需更新为「累计工时（小时）—— 指定年月时为该月，未指定时为全周期」 |
| `TeamMemberDailyVO.dailyHours` | 无年月时恒为空 Map | 前端 `dayColumns` 为空数组，不渲染任何日历格 |
| `TeamDailyReportVO.actualPersonDays` | **不变** | 公式 `ROUND(actual_workload/8,3) + COALESCE(adjust_workload,0)` 恒为全周期，与 `yearMonth` 无关 |
| `TeamDailyReportVO.actualPersonDays` 注释 | 「实际人天」→「项目累计人天」 | `TeamDailyReportVO.java:24`，公式保留 |
| `TeamMemberDailyVO.isFormer` 注释 | 去掉「本月」限定 | 现注释写「**本月**填报了工时」，放宽月窗后不准确 |

---

## 4. 数据量与性能基线

| 形态 | 主查询原始行数 | 备注 |
|---|---|---|
| 现状（2026-07 月窗） | 4,642 | 基线 |
| 无年月 + `daily_hrs` 保留 `report_date` | **16,111** | ❌ 禁止形态。日期 key 达 181 个 |
| 无年月 + `daily_hrs` 退化为 per-(user,project) SUM | **2,199** | ✅ 目标形态（其中 917 行有工时） |

`daily_hrs` 分组基数：全周期 14,874 组 vs 2026-07 单月 2,854 组（5.2x）。

**索引现状**：`pm_daily_report_detail` 仅有 `PRIMARY` / `idx_report_id` / `idx_project_id` / `idx_create_time`，无覆盖 `(report_id, entry_type, del_flag)` 的复合索引。`EXPLAIN` 显示 `daily_hrs` 派生表即便带月窗也是 `rd ALL, rows≈15666` 全表扫（驱动表是 `rd`，过滤条件在 `r.report_date` 上）。去月窗不改变扫描策略，但放大物化行数。**若上线后发现慢，加该复合索引是第一手。**

> 所有数字为**本地 docker 库 2026-08-03 快照**，口径 `del_flag='0'`。本地数据量小，耗时数字对生产不具外推性。
