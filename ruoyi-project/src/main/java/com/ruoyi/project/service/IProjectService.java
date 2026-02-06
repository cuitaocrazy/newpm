package com.ruoyi.project.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.Project;

/**
 * 项目管理Service接口
 *
 * @author ruoyi
 * @date 2026-02-05
 */
public interface IProjectService
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
     * 批量删除项目管理
     * 
     * @param projectIds 需要删除的项目管理主键集合
     * @return 结果
     */
    public int deleteProjectByProjectIds(Long[] projectIds);

    /**
     * 删除项目管理信息
     *
     * @param projectId 项目管理主键
     * @return 结果
     */
    public int deleteProjectByProjectId(Long projectId);

    /**
     * 根据部门查询项目列表（用于下拉选择）
     *
     * @param deptId 部门ID
     * @param excludeContractId 排除已关联此合同的项目（编辑时使用）
     * @return 项目列表
     */
    public List<Project> selectProjectListByDept(Long deptId, Long excludeContractId);

    /**
     * 获取项目名称列表（用于智能提示）
     *
     * @param projectName 项目名称关键词
     * @return 项目名称列表
     */
    public List<String> selectProjectNameList(String projectName);

    /**
     * 获取项目编号列表（用于智能提示）
     *
     * @param projectCode 项目编号关键词
     * @return 项目编号列表
     */
    public List<String> selectProjectCodeList(String projectCode);

    /**
     * 获取项目金额汇总
     *
     * @param project 查询条件
     * @return 汇总数据
     */
    public Map<String, BigDecimal> selectProjectSummary(Project project);
}
