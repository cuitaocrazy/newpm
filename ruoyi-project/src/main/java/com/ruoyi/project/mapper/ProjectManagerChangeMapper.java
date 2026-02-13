package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.ProjectManagerChange;
import com.ruoyi.project.domain.vo.ProjectManagerChangeVo;

/**
 * 项目经理变更Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-13
 */
public interface ProjectManagerChangeMapper 
{
    /**
     * 查询项目经理变更
     * 
     * @param changeId 项目经理变更主键
     * @return 项目经理变更
     */
    public ProjectManagerChange selectProjectManagerChangeByChangeId(Long changeId);

    /**
     * 查询项目经理变更列表（包含项目信息和最新变更记录）
     *
     * @param projectManagerChange 项目经理变更
     * @return 项目经理变更集合
     */
    public List<ProjectManagerChangeVo> selectProjectManagerChangeList(ProjectManagerChange projectManagerChange);

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
     * 删除项目经理变更
     * 
     * @param changeId 项目经理变更主键
     * @return 结果
     */
    public int deleteProjectManagerChangeByChangeId(Long changeId);

    /**
     * 批量删除项目经理变更
     *
     * @param changeIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProjectManagerChangeByChangeIds(Long[] changeIds);

    /**
     * 查询项目的所有变更历史记录
     *
     * @param projectId 项目ID
     * @return 变更历史记录列表
     */
    public List<ProjectManagerChange> selectChangeHistoryByProjectId(Long projectId);
}
