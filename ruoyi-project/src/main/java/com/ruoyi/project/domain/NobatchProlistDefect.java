package com.ruoyi.project.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 非批次任务问题单及缺陷对象 pm_nobatch_prolist_defect
 * <p>
 * 与批次问题单(pm_prolist_defect)业务相同，区别：任务信息(任务号/任务名/二级产品/三测试日期/排期状态)
 * 全部冗余实存手填，不走任务库联动、无 task_id FK。问题单=缺陷同一条记录。
 * 迁移自老 yadapm T_B_NOBATCH_PROLIST_AND_DEFECT。
 *
 * @author yadapm-migrate
 */
public class NobatchProlistDefect extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 问题单ID */
    private Long problemId;

    // ===== 任务信息（冗余实存手填）=====
    /** 软件中心任务号(手填) */
    @Excel(name = "软件中心任务号")
    private String taskNo;

    /** 任务名称(手填) */
    @Excel(name = "任务名称")
    private String taskName;

    /** 二级产品(字典sys_product) */
    @Excel(name = "二级产品")
    private String product;

    /** 提交内部测试B包日期(手填) */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "提交内部测试B包日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date internalClosureDate;

    /** 提交功能测试版本日期(手填) */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "提交功能测试版本日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date functionalTestDate;

    /** 提交生产版本日期(手填) */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "提交生产版本日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date productionVersionDate;

    /** 排期状态(字典sys_pqzt手选) */
    @Excel(name = "排期状态", dictType = "sys_pqzt")
    private String scheduleStatus;

    // ===== 问题单及缺陷（同批次）=====
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

    /** 部门ID(项目组手选) */
    private Long deptId;

    /** 备注 */
    @Excel(name = "备注")
    private String remarks;

    /** 删除标志(0正常1删除) */
    private String delFlag;

    // ===== 关联展示字段（JOIN，不持久化）=====
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
    private String creatorName;
    private String submitDateStart;
    private String submitDateEnd;
    private String createDateStart;
    private String createDateEnd;

    public void setProblemId(Long problemId) { this.problemId = problemId; }
    public Long getProblemId() { return problemId; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
    public String getTaskNo() { return taskNo; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskName() { return taskName; }
    public void setProduct(String product) { this.product = product; }
    public String getProduct() { return product; }
    public void setInternalClosureDate(Date internalClosureDate) { this.internalClosureDate = internalClosureDate; }
    public Date getInternalClosureDate() { return internalClosureDate; }
    public void setFunctionalTestDate(Date functionalTestDate) { this.functionalTestDate = functionalTestDate; }
    public Date getFunctionalTestDate() { return functionalTestDate; }
    public void setProductionVersionDate(Date productionVersionDate) { this.productionVersionDate = productionVersionDate; }
    public Date getProductionVersionDate() { return productionVersionDate; }
    public void setScheduleStatus(String scheduleStatus) { this.scheduleStatus = scheduleStatus; }
    public String getScheduleStatus() { return scheduleStatus; }
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
            .append("taskNo", taskNo)
            .append("taskName", taskName)
            .append("product", product)
            .append("problemNo", problemNo)
            .append("problemLevel", problemLevel)
            .append("currentStatus", currentStatus)
            .append("scheduleStatus", scheduleStatus)
            .append("solutionTimeOverOneDay", solutionTimeOverOneDay)
            .append("productionYear", productionYear)
            .append("batchId", batchId)
            .append("deptId", deptId)
            .append("delFlag", delFlag)
            .toString();
    }
}
