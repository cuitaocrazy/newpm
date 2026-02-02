package com.ruoyi.project.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.ProjectApproval;
import com.ruoyi.project.service.IProjectApprovalService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目审核Controller
 * 
 * @author ruoyi
 * @date 2026-02-01
 */
@RestController
@RequestMapping("/project/approval")
public class ProjectApprovalController extends BaseController
{
    @Autowired
    private IProjectApprovalService projectApprovalService;

    /**
     * 查询项目审核列表
     */
    @PreAuthorize("@ss.hasPermi('project:approval:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProjectApproval projectApproval)
    {
        startPage();
        List<ProjectApproval> list = projectApprovalService.selectProjectApprovalList(projectApproval);
        return getDataTable(list);
    }

    /**
     * 导出项目审核列表
     */
    @PreAuthorize("@ss.hasPermi('project:approval:export')")
    @Log(title = "项目审核", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProjectApproval projectApproval)
    {
        List<ProjectApproval> list = projectApprovalService.selectProjectApprovalList(projectApproval);
        ExcelUtil<ProjectApproval> util = new ExcelUtil<ProjectApproval>(ProjectApproval.class);
        util.exportExcel(response, list, "项目审核数据");
    }

    /**
     * 获取项目审核详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:approval:query')")
    @GetMapping(value = "/{approvalId}")
    public AjaxResult getInfo(@PathVariable("approvalId") Long approvalId)
    {
        return success(projectApprovalService.selectProjectApprovalByApprovalId(approvalId));
    }

    /**
     * 新增项目审核
     */
    @PreAuthorize("@ss.hasPermi('project:approval:add')")
    @Log(title = "项目审核", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProjectApproval projectApproval)
    {
        return toAjax(projectApprovalService.insertProjectApproval(projectApproval));
    }

    /**
     * 修改项目审核
     */
    @PreAuthorize("@ss.hasPermi('project:approval:edit')")
    @Log(title = "项目审核", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProjectApproval projectApproval)
    {
        return toAjax(projectApprovalService.updateProjectApproval(projectApproval));
    }

    /**
     * 删除项目审核
     */
    @PreAuthorize("@ss.hasPermi('project:approval:remove')")
    @Log(title = "项目审核", businessType = BusinessType.DELETE)
	@DeleteMapping("/{approvalIds}")
    public AjaxResult remove(@PathVariable Long[] approvalIds)
    {
        return toAjax(projectApprovalService.deleteProjectApprovalByApprovalIds(approvalIds));
    }
}
