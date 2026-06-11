package com.ruoyi.project.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
}
