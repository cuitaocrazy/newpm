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

    /** 月累计工时（小时） */
    private BigDecimal totalHours = BigDecimal.ZERO;

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
}
