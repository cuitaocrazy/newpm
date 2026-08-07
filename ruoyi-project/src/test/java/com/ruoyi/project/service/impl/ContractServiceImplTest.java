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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

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

    // ================================================================================
    // 合同编号判重（Issue #32 / specs/020-contract-code-unique）
    //
    // 以下用例对应 bdd/coverage.md 的场景映射（方法上的 @DisplayName 内嵌场景编号）。
    // 它们按 TDD 先红后绿产出：实现落地前 47 run / 28 failures，落地后 47 / 0。
    //
    // 原用例 checkContractCodeUnique_sameLogicAsName 已删除：它锁的是「编号逻辑与名称一致」
    // 的旧行为——空编号判定为「不唯一」(ContractServiceImpl:368-370) + 走 selectContractList
    // 的 like 模糊匹配 (ContractMapper.xml:9)。这两条正是本次要改掉的缺陷，
    // 且它 mock 的 selectContractList 在新实现下不再被调用，STRICT_STUBS 也会让它必红。
    // 这是「不改测试就必红」，不是实现有 bug。
    //
    // 新实现约定：判重走 contractMapper.countByContractCode(归一化编号, 排除的合同ID)，
    // 精确匹配 + 仅在用记录 + 不带数据权限；排除自身由 SQL 承担，不在 Java 侧比对
    // （库表是 utf8mb4_0900_ai_ci，大小写不敏感，Java 侧再比一次会与索引产生裂缝）。
    // ================================================================================

    /** 场景背景：已被"甲方框架协议"（部门"项目一组"）占用的在用编号 */
    private static final String OCCUPIED_CODE = "4004-2024-1210-01-00018";

    @ParameterizedTest(name = "输入=[{0}] 视为未填写 → 唯一")
    @DisplayName("场景 2.3 / T1：空、空格、TAB、CR/LF、字面「无」都等同于没填 → 唯一且不查库")
    @NullSource
    @ValueSource(strings = {"", "   ", "\t", " \t ", "\r\n", "无"})
    void checkContractCodeUnique_blankVariants_returnTrue(String typed) {
        assertTrue(service.checkContractCodeUnique(typed, null),
            "未填写的编号不参与判重，必须判为唯一（存量一百多条空编号合同全靠这条不被卡死）");
        verifyNoInteractions(contractMapper);
    }

    @Test
    @DisplayName("场景 1.1a：编号精确重复 → 不唯一")
    void checkContractCodeUnique_exactDuplicate_returnsFalse() {
        when(contractMapper.countByContractCode(OCCUPIED_CODE, null)).thenReturn(1);

        assertFalse(service.checkContractCodeUnique(OCCUPIED_CODE, null),
            "编号已被在用合同占用，必须判为不唯一");
    }

    @ParameterizedTest(name = "键入=[{0}] 归一化后应命中同一个编号")
    @DisplayName("场景 3.1 / T3：只差首尾空白或 TAB，仍算同一个编号")
    @ValueSource(strings = {
        "  4004-2024-1210-01-00018",
        "4004-2024-1210-01-00018  ",
        "4004-2024-1210-01-00018\t",
        "\t4004-2024-1210-01-00018"
    })
    void checkContractCodeUnique_whitespaceVariantsHitSameCode_returnsFalse(String typed) {
        when(contractMapper.countByContractCode(OCCUPIED_CODE, null)).thenReturn(1);

        assertFalse(service.checkContractCodeUnique(typed, null),
            "肉眼一样的编号必须判为重复（生产 contract_id=263 的编号末尾就带一个 TAB）");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(contractMapper).countByContractCode(captor.capture(), isNull());
        assertEquals(OCCUPIED_CODE, captor.getValue(),
            "传给判重 SQL 的必须是归一化后的编号，不能把原始空白带进去");
    }

    @Test
    @DisplayName("场景 3.2：编号中间的空格不被抹掉，「ABC 001」与「ABC001」是两个编号")
    void checkContractCodeUnique_keepsInnerSpace() {
        when(contractMapper.countByContractCode("ABC 001", null)).thenReturn(1);

        assertFalse(service.checkContractCodeUnique(" ABC 001 ", null),
            "去掉首尾空白后就是已占用的「ABC 001」");
        assertTrue(service.checkContractCodeUnique("ABC001", null),
            "归一化只去首尾空白与 TAB/CR/LF，中间空格不得被抹掉，否则会把两个真编号误判成一个");

        verify(contractMapper).countByContractCode("ABC001", null);
    }

    @Test
    @DisplayName("场景 4.1 / T2：ABC 是 ABC-001 的前缀，不算重复（必须精确匹配，不能走 like）")
    void checkContractCodeUnique_prefixOfExistingCode_returnsTrue() {
        assertTrue(service.checkContractCodeUnique("ABC", null),
            "旧实现走 selectContractList 的 like '%ABC%'，会命中 ABC-001 而误报「已存在」");

        verify(contractMapper).countByContractCode("ABC", null);
        verify(contractMapper, never()).selectContractList(any());
    }

    @Test
    @DisplayName("场景 4.2 / T4：编辑模式把自身ID交给SQL排除 → 唯一")
    void checkContractCodeUnique_selfOnly_returnsTrue() {
        assertTrue(service.checkContractCodeUnique(OCCUPIED_CODE, 7L),
            "编号只被自己占着，编辑时不能被自己拦住");

        // 自排除必须交给 SQL（库表 utf8mb4_0900_ai_ci 大小写不敏感，
        // Java 侧再比一次会与唯一索引产生裂缝）
        verify(contractMapper).countByContractCode(OCCUPIED_CODE, 7L);
    }

    @Test
    @DisplayName("场景 1.1b：编辑模式下编号被别人占用 → 不唯一")
    void checkContractCodeUnique_otherContractHoldsCode_returnsFalse() {
        when(contractMapper.countByContractCode(OCCUPIED_CODE, 7L)).thenReturn(1);

        assertFalse(service.checkContractCodeUnique(OCCUPIED_CODE, 7L),
            "排除自身之后仍有别的在用合同占着这个编号");
    }

    @Test
    @DisplayName("场景 6.1：判重直调 mapper 绕过 @DataScope，别的部门录过的编号也能检出")
    void checkContractCodeUnique_callsMapperDirectly_notDataScopedService() {
        when(contractMapper.countByContractCode(OCCUPIED_CODE, null)).thenReturn(1);

        assertFalse(service.checkContractCodeUnique(OCCUPIED_CODE, null));

        verify(contractMapper).countByContractCode(OCCUPIED_CODE, null);
        verify(contractMapper, never()).selectContractList(any());
    }

    @Test
    @DisplayName("场景 1.1a / T5：新增遇重复编号 → 抛 ServiceException 且不落库")
    void insertContract_duplicateCode_throwsAndSkipsInsert() {
        Contract contract = buildContractForInsert("100", "6");
        contract.setContractCode(OCCUPIED_CODE);
        when(contractMapper.countByContractCode(OCCUPIED_CODE, null)).thenReturn(1);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.insertContract(contract));

        assertTrue(ex.getMessage().contains("合同编号已存在"),
            "场景 1.5：提示要是能读懂的中文，实际为：" + ex.getMessage());
        assertTrue(ex.getMessage().contains(OCCUPIED_CODE),
            "场景 1.5：提示里要带出撞车的编号本身，实际为：" + ex.getMessage());
        assertFalse(ex.getMessage().contains("Exception")
                || ex.getMessage().toLowerCase().contains("select"),
            "场景 1.5：提示中不得出现异常堆栈或 SQL，实际为：" + ex.getMessage());
        verify(contractMapper, never()).insertContract(any());
    }

    @ParameterizedTest(name = "编号输入=[{0}] → 不查库、以 null 落库")
    @DisplayName("场景 2.1/2.3 / T8：等同于没填的输入都能正常新增，且短路不查库")
    @ValueSource(strings = {"", "   ", "\t", " \t ", "无"})
    void insertContract_blankVariants_skipDuplicateQueryAndPersistNull(String typed) {
        Contract contract = buildContractForInsert("100", "6");
        contract.setContractCode(typed);
        when(contractMapper.insertContract(any())).thenReturn(1);

        int rows = service.insertContract(contract);

        assertEquals(1, rows, "不填编号的框架协议必须能正常保存");
        verify(contractMapper, never()).countByContractCode(any(), any());
        assertNull(contract.getContractCode(),
            "归一化后为空的编号必须以 NULL 落库，否则空串会与唯一索引/判重口径打架");
    }

    @Test
    @DisplayName("场景 3.3 / T7：落库前先归一化，库里不再产生带 TAB 的脏编号")
    void insertContract_normalizesCodeBeforePersist() {
        Contract contract = buildContractForInsert("100", "6");
        contract.setContractCode("\t" + OCCUPIED_CODE + " ");
        when(contractMapper.insertContract(any())).thenReturn(1);

        service.insertContract(contract);

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).insertContract(captor.capture());
        assertEquals(OCCUPIED_CODE, captor.getValue().getContractCode(),
            "落库实体上的编号必须是干净值（全局 trim 去不掉中间/多种空白，必须由这里统一归一化）");
    }

    @Test
    @DisplayName("场景 1.1b/1.3 / T6：编辑时把编号改成别人的 → 抛异常且不更新")
    void updateContract_duplicateCode_throwsAndSkipsUpdate() {
        Contract contract = new Contract();
        contract.setContractId(7L);
        contract.setContractCode(OCCUPIED_CODE);
        when(contractMapper.countByContractCode(OCCUPIED_CODE, 7L)).thenReturn(1);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.updateContract(contract));

        assertTrue(ex.getMessage().contains("合同编号已存在"),
            "实际提示为：" + ex.getMessage());
        verify(contractMapper, never()).updateContract(any());
    }

    @Test
    @DisplayName("场景 4.2：编辑时编号与自身原值相同 → 正常更新，不被自己拦住")
    void updateContract_ownCodeUnchanged_succeeds() {
        Contract contract = new Contract();
        contract.setContractId(7L);
        contract.setContractCode(OCCUPIED_CODE);
        when(contractMapper.updateContract(any())).thenReturn(1);

        int rows = service.updateContract(contract);

        assertEquals(1, rows, "只改金额、编号原样不动，必须能保存成功");
        verify(contractMapper).countByContractCode(OCCUPIED_CODE, 7L);
        verify(contractMapper).updateContract(any());
    }

    @Test
    @DisplayName("FR-13 / D5：编号判重排在「项目已关联合同」校验之后，不抢它的提示")
    void updateContract_checksProjectRelationBeforeCodeDuplicate() {
        Contract contract = new Contract();
        contract.setContractId(7L);
        contract.setContractCode(OCCUPIED_CODE);
        contract.setProjectIds(List.of(10L));
        // 项目关联的就是自己 → 项目校验通过，流程继续走到编号判重
        when(contractMapper.selectContractIdByProjectId(10L)).thenReturn(7L);
        when(contractMapper.countByContractCode(OCCUPIED_CODE, 7L)).thenReturn(1);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.updateContract(contract));
        assertTrue(ex.getMessage().contains("合同编号已存在"), "实际提示为：" + ex.getMessage());

        InOrder inOrder = inOrder(contractMapper);
        inOrder.verify(contractMapper).selectContractIdByProjectId(10L);
        inOrder.verify(contractMapper).countByContractCode(OCCUPIED_CODE, 7L);
        verify(contractMapper, never()).updateContract(any());
    }

    @Test
    @DisplayName("场景 6.4 / D7：唯一索引报 1062 时转译成同一句人话，而不是系统错误")
    void insertContract_duplicateKeyException_translatedToServiceException() {
        Contract contract = buildContractForInsert("100", "6");
        contract.setContractCode(OCCUPIED_CODE);
        when(contractMapper.insertContract(any())).thenThrow(new DuplicateKeyException(
            "Duplicate entry '" + OCCUPIED_CODE + "' for key 'uk_contract_code_norm'"));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.insertContract(contract),
            "并发撞车时数据库兜底抛 DuplicateKeyException，必须转译为业务异常");

        assertTrue(ex.getMessage().contains("合同编号已存在"),
            "并发与手工重复要给同一句提示，实际为：" + ex.getMessage());
    }

    @Test
    @DisplayName("并发兜底：非合同编号的唯一键冲突，既不误报为编号重复，也不泄露数据库细节")
    void insertContract_otherUniqueKeyViolation_neitherMisreportedNorLeaked() {
        Contract contract = buildContractForInsert("100", "6");
        contract.setContractCode(OCCUPIED_CODE);
        // 判重放行（countByContractCode 未 stub，Mockito 默认返回 0），但落库时撞上了另一个唯一约束。
        // 异常消息照抄 MyBatis 真实格式：它会把 SQL、表名、服务器上的 jar 路径全拼进去。
        when(contractMapper.insertContract(any())).thenThrow(new DuplicateKeyException(
            "### Error updating database. Cause: java.sql.SQLIntegrityConstraintViolationException: "
                + "Duplicate entry 'foo' for key 'uk_some_other_constraint' "
                + "### The error may exist in URL [jar:nested:/app/ruoyi-admin.jar!/BOOT-INF/lib/x.jar!/m.xml] "
                + "### SQL: insert into pm_contract ( contract_code, contract_name, customer_id ) values (?,?,?)"));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.insertContract(contract),
            "非编号冲突也必须转成业务异常：GlobalExceptionHandler:123-128 对 RuntimeException 的处理是 "
                + "AjaxResult.error(e.getMessage())，原样抛出会把表名/列名/完整 SQL/服务器 jar 路径回吐到浏览器");

        String msg = ex.getMessage();
        assertFalse(msg.contains("合同编号已存在"), "不是编号冲突，不得谎报成编号重复，实际为：" + msg);
        assertFalse(msg.contains("uk_some_other_constraint"), "不得泄露索引名，实际为：" + msg);
        assertFalse(msg.contains("pm_contract"), "不得泄露表名，实际为：" + msg);
        assertFalse(msg.contains("insert into"), "不得泄露 SQL 语句，实际为：" + msg);
        assertFalse(msg.contains(".jar"), "不得泄露服务器文件路径，实际为：" + msg);
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
