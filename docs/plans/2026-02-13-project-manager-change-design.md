# 项目经理变更功能 - 详细设计文档

**创建日期**: 2026-02-13
**作者**: Claude + 用户协作
**状态**: 待实施

---

## 1. 功能概述

### 1.1 业务目标

实现项目经理管理功能，允许管理员查看所有项目的项目经理信息，并执行单个或批量变更操作。系统自动记录所有变更历史，确保审计追溯性。

### 1.2 核心功能

- **项目列表管理**: 显示所有项目及其当前项目经理信息
- **变更操作**: 支持单个项目和批量项目的经理变更
- **历史记录**: 自动记录所有变更历史，支持查看详情
- **查询过滤**: 按项目名称、项目经理进行筛选

### 1.3 关键设计决策

| 决策点 | 选择方案 | 理由 |
|--------|---------|------|
| 数据展示模式 | 以项目为主，关联最新变更 | 管理视角，能看到所有可变更项目 |
| 变更生效方式 | 立即生效（事务保证） | 直接高效，无需审批流程 |
| 项目经理识别 | 岗位过滤（post_code='pm'） | 利用现有接口，精准过滤 |
| 项目搜索策略 | 后端远程搜索（≥2字符） | 平衡性能和体验，适合中等数据量 |

---

## 2. 数据模型

### 2.1 主要涉及的表

**`pm_project`** - 项目主表
```sql
- project_id (主键)
- project_name (项目名称)
- project_code (项目编号)
- project_manager_id (当前项目经理ID)
- create_time (立项时间)
- ...
```

**`pm_project_manager_change`** - 项目经理变更记录表
```sql
- change_id (主键)
- project_id (项目ID, FK)
- old_manager_id (原项目经理ID)
- new_manager_id (新项目经理ID)
- change_reason (变更原因)
- create_by (变更人)
- create_time (变更时间)
```

**`sys_user`** - 用户表
```sql
- user_id (主键)
- nick_name (用户昵称)
- user_name (用户名)
```

**`sys_user_post`** - 用户岗位关联表
```sql
- user_id (用户ID)
- post_id (岗位ID)
```

**`sys_post`** - 岗位表
```sql
- post_id (主键)
- post_code (岗位编码, 'pm' = 项目经理)
- post_name (岗位名称)
```

### 2.2 数据流

```
变更操作流程：
1. 用户选择项目 + 新经理 + 输入原因
   ↓
2. Controller 接收请求
   ↓
3. Service 开启事务
   ↓
4. 查询项目当前经理（old_manager_id）
   ↓
5. 更新 pm_project.project_manager_id
   ↓
6. 插入 pm_project_manager_change 记录
   ↓
7. 提交事务（成功）或回滚（失败）
   ↓
8. 返回结果给前端
```

---

## 3. 后端设计

### 3.1 核心接口列表

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 项目搜索 | GET | `/project/projectManagerChange/searchProjects` | 项目名称自动补全搜索 |
| 项目经理列表 | GET | `/system/user/listByPost` | 获取岗位为'pm'的用户列表（已存在） |
| 项目列表查询 | GET | `/project/projectManagerChange/list` | 关联查询项目+最新变更信息 |
| 单个变更 | POST | `/project/projectManagerChange/change` | 变更单个项目的项目经理 |
| 批量变更 | POST | `/project/projectManagerChange/batchChange` | 批量变更多个项目的项目经理 |
| 变更详情 | GET | `/project/projectManagerChange/detail/{projectId}` | 查看项目的完整变更历史 |

### 3.2 关键实现 - 列表查询SQL

**文件**: `ProjectManagerChangeMapper.xml`

