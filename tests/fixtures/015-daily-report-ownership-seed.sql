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
--
-- ⚠️ 明细的清理【不能】JOIN 主表：
--   ① 用例跑完可能留下「主记录已被删、明细还在」的孤儿明细（e2e 的删除用例本就在验证这种保留逻辑）；
--   ② 应用侧也清不掉它们——deleteByReportIdInScope 为了做归属校验改成了
--      INNER JOIN pm_daily_report 的多表 DELETE，主记录不存在就匹配不到任何行；
--   ③ 991xxx 是显式指定的固定 ID（非自增），下一次造数会重建同号主记录，
--      孤儿明细按 report_id 自动「挂回」新日报 → 当日工时凭空多一份，断言随机变红且极难定位。
-- 因此明细清理【不能】加 remark 条件（孤儿明细与 API 新建的明细 remark 都是 NULL）。
--
-- ⚠️ 但范围必须严格限定为本脚本显式使用的那 4 个 ID，【不可】写成 991000-991999 号段：
--    显式插入 991000-991003 会把 pm_daily_report 的 AUTO_INCREMENT 推到 991004，此后在该库上
--    新建的每一条日报（e2e 保存当天/15 号、或有人手工使用同一开发库）都会落进 991xxx 且
--    remark 为 NULL。若按号段无差别删明细，这些【真实日报】的明细会被删光、而主记录因 remark
--    不匹配而留下 → 日历卡显示 8h 点开却是空白，且 pm_project.actual_workload 继续统计
--    已不存在的明细，直到有人重新保存那天。主记录清理同样用精确 ID，与明细保持一致。
DELETE FROM pm_daily_report_detail WHERE report_id IN (991000, 991001, 991002, 991003);
DELETE FROM pm_daily_report        WHERE report_id IN (991000, 991001, 991002, 991003);
DELETE FROM pm_project_member WHERE project_id BETWEEN 990000 AND 990999 AND remark = '015-e2e-fixture';
DELETE FROM pm_project        WHERE project_id BETWEEN 990000 AND 990999 AND remark = '015-e2e-fixture';

-- ---------------------------------------------------------------- 项目 ----
-- 990100 在建 + admin 是成员 → 出现在「我的项目」→ 作用范围【内】
INSERT INTO pm_project (project_id, project_code, project_name, project_stage,
                        approval_status, project_status, del_flag, project_dept,
                        project_manager_id, actual_workload, create_by, create_time, remark)
-- actual_workload 必须与明细汇总自洽：990100 上挂着 991000 的 4h + 991001 的 4h
-- + 991003（他人日报）的 4h = 12.00。写错会让 quickstart.md 的 SC-008 对账报差异，
-- 而那条对账的唯一目的就是发现这种不一致，执行者会把造数缺陷误判成产品缺陷。
VALUES (990100, 'E2E-015-ACTIVE', '015在建项目A', '3', '1', '0', '0', 103, 1, 12.00, 'admin', NOW(), '015-e2e-fixture');

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

-- 990500 在建，admin 是【市场经理】但 pm_project_member 里【没有任何行】→ 归属校验须放行
-- 复刻生产上的真实数据形态：历史项目未经 syncProjectMembers 重新保存过，成员表漏同步
-- （实测市场经理缺行 30 个 / 销售经理缺行 27 个）。myProjects（selectProjectsByUserId）的
-- OR 列表含 market_manager_id，所以填报页会把它列出来——写侧若只认成员行，这个人当日
-- 整张日报永久保存不了。参见 ProjectMapper.selectProjectRoleProjectIds 的注释。
-- ⚠️ 刻意【不】给它插 pm_project_member 行，这正是本场景的全部要害。
INSERT INTO pm_project (project_id, project_code, project_name, project_stage,
                        approval_status, project_status, del_flag, project_dept,
                        project_manager_id, market_manager_id, actual_workload,
                        create_by, create_time, remark)
VALUES (990500, 'E2E-015-ROLEONLY', '015仅角色项目E', '3', '1', '0', '0', 103, 2, 1, 0.00, 'admin', NOW(), '015-e2e-fixture');

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

-- 991003 / 2026-07-22：【Issue #13】别人的日报（user_id=2 'ry'，dept 105）
-- 这条是归属校验的唯一证据：其余 991xxx 全是 admin 自己的，删与不删都是绿的。
-- 两条明细刻意都落在 admin 的「作用范围内」——
--   · 990100 是 admin 可填的在建项目
--   · project_id IS NULL 的假期行对任何调用者都无条件在范围内
-- 也就是说：仅靠 015 的作用范围裁剪，这两条都会被 admin 删掉；
-- 只有用户级的 user_id 限定才拦得住（Issue #13）。
INSERT INTO pm_daily_report (report_id, report_date, user_id, dept_id, total_work_hours, del_flag, create_by, create_time, remark)
VALUES (991003, '2026-07-22', 2, 105, 4.00, '0', 'ry', NOW(), '015-e2e-fixture');
INSERT INTO pm_daily_report_detail (report_id, project_id, entry_type, leave_hours, work_hours, work_content, del_flag, create_by, create_time, remark)
VALUES (991003, 990100, 'work',   NULL, 4.00, '他人的工时（admin 不得删除）', '0', 'ry', NOW(), '015-e2e-fixture'),
       (991003, NULL,   'annual', 4.00, 4.00, '他人的年假（admin 不得删除）', '0', 'ry', NOW(), '015-e2e-fixture');

SELECT '015 e2e 造数完成（安全闸门已通过）' AS status;
