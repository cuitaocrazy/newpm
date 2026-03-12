# 项目分解任务（子项目）完整设计方案与实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为复杂项目提供子项目拆分能力。子项目具备主项目全部属性，支持独立菜单管理、成员管理、阶段变更、日报填写（Phase 2）。

**Architecture:** `pm_project` 表自引用（`parent_id` / `project_level` / `task_code`），现有列表过滤 `project_level=0`，新增独立菜单及子项目 CRUD 页面，Phase 2 在 `pm_daily_report_detail` 增加 `sub_project_id` / `work_category` 并改造日报填写页。

**Tech Stack:** Java 17 / Spring Boot 3 / MyBatis XML / Vue 3 Composition API / Element Plus 2

---

## 一、设计方案

### 1.1 数据模型

#### Phase 1：`pm_project` 表新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `parent_id` | bigint DEFAULT NULL | 父项目 ID，NULL = 顶层主项目 |
| `project_level` | tinyint NOT NULL DEFAULT 0 | 0=主项目，1=子项目 |
| `task_code` | varchar(50) DEFAULT NULL | 子项目编号，用户手填，如 "01"、"用户系统" |

> 存量数据 `project_level` 默认为 0，与主项目过滤条件一致，无需 UPDATE 补数据。

#### Phase 2：`pm_daily_report_detail` 表新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `sub_project_id` | bigint DEFAULT NULL | 子项目 ID（关联 pm_project.project_level=1 的记录） |
| `work_category` | varchar(50) DEFAULT NULL | 工作任务类别，字典 sys_gzlb |

> `project_id` 仍存**主项目** ID，`sub_project_id` 存子项目 ID，保证主项目维度统计不受影响。

---

### 1.2 架构决策

| 决策点 | 结论 | 原因 |
|--------|------|------|
| 表结构 | 自引用 pm_project | 复用所有字段、Service、Mapper，改动最小 |
| 子项目审批流 | **无审批**，直接有效 | 子项目是主项目内部拆分，不需要走审核流程 |
| 成员管理 | 复用 pm_project_member | project_id 指向子项目 ID 即可，无需新表 |
| 阶段变更记录 | 复用 pm_project_stage_change | 同上，project_id 指向子项目 ID |
| 附件管理 | 复用 pm_attachment | business_type='project', business_id=子项目ID |
| 合同关联 | 子项目**不关联合同** | 合同挂主项目层级，子项目不需要独立合同 |
| 日报写入粒度 | 主项目行 → 选子项目 | 用户先找到主项目，再在该行内选对应子项目 |
| 子项目是否再分解 | **只支持两层**（level 0/1） | YAGNI，当前无三层需求 |

---

### 1.3 API 设计

#### 新增端点

| Method | URL | 说明 | 权限 |
|--------|-----|------|------|
| GET | `/project/project/subList` | 查询子项目列表（分页，带数据权限） | `project:subproject:list` |
| GET | `/project/project/subProjectOptions` | 轻量下拉（仅 id/name/taskCode） | 无权限要求（日报用） |

#### 复用现有端点

| Method | URL | 场景 |
|--------|-----|------|
| POST | `/project/project` | 新建子项目（前端注入 parentId + projectLevel=1） |
| PUT | `/project/project` | 编辑子项目 |
| DELETE | `/project/project/{id}` | 删除子项目 |
| GET | `/project/project/{id}` | 子项目详情/编辑回显 |

---

### 1.4 前端路由设计

子项目相关路由全部挂在菜单 2249 下。隐藏路由（visible='1'）用于 add/edit/detail 页面：

| path | component | 说明 |
|------|-----------|------|
| `subproject` | `project/subproject/index` | 列表（菜单可见） |
| `subproject/add` | `project/subproject/add` | 新建（隐藏路由） |
| `subproject/edit/:projectId` | `project/subproject/edit` | 编辑（隐藏路由） |
| `subproject/detail/:projectId` | `project/subproject/detail` | 详情（隐藏路由） |

路由跳转规则：
- index → add：`router.push({ path: '/project/subproject/add', query: { parentId } })`
- index → edit：`router.push(\`/project/subproject/edit/${row.projectId}\`)`
- index → detail：`router.push(\`/project/subproject/detail/${row.projectId}\`)`
- add/edit/detail → 返回：`router.push({ path: '/project/subproject', query: { parentId } })`

---

### 1.5 Phase 2 日报改造设计

#### write.vue 改造

每个主项目行的交互区，若该项目有子项目，则在"工作内容 + 小时数"之前展示子项目选择行：

```
┌─ 项目：TMS系统  预计人天：20  已花人天：12.5  当前阶段：需求及设计 ─┐
│ [子项目: TMS-用户系统 ▼]  [类别: 需求分析 ▼]  ← 有子项目才显示      │
│ 工作内容: [__________________________]  小时: [4]  [×]              │
│ [+ 添加明细]                                                         │
└──────────────────────────────────────────────────────────────────────┘
```

- 子项目下拉：`getSubProjectOptions(projectId)` 接口，选中后仅展示名称（不另设"回显任务"字段）
- 工作类别下拉：`sys_gzlb` 字典（已建）
- 无子项目时：不展示这两个下拉，保持现有布局
- 月览接口新增 `hasSubProjects: boolean` 字段供前端判断

#### activity.vue 改造

个人模式每条明细增加展示：`[子项目名] · [工作类别]`（若有值），样式同现有工作内容行。

---

### 1.6 关键边界说明

| 场景 | 处理方式 |
|------|---------|
| 删除主项目时，有子项目 | 后端 deleteProject 检查 `parent_id`，有子项目时抛 ServiceException |
| 子项目的 approvalStatus | 固定为 '1'（已通过），不走审批流，insertProject 时由 Service 自动设置 |
| 主项目列表合计行 | 不变，仍只计算 project_level=0 的数据 |
| 日报月览接口性能 | 批量查 `hasSubProjects`，用 `SELECT DISTINCT parent_id FROM pm_project WHERE parent_id IN (...)` |
| 子项目成员 | 与主项目成员管理入口相同（index.vue 里的成员管理按钮），project_id 传子项目 ID |

---

## 二、Phase 1 实施计划：子项目 CRUD

---

### Task 1：数据库 Schema

**Files:**
- Create: `pm-sql/fix_subproject_20260308.sql`
- Modify: `pm-sql/init/00_tables_ddl.sql`

#### Step 1：创建 SQL 文件

```sql
-- pm-sql/fix_subproject_20260308.sql

ALTER TABLE pm_project
  ADD COLUMN parent_id     bigint      DEFAULT NULL    COMMENT '父项目ID，NULL表示顶层主项目',
  ADD COLUMN project_level tinyint     NOT NULL DEFAULT 0 COMMENT '层级: 0=主项目 1=子项目',
  ADD COLUMN task_code     varchar(50) DEFAULT NULL    COMMENT '子项目编号，如 01、用户系统';

CREATE INDEX idx_pm_project_parent ON pm_project(parent_id);
```

#### Step 2：本地执行

```bash
cat pm-sql/fix_subproject_20260308.sql | docker exec -i 3523a41063b7 \
  mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

预期：无报错。

#### Step 3：验证字段

```bash
echo "SHOW COLUMNS FROM pm_project LIKE 'parent_id';" | docker exec -i 3523a41063b7 \
  mysql -u root -ppassword ry-vue
```

预期：返回 1 行字段描述。

#### Step 4：同步 00_tables_ddl.sql

在 `pm_project` 建表语句 `del_flag` 字段**之前**插入：

```sql
  `parent_id`     bigint      DEFAULT NULL    COMMENT '父项目ID，NULL表示顶层主项目',
  `project_level` tinyint     NOT NULL DEFAULT 0 COMMENT '层级: 0=主项目 1=子项目',
  `task_code`     varchar(50) DEFAULT NULL    COMMENT '子项目编号，如 01、用户系统',
