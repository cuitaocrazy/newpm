package com.ruoyi.project.domain;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 出入库版本信息对象 pm_version_out
 * 迁移自 yadapm T_B_VERSION_OUT（批次 + 非批次共用，manual_input 区分；本期仅批次=0）
 *
 * @author ruoyi
 * @date 2026-06-11
 */
public class VersionOut extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 版本信息ID */
    private Long id;

    /** 投产年份(字典 sys_ndgl) */
    @NotBlank(message = "投产年份不能为空")
    @Excel(name = "投产年份")
    private String productionYear;

    /** 投产批次id */
    @NotNull(message = "投产批次不能为空")
    private Long batchId;

    /** 投产批次号 */
    @Excel(name = "投产批次号")
    private String proBatchNo;

    /** 子产品ID(字典 sys_product) */
    @NotBlank(message = "子产品不能为空")
    @Excel(name = "子产品")
    private String subVersionCode;

    /** 产品(冗余,来源 pm_sys_name) */
    @Excel(name = "产品")
    private String product;

    /** 版本类型1-6(字典 sys_version_type) */
    @NotBlank(message = "版本类型不能为空")
    @Excel(name = "版本类型")
    private String versionType;

    /** 子系统名称 */
    @NotBlank(message = "子系统名称不能为空")
    @Excel(name = "子系统名称")
    private String sysName;

    /** 基准版本号 */
    @Excel(name = "基准版本号")
    private String baseVersionCode;

    /** 出入库版本号(自动生成,只读) */
    @Excel(name = "出入库版本号")
    private String outLibVersion;

    /** 版本编号(生成中间值) */
    private String versionCode;

    /** 升级包初级版本号(类型5/6) */
    private String outVersion;

    /** 提交人员(sys_user.user_name) */
    @Excel(name = "提交人员")
    private String commName;

    /** 提交人员姓名(显示,JOIN) */
    private String userName;

    /** 版本投产日期(只读,批次带出) */
    @Excel(name = "版本投产日期")
    private String versionPDate;

    /** 是否涉及TWS改造 0是1否 */
    @NotBlank(message = "是否涉及TWS改造不能为空")
    private String isInvolved;

    /** 数据库是否修改 0是1否 */
    @NotBlank(message = "数据库是否修改不能为空")
    private String dbUpdate;

    /** 接口是否修改 0是1否 */
    @NotBlank(message = "接口是否修改不能为空")
    private String usbUpdate;

    /** 组包方式1-6(字典 sys_package_mode) */
    @NotBlank(message = "组包方式不能为空")
    @Excel(name = "组包方式")
    private String packageMode;

    /** 版本状态(字典 sys_version_status,本期可选) */
    @Excel(name = "版本状态")
    private String versionStatus;

    /** 版本说明 */
    @Excel(name = "版本说明")
    private String versionDescr;

    /** 备注(业务) */
    @Excel(name = "备注")
    private String remarks;

    /** 批次标志 0批次 1非批次 */
    private String manualInput;

    /** 关联任务列表(主从) */
    private List<VersionOutTask> taskList;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setProductionYear(String productionYear) { this.productionYear = productionYear; }
    public String getProductionYear() { return productionYear; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getBatchId() { return batchId; }
    public void setProBatchNo(String proBatchNo) { this.proBatchNo = proBatchNo; }
    public String getProBatchNo() { return proBatchNo; }
    public void setSubVersionCode(String subVersionCode) { this.subVersionCode = subVersionCode; }
    public String getSubVersionCode() { return subVersionCode; }
    public void setProduct(String product) { this.product = product; }
    public String getProduct() { return product; }
    public void setVersionType(String versionType) { this.versionType = versionType; }
    public String getVersionType() { return versionType; }
    public void setSysName(String sysName) { this.sysName = sysName; }
    public String getSysName() { return sysName; }
    public void setBaseVersionCode(String baseVersionCode) { this.baseVersionCode = baseVersionCode; }
    public String getBaseVersionCode() { return baseVersionCode; }
    public void setOutLibVersion(String outLibVersion) { this.outLibVersion = outLibVersion; }
    public String getOutLibVersion() { return outLibVersion; }
    public void setVersionCode(String versionCode) { this.versionCode = versionCode; }
    public String getVersionCode() { return versionCode; }
    public void setOutVersion(String outVersion) { this.outVersion = outVersion; }
    public String getOutVersion() { return outVersion; }
    public void setCommName(String commName) { this.commName = commName; }
    public String getCommName() { return commName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserName() { return userName; }
    public void setVersionPDate(String versionPDate) { this.versionPDate = versionPDate; }
    public String getVersionPDate() { return versionPDate; }
    public void setIsInvolved(String isInvolved) { this.isInvolved = isInvolved; }
    public String getIsInvolved() { return isInvolved; }
    public void setDbUpdate(String dbUpdate) { this.dbUpdate = dbUpdate; }
    public String getDbUpdate() { return dbUpdate; }
    public void setUsbUpdate(String usbUpdate) { this.usbUpdate = usbUpdate; }
    public String getUsbUpdate() { return usbUpdate; }
    public void setPackageMode(String packageMode) { this.packageMode = packageMode; }
    public String getPackageMode() { return packageMode; }
    public void setVersionStatus(String versionStatus) { this.versionStatus = versionStatus; }
    public String getVersionStatus() { return versionStatus; }
    public void setVersionDescr(String versionDescr) { this.versionDescr = versionDescr; }
    public String getVersionDescr() { return versionDescr; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getRemarks() { return remarks; }
    public void setManualInput(String manualInput) { this.manualInput = manualInput; }
    public String getManualInput() { return manualInput; }
    public void setTaskList(List<VersionOutTask> taskList) { this.taskList = taskList; }
    public List<VersionOutTask> getTaskList() { return taskList; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("productionYear", getProductionYear())
            .append("batchId", getBatchId())
            .append("subVersionCode", getSubVersionCode())
            .append("versionType", getVersionType())
            .append("sysName", getSysName())
            .append("outLibVersion", getOutLibVersion())
            .append("versionCode", getVersionCode())
            .append("commName", getCommName())
            .append("versionPDate", getVersionPDate())
            .append("packageMode", getPackageMode())
            .append("versionStatus", getVersionStatus())
            .append("manualInput", getManualInput())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
