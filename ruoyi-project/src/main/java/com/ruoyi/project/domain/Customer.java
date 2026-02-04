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
 * @date 2026-02-04
 */
public class Customer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 客户主键ID */
    private Long customerId;

    /** 客户简称 */
    @Excel(name = "客户简称", sort = 1)
    private String customerSimpleName;

    /** 客户全称 */
    @Excel(name = "客户全称", sort = 2)
    private String customerAllName;

    /** 所属行业 */
    private String industry;

    /** 所属区域 */
    private String region;

    /** 销售负责人ID */
    private Long salesManagerId;

    /** 办公地址 */
    @Excel(name = "办公地址", sort = 6)
    private String officeAddress;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    /** 所属行业名称（用于导出） */
    @Excel(name = "所属行业", sort = 3)
    private String industryName;

    /** 所属区域名称（用于导出） */
    @Excel(name = "所属区域", sort = 4)
    private String regionName;

    /** 销售负责人名称（用于导出） */
    @Excel(name = "销售负责人", sort = 5)
    private String salesManagerName;

    /** 客户联系人信息 */
    private List<CustomerContact> customerContactList;

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

    public List<CustomerContact> getCustomerContactList()
    {
        return customerContactList;
    }

    public void setCustomerContactList(List<CustomerContact> customerContactList)
    {
        this.customerContactList = customerContactList;
    }

    public String getIndustryName()
    {
        return industryName;
    }

    public void setIndustryName(String industryName)
    {
        this.industryName = industryName;
    }

    public String getRegionName()
    {
        return regionName;
    }

    public void setRegionName(String regionName)
    {
        this.regionName = regionName;
    }

    public String getSalesManagerName()
    {
        return salesManagerName;
    }

    public void setSalesManagerName(String salesManagerName)
    {
        this.salesManagerName = salesManagerName;
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
            .append("officeAddress", getOfficeAddress())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .append("customerContactList", getCustomerContactList())
            .toString();
    }
}
