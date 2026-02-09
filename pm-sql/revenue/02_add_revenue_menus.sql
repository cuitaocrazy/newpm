-- ========================================
-- 收入确认管理模块菜单数据
-- ========================================

-- 一级菜单：收入确认管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('收入确认管理', 0, 6, 'revenue', NULL, 1, 0, 'M', '0', '0', '', 'money', 'admin', sysdate(), '', NULL, '收入确认管理目录');

-- 获取收入确认管理菜单ID
SELECT @revenueMenuId := LAST_INSERT_ID();

-- 二级菜单：公司收入确认
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('公司收入确认', @revenueMenuId, 1, 'company', 'revenue/company/index', 1, 0, 'C', '0', '0', 'revenue:company:list', 'money', 'admin', sysdate(), '', NULL, '公司收入确认菜单');

-- 获取公司收入确认菜单ID
SELECT @companyRevenueMenuId := LAST_INSERT_ID();

-- 按钮权限：查询
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('公司收入确认查询', @companyRevenueMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:query', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：查看
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('公司收入确认查看', @companyRevenueMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:view', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：编辑
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('公司收入确认编辑', @companyRevenueMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:edit', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：导出
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('公司收入确认导出', @companyRevenueMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:export', '#', 'admin', sysdate(), '', NULL, '');
