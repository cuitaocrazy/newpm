-- ----------------------------
-- 项目信息管理DDL
-- 数据库: ry-vue
-- ----------------------------

-- ----------------------------
-- 一、系统基础模块
-- ----------------------------

-- 禁用外键检查
SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- 1、部门表
-- ----------------------------
drop table if exists sys_dept;
create table sys_dept (
  dept_id           bigint(20)      not null auto_increment    comment '部门id',
  parent_id         bigint(20)      default 0                  comment '父部门id',
  ancestors         varchar(50)     default ''                 comment '祖级列表',
  dept_name         varchar(30)     default ''                 comment '部门名称',
  order_num         int(4)          default 0                  comment '显示顺序',
  leader            varchar(20)     default null               comment '负责人',
  phone             varchar(11)     default null               comment '联系电话',
  email             varchar(50)     default null               comment '邮箱',
  status            char(1)         default '0'                comment '部门状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time 	    datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  primary key (dept_id)
) engine=innodb auto_increment=200 comment = '部门表';


-- ----------------------------
-- 2、用户信息表
-- ----------------------------
drop table if exists sys_user;
create table sys_user (
  user_id           bigint(20)      not null auto_increment    comment '用户ID',
  dept_id           bigint(20)      default null               comment '部门ID',
  user_name         varchar(30)     not null                   comment '用户账号',
  nick_name         varchar(30)     not null                   comment '用户昵称',
  user_type         varchar(2)      default '00'               comment '用户类型（00系统用户）',
  email             varchar(50)     default ''                 comment '用户邮箱',
  phonenumber       varchar(11)     default ''                 comment '手机号码',
  sex               char(1)         default '0'                comment '用户性别（0男 1女 2未知）',
  avatar            varchar(100)    default ''                 comment '头像地址',
  password          varchar(100)    default ''                 comment '密码',
  status            char(1)         default '0'                comment '账号状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  login_ip          varchar(128)    default ''                 comment '最后登录IP',
  login_date        datetime                                   comment '最后登录时间',
  pwd_update_date   datetime                                   comment '密码最后更新时间',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (user_id)
) engine=innodb auto_increment=100 comment = '用户信息表';


-- ----------------------------
-- 3、岗位信息表
-- ----------------------------
drop table if exists sys_post;
create table sys_post
(
  post_id       bigint(20)      not null auto_increment    comment '岗位ID',
  post_code     varchar(64)     not null                   comment '岗位编码',
  post_name     varchar(50)     not null                   comment '岗位名称',
  post_sort     int(4)          not null                   comment '显示顺序',
  status        char(1)         not null                   comment '状态（0正常 1停用）',
  create_by     varchar(64)     default ''                 comment '创建者',
  create_time   datetime                                   comment '创建时间',
  update_by     varchar(64)     default ''			       comment '更新者',
  update_time   datetime                                   comment '更新时间',
  remark        varchar(500)    default null               comment '备注',
  primary key (post_id)
) engine=innodb comment = '岗位信息表';


-- ----------------------------
-- 4、角色信息表
-- ----------------------------
drop table if exists sys_role;
create table sys_role (
  role_id              bigint(20)      not null auto_increment    comment '角色ID',
  role_name            varchar(30)     not null                   comment '角色名称',
  role_key             varchar(100)    not null                   comment '角色权限字符串',
  role_sort            int(4)          not null                   comment '显示顺序',
  data_scope           char(1)         default '1'                comment '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  menu_check_strictly  tinyint(1)      default 1                  comment '菜单树选择项是否关联显示',
  dept_check_strictly  tinyint(1)      default 1                  comment '部门树选择项是否关联显示',
  status               char(1)         not null                   comment '角色状态（0正常 1停用）',
  del_flag             char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by            varchar(64)     default ''                 comment '创建者',
  create_time          datetime                                   comment '创建时间',
  update_by            varchar(64)     default ''                 comment '更新者',
  update_time          datetime                                   comment '更新时间',
  remark               varchar(500)    default null               comment '备注',
  primary key (role_id)
) engine=innodb auto_increment=100 comment = '角色信息表';


