package com.ruoyi.project.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.ProjectApproval;
import com.ruoyi.project.mapper.ProjectApprovalMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.service.IProjectEmailService;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * ProjectReviewServiceImpl 行为锁定测试
 *
 * 覆盖两处此前「有测试文件但那条真实路径从未被执行过」的代码：
 *
 * 1. Issue #10：approveProject / rollbackProject 必须走专用语句 updateProjectApprovalFields，
 *    不能拿只填了审核字段的裸 Project 去调 ProjectMapper.updateProject —— 后者已解放
 *    start_date / end_date / production_date / acceptance_date / apply_date 五个日期的
 *    &lt;if&gt; 守卫，会在每次审核时把这 5 个日期一并写成 NULL。
 *    本类用 verify(projectMapper, never()).updateProject(any()) 把这条锁死。
 *
 * 2. 审核通过时未填意见 → 必须回读并保留 pm_project.approval_reason 的原有意见，
 *    而不是把 null 无条件写进去（updateProjectApprovalFields 是无条件写入）。
 *    本类用 ArgumentCaptor 捕获真正传给 mapper 的 reason 实参来断言。
 *
 * 注意与 ProjectApprovalServiceImpl 的三处语义差异（别抄错方向）：
 *   - approveProject 这里**没有**任何前置状态校验（那边要求当前状态 0 或 3）
 *   - rollbackProject 这里写的目标状态是 "0"（待审核），那边写的是 "3"（退回待审核）
 *   - 主表写 reasonToWrite（可能是回读的旧意见），历史表写原始 approvalReason（可能是 null）
 *
 * 纯单测：不连 MySQL / Redis。
 * 单测只能锁住「调了哪个 mapper 方法、实参是什么」，锁不住「5 个日期没被写 NULL」——
 * 后者由 tests/e2e-approval-workflow.spec.js 的「审核通过不清空项目日期」块打真库证明，两者不可互替。
 */
@ExtendWith(MockitoExtension.class)
class ProjectReviewServiceImplTest {

    @InjectMocks
    private ProjectReviewServiceImpl service;

    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectApprovalMapper projectApprovalMapper;
    @Mock private IProjectEmailService projectEmailService;
    @Mock private SysDeptMapper sysDeptMapper;
    @Mock private SysUserMapper sysUserMapper;

    private MockedStatic<SecurityUtils> securityMock;

    private static final Long PROJECT_ID = 100L;

