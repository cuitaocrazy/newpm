package com.ruoyi.project.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.service.IProjectEmailService;
import com.ruoyi.system.domain.SysUser;
import com.ruoyi.system.mapper.SysUserMapper;

/**
 * 项目邮件通知Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-08
 */
@Service
public class ProjectEmailServiceImpl implements IProjectEmailService
{
    private static final Logger log = LoggerFactory.getLogger(ProjectEmailServiceImpl.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * 发送审核结果通知邮件（异步）
     *
     * @param projectId 项目ID
     * @param approvalStatus 审核状态（1通过/2拒绝）
     * @param approvalReason 审核意见
     */
    @Override
    @Async
    public void sendApprovalNotification(Long projectId, String approvalStatus, String approvalReason)
    {
        try
        {
            // 检查邮件服务是否配置
            if (mailSender == null || StringUtils.isEmpty(fromEmail))
            {
                log.warn("邮件服务未配置，跳过发送审核通知邮件");
                return;
            }

            // 查询项目信息
            Project project = projectMapper.selectProjectByProjectId(projectId);
            if (project == null)
            {
                log.error("项目不存在，无法发送审核通知邮件，projectId: {}", projectId);
                return;
            }

            // 查询项目创建人信息
            SysUser creator = userMapper.selectUserByUserName(project.getCreateBy());
            if (creator == null || StringUtils.isEmpty(creator.getEmail()))
            {
                log.warn("项目创建人邮箱为空，无法发送审核通知邮件，projectId: {}, createBy: {}",
                    projectId, project.getCreateBy());
                return;
            }

            // 构建邮件内容
            String statusText = "1".equals(approvalStatus) ? "通过" : "拒绝";
            String subject = String.format("【项目立项审核】%s - 审核%s", project.getProjectName(), statusText);

            StringBuilder content = new StringBuilder();
            content.append("尊敬的 ").append(creator.getNickName()).append("：\n\n");
            content.append("您提交的项目立项申请已完成审核。\n\n");
            content.append("项目名称：").append(project.getProjectName()).append("\n");
            content.append("项目编码：").append(project.getProjectCode()).append("\n");
            content.append("审核结果：").append(statusText).append("\n");

            if (StringUtils.isNotEmpty(approvalReason))
            {
                content.append("审核意见：").append(approvalReason).append("\n");
            }

            content.append("\n请登录系统查看详情。\n\n");
            content.append("此邮件由系统自动发送，请勿回复。");

            // 发送邮件
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(creator.getEmail());
            message.setSubject(subject);
            message.setText(content.toString());

            mailSender.send(message);
            log.info("审核通知邮件发送成功，projectId: {}, to: {}", projectId, creator.getEmail());
        }
        catch (Exception e)
        {
            log.error("发送审核通知邮件失败，projectId: {}", projectId, e);
        }
    }
}
