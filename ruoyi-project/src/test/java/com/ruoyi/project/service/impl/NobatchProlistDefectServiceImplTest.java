package com.ruoyi.project.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.NobatchProlistDefect;
import com.ruoyi.project.mapper.NobatchProlistDefectMapper;

/**
 * 非批次任务问题单及缺陷 Service 特征测试。
 * 覆盖：派生算法各分支、问题单编号查重(新增/编辑排除自己)、CRUD、联动转发。
 */
@ExtendWith(MockitoExtension.class)
public class NobatchProlistDefectServiceImplTest
{
    @Mock
    private NobatchProlistDefectMapper mapper;

    @InjectMocks
    private NobatchProlistDefectServiceImpl service;

    private Date date(String yyyyMMdd)
    {
        return Date.from(LocalDate.parse(yyyyMMdd).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Test
    public void selectList_delegates()
    {
        NobatchProlistDefect q = new NobatchProlistDefect();
        List<NobatchProlistDefect> expected = Arrays.asList(new NobatchProlistDefect());
        when(mapper.selectNobatchProlistDefectList(q)).thenReturn(expected);
        assertSame(expected, service.selectNobatchProlistDefectList(q));
    }

    @Test
    public void selectById_delegates()
    {
        NobatchProlistDefect e = new NobatchProlistDefect();
        when(mapper.selectNobatchProlistDefectByProblemId(3L)).thenReturn(e);
        assertSame(e, service.selectNobatchProlistDefectByProblemId(3L));
    }

    @Test
    public void delete_delegates()
    {
        Long[] ids = { 1L };
        when(mapper.deleteNobatchProlistDefectByProblemIds(ids)).thenReturn(1);
        assertEquals(1, service.deleteNobatchProlistDefectByProblemIds(ids));
    }

    @Test
    public void linkage_delegates()
    {
        when(mapper.selectPlanProductionDate(1L)).thenReturn("2026-01-18");
        assertEquals("2026-01-18", service.selectPlanProductionDate(1L));
        List<java.util.Map<String, Object>> opts = Arrays.asList();
        when(mapper.selectBatchByYear("2026")).thenReturn(opts);
        assertSame(opts, service.selectBatchByYear("2026"));
    }

    @Test
    public void checkProblemNoUnique_empty_false()
    {
        assertFalse(service.checkProblemNoUnique("", null));
        assertFalse(service.checkProblemNoUnique(null, null));
        verify(mapper, never()).checkProblemNoUnique(any(), any());
    }

    @Test
    public void checkProblemNoUnique_zero_true()
    {
        when(mapper.checkProblemNoUnique("NB-1", null)).thenReturn(0);
        assertTrue(service.checkProblemNoUnique("NB-1", null));
    }

    @Test
    public void checkProblemNoUnique_positive_false()
    {
        when(mapper.checkProblemNoUnique("NB-1", null)).thenReturn(1);
        assertFalse(service.checkProblemNoUnique("NB-1", null));
    }

    @Test
    public void checkProblemNoUnique_editExcludesSelf()
    {
        when(mapper.checkProblemNoUnique("NB-1", 5L)).thenReturn(0);
        assertTrue(service.checkProblemNoUnique("NB-1", 5L));
        verify(mapper).checkProblemNoUnique("NB-1", 5L);
    }

    @Test
    public void insert_overdue_settleMinusSubmitGtOneDay()
    {
        NobatchProlistDefect d = new NobatchProlistDefect();
        d.setProblemNo("NB-A");
        d.setSubmitDate(date("2026-06-01"));
        d.setSettleDate(date("2026-06-05"));
        when(mapper.checkProblemNoUnique("NB-A", null)).thenReturn(0);
        when(mapper.insertNobatchProlistDefect(d)).thenReturn(1);
        try (MockedStatic<SecurityUtils> m = mockStatic(SecurityUtils.class)) {
            m.when(SecurityUtils::getUsername).thenReturn("admin");
            service.insertNobatchProlistDefect(d);
        }
        assertEquals("1", d.getSolutionTimeOverOneDay());
        assertEquals("admin", d.getCreateBy());
    }

    @Test
    public void insert_notOverdue_withinOneDay()
    {
        NobatchProlistDefect d = new NobatchProlistDefect();
        d.setProblemNo("NB-B");
        d.setSubmitDate(date("2026-06-01"));
        d.setSettleDate(date("2026-06-02"));
        when(mapper.checkProblemNoUnique("NB-B", null)).thenReturn(0);
        when(mapper.insertNobatchProlistDefect(d)).thenReturn(1);
        try (MockedStatic<SecurityUtils> m = mockStatic(SecurityUtils.class)) {
            m.when(SecurityUtils::getUsername).thenReturn("admin");
            service.insertNobatchProlistDefect(d);
        }
        assertEquals("0", d.getSolutionTimeOverOneDay());
    }

    @Test
    public void insert_settleNull_usesToday()
    {
        NobatchProlistDefect d = new NobatchProlistDefect();
        d.setProblemNo("NB-C");
        d.setSubmitDate(date("2026-06-01"));
        d.setSettleDate(null);
        when(mapper.checkProblemNoUnique("NB-C", null)).thenReturn(0);
        when(mapper.insertNobatchProlistDefect(d)).thenReturn(1);
        try (MockedStatic<SecurityUtils> m = mockStatic(SecurityUtils.class)) {
            m.when(SecurityUtils::getUsername).thenReturn("admin");
            service.insertNobatchProlistDefect(d);
        }
        assertEquals("1", d.getSolutionTimeOverOneDay());
    }

    @Test
    public void insert_submitNull_notOverdue()
    {
        NobatchProlistDefect d = new NobatchProlistDefect();
        d.setProblemNo("NB-D");
        d.setSubmitDate(null);
        when(mapper.checkProblemNoUnique("NB-D", null)).thenReturn(0);
        when(mapper.insertNobatchProlistDefect(d)).thenReturn(1);
        try (MockedStatic<SecurityUtils> m = mockStatic(SecurityUtils.class)) {
            m.when(SecurityUtils::getUsername).thenReturn("admin");
            service.insertNobatchProlistDefect(d);
        }
        assertEquals("0", d.getSolutionTimeOverOneDay());
    }

    @Test
    public void insert_duplicate_throws()
    {
        NobatchProlistDefect d = new NobatchProlistDefect();
        d.setProblemNo("NB-DUP");
        when(mapper.checkProblemNoUnique("NB-DUP", null)).thenReturn(1);
        assertThrows(ServiceException.class, () -> service.insertNobatchProlistDefect(d));
        verify(mapper, never()).insertNobatchProlistDefect(any());
    }

    @Test
    public void update_excludesSelf_recomputesDerived()
    {
        NobatchProlistDefect d = new NobatchProlistDefect();
        d.setProblemId(5L);
        d.setProblemNo("NB-E");
        d.setSubmitDate(date("2026-06-01"));
        d.setSettleDate(date("2026-06-10"));
        when(mapper.checkProblemNoUnique("NB-E", 5L)).thenReturn(0);
        when(mapper.updateNobatchProlistDefect(d)).thenReturn(1);
        try (MockedStatic<SecurityUtils> m = mockStatic(SecurityUtils.class)) {
            m.when(SecurityUtils::getUsername).thenReturn("editor");
            service.updateNobatchProlistDefect(d);
        }
        verify(mapper).checkProblemNoUnique("NB-E", 5L);
        assertEquals("1", d.getSolutionTimeOverOneDay());
        assertEquals("editor", d.getUpdateBy());
    }

    @Test
    public void update_duplicate_throws()
    {
        NobatchProlistDefect d = new NobatchProlistDefect();
        d.setProblemId(5L);
        d.setProblemNo("NB-DUP");
        when(mapper.checkProblemNoUnique("NB-DUP", 5L)).thenReturn(1);
        assertThrows(ServiceException.class, () -> service.updateNobatchProlistDefect(d));
        verify(mapper, never()).updateNobatchProlistDefect(any());
    }
}
