package com.ruoyi.project.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.VersionOut;
import com.ruoyi.project.domain.VersionOutTask;
import com.ruoyi.project.mapper.VersionOutMapper;

/**
 * 出入库版本 Service 特征测试（CRUD + 级联）。
 */
@ExtendWith(MockitoExtension.class)
public class VersionOutServiceImplTest
{
    @Mock
    private VersionOutMapper versionOutMapper;

    @Mock
    private VersionNumberGenerator versionNumberGenerator;

    @InjectMocks
    private VersionOutServiceImpl service;

    @Test
    public void selectList_delegatesToMapper()
    {
        VersionOut q = new VersionOut();
        List<VersionOut> expected = Arrays.asList(new VersionOut(), new VersionOut());
        when(versionOutMapper.selectVersionOutList(q)).thenReturn(expected);
        assertSame(expected, service.selectVersionOutList(q));
    }

    @Test
    public void selectById_delegatesToMapper()
    {
        VersionOut v = new VersionOut();
        when(versionOutMapper.selectVersionOutById(9L)).thenReturn(v);
        assertSame(v, service.selectVersionOutById(9L));
    }

    @Test
    public void insert_generatesNumber_setsBatchFlag_andCascadesTasks()
    {
        VersionOut v = new VersionOut();
        v.setSysName("SYS");
        v.setVersionType("1");
        VersionOutTask t = new VersionOutTask();
        t.setTaskId(100L);
        v.setTaskList(new ArrayList<>(Arrays.asList(t)));

        when(versionNumberGenerator.generate(eq("SYS"), any(), eq("1"), any(), eq("1"), any(), any(), any()))
                .thenReturn(new String[] { "ABC_SP01", "01" });
        when(versionOutMapper.insertVersionOut(v)).thenReturn(1);

        try (MockedStatic<SecurityUtils> ms = mockStatic(SecurityUtils.class))
        {
            ms.when(SecurityUtils::getUsername).thenReturn("tester");
            int rows = service.insertVersionOut(v);
            assertEquals(1, rows);
        }

        assertEquals("ABC_SP01", v.getOutLibVersion());
        assertEquals("01", v.getVersionCode());
        assertEquals("0", v.getManualInput());
        verify(versionOutMapper).batchInsertVersionOutTask(any());
    }

    @Test
    public void update_keyChanged_regeneratesNumber()
    {
        VersionOut old = new VersionOut();
        old.setId(5L);
        old.setSysName("OLD"); old.setVersionType("1"); old.setSubVersionCode("S1");
        old.setOutLibVersion("OLD_SP01"); old.setVersionCode("01");
        when(versionOutMapper.selectVersionOutById(5L)).thenReturn(old);

        VersionOut upd = new VersionOut();
        upd.setId(5L);
        upd.setSysName("NEW"); upd.setVersionType("1"); upd.setSubVersionCode("S1");
        when(versionNumberGenerator.generate(eq("NEW"), any(), eq("1"), any(), eq("2"), eq(5L), any(), any()))
                .thenReturn(new String[] { "NEW_SP03", "03" });
        when(versionOutMapper.updateVersionOut(upd)).thenReturn(1);

        try (MockedStatic<SecurityUtils> ms = mockStatic(SecurityUtils.class))
        {
            ms.when(SecurityUtils::getUsername).thenReturn("tester");
            service.updateVersionOut(upd);
        }
        assertEquals("NEW_SP03", upd.getOutLibVersion());
        verify(versionOutMapper).deleteVersionOutTaskByVersionId(5L);
    }

