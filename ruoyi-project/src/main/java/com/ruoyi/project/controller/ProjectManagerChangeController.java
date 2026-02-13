package com.ruoyi.project.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
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
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.ProjectManagerChange;
import com.ruoyi.project.domain.request.ChangeRequest;
import com.ruoyi.project.domain.request.BatchChangeRequest;
import com.ruoyi.project.domain.vo.ProjectManagerChangeVo;
import com.ruoyi.project.service.IProjectManagerChangeService;
import com.ruoyi.project.service.IProjectService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目经理变更Controller
 *
 * @author ruoyi
 * @date 2026-02-13
 */
@RestController
@RequestMapping("/project/projectManagerChange")
public class ProjectManagerChangeController extends BaseController
{
    @Autowired
    private IProjectManagerChangeService projectManagerChangeService;

    @Autowired
    private IProjectService projectService;

    @Autowired
    private ISysUserService userService;

    /**
     * 查询项目经理变更列表
     */
    @PreAuthorize("@ss.hasPermi('project:projectManagerChange:query')")
    @GetMapping("/list")
    public TableDataInfo list(ProjectManagerChange projectManagerChange)
    {
        startPage();
        List<ProjectManagerChangeVo> list = projectManagerChangeService.selectProjectManagerChangeList(projectManagerChange);
        return getDataTable(list);
    }

    /**
     * 导出项目经理变更列表
     */
    @PreAuthorize("@ss.hasPermi('project:projectManagerChange:export')")
    @Log(title = "项目经理变更", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProjectManagerChange projectManagerChange)
    {
        List<ProjectManagerChangeVo> list = projectManagerChangeService.selectProjectManagerChangeList(projectManagerChange);
        ExcelUtil<ProjectManagerChangeVo> util = new ExcelUtil<ProjectManagerChangeVo>(ProjectManagerChangeVo.class);
        util.exportExcel(response, list, "项目经理变更数据");
    }

    /**
     * 获取项目经理变更详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:projectManagerChange:query')")
    @GetMapping(value = "/{changeId}")
    public AjaxResult getInfo(@PathVariable("changeId") Long changeId)
    {
        return success(projectManagerChangeService.selectProjectManagerChangeByChangeId(changeId));
    }

    /**
     * 新增项目经理变更
     */
    @PreAuthorize("@ss.hasPermi('project:projectManagerChange:add')")
    @Log(title = "项目经理变更", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProjectManagerChange projectManagerChange)
    {
        return toAjax(projectManagerChangeService.insertProjectManagerChange(projectManagerChange));
    }

    /**
     * 修改项目经理变更
     */
    @PreAuthorize("@ss.hasPermi('project:projectManagerChange:edit')")
    @Log(title = "项目经理变更", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProjectManagerChange projectManagerChange)
    {
        return toAjax(projectManagerChangeService.updateProjectManagerChange(projectManagerChange));
    }

    /**
     * 删除项目经理变更
     */
    @PreAuthorize("@ss.hasPermi('project:projectManagerChange:remove')")
    @Log(title = "项目经理变更", businessType = BusinessType.DELETE)
	@DeleteMapping("/{changeIds}")
    public AjaxResult remove(@PathVariable Long[] changeIds)
    {
        return toAjax(projectManagerChangeService.deleteProjectManagerChangeByChangeIds(changeIds));
    }

    /**
     * 项目名称自动补全搜索
     */
    @PreAuthorize("@ss.hasPermi('project:projectManagerChange:query')")
    @GetMapping("/searchProjects")
    public AjaxResult searchProjects(@RequestParam String keyword)
    {
        if (StringUtils.isEmpty(keyword) || keyword.length() < 2)
        {
            return success(Collections.emptyList());
        }

        Project query = new Project();
        query.setProjectName(keyword);

        // 只查询前20条
        List<Map<String, Object>> projects = projectService.selectProjectList(query)
            .stream()
            .limit(20)
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("projectId", p.getProjectId());
                map.put("projectName", p.getProjectName());
                map.put("projectCode", p.getProjectCode());
                map.put("value", p.getProjectName()); // autocomplete需要的value字段
                return map;
            })
            .collect(Collectors.toList());

        return success(projects);
    }

    /**
     * 变更项目经理
     */
    @PreAuthorize("@ss.hasPermi('project:projectManagerChange:change')")
    @Log(title = "项目经理变更", businessType = BusinessType.UPDATE)
    @PostMapping("/change")
    public AjaxResult change(@Validated @RequestBody ChangeRequest request)
    {
        return toAjax(projectManagerChangeService.changeProjectManager(
            request.getProjectId(),
            request.getNewManagerId(),
            request.getChangeReason()
        ));
    }

    /**
     * 批量变更项目经理
     */
    @PreAuthorize("@ss.hasPermi('project:projectManagerChange:batchChange')")
    @Log(title = "批量变更项目经理", businessType = BusinessType.UPDATE)
    @PostMapping("/batchChange")
    public AjaxResult batchChange(@Validated @RequestBody BatchChangeRequest request)
    {
        int count = projectManagerChangeService.batchChangeProjectManager(
            request.getProjectIds(),
            request.getNewManagerId(),
            request.getChangeReason()
        );
        return success("成功变更 " + count + " 个项目的项目经理");
    }

    /**
     * 获取项目经理变更详情
     */
    @PreAuthorize("@ss.hasPermi('project:projectManagerChange:detail')")
    @GetMapping("/detail/{projectId}")
    public AjaxResult detail(@PathVariable("projectId") Long projectId)
    {
        return success(projectManagerChangeService.getChangeDetail(projectId));
    }
}
