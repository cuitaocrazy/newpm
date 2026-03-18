package com.ruoyi.project.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.DailyReportWhitelist;
import com.ruoyi.project.mapper.DailyReportWhitelistMapper;
import com.ruoyi.project.service.IDailyReportWhitelistService;

/**
 * 日报白名单 Service 实现
 */
@Service
public class DailyReportWhitelistServiceImpl implements IDailyReportWhitelistService {

    @Autowired
    private DailyReportWhitelistMapper whitelistMapper;

    @Override
    public List<DailyReportWhitelist> selectWhitelistPage(DailyReportWhitelist query) {
        return whitelistMapper.selectWhitelistPage(query);
    }

    @Override
    public int addToWhitelist(DailyReportWhitelist whitelist) {
        if (whitelistMapper.countByUserId(whitelist.getUserId()) > 0) {
            throw new ServiceException("该人员已在白名单中，请勿重复添加");
        }
        whitelist.setCreateBy(SecurityUtils.getUsername());
        whitelist.setCreateTime(DateUtils.getNowDate());
        return whitelistMapper.insertWhitelist(whitelist);
    }

    @Override
    public int removeFromWhitelist(Long id) {
        return whitelistMapper.deleteWhitelistById(id);
    }

    @Override
    public boolean isInWhitelist(Long userId) {
        return whitelistMapper.checkSelfInWhitelist(userId) > 0;
    }
}
