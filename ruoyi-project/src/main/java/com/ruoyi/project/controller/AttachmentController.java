package com.ruoyi.project.controller;

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
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.Attachment;
import com.ruoyi.project.domain.AttachmentLog;
import com.ruoyi.project.service.IAttachmentService;

/**
 * 附件Controller
 *
 * @author ruoyi
 * @date 2026-02-03
 */
@RestController
@RequestMapping("/project/attachment")
public class AttachmentController extends BaseController
{
    @Autowired
    private IAttachmentService attachmentService;

    /**
     * 查询附件列表
     */
    @PreAuthorize("@ss.hasPermi('project:attachment:list')")
    @GetMapping("/list")
    public AjaxResult list(
            @RequestParam("businessType") String businessType,
            @RequestParam("businessId") Long businessId)
    {
        List<Attachment> list = attachmentService.selectAttachmentList(businessType, businessId);
        return success(list);
    }

    /**
     * 获取附件详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:attachment:query')")
    @GetMapping(value = "/{attachmentId}")
    public AjaxResult getInfo(@PathVariable("attachmentId") Long attachmentId)
    {
        return success(attachmentService.selectAttachmentByAttachmentId(attachmentId));
    }

    /**
     * 上传附件
     */
    @PreAuthorize("@ss.hasPermi('project:attachment:add')")
    @Log(title = "附件管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult upload(
            @RequestParam("businessType") String businessType,
            @RequestParam("businessId") Long businessId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileDescription", required = false) String fileDescription)
    {
        return attachmentService.uploadAttachment(businessType, businessId, documentType, file, fileDescription);
    }

    /**
     * 下载附件
     */
    @PreAuthorize("@ss.hasPermi('project:attachment:download')")
    @Log(title = "附件管理", businessType = BusinessType.OTHER)
    @GetMapping("/download/{attachmentId}")
    public void download(@PathVariable Long attachmentId, HttpServletResponse response)
    {
        attachmentService.downloadAttachment(attachmentId, response);
    }

    /**
     * 删除附件
     */
    @PreAuthorize("@ss.hasPermi('project:attachment:remove')")
    @Log(title = "附件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{attachmentId}")
    public AjaxResult remove(@PathVariable Long attachmentId)
    {
        return toAjax(attachmentService.deleteAttachment(attachmentId));
    }

    /**
     * 查询附件操作日志
     */
    @PreAuthorize("@ss.hasPermi('project:attachment:log')")
    @GetMapping("/log")
    public AjaxResult log(
            @RequestParam("businessType") String businessType,
            @RequestParam("businessId") Long businessId)
    {
        List<AttachmentLog> list = attachmentService.selectAttachmentLogList(businessType, businessId);
        return success(list);
    }
}
