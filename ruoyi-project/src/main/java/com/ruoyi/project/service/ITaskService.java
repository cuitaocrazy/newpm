package com.ruoyi.project.service;

import com.ruoyi.project.domain.Task;
import java.util.List;
import java.util.Map;

public interface ITaskService {

    List<Task> selectTaskList(Task task);

    Task selectTaskById(Long taskId);

    int insertTask(Task task);

    int updateTask(Task task);

    int deleteTaskByIds(Long[] taskIds);

    int countTasksByProjectId(Long projectId);

    List<Map<String, Object>> selectTaskOptions(Long projectId);

    List<Long> selectProjectsHasTasks(List<Long> projectIds);

    List<String> searchTaskCode(String taskCode);

    List<String> searchTaskName(String taskName);

    List<String> searchSoftwareDemandNo(String softwareDemandNo);
}
