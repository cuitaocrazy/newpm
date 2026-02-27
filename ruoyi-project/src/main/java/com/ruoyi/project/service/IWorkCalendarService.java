package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.WorkCalendar;

public interface IWorkCalendarService
{
    WorkCalendar selectWorkCalendarById(Long id);

    List<WorkCalendar> selectWorkCalendarList(WorkCalendar query);

    List<WorkCalendar> selectByYear(Integer year);

    int insertWorkCalendar(WorkCalendar workCalendar);

    int updateWorkCalendar(WorkCalendar workCalendar);

    int deleteWorkCalendarByIds(Long[] ids);

    /** 批量保存（先删再插） */
    int batchSave(List<WorkCalendar> list, Integer year);
}
