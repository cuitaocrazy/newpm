package com.ruoyi.project.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class WorkCalendar extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date calendarDate;

    /** holiday=节假日 / workday=调休上班日 */
    private String dayType;

    /** 名称，如 春节、元旦调休 */
    private String dayName;

    /** 年份 */
    private Integer year;

    private String delFlag;

    /** 查询用：日期字符串 */
    private String calendarDateStr;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getCalendarDate() { return calendarDate; }
    public void setCalendarDate(Date calendarDate) { this.calendarDate = calendarDate; }
    public String getDayType() { return dayType; }
    public void setDayType(String dayType) { this.dayType = dayType; }
    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getCalendarDateStr() { return calendarDateStr; }
    public void setCalendarDateStr(String calendarDateStr) { this.calendarDateStr = calendarDateStr; }
}
