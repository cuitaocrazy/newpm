# 实现方案：日报统计报表

**Branch**: `001-daily-report-stats` | **Date**: 2026-03-18 | **Spec**: [spec.md](./spec.md)

---

## Summary

在日报管理菜单下新增"日报统计报表"页面，按月展示每天的日报提交/未提交人数（按自然周双列卡片分组），支持部门筛选、周次快速定位、人员明细弹框和 Excel 导出。

**核心变更（2026-03-18 澄清）**：
1. 活跃用户口径排除超级管理员（`user_id=1`）和白名单；页面顶部展示"统计范围：N 人"随部门筛选联动
2. 已提交明细的工时列：`< 8h` → 单元格淡红色背景（`#ffecec`）；`≥ 8h` → 默认样式

---

## Technical Context

**Language/Version**: Java 17 / TypeScript 5.6
**Primary Dependencies**: Spring Boot 3.5.8, MyBatis, Apache POI (Excel), Vue 3.5, Element Plus 2.13, dayjs
**Storage**: MySQL 8.x (`ry-vue`)，涉及表：`pm_daily_report`、`pm_daily_report_detail`、`sys_user`、`sys_dept`、`pm_work_calendar`、`pm_daily_report_whitelist`
**Testing**: Playwright E2E（项目已有 `tests/` 目录）
**Target Platform**: Spring Boot 单体应用 + Vite SPA
**Performance Goals**: 整月数据（31天）渲染 < 3秒（SC-003）
**Constraints**: 遵循数据权限（@DataScope），部门过滤用 ancestors 层级查询
**Scale/Scope**: 约 150 名活跃用户，每月 ~31 天，明细 < 200 条

---

## Constitution Check

| 原则 | 检查项 | 状态 |
|------|--------|------|
| §I 业务完整性 | 此功能为只读统计，无状态变更，无需状态机 | ✅ Pass |
| §I 审计日志 | GET 接口无需 `@Log`；导出接口加 `@Log(businessType=EXPORT)` | ✅ Pass |
| §II 权限控制 | `@PreAuthorize("@ss.hasPermi('...')")` 必须加在所有 Controller 方法 | ✅ 计划中 |
| §II 数据权限 | `@DataScope(deptAlias="d", userAlias="u")` + Mapper `${params.dataScope}` | ✅ 计划中 |
| §II 代理 API | 前端部门树使用 `/project/dailyReport/weeklyStatsDeptTree`，不调用 `system/dept` | ✅ Pass |
| §III Controller 结构 | 继承 `BaseController`，统计接口返回 `AjaxResult` | ✅ 计划中 |
| §IV 任务/项目解耦 | 本功能无涉及 `pm_project` / `pm_task` 字段变更 | ✅ Pass |
| §V 字符集 | `pm_daily_report` (0900_ai_ci) JOIN `sys_dept` (unicode_ci) 需加 COLLATE；Mapper deptFilter 已处理 | ✅ 计划中 |
| §V 部门过滤 | 使用 `find_in_set(deptId, ancestors)` 模式 | ✅ 计划中 |
| §VI 前端组件 | 部门选择器使用 `<project-dept-select />`，不直接调用 `system/dept` | ✅ 计划中 |

---

## Project Structure

### Documentation (this feature)

```text
specs/001-daily-report-stats/
├── plan.md              ← 本文件
├── research.md          ← Phase 0 研究结论
├── data-model.md        ← Phase 1 数据模型
├── contracts/api.md     ← Phase 1 API 契约
└── tasks.md             ← Phase 2 任务列表（/speckit.tasks 生成）
```

### Source Code

