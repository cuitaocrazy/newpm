# 任务管理改造实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将现有「任务管理」拆分为「项目分解任务」和「任务管理」两个菜单；新增批次联动选择；pm_project 表新增 bank_demand_no / software_demand_no / product / production_year / batch_id 字段；任务管理列表增加多个查询条件。

**Tech Stack:** Java 17 / Spring Boot 3 / MyBatis XML / Vue 3 Composition API / Element Plus 2

---

## Task 0：数据库 Schema 变更

**Files:**
- Create: `pm-sql/fix_task_redesign_20260311.sql`
- Modify: `pm-sql/init/00_tables_ddl.sql`

### Step 1：创建 fix SQL

```sql
-- pm-sql/fix_task_redesign_20260311.sql

-- 1. 删除旧的 production_batch 列（清空历史字典数据），新增 batch_id 外键列
ALTER TABLE pm_project
  DROP COLUMN production_batch,
  ADD COLUMN `batch_id`           bigint       DEFAULT NULL COMMENT '投产批次ID(FK:pm_production_batch.batch_id)' AFTER task_code,
  ADD COLUMN `production_year`    varchar(10)  DEFAULT NULL COMMENT '投产年份(字典:sys_ndgl)'                     AFTER batch_id,
  ADD COLUMN `bank_demand_no`     varchar(100) DEFAULT NULL COMMENT '总行需求号'                                   AFTER functional_test_date,
  ADD COLUMN `software_demand_no` varchar(100) DEFAULT NULL COMMENT '软件中心需求编号'                            AFTER bank_demand_no,
  ADD COLUMN `product`            varchar(50)  DEFAULT NULL COMMENT '二级产品(字典:sys_product)'                  AFTER software_demand_no;
```

### Step 2：同步更新 00_tables_ddl.sql 中 pm_project 建表语句

在 `task_code` 行后添加：
```sql
`batch_id`           bigint       DEFAULT NULL COMMENT '投产批次ID(FK:pm_production_batch.batch_id)',
`production_year`    varchar(10)  DEFAULT NULL COMMENT '投产年份(字典:sys_ndgl)',
```
在 `functional_test_date` 行后添加：
```sql
`bank_demand_no`     varchar(100) DEFAULT NULL COMMENT '总行需求号',
`software_demand_no` varchar(100) DEFAULT NULL COMMENT '软件中心需求编号',
`product`            varchar(50)  DEFAULT NULL COMMENT '二级产品(字典:sys_product)',
```
同时删除 `production_batch` 行。

### Step 3：本地 + 远程执行 fix SQL

```bash
# 本地
cat pm-sql/fix_task_redesign_20260311.sql | docker exec -i 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue

# 远程
cat pm-sql/fix_task_redesign_20260311.sql | ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
```

---

## Task 1：后端 Java 变更

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/domain/Project.java`
- Modify: `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProductionBatchController.java`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/IProductionBatchService.java`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProductionBatchServiceImpl.java`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProductionBatchMapper.java`
- Modify: `ruoyi-project/src/main/resources/mapper/project/ProductionBatchMapper.xml`

### Step 1：Project.java 字段变更

- 删除 `String productionBatch` 及其 getter/setter 和 toString 中的 `.append("productionBatch", ...)`
- 新增字段：
```java
/** 投产批次ID */
private Long batchId;

/** 投产年份 */
private String productionYear;

/** 总行需求号 */
private String bankDemandNo;

/** 软件中心需求编号 */
private String softwareDemandNo;

/** 二级产品 */
private String product;

/** 批次号（关联字段，非数据库字段）*/
private String batchNo;

/** 计划投产日期（关联字段，非数据库字段）*/
@JsonFormat(pattern = "yyyy-MM-dd")
private Date planProductionDate;
```

### Step 2：ProjectMapper.xml 变更

**resultMap 更新：**
- 删除 `production_batch` 映射
- 新增：`batch_id`, `production_year`, `bank_demand_no`, `software_demand_no`, `product` 映射
- 新增关联字段：`batchNo`, `planProductionDate`

