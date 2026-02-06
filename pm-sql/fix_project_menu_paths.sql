-- ========================================
-- 修正项目管理菜单的路由路径
-- 执行日期：2026-02-06
-- ========================================

-- 1. 更新立项申请菜单的 path
UPDATE sys_menu 
SET path = 'project/apply', 
    component = 'project/project/apply' 
WHERE menu_name = '立项申请' 
  AND parent_id IN (
    SELECT menu_id FROM (
      SELECT menu_id FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0
    ) AS temp
  );

-- 2. 更新项目列表菜单的 path
UPDATE sys_menu 
SET path = 'project' 
WHERE menu_name = '项目列表' 
  AND parent_id IN (
    SELECT menu_id FROM (
      SELECT menu_id FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0
    ) AS temp
  );

-- 3. 查询验证更新结果
SELECT 
  menu_id AS '菜单ID',
  menu_name AS '菜单名称',
  path AS '路由地址',
  component AS '组件路径',
  order_num AS '排序',
  CASE 
    WHEN menu_name = '立项申请' AND path = 'project/apply' AND component = 'project/project/apply' THEN '✓ 正确'
    WHEN menu_name = '项目列表' AND path = 'project' AND component = 'project/project/index' THEN '✓ 正确'
    ELSE '✗ 需要检查'
  END AS '状态'
FROM sys_menu 
WHERE menu_name IN ('立项申请', '项目列表') 
  AND parent_id IN (
    SELECT menu_id FROM (
      SELECT menu_id FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0
    ) AS temp2
  )
ORDER BY order_num;

-- ========================================
-- 预期结果：
-- 立项申请：path = 'project/apply', component = 'project/project/apply'
-- 项目列表：path = 'project', component = 'project/project/index'
-- 
-- 完整路由：
-- 立项申请：/project/project/apply
-- 项目列表：/project/project
-- ========================================
