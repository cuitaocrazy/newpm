# 任务管理表拆分实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 `pm_project` 自引用表中的任务数据（project_level=1）拆分到独立的 `pm_task` 表，不影响现有所有功能（项目管理、合同、收入确认、审核、日报等）。

**Architecture:** 新建 `pm_task` 表保存任务数据，通过 `project_id` 外键关联主项目（一个项目可有多个任务）。`pm_daily_report_detail.sub_project_id` 的值不变（迁移时保留原 project_id 作为 task_id），日报关联零风险。分阶段部署：先建表迁移，再换后端，再换前端，最后清理旧数据。

**Tech Stack:** Java 17 / Spring Boot 3 / MyBatis / Vue 3 / TypeScript / MySQL 8

---

## 关键设计决策

| 决策项 | 选择 | 原因 |
|--------|------|------|
| task_id 值 | = 原 project_id 值 | `pm_daily_report_detail.sub_project_id` 无需更新 |
| 删除策略 | 硬删除（无 del_flag） | 任务生命周期简单 |
| 审核 | 不需要 | 任务无审核流程 |
| 团队收入确认 | 不需要 | 任务不参与收入确认 |
| 成员 | 继承父项目，不维护 pm_project_member | 不变 |
| 一个项目多任务 | 一对多关系（project_id FK） | pm_task.project_id → pm_project.project_id |

---

## 字段映射（旧 pm_project → 新 pm_task）

| pm_project 字段 | pm_task 字段 | 说明 |
|----------------|-------------|------|
| project_id | task_id | **保留原值**，AUTO_INCREMENT 跟随最大值 |
| parent_id | project_id | 改名，语义更清晰 |
| project_name | task_name | 改名 |
| task_code | task_code | 不变 |
| project_stage | task_stage | 改名 |
| project_manager_id | task_manager_id | 改名 |
| project_budget | task_budget | 改名 |
| estimated_workload | estimated_workload | 不变 |
| actual_workload | actual_workload | 不变（日报汇总小时数） |
| production_year | production_year | 不变 |
| batch_id | batch_id | 不变 |
| bank_demand_no | bank_demand_no | 不变 |
| software_demand_no | software_demand_no | 不变 |
| product | product | 不变（改为 varchar(50) 字典值） |
| project_plan | task_plan | 改名 |
| project_description | task_description | 改名 |
| start_date | start_date | 不变 |
| end_date | end_date | 不变 |
| production_date | production_date | 不变 |
| internal_closure_date | internal_closure_date | 改名（原 internal_b_package_date） |
| functional_test_date | functional_test_date | 不变 |
| production_version_date | production_version_date | 不变 |
| actual_production_date | actual_production_date | 不变 |
| schedule_status | schedule_status | 不变 |
| function_description | function_description | 改名（原 function_point_desc） |
| implementation_plan | implementation_plan | 不变 |
| create_by/time | create_by/time | 不变（BaseEntity） |
| update_by/time | update_by/time | 不变（BaseEntity） |
| remark | remark | 不变 |

**新增字段（pm_project 中没有，需前端补充录入）：**
- task_plan（之前存在 project_plan，迁移过来）
- task_description（之前存在 project_description，迁移过来）
- task_budget（之前存在 project_budget，迁移过来）

---

## 影响范围总览

### 后端需改动文件

| 文件 | 操作 |
|------|------|
| `ruoyi-project/src/main/java/com/ruoyi/project/domain/Task.java` | **新建** |
| `ruoyi-project/src/main/java/com/ruoyi/project/mapper/TaskMapper.java` | **新建** |
| `ruoyi-project/src/main/resources/mapper/project/TaskMapper.xml` | **新建** |
| `ruoyi-project/src/main/java/com/ruoyi/project/service/ITaskService.java` | **新建** |
| `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/TaskServiceImpl.java` | **新建** |
| `ruoyi-project/src/main/java/com/ruoyi/project/controller/TaskController.java` | **新建** |
| `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java` | **修改**（selectMyProjects、saveDailyReport） |
| `ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml` | **修改**（JOIN pm_task 替换 pm_project） |
| `ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java` | **修改**（删除 sub-project 相关方法） |
| `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml` | **修改**（删除 sub-project 相关 SQL） |
| `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java` | **修改**（删除 subList/subProjectOptions/siblingTasks 接口） |
| `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java` | **修改**（删除 sub-project 相关方法） |

### 前端需改动文件

| 文件 | 操作 |
|------|------|
| `ruoyi-ui/src/api/project/task.ts` | **新建** |
| `ruoyi-ui/src/views/project/subproject/index.vue` | **修改**（改用 task API，字段名更新） |
| `ruoyi-ui/src/views/project/subproject/add.vue` | **修改**（改用 task API，字段名更新） |
| `ruoyi-ui/src/views/project/subproject/edit.vue` | **修改**（改用 task API，字段名更新） |
| `ruoyi-ui/src/views/project/subproject/detail.vue` | **修改**（改用 task API，字段名更新） |
| `ruoyi-ui/src/views/project/subproject/decompose.vue` | **修改**（改用 task API，字段名更新） |
| `ruoyi-ui/src/views/project/dailyReport/write.vue` | **修改**（task options API，taskId 字段） |

### 数据库

| 操作 | 时机 |
|------|------|
| 创建 pm_task 表 | Phase 1（最先） |
| 迁移 pm_project level=1 数据到 pm_task | Phase 1 |
| 验证迁移数据 | Phase 1 |
| 删除 pm_project 中 project_level=1 数据 | Phase 5（最后，验证无误后） |

---

## 实施计划

### Phase 1：数据库建表与迁移（可独立执行，不影响现有功能）

#### Task 1.1：创建 pm_task 表

**Files:**
- Create: `pm-sql/fix_task_table_split_2026.sql`

**Step 1: 编写建表 SQL**

