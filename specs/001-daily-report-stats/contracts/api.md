# API 契约：日报统计报表

**特性分支**：`001-daily-report-stats`
**日期**：2026-03-17

---

## GET /project/dailyReport/weeklyStats

**权限**：`project:dailyReport:weeklyStats`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `yearMonth` | string | ✅ | 月份，格式 `yyyy-MM`（如 `2026-03`） |
| `deptId` | number | ❌ | 部门ID，不传则按数据权限范围 |

**响应**：`AjaxResult`，`data` 为对象（含 `totalUsers` + `list`）

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "totalUsers": 148,
    "list": [
      {
        "reportDate": "2026-03-09",
        "dayOfWeek": "周一",
        "isWorkday": true,
        "submittedCount": 18,
        "unsubmittedCount": 130
      },
      {
        "reportDate": "2026-03-15",
        "dayOfWeek": "周日",
        "isWorkday": false,
        "submittedCount": 3,
        "unsubmittedCount": null
      }
    ]
  }
}
```

> `totalUsers`：统计范围活跃用户总数（排除超级管理员 user_id=1 和白名单，随部门筛选联动）
> 非工作日的 `unsubmittedCount` 为 `null`（前端显示"非工作日"）
> 未来日期的 `submittedCount` / `unsubmittedCount` 均为 `null`（前端显示灰色 `—`）

---

## GET /project/dailyReport/weeklyStatsDetail

**权限**：`project:dailyReport:weeklyStats`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reportDate` | string | ✅ | 日期，格式 `yyyy-MM-dd` |
| `type` | string | ✅ | `submitted`（已提交）/ `unsubmitted`（未提交） |
| `deptId` | number | ❌ | 部门ID |

**响应（已提交）**：

```json
{
  "code": 200,
  "data": [
    {
      "userId": 101,
      "nickName": "张三",
      "deptName": "研发部",
      "totalWorkHours": 8.0,
      "workContentSummary": "完成用户模块接口开发；代码审查"
    }
  ]
}
```

**响应（未提交）**：

```json
{
  "code": 200,
  "data": [
    {
      "userId": 102,
      "nickName": "李四",
      "deptName": "测试部"
    }
  ]
}
```

---

## GET /project/dailyReport/weeklyStatsExport

**权限**：`project:dailyReport:weeklyStatsExport`

**请求参数**：同 `/weeklyStats`

**响应**：Excel 文件（`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`）

**文件名**：`日报统计报表_2026-03.xlsx`

**Sheet 1 - 汇总表**：

| 日期 | 星期 | 是否工作日 | 已提交人数 | 未提交人数 |
|------|------|-----------|-----------|-----------|
| 2026-03-09 | 周一 | 是 | 18 | 2 |
| 2026-03-15 | 周日 | 否 | 3 | 17 |

**Sheet 2 - 明细表**：

| 日期 | 姓名 | 部门 | 提交状态 | 工时合计 |
|------|------|------|---------|---------|
| 2026-03-09 | 张三 | 研发部 | 已提交 | 8.0 |
| 2026-03-09 | 李四 | 测试部 | 未提交 | - |
