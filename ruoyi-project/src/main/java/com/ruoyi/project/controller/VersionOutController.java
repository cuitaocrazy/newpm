package com.ruoyi.project.controller;

import java.util.List;
import java.util.Map;
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
import com.ruoyi.project.domain.SysName;
import com.ruoyi.project.domain.VersionOut;
import com.ruoyi.project.domain.VersionOutTask;
import com.ruoyi.project.service.ISysNameService;
import com.ruoyi.project.service.IVersionOutService;

/**
 * 批次版本管理（出入库版本）Controller
 *
 * @author ruoyi
 * @date 2026-06-11
 */
@RestController
@RequestMapping("/project/versionOut")
public class VersionOutController extends BaseController
{
    @Autowired
    private IVersionOutService versionOutService;

    @Autowired
    private ISysNameService sysNameService;

    /** 批次版本列表 */
    @PreAuthorize("@ss.hasPermi('project:versionOut:list')")
    @GetMapping("/list")
    public TableDataInfo list(VersionOut versionOut)
    {
        startPage();
        return getDataTable(versionOutService.selectVersionOutList(versionOut));
    }

    /** 批次版本详情 */
    @PreAuthorize("@ss.hasPermi('project:versionOut:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(versionOutService.selectVersionOutById(id));
    }

    /** 新增批次版本（服务端生成出入库版本号） */
    @PreAuthorize("@ss.hasPermi('project:versionOut:add')")
    @Log(title = "批次版本管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VersionOut versionOut)
    {
        return toAjax(versionOutService.insertVersionOut(versionOut));
    }

    /** 修改批次版本（关键字段变更则重算版本号） */
    @PreAuthorize("@ss.hasPermi('project:versionOut:edit')")
    @Log(title = "批次版本管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VersionOut versionOut)
    {
        return toAjax(versionOutService.updateVersionOut(versionOut));
    }

    /** 删除批次版本（软删除） */
    @PreAuthorize("@ss.hasPermi('project:versionOut:remove')")
    @Log(title = "批次版本管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(versionOutService.deleteVersionOutByIds(ids));
    }

    /** 导出批次版本 Excel */
    @PreAuthorize("@ss.hasPermi('project:versionOut:export')")
    @Log(title = "批次版本管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VersionOut versionOut)
    {
        List<VersionOut> list = versionOutService.selectVersionOutList(versionOut);
        ExcelUtil<VersionOut> util = new ExcelUtil<VersionOut>(VersionOut.class);
        util.exportExcel(response, list, "批次版本数据");
    }

    /** 实时生成出入库版本号（前端回填只读框） */
    @PreAuthorize("@ss.hasPermi('project:versionOut:list')")
    @PostMapping("/generateOutLibVersion")
    public AjaxResult generateOutLibVersion(@RequestBody VersionOut versionOut,
            @RequestParam(required = false) String addFlag,
            @RequestParam(required = false) String oldSubVersionCode,
            @RequestParam(required = false) String oldVersionType)
    {
        return success(versionOutService.generateOutLibVersion(versionOut, addFlag, oldSubVersionCode, oldVersionType));
    }

    // ---------- 级联/联动 ----------

    /** 年份→批次列表 */
    @PreAuthorize("@ss.hasPermi('project:versionOut:list')")
    @GetMapping("/batchByYear")
    public AjaxResult batchByYear(@RequestParam String year)
    {
        return success(versionOutService.selectBatchByYear(year));
    }

    /** 产品→子系统列表 */
    @PreAuthorize("@ss.hasPermi('project:versionOut:list')")
    @GetMapping("/sysNameByProduct")
    public AjaxResult sysNameByProduct(@RequestParam String product)
    {
        return success(sysNameService.selectSysNameByProduct(product));
    }

    /** 子系统+版本类型→升级包初级版本号候选（类型5/6） */
    @PreAuthorize("@ss.hasPermi('project:versionOut:list')")
    @GetMapping("/outVersionOptions")
    public AjaxResult outVersionOptions(@RequestParam String sysName, @RequestParam String versionType)
    {
        return success(versionOutService.selectOutVersionOptions(sysName, versionType));
    }

    /** 批次→投产日期 */
    @PreAuthorize("@ss.hasPermi('project:versionOut:list')")
    @GetMapping("/versionPDate")
    public AjaxResult versionPDate(@RequestParam Long batchId)
    {
        return success(versionOutService.selectVersionPDate(batchId));
    }

    /** 软件中心任务号→任务回显信息 */
    @PreAuthorize("@ss.hasPermi('project:versionOut:list')")
    @GetMapping("/taskInfo")
    public AjaxResult taskInfo(@RequestParam String taskNo)
    {
        VersionOutTask info = versionOutService.selectTaskInfoByDemandNo(taskNo);
        return success(info);
    }
}
