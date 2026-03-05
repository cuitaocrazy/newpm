# 数据迁移实施方案

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将旧库（rjpm-mysql, db: `ry`）业务数据迁移到新库（newpm-mysql-1, db: `ry-vue`），使用新 DDL 标准表结构。

**Architecture:** 先备份新库现有数据，再清空重建，最后按字段映射规则从旧容器直接导入数据。字段不兼容的表（pm_project、pm_contract）使用临时表 + INSERT SELECT 方式处理。pm_province 表迁移到新表 pm_secondary_region。

**Tech Stack:** Docker exec + mysqldump + MySQL pipe（容器间直连，无需中间文件）

---

## 容器信息

| 角色 | 容器 ID | 容器名 | 数据库 | 密码 |
|------|---------|--------|--------|------|
| 旧库（源） | `7fb7d3fcd935` | rjpm-mysql | `ry` | root: `root123456` |
| 新库（目标） | `3523a41063b7` | newpm-mysql-1 | `ry-vue` | root: `password` |

## 预期目标行数

| 表 | 期望行数 |
|----|---------|
| pm_project | 51 |
| pm_contract | 169 |
| pm_payment | 227 |
| pm_customer | 46 |
| pm_secondary_region | 37 |
| pm_attachment | 164 |

## 字段差异分析

### pm_project

| 旧字段 | 新字段 | 处理 |
|--------|--------|------|
| `province_id` bigint | `region_id` bigint | 直接映射 |
| `year` int | `established_year` varchar(20) | `CAST(\`year\` AS CHAR)` |
| `confirm_status` char(1) | `revenue_confirm_status` varchar(20) | 直接映射 |
| `confirm_quarter` varchar(20) | `revenue_confirm_year` varchar(20) | 直接映射 |
| `confirm_user_name` varchar(64) | `company_revenue_confirmed_by` varchar(64) | 直接映射 |
| `confirm_time` datetime | `company_revenue_confirmed_time` datetime | 同时映射到 `confirm_time` 和 `company_revenue_confirmed_time` |
| `project_full_name` | —— | 丢弃 |
| `implementation_year` | —— | 丢弃 |

### pm_contract

旧表多余字段（新表无，直接丢弃）：`customer_name`、`dept_name`、`team_id`、`team_leader_id`

### pm_province → pm_secondary_region

| 旧字段 | 新字段 | 处理 |
|--------|--------|------|
| `province_id` | `region_id` | 直接映射（保持 ID） |
| `province_code` | `region_code` | 直接映射 |
| `province_name` | `region_name` | 直接映射 |
| `province_type` | `region_type` | 值映射：省→0, 直辖市→1, 自治区→2, 计划单列市→4 |
| `region_dict_value` | `region_dict_value` | 直接映射 |

### 废弃旧表

- `pm_payment_milestone`（0 条）→ 不迁移

---

## Phase 0：备份新库当前数据

```bash
docker exec 3523a41063b7 mysqldump \
  -u root -ppassword \
  --default-character-set=utf8mb4 \
  --no-tablespaces \
  ry-vue \
  > /tmp/newpm_backup_20260305.sql

echo "备份行数: $(wc -l < /tmp/newpm_backup_20260305.sql)"
```

预期：生成备份文件约 1600+ 行，包含当前 sys_* 数据。

---

## Phase 1：清空新库所有表数据和结构

```bash
# 获取所有表名并生成 DROP TABLE 语句
TABLES=$(docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue \
  -sN -e "SELECT table_name FROM information_schema.tables WHERE table_schema='ry-vue';" 2>/dev/null \
  | tr '\n' ',')

echo "DROP tables: $TABLES"

docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue -e "
SET FOREIGN_KEY_CHECKS = 0;
SET @tables = NULL;
SELECT GROUP_CONCAT('\`', table_name, '\`') INTO @tables
  FROM information_schema.tables
  WHERE table_schema = 'ry-vue';
SET @tables = CONCAT('DROP TABLE IF EXISTS ', @tables);
PREPARE stmt FROM @tables;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET FOREIGN_KEY_CHECKS = 1;
" 2>/dev/null

echo "验证（应返回空）:"
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue \
  -e "SHOW TABLES;" 2>/dev/null
```

