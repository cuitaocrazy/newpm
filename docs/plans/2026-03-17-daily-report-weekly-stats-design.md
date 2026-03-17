# 日报统计报表 — 设计文档

## 需求概述

在日报管理一级菜单下新增"日报统计报表"二级菜单，支持按月/周查看每天已提交和未提交日报的人员情况，并支持查看明细和导出。

## 功能定义

- **统计维度**：按天（每天一行），按周分组展示
- **已提交**：当天在数据权限范围内已提交日报的用户数
- **未提交**：当天在数据权限范围内未提交日报的用户数（= 总用户数 - 已提交数）
- **非工作日**：周末/节假日仍展示，但"未提交"标注为"(非工作日)"并灰化行显示

## 查询条件

| 字段 | 类型 | 说明 |
|------|------|------|
| 月份 | 月份选择器 | 默认当月 |
| 第几周 | 下拉（动态生成） | 全部 / 第N周(起止日期)；根据月份动态计算 |
| 部门 | 部门树选择器 | 可选，不选则按数据权限范围 |

## 页面布局

```
┌────────────────────────────────────────────────────────┐
│  月份: [2026年3月 ▼]  第几周: [全部 ▼]  部门: [全部 ▼]  │
│  [查询]  [重置]                            [导出 Excel] │
└────────────────────────────────────────────────────────┘

表格（按天展示，按周分组）：

▶ 第1周  2026-03-02 ~ 2026-03-08
┌──────────┬─────┬────────────┬────────────────────────────┐
│ 日期     │ 星期│ 已提交     │ 未提交                     │
├──────────┼─────┼────────────┼────────────────────────────┤
│ 03-02    │ 周一│ 18人 [链接]│ 2人 [链接]                 │
│ 03-07    │ 周六│ 3人 [链接] │ 17人 (非工作日) [灰色行]   │
└──────────┴─────┴────────────┴────────────────────────────┘

▶ 第2周  2026-03-09 ~ 2026-03-15
...
```

## 明细弹框

点击"已提交 N人"：
| 姓名 | 部门 | 工时合计 | 工作内容摘要 |
|------|------|----------|------------|

点击"未提交 N人"：
| 姓名 | 部门 |
|------|------|

## 导出

- 导出当前查询条件下所有数据（含明细展开）
- Excel 按周分组，每天一行，包含：日期、星期、已提交人数、未提交人数
- 明细 sheet：日期、姓名、部门、是否已提交、工时合计

---

## 后端设计

### 新增 API

#### 1. 按天统计汇总
```
GET /project/dailyReport/weeklyStats
权限: project:dailyReport:weeklyStats
```

参数：
- `yearMonth` — 格式 `yyyy-MM`（必填）
- `weekNum` — 第几周，0 = 全部（可选）
- `deptId` — 部门 ID（可选）

返回：
```json
[
  {
    "reportDate": "2026-03-09",
    "dayOfWeek": "周一",
    "isWorkday": true,
    "submittedCount": 18,
    "unsubmittedCount": 2
  }
]
```

SQL 思路：
1. 获取数据权限范围内的活跃用户总数 N
2. 对日期范围内每天 `COUNT(DISTINCT user_id)` from `pm_daily_report`
3. `unsubmittedCount = N - submittedCount`
4. 通过 `pm_work_calendar` 判断 `isWorkday`

#### 2. 明细查询
```
GET /project/dailyReport/weeklyStatsDetail
权限: project:dailyReport:weeklyStats
```

参数：
- `reportDate` — 日期（必填）
- `type` — `submitted` / `unsubmitted`
- `deptId` — 部门 ID（可选）

返回：
```json
[
  {
    "userId": 1,
    "nickName": "张三",
    "deptName": "研发部",
    "totalWorkHours": 8.0,          // 仅 submitted 有值
    "workContentSummary": "..."      // 仅 submitted 有值，截取前50字
  }
]
```

#### 3. 导出
```
GET /project/dailyReport/weeklyStatsExport
权限: project:dailyReport:weeklyStats
```

参数同 weeklyStats，返回 Excel 文件。

### 数据权限

所有新接口均使用 `@DataScope(deptAlias = "d", userAlias = "u")`，与现有 `selectActivityUsers` 保持一致。

---

## 前端设计

### 新增文件
- `ruoyi-ui/src/views/project/dailyReport/weeklyStats.vue`
- `ruoyi-ui/src/api/project/dailyReport.js` — 追加 3 个新 API 函数

### 周次计算逻辑（前端）

根据选中月份，计算该月所有完整自然周（周一为第一天），生成下拉选项：
```
全部 / 第1周(03-02~03-08) / 第2周(03-09~03-15) / ...
```

### 数据结构（前端）

将后端返回的按天数组，在前端按周分组：
```typescript
interface WeekGroup {
  weekNum: number
  startDate: string
  endDate: string
  days: DailySubmissionStat[]
}
```

---

## 菜单 SQL

在日报管理菜单下新增：
- 菜单名：日报统计报表
- 路由：`weeklyStats`
- 组件：`project/dailyReport/weeklyStats`
- 权限：`project:dailyReport:weeklyStats`
- 图标：`bar-chart`
- 顺序：6（排在项目人天统计之后）

按钮权限：
- `project:dailyReport:weeklyStats` — 查询
- `project:dailyReport:weeklyStatsExport` — 导出

---

## 实现任务拆解

1. **后端** — `DailyReport` 实体扩展 + Mapper SQL（weeklyStats 查询）
2. **后端** — 新增 Service 方法（weeklyStats、weeklyStatsDetail）
3. **后端** — Controller 新增 3 个端点 + Excel 导出
4. **前端** — `weeklyStats.vue` 主页面（查询条件 + 分组表格）
5. **前端** — 明细 Dialog 组件
6. **前端** — 导出功能
7. **SQL** — 菜单数据 + 权限数据
