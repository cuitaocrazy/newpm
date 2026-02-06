package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.PaymentMapper;
import com.ruoyi.project.mapper.AttachmentMapper;
import com.ruoyi.project.mapper.AttachmentLogMapper;
import com.ruoyi.project.domain.Payment;
import com.ruoyi.project.service.IPaymentService;

/**
 * 款项管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-04
 */
@Service
public class PaymentServiceImpl implements IPaymentService
{
    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private AttachmentMapper attachmentMapper;

    @Autowired
    private AttachmentLogMapper attachmentLogMapper;

    /**
     * 查询款项管理
     *
     * @param paymentId 款项管理主键
     * @return 款项管理
     */
    @Override
    public Payment selectPaymentByPaymentId(Long paymentId)
    {
        return paymentMapper.selectPaymentByPaymentId(paymentId);
    }

    /**
     * 查询款项管理列表
     *
     * @param payment 款项管理
     * @return 款项管理
     */
    @Override
    public List<Payment> selectPaymentList(Payment payment)
    {
        return paymentMapper.selectPaymentList(payment);
    }

    /**
     * 新增款项管理
     *
     * @param payment 款项管理
     * @return 结果
     */
    @Override
    public int insertPayment(Payment payment)
    {
        payment.setCreateTime(DateUtils.getNowDate());
        return paymentMapper.insertPayment(payment);
    }

    /**
     * 修改款项管理
     *
     * @param payment 款项管理
     * @return 结果
     */
    @Override
    public int updatePayment(Payment payment)
    {
        payment.setUpdateTime(DateUtils.getNowDate());
        return paymentMapper.updatePayment(payment);
    }

    /**
     * 批量删除款项管理
     *
     * @param paymentIds 需要删除的款项管理主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deletePaymentByPaymentIds(Long[] paymentIds)
    {
        for (Long paymentId : paymentIds)
        {
            // 删除关联的附件
            attachmentMapper.deleteAttachmentByBusinessId("payment", paymentId);
        }
        return paymentMapper.deletePaymentByPaymentIds(paymentIds);
    }

    /**
     * 删除款项管理信息
     *
     * @param paymentId 款项管理主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deletePaymentByPaymentId(Long paymentId)
    {
        // 删除关联的附件
        attachmentMapper.deleteAttachmentByBusinessId("payment", paymentId);
        return paymentMapper.deletePaymentByPaymentId(paymentId);
    }

    /**
     * 统计付款里程碑的附件数量
     *
     * @param paymentId 款项管理主键
     * @return 附件数量
     */
    @Override
    public int countAttachments(Long paymentId)
    {
        return attachmentMapper.countByBusinessTypeAndId("payment", paymentId);
    }
}
