package com.ruoyi.project.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.Payment;
import com.ruoyi.project.service.IPaymentService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 款项管理Controller
 * 
 * @author ruoyi
 * @date 2026-02-04
 */
@RestController
@RequestMapping("/project/payment")
public class PaymentController extends BaseController
{
    @Autowired
    private IPaymentService paymentService;

    /**
     * 查询款项管理列表
     */
    @PreAuthorize("@ss.hasPermi('project:payment:list')")
    @GetMapping("/list")
    public TableDataInfo list(Payment payment)
    {
        startPage();
        List<Payment> list = paymentService.selectPaymentList(payment);
        return getDataTable(list);
    }

    /**
     * 导出款项管理列表
     */
    @PreAuthorize("@ss.hasPermi('project:payment:export')")
    @Log(title = "款项管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Payment payment)
    {
        List<Payment> list = paymentService.selectPaymentList(payment);
        ExcelUtil<Payment> util = new ExcelUtil<Payment>(Payment.class);
        util.exportExcel(response, list, "款项管理数据");
    }

    /**
     * 获取款项管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:payment:query')")
    @GetMapping(value = "/{paymentId}")
    public AjaxResult getInfo(@PathVariable("paymentId") Long paymentId)
    {
        return success(paymentService.selectPaymentByPaymentId(paymentId));
    }

    /**
     * 新增款项管理
     */
    @PreAuthorize("@ss.hasPermi('project:payment:add')")
    @Log(title = "款项管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Payment payment)
    {
        return toAjax(paymentService.insertPayment(payment));
    }

    /**
     * 修改款项管理
     */
    @PreAuthorize("@ss.hasPermi('project:payment:edit')")
    @Log(title = "款项管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Payment payment)
    {
        return toAjax(paymentService.updatePayment(payment));
    }

    /**
     * 删除款项管理
     */
    @PreAuthorize("@ss.hasPermi('project:payment:remove')")
    @Log(title = "款项管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{paymentIds}")
    public AjaxResult remove(@PathVariable Long[] paymentIds)
    {
        return toAjax(paymentService.deletePaymentByPaymentIds(paymentIds));
    }

    /**
     * 检查付款里程碑是否有附件
     */
    @PreAuthorize("@ss.hasPermi('project:payment:query')")
    @GetMapping("/checkAttachments/{paymentId}")
    public AjaxResult checkAttachments(@PathVariable Long paymentId)
    {
        int count = paymentService.countAttachments(paymentId);
        return success(count);
    }
}
