-- pm-sql/fix_menu_task_redesign_20260311.sql
-- 新增「项目分解任务」菜单，挂在一级「任务管理」下

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '项目分解任务', menu_id, 1, 'decompose', 'project/subproject/decompose',
       1, 0, 'C', '0', '0', 'project:subproject:add', 'list', 'admin', NOW(), '', NULL, '项目分解任务'
FROM sys_menu WHERE menu_name='任务管理' AND parent_id=0;
