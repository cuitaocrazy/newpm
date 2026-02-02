package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.ProjectContractRel;
import com.ruoyi.project.mapper.ProjectContractRelMapper;
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
 * @date 2026-02-01
 */
@Service
public class ContractServiceImpl implements IContractService
{
    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ProjectContractRelMapper projectContractRelMapper;

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
        if (contract != null)
        {
            // 查询关联的项目ID列表
            List<Long> projectIds = projectContractRelMapper.selectProjectIdsByContractId(contractId);
            contract.setProjectIds(projectIds);
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
        return contractMapper.selectContractList(contract);
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
        contract.setCreateTime(DateUtils.getNowDate());
        int result = contractMapper.insertContract(contract);

        // 保存项目关联关系
        insertProjectContractRel(contract);

        return result;
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
        contract.setUpdateTime(DateUtils.getNowDate());

        // 删除原有的项目关联关系
        projectContractRelMapper.deleteProjectContractRelByContractId(contract.getContractId());

        // 重新保存项目关联关系
        insertProjectContractRel(contract);

        return contractMapper.updateContract(contract);
    }

    /**
     * 批量删除合同管理（逻辑删除）
     *
     * @param contractIds 需要删除的合同管理主键
     * @return 结果
     */
    @Override
    public int deleteContractByContractIds(Long[] contractIds)
    {
        // 逻辑删除：更新删除标志和更新信息
        int result = 0;
        for (Long contractId : contractIds)
        {
            Contract contract = new Contract();
            contract.setContractId(contractId);
            contract.setDelFlag("1");
            contract.setUpdateBy(SecurityUtils.getUsername());
            contract.setUpdateTime(DateUtils.getNowDate());
            result += contractMapper.updateContract(contract);
        }
        return result;
    }

    /**
     * 删除合同管理信息（逻辑删除）
     *
     * @param contractId 合同管理主键
     * @return 结果
     */
    @Override
    public int deleteContractByContractId(Long contractId)
    {
        // 逻辑删除：更新删除标志和更新信息
        Contract contract = new Contract();
        contract.setContractId(contractId);
        contract.setDelFlag("1");
        contract.setUpdateBy(SecurityUtils.getUsername());
        contract.setUpdateTime(DateUtils.getNowDate());
        return contractMapper.updateContract(contract);
    }

    /**
     * 新增项目合同关联信息
     *
     * @param contract 合同对象
     */
    private void insertProjectContractRel(Contract contract)
    {
        List<Long> projectIds = contract.getProjectIds();
        if (projectIds != null && !projectIds.isEmpty())
        {
            for (Long projectId : projectIds)
            {
                ProjectContractRel rel = new ProjectContractRel();
                rel.setProjectId(String.valueOf(projectId));
                rel.setContractId(contract.getContractId());
                rel.setRelStatus("有效");
                rel.setBindDate(DateUtils.getNowDate());
                rel.setDelFlag("0");
                rel.setCreateBy(SecurityUtils.getUsername());
                rel.setCreateTime(DateUtils.getNowDate());
                projectContractRelMapper.insertProjectContractRel(rel);
            }
        }
    }
}
