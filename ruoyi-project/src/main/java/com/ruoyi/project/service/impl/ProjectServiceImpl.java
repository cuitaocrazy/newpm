package com.ruoyi.project.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.mapper.ProjectMemberMapper;
import com.ruoyi.project.mapper.ContractMapper;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.ProjectMember;
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

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

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
     * 查询收入确认汇总（全量筛选，不分页）
     */
    @Override
    public Map<String, Object> selectRevenueSummary(Project project)
    {
        return projectMapper.selectRevenueSummary(project);
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
    @Transactional
    @Override
    public int insertProject(Project project)
    {
        project.setCreateTime(DateUtils.getNowDate());
        int rows = projectMapper.insertProject(project);
        syncProjectMembers(project);
        return rows;
    }

    /**
     * 修改项目管理
     *
     * @param project 项目管理
     * @return 结果
     */
    @Transactional
    @Override
    public int updateProject(Project project)
    {
        project.setUpdateTime(DateUtils.getNowDate());
        int rows = projectMapper.updateProject(project);
        syncProjectMembers(project);
        return rows;
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

    /**
     * 同步项目成员到 pm_project_member 表
     * 从项目的项目经理、市场经理、团队负责人、参与人字段中收集所有用户ID，
     * 先删除旧成员再批量插入新成员。
     *
     * @param project 项目对象
     */
    private void syncProjectMembers(Project project)
    {
        Long projectId = project.getProjectId();
        if (projectId == null)
        {
            return;
        }

        // 1. 删除该项目的旧成员记录
        projectMemberMapper.deleteByProjectId(projectId);

        // 2. 收集所有关联的用户ID（使用LinkedHashSet去重并保持顺序）
        Set<Long> userIds = new LinkedHashSet<>();

        if (project.getProjectManagerId() != null)
        {
            userIds.add(project.getProjectManagerId());
        }
        if (project.getMarketManagerId() != null)
        {
            userIds.add(project.getMarketManagerId());
        }
        if (project.getTeamLeaderId() != null)
        {
            userIds.add(project.getTeamLeaderId());
        }

        // 解析参与人（逗号分隔的用户ID字符串，如 "1,2,3"）
        String participants = project.getParticipants();
        if (StringUtils.isNotEmpty(participants))
        {
            String[] parts = participants.split(",");
            for (String part : parts)
            {
                String trimmed = part.trim();
                if (StringUtils.isNotEmpty(trimmed))
                {
                    try
                    {
                        userIds.add(Long.parseLong(trimmed));
                    }
                    catch (NumberFormatException ignored)
                    {
                        // 跳过非法的用户ID
                    }
                }
            }
        }

        // 3. 构建成员列表并批量插入
        if (!userIds.isEmpty())
        {
            List<ProjectMember> members = new ArrayList<>();
            Date now = new Date();
            String createBy = SecurityUtils.getUsername();

            for (Long userId : userIds)
            {
                ProjectMember member = new ProjectMember();
                member.setProjectId(projectId);
                member.setUserId(userId);
                member.setJoinDate(now);
                member.setIsActive("1");
                member.setCreateBy(createBy);
                members.add(member);
            }

            projectMemberMapper.batchInsert(members);
        }
    }
}