```sql
-- pm-sql/fix_task_table_split_2026.sql
-- ============================================================
-- Phase 1: 创建 pm_task 表并迁移数据
-- ============================================================

-- 1. 创建任务表
CREATE TABLE IF NOT EXISTS `pm_task` (
  `task_id`                 bigint        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `project_id`              bigint        NOT NULL               COMMENT '所属主项目ID(FK→pm_project.project_id)',
  `task_code`               varchar(50)   DEFAULT NULL           COMMENT '任务编号',
  `task_name`               varchar(200)  NOT NULL               COMMENT '任务名称',
  `task_stage`              varchar(50)   DEFAULT NULL           COMMENT '任务阶段(sys_xmjd)',
  `task_manager_id`         bigint        DEFAULT NULL           COMMENT '任务负责人ID(FK→sys_user.user_id)',
  `product`                 varchar(50)   DEFAULT NULL           COMMENT '产品(sys_product)',
  `bank_demand_no`          varchar(100)  DEFAULT NULL           COMMENT '总行需求号',
  `software_demand_no`      varchar(100)  DEFAULT NULL           COMMENT '软件中心需求编号',
  `task_budget`             decimal(15,2) DEFAULT NULL           COMMENT '任务预算(元)',
  `estimated_workload`      decimal(10,2) DEFAULT NULL           COMMENT '预估工作量(人天)',
  `actual_workload`         decimal(10,2) DEFAULT 0              COMMENT '实际工作量(小时，由日报汇总)',
  `production_year`         varchar(10)   DEFAULT NULL           COMMENT '投产年度(sys_ndgl)',
  `batch_id`                bigint        DEFAULT NULL           COMMENT '投产批次ID(FK→pm_production_batch.batch_id)',
  `task_plan`               text          DEFAULT NULL           COMMENT '任务计划',
  `task_description`        text          DEFAULT NULL           COMMENT '任务描述',
  `start_date`              date          DEFAULT NULL           COMMENT '启动日期',
  `end_date`                date          DEFAULT NULL           COMMENT '结束日期',
  `production_date`         date          DEFAULT NULL           COMMENT '投产时间',
  `production_version_date` date          DEFAULT NULL           COMMENT '生产版本日期',
  `actual_production_date`  date          DEFAULT NULL           COMMENT '实际投产日期',
  `internal_closure_date`   date          DEFAULT NULL           COMMENT '内部B包日期',
  `functional_test_date`    date          DEFAULT NULL           COMMENT '功能测试版本日期',
  `schedule_status`         varchar(50)   DEFAULT NULL           COMMENT '排期状态(sys_pqzt)',
  `function_description`    text          DEFAULT NULL           COMMENT '功能点说明',
  `implementation_plan`     text          DEFAULT NULL           COMMENT '实施计划',
  `create_by`               varchar(64)   DEFAULT NULL           COMMENT '创建者',
  `create_time`             datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`               varchar(64)   DEFAULT NULL           COMMENT '更新者',
  `update_time`             datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`                  varchar(500)  DEFAULT NULL           COMMENT '备注',
  PRIMARY KEY (`task_id`),
  KEY `idx_project_id`    (`project_id`),
  KEY `idx_task_stage`    (`task_stage`),
  KEY `idx_schedule_status` (`schedule_status`),
  KEY `idx_create_time`   (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务管理表';

-- 2. 迁移数据（保留原 project_id 作为 task_id，确保日报外键不断链）
INSERT INTO pm_task (
    task_id, project_id, task_code, task_name, task_stage,
    task_manager_id, product, bank_demand_no, software_demand_no,
    task_budget, estimated_workload, actual_workload,
    production_year, batch_id,
    task_plan, task_description,
    start_date, end_date, production_date, production_version_date,
    actual_production_date, internal_closure_date, functional_test_date,
    schedule_status, function_description, implementation_plan,
    create_by, create_time, update_by, update_time, remark
)
SELECT
    p.project_id,
    p.parent_id,
    p.task_code,
    p.project_name,
    p.project_stage,
    p.project_manager_id,
    p.product,
    p.bank_demand_no,
    p.software_demand_no,
    p.project_budget,
    p.estimated_workload,
    p.actual_workload,
    p.production_year,
    p.batch_id,
    p.project_plan,
    p.project_description,
    p.start_date,
    p.end_date,
    p.production_date,
    p.production_version_date,
    p.actual_production_date,
    p.internal_closure_date,
    p.functional_test_date,
    p.schedule_status,
    p.function_description,
    p.implementation_plan,
    p.create_by,
    p.create_time,
    p.update_by,
    p.update_time,
    p.remark
FROM pm_project p
WHERE p.project_level = 1
  AND p.del_flag = '0';

-- 3. 重置 AUTO_INCREMENT（确保新任务 ID 从最大值之后开始）
SET @max_id = (SELECT IFNULL(MAX(task_id), 0) FROM pm_task);
SET @sql = CONCAT('ALTER TABLE pm_task AUTO_INCREMENT = ', @max_id + 1);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 验证（运行后人工确认两个数字相同）
SELECT '迁移前任务数' AS label, COUNT(*) AS cnt FROM pm_project WHERE project_level = 1 AND del_flag = '0'
UNION ALL
SELECT '迁移后任务数', COUNT(*) FROM pm_task;
```

**Step 2: 在本地执行**

```bash
cat pm-sql/fix_task_table_split_2026.sql | docker exec -i 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

**Step 3: 确认两行数字相同**

预期输出：
```
迁移前任务数  | 42
迁移后任务数  | 42
```

**Step 4: 在生产环境（k3s001）执行**

```bash
cat pm-sql/fix_task_table_split_2026.sql | ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
```

**Step 5: Commit**

```bash
git add pm-sql/fix_task_table_split_2026.sql
git commit -m "sql: 创建pm_task表并迁移子项目数据"
```

---

### Phase 2：后端 - 新建 Task 模块

#### Task 2.1：Task 实体类

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/domain/Task.java`

**Step 1: 创建实体类**

