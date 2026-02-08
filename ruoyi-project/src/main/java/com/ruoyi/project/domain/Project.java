package com.ruoyi.project.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目管理对象 pm_project
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
public class Project extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    private Long projectId;

    /** 项目编号 */
    @Excel(name = "项目编号")
    private String projectCode;

    /** 项目名称 */
    @Excel(name = "项目名称")
    private String projectName;

    /** 项目全称 */
    private String projectFullName;

    /** 行业 */
    @Excel(name = "行业")
    private String industry;

    /** 区域 */
    @Excel(name = "区域")
    private String region;

    /** 简称 */
    private String shortName;

    /** 立项年份 */
    @Excel(name = "立项年份")
    private Integer establishedYear;

    /** 项目分类 */
    @Excel(name = "项目分类")
    private String projectCategory;

    /** 项目部门 */
    @Excel(name = "项目部门")
    private String projectDept;

    /** 项目状态 */
    @Excel(name = "项目状态")
    private String projectStatus;

    /** 验收状态 */
    @Excel(name = "验收状态")
    private String acceptanceStatus;

    /** 预估工作量(人天) */
    @Excel(name = "预估工作量(人天)")
    private BigDecimal estimatedWorkload;

    /** 实际工作量(人天) */
    @Excel(name = "实际工作量(人天)")
    private BigDecimal actualWorkload;

    /** 项目地址 */
    private String projectAddress;

    /** 项目计划 */
    private String projectPlan;

    /** 项目描述 */
    private String projectDescription;

    /** 项目经理ID */
    @Excel(name = "项目经理ID")
    private Long projectManagerId;

    /** 市场经理ID */
    @Excel(name = "市场经理ID")
    private Long marketManagerId;

    /** 参与人员ID列表 */
    @Excel(name = "参与人员ID列表")
    private String participants;

    /** 销售负责人ID */
    @Excel(name = "销售负责人ID")
    private Long salesManagerId;

    /** 销售联系方式 */
    private String salesContact;

    /** 团队负责人ID */
    @Excel(name = "团队负责人ID")
    private Long teamLeaderId;

    /** 客户ID */
    @Excel(name = "客户ID")
    private Long customerId;

    /** 客户联系人ID */
    private Long customerContactId;

    /** 商户联系人 */
    private String merchantContact;

    /** 商户联系方式 */
    private String merchantPhone;

    /** 启动日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "启动日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 投产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "投产日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date productionDate;

    /** 验收日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "验收日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date acceptanceDate;

    /** 项目预算(元) */
    @Excel(name = "项目预算(元)")
    private BigDecimal projectBudget;

    /** 项目费用(元) */
    @Excel(name = "项目费用(元)")
    private BigDecimal projectCost;

    /** 费用预算(元) */
    private BigDecimal expenseBudget;

    /** 成本预算(元) */
    private BigDecimal costBudget;

    /** 人力费用(元) */
    private BigDecimal laborCost;

    /** 采购成本 */
    private BigDecimal purchaseCost;

    /** 审批状态 */
    @Excel(name = "审批状态")
    private String approvalStatus;

    /** 审批意见 */
    private String approvalReason;

    /** 行业代码 */
    private String industryCode;

    /** 区域代码 */
    private String regionCode;

    /** 二级区域ID */
    private Long provinceId;

    /** 审批时间 */
    private Date approvalTime;

    /** 审批人 */
    private String approverId;

    /** 税率(%) */
    private BigDecimal taxRate;

    /** 确认人ID */
    private Long confirmUserId;

    /** 确认时间 */
    private Date confirmTime;

    /** 备用域1 */
    private String reservedField1;

    /** 备用域2 */
    private String reservedField2;

    /** 备用域3 */
    private String reservedField3;

    /** 备用域4 */
    private String reservedField4;

    /** 备用域5 */
    private String reservedField5;

    /** 删除标志 */
    private String delFlag;

    /** 收入确认状态 */
    @Excel(name = "收入确认状态")
    private String revenueConfirmStatus;

    /** 收入确认年度 */
    @Excel(name = "收入确认年度")
    private String revenueConfirmYear;

    /** 确认金额(含税) */
    @Excel(name = "确认金额(含税)")
    private BigDecimal confirmAmount;

    /** 税后金额 */
    private BigDecimal afterTaxAmount;

    /** 公司收入确认人姓名 */
    private String companyRevenueConfirmedByName;

    /** 项目审核信息 */
    private List<ProjectApproval> projectApprovalList;

    /** 合同金额 */
    private BigDecimal contractAmount;

    /** 合同状态 */
    private String contractStatus;

    /** 项目经理姓名 */
    private String projectManagerName;

    /** 市场经理姓名 */
    private String marketManagerName;

    /** 销售负责人姓名 */
    private String salesManagerName;

    /** 参与人员姓名列表 */
    private String participantsNames;

    /** 部门名称 */
    private String deptName;

    /** 客户名称 */
    private String customerName;

    /** 客户联系人姓名 */
    private String customerContactName;

    /** 客户联系人电话 */
    private String customerContactPhone;

    /** 二级区域名称 */
    private String provinceName;

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setProjectCode(String projectCode) 
    {
        this.projectCode = projectCode;
    }

    public String getProjectCode() 
    {
        return projectCode;
    }

    public void setProjectName(String projectName) 
    {
        this.projectName = projectName;
    }

    public String getProjectName() 
    {
        return projectName;
    }

    public void setProjectFullName(String projectFullName) 
    {
        this.projectFullName = projectFullName;
    }

    public String getProjectFullName() 
    {
        return projectFullName;
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

    public void setShortName(String shortName) 
    {
        this.shortName = shortName;
    }

    public String getShortName() 
    {
        return shortName;
    }

    public void setEstablishedYear(Integer establishedYear) 
    {
        this.establishedYear = establishedYear;
    }

    public Integer getEstablishedYear() 
    {
        return establishedYear;
    }

    public void setProjectCategory(String projectCategory) 
    {
        this.projectCategory = projectCategory;
    }

    public String getProjectCategory() 
    {
        return projectCategory;
    }

    public void setProjectDept(String projectDept) 
    {
        this.projectDept = projectDept;
    }

    public String getProjectDept() 
    {
        return projectDept;
    }

    public void setProjectStatus(String projectStatus) 
    {
        this.projectStatus = projectStatus;
    }

    public String getProjectStatus() 
    {
        return projectStatus;
    }

    public void setAcceptanceStatus(String acceptanceStatus) 
    {
        this.acceptanceStatus = acceptanceStatus;
    }

    public String getAcceptanceStatus() 
    {
        return acceptanceStatus;
    }

    public void setEstimatedWorkload(BigDecimal estimatedWorkload) 
    {
        this.estimatedWorkload = estimatedWorkload;
    }

    public BigDecimal getEstimatedWorkload() 
    {
        return estimatedWorkload;
    }

    public void setActualWorkload(BigDecimal actualWorkload) 
    {
        this.actualWorkload = actualWorkload;
    }

    public BigDecimal getActualWorkload() 
    {
        return actualWorkload;
    }

    public void setProjectAddress(String projectAddress) 
    {
        this.projectAddress = projectAddress;
    }

    public String getProjectAddress() 
    {
        return projectAddress;
    }

    public void setProjectPlan(String projectPlan) 
    {
        this.projectPlan = projectPlan;
    }

    public String getProjectPlan() 
    {
        return projectPlan;
    }

    public void setProjectDescription(String projectDescription) 
    {
        this.projectDescription = projectDescription;
    }

    public String getProjectDescription() 
    {
        return projectDescription;
    }

    public void setProjectManagerId(Long projectManagerId) 
    {
        this.projectManagerId = projectManagerId;
    }

    public Long getProjectManagerId() 
    {
        return projectManagerId;
    }

    public void setMarketManagerId(Long marketManagerId) 
    {
        this.marketManagerId = marketManagerId;
    }

    public Long getMarketManagerId() 
    {
        return marketManagerId;
    }

    public void setParticipants(String participants) 
    {
        this.participants = participants;
    }

    public String getParticipants() 
    {
        return participants;
    }

    public void setSalesManagerId(Long salesManagerId) 
    {
        this.salesManagerId = salesManagerId;
    }

    public Long getSalesManagerId() 
    {
        return salesManagerId;
    }

    public void setSalesContact(String salesContact) 
    {
        this.salesContact = salesContact;
    }

    public String getSalesContact() 
    {
        return salesContact;
    }

    public void setTeamLeaderId(Long teamLeaderId) 
    {
        this.teamLeaderId = teamLeaderId;
    }

    public Long getTeamLeaderId() 
    {
        return teamLeaderId;
    }

    public void setCustomerId(Long customerId) 
    {
        this.customerId = customerId;
    }

    public Long getCustomerId() 
    {
        return customerId;
    }

    public void setCustomerContactId(Long customerContactId) 
    {
        this.customerContactId = customerContactId;
    }

    public Long getCustomerContactId() 
    {
        return customerContactId;
    }

    public void setMerchantContact(String merchantContact) 
    {
        this.merchantContact = merchantContact;
    }

    public String getMerchantContact() 
    {
        return merchantContact;
    }

    public void setMerchantPhone(String merchantPhone) 
    {
        this.merchantPhone = merchantPhone;
    }

    public String getMerchantPhone() 
    {
        return merchantPhone;
    }

    public void setStartDate(Date startDate) 
    {
        this.startDate = startDate;
    }

    public Date getStartDate() 
    {
        return startDate;
    }

    public void setEndDate(Date endDate) 
    {
        this.endDate = endDate;
    }

    public Date getEndDate() 
    {
        return endDate;
    }

    public void setProductionDate(Date productionDate) 
    {
        this.productionDate = productionDate;
    }

    public Date getProductionDate() 
    {
        return productionDate;
    }

    public void setAcceptanceDate(Date acceptanceDate) 
    {
        this.acceptanceDate = acceptanceDate;
    }

    public Date getAcceptanceDate() 
    {
        return acceptanceDate;
    }

    public void setProjectBudget(BigDecimal projectBudget) 
    {
        this.projectBudget = projectBudget;
    }

    public BigDecimal getProjectBudget() 
    {
        return projectBudget;
    }

    public void setProjectCost(BigDecimal projectCost) 
    {
        this.projectCost = projectCost;
    }

    public BigDecimal getProjectCost() 
    {
        return projectCost;
    }

    public void setExpenseBudget(BigDecimal expenseBudget) 
    {
        this.expenseBudget = expenseBudget;
    }

    public BigDecimal getExpenseBudget() 
    {
        return expenseBudget;
    }

    public void setCostBudget(BigDecimal costBudget) 
    {
        this.costBudget = costBudget;
    }

    public BigDecimal getCostBudget() 
    {
        return costBudget;
    }

    public void setLaborCost(BigDecimal laborCost) 
    {
        this.laborCost = laborCost;
    }

    public BigDecimal getLaborCost() 
    {
        return laborCost;
    }

    public void setPurchaseCost(BigDecimal purchaseCost) 
    {
        this.purchaseCost = purchaseCost;
    }

    public BigDecimal getPurchaseCost() 
    {
        return purchaseCost;
    }

    public void setApprovalStatus(String approvalStatus) 
    {
        this.approvalStatus = approvalStatus;
    }

    public String getApprovalStatus() 
    {
        return approvalStatus;
    }

    public void setApprovalReason(String approvalReason) 
    {
        this.approvalReason = approvalReason;
    }

    public String getApprovalReason() 
    {
        return approvalReason;
    }

    public void setIndustryCode(String industryCode) 
    {
        this.industryCode = industryCode;
    }

    public String getIndustryCode() 
    {
        return industryCode;
    }

    public void setRegionCode(String regionCode) 
    {
        this.regionCode = regionCode;
    }

    public String getRegionCode()
    {
        return regionCode;
    }

    public void setProvinceId(Long provinceId)
    {
        this.provinceId = provinceId;
    }

    public Long getProvinceId()
    {
        return provinceId;
    }

    public void setApprovalTime(Date approvalTime) 
    {
        this.approvalTime = approvalTime;
    }

    public Date getApprovalTime() 
    {
        return approvalTime;
    }

    public void setApproverId(String approverId) 
    {
        this.approverId = approverId;
    }

    public String getApproverId() 
    {
        return approverId;
    }

    public void setTaxRate(BigDecimal taxRate) 
    {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxRate() 
    {
        return taxRate;
    }

    public void setConfirmUserId(Long confirmUserId) 
    {
        this.confirmUserId = confirmUserId;
    }

    public Long getConfirmUserId() 
    {
        return confirmUserId;
    }

    public void setConfirmTime(Date confirmTime) 
    {
        this.confirmTime = confirmTime;
    }

    public Date getConfirmTime() 
    {
        return confirmTime;
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

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    public void setRevenueConfirmStatus(String revenueConfirmStatus) 
    {
        this.revenueConfirmStatus = revenueConfirmStatus;
    }

    public String getRevenueConfirmStatus() 
    {
        return revenueConfirmStatus;
    }

    public void setRevenueConfirmYear(String revenueConfirmYear) 
    {
        this.revenueConfirmYear = revenueConfirmYear;
    }

    public String getRevenueConfirmYear() 
    {
        return revenueConfirmYear;
    }

    public void setConfirmAmount(BigDecimal confirmAmount) 
    {
        this.confirmAmount = confirmAmount;
    }

    public BigDecimal getConfirmAmount() 
    {
        return confirmAmount;
    }

    public void setAfterTaxAmount(BigDecimal afterTaxAmount) 
    {
        this.afterTaxAmount = afterTaxAmount;
    }

    public BigDecimal getAfterTaxAmount() 
    {
        return afterTaxAmount;
    }

    public void setCompanyRevenueConfirmedByName(String companyRevenueConfirmedByName) 
    {
        this.companyRevenueConfirmedByName = companyRevenueConfirmedByName;
    }

    public String getCompanyRevenueConfirmedByName() 
    {
        return companyRevenueConfirmedByName;
    }

    public List<ProjectApproval> getProjectApprovalList()
    {
        return projectApprovalList;
    }

    public void setProjectApprovalList(List<ProjectApproval> projectApprovalList)
    {
        this.projectApprovalList = projectApprovalList;
    }

    public BigDecimal getContractAmount()
    {
        return contractAmount;
    }

    public void setContractAmount(BigDecimal contractAmount)
    {
        this.contractAmount = contractAmount;
    }

    public String getContractStatus()
    {
        return contractStatus;
    }

    public void setContractStatus(String contractStatus)
    {
        this.contractStatus = contractStatus;
    }

    public String getProjectManagerName()
    {
        return projectManagerName;
    }

    public void setProjectManagerName(String projectManagerName)
    {
        this.projectManagerName = projectManagerName;
    }

    public String getMarketManagerName()
    {
        return marketManagerName;
    }

    public void setMarketManagerName(String marketManagerName)
    {
        this.marketManagerName = marketManagerName;
    }

    public String getParticipantsNames()
    {
        return participantsNames;
    }

    public void setParticipantsNames(String participantsNames)
    {
        this.participantsNames = participantsNames;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getSalesManagerName()
    {
        return salesManagerName;
    }

    public void setSalesManagerName(String salesManagerName)
    {
        this.salesManagerName = salesManagerName;
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public String getCustomerContactName()
    {
        return customerContactName;
    }

    public void setCustomerContactName(String customerContactName)
    {
        this.customerContactName = customerContactName;
    }

    public String getCustomerContactPhone()
    {
        return customerContactPhone;
    }

    public void setCustomerContactPhone(String customerContactPhone)
    {
        this.customerContactPhone = customerContactPhone;
    }

    public String getProvinceName()
    {
        return provinceName;
    }

    public void setProvinceName(String provinceName)
    {
        this.provinceName = provinceName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("projectId", getProjectId())
            .append("projectCode", getProjectCode())
            .append("projectName", getProjectName())
            .append("projectFullName", getProjectFullName())
            .append("industry", getIndustry())
            .append("region", getRegion())
            .append("shortName", getShortName())
            .append("establishedYear", getEstablishedYear())
            .append("projectCategory", getProjectCategory())
            .append("projectDept", getProjectDept())
            .append("projectStatus", getProjectStatus())
            .append("acceptanceStatus", getAcceptanceStatus())
            .append("estimatedWorkload", getEstimatedWorkload())
            .append("actualWorkload", getActualWorkload())
            .append("projectAddress", getProjectAddress())
            .append("projectPlan", getProjectPlan())
            .append("projectDescription", getProjectDescription())
            .append("projectManagerId", getProjectManagerId())
            .append("marketManagerId", getMarketManagerId())
            .append("participants", getParticipants())
            .append("salesManagerId", getSalesManagerId())
            .append("salesContact", getSalesContact())
            .append("teamLeaderId", getTeamLeaderId())
            .append("customerId", getCustomerId())
            .append("customerContactId", getCustomerContactId())
            .append("merchantContact", getMerchantContact())
            .append("merchantPhone", getMerchantPhone())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("productionDate", getProductionDate())
            .append("acceptanceDate", getAcceptanceDate())
            .append("projectBudget", getProjectBudget())
            .append("projectCost", getProjectCost())
            .append("expenseBudget", getExpenseBudget())
            .append("costBudget", getCostBudget())
            .append("laborCost", getLaborCost())
            .append("purchaseCost", getPurchaseCost())
            .append("approvalStatus", getApprovalStatus())
            .append("approvalReason", getApprovalReason())
            .append("industryCode", getIndustryCode())
            .append("regionCode", getRegionCode())
            .append("provinceId", getProvinceId())
            .append("approvalTime", getApprovalTime())
            .append("approverId", getApproverId())
            .append("remark", getRemark())
            .append("taxRate", getTaxRate())
            .append("confirmUserId", getConfirmUserId())
            .append("confirmTime", getConfirmTime())
            .append("reservedField1", getReservedField1())
            .append("reservedField2", getReservedField2())
            .append("reservedField3", getReservedField3())
            .append("reservedField4", getReservedField4())
            .append("reservedField5", getReservedField5())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("revenueConfirmStatus", getRevenueConfirmStatus())
            .append("revenueConfirmYear", getRevenueConfirmYear())
            .append("confirmAmount", getConfirmAmount())
            .append("afterTaxAmount", getAfterTaxAmount())
            .append("companyRevenueConfirmedByName", getCompanyRevenueConfirmedByName())
            .append("projectApprovalList", getProjectApprovalList())
            .toString();
    }
}
