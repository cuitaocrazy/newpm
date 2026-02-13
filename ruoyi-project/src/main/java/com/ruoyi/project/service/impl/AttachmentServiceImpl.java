package com.ruoyi.project.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.Attachment;
import com.ruoyi.project.domain.AttachmentLog;
import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.mapper.AttachmentMapper;
import com.ruoyi.project.mapper.AttachmentLogMapper;
import com.ruoyi.project.mapper.ContractMapper;
import com.ruoyi.project.service.IAttachmentService;

/**
 * 附件Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-03
 */
@Service
public class AttachmentServiceImpl implements IAttachmentService
{
    @Autowired
    private AttachmentMapper attachmentMapper;

    @Autowired
    private AttachmentLogMapper attachmentLogMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private com.ruoyi.project.mapper.ProjectMapper projectMapper;

    /** 允许的文件扩展名 */
    private static final String[] ALLOWED_EXTENSIONS = {
        ".doc", ".docx", ".xls", ".xlsx", ".pdf", ".csv",
        ".png", ".jpg", ".gif", ".txt", ".7z", ".zip", ".gz"
    };

    /** 文件大小限制：30MB */
    private static final long MAX_FILE_SIZE = 30 * 1024 * 1024;

    /**
     * 查询附件
     *
     * @param attachmentId 附件主键
     * @return 附件
     */
    @Override
    public Attachment selectAttachmentByAttachmentId(Long attachmentId)
    {
        return attachmentMapper.selectAttachmentByAttachmentId(attachmentId);
    }

