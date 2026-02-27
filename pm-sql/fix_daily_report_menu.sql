-- 日报管理模块菜单数据
-- 执行时间：2026-02-27

-- 一级菜单：日报管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('日报管理', 0, 5, 'dailyReport', NULL, 1, 0, 'M', '0', '0', '', 'date', 'admin', sysdate(), '', NULL, '日报管理目录', 'DailyReportRoot');

SELECT @dailyReportRootId := LAST_INSERT_ID();

-- 二级菜单：我的日报
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('我的日报', @dailyReportRootId, 1, 'write', 'project/dailyReport/write', 1, 0, 'C', '0', '0', 'project:dailyReport:list', 'edit', 'admin', sysdate(), '', NULL, '日报填写菜单', 'DailyReportWrite');

SELECT @dailyReportWriteId := LAST_INSERT_ID();

-- 我的日报 - 按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('日报查询', @dailyReportWriteId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:query', '#', 'admin', sysdate(), '', NULL, '');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('日报新增', @dailyReportWriteId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:add', '#', 'admin', sysdate(), '', NULL, '');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('日报修改', @dailyReportWriteId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:edit', '#', 'admin', sysdate(), '', NULL, '');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('日报删除', @dailyReportWriteId, 4, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 二级菜单：工作日报动态
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('工作日报动态', @dailyReportRootId, 2, 'activity', 'project/dailyReport/activity', 1, 0, 'C', '0', '0', 'project:dailyReport:activity', 'peoples', 'admin', sysdate(), '', NULL, '工作日报动态菜单', 'DailyReportActivity');

SELECT @dailyReportActivityId := LAST_INSERT_ID();

-- 工作日报动态 - 按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('日报动态查询', @dailyReportActivityId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:activity', '#', 'admin', sysdate(), '', NULL, '');
