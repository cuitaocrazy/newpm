-- ============================================================
-- 修复 pm_payment 表缺少 payment_amount 字段的问题
-- 日期: 2026-02-12
-- 问题: 数据库表与 DDL 文件不一致，缺少 payment_amount 字段
-- ============================================================

-- 检查字段是否存在
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN 'payment_amount 字段已存在，无需添加'
        ELSE 'payment_amount 字段不存在，需要添加'
    END AS check_result
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'ry-vue'
  AND TABLE_NAME = 'pm_payment'
  AND COLUMN_NAME = 'payment_amount';

-- 添加 payment_amount 字段（如果不存在）
ALTER TABLE pm_payment
ADD COLUMN IF NOT EXISTS payment_amount decimal(15,2) DEFAULT '0.00' COMMENT '付款总金额'
AFTER payment_method_name;

-- 验证字段添加成功
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'ry-vue'
  AND TABLE_NAME = 'pm_payment'
  AND COLUMN_NAME = 'payment_amount';
