package com.ruoyi.project.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.service.IContractService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 合同管理Controller
 *
 * @author ruoyi
 * @date 2026-02-03
 */
@RestController
@RequestMapping("/project/contract")
public class ContractController extends BaseController
{
    @Autowired
    private IContractService contractService;

    /**
     * 查询合同管理列表
     */
    @PreAuthorize("@ss.hasPermi('project:contract:list')")
    @GetMapping("/list")
    public TableDataInfo list(Contract contract)
    {
        startPage();
        List<Contract> list = contractService.selectContractList(contract);
        TableDataInfo dataInfo = getDataTable(list);

        // 添加金额总计
        Map<String, BigDecimal> summary = contractService.selectContractSummary(contract);
        dataInfo.put("summary", summary);

        return dataInfo;
    }

    /**
     * 导出合同管理列表
     */
    @PreAuthorize("@ss.hasPermi('project:contract:export')")
    @Log(title = "合同管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Contract contract)
    {
        List<Contract> list = contractService.selectContractList(contract);
        ExcelUtil<Contract> util = new ExcelUtil<Contract>(Contract.class);
        util.exportExcel(response, list, "合同管理数据");
    }

    /**
     * 获取合同管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:contract:query')")
    @GetMapping(value = "/{contractId}")
    public AjaxResult getInfo(@PathVariable("contractId") Long contractId)
    {
        return success(contractService.selectContractByContractId(contractId));
    }

    /**
     * 新增合同管理
     */
    @PreAuthorize("@ss.hasPermi('project:contract:add')")
    @Log(title = "合同管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Contract contract)
    {
        return toAjax(contractService.insertContract(contract));
    }

    /**
     * 修改合同管理
     */
    @PreAuthorize("@ss.hasPermi('project:contract:edit')")
    @Log(title = "合同管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Contract contract)
    {
        return toAjax(contractService.updateContract(contract));
    }

    /**
     * 删除合同管理
     */
    @PreAuthorize("@ss.hasPermi('project:contract:remove')")
    @Log(title = "合同管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{contractIds}")
    public AjaxResult remove(@PathVariable Long[] contractIds)
    {
        return toAjax(contractService.deleteContractByContractIds(contractIds));
    }

    /**
     * 搜索合同（用于下拉选择）
     */
    @PreAuthorize("@ss.hasPermi('project:contract:query')")
    @GetMapping("/search")
    public AjaxResult search(String keyword)
    {
        List<Contract> list = contractService.searchContracts(keyword);
        return success(list);
    }

    /**
     * 检查合同名称是否唯一
     */
    @PreAuthorize("@ss.hasPermi('project:contract:query')")
    @GetMapping("/checkContractNameUnique")
    public AjaxResult checkContractNameUnique(String contractName, Long contractId)
    {
        boolean isUnique = contractService.checkContractNameUnique(contractName, contractId);
        return success(isUnique);
    }
}
