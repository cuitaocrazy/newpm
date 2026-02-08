package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.service.IProjectReviewService;
import com.ruoyi.project.service.IProjectEmailService;

/**
 * 项目审核Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-08
 */
@Service
public class ProjectReviewServiceImpl implements IProjectReviewService
{
    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private IProjectEmailService projectEmailService;

    /**
     * 查询待审核项目列表
     *
     * @param project 项目查询条件
     * @return 待审核项目列表
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u1")
    public List<Project> selectReviewList(Project project)
    {
        return projectMapper.selectReviewList(project);
    }

    /**
     * 查询项目详细信息（用于审核）
     *
     * @param projectId 项目ID
     * @return 项目详细信息
     */
    @Override
    public Project selectProjectById(Long projectId)
    {
        return projectMapper.selectProjectByProjectId(projectId);
    }

    /**
     * 审核项目
     *
     * @param projectId 项目ID
     * @param approvalStatus 审核状态（1通过/2拒绝）
     * @param approvalReason 审核意见
     * @return 结果
     */
    @Override
    @Transactional
    public int approveProject(Long projectId, String approvalStatus, String approvalReason)
    {
        Project project = new Project();
        project.setProjectId(projectId);
        project.setApprovalStatus(approvalStatus);
        project.setApprovalReason(approvalReason);
        project.setApproverId(SecurityUtils.getUserId());
        project.setApprovalTime(DateUtils.getNowDate());
        project.setUpdateBy(SecurityUtils.getUsername());
        project.setUpdateTime(DateUtils.getNowDate());

        int result = projectMapper.updateProject(project);

        // 异步发送邮件通知
        if (result > 0)
        {
            projectEmailService.sendApprovalNotification(projectId, approvalStatus, approvalReason);
        }

        return result;
    }
}
