-- 修复 pm_project 表字段名：province_id -> region_id
-- 执行日期: 2026-02-11

USE ry_vue;

-- 修改列名
ALTER TABLE `pm_project` CHANGE COLUMN `province_id` `region_id` bigint DEFAULT NULL COMMENT '二级区域ID（关联pm_secondary_region表）';
