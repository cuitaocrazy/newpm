package com.ruoyi.project.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
     * 按数据权限搜索合同编号/合同名称（用于项目管理、公司收入确认页面的查询条件 autoComplete）
     * 通过 d/u1 别名配合 @DataScope 注入数据权限，仅返回当前用户有权查看的合同
     *
     * @param contract 携带 contractCode / contractName 模糊关键词及 params.dataScope
     * @return 精简字段：contractId, contractCode, contractName
     */
    public List<Map<String, Object>> searchContractsForFilter(Contract contract);

    /**
     * 按部门查询合同（轻量，用于关联合同选择器）
     */
    public List<Map<String, Object>> listContractsByDept(@org.apache.ibatis.annotations.Param("deptId") Long deptId,
                                                         @org.apache.ibatis.annotations.Param("keyword") String keyword);

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

    /**
     * 根据项目ID查询关联的合同ID
     *
     * @param projectId 项目ID
     * @return 合同ID，无关联合同时返回null
     */
    public Long selectContractIdByProjectId(Long projectId);

    /**
     * 合同编号判重专用：按「归一化后的编号」精确统计在用合同数量
     *
     * <p>实现见 ContractMapper.xml 同名语句，调用方为
     * {@code ContractServiceImpl.checkContractCodeUnique} 与
     * {@code ContractServiceImpl.normalizeAndCheckContractCode}。
     * 以下是该语句的硬性约束，改动前先读 specs/020-contract-code-unique/plan.md：
     * <ul>
     *   <li>精确匹配，禁止复用 contractFilterConditions 里的 like 模糊匹配（那是列表搜索功能）</li>
     *   <li>SQL 侧归一化必须与 Java 侧逐字一致：MySQL 的 TRIM() 只去空格不去 TAB，
     *       必须显式 REPLACE 掉 CHAR(9)/CHAR(13)/CHAR(10)</li>
     *   <li>只统计在用记录（del_flag = '0'），软删记录不占用编号</li>
     *   <li>不得带 ${params.dataScope} —— 判重必须跳出数据权限，否则跨部门重复检不出来</li>
     * </ul>
     *
     * @param normalizedCode    已归一化的合同编号（调用方保证非空）
     * @param excludeContractId 需要排除的合同ID（编辑时传自身ID，新增时传 null）
     * @return 匹配到的在用合同数量，0 表示编号可用
     */
    public int countByContractCode(@Param("normalizedCode") String normalizedCode,
                                   @Param("excludeContractId") Long excludeContractId);
}
