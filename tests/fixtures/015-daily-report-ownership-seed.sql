-- ============================================================================
-- e2e 造数：015 日报保存的工时保护与项目归属校验
-- 配套 tests/e2e-daily-report-ownership.spec.js
--
-- 用法（本地隔离实例示例）：
--   cat tests/fixtures/015-daily-report-ownership-seed.sql | \
--     mysql --protocol=TCP -h 127.0.0.1 -P 3307 -u root -ppassword \
--           --default-character-set=utf8mb4 ry-vue
--
-- ⚠️⚠️ 本脚本含 DELETE，只能在「从 pm-sql/init 全新导入的空测试库」上执行。⚠️⚠️
--
-- 三重保护（缺一不可，改动时不要削弱）：
--   ① 安全闸门：项目数 > 20 判定为真实库，SIGNAL 报错中止（见下方 015_guard）
--   ② 高位 ID：990xxx / 991xxx 号段，远高于真实库的实际水位
--      （2026-08-03 实测：pm_project 最大 371、pm_daily_report 最大 13251）
--   ③ DELETE 限定 remark 标记：只清理本脚本自己造的行，不碰任何他人数据
--      （标记放 remark 而非 create_by —— create_by 存的是 sys_user.user_name，
--        往里塞标记会污染审计字段，将来查「谁建的」会看到一个不存在的用户）
--
-- 历史教训：初版用了 100/200/300/400 与 1000/1001/1002 这类低位 ID，
-- 而它们在生产库里全是真实数据——跑一次会删掉 2 个真实项目、22 条成员记录、
-- 3 条他人日报。低位 ID + 无差别 DELETE 是数据事故的标准配方，不要重蹈。
--
-- 幂等：可重复执行。
-- 前提：admin 的 user_id = 1（RuoYi 默认初始数据）。
-- ============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------- 闸门 ----
DELIMITER $$
DROP PROCEDURE IF EXISTS `015_guard`$$
CREATE PROCEDURE `015_guard`()
BEGIN
    DECLARE v_projects INT;
    SELECT COUNT(*) INTO v_projects FROM pm_project WHERE del_flag = '0';
    IF v_projects > 20 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            '[015 fixture] 检测到疑似真实库（有效项目数 > 20），拒绝执行。本脚本含 DELETE，仅供全新导入的空测试库使用。';
    END IF;
END$$
DELIMITER ;
CALL `015_guard`();
DROP PROCEDURE `015_guard`;

-- ---------------------------------------------------------------- 清理 ----
-- 只删本脚本造的行：ID 在 99xxxx 号段 且 remark 带本脚本的标记（双条件）
DELETE d FROM pm_daily_report_detail d
    JOIN pm_daily_report r ON r.report_id = d.report_id
    WHERE r.report_id BETWEEN 991000 AND 991999 AND r.remark = '015-e2e-fixture';
DELETE FROM pm_daily_report  WHERE report_id  BETWEEN 991000 AND 991999 AND remark = '015-e2e-fixture';
DELETE FROM pm_project_member WHERE project_id BETWEEN 990000 AND 990999 AND remark = '015-e2e-fixture';
DELETE FROM pm_project        WHERE project_id BETWEEN 990000 AND 990999 AND remark = '015-e2e-fixture';

-- ---------------------------------------------------------------- 项目 ----
-- 990100 在建 + admin 是成员 → 出现在「我的项目」→ 作用范围【内】
INSERT INTO pm_project (project_id, project_code, project_name, project_stage,
                        approval_status, project_status, del_flag, project_dept,
                        project_manager_id, actual_workload, create_by, create_time, remark)
VALUES (990100, 'E2E-015-ACTIVE', '015在建项目A', '3', '1', '0', '0', 103, 1, 8.00, 'admin', NOW(), '015-e2e-fixture');

-- 990200 已结项（stage=11）+ admin 仍是成员 → 不出现在「我的项目」→ 作用范围【外】
-- 结项不解除成员关系，因此 admin 对它仍持有「曾参与」凭据
-- actual_workload 必须与明细汇总自洽（三条日报各 2h = 6.00）：
-- 写错的话，保存触发重算后会纠正它，导致「人天不变」的断言失败
INSERT INTO pm_project (project_id, project_code, project_name, project_stage,
                        approval_status, project_status, del_flag, project_dept,
                        project_manager_id, actual_workload, create_by, create_time, remark)
