package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.ProjectStageChange;

/**
 * 项目阶段变更Service接口
 *
 * @author ruoyi
 * @date 2026-03-03
 */
public interface IProjectStageChangeService
{
    ProjectStageChange selectProjectStageChangeByChangeId(Long changeId);

    /** 以项目为主体的列表查询 */
    List<ProjectStageChange> selectProjectStageChangeList(ProjectStageChange projectStageChange);

    /** 查询某项目的全部变更历史 */
    List<ProjectStageChange> historyByProject(Long projectId);

    /** 新增变更记录（同时更新 pm_project.project_stage）*/
    int insertProjectStageChange(ProjectStageChange projectStageChange);

    /** 批量变更阶段（为每个项目创建变更记录并更新 pm_project.project_stage；newProjectStatus 不为空时同步更新 project_status）*/
    int batchChange(Long[] projectIds, String newStage, String changeReason, String newProjectStatus);

    int updateProjectStageChange(ProjectStageChange projectStageChange);

    int deleteProjectStageChangeByChangeIds(Long[] changeIds);

    int deleteProjectStageChangeByChangeId(Long changeId);
}
