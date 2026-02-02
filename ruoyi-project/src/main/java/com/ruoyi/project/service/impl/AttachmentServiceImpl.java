package com.ruoyi.project.service.impl;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.project.mapper.AttachmentMapper;
import com.ruoyi.project.mapper.AttachmentLogMapper;
import com.ruoyi.project.domain.Attachment;
import com.ruoyi.project.domain.AttachmentLog;
import com.ruoyi.project.service.IAttachmentService;

/**
 * 附件管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-01
 */
@Service
public class AttachmentServiceImpl implements IAttachmentService
{
    @Autowired
    private AttachmentMapper attachmentMapper;

    @Autowired
    private AttachmentLogMapper attachmentLogMapper;

    @Value("${attachment.max-file-size:50}")
    private Integer maxFileSize;

    /**
     * 查询附件管理列表
     *
     * @param attachment 附件管理
     * @return 附件管理
     */
    @Override
    public List<Attachment> selectAttachmentList(Attachment attachment)
    {
        return attachmentMapper.selectAttachmentList(attachment);
    }

    /**
     * 查询附件管理
     *
     * @param attachmentId 附件管理主键
     * @return 附件管理
     */
    @Override
    public Attachment selectAttachmentByAttachmentId(Long attachmentId)
    {
        return attachmentMapper.selectAttachmentByAttachmentId(attachmentId);
    }

    /**
     * 上传附件
     *
     * @param file 文件
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param documentType 文档类型
     * @return 结果
     */
    @Override
    @Transactional
    public Attachment uploadAttachment(MultipartFile file, String businessType, Long businessId, String documentType) throws Exception
    {
        // 检查文件大小
        long fileSizeInMB = file.getSize() / (1024 * 1024);
        if (fileSizeInMB > maxFileSize)
        {
            throw new Exception("文件大小超过限制，最大允许" + maxFileSize + "MB");
        }

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 验证文件类型
        String[] allowedExtensions = new String[]{".doc", ".docx", ".pdf", ".xls", ".xlsx", ".txt", ".png", ".jpg", ".gz", ".zip", ".csv"};
        boolean isAllowed = false;
        for (String ext : allowedExtensions)
        {
            if (extension.equalsIgnoreCase(ext))
            {
                isAllowed = true;
                break;
            }
        }
        if (!isAllowed)
        {
            throw new Exception("不支持的文件类型：" + extension);
        }

        // 上传文件（使用RuoYi自带的文件上传工具）
        String filePath = FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file, allowedExtensions);

        // 生成文件名（从返回的路径中提取）
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);

        // 保存附件信息
        Attachment attachment = new Attachment();
        attachment.setFileName(fileName);
        attachment.setFileOriginalName(originalFilename);
        attachment.setFilePath(filePath);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(extension.substring(1));
        attachment.setFileUrl(filePath);
        attachment.setBusinessType(businessType);
        attachment.setBusinessId(businessId);
        attachment.setDocumentType(documentType);
        attachment.setUploadUserId(SecurityUtils.getUserId());
        attachment.setUploadUserName(SecurityUtils.getUsername());
        attachment.setDownloadCount(0);
        attachment.setDelFlag("0");
        attachment.setCreateBy(SecurityUtils.getUsername());
        attachment.setCreateTime(DateUtils.getNowDate());

        attachmentMapper.insertAttachment(attachment);

        // 记录操作日志
        saveAttachmentLog(attachment, "upload");

        return attachment;
    }

    /**
     * 下载附件
     *
     * @param attachmentId 附件ID
     * @return 附件信息
     */
    @Override
    @Transactional
    public Attachment downloadAttachment(Long attachmentId)
    {
        Attachment attachment = attachmentMapper.selectAttachmentByAttachmentId(attachmentId);
        if (attachment != null)
        {
            // 更新下载次数
            attachment.setDownloadCount(attachment.getDownloadCount() + 1);
            attachmentMapper.updateAttachment(attachment);

            // 记录操作日志
            saveAttachmentLog(attachment, "download");
        }
        return attachment;
    }

    /**
     * 删除附件
     *
     * @param attachmentId 附件管理主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteAttachmentByAttachmentId(Long attachmentId)
    {
        Attachment attachment = attachmentMapper.selectAttachmentByAttachmentId(attachmentId);
        if (attachment != null)
        {
            // 记录操作日志
            saveAttachmentLog(attachment, "delete");

            // 逻辑删除
            return attachmentMapper.deleteAttachmentByAttachmentId(attachmentId);
        }
        return 0;
    }

    /**
     * 查询附件操作日志列表
     *
     * @param attachmentLog 附件操作日志
     * @return 附件操作日志集合
     */
    @Override
    public List<AttachmentLog> selectAttachmentLogList(AttachmentLog attachmentLog)
    {
        return attachmentLogMapper.selectAttachmentLogList(attachmentLog);
    }

    /**
     * 保存附件操作日志
     *
     * @param attachment 附件对象
     * @param operationType 操作类型
     */
    private void saveAttachmentLog(Attachment attachment, String operationType)
    {
        AttachmentLog log = new AttachmentLog();
        log.setAttachmentId(attachment.getAttachmentId());
        log.setFileOriginalName(attachment.getFileOriginalName());
        log.setBusinessType(attachment.getBusinessType());
        log.setBusinessId(attachment.getBusinessId());
        log.setDocumentType(attachment.getDocumentType());
        log.setOperationType(operationType);

        // 构建操作描述
        String operationTypeDesc = "";
        switch (operationType) {
            case "upload":
                operationTypeDesc = "上传附件";
                break;
            case "download":
                operationTypeDesc = "下载附件";
                break;
            case "delete":
                operationTypeDesc = "删除附件";
                break;
            case "view":
                operationTypeDesc = "查看附件";
                break;
            default:
                operationTypeDesc = operationType;
        }
        log.setOperationDesc("【" + operationTypeDesc + "】：" + attachment.getFileOriginalName());

        log.setOperationUserId(SecurityUtils.getUserId());
        log.setOperationUserName(SecurityUtils.getUsername());
        //log.setOperationIp(ServletUtils.getClientIP());
        log.setOperationTime(new Date());
        attachmentLogMapper.insertAttachmentLog(log);
    }
}
