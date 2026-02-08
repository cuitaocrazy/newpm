-- ========================================
-- 修复项目详情和编辑项目菜单的 active_menu 配置
-- 用途：使隐藏路由访问时，侧边栏"项目列表"菜单高亮显示
-- 执行时间：2026-02-08
-- ========================================

-- 更新编辑项目菜单的 active_menu
UPDATE sys_menu
SET active_menu = '/project/list'
WHERE menu_name = '编辑项目'
  AND route_name = 'ProjectEdit'
  AND path = 'list/edit/:projectId(\\d+)';

-- 更新项目详情菜单的 active_menu
UPDATE sys_menu
SET active_menu = '/project/list'
WHERE menu_name = '项目详情'
  AND route_name = 'ProjectDetail'
  AND path = 'list/detail/:projectId(\\d+)';

-- 验证更新结果
SELECT menu_id, menu_name, path, route_name, active_menu, visible
FROM sys_menu
WHERE route_name IN ('ProjectEdit', 'ProjectDetail')
ORDER BY order_num;
