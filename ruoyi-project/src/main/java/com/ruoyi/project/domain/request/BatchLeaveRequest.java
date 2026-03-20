package com.ruoyi.project.domain.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * 批量填写假期请求对象
 */
public class BatchLeaveRequest {

    /** 假期类型（sys_rbtype 字典值，不能为 work） */
    @NotBlank(message = "假期类型不能为空")
    private String entryType;

    /** 日期范围开始（inclusive），格式 yyyy-MM-dd */
    @NotBlank(message = "开始日期不能为空")
    private String startDate;

    /** 日期范围结束（inclusive），格式 yyyy-MM-dd */
    @NotBlank(message = "结束日期不能为空")
    private String endDate;

    /** 每日假期时长（小时），默认 8 */
    private BigDecimal leaveHoursPerDay = BigDecimal.valueOf(8);

    /** 冲突处理策略：skip（跳过，默认）| overwrite（覆盖） */
    private String conflictStrategy = "skip";

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public BigDecimal getLeaveHoursPerDay() { return leaveHoursPerDay; }
    public void setLeaveHoursPerDay(BigDecimal leaveHoursPerDay) { this.leaveHoursPerDay = leaveHoursPerDay; }

    public String getConflictStrategy() { return conflictStrategy; }
    public void setConflictStrategy(String conflictStrategy) { this.conflictStrategy = conflictStrategy; }
}
