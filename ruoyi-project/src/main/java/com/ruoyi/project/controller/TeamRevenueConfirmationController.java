package com.ruoyi.project.controller;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
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
import com.ruoyi.project.domain.TeamRevenueConfirmation;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.service.ITeamRevenueConfirmationService;
import com.ruoyi.project.service.IProjectService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 团队收入确认Controller
 *
 * @author ruoyi
 * @date 2026-02-12
 */
@RestController
@RequestMapping("/revenue/team")
public class TeamRevenueConfirmationController extends BaseController
{
    @Autowired
    private ITeamRevenueConfirmationService teamRevenueConfirmationService;

    @Autowired
    private IProjectService projectService;

    /**
     * 查询团队收入确认列表（主表从项目表查询）
     */
    @PreAuthorize("@ss.hasPermi('revenue:team:list')")
    @GetMapping("/list")
    public TableDataInfo list(Project project)
    {
        startPage();
        // 从项目表查询，与公司收入确认查询逻辑一致
        List<Project> list = projectService.selectProjectList(project);
        return getDataTable(list);
    }

    /**
     * 获取团队收入确认详细信息（项目详情 + 团队确认明细列表）
     */
    @PreAuthorize("@ss.hasPermi('revenue:team:query')")
    @GetMapping(value = "/{projectId}")
    public AjaxResult getInfo(@PathVariable("projectId") Long projectId)
    {
        // 获取项目详情
        Project project = projectService.selectProjectByProjectId(projectId);

        // 获取该项目的团队确认明细列表
        TeamRevenueConfirmation query = new TeamRevenueConfirmation();
        query.setProjectId(projectId);
        List<TeamRevenueConfirmation> detailList = teamRevenueConfirmationService.selectTeamRevenueConfirmationList(query);

        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("project", project);
        result.put("detailList", detailList);

        return success(result);
    }

    /**
     * 新增团队收入确认（批量保存明细）
     */
    @PreAuthorize("@ss.hasPermi('revenue:team:add')")
    @Log(title = "团队收入确认", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> params)
    {
        // 获取项目ID
        if (params.get("projectId") == null) {
            return error("项目ID不能为空");
        }
        Long projectId = Long.valueOf(params.get("projectId").toString());

        // 获取明细列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detailList = (List<Map<String, Object>>) params.get("detailList");

        if (detailList == null || detailList.isEmpty()) {
            return error("请至少添加一条确认明细");
        }

        // 批量保存明细
        int successCount = 0;
        for (Map<String, Object> detail : detailList) {
            // 验证必填字段
            if (detail.get("deptId") == null) {
                return error("部门不能为空");
            }
            if (detail.get("confirmAmount") == null) {
                return error("确认金额不能为空");
            }
            if (detail.get("confirmUserId") == null) {
                return error("确认人不能为空");
            }

            TeamRevenueConfirmation teamConfirm = new TeamRevenueConfirmation();
            teamConfirm.setProjectId(projectId);
            teamConfirm.setDeptId(Long.valueOf(detail.get("deptId").toString()));
            teamConfirm.setConfirmAmount(new BigDecimal(detail.get("confirmAmount").toString()));
            teamConfirm.setConfirmUserId(Long.valueOf(detail.get("confirmUserId").toString()));
            teamConfirm.setConfirmTime(new Date()); // 设置确认时间为当前时间
            if (detail.get("remark") != null) {
                teamConfirm.setRemark(detail.get("remark").toString());
            }

            successCount += teamRevenueConfirmationService.insertTeamRevenueConfirmation(teamConfirm);
        }

        return toAjax(successCount);
    }

    /**
     * 修改团队收入确认（批量更新明细）
     */
    @PreAuthorize("@ss.hasPermi('revenue:team:edit')")
    @Log(title = "团队收入确认", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> params)
    {
        // 获取项目ID
        if (params.get("projectId") == null) {
            return error("项目ID不能为空");
        }
        Long projectId = Long.valueOf(params.get("projectId").toString());

        // 获取明细列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detailList = (List<Map<String, Object>>) params.get("detailList");

        if (detailList == null || detailList.isEmpty()) {
            return error("请至少添加一条确认明细");
        }

        // 先删除该项目的所有明细
        TeamRevenueConfirmation query = new TeamRevenueConfirmation();
        query.setProjectId(projectId);
        List<TeamRevenueConfirmation> existingList = teamRevenueConfirmationService.selectTeamRevenueConfirmationList(query);
        if (existingList != null && !existingList.isEmpty()) {
            Long[] ids = existingList.stream()
                .map(TeamRevenueConfirmation::getTeamConfirmId)
                .toArray(Long[]::new);
            teamRevenueConfirmationService.deleteTeamRevenueConfirmationByTeamConfirmIds(ids);
        }

        // 批量保存新的明细
        int successCount = 0;
        for (Map<String, Object> detail : detailList) {
            // 验证必填字段
            if (detail.get("deptId") == null) {
                return error("部门不能为空");
            }
            if (detail.get("confirmAmount") == null) {
                return error("确认金额不能为空");
            }
            if (detail.get("confirmUserId") == null) {
                return error("确认人不能为空");
            }

            TeamRevenueConfirmation teamConfirm = new TeamRevenueConfirmation();
            teamConfirm.setProjectId(projectId);
            teamConfirm.setDeptId(Long.valueOf(detail.get("deptId").toString()));
            teamConfirm.setConfirmAmount(new BigDecimal(detail.get("confirmAmount").toString()));
            teamConfirm.setConfirmUserId(Long.valueOf(detail.get("confirmUserId").toString()));
            teamConfirm.setConfirmTime(new Date()); // 设置确认时间为当前时间
            if (detail.get("remark") != null) {
                teamConfirm.setRemark(detail.get("remark").toString());
            }

            successCount += teamRevenueConfirmationService.insertTeamRevenueConfirmation(teamConfirm);
        }

        return toAjax(successCount);
    }

    /**
     * 删除团队收入确认
     */
    @PreAuthorize("@ss.hasPermi('revenue:team:remove')")
    @Log(title = "团队收入确认", businessType = BusinessType.DELETE)
	@DeleteMapping("/{teamConfirmIds}")
    public AjaxResult remove(@PathVariable Long[] teamConfirmIds)
    {
        return toAjax(teamRevenueConfirmationService.deleteTeamRevenueConfirmationByTeamConfirmIds(teamConfirmIds));
    }

    /**
     * 导出团队收入确认列表
     */
    @PreAuthorize("@ss.hasPermi('revenue:team:export')")
    @Log(title = "团队收入确认", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Project project)
    {
        List<Project> list = projectService.selectProjectList(project);
        ExcelUtil<Project> util = new ExcelUtil<Project>(Project.class);
        util.exportExcel(response, list, "团队收入确认数据");
    }

    /**
     * 根据项目ID获取项目基本信息（用于新增页面自动带出字段）
     */
    @PreAuthorize("@ss.hasPermi('revenue:team:query')")
    @GetMapping("/project/{projectId}")
    public AjaxResult getProjectInfo(@PathVariable("projectId") Long projectId)
    {
        Project project = projectService.selectProjectByProjectId(projectId);
        if (project == null) {
            return error("项目不存在");
        }

        // 返回需要的字段
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", project.getProjectId());
        result.put("projectName", project.getProjectName());
        result.put("projectBudget", project.getProjectBudget());
        result.put("confirmAmount", project.getConfirmAmount());
        result.put("revenueConfirmYear", project.getRevenueConfirmYear());

        return success(result);
    }
}
