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

-- ========================================
-- 二级区域管理菜单数据
-- ========================================

-- 先删除可能存在的重复菜单（防止重复添加）
DELETE FROM sys_menu WHERE menu_name = '二级区域管理' AND parent_id = 1 AND path = 'secondaryRegion';

-- 二级菜单：二级区域管理（挂在系统管理下，parent_id=1）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('二级区域管理', 1, 10, 'secondaryRegion', 'project/secondaryRegion/index', 1, 0, 'C', '0', '0', 'project:secondaryRegion:list', 'tree', 'admin', sysdate(), '', NULL, '省级区域管理菜单');

-- 获取二级区域管理菜单ID
SELECT @secondaryRegionMenuId := LAST_INSERT_ID();

-- 按钮权限：查询
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('二级区域查询', @secondaryRegionMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:secondaryRegion:query', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：新增
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('二级区域新增', @secondaryRegionMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:secondaryRegion:add', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：修改
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('二级区域修改', @secondaryRegionMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:secondaryRegion:edit', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：删除
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('二级区域删除', @secondaryRegionMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'project:secondaryRegion:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：导出
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('二级区域导出', @secondaryRegionMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'project:secondaryRegion:export', '#', 'admin', sysdate(), '', NULL, '');
-- ========================================
-- 项目管理模块菜单数据
-- ========================================

-- 先删除可能存在的重复菜单（防止重复添加）
DELETE FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0 AND path = 'project';

-- 一级菜单：项目管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('项目管理', 0, 3, 'project', NULL, 1, 0, 'M', '0', '0', '', 'guide', 'admin', sysdate(), '', NULL, '项目管理目录', 'ProjectManage');

-- 获取项目管理一级菜单ID
SELECT @projectRootMenuId := LAST_INSERT_ID();

-- 二级菜单：立项申请
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('立项申请', @projectRootMenuId, 1, 'apply', 'project/project/apply', 1, 0, 'C', '0', '0', 'project:project:add', 'edit', 'admin', sysdate(), '', NULL, '项目立项申请菜单', 'ProjectApply');

-- 二级菜单：项目管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('项目管理', @projectRootMenuId, 2, 'list', 'project/project/index', 1, 0, 'C', '0', '0', 'project:project:list', 'list', 'admin', sysdate(), '', NULL, '项目管理菜单', 'ProjectList');

-- 获取项目管理二级菜单ID
SELECT @projectMenuId := LAST_INSERT_ID();

-- 按钮权限：查询
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目管理查询', @projectMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:project:query', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：新增
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目管理新增', @projectMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:project:add', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：修改
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目管理修改', @projectMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:project:edit', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：删除
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目管理删除', @projectMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'project:project:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：导出
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目管理导出', @projectMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'project:project:export', '#', 'admin', sysdate(), '', NULL, '');

-- 隐藏菜单：编辑项目（挂在项目管理根目录下）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('编辑项目', @projectRootMenuId, 10, 'list/edit/:projectId(\\d+)', 'project/project/edit', 1, 0, 'C', '1', '0', 'project:project:edit', '#', 'admin', sysdate(), '', NULL, '编辑项目页面', 'ProjectEdit', '/project/list');

-- 隐藏菜单：项目详情（挂在项目管理根目录下）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('项目详情', @projectRootMenuId, 11, 'list/detail/:projectId(\\d+)', 'project/project/detail', 1, 0, 'C', '1', '0', 'project:project:query', '#', 'admin', sysdate(), '', NULL, '项目详情页面', 'ProjectDetail', '/project/list');