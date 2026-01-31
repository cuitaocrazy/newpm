package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.CustomerContactMapper;
import com.ruoyi.project.domain.CustomerContact;
import com.ruoyi.project.service.ICustomerContactService;

/**
 * 客户联系人Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-01-30
 */
@Service
public class CustomerContactServiceImpl implements ICustomerContactService 
{
    @Autowired
    private CustomerContactMapper customerContactMapper;

    /**
     * 查询客户联系人
     * 
     * @param contactId 客户联系人主键
     * @return 客户联系人
     */
    @Override
    public CustomerContact selectCustomerContactByContactId(Long contactId)
    {
        return customerContactMapper.selectCustomerContactByContactId(contactId);
    }

    /**
     * 查询客户联系人列表
     * 
     * @param customerContact 客户联系人
     * @return 客户联系人
     */
    @Override
    public List<CustomerContact> selectCustomerContactList(CustomerContact customerContact)
    {
        return customerContactMapper.selectCustomerContactList(customerContact);
    }

    /**
     * 新增客户联系人
     * 
     * @param customerContact 客户联系人
     * @return 结果
     */
    @Override
    public int insertCustomerContact(CustomerContact customerContact)
    {
        customerContact.setCreateTime(DateUtils.getNowDate());
        return customerContactMapper.insertCustomerContact(customerContact);
    }

    /**
     * 修改客户联系人
     * 
     * @param customerContact 客户联系人
     * @return 结果
     */
    @Override
    public int updateCustomerContact(CustomerContact customerContact)
    {
        customerContact.setUpdateTime(DateUtils.getNowDate());
        return customerContactMapper.updateCustomerContact(customerContact);
    }

    /**
     * 批量删除客户联系人
     * 
     * @param contactIds 需要删除的客户联系人主键
     * @return 结果
     */
    @Override
    public int deleteCustomerContactByContactIds(Long[] contactIds)
    {
        return customerContactMapper.deleteCustomerContactByContactIds(contactIds);
    }

    /**
     * 删除客户联系人信息
     * 
     * @param contactId 客户联系人主键
     * @return 结果
     */
    @Override
    public int deleteCustomerContactByContactId(Long contactId)
    {
        return customerContactMapper.deleteCustomerContactByContactId(contactId);
    }
}
