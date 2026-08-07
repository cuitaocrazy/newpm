package com.ruoyi.project.mapper;

import java.io.InputStream;
import java.util.Locale;

import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.domain.Payment;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【Issue #36 / specs/021-payment-invoice-date-filter】付款里程碑「开票日期」区间查询的 SQL 渲染断言。
 *
 * <p><b>业务</b>：付款里程碑管理页新增「开票日期」起止筛选。物理列是
 * {@code pm_payment.submit_acceptance_date}（历史命名「提交验收日期」，业务含义已改为开票日期，
 * <b>列名未改</b>；库里不存在 {@code invoice_date} 这一列）。请求参数名为
 * {@code submitAcceptanceDateStart} / {@code submitAcceptanceDateEnd}，由
 * {@code PaymentController} 写进 {@code contract.getParams()}。
 *
 * <p><b>本类存在的理由</b>：该筛选条件同时落在 <b>三条</b> SQL 语句上，
 * 三条各自被不同接口使用，任何一条漏改都会造成「列表筛掉了、合计没筛」这类静默口径打架：
 * <ol>
 *   <li>{@code ContractMapper.selectContractWithPaymentsList} — {@code GET /project/payment/listWithContracts}
 *       与 {@code POST /project/payment/export} 共用；
 *   <li>{@code ContractMapper.sumPaymentAmount} — {@code GET /project/payment/sumPaymentAmount}（页脚合计）；
 *   <li>{@code PaymentMapper.selectPaymentList} — 款项列表。
 * </ol>
 *
 * <p><b>做法</b>：照搬 {@link DailyReportProjectScopeSqlTest} 的路子 —— 直接用 MyBatis
 * {@link XMLMapperBuilder} 解析 mapper XML，取 {@link BoundSql#getSql()} 断言渲染出的 SQL 文本。
 * 不连数据库、不起 Spring，毫秒级。
 *
 * <p><b>补写缘由（假绿事故登记）</b>：{@code specs/021-payment-invoice-date-filter/bdd/coverage.md} 曾声称
 * 本类已存在（T1~T8），但文件<b>根本不存在</b>；而它给出的验收命令带
 * {@code -Dsurefire.failIfNoSpecifiedTests=false} —— 该 flag 会让「测试类不存在」也 BUILD SUCCESS，
 * 于是跑 0 个用例照样绿。<b>验收时必须看 {@code Tests run: N} 那一行确认 N 符合预期，
 * 不能只看 BUILD SUCCESS。</b>
 *
 * <p><b>三个会造成「假绿/假红」的坑（勿踩）</b>：
 * <ol>
 *   <li><b>【最关键】{@code submit_acceptance_date} 本来就在两条列表语句的 SELECT 字段列表里</b>
 *       （{@code ContractMapper.xml:376} / {@code PaymentMapper.xml:50}）。所以
 *       {@code assertTrue(sql.contains("submit_acceptance_date"))} 是<b>恒真的废断言</b> ——
 *       把整个 {@code <if>} 删掉它依然绿。断言必须落在<b>完整的比较形状</b>上
 *       （{@link #BARE_GE} / {@link #FMT_GE} 这种「列 + 比较符 + 占位符」的整串）。
 *       该恒真性由 {@link #trapRegistry_bareColumnNameIsAlwaysPresent} 现场证明并钉死。
 *   <li>XML 里的缩进/换行会让 {@code contains} 失配（PaymentMapper 的两个 {@code <if>} 是换行写法）。
 *       所有断言一律先过 {@link #norm(String)} 把连续空白折叠为单空格。
 *   <li>{@code params} 里若没有 {@code dataScope} 这个 key，{@code ${params.dataScope}} 渲染成空串，
 *       于是所有「相对位置」断言恒假 —— 看着像红，其实测的是别的东西。必须显式放哨兵值
 *       （{@link #DATA_SCOPE_SENTINEL}）。
 * </ol>
 *
 * <p><b>两种写法故意不同，不得互相串味</b>：ContractMapper 的两条用<b>裸列比较</b>
 * （{@code p.submit_acceptance_date >= ?}，与该文件既有的 {@code actual_payment_date} 逐字一致）；
 * PaymentMapper 用 <b>date_format 归一化</b>（{@code date_format(...,'%Y-%m-%d') >= date_format(?,'%Y-%m-%d')}，
 * 同样与该文件既有的 {@code actual_payment_date} 逐字一致）。本次实现的原则是「与同文件既有日期条件
 * 保持同一风格」，而不是「全仓统一成一种」—— 因此 INV-2 双向锁死：串味了要红。
 */
class PaymentInvoiceDateFilterSqlTest
{
    private static final String CONTRACT_RESOURCE = "mapper/project/ContractMapper.xml";

    private static final String PAYMENT_RESOURCE = "mapper/project/PaymentMapper.xml";

    /** ContractMapper 风格：裸列比较（norm 后的形状） */
    private static final String BARE_GE = "p.submit_acceptance_date >= ?";

    private static final String BARE_LE = "p.submit_acceptance_date <= ?";

    /** PaymentMapper 风格：date_format 两端归一化（norm 后的形状） */
    private static final String FMT_GE =
            "date_format(p.submit_acceptance_date,'%Y-%m-%d') >= date_format(?,'%Y-%m-%d')";

    private static final String FMT_LE =
            "date_format(p.submit_acceptance_date,'%Y-%m-%d') <= date_format(?,'%Y-%m-%d')";

    /** 只要出现这个前缀，就说明该语句用了 date_format 风格（用于 INV-2 的串味检测） */
    private static final String FMT_MARKER = "date_format(p.submit_acceptance_date";

    /** 与本次新增条件共存的既有条件（INV-7），两种风格各一份 */
    private static final String BARE_ACTUAL_GE = "p.actual_payment_date >= ?";

    private static final String BARE_ACTUAL_LE = "p.actual_payment_date <= ?";

    private static final String FMT_ACTUAL_GE =
            "date_format(p.actual_payment_date,'%Y-%m-%d') >= date_format(?,'%Y-%m-%d')";

    private static final String FMT_ACTUAL_LE =
            "date_format(p.actual_payment_date,'%Y-%m-%d') <= date_format(?,'%Y-%m-%d')";

    /** 模拟 DataScopeAspect 产出的片段（data_scope=4 / dept 212 的形状） */
    private static final String DATA_SCOPE_SENTINEL =
            " AND (c.dept_id IN ( SELECT dept_id FROM sys_dept WHERE dept_id = 212 or find_in_set( 212 , ancestors ) ))";

    /** 剥掉前导 " AND " 也仍然存在的内层特征串 */
    private static final String DATA_SCOPE_MARKER =
            "c.dept_id IN ( SELECT dept_id FROM sys_dept WHERE dept_id = 212";

    private static final String START = "2026-01-01";

    private static final String END = "2026-03-31";

    private static Configuration configuration;

    /** 本特性涉及的三条语句。{@code dateFormatStyle} 决定它期望的比较片段形状。 */
    private enum Stmt
    {
        /** 列表页 + 导出共用 */
        CONTRACT_LIST("com.ruoyi.project.mapper.ContractMapper.selectContractWithPaymentsList", false, true),
        /** 页脚合计。注意：这条语句本来就<b>没有</b> {@code ${params.dataScope}}，见 INV-3 的说明 */
        CONTRACT_SUM("com.ruoyi.project.mapper.ContractMapper.sumPaymentAmount", false, false),
        /** 款项列表 */
        PAYMENT_LIST("com.ruoyi.project.mapper.PaymentMapper.selectPaymentList", true, true);

        private final String id;

        private final boolean dateFormatStyle;

        private final boolean hasDataScope;

        Stmt(String id, boolean dateFormatStyle, boolean hasDataScope)
        {
            this.id = id;
            this.dateFormatStyle = dateFormatStyle;
            this.hasDataScope = hasDataScope;
        }

        String ge()
        {
            return dateFormatStyle ? FMT_GE : BARE_GE;
        }

        String le()
        {
            return dateFormatStyle ? FMT_LE : BARE_LE;
        }

        boolean isContract()
        {
            return id.startsWith("com.ruoyi.project.mapper.ContractMapper.");
        }
    }

    @BeforeAll
    static void parseMapperXml() throws Exception
    {
        configuration = new Configuration();
        // parameterType="Contract" / "Payment" 等别名靠这行解析，缺了会 ClassNotFound
        configuration.getTypeAliasRegistry().registerAliases("com.ruoyi.project.domain");
        parse(CONTRACT_RESOURCE);
        parse(PAYMENT_RESOURCE);
    }

    private static void parse(String resource) throws Exception
    {
        try (InputStream in = Resources.getResourceAsStream(resource))
        {
            // 必须用 4 参构造：sqlFragments 传 configuration 自己的 map，否则 <include refid> 解析不动
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    // ==================== 基础设施 ====================

    /** 连续空白折叠成单空格 —— 不做归一化，XML 缩进会让所有 contains 失配（坑 2） */
    private static String norm(String sql)
    {
        return sql.replaceAll("\\s+", " ").trim();
    }

    /**
     * 渲染指定语句。{@code start}/{@code end} 传 null 表示「该参数没进 params」
     * —— 与 {@code PaymentController} 里 {@code if (x != null) params.put(...)} 的行为一致。
     */
    private static String render(Stmt stmt, String start, String end)
    {
        Object param = stmt.isContract() ? new Contract() : new Payment();
        java.util.Map<String, Object> params = stmt.isContract()
                ? ((Contract) param).getParams()
                : ((Payment) param).getParams();
        params.put("dataScope", DATA_SCOPE_SENTINEL);
        if (start != null)
        {
            params.put("submitAcceptanceDateStart", start);
        }
        if (end != null)
        {
            params.put("submitAcceptanceDateEnd", end);
        }
        return norm(configuration.getMappedStatement(stmt.id).getBoundSql(param).getSql());
    }

    private static void assertContains(String sql, String fragment, String why)
    {
        assertTrue(sql.contains(fragment),
                why + "\n缺失片段: " + fragment + "\n渲染出的 SQL:\n" + sql);
    }

    private static void assertNotContains(String sql, String fragment, String why)
    {
        assertFalse(sql.contains(fragment),
                why + "\n不该出现的片段: " + fragment + "\n渲染出的 SQL:\n" + sql);
    }

    // ==================== INV-1：三条语句都必须渲染出比较片段 ====================

    @Test
    @DisplayName("[INV-1] selectContractWithPaymentsList：只传开票日期区间 → 渲染出裸列比较（列表页 + 导出共用）")
    void inv1_contractList_rendersBareColumnComparison()
    {
        String sql = render(Stmt.CONTRACT_LIST, START, END);
        assertContains(sql, BARE_GE, "列表页/导出的开票日期起始条件没进 SQL —— 用户选了区间却查回全量（Issue #36）。");
        assertContains(sql, BARE_LE, "列表页/导出的开票日期结束条件没进 SQL（Issue #36）。");
    }

    @Test
    @DisplayName("[INV-1] sumPaymentAmount：只传开票日期区间 → 渲染出裸列比较（页脚合计必须与列表同口径）")
    void inv1_contractSum_rendersBareColumnComparison()
    {
        String sql = render(Stmt.CONTRACT_SUM, START, END);
        assertContains(sql, BARE_GE,
                "页脚合计漏了开票日期起始条件 —— 列表已按区间筛过、合计仍是全量，两个数字对不上，"
                        + "是最容易被当成「数据错了」上报的静默口径打架。");
        assertContains(sql, BARE_LE, "页脚合计漏了开票日期结束条件（同上，口径与列表打架）。");
    }

    @Test
    @DisplayName("[INV-1] selectPaymentList：只传开票日期区间 → 渲染出 date_format 归一化比较")
    void inv1_paymentList_rendersDateFormatComparison()
    {
        String sql = render(Stmt.PAYMENT_LIST, START, END);
        assertContains(sql, FMT_GE, "款项列表的开票日期起始条件没进 SQL（Issue #36）。");
        assertContains(sql, FMT_LE, "款项列表的开票日期结束条件没进 SQL（Issue #36）。");
    }

    // ==================== INV-2：两种风格不得串味 ====================

    /**
     * ContractMapper 的两条语句用裸列比较，与该文件既有的 {@code actual_payment_date} 条件逐字一致。
     * 一旦有人「顺手统一」成 date_format，虽然结果集多半不变，但会
     * ①让 {@code submit_acceptance_date} 上的索引失效（列被函数包裹 → 无法走索引），
     * ②与同文件相邻两行的写法不一致，成为下一次改动的误导样板。
     */
    @Test
    @DisplayName("[INV-2] ContractMapper 的两条语句不得出现 date_format(p.submit_acceptance_date —— 风格串味即红")
    void inv2_contractStatements_mustNotUseDateFormatStyle()
    {
        for (Stmt stmt : new Stmt[] { Stmt.CONTRACT_LIST, Stmt.CONTRACT_SUM })
        {
            String sql = render(stmt, START, END);
            assertNotContains(sql, FMT_MARKER,
                    stmt.id + "：本该用裸列比较（与同文件既有 actual_payment_date 条件逐字一致），"
                            + "却出现了 date_format 包裹 —— 列被函数包裹会让该列上的索引失效。");
        }
    }

    /**
     * PaymentMapper 反过来：它既有的 {@code actual_payment_date} 条件就是 date_format 归一化写法
     * （因为该列在这条语句里可能是 datetime，需要按天比较）。开票日期必须跟随同文件风格。
     */
    @Test
    @DisplayName("[INV-2] PaymentMapper 必须用 date_format 风格，且不得退化成裸列比较")
    void inv2_paymentList_mustUseDateFormatStyle()
    {
        String sql = render(Stmt.PAYMENT_LIST, START, END);
        assertContains(sql, FMT_MARKER,
                "selectPaymentList 的开票日期条件丢了 date_format 归一化 —— 该文件既有 actual_payment_date "
                        + "就是按天归一化比较，风格不一致会让「同一天的记录被筛掉」这类时分秒边界问题复发。");
        assertNotContains(sql, BARE_GE, "selectPaymentList 出现了 ContractMapper 风格的裸列比较（风格串味）。");
        assertNotContains(sql, BARE_LE, "selectPaymentList 出现了 ContractMapper 风格的裸列比较（风格串味）。");
    }

    // ==================== INV-3：<if> 必须在 ${params.dataScope} 之前 ====================

    /**
     * 新增的 {@code <if>} 必须插在 {@code ${params.dataScope}} <b>之前</b>。
     *
     * <p>为什么要钉死相对位置：{@code ${}} 是<b>字符串直接拼接</b>（非占位符），
     * 数据权限片段自带前导 {@code " AND "}。若把新条件挪到它<b>之后</b>，SQL 变成
     * {@code ... AND (dept_id IN (...)) and p.submit_acceptance_date >= ?} —— 本次凑巧仍成立，
     * 但已经脱离了「所有业务条件在前、权限收口在后」的既定形状；一旦将来数据权限片段带上
     * {@code OR} 顶层分支或换成 {@code ${}} 拼子句，位置错乱会直接放大数据可见范围。
     * 这里用 {@link #DATA_SCOPE_MARKER} 的 {@code indexOf} 比较位置来锁死。
     *
     * <p><b>{@code sumPaymentAmount} 不适用本断言</b>：那条语句<b>本来就没有</b>
     * {@code ${params.dataScope}}（见 {@code ContractMapper.xml:435-464}，{@code <where>} 里
     * 从 {@code c.del_flag} 一路到收尾都没有数据权限片段）。这是<b>既有缺陷</b>
     * ——「合计」接口不受部门数据权限约束，与它配对的列表接口却受约束 —— <b>已知，且不在本次
     * 特性范围内</b>，本类不做修改也不做掩饰。对它改为断言片段落在 {@code <where>} 子句内
     * （见 {@link #inv3_contractSum_fragmentInsideWhereClause}）。
     */
    @Test
    @DisplayName("[INV-3] 带 dataScope 的两条语句：开票日期片段必须出现在 dataScope 哨兵之前")
    void inv3_fragmentRenderedBeforeDataScope()
    {
        for (Stmt stmt : new Stmt[] { Stmt.CONTRACT_LIST, Stmt.PAYMENT_LIST })
        {
            String sql = render(stmt, START, END);

            int scopeAt = sql.indexOf(DATA_SCOPE_MARKER);
            assertTrue(scopeAt >= 0,
                    stmt.id + "：dataScope 哨兵没渲染出来 —— 说明 ${params.dataScope} 被挪走或删了，"
                            + "本用例的位置断言会失去意义（假红）。渲染出的 SQL:\n" + sql);

            int geAt = sql.indexOf(stmt.ge());
            int leAt = sql.indexOf(stmt.le());
            assertTrue(geAt >= 0 && leAt >= 0,
                    stmt.id + "：开票日期比较片段缺失，位置断言无从谈起。渲染出的 SQL:\n" + sql);

            assertTrue(geAt < scopeAt,
                    stmt.id + "：开票日期起始条件跑到了 ${params.dataScope} 之后 —— 业务条件必须全部排在"
                            + "数据权限收口之前。渲染出的 SQL:\n" + sql);
            assertTrue(leAt < scopeAt,
                    stmt.id + "：开票日期结束条件跑到了 ${params.dataScope} 之后。渲染出的 SQL:\n" + sql);
        }
    }

    /**
     * {@code sumPaymentAmount} 的 INV-3 变体。理由见
     * {@link #inv3_fragmentRenderedBeforeDataScope} 的 Javadoc：这条语句没有
     * {@code ${params.dataScope}}，无法用哨兵定位，改为断言片段确实落在 {@code <where>} 子句内部
     * —— 即出现在 {@code WHERE} 关键字之后，且它与 {@code WHERE} 之间没有
     * {@code order by / group by / having / limit} 这类会把它挤出 WHERE 的子句边界。
     */
    @Test
    @DisplayName("[INV-3'] sumPaymentAmount（无 dataScope）：开票日期片段必须落在 where 子句内")
    void inv3_contractSum_fragmentInsideWhereClause()
    {
        String sql = render(Stmt.CONTRACT_SUM, START, END);

        assertNotContains(sql, DATA_SCOPE_MARKER,
                "sumPaymentAmount 现在渲染出了 dataScope —— 这是好事（既有缺陷被修了），"
                        + "但本用例的「无 dataScope」前提失效了，请把它并回 inv3_fragmentRenderedBeforeDataScope，"
                        + "并同步更新该方法的 Javadoc 与 specs/021 的说明。");

        int whereAt = sql.toUpperCase(Locale.ROOT).indexOf("WHERE ");
        assertTrue(whereAt >= 0, "sumPaymentAmount 渲染不出 WHERE 子句。渲染出的 SQL:\n" + sql);

        for (String fragment : new String[] { BARE_GE, BARE_LE })
        {
            int at = sql.indexOf(fragment);
            assertTrue(at > whereAt,
                    "开票日期条件没落在 WHERE 子句里（出现在 WHERE 之前，多半是被误插进了 select 字段列表）。"
                            + "\n片段: " + fragment + "\n渲染出的 SQL:\n" + sql);

            String between = sql.substring(whereAt, at).toLowerCase(Locale.ROOT);
            for (String boundary : new String[] { "order by", "group by", "having", "limit" })
            {
                assertFalse(between.contains(boundary),
                        "开票日期条件与 WHERE 之间出现了子句边界「" + boundary + "」—— 它已经不在 WHERE 里了。"
                                + "\n片段: " + fragment + "\n渲染出的 SQL:\n" + sql);
            }
        }
    }

    // ==================== INV-4：空串不触发 ====================

    /**
     * {@code <if>} 的判据是 {@code != null and != ''}。前端「清空日期选择器」时，
     * Element Plus 的 el-date-picker 会回传空串（不是 null），经 {@code @RequestParam} 进来仍是
     * {@code ""} —— 若 {@code <if>} 只判 null，就会渲染出
     * {@code p.submit_acceptance_date >= ''}，把所有记录筛没。
     */
    @Test
    @DisplayName("[INV-4] 空串不得触发条件 —— 清空日期框不能把结果集筛成 0 行")
    void inv4_emptyStringDoesNotTrigger()
    {
        for (Stmt stmt : Stmt.values())
        {
            String sql = render(stmt, "", "");
            assertNotContains(sql, stmt.ge(),
                    stmt.id + "：空串触发了开票日期起始条件 —— 用户清空日期框后会渲染出「>= ''」，结果集变 0 行。");
            assertNotContains(sql, stmt.le(),
                    stmt.id + "：空串触发了开票日期结束条件 —— 用户清空日期框后会渲染出「<= ''」，结果集变 0 行。");
        }
    }

    // ==================== INV-5：单边可用 ====================

    @Test
    @DisplayName("[INV-5] 只传起始 → 只出现 >= 片段，不得出现 <= 片段")
    void inv5_startOnly_rendersOnlyLowerBound()
    {
        for (Stmt stmt : Stmt.values())
        {
            String sql = render(stmt, START, null);
            assertContains(sql, stmt.ge(), stmt.id + "：只传起始时，>= 条件没渲染出来。");
            assertNotContains(sql, stmt.le(),
                    stmt.id + "：只传起始却渲染出了 <= 条件 —— 上界会拿到 null，「开票日期之后的全部」查不出来。");
        }
    }

    @Test
    @DisplayName("[INV-5] 只传结束 → 只出现 <= 片段，不得出现 >= 片段")
    void inv5_endOnly_rendersOnlyUpperBound()
    {
        for (Stmt stmt : Stmt.values())
        {
            String sql = render(stmt, null, END);
            assertContains(sql, stmt.le(), stmt.id + "：只传结束时，<= 条件没渲染出来。");
            assertNotContains(sql, stmt.ge(),
                    stmt.id + "：只传结束却渲染出了 >= 条件 —— 下界会拿到 null，「开票日期之前的全部」查不出来。");
        }
    }

    // ==================== INV-6：不传不影响既有行为 ====================

    @Test
    @DisplayName("[INV-6] 两端都不传 → 渲染结果与本特性上线前一致（不含任何开票日期比较片段）")
    void inv6_noBounds_rendersNoInvoiceDatePredicate()
    {
        for (Stmt stmt : Stmt.values())
        {
            String sql = render(stmt, null, null);
            assertNotContains(sql, BARE_GE, stmt.id + "：不传开票日期却渲染出了比较条件，既有查询被污染。");
            assertNotContains(sql, BARE_LE, stmt.id + "：不传开票日期却渲染出了比较条件，既有查询被污染。");
            assertNotContains(sql, FMT_MARKER, stmt.id + "：不传开票日期却渲染出了比较条件，既有查询被污染。");
        }
    }

    // ==================== INV-7：与实际回款日期共存（AND 关系） ====================

    /**
     * 开票日期与既有的「实际回款日期」是两个<b>独立</b>的区间条件，同时传入时必须是 AND 叠加。
     * 若有人把新条件写进既有条件的 {@code <if>} 里，或复用了同一个 params key，
     * 四个片段就不会同时出现。
     */
    @Test
    @DisplayName("[INV-7] 开票日期与实际回款日期同时传入 → 四个片段同时出现（AND 叠加，互不吞并）")
    void inv7_coexistsWithActualPaymentDateFilter()
    {
        for (Stmt stmt : Stmt.values())
        {
            Object param = stmt.isContract() ? new Contract() : new Payment();
            java.util.Map<String, Object> params = stmt.isContract()
                    ? ((Contract) param).getParams()
                    : ((Payment) param).getParams();
            params.put("dataScope", DATA_SCOPE_SENTINEL);
            params.put("submitAcceptanceDateStart", START);
            params.put("submitAcceptanceDateEnd", END);
            params.put("actualPaymentDateStart", START);
            params.put("actualPaymentDateEnd", END);

            String sql = norm(configuration.getMappedStatement(stmt.id).getBoundSql(param).getSql());

            boolean fmt = stmt == Stmt.PAYMENT_LIST;
            assertContains(sql, fmt ? FMT_ACTUAL_GE : BARE_ACTUAL_GE,
                    stmt.id + "：既有的实际回款日期起始条件不见了 —— 新条件吞掉了老条件。");
            assertContains(sql, fmt ? FMT_ACTUAL_LE : BARE_ACTUAL_LE,
                    stmt.id + "：既有的实际回款日期结束条件不见了 —— 新条件吞掉了老条件。");
            assertContains(sql, stmt.ge(),
                    stmt.id + "：同时传两个区间时，开票日期起始条件不见了 —— 两个条件复用了同一个 params key？");
            assertContains(sql, stmt.le(),
                    stmt.id + "：同时传两个区间时，开票日期结束条件不见了 —— 两个条件复用了同一个 params key？");
        }
    }

    // ==================== 陷阱登记（本用例本身就是证据） ====================

    /**
     * 【假绿陷阱的现场证明 / 勿删】把「为什么不能断言裸列名」变成可执行的证据，而不是只写在注释里。
     *
     * <p>本用例在<b>完全不传开票日期参数</b>的前提下断言两条列表语句的 SQL 里<b>依然含有</b>
     * {@code submit_acceptance_date} —— 因为它本来就在 SELECT 字段列表中
     * （{@code ContractMapper.xml:376} / {@code PaymentMapper.xml:50}）。
     *
     * <p>结论：{@code assertTrue(sql.contains("submit_acceptance_date"))} 这种断言对这两条语句<b>恒真</b>，
     * 把整个 {@code <if>} 删光它照样绿。任何后来者若想「简化」本类的断言，请先看这条用例。
     *
     * <p><b>{@code sumPaymentAmount} 被排除在「恒真」之外，而这让陷阱更毒</b>：实测（本用例首跑时的
     * 失败输出）它渲染成 {@code select COALESCE(sum(p.payment_amount), 0) from ... WHERE c.del_flag = '0'}
     * —— 聚合语句<b>没有字段列表</b>，因此裸列名断言在它身上是<b>真断言</b>。也就是说，三条语句放进
     * 同一个循环用裸列名断言时，会出现「两条恒绿 + 一条真红」的混合表现，极易被误读成
     * 「断言是有效的，只是那一条没改到」。这正是必须一律断言完整比较形状的理由。
     */
    @Test
    @DisplayName("[陷阱登记] 两条列表语句的裸列名 submit_acceptance_date 不传参时也恒在 —— 断言必须落在完整比较形状上")
    void trapRegistry_bareColumnNameIsAlwaysPresent()
    {
        // 只有带字段列表的两条列表语句适用「恒真」；sumPaymentAmount 是聚合语句，无字段列表（见 Javadoc）
        for (Stmt stmt : new Stmt[] { Stmt.CONTRACT_LIST, Stmt.PAYMENT_LIST })
        {
            String sql = render(stmt, null, null);
            assertContains(sql, "submit_acceptance_date",
                    stmt.id + "：列名连 SELECT 列表里都没有了？那本条「陷阱登记」的前提变了，"
                            + "请复核其余用例的断言形状是否仍然必要。");
        }

        // 同一份 SQL 里，完整比较形状必须不存在 —— 这正是「列名 contains」与「形状 contains」的差别
        for (Stmt stmt : Stmt.values())
        {
            String sql = render(stmt, null, null);
            assertNotContains(sql, BARE_GE, stmt.id + "：不传参却有比较片段（与 INV-6 重复守护，勿删）。");
            assertNotContains(sql, FMT_GE, stmt.id + "：不传参却有比较片段（与 INV-6 重复守护，勿删）。");
        }
    }
}
