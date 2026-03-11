-- pm-sql/fix_add_production_version_date_20260311.sql
-- 为 pm_project 表新增生产版本日期字段

ALTER TABLE pm_project
  ADD COLUMN `production_version_date` date DEFAULT NULL COMMENT '生产版本日期（非必填）'
  AFTER `functional_test_date`;
