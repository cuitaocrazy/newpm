package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.ProjectStageChangeMapper;
import com.ruoyi.project.domain.ProjectStageChange;
import com.ruoyi.project.service.IProjectStageChangeService;

/**
 * 项目阶段变更Service业务层处理
 *
 * @author ruoyi
 * @date 2026-03-03
 */
@Service
public class ProjectStageChangeServiceImpl implements IProjectStageChangeService
{
    @Autowired
    private ProjectStageChangeMapper projectStageChangeMapper;

    @Override
    public ProjectStageChange selectProjectStageChangeByChangeId(Long changeId)
    {
        return projectStageChangeMapper.selectProjectStageChangeByChangeId(changeId);
    }

    @Override
    public List<ProjectStageChange> selectProjectStageChangeList(ProjectStageChange projectStageChange)
    {
        return projectStageChangeMapper.selectProjectStageChangeList(projectStageChange);
    }

    @Override
    public List<ProjectStageChange> historyByProject(Long projectId)
    {
        return projectStageChangeMapper.selectHistoryByProjectId(projectId);
    }

    /**
     * 新增变更记录，同时更新 pm_project.project_stage
     */
    @Override
    @Transactional
    public int insertProjectStageChange(ProjectStageChange projectStageChange)
    {
        projectStageChange.setDelFlag("0");
        projectStageChange.setCreateBy(SecurityUtils.getUsername());
        projectStageChange.setCreateTime(DateUtils.getNowDate());
        int rows = projectStageChangeMapper.insertProjectStageChange(projectStageChange);
        if (rows > 0 && projectStageChange.getProjectId() != null && projectStageChange.getNewStage() != null)
        {
            projectStageChangeMapper.updateProjectStageByProjectId(
                    projectStageChange.getProjectId(), projectStageChange.getNewStage());
        }
        return rows;
    }

    /**
     * 批量变更：为每个项目取当前阶段作为 old_stage，创建变更记录，更新 pm_project.project_stage
     */
    @Override
    @Transactional
    public int batchChange(Long[] projectIds, String newStage, String changeReason)
    {
        String changeBy = SecurityUtils.getUsername();
        for (Long projectId : projectIds)
        {
            String oldStage = projectStageChangeMapper.selectProjectStageByProjectId(projectId);
            ProjectStageChange change = new ProjectStageChange();
            change.setProjectId(projectId);
            change.setOldStage(oldStage);
            change.setNewStage(newStage);
            change.setChangeReason(changeReason);
            change.setDelFlag("0");
            change.setCreateBy(changeBy);
            change.setCreateTime(DateUtils.getNowDate());
            projectStageChangeMapper.insertProjectStageChange(change);
            projectStageChangeMapper.updateProjectStageByProjectId(projectId, newStage);
        }
        return projectIds.length;
    }

    @Override
    public int updateProjectStageChange(ProjectStageChange projectStageChange)
    {
        projectStageChange.setUpdateTime(DateUtils.getNowDate());
        return projectStageChangeMapper.updateProjectStageChange(projectStageChange);
    }

    @Override
    public int deleteProjectStageChangeByChangeIds(Long[] changeIds)
    {
        return projectStageChangeMapper.deleteProjectStageChangeByChangeIds(changeIds);
    }

    @Override
    public int deleteProjectStageChangeByChangeId(Long changeId)
    {
        return projectStageChangeMapper.deleteProjectStageChangeByChangeId(changeId);
    }
}
