package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.ProjectManagerChange;
import com.ruoyi.project.domain.vo.ProjectManagerChangeVO;

/**
 * 项目经理变更Service接口
 *
 * @author ruoyi
 * @date 2026-02-14
 */
public interface IProjectManagerChangeService
{
    /**
     * 查询项目列表（带最新变更信息）
     *
     * @param vo 查询条件
     * @return 项目列表
     */
    public List<ProjectManagerChangeVO> selectProjectListWithLatestChange(ProjectManagerChangeVO vo);

    /**
     * 查询项目的完整变更历史
     *
     * @param projectId 项目ID
     * @return 变更历史列表
     */
    public List<ProjectManagerChange> selectProjectChangeHistory(Long projectId);

    /**
     * 变更项目经理（单个）
     *
     * @param projectId 项目ID
     * @param newManagerId 新项目经理ID
     * @param changeReason 变更原因
     * @return 结果
     */
    public int changeProjectManager(Long projectId, Long newManagerId, String changeReason);

    /**
     * 批量变更项目经理
     *
     * @param projectIds 项目ID数组
     * @param newManagerId 新项目经理ID
     * @param changeReason 变更原因
     * @return 结果
     */
    public int batchChangeProjectManager(Long[] projectIds, Long newManagerId, String changeReason);
    /**
     * 查询项目经理变更
     * 
     * @param changeId 项目经理变更主键
     * @return 项目经理变更
     */
    public ProjectManagerChange selectProjectManagerChangeByChangeId(Long changeId);

    /**
     * 查询项目经理变更列表
     * 
     * @param projectManagerChange 项目经理变更
     * @return 项目经理变更集合
     */
    public List<ProjectManagerChange> selectProjectManagerChangeList(ProjectManagerChange projectManagerChange);

    /**
     * 新增项目经理变更
     * 
     * @param projectManagerChange 项目经理变更
     * @return 结果
     */
    public int insertProjectManagerChange(ProjectManagerChange projectManagerChange);

    /**
     * 修改项目经理变更
     * 
     * @param projectManagerChange 项目经理变更
     * @return 结果
     */
    public int updateProjectManagerChange(ProjectManagerChange projectManagerChange);

    /**
     * 批量删除项目经理变更
     * 
     * @param changeIds 需要删除的项目经理变更主键集合
     * @return 结果
     */
    public int deleteProjectManagerChangeByChangeIds(Long[] changeIds);

    /**
     * 删除项目经理变更信息
     * 
     * @param changeId 项目经理变更主键
     * @return 结果
     */
    public int deleteProjectManagerChangeByChangeId(Long changeId);
}
