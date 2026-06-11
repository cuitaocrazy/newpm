package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.SysName;

/**
 * 出入库子系统配置 Mapper 接口
 *
 * @author ruoyi
 * @date 2026-06-11
 */
public interface SysNameMapper
{
    /** 按产品查子系统列表 */
    public List<SysName> selectSysNameByProduct(String product);

    /** 按子系统名称取配置（含基准版本号/产品） */
    public SysName selectSysNameByName(String sysName);

    /** 子系统列表（全量） */
    public List<SysName> selectSysNameList(SysName sysName);
}
