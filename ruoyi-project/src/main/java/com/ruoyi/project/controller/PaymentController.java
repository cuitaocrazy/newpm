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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.Payment;
import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.service.IPaymentService;
import com.ruoyi.project.service.IContractService;
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

    @Autowired
    private IContractService contractService;

    /**
     * 查询款项管理列表
     */
    @PreAuthorize("@ss.hasAnyPermi('project:payment:list,project:project:query,project:contract:query,project:contract:add,project:payment:add,project:payment:edit')")
    @GetMapping("/list")
    public TableDataInfo list(Payment payment)
    {
        startPage();
        List<Payment> list = paymentService.selectPaymentList(payment);
        return getDataTable(list);
    }

    /**
     * 查询合同及其付款里程碑列表（用于付款里程碑查询页面）
     */
    @PreAuthorize("@ss.hasPermi('project:payment:list')")
    @GetMapping("/listWithContracts")
    public TableDataInfo listWithContracts(Contract contract,
            @RequestParam(required = false) String actualPaymentDateStart,
            @RequestParam(required = false) String actualPaymentDateEnd)
    {
        if (actualPaymentDateStart != null) {
            contract.getParams().put("actualPaymentDateStart", actualPaymentDateStart);
        }
        if (actualPaymentDateEnd != null) {
            contract.getParams().put("actualPaymentDateEnd", actualPaymentDateEnd);
        }
        startPage();
        List<Contract> list = contractService.selectContractWithPaymentsList(contract);
        return getDataTable(list);
    }

    /**
     * 统计付款里程碑总金额（用于付款里程碑查询页面）
     */
    @PreAuthorize("@ss.hasPermi('project:payment:list')")
    @GetMapping("/sumPaymentAmount")
    public AjaxResult sumPaymentAmount(Contract contract,
            @RequestParam(required = false) String actualPaymentDateStart,
            @RequestParam(required = false) String actualPaymentDateEnd)
    {
        if (actualPaymentDateStart != null) {
            contract.getParams().put("actualPaymentDateStart", actualPaymentDateStart);
        }
        if (actualPaymentDateEnd != null) {
            contract.getParams().put("actualPaymentDateEnd", actualPaymentDateEnd);
        }
        return success(contractService.sumPaymentAmount(contract));
    }

    /**
     * 导出款项管理列表
     */
    @PreAuthorize("@ss.hasPermi('project:payment:export')")
    @Log(title = "款项管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Contract contract,
            @RequestParam(required = false) String actualPaymentDateStart,
            @RequestParam(required = false) String actualPaymentDateEnd)
    {
        if (actualPaymentDateStart != null) {
            contract.getParams().put("actualPaymentDateStart", actualPaymentDateStart);
        }
        if (actualPaymentDateEnd != null) {
            contract.getParams().put("actualPaymentDateEnd", actualPaymentDateEnd);
        }
        // 不分页，全量查询（与列表页数据源一致）
        List<Contract> contractList = contractService.selectContractWithPaymentsList(contract);
        // 平铺：每个付款里程碑一行（无里程碑的合同跳过）
        List<Payment> list = new java.util.ArrayList<>();
        for (Contract c : contractList) {
            if (c.getPaymentList() == null || c.getPaymentList().isEmpty()) continue;
            for (Payment p : c.getPaymentList()) {
                p.setContractName(c.getContractName());
                p.setContractCode(c.getContractCode());
                p.setContractStatus(c.getContractStatus());
                p.setCustomerName(c.getCustomerName());
                p.setContractAmount(c.getContractAmount());
                p.setContractSignDate(c.getContractSignDate());
                p.setFreeMaintenancePeriod(c.getFreeMaintenancePeriod());
                p.setDeptName(c.getDeptName());
                list.add(p);
            }
        }
        ExcelUtil<Payment> util = new ExcelUtil<Payment>(Payment.class);
        util.exportExcel(response, list, "付款里程碑数据");
    }

    /**
     * 获取款项管理详细信息
     */
    @PreAuthorize("@ss.hasAnyPermi('project:payment:query,project:payment:edit,project:payment:attachment')")
    @GetMapping(value = "/{paymentId:\\d+}")
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
	@DeleteMapping("/{paymentIds:\\d+}")
    public AjaxResult remove(@PathVariable Long[] paymentIds)
    {
        return toAjax(paymentService.deletePaymentByPaymentIds(paymentIds));
    }

    /**
     * 检查付款里程碑是否有附件
     */
    @PreAuthorize("@ss.hasAnyPermi('project:payment:query,project:payment:list')")
    @GetMapping("/checkAttachments/{paymentId:\\d+}")
    public AjaxResult checkAttachments(@PathVariable Long paymentId)
    {
        int count = paymentService.countAttachments(paymentId);
        return success(count);
    }
}
