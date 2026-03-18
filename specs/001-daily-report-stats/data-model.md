# 数据模型：日报统计报表

**特性分支**：`001-daily-report-stats`
**日期**：2026-03-17

---

## 新增 DTO

### DailySubmissionStat（每天统计结果）

```
位置：ruoyi-project/src/main/java/com/ruoyi/project/domain/vo/DailySubmissionStat.java
```

| 字段 | Java 类型 | 说明 |
|------|-----------|------|
| `reportDate` | `String` | 日期（yyyy-MM-dd） |
| `dayOfWeek` | `String` | 星期（周一~周日） |
| `isWorkday` | `Boolean` | 是否工作日 |
| `submittedCount` | `Integer` | 已提交人数 |
| `unsubmittedCount` | `Integer` | 未提交人数（= 总人数 - 已提交） |

---

## 复用领域对象

### DailyReport（查询参数载体）

复用现有 `DailyReport` 实体的以下字段传参：
- `deptId`：部门筛选（已有）
- `yearMonth`：月份（格式 `yyyy-MM`，已有）
- `params.dataScope`：数据权限 SQL（由 `@DataScope` 注入，已有）

---

## 数据来源表

| 表 | 用途 |
|----|------|
| `pm_daily_report` | 已提交记录（按日期分组统计） |
| `pm_daily_report_detail` | 工作内容摘要（明细查询时 GROUP_CONCAT） |
| `sys_user` | 用户总数和未提交人员列表 |
| `sys_dept` | 部门名称关联 |
| `pm_work_calendar` | 节假日/调休工作日判断 |

---

## SQL 查询设计

### Query 1：每天已提交人数

```sql
SELECT r.report_date AS reportDate,
       COUNT(DISTINCT r.user_id) AS submittedCount
FROM pm_daily_report r
INNER JOIN sys_user u ON r.user_id = u.user_id
LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
WHERE r.del_flag = '0'
  AND u.del_flag = '0' AND u.status = '0'
  AND r.report_date BETWEEN #{startDate} AND #{endDate}
  [可选部门过滤: AND (u.dept_id = #{deptId} OR u.dept_id IN (子查询 ancestors))]
  ${params.dataScope}
GROUP BY r.report_date
```

### Query 2：数据权限范围内活跃用户总数

复用 `selectActivityUsers` 的 SQL 结构，增加超管排除条件，只取 COUNT：

```sql
SELECT COUNT(*)
FROM sys_user u
LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
WHERE u.del_flag = '0' AND u.status = '0'
  AND u.user_id != 1  -- 排除超级管理员（2026-03-18 澄清）
  AND u.user_id NOT IN (
      SELECT user_id FROM pm_daily_report_whitelist WHERE del_flag = '0'
  )
  [可选部门过滤]
  ${params.dataScope}
```

### Query 3：某天已提交人员明细

```sql
SELECT u.user_id AS userId,
       u.nick_name AS nickName,
       d.dept_name AS deptName,
       r.total_work_hours AS totalWorkHours,
       LEFT(GROUP_CONCAT(dd.work_content ORDER BY dd.detail_id SEPARATOR '；'), 50) AS workContentSummary
FROM pm_daily_report r
INNER JOIN sys_user u ON r.user_id = u.user_id
LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
LEFT JOIN pm_daily_report_detail dd ON dd.report_id = r.report_id
    AND dd.entry_type = 'work' AND dd.del_flag = '0'
WHERE r.del_flag = '0'
  AND u.del_flag = '0' AND u.status = '0'
  AND r.report_date = #{reportDate}
  [可选部门过滤]
  ${params.dataScope}
GROUP BY r.report_id, u.user_id, u.nick_name, d.dept_name, r.total_work_hours
ORDER BY d.dept_name, u.nick_name
```

### Query 4：某天未提交人员明细

```sql
SELECT u.user_id AS userId,
       u.nick_name AS nickName,
       d.dept_name AS deptName
FROM sys_user u
LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
WHERE u.del_flag = '0' AND u.status = '0'
  AND u.user_id != 1  -- 排除超级管理员
  AND u.user_id NOT IN (
      SELECT user_id FROM pm_daily_report_whitelist WHERE del_flag = '0'
  )
  AND u.user_id NOT IN (
      SELECT r.user_id FROM pm_daily_report r
      WHERE r.report_date = #{reportDate} AND r.del_flag = '0'
  )
  [可选部门过滤: AND (u.dept_id = #{deptId} OR ...)]
  ${params.dataScope}
ORDER BY d.dept_name, u.nick_name
```

---

## 工作日判断逻辑（Java Service 层）

```
查询 pm_work_calendar WHERE calendar_date IN (月份日期范围) AND del_flag = '0'
→ 构建 Map<date, day_type>

对每个日期：
  if (calendarMap.containsKey(date)):
    isWorkday = "workday".equals(calendarMap.get(date))  // workday=调休工作日, holiday=节假日
  else:
    dayOfWeek = date.getDayOfWeek()
    isWorkday = dayOfWeek != SATURDAY && dayOfWeek != SUNDAY
```

---

## API 接口定义

| 接口 | 方法 | URL | 权限 |
|------|------|-----|------|
| 每天汇总统计 | GET | `/project/dailyReport/weeklyStats` | `project:dailyReport:weeklyStats` |
| 人员明细 | GET | `/project/dailyReport/weeklyStatsDetail` | `project:dailyReport:weeklyStats` |
| Excel 导出 | GET | `/project/dailyReport/weeklyStatsExport` | `project:dailyReport:weeklyStatsExport` |

**weeklyStats 参数**：`yearMonth`（必填，yyyy-MM）、`deptId`（可选）

**weeklyStatsDetail 参数**：`reportDate`（必填，yyyy-MM-dd）、`type`（必填，`submitted`/`unsubmitted`）、`deptId`（可选）

**weeklyStatsExport 参数**：同 weeklyStats
