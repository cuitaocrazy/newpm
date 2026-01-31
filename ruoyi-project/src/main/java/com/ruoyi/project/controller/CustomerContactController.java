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
import com.ruoyi.project.domain.CustomerContact;
import com.ruoyi.project.service.ICustomerContactService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 客户联系人Controller
 * 
 * @author ruoyi
 * @date 2026-01-30
 */
@RestController
@RequestMapping("/project/contact")
public class CustomerContactController extends BaseController
{
    @Autowired
    private ICustomerContactService customerContactService;

    /**
     * 查询客户联系人列表
     */
    @PreAuthorize("@ss.hasPermi('project:contact:list')")
    @GetMapping("/list")
    public TableDataInfo list(CustomerContact customerContact)
    {
        startPage();
        List<CustomerContact> list = customerContactService.selectCustomerContactList(customerContact);
        return getDataTable(list);
    }

    /**
     * 导出客户联系人列表
     */
    @PreAuthorize("@ss.hasPermi('project:contact:export')")
    @Log(title = "客户联系人", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CustomerContact customerContact)
    {
        List<CustomerContact> list = customerContactService.selectCustomerContactList(customerContact);
        ExcelUtil<CustomerContact> util = new ExcelUtil<CustomerContact>(CustomerContact.class);
        util.exportExcel(response, list, "客户联系人数据");
    }

    /**
     * 获取客户联系人详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:contact:query')")
    @GetMapping(value = "/{contactId}")
    public AjaxResult getInfo(@PathVariable("contactId") Long contactId)
    {
        return success(customerContactService.selectCustomerContactByContactId(contactId));
    }

    /**
     * 新增客户联系人
     */
    @PreAuthorize("@ss.hasPermi('project:contact:add')")
    @Log(title = "客户联系人", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CustomerContact customerContact)
    {
        return toAjax(customerContactService.insertCustomerContact(customerContact));
    }

    /**
     * 修改客户联系人
     */
    @PreAuthorize("@ss.hasPermi('project:contact:edit')")
    @Log(title = "客户联系人", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CustomerContact customerContact)
    {
        return toAjax(customerContactService.updateCustomerContact(customerContact));
    }

    /**
     * 删除客户联系人
     */
    @PreAuthorize("@ss.hasPermi('project:contact:remove')")
    @Log(title = "客户联系人", businessType = BusinessType.DELETE)
	@DeleteMapping("/{contactIds}")
    public AjaxResult remove(@PathVariable Long[] contactIds)
    {
        return toAjax(customerContactService.deleteCustomerContactByContactIds(contactIds));
    }
}
