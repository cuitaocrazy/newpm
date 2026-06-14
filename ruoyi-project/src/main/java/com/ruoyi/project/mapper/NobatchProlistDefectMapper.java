package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.NobatchProlistDefect;

/**
 * 非批次任务问题单及缺陷 Mapper
 *
 * @author yadapm-migrate
 */
public interface NobatchProlistDefectMapper
{
    /** 列表（多维查询 + JOIN 批次/部门/用户；任务字段读本表；部门走 ancestors） */
    List<NobatchProlistDefect> selectNobatchProlistDefectList(NobatchProlistDefect nobatchProlistDefect);

    /** 按 id 查 */
    NobatchProlistDefect selectNobatchProlistDefectByProblemId(Long problemId);

    int insertNobatchProlistDefect(NobatchProlistDefect nobatchProlistDefect);

    int updateNobatchProlistDefect(NobatchProlistDefect nobatchProlistDefect);

    /** 软删除（del_flag='1' + problem_no 加 _DEL_ 后缀腾位） */
    int deleteNobatchProlistDefectByProblemIds(Long[] problemIds);

    /** 问题单编号查重（problemId 非空时排除自己） */
    int checkProblemNoUnique(@Param("problemNo") String problemNo, @Param("problemId") Long problemId);

    /** 年份→批次下拉 */
    List<Map<String, Object>> selectBatchByYear(@Param("year") String year);

    /** 批次→计划投产日期 */
    String selectPlanProductionDate(@Param("batchId") Long batchId);
}
