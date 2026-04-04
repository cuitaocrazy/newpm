package com.ruoyi.project.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.WorkloadCorrectLog;
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
        Project query = new Project();
        query.setProjectName(projectName);
        long total = projectStatsService.countProjects(query);
        List<ProjectStatsVO> rows = projectStatsService.selectProjectStatsList(query, pageNum, pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("rows", rows);
        return success(result);
    }

    /**
     * 人天补正（更新调整人天并记录日志）
     * 请求体: { direction, delta, afterAdjust, reason }
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:list')")
    @PostMapping("/projectStats/{projectId}/correct")
    public AjaxResult correct(
            @PathVariable Long projectId,
            @Validated @RequestBody com.ruoyi.project.domain.dto.WorkloadCorrectRequest request)
    {
        projectStatsService.correctAdjustWorkload(projectId, request.getDirection(),
                request.getDelta(), request.getAfterAdjust(), request.getReason());
        return success();
    }

    /**
     * 查询项目补正日志
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:list')")
    @GetMapping("/projectStats/{projectId}/correctLog")
    public AjaxResult correctLog(@PathVariable Long projectId)
    {
        List<WorkloadCorrectLog> list = projectStatsService.selectCorrectLogs(projectId);
        return success(list);
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
