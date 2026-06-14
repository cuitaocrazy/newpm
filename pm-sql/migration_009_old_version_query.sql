-- ============================================================================
-- Feature 009 旧数据查询 数据库迁移脚本
-- 独立只读历史归档表 pm_old_version_out（扁平快照，字段照搬老 T_B_OLD_VERSION_OUT）+ 菜单。
-- 执行：cat pm-sql/migration_009_old_version_query.sql | \
--   ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
-- ============================================================================

-- 1. 旧版本归档表（纯只读，数据来自迁移；无 del_flag/审计，扁平快照）
CREATE TABLE IF NOT EXISTS pm_old_version_out (
  id                bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  sys_name          varchar(128)  DEFAULT NULL COMMENT '子系统名称',
  product           varchar(64)   DEFAULT NULL COMMENT '子产品名称',
  base_version_code varchar(64)   DEFAULT NULL COMMENT '基准版本号',
  out_lib_version   varchar(128)  DEFAULT NULL COMMENT '出入库版本号',
  version_type      varchar(64)   DEFAULT NULL COMMENT '版本类型(历史文本)',
  version_code      varchar(64)   DEFAULT NULL COMMENT '版本编号',
  comm_name         varchar(64)   DEFAULT NULL COMMENT '提交人员(历史文本,非user_id)',
  version_p_date    varchar(20)   DEFAULT NULL COMMENT '版本投产日期',
  version_descr     varchar(512)  DEFAULT NULL COMMENT '版本说明',
  remarks           varchar(2048) DEFAULT NULL COMMENT '备注',
  task_no           varchar(64)   DEFAULT NULL COMMENT '任务编号',
  task_name         varchar(255)  DEFAULT NULL COMMENT '任务名称',
  pro_year          varchar(20)   DEFAULT NULL COMMENT '投产年份',
  pro_batch_no      varchar(64)   DEFAULT NULL COMMENT '投产批次号',
  is_involved       varchar(8)    DEFAULT NULL COMMENT '是否涉及TWS改造',
  db_update         varchar(8)    DEFAULT NULL COMMENT '数据库是否修改',
  usb_update        varchar(8)    DEFAULT NULL COMMENT '接口是否修改',
  sequence_no       varchar(32)   DEFAULT NULL COMMENT '顺序号',
  PRIMARY KEY (id),
  KEY idx_task_no (task_no),
  KEY idx_pro_batch_no (pro_batch_no),
  KEY idx_product (product)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出入库版本旧数据归档表';

-- 2. 菜单 + 权限（出入库版本管理一级菜单下二级"旧数据查询"，单一查询权限）
SET @parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name='出入库版本管理' AND parent_id=0 LIMIT 1);

-- 父菜单存在性校验：为空则中止，避免插出 parent_id=NULL 的孤儿菜单（M1）
-- 下面这句若 @parent_id 为 NULL 会因 1062/NULL 列约束或人工肉眼检查行数发现，请执行前确认结果非空：
SELECT IFNULL(@parent_id, 0) AS parent_id_check_must_not_be_zero;

-- 幂等：重复执行先清旧菜单/权限再插，避免重复菜单（M2，对齐 02_menu_data.sql 约定）
DELETE FROM sys_menu WHERE perms='project:oldVersionOut:list';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('旧数据查询', @parent_id, 3, 'oldVersionOut', 'project/oldVersionOut/index', 'OldVersionOut', 1, 0, 'C', '0', '0', 'project:oldVersionOut:list', 'time-range', 'admin', NOW(), '旧数据查询菜单');
SET @sub_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('旧数据查询权限', @sub_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'project:oldVersionOut:list', '#', 'admin', NOW());

COMMIT;
