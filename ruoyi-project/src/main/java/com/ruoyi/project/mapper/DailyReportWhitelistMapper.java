package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.DailyReportWhitelist;

/**
 * 日报白名单 Mapper 接口
 */
public interface DailyReportWhitelistMapper {

    /**
     * 查询白名单列表（含用户姓名和部门名称，支持关键词过滤）
     */
    List<DailyReportWhitelist> selectWhitelistPage(DailyReportWhitelist query);

    /**
     * 统计某用户是否在白名单中（del_flag='0'）
     */
    int countByUserId(Long userId);

    /**
     * 新增白名单记录
     */
    int insertWhitelist(DailyReportWhitelist whitelist);

    /**
     * 软删除白名单记录（update del_flag='1'）
     */
    int deleteWhitelistById(Long id);

    /**
     * 检查当前用户是否在白名单中（语义与 countByUserId 相同，供 checkSelf 端点使用）
     */
    int checkSelfInWhitelist(Long userId);
}
