-- 日报明细表新增 entry_type/leave_hours 字段 + sys_rbtype 字典
-- 日期: 2026-03-08

-- 1. pm_daily_report_detail 新增字段
ALTER TABLE pm_daily_report_detail
  MODIFY COLUMN `project_id` bigint DEFAULT NULL COMMENT '项目ID，entry_type=work时必填',
  MODIFY COLUMN `work_content` text DEFAULT NULL COMMENT '工作内容，假期行可为空',
  ADD COLUMN `entry_type` varchar(20) NOT NULL DEFAULT 'work'
      COMMENT '条目类型: work=项目工时 / leave=请假 / comp=倒休 / annual=年假'
      AFTER `project_stage`,
  ADD COLUMN `leave_hours` decimal(5,2) DEFAULT NULL
      COMMENT '假期时长(小时)，entry_type非work时使用'
      AFTER `entry_type`;

-- 2. 字典类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark, create_by, create_time)
VALUES ('日报条目类型', 'sys_rbtype', '0', '日报明细行的类型', 'admin', NOW());

-- 3. 字典值
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
VALUES
  (1, '项目工时', 'work',   'sys_rbtype', '', 'primary', 'N', '0', 'admin', NOW()),
  (2, '请假',     'leave',  'sys_rbtype', '', 'danger',  'N', '0', 'admin', NOW()),
  (3, '倒休',     'comp',   'sys_rbtype', '', 'warning', 'N', '0', 'admin', NOW()),
  (4, '年假',     'annual', 'sys_rbtype', '', 'success', 'N', '0', 'admin', NOW());
