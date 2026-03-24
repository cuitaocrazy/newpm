package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ruoyi.project.domain.ProjectMember;

/**
 * 项目人员管理Service接口
 *
 * @author ruoyi
 * @date 2026-02-28
 */
public interface IProjectMemberService
{
    /**
     * 查询项目列表（带成员聚合信息，用于分页列表）
     *
     * @param query 查询参数（projectName, deptId）
     * @return 项目+成员聚合列表
     */
    public List<Map<String, Object>> selectProjectMemberList(ProjectMember query);

    /**
     * 查询项目成员详情（完整成员列表）
     *
     * @param projectId 项目ID
     * @return 项目成员集合
     */
    public List<ProjectMember> selectProjectMemberDetail(Long projectId);

    /**
     * 更新项目成员（同步更新 pm_project_member 和 pm_project.participants）
     *
     * @param projectId 项目ID
     * @param userIds 用户ID数组
     * @return 结果
     */
    public int updateProjectMembers(Long projectId, Long[] userIds);

    /**
     * 增量同步 pm_project_member（只做成员表的增删，不触碰 pm_project）
     *
     * @param projectId 项目ID
     * @param targetUserIds 目标用户ID集合
     */
    public void syncMembers(Long projectId, Set<Long> targetUserIds);
}
