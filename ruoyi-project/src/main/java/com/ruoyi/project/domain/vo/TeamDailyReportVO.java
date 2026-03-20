package com.ruoyi.project.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 团队日报 - 项目聚合 VO
 * 每个实例代表一个项目在某月的团队日报数据
 */
public class TeamDailyReportVO
{
    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 是否有关联合同（有合同=带来收入） */
    private Boolean hasContract;

    /** 预算人天（estimated_workload，单位：人天） */
    private BigDecimal estimatedWorkload;

    /** 实际人天 = ROUND(actual_workload/8, 3) + COALESCE(adjust_workload, 0) */
    private BigDecimal actualPersonDays;

    /** 成员日报列表 */
    private List<TeamMemberDailyVO> members;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Boolean getHasContract() { return hasContract; }
    public void setHasContract(Boolean hasContract) { this.hasContract = hasContract; }

    public BigDecimal getEstimatedWorkload() { return estimatedWorkload; }
    public void setEstimatedWorkload(BigDecimal estimatedWorkload) { this.estimatedWorkload = estimatedWorkload; }

    public BigDecimal getActualPersonDays() { return actualPersonDays; }
    public void setActualPersonDays(BigDecimal actualPersonDays) { this.actualPersonDays = actualPersonDays; }

    public List<TeamMemberDailyVO> getMembers() { return members; }
    public void setMembers(List<TeamMemberDailyVO> members) { this.members = members; }
}
