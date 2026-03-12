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
import com.ruoyi.project.domain.ProductionBatch;
import com.ruoyi.project.service.IProductionBatchService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 投产批次管理Controller
 * 
 * @author ruoyi
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/project/productionBatch")
public class ProductionBatchController extends BaseController
{
    @Autowired
    private IProductionBatchService productionBatchService;

    /**
     * 查询投产批次管理列表
     */
    @PreAuthorize("@ss.hasPermi('project:productionBatch:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProductionBatch productionBatch)
    {
        startPage();
        List<ProductionBatch> list = productionBatchService.selectProductionBatchList(productionBatch);
        return getDataTable(list);
    }

    /**
     * 导出投产批次管理列表
     */
    @PreAuthorize("@ss.hasPermi('project:productionBatch:export')")
    @Log(title = "投产批次管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProductionBatch productionBatch)
    {
        List<ProductionBatch> list = productionBatchService.selectProductionBatchList(productionBatch);
        ExcelUtil<ProductionBatch> util = new ExcelUtil<ProductionBatch>(ProductionBatch.class);
        util.exportExcel(response, list, "投产批次管理数据");
    }

    /**
     * 获取投产批次管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:productionBatch:query')")
    @GetMapping(value = "/{batchId}")
    public AjaxResult getInfo(@PathVariable("batchId") Long batchId)
    {
        return success(productionBatchService.selectProductionBatchByBatchId(batchId));
    }

    /**
     * 新增投产批次管理
     */
    @PreAuthorize("@ss.hasPermi('project:productionBatch:add')")
    @Log(title = "投产批次管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProductionBatch productionBatch)
    {
        return toAjax(productionBatchService.insertProductionBatch(productionBatch));
    }

    /**
     * 修改投产批次管理
     */
    @PreAuthorize("@ss.hasPermi('project:productionBatch:edit')")
    @Log(title = "投产批次管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProductionBatch productionBatch)
    {
        return toAjax(productionBatchService.updateProductionBatch(productionBatch));
    }

    /**
     * 删除投产批次管理
     */
    @PreAuthorize("@ss.hasPermi('project:productionBatch:remove')")
    @Log(title = "投产批次管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{batchIds}")
    public AjaxResult remove(@PathVariable Long[] batchIds)
    {
        return toAjax(productionBatchService.deleteProductionBatchByBatchIds(batchIds));
    }

    /**
     * 获取批次号下拉选项
     */
    @PreAuthorize("@ss.hasPermi('project:productionBatch:list')")
    @GetMapping("/batchNoOptions")
    public AjaxResult batchNoOptions()
    {
        return success(productionBatchService.selectBatchNoOptions());
    }

    /**
     * 按投产年份查询批次列表（用于任务管理联动下拉）
     */
    @PreAuthorize("@ss.hasAnyPermi('project:task:list,project:task:add')")
    @GetMapping("/byYear")
    public AjaxResult byYear(@RequestParam String productionYear)
    {
        return success(productionBatchService.selectByYear(productionYear));
    }
}
