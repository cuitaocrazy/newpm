-- ============================================
-- 客户信息管理菜单 SQL
-- ============================================
-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('客户信息管理', '3', '1', 'customer', 'project/customer/index', 1, 0, 'C', '0', '0', 'project:customer:list', 'peoples', 'admin', sysdate(), '', null, '客户信息管理菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('客户查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'project:customer:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('客户新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'project:customer:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('客户修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'project:customer:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('客户删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'project:customer:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('客户导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'project:customer:export',       '#', 'admin', sysdate(), '', null, '');

-- ============================================
-- 联系人信息管理菜单 SQL
-- ============================================
-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('联系人信息管理', '3', '2', 'contact', 'project/contact/index', 1, 0, 'C', '0', '0', 'project:contact:list', 'user', 'admin', sysdate(), '', null, '联系人信息管理菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('联系人查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'project:contact:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('联系人新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'project:contact:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('联系人修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'project:contact:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('联系人删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'project:contact:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('联系人导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'project:contact:export',       '#', 'admin', sysdate(), '', null, '');

-- ============================================
-- 项目管理菜单 SQL
-- ============================================
-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目管理', '3', '3', 'project', 'project/project/index', 1, 0, 'C', '0', '0', 'project:project:list', '#', 'admin', sysdate(), '', null, '项目管理菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目管理查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'project:project:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目管理新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'project:project:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目管理修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'project:project:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目管理删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'project:project:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目管理导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'project:project:export',       '#', 'admin', sysdate(), '', null, '');

-- ============================================
-- 项目审核菜单 SQL
-- ============================================
-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目审核', '3', '4', 'approval', 'project/approval/index', 1, 0, 'C', '0', '0', 'project:approval:list', '#', 'admin', sysdate(), '', null, '项目审核菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目审核查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'project:approval:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目审核新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'project:approval:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目审核修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'project:approval:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目审核删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'project:approval:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('项目审核导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'project:approval:export',       '#', 'admin', sysdate(), '', null, '');

-- 合同管理模块菜单和权限资源
-- 放在合同款项(menu_id=2010)下
-- 1. 合同管理菜单
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values(2058, '合同管理', '2010', '1', 'contract', 'project/contract/index', 1, 0, 'C', '0', '0', 'project:contract:list', '#', 'admin', sysdate(), '', null, '合同管理菜单');

-- 2. 合同管理按钮权限
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values(2059, '合同管理查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'project:contract:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values(2060, '合同管理新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'project:contract:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values(2061, '合同管理修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'project:contract:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values(2062, '合同管理删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'project:contract:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values(2063, '合同管理导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'project:contract:export',       '#', 'admin', sysdate(), '', null, '');