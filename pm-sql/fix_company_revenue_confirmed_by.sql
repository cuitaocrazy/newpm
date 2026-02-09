-- ----------------------------
-- 修改项目表的公司收入确认人字段
-- 从存储姓名改为存储用户ID
-- 日期: 2026-02-09
-- ----------------------------

USE `ry-vue`;

-- 修改字段名称和注释
ALTER TABLE `pm_project`
CHANGE COLUMN `company_revenue_confirmed_by_name` `company_revenue_confirmed_by` varchar(64) DEFAULT NULL COMMENT '公司收入确认人ID';

-- 验证修改结果
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    TABLE_SCHEMA = 'ry-vue'
    AND TABLE_NAME = 'pm_project'
    AND COLUMN_NAME = 'company_revenue_confirmed_by';
