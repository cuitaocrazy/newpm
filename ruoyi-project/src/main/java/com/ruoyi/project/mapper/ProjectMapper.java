package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.Project;

/**
 * 项目管理Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-01
 */
public interface ProjectMapper
{
    /**
     * 查询项目管理
     *
     * @param projectId 项目管理主键
     * @return 项目管理
     */
    public Project selectProjectByProjectId(Long projectId);

    /**
     * 查询项目管理列表
     *
     * @param project 项目管理
     * @return 项目管理集合
     */
    public List<Project> selectProjectList(Project project);

    /**
     * 新增项目管理
     *
     * @param project 项目管理
     * @return 结果
     */
    public int insertProject(Project project);

    /**
     * 修改项目管理
     *
     * @param project 项目管理
     * @return 结果
     */
    public int updateProject(Project project);

    /**
     * 删除项目管理
     *
     * @param projectId 项目管理主键
     * @return 结果
     */
    public int deleteProjectByProjectId(Long projectId);

    /**
     * 批量删除项目管理
     *
     * @param projectIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProjectByProjectIds(Long[] projectIds);

    /**
     * 查询项目金额汇总
     *
     * @param project 查询条件
     * @return 汇总结果
     */
    public Map<String, Object> selectProjectSummary(Project project);

    /**
     * 查询项目名称列表（用于智能提示）
     *
     * @param projectName 项目名称关键字
     * @return 项目名称列表
     */
    public List<String> selectProjectNameList(String projectName);

    /**
     * 查询项目编号列表（用于智能提示）
     *
     * @param projectCode 项目编号关键字
     * @return 项目编号列表
     */
    public List<String> selectProjectCodeList(String projectCode);
}
