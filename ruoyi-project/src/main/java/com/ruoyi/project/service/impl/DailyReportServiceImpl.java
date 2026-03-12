package com.ruoyi.project.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.DailyReportDetail;
import com.ruoyi.project.mapper.DailyReportDetailMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.DailyReportMapper;
import com.ruoyi.project.domain.DailyReport;
import com.ruoyi.project.service.IDailyReportService;

/**
 * 工作日报Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-26
 */
@Service
public class DailyReportServiceImpl implements IDailyReportService
{
    @Autowired
    private DailyReportMapper dailyReportMapper;

    @Autowired
    private DailyReportDetailMapper detailMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private com.ruoyi.project.mapper.TaskMapper taskMapper;

    /**
     * 查询工作日报
     *
     * @param reportId 日报主键
     * @return 工作日报
     */
    @Override
    public DailyReport selectDailyReportById(Long reportId)
    {
        return dailyReportMapper.selectDailyReportById(reportId);
    }

    /**
     * 查询当前用户指定日期的日报
     *
     * @param reportDate 日报日期(yyyy-MM-dd)
     * @return 工作日报
     */
    @Override
    public DailyReport selectMyReportByDate(String reportDate)
    {
        Long userId = SecurityUtils.getUserId();
        return dailyReportMapper.selectByUserAndDate(userId, reportDate);
    }

    /**
     * 查询工作日报列表
     *
     * @param query 查询条件
     * @return 工作日报集合
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<DailyReport> selectDailyReportList(DailyReport query)
    {
        return dailyReportMapper.selectDailyReportList(query);
    }

    /**
     * 查询月度日报列表（含明细）
     *
     * @param query 查询条件
     * @return 工作日报集合（含明细）
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<DailyReport> selectMonthlyReports(DailyReport query)
    {
        return dailyReportMapper.selectMonthlyReports(query);
    }

    /**
     * 查询当前用户关联的项目列表
     *
     * @return 项目列表
     */
    @Override
    public List<Map<String, Object>> selectMyProjects()
    {
        Long userId = SecurityUtils.getUserId();
        List<Map<String, Object>> list = projectMapper.selectProjectsByUserId(userId);
        if (!list.isEmpty()) {
            List<Long> ids = list.stream()
                .map(p -> Long.parseLong(p.get("projectId").toString()))
                .collect(java.util.stream.Collectors.toList());
            List<Long> hasSubIds = taskMapper.selectProjectsHasTasks(ids);
            java.util.Set<Long> hasSubSet = new java.util.HashSet<>(hasSubIds);
            list.forEach(p -> p.put("hasSubProject", hasSubSet.contains(Long.parseLong(p.get("projectId").toString()))));
        }
        return list;
    }

