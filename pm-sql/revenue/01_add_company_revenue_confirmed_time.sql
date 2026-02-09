-- 添加公司收入确认时间字段
-- 日期: 2026-02-09
-- 说明: 在 pm_project 表中添加 company_revenue_confirmed_time 字段，用于记录公司收入确认的时间

ALTER TABLE pm_project
ADD COLUMN `company_revenue_confirmed_time` datetime DEFAULT NULL COMMENT '公司收入确认时间';
