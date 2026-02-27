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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("detailId", getDetailId())
            .append("reportId", getReportId())
            .append("projectId", getProjectId())
            .append("projectStage", getProjectStage())
            .append("workHours", getWorkHours())
            .append("workContent", getWorkContent())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
