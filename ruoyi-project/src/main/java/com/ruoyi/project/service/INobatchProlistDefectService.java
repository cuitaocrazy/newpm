package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.NobatchProlistDefect;

/**
 * 非批次任务问题单及缺陷 Service
 *
 * @author yadapm-migrate
 */
public interface INobatchProlistDefectService
{
    List<NobatchProlistDefect> selectNobatchProlistDefectList(NobatchProlistDefect nobatchProlistDefect);

    NobatchProlistDefect selectNobatchProlistDefectByProblemId(Long problemId);

    /** 新增（算派生 solutionTimeOverOneDay + 编号查重） */
    int insertNobatchProlistDefect(NobatchProlistDefect nobatchProlistDefect);

    /** 修改（重算派生 + 编号查重排除自己） */
    int updateNobatchProlistDefect(NobatchProlistDefect nobatchProlistDefect);

    int deleteNobatchProlistDefectByProblemIds(Long[] problemIds);

    /** 问题单编号是否唯一(true=可用)。problemId 非空排除自己 */
    boolean checkProblemNoUnique(String problemNo, Long problemId);

    List<Map<String, Object>> selectBatchByYear(String year);

    String selectPlanProductionDate(Long batchId);
}
