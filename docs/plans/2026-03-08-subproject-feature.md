# 项目分解任务（子项目）实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为复杂项目提供子项目拆分能力，子项目具备主项目全部属性，支持独立菜单管理、成员管理、阶段变更；日报集成作为独立 Phase 2，待本计划完成后再实施。

**Architecture:** `pm_project` 表自引用（新增 `parent_id` / `project_level` / `task_code`），现有主项目列表查询加 `project_level = 0` 过滤，新增 `/project/project/subList` 端点和 `subProjectOptions` 轻量端点，新建"项目分解任务"独立菜单，前端三个新页面（index / add / edit）复用主项目的 Service/Mapper 逻辑，add/edit 在提交前由前端注入 `parentId=xxx` 和 `projectLevel=1`。

**Tech Stack:** Java 17 / Spring Boot 3 / MyBatis XML / Vue 3 Composition API / Element Plus 2 / Pinia

---

## Task 1：数据库 Schema 变更

**Files:**
- Create: `pm-sql/fix_subproject_20260308.sql`
- Modify: `pm-sql/init/00_tables_ddl.sql`（在 `pm_project` 建表语句末尾同步加字段注释）

### Step 1：创建 SQL 文件

```sql
-- pm-sql/fix_subproject_20260308.sql
-- 项目分解任务：pm_project 表新增子项目支持字段

ALTER TABLE pm_project
  ADD COLUMN parent_id     bigint      DEFAULT NULL    COMMENT '父项目ID，NULL表示顶层主项目',
  ADD COLUMN project_level tinyint     NOT NULL DEFAULT 0
                                                       COMMENT '层级: 0=主项目(默认) 1=子项目',
  ADD COLUMN task_code     varchar(50) DEFAULT NULL    COMMENT '子项目编号，如 01、用户系统';

CREATE INDEX idx_pm_project_parent ON pm_project(parent_id);
```

### Step 2：在本地 Docker MySQL 执行

```bash
cat pm-sql/fix_subproject_20260308.sql | docker exec -i 3523a41063b7 \
  mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

预期输出：无报错，`Query OK`。

### Step 3：验证字段已加

```bash
echo "DESCRIBE pm_project;" | docker exec -i 3523a41063b7 \
  mysql -u root -ppassword ry-vue | grep -E "parent_id|project_level|task_code"
```

预期输出：3 行字段描述。

### Step 4：同步 `00_tables_ddl.sql`

在 `pm_project` 建表语句的 `del_flag` 字段之前加入（保持字段顺序一致）：

```sql
  `parent_id`     bigint      DEFAULT NULL    COMMENT '父项目ID，NULL表示顶层主项目',
  `project_level` tinyint     NOT NULL DEFAULT 0 COMMENT '层级: 0=主项目 1=子项目',
  `task_code`     varchar(50) DEFAULT NULL    COMMENT '子项目编号，如 01、用户系统',
```

### Step 5：Commit

```bash
git add pm-sql/fix_subproject_20260308.sql pm-sql/init/00_tables_ddl.sql
git commit -m "feat: pm_project 新增 parent_id/project_level/task_code 字段支持子项目"
```

---

## Task 2：后端 — Project 实体扩展

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/domain/Project.java`

### Step 1：在 `delFlag` 字段声明之前加入新字段

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

### Step 2：在文件末尾（toString 之前）加 getter/setter

```java
public void setParentId(Long parentId)              { this.parentId = parentId; }
public Long getParentId()                           { return parentId; }

public void setProjectLevel(Integer projectLevel)   { this.projectLevel = projectLevel; }
public Integer getProjectLevel()                    { return projectLevel; }

public void setTaskCode(String taskCode)            { this.taskCode = taskCode; }
public String getTaskCode()                         { return taskCode; }

public void setParentProjectName(String parentProjectName) { this.parentProjectName = parentProjectName; }
public String getParentProjectName()               { return parentProjectName; }
```

### Step 3：toString 里追加

```java
.append("parentId", getParentId())
.append("projectLevel", getProjectLevel())
.append("taskCode", getTaskCode())
```

### Step 4：编译验证

```bash
mvn clean compile -pl ruoyi-project -am -q
```

预期：`BUILD SUCCESS`，无编译错误。

