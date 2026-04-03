package com.ruoyi.project.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.mapper.ContractMapper;
import com.ruoyi.project.mapper.ProjectContractRelMapper;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.domain.ProjectContractRel;
import com.ruoyi.project.service.IProjectMemberService;
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
    private ProjectContractRelMapper projectContractRelMapper;

    @Autowired
    private IProjectMemberService projectMemberService;

    @Autowired
    private com.ruoyi.project.mapper.TaskMapper taskMapper;

    @Autowired
    private com.ruoyi.project.mapper.ProjectMemberMapper memberMapper;

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
    @DataScope(deptAlias = "d", userAlias = "u_create")
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
    @DataScope(deptAlias = "d", userAlias = "u_create")
    public List<Project> selectProjectList(Project project)
    {
        List<Project> list = projectMapper.selectProjectList(project);
        if (!list.isEmpty()) {
            List<Long> ids = list.stream()
                .map(Project::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            List<Map<String, Object>> details = projectMapper.selectTeamConfirmDetailsByIds(ids);
            Map<Long, List<Map<String, Object>>> detailMap = details.stream()
                .collect(Collectors.groupingBy(d -> Long.parseLong(d.get("projectId").toString())));
            for (Project p : list) {
                if (p.getProjectId() != null) {
                    p.setTeamConfirmList(detailMap.getOrDefault(p.getProjectId(), Collections.emptyList()));
                }
            }
        }
        return list;
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "u_create")
    public Map<String, Object> selectProjectSummary(Project project)
    {
        return projectMapper.selectProjectSummary(project);
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
        String username = SecurityUtils.getUsername();
        Date now = DateUtils.getNowDate();
        project.setCreateBy(username);
        project.setCreateTime(now);
        project.setUpdateBy(username);
        project.setUpdateTime(now);
        // 校验 project_code 长度
        if (project.getProjectCode() != null && project.getProjectCode().length() > 500) {
            throw new com.ruoyi.common.exception.ServiceException("项目编号过长（超过500字符），请缩短项目简称");
        }
        // 立项时自动初始化收入确认年度和收入确认状态
        if (project.getRevenueConfirmYear() == null) {
            project.setRevenueConfirmYear("dd");   // sys_ndgl 待定
        }
        if (project.getRevenueConfirmStatus() == null) {
            project.setRevenueConfirmStatus("0");  // sys_qrzt 待定
        }
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
        // 校验 project_code 长度
        if (project.getProjectCode() != null && project.getProjectCode().length() > 500) {
            throw new com.ruoyi.common.exception.ServiceException("项目编号过长（超过500字符），请缩短项目简称");
        }
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
    @Transactional
    public int deleteProjectByProjectIds(Long[] projectIds)
    {
        for (Long projectId : projectIds) {
            // 检查是否有关联合同
            Long contractId = contractMapper.selectContractIdByProjectId(projectId);
            if (contractId != null) {
                throw new ServiceException("项目已关联合同，请先解除合同关联后再删除");
            }
            // 检查是否有子任务
            int taskCount = taskMapper.countTasksByProjectId(projectId);
            if (taskCount > 0) {
                throw new ServiceException("项目下存在子任务，请先删除子任务后再删除项目");
            }
            // 级联删除项目成员
            memberMapper.deleteByProjectId(projectId);
        }
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

    @Override
    public List<Map<String, Object>> selectParticipantsWithWorkload(Long projectId) {
        return projectMapper.selectParticipantsWithWorkload(projectId);
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
    @DataScope(deptAlias = "d")
    public List<Map<String, Object>> getDeptTree(Project project)
    {
        return projectMapper.selectDeptTree(project);
    }

    @Override
    public List<Map<String, Object>> getAllDeptTree()
    {
        return projectMapper.selectAllDeptTree();
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
     * 检查项目编号是否与已有项目冲突，并返回建议编号
     */
    @Override
    public Map<String, Object> checkProjectCode(String projectCode, Long excludeProjectId)
    {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> existing = projectMapper.selectProjectsByCodePrefix(projectCode, excludeProjectId);

        // 精确匹配：是否有完全相同的编号
        Map<String, Object> exactMatch = existing.stream()
            .filter(p -> projectCode.equals(p.get("projectCode")))
            .findFirst()
            .orElse(null);

        if (exactMatch == null)
        {
            result.put("exists", false);
            result.put("suggestedCode", projectCode);
            return result;
        }

        // 有冲突 → 找后缀最大值，推荐下一个可用编号
        result.put("exists", true);
        result.put("existingProject", exactMatch);

        int maxSuffix = existing.stream()
            .map(p -> String.valueOf(p.get("projectCode")))
            .filter(code -> code.matches(projectCode + "-\\d{2}"))
            .mapToInt(code -> Integer.parseInt(code.substring(code.lastIndexOf("-") + 1)))
            .max()
            .orElse(0);

        result.put("suggestedCode", String.format("%s-%02d", projectCode, maxSuffix + 1));
        return result;
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
    @DataScope(deptAlias = "d", userAlias = "u_create")
    public List<Map<String, Object>> searchProjectsByName(Project project)
    {
        return projectMapper.searchProjectsByName(project);
    }

    /**
     * 为导出填充附加字段：部门路径、参与人员名称、更新时间显示
     *
     * @param list 项目列表
     */
    @Override
    public void enrichForExport(List<Project> list)
    {
        if (list == null || list.isEmpty())
        {
            return;
        }

        // 1. 加载全部部门，构建 deptId -> {deptName, ancestors} 映射
        List<Map<String, Object>> allDepts = projectMapper.selectAllDeptsForPath();
        Map<Long, Map<String, Object>> deptMapById = new HashMap<>();
        for (Map<String, Object> dept : allDepts)
        {
            Object id = dept.get("deptId");
            if (id != null)
            {
                deptMapById.put(Long.parseLong(id.toString()), dept);
            }
        }

        // 2. 收集所有参与人员 ID（逗号分隔字符串解析）
        Set<Long> allParticipantIds = new LinkedHashSet<>();
        for (Project p : list)
        {
            if (StringUtils.isNotEmpty(p.getParticipants()))
            {
                for (String idStr : p.getParticipants().split(","))
                {
                    String trimmed = idStr.trim();
                    if (StringUtils.isNotEmpty(trimmed))
                    {
                        try { allParticipantIds.add(Long.parseLong(trimmed)); } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        // 3. 批量查询参与人员昵称
        Map<Long, String> userNickMap = new HashMap<>();
        if (!allParticipantIds.isEmpty())
        {
            List<Map<String, Object>> userList = projectMapper.selectUserNickNamesByIds(new ArrayList<>(allParticipantIds));
            for (Map<String, Object> u : userList)
            {
                Object uid = u.get("userId");
                Object nick = u.get("nickName");
                if (uid != null && nick != null)
                {
                    userNickMap.put(Long.parseLong(uid.toString()), nick.toString());
                }
            }
        }

        // 4. 逐项目填充导出字段
        for (Project p : list)
        {
            // 4a. 部门路径
            if (StringUtils.isNotEmpty(p.getProjectDept()))
            {
                try
                {
                    long deptId = Long.parseLong(p.getProjectDept().trim());
                    Map<String, Object> dept = deptMapById.get(deptId);
                    if (dept != null)
                    {
                        String ancestors = (String) dept.get("ancestors");
                        List<Long> fullPath = new ArrayList<>();
                        if (StringUtils.isNotEmpty(ancestors))
                        {
                            for (String anc : ancestors.split(","))
                            {
                                try { fullPath.add(Long.parseLong(anc.trim())); } catch (NumberFormatException ignored) {}
                            }
                        }
                        fullPath.add(deptId);
                        // 从第2个索引起显示（跳过根节点和一级节点），按层级分列
                        List<Long> displayIds = fullPath.size() > 2 ? fullPath.subList(2, fullPath.size()) : fullPath;
                        // 将路径分解为层级字段（二级机构=index0, 三级机构=index1, ...）
                        String[] levelSetters = new String[displayIds.size()];
                        for (int i = 0; i < displayIds.size(); i++) {
                            Map<String, Object> d = deptMapById.get(displayIds.get(i));
                            levelSetters[i] = d != null ? d.get("deptName").toString() : String.valueOf(displayIds.get(i));
                        }
                        // 保留完整路径列
                        p.setDeptPathDisplay(String.join(" - ", levelSetters));
                        if (levelSetters.length > 0) p.setDeptOrgLevel2(levelSetters[0]);
                        if (levelSetters.length > 1) p.setDeptOrgLevel3(levelSetters[1]);
                        if (levelSetters.length > 2) p.setDeptOrgLevel4(levelSetters[2]);
                        if (levelSetters.length > 3) p.setDeptOrgLevel5(levelSetters[3]);
                        if (levelSetters.length > 4) p.setDeptOrgLevel6(levelSetters[4]);
                    }
                }
                catch (NumberFormatException ignored) {}
            }

            // 4b. 参与人员名称
            if (StringUtils.isNotEmpty(p.getParticipants()))
            {
                List<String> names = new ArrayList<>();
                for (String idStr : p.getParticipants().split(","))
                {
                    try
                    {
                        long uid = Long.parseLong(idStr.trim());
                        String nick = userNickMap.get(uid);
                        if (nick != null) names.add(nick);
                    }
                    catch (NumberFormatException ignored) {}
                }
                p.setParticipantsNames(String.join("、", names));
            }

            // 4c. 更新时间
            p.setUpdateTimeDisplay(p.getUpdateTime());
        }
    }

    @Override
    public List<Map<String, Object>> selectTeamRevenueFlatList(Project project)
    {
        return projectMapper.selectTeamRevenueFlatList(project);
    }

    @Override
    public Map<String, Object> selectTeamRevenueFlatSummary(Project project)
    {
        return projectMapper.selectTeamRevenueFlatSummary(project);
    }

    /**
     * 同步项目成员到 pm_project_member 表     * 从项目的项目经理、市场经理、团队负责人、参与人字段中收集所有用户ID，
     * 先删除旧成员再批量插入新成员。
     *
     * @param project 项目对象
     */
    @Override
    public void syncProjectMembers(Project project)
    {
        Long projectId = project.getProjectId();
        if (projectId == null)
        {
            return;
        }

        // 收集所有关联的用户ID（项目经理 + 市场经理 + 销售经理 + 团队负责人 + 参与人）
        Set<Long> userIds = new LinkedHashSet<>();

        if (project.getProjectManagerId() != null)
        {
            userIds.add(project.getProjectManagerId());
        }
        if (project.getMarketManagerId() != null)
        {
            userIds.add(project.getMarketManagerId());
        }
        if (project.getSalesManagerId() != null)
        {
            userIds.add(project.getSalesManagerId());
        }
        if (project.getTeamLeaderId() != null)
        {
            userIds.add(project.getTeamLeaderId());
        }

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
                    catch (NumberFormatException ignored) {}
                }
            }
        }

        // 委托给 ProjectMemberService 做增量同步
        projectMemberService.syncMembers(projectId, userIds);
    }

    @Override
    @Transactional
    public void unbindContractFromProject(Long projectId)
    {
        projectContractRelMapper.invalidateByProjectId(projectId);
    }

    @Override
    @Transactional
    public void bindContractToProject(Long projectId, Long contractId)
    {
        // 先将该项目已有的有效关联置为失效
        projectContractRelMapper.invalidateByProjectId(projectId);
        // 新建关联
        ProjectContractRel rel = new ProjectContractRel();
        rel.setProjectId(projectId.toString());
        rel.setContractId(contractId);
        rel.setRelStatus("有效");
        rel.setBindDate(new Date());
        rel.setDelFlag("0");
        rel.setCreateBy(SecurityUtils.getUsername());
        rel.setCreateTime(DateUtils.getNowDate());
        projectContractRelMapper.insertProjectContractRel(rel);
    }
}
