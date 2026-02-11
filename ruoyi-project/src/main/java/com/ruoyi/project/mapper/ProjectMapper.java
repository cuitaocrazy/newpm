package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.Project;

/**
 * 项目管理Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-11
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
     * 获取用户列表（按岗位过滤）
     *
     * @param postCode 岗位编码
     * @return 用户列表
     */
    public List<Map<String, Object>> selectUsersByPost(@Param("postCode") String postCode);

    /**
     * 获取二级区域列表（根据一级区域）
     *
     * @param regionDictValue 一级区域字典值
     * @return 二级区域列表
     */
    public List<Map<String, Object>> selectSecondaryRegionsByRegion(@Param("regionDictValue") String regionDictValue);

    /**
     * 获取客户列表（支持搜索）
     *
     * @param customerSimpleName 客户简称
     * @return 客户列表
     */
    public List<Map<String, Object>> selectCustomers(@Param("customerSimpleName") String customerSimpleName);

    /**
     * 获取客户联系人列表（根据客户ID）
     *
     * @param customerId 客户ID
     * @return 客户联系人列表
     */
    public List<Map<String, Object>> selectCustomerContacts(@Param("customerId") Long customerId);

    /**
     * 获取部门树（三级及以下机构）
     *
     * @return 部门树
     */
    public List<Map<String, Object>> selectDeptTree();

    /**
     * 查询待审核项目列表
     *
     * @param project 项目查询条件
     * @return 待审核项目列表
     */
    public List<Project> selectReviewList(Project project);

    /**
     * 根据部门查询项目列表（用于合同关联，可排除已关联的项目）
     *
     * @param deptId 部门ID
     * @param excludeContractId 要排除的合同ID（编辑时使用）
     * @return 项目列表
     */
    public List<Project> selectProjectListByDept(@Param("deptId") Long deptId, @Param("excludeContractId") Long excludeContractId);
}
