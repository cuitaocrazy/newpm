package com.ruoyi.project.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工作日报明细对象 pm_daily_report_detail
 *
 * @author ruoyi
 * @date 2026-02-26
 */
public class DailyReportDetail extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 明细主键ID */
    private Long detailId;

    /** 日报ID */
    private Long reportId;

    /** 项目ID */
    private Long projectId;

    /** 项目阶段 */
    @Excel(name = "项目阶段", dictType = "sys_xmjd")
    private String projectStage;

    /** 工时(小时) */
    @Excel(name = "工时(小时)")
    private BigDecimal workHours;

    /** 工作内容 */
    @Excel(name = "工作内容")
    private String workContent;

    /** 条目类型: work=项目工时 / leave=请假 / comp=倒休 / annual=年假 */
    private String entryType;

    /** 假期时长(小时)，entryType非work时使用 */
    private BigDecimal leaveHours;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    /** 项目名称（扩展字段，用于列表展示） */
    @Excel(name = "项目名称")
    private String projectName;

    /** 项目编号（扩展字段，用于列表展示） */
    @Excel(name = "项目编号")
    private String projectCode;

    /** 项目阶段名称（扩展字段，用于列表展示） */
    private String projectStageName;

    /** 预计人天（扩展字段，来自pm_project.estimated_workload） */
    private BigDecimal estimatedWorkload;

    /** 已花人天（扩展字段，= actual_workload/8 + adjust_workload） */
    private BigDecimal actualWorkload;

    /** 收入确认年度（扩展字段，来自pm_project.revenue_confirm_year） */
    private String revenueConfirmYear;

    /** 子项目ID */
    private Long subProjectId;

    /** 工作任务类别（字典 sys_gzlb） */
    private String workCategory;

    /** 子项目名称（扩展字段，来自pm_project.project_name） */
    private String subProjectName;

    /** 子任务阶段（扩展字段，非DB列） */
    private String subProjectStage;

    /** 子任务负责人ID（扩展字段，非DB列） */
    private Long subProjectManagerId;

    /** 子任务负责人姓名（扩展字段，非DB列） */
    private String subProjectManagerName;

    /** 子任务编号（扩展字段，非DB列） */
    private String subProjectTaskCode;

    /** 子任务批次号（扩展字段，非DB列） */
    private String subProjectBatchNo;

    /** 项目经理姓名（扩展字段，非DB列） */
    private String projectManagerName;

    public void setSubProjectBatchNo(String subProjectBatchNo) { this.subProjectBatchNo = subProjectBatchNo; }
    public String getSubProjectBatchNo() { return subProjectBatchNo; }

    public void setSubProjectId(Long subProjectId) { this.subProjectId = subProjectId; }
    public Long getSubProjectId() { return subProjectId; }

    public void setWorkCategory(String workCategory) { this.workCategory = workCategory; }
    public String getWorkCategory() { return workCategory; }

    public void setSubProjectName(String subProjectName) { this.subProjectName = subProjectName; }
    public String getSubProjectName() { return subProjectName; }

    public void setSubProjectStage(String subProjectStage) { this.subProjectStage = subProjectStage; }
    public String getSubProjectStage() { return subProjectStage; }

    public void setSubProjectManagerId(Long subProjectManagerId) { this.subProjectManagerId = subProjectManagerId; }
    public Long getSubProjectManagerId() { return subProjectManagerId; }

    public void setSubProjectManagerName(String subProjectManagerName) { this.subProjectManagerName = subProjectManagerName; }
    public String getSubProjectManagerName() { return subProjectManagerName; }

    public void setSubProjectTaskCode(String subProjectTaskCode) { this.subProjectTaskCode = subProjectTaskCode; }
    public String getSubProjectTaskCode() { return subProjectTaskCode; }

    public void setProjectManagerName(String projectManagerName) { this.projectManagerName = projectManagerName; }
    public String getProjectManagerName() { return projectManagerName; }

    public void setDetailId(Long detailId)
    {
        this.detailId = detailId;
    }

    public Long getDetailId()
    {
        return detailId;
    }

    public void setReportId(Long reportId)
    {
        this.reportId = reportId;
    }

    public Long getReportId()
    {
        return reportId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectStage(String projectStage)
    {
        this.projectStage = projectStage;
    }

    public String getProjectStage()
    {
        return projectStage;
    }

    public void setWorkHours(BigDecimal workHours)
    {
        this.workHours = workHours;
    }

    public BigDecimal getWorkHours()
    {
        return workHours;
    }

    public void setWorkContent(String workContent)
    {
        this.workContent = workContent;
    }

    public String getWorkContent()
    {
        return workContent;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectCode(String projectCode)
    {
        this.projectCode = projectCode;
    }

    public String getProjectCode()
    {
        return projectCode;
    }

    public void setProjectStageName(String projectStageName)
    {
        this.projectStageName = projectStageName;
    }

    public String getProjectStageName()
    {
        return projectStageName;
    }

    public void setEstimatedWorkload(BigDecimal estimatedWorkload) { this.estimatedWorkload = estimatedWorkload; }
    public BigDecimal getEstimatedWorkload() { return estimatedWorkload; }

    public void setActualWorkload(BigDecimal actualWorkload) { this.actualWorkload = actualWorkload; }
    public BigDecimal getActualWorkload() { return actualWorkload; }

    public void setRevenueConfirmYear(String revenueConfirmYear) { this.revenueConfirmYear = revenueConfirmYear; }
    public String getRevenueConfirmYear() { return revenueConfirmYear; }

    public void setEntryType(String entryType) { this.entryType = entryType; }
    public String getEntryType() { return entryType; }

    public void setLeaveHours(BigDecimal leaveHours) { this.leaveHours = leaveHours; }
    public BigDecimal getLeaveHours() { return leaveHours; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("detailId", getDetailId())
            .append("reportId", getReportId())
            .append("projectId", getProjectId())
            .append("projectStage", getProjectStage())
            .append("workHours", getWorkHours())
            .append("workContent", getWorkContent())
            .append("entryType", getEntryType())
            .append("leaveHours", getLeaveHours())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
