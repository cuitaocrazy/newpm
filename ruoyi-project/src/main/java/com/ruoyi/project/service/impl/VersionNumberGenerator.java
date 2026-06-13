package com.ruoyi.project.service.impl;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.domain.SysName;
import com.ruoyi.project.domain.VersionOut;
import com.ruoyi.project.mapper.SysNameMapper;
import com.ruoyi.project.mapper.VersionOutMapper;

/**
 * 出入库版本号生成器（迁移自 yadapm StorageService.getOutLibVersion）。
 * 6 种版本类型各有规则，详见 specs/007-batch-version-management/reference-version-algorithm.md。
 * 纯逻辑 + mapper 查询，便于特征测试覆盖。
 *
 * @author ruoyi
 * @date 2026-06-11
 */
@Component
public class VersionNumberGenerator
{
    @Autowired
    private VersionOutMapper versionOutMapper;

    @Autowired
    private SysNameMapper sysNameMapper;

    /**
     * 生成出入库版本号。
     *
     * @param sysName          子系统名称
     * @param subVersionCode   子产品ID
     * @param versionType      版本类型 1-6
     * @param outVersion       升级包初级版本号（类型5/6）
     * @param addFlag          1=新增 2=编辑
     * @param id               记录id（编辑时用）
     * @param oldSubVersionCode 编辑前子产品
     * @param oldVersionType   编辑前版本类型
     * @return [出入库版本号, 版本编号]
     */
    public String[] generate(String sysName, String subVersionCode, String versionType, String outVersion,
            String addFlag, Long id, String oldSubVersionCode, String oldVersionType)
    {
        SysName cfg = sysNameMapper.selectSysNameByName(sysName);
        String base = cfg != null ? StringUtils.nvl(cfg.getBaseVersionCode(), "") : "";
        String product = cfg != null ? StringUtils.nvl(cfg.getProduct(), "") : "";

        String code = "";
        String outLibVersion = "";

        switch (versionType == null ? "" : versionType)
        {
            case "1": // SP升级包
                code = pad2(maxCode(sysName, versionType) + 1);
                outLibVersion = base + "_SP" + code;
                break;
            case "2": // PTF补丁包
                code = pad2(maxCode(sysName, versionType) + 1);
                outLibVersion = base + "_PTF" + code;
                break;
            case "3": // B测试包
                code = pad2(maxCode(sysName, versionType) + 1);
                outLibVersion = base + "_B" + code;
                break;
            case "4": // 临时版本包
                if ("2".equals(addFlag) && !keyChanged(oldSubVersionCode, subVersionCode, oldVersionType, versionType)
                        && id != null)
                {
                    // 编辑且关键字段未变：沿用原版本编号
                    VersionOut old = versionOutMapper.selectVersionOutById(id);
                    code = old != null ? StringUtils.nvl(old.getVersionCode(), "") : "";
                }
                else
                {
                    code = pad3(maxCodeByYear(subVersionCode, versionType) + 1);
                }
                outLibVersion = "T_" + curYear() + "_" + code + "_" + product;
                break;
            case "5": // B包升级包（回退基线类型3）
                code = byOutVersion(sysName, versionType, outVersion, "3");
                outLibVersion = base + "_B" + code;
                break;
            case "6": // SP包升级包（回退基线类型1）
                code = byOutVersion(sysName, versionType, outVersion, "1");
                outLibVersion = base + "_SP" + code;
                break;
            default:
                break;
        }
        return new String[] { outLibVersion, code };
    }

    /** 类型1/2/3：同子系统+类型最大版本编号 */
    private int maxCode(String sysName, String versionType)
    {
        Integer m = versionOutMapper.getMaxVersionCode(sysName, versionType);
        return m == null ? 0 : m;
    }

    /** 类型4：同子产品+类型按年份最大版本编号 */
    private int maxCodeByYear(String subVersionCode, String versionType)
    {
        Integer m = versionOutMapper.getMaxVersionCodeByYear(subVersionCode, versionType);
        return m == null ? 0 : m;
    }

    /** 类型5/6：升级包大小版本号生成（含 .9 数值进位 + 回退基线类型） */
    private String byOutVersion(String sysName, String versionType, String outVersion, String baseType)
    {
        String vc = versionOutMapper.getCodeByOutVersion(sysName, versionType, outVersion);
        if (StringUtils.isEmpty(vc))
        {
            vc = versionOutMapper.getCodeByBaseVersion(sysName, baseType, outVersion);
        }
        if (StringUtils.isEmpty(vc))
        {
            // 兜底：无任何基线，视升级包初级版本号为大版本
            vc = StringUtils.nvl(outVersion, "0");
        }
        String[] arr = vc.split("\\.");
        if (arr.length == 1)
        {
            return pad2(parseIntSafe(arr[0])) + ".01";
        }
        else
        {
            int suffix = parseIntSafe(arr[1]) + 1;
            return arr[0] + "." + pad2(suffix);
        }
    }

    private boolean keyChanged(String oldSub, String sub, String oldType, String type)
    {
        return !StringUtils.equals(oldSub, sub) || !StringUtils.equals(oldType, type);
    }

    private static int parseIntSafe(String s)
    {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static String pad2(int n) { return String.format("%02d", n); }

    private static String pad3(int n) { return String.format("%03d", n); }

    protected String curYear() { return String.valueOf(LocalDate.now().getYear()); }
}
