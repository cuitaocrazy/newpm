-- 日报模块建表 DDL（只建新表，不影响已有表）
-- 执行时间：2026-02-27

-- 1. 项目成员表
DROP TABLE IF EXISTS `pm_project_member`;
CREATE TABLE `pm_project_member` (
  `member_id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员主键ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `join_date` date DEFAULT NULL COMMENT '加入日期',
  `leave_date` date DEFAULT NULL COMMENT '离开日期',
  `is_active` char(1) DEFAULT '1' COMMENT '是否在项目中(1是 0否)',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`member_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目人员管理表';

-- 2. 日报主表
DROP TABLE IF EXISTS `pm_daily_report_detail`;
DROP TABLE IF EXISTS `pm_daily_report`;
CREATE TABLE `pm_daily_report` (
  `report_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日报主键ID',
  `report_date` date NOT NULL COMMENT '日报日期',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `total_work_hours` decimal(5,2) DEFAULT 0.00 COMMENT '当日总工时(小时,由明细自动汇总)',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `report_date`),
  KEY `idx_report_date` (`report_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日报主表';

-- 3. 日报明细表
CREATE TABLE `pm_daily_report_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细主键ID',
  `report_id` bigint NOT NULL COMMENT '日报ID(外键)',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `project_stage` varchar(50) DEFAULT NULL COMMENT '项目阶段(字典:sys_xmjd)',
  `work_hours` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT '工时(小时)',
  `work_content` text NOT NULL COMMENT '工作内容',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`detail_id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日报明细表';
