-- 客户管理模块菜单和权限资源
-- 放在市场管理(menu_id=2037)下

-- 1. 客户信息管理菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2057, '客户信息管理', 2037, 1, 'customer', 'project/customer/index', '', 1, 0, 'C', '0', '0', 'project:customer:list', 'peoples', 'admin', NOW(), '', NULL, '客户信息管理菜单');

-- 2. 客户信息管理按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2058, '客户查询', 2057, 1, '#', '', '', 1, 0, 'F', '0', '0', 'project:customer:query', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2059, '客户新增', 2057, 2, '#', '', '', 1, 0, 'F', '0', '0', 'project:customer:add', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2060, '客户修改', 2057, 3, '#', '', '', 1, 0, 'F', '0', '0', 'project:customer:edit', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2061, '客户删除', 2057, 4, '#', '', '', 1, 0, 'F', '0', '0', 'project:customer:remove', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2062, '客户导出', 2057, 5, '#', '', '', 1, 0, 'F', '0', '0', 'project:customer:export', '#', 'admin', NOW(), '', NULL, '');

-- 3. 联系人信息管理菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2063, '联系人信息管理', 2037, 2, 'contact', 'project/contact/index', '', 1, 0, 'C', '0', '0', 'project:contact:list', 'user', 'admin', NOW(), '', NULL, '联系人信息管理菜单');

-- 4. 联系人信息管理按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2064, '联系人查询', 2063, 1, '#', '', '', 1, 0, 'F', '0', '0', 'project:contact:query', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2065, '联系人新增', 2063, 2, '#', '', '', 1, 0, 'F', '0', '0', 'project:contact:add', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2066, '联系人修改', 2063, 3, '#', '', '', 1, 0, 'F', '0', '0', 'project:contact:edit', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2067, '联系人删除', 2063, 4, '#', '', '', 1, 0, 'F', '0', '0', 'project:contact:remove', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2068, '联系人导出', 2063, 5, '#', '', '', 1, 0, 'F', '0', '0', 'project:contact:export', '#', 'admin', NOW(), '', NULL, '');