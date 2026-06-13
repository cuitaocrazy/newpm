package com.ruoyi.project.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.domain.VersionOut;
import com.ruoyi.project.domain.VersionOutTask;
import com.ruoyi.project.mapper.VersionOutMapper;
import com.ruoyi.project.service.IVersionOutService;

/**
 * 出入库版本 Service 实现
 *
 * @author ruoyi
 * @date 2026-06-11
 */
@Service
public class VersionOutServiceImpl implements IVersionOutService
{
    /** 出入库版本号唯一冲突重试次数 */
    private static final int MAX_RETRY = 3;

    @Autowired
    private VersionOutMapper versionOutMapper;

    @Autowired
    private VersionNumberGenerator versionNumberGenerator;

    @Override
    public List<VersionOut> selectVersionOutList(VersionOut versionOut)
    {
        return versionOutMapper.selectVersionOutList(versionOut);
    }

    @Override
    public VersionOut selectVersionOutById(Long id)
    {
        return versionOutMapper.selectVersionOutById(id);
    }

    @Override
    @Transactional
    public int insertVersionOut(VersionOut versionOut)
    {
        versionOut.setManualInput("0");
        versionOut.setCreateBy(SecurityUtils.getUsername());
        if (StringUtils.isEmpty(versionOut.getCommName()))
        {
            versionOut.setCommName(SecurityUtils.getUsername());
        }

        DuplicateKeyException last = null;
        for (int attempt = 0; attempt < MAX_RETRY; attempt++)
        {
            // 新增场景 addFlag=1
            String[] gen = versionNumberGenerator.generate(versionOut.getSysName(), versionOut.getSubVersionCode(),
                    versionOut.getVersionType(), versionOut.getOutVersion(), "1", null, null, null);
            versionOut.setOutLibVersion(gen[0]);
            versionOut.setVersionCode(gen[1]);
            try
            {
                int rows = versionOutMapper.insertVersionOut(versionOut);
                insertTasks(versionOut);
                return rows;
            }
            catch (DuplicateKeyException e)
            {
                last = e; // 撞号，重新生成重试
            }
        }
        throw new ServiceException("出入库版本号生成冲突，请重试");
    }

    @Override
    @Transactional
    public int updateVersionOut(VersionOut versionOut)
    {
        VersionOut old = versionOutMapper.selectVersionOutById(versionOut.getId());
        if (old == null)
        {
            throw new ServiceException("版本记录不存在");
        }
        // 关键字段变更则重算版本号，否则沿用原号
        boolean keyChanged = !StringUtils.equals(old.getSysName(), versionOut.getSysName())
                || !StringUtils.equals(old.getVersionType(), versionOut.getVersionType())
                || !StringUtils.equals(old.getSubVersionCode(), versionOut.getSubVersionCode());
        if (keyChanged)
        {
            String[] gen = versionNumberGenerator.generate(versionOut.getSysName(), versionOut.getSubVersionCode(),
                    versionOut.getVersionType(), versionOut.getOutVersion(), "2", versionOut.getId(),
                    old.getSubVersionCode(), old.getVersionType());
            versionOut.setOutLibVersion(gen[0]);
            versionOut.setVersionCode(gen[1]);
        }
        else
        {
            versionOut.setOutLibVersion(old.getOutLibVersion());
            versionOut.setVersionCode(old.getVersionCode());
        }
        versionOut.setUpdateBy(SecurityUtils.getUsername());
        int rows = versionOutMapper.updateVersionOut(versionOut);
        // 任务行整体替换
        versionOutMapper.deleteVersionOutTaskByVersionId(versionOut.getId());
        insertTasks(versionOut);
        return rows;
    }

    @Override
    @Transactional
    public int deleteVersionOutByIds(Long[] ids)
    {
        versionOutMapper.deleteVersionOutTaskByVersionIds(ids);
        return versionOutMapper.deleteVersionOutByIds(ids);
    }

    /** 级联插入任务行（task_id 可空，仅存关联键） */
    private void insertTasks(VersionOut versionOut)
    {
        List<VersionOutTask> taskList = versionOut.getTaskList();
        if (taskList == null || taskList.isEmpty())
        {
            return;
        }
        List<VersionOutTask> toInsert = new ArrayList<>();
        for (VersionOutTask t : taskList)
        {
            if (t.getTaskId() == null)
            {
                continue;
            }
            t.setVersionId(versionOut.getId());
            toInsert.add(t);
        }
        if (!toInsert.isEmpty())
        {
            versionOutMapper.batchInsertVersionOutTask(toInsert);
        }
    }

    // ---------- 非批次（manual_input='1'，任务手填存主表列，复用版本号生成+重试） ----------

    @Override
    public List<VersionOut> selectVersionOutManualList(VersionOut versionOut)
    {
        return versionOutMapper.selectVersionOutManualList(versionOut);
    }

