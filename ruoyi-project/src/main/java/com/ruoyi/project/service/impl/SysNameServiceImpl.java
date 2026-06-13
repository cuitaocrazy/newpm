package com.ruoyi.project.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.domain.SysName;
import com.ruoyi.project.mapper.SysNameMapper;
import com.ruoyi.project.service.ISysNameService;

/**
 * 出入库子系统配置 Service 实现
 *
 * @author ruoyi
 * @date 2026-06-11
 */
@Service
public class SysNameServiceImpl implements ISysNameService
{
    @Autowired
    private SysNameMapper sysNameMapper;

    @Override
    public List<SysName> selectSysNameByProduct(String product)
    {
        return sysNameMapper.selectSysNameByProduct(product);
    }

    @Override
    public SysName selectSysNameByName(String sysName)
    {
        return sysNameMapper.selectSysNameByName(sysName);
    }

    @Override
    public List<SysName> selectSysNameList(SysName sysName)
    {
        return sysNameMapper.selectSysNameList(sysName);
    }
}
