package com.ruoyi.project.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.DailyReportWhitelist;
import com.ruoyi.project.service.IDailyReportWhitelistService;

/**
 * 日报白名单管理
 */
@RestController
@RequestMapping("/project/whitelist")
public class DailyReportWhitelistController extends BaseController {

    @Autowired
    private IDailyReportWhitelistService whitelistService;

    /** 白名单列表（仅 admin） */
    @PreAuthorize("@ss.hasRole('admin')")
    @GetMapping("/list")
    public TableDataInfo list(DailyReportWhitelist whitelist) {
        startPage();
        List<DailyReportWhitelist> list = whitelistService.selectWhitelistPage(whitelist);
        return getDataTable(list);
    }

    /** 添加白名单（仅 admin） */
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "日报白名单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DailyReportWhitelist whitelist) {
        return toAjax(whitelistService.addToWhitelist(whitelist));
    }

    /** 移除白名单（仅 admin） */
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "日报白名单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(whitelistService.removeFromWhitelist(id));
    }

    /** 检查当前登录用户是否在白名单中（任意登录用户可调用） */
    @GetMapping("/checkSelf")
    public AjaxResult checkSelf() {
        Long userId = SecurityUtils.getUserId();
        return success(whitelistService.isInWhitelist(userId));
    }
}
