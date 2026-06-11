package com.ruoyi.project.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.project.domain.SysName;
import com.ruoyi.project.domain.VersionOut;
import com.ruoyi.project.mapper.SysNameMapper;
import com.ruoyi.project.mapper.VersionOutMapper;

/**
 * 出入库版本号生成算法特征测试（锁定 6 类型规则，迁移自 yadapm）。
 */
@ExtendWith(MockitoExtension.class)
public class VersionNumberGeneratorTest
{
    @Mock
    private VersionOutMapper versionOutMapper;

    @Mock
    private SysNameMapper sysNameMapper;

    @Spy
    @InjectMocks
    private VersionNumberGenerator generator;

    @BeforeEach
    public void setup()
    {
        SysName cfg = new SysName();
        cfg.setSysName("SYS");
        cfg.setBaseVersionCode("ABC");
        cfg.setProduct("P1");
        lenient().when(sysNameMapper.selectSysNameByName("SYS")).thenReturn(cfg);
        // 固定年份，保证类型4 用例可断言
        lenient().doReturn("2026").when(generator).curYear();
    }

    @Test
    public void type1_firstVersion_padTo01()
    {
        when(versionOutMapper.getMaxVersionCode("SYS", "1")).thenReturn(0);
        String[] r = generator.generate("SYS", "SUB", "1", null, "1", null, null, null);
        assertEquals("ABC_SP01", r[0]);
        assertEquals("01", r[1]);
    }

    @Test
    public void type1_continuation()
    {
        when(versionOutMapper.getMaxVersionCode("SYS", "1")).thenReturn(5);
        String[] r = generator.generate("SYS", "SUB", "1", null, "1", null, null, null);
        assertEquals("ABC_SP06", r[0]);
    }

    @Test
    public void type2_ptf()
    {
        when(versionOutMapper.getMaxVersionCode("SYS", "2")).thenReturn(0);
        String[] r = generator.generate("SYS", "SUB", "2", null, "1", null, null, null);
        assertEquals("ABC_PTF01", r[0]);
    }

    @Test
    public void type3_carryFrom09To10()
    {
        when(versionOutMapper.getMaxVersionCode("SYS", "3")).thenReturn(9);
        String[] r = generator.generate("SYS", "SUB", "3", null, "1", null, null, null);
        assertEquals("ABC_B10", r[0]);
    }

    @Test
    public void type4_add_padTo3Digits()
    {
        when(versionOutMapper.getMaxVersionCodeByYear("SUB", "4")).thenReturn(2);
        String[] r = generator.generate("SYS", "SUB", "4", null, "1", null, null, null);
        assertEquals("T_2026_003_P1", r[0]);
        assertEquals("003", r[1]);
    }

    @Test
    public void type4_edit_keyUnchanged_keepOldCode()
    {
        VersionOut old = new VersionOut();
        old.setVersionCode("005");
        when(versionOutMapper.selectVersionOutById(1L)).thenReturn(old);
        String[] r = generator.generate("SYS", "SUB", "4", null, "2", 1L, "SUB", "4");
        assertEquals("T_2026_005_P1", r[0]);
    }

    @Test
    public void type4_edit_keyChanged_recompute()
    {
        when(versionOutMapper.getMaxVersionCodeByYear("SUB", "4")).thenReturn(7);
        String[] r = generator.generate("SYS", "SUB", "4", null, "2", 1L, "OLDSUB", "4");
        assertEquals("T_2026_008_P1", r[0]);
    }

    @Test
    public void type5_firstUpgrade_fallbackToBaseType3()
    {
        // 升级包初级版本号下无记录 → 回退查 B测试包(3) 基线，得单段 "02"
        when(versionOutMapper.getCodeByOutVersion("SYS", "5", "ABC_B02")).thenReturn(null);
        when(versionOutMapper.getCodeByBaseVersion("SYS", "3", "ABC_B02")).thenReturn("02");
        String[] r = generator.generate("SYS", "SUB", "5", "ABC_B02", "1", null, null, null);
        assertEquals("ABC_B02.01", r[0]);
    }

    @Test
    public void type5_existingUpgrade_carry09To10()
    {
        when(versionOutMapper.getCodeByOutVersion("SYS", "5", "ABC_B02")).thenReturn("02.09");
        String[] r = generator.generate("SYS", "SUB", "5", "ABC_B02", "1", null, null, null);
        assertEquals("ABC_B02.10", r[0]);
    }

    @Test
    public void type6_firstUpgrade_fallbackToBaseType1()
    {
        when(versionOutMapper.getCodeByOutVersion("SYS", "6", "ABC_SP03")).thenReturn(null);
        when(versionOutMapper.getCodeByBaseVersion("SYS", "1", "ABC_SP03")).thenReturn("03");
        String[] r = generator.generate("SYS", "SUB", "6", "ABC_SP03", "1", null, null, null);
        assertEquals("ABC_SP03.01", r[0]);
    }
}