```

#### Step 5：Commit

```bash
git add pm-sql/fix_subproject_20260308.sql pm-sql/init/00_tables_ddl.sql
git commit -m "feat: pm_project 新增 parent_id/project_level/task_code 字段"
```

---

### Task 2：Project.java 实体扩展

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/domain/Project.java`

#### Step 1：在 `delFlag` 字段声明之前新增

```java
/** 父项目ID，NULL 表示顶层主项目 */
private Long parentId;

/** 项目层级：0=主项目（默认），1=子项目 */
private Integer projectLevel;

/** 子项目编号（在父项目内的简短标识，如 01、用户系统） */
private String taskCode;

/** 父项目名称（展示用，非 DB 字段，由 Mapper 关联查询填充） */
private String parentProjectName;
```

#### Step 2：在 toString() 之前添加 getter/setter

```java
public void setParentId(Long parentId)              { this.parentId = parentId; }
public Long getParentId()                           { return parentId; }

public void setProjectLevel(Integer projectLevel)   { this.projectLevel = projectLevel; }
public Integer getProjectLevel()                    { return projectLevel; }

public void setTaskCode(String taskCode)            { this.taskCode = taskCode; }
public String getTaskCode()                         { return taskCode; }

public void setParentProjectName(String n)          { this.parentProjectName = n; }
public String getParentProjectName()                { return parentProjectName; }
```

#### Step 3：toString() 追加

```java
.append("parentId", getParentId())
.append("projectLevel", getProjectLevel())
.append("taskCode", getTaskCode())
```

#### Step 4：编译验证

```bash
mvn clean compile -pl ruoyi-project -am -q
```

预期：BUILD SUCCESS。

#### Step 5：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/Project.java
git commit -m "feat: Project 实体新增 parentId/projectLevel/taskCode/parentProjectName"
```

---

### Task 3：ProjectServiceImpl — 子项目审批状态自动设置

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java`

#### Step 1：找到 insertProject 方法，在 projectMapper.insertProject 调用之前插入

```java
// 子项目不走审批流，直接设为"审核通过"
if (project.getProjectLevel() != null && project.getProjectLevel() == 1) {
    project.setApprovalStatus("1");
}
```

#### Step 2：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java
git commit -m "feat: 子项目新建时自动设置审批状态为已通过"
```

---

### Task 4：ProjectServiceImpl — 删除主项目时检查子项目

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java`

#### Step 1：找到 deleteProjectByProjectId 方法，在删除前加检查

```java
@Override
public int deleteProjectByProjectId(Long projectId) {
    // 检查是否有子项目，有则禁止删除
    int subCount = projectMapper.countSubProjects(projectId);
    if (subCount > 0) {
        throw new ServiceException("该项目存在 " + subCount + " 个子项目，请先删除子项目再操作");
    }
    // ... 原有删除逻辑
}
```

#### Step 2：ProjectMapper.java 新增 countSubProjects 接口

```java
/** 统计子项目数量 */
int countSubProjects(@Param("parentId") Long parentId);
```

#### Step 3：ProjectMapper.xml 新增 SQL

```xml
<select id="countSubProjects" resultType="int">
    SELECT COUNT(1) FROM pm_project
    WHERE parent_id = #{parentId} AND del_flag = '0' AND project_level = 1
</select>
```

#### Step 4：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java \
        ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml \
        ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java
git commit -m "feat: 删除主项目时检查子项目，有子项目则阻止删除"
```

---

### Task 5：ProjectMapper.xml — 查询改造

**Files:**
- Modify: `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml`

#### Step 1：ResultMap 新增字段映射

在 `ProjectResult` resultMap 中，`<result property="delFlag"` 之前插入：

```xml
<result property="parentId"           column="parent_id" />
<result property="projectLevel"       column="project_level" />
<result property="taskCode"           column="task_code" />
<result property="parentProjectName"  column="parent_project_name" />
```

#### Step 2：selectProjectVo SQL 片段扩展

① SELECT 列末尾（`p.del_flag` 前）追加：

```sql
        p.parent_id, p.project_level, p.task_code,
        pp.project_name as parent_project_name,
```

② FROM/JOIN 部分，现有 LEFT JOIN 末尾追加：

```xml
        left join pm_project pp on p.parent_id = pp.project_id
```

#### Step 3：selectProjectList WHERE 加主项目过滤

在 `where p.del_flag = '0'` 之后的第一个 `<if>` 之前插入：

```xml
        and p.project_level = 0
