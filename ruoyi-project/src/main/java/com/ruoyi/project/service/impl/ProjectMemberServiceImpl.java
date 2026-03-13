package com.ruoyi.project.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
     * 更新项目成员（删旧插新 + 同步 pm_project.participants）
     */
    @Override
    @Transactional
    public int updateProjectMembers(Long projectId, Long[] userIds)
    {
        // 1. 删除旧成员
        projectMemberMapper.deleteByProjectId(projectId);

        // 2. 批量插入新成员
        if (userIds != null && userIds.length > 0)
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
                member.setCreateTime(now);
                members.add(member);
            }
            projectMemberMapper.batchInsert(members);
        }

        // 3. 同步更新 pm_project.participants 字段
        String participants = "";
        if (userIds != null && userIds.length > 0)
        {
            participants = Arrays.stream(userIds)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        }

        Project project = new Project();
        project.setProjectId(projectId);
        project.setParticipants(participants);
        project.setUpdateBy(SecurityUtils.getUsername());
        project.setUpdateTime(new Date());
        return projectMapper.updateProject(project);
    }
}
