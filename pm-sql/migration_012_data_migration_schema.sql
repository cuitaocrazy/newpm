-- ============================================================================
-- Feature 012 历史数据迁移 - schema 变更（A 反范式化文本快照）
-- 1) ④pm_prolist_defect 加 8 个快照列；⑤pm_nobatch_prolist_defect 加 pro_batch_no
-- 2) sys_problem_state 字典 6→16（老 T_C_CURRENT_STATE 的转派类状态，问题单实际用到 7/9 等）
-- 幂等：列用 information_schema 守卫；字典先删后插。
-- 执行：cat pm-sql/migration_012_data_migration_schema.sql | docker exec -i newpm-mysql-1 mysql -u root -ppassword --default-character-set=utf8mb4 <库>
-- ============================================================================

-- ① ④ pm_prolist_defect 加 8 个可空快照列（无 task_id/batch_id FK 时读这些）
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pm_prolist_defect' AND column_name='task_no');
SET @s := IF(@c=0,
 'ALTER TABLE pm_prolist_defect
    ADD COLUMN task_no varchar(64) DEFAULT NULL COMMENT ''软件中心任务号(历史快照,无FK时用)'' AFTER task_id,
    ADD COLUMN task_name varchar(255) DEFAULT NULL COMMENT ''任务名称(历史快照)'' AFTER task_no,
    ADD COLUMN product varchar(64) DEFAULT NULL COMMENT ''二级产品(历史快照)'' AFTER task_name,
    ADD COLUMN internal_closure_date date DEFAULT NULL COMMENT ''提交内部测试B包日期(历史快照)'' AFTER product,
    ADD COLUMN functional_test_date date DEFAULT NULL COMMENT ''提交功能测试版本日期(历史快照)'' AFTER internal_closure_date,
    ADD COLUMN production_version_date date DEFAULT NULL COMMENT ''提交生产版本日期(历史快照)'' AFTER functional_test_date,
    ADD COLUMN schedule_status varchar(32) DEFAULT NULL COMMENT ''排期状态(历史快照,sys_pqzt)'' AFTER production_version_date,
    ADD COLUMN pro_batch_no varchar(64) DEFAULT NULL COMMENT ''投产批次号(历史快照,无FK时用)'' AFTER batch_id',
 'SELECT ''pm_prolist_defect snapshot cols exist''');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ①b ③ pm_old_version_out.task_no 加宽 64→255（老 TASK_NO 最长 104，可含多任务号逗号串）
ALTER TABLE pm_old_version_out MODIFY task_no varchar(255) DEFAULT NULL COMMENT '任务编号';

-- ② ⑤ pm_nobatch_prolist_defect 加 pro_batch_no（任务字段已是文本）
SET @c2 := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pm_nobatch_prolist_defect' AND column_name='pro_batch_no');
SET @s2 := IF(@c2=0,
 'ALTER TABLE pm_nobatch_prolist_defect ADD COLUMN pro_batch_no varchar(64) DEFAULT NULL COMMENT ''投产批次号(历史快照,无FK时用)'' AFTER batch_id',
 'SELECT ''pm_nobatch_prolist_defect pro_batch_no exists''');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;

-- ③ sys_problem_state 扩到 16（值=老id 标签=老CURRENT_STATUS；id17空跳过）
DELETE FROM sys_dict_data WHERE dict_type='sys_problem_state';
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark) VALUES
(1,'已定位','1','sys_problem_state','','default','N','0','admin',NOW(),'迁移补全'),
(2,'待验证','2','sys_problem_state','','warning','N','0','admin',NOW(),'迁移补全'),
(3,'未受理','3','sys_problem_state','','info','N','0','admin',NOW(),'迁移补全'),
(4,'已受理','4','sys_problem_state','','primary','N','0','admin',NOW(),'迁移补全'),
(5,'问题已解决','5','sys_problem_state','','success','N','0','admin',NOW(),'迁移补全'),
(6,'问题再现','6','sys_problem_state','','danger','N','0','admin',NOW(),'迁移补全'),
(7,'已转至外部系统处理','7','sys_problem_state','','info','N','0','admin',NOW(),'迁移补全'),
(8,'已转至HHAP-CFS排查','8','sys_problem_state','','info','N','0','admin',NOW(),'迁移补全'),
(9,'已转至HHAP-TMS排查','9','sys_problem_state','','info','N','0','admin',NOW(),'迁移补全'),
(10,'已转至HHAP-ICA排查','10','sys_problem_state','','info','N','0','admin',NOW(),'迁移补全'),
(11,'已转至HHAP-CSKJ排查','11','sys_problem_state','','info','N','0','admin',NOW(),'迁移补全'),
(12,'已转至COR-商户服务组','12','sys_problem_state','','info','N','0','admin',NOW(),'迁移补全'),
(13,'已转至COR-特色业务组','13','sys_problem_state','','info','N','0','admin',NOW(),'迁移补全'),
(14,'已转至COR-快捷业务组','14','sys_problem_state','','info','N','0','admin',NOW(),'迁移补全'),
(15,'转出至HHAP','15','sys_problem_state','','warning','N','0','admin',NOW(),'迁移补全'),
(16,'转出至WCB','16','sys_problem_state','','warning','N','0','admin',NOW(),'迁移补全');

