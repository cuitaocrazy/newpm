package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
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
     * @param params 查询参数（projectName, deptId）
     * @return 项目+成员聚合列表
     */
    public List<Map<String, Object>> selectProjectMemberList(Map<String, Object> params);

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
}
