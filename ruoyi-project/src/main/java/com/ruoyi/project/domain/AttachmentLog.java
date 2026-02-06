package com.ruoyi.project.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 附件操作日志对象 pm_attachment_log
 *
 * @author ruoyi
 * @date 2026-02-03
 */
public class AttachmentLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志主键ID */
    private Long logId;

    /** 附件ID */
    @Excel(name = "附件ID")
    private Long attachmentId;

    /** 业务类型(project-项目、contract-合同、payment-款项) */
    @Excel(name = "业务类型")
    private String businessType;

    /** 业务ID(项目ID/合同ID/款项ID) */
    @Excel(name = "业务ID")
    private Long businessId;

    /** 操作类型(upload-上传、delete-删除、download-下载) */
    @Excel(name = "操作类型")
    private String operationType;

    /** 操作描述(如：【上传文件】：测试文件.pdf) */
    @Excel(name = "操作描述")
    private String operationDesc;

    /** 文档类型(字典:sys_wdlx) */
    @Excel(name = "文档类型")
    private String documentType;

    /** 文档类型名称 */
    private String documentTypeName;

    /** 文件名 */
    @Excel(name = "文件名")
    private String fileName;

    /** 操作人ID */
    @Excel(name = "操作人ID")
    private Long operatorId;

    /** 操作人姓名 */
    @Excel(name = "操作人姓名")
    private String operatorName;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "操作时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date operationTime;

    /** 创建时间（前端兼容字段，映射到operationTime） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public Long getLogId()
    {
        return logId;
    }

    public void setAttachmentId(Long attachmentId)
    {
        this.attachmentId = attachmentId;
    }

    public Long getAttachmentId()
    {
        return attachmentId;
    }

    public void setBusinessType(String businessType)
    {
        this.businessType = businessType;
    }

    public String getBusinessType()
    {
        return businessType;
    }

    public void setBusinessId(Long businessId)
    {
        this.businessId = businessId;
    }

    public Long getBusinessId()
    {
        return businessId;
    }

    public void setOperationType(String operationType)
    {
        this.operationType = operationType;
    }

    public String getOperationType()
    {
        return operationType;
    }

    public void setOperationDesc(String operationDesc)
    {
        this.operationDesc = operationDesc;
    }

    public String getOperationDesc()
    {
        return operationDesc;
    }

    public void setDocumentType(String documentType)
    {
        this.documentType = documentType;
    }

    public String getDocumentType()
    {
        return documentType;
    }

    public void setDocumentTypeName(String documentTypeName)
    {
        this.documentTypeName = documentTypeName;
    }

    public String getDocumentTypeName()
    {
        return documentTypeName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperationTime(Date operationTime)
    {
        this.operationTime = operationTime;
    }

    public Date getOperationTime()
    {
        return operationTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("attachmentId", getAttachmentId())
            .append("businessType", getBusinessType())
            .append("businessId", getBusinessId())
            .append("operationType", getOperationType())
            .append("operationDesc", getOperationDesc())
            .append("documentType", getDocumentType())
            .append("documentTypeName", getDocumentTypeName())
            .append("fileName", getFileName())
            .append("operatorId", getOperatorId())
            .append("operatorName", getOperatorName())
            .append("operationTime", getOperationTime())
            .append("createTime", getCreateTime())
            .append("remark", getRemark())
            .toString();
    }
}
