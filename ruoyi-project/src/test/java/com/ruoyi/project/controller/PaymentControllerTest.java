package com.ruoyi.project.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.github.pagehelper.PageHelper;
import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.service.IContractService;
import com.ruoyi.project.service.IPaymentService;

/**
 * 付款里程碑 Controller 单元测试。
 *
 * <p>关注点：查询区「开票日期」区间条件（物理列 pm_payment.submit_acceptance_date，
 * 参数名 submitAcceptanceDateStart / submitAcceptanceDateEnd）是否被正确写入
 * {@code contract.getParams()}，并与既有「实际回款日期」
 * （actualPaymentDateStart / actualPaymentDateEnd）行为完全一致。</p>
 *
 * <p>纯 Mockito 单测，不连 MySQL / Redis。listWithContracts / export 走
 * {@code startPage()}（依赖 RequestContextHolder），故在 @BeforeEach 里装一个
 * MockHttpServletRequest。</p>
 */
@ExtendWith(MockitoExtension.class)
public class PaymentControllerTest
{
    private static final String K_SUBMIT_START = "submitAcceptanceDateStart";
    private static final String K_SUBMIT_END = "submitAcceptanceDateEnd";
    private static final String K_ACTUAL_START = "actualPaymentDateStart";
    private static final String K_ACTUAL_END = "actualPaymentDateEnd";

    @Mock
    private IPaymentService paymentService;

    @Mock
    private IContractService contractService;

    @InjectMocks
    private PaymentController controller;

    private MockHttpServletResponse response;

    @BeforeEach
    public void setUp()
    {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
        response = new MockHttpServletResponse();
    }

    @AfterEach
    public void tearDown()
    {
        PageHelper.clearPage();
        RequestContextHolder.resetRequestAttributes();
    }

    // ------------------------------------------------------------------
    // listWithContracts —— 列表
    // ------------------------------------------------------------------

    /** SC-002 / FR-002：开票日期起止被写入 params。 */
    @Test
    public void listWithContracts_writesSubmitAcceptanceDateRangeIntoParams()
    {
        when(contractService.selectContractWithPaymentsList(any(Contract.class)))
                .thenReturn(new ArrayList<>());

        Contract contract = new Contract();
        controller.listWithContracts(contract, null, null, "2026-03-01", "2026-03-31");

        Map<String, Object> params = capturedListParams();
        assertEquals("2026-03-01", params.get(K_SUBMIT_START));
        assertEquals("2026-03-31", params.get(K_SUBMIT_END));
    }

    /** SC-012 / FR-013：不传开票日期时，params 里不出现这两个 key（与 actualPaymentDate 的 != null 语义一致）。 */
    @Test
    public void listWithContracts_nullSubmitAcceptanceDates_keysAbsentFromParams()
    {
        when(contractService.selectContractWithPaymentsList(any(Contract.class)))
                .thenReturn(new ArrayList<>());

        controller.listWithContracts(new Contract(), null, null, null, null);

        Map<String, Object> params = capturedListParams();
        assertFalse(params.containsKey(K_SUBMIT_START), "null 起始日期不应写入 params");
        assertFalse(params.containsKey(K_SUBMIT_END), "null 结束日期不应写入 params");
    }

    /** SC-012 / FR-013：空串与既有 actualPaymentDate 一致——key 写入且值为空串（由 SQL 的 != '' 兜底）。 */
    @Test
    public void listWithContracts_emptySubmitAcceptanceDates_keysPresentAsEmptyString()
    {
        when(contractService.selectContractWithPaymentsList(any(Contract.class)))
                .thenReturn(new ArrayList<>());

        controller.listWithContracts(new Contract(), "", "", "", "");

        Map<String, Object> params = capturedListParams();
        assertEquals("", params.get(K_ACTUAL_START));
        assertEquals("", params.get(K_ACTUAL_END));
        assertTrue(params.containsKey(K_SUBMIT_START), "空串起始日期应与 actualPaymentDate 同样写入 params");
        assertTrue(params.containsKey(K_SUBMIT_END), "空串结束日期应与 actualPaymentDate 同样写入 params");
        assertEquals("", params.get(K_SUBMIT_START));
        assertEquals("", params.get(K_SUBMIT_END));
    }

