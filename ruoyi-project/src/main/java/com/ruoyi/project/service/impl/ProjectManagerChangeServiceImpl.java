package com.ruoyi.project.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.ProjectManagerChangeMapper;
import com.ruoyi.project.domain.ProjectManagerChange;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.vo.ProjectManagerChangeVo;
import com.ruoyi.project.service.IProjectManagerChangeService;
import com.ruoyi.project.service.IProjectService;

/**
 * 项目经理变更Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-13
 */
@Service
public class ProjectManagerChangeServiceImpl implements IProjectManagerChangeService
{
    private static final Logger log = LoggerFactory.getLogger(ProjectManagerChangeServiceImpl.class);

    @Autowired
    private ProjectManagerChangeMapper projectManagerChangeMapper;

    @Autowired
    private IProjectService projectService;

    /**
     * 查询项目经理变更
     * 
     * @param changeId 项目经理变更主键
     * @return 项目经理变更
     */
    @Override
    public ProjectManagerChange selectProjectManagerChangeByChangeId(Long changeId)
    {
        return projectManagerChangeMapper.selectProjectManagerChangeByChangeId(changeId);
    }

    /**
     * 查询项目经理变更列表
     *
     * @param projectManagerChange 项目经理变更
     * @return 项目经理变更
     */
    @Override
    public List<ProjectManagerChangeVo> selectProjectManagerChangeList(ProjectManagerChange projectManagerChange)
    {
        return projectManagerChangeMapper.selectProjectManagerChangeList(projectManagerChange);
    }

    /**
     * 新增项目经理变更
     * 
     * @param projectManagerChange 项目经理变更
     * @return 结果
     */
    @Override
    public int insertProjectManagerChange(ProjectManagerChange projectManagerChange)
    {
        projectManagerChange.setCreateTime(DateUtils.getNowDate());
        return projectManagerChangeMapper.insertProjectManagerChange(projectManagerChange);
    }

    /**
     * 修改项目经理变更
     * 
     * @param projectManagerChange 项目经理变更
     * @return 结果
     */
    @Override
    public int updateProjectManagerChange(ProjectManagerChange projectManagerChange)
    {
        projectManagerChange.setUpdateTime(DateUtils.getNowDate());
        return projectManagerChangeMapper.updateProjectManagerChange(projectManagerChange);
    }

    /**
     * 批量删除项目经理变更
     * 
     * @param changeIds 需要删除的项目经理变更主键
     * @return 结果
     */
    @Override
    public int deleteProjectManagerChangeByChangeIds(Long[] changeIds)
    {
        return projectManagerChangeMapper.deleteProjectManagerChangeByChangeIds(changeIds);
    }

    /**
     * 删除项目经理变更信息
     *
     * @param changeId 项目经理变更主键
     * @return 结果
     */
    @Override
    public int deleteProjectManagerChangeByChangeId(Long changeId)
    {
        return projectManagerChangeMapper.deleteProjectManagerChangeByChangeId(changeId);
    }

    /**
     * 变更项目经理
     *
     * @param projectId 项目ID
     * @param newManagerId 新项目经理ID
     * @param changeReason 变更原因
     * @return 结果
     */
    @Override
    @Transactional
    public int changeProjectManager(Long projectId, Long newManagerId, String changeReason)
    {
        // 1. 查询项目当前信息
        Project project = projectService.selectProjectByProjectId(projectId);
        if (project == null)
        {
            throw new ServiceException("项目不存在");
        }

        Long oldManagerId = project.getProjectManagerId();

        // 2. 检查新旧经理是否相同
        if (Objects.equals(oldManagerId, newManagerId))
        {
            throw new ServiceException("新旧项目经理相同，无需变更");
        }

        // 3. 更新项目表的项目经理
        project.setProjectManagerId(newManagerId);
        int rows = projectService.updateProject(project);

        // 4. 插入变更记录
        ProjectManagerChange change = new ProjectManagerChange();
        change.setProjectId(projectId);
        change.setOldManagerId(oldManagerId);
        change.setNewManagerId(newManagerId);
        change.setChangeReason(changeReason);
        projectManagerChangeMapper.insertProjectManagerChange(change);

        return rows;
    }

    /**
     * 批量变更项目经理
     *
     * @param projectIds 项目ID数组
     * @param newManagerId 新项目经理ID
     * @param changeReason 变更原因
     * @return 成功变更的项目数量
     */
    @Override
    @Transactional
    public int batchChangeProjectManager(Long[] projectIds, Long newManagerId, String changeReason)
    {
        if (projectIds == null || projectIds.length == 0)
        {
            throw new ServiceException("请选择要变更的项目");
        }

        int count = 0;
        for (Long projectId : projectIds)
        {
            try
            {
                count += changeProjectManager(projectId, newManagerId, changeReason);
            }
            catch (ServiceException e)
            {
                // 记录日志，继续处理下一个项目
                log.warn("批量变更项目经理失败，项目ID: {}, 原因: {}", projectId, e.getMessage());
            }
        }

        return count;
    }

    /**
     * 查询项目变更详情（项目信息+完整变更历史）
     *
     * @param projectId 项目ID
     * @return 变更详情
     */
    @Override
    public Map<String, Object> getChangeDetail(Long projectId)
    {
        Map<String, Object> result = new HashMap<>();

        // 1. 查询项目基本信息
        Project project = projectService.selectProjectByProjectId(projectId);
        if (project == null)
        {
            throw new ServiceException("项目不存在");
        }
        result.put("project", project);

        // 2. 查询完整变更历史
        List<ProjectManagerChange> changes = projectManagerChangeMapper.selectChangeHistoryByProjectId(projectId);
        result.put("changes", changes);

        return result;
    }
}
