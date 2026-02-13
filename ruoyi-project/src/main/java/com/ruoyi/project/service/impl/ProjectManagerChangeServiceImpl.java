package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.ProjectManagerChangeMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.domain.ProjectManagerChange;
import com.ruoyi.project.domain.vo.ProjectManagerChangeVO;
import com.ruoyi.project.service.IProjectManagerChangeService;

/**
 * 项目经理变更Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-14
 */
@Service
public class ProjectManagerChangeServiceImpl implements IProjectManagerChangeService
{
    @Autowired
    private ProjectManagerChangeMapper projectManagerChangeMapper;

    @Autowired
    private ProjectMapper projectMapper;

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
    public List<ProjectManagerChange> selectProjectManagerChangeList(ProjectManagerChange projectManagerChange)
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
     * 查询项目列表（带最新变更信息）
     *
     * @param vo 查询条件
     * @return 项目列表
     */
    @Override
    public List<ProjectManagerChangeVO> selectProjectListWithLatestChange(ProjectManagerChangeVO vo)
    {
        return projectManagerChangeMapper.selectProjectListWithLatestChange(vo);
    }

    /**
     * 查询项目的完整变更历史
     *
     * @param projectId 项目ID
     * @return 变更历史列表
     */
    @Override
    public List<ProjectManagerChange> selectProjectChangeHistory(Long projectId)
    {
        return projectManagerChangeMapper.selectProjectChangeHistory(projectId);
    }

    /**
     * 变更项目经理（单个）
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
        // 查询项目
        Project project = projectMapper.selectProjectByProjectId(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }

        Long oldManagerId = project.getProjectManagerId();

        // 更新项目表的项目经理
        project.setProjectManagerId(newManagerId);
        int rows = projectMapper.updateProject(project);

        // 记录变更
        ProjectManagerChange change = new ProjectManagerChange();
        change.setProjectId(projectId);
        change.setOldManagerId(oldManagerId);
        change.setNewManagerId(newManagerId);
        change.setChangeReason(changeReason);
        change.setCreateBy(SecurityUtils.getUsername());
        change.setCreateTime(DateUtils.getNowDate());
        projectManagerChangeMapper.insertProjectManagerChange(change);

        return rows;
    }

    /**
     * 批量变更项目经理
     *
     * @param projectIds 项目ID数组
     * @param newManagerId 新项目经理ID
     * @param changeReason 变更原因
     * @return 结果
     */
    @Override
    @Transactional
    public int batchChangeProjectManager(Long[] projectIds, Long newManagerId, String changeReason)
    {
        int count = 0;
        for (Long projectId : projectIds) {
            count += changeProjectManager(projectId, newManagerId, changeReason);
        }
        return count;
    }
}
