-- ============================================================================
-- Feature 010 批次任务问题单及缺陷 数据库迁移脚本
-- 迁移自老 yadapm「项目质量管理→批次任务问题单及缺陷」(/proListAndDefect)。
-- 主表 pm_prolist_defect + 3字典 + 新一级菜单「项目质量管理」+二级+4权限。附件复用 pm_attachment。
-- 执行：cat pm-sql/migration_010_batch_problem_defect.sql | \
--   ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
-- ============================================================================

-- 1. 主表（问题单=缺陷同一条记录；布尔标记+派生字段存主表；关联字段实时JOIN取，不冗余）
CREATE TABLE IF NOT EXISTS pm_prolist_defect (
  problem_id                bigint        NOT NULL AUTO_INCREMENT COMMENT '问题单ID',
  task_id                   bigint        DEFAULT NULL COMMENT '任务ID(FK→pm_task)',
  problem_no                varchar(160)  DEFAULT NULL COMMENT '问题单编号(唯一,留余量给软删_DEL_后缀)',
  problem_level             varchar(32)   DEFAULT NULL COMMENT '问题单级别(字典sys_problem_level)',
  current_status            varchar(32)   DEFAULT NULL COMMENT '当前状态(字典sys_problem_state)',
  submit_date               date          DEFAULT NULL COMMENT '提交日期',
  settle_date               date          DEFAULT NULL COMMENT '解决/关闭日期(可空)',
  verify_date               date          DEFAULT NULL COMMENT '核查日期',
  whether_defect            char(1)       DEFAULT '0' COMMENT '是否缺陷(0否1是)',
  whether_overtime          char(1)       DEFAULT '0' COMMENT '是否超时(0否1是)',
  whether_pro_recurrence    char(1)       DEFAULT '0' COMMENT '是否问题重现(0否1是)',
  whether_att_required      char(1)       DEFAULT '0' COMMENT '是否须关注(0否1是)',
  whether_update_version    char(1)       DEFAULT '0' COMMENT '是否更新版本(0否1是)',
  solution_time_over_one_day char(1)      DEFAULT '0' COMMENT '解决时间超一天(派生:0否1是)',
  defect_desc               varchar(128)  DEFAULT NULL COMMENT '缺陷说明/超时说明',
  production_year           varchar(20)   DEFAULT NULL COMMENT '投产年份(字典sys_ndgl)',
  batch_id                  bigint        DEFAULT NULL COMMENT '投产批次ID(FK→pm_production_batch)',
  dept_id                   bigint        DEFAULT NULL COMMENT '部门ID(项目组→部门,FK sys_dept)',
  remarks                   varchar(2048) DEFAULT NULL COMMENT '备注',
  del_flag                  char(1)       DEFAULT '0' COMMENT '删除标志(0正常1删除)',
  create_by                 varchar(64)   DEFAULT '' COMMENT '创建者',
  create_time               datetime      DEFAULT NULL COMMENT '创建时间',
  update_by                 varchar(64)   DEFAULT '' COMMENT '更新者',
  update_time               datetime      DEFAULT NULL COMMENT '更新时间',
  remark                    varchar(500)  DEFAULT NULL COMMENT '备注(系统)',
  PRIMARY KEY (problem_id),
  UNIQUE KEY uk_problem_no (problem_no),
  KEY idx_task_id (task_id),
  KEY idx_batch_id (batch_id),
  KEY idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='批次任务问题单及缺陷';

-- 2. 字典（照搬老 T_C_CURRENT_STATE / T_C_PROBLEM_LEVEL / T_C_PROLISTFILE_TYPE 预置值）
DELETE FROM sys_dict_data WHERE dict_type IN ('sys_problem_state','sys_problem_level','sys_prolist_file_type');
DELETE FROM sys_dict_type WHERE dict_type IN ('sys_problem_state','sys_problem_level','sys_prolist_file_type');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES
('问题单当前状态', 'sys_problem_state', '0', 'admin', NOW(), '批次任务问题单及缺陷-当前状态'),
('问题单级别', 'sys_problem_level', '0', 'admin', NOW(), '批次任务问题单及缺陷-级别'),
('问题单附件类型', 'sys_prolist_file_type', '0', 'admin', NOW(), '批次任务问题单及缺陷-附件文档类型');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(1, '已定位',     '1', 'sys_problem_state', '', 'default', 'N', '0', 'admin', NOW()),
(2, '待验证',     '2', 'sys_problem_state', '', 'warning', 'N', '0', 'admin', NOW()),
(3, '未受理',     '3', 'sys_problem_state', '', 'info',    'N', '0', 'admin', NOW()),
(4, '已受理',     '4', 'sys_problem_state', '', 'primary', 'N', '0', 'admin', NOW()),
(5, '问题已解决', '5', 'sys_problem_state', '', 'success', 'N', '0', 'admin', NOW()),
(6, '问题再现',   '6', 'sys_problem_state', '', 'danger',  'N', '0', 'admin', NOW()),
(1, '高优先级:2天内解决', '1', 'sys_problem_level', '', 'danger',  'N', '0', 'admin', NOW()),
(2, '中优先级:4天内解决', '2', 'sys_problem_level', '', 'warning', 'N', '0', 'admin', NOW()),
(3, '低优先级:6天内解决', '3', 'sys_problem_level', '', 'info',    'N', '0', 'admin', NOW()),
(1, '相关材料', '1', 'sys_prolist_file_type', '', 'default', 'Y', '0', 'admin', NOW());

-- 3. 菜单（新一级「项目质量管理」+ 二级「批次任务问题单及缺陷」+ 4权限；幂等）
-- 先删旧（按 perms 前缀 + 一级菜单名）
DELETE FROM sys_menu WHERE perms LIKE 'project:prolistDefect:%';
DELETE FROM sys_menu WHERE menu_name='批次任务问题单及缺陷' AND menu_type='C';

-- 一级菜单：项目质量管理（不存在才建）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '项目质量管理', 0, 9, 'quality', NULL, '', 1, 0, 'M', '0', '0', '', 'cascader', 'admin', NOW(), '项目质量管理目录'
FROM dual WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE menu_name='项目质量管理' AND parent_id=0) t);

SET @quality_id = (SELECT menu_id FROM sys_menu WHERE menu_name='项目质量管理' AND parent_id=0 LIMIT 1);
SELECT IFNULL(@quality_id, 0) AS quality_parent_must_not_be_zero;

-- 二级菜单：批次任务问题单及缺陷
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('批次任务问题单及缺陷', @quality_id, 1, 'prolistDefect', 'project/prolistDefect/index', 'ProlistDefect', 1, 0, 'C', '0', '0', 'project:prolistDefect:list', 'bug', 'admin', NOW(), '批次任务问题单及缺陷菜单');
SET @sub_id = LAST_INSERT_ID();

-- 4级权限按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('问题单查询', @sub_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'project:prolistDefect:query',  '#', 'admin', NOW()),
('问题单新增导出', @sub_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'project:prolistDefect:edit',  '#', 'admin', NOW()),
('问题单删除', @sub_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'project:prolistDefect:remove','#', 'admin', NOW()),
('问题单附件', @sub_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'project:prolistDefect:file',  '#', 'admin', NOW());

COMMIT;
