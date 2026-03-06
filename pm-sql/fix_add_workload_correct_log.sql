-- 工作量补正日志表
CREATE TABLE IF NOT EXISTS `pm_workload_correct_log` (
  `log_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `project_id`    BIGINT       NOT NULL                COMMENT '项目ID',
  `direction`     TINYINT      NOT NULL                COMMENT '调整方向(0=增加,1=减少)',
  `delta`         DECIMAL(10,2) NOT NULL               COMMENT '调整人天数',
  `before_adjust` DECIMAL(10,2) NOT NULL               COMMENT '调整前调整人天值',
  `after_adjust`  DECIMAL(10,2) NOT NULL               COMMENT '调整后调整人天值',
  `reason`        VARCHAR(500)  DEFAULT NULL            COMMENT '补正理由',
  `create_by`     VARCHAR(64)   DEFAULT ''             COMMENT '创建者',
  `create_time`   DATETIME      DEFAULT NULL            COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作量补正日志';