---

## Phase 2：按新 DDL 重建表结构

```bash
cat /Users/kongli/ws-claude/PM/newpm/pm-sql/init/00_tables_ddl.sql \
  | docker exec -i 3523a41063b7 mysql \
    -u root -ppassword \
    --default-character-set=utf8mb4 \
    ry-vue

echo "验证表数量:"
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue \
  -e "SELECT COUNT(*) as 表数量 FROM information_schema.tables WHERE table_schema='ry-vue';" 2>/dev/null
```

预期：约 47 张表（含 qrtz_* 和 gen_* 表）。

---

## Phase 3：从旧库导入 sys_* 系统表数据

### 3.1 从旧库导出 sys_* 数据并导入新库

```bash
docker exec 7fb7d3fcd935 mysqldump \
  -u root -proot123456 \
  --default-character-set=utf8mb4 \
  --no-create-info \
  --no-tablespaces \
  --skip-triggers \
  ry \
  sys_config sys_dept sys_dict_data sys_dict_type sys_job \
  sys_logininfor sys_menu sys_notice sys_oper_log sys_post \
  sys_role sys_role_dept sys_role_menu sys_user sys_user_post sys_user_role \
  2>/dev/null \
  | docker exec -i 3523a41063b7 mysql \
    -u root -ppassword \
    --default-character-set=utf8mb4 \
    ry-vue

echo "验证用户数:"
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue \
  -e "SELECT COUNT(*) as 用户数 FROM sys_user; SELECT COUNT(*) as 菜单数 FROM sys_menu; SELECT COUNT(*) as 字典数 FROM sys_dict_data;" 2>/dev/null
```

预期：用户 152，菜单 118，字典 168。

### 3.2 补充新增 PM 菜单（不覆盖旧库已有配置）

```bash
sed 's/^INSERT INTO/INSERT IGNORE INTO/g' \
  /Users/kongli/ws-claude/PM/newpm/pm-sql/init/02_menu_data.sql \
  | docker exec -i 3523a41063b7 mysql \
    -u root -ppassword \
    --default-character-set=utf8mb4 \
    ry-vue

echo "最终菜单数:"
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue \
  -e "SELECT COUNT(*) as 菜单数 FROM sys_menu;" 2>/dev/null
```

---

## Phase 4：导入 PM 直接兼容表（13 张）

这些表字段与新库完全兼容，直接导入：

```bash
docker exec 7fb7d3fcd935 mysqldump \
  -u root -proot123456 \
  --default-character-set=utf8mb4 \
  --no-create-info \
  --no-tablespaces \
  --skip-triggers \
  ry \
  pm_attachment \
  pm_customer \
  pm_customer_contact \
  pm_daily_report \
  pm_daily_report_detail \
  pm_payment \
  pm_project_approval \
  pm_project_contract_rel \
  pm_project_manager_change \
  pm_project_member \
  pm_project_stage_change \
  pm_team_revenue_confirmation \
  pm_work_calendar \
  2>/dev/null \
  | docker exec -i 3523a41063b7 mysql \
    -u root -ppassword \
    --default-character-set=utf8mb4 \
    ry-vue

echo "验证直接兼容表行数:"
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue -e "
SELECT 'pm_attachment'              AS 表名, COUNT(*) AS 行数 FROM pm_attachment
UNION ALL SELECT 'pm_customer',            COUNT(*) FROM pm_customer
UNION ALL SELECT 'pm_customer_contact',    COUNT(*) FROM pm_customer_contact
UNION ALL SELECT 'pm_payment',             COUNT(*) FROM pm_payment
UNION ALL SELECT 'pm_project_approval',    COUNT(*) FROM pm_project_approval
UNION ALL SELECT 'pm_project_member',      COUNT(*) FROM pm_project_member
UNION ALL SELECT 'pm_team_revenue_confirmation', COUNT(*) FROM pm_team_revenue_confirmation;" 2>/dev/null
```

预期：attachment=164, customer=46, payment=227, project_approval=68, project_member=163。

---

## Phase 5：导入 pm_contract（临时表方案）

旧表有 4 个新表不存在的字段：`customer_name`、`dept_name`、`team_id`、`team_leader_id`。

