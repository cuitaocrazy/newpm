package com.ruoyi.project.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.WorkloadCorrectLog;

/**
 * 项目人天统计 Mapper
 */
public interface ProjectStatsMapper
{
    /**
     * 统计符合条件的项目总数（用于分页）
     */
    long countProjects(Project query);

    /**
     * 分页查询项目人天统计（按项目+阶段平铺）
     * 先按项目分页，再取各项目的阶段明细，避免 rowspan 被截断
     */
    List<Map<String, Object>> selectProjectStatsByStageWithPage(
            @Param("query") Project query,
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

    /**
     * 查询项目当前调整人天值（用于记录补正前值）
     */
    BigDecimal selectCurrentAdjustWorkload(@Param("projectId") Long projectId);

    /**
     * 插入补正日志
     */
    int insertCorrectLog(WorkloadCorrectLog log);

    /**
     * 查询项目补正日志列表（按时间倒序）
     */
    List<WorkloadCorrectLog> selectCorrectLogsByProjectId(@Param("projectId") Long projectId);
}
