package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.Project;

/**
 * 项目审核Service接口
 *
 * @author ruoyi
 * @date 2026-02-08
 */
public interface IProjectReviewService
{
    /**
     * 查询待审核项目列表
     *
     * @param project 项目查询条件
     * @return 待审核项目列表
     */
    public List<Project> selectReviewList(Project project);

    /**
     * 查询项目详细信息（用于审核）
     *
     * @param projectId 项目ID
     * @return 项目详细信息
     */
    public Project selectProjectById(Long projectId);

    /**
     * 审核项目
     *
     * @param projectId 项目ID
     * @param approvalStatus 审核状态（1通过/2拒绝）
     * @param approvalReason 审核意见
     * @return 结果
     */
    public int approveProject(Long projectId, String approvalStatus, String approvalReason);
}
