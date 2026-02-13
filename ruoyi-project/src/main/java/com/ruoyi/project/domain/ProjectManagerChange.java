package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目经理变更对象 pm_project_manager_change
 * 
 * @author ruoyi
 * @date 2026-02-14
 */
public class ProjectManagerChange extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 变更主键ID */
    @Excel(name = "变更主键ID")
    private Long changeId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 原项目经理ID */
    @Excel(name = "原项目经理ID")
    private Long oldManagerId;

    /** 新项目经理ID */
    @Excel(name = "新项目经理ID")
    private Long newManagerId;

    /** 变更原因 */
    @Excel(name = "变更原因")
    private String changeReason;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    /** 项目名称（关联查询） */
    private String projectName;

    /** 项目编号（关联查询） */
    private String projectCode;

    /** 原项目经理姓名（关联查询） */
    private String oldManagerName;

    /** 新项目经理姓名（关联查询） */
    private String newManagerName;

    public void setChangeId(Long changeId) 
    {
        this.changeId = changeId;
    }

    public Long getChangeId() 
    {
        return changeId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setOldManagerId(Long oldManagerId) 
    {
        this.oldManagerId = oldManagerId;
    }

    public Long getOldManagerId() 
    {
        return oldManagerId;
    }

    public void setNewManagerId(Long newManagerId) 
    {
        this.newManagerId = newManagerId;
    }

    public Long getNewManagerId() 
    {
        return newManagerId;
    }

    public void setChangeReason(String changeReason) 
    {
        this.changeReason = changeReason;
    }

    public String getChangeReason() 
    {
        return changeReason;
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

    public void setOldManagerName(String oldManagerName)
    {
        this.oldManagerName = oldManagerName;
    }

    public String getOldManagerName()
    {
        return oldManagerName;
    }

    public void setNewManagerName(String newManagerName)
    {
        this.newManagerName = newManagerName;
    }

    public String getNewManagerName()
    {
        return newManagerName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("changeId", getChangeId())
            .append("projectId", getProjectId())
            .append("oldManagerId", getOldManagerId())
            .append("newManagerId", getNewManagerId())
            .append("changeReason", getChangeReason())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
