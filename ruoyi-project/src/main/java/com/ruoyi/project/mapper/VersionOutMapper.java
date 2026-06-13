package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.VersionOut;
import com.ruoyi.project.domain.VersionOutTask;

/**
 * 出入库版本 Mapper 接口
 *
 * @author ruoyi
 * @date 2026-06-11
 */
public interface VersionOutMapper
{
    /** 查询批次版本列表（manual_input='0'） */
    public List<VersionOut> selectVersionOutList(VersionOut versionOut);

    /** 查询批次版本详情（含 taskList） */
    public VersionOut selectVersionOutById(Long id);

    /** 新增批次版本主表 */
    public int insertVersionOut(VersionOut versionOut);

    /** 修改批次版本主表 */
    public int updateVersionOut(VersionOut versionOut);

    /** 软删除批次版本（del_flag='1'） */
    public int deleteVersionOutByIds(Long[] ids);

    /** 批量插入版本-任务关联 */
    public int batchInsertVersionOutTask(List<VersionOutTask> list);

    /** 按版本id删除任务关联 */
    public int deleteVersionOutTaskByVersionId(Long versionId);

    /** 按版本id批量删除任务关联 */
    public int deleteVersionOutTaskByVersionIds(Long[] versionIds);

    // ---------- 版本号生成相关查询 ----------

    /** 同子系统+版本类型当前最大版本编号（类型1/2/3） */
    public Integer getMaxVersionCode(@Param("sysName") String sysName, @Param("versionType") String versionType);

    /** 同子产品+版本类型按年份的最大版本编号（类型4 临时版本包） */
    public Integer getMaxVersionCodeByYear(@Param("subVersionCode") String subVersionCode, @Param("versionType") String versionType);

    /** 按升级包初级版本号取版本编号（类型5/6） */
    public String getCodeByOutVersion(@Param("sysName") String sysName, @Param("versionType") String versionType, @Param("outVersion") String outVersion);

    /** 回退查基线版本类型的版本编号（类型5→3、6→1） */
    public String getCodeByBaseVersion(@Param("sysName") String sysName, @Param("baseVersionType") String baseVersionType, @Param("outVersion") String outVersion);

    // ---------- 级联/联动查询 ----------

    /** 年份→批次列表 */
    public List<Map<String, Object>> selectBatchByYear(String productionYear);

    /** 批次→投产日期(plan_production_date) */
    public String selectVersionPDate(Long batchId);

    /** 软件中心任务号→任务回显信息(task_name/demand_name/project_name) */
    public VersionOutTask selectTaskInfoByDemandNo(String softwareDemandNo);

    /** 任务下拉选项：按 年份+批次+产品 查 pm_task（含回显字段） */
    public List<VersionOutTask> selectTaskOptions(@Param("productionYear") String productionYear,
            @Param("batchId") Long batchId, @Param("product") String product);

    /** 子系统+版本类型→已有升级包初级版本号候选（类型5/6） */
    public List<String> selectOutVersionOptions(@Param("sysName") String sysName, @Param("versionType") String versionType);
}
