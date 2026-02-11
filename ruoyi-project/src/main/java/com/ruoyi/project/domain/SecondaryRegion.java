package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 二级区域对象 pm_secondary_region
 *
 * @author ruoyi
 * @date 2026-02-04
 */
public class SecondaryRegion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 二级区域ID */
    private Long regionId;

    /** 二级区域代码（行政区划代码前2位） */
    @Excel(name = "二级区域代码", readConverterExp = "行=政区划代码前2位")
    private String regionCode;

    /** 二级区域名称 */
    @Excel(name = "二级区域名称")
    private String regionName;

    /** 二级区域类型（0=省/1=直辖市/2=自治区/3=特别行政区/4=计划单列市） */
    @Excel(name = "二级区域类型", readConverterExp = "0==省/1=直辖市/2=自治区/3=特别行政区/4=计划单列市")
    private String regionType;

    /** 一级区域字典值 */
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

    public void setRegionId(Long regionId)
    {
        this.regionId = regionId;
    }

    public Long getRegionId()
    {
        return regionId;
    }

    public void setRegionCode(String regionCode)
    {
        this.regionCode = regionCode;
    }

    public String getRegionCode()
    {
        return regionCode;
    }

    public void setRegionName(String regionName)
    {
        this.regionName = regionName;
    }

    public String getRegionName()
    {
        return regionName;
    }

    public void setRegionType(String regionType)
    {
        this.regionType = regionType;
    }

    public String getRegionType()
    {
        return regionType;
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
            .append("regionId", getRegionId())
            .append("regionCode", getRegionCode())
            .append("regionName", getRegionName())
            .append("regionType", getRegionType())
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
