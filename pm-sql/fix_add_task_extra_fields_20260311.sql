-- 添加任务扩展字段：排期状态、功能点说明、实施计划、实际投产日期
ALTER TABLE pm_project
    ADD COLUMN schedule_status     VARCHAR(10)  DEFAULT NULL COMMENT '排期状态（字典 sys_pqzt）' AFTER production_version_date,
    ADD COLUMN function_description TEXT         DEFAULT NULL COMMENT '功能点说明' AFTER schedule_status,
    ADD COLUMN implementation_plan  TEXT         DEFAULT NULL COMMENT '实施计划'   AFTER function_description,
    ADD COLUMN actual_production_date DATE        DEFAULT NULL COMMENT '实际投产日期' AFTER implementation_plan;