```xml
<select id="selectProjectManagerChangeVoList" resultMap="ProjectManagerChangeVoResult">
    SELECT
        p.project_id,
        p.project_name,
        p.project_code,
        p.create_time as project_create_time,
        p.project_manager_id as current_manager_id,
        u_current.nick_name as current_manager_name,
        latest_change.old_manager_id,
        u_old.nick_name as old_manager_name,
        latest_change.change_reason,
        latest_change.create_by as change_by,
        u_changer.nick_name as change_by_name,
        latest_change.create_time as change_time
    FROM pm_project p
    -- 关联最新变更记录（子查询）
    LEFT JOIN (
        SELECT
            c1.project_id,
            c1.old_manager_id,
            c1.new_manager_id,
            c1.change_reason,
            c1.create_by,
            c1.create_time
        FROM pm_project_manager_change c1
        INNER JOIN (
            SELECT project_id, MAX(create_time) as max_time
            FROM pm_project_manager_change
            WHERE del_flag = '0'
            GROUP BY project_id
        ) c2 ON c1.project_id = c2.project_id AND c1.create_time = c2.max_time
        WHERE c1.del_flag = '0'
    ) latest_change ON p.project_id = latest_change.project_id
    -- 关联当前项目经理
    LEFT JOIN sys_user u_current ON p.project_manager_id = u_current.user_id
    -- 关联原项目经理
    LEFT JOIN sys_user u_old ON latest_change.old_manager_id = u_old.user_id
    -- 关联变更人
    LEFT JOIN sys_user u_changer ON latest_change.create_by = u_changer.user_name
    <where>
        p.del_flag = '0'
        <if test="projectName != null and projectName != ''">
            AND p.project_name LIKE CONCAT('%', #{projectName}, '%')
        </if>
        <if test="currentManagerId != null">
            AND p.project_manager_id = #{currentManagerId}
        </if>
        <if test="params.beginTime != null and params.beginTime != ''">
            AND latest_change.create_time &gt;= #{params.beginTime}
        </if>
        <if test="params.endTime != null and params.endTime != ''">
            AND latest_change.create_time &lt;= #{params.endTime}
        </if>
    </where>
    ORDER BY p.create_time DESC
</select>
```

**ResultMap**:
```xml
<resultMap id="ProjectManagerChangeVoResult" type="ProjectManagerChangeVo">
    <result property="projectId" column="project_id"/>
    <result property="projectName" column="project_name"/>
    <result property="projectCode" column="project_code"/>
    <result property="projectCreateTime" column="project_create_time"/>
    <result property="currentManagerId" column="current_manager_id"/>
    <result property="currentManagerName" column="current_manager_name"/>
    <result property="oldManagerId" column="old_manager_id"/>
    <result property="oldManagerName" column="old_manager_name"/>
    <result property="changeReason" column="change_reason"/>
    <result property="changeBy" column="change_by"/>
    <result property="changeByName" column="change_by_name"/>
    <result property="changeTime" column="change_time"/>
</resultMap>
```

### 3.3 关键实现 - 变更业务逻辑

**文件**: `ProjectManagerChangeServiceImpl.java`

```java
/**
 * 变更项目经理
 * @param projectId 项目ID
 * @param newManagerId 新项目经理ID
 * @param changeReason 变更原因
 * @return 结果
 */
@Transactional
public int changeProjectManager(Long projectId, Long newManagerId, String changeReason) {
    // 1. 查询项目当前信息
    Project project = projectMapper.selectProjectById(projectId);
    if (project == null) {
        throw new ServiceException("项目不存在");
    }

    Long oldManagerId = project.getProjectManagerId();

    // 2. 检查新旧经理是否相同
    if (Objects.equals(oldManagerId, newManagerId)) {
        throw new ServiceException("新旧项目经理相同，无需变更");
    }

    // 3. 更新项目表的项目经理
    project.setProjectManagerId(newManagerId);
    int rows = projectMapper.updateProject(project);

    // 4. 插入变更记录
    ProjectManagerChange change = new ProjectManagerChange();
    change.setProjectId(projectId);
    change.setOldManagerId(oldManagerId);
    change.setNewManagerId(newManagerId);
    change.setChangeReason(changeReason);
    projectManagerChangeMapper.insertProjectManagerChange(change);

    return rows;
}

/**
 * 批量变更项目经理
 * @param projectIds 项目ID数组
 * @param newManagerId 新项目经理ID
 * @param changeReason 变更原因
 * @return 成功变更的项目数量
 */
@Transactional
public int batchChangeProjectManager(Long[] projectIds, Long newManagerId, String changeReason) {
    if (projectIds == null || projectIds.length == 0) {
        throw new ServiceException("请选择要变更的项目");
    }

    int count = 0;
    for (Long projectId : projectIds) {
        try {
            count += changeProjectManager(projectId, newManagerId, changeReason);
        } catch (ServiceException e) {
            // 记录日志，继续处理下一个项目
            log.warn("批量变更项目经理失败，项目ID: {}, 原因: {}", projectId, e.getMessage());
        }
    }

    return count;
}
```