    @Test
    public void update_keyUnchanged_keepsOldNumber()
    {
        VersionOut old = new VersionOut();
        old.setId(6L);
        old.setSysName("SYS"); old.setVersionType("1"); old.setSubVersionCode("S1");
        old.setOutLibVersion("SYS_SP09"); old.setVersionCode("09");
        when(versionOutMapper.selectVersionOutById(6L)).thenReturn(old);

        VersionOut upd = new VersionOut();
        upd.setId(6L);
        upd.setSysName("SYS"); upd.setVersionType("1"); upd.setSubVersionCode("S1");
        when(versionOutMapper.updateVersionOut(upd)).thenReturn(1);

        try (MockedStatic<SecurityUtils> ms = mockStatic(SecurityUtils.class))
        {
            ms.when(SecurityUtils::getUsername).thenReturn("tester");
            service.updateVersionOut(upd);
        }
        // 关键字段未变 → 沿用原号，不调用生成器
        assertEquals("SYS_SP09", upd.getOutLibVersion());
        Mockito.verifyNoInteractions(versionNumberGenerator);
    }

    @Test
    public void delete_removesTasksThenMain()
    {
        Long[] ids = new Long[] { 1L, 2L };
        when(versionOutMapper.deleteVersionOutByIds(ids)).thenReturn(2);
        int rows = service.deleteVersionOutByIds(ids);
        assertEquals(2, rows);
        verify(versionOutMapper, times(1)).deleteVersionOutTaskByVersionIds(ids);
        verify(versionOutMapper, times(1)).deleteVersionOutByIds(ids);
    }

    @Test
    public void insert_emptyTaskList_skipsCascade()
    {
        VersionOut v = new VersionOut();
        v.setSysName("SYS"); v.setVersionType("1");
        // 无 taskList
        when(versionNumberGenerator.generate(eq("SYS"), any(), eq("1"), any(), eq("1"), any(), any(), any()))
                .thenReturn(new String[] { "ABC_SP01", "01" });
        when(versionOutMapper.insertVersionOut(v)).thenReturn(1);
        try (MockedStatic<SecurityUtils> ms = mockStatic(SecurityUtils.class))
        {
            ms.when(SecurityUtils::getUsername).thenReturn("tester");
            assertEquals(1, service.insertVersionOut(v));
        }
        // 空任务列表 → 不调级联插入
        verify(versionOutMapper, Mockito.never()).batchInsertVersionOutTask(any());
    }

    @Test
    public void insert_duplicateThenSucceed_retries()
    {
        VersionOut v = new VersionOut();
        v.setSysName("SYS"); v.setVersionType("1");
        when(versionNumberGenerator.generate(eq("SYS"), any(), eq("1"), any(), eq("1"), any(), any(), any()))
                .thenReturn(new String[] { "ABC_SP01", "01" });
        // 第一次插入撞唯一键，第二次成功
        when(versionOutMapper.insertVersionOut(v))
                .thenThrow(new DuplicateKeyException("dup"))
                .thenReturn(1);
        try (MockedStatic<SecurityUtils> ms = mockStatic(SecurityUtils.class))
        {
            ms.when(SecurityUtils::getUsername).thenReturn("tester");
            assertEquals(1, service.insertVersionOut(v));
        }
        // 重试2次插入
        verify(versionOutMapper, times(2)).insertVersionOut(v);
    }

    @Test
    public void insert_alwaysDuplicate_throwsAfterRetries()
    {
        VersionOut v = new VersionOut();
        v.setSysName("SYS"); v.setVersionType("1");
        when(versionNumberGenerator.generate(eq("SYS"), any(), eq("1"), any(), eq("1"), any(), any(), any()))
                .thenReturn(new String[] { "ABC_SP01", "01" });
        when(versionOutMapper.insertVersionOut(v)).thenThrow(new DuplicateKeyException("dup"));
        try (MockedStatic<SecurityUtils> ms = mockStatic(SecurityUtils.class))
        {
            ms.when(SecurityUtils::getUsername).thenReturn("tester");
            ServiceException ex = assertThrows(
                    ServiceException.class, () -> service.insertVersionOut(v));
            assertTrue(ex.getMessage().contains("版本号生成冲突"));
        }
        // 重试满3次
        verify(versionOutMapper, times(3)).insertVersionOut(v);
    }

