package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.WorkCalendar;

public interface WorkCalendarMapper
{
    WorkCalendar selectWorkCalendarById(Long id);

    List<WorkCalendar> selectWorkCalendarList(WorkCalendar query);

    List<WorkCalendar> selectByYear(Integer year);

    int insertWorkCalendar(WorkCalendar workCalendar);

    int updateWorkCalendar(WorkCalendar workCalendar);

    int deleteWorkCalendarByIds(Long[] ids);

    int deleteWorkCalendarById(Long id);
}
