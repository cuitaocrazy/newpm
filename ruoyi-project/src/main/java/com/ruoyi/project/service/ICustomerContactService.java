package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.CustomerContact;

/**
 * 客户联系人Service接口
 * 
 * @author ruoyi
 * @date 2026-01-30
 */
public interface ICustomerContactService 
{
    /**
     * 查询客户联系人
     * 
     * @param contactId 客户联系人主键
     * @return 客户联系人
     */
    public CustomerContact selectCustomerContactByContactId(Long contactId);

    /**
     * 查询客户联系人列表
     * 
     * @param customerContact 客户联系人
     * @return 客户联系人集合
     */
    public List<CustomerContact> selectCustomerContactList(CustomerContact customerContact);

    /**
     * 新增客户联系人
     * 
     * @param customerContact 客户联系人
     * @return 结果
     */
    public int insertCustomerContact(CustomerContact customerContact);

    /**
     * 修改客户联系人
     * 
     * @param customerContact 客户联系人
     * @return 结果
     */
    public int updateCustomerContact(CustomerContact customerContact);

    /**
     * 批量删除客户联系人
     * 
     * @param contactIds 需要删除的客户联系人主键集合
     * @return 结果
     */
    public int deleteCustomerContactByContactIds(Long[] contactIds);

    /**
     * 删除客户联系人信息
     * 
     * @param contactId 客户联系人主键
     * @return 结果
     */
    public int deleteCustomerContactByContactId(Long contactId);
}
