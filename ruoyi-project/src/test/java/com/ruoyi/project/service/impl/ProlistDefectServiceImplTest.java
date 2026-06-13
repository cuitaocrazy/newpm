package com.ruoyi.project.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.ProlistDefect;
import com.ruoyi.project.mapper.ProlistDefectMapper;

/**
 * 批次任务问题单及缺陷 Service 特征测试。
 * 覆盖：派生算法 solutionTimeOverOneDay 各分支、问题单编号查重(新增/编辑排除自己)、CRUD、联动转发。
 */
@ExtendWith(MockitoExtension.class)
public class ProlistDefectServiceImplTest
{
    @Mock
    private ProlistDefectMapper prolistDefectMapper;

    @InjectMocks
    private ProlistDefectServiceImpl service;

    private Date date(String yyyyMMdd)
    {
        LocalDate ld = LocalDate.parse(yyyyMMdd);
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // ========== 转发类 ==========

    @Test
    public void selectList_delegatesToMapper()
    {
        ProlistDefect q = new ProlistDefect();
        List<ProlistDefect> expected = Arrays.asList(new ProlistDefect(), new ProlistDefect());
        when(prolistDefectMapper.selectProlistDefectList(q)).thenReturn(expected);
        assertSame(expected, service.selectProlistDefectList(q));
    }

    @Test
    public void selectById_delegatesToMapper()
    {
        ProlistDefect expected = new ProlistDefect();
        when(prolistDefectMapper.selectProlistDefectByProblemId(7L)).thenReturn(expected);
        assertSame(expected, service.selectProlistDefectByProblemId(7L));
    }

    @Test
    public void delete_delegatesToMapper()
    {
        Long[] ids = { 1L, 2L };
        when(prolistDefectMapper.deleteProlistDefectByProblemIds(ids)).thenReturn(2);
        assertEquals(2, service.deleteProlistDefectByProblemIds(ids));
    }

    @Test
    public void linkage_delegatesToMapper()
    {
        when(prolistDefectMapper.selectPlanProductionDate(1L)).thenReturn("2026-01-18");
        assertEquals("2026-01-18", service.selectPlanProductionDate(1L));

        ProlistDefect info = new ProlistDefect();
        when(prolistDefectMapper.selectTaskInfo(9L)).thenReturn(info);
        assertSame(info, service.selectTaskInfo(9L));

        List<java.util.Map<String, Object>> opts = Arrays.asList();
        when(prolistDefectMapper.selectTaskOptions("2026", 1L, 201L)).thenReturn(opts);
        assertSame(opts, service.selectTaskOptions("2026", 1L, 201L));
    }

    // ========== 查重 ==========

    @Test
    public void checkProblemNoUnique_emptyReturnsFalse()
    {
        assertFalse(service.checkProblemNoUnique("", null));
        assertFalse(service.checkProblemNoUnique(null, null));
        verify(prolistDefectMapper, never()).checkProblemNoUnique(any(), any());
    }

    @Test
    public void checkProblemNoUnique_zeroCountReturnsTrue()
    {
        when(prolistDefectMapper.checkProblemNoUnique("PB-1", null)).thenReturn(0);
        assertTrue(service.checkProblemNoUnique("PB-1", null));
    }

    @Test
    public void checkProblemNoUnique_positiveCountReturnsFalse()
    {
        when(prolistDefectMapper.checkProblemNoUnique("PB-1", null)).thenReturn(1);
        assertFalse(service.checkProblemNoUnique("PB-1", null));
    }

    @Test
    public void checkProblemNoUnique_editPassesProblemIdToExcludeSelf()
    {
        when(prolistDefectMapper.checkProblemNoUnique("PB-1", 5L)).thenReturn(0);
        assertTrue(service.checkProblemNoUnique("PB-1", 5L));
        verify(prolistDefectMapper).checkProblemNoUnique("PB-1", 5L);
    }

    // ========== 派生算法 solutionTimeOverOneDay（经 insert 验证） ==========

    @Test
    public void insert_settleMinusSubmitGreaterThanOneDay_marksOverdue()
    {
        ProlistDefect d = new ProlistDefect();
        d.setProblemNo("PB-A");
        d.setSubmitDate(date("2026-06-01"));
        d.setSettleDate(date("2026-06-05")); // 差4天 > 1
        when(prolistDefectMapper.checkProblemNoUnique("PB-A", null)).thenReturn(0);
        when(prolistDefectMapper.insertProlistDefect(d)).thenReturn(1);
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class))
        {
            mocked.when(SecurityUtils::getUsername).thenReturn("admin");
            service.insertProlistDefect(d);
        }
        assertEquals("1", d.getSolutionTimeOverOneDay());
        assertEquals("admin", d.getCreateBy());
    }

    @Test
    public void insert_settleMinusSubmitWithinOneDay_marksNotOverdue()
    {
        ProlistDefect d = new ProlistDefect();
        d.setProblemNo("PB-B");
        d.setSubmitDate(date("2026-06-01"));
        d.setSettleDate(date("2026-06-02")); // 差1天，不>1
        when(prolistDefectMapper.checkProblemNoUnique("PB-B", null)).thenReturn(0);
        when(prolistDefectMapper.insertProlistDefect(d)).thenReturn(1);
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class))
        {
            mocked.when(SecurityUtils::getUsername).thenReturn("admin");
            service.insertProlistDefect(d);
        }
        assertEquals("0", d.getSolutionTimeOverOneDay());
    }

    @Test
    public void insert_settleNull_usesToday()
    {
        ProlistDefect d = new ProlistDefect();
        d.setProblemNo("PB-C");
        d.setSubmitDate(date("2026-06-01"));
        d.setSettleDate(null); // 用当天，今天必然 > 提交日1天以上（除非当天=次日内，测当下日期）
        when(prolistDefectMapper.checkProblemNoUnique("PB-C", null)).thenReturn(0);
        when(prolistDefectMapper.insertProlistDefect(d)).thenReturn(1);
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class))
        {
            mocked.when(SecurityUtils::getUsername).thenReturn("admin");
            service.insertProlistDefect(d);
        }
        // 提交日 2026-06-01 距今已远超1天
        assertEquals("1", d.getSolutionTimeOverOneDay());
    }

    @Test
    public void insert_submitDateNull_marksNotOverdue()
    {
        ProlistDefect d = new ProlistDefect();
        d.setProblemNo("PB-D");
        d.setSubmitDate(null);
        when(prolistDefectMapper.checkProblemNoUnique("PB-D", null)).thenReturn(0);
        when(prolistDefectMapper.insertProlistDefect(d)).thenReturn(1);
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class))
        {
            mocked.when(SecurityUtils::getUsername).thenReturn("admin");
            service.insertProlistDefect(d);
        }
        assertEquals("0", d.getSolutionTimeOverOneDay());
    }

    // ========== insert/update 查重拦截 ==========

    @Test
    public void insert_duplicateProblemNo_throws()
    {
        ProlistDefect d = new ProlistDefect();
        d.setProblemNo("PB-DUP");
        when(prolistDefectMapper.checkProblemNoUnique("PB-DUP", null)).thenReturn(1);
        assertThrows(ServiceException.class, () -> service.insertProlistDefect(d));
        verify(prolistDefectMapper, never()).insertProlistDefect(any());
    }

    @Test
    public void update_excludesSelfInUniqueCheck_andRecomputesDerived()
    {
        ProlistDefect d = new ProlistDefect();
        d.setProblemId(5L);
        d.setProblemNo("PB-E");
        d.setSubmitDate(date("2026-06-01"));
        d.setSettleDate(date("2026-06-10"));
        when(prolistDefectMapper.checkProblemNoUnique("PB-E", 5L)).thenReturn(0);
        when(prolistDefectMapper.updateProlistDefect(d)).thenReturn(1);
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class))
        {
            mocked.when(SecurityUtils::getUsername).thenReturn("editor");
            service.updateProlistDefect(d);
        }
        verify(prolistDefectMapper).checkProblemNoUnique("PB-E", 5L);
        assertEquals("1", d.getSolutionTimeOverOneDay());
        assertEquals("editor", d.getUpdateBy());
    }

    @Test
    public void update_duplicateProblemNo_throws()
    {
        ProlistDefect d = new ProlistDefect();
        d.setProblemId(5L);
        d.setProblemNo("PB-DUP");
        when(prolistDefectMapper.checkProblemNoUnique("PB-DUP", 5L)).thenReturn(1);
        assertThrows(ServiceException.class, () -> service.updateProlistDefect(d));
        verify(prolistDefectMapper, never()).updateProlistDefect(any());
    }
}
