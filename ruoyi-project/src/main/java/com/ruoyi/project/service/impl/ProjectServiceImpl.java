package com.ruoyi.project.service.impl;

import java.util.List;
import java.util.Map;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.mapper.ContractMapper;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.service.IProjectService;

/**
 * 项目管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-11
 */
@Service
public class ProjectServiceImpl implements IProjectService
{
    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ContractMapper contractMapper;

    /**
     * 查询项目管理
     *
     * @param projectId 项目管理主键
     * @return 项目管理
     */
    @Override
    public Project selectProjectByProjectId(Long projectId)
    {
        return projectMapper.selectProjectByProjectId(projectId);
    }

    /**
     * 查询项目管理列表
     *
     * @param project 项目管理
     * @return 项目管理
     */
    @Override
    public List<Project> selectProjectList(Project project)
    {
        return projectMapper.selectProjectList(project);
    }

    /**
     * 新增项目管理
     *
     * @param project 项目管理
     * @return 结果
     */
    @Override
    public int insertProject(Project project)
    {
        project.setCreateTime(DateUtils.getNowDate());
        return projectMapper.insertProject(project);
    }

    /**
     * 修改项目管理
     *
     * @param project 项目管理
     * @return 结果
     */
    @Override
    public int updateProject(Project project)
    {
        project.setUpdateTime(DateUtils.getNowDate());
        return projectMapper.updateProject(project);
    }

    /**
     * 批量删除项目管理
     *
     * @param projectIds 需要删除的项目管理主键
     * @return 结果
     */
    @Override
    public int deleteProjectByProjectIds(Long[] projectIds)
    {
        return projectMapper.deleteProjectByProjectIds(projectIds);
    }

    /**
     * 删除项目管理信息
     *
     * @param projectId 项目管理主键
     * @return 结果
     */
    @Override
    public int deleteProjectByProjectId(Long projectId)
    {
        return projectMapper.deleteProjectByProjectId(projectId);
    }

    /**
     * 获取用户列表（按岗位过滤）
     *
     * @param postCode 岗位编码
     * @return 用户列表
     */
    @Override
    public List<Map<String, Object>> getUsersByPost(String postCode)
    {
        return projectMapper.selectUsersByPost(postCode);
    }

    /**
     * 获取二级区域列表（根据一级区域）
     *
     * @param regionDictValue 一级区域字典值
     * @return 二级区域列表
     */
    @Override
    public List<Map<String, Object>> getSecondaryRegionsByRegion(String regionDictValue)
    {
        return projectMapper.selectSecondaryRegionsByRegion(regionDictValue);
    }

    /**
     * 获取客户列表（支持搜索）
     *
     * @param customerSimpleName 客户简称
     * @return 客户列表
     */
    @Override
    public List<Map<String, Object>> getCustomers(String customerSimpleName)
    {
        return projectMapper.selectCustomers(customerSimpleName);
    }

    /**
     * 获取客户联系人列表（根据客户ID）
     *
     * @param customerId 客户ID
     * @return 客户联系人列表
     */
    @Override
    public List<Map<String, Object>> getCustomerContacts(Long customerId)
    {
        return projectMapper.selectCustomerContacts(customerId);
    }

    /**
     * 获取部门树（三级及以下机构）
     *
     * @return 部门树
     */
    @Override
    public List<Map<String, Object>> getDeptTree()
    {
        return projectMapper.selectDeptTree();
    }

    /**
     * 生成项目编号
     * 格式：{行业代码}-{一级区域代码}-{二级区域代码}-{简称}-{立项年份}
     *
     * @param industryCode 行业代码
     * @param regionCode 一级区域代码
     * @param provinceCode 二级区域代码
     * @param shortName 简称
     * @param establishedYear 立项年份
     * @return 项目编号
     */
    @Override
    public String generateProjectCode(String industryCode, String regionCode, String provinceCode, String shortName, String establishedYear)
    {
        StringBuilder code = new StringBuilder();

        if (StringUtils.isNotEmpty(industryCode))
        {
            code.append(industryCode);
        }

        if (StringUtils.isNotEmpty(regionCode))
        {
            if (code.length() > 0) code.append("-");
            code.append(regionCode);
        }

        if (StringUtils.isNotEmpty(provinceCode))
        {
            if (code.length() > 0) code.append("-");
            code.append(provinceCode);
        }

        if (StringUtils.isNotEmpty(shortName))
        {
            if (code.length() > 0) code.append("-");
            code.append(shortName);
        }

        if (StringUtils.isNotEmpty(establishedYear))
        {
            if (code.length() > 0) code.append("-");
            code.append(establishedYear);
        }

        return code.toString();
    }

    /**
     * 根据部门查询项目列表（用于合同关联，可排除已关联的项目）
     *
     * @param deptId 部门ID
     * @param excludeContractId 要排除的合同ID（编辑时使用）
     * @return 项目列表
     */
    @Override
    public List<Project> selectProjectListByDept(Long deptId, Long excludeContractId)
    {
        return projectMapper.selectProjectListByDept(deptId, excludeContractId);
    }

    /**
     * 根据项目ID查询关联的合同信息
     *
     * @param projectId 项目ID
     * @return 合同信息，无关联合同时返回null
     */
    @Override
    public Contract selectContractByProjectId(Long projectId)
    {
        // 1. 通过项目合同关联表查询合同ID
        Long contractId = contractMapper.selectContractIdByProjectId(projectId);
        if (contractId == null)
        {
            return null;
        }

        // 2. 根据合同ID查询合同详情
        return contractMapper.selectContractByContractId(contractId);
    }

    /**
     * 项目搜索（轻量接口，用于 autocomplete）
     *
     * @param projectName 项目名称（模糊搜索）
     * @return 精简字段列表：projectId, projectName, projectCode
     */
    @Override
    public List<Map<String, Object>> searchProjectsByName(String projectName)
    {
        return projectMapper.searchProjectsByName(projectName);
    }
}