### 3.4 新增VO类

**文件**: `ProjectManagerChangeVo.java`

```java
package com.ruoyi.project.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 项目经理变更视图对象
 */
public class ProjectManagerChangeVo {
    // 项目信息
    private Long projectId;
    private String projectName;
    private String projectCode;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date projectCreateTime;

    // 当前项目经理
    private Long currentManagerId;
    private String currentManagerName;

    // 最新变更信息
    private Long oldManagerId;
    private String oldManagerName;
    private String changeReason;
    private String changeBy;
    private String changeByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date changeTime;

    // Getters and Setters...
}
```

### 3.5 Controller新增方法

**文件**: `ProjectManagerChangeController.java`

```java
/**
 * 项目名称自动补全搜索
 */
@PreAuthorize("@ss.hasPermi('project:projectManagerChange:query')")
@GetMapping("/searchProjects")
public AjaxResult searchProjects(@RequestParam String keyword) {
    if (StringUtils.isEmpty(keyword) || keyword.length() < 2) {
        return success(Collections.emptyList());
    }

    Project query = new Project();
    query.setProjectName(keyword);
    // 只查询前20条
    List<Project> projects = projectService.selectProjectList(query)
        .stream()
        .limit(20)
        .map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("projectId", p.getProjectId());
            map.put("projectName", p.getProjectName());
            map.put("projectCode", p.getProjectCode());
            map.put("value", p.getProjectName()); // autocomplete需要的value字段
            return map;
        })
        .collect(Collectors.toList());

    return success(projects);
}

/**
 * 变更项目经理
 */
@PreAuthorize("@ss.hasPermi('project:projectManagerChange:change')")
@Log(title = "项目经理变更", businessType = BusinessType.UPDATE)
@PostMapping("/change")
public AjaxResult change(@RequestBody ChangeRequest request) {
    return toAjax(projectManagerChangeService.changeProjectManager(
        request.getProjectId(),
        request.getNewManagerId(),
        request.getChangeReason()
    ));
}

/**
 * 批量变更项目经理
 */
@PreAuthorize("@ss.hasPermi('project:projectManagerChange:batchChange')")
@Log(title = "批量变更项目经理", businessType = BusinessType.UPDATE)
@PostMapping("/batchChange")
public AjaxResult batchChange(@RequestBody BatchChangeRequest request) {
    int count = projectManagerChangeService.batchChangeProjectManager(
        request.getProjectIds(),
        request.getNewManagerId(),
        request.getChangeReason()
    );
    return success("成功变更 " + count + " 个项目的项目经理");
}

/**
 * 获取项目经理变更详情
 */
@PreAuthorize("@ss.hasPermi('project:projectManagerChange:detail')")
@GetMapping("/detail/{projectId}")
public AjaxResult detail(@PathVariable Long projectId) {
    // 查询项目基本信息
    Project project = projectService.selectProjectById(projectId);
    if (project == null) {
        return error("项目不存在");
    }

    // 查询变更历史
    ProjectManagerChange query = new ProjectManagerChange();
    query.setProjectId(projectId);
    List<ProjectManagerChange> changes = projectManagerChangeService.selectProjectManagerChangeList(query);

    Map<String, Object> result = new HashMap<>();
    result.put("project", project);
    result.put("changes", changes);

    return success(result);
}
```

