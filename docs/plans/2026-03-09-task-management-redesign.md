# 任务管理改造实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将现有"子项目"改造为"任务管理"，字段聚焦任务本身，新增3个业务字段（production_batch / internal_closure_date / functional_test_date），修复工时汇总层级滚动逻辑，改造日报填写/动态页面支持多任务行。

**Architecture:** `pm_project` 自引用方案（project_level=0主项目，1子任务）。`work_category` 不属于任务实体，仅在 `pm_daily_report_detail` 中存储（填日报时选择）。工时汇总两级：子任务 actual_workload → 父项目 actual_workload 滚动汇总。日报 write.vue 对有任务的项目展开多任务行，每行独立填工时+内容+工作任务类别。

**Tech Stack:** Java 17 / Spring Boot 3 / MyBatis XML / Vue 3 Composition API / Element Plus 2

---

## Task 0：预防性 Mapper 修复——隔离子任务不污染现有查询

**背景：** `pm_project` 自引用方案中子任务 `project_level=1`。经全量 review，以下 7 个查询缺少 `project_level` 过滤，子任务创建后会出现在不该出现的页面。必须在 Task 1 之前修复。

**Files:**
- Modify: `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml`

**受影响查询：**

| 查询 ID | 页面影响 | 行号 |
|---------|---------|------|
| `selectProjectSummary` | 项目管理列表合计行虚高 | L307 |
| `selectRevenueSummary` | 公司收入确认合计失真 | L255 |
| `teamRevenueFlatWhere` | 团队收入确认列表+合计含子任务（覆盖2个查询）| L702 |
| `selectReviewList` | 立项审核列表混入子任务 | L934 |
| `selectReviewSummary` | 立项审核合计混入子任务 | L987 |
| `searchProjectsByName` | 项目搜索 autocomplete 建议子任务 | L1122 |
| `selectProjectListByDept` | 合同绑定选项含子任务 | L1027 |

### Step 1：在 ProjectMapper.xml 7 处 WHERE 中加过滤条件

在每个查询 `p.del_flag = '0'`（或 `del_flag = '0'`）之后立即追加：
```xml
AND (p.project_level IS NULL OR p.project_level = 0)
```

**selectProjectSummary（L307 `p.del_flag = '0'` 后）**
**selectRevenueSummary（L255 `p.del_flag = '0'` 后）**
**teamRevenueFlatWhere sql片段（L702 `p.del_flag = '0'` 后，自动覆盖 FlatList + FlatSummary）**
**selectReviewList（L934 `WHERE p.del_flag = '0'` 后）**
**selectReviewSummary（L987 `WHERE p.del_flag = '0'` 后）**
**searchProjectsByName（L1122 `WHERE del_flag = '0'` 后，注意此处无表别名 `p.`）**
**selectProjectListByDept（L1027 `WHERE p.del_flag = '0'` 后）**

### Step 2：验证修改

```bash
grep -c "project_level IS NULL" ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml
```
预期输出：`7`

### Step 3：Commit

```bash
git add ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml
git commit -m "fix: 预防子任务污染现有查询，7处mapper加project_level过滤"
```

---

## Task 1：数据库 Schema 变更

**Files:**
- Create: `pm-sql/fix_task_fields_20260309.sql`
- Modify: `pm-sql/init/00_tables_ddl.sql`
- Modify: `pm-sql/init/01_tables_data.sql`

### Step 1：创建 fix SQL

```sql
-- pm-sql/fix_task_fields_20260309.sql
ALTER TABLE pm_project
  ADD COLUMN `production_batch`      varchar(50) DEFAULT NULL COMMENT '投产批次(字典:sys_tcpc)'        AFTER `task_code`,
  ADD COLUMN `internal_closure_date` date        DEFAULT NULL COMMENT '提供内部闭包日期（非必填）'      AFTER `production_batch`,
  ADD COLUMN `functional_test_date`  date        DEFAULT NULL COMMENT '提供功能测试版本日期（非必填）'  AFTER `internal_closure_date`;
```

### Step 2：在本地 Docker MySQL 执行

```bash
cat pm-sql/fix_task_fields_20260309.sql | docker exec -i 3523a41063b7 \
  mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

预期：`Query OK, 0 rows affected`

### Step 3：验证

```bash
echo "DESCRIBE pm_project;" | docker exec -i 3523a41063b7 \
  mysql -u root -ppassword ry-vue | grep -E "production_batch|internal_closure|functional_test"
```

预期：输出3行字段信息。

### Step 4：同步 00_tables_ddl.sql

在 `pm_project` 建表语句 `task_code` 字段之后加入：

```sql
  `production_batch`      varchar(50) DEFAULT NULL COMMENT '投产批次(字典:sys_tcpc)',
  `internal_closure_date` date        DEFAULT NULL COMMENT '提供内部闭包日期（非必填）',
  `functional_test_date`  date        DEFAULT NULL COMMENT '提供功能测试版本日期（非必填）',
