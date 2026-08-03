package com.ruoyi.project.mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.Project;

/**
 * 项目管理Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-11
 */
public interface ProjectMapper
{
    /**
     * 查询项目管理
     *
     * @param projectId 项目管理主键
     * @return 项目管理
     */
    public Project selectProjectByProjectId(Long projectId);

    /**
     * 查询收入确认汇总（全量筛选，不分页）
     *
     * @param project 查询条件
     * @return 5个数值字段的合计
     */
    public Map<String, Object> selectRevenueSummary(Project project);

    /**
     * 查询项目管理列表
     *
     * @param project 项目管理
     * @return 项目管理集合
     */
    public List<Project> selectProjectList(Project project);

    /**
     * 查询项目合计（全量，不分页）
     */
    public Map<String, Object> selectProjectSummary(Project project);

    /**
     * 新增项目管理
     *
     * @param project 项目管理
     * @return 结果
     */
    public int insertProject(Project project);

    /**
     * 仅更新项目审核相关字段（不触碰 update_by / update_time）
     */
    public int updateProjectApprovalFields(@Param("projectId") Long projectId,
                                           @Param("approvalStatus") String approvalStatus,
                                           @Param("approvalReason") String approvalReason,
                                           @Param("approvalTime") java.util.Date approvalTime,
                                           @Param("approverId") String approverId);

    /**
     * 仅更新项目参与人字段（不触碰 update_by / update_time）
     */
    public int updateProjectParticipants(@Param("projectId") Long projectId,
                                         @Param("participants") String participants);

    /**
     * 修改项目管理
     *
     * @param project 项目管理
     * @return 结果
     */
    public int updateProject(Project project);

    /**
     * 更新主项目实际工作量(小时)——由日报保存时汇总子任务后调用
     *
     * @param projectId      项目ID
     * @param actualWorkload 实际工作量(小时)
     * @return 结果
     */
    public int updateActualWorkload(@Param("projectId") Long projectId, @Param("actualWorkload") BigDecimal actualWorkload);

    /**
     * 仅更新项目经理（不触碰 update_by / update_time）
     */
    public int updateProjectManagerId(@Param("projectId") Long projectId, @Param("projectManagerId") Long projectManagerId);

    /**
     * 删除项目管理
     *
     * @param projectId 项目管理主键
     * @return 结果
     */
    public int deleteProjectByProjectId(Long projectId);

    /**
     * 批量删除项目管理
     *
     * @param projectIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProjectByProjectIds(Long[] projectIds);

    /**
     * 批量查询团队确认明细（用于列表分行展示）
     *
     * @param projectIds 项目ID列表
     * @return 明细列表（含 projectId, deptId, deptName, confirmAmount）
     */
    public List<Map<String, Object>> selectTeamConfirmDetailsByIds(@Param("projectIds") List<Long> projectIds);

    /**
     * 获取用户列表（按岗位过滤）
     *
     * @param postCode 岗位编码
     * @return 用户列表
     */
    public List<Map<String, Object>> selectUsersByPost(@Param("postCode") String postCode);

    /**
     * 获取二级区域列表（根据一级区域）
     *
     * @param regionDictValue 一级区域字典值
     * @return 二级区域列表
     */
    public List<Map<String, Object>> selectSecondaryRegionsByRegion(@Param("regionDictValue") String regionDictValue);

    /**
     * 获取客户列表（支持搜索）
     *
     * @param customerSimpleName 客户简称
     * @return 客户列表
     */
    public List<Map<String, Object>> selectCustomers(@Param("customerSimpleName") String customerSimpleName);

    /**
     * 获取客户联系人列表（根据客户ID）
     *
     * @param customerId 客户ID
     * @return 客户联系人列表
     */
    public List<Map<String, Object>> selectCustomerContacts(@Param("customerId") Long customerId);

    /**
     * 获取部门树（三级及以下机构）
     *
     * @param project 查询参数（用于数据权限过滤）
     * @return 部门树
     */
    public List<Map<String, Object>> selectDeptTree(Project project);

    /**
     * 获取全量部门树（不限数据权限）
     */
    public List<Map<String, Object>> selectAllDeptTree();

    /**
     * 查询待审核项目列表
     *
     * @param project 项目查询条件
     * @return 待审核项目列表
     */
    public List<Project> selectReviewList(Project project);

    /**
     * 查询立项审核列表预算合计
     */
    public Map<String, Object> selectReviewSummary(Project project);

    /**
     * 根据部门查询项目列表（用于合同关联，可排除已关联的项目）
     *
     * @param deptId 部门ID
     * @param excludeContractId 要排除的合同ID（编辑时使用）
     * @return 项目列表
     */
    public List<Project> selectProjectListByDept(@Param("deptId") Long deptId, @Param("excludeContractId") Long excludeContractId);

    /**
     * 根据合同ID查询关联的项目列表
     *
     * @param contractId 合同ID
     * @return 项目列表
     */
    public List<Project> selectProjectListByContractId(@Param("contractId") Long contractId);

