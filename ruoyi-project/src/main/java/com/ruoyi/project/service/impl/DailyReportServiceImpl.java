package com.ruoyi.project.service.impl;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.ruoyi.project.domain.vo.TeamDailyReportVO;
import com.ruoyi.project.domain.vo.TeamMemberDailyVO;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.DailyReportDetail;
import com.ruoyi.project.domain.WorkCalendar;
import com.ruoyi.project.domain.vo.DailySubmissionStat;
import com.ruoyi.project.mapper.DailyReportDetailMapper;
import com.ruoyi.project.mapper.WorkCalendarMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.mapper.ProjectMapper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.DailyReportMapper;
import com.ruoyi.project.domain.DailyReport;
import com.ruoyi.project.service.IDailyReportService;
import com.ruoyi.project.service.IDailyReportWhitelistService;

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
    private IDailyReportWhitelistService whitelistService;

    @Autowired
    private com.ruoyi.project.mapper.TaskMapper taskMapper;

    @Autowired
    private WorkCalendarMapper workCalendarMapper;

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
        // 白名单用户禁止提交日报
        if (whitelistService.isInWhitelist(userId)) {
            throw new ServiceException("您已被设置为无需填写日报，如有疑问请联系管理员");
        }
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

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<DailySubmissionStat> selectWeeklyStats(DailyReport query)
    {
        // 解析月份，获取起止日期
        YearMonth ym = YearMonth.parse(query.getYearMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        query.setStartDate(start.toString());
        query.setEndDate(end.toString());

        // 查询该月每天已提交人数 → Map<date, count>
        List<Map<String, Object>> submittedRows = dailyReportMapper.selectSubmittedCountByDate(query);
        Map<String, Integer> submittedMap = new HashMap<>();
        for (Map<String, Object> row : submittedRows) {
            String date = row.get("reportDate").toString();
            int count = ((Number) row.get("submittedCount")).intValue();
            submittedMap.put(date, count);
        }

        // 查询总用户数
        int total = dailyReportMapper.selectTotalUserCount(query);

        // 查询工作日历（按年）
        Map<String, String> calendarMap = new HashMap<>();
        List<WorkCalendar> calendars = workCalendarMapper.selectByYear(start.getYear());
        // 如果跨年则追加下一年（极少发生）
        if (start.getYear() != end.getYear()) {
            calendars.addAll(workCalendarMapper.selectByYear(end.getYear()));
        }
        for (WorkCalendar wc : calendars) {
            if (wc.getCalendarDateStr() != null) {
                calendarMap.put(wc.getCalendarDateStr(), wc.getDayType());
            } else if (wc.getCalendarDate() != null) {
                String ds = new SimpleDateFormat("yyyy-MM-dd").format(wc.getCalendarDate());
                calendarMap.put(ds, wc.getDayType());
            }
        }

        // 星期名称（ISO: 1=周一 ... 7=周日）
        String[] weekNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        // 构建结果
        LocalDate today = LocalDate.now();
        List<DailySubmissionStat> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String dateStr = date.toString();
            boolean future = date.isAfter(today);

            boolean workday;
            if (calendarMap.containsKey(dateStr)) {
                workday = "workday".equals(calendarMap.get(dateStr));
            } else {
                DayOfWeek dow = date.getDayOfWeek();
                workday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            }

            DailySubmissionStat stat = new DailySubmissionStat();
            stat.setReportDate(dateStr);
            stat.setDayOfWeek(weekNames[date.getDayOfWeek().getValue() - 1]);
            stat.setIsWorkday(workday);
            stat.setIsFuture(future);
            // 未来日期不统计人数
            stat.setSubmittedCount(future ? null : submittedMap.getOrDefault(dateStr, 0));
            stat.setUnsubmittedCount(future ? null : (workday ? Math.max(0, total - submittedMap.getOrDefault(dateStr, 0)) : 0));
            result.add(stat);
        }
        return result;
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<Map<String, Object>> selectSubmittedDetail(DailyReport query)
    {
        return dailyReportMapper.selectSubmittedUsersOnDate(query);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<Map<String, Object>> selectUnsubmittedDetail(DailyReport query)
    {
        return dailyReportMapper.selectUnsubmittedUsersOnDate(query);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public int selectTotalUsersForStats(DailyReport query)
    {
        return dailyReportMapper.selectTotalUserCount(query);
    }

    @Override
    @DataScope(deptAlias = "d")
    public List<Map<String, Object>> selectStatsDeptTree(DailyReport query)
    {
        return dailyReportMapper.selectStatsDeptTree(query);
    }

    @Override
    public void exportWeeklyStats(HttpServletResponse response, List<DailySubmissionStat> statList, DailyReport query)
    {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Sheet1：汇总表
            Sheet sheet1 = wb.createSheet("汇总");
            String[] headers1 = {"日期", "星期", "是否工作日", "已提交人数", "未提交人数"};
            Row h1 = sheet1.createRow(0);
            for (int i = 0; i < headers1.length; i++) h1.createCell(i).setCellValue(headers1[i]);
            int r1 = 1;
            for (DailySubmissionStat s : statList) {
                Row row = sheet1.createRow(r1++);
                row.createCell(0).setCellValue(s.getReportDate());
                row.createCell(1).setCellValue(s.getDayOfWeek());
                row.createCell(2).setCellValue(Boolean.TRUE.equals(s.getIsWorkday()) ? "是" : "否");
                row.createCell(3).setCellValue(s.getSubmittedCount() != null ? s.getSubmittedCount() : 0);
                row.createCell(4).setCellValue(s.getUnsubmittedCount() != null ? s.getUnsubmittedCount() : 0);
            }

            // Sheet2：明细表
            Sheet sheet2 = wb.createSheet("明细");
            String[] headers2 = {"日期", "姓名", "部门", "提交状态", "工时合计"};
            Row h2 = sheet2.createRow(0);
            for (int i = 0; i < headers2.length; i++) h2.createCell(i).setCellValue(headers2[i]);
            int r2 = 1;
            for (DailySubmissionStat s : statList) {
                if (!Boolean.TRUE.equals(s.getIsWorkday())) continue;
                DailyReport detailQuery = new DailyReport();
                detailQuery.setReportDate(null); // 使用 String reportDate
                detailQuery.setStartDate(s.getReportDate());
                detailQuery.setEndDate(s.getReportDate());
                detailQuery.setDeptId(query.getDeptId());
                // 传递 dataScope：复用 query 的 params
                detailQuery.setParams(query.getParams());

                // 通过 selectSubmittedUsersOnDate 和 selectUnsubmittedUsersOnDate 查明细
                DailyReport singleQuery = new DailyReport();
                singleQuery.setDeptId(query.getDeptId());
                singleQuery.setParams(query.getParams());

                // 已提交人员：借 Map 传 reportDate 字符串（XML 用 #{reportDate}）
                singleQuery.getParams().put("reportDateStr", s.getReportDate());

                List<Map<String, Object>> submitted = dailyReportMapper.selectSubmittedUsersOnDate(buildDetailQuery(query, s.getReportDate()));
                for (Map<String, Object> p : submitted) {
                    Row row = sheet2.createRow(r2++);
                    row.createCell(0).setCellValue(s.getReportDate());
                    row.createCell(1).setCellValue(str(p.get("nickName")));
                    row.createCell(2).setCellValue(str(p.get("deptName")));
                    row.createCell(3).setCellValue("已提交");
                    Object h = p.get("totalWorkHours");
                    row.createCell(4).setCellValue(h != null ? h.toString() : "0");
                }
                List<Map<String, Object>> unsubmitted = dailyReportMapper.selectUnsubmittedUsersOnDate(buildDetailQuery(query, s.getReportDate()));
                for (Map<String, Object> p : unsubmitted) {
                    Row row = sheet2.createRow(r2++);
                    row.createCell(0).setCellValue(s.getReportDate());
                    row.createCell(1).setCellValue(str(p.get("nickName")));
                    row.createCell(2).setCellValue(str(p.get("deptName")));
                    row.createCell(3).setCellValue("未提交");
                    row.createCell(4).setCellValue("");
                }
            }

            String filename = URLEncoder.encode("日报统计报表_" + query.getYearMonth() + ".xlsx", "UTF-8");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            wb.write(response.getOutputStream());
        } catch (Exception e) {
            throw new ServiceException("导出失败：" + e.getMessage());
        }
    }

    private DailyReport buildDetailQuery(DailyReport base, String reportDateStr) {
        DailyReport q = new DailyReport();
        q.setDeptId(base.getDeptId());
        q.setParams(base.getParams());
        // XML 中用 #{reportDate}，DailyReport.reportDate 是 Date 类型
        // 通过 params Map 绕过类型转换，在 XML 中改为 #{params.reportDateStr}
        // 但现有 XML 用 #{reportDate}，故直接用 java.sql.Date 包装
        try {
            q.setReportDate(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(reportDateStr));
        } catch (Exception ignored) {}
        return q;
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }

    /**
     * 团队日报 - 项目 autocomplete
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<Map<String, Object>> selectTeamProjectOptions(DailyReport query)
    {
        return dailyReportMapper.selectTeamProjectOptions(query);
    }

    /**
     * 团队日报 - 按项目→成员聚合
     * 原始行：项目×成员×日期（LEFT JOIN，无日报则 reportDate=null）
     * Java 层两层聚合：projectId → userId → date→hours
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<TeamDailyReportVO> selectTeamMonthly(DailyReport query)
    {
        List<Map<String, Object>> rows = dailyReportMapper.selectTeamMonthlyRaw(query);

        // projectId → TeamDailyReportVO（保序用 LinkedHashMap）
        LinkedHashMap<Long, TeamDailyReportVO> projectMap = new LinkedHashMap<>();
        // (projectId, userId) → TeamMemberDailyVO
        LinkedHashMap<String, TeamMemberDailyVO> memberMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rows)
        {
            Long projectId = toLong(row.get("projectId"));
            Long userId    = toLong(row.get("userId"));

            // 聚合项目层
            TeamDailyReportVO project = projectMap.computeIfAbsent(projectId, id -> {
                TeamDailyReportVO vo = new TeamDailyReportVO();
                vo.setProjectId(id);
                vo.setProjectName(str(row.get("projectName")));
                vo.setHasContract(toBoolean(row.get("hasContract")));
                vo.setEstimatedWorkload(toBigDecimal(row.get("estimatedWorkload")));
                vo.setActualPersonDays(toBigDecimal(row.get("actualPersonDays")));
                vo.setMembers(new ArrayList<>());
                return vo;
            });

            // 聚合成员层
            String memberKey = projectId + "_" + userId;
            TeamMemberDailyVO member = memberMap.computeIfAbsent(memberKey, k -> {
                TeamMemberDailyVO vo = new TeamMemberDailyVO();
                vo.setUserId(userId);
                vo.setNickName(str(row.get("nickName")));
                vo.setDeptName(str(row.get("deptName")));
                project.getMembers().add(vo);
                return vo;
            });

            // 填充日期工时
            Object reportDate = row.get("reportDate");
            Object totalWorkHours = row.get("totalWorkHours");
            if (reportDate != null && totalWorkHours != null)
            {
                String dateStr = reportDate.toString().substring(0, 10); // yyyy-MM-dd
                BigDecimal hours = toBigDecimal(totalWorkHours);
                member.getDailyHours().merge(dateStr, hours, BigDecimal::add);
                member.setTotalHours(member.getTotalHours().add(hours));
            }
        }

        return new ArrayList<>(projectMap.values());
    }

    private Long toLong(Object val)
    {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        return Long.valueOf(val.toString());
    }

    private BigDecimal toBigDecimal(Object val)
    {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    private Boolean toBoolean(Object val)
    {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        // MySQL BIT/TINYINT: 1 → true
        return "1".equals(val.toString()) || "true".equalsIgnoreCase(val.toString());
    }
}
