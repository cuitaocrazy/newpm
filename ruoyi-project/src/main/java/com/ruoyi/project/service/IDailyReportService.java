package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.DailyReport;
/**
 * 工作日报Service接口
 *
 * @author ruoyi
 * @date 2026-02-26
 */
public interface IDailyReportService
{
    /**
     * 查询工作日报
     *
     * @param reportId 日报主键
     * @return 工作日报
     */
    public DailyReport selectDailyReportById(Long reportId);

    /**
     * 查询当前用户指定日期的日报
     *
     * @param reportDate 日报日期(yyyy-MM-dd)
     * @return 工作日报
     */
    public DailyReport selectMyReportByDate(String reportDate);

    /**
     * 查询工作日报列表（不含明细）
     *
     * @param dailyReport 查询条件
     * @return 工作日报集合
     */
    public List<DailyReport> selectDailyReportList(DailyReport dailyReport);

    /**
     * 查询月度日报列表（含明细）
     *
     * @param dailyReport 查询条件（需包含yearMonth）
     * @return 工作日报集合
     */
    public List<DailyReport> selectMonthlyReports(DailyReport dailyReport);

    /**
     * 查询当前用户参与的项目列表
     *
     * @return 项目列表
     */
    public List<Map<String, Object>> selectMyProjects();

    /**
     * 保存工作日报（新增或修改）
     *
     * @param dailyReport 工作日报
     * @return 结果
     */
    public int saveDailyReport(DailyReport dailyReport);

    /**
     * 批量删除工作日报
     *
     * @param reportIds 需要删除的日报主键集合
     * @return 结果
     */
    public int deleteDailyReportByIds(Long[] reportIds);

    /**
     * 查询活动页用户列表（数据权限过滤）
     *
     * @param query 查询条件（deptId）
     * @return 用户列表
     */
    public List<Map<String, Object>> selectActivityUsers(DailyReport query);
}