```java
package com.ruoyi.project.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 任务管理实体 pm_task
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Task extends BaseEntity {

    /** 任务ID */
    private Long taskId;

    /** 所属主项目ID */
    private Long projectId;

    /** 任务编号 */
    @Excel(name = "任务编号")
    private String taskCode;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String taskName;

    /** 任务阶段 (sys_xmjd) */
    @Excel(name = "任务阶段")
    private String taskStage;

    /** 任务负责人ID */
    private Long taskManagerId;

    /** 产品 (sys_product) */
    @Excel(name = "产品")
    private String product;

    /** 总行需求号 */
    @Excel(name = "总行需求号")
    private String bankDemandNo;

    /** 软件中心需求编号 */
    @Excel(name = "软件中心需求编号")
    private String softwareDemandNo;

    /** 任务预算(元) */
    @Excel(name = "任务预算(元)")
    private BigDecimal taskBudget;

    /** 预估工作量(人天) */
    @Excel(name = "预估工作量(人天)")
    private BigDecimal estimatedWorkload;

    /** 实际工作量(小时，由日报汇总) */
    @Excel(name = "实际工作量(小时)")
    private BigDecimal actualWorkload;

    /** 投产年度 (sys_ndgl) */
    @Excel(name = "投产年度")
    private String productionYear;

    /** 投产批次ID */
    private Long batchId;

    /** 任务计划 */
    private String taskPlan;

    /** 任务描述 */
    private String taskDescription;

    /** 启动日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 投产时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date productionDate;

    /** 生产版本日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date productionVersionDate;

    /** 实际投产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date actualProductionDate;

    /** 内部B包日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date internalClosureDate;

    /** 功能测试版本日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date functionalTestDate;

    /** 排期状态 (sys_pqzt) */
    @Excel(name = "排期状态")
    private String scheduleStatus;

    /** 功能点说明 */
    private String functionDescription;

    /** 实施计划 */
    private String implementationPlan;

    // ===== 非DB字段（展示用）=====

    /** 任务负责人姓名 */
    private String taskManagerName;

    /** 所属主项目名称 */
    private String parentProjectName;

    /** 所属主项目状态（来自父项目，展示用） */
    private String projectStatus;

    /** 投产批次号（来自 pm_production_batch） */
    private String batchNo;

    /** 查询参数：按父项目ID筛选 */
    private Long parentId;

    /** 查询参数：父项目确认年度 */
    private String parentRevenueConfirmYear;

    /** 查询参数：父项目部门 */
    private String projectDept;
}
```

**Step 2: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/Task.java
git commit -m "feat: 新增Task实体类"
```

---

#### Task 2.2：TaskMapper 接口

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/TaskMapper.java`

**Step 1: 创建 Mapper 接口**

```java
package com.ruoyi.project.mapper;

import com.ruoyi.project.domain.Task;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface TaskMapper {

    /** 查询任务列表（含父项目信息） */
    List<Task> selectTaskList(Task task);

    /** 按 ID 查询任务详情 */
    Task selectTaskById(Long taskId);

    /** 新增任务 */
    int insertTask(Task task);

    /** 修改任务 */
    int updateTask(Task task);

    /** 硬删除单个任务 */
    int deleteTaskById(Long taskId);

    /** 批量硬删除任务 */
    int deleteTaskByIds(Long[] taskIds);

    /**
     * 获取项目的任务轻量选项（日报下拉用）
     * 返回字段：taskId, taskName, taskCode, taskStage, taskManagerId, taskManagerName,
     *           estimatedWorkload, actualWorkload, batchNo, scheduleStatus
     */
    List<Map<String, Object>> selectTaskOptions(@Param("projectId") Long projectId);

    /**
     * 批量判断哪些项目有任务
     * 用于日报 selectMyProjects 标记 hasSubProject
     */
    List<Long> selectProjectsHasTasks(@Param("projectIds") List<Long> projectIds);

    /** 统计项目的任务数 */
    int countTasksByProjectId(@Param("projectId") Long projectId);

    /**
     * 更新任务实际工作量（日报保存时调用）
     * hours 单位：小时
     */
    int updateActualWorkload(@Param("taskId") Long taskId, @Param("hours") BigDecimal hours);
}
```

