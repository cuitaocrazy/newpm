package com.ruoyi.project.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目阶段变更对象 pm_project_stage_change
 *
 * @author ruoyi
 * @date 2026-03-03
 */
public class ProjectStageChange extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    // ===== 变更记录字段 =====

    /** 变更记录ID */
    private Long changeId;

    /** 项目ID */
    private Long projectId;

    /** 变更前阶段(字典:sys_xmjd) */
    @Excel(name = "原阶段")
    private String oldStage;

    /** 变更后阶段(字典:sys_xmjd) */
    @Excel(name = "新阶段")
    private String newStage;

    /** 变更原因 */
    @Excel(name = "变更原因")
    private String changeReason;

    /** 删除标志 */
    private String delFlag;

    // ===== 项目信息字段（来自 pm_project，非持久化）=====

    /** 项目名称 */
    @Excel(name = "项目名称")
    private String projectName;

    /** 收入确认年度 */
    @Excel(name = "收入确认年度")
    private String revenueConfirmYear;

    /** 确认金额（含税）*/
    @Excel(name = "确认金额")
    private BigDecimal confirmAmount;

    /** 收入确认状态（字典:sys_srqrzt）*/
    @Excel(name = "确认状态")
    private String revenueConfirmStatus;

    /** 项目部门 */
    @Excel(name = "项目部门")
    private String projectDept;

    /** 当前阶段（来自 pm_project.project_stage，字典:sys_xmjd）*/
    @Excel(name = "当前阶段")
    private String projectStage;

    // ===== 合同字段（来自 pm_contract，非持久化）=====

    /** 合同状态（字典:sys_htzt）*/
    @Excel(name = "合同状态")
    private String contractStatus;

    /** 合同金额（含税，来自 pm_contract.contract_amount）*/
    @Excel(name = "合同金额")
    private BigDecimal contractAmount;

    // ===== 最近变更人/时间（来自最新 pm_project_stage_change，非持久化）=====

    /** 变更人 */
    @Excel(name = "变更人")
    private String changeBy;

    /** 变更时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "变更时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date changeTime;

    // ===== 批量操作辅助字段 =====

    /** 批量变更时的项目ID列表 */
    private Long[] projectIds;

    // ===== Getters & Setters =====

    public Long getChangeId() { return changeId; }
    public void setChangeId(Long changeId) { this.changeId = changeId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getOldStage() { return oldStage; }
    public void setOldStage(String oldStage) { this.oldStage = oldStage; }

    public String getNewStage() { return newStage; }
    public void setNewStage(String newStage) { this.newStage = newStage; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getRevenueConfirmYear() { return revenueConfirmYear; }
    public void setRevenueConfirmYear(String revenueConfirmYear) { this.revenueConfirmYear = revenueConfirmYear; }

    public BigDecimal getConfirmAmount() { return confirmAmount; }
    public void setConfirmAmount(BigDecimal confirmAmount) { this.confirmAmount = confirmAmount; }

    public String getRevenueConfirmStatus() { return revenueConfirmStatus; }
    public void setRevenueConfirmStatus(String revenueConfirmStatus) { this.revenueConfirmStatus = revenueConfirmStatus; }

    public String getProjectDept() { return projectDept; }
    public void setProjectDept(String projectDept) { this.projectDept = projectDept; }

    public String getProjectStage() { return projectStage; }
    public void setProjectStage(String projectStage) { this.projectStage = projectStage; }

    public String getContractStatus() { return contractStatus; }
    public void setContractStatus(String contractStatus) { this.contractStatus = contractStatus; }

    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }

    public String getChangeBy() { return changeBy; }
    public void setChangeBy(String changeBy) { this.changeBy = changeBy; }

    public Date getChangeTime() { return changeTime; }
    public void setChangeTime(Date changeTime) { this.changeTime = changeTime; }

    public Long[] getProjectIds() { return projectIds; }
    public void setProjectIds(Long[] projectIds) { this.projectIds = projectIds; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("changeId", getChangeId())
            .append("projectId", getProjectId())
            .append("projectName", getProjectName())
            .append("oldStage", getOldStage())
            .append("newStage", getNewStage())
            .append("changeReason", getChangeReason())
            .append("projectStage", getProjectStage())
            .append("changeBy", getChangeBy())
            .append("changeTime", getChangeTime())
            .toString();
    }
}
