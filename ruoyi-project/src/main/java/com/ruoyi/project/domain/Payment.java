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
    @Excel(name = "合同ID")
    private Long contractId;

    /** 付款方式名称 */
    @Excel(name = "付款方式名称")
    private String paymentMethodName;

    /** 付款总金额 */
    @Excel(name = "付款总金额")
    private BigDecimal paymentAmount;

    /** 是否涉及违约扣款(1是 0否) */
    @Excel(name = "是否涉及违约扣款(1是 0否)")
    private String hasPenalty;

    /** 扣款金额(元) */
    @Excel(name = "扣款金额(元)")
    private BigDecimal penaltyAmount;

    /** 付款状态 */
    @Excel(name = "付款状态")
    private String paymentStatus;

    /** 预计回款所属季度 */
    @Excel(name = "预计回款所属季度")
    private String expectedQuarter;

    /** 实际回款所属季度 */
    @Excel(name = "实际回款所属季度")
    private String actualQuarter;

    /** 提交验收材料日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "提交验收材料日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date submitAcceptanceDate;

    /** 实际回款日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "实际回款日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date actualPaymentDate;

    /** 款项确认年份 */
    @Excel(name = "款项确认年份")
    private String confirmYear;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

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