**请求对象**:
```java
// ChangeRequest.java
@Data
public class ChangeRequest {
    private Long projectId;
    private Long newManagerId;
    private String changeReason;
}

// BatchChangeRequest.java
@Data
public class BatchChangeRequest {
    private Long[] projectIds;
    private Long newManagerId;
    private String changeReason;
}
```

---

## 4. 前端设计

### 4.1 查询表单改造

**文件**: `ruoyi-ui/src/views/project/projectManagerChange/index.vue`

```vue
<el-form :model="queryParams" ref="queryRef" :inline="true">
  <!-- 项目名称自动补全 -->
  <el-form-item label="项目名称" prop="projectName">
    <el-autocomplete
      v-model="queryParams.projectName"
      :fetch-suggestions="querySearchProjects"
      placeholder="请输入项目名称（至少2个字符）"
      :trigger-on-focus="false"
      :debounce="300"
      @select="handleSelectProject"
      clearable
      style="width: 240px"
    >
      <template #default="{ item }">
        <div class="autocomplete-item">
          <span class="name">{{ item.projectName }}</span>
          <span class="code">{{ item.projectCode }}</span>
        </div>
      </template>
    </el-autocomplete>
  </el-form-item>

  <!-- 项目经理下拉框 -->
  <el-form-item label="项目经理" prop="currentManagerId">
    <el-select
      v-model="queryParams.currentManagerId"
      placeholder="请选择项目经理"
      clearable
      style="width: 200px"
    >
      <el-option
        v-for="manager in managerList"
        :key="manager.userId"
        :label="manager.nickName"
        :value="manager.userId"
      />
    </el-select>
  </el-form-item>

  <!-- 变更时间范围 -->
  <el-form-item label="变更时间">
    <el-date-picker
      v-model="dateRange"
      type="daterange"
      range-separator="-"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      value-format="YYYY-MM-DD"
    />
  </el-form-item>

  <el-form-item>
    <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
    <el-button icon="Refresh" @click="resetQuery">重置</el-button>
  </el-form-item>
</el-form>
```

**Script部分**:
```typescript
// 项目经理列表
const managerList = ref<SysUser[]>([])

// 加载项目经理列表
function loadManagerList() {
  listUserByPost('pm').then(response => {
    managerList.value = response.data
  })
}

// 项目搜索建议
const querySearchProjects = (queryString: string, cb: Function) => {
  if (!queryString || queryString.length < 2) {
    cb([])
    return
  }

  searchProjects(queryString).then(response => {
    cb(response.data)
  })
}

// 选择项目
const handleSelectProject = (item: any) => {
  queryParams.value.projectName = item.projectName
}

// 页面加载时
onMounted(() => {
  loadManagerList()
  getList()
})
```

### 4.2 列表表格改造

