package com.ruoyi.project.domain.vo;

import java.math.BigDecimal;

/**
 * 项目阶段人天统计 VO
 */
public class StageStatsVO
{
    /** 项目阶段（字典值，sys_xmjd） */
    private String projectStage;

    /** 该阶段日报人天（SUM(work_hours)/8） */
    private BigDecimal stageDays;

    public String getProjectStage() { return projectStage; }
    public void setProjectStage(String projectStage) { this.projectStage = projectStage; }

    public BigDecimal getStageDays() { return stageDays; }
    public void setStageDays(BigDecimal stageDays) { this.stageDays = stageDays; }
}
