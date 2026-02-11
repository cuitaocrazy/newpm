package com.ruoyi.project.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 款项管理对象 pm_payment
 * 
 * @author ruoyi
 * @date 2026-02-04
 */
public class Payment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 款项主键ID */
    private Long paymentId;

    /** 合同ID */
    private Long contractId;

    /** 付款方式名称 */
    @Excel(name = "付款里程碑名称", sort = 2)
    private String paymentMethodName;

    /** 付款总金额 */
    @Excel(name = "付款金额（元）", sort = 4)
    private BigDecimal paymentAmount;

    /** 是否涉及违约扣款(1是 0否) */
    @Excel(name = "是否涉及违约扣款", sort = 5, readConverterExp = "0=否,1=是")
    private String hasPenalty;

    /** 扣款金额(元) */
    @Excel(name = "扣款金额（元）", sort = 6)
    private BigDecimal penaltyAmount;

    /** 付款状态 */
    @Excel(name = "付款状态", sort = 3, dictType = "sys_fkzt")
    private String paymentStatus;

    /** 预计回款所属季度 */
    @Excel(name = "预计回款季度", sort = 7, dictType = "sys_jdgl")
    private String expectedQuarter;

    /** 实际回款所属季度 */
    @Excel(name = "实际回款季度", sort = 8, dictType = "sys_jdgl")
    private String actualQuarter;

    /** 提交验收材料日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "提交验收材料日期", sort = 11, width = 30, dateFormat = "yyyy-MM-dd")
    private Date submitAcceptanceDate;

    /** 实际回款日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "实际回款日期", sort = 9, width = 30, dateFormat = "yyyy-MM-dd")
    private Date actualPaymentDate;

    /** 款项确认年份 */
    @Excel(name = "里程碑确认年份", sort = 10, dictType = "sys_ndgl")
    private String confirmYear;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    /** 合同名称（关联查询） */
    @Excel(name = "合同名称", sort = 1)
    private String contractName;

    /** 合同编号（关联查询） */
    private String contractCode;

    /** 合同状态（关联查询） */
    private String contractStatus;

    /** 客户名称（关联查询） */
    private String customerName;

    /** 合同金额（关联查询） */
    private BigDecimal contractAmount;

    /** 合同签订日期（关联查询） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date contractSignDate;

    /** 免维期（关联查询） */
    private Integer freeMaintenancePeriod;

    /** 部门ID（关联查询） */
    private Long deptId;

    /** 部门名称（关联查询） */
    private String deptName;

    /** 创建人姓名（关联查询） */
    private String createByName;

    /** 更新人姓名（关联查询） */
    private String updateByName;

    public void setPaymentId(Long paymentId) 
    {
        this.paymentId = paymentId;
    }

    public Long getPaymentId() 
    {
        return paymentId;
    }

    public void setContractId(Long contractId) 
    {
        this.contractId = contractId;
    }

    public Long getContractId() 
    {
        return contractId;
    }

    public void setPaymentMethodName(String paymentMethodName) 
    {
        this.paymentMethodName = paymentMethodName;
    }

    public String getPaymentMethodName() 
    {
        return paymentMethodName;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) 
    {
        this.paymentAmount = paymentAmount;
    }

    public BigDecimal getPaymentAmount() 
    {
        return paymentAmount;
    }

    public void setHasPenalty(String hasPenalty) 
    {
        this.hasPenalty = hasPenalty;
    }

    public String getHasPenalty() 
    {
        return hasPenalty;
    }

    public void setPenaltyAmount(BigDecimal penaltyAmount) 
    {
        this.penaltyAmount = penaltyAmount;
    }

    public BigDecimal getPenaltyAmount() 
    {
        return penaltyAmount;
    }

    public void setPaymentStatus(String paymentStatus) 
    {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentStatus() 
    {
        return paymentStatus;
    }

    public void setExpectedQuarter(String expectedQuarter) 
    {
        this.expectedQuarter = expectedQuarter;
    }

    public String getExpectedQuarter() 
    {
        return expectedQuarter;
    }

    public void setActualQuarter(String actualQuarter) 
    {
        this.actualQuarter = actualQuarter;
    }

    public String getActualQuarter() 
    {
        return actualQuarter;
    }

    public void setSubmitAcceptanceDate(Date submitAcceptanceDate) 
    {
        this.submitAcceptanceDate = submitAcceptanceDate;
    }

    public Date getSubmitAcceptanceDate() 
    {
        return submitAcceptanceDate;
    }

    public void setActualPaymentDate(Date actualPaymentDate) 
    {
        this.actualPaymentDate = actualPaymentDate;
    }

    public Date getActualPaymentDate() 
    {
        return actualPaymentDate;
    }

    public void setConfirmYear(String confirmYear) 
    {
        this.confirmYear = confirmYear;
    }

    public String getConfirmYear() 
    {
        return confirmYear;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setContractName(String contractName)
    {
        this.contractName = contractName;
    }

    public String getContractName()
    {
        return contractName;
    }

    public void setContractCode(String contractCode)
    {
        this.contractCode = contractCode;
    }

    public String getContractCode()
    {
        return contractCode;
    }

    public void setContractStatus(String contractStatus)
    {
        this.contractStatus = contractStatus;
    }

    public String getContractStatus()
    {
        return contractStatus;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public void setContractAmount(BigDecimal contractAmount)
    {
        this.contractAmount = contractAmount;
    }

    public BigDecimal getContractAmount()
    {
        return contractAmount;
    }

    public void setContractSignDate(Date contractSignDate)
    {
        this.contractSignDate = contractSignDate;
    }

    public Date getContractSignDate()
    {
        return contractSignDate;
    }

    public void setFreeMaintenancePeriod(Integer freeMaintenancePeriod)
    {
        this.freeMaintenancePeriod = freeMaintenancePeriod;
    }

    public Integer getFreeMaintenancePeriod()
    {
        return freeMaintenancePeriod;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setCreateByName(String createByName)
    {
        this.createByName = createByName;
    }

    public String getCreateByName()
    {
        return createByName;
    }

    public void setUpdateByName(String updateByName)
    {
        this.updateByName = updateByName;
    }

    public String getUpdateByName()
    {
        return updateByName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("paymentId", getPaymentId())
            .append("contractId", getContractId())
            .append("paymentMethodName", getPaymentMethodName())
            .append("paymentAmount", getPaymentAmount())
            .append("hasPenalty", getHasPenalty())
            .append("penaltyAmount", getPenaltyAmount())
            .append("paymentStatus", getPaymentStatus())
            .append("expectedQuarter", getExpectedQuarter())
            .append("actualQuarter", getActualQuarter())
            .append("submitAcceptanceDate", getSubmitAcceptanceDate())
            .append("actualPaymentDate", getActualPaymentDate())
            .append("confirmYear", getConfirmYear())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
