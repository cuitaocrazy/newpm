package com.ruoyi.project.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.Contract;

/**
 * 合同管理Service接口
 *
 * @author ruoyi
 * @date 2026-02-03
 */
public interface IContractService
{
    /**
     * 查询合同管理
     *
     * @param contractId 合同管理主键
     * @return 合同管理
     */
    public Contract selectContractByContractId(Long contractId);

    /**
     * 查询合同管理列表
     *
     * @param contract 合同管理
     * @return 合同管理集合
     */
    public List<Contract> selectContractList(Contract contract);

    /**
     * 查询合同金额总计
     *
     * @param contract 查询条件
     * @return 总计数据
     */
    public Map<String, BigDecimal> selectContractSummary(Contract contract);

    /**
     * 新增合同管理
     *
     * @param contract 合同管理
     * @return 结果
     */
    public int insertContract(Contract contract);

    /**
     * 修改合同管理
     *
     * @param contract 合同管理
     * @return 结果
     */
    public int updateContract(Contract contract);

    /**
     * 批量删除合同管理
     *
     * @param contractIds 需要删除的合同管理主键集合
     * @return 结果
     */
    public int deleteContractByContractIds(Long[] contractIds);

    /**
     * 删除合同管理信息
     *
     * @param contractId 合同管理主键
     * @return 结果
     */
    public int deleteContractByContractId(Long contractId);

    /**
     * 搜索合同（用于下拉选择）
     *
     * @param keyword 搜索关键词（合同名称或合同编号）
     * @return 合同列表
     */
    public List<Contract> searchContracts(String keyword);

    /**
     * 检查合同名称是否唯一
     *
     * @param contractName 合同名称
     * @param contractId 合同ID（编辑时传入，新增时为null）
     * @return true-唯一，false-不唯一
     */
    public boolean checkContractNameUnique(String contractName, Long contractId);
}