**selectProjectList / selectSubProjectList：**
- SELECT 中删除 `p.production_batch`（注：该字段历史上从未在 resultMap 中映射，删除无数据风险），加入新字段 `p.batch_id`, `p.production_year`, `p.bank_demand_no`, `p.software_demand_no`, `p.product`
- 新增 LEFT JOIN：`LEFT JOIN pm_production_batch pb ON p.batch_id = pb.batch_id`
- 带出 `pb.batch_no AS batch_no`, `pb.plan_production_date`

**selectSubProjectList WHERE 关键改造（parentId 解绑）：**

将原有硬编码条件（非 `<if>` 包裹，line 651）：
```xml
and p.parent_id = #{parentId}
```
改为可选条件，并补充常驻 project_level 过滤（防止主项目混入）：
```xml
and p.project_level = 1
<if test="parentId != null">and p.parent_id = #{parentId}</if>
```

**selectSubProjectList WHERE 新增条件（仅新增，勿重复添加已有的 projectManagerId/projectName/projectStage/projectStatus 条件）：**
```xml
<if test="projectDept != null and projectDept != ''">
  and (p.project_dept = #{projectDept}
       or p.project_dept in (select dept_id from sys_dept where find_in_set(#{projectDept}, ancestors) > 0))
</if>
<if test="taskCode != null and taskCode != ''">and p.task_code like concat('%', #{taskCode}, '%')</if>
<if test="batchId != null">and p.batch_id = #{batchId}</if>
<if test="productionYear != null and productionYear != ''">and p.production_year = #{productionYear}</if>
<if test="bankDemandNo != null and bankDemandNo != ''">and p.bank_demand_no like concat('%', #{bankDemandNo}, '%')</if>
<if test="softwareDemandNo != null and softwareDemandNo != ''">and p.software_demand_no like concat('%', #{softwareDemandNo}, '%')</if>
<if test="product != null and product != ''">and p.product = #{product}</if>
<if test="parentRevenueConfirmYear != null and parentRevenueConfirmYear != ''">
  and exists (select 1 from pm_project pp where pp.project_id = p.parent_id and pp.revenue_confirm_year = #{parentRevenueConfirmYear})
</if>
```

**insert / update：**
- 删除 production_batch
- 新增 batch_id, production_year, bank_demand_no, software_demand_no, product

**searchProjectsByName 查询扩展（支持 projectDept 过滤）：**

将现有 `searchProjectsByName` 查询增加 projectDept 条件：
```xml
<select id="searchProjectsByName" resultType="java.util.Map">
    SELECT project_id as projectId, project_name as projectName, project_code as projectCode
    FROM pm_project
    WHERE del_flag = '0'
    AND (project_level IS NULL OR project_level = 0)
    <if test="projectDept != null and projectDept != ''">
        AND (project_dept = #{projectDept}
             OR project_dept IN (SELECT dept_id FROM sys_dept WHERE FIND_IN_SET(#{projectDept}, ancestors) > 0))
    </if>
    <if test="projectName != null and projectName != ''">
        AND project_name LIKE CONCAT('%', #{projectName}, '%')
    </if>
    ORDER BY create_time DESC
    LIMIT 20
</select>
```

同步更新 `ProjectMapper.java` 中 `searchProjectsByName` 方法签名：
```java
List<Map<String, Object>> searchProjectsByName(
    @Param("projectName") String projectName,
    @Param("projectDept") String projectDept);
```

同步更新 `IProjectService.java` + `ProjectServiceImpl.java` 的方法签名以透传 projectDept。

同步更新 `ProjectController.java` 的 `/search` 端点：
```java
@GetMapping("/search")
public AjaxResult searchProjects(
    @RequestParam(required = false) String projectName,
    @RequestParam(required = false) String projectDept) {
    return success(projectService.searchProjectsByName(projectName, projectDept));
}
```

**新增 searchTaskCode 查询（供任务编号 autoComplete）：**
```xml
<select id="searchTaskCode" resultType="string">
    SELECT DISTINCT task_code
    FROM pm_project
    WHERE del_flag = '0'
    AND project_level = 1
    AND task_code IS NOT NULL
    <if test="taskCode != null and taskCode != ''">
        AND task_code LIKE CONCAT('%', #{taskCode}, '%')
    </if>
    ORDER BY task_code
    LIMIT 20
</select>
```