    /** SC-006 / FR-008：只填开始日期。 */
    @Test
    public void listWithContracts_onlySubmitStart_writesOnlyStartKey()
    {
        when(contractService.selectContractWithPaymentsList(any(Contract.class)))
                .thenReturn(new ArrayList<>());

        controller.listWithContracts(new Contract(), null, null, "2026-03-01", null);

        Map<String, Object> params = capturedListParams();
        assertEquals("2026-03-01", params.get(K_SUBMIT_START));
        assertFalse(params.containsKey(K_SUBMIT_END));
    }

    /** SC-007 / FR-009：只填结束日期。 */
    @Test
    public void listWithContracts_onlySubmitEnd_writesOnlyEndKey()
    {
        when(contractService.selectContractWithPaymentsList(any(Contract.class)))
                .thenReturn(new ArrayList<>());

        controller.listWithContracts(new Contract(), null, null, null, "2026-03-31");

        Map<String, Object> params = capturedListParams();
        assertFalse(params.containsKey(K_SUBMIT_START));
        assertEquals("2026-03-31", params.get(K_SUBMIT_END));
    }

    /** SC-013 / FR-014 / INV-1：开票期间与回款期间同时填，四个 key 共存互不覆盖。 */
    @Test
    public void listWithContracts_bothDateRangesCoexistInParams()
    {
        when(contractService.selectContractWithPaymentsList(any(Contract.class)))
                .thenReturn(new ArrayList<>());

        controller.listWithContracts(new Contract(),
                "2026-01-01", "2026-01-31", "2026-03-01", "2026-03-31");

        Map<String, Object> params = capturedListParams();
        assertEquals("2026-01-01", params.get(K_ACTUAL_START));
        assertEquals("2026-01-31", params.get(K_ACTUAL_END));
        assertEquals("2026-03-01", params.get(K_SUBMIT_START));
        assertEquals("2026-03-31", params.get(K_SUBMIT_END));
    }

    /** 回归：既有「实际回款日期」写入行为不被破坏。 */
    @Test
    public void listWithContracts_actualPaymentDateBehaviorUnchanged()
    {
        when(contractService.selectContractWithPaymentsList(any(Contract.class)))
                .thenReturn(new ArrayList<>());

        controller.listWithContracts(new Contract(), "2026-02-01", "2026-02-28", null, null);

        Map<String, Object> params = capturedListParams();
        assertEquals("2026-02-01", params.get(K_ACTUAL_START));
        assertEquals("2026-02-28", params.get(K_ACTUAL_END));
        assertFalse(params.containsKey(K_SUBMIT_START));
        assertFalse(params.containsKey(K_SUBMIT_END));
    }

    // ------------------------------------------------------------------
    // sumPaymentAmount —— 底部合计金额（FR-005 核心：漏了就「列表筛过了、合计还是全量」）
    // ------------------------------------------------------------------

    /** SC-004 / FR-005：合计金额同样按开票期间过滤。 */
    @Test
    public void sumPaymentAmount_writesSubmitAcceptanceDateRangeIntoParams()
    {
        when(contractService.sumPaymentAmount(any(Contract.class))).thenReturn(BigDecimal.TEN);

        controller.sumPaymentAmount(new Contract(), null, null, "2026-03-01", "2026-03-31");

        Map<String, Object> params = capturedSumParams();
        assertEquals("2026-03-01", params.get(K_SUBMIT_START));
        assertEquals("2026-03-31", params.get(K_SUBMIT_END));
    }

    /** SC-012 / FR-013：合计金额——不传时 key 不出现。 */
    @Test
    public void sumPaymentAmount_nullSubmitAcceptanceDates_keysAbsentFromParams()
    {
        when(contractService.sumPaymentAmount(any(Contract.class))).thenReturn(BigDecimal.ZERO);

        controller.sumPaymentAmount(new Contract(), null, null, null, null);

        Map<String, Object> params = capturedSumParams();
        assertFalse(params.containsKey(K_SUBMIT_START));
        assertFalse(params.containsKey(K_SUBMIT_END));
    }

    /** SC-013：合计金额——两个区间条件共存。 */
    @Test
    public void sumPaymentAmount_bothDateRangesCoexistInParams()
    {
        when(contractService.sumPaymentAmount(any(Contract.class))).thenReturn(BigDecimal.ONE);

        controller.sumPaymentAmount(new Contract(),
                "2026-01-01", "2026-01-31", "2026-03-01", "2026-03-31");

        Map<String, Object> params = capturedSumParams();
        assertEquals("2026-01-01", params.get(K_ACTUAL_START));
        assertEquals("2026-01-31", params.get(K_ACTUAL_END));
        assertEquals("2026-03-01", params.get(K_SUBMIT_START));
        assertEquals("2026-03-31", params.get(K_SUBMIT_END));
    }