```bash
# Step 1：在新库建临时表（新表结构 + 旧库多余字段）
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue -e "
DROP TABLE IF EXISTS pm_contract_old;
CREATE TABLE pm_contract_old LIKE pm_contract;
ALTER TABLE pm_contract_old
  ADD COLUMN customer_name varchar(200) DEFAULT NULL,
  ADD COLUMN dept_name varchar(100) DEFAULT NULL,
  ADD COLUMN team_id bigint DEFAULT NULL,
  ADD COLUMN team_leader_id bigint DEFAULT NULL;
" 2>/dev/null

# Step 2：从旧库导入到临时表
docker exec 7fb7d3fcd935 mysqldump \
  -u root -proot123456 \
  --default-character-set=utf8mb4 \
  --no-create-info \
  --no-tablespaces \
  --skip-triggers \
  ry pm_contract \
  2>/dev/null \
  | sed 's/`pm_contract`/`pm_contract_old`/g' \
  | docker exec -i 3523a41063b7 mysql \
    -u root -ppassword \
    --default-character-set=utf8mb4 \
    ry-vue

# Step 3：从临时表迁移到正式表（显式列名，排除旧字段）
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue -e "
INSERT INTO pm_contract (
  contract_id, contract_code, contract_name, customer_id,
  dept_id, contract_type, contract_status, contract_sign_date,
  contract_period, contract_amount, tax_rate, amount_no_tax,
  tax_amount, confirm_amount, confirm_year, free_maintenance_period,
  del_flag, create_by, create_time, update_by, update_time, remark,
  reserved_field1, reserved_field2, reserved_field3, reserved_field4, reserved_field5
)
SELECT
  contract_id, contract_code, contract_name, customer_id,
  dept_id, contract_type, contract_status, contract_sign_date,
  contract_period, contract_amount, tax_rate, amount_no_tax,
  tax_amount, confirm_amount, confirm_year, free_maintenance_period,
  del_flag, create_by, create_time, update_by, update_time, remark,
  reserved_field1, reserved_field2, reserved_field3, reserved_field4, reserved_field5
FROM pm_contract_old;

DROP TABLE pm_contract_old;
" 2>/dev/null

echo "验证 pm_contract:"
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue \
  -e "SELECT COUNT(*) AS 行数 FROM pm_contract;" 2>/dev/null
```

预期：169 条。

---

## Phase 6：导入 pm_project（临时表 + 字段映射）

旧表有 7 个字段需要映射或丢弃：

