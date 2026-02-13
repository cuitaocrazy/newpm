-- 项目经理变更索引优化（安全版本）
-- 作者: Claude
-- 日期: 2026-02-13
-- 说明: 为项目经理变更功能添加数据库索引，提升查询性能
-- 注意: 使用 IF NOT EXISTS 避免重复创建

USE `ry-vue`;

-- 检查并创建索引的存储过程
DELIMITER $$

-- 1. 为 pm_project 表的 project_manager_id 添加索引（用于项目经理筛选）
DROP PROCEDURE IF EXISTS add_index_if_not_exists_1$$
CREATE PROCEDURE add_index_if_not_exists_1()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = 'ry-vue'
        AND table_name = 'pm_project'
        AND index_name = 'idx_project_manager_id'
    ) THEN
        ALTER TABLE pm_project
        ADD INDEX idx_project_manager_id (project_manager_id)
        COMMENT '项目经理ID索引，用于项目经理变更查询';
    END IF;
END$$

-- 2. 为 pm_project_manager_change 表添加复合索引（用于查询最新变更记录）
DROP PROCEDURE IF EXISTS add_index_if_not_exists_2$$
CREATE PROCEDURE add_index_if_not_exists_2()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = 'ry-vue'
        AND table_name = 'pm_project_manager_change'
        AND index_name = 'idx_project_create_time'
    ) THEN
        ALTER TABLE pm_project_manager_change
        ADD INDEX idx_project_create_time (project_id, create_time DESC, del_flag)
        COMMENT '项目ID+创建时间复合索引，用于查询项目最新变更记录';
    END IF;
END$$

-- 3. 为 pm_project_manager_change 表的 old_manager_id 添加索引（用于关联查询）
DROP PROCEDURE IF EXISTS add_index_if_not_exists_3$$
CREATE PROCEDURE add_index_if_not_exists_3()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = 'ry-vue'
        AND table_name = 'pm_project_manager_change'
        AND index_name = 'idx_old_manager_id'
    ) THEN
        ALTER TABLE pm_project_manager_change
        ADD INDEX idx_old_manager_id (old_manager_id)
        COMMENT '原项目经理ID索引，用于变更记录关联查询';
    END IF;
END$$

-- 4. 为 pm_project_manager_change 表的 new_manager_id 添加索引（用于关联查询）
DROP PROCEDURE IF EXISTS add_index_if_not_exists_4$$
CREATE PROCEDURE add_index_if_not_exists_4()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = 'ry-vue'
        AND table_name = 'pm_project_manager_change'
        AND index_name = 'idx_new_manager_id'
    ) THEN
        ALTER TABLE pm_project_manager_change
        ADD INDEX idx_new_manager_id (new_manager_id)
        COMMENT '新项目经理ID索引，用于变更记录关联查询';
    END IF;
END$$

DELIMITER ;

-- 执行存储过程
CALL add_index_if_not_exists_1();
CALL add_index_if_not_exists_2();
CALL add_index_if_not_exists_3();
CALL add_index_if_not_exists_4();

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_index_if_not_exists_1;
DROP PROCEDURE IF EXISTS add_index_if_not_exists_2;
DROP PROCEDURE IF EXISTS add_index_if_not_exists_3;
DROP PROCEDURE IF EXISTS add_index_if_not_exists_4;

-- 验证索引创建情况
SELECT
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS,
    INDEX_COMMENT
FROM information_schema.statistics
WHERE table_schema = 'ry-vue'
  AND table_name IN ('pm_project', 'pm_project_manager_change')
  AND index_name LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_COMMENT
ORDER BY TABLE_NAME, INDEX_NAME;
