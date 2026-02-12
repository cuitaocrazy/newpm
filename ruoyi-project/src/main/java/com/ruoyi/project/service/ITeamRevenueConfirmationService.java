package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.TeamRevenueConfirmation;

/**
 * 团队收入确认Service接口
 * 
 * @author ruoyi
 * @date 2026-02-12
 */
public interface ITeamRevenueConfirmationService 
{
    /**
     * 查询团队收入确认
     * 
     * @param teamConfirmId 团队收入确认主键
     * @return 团队收入确认
     */
    public TeamRevenueConfirmation selectTeamRevenueConfirmationByTeamConfirmId(Long teamConfirmId);

    /**
     * 查询团队收入确认列表
     * 
     * @param teamRevenueConfirmation 团队收入确认
     * @return 团队收入确认集合
     */
    public List<TeamRevenueConfirmation> selectTeamRevenueConfirmationList(TeamRevenueConfirmation teamRevenueConfirmation);

    /**
     * 新增团队收入确认
     * 
     * @param teamRevenueConfirmation 团队收入确认
     * @return 结果
     */
    public int insertTeamRevenueConfirmation(TeamRevenueConfirmation teamRevenueConfirmation);

    /**
     * 修改团队收入确认
     * 
     * @param teamRevenueConfirmation 团队收入确认
     * @return 结果
     */
    public int updateTeamRevenueConfirmation(TeamRevenueConfirmation teamRevenueConfirmation);

    /**
     * 批量删除团队收入确认
     * 
     * @param teamConfirmIds 需要删除的团队收入确认主键集合
     * @return 结果
     */
    public int deleteTeamRevenueConfirmationByTeamConfirmIds(Long[] teamConfirmIds);

    /**
     * 删除团队收入确认信息
     * 
     * @param teamConfirmId 团队收入确认主键
     * @return 结果
     */
    public int deleteTeamRevenueConfirmationByTeamConfirmId(Long teamConfirmId);
}