### Step 5：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/Project.java
git commit -m "feat: Project 实体新增 parentId/projectLevel/taskCode/parentProjectName 字段"
```

---

## Task 3：后端 — ProjectMapper.xml 改造

**Files:**
- Modify: `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml`

### Step 1：ResultMap 新增字段映射

在 `ProjectResult` resultMap 中，找到 `<result property="delFlag"` 这行，在其**之前**插入：

```xml
<result property="parentId"           column="parent_id" />
<result property="projectLevel"       column="project_level" />
<result property="taskCode"           column="task_code" />
<result property="parentProjectName"  column="parent_project_name" />
```

### Step 2：`selectProjectVo` SQL 片段扩展

找到 `<sql id="selectProjectVo">` 块：

①  SELECT 列末尾（在 `p.del_flag` 前）追加：

```sql
        p.parent_id, p.project_level, p.task_code,
        pp.project_name as parent_project_name,
```

②  JOIN 部分末尾（在 `where` 之前）追加：

```xml
        left join pm_project pp on p.parent_id = pp.project_id
```

### Step 3：`selectProjectList` WHERE 子句加主项目过滤

找到 `<select id="selectProjectList"` 块，在 `where p.del_flag = '0'` 之后的第一个 `<if>` 之前加：

```xml
        and p.project_level = 0
```

> **说明：** 存量数据的 `project_level` 默认为 0，此过滤不影响历史数据。

### Step 4：新增 `selectSubProjectList`

在 `selectProjectList` 结束的 `</select>` 之后追加：

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

### Step 5：新增 `selectSubProjectOptions`（轻量下拉，日报阶段用）

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

### Step 6：验证编译

```bash
mvn clean compile -pl ruoyi-project -am -q
```

预期：`BUILD SUCCESS`。

### Step 7：Commit

```bash
git add ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml
git commit -m "feat: ProjectMapper.xml 新增子项目查询，主项目列表过滤 project_level=0"
```

---

## Task 4：后端 — Mapper 接口、Service 接口与实现

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/IProjectService.java`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java`

### Step 1：ProjectMapper.java 追加接口方法

在文件末尾接口方法列表加：

```java
/**
 * 查询子项目列表
 */
List<Project> selectSubProjectList(Project project);

/**
 * 查询子项目轻量选项（id + name + taskCode）
 */
List<Map<String, Object>> selectSubProjectOptions(@Param("parentId") Long parentId);
```

注意 import：`org.apache.ibatis.annotations.Param` 和 `java.util.Map`。

### Step 2：IProjectService.java 追加接口声明

```java
/**
 * 查询子项目列表（带数据权限）
 */
List<Project> selectSubProjectList(Project project);

/**
 * 查询子项目轻量选项列表
 */
List<Map<String, Object>> selectSubProjectOptions(Long parentId);
```

### Step 3：ProjectServiceImpl.java 追加实现

在 `selectProjectList` 方法附近加（保持 `@DataScope` 注解）：

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
```

### Step 4：验证编译

```bash
mvn clean compile -pl ruoyi-project -am -q
```

预期：`BUILD SUCCESS`。

### Step 5：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java \
        ruoyi-project/src/main/java/com/ruoyi/project/service/IProjectService.java \
        ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java
git commit -m "feat: 新增 selectSubProjectList / selectSubProjectOptions Service 方法"
```

---

## Task 5：后端 — Controller 新增端点

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java`

### Step 1：在 `list` 方法之后插入

```java
/**
 * 查询子项目列表
 */
@PreAuthorize("@ss.hasPermi('project:subproject:list')")
@GetMapping("/subList")
public TableDataInfo subList(Project project) {
    startPage();
    return getDataTable(projectService.selectSubProjectList(project));
}

/**
 * 获取子项目轻量选项（供日报、下拉等场景使用，无需权限）
 */
@GetMapping("/subProjectOptions")
public AjaxResult subProjectOptions(@RequestParam Long parentId) {
    return AjaxResult.success(projectService.selectSubProjectOptions(parentId));
}
```

> **说明：** 子项目的新建（POST）和编辑（PUT）复用现有端点，前端在提交前注入 `parentId` 和 `projectLevel=1`，后端直接 persist，无需额外端点。

### Step 2：打包验证

```bash
mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true -q
```

预期：`BUILD SUCCESS`，`ruoyi-admin/target/ruoyi-admin.jar` 生成。

