package com.ruoyi.project.service;

/**
 * 项目邮件通知Service接口
 *
 * @author ruoyi
 * @date 2026-02-08
 */
public interface IProjectEmailService
{
    /**
     * 发送审核结果通知邮件
     *
     * @param projectId 项目ID
     * @param approvalStatus 审核状态（1通过/2拒绝）
     * @param approvalReason 审核意见
     */
    public void sendApprovalNotification(Long projectId, String approvalStatus, String approvalReason);
}
