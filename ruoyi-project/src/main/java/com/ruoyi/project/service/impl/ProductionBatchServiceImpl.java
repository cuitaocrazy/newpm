package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.ProductionBatchMapper;
import com.ruoyi.project.domain.ProductionBatch;
import com.ruoyi.project.service.IProductionBatchService;

/**
 * 投产批次管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-11
 */
@Service
public class ProductionBatchServiceImpl implements IProductionBatchService 
{
    @Autowired
    private ProductionBatchMapper productionBatchMapper;

    /**
     * 查询投产批次管理
     * 
     * @param batchId 投产批次管理主键
     * @return 投产批次管理
     */
    @Override
    public ProductionBatch selectProductionBatchByBatchId(Long batchId)
    {
        return productionBatchMapper.selectProductionBatchByBatchId(batchId);
    }

    /**
     * 查询投产批次管理列表
     * 
     * @param productionBatch 投产批次管理
     * @return 投产批次管理
     */
    @Override
    public List<ProductionBatch> selectProductionBatchList(ProductionBatch productionBatch)
    {
        return productionBatchMapper.selectProductionBatchList(productionBatch);
    }

    /**
     * 新增投产批次管理
     * 
     * @param productionBatch 投产批次管理
     * @return 结果
     */
    @Override
    public int insertProductionBatch(ProductionBatch productionBatch)
    {
        productionBatch.setCreateTime(DateUtils.getNowDate());
        return productionBatchMapper.insertProductionBatch(productionBatch);
    }

    /**
     * 修改投产批次管理
     * 
     * @param productionBatch 投产批次管理
     * @return 结果
     */
    @Override
    public int updateProductionBatch(ProductionBatch productionBatch)
    {
        productionBatch.setUpdateTime(DateUtils.getNowDate());
        return productionBatchMapper.updateProductionBatch(productionBatch);
    }

    /**
     * 批量删除投产批次管理
     * 
     * @param batchIds 需要删除的投产批次管理主键
     * @return 结果
     */
    @Override
    public int deleteProductionBatchByBatchIds(Long[] batchIds)
    {
        return productionBatchMapper.deleteProductionBatchByBatchIds(batchIds);
    }

    /**
     * 删除投产批次管理信息
     *
     * @param batchId 投产批次管理主键
     * @return 结果
     */
    @Override
    public int deleteProductionBatchByBatchId(Long batchId)
    {
        return productionBatchMapper.deleteProductionBatchByBatchId(batchId);
    }

    @Override
    public List<String> selectBatchNoOptions()
    {
        return productionBatchMapper.selectBatchNoOptions();
    }
}
