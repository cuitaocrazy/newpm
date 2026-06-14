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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.project.domain.ProlistDefect;
import com.ruoyi.project.service.IProlistDefectService;

/**
 * 批次任务问题单及缺陷 Controller
 *
 * @author yadapm-migrate
 */
@RestController
@RequestMapping("/project/prolistDefect")
public class ProlistDefectController extends BaseController
{
    @Autowired
    private IProlistDefectService prolistDefectService;

    /** 列表 */
    @PreAuthorize("@ss.hasPermi('project:prolistDefect:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProlistDefect prolistDefect)
    {
        startPage();
        return getDataTable(prolistDefectService.selectProlistDefectList(prolistDefect));
    }

    /** 详情 */
    @PreAuthorize("@ss.hasPermi('project:prolistDefect:query')")
    @GetMapping("/{problemId}")
    public AjaxResult getInfo(@PathVariable("problemId") Long problemId)
    {
        return success(prolistDefectService.selectProlistDefectByProblemId(problemId));
    }

    /** 新增 */
    @PreAuthorize("@ss.hasPermi('project:prolistDefect:edit')")
    @Log(title = "问题单及缺陷", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProlistDefect prolistDefect)
    {
        return toAjax(prolistDefectService.insertProlistDefect(prolistDefect));
    }

    /** 修改 */
    @PreAuthorize("@ss.hasPermi('project:prolistDefect:edit')")
    @Log(title = "问题单及缺陷", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProlistDefect prolistDefect)
    {
        return toAjax(prolistDefectService.updateProlistDefect(prolistDefect));
    }

    /** 删除（软删除） */
    @PreAuthorize("@ss.hasPermi('project:prolistDefect:remove')")
    @Log(title = "问题单及缺陷", businessType = BusinessType.DELETE)
    @DeleteMapping("/{problemIds}")
    public AjaxResult remove(@PathVariable Long[] problemIds)
    {
        return toAjax(prolistDefectService.deleteProlistDefectByProblemIds(problemIds));
    }

    /** 导出 */
    @PreAuthorize("@ss.hasPermi('project:prolistDefect:edit')")
    @Log(title = "问题单及缺陷", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProlistDefect prolistDefect)
    {
        List<ProlistDefect> list = prolistDefectService.selectProlistDefectList(prolistDefect);
        ExcelUtil<ProlistDefect> util = new ExcelUtil<>(ProlistDefect.class);
        util.exportExcel(response, list, "问题单及缺陷");
    }

    // ===== 联动端点 =====

    /** 年份→批次下拉 */
    @PreAuthorize("@ss.hasPermi('project:prolistDefect:list')")
    @GetMapping("/batchByYear")
    public AjaxResult batchByYear(@RequestParam String year)
    {
        return success(prolistDefectService.selectBatchByYear(year));
    }

    /** 批次→计划投产日期 */
    @PreAuthorize("@ss.hasPermi('project:prolistDefect:list')")
    @GetMapping("/tcDate")
    public AjaxResult tcDate(@RequestParam Long batchId)
    {
        // 注意：success(String) 会把值塞进 msg 而非 data，这里用 2 参重载确保进 data
        return AjaxResult.success("查询成功", prolistDefectService.selectPlanProductionDate(batchId));
    }

    /** 年份+批次+部门→任务号下拉 */
    @PreAuthorize("@ss.hasPermi('project:prolistDefect:list')")
    @GetMapping("/taskOptions")
    public AjaxResult taskOptions(@RequestParam String productionYear,
                                  @RequestParam Long batchId,
                                  @RequestParam(required = false) Long deptId)
    {
        return success(prolistDefectService.selectTaskOptions(productionYear, batchId, deptId));
    }

    /** 任务回显（任务名/二级产品/各测试日期/排期状态） */
    @PreAuthorize("@ss.hasPermi('project:prolistDefect:list')")
    @GetMapping("/taskInfo")
    public AjaxResult taskInfo(@RequestParam Long taskId)
    {
        return success(prolistDefectService.selectTaskInfo(taskId));
    }

    /** 问题单编号查重（problemId 非空时排除自己） */
    @PreAuthorize("@ss.hasPermi('project:prolistDefect:list')")
    @GetMapping("/checkProblemNo")
    public AjaxResult checkProblemNo(@RequestParam String problemNo,
                                     @RequestParam(required = false) Long problemId)
    {
        return success(prolistDefectService.checkProblemNoUnique(problemNo, problemId));
    }
}
