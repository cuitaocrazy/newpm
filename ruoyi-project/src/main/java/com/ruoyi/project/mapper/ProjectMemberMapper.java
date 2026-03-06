package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.ProjectMember;

/**
 * 项目成员Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-26
 */
public interface ProjectMemberMapper
{
    /**
     * 查询项目成员列表
     *
     * @param projectId 项目ID
     * @return 项目成员集合
     */
    public List<ProjectMember> selectMembersByProjectId(Long projectId);

    /**
     * 查询用户参与的项目ID列表
     *
     * @param userId 用户ID
     * @return 项目ID集合
     */
    public List<Long> selectProjectIdsByUserId(Long userId);

    /**
     * 新增项目成员
     *
     * @param member 项目成员
     * @return 结果
     */
    public int insertProjectMember(ProjectMember member);

    /**
     * 根据项目ID删除项目成员
     *
     * @param projectId 项目ID
     * @return 结果
     */
    public int deleteByProjectId(Long projectId);

    /**
     * 批量新增项目成员
     *
     * @param list 项目成员列表
     * @return 结果
     */
    public int batchInsert(List<ProjectMember> list);

    /**
     * 查询项目列表（带成员聚合信息）
     *
     * @param query 查询参数（projectName, deptId via params.dataScope）
     * @return 项目+成员聚合列表
     */
    public List<Map<String, Object>> selectProjectWithMembers(ProjectMember query);
}