**Step 2: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/mapper/TaskMapper.java
git commit -m "feat: 新增TaskMapper接口"
```

---

#### Task 2.3：TaskMapper.xml

**Files:**
- Create: `ruoyi-project/src/main/resources/mapper/project/TaskMapper.xml`

**Step 1: 创建 XML**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.project.mapper.TaskMapper">

    <resultMap type="com.ruoyi.project.domain.Task" id="TaskResult">
        <id     property="taskId"               column="task_id"/>
        <result property="projectId"            column="project_id"/>
        <result property="taskCode"             column="task_code"/>
        <result property="taskName"             column="task_name"/>
        <result property="taskStage"            column="task_stage"/>
        <result property="taskManagerId"        column="task_manager_id"/>
        <result property="product"              column="product"/>
        <result property="bankDemandNo"         column="bank_demand_no"/>
        <result property="softwareDemandNo"     column="software_demand_no"/>
        <result property="taskBudget"           column="task_budget"/>
        <result property="estimatedWorkload"    column="estimated_workload"/>
        <result property="actualWorkload"       column="actual_workload"/>
        <result property="productionYear"       column="production_year"/>
        <result property="batchId"              column="batch_id"/>
        <result property="taskPlan"             column="task_plan"/>
        <result property="taskDescription"      column="task_description"/>
        <result property="startDate"            column="start_date"/>
        <result property="endDate"              column="end_date"/>
        <result property="productionDate"       column="production_date"/>
        <result property="productionVersionDate" column="production_version_date"/>
        <result property="actualProductionDate" column="actual_production_date"/>
        <result property="internalClosureDate"  column="internal_closure_date"/>
        <result property="functionalTestDate"   column="functional_test_date"/>
        <result property="scheduleStatus"       column="schedule_status"/>
        <result property="functionDescription"  column="function_description"/>
        <result property="implementationPlan"   column="implementation_plan"/>
        <result property="createBy"             column="create_by"/>
        <result property="createTime"           column="create_time"/>
        <result property="updateBy"             column="update_by"/>
        <result property="updateTime"           column="update_time"/>
        <result property="remark"               column="remark"/>
        <!-- 非DB展示字段 -->
        <result property="taskManagerName"      column="task_manager_name"/>
        <result property="parentProjectName"    column="parent_project_name"/>
        <result property="projectStatus"        column="project_status"/>
        <result property="batchNo"              column="batch_no"/>
    </resultMap>

    <sql id="selectTaskVo">
        select t.task_id, t.project_id, t.task_code, t.task_name, t.task_stage,
               t.task_manager_id, t.product, t.bank_demand_no, t.software_demand_no,
               t.task_budget, t.estimated_workload, t.actual_workload,
               t.production_year, t.batch_id,
               t.task_plan, t.task_description,
               t.start_date, t.end_date, t.production_date,
               t.production_version_date, t.actual_production_date,
               t.internal_closure_date, t.functional_test_date,
               t.schedule_status, t.function_description, t.implementation_plan,
               t.create_by, t.create_time, t.update_by, t.update_time, t.remark,
               u.nick_name                      as task_manager_name,
               p.project_name                   as parent_project_name,
               p.project_status                 as project_status,
               pb.batch_no                      as batch_no
        from pm_task t
        left join sys_user u  on t.task_manager_id = u.user_id
        left join pm_project p on t.project_id = p.project_id
        left join pm_production_batch pb on t.batch_id = pb.batch_id
    </sql>

    <select id="selectTaskList" parameterType="com.ruoyi.project.domain.Task" resultMap="TaskResult">
        <include refid="selectTaskVo"/>
        <where>
            <if test="projectId != null">
                and t.project_id = #{projectId}
            </if>
            <if test="parentId != null">
                and t.project_id = #{parentId}
            </if>
            <if test="taskName != null and taskName != ''">
                and t.task_name like concat('%', #{taskName}, '%')
            </if>
            <if test="taskCode != null and taskCode != ''">
                and t.task_code like concat('%', #{taskCode}, '%')
            </if>
            <if test="taskStage != null and taskStage != ''">
                and t.task_stage = #{taskStage}
            </if>
            <if test="taskManagerId != null">
                and t.task_manager_id = #{taskManagerId}
            </if>
            <if test="productionYear != null and productionYear != ''">
                and t.production_year = #{productionYear}
            </if>
            <if test="batchId != null">
                and t.batch_id = #{batchId}
            </if>
            <if test="scheduleStatus != null and scheduleStatus != ''">
                and t.schedule_status = #{scheduleStatus}
            </if>
            <if test="softwareDemandNo != null and softwareDemandNo != ''">
                and t.software_demand_no like concat('%', #{softwareDemandNo}, '%')
            </if>
            <if test="product != null and product != ''">
                and t.product = #{product}
            </if>
            <if test="projectDept != null and projectDept != ''">
                and (p.project_dept = #{projectDept}
                     or p.project_dept in (select dept_id from sys_dept where find_in_set(#{projectDept}, ancestors) > 0))
            </if>
            <if test="parentRevenueConfirmYear != null and parentRevenueConfirmYear != ''">
                and p.revenue_confirm_year = #{parentRevenueConfirmYear}
            </if>
        </where>
        order by t.task_code, t.create_time desc
    </select>

    <select id="selectTaskById" parameterType="Long" resultMap="TaskResult">
        <include refid="selectTaskVo"/>
        where t.task_id = #{taskId}
    </select>

    <insert id="insertTask" parameterType="com.ruoyi.project.domain.Task" useGeneratedKeys="true" keyProperty="taskId">
        insert into pm_task (
            project_id, task_code, task_name, task_stage, task_manager_id,
            product, bank_demand_no, software_demand_no, task_budget,
            estimated_workload, actual_workload,
            production_year, batch_id,
            task_plan, task_description,
            start_date, end_date, production_date, production_version_date,
            actual_production_date, internal_closure_date, functional_test_date,
            schedule_status, function_description, implementation_plan,
            create_by, create_time, update_by, update_time, remark
        ) values (
            #{projectId}, #{taskCode}, #{taskName}, #{taskStage}, #{taskManagerId},
            #{product}, #{bankDemandNo}, #{softwareDemandNo}, #{taskBudget},
            #{estimatedWorkload}, 0,
            #{productionYear}, #{batchId},
            #{taskPlan}, #{taskDescription},
            #{startDate}, #{endDate}, #{productionDate}, #{productionVersionDate},
            #{actualProductionDate}, #{internalClosureDate}, #{functionalTestDate},
            #{scheduleStatus}, #{functionDescription}, #{implementationPlan},
            #{createBy}, #{createTime}, #{updateBy}, #{updateTime}, #{remark}
        )
    </insert>

    <update id="updateTask" parameterType="com.ruoyi.project.domain.Task">
        update pm_task
        <set>
            <if test="taskCode != null">task_code = #{taskCode},</if>
            <if test="taskName != null and taskName != ''">task_name = #{taskName},</if>
            <if test="taskStage != null">task_stage = #{taskStage},</if>
            <if test="taskManagerId != null">task_manager_id = #{taskManagerId},</if>
            <if test="product != null">product = #{product},</if>
            <if test="bankDemandNo != null">bank_demand_no = #{bankDemandNo},</if>
            <if test="softwareDemandNo != null">software_demand_no = #{softwareDemandNo},</if>
            <if test="taskBudget != null">task_budget = #{taskBudget},</if>
            <if test="estimatedWorkload != null">estimated_workload = #{estimatedWorkload},</if>
            <if test="productionYear != null">production_year = #{productionYear},</if>
            <if test="batchId != null">batch_id = #{batchId},</if>
            <if test="taskPlan != null">task_plan = #{taskPlan},</if>
            <if test="taskDescription != null">task_description = #{taskDescription},</if>
            <if test="startDate != null">start_date = #{startDate},</if>
            <if test="endDate != null">end_date = #{endDate},</if>
            <if test="productionDate != null">production_date = #{productionDate},</if>
            <if test="productionVersionDate != null">production_version_date = #{productionVersionDate},</if>
            <if test="actualProductionDate != null">actual_production_date = #{actualProductionDate},</if>
            <if test="internalClosureDate != null">internal_closure_date = #{internalClosureDate},</if>
            <if test="functionalTestDate != null">functional_test_date = #{functionalTestDate},</if>
            <if test="scheduleStatus != null">schedule_status = #{scheduleStatus},</if>
            <if test="functionDescription != null">function_description = #{functionDescription},</if>
            <if test="implementationPlan != null">implementation_plan = #{implementationPlan},</if>
            <if test="updateBy != null">update_by = #{updateBy},</if>
            <if test="remark != null">remark = #{remark},</if>
            update_time = NOW()
        </set>
        where task_id = #{taskId}
    </update>

    <delete id="deleteTaskById" parameterType="Long">
        DELETE FROM pm_task WHERE task_id = #{taskId}
    </delete>

    <delete id="deleteTaskByIds">
        DELETE FROM pm_task WHERE task_id in
        <foreach item="taskId" collection="array" open="(" separator="," close=")">
            #{taskId}
        </foreach>
    </delete>

    <select id="selectTaskOptions" resultType="map">
        select t.task_id          as "taskId",
               t.task_name        as "taskName",
               t.task_code        as "taskCode",
               t.task_stage       as "taskStage",
               t.task_manager_id  as "taskManagerId",
               u.nick_name        as "taskManagerName",
               t.estimated_workload as "estimatedWorkload",
               ROUND(t.actual_workload / 8, 3) as "actualWorkload",
               pb.batch_no        as "batchNo",
               t.schedule_status  as "scheduleStatus"
        from pm_task t
        left join sys_user u on t.task_manager_id = u.user_id
        left join pm_production_batch pb on t.batch_id = pb.batch_id
        where t.project_id = #{projectId}
          and (t.task_stage is null or t.task_stage != '11')
        order by t.task_code, t.create_time
    </select>

    <select id="selectProjectsHasTasks" resultType="long">
        select distinct project_id
        from pm_task
        where project_id in
        <foreach item="id" collection="projectIds" open="(" separator="," close=")">
            #{id}
        </foreach>
    </select>

    <select id="countTasksByProjectId" resultType="int">
        select count(*) from pm_task where project_id = #{projectId}
    </select>

    <update id="updateActualWorkload">
        update pm_task set actual_workload = #{hours}, update_time = NOW()
        where task_id = #{taskId}
    </update>

</mapper>
```

