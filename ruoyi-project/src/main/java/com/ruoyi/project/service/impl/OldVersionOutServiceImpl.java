package com.ruoyi.project.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.domain.OldVersionOut;
import com.ruoyi.project.mapper.OldVersionOutMapper;
import com.ruoyi.project.service.IOldVersionOutService;

/**
 * 出入库版本旧数据查询 Service 实现（纯转发，无业务逻辑）
 *
 * @author yadapm-migrate
 */
@Service
public class OldVersionOutServiceImpl implements IOldVersionOutService
{
    @Autowired
    private OldVersionOutMapper oldVersionOutMapper;

    @Override
    public List<OldVersionOut> selectOldVersionOutList(OldVersionOut oldVersionOut)
    {
        return oldVersionOutMapper.selectOldVersionOutList(oldVersionOut);
    }

    @Override
    public List<String> selectProBatchNoOptions()
    {
        return oldVersionOutMapper.selectProBatchNoOptions();
    }

    @Override
    public List<String> selectProductOptions()
    {
        return oldVersionOutMapper.selectProductOptions();
    }

    @Override
    public List<String> selectVersionTypeOptions()
    {
        return oldVersionOutMapper.selectVersionTypeOptions();
    }
}