-- ----------------------------
-- 5、菜单权限表
-- ----------------------------
drop table if exists sys_menu;
create table sys_menu (
  menu_id           bigint(20)      not null auto_increment    comment '菜单ID',
  menu_name         varchar(50)     not null                   comment '菜单名称',
  parent_id         bigint(20)      default 0                  comment '父菜单ID',
  order_num         int(4)          default 0                  comment '显示顺序',
  path              varchar(200)    default ''                 comment '路由地址',
  component         varchar(255)    default null               comment '组件路径',
  query             varchar(255)    default null               comment '路由参数',
  route_name        varchar(50)     default ''                 comment '路由名称',
  is_frame          int(1)          default 1                  comment '是否为外链（0是 1否）',
  is_cache          int(1)          default 0                  comment '是否缓存（0缓存 1不缓存）',
  menu_type         char(1)         default ''                 comment '菜单类型（M目录 C菜单 F按钮）',
  visible           char(1)         default 0                  comment '菜单状态（0显示 1隐藏）',
  status            char(1)         default 0                  comment '菜单状态（0正常 1停用）',
  perms             varchar(100)    default null               comment '权限标识',
  icon              varchar(100)    default '#'                comment '菜单图标',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default ''                 comment '备注',
  primary key (menu_id)
) engine=innodb auto_increment=2000 comment = '菜单权限表';

-- ----------------------------
-- 6、用户和角色关联表  用户N-1角色
-- ----------------------------
drop table if exists sys_user_role;
create table sys_user_role (
  user_id   bigint(20) not null comment '用户ID',
  role_id   bigint(20) not null comment '角色ID',
  primary key(user_id, role_id)
) engine=innodb comment = '用户和角色关联表';


-- ----------------------------
-- 7、角色和菜单关联表  角色1-N菜单
-- ----------------------------
drop table if exists sys_role_menu;
create table sys_role_menu (
  role_id   bigint(20) not null comment '角色ID',
  menu_id   bigint(20) not null comment '菜单ID',
  primary key(role_id, menu_id)
) engine=innodb comment = '角色和菜单关联表';


-- ----------------------------
-- 8、角色和部门关联表  角色1-N部门
-- ----------------------------
drop table if exists sys_role_dept;
create table sys_role_dept (
  role_id   bigint(20) not null comment '角色ID',
  dept_id   bigint(20) not null comment '部门ID',
  primary key(role_id, dept_id)
) engine=innodb comment = '角色和部门关联表';


-- ----------------------------
-- 9、用户与岗位关联表  用户1-N岗位
-- ----------------------------
drop table if exists sys_user_post;
create table sys_user_post
(
  user_id   bigint(20) not null comment '用户ID',
  post_id   bigint(20) not null comment '岗位ID',
  primary key (user_id, post_id)
) engine=innodb comment = '用户与岗位关联表';


-- ----------------------------
-- 10、操作日志记录
-- ----------------------------
drop table if exists sys_oper_log;
create table sys_oper_log (
  oper_id           bigint(20)      not null auto_increment    comment '日志主键',
  title             varchar(50)     default ''                 comment '模块标题',
  business_type     int(2)          default 0                  comment '业务类型（0其它 1新增 2修改 3删除）',
  method            varchar(200)    default ''                 comment '方法名称',
  request_method    varchar(10)     default ''                 comment '请求方式',
  operator_type     int(1)          default 0                  comment '操作类别（0其它 1后台用户 2手机端用户）',
  oper_name         varchar(50)     default ''                 comment '操作人员',
  dept_name         varchar(50)     default ''                 comment '部门名称',
  oper_url          varchar(255)    default ''                 comment '请求URL',
  oper_ip           varchar(128)    default ''                 comment '主机地址',
  oper_location     varchar(255)    default ''                 comment '操作地点',
  oper_param        varchar(2000)   default ''                 comment '请求参数',
  json_result       varchar(2000)   default ''                 comment '返回参数',
  status            int(1)          default 0                  comment '操作状态（0正常 1异常）',
  error_msg         varchar(2000)   default ''                 comment '错误消息',
  oper_time         datetime                                   comment '操作时间',
  cost_time         bigint(20)      default 0                  comment '消耗时间',
  primary key (oper_id),
  key idx_sys_oper_log_bt (business_type),
  key idx_sys_oper_log_s  (status),
  key idx_sys_oper_log_ot (oper_time)
) engine=innodb auto_increment=100 comment = '操作日志记录';


-- ----------------------------
-- 11、字典类型表
-- ----------------------------
drop table if exists sys_dict_type;
create table sys_dict_type
(
  dict_id          bigint(20)      not null auto_increment    comment '字典主键',
  dict_name        varchar(100)    default ''                 comment '字典名称',
  dict_type        varchar(100)    default ''                 comment '字典类型',
  status           char(1)         default '0'                comment '状态（0正常 1停用）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (dict_id),
  unique (dict_type)
) engine=innodb auto_increment=100 comment = '字典类型表';


