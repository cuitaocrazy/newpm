package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.ProjectManagerChange;
import com.ruoyi.project.domain.vo.ProjectManagerChangeVO;
import org.apache.ibatis.annotations.Param;

/**
 * 项目经理变更Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-14
 */
public interface ProjectManagerChangeMapper
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
    public List<ProjectManagerChange> selectProjectChangeHistory(@Param("projectId") Long projectId);
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
}
