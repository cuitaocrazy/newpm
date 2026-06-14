package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.ProlistDefect;

/**
 * 批次任务问题单及缺陷 Mapper
 *
 * @author yadapm-migrate
 */
public interface ProlistDefectMapper
{
    /** 列表（多维查询 + JOIN pm_task/批次/部门/用户 取展示字段；部门走 ancestors） */
    List<ProlistDefect> selectProlistDefectList(ProlistDefect prolistDefect);

    /** 按 id 查（含关联展示字段） */
    ProlistDefect selectProlistDefectByProblemId(Long problemId);

    /** 新增 */
    int insertProlistDefect(ProlistDefect prolistDefect);

    /** 修改 */
    int updateProlistDefect(ProlistDefect prolistDefect);

    /** 软删除（del_flag='1'） */
    int deleteProlistDefectByProblemIds(Long[] problemIds);

    /** 问题单编号查重（problemId 非空时排除自己） */
    int checkProblemNoUnique(@Param("problemNo") String problemNo, @Param("problemId") Long problemId);

    /** 年份→批次下拉（pm_production_batch） */
    List<Map<String, Object>> selectBatchByYear(@Param("year") String year);

    /** 批次→计划投产日期 */
    String selectPlanProductionDate(@Param("batchId") Long batchId);

    /** 年份+批次+部门 → 任务号下拉（部门走 ancestors） */
    List<Map<String, Object>> selectTaskOptions(@Param("productionYear") String productionYear,
                                                @Param("batchId") Long batchId,
                                                @Param("deptId") Long deptId);

    /** 任务回显（taskName/product/各测试日期/排期状态） */
    ProlistDefect selectTaskInfo(@Param("taskId") Long taskId);

    /** 取任务所属项目的部门(project_dept)，落库 dept_id 用，保证列表过滤与任务下拉同源 */
    String selectTaskProjectDept(@Param("taskId") Long taskId);
}
