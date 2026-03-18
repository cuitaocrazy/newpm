# API 契约：日报白名单管理

**特性分支**：`002-daily-report-whitelist`
**日期**：2026-03-17

---

## GET /project/whitelist/list

**权限**：admin 角色

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | string | ❌ | 姓名或部门关键词 |
| `pageNum` | number | ❌ | 页码（默认1） |
| `pageSize` | number | ❌ | 每页条数（默认10） |

**响应**：`TableDataInfo`

```json
{
  "code": 200,
  "total": 3,
  "rows": [
    {
      "id": 1,
      "userId": 100,
      "nickName": "张三",
      "deptName": "外派团队",
      "reason": "长期外派项目，无需填写日报",
      "createBy": "admin",
      "createTime": "2026-03-17 10:00:00"
    }
  ]
}
```

---

## POST /project/whitelist

**权限**：admin 角色

**请求体**：

```json
{
  "userId": 100,
  "reason": "长期外派项目，无需填写日报"
}
```

**响应**：`AjaxResult`，成功 `code: 200`；重复添加返回 `code: 500`，`msg: "该人员已在白名单中，请勿重复添加"`

---

## DELETE /project/whitelist/{id}

**权限**：admin 角色

**路径参数**：`id` — 白名单记录ID

**响应**：`AjaxResult`，成功 `code: 200`

---

## GET /project/whitelist/checkSelf

**权限**：登录即可（无需特殊权限）

**响应**：

```json
{ "code": 200, "data": true }   // true = 当前用户在白名单中
{ "code": 200, "data": false }  // false = 当前用户不在白名单中
```
