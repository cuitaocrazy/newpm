package com.ruoyi.project.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
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
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.service.IProjectService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目管理Controller
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@RestController
@RequestMapping("/project/project")
public class ProjectController extends BaseController
{
    @Autowired
    private IProjectService projectService;

    /**
     * 查询项目管理列表
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/list")
    public TableDataInfo list(Project project)
    {
        startPage();
        List<Project> list = projectService.selectProjectList(project);
        return getDataTable(list);
    }

    /**
     * 导出项目管理列表
     */
    @PreAuthorize("@ss.hasPermi('project:project:export')")
    @Log(title = "项目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Project project)
    {
        List<Project> list = projectService.selectProjectList(project);
        ExcelUtil<Project> util = new ExcelUtil<Project>(Project.class);
        util.exportExcel(response, list, "项目管理数据");
    }

    /**
     * 获取项目管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:project:query')")
    @GetMapping(value = "/{projectId}")
    public AjaxResult getInfo(@PathVariable("projectId") Long projectId)
    {
        return success(projectService.selectProjectByProjectId(projectId));
    }

    /**
     * 新增项目管理
     */
    @PreAuthorize("@ss.hasPermi('project:project:add')")
    @Log(title = "项目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Project project)
    {
        return toAjax(projectService.insertProject(project));
    }

    /**
     * 修改项目管理
     */
    @PreAuthorize("@ss.hasPermi('project:project:edit')")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Project project)
    {
        return toAjax(projectService.updateProject(project));
    }

    /**
     * 删除项目管理
     */
    @PreAuthorize("@ss.hasPermi('project:project:remove')")
    @Log(title = "项目管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{projectIds}")
    public AjaxResult remove(@PathVariable Long[] projectIds)
    {
        return toAjax(projectService.deleteProjectByProjectIds(projectIds));
    }

    /**
     * 根据部门查询项目列表（用于下拉选择）
     * @param deptId 部门ID
     * @param excludeContractId 排除已关联此合同的项目（编辑时使用）
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/listByDept")
    public AjaxResult listByDept(Long deptId, Long excludeContractId)
    {
        List<Project> list = projectService.selectProjectListByDept(deptId, excludeContractId);
        return success(list);
    }

    /**
     * 获取项目名称列表（用于智能提示）
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/nameList")
    public AjaxResult nameList(String projectName)
    {
        List<String> list = projectService.selectProjectNameList(projectName);
        return success(list);
    }

    /**
     * 获取项目编号列表（用于智能提示）
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/codeList")
    public AjaxResult codeList(String projectCode)
    {
        List<String> list = projectService.selectProjectCodeList(projectCode);
        return success(list);
    }

    /**
     * 获取项目金额汇总
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/summary")
    public AjaxResult summary(Project project)
    {
        return success(projectService.selectProjectSummary(project));
    }

    // ========================================
    // 公司收入确认相关接口
    // ========================================

    /**
     * 查询公司收入确认列表
     */
    @PreAuthorize("@ss.hasPermi('revenue:company:query')")
    @GetMapping("/revenueList")
    public TableDataInfo revenueList(Project project)
    {
        startPage();
        List<Project> list = projectService.selectProjectList(project);
        return getDataTable(list);
    }

    /**
     * 获取公司收入确认详情
     */
    @PreAuthorize("@ss.hasPermi('revenue:company:view')")
    @GetMapping("/revenue/{projectId}")
    public AjaxResult getRevenueInfo(@PathVariable("projectId") Long projectId)
    {
        return success(projectService.selectProjectByProjectId(projectId));
    }

    /**
     * 更新公司收入确认信息
     */
    @PreAuthorize("@ss.hasPermi('revenue:company:edit')")
    @Log(title = "公司收入确认", businessType = BusinessType.UPDATE)
    @PutMapping("/revenueConfirm")
    public AjaxResult updateRevenueConfirm(@RequestBody Project project)
    {
        // 自动设置收入确认状态为已确认
        project.setRevenueConfirmStatus("1");

        // 自动设置确认人为当前登录用户
        project.setCompanyRevenueConfirmedBy(getUserId());

        // 自动设置确认时间为当前时间
        project.setCompanyRevenueConfirmedTime(new Date());

        // 自动计算税后金额：税后金额 = 确认金额 / (1 + 税率/100)
        if (project.getConfirmAmount() != null && project.getTaxRate() != null) {
            BigDecimal confirmAmount = project.getConfirmAmount();
            BigDecimal taxRate = project.getTaxRate();
            BigDecimal afterTaxAmount = confirmAmount.divide(
                BigDecimal.ONE.add(taxRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)),
                2,
                RoundingMode.HALF_UP
            );
            project.setAfterTaxAmount(afterTaxAmount);
        }

        return toAjax(projectService.updateProject(project));
    }

    /**
     * 导出公司收入确认列表
     */
    @PreAuthorize("@ss.hasPermi('revenue:company:export')")
    @Log(title = "公司收入确认", businessType = BusinessType.EXPORT)
    @PostMapping("/revenueExport")
    public void revenueExport(HttpServletResponse response, Project project)
    {
        List<Project> list = projectService.selectProjectList(project);
        ExcelUtil<Project> util = new ExcelUtil<Project>(Project.class);
        util.exportExcel(response, list, "公司收入确认数据");
    }
}
