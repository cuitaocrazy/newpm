package com.ruoyi.project.service.impl;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.Customer;
import com.ruoyi.project.domain.CustomerContact;
import com.ruoyi.project.mapper.CustomerMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CustomerServiceImpl 行为锁定测试
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @InjectMocks
    private CustomerServiceImpl service;

    @Mock private CustomerMapper customerMapper;

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

    // ========== checkCustomerSimpleNameUnique 行为锁定 ==========

    @Test
    @DisplayName("客户简称唯一性：无同名 → 唯一")
    void checkUnique_noMatch() {
        when(customerMapper.selectCustomerList(any())).thenReturn(Collections.emptyList());
        assertTrue(service.checkCustomerSimpleNameUnique("新客户", null));
    }

    @Test
    @DisplayName("客户简称唯一性：新增模式有同名 → 不唯一")
    void checkUnique_duplicateOnInsert() {
        Customer existing = new Customer();
        existing.setCustomerId(1L);
        when(customerMapper.selectCustomerList(any())).thenReturn(List.of(existing));

        assertFalse(service.checkCustomerSimpleNameUnique("已有客户", null));
    }

    @Test
    @DisplayName("客户简称唯一性：编辑模式排除自身 → 唯一")
    void checkUnique_editModeSelf() {
        Customer existing = new Customer();
        existing.setCustomerId(1L);
        when(customerMapper.selectCustomerList(any())).thenReturn(List.of(existing));

        assertTrue(service.checkCustomerSimpleNameUnique("自己", 1L));
    }

    @Test
    @DisplayName("客户简称唯一性：编辑模式有其他同名 → 不唯一")
    void checkUnique_editModeOther() {
        Customer existing = new Customer();
        existing.setCustomerId(2L);
        when(customerMapper.selectCustomerList(any())).thenReturn(List.of(existing));

        assertFalse(service.checkCustomerSimpleNameUnique("别人的", 1L));
    }

    // ========== insertCustomer 行为锁定 ==========

    @Test
    @DisplayName("新增客户：设置审计字段 + 级联插入联系人")
    void insertCustomer_withContacts() {
        when(customerMapper.selectCustomerList(any())).thenReturn(Collections.emptyList());
        when(customerMapper.insertCustomer(any())).thenReturn(1);

        Customer customer = new Customer();
        customer.setCustomerId(10L);
        customer.setCustomerSimpleName("测试客户");
        CustomerContact c1 = new CustomerContact();
        CustomerContact c2 = new CustomerContact();
        customer.setCustomerContactList(List.of(c1, c2));

        int result = service.insertCustomer(customer);

        assertEquals(1, result);
        assertEquals("testuser", customer.getCreateBy());
        assertNotNull(customer.getCreateTime());
        // 联系人应绑定 customerId
        verify(customerMapper).batchCustomerContact(argThat(list ->
            list.size() == 2 && list.get(0).getCustomerId().equals(10L)));
    }

    @Test
    @DisplayName("新增客户：简称重复时抛RuntimeException")
    void insertCustomer_duplicateName() {
        Customer existing = new Customer();
        existing.setCustomerId(1L);
        when(customerMapper.selectCustomerList(any())).thenReturn(List.of(existing));

        Customer customer = new Customer();
        customer.setCustomerSimpleName("已有客户");

        // 注意：当前代码抛 RuntimeException 而非 ServiceException（已知问题 H11）
        assertThrows(RuntimeException.class, () -> service.insertCustomer(customer));
        verify(customerMapper, never()).insertCustomer(any());
    }

    @Test
    @DisplayName("新增客户：无联系人时不调用batchInsert")
    void insertCustomer_noContacts() {
        when(customerMapper.selectCustomerList(any())).thenReturn(Collections.emptyList());
        when(customerMapper.insertCustomer(any())).thenReturn(1);

        Customer customer = new Customer();
        customer.setCustomerSimpleName("测试客户");
        // 不设置 customerContactList

        service.insertCustomer(customer);

        verify(customerMapper, never()).batchCustomerContact(any());
    }

    // ========== updateCustomer 行为锁定 ==========

    @Test
    @DisplayName("修改客户：先删旧联系人再插新联系人")
    void updateCustomer_replacesContacts() {
        when(customerMapper.selectCustomerList(any())).thenReturn(Collections.emptyList());
        when(customerMapper.updateCustomer(any())).thenReturn(1);

        Customer customer = new Customer();
        customer.setCustomerId(10L);
        customer.setCustomerSimpleName("更新客户");
        CustomerContact c1 = new CustomerContact();
        customer.setCustomerContactList(List.of(c1));

        service.updateCustomer(customer);

        InOrder inOrder = inOrder(customerMapper);
        inOrder.verify(customerMapper).deleteCustomerContactByCustomerId(10L);
        inOrder.verify(customerMapper).batchCustomerContact(any());
        inOrder.verify(customerMapper).updateCustomer(any());
    }

    // ========== deleteCustomer 行为锁定 ==========

    @Test
    @DisplayName("批量删除客户：先删联系人再删客户")
    void deleteCustomerByIds_cascadeDelete() {
        Long[] ids = {1L, 2L};
        when(customerMapper.deleteCustomerByCustomerIds(ids)).thenReturn(2);

        int result = service.deleteCustomerByCustomerIds(ids);

        assertEquals(2, result);
        InOrder inOrder = inOrder(customerMapper);
        inOrder.verify(customerMapper).deleteCustomerContactByCustomerIds(ids);
        inOrder.verify(customerMapper).deleteCustomerByCustomerIds(ids);
    }

    @Test
    @DisplayName("单个删除客户：先删联系人再删客户")
    void deleteCustomerById_cascadeDelete() {
        when(customerMapper.deleteCustomerByCustomerId(1L)).thenReturn(1);

        service.deleteCustomerByCustomerId(1L);

        InOrder inOrder = inOrder(customerMapper);
        inOrder.verify(customerMapper).deleteCustomerContactByCustomerId(1L);
        inOrder.verify(customerMapper).deleteCustomerByCustomerId(1L);
    }
}