COMMIT;

-- ===== 补迁字段(项目组/计划投产日期/创建人姓名/修改人姓名 快照) =====
-- ④ pm_prolist_defect 加 4 列
SET @c3 := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pm_prolist_defect' AND column_name='subtask_team');
SET @s3 := IF(@c3=0,
 'ALTER TABLE pm_prolist_defect
    ADD COLUMN subtask_team varchar(64) DEFAULT NULL COMMENT ''项目组(历史快照,无dept_id时用)'' AFTER dept_id,
    ADD COLUMN plan_production_date date DEFAULT NULL COMMENT ''计划投产日期(历史快照,无batch_id时用)'' AFTER pro_batch_no,
    ADD COLUMN creator_name varchar(64) DEFAULT NULL COMMENT ''创建人姓名(历史快照,老录入人)'' AFTER create_by,
    ADD COLUMN modifier_name varchar(64) DEFAULT NULL COMMENT ''修改人姓名(历史快照,老修改人)'' AFTER update_by',
 'SELECT ''pm_prolist_defect 补迁列 exist''');
PREPARE st3 FROM @s3; EXECUTE st3; DEALLOCATE PREPARE st3;
-- ⑤ pm_nobatch_prolist_defect 加 4 列
SET @c4 := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pm_nobatch_prolist_defect' AND column_name='subtask_team');
SET @s4 := IF(@c4=0,
 'ALTER TABLE pm_nobatch_prolist_defect
    ADD COLUMN subtask_team varchar(64) DEFAULT NULL COMMENT ''项目组(历史快照,无dept_id时用)'' AFTER dept_id,
    ADD COLUMN plan_production_date date DEFAULT NULL COMMENT ''计划投产日期(历史快照,无batch_id时用)'' AFTER pro_batch_no,
    ADD COLUMN creator_name varchar(64) DEFAULT NULL COMMENT ''创建人姓名(历史快照,老录入人)'' AFTER create_by,
    ADD COLUMN modifier_name varchar(64) DEFAULT NULL COMMENT ''修改人姓名(历史快照,老修改人)'' AFTER update_by',
 'SELECT ''pm_nobatch_prolist_defect 补迁列 exist''');
PREPARE st4 FROM @s4; EXECUTE st4; DEALLOCATE PREPARE st4;

-- ===== ① ② pm_version_out 加 comm_name_display(提交人显示名快照) =====
-- 老 comm_name 存的是 user_name(可能匹配不到 sys_user),迁移时解码为显示名兜底
SET @c5 := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pm_version_out' AND column_name='comm_name_display');
SET @s5 := IF(@c5=0,
 'ALTER TABLE pm_version_out
    ADD COLUMN comm_name_display varchar(64) DEFAULT NULL COMMENT ''提交人员显示名(迁移历史文本快照,优先nick_name再兜底此列)'' AFTER comm_name',
 'SELECT ''pm_version_out.comm_name_display exist''');
PREPARE st5 FROM @s5; EXECUTE st5; DEALLOCATE PREPARE st5;

