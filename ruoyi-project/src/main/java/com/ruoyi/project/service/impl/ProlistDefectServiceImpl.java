package com.ruoyi.project.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.domain.ProlistDefect;
import com.ruoyi.project.mapper.ProlistDefectMapper;
import com.ruoyi.project.service.IProlistDefectService;

/**
 * 批次任务问题单及缺陷 Service 实现
 *
 * @author yadapm-migrate
 */
@Service
public class ProlistDefectServiceImpl implements IProlistDefectService
{
    @Autowired
    private ProlistDefectMapper prolistDefectMapper;

    @Override
    public List<ProlistDefect> selectProlistDefectList(ProlistDefect prolistDefect)
    {
        return prolistDefectMapper.selectProlistDefectList(prolistDefect);
    }

    @Override
    public ProlistDefect selectProlistDefectByProblemId(Long problemId)
    {
        return prolistDefectMapper.selectProlistDefectByProblemId(problemId);
    }

    @Override
    public int insertProlistDefect(ProlistDefect prolistDefect)
    {
        if (!checkProblemNoUnique(prolistDefect.getProblemNo(), null))
        {
            throw new ServiceException("问题单编号已存在：" + prolistDefect.getProblemNo());
        }
        prolistDefect.setSolutionTimeOverOneDay(
            calcSolutionTimeOverOneDay(prolistDefect.getSubmitDate(), prolistDefect.getSettleDate()));
        // dept_id 以任务所属项目的 project_dept 为准，保证列表部门过滤与任务下拉同源
        syncDeptIdFromTask(prolistDefect);
        prolistDefect.setCreateBy(SecurityUtils.getUsername());
        return prolistDefectMapper.insertProlistDefect(prolistDefect);
    }

    @Override
    public int updateProlistDefect(ProlistDefect prolistDefect)
    {
        // 查重排除自己（修复老系统 edit 卡自己的 bug）
        if (!checkProblemNoUnique(prolistDefect.getProblemNo(), prolistDefect.getProblemId()))
        {
            throw new ServiceException("问题单编号已存在：" + prolistDefect.getProblemNo());
        }
        prolistDefect.setSolutionTimeOverOneDay(
            calcSolutionTimeOverOneDay(prolistDefect.getSubmitDate(), prolistDefect.getSettleDate()));
        syncDeptIdFromTask(prolistDefect);
        prolistDefect.setUpdateBy(SecurityUtils.getUsername());
        return prolistDefectMapper.updateProlistDefect(prolistDefect);
    }

    @Override
    public int deleteProlistDefectByProblemIds(Long[] problemIds)
    {
        return prolistDefectMapper.deleteProlistDefectByProblemIds(problemIds);
    }

    @Override
    public boolean checkProblemNoUnique(String problemNo, Long problemId)
    {
        if (StringUtils.isEmpty(problemNo))
        {
            return false;
        }
        return prolistDefectMapper.checkProblemNoUnique(problemNo, problemId) == 0;
    }

    @Override
    public List<Map<String, Object>> selectBatchByYear(String year)
    {
        return prolistDefectMapper.selectBatchByYear(year);
    }

    @Override
    public String selectPlanProductionDate(Long batchId)
    {
        return prolistDefectMapper.selectPlanProductionDate(batchId);
    }

    @Override
    public List<Map<String, Object>> selectTaskOptions(String productionYear, Long batchId, Long deptId)
    {
        return prolistDefectMapper.selectTaskOptions(productionYear, batchId, deptId);
    }

    @Override
    public ProlistDefect selectTaskInfo(Long taskId)
    {
        return prolistDefectMapper.selectTaskInfo(taskId);
    }

    /**
     * 解决时间超一天派生算法（照搬老 yadapm 口径）：
     * base = 解决日期非空 ? 解决日期 : 当天；diff = base − 提交日期；diff > 1 天 → '1' 否则 '0'。
     */
    private String calcSolutionTimeOverOneDay(Date submitDate, Date settleDate)
    {
        if (submitDate == null)
        {
            return "0";
        }
        LocalDate submit = toLocalDate(submitDate);
        LocalDate base = settleDate != null ? toLocalDate(settleDate) : LocalDate.now();
        long diff = ChronoUnit.DAYS.between(submit, base);
        return diff > 1 ? "1" : "0";
    }

    private LocalDate toLocalDate(Date date)
    {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 用任务所属项目的 project_dept 覆盖 dept_id：列表"项目组"过滤(按 dept_id+ancestors)
     * 与任务下拉(按 project_dept)同源，避免"新增搜得到、列表按部门筛不到"的口径不一致。
     * project_dept 存的是部门 id 的字符串，解析为 Long；解析失败则保留前端传入值。
     */
    private void syncDeptIdFromTask(ProlistDefect prolistDefect)
    {
        if (prolistDefect.getTaskId() == null)
        {
            return;
        }
        String projectDept = prolistDefectMapper.selectTaskProjectDept(prolistDefect.getTaskId());
        if (StringUtils.isNotEmpty(projectDept))
        {
            try
            {
                prolistDefect.setDeptId(Long.parseLong(projectDept.trim()));
            }
            catch (NumberFormatException ignore)
            {
                // project_dept 非数字时保留前端传入的 dept_id
            }
        }
    }
}
