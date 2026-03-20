# API Contracts: 日报假期模块扩展

**Feature**: 004-daily-report-leave-types
**Date**: 2026-03-20

## 新增接口

### POST /project/dailyReport/batchLeave

批量填写假期，为指定日期范围内的每个工作日生成假期记录。

**权限**: `project:dailyReport:add` 或 `project:dailyReport:edit`（与现有保存接口相同）

**请求**

```json
{
  "entryType": "marriage",
  "startDate": "2026-03-23",
  "endDate": "2026-04-04",
  "leaveHoursPerDay": 8,
  "conflictStrategy": "skip"
}
```

| 字段              | 类型    | 必填 | 说明                                      |
|-------------------|---------|------|-------------------------------------------|
| entryType         | string  | 是   | 假期类型（sys_rbtype 字典值，不能为 work） |
| startDate         | string  | 是   | 开始日期 yyyy-MM-dd（含）                  |
| endDate           | string  | 是   | 结束日期 yyyy-MM-dd（含）                  |
| leaveHoursPerDay  | number  | 否   | 每日时长（小时），默认 8，范围 0.5~24      |
| conflictStrategy  | string  | 否   | 冲突策略：`skip`（默认）或 `overwrite`    |

**响应 — 成功**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "totalWorkdays": 10,
    "created": 9,
    "skipped": 1,
    "overwritten": 0
  }
}
```

**响应 — 校验失败**

```json
{
  "code": 500,
  "msg": "日期范围不合法：startDate 不能晚于 endDate"
}
```

```json
{
  "code": 500,
  "msg": "假期类型不合法：work 不能作为假期类型"
}
```

```json
{
  "code": 500,
  "msg": "所选范围内无工作日，未生成任何记录"
}
```

**业务规则**

1. 仅当前登录用户的假期记录；不支持代填他人。
2. 跳过周末（周六、周日），除非工作日历标记为 `dayType='workday'`（调班）。
3. 跳过工作日历中 `dayType='holiday'` 的日期。
4. 冲突判断：同用户、同日期、同 `entryType` 的记录已存在 → 按 `conflictStrategy` 处理。
5. 不受「仅限本周」限制，支持历史和未来日期。
6. 批量操作不影响已有工时记录（work 条目）。

---

## 无变更接口（复用）

| 接口                              | 说明                            |
|-----------------------------------|---------------------------------|
| `POST /project/dailyReport`       | 单日保存（包含新假期类型，自动兼容）|
| `GET /project/dailyReport/my/{date}` | 查询单日日报（新类型自动出现）  |
| `GET /project/dailyReport/list`   | 日报列表（leaveSummary 自动包含新类型）|
| `GET /project/workCalendar/year/{year}` | 工作日历（批量填写用，无需改动）|

---

## 前端新增 API 函数（dailyReport.js）

```javascript
// 批量填写假期
export function batchSaveLeave(data) {
  return request({
    url: '/project/dailyReport/batchLeave',
    method: 'post',
    data,
    headers: { repeatSubmit: false }
  })
}
```
