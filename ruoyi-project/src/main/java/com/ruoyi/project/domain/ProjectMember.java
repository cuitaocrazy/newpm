package com.ruoyi.project.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目成员对象 pm_project_member
 *
 * @author ruoyi
 * @date 2026-02-26
 */
public class ProjectMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 成员主键ID */
    private Long memberId;

    /** 项目ID */
    private Long projectId;

    /** 用户ID */
    private Long userId;

    /** 加入日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "加入日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date joinDate;

    /** 离开日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "离开日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date leaveDate;

    /** 是否在项目中(1是 0否) */
    private String isActive;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    /** 用户名（关联查询） */
    @Excel(name = "用户名")
    private String userName;

    /** 用户昵称（关联查询） */
    @Excel(name = "用户昵称")
    private String nickName;

    /** 部门名称（关联查询） */
    @Excel(name = "部门名称")
    private String deptName;

    /** 项目名称（查询条件，非DB字段） */
    private String projectName;

    /** 部门ID（查询条件，非DB字段） */
    private Long deptId;

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public Long getMemberId()
    {
        return memberId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setJoinDate(Date joinDate)
    {
        this.joinDate = joinDate;
    }

    public Date getJoinDate()
    {
        return joinDate;
    }

    public void setLeaveDate(Date leaveDate)
    {
        this.leaveDate = leaveDate;
    }

    public Date getLeaveDate()
    {
        return leaveDate;
    }

    public void setIsActive(String isActive)
    {
        this.isActive = isActive;
    }

    public String getIsActive()
    {
        return isActive;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("memberId", getMemberId())
            .append("projectId", getProjectId())
            .append("userId", getUserId())
            .append("joinDate", getJoinDate())
            .append("leaveDate", getLeaveDate())
            .append("isActive", getIsActive())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
