package com.ruoyi.project.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 附件对象 pm_attachment
 *
 * @author ruoyi
 * @date 2026-02-03
 */
public class Attachment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 附件主键ID */
    private Long attachmentId;

    /** 业务类型(project-项目、contract-合同、payment-款项) */
    @Excel(name = "业务类型")
    private String businessType;

    /** 业务ID(项目ID/合同ID/款项ID) */
    @Excel(name = "业务ID")
    private Long businessId;

    /** 文档类型(字典:sys_wdlx) */
    @Excel(name = "文档类型")
    private String documentType;

    /** 文件名 */
    @Excel(name = "文件名")
    private String fileName;

    /** 文件路径 */
    @Excel(name = "文件路径")
    private String filePath;

    /** 文件大小(字节) */
    @Excel(name = "文件大小")
    private Long fileSize;

    /** 文件类型(扩展名) */
    @Excel(name = "文件类型")
    private String fileType;

    /** 文件描述 */
    @Excel(name = "文件描述")
    private String fileDescription;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    /** 文档类型名称（扩展字段，用于列表展示） */
    private String documentTypeName;

    /** 创建人姓名（扩展字段，用于列表展示） */
    private String createByName;

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

    public void setDocumentType(String documentType)
    {
        this.documentType = documentType;
    }

    public String getDocumentType()
    {
        return documentType;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }

    public String getFileType()
    {
        return fileType;
    }

    public void setFileDescription(String fileDescription)
    {
        this.fileDescription = fileDescription;
    }

    public String getFileDescription()
    {
        return fileDescription;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDocumentTypeName(String documentTypeName)
    {
        this.documentTypeName = documentTypeName;
    }

    public String getDocumentTypeName()
    {
        return documentTypeName;
    }

    public void setCreateByName(String createByName)
    {
        this.createByName = createByName;
    }

    public String getCreateByName()
    {
        return createByName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("attachmentId", getAttachmentId())
            .append("businessType", getBusinessType())
            .append("businessId", getBusinessId())
            .append("documentType", getDocumentType())
            .append("fileName", getFileName())
            .append("filePath", getFilePath())
            .append("fileSize", getFileSize())
            .append("fileType", getFileType())
            .append("fileDescription", getFileDescription())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
