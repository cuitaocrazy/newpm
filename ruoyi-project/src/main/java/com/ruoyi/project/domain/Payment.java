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
    @Excel(name = "付款里程碑名称", sort = 14)
    private String paymentMethodName;

    /** 付款总金额 */
    @Excel(name = "付款金额（元）", sort = 15)
    private BigDecimal paymentAmount;

    /** 付款状态 */
    @Excel(name = "付款状态", sort = 16, dictType = "sys_fkzt")
    private String paymentStatus;

    /** 是否涉及违约扣款(1是 0否) */
    @Excel(name = "是否涉及违约扣款", sort = 17, readConverterExp = "0=否,1=是")
    private String hasPenalty;

    /** 扣款金额(元) */
    @Excel(name = "扣款金额（元）", sort = 18)
    private BigDecimal penaltyAmount;

    /** 预计回款所属季度 */
    @Excel(name = "预计回款季度", sort = 19, dictType = "sys_jdgl")
    private String expectedQuarter;

    /** 实际回款所属季度 */
    @Excel(name = "实际回款季度", sort = 20, dictType = "sys_jdgl")
    private String actualQuarter;

    /** 实际回款日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "实际回款日期", sort = 21, width = 30, dateFormat = "yyyy-MM-dd")
    private Date actualPaymentDate;

    /** 款项确认年份 */
    @Excel(name = "里程碑确认年份", sort = 22, dictType = "sys_ndgl")
    private String confirmYear;

    /** 提交验收材料日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开票日期", sort = 23, width = 30, dateFormat = "yyyy-MM-dd")
    private Date submitAcceptanceDate;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    /** 合同名称（关联查询） */
    @Excel(name = "合同名称", sort = 1)
    private String contractName;

    /** 合同编号（关联查询） */
    @Excel(name = "合同编号", sort = 2)
    private String contractCode;

    /** 合同状态（关联查询） */
    @Excel(name = "合同状态", sort = 3, dictType = "sys_htzt")
    private String contractStatus;

    /** 客户名称（关联查询） */
    @Excel(name = "客户名称", sort = 4)
    private String customerName;

    /** 合同金额（关联查询） */
    @Excel(name = "合同金额（元）", sort = 5)
    private BigDecimal contractAmount;

    /** 合同签订日期（关联查询） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "合同签订日期", sort = 6, width = 30, dateFormat = "yyyy-MM-dd")
    private Date contractSignDate;

    /** 免维期（关联查询） */
    @Excel(name = "免维期（月）", sort = 7)
    private Integer freeMaintenancePeriod;

    /** 部门ID（关联查询） */
    private Long deptId;

    /** 部门名称（关联查询） */
    @Excel(name = "合同所属团队", sort = 8)
    private String deptName;

    /** 合同所属团队 二级机构（导出用，非数据库字段） */
    @Excel(name = "二级机构", sort = 9)
    private String deptOrgLevel2;

    /** 合同所属团队 三级机构（导出用，非数据库字段） */
    @Excel(name = "三级机构", sort = 10)
    private String deptOrgLevel3;

    /** 合同所属团队 四级机构（导出用，非数据库字段） */
    @Excel(name = "四级机构", sort = 11)
    private String deptOrgLevel4;

    /** 合同所属团队 五级机构（导出用，非数据库字段） */
    @Excel(name = "五级机构", sort = 12)
    private String deptOrgLevel5;

    /** 合同所属团队 六级机构（导出用，非数据库字段） */
    @Excel(name = "六级机构", sort = 13)
    private String deptOrgLevel6;

    /** 创建人姓名（关联查询） */
    private String createByName;

    /** 更新人姓名（关联查询） */
    @Excel(name = "更新人", sort = 26)
    private String updateByName;

    /** 多选过滤字段（不存库） */
    private List<Long> deptIds;
    private List<String> paymentStatuses;
    private List<String> expectedQuarters;
    private List<String> actualQuarters;

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

    public void setDeptOrgLevel2(String deptOrgLevel2) { this.deptOrgLevel2 = deptOrgLevel2; }
    public String getDeptOrgLevel2() { return deptOrgLevel2; }

    public void setDeptOrgLevel3(String deptOrgLevel3) { this.deptOrgLevel3 = deptOrgLevel3; }
    public String getDeptOrgLevel3() { return deptOrgLevel3; }

    public void setDeptOrgLevel4(String deptOrgLevel4) { this.deptOrgLevel4 = deptOrgLevel4; }
    public String getDeptOrgLevel4() { return deptOrgLevel4; }

    public void setDeptOrgLevel5(String deptOrgLevel5) { this.deptOrgLevel5 = deptOrgLevel5; }
    public String getDeptOrgLevel5() { return deptOrgLevel5; }

    public void setDeptOrgLevel6(String deptOrgLevel6) { this.deptOrgLevel6 = deptOrgLevel6; }
    public String getDeptOrgLevel6() { return deptOrgLevel6; }

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

    public List<Long> getDeptIds() { return deptIds; }
    public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds; }

    public List<String> getPaymentStatuses() { return paymentStatuses; }
    public void setPaymentStatuses(List<String> paymentStatuses) { this.paymentStatuses = paymentStatuses; }

    public List<String> getExpectedQuarters() { return expectedQuarters; }
    public void setExpectedQuarters(List<String> expectedQuarters) { this.expectedQuarters = expectedQuarters; }

    public List<String> getActualQuarters() { return actualQuarters; }
    public void setActualQuarters(List<String> actualQuarters) { this.actualQuarters = actualQuarters; }

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
