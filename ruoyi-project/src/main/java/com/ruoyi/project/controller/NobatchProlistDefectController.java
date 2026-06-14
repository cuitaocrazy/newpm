package com.ruoyi.project.controller;

import java.util.List;
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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.project.domain.NobatchProlistDefect;
import com.ruoyi.project.service.INobatchProlistDefectService;

/**
 * 非批次任务问题单及缺陷 Controller
 *
 * @author yadapm-migrate
 */
@RestController
@RequestMapping("/project/nobatchProlist")
public class NobatchProlistDefectController extends BaseController
{
    @Autowired
    private INobatchProlistDefectService nobatchProlistDefectService;

    @PreAuthorize("@ss.hasPermi('project:nobatchProlist:list')")
    @GetMapping("/list")
    public TableDataInfo list(NobatchProlistDefect nobatchProlistDefect)
    {
        startPage();
        return getDataTable(nobatchProlistDefectService.selectNobatchProlistDefectList(nobatchProlistDefect));
    }

    @PreAuthorize("@ss.hasPermi('project:nobatchProlist:query')")
    @GetMapping("/{problemId}")
    public AjaxResult getInfo(@PathVariable("problemId") Long problemId)
    {
        return success(nobatchProlistDefectService.selectNobatchProlistDefectByProblemId(problemId));
    }

    @PreAuthorize("@ss.hasPermi('project:nobatchProlist:edit')")
    @Log(title = "非批次问题单及缺陷", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody NobatchProlistDefect nobatchProlistDefect)
    {
        return toAjax(nobatchProlistDefectService.insertNobatchProlistDefect(nobatchProlistDefect));
    }

    @PreAuthorize("@ss.hasPermi('project:nobatchProlist:edit')")
    @Log(title = "非批次问题单及缺陷", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody NobatchProlistDefect nobatchProlistDefect)
    {
        return toAjax(nobatchProlistDefectService.updateNobatchProlistDefect(nobatchProlistDefect));
    }

    @PreAuthorize("@ss.hasPermi('project:nobatchProlist:remove')")
    @Log(title = "非批次问题单及缺陷", businessType = BusinessType.DELETE)
    @DeleteMapping("/{problemIds}")
    public AjaxResult remove(@PathVariable Long[] problemIds)
    {
        return toAjax(nobatchProlistDefectService.deleteNobatchProlistDefectByProblemIds(problemIds));
    }

    @PreAuthorize("@ss.hasPermi('project:nobatchProlist:edit')")
    @Log(title = "非批次问题单及缺陷", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, NobatchProlistDefect nobatchProlistDefect)
    {
        List<NobatchProlistDefect> list = nobatchProlistDefectService.selectNobatchProlistDefectList(nobatchProlistDefect);
        ExcelUtil<NobatchProlistDefect> util = new ExcelUtil<>(NobatchProlistDefect.class);
        util.exportExcel(response, list, "非批次问题单及缺陷");
    }

    // ===== 联动端点（仅批次→计划投产日期，无任务联动） =====

    @PreAuthorize("@ss.hasPermi('project:nobatchProlist:list')")
    @GetMapping("/batchByYear")
    public AjaxResult batchByYear(@RequestParam String year)
    {
        return success(nobatchProlistDefectService.selectBatchByYear(year));
    }

    @PreAuthorize("@ss.hasPermi('project:nobatchProlist:list')")
    @GetMapping("/tcDate")
    public AjaxResult tcDate(@RequestParam Long batchId)
    {
        // success(String) 会塞 msg，用 2 参重载确保进 data
        return AjaxResult.success("查询成功", nobatchProlistDefectService.selectPlanProductionDate(batchId));
    }

    @PreAuthorize("@ss.hasPermi('project:nobatchProlist:list')")
    @GetMapping("/checkProblemNo")
    public AjaxResult checkProblemNo(@RequestParam String problemNo,
                                     @RequestParam(required = false) Long problemId)
    {
        return success(nobatchProlistDefectService.checkProblemNoUnique(problemNo, problemId));
    }
}
