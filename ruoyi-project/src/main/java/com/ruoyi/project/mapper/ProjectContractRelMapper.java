package com.ruoyi.project.mapper;

import com.ruoyi.project.domain.ProjectContractRel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目合同关系Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-03
 */
@Mapper
public interface ProjectContractRelMapper
{
    /**
     * 新增项目合同关系
     *
     * @param projectContractRel 项目合同关系
     * @return 结果
     */
    public int insertProjectContractRel(ProjectContractRel projectContractRel);

    /**
     * 删除项目合同关系（根据合同ID）
     *
     * @param contractId 合同ID
     * @return 结果
     */
    public int deleteProjectContractRelByContractId(Long contractId);
}
