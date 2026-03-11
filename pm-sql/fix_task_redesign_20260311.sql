-- pm-sql/fix_task_redesign_20260311.sql
-- 任务管理改造：pm_project 表字段变更
-- 删除旧的 production_batch 列，新增 batch_id 外键及相关任务字段

ALTER TABLE pm_project
  DROP COLUMN production_batch,
  ADD COLUMN `batch_id`           bigint       DEFAULT NULL COMMENT '投产批次ID(FK:pm_production_batch.batch_id)' AFTER task_code,
  ADD COLUMN `production_year`    varchar(10)  DEFAULT NULL COMMENT '投产年份(字典:sys_ndgl)'                     AFTER batch_id,
  ADD COLUMN `bank_demand_no`     varchar(100) DEFAULT NULL COMMENT '总行需求号'                                   AFTER functional_test_date,
  ADD COLUMN `software_demand_no` varchar(100) DEFAULT NULL COMMENT '软件中心需求编号'                            AFTER bank_demand_no,
  ADD COLUMN `product`            varchar(50)  DEFAULT NULL COMMENT '二级产品(字典:sys_product)'                  AFTER software_demand_no;
