-- 日报明细表新增子项目ID和工作任务类别字段
-- 对应 feat: 日报填写支持子项目选择和工作任务类别

ALTER TABLE pm_daily_report_detail
  ADD COLUMN `sub_project_id` bigint DEFAULT NULL COMMENT '子项目ID（project_level=1）' AFTER `work_content`,
  ADD COLUMN `work_category` varchar(50) DEFAULT NULL COMMENT '工作任务类别 sys_gzlb' AFTER `sub_project_id`;