```

### Step 5：在 01_tables_data.sql 末尾追加并只执行新增行

在 `01_tables_data.sql` 末尾追加（仅追加，不重跑整个文件——整个文件用普通 INSERT，重跑会触发重复主键报错）：

```sql
-- 投产批次字典类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('投产批次', 'sys_tcpc', '0', 'admin', NOW(), '任务投产批次');
```

只执行这一条：

```bash
echo "INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES ('投产批次', 'sys_tcpc', '0', 'admin', NOW(), '任务投产批次');" \
  | docker exec -i 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

验证：

```bash
echo "SELECT dict_type, dict_name FROM sys_dict_type WHERE dict_type='sys_tcpc';" \
  | docker exec -i 3523a41063b7 mysql -u root -ppassword ry-vue
```

预期：返回 `sys_tcpc | 投产批次` 一行。

### Step 6：Commit

```bash
git add pm-sql/fix_task_fields_20260309.sql pm-sql/init/00_tables_ddl.sql pm-sql/init/01_tables_data.sql
git commit -m "feat: pm_project 新增 production_batch/internal_closure_date/functional_test_date"
```

---

## Task 2：后端 — Project 实体新增字段

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/domain/Project.java`

### Step 1：在 `taskCode` 字段声明之后加入3个字段

```java
/** 投产批次(字典:sys_tcpc) */
private String productionBatch;

/** 提供内部闭包日期 */
@JsonFormat(pattern = "yyyy-MM-dd")
private java.util.Date internalClosureDate;

/** 提供功能测试版本日期 */
@JsonFormat(pattern = "yyyy-MM-dd")
private java.util.Date functionalTestDate;
```

### Step 2：加对应 getter/setter

```java
public String getProductionBatch() { return productionBatch; }
public void setProductionBatch(String productionBatch) { this.productionBatch = productionBatch; }

public java.util.Date getInternalClosureDate() { return internalClosureDate; }
public void setInternalClosureDate(java.util.Date internalClosureDate) { this.internalClosureDate = internalClosureDate; }

public java.util.Date getFunctionalTestDate() { return functionalTestDate; }
public void setFunctionalTestDate(java.util.Date functionalTestDate) { this.functionalTestDate = functionalTestDate; }
```

### Step 3：编译验证

```bash
mvn clean compile -pl ruoyi-project -am -q
```

预期：`BUILD SUCCESS`

### Step 4：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/Project.java
git commit -m "feat: Project 实体新增 productionBatch/internalClosureDate/functionalTestDate"
```

---

## Task 3：后端 — Mapper 查询扩展

**Files:**
- Modify: `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml`

### Step 1：selectSubProjectList 加入新字段

在 `selectSubProjectList` 的 SELECT 列表末尾（`p.approval_reason,` 之后）加入：

```xml
       p.task_code, p.project_level, p.parent_id,
       p.production_batch, p.internal_closure_date, p.functional_test_date,
       ROUND(p.actual_workload / 8, 3) + COALESCE(p.adjust_workload, 0) AS actual_workload,
```

> 注意：`actual_workload` 在 selectSubProjectList 里已有（显示人天计算结果），确认是否已包含，若已有则跳过该行。

### Step 2：扩展 selectSubProjectOptions（日报多任务行需要）

当前该查询仅返回 `projectId / projectName / taskCode`，日报展开任务行需要额外显示任务阶段和任务负责人。

找到 `selectSubProjectOptions`：

```xml
<select id="selectSubProjectOptions" resultType="map">
    select project_id   as "projectId",
           project_name as "projectName",
           task_code    as "taskCode"
    from pm_project
    where del_flag = '0'
      and parent_id = #{parentId}
      and project_level = 1
    order by task_code, create_time
```

替换为：

```xml
<select id="selectSubProjectOptions" resultType="map">
    select p.project_id          as "projectId",
           p.project_name        as "projectName",
           p.task_code           as "taskCode",
           p.project_stage       as "projectStage",
           p.project_manager_id  as "projectManagerId",
           u.nick_name           as "projectManagerName"
    from pm_project p
    left join sys_user u on p.project_manager_id = u.user_id
    where p.del_flag = '0'
      and p.parent_id = #{parentId}
      and p.project_level = 1
    order by p.task_code, p.create_time
```

### Step 3：DailyReportMapper.xml — 子任务字段扩展

activity.vue 展示任务阶段和任务负责人，需要在 `selectDailyReportList` 的 SQL 中扩展子任务 JOIN 返回字段。

找到 `DailyReportMapper.xml` 中 `left join pm_project sp on dd.sub_project_id = sp.project_id` 附近，在 SELECT 列表中加入：