**Step 2: Commit**

```bash
git add ruoyi-project/src/main/resources/mapper/project/TaskMapper.xml
git commit -m "feat: 新增TaskMapper.xml"
```

---

#### Task 2.4：ITaskService + TaskServiceImpl

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/service/ITaskService.java`
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/TaskServiceImpl.java`

**Step 1: 创建 ITaskService 接口**

```java
package com.ruoyi.project.service;

import com.ruoyi.project.domain.Task;
import java.util.List;
import java.util.Map;

public interface ITaskService {
    List<Task> selectTaskList(Task task);
    Task selectTaskById(Long taskId);
    int insertTask(Task task);
    int updateTask(Task task);
    int deleteTaskByIds(Long[] taskIds);
    int countTasksByProjectId(Long projectId);
    List<Map<String, Object>> selectTaskOptions(Long projectId);
    List<Long> selectProjectsHasTasks(List<Long> projectIds);
}
```

**Step 2: 创建 TaskServiceImpl**

```java
package com.ruoyi.project.service.impl;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.Task;
import com.ruoyi.project.mapper.TaskMapper;
import com.ruoyi.project.service.ITaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TaskServiceImpl implements ITaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public List<Task> selectTaskList(Task task) {
        return taskMapper.selectTaskList(task);
    }

    @Override
    public Task selectTaskById(Long taskId) {
        return taskMapper.selectTaskById(taskId);
    }

    @Override
    public int insertTask(Task task) {
        task.setCreateBy(SecurityUtils.getUsername());
        task.setCreateTime(DateUtils.getNowDate());
        return taskMapper.insertTask(task);
    }

    @Override
    public int updateTask(Task task) {
        task.setUpdateBy(SecurityUtils.getUsername());
        task.setUpdateTime(DateUtils.getNowDate());
        return taskMapper.updateTask(task);
    }

    @Override
    public int deleteTaskByIds(Long[] taskIds) {
        return taskMapper.deleteTaskByIds(taskIds);
    }

    @Override
    public int countTasksByProjectId(Long projectId) {
        return taskMapper.countTasksByProjectId(projectId);
    }

    @Override
    public List<Map<String, Object>> selectTaskOptions(Long projectId) {
        return taskMapper.selectTaskOptions(projectId);
    }

    @Override
    public List<Long> selectProjectsHasTasks(List<Long> projectIds) {
        return taskMapper.selectProjectsHasTasks(projectIds);
    }
}
```

**Step 3: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/ITaskService.java
git add ruoyi-project/src/main/java/com/ruoyi/project/service/impl/TaskServiceImpl.java
git commit -m "feat: 新增TaskService"
```

---

#### Task 2.5：TaskController

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/controller/TaskController.java`

**Step 1: 创建 Controller**

```java
package com.ruoyi.project.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.Task;
import com.ruoyi.project.service.ITaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理 /project/task/**
 */
@RestController
@RequestMapping("/project/task")
public class TaskController extends BaseController {

    @Autowired
    private ITaskService taskService;

    /** 任务列表（分页） */
    @PreAuthorize("@ss.hasPermi('project:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(Task task) {
        startPage();
        return getDataTable(taskService.selectTaskList(task));
    }

    /** 任务详情 */
    @PreAuthorize("@ss.hasAnyPermi('project:task:list,project:task:query')")
    @GetMapping("/{taskId}")
    public AjaxResult getInfo(@PathVariable Long taskId) {
        return AjaxResult.success(taskService.selectTaskById(taskId));
    }

    /** 日报用：获取项目的任务轻量选项 */
    @GetMapping("/options")
    public AjaxResult getTaskOptions(@RequestParam Long projectId) {
        return AjaxResult.success(taskService.selectTaskOptions(projectId));
    }

    /** 日报用：批量判断哪些项目有任务 */
    @GetMapping("/projectsHasTasks")
    public AjaxResult getProjectsHasTasks(@RequestParam List<Long> projectIds) {
        return AjaxResult.success(taskService.selectProjectsHasTasks(projectIds));
    }

    /** 新增任务 */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('project:task:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody Task task) {
        return toAjax(taskService.insertTask(task));
    }

    /** 修改任务 */
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('project:task:edit')")
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Task task) {
        return toAjax(taskService.updateTask(task));
    }

    /** 删除任务（硬删除） */
    @Log(title = "任务管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('project:task:remove')")
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable Long[] taskIds) {
        return toAjax(taskService.deleteTaskByIds(taskIds));
    }
}
```

**Step 2: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/TaskController.java
git commit -m "feat: 新增TaskController，接口前缀/project/task"
```

---

### Phase 3：后端 - 更新日报模块

#### Task 3.1：更新 DailyReportServiceImpl

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java`

