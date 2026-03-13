package com.ruoyi.project.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.ProjectStageChange;

/**
 * 项目阶段变更Mapper接口
 *
 * @author ruoyi
 * @date 2026-03-03
 */
public interface ProjectStageChangeMapper
{
    ProjectStageChange selectProjectStageChangeByChangeId(Long changeId);

    /** 以项目为主体的列表查询（含最新变更信息、合同状态） */
    List<ProjectStageChange> selectProjectStageChangeList(ProjectStageChange projectStageChange);

    /** 查询某项目的全部变更历史（时间倒序，用于变更记录弹窗） */
    List<ProjectStageChange> selectHistoryByProjectId(Long projectId);

    /** 查询项目当前阶段（来自 pm_project.project_stage） */
    String selectProjectStageByProjectId(Long projectId);

    /** 查询项目当前状态（来自 pm_project.project_status） */
    String selectProjectStatusByProjectId(Long projectId);

    /** 更新 pm_project.project_stage */
    int updateProjectStageByProjectId(@Param("projectId") Long projectId, @Param("projectStage") String projectStage);

    /** 更新 pm_project.project_status */
    int updateProjectStatusByProjectId(@Param("projectId") Long projectId, @Param("projectStatus") String projectStatus);

    int insertProjectStageChange(ProjectStageChange projectStageChange);

    int updateProjectStageChange(ProjectStageChange projectStageChange);

    int deleteProjectStageChangeByChangeId(Long changeId);

    int deleteProjectStageChangeByChangeIds(Long[] changeIds);
}
