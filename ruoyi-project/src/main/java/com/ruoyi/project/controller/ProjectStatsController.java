package com.ruoyi.project.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.project.domain.vo.ProjectStatsVO;
import com.ruoyi.project.service.IProjectStatsService;

/**
 * 项目人天统计 Controller
 */
@RestController
@RequestMapping("/project/dailyReport")
public class ProjectStatsController extends BaseController
{
    @Autowired
    private IProjectStatsService projectStatsService;

    /**
     * 分页查询项目人天统计
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:list')")
    @GetMapping("/projectStats")
    public AjaxResult projectStats(
            @RequestParam(required = false) String projectName,
            @RequestParam(defaultValue = "1")  int pageNum,
            @RequestParam(defaultValue = "10") int pageSize)
    {
        long total = projectStatsService.countProjects(projectName);
        List<ProjectStatsVO> rows = projectStatsService.selectProjectStatsList(projectName, pageNum, pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("rows", rows);
        return success(result);
    }

    /**
     * 更新调整人天
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:list')")
    @PutMapping("/projectStats/{projectId}/adjustWorkload")
    public AjaxResult updateAdjustWorkload(
            @PathVariable Long projectId,
            @RequestParam BigDecimal adjustWorkload)
    {
        return toAjax(projectStatsService.updateAdjustWorkload(projectId, adjustWorkload));
    }

    /**
     * 项目名称 autocomplete
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:list')")
    @GetMapping("/projectNameSuggestions")
    public AjaxResult projectNameSuggestions(@RequestParam(required = false) String keyword)
    {
        List<Map<String, Object>> list = projectStatsService.selectProjectNameSuggestions(keyword);
        return success(list);
    }
}
