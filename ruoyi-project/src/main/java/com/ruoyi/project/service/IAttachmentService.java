package com.ruoyi.project.service;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.project.domain.Attachment;
import com.ruoyi.project.domain.AttachmentLog;

/**
 * 附件Service接口
 *
 * @author ruoyi
 * @date 2026-02-03
 */
public interface IAttachmentService
{
    /**
     * 查询附件
     *
     * @param attachmentId 附件主键
     * @return 附件
     */
    public Attachment selectAttachmentByAttachmentId(Long attachmentId);

    /**
     * 查询附件列表
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 附件集合
     */
    public List<Attachment> selectAttachmentList(String businessType, Long businessId);

    /**
     * 上传附件
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param documentType 文档类型
     * @param file 文件
     * @param fileDescription 文件描述
     * @return 结果
     */
    public AjaxResult uploadAttachment(String businessType, Long businessId, String documentType,
                                       MultipartFile file, String fileDescription);

    /**
     * 下载附件
     *
     * @param attachmentId 附件主键
     * @param response HTTP响应
     */
    public void downloadAttachment(Long attachmentId, HttpServletResponse response);

    /**
     * 删除附件
     *
     * @param attachmentId 附件主键
     * @return 结果
     */
    public int deleteAttachment(Long attachmentId);

    /**
     * 查询附件操作日志列表
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 附件操作日志集合
     */
    public List<AttachmentLog> selectAttachmentLogList(String businessType, Long businessId);
}
