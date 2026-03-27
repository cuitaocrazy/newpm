package com.ruoyi.project.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工作日报对象 pm_daily_report
 *
 * @author ruoyi
 * @date 2026-02-26
 */
public class DailyReport extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日报主键ID */
    private Long reportId;

    /** 日报日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "日报日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date reportDate;

    /** 用户ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;

    /** 总工时(小时) */
    @Excel(name = "总工时(小时)")
    private BigDecimal totalWorkHours;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    /** 日报明细列表 */
    private List<DailyReportDetail> detailList;

    /** 用户名（扩展字段，用于列表展示） */
    private String userName;

    /** 用户昵称（扩展字段，用于列表展示） */
    @Excel(name = "姓名")
    private String nickName;

    /** 部门名称（扩展字段，用于列表展示） */
    @Excel(name = "部门名称")
    private String deptName;

    /** 查询条件：日报日期起始 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date reportDateStart;

    /** 查询条件：日报日期结束 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date reportDateEnd;

    /** 查询条件：年月(如 2026-02) */
    private String yearMonth;

    /** 查询条件：项目名称（模糊） */
    private String projectName;

    /** 查询条件：项目ID（精确，按项目成员查人） */
    private Long projectId;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    /** 假期摘要（月览接口用）格式: "leave:2.00,comp:1.00" */
    private String leaveSummary;

    public void setLeaveSummary(String leaveSummary) { this.leaveSummary = leaveSummary; }
    public String getLeaveSummary() { return leaveSummary; }

    public void setReportId(Long reportId)
    {
        this.reportId = reportId;
    }

    public Long getReportId()
    {
        return reportId;
    }

    public void setReportDate(Date reportDate)
    {
        this.reportDate = reportDate;
    }

    public Date getReportDate()
    {
        return reportDate;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setTotalWorkHours(BigDecimal totalWorkHours)
    {
        this.totalWorkHours = totalWorkHours;
    }

    public BigDecimal getTotalWorkHours()
    {
        return totalWorkHours;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDetailList(List<DailyReportDetail> detailList)
    {
        this.detailList = detailList;
    }

    public List<DailyReportDetail> getDetailList()
    {
        return detailList;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setReportDateStart(Date reportDateStart)
    {
        this.reportDateStart = reportDateStart;
    }

    public Date getReportDateStart()
    {
        return reportDateStart;
    }

    public void setReportDateEnd(Date reportDateEnd)
    {
        this.reportDateEnd = reportDateEnd;
    }

    public Date getReportDateEnd()
    {
        return reportDateEnd;
    }

    public void setYearMonth(String yearMonth)
    {
        this.yearMonth = yearMonth;
    }

    public String getYearMonth()
    {
        return yearMonth;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getProjectName()
    {
        return projectName;
    }

    /** 统计查询：起始日期字符串（yyyy-MM-dd，由 Service 层设置） */
    private String startDate;

    /** 统计查询：结束日期字符串（yyyy-MM-dd，由 Service 层设置） */
    private String endDate;

    /** 统计查询：明细类型（submitted=已提交 / unsubmitted=未提交） */
    private String type;

    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getStartDate() { return startDate; }

    public void setEndDate(String endDate) { this.endDate = endDate; }
    public String getEndDate() { return endDate; }

    public void setType(String type) { this.type = type; }
    public String getType() { return type; }

    /** 查询条件：收入确认年度（多选），逗号分隔字符串，如 "2024,2025"（团队日报筛选） */
    private String revenueConfirmYears;

    public void setRevenueConfirmYears(String revenueConfirmYears) { this.revenueConfirmYears = revenueConfirmYears; }
    public String getRevenueConfirmYears() { return revenueConfirmYears; }

    /** 将 revenueConfirmYears 字符串解析为 List，供 MyBatis foreach 使用 */
    public List<String> getRevenueConfirmYearList() {
        if (revenueConfirmYears == null || revenueConfirmYears.trim().isEmpty()) return null;
        return Arrays.stream(revenueConfirmYears.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /** 查询条件：多部门ID，逗号分隔字符串，如 "1,2,3"（日报统计报表多选） */
    private String deptIds;

    public void setDeptIds(String deptIds) { this.deptIds = deptIds; }
    public String getDeptIds() { return deptIds; }

    /** 将 deptIds 字符串解析为 Long 列表，供 MyBatis OGNL 使用 */
    public List<Long> getDeptIdList() {
        if (deptIds == null || deptIds.trim().isEmpty()) return null;
        return Arrays.stream(deptIds.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("reportId", getReportId())
            .append("reportDate", getReportDate())
            .append("userId", getUserId())
            .append("deptId", getDeptId())
            .append("totalWorkHours", getTotalWorkHours())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
