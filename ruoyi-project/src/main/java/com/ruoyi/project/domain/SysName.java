package com.ruoyi.project.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 出入库子系统配置对象 pm_sys_name
 * 迁移自 yadapm T_C_SYS_NAME（子系统名称 + 基准版本号 + 一级产品 + 产品）
 *
 * @author ruoyi
 * @date 2026-06-11
 */
public class SysName extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 子系统名称 */
    @Excel(name = "子系统名称")
    private String sysName;

    /** 基准版本号 */
    @Excel(name = "基准版本号")
    private String baseVersionCode;

    /** 一级产品ID */
    private String pId;

    /** 产品名称 */
    @Excel(name = "产品")
    private String product;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setSysName(String sysName) { this.sysName = sysName; }
    public String getSysName() { return sysName; }
    public void setBaseVersionCode(String baseVersionCode) { this.baseVersionCode = baseVersionCode; }
    public String getBaseVersionCode() { return baseVersionCode; }
    public void setpId(String pId) { this.pId = pId; }
    public String getpId() { return pId; }
    public void setProduct(String product) { this.product = product; }
    public String getProduct() { return product; }
}
