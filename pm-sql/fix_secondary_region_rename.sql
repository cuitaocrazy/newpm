-- ====================================================
-- 二级区域表字段重命名（统一使用 region_* 前缀）
-- 执行时间：2026-02-11
-- 说明：将 province_* 字段重命名为 region_*，使字段名与表语义保持一致
-- ====================================================

USE ry_vue;

-- 重命名字段：province_id → region_id
ALTER TABLE `pm_secondary_region`
  CHANGE COLUMN `province_id` `region_id` bigint NOT NULL AUTO_INCREMENT COMMENT '二级区域ID',
  CHANGE COLUMN `province_code` `region_code` varchar(10) NOT NULL COMMENT '二级区域代码',
  CHANGE COLUMN `province_name` `region_name` varchar(50) NOT NULL COMMENT '二级区域名称',
  CHANGE COLUMN `province_type` `region_type` varchar(20) DEFAULT '0' COMMENT '二级区域类型（0=省/1=直辖市/2=自治区/3=特别行政区/4=计划单列市）';

-- 更新唯一键名称（可选，保持命名一致性）
ALTER TABLE `pm_secondary_region`
  DROP KEY `uk_province_code`,
  ADD UNIQUE KEY `uk_region_code` (`region_code`);

-- 验证修改结果
DESC pm_secondary_region;

SELECT '二级区域表字段重命名完成' AS status;