    @Test
    public void generateOutLibVersion_returnsMap()
    {
        VersionOut v = new VersionOut();
        v.setSysName("SYS"); v.setVersionType("1");
        when(versionNumberGenerator.generate(eq("SYS"), any(), eq("1"), any(), any(), any(), any(), any()))
                .thenReturn(new String[] { "ABC_SP05", "05" });
        java.util.Map<String, String> r = service.generateOutLibVersion(v, "1", null, null);
        assertEquals("ABC_SP05", r.get("outLibVersion"));
        assertEquals("05", r.get("versionCode"));
    }

    @Test
    public void cascadeQueries_delegateToMapper()
    {
        // 5 个联动/查询方法均转发 mapper
        List<java.util.Map<String, Object>> batches = Arrays.asList(new java.util.HashMap<>());
        when(versionOutMapper.selectBatchByYear("2026")).thenReturn(batches);
        assertSame(batches, service.selectBatchByYear("2026"));

        when(versionOutMapper.selectVersionPDate(1L)).thenReturn("2026-01-18");
        assertEquals("2026-01-18", service.selectVersionPDate(1L));

        VersionOutTask t = new VersionOutTask();
        when(versionOutMapper.selectTaskInfoByDemandNo("FR-1")).thenReturn(t);
        assertSame(t, service.selectTaskInfoByDemandNo("FR-1"));

        List<String> opts = Arrays.asList("v1", "v2");
        when(versionOutMapper.selectOutVersionOptions("SYS", "5")).thenReturn(opts);
        assertSame(opts, service.selectOutVersionOptions("SYS", "5"));

        List<VersionOutTask> tasks = Arrays.asList(new VersionOutTask());
        when(versionOutMapper.selectTaskOptions("2026", 1L, "P1")).thenReturn(tasks);
        assertSame(tasks, service.selectTaskOptions("2026", 1L, "P1"));
    }

    // ---------- 非批次（manual） ----------

    /** 构造一条非批次必填齐全的实体 */
    private VersionOut validManual()
    {
        VersionOut v = new VersionOut();
        v.setProductionYear("2026"); v.setBatchId(1L); v.setProduct("P1");
        v.setSysName("SYS"); v.setVersionType("1");
        v.setManualTaskNo("M-001"); v.setManualTaskName("手填任务");
        v.setIsInvolved("1"); v.setDbUpdate("1"); v.setUsbUpdate("1"); v.setVersionDescr("说明");
        return v;
    }

    @Test
    public void insertManual_setsManualFlag_generatesNumber()
    {
        VersionOut v = validManual();
        when(versionNumberGenerator.generate(eq("SYS"), any(), eq("1"), any(), eq("1"), any(), any(), any()))
                .thenReturn(new String[] { "ABC_SP01", "01" });
        when(versionOutMapper.insertVersionOutManual(v)).thenReturn(1);
        try (MockedStatic<SecurityUtils> ms = mockStatic(SecurityUtils.class))
        {
            ms.when(SecurityUtils::getUsername).thenReturn("tester");
            assertEquals(1, service.insertVersionOutManual(v));
        }
        assertEquals("1", v.getManualInput());
        assertEquals("ABC_SP01", v.getOutLibVersion());
    }

    @Test
    public void insertManual_missingTaskNo_throws()
    {
        VersionOut v = validManual();
        v.setManualTaskNo(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.insertVersionOutManual(v));
        assertTrue(ex.getMessage().contains("软件中心任务号"));
    }

    @Test
    public void insertManual_upgradeTypeMissingOutVersion_throws()
    {
        VersionOut v = validManual();
        v.setVersionType("5"); // 升级包需初级版本号
        ServiceException ex = assertThrows(ServiceException.class, () -> service.insertVersionOutManual(v));
        assertTrue(ex.getMessage().contains("初级版本号"));
    }

