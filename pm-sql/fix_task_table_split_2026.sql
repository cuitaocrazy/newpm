-- ============================================================
-- 任务管理表拆分迁移脚本
-- 将 pm_project.project_level=1 的任务数据迁移到 pm_task 表
-- 关键设计：保留原 project_id 作为 task_id，日报引用无需更新
-- 执行日期：2026-03-12
-- ============================================================

-- ============================================================
-- 第1部分：创建 pm_task 表
-- ============================================================
CREATE TABLE IF NOT EXISTS `pm_task` (
  `task_id`                 bigint        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `project_id`              bigint        NOT NULL               COMMENT '所属主项目ID(FK→pm_project.project_id)',
  `task_code`               varchar(50)   DEFAULT NULL           COMMENT '任务编号',
  `task_name`               varchar(200)  NOT NULL               COMMENT '任务名称',
  `task_stage`              varchar(50)   DEFAULT NULL           COMMENT '任务阶段(sys_xmjd)',
  `task_manager_id`         bigint        DEFAULT NULL           COMMENT '任务负责人ID(FK→sys_user.user_id)',
  `product`                 varchar(50)   DEFAULT NULL           COMMENT '产品(sys_product)',
  `bank_demand_no`          varchar(100)  DEFAULT NULL           COMMENT '总行需求号',
  `software_demand_no`      varchar(100)  DEFAULT NULL           COMMENT '软件中心需求编号',
  `task_budget`             decimal(15,2) DEFAULT NULL           COMMENT '任务预算(元)',
  `estimated_workload`      decimal(10,2) DEFAULT NULL           COMMENT '预估工作量(人天)',
  `actual_workload`         decimal(10,2) DEFAULT 0              COMMENT '实际工作量(小时，由日报汇总)',
  `production_year`         varchar(10)   DEFAULT NULL           COMMENT '投产年度(sys_ndgl)',
  `batch_id`                bigint        DEFAULT NULL           COMMENT '投产批次ID(FK→pm_production_batch.batch_id)',
  `task_plan`               text          DEFAULT NULL           COMMENT '任务计划',
  `task_description`        text          DEFAULT NULL           COMMENT '任务描述',
  `start_date`              date          DEFAULT NULL           COMMENT '启动日期',
  `end_date`                date          DEFAULT NULL           COMMENT '结束日期',
  `production_date`         date          DEFAULT NULL           COMMENT '投产时间',
  `production_version_date` date          DEFAULT NULL           COMMENT '生产版本日期',
  `actual_production_date`  date          DEFAULT NULL           COMMENT '实际投产日期',
  `internal_closure_date`   date          DEFAULT NULL           COMMENT '内部B包日期',
  `functional_test_date`    date          DEFAULT NULL           COMMENT '功能测试版本日期',
  `schedule_status`         varchar(50)   DEFAULT NULL           COMMENT '排期状态(sys_pqzt)',
  `function_description`    text          DEFAULT NULL           COMMENT '功能点说明',
  `implementation_plan`     text          DEFAULT NULL           COMMENT '实施计划',
  `create_by`               varchar(64)   DEFAULT NULL           COMMENT '创建者',
  `create_time`             datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`               varchar(64)   DEFAULT NULL           COMMENT '更新者',
  `update_time`             datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`                  varchar(500)  DEFAULT NULL           COMMENT '备注',
  PRIMARY KEY (`task_id`),
  KEY `idx_project_id`      (`project_id`),
  KEY `idx_task_stage`      (`task_stage`),
  KEY `idx_schedule_status` (`schedule_status`),
  KEY `idx_create_time`     (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务管理表';

-- ============================================================
-- 第2部分：从 pm_project 迁移 project_level=1 的任务数据
-- 保留 project_id 作为 task_id，日报的 sub_project_id 引用不变
-- function_description/implementation_plan/schedule_status/actual_production_date
-- 为 pm_task 新增字段，pm_project 中不存在，迁移时置 NULL
-- ============================================================
INSERT INTO `pm_task` (
  `task_id`,
  `project_id`,
  `task_code`,
  `task_name`,
  `task_stage`,
  `task_manager_id`,
  `product`,
  `bank_demand_no`,
  `software_demand_no`,
  `task_budget`,
  `estimated_workload`,
  `actual_workload`,
  `production_year`,
  `batch_id`,
  `task_plan`,
  `task_description`,
  `start_date`,
  `end_date`,
  `production_date`,
  `production_version_date`,
  `actual_production_date`,
  `internal_closure_date`,
  `functional_test_date`,
  `schedule_status`,
  `function_description`,
  `implementation_plan`,
  `create_by`,
  `create_time`,
  `update_by`,
  `update_time`,
  `remark`
)
SELECT
  p.project_id            AS task_id,
  p.parent_id             AS project_id,
  p.task_code             AS task_code,
  p.project_name          AS task_name,
  p.project_stage         AS task_stage,
  p.project_manager_id    AS task_manager_id,
  p.product               AS product,
  p.bank_demand_no        AS bank_demand_no,
  p.software_demand_no    AS software_demand_no,
  p.project_budget        AS task_budget,
  p.estimated_workload    AS estimated_workload,
  p.actual_workload       AS actual_workload,
  p.production_year       AS production_year,
  p.batch_id              AS batch_id,
  p.project_plan          AS task_plan,
  p.project_description   AS task_description,
  p.start_date            AS start_date,
  p.end_date              AS end_date,
  p.production_date       AS production_date,
  p.production_version_date AS production_version_date,
  NULL                    AS actual_production_date,
  p.internal_closure_date AS internal_closure_date,
  p.functional_test_date  AS functional_test_date,
  NULL                    AS schedule_status,
  NULL                    AS function_description,
  NULL                    AS implementation_plan,
  p.create_by             AS create_by,
  p.create_time           AS create_time,
  p.update_by             AS update_by,
  p.update_time           AS update_time,
  p.remark                AS remark
FROM `pm_project` p
WHERE p.project_level = 1
  AND p.del_flag = '0';

-- ============================================================
-- 第3部分：重置 AUTO_INCREMENT 到最大 task_id + 1
-- ============================================================
SET @max_id = (SELECT IFNULL(MAX(task_id), 0) FROM pm_task);
SET @sql = CONCAT('ALTER TABLE pm_task AUTO_INCREMENT = ', @max_id + 1);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 第4部分：验证查询（两行数字应相同）
-- ============================================================
SELECT '迁移前任务数' AS label, COUNT(*) AS cnt
FROM pm_project WHERE project_level = 1 AND del_flag = '0'
UNION ALL
SELECT '迁移后pm_task数', COUNT(*) FROM pm_task;
