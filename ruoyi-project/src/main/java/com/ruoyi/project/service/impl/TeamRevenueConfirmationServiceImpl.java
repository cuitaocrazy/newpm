package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.TeamRevenueConfirmationMapper;
import com.ruoyi.project.domain.TeamRevenueConfirmation;
import com.ruoyi.project.service.ITeamRevenueConfirmationService;

/**
 * 团队收入确认Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-02-12
 */
@Service
public class TeamRevenueConfirmationServiceImpl implements ITeamRevenueConfirmationService 
{
    @Autowired
    private TeamRevenueConfirmationMapper teamRevenueConfirmationMapper;

    /**
     * 查询团队收入确认
     * 
     * @param teamConfirmId 团队收入确认主键
     * @return 团队收入确认
     */
    @Override
    public TeamRevenueConfirmation selectTeamRevenueConfirmationByTeamConfirmId(Long teamConfirmId)
    {
        return teamRevenueConfirmationMapper.selectTeamRevenueConfirmationByTeamConfirmId(teamConfirmId);
    }

    /**
     * 查询团队收入确认列表
     * 
     * @param teamRevenueConfirmation 团队收入确认
     * @return 团队收入确认
     */
    @Override
    public List<TeamRevenueConfirmation> selectTeamRevenueConfirmationList(TeamRevenueConfirmation teamRevenueConfirmation)
    {
        return teamRevenueConfirmationMapper.selectTeamRevenueConfirmationList(teamRevenueConfirmation);
    }

    /**
     * 新增团队收入确认
     * 
     * @param teamRevenueConfirmation 团队收入确认
     * @return 结果
     */
    @Override
    public int insertTeamRevenueConfirmation(TeamRevenueConfirmation teamRevenueConfirmation)
    {
        teamRevenueConfirmation.setCreateTime(DateUtils.getNowDate());
        return teamRevenueConfirmationMapper.insertTeamRevenueConfirmation(teamRevenueConfirmation);
    }

    /**
     * 修改团队收入确认
     * 
     * @param teamRevenueConfirmation 团队收入确认
     * @return 结果
     */
    @Override
    public int updateTeamRevenueConfirmation(TeamRevenueConfirmation teamRevenueConfirmation)
    {
        teamRevenueConfirmation.setUpdateTime(DateUtils.getNowDate());
        return teamRevenueConfirmationMapper.updateTeamRevenueConfirmation(teamRevenueConfirmation);
    }

    /**
     * 批量删除团队收入确认
     * 
     * @param teamConfirmIds 需要删除的团队收入确认主键
     * @return 结果
     */
    @Override
    public int deleteTeamRevenueConfirmationByTeamConfirmIds(Long[] teamConfirmIds)
    {
        return teamRevenueConfirmationMapper.deleteTeamRevenueConfirmationByTeamConfirmIds(teamConfirmIds);
    }

    /**
     * 删除团队收入确认信息
     * 
     * @param teamConfirmId 团队收入确认主键
     * @return 结果
     */
    @Override
    public int deleteTeamRevenueConfirmationByTeamConfirmId(Long teamConfirmId)
    {
        return teamRevenueConfirmationMapper.deleteTeamRevenueConfirmationByTeamConfirmId(teamConfirmId);
    }
}
