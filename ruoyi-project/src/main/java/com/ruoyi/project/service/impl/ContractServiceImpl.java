package com.ruoyi.project.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.ProjectContractRel;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.mapper.ProjectContractRelMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.ContractMapper;
import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.service.IContractService;

/**
 * 合同管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-03
 */
@Service
public class ContractServiceImpl implements IContractService
{
    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ProjectContractRelMapper projectContractRelMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private com.ruoyi.project.mapper.PaymentMapper paymentMapper;

    @Autowired
    private com.ruoyi.project.mapper.AttachmentMapper attachmentMapper;

    /**
     * 查询合同管理
     *
     * @param contractId 合同管理主键
     * @return 合同管理
     */
    @Override
    public Contract selectContractByContractId(Long contractId)
    {
        Contract contract = contractMapper.selectContractByContractId(contractId);
        if (contract != null) {
            // 查询关联项目列表
            List<Project> projectList = projectMapper.selectProjectListByContractId(contractId);
            contract.setProjectList(projectList);

            // 提取项目ID列表（用于编辑页面回显）
            if (projectList != null && !projectList.isEmpty()) {
                List<Long> projectIds = new java.util.ArrayList<>();
                for (Project project : projectList) {
                    projectIds.add(project.getProjectId());
                }
                contract.setProjectIds(projectIds);
            }

            // 查询创建人和更新人姓名
            if (contract.getCreateBy() != null) {
                SysUser createUser = userMapper.selectUserByUserName(contract.getCreateBy());
                if (createUser != null) {
                    contract.setCreateByName(createUser.getNickName());
                }
            }
            if (contract.getUpdateBy() != null) {
                SysUser updateUser = userMapper.selectUserByUserName(contract.getUpdateBy());
                if (updateUser != null) {
                    contract.setUpdateByName(updateUser.getNickName());
                }
            }
        }
        return contract;
    }

    /**
     * 查询合同管理列表
     *
     * @param contract 合同管理
     * @return 合同管理
     */
    @Override
    public List<Contract> selectContractList(Contract contract)
    {
        List<Contract> contractList = contractMapper.selectContractList(contract);
        // 为每个合同查询关联项目列表
        for (Contract c : contractList) {
            List<Project> projectList = projectMapper.selectProjectListByContractId(c.getContractId());
            c.setProjectList(projectList);
        }
        return contractList;
    }

    /**
     * 查询合同金额总计
     *
     * @param contract 查询条件
     * @return 总计数据
     */
    @Override
    public Map<String, BigDecimal> selectContractSummary(Contract contract)
    {
        return contractMapper.selectContractSummary(contract);
    }

    /**
     * 新增合同管理
     *
     * @param contract 合同管理
     * @return 结果
     */
    @Override
    @Transactional
    public int insertContract(Contract contract)
    {
        // 计算不含税金额和税金
        if (contract.getContractAmount() != null && contract.getTaxRate() != null) {
            BigDecimal taxRate = contract.getTaxRate().divide(new BigDecimal(100));
            BigDecimal amountNoTax = contract.getContractAmount().divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = contract.getContractAmount().subtract(amountNoTax);
            contract.setAmountNoTax(amountNoTax);
            contract.setTaxAmount(taxAmount);
        }

        contract.setDelFlag("0");
        contract.setCreateBy(SecurityUtils.getUsername());
        contract.setCreateTime(DateUtils.getNowDate());
        int rows = contractMapper.insertContract(contract);

        // 保存关联项目
        insertProjectContractRel(contract);

        return rows;
    }

    /**
     * 新增项目合同关联信息
     */
    private void insertProjectContractRel(Contract contract)
    {
        List<Long> projectIds = contract.getProjectIds();
        if (projectIds != null && !projectIds.isEmpty()) {
            for (Long projectId : projectIds) {
                ProjectContractRel rel = new ProjectContractRel();
                rel.setProjectId(projectId.toString());
                rel.setContractId(contract.getContractId());
                rel.setRelStatus("有效");
                rel.setBindDate(new Date());
                rel.setDelFlag("0");
                rel.setCreateBy(SecurityUtils.getUsername());
                rel.setCreateTime(DateUtils.getNowDate());
                projectContractRelMapper.insertProjectContractRel(rel);
            }
        }
    }

