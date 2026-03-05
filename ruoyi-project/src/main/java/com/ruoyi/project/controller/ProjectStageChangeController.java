package com.ruoyi.project.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.ProjectStageChange;
import com.ruoyi.project.service.IProjectStageChangeService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目阶段变更Controller
 *
 * @author ruoyi
 * @date 2026-03-03
 */
@RestController
@RequestMapping("/project/projectStageChange")
public class ProjectStageChangeController extends BaseController
{
    @Autowired
    private IProjectStageChangeService projectStageChangeService;

    /** 以项目为主体的列表（含最新变更信息和合同状态） */
    @PreAuthorize("@ss.hasPermi('project:projectStageChange:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProjectStageChange projectStageChange)
    {
        startPage();
        List<ProjectStageChange> list = projectStageChangeService.selectProjectStageChangeList(projectStageChange);
        return getDataTable(list);
    }

    /** 导出 */
    @PreAuthorize("@ss.hasPermi('project:projectStageChange:export')")
    @Log(title = "项目阶段变更", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProjectStageChange projectStageChange)
    {
        List<ProjectStageChange> list = projectStageChangeService.selectProjectStageChangeList(projectStageChange);
        ExcelUtil<ProjectStageChange> util = new ExcelUtil<>(ProjectStageChange.class);
        util.exportExcel(response, list, "项目阶段变更数据");
    }

    /** 获取单条变更记录详情 */
    @PreAuthorize("@ss.hasPermi('project:projectStageChange:query')")
    @GetMapping("/{changeId}")
    public AjaxResult getInfo(@PathVariable Long changeId)
    {
        return success(projectStageChangeService.selectProjectStageChangeByChangeId(changeId));
    }

    /** 查询项目的全部变更历史（变更记录弹窗） */
    @PreAuthorize("@ss.hasPermi('project:projectStageChange:list')")
    @GetMapping("/history/{projectId}")
    public AjaxResult historyByProject(@PathVariable Long projectId)
    {
        return success(projectStageChangeService.historyByProject(projectId));
    }

    /** 新增变更记录（同时更新 pm_project.project_stage）*/
    @PreAuthorize("@ss.hasPermi('project:projectStageChange:add')")
    @Log(title = "项目阶段变更", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProjectStageChange projectStageChange)
    {
        return toAjax(projectStageChangeService.insertProjectStageChange(projectStageChange));
    }

    /** 批量变更阶段 */
    @PreAuthorize("@ss.hasPermi('project:projectStageChange:batchChange')")
    @Log(title = "项目阶段批量变更", businessType = BusinessType.UPDATE)
    @PostMapping("/batchChange")
    public AjaxResult batchChange(@RequestBody ProjectStageChange request)
    {
        return toAjax(projectStageChangeService.batchChange(
                request.getProjectIds(), request.getNewStage(), request.getChangeReason()));
    }

    /** 修改变更记录 */
    @PreAuthorize("@ss.hasPermi('project:projectStageChange:edit')")
    @Log(title = "项目阶段变更", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProjectStageChange projectStageChange)
    {
        return toAjax(projectStageChangeService.updateProjectStageChange(projectStageChange));
    }

    /** 删除变更记录 */
    @PreAuthorize("@ss.hasPermi('project:projectStageChange:remove')")
    @Log(title = "项目阶段变更", businessType = BusinessType.DELETE)
    @DeleteMapping("/{changeIds}")
    public AjaxResult remove(@PathVariable Long[] changeIds)
    {
        return toAjax(projectStageChangeService.deleteProjectStageChangeByChangeIds(changeIds));
    }
}
