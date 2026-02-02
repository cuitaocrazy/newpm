package com.ruoyi.project.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 附件管理对象 pm_attachment
 *
 * @author ruoyi
 * @date 2026-02-01
 */
public class Attachment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 附件主键ID */
    private Long attachmentId;

    /** 文件名称 */
    @Excel(name = "文件名称")
    private String fileName;

    /** 文件原始名称 */
    @Excel(name = "文件原始名称")
    private String fileOriginalName;

    /** 文件存储路径 */
    private String filePath;

    /** 文件大小(字节) */
    @Excel(name = "文件大小")
    private Long fileSize;

    /** 文件类型(扩展名) */
    @Excel(name = "文件类型")
    private String fileType;

    /** 文件访问URL */
    private String fileUrl;

    /** 业务类型 */
    @Excel(name = "业务类型")
    private String businessType;

    /** 业务ID */
    @Excel(name = "业务ID")
    private Long businessId;

    /** 文档类型 */
    @Excel(name = "文档类型")
    private String documentType;

    /** 上传人ID */
    private Long uploadUserId;

    /** 上传人姓名 */
    @Excel(name = "上传人")
    private String uploadUserName;

    /** 下载次数 */
    @Excel(name = "下载次数")
    private Integer downloadCount;

    /** 删除标志 */
    private String delFlag;

    public void setAttachmentId(Long attachmentId)
    {
        this.attachmentId = attachmentId;
    }

    public Long getAttachmentId()
    {
        return attachmentId;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileOriginalName(String fileOriginalName)
    {
        this.fileOriginalName = fileOriginalName;
    }

    public String getFileOriginalName()
    {
        return fileOriginalName;
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

    public void setFileUrl(String fileUrl)
    {
        this.fileUrl = fileUrl;
    }

    public String getFileUrl()
    {
        return fileUrl;
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

    public void setUploadUserId(Long uploadUserId)
    {
        this.uploadUserId = uploadUserId;
    }

    public Long getUploadUserId()
    {
        return uploadUserId;
    }

    public void setUploadUserName(String uploadUserName)
    {
        this.uploadUserName = uploadUserName;
    }

    public String getUploadUserName()
    {
        return uploadUserName;
    }

    public void setDownloadCount(Integer downloadCount)
    {
        this.downloadCount = downloadCount;
    }

    public Integer getDownloadCount()
    {
        return downloadCount;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("attachmentId", getAttachmentId())
            .append("fileName", getFileName())
            .append("fileOriginalName", getFileOriginalName())
            .append("filePath", getFilePath())
            .append("fileSize", getFileSize())
            .append("fileType", getFileType())
            .append("fileUrl", getFileUrl())
            .append("businessType", getBusinessType())
            .append("businessId", getBusinessId())
            .append("documentType", getDocumentType())
            .append("uploadUserId", getUploadUserId())
            .append("uploadUserName", getUploadUserName())
            .append("downloadCount", getDownloadCount())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
