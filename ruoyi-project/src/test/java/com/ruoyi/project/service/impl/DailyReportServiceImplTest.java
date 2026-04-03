package com.ruoyi.project.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.DailyReport;
import com.ruoyi.project.domain.DailyReportDetail;
import com.ruoyi.project.domain.WorkCalendar;
import com.ruoyi.project.domain.request.BatchLeaveRequest;
import com.ruoyi.project.domain.vo.DailySubmissionStat;
import com.ruoyi.project.mapper.DailyReportDetailMapper;
import com.ruoyi.project.mapper.DailyReportMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.mapper.TaskMapper;
import com.ruoyi.project.mapper.WorkCalendarMapper;
import com.ruoyi.project.service.IDailyReportWhitelistService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DailyReportServiceImpl 行为锁定测试（Characterization Test）
 * 目的：锁定现有正确行为，后续重构时防止回归
 */
@ExtendWith(MockitoExtension.class)
class DailyReportServiceImplTest {

    @InjectMocks
    private DailyReportServiceImpl service;

    @Mock private DailyReportMapper dailyReportMapper;
    @Mock private DailyReportDetailMapper detailMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private WorkCalendarMapper workCalendarMapper;
    @Mock private IDailyReportWhitelistService whitelistService;

    private MockedStatic<SecurityUtils> securityMock;

