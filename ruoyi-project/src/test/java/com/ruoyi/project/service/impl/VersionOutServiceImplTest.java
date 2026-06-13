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
}
