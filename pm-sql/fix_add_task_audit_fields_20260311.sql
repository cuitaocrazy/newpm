-- pm-sql/fix_add_task_audit_fields_20260311.sql
-- 为 pm_project 表新增任务专属审计字段

ALTER TABLE pm_project
  ADD COLUMN `task_create_by`   varchar(64) DEFAULT NULL COMMENT '任务创建人'   AFTER `production_version_date`,
  ADD COLUMN `task_create_time` datetime    DEFAULT NULL COMMENT '任务创建时间' AFTER `task_create_by`,
  ADD COLUMN `task_update_by`   varchar(64) DEFAULT NULL COMMENT '任务更新人'   AFTER `task_create_time`,
  ADD COLUMN `task_update_time` datetime    DEFAULT NULL COMMENT '任务更新时间' AFTER `task_update_by`;
