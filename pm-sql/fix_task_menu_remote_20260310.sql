-- 任务管理菜单（远程服务器迁移）
-- 远程服务器最大 menu_id=2255，使用 2256~2264
-- parent_id 通过子查询动态获取「项目管理」一级菜单 ID，兼容不同环境

-- ① 列表菜单（可见）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2256, '任务管理', menu_id, 10, 'subproject', 'project/subproject/index',
  1, 0, 'C', '0', '0', 'project:subproject:list', 'list', 'admin', NOW(), '任务管理'
FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0 LIMIT 1;

-- ② 新增页（隐藏路由，挂在「项目管理」一级菜单下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2257, '任务新增页', menu_id, 1, 'subproject/add', 'project/subproject/add',
  1, 0, 'C', '1', '0', 'project:subproject:add', '#', 'admin', NOW()
FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0 LIMIT 1;

-- ③ 编辑页（隐藏路由）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2258, '任务编辑页', menu_id, 2, 'subproject/edit/:projectId', 'project/subproject/edit',
  1, 0, 'C', '1', '0', 'project:subproject:edit', '#', 'admin', NOW()
FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0 LIMIT 1;

-- ④ 详情页（隐藏路由）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2259, '任务详情页', menu_id, 3, 'subproject/detail/:projectId', 'project/subproject/detail',
  1, 0, 'C', '1', '0', 'project:subproject:query', '#', 'admin', NOW()
FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0 LIMIT 1;

-- ⑤ 按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES
  (2260, '任务查询', 2256, 4, '', '', 1, 0, 'F', '0', '0', 'project:subproject:query', '#', 'admin', NOW()),
  (2261, '任务新增', 2256, 5, '', '', 1, 0, 'F', '0', '0', 'project:subproject:add',   '#', 'admin', NOW()),
  (2262, '任务修改', 2256, 6, '', '', 1, 0, 'F', '0', '0', 'project:subproject:edit',  '#', 'admin', NOW()),
  (2263, '任务删除', 2256, 7, '', '', 1, 0, 'F', '0', '0', 'project:subproject:remove','#', 'admin', NOW());

-- ⑥ admin 角色授权
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (1, 2256), (1, 2257), (1, 2258), (1, 2259),
  (1, 2260), (1, 2261), (1, 2262), (1, 2263);