    /**
     * 查询附件列表
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 附件集合
     */
    @Override
    public List<Attachment> selectAttachmentList(String businessType, Long businessId)
    {
        return attachmentMapper.selectAttachmentList(businessType, businessId);
    }

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
    @Override
    @Transactional
    public AjaxResult uploadAttachment(String businessType, Long businessId, String documentType,
                                       MultipartFile file, String fileDescription)
    {
        try
        {
            // 1. 文件格式校验
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty())
            {
                return AjaxResult.error("文件名不能为空");
            }

            String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
            if (!Arrays.asList(ALLOWED_EXTENSIONS).contains(extension))
            {
                return AjaxResult.error("不支持的文件格式，仅支持：doc、docx、xls、xlsx、pdf、csv、png、jpg、txt、7z、zip、gz");
            }

            // 2. 文件大小校验（30MB）
            if (file.getSize() > MAX_FILE_SIZE)
            {
                return AjaxResult.error("文件大小不能超过30MB");
            }

            // 3. 构建文件路径
            String basePath = RuoYiConfig.getProfile();
            String dateFolder = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String businessFolder = getBusinessFolder(businessType, businessId);

            if (businessFolder == null)
            {
                return AjaxResult.error("业务数据不存在");
            }

            String relativePath = businessFolder + File.separator + dateFolder + File.separator + fileName;
            String filePath = basePath + File.separator + relativePath;

            // 4. 保存文件
            File dest = new File(filePath);
            if (!dest.getParentFile().exists())
            {
                dest.getParentFile().mkdirs();
            }
            file.transferTo(dest);

            // 5. 保存附件记录
            Attachment attachment = new Attachment();
            attachment.setBusinessType(businessType);
            attachment.setBusinessId(businessId);
            attachment.setDocumentType(documentType);
            attachment.setFileName(fileName);
            attachment.setFilePath(relativePath);
            attachment.setFileSize(file.getSize());
            attachment.setFileType(extension);
            attachment.setFileDescription(fileDescription);
            attachment.setDelFlag("0");
            attachment.setCreateBy(SecurityUtils.getUsername());
            attachmentMapper.insertAttachment(attachment);

            // 6. 记录日志
            AttachmentLog log = new AttachmentLog();
            log.setAttachmentId(attachment.getAttachmentId());
            log.setBusinessType(businessType);
            log.setBusinessId(businessId);
            log.setOperationType("upload");
            log.setOperationDesc("【上传文件】：" + fileName);
            log.setDocumentType(documentType);
            log.setFileName(fileName);
            log.setOperatorId(SecurityUtils.getUserId());
            log.setOperatorName(SecurityUtils.getLoginUser().getUser().getNickName());
            attachmentLogMapper.insertAttachmentLog(log);

            return AjaxResult.success("上传成功", attachment);
        }
        catch (Exception e)
        {
            throw new ServiceException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 下载附件
     *
     * @param attachmentId 附件主键
     * @param response HTTP响应
     */
    @Override
    @Transactional
    public void downloadAttachment(Long attachmentId, HttpServletResponse response)
    {
        try
        {
            Attachment attachment = attachmentMapper.selectAttachmentByAttachmentId(attachmentId);
            if (attachment == null || "1".equals(attachment.getDelFlag()))
            {
                throw new ServiceException("附件不存在");
            }

            String basePath = RuoYiConfig.getProfile();
            String filePath = basePath + File.separator + attachment.getFilePath();
            File file = new File(filePath);

            if (!file.exists())
            {
                throw new ServiceException("文件不存在");
            }

            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("UTF-8");
            String encodedFileName = URLEncoder.encode(attachment.getFileName(), "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName);

            // 输出文件
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream())
            {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0)
                {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }

            // 记录下载日志
            AttachmentLog log = new AttachmentLog();
            log.setAttachmentId(attachmentId);
            log.setBusinessType(attachment.getBusinessType());
            log.setBusinessId(attachment.getBusinessId());
            log.setOperationType("download");
            log.setOperationDesc("【下载文件】：" + attachment.getFileName());
            log.setDocumentType(attachment.getDocumentType());
            log.setFileName(attachment.getFileName());
            log.setOperatorId(SecurityUtils.getUserId());
            log.setOperatorName(SecurityUtils.getLoginUser().getUser().getNickName());
            attachmentLogMapper.insertAttachmentLog(log);
        }
        catch (Exception e)
        {
            throw new ServiceException("文件下载失败：" + e.getMessage());
        }
    }

    /**
     * 删除附件
     *
     * @param attachmentId 附件主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteAttachment(Long attachmentId)
    {
        Attachment attachment = attachmentMapper.selectAttachmentByAttachmentId(attachmentId);
        if (attachment == null)
        {
            throw new ServiceException("附件不存在");
        }

        // 逻辑删除附件
        int rows = attachmentMapper.deleteAttachmentByAttachmentId(attachmentId);

        // 记录删除日志
        AttachmentLog log = new AttachmentLog();
        log.setAttachmentId(attachmentId);
        log.setBusinessType(attachment.getBusinessType());
        log.setBusinessId(attachment.getBusinessId());
        log.setOperationType("delete");
        log.setOperationDesc("【删除文件】：" + attachment.getFileName());
        log.setDocumentType(attachment.getDocumentType());
        log.setFileName(attachment.getFileName());
        log.setOperatorId(SecurityUtils.getUserId());
        log.setOperatorName(SecurityUtils.getLoginUser().getUser().getNickName());
        attachmentLogMapper.insertAttachmentLog(log);

        return rows;
    }

    /**
     * 查询附件操作日志列表
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 附件操作日志集合
     */
    @Override
    public List<AttachmentLog> selectAttachmentLogList(String businessType, Long businessId)
    {
        return attachmentLogMapper.selectAttachmentLogList(businessType, businessId);
    }

    /**
     * 根据业务类型和业务ID构建业务文件夹路径
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 业务文件夹路径
     */
    private String getBusinessFolder(String businessType, Long businessId)
    {
        if ("contract".equals(businessType))
        {
            Contract contract = contractMapper.selectContractByContractId(businessId);
            if (contract == null)
            {
                return null;
            }
            return "合同" + File.separator + businessId + "_" + contract.getContractName();
        }
        else if ("project".equals(businessType))
        {
            com.ruoyi.project.domain.Project project = projectMapper.selectProjectByProjectId(businessId);
            if (project == null)
            {
                return null;
            }
            return "项目" + File.separator + businessId + "_" + project.getProjectName();
        }
        else if ("payment".equals(businessType))
        {
            // 款项附件路径，可以根据需要扩展
            return "款项" + File.separator + businessId;
        }
        return null;
    }
}