同步在 `ProjectMapper.java` 中新增：
```java
List<String> searchTaskCode(@Param("taskCode") String taskCode);
```

同步在 `IProjectService.java` + `ProjectServiceImpl.java` 新增 `searchTaskCode`。

同步在 `ProjectController.java` 新增端点：
```java
@GetMapping("/searchTaskCode")
@PreAuthorize("@ss.hasAnyPermi('project:subproject:list,project:subproject:add')")
public AjaxResult searchTaskCode(@RequestParam(required = false) String taskCode) {
    return success(projectService.searchTaskCode(taskCode));
}
```

前端调用路径：`GET /project/project/searchTaskCode?taskCode=xxx`

### Step 3：ProductionBatchController 新增 byYear 端点

`batchByYear` 端点归属 `ProductionBatchController`（而非 ProjectController），根据年份查询批次选项：

```java
@PreAuthorize("@ss.hasAnyPermi('project:subproject:list,project:subproject:add')")
@GetMapping("/byYear")
public AjaxResult byYear(@RequestParam String productionYear) {
    return success(productionBatchService.selectByYear(productionYear));
}
```

前端调用路径：`GET /project/productionBatch/byYear?productionYear=xxx`

### Step 4：ProductionBatchService + Mapper 新增 selectByYear

**IProductionBatchService.java：**
```java
List<ProductionBatch> selectByYear(String year);
```

**ProductionBatchServiceImpl.java：**
```java
@Override
public List<ProductionBatch> selectByYear(String year) {
    return productionBatchMapper.selectByYear(year);
}
```

**ProductionBatchMapper.java：**
```java
List<ProductionBatch> selectByYear(String year);
```

**ProductionBatchMapper.xml：**
```xml
<select id="selectByYear" parameterType="String" resultType="com.ruoyi.project.domain.ProductionBatch">
    select batch_id, batch_no, plan_production_date
    from pm_production_batch
    where production_year = #{year}
    order by sort_order, batch_no
</select>
```

---

## Task 2：菜单 SQL 变更

**Files:**
- Modify: `pm-sql/init/02_menu_data.sql`
- Create: `pm-sql/fix_menu_task_redesign_20260311.sql`

### Step 1：创建 fix SQL

```sql
-- pm-sql/fix_menu_task_redesign_20260311.sql

-- 1. 将现有「任务管理」菜单名称保留，不变动menu_id=2263

-- 2. 新增「项目分解任务」菜单（挂在项目管理下）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目分解任务',
  (SELECT menu_id FROM sys_menu WHERE menu_name='项目管理' AND parent_id=0),
  9, 'decompose', 'project/subproject/decompose',
  1, 0, 'C', '0', '0', 'project:subproject:add', 'plus', 'admin', NOW(), '', NULL, '项目分解任务');
```

### Step 2：本地 + 远程执行

---

## Task 3：前端 — 新建「项目分解任务」页面

**Files:**
- Create: `ruoyi-ui/src/views/project/subproject/decompose.vue`

### 页面结构

**第一步：选择项目（Card）**
- `ProjectDeptSelect` → 机构选择，change 时清空项目选择并重新查询
- 项目 autoComplete：`el-autocomplete`，`fetchSuggestions` 调用后端搜索接口（`GET /project/project/search?projectDept=xxx&projectName=keyword`），选定后只读展示项目基本信息

**只读展示项目基本信息（排除：收入确认金额、项目阶段、确认日期、项目地址、项目计划、项目描述、审核意见）：**
以 `el-descriptions` 组件只读展示：项目名称、项目编码、行业、区域、项目经理、客户名称、合同金额、预算、建设年度、项目分类、验收状态

**第二步：填写任务信息（Card，项目选定后启用）**
- 投产年度（sys_ndgl 下拉）→ 投产批次（动态下拉，调 `/project/productionBatch/byYear?productionYear=xxx`）→ 计划投产日期（只读 span，批次选定后带出）
- 任务负责人（必填）/ 任务名称（必填）
- 任务编号 / 任务阶段（必填）
- 总行需求号 / 软件中心需求编号
- 二级产品（sys_product 下拉）
- 预估工作量（必填）/ 任务预算
- 启动日期（必填）/ 结束日期（必填）
- 内部闭包日期 / 功能测试版本日期
- 备注