-- ----------------------------
-- 12、字典数据表
-- ----------------------------
drop table if exists sys_dict_data;
create table sys_dict_data
(
  dict_code        bigint(20)      not null auto_increment    comment '字典编码',
  dict_sort        int(4)          default 0                  comment '字典排序',
  dict_label       varchar(100)    default ''                 comment '字典标签',
  dict_value       varchar(100)    default ''                 comment '字典键值',
  dict_type        varchar(100)    default ''                 comment '字典类型',
  css_class        varchar(100)    default null               comment '样式属性（其他样式扩展）',
  list_class       varchar(100)    default null               comment '表格回显样式',
  is_default       char(1)         default 'N'                comment '是否默认（Y是 N否）',
  status           char(1)         default '0'                comment '状态（0正常 1停用）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (dict_code)
) engine=innodb auto_increment=100 comment = '字典数据表';


-- ----------------------------
-- 13、参数配置表
-- ----------------------------
drop table if exists sys_config;
create table sys_config (
  config_id         int(5)          not null auto_increment    comment '参数主键',
  config_name       varchar(100)    default ''                 comment '参数名称',
  config_key        varchar(100)    default ''                 comment '参数键名',
  config_value      varchar(500)    default ''                 comment '参数键值',
  config_type       char(1)         default 'N'                comment '系统内置（Y是 N否）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (config_id)
) engine=innodb auto_increment=100 comment = '参数配置表';


-- ----------------------------
-- 14、系统访问记录
-- ----------------------------
drop table if exists sys_logininfor;
create table sys_logininfor (
  info_id        bigint(20)     not null auto_increment   comment '访问ID',
  user_name      varchar(50)    default ''                comment '用户账号',
  ipaddr         varchar(128)   default ''                comment '登录IP地址',
  login_location varchar(255)   default ''                comment '登录地点',
  browser        varchar(50)    default ''                comment '浏览器类型',
  os             varchar(50)    default ''                comment '操作系统',
  status         char(1)        default '0'               comment '登录状态（0成功 1失败）',
  msg            varchar(255)   default ''                comment '提示消息',
  login_time     datetime                                 comment '访问时间',
  primary key (info_id),
  key idx_sys_logininfor_s  (status),
  key idx_sys_logininfor_lt (login_time)
) engine=innodb auto_increment=100 comment = '系统访问记录';


-- ----------------------------
-- 15、定时任务调度表
-- ----------------------------
drop table if exists sys_job;
create table sys_job (
  job_id              bigint(20)    not null auto_increment    comment '任务ID',
  job_name            varchar(64)   default ''                 comment '任务名称',
  job_group           varchar(64)   default 'DEFAULT'          comment '任务组名',
  invoke_target       varchar(500)  not null                   comment '调用目标字符串',
  cron_expression     varchar(255)  default ''                 comment 'cron执行表达式',
  misfire_policy      varchar(20)   default '3'                comment '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  concurrent          char(1)       default '1'                comment '是否并发执行（0允许 1禁止）',
  status              char(1)       default '0'                comment '状态（0正常 1暂停）',
  create_by           varchar(64)   default ''                 comment '创建者',
  create_time         datetime                                 comment '创建时间',
  update_by           varchar(64)   default ''                 comment '更新者',
  update_time         datetime                                 comment '更新时间',
  remark              varchar(500)  default ''                 comment '备注信息',
  primary key (job_id, job_name, job_group)
) engine=innodb auto_increment=100 comment = '定时任务调度表';


-- ----------------------------
-- 16、定时任务调度日志表
-- ----------------------------
drop table if exists sys_job_log;
create table sys_job_log (
  job_log_id          bigint(20)     not null auto_increment    comment '任务日志ID',
  job_name            varchar(64)    not null                   comment '任务名称',
  job_group           varchar(64)    not null                   comment '任务组名',
  invoke_target       varchar(500)   not null                   comment '调用目标字符串',
  job_message         varchar(500)                              comment '日志信息',
  status              char(1)        default '0'                comment '执行状态（0正常 1失败）',
  exception_info      varchar(2000)  default ''                 comment '异常信息',
  create_time         datetime                                  comment '创建时间',
  primary key (job_log_id)
) engine=innodb comment = '定时任务调度日志表';