    @BeforeEach
    void setUp() {
        securityMock = mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::getUserId).thenReturn(1L);
        securityMock.when(SecurityUtils::getUsername).thenReturn("approver");
    }

    @AfterEach
    void tearDown() {
        securityMock.close();
    }

    // ========== approveProject：审核意见的写入口径 ==========

    @Test
    @DisplayName("审核通过·未填意见：写回的是原有意见而不是 null（回归：意见凭空消失）")
    void approveProject_approveWithoutReason_keepsExistingReason() {
        Project current = new Project();
        current.setApprovalStatus("0");
        current.setApprovalReason("上一轮退回意见：预算测算缺依据");
        when(projectMapper.selectProjectByProjectId(PROJECT_ID)).thenReturn(current);
        when(projectMapper.updateProjectApprovalFields(anyLong(), any(), any(), any(), any())).thenReturn(1);

        int result = service.approveProject(PROJECT_ID, "1", null);

        assertEquals(1, result);

        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(projectMapper).updateProjectApprovalFields(
                eq(PROJECT_ID), eq("1"), reasonCaptor.capture(), any(), eq("1"));

        assertNotNull(reasonCaptor.getValue(),
                "未填意见时传给 updateProjectApprovalFields 的 reason 不得为 null —— "
                        + "该语句无条件写入，传 null 会把主表上一次的审核意见清空");
        assertEquals("上一轮退回意见：预算测算缺依据", reasonCaptor.getValue(),
                "未填意见时必须回读 pm_project.approval_reason 的原值写回");

        // 必须走专用语句，绝不能是裸 Project 调 updateProject（会清空 5 个日期）
        verify(projectMapper, never()).updateProject(any());
    }

    @Test
    @DisplayName("审核通过·意见为纯空白：同样按未填处理，保留原有意见")
    void approveProject_approveWithBlankReason_keepsExistingReason() {
        Project current = new Project();
        current.setApprovalReason("原有意见-空白分支");
        when(projectMapper.selectProjectByProjectId(PROJECT_ID)).thenReturn(current);
        when(projectMapper.updateProjectApprovalFields(anyLong(), any(), any(), any(), any())).thenReturn(1);

        service.approveProject(PROJECT_ID, "1", "   ");

        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(projectMapper).updateProjectApprovalFields(
                eq(PROJECT_ID), eq("1"), reasonCaptor.capture(), any(), eq("1"));
        assertEquals("原有意见-空白分支", reasonCaptor.getValue(),
                "纯空白意见应等同于未填，回读原值写回");
        verify(projectMapper, never()).updateProject(any());
    }

    @Test
    @DisplayName("审核通过·填了意见：写入新意见，且不回读原值")
    void approveProject_approveWithReason_writesNewReason() {
        when(projectMapper.updateProjectApprovalFields(anyLong(), any(), any(), any(), any())).thenReturn(1);

        int result = service.approveProject(PROJECT_ID, "1", "同意立项");

        assertEquals(1, result);

        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(projectMapper).updateProjectApprovalFields(
                eq(PROJECT_ID), eq("1"), reasonCaptor.capture(), any(), eq("1"));
        assertEquals("同意立项", reasonCaptor.getValue());

        // 传了意见就不该多打一次回读查询
        verify(projectMapper, never()).selectProjectByProjectId(anyLong());
        verify(projectMapper, never()).updateProject(any());
    }

    @Test
    @DisplayName("审核拒绝·填了意见：状态为 2 且写入新意见")
    void approveProject_rejectWithReason_writesNewReasonAndRejectStatus() {
        when(projectMapper.updateProjectApprovalFields(anyLong(), any(), any(), any(), any())).thenReturn(1);

        int result = service.approveProject(PROJECT_ID, "2", "材料不全，请补充");

        assertEquals(1, result);

        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(projectMapper).updateProjectApprovalFields(
                eq(PROJECT_ID), statusCaptor.capture(), reasonCaptor.capture(), any(), eq("1"));
        assertEquals("2", statusCaptor.getValue(), "拒绝时审核状态应为 2");
        assertEquals("材料不全，请补充", reasonCaptor.getValue());

        verify(projectMapper, never()).selectProjectByProjectId(anyLong());
        verify(projectMapper, never()).updateProject(any());
    }

    @Test
    @DisplayName("审核通过·未填意见且项目不存在：不抛异常，reason 写 null（锁定「无前置校验」现状）")
    void approveProject_projectNotFound_stillCallsUpdateWithNullReason() {
        // 与 ProjectApprovalServiceImpl 不同：这里没有「项目不存在」前置校验，
        // 影响 0 行 → 不写历史 → Controller toAjax(0) 返回 error。
        // 这正是 e2e 里 fakeProjectId 那条用例「返回错误」的真实原因（它根本没走到主干）。
        when(projectMapper.selectProjectByProjectId(PROJECT_ID)).thenReturn(null);

        int result = service.approveProject(PROJECT_ID, "1", null);

        assertEquals(0, result, "影响 0 行时应原样返回 0");

        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(projectMapper).updateProjectApprovalFields(
                eq(PROJECT_ID), eq("1"), reasonCaptor.capture(), any(), eq("1"));
        assertNull(reasonCaptor.getValue());
        verify(projectMapper, never()).updateProject(any());
    }

    // ========== approveProject：审核历史与邮件通知 ==========

    @Test
    @DisplayName("审核通过·未填意见：历史表插入一条，approvalReason 记原始传入值(null)")
    void approveProject_approveWithoutReason_insertsHistory() {
        Project current = new Project();
        current.setApprovalReason("原有意见-历史分支");
        when(projectMapper.selectProjectByProjectId(PROJECT_ID)).thenReturn(current);
        when(projectMapper.updateProjectApprovalFields(anyLong(), any(), any(), any(), any())).thenReturn(1);

        service.approveProject(PROJECT_ID, "1", null);

        ArgumentCaptor<ProjectApproval> captor = ArgumentCaptor.forClass(ProjectApproval.class);
        verify(projectApprovalMapper).insertProjectApproval(captor.capture());
        ProjectApproval approval = captor.getValue();
        assertEquals(PROJECT_ID, approval.getProjectId());
        assertEquals("1", approval.getApprovalStatus());
        // 既有行为：历史表记的是原始传入的 approvalReason（null），主表记的是回读的旧意见，
        // 两边本就不一致，别断言成一致。
        assertNull(approval.getApprovalReason(),
                "历史表记录的是原始传入意见，未填时即为 null");
        assertEquals(1L, approval.getApproverId());
        assertEquals("approver", approval.getCreateBy());
        assertNotNull(approval.getApprovalTime());

        verify(projectEmailService).sendApprovalNotification(PROJECT_ID, "1", null);
    }

    @Test
    @DisplayName("审核通过·填了意见：历史表记录新意见")
    void approveProject_approveWithReason_insertsHistory() {
        when(projectMapper.updateProjectApprovalFields(anyLong(), any(), any(), any(), any())).thenReturn(1);

        service.approveProject(PROJECT_ID, "1", "同意立项");

        ArgumentCaptor<ProjectApproval> captor = ArgumentCaptor.forClass(ProjectApproval.class);
        verify(projectApprovalMapper).insertProjectApproval(captor.capture());
        assertEquals("1", captor.getValue().getApprovalStatus());
        assertEquals("同意立项", captor.getValue().getApprovalReason());

        verify(projectEmailService).sendApprovalNotification(PROJECT_ID, "1", "同意立项");
    }

    @Test
    @DisplayName("审核拒绝：历史表记录拒绝状态与意见")
    void approveProject_reject_insertsHistory() {
        when(projectMapper.updateProjectApprovalFields(anyLong(), any(), any(), any(), any())).thenReturn(1);

        service.approveProject(PROJECT_ID, "2", "材料不全，请补充");

        ArgumentCaptor<ProjectApproval> captor = ArgumentCaptor.forClass(ProjectApproval.class);
        verify(projectApprovalMapper).insertProjectApproval(captor.capture());
        assertEquals("2", captor.getValue().getApprovalStatus());
        assertEquals("材料不全，请补充", captor.getValue().getApprovalReason());

        verify(projectEmailService).sendApprovalNotification(PROJECT_ID, "2", "材料不全，请补充");
    }

    @Test
    @DisplayName("审核：影响 0 行时不写历史、不发邮件")
    void approveProject_noRowAffected_skipsHistoryAndEmail() {
        // updateProjectApprovalFields 未打桩，默认返回 0
        int result = service.approveProject(PROJECT_ID, "1", "同意立项");

        assertEquals(0, result);
        verify(projectMapper).updateProjectApprovalFields(eq(PROJECT_ID), eq("1"), eq("同意立项"), any(), eq("1"));
        verify(projectApprovalMapper, never()).insertProjectApproval(any());
        verify(projectEmailService, never()).sendApprovalNotification(any(), any(), any());
    }

    // ========== rollbackProject ==========

    @Test
    @DisplayName("退回：走 updateProjectApprovalFields 而非 updateProject，目标状态是 0（不是 3）")
    void rollbackProject_usesApprovalFieldsUpdate_notUpdateProject() {
        Project current = new Project();
        current.setApprovalStatus("1");
        when(projectMapper.selectProjectByProjectId(PROJECT_ID)).thenReturn(current);
        when(projectApprovalMapper.insertProjectApproval(any())).thenReturn(1);

        int result = service.rollbackProject(PROJECT_ID, "需要补充材料");

        assertEquals(1, result, "返回的是历史表插入行数");

        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(projectMapper).updateProjectApprovalFields(
                eq(PROJECT_ID), statusCaptor.capture(), reasonCaptor.capture(), any(), eq("1"));
        assertEquals("0", statusCaptor.getValue(),
                "ProjectReviewServiceImpl.rollbackProject 写的是 0（待审核），"
                        + "ProjectApprovalServiceImpl 写的才是 3（退回待审核），别搞混");
        assertEquals("需要补充材料", reasonCaptor.getValue());

        // 这条锁定 Issue #10 的改动本身：
        // 若改回 new Project() 后调 updateProject，5 个日期会被无条件写成 NULL。
        verify(projectMapper, never()).updateProject(any());

        ArgumentCaptor<ProjectApproval> captor = ArgumentCaptor.forClass(ProjectApproval.class);
        verify(projectApprovalMapper).insertProjectApproval(captor.capture());
        ProjectApproval approval = captor.getValue();
        assertEquals(PROJECT_ID, approval.getProjectId());
        assertEquals("0", approval.getApprovalStatus());
        assertEquals("需要补充材料", approval.getApprovalReason());
        assertEquals(1L, approval.getApproverId());
        assertEquals("approver", approval.getCreateBy());
    }

    @Test
    @DisplayName("退回：非审核通过状态时抛异常，且不落任何更新")
    void rollbackProject_wrongStatus() {
        Project current = new Project();
        current.setApprovalStatus("0");
        when(projectMapper.selectProjectByProjectId(PROJECT_ID)).thenReturn(current);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.rollbackProject(PROJECT_ID, "退回"));
        assertTrue(ex.getMessage().contains("审核通过"));

        verify(projectMapper, never()).updateProjectApprovalFields(any(), any(), any(), any(), any());
        verify(projectMapper, never()).updateProject(any());
        verify(projectApprovalMapper, never()).insertProjectApproval(any());
    }

    @Test
    @DisplayName("退回：项目不存在时抛异常")
    void rollbackProject_notFound() {
        when(projectMapper.selectProjectByProjectId(PROJECT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.rollbackProject(PROJECT_ID, "退回"));
        assertTrue(ex.getMessage().contains("不存在"));

        verify(projectMapper, never()).updateProjectApprovalFields(any(), any(), any(), any(), any());
        verify(projectMapper, never()).updateProject(any());
    }
}
