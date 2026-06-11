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