### Step 3：Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java
git commit -m "feat: ProjectController 新增 /subList 和 /subProjectOptions 端点"
```

---

## Task 6：菜单与权限 SQL

**Files:**
- Create: `pm-sql/fix_subproject_menu_20260308.sql`
- Modify: `pm-sql/init/02_menu_data.sql`（同步追加）

### Step 1：创建菜单 SQL

```sql
-- pm-sql/fix_subproject_menu_20260308.sql
-- 项目分解任务菜单及按钮权限

-- 菜单：项目分解任务（parent_id=2059 为"项目管理"一级菜单）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, remark)
VALUES (2249, '项目分解任务', 2059, 10, 'subproject', 'project/subproject/index',
  1, 0, 'C', '0', '0', 'project:subproject:list', 'list',
  'admin', NOW(), '项目子任务管理');

-- 按钮：查询
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time)
VALUES (2250, '子项目查询', 2249, 1, '', '', 1, 0, 'F', '0', '0',
  'project:subproject:query', '#', 'admin', NOW());

-- 按钮：新增
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time)
VALUES (2251, '子项目新增', 2249, 2, '', '', 1, 0, 'F', '0', '0',
  'project:subproject:add', '#', 'admin', NOW());

-- 按钮：修改
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time)
VALUES (2252, '子项目修改', 2249, 3, '', '', 1, 0, 'F', '0', '0',
  'project:subproject:edit', '#', 'admin', NOW());

-- 按钮：删除
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time)
VALUES (2253, '子项目删除', 2249, 4, '', '', 1, 0, 'F', '0', '0',
  'project:subproject:remove', '#', 'admin', NOW());

-- 为 admin 角色授权（role_id=1）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (1, 2249), (1, 2250), (1, 2251), (1, 2252), (1, 2253);
```

### Step 2：执行到本地 MySQL

```bash
cat pm-sql/fix_subproject_menu_20260308.sql | docker exec -i 3523a41063b7 \
  mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

预期：无报错。

### Step 3：验证菜单已插入

```bash
echo "SELECT menu_id, menu_name, perms FROM sys_menu WHERE parent_id=2249;" \
  | docker exec -i 3523a41063b7 mysql -u root -ppassword ry-vue
```

预期：4 条按钮记录。

### Step 4：同步 `02_menu_data.sql`

将 `fix_subproject_menu_20260308.sql` 全部内容追加到 `02_menu_data.sql` 末尾。

### Step 5：Commit

```bash
git add pm-sql/fix_subproject_menu_20260308.sql pm-sql/init/02_menu_data.sql
git commit -m "feat: 新增项目分解任务菜单及权限 SQL"
```

---

## Task 7：前端 — API 函数扩展

**Files:**
- Modify: `ruoyi-ui/src/api/project/project.js`

### Step 1：在文件末尾追加

```javascript
// ===== 子项目相关 =====

/**
 * 查询子项目列表（分页）
 */
export function listSubProject(query) {
  return request({
    url: '/project/project/subList',
    method: 'get',
    params: query
  })
}

/**
 * 获取子项目轻量选项（供下拉使用）
 */
export function getSubProjectOptions(parentId) {
  return request({
    url: '/project/project/subProjectOptions',
    method: 'get',
    params: { parentId }
  })
}
```

> **说明：** 子项目的新增复用 `addProject`，编辑复用 `updateProject`，详情复用 `getProject`，删除复用 `delProject`。

### Step 2：Commit

```bash
git add ruoyi-ui/src/api/project/project.js
git commit -m "feat: 前端 API 新增 listSubProject / getSubProjectOptions"
```

---

## Task 8：前端 — 子项目列表页

**Files:**
- Create: `ruoyi-ui/src/views/project/subproject/index.vue`

### Step 1：创建页面文件

页面结构：顶部筛选区（主项目选择 + 名称 + 阶段），表格区，操作按钮。

