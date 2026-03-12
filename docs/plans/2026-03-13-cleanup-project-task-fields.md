# 清理 pm_project 任务专用字段 - 实施方案

**目标：** 将 `pm_project` 表恢复为纯项目信息表，删除当初为支持子任务管理而添加的字段，并同步清理后端 Java 代码和前端代码。

**前提条件（已完成）：**
- ✅ `pm_task` 表已创建并完成数据迁移（本地+生产均有 33 条）
- ✅ `pm_project` 中 `project_level=1` 的任务行已从本地删除（生产待删）
- ✅ 前端 subproject/ 页面已全部切换到 Task API
- ✅ 日报 Mapper 已切换到 JOIN pm_task

---

## 要删除的字段（共 19 个）

| pm_project 列名 | Java 字段名 | 说明 |
|----------------|------------|------|
| `parent_id` | `parentId` | 子任务关联父项目 |
| `project_level` | `projectLevel` | 区分主项目/子任务（0/1） |
| `task_code` | `taskCode` | 任务编号 |
| `batch_id` | `batchId` | 投产批次 |
| `production_year` | `productionYear` | 投产年度 |
| `internal_closure_date` | `internalClosureDate` | 内部B包日期 |
| `functional_test_date` | `functionalTestDate` | 功能测试版本日期 |
| `production_version_date` | `productionVersionDate` | 生产版本日期 |
| `actual_production_date` | `actualProductionDate` | 实际投产日期 |
| `schedule_status` | `scheduleStatus` | 排期状态 |
| `function_description` | `functionDescription` | 功能点说明 |
| `implementation_plan` | `implementationPlan` | 实施计划 |
| `task_create_by` | `taskCreateBy` | 任务创建人 |
| `task_create_time` | `taskCreateTime` | 任务创建时间 |
| `task_update_by` | `taskUpdateBy` | 任务更新人 |
| `task_update_time` | `taskUpdateTime` | 任务更新时间 |
| `bank_demand_no` | `bankDemandNo` | 总行需求号 |
| `software_demand_no` | `softwareDemandNo` | 软件中心需求编号 |
| `product` | `product` | 产品（项目表原本没有） |

---

## 影响文件清单

### 后端

| 文件 | 操作 |
|------|------|
| `ruoyi-project/src/main/java/com/ruoyi/project/domain/Project.java` | 删除 19 个字段声明及其 getter/setter，更新 toString() |
| `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml` | 删除 resultMap 映射、SELECT/INSERT/UPDATE 中的字段引用，删除 project_level 过滤条件 |
| `ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml` | 删除 parent_id 子查询（第 160 行） |
| `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java` | 删除 projectLevel 判断逻辑 |
| `pm-sql/init/00_tables_ddl.sql` | 从 pm_project 建表 DDL 中删除这 19 列 |
| `pm-sql/fix_cleanup_project_fields_20260313.sql` | 新建：生产执行的 ALTER TABLE DROP COLUMN |

### 前端（无需修改）
- `ruoyi-ui/src/views/project/subproject/` — 已切换到 Task API，字段引用均为 pm_task 字段 ✅
- `ruoyi-ui/src/views/project/project/detail.vue`、`contract/detail.vue` — 引用的 taskCode/scheduleStatus 等均来自 pm_task 接口数据，不来自 pm_project ✅

---

## 详细实施步骤

### Task 1：修改 DailyReportMapper.xml

**文件：** `ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml`

删除 `parent_id` 子查询（约第 155-161 行），将带 OR 的复合条件简化为：

```xml
<!-- 改前 -->
and exists (
    select 1 from pm_daily_report_detail dd_pid
    where dd_pid.report_id = r.report_id
      and dd_pid.del_flag = '0'
      and (dd_pid.project_id = #{projectId}
           or dd_pid.project_id in (select project_id from pm_project where parent_id = #{projectId} and del_flag = '0'))
)

<!-- 改后 -->
and exists (
    select 1 from pm_daily_report_detail dd_pid
    where dd_pid.report_id = r.report_id
      and dd_pid.del_flag = '0'
      and dd_pid.project_id = #{projectId}
)
```

