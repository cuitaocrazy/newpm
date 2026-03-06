package com.ruoyi.project.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.vo.ProjectStatsVO;
import com.ruoyi.project.domain.vo.StageStatsVO;
import com.ruoyi.project.mapper.ProjectStatsMapper;
import com.ruoyi.project.service.IProjectStatsService;

/**
 * 项目人天统计 Service 实现
 */
@Service
public class ProjectStatsServiceImpl implements IProjectStatsService
{
    @Autowired
    private ProjectStatsMapper projectStatsMapper;

    @Override
    @DataScope(deptAlias = "d", userAlias = "u_create")
    public long countProjects(Project query)
    {
        return projectStatsMapper.countProjects(query);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "u_create")
    public List<ProjectStatsVO> selectProjectStatsList(Project query, int pageNum, int pageSize)
    {
        long offset = (long) (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = projectStatsMapper.selectProjectStatsByStageWithPage(query, offset, pageSize);

        // 按 projectId 聚合（保持 ORDER BY 顺序）
        Map<Long, ProjectStatsVO> projectMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rows)
        {
            Long projectId = toLong(row.get("projectId"));
            ProjectStatsVO vo = projectMap.get(projectId);
            if (vo == null)
            {
                vo = new ProjectStatsVO();
                vo.setProjectId(projectId);
                vo.setProjectName(toString(row.get("projectName")));
                vo.setProjectManagerName(toString(row.get("projectManagerName")));
                vo.setEstimatedWorkload(toDecimal(row.get("estimatedWorkload")));
                vo.setAdjustWorkload(toDecimal(row.get("adjustWorkload")));
                vo.setStages(new ArrayList<>());
                projectMap.put(projectId, vo);
            }

            // 只添加有实际日报数据的阶段
            String projectStage = toString(row.get("projectStage"));
            if (projectStage != null)
            {
                StageStatsVO stage = new StageStatsVO();
                stage.setProjectStage(projectStage);
                stage.setStageDays(toDecimal(row.get("stageDays")));
                vo.getStages().add(stage);
            }
        }

        // 计算项目汇总值
        List<ProjectStatsVO> result = new ArrayList<>(projectMap.values());
        for (ProjectStatsVO vo : result)
        {
            BigDecimal total = vo.getStages().stream()
                    .map(StageStatsVO::getStageDays)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal adjust = vo.getAdjustWorkload() != null ? vo.getAdjustWorkload() : BigDecimal.ZERO;
            vo.setTotalActualDays(total);
            vo.setActualDays(total.add(adjust));
        }

        return result;
    }

    @Override
    public int updateAdjustWorkload(Long projectId, BigDecimal adjustWorkload)
    {
        return projectStatsMapper.updateAdjustWorkload(projectId, adjustWorkload);
    }

    @Override
    public List<Map<String, Object>> selectProjectNameSuggestions(String keyword)
    {
        return projectStatsMapper.selectProjectNameSuggestions(keyword);
    }

    private Long toLong(Object val)
    {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private String toString(Object val)
    {
        return val == null ? null : val.toString();
    }

    private BigDecimal toDecimal(Object val)
    {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return new BigDecimal(val.toString());
        return new BigDecimal(val.toString());
    }
}
