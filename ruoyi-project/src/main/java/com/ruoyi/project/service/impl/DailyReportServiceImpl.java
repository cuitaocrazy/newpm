package com.ruoyi.project.service.impl;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.ruoyi.project.domain.vo.TeamDailyReportVO;
import com.ruoyi.project.domain.vo.TeamMemberDailyVO;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.DailyReportDetail;
import com.ruoyi.project.domain.WorkCalendar;
import com.ruoyi.project.domain.vo.DailySubmissionStat;
import com.ruoyi.project.mapper.DailyReportDetailMapper;
import com.ruoyi.project.mapper.WorkCalendarMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.mapper.ProjectMapper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.DailyReportMapper;
import com.ruoyi.project.domain.DailyReport;
import com.ruoyi.project.domain.request.BatchLeaveRequest;
import com.ruoyi.project.service.IDailyReportService;
import com.ruoyi.project.service.IDailyReportWhitelistService;

/**
 * 工作日报Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-26
 */
@Service
public class DailyReportServiceImpl implements IDailyReportService
{
    @Autowired
    private DailyReportMapper dailyReportMapper;

    @Autowired
    private DailyReportDetailMapper detailMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private IDailyReportWhitelistService whitelistService;

    @Autowired
    private com.ruoyi.project.mapper.TaskMapper taskMapper;

    @Autowired
    private WorkCalendarMapper workCalendarMapper;

    /** 【015】项目归属校验用：判定填报人是否「曾以任意身份参与过」目标项目 */
    @Autowired
    private com.ruoyi.project.mapper.ProjectMemberMapper projectMemberMapper;

    /** 【Issue #24】按项目筛选时豁免部门数据权限的放行标记；只由服务端计算，永不信任请求入参 */
    private static final String PROJECT_SCOPE_BYPASS = "projectScopeBypass";

    /**
     * 查询工作日报（只能查本人的）
     *
     * <p>【Issue #13 读侧】reportId 来自 URL 且连续自增，故按当前登录人硬限定；
     * 非本人的日报返回 null（前端会显示为空），不额外抛异常——避免形成比删除路径更细的存在性探测。
     * 详见 {@link DailyReportMapper#selectDailyReportById}。
     *
     * <p>【与 Issue #24 的关系】该缺陷（可枚举 IDOR：resultMap 含明细即 {@code work_content} 原文，
     * {@code report_id} 稠密可枚举，持 {@code project:dailyReport:activity} 的受限账号顺序枚举
     * 即可取尽全库日报）曾在 #24 的 hotfix 里以注释登记「本次不修、由 #13 负责」。
     * 现已按上述方式修掉，且比部门级 dataScope 更紧；前端零调用方
     * （{@code src/api/project/dailyReport.js#getDailyReport} 定义了但无引用），收紧无损。
     * #24 的那段旧注释已随合并删除，不要再照它理解本方法。
     *
     * @param reportId 日报主键
     * @return 工作日报；非本人或不存在时为 null
     */
    @Override
    public DailyReport selectDailyReportById(Long reportId)
    {
        return dailyReportMapper.selectDailyReportById(reportId, SecurityUtils.getUserId());
    }

    /**
     * 查询当前用户指定日期的日报
     *
     * @param reportDate 日报日期(yyyy-MM-dd)
     * @return 工作日报
     */
    @Override
    public DailyReport selectMyReportByDate(String reportDate)
    {
        Long userId = SecurityUtils.getUserId();
        return dailyReportMapper.selectByUserAndDate(userId, reportDate);
    }