-- ----------------------------
-- 17、通知公告表
-- ----------------------------
drop table if exists sys_notice;
create table sys_notice (
  notice_id         int(4)          not null auto_increment    comment '公告ID',
  notice_title      varchar(50)     not null                   comment '公告标题',
  notice_type       char(1)         not null                   comment '公告类型（1通知 2公告）',
  notice_content    longblob        default null               comment '公告内容',
  status            char(1)         default '0'                comment '公告状态（0正常 1关闭）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(255)    default null               comment '备注',
  primary key (notice_id)
) engine=innodb auto_increment=10 comment = '通知公告表';


-- ----------------------------
-- 18、代码生成业务表
-- ----------------------------
drop table if exists gen_table;
create table gen_table (
  table_id          bigint(20)      not null auto_increment    comment '编号',
  table_name        varchar(200)    default ''                 comment '表名称',
  table_comment     varchar(500)    default ''                 comment '表描述',
  sub_table_name    varchar(64)     default null               comment '关联子表的表名',
  sub_table_fk_name varchar(64)     default null               comment '子表关联的外键名',
  class_name        varchar(100)    default ''                 comment '实体类名称',
  tpl_category      varchar(200)    default 'crud'             comment '使用的模板（crud单表操作 tree树表操作）',
  tpl_web_type      varchar(30)     default ''                 comment '前端模板类型（element-ui模版 element-plus模版）',
  package_name      varchar(100)                               comment '生成包路径',
  module_name       varchar(30)                                comment '生成模块名',
  business_name     varchar(30)                                comment '生成业务名',
  function_name     varchar(50)                                comment '生成功能名',
  function_author   varchar(50)                                comment '生成功能作者',
  gen_type          char(1)         default '0'                comment '生成代码方式（0zip压缩包 1自定义路径）',
  gen_path          varchar(200)    default '/'                comment '生成路径（不填默认项目路径）',
  options           varchar(1000)                              comment '其它生成选项',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time 	    datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (table_id)
) engine=innodb auto_increment=1 comment = '代码生成业务表';


-- ----------------------------
-- 19、代码生成业务表字段
-- ----------------------------
drop table if exists gen_table_column;
create table gen_table_column (
  column_id         bigint(20)      not null auto_increment    comment '编号',
  table_id          bigint(20)                                 comment '归属表编号',
  column_name       varchar(200)                               comment '列名称',
  column_comment    varchar(500)                               comment '列描述',
  column_type       varchar(100)                               comment '列类型',
  java_type         varchar(500)                               comment 'JAVA类型',
  java_field        varchar(200)                               comment 'JAVA字段名',
  is_pk             char(1)                                    comment '是否主键（1是）',
  is_increment      char(1)                                    comment '是否自增（1是）',
  is_required       char(1)                                    comment '是否必填（1是）',
  is_insert         char(1)                                    comment '是否为插入字段（1是）',
  is_edit           char(1)                                    comment '是否编辑字段（1是）',
  is_list           char(1)                                    comment '是否列表字段（1是）',
  is_query          char(1)                                    comment '是否查询字段（1是）',
  query_type        varchar(200)    default 'EQ'               comment '查询方式（等于、不等于、大于、小于、范围）',
  html_type         varchar(200)                               comment '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  dict_type         varchar(200)    default ''                 comment '字典类型',
  sort              int                                        comment '排序',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time 	    datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  primary key (column_id)
) engine=innodb auto_increment=1 comment = '代码生成业务表字段';


