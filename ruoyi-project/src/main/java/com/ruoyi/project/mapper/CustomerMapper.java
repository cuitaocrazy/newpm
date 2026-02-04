package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.Customer;
import com.ruoyi.project.domain.CustomerContact;

/**
 * 客户管理Mapper接口
 * 
 * @author ruoyi
 * @date 2026-02-04
 */
public interface CustomerMapper 
{
    /**
     * 查询客户管理
     * 
     * @param customerId 客户管理主键
     * @return 客户管理
     */
    public Customer selectCustomerByCustomerId(Long customerId);

    /**
     * 查询客户管理列表
     * 
     * @param customer 客户管理
     * @return 客户管理集合
     */
    public List<Customer> selectCustomerList(Customer customer);

    /**
     * 新增客户管理
     * 
     * @param customer 客户管理
     * @return 结果
     */
    public int insertCustomer(Customer customer);

    /**
     * 修改客户管理
     * 
     * @param customer 客户管理
     * @return 结果
     */
    public int updateCustomer(Customer customer);

    /**
     * 删除客户管理
     * 
     * @param customerId 客户管理主键
     * @return 结果
     */
    public int deleteCustomerByCustomerId(Long customerId);

    /**
     * 批量删除客户管理
     * 
     * @param customerIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCustomerByCustomerIds(Long[] customerIds);

    /**
     * 批量删除客户联系人
     * 
     * @param customerIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCustomerContactByCustomerIds(Long[] customerIds);
    
    /**
     * 批量新增客户联系人
     * 
     * @param customerContactList 客户联系人列表
     * @return 结果
     */
    public int batchCustomerContact(List<CustomerContact> customerContactList);
    

    /**
     * 通过客户管理主键删除客户联系人信息
     * 
     * @param customerId 客户管理ID
     * @return 结果
     */
    public int deleteCustomerContactByCustomerId(Long customerId);
}
