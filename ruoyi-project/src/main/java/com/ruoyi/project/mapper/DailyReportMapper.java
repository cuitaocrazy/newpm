package com.ruoyi.project.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.DailyReport;
import com.ruoyi.project.domain.vo.TeamDailyReportVO;

/**
 * 工作日报Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-26
 */
public interface DailyReportMapper
{
    /**
     * 查询工作日报（含明细）
     *
     * <p><b>【Issue #13 读侧】必须传 userId，语句带 {@code and r.user_id = #{userId}}。</b>
     * 唯一调用方是 {@code GET /project/dailyReport/{reportId}}，reportId 全部来自 URL 且是连续自增；
     * 这个查询既没有 {@code ${params.dataScope}} 也无法挂 {@code @DataScope}（入参不是 BaseEntity），
     * 所以在补 user_id 之前，任何持 {@code project:dailyReport:activity} 的账号（8 个角色，
     * 含普通用户角色 role_id=2）都能逐个 ID 拉走全公司日报正文——比 /list、/monthly
     * （两者都有部门级 dataScope）的越权范围还大。
     *
     * <p>限定为「只能读本人的」而非改成部门级 dataScope：前端没有任何页面调用该接口
     * （{@code src/api/project/dailyReport.js#getDailyReport} 定义了但零引用），
     * 团队/动态视图走的是带 dataScope 的 /list 与 /monthly。
     * 将来若确需跨用户查看单条日报，应新增独立接口并挂 {@code @DataScope}，而不是放宽这里。
     *
     * @param reportId 日报主键
     * @param userId   当前登录用户ID；只返回 user_id 匹配的日报，否则为 null
     * @return 工作日报
     */
    public DailyReport selectDailyReportById(@Param("reportId") Long reportId,
                                            @Param("userId") Long userId);

    /**
     * 根据用户ID和日期查询日报
     *
     * @param userId 用户ID
     * @param reportDate 日报日期
     * @return 工作日报
     */
    public DailyReport selectByUserAndDate(@Param("userId") Long userId, @Param("reportDate") String reportDate);

    /**
     * 根据用户ID和日期查询日报ID（简单查询，用于存在性检查）
     *
     * @param userId 用户ID
     * @param reportDate 日报日期(yyyy-MM-dd)
     * @return 日报ID，不存在则返回null
     */
    public Long selectReportIdByUserAndDate(@Param("userId") Long userId, @Param("reportDate") String reportDate);

    /**
     * 查询工作日报列表（不含明细）
     *
     * @param query 查询条件
     * @return 工作日报集合
     */
    public List<DailyReport> selectDailyReportList(DailyReport query);

    /**
     * 查询月度日报列表（含明细）
     *
     * @param query 查询条件
     * @return 工作日报集合（含明细）
     */
    public List<DailyReport> selectMonthlyReports(DailyReport query);

    /**
     * 新增工作日报
     *
     * @param report 工作日报
     * @return 结果
     */
    public int insertDailyReport(DailyReport report);

    /**
     * 修改工作日报
     *
     * <p>【Issue #13】语句带 {@code and user_id = #{userId}}，与本 Mapper 里其余三条
     * pm_daily_report 写语句（{@link #deleteDailyReportByIds}、{@link #updateTotalWorkHours}）保持一致：
     * <b>这张表上不留任何无归属限定的写原语</b>。
     *
     * <p>当前唯一调用方（{@code saveDailyReport}）的 reportId 由
     * {@link #selectReportIdByUserAndDate} 按 (userId, date) 自行定位，归属本就正确，
     * 所以这条限定此刻是纯纵深防御。它防的是将来新增「reportId 由客户端传入」的调用点：
     * 那时若无此条件，攻击者拿受害者的 reportId 配自己的 userId 调用，就会把受害者的日报
     * <b>改判归属到自己名下</b>——受害者日历上凭空少一天、攻击者名下多一天，
     * 而 pm_daily_report 是硬删除表、无历史版本，事后无从追溯是谁改的。
     *
     * <p>{@code report.userId} 为 null 时条件不成立、更新 0 行（fail-closed），
     * 不会退化成无限定更新。
     *
     * @param report 工作日报；<b>必须已设置 userId</b>
     * @return 结果
     */
    public int updateDailyReport(DailyReport report);

    /**
     * 删除工作日报（<b>硬删除</b>，不可恢复——pm_daily_report 属硬删除例外表）
     *
     * <p><b>⚠️ 无归属限定，当前无任何生产调用方。</b>新增调用点前必须先自行完成归属校验
     * （只能删本人日报，Issue #13），或改用带 userId 的 {@link #deleteDailyReportByIds}。
     * 本方法既绕过归属校验，也绕过 015 的作用范围保护，是这两条防线的回归载体。
     *
     * @param reportId 日报主键
     * @return 结果
     */
    public int deleteDailyReportById(Long reportId);

