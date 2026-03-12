package com.ruoyi.project.service.impl;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.Task;
import com.ruoyi.project.mapper.TaskMapper;
import com.ruoyi.project.service.ITaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class TaskServiceImpl implements ITaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public List<Task> selectTaskList(Task task) {
        return taskMapper.selectTaskList(task);
    }

    @Override
    public Task selectTaskById(Long taskId) {
        return taskMapper.selectTaskById(taskId);
    }

    @Override
    public int insertTask(Task task) {
        task.setCreateBy(SecurityUtils.getUsername());
        task.setCreateTime(DateUtils.getNowDate());
        return taskMapper.insertTask(task);
    }

    @Override
    public int updateTask(Task task) {
        task.setUpdateBy(SecurityUtils.getUsername());
        task.setUpdateTime(DateUtils.getNowDate());
        return taskMapper.updateTask(task);
    }

    @Override
    public int deleteTaskByIds(Long[] taskIds) {
        return taskMapper.deleteTaskByIds(taskIds);
    }

    @Override
    public int countTasksByProjectId(Long projectId) {
        return taskMapper.countTasksByProjectId(projectId);
    }

    @Override
    public List<Map<String, Object>> selectTaskOptions(Long projectId) {
        return taskMapper.selectTaskOptions(projectId);
    }

    @Override
    public List<Long> selectProjectsHasTasks(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyList();
        }
        return taskMapper.selectProjectsHasTasks(projectIds);
    }
}