```vue
<template>
  <div class="app-container">
    <!-- 搜索区 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true">
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
          clearable @keyup.enter="handleQuery" style="width: 200px" />
      </el-form-item>

      <el-form-item label="当前阶段" prop="projectStage">
        <el-select v-model="queryParams.projectStage" placeholder="请选择阶段"
          clearable style="width: 160px">
          <el-option v-for="dict in sys_xmjd" :key="dict.value"
            :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" v-hasPermi="['project:subproject:add']"
          @click="handleAdd" :disabled="!queryParams.parentId">新增子项目</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="subprojectList" border>
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column label="子项目编号" prop="taskCode" width="120" />
      <el-table-column label="子项目名称" prop="projectName" min-width="180"
        :show-overflow-tooltip="true" />
      <el-table-column label="当前阶段" prop="projectStage" width="120">
        <template #default="scope">
          <dict-tag :options="sys_xmjd" :value="scope.row.projectStage" />
        </template>
      </el-table-column>
      <el-table-column label="项目经理" prop="projectManagerName" width="100" />
      <el-table-column label="预计人天" prop="estimatedWorkload" width="100" align="right" />
      <el-table-column label="实际人天" prop="actualWorkload" width="100" align="right" />
      <el-table-column label="项目状态" prop="projectStatus" width="100">
        <template #default="scope">
          <dict-tag :options="sys_xmzt" :value="scope.row.projectStatus" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="160" />
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View"
            v-hasPermi="['project:subproject:query']"
            @click="handleDetail(scope.row)">详情</el-button>
          <el-button link type="primary" icon="Edit"
            v-hasPermi="['project:subproject:edit']"
            @click="handleEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" icon="Delete"
            v-hasPermi="['project:subproject:remove']"
            @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total > 0" :total="total"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      @pagination="getList" />
  </div>
</template>

<script setup name="Subproject">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { listSubProject, listProject, delProject } from '@/api/project/project'

const router = useRouter()
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

/** 加载主项目选项（不含子项目） */
async function loadMainProjectOptions() {
  const res = await listProject({ pageNum: 1, pageSize: 999 })
  mainProjectOptions.value = res.rows || []
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

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.$refs['queryRef'].resetFields()
  handleQuery()
}

function handleAdd() {
  router.push({
    path: '/project/subproject/add',
    query: { parentId: queryParams.parentId }
  })
}

function handleEdit(row) {
  router.push({
    path: '/project/subproject/edit',
    query: { projectId: row.projectId }
  })
}

function handleDetail(row) {
  router.push({
    path: '/project/subproject/detail',
    query: { projectId: row.projectId }
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除子项目「${row.projectName}」？`).then(() => {
    return delProject(row.projectId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  })
}

onMounted(() => {
  loadMainProjectOptions()
})
</script>
```

### Step 2：Commit

```bash
git add ruoyi-ui/src/views/project/subproject/index.vue
git commit -m "feat: 新增子项目列表页 subproject/index.vue"
```

---

## Task 9：前端 — 子项目新建页

**Files:**
- Create: `ruoyi-ui/src/views/project/subproject/add.vue`

### Step 1：创建完整页面文件

基于 `apply.vue` 改造，核心差异：顶部父项目提示、`taskCode` 字段、提交注入 `parentId`/`projectLevel=1`、跳转目标为子项目列表。

```vue
<template>
  <div class="app-container">
    <h2 style="margin: 0 0 6px 0; font-weight: bold;">新增子项目</h2>

    <!-- 所属主项目提示 -->
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
                  <el-option v-for="dict in industry" :key="dict.value" :label="dict.label" :value="dict.value" />
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
                  <el-option v-for="customer in customerOptions" :key="customer.customerId" :label="customer.customerSimpleName" :value="customer.customerId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户联系人" prop="customerContactId" data-prop="customerContactId">
                <el-select v-model="form.customerContactId" placeholder="请选择客户联系人" :disabled="!form.customerId" @change="handleContactChange" @blur="validateOnBlur('customerContactId')">
                  <el-option v-for="contact in contactOptions" :key="contact.contactId" :label="contact.contactName" :value="contact.contactId" />
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
                <el-input v-model="form.projectCost" placeholder="请输入项目费用"
                  @input="handleAmountInput('projectCost', $event)" @blur="handleAmountBlur('projectCost')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="费用预算">
                <el-input v-model="form.expenseBudget" placeholder="请输入费用预算"
                  @input="handleAmountInput('expenseBudget', $event)" @blur="handleAmountBlur('expenseBudget')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="成本预算">
                <el-input v-model="form.costBudget" placeholder="请输入成本预算"
                  @input="handleAmountInput('costBudget', $event)" @blur="handleAmountBlur('costBudget')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="人力费用">
                <el-input v-model="form.laborCost" placeholder="请输入人力费用"
                  @input="handleAmountInput('laborCost', $event)" @blur="handleAmountBlur('laborCost')">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="采购成本">
                <el-input v-model="form.purchaseCost" placeholder="请输入采购成本"
                  @input="handleAmountInput('purchaseCost', $event)" @blur="handleAmountBlur('purchaseCost')">
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
  // 继承父项目部门和客户
  if (res.data.projectDept) form.value.projectDept = Number(res.data.projectDept)
  if (res.data.customerId) {
    form.value.customerId = res.data.customerId
    const cRes = await request({ url: '/project/customer/contact/listByCustomer', method: 'get', params: { customerId: res.data.customerId } })
    contactOptions.value = cRes.data || []
  }
}

