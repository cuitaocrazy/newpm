-- 修复子任务 project_status 为 NULL 的问题，补设为启用状态
UPDATE pm_project SET project_status = '0' WHERE project_level = 1 AND project_status IS NULL;
