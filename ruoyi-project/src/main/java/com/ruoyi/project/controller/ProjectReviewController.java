package com.ruoyi.project.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.service.IProjectReviewService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目审核Controller
 *
 * @author ruoyi
 * @date 2026-02-08
 */
@RestController
@RequestMapping("/project/review")
public class ProjectReviewController extends BaseController
{
    @Autowired
    private IProjectReviewService projectReviewService;

    /**
     * 查询待审核项目列表
     */
    @PreAuthorize("@ss.hasPermi('project:review:query')")
    @GetMapping("/list")
    public TableDataInfo list(Project project)
    {
        startPage();
        List<Project> list = projectReviewService.selectReviewList(project);
        return getDataTable(list);
    }

    /**
     * 获取项目详细信息（用于审核）
     */
    @PreAuthorize("@ss.hasPermi('project:review:query')")
    @GetMapping(value = "/{projectId}")
    public AjaxResult getInfo(@PathVariable("projectId") Long projectId)
    {
        return success(projectReviewService.selectProjectById(projectId));
    }

    /**
     * 审核项目
     */
    @PreAuthorize("@ss.hasPermi('project:review:approve')")
    @Log(title = "项目审核", businessType = BusinessType.UPDATE)
    @PostMapping("/approve")
    public AjaxResult approve(@RequestBody Project project)
    {
        // 验证审核状态
        if (project.getApprovalStatus() == null ||
            (!project.getApprovalStatus().equals("1") && !project.getApprovalStatus().equals("2")))
        {
            return error("审核状态无效");
        }

        // 拒绝时必须填写审核意见
        if (project.getApprovalStatus().equals("2") &&
            (project.getApprovalReason() == null || project.getApprovalReason().trim().isEmpty()))
        {
            return error("拒绝审核时必须填写审核意见");
        }

        int result = projectReviewService.approveProject(
            project.getProjectId(),
            project.getApprovalStatus(),
            project.getApprovalReason()
        );

        return toAjax(result);
    }
}
