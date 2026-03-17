package com.ruoyi.project.domain;
// trigger rebuild

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.project.utils.AmountFormatHandler;

/**
 * 项目管理对象 pm_project
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
public class Project extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    private Long projectId;

    /** 项目编号(格式:行业-一级区域-二级区域-简称-年份) */
    private String projectCode;

    /** 项目名称 */
    @Excel(name = "项目名称", sort = 10)
    private String projectName;

    /** 行业 */
    private String industry;

    /** 一级区域 */
    private String region;

    /** 二级区域ID（关联pm_secondary_region表 ） */
    private Long regionId;

    /** 二级区域名称（关联查询） */
    @Excel(name = "二级区域", sort = 30)
    private String regionName;

    /** 简称 */
    private String shortName;

    /** 立项年度 */
    private String establishedYear;

    /** 项目分类 */
    @Excel(name = "项目分类", dictType = "sys_xmfl", sort = 20)
    private String projectCategory;

    /** 项目部门 */
    private String projectDept;

    /** 项目部门路径（导出用，非数据库字段） */
    @Excel(name = "项目部门路径", sort = 40)
    private String deptPathDisplay;

    /** 项目部门 二级机构（导出用，非数据库字段） */
    @Excel(name = "二级机构", sort = 41)
    private String deptOrgLevel2;

    /** 项目部门 三级机构（导出用，非数据库字段） */
    @Excel(name = "三级机构", sort = 42)
    private String deptOrgLevel3;

    /** 项目部门 四级机构（导出用，非数据库字段） */
    @Excel(name = "四级机构", sort = 43)
    private String deptOrgLevel4;

    /** 项目部门 五级机构（导出用，非数据库字段） */
    @Excel(name = "五级机构", sort = 44)
    private String deptOrgLevel5;

    /** 项目部门 六级机构（导出用，非数据库字段） */
    @Excel(name = "六级机构", sort = 45)
    private String deptOrgLevel6;

    /** 项目状态 */
    @Excel(name = "项目状态", dictType = "sys_xmzt", sort = 190)
    private String projectStatus;

    /** 项目阶段 */
    @Excel(name = "项目阶段", dictType = "sys_xmjd", sort = 210)
    private String projectStage;

    /** 验收状态 */
    @Excel(name = "验收状态", dictType = "sys_yszt", sort = 200)
    private String acceptanceStatus;

    /** 预估工作量(人天) */
    @Excel(name = "预估工作量(人天)", sort = 70, handler = AmountFormatHandler.class, args = {"id:1"})
    private BigDecimal estimatedWorkload;

    /** 实际工作量(人天) */
    @Excel(name = "实际人天", sort = 80, handler = AmountFormatHandler.class, args = {"0.000"})
    private BigDecimal actualWorkload;

    /** 调整工作量(人天) */
    private BigDecimal adjustWorkload;

    /** 项目地址 */
    private String projectAddress;

    /** 项目计划 */
    private String projectPlan;

    /** 项目描述 */
    private String projectDescription;

    /** 项目经理ID */
    private Long projectManagerId;

    /** 市场经理ID */
    private Long marketManagerId;

    /** 参与人员ID列表(逗号分隔) */
    private String participants;

    /** 销售负责人ID */
    private Long salesManagerId;

    /** 销售联系方式 */
    private String salesContact;

    /** 团队负责人ID */
    private Long teamLeaderId;

    /** 客户ID */
    private Long customerId;

    /** 客户联系人ID */
    private Long customerContactId;

    /** 关联的合同ID */
    private Long contractId;

    /** 商户联系人 */
    private String merchantContact;

    /** 商户联系方式 */
    private String merchantPhone;

    /** 启动日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "启动日期", width = 30, dateFormat = "yyyy-MM-dd", sort = 150)
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束日期", width = 30, dateFormat = "yyyy-MM-dd", sort = 160)
    private Date endDate;

    /** 投产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date productionDate;

    /** 验收日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "验收日期", width = 30, dateFormat = "yyyy-MM-dd", sort = 170)
    private Date acceptanceDate;

    /** 项目预算(元) */
    @Excel(name = "项目预算(元)", sort = 60, handler = AmountFormatHandler.class)
    private BigDecimal projectBudget;

    /** 项目费用(元) */
    private BigDecimal projectCost;

    /** 费用预算(元) */
    private BigDecimal expenseBudget;

    /** 成本预算(元) */
    private BigDecimal costBudget;

    /** 人力费用(元) */
    private BigDecimal laborCost;

    /** 采购成本 */
    private BigDecimal purchaseCost;

    /** 审核状态(0待审核/1已通过/2已拒绝) */
    @Excel(name = "审核状态", readConverterExp = "0=待审核,1=已通过,2=已拒绝", sort = 180)
    private String approvalStatus;

    /** 审核意见 */
    private String approvalReason;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date approvalTime;

    /** 审核人ID */
    private String approverId;

    /** 行业代码 */
    private String industryCode;

    /** 区域代码(字典:sys_yjqy) */
    private String regionCode;

    /** 税率(%) */
    private BigDecimal taxRate;

    /** 确认人ID */
    private Long confirmUserId;

    /** 确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
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

    /** 父项目ID，NULL 表示顶层主项目 */
    private Long parentId;

    /** 项目层级：0=主项目（默认），1=子项目 */
    private Integer projectLevel;

    /** 子项目编号（在父项目内的简短标识，如 01、用户系统） */
    private String taskCode;

    /** 投产批次ID */
    private Long batchId;

    /** 投产年份 */
    private String productionYear;

    /** 提供内部闭包日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.util.Date internalClosureDate;

    /** 提供功能测试版本日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.util.Date functionalTestDate;

    /** 生产版本日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.util.Date productionVersionDate;

    /** 排期状态（字典 sys_pqzt） */
    private String scheduleStatus;

    /** 功能点说明 */
    private String functionDescription;

    /** 实施计划 */
    private String implementationPlan;

    /** 实际投产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.util.Date actualProductionDate;

    /** 总行需求号 */
    private String bankDemandNo;

    /** 软件中心需求编号 */
    private String softwareDemandNo;

    /** 二级产品 */
    private String product;

    /** 批次号（关联字段，非数据库字段）*/
    private String batchNo;

    /** 计划投产日期（关联字段，非数据库字段）*/
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.util.Date planProductionDate;

    /** 父项目名称（展示用，非 DB 字段，由 Mapper 关联查询填充） */
    private String parentProjectName;

    /** 父项目收入确认年度（查询条件用，非 DB 字段） */
    private String parentRevenueConfirmYear;

    /** 任务创建人 */
    private String taskCreateBy;

    /** 任务创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date taskCreateTime;

    /** 任务更新人 */
    private String taskUpdateBy;

    /** 任务更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date taskUpdateTime;

    /** 任务创建人昵称（关联 sys_user，非 DB 字段） */
    private String taskCreateByName;

    /** 任务更新人昵称（关联 sys_user，非 DB 字段） */
    private String taskUpdateByName;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    /** 收入确认状态（字典:sys_qrzt 未确认、待确认、已确认、无法确认） */
    @Excel(name = "确认状态", dictType = "sys_qrzt", sort = 120)
    private String revenueConfirmStatus;

    /** 收入确认年度 */
    @Excel(name = "收入确认年度", dictType = "sys_ndgl", sort = 110)
    private String revenueConfirmYear;

    /** 收入确认日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date revenueConfirmDate;

    /** 确认金额（含税） */
    @Excel(name = "确认金额(元)", sort = 130, handler = AmountFormatHandler.class)
    private BigDecimal confirmAmount;

    /** 税后金额 */
    private BigDecimal afterTaxAmount;

    /** 公司收入确认人ID */
    private String companyRevenueConfirmedBy;

    /** 公司收入确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date companyRevenueConfirmedTime;

    /** 合同金额（关联字段，非数据库字段） */
    @Excel(name = "合同金额(元)", sort = 90, handler = AmountFormatHandler.class)
    private BigDecimal contractAmount;

    /** 合同名称（关联字段，非数据库字段） */
    private String contractName;

    /** 合同编号（关联字段，非数据库字段） */
    private String contractCode;

    /** 合同状态（关联字段，非数据库字段） */
    @Excel(name = "合同状态", dictType = "sys_htzt", sort = 100)
    private String contractStatus;

    /** 部门名称（关联字段，非数据库字段） */
    private String deptName;

    /** 项目经理名称（关联字段，非数据库字段） */
    @Excel(name = "项目经理", sort = 50)
    private String projectManagerName;

    /** 市场经理名称（关联字段，非数据库字段） */
    private String marketManagerName;

    /** 销售负责人名称（关联字段，非数据库字段） */
    private String salesManagerName;

    /** 客户名称（关联字段，非数据库字段） */
    private String customerName;

    /** 客户联系人名称（关联字段，非数据库字段） */
    private String customerContactName;

    /** 客户联系方式（关联字段，非数据库字段） */
    private String customerContactPhone;

    /** 参与人员名称列表（关联字段，非数据库字段） */
    @Excel(name = "参与人员", sort = 140)
    private String participantsNames;

    /** 更新人昵称（关联字段，非数据库字段） */
    @Excel(name = "更新人", sort = 220)
    private String updateByName;

    /** 申请人昵称（关联字段，非数据库字段） */
    private String createByName;

    /** 确认团队部门名称（关联字段，多个用顿号分隔，非数据库字段） */
    private String teamConfirmDepts;

    /** 团队确认收入汇总（关联字段，非数据库字段） */
    private BigDecimal teamConfirmAmount;

    /** 团队确认明细列表（用于列表分行展示，非数据库字段） */
    private List<Map<String, Object>> teamConfirmList;

    /** 查询条件：确认团队部门ID（-1表示无确认团队，非数据库字段） */
    private Long confirmDeptId;

    /** 更新时间（导出用，非数据库字段） */
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 230)
    private Date updateTimeDisplay;

    public void setUpdateTimeDisplay(Date updateTimeDisplay) { this.updateTimeDisplay = updateTimeDisplay; }
    public Date getUpdateTimeDisplay() { return updateTimeDisplay; }

    public void setDeptPathDisplay(String deptPathDisplay) { this.deptPathDisplay = deptPathDisplay; }
    public String getDeptPathDisplay() { return deptPathDisplay; }

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

    public void setUpdateByName(String updateByName) { this.updateByName = updateByName; }
    public String getUpdateByName() { return updateByName; }

    public void setCreateByName(String createByName) { this.createByName = createByName; }
    public String getCreateByName() { return createByName; }

    public void setTeamConfirmDepts(String teamConfirmDepts) { this.teamConfirmDepts = teamConfirmDepts; }
    public String getTeamConfirmDepts() { return teamConfirmDepts; }

    public void setTeamConfirmAmount(BigDecimal teamConfirmAmount) { this.teamConfirmAmount = teamConfirmAmount; }
    public BigDecimal getTeamConfirmAmount() { return teamConfirmAmount; }

    public void setTeamConfirmList(List<Map<String, Object>> teamConfirmList) { this.teamConfirmList = teamConfirmList; }
    public List<Map<String, Object>> getTeamConfirmList() { return teamConfirmList; }

    public void setConfirmDeptId(Long confirmDeptId) { this.confirmDeptId = confirmDeptId; }
    public Long getConfirmDeptId() { return confirmDeptId; }

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

    public void setRegionId(Long regionId)
    {
        this.regionId = regionId;
    }

    public Long getRegionId()
    {
        return regionId;
    }

    public void setRegionName(String regionName)
    {
        this.regionName = regionName;
    }

    public String getRegionName()
    {
        return regionName;
    }

    public void setShortName(String shortName) 
    {
        this.shortName = shortName;
    }

    public String getShortName() 
    {
        return shortName;
    }

    public void setEstablishedYear(String establishedYear)
    {
        this.establishedYear = establishedYear;
    }

    public String getEstablishedYear()
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

    public void setProjectStage(String projectStage) 
    {
        this.projectStage = projectStage;
    }

    public String getProjectStage()
    {
        return projectStage;
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

    public void setAdjustWorkload(BigDecimal adjustWorkload)
    {
        this.adjustWorkload = adjustWorkload;
    }

    public BigDecimal getAdjustWorkload()
    {
        return adjustWorkload;
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

    public void setContractId(Long contractId)
    {
        this.contractId = contractId;
    }

    public Long getContractId()
    {
        return contractId;
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

    public void setParentId(Long parentId)              { this.parentId = parentId; }
    public Long getParentId()                           { return parentId; }

    public void setProjectLevel(Integer projectLevel)   { this.projectLevel = projectLevel; }
    public Integer getProjectLevel()                    { return projectLevel; }

    public void setTaskCode(String taskCode)            { this.taskCode = taskCode; }
    public String getTaskCode()                         { return taskCode; }

    public Long getBatchId()                            { return batchId; }
    public void setBatchId(Long batchId)                { this.batchId = batchId; }
    public String getProductionYear()                   { return productionYear; }
    public void setProductionYear(String productionYear){ this.productionYear = productionYear; }
    public String getBankDemandNo()                     { return bankDemandNo; }
    public void setBankDemandNo(String bankDemandNo)    { this.bankDemandNo = bankDemandNo; }
    public String getSoftwareDemandNo()                 { return softwareDemandNo; }
    public void setSoftwareDemandNo(String v)           { this.softwareDemandNo = v; }
    public String getProduct()                          { return product; }
    public void setProduct(String product)              { this.product = product; }
    public String getBatchNo()                          { return batchNo; }
    public void setBatchNo(String batchNo)              { this.batchNo = batchNo; }
    public java.util.Date getPlanProductionDate()       { return planProductionDate; }
    public void setPlanProductionDate(java.util.Date d) { this.planProductionDate = d; }

    public java.util.Date getInternalClosureDate() { return internalClosureDate; }
    public void setInternalClosureDate(java.util.Date internalClosureDate) { this.internalClosureDate = internalClosureDate; }

    public java.util.Date getFunctionalTestDate() { return functionalTestDate; }
    public void setFunctionalTestDate(java.util.Date functionalTestDate) { this.functionalTestDate = functionalTestDate; }

    public java.util.Date getProductionVersionDate() { return productionVersionDate; }
    public void setProductionVersionDate(java.util.Date productionVersionDate) { this.productionVersionDate = productionVersionDate; }

    public String getScheduleStatus() { return scheduleStatus; }
    public void setScheduleStatus(String scheduleStatus) { this.scheduleStatus = scheduleStatus; }

    public String getFunctionDescription() { return functionDescription; }
    public void setFunctionDescription(String functionDescription) { this.functionDescription = functionDescription; }

    public String getImplementationPlan() { return implementationPlan; }
    public void setImplementationPlan(String implementationPlan) { this.implementationPlan = implementationPlan; }

    public java.util.Date getActualProductionDate() { return actualProductionDate; }
    public void setActualProductionDate(java.util.Date actualProductionDate) { this.actualProductionDate = actualProductionDate; }

    public void setParentProjectName(String n)          { this.parentProjectName = n; }
    public String getParentProjectName()                { return parentProjectName; }

    public String getParentRevenueConfirmYear()         { return parentRevenueConfirmYear; }
    public void setParentRevenueConfirmYear(String y)   { this.parentRevenueConfirmYear = y; }

    public String getTaskCreateBy()                     { return taskCreateBy; }
    public void setTaskCreateBy(String taskCreateBy)    { this.taskCreateBy = taskCreateBy; }
    public java.util.Date getTaskCreateTime()           { return taskCreateTime; }
    public void setTaskCreateTime(java.util.Date t)     { this.taskCreateTime = t; }
    public String getTaskUpdateBy()                     { return taskUpdateBy; }
    public void setTaskUpdateBy(String taskUpdateBy)    { this.taskUpdateBy = taskUpdateBy; }
    public java.util.Date getTaskUpdateTime()           { return taskUpdateTime; }
    public void setTaskUpdateTime(java.util.Date t)     { this.taskUpdateTime = t; }
    public String getTaskCreateByName()                 { return taskCreateByName; }
    public void setTaskCreateByName(String n)           { this.taskCreateByName = n; }
    public String getTaskUpdateByName()                 { return taskUpdateByName; }
    public void setTaskUpdateByName(String n)           { this.taskUpdateByName = n; }

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

    public void setRevenueConfirmDate(Date revenueConfirmDate)
    {
        this.revenueConfirmDate = revenueConfirmDate;
    }

    public Date getRevenueConfirmDate()
    {
        return revenueConfirmDate;
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

    public void setCompanyRevenueConfirmedBy(String companyRevenueConfirmedBy) 
    {
        this.companyRevenueConfirmedBy = companyRevenueConfirmedBy;
    }

    public String getCompanyRevenueConfirmedBy() 
    {
        return companyRevenueConfirmedBy;
    }

    public void setCompanyRevenueConfirmedTime(Date companyRevenueConfirmedTime) 
    {
        this.companyRevenueConfirmedTime = companyRevenueConfirmedTime;
    }

    public Date getCompanyRevenueConfirmedTime()
    {
        return companyRevenueConfirmedTime;
    }

    public void setContractAmount(BigDecimal contractAmount)
    {
        this.contractAmount = contractAmount;
    }

    public BigDecimal getContractAmount()
    {
        return contractAmount;
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

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setProjectManagerName(String projectManagerName)
    {
        this.projectManagerName = projectManagerName;
    }

    public String getProjectManagerName()
    {
        return projectManagerName;
    }

    public void setMarketManagerName(String marketManagerName)
    {
        this.marketManagerName = marketManagerName;
    }

    public String getMarketManagerName()
    {
        return marketManagerName;
    }

    public void setSalesManagerName(String salesManagerName)
    {
        this.salesManagerName = salesManagerName;
    }

    public String getSalesManagerName()
    {
        return salesManagerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerContactName(String customerContactName)
    {
        this.customerContactName = customerContactName;
    }

    public String getCustomerContactName()
    {
        return customerContactName;
    }

    public void setCustomerContactPhone(String customerContactPhone)
    {
        this.customerContactPhone = customerContactPhone;
    }

    public String getCustomerContactPhone()
    {
        return customerContactPhone;
    }

    public void setParticipantsNames(String participantsNames)
    {
        this.participantsNames = participantsNames;
    }

    public String getParticipantsNames()
    {
        return participantsNames;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("projectId", getProjectId())
            .append("projectCode", getProjectCode())
            .append("projectName", getProjectName())
            .append("industry", getIndustry())
            .append("region", getRegion())
            .append("regionId", getRegionId())
            .append("shortName", getShortName())
            .append("establishedYear", getEstablishedYear())
            .append("projectCategory", getProjectCategory())
            .append("projectDept", getProjectDept())
            .append("projectStatus", getProjectStatus())
            .append("projectStage", getProjectStage())
            .append("acceptanceStatus", getAcceptanceStatus())
            .append("estimatedWorkload", getEstimatedWorkload())
            .append("actualWorkload", getActualWorkload())
            .append("adjustWorkload", getAdjustWorkload())
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
            .append("approvalTime", getApprovalTime())
            .append("approverId", getApproverId())
            .append("industryCode", getIndustryCode())
            .append("regionCode", getRegionCode())
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
            .append("parentId", getParentId())
            .append("projectLevel", getProjectLevel())
            .append("taskCode", getTaskCode())
            .append("batchId", getBatchId())
            .append("productionYear", getProductionYear())
            .append("internalClosureDate", getInternalClosureDate())
            .append("functionalTestDate", getFunctionalTestDate())
            .append("bankDemandNo", getBankDemandNo())
            .append("softwareDemandNo", getSoftwareDemandNo())
            .append("product", getProduct())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("revenueConfirmStatus", getRevenueConfirmStatus())
            .append("revenueConfirmYear", getRevenueConfirmYear())
            .append("confirmAmount", getConfirmAmount())
            .append("afterTaxAmount", getAfterTaxAmount())
            .append("companyRevenueConfirmedBy", getCompanyRevenueConfirmedBy())
            .append("companyRevenueConfirmedTime", getCompanyRevenueConfirmedTime())
            .toString();
    }
}