**Step 1: 注入 TaskMapper，替换 sub-project 相关调用**

找到类顶部 `@Autowired` 注入区域，添加：
```java
@Autowired
private TaskMapper taskMapper;
```

找到 `selectMyProjects()` 方法中的这段代码：
```java
// 旧代码
List<Long> hasSubIds = projectMapper.selectProjectsHasSubProject(ids);
```
替换为：
```java
// 新代码
List<Long> hasSubIds = taskMapper.selectProjectsHasTasks(ids);
```

找到 `saveDailyReport()` 方法中更新子项目工作量的部分：
```java
// 旧代码
for (Long subProjectId : affectedSubProjectIds) {
    BigDecimal subHours = detailMapper.sumWorkHoursBySubProjectId(subProjectId);
    projectMapper.updateActualWorkload(subProjectId, subHours);
}
```
替换为：
```java
// 新代码
for (Long taskId : affectedSubProjectIds) {
    BigDecimal taskHours = detailMapper.sumWorkHoursBySubProjectId(taskId);
    taskMapper.updateActualWorkload(taskId, taskHours);
}
```

**Step 2: 验证编译**

```bash
mvn clean compile -pl ruoyi-project -am -q
```

**Step 3: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java
git commit -m "feat: 日报服务改用TaskMapper查询任务数据"
```

---

#### Task 3.2：更新 DailyReportDetailMapper.xml

**Files:**
- Modify: `ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml`

**Step 1: 修改 selectByReportId 查询中的 JOIN**

找到：
```xml
left join pm_project sp on dd.sub_project_id = sp.project_id and sp.del_flag = '0'
```
替换为：
```xml
left join pm_task sp on dd.sub_project_id = sp.task_id
```

找到 select 字句中的子项目名称（通常是 `sp.project_name as sub_project_name`），确认 pm_task 中对应字段是 `task_name`：
```xml
-- 旧
sp.project_name as sub_project_name
-- 新
sp.task_name as sub_project_name
```

**Step 2: 验证编译**

```bash
mvn clean compile -pl ruoyi-project -am -q
```

**Step 3: Commit**

```bash
git add ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml
git commit -m "feat: 日报明细Mapper改JOIN pm_task"
```

---

### Phase 4：后端 - 清理 Project 模块子项目代码

#### Task 4.1：清理 ProjectMapper.java

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java`

**Step 1: 删除以下方法声明**

```java
// 删除这些方法
int countSubProjects(@Param("parentId") Long parentId);
List<Map<String, Object>> selectSubProjectOptions(@Param("parentId") Long parentId);
List<Map<String, Object>> selectSiblingTasks(@Param("parentId") Long parentId);
int selectSubProjectMaxSeq(@Param("parentId") Long parentId);
List<Long> selectProjectsHasSubProject(List<Long> projectIds);
BigDecimal sumSubTasksWorkload(@Param("parentId") Long parentId);
// 同时删除 selectSubProjectList 如有
```

**Step 2: 验证编译（此时 ProjectServiceImpl 可能报错，一并修改）**

**Step 3: Commit（与 4.2 合并提交）**

---

#### Task 4.2：清理 ProjectServiceImpl.java

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java`

**Step 1: 删除以下方法**

```java
// 删除这些方法
public List<Map<String, Object>> selectSubProjectOptions(Long parentId) { ... }
public List<Map<String, Object>> selectSiblingTasks(Long parentId) { ... }
```

删除 `insertProject` / `updateProject` 中向 `pm_project_member` 同步子项目时与 `project_level` 相关的逻辑（子项目成员继承父项目，但新建/修改任务不再经过 ProjectService）。

**Step 2: 验证编译**

```bash
mvn clean compile -pl ruoyi-project -am -q
```

**Step 3: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java
git add ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java
git commit -m "refactor: 移除Project模块子项目相关方法"
```

---

#### Task 4.3：清理 ProjectController.java

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java`

**Step 1: 删除以下接口方法**

```java
// 删除
@GetMapping("/subList")         // 子项目列表
@GetMapping("/subProjectOptions") // 子项目下拉选项
@GetMapping("/siblingTasks")    // 兄弟任务列表
```

**Step 2: 验证编译**

```bash
mvn clean compile -pl ruoyi-project -am -q
```

**Step 3: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java
git commit -m "refactor: 移除ProjectController子项目接口"
```

---

#### Task 4.4：清理 ProjectMapper.xml

**Files:**
- Modify: `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml`

**Step 1: 删除以下 SQL 片段**

- `<select id="selectSubProjectList" ...>` （行 699-761）
- `<select id="selectSubProjectOptions" ...>` （行 763-782）
- `<select id="selectSiblingTasks" ...>` （行 784-802）
- `<select id="selectProjectsHasSubProject" ...>` （行 815-824）
- `<select id="countSubProjects" ...>`
- `<select id="selectSubProjectMaxSeq" ...>`
- `<select id="sumSubTasksWorkload" ...>`

**Step 2: 清理 selectProjectList 中的父项目 JOIN（可选）**

当前 selectProjectList 中有：
```xml
LEFT JOIN pm_project pp ON p.parent_id = pp.project_id
```
由于主项目 parent_id 为 NULL，这个 JOIN 实际上永远不会命中，可以删除。同时删除 select 中的 `p.parent_id, p.project_level, p.task_code, pp.project_name as parent_project_name`。

**Step 3: 验证编译**

```bash
mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true -q
```

**Step 4: Commit**

```bash
git add ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml
git commit -m "refactor: 移除ProjectMapper.xml子项目SQL"
```

---

### Phase 5：前端 - 新建 Task API

#### Task 5.1：创建 task.ts API 文件

**Files:**
- Create: `ruoyi-ui/src/api/project/task.ts`

**Step 1: 创建 API 文件**

