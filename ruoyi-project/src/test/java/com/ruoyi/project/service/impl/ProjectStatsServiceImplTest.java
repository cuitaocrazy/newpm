package com.ruoyi.project.service.impl;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.WorkloadCorrectLog;
import com.ruoyi.project.domain.vo.ProjectStatsVO;
import com.ruoyi.project.mapper.ProjectStatsMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ProjectStatsServiceImpl 行为锁定测试
 */
@ExtendWith(MockitoExtension.class)
class ProjectStatsServiceImplTest {

    @InjectMocks
    private ProjectStatsServiceImpl service;

    @Mock private ProjectStatsMapper projectStatsMapper;

    private MockedStatic<SecurityUtils> securityMock;

    @BeforeEach
    void setUp() {
        securityMock = mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::getUsername).thenReturn("admin");
    }

    @AfterEach
    void tearDown() {
        securityMock.close();
    }

    // ========== selectProjectStatsList 行为锁定 ==========

    @Test
    @DisplayName("人天统计：按projectId聚合，保持顺序")
    void selectProjectStatsList_aggregation() {
        List<Map<String, Object>> rows = List.of(
            makeRow(1L, "项目A", "张三", "10.0", "0.5", "1", "3.000"),
            makeRow(1L, "项目A", "张三", "10.0", "0.5", "2", "2.000"),
            makeRow(2L, "项目B", "李四", "20.0", null,  "1", "5.000")
        );
        when(projectStatsMapper.selectProjectStatsByStageWithPage(any(), eq(0L), eq(10))).thenReturn(rows);

        List<ProjectStatsVO> result = service.selectProjectStatsList(new Project(), 1, 10);

        assertEquals(2, result.size());
        // 项目A：2个阶段
        ProjectStatsVO voA = result.get(0);
        assertEquals(1L, voA.getProjectId());
        assertEquals(2, voA.getStages().size());
        assertEquals(0, new BigDecimal("5.000").compareTo(voA.getTotalActualDays())); // 3+2
        assertEquals(0, new BigDecimal("5.500").compareTo(voA.getActualDays()));      // 5+0.5

        // 项目B：1个阶段，adjustWorkload=null → 按0处理
        ProjectStatsVO voB = result.get(1);
        assertEquals(0, new BigDecimal("5.000").compareTo(voB.getTotalActualDays()));
        assertEquals(0, new BigDecimal("5.000").compareTo(voB.getActualDays()));
    }

    @Test
    @DisplayName("人天统计：无阶段数据时stages为空列表")
    void selectProjectStatsList_noStage() {
        List<Map<String, Object>> rows = List.of(
            makeRow(1L, "项目A", "张三", "10.0", null, null, null)
        );
        when(projectStatsMapper.selectProjectStatsByStageWithPage(any(), eq(0L), eq(10))).thenReturn(rows);

        List<ProjectStatsVO> result = service.selectProjectStatsList(new Project(), 1, 10);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getStages().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).getTotalActualDays()));
    }

    @Test
    @DisplayName("人天统计：空结果")
    void selectProjectStatsList_empty() {
        when(projectStatsMapper.selectProjectStatsByStageWithPage(any(), eq(0L), eq(10))).thenReturn(Collections.emptyList());

        List<ProjectStatsVO> result = service.selectProjectStatsList(new Project(), 1, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("人天统计：分页偏移计算正确")
    void selectProjectStatsList_pagination() {
        when(projectStatsMapper.selectProjectStatsByStageWithPage(any(), eq(20L), eq(10))).thenReturn(Collections.emptyList());

        service.selectProjectStatsList(new Project(), 3, 10); // page3 → offset=20

        verify(projectStatsMapper).selectProjectStatsByStageWithPage(any(), eq(20L), eq(10));
    }

    // ========== correctAdjustWorkload 行为锁定 ==========

    @Test
    @DisplayName("工时补正（加）：服务端计算 afterAdjust = before + delta")
    void correctAdjustWorkload_add() {
        when(projectStatsMapper.selectCurrentAdjustWorkload(1L)).thenReturn(new BigDecimal("1.0"));

        service.correctAdjustWorkload(1L, 1, new BigDecimal("0.5"), new BigDecimal("999"), "补正原因");

        // 忽略客户端传的999，服务端算出 1.0 + 0.5 = 1.5
        verify(projectStatsMapper).updateAdjustWorkload(1L, new BigDecimal("1.5"));

        ArgumentCaptor<WorkloadCorrectLog> captor = ArgumentCaptor.forClass(WorkloadCorrectLog.class);
        verify(projectStatsMapper).insertCorrectLog(captor.capture());
        WorkloadCorrectLog log = captor.getValue();
        assertEquals(1L, log.getProjectId());
        assertEquals(1, log.getDirection());
        assertEquals(0, new BigDecimal("0.5").compareTo(log.getDelta()));
        assertEquals(0, new BigDecimal("1.0").compareTo(log.getBeforeAdjust()));
        assertEquals(0, new BigDecimal("1.5").compareTo(log.getAfterAdjust()));
        assertEquals("补正原因", log.getReason());
        assertEquals("admin", log.getCreateBy());
    }

    @Test
    @DisplayName("工时补正（减）：服务端计算 afterAdjust = before - delta")
    void correctAdjustWorkload_subtract() {
        when(projectStatsMapper.selectCurrentAdjustWorkload(1L)).thenReturn(new BigDecimal("2.0"));

        service.correctAdjustWorkload(1L, 0, new BigDecimal("0.5"), null, "扣减");

        verify(projectStatsMapper).updateAdjustWorkload(1L, new BigDecimal("1.5"));
    }

    @Test
    @DisplayName("工时补正：当前adjustWorkload为null时按0处理")
    void correctAdjustWorkload_nullBefore() {
        when(projectStatsMapper.selectCurrentAdjustWorkload(1L)).thenReturn(null);

        service.correctAdjustWorkload(1L, 1, new BigDecimal("1.0"), null, "首次补正");

        verify(projectStatsMapper).updateAdjustWorkload(1L, new BigDecimal("1.0"));

        ArgumentCaptor<WorkloadCorrectLog> captor = ArgumentCaptor.forClass(WorkloadCorrectLog.class);
        verify(projectStatsMapper).insertCorrectLog(captor.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getValue().getBeforeAdjust()));
        assertEquals(0, new BigDecimal("1.0").compareTo(captor.getValue().getAfterAdjust()));
    }

    @Test
    @DisplayName("工时补正：恶意客户端传值被忽略，服务端自己算")
    void correctAdjustWorkload_ignoresClientValue() {
        when(projectStatsMapper.selectCurrentAdjustWorkload(1L)).thenReturn(new BigDecimal("1.0"));

        // 客户端传 afterAdjust=999，服务端忽略，自己算 1.0 + 0.5 = 1.5
        service.correctAdjustWorkload(1L, 1, new BigDecimal("0.5"), new BigDecimal("999"), "恶意值");

        verify(projectStatsMapper).updateAdjustWorkload(1L, new BigDecimal("1.5"));
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> makeRow(Long projectId, String name, String manager,
                                         String estimated, String adjust,
                                         String stage, String stageDays) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("projectId", projectId);
        row.put("projectName", name);
        row.put("projectManagerName", manager);
        row.put("estimatedWorkload", estimated != null ? new BigDecimal(estimated) : null);
        row.put("adjustWorkload", adjust != null ? new BigDecimal(adjust) : null);
        row.put("projectStage", stage);
        row.put("stageDays", stageDays != null ? new BigDecimal(stageDays) : null);
        return row;
    }
}