    /**
     * 查询工作日报列表
     *
     * @param query 查询条件
     * @return 工作日报集合
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<DailyReport> selectDailyReportList(DailyReport query)
    {
        applyProjectScopeBypass(query);
        return dailyReportMapper.selectDailyReportList(query);
    }

    /**
     * 查询月度日报列表（含明细）
     *
     * @param query 查询条件
     * @return 工作日报集合（含明细）
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<DailyReport> selectMonthlyReports(DailyReport query)
    {
        applyProjectScopeBypass(query);
        return dailyReportMapper.selectMonthlyReports(query);
    }

    /**
     * 【Issue #24】判定「按项目筛选」能否豁免部门数据权限。
     *
     * <p>PM需求.md:755 授权的是「项目经理能看到参与本项目的非本团队人员日报」，但 7adb75d 的实现是
     * 「传了 projectId 就整条摘掉 ${params.dataScope}」—— 授权对象被放大成<b>全部 175 个受限账号</b>，且
     * projectNameSuggestions 无数据权限（全公司项目都能选），UI 点一下即越权。
     *
     * <p>账号口径（可复现）：{@code sys_user_role ⋈ sys_role ⋈ sys_role_menu ⋈ sys_menu}
     * 取 {@code perms='project:dailyReport:activity'}、排除 admin、只算 {@code del_flag='0' and status='0'}
     * 的账号 → 共 180 个，其中 5 个持 data_scope=1 角色（不受影响），<b>175 个全部角色受限</b>。
     *
     * <p><b>放行主体比 :755 宽，这是有意的取舍而非需求背书</b>：:755 字面授权的是「项目经理」，
     * 而本闸门放行任意「曾参与者」。之所以不收到只认经理：那会把 :755 之外、修复前一直能正常工作的
     * 普通成员视角一并砍掉，制造新的回归。所以别把「闸门 ⊋ :755」当成需求依据来引用。
     *
     * <p>本方法把豁免条件收窄为「调用者曾参与该项目<b>或</b>在该项目上担任四类项目角色」，两条凭据：
     * <ol>
     *   <li>{@code pm_project_member} 的「曾参与」永久凭据（沿用 015）；
     *   <li>{@code pm_project} 上的 项目经理 / 团队负责人 / 市场经理 / 销售经理
     *       （{@code ProjectMapper#selectProjectRoleProjectIds}）。
     * </ol>
     * 两者都只能由持项目编辑权限者写入，填报人不可自助伪造，故仍是 deny-by-default。
     *
     * <p><b>为什么必须要第二条凭据</b>：{@code ProjectServiceImpl#syncProjectMembers} 确实把这四类
     * 角色都写进成员表，但历史项目存在漏同步 —— 实测有日报的项目里市场经理缺行 30 个、销售经理缺行
     * 27 个（项目经理与团队负责人当前为 0，是数据巧合而非结构保证）。只查成员表会让同一个人、同一个
     * 角色的可见范围取决于「该项目行有没有被重新保存过」：反例 王辉 user 111 是 project 295/329 的
     * 市场经理、成员行 0 条，只查成员表他就从 387/26 份日报直接归零、活动页全白。
     *
     * <p><b>需求场景的覆盖率（可复现）</b>：以「写过该项目工时」的 (人,项目) 对为口径
     * （{@code pm_daily_report_detail ⋈ pm_daily_report}，两侧 {@code del_flag='0'}、
     * {@code project_id is not null}，distinct 后 <b>942</b> 对），本闸门放行 <b>934</b> 对、拒绝 8 对；
     * 被拒的 8 对<b>全部落在 {@code dd.project_id=93}</b>，而 93 在 {@code pm_project} 里查不到
     * （已删项目的残留明细，成员行随项目硬删一并消失）。即：闸门对现存项目的覆盖率是 100%，
     * 但这个结论依赖「排除孤儿 project_id」这个前提 —— 引用时请连前提一起引用。
     *
     * <p><b>不要给 selectEverMemberProjectIds 补 is_active/del_flag 过滤</b>：实测会挡掉
     * 离场成员维护自己历史工时的 (人,项目) 对，与 015 / DailyReportMapper.xml 离场分支口径冲突。
     *
     * <p><b>已知取舍（非缺陷，但须知晓）</b>：因此凭据是<b>永久不可撤销</b>的 —— 把人移出项目只是
     * {@code is_active='0'} 软离场（{@code ProjectMemberMapper.xml} 的 deactivate 语句），本闸门不看该
     * 标志，实测有 18 个 (人,项目) 对已无在册行仍被放行（其中 7 对从未在该项目写过工时）。
     * 「离场即回收读权限」需要单独的 Issue 决策，不要在这里顺手加过滤（会打破上一段的 015 口径）。
     *
     * <p><b>第一行 remove 不可删</b>：BaseEntity 有 setParams(Map)，Spring MVC 会把查询串里的
     * params[xxx] 绑进来（前端 addDateRange 传 params[beginTime] 就是这个通道，
     * ruoyi-ui/src/utils/ruoyi.ts:62；本 mapper 也在读 params.nickName）。已实测：params 里放
     * 字符串 "1" 同样会触发豁免（OGNL 只判非 null），故不清除就能用
     * ?projectId=36&amp;params[projectScopeBypass]=1 自助放行，XML 层没有兜底。
     * 与 DataScopeAspect#clearDataScope 同一思路。
     *
     * <p>时序安全：@DataScope 是 @Before 切面，先于方法体执行；clearDataScope 只 put DATA_SCOPE
     * 一个 key，不会清掉这里写入的值。
     *
     * <p><b>放行后恢复的边界是什么（勿照字面理解成「本部门自己人」）</b>：被恢复的
     * {@code ${params.dataScope}} 打在 {@code d} 上，而 {@code d} 在 selectReportBase/
     * selectReportWithDetail 里 join 的是 {@code r.dept_id} —— 那是<b>日报行最后一次保存时的部门快照</b>
     * （{@code saveDailyReport} 用 {@code SecurityUtils.getDeptId()} 写入，且保存走 upsert，调岗后重存
     * 会被改写），既不是填报人当前部门、也不是稳定历史。实测 1568 份日报 / 35 人的快照部门与当前部门
     * 不一致，所以拒绝路径上仍能读到当前已在部门范围外的人的日报。这是既有绑定、非本次引入，
     * 本次修复相对基线是严格收紧；但描述它时不要写成「只能看本部门自己人」。
     *
     * <p><b>与 Issue #22 的关系是「交叉」而非「包含」</b>：#22 要做的是按<b>项目归属部门</b>授权，
     * 与本闸门的「曾参与 / 担任项目角色」是两个不同维度。实测 2208 个闸门授权里有 417 个（18.9%）
     * 是项目部门维度覆盖不到的 —— 含 :755 点名的场景本身（project 43 归属 220 机动组，项目经理
     * 梁艳兰在 208 项目六组）。因此 #22 落地时必须实现成
     * {@code everMember/projectRole OR projectDeptInMyScope}（<b>加法</b>），
     * <b>绝不可</b>实现成 {@code projectDeptInMyScope}（替换）—— 那会精确撤销本次要保住的需求。
     */
    private void applyProjectScopeBypass(DailyReport query)
    {
        query.getParams().remove(PROJECT_SCOPE_BYPASS);
        Long projectId = query.getProjectId();
        if (projectId == null)
        {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        List<Long> projectIds = List.of(projectId);

        // 凭据①：成员表里的「曾参与」（015 永久凭据）
        boolean authorized = isNotEmpty(projectMemberMapper.selectEverMemberProjectIds(userId, projectIds));
        if (!authorized)
        {
            // 凭据②：pm_project 上的四类项目角色 —— 成员表可能漏同步，见本方法 javadoc
            authorized = isNotEmpty(projectMapper.selectProjectRoleProjectIds(userId, projectIds));
        }
        if (authorized)
        {
            query.getParams().put(PROJECT_SCOPE_BYPASS, Boolean.TRUE);
        }
    }

    private static boolean isNotEmpty(List<Long> ids)
    {
        return ids != null && !ids.isEmpty();
    }

    /**
     * 查询当前用户关联的项目列表
     *
     * @return 项目列表
     */
    @Override
    public List<Map<String, Object>> selectMyProjects()
    {
        Long userId = SecurityUtils.getUserId();
        List<Map<String, Object>> list = projectMapper.selectProjectsByUserId(userId);
        if (!list.isEmpty()) {
            List<Long> ids = list.stream()
                .map(p -> Long.parseLong(p.get("projectId").toString()))
                .collect(java.util.stream.Collectors.toList());
            List<Long> hasSubIds = taskMapper.selectProjectsHasTasks(ids);
            java.util.Set<Long> hasSubSet = new java.util.HashSet<>(hasSubIds);
            list.forEach(p -> p.put("hasSubProject", hasSubSet.contains(Long.parseLong(p.get("projectId").toString()))));
        }
        return list;
    }

    /**
     * 保存工作日报（新增或更新）
     * 根据用户ID和日报日期判断是新增还是更新
     *
     * @param report 工作日报
     * @return 结果
     */
    @Override
    @Transactional
    public int saveDailyReport(DailyReport report)
    {
        Long userId = SecurityUtils.getUserId();
        // 白名单用户禁止提交日报
        if (whitelistService.isInWhitelist(userId)) {
            throw new ServiceException("您已被设置为无需填写日报，如有疑问请联系管理员");
        }

        // 【015】校验提交内容的项目归属。
        // ⚠️ 必须在任何写操作之前完成——否则会出现「校验失败了、但工时已经被删了」，
        //    那比不校验更糟（spec Edge Cases / INV-1）。
        //    已用变异测试确认：把本行移到 deleteByReportIdInScope 之后，
        //    saveDailyReport_neverMemberProject_isRejected 会立刻变红。
        validateSubmissionOwnership(userId, report.getDetailList());

        Long deptId = SecurityUtils.getDeptId();
        String username = SecurityUtils.getUsername();

        report.setUserId(userId);
        report.setDeptId(deptId);

        // 计算总工时
        BigDecimal totalWorkHours = BigDecimal.ZERO;
        List<DailyReportDetail> detailList = report.getDetailList();
        if (detailList != null)
        {
            for (DailyReportDetail detail : detailList)
            {
                if ("work".equals(detail.getEntryType()) && detail.getWorkHours() != null)
                {
                    totalWorkHours = totalWorkHours.add(detail.getWorkHours());
                }
            }
        }
        report.setTotalWorkHours(totalWorkHours);

        // 格式化日报日期为 yyyy-MM-dd 字符串
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(report.getReportDate());

        // 检查是否已存在该日期的日报（使用简单查询，避免复杂JOIN导致的结果映射问题）
        Long existingReportId = dailyReportMapper.selectReportIdByUserAndDate(userId, dateStr);

        // 用于记录旧明细中涉及的子任务ID和项目ID，确保删除行也参与工时重算
        Set<Long> oldSubProjectIds = new HashSet<>();
        Set<Long> oldProjectIds = new HashSet<>();

        int rows;
        if (existingReportId != null)
        {
            // 更新已有日报
            report.setReportId(existingReportId);
            report.setUpdateBy(username);
            report.setUpdateTime(DateUtils.getNowDate());
            rows = dailyReportMapper.updateDailyReport(report);

            // 在删除前先记录旧明细的子任务ID和项目ID，用于后续工时重算
            List<DailyReportDetail> oldDetails = detailMapper.selectByReportId(existingReportId);
            oldDetails.stream()
                    .filter(d -> d.getSubProjectId() != null)
                    .map(DailyReportDetail::getSubProjectId)
                    .forEach(oldSubProjectIds::add);
            oldDetails.stream()
                    .filter(d -> d.getProjectId() != null)
                    .map(DailyReportDetail::getProjectId)
                    .forEach(oldProjectIds::add);

            // 删除旧明细，插入新明细
            // 【015】只删除本次提交「有能力表达」的明细：填报人可填项目的工时 + 全部非项目工时。
            // 范围外的明细（如已结项项目的历史工时）原样保留——填报人在填写页上根本看不到它们，
            // 未出现在提交中不等于要删除。改回 deleteByReportId 会重新引入静默数据丢失（FR-001）。
            // userId 同时作为归属限定传入：这条 reportId 本就由 selectReportIdByUserAndDate(userId,...)
            // 自己定位（保存路径从不接受外部 reportId），传 userId 只是把这份归属显式落到 SQL 上（Issue #13）
            detailMapper.deleteByReportIdInScope(existingReportId,
                    resolveSubmissionScope(userId, detailList), userId);
        }
        else
        {
            // 新增日报
            report.setCreateBy(username);
            report.setCreateTime(DateUtils.getNowDate());
            rows = dailyReportMapper.insertDailyReport(report);
        }

        // 批量插入明细
        if (detailList != null && !detailList.isEmpty())
        {
            for (DailyReportDetail detail : detailList)
            {
                detail.setReportId(report.getReportId());
                detail.setCreateBy(username);
                // entryType 默认 work
                if (detail.getEntryType() == null || detail.getEntryType().isEmpty()) {
                    detail.setEntryType("work");
                }
                // 假期行 workContent 默认空字符串
                if (!"work".equals(detail.getEntryType()) && detail.getWorkContent() == null) {
                    detail.setWorkContent("");
                }
                // 假期行 workHours = leaveHours（前端传 leaveHours，统一用 workHours 存储）
                if (!"work".equals(detail.getEntryType()) && detail.getLeaveHours() != null) {
                    detail.setWorkHours(detail.getLeaveHours());
                }
            }
            detailMapper.batchInsert(detailList);
        }

        // 【015】重算当日汇总工时。
        // 上面按「提交内容」算出的 totalWorkHours 不含作用范围外被保留的明细，
        // 直接写入会让主记录与明细对不上——填写页日历卡上的当日工时会偏小（SC-010）。
        // 只在更新既有日报时需要：新建日报不存在「既有明细被保留」的情形。
        // e2e 对账实测暴露（2026-08-03）。
        if (existingReportId != null) {
            BigDecimal actualTotal = detailMapper.sumWorkHoursByReportId(existingReportId);
            if (actualTotal != null && actualTotal.compareTo(totalWorkHours) != 0) {
                dailyReportMapper.updateTotalWorkHours(existingReportId, actualTotal, userId);
            }
        }

        // 更新受影响项目的实际工作量（两级滚动：先子任务，再主项目）
        List<DailyReportDetail> workDetails = (detailList != null ? detailList : java.util.Collections.<DailyReportDetail>emptyList())
                .stream()
                .filter(d -> d.getProjectId() != null && "work".equals(d.getEntryType()))
                .collect(Collectors.toList());

        // Step 1：更新受影响子任务工时（含旧明细中被删除的子任务行）
        Set<Long> affectedSubProjectIds = workDetails.stream()
                .filter(d -> d.getSubProjectId() != null)
                .map(DailyReportDetail::getSubProjectId)
                .collect(Collectors.toSet());
        affectedSubProjectIds.addAll(oldSubProjectIds);
        for (Long taskId : affectedSubProjectIds) {
            BigDecimal taskHours = detailMapper.sumWorkHoursBySubProjectId(taskId);
            taskMapper.updateActualWorkload(taskId, taskHours);
        }

        // Step 2：更新主项目工时（含旧明细中被删除行对应的项目）
        Set<Long> affectedProjectIds = workDetails.stream()
                .map(DailyReportDetail::getProjectId)
                .collect(Collectors.toSet());
        affectedProjectIds.addAll(oldProjectIds);

        // 2a. 受影响任务的父项目一并纳入重算范围（仅扩大范围，不替代 2b 的汇总口径）
        if (!affectedSubProjectIds.isEmpty()) {
            affectedProjectIds.addAll(taskMapper.selectProjectIdsByTaskIds(
                    new java.util.ArrayList<>(affectedSubProjectIds)));
        }
        // 2b. 所有受影响主项目：按日报明细全量汇总工时
        // pm_daily_report_detail.project_id 存的始终是父项目 id（任务另用 sub_project_id 标识），
        // 故该汇总天然覆盖「直挂父项目」与「挂在任务上」两类工时。
        // 不可改用 SUM(pm_task.actual_workload)：项目建任务之前直挂父项目的工时会被永久抹掉（Issue #5 ①）。
        for (Long projectId : affectedProjectIds) {
            BigDecimal directHours = detailMapper.sumWorkHoursByProjectId(projectId);
            projectMapper.updateActualWorkload(projectId, directHours);
        }

        return rows;
    }

    /**
     * 【015】校验提交明细的项目归属，任一条不通过即拒绝整次保存
     *
     * <p>校验规则（只作用于 {@code entry_type='work'} 且 projectId 非空的记录，
     * 假期类记录不关联项目、不适用本校验）：
     * <ul>
     *   <li><b>V1 曾参与</b>：{@code pm_project_member} 中存在该 (project, user) 行，
     *       <b>不限</b>在册或已离场——离场者仍可维护自己填过的历史工时（FR-006 / US4）。
     *       项目不存在或已删除时同样按本条拒绝。
     *   <li><b>V2 未结项</b>：{@code project_stage != '11'}，已结项项目不再接受新增或修改
     *       其工时（FR-010 / FR-011）。仅作用于本次提交内容——该日既有的已结项工时
     *       由作用范围机制保留，不因此被拒。
     *   <li><b>V3 任务归属</b>：明细的 {@code sub_project_id} 所指任务，其 project_id
     *       必须等于该明细自己声明的 projectId；映射缺失同等视为不匹配（FR-007）。
     * </ul>
     *
     * <p><b>本方法不校验 reportId 归属</b>——保存路径按 (userId, date) 自行定位日报主记录，
     * 不接受外部传入的 reportId，故无此风险。删除路径的情况不同，见
     * {@link #deleteDailyReportByIds}（Issue #13）。
     *
     * <p><b>调用位置不可改</b>：必须在所有 delete / insert / 工时重算之前。
     * 本方法只读不写，抛出时事务内尚无任何变更，故拒绝后数据库状态与请求前完全一致（INV-1）。
     *
     * @param userId     填报人用户ID
     * @param detailList 本次提交的明细
     * @throws ServiceException 任一条不通过时抛出，消息含被拒项目名称（FR-008）
     */
    private void validateSubmissionOwnership(Long userId, List<DailyReportDetail> detailList)
    {
        if (detailList == null || detailList.isEmpty()) {
            return;
        }
        Set<Long> projectIds = detailList.stream()
                .filter(d -> "work".equals(d.getEntryType()) && d.getProjectId() != null)
                .map(DailyReportDetail::getProjectId)
                .collect(Collectors.toSet());
        if (projectIds.isEmpty()) {
            return;
        }

        // 批量查询：项目名（用于提示）与阶段状态。生产实测单次提交最多涉及 6 个项目，无 N+1 之虞。
        java.util.Map<Long, java.util.Map<String, Object>> states = new java.util.HashMap<>();
        for (java.util.Map<String, Object> row : projectMapper.selectProjectStatesIn(projectIds)) {
            states.put(Long.parseLong(row.get("projectId").toString()), row);
        }
        Set<Long> everMember = new HashSet<>(
                projectMemberMapper.selectEverMemberProjectIds(userId, projectIds));

        for (Long projectId : projectIds) {
            java.util.Map<String, Object> state = states.get(projectId);
            String projectName = (state != null && state.get("projectName") != null)
                    ? state.get("projectName").toString()
                    : ("#" + projectId);

            // V1：项目不存在或已被删除 → 无成员关系可言，与「从未参与」同等处理
            if (state == null || !everMember.contains(projectId)) {
                throw new ServiceException("项目《" + projectName + "》不在您参与的项目范围内");
            }

            // V2：项目已结项 → 不再接受新增或修改其工时（FR-010 / FR-011）
            // 注意本校验只作用于「本次提交的内容」。该日既有的已结项工时不在此列——
            // 它们由作用范围机制原样保留，不得因此把整次保存拒掉。
            if ("11".equals(String.valueOf(state.get("projectStage")))) {
                throw new ServiceException("项目《" + projectName + "》已结项，不能新增或修改其工时");
            }
        }

        // V3：任务归属——明细挂的任务必须确实隶属于它自己声明的项目（FR-007）
        Set<Long> taskIds = detailList.stream()
                .filter(d -> "work".equals(d.getEntryType()) && d.getSubProjectId() != null)
                .map(DailyReportDetail::getSubProjectId)
                .collect(Collectors.toSet());
        if (!taskIds.isEmpty()) {
            java.util.Map<Long, Long> taskOwner = new java.util.HashMap<>();
            for (java.util.Map<String, Object> row : taskMapper.selectTaskProjectPairs(taskIds)) {
                Object owner = row.get("projectId");
                taskOwner.put(Long.parseLong(row.get("taskId").toString()),
                        owner == null ? null : Long.parseLong(owner.toString()));
            }
            for (DailyReportDetail d : detailList) {
                if (!"work".equals(d.getEntryType()) || d.getSubProjectId() == null) {
                    continue;
                }
                Long owner = taskOwner.get(d.getSubProjectId());
                // 映射缺失（任务不存在）与映射不符，同等视为不匹配
                if (owner == null || !owner.equals(d.getProjectId())) {
                    throw new ServiceException("任务与所选项目不匹配，请重新选择");
                }
            }
        }
    }

    /**
     * 解析本次操作的「作用范围」——填报人当前可填的项目ID集合
     *
     * <p>与 {@link #selectMyProjects()} 使用同一口径（{@code selectProjectsByUserId}），
     * 但不做 hasSubProject 的附加查询——界定范围只需要项目ID。
     *
     * <p>作用范围决定哪些既有明细「归本次提交管」：范围内的按提交内容替换（未提交即删除），
     * 范围外的原样保留。非项目工时（project_id 为 null）由 SQL 侧无条件纳入范围。
     *
     * @param userId 填报人用户ID
     * @return 可填项目ID集合；无可填项目时返回空集合（此时仅非项目工时归本次提交管）
     */
    private Set<Long> resolveVisibleProjectIds(Long userId)
    {
        return projectMapper.selectProjectsByUserId(userId).stream()
                .map(p -> Long.parseLong(p.get("projectId").toString()))
                .collect(Collectors.toSet());
    }

    /**
     * 【015】保存路径的「作用范围」= 可填项目 ∪ <b>本次提交里出现的项目</b>
     *
     * <p>为什么必须并上提交里的项目：{@link #resolveVisibleProjectIds} 的口径
     * （{@code selectProjectsByUserId}）带有项目生命周期条件——{@code approval_status='1'}、
     * {@code project_status='0'}。一个「审核通过时填过日报、之后被退回待审核或暂停」的项目
     * 会掉出该集合；此时若填报人再次提交它的工时，旧明细因不在范围内而删不掉，新明细又照常插入，
     * <b>工时会凭空翻倍</b>——比修复前的「丢数据」更危险，因为它虚增收入确认依据。
     *
     * <p>语义上也更自洽：填报人这次明确提交了某项目的工时，该项目当然归本次提交管。
     * 而「已结项项目的历史工时」之所以受保护，是因为它<b>不在提交里</b>（填报人看不到、提交不了），
     * 这条保护不受本方法影响。
     *
     * <p>删除路径不适用本方法——那里没有提交内容，作用范围就是可填项目集合本身。
     *
     * <p>e2e 回归实测暴露（2026-08-03）：<code>e2e-team-daily-workload</code> 的核心用例
     * 因新建项目处于待审核态而出现 8+8+4=20（应为 12）。
     */
    private Set<Long> resolveSubmissionScope(Long userId, List<DailyReportDetail> detailList)
    {
        Set<Long> scope = resolveVisibleProjectIds(userId);
        if (detailList != null) {
            detailList.stream()
                    .filter(d -> d.getProjectId() != null)
                    .map(DailyReportDetail::getProjectId)
                    .forEach(scope::add);
        }
        return scope;
    }

    /**
     * 批量删除工作日报
     * 明细与主记录<b>均为硬删除</b>（pm_daily_report / pm_daily_report_detail 是硬删除例外表）
     *
     * <p><b>【Issue #13】只能删除本人的日报，不设管理员例外。</b>
     * reportIds 完全来自 URL（{@code DELETE /project/dailyReport/{reportIds}}，Spring 按逗号拆数组），
     * 而 {@code project:dailyReport:remove} 授给了 8 个角色（含普通用户角色 role_id=2）、
     * report_id 又是连续自增，且 pm_daily_report / pm_daily_report_detail 都是<b>硬删除</b>——
     * 不校验归属就等于把「删掉任意人的日报」开放给全部账号，且误删只能靠 OSS 归档备份（付费解冻）恢复。
     *
     * <p>不设管理员例外的依据：前端唯一的删除入口是填报人删自己当天的日报（write.vue），
     * 系统内不存在任何管理端批量删除日报的界面，「人人管自己」就是该权限的设计意图。
     * 若将来确有管理端代删需求，应新增独立权限 + 独立接口（并留审计），而不是放宽此处。
     *
     * <p>失败模式的取舍：<b>「查不到」按幂等 no-op，「查得到但归属他人」硬拒绝并整批回滚。</b>
     * 前者是因为过期页面/重复点击会重发已被删掉的 reportId，报错只会让填报人困惑；
     * 后者用异常而非静默跳过——静默跳过会返回「成功」却什么都没删，比报错更糟。
     *
     * <p>入参先规整（剔 null、去重）：URL 拆出的数组可能含 null（如 {@code DELETE /.../,}），
     * 而返回值是「处理条数」，不规整就会出现「返回成功但一行未动」以及重复 ID 重复重算。
     *
     * @param reportIds 需要删除的日报主键集合
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteDailyReportByIds(Long[] reportIds)
    {
        // 入参先规整：剔除 null、去重、保持原顺序。
        // null 元素来自 URL 本身——Spring 把 @PathVariable Long[] 按逗号拆开，
        // "DELETE /project/dailyReport/," 会转成 Long[]{null, null}（length=2）；
        // 不剔除的话它绕过下面的空集守卫，最终 rows = reportIds.length 让 toAjax 报「操作成功」，
        // 而实际上三条 SQL 都是 0 行（NULL 永不匹配 report_id）——「说删了其实没删」。
        // 去重则是为了让 rows 与实际处理的日报条数恒等（"70,70,70" 否则会重算 3 遍并返回 3）。
        Set<Long> targetReportIds = new java.util.LinkedHashSet<>();
        if (reportIds != null) {
            for (Long reportId : reportIds) {
                if (reportId != null) {
                    targetReportIds.add(reportId);
                }
            }
        }
        if (targetReportIds.isEmpty()) {
            return 0;
        }
        Long currentUserId = SecurityUtils.getUserId();

        // 【Issue #13】归属校验必须在任何读写之前完成：
        // 一旦进入下面的收集循环，就会读取他人明细并对他人的项目发起 pm_project 工时重算——
        // 即使数值上幂等，被拒绝的请求也不该产生任何写入（与保存路径的 INV-1 同构）。
        //
        // 该查询带 FOR UPDATE，在此一次性取得主记录的排他锁。若改成普通 SELECT，
        // 后续的 updateTotalWorkHours / deleteDailyReportByIds 会构成 S→X 锁升级而死锁
        // （详见 DailyReportMapper#selectReportOwnersForUpdate 的 javadoc）。
        List<Map<String, Object>> owners =
                dailyReportMapper.selectReportOwnersForUpdate(targetReportIds);
        // ownedReportIds = 「主记录确实存在、且归属本人」的那些。
        // 查不到主记录的 ID 不进这个集合——它们不参与明细读取与工时重算：
        // 归属探针查不到主记录就无从判断数据归谁，若「查不到就当属于我」继续往下走，
        // 「主记录已不存在但明细还在」的孤儿数据会被读出来，并触发对调用者无数据权限的
        // 项目发起 actual_workload 重算（取排他锁）。
        Set<Long> ownedReportIds = new java.util.LinkedHashSet<>();
        if (owners != null) {
            for (Map<String, Object> row : owners) {
                Object owner = row.get("userId");
                if (owner != null && !currentUserId.equals(Long.parseLong(owner.toString()))) {
                    throw new ServiceException("只能删除本人的日报，请勿删除他人日报");
                }
                Object rid = row.get("reportId");
                if (rid != null) {
                    ownedReportIds.add(Long.parseLong(rid.toString()));
                }
            }
        }

        // 删除前收集受影响的项目和子任务ID，用于工时重算
        Set<Long> affectedProjectIds = new java.util.HashSet<>();
        Set<Long> affectedSubProjectIds = new java.util.HashSet<>();
        for (Long reportId : ownedReportIds) {
            List<DailyReportDetail> details = detailMapper.selectByReportId(reportId);
            if (details != null) {
                for (DailyReportDetail d : details) {
                    if ("work".equals(d.getEntryType()) && d.getProjectId() != null) {
                        affectedProjectIds.add(d.getProjectId());
                    }
                    if ("work".equals(d.getEntryType()) && d.getSubProjectId() != null) {
                        affectedSubProjectIds.add(d.getSubProjectId());
                    }
                }
            }
        }

        // 【015】按作用范围删除明细：只删填报人看得见的部分。
        // 已结项项目的历史工时在界面上根本不显示，不能因为他点了「删除日报」就被无辜清除（FR-013）。
        //
        // 主记录的去留必须跟着明细走：明细靠 report_id 归属主记录，而除 selectByReportId 外的查询
        // 都要经 "pm_daily_report r ... WHERE r.del_flag='0'" 过滤。若主记录被软删而明细仍在，
        // 这些明细将无法通过任何业务查询到达——等于换一种方式把工时弄丢（FR-014 / INV-D1）。
        // 归属校验已通过，故此处的「调用者」必然就是这些日报的所有者，
        // 拿调用者的可填项目集合去裁剪自己的明细，语义是自洽的（Issue #13 前是拿攻击者的范围裁剪受害者数据）。
        Set<Long> visibleProjectIds = resolveVisibleProjectIds(currentUserId);
        List<Long> deletableReportIds = new java.util.ArrayList<>();
        for (Long reportId : ownedReportIds) {
            detailMapper.deleteByReportIdInScope(reportId, visibleProjectIds, currentUserId);
            if (detailMapper.countByReportId(reportId) > 0) {
                // 有明细被保留 → 主记录一并保留，并按剩余 work 明细重算当日汇总工时（INV-D2）
                BigDecimal remaining = detailMapper.sumWorkHoursByReportId(reportId);
                dailyReportMapper.updateTotalWorkHours(reportId,
                        remaining != null ? remaining : BigDecimal.ZERO, currentUserId);
            } else {
                // 无残留 → 走既有行为：硬删除主记录（DELETE FROM，不是 del_flag 标记）
                deletableReportIds.add(reportId);
            }
        }
        // 查不到主记录的 ID 也一并交给主记录删除语句：它带 user_id 限定，查不到就删 0 行，
        // 语义仍是幂等 no-op；这样做是纵深防御——万一该 report_id 在本事务开始后被他人复用，
        // SQL 的 user_id 条件仍会挡住，而不是靠「反正查不到」这个瞬时判断。
        for (Long reportId : targetReportIds) {
            if (!ownedReportIds.contains(reportId)) {
                deletableReportIds.add(reportId);
            }
        }
        if (!deletableReportIds.isEmpty()) {
            dailyReportMapper.deleteDailyReportByIds(deletableReportIds.toArray(new Long[0]), currentUserId);
        }
        // 返回「本次处理的日报条数」，而不是「删掉了几条主记录」。
        // 当明细全部因不可见而被保留时，没有主记录可删，但删除操作本身是成功完成的
        // （可见明细已删、汇总已重算）。若此处返回 0，BaseController.toAjax 会判为「操作失败」，
        // 填报人看到错误提示后会重复点击——e2e 实测暴露（2026-08-03）。
        // 计数用规整后的集合（剔 null、去重），因此它与实际处理的日报条数恒等。
        int rows = targetReportIds.size();

        // 重算受影响子任务的工时
        for (Long taskId : affectedSubProjectIds) {
            BigDecimal taskHours = detailMapper.sumWorkHoursBySubProjectId(taskId);
            taskMapper.updateActualWorkload(taskId, taskHours);
        }

        // 重算受影响主项目的工时（口径同 insertOrUpdateMyReport：按明细全量汇总，见 Issue #5 ①）
        if (!affectedSubProjectIds.isEmpty()) {
            affectedProjectIds.addAll(taskMapper.selectProjectIdsByTaskIds(
                    new java.util.ArrayList<>(affectedSubProjectIds)));
        }
        for (Long projectId : affectedProjectIds) {
            BigDecimal directHours = detailMapper.sumWorkHoursByProjectId(projectId);
            projectMapper.updateActualWorkload(projectId, directHours);
        }

        return rows;
    }

    /**
     * 查询活动页用户列表（数据权限过滤）
     *
     * @param query 查询条件（deptId）
     * @return 用户列表
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<Map<String, Object>> selectActivityUsers(DailyReport query)
    {
        applyProjectScopeBypass(query);
        return dailyReportMapper.selectActivityUsers(query);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<DailySubmissionStat> selectWeeklyStats(DailyReport query)
    {
        // 解析月份，获取起止日期
        YearMonth ym = YearMonth.parse(query.getYearMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        query.setStartDate(start.toString());
        query.setEndDate(end.toString());

        // 查询该月每天已提交人数 → Map<date, count>
        List<Map<String, Object>> submittedRows = dailyReportMapper.selectSubmittedCountByDate(query);
        Map<String, Integer> submittedMap = new HashMap<>();
        for (Map<String, Object> row : submittedRows) {
            String date = row.get("reportDate").toString();
            int count = ((Number) row.get("submittedCount")).intValue();
            submittedMap.put(date, count);
        }

        // 查询总用户数（固定值，用于前端顶部显示）
        int total = dailyReportMapper.selectTotalUserCount(query);

        // 按天查询需提交日报的用户数（基于用户创建时间）
        List<Map<String, Object>> totalByDateRows = dailyReportMapper.selectTotalUserCountByDate(query);
        Map<String, Integer> totalByDateMap = new HashMap<>();
        for (Map<String, Object> row : totalByDateRows) {
            String date = row.get("reportDate").toString();
            int count = ((Number) row.get("totalCount")).intValue();
            totalByDateMap.put(date, count);
        }

        // 查询工作日历（按年）
        Map<String, String> calendarMap = new HashMap<>();
        List<WorkCalendar> calendars = workCalendarMapper.selectByYear(start.getYear());
        // 如果跨年则追加下一年（极少发生）
        if (start.getYear() != end.getYear()) {
            calendars.addAll(workCalendarMapper.selectByYear(end.getYear()));
        }
        for (WorkCalendar wc : calendars) {
            if (wc.getCalendarDateStr() != null) {
                calendarMap.put(wc.getCalendarDateStr(), wc.getDayType());
            } else if (wc.getCalendarDate() != null) {
                String ds = new SimpleDateFormat("yyyy-MM-dd").format(wc.getCalendarDate());
                calendarMap.put(ds, wc.getDayType());
            }
        }

        // 星期名称（ISO: 1=周一 ... 7=周日）
        String[] weekNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        // 构建结果
        LocalDate today = LocalDate.now();
        List<DailySubmissionStat> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String dateStr = date.toString();
            boolean future = date.isAfter(today);

            boolean workday;
            if (calendarMap.containsKey(dateStr)) {
                workday = "workday".equals(calendarMap.get(dateStr));
            } else {
                DayOfWeek dow = date.getDayOfWeek();
                workday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            }

            DailySubmissionStat stat = new DailySubmissionStat();
            stat.setReportDate(dateStr);
            stat.setDayOfWeek(weekNames[date.getDayOfWeek().getValue() - 1]);
            stat.setIsWorkday(workday);
            stat.setIsFuture(future);
            // 未来日期不统计人数
            int dailyTotal = totalByDateMap.getOrDefault(dateStr, total);
            stat.setSubmittedCount(future ? null : submittedMap.getOrDefault(dateStr, 0));
            stat.setUnsubmittedCount(future ? null : (workday ? Math.max(0, dailyTotal - submittedMap.getOrDefault(dateStr, 0)) : 0));
            result.add(stat);
        }
        return result;
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<Map<String, Object>> selectSubmittedDetail(DailyReport query)
    {
        return dailyReportMapper.selectSubmittedUsersOnDate(query);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<Map<String, Object>> selectUnsubmittedDetail(DailyReport query)
    {
        return dailyReportMapper.selectUnsubmittedUsersOnDate(query);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public int selectTotalUsersForStats(DailyReport query)
    {
        return dailyReportMapper.selectTotalUserCount(query);
    }

    @Override
    @DataScope(deptAlias = "d")
    public List<Map<String, Object>> selectStatsDeptTree(DailyReport query)
    {
        return dailyReportMapper.selectStatsDeptTree(query);
    }

    @Override
    public void exportWeeklyStats(HttpServletResponse response, List<DailySubmissionStat> statList, DailyReport query)
    {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Sheet1：汇总表
            Sheet sheet1 = wb.createSheet("汇总");
            String[] headers1 = {"日期", "星期", "是否工作日", "已提交人数", "未提交人数"};
            Row h1 = sheet1.createRow(0);
            for (int i = 0; i < headers1.length; i++) h1.createCell(i).setCellValue(headers1[i]);
            int r1 = 1;
            for (DailySubmissionStat s : statList) {
                Row row = sheet1.createRow(r1++);
                row.createCell(0).setCellValue(s.getReportDate());
                row.createCell(1).setCellValue(s.getDayOfWeek());
                row.createCell(2).setCellValue(Boolean.TRUE.equals(s.getIsWorkday()) ? "是" : "否");
                row.createCell(3).setCellValue(s.getSubmittedCount() != null ? s.getSubmittedCount() : 0);
                row.createCell(4).setCellValue(s.getUnsubmittedCount() != null ? s.getUnsubmittedCount() : 0);
            }

            // Sheet2：明细表
            Sheet sheet2 = wb.createSheet("明细");
            String[] headers2 = {"日期", "姓名", "部门", "提交状态", "工时合计"};
            Row h2 = sheet2.createRow(0);
            for (int i = 0; i < headers2.length; i++) h2.createCell(i).setCellValue(headers2[i]);
            int r2 = 1;
            for (DailySubmissionStat s : statList) {
                if (!Boolean.TRUE.equals(s.getIsWorkday())) continue;
                DailyReport detailQuery = new DailyReport();
                detailQuery.setReportDate(null); // 使用 String reportDate
                detailQuery.setStartDate(s.getReportDate());
                detailQuery.setEndDate(s.getReportDate());
                detailQuery.setDeptId(query.getDeptId());
                // 传递 dataScope：复用 query 的 params
                detailQuery.setParams(query.getParams());

                // 通过 selectSubmittedUsersOnDate 和 selectUnsubmittedUsersOnDate 查明细
                DailyReport singleQuery = new DailyReport();
                singleQuery.setDeptId(query.getDeptId());
                singleQuery.setParams(query.getParams());

                // 已提交人员：借 Map 传 reportDate 字符串（XML 用 #{reportDate}）
                singleQuery.getParams().put("reportDateStr", s.getReportDate());

                List<Map<String, Object>> submitted = dailyReportMapper.selectSubmittedUsersOnDate(buildDetailQuery(query, s.getReportDate()));
                for (Map<String, Object> p : submitted) {
                    Row row = sheet2.createRow(r2++);
                    row.createCell(0).setCellValue(s.getReportDate());
                    row.createCell(1).setCellValue(str(p.get("nickName")));
                    row.createCell(2).setCellValue(str(p.get("deptName")));
                    row.createCell(3).setCellValue("已提交");
                    Object h = p.get("totalWorkHours");
                    row.createCell(4).setCellValue(h != null ? h.toString() : "0");
                }
                List<Map<String, Object>> unsubmitted = dailyReportMapper.selectUnsubmittedUsersOnDate(buildDetailQuery(query, s.getReportDate()));
                for (Map<String, Object> p : unsubmitted) {
                    Row row = sheet2.createRow(r2++);
                    row.createCell(0).setCellValue(s.getReportDate());
                    row.createCell(1).setCellValue(str(p.get("nickName")));
                    row.createCell(2).setCellValue(str(p.get("deptName")));
                    row.createCell(3).setCellValue("未提交");
                    row.createCell(4).setCellValue("");
                }
            }

            String filename = URLEncoder.encode("日报统计报表_" + query.getYearMonth() + ".xlsx", "UTF-8");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            wb.write(response.getOutputStream());
        } catch (Exception e) {
            throw new ServiceException("导出失败：" + e.getMessage());
        }
    }

    private DailyReport buildDetailQuery(DailyReport base, String reportDateStr) {
        DailyReport q = new DailyReport();
        q.setDeptId(base.getDeptId());
        q.setParams(base.getParams());
        // XML 中用 #{reportDate}，DailyReport.reportDate 是 Date 类型
        // 通过 params Map 绕过类型转换，在 XML 中改为 #{params.reportDateStr}
        // 但现有 XML 用 #{reportDate}，故直接用 java.sql.Date 包装
        try {
            q.setReportDate(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(reportDateStr));
        } catch (Exception ignored) {}
        return q;
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }

    /**
     * 团队日报 - 项目 autocomplete
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<Map<String, Object>> selectTeamProjectOptions(DailyReport query)
    {
        return dailyReportMapper.selectTeamProjectOptions(query);
    }

    /**
     * 团队日报 - 按项目→成员聚合
     * 原始行：项目×成员×日期（LEFT JOIN，无日报则 reportDate=null）
     * Java 层两层聚合：projectId → userId → date→hours
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<TeamDailyReportVO> selectTeamMonthly(DailyReport query)
    {
        List<Map<String, Object>> rows = dailyReportMapper.selectTeamMonthlyRaw(query);

        // projectId → TeamDailyReportVO（保序用 LinkedHashMap）
        LinkedHashMap<Long, TeamDailyReportVO> projectMap = new LinkedHashMap<>();
        // (projectId, userId) → TeamMemberDailyVO
        LinkedHashMap<String, TeamMemberDailyVO> memberMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rows)
        {
            Long projectId = toLong(row.get("projectId"));
            Long userId    = toLong(row.get("userId"));

            // 聚合项目层
            TeamDailyReportVO project = projectMap.computeIfAbsent(projectId, id -> {
                TeamDailyReportVO vo = new TeamDailyReportVO();
                vo.setProjectId(id);
                vo.setProjectName(str(row.get("projectName")));
                vo.setHasContract(toBoolean(row.get("hasContract")));
                vo.setEstimatedWorkload(toBigDecimal(row.get("estimatedWorkload")));
                vo.setActualPersonDays(toBigDecimal(row.get("actualPersonDays")));
                vo.setProjectStage(str(row.get("projectStage")));
                vo.setRevenueConfirmYear(str(row.get("revenueConfirmYear")));
                vo.setConfirmAmount(toBigDecimal(row.get("confirmAmount")));
                vo.setRevenueConfirmStatus(str(row.get("revenueConfirmStatus")));
                vo.setProjectBudget(toBigDecimal(row.get("projectBudget")));
                vo.setContractAmount(toBigDecimal(row.get("contractAmount")));
                vo.setMembers(new ArrayList<>());
                return vo;
            });

            // 聚合成员层
            String memberKey = projectId + "_" + userId;
            TeamMemberDailyVO member = memberMap.computeIfAbsent(memberKey, k -> {
                TeamMemberDailyVO vo = new TeamMemberDailyVO();
                vo.setUserId(userId);
                vo.setNickName(str(row.get("nickName")));
                vo.setDeptName(str(row.get("deptName")));
                vo.setIsFormer("1".equals(str(row.get("isFormer"))));
                project.getMembers().add(vo);
                return vo;
            });

            // 填充日期工时
            Object reportDate = row.get("reportDate");
            Object totalWorkHours = row.get("totalWorkHours");
            if (reportDate != null && totalWorkHours != null)
            {
                String dateStr = reportDate.toString().substring(0, 10); // yyyy-MM-dd
                BigDecimal hours = toBigDecimal(totalWorkHours);
                member.getDailyHours().merge(dateStr, hours, BigDecimal::add);
                member.setTotalHours(member.getTotalHours().add(hours));
            }
        }

        return new ArrayList<>(projectMap.values());
    }

    private Long toLong(Object val)
    {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        return Long.valueOf(val.toString());
    }

    private BigDecimal toBigDecimal(Object val)
    {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    private Boolean toBoolean(Object val)
    {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        // MySQL BIT/TINYINT: 1 → true
        return "1".equals(val.toString()) || "true".equalsIgnoreCase(val.toString());
    }

    @Override
    @Transactional
    public Map<String, Integer> batchSaveLeave(BatchLeaveRequest request)
    {
        // 校验入参
        if ("work".equals(request.getEntryType())) {
            throw new ServiceException("假期类型不合法：work 不能作为假期类型");
        }
        LocalDate start = LocalDate.parse(request.getStartDate());
        LocalDate end   = LocalDate.parse(request.getEndDate());
        if (start.isAfter(end)) {
            throw new ServiceException("日期范围不合法：startDate 不能晚于 endDate");
        }

        // 查询范围内工作日历，构建节假日/调班集合
        Set<String> holidaySet       = new HashSet<>();
        Set<String> forcedWorkdaySet = new HashSet<>();
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            List<WorkCalendar> calendars = workCalendarMapper.selectByYear(year);
            for (WorkCalendar wc : calendars) {
                String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(wc.getCalendarDate());
                if ("holiday".equals(wc.getDayType())) {
                    holidaySet.add(dateStr);
                } else if ("workday".equals(wc.getDayType())) {
                    forcedWorkdaySet.add(dateStr);
                }
            }
        }

        int totalWorkdays = 0, created = 0, skipped = 0, overwritten = 0;
        LocalDate current = start;

        while (!current.isAfter(end)) {
            String dateStr = current.toString(); // yyyy-MM-dd
            DayOfWeek dow  = current.getDayOfWeek();
            boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

            // 跳过周末（除非工作日历标记为调班工作日），跳过节假日
            if ((isWeekend && !forcedWorkdaySet.contains(dateStr)) || holidaySet.contains(dateStr)) {
                current = current.plusDays(1);
                continue;
            }
            totalWorkdays++;

            // 查询该日已有日报 ID
            Long userId = SecurityUtils.getUserId();
            Long existingReportId = dailyReportMapper.selectReportIdByUserAndDate(userId, dateStr);

            // 查询已有 detail 列表
            List<DailyReportDetail> existingDetails = new ArrayList<>();
            if (existingReportId != null) {
                existingDetails = detailMapper.selectByReportId(existingReportId);
            }

            // 检查同类型假期冲突
            boolean hasSameTypeLeave = existingDetails.stream()
                    .anyMatch(d -> request.getEntryType().equals(d.getEntryType()));

            if (hasSameTypeLeave) {
                if ("skip".equals(request.getConflictStrategy())) {
                    skipped++;
                    current = current.plusDays(1);
                    continue;
                } else {
                    // overwrite：过滤掉同类型旧条目
                    existingDetails = existingDetails.stream()
                            .filter(d -> !request.getEntryType().equals(d.getEntryType()))
                            .collect(Collectors.toList());
                    overwritten++;
                }
            } else {
                created++;
            }

            // 构建新假期条目
            DailyReportDetail leaveDetail = new DailyReportDetail();
            leaveDetail.setEntryType(request.getEntryType());
            leaveDetail.setLeaveHours(request.getLeaveHoursPerDay());
            leaveDetail.setWorkHours(request.getLeaveHoursPerDay());
            leaveDetail.setWorkContent("");
            leaveDetail.setRemark("");

            // 合并：保留现有 work 条目 + 新假期条目
            List<DailyReportDetail> mergedDetails = new ArrayList<>(existingDetails);
            mergedDetails.add(leaveDetail);

            // 构造 DailyReport 并调用现有保存逻辑
            DailyReport report = new DailyReport();
            try {
                report.setReportDate(new SimpleDateFormat("yyyy-MM-dd").parse(dateStr));
            } catch (Exception e) {
                throw new ServiceException("日期解析失败：" + dateStr);
            }
            report.setDetailList(mergedDetails);
            saveDailyReport(report);

            current = current.plusDays(1);
        }

        if (totalWorkdays == 0) {
            throw new ServiceException("所选范围内无工作日，未生成任何记录");
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("totalWorkdays", totalWorkdays);
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("overwritten", overwritten);
        return result;
    }
}
