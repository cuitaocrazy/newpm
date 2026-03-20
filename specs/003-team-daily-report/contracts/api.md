# API Contracts: 团队日报

**Branch**: `003-team-daily-report` | **Date**: 2026-03-19

---

## 新增接口

### 1. GET /project/dailyReport/teamMonthly

**描述**: 按部门+年月查询团队日报（按项目+成员聚合）

**权限**: `project:dailyReport:teamList`

**Query Params**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| yearMonth | String | ✓ | 格式 "2026-03" |
| deptId | Long | ✓ | 三级部门ID，后端自动包含子部门 |
| projectId | Long | - | 精确过滤某个项目 |

**Response** `200 AjaxResult`:

```json
{
  "code": 200,
  "data": [
    {
      "projectId": 1,
      "projectName": "项目A",
      "hasContract": true,
      "estimatedWorkload": 10.0,
      "actualPersonDays": 2.75,
      "members": [
        {
          "userId": 101,
          "nickName": "张三",
          "deptName": "研发部",
          "dailyHours": {
            "2026-03-01": 8.0,
            "2026-03-03": 6.0
          },
          "totalHours": 14.0
        }
      ]
    }
  ]
}
```

**数据权限**: `@DataScope(deptAlias="d", userAlias="u")`

---

### 2. GET /project/dailyReport/teamProjectOptions

**描述**: 项目名称 autocomplete，按部门范围模糊搜索项目

**权限**: `project:dailyReport:teamList`

**Query Params**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deptId | Long | - | 限制部门范围（含子部门） |
| keyword | String | - | 项目名称关键词 |

**Response** `200 AjaxResult`:

```json
{
  "code": 200,
  "data": [
    { "projectId": 1, "projectName": "项目A" },
    { "projectId": 2, "projectName": "项目B" }
  ]
}
```

最多返回 20 条，前端防抖 300ms。

---

## 复用的现有接口

| 接口 | 用途 |
|------|------|
| `GET /project/project/deptTree` | 部门树（已有 `ProjectDeptSelect` 组件封装） |

---

## 菜单 SQL

```sql
-- 在「日报管理」下新增「团队日报」二级菜单（第5个）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (
  '团队日报',
  (SELECT menu_id FROM sys_menu WHERE menu_name = '日报管理' AND parent_id = 0 LIMIT 1),
  5,
  'teamReport',
  'project/dailyReport/teamReport',
  1, 0, 'C', '0', '0',
  'project:dailyReport:teamList',
  'date',
  'admin', NOW(), '', NULL, ''
);

-- 查询权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '查询', menu_id, 1, '#', '#', 1, 0, 'F', '0', '0', 'project:dailyReport:teamList', '#', 'admin', NOW()
FROM sys_menu WHERE menu_name = '团队日报' AND menu_type = 'C';
```
