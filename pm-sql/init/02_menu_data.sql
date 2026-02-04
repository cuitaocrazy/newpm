-- ========================================
-- 市场管理模块菜单数据
-- ========================================

-- 一级菜单：市场管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('市场管理', 0, 4, 'market', NULL, 1, 0, 'M', '0', '0', '', 'chart', 'admin', sysdate(), '', NULL, '市场管理目录');

-- 获取市场管理菜单ID
SELECT @marketMenuId := LAST_INSERT_ID();

-- 二级菜单：客户信息管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('客户信息', @marketMenuId, 1, 'customer', 'project/customer/index', 1, 0, 'C', '0', '0', 'project:customer:list', 'peoples', 'admin', sysdate(), '', NULL, '客户信息菜单');

-- 获取客户信息菜单ID
SELECT @customerMenuId := LAST_INSERT_ID();

-- 按钮权限：查询
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('客户信息查询', @customerMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:customer:query', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：新增
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('客户信息新增', @customerMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:customer:add', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：修改
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('客户信息修改', @customerMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:customer:edit', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：删除
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('客户信息删除', @customerMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'project:customer:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：导出
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('客户信息导出', @customerMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'project:customer:export', '#', 'admin', sysdate(), '', NULL, '');
