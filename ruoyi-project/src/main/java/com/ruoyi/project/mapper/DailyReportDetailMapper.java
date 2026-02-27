package com.ruoyi.project.mapper;

import java.util.List;
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
}
