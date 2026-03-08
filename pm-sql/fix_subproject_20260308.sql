-- 子项目功能：pm_project 新增 parent_id/project_level/task_code 字段

ALTER TABLE pm_project
  ADD COLUMN parent_id     bigint      DEFAULT NULL    COMMENT '父项目ID，NULL表示顶层主项目',
  ADD COLUMN project_level tinyint     NOT NULL DEFAULT 0 COMMENT '层级: 0=主项目 1=子项目',
  ADD COLUMN task_code     varchar(50) DEFAULT NULL    COMMENT '子项目编号，如 01、用户系统';

CREATE INDEX idx_pm_project_parent ON pm_project(parent_id);
