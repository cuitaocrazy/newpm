package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 款项管理Mapper接口
 * 
 * @author ruoyi
 * @date 2026-02-04
 */
@Mapper
public interface PaymentMapper 
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
     * 删除款项管理
     * 
     * @param paymentId 款项管理主键
     * @return 结果
     */
    public int deletePaymentByPaymentId(Long paymentId);

    /**
     * 批量删除款项管理
     *
     * @param paymentIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePaymentByPaymentIds(Long[] paymentIds);

    /**
     * 统计合同下的款项数量
     *
     * @param contractId 合同ID
     * @return 款项数量
     */
    public int countPaymentByContractId(Long contractId);
}
