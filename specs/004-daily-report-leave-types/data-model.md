# Data Model: 日报假期模块扩展

**Feature**: 004-daily-report-leave-types
**Date**: 2026-03-20

## 变更概述

本功能**不新增任何数据库表**，所有变更通过以下方式实现：
1. 字典数据新增（`sys_dict_data`）
2. 新增后端 DTO 类（`BatchLeaveRequest`）

---

## 1. 字典数据变更（sys_rbtype）

### 新增 3 条字典记录

| dict_code | dict_sort | dict_label | dict_value | dict_type   | list_class | 说明           |
|-----------|-----------|------------|------------|-------------|------------|----------------|
| 384       | 5         | 婚假        | marriage   | sys_rbtype  | primary    | Element tag 蓝 |
| 385       | 6         | 产假        | maternity  | sys_rbtype  | warning    | Element tag 橙 |
| 386       | 7         | 丧假        | bereavement| sys_rbtype  | info       | Element tag 灰 |

### 完整 sys_rbtype 字典（变更后）

| dict_value   | dict_label | list_class | 用途         |
|--------------|------------|------------|--------------|
| work         | 项目工时   | primary    | 工时条目（不显示在假期选项）|
| leave        | 请假       | danger     | 普通请假     |
| comp         | 倒休       | warning    | 调休         |
| annual       | 年假       | success    | 年假         |
| marriage     | 婚假       | primary    | 新增         |
| maternity    | 产假       | warning    | 新增         |
| bereavement  | 丧假       | info       | 新增         |

**颜色映射**（前端 computed）：
```
primary  → #409eff （蓝）
success  → #67c23a （绿）
warning  → #e6a23c （橙）
danger   → #f56c6c （红）
info     → #909399 （灰）
```

> 注意：`marriage` 与 `work` 同为 `primary`，但在日历角标/颜色圆点场景中只有假期类型（非 work）会渲染颜色，不会混淆。如需区分可将 `marriage` 改为 `info`。

---

## 2. 新增 DTO：BatchLeaveRequest

### Java DTO（ruoyi-project 模块）

```java
// com.ruoyi.project.domain.dto.BatchLeaveRequest
public class BatchLeaveRequest {
    /** 假期类型（sys_rbtype 字典值，非 work） */
    @NotBlank
    private String entryType;

    /** 日期范围开始（inclusive）yyyy-MM-dd */
    @NotNull
    private String startDate;

    /** 日期范围结束（inclusive）yyyy-MM-dd */
    @NotNull
    private String endDate;

    /** 每日假期时长（小时），默认 8 */
    private BigDecimal leaveHoursPerDay = BigDecimal.valueOf(8);

    /** 冲突处理：skip（跳过，默认）| overwrite（覆盖） */
    private String conflictStrategy = "skip";
}
```

### 响应体（内嵌在 AjaxResult.data 中）

```json
{
  "totalWorkdays": 5,
  "created": 4,
  "skipped": 1,
  "overwritten": 0
}
```

| 字段          | 说明                                  |
|---------------|---------------------------------------|
| totalWorkdays | 范围内识别到的工作日总数（去掉周末/节假日）|
| created       | 新建假期记录的天数                    |
| skipped       | 因冲突跳过的天数                      |
| overwritten   | 因冲突覆盖的天数                      |

---

## 3. 现有实体（无结构变更）

### pm_daily_report_detail（复用现有字段）

| 字段        | 说明                                                |
|-------------|-----------------------------------------------------|
| entry_type  | 新增 marriage / maternity / bereavement 均存入此字段 |
| leave_hours | 批量填写时存入每日时长                              |
| work_hours  | 与 leave_hours 相同值（现有 saveDailyReport 约定）   |
| project_id  | 假期行为 null                                        |
| remark      | 批量填写时为空字符串                                |

### leaveSummary 聚合（自动兼容）

现有 `DailyReportMapper.xml` 中 `leaveSummary` 子查询：

```sql
SELECT GROUP_CONCAT(CONCAT(dd_l.entry_type, ':', dd_l.leave_hours) SEPARATOR ',')
FROM pm_daily_report_detail dd_l
WHERE dd_l.report_id = r.report_id AND dd_l.entry_type != 'work'
```

新类型 `marriage`/`maternity`/`bereavement` 自动被此查询包含，**无需修改 SQL**。

---

## 4. 工作日历（pm_work_calendar）

批量填写后端逻辑查询此表，无结构变更：

| 字段          | 用途                                        |
|---------------|---------------------------------------------|
| calendar_date | 日期（yyyy-MM-dd）                          |
| day_type      | `holiday`=法定节假日跳过；`workday`=调班工作日不跳过 |
