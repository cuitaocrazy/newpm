package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.ProjectApproval;

/**
 * 立项审核Service接口
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
public interface IProjectApprovalService 
{
    /**
     * 查询立项审核
     * 
     * @param approvalId 立项审核主键
     * @return 立项审核
     */
    public ProjectApproval selectProjectApprovalByApprovalId(Long approvalId);

    /**
     * 查询立项审核列表
     * 
     * @param projectApproval 立项审核
     * @return 立项审核集合
     */
    public List<ProjectApproval> selectProjectApprovalList(ProjectApproval projectApproval);

    /**
     * 新增立项审核
     * 
     * @param projectApproval 立项审核
     * @return 结果
     */
    public int insertProjectApproval(ProjectApproval projectApproval);

    /**
     * 修改立项审核
     * 
     * @param projectApproval 立项审核
     * @return 结果
     */
    public int updateProjectApproval(ProjectApproval projectApproval);

    /**
     * 批量删除立项审核
     * 
     * @param approvalIds 需要删除的立项审核主键集合
     * @return 结果
     */
    public int deleteProjectApprovalByApprovalIds(Long[] approvalIds);

    /**
     * 删除立项审核信息
     * 
     * @param approvalId 立项审核主键
     * @return 结果
     */
    public int deleteProjectApprovalByApprovalId(Long approvalId);
}
