package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.ProductionBatch;

/**
 * 投产批次管理Service接口
 * 
 * @author ruoyi
 * @date 2026-03-11
 */
public interface IProductionBatchService 
{
    /**
     * 查询投产批次管理
     * 
     * @param batchId 投产批次管理主键
     * @return 投产批次管理
     */
    public ProductionBatch selectProductionBatchByBatchId(Long batchId);

    /**
     * 查询投产批次管理列表
     * 
     * @param productionBatch 投产批次管理
     * @return 投产批次管理集合
     */
    public List<ProductionBatch> selectProductionBatchList(ProductionBatch productionBatch);

    /**
     * 新增投产批次管理
     * 
     * @param productionBatch 投产批次管理
     * @return 结果
     */
    public int insertProductionBatch(ProductionBatch productionBatch);

    /**
     * 修改投产批次管理
     * 
     * @param productionBatch 投产批次管理
     * @return 结果
     */
    public int updateProductionBatch(ProductionBatch productionBatch);

    /**
     * 批量删除投产批次管理
     * 
     * @param batchIds 需要删除的投产批次管理主键集合
     * @return 结果
     */
    public int deleteProductionBatchByBatchIds(Long[] batchIds);

    /**
     * 删除投产批次管理信息
     *
     * @param batchId 投产批次管理主键
     * @return 结果
     */
    public int deleteProductionBatchByBatchId(Long batchId);

    /**
     * 查询所有批次号（去重，用于下拉选）
     */
    public List<String> selectBatchNoOptions();

    /** 按投产年份查询批次列表（用于联动下拉） */
    public List<ProductionBatch> selectByYear(String year);
}
