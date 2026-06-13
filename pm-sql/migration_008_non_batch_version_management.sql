-- ============================================================================
-- Feature 008 非批次版本管理 数据库迁移脚本
-- 复用 pm_version_out（manual_input='1' 区分），加 2 列存手填任务 + 独立菜单权限。
-- 执行：cat pm-sql/migration_008_non_batch_version_management.sql | \
--   ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
-- ============================================================================

-- 1. pm_version_out 加 2 列（手填任务号/任务名，非批次专用）
ALTER TABLE pm_version_out ADD COLUMN manual_task_no varchar(64) DEFAULT NULL COMMENT '手填软件中心任务号(非批次)' AFTER product;
ALTER TABLE pm_version_out ADD COLUMN manual_task_name varchar(255) DEFAULT NULL COMMENT '手填任务名称(非批次)' AFTER manual_task_no;

-- 2. 菜单 + 权限（出入库版本管理一级菜单下新增二级"非批次版本管理"）
SET @parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name='出入库版本管理' AND parent_id=0 LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('非批次版本管理', @parent_id, 2, 'versionOutManual', 'project/versionOutManual/index', 1, 0, 'C', '0', '0', 'project:versionOutManual:list', 'documentation', 'admin', NOW(), '非批次版本管理菜单');
SET @sub_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('非批次版本查询', @sub_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'project:versionOutManual:query',  '#', 'admin', NOW()),
('非批次版本新增', @sub_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'project:versionOutManual:add',    '#', 'admin', NOW()),
('非批次版本修改', @sub_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'project:versionOutManual:edit',   '#', 'admin', NOW()),
('非批次版本删除', @sub_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'project:versionOutManual:remove', '#', 'admin', NOW()),
('非批次版本导出', @sub_id, 5, '', NULL, 1, 0, 'F', '0', '0', 'project:versionOutManual:export', '#', 'admin', NOW());

COMMIT;
