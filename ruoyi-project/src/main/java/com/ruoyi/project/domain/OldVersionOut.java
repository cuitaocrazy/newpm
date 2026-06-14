package com.ruoyi.project.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 出入库版本旧数据归档对象 pm_old_version_out
 * <p>
 * 纯只读历史快照（迁移自老 T_B_OLD_VERSION_OUT）。所有字段为迁移定格的文本/值，
 * 不与任务库/项目库/字典实时关联（提交人员、版本类型均存文本）。
 *
 * @author yadapm-migrate
 */
public class OldVersionOut extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 子系统名称 */
    @Excel(name = "子系统名称")
    private String sysName;

    /** 子产品名称 */
    @Excel(name = "子产品名称")
    private String product;

    /** 基准版本号 */
    @Excel(name = "基准版本号")
    private String baseVersionCode;

    /** 出入库版本号 */
    @Excel(name = "出入库版本号")
    private String outLibVersion;

    /** 版本类型（历史文本） */
    @Excel(name = "版本类型")
    private String versionType;

    /** 版本编号 */
    @Excel(name = "版本编号")
    private String versionCode;

    /** 提交人员（历史文本，非 user_id） */
    @Excel(name = "提交人员")
    private String commName;

    /** 版本投产日期 */
    @Excel(name = "版本投产日期")
    private String versionPDate;

    /** 版本说明 */
    @Excel(name = "版本说明")
    private String versionDescr;

    /** 备注 */
    @Excel(name = "备注")
    private String remarks;

    /** 任务编号 */
    @Excel(name = "任务编号")
    private String taskNo;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String taskName;

    /** 投产年份 */
    @Excel(name = "投产年份")
    private String proYear;

    /** 投产批次号 */
    @Excel(name = "投产批次号")
    private String proBatchNo;

    /** 是否涉及TWS改造 */
    @Excel(name = "是否涉及TWS改造")
    private String isInvolved;

    /** 数据库是否修改 */
    @Excel(name = "数据库是否修改")
    private String dbUpdate;

    /** 接口是否修改 */
    @Excel(name = "接口是否修改")
    private String usbUpdate;

    /** 顺序号 */
    @Excel(name = "顺序号")
    private String sequenceNo;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setSysName(String sysName) { this.sysName = sysName; }
    public String getSysName() { return sysName; }
    public void setProduct(String product) { this.product = product; }
    public String getProduct() { return product; }
    public void setBaseVersionCode(String baseVersionCode) { this.baseVersionCode = baseVersionCode; }
    public String getBaseVersionCode() { return baseVersionCode; }
    public void setOutLibVersion(String outLibVersion) { this.outLibVersion = outLibVersion; }
    public String getOutLibVersion() { return outLibVersion; }
    public void setVersionType(String versionType) { this.versionType = versionType; }
    public String getVersionType() { return versionType; }
    public void setVersionCode(String versionCode) { this.versionCode = versionCode; }
    public String getVersionCode() { return versionCode; }
    public void setCommName(String commName) { this.commName = commName; }
    public String getCommName() { return commName; }
    public void setVersionPDate(String versionPDate) { this.versionPDate = versionPDate; }
    public String getVersionPDate() { return versionPDate; }
    public void setVersionDescr(String versionDescr) { this.versionDescr = versionDescr; }
    public String getVersionDescr() { return versionDescr; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getRemarks() { return remarks; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
    public String getTaskNo() { return taskNo; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskName() { return taskName; }
    public void setProYear(String proYear) { this.proYear = proYear; }
    public String getProYear() { return proYear; }
    public void setProBatchNo(String proBatchNo) { this.proBatchNo = proBatchNo; }
    public String getProBatchNo() { return proBatchNo; }
    public void setIsInvolved(String isInvolved) { this.isInvolved = isInvolved; }
    public String getIsInvolved() { return isInvolved; }
    public void setDbUpdate(String dbUpdate) { this.dbUpdate = dbUpdate; }
    public String getDbUpdate() { return dbUpdate; }
    public void setUsbUpdate(String usbUpdate) { this.usbUpdate = usbUpdate; }
    public String getUsbUpdate() { return usbUpdate; }
    public void setSequenceNo(String sequenceNo) { this.sequenceNo = sequenceNo; }
    public String getSequenceNo() { return sequenceNo; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", id)
            .append("sysName", sysName)
            .append("product", product)
            .append("baseVersionCode", baseVersionCode)
            .append("outLibVersion", outLibVersion)
            .append("versionType", versionType)
            .append("versionCode", versionCode)
            .append("commName", commName)
            .append("versionPDate", versionPDate)
            .append("versionDescr", versionDescr)
            .append("remarks", remarks)
            .append("taskNo", taskNo)
            .append("taskName", taskName)
            .append("proYear", proYear)
            .append("proBatchNo", proBatchNo)
            .append("isInvolved", isInvolved)
            .append("dbUpdate", dbUpdate)
            .append("usbUpdate", usbUpdate)
            .append("sequenceNo", sequenceNo)
            .toString();
    }
}
