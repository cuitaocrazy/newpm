-- ============================================================
-- 批量补全项目成员：将市场经理、销售负责人同步到 pm_project_member
-- 仅针对主项目（project_level IS NULL OR project_level = 0）
-- 已存在的记录不重复插入
-- ============================================================

-- 1. 补全市场经理
INSERT INTO pm_project_member (project_id, user_id, join_date, is_active, del_flag, create_by, create_time)
SELECT
    p.project_id,
    p.market_manager_id,
    NOW(),
    '1',
    '0',
    'admin',
    NOW()
FROM pm_project p
WHERE (p.project_level IS NULL OR p.project_level = 0)
  AND p.del_flag = '0'
  AND p.market_manager_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM pm_project_member m
      WHERE m.project_id = p.project_id
        AND m.user_id = p.market_manager_id
        AND m.del_flag = '0'
  );

-- 2. 补全销售负责人
INSERT INTO pm_project_member (project_id, user_id, join_date, is_active, del_flag, create_by, create_time)
SELECT
    p.project_id,
    p.sales_manager_id,
    NOW(),
    '1',
    '0',
    'admin',
    NOW()
FROM pm_project p
WHERE (p.project_level IS NULL OR p.project_level = 0)
  AND p.del_flag = '0'
  AND p.sales_manager_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM pm_project_member m
      WHERE m.project_id = p.project_id
        AND m.user_id = p.sales_manager_id
        AND m.del_flag = '0'
  );

-- 查看补全结果
SELECT
    '市场经理' AS type,
    COUNT(*) AS total
FROM pm_project p
WHERE (p.project_level IS NULL OR p.project_level = 0)
  AND p.del_flag = '0'
  AND p.market_manager_id IS NOT NULL
  AND EXISTS (
      SELECT 1 FROM pm_project_member m
      WHERE m.project_id = p.project_id
        AND m.user_id = p.market_manager_id
        AND m.del_flag = '0'
  )
UNION ALL
SELECT
    '销售负责人',
    COUNT(*)
FROM pm_project p
WHERE (p.project_level IS NULL OR p.project_level = 0)
  AND p.del_flag = '0'
  AND p.sales_manager_id IS NOT NULL
  AND EXISTS (
      SELECT 1 FROM pm_project_member m
      WHERE m.project_id = p.project_id
        AND m.user_id = p.sales_manager_id
        AND m.del_flag = '0'
  );