```typescript
import request from '@/utils/request'

// 任务列表
export function listTask(query: any) {
  return request({
    url: '/project/task/list',
    method: 'get',
    params: query
  })
}

// 任务详情
export function getTask(taskId: number | string) {
  return request({
    url: `/project/task/${taskId}`,
    method: 'get'
  })
}

// 新增任务
export function addTask(data: any) {
  return request({
    url: '/project/task',
    method: 'post',
    data
  })
}

// 修改任务
export function updateTask(data: any) {
  return request({
    url: '/project/task',
    method: 'put',
    data
  })
}

// 删除任务
export function deleteTask(taskIds: string) {
  return request({
    url: `/project/task/${taskIds}`,
    method: 'delete'
  })
}

// 获取项目的任务选项（日报下拉用）
export function getTaskOptions(projectId: number | string) {
  return request({
    url: '/project/task/options',
    method: 'get',
    params: { projectId }
  })
}

// 批量判断哪些项目有任务（日报用）
export function getProjectsHasTasks(projectIds: number[]) {
  return request({
    url: '/project/task/projectsHasTasks',
    method: 'get',
    params: { projectIds: projectIds.join(',') }
  })
}
```

**Step 2: Commit**

```bash
git add ruoyi-ui/src/api/project/task.ts
git commit -m "feat: 新增任务管理API task.ts"
```

---

### Phase 6：前端 - 更新子项目页面（改用 Task API）

> 所有页面在 `ruoyi-ui/src/views/project/subproject/` 下，**字段名需按映射更新**。

#### 字段名映射速查表（旧→新）

| 旧字段名（来自 pm_project） | 新字段名（来自 pm_task） |
|---|---|
| `projectId`（任务自身ID，用于 CRUD 的 id） | `taskId` |
| `parentId` | `projectId`（所属主项目ID） |
| `projectName`（任务名） | `taskName` |
| `projectStage` | `taskStage` |
| `projectManagerId` | `taskManagerId` |
| `projectManagerName` | `taskManagerName` |
| `projectBudget` | `taskBudget` |
| `projectPlan` | `taskPlan` |
| `projectDescription` | `taskDescription` |
| `functionDescription` | `functionDescription`（不变） |
| `internalClosureDate` | `internalClosureDate`（不变） |
| `subProjectId`（日报中） | `taskId`（任务自己的ID） |

#### Task 6.1：更新 index.vue

**Files:**
- Modify: `ruoyi-ui/src/views/project/subproject/index.vue`

**Step 1: 替换 API 导入**

```typescript
// 删除
import { listSubProject, deleteSubProject } from '@/api/project/project'
// 新增
import { listTask, deleteTask } from '@/api/project/task'
```

**Step 2: 更新列表查询调用**

```typescript
// 旧
const res = await listSubProject(queryParams.value)
// 新
const res = await listTask(queryParams.value)
```

**Step 3: 更新删除调用**

```typescript
// 旧
await deleteSubProject(ids)
// 新
await deleteTask(ids)
```

**Step 4: 更新表格列绑定字段名**（按映射表替换 prop 属性）

**Step 5: 验证页面正常加载**

```bash
cd ruoyi-ui && npm run dev
```

**Step 6: Commit**

```bash
git add ruoyi-ui/src/views/project/subproject/index.vue
git commit -m "feat: 任务列表页改用Task API"
```

---

#### Task 6.2：更新 add.vue

**Files:**
- Modify: `ruoyi-ui/src/views/project/subproject/add.vue`

**Step 1: 替换 API 导入**

```typescript
// 删除 project.js 中 addSubProject 相关
import { addTask } from '@/api/project/task'
```

**Step 2: 更新 form 字段名**（按映射表替换所有 v-model 绑定）

```typescript
// 旧 form 初始值
const form = ref({
  parentId: null,
  projectName: '',
  projectManagerId: null,
  // ...
})
// 新 form 初始值
const form = ref({
  projectId: null,    // 所属主项目ID（来自路由参数）
  taskName: '',
  taskManagerId: null,
  // ...
})
```

**Step 3: 更新提交调用**

```typescript
// 旧
await addSubProject(form.value)
// 新
await addTask(form.value)
```

**Step 4: Commit**

```bash
git add ruoyi-ui/src/views/project/subproject/add.vue
git commit -m "feat: 任务新增页改用Task API"
```

---

#### Task 6.3：更新 edit.vue

**Files:**
- Modify: `ruoyi-ui/src/views/project/subproject/edit.vue`

**Step 1: 替换 API 导入**

```typescript
import { getTask, updateTask } from '@/api/project/task'
```

**Step 2: 更新 getDetail 调用**

```typescript
// 旧
const res = await getSubProject(taskId)
// 新
const res = await getTask(taskId)
```

**Step 3: 更新表单字段名**（同 add.vue，按映射表全部替换）

**Step 4: 更新提交调用**

```typescript
await updateTask(form.value)
```

**Step 5: Commit**

```bash
git add ruoyi-ui/src/views/project/subproject/edit.vue
git commit -m "feat: 任务编辑页改用Task API"
```

---

#### Task 6.4：更新 detail.vue

**Files:**
- Modify: `ruoyi-ui/src/views/project/subproject/detail.vue`

**Step 1: 替换 API 导入**

```typescript
import { getTask } from '@/api/project/task'
```

**Step 2: 更新详情字段名**（按映射表替换所有展示字段）

**Step 3: Commit**

```bash
git add ruoyi-ui/src/views/project/subproject/detail.vue
git commit -m "feat: 任务详情页改用Task API"
```

---

#### Task 6.5：更新 decompose.vue

**Files:**
- Modify: `ruoyi-ui/src/views/project/subproject/decompose.vue`

**Step 1: 替换 API 导入**

```typescript
import { listTask, addTask } from '@/api/project/task'
```

**Step 2: 更新字段名和调用**（同 add.vue 逻辑）

**Step 3: Commit**

```bash
git add ruoyi-ui/src/views/project/subproject/decompose.vue
git commit -m "feat: 项目分解页改用Task API"
```

---

### Phase 7：前端 - 更新日报 write.vue

#### Task 7.1：更新日报编辑页的任务加载逻辑

**Files:**
- Modify: `ruoyi-ui/src/views/project/dailyReport/write.vue`

**Step 1: 替换 API 导入**

```typescript
// 删除旧 import（来自 project.js）
// import { getSubProjectOptions, getProjectsHasSubProject } from '@/api/project/project'
// 新增
import { getTaskOptions, getProjectsHasTasks } from '@/api/project/task'
```

**Step 2: 更新 selectMyProjects 后的 hasSubProject 判断**