-- ----------------------------
-- 20、代码生成业务表字段
-- ----------------------------
DROP TABLE IF EXISTS `附件表`;
CREATE TABLE `pm_attachment` (
  `attachment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件主键ID',
  `business_type` varchar(50) NOT NULL COMMENT '业务类型(payment-款项、contract-合同、project-项目)',
  `business_id` bigint NOT NULL COMMENT '业务ID',
  `project_id` varchar(50) DEFAULT NULL COMMENT '项目ID(关联项目时)',
  `contract_id` bigint DEFAULT NULL COMMENT '合同ID(关联合同时)',
  `document_type` varchar(50) DEFAULT NULL COMMENT '文档类型(字典:sys_wdlx)',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型(扩展名)',
  `file_description` varchar(500) DEFAULT NULL COMMENT '文件描述',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`attachment_id`),
  KEY `idx_business_type_id` (`business_type`,`business_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='附件表';


-- ----------------------------
-- 21、Blob类型的触发器表
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `blob_data` blob COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Blob类型的触发器表';

-- ----------------------------
-- 22、日历信息表
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日历信息表';

-- ----------------------------
-- 23、Cron类型的触发器表
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `cron_expression` varchar(200) NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cron类型的触发器表';

-- ----------------------------
-- 24、已触发的触发器表
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `instance_name` varchar(200) NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint NOT NULL COMMENT '触发的时间',
  `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
  `priority` int NOT NULL COMMENT '优先级',
  `state` varchar(16) NOT NULL COMMENT '状态',
  `job_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已触发的触发器表';


-- ----------------------------
-- 25、任务详细信息表
-- ----------------------------
DROP TABLE IF EXISTS `任务详细信息表`;
CREATE TABLE `qrtz_job_details` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) NOT NULL COMMENT '任务组名',
  `description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) NOT NULL COMMENT '是否接受恢复执行',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务详细信息表';


-- ----------------------------
-- 26、存储的悲观锁信息表
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='存储的悲观锁信息表';


-- ----------------------------
-- 27、暂停的触发器表
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='暂停的触发器表';


-- ----------------------------
-- 28、调度器状态表
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调度器状态表';

-- ----------------------------
-- 29、简单触发器的信息表
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简单触发器的信息表';

-- ----------------------------
-- 30、同步机制的行锁表
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `str_prop_1` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `str_prop_2` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `str_prop_3` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `int_prop_1` int DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `int_prop_2` int DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `long_prop_1` bigint DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `long_prop_2` bigint DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `bool_prop_1` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `bool_prop_2` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='同步机制的行锁表';

-- ----------------------------
-- 31、触发器详细信息表
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
  `job_group` varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
  `description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) NOT NULL COMMENT '触发器的类型',
  `start_time` bigint NOT NULL COMMENT '开始时间',
  `end_time` bigint DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  KEY `sched_name` (`sched_name`,`job_name`,`job_group`),
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='触发器详细信息表';
-- 启用外键检查
SET FOREIGN_KEY_CHECKS=1;


-- ----------------------------
-- 二 、项目管理模块
-- ----------------------------


