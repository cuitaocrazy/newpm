-- 清理项目人员管理表中错误加入的市场经理
-- 仅删除那些只是市场经理角色（而非同时也是项目经理、团队负责人、或参与人）的成员记录
DELETE pm
FROM pm_project_member pm
INNER JOIN pm_project p ON pm.project_id = p.project_id
WHERE p.market_manager_id IS NOT NULL
  AND pm.user_id = p.market_manager_id
  AND (p.project_manager_id IS NULL OR p.market_manager_id != p.project_manager_id)
  AND (p.team_leader_id IS NULL OR p.market_manager_id != p.team_leader_id)
  AND NOT FIND_IN_SET(CAST(p.market_manager_id AS CHAR) COLLATE utf8mb4_0900_ai_ci, IFNULL(p.participants, ''));
