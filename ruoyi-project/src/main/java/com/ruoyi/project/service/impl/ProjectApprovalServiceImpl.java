package com.ruoyi.project.service.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.ProjectApprovalMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.ProjectApproval;
import com.ruoyi.project.service.IProjectApprovalService;

/**
 * 立项审核Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
@Service
public class ProjectApprovalServiceImpl implements IProjectApprovalService
{
    @Autowired
    private ProjectApprovalMapper projectApprovalMapper;

    @Autowired
    private ProjectMapper projectMapper;

    /**
     * 查询立项审核
     * 
     * @param approvalId 立项审核主键
     * @return 立项审核
     */
    @Override
    public ProjectApproval selectProjectApprovalByApprovalId(Long approvalId)
    {
        return projectApprovalMapper.selectProjectApprovalByApprovalId(approvalId);
    }

    /**
     * 查询立项审核列表
     * 
     * @param projectApproval 立项审核
     * @return 立项审核
     */
    @Override
    public List<ProjectApproval> selectProjectApprovalList(ProjectApproval projectApproval)
    {
        return projectApprovalMapper.selectProjectApprovalList(projectApproval);
    }

    /**
     * 新增立项审核
     * 
     * @param projectApproval 立项审核
     * @return 结果
     */
    @Override
    public int insertProjectApproval(ProjectApproval projectApproval)
    {
        projectApproval.setCreateTime(DateUtils.getNowDate());
        return projectApprovalMapper.insertProjectApproval(projectApproval);
    }

    /**
     * 修改立项审核
     * 
     * @param projectApproval 立项审核
     * @return 结果
     */
    @Override
    public int updateProjectApproval(ProjectApproval projectApproval)
    {
        projectApproval.setUpdateTime(DateUtils.getNowDate());
        return projectApprovalMapper.updateProjectApproval(projectApproval);
    }

    /**
     * 批量删除立项审核
     * 
     * @param approvalIds 需要删除的立项审核主键
     * @return 结果
     */
    @Override
    public int deleteProjectApprovalByApprovalIds(Long[] approvalIds)
    {
        return projectApprovalMapper.deleteProjectApprovalByApprovalIds(approvalIds);
    }

    /**
     * 删除立项审核信息
     *
     * @param approvalId 立项审核主键
     * @return 结果
     */
    @Override
    public int deleteProjectApprovalByApprovalId(Long approvalId)
    {
        return projectApprovalMapper.deleteProjectApprovalByApprovalId(approvalId);
    }

    /**
     * 审核项目（通过/拒绝）
     *
     * @param projectId 项目ID
     * @param approvalStatus 审核状态（1-通过/2-拒绝）
     * @param approvalReason 审核意见（拒绝时必填）
     * @return 结果
     */
    @Override
    @Transactional
    public int approveProject(Long projectId, String approvalStatus, String approvalReason)
    {
        // 1. 更新项目表的审核状态
        Project project = new Project();
        project.setProjectId(projectId);
        project.setApprovalStatus(approvalStatus);
        project.setApprovalReason(approvalReason);
        project.setApprovalTime(new Date());
        project.setApproverId(String.valueOf(SecurityUtils.getUserId()));
        projectMapper.updateProject(project);

        // 2. 新增审核记录
        ProjectApproval approval = new ProjectApproval();
        approval.setProjectId(projectId);
        approval.setApprovalStatus(approvalStatus);
        approval.setApprovalReason(approvalReason);
        approval.setApprovalTime(new Date());
        approval.setApproverId(SecurityUtils.getUserId());
        approval.setCreateTime(new Date());

        return projectApprovalMapper.insertProjectApproval(approval);
    }

    /**
     * 查询项目的审核历史
     *
     * @param projectId 项目ID
     * @return 审核历史列表（按时间倒序）
     */
    @Override
    public List<ProjectApproval> selectApprovalHistory(Long projectId)
    {
        ProjectApproval query = new ProjectApproval();
        query.setProjectId(projectId);
        return projectApprovalMapper.selectProjectApprovalList(query);
    }
}
