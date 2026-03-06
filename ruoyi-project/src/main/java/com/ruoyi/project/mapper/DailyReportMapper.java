package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.DailyReport;

/**
 * 工作日报Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-26
 */
public interface DailyReportMapper
{
    /**
     * 查询工作日报
     *
     * @param reportId 日报主键
     * @return 工作日报
     */
    public DailyReport selectDailyReportById(Long reportId);

    /**
     * 根据用户ID和日期查询日报
     *
     * @param userId 用户ID
     * @param reportDate 日报日期
     * @return 工作日报
     */
    public DailyReport selectByUserAndDate(@Param("userId") Long userId, @Param("reportDate") String reportDate);

    /**
     * 查询工作日报列表（不含明细）
     *
     * @param query 查询条件
     * @return 工作日报集合
     */
    public List<DailyReport> selectDailyReportList(DailyReport query);

    /**
     * 查询月度日报列表（含明细）
     *
     * @param query 查询条件
     * @return 工作日报集合（含明细）
     */
    public List<DailyReport> selectMonthlyReports(DailyReport query);

    /**
     * 新增工作日报
     *
     * @param report 工作日报
     * @return 结果
     */
    public int insertDailyReport(DailyReport report);

    /**
     * 修改工作日报
     *
     * @param report 工作日报
     * @return 结果
     */
    public int updateDailyReport(DailyReport report);

    /**
     * 删除工作日报（软删除）
     *
     * @param reportId 日报主键
     * @return 结果
     */
    public int deleteDailyReportById(Long reportId);

    /**
     * 批量删除工作日报（软删除）
     *
     * @param reportIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDailyReportByIds(Long[] reportIds);

    /**
     * 查询活动页可见用户列表（按数据权限 + deptId 过滤）
     *
     * @param query 查询条件（deptId + dataScope）
     * @return 用户列表
     */
    public List<Map<String, Object>> selectActivityUsers(DailyReport query);
}
