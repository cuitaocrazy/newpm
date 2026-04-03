package com.ruoyi.project.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.ProjectApproval;
import com.ruoyi.project.mapper.ProjectApprovalMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ProjectApprovalServiceImpl 行为锁定测试
 */
@ExtendWith(MockitoExtension.class)
class ProjectApprovalServiceImplTest {

    @InjectMocks
    private ProjectApprovalServiceImpl service;

    @Mock private ProjectApprovalMapper projectApprovalMapper;
    @Mock private ProjectMapper projectMapper;

    private MockedStatic<SecurityUtils> securityMock;

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

    // ========== approveProject 行为锁定 ==========

    @Test
    @DisplayName("审批通过：待审核(0)状态 → 审批通过")
    void approveProject_approve() {
        Project project = new Project();
        project.setApprovalStatus("0");
        when(projectMapper.selectProjectByProjectId(100L)).thenReturn(project);
        when(projectApprovalMapper.insertProjectApproval(any())).thenReturn(1);

        int result = service.approveProject(100L, "1", "同意立项");

        assertEquals(1, result);
        verify(projectMapper).updateProjectApprovalFields(eq(100L), eq("1"), eq("同意立项"), any(), eq("1"));

        ArgumentCaptor<ProjectApproval> captor = ArgumentCaptor.forClass(ProjectApproval.class);
        verify(projectApprovalMapper).insertProjectApproval(captor.capture());
        ProjectApproval approval = captor.getValue();
        assertEquals(100L, approval.getProjectId());
        assertEquals("1", approval.getApprovalStatus());
        assertEquals("同意立项", approval.getApprovalReason());
        assertEquals(1L, approval.getApproverId());
        assertEquals("approver", approval.getCreateBy());
    }

    @Test
    @DisplayName("审批拒绝：退回待审核(3)状态 → 可以审批拒绝")
    void approveProject_rejectFromRollback() {
        Project project = new Project();
        project.setApprovalStatus("3");
        when(projectMapper.selectProjectByProjectId(100L)).thenReturn(project);
        when(projectApprovalMapper.insertProjectApproval(any())).thenReturn(1);

        service.approveProject(100L, "2", "材料不全");

        verify(projectMapper).updateProjectApprovalFields(eq(100L), eq("2"), eq("材料不全"), any(), eq("1"));
    }

    @Test
    @DisplayName("审批：已通过(1)状态不允许再次审批")
    void approveProject_blockedWhenAlreadyApproved() {
        Project project = new Project();
        project.setApprovalStatus("1");
        when(projectMapper.selectProjectByProjectId(100L)).thenReturn(project);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.approveProject(100L, "1", null));
        assertTrue(ex.getMessage().contains("不允许"));
    }

    @Test
    @DisplayName("审批：已拒绝(2)状态不允许审批")
    void approveProject_blockedWhenRejected() {
        Project project = new Project();
        project.setApprovalStatus("2");
        when(projectMapper.selectProjectByProjectId(100L)).thenReturn(project);

        assertThrows(ServiceException.class,
            () -> service.approveProject(100L, "1", null));
    }

    @Test
    @DisplayName("审批：项目不存在时抛异常")
    void approveProject_notFound() {
        when(projectMapper.selectProjectByProjectId(99L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.approveProject(99L, "1", null));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    // ========== rollbackProject 行为锁定 ==========

    @Test
    @DisplayName("退回：审核通过(1)的项目退回为退回待审核(3)")
    void rollbackProject_success() {
        Project project = new Project();
        project.setApprovalStatus("1");
        when(projectMapper.selectProjectByProjectId(100L)).thenReturn(project);
        when(projectApprovalMapper.insertProjectApproval(any())).thenReturn(1);

        int result = service.rollbackProject(100L, "需要补充材料");

        assertEquals(1, result);
        verify(projectMapper).updateProjectApprovalFields(eq(100L), eq("3"), eq("需要补充材料"), any(), eq("1"));

        ArgumentCaptor<ProjectApproval> captor = ArgumentCaptor.forClass(ProjectApproval.class);
        verify(projectApprovalMapper).insertProjectApproval(captor.capture());
        assertEquals("3", captor.getValue().getApprovalStatus());
    }

    @Test
    @DisplayName("退回：非审核通过状态时抛异常")
    void rollbackProject_wrongStatus() {
        Project project = new Project();
        project.setApprovalStatus("0"); // 待审核
        when(projectMapper.selectProjectByProjectId(100L)).thenReturn(project);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.rollbackProject(100L, "退回"));
        assertTrue(ex.getMessage().contains("审核通过"));
    }

    @Test
    @DisplayName("退回：项目不存在时抛异常")
    void rollbackProject_notFound() {
        when(projectMapper.selectProjectByProjectId(99L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.rollbackProject(99L, "退回"));
        assertTrue(ex.getMessage().contains("不存在"));
    }
}
