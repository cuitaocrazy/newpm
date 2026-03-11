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
     * 查询收入确认汇总（全量筛选，不分页）
     *
     * @param project 查询条件
     * @return 5个数值字段的合计
     */
    public Map<String, Object> selectRevenueSummary(Project project);

    /**
     * 查询项目管理列表
     *
     * @param project 项目管理
     * @return 项目管理集合
     */
    public List<Project> selectProjectList(Project project);

    /**
     * 查询项目合计（全量，不分页）
     */
    public Map<String, Object> selectProjectSummary(Project project);

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

    /** 查询子项目列表（分页，带数据权限） */
    List<Project> selectSubProjectList(Project project);

    /** 获取子项目轻量选项（下拉用） */
    List<Map<String, Object>> selectSubProjectOptions(Long parentId);

    /** 批量判断哪些主项目有子项目，返回有子项目的 projectId 数组 */
    List<Long> selectProjectsHasSubProject(List<Long> projectIds);

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
     * @param project 查询参数（用于数据权限过滤）
     * @return 部门树
     */
    public List<Map<String, Object>> getDeptTree(Project project);

    /**
     * 获取全量部门树（不限数据权限，用于参与人员选择）
     * @return 部门树
     */
    public List<Map<String, Object>> getAllDeptTree();

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
    public List<Map<String, Object>> searchProjectsByName(String projectName, String projectDept);

    /**
     * 为导出填充附加字段：部门路径、参与人员名称、更新时间显示
     *
     * @param list 项目列表
     */
    public void enrichForExport(List<Project> list);

    /**
     * 查询团队收入确认平铺列表（以团队为维度）
     *
     * @param project 查询条件
     * @return 平铺列表
     */
    public List<Map<String, Object>> selectTeamRevenueFlatList(Project project);

    /**
     * 查询团队收入确认平铺合计
     *
     * @param project 查询条件
     * @return 合计（teamConfirmAmount）
     */
    public Map<String, Object> selectTeamRevenueFlatSummary(Project project);

    /**
     * 检查项目编号是否与已有项目冲突，并返回建议编号
     *
     * @param projectCode      待检查的基础编号
     * @param excludeProjectId 编辑时排除自身（新增传 null）
     * @return Map 包含：exists(boolean), existingProject(Map{projectCode,projectName}), suggestedCode(String)
     */
    public Map<String, Object> checkProjectCode(String projectCode, Long excludeProjectId);

    /**
     * 关联合同到项目
     */
    public void bindContractToProject(Long projectId, Long contractId);

    public List<String> searchTaskCode(String taskCode);

    public List<String> searchTaskName(String projectName);
}
