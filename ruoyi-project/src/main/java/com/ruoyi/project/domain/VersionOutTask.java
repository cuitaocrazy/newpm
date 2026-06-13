package com.ruoyi.project.domain;

/**
 * 出入库版本-任务关联对象 pm_version_out_task
 * 最小化设计：仅持久 version_id/task_id；回显字段由 JOIN pm_task/pm_project 填充。
 *
 * @author ruoyi
 * @date 2026-06-11
 */
public class VersionOutTask
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 版本id(pm_version_out.id) */
    private Long versionId;

    /** 任务id(pm_task.task_id) */
    private Long taskId;

    /** 软件中心任务号(回显, JOIN pm_task.software_demand_no) */
    private String taskNo;

    /** 任务名称(回显, JOIN pm_task.task_name) */
    private String taskName;

    /** 项目名称(回显, JOIN pm_project.project_name) */
    private String prjName;

    /** 需求名称(回显, JOIN pm_task.demand_name) */
    private String demandName;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Long getVersionId() { return versionId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getTaskId() { return taskId; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
    public String getTaskNo() { return taskNo; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskName() { return taskName; }
    public void setPrjName(String prjName) { this.prjName = prjName; }
    public String getPrjName() { return prjName; }
    public void setDemandName(String demandName) { this.demandName = demandName; }
    public String getDemandName() { return demandName; }
}
