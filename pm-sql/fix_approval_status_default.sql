-- ========================================
-- 修复项目表审核状态字段默认值
-- 创建时间: 2026-02-06
-- 问题描述: approval_status 字段默认为 NULL，导致列表显示为空
-- 解决方案: 设置默认值为 '0'（待审核），并更新历史数据
-- ========================================

-- 1. 更新历史数据：将 NULL 值更新为 '0'（待审核）
UPDATE pm_project
SET approval_status = '0'
WHERE approval_status IS NULL;

-- 2. 修改字段定义：设置默认值为 '0'
ALTER TABLE pm_project
MODIFY COLUMN approval_status varchar(20) DEFAULT '0' COMMENT '审批状态(0待审核/1已通过/2已拒绝)';

-- 验证修改结果
SELECT
    COUNT(*) AS total_count,
    SUM(CASE WHEN approval_status IS NULL THEN 1 ELSE 0 END) AS null_count,
    SUM(CASE WHEN approval_status = '0' THEN 1 ELSE 0 END) AS pending_count,
    SUM(CASE WHEN approval_status = '1' THEN 1 ELSE 0 END) AS approved_count,
    SUM(CASE WHEN approval_status = '2' THEN 1 ELSE 0 END) AS rejected_count
FROM pm_project
WHERE del_flag = '0';
