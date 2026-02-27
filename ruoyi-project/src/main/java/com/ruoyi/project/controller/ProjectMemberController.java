package com.ruoyi.project.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.ProjectMember;
import com.ruoyi.project.service.IProjectMemberService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目人员管理Controller
 *
 * @author ruoyi
 * @date 2026-02-28
 */
@RestController
@RequestMapping("/project/member")
public class ProjectMemberController extends BaseController
{
    @Autowired
    private IProjectMemberService projectMemberService;

    /**
     * 查询项目列表（带成员聚合信息）
     */
    @PreAuthorize("@ss.hasPermi('project:member:list')")
    @GetMapping("/list")
    public TableDataInfo list(String projectName, Long deptId)
    {
        startPage();
        Map<String, Object> params = new HashMap<>();
        params.put("projectName", projectName);
        params.put("deptId", deptId);
        List<Map<String, Object>> list = projectMemberService.selectProjectMemberList(params);
        return getDataTable(list);
    }

    /**
     * 查询项目成员详情
     */
    @PreAuthorize("@ss.hasPermi('project:member:query')")
    @GetMapping("/{projectId}")
    public AjaxResult getDetail(@PathVariable("projectId") Long projectId)
    {
        List<ProjectMember> members = projectMemberService.selectProjectMemberDetail(projectId);
        return success(members);
    }

    /**
     * 更新项目成员
     */
    @PreAuthorize("@ss.hasPermi('project:member:edit')")
    @Log(title = "项目人员管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> params)
    {
        Long projectId = Long.valueOf(params.get("projectId").toString());
        @SuppressWarnings("unchecked")
        List<Number> userIdList = (List<Number>) params.get("userIds");
        Long[] userIds = userIdList.stream().map(Number::longValue).toArray(Long[]::new);
        return toAjax(projectMemberService.updateProjectMembers(projectId, userIds));
    }
}