-- ===== ①② pm_version_out 加 creator_name/modifier_name(创建人/修改人姓名快照) =====
-- 老 T_B_VERSION_OUT.CREATOR/MODIFIER 存旧USER_ID,迁移时解码为姓名快照(参照旧系统显示纯姓名);
-- create_by 仍恒为 'yadapm-migrate' 作幂等标记,显示走快照列(对齐 ④⑤ creator_name/modifier_name)
SET @c7 := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pm_version_out' AND column_name='creator_name');
SET @s7 := IF(@c7=0,
 'ALTER TABLE pm_version_out
    ADD COLUMN creator_name varchar(64) DEFAULT NULL COMMENT ''创建人姓名(历史快照,老录入人)'' AFTER create_by,
    ADD COLUMN modifier_name varchar(64) DEFAULT NULL COMMENT ''修改人姓名(历史快照,老修改人)'' AFTER update_by',
 'SELECT ''pm_version_out 创建/修改人快照列 exist''');
PREPARE st7 FROM @s7; EXECUTE st7; DEALLOCATE PREPARE st7;
COMMIT;

-- ===== ① 批次版本 manual_task_no 加宽 64→512 =====
-- 批次版本关联任务号(老 TASK_NO 解码成软件中心任务号串,多任务时较长)复用此列作快照,64 不够
ALTER TABLE pm_version_out MODIFY manual_task_no varchar(512) DEFAULT NULL COMMENT '手填软件中心任务号(非批次)/批次版本关联任务号快照';
COMMIT;

-- ===== ①② pm_version_out 加 legacy_id(老ID,列表排序对齐老 ID DESC) =====
SET @c6 := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pm_version_out' AND column_name='legacy_id');
SET @s6 := IF(@c6=0,
 'ALTER TABLE pm_version_out ADD COLUMN legacy_id bigint DEFAULT NULL COMMENT ''老yadapm T_B_VERSION_OUT.ID(迁移行排序对齐老ID DESC)'', ADD INDEX idx_legacy_id (legacy_id)',
 'SELECT ''pm_version_out.legacy_id exist''');
PREPARE st6 FROM @s6; EXECUTE st6; DEALLOCATE PREPARE st6;

-- ===== sys_version_status 字典(版本状态,建表时漏建→版本管理"版本状态"列空白) =====
DELETE FROM sys_dict_data WHERE dict_type='sys_version_status';
DELETE FROM sys_dict_type WHERE dict_type='sys_version_status';
INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time,remark) VALUES ('版本状态','sys_version_status','0','admin',NOW(),'迁移补全(老T_C_VERSION_STATUS)');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) VALUES
(1,'B1-参与组生产包','1','sys_version_status','default','N','0','admin',NOW()),
(2,'B2-不参与组生产包','2','sys_version_status','info','N','0','admin',NOW()),
(3,'S1-参与投产','3','sys_version_status','success','N','0','admin',NOW()),
(4,'S2-不参与投产','4','sys_version_status','warning','N','0','admin',NOW()),
(5,'T-其他','5','sys_version_status','default','N','0','admin',NOW());
COMMIT;