    private static final Long USER_ID = 1L;
    private static final Long DEPT_ID = 100L;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        securityMock = mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::getUserId).thenReturn(USER_ID);
        securityMock.when(SecurityUtils::getDeptId).thenReturn(DEPT_ID);
        securityMock.when(SecurityUtils::getUsername).thenReturn(USERNAME);
    }

    @AfterEach
    void tearDown() {
        securityMock.close();
    }

    // ========== saveDailyReport: totalWorkHours 计算 ==========

    @Test
    @DisplayName("保存日报：totalWorkHours 只累加 entryType=work 的工时")
    void saveDailyReport_totalWorkHoursOnlySumsWorkEntries() throws Exception {
        DailyReport report = buildReport("2026-03-10");
        DailyReportDetail work1 = buildDetail("work", new BigDecimal("3.5"), null, 10L, null);
        DailyReportDetail work2 = buildDetail("work", new BigDecimal("4.5"), null, 11L, null);
        DailyReportDetail leave = buildDetail("leave", null, new BigDecimal("8"), null, null);
        DailyReportDetail comp = buildDetail("comp", null, new BigDecimal("4"), null, null);
        DailyReportDetail annual = buildDetail("annual", null, new BigDecimal("8"), null, null);
        report.setDetailList(Arrays.asList(work1, work2, leave, comp, annual));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), eq("2026-03-10"))).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(100L);
            return 1;
        });

        service.saveDailyReport(report);

        // 3.5 + 4.5 = 8.0 (leave/comp/annual excluded)
        assertEquals(0, new BigDecimal("8.0").compareTo(report.getTotalWorkHours()),
            "totalWorkHours 应只累加 work 类型条目");
    }

    @Test
    @DisplayName("保存日报：明细为空时 totalWorkHours 为 0")
    void saveDailyReport_emptyDetailList_zeroTotalHours() throws Exception {
        DailyReport report = buildReport("2026-03-10");
        report.setDetailList(null);

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), eq("2026-03-10"))).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(100L);
            return 1;
        });

        service.saveDailyReport(report);

        assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalWorkHours()),
            "明细为空时 totalWorkHours 应为 0");
    }

    @Test
    @DisplayName("保存日报：只有非 work 条目时 totalWorkHours 为 0")
    void saveDailyReport_onlyLeaveEntries_zeroTotalHours() throws Exception {
        DailyReport report = buildReport("2026-03-10");
        DailyReportDetail leave = buildDetail("leave", null, new BigDecimal("8"), null, null);
        report.setDetailList(Collections.singletonList(leave));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), eq("2026-03-10"))).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(100L);
            return 1;
        });

        service.saveDailyReport(report);

        assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalWorkHours()),
            "只有假期条目时 totalWorkHours 应为 0");
    }

    // ========== saveDailyReport: 新增 vs 更新判断 ==========

    @Test
    @DisplayName("保存日报：新增时调用 insertDailyReport")
    void saveDailyReport_insert_whenNoExistingReport() throws Exception {
        DailyReport report = buildReport("2026-03-10");
        report.setDetailList(Collections.emptyList());

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), eq("2026-03-10"))).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(100L);
            return 1;
        });

        service.saveDailyReport(report);

        verify(dailyReportMapper).insertDailyReport(any());
        verify(dailyReportMapper, never()).updateDailyReport(any());
        assertEquals(USERNAME, report.getCreateBy());
    }

    @Test
    @DisplayName("保存日报：更新时调用 updateDailyReport 并删除旧明细")
    void saveDailyReport_update_whenExistingReport() throws Exception {
        Long existingReportId = 50L;
        DailyReport report = buildReport("2026-03-10");
        report.setDetailList(Collections.emptyList());

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), eq("2026-03-10"))).thenReturn(existingReportId);
        when(dailyReportMapper.updateDailyReport(any())).thenReturn(1);
        when(detailMapper.selectByReportId(existingReportId)).thenReturn(Collections.emptyList());

        service.saveDailyReport(report);

        verify(dailyReportMapper).updateDailyReport(any());
        verify(dailyReportMapper, never()).insertDailyReport(any());
        verify(detailMapper).deleteByReportId(existingReportId);
        assertEquals(existingReportId, report.getReportId());
        assertEquals(USERNAME, report.getUpdateBy());
    }

    // ========== saveDailyReport: 白名单用户禁止提交 ==========

    @Test
    @DisplayName("保存日报：白名单用户抛出异常")
    void saveDailyReport_whitelistUser_throwsException() throws Exception {
        DailyReport report = buildReport("2026-03-10");
        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(true);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.saveDailyReport(report));
        assertTrue(ex.getMessage().contains("无需填写日报"));
    }

    // ========== saveDailyReport: entryType 默认值和假期行处理 ==========

    @Test
    @DisplayName("保存日报：entryType 为空时默认设为 work")
    void saveDailyReport_defaultEntryType() throws Exception {
        DailyReport report = buildReport("2026-03-10");
        DailyReportDetail detail = new DailyReportDetail();
        detail.setWorkHours(new BigDecimal("4"));
        detail.setProjectId(10L);
        // entryType is null
        report.setDetailList(Collections.singletonList(detail));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(100L);
            return 1;
        });

        service.saveDailyReport(report);

        assertEquals("work", detail.getEntryType(), "空 entryType 应默认为 work");
    }

    @Test
    @DisplayName("保存日报：假期行 workContent 默认空字符串")
    void saveDailyReport_leaveEntry_defaultWorkContent() throws Exception {
        DailyReport report = buildReport("2026-03-10");
        DailyReportDetail leave = buildDetail("leave", null, new BigDecimal("8"), null, null);
        leave.setWorkContent(null); // explicitly null
        report.setDetailList(Collections.singletonList(leave));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(100L);
            return 1;
        });

        service.saveDailyReport(report);

        assertEquals("", leave.getWorkContent(), "假期行 workContent 应默认空字符串");
    }

    @Test
    @DisplayName("保存日报：假期行 workHours 设为 leaveHours 的值")
    void saveDailyReport_leaveEntry_workHoursFromLeaveHours() throws Exception {
        DailyReport report = buildReport("2026-03-10");
        DailyReportDetail leave = buildDetail("leave", null, new BigDecimal("6"), null, null);
        report.setDetailList(Collections.singletonList(leave));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(100L);
            return 1;
        });

        service.saveDailyReport(report);

        assertEquals(0, new BigDecimal("6").compareTo(leave.getWorkHours()),
            "假期行 workHours 应等于 leaveHours");
    }

    // ========== saveDailyReport: 工时滚动更新 ==========

    @Test
    @DisplayName("保存日报：子任务工时滚动 → 先更新子任务，再汇总到主项目")
    void saveDailyReport_workloadRollup_taskThenProject() throws Exception {
        Long taskId = 200L;
        Long projectId = 10L;
        Long parentProjectId = 10L;

        DailyReport report = buildReport("2026-03-10");
        DailyReportDetail work = buildDetail("work", new BigDecimal("4"), null, projectId, taskId);
        report.setDetailList(Collections.singletonList(work));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(100L);
            return 1;
        });

        // Step 1: task workload rollup
        when(detailMapper.sumWorkHoursBySubProjectId(taskId)).thenReturn(new BigDecimal("16"));
        when(taskMapper.updateActualWorkload(eq(taskId), any())).thenReturn(1);

        // Step 2: parent project rollup from tasks
        when(taskMapper.selectProjectIdsByTaskIds(anyList())).thenReturn(Collections.singletonList(parentProjectId));
        when(taskMapper.sumActualWorkloadByProjectId(parentProjectId)).thenReturn(new BigDecimal("32"));
        when(projectMapper.updateActualWorkload(eq(parentProjectId), any())).thenReturn(1);

        service.saveDailyReport(report);

        // Verify step 1: task updated with summed hours
        verify(taskMapper).updateActualWorkload(taskId, new BigDecimal("16"));
        // Verify step 2: parent project updated with task sum
        verify(projectMapper).updateActualWorkload(parentProjectId, new BigDecimal("32"));
    }

    @Test
    @DisplayName("保存日报：无子任务的普通项目直接从明细汇总工时")
    void saveDailyReport_workloadRollup_directProjectNoTask() throws Exception {
        Long projectId = 10L;

        DailyReport report = buildReport("2026-03-10");
        DailyReportDetail work = buildDetail("work", new BigDecimal("4"), null, projectId, null);
        report.setDetailList(Collections.singletonList(work));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(100L);
            return 1;
        });

        when(detailMapper.sumWorkHoursByProjectId(projectId)).thenReturn(new BigDecimal("24"));
        when(projectMapper.updateActualWorkload(eq(projectId), any())).thenReturn(1);

        service.saveDailyReport(report);

        // No task updates
        verify(taskMapper, never()).updateActualWorkload(anyLong(), any());
        // Direct project workload update
        verify(projectMapper).updateActualWorkload(projectId, new BigDecimal("24"));
    }

    @Test
    @DisplayName("保存日报：更新时旧明细的子任务和项目也参与工时重算")
    void saveDailyReport_update_oldDetailsAlsoRecomputed() throws Exception {
        Long existingReportId = 50L;
        Long oldTaskId = 300L;
        Long oldProjectId = 20L;
        Long newProjectId = 10L;

        DailyReport report = buildReport("2026-03-10");
        DailyReportDetail newWork = buildDetail("work", new BigDecimal("4"), null, newProjectId, null);
        report.setDetailList(Collections.singletonList(newWork));

        // Old detail with a different sub-project
        DailyReportDetail oldDetail = new DailyReportDetail();
        oldDetail.setSubProjectId(oldTaskId);
        oldDetail.setProjectId(oldProjectId);
        oldDetail.setEntryType("work");

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(existingReportId);
        when(dailyReportMapper.updateDailyReport(any())).thenReturn(1);
        when(detailMapper.selectByReportId(existingReportId)).thenReturn(Collections.singletonList(oldDetail));

        // Old task rollup
        when(detailMapper.sumWorkHoursBySubProjectId(oldTaskId)).thenReturn(BigDecimal.ZERO);
        when(taskMapper.updateActualWorkload(eq(oldTaskId), any())).thenReturn(1);

        // Old task's parent project
        when(taskMapper.selectProjectIdsByTaskIds(anyList())).thenReturn(Collections.singletonList(oldProjectId));
        when(taskMapper.sumActualWorkloadByProjectId(oldProjectId)).thenReturn(BigDecimal.ZERO);
        when(projectMapper.updateActualWorkload(eq(oldProjectId), any())).thenReturn(1);

        // New direct project
        when(detailMapper.sumWorkHoursByProjectId(newProjectId)).thenReturn(new BigDecimal("4"));
        when(projectMapper.updateActualWorkload(eq(newProjectId), any())).thenReturn(1);

        service.saveDailyReport(report);

        // Old task should be recomputed (zeroed out)
        verify(taskMapper).updateActualWorkload(oldTaskId, BigDecimal.ZERO);
        // Old parent project recomputed
        verify(projectMapper).updateActualWorkload(oldProjectId, BigDecimal.ZERO);
        // New project recomputed
        verify(projectMapper).updateActualWorkload(newProjectId, new BigDecimal("4"));
    }

    @Test
    @DisplayName("保存日报：假期条目不参与工时滚动更新")
    void saveDailyReport_leaveEntries_noWorkloadRollup() throws Exception {
        DailyReport report = buildReport("2026-03-10");
        DailyReportDetail leave = buildDetail("leave", null, new BigDecimal("8"), null, null);
        report.setDetailList(Collections.singletonList(leave));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(100L);
            return 1;
        });

        service.saveDailyReport(report);

        // No workload rollup for non-work entries
        verify(taskMapper, never()).updateActualWorkload(anyLong(), any());
        verify(projectMapper, never()).updateActualWorkload(anyLong(), any());
    }

    // ========== selectWeeklyStats: 工作日判定 ==========

    @Test
    @DisplayName("周统计：普通工作日（周一至周五）标记为工作日")
    void selectWeeklyStats_normalWeekday_isWorkday() {
        DailyReport query = new DailyReport();
        // 2026-03 has March 2 (Mon) to March 6 (Fri) as a normal workweek
        query.setYearMonth("2026-03");

        when(dailyReportMapper.selectSubmittedCountByDate(any())).thenReturn(Collections.emptyList());
        when(dailyReportMapper.selectTotalUserCount(any())).thenReturn(10);
        when(dailyReportMapper.selectTotalUserCountByDate(any())).thenReturn(Collections.emptyList());
        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.emptyList());

        List<DailySubmissionStat> result = service.selectWeeklyStats(query);

        // March 2, 2026 = Monday
        DailySubmissionStat monday = result.stream()
            .filter(s -> "2026-03-02".equals(s.getReportDate()))
            .findFirst().orElseThrow();
        assertTrue(monday.getIsWorkday(), "周一应为工作日");
        assertEquals("周一", monday.getDayOfWeek());
    }

    @Test
    @DisplayName("周统计：周末默认标记为非工作日")
    void selectWeeklyStats_weekend_isNotWorkday() {
        DailyReport query = new DailyReport();
        query.setYearMonth("2026-03");

        when(dailyReportMapper.selectSubmittedCountByDate(any())).thenReturn(Collections.emptyList());
        when(dailyReportMapper.selectTotalUserCount(any())).thenReturn(10);
        when(dailyReportMapper.selectTotalUserCountByDate(any())).thenReturn(Collections.emptyList());
        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.emptyList());

        List<DailySubmissionStat> result = service.selectWeeklyStats(query);

        // March 7, 2026 = Saturday
        DailySubmissionStat saturday = result.stream()
            .filter(s -> "2026-03-07".equals(s.getReportDate()))
            .findFirst().orElseThrow();
        assertFalse(saturday.getIsWorkday(), "周六应为非工作日");
        assertEquals("周六", saturday.getDayOfWeek());

        // March 8, 2026 = Sunday
        DailySubmissionStat sunday = result.stream()
            .filter(s -> "2026-03-08".equals(s.getReportDate()))
            .findFirst().orElseThrow();
        assertFalse(sunday.getIsWorkday(), "周日应为非工作日");
        assertEquals("周日", sunday.getDayOfWeek());
    }

    @Test
    @DisplayName("周统计：工作日历节假日覆盖周一为非工作日")
    void selectWeeklyStats_calendarHoliday_overridesWeekday() {
        DailyReport query = new DailyReport();
        query.setYearMonth("2026-03");

        // Mark March 2 (Monday) as holiday
        WorkCalendar holiday = buildCalendar("2026-03-02", "holiday");
        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.singletonList(holiday));
        when(dailyReportMapper.selectSubmittedCountByDate(any())).thenReturn(Collections.emptyList());
        when(dailyReportMapper.selectTotalUserCount(any())).thenReturn(10);
        when(dailyReportMapper.selectTotalUserCountByDate(any())).thenReturn(Collections.emptyList());

        List<DailySubmissionStat> result = service.selectWeeklyStats(query);

        DailySubmissionStat monday = result.stream()
            .filter(s -> "2026-03-02".equals(s.getReportDate()))
            .findFirst().orElseThrow();
        assertFalse(monday.getIsWorkday(), "日历标记为 holiday 的周一应为非工作日");
    }

    @Test
    @DisplayName("周统计：工作日历调班覆盖周六为工作日")
    void selectWeeklyStats_calendarForcedWorkday_overridesWeekend() {
        DailyReport query = new DailyReport();
        query.setYearMonth("2026-03");

        // Mark March 7 (Saturday) as forced workday
        WorkCalendar forcedWorkday = buildCalendar("2026-03-07", "workday");
        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.singletonList(forcedWorkday));
        when(dailyReportMapper.selectSubmittedCountByDate(any())).thenReturn(Collections.emptyList());
        when(dailyReportMapper.selectTotalUserCount(any())).thenReturn(10);
        when(dailyReportMapper.selectTotalUserCountByDate(any())).thenReturn(Collections.emptyList());

        List<DailySubmissionStat> result = service.selectWeeklyStats(query);

        DailySubmissionStat saturday = result.stream()
            .filter(s -> "2026-03-07".equals(s.getReportDate()))
            .findFirst().orElseThrow();
        assertTrue(saturday.getIsWorkday(), "日历标记为 workday 的周六应为工作日");
    }

    @Test
    @DisplayName("周统计：非工作日的未提交人数为 0")
    void selectWeeklyStats_nonWorkday_unsubmittedCountIsZero() {
        DailyReport query = new DailyReport();
        query.setYearMonth("2026-01"); // January 2026 — use past month to avoid future

        when(dailyReportMapper.selectSubmittedCountByDate(any())).thenReturn(Collections.emptyList());
        when(dailyReportMapper.selectTotalUserCount(any())).thenReturn(10);
        when(dailyReportMapper.selectTotalUserCountByDate(any())).thenReturn(Collections.emptyList());
        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.emptyList());

        List<DailySubmissionStat> result = service.selectWeeklyStats(query);

        // Jan 3, 2026 = Saturday
        DailySubmissionStat saturday = result.stream()
            .filter(s -> "2026-01-03".equals(s.getReportDate()))
            .findFirst().orElseThrow();
        assertFalse(saturday.getIsWorkday());
        assertEquals(0, saturday.getUnsubmittedCount(), "非工作日未提交人数应为 0");
    }

    @Test
    @DisplayName("周统计：工作日的未提交人数 = 总人数 - 已提交人数")
    void selectWeeklyStats_workday_unsubmittedCountCalculation() {
        DailyReport query = new DailyReport();
        query.setYearMonth("2026-01");

        // Jan 2 (Friday) has 3 submitted
        Map<String, Object> submittedRow = new HashMap<>();
        submittedRow.put("reportDate", "2026-01-02");
        submittedRow.put("submittedCount", 3);
        when(dailyReportMapper.selectSubmittedCountByDate(any())).thenReturn(Collections.singletonList(submittedRow));
        when(dailyReportMapper.selectTotalUserCount(any())).thenReturn(10);
        when(dailyReportMapper.selectTotalUserCountByDate(any())).thenReturn(Collections.emptyList());
        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.emptyList());

        List<DailySubmissionStat> result = service.selectWeeklyStats(query);

        DailySubmissionStat friday = result.stream()
            .filter(s -> "2026-01-02".equals(s.getReportDate()))
            .findFirst().orElseThrow();
        assertTrue(friday.getIsWorkday());
        assertEquals(3, friday.getSubmittedCount());
        assertEquals(7, friday.getUnsubmittedCount(), "未提交 = 10 - 3 = 7");
    }

    @Test
    @DisplayName("周统计：每日总人数优先使用 totalByDateMap 数据")
    void selectWeeklyStats_usesTotalByDateMap_whenAvailable() {
        DailyReport query = new DailyReport();
        query.setYearMonth("2026-01");

        // Jan 2 (Friday): submitted=2, totalByDate=5 (overrides default total=10)
        Map<String, Object> submittedRow = new HashMap<>();
        submittedRow.put("reportDate", "2026-01-02");
        submittedRow.put("submittedCount", 2);
        when(dailyReportMapper.selectSubmittedCountByDate(any())).thenReturn(Collections.singletonList(submittedRow));
        when(dailyReportMapper.selectTotalUserCount(any())).thenReturn(10);

        Map<String, Object> totalByDateRow = new HashMap<>();
        totalByDateRow.put("reportDate", "2026-01-02");
        totalByDateRow.put("totalCount", 5);
        when(dailyReportMapper.selectTotalUserCountByDate(any())).thenReturn(Collections.singletonList(totalByDateRow));
        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.emptyList());

        List<DailySubmissionStat> result = service.selectWeeklyStats(query);

        DailySubmissionStat friday = result.stream()
            .filter(s -> "2026-01-02".equals(s.getReportDate()))
            .findFirst().orElseThrow();
        assertEquals(3, friday.getUnsubmittedCount(), "未提交 = 5(dailyTotal) - 2(submitted) = 3");
    }

    @Test
    @DisplayName("周统计：结果包含整月每一天")
    void selectWeeklyStats_coversEntireMonth() {
        DailyReport query = new DailyReport();
        query.setYearMonth("2026-02"); // Feb 2026 has 28 days

        when(dailyReportMapper.selectSubmittedCountByDate(any())).thenReturn(Collections.emptyList());
        when(dailyReportMapper.selectTotalUserCount(any())).thenReturn(5);
        when(dailyReportMapper.selectTotalUserCountByDate(any())).thenReturn(Collections.emptyList());
        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.emptyList());

        List<DailySubmissionStat> result = service.selectWeeklyStats(query);

        assertEquals(28, result.size(), "二月应有 28 天");
        assertEquals("2026-02-01", result.get(0).getReportDate());
        assertEquals("2026-02-28", result.get(27).getReportDate());
    }

    // ========== batchSaveLeave ==========

    @Test
    @DisplayName("批量请假：entryType=work 抛异常")
    void batchSaveLeave_workEntryType_throwsException() {
        BatchLeaveRequest request = new BatchLeaveRequest();
        request.setEntryType("work");
        request.setStartDate("2026-03-02");
        request.setEndDate("2026-03-06");

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.batchSaveLeave(request));
        assertTrue(ex.getMessage().contains("假期类型不合法"));
    }

    @Test
    @DisplayName("批量请假：startDate 晚于 endDate 抛异常")
    void batchSaveLeave_invertedDateRange_throwsException() {
        BatchLeaveRequest request = new BatchLeaveRequest();
        request.setEntryType("leave");
        request.setStartDate("2026-03-10");
        request.setEndDate("2026-03-05");

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.batchSaveLeave(request));
        assertTrue(ex.getMessage().contains("startDate 不能晚于 endDate"));
    }

    @Test
    @DisplayName("批量请假：跳过周末（周六周日不生成日报）")
    void batchSaveLeave_skipsWeekends() {
        // 2026-03-06 (Fri) to 2026-03-09 (Mon) — Sat+Sun should be skipped
        BatchLeaveRequest request = new BatchLeaveRequest();
        request.setEntryType("leave");
        request.setStartDate("2026-03-06");
        request.setEndDate("2026-03-09");
        request.setLeaveHoursPerDay(new BigDecimal("8"));

        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.emptyList());
        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        // Each day: no existing report
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(System.nanoTime());
            return 1;
        });

        Map<String, Integer> result = service.batchSaveLeave(request);

        assertEquals(2, result.get("totalWorkdays"), "应只有 Fri + Mon = 2 个工作日");
        assertEquals(2, result.get("created"));
    }

    @Test
    @DisplayName("批量请假：跳过工作日历标记的节假日")
    void batchSaveLeave_skipsHolidays() {
        // 2026-03-02 (Mon) to 2026-03-04 (Wed), mark Mar 3 as holiday
        BatchLeaveRequest request = new BatchLeaveRequest();
        request.setEntryType("leave");
        request.setStartDate("2026-03-02");
        request.setEndDate("2026-03-04");
        request.setLeaveHoursPerDay(new BigDecimal("8"));

        WorkCalendar holiday = new WorkCalendar();
        try { holiday.setCalendarDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-03-03")); } catch (Exception e) {}
        holiday.setDayType("holiday");
        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.singletonList(holiday));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(System.nanoTime());
            return 1;
        });

        Map<String, Integer> result = service.batchSaveLeave(request);

        assertEquals(2, result.get("totalWorkdays"), "Mon+Wed=2 (Tue is holiday)");
        assertEquals(2, result.get("created"));
    }

    @Test
    @DisplayName("批量请假：调班工作日的周末不跳过")
    void batchSaveLeave_forcedWorkdayWeekend_notSkipped() {
        // 2026-03-07 (Sat) to 2026-03-08 (Sun), mark Sat as forced workday
        BatchLeaveRequest request = new BatchLeaveRequest();
        request.setEntryType("leave");
        request.setStartDate("2026-03-07");
        request.setEndDate("2026-03-08");
        request.setLeaveHoursPerDay(new BigDecimal("8"));

        WorkCalendar forcedWorkday = new WorkCalendar();
        try { forcedWorkday.setCalendarDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-03-07")); } catch (Exception e) {}
        forcedWorkday.setDayType("workday");
        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.singletonList(forcedWorkday));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(System.nanoTime());
            return 1;
        });

        Map<String, Integer> result = service.batchSaveLeave(request);

        assertEquals(1, result.get("totalWorkdays"), "只有周六(调班)算工作日");
        assertEquals(1, result.get("created"));
    }

    @Test
    @DisplayName("批量请假：冲突策略 skip — 已有同类假期的日期跳过")
    void batchSaveLeave_conflictSkip() {
        BatchLeaveRequest request = new BatchLeaveRequest();
        request.setEntryType("leave");
        request.setStartDate("2026-03-02");
        request.setEndDate("2026-03-03");
        request.setConflictStrategy("skip");
        request.setLeaveHoursPerDay(new BigDecimal("8"));

        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.emptyList());
        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);

        // Mar 2 (Mon): existing report with leave
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), eq("2026-03-02"))).thenReturn(50L);
        DailyReportDetail existingLeave = new DailyReportDetail();
        existingLeave.setEntryType("leave");
        existingLeave.setWorkHours(new BigDecimal("8"));
        when(detailMapper.selectByReportId(50L)).thenReturn(Collections.singletonList(existingLeave));

        // Mar 3 (Tue): no existing report
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), eq("2026-03-03"))).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(System.nanoTime());
            return 1;
        });

        Map<String, Integer> result = service.batchSaveLeave(request);

        assertEquals(2, result.get("totalWorkdays"));
        assertEquals(1, result.get("skipped"), "Mar 2 应被跳过");
        assertEquals(1, result.get("created"), "Mar 3 应被创建");
        assertEquals(0, result.get("overwritten"));
    }

    @Test
    @DisplayName("批量请假：冲突策略 overwrite — 覆盖已有同类假期条目")
    void batchSaveLeave_conflictOverwrite() {
        BatchLeaveRequest request = new BatchLeaveRequest();
        request.setEntryType("leave");
        request.setStartDate("2026-03-02");
        request.setEndDate("2026-03-02");
        request.setConflictStrategy("overwrite");
        request.setLeaveHoursPerDay(new BigDecimal("4"));

        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.emptyList());
        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);

        // Existing report with work + leave entries
        Long existingReportId = 50L;
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), eq("2026-03-02"))).thenReturn(existingReportId);
        DailyReportDetail existingWork = new DailyReportDetail();
        existingWork.setEntryType("work");
        existingWork.setWorkHours(new BigDecimal("4"));
        existingWork.setProjectId(10L);
        DailyReportDetail existingLeave = new DailyReportDetail();
        existingLeave.setEntryType("leave");
        existingLeave.setWorkHours(new BigDecimal("8"));
        when(detailMapper.selectByReportId(existingReportId)).thenReturn(Arrays.asList(existingWork, existingLeave));

        // The overwritten call goes through saveDailyReport which does another selectReportIdByUserAndDate
        when(dailyReportMapper.updateDailyReport(any())).thenReturn(1);
        when(detailMapper.selectByReportId(existingReportId))
            .thenReturn(Arrays.asList(existingWork, existingLeave)) // first call in batchSaveLeave
            .thenReturn(Collections.emptyList()); // second call inside saveDailyReport

        Map<String, Integer> result = service.batchSaveLeave(request);

        assertEquals(1, result.get("totalWorkdays"));
        assertEquals(0, result.get("skipped"));
        assertEquals(1, result.get("overwritten"), "应覆盖一条");
    }

    @Test
    @DisplayName("批量请假：范围内无工作日抛异常")
    void batchSaveLeave_noWorkdays_throwsException() {
        // 2026-03-07 (Sat) to 2026-03-08 (Sun) — no forced workdays
        BatchLeaveRequest request = new BatchLeaveRequest();
        request.setEntryType("leave");
        request.setStartDate("2026-03-07");
        request.setEndDate("2026-03-08");

        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.batchSaveLeave(request));
        assertTrue(ex.getMessage().contains("无工作日"));
    }

    @Test
    @DisplayName("批量请假：不同类型假期不算冲突")
    void batchSaveLeave_differentLeaveType_noConflict() {
        BatchLeaveRequest request = new BatchLeaveRequest();
        request.setEntryType("annual");
        request.setStartDate("2026-03-02");
        request.setEndDate("2026-03-02");
        request.setConflictStrategy("skip");
        request.setLeaveHoursPerDay(new BigDecimal("8"));

        when(workCalendarMapper.selectByYear(2026)).thenReturn(Collections.emptyList());
        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);

        // Existing report has "leave" type (not "annual")
        Long existingReportId = 50L;
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), eq("2026-03-02"))).thenReturn(existingReportId);
        DailyReportDetail existingLeave = new DailyReportDetail();
        existingLeave.setEntryType("leave");
        existingLeave.setWorkHours(new BigDecimal("8"));
        when(detailMapper.selectByReportId(existingReportId))
            .thenReturn(Collections.singletonList(existingLeave)) // first call in batchSaveLeave
            .thenReturn(Collections.emptyList()); // second call inside saveDailyReport (update path)

        when(dailyReportMapper.updateDailyReport(any())).thenReturn(1);

        Map<String, Integer> result = service.batchSaveLeave(request);

        assertEquals(1, result.get("created"), "不同类型假期不算冲突，应被创建");
        assertEquals(0, result.get("skipped"));
    }

    // ========== selectMyProjects ==========

    @Test
    @DisplayName("我的项目：空列表不调用 hasSubProject 查询")
    void selectMyProjects_emptyList_noHasSubCheck() {
        when(projectMapper.selectProjectsByUserId(USER_ID)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = service.selectMyProjects();

        assertTrue(result.isEmpty());
        verify(taskMapper, never()).selectProjectsHasTasks(anyList());
    }

    @Test
    @DisplayName("我的项目：有子任务的项目标记 hasSubProject=true")
    void selectMyProjects_setsHasSubProjectFlag() {
        Map<String, Object> proj1 = new HashMap<>();
        proj1.put("projectId", 10L);
        Map<String, Object> proj2 = new HashMap<>();
        proj2.put("projectId", 20L);
        when(projectMapper.selectProjectsByUserId(USER_ID)).thenReturn(Arrays.asList(proj1, proj2));
        when(taskMapper.selectProjectsHasTasks(anyList())).thenReturn(Collections.singletonList(10L));

        List<Map<String, Object>> result = service.selectMyProjects();

        assertEquals(true, result.get(0).get("hasSubProject"));
        assertEquals(false, result.get(1).get("hasSubProject"));
    }

    // ========== helper methods ==========

    private DailyReport buildReport(String dateStr) throws Exception {
        DailyReport report = new DailyReport();
        report.setReportDate(new SimpleDateFormat("yyyy-MM-dd").parse(dateStr));
        return report;
    }

    private DailyReportDetail buildDetail(String entryType, BigDecimal workHours,
                                           BigDecimal leaveHours, Long projectId, Long subProjectId) {
        DailyReportDetail detail = new DailyReportDetail();
        detail.setEntryType(entryType);
        detail.setWorkHours(workHours);
        detail.setLeaveHours(leaveHours);
        detail.setProjectId(projectId);
        detail.setSubProjectId(subProjectId);
        detail.setWorkContent("test content");
        return detail;
    }

    private WorkCalendar buildCalendar(String dateStr, String dayType) {
        WorkCalendar wc = new WorkCalendar();
        wc.setCalendarDateStr(dateStr);
        wc.setDayType(dayType);
        return wc;
    }
}
