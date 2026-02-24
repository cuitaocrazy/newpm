package com.ruoyi.project.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 项目经理变更视图对象（以项目为中心）
 *
 * @author ruoyi
 * @date 2026-02-14
 */
public class ProjectManagerChangeVO
{
    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 项目编号 */
    private String projectCode;

    /** 立项时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date projectCreateTime;

    /** 当前项目经理ID */
    private Long currentManagerId;

    /** 当前项目经理姓名 */
    private String currentManagerName;

    /** 原项目经理ID（最新变更记录） */
    private Long oldManagerId;

    /** 原项目经理姓名（最新变更记录） */
    private String oldManagerName;

    /** 变更原因（最新变更记录） */
    private String changeReason;

    /** 变更人（最新变更记录） */
    private String changeBy;

    /** 变更人姓名（最新变更记录） */
    private String changeByName;

    /** 变更时间（最新变更记录） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date changeTime;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public Date getProjectCreateTime() {
        return projectCreateTime;
    }

    public void setProjectCreateTime(Date projectCreateTime) {
        this.projectCreateTime = projectCreateTime;
    }

    public Long getCurrentManagerId() {
        return currentManagerId;
    }

    public void setCurrentManagerId(Long currentManagerId) {
        this.currentManagerId = currentManagerId;
    }

    public String getCurrentManagerName() {
        return currentManagerName;
    }

    public void setCurrentManagerName(String currentManagerName) {
        this.currentManagerName = currentManagerName;
    }

    public Long getOldManagerId() {
        return oldManagerId;
    }

    public void setOldManagerId(Long oldManagerId) {
        this.oldManagerId = oldManagerId;
    }

    public String getOldManagerName() {
        return oldManagerName;
    }

    public void setOldManagerName(String oldManagerName) {
        this.oldManagerName = oldManagerName;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getChangeBy() {
        return changeBy;
    }

    public void setChangeBy(String changeBy) {
        this.changeBy = changeBy;
    }

    public String getChangeByName() {
        return changeByName;
    }

    public void setChangeByName(String changeByName) {
        this.changeByName = changeByName;
    }

    public Date getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(Date changeTime) {
        this.changeTime = changeTime;
    }
}
