package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.Customer;

/**
 * 客户管理Service接口
 * 
 * @author ruoyi
 * @date 2026-02-04
 */
public interface ICustomerService 
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
     * 检查客户简称是否唯一
     *
     * @param customerSimpleName 客户简称
     * @param customerId 客户ID（编辑时传入，新增时为null）
     * @return true=唯一，false=重复
     */
    public boolean checkCustomerSimpleNameUnique(String customerSimpleName, Long customerId);

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
     * 批量删除客户管理
     * 
     * @param customerIds 需要删除的客户管理主键集合
     * @return 结果
     */
    public int deleteCustomerByCustomerIds(Long[] customerIds);

    /**
     * 删除客户管理信息
     *
     * @param customerId 客户管理主键
     * @return 结果
     */
    public int deleteCustomerByCustomerId(Long customerId);

    /**
     * 查询所有客户列表（用于下拉选择）
     *
     * @return 客户列表
     */
    public List<Customer> selectCustomerListAll();
}
