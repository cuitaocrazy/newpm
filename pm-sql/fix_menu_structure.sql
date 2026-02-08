-- 菜单结构修复脚本
-- 执行日期：2026-02-08
-- 说明：清理重复菜单，确保菜单结构正确

-- ========================================
-- 1. 清理重复的二级区域管理菜单（已完成）
-- ========================================
-- DELETE FROM sys_menu WHERE menu_id IN (2072, 2073);
-- DELETE FROM sys_role_menu WHERE menu_id IN (2072, 2073);

-- ========================================
-- 2. 验证菜单结构
-- ========================================

-- 查看所有一级菜单
SELECT '=== 一级菜单 ===' as info;
SELECT menu_id, menu_name, path, order_num, icon
FROM sys_menu
WHERE parent_id = 0
ORDER BY order_num;

-- 查看项目管理下的所有菜单
SELECT '=== 项目管理模块 ===' as info;
SELECT m2.menu_id, m2.menu_name, m2.path, m2.component, m2.visible, m2.order_num, m2.perms
FROM sys_menu m1
JOIN sys_menu m2 ON m1.menu_id = m2.parent_id
WHERE m1.menu_name = '项目管理' AND m1.parent_id = 0
ORDER BY m2.order_num;

-- 查看市场管理下的所有菜单
SELECT '=== 市场管理模块 ===' as info;
SELECT m2.menu_id, m2.menu_name, m2.path, m2.component, m2.visible, m2.order_num, m2.perms
FROM sys_menu m1
JOIN sys_menu m2 ON m1.menu_id = m2.parent_id
WHERE m1.menu_name = '市场管理' AND m1.parent_id = 0
ORDER BY m2.order_num;

-- 查看系统管理下的项目相关菜单
SELECT '=== 系统管理下的项目相关菜单 ===' as info;
SELECT menu_id, menu_name, path, component, perms
FROM sys_menu
WHERE parent_id = 1 AND menu_name LIKE '%区域%'
ORDER BY order_num;

-- ========================================
-- 3. 检查管理员角色的菜单权限
-- ========================================

SELECT '=== 管理员角色的项目管理相关权限 ===' as info;
SELECT rm.menu_id, m.menu_name, m.path, m.perms
FROM sys_role_menu rm
JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE rm.role_id = 1
  AND m.menu_id IN (
    SELECT menu_id FROM sys_menu
    WHERE menu_name IN ('项目管理', '立项申请', '立项审核', '编辑项目', '项目详情', '市场管理', '客户信息', '二级区域管理')
    OR parent_id IN (
      SELECT menu_id FROM sys_menu WHERE menu_name IN ('项目管理', '市场管理')
    )
  )
ORDER BY m.menu_id;

-- ========================================
-- 4. 菜单结构总结
-- ========================================

SELECT '=== 菜单结构总结 ===' as info;
SELECT
  '一级菜单' as level,
  COUNT(*) as count
FROM sys_menu WHERE parent_id = 0
UNION ALL
SELECT
  '项目管理子菜单' as level,
  COUNT(*) as count
FROM sys_menu
WHERE parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name='项目管理' AND parent_id=0 LIMIT 1)
UNION ALL
SELECT
  '市场管理子菜单' as level,
  COUNT(*) as count
FROM sys_menu
WHERE parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name='市场管理' AND parent_id=0 LIMIT 1);
