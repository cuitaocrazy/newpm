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
import com.ruoyi.project.domain.SecondaryRegion;
import com.ruoyi.project.service.ISecondaryRegionService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 省级区域Controller
 * 
 * @author ruoyi
 * @date 2026-02-04
 */
@RestController
@RequestMapping("/project/secondaryRegion")
public class SecondaryRegionController extends BaseController
{
    @Autowired
    private ISecondaryRegionService secondaryRegionService;

    /**
     * 查询省级区域列表
     */
    @PreAuthorize("@ss.hasPermi('project:secondaryRegion:list')")
    @GetMapping("/list")
    public TableDataInfo list(SecondaryRegion secondaryRegion)
    {
        startPage();
        List<SecondaryRegion> list = secondaryRegionService.selectSecondaryRegionList(secondaryRegion);
        return getDataTable(list);
    }

    /**
     * 导出省级区域列表
     */
    @PreAuthorize("@ss.hasPermi('project:secondaryRegion:export')")
    @Log(title = "省级区域", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SecondaryRegion secondaryRegion)
    {
        List<SecondaryRegion> list = secondaryRegionService.selectSecondaryRegionList(secondaryRegion);
        ExcelUtil<SecondaryRegion> util = new ExcelUtil<SecondaryRegion>(SecondaryRegion.class);
        util.exportExcel(response, list, "省级区域数据");
    }

    /**
     * 获取省级区域详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:secondaryRegion:query')")
    @GetMapping(value = "/{provinceId}")
    public AjaxResult getInfo(@PathVariable("provinceId") Long provinceId)
    {
        return success(secondaryRegionService.selectSecondaryRegionByProvinceId(provinceId));
    }

    /**
     * 新增省级区域
     */
    @PreAuthorize("@ss.hasPermi('project:secondaryRegion:add')")
    @Log(title = "省级区域", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SecondaryRegion secondaryRegion)
    {
        return toAjax(secondaryRegionService.insertSecondaryRegion(secondaryRegion));
    }

    /**
     * 修改省级区域
     */
    @PreAuthorize("@ss.hasPermi('project:secondaryRegion:edit')")
    @Log(title = "省级区域", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SecondaryRegion secondaryRegion)
    {
        return toAjax(secondaryRegionService.updateSecondaryRegion(secondaryRegion));
    }

    /**
     * 删除省级区域
     */
    @PreAuthorize("@ss.hasPermi('project:secondaryRegion:remove')")
    @Log(title = "省级区域", businessType = BusinessType.DELETE)
	@DeleteMapping("/{provinceIds}")
    public AjaxResult remove(@PathVariable Long[] provinceIds)
    {
        return toAjax(secondaryRegionService.deleteSecondaryRegionByProvinceIds(provinceIds));
    }

    /**
     * 根据一级区域查询省份列表
     */
    @GetMapping("/listByRegion")
    public AjaxResult listByRegion(String regionDictValue)
    {
        if (regionDictValue == null || regionDictValue.trim().isEmpty())
        {
            return success();
        }
        SecondaryRegion query = new SecondaryRegion();
        query.setRegionDictValue(regionDictValue);
        List<SecondaryRegion> list = secondaryRegionService.selectSecondaryRegionList(query);
        return success(list);
    }
}
