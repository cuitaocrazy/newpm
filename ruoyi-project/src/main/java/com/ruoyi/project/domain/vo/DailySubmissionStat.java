package com.ruoyi.project.domain.vo;

/**
 * 日报统计报表 — 每天提交情况 VO
 */
public class DailySubmissionStat {

    /** 日期（yyyy-MM-dd） */
    private String reportDate;

    /** 星期（周一~周日） */
    private String dayOfWeek;

    /** 是否工作日 */
    private Boolean isWorkday;

    /** 已提交人数 */
    private Integer submittedCount;

    /** 未提交人数（= 数据权限范围内总人数 - 已提交人数） */
    private Integer unsubmittedCount;

    /** 是否为未来日期（未来日期不统计提交情况） */
    private Boolean isFuture;

    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Boolean getIsWorkday() { return isWorkday; }
    public void setIsWorkday(Boolean isWorkday) { this.isWorkday = isWorkday; }

    public Integer getSubmittedCount() { return submittedCount; }
    public void setSubmittedCount(Integer submittedCount) { this.submittedCount = submittedCount; }

    public Integer getUnsubmittedCount() { return unsubmittedCount; }
    public void setUnsubmittedCount(Integer unsubmittedCount) { this.unsubmittedCount = unsubmittedCount; }

    public Boolean getIsFuture() { return isFuture; }
    public void setIsFuture(Boolean isFuture) { this.isFuture = isFuture; }
}