```vue
<el-table v-loading="loading" :data="changeList" @selection-change="handleSelectionChange">
  <!-- 复选框列 -->
  <el-table-column type="selection" width="55" align="center" />

  <!-- 序号 -->
  <el-table-column label="序号" type="index" width="60" align="center" />

  <!-- 项目名称 -->
  <el-table-column label="项目名称" prop="projectName" :show-overflow-tooltip="true" min-width="150" />

  <!-- 项目编号 -->
  <el-table-column label="项目编号" prop="projectCode" width="150" />

  <!-- 立项时间 -->
  <el-table-column label="立项时间" prop="projectCreateTime" width="110" align="center">
    <template #default="scope">
      <span>{{ parseTime(scope.row.projectCreateTime, '{y}-{m}-{d}') }}</span>
    </template>
  </el-table-column>

  <!-- 当前项目经理 -->
  <el-table-column label="当前项目经理" prop="currentManagerName" width="120" />

  <!-- 原项目经理 -->
  <el-table-column label="原项目经理" prop="oldManagerName" width="120">
    <template #default="scope">
      <span v-if="scope.row.oldManagerName">{{ scope.row.oldManagerName }}</span>
      <span v-else class="text-gray">-</span>
    </template>
  </el-table-column>

  <!-- 变更原因 -->
  <el-table-column label="变更原因" prop="changeReason" :show-overflow-tooltip="true" min-width="150">
    <template #default="scope">
      <span v-if="scope.row.changeReason">{{ scope.row.changeReason }}</span>
      <span v-else class="text-gray">-</span>
    </template>
  </el-table-column>

  <!-- 变更人 -->
  <el-table-column label="变更人" prop="changeByName" width="100">
    <template #default="scope">
      <span v-if="scope.row.changeByName">{{ scope.row.changeByName }}</span>
      <span v-else class="text-gray">-</span>
    </template>
  </el-table-column>

  <!-- 变更时间 -->
  <el-table-column label="变更时间" prop="changeTime" width="160" align="center">
    <template #default="scope">
      <span v-if="scope.row.changeTime">{{ parseTime(scope.row.changeTime) }}</span>
      <span v-else class="text-gray">-</span>
    </template>
  </el-table-column>

  <!-- 操作列 -->
  <el-table-column label="操作" align="center" width="180" fixed="right" class-name="small-padding fixed-width">
    <template #default="scope">
      <el-button
        link
        type="primary"
        icon="View"
        @click="handleDetail(scope.row)"
        v-hasPermi="['project:projectManagerChange:detail']"
      >
        详情
      </el-button>
      <el-button
        link
        type="primary"
        icon="EditPen"
        @click="handleChange(scope.row)"
        v-hasPermi="['project:projectManagerChange:change']"
      >
        变更
      </el-button>
    </template>
  </el-table-column>
</el-table>
```

### 4.3 工具栏按钮改造

```vue
<el-row :gutter="10" class="mb8">
  <!-- 批量变更按钮 -->
  <el-col :span="1.5">
    <el-button
      type="warning"
      plain
      icon="Edit"
      :disabled="multiple"
      @click="handleBatchChange"
      v-hasPermi="['project:projectManagerChange:batchChange']"
    >
      批量变更
    </el-button>
  </el-col>

  <!-- 导出按钮 -->
  <el-col :span="1.5">
    <el-button
      type="info"
      plain
      icon="Download"
      @click="handleExport"
      v-hasPermi="['project:projectManagerChange:export']"
    >
      导出
    </el-button>
  </el-col>

  <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
</el-row>
```

### 4.4 变更对话框

```vue
<!-- 变更对话框 -->
<el-dialog :title="changeTitle" v-model="changeDialogOpen" width="500px" append-to-body>
  <el-form ref="changeFormRef" :model="changeForm" :rules="changeRules" label-width="110px">
    <!-- 单个变更：显示项目名称 -->
    <el-form-item label="项目名称" v-if="!isBatchChange">
      <el-input v-model="changeForm.projectName" disabled />
    </el-form-item>

    <!-- 批量变更：显示选中项目列表 -->
    <el-form-item label="选中项目" v-if="isBatchChange">
      <div class="project-tags">
        <el-tag
          v-for="project in selectedProjects"
          :key="project.projectId"
          style="margin: 2px"
        >
          {{ project.projectName }}
        </el-tag>
      </div>
    </el-form-item>

    <!-- 当前经理 -->
    <el-form-item label="当前经理" v-if="!isBatchChange">
      <el-input v-model="changeForm.currentManagerName" disabled />
    </el-form-item>

    <!-- 新项目经理 -->
    <el-form-item label="新项目经理" prop="newManagerId">
      <el-select v-model="changeForm.newManagerId" placeholder="请选择新项目经理" style="width: 100%">
        <el-option
          v-for="manager in managerList"
          :key="manager.userId"
          :label="manager.nickName"
          :value="manager.userId"
        />
      </el-select>
    </el-form-item>

    <!-- 变更原因 -->
    <el-form-item label="变更原因" prop="changeReason">
      <el-input
        v-model="changeForm.changeReason"
        type="textarea"
        :rows="4"
        placeholder="请输入变更原因（必填）"
        maxlength="500"
        show-word-limit
      />
    </el-form-item>
  </el-form>

  <template #footer>
    <el-button @click="changeDialogOpen = false">取消</el-button>
    <el-button type="primary" @click="submitChange" :loading="changeLoading">确定</el-button>
  </template>
</el-dialog>
```

