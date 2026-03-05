-- 立项年度字段类型从 int 改为 varchar(20)
-- 原因：sys_ndgl 字典值包含非纯数字值（如 "2025q"、"dd"），int 类型无法正确存储
ALTER TABLE pm_project MODIFY COLUMN established_year VARCHAR(20) DEFAULT NULL COMMENT '立项年度';
