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
import com.ruoyi.project.domain.NobatchProlistDefect;
import com.ruoyi.project.mapper.NobatchProlistDefectMapper;
import com.ruoyi.project.service.INobatchProlistDefectService;

/**
 * 非批次任务问题单及缺陷 Service 实现
 *
 * @author yadapm-migrate
 */
@Service
public class NobatchProlistDefectServiceImpl implements INobatchProlistDefectService
{
    @Autowired
    private NobatchProlistDefectMapper nobatchProlistDefectMapper;

    @Override
    public List<NobatchProlistDefect> selectNobatchProlistDefectList(NobatchProlistDefect nobatchProlistDefect)
    {
        return nobatchProlistDefectMapper.selectNobatchProlistDefectList(nobatchProlistDefect);
    }

    @Override
    public NobatchProlistDefect selectNobatchProlistDefectByProblemId(Long problemId)
    {
        return nobatchProlistDefectMapper.selectNobatchProlistDefectByProblemId(problemId);
    }

    @Override
    public int insertNobatchProlistDefect(NobatchProlistDefect d)
    {
        if (!checkProblemNoUnique(d.getProblemNo(), null))
        {
            throw new ServiceException("问题单编号已存在：" + d.getProblemNo());
        }
        d.setSolutionTimeOverOneDay(calcSolutionTimeOverOneDay(d.getSubmitDate(), d.getSettleDate()));
        d.setCreateBy(SecurityUtils.getUsername());
        return nobatchProlistDefectMapper.insertNobatchProlistDefect(d);
    }

    @Override
    public int updateNobatchProlistDefect(NobatchProlistDefect d)
    {
        // 查重排除自己（修复老系统 edit 卡自己的 bug）
        if (!checkProblemNoUnique(d.getProblemNo(), d.getProblemId()))
        {
            throw new ServiceException("问题单编号已存在：" + d.getProblemNo());
        }
        d.setSolutionTimeOverOneDay(calcSolutionTimeOverOneDay(d.getSubmitDate(), d.getSettleDate()));
        d.setUpdateBy(SecurityUtils.getUsername());
        return nobatchProlistDefectMapper.updateNobatchProlistDefect(d);
    }

    @Override
    public int deleteNobatchProlistDefectByProblemIds(Long[] problemIds)
    {
        return nobatchProlistDefectMapper.deleteNobatchProlistDefectByProblemIds(problemIds);
    }

    @Override
    public boolean checkProblemNoUnique(String problemNo, Long problemId)
    {
        if (StringUtils.isEmpty(problemNo))
        {
            return false;
        }
        return nobatchProlistDefectMapper.checkProblemNoUnique(problemNo, problemId) == 0;
    }

    @Override
    public List<Map<String, Object>> selectBatchByYear(String year)
    {
        return nobatchProlistDefectMapper.selectBatchByYear(year);
    }

    @Override
    public String selectPlanProductionDate(Long batchId)
    {
        return nobatchProlistDefectMapper.selectPlanProductionDate(batchId);
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
}