**Script部分**:
```typescript
// 变更对话框相关
const changeDialogOpen = ref(false)
const changeLoading = ref(false)
const changeTitle = ref('')
const isBatchChange = ref(false)
const selectedProjects = ref<any[]>([])

const changeForm = ref({
  projectId: undefined,
  projectName: '',
  currentManagerName: '',
  newManagerId: undefined,
  changeReason: ''
})

const changeRules = {
  newManagerId: [{ required: true, message: '请选择新项目经理', trigger: 'change' }],
  changeReason: [{ required: true, message: '请输入变更原因', trigger: 'blur' }]
}

// 单个变更
function handleChange(row: any) {
  reset()
  isBatchChange.value = false
  changeTitle.value = '变更项目经理'
  changeForm.value = {
    projectId: row.projectId,
    projectName: row.projectName,
    currentManagerName: row.currentManagerName,
    newManagerId: undefined,
    changeReason: ''
  }
  changeDialogOpen.value = true
}

// 批量变更
function handleBatchChange() {
  if (ids.value.length === 0) {
    proxy.$modal.msgWarning('请选择要变更的项目')
    return
  }

  reset()
  isBatchChange.value = true
  changeTitle.value = '批量变更项目经理'
  selectedProjects.value = multipleSelection.value.map(item => ({
    projectId: item.projectId,
    projectName: item.projectName
  }))
  changeDialogOpen.value = true
}

// 提交变更
function submitChange() {
  proxy.$refs.changeFormRef.validate((valid: boolean) => {
    if (valid) {
      changeLoading.value = true

      const request = {
        projectIds: isBatchChange.value ? ids.value : [changeForm.value.projectId],
        newManagerId: changeForm.value.newManagerId,
        changeReason: changeForm.value.changeReason
      }

      const api = isBatchChange.value ? batchChangeManager : changeManager

      api(request).then(response => {
        proxy.$modal.msgSuccess(isBatchChange.value ? '批量变更成功' : '变更成功')
        changeDialogOpen.value = false
        getList()
      }).finally(() => {
        changeLoading.value = false
      })
    }
  })
}
```

### 4.5 详情对话框