**提交：** `addProject`，提交后跳转 `/project/subproject`

---

## Task 4：前端 — 改造「任务管理」index.vue

**Files:**
- Modify: `ruoyi-ui/src/views/project/subproject/index.vue`

### 查询条件变更

1. 新增：项目部门（`ProjectDeptSelect`，v-model: `queryParams.projectDept`，change 时清空项目名称选择）
2. 「所属主项目」标签改为「项目名称」，`el-select` 改为 `el-autocomplete`，调用 `GET /project/project/search?projectName=keyword`（不带 projectDept，任务管理全局搜索）
3. 新增：任务编号（`el-autocomplete`，调用 `GET /project/project/searchTaskCode?taskCode=keyword`，选中后设置查询参数 `taskCode`）
4. 新增：投产年份（sys_ndgl 下拉）
5. 新增：批次号（下拉，根据投产年份动态加载，调 `/project/productionBatch/byYear`，年份为空时禁用）
6. 新增：父项目收入确认年份（sys_ndgl 下拉）
7. 新增：二级产品（sys_product 下拉）
8. 新增：任务负责人（下拉，同项目经理数据源 `GET /project/project/users?postCode=pm`）
9. 新增：软件中心需求编号（input）

### 行为变更

- **删除 parentId 门槛**：移除现有的 `if (!queryParams.parentId) return` 判断，页面打开时直接查询（无需先选主项目）
- **删除 `sys_tcpc` 导入**：原「投产批次」列用 dict-tag + sys_tcpc 显示，改为直接展示 `batchNo` 字符串

### 列表变更

- 删除「新增任务」按钮（分解任务入口移至新菜单）
- 新增列：投产批次（显示 batchNo）、投产年份

---

## Task 5：前端 — 改造 edit.vue / add.vue / detail.vue

**Files:**
- Modify: `ruoyi-ui/src/views/project/subproject/edit.vue`
- Modify: `ruoyi-ui/src/views/project/subproject/add.vue`（保留，edit 同步改动即可；decompose.vue 是新增主入口，add.vue 作为降级备用）
- Modify: `ruoyi-ui/src/views/project/subproject/detail.vue`

### edit.vue 变更

**基本信息 Card：**
- 「投产批次」从 `dict-select sys_tcpc` → 改为两个联动字段：
  - 投产年度（sys_ndgl 下拉，v-model: form.productionYear）
  - 投产批次（动态下拉，v-model: form.batchId，调 `/project/productionBatch/byYear`，根据年度动态加载）
  - 计划投产日期（只读 span，批次选定后带出）
- 新增：总行需求号（input）
- 新增：软件中心需求编号（input）
- 新增：二级产品（sys_product 下拉）

**表单数据：**
- 删除 `productionBatch`
- 新增 `batchId`, `productionYear`, `bankDemandNo`, `softwareDemandNo`, `product`

### add.vue 变更（同 edit.vue 相同改动）

### detail.vue 变更

- 删除 `productionBatch` 字段展示（及 `sys_tcpc` dict import）
- 新增只读展示：投产年份（`productionYear` dict-tag sys_ndgl）、投产批次（`batchNo` 字符串）、计划投产日期（`planProductionDate`）
- 新增只读展示：总行需求号、软件中心需求编号、二级产品（`product` dict-tag sys_product）

---

## Task 6：编译、重启、验证

```bash
mvn clean package -Dmaven.test.skip=true -pl ruoyi-project,ruoyi-admin -am && ./ry.sh restart
```

验证点：
- [ ] 项目分解任务页面正常打开，选机构→选项目→带出信息→填任务→提交成功
- [ ] 任务管理列表页面打开即有数据（无需先选主项目）
- [ ] 任务管理列表查询条件全部正常，批次下拉随年份联动
- [ ] 任务管理编辑页批次联动正常，保存后 detail.vue 展示 batchNo
- [ ] 已有任务数据不受影响（batch_id / production_year 为 NULL）