function getCustomers() {
  request({ url: '/project/customer/list', method: 'get', params: { pageNum: 1, pageSize: 1000 } })
    .then(res => { customerOptions.value = res.rows || [] })
}

function getSecondaryRegions(regionDictValue) {
  if (!regionDictValue) { secondaryRegionOptions.value = []; return }
  request({ url: '/project/secondaryRegion/listByRegion', method: 'get', params: { regionDictValue } })
    .then(res => { secondaryRegionOptions.value = res.data || [] })
}

function handleRegionChange(value) {
  form.value.regionCode = null; form.value.regionId = null
  getSecondaryRegions(value); generateProjectCode()
}

function handleSecondaryRegionChange(regionCode) {
  if (!regionCode) { form.value.regionId = null; generateProjectCode(); return }
  const sel = secondaryRegionOptions.value.find(item => item.regionCode === regionCode)
  if (sel) form.value.regionId = sel.regionId
  generateProjectCode()
}

function handleSalesManagerChange(userId, user) {
  if (!userId) { form.value.salesContact = null; return }
  if (user) { form.value.salesContact = user.phonenumber || ''; nextTick(() => validateOnBlur('salesContact')) }
}

function handleCustomerChange(customerId) {
  form.value.customerContactId = null; customerContactPhone.value = ''
  if (!customerId) { contactOptions.value = []; return }
  request({ url: '/project/customer/contact/listByCustomer', method: 'get', params: { customerId } })
    .then(res => { contactOptions.value = res.data || [] })
}

function handleContactChange(contactId) {
  if (!contactId) { customerContactPhone.value = ''; return }
  const c = contactOptions.value.find(c => c.contactId === contactId)
  if (c) customerContactPhone.value = c.contactPhone || ''
}

async function generateProjectCode() {
  const { industry, region, regionCode, shortName, establishedYear } = form.value
  if (!(industry && region && regionCode && shortName && establishedYear)) return
  const mySeq = ++codeGenSeq
  const baseCode = `${industry}-${region}-${regionCode}-${shortName}-${establishedYear}`
  const res = await checkProjectCode(baseCode)
  if (mySeq !== codeGenSeq) return
  const { exists, existingProject, suggestedCode } = res.data
  if (!exists) { form.value.projectCode = baseCode; return }
  try {
    await ElMessageBox.confirm(
      `该项目编号【${existingProject.projectCode}】已被项目【${existingProject.projectName}】使用，是否继续？`,
      '项目编号重复', { confirmButtonText: '继续', cancelButtonText: '取消', type: 'warning' }
    )
    if (mySeq !== codeGenSeq) return
    form.value.projectCode = suggestedCode
  } catch {
    if (mySeq !== codeGenSeq) return
    form.value.projectCode = ''; form.value.shortName = ''
  }
}

function handleAmountInput(field, value) {
  let c = String(value).replace(/[^\d.]/g, '')
  const d = c.indexOf('.')
  if (d !== -1) c = c.substring(0, d + 1) + c.substring(d + 1).replace(/\./g, '')
  const p = c.split('.')
  if (p.length === 2 && p[1].length > 2) c = p[0] + '.' + p[1].substring(0, 2)
  form.value[field] = c
}

function handleAmountBlur(field) {
  const v = form.value[field]
  if (!v && v !== 0) return
  const s = String(v).replace(/,/g, '')
  const n = parseFloat(s)
  if (isNaN(n)) return
  const p = s.split('.')
  form.value[field] = p[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',') + '.' + (p.length === 2 ? p[1].padEnd(2, '0').substring(0, 2) : '00')
  validateOnBlur(field)
}

