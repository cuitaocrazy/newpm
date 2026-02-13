package com.ruoyi.project.domain.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 批量变更项目经理请求对象
 *
 * @author ruoyi
 */
public class BatchChangeRequest {

    /** 项目ID数组 */
    @NotEmpty(message = "项目ID数组不能为空")
    private Long[] projectIds;

    /** 新项目经理ID */
    @NotNull(message = "新项目经理ID不能为空")
    private Long newManagerId;

    /** 变更原因 */
    @NotNull(message = "变更原因不能为空")
    private String changeReason;

    // Getters and Setters
    public Long[] getProjectIds() { return projectIds; }
    public void setProjectIds(Long[] projectIds) { this.projectIds = projectIds; }

    public Long getNewManagerId() { return newManagerId; }
    public void setNewManagerId(Long newManagerId) { this.newManagerId = newManagerId; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}
