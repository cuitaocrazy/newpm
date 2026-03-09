-- 新增收入确认日期字段
ALTER TABLE pm_project
  ADD COLUMN `revenue_confirm_date` date DEFAULT NULL COMMENT '收入确认日期'
  AFTER `revenue_confirm_year`;
