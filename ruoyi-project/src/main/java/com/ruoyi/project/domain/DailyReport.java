package com.ruoyi.project.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
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
