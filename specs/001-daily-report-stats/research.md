# 研究报告：日报统计报表

**特性分支**：`001-daily-report-stats`
**日期**：2026-03-17

---

## 决策 1：工作日判断逻辑

**决策**：三层判断（日历表优先，回退到周几）

**实现逻辑**：
1. 若日期存在于 `pm_work_calendar` 且 `day_type = 'holiday'` → 非工作日
2. 若日期存在于 `pm_work_calendar` 且 `day_type = 'workday'` → 工作日（调休上班）
3. 不在日历表：`DAYOFWEEK` 为 1（周日）或 7（周六）→ 非工作日；其余 → 工作日

**备选方案**：仅按周六/周日判断（不查日历表）
**放弃原因**：法定节假日（如五一、国庆）不是周末，仅按周几无法正确标注；调休上班日（如某些周六）也需标为工作日

---

## 决策 2：按天统计的 SQL 策略

**决策**：两次查询 + Java 层合并

流程：
1. 查询该月日期范围内每天的已提交人数（`GROUP BY report_date`）→ Map<date, count>
2. 查询数据权限范围内的用户总数（复用 `selectActivityUsers` 模式）→ totalCount
3. Java 层生成完整日期列表，填入 submittedCount，计算 unsubmittedCount = total - submitted

**备选方案**：MySQL 递归 CTE 生成日期序列，一条 SQL 完成
**放弃原因**：CTE 方案 SQL 复杂，且总用户数与数据权限相关，两者在一条 SQL 中难以正确结合；分开查询更清晰易维护

---

## 决策 3：Excel 导出方案（2 个 Sheet）

**决策**：直接使用 Apache POI `XSSFWorkbook` 实现双 Sheet 导出

RuoYi 内置的 `ExcelUtil` 只支持单 Sheet，无法满足需求。改用 POI 原生 API：
- Sheet1（汇总表）：日期、星期、是否工作日、已提交人数、未提交人数
- Sheet2（明细表）：日期、姓名、部门、提交状态、工时合计

**备选方案**：仅导出单 Sheet（只有汇总，不含明细）
**放弃原因**：spec 明确要求明细 Sheet，管理人员需要导出完整名单用于跟进

---

## 决策 4：明细查询策略

**已提交明细**：直接 JOIN `pm_daily_report` WHERE `report_date = ?`，包含工时和工作内容摘要（GROUP_CONCAT 前50字）

**未提交明细**：查询用户表，排除已提交用户（NOT IN 子查询），与 `selectActivityUsers` 同样的数据权限过滤

---

## 决策 5：查询参数传递方式

**决策**：复用现有 `DailyReport` 领域对象作为查询参数（已有 `deptId`、`params.dataScope`）；新增两个 transient 字段 `yearMonth`（已存在）和 `startDate`/`endDate`（新增用于内部传递）

不创建新的 Query DTO，保持与现有代码风格一致。

---

## 前端周次计算方案

给定年月（如 2026-03），计算该月的自然周列表（周一为每周第一天）：

```typescript
// 示例：2026年3月
// 第1周: 02-23 ~ 03-01（跨月，包含03月天数）
// 第2周: 03-02 ~ 03-08
// ...
// 周的边界：本月内有天数的周都列出来
```

实现：遍历月份第一天到最后一天，按 ISO 周分组，只保留在本月内有日期的周。