```xml
sp.project_stage      as detail_sub_project_stage,
sp.project_manager_id as detail_sub_project_manager_id,
su.nick_name          as detail_sub_project_manager_name,
```

并在 JOIN 部分加：

```xml
left join sys_user su on sp.project_manager_id = su.user_id
           and su.del_flag = '0'
```

同步更新 resultMap 的 `<collection>` 中加入映射：

```xml
<result property="subProjectStage"       column="detail_sub_project_stage"        />
<result property="subProjectManagerId"   column="detail_sub_project_manager_id"   />
<result property="subProjectManagerName" column="detail_sub_project_manager_name" />
```

### Step 4：DailyReportDetail.java 加字段

```java
/** 子任务阶段（扩展字段） */
private String subProjectStage;

/** 子任务负责人ID（扩展字段） */
private Long subProjectManagerId;

/** 子任务负责人姓名（扩展字段） */
private String subProjectManagerName;

// getter/setter 略
```

同时在 DailyReportMapper.xml 主项目部分加入项目经理信息：

在 SELECT 中加：`pu.nick_name as detail_project_manager_name,`

加 JOIN：`left join sys_user pu on p.project_manager_id = pu.user_id and pu.del_flag = '0'`

resultMap 中加：`<result property="projectManagerName" column="detail_project_manager_name" />`

DailyReportDetail.java 加字段：`private String projectManagerName;`

### Step 5：编译验证

```bash
mvn clean compile -pl ruoyi-project -am -q
```

预期：`BUILD SUCCESS`

### Step 6：Commit

```bash
git add ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml \
        ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml \
        ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReportDetail.java
git commit -m "feat: 扩展子任务选项返回阶段/负责人，日报明细返回项目经理/子任务阶段和负责人"
```

---

## Task 3.5：后端 — 子任务创建/更新时跳过成员写入

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java`

### 背景

子任务（`project_level=1`）的参与人员继承自父项目，不独立维护 `pm_project_member`。

### Step 1：insertProject 成员写入加保护

在 `ProjectServiceImpl.insertProject()` 中，找到往 `pm_project_member` 插入成员的代码块，整体包裹：

```java
// project_level NOT NULL DEFAULT 0，无需判断 null
if (project.getProjectLevel() == 0) {
    // --- 原有的 pm_project_member 插入逻辑保持不变 ---
}
```

### Step 2：updateProject 成员更新加保护

```java
if (project.getProjectLevel() == 0) {
    // --- 原有的成员删除+插入逻辑保持不变 ---
}
```

### Step 3：编译验证

```bash
mvn clean compile -pl ruoyi-project -am -q
```

### Step 4：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java
git commit -m "fix: 子任务创建/更新时跳过 pm_project_member 写入，成员继承父项目"
```

---

## Task 4：后端 — 工时汇总层级滚动修复

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportDetailMapper.java`
- Modify: `ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java`
- Modify: `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java`

### Step 1：DailyReportDetailMapper.java 新增方法

```java
/** 统计指定子任务在所有日报中的总工时(小时) */
BigDecimal sumWorkHoursBySubProjectId(@Param("subProjectId") Long subProjectId);
```

### Step 2：DailyReportDetailMapper.xml 新增 SQL

```xml
<select id="sumWorkHoursBySubProjectId" parameterType="Long" resultType="java.math.BigDecimal">
    select coalesce(sum(work_hours), 0)
    from pm_daily_report_detail
    where sub_project_id = #{subProjectId}
      and entry_type = 'work'
      and del_flag = '0'
</select>
```

### Step 3：ProjectMapper.java 新增方法

```java
/** 汇总主项目下所有子任务的 actual_workload(小时) */
BigDecimal sumSubTasksWorkload(@Param("parentId") Long parentId);
```

### Step 4：ProjectMapper.xml 新增 SQL

```xml
<select id="sumSubTasksWorkload" resultType="java.math.BigDecimal">
    select coalesce(sum(actual_workload), 0)
    from pm_project
    where parent_id = #{parentId}
      and project_level = 1
      and del_flag = '0'
</select>
```

### Step 5：修改 DailyReportServiceImpl.java 工时汇总逻辑

找到约 194-204 行的工时汇总块，替换为：

```java
List<DailyReportDetail> workDetails = (detailList != null ? detailList : Collections.<DailyReportDetail>emptyList())
        .stream()
        .filter(d -> d.getProjectId() != null && "work".equals(d.getEntryType()))
        .collect(Collectors.toList());

// Step 1：更新受影响子任务工时
Set<Long> affectedSubProjectIds = workDetails.stream()
        .filter(d -> d.getSubProjectId() != null)
        .map(DailyReportDetail::getSubProjectId)
        .collect(Collectors.toSet());
