package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.Contract;

/**
 * 项目管理Service接口
 *
 * @author ruoyi
 * @date 2026-02-11
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
     * 获取用户列表（按岗位过滤）
     *
     * @param postCode 岗位编码：pm-项目经理, scjl-市场经理, xsfzr-销售负责人
     * @return 用户列表
     */
    public List<Map<String, Object>> getUsersByPost(String postCode);

    /**
     * 获取二级区域列表（根据一级区域）
     *
     * @param regionDictValue 一级区域字典值
     * @return 二级区域列表
     */
    public List<Map<String, Object>> getSecondaryRegionsByRegion(String regionDictValue);

    /**
     * 获取客户列表（支持搜索）
     *
     * @param customerSimpleName 客户简称（模糊搜索）
     * @return 客户列表
     */
    public List<Map<String, Object>> getCustomers(String customerSimpleName);

    /**
     * 获取客户联系人列表（根据客户ID）
     *
     * @param customerId 客户ID
     * @return 客户联系人列表
     */
    public List<Map<String, Object>> getCustomerContacts(Long customerId);

    /**
     * 获取部门树（三级及以下机构）
     *
     * @return 部门树
     */
    public List<Map<String, Object>> getDeptTree();

    /**
     * 生成项目编号
     *
     * @param industryCode 行业代码
     * @param regionCode 一级区域代码
     * @param provinceCode 二级区域代码
     * @param shortName 简称
     * @param establishedYear 立项年份
     * @return 项目编号
     */
    public String generateProjectCode(String industryCode, String regionCode, String provinceCode, String shortName, String establishedYear);

    /**
     * 根据部门查询项目列表（用于合同关联，可排除已关联的项目）
     *
     * @param deptId 部门ID
     * @param excludeContractId 要排除的合同ID（编辑时使用）
     * @return 项目列表
     */
    public List<Project> selectProjectListByDept(Long deptId, Long excludeContractId);

    /**
     * 根据项目ID查询关联的合同信息
     *
     * @param projectId 项目ID
     * @return 合同信息，无关联合同时返回null
     */
    public Contract selectContractByProjectId(Long projectId);

    /**
     * 项目搜索（轻量接口，用于 autocomplete）
     *
     * @param projectName 项目名称（模糊搜索）
     * @return 精简字段列表：projectId, projectName, projectCode
     */
    public List<Map<String, Object>> searchProjectsByName(String projectName);
}
