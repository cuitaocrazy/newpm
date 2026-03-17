package com.ruoyi.project.controller;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.DailyReport;
import com.ruoyi.project.domain.vo.DailySubmissionStat;
import com.ruoyi.project.service.IDailyReportService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 日报管理Controller
 *
 * @author ruoyi
 * @date 2026-02-26
 */
@RestController
@RequestMapping("/project/dailyReport")
public class DailyReportController extends BaseController
{
    @Autowired
    private IDailyReportService dailyReportService;

    /**
     * 查询工作日报列表（分页，不含明细）
     */
    @PreAuthorize("@ss.hasAnyPermi('project:dailyReport:list,project:dailyReport:activity')")
    @GetMapping("/list")
    public TableDataInfo list(DailyReport dailyReport)
    {
        startPage();
        List<DailyReport> list = dailyReportService.selectDailyReportList(dailyReport);
        return getDataTable(list);
    }

    /**
     * 查询月度日报列表（含明细，不分页）
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:activity')")
    @GetMapping("/monthly")
    public AjaxResult monthly(DailyReport dailyReport)
    {
        List<DailyReport> list = dailyReportService.selectMonthlyReports(dailyReport);
        return success(list);
    }

    /**
     * 获取工作日报详细信息
     */
    @PreAuthorize("@ss.hasAnyPermi('project:dailyReport:query,project:dailyReport:activity')")
    @GetMapping(value = "/{reportId}")
    public AjaxResult getInfo(@PathVariable("reportId") Long reportId)
    {
        return success(dailyReportService.selectDailyReportById(reportId));
    }

    /**
     * 获取当前用户指定日期的日报
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:list')")
    @GetMapping(value = "/my/{reportDate}")
    public AjaxResult getMyReport(@PathVariable("reportDate") String reportDate)
    {
        return success(dailyReportService.selectMyReportByDate(reportDate));
    }

    /**
     * 获取当前用户参与的项目列表
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:list')")
    @GetMapping("/myProjects")
    public AjaxResult myProjects()
    {
        List<Map<String, Object>> list = dailyReportService.selectMyProjects();
        return success(list);
    }

    /**
     * 保存工作日报（新增或修改）
     */
    @PreAuthorize("@ss.hasAnyPermi('project:dailyReport:add,project:dailyReport:edit')")
    @Log(title = "日报管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult save(@RequestBody DailyReport dailyReport)
    {
        return toAjax(dailyReportService.saveDailyReport(dailyReport));
    }

    /**
     * 删除工作日报
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:remove')")
    @Log(title = "日报管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reportIds}")
    public AjaxResult remove(@PathVariable Long[] reportIds)
    {
        return toAjax(dailyReportService.deleteDailyReportByIds(reportIds));
    }

    /**
     * 获取活动页用户列表（数据权限过滤）
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:activity')")
    @GetMapping("/activityUsers")
    public AjaxResult activityUsers(DailyReport dailyReport)
    {
        List<Map<String, Object>> list = dailyReportService.selectActivityUsers(dailyReport);
        return success(list);
    }

    /**
     * 日报统计报表 - 按天汇总
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:weeklyStats')")
    @GetMapping("/weeklyStats")
    public AjaxResult weeklyStats(DailyReport dailyReport)
    {
        List<DailySubmissionStat> list = dailyReportService.selectWeeklyStats(dailyReport);
        return success(list);
    }

    /**
     * 日报统计报表 - 人员明细
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:weeklyStats')")
    @GetMapping("/weeklyStatsDetail")
    public AjaxResult weeklyStatsDetail(DailyReport dailyReport)
    {
        List<Map<String, Object>> list;
        if ("submitted".equals(dailyReport.getType())) {
            list = dailyReportService.selectSubmittedDetail(dailyReport);
        } else {
            list = dailyReportService.selectUnsubmittedDetail(dailyReport);
        }
        return success(list);
    }

    /**
     * 日报统计报表 - Excel 导出（双 Sheet）
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:weeklyStatsExport')")
    @Log(title = "日报统计报表", businessType = BusinessType.EXPORT)
    @GetMapping("/weeklyStatsExport")
    public void weeklyStatsExport(HttpServletResponse response, DailyReport dailyReport)
    {
        List<DailySubmissionStat> statList = dailyReportService.selectWeeklyStats(dailyReport);
        dailyReportService.exportWeeklyStats(response, statList, dailyReport);
    }
}
