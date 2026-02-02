package com.ruoyi.project.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 合同管理对象 pm_contract
 *
 * @author ruoyi
 * @date 2026-02-01
 */
public class Contract extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 合同主键ID */
    private Long contractId;

    /** 合同编号 */
    @Excel(name = "合同编号")
    private String contractCode;

    /** 合同名称 */
    @Excel(name = "合同名称")
    private String contractName;

    /** 关联客户ID */
    @Excel(name = "关联客户ID")
    private Long customerId;

    /** 部门ID */
    @Excel(name = "部门ID")
    private Long deptId;

    /** 合同类型 */
    @Excel(name = "合同类型")
    private String contractType;

    /** 合同状态 */
    @Excel(name = "合同状态")
    private String contractStatus;

    /** 合同签订日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "合同签订日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date contractSignDate;

    /** 合同周期(月) */
    @Excel(name = "合同周期(月)")
    private Integer contractPeriod;

    /** 合同金额(含税) */
    @Excel(name = "合同金额(含税)")
    private BigDecimal contractAmount;

    /** 税率(%) */
    @Excel(name = "税率(%)")
    private BigDecimal taxRate;

    /** 不含税金额 */
    @Excel(name = "不含税金额")
    private BigDecimal amountNoTax;

    /** 税金 */
    @Excel(name = "税金")
    private BigDecimal taxAmount;

    /** 合同确认金额 */
    @Excel(name = "合同确认金额")
    private BigDecimal confirmAmount;

    /** 确认年份 */
    @Excel(name = "确认年份")
    private String confirmYear;

    /** 免维期(月) */
    @Excel(name = "免维期(月)")
    private Integer freeMaintenancePeriod;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    /** 备用域1 */
    @Excel(name = "备用域1")
    private String reservedField1;

    /** 备用域2 */
    @Excel(name = "备用域2")
    private String reservedField2;

    /** 备用域3 */
    @Excel(name = "备用域3")
    private String reservedField3;

    /** 备用域4 */
    @Excel(name = "备用域4")
    private String reservedField4;

    /** 备用域5 */
    @Excel(name = "备用域5")
    private String reservedField5;

    public void setContractId(Long contractId) 
    {
        this.contractId = contractId;
    }

    public Long getContractId() 
    {
        return contractId;
    }

    public void setContractCode(String contractCode) 
    {
        this.contractCode = contractCode;
    }

    public String getContractCode() 
    {
        return contractCode;
    }

    public void setContractName(String contractName) 
    {
        this.contractName = contractName;
    }

    public String getContractName() 
    {
        return contractName;
    }

    public void setCustomerId(Long customerId) 
    {
        this.customerId = customerId;
    }

    public Long getCustomerId() 
    {
        return customerId;
    }

    public void setDeptId(Long deptId) 
    {
        this.deptId = deptId;
    }

    public Long getDeptId() 
    {
        return deptId;
    }

    public void setContractType(String contractType) 
    {
        this.contractType = contractType;
    }

    public String getContractType() 
    {
        return contractType;
    }

    public void setContractStatus(String contractStatus) 
    {
        this.contractStatus = contractStatus;
    }

    public String getContractStatus() 
    {
        return contractStatus;
    }

    public void setContractSignDate(Date contractSignDate) 
    {
        this.contractSignDate = contractSignDate;
    }

    public Date getContractSignDate() 
    {
        return contractSignDate;
    }

    public void setContractPeriod(Integer contractPeriod) 
    {
        this.contractPeriod = contractPeriod;
    }

    public Integer getContractPeriod() 
    {
        return contractPeriod;
    }

    public void setContractAmount(BigDecimal contractAmount) 
    {
        this.contractAmount = contractAmount;
    }

    public BigDecimal getContractAmount() 
    {
        return contractAmount;
    }

    public void setTaxRate(BigDecimal taxRate) 
    {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxRate() 
    {
        return taxRate;
    }

    public void setAmountNoTax(BigDecimal amountNoTax) 
    {
        this.amountNoTax = amountNoTax;
    }

    public BigDecimal getAmountNoTax() 
    {
        return amountNoTax;
    }

    public void setTaxAmount(BigDecimal taxAmount) 
    {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTaxAmount() 
    {
        return taxAmount;
    }

    public void setConfirmAmount(BigDecimal confirmAmount) 
    {
        this.confirmAmount = confirmAmount;
    }

    public BigDecimal getConfirmAmount() 
    {
        return confirmAmount;
    }

    public void setConfirmYear(String confirmYear) 
    {
        this.confirmYear = confirmYear;
    }

    public String getConfirmYear() 
    {
        return confirmYear;
    }

    public void setFreeMaintenancePeriod(Integer freeMaintenancePeriod) 
    {
        this.freeMaintenancePeriod = freeMaintenancePeriod;
    }

    public Integer getFreeMaintenancePeriod() 
    {
        return freeMaintenancePeriod;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    public void setReservedField1(String reservedField1) 
    {
        this.reservedField1 = reservedField1;
    }

    public String getReservedField1() 
    {
        return reservedField1;
    }

    public void setReservedField2(String reservedField2) 
    {
        this.reservedField2 = reservedField2;
    }

    public String getReservedField2() 
    {
        return reservedField2;
    }

    public void setReservedField3(String reservedField3) 
    {
        this.reservedField3 = reservedField3;
    }

    public String getReservedField3() 
    {
        return reservedField3;
    }

    public void setReservedField4(String reservedField4) 
    {
        this.reservedField4 = reservedField4;
    }

    public String getReservedField4() 
    {
        return reservedField4;
    }

    public void setReservedField5(String reservedField5) 
    {
        this.reservedField5 = reservedField5;
    }

    public String getReservedField5() 
    {
        return reservedField5;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("contractId", getContractId())
            .append("contractCode", getContractCode())
            .append("contractName", getContractName())
            .append("customerId", getCustomerId())
            .append("deptId", getDeptId())
            .append("contractType", getContractType())
            .append("contractStatus", getContractStatus())
            .append("contractSignDate", getContractSignDate())
            .append("contractPeriod", getContractPeriod())
            .append("contractAmount", getContractAmount())
            .append("taxRate", getTaxRate())
            .append("amountNoTax", getAmountNoTax())
            .append("taxAmount", getTaxAmount())
            .append("confirmAmount", getConfirmAmount())
            .append("confirmYear", getConfirmYear())
            .append("freeMaintenancePeriod", getFreeMaintenancePeriod())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .append("reservedField1", getReservedField1())
            .append("reservedField2", getReservedField2())
            .append("reservedField3", getReservedField3())
            .append("reservedField4", getReservedField4())
            .append("reservedField5", getReservedField5())
            .append("projectIds", getProjectIds())
            .toString();
    }

    /** 关联的项目ID列表（非数据库字段） */
    private List<Long> projectIds;

    public List<Long> getProjectIds()
    {
        return projectIds;
    }

    public void setProjectIds(List<Long> projectIds)
    {
        this.projectIds = projectIds;
    }
}
