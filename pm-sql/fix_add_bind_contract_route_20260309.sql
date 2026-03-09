-- 新增"关联合同"隐藏路由（从项目管理列表跳转）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
SELECT '关联合同', menu_id, 13, 'list/bind-contract/:projectId(\\d+)', 'project/project/bind-contract', 1, 0, 'C', '1', '0', 'project:contract:add', '#', 'admin', NOW(), '', NULL, '关联合同页面', 'BindContract'
FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0;