    /**
     * 修改合同管理
     *
     * @param contract 合同管理
     * @return 结果
     */
    @Override
    @Transactional
    public int updateContract(Contract contract)
    {
        // 计算不含税金额和税金
        if (contract.getContractAmount() != null && contract.getTaxRate() != null) {
            BigDecimal taxRate = contract.getTaxRate().divide(new BigDecimal(100));
            BigDecimal amountNoTax = contract.getContractAmount().divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = contract.getContractAmount().subtract(amountNoTax);
            contract.setAmountNoTax(amountNoTax);
            contract.setTaxAmount(taxAmount);
        }

        contract.setUpdateBy(SecurityUtils.getUsername());
        contract.setUpdateTime(DateUtils.getNowDate());
        int rows = contractMapper.updateContract(contract);

        // 删除旧的关联项目
        projectContractRelMapper.deleteProjectContractRelByContractId(contract.getContractId());

        // 保存新的关联项目
        insertProjectContractRel(contract);

        return rows;
    }

    /**
     * 批量删除合同管理
     *
     * @param contractIds 需要删除的合同管理主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteContractByContractIds(Long[] contractIds)
    {
        for (Long contractId : contractIds) {
            Contract contract = contractMapper.selectContractByContractId(contractId);
            if (contract == null) {
                throw new ServiceException("合同不存在");
            }

            String contractName = contract.getContractName();

            // 检查是否有关联款项
            int paymentCount = paymentMapper.countPaymentByContractId(contractId);
            if (paymentCount > 0) {
                throw new ServiceException("【" + contractName + "】下已有款项里程碑信息，不可进行删除操作！");
            }

            // 检查是否有关联附件
            int attachmentCount = attachmentMapper.countAttachmentByBusiness("contract", contractId);
            if (attachmentCount > 0) {
                throw new ServiceException("【" + contractName + "】已上传附件信息，不可进行删除操作！");
            }

            // 物理删除项目关联关系
            projectContractRelMapper.deleteProjectContractRelByContractId(contractId);

            // 逻辑删除合同
            contract.setDelFlag("1");
            contract.setUpdateBy(SecurityUtils.getUsername());
            contract.setUpdateTime(DateUtils.getNowDate());
            contractMapper.updateContract(contract);
        }
        return contractIds.length;
    }

    /**
     * 删除合同管理信息
     *
     * @param contractId 合同管理主键
     * @return 结果
     */
    @Override
    public int deleteContractByContractId(Long contractId)
    {
        return contractMapper.deleteContractByContractId(contractId);
    }

    /**
     * 搜索合同（用于下拉选择）
     *
     * @param keyword 搜索关键词（合同名称或合同编号）
     * @return 合同列表
     */
    @Override
    public List<Contract> searchContracts(String keyword)
    {
        return contractMapper.searchContracts(keyword);
    }

    /**
     * 检查合同名称是否唯一
     *
     * @param contractName 合同名称
     * @param contractId 合同ID（编辑时传入，新增时为null）
     * @return true-唯一，false-不唯一
     */
    @Override
    public boolean checkContractNameUnique(String contractName, Long contractId)
    {
        if (contractName == null || contractName.trim().isEmpty()) {
            return false;
        }

        Contract contract = new Contract();
        contract.setContractName(contractName.trim());
        List<Contract> list = contractMapper.selectContractList(contract);

        // 如果没有找到同名合同，则唯一
        if (list == null || list.isEmpty()) {
            return true;
        }

        // 如果是编辑模式，排除自己
        if (contractId != null) {
            for (Contract c : list) {
                if (!contractId.equals(c.getContractId())) {
                    // 找到其他同名合同，不唯一
                    return false;
                }
            }
            // 只找到自己，唯一
            return true;
        }

        // 新增模式，找到同名合同，不唯一
        return false;
    }

    /**
     * 查询合同及其付款里程碑列表（用于付款里程碑查询页面）
     *
     * @param contract 查询条件
     * @return 合同及付款里程碑列表
     */
    @Override
    public List<Contract> selectContractWithPaymentsList(Contract contract)
    {
        return contractMapper.selectContractWithPaymentsList(contract);
    }

    /**
     * 统计付款里程碑总金额（用于付款里程碑查询页面）
     *
     * @param contract 查询条件
     * @return 付款总金额
     */
    @Override
    public BigDecimal sumPaymentAmount(Contract contract)
    {
        return contractMapper.sumPaymentAmount(contract);
    }
}
