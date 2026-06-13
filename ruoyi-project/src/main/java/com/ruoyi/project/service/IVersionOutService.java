package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.VersionOut;
import com.ruoyi.project.domain.VersionOutTask;

/**
 * 出入库版本 Service 接口
 *
 * @author ruoyi
 * @date 2026-06-11
 */
public interface IVersionOutService
{
    /** 查询批次版本列表 */
    public List<VersionOut> selectVersionOutList(VersionOut versionOut);

    /** 查询批次版本详情 */
    public VersionOut selectVersionOutById(Long id);

    /** 新增批次版本（生成版本号 + 级联任务 + 唯一性重试） */
    public int insertVersionOut(VersionOut versionOut);

    /** 修改批次版本（按需重算版本号 + 级联任务替换） */
    public int updateVersionOut(VersionOut versionOut);

    /** 软删除批次版本 */
    public int deleteVersionOutByIds(Long[] ids);

    // ---------- 非批次（manual_input='1'，任务手填，复用版本号生成） ----------
    public List<VersionOut> selectVersionOutManualList(VersionOut versionOut);
    public int insertVersionOutManual(VersionOut versionOut);
    public int updateVersionOutManual(VersionOut versionOut);

    /** 生成出入库版本号（实时，供前端回填）。返回 {outLibVersion, versionCode} */
    public Map<String, String> generateOutLibVersion(VersionOut versionOut, String addFlag,
            String oldSubVersionCode, String oldVersionType);

    /** 年份→批次列表 */
    public List<Map<String, Object>> selectBatchByYear(String productionYear);

    /** 批次→投产日期 */
    public String selectVersionPDate(Long batchId);

    /** 软件中心任务号→任务回显信息 */
    public VersionOutTask selectTaskInfoByDemandNo(String softwareDemandNo);

    /** 任务下拉选项：按 年份+批次+产品 */
    public List<VersionOutTask> selectTaskOptions(String productionYear, Long batchId, String product);

    /** 子系统+版本类型→升级包初级版本号候选 */
    public List<String> selectOutVersionOptions(String sysName, String versionType);
}
