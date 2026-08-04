package com.ruoyi.project.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.DailyReport;
import com.ruoyi.project.domain.DailyReportDetail;
import com.ruoyi.project.domain.WorkCalendar;
import com.ruoyi.project.domain.request.BatchLeaveRequest;
import com.ruoyi.project.domain.vo.DailySubmissionStat;
import com.ruoyi.project.domain.vo.TeamDailyReportVO;
import com.ruoyi.project.domain.vo.TeamMemberDailyVO;
import com.ruoyi.project.mapper.DailyReportDetailMapper;
import com.ruoyi.project.mapper.DailyReportMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.mapper.ProjectMemberMapper;
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
 *
 * <p>本类中的测试分两类，维护时须区分对待（specs/015-daily-report-ownership-check）：
 *
 * <ul>
 *   <li><b>行为锁定 / 回归护栏</b>：描述的是<b>现有正确行为</b>，新写时应当<b>立刻通过</b>。
 *       若它失败，说明改动踩坏了现状，须立刻停下——而不是去改测试迁就实现。
 *   <li><b>TDD 红-绿循环</b>：描述的是<b>尚未实现的行为</b>，新写时必须<b>先失败</b>，
 *       且失败原因须是「功能缺失」而非拼写或编译错误。若它一写就通过，说明测错了东西。
 * </ul>
 *
 * <p>015 特性新增的测试在 @DisplayName 中以「[护栏]」「[TDD]」前缀标注类型。
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
    @Mock private ProjectMemberMapper projectMemberMapper;

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

        // 【015】归属校验默认放行：本类多数测试关注的是工时计算与滚动更新，而非权限。
        // 默认让「提交里出现的每个项目」都视为「填报人曾参与且项目在建」，
        // 专门验证拒绝行为的测试会在方法内重新 stub 这两处，后设置的覆盖先设置的。
        // 注意：answer 内必须容忍 null 入参——测试方法里重新 stub 同一方法时，
        // Mockito 的 when(mock.method(any())) 会先以 null 调用一次这里已注册的 answer。
        lenient().when(projectMapper.selectProjectStatesIn(any())).thenAnswer(inv -> {
            Collection<?> ids = inv.getArgument(0);
            List<Map<String, Object>> rows = new ArrayList<>();
            if (ids == null) {
                return rows;
            }
            for (Object id : ids) {
                Map<String, Object> row = new HashMap<>();
                row.put("projectId", id);
                row.put("projectName", "项目" + id);
                row.put("projectStage", "3");   // 非 '11'，即未结项
                rows.add(row);
            }
            return rows;
        });
        lenient().when(projectMemberMapper.selectEverMemberProjectIds(anyLong(), any()))
                .thenAnswer(inv -> {
                    Collection<Long> ids = inv.getArgument(1);
                    return ids == null ? new ArrayList<Long>() : new ArrayList<>(ids);
                });
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
    @DisplayName("保存日报：更新时调用 updateDailyReport 并按作用范围删除旧明细")
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
        // 【015】删除已收窄为「按作用范围」——本例未 stub 可填项目列表，故范围为空集合，
        // 语义上退化为「只清非项目工时」。无差别的 deleteByReportId 不得再被调用。
        verify(detailMapper).deleteByReportIdInScope(eq(existingReportId), any());
        verify(detailMapper, never()).deleteByReportId(anyLong());
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
    @DisplayName("保存日报：子任务工时滚动 → 先更新子任务，父项目再按明细全量汇总")
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

        // 【015】该任务确实隶属于所声明的项目，故归属校验放行
        lenient().when(taskMapper.selectTaskProjectPairs(any()))
                .thenReturn(Collections.singletonList(taskPair(taskId, projectId)));

        // Step 1: task workload rollup
        when(detailMapper.sumWorkHoursBySubProjectId(taskId)).thenReturn(new BigDecimal("16"));
        when(taskMapper.updateActualWorkload(eq(taskId), any())).thenReturn(1);

        // Step 2: 父项目按日报明细全量汇总（40 = 16 挂在任务上 + 24 直挂父项目）
        when(taskMapper.selectProjectIdsByTaskIds(anyList())).thenReturn(Collections.singletonList(parentProjectId));
        when(detailMapper.sumWorkHoursByProjectId(parentProjectId)).thenReturn(new BigDecimal("40"));
        when(projectMapper.updateActualWorkload(eq(parentProjectId), any())).thenReturn(1);

        service.saveDailyReport(report);

        // Verify step 1: task updated with summed hours
        verify(taskMapper).updateActualWorkload(taskId, new BigDecimal("16"));
        // Verify step 2: 父项目取明细全量汇总，而非 SUM(pm_task)——
        // 后者会抹掉「直挂父项目」的工时（Issue #5 ①）
        verify(projectMapper).updateActualWorkload(parentProjectId, new BigDecimal("40"));
        verify(taskMapper, never()).sumActualWorkloadByProjectId(anyLong());
    }

    @Test
    @DisplayName("保存日报回归（Issue #5 ①）：项目已有任务时，建任务前直挂父项目的工时不得被抹掉")
    void saveDailyReport_projectWithTask_keepsHoursDirectlyOnParent() throws Exception {
        Long taskId = 200L;
        Long projectId = 10L;

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

        // 【015】该任务确实隶属于所声明的项目，故归属校验放行
        lenient().when(taskMapper.selectTaskProjectPairs(any()))
                .thenReturn(Collections.singletonList(taskPair(taskId, projectId)));

        when(detailMapper.sumWorkHoursBySubProjectId(taskId)).thenReturn(new BigDecimal("89"));
        when(taskMapper.updateActualWorkload(eq(taskId), any())).thenReturn(1);
        when(taskMapper.selectProjectIdsByTaskIds(anyList())).thenReturn(Collections.singletonList(projectId));
        // 复刻生产项目106 的真实场景：89 小时挂在任务上，95 小时是建任务之前直挂父项目的
        when(detailMapper.sumWorkHoursByProjectId(projectId)).thenReturn(new BigDecimal("184"));
        when(projectMapper.updateActualWorkload(eq(projectId), any())).thenReturn(1);

        service.saveDailyReport(report);

        // 一旦回退成 SUM(pm_task.actual_workload)，此处会变成 89 —— 那 95 小时就被永久吞掉
        verify(projectMapper).updateActualWorkload(projectId, new BigDecimal("184"));
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

        // Old task's parent project —— 同样按明细全量汇总
        when(taskMapper.selectProjectIdsByTaskIds(anyList())).thenReturn(Collections.singletonList(oldProjectId));
        when(detailMapper.sumWorkHoursByProjectId(oldProjectId)).thenReturn(BigDecimal.ZERO);
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

    // ==========================================================================
    // 015 特性：日报保存的工时保护与项目归属校验
    // specs/015-daily-report-ownership-check
    // ==========================================================================

    // ---------- User Story 1a：保存日报时不丢失填报人看不见的工时 ----------

    @Test
    @DisplayName("[TDD] 保存日报：作用范围外（填报人看不见）的既有工时不得被删除")
    void saveDailyReport_invisibleProjectHours_arePreserved() throws Exception {
        Long existingReportId = 60L;
        Long visibleProjectId = 10L;    // 在建项目，出现在填报人的可填列表中
        Long invisibleProjectId = 20L;  // 已结项项目，填写页上根本不显示

        // 本次提交只含可见项目——前端只能提交它显示得出来的行
        DailyReport report = buildReport("2026-03-10");
        report.setDetailList(Collections.singletonList(
                buildDetail("work", new BigDecimal("6"), null, visibleProjectId, null)));

        // 该日既有明细：可见项目 4h + 不可见项目 2h
        List<DailyReportDetail> oldDetails = Arrays.asList(
                buildDetail("work", new BigDecimal("4"), null, visibleProjectId, null),
                buildDetail("work", new BigDecimal("2"), null, invisibleProjectId, null));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(existingReportId);
        when(dailyReportMapper.updateDailyReport(any())).thenReturn(1);
        when(detailMapper.selectByReportId(existingReportId)).thenReturn(oldDetails);

        // 可填项目列表只有 visibleProjectId —— 这就是本次操作的「作用范围」
        Map<String, Object> visible = new HashMap<>();
        visible.put("projectId", visibleProjectId);
        lenient().when(projectMapper.selectProjectsByUserId(USER_ID))
                .thenReturn(Collections.singletonList(visible));

        service.saveDailyReport(report);

        // 核心：不得再无差别删除整天明细
        verify(detailMapper, never()).deleteByReportId(anyLong());

        // 删除必须限定在作用范围内：含可见项目、不含不可见项目
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Collection> scope = ArgumentCaptor.forClass(Collection.class);
        verify(detailMapper).deleteByReportIdInScope(eq(existingReportId), scope.capture());
        assertTrue(scope.getValue().contains(visibleProjectId),
                "可见项目必须在作用范围内，否则填报人无法清零自己的工时（FR-002）");
        assertFalse(scope.getValue().contains(invisibleProjectId),
                "不可见项目不得进入作用范围——把它删掉正是静默丢失工时的根因（FR-001）");
    }

    @Test
    @DisplayName("[护栏] 保存日报：可见项目的工时清零后仍须被删除（防丢失逻辑不得堵死正常删除）")
    void saveDailyReport_visibleProjectClearedToZero_isDeleted() throws Exception {
        Long existingReportId = 61L;
        Long visibleProjectId = 10L;

        // 提交为空——填报人把当天唯一一条工时清零了，这是合法操作
        DailyReport report = buildReport("2026-03-11");
        report.setDetailList(Collections.emptyList());

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(existingReportId);
        when(dailyReportMapper.updateDailyReport(any())).thenReturn(1);
        when(detailMapper.selectByReportId(existingReportId)).thenReturn(Collections.singletonList(
                buildDetail("work", new BigDecimal("4"), null, visibleProjectId, null)));

        Map<String, Object> visible = new HashMap<>();
        visible.put("projectId", visibleProjectId);
        when(projectMapper.selectProjectsByUserId(USER_ID)).thenReturn(Collections.singletonList(visible));

        service.saveDailyReport(report);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Collection> scope = ArgumentCaptor.forClass(Collection.class);
        verify(detailMapper).deleteByReportIdInScope(eq(existingReportId), scope.capture());
        assertTrue(scope.getValue().contains(visibleProjectId),
                "可见项目必须落在删除范围内，否则填报人再也删不掉自己填错的工时（FR-002 / INV-4）");
    }

    @Test
    @DisplayName("[TDD] 保存日报：当日汇总须包含被保留的不可见工时，不能只按提交内容计算")
    void saveDailyReport_totalWorkHours_includesPreservedDetails() throws Exception {
        Long existingReportId = 62L;
        Long visibleProjectId = 10L;
        Long invisibleProjectId = 20L;

        // 提交 3h，但该日还有一条不可见的 2h 会被保留 → 当日实际为 5h
        DailyReport report = buildReport("2026-03-18");
        report.setDetailList(Collections.singletonList(
                buildDetail("work", new BigDecimal("3"), null, visibleProjectId, null)));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(existingReportId);
        when(dailyReportMapper.updateDailyReport(any())).thenReturn(1);
        when(detailMapper.selectByReportId(existingReportId)).thenReturn(Arrays.asList(
                buildDetail("work", new BigDecimal("4"), null, visibleProjectId, null),
                buildDetail("work", new BigDecimal("2"), null, invisibleProjectId, null)));

        Map<String, Object> visible = new HashMap<>();
        visible.put("projectId", visibleProjectId);
        lenient().when(projectMapper.selectProjectsByUserId(USER_ID))
                .thenReturn(Collections.singletonList(visible));
        lenient().when(detailMapper.sumWorkHoursByReportId(existingReportId))
                .thenReturn(new BigDecimal("5"));

        service.saveDailyReport(report);

        // 只按提交内容算会得到 3.00，与明细之和 5.00 不符——日历卡上的当日工时会偏小（SC-010）。
        // e2e 对账实测暴露（2026-08-03）。
        verify(dailyReportMapper).updateTotalWorkHours(eq(existingReportId), eq(new BigDecimal("5")));
    }

    // ---------- User Story 1b：删除整条日报时不丢失填报人看不见的工时 ----------

    @Test
    @DisplayName("[TDD] 删除日报：作用范围外（填报人看不见）的工时不得被连带删除")
    void deleteDailyReport_invisibleHours_arePreserved() throws Exception {
        Long reportId = 70L;
        Long visibleProjectId = 10L;
        Long invisibleProjectId = 20L;

        when(detailMapper.selectByReportId(reportId)).thenReturn(Arrays.asList(
                buildDetail("work", new BigDecimal("4"), null, visibleProjectId, null),
                buildDetail("work", new BigDecimal("2"), null, invisibleProjectId, null)));

        Map<String, Object> visible = new HashMap<>();
        visible.put("projectId", visibleProjectId);
        lenient().when(projectMapper.selectProjectsByUserId(USER_ID))
                .thenReturn(Collections.singletonList(visible));

        service.deleteDailyReportByIds(new Long[]{reportId});

        // 不得再无差别删除该日报的全部明细
        verify(detailMapper, never()).deleteByReportIds(any());

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Collection> scope = ArgumentCaptor.forClass(Collection.class);
        verify(detailMapper).deleteByReportIdInScope(eq(reportId), scope.capture());
        assertTrue(scope.getValue().contains(visibleProjectId),
                "填报人看得见的工时应当被删除——这是他执行删除的本意");
        assertFalse(scope.getValue().contains(invisibleProjectId),
                "已结项项目的历史工时不得因一次删除操作被无辜清除（FR-013，业务方 2026-07-30 确认）");
    }

    @Test
    @DisplayName("[TDD] 删除日报：仍有工时被保留时，主记录必须保留并重算当日汇总")
    void deleteDailyReport_withRemainingDetails_keepsMasterRecord() throws Exception {
        Long reportId = 71L;
        Long visibleProjectId = 10L;
        Long invisibleProjectId = 20L;

        when(detailMapper.selectByReportId(reportId)).thenReturn(Arrays.asList(
                buildDetail("work", new BigDecimal("4"), null, visibleProjectId, null),
                buildDetail("work", new BigDecimal("2"), null, invisibleProjectId, null)));

        Map<String, Object> visible = new HashMap<>();
        visible.put("projectId", visibleProjectId);
        lenient().when(projectMapper.selectProjectsByUserId(USER_ID))
                .thenReturn(Collections.singletonList(visible));

        // 删完可见部分后仍剩 1 条（不可见项目的 2h）
        lenient().when(detailMapper.countByReportId(reportId)).thenReturn(1);
        lenient().when(detailMapper.sumWorkHoursByReportId(reportId)).thenReturn(new BigDecimal("2"));

        service.deleteDailyReportByIds(new Long[]{reportId});

        // 主记录不得被软删——否则被保留的明细无主记录可归属，任何业务查询都到不了它（INV-D1 / SC-010）
        verify(dailyReportMapper, never()).deleteDailyReportByIds(any());
        // 当日汇总须按剩余 work 明细重算，否则主记录与明细不符（INV-D2）
        verify(dailyReportMapper).updateTotalWorkHours(eq(reportId), eq(new BigDecimal("2")));
    }

    @Test
    @DisplayName("[护栏] 删除日报：无任何工时需要保留时，主记录应被正常删除（不引入过度保护）")
    void deleteDailyReport_noRemainingDetails_deletesMasterRecord() throws Exception {
        Long reportId = 72L;
        Long visibleProjectId = 10L;

        when(detailMapper.selectByReportId(reportId)).thenReturn(Collections.singletonList(
                buildDetail("work", new BigDecimal("4"), null, visibleProjectId, null)));

        Map<String, Object> visible = new HashMap<>();
        visible.put("projectId", visibleProjectId);
        lenient().when(projectMapper.selectProjectsByUserId(USER_ID))
                .thenReturn(Collections.singletonList(visible));

        lenient().when(detailMapper.countByReportId(reportId)).thenReturn(0);

        service.deleteDailyReportByIds(new Long[]{reportId});

        verify(dailyReportMapper).deleteDailyReportByIds(any());
        verify(dailyReportMapper, never()).updateTotalWorkHours(anyLong(), any());
    }

    @Test
    @DisplayName("[TDD] 删除日报：明细全部被保留、无主记录可删时，仍须返回成功（否则前端误报「操作失败」）")
    void deleteDailyReport_allPreserved_stillReportsSuccess() throws Exception {
        Long reportId = 73L;
        Long invisibleProjectId = 20L;

        // 该日报只有一条不可见工时 → 删完之后什么都没删掉，也没有主记录可删
        when(detailMapper.selectByReportId(reportId)).thenReturn(Collections.singletonList(
                buildDetail("work", new BigDecimal("2"), null, invisibleProjectId, null)));
        lenient().when(projectMapper.selectProjectsByUserId(USER_ID)).thenReturn(Collections.emptyList());
        lenient().when(detailMapper.countByReportId(reportId)).thenReturn(1);
        lenient().when(detailMapper.sumWorkHoursByReportId(reportId)).thenReturn(new BigDecimal("2"));

        int rows = service.deleteDailyReportByIds(new Long[]{reportId});

        assertTrue(rows > 0,
                "返回 0 会被 BaseController.toAjax 判为「操作失败」，但数据层面操作其实已成功完成——"
                        + "填报人会看到失败提示并重复点击。e2e 实测暴露（2026-08-03）。");
    }

    // ---------- User Story 2：阻止把工时填到无关项目上 ----------

    @Test
    @DisplayName("[TDD] 保存日报：工时填到从未参与过的项目，整次保存被拒绝且不产生任何写入")
    void saveDailyReport_neverMemberProject_isRejected() throws Exception {
        Long strangerProjectId = 99L;

        DailyReport report = buildReport("2026-03-12");
        report.setDetailList(Collections.singletonList(
                buildDetail("work", new BigDecimal("8"), null, strangerProjectId, null)));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);

        // 项目存在且在建，但填报人与它毫无关系
        Map<String, Object> state = new HashMap<>();
        state.put("projectId", strangerProjectId);
        state.put("projectName", "别人的项目");
        state.put("projectStage", "3");
        lenient().when(projectMapper.selectProjectStatesIn(any()))
                .thenReturn(Collections.singletonList(state));
        lenient().when(projectMemberMapper.selectEverMemberProjectIds(eq(USER_ID), any()))
                .thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.saveDailyReport(report));

        assertTrue(ex.getMessage().contains("别人的项目"),
                "提示须指明被拒项目的名称，让填报人无需联系管理员即可判断（FR-008 / SC-006）");

        // 拒绝路径不得触发任何写入（FR-009 / INV-1）
        verify(detailMapper, never()).batchInsert(any());
        verify(detailMapper, never()).deleteByReportIdInScope(anyLong(), any());
        verify(projectMapper, never()).updateActualWorkload(anyLong(), any());
    }

    @Test
    @DisplayName("[TDD] 保存日报：工时挂在不隶属于该项目的任务上，保存被拒绝")
    void saveDailyReport_taskNotBelongingToProject_isRejected() throws Exception {
        Long projectId = 10L;
        Long taskId = 500L;   // 该任务实际隶属于项目 88，而非 10

        DailyReport report = buildReport("2026-03-13");
        report.setDetailList(Collections.singletonList(
                buildDetail("work", new BigDecimal("8"), null, projectId, taskId)));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        Map<String, Object> pair = new HashMap<>();
        pair.put("taskId", taskId);
        pair.put("projectId", 88L);
        lenient().when(taskMapper.selectTaskProjectPairs(any()))
                .thenReturn(Collections.singletonList(pair));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.saveDailyReport(report));

        assertTrue(ex.getMessage().contains("任务"),
                "提示须说明任务与所选项目不匹配（FR-007）");
        verify(detailMapper, never()).batchInsert(any());
    }

    // ---------- User Story 3：项目结项后不再接受新增或变更工时 ----------

    @Test
    @DisplayName("[TDD] 保存日报：为已结项项目新增工时，保存被拒绝")
    void saveDailyReport_closedProject_isRejected() throws Exception {
        Long closedProjectId = 30L;

        DailyReport report = buildReport("2026-03-14");
        report.setDetailList(Collections.singletonList(
                buildDetail("work", new BigDecimal("8"), null, closedProjectId, null)));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        Map<String, Object> state = new HashMap<>();
        state.put("projectId", closedProjectId);
        state.put("projectName", "已结项的项目");
        state.put("projectStage", "11");   // sys_xmjd: 11 = 项目结项
        lenient().when(projectMapper.selectProjectStatesIn(any()))
                .thenReturn(Collections.singletonList(state));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.saveDailyReport(report));

        assertTrue(ex.getMessage().contains("已结项"), "提示须说明项目已结项（FR-010 / FR-011）");
        assertTrue(ex.getMessage().contains("已结项的项目"), "提示须含项目名称（FR-008）");
        verify(detailMapper, never()).batchInsert(any());
    }

    @Test
    @DisplayName("[护栏] 保存日报：当天有已结项项目的既有工时但本次不提交它，须保存成功且原样保留")
    void saveDailyReport_closedProjectNotSubmitted_isPreservedNotRejected() throws Exception {
        Long existingReportId = 80L;
        Long visibleProjectId = 10L;
        Long closedProjectId = 30L;

        // 提交只含在建项目——填报人看不到已结项那条，自然也提交不了
        DailyReport report = buildReport("2026-03-16");
        report.setDetailList(Collections.singletonList(
                buildDetail("work", new BigDecimal("6"), null, visibleProjectId, null)));

        when(detailMapper.selectByReportId(existingReportId)).thenReturn(Arrays.asList(
                buildDetail("work", new BigDecimal("4"), null, visibleProjectId, null),
                buildDetail("work", new BigDecimal("2"), null, closedProjectId, null)));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(existingReportId);
        when(dailyReportMapper.updateDailyReport(any())).thenReturn(1);

        Map<String, Object> visible = new HashMap<>();
        visible.put("projectId", visibleProjectId);
        lenient().when(projectMapper.selectProjectsByUserId(USER_ID))
                .thenReturn(Collections.singletonList(visible));

        // 关键：校验只看「本次提交的内容」，不看「该日既有明细」。
        // 若误把既有的已结项工时也纳入校验，US3 会把 US1 要保护的场景整个拒掉——修复变成新 bug。
        assertDoesNotThrow(() -> service.saveDailyReport(report));

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Collection> scope = ArgumentCaptor.forClass(Collection.class);
        verify(detailMapper).deleteByReportIdInScope(eq(existingReportId), scope.capture());
        assertFalse(scope.getValue().contains(closedProjectId),
                "已结项项目的历史工时必须被保留——US1 与 US3 各管一侧，两条规则不得互相抵消");
    }

    // ---------- User Story 4：离场成员的历史日报仍可维护 ----------

    @Test
    @DisplayName("[护栏] 保存日报：已离场成员仍可维护其在该项目上的历史工时")
    void saveDailyReport_formerMember_canMaintainHistory() throws Exception {
        Long formerProjectId = 40L;

        DailyReport report = buildReport("2026-03-17");
        report.setDetailList(Collections.singletonList(
                buildDetail("work", new BigDecimal("8"), null, formerProjectId, null)));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(101L);
            return 1;
        });

        // 已离场（is_active='0'），但「曾参与」的凭据仍在，故该查询仍返回它
        lenient().when(projectMemberMapper.selectEverMemberProjectIds(eq(USER_ID), any()))
                .thenReturn(Collections.singletonList(formerProjectId));

        assertDoesNotThrow(() -> service.saveDailyReport(report),
                "离场成员维护历史工时不得被拒——若此处失败，说明 SQL 误加了 is_active 过滤（FR-006 / SC-007）");
    }

    @Test
    @DisplayName("[护栏] 保存日报：假期类记录不关联项目，不触发归属校验")
    void saveDailyReport_leaveEntries_skipOwnershipCheck() throws Exception {
        DailyReport report = buildReport("2026-03-15");
        report.setDetailList(Collections.singletonList(
                buildDetail("annual", null, new BigDecimal("8"), null, null)));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(null);
        when(dailyReportMapper.insertDailyReport(any())).thenAnswer(inv -> {
            DailyReport r = inv.getArgument(0);
            r.setReportId(102L);
            return 1;
        });

        assertDoesNotThrow(() -> service.saveDailyReport(report));
        // 提交里没有任何项目工时 → 根本不该发起归属查询（FR-005 / spec US2 场景 4）
        verify(projectMemberMapper, never()).selectEverMemberProjectIds(anyLong(), any());
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

    /** 【015】构造一条「任务 → 父项目」映射，供任务归属校验（V3）的 stub 使用 */
    private Map<String, Object> taskPair(Long taskId, Long projectId) {
        Map<String, Object> pair = new HashMap<>();
        pair.put("taskId", taskId);
        pair.put("projectId", projectId);
        return pair;
    }

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

    // ══════════════════════════════════════════════════════════════════════════
    // 018 项目日报（specs/018-project-daily-report）：selectTeamMonthly 聚合层
    //
    // 【务必先读】本区块是纯单元测试，dailyReportMapper 是 @Mock ——
    // 它只验证「给定这样的原始行，Java 层聚合成什么样的 VO 树」，**不执行任何 SQL**。
    //
    // SQL 形态（月窗条件化、角色 CASE 反推、三个新增 LEFT JOIN）这里一概测不到，
    // 由真实 SQL 执行与 Playwright e2e 验证，映射见 bdd/coverage.md。
    // 别被本区块的绿色骗了：它绿只证明聚合逻辑对，不证明 SQL 会产出这样的原始行。
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 构造一行 selectTeamMonthlyRaw 的原始返回行。
     * 默认值取自 project 38（兵团人社全程电子化）的真实数据形态：预算 150 人天、累计 66.750 人天。
     * 调用方按需 put 覆盖 reportDate / totalWorkHours / isFormer 等字段。
     */
    private Map<String, Object> teamRawRow(Long projectId, Long userId) {
        Map<String, Object> row = new HashMap<>();
        row.put("projectId", projectId);
        row.put("projectName", "项目" + projectId);
        row.put("hasContract", "0");
        row.put("estimatedWorkload", new BigDecimal("150.00"));
        row.put("actualPersonDays", new BigDecimal("66.750"));
        row.put("projectStage", "3");
        row.put("userId", userId);
        row.put("nickName", "用户" + userId);
        row.put("deptName", "研发中心");
        row.put("isFormer", "0");
        return row;
    }

    @Test
    @DisplayName("[护栏][018] 指定年月时：多天工时既进日历格，也累加进个人总工时")
    void selectTeamMonthly_withYearMonth_dailyHoursAndTotalPreserved() {
        Map<String, Object> d1 = teamRawRow(38L, 250L);
        d1.put("reportDate", "2026-03-02");
        d1.put("totalWorkHours", new BigDecimal("8"));
        Map<String, Object> d2 = teamRawRow(38L, 250L);
        d2.put("reportDate", "2026-03-03");
        d2.put("totalWorkHours", new BigDecimal("6"));
        Map<String, Object> d3 = teamRawRow(38L, 250L);
        d3.put("reportDate", "2026-03-04");
        d3.put("totalWorkHours", new BigDecimal("4"));
        when(dailyReportMapper.selectTeamMonthlyRaw(any())).thenReturn(Arrays.asList(d1, d2, d3));

        List<TeamDailyReportVO> result = service.selectTeamMonthly(new DailyReport());

        assertEquals(1, result.size(), "三行同项目同成员应聚合为一个项目");
        TeamMemberDailyVO member = result.get(0).getMembers().get(0);
        assertEquals(3, member.getDailyHours().size(), "三天应产生三个日历格");
        assertEquals(0, new BigDecimal("18").compareTo(member.getTotalHours()),
                "个人总工时应为 8+6+4=18");
    }

    @Test
    @DisplayName("[护栏][018] 同一项目的多个成员各自归集，工时不互相串行")
    void selectTeamMonthly_multipleMembers_aggregatedUnderSameProject() {
        Map<String, Object> a = teamRawRow(38L, 250L);
        a.put("reportDate", "2026-03-02");
        a.put("totalWorkHours", new BigDecimal("8"));
        Map<String, Object> b = teamRawRow(38L, 251L);
        b.put("reportDate", "2026-03-02");
        b.put("totalWorkHours", new BigDecimal("5"));
        when(dailyReportMapper.selectTeamMonthlyRaw(any())).thenReturn(Arrays.asList(a, b));

        List<TeamDailyReportVO> result = service.selectTeamMonthly(new DailyReport());

        assertEquals(1, result.size(), "两行属同一项目");
        assertEquals(2, result.get(0).getMembers().size(), "应有两个成员行");
        assertEquals(0, new BigDecimal("8").compareTo(result.get(0).getMembers().get(0).getTotalHours()));
        assertEquals(0, new BigDecimal("5").compareTo(result.get(0).getMembers().get(1).getTotalHours()));
    }

    @Test
    @DisplayName("[TDD][018] 不指定年月时：reportDate 为 null，个人工时仍须累加（FR-009）")
    void selectTeamMonthly_noYearMonth_totalHoursAccumulatedWithoutDate() {
        // 未指定年月时 SQL 输出 `NULL AS reportDate`，daily_hrs 退化为 per-(user,project) 全周期 SUM，
        // 因此每个 (项目,成员) 只有一行、无日期、工时为全周期合计（曲君在 project 38 的 166 小时）。
        Map<String, Object> row = teamRawRow(38L, 250L);
        row.put("reportDate", null);
        row.put("totalWorkHours", new BigDecimal("166"));
        when(dailyReportMapper.selectTeamMonthlyRaw(any())).thenReturn(Collections.singletonList(row));

        List<TeamDailyReportVO> result = service.selectTeamMonthly(new DailyReport());

        TeamMemberDailyVO member = result.get(0).getMembers().get(0);
        assertEquals(0, new BigDecimal("166").compareTo(member.getTotalHours()),
                "无年月时个人人天须取自全周期合计；现有 `reportDate != null && totalWorkHours != null` "
                        + "守卫会让整个分支不执行，把 totalHours 吞成 0");
        assertTrue(member.getDailyHours().isEmpty(), "无年月时不应产生任何日历格");
    }

    @Test
    @DisplayName("[TDD][018] 角色标签透传到成员 VO（FR-011）")
    void selectTeamMonthly_multiRole_takesHighestPriority() {
        // 优先级取一在 SQL 的 CASE 中完成（项目经理 > 市场经理 > 销售负责人 > 参与人员），
        // 聚合层只负责把 SQL 已选定的那一个透传出去。
        Map<String, Object> row = teamRawRow(55L, 103L);
        row.put("roleLabel", "项目经理");
        row.put("reportDate", "2026-03-02");
        row.put("totalWorkHours", new BigDecimal("8"));
        when(dailyReportMapper.selectTeamMonthlyRaw(any())).thenReturn(Collections.singletonList(row));

        List<TeamDailyReportVO> result = service.selectTeamMonthly(new DailyReport());

        assertEquals("项目经理", result.get(0).getMembers().get(0).getRoleLabel());
    }

    @Test
    @DisplayName("[TDD][018] 反推不出角色时 roleLabel 为 null 而非空串（FR-012）")
    void selectTeamMonthly_noRole_returnsNullLabel() {
        // 前端据此决定「只显示昵称、不显示空括号」。若这里返回 ""，虽然 v-if 同样为 falsy，
        // 但日期字段用同一套取值逻辑，"" 会让参与时间拼出「 ~ 」这种残缺串，故统一保持 null 语义。
        Map<String, Object> row = teamRawRow(38L, 250L);
        row.put("roleLabel", null);
        row.put("reportDate", "2026-03-02");
        row.put("totalWorkHours", new BigDecimal("8"));
        when(dailyReportMapper.selectTeamMonthlyRaw(any())).thenReturn(Collections.singletonList(row));

        List<TeamDailyReportVO> result = service.selectTeamMonthly(new DailyReport());

        assertNull(result.get(0).getMembers().get(0).getRoleLabel(),
                "str() 对 null 返回空串，新字段不能直接套用它");
    }

    @Test
    @DisplayName("[TDD][018] 项目机构分组不得覆盖成员本人部门（FR-014）")
    void selectTeamMonthly_projectDeptName_doesNotOverwriteMemberDeptName() {
        // 实测 2188 条在册成员行中 831 条（38%）成员部门 ≠ 项目部门，别名撞名会静默覆盖。
        Map<String, Object> row = teamRawRow(38L, 250L);
        row.put("projectDeptName", "深圳组");
        row.put("deptName", "研发中心");
        row.put("reportDate", "2026-03-02");
        row.put("totalWorkHours", new BigDecimal("8"));
        when(dailyReportMapper.selectTeamMonthlyRaw(any())).thenReturn(Collections.singletonList(row));

        List<TeamDailyReportVO> result = service.selectTeamMonthly(new DailyReport());

        assertEquals("深圳组", result.get(0).getProjectDeptName(), "项目层应拿到项目所属部门");
        assertEquals("研发中心", result.get(0).getMembers().get(0).getDeptName(),
                "成员层的部门必须仍是成员本人部门，不能被项目部门覆盖");
    }

    @Test
    @DisplayName("[TDD][018] 参与时间四个日期字段透传（FR-017）")
    void selectTeamMonthly_participationSpan_prefersReportDates() {
        // 主源=日报首末日、兜底源=成员表在册区间，两者都吐给前端，由前端决定展示口径。
        Map<String, Object> row = teamRawRow(38L, 250L);
        row.put("firstReportDate", "2026-03-02");
        row.put("lastReportDate", "2026-04-16");
        row.put("joinDate", "2026-03-11");
        row.put("leaveDate", null);
        row.put("reportDate", null);
        row.put("totalWorkHours", new BigDecimal("166"));
        when(dailyReportMapper.selectTeamMonthlyRaw(any())).thenReturn(Collections.singletonList(row));

        TeamMemberDailyVO member = service.selectTeamMonthly(new DailyReport())
                .get(0).getMembers().get(0);

        assertEquals("2026-03-02", member.getFirstReportDate());
        assertEquals("2026-04-16", member.getLastReportDate());
        assertEquals("2026-03-11", member.getJoinDate());
        assertNull(member.getLeaveDate(), "在册成员的 leave_date 为 NULL，须保持 null 而非空串");
    }
}
