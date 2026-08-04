package com.ruoyi.project.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 团队日报 - 项目聚合 VO
 * 每个实例代表一个项目在某月的团队日报数据
 */
public class TeamDailyReportVO
{
    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 是否有关联合同（有合同=带来收入） */
    private Boolean hasContract;

    /** 预算人天（estimated_workload，单位：人天） */
    private BigDecimal estimatedWorkload;

    /**
     * 项目累计人天 = ROUND(actual_workload/8, 3) + COALESCE(adjust_workload, 0)。
     * <p>全周期口径，<b>不受查询年月影响</b>——即「从项目开始到最新日报日」的累计值（018 FR-010）。
     */
    private BigDecimal actualPersonDays;

    /** 项目阶段（字典 sys_xmjd） */
    private String projectStage;

    /** 公司收入确认年度（字典 sys_ndgl） */
    private String revenueConfirmYear;

    /** 确认金额（元） */
    private BigDecimal confirmAmount;

    /** 收入确认状态（字典 sys_qrzt） */
    private String revenueConfirmStatus;

    /** 项目预算（元） */
    private BigDecimal projectBudget;

    /** 合同金额合计（元，SUM of associated contracts） */
    private BigDecimal contractAmount;

    /**
     * 项目所属机构分组名称（018 FR-013）= pm_project.project_dept 对应的 sys_dept.dept_name。
     * <p>⚠️ 这是<b>项目</b>所属部门，不要与 {@link TeamMemberDailyVO#getDeptName()}（<b>成员本人</b>
     * 所属部门）混淆——实测 38% 的成员行两者不同。
     */
    private String projectDeptName;

    /** 成员日报列表 */
    private List<TeamMemberDailyVO> members;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Boolean getHasContract() { return hasContract; }
    public void setHasContract(Boolean hasContract) { this.hasContract = hasContract; }

    public BigDecimal getEstimatedWorkload() { return estimatedWorkload; }
    public void setEstimatedWorkload(BigDecimal estimatedWorkload) { this.estimatedWorkload = estimatedWorkload; }

    public BigDecimal getActualPersonDays() { return actualPersonDays; }
    public void setActualPersonDays(BigDecimal actualPersonDays) { this.actualPersonDays = actualPersonDays; }

    public String getProjectStage() { return projectStage; }
    public void setProjectStage(String projectStage) { this.projectStage = projectStage; }

    public String getRevenueConfirmYear() { return revenueConfirmYear; }
    public void setRevenueConfirmYear(String revenueConfirmYear) { this.revenueConfirmYear = revenueConfirmYear; }

    public BigDecimal getConfirmAmount() { return confirmAmount; }
    public void setConfirmAmount(BigDecimal confirmAmount) { this.confirmAmount = confirmAmount; }

    public String getRevenueConfirmStatus() { return revenueConfirmStatus; }
    public void setRevenueConfirmStatus(String revenueConfirmStatus) { this.revenueConfirmStatus = revenueConfirmStatus; }

    public BigDecimal getProjectBudget() { return projectBudget; }
    public void setProjectBudget(BigDecimal projectBudget) { this.projectBudget = projectBudget; }

    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }

    public String getProjectDeptName() { return projectDeptName; }
    public void setProjectDeptName(String projectDeptName) { this.projectDeptName = projectDeptName; }

    public List<TeamMemberDailyVO> getMembers() { return members; }
    public void setMembers(List<TeamMemberDailyVO> members) { this.members = members; }
}
