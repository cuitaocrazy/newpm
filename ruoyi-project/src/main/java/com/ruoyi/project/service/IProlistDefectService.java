package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.ProlistDefect;

/**
 * 批次任务问题单及缺陷 Service
 *
 * @author yadapm-migrate
 */
public interface IProlistDefectService
{
    List<ProlistDefect> selectProlistDefectList(ProlistDefect prolistDefect);

    ProlistDefect selectProlistDefectByProblemId(Long problemId);

    /** 新增（算派生字段 solutionTimeOverOneDay + 编号查重） */
    int insertProlistDefect(ProlistDefect prolistDefect);

    /** 修改（重算派生字段 + 编号查重排除自己） */
    int updateProlistDefect(ProlistDefect prolistDefect);

    /** 软删除 */
    int deleteProlistDefectByProblemIds(Long[] problemIds);

    /** 问题单编号是否唯一(true=可用)。problemId 非空时排除自己 */
    boolean checkProblemNoUnique(String problemNo, Long problemId);

    // ===== 联动 =====
    List<Map<String, Object>> selectBatchByYear(String year);

    String selectPlanProductionDate(Long batchId);

    List<Map<String, Object>> selectTaskOptions(String productionYear, Long batchId, Long deptId);

    ProlistDefect selectTaskInfo(Long taskId);
}
