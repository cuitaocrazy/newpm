-- 子项目功能：菜单及隐藏路由 SQL (使用 menu_id 2255~2263)

-- ① 列表菜单（可见）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2255, '项目分解任务', 2059, 10, 'subproject', 'project/subproject/index',
  1, 0, 'C', '0', '0', 'project:subproject:list', 'list', 'admin', NOW(), '子项目管理');

-- ② 新增页（隐藏路由）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES (2256, '子项目新增页', 2255, 1, 'subproject/add', 'project/subproject/add',
  1, 0, 'C', '1', '0', 'project:subproject:add', '#', 'admin', NOW());

-- ③ 编辑页（隐藏路由）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES (2257, '子项目编辑页', 2255, 2, 'subproject/edit/:projectId', 'project/subproject/edit',
  1, 0, 'C', '1', '0', 'project:subproject:edit', '#', 'admin', NOW());

-- ④ 详情页（隐藏路由）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES (2258, '子项目详情页', 2255, 3, 'subproject/detail/:projectId', 'project/subproject/detail',
  1, 0, 'C', '1', '0', 'project:subproject:query', '#', 'admin', NOW());

-- ⑤ 按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES
  (2259, '子项目查询', 2255, 4, '', '', 1, 0, 'F', '0', '0', 'project:subproject:query', '#', 'admin', NOW()),
  (2260, '子项目新增', 2255, 5, '', '', 1, 0, 'F', '0', '0', 'project:subproject:add',   '#', 'admin', NOW()),
  (2261, '子项目修改', 2255, 6, '', '', 1, 0, 'F', '0', '0', 'project:subproject:edit',  '#', 'admin', NOW()),
  (2262, '子项目删除', 2255, 7, '', '', 1, 0, 'F', '0', '0', 'project:subproject:remove','#', 'admin', NOW());

-- ⑥ admin 角色授权
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (1, 2255), (1, 2256), (1, 2257), (1, 2258), (1, 2259), (1, 2260), (1, 2261), (1, 2262);
