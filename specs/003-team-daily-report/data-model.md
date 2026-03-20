# Data Model: 团队日报

**Branch**: `003-team-daily-report` | **Date**: 2026-03-19

## 涉及的现有表（只读）

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `pm_daily_report` | 日报主表 | `report_id`, `report_date`, `user_id`, `dept_id`, `total_work_hours` |
| `pm_daily_report_detail` | 日报明细 | `report_id`, `project_id`, `work_hours`, `entry_type`('work'/'leave'/'comp'/'annual') |
| `pm_project` | 项目 | `project_id`, `project_name`, `project_dept`, `estimated_workload`(人天), `actual_workload`(小时!), `adjust_workload`(人天) |
| `pm_project_member` | 项目成员 | `project_id`, `user_id`, `is_active` |
| `pm_project_contract_rel` | 项目合同关联 | `project_id`, `contract_id`, `del_flag` |
| `sys_user` | 用户 | `user_id`, `nick_name`, `dept_id` |
| `sys_dept` | 部门 | `dept_id`, `dept_name`, `parent_id`, `ancestors` |

**无新建表**，本功能纯查询，不写入任何数据。

---

## 查询逻辑

### 核心 SQL 草稿（teamMonthly）

```sql
SELECT
    p.project_id,
    p.project_name,
    p.project_dept,
    p.estimated_workload,
    ROUND(p.actual_workload / 8, 3) + COALESCE(p.adjust_workload, 0) AS actualPersonDays,
    EXISTS(
        SELECT 1 FROM pm_project_contract_rel pcr
        WHERE pcr.project_id = p.project_id AND pcr.del_flag = '0'
    ) AS hasContract,
    u.user_id,
    u.nick_name COLLATE utf8mb4_unicode_ci AS nickName,
    d.dept_name COLLATE utf8mb4_unicode_ci AS deptName,
    r.report_date,
    r.total_work_hours
FROM pm_project p
INNER JOIN pm_project_member m ON m.project_id = p.project_id
    AND m.is_active = '1' AND m.del_flag = '0'
INNER JOIN sys_user u ON u.user_id = m.user_id
INNER JOIN sys_dept d ON d.dept_id = u.dept_id
LEFT JOIN pm_daily_report r ON r.user_id = m.user_id
    AND DATE_FORMAT(r.report_date, '%Y-%m') = #{yearMonth}
WHERE p.del_flag = '0'
  AND p.project_dept = #{deptId}  -- 或 find_in_set 子部门扩展
  <if test="projectId != null">AND p.project_id = #{projectId}</if>
  ${params.dataScope}
ORDER BY p.project_id, u.user_id, r.report_date
```

**注意**：后端 Java 代码将以上结果聚合为嵌套 VO（按项目 → 成员 → 日期工时）再返回。

---

## VO 设计（后端返回）

### TeamDailyReportVO（顶层列表元素）

```java
public class TeamDailyReportVO {
    private Long projectId;
    private String projectName;
    private Boolean hasContract;          // true=有合同=带来收入
    private BigDecimal estimatedWorkload; // 预算人天（estimated_workload字段，单位已是人天）
    private BigDecimal actualPersonDays;  // 实际人天 = actual_workload/8 + adjust_workload
    private List<TeamMemberDailyVO> members;
}
```

### TeamMemberDailyVO（成员行）

```java
public class TeamMemberDailyVO {
    private Long userId;
    private String nickName;
    private String deptName;
    private Map<String, BigDecimal> dailyHours; // key: "2026-03-01", value: 工时(h)
    private BigDecimal totalHours;              // 月累计工时
}
```

### 查询参数 VO

```java
public class TeamDailyReportQuery extends BaseEntity {
    private String yearMonth;   // "2026-03"
    private Long deptId;        // 三级部门ID
    private Long projectId;     // autocomplete选中
    private String projectName; // autocomplete关键词（用于 teamProjectOptions 接口）
}
```

---

## 人天预警规则

```
若 estimatedWorkload != null && estimatedWorkload > 0:
    若 actualPersonDays > estimatedWorkload * 0.5 → 标红预警
    否则 → 正常显示
若 estimatedWorkload == null || estimatedWorkload == 0:
    不显示对比，隐藏预算列或显示 "—"
```

---

## 部门树层级过滤规则

```
ancestors 字段格式：如 "0,100,101" 表示路径 root→100→101
层级深度 = ancestors.split(',').length - 1

一级部门：depth=1，ancestors="0"
二级部门：depth=2，ancestors="0,100"
三级部门：depth=3，ancestors="0,100,101"  ← 作为树根（parentId=0）
四级部门：depth=4，ancestors="0,100,101,102" ← 挂在三级下

过滤条件：depth >= 3，即 LENGTH(ancestors) - LENGTH(REPLACE(ancestors, ',', '')) >= 2
```
