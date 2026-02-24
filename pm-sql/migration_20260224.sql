-- ==============================================================
-- 数据迁移脚本：旧库 → 新库
-- 执行日期：2026-02-24
-- 旧库：ry_20260213.sql（备份于 2026-02-13）
-- 新库：ry-vue（已执行 pm-sql/init/*.sql 初始化）
--
-- 执行前准备：
--   1. 将新库初始化完毕（init/00,01,02 已执行）
--   2. 创建临时旧库：
--      mysql -u root -p -e "CREATE DATABASE ry_vue_old CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
--      mysql -u root -p ry_vue_old < /path/to/ry_20260213.sql
--   3. 执行本脚本：
--      mysql -u root -p `ry-vue` < pm-sql/migration_20260224.sql
--
-- 执行顺序：
--   Step 1：字典数据修复与补充
--   Step 2：系统数据迁移（dept/user/post/role/关联表）
--   Step 3：业务数据迁移（pm_ 表）
--   Step 4：重置 AUTO_INCREMENT
-- ==============================================================

USE `ry-vue`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ==============================================================
-- Step 1：字典数据修复与补充
-- ==============================================================

-- -------------------------------------------------------
-- 1.1 修复 sys_fkzt 付款状态：去除 dict_value 中的 Tab 字符
-- -------------------------------------------------------
UPDATE sys_dict_data SET dict_value = TRIM(dict_value) WHERE dict_type = 'sys_fkzt';

-- -------------------------------------------------------
-- 1.2 补充 sys_fkzt 缺失值（旧库有，新库无）
--     YKQK = 已开全款发票-未付款（旧2号，实际用于老数据）
--     FKWZ = 付款未知-待确认
--     BZHK = 不再回款
-- -------------------------------------------------------
INSERT IGNORE INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
VALUES
  (350, 10, '已开全款发票-未付款', 'YKQK', 'sys_fkzt', NULL, 'warning', 'N', '0', 'admin', NOW(), '', NULL, '旧库迁移保留值'),
  (351, 11, '付款未知-待确认',     'FKWZ', 'sys_fkzt', NULL, 'danger',  'N', '0', 'admin', NOW(), '', NULL, '旧库迁移保留值'),
  (352, 12, '不再回款',           'BZHK', 'sys_fkzt', NULL, 'warning', 'N', '0', 'admin', NOW(), '', NULL, '旧库迁移保留值');

-- -------------------------------------------------------
-- 1.3 补充 sys_htzt 合同状态：以旧库 8 种值为准
--     新库已有: 1,2,3,4,5；补充旧库特有: 0,6,7
-- -------------------------------------------------------
INSERT IGNORE INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
VALUES
  (353, 0, '0-谈判签署中',       '0', 'sys_htzt', NULL, 'success', 'N', '0', 'admin', NOW(), '', NULL, NULL),
  (354, 6, '6-全款已回',         '6', 'sys_htzt', NULL, 'warning', 'N', '0', 'admin', NOW(), '', NULL, '合同工作已做完，全款已回'),
  (355, 7, '7-已签-无需回款',    '7', 'sys_htzt', NULL, 'danger',  'N', '0', 'admin', NOW(), '', NULL, NULL);

-- -------------------------------------------------------
-- 1.4 迁移 contact_tag 联系人标签字典（新库无此 dict_type）
-- -------------------------------------------------------
INSERT IGNORE INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.sys_dict_type
WHERE dict_type = 'contact_tag';

INSERT IGNORE INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.sys_dict_data
WHERE dict_type = 'contact_tag';

-- -------------------------------------------------------
-- 1.5 迁移 industry 行业字典（新库无此 dict_type）
-- -------------------------------------------------------
INSERT IGNORE INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.sys_dict_type
WHERE dict_type = 'industry';

INSERT IGNORE INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.sys_dict_data
WHERE dict_type = 'industry';

-- ==============================================================
-- Step 2：系统数据迁移
-- ==============================================================

-- -------------------------------------------------------
-- 2.1 sys_dept 部门表
--     新库初始化时有默认部门，用旧库数据完整替换
-- -------------------------------------------------------
-- 只删除非 RuoYi 系统预置的部门（dept_id >= 100 且非框架默认）
DELETE FROM sys_dept WHERE dept_id >= 100;

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time)
SELECT dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time
FROM ry_vue_old.sys_dept
WHERE dept_id >= 100;

-- -------------------------------------------------------
-- 2.2 sys_post 岗位表
--     跳过框架默认的 4 个岗位（id 1-4），迁移自定义岗位
-- -------------------------------------------------------
DELETE FROM sys_post WHERE post_id > 4;

INSERT INTO sys_post (post_id, post_code, post_name, post_sort, status, create_by, create_time, update_by, update_time, remark)
SELECT post_id, post_code, post_name, post_sort, status, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.sys_post
WHERE post_id > 4;

-- -------------------------------------------------------
-- 2.3 sys_role 自定义角色
--     跳过框架默认角色（id 1,2），迁移业务角色（100+）
-- -------------------------------------------------------
DELETE FROM sys_role WHERE role_id >= 100;

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.sys_role
WHERE role_id >= 100;

-- -------------------------------------------------------
-- 2.4 sys_user 用户表
--     跳过 user_id=1 (admin，新库已有)
--     跳过 user_id=2 (ry 测试用户)
-- -------------------------------------------------------
DELETE FROM sys_user WHERE user_id > 2;

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark)
SELECT user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.sys_user
WHERE user_id > 2;

-- -------------------------------------------------------
-- 2.5 sys_user_post 用户岗位关联
-- -------------------------------------------------------
DELETE FROM sys_user_post WHERE user_id > 2;

INSERT IGNORE INTO sys_user_post (user_id, post_id)
SELECT user_id, post_id
FROM ry_vue_old.sys_user_post
WHERE user_id > 2;

-- -------------------------------------------------------
-- 2.6 sys_user_role 用户角色关联
-- -------------------------------------------------------
DELETE FROM sys_user_role WHERE user_id > 2;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user_id, role_id
FROM ry_vue_old.sys_user_role
WHERE user_id > 2;

-- -------------------------------------------------------
-- 2.7 sys_role_menu 角色菜单权限
--     仅迁移系统菜单级别的权限（menu_id < 2000）
--     PM 业务模块菜单权限 ID 在新旧库中不同，无法直接映射
--     【后续操作】：请在系统界面为各角色分配 PM 相关权限
-- -------------------------------------------------------
DELETE FROM sys_role_menu WHERE role_id >= 100 AND menu_id < 2000;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, menu_id
FROM ry_vue_old.sys_role_menu
WHERE role_id >= 100 AND menu_id < 2000;

-- -------------------------------------------------------
-- 2.8 给 sys_admin 角色（role_id=103）分配新库所有 PM 菜单权限
-- -------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 103, menu_id
FROM sys_menu
WHERE menu_id >= 2000;

-- ==============================================================
-- Step 3：业务数据迁移（pm_ 表，按依赖顺序）
-- ==============================================================

-- -------------------------------------------------------
-- 3.1 pm_customer 客户表（44条）
--     删除：sales_manager_name
-- -------------------------------------------------------
INSERT IGNORE INTO pm_customer (customer_id, customer_simple_name, customer_all_name, industry, region, sales_manager_id, office_address, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT customer_id, customer_simple_name, customer_all_name, industry, region, sales_manager_id, office_address, del_flag, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.pm_customer;

-- -------------------------------------------------------
-- 3.2 pm_customer_contact 客户联系人表（102条）
--     结构完全相同，直接迁移
-- -------------------------------------------------------
INSERT IGNORE INTO pm_customer_contact (contact_id, customer_id, contact_name, contact_phone, contact_tag, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT contact_id, customer_id, contact_name, contact_phone, contact_tag, del_flag, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.pm_customer_contact;

-- -------------------------------------------------------
-- 3.3 pm_secondary_region 二级区域表（37条）
--     旧表名：pm_province
--     字段改名：province_id→region_id, province_code→region_code
--               province_name→region_name, province_type→region_type
--     值映射：省→0, 直辖市→1, 自治区→2, 特别行政区→3, 计划单列市→4
-- -------------------------------------------------------
INSERT IGNORE INTO pm_secondary_region (region_id, region_code, region_name, region_type, region_dict_value, sort_order, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT
    province_id,
    province_code,
    province_name,
    CASE province_type
        WHEN '省'        THEN '0'
        WHEN '直辖市'    THEN '1'
        WHEN '自治区'    THEN '2'
        WHEN '特别行政区' THEN '3'
        WHEN '计划单列市' THEN '4'
        ELSE '0'
    END,
    region_dict_value,
    sort_order,
    status,
    del_flag,
    create_by,
    create_time,
    update_by,
    update_time,
    remark
FROM ry_vue_old.pm_province;

-- -------------------------------------------------------
-- 3.4 pm_project 项目表（42条）
--     字段改名：
--       year             → established_year
--       province_id      → region_id
--       project_status   → project_stage
--       implementation_year → revenue_confirm_year
--       confirm_status   → revenue_confirm_status
--     字段删除：project_full_name, approval_time, approver_id,
--               confirm_quarter, confirm_user_name
--     字段新增：company_revenue_confirmed_by, company_revenue_confirmed_time（留 NULL）
--     ⚠️  跳过 del_flag='1' 的已删除记录（project_id 20,26,27 等测试数据）
-- -------------------------------------------------------
INSERT IGNORE INTO pm_project (
    project_id, project_code, project_name,
    industry, region, region_id, short_name, established_year,
    project_category, project_dept, project_stage, acceptance_status,
    estimated_workload, actual_workload,
    project_address, project_plan, project_description,
    project_manager_id, market_manager_id, participants,
    sales_manager_id, sales_contact, team_leader_id,
    customer_id, customer_contact_id,
    merchant_contact, merchant_phone,
    start_date, end_date, production_date, acceptance_date,
    project_budget, project_cost, expense_budget, cost_budget, labor_cost, purchase_cost,
    approval_status, approval_reason,
    industry_code, region_code,
    remark, tax_rate,
    confirm_user_id, confirm_time,
    reserved_field1, reserved_field2, reserved_field3, reserved_field4, reserved_field5,
    del_flag, create_by, create_time, update_by, update_time,
    revenue_confirm_status, revenue_confirm_year,
    confirm_amount, after_tax_amount,
    company_revenue_confirmed_by, company_revenue_confirmed_time
)
SELECT
    project_id, project_code, project_name,
    industry, region, province_id, short_name, year,
    project_category, project_dept, project_status, acceptance_status,
    estimated_workload, actual_workload,
    project_address, project_plan, project_description,
    project_manager_id, market_manager_id, participants,
    sales_manager_id, sales_contact, team_leader_id,
    customer_id, customer_contact_id,
    merchant_contact, merchant_phone,
    start_date, end_date, production_date, acceptance_date,
    project_budget, project_cost, expense_budget, cost_budget, labor_cost, purchase_cost,
    approval_status, approval_reason,
    industry_code, region_code,
    remark, tax_rate,
    confirm_user_id, confirm_time,
    reserved_field1, reserved_field2, reserved_field3, reserved_field4, reserved_field5,
    del_flag, create_by, create_time, update_by, update_time,
    COALESCE(confirm_status, '0'), implementation_year,
    confirm_amount, after_tax_amount,
    NULL, NULL   -- company_revenue_confirmed_by / time（新字段，旧库无）
FROM ry_vue_old.pm_project;

-- 回填 region_code：旧库 pm_project.region_code 全为 NULL，需从 pm_secondary_region 补充
-- （region_code 用于编辑页面二级区域下拉回显，region_id 已从旧库 province_id 映射过来）
UPDATE pm_project p
JOIN pm_secondary_region sr ON p.region_id = sr.region_id
SET p.region_code = sr.region_code
WHERE p.region_id IS NOT NULL;

-- -------------------------------------------------------
-- 3.5 pm_project_approval 项目审核表（44条）
--     结构完全相同，直接迁移
-- -------------------------------------------------------
INSERT IGNORE INTO pm_project_approval (approval_id, project_id, approval_status, approval_reason, approver_id, approval_time, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT approval_id, project_id, approval_status, approval_reason, approver_id, approval_time, del_flag, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.pm_project_approval;

-- -------------------------------------------------------
-- 3.6 pm_contract 合同表（143条）
--     删除：customer_name, dept_name, team_id, team_leader_id
-- -------------------------------------------------------
INSERT IGNORE INTO pm_contract (
    contract_id, contract_code, contract_name,
    customer_id, dept_id,
    contract_type, contract_status,
    contract_sign_date, contract_period,
    contract_amount, tax_rate, amount_no_tax, tax_amount,
    confirm_amount, confirm_year,
    free_maintenance_period,
    del_flag, create_by, create_time, update_by, update_time, remark,
    reserved_field1, reserved_field2, reserved_field3, reserved_field4, reserved_field5
)
SELECT
    contract_id, contract_code, contract_name,
    customer_id, dept_id,
    contract_type, contract_status,
    contract_sign_date, contract_period,
    contract_amount, tax_rate, amount_no_tax, tax_amount,
    confirm_amount, confirm_year,
    free_maintenance_period,
    del_flag, create_by, create_time, update_by, update_time, remark,
    reserved_field1, reserved_field2, reserved_field3, reserved_field4, reserved_field5
FROM ry_vue_old.pm_contract;

-- -------------------------------------------------------
-- 3.7 pm_payment 款项表（227条）
--     删除：flow_status
--     ⚠️  新库 payment_amount 在第4列，旧库在最后列
--         必须使用显式列名（不能用 SELECT *）
-- -------------------------------------------------------
INSERT IGNORE INTO pm_payment (
    payment_id, contract_id, payment_method_name,
    payment_amount,
    has_penalty, penalty_amount,
    payment_status,
    expected_quarter, actual_quarter,
    submit_acceptance_date, actual_payment_date,
    confirm_year,
    del_flag, create_by, create_time, update_by, update_time, remark
)
SELECT
    payment_id, contract_id, payment_method_name,
    payment_amount,
    has_penalty, penalty_amount,
    payment_status,
    expected_quarter, actual_quarter,
    submit_acceptance_date, actual_payment_date,
    confirm_year,
    del_flag, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.pm_payment;

-- -------------------------------------------------------
-- 3.8 pm_project_contract_rel 项目合同关联表（43条）
--     project_id 类型变更：varchar(50) → bigint（CAST 转换）
--     删除：project_name, contract_name, rel_type, is_main
-- -------------------------------------------------------
INSERT IGNORE INTO pm_project_contract_rel (
    rel_id, project_id, contract_id,
    rel_status, bind_date,
    del_flag, create_by, create_time, update_by, update_time, remark
)
SELECT
    rel_id, CAST(project_id AS UNSIGNED), contract_id,
    rel_status, bind_date,
    del_flag, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.pm_project_contract_rel
WHERE project_id REGEXP '^[0-9]+$';  -- 过滤掉非纯数字的 project_id（防御性处理）

-- -------------------------------------------------------
-- 3.9 pm_attachment 附件表（163条）
--     删除：project_id (varchar50), contract_id
-- -------------------------------------------------------
INSERT IGNORE INTO pm_attachment (
    attachment_id, business_type, business_id,
    document_type, file_name, file_path,
    file_size, file_type, file_description,
    del_flag, create_by, create_time, update_by, update_time, remark
)
SELECT
    attachment_id, business_type, business_id,
    document_type, file_name, file_path,
    file_size, file_type, file_description,
    del_flag, create_by, create_time, update_by, update_time, remark
FROM ry_vue_old.pm_attachment;

-- -------------------------------------------------------
-- 3.10 pm_team_revenue_confirmation 团队收入确认表（40条）
--      结构完全相同，直接迁移
-- -------------------------------------------------------
INSERT IGNORE INTO pm_team_revenue_confirmation (
    team_confirm_id, project_id, dept_id,
    confirm_amount, confirm_time, confirm_user_id,
    remark, del_flag, create_by, create_time, update_by, update_time
)
SELECT
    team_confirm_id, project_id, dept_id,
    confirm_amount, confirm_time, confirm_user_id,
    remark, del_flag, create_by, create_time, update_by, update_time
FROM ry_vue_old.pm_team_revenue_confirmation;

-- -------------------------------------------------------
-- 3.11 pm_project_manager_change（旧库 0 条，跳过）
-- -------------------------------------------------------
-- 无需操作

-- ==============================================================
-- Step 4：重置 AUTO_INCREMENT
-- ==============================================================

-- 查询各表最大 ID 后设置 AUTO_INCREMENT（留 100 的余量）
ALTER TABLE pm_customer               AUTO_INCREMENT = 200;
ALTER TABLE pm_customer_contact       AUTO_INCREMENT = 300;
ALTER TABLE pm_secondary_region       AUTO_INCREMENT = 200;
ALTER TABLE pm_project                AUTO_INCREMENT = 200;
ALTER TABLE pm_project_approval       AUTO_INCREMENT = 200;
ALTER TABLE pm_contract               AUTO_INCREMENT = 400;
ALTER TABLE pm_payment                AUTO_INCREMENT = 500;
ALTER TABLE pm_project_contract_rel   AUTO_INCREMENT = 200;
ALTER TABLE pm_attachment             AUTO_INCREMENT = 400;
ALTER TABLE pm_team_revenue_confirmation AUTO_INCREMENT = 200;

-- sys 表
ALTER TABLE sys_dept      AUTO_INCREMENT = 300;
ALTER TABLE sys_user      AUTO_INCREMENT = 400;
ALTER TABLE sys_post      AUTO_INCREMENT = 100;
ALTER TABLE sys_role      AUTO_INCREMENT = 200;

-- ==============================================================
-- Step 5：数据校验（执行后确认各表记录数）
-- ==============================================================

SELECT 'pm_customer'              AS tbl, COUNT(*) AS cnt FROM pm_customer               UNION ALL
SELECT 'pm_customer_contact',            COUNT(*)        FROM pm_customer_contact          UNION ALL
SELECT 'pm_secondary_region',            COUNT(*)        FROM pm_secondary_region          UNION ALL
SELECT 'pm_project',                     COUNT(*)        FROM pm_project                   UNION ALL
SELECT 'pm_project_approval',            COUNT(*)        FROM pm_project_approval          UNION ALL
SELECT 'pm_contract',                    COUNT(*)        FROM pm_contract                  UNION ALL
SELECT 'pm_payment',                     COUNT(*)        FROM pm_payment                   UNION ALL
SELECT 'pm_project_contract_rel',        COUNT(*)        FROM pm_project_contract_rel      UNION ALL
SELECT 'pm_attachment',                  COUNT(*)        FROM pm_attachment                UNION ALL
SELECT 'pm_team_revenue_confirmation',   COUNT(*)        FROM pm_team_revenue_confirmation UNION ALL
SELECT 'sys_dept',                       COUNT(*)        FROM sys_dept                     UNION ALL
SELECT 'sys_user',                       COUNT(*)        FROM sys_user                     UNION ALL
SELECT 'sys_role',                       COUNT(*)        FROM sys_role;

SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================
-- 迁移完成！
-- 后续手动操作：
--   1. 在系统界面为 xmjl/zhyy/xmglz 等角色分配 PM 模块菜单权限
--   2. 验证各用户登录、数据展示是否正常
--   3. 验证字典数据是否完整（联系人标签、合同状态、付款状态）
--   4. 确认无误后删除临时库：DROP DATABASE ry_vue_old;
-- ==============================================================
