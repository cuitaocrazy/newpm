package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.domain.ProjectApproval;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.service.IProjectService;

/**
 * 项目管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@Service
public class ProjectServiceImpl implements IProjectService 
{
    @Autowired
    private ProjectMapper projectMapper;

    /**
     * 查询项目管理
     * 
     * @param projectId 项目管理主键
     * @return 项目管理
     */
    @Override
    public Project selectProjectByProjectId(Long projectId)
    {
        return projectMapper.selectProjectByProjectId(projectId);
    }

    /**
     * 查询项目管理列表
     * 
     * @param project 项目管理
     * @return 项目管理
     */
    @Override
    public List<Project> selectProjectList(Project project)
    {
        return projectMapper.selectProjectList(project);
    }

    /**
     * 新增项目管理
     * 
     * @param project 项目管理
     * @return 结果
     */
    @Transactional
    @Override
    public int insertProject(Project project)
    {
        project.setCreateBy(SecurityUtils.getUsername());
        project.setCreateTime(DateUtils.getNowDate());
        int rows = projectMapper.insertProject(project);
        insertProjectApproval(project);
        return rows;
    }

    /**
     * 修改项目管理
     * 
     * @param project 项目管理
     * @return 结果
     */
    @Transactional
    @Override
    public int updateProject(Project project)
    {
        project.setUpdateBy(SecurityUtils.getUsername());
        project.setUpdateTime(DateUtils.getNowDate());
        projectMapper.deleteProjectApprovalByProjectId(project.getProjectId());
        insertProjectApproval(project);
        return projectMapper.updateProject(project);
    }

    /**
     * 批量删除项目管理
     * 
     * @param projectIds 需要删除的项目管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteProjectByProjectIds(Long[] projectIds)
    {
        projectMapper.deleteProjectApprovalByProjectIds(projectIds);
        return projectMapper.deleteProjectByProjectIds(projectIds);
    }

    /**
     * 删除项目管理信息
     * 
     * @param projectId 项目管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteProjectByProjectId(Long projectId)
    {
        projectMapper.deleteProjectApprovalByProjectId(projectId);
        return projectMapper.deleteProjectByProjectId(projectId);
    }

    /**
     * 新增项目审核信息
     * 
     * @param project 项目管理对象
     */
    public void insertProjectApproval(Project project)
    {
        List<ProjectApproval> projectApprovalList = project.getProjectApprovalList();
        Long projectId = project.getProjectId();
        if (StringUtils.isNotNull(projectApprovalList))
        {
            List<ProjectApproval> list = new ArrayList<ProjectApproval>();
            for (ProjectApproval projectApproval : projectApprovalList)
            {
                projectApproval.setProjectId(projectId);
                list.add(projectApproval);
            }
            if (list.size() > 0)
            {
                projectMapper.batchProjectApproval(list);
            }
        }
    }
}