for (Long subProjectId : affectedSubProjectIds) {
    BigDecimal subHours = detailMapper.sumWorkHoursBySubProjectId(subProjectId);
    projectMapper.updateActualWorkload(subProjectId, subHours);
}

// Step 2：更新主项目工时（有子任务→汇总子任务，无子任务→直接汇总明细）
Set<Long> affectedProjectIds = workDetails.stream()
        .map(DailyReportDetail::getProjectId)
        .collect(Collectors.toSet());
for (Long projectId : affectedProjectIds) {
    List<Long> hasSubList = projectMapper.selectProjectsHasSubProject(Collections.singletonList(projectId));
    if (!hasSubList.isEmpty()) {
        BigDecimal totalHours = projectMapper.sumSubTasksWorkload(projectId);
        projectMapper.updateActualWorkload(projectId, totalHours);
    } else {
        BigDecimal totalHours = detailMapper.sumWorkHoursByProjectId(projectId);
        projectMapper.updateActualWorkload(projectId, totalHours);
    }
}
```

### Step 6：编译验证

```bash
mvn clean compile -pl ruoyi-project -am -q
```

### Step 7：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportDetailMapper.java \
        ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml \
        ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java \
        ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml \
        ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java
git commit -m "fix: 日报保存工时汇总支持子任务层级滚动更新"
```

---

## Task 5：前端 — 任务管理页面重写

**Files:**
- Modify: `ruoyi-ui/src/views/project/subproject/index.vue`
- Modify: `ruoyi-ui/src/views/project/subproject/add.vue`
- Modify: `ruoyi-ui/src/views/project/subproject/edit.vue`
- Modify: `ruoyi-ui/src/views/project/subproject/detail.vue`

### Step 1：重写 add.vue

**表单字段布局（移除所有客户/销售/区域/工作任务类别相关字段）：**

必填：任务负责人、任务名称、任务阶段、预估工作量、启动日期、结束日期

非必填：任务编号、任务预算、投产批次、任务计划、任务描述、投产时间、内部闭包日期、功能测试版本日期、备注

**参与人员（只读）：** 从父项目继承，展示为 `el-tag` 列表，不提交

完整 add.vue：

