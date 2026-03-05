package com.ruoyi.project.controller;

import java.util.List;
import java.util.Map;
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

@RestController
@RequestMapping("/project/review")
public class ProjectReviewController extends BaseController
{
    @Autowired
    private IProjectReviewService projectReviewService;

    @PreAuthorize("@ss.hasPermi('project:review:query')")
    @GetMapping("/list")
    public TableDataInfo list(Project project)
    {
        startPage();
        List<Project> list = projectReviewService.selectReviewList(project);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('project:review:query')")
    @GetMapping("/summary")
    public AjaxResult summary(Project project)
    {
        return success(projectReviewService.selectReviewSummary(project));
    }

    @PreAuthorize("@ss.hasPermi('project:review:query')")
    @GetMapping(value = "/{projectId}")
    public AjaxResult getInfo(@PathVariable("projectId") Long projectId)
    {
        return success(projectReviewService.selectProjectById(projectId));
    }

    @PreAuthorize("@ss.hasPermi('project:review:approve')")
    @Log(title = "项目审核", businessType = BusinessType.UPDATE)
    @PostMapping("/approve")
    public AjaxResult approve(@RequestBody Project project)
    {
        if (project.getApprovalStatus() == null ||
            (!project.getApprovalStatus().equals("1") && !project.getApprovalStatus().equals("2")))
        {
            return error("审核状态无效");
        }
        if (project.getApprovalStatus().equals("2") &&
            (project.getApprovalReason() == null || project.getApprovalReason().trim().isEmpty()))
        {
            return error("拒绝审核时必须填写审核意见");
        }
        return toAjax(projectReviewService.approveProject(
            project.getProjectId(),
            project.getApprovalStatus(),
            project.getApprovalReason()
        ));
    }

    @PreAuthorize("@ss.hasPermi('project:review:approve')")
    @Log(title = "项目审核退回", businessType = BusinessType.UPDATE)
    @PostMapping("/rollback")
    public AjaxResult rollback(@RequestBody Map<String, Object> params)
    {
        Long projectId = Long.valueOf(params.get("projectId").toString());
        String rollbackReason = params.getOrDefault("rollbackReason", "").toString();
        return toAjax(projectReviewService.rollbackProject(projectId, rollbackReason));
    }
}