-- ----------------------------
-- 1、合同管理表
-- ----------------------------
DROP TABLE IF EXISTS `pm_contract`;
CREATE TABLE `pm_contract` (
  `contract_id` bigint NOT NULL AUTO_INCREMENT COMMENT '合同主键ID',
  `contract_code` varchar(100) DEFAULT NULL COMMENT '合同编号',
  `contract_name` varchar(200) NOT NULL COMMENT '合同名称',
  `customer_id` bigint NOT NULL COMMENT '关联客户ID',
  `customer_name` varchar(200) DEFAULT NULL COMMENT '客户名称',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `team_id` bigint DEFAULT NULL COMMENT '合同所属团队ID',
  `team_leader_id` bigint DEFAULT NULL COMMENT '团队负责人ID',
  `contract_type` varchar(50) NOT NULL COMMENT '合同类型(字典:sys_htlx)',
  `contract_status` varchar(50) DEFAULT '未签署' COMMENT '合同状态(字典:sys_htzt)',
  `contract_sign_date` date DEFAULT NULL COMMENT '合同签订日期',
  `contract_period` int DEFAULT NULL COMMENT '合同周期(月)',
  `contract_amount` decimal(15,2) NOT NULL COMMENT '合同金额(含税)',
  `tax_rate` decimal(5,2) DEFAULT NULL COMMENT '税率(%)',
  `amount_no_tax` decimal(15,2) DEFAULT NULL COMMENT '不含税金额',
  `tax_amount` decimal(15,2) DEFAULT NULL COMMENT '税金',
  `confirm_amount` decimal(15,2) DEFAULT '0.00' COMMENT '合同确认金额',
  `confirm_year` varchar(10) DEFAULT NULL COMMENT '确认年份(字典:sys_ndgl)',
  `free_maintenance_period` int DEFAULT NULL COMMENT '免维期(月)',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `reserved_field1` varchar(64) DEFAULT NULL COMMENT '备用域1',
  `reserved_field2` varchar(64) DEFAULT NULL COMMENT '备用域2',
  `reserved_field3` varchar(64) DEFAULT NULL COMMENT '备用域3',
  `reserved_field4` varchar(64) DEFAULT NULL COMMENT '备用域4',
  `reserved_field5` varchar(64) DEFAULT NULL COMMENT '备用域5',
  PRIMARY KEY (`contract_id`),
  UNIQUE KEY `uk_contract_code` (`contract_code`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_contract_status` (`contract_status`),
  KEY `idx_contract_sign_date` (`contract_sign_date`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='合同管理表';

-- ----------------------------
-- 2、客户信息表
-- ----------------------------
DROP TABLE IF EXISTS `pm_customer`;
CREATE TABLE `pm_customer` (
  `customer_id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户主键ID',
  `customer_simple_name` varchar(200) NOT NULL COMMENT '客户简称',
  `customer_all_name` varchar(200) NOT NULL COMMENT '客户全称',
  `industry` varchar(100) DEFAULT NULL COMMENT '所属行业',
  `region` varchar(100) DEFAULT NULL COMMENT '所属区域',
  `sales_manager_id` bigint DEFAULT NULL COMMENT '销售负责人ID',
  `office_address` varchar(500) DEFAULT NULL COMMENT '办公地址',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`customer_id`),
  KEY `idx_sales_manager_id` (`sales_manager_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户信息表';

-- ----------------------------
-- 3、客户联系人表
-- ----------------------------
DROP TABLE IF EXISTS `pm_customer_contact`;
CREATE TABLE `pm_customer_contact` (
  `contact_id` bigint NOT NULL AUTO_INCREMENT COMMENT '联系人主键ID',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `contact_name` varchar(100) NOT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(50) NOT NULL COMMENT '联系人电话',
  `contact_tag` varchar(100) NOT NULL COMMENT '联系人标签',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`contact_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户联系人表';

-- --------------------------
-- 4、款项表
-- ----------------------------
DROP TABLE IF EXISTS `pm_payment`;
CREATE TABLE `pm_payment` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '款项主键ID',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `payment_method_name` varchar(200) DEFAULT NULL COMMENT '付款方式名称',
  `has_penalty` char(1) DEFAULT '0' COMMENT '是否涉及违约扣款(1是 0否)',
  `penalty_amount` decimal(15,2) DEFAULT '0.00' COMMENT '扣款金额(元)',
  `payment_status` varchar(50) DEFAULT NULL COMMENT '付款状态(字典:sys_fkzt)',
  `expected_quarter` varchar(20) DEFAULT NULL COMMENT '预计回款所属季度(字典:sys_jdgl)',
  `actual_quarter` varchar(20) DEFAULT NULL COMMENT '实际回款所属季度(字典:sys_jdgl)',
  `submit_acceptance_date` date DEFAULT NULL COMMENT '提交验收材料日期',
  `actual_payment_date` date DEFAULT NULL COMMENT '实际回款日期',
  `flow_status` varchar(50) DEFAULT NULL COMMENT '流程状态',
  `confirm_year` varchar(10) DEFAULT NULL COMMENT '款项确认年份(字典:sys_ndgl)',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`payment_id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_payment_status` (`payment_status`),
  KEY `idx_expected_quarter` (`expected_quarter`),
  KEY `idx_actual_quarter` (`actual_quarter`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='款项表';

-- ----------------------------
-- 5、款项里程碑表
-- ----------------------------
DROP TABLE IF EXISTS `pm_payment_milestone`;
CREATE TABLE `pm_payment_milestone` (
  `milestone_id` bigint NOT NULL AUTO_INCREMENT COMMENT '里程碑主键ID',
  `payment_id` bigint NOT NULL COMMENT '款项ID',
  `milestone_name` varchar(200) NOT NULL COMMENT '里程碑名称',
  `milestone_amount` decimal(15,2) NOT NULL COMMENT '里程碑金额(元)',
  `milestone_order` int DEFAULT '0' COMMENT '里程碑排序',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`milestone_id`),
  KEY `idx_payment_id` (`payment_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='款项里程碑表';

-- ----------------------------
-- 6、项目管理表
-- ----------------------------
DROP TABLE IF EXISTS `pm_project`;
CREATE TABLE `pm_project` (
  `project_id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_code` varchar(50) NOT NULL COMMENT '项目编号(格式:行业-区域-简称-年份)',
  `project_name` varchar(200) NOT NULL COMMENT '项目名称',
  `project_full_name` varchar(500) DEFAULT NULL COMMENT '项目全称',
  `industry` varchar(50) DEFAULT NULL COMMENT '行业',
  `region` varchar(50) DEFAULT NULL COMMENT '区域',
  `short_name` varchar(50) DEFAULT NULL COMMENT '简称',
  `year` int DEFAULT NULL COMMENT '年份',
  `project_category` varchar(50) DEFAULT NULL COMMENT '项目分类',
  `project_dept` varchar(100) DEFAULT NULL COMMENT '项目部门',
  `project_status` varchar(50) DEFAULT NULL COMMENT '项目状态',
  `acceptance_status` varchar(50) DEFAULT NULL COMMENT '验收状态',
  `estimated_workload` decimal(10,2) DEFAULT NULL COMMENT '预估工作量(人天)',
  `actual_workload` decimal(10,2) DEFAULT '0.00' COMMENT '实际工作量(人天)',
  `project_address` varchar(500) DEFAULT NULL COMMENT '项目地址',
  `project_plan` text COMMENT '项目计划',
  `project_description` text COMMENT '项目描述',
  `project_manager_id` bigint DEFAULT NULL COMMENT '项目经理ID',
  `market_manager_id` bigint DEFAULT NULL COMMENT '市场经理ID',
  `participants` varchar(500) DEFAULT NULL COMMENT '参与人员ID列表(逗号分隔)',
  `sales_manager_id` bigint DEFAULT NULL COMMENT '销售负责人ID',
  `sales_contact` varchar(50) DEFAULT NULL COMMENT '销售联系方式',
  `team_leader_id` bigint DEFAULT NULL COMMENT '团队负责人ID',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `customer_contact_id` bigint DEFAULT NULL COMMENT '客户联系人ID',
  `merchant_contact` varchar(100) DEFAULT NULL COMMENT '商户联系人',
  `merchant_phone` varchar(50) DEFAULT NULL COMMENT '商户联系方式',
  `start_date` date DEFAULT NULL COMMENT '启动日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `production_date` date DEFAULT NULL COMMENT '投产日期',
  `acceptance_date` date DEFAULT NULL COMMENT '验收日期',
  `project_budget` decimal(15,2) DEFAULT NULL COMMENT '项目预算(元)',
  `project_cost` decimal(15,2) DEFAULT NULL COMMENT '项目费用(元)',
  `cost_budget` decimal(15,2) DEFAULT NULL COMMENT '费用预算(元)',
  `budget_cost` decimal(15,2) DEFAULT NULL COMMENT '成本预算(元)',
  `labor_cost` decimal(15,2) DEFAULT NULL COMMENT '人力费用(元)',
  `purchase_cost` decimal(15,2) DEFAULT '0.00' COMMENT '采购成本',
  `approval_status` varchar(20) DEFAULT NULL COMMENT '审批状态(0待审核/1已通过/2已拒绝)',
  `approval_reason` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `industry_code` varchar(50) DEFAULT NULL COMMENT '行业代码',
  `region_code` varchar(50) DEFAULT NULL COMMENT '区域代码(字典:sys_yjqy)',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approver_id` varchar(64) DEFAULT NULL COMMENT '审批人',
  `remark` text COMMENT '备注',
  `tax_rate` decimal(5,2) DEFAULT NULL COMMENT '税率(%)',
  `confirm_user_id` bigint DEFAULT NULL COMMENT '确认人ID',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `reserved_field1` varchar(64) DEFAULT NULL COMMENT '备用域1',
  `reserved_field2` varchar(64) DEFAULT NULL COMMENT '备用域2',
  `reserved_field3` varchar(64) DEFAULT NULL COMMENT '备用域3',
  `reserved_field4` varchar(64) DEFAULT NULL COMMENT '备用域4',
  `reserved_field5` varchar(64) DEFAULT NULL COMMENT '备用域5',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `confirm_status` char(1) DEFAULT '0' COMMENT '确认状态（0=未确认 1=已确认）',
  `confirm_quarter` varchar(20) DEFAULT NULL COMMENT '确认季度',
  `confirm_amount` decimal(15,2) DEFAULT NULL COMMENT '确认金额（含税）',
  `after_tax_amount` decimal(15,2) DEFAULT NULL COMMENT '税后金额',
  `confirm_user_name` varchar(64) DEFAULT NULL COMMENT '确认人姓名',
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_approval_status` (`approval_status`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目管理表';

-- ----------------------------
-- 7、项目审核表
-- ----------------------------
DROP TABLE IF EXISTS `pm_project_approval`;
CREATE TABLE `pm_project_approval` (
  `approval_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审核主键ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `approval_status` varchar(50) NOT NULL DEFAULT '待审核' COMMENT '审核状态(0-待审核、1-通过、2-不通过)',
  `approval_reason` text COMMENT '审核原因/意见',
  `approver_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `approval_time` datetime DEFAULT NULL COMMENT '审核时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`approval_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_approval_status` (`approval_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目审核表';

-- ----------------------------
-- 8、项目合同关系表
-- ----------------------------
DROP TABLE IF EXISTS `pm_project_contract_rel`;
CREATE TABLE `pm_project_contract_rel` (
  `rel_id` bigint NOT NULL AUTO_INCREMENT COMMENT '关系主键ID',
  `project_id` varchar(50) NOT NULL COMMENT '项目ID',
  `project_name` varchar(200) DEFAULT NULL COMMENT '项目名称',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `contract_name` varchar(200) DEFAULT NULL COMMENT '合同名称',
  `rel_type` varchar(50) DEFAULT '主合同' COMMENT '关联类型(主合同、补充协议、变更合同)',
  `is_main` char(1) DEFAULT '1' COMMENT '是否主合同(1是 0否)',
  `rel_status` varchar(50) DEFAULT '有效' COMMENT '关系状态(有效、失效)',
  `bind_date` date DEFAULT NULL COMMENT '关联日期',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`rel_id`),
  UNIQUE KEY `uk_project_contract` (`project_id`,`contract_id`,`del_flag`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_is_main` (`is_main`),
  KEY `idx_rel_status` (`rel_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目合同关系表';

-- ----------------------------
-- 9、项目经理表
-- ----------------------------
CREATE TABLE `pm_project_manager_change` (
  `change_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '变更主键ID',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  `old_manager_id` bigint(20) DEFAULT NULL COMMENT '原项目经理ID',
  `new_manager_id` bigint(20) NOT NULL COMMENT '新项目经理ID',

  -- 通用字段
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
 `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',

  PRIMARY KEY (`change_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_old_manager_id` (`old_manager_id`),
  KEY `idx_new_manager_id` (`new_manager_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目经理变更表';

-- ----------------------------
-- 10、项目人员管理表
-- ----------------------------
DROP TABLE IF EXISTS `pm_project_member`;
CREATE TABLE `pm_project_member` (
  `member_id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员主键ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `join_date` date DEFAULT NULL COMMENT '加入日期',
  `leave_date` date DEFAULT NULL COMMENT '离开日期',
  `is_active` char(1) DEFAULT '1' COMMENT '是否在项目中(1是 0否)',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`member_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目人员管理表';

-- ----------------------------
-- 11、团队收入确认表
-- ----------------------------
DROP TABLE IF EXISTS `pm_team_revenue_confirmation`;
CREATE TABLE `pm_team_revenue_confirmation` (
  `team_confirm_id` bigint NOT NULL AUTO_INCREMENT COMMENT '团队确认ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `confirm_amount` decimal(15,2) NOT NULL COMMENT '确认金额',
  `confirm_time` datetime NOT NULL COMMENT '确认时间',
  `confirm_user_id` bigint NOT NULL COMMENT '确认人ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`team_confirm_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_confirm_time` (`confirm_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队收入确认表';



-- =============================================
-- 中国区域表设计（省、市两级）- V2版本
-- 日期: 2026-02-03
-- 说明:
--   一级区域：使用字典表 sys_dict_data（字典类型：sys_yjqy）- 需手动维护
--   二级区域：省级（省、直辖市、自治区）- 不包含港澳台
--   三级区域：市级
-- 修改内容：
--   1. 移除一级区域字典数据的自动插入（需在字典管理中手动维护）
--   2. 北京市使用独立区域值：BJ
--   3. 其他省份使用区域缩写：HBQY、DBQY、HDQY、HZQY、HNQY、XNQY、XBQY
--   4. 移除港澳台地区相关数据
-- =============================================
-- =============================================
-- 12. 二级区域表
-- =============================================
DROP TABLE IF EXISTS `pm_secondary_region`;
CREATE TABLE `pm_secondary_region` (
  `province_id` bigint NOT NULL AUTO_INCREMENT COMMENT '省份ID',
  `province_code` varchar(10) NOT NULL COMMENT '省份代码（行政区划代码前2位）',
  `province_name` varchar(50) NOT NULL COMMENT '省份名称',
  `province_type` varchar(20) DEFAULT '0' COMMENT '省份类型（0=省/1=直辖市/2=自治区/3=特别行政区/4=计划单列市）',
  `region_dict_value` varchar(50) NOT NULL COMMENT '一级区域字典值（关联sys_dict_data的dict_value）',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`province_id`),
  UNIQUE KEY `uk_province_code` (`province_code`),
  KEY `idx_region_dict_value` (`region_dict_value`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='省级区域表';





