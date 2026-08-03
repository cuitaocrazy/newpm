package com.ruoyi.project.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
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
     * 查询项目全量成员（不过滤，sync专用）
     *
     * @param projectId 项目ID
     * @return 项目成员集合
     */
    public List<ProjectMember> selectAllMembersByProjectId(Long projectId);

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
     * 根据项目ID和用户ID列表硬删除项目成员
     *
     * <p>成员同步已改用 {@link #deactivateByProjectIdAndUserIds} 做软离场（Issue #5 ⑤），
     * 硬删会抹掉离场记录，使这些人的历史工时在团队日报里无从呈现。仅在确需彻底清除时使用。
     *
     * @param projectId 项目ID
     * @param userIds 用户ID集合
     * @return 结果
     */
    public int deleteByProjectIdAndUserIds(@Param("projectId") Long projectId, @Param("userIds") Set<Long> userIds);

    /**
     * 将指定成员置为已离场（软离场：is_active='0' 并记录 leave_date），保留历史行。
     *
     * <p>保留离场记录后，这些人过去填报的工时仍可在团队日报以「已离场」行呈现，
     * 使个人人天能与实际人天对上账（Issue #5 ③⑤）。
     *
     * @param projectId 项目ID
     * @param userIds 用户ID集合
     * @param updateBy 操作人
     * @return 结果
     */
    public int deactivateByProjectIdAndUserIds(@Param("projectId") Long projectId,
                                               @Param("userIds") Set<Long> userIds,
                                               @Param("updateBy") String updateBy);

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

    /**
     * 从给定项目集合中，筛出该用户<b>曾以任意身份参与过</b>的项目ID
     *
     * <p><b>实现约束（关键）</b>：SQL <b>不得</b>过滤 {@code is_active} 与 {@code del_flag}。
     * 成员关系一旦建立即为「曾参与」的永久凭据——离场、被移出、调岗的成员仍须能维护
     * 自己在该项目上填过的历史工时（FR-006 / User Story 4 / SC-007）。
     *
     * <p>若误加 {@code is_active='1'} 过滤，将误拒 126 组历史合法组合
     * （见 research.md Decision 3 的实测取证）。
     *
     * <p>该凭据不可由填报人自助获得——写入成员行需要项目编辑权限，故门槛不可伪造。
     *
     * @param userId     填报人用户ID
     * @param projectIds 待判定的项目ID集合（来自本次提交的明细，实测最多 6 个）
     * @return 其中该用户曾参与过的项目ID子集
     */
    List<Long> selectEverMemberProjectIds(@Param("userId") Long userId,
                                          @Param("projectIds") Collection<Long> projectIds);
}