```vue
<template>
  <div class="app-container">
    <h2 style="margin: 0 0 6px 0; font-weight: bold;">新增任务</h2>

    <el-alert v-if="parentProject" type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>
        所属项目：{{ parentProject.projectName }}（{{ parentProject.projectCode }}）
      </template>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="160px">

      <!-- 一、基本信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">一、基本信息</span></template>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务负责人" prop="projectManagerId">
              <user-select v-model="form.projectManagerId" post-code="pm" placeholder="请选择任务负责人" filterable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务名称" prop="projectName">
              <el-input v-model="form.projectName" placeholder="请输入任务名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务编号" prop="taskCode">
              <el-input v-model="form.taskCode" placeholder="如：01、用户系统（同一主项目下需唯一）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务阶段" prop="projectStage">
              <dict-select v-model="form.projectStage" dict-type="sys_xmjd" placeholder="请选择任务阶段" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预估工作量" prop="estimatedWorkload">
              <el-input v-model="form.estimatedWorkload" placeholder="请输入">
                <template #append>人天</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务预算">
              <el-input v-model="form.projectBudget" placeholder="请输入金额">
                <template #append>元</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="投产批次">
              <dict-select v-model="form.productionBatch" dict-type="sys_tcpc" placeholder="请选择投产批次" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="参与人员">
              <div v-if="parentParticipants.length > 0">
                <el-tag v-for="u in parentParticipants" :key="u.userId"
                  type="info" style="margin: 0 4px 4px 0">{{ u.nickName }}</el-tag>
              </div>
              <span v-else style="color: #909399;">主项目暂无参与人员</span>
              <div style="color: #909399; font-size: 12px; margin-top: 4px;">继承自主项目，不可修改</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="任务计划">
              <el-input v-model="form.projectPlan" type="textarea" :rows="3" placeholder="请输入任务计划" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="任务描述">
              <el-input v-model="form.projectDescription" type="textarea" :rows="3" placeholder="请输入任务描述" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 二、时间规划 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">二、时间规划</span></template>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="启动日期" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" placeholder="选择启动日期"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" placeholder="选择结束日期"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="投产时间">
              <el-date-picker v-model="form.productionDate" type="date" placeholder="选填"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内部闭包日期">
              <el-date-picker v-model="form.internalClosureDate" type="date" placeholder="选填"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="功能测试版本日期">
              <el-date-picker v-model="form.functionalTestDate" type="date" placeholder="选填"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 三、备注 -->
      <el-card shadow="hover" style="margin-bottom: 15px;">
        <template #header><span style="font-size: 16px; font-weight: bold;">三、备注</span></template>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注（选填）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>
    </el-form>

    <div class="form-footer">
      <el-button type="primary" size="large" @click="submitForm">保存</el-button>
      <el-button size="large" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script setup name="TaskAdd">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { addProject, getProject, getUsersByPost } from '@/api/project/project'
import { getCurrentInstance } from 'vue'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const formRef = ref()
const parentProject = ref(null)
const parentParticipants = ref([])

const form = ref({
  taskCode: null, projectName: null, projectManagerId: null, projectStage: null,
  estimatedWorkload: null, projectBudget: null, productionBatch: null,
  projectPlan: null, projectDescription: null,
  startDate: null, endDate: null, productionDate: null,
  internalClosureDate: null, functionalTestDate: null, remark: null
})

const rules = ref({
  taskCode:         [{ required: true, message: '任务编号不能为空（同一主项目下需唯一，用于生成项目编号）', trigger: 'blur' }],
  projectManagerId: [{ required: true, message: '任务负责人不能为空', trigger: 'change' }],
  projectName:      [{ required: true, message: '任务名称不能为空',   trigger: 'blur'   }],
  projectStage:     [{ required: true, message: '任务阶段不能为空',   trigger: 'change' }],
  estimatedWorkload:[{ required: true, message: '预估工作量不能为空', trigger: 'blur'   }],
  startDate:        [{ required: true, message: '启动日期不能为空',   trigger: 'change' }],
  endDate:          [{ required: true, message: '结束日期不能为空',   trigger: 'change' }]
})

async function loadParentProject(parentId) {
  try {
    const [projectRes, usersRes] = await Promise.all([
      getProject(parentId),
      getUsersByPost()
    ])
    parentProject.value = projectRes.data
    const ids = (projectRes.data.participants || '').split(',').map(Number).filter(Boolean)
    parentParticipants.value = (usersRes.data || []).filter(u => ids.includes(u.userId))
  } catch (e) {
    console.error('加载主项目信息失败', e)
  }
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    const parentId = route.query.parentId ? Number(route.query.parentId) : null
    const p = parentProject.value || {}
    const taskCode = form.value.taskCode  // 已校验必填，此处必有值
    const projectCode = p.projectCode ? `${p.projectCode}-${taskCode}` : null
    const submitData = {
      ...form.value,
      parentId,
      projectLevel: 1,
      projectCode,
      projectDept: p.projectDept || null,
      industry: p.industry || null,
      region: p.region || null,
      regionId: p.regionId || null,
      regionCode: p.regionCode || null,
      shortName: p.shortName || null,
      establishedYear: p.establishedYear || null,
      projectCategory: p.projectCategory || null,
      projectBudget: form.value.projectBudget ? parseFloat(String(form.value.projectBudget).replace(/,/g, '')) : null,
      estimatedWorkload: form.value.estimatedWorkload ? parseFloat(form.value.estimatedWorkload) : null
    }
    addProject(submitData).then(() => {
      proxy.$modal.msgSuccess('新增成功')
      router.push({ path: '/project/subproject', query: { parentId } })
    })
  })
}

function cancel() {
  const parentId = route.query.parentId ? Number(route.query.parentId) : null
  router.push({ path: '/project/subproject', query: { parentId } })
}

onMounted(() => {
  const parentId = route.query.parentId
  if (parentId) loadParentProject(Number(parentId))
})
</script>

<style scoped>
.app-container { padding-bottom: 80px; }
.form-footer {
  position: sticky; bottom: 0; padding: 20px;
  background-color: #fff; border-top: 1px solid #dcdfe6;
  text-align: center; z-index: 10;
}
.form-footer .el-button { min-width: 120px; margin: 0 10px; }
</style>
```

### Step 2：重写 edit.vue

结构与 add.vue 完全相同，差异：
- 标题改为"编辑任务"
- `onMounted` 额外调用 `getProject(taskId)` 回填表单（`taskId` 来自 `route.params.taskId` 或 `route.query.taskId`）
- 提交调用 `updateProject(submitData)`，需包含 `projectId`
- 日期字段回填：后端 `@JsonFormat` 已配置 `yyyy-MM-dd`，直接赋值到 date-picker 的 `value-format="YYYY-MM-DD"` 即可
- 参与人员同样从父项目加载（通过 `project.parentId` 获取父项目ID）

### Step 3：更新 index.vue 查询条件和列表列

**查询条件加入项目部门 + 联动所属主项目：**

```vue
<!-- 1. 新增项目部门筛选 -->
<el-form-item label="项目部门">
  <project-dept-select v-model="queryParams.projectDept" @change="handleDeptChange" clearable />
</el-form-item>

<!-- 2. 所属主项目（由项目部门联动过滤） -->
<el-form-item label="所属主项目">
  <el-select v-model="queryParams.parentId" placeholder="请选择主项目" filterable clearable
    :disabled="!queryParams.projectDept">
    <el-option v-for="p in parentProjectOptions"
      :key="p.projectId" :label="p.projectName" :value="p.projectId" />
  </el-select>
</el-form-item>
```

