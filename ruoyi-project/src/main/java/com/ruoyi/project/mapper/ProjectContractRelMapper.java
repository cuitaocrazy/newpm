package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.ProjectContractRel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目合同关联Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-01
 */
@Mapper
public interface ProjectContractRelMapper
{
    /**
     * 查询项目合同关联列表
     *
     * @param projectContractRel 项目合同关联
     * @return 项目合同关联集合
     */
    public List<ProjectContractRel> selectProjectContractRelList(ProjectContractRel projectContractRel);

    /**
     * 根据合同ID查询关联的项目ID列表
     *
     * @param contractId 合同ID
     * @return 项目ID列表
     */
    public List<Long> selectProjectIdsByContractId(Long contractId);

    /**
     * 新增项目合同关联
     *
     * @param projectContractRel 项目合同关联
     * @return 结果
     */
    public int insertProjectContractRel(ProjectContractRel projectContractRel);

    /**
     * 批量删除项目合同关联（根据合同ID）
     *
     * @param contractId 合同ID
     * @return 结果
     */
    public int deleteProjectContractRelByContractId(Long contractId);
}
