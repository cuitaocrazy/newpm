package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.DailyReport;
import com.ruoyi.project.domain.vo.TeamDailyReportVO;

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
     * 根据用户ID和日期查询日报ID（简单查询，用于存在性检查）
     *
     * @param userId 用户ID
     * @param reportDate 日报日期(yyyy-MM-dd)
     * @return 日报ID，不存在则返回null
     */
    public Long selectReportIdByUserAndDate(@Param("userId") Long userId, @Param("reportDate") String reportDate);

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

    /**
     * 按日期统计已提交人数（指定日期范围内每天去重计数）
     * 用于日报统计报表的汇总数据
     */
    List<Map<String, Object>> selectSubmittedCountByDate(DailyReport query);

    /**
     * 查询数据权限范围内的活跃用户总数（排除白名单）
     * 用于计算未提交人数 = 总数 - 已提交数
     */
    int selectTotalUserCount(DailyReport query);

    /**
     * 查询某天已提交人员明细（含工时和工作内容摘要）
     */
    List<Map<String, Object>> selectSubmittedUsersOnDate(DailyReport query);

    /**
     * 查询某天未提交人员明细（排除白名单）
     */
    List<Map<String, Object>> selectUnsubmittedUsersOnDate(DailyReport query);

    /**
     * 查询日报统计报表专用部门树（三级及以下，三级节点 parentId=0 作为根节点）
     */
    List<Map<String, Object>> selectStatsDeptTree(DailyReport query);

    /**
     * 团队日报 - 按部门+月份查询原始平铺行（项目×成员×日期）
     * Java 层再按 projectId → userId 聚合
     */
    List<Map<String, Object>> selectTeamMonthlyRaw(DailyReport query);

    /**
     * 团队日报 - 项目名称 autocomplete（按部门范围模糊搜索，最多20条）
     */
    List<Map<String, Object>> selectTeamProjectOptions(DailyReport query);
}
