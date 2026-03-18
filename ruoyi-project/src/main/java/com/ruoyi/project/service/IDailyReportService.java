package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.project.domain.DailyReport;
import com.ruoyi.project.domain.vo.DailySubmissionStat;
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

    /**
     * 查询某月每天日报提交统计（已提交/未提交人数）
     * @param query 查询条件（yearMonth 必填，deptId/deptIds 可选）
     */
    List<DailySubmissionStat> selectWeeklyStats(DailyReport query);

    /**
     * 查询统计范围内活跃用户总数（排除白名单，供统计报表展示）
     */
    int selectTotalUsersForStats(DailyReport query);

    /**
     * 查询日报统计报表专用部门树（三级及以下）
     */
    List<Map<String, Object>> selectStatsDeptTree(DailyReport query);

    /**
     * 查询某天已提交人员明细（含工时和工作内容摘要）
     * @param query 查询条件（reportDate 必填，deptId 可选）
     */
    List<Map<String, Object>> selectSubmittedDetail(DailyReport query);

    /**
     * 查询某天未提交人员明细
     * @param query 查询条件（reportDate 必填，deptId 可选）
     */
    List<Map<String, Object>> selectUnsubmittedDetail(DailyReport query);

    /**
     * 导出日报统计报表为 Excel（双 Sheet）
     */
    void exportWeeklyStats(HttpServletResponse response, List<DailySubmissionStat> statList, DailyReport query);
}
