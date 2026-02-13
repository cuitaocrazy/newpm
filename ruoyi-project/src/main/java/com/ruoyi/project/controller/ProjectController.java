package com.ruoyi.project.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.ProjectApproval;
import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.service.IProjectService;
import com.ruoyi.project.service.IProjectApprovalService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目管理Controller
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
@RestController
@RequestMapping("/project/project")
public class ProjectController extends BaseController
{
    @Autowired
    private IProjectService projectService;

    @Autowired
    private IProjectApprovalService projectApprovalService;

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
        // 查询原项目状态
        Project oldProject = projectService.selectProjectByProjectId(project.getProjectId());

        // 如果原状态是"已拒绝"(2)，修改后自动变为"待审核"(0)并记录重新提交
        if ("2".equals(oldProject.getApprovalStatus())) {
            project.setApprovalStatus("0");

            // 新增重新提交审核记录
            ProjectApproval resubmitRecord = new ProjectApproval();
            resubmitRecord.setProjectId(project.getProjectId());
            resubmitRecord.setApprovalStatus("0");
            resubmitRecord.setApprovalReason("重新提交审核");
            resubmitRecord.setApprovalTime(new Date());
            resubmitRecord.setCreateTime(new Date());
            projectApprovalService.insertProjectApproval(resubmitRecord);
        }

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
     * 获取用户列表（按岗位过滤）
     * @param postCode 岗位编码：pm-项目经理, scjl-市场经理, xsfzr-销售负责人
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/users")
    public AjaxResult getUsersByPost(@RequestParam(required = false) String postCode)
    {
        return success(projectService.getUsersByPost(postCode));
    }

    /**
     * 获取二级区域列表（根据一级区域）
     * @param regionDictValue 一级区域字典值
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/secondaryRegions")
    public AjaxResult getSecondaryRegions(@RequestParam String regionDictValue)
    {
        return success(projectService.getSecondaryRegionsByRegion(regionDictValue));
    }

    /**
     * 获取客户列表（支持搜索）
     * @param customerSimpleName 客户简称（模糊搜索）
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/customers")
    public AjaxResult getCustomers(@RequestParam(required = false) String customerSimpleName)
    {
        return success(projectService.getCustomers(customerSimpleName));
    }

    /**
     * 获取客户联系人列表（根据客户ID）
     * @param customerId 客户ID
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/customerContacts")
    public AjaxResult getCustomerContacts(@RequestParam Long customerId)
    {
        return success(projectService.getCustomerContacts(customerId));
    }

    /**
     * 获取部门树（三级及以下机构）
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/deptTree")
    public AjaxResult getDeptTree()
    {
        return success(projectService.getDeptTree());
    }

    /**
     * 生成项目编号
     * @param params 包含：industryCode, regionCode, provinceCode, shortName, establishedYear
     */
    @PreAuthorize("@ss.hasPermi('project:project:add')")
    @PostMapping("/generateCode")
    public AjaxResult generateProjectCode(@RequestBody Map<String, String> params)
    {
        String projectCode = projectService.generateProjectCode(
            params.get("industryCode"),
            params.get("regionCode"),
            params.get("provinceCode"),
            params.get("shortName"),
            params.get("establishedYear")
        );
        return success(projectCode);
    }

    /**
     * 查询公司收入确认列表
     */
    @PreAuthorize("@ss.hasPermi('revenue:company:list')")
    @GetMapping("/revenue/list")
    public TableDataInfo revenueList(Project project)
    {
        startPage();
        List<Project> list = projectService.selectProjectList(project);
        return getDataTable(list);
    }

    /**
     * 获取收入确认详情
     */
    @PreAuthorize("@ss.hasPermi('revenue:company:query')")
    @GetMapping("/revenue/{projectId}")
    public AjaxResult getRevenueInfo(@PathVariable("projectId") Long projectId)
    {
        return success(projectService.selectProjectByProjectId(projectId));
    }

    /**
     * 保存收入确认信息
     */
    @PreAuthorize("@ss.hasPermi('revenue:company:edit')")
    @Log(title = "收入确认", businessType = BusinessType.UPDATE)
    @PutMapping("/revenue")
    public AjaxResult updateRevenue(@RequestBody Project project)
    {
        // 验证必填字段
        if (project.getRevenueConfirmStatus() == null || project.getRevenueConfirmStatus().trim().isEmpty()) {
            return error("收入确认状态不能为空");
        }
        if (project.getRevenueConfirmYear() == null || project.getRevenueConfirmYear().trim().isEmpty()) {
            return error("收入确认年度不能为空");
        }
        if (project.getConfirmAmount() == null) {
            return error("确认金额不能为空");
        }
        if (project.getTaxRate() == null) {
            return error("税率不能为空");
        }

        // 自动计算税后金额
        BigDecimal confirmAmount = project.getConfirmAmount();
        BigDecimal taxRate = project.getTaxRate();
        BigDecimal afterTaxAmount = confirmAmount.divide(
            BigDecimal.ONE.add(taxRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)),
            2,
            RoundingMode.HALF_UP
        );
        project.setAfterTaxAmount(afterTaxAmount);

        return toAjax(projectService.updateProject(project));
    }

    /**
     * 导出收入确认列表
     */
    @PreAuthorize("@ss.hasPermi('revenue:company:export')")
    @Log(title = "收入确认", businessType = BusinessType.EXPORT)
    @PostMapping("/revenue/export")
    public void exportRevenue(HttpServletResponse response, Project project)
    {
        List<Project> list = projectService.selectProjectList(project);
        ExcelUtil<Project> util = new ExcelUtil<Project>(Project.class);
        util.exportExcel(response, list, "收入确认数据");
    }

    /**
     * 根据部门查询项目列表（用于合同关联，可排除已关联的项目）
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/listByDept")
    public AjaxResult listByDept(Long deptId, Long excludeContractId)
    {
        if (deptId == null) {
            return error("部门ID不能为空");
        }
        List<Project> list = projectService.selectProjectListByDept(deptId, excludeContractId);
        return success(list);
    }

    /**
     * 根据项目名称搜索（用于 autocomplete）
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/listByName")
    public AjaxResult listByName(@RequestParam(required = false) String projectName)
    {
        Project query = new Project();
        query.setProjectName(projectName);
        List<Project> list = projectService.selectProjectList(query);
        return success(list);
    }

    /**
     * 根据项目ID查询关联的合同信息
     */
    @PreAuthorize("@ss.hasPermi('project:project:query')")
    @GetMapping("/{projectId}/contract")
    public AjaxResult getContractByProjectId(@PathVariable Long projectId)
    {
        Contract contract = projectService.selectContractByProjectId(projectId);
        return success(contract);
    }

    /**
     * 项目搜索（轻量接口，用于 autocomplete）
     * @param projectName 项目名称（模糊搜索）
     * @return 返回精简字段：projectId, projectName, projectCode
     */
    @PreAuthorize("@ss.hasPermi('project:project:list')")
    @GetMapping("/search")
    public AjaxResult searchProjects(@RequestParam(required = false) String projectName)
    {
        return success(projectService.searchProjectsByName(projectName));
    }
}