```text
ruoyi-project/src/main/java/com/ruoyi/project/
├── controller/
│   └── DailyReportController.java         ← weeklyStats/weeklyStatsDetail/weeklyStatsDeptTree/weeklyStatsExport
├── service/
│   ├── IDailyReportService.java           ← 接口方法签名
│   └── impl/DailyReportServiceImpl.java   ← 统计逻辑实现
└── domain/vo/
    └── DailySubmissionStat.java           ← 每天统计结果 VO

ruoyi-project/src/main/resources/mapper/project/
└── DailyReportMapper.xml                  ← selectTotalUserCount / selectDailySubmittedCount
                                             / selectSubmittedDetail / selectUnsubmittedDetail
                                             / selectStatsDeptTree

ruoyi-ui/src/views/project/dailyReport/
└── weeklyStats.vue                        ← 统计报表页面

ruoyi-ui/src/api/project/
└── dailyReport.js                         ← getWeeklyStats / getWeeklyStatsDetail
                                             / getWeeklyStatsDeptTree / exportWeeklyStats

pm-sql/init/
└── 02_menu_data.sql                       ← 日报统计报表菜单和权限按钮
```

**Structure Decision**: 后端 Java 业务代码全部在 `ruoyi-project`（宪法 §开发工作流），前端在 `ruoyi-ui/src/views/project/dailyReport/`，与日报现有目录一致。

---

## 实现策略

### 后端核心逻辑（DailyReportServiceImpl）

```
selectWeeklyStats(yearMonth, deptId, dataScope):
  1. 解析 yearMonth → startDate, endDate
  2. 查 pm_work_calendar (年份) → calendarMap<date, dayType>
  3. selectDailySubmittedCount(startDate, endDate, ...) → Map<date, submittedCount>
  4. selectTotalUserCount(deptId, dataScope) → totalUsers
  5. 遍历 [startDate..endDate]，生成 DailySubmissionStat 列表：
     - isFuture = date > today → submittedCount=null, unsubmittedCount=null
     - isWorkday from calendarMap（fallback 周几）
     - !isWorkday → unsubmittedCount=null（"非工作日"）
     - isWorkday && !isFuture → unsubmittedCount = totalUsers - submittedCount
  6. 返回 { totalUsers, list }
```

### 活跃用户 SQL 规则（2026-03-18 澄清）

```sql
-- selectTotalUserCount / selectUnsubmittedDetail / selectDailySubmittedCount
u.del_flag = '0'
AND u.status = '0'
AND u.user_id != 1   ← 排除超级管理员
AND u.user_id NOT IN (SELECT user_id FROM pm_daily_report_whitelist WHERE del_flag = '0')
${params.dataScope}
[可选部门过滤 deptFilter]
```

### 前端展示（weeklyStats.vue）

- 月份选择器（默认当月）+ 部门筛选器（`<project-dept-select />`）
- 顶部展示"统计范围：**N 人**"（从 `res.data.totalUsers` 取值，随搜索联动）
- 双列卡片网格，卡片倒序（最新周在前），当前自然周橙色边框高亮
- 非工作日行：灰色背景，未提交列显示"非工作日"
- 未来日期行：灰色"—"不可点击
- 人数 > 0 时为蓝色链接，点击打开明细 Dialog

### 已提交明细工时单元格样式（FR-008，2026-03-18 澄清）

```vue
<!-- el-table 中工时列使用 :cell-style 回调 -->
<el-table-column label="工时（h）" prop="totalWorkHours">
  <template #default="{ row }">
    <span :style="row.totalWorkHours < 8 ? { backgroundColor: '#ffecec', display: 'block', padding: '4px 8px', borderRadius: '4px' } : {}">
      {{ row.totalWorkHours }}
    </span>
  </template>
</el-table-column>
```

规则：
- `totalWorkHours < 8` → 单元格背景色 `#ffecec`（淡红色）
- `totalWorkHours >= 8` → 默认样式（无特殊颜色）

---

## 关键风险

| 风险 | 缓解 |
|------|------|
| 字符集冲突（`pm_daily_report` 0900 vs `sys_dept` unicode） | Mapper deptFilter 已加 COLLATE；新增 SQL 需同样处理 |
| 超管 user_id=1 排除漏加 | selectTotalUserCount、selectUnsubmittedDetail、selectDailySubmittedCount 三处均需加条件 |
| weeklyStats 响应结构变更（data 从 array 变为 {totalUsers, list}） | 前端取值 `res.data.list`，`res.data.totalUsers` 独立渲染 |
| 工时背景色在深色主题下可读性差 | `#ffecec` 为浅色，当前系统无深色主题，风险低 |
