package com.ruoyi.project.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 团队收入确认对象 pm_team_revenue_confirmation
 * 
 * @author ruoyi
 * @date 2026-02-12
 */
public class TeamRevenueConfirmation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 团队确认ID */
    private Long teamConfirmId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 部门ID */
    @Excel(name = "部门ID")
    private Long deptId;

    /** 确认金额 */
    @Excel(name = "确认金额")
    private BigDecimal confirmAmount;

    /** 确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "确认时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date confirmTime;

    /** 确认人ID */
    @Excel(name = "确认人ID")
    private Long confirmUserId;

    /** 部门名称（关联查询字段） */
    @Excel(name = "部门名称")
    private String deptName;

    /** 确认人姓名（关联查询字段） */
    @Excel(name = "确认人姓名")
    private String confirmUserName;

    /** 删除标志 */
    private String delFlag;

    public void setTeamConfirmId(Long teamConfirmId) 
    {
        this.teamConfirmId = teamConfirmId;
    }

    public Long getTeamConfirmId() 
    {
        return teamConfirmId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setDeptId(Long deptId) 
    {
        this.deptId = deptId;
    }

    public Long getDeptId() 
    {
        return deptId;
    }

    public void setConfirmAmount(BigDecimal confirmAmount) 
    {
        this.confirmAmount = confirmAmount;
    }

    public BigDecimal getConfirmAmount() 
    {
        return confirmAmount;
    }

    public void setConfirmTime(Date confirmTime) 
    {
        this.confirmTime = confirmTime;
    }

    public Date getConfirmTime() 
    {
        return confirmTime;
    }

    public void setConfirmUserId(Long confirmUserId) 
    {
        this.confirmUserId = confirmUserId;
    }

    public Long getConfirmUserId()
    {
        return confirmUserId;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setConfirmUserName(String confirmUserName)
    {
        this.confirmUserName = confirmUserName;
    }

    public String getConfirmUserName()
    {
        return confirmUserName;
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
            .append("teamConfirmId", getTeamConfirmId())
            .append("projectId", getProjectId())
            .append("deptId", getDeptId())
            .append("confirmAmount", getConfirmAmount())
            .append("confirmTime", getConfirmTime())
            .append("confirmUserId", getConfirmUserId())
            .append("remark", getRemark())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