> **原因：** `pm_daily_report_detail.project_id` 始终是主项目 ID，不再有任务行独立存 project_id 的情况，OR 子查询已无意义且引用了即将删除的 `parent_id` 列。

---

### Task 2：清理 ProjectMapper.xml

**2.1 删除 resultMap 中的映射（约第 91-117 行）：**

```xml
<!-- 删除以下所有行 -->
<result property="parentId"              column="parent_id" />
<result property="projectLevel"          column="project_level" />
<result property="taskCode"              column="task_code" />
<result property="batchId"               column="batch_id" />
<result property="productionYear"        column="production_year" />
<result property="bankDemandNo"          column="bank_demand_no" />
<result property="softwareDemandNo"      column="software_demand_no" />
<result property="product"               column="product" />
<result property="internalClosureDate"   column="internal_closure_date" />
<result property="functionalTestDate"    column="functional_test_date" />
<result property="productionVersionDate" column="production_version_date" />
<result property="scheduleStatus"        column="schedule_status" />
<result property="functionDescription"   column="function_description" />
<result property="implementationPlan"    column="implementation_plan" />
<result property="actualProductionDate"  column="actual_production_date" />
<result property="taskCreateBy"          column="task_create_by" />
<result property="taskCreateTime"        column="task_create_time" />
<result property="taskUpdateBy"          column="task_update_by" />
<result property="taskUpdateTime"        column="task_update_time" />
<result property="taskCreateByName"      column="task_create_by_name" />
<result property="taskUpdateByName"      column="task_update_by_name" />
<result property="batchNo"               column="batch_no" />
<result property="planProductionDate"    column="plan_production_date" />
```

**2.2 删除 selectProjectList 中任务字段的 SELECT 引用（约第 152 行）：**

删除：
```sql
p.parent_id, p.project_level, p.task_code,
```

删除 JOIN：
```sql
LEFT JOIN pm_project pp ON p.parent_id = pp.project_id
```

**2.3 删除所有 `project_level` 过滤条件（共 6 处）：**

```xml
<!-- 删除形如以下的所有条件 -->
AND (p.project_level IS NULL OR p.project_level = 0)
AND (project_level IS NULL OR project_level = 0)
```

**2.4 删除 selectProjectByProjectId 中的任务字段引用（约第 394-413 行）：**

删除 SELECT 中：
```sql
p.parent_id, p.project_level, p.task_code,
p.batch_id, p.production_year,
p.bank_demand_no, p.software_demand_no, p.product,
p.internal_closure_date, p.functional_test_date, p.production_version_date,
p.schedule_status, p.function_description, p.implementation_plan, p.actual_production_date,
p.task_create_by, p.task_create_time, p.task_update_by, p.task_update_time,
u_tc.nick_name AS task_create_by_name,
u_tu.nick_name AS task_update_by_name,
```

删除 JOIN：
```sql
LEFT JOIN pm_project pp2 ON p.parent_id = pp2.project_id
LEFT JOIN sys_user u_tc ON p.task_create_by COLLATE utf8mb4_unicode_ci = u_tc.user_name
LEFT JOIN sys_user u_tu ON p.task_update_by COLLATE utf8mb4_unicode_ci = u_tu.user_name
LEFT JOIN pm_production_batch pb ON p.batch_id = pb.batch_id
```

**2.5 删除 insertProject 中的任务字段（约第 485-574 行）：**

删除所有涉及以上 19 个字段的 `<if>` 块（column 列表和 values 列表各一份）。

**2.6 删除 updateProject 中的任务字段（约第 640-670 行）：**

删除所有涉及以上 19 个字段的 `<if>` 块。

---

### Task 3：清理 Project.java

