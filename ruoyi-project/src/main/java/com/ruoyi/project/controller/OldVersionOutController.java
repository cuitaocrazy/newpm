package com.ruoyi.project.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.project.domain.OldVersionOut;
import com.ruoyi.project.service.IOldVersionOutService;

/**
 * 出入库版本旧数据查询 Controller（纯只读：列表 + 3 个下拉）
 *
 * @author yadapm-migrate
 */
@RestController
@RequestMapping("/project/oldVersionOut")
public class OldVersionOutController extends BaseController
{
    @Autowired
    private IOldVersionOutService oldVersionOutService;

    /**
     * 查询旧版本归档列表
     */
    @PreAuthorize("@ss.hasPermi('project:oldVersionOut:list')")
    @GetMapping("/list")
    public TableDataInfo list(OldVersionOut oldVersionOut)
    {
        startPage();
        List<OldVersionOut> list = oldVersionOutService.selectOldVersionOutList(oldVersionOut);
        return getDataTable(list);
    }

    /**
     * 投产批次号下拉选项（distinct）
     */
    @PreAuthorize("@ss.hasPermi('project:oldVersionOut:list')")
    @GetMapping("/proBatchNoOptions")
    public AjaxResult proBatchNoOptions()
    {
        return success(oldVersionOutService.selectProBatchNoOptions());
    }

    /**
     * 子产品下拉选项（distinct）
     */
    @PreAuthorize("@ss.hasPermi('project:oldVersionOut:list')")
    @GetMapping("/productOptions")
    public AjaxResult productOptions()
    {
        return success(oldVersionOutService.selectProductOptions());
    }

    /**
     * 版本类型下拉选项（distinct）
     */
    @PreAuthorize("@ss.hasPermi('project:oldVersionOut:list')")
    @GetMapping("/versionTypeOptions")
    public AjaxResult versionTypeOptions()
    {
        return success(oldVersionOutService.selectVersionTypeOptions());
    }
}
