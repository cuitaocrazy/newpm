-- ============================================================================
-- e2e 造数：015 日报保存的工时保护与项目归属校验
-- 配套 tests/e2e-daily-report-ownership.spec.js
--
-- 用法（本地隔离实例示例）：
--   cat tests/fixtures/015-daily-report-ownership-seed.sql | \
--     mysql --protocol=TCP -h 127.0.0.1 -P 3307 -u root -ppassword \
--           --default-character-set=utf8mb4 ry-vue
--
-- 幂等：可重复执行，每次都会把四个项目与三条日报重置到初始状态。
-- 前提：admin 的 user_id = 1、dept_id = 103（RuoYi 默认初始数据）。
-- ============================================================================

SET NAMES utf8mb4;

DELETE FROM pm_daily_report_detail WHERE report_id IN (1000, 1001, 1002);
DELETE FROM pm_daily_report       WHERE report_id IN (1000, 1001, 1002);
DELETE FROM pm_daily_report_detail WHERE report_id IN
    (SELECT report_id FROM pm_daily_report WHERE user_id = 1 AND report_date IN ('2026-07-25','2026-07-26','2026-07-27'));
DELETE FROM pm_daily_report       WHERE user_id = 1 AND report_date IN ('2026-07-25','2026-07-26','2026-07-27');
DELETE FROM pm_project_member WHERE project_id IN (100, 200, 300, 400);
DELETE FROM pm_project        WHERE project_id IN (100, 200, 300, 400);

-- ---------------------------------------------------------------- 项目 ----
-- P100 在建 + admin 是成员 → 出现在「我的项目」→ 作用范围【内】
INSERT INTO pm_project (project_id, project_code, project_name, project_stage,
                        approval_status, project_status, del_flag, project_dept,
                        project_manager_id, actual_workload, create_by, create_time)
VALUES (100, 'E2E-ACTIVE-2026', '015在建项目A', '3', '1', '0', '0', 103, 1, 8.00, 'admin', NOW());

-- P200 已结项（stage=11）+ admin 仍是成员 → 不出现在「我的项目」→ 作用范围【外】
-- 结项不解除成员关系，因此 admin 对它仍持有「曾参与」凭据
INSERT INTO pm_project (project_id, project_code, project_name, project_stage,
                        approval_status, project_status, del_flag, project_dept,
                        project_manager_id, actual_workload, create_by, create_time)
-- actual_workload 必须与明细汇总自洽：P200 在日报 1000/1001/1002 各有 2h，合计 6.00。
-- 若此处写错，保存操作触发重算后会把它纠正过来，导致「人天不变」的断言失败。
VALUES (200, 'E2E-CLOSED-2026', '015已结项项目B', '11', '1', '0', '0', 103, 1, 6.00, 'admin', NOW());

-- P300 在建，admin 从未以任何身份参与 → 归属校验须拒绝
INSERT INTO pm_project (project_id, project_code, project_name, project_stage,
                        approval_status, project_status, del_flag, project_dept,
                        project_manager_id, actual_workload, create_by, create_time)
VALUES (300, 'E2E-STRANGER-2026', '015无关项目C', '3', '1', '0', '0', 103, 2, 0.00, 'admin', NOW());

-- P400 在建，admin 曾是成员但已离场（is_active=0）且非任何 manager → 归属校验须放行
INSERT INTO pm_project (project_id, project_code, project_name, project_stage,
                        approval_status, project_status, del_flag, project_dept,
                        project_manager_id, actual_workload, create_by, create_time)
VALUES (400, 'E2E-FORMER-2026', '015离场项目D', '3', '1', '0', '0', 103, 2, 0.00, 'admin', NOW());

-- ------------------------------------------------------------ 成员关系 ----
INSERT INTO pm_project_member (project_id, user_id, is_active, del_flag, join_date, create_by, create_time)
VALUES (100, 1, '1', '0', '2026-01-01', 'admin', NOW()),
       (200, 1, '1', '0', '2026-01-01', 'admin', NOW());
INSERT INTO pm_project_member (project_id, user_id, is_active, del_flag, join_date, leave_date, create_by, create_time)
VALUES (400, 1, '0', '0', '2026-01-01', '2026-05-01', 'admin', NOW());

-- -------------------------------------------------------------- 日报 ----
-- 1000 / 2026-07-20：保存场景（可见 4h + 不可见 2h）
INSERT INTO pm_daily_report (report_id, report_date, user_id, dept_id, total_work_hours, del_flag, create_by, create_time)
VALUES (1000, '2026-07-20', 1, 103, 6.00, '0', 'admin', NOW());
INSERT INTO pm_daily_report_detail (report_id, project_id, entry_type, work_hours, work_content, del_flag, create_by, create_time)
VALUES (1000, 100, 'work', 4.00, '在建项目的工时（填报人看得见）', '0', 'admin', NOW()),
       (1000, 200, 'work', 2.00, '已结项项目的历史工时（填报人看不见）', '0', 'admin', NOW());

-- 1001 / 2026-07-21：删除场景（可见 4h + 不可见 2h）
INSERT INTO pm_daily_report (report_id, report_date, user_id, dept_id, total_work_hours, del_flag, create_by, create_time)
VALUES (1001, '2026-07-21', 1, 103, 6.00, '0', 'admin', NOW());
INSERT INTO pm_daily_report_detail (report_id, project_id, entry_type, work_hours, work_content, del_flag, create_by, create_time)
VALUES (1001, 100, 'work', 4.00, '在建项目的工时（填报人看得见）', '0', 'admin', NOW()),
       (1001, 200, 'work', 2.00, '已结项项目的历史工时（填报人看不见）', '0', 'admin', NOW());

-- 1002 / 2026-07-24：整条日报都是不可见工时（删除后应「零主记录可删」）
INSERT INTO pm_daily_report (report_id, report_date, user_id, dept_id, total_work_hours, del_flag, create_by, create_time)
VALUES (1002, '2026-07-24', 1, 103, 2.00, '0', 'admin', NOW());
INSERT INTO pm_daily_report_detail (report_id, project_id, entry_type, work_hours, work_content, del_flag, create_by, create_time)
VALUES (1002, 200, 'work', 2.00, '整条日报只有不可见工时', '0', 'admin', NOW());

SELECT '015 e2e 造数完成' AS status;
