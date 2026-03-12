package com.ruoyi.project.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.Task;
import com.ruoyi.project.service.ITaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理 Controller
 * URL 前缀：/project/task
 */
@RestController
@RequestMapping("/project/task")
public class TaskController extends BaseController {

    @Autowired
    private ITaskService taskService;

    /**
     * 任务列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('project:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(Task task) {
        startPage();
        return getDataTable(taskService.selectTaskList(task));
    }

    /**
     * 任务详情
     */
    @PreAuthorize("@ss.hasAnyPermi('project:task:list,project:task:query')")
    @GetMapping("/{taskId}")
    public AjaxResult getInfo(@PathVariable Long taskId) {
        return AjaxResult.success(taskService.selectTaskById(taskId));
    }

    /**
     * 获取项目的任务轻量选项（日报填写下拉用）
     */
    @GetMapping("/options")
    public AjaxResult getTaskOptions(@RequestParam Long projectId) {
        return AjaxResult.success(taskService.selectTaskOptions(projectId));
    }

    /**
     * 批量判断哪些项目有任务（日报 hasSubProject 标记用）
     */
    @GetMapping("/projectsHasTasks")
    public AjaxResult getProjectsHasTasks(@RequestParam List<Long> projectIds) {
        return AjaxResult.success(taskService.selectProjectsHasTasks(projectIds));
    }

    /**
     * 新增任务
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('project:task:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody Task task) {
        return toAjax(taskService.insertTask(task));
    }

    /**
     * 修改任务
     */
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('project:task:edit')")
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Task task) {
        return toAjax(taskService.updateTask(task));
    }

    /**
     * 删除任务（硬删除）
     */
    @Log(title = "任务管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('project:task:remove')")
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable Long[] taskIds) {
        return toAjax(taskService.deleteTaskByIds(taskIds));
    }
}
