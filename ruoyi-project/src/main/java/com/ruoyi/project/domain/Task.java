package com.ruoyi.project.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 任务管理对象 pm_task
 *
 * @author ruoyi
 * @date 2026-03-12
 */
public class Task extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 所属主项目ID */
    private Long projectId;

    /** 任务编号 */
    @Excel(name = "任务编号")
    private String taskCode;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String taskName;

    /** 任务阶段(sys_xmjd) */
    @Excel(name = "任务阶段", dictType = "sys_xmjd")
    private String taskStage;

    /** 任务负责人ID */
    private Long taskManagerId;

    /** 产品(sys_product) */
    private String product;

    /** 总行需求号 */
    private String bankDemandNo;

    /** 软件中心需求编号 */
    private String softwareDemandNo;

    /** 任务预算(元) */
    @Excel(name = "任务预算(元)")
    private BigDecimal taskBudget;

    /** 预估工作量(人天) */
    @Excel(name = "预估工作量(人天)")
    private BigDecimal estimatedWorkload;

    /** 实际工作量(小时，日报汇总，只读) */
    @Excel(name = "实际工作量(小时)")
    private BigDecimal actualWorkload;

    /** 投产年度(sys_ndgl) */
    private String productionYear;

    /** 投产批次ID */
    private Long batchId;

    /** 任务计划 */
    private String taskPlan;

    /** 任务描述 */
    private String taskDescription;

    /** 启动日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "启动日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 投产时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date productionDate;

    /** 生产版本日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date productionVersionDate;

    /** 实际投产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date actualProductionDate;

    /** 内部B包日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date internalClosureDate;

    /** 功能测试版本日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date functionalTestDate;

    /** 排期状态(sys_pqzt) */
    @Excel(name = "排期状态", dictType = "sys_pqzt")
    private String scheduleStatus;

    /** 功能点说明 */
    private String functionDescription;

    /** 实施计划 */
    private String implementationPlan;

    // ========== 非DB展示字段 ==========

    /** 任务负责人姓名（关联字段，非数据库字段） */
    private String taskManagerName;

    /** 所属主项目名称（关联字段，非数据库字段） */
    private String parentProjectName;

    /** 父项目状态（展示用，非数据库字段） */
    private String projectStatus;

    /** 投产批次号（来自 pm_production_batch，非数据库字段） */
    private String batchNo;

    // ========== 查询参数字段（非DB） ==========

    /** 查询参数：按父项目ID筛选（等同 projectId，前端传参用） */
    private Long parentId;

    /** 查询参数：父项目确认年度 */
    private String parentRevenueConfirmYear;

    /** 查询参数：父项目部门（用于部门层级过滤） */
    private String projectDept;

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setTaskCode(String taskCode)
    {
        this.taskCode = taskCode;
    }

    public String getTaskCode()
    {
        return taskCode;
    }

    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }

    public String getTaskName()
    {
        return taskName;
    }

    public void setTaskStage(String taskStage)
    {
        this.taskStage = taskStage;
    }

    public String getTaskStage()
    {
        return taskStage;
    }

    public void setTaskManagerId(Long taskManagerId)
    {
        this.taskManagerId = taskManagerId;
    }

    public Long getTaskManagerId()
    {
        return taskManagerId;
    }

    public void setProduct(String product)
    {
        this.product = product;
    }

    public String getProduct()
    {
        return product;
    }

    public void setBankDemandNo(String bankDemandNo)
    {
        this.bankDemandNo = bankDemandNo;
    }

    public String getBankDemandNo()
    {
        return bankDemandNo;
    }

    public void setSoftwareDemandNo(String softwareDemandNo)
    {
        this.softwareDemandNo = softwareDemandNo;
    }

    public String getSoftwareDemandNo()
    {
        return softwareDemandNo;
    }

    public void setTaskBudget(BigDecimal taskBudget)
    {
        this.taskBudget = taskBudget;
    }

    public BigDecimal getTaskBudget()
    {
        return taskBudget;
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

    public void setProductionYear(String productionYear)
    {
        this.productionYear = productionYear;
    }

    public String getProductionYear()
    {
        return productionYear;
    }

    public void setBatchId(Long batchId)
    {
        this.batchId = batchId;
    }

    public Long getBatchId()
    {
        return batchId;
    }

    public void setTaskPlan(String taskPlan)
    {
        this.taskPlan = taskPlan;
    }

    public String getTaskPlan()
    {
        return taskPlan;
    }

    public void setTaskDescription(String taskDescription)
    {
        this.taskDescription = taskDescription;
    }

    public String getTaskDescription()
    {
        return taskDescription;
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

    public void setProductionVersionDate(Date productionVersionDate)
    {
        this.productionVersionDate = productionVersionDate;
    }

    public Date getProductionVersionDate()
    {
        return productionVersionDate;
    }

    public void setActualProductionDate(Date actualProductionDate)
    {
        this.actualProductionDate = actualProductionDate;
    }

    public Date getActualProductionDate()
    {
        return actualProductionDate;
    }

    public void setInternalClosureDate(Date internalClosureDate)
    {
        this.internalClosureDate = internalClosureDate;
    }

    public Date getInternalClosureDate()
    {
        return internalClosureDate;
    }

    public void setFunctionalTestDate(Date functionalTestDate)
    {
        this.functionalTestDate = functionalTestDate;
    }

    public Date getFunctionalTestDate()
    {
        return functionalTestDate;
    }

    public void setScheduleStatus(String scheduleStatus)
    {
        this.scheduleStatus = scheduleStatus;
    }

    public String getScheduleStatus()
    {
        return scheduleStatus;
    }

    public void setFunctionDescription(String functionDescription)
    {
        this.functionDescription = functionDescription;
    }

    public String getFunctionDescription()
    {
        return functionDescription;
    }

    public void setImplementationPlan(String implementationPlan)
    {
        this.implementationPlan = implementationPlan;
    }

    public String getImplementationPlan()
    {
        return implementationPlan;
    }

    public void setTaskManagerName(String taskManagerName)   { this.taskManagerName = taskManagerName; }
    public String getTaskManagerName()                       { return taskManagerName; }

    public void setParentProjectName(String parentProjectName) { this.parentProjectName = parentProjectName; }
    public String getParentProjectName()                       { return parentProjectName; }

    public void setProjectStatus(String projectStatus)       { this.projectStatus = projectStatus; }
    public String getProjectStatus()                         { return projectStatus; }

    public void setBatchNo(String batchNo)                   { this.batchNo = batchNo; }
    public String getBatchNo()                               { return batchNo; }

    public void setParentId(Long parentId)                   { this.parentId = parentId; }
    public Long getParentId()                                { return parentId; }

    public void setParentRevenueConfirmYear(String y)        { this.parentRevenueConfirmYear = y; }
    public String getParentRevenueConfirmYear()              { return parentRevenueConfirmYear; }

    public void setProjectDept(String projectDept)           { this.projectDept = projectDept; }
    public String getProjectDept()                           { return projectDept; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("taskId", getTaskId())
            .append("projectId", getProjectId())
            .append("taskCode", getTaskCode())
            .append("taskName", getTaskName())
            .append("taskStage", getTaskStage())
            .append("taskManagerId", getTaskManagerId())
            .append("product", getProduct())
            .append("bankDemandNo", getBankDemandNo())
            .append("softwareDemandNo", getSoftwareDemandNo())
            .append("taskBudget", getTaskBudget())
            .append("estimatedWorkload", getEstimatedWorkload())
            .append("actualWorkload", getActualWorkload())
            .append("productionYear", getProductionYear())
            .append("batchId", getBatchId())
            .append("taskPlan", getTaskPlan())
            .append("taskDescription", getTaskDescription())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("productionDate", getProductionDate())
            .append("productionVersionDate", getProductionVersionDate())
            .append("actualProductionDate", getActualProductionDate())
            .append("internalClosureDate", getInternalClosureDate())
            .append("functionalTestDate", getFunctionalTestDate())
            .append("scheduleStatus", getScheduleStatus())
            .append("functionDescription", getFunctionDescription())
            .append("implementationPlan", getImplementationPlan())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
