package com.ruoyi.project.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 工作量补正日志 pm_workload_correct_log
 */
public class WorkloadCorrectLog
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 项目ID */
    private Long projectId;

    /** 项目名称（联查，非持久化） */
    private String projectName;

    /** 调整方向(0=增加,1=减少) */
    private Integer direction;

    /** 调整人天数 */
    private BigDecimal delta;

    /** 调整前调整人天值 */
    private BigDecimal beforeAdjust;

    /** 调整后调整人天值 */
    private BigDecimal afterAdjust;

    /** 补正理由 */
    private String reason;

    /** 创建者 */
    private String createBy;

    /** 操作人昵称（联查，非持久化） */
    private String createByName;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Integer getDirection() { return direction; }
    public void setDirection(Integer direction) { this.direction = direction; }

    public BigDecimal getDelta() { return delta; }
    public void setDelta(BigDecimal delta) { this.delta = delta; }

    public BigDecimal getBeforeAdjust() { return beforeAdjust; }
    public void setBeforeAdjust(BigDecimal beforeAdjust) { this.beforeAdjust = beforeAdjust; }

    public BigDecimal getAfterAdjust() { return afterAdjust; }
    public void setAfterAdjust(BigDecimal afterAdjust) { this.afterAdjust = afterAdjust; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public String getCreateByName() { return createByName; }
    public void setCreateByName(String createByName) { this.createByName = createByName; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