    /** 回归：合计金额的「实际回款日期」写入行为不被破坏。 */
    @Test
    public void sumPaymentAmount_actualPaymentDateBehaviorUnchanged()
    {
        when(contractService.sumPaymentAmount(any(Contract.class))).thenReturn(BigDecimal.ZERO);

        controller.sumPaymentAmount(new Contract(), "2026-02-01", "2026-02-28", null, null);

        Map<String, Object> params = capturedSumParams();
        assertEquals("2026-02-01", params.get(K_ACTUAL_START));
        assertEquals("2026-02-28", params.get(K_ACTUAL_END));
        assertFalse(params.containsKey(K_SUBMIT_START));
        assertFalse(params.containsKey(K_SUBMIT_END));
    }

    // ------------------------------------------------------------------
    // export —— 导出（FR-006：导出与列表同一批数据）
    // ------------------------------------------------------------------

    /** SC-005 / FR-006：导出同样带上开票期间条件。 */
    @Test
    public void export_writesSubmitAcceptanceDateRangeIntoParams()
    {
        when(contractService.selectContractWithPaymentsList(any(Contract.class)))
                .thenReturn(Collections.emptyList());

        controller.export(response, new Contract(), null, null, "2026-03-01", "2026-03-31");

        Map<String, Object> params = capturedListParams();
        assertEquals("2026-03-01", params.get(K_SUBMIT_START));
        assertEquals("2026-03-31", params.get(K_SUBMIT_END));
    }

    /** SC-012 / FR-013：导出——不传时 key 不出现。 */
    @Test
    public void export_nullSubmitAcceptanceDates_keysAbsentFromParams()
    {
        when(contractService.selectContractWithPaymentsList(any(Contract.class)))
                .thenReturn(Collections.emptyList());

        controller.export(response, new Contract(), null, null, null, null);

        Map<String, Object> params = capturedListParams();
        assertFalse(params.containsKey(K_SUBMIT_START));
        assertFalse(params.containsKey(K_SUBMIT_END));
    }

    /** SC-005：导出与列表口径一致——四个 key 共存。 */
    @Test
    public void export_bothDateRangesCoexistInParams()
    {
        when(contractService.selectContractWithPaymentsList(any(Contract.class)))
                .thenReturn(Collections.emptyList());

        controller.export(response, new Contract(),
                "2026-01-01", "2026-01-31", "2026-03-01", "2026-03-31");

        Map<String, Object> params = capturedListParams();
        assertEquals("2026-01-01", params.get(K_ACTUAL_START));
        assertEquals("2026-01-31", params.get(K_ACTUAL_END));
        assertEquals("2026-03-01", params.get(K_SUBMIT_START));
        assertEquals("2026-03-31", params.get(K_SUBMIT_END));
    }

    /** 回归：导出的「实际回款日期」写入行为不被破坏。 */
    @Test
    public void export_actualPaymentDateBehaviorUnchanged()
    {
        when(contractService.selectContractWithPaymentsList(any(Contract.class)))
                .thenReturn(Collections.emptyList());

        controller.export(response, new Contract(), "2026-02-01", "2026-02-28", null, null);

        Map<String, Object> params = capturedListParams();
        assertEquals("2026-02-01", params.get(K_ACTUAL_START));
        assertEquals("2026-02-28", params.get(K_ACTUAL_END));
        assertFalse(params.containsKey(K_SUBMIT_START));
        assertFalse(params.containsKey(K_SUBMIT_END));
    }

    // ------------------------------------------------------------------
    // helpers —— 断言的是「真正交给 Service 的那个 Contract」的 params，
    // 而不是测试自己持有的引用，避免 Controller 内部换对象时假绿。
    // ------------------------------------------------------------------

    private Map<String, Object> capturedListParams()
    {
        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractService).selectContractWithPaymentsList(captor.capture());
        return captor.getValue().getParams();
    }

    private Map<String, Object> capturedSumParams()
    {
        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractService).sumPaymentAmount(captor.capture());
        return captor.getValue().getParams();
    }
}