联动逻辑（`handleDeptChange`）：当部门变更时，清空 `parentId`，调用接口查询该部门下的主项目列表填充下拉：

```js
async function handleDeptChange(deptId) {
  queryParams.value.parentId = null
  parentProjectOptions.value = []
  if (!deptId) return
  const res = await request({
    url: '/project/project/list',
    method: 'get',
    params: { projectDept: deptId, projectLevel: 0, pageNum: 1, pageSize: 200 }
  })
  parentProjectOptions.value = res.rows || []
}
```

**列表列调整（保留 + 新增）：**

| 列 | 说明 |
|---|---|
| 任务编号（taskCode）| 保留 |
| 任务名称（projectName）| 保留 |
| 所属主项目名称 | 新增，通过 parentId 查询或在后端 selectSubProjectList 加 JOIN 返回 |
| 任务阶段 | 保留 |
| 任务负责人 | 保留 |
| 预估工作量 | 保留 |
| 实际工作量（actual_workload）| **明确保留**，展示任务累计工时 |
| 投产批次 | 新增 |
| 启动日期 / 结束日期 | 保留 |
| 操作 | 保留 |

> 注意：selectSubProjectList 需要额外返回父项目名称，在 ProjectMapper.xml 中加 `left join pm_project pp on p.parent_id = pp.project_id` 并返回 `pp.project_name as parentProjectName`，Project.java 加 `transient String parentProjectName` 字段。

### Step 4：更新 detail.vue

**基本信息区新增：** 投产批次（`<dict-tag dict-type="sys_tcpc">`）

**时间规划区新增：** 提供内部闭包日期、提供功能测试版本日期

**去掉：** 客户信息、销售信息、成本细项、工作任务类别（因为 work_category 不在任务上）

### Step 5：Commit

```bash
git add ruoyi-ui/src/views/project/subproject/
git commit -m "feat: 任务管理页面重写，精简表单，新增项目部门/主项目联动查询，保留 actual_workload 列"
```

---

## Task 6：前端 — 日报填写 write.vue 多任务行支持

**Files:**
- Modify: `ruoyi-ui/src/views/project/dailyReport/write.vue`

### 背景与设计

**现状：** 每个项目只有一个任务下拉 + 一条工时记录（1:1）

**新需求：** 有任务的项目展开为多任务行，每行独立填工时（1:N）

```
[项目：XX系统建设]
  ├─ [任务A] [开发阶段] [张三负责]  [工作任务类别▼] [4h] [工作内容...]
  ├─ [任务B] [测试阶段] [李四负责]  [工作任务类别▼] [2h] [工作内容...]
  └─ [任务C] [上线阶段] [王五负责]  [工作任务类别▼] [0h] （不填=不保存）
保存时：为每个 workHours > 0 的任务行生成一条 detail 记录
```

### Step 1：数据结构调整

每个项目的 item 对象从：

```js
{ projectId, workHours, workContent, subProjectId, workCategory, hasSubProject, subProjectOptions }
```

改为：

```js
{
  projectId,
  hasSubProject,
  // 无子任务时使用（原有字段）
  workHours, workContent, workCategory,
  // 有子任务时使用
  taskRows: [
    {
      subProjectId,       // 任务ID
      taskName,           // 任务名称（只读展示）
      projectStage,       // 任务阶段（只读展示）
      projectManagerName, // 任务负责人（只读展示）
      workCategory,       // 工作任务类别（可编辑）
      workHours,          // 工时（可编辑）
      workContent         // 工作内容（可编辑）
    }
  ]
}
```

### Step 2：加载任务行数据

`loadSubProjectOptions(item)` 调整为：加载后直接构建 `item.taskRows`，而不是仅填充下拉选项：

```js
async function loadTaskRows(item) {
  if (item.taskRows && item.taskRows.length > 0) return
  const res = await getSubProjectOptions(item.projectId)
  const tasks = res.data || []
  // 如果是加载已有日报，匹配已有明细
  item.taskRows = tasks.map(t => {
    const existingDetail = (item._existingDetails || []).find(d => d.subProjectId === t.projectId)
    return {
      subProjectId: t.projectId,
      taskName: t.taskCode ? `[${t.taskCode}] ${t.projectName}` : t.projectName,
      projectStage: t.projectStage,
      projectManagerName: t.projectManagerName || '-',
      workCategory: existingDetail?.workCategory || null,
      workHours: existingDetail ? Number(existingDetail.workHours) : 0,
      workContent: existingDetail?.workContent || ''
    }
  })
}
```

