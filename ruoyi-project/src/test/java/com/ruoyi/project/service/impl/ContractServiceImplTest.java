package com.ruoyi.project.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.domain.ProjectContractRel;
import com.ruoyi.project.mapper.ContractMapper;
import com.ruoyi.project.mapper.ProjectContractRelMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.mapper.PaymentMapper;
import com.ruoyi.project.mapper.AttachmentMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ContractServiceImpl 行为锁定测试（Characterization Test）
 * 目的：锁定现有正确行为，后续重构时防止回归
 */
@ExtendWith(MockitoExtension.class)
class ContractServiceImplTest {

    @InjectMocks
    private ContractServiceImpl service;

    @Mock private ContractMapper contractMapper;
    @Mock private ProjectContractRelMapper projectContractRelMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private PaymentMapper paymentMapper;
    @Mock private AttachmentMapper attachmentMapper;
    @Mock private SysUserMapper userMapper;

    private MockedStatic<SecurityUtils> securityMock;

    @BeforeEach
    void setUp() {
        securityMock = mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::getUsername).thenReturn("testuser");
    }

    @AfterEach
    void tearDown() {
        securityMock.close();
    }

    // ========== 税额计算行为锁定 ==========

    @ParameterizedTest(name = "合同金额={0}, 税率={1}% → 不含税={2}, 税金={3}")
    @DisplayName("税额计算：常见税率")
    @CsvSource({
        "100.00, 6,   94.34,  5.66",
        "100.00, 13,  88.50,  11.50",
        "100.00, 9,   91.74,  8.26",
        "100.00, 3,   97.09,  2.91",
        "100.00, 0,   100.00, 0.00",
    })
    void insertContract_taxCalculation(String amount, String rate,
                                        String expectedNoTax, String expectedTax) {
        Contract contract = buildContractForInsert(amount, rate);
        when(contractMapper.insertContract(any())).thenReturn(1);

        service.insertContract(contract);

        assertEquals(new BigDecimal(expectedNoTax), contract.getAmountNoTax(),
            "不含税金额");
        assertEquals(new BigDecimal(expectedTax), contract.getTaxAmount(),
            "税金");
    }

    @Test
    @DisplayName("税额计算：金额或税率为null时不设置税额字段")
    void insertContract_nullAmountSkipsTaxCalc() {
        Contract contract = buildContractForInsert(null, "6");
        when(contractMapper.insertContract(any())).thenReturn(1);

        service.insertContract(contract);

        assertNull(contract.getAmountNoTax());
        assertNull(contract.getTaxAmount());
    }

    @Test
    @DisplayName("税额计算：大金额精度 - 不含税+税金=合同金额")
    void insertContract_largeAmountPrecision() {
        Contract contract = buildContractForInsert("9999999.99", "6");
        when(contractMapper.insertContract(any())).thenReturn(1);

        service.insertContract(contract);

        BigDecimal sum = contract.getAmountNoTax().add(contract.getTaxAmount());
        assertEquals(0, new BigDecimal("9999999.99").compareTo(sum),
            "不含税 + 税金 应等于合同金额");
    }

    @Test
    @DisplayName("updateContract 也执行税额计算")
    void updateContract_alsoCalculatesTax() {
        Contract contract = new Contract();
        contract.setContractId(1L);
        contract.setContractAmount(new BigDecimal("100.00"));
        contract.setTaxRate(new BigDecimal("6"));
        when(contractMapper.updateContract(any())).thenReturn(1);

        service.updateContract(contract);

        assertEquals(new BigDecimal("94.34"), contract.getAmountNoTax());
        assertEquals(new BigDecimal("5.66"), contract.getTaxAmount());
    }

    // ========== 删除保护行为锁定 ==========

    @Test
    @DisplayName("删除合同：有付款记录时阻止删除")
    void deleteContract_blockedByPayments() {
        Long contractId = 1L;
        Contract contract = new Contract();
        contract.setContractId(contractId);
        contract.setContractName("测试合同");
        when(contractMapper.selectContractByContractId(contractId)).thenReturn(contract);
        when(paymentMapper.countPaymentByContractId(contractId)).thenReturn(2);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.deleteContractByContractIds(new Long[]{contractId}));

        assertTrue(ex.getMessage().contains("款项里程碑"));
        verify(contractMapper, never()).updateContract(any());
    }

    @Test
    @DisplayName("删除合同：有附件时阻止删除")
    void deleteContract_blockedByAttachments() {
        Long contractId = 1L;
        Contract contract = new Contract();
        contract.setContractId(contractId);
        contract.setContractName("测试合同");
        when(contractMapper.selectContractByContractId(contractId)).thenReturn(contract);
        when(paymentMapper.countPaymentByContractId(contractId)).thenReturn(0);
        when(attachmentMapper.countAttachmentByBusiness("contract", contractId)).thenReturn(1);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.deleteContractByContractIds(new Long[]{contractId}));

        assertTrue(ex.getMessage().contains("附件"));
        verify(contractMapper, never()).updateContract(any());
    }

    @Test
    @DisplayName("删除合同：无关联数据时成功软删除")
    void deleteContract_success() {
        Long contractId = 1L;
        Contract contract = new Contract();
        contract.setContractId(contractId);
        contract.setContractName("测试合同");
        when(contractMapper.selectContractByContractId(contractId)).thenReturn(contract);
        when(paymentMapper.countPaymentByContractId(contractId)).thenReturn(0);
        when(attachmentMapper.countAttachmentByBusiness("contract", contractId)).thenReturn(0);
        when(contractMapper.updateContract(any())).thenReturn(1);

        int result = service.deleteContractByContractIds(new Long[]{contractId});

        assertEquals(1, result);
        assertEquals("1", contract.getDelFlag(), "应设置为软删除");
        verify(projectContractRelMapper).deleteProjectContractRelByContractId(contractId);
    }

    @Test
    @DisplayName("删除合同：合同不存在时抛异常")
    void deleteContract_notFound() {
        when(contractMapper.selectContractByContractId(99L)).thenReturn(null);

        assertThrows(ServiceException.class,
            () -> service.deleteContractByContractIds(new Long[]{99L}));
    }

    // ========== 唯一性校验行为锁定 ==========

    @Test
    @DisplayName("合同名称唯一性：无同名 → 唯一")
    void checkContractNameUnique_noMatch() {
        when(contractMapper.selectContractList(any())).thenReturn(Collections.emptyList());

        assertTrue(service.checkContractNameUnique("新合同", null));
    }

    @Test
    @DisplayName("合同名称唯一性：新增模式有同名 → 不唯一")
    void checkContractNameUnique_duplicateOnInsert() {
        Contract existing = new Contract();
        existing.setContractId(1L);
        when(contractMapper.selectContractList(any())).thenReturn(List.of(existing));

        assertFalse(service.checkContractNameUnique("已有合同", null));
    }

    @Test
    @DisplayName("合同名称唯一性：编辑模式排除自身 → 唯一")
    void checkContractNameUnique_editModeSelf() {
        Contract existing = new Contract();
        existing.setContractId(1L);
        when(contractMapper.selectContractList(any())).thenReturn(List.of(existing));

        assertTrue(service.checkContractNameUnique("自己的合同", 1L));
    }

    @Test
    @DisplayName("合同名称唯一性：编辑模式有其他同名 → 不唯一")
    void checkContractNameUnique_editModeOther() {
        Contract existing = new Contract();
        existing.setContractId(2L);
        when(contractMapper.selectContractList(any())).thenReturn(List.of(existing));

        assertFalse(service.checkContractNameUnique("别人的合同", 1L));
    }

    @Test
    @DisplayName("合同名称唯一性：null或空字符串 → 不唯一")
    void checkContractNameUnique_nullOrEmpty() {
        assertFalse(service.checkContractNameUnique(null, null));
        assertFalse(service.checkContractNameUnique("", null));
        assertFalse(service.checkContractNameUnique("  ", null));
    }

    @Test
    @DisplayName("合同编号唯一性：逻辑与名称一致")
    void checkContractCodeUnique_sameLogicAsName() {
        when(contractMapper.selectContractList(any())).thenReturn(Collections.emptyList());
        assertTrue(service.checkContractCodeUnique("CODE-001", null));

        Contract existing = new Contract();
        existing.setContractId(1L);
        when(contractMapper.selectContractList(any())).thenReturn(List.of(existing));
        assertFalse(service.checkContractCodeUnique("CODE-001", null));
        assertTrue(service.checkContractCodeUnique("CODE-001", 1L));
    }

    // ========== 新增合同：项目关联行为锁定 ==========

    @Test
    @DisplayName("新增合同：项目已有合同时阻止")
    void insertContract_blockedByExistingProjectContract() {
        Contract contract = buildContractForInsert("100", "6");
        contract.setProjectIds(List.of(10L));
        when(contractMapper.selectContractIdByProjectId(10L)).thenReturn(5L);

        assertThrows(ServiceException.class, () -> service.insertContract(contract));
        verify(contractMapper, never()).insertContract(any());
    }

    @Test
    @DisplayName("新增合同：正确创建项目关联关系")
    void insertContract_createsProjectRel() {
        Contract contract = buildContractForInsert("100", "6");
        contract.setProjectIds(List.of(10L, 20L));
        when(contractMapper.selectContractIdByProjectId(anyLong())).thenReturn(null);
        when(contractMapper.insertContract(any())).thenReturn(1);

        service.insertContract(contract);

        verify(projectContractRelMapper, times(2)).insertProjectContractRel(any());
        assertEquals("testuser", contract.getCreateBy());
        assertEquals("0", contract.getDelFlag());
    }

    // ========== 辅助方法 ==========

    private Contract buildContractForInsert(String amount, String rate) {
        Contract contract = new Contract();
        if (amount != null) {
            contract.setContractAmount(new BigDecimal(amount));
        }
        if (rate != null) {
            contract.setTaxRate(new BigDecimal(rate));
        }
        return contract;
    }
}
