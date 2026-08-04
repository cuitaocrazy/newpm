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
        verify(detailMapper).deleteByReportIdInScope(eq(existingReportId), any(), eq(USER_ID));
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
        verify(detailMapper).deleteByReportIdInScope(eq(existingReportId), scope.capture(), eq(USER_ID));
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
        verify(detailMapper).deleteByReportIdInScope(eq(existingReportId), scope.capture(), eq(USER_ID));
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
        verify(dailyReportMapper).updateTotalWorkHours(eq(existingReportId), eq(new BigDecimal("5")), eq(USER_ID));
    }

    @Test
    @DisplayName("[TDD] 保存日报：本次提交里出现的项目必须纳入作用范围，否则旧明细删不掉会重复累加")
    void saveDailyReport_submittedProjectOutsideMyProjects_stillReplacesOldDetails() throws Exception {
        Long existingReportId = 63L;
        // 待审核 / 已暂停的项目：approval_status≠'1' 或 project_status≠'0'，
        // 于是它不出现在 selectProjectsByUserId 的结果里
        Long pendingProjectId = 70L;

        DailyReport report = buildReport("2026-03-19");
        report.setDetailList(Collections.singletonList(
                buildDetail("work", new BigDecimal("8"), null, pendingProjectId, null)));

        when(whitelistService.isInWhitelist(USER_ID)).thenReturn(false);
        when(dailyReportMapper.selectReportIdByUserAndDate(eq(USER_ID), any())).thenReturn(existingReportId);
        when(dailyReportMapper.updateDailyReport(any())).thenReturn(1);
        // 该日已有同一项目的旧明细
        when(detailMapper.selectByReportId(existingReportId)).thenReturn(Collections.singletonList(
                buildDetail("work", new BigDecimal("8"), null, pendingProjectId, null)));

        // 可填项目列表为空——项目因生命周期状态被 selectProjectsByUserId 过滤掉了
        lenient().when(projectMapper.selectProjectsByUserId(USER_ID)).thenReturn(Collections.emptyList());

        service.saveDailyReport(report);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Collection> scope = ArgumentCaptor.forClass(Collection.class);
        verify(detailMapper).deleteByReportIdInScope(eq(existingReportId), scope.capture(), eq(USER_ID));
        assertTrue(scope.getValue().contains(pendingProjectId),
                "填报人这次明确提交了该项目的工时，它就归本次提交管——旧明细必须先删。"
                        + "否则旧的留着、新的又插进来，工时凭空翻倍。e2e 回归实测暴露（2026-08-03）。");
    }

    // ---------- User Story 1b：删除整条日报时不丢失填报人看不见的工时 ----------

    @Test
    @DisplayName("[TDD] 删除日报：作用范围外（填报人看不见）的工时不得被连带删除")
    void deleteDailyReport_invisibleHours_arePreserved() throws Exception {
        Long reportId = 70L;
        // 【Issue #13】归属探针：该主记录存在且归属本人。
        // 不 stub 的话 ownedReportIds 为空，整条日报会被当成「主记录不存在」而跳过。
        lenient().when(dailyReportMapper.selectReportOwnersForUpdate(any()))
                .thenReturn(Collections.singletonList(ownerRow(reportId, USER_ID)));
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
        verify(detailMapper).deleteByReportIdInScope(eq(reportId), scope.capture(), eq(USER_ID));
        assertTrue(scope.getValue().contains(visibleProjectId),
                "填报人看得见的工时应当被删除——这是他执行删除的本意");
        assertFalse(scope.getValue().contains(invisibleProjectId),
                "已结项项目的历史工时不得因一次删除操作被无辜清除（FR-013，业务方 2026-07-30 确认）");
    }

    @Test
    @DisplayName("[TDD] 删除日报：仍有工时被保留时，主记录必须保留并重算当日汇总")
    void deleteDailyReport_withRemainingDetails_keepsMasterRecord() throws Exception {
        Long reportId = 71L;
        // 【Issue #13】归属探针：该主记录存在且归属本人。
        // 不 stub 的话 ownedReportIds 为空，整条日报会被当成「主记录不存在」而跳过。
        lenient().when(dailyReportMapper.selectReportOwnersForUpdate(any()))
                .thenReturn(Collections.singletonList(ownerRow(reportId, USER_ID)));
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
        // ⚠️ 第二个参数用 any() 而不是 anyLong()：Mockito 的 anyLong() 不匹配 null，
        //    用它会让「userId 传了 null 的越权删除」从这条护栏底下溜过去（护栏反而更窄）。
        verify(dailyReportMapper, never()).deleteDailyReportByIds(any(), any());
        // 当日汇总须按剩余 work 明细重算，否则主记录与明细不符（INV-D2）
        verify(dailyReportMapper).updateTotalWorkHours(eq(reportId), eq(new BigDecimal("2")), eq(USER_ID));
    }

    @Test
    @DisplayName("[护栏] 删除日报：无任何工时需要保留时，主记录应被正常删除（不引入过度保护）")
    void deleteDailyReport_noRemainingDetails_deletesMasterRecord() throws Exception {
        Long reportId = 72L;
        // 【Issue #13】归属探针：该主记录存在且归属本人。
        // 不 stub 的话 ownedReportIds 为空，整条日报会被当成「主记录不存在」而跳过。
        lenient().when(dailyReportMapper.selectReportOwnersForUpdate(any()))
                .thenReturn(Collections.singletonList(ownerRow(reportId, USER_ID)));
        Long visibleProjectId = 10L;

        when(detailMapper.selectByReportId(reportId)).thenReturn(Collections.singletonList(
                buildDetail("work", new BigDecimal("4"), null, visibleProjectId, null)));

        Map<String, Object> visible = new HashMap<>();
        visible.put("projectId", visibleProjectId);
        lenient().when(projectMapper.selectProjectsByUserId(USER_ID))
                .thenReturn(Collections.singletonList(visible));

        lenient().when(detailMapper.countByReportId(reportId)).thenReturn(0);

        service.deleteDailyReportByIds(new Long[]{reportId});

        // userId 必须断言成 eq(USER_ID)：删除语句带 "and user_id = #{userId}"，
        // 传错人就是 0 行；而返回值是「处理条数」与影响行数解耦，
        // 「SQL 一行没删却报成功」不会体现在响应上，只能靠这里钉住（Issue #13）。
        ArgumentCaptor<Long[]> deletedIds = ArgumentCaptor.forClass(Long[].class);
        verify(dailyReportMapper).deleteDailyReportByIds(deletedIds.capture(), eq(USER_ID));
        assertArrayEquals(new Long[]{reportId}, deletedIds.getValue(),
                "只应删除本次请求里的日报，且 ID 不得在传递过程中被改写");
        verify(dailyReportMapper, never()).updateTotalWorkHours(any(), any(), any());
    }

    @Test
    @DisplayName("[TDD] 删除日报：明细全部被保留、无主记录可删时，仍须返回成功（否则前端误报「操作失败」）")
    void deleteDailyReport_allPreserved_stillReportsSuccess() throws Exception {
        Long reportId = 73L;
        // 【Issue #13】归属探针：该主记录存在且归属本人。
        // 不 stub 的话 ownedReportIds 为空，整条日报会被当成「主记录不存在」而跳过。
        lenient().when(dailyReportMapper.selectReportOwnersForUpdate(any()))
                .thenReturn(Collections.singletonList(ownerRow(reportId, USER_ID)));
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

    // ---------- Issue #13：删除日报的归属校验（只能删自己的） ----------

    @Test
    @DisplayName("[TDD] 删除日报：他人的日报必须被拒绝，且在抛错前不得读写任何数据")
    void deleteDailyReport_othersReport_isRejected() {
        Long othersReportId = 9001L;

        // 该日报存在，但 user_id 不是当前登录人
        when(dailyReportMapper.selectReportOwnersForUpdate(any()))
                .thenReturn(Collections.singletonList(ownerRow(othersReportId, 999L)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.deleteDailyReportByIds(new Long[]{othersReportId}),
                "持 project:dailyReport:remove 的账号有 180+ 个，report_id 连续自增且日报是硬删除——"
                        + "不校验归属等于开放「删除任意人日报」（Issue #13）");
        assertTrue(ex.getMessage().contains("本人"),
                "错误提示要让填报人看得懂是「只能删自己的」，实际为：" + ex.getMessage());

        // 拒绝必须发生在任何读写之前：不得读取他人明细，也不得对他人的项目发起工时重算
        verify(detailMapper, never()).selectByReportId(anyLong());
        verify(detailMapper, never()).countByReportId(anyLong());
        verify(projectMapper, never()).updateActualWorkload(anyLong(), any());
        verify(taskMapper, never()).updateActualWorkload(anyLong(), any());
    }

    @Test
    @DisplayName("[TDD] 删除日报：混合传入自己的与他人的日报，整批拒绝、不得部分执行")
    void deleteDailyReport_mixedBatch_rejectsEntirely() {
        Long myReportId = 70L;
        Long othersReportId = 9002L;

        // Spring 会把 @PathVariable Long[] 按逗号拆开，攻击可以混在一次合法自删里
        when(dailyReportMapper.selectReportOwnersForUpdate(any()))
                .thenReturn(Collections.singletonList(ownerRow(othersReportId, 999L)));

        assertThrows(ServiceException.class,
                () -> service.deleteDailyReportByIds(new Long[]{myReportId, othersReportId}),
                "DELETE /project/dailyReport/70,9002 这类混合批次必须整批拒绝");

        // 整批拒绝 = 一条明细都不许动（本方法带 @Transactional，但拒绝应在写入前发生）
        verify(detailMapper, never()).selectByReportId(anyLong());
        verify(detailMapper, never()).countByReportId(anyLong());
        verify(projectMapper, never()).updateActualWorkload(anyLong(), any());
        verify(taskMapper, never()).updateActualWorkload(anyLong(), any());
    }

    @Test
    @DisplayName("[TDD] 删除日报：删自己的日报行为不变——归属校验通过后 015 的不可见工时保护照旧成立")
    void deleteDailyReport_ownReport_behaviourUnchanged() {
        Long reportId = 74L;
        Long visibleProjectId = 10L;
        Long invisibleProjectId = 20L;

        // 归属校验通过：该主记录存在且归属本人（emptyList 现在表示「主记录不存在」）
        lenient().when(dailyReportMapper.selectReportOwnersForUpdate(any()))
                .thenReturn(Collections.singletonList(ownerRow(reportId, USER_ID)));

        when(detailMapper.selectByReportId(reportId)).thenReturn(Arrays.asList(
                buildDetail("work", new BigDecimal("4"), null, visibleProjectId, null),
                buildDetail("work", new BigDecimal("2"), null, invisibleProjectId, null)));

        Map<String, Object> visible = new HashMap<>();
        visible.put("projectId", visibleProjectId);
        lenient().when(projectMapper.selectProjectsByUserId(USER_ID))
                .thenReturn(Collections.singletonList(visible));
        lenient().when(detailMapper.countByReportId(reportId)).thenReturn(1);
        lenient().when(detailMapper.sumWorkHoursByReportId(reportId)).thenReturn(new BigDecimal("2"));

        int rows = service.deleteDailyReportByIds(new Long[]{reportId});

        // 015 的四条不变式在加了归属校验之后必须逐条照旧成立
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Collection> scope = ArgumentCaptor.forClass(Collection.class);
        verify(detailMapper).deleteByReportIdInScope(eq(reportId), scope.capture(), eq(USER_ID));
        assertTrue(scope.getValue().contains(visibleProjectId), "看得见的工时该删（FR-013）");
        assertFalse(scope.getValue().contains(invisibleProjectId), "看不见的工时不许连带删（FR-001/FR-013）");
        // any() 而非 anyLong()：anyLong() 不匹配 null，会放过 userId=null 的调用（见另一条同名护栏的说明）
        verify(dailyReportMapper, never()).deleteDailyReportByIds(any(), any());
        verify(dailyReportMapper).updateTotalWorkHours(eq(reportId), eq(new BigDecimal("2")), eq(USER_ID));
        assertTrue(rows > 0, "返回值仍是「本次处理条数」，不能退化成「删了几条主记录」（FR-014）");
    }

    @Test
    @DisplayName("[护栏] 删除日报：不存在的 reportId 按幂等 no-op 放行，不得当成越权而报错")
    void deleteDailyReport_unknownReportId_isIdempotentNoOp() {
        Long goneReportId = 9999L;

        // 归属查询的语义：只返回「确实存在、且属于他人」的 ID。
        // 已被删掉的 ID 查不出来 → 返回空集 → 必须放行。
        // 这条不变式完全落在 selectReportIdsNotOwnedBy 的 SQL 上（"where user_id <> ? and report_id in (...)"）：
        // 若有人把它「加固」成 not exists / left join 之类让「查不到」也算「非本人」，
        // 那么每一次重复点击删除、每一个过期页面都会变成 500「只能删除本人的日报」。
        lenient().when(dailyReportMapper.selectReportOwnersForUpdate(any()))
                .thenReturn(Collections.emptyList());
        lenient().when(detailMapper.selectByReportId(goneReportId)).thenReturn(Collections.emptyList());
        lenient().when(detailMapper.countByReportId(goneReportId)).thenReturn(0);

        int rows = assertDoesNotThrow(() -> service.deleteDailyReportByIds(new Long[]{goneReportId}),
                "过期页面/重复点击会重发已删掉的 reportId，对它报错只会让填报人以为删除失败");
        assertTrue(rows > 0, "幂等 no-op 也要回成功，否则 toAjax 判为「操作失败」");

        // 即便是 no-op，落到 SQL 的仍必须带本人 userId——否则一旦该 ID 后来被别人复用就是越权
        verify(dailyReportMapper).deleteDailyReportByIds(any(), eq(USER_ID));
    }

    @Test
    @DisplayName("[护栏] 删除日报：URL 拆出的 null 元素不得被当成一条日报，全 null 时须返回 0（不能报「成功」）")
    void deleteDailyReport_nullIds_areNotCountedAsSuccess() {
        // Spring 把 @PathVariable Long[] 按逗号拆开：路径段为 "," 时得到 Long[]{null, null}（length=2）。
        // 若直接用 reportIds.length 计数，会返回 2 → toAjax 报「操作成功」，而 SQL 一行都没动
        // （report_id in (NULL,NULL) 永不匹配）。
        int rows = service.deleteDailyReportByIds(new Long[]{null, null});

        assertEquals(0, rows, "一条有效 ID 都没有时必须回 0，让 toAjax 判为失败——不能谎报删除成功");
        // null 元素也不该白跑查询（原实现会为每个 null 各跑 3 条 SQL）
        verify(dailyReportMapper, never()).selectReportOwnersForUpdate(any());
        verify(detailMapper, never()).selectByReportId(any());
        verify(detailMapper, never()).deleteByReportIdInScope(any(), any(), any());
        verify(detailMapper, never()).countByReportId(any());
        verify(dailyReportMapper, never()).deleteDailyReportByIds(any(), any());
        verify(dailyReportMapper, never()).updateTotalWorkHours(any(), any(), any());
    }

    @Test
    @DisplayName("[TDD] 删除日报：主记录已不存在但明细还在（孤儿数据），不得进入读写段")
    void deleteDailyReport_orphanDetails_areNotProcessed() {
        Long orphanReportId = 75L;
        Long foreignProjectId = 88L;

        // 主记录查不到 —— 孤儿明细的典型形态
        when(dailyReportMapper.selectReportOwnersForUpdate(any())).thenReturn(Collections.emptyList());
        // 但明细还在，且挂在调用者无数据权限的项目上
        lenient().when(detailMapper.selectByReportId(orphanReportId)).thenReturn(
                Collections.singletonList(
                        buildDetail("work", new BigDecimal("8"), null, foreignProjectId, null)));

        service.deleteDailyReportByIds(new Long[]{orphanReportId});

        // 归属探针查不到主记录，就无从判断这条数据归谁——必须整条跳过，
        // 而不是「查不到就当属于我」继续往下走：那会读到他人明细，
        // 并对调用者无数据权限的项目发起 pm_project.actual_workload 重算（取排他锁）。
        verify(detailMapper, never()).selectByReportId(anyLong());
        verify(detailMapper, never()).deleteByReportIdInScope(any(), any(), any());
        verify(projectMapper, never()).updateActualWorkload(anyLong(), any());
        verify(taskMapper, never()).updateActualWorkload(anyLong(), any());
    }

    @Test
    @DisplayName("[护栏] 删除日报：重复 ID 只处理一次，返回值与实际处理条数保持恒等")
    void deleteDailyReport_duplicateIds_areProcessedOnce() {
        Long reportId = 70L;
        Long invisibleProjectId = 20L;

        lenient().when(dailyReportMapper.selectReportOwnersForUpdate(any()))
                .thenReturn(Collections.singletonList(ownerRow(reportId, USER_ID)));
        when(detailMapper.selectByReportId(reportId)).thenReturn(Collections.singletonList(
                buildDetail("work", new BigDecimal("2"), null, invisibleProjectId, null)));
        lenient().when(projectMapper.selectProjectsByUserId(USER_ID)).thenReturn(Collections.emptyList());
        lenient().when(detailMapper.countByReportId(reportId)).thenReturn(1);
        lenient().when(detailMapper.sumWorkHoursByReportId(reportId)).thenReturn(new BigDecimal("2"));

        // "DELETE /project/dailyReport/70,70,70"
        int rows = service.deleteDailyReportByIds(new Long[]{reportId, reportId, reportId});

        assertEquals(1, rows, "返回值是「处理了几条日报」，重复 ID 不该把它放大成 3");
        verify(detailMapper, times(1)).deleteByReportIdInScope(eq(reportId), any(), eq(USER_ID));
        verify(dailyReportMapper, times(1))
                .updateTotalWorkHours(eq(reportId), eq(new BigDecimal("2")), eq(USER_ID));
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
        verify(detailMapper, never()).deleteByReportIdInScope(any(), any(), any());
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
        verify(detailMapper).deleteByReportIdInScope(eq(existingReportId), scope.capture(), eq(USER_ID));
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

    /**
     * 【Issue #13】构造一行 selectReportOwnersForUpdate 的返回值（reportId → 归属人）。
     *
     * <p>注意语义：该查询<b>查不到主记录就不返回该行</b>。所以 emptyList() 表示
     * 「这些 reportId 的主记录都不存在」（走幂等 no-op），而不是「没有他人的日报」。
     * 想表达「这条属于我、可以正常删」必须显式返回一行 ownerRow(reportId, USER_ID)。
     */
    private Map<String, Object> ownerRow(Long reportId, Long userId) {
        Map<String, Object> row = new HashMap<>();
        row.put("reportId", reportId);
        row.put("userId", userId);
        return row;
    }

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
}
