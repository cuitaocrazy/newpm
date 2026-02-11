package com.ruoyi.project.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 立项审核对象 pm_project_approval
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
public class ProjectApproval extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 审核主键ID */
    private Long approvalId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 审核状态(0-待审核、1-通过、2-不通过) */
    @Excel(name = "审核状态(0-待审核、1-通过、2-不通过)")
    private String approvalStatus;

    /** 审核原因/意见 */
    @Excel(name = "审核原因/意见")
    private String approvalReason;

    /** 审核人ID */
    @Excel(name = "审核人ID")
    private Long approverId;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "审核时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date approvalTime;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    public void setApprovalId(Long approvalId) 
    {
        this.approvalId = approvalId;
    }

    public Long getApprovalId() 
    {
        return approvalId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setApprovalStatus(String approvalStatus) 
    {
        this.approvalStatus = approvalStatus;
    }

    public String getApprovalStatus() 
    {
        return approvalStatus;
    }

    public void setApprovalReason(String approvalReason) 
    {
        this.approvalReason = approvalReason;
    }

    public String getApprovalReason() 
    {
        return approvalReason;
    }

    public void setApproverId(Long approverId) 
    {
        this.approverId = approverId;
    }

    public Long getApproverId() 
    {
        return approverId;
    }

    public void setApprovalTime(Date approvalTime) 
    {
        this.approvalTime = approvalTime;
    }

    public Date getApprovalTime() 
    {
        return approvalTime;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("approvalId", getApprovalId())
            .append("projectId", getProjectId())
            .append("approvalStatus", getApprovalStatus())
            .append("approvalReason", getApprovalReason())
            .append("approverId", getApproverId())
            .append("approvalTime", getApprovalTime())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
