package com.ruoyi.project.service.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.PaymentMapper;
import com.ruoyi.project.mapper.AttachmentMapper;
import com.ruoyi.project.mapper.AttachmentLogMapper;
import com.ruoyi.project.domain.Payment;
import com.ruoyi.project.domain.Contract;
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
    @DataScope(deptAlias = "d", userAlias = "u1")
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
        String username = com.ruoyi.common.utils.SecurityUtils.getUsername();
        java.util.Date now = DateUtils.getNowDate();
        payment.setCreateBy(username);
        payment.setCreateTime(now);
        payment.setUpdateBy(username);
        payment.setUpdateTime(now);
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
        payment.setUpdateBy(com.ruoyi.common.utils.SecurityUtils.getUsername());
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

    @Override
    public List<String> searchPaymentMethodNames(String keyword)
    {
        return paymentMapper.searchPaymentMethodNames(keyword);
    }

    /**
     * 导出付款里程碑列表
     *
     * @param response HTTP响应
     * @param contractList 合同及付款里程碑列表
     */
    @Override
    public void exportPaymentList(HttpServletResponse response, List<Contract> contractList)
    {
        // 将主子表数据展开为平铺列表
        List<Payment> exportList = new ArrayList<>();

        for (Contract contract : contractList) {
            // 只导出有付款里程碑的合同
            if (contract.getPaymentList() != null && !contract.getPaymentList().isEmpty()) {
                for (Payment payment : contract.getPaymentList()) {
                    // 检查 paymentId 是否有效（过滤掉空的付款里程碑）
                    if (payment.getPaymentId() != null) {
                        // 设置合同名称到付款里程碑对象中
                        payment.setContractName(contract.getContractName());
                        exportList.add(payment);
                    }
                }
            }
        }

        // 生成文件名：付款里程碑_yyyyMMddHHmmss.xlsx
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileName = "付款里程碑_" + sdf.format(new java.util.Date());

        // 导出Excel
        ExcelUtil<Payment> util = new ExcelUtil<Payment>(Payment.class);
        util.exportExcel(response, exportList, fileName);
    }

    @Override
    public void enrichDeptPathForExport(List<Payment> list)
    {
        if (list == null || list.isEmpty()) return;
        // 查询所有部门，构建 deptId -> {deptName, ancestors} 映射
        List<Map<String, Object>> allDepts = paymentMapper.selectAllDeptsForPath();
        Map<Long, Map<String, Object>> deptMapById = new HashMap<>();
        for (Map<String, Object> dept : allDepts) {
            Object id = dept.get("deptId");
            if (id != null) {
                deptMapById.put(Long.parseLong(id.toString()), dept);
            }
        }
        for (Payment p : list) {
            if (p.getDeptId() == null) continue;
            Map<String, Object> dept = deptMapById.get(p.getDeptId());
            if (dept == null) continue;
            String ancestors = (String) dept.get("ancestors");
            List<Long> fullPath = new ArrayList<>();
            if (ancestors != null && !ancestors.isEmpty()) {
                for (String anc : ancestors.split(",")) {
                    try { fullPath.add(Long.parseLong(anc.trim())); } catch (NumberFormatException ignored) {}
                }
            }
            fullPath.add(p.getDeptId());
            // 跳过根节点和一级节点
            List<Long> displayIds = fullPath.size() > 2 ? fullPath.subList(2, fullPath.size()) : fullPath;
            String[] levels = new String[displayIds.size()];
            for (int i = 0; i < displayIds.size(); i++) {
                Map<String, Object> d = deptMapById.get(displayIds.get(i));
                levels[i] = d != null ? d.get("deptName").toString() : String.valueOf(displayIds.get(i));
            }
            if (levels.length > 0) p.setDeptOrgLevel2(levels[0]);
            if (levels.length > 1) p.setDeptOrgLevel3(levels[1]);
            if (levels.length > 2) p.setDeptOrgLevel4(levels[2]);
            if (levels.length > 3) p.setDeptOrgLevel5(levels[3]);
            if (levels.length > 4) p.setDeptOrgLevel6(levels[4]);
        }
    }
}
