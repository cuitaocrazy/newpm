package com.ruoyi.project.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.WorkCalendar;
import com.ruoyi.project.service.IWorkCalendarService;

@RestController
@RequestMapping("/project/workCalendar")
public class WorkCalendarController extends BaseController
{
    @Autowired
    private IWorkCalendarService workCalendarService;

    /** 查询列表（分页） */
    @PreAuthorize("@ss.hasPermi('project:workCalendar:list')")
    @GetMapping("/list")
    public TableDataInfo list(WorkCalendar query)
    {
        startPage();
        List<WorkCalendar> list = workCalendarService.selectWorkCalendarList(query);
        return getDataTable(list);
    }

    /** 按年份查询全部（日历渲染用，不分页） */
    @PreAuthorize("@ss.hasAnyPermi('project:workCalendar:list,project:dailyReport:activity')")
    @GetMapping("/year/{year}")
    public AjaxResult getByYear(@PathVariable Integer year)
    {
        return success(workCalendarService.selectByYear(year));
    }

    /** 查询详情 */
    @PreAuthorize("@ss.hasPermi('project:workCalendar:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(workCalendarService.selectWorkCalendarById(id));
    }

    /** 新增 */
    @PreAuthorize("@ss.hasPermi('project:workCalendar:add')")
    @Log(title = "工作日历", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WorkCalendar workCalendar)
    {
        return toAjax(workCalendarService.insertWorkCalendar(workCalendar));
    }

    /** 修改 */
    @PreAuthorize("@ss.hasPermi('project:workCalendar:edit')")
    @Log(title = "工作日历", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WorkCalendar workCalendar)
    {
        return toAjax(workCalendarService.updateWorkCalendar(workCalendar));
    }

    /** 删除 */
    @PreAuthorize("@ss.hasPermi('project:workCalendar:remove')")
    @Log(title = "工作日历", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(workCalendarService.deleteWorkCalendarByIds(ids));
    }

    /** 批量保存 */
    @PreAuthorize("@ss.hasPermi('project:workCalendar:edit')")
    @Log(title = "工作日历", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSave")
    public AjaxResult batchSave(@RequestBody List<WorkCalendar> list)
    {
        if (list == null || list.isEmpty())
        {
            return error("数据不能为空");
        }
        Integer year = null;
        if (list.get(0).getYear() != null)
        {
            year = list.get(0).getYear();
        }
        return toAjax(workCalendarService.batchSave(list, year));
    }
}