### Step 3：模板 — 有任务项目的展开行

在项目列表的每个 item 渲染块中，将原来的单行工时区域改为条件渲染：

```vue
<!-- 无子任务：原有单行逻辑 -->
<template v-if="!item.hasSubProject">
  <!-- 工时滑块 + 工作内容 textarea（现有代码不变）-->
</template>

<!-- 有子任务：多任务行 -->
<template v-else>
  <div class="task-rows-container">
    <div v-for="(task, idx) in item.taskRows" :key="task.subProjectId" class="task-row">
      <!-- 任务头（只读信息） -->
      <div class="task-row-header">
        <span class="task-name">{{ task.taskName }}</span>
        <el-tag size="small" type="info">{{ task.projectStage | dictLabel('sys_xmjd') }}</el-tag>
        <span class="task-manager">负责人：{{ task.projectManagerName }}</span>
      </div>
      <!-- 工作任务类别 + 工时 -->
      <div class="task-row-inputs">
        <el-select v-model="task.workCategory" placeholder="工作任务类别" clearable
          size="small" style="width: 150px;" :disabled="!isEditable">
          <el-option v-for="d in sys_gzlb" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
        <el-input-number v-model="task.workHours" :min="0" :max="24" :step="0.5"
          size="small" style="width: 110px;" :disabled="!isEditable" />
        <span style="font-size: 12px; color: #909399;">小时</span>
      </div>
      <!-- 工作内容 -->
      <el-input v-model="task.workContent" type="textarea" :rows="2"
        placeholder="请填写工作内容" :disabled="!isEditable"
        style="margin-top: 6px;" />
    </div>
  </div>
</template>
```

> `dictLabel` 过滤器：若项目中无全局过滤器，直接用已有的 `proxy.useDict('sys_xmjd')` 的 label 映射替代。

### Step 4：保存逻辑调整

在 `buildSavePayload()` 或现有的 detail 构建块中，对有子任务的项目，遍历 `taskRows` 生成多条 detail：

```js
// 现有逻辑：一个项目一条明细
// 新逻辑：有子任务的项目，每个 workHours > 0 的任务行生成一条明细
projects.value.forEach(item => {
  if (item.hasSubProject) {
    ;(item.taskRows || [])
      .filter(t => t.workHours > 0)
      .forEach(t => {
        details.push({
          projectId: item.projectId,
          projectStage: item.projectStage,
          workHours: t.workHours,
          workContent: t.workContent,
          entryType: 'work',
          subProjectId: t.subProjectId,
          workCategory: t.workCategory || null
        })
      })
  } else {
    // 原有逻辑
    if (item.workHours > 0) {
      details.push({ projectId: item.projectId, ... })
    }
  }
})
```

### Step 5：加载已有日报回填多任务行

在 `loadDayReport()` 中，当拿到已有日报的 detailList 后，对有子任务的项目需要把多条 detail 回填到对应的 `taskRows`：

```js
// item._existingDetails 存储该项目的所有 detail 记录（包含 subProjectId）
// loadTaskRows(item) 时会用 _existingDetails 匹配回填
item._existingDetails = detailList.filter(d => d.projectId === item.projectId && d.subProjectId)
await loadTaskRows(item)
```

> 注意：`loadTaskRows` 需要在项目列表加载完、且已有日报 detailList 拿到后调用（而非按需懒加载）。当日期切换时需要 reset `taskRows` 再重新加载。

### Step 6：Commit

```bash
git add ruoyi-ui/src/views/project/dailyReport/write.vue
git commit -m "feat: 日报填写支持有任务项目的多任务行独立填写工时和工作内容"
```

---

## Task 7：前端 — 工作日报动态 activity.vue 项目/任务信息区分

**Files:**
- Modify: `ruoyi-ui/src/views/project/dailyReport/activity.vue`

### 需求

**日报详情每条 detail 展示结构：**

```
项目：[项目经理姓名]  [项目名称]  预计人天: X  已花人天: X  [项目阶段]  Xh
任务：[任务名称]  [任务阶段]  负责人: [任务负责人]  [任务类别]    ← 有 subProjectId 才显示
工作内容: ...
```

### Step 1：找到当前 detail 渲染块（约 178-196 行）

当前结构：

```vue
<div style="display: flex; ...">
  <el-tag ...>{{ detail.projectName }}</el-tag>
  <el-tag ...>{{ detail.projectStageName }}</el-tag>
  <span>{{ detail.workHours }}h</span>
</div>
<div v-if="detail.subProjectName || detail.workCategory" ...>
  <el-tag v-if="detail.subProjectName">{{ detail.subProjectName }}</el-tag>
  <dict-tag v-if="detail.workCategory" .../>
</div>
<div>预计人天... 已花人天...</div>
```

### Step 2：替换为新结构

