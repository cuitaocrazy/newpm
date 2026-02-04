package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 省级区域对象 pm_secondary_region
 * 
 * @author ruoyi
 * @date 2026-02-04
 */
public class SecondaryRegion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 省份ID */
    private Long provinceId;

    /** 省份代码（行政区划代码前2位） */
    @Excel(name = "省份代码", readConverterExp = "行=政区划代码前2位")
    private String provinceCode;

    /** 省份名称 */
    @Excel(name = "省份名称")
    private String provinceName;

    /** 省份类型（0=省/1=直辖市/2=自治区/3=特别行政区/4=计划单列市） */
    @Excel(name = "省份类型", readConverterExp = "0==省/1=直辖市/2=自治区/3=特别行政区/4=计划单列市")
    private String provinceType;

    /** 一级区域 */
    @Excel(name = "一级区域")
    private String regionDictValue;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0存在 1删除） */
    private String delFlag;

    public void setProvinceId(Long provinceId) 
    {
        this.provinceId = provinceId;
    }

    public Long getProvinceId() 
    {
        return provinceId;
    }

    public void setProvinceCode(String provinceCode) 
    {
        this.provinceCode = provinceCode;
    }

    public String getProvinceCode() 
    {
        return provinceCode;
    }

    public void setProvinceName(String provinceName) 
    {
        this.provinceName = provinceName;
    }

    public String getProvinceName() 
    {
        return provinceName;
    }

    public void setProvinceType(String provinceType) 
    {
        this.provinceType = provinceType;
    }

    public String getProvinceType() 
    {
        return provinceType;
    }

    public void setRegionDictValue(String regionDictValue) 
    {
        this.regionDictValue = regionDictValue;
    }

    public String getRegionDictValue() 
    {
        return regionDictValue;
    }

    public void setSortOrder(Integer sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder() 
    {
        return sortOrder;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
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
            .append("provinceId", getProvinceId())
            .append("provinceCode", getProvinceCode())
            .append("provinceName", getProvinceName())
            .append("provinceType", getProvinceType())
            .append("regionDictValue", getRegionDictValue())
            .append("sortOrder", getSortOrder())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
