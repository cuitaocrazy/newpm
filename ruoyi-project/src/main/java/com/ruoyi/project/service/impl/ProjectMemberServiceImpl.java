package com.ruoyi.project.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.ProjectMember;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.mapper.ProjectMemberMapper;
import com.ruoyi.project.service.IProjectMemberService;

/**
 * 项目人员管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-28
 */
@Service
public class ProjectMemberServiceImpl implements IProjectMemberService
{
    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Autowired
    private ProjectMapper projectMapper;

    /**
     * 查询项目列表（带成员聚合信息）
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u_create")
    public List<Map<String, Object>> selectProjectMemberList(ProjectMember query)
    {
        return projectMemberMapper.selectProjectWithMembers(query);
    }

    /**
     * 查询项目成员详情
     */
    @Override
    public List<ProjectMember> selectProjectMemberDetail(Long projectId)
    {
        return projectMemberMapper.selectMembersByProjectId(projectId);
    }

    /**
     * 更新项目成员（同步 pm_project_member + pm_project.participants）
     */
    @Override
    @Transactional
    public int updateProjectMembers(Long projectId, Long[] userIds)
    {
        Set<Long> targetUserIds = new LinkedHashSet<>();
        if (userIds != null)
        {
            for (Long uid : userIds)
            {
                targetUserIds.add(uid);
            }
        }

        // 1. 增量同步成员表
        syncMembers(projectId, targetUserIds);

        // 2. 同步更新 pm_project.participants 字段（不触碰 update_by / update_time）
        String participants = targetUserIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return projectMapper.updateProjectParticipants(projectId, participants);
    }

    /**
     * 增量同步 pm_project_member（只做成员表的增删，不触碰 pm_project）
     */
    @Override
    public void syncMembers(Long projectId, Set<Long> targetUserIds)
    {
        List<ProjectMember> existingMembers = projectMemberMapper.selectAllMembersByProjectId(projectId);
        Set<Long> existingUserIds = existingMembers.stream()
                .map(ProjectMember::getUserId)
                .collect(Collectors.toSet());

        Set<Long> toRemove = new LinkedHashSet<>(existingUserIds);
        toRemove.removeAll(targetUserIds);

        Set<Long> toAdd = new LinkedHashSet<>(targetUserIds);
        toAdd.removeAll(existingUserIds);

        if (!toRemove.isEmpty())
        {
            projectMemberMapper.deleteByProjectIdAndUserIds(projectId, toRemove);
        }

        if (!toAdd.isEmpty())
        {
            List<ProjectMember> members = new ArrayList<>();
            Date now = new Date();
            String createBy = SecurityUtils.getUsername();
            for (Long userId : toAdd)
            {
                ProjectMember member = new ProjectMember();
                member.setProjectId(projectId);
                member.setUserId(userId);
                member.setJoinDate(now);
                member.setIsActive("1");
                member.setCreateBy(createBy);
                member.setCreateTime(now);
                members.add(member);
            }
            projectMemberMapper.batchInsert(members);
        }
    }
}
