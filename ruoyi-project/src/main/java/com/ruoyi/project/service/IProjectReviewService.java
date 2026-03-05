package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.Project;

/**
 * 项目审核Service接口
 *
 * @author ruoyi
 * @date 2026-02-08
 */
public interface IProjectReviewService
{
    public List<Project> selectReviewList(Project project);

    public Map<String, Object> selectReviewSummary(Project project);

    public Project selectProjectById(Long projectId);

    public int approveProject(Long projectId, String approvalStatus, String approvalReason);

    public int rollbackProject(Long projectId, String rollbackReason);
}