```vue
<!-- 详情对话框 -->
<el-dialog title="项目经理变更详情" v-model="detailDialogOpen" width="800px" append-to-body>
  <div class="detail-content" v-loading="detailLoading">
    <!-- 项目基本信息 -->
    <el-descriptions title="项目信息" :column="2" border>
      <el-descriptions-item label="项目名称">{{ detailData.project?.projectName }}</el-descriptions-item>
      <el-descriptions-item label="项目编号">{{ detailData.project?.projectCode }}</el-descriptions-item>
      <el-descriptions-item label="当前项目经理">{{ detailData.project?.currentManagerName }}</el-descriptions-item>
      <el-descriptions-item label="立项时间">
        {{ parseTime(detailData.project?.createTime, '{y}-{m}-{d}') }}
      </el-descriptions-item>
    </el-descriptions>

    <!-- 变更历史时间线 -->
    <div class="history-section">
      <h3>变更历史</h3>
      <el-empty v-if="!detailData.changes || detailData.changes.length === 0" description="暂无变更记录" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="change in detailData.changes"
          :key="change.changeId"
          :timestamp="parseTime(change.createTime)"
          placement="top"
        >
          <el-card>
            <div class="change-item">
              <div class="change-manager">
                <span class="label">项目经理变更：</span>
                <span class="old-manager">{{ change.oldManagerName || '无' }}</span>
                <el-icon><Right /></el-icon>
                <span class="new-manager">{{ change.newManagerName }}</span>
              </div>
              <div class="change-reason">
                <span class="label">变更原因：</span>
                <span>{{ change.changeReason }}</span>
              </div>
              <div class="change-by">
                <span class="label">变更人：</span>
                <span>{{ change.changeByName }}</span>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>

  <template #footer>
    <el-button @click="detailDialogOpen = false">关闭</el-button>
  </template>
</el-dialog>
```

**Script部分**:
```typescript
// 详情对话框相关
const detailDialogOpen = ref(false)
const detailLoading = ref(false)
const detailData = ref<any>({})

// 查看详情
function handleDetail(row: any) {
  detailLoading.value = true
  detailDialogOpen.value = true

  getChangeDetail(row.projectId).then(response => {
    detailData.value = response.data
  }).finally(() => {
    detailLoading.value = false
  })
}
```

### 4.6 API函数

**文件**: `ruoyi-ui/src/api/project/projectManagerChange.ts`

```typescript
import request from '@/utils/request'

// 项目搜索
export function searchProjects(keyword: string) {
  return request({
    url: '/project/projectManagerChange/searchProjects',
    method: 'get',
    params: { keyword }
  })
}

// 变更项目经理
export function changeManager(data: any) {
  return request({
    url: '/project/projectManagerChange/change',
    method: 'post',
    data: data
  })
}

// 批量变更项目经理
export function batchChangeManager(data: any) {
  return request({
    url: '/project/projectManagerChange/batchChange',
    method: 'post',
    data: data
  })
}

// 获取变更详情
export function getChangeDetail(projectId: number) {
  return request({
    url: `/project/projectManagerChange/detail/${projectId}`,
    method: 'get'
  })
}
```

---

## 5. 测试计划

### 5.1 单元测试

| 测试项 | 测试方法 | 预期结果 |
|--------|---------|---------|
| 查询项目列表 | testSelectProjectList | 返回所有项目及最新变更信息 |
| 单个变更 | testChangeProjectManager | 事务成功，两表都更新 |
| 批量变更 | testBatchChange | 所有项目都成功变更 |
| 变更详情 | testGetDetail | 返回完整变更历史 |

### 5.2 集成测试

| 测试场景 | 步骤 | 预期结果 |
|---------|------|---------|
| 正常变更流程 | 选择项目→选择新经理→输入原因→提交 | 变更成功，列表刷新 |
| 批量变更 | 勾选多个项目→批量变更→选择经理→提交 | 所有项目都变更成功 |
| 查看详情 | 点击详情按钮 | 显示完整变更历史 |
| 项目搜索 | 输入项目名称≥2字符 | 显示匹配的项目列表 |
| 权限控制 | 无权限用户访问 | 按钮不显示/接口返回403 |

### 5.3 异常测试

| 异常场景 | 预期处理 |
|---------|---------|
| 变更时项目不存在 | 提示"项目不存在" |
| 新旧经理相同 | 提示"新旧项目经理相同，无需变更" |
| 变更原因为空 | 前端校验不通过 |
| 数据库连接失败 | 事务回滚，提示错误 |
| 并发变更冲突 | 乐观锁/悲观锁处理 |

---

## 6. 实施计划

### 6.1 实施步骤（分5个阶段）