    /** 非批次必填校验（实体 @NotBlank 是批次专用字段，非批次手动校验真正必填项） */
    private void validateManual(VersionOut v)
    {
        if (StringUtils.isEmpty(v.getProductionYear())) throw new ServiceException("投产年份不能为空");
        if (v.getBatchId() == null) throw new ServiceException("投产批次不能为空");
        if (StringUtils.isEmpty(v.getProduct())) throw new ServiceException("产品不能为空");
        if (StringUtils.isEmpty(v.getSysName())) throw new ServiceException("子系统不能为空");
        if (StringUtils.isEmpty(v.getVersionType())) throw new ServiceException("版本类型不能为空");
        if (StringUtils.isEmpty(v.getManualTaskNo())) throw new ServiceException("软件中心任务号不能为空");
        if (StringUtils.isEmpty(v.getManualTaskName())) throw new ServiceException("任务名称不能为空");
        if (StringUtils.isEmpty(v.getIsInvolved())) throw new ServiceException("请选择是否涉及TWS改造");
        if (StringUtils.isEmpty(v.getDbUpdate())) throw new ServiceException("请选择数据库是否修改");
        if (StringUtils.isEmpty(v.getUsbUpdate())) throw new ServiceException("请选择接口是否修改");
        if (StringUtils.isEmpty(v.getVersionDescr())) throw new ServiceException("版本说明不能为空");
        if (("5".equals(v.getVersionType()) || "6".equals(v.getVersionType())) && StringUtils.isEmpty(v.getOutVersion()))
            throw new ServiceException("请选择升级包初级版本号");
    }

    @Override
    @Transactional
    public int insertVersionOutManual(VersionOut versionOut)
    {
        validateManual(versionOut);
        versionOut.setManualInput("1");
        versionOut.setCreateBy(SecurityUtils.getUsername());
        if (StringUtils.isEmpty(versionOut.getCommName()))
        {
            versionOut.setCommName(SecurityUtils.getUsername());
        }
        for (int attempt = 0; attempt < MAX_RETRY; attempt++)
        {
            String[] gen = versionNumberGenerator.generate(versionOut.getSysName(), versionOut.getSubVersionCode(),
                    versionOut.getVersionType(), versionOut.getOutVersion(), "1", null, null, null);
            versionOut.setOutLibVersion(gen[0]);
            versionOut.setVersionCode(gen[1]);
            try
            {
                return versionOutMapper.insertVersionOutManual(versionOut);
            }
            catch (DuplicateKeyException e)
            {
                // 撞号，重新生成重试
            }
        }
        throw new ServiceException("出入库版本号生成冲突，请重试");
    }

    @Override
    @Transactional
    public int updateVersionOutManual(VersionOut versionOut)
    {
        validateManual(versionOut);
        VersionOut old = versionOutMapper.selectVersionOutById(versionOut.getId());
        if (old == null)
        {
            throw new ServiceException("版本记录不存在");
        }
        boolean keyChanged = !StringUtils.equals(old.getSysName(), versionOut.getSysName())
                || !StringUtils.equals(old.getVersionType(), versionOut.getVersionType())
                || !StringUtils.equals(old.getSubVersionCode(), versionOut.getSubVersionCode());
        if (keyChanged)
        {
            String[] gen = versionNumberGenerator.generate(versionOut.getSysName(), versionOut.getSubVersionCode(),
                    versionOut.getVersionType(), versionOut.getOutVersion(), "2", versionOut.getId(),
                    old.getSubVersionCode(), old.getVersionType());
            versionOut.setOutLibVersion(gen[0]);
            versionOut.setVersionCode(gen[1]);
        }
        else
        {
            versionOut.setOutLibVersion(old.getOutLibVersion());
            versionOut.setVersionCode(old.getVersionCode());
        }
        versionOut.setUpdateBy(SecurityUtils.getUsername());
        return versionOutMapper.updateVersionOutManual(versionOut);
    }

    @Override
    public Map<String, String> generateOutLibVersion(VersionOut versionOut, String addFlag,
            String oldSubVersionCode, String oldVersionType)
    {
        String[] gen = versionNumberGenerator.generate(versionOut.getSysName(), versionOut.getSubVersionCode(),
                versionOut.getVersionType(), versionOut.getOutVersion(),
                StringUtils.isEmpty(addFlag) ? "1" : addFlag, versionOut.getId(), oldSubVersionCode, oldVersionType);
        Map<String, String> result = new HashMap<>();
        result.put("outLibVersion", gen[0]);
        result.put("versionCode", gen[1]);
        return result;
    }

    @Override
    public List<Map<String, Object>> selectBatchByYear(String productionYear)
    {
        return versionOutMapper.selectBatchByYear(productionYear);
    }

    @Override
    public String selectVersionPDate(Long batchId)
    {
        return versionOutMapper.selectVersionPDate(batchId);
    }

    @Override
    public VersionOutTask selectTaskInfoByDemandNo(String softwareDemandNo)
    {
        return versionOutMapper.selectTaskInfoByDemandNo(softwareDemandNo);
    }

    @Override
    public List<String> selectOutVersionOptions(String sysName, String versionType)
    {
        return versionOutMapper.selectOutVersionOptions(sysName, versionType);
    }

    @Override
    public List<VersionOutTask> selectTaskOptions(String productionYear, Long batchId, String product)
    {
        return versionOutMapper.selectTaskOptions(productionYear, batchId, product);
    }
}
