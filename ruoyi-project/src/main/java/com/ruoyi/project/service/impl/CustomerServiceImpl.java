package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.domain.CustomerContact;
import com.ruoyi.project.mapper.CustomerMapper;
import com.ruoyi.project.domain.Customer;
import com.ruoyi.project.service.ICustomerService;

/**
 * 客户管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-02-04
 */
@Service
public class CustomerServiceImpl implements ICustomerService 
{
    @Autowired
    private CustomerMapper customerMapper;

    /**
     * 查询客户管理
     * 
     * @param customerId 客户管理主键
     * @return 客户管理
     */
    @Override
    public Customer selectCustomerByCustomerId(Long customerId)
    {
        return customerMapper.selectCustomerByCustomerId(customerId);
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
     * 检查客户简称是否唯一
     *
     * @param customerSimpleName 客户简称
     * @param customerId 客户ID（编辑时传入，新增时为null）
     * @return true=唯一，false=重复
     */
    @Override
    public boolean checkCustomerSimpleNameUnique(String customerSimpleName, Long customerId)
    {
        Customer customer = new Customer();
        customer.setCustomerSimpleName(customerSimpleName);
        List<Customer> list = customerMapper.selectCustomerList(customer);

        if (list == null || list.isEmpty())
        {
            return true;
        }

        // 新增时，查到数据即为重复
        if (customerId == null)
        {
            return false;
        }

        // 编辑时，排除自己后判断是否重复
        for (Customer c : list)
        {
            if (!c.getCustomerId().equals(customerId))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * 新增客户管理
     *
     * @param customer 客户管理
     * @return 结果
     */
    @Transactional
    @Override
    public int insertCustomer(Customer customer)
    {
        // 验证客户简称唯一性
        if (!checkCustomerSimpleNameUnique(customer.getCustomerSimpleName(), null))
        {
            throw new RuntimeException("客户简称已存在");
        }

        customer.setCreateTime(DateUtils.getNowDate());
        int rows = customerMapper.insertCustomer(customer);
        insertCustomerContact(customer);
        return rows;
    }

    /**
     * 修改客户管理
     * 
     * @param customer 客户管理
     * @return 结果
     */
    @Transactional
    @Override
    public int updateCustomer(Customer customer)
    {
        // 验证客户简称唯一性
        if (!checkCustomerSimpleNameUnique(customer.getCustomerSimpleName(), customer.getCustomerId()))
        {
            throw new RuntimeException("客户简称已存在");
        }

        customer.setUpdateTime(DateUtils.getNowDate());
        customerMapper.deleteCustomerContactByCustomerId(customer.getCustomerId());
        insertCustomerContact(customer);
        return customerMapper.updateCustomer(customer);
    }

    /**
     * 批量删除客户管理
     * 
     * @param customerIds 需要删除的客户管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteCustomerByCustomerIds(Long[] customerIds)
    {
        customerMapper.deleteCustomerContactByCustomerIds(customerIds);
        return customerMapper.deleteCustomerByCustomerIds(customerIds);
    }

    /**
     * 删除客户管理信息
     * 
     * @param customerId 客户管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteCustomerByCustomerId(Long customerId)
    {
        customerMapper.deleteCustomerContactByCustomerId(customerId);
        return customerMapper.deleteCustomerByCustomerId(customerId);
    }

    /**
     * 新增客户联系人信息
     * 
     * @param customer 客户管理对象
     */
    public void insertCustomerContact(Customer customer)
    {
        List<CustomerContact> customerContactList = customer.getCustomerContactList();
        Long customerId = customer.getCustomerId();
        if (StringUtils.isNotNull(customerContactList))
        {
            List<CustomerContact> list = new ArrayList<CustomerContact>();
            for (CustomerContact customerContact : customerContactList)
            {
                customerContact.setCustomerId(customerId);
                list.add(customerContact);
            }
            if (list.size() > 0)
            {
                customerMapper.batchCustomerContact(list);
            }
        }
    }
}
