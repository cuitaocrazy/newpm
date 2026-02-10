-- ----------------------------
-- 删除孤儿菜单
-- 日期: 2026-02-09
-- 说明: 清理孤儿菜单（parent_id 指向不存在的菜单）
-- ----------------------------

USE `ry-vue`;

-- 删除旧的项目管理相关菜单（parent_id=2034，但2034不存在）
DELETE FROM sys_menu WHERE menu_id IN (2035, 2036, 2037, 2038, 2039, 2040, 2041);

-- 删除旧的客户信息相关菜单（parent_id=2021，但2021不存在）
DELETE FROM sys_menu WHERE menu_id IN (2022, 2023, 2024, 2025, 2026, 2027);

-- 验证删除结果 - 项目管理菜单
SELECT '=== 项目管理菜单 ===' AS info;
SELECT
    menu_id,
    menu_name,
    parent_id,
    path,
    component
FROM sys_menu
WHERE menu_name LIKE '%项目%' OR menu_name LIKE '%立项%'
ORDER BY parent_id, order_num;

-- 验证删除结果 - 客户信息菜单
SELECT '=== 客户信息菜单 ===' AS info;
SELECT
    menu_id,
    menu_name,
    parent_id,
    path,
    component
FROM sys_menu
WHERE menu_name LIKE '%客户%' OR menu_name LIKE '%市场%'
ORDER BY parent_id, order_num;
