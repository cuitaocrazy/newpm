package com.ruoyi.project.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 投产批次管理对象 pm_production_batch
 * 
 * @author ruoyi
 * @date 2026-03-11
 */
public class ProductionBatch extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 批次ID */
    private Long batchId;

    /** 批次号 */
    @Excel(name = "批次号")
    private String batchNo;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sortOrder;

    /** 投产年份 */
    @Excel(name = "投产年份")
    private String productionYear;

    /** 计划投产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "计划投产日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date planProductionDate;

    public void setBatchId(Long batchId) 
    {
        this.batchId = batchId;
    }

    public Long getBatchId() 
    {
        return batchId;
    }

    public void setBatchNo(String batchNo) 
    {
        this.batchNo = batchNo;
    }

    public String getBatchNo() 
    {
        return batchNo;
    }

    public void setSortOrder(Integer sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder() 
    {
        return sortOrder;
    }

    public void setProductionYear(String productionYear) 
    {
        this.productionYear = productionYear;
    }

    public String getProductionYear() 
    {
        return productionYear;
    }

    public void setPlanProductionDate(Date planProductionDate) 
    {
        this.planProductionDate = planProductionDate;
    }

    public Date getPlanProductionDate() 
    {
        return planProductionDate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("batchId", getBatchId())
            .append("batchNo", getBatchNo())
            .append("sortOrder", getSortOrder())
            .append("productionYear", getProductionYear())
            .append("planProductionDate", getPlanProductionDate())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
