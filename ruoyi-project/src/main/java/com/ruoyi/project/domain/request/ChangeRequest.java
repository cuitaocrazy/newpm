package com.ruoyi.project.domain.request;

import jakarta.validation.constraints.NotNull;

/**
 * 变更项目经理请求对象
 *
 * @author ruoyi
 */
public class ChangeRequest {

    /** 项目ID */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /** 新项目经理ID */
    @NotNull(message = "新项目经理ID不能为空")
    private Long newManagerId;

    /** 变更原因 */
    @NotNull(message = "变更原因不能为空")
    private String changeReason;

    // Getters and Setters
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getNewManagerId() { return newManagerId; }
    public void setNewManagerId(Long newManagerId) { this.newManagerId = newManagerId; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}
