package com.ruoyi.project.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.Contract;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同管理Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-03
 */
@Mapper
public interface ContractMapper
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
     * 删除合同管理
     *
     * @param contractId 合同管理主键
     * @return 结果
     */
    public int deleteContractByContractId(Long contractId);

    /**
     * 批量删除合同管理
     *
     * @param contractIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteContractByContractIds(Long[] contractIds);

    /**
     * 搜索合同（用于下拉选择）
     *
     * @param keyword 搜索关键词（合同名称或合同编号）
     * @return 合同列表
     */
    public List<Contract> searchContracts(String keyword);

    /**
     * 查询合同及其付款里程碑列表（用于付款里程碑查询页面）
     *
     * @param contract 查询条件
     * @return 合同及付款里程碑列表
     */
    public List<Contract> selectContractWithPaymentsList(Contract contract);

    /**
     * 统计付款里程碑总金额（用于付款里程碑查询页面）
     *
     * @param contract 查询条件
     * @return 付款总金额
     */
    public BigDecimal sumPaymentAmount(Contract contract);
}
