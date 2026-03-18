# 数据模型：日报白名单管理

**特性分支**：`002-daily-report-whitelist`
**日期**：2026-03-17

---

## 新增表：pm_daily_report_whitelist

```sql
CREATE TABLE `pm_daily_report_whitelist` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL                COMMENT '白名单用户ID（FK: sys_user.user_id）',
  `reason`      VARCHAR(500) NOT NULL                COMMENT '加入原因（必填）',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志（0正常 1删除）',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建者',
  `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新者',
  `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='日报填写白名单（免填日报人员名单）';
```

---

## 新增实体：DailyReportWhitelist

```
位置：ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReportWhitelist.java
继承：BaseEntity（含 createBy/createTime/updateBy/updateTime/remark）
```

| 字段 | Java类型 | 数据库列 | 说明 |
|------|---------|---------|------|
| `id` | `Long` | `id` | 主键 |
| `userId` | `Long` | `user_id` | 白名单用户ID |
| `reason` | `String` | `reason` | 加入原因 |
| `delFlag` | `String` | `del_flag` | 软删除标志 |
| `nickName` | `String` | — | 显示字段，JOIN sys_user |
| `deptName` | `String` | — | 显示字段，JOIN sys_dept |

---

## SQL 查询设计

### Query 1：白名单列表（含用户信息）

```sql
SELECT w.id, w.user_id, u.nick_name AS nickName,
       d.dept_name AS deptName,
       w.reason, w.create_by, w.create_time
FROM pm_daily_report_whitelist w
INNER JOIN sys_user u ON w.user_id = u.user_id
LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
WHERE w.del_flag = '0'
  [可选: AND (u.nick_name LIKE #{keyword} OR d.dept_name LIKE #{keyword})]
ORDER BY w.create_time DESC
```

### Query 2：检查用户是否在白名单中

```sql
SELECT COUNT(*) FROM pm_daily_report_whitelist
WHERE user_id = #{userId} AND del_flag = '0'
```

### Query 3：防重复添加（添加前校验）

与 Query 2 相同，返回 count > 0 则拒绝

---

## 修改现有 SQL

### 修改 selectActivityUsers（DailyReportMapper.xml）

在 `<where>` 块末尾追加：

```xml
AND u.user_id NOT IN (
    SELECT user_id FROM pm_daily_report_whitelist WHERE del_flag = '0'
)
```

### 修改 selectTotalUserCount（DailyReportMapper.xml，001特性新增）

同上，在 `<where>` 块末尾追加相同排除子查询

### 修改 selectUnsubmittedUsersOnDate（DailyReportMapper.xml，001特性新增）

同上，在 `<where>` 块末尾追加相同排除子查询

---

## API 接口定义

| 接口 | 方法 | URL | 权限 |
|------|------|-----|------|
| 白名单列表 | GET | `/project/whitelist/list` | `project:whitelist:list`（admin） |
| 添加白名单 | POST | `/project/whitelist` | `project:whitelist:add`（admin） |
| 移除白名单 | DELETE | `/project/whitelist/{id}` | `project:whitelist:remove`（admin） |
| 检查当前用户 | GET | `/project/whitelist/checkSelf` | 登录即可 |
