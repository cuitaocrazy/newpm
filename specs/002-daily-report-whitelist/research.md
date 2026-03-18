# 研究报告：日报白名单管理

**特性分支**：`002-daily-report-whitelist`
**日期**：2026-03-17

---

## 决策 1：白名单表软删除策略

**决策**：使用软删除（`del_flag='1'`），与 PM 模块大多数表保持一致

**理由**：
- 软删除保留历史记录，便于审计"谁曾被加入白名单、何时移除"
- `pm_daily_report_whitelist` 不属于宪法中例外的硬删除表（`pm_project`、`pm_task`、`pm_daily_report`、`pm_daily_report_detail`）

**防重复添加**：在 Service 层查询 `del_flag='0'` 的记录，存在则拒绝插入（不用数据库唯一索引，因为软删除会导致历史记录冲突）

---

## 决策 2：白名单排除逻辑的注入点

**决策**：在 `selectActivityUsers` SQL 末尾追加 `AND u.user_id NOT IN (...)` 排除子查询

**影响范围**（一处修改，全局生效）：
- 日报动态（`activity.vue`）：已填/未填统计 ← 直接使用 `selectActivityUsers` 返回的用户列表
- 日报统计报表（001 特性）：`selectTotalUserCount` 和 `selectUnsubmittedUsersOnDate` 均基于相同用户范围，也需要加排除条件

**001 特性的两个 SQL 同样需要修改**：
- `selectTotalUserCount`：加白名单排除
- `selectUnsubmittedUsersOnDate`：加白名单排除
- `selectSubmittedCountByDate`：**不需要修改**（只统计实际提交者，白名单人员不会提交，自然不在里面）

---

## 决策 3：前端拦截策略

**决策**：write.vue 在初始化时调用新接口 `GET /project/whitelist/checkSelf` 检查当前用户是否在白名单中；若在白名单，显示提示卡片并禁用整个表单区域

**备选方案**：在 `getMyProjects()` 响应中附带白名单状态
**放弃原因**：会污染现有 API 的职责；独立端点更清晰，且其他页面将来也可复用

---

## 决策 4：Controller 路由位置

**决策**：`/project/whitelist/**`，放在 `ruoyi-project` 模块

菜单放在系统管理下，但代码仍在 `ruoyi-project` 模块（业务逻辑归属于日报模块，系统管理菜单只是入口位置）

---

## 决策 5：管理员权限绑定

**决策**：使用 `@PreAuthorize("@ss.hasRole('admin')")` 保护所有白名单管理接口

自检接口（`/checkSelf`）使用普通登录权限即可，无需 admin 角色，因为任何登录用户都可能需要查自己是否在白名单中。
