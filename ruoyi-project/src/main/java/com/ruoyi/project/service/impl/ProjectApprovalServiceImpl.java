package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.ProjectApprovalMapper;
import com.ruoyi.project.domain.ProjectApproval;
import com.ruoyi.project.service.IProjectApprovalService;

/**
 * 项目审核Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-02-01
 */
@Service
public class ProjectApprovalServiceImpl implements IProjectApprovalService 
{
    @Autowired
    private ProjectApprovalMapper projectApprovalMapper;

    /**
     * 查询项目审核
     * 
     * @param approvalId 项目审核主键
     * @return 项目审核
     */
    @Override
    public ProjectApproval selectProjectApprovalByApprovalId(Long approvalId)
    {
        return projectApprovalMapper.selectProjectApprovalByApprovalId(approvalId);
    }

    /**
     * 查询项目审核列表
     * 
     * @param projectApproval 项目审核
     * @return 项目审核
     */
    @Override
    public List<ProjectApproval> selectProjectApprovalList(ProjectApproval projectApproval)
    {
        return projectApprovalMapper.selectProjectApprovalList(projectApproval);
    }

    /**
     * 新增项目审核
     * 
     * @param projectApproval 项目审核
     * @return 结果
     */
    @Override
    public int insertProjectApproval(ProjectApproval projectApproval)
    {
        projectApproval.setCreateTime(DateUtils.getNowDate());
        return projectApprovalMapper.insertProjectApproval(projectApproval);
    }

    /**
     * 修改项目审核
     * 
     * @param projectApproval 项目审核
     * @return 结果
     */
    @Override
    public int updateProjectApproval(ProjectApproval projectApproval)
    {
        projectApproval.setUpdateTime(DateUtils.getNowDate());
        return projectApprovalMapper.updateProjectApproval(projectApproval);
    }

    /**
     * 批量删除项目审核
     * 
     * @param approvalIds 需要删除的项目审核主键
     * @return 结果
     */
    @Override
    public int deleteProjectApprovalByApprovalIds(Long[] approvalIds)
    {
        return projectApprovalMapper.deleteProjectApprovalByApprovalIds(approvalIds);
    }

    /**
     * 删除项目审核信息
     * 
     * @param approvalId 项目审核主键
     * @return 结果
     */
    @Override
    public int deleteProjectApprovalByApprovalId(Long approvalId)
    {
        return projectApprovalMapper.deleteProjectApprovalByApprovalId(approvalId);
    }
}
