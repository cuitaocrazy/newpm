package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.DailyReportWhitelist;

/**
 * 日报白名单 Service 接口
 */
public interface IDailyReportWhitelistService {

    /**
     * 查询白名单分页列表（含用户姓名和部门，支持关键词搜索）
     */
    List<DailyReportWhitelist> selectWhitelistPage(DailyReportWhitelist query);

    /**
     * 添加人员到白名单（防重复）
     */
    int addToWhitelist(DailyReportWhitelist whitelist);

    /**
     * 从白名单移除（软删除）
     */
    int removeFromWhitelist(Long id);

    /**
     * 检查指定用户是否在白名单中
     */
    boolean isInWhitelist(Long userId);
}