    @Test
    public void insertManual_validateBranches_throwForEachMissing()
    {
        // 逐个必填缺失都应抛 ServiceException（覆盖 validateManual 各分支）
        VersionOut a = validManual(); a.setProductionYear(null);
        assertThrows(ServiceException.class, () -> service.insertVersionOutManual(a));
        VersionOut b = validManual(); b.setBatchId(null);
        assertThrows(ServiceException.class, () -> service.insertVersionOutManual(b));
        VersionOut c = validManual(); c.setProduct(null);
        assertThrows(ServiceException.class, () -> service.insertVersionOutManual(c));
        VersionOut d = validManual(); d.setSysName(null);
        assertThrows(ServiceException.class, () -> service.insertVersionOutManual(d));
        VersionOut e = validManual(); e.setVersionType(null);
        assertThrows(ServiceException.class, () -> service.insertVersionOutManual(e));
        VersionOut f = validManual(); f.setManualTaskName(null);
        assertThrows(ServiceException.class, () -> service.insertVersionOutManual(f));
        VersionOut g = validManual(); g.setIsInvolved(null);
        assertThrows(ServiceException.class, () -> service.insertVersionOutManual(g));
        VersionOut h = validManual(); h.setDbUpdate(null);
        assertThrows(ServiceException.class, () -> service.insertVersionOutManual(h));
        VersionOut i = validManual(); i.setUsbUpdate(null);
        assertThrows(ServiceException.class, () -> service.insertVersionOutManual(i));
        VersionOut j = validManual(); j.setVersionDescr(null);
        assertThrows(ServiceException.class, () -> service.insertVersionOutManual(j));
    }

    @Test
    public void updateManual_keyUnchanged_keepsNumber()
    {
        VersionOut old = new VersionOut();
        old.setId(6L); old.setSysName("SYS"); old.setVersionType("1"); old.setSubVersionCode("P1");
        old.setOutLibVersion("SYS_SP09"); old.setVersionCode("09");
        when(versionOutMapper.selectVersionOutById(6L)).thenReturn(old);

        VersionOut upd = validManual();
        upd.setId(6L); upd.setSysName("SYS"); upd.setSubVersionCode("P1");
        when(versionOutMapper.updateVersionOutManual(upd)).thenReturn(1);
        try (MockedStatic<SecurityUtils> ms = mockStatic(SecurityUtils.class))
        {
            ms.when(SecurityUtils::getUsername).thenReturn("tester");
            service.updateVersionOutManual(upd);
        }
        assertEquals("SYS_SP09", upd.getOutLibVersion());
        verifyNoInteractions(versionNumberGenerator);
    }

    @Test
    public void updateManual_recordNotFound_throws()
    {
        VersionOut upd = validManual();
        upd.setId(99L);
        when(versionOutMapper.selectVersionOutById(99L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.updateVersionOutManual(upd));
    }

    @Test
    public void updateManual_keyChanged_regenerates()
    {
        VersionOut old = new VersionOut();
        old.setId(5L); old.setSysName("OLD"); old.setVersionType("1"); old.setSubVersionCode("S1");
        old.setOutLibVersion("OLD_SP01"); old.setVersionCode("01");
        when(versionOutMapper.selectVersionOutById(5L)).thenReturn(old);

        VersionOut upd = validManual();
        upd.setId(5L); upd.setSysName("NEW"); upd.setSubVersionCode("S1");
        when(versionNumberGenerator.generate(eq("NEW"), any(), eq("1"), any(), eq("2"), eq(5L), any(), any()))
                .thenReturn(new String[] { "NEW_SP03", "03" });
        when(versionOutMapper.updateVersionOutManual(upd)).thenReturn(1);
        try (MockedStatic<SecurityUtils> ms = mockStatic(SecurityUtils.class))
        {
            ms.when(SecurityUtils::getUsername).thenReturn("tester");
            service.updateVersionOutManual(upd);
        }
        assertEquals("NEW_SP03", upd.getOutLibVersion());
    }

    @Test
    public void selectManualList_delegates()
    {
        VersionOut q = new VersionOut();
        List<VersionOut> list = Arrays.asList(new VersionOut());
        when(versionOutMapper.selectVersionOutManualList(q)).thenReturn(list);
        assertSame(list, service.selectVersionOutManualList(q));
    }
}
