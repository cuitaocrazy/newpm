package com.ruoyi.project.service.impl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.WorkCalendar;
import com.ruoyi.project.mapper.WorkCalendarMapper;
import com.ruoyi.project.service.IWorkCalendarService;

@Service
public class WorkCalendarServiceImpl implements IWorkCalendarService
{
    @Autowired
    private WorkCalendarMapper workCalendarMapper;

    @Override
    public WorkCalendar selectWorkCalendarById(Long id)
    {
        return workCalendarMapper.selectWorkCalendarById(id);
    }

    @Override
    public List<WorkCalendar> selectWorkCalendarList(WorkCalendar query)
    {
        return workCalendarMapper.selectWorkCalendarList(query);
    }

    @Override
    public List<WorkCalendar> selectByYear(Integer year)
    {
        return workCalendarMapper.selectByYear(year);
    }

    @Override
    public int insertWorkCalendar(WorkCalendar workCalendar)
    {
        // 自动填充 year 字段
        if (workCalendar.getCalendarDate() != null && workCalendar.getYear() == null)
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(workCalendar.getCalendarDate());
            workCalendar.setYear(cal.get(Calendar.YEAR));
        }
        workCalendar.setCreateBy(SecurityUtils.getUsername());
        return workCalendarMapper.insertWorkCalendar(workCalendar);
    }

    @Override
    public int updateWorkCalendar(WorkCalendar workCalendar)
    {
        if (workCalendar.getCalendarDate() != null)
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(workCalendar.getCalendarDate());
            workCalendar.setYear(cal.get(Calendar.YEAR));
        }
        workCalendar.setUpdateBy(SecurityUtils.getUsername());
        return workCalendarMapper.updateWorkCalendar(workCalendar);
    }

    @Override
    public int deleteWorkCalendarByIds(Long[] ids)
    {
        return workCalendarMapper.deleteWorkCalendarByIds(ids);
    }

    @Override
    @Transactional
    public int batchSave(List<WorkCalendar> list, Integer year)
    {
        String username = SecurityUtils.getUsername();
        int count = 0;
        for (WorkCalendar wc : list)
        {
            if (wc.getCalendarDate() != null && wc.getYear() == null)
            {
                Calendar cal = Calendar.getInstance();
                cal.setTime(wc.getCalendarDate());
                wc.setYear(cal.get(Calendar.YEAR));
            }
            if (wc.getId() != null)
            {
                wc.setUpdateBy(username);
                count += workCalendarMapper.updateWorkCalendar(wc);
            }
            else
            {
                wc.setCreateBy(username);
                count += workCalendarMapper.insertWorkCalendar(wc);
            }
        }
        return count;
    }
}