#### 阶段1：后端基础改造（2-3小时）

1. ✅ 创建 `ProjectManagerChangeVo.java`
2. ✅ 修改 `ProjectManagerChangeMapper.xml`（关联查询SQL）
3. ✅ 新增 Service 方法（`changeProjectManager`、`batchChangeProjectManager`）
4. ✅ 新增 Controller 接口（`searchProjects`、`change`、`batchChange`、`detail`）
5. ✅ 创建请求对象（`ChangeRequest`、`BatchChangeRequest`）

#### 阶段2：后端单元测试（1小时）

1. ✅ 测试关联查询SQL
2. ✅ 测试变更业务逻辑（单个 + 批量）
3. ✅ 测试详情查询

#### 阶段3：前端查询和列表改造（2-3小时）

1. ✅ 修改查询条件（autoComplete + 下拉框）
2. ✅ 修改列表字段（调整列定义）
3. ✅ 修改工具栏按钮（移除增删改，保留导出）
4. ✅ API 方法调整

#### 阶段4：前端对话框实现（2-3小时）

1. ✅ 实现变更对话框（单个/批量模式切换）
2. ✅ 实现详情对话框（时间线展示）
3. ✅ 表单校验和提交逻辑

#### 阶段5：联调测试（1-2小时）

1. ✅ 测试查询功能
2. ✅ 测试单个变更
3. ✅ 测试批量变更
4. ✅ 测试详情查看
5. ✅ 异常处理测试

### 6.2 预计总时间

**8-12小时**（1-2个工作日）

### 6.3 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 关联查询性能问题 | 数据量大时响应慢 | 添加索引，分页优化 |
| 并发变更冲突 | 数据不一致 | 使用乐观锁或悲观锁 |
| 事务回滚失败 | 数据污染 | 加强异常处理和日志记录 |
| 前端状态管理复杂 | 用户体验差 | 充分测试各种操作场景 |

---

## 7. 后续优化建议

### 7.1 性能优化

- [ ] 添加数据库索引：`pm_project(project_manager_id)`、`pm_project_manager_change(project_id, create_time)`
- [ ] 引入 Redis 缓存项目经理列表（1小时刷新）
- [ ] 关联查询改为分步查询 + 内存组装（如果性能问题严重）

### 7.2 功能增强

- [ ] 导出功能：支持导出变更历史报表
- [ ] 消息通知：变更后发送邮件/站内信给相关人员
- [ ] 变更审批：可选的审批流程（如果业务需要）
- [ ] 数据统计：项目经理变更频率统计、分析报表

### 7.3 用户体验

- [ ] 添加操作确认提示："确定要变更项目经理吗？"
- [ ] 批量变更时显示进度条
- [ ] 详情对话框支持导出为PDF
- [ ] 移动端适配（响应式布局）

---

## 8. 附录

### 8.1 数据字典

| 字段 | 字典类型 | 说明 |
|------|---------|------|
| post_code | sys_post | 岗位编码，'pm'表示项目经理 |

### 8.2 权限配置

| 权限标识 | 权限名称 | 说明 |
|---------|---------|------|
| project:projectManagerChange:query | 查询权限 | 查询项目列表和搜索 |
| project:projectManagerChange:detail | 详情权限 | 查看变更历史详情 |
| project:projectManagerChange:change | 变更权限 | 单个项目变更 |
| project:projectManagerChange:batchChange | 批量变更权限 | 批量变更项目经理 |
| project:projectManagerChange:export | 导出权限 | 导出变更数据 |

### 8.3 参考资料

- RuoYi-Vue 官方文档：http://doc.ruoyi.vip
- Element Plus 文档：https://element-plus.org
- MyBatis 官方文档：https://mybatis.org/mybatis-3/zh/index.html

---

**文档状态**: ✅ 设计完成，待实施
**下一步**: 开始阶段1的后端基础改造
