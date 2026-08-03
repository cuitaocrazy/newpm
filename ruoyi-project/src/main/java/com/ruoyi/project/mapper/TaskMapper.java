package com.ruoyi.project.mapper;

import com.ruoyi.project.domain.Task;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TaskMapper {

    List<Task> selectTaskList(Task task);

    Task selectTaskById(Long taskId);

    int insertTask(Task task);

    int updateTask(Task task);

    int deleteTaskById(Long taskId);

    int deleteTaskByIds(Long[] taskIds);

    /**
     * 获取项目的任务轻量选项（日报下拉用）
     * 返回字段：taskId, taskName, taskCode, taskStage, taskManagerId, taskManagerName,
     *           estimatedWorkload, actualWorkload, batchNo, scheduleStatus
     */
    List<Map<String, Object>> selectTaskOptions(@Param("projectId") Long projectId);

    /**
     * 批量判断哪些项目有任务（日报 hasSubProject 标记用）
     * 返回有任务的项目ID列表
     */
    List<Long> selectProjectsHasTasks(@Param("projectIds") List<Long> projectIds);

    /** 统计项目的任务数量 */
    int countTasksByProjectId(@Param("projectId") Long projectId);

    /** 统计引用该批次的任务数量 */
    int countTasksByBatchId(@Param("batchId") Long batchId);

    /**
     * 更新任务实际工作量（日报保存时调用）
     * @param taskId 任务ID
     * @param hours 工时（小时）
     */
    int updateActualWorkload(@Param("taskId") Long taskId, @Param("hours") BigDecimal hours);

    /**
     * 汇总项目下所有任务的实际工时（小时）
     *
     * <p><b>警告：不要用它更新 pm_project.actual_workload。</b>该口径只覆盖挂在任务上的工时，
     * 会漏掉「项目建任务之前直挂父项目」的工时（明细 sub_project_id IS NULL），
     * 导致父项目实际人天被永久抹掉——生产曾因此丢失 22 个项目共 4341.5 小时（Issue #5 ①）。
     * 主项目工时请统一用 {@code DailyReportDetailMapper.sumWorkHoursByProjectId}：
     * 明细的 project_id 存的始终是父项目 id，该汇总天然覆盖两类工时。
     */
    BigDecimal sumActualWorkloadByProjectId(@Param("projectId") Long projectId);

    /**
     * 批量查询任务所属的主项目ID（去重）
     * 用于把受影响任务的父项目纳入工时重算范围
     */
    List<Long> selectProjectIdsByTaskIds(@Param("taskIds") List<Long> taskIds);

    List<String> searchTaskCode(@Param("taskCode") String taskCode);

    List<String> searchTaskName(@Param("taskName") String taskName);

    List<String> searchSoftwareDemandNo(@Param("softwareDemandNo") String softwareDemandNo);

    Map<String, Object> selectTaskSummary(Task task);

    /**
     * 批量查询任务与其父项目的对应关系，供日报保存时的任务归属校验使用
     *
     * <p>日报明细的 {@code sub_project_id} 指向 {@code pm_task.task_id}。校验要求：
     * 该任务的 {@code project_id} 必须等于明细自己声明的 {@code project_id}，
     * 否则拒绝保存（FR-007）——映射缺失（任务不存在）同样视为不匹配。
     *
     * @param taskIds 待校验的任务ID集合
     * @return 每项含 taskId / projectId
     */
    List<Map<String, Object>> selectTaskProjectPairs(@Param("taskIds") Collection<Long> taskIds);
}