    /**
     * 保存工作日报（新增或更新）
     * 根据用户ID和日报日期判断是新增还是更新
     *
     * @param report 工作日报
     * @return 结果
     */
    @Override
    @Transactional
    public int saveDailyReport(DailyReport report)
    {
        Long userId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDeptId();
        String username = SecurityUtils.getUsername();

        report.setUserId(userId);
        report.setDeptId(deptId);

        // 计算总工时
        BigDecimal totalWorkHours = BigDecimal.ZERO;
        List<DailyReportDetail> detailList = report.getDetailList();
        if (detailList != null)
        {
            for (DailyReportDetail detail : detailList)
            {
                if ("work".equals(detail.getEntryType()) && detail.getWorkHours() != null)
                {
                    totalWorkHours = totalWorkHours.add(detail.getWorkHours());
                }
            }
        }
        report.setTotalWorkHours(totalWorkHours);

        // 格式化日报日期为 yyyy-MM-dd 字符串
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(report.getReportDate());

        // 检查是否已存在该日期的日报（使用简单查询，避免复杂JOIN导致的结果映射问题）
        Long existingReportId = dailyReportMapper.selectReportIdByUserAndDate(userId, dateStr);

        // 用于记录旧明细中涉及的子任务ID和项目ID，确保删除行也参与工时重算
        Set<Long> oldSubProjectIds = new HashSet<>();
        Set<Long> oldProjectIds = new HashSet<>();

        int rows;
        if (existingReportId != null)
        {
            // 更新已有日报
            report.setReportId(existingReportId);
            report.setUpdateBy(username);
            report.setUpdateTime(DateUtils.getNowDate());
            rows = dailyReportMapper.updateDailyReport(report);

            // 在删除前先记录旧明细的子任务ID和项目ID，用于后续工时重算
            List<DailyReportDetail> oldDetails = detailMapper.selectByReportId(existingReportId);
            oldDetails.stream()
                    .filter(d -> d.getSubProjectId() != null)
                    .map(DailyReportDetail::getSubProjectId)
                    .forEach(oldSubProjectIds::add);
            oldDetails.stream()
                    .filter(d -> d.getProjectId() != null)
                    .map(DailyReportDetail::getProjectId)
                    .forEach(oldProjectIds::add);

            // 删除旧明细，插入新明细
            detailMapper.deleteByReportId(existingReportId);
        }
        else
        {
            // 新增日报
            report.setCreateBy(username);
            report.setCreateTime(DateUtils.getNowDate());
            rows = dailyReportMapper.insertDailyReport(report);
        }

        // 批量插入明细
        if (detailList != null && !detailList.isEmpty())
        {
            for (DailyReportDetail detail : detailList)
            {
                detail.setReportId(report.getReportId());
                detail.setCreateBy(username);
                // entryType 默认 work
                if (detail.getEntryType() == null || detail.getEntryType().isEmpty()) {
                    detail.setEntryType("work");
                }
                // 假期行 workContent 默认空字符串
                if (!"work".equals(detail.getEntryType()) && detail.getWorkContent() == null) {
                    detail.setWorkContent("");
                }
                // 假期行 workHours = leaveHours（前端传 leaveHours，统一用 workHours 存储）
                if (!"work".equals(detail.getEntryType()) && detail.getLeaveHours() != null) {
                    detail.setWorkHours(detail.getLeaveHours());
                }
            }
            detailMapper.batchInsert(detailList);
        }

        // 更新受影响项目的实际工作量（两级滚动：先子任务，再主项目）
        List<DailyReportDetail> workDetails = (detailList != null ? detailList : java.util.Collections.<DailyReportDetail>emptyList())
                .stream()
                .filter(d -> d.getProjectId() != null && "work".equals(d.getEntryType()))
                .collect(Collectors.toList());

        // Step 1：更新受影响子任务工时（含旧明细中被删除的子任务行）
        Set<Long> affectedSubProjectIds = workDetails.stream()
                .filter(d -> d.getSubProjectId() != null)
                .map(DailyReportDetail::getSubProjectId)
                .collect(Collectors.toSet());
        affectedSubProjectIds.addAll(oldSubProjectIds);
        for (Long taskId : affectedSubProjectIds) {
            BigDecimal taskHours = detailMapper.sumWorkHoursBySubProjectId(taskId);
            taskMapper.updateActualWorkload(taskId, taskHours);
        }

        // Step 2：更新主项目工时（含旧明细中被删除行对应的项目）
        Set<Long> affectedProjectIds = workDetails.stream()
                .map(DailyReportDetail::getProjectId)
                .collect(Collectors.toSet());
        affectedProjectIds.addAll(oldProjectIds);

        // 2a. 有子任务的主项目：从 pm_task 汇总（只更新那些确实有任务记录的父项目）
        if (!affectedSubProjectIds.isEmpty()) {
            List<Long> parentProjectIds = taskMapper.selectProjectIdsByTaskIds(
                    new java.util.ArrayList<>(affectedSubProjectIds));
            for (Long parentProjectId : parentProjectIds) {
                BigDecimal totalTaskHours = taskMapper.sumActualWorkloadByProjectId(parentProjectId);
                projectMapper.updateActualWorkload(parentProjectId, totalTaskHours);
                affectedProjectIds.remove(parentProjectId);
            }
        }
        // 2b. 普通项目（无子任务）：从日报明细直接汇总工时
        for (Long projectId : affectedProjectIds) {
            BigDecimal directHours = detailMapper.sumWorkHoursByProjectId(projectId);
            projectMapper.updateActualWorkload(projectId, directHours);
        }

        return rows;
    }

    /**
     * 批量删除工作日报
     * 先物理删除明细，再软删除主记录
     *
     * @param reportIds 需要删除的日报主键集合
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteDailyReportByIds(Long[] reportIds)
    {
        // 先物理删除明细
        detailMapper.deleteByReportIds(reportIds);
        // 再物理删除主记录
        return dailyReportMapper.deleteDailyReportByIds(reportIds);
    }

    /**
     * 查询活动页用户列表（数据权限过滤）
     *
     * @param query 查询条件（deptId）
     * @return 用户列表
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<Map<String, Object>> selectActivityUsers(DailyReport query)
    {
        return dailyReportMapper.selectActivityUsers(query);
    }
}
