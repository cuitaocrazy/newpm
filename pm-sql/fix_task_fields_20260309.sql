ALTER TABLE pm_project
  ADD COLUMN `production_batch`      varchar(50) DEFAULT NULL COMMENT '投产批次(字典:sys_tcpc)'        AFTER `task_code`,
  ADD COLUMN `internal_closure_date` date        DEFAULT NULL COMMENT '提供内部闭包日期（非必填）'      AFTER `production_batch`,
  ADD COLUMN `functional_test_date`  date        DEFAULT NULL COMMENT '提供功能测试版本日期（非必填）'  AFTER `internal_closure_date`;
