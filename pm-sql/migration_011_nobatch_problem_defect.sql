-- ============================================================================
-- Feature 011 非批次任务问题单及缺陷 数据库迁移脚本
-- 迁移自老 yadapm「项目质量管理→非批次任务问题单及缺陷」(/proNoBatchListAndDefect)。
-- 独立表 pm_nobatch_prolist_defect(任务字段冗余手填) + 二级菜单 + 4权限。
-- 复用 ④ 建的3字典(sys_problem_state/sys_problem_level/sys_prolist_file_type)，不新增。
-- 附件复用 pm_attachment(business_type=nobatch_prolist)。
-- 执行：cat pm-sql/migration_011_nobatch_problem_defect.sql | \
--   ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
-- ============================================================================

-- 1. 主表（任务字段冗余实存手填，无 task_id FK；问题单=缺陷同一条记录）
CREATE TABLE IF NOT EXISTS pm_nobatch_prolist_defect (
  problem_id                bigint        NOT NULL AUTO_INCREMENT COMMENT '问题单ID',
  task_no                   varchar(64)   DEFAULT NULL COMMENT '软件中心任务号(手填)',
  task_name                 varchar(255)  DEFAULT NULL COMMENT '任务名称(手填)',
  product                   varchar(64)   DEFAULT NULL COMMENT '二级产品(字典sys_product手选)',
  internal_closure_date     date          DEFAULT NULL COMMENT '提交内部测试B包日期(手填)',
  functional_test_date      date          DEFAULT NULL COMMENT '提交功能测试版本日期(手填)',
  production_version_date    date          DEFAULT NULL COMMENT '提交生产版本日期(手填)',
  schedule_status           varchar(32)   DEFAULT NULL COMMENT '排期状态(字典sys_pqzt手选)',
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
  dept_id                   bigint        DEFAULT NULL COMMENT '部门ID(项目组手选,FK sys_dept)',
  remarks                   varchar(2048) DEFAULT NULL COMMENT '备注',
  del_flag                  char(1)       DEFAULT '0' COMMENT '删除标志(0正常1删除)',
  create_by                 varchar(64)   DEFAULT '' COMMENT '创建者',
  create_time               datetime      DEFAULT NULL COMMENT '创建时间',
  update_by                 varchar(64)   DEFAULT '' COMMENT '更新者',
  update_time               datetime      DEFAULT NULL COMMENT '更新时间',
  remark                    varchar(500)  DEFAULT NULL COMMENT '备注(系统)',
  PRIMARY KEY (problem_id),
  UNIQUE KEY uk_nobatch_problem_no (problem_no),
  KEY idx_nb_task_no (task_no),
  KEY idx_nb_batch_id (batch_id),
  KEY idx_nb_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='非批次任务问题单及缺陷';

-- 2. 菜单（项目质量管理一级下二级"非批次任务问题单及缺陷" + 4权限；幂等）
SET @quality_id = (SELECT menu_id FROM sys_menu WHERE menu_name='项目质量管理' AND parent_id=0 LIMIT 1);
SELECT IFNULL(@quality_id, 0) AS quality_parent_must_not_be_zero;

DELETE FROM sys_menu WHERE perms LIKE 'project:nobatchProlist:%';
DELETE FROM sys_menu WHERE menu_name='非批次任务问题单及缺陷' AND menu_type='C';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('非批次任务问题单及缺陷', @quality_id, 2, 'nobatchProlist', 'project/nobatchProlist/index', 'NobatchProlistDefect', 1, 0, 'C', '0', '0', 'project:nobatchProlist:list', 'bug', 'admin', NOW(), '非批次任务问题单及缺陷菜单');
SET @sub_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('非批次问题单查询', @sub_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'project:nobatchProlist:query',  '#', 'admin', NOW()),
('非批次问题单新增导出', @sub_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'project:nobatchProlist:edit',  '#', 'admin', NOW()),
('非批次问题单删除', @sub_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'project:nobatchProlist:remove','#', 'admin', NOW()),
('非批次问题单附件', @sub_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'project:nobatchProlist:file',  '#', 'admin', NOW());

COMMIT;
