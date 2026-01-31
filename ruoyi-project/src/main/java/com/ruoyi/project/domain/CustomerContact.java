package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 客户联系人对象 pm_customer_contact
 * 
 * @author ruoyi
 * @date 2026-01-30
 */
public class CustomerContact extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 联系人主键ID */
    @Excel(name = "联系人主键ID")
    private Long contactId;

    /** 客户ID */
    @Excel(name = "客户ID")
    private Long customerId;

    /** 联系人姓名 */
    @Excel(name = "联系人姓名")
    private String contactName;

    /** 联系人电话 */
    @Excel(name = "联系人电话")
    private String contactPhone;

    /** 联系人标签(字典表，字典类型contact_tag) */
    @Excel(name = "联系人标签(字典表，字典类型contact_tag)")
    private String contactTag;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    public void setContactId(Long contactId) 
    {
        this.contactId = contactId;
    }

    public Long getContactId() 
    {
        return contactId;
    }

    public void setCustomerId(Long customerId) 
    {
        this.customerId = customerId;
    }

    public Long getCustomerId() 
    {
        return customerId;
    }

    public void setContactName(String contactName) 
    {
        this.contactName = contactName;
    }

    public String getContactName() 
    {
        return contactName;
    }

    public void setContactPhone(String contactPhone) 
    {
        this.contactPhone = contactPhone;
    }

    public String getContactPhone() 
    {
        return contactPhone;
    }

    public void setContactTag(String contactTag) 
    {
        this.contactTag = contactTag;
    }

    public String getContactTag() 
    {
        return contactTag;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("contactId", getContactId())
            .append("customerId", getCustomerId())
            .append("contactName", getContactName())
            .append("contactPhone", getContactPhone())
            .append("contactTag", getContactTag())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