```vue
<!-- 项目行：项目经理 | 项目名称 | 工时 | 项目阶段 -->
<div class="detail-project-row">
  <span v-if="detail.projectManagerName" class="manager-label">
    {{ detail.projectManagerName }}
  </span>
  <el-tag size="small" type="primary">{{ detail.projectName }}</el-tag>
  <el-tag size="small" type="info">{{ detail.projectStageName }}</el-tag>
  <span style="margin-left: auto; font-weight: 700;">{{ detail.workHours }}h</span>
</div>

<!-- 人天行 -->
<div style="font-size: 12px; color: #909399; margin: 2px 0;">
  预计人天：<strong>{{ detail.estimatedWorkload ?? '-' }}</strong>
  &nbsp;&nbsp;已花人天：<strong>{{ detail.actualWorkload != null ? Number(detail.actualWorkload).toFixed(3) : '-' }}</strong>
</div>

<!-- 任务行：仅有 subProjectId 时显示 -->
<div v-if="detail.subProjectId" class="detail-task-row">
  <el-tag size="small" type="success">{{ detail.subProjectName }}</el-tag>
  <el-tag v-if="detail.subProjectStage" size="small" type="warning">
    <dict-tag :options="sys_xmjd" :value="detail.subProjectStage" />
  </el-tag>
  <span v-if="detail.subProjectManagerName" style="font-size: 12px; color: #606266;">
    负责人：{{ detail.subProjectManagerName }}
  </span>
  <dict-tag v-if="detail.workCategory" :options="sys_gzlb" :value="detail.workCategory" />
</div>

<!-- 工作内容 -->
<div style="font-size: 13px; color: #606266; line-height: 1.6; padding-left: 8px; border-left: 2px solid #e4e7ed;"
  v-html="formatWorkContent(detail.workContent)"></div>
```

### Step 3：加入缺失的字典引用

在 `useDict` 调用中加入 `sys_xmjd`（如果已有则跳过）：

```js
const { sys_gzlb, sys_xmjd } = proxy.useDict('sys_gzlb', 'sys_xmjd')
```

### Step 4：Commit

```bash
git add ruoyi-ui/src/views/project/dailyReport/activity.vue
git commit -m "feat: 工作日报动态详情区分项目/任务信息，新增项目经理和任务阶段/负责人展示"
```

---

## Task 8：端到端验证

### Step 1：启动服务

```bash
# 后端
mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true -q
java -Xms512m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar

# 前端
cd ruoyi-ui && npm run dev
```

### Step 2：验证点

**任务管理：**
1. 新增任务：表单仅显示任务相关字段，参与人员自动带出父项目成员（只读）
2. 任务列表：项目部门筛选 → 主项目下拉联动过滤 → 正常查询，actual_workload 列有值
3. 投产批次下拉：需先在系统管理→字典管理→投产批次中添加字典值

**日报填写：**
4. 选择有任务的项目 → 展开多任务行，每行显示任务名/阶段/负责人
5. 分别给任务A填4h、任务B填2h → 保存
6. 验证：任务A.actual_workload=4h，任务B.actual_workload=2h，父项目.actual_workload=6h
7. 切换日期回来 → 任务行已填数据正确回填

**工作日报动态：**
8. 查看刚填的日报详情：项目行显示项目经理，任务行显示任务名/阶段/负责人/类别
9. 无任务的项目：只显示项目行，不显示任务行

---

## 字段映射总结

| 领导需求 | 表单字段 | 数据库字段 | 备注 |
|---------|---------|---------|------|
| 任务负责人 | projectManagerId | project_manager_id | 已有 |
| 任务名称 | projectName | project_name | 已有 |
| 任务编号（**必填**，同一父项目下唯一）| taskCode | task_code | 已有 |
| 任务阶段 | projectStage | project_stage | 已有 |
| 任务预算 | projectBudget | project_budget | 已有 |
| 预估工作量 | estimatedWorkload | estimated_workload | 已有 |
| **实际工作量** | actualWorkload | actual_workload | 已有，Task 4 滚动汇总 |
| 投产批次 | productionBatch | production_batch | **新增** |
| 任务计划 | projectPlan | project_plan | 已有 |
| 任务描述 | projectDescription | project_description | 已有 |
| 投产时间 | productionDate | production_date | 已有 |
| 内部闭包日期（非必填）| internalClosureDate | internal_closure_date | **新增** |
| 功能测试版本日期（非必填）| functionalTestDate | functional_test_date | **新增** |
| 启动日期 | startDate | start_date | 已有 |
| 结束日期 | endDate | end_date | 已有 |
| 备注（非必填）| remark | remark | 已有 |
| 工作任务类别 | workCategory（日报明细）| work_category（pm_daily_report_detail）| 日报填写时选，不在任务表单 |