VALUES (990200, 'E2E-015-CLOSED', '015已结项项目B', '11', '1', '0', '0', 103, 1, 6.00, 'admin', NOW(), '015-e2e-fixture');

-- 990300 在建，admin 从未以任何身份参与 → 归属校验须拒绝
INSERT INTO pm_project (project_id, project_code, project_name, project_stage,
                        approval_status, project_status, del_flag, project_dept,
                        project_manager_id, actual_workload, create_by, create_time, remark)
VALUES (990300, 'E2E-015-STRANGER', '015无关项目C', '3', '1', '0', '0', 103, 2, 0.00, 'admin', NOW(), '015-e2e-fixture');

-- 990400 在建，admin 曾是成员但已离场（is_active=0）且非任何 manager → 归属校验须放行
INSERT INTO pm_project (project_id, project_code, project_name, project_stage,
                        approval_status, project_status, del_flag, project_dept,
                        project_manager_id, actual_workload, create_by, create_time, remark)
VALUES (990400, 'E2E-015-FORMER', '015离场项目D', '3', '1', '0', '0', 103, 2, 0.00, 'admin', NOW(), '015-e2e-fixture');

-- ------------------------------------------------------------ 成员关系 ----
INSERT INTO pm_project_member (project_id, user_id, is_active, del_flag, join_date, create_by, create_time, remark)
VALUES (990100, 1, '1', '0', '2026-01-01', 'admin', NOW(), '015-e2e-fixture'),
       (990200, 1, '1', '0', '2026-01-01', 'admin', NOW(), '015-e2e-fixture');
INSERT INTO pm_project_member (project_id, user_id, is_active, del_flag, join_date, leave_date, create_by, create_time, remark)
VALUES (990400, 1, '0', '0', '2026-01-01', '2026-05-01', 'admin', NOW(), '015-e2e-fixture');

-- -------------------------------------------------------------- 日报 ----
-- 991000 / 2026-07-20：保存场景（可见 4h + 不可见 2h）
INSERT INTO pm_daily_report (report_id, report_date, user_id, dept_id, total_work_hours, del_flag, create_by, create_time, remark)
VALUES (991000, '2026-07-20', 1, 103, 6.00, '0', 'admin', NOW(), '015-e2e-fixture');
INSERT INTO pm_daily_report_detail (report_id, project_id, entry_type, work_hours, work_content, del_flag, create_by, create_time, remark)
VALUES (991000, 990100, 'work', 4.00, '在建项目的工时（填报人看得见）', '0', 'admin', NOW(), '015-e2e-fixture'),
       (991000, 990200, 'work', 2.00, '已结项项目的历史工时（填报人看不见）', '0', 'admin', NOW(), '015-e2e-fixture');

-- 991001 / 2026-07-21：删除场景（可见 4h + 不可见 2h）
INSERT INTO pm_daily_report (report_id, report_date, user_id, dept_id, total_work_hours, del_flag, create_by, create_time, remark)
VALUES (991001, '2026-07-21', 1, 103, 6.00, '0', 'admin', NOW(), '015-e2e-fixture');
INSERT INTO pm_daily_report_detail (report_id, project_id, entry_type, work_hours, work_content, del_flag, create_by, create_time, remark)
VALUES (991001, 990100, 'work', 4.00, '在建项目的工时（填报人看得见）', '0', 'admin', NOW(), '015-e2e-fixture'),
       (991001, 990200, 'work', 2.00, '已结项项目的历史工时（填报人看不见）', '0', 'admin', NOW(), '015-e2e-fixture');

-- 991002 / 2026-07-24：整条日报都是不可见工时（删除后应「零主记录可删」）
INSERT INTO pm_daily_report (report_id, report_date, user_id, dept_id, total_work_hours, del_flag, create_by, create_time, remark)
VALUES (991002, '2026-07-24', 1, 103, 2.00, '0', 'admin', NOW(), '015-e2e-fixture');
INSERT INTO pm_daily_report_detail (report_id, project_id, entry_type, work_hours, work_content, del_flag, create_by, create_time, remark)
VALUES (991002, 990200, 'work', 2.00, '整条日报只有不可见工时', '0', 'admin', NOW(), '015-e2e-fixture');

SELECT '015 e2e 造数完成（安全闸门已通过）' AS status;
