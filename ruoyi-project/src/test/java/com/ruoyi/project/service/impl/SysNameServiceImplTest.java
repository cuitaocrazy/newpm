package com.ruoyi.project.service.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.project.domain.SysName;
import com.ruoyi.project.mapper.SysNameMapper;

/**
 * 出入库子系统配置 Service 测试。
 */
@ExtendWith(MockitoExtension.class)
public class SysNameServiceImplTest
{
    @Mock
    private SysNameMapper sysNameMapper;

    @InjectMocks
    private SysNameServiceImpl service;

    @Test
    public void selectByProduct_delegates()
    {
        List<SysName> list = Arrays.asList(new SysName(), new SysName());
        when(sysNameMapper.selectSysNameByProduct("HHAP-COR")).thenReturn(list);
        assertSame(list, service.selectSysNameByProduct("HHAP-COR"));
    }

    @Test
    public void selectByName_delegates()
    {
        SysName s = new SysName();
        when(sysNameMapper.selectSysNameByName("核心子系统")).thenReturn(s);
        assertSame(s, service.selectSysNameByName("核心子系统"));
    }

    @Test
    public void selectList_delegates()
    {
        SysName q = new SysName();
        List<SysName> list = Arrays.asList(new SysName());
        when(sysNameMapper.selectSysNameList(q)).thenReturn(list);
        assertSame(list, service.selectSysNameList(q));
    }
}
