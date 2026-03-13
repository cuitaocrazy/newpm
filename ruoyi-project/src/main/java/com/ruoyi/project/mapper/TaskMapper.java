package com.ruoyi.project.mapper;

import com.ruoyi.project.domain.Task;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
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

    /**
     * 更新任务实际工作量（日报保存时调用）
     * @param taskId 任务ID
     * @param hours 工时（小时）
     */
    int updateActualWorkload(@Param("taskId") Long taskId, @Param("hours") BigDecimal hours);

    /**
     * 汇总项目下所有任务的实际工时（小时）
     * 用于更新主项目 actual_workload
     */
    BigDecimal sumActualWorkloadByProjectId(@Param("projectId") Long projectId);

    /**
     * 批量查询任务所属的主项目ID（去重）
     * 用于确定哪些主项目需要用任务汇总更新工时
     */
    List<Long> selectProjectIdsByTaskIds(@Param("taskIds") List<Long> taskIds);

    List<String> searchTaskCode(@Param("taskCode") String taskCode);

    List<String> searchTaskName(@Param("taskName") String taskName);

    List<String> searchSoftwareDemandNo(@Param("softwareDemandNo") String softwareDemandNo);

    Map<String, Object> selectTaskSummary(Task task);
}
