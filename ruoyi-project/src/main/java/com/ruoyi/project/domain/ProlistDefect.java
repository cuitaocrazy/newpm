package com.ruoyi.project.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 批次任务问题单及缺陷对象 pm_prolist_defect
 * <p>
 * 「问题单 = 缺陷」同一条记录：一行问题单挂一组布尔标记（是否缺陷/超时/重现/须关注/更新版本）。
 * 任务名/二级产品/各测试日期/批次/部门/状态名 等关联实时 JOIN 取，不冗余存主表。
 * 迁移自老 yadapm T_B_PROLIST_AND_DEFECT。
 *
 * @author yadapm-migrate
 */
public class ProlistDefect extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 问题单ID */
    private Long problemId;

    /** 任务ID(FK→pm_task) */
    @Excel(name = "任务ID")
    private Long taskId;

    /** 问题单编号(唯一) */
    @Excel(name = "问题单编号")
    private String problemNo;

    /** 问题单级别(字典sys_problem_level) */
    @Excel(name = "问题单级别", dictType = "sys_problem_level")
    private String problemLevel;

    /** 当前状态(字典sys_problem_state) */
    @Excel(name = "当前状态", dictType = "sys_problem_state")
    private String currentStatus;

    /** 提交日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "提交日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date submitDate;

    /** 解决/关闭日期(可空) */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "问题单关闭日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date settleDate;

    /** 核查日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "核查日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date verifyDate;

    /** 是否缺陷(0否1是) */
    @Excel(name = "是否缺陷", readConverterExp = "0=否,1=是")
    private String whetherDefect;

    /** 是否超时(0否1是) */
    @Excel(name = "是否超时", readConverterExp = "0=否,1=是")
    private String whetherOvertime;

    /** 是否问题重现(0否1是) */
    @Excel(name = "是否问题重现", readConverterExp = "0=否,1=是")
    private String whetherProRecurrence;

    /** 是否须关注(0否1是) */
    @Excel(name = "是否须关注", readConverterExp = "0=否,1=是")
    private String whetherAttRequired;

    /** 是否更新版本(0否1是) */
    @Excel(name = "是否更新版本", readConverterExp = "0=否,1=是")
    private String whetherUpdateVersion;

    /** 解决时间超一天(派生:0否1是) */
    @Excel(name = "解决时间超一天", readConverterExp = "0=否,1=是")
    private String solutionTimeOverOneDay;

    /** 缺陷说明/超时说明 */
    @Excel(name = "缺陷说明")
    private String defectDesc;

    /** 投产年份(字典sys_ndgl) */
    @Excel(name = "投产年份")
    private String productionYear;

    /** 投产批次ID(FK→pm_production_batch) */
    private Long batchId;

    /** 部门ID(项目组→部门) */
    private Long deptId;

    /** 备注 */
    @Excel(name = "备注")
    private String remarks;

    /** 删除标志(0正常1删除) */
    private String delFlag;

    // ===== 关联展示字段（JOIN 实时取，不持久化）=====
    /** 任务名称 */
    @Excel(name = "任务名称")
    private String taskName;
    /** 软件中心任务号 */
    @Excel(name = "软件中心任务号")
    private String taskCode;
    /** 二级产品 */
    @Excel(name = "二级产品")
    private String product;
    /** 内部B包日期 */
    private String internalClosureDate;
    /** 功能测试版本日期 */
    private String functionalTestDate;
    /** 生产版本日期 */
    private String productionVersionDate;
    /** 排期状态(字典sys_pqzt) */
    private String scheduleStatus;
    /** 批次号 */
    @Excel(name = "批次号")
    private String batchNo;
    /** 计划投产日期 */
    private String planProductionDate;
    /** 部门名称 */
    @Excel(name = "项目组")
    private String deptName;
    /** 创建人姓名 */
    @Excel(name = "创建人员")
    private String createByName;
    /** 修改人姓名 */
    @Excel(name = "修改人员")
    private String updateByName;

    // ===== 查询专用字段（不持久化）=====
    /** 一级产品(查询用，联动二级) */
    private String productTop;
    /** 创建人(查询用) */
    private String creatorName;
    /** 提交日期范围-起 */
    private String submitDateStart;
    /** 提交日期范围-止 */
    private String submitDateEnd;
    /** 创建日期范围-起 */
    private String createDateStart;
    /** 创建日期范围-止 */
    private String createDateEnd;

    public void setProblemId(Long problemId) { this.problemId = problemId; }
    public Long getProblemId() { return problemId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getTaskId() { return taskId; }
    public void setProblemNo(String problemNo) { this.problemNo = problemNo; }
    public String getProblemNo() { return problemNo; }
    public void setProblemLevel(String problemLevel) { this.problemLevel = problemLevel; }
    public String getProblemLevel() { return problemLevel; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public String getCurrentStatus() { return currentStatus; }
    public void setSubmitDate(Date submitDate) { this.submitDate = submitDate; }
    public Date getSubmitDate() { return submitDate; }
    public void setSettleDate(Date settleDate) { this.settleDate = settleDate; }
    public Date getSettleDate() { return settleDate; }
    public void setVerifyDate(Date verifyDate) { this.verifyDate = verifyDate; }
    public Date getVerifyDate() { return verifyDate; }
    public void setWhetherDefect(String whetherDefect) { this.whetherDefect = whetherDefect; }
    public String getWhetherDefect() { return whetherDefect; }
    public void setWhetherOvertime(String whetherOvertime) { this.whetherOvertime = whetherOvertime; }
    public String getWhetherOvertime() { return whetherOvertime; }
    public void setWhetherProRecurrence(String whetherProRecurrence) { this.whetherProRecurrence = whetherProRecurrence; }
    public String getWhetherProRecurrence() { return whetherProRecurrence; }
    public void setWhetherAttRequired(String whetherAttRequired) { this.whetherAttRequired = whetherAttRequired; }
    public String getWhetherAttRequired() { return whetherAttRequired; }
    public void setWhetherUpdateVersion(String whetherUpdateVersion) { this.whetherUpdateVersion = whetherUpdateVersion; }
    public String getWhetherUpdateVersion() { return whetherUpdateVersion; }
    public void setSolutionTimeOverOneDay(String solutionTimeOverOneDay) { this.solutionTimeOverOneDay = solutionTimeOverOneDay; }
    public String getSolutionTimeOverOneDay() { return solutionTimeOverOneDay; }
    public void setDefectDesc(String defectDesc) { this.defectDesc = defectDesc; }
    public String getDefectDesc() { return defectDesc; }
    public void setProductionYear(String productionYear) { this.productionYear = productionYear; }
    public String getProductionYear() { return productionYear; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getBatchId() { return batchId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getDeptId() { return deptId; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getRemarks() { return remarks; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getDelFlag() { return delFlag; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskName() { return taskName; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }
    public String getTaskCode() { return taskCode; }
    public void setProduct(String product) { this.product = product; }
    public String getProduct() { return product; }
    public void setInternalClosureDate(String internalClosureDate) { this.internalClosureDate = internalClosureDate; }
    public String getInternalClosureDate() { return internalClosureDate; }
    public void setFunctionalTestDate(String functionalTestDate) { this.functionalTestDate = functionalTestDate; }
    public String getFunctionalTestDate() { return functionalTestDate; }
    public void setProductionVersionDate(String productionVersionDate) { this.productionVersionDate = productionVersionDate; }
    public String getProductionVersionDate() { return productionVersionDate; }
    public void setScheduleStatus(String scheduleStatus) { this.scheduleStatus = scheduleStatus; }
    public String getScheduleStatus() { return scheduleStatus; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getBatchNo() { return batchNo; }
    public void setPlanProductionDate(String planProductionDate) { this.planProductionDate = planProductionDate; }
    public String getPlanProductionDate() { return planProductionDate; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getDeptName() { return deptName; }
    public void setCreateByName(String createByName) { this.createByName = createByName; }
    public String getCreateByName() { return createByName; }
    public void setUpdateByName(String updateByName) { this.updateByName = updateByName; }
    public String getUpdateByName() { return updateByName; }
    public void setProductTop(String productTop) { this.productTop = productTop; }
    public String getProductTop() { return productTop; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public String getCreatorName() { return creatorName; }
    public void setSubmitDateStart(String submitDateStart) { this.submitDateStart = submitDateStart; }
    public String getSubmitDateStart() { return submitDateStart; }
    public void setSubmitDateEnd(String submitDateEnd) { this.submitDateEnd = submitDateEnd; }
    public String getSubmitDateEnd() { return submitDateEnd; }
    public void setCreateDateStart(String createDateStart) { this.createDateStart = createDateStart; }
    public String getCreateDateStart() { return createDateStart; }
    public void setCreateDateEnd(String createDateEnd) { this.createDateEnd = createDateEnd; }
    public String getCreateDateEnd() { return createDateEnd; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("problemId", problemId)
            .append("taskId", taskId)
            .append("problemNo", problemNo)
            .append("problemLevel", problemLevel)
            .append("currentStatus", currentStatus)
            .append("submitDate", submitDate)
            .append("settleDate", settleDate)
            .append("verifyDate", verifyDate)
            .append("whetherDefect", whetherDefect)
            .append("whetherOvertime", whetherOvertime)
            .append("whetherProRecurrence", whetherProRecurrence)
            .append("whetherAttRequired", whetherAttRequired)
            .append("whetherUpdateVersion", whetherUpdateVersion)
            .append("solutionTimeOverOneDay", solutionTimeOverOneDay)
            .append("defectDesc", defectDesc)
            .append("productionYear", productionYear)
            .append("batchId", batchId)
            .append("deptId", deptId)
            .append("remarks", remarks)
            .append("delFlag", delFlag)
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
