package com.ruoyi.project.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 项目人天统计 VO
 */
public class ProjectStatsVO
{
    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 项目经理姓名 */
    private String projectManagerName;

    /** 预估工作量(人天) */
    private BigDecimal estimatedWorkload;

    /** 调整工作量(人天) */
    private BigDecimal adjustWorkload;

    /** 日报人天合计（纯日报 SUM(work_hours)/8，所有阶段之和） */
    private BigDecimal totalActualDays;

    /** 实际人天（日报人天合计 + 调整人天） */
    private BigDecimal actualDays;

    /** 阶段明细（每个阶段的日报人天） */
    private List<StageStatsVO> stages;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectManagerName() { return projectManagerName; }
    public void setProjectManagerName(String projectManagerName) { this.projectManagerName = projectManagerName; }

    public BigDecimal getEstimatedWorkload() { return estimatedWorkload; }
    public void setEstimatedWorkload(BigDecimal estimatedWorkload) { this.estimatedWorkload = estimatedWorkload; }

    public BigDecimal getAdjustWorkload() { return adjustWorkload; }
    public void setAdjustWorkload(BigDecimal adjustWorkload) { this.adjustWorkload = adjustWorkload; }

    public BigDecimal getTotalActualDays() { return totalActualDays; }
    public void setTotalActualDays(BigDecimal totalActualDays) { this.totalActualDays = totalActualDays; }

    public BigDecimal getActualDays() { return actualDays; }
    public void setActualDays(BigDecimal actualDays) { this.actualDays = actualDays; }

    public List<StageStatsVO> getStages() { return stages; }
    public void setStages(List<StageStatsVO> stages) { this.stages = stages; }
}
