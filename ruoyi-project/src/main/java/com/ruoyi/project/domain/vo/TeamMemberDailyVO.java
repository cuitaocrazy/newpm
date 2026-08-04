package com.ruoyi.project.domain.vo;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 团队日报 - 成员行 VO
 * 每个实例代表某成员在某项目某月的日报数据
 */
public class TeamMemberDailyVO
{
    /** 用户ID */
    private Long userId;

    /** 姓名 */
    private String nickName;

    /** 所属部门名称 */
    private String deptName;

    /**
     * 每日工时 Map，key 为日期字符串 "yyyy-MM-dd"，value 为当日工时（小时）
     * 使用 LinkedHashMap 保持日期插入顺序
     */
    private Map<String, BigDecimal> dailyHours = new LinkedHashMap<>();

    /**
     * 累计工时（小时）。指定年月时为该月累计，未指定年月时为全周期累计（018 FR-009）。
     */
    private BigDecimal totalHours = BigDecimal.ZERO;

    /**
     * 是否已离场成员：填报过本项目工时，但已不在项目在册成员名单中（离职/被移出/is_active=0）。
     * 这类人的工时已计入 pm_project.actual_workload，必须显示出来，否则个人人天
     * 与项目累计人天对不上账且缺口无迹可寻（Issue #5 ③）。前端以灰色行 + 「已离场」标签呈现。
     *
     * <p>018 起取数范围为<b>全周期</b>（原为「本月有工时」），使「项目累计人天」在任何
     * 筛选条件下都有对应的人员承载行（FR-004）。
     */
    private Boolean isFormer = Boolean.FALSE;

    /**
     * 项目内角色标签（018 FR-011）。取值：项目经理 / 市场经理 / 销售负责人 / 参与人员 / null。
     * 由查询期从 pm_project 的四个人员字段反推，命中多个时按上述顺序取最高一个。
     * 反推不出时为 null，前端只显示昵称、不显示空括号（FR-012）。
     */
    private String roleLabel;

    /** 该成员在本项目的日报首日 yyyy-MM-dd（018 FR-017 主源），从未填报时为 null */
    private String firstReportDate;

    /** 该成员在本项目的日报末日 yyyy-MM-dd（018 FR-017 主源），从未填报时为 null */
    private String lastReportDate;

    /**
     * 成员表在册起始日（018 FR-017 兜底源）。
     * ⚠️ 这是<b>系统录入日</b>而非实际到岗日（实测 41% 早于其首次日报），
     * 仅在无日报时作为兜底展示，不作为参与时间的主口径。
     */
    private String joinDate;

    /** 成员表在册截止日（018 FR-017 兜底源），在册成员为 null */
    private String leaveDate;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public Map<String, BigDecimal> getDailyHours() { return dailyHours; }
    public void setDailyHours(Map<String, BigDecimal> dailyHours) { this.dailyHours = dailyHours; }

    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }

    public Boolean getIsFormer() { return isFormer; }
    public void setIsFormer(Boolean isFormer) { this.isFormer = isFormer; }

    public String getRoleLabel() { return roleLabel; }
    public void setRoleLabel(String roleLabel) { this.roleLabel = roleLabel; }

    public String getFirstReportDate() { return firstReportDate; }
    public void setFirstReportDate(String firstReportDate) { this.firstReportDate = firstReportDate; }

    public String getLastReportDate() { return lastReportDate; }
    public void setLastReportDate(String lastReportDate) { this.lastReportDate = lastReportDate; }

    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

    public String getLeaveDate() { return leaveDate; }
    public void setLeaveDate(String leaveDate) { this.leaveDate = leaveDate; }
}
