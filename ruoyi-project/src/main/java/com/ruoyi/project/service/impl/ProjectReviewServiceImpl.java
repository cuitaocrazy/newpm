package com.ruoyi.project.service.impl;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.mapper.ProjectApprovalMapper;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.ProjectApproval;
import com.ruoyi.project.service.IProjectReviewService;
import com.ruoyi.project.service.IProjectEmailService;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;

/**
 * 项目审核Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-08
 */
@Service
public class ProjectReviewServiceImpl implements IProjectReviewService
{
    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectApprovalMapper projectApprovalMapper;

    @Autowired
    private IProjectEmailService projectEmailService;

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 查询待审核项目列表
     *
     * @param project 项目查询条件
     * @return 待审核项目列表
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u1")
    public List<Project> selectReviewList(Project project)
    {
        List<Project> list = projectMapper.selectReviewList(project);
        // 为每个项目构建完整的部门层级路径
        for (Project p : list) {
            if (p.getDeptName() != null && p.getProjectDept() != null) {
                String fullDeptPath = buildDeptPath(p.getProjectDept());
                if (fullDeptPath != null) {
                    p.setDeptName(fullDeptPath);
                }
            }
        }
        return list;
    }

    /**
     * 构建部门层级路径（三级机构及以下，用"-"分隔）
     *
     * @param deptId 部门ID（字符串格式）
     * @return 完整的部门路径，如"一级-二级-三级"
     */
    private String buildDeptPath(String deptId) {
        try {
            Long deptIdLong = Long.parseLong(deptId);
            SysDept dept = sysDeptMapper.selectDeptById(deptIdLong);
            if (dept == null || dept.getAncestors() == null) {
                return dept != null ? dept.getDeptName() : null;
            }

            // 获取祖先部门ID列表
            String ancestors = dept.getAncestors();
            String[] ancestorIds = ancestors.split(",");

            // 过滤掉根节点（0）和前两级（假设是公司和总部），只保留三级及以下
            List<String> deptNames = Arrays.stream(ancestorIds)
                .filter(id -> !"0".equals(id)) // 过滤根节点
                .skip(2) // 跳过前两级
                .map(id -> {
                    SysDept ancestorDept = sysDeptMapper.selectDeptById(Long.parseLong(id));
                    return ancestorDept != null ? ancestorDept.getDeptName() : null;
                })
                .filter(name -> name != null)
                .collect(Collectors.toList());

            // 添加当前部门
            deptNames.add(dept.getDeptName());

            // 用"-"连接
            return String.join("-", deptNames);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询项目详细信息（用于审核）
     *
     * @param projectId 项目ID
     * @return 项目详细信息
     */
    @Override
    public Project selectProjectById(Long projectId)
    {
        Project project = projectMapper.selectProjectByProjectId(projectId);
        if (project != null) {
            // 构建完整的部门层级路径
            if (project.getProjectDept() != null) {
                String fullDeptPath = buildDeptPath(project.getProjectDept());
                if (fullDeptPath != null) {
                    project.setDeptName(fullDeptPath);
                }
            }

            // 构建参与人员名称列表
            if (StringUtils.isNotEmpty(project.getParticipants())) {
                String participantsNames = buildParticipantsNames(project.getParticipants());
                project.setParticipantsNames(participantsNames);
            }
        }
        return project;
    }

    /**
     * 构建参与人员名称列表
     *
     * @param participants 参与人员ID列表（逗号分隔）
     * @return 参与人员名称列表（逗号分隔）
     */
    private String buildParticipantsNames(String participants) {
        try {
            String[] userIds = participants.split(",");
            List<String> userNames = Arrays.stream(userIds)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(id -> {
                    try {
                        SysUser user = sysUserMapper.selectUserById(Long.parseLong(id));
                        return user != null ? user.getNickName() : null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(name -> name != null)
                .collect(Collectors.toList());

            return String.join(", ", userNames);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 审核项目
     *
     * @param projectId 项目ID
     * @param approvalStatus 审核状态（1通过/2拒绝）
     * @param approvalReason 审核意见
     * @return 结果
     */
    @Override
    @Transactional
    public int approveProject(Long projectId, String approvalStatus, String approvalReason)
    {
        // 1. 更新 pm_project 表（保存最新审核状态）
        Project project = new Project();
        project.setProjectId(projectId);
        project.setApprovalStatus(approvalStatus);
        project.setApprovalReason(approvalReason);
        project.setApproverId(String.valueOf(SecurityUtils.getUserId()));
        project.setApprovalTime(DateUtils.getNowDate());
        project.setUpdateBy(SecurityUtils.getUsername());
        project.setUpdateTime(DateUtils.getNowDate());

        int result = projectMapper.updateProject(project);

        // 2. 插入 pm_project_approval 表（保存审核历史记录）
        if (result > 0)
        {
            ProjectApproval approval = new ProjectApproval();
            approval.setProjectId(projectId);
            approval.setApprovalStatus(approvalStatus);
            approval.setApprovalReason(approvalReason);
            approval.setApproverId(SecurityUtils.getUserId());
            approval.setApprovalTime(DateUtils.getNowDate());
            approval.setCreateBy(SecurityUtils.getUsername());
            approval.setCreateTime(DateUtils.getNowDate());

            projectApprovalMapper.insertProjectApproval(approval);

            // 3. 异步发送邮件通知
            projectEmailService.sendApprovalNotification(projectId, approvalStatus, approvalReason);
        }

        return result;
    }
}
