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
COMMIT;
