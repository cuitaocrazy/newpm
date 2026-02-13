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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.ProjectManagerChange;
import com.ruoyi.project.domain.vo.ProjectManagerChangeVO;
import com.ruoyi.project.service.IProjectManagerChangeService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目经理变更Controller
 *
 * @author ruoyi
 * @date 2026-02-14
 */
@RestController
@RequestMapping("/project/managerChange")
public class ProjectManagerChangeController extends BaseController
{
    @Autowired
    private IProjectManagerChangeService projectManagerChangeService;

    /**
     * 查询项目列表（带最新变更信息）
     */
    @PreAuthorize("@ss.hasPermi('project:managerChange:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProjectManagerChangeVO vo)
    {
        startPage();
        List<ProjectManagerChangeVO> list = projectManagerChangeService.selectProjectListWithLatestChange(vo);
        return getDataTable(list);
    }

    /**
     * 查询项目的变更历史
     */
    @PreAuthorize("@ss.hasPermi('project:managerChange:query')")
    @GetMapping("/history/{projectId}")
    public AjaxResult history(@PathVariable("projectId") Long projectId)
    {
        return success(projectManagerChangeService.selectProjectChangeHistory(projectId));
    }

    /**
     * 单个项目变更经理
     */
    @PreAuthorize("@ss.hasPermi('project:managerChange:change')")
    @Log(title = "项目经理变更", businessType = BusinessType.UPDATE)
    @PostMapping("/change")
    public AjaxResult change(@RequestBody ProjectManagerChange data)
    {
        return toAjax(projectManagerChangeService.changeProjectManager(
            data.getProjectId(),
            data.getNewManagerId(),
            data.getChangeReason()
        ));
    }

    /**
     * 批量变更项目经理
     */
    @PreAuthorize("@ss.hasPermi('project:managerChange:batchChange')")
    @Log(title = "项目经理批量变更", businessType = BusinessType.UPDATE)
    @PostMapping("/batchChange")
    public AjaxResult batchChange(@RequestParam("projectIds") Long[] projectIds,
                                   @RequestParam("newManagerId") Long newManagerId,
                                   @RequestParam(value = "changeReason", required = false) String changeReason)
    {
        return toAjax(projectManagerChangeService.batchChangeProjectManager(projectIds, newManagerId, changeReason));
    }

    /**
     * 导出项目经理变更列表
     */
    @PreAuthorize("@ss.hasPermi('project:managerChange:export')")
    @Log(title = "项目经理变更", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProjectManagerChange projectManagerChange)
    {
        List<ProjectManagerChange> list = projectManagerChangeService.selectProjectManagerChangeList(projectManagerChange);
        ExcelUtil<ProjectManagerChange> util = new ExcelUtil<ProjectManagerChange>(ProjectManagerChange.class);
        util.exportExcel(response, list, "项目经理变更数据");
    }

    /**
     * 获取项目经理变更详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:managerChange:query')")
    @GetMapping(value = "/{changeId}")
    public AjaxResult getInfo(@PathVariable("changeId") Long changeId)
    {
        return success(projectManagerChangeService.selectProjectManagerChangeByChangeId(changeId));
    }

    /**
     * 新增项目经理变更
     */
    @PreAuthorize("@ss.hasPermi('project:managerChange:add')")
    @Log(title = "项目经理变更", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProjectManagerChange projectManagerChange)
    {
        return toAjax(projectManagerChangeService.insertProjectManagerChange(projectManagerChange));
    }

    /**
     * 修改项目经理变更
     */
    @PreAuthorize("@ss.hasPermi('project:managerChange:edit')")
    @Log(title = "项目经理变更", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProjectManagerChange projectManagerChange)
    {
        return toAjax(projectManagerChangeService.updateProjectManagerChange(projectManagerChange));
    }

    /**
     * 删除项目经理变更
     */
    @PreAuthorize("@ss.hasPermi('project:managerChange:remove')")
    @Log(title = "项目经理变更", businessType = BusinessType.DELETE)
	@DeleteMapping("/{changeIds}")
    public AjaxResult remove(@PathVariable Long[] changeIds)
    {
        return toAjax(projectManagerChangeService.deleteProjectManagerChangeByChangeIds(changeIds));
    }
}
