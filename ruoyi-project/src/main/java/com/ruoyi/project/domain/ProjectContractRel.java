package com.ruoyi.project.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目合同关系对象 pm_project_contract_rel
 *
 * @author ruoyi
 * @date 2026-02-03
 */
public class ProjectContractRel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 关系主键ID */
    private Long relId;

    /** 项目ID */
    private String projectId;

    /** 合同ID */
    private Long contractId;

    /** 关系状态(有效、失效) */
    private String relStatus;

    /** 关联日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date bindDate;

    /** 删除标志(0正常 1删除) */
    private String delFlag;

    public void setRelId(Long relId)
    {
        this.relId = relId;
    }

    public Long getRelId()
    {
        return relId;
    }

    public void setProjectId(String projectId)
    {
        this.projectId = projectId;
    }

    public String getProjectId()
    {
        return projectId;
    }

    public void setContractId(Long contractId)
    {
        this.contractId = contractId;
    }

    public Long getContractId()
    {
        return contractId;
    }

    public void setRelStatus(String relStatus)
    {
        this.relStatus = relStatus;
    }

    public String getRelStatus()
    {
        return relStatus;
    }

    public void setBindDate(Date bindDate)
    {
        this.bindDate = bindDate;
    }

    public Date getBindDate()
    {
        return bindDate;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }
}
