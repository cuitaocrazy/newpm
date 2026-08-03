package com.ruoyi.project.mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.DailyReportDetail;

/**
 * 工作日报明细Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-26
 */
public interface DailyReportDetailMapper
{
    /**
     * 根据日报ID查询明细列表
     *
     * @param reportId 日报ID
     * @return 明细列表
     */
    public List<DailyReportDetail> selectByReportId(Long reportId);

    /**
     * 新增日报明细
     *
     * @param detail 日报明细
     * @return 结果
     */
    public int insertDetail(DailyReportDetail detail);

    /**
     * 批量新增日报明细
     *
     * @param list 日报明细列表
     * @return 结果
     */
    public int batchInsert(List<DailyReportDetail> list);

    /**
     * 统计指定项目在所有日报中的总工时(小时)
     *
     * @param projectId 项目ID
     * @return 总工时
     */
    public BigDecimal sumWorkHoursByProjectId(Long projectId);

    /** 统计指定子任务在所有日报中的总工时(小时) */
    BigDecimal sumWorkHoursBySubProjectId(@Param("subProjectId") Long subProjectId);

    /**
     * 根据日报ID删除明细（物理删除）
     *
     * @param reportId 日报ID
     * @return 结果
     */
    public int deleteByReportId(Long reportId);

    /**
     * 根据日报ID批量删除明细（物理删除）
     *
     * @param reportIds 日报ID集合
     * @return 结果
     */
    public int deleteByReportIds(Long[] reportIds);

    /** 统计引用指定子任务的日报明细数量 */
    int countBySubProjectId(@Param("subProjectId") Long subProjectId);

    /**
     * 按「作用范围」删除日报明细（物理删除）
     *
     * <p>只删除填报人本次提交<b>有能力表达</b>的明细，即：非项目工时（project_id 为 null）
     * 与所属项目在其可填列表中的工时。范围外的明细（如已结项项目的历史工时）原样保留——
     * 填报人根本看不到它们，未出现在提交中不代表要删除。
     *
     * <p><b>不要退回 deleteByReportId</b>：那会把填报人看不见的工时一并删掉，
     * 正是 015 特性要修复的静默数据丢失（已确证造成 7.63 人天工时凭空消失）。
     *
     * @param reportId          日报ID
     * @param visibleProjectIds 填报人当前可填的项目ID集合；为空时退化为仅删非项目工时
     * @return 实际删除的明细条数
     */
    int deleteByReportIdInScope(@Param("reportId") Long reportId,
                                @Param("visibleProjectIds") Collection<Long> visibleProjectIds);

    /**
     * 统计该日报当前剩余的明细条数
     *
     * <p>用于删除日报时判断主记录去留：仍有残留则必须保留主记录，
     * 否则被保留的明细将无主记录可归属，无法通过任何业务查询到达（FR-014）。
     *
     * @param reportId 日报ID
     * @return 剩余明细条数
     */
    int countByReportId(@Param("reportId") Long reportId);

    /**
     * 统计该日报剩余 work 类型明细的工时之和
     *
     * <p>口径与 saveDailyReport 计算 totalWorkHours 一致：只累加 entry_type='work'，
     * 假期类记录不计入。用于保留主记录时重算其当日汇总工时（INV-D2）。
     *
     * @param reportId 日报ID
     * @return 剩余 work 工时之和；无记录时可能返回 null
     */
    BigDecimal sumWorkHoursByReportId(@Param("reportId") Long reportId);
}
