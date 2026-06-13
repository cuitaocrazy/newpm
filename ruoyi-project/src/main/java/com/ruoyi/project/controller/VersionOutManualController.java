package com.ruoyi.project.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.project.domain.VersionOut;
import com.ruoyi.project.service.ISysNameService;
import com.ruoyi.project.service.IVersionOutService;

/**
 * 非批次版本管理（出入库版本·手动输入）Controller
 * 复用 VersionOut 体系（manual_input='1'，任务手填），独立权限 project:versionOutManual:*。
 *
 * @author ruoyi
 * @date 2026-06-13
 */
@RestController
@RequestMapping("/project/versionOutManual")
public class VersionOutManualController extends BaseController
{
    @Autowired
    private IVersionOutService versionOutService;

    @Autowired
    private ISysNameService sysNameService;

    /** 非批次版本列表 */
    @PreAuthorize("@ss.hasPermi('project:versionOutManual:list')")
    @GetMapping("/list")
    public TableDataInfo list(VersionOut versionOut)
    {
        startPage();
        return getDataTable(versionOutService.selectVersionOutManualList(versionOut));
    }

    /** 非批次版本详情 */
    @PreAuthorize("@ss.hasPermi('project:versionOutManual:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(versionOutService.selectVersionOutById(id));
    }

    /** 新增非批次版本（生成版本号 + 手填任务存主表） */
    @PreAuthorize("@ss.hasPermi('project:versionOutManual:add')")
    @Log(title = "非批次版本管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VersionOut versionOut)
    {
        // 不用 @Validated：VersionOut 实体上的 @NotBlank 是批次专用字段(组包方式/版本状态等)，
        // 非批次没有这些字段，会被误伤。非批次必填由 service 层校验。
        return toAjax(versionOutService.insertVersionOutManual(versionOut));
    }

    /** 修改非批次版本（关键字段变更重算） */
    @PreAuthorize("@ss.hasPermi('project:versionOutManual:edit')")
    @Log(title = "非批次版本管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VersionOut versionOut)
    {
        return toAjax(versionOutService.updateVersionOutManual(versionOut));
    }

    /** 删除非批次版本（软删除，复用批次删除逻辑） */
    @PreAuthorize("@ss.hasPermi('project:versionOutManual:remove')")
    @Log(title = "非批次版本管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(versionOutService.deleteVersionOutByIds(ids));
    }

    /** 导出非批次版本 Excel */
    @PreAuthorize("@ss.hasPermi('project:versionOutManual:export')")
    @Log(title = "非批次版本管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VersionOut versionOut)
    {
        java.util.List<VersionOut> list = versionOutService.selectVersionOutManualList(versionOut);
        ExcelUtil<VersionOut> util = new ExcelUtil<VersionOut>(VersionOut.class);
        util.exportExcel(response, list, "非批次版本数据");
    }

    /** 实时生成出入库版本号 */
    @PreAuthorize("@ss.hasPermi('project:versionOutManual:list')")
    @PostMapping("/generateOutLibVersion")
    public AjaxResult generateOutLibVersion(@RequestBody VersionOut versionOut,
            @RequestParam(required = false) String addFlag,
            @RequestParam(required = false) String oldSubVersionCode,
            @RequestParam(required = false) String oldVersionType)
    {
        return success(versionOutService.generateOutLibVersion(versionOut, addFlag, oldSubVersionCode, oldVersionType));
    }

    // ---------- 级联（复用批次 service 逻辑；非批次无 taskInfo/taskOptions） ----------

    @PreAuthorize("@ss.hasPermi('project:versionOutManual:list')")
    @GetMapping("/batchByYear")
    public AjaxResult batchByYear(@RequestParam String year)
    {
        return success(versionOutService.selectBatchByYear(year));
    }

    @PreAuthorize("@ss.hasPermi('project:versionOutManual:list')")
    @GetMapping("/sysNameByProduct")
    public AjaxResult sysNameByProduct(@RequestParam String product)
    {
        return success(sysNameService.selectSysNameByProduct(product));
    }

    @PreAuthorize("@ss.hasPermi('project:versionOutManual:list')")
    @GetMapping("/outVersionOptions")
    public AjaxResult outVersionOptions(@RequestParam String sysName, @RequestParam String versionType)
    {
        return success(versionOutService.selectOutVersionOptions(sysName, versionType));
    }

    @PreAuthorize("@ss.hasPermi('project:versionOutManual:list')")
    @GetMapping("/versionPDate")
    public AjaxResult versionPDate(@RequestParam Long batchId)
    {
        return success(versionOutService.selectVersionPDate(batchId));
    }
}
