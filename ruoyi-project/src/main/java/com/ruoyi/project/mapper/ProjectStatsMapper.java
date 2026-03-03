package com.ruoyi.project.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 项目人天统计 Mapper
 */
public interface ProjectStatsMapper
{
    /**
     * 统计符合条件的项目总数（用于分页）
     */
    long countProjects(@Param("projectName") String projectName);

    /**
     * 分页查询项目人天统计（按项目+阶段平铺）
     * 先按项目分页，再取各项目的阶段明细，避免 rowspan 被截断
     */
    List<Map<String, Object>> selectProjectStatsByStageWithPage(
            @Param("projectName") String projectName,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize);

    /**
     * 更新调整人天
     */
    int updateAdjustWorkload(@Param("projectId") Long projectId,
                             @Param("adjustWorkload") BigDecimal adjustWorkload);

    /**
     * 项目名称 autocomplete 查询
     */
    List<Map<String, Object>> selectProjectNameSuggestions(@Param("keyword") String keyword);
}