找到调用 `getProjectsHasSubProject` 的地方（或直接在 DailyReportService 后已改后端），
确认前端 `loadProjects` 方法返回的数据中 `hasSubProject` 字段由后端填充（已在 Phase 3 更新），无需前端额外改动。

**Step 3: 更新 loadTaskRows 中的 API 调用**

```typescript
// 旧
const res = await getSubProjectOptions(item.projectId)
// 新
const res = await getTaskOptions(item.projectId)
```

**Step 4: 更新 taskRows 映射中的 ID 字段**

```typescript
// 旧
return {
  subProjectId: t.projectId,  // 旧字段名
  // ...
}
// 新
return {
  subProjectId: t.taskId,     // 新字段名
  // ...
}
```

**Step 5: 验证日报填写页功能完整**

- 打开日报填写页
- 确认有任务的项目展示任务列表
- 确认任务工时可以正常录入保存
- 确认保存后 pm_task.actual_workload 被正确更新

**Step 6: Commit**

```bash
git add ruoyi-ui/src/views/project/dailyReport/write.vue
git commit -m "feat: 日报填写页改用Task API"
```

---

### Phase 8：添加菜单权限数据

#### Task 8.1：添加任务管理菜单 SQL

**Files:**
- Create: `pm-sql/fix_task_menu_2026.sql`

**Step 1: 编写菜单 SQL**

```sql
-- 任务管理菜单（挂在项目管理一级菜单下）
-- 使用子查询动态获取父菜单 ID，避免硬编码
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
-- 一级菜单（任务管理）
('任务管理',
 (SELECT menu_id FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0 LIMIT 1),
 3, 'task', 'project/subproject/index', 1, 0, 'C', '0', '0',
 'project:task:list', 'list', 'admin', NOW(), '任务管理菜单');

-- 获取刚插入的菜单ID，添加操作权限
SET @task_menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, menu_type, visible, status, perms, create_by, create_time)
VALUES
('任务查询', @task_menu_id, 1, '', 'F', '0', '0', 'project:task:list',  'admin', NOW()),
('任务新增', @task_menu_id, 2, '', 'F', '0', '0', 'project:task:add',   'admin', NOW()),
('任务编辑', @task_menu_id, 3, '', 'F', '0', '0', 'project:task:edit',  'admin', NOW()),
('任务删除', @task_menu_id, 4, '', 'F', '0', '0', 'project:task:remove','admin', NOW());
```

**Step 2: 在本地执行**

```bash
cat pm-sql/fix_task_menu_2026.sql | docker exec -i 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

**Step 3: 在生产执行**

```bash
cat pm-sql/fix_task_menu_2026.sql | ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
```

**Step 4: 给相关角色分配权限（在系统管理→角色管理中操作）**

**Step 5: Commit**

```bash
git add pm-sql/fix_task_menu_2026.sql
git commit -m "sql: 添加任务管理菜单和权限数据"
```

---

### Phase 9：全量验证

#### Task 9.1：功能验证清单

**验证日报功能（最重要）：**

- [ ] 日报填写页：有任务的项目正常展示任务列表
- [ ] 日报填写页：工时录入保存正常
- [ ] 日报保存后：`pm_task.actual_workload` 正确更新
- [ ] 日报历史记录：已有日报的任务名称、工时正常展示
- [ ] 日报统计页：任务关联数据正常

**验证任务管理页面：**

- [ ] 任务列表：正常显示，父项目信息正确
- [ ] 任务新增：保存成功，`pm_task` 新增记录
- [ ] 任务编辑：保存成功，数据正确更新
- [ ] 任务删除：硬删除，数据库记录消失
- [ ] 项目分解页：可以新增任务到指定项目

**验证项目相关功能（确保未受影响）：**

- [ ] 项目列表：无任务数据混入（project_level=0 过滤正常）
- [ ] 项目详情：正常显示（无 sub-project 相关字段报错）
- [ ] 合同管理：正常
- [ ] 公司收入确认：正常
- [ ] 团队收入确认：正常
- [ ] 立项审核：正常
- [ ] 付款里程碑：正常

---

### Phase 10：清理 pm_project 旧任务数据

> ⚠️ **此步骤在全量验证通过后才执行**

#### Task 10.1：删除 pm_project 中的任务记录

**Files:**
- Create: `pm-sql/fix_task_cleanup_2026.sql`

**Step 1: 编写清理 SQL**

```sql
-- 最终确认数量
SELECT '清理前 pm_project 任务记录' AS label, COUNT(*) FROM pm_project WHERE project_level = 1
UNION ALL
SELECT 'pm_task 记录数', COUNT(*) FROM pm_task;

-- 删除 pm_project 中的任务记录（硬删除）
DELETE FROM pm_project WHERE project_level = 1;

-- 验证
SELECT '清理后 pm_project 任务记录' AS label, COUNT(*) FROM pm_project WHERE project_level = 1;
```

**Step 2: 执行（本地先，生产后）**

```bash
# 本地
cat pm-sql/fix_task_cleanup_2026.sql | docker exec -i 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue

# 生产
cat pm-sql/fix_task_cleanup_2026.sql | ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
```

**Step 3: Commit**

```bash
git add pm-sql/fix_task_cleanup_2026.sql
git commit -m "sql: 清理pm_project中的历史任务记录"
```

---

## 部署顺序

```
Phase 1  → 数据库建表迁移（不影响现有功能）
    ↓
Phase 2-4 → 后端代码（new Task模块 + DailyReport更新 + Project清理）
    ↓
Phase 5-7 → 前端代码（task API + 页面更新 + 日报更新）
    ↓
Phase 8  → 菜单权限数据
    ↓
Phase 9  → 全量验证
    ↓
Phase 10 → 清理 pm_project 旧任务记录（仅在验证完全通过后）
```

## 回滚方案

如果 Phase 1 之后、Phase 10 之前发现问题：
- `pm_project` 中 project_level=1 数据仍然存在，可随时回滚代码到旧版本
- `pm_daily_report_detail.sub_project_id` 值未变，日报数据安全

如果 Phase 10 已执行：
- 从 `pm_task` 反向恢复数据回 `pm_project`（INSERT INTO pm_project SELECT ... FROM pm_task）
