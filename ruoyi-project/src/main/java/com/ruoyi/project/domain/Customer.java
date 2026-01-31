package com.ruoyi.project.domain;

import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 客户管理对象 pm_customer
 *
 * @author ruoyi
 * @date 2026-01-30
 */
public class Customer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 客户主键ID */
    @Excel(name = "客户主键ID")
    private Long customerId;

    /** 客户简称 */
    @Excel(name = "客户简称")
    private String customerSimpleName;

    /** 客户全称 */
    @Excel(name = "客户全称")
    private String customerAllName;

    /** 所属行业(字典表 字典类型industry) */
    @Excel(name = "所属行业(字典表 字典类型industry)")
    private String industry;

    /** 所属区域(字典表 : 字典类型sys_yjqy) */
    @Excel(name = "所属区域(字典表 : 字典类型sys_yjqy)")
    private String region;

    /** 销售负责人ID */
    private Long salesManagerId;

    /** 销售负责人姓名 */
    @Excel(name = "销售负责人姓名")
    private String salesManagerName;

    /** 办公地址 */
    private String officeAddress;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    /** 客户联系人列表 */
    private List<CustomerContact> contactList;

    public void setCustomerId(Long customerId) 
    {
        this.customerId = customerId;
    }

    public Long getCustomerId() 
    {
        return customerId;
    }

    public void setCustomerSimpleName(String customerSimpleName) 
    {
        this.customerSimpleName = customerSimpleName;
    }

    public String getCustomerSimpleName() 
    {
        return customerSimpleName;
    }

    public void setCustomerAllName(String customerAllName) 
    {
        this.customerAllName = customerAllName;
    }

    public String getCustomerAllName() 
    {
        return customerAllName;
    }

    public void setIndustry(String industry) 
    {
        this.industry = industry;
    }

    public String getIndustry() 
    {
        return industry;
    }

    public void setRegion(String region) 
    {
        this.region = region;
    }

    public String getRegion() 
    {
        return region;
    }

    public void setSalesManagerId(Long salesManagerId) 
    {
        this.salesManagerId = salesManagerId;
    }

    public Long getSalesManagerId() 
    {
        return salesManagerId;
    }

    public void setSalesManagerName(String salesManagerName) 
    {
        this.salesManagerName = salesManagerName;
    }

    public String getSalesManagerName() 
    {
        return salesManagerName;
    }

    public void setOfficeAddress(String officeAddress) 
    {
        this.officeAddress = officeAddress;
    }

    public String getOfficeAddress() 
    {
        return officeAddress;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setContactList(List<CustomerContact> contactList)
    {
        this.contactList = contactList;
    }

    public List<CustomerContact> getContactList()
    {
        return contactList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("customerId", getCustomerId())
            .append("customerSimpleName", getCustomerSimpleName())
            .append("customerAllName", getCustomerAllName())
            .append("industry", getIndustry())
            .append("region", getRegion())
            .append("salesManagerId", getSalesManagerId())
            .append("salesManagerName", getSalesManagerName())
            .append("officeAddress", getOfficeAddress())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