function parseAmount(v) {
  if (v === null || v === undefined || v === '') return null
  const n = parseFloat(String(v).replace(/,/g, ''))
  return isNaN(n) ? null : n
}

function submitForm() {
  validateAndScroll(() => {
    const parentId = Number(route.query.parentId)
    const submitData = {
      ...form.value,
      parentId,
      projectLevel: 1,
      projectBudget: parseAmount(form.value.projectBudget),
      projectCost:   parseAmount(form.value.projectCost),
      expenseBudget: parseAmount(form.value.expenseBudget),
      costBudget:    parseAmount(form.value.costBudget),
      laborCost:     parseAmount(form.value.laborCost),
      purchaseCost:  parseAmount(form.value.purchaseCost)
    }
    addProject(submitData).then(() => {
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
function togglePanel(name) {
  const i = activeNames.value.indexOf(name)
  if (i > -1) activeNames.value.splice(i, 1); else activeNames.value.push(name)
}

onMounted(() => { getCustomers(); loadParentProject() })
</script>

<style scoped>
.app-container { padding-bottom: 80px; }
:deep(.el-form-item__label) { pointer-events: none; }
.form-footer {
  position: sticky; bottom: 0; padding: 20px;
  background-color: #fff; border-top: 1px solid #dcdfe6;
  text-align: center; z-index: 10;
}
.form-footer .el-button { min-width: 120px; margin: 0 10px; }
</style>
<style scoped src="@/assets/styles/form-validation.scss"></style>
```

### Step 2：Commit

```bash
git add ruoyi-ui/src/views/project/subproject/add.vue
git commit -m "feat: 新增子项目新建页 subproject/add.vue"
```

---

## Task 10：前端 — 子项目编辑页 & 详情页

**Files:**
- Create: `ruoyi-ui/src/views/project/subproject/edit.vue`
- Create: `ruoyi-ui/src/views/project/subproject/detail.vue`

### Step 1：创建 edit.vue

基于 `project/edit.vue` 改造。差异：顶部父项目提示、`taskCode` 字段、保存/取消后跳回子项目列表。

**与 project/edit.vue 相比的完整差异清单：**

① `<script setup name>` 改为 `SubprojectEdit`

② 顶部 `<el-card class="page-header">` 的标题改为 `编辑子项目`

③ `activeNames` 默认值保持 `['1', '2', '3', '4', '5', '6']`

④ 在 `form` reactive 对象里新增：
```javascript
taskCode: '',
parentId: null,
projectLevel: 1,
```

⑤ 在"一、项目基本信息"折叠面板里，紧接着项目名称字段**之前**插入：
```vue
<el-row :gutter="20">
  <el-col :span="12">
    <el-form-item label="子项目编号" prop="taskCode">
      <el-input v-model="form.taskCode" placeholder="请输入子项目编号，如 01、用户系统" />
    </el-form-item>
  </el-col>
</el-row>
```

⑥ `el-card class="page-header"` 后面、`el-form` 前面插入父项目提示：
```vue
<el-alert v-if="parentProject" type="info" :closable="false" style="margin: 16px 0">
  <template #title>
    所属主项目：{{ parentProject.projectName }}（{{ parentProject.projectCode }}）
  </template>
</el-alert>
```

⑦ 新增 `parentProject` ref 和加载逻辑，加在 `contractInfo` ref 旁边：
```javascript
const parentProject = ref(null)
```

⑧ `loadProjectData()` 方法里，`Object.assign(form.value, data)` 之后加：
```javascript
// 加载父项目信息（用于展示提示）
if (data.parentId) {
  getProject(data.parentId).then(pRes => {
    parentProject.value = pRes.data
  })
}
```

⑨ `submitForm()` 里 `updateProject` 成功回调改为：
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

⑪ `loadProjectData()` 里的 `router.push` 跳转目标从 `/project/project` 改为 `/project/subproject`

### Step 2：创建 detail.vue

基于 `project/detail.vue` 改造，差异极小：

① `<script setup name>` 改为 `SubprojectDetail`

② `h2` 标题改为 `子项目详情`

③ 在"项目基本信息" `el-descriptions` 里，`项目阶段` 字段**之前**插入两个新字段：
```vue
<el-descriptions-item label="所属主项目" :span="3">
  {{ form.parentProjectName || '-' }}
</el-descriptions-item>
<el-descriptions-item label="子项目编号">
  {{ form.taskCode || '-' }}
</el-descriptions-item>
```

④ `form` reactive 对象里新增：
```javascript
taskCode: '',
parentId: null,
parentProjectName: '',
```

⑤ `goBack()` 改为：
```javascript
function goBack() {
  router.push({ path: '/project/subproject', query: { parentId: form.value.parentId } })
}
```

⑥ `loadProjectData()` 里路由参数改从 `route.query.projectId` 读取（detail.vue 原来用 `route.params.projectId`）：
```javascript
const projectId = route.query.projectId
```

### Step 3：Commit

```bash
git add ruoyi-ui/src/views/project/subproject/edit.vue \
        ruoyi-ui/src/views/project/subproject/detail.vue
git commit -m "feat: 新增子项目编辑页和详情页"
```

---

## Task 11：主项目列表加"子项目"操作按钮

**Files:**
- Modify: `ruoyi-ui/src/views/project/project/index.vue`

### Step 1：操作列宽度从 350 改为 380

找到：
```vue
<el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="350"
```
改为 `width="380"`。

### Step 2：在操作列删除按钮之前插入"子项目"按钮

找到：
```vue
<el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:project:remove']">删除</el-button>
```
在其**前面**插入：
```vue
<el-button link type="primary" icon="Grid" @click="handleSubProject(scope.row)" v-hasPermi="['project:subproject:list']">子项目</el-button>
```

### Step 3：添加处理函数

在 `handleDelete` 函数附近追加（`router` 在 index.vue 已有 `useRouter()` 实例，直接使用）：

```javascript
/** 子项目跳转 */
function handleSubProject(row) {
  router.push({ path: '/project/subproject', query: { parentId: row.projectId } })
}
```

### Step 4：Commit

```bash
git add ruoyi-ui/src/views/project/project/index.vue
git commit -m "feat: 主项目列表新增子项目跳转按钮"
```

---

## Task 12：集成验证

### Step 1：启动后端

```bash
java -Xms512m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
```

### Step 2：启动前端

```bash
cd ruoyi-ui && npm run dev
```

### Step 3：逐项验证

| 验证项 | 操作步骤 | 预期结果 |
|--------|----------|----------|
| 主项目列表不含子项目 | 访问"项目管理"列表 | 列表不出现 project_level=1 的记录 |
| 创建子项目 | 点主项目"子项目"→新增 | 子项目保存成功，parent_id 有值 |
| 子项目列表 | 访问"项目分解任务"菜单，选主项目 | 显示该主项目的子项目列表 |
| 子项目编辑 | 点编辑，修改名称保存 | 更新成功，列表刷新 |
| 子项目删除 | 点删除 | 确认后删除，列表刷新 |
| 父项目提示 | 进入子项目新建/编辑页 | 顶部显示父项目名称 |

### Step 4：完成 Commit

```bash
git add -A
git commit -m "feat: 项目分解任务（子项目）功能完整实现"
```

---

## Phase 2（本计划完成后另行实施）：日报集成子项目

Phase 2 另存为独立计划 `docs/plans/2026-03-08-subproject-daily-report.md`，主要改动：

| 改动 | 内容 |
|------|------|
| DB | `pm_daily_report_detail` 加 `sub_project_id`、`work_category` |
| 后端 | `DailyReportDetail.java` 加字段；月览接口返回每个项目是否含子项目 |
| 前端 | `write.vue` 每个项目行按需显示子项目下拉（`getSubProjectOptions`）+ `sys_gzlb` 工作类别下拉 |
| 日报详情 | `activity.vue` 展示子项目名和工作类别 |

---

## 常见问题排查

| 现象 | 原因 | 解决 |
|------|------|------|
| 子项目出现在主项目列表 | `selectProjectList` WHERE 未加 `project_level=0` | 检查 mapper XML Task 3 Step 3 |
| 菜单不可见 | 未为角色授权 | 执行 `sys_role_menu` INSERT 语句 |
| 子项目 parentId 为 null | 前端提交前未注入 | 检查 add.vue 提交函数 Task 9 Step ④ |
| `selectSubProjectList` 无数据权限 | `@DataScope` 注解缺失 | 检查 ServiceImpl Task 4 Step 3 |
| Collation 报错 | 子项目 JOIN 系统表 | JOIN 条件加 `COLLATE utf8mb4_unicode_ci` |
