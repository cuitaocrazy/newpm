package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.CustomerMapper;
import com.ruoyi.project.mapper.CustomerContactMapper;
import com.ruoyi.project.domain.Customer;
import com.ruoyi.project.domain.CustomerContact;
import com.ruoyi.project.service.ICustomerService;
import com.ruoyi.common.utils.StringUtils;

/**
 * 客户管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-01-30
 */
@Service
public class CustomerServiceImpl implements ICustomerService
{
    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerContactMapper customerContactMapper;

    /**
     * 查询客户管理
     *
     * @param customerId 客户管理主键
     * @return 客户管理
     */
    @Override
    public Customer selectCustomerByCustomerId(Long customerId)
    {
        Customer customer = customerMapper.selectCustomerByCustomerId(customerId);
        if (customer != null) {
            // 查询客户的联系人列表
            CustomerContact query = new CustomerContact();
            query.setCustomerId(customerId);
            List<CustomerContact> contactList = customerContactMapper.selectCustomerContactList(query);
            customer.setContactList(contactList);
        }
        return customer;
    }

    /**
     * 查询客户管理列表
     * 
     * @param customer 客户管理
     * @return 客户管理
     */
    @Override
    public List<Customer> selectCustomerList(Customer customer)
    {
        return customerMapper.selectCustomerList(customer);
    }

    /**
     * 新增客户管理
     *
     * @param customer 客户管理
     * @return 结果
     */
    @Override
    @Transactional
    public int insertCustomer(Customer customer)
    {
        customer.setCreateTime(DateUtils.getNowDate());
        int rows = customerMapper.insertCustomer(customer);
        // 保存联系人列表
        insertCustomerContact(customer);
        return rows;
    }

    /**
     * 新增客户联系人信息
     *
     * @param customer 客户对象
     */
    public void insertCustomerContact(Customer customer)
    {
        List<CustomerContact> contactList = customer.getContactList();
        Long customerId = customer.getCustomerId();
        if (StringUtils.isNotNull(contactList))
        {
            for (CustomerContact contact : contactList)
            {
                contact.setCustomerId(customerId);
                customerContactMapper.insertCustomerContact(contact);
            }
        }
    }

    /**
     * 修改客户管理
     *
     * @param customer 客户管理
     * @return 结果
     */
    @Override
    @Transactional
    public int updateCustomer(Customer customer)
    {
        customer.setUpdateTime(DateUtils.getNowDate());
        // 删除原有联系人
        customerContactMapper.deleteCustomerContactByCustomerId(customer.getCustomerId());
        // 新增联系人列表
        insertCustomerContact(customer);
        return customerMapper.updateCustomer(customer);
    }

    /**
     * 批量删除客户管理
     * 
     * @param customerIds 需要删除的客户管理主键
     * @return 结果
     */
    @Override
    public int deleteCustomerByCustomerIds(Long[] customerIds)
    {
        return customerMapper.deleteCustomerByCustomerIds(customerIds);
    }

    /**
     * 删除客户管理信息
     * 
     * @param customerId 客户管理主键
     * @return 结果
     */
    @Override
    public int deleteCustomerByCustomerId(Long customerId)
    {
        return customerMapper.deleteCustomerByCustomerId(customerId);
    }
}
