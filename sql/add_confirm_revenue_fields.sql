-- 为 pm_project 表添加确认收入字段
-- 执行时间：2026-02-02

ALTER TABLE `pm_project`
ADD COLUMN `confirm_status` varchar(20) DEFAULT NULL COMMENT '确认状态(字典:sys_qrzt 未确认、待确认、已确认、无法确认)' AFTER `purchase_cost`,
ADD COLUMN `confirm_quarter` varchar(20) DEFAULT NULL COMMENT '确认季度(格式:2026年Q1,字典:sys_jdgl)' AFTER `confirm_status`,
ADD COLUMN `confirm_amount` decimal(15,2) DEFAULT NULL COMMENT '确认金额(含税)' AFTER `confirm_quarter`,
ADD COLUMN `after_tax_amount` decimal(15,2) DEFAULT NULL COMMENT '税后金额' AFTER `confirm_amount`,
ADD COLUMN `tax_rate` decimal(5,2) DEFAULT NULL COMMENT '税率(%)' AFTER `after_tax_amount`;
