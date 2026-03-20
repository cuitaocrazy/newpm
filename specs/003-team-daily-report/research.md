# Research: 团队日报

**Branch**: `003-team-daily-report` | **Date**: 2026-03-19

## 可复用的现有代码

### 部门树（三级+过滤）

**Decision**: 直接复用 `GET /project/project/deptTree` + `ProjectDeptSelect` 组件，无需新建接口。

**How it works**:
- 后端 `ProjectServiceImpl.getDeptTree()` 带 `@DataScope(deptAlias="d")`，返回平铺部门列表
- 前端 `ProjectDeptSelect/index.vue` 过滤 `ancestors.split(',').length >= 3`，再调 `handleTree()` 建树
- 三级节点的 `parentId` 在前端被设为 0，使其成为根节点 —— **已经满足 spec 要求**

**Alternatives considered**: 新建 `/project/dailyReport/teamDeptTree` 接口 —— 不必要，已有组件已实现相同逻辑。

---

### 部门子级过滤（find_in_set）

**Decision**: 复用 `DailyReportMapper.xml` 中已有的 `deptFilter` SQL fragment。

```xml
<if test="deptId != null">
    AND (u.dept_id = #{deptId}
         OR u.dept_id IN (SELECT dept_id FROM sys_dept WHERE find_in_set(#{deptId}, ancestors) > 0))
</if>
```

这个模式在 DailyReportMapper、ProjectMapper 中均已验证可用。

---

### 数据权限

**Decision**: Service 层加 `@DataScope(deptAlias="d", userAlias="u")`，Mapper XML 末尾加 `${params.dataScope}`。

已有完整参考：`DailyReportServiceImpl.selectActivityUsers()`。

---

### 项目是否有合同（带收入判断）

**Decision**: EXISTS 子查询，检查 `pm_project_contract_rel` 中是否有 `del_flag='0'` 的记录。

```sql
EXISTS (
  SELECT 1 FROM pm_project_contract_rel pcr
  WHERE pcr.project_id = p.project_id AND pcr.del_flag = '0'
) AS hasContract
```

**Alternatives considered**: 在 Project 实体上加字段 —— 不需要持久化，查询时算即可。

---

### 实际人天计算

**Decision**: `ROUND(p.actual_workload / 8, 3) + COALESCE(p.adjust_workload, 0)`

- `actual_workload` 在 DB 存储单位是**小时**
- `adjust_workload` 单位是**人天**
- 两者单位不同，公式里 `/8` 是换算关键

---

### 项目名称 autocomplete

**Decision**: 新建轻量接口 `GET /project/dailyReport/teamProjectOptions?deptId=&keyword=`，按 `deptId` 范围 + `project_name LIKE` 模糊搜索，返回 `[{projectId, projectName}]`，最多 20 条。

在 `pm_project_member` 关联 `pm_project` 上加部门过滤，避免跨部门数据泄露。

---

### 月度日报数据聚合

**Decision**: 新建 `GET /project/dailyReport/teamMonthly` 接口，返回按「项目+人员」分组、按日期展开的工时矩阵。

**数据结构**（后端返回，前端直接渲染日历卡）：

```json
[
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
        "dailyHours": { "2026-03-01": 8.0, "2026-03-03": 6.0 },
        "totalHours": 14.0
      }
    ]
  }
]
```

**Rationale**: 把聚合逻辑放后端，前端只做渲染。如果前端拉原始日报列表再聚合，数据量大时会很慢。

---

## 技术风险与注意事项

| 风险 | 缓解措施 |
|------|---------|
| 字符集冲突（PM表 vs sys表） | JOIN sys_user/sys_dept 时加 `COLLATE utf8mb4_unicode_ci` |
| 月份数据量大（全部门全月） | 限制查询范围：必须选部门才能查；后端分页或限制项目数 |
| 预算人天为0/NULL时的除零或误判 | 前端：`estimatedWorkload` 为空时不显示对比列；后端不参与计算 |
| `deptTree` 返回所有部门 | 前端过滤已有，直接复用 `ProjectDeptSelect` 组件 |