```bash
# Step 1：在新库建临时表（新表结构改造为旧库字段）
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue -e "
DROP TABLE IF EXISTS pm_project_old;
CREATE TABLE pm_project_old LIKE pm_project;
ALTER TABLE pm_project_old
  -- 添加旧库有、新库无的字段
  ADD COLUMN project_full_name varchar(500) DEFAULT NULL,
  ADD COLUMN \`year\` int DEFAULT NULL,
  ADD COLUMN implementation_year varchar(10) DEFAULT NULL,
  ADD COLUMN confirm_status char(1) DEFAULT NULL,
  ADD COLUMN confirm_quarter varchar(20) DEFAULT NULL,
  ADD COLUMN confirm_user_name varchar(64) DEFAULT NULL,
  ADD COLUMN province_id bigint DEFAULT NULL,
  -- 删除新库有、旧库无的字段
  DROP COLUMN region_id,
  DROP COLUMN established_year,
  DROP COLUMN revenue_confirm_status,
  DROP COLUMN revenue_confirm_year,
  DROP COLUMN company_revenue_confirmed_by,
  DROP COLUMN company_revenue_confirmed_time;
" 2>/dev/null

# Step 2：从旧库导入到临时表
docker exec 7fb7d3fcd935 mysqldump \
  -u root -proot123456 \
  --default-character-set=utf8mb4 \
  --no-create-info \
  --no-tablespaces \
  --skip-triggers \
  ry pm_project \
  2>/dev/null \
  | sed 's/`pm_project`/`pm_project_old`/g' \
  | docker exec -i 3523a41063b7 mysql \
    -u root -ppassword \
    --default-character-set=utf8mb4 \
    ry-vue

echo "临时表行数:"
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue \
  -e "SELECT COUNT(*) FROM pm_project_old;" 2>/dev/null

# Step 3：从临时表迁移到正式表（含字段映射）
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue -e "
INSERT INTO pm_project (
  project_id, project_code, project_name,
  industry, region, region_id, short_name,
  established_year,
  project_category, project_dept, project_status, project_stage,
  acceptance_status, estimated_workload, actual_workload, adjust_workload,
  project_address, project_plan, project_description,
  project_manager_id, market_manager_id, participants,
  sales_manager_id, sales_contact, team_leader_id,
  customer_id, customer_contact_id,
  merchant_contact, merchant_phone,
  start_date, end_date, production_date, acceptance_date,
  project_budget, project_cost, expense_budget, cost_budget, labor_cost, purchase_cost,
  approval_status, approval_reason,
  industry_code, region_code,
  approval_time, approver_id, remark,
  tax_rate, confirm_user_id, confirm_time,
  reserved_field1, reserved_field2, reserved_field3, reserved_field4, reserved_field5,
  del_flag, create_by, create_time, update_by, update_time,
  revenue_confirm_status, revenue_confirm_year,
  confirm_amount, after_tax_amount,
  company_revenue_confirmed_by, company_revenue_confirmed_time
)
SELECT
  project_id, project_code, project_name,
  industry, region,
  province_id,                      -- province_id → region_id
  short_name,
  CAST(\`year\` AS CHAR),            -- year(int) → established_year(varchar)
  project_category, project_dept, project_status, project_stage,
  acceptance_status, estimated_workload, actual_workload, adjust_workload,
  project_address, project_plan, project_description,
  project_manager_id, market_manager_id, participants,
  sales_manager_id, sales_contact, team_leader_id,
  customer_id, customer_contact_id,
  merchant_contact, merchant_phone,
  start_date, end_date, production_date, acceptance_date,
  project_budget, project_cost, expense_budget, cost_budget, labor_cost, purchase_cost,
  approval_status, approval_reason,
  industry_code, region_code,
  approval_time, approver_id, remark,
  tax_rate, confirm_user_id, confirm_time,
  reserved_field1, reserved_field2, reserved_field3, reserved_field4, reserved_field5,
  del_flag, create_by, create_time, update_by, update_time,
  confirm_status,                    -- confirm_status → revenue_confirm_status
  confirm_quarter,                   -- confirm_quarter → revenue_confirm_year
  confirm_amount,
  after_tax_amount,
  confirm_user_name,                 -- confirm_user_name → company_revenue_confirmed_by
  confirm_time                       -- confirm_time → company_revenue_confirmed_time
FROM pm_project_old;

DROP TABLE pm_project_old;
" 2>/dev/null

echo "验证 pm_project:"
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue \
  -e "SELECT COUNT(*) AS 行数 FROM pm_project;" 2>/dev/null
```

预期：51 条。

---

## Phase 7：迁移 pm_province → pm_secondary_region

```bash
# Step 1：在新库建临时表承接旧库 pm_province 数据
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue -e "
DROP TABLE IF EXISTS pm_province_temp;
CREATE TABLE pm_province_temp (
  id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  province_code varchar(10),
  province_name varchar(50),
  province_type varchar(20),
  region_dict_value varchar(50),
  sort_order int DEFAULT 0,
  del_flag char(1) DEFAULT '0',
  status char(1) DEFAULT '0',
  create_by varchar(64),
  create_time datetime,
  update_by varchar(64),
  update_time datetime,
  remark varchar(500)
);
" 2>/dev/null

# Step 2：从旧库导入 pm_province 到临时表
docker exec 7fb7d3fcd935 mysqldump \
  -u root -proot123456 \
  --default-character-set=utf8mb4 \
  --no-create-info \
  --no-tablespaces \
  --skip-triggers \
  ry pm_province \
  2>/dev/null \
  | sed 's/`pm_province`/`pm_province_temp`/g' \
  | docker exec -i 3523a41063b7 mysql \
    -u root -ppassword \
    --default-character-set=utf8mb4 \
    ry-vue

# Step 3：迁移到 pm_secondary_region（含 province_type 值映射）
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue -e "
INSERT INTO pm_secondary_region (
  region_id, region_code, region_name, region_type,
  region_dict_value, sort_order, status, del_flag,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  id,
  province_code,
  province_name,
  CASE province_type
    WHEN '省'         THEN '0'
    WHEN '直辖市'     THEN '1'
    WHEN '自治区'     THEN '2'
    WHEN '计划单列市' THEN '4'
    ELSE '0'
  END,
  region_dict_value, sort_order, status, del_flag,
  create_by, create_time, update_by, update_time, remark
FROM pm_province_temp;

DROP TABLE pm_province_temp;
" 2>/dev/null

echo "验证 pm_secondary_region:"
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue \
  -e "SELECT COUNT(*) AS 行数 FROM pm_secondary_region;" 2>/dev/null
```