-- ===== 历史任务快照表 pm_task_snapshot(迁移自 T_B_TASK 完整任务详情;老任务在新pm_task无对应时点任务号看这里) =====
-- ============================================================
-- 历史任务快照表 pm_task_snapshot
-- 承载老 yadapm T_B_TASK(46列) 完整任务详情，纯归档只读快照(不入主业务流)
-- 字段命名对齐 pm_task；加 legacy_task_id(老PK) 与 task_no(=center_task_no)
-- collation 与新表 pm_task 一致: utf8mb4_unicode_ci
-- ============================================================
CREATE TABLE IF NOT EXISTS `pm_task_snapshot` (
  `snapshot_id`             bigint        NOT NULL AUTO_INCREMENT                       COMMENT '快照主键(自增)',
  `legacy_task_id`          bigint        NOT NULL                                      COMMENT '老任务内部ID(T_B_TASK.TASK_ID，真PK，去重用)',
  `task_no`                 varchar(64)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '软件中心任务号(T_B_TASK.CENTER_TASK_NO，非唯一,可空/脏值)',
  `new_task_id`             bigint        DEFAULT NULL                                  COMMENT '新系统任务ID(pm_task.task_id；按task_no命中pm_task.task_code则挂上，否则NULL=纯快照)',
  `task_name`               varchar(200)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '任务名称(T_B_TASK.TASK_NAME)',
  `task_kind`               varchar(32)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '任务类型(T_B_TASK.TASK_KIND，老编码1-9，无对应字典保留原值)',
  `product`                 varchar(64)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '产品(T_B_TASK.PRODUCT，多为产品名如HHAP-TMS；命中T_C_PRODUCT则解码)',
  `subtask_team`            varchar(64)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '子任务团队/项目组(T_B_TASK.SUBTASK_TEAM)',
  `task_holders_name`       varchar(128)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '任务负责人姓名(T_B_TASK.TASK_HOLDERS 用户id->姓名)',
  `subtask_holders_name`    varchar(128)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '子任务负责人姓名(T_B_TASK.SUBTASK_HOLDERS 用户id->姓名)',
  `saler_name`              varchar(128)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '销售姓名(T_B_TASK.SALER 用户id->姓名)',
  `inside_subtask_no`       varchar(64)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '内部子任务号(T_B_TASK.INSIDE_SUBTASK_NO)',
  `lx_no`                   varchar(128)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '立项号(T_B_TASK.LX_NO)',
  `batch_no`                varchar(64)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '投产批次号(T_B_TASK.BATCH_NO，冗余文本展示)',
  `batch_id`                bigint        DEFAULT NULL                                  COMMENT '新系统投产批次ID(老BATCH_ID->BATCH_NO->pm_production_batch.batch_id命中则挂，否则NULL)',
  `legacy_batch_id`         bigint        DEFAULT NULL                                  COMMENT '老投产批次内部id(T_B_TASK.BATCH_ID原值，留档追溯)',
  `task_manday`             decimal(10,2) DEFAULT NULL                                  COMMENT '任务人天/预估工作量(T_B_TASK.TASK_MANDAY)',
  `manday_amount`           decimal(15,2) DEFAULT NULL                                  COMMENT '人天金额(T_B_TASK.MANDAY_AMOUNT)',
  `ys_amount`               decimal(15,2) DEFAULT NULL                                  COMMENT '预算/验收金额(T_B_TASK.YS_AMOUNT)',
  `schedule_status`         varchar(64)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '排期状态(T_B_TASK.SCHEDULING_STATUS 中文->sys_pqzt字典值,无对应保留原文)',
  `product_report_status`   varchar(32)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '投产报告状态(T_B_TASK.RRODUCT_REPORT_STATUS)',
  `product_report_tracking` varchar(512)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '投产报告跟踪(T_B_TASK.RRODUCT_REPORT_TRACKING)',
  `product_report_url`      varchar(512)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '投产报告URL(T_B_TASK.RRODUCT_REPORT_URL，物理文件可能缺失)',
  `product_report_date`     date          DEFAULT NULL                                  COMMENT '投产报告日期(T_B_TASK.RRODUCT_REPORT_DATE yyyymmdd)',
  `function_description`    text          COLLATE utf8mb4_unicode_ci                    COMMENT '功能点说明(T_B_TASK.FUNCTION_DESC，对应详情页"功能点说明")',
  `task_plan`               text          COLLATE utf8mb4_unicode_ci                    COMMENT '任务计划/实施计划(T_B_TASK.PLAN，对应详情页"实施计划")',
  `internal_closure_date`   date          DEFAULT NULL                                  COMMENT '内部B包日期(T_B_TASK.TEST_SUB_B_DATE yyyymmdd)',
  `functional_test_date`    date          DEFAULT NULL                                  COMMENT '功能测试版本日期(T_B_TASK.TEST_VERSION_DATE yyyymmdd)',
  `production_version_date` date          DEFAULT NULL                                  COMMENT '生产版本日期(T_B_TASK.PRO_VERSION_DATE yyyymmdd)',
  `actual_production_date`  date          DEFAULT NULL                                  COMMENT '实际投产日期(T_B_TASK.ACTUAL_TC_DATE yyyymmdd)',
  `demand_id`               bigint        DEFAULT NULL                                  COMMENT '需求内部id(T_B_TASK.DEMAND_ID原值留档)',
  `bank_demand_no`          varchar(100)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '需求号(T_B_TASK.DEMAND_NO，对齐pm_task.bank_demand_no/总行需求号)',
  `demand_name`             varchar(255)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '需求名称(T_B_TASK.DEMAND_NAME)',
  `demand_contacts`         varchar(128)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '需求联系人(T_B_TASK.DEMAND_CONTACTS)',
  `contacts_tel`            varchar(32)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '联系人电话(T_B_TASK.CONTACTS_TEL)',
  `contacts_mobile`         varchar(32)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '联系人手机(T_B_TASK.CONTACTS_MOBILE)',
  `parent_project_name`     varchar(200)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '所属项目名称(T_B_TASK.PRJ_NAME，冗余文本)',
  `legacy_project_id`       bigint        DEFAULT NULL                                  COMMENT '老所属项目内部id(T_B_TASK.PRJ_ID原值留档)',
  `production_year`         varchar(10)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '投产年度(T_B_TASK.YEAR_ID 老年份id->年份文本/sys_ndgl)',
  `check_status`            varchar(32)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '审核状态(T_B_TASK.CHECK_STATUS，老编码1/2/3，无对应字典保留原值)',
  `reviewer_name`           varchar(128)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '审核人姓名(T_B_TASK.REVIEWER 用户id->姓名)',
  `check_date`              datetime      DEFAULT NULL                                  COMMENT '审核时间(T_B_TASK.CHECK_DATE yyyymmddhhmmss)',
  `print_date`              datetime      DEFAULT NULL                                  COMMENT '打印时间(T_B_TASK.PRINT_DATE yyyymmddhhmmss)',
  `remarks`                 text          COLLATE utf8mb4_unicode_ci                    COMMENT '备注(T_B_TASK.REMARKS，对齐详情页"备注")',
  `creator_name`            varchar(128)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '创建人姓名快照(T_B_TASK.CREATOR 用户id->登录名(姓名))',
  `modifier_name`           varchar(128)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '修改人姓名快照(T_B_TASK.MODIFIER 用户id->登录名(姓名))',
  `legacy_creation_date`    datetime      DEFAULT NULL                                  COMMENT '老创建时间(T_B_TASK.CREATION_DATE yyyymmddhhmmss)',
  `legacy_modification_date` datetime     DEFAULT NULL                                  COMMENT '老最后修改时间(T_B_TASK.LAST_MODIFICATION_DATE yyyymmddhhmmss)',
  `del_flag`                char(1)       COLLATE utf8mb4_unicode_ci DEFAULT '0'        COMMENT '删除标志(0正常 1删除)',
  `create_by`               varchar(64)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '迁移标记(固定 yadapm-migrate)',
  `create_time`             datetime      DEFAULT CURRENT_TIMESTAMP                     COMMENT '快照入库时间',
  `update_by`               varchar(64)   COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '更新者',
  `update_time`             datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`                  varchar(500)  COLLATE utf8mb4_unicode_ci DEFAULT NULL       COMMENT '备注(框架字段)',
  PRIMARY KEY (`snapshot_id`),
  UNIQUE KEY `uk_legacy_task_id` (`legacy_task_id`),
  KEY `idx_task_no` (`task_no`),
  KEY `idx_new_task_id` (`new_task_id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_production_year` (`production_year`),
  KEY `idx_schedule_status` (`schedule_status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='历史任务快照表(迁移自yadapm T_B_TASK)';

-- ===== pm_version_out_task 加 task_no/legacy_task_id(承载迁移任务关联+快照链接) =====
SET @c8 := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pm_version_out_task' AND column_name='task_no');
SET @s8 := IF(@c8=0,
 'ALTER TABLE pm_version_out_task
    ADD COLUMN task_no varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''软件中心任务号快照(迁移记录用,新记录走JOIN)'' AFTER task_id,
    ADD COLUMN legacy_task_id bigint DEFAULT NULL COMMENT ''老任务内部id(T_B_TASK.TASK_ID,链接历史任务快照详情用)'' AFTER task_no,
    ADD INDEX idx_vot_task_no (task_no),
    ADD INDEX idx_vot_legacy_task_id (legacy_task_id)',
 'SELECT ''pm_version_out_task task_no/legacy_task_id exist''');
PREPARE st8 FROM @s8; EXECUTE st8; DEALLOCATE PREPARE st8;
COMMIT;
