package com.ruoyi.project.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.Attachment;
import com.ruoyi.project.domain.AttachmentLog;
import com.ruoyi.project.service.IAttachmentService;

/**
 * 附件管理Controller
 *
 * @author ruoyi
 * @date 2026-02-01
 */
@RestController
@RequestMapping("/project/attachment")
public class AttachmentController extends BaseController
{
    @Autowired
    private IAttachmentService attachmentService;

    /**
     * 查询附件管理列表
     */
    @PreAuthorize("@ss.hasPermi('project:attachment:list')")
    @GetMapping("/list")
    public TableDataInfo list(Attachment attachment)
    {
        startPage();
        List<Attachment> list = attachmentService.selectAttachmentList(attachment);
        return getDataTable(list);
    }

    /**
     * 上传附件
     */
    @PreAuthorize("@ss.hasPermi('project:attachment:upload')")
    @Log(title = "附件管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam("businessType") String businessType,
                             @RequestParam("businessId") Long businessId,
                             @RequestParam(value = "documentType", required = false) String documentType)
    {
        try
        {
            Attachment attachment = attachmentService.uploadAttachment(file, businessType, businessId, documentType);
            return success(attachment);
        }
        catch (Exception e)
        {
            return error(e.getMessage());
        }
    }

    /**
     * 下载附件
     */
    @PreAuthorize("@ss.hasPermi('project:attachment:download')")
    @Log(title = "附件管理", businessType = BusinessType.OTHER)
    @GetMapping("/download/{attachmentId}")
    public void download(@PathVariable Long attachmentId, HttpServletResponse response)
    {
        try
        {
            Attachment attachment = attachmentService.downloadAttachment(attachmentId);
            if (attachment == null)
            {
                return;
            }

            String filePath = RuoYiConfig.getProfile() + attachment.getFilePath();
            File file = new File(filePath);
            if (!file.exists())
            {
                return;
            }

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(attachment.getFileOriginalName(), "UTF-8"));
            response.setContentLengthLong(file.length());

            try (InputStream inputStream = new FileInputStream(file);
                 OutputStream outputStream = response.getOutputStream())
            {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1)
                {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        }
        catch (Exception e)
        {
            logger.error("下载附件失败", e);
        }
    }

    /**
     * 删除附件
     */
    @PreAuthorize("@ss.hasPermi('project:attachment:remove')")
    @Log(title = "附件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{attachmentId}")
    public AjaxResult remove(@PathVariable Long attachmentId)
    {
        return toAjax(attachmentService.deleteAttachmentByAttachmentId(attachmentId));
    }

    /**
     * 查询附件操作日志列表
     */
    @PreAuthorize("@ss.hasPermi('project:attachment:log')")
    @GetMapping("/log/{attachmentId}")
    public TableDataInfo log(@PathVariable Long attachmentId)
    {
        startPage();
        AttachmentLog attachmentLog = new AttachmentLog();
        attachmentLog.setAttachmentId(attachmentId);
        List<AttachmentLog> list = attachmentService.selectAttachmentLogList(attachmentLog);
        return getDataTable(list);
    }
}