删除以下字段声明及其 getter/setter：

```java
private Long parentId;
private Integer projectLevel;
private String taskCode;
private Long batchId;
private String productionYear;
private Date internalClosureDate;
private Date functionalTestDate;
private Date productionVersionDate;
private Date actualProductionDate;
private String scheduleStatus;
private String functionDescription;
private String implementationPlan;
private String bankDemandNo;
private String softwareDemandNo;
private String product;
private String taskCreateBy;
private Date taskCreateTime;
private String taskUpdateBy;
private Date taskUpdateTime;
private String taskCreateByName;
private String taskUpdateByName;
// 非DB关联显示字段（来自 pm_production_batch，随 batch_id 一并删除）：
private String batchNo;
private Date planProductionDate;
```

同步更新 `toString()` 方法，移除上述字段的 `.append()` 调用。

---

### Task 4：清理 ProjectServiceImpl.java

定位约第 173 行，删除 `projectLevel` 相关判断逻辑：

```java
// 删除此类代码：
if (existing != null && existing.getProjectLevel() != null && existing.getProjectLevel() == 1) {
    ...
}
```

---

### Task 5：生成 ALTER TABLE SQL

新建 `pm-sql/fix_cleanup_project_fields_20260313.sql`：

```sql
-- 清理 pm_project 表中的任务专用字段
-- 执行前提：
--   1. 新代码已部署（代码不再引用这些列）
--   2. pm_project 中 project_level=1 的行已删除
ALTER TABLE `pm_project`
  DROP COLUMN `parent_id`,
  DROP COLUMN `project_level`,
  DROP COLUMN `task_code`,
  DROP COLUMN `batch_id`,
  DROP COLUMN `production_year`,
  DROP COLUMN `internal_closure_date`,
  DROP COLUMN `functional_test_date`,
  DROP COLUMN `production_version_date`,
  DROP COLUMN `actual_production_date`,
  DROP COLUMN `schedule_status`,
  DROP COLUMN `function_description`,
  DROP COLUMN `implementation_plan`,
  DROP COLUMN `task_create_by`,
  DROP COLUMN `task_create_time`,
  DROP COLUMN `task_update_by`,
  DROP COLUMN `task_update_time`,
  DROP COLUMN `bank_demand_no`,
  DROP COLUMN `software_demand_no`,
  DROP COLUMN `product`;
```

---

### Task 6：更新 00_tables_ddl.sql

从 `pm_project` 建表语句中删除上述 19 列的定义。

---

## 执行顺序

### 本地

```
Step 1: 修改代码（Task 1 DailyReportMapper + Task 2 ProjectMapper + Task 3 Project.java + Task 4 ProjectServiceImpl）
Step 2: mvn clean compile 验证编译通过
Step 3: 本地执行 ALTER TABLE（Task 5 的 SQL）
Step 4: 本地启动，回归测试：项目列表、项目详情、新增/编辑项目、收入确认、审核、日报
Step 5: 更新 00_tables_ddl.sql（Task 6）
Step 6: git commit & push → CI/CD 自动部署
```

### 生产（在 CI/CD 部署并验证功能正常后执行）

```
Step 7: 验证生产新代码功能正常
Step 8: 删除 pm_project level=1 行（已迁移到 pm_task，安全删除）
Step 9: 执行 ALTER TABLE DROP COLUMN
```

> ⚠️ **必须等新代码部署并验证后再执行 ALTER TABLE**，否则旧代码仍在引用这些列，会立即报错。

---

## 风险提示

1. **DailyReportMapper 修改** — 需测试日报活动统计（activity.vue）中按项目过滤的功能是否正常。
2. **生产执行顺序** — 必须：新代码上线 → 验证 → 删数据行 → DROP COLUMN，不可颠倒。
3. **编译验证** — 编译通过后务必跑一遍：项目列表、项目详情、收入确认、审核列表，确认无字段缺失报错。