    /**
     * 项目搜索（轻量接口，用于 autocomplete）
     *
     * @param projectName 项目名称（模糊搜索）
     * @return 精简字段列表：projectId, projectName, projectCode
     */
    public List<Map<String, Object>> searchProjectsByName(Project project);

    /**
     * 根据用户ID查询关联的项目列表（用于工作日报）
     * 查询用户作为项目经理、市场经理、团队负责人、参与人或项目成员的所有已审核项目
     *
     * @param userId 用户ID
     * @return 项目列表（projectId, projectName, projectCode, projectStage, projectStageName）
     */
    public List<Map<String, Object>> selectProjectsByUserId(@Param("userId") Long userId);

    /**
     * 查询项目参与人员及其日报实际人天
     */
    public List<Map<String, Object>> selectParticipantsWithWorkload(@Param("projectId") Long projectId);

    /**
     * 查询所有部门信息（用于导出时构建部门路径）
     *
     * @return 部门列表（deptId, deptName, ancestors）
     */
    public List<Map<String, Object>> selectAllDeptsForPath();

    /**
     * 根据用户ID列表查询昵称（用于导出时解析参与人员）
     *
     * @param userIds 用户ID列表
     * @return 用户列表（userId, nickName）
     */
    public List<Map<String, Object>> selectUserNickNamesByIds(@Param("userIds") List<Long> userIds);

    /**
     * 查询团队收入确认平铺列表（以团队为维度，每行一条团队确认记录 + 关联项目信息）
     *
     * @param project 查询条件（含 confirmDeptId 等扩展字段）
     * @return 平铺列表
     */
    public List<Map<String, Object>> selectTeamRevenueFlatList(Project project);

    /**
     * 查询团队收入确认平铺合计（与 selectTeamRevenueFlatList 使用相同筛选条件）
     *
     * @param project 查询条件
     * @return 合计（teamConfirmAmount）
     */
    public Map<String, Object> selectTeamRevenueFlatSummary(Project project);

    /**
     * 按项目编号前缀查询项目列表（用于编号冲突检测）
     * 返回与 codePrefix 完全相同 或 以 codePrefix- 开头的所有项目
     *
     * @param codePrefix      基础项目编号（不含后缀）
     * @param excludeProjectId 编辑时排除自身（新增传 null）
     * @return 编号冲突相关项目列表（projectId, projectCode, projectName）
     */
    public List<Map<String, Object>> selectProjectsByCodePrefix(@Param("codePrefix") String codePrefix,
                                                                @Param("excludeProjectId") Long excludeProjectId);

    /**
     * 批量查询项目的名称与阶段状态，供日报保存时的归属校验使用
     *
     * <p>一次查询同时服务三个判定：
     * <ul>
     *   <li>结果中<b>缺失</b>的项目ID → 项目不存在或已删除 → 拒绝（无成员关系）
     *   <li>{@code projectStage='11'} → 项目已结项 → 拒绝新增或修改其工时（FR-010 / FR-011）
     *   <li>{@code projectName} → 拒绝提示中呈现给填报人，<b>不暴露内部ID</b>（FR-008）
     * </ul>
     *
     * <p>注意与 {@code selectProjectsByUserId}（可填项目列表）的区别：那个口径混入了
     * 审批状态与项目启停状态，只可用于界定「作用范围」，<b>不得</b>用作归属校验判据。
     *
     * @param projectIds 待查询的项目ID集合
     * @return 每项含 projectId / projectName / projectStage
     */
    List<Map<String, Object>> selectProjectStatesIn(@Param("projectIds") Collection<Long> projectIds);

    /**
     * 【Issue #24】查询用户在给定项目中担任「四类项目角色」的项目ID
     * （项目经理 / 团队负责人 / 市场经理 / 销售经理，取自 {@code pm_project} 本身）。
     *
     * <p>与 {@code ProjectMemberMapper#selectEverMemberProjectIds} 并列构成
     * {@code DailyReportServiceImpl#applyProjectScopeBypass} 的授权闸门。之所以不能只查成员表：
     * {@code ProjectServiceImpl#syncProjectMembers} 确实会把这四类角色写进 {@code pm_project_member}，
     * 但历史项目存在漏同步 —— 实测有日报的项目里市场经理缺行 30 个、销售经理缺行 27 个
     * （项目经理与团队负责人当前为 0，是数据巧合而非结构保证）。只查成员表会让「同一个人、同一个角色」
     * 的可见范围取决于「该项目行有没有被重新保存过」。
     *
     * <p>这四个字段只能由持 {@code project:project:edit} 的人写入，填报人无法自助伪造，
     * 因此仍是 deny-by-default。
     *
     * @param userId 调用者用户ID
     * @param projectIds 待判定的项目ID集合
     * @return 该用户担任上述任一角色的项目ID
     */
    List<Long> selectProjectRoleProjectIds(@Param("userId") Long userId,
                                           @Param("projectIds") Collection<Long> projectIds);
}