```

#### Step 4：新增 selectSubProjectList

```xml
<select id="selectSubProjectList" parameterType="Project" resultMap="ProjectResult">
    <include refid="selectProjectVo"/>
    where p.del_flag = '0'
      and p.parent_id = #{parentId}
    <if test="projectName != null and projectName != ''">
        and p.project_name like concat('%', #{projectName}, '%')
    </if>
    <if test="projectStage != null and projectStage != ''">
        and p.project_stage = #{projectStage}
    </if>
    <if test="projectStatus != null and projectStatus != ''">
        and p.project_status = #{projectStatus}
    </if>
    <if test="projectManagerId != null">
        and p.project_manager_id = #{projectManagerId}
    </if>
    ${params.dataScope}
    order by p.task_code asc, p.create_time desc
</select>
```

#### Step 5：新增 selectSubProjectOptions（轻量下拉）

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
</select>
```

#### Step 6：新增 selectProjectsHasSubProject（批量判断，Phase 2 备用）

```xml
<select id="selectProjectsHasSubProject" resultType="long">
    select distinct parent_id
    from pm_project
    where del_flag = '0'
      and project_level = 1
      and parent_id in
    <foreach collection="projectIds" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

#### Step 7：编译验证

```bash
mvn clean compile -pl ruoyi-project -am -q
```

#### Step 8：Commit

```bash
git add ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml
git commit -m "feat: ProjectMapper.xml 新增子项目查询，主项目列表过滤 project_level=0"
```

---

### Task 6：Mapper接口 + Service接口 + ServiceImpl

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/IProjectService.java`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java`

#### Step 1：ProjectMapper.java 追加接口

```java
List<Project> selectSubProjectList(Project project);
List<Map<String, Object>> selectSubProjectOptions(@Param("parentId") Long parentId);
List<Long> selectProjectsHasSubProject(@Param("projectIds") List<Long> projectIds);
```

（import：`org.apache.ibatis.annotations.Param`，`java.util.Map`，`java.util.List`）

#### Step 2：IProjectService.java 追加声明

```java
List<Project> selectSubProjectList(Project project);
List<Map<String, Object>> selectSubProjectOptions(Long parentId);
List<Long> selectProjectsHasSubProject(List<Long> projectIds);
```

#### Step 3：ProjectServiceImpl.java 追加实现

```java
@Override
@DataScope(deptAlias = "d", userAlias = "u_create")
public List<Project> selectSubProjectList(Project project) {
    return projectMapper.selectSubProjectList(project);
}

@Override
public List<Map<String, Object>> selectSubProjectOptions(Long parentId) {
    return projectMapper.selectSubProjectOptions(parentId);
}

@Override
public List<Long> selectProjectsHasSubProject(List<Long> projectIds) {
    if (projectIds == null || projectIds.isEmpty()) return Collections.emptyList();
    return projectMapper.selectProjectsHasSubProject(projectIds);
}
```

#### Step 4：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java \
        ruoyi-project/src/main/java/com/ruoyi/project/service/IProjectService.java \
        ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java
git commit -m "feat: 新增子项目相关 Service 方法"
```

---

### Task 7：ProjectController 新增端点

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java`

#### Step 1：在 list 方法之后插入

```java
/**
 * 查询子项目列表（分页，带数据权限）
 */
@PreAuthorize("@ss.hasPermi('project:subproject:list')")
@GetMapping("/subList")
public TableDataInfo subList(Project project) {
    startPage();
    return getDataTable(projectService.selectSubProjectList(project));
}

/**
 * 获取子项目轻量选项（无需权限，日报/下拉场景使用）
 */
@GetMapping("/subProjectOptions")
public AjaxResult subProjectOptions(@RequestParam Long parentId) {
    return AjaxResult.success(projectService.selectSubProjectOptions(parentId));
}

/**
 * 批量判断哪些项目有子项目（无需权限，日报场景使用）
 */
@GetMapping("/projectsHasSubProject")
public AjaxResult projectsHasSubProject(@RequestParam List<Long> projectIds) {
    return AjaxResult.success(projectService.selectProjectsHasSubProject(projectIds));
}
```

#### Step 2：打包验证

```bash
mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true -q
```

预期：BUILD SUCCESS，jar 生成。

#### Step 3：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java
git commit -m "feat: 新增 /subList、/subProjectOptions、/projectsHasSubProject 端点"
```

---

### Task 8：菜单与路由 SQL

**Files:**
- Create: `pm-sql/fix_subproject_menu_20260308.sql`
- Modify: `pm-sql/init/02_menu_data.sql`

#### Step 1：创建菜单 SQL

```sql
-- pm-sql/fix_subproject_menu_20260308.sql

-- ① 列表菜单（可见）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2249, '项目分解任务', 2059, 10, 'subproject', 'project/subproject/index',
  1, 0, 'C', '0', '0', 'project:subproject:list', 'list', 'admin', NOW(), '子项目管理');

-- ② 新增页（隐藏路由）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES (2254, '子项目新增页', 2249, 1, 'subproject/add', 'project/subproject/add',
  1, 0, 'C', '1', '0', 'project:subproject:add', '#', 'admin', NOW());

-- ③ 编辑页（隐藏路由）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES (2255, '子项目编辑页', 2249, 2, 'subproject/edit/:projectId', 'project/subproject/edit',
  1, 0, 'C', '1', '0', 'project:subproject:edit', '#', 'admin', NOW());

-- ④ 详情页（隐藏路由）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES (2256, '子项目详情页', 2249, 3, 'subproject/detail/:projectId', 'project/subproject/detail',
  1, 0, 'C', '1', '0', 'project:subproject:query', '#', 'admin', NOW());

-- ⑤ 按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES
  (2250, '子项目查询', 2249, 4, '', '', 1, 0, 'F', '0', '0', 'project:subproject:query', '#', 'admin', NOW()),
  (2251, '子项目新增', 2249, 5, '', '', 1, 0, 'F', '0', '0', 'project:subproject:add',   '#', 'admin', NOW()),
  (2252, '子项目修改', 2249, 6, '', '', 1, 0, 'F', '0', '0', 'project:subproject:edit',  '#', 'admin', NOW()),
  (2253, '子项目删除', 2249, 7, '', '', 1, 0, 'F', '0', '0', 'project:subproject:remove','#', 'admin', NOW());

-- ⑥ admin 角色授权
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (1, 2249), (1, 2250), (1, 2251), (1, 2252), (1, 2253), (1, 2254), (1, 2255), (1, 2256);
```

#### Step 2：执行到本地 MySQL

```bash
cat pm-sql/fix_subproject_menu_20260308.sql | docker exec -i 3523a41063b7 \
  mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

#### Step 3：验证

```bash
echo "SELECT menu_id, menu_name, path, visible FROM sys_menu WHERE parent_id=2249 ORDER BY menu_id;" \
  | docker exec -i 3523a41063b7 mysql -u root -ppassword ry-vue
```

预期：8 条记录（2250~2256 + 2249 本身的子记录）。

#### Step 4：同步 02_menu_data.sql

将 `fix_subproject_menu_20260308.sql` 全部内容追加到 `02_menu_data.sql` 末尾。

#### Step 5：Commit

```bash
git add pm-sql/fix_subproject_menu_20260308.sql pm-sql/init/02_menu_data.sql
git commit -m "feat: 新增项目分解任务菜单及隐藏路由 SQL"
```

---

### Task 9：前端 API 扩展

**Files:**
- Modify: `ruoyi-ui/src/api/project/project.js`

#### Step 1：文件末尾追加

```javascript
// ===== 子项目相关 API =====

/** 查询子项目列表（分页） */
export function listSubProject(query) {
  return request({ url: '/project/project/subList', method: 'get', params: query })
}

/** 获取子项目轻量选项（下拉用） */
export function getSubProjectOptions(parentId) {
  return request({ url: '/project/project/subProjectOptions', method: 'get', params: { parentId } })
}

/** 批量判断哪些主项目有子项目，返回有子项目的 projectId 数组 */
export function getProjectsHasSubProject(projectIds) {
  return request({
    url: '/project/project/projectsHasSubProject',
    method: 'get',
    params: { projectIds: projectIds.join(',') }
  })
}
```

#### Step 2：Commit

```bash
git add ruoyi-ui/src/api/project/project.js
git commit -m "feat: 前端 API 新增子项目相关接口"
```

---

### Task 10：子项目列表页

**Files:**
- Create: `ruoyi-ui/src/views/project/subproject/index.vue`

#### Step 1：创建完整页面

```vue
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="所属主项目" prop="parentId">
        <el-select
          v-model="queryParams.parentId"
          placeholder="请选择主项目"
          filterable
          clearable
          style="width: 260px"
          @change="handleQuery"
        >
          <el-option
            v-for="p in mainProjectOptions"
            :key="p.projectId"
            :label="`${p.projectName}（${p.projectCode}）`"
            :value="p.projectId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="子项目名称" prop="projectName">
        <el-input v-model="queryParams.projectName" placeholder="请输入子项目名称"
          clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="当前阶段" prop="projectStage">
        <el-select v-model="queryParams.projectStage" placeholder="请选择" clearable style="width: 160px">
          <el-option v-for="d in sys_xmjd" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" v-hasPermi="['project:subproject:add']"
          :disabled="!queryParams.parentId" @click="handleAdd">新增子项目</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="subprojectList" border>
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column label="子项目编号" prop="taskCode" width="130" />
      <el-table-column label="子项目名称" prop="projectName" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          <el-link type="primary" @click="handleDetail(scope.row)"
            v-hasPermi="['project:subproject:query']">{{ scope.row.projectName }}</el-link>
          <span v-if="!checkPermi(['project:subproject:query'])">{{ scope.row.projectName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前阶段" prop="projectStage" width="130">
        <template #default="scope">
          <dict-tag :options="sys_xmjd" :value="scope.row.projectStage" />
        </template>
      </el-table-column>
      <el-table-column label="项目经理" prop="projectManagerName" width="100" align="center" />
      <el-table-column label="预计人天" prop="estimatedWorkload" width="100" align="right" />
      <el-table-column label="实际人天" prop="actualWorkload" width="100" align="right">
        <template #default="scope">
          {{ scope.row.actualWorkload != null ? parseFloat(scope.row.actualWorkload).toFixed(3) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="项目状态" prop="projectStatus" width="100" align="center">
        <template #default="scope">
          <dict-tag :options="sys_xmzt" :value="scope.row.projectStatus" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" v-hasPermi="['project:subproject:query']"
            @click="handleDetail(scope.row)">详情</el-button>
          <el-button link type="primary" icon="Edit" v-hasPermi="['project:subproject:edit']"
            @click="handleEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" icon="Delete" v-hasPermi="['project:subproject:remove']"
            @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      @pagination="getList" />
  </div>
</template>

<script setup name="SubprojectIndex">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { listSubProject, listProject, delProject } from '@/api/project/project'
import { checkPermi } from '@/utils/permission'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()
const { sys_xmjd, sys_xmzt } = proxy.useDict('sys_xmjd', 'sys_xmzt')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const subprojectList = ref([])
const mainProjectOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  parentId: null,
  projectName: '',
  projectStage: ''
})

/** 加载主项目下拉选项 */
function loadMainProjectOptions() {
  listProject({ pageNum: 1, pageSize: 999 }).then(res => {
    mainProjectOptions.value = res.rows || []
  })
}

/** 查询子项目列表 */
function getList() {
  if (!queryParams.parentId) {
    subprojectList.value = []
    total.value = 0
    return
  }
  loading.value = true
  listSubProject(queryParams).then(res => {
    subprojectList.value = res.rows
    total.value = res.total
  }).finally(() => { loading.value = false })
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery()  { proxy.$refs['queryRef'].resetFields(); handleQuery() }

function handleAdd() {
  router.push({ path: '/project/subproject/add', query: { parentId: queryParams.parentId } })
}
function handleEdit(row) {
  router.push(`/project/subproject/edit/${row.projectId}`)
}
function handleDetail(row) {
  router.push(`/project/subproject/detail/${row.projectId}`)
}
function handleDelete(row) {
  proxy.$modal.confirm(`确认删除子项目「${row.projectName}」？`).then(() => {
    return delProject(row.projectId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

onMounted(() => {
  loadMainProjectOptions()
  // 若从主项目列表跳转过来，直接带入 parentId
  if (route.query.parentId) {
    queryParams.parentId = Number(route.query.parentId)
    getList()
  }
})
</script>
```

#### Step 2：Commit

```bash
git add ruoyi-ui/src/views/project/subproject/index.vue
git commit -m "feat: 新增子项目列表页 subproject/index.vue"
```

---

### Task 11：子项目新建页

**Files:**
- Create: `ruoyi-ui/src/views/project/subproject/add.vue`

#### Step 1：创建完整页面

```vue
<template>
  <div class="app-container">
    <h2 style="margin: 0 0 6px 0; font-weight: bold;">新增子项目</h2>

    <el-alert v-if="parentProject" type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>
        所属主项目：{{ parentProject.projectName }}（{{ parentProject.projectCode }}）
      </template>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
      <div style="text-align: right; margin-bottom: 10px;">
        <el-link type="primary" @click="expandAll" style="margin-right: 10px;">全部展开</el-link>
        <el-link type="primary" @click="collapseAll">全部折叠</el-link>
      </div>

      <!-- 一、项目基本信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="1">
        <template #header>
          <div @click="togglePanel('1')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('1') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">一、项目基本信息</span>
          </div>
        </template>
        <div v-show="activeNames.includes('1')">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="子项目编号" prop="taskCode">
                <el-input v-model="form.taskCode" placeholder="请输入子项目编号，如 01、用户系统" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="子项目名称" prop="projectName" data-prop="projectName">
                <el-input v-model="form.projectName" placeholder="请输入子项目名称" @blur="validateOnBlur('projectName')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="行业" prop="industry" data-prop="industry">
                <el-select v-model="form.industry" placeholder="请选择行业" @change="generateProjectCode" @blur="validateOnBlur('industry')">
                  <el-option v-for="d in industry" :key="d.value" :label="d.label" :value="d.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="一级区域" prop="region" data-prop="region">
                <dict-select v-model="form.region" dict-type="sys_yjqy" placeholder="请选择一级区域" @change="handleRegionChange" @blur="validateOnBlur('region')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="二级区域" prop="regionCode" data-prop="regionCode">
                <el-select v-model="form.regionCode" placeholder="请选择二级区域" :disabled="!form.region" @change="handleSecondaryRegionChange" @blur="validateOnBlur('regionCode')">
                  <el-option v-for="item in secondaryRegionOptions" :key="item.regionCode" :label="item.regionName" :value="item.regionCode" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="简称" prop="shortName" data-prop="shortName">
                <el-input v-model="form.shortName" placeholder="请输入简称" @input="generateProjectCode" @blur="validateOnBlur('shortName')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="立项年度" prop="establishedYear" data-prop="establishedYear">
                <dict-select v-model="form.establishedYear" dict-type="sys_ndgl" placeholder="请选择立项年度" @change="generateProjectCode" @blur="validateOnBlur('establishedYear')" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目编号" prop="projectCode" data-prop="projectCode">
                <el-input v-model="form.projectCode" placeholder="自动生成" readonly />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目分类" prop="projectCategory" data-prop="projectCategory">
                <dict-select v-model="form.projectCategory" dict-type="sys_xmfl" placeholder="请选择项目分类" @blur="validateOnBlur('projectCategory')" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目部门" prop="projectDept" data-prop="projectDept">
                <project-dept-select v-model="form.projectDept" @blur="validateOnBlur('projectDept')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目状态" prop="projectStatus" data-prop="projectStatus">
                <dict-select v-model="form.projectStatus" dict-type="sys_xmzt" placeholder="请选择项目状态" clearable @blur="validateOnBlur('projectStatus')" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目阶段" prop="projectStage" data-prop="projectStage">
                <dict-select v-model="form.projectStage" dict-type="sys_xmjd" placeholder="请选择项目阶段" @blur="validateOnBlur('projectStage')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="验收状态" prop="acceptanceStatus" data-prop="acceptanceStatus">
                <dict-select v-model="form.acceptanceStatus" dict-type="sys_yszt" placeholder="请选择验收状态" @blur="validateOnBlur('acceptanceStatus')" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预估工作量" prop="estimatedWorkload" data-prop="estimatedWorkload">
                <el-input v-model="form.estimatedWorkload" placeholder="请输入" @blur="validateOnBlur('estimatedWorkload')">
                  <template #append>人天</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目预算" prop="projectBudget" data-prop="projectBudget">
                <el-input v-model="form.projectBudget" placeholder="请输入金额"
                  @input="handleAmountInput('projectBudget', $event)" @blur="handleAmountBlur('projectBudget')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="项目地址" prop="projectAddress" data-prop="projectAddress">
                <el-input v-model="form.projectAddress" placeholder="请输入项目地址" @blur="validateOnBlur('projectAddress')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="项目计划" prop="projectPlan" data-prop="projectPlan">
                <el-input v-model="form.projectPlan" type="textarea" :rows="3" placeholder="请输入项目计划" @blur="validateOnBlur('projectPlan')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="项目描述" prop="projectDescription" data-prop="projectDescription">
                <el-input v-model="form.projectDescription" type="textarea" :rows="3" placeholder="请输入项目描述" @blur="validateOnBlur('projectDescription')" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 二、人员配置 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="3">
        <template #header>
          <div @click="togglePanel('3')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('3') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">二、人员配置</span>
          </div>
        </template>
        <div v-show="activeNames.includes('3')">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目经理" prop="projectManagerId" data-prop="projectManagerId">
                <user-select v-model="form.projectManagerId" post-code="pm" placeholder="请选择项目经理" filterable @blur="validateOnBlur('projectManagerId')" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="市场经理" prop="marketManagerId" data-prop="marketManagerId">
                <user-select v-model="form.marketManagerId" post-code="scjl" placeholder="请选择市场经理" filterable @blur="validateOnBlur('marketManagerId')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="销售负责人" prop="salesManagerId" data-prop="salesManagerId">
                <user-select v-model="form.salesManagerId" post-code="xsfzr" placeholder="请选择销售负责人" filterable @change="handleSalesManagerChange" @blur="validateOnBlur('salesManagerId')" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="销售联系方式" prop="salesContact" data-prop="salesContact">
                <el-input v-model="form.salesContact" placeholder="自动带出" readonly />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="参与人员" prop="participants" data-prop="participants">
                <org-user-select v-model="participantIds" placeholder="请选择参与人员" style="width: 100%" :no-data-scope="true" @blur="validateOnBlur('participants')" />
                <div v-if="participantIds.length > 0" style="margin-top: 6px; color: #606266; font-size: 13px;">已选 {{ participantIds.length }} 人</div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 三、客户信息 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="4">
        <template #header>
          <div @click="togglePanel('4')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('4') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">三、客户信息</span>
          </div>
        </template>
        <div v-show="activeNames.includes('4')">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="客户名称" prop="customerId" data-prop="customerId">
                <el-select v-model="form.customerId" placeholder="请选择客户" filterable @change="handleCustomerChange" @blur="validateOnBlur('customerId')">
                  <el-option v-for="c in customerOptions" :key="c.customerId" :label="c.customerSimpleName" :value="c.customerId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户联系人" prop="customerContactId" data-prop="customerContactId">
                <el-select v-model="form.customerContactId" placeholder="请选择客户联系人" :disabled="!form.customerId" @change="handleContactChange" @blur="validateOnBlur('customerContactId')">
                  <el-option v-for="c in contactOptions" :key="c.contactId" :label="c.contactName" :value="c.contactId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户联系方式">
                <el-input v-model="customerContactPhone" placeholder="自动带出" readonly />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="商户联系人">
                <el-input v-model="form.merchantContact" placeholder="请输入商户联系人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="商户联系方式">
                <el-input v-model="form.merchantPhone" placeholder="请输入商户联系方式" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 四、时间规划 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="5">
        <template #header>
          <div @click="togglePanel('5')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('5') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">四、时间规划</span>
          </div>
        </template>
        <div v-show="activeNames.includes('5')">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="启动日期">
                <el-date-picker v-model="form.startDate" type="date" placeholder="选择启动日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束日期">
                <el-date-picker v-model="form.endDate" type="date" placeholder="选择结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="投产日期">
                <el-date-picker v-model="form.productionDate" type="date" placeholder="选择投产日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="验收日期">
                <el-date-picker v-model="form.acceptanceDate" type="date" placeholder="选择验收日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 五、成本预算 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="6">
        <template #header>
          <div @click="togglePanel('6')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('6') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">五、成本预算</span>
          </div>
        </template>
        <div v-show="activeNames.includes('6')">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目费用">
                <el-input v-model="form.projectCost" placeholder="请输入" @input="handleAmountInput('projectCost', $event)" @blur="handleAmountBlur('projectCost')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="费用预算">
                <el-input v-model="form.expenseBudget" placeholder="请输入" @input="handleAmountInput('expenseBudget', $event)" @blur="handleAmountBlur('expenseBudget')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="成本预算">
                <el-input v-model="form.costBudget" placeholder="请输入" @input="handleAmountInput('costBudget', $event)" @blur="handleAmountBlur('costBudget')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="人力费用">
                <el-input v-model="form.laborCost" placeholder="请输入" @input="handleAmountInput('laborCost', $event)" @blur="handleAmountBlur('laborCost')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="采购成本">
                <el-input v-model="form.purchaseCost" placeholder="请输入" @input="handleAmountInput('purchaseCost', $event)" @blur="handleAmountBlur('purchaseCost')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 六、备注 -->
      <el-card shadow="hover" style="margin-bottom: 15px;" data-panel="7">
        <template #header>
          <div @click="togglePanel('7')" style="cursor: pointer; user-select: none;">
            <i :class="activeNames.includes('7') ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" style="margin-right: 5px;"></i>
            <span style="font-size: 16px; font-weight: bold;">六、备注</span>
          </div>
        </template>
        <div v-show="activeNames.includes('7')">
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </el-form>

    <div class="form-footer">
      <el-button type="primary" size="large" @click="submitForm">保存</el-button>
      <el-button size="large" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script setup name="SubprojectAdd">
import { ref, watch, getCurrentInstance, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { addProject, getProject, checkProjectCode } from '@/api/project/project'
import { ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useFormValidation } from '@/composables/useFormValidation'

let codeGenSeq = 0
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const { industry, sys_xmfl, sys_ndgl, sys_yjqy, sys_xmjd, sys_yszt, sys_xmzt } =
  proxy.useDict('industry', 'sys_xmfl', 'sys_ndgl', 'sys_yjqy', 'sys_xmjd', 'sys_yszt', 'sys_xmzt')

const formRef = ref()
const activeNames = ref(['1', '3', '4', '5', '6', '7'])
const allPanelNames = ['1', '3', '4', '5', '6', '7']
const { validateOnBlur, validateAndScroll } = useFormValidation(formRef, activeNames)
const parentProject = ref(null)

const form = ref({
  taskCode: null, industry: null, region: null, regionCode: null, regionId: null,
  shortName: null, establishedYear: null, projectCode: null, projectName: null,
  projectCategory: null, projectDept: null, projectStatus: null, projectStage: null,
  acceptanceStatus: null, estimatedWorkload: null, projectBudget: null, projectAddress: null,
  projectPlan: null, projectDescription: null, projectManagerId: null, marketManagerId: null,
  participants: null, salesManagerId: null, salesContact: null, customerId: null,
  customerContactId: null, merchantPhone: null, merchantContact: null,
  startDate: null, endDate: null, productionDate: null, acceptanceDate: null,
  projectCost: null, expenseBudget: null, costBudget: null, laborCost: null,
  purchaseCost: null, remark: null
})

const rules = ref({
  projectName:       [{ required: true, message: '子项目名称不能为空', trigger: 'blur' }],
  industry:          [{ required: true, message: '行业不能为空', trigger: 'change' }],
  region:            [{ required: true, message: '一级区域不能为空', trigger: 'change' }],
  regionCode:        [{ required: true, message: '二级区域不能为空', trigger: 'change' }],
  shortName:         [{ required: true, message: '简称不能为空', trigger: 'blur' }],
  establishedYear:   [{ required: true, message: '立项年度不能为空', trigger: 'change' }],
  projectCategory:   [{ required: true, message: '项目分类不能为空', trigger: 'change' }],
  projectDept:       [{ required: true, message: '项目部门不能为空', trigger: 'change' }],
  projectStatus:     [{ required: true, message: '项目状态不能为空', trigger: 'change' }],
  projectStage:      [{ required: true, message: '项目阶段不能为空', trigger: 'change' }],
  acceptanceStatus:  [{ required: true, message: '验收状态不能为空', trigger: 'change' }],
  estimatedWorkload: [{ required: true, message: '预估工作量不能为空', trigger: 'blur' }],
  projectBudget:     [{ required: true, message: '项目预算不能为空', trigger: 'blur' }],
  projectAddress:    [{ required: true, message: '项目地址不能为空', trigger: 'blur' }],
  projectPlan:       [{ required: true, message: '项目计划不能为空', trigger: 'blur' }],
  projectDescription:[{ required: true, message: '项目描述不能为空', trigger: 'blur' }],
  projectManagerId:  [{ required: true, message: '项目经理不能为空', trigger: 'change' }],
  marketManagerId:   [{ required: true, message: '市场经理不能为空', trigger: 'change' }],
  salesManagerId:    [{ required: true, message: '销售负责人不能为空', trigger: 'change' }],
  customerId:        [{ required: true, message: '客户名称不能为空', trigger: 'change' }],
  customerContactId: [{ required: true, message: '客户联系人不能为空', trigger: 'change' }]
})

const secondaryRegionOptions = ref([])
const customerOptions = ref([])
const contactOptions = ref([])
const participantIds = ref([])
const customerContactPhone = ref('')

watch(participantIds, (val) => { form.value.participants = val.join(',') })

async function loadParentProject() {
  const parentId = route.query.parentId
  if (!parentId) return
  const res = await getProject(parentId)
  parentProject.value = res.data
  if (res.data.projectDept) form.value.projectDept = Number(res.data.projectDept)
  if (res.data.customerId) {
    form.value.customerId = res.data.customerId
    const cRes = await request({ url: '/project/customer/contact/listByCustomer', method: 'get', params: { customerId: res.data.customerId } })
    contactOptions.value = cRes.data || []
  }
}

function getCustomers() {
  request({ url: '/project/customer/list', method: 'get', params: { pageNum: 1, pageSize: 1000 } })
    .then(r => { customerOptions.value = r.rows || [] })
}
function getSecondaryRegions(v) {
  if (!v) { secondaryRegionOptions.value = []; return }
  request({ url: '/project/secondaryRegion/listByRegion', method: 'get', params: { regionDictValue: v } })
    .then(r => { secondaryRegionOptions.value = r.data || [] })
}
function handleRegionChange(v) { form.value.regionCode = null; form.value.regionId = null; getSecondaryRegions(v); generateProjectCode() }
function handleSecondaryRegionChange(v) {
  if (!v) { form.value.regionId = null; generateProjectCode(); return }
  const s = secondaryRegionOptions.value.find(i => i.regionCode === v)
  if (s) form.value.regionId = s.regionId
  generateProjectCode()
}
function handleSalesManagerChange(userId, user) {
  if (!userId) { form.value.salesContact = null; return }
  if (user) { form.value.salesContact = user.phonenumber || ''; nextTick(() => validateOnBlur('salesContact')) }
}
function handleCustomerChange(v) {
  form.value.customerContactId = null; customerContactPhone.value = ''
  if (!v) { contactOptions.value = []; return }
  request({ url: '/project/customer/contact/listByCustomer', method: 'get', params: { customerId: v } })
    .then(r => { contactOptions.value = r.data || [] })
}
function handleContactChange(v) {
  const c = contactOptions.value.find(c => c.contactId === v)
  customerContactPhone.value = c ? (c.contactPhone || '') : ''
}

async function generateProjectCode() {
  const { industry, region, regionCode, shortName, establishedYear } = form.value
  if (!(industry && region && regionCode && shortName && establishedYear)) return
  const mySeq = ++codeGenSeq
  const base = `${industry}-${region}-${regionCode}-${shortName}-${establishedYear}`
  const res = await checkProjectCode(base)
  if (mySeq !== codeGenSeq) return
  const { exists, existingProject, suggestedCode } = res.data
  if (!exists) { form.value.projectCode = base; return }
  try {
    await ElMessageBox.confirm(`编号【${existingProject.projectCode}】已被【${existingProject.projectName}】使用，是否继续？`, '编号重复', { confirmButtonText: '继续', cancelButtonText: '取消', type: 'warning' })
    if (mySeq !== codeGenSeq) return
    form.value.projectCode = suggestedCode
  } catch { if (mySeq !== codeGenSeq) return; form.value.projectCode = ''; form.value.shortName = '' }
}

function handleAmountInput(f, v) {
  let c = String(v).replace(/[^\d.]/g, '')
  const d = c.indexOf('.')
  if (d !== -1) c = c.substring(0, d + 1) + c.substring(d + 1).replace(/\./g, '')
  const p = c.split('.')
  if (p.length === 2 && p[1].length > 2) c = p[0] + '.' + p[1].substring(0, 2)
  form.value[f] = c
}
function handleAmountBlur(f) {
  const v = form.value[f]
  if (!v && v !== 0) return
  const s = String(v).replace(/,/g, ''); const n = parseFloat(s)
  if (isNaN(n)) return
  const p = s.split('.')
  form.value[f] = p[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',') + '.' + (p.length === 2 ? p[1].padEnd(2,'0').substring(0,2) : '00')
  validateOnBlur(f)
}
function parseAmount(v) {
  if (v == null || v === '') return null
  const n = parseFloat(String(v).replace(/,/g, ''))
  return isNaN(n) ? null : n
}

function submitForm() {
  validateAndScroll(() => {
    const parentId = Number(route.query.parentId)
    addProject({
      ...form.value, parentId, projectLevel: 1,
      projectBudget: parseAmount(form.value.projectBudget), projectCost: parseAmount(form.value.projectCost),
      expenseBudget: parseAmount(form.value.expenseBudget), costBudget: parseAmount(form.value.costBudget),
      laborCost: parseAmount(form.value.laborCost), purchaseCost: parseAmount(form.value.purchaseCost)
    }).then(() => {
      proxy.$modal.msgSuccess('新增成功')
      router.push({ path: '/project/subproject', query: { parentId } })
    })
  })
}
function cancel() {
  proxy.$modal.confirm('确认取消？未保存的数据将丢失').then(() => {
    router.push({ path: '/project/subproject', query: { parentId: route.query.parentId } })
  })
}
function expandAll()  { activeNames.value = [...allPanelNames] }
function collapseAll() { activeNames.value = [] }
function togglePanel(n) { const i = activeNames.value.indexOf(n); if (i > -1) activeNames.value.splice(i,1); else activeNames.value.push(n) }

onMounted(() => { getCustomers(); loadParentProject() })
</script>

<style scoped>
.app-container { padding-bottom: 80px; }
:deep(.el-form-item__label) { pointer-events: none; }
.form-footer { position: sticky; bottom: 0; padding: 20px; background-color: #fff; border-top: 1px solid #dcdfe6; text-align: center; z-index: 10; }
.form-footer .el-button { min-width: 120px; margin: 0 10px; }
</style>
<style scoped src="@/assets/styles/form-validation.scss"></style>
```

#### Step 2：Commit

```bash
git add ruoyi-ui/src/views/project/subproject/add.vue
git commit -m "feat: 新增子项目新建页 subproject/add.vue"
```

---

### Task 12：子项目编辑页

**Files:**
- Create: `ruoyi-ui/src/views/project/subproject/edit.vue`

#### Step 1：以 `project/edit.vue` 为基础，应用以下 10 处改动

① `<script setup name>` → `SubprojectEdit`

② `<h2>` 标题 → `编辑子项目`

③ `form` reactive 对象新增字段：
```javascript
taskCode: '',
parentId: null,
projectLevel: 1,
```

④ `el-alert`（已拒绝提示）之后，`el-form` 之前，插入父项目提示：
```vue
<el-alert v-if="parentProject" type="info" :closable="false" style="margin: 16px 0">
  <template #title>
    所属主项目：{{ parentProject.projectName }}（{{ parentProject.projectCode }}）
  </template>
</el-alert>
```

⑤ 新增 ref：
```javascript
const parentProject = ref(null)
```

⑥ 在"一、项目基本信息"面板，项目名称字段之前插入：
```vue
<el-row :gutter="20">
  <el-col :span="12">
    <el-form-item label="子项目编号" prop="taskCode">
      <el-input v-model="form.taskCode" placeholder="请输入子项目编号" />
    </el-form-item>
  </el-col>
</el-row>
```

⑦ `loadProjectData` 方法里，`Object.assign(form.value, data)` 之后插入：
```javascript
// 加载父项目信息
if (data.parentId) {
  getProject(data.parentId).then(pRes => { parentProject.value = pRes.data })
}
```

⑧ `loadProjectData` 里获取 projectId 从 `route.params.projectId`（路由参数），不是 `route.query`

⑨ `submitForm` 成功回调改为：
```javascript
proxy.$modal.msgSuccess('保存成功')
router.push({ path: '/project/subproject', query: { parentId: form.value.parentId } })
```

⑩ `cancel()` 改为：
```javascript
function cancel() {
  proxy.$modal.confirm('确认取消编辑？未保存的数据将丢失').then(() => {
    router.push({ path: '/project/subproject', query: { parentId: form.value.parentId } })
  })
}
```

同时将 `loadProjectData` 末尾错误处理里的 `router.push` 目标改为 `/project/subproject`。

#### Step 2：Commit

```bash
git add ruoyi-ui/src/views/project/subproject/edit.vue
git commit -m "feat: 新增子项目编辑页 subproject/edit.vue"
```

---

### Task 13：子项目详情页

**Files:**
- Create: `ruoyi-ui/src/views/project/subproject/detail.vue`

#### Step 1：以 `project/detail.vue` 为基础，应用以下改动

① `<script setup name>` → `SubprojectDetail`

② `<h2>` 标题 → `子项目详情`

③ `form` reactive 对象新增字段：
```javascript
taskCode: '',
parentId: null,
parentProjectName: '',
```

④ "项目基本信息" `el-descriptions` 里，`项目阶段`字段之前插入：
```vue
<el-descriptions-item label="所属主项目" :span="3">
  {{ form.parentProjectName || '-' }}
</el-descriptions-item>
<el-descriptions-item label="子项目编号">
  {{ form.taskCode || '-' }}
</el-descriptions-item>
```

⑤ `loadProjectData` 里获取 projectId 改为：
```javascript
const projectId = route.params.projectId
```

⑥ `goBack()` 改为：
```javascript
function goBack() {
  router.push({ path: '/project/subproject', query: { parentId: form.value.parentId } })
}
```

⑦ 移除合同相关卡片（`contractInfo`、付款里程碑）：子项目不关联合同，删除这两个 `el-card`

⑧ 移除 `getContractByProjectId` import 和相关逻辑

#### Step 2：Commit

```bash
git add ruoyi-ui/src/views/project/subproject/detail.vue
git commit -m "feat: 新增子项目详情页 subproject/detail.vue"
```

---

### Task 14：主项目列表添加"子项目"按钮

**Files:**
- Modify: `ruoyi-ui/src/views/project/project/index.vue`

#### Step 1：操作列宽度 350 → 380

#### Step 2：在 `删除` 按钮之前插入

```vue
<el-button link type="primary" icon="Grid"
  v-hasPermi="['project:subproject:list']"
  @click="handleSubProject(scope.row)">子项目</el-button>
```

#### Step 3：追加处理函数（router 在 index.vue 已存在）

```javascript
function handleSubProject(row) {
  router.push({ path: '/project/subproject', query: { parentId: row.projectId } })
}
```

#### Step 4：Commit

```bash
git add ruoyi-ui/src/views/project/project/index.vue
git commit -m "feat: 主项目列表新增子项目跳转按钮"
```

---

### Task 15：Phase 1 集成验证

#### Step 1：启动后端

```bash
java -Xms512m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
```

#### Step 2：启动前端

```bash
cd ruoyi-ui && npm run dev
```

#### Step 3：验证清单

| # | 验证项 | 操作 | 预期 |
|---|--------|------|------|
| 1 | 主项目列表不含子项目 | 访问"项目管理"列表 | 无 project_level=1 的记录 |
| 2 | 主项目操作列有"子项目"按钮 | 查看操作列 | 按钮存在 |
| 3 | 点"子项目"跳转 | 点击任意主项目"子项目"按钮 | 跳转到子项目列表，parentId 带入 |
| 4 | 新增子项目 | 点"新增子项目"，填写表单保存 | 保存成功，审批状态=已通过，列表出现该子项目 |
| 5 | 子项目编辑 | 点"编辑"，修改名称保存 | 更新成功 |
| 6 | 子项目详情 | 点子项目名称 | 显示详情，包含"所属主项目"字段 |
| 7 | 删除子项目 | 点"删除" | 确认后删除 |
| 8 | 删除有子项目的主项目 | 在主项目列表删除一个有子项目的主项目 | 报错"请先删除子项目" |
| 9 | 子项目继承父项目部门和客户 | 新增子项目时查看部门/客户字段 | 已自动填入父项目数据 |

---

## 三、Phase 2 实施计划：日报集成子项目

> **前提：Phase 1 完全完成并验证通过后再执行 Phase 2**

---

### Task 16：日报数据库变更

**Files:**
- Create: `pm-sql/fix_subproject_dailyreport_20260308.sql`
- Modify: `pm-sql/init/00_tables_ddl.sql`

#### Step 1：创建 SQL

```sql
-- pm-sql/fix_subproject_dailyreport_20260308.sql

ALTER TABLE pm_daily_report_detail
  ADD COLUMN sub_project_id bigint      DEFAULT NULL COMMENT '子项目ID（project_level=1）',
  ADD COLUMN work_category  varchar(50) DEFAULT NULL COMMENT '工作任务类别 sys_gzlb';
```

#### Step 2：执行并验证

```bash
cat pm-sql/fix_subproject_dailyreport_20260308.sql | docker exec -i 3523a41063b7 \
  mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue

echo "SHOW COLUMNS FROM pm_daily_report_detail LIKE 'sub_project_id';" \
  | docker exec -i 3523a41063b7 mysql -u root -ppassword ry-vue
```

#### Step 3：同步 `00_tables_ddl.sql`

在 `pm_daily_report_detail` 建表语句的 `del_flag` 之前插入两个字段。

#### Step 4：Commit

```bash
git add pm-sql/fix_subproject_dailyreport_20260308.sql pm-sql/init/00_tables_ddl.sql
git commit -m "feat: pm_daily_report_detail 新增 sub_project_id/work_category 字段"
```

---

### Task 17：日报后端实体与 Mapper

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReportDetail.java`
- Modify: `ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml`

#### Step 1：DailyReportDetail.java 新增字段

```java
/** 子项目ID */
private Long subProjectId;

/** 工作任务类别（字典 sys_gzlb） */
private String workCategory;

/** 子项目名称（展示用，非DB字段） */
private String subProjectName;
```

加 getter/setter。

#### Step 2：DailyReportDetailMapper.xml 改造

① ResultMap 新增：
```xml
<result property="subProjectId"   column="sub_project_id" />
<result property="workCategory"   column="work_category" />
<result property="subProjectName" column="sub_project_name" />
```

② INSERT SQL 加上 `sub_project_id, work_category`（同步 values 参数）

③ SELECT SQL 加上 `d.sub_project_id, d.work_category` 和 LEFT JOIN：
```sql
left join pm_project sp on d.sub_project_id = sp.project_id and sp.del_flag = '0'
```
SELECT 里加 `sp.project_name as sub_project_name`

#### Step 3：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReportDetail.java \
        ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml
git commit -m "feat: DailyReportDetail 新增 subProjectId/workCategory 字段"
```

---

### Task 18：日报月览接口扩展

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportController.java`（或 Service）

#### Step 1：月览接口 `listDailyReport` 返回中，每条记录增加 `hasSubProjects` 字段

在 Service 的 `listDailyReportByMonth` 方法（或 Controller 的响应组装处），获取返回的 projectId 列表后，批量查询哪些有子项目：

```java
// 获取本月日报涉及的所有 projectId
List<Long> projectIds = list.stream()
    .map(DailyReport::getProjectId)
    .distinct()
    .collect(Collectors.toList());

// 批量查询有子项目的 projectId
List<Long> hasSubProjectIds = projectService.selectProjectsHasSubProject(projectIds);
Set<Long> hasSubSet = new HashSet<>(hasSubProjectIds);

// 注入 hasSubProjects 字段（需在 DailyReport 实体加该非 DB 字段）
list.forEach(r -> r.setHasSubProjects(hasSubSet.contains(r.getProjectId())));
```

> `DailyReport.java` 需新增 `private Boolean hasSubProjects;` 及 getter/setter。

#### Step 2：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReport.java \
        ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportController.java
git commit -m "feat: 日报月览接口增加 hasSubProjects 字段"
```

---

### Task 19：write.vue 改造

**Files:**
- Modify: `ruoyi-ui/src/views/project/dailyReport/write.vue`

#### Step 1：导入新 API

```javascript
import { getSubProjectOptions } from '@/api/project/project'
```

#### Step 2：项目数据结构扩展

每个项目对象加：
```javascript
{
  // 原有字段
  projectId, projectName, ...
  // 新增
  hasSubProjects: false,        // 来自月览接口
  subProjectOptions: [],        // 懒加载，选子项目时才填充
}
```

#### Step 3：每个明细行数据结构扩展

```javascript
{
  projectId, workHours, workContent,
  subProjectId: null,    // 新增
  workCategory: null,    // 新增
}
```

#### Step 4：模板改造（每个项目的明细行区域）

在工作内容 input 之前，若 `project.hasSubProjects === true`，展示：

```vue
<template v-if="project.hasSubProjects">
  <el-select
    v-model="detail.subProjectId"
    placeholder="选择子项目（可选）"
    clearable
    size="small"
    style="width: 180px; margin-right: 8px"
    @focus="loadSubProjectOptions(project)"
  >
    <el-option
      v-for="sp in project.subProjectOptions"
      :key="sp.projectId"
      :label="sp.taskCode ? `${sp.taskCode}-${sp.projectName}` : sp.projectName"
      :value="sp.projectId"
    />
  </el-select>
  <el-select
    v-model="detail.workCategory"
    placeholder="工作类别（可选）"
    clearable
    size="small"
    style="width: 140px; margin-right: 8px"
  >
    <el-option v-for="d in sys_gzlb" :key="d.value" :label="d.label" :value="d.value" />
  </el-select>
</template>
```

#### Step 5：懒加载子项目选项

```javascript
async function loadSubProjectOptions(project) {
  if (project.subProjectOptions.length > 0) return  // 已加载过，跳过
  const res = await getSubProjectOptions(project.projectId)
  project.subProjectOptions = res.data || []
}
```

#### Step 6：字典注册

在 `useDict(...)` 调用里增加 `'sys_gzlb'`。

#### Step 7：月览数据组装时同步 hasSubProjects

```javascript
// 月览接口返回数据后，将 hasSubProjects 字段同步到 myProjects 列表对应项目
function syncHasSubProjects(monthReports) {
  const map = {}
  monthReports.forEach(r => { if (r.projectId) map[r.projectId] = r.hasSubProjects || false })
  myProjects.value.forEach(p => { p.hasSubProjects = map[p.projectId] || false })
}
```

#### Step 8：保存时 detailList 包含新字段

`POST /project/dailyReport` 的 detailList 里每条明细直接带 `subProjectId` 和 `workCategory`，后端实体已新增，直接入库。

#### Step 9：Commit

```bash
git add ruoyi-ui/src/views/project/dailyReport/write.vue
git commit -m "feat: 日报填写页支持子项目选择和工作类别"
```

---

### Task 20：activity.vue 改造

**Files:**
- Modify: `ruoyi-ui/src/views/project/dailyReport/activity.vue`

#### Step 1：个人模式/团队模式中，每条工作明细展示子项目和工作类别

在现有 `工作内容 · Xh` 展示行旁边增加：

```vue
<span v-if="detail.subProjectName" class="sub-project-tag">
  [{{ detail.subProjectName }}]
</span>
<span v-if="detail.workCategory" class="work-category-tag">
  <dict-tag :options="sys_gzlb" :value="detail.workCategory" />
</span>
```

#### Step 2：字典注册

在 `useDict(...)` 里增加 `'sys_gzlb'`。

#### Step 3：Commit

```bash
git add ruoyi-ui/src/views/project/dailyReport/activity.vue
git commit -m "feat: 日报动态页展示子项目名称和工作类别"
```

---

### Task 21：Phase 2 集成验证

| # | 验证项 | 操作 | 预期 |
|---|--------|------|------|
| 1 | 有子项目的项目显示子项目下拉 | 打开日报填写，找到有子项目的项目行 | 显示"选择子项目"下拉 |
| 2 | 无子项目的项目不显示下拉 | 找到无子项目的项目行 | 不显示子项目下拉 |
| 3 | 子项目下拉选项正确 | 展开子项目下拉 | 显示该主项目的子项目列表 |
| 4 | 工作类别下拉正确 | 展开工作类别下拉 | 显示 sys_gzlb 字典值 |
| 5 | 保存日报含子项目信息 | 选子项目和类别后保存 | 入库后 sub_project_id / work_category 有值 |
| 6 | 查看日报动态显示子项目名 | 在 activity.vue 查看当天日报 | 展示子项目名称和工作类别 |

---

## 四、完整文件清单

### Phase 1 涉及文件

| 类型 | 文件 |
|------|------|
| SQL | `pm-sql/fix_subproject_20260308.sql` |
| SQL | `pm-sql/fix_subproject_menu_20260308.sql` |
| SQL | `pm-sql/init/00_tables_ddl.sql`（同步） |
| SQL | `pm-sql/init/02_menu_data.sql`（同步） |
| Java | `ruoyi-project/.../domain/Project.java` |
| Java | `ruoyi-project/.../mapper/ProjectMapper.java` |
| Java | `ruoyi-project/.../service/IProjectService.java` |
| Java | `ruoyi-project/.../service/impl/ProjectServiceImpl.java` |
| Java | `ruoyi-project/.../controller/ProjectController.java` |
| XML | `ruoyi-project/.../mapper/project/ProjectMapper.xml` |
| Vue | `ruoyi-ui/src/api/project/project.js` |
| Vue | `ruoyi-ui/src/views/project/subproject/index.vue`（新建） |
| Vue | `ruoyi-ui/src/views/project/subproject/add.vue`（新建） |
| Vue | `ruoyi-ui/src/views/project/subproject/edit.vue`（新建） |
| Vue | `ruoyi-ui/src/views/project/subproject/detail.vue`（新建） |
| Vue | `ruoyi-ui/src/views/project/project/index.vue` |

### Phase 2 涉及文件

| 类型 | 文件 |
|------|------|
| SQL | `pm-sql/fix_subproject_dailyreport_20260308.sql` |
| SQL | `pm-sql/init/00_tables_ddl.sql`（同步） |
| Java | `ruoyi-project/.../domain/DailyReport.java` |
| Java | `ruoyi-project/.../domain/DailyReportDetail.java` |
| Java | `ruoyi-project/.../controller/DailyReportController.java` |
| XML | `ruoyi-project/.../mapper/project/DailyReportDetailMapper.xml` |
| Vue | `ruoyi-ui/src/views/project/dailyReport/write.vue` |
| Vue | `ruoyi-ui/src/views/project/dailyReport/activity.vue` |

---

## 五、常见问题排查

| 现象 | 原因 | 解决 |
|------|------|------|
| 子项目出现在主项目列表 | `selectProjectList` 未加 `project_level=0` 过滤 | 检查 Task 5 Step 3 |
| 新增子项目审批状态为"待审核" | `insertProject` 未判断子项目设置 approvalStatus | 检查 Task 3 |
| 删除主项目没有报错提示 | `deleteProjectByProjectId` 未加检查 | 检查 Task 4 |
| 菜单不可见 | `sys_role_menu` 未授权 | 重新执行菜单 SQL 的 INSERT |
| 子项目详情页报错"缺少项目ID" | `route.params.projectId` 取不到 | 确认路由配置为 `subproject/detail/:projectId` |
| 日报写入页有子项目但下拉不显示 | `hasSubProjects` 字段未从月览接口返回 | 检查 Task 18 |
| Collation 报错 | 子项目 JOIN sys_* 系统表 | JOIN 条件加 `COLLATE utf8mb4_unicode_ci` |
