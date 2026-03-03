package com.ruoyi.project.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.vo.ProjectStatsVO;

/**
 * 项目人天统计 Service 接口
 */
public interface IProjectStatsService
{
    /**
     * 统计项目总数
     */
    long countProjects(String projectName);

    /**
     * 分页查询项目人天统计
     *
     * @param projectName 项目名称（模糊，可为 null）
     * @param pageNum     页码（从1开始）
     * @param pageSize    每页条数
     * @return 按项目聚合的统计列表（当前页）
     */
    List<ProjectStatsVO> selectProjectStatsList(String projectName, int pageNum, int pageSize);

    /**
     * 更新调整人天
     */
    int updateAdjustWorkload(Long projectId, BigDecimal adjustWorkload);

    /**
     * 项目名称 autocomplete
     */
    List<Map<String, Object>> selectProjectNameSuggestions(String keyword);
}
