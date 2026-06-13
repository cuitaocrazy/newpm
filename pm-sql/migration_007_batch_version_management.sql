-- ============================================================================
-- Feature 007 批次版本管理（出入库版本）数据库迁移脚本
-- 用途：在新环境（如 k3s 生产）一次性创建本功能所需的全部表/列/字典/菜单。
-- 幂等性：建表用 IF NOT EXISTS；列新增/字典/菜单为一次性，重复执行会报错（请勿重复跑）。
-- 执行：cat pm-sql/migration_007_batch_version_management.sql | \
--   ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
-- 执行后务必刷 Redis 字典缓存（见文件末尾）。
-- 设计文档：specs/007-batch-version-management/
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 新建表
-- ----------------------------------------------------------------------------

-- 1.1 出入库版本主表（批次+非批次共用，manual_input 区分；本期仅批次=0）
CREATE TABLE IF NOT EXISTS pm_version_out (
  id                bigint        NOT NULL AUTO_INCREMENT COMMENT '版本信息ID',
  production_year   varchar(20)   DEFAULT NULL COMMENT '投产年份(字典 sys_ndgl)',
  batch_id          bigint        DEFAULT NULL COMMENT '投产批次id(pm_production_batch)',
  pro_batch_no      varchar(64)   DEFAULT NULL COMMENT '投产批次号(冗余展示)',
  sub_version_code  varchar(64)   DEFAULT NULL COMMENT '子产品ID(字典 sys_product)',
  product           varchar(64)   DEFAULT NULL COMMENT '产品(冗余,来源 pm_sys_name)',
  version_type      varchar(8)    DEFAULT NULL COMMENT '版本类型1-6(字典 sys_version_type)',
  sys_name          varchar(64)   DEFAULT NULL COMMENT '子系统名称(pm_sys_name)',
  base_version_code varchar(64)   DEFAULT NULL COMMENT '基准版本号',
  out_lib_version   varchar(128)  DEFAULT NULL COMMENT '出入库版本号(自动生成,只读)',
  version_code      varchar(64)   DEFAULT NULL COMMENT '版本编号(生成中间值)',
  out_version       varchar(64)   DEFAULT NULL COMMENT '升级包初级版本号(类型5/6)',
  comm_name         varchar(64)   DEFAULT NULL COMMENT '提交人员(sys_user.user_id)',
  version_p_date    varchar(20)   DEFAULT NULL COMMENT '版本投产日期(只读,批次带出)',
  is_involved       char(1)       DEFAULT NULL COMMENT '是否涉及TWS改造 0是1否',
  db_update         char(1)       DEFAULT NULL COMMENT '数据库是否修改 0是1否',
  usb_update        char(1)       DEFAULT NULL COMMENT '接口是否修改 0是1否',
  package_mode      varchar(8)    DEFAULT NULL COMMENT '组包方式1-6(字典 sys_package_mode)',
  version_status    varchar(20)   DEFAULT NULL COMMENT '版本状态(字典 sys_version_status,可选)',
  version_brief     varchar(512)  DEFAULT NULL COMMENT '版本简介',
  version_descr     varchar(512)  DEFAULT NULL COMMENT '版本说明',
  remarks           varchar(2048) DEFAULT NULL COMMENT '备注(业务备注)',
  manual_input      char(1)       DEFAULT '0' COMMENT '批次标志 0批次 1非批次',
  del_flag          char(1)       DEFAULT '0' COMMENT '删除标志 0正常 1删除',
  create_by         varchar(64)   DEFAULT '' COMMENT '创建者',
  create_time       datetime      DEFAULT NULL COMMENT '创建时间',
  update_by         varchar(64)   DEFAULT '' COMMENT '更新者',
  update_time       datetime      DEFAULT NULL COMMENT '更新时间',
  remark            varchar(500)  DEFAULT NULL COMMENT '备注(框架字段)',
  PRIMARY KEY (id),
  KEY idx_year_batch (production_year, batch_id),
  KEY idx_sys_type (sys_name, version_type),
  UNIQUE KEY uk_sys_type_outlib (sys_name, version_type, out_lib_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出入库版本信息表';

-- 1.2 版本-任务关联子表（最小化：只存关联键，显示字段 JOIN 取）
CREATE TABLE IF NOT EXISTS pm_version_out_task (
  id           bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  version_id   bigint NOT NULL COMMENT '版本id(pm_version_out.id)',
  task_id      bigint DEFAULT NULL COMMENT '任务id(pm_task.task_id)',
  PRIMARY KEY (id),
  KEY idx_version_id (version_id),
  KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出入库版本任务关联表';

-- 1.3 子系统配置表
CREATE TABLE IF NOT EXISTS pm_sys_name (
  id                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  sys_name          varchar(64)  DEFAULT NULL COMMENT '子系统名称',
  base_version_code varchar(64)  DEFAULT NULL COMMENT '基准版本号',
  p_id              varchar(32)  DEFAULT NULL COMMENT '一级产品ID',
  product           varchar(64)  DEFAULT NULL COMMENT '产品名称',
  del_flag          char(1)      DEFAULT '0' COMMENT '删除标志 0正常 1删除',
  create_by         varchar(64)  DEFAULT '' COMMENT '创建者',
  create_time       datetime     DEFAULT NULL COMMENT '创建时间',
  update_by         varchar(64)  DEFAULT '' COMMENT '更新者',
  update_time       datetime     DEFAULT NULL COMMENT '更新时间',
  remark            varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_product (product)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出入库子系统配置表';

-- ----------------------------------------------------------------------------
-- 2. 既有表列新增（一次性，重复执行会报 Duplicate column）
-- ----------------------------------------------------------------------------
ALTER TABLE pm_task ADD COLUMN demand_name varchar(255) DEFAULT NULL COMMENT '需求名称';

-- ----------------------------------------------------------------------------
-- 3. 字典（版本类型 / 组包方式 / 版本状态）
-- ----------------------------------------------------------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES
('出入库版本类型', 'sys_version_type',   '0', 'admin', NOW(), '出入库版本类型1-6'),
('出入库组包方式', 'sys_package_mode',   '0', 'admin', NOW(), '组包方式1-6'),
('出入库版本状态', 'sys_version_status', '0', 'admin', NOW(), '版本状态(空字典,值待生产数据填充)');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time) VALUES
(1, 'SP升级包',   '1', 'sys_version_type', 'N', '0', 'admin', NOW()),
(2, 'PTF补丁包',  '2', 'sys_version_type', 'N', '0', 'admin', NOW()),
(3, 'B测试包',    '3', 'sys_version_type', 'N', '0', 'admin', NOW()),
(4, '临时版本包', '4', 'sys_version_type', 'N', '0', 'admin', NOW()),
(5, 'B包升级包',  '5', 'sys_version_type', 'N', '0', 'admin', NOW()),
(6, 'SP包升级包', '6', 'sys_version_type', 'N', '0', 'admin', NOW()),
(1, 'A1-本批次全量版本',   '1', 'sys_package_mode', 'N', '0', 'admin', NOW()),
(2, 'A2-本批次增量版本',   '2', 'sys_package_mode', 'N', '0', 'admin', NOW()),
(3, 'B1-单个任务全量版本', '3', 'sys_package_mode', 'N', '0', 'admin', NOW()),
(4, 'B2-单个任务增量版本', '4', 'sys_package_mode', 'N', '0', 'admin', NOW()),
(5, 'C1-多个任务全量版本', '5', 'sys_package_mode', 'N', '0', 'admin', NOW()),
(6, 'C2-多个任务增量版本', '6', 'sys_package_mode', 'N', '0', 'admin', NOW());
-- sys_version_status 不 seed 数据值（空字典，枚举来自生产库，由管理员/数据迁移填充）

-- ----------------------------------------------------------------------------
-- 4. 菜单 + 权限（用 LAST_INSERT_ID 动态取父 id，避免硬编码 menu_id）
-- ----------------------------------------------------------------------------
-- 4.1 一级菜单 出入库版本管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('出入库版本管理', 0, 8, 'storageVersion', NULL, 1, 0, 'M', '0', '0', '', 'build', 'admin', NOW(), '出入库版本管理一级菜单');
SET @parent_id = LAST_INSERT_ID();

-- 4.2 二级菜单 批次版本管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('批次版本管理', @parent_id, 1, 'versionOut', 'project/versionOut/index', 1, 0, 'C', '0', '0', 'project:versionOut:list', 'list', 'admin', NOW(), '批次版本管理菜单');
SET @sub_id = LAST_INSERT_ID();

-- 4.3 按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('批次版本查询', @sub_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'project:versionOut:query',  '#', 'admin', NOW()),
('批次版本新增', @sub_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'project:versionOut:add',    '#', 'admin', NOW()),
('批次版本修改', @sub_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'project:versionOut:edit',   '#', 'admin', NOW()),
('批次版本删除', @sub_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'project:versionOut:remove', '#', 'admin', NOW()),
('批次版本导出', @sub_id, 5, '', NULL, 1, 0, 'F', '0', '0', 'project:versionOut:export', '#', 'admin', NOW());

COMMIT;

-- ----------------------------------------------------------------------------
-- 5. 执行后操作（必须！否则前端字典下拉用旧缓存）
-- ----------------------------------------------------------------------------
-- 生产 Redis（Deployment，pod 名带 hash）：
--   ssh k3s001 "kubectl exec -i \$(kubectl get pods -n newpm -o name | grep -i redis | head -1 | cut -d/ -f2) -n newpm -- redis-cli DEL sys_dict:sys_version_type sys_dict:sys_package_mode sys_dict:sys_version_status"
-- 本地：
--   docker exec -i newpm-redis-1 redis-cli DEL sys_dict:sys_version_type sys_dict:sys_package_mode sys_dict:sys_version_status
