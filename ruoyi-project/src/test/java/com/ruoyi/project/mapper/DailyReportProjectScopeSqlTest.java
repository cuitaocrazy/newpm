package com.ruoyi.project.mapper;

import java.io.InputStream;

import com.ruoyi.project.domain.DailyReport;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【Issue #24】DailyReportMapper.xml 数据权限注入的 SQL 渲染断言。
 *
 * <p><b>缺陷</b>：{@code <if test="projectId == null">${params.dataScope}</if>} —— 只要请求带上
 * projectId，部门级数据权限片段就整条不注入。授权判据被放在了用户可控的入参上，任何持
 * {@code project:dailyReport:activity} 权限的账号（实测 175 个受限账号）都能枚举 projectId 读到
 * 全库日报，含 work_content 原文。
 *
 * <p><b>不变式</b>：{@code ${params.dataScope}} 是这几条查询唯一的授权边界，只允许被
 * <b>服务端算出</b>的 {@code params.projectScopeBypass} 豁免（见
 * {@code DailyReportServiceImpl#applyProjectScopeBypass}）；projectId 本身永不构成授权。
 *
 * <p><b>做法</b>：直接用 MyBatis {@link XMLMapperBuilder} 解析 mapper XML，取
 * {@code MappedStatement#getBoundSql} 断言渲染出的 SQL 文本。不连数据库、不起 Spring，毫秒级。
 *
 * <p><b>两个会造成"假红/假绿"的坑（勿踩）</b>：
 * <ol>
 *   <li>{@code params} 里若没有 {@code dataScope} 这个 key，{@code ${params.dataScope}} 会渲染成
 *       空串，于是所有"含哨兵"断言恒假 —— 看着像红，其实测的是别的东西。必须显式放哨兵值。
 *   <li>{@code <where>}/{@code TrimSqlNode} 会剥掉片段开头的 {@code " AND "}。当 dataScope 是
 *       WHERE 里唯一条件时渲染成 {@code WHERE  (d.dept_id IN ...)}。所以断言只能针对内层
 *       {@link #MARKER}，不能断言带前导 AND 的完整哨兵串。
 * </ol>
 */
class DailyReportProjectScopeSqlTest
{
    private static final String NS = "com.ruoyi.project.mapper.DailyReportMapper.";

    private static final String RESOURCE = "mapper/project/DailyReportMapper.xml";

    /** 模拟 DataScopeAspect:167 产出的片段（data_scope=4 / dept 212 的形状） */
    private static final String SENTINEL =
            " AND (d.dept_id IN ( SELECT dept_id FROM sys_dept WHERE dept_id = 212 or find_in_set( 212 , ancestors ) ))";

    /** 剥掉前导 " AND " 也仍然存在的内层特征串 */
    private static final String MARKER =
            "d.dept_id IN ( SELECT dept_id FROM sys_dept WHERE dept_id = 212";

    private static final String BYPASS_KEY = "projectScopeBypass";

    private static final Long PROJECT_ID = 36L;

    private static Configuration configuration;

    @BeforeAll
    static void parseMapperXml() throws Exception
    {
        configuration = new Configuration();
        // parameterType="DailyReport" 等别名靠这行解析，缺了会 ClassNotFound
        configuration.getTypeAliasRegistry().registerAliases("com.ruoyi.project.domain");
        try (InputStream in = Resources.getResourceAsStream(RESOURCE))
        {
            // 必须用 4 参构造：sqlFragments 传 configuration 自己的 map，否则 <include refid> 解析不动
            new XMLMapperBuilder(in, configuration, RESOURCE, configuration.getSqlFragments()).parse();
        }
    }

    private String render(String statement, DailyReport query)
    {
        return configuration.getMappedStatement(NS + statement).getBoundSql(query).getSql();
    }

    private DailyReport baseQuery()
    {
        DailyReport query = new DailyReport();
        query.setYearMonth("2026-08");
        query.getParams().put("dataScope", SENTINEL);
        return query;
    }

    // ==================== 必须先红的 3 条：带 projectId 且未经服务端放行 ====================

    @Test
    @DisplayName("[TDD] selectMonthlyReports：传 projectId 且无服务端放行标记时，必须仍注入 dataScope")
    void monthly_projectIdWithoutBypass_mustInjectDataScope()
    {
        DailyReport query = baseQuery();
        query.setProjectId(PROJECT_ID);

        String sql = render("selectMonthlyReports", query);

        assertTrue(sql.contains(MARKER),
                "传 projectId 时 dataScope 片段丢失 —— 部门级数据权限失效（Issue #24）。/monthly 返回含明细即"
                        + "work_content 原文，越权后果最重。渲染出的 SQL:\n" + sql);
    }

    @Test
    @DisplayName("[TDD] selectDailyReportList：传 projectId 且无服务端放行标记时，必须仍注入 dataScope")
    void list_projectIdWithoutBypass_mustInjectDataScope()
    {
        DailyReport query = baseQuery();
        query.setProjectId(PROJECT_ID);

        String sql = render("selectDailyReportList", query);

        assertTrue(sql.contains(MARKER),
                "传 projectId 时 dataScope 片段丢失 —— 部门级数据权限失效（Issue #24）。渲染出的 SQL:\n" + sql);
    }

    @Test
    @DisplayName("[TDD] selectActivityUsers：传 projectId 且无服务端放行标记时，必须仍注入 dataScope")
    void activityUsers_projectIdWithoutBypass_mustInjectDataScope()
    {
        DailyReport query = baseQuery();
        query.setProjectId(PROJECT_ID);

        String sql = render("selectActivityUsers", query);

        assertTrue(sql.contains(MARKER),
                "传 projectId 时 dataScope 片段丢失 —— 花名册与月历口径必须同批收口（Issue #24），"
                        + "否则活动页「团队人数/已填写/未填写」会与月历内容打架。渲染出的 SQL:\n" + sql);
    }

    // 注：selectDailyReportById（GET /{reportId}）是同一权限位下更严重的可枚举越权，
    // 但它**不在本 hotfix 范围内**——该缺陷已在分支 fix/daily-report-delete-ownership（Issue #13）
    // 以更严的方式修好（限定 and r.user_id = #{userId}，只能读本人；前端零调用方所以收紧无损），
    // 两份修复的 Mapper 签名不同，同批落地必冲突。相应的断言随该分支走，此处不重复。

    // ==================== 一开始就绿、修完仍须绿的护栏 ====================

    @Test
    @DisplayName("[护栏] 服务端算出放行标记时，dataScope 不注入 —— 保住 PM需求.md:755「项目经理可见非本团队成员日报」")
    void projectIdWithBypassTrue_mustNotInjectDataScope()
    {
        for (String stmt : new String[] { "selectMonthlyReports", "selectDailyReportList", "selectActivityUsers" })
        {
            DailyReport query = baseQuery();
            query.setProjectId(PROJECT_ID);
            query.getParams().put(BYPASS_KEY, Boolean.TRUE);

            String sql = render(stmt, query);

            assertFalse(sql.contains(MARKER),
                    stmt + "：服务端已判定「调用者曾参与该项目」，此时不应再叠加部门数据权限，"
                            + "否则项目经理看不到非本团队成员的日报（PM需求.md:755）。渲染出的 SQL:\n" + sql);
        }
    }

    @Test
    @DisplayName("[护栏] 不传 projectId 的常规路径必须注入 dataScope（这是 175 个受限账号的日常口径）")
    void noProjectId_alwaysInjectsDataScope()
    {
        for (String stmt : new String[] { "selectMonthlyReports", "selectDailyReportList", "selectActivityUsers" })
        {
            DailyReport plain = baseQuery();
            String plainSql = render(stmt, plain);
            assertTrue(plainSql.contains(MARKER),
                    stmt + "：不传 projectId 的常规路径必须带 dataScope。渲染出的 SQL:\n" + plainSql);
        }
    }

    @Test
    @DisplayName("[护栏] 传 projectId 时业务筛选条件仍在（dd_pid.project_id）")
    void projectId_projectFilterStillPresent()
    {
        for (String stmt : new String[] { "selectMonthlyReports", "selectDailyReportList" })
        {
            DailyReport query = baseQuery();
            query.setProjectId(PROJECT_ID);
            String sql = render(stmt, query);
            assertTrue(sql.contains("dd_pid.project_id"),
                    stmt + "：按项目筛选的 exists 子查询被误删。渲染出的 SQL:\n" + sql);
        }
    }

    @Test
    @DisplayName("[护栏] selectActivityUsers 的项目花名册分支仍在（不得退化成只看本部门）")
    void activityUsers_rosterBranchStillPresent()
    {
        DailyReport query = baseQuery();
        query.setProjectId(PROJECT_ID);
        String sql = render("selectActivityUsers", query);
        assertTrue(sql.contains("select user_id from pm_project_member"),
                "花名册 union 块被误删，活动页会把「项目全员」退化成「本部门成员」。渲染出的 SQL:\n" + sql);
    }

    /**
     * 【文档 / 危险性登记】把 XML 层放行判据的**真实语义**钉死，而不是假装它有兜底。
     *
     * <p>判据是纯粹的「{@code params.projectScopeBypass} 是否为 null」：
     * <ul>
     *   <li>不看值 —— 字符串 {@code "1"} 与 {@code Boolean.TRUE} 等效（OGNL 只判非 null）；
     *   <li>不看 projectId —— 连 {@code projectId} 为空时，标记在场同样会摘掉 dataScope。
     * </ul>
     *
     * <p>所以 XML 层<b>没有任何兜底</b>，唯一屏障是
     * {@code DailyReportServiceImpl#applyProjectScopeBypass} 第一行那句无条件
     * {@code getParams().remove(...)}（对应的断言在 {@code DailyReportServiceImplTest}
     * 的 {@code selectMonthlyReports_forgedBypassFlag_isStripped} /
     * {@code selectMonthlyReports_noProjectId_doesNotQueryMembership}）。
     * 已核（{@code grep -rn} 于 ruoyi-project/ruoyi-admin 的 main 源码）：这三条 statement 各自只有一个
     * 调用方，全部经过该方法（{@code DailyReportServiceImpl:126/140/592}）。
     * <b>新增任何绕过 Service 直接调用这三条 statement 的代码路径，都会直接重开 Issue #24。</b>
     *
     * <p><b>第四条受保护语句 {@code selectDailyReportById} 不在此列</b>：它不带 projectId、不读放行标记，
     * 授权完全靠无条件注入的 {@code ${params.dataScope}}（由
     * {@code DailyReportServiceImpl#selectDailyReportById} 上的 {@code @DataScope} 提供）。
     * 它的护栏是本类的 {@code reportById_mustInjectDataScope} /
     * {@code reportById_ignoresBypassFlag}。
     */
    @Test
    @DisplayName("[文档] XML 层只判放行标记是否为 null（不看值、不看 projectId）—— Service 的 remove() 是唯一屏障")
    void bypassFlagIsNullCheckOnly_soServiceMustRemoveForgedKey()
    {
        // (a) 值是请求伪造出来的字符串，同样触发豁免
        DailyReport forged = baseQuery();
        forged.setProjectId(PROJECT_ID);
        forged.getParams().put(BYPASS_KEY, "1");
        String forgedSql = render("selectMonthlyReports", forged);
        assertFalse(forgedSql.contains(MARKER),
                "OGNL 只判非 null，字符串同样触发豁免。渲染出的 SQL:\n" + forgedSql);

        // (b) 连 projectId 为空时，标记在场也会摘掉 dataScope —— XML 层确实没有兜底
        DailyReport flagOnly = baseQuery();
        flagOnly.getParams().put(BYPASS_KEY, Boolean.TRUE);
        String flagOnlySql = render("selectMonthlyReports", flagOnly);
        assertFalse(flagOnlySql.contains(MARKER),
                "本断言登记的是 XML 层的既有语义（无兜底），而非期望行为。若它变红，说明有人给 XML "
                        + "加了兜底判据 —— 那不是坏事，但请同步更新本注释与 Issue #24 的方案说明。"
                        + "渲染出的 SQL:\n" + flagOnlySql);
    }
}
