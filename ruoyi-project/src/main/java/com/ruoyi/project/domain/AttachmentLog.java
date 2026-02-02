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
 * @date 2026-02-01
 */
public class AttachmentLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志主键ID */
    private Long logId;

    /** 附件ID */
    @Excel(name = "附件ID")
    private Long attachmentId;

    /** 文件原始名称 */
    @Excel(name = "文件名称")
    private String fileOriginalName;

    /** 业务类型 */
    @Excel(name = "业务类型")
    private String businessType;

    /** 业务ID */
    private Long businessId;

    /** 文档类型 */
    @Excel(name = "文档类型")
    private String documentType;

    /** 操作类型 */
    @Excel(name = "操作类型")
    private String operationType;

    /** 操作描述 */
    @Excel(name = "操作描述")
    private String operationDesc;

    /** 操作人ID */
    private Long operationUserId;

    /** 操作人姓名 */
    @Excel(name = "操作人")
    private String operationUserName;

    /** 操作IP */
    @Excel(name = "操作IP")
    private String operationIp;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "操作时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date operationTime;

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

    public void setFileOriginalName(String fileOriginalName)
    {
        this.fileOriginalName = fileOriginalName;
    }

    public String getFileOriginalName()
    {
        return fileOriginalName;
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

    public void setDocumentType(String documentType)
    {
        this.documentType = documentType;
    }

    public String getDocumentType()
    {
        return documentType;
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

    public void setOperationUserId(Long operationUserId)
    {
        this.operationUserId = operationUserId;
    }

    public Long getOperationUserId()
    {
        return operationUserId;
    }

    public void setOperationUserName(String operationUserName)
    {
        this.operationUserName = operationUserName;
    }

    public String getOperationUserName()
    {
        return operationUserName;
    }

    public void setOperationIp(String operationIp)
    {
        this.operationIp = operationIp;
    }

    public String getOperationIp()
    {
        return operationIp;
    }

    public void setOperationTime(Date operationTime)
    {
        this.operationTime = operationTime;
    }

    public Date getOperationTime()
    {
        return operationTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("attachmentId", getAttachmentId())
            .append("fileOriginalName", getFileOriginalName())
            .append("businessType", getBusinessType())
            .append("businessId", getBusinessId())
            .append("documentType", getDocumentType())
            .append("operationType", getOperationType())
            .append("operationDesc", getOperationDesc())
            .append("operationUserId", getOperationUserId())
            .append("operationUserName", getOperationUserName())
            .append("operationIp", getOperationIp())
            .append("operationTime", getOperationTime())
            .append("remark", getRemark())
            .toString();
    }
}