预期：37 条。

---

## Phase 8：全量验证

```bash
docker exec 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue -e "
SELECT '=== PM 业务表 ===' AS '';
SELECT 'pm_project'               AS 表名, COUNT(*) AS 行数, '期望: 51'  AS 期望 FROM pm_project
UNION ALL SELECT 'pm_contract',   COUNT(*), '期望: 169' FROM pm_contract
UNION ALL SELECT 'pm_payment',    COUNT(*), '期望: 227' FROM pm_payment
UNION ALL SELECT 'pm_customer',   COUNT(*), '期望: 46'  FROM pm_customer
UNION ALL SELECT 'pm_secondary_region', COUNT(*), '期望: 37' FROM pm_secondary_region
UNION ALL SELECT 'pm_attachment', COUNT(*), '期望: 164' FROM pm_attachment
UNION ALL SELECT 'pm_customer_contact',   COUNT(*), '' FROM pm_customer_contact
UNION ALL SELECT 'pm_project_approval',   COUNT(*), '' FROM pm_project_approval
UNION ALL SELECT 'pm_project_member',     COUNT(*), '' FROM pm_project_member
UNION ALL SELECT 'pm_team_revenue_confirmation', COUNT(*), '' FROM pm_team_revenue_confirmation;
SELECT '=== 系统表 ===' AS '';
SELECT 'sys_user',  COUNT(*) AS 行数, '' FROM sys_user
UNION ALL SELECT 'sys_menu',  COUNT(*), '' FROM sys_menu
UNION ALL SELECT 'sys_dept',  COUNT(*), '' FROM sys_dept
UNION ALL SELECT 'sys_dict_data', COUNT(*), '' FROM sys_dict_data;" 2>/dev/null
```

---

## 执行顺序汇总

| Phase | 内容 | 状态 |
|-------|------|------|
| 0 | 备份新库到 /tmp/newpm_backup_20260305.sql | 待执行 |
| 1 | 清空新库所有表 | 待执行 |
| 2 | 按新 DDL 重建表结构 | 待执行 |
| 3.1 | 从旧库导入 sys_* 系统数据（16张表） | 待执行 |
| 3.2 | INSERT IGNORE 补充新增 PM 菜单 | 待执行 |
| 4 | 直接兼容 PM 表（13张）直接导入 | 待执行 |
| 5 | pm_contract 临时表迁移（排除4个旧字段） | 待执行 |
| 6 | pm_project 临时表迁移（7字段映射） | 待执行 |
| 7 | pm_province → pm_secondary_region（类型值映射） | 待执行 |
| 8 | 全量验证行数 | 待执行 |

---

## 注意事项

1. **approval_status 已是数字**：旧库实际存 `0/1/2`（非中文字符串），可直接迁移。
2. **company_revenue_confirmed_by 字段**：从旧库 `confirm_user_name`（姓名字符串）迁移，新字段语义存用户名，后续可对照 `sys_user` 做二次修正。
3. **pm_project.`year` 保留字**：ALTER/SELECT 时已加反引号，执行时确保保留。
4. **旧库 pm_contract.contract_code 在新库有唯一索引**：如有重复值会报错，Phase 5 执行后确认无 ERROR 输出。
5. **docker exec 2>/dev/null**：抑制密码警告，不影响 SQL 执行结果；如需调试去掉此重定向。