    /**
     * 【Issue #13】在给定日报ID里挑出「确实存在、但不属于该用户」的那些（归属校验专用的轻量查询）
     *
     * <p>删除日报前必须先过这道校验。原因：{@code DELETE /project/dailyReport/{reportIds}} 的
     * ID 全部来自客户端，而 8 个角色（含普通用户角色 role_id=2）都持有
     * {@code project:dailyReport:remove} 权限、{@code report_id} 又是连续自增——
     * 不校验归属就等于把「删除任意人的日报」开放给全部账号，且日报是<b>硬删除</b>不可恢复。
     *
     * <p>不存在的 report_id 查不出来：删除要保持幂等——过期页面/重复点击会重发已被删掉的 ID，
     * 那种情况按 no-op 处理；只有「查得到、但归属他人」才是越权，必须拒绝。
     *
     * <p><b>为什么带 FOR UPDATE（务必保留）</b>：本方法之后，同一事务还要对这些主记录做
     * {@code updateTotalWorkHours}（UPDATE）或 {@code deleteDailyReportByIds}（DELETE），
     * 两者都需要排他锁。若这里只用普通 SELECT、而明细删除又通过 JOIN 顺带对主记录取共享锁，
     * 就形成 <b>S → X 锁升级</b>：两个并发事务各持 S、各等对方释放才能升 X，必然死锁
     * （本地 MySQL 8 实测 3/3 次 ERROR 1213）。删除接口没有 {@code @RepeatSubmit}，
     * 而填报人重复点击删除是常态，所以这不是理论风险。
     * 在这里<b>一次性取排他锁</b>，后续操作就不再升级——并发退化为排队等待而非死锁。
     *
     * <p><b>为什么返回 owner 而不是「不属于我的 ID」</b>：一次查询同时给出三件事——
     * 锁定、归属、以及<b>该主记录是否存在</b>。第三点是必需的：只处理查得到主记录的 ID，
     * 「主记录已不存在但明细还在」的孤儿数据才不会被带进后续的读写段
     * （否则会读到他人明细，并对无数据权限的项目发起 actual_workload 重算）。
     *
     * @param reportIds 待校验的日报ID集合
     * @return 每项含 reportId 与 userId；不存在的 ID 不出现在结果中
     */
    List<Map<String, Object>> selectReportOwnersForUpdate(
            @Param("reportIds") java.util.Collection<Long> reportIds);

    /**
     * 批量删除工作日报（<b>硬删除</b>，不可恢复）
     *
     * <p>【Issue #13】语句带 {@code and user_id = #{userId}}：日报是硬删除，误删只能靠 6 小时一次的
     * OSS 归档备份（需付费解冻），所以「只能删自己的」这条限定要落到 SQL 里，
     * 不能只依赖 Service 层的前置校验——后者一旦被新调用点绕过就没有第二道防线。
     *
     * <p>不设管理员例外，理由见 {@code DailyReportDetailMapper#deleteByReportIdInScope}。
     *
     * @param reportIds 需要删除的数据主键集合
     * @param userId    当前登录用户ID；只有 user_id 匹配的行会被删除
     * @return 结果
     */
    public int deleteDailyReportByIds(@Param("reportIds") Long[] reportIds,
                                      @Param("userId") Long userId);

    /**
     * 查询活动页可见用户列表（按数据权限 + deptId 过滤）
     *
     * @param query 查询条件（deptId + dataScope）
     * @return 用户列表
     */
    public List<Map<String, Object>> selectActivityUsers(DailyReport query);

    /**
     * 按日期统计已提交人数（指定日期范围内每天去重计数）
     * 用于日报统计报表的汇总数据
     */
    List<Map<String, Object>> selectSubmittedCountByDate(DailyReport query);

    /**
     * 查询数据权限范围内的活跃用户总数（排除白名单）
     * 用于计算未提交人数 = 总数 - 已提交数
     */
    int selectTotalUserCount(DailyReport query);

    /**
     * 按天统计需提交日报的用户数（基于项目成员 join_date/leave_date）
     * 返回 List&lt;Map&gt;，每项包含 reportDate 和 totalCount
     */
    List<Map<String, Object>> selectTotalUserCountByDate(DailyReport query);

    /**
     * 查询某天已提交人员明细（含工时和工作内容摘要）
     */
    List<Map<String, Object>> selectSubmittedUsersOnDate(DailyReport query);

    /**
     * 查询某天未提交人员明细（排除白名单）
     */
    List<Map<String, Object>> selectUnsubmittedUsersOnDate(DailyReport query);

    /**
     * 查询日报统计报表专用部门树（三级及以下，三级节点 parentId=0 作为根节点）
     */
    List<Map<String, Object>> selectStatsDeptTree(DailyReport query);

    /**
     * 团队日报 - 按部门+月份查询原始平铺行（项目×成员×日期）
     * Java 层再按 projectId → userId 聚合
     */
    List<Map<String, Object>> selectTeamMonthlyRaw(DailyReport query);

    /**
     * 团队日报 - 项目名称 autocomplete（按部门范围模糊搜索，最多20条）
     */
    List<Map<String, Object>> selectTeamProjectOptions(DailyReport query);

    /**
     * 重算并更新日报主记录的当日汇总工时
     *
     * <p>用于删除日报时：若有明细因不在填报人可见范围内而被保留，主记录必须一并保留，
     * 其汇总工时须按剩余 work 明细重算，否则会与明细不符（INV-D2 / SC-010）。
     *
     * <p><b>实现约束</b>：SQL 必须带 {@code update_time = update_time}，
     * 不得触碰审计字段（宪法 IV）。
     *
     * <p>【Issue #13】语句带 {@code and user_id = #{userId}}。这条限定容易被忽略却是必需的：
     * 若只给删除语句补归属，攻击者传入他人 reportId 时删除删不到行、
     * 但 countByReportId 仍会读到受害者的真实明细数（&gt;0）而走进「保留主记录」分支，
     * 于是这条 update 就成了残存的跨用户写原语，会强制改写他人日报的汇总工时。
     *
     * @param reportId 日报ID
     * @param hours    重算后的当日汇总工时
     * @param userId   当前登录用户ID；只有 user_id 匹配的行会被更新
     * @return 结果
     */
    int updateTotalWorkHours(@Param("reportId") Long reportId,
                             @Param("hours") BigDecimal hours,
                             @Param("userId") Long userId);
}
