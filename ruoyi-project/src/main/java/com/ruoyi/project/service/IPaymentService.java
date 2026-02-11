package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.Payment;

/**
 * 款项管理Service接口
 * 
 * @author ruoyi
 * @date 2026-02-04
 */
public interface IPaymentService 
{
    /**
     * 查询款项管理
     * 
     * @param paymentId 款项管理主键
     * @return 款项管理
     */
    public Payment selectPaymentByPaymentId(Long paymentId);

    /**
     * 查询款项管理列表
     * 
     * @param payment 款项管理
     * @return 款项管理集合
     */
    public List<Payment> selectPaymentList(Payment payment);

    /**
     * 新增款项管理
     * 
     * @param payment 款项管理
     * @return 结果
     */
    public int insertPayment(Payment payment);

    /**
     * 修改款项管理
     * 
     * @param payment 款项管理
     * @return 结果
     */
    public int updatePayment(Payment payment);

    /**
     * 批量删除款项管理
     * 
     * @param paymentIds 需要删除的款项管理主键集合
     * @return 结果
     */
    public int deletePaymentByPaymentIds(Long[] paymentIds);

    /**
     * 删除款项管理信息
     *
     * @param paymentId 款项管理主键
     * @return 结果
     */
    public int deletePaymentByPaymentId(Long paymentId);

    /**
     * 统计付款里程碑的附件数量
     *
     * @param paymentId 款项管理主键
     * @return 附件数量
     */
    public int countAttachments(Long paymentId);

    /**
     * 导出付款里程碑列表
     *
     * @param response HTTP响应
     * @param contractList 合同及付款里程碑列表
     */
    public void exportPaymentList(jakarta.servlet.http.HttpServletResponse response, java.util.List<com.ruoyi.project.domain.Contract> contractList);
}
