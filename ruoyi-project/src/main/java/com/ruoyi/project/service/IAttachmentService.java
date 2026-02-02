package com.ruoyi.project.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.project.domain.Attachment;
import com.ruoyi.project.domain.AttachmentLog;

/**
 * 附件管理Service接口
 *
 * @author ruoyi
 * @date 2026-02-01
 */
public interface IAttachmentService
{
    /**
     * 查询附件管理列表
     *
     * @param attachment 附件管理
     * @return 附件管理集合
     */
    public List<Attachment> selectAttachmentList(Attachment attachment);

    /**
     * 查询附件管理
     *
     * @param attachmentId 附件管理主键
     * @return 附件管理
     */
    public Attachment selectAttachmentByAttachmentId(Long attachmentId);

    /**
     * 上传附件
     *
     * @param file 文件
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param documentType 文档类型
     * @return 结果
     */
    public Attachment uploadAttachment(MultipartFile file, String businessType, Long businessId, String documentType) throws Exception;

    /**
     * 下载附件
     *
     * @param attachmentId 附件ID
     * @return 附件信息
     */
    public Attachment downloadAttachment(Long attachmentId);

    /**
     * 删除附件
     *
     * @param attachmentId 附件管理主键
     * @return 结果
     */
    public int deleteAttachmentByAttachmentId(Long attachmentId);

    /**
     * 查询附件操作日志列表
     *
     * @param attachmentLog 附件操作日志
     * @return 附件操作日志集合
     */
    public List<AttachmentLog> selectAttachmentLogList(AttachmentLog attachmentLog);
}
