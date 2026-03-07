# 项目经理变更管理功能设计方案

**设计日期**: 2026-02-14  
**设计者**: Claude  
**版本**: v1.0

---

## 一、功能概述

### 1.1 背景与目标

在项目生命周期管理中，项目经理的变更是常见的业务场景。为了规范项目经理变更流程，追溯变更历史，需要建立完整的项目经理变更管理机制。

**核心目标**：
- ✅ 以**项目为中心**的管理视角，而非以变更记录为中心
- ✅ 支持单个和批量变更操作
- ✅ 完整记录变更历史，支持审计追溯
- ✅ 提供直观的时间线展示变更过程

### 1.2 设计原则

1. **以项目为中心** - 列表展示所有项目及其最新变更信息，便于快速查看当前状态
2. **简化操作流程** - 直接在项目列表中操作变更，减少页面跳转
3. **完整历史追溯** - 保留所有变更记录，支持查看完整变更时间线
4. **事务一致性** - 变更操作同时更新项目表和变更记录表，确保数据一致

---

## 二、业务流程设计

### 2.1 主要业务场景

#### 场景1：查看项目经理分配情况
**操作者**: 项目管理部门、部门领导  
**流程**:
```
进入页面 → 查看项目列表 → 了解当前项目经理分配情况
         ↓
         可按条件筛选（项目名称/当前经理/变更时间）
```

#### 场景2：单个项目变更经理
**操作者**: 项目管理员、部门领导  
**流程**:
```
选择项目 → 点击"变更"按钮 → 填写新项目经理 → 填写变更原因（可选）→ 提交
         ↓
    系统更新项目表 + 记录变更历史
```

#### 场景3：批量变更项目经理
**操作者**: 项目管理员、部门领导  
**流程**:
```
勾选多个项目 → 点击"批量变更"按钮 → 填写新项目经理 → 填写变更原因（可选）→ 提交
         ↓
    系统批量更新项目表 + 记录每个项目的变更历史
```

#### 场景4：查看项目变更历史
**操作者**: 任何有权限的用户  
**流程**:
```
点击项目名称或"详情"按钮 → 查看项目信息 + 完整变更历史时间线
```

### 2.2 权限控制

| 操作 | 权限标识 | 说明 |
|------|---------|------|
| 查看项目列表 | `project:managerChange:list` | 查看所有项目的经理分配情况 |
| 查看变更历史 | `project:managerChange:query` | 查看项目的完整变更历史 |
| 单个变更 | `project:managerChange:change` | 变更单个项目的经理 |
| 批量变更 | `project:managerChange:batchChange` | 批量变更多个项目的经理 |

---

## 三、数据库设计

### 3.1 核心表结构

#### pm_project（项目表）
```sql
-- 关键字段
project_id              bigint       -- 项目ID（主键）
project_name            varchar(200) -- 项目名称
project_code            varchar(100) -- 项目编号
project_manager_id      bigint       -- 当前项目经理ID（外键 → sys_user.user_id）
create_time             datetime     -- 立项时间
```

#### pm_project_manager_change（项目经理变更记录表）
```sql
CREATE TABLE pm_project_manager_change (
  change_id       bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '变更主键ID',
  project_id      bigint(20)   NOT NULL COMMENT '项目ID',
  old_manager_id  bigint(20)   DEFAULT NULL COMMENT '原项目经理ID',
  new_manager_id  bigint(20)   NOT NULL COMMENT '新项目经理ID',
  change_reason   varchar(500) DEFAULT NULL COMMENT '变更原因',
  del_flag        char(1)      DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  create_by       varchar(64)  DEFAULT '' COMMENT '创建者',
  create_time     datetime     DEFAULT NULL COMMENT '创建时间',
  update_by       varchar(64)  DEFAULT '' COMMENT '更新者',
  update_time     datetime     DEFAULT NULL COMMENT '更新时间',
  remark          varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (change_id),
  KEY idx_project_id (project_id),
  KEY idx_create_time (create_time),
  CONSTRAINT fk_pmc_project FOREIGN KEY (project_id) 
    REFERENCES pm_project (project_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目经理变更记录表';
```

### 3.2 索引设计

| 索引名 | 字段 | 类型 | 说明 |
|--------|------|------|------|
| PRIMARY | change_id | 主键 | 变更记录唯一标识 |
| idx_project_id | project_id | 普通索引 | 快速查询某项目的变更历史 |
| idx_create_time | create_time | 普通索引 | 按变更时间范围查询 |

### 3.3 核心查询SQL

#### 项目列表查询（带最新变更信息）
```sql
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
LEFT JOIN (
    -- 最新变更记录子查询
    SELECT c1.*
    FROM pm_project_manager_change c1
    INNER JOIN (
        SELECT project_id, MAX(create_time) as max_time
        FROM pm_project_manager_change
        WHERE del_flag = '0'
        GROUP BY project_id
    ) c2 ON c1.project_id = c2.project_id
        AND c1.create_time = c2.max_time
    WHERE c1.del_flag = '0'
) latest_change ON p.project_id = latest_change.project_id
LEFT JOIN sys_user u_current ON p.project_manager_id = u_current.user_id
LEFT JOIN sys_user u_old ON latest_change.old_manager_id = u_old.user_id
LEFT JOIN sys_user u_changer ON latest_change.create_by = u_changer.user_name
WHERE p.del_flag = '0'
ORDER BY p.create_time DESC;
```

**查询逻辑说明**:
- 主查询：所有项目（pm_project）
- LEFT JOIN 最新变更记录：每个项目只关联最新的一条变更记录
- 如果项目从未变更过，变更相关字段为 NULL
- 关联3个 sys_user 表：当前经理、原经理、变更人

#### 项目变更历史查询
```sql
SELECT
    pmc.change_id,
    pmc.project_id,
    pmc.old_manager_id,
    pmc.new_manager_id,
    pmc.change_reason,
    pmc.create_by,
    pmc.create_time,
    p.project_name,
    p.project_code,
    u1.nick_name as old_manager_name,
    u2.nick_name as new_manager_name
FROM pm_project_manager_change pmc
LEFT JOIN pm_project p ON pmc.project_id = p.project_id
LEFT JOIN sys_user u1 ON pmc.old_manager_id = u1.user_id
LEFT JOIN sys_user u2 ON pmc.new_manager_id = u2.user_id
WHERE pmc.project_id = #{projectId}
  AND pmc.del_flag = '0'
ORDER BY pmc.create_time DESC;
```

---

## 四、后端接口设计

### 4.1 数据传输对象（DTO/VO）

#### ProjectManagerChangeVO（项目视图对象）
```java
public class ProjectManagerChangeVO {
    private Long projectId;              // 项目ID
    private String projectName;          // 项目名称
    private String projectCode;          // 项目编号
    private Date projectCreateTime;      // 立项时间
    private Long currentManagerId;       // 当前项目经理ID
    private String currentManagerName;   // 当前项目经理姓名
    private Long oldManagerId;           // 原项目经理ID（最新变更）
    private String oldManagerName;       // 原项目经理姓名（最新变更）
    private String changeReason;         // 变更原因（最新变更）
    private String changeBy;             // 变更人（最新变更）
    private String changeByName;         // 变更人姓名（最新变更）
    private Date changeTime;             // 变更时间（最新变更）
}
```

### 4.2 RESTful API

#### 查询项目列表
```
GET /project/managerChange/list

请求参数（QueryString）:
- pageNum: 页码
- pageSize: 每页条数
- projectName: 项目名称（模糊查询）
- currentManagerId: 当前项目经理ID
- params.beginChangeTime: 变更开始时间
- params.endChangeTime: 变更结束时间

响应（TableDataInfo）:
{
  "code": 200,
  "msg": "查询成功",
  "total": 100,
  "rows": [
    {
      "projectId": 1,
      "projectName": "XX项目",
      "projectCode": "ZH-BJ-001-2026",
      "projectCreateTime": "2026-01-15",
      "currentManagerId": 128,
      "currentManagerName": "张三",
      "oldManagerId": 159,
      "oldManagerName": "李四",
      "changeReason": "项目交接",
      "changeBy": "admin",
      "changeByName": "管理员",
      "changeTime": "2026-02-10 10:30:00"
    }
  ]
}
```

#### 查询项目变更历史
```
GET /project/managerChange/history/{projectId}

路径参数:
- projectId: 项目ID

响应（AjaxResult）:
{
  "code": 200,
  "msg": "查询成功",
  "data": [
    {
      "changeId": 2,
      "projectId": 1,
      "oldManagerId": 159,
      "oldManagerName": "李四",
      "newManagerId": 128,
      "newManagerName": "张三",
      "changeReason": "项目交接",
      "createBy": "admin",
      "createTime": "2026-02-10 10:30:00"
    },
    {
      "changeId": 1,
      "projectId": 1,
      "oldManagerId": null,
      "oldManagerName": null,
      "newManagerId": 159,
      "newManagerName": "李四",
      "changeReason": "初始设置",
      "createBy": "admin",
      "createTime": "2026-01-15 09:00:00"
    }
  ]
}
```

#### 单个项目变更经理
```
POST /project/managerChange/change

请求体（JSON）:
{
  "projectId": 1,
  "newManagerId": 128,
  "changeReason": "项目交接"  // 可选
}

响应（AjaxResult）:
{
  "code": 200,
  "msg": "变更成功"
}

业务逻辑:
1. 查询项目，获取当前项目经理ID（oldManagerId）
2. 更新项目表的 project_manager_id = newManagerId
3. 插入变更记录到 pm_project_manager_change
4. 事务提交

权限: project:managerChange:change
日志: @Log(title="项目经理变更", businessType=UPDATE)
```

#### 批量变更项目经理
```
POST /project/managerChange/batchChange

请求参数（FormData）:
- projectIds: 项目ID数组
- newManagerId: 新项目经理ID
- changeReason: 变更原因（可选）

响应（AjaxResult）:
{
  "code": 200,
  "msg": "批量变更成功"
}

业务逻辑:
1. 遍历 projectIds
2. 对每个项目执行单个变更逻辑
3. 批量事务提交

权限: project:managerChange:batchChange
日志: @Log(title="项目经理批量变更", businessType=UPDATE)
```

### 4.3 Service层核心方法

```java
public interface IProjectManagerChangeService {
    
    /** 查询项目列表（带最新变更信息） */
    List<ProjectManagerChangeVO> selectProjectListWithLatestChange(ProjectManagerChangeVO vo);
    
    /** 查询项目的完整变更历史 */
    List<ProjectManagerChange> selectProjectChangeHistory(Long projectId);
    
    /** 变更项目经理（单个）- 事务方法 */
    @Transactional
    int changeProjectManager(Long projectId, Long newManagerId, String changeReason);
    
    /** 批量变更项目经理 - 事务方法 */
    @Transactional
    int batchChangeProjectManager(Long[] projectIds, Long newManagerId, String changeReason);
}
```

**事务处理**:
- `changeProjectManager()` 使用 `@Transactional` 确保项目表更新和变更记录插入的原子性
- 任何异常都会触发回滚
- `batchChangeProjectManager()` 内部调用单个变更方法，整体在一个事务中

---

## 五、前端页面设计

### 5.1 页面布局

```
┌─────────────────────────────────────────────────────────┐
│  查询条件区                                               │
│  [项目名称: autocomplete] [当前经理: select] [变更时间]   │
│  [搜索] [重置]                                            │
├─────────────────────────────────────────────────────────┤
│  工具栏                                                   │
│  [批量变更] [显示搜索▼]                                   │
├─────────────────────────────────────────────────────────┤
│  项目列表表格                                             │
│  ☑│序号│项目名称│项目编号│立项时间│当前经理│原经理│...   │
│  ☑│ 1  │XX项目 │ZH-001  │2026-01 │张三   │李四  │...    │
│  ☑│ 2  │YY项目 │ZH-002  │2026-02 │王五   │-     │...    │
│                                                [详情][变更]│
├─────────────────────────────────────────────────────────┤
│  分页控件                                                 │
│  共100条  [1] 2 3 4 5 ... 10  每页10条▼                  │
└─────────────────────────────────────────────────────────┘
```

### 5.2 表格列定义

| 列名 | 宽度 | 对齐 | 说明 |
|------|------|------|------|
| 复选框 | 55px | center | 用于批量选择 |
| 序号 | 55px | center | 行号 |
| 项目名称 | 180px | left | 可点击查看详情，超出显示... |
| 项目编号 | 150px | center | - |
| 立项时间 | 110px | center | 格式：YYYY-MM-DD |
| 当前项目经理 | 120px | center | 重点列 |
| 原项目经理 | 120px | center | 无变更记录显示"-" |
| 变更原因 | 150px | left | 超出显示...，无变更显示"-" |
| 变更人 | 100px | center | 无变更显示"-" |
| 变更时间 | 160px | center | 格式：YYYY-MM-DD HH:mm:ss，无变更显示"-" |
| 操作 | 150px | center | [详情] [变更] 按钮 |

### 5.3 查询条件交互

#### 项目名称（el-autocomplete）
- 触发条件：输入≥2个字符
- 防抖：300ms
- 远程搜索：`GET /project/project/search?projectName={keyword}`
- 显示格式：
  ```
  XX项目
  ZH-BJ-001-2026
  ```
- 选中后：绑定到 `queryParams.projectName`

#### 当前项目经理（el-select）
- 数据来源：`GET /system/user/listByPost?postCode=pm`
- 可筛选：`filterable`
- 可清空：`clearable`
- 显示：用户昵称
- 值：userId

#### 变更时间范围（el-date-picker）
- 类型：`daterange`
- 格式：`YYYY-MM-DD`
- 绑定：`daterangeChangeTime` 数组
- 转换：查询时写入 `params.beginChangeTime` 和 `params.endChangeTime`

### 5.4 变更对话框设计

#### 单个变更对话框
```
┌────────────────────────────┐
│  变更项目经理               │
├────────────────────────────┤
│  项目: [XX项目]             │
│  当前项目经理: 李四（只读） │
│  新项目经理: [下拉选择 ▼]   │
│  变更原因: [文本域]         │
│            最多500字        │
│                            │
│          [取消] [确定]      │
└────────────────────────────┘
```

#### 批量变更对话框
```
┌────────────────────────────┐
│  批量变更项目经理           │
├────────────────────────────┤
│  选中项目:                  │
│  [XX项目] [YY项目] [ZZ项目] │
│  新项目经理: [下拉选择 ▼]   │
│  变更原因: [文本域]         │
│            最多500字        │
│                            │
│          [取消] [确定]      │
└────────────────────────────┘
```

**表单校验规则**:
- 新项目经理：必填
- 变更原因：选填，最多500字符

### 5.5 详情对话框（时间线）

```
┌──────────────────────────────────────┐
│  项目经理变更详情                      │
├──────────────────────────────────────┤
│  【项目信息】                          │
│  ┌────────────────────────────────┐  │
│  │ 项目名称: XX项目                │  │
│  │ 项目编号: ZH-BJ-001-2026       │  │
│  │ 当前项目经理: 张三              │  │
│  │ 立项时间: 2026-01-15           │  │
│  └────────────────────────────────┘  │
│                                      │
│  【变更历史】                          │
│  ○ 2026-02-10 10:30:00             │
│  │ 项目经理变更: 李四 → 张三          │
│  │ 变更原因: 项目交接                │
│  │ 变更人: admin                     │
│  │                                   │
│  ○ 2026-01-15 09:00:00             │
│    项目经理变更: 无 → 李四            │
│    变更原因: 初始设置                │
│    变更人: admin                     │
│                                      │
│               [关闭]                 │
└──────────────────────────────────────┘
```

**技术实现**:
- 使用 `el-descriptions` 展示项目信息
- 使用 `el-timeline` 组件展示变更历史
- 使用 `el-card` 包裹每条变更记录
- 使用 `el-empty` 处理无变更记录的情况

### 5.6 前端API函数

```javascript
// 查询项目列表（带最新变更信息）
export function listManagerChange(query) {
  return request({
    url: '/project/managerChange/list',
    method: 'get',
    params: query
  })
}

// 查询项目变更历史
export function getProjectChangeHistory(projectId) {
  return request({
    url: '/project/managerChange/history/' + projectId,
    method: 'get'
  })
}

// 单个项目变更经理
export function changeManager(data) {
  return request({
    url: '/project/managerChange/change',
    method: 'post',
    data: data
  })
}

// 批量变更项目经理
export function batchChangeManager(projectIds, newManagerId, changeReason) {
  return request({
    url: '/project/managerChange/batchChange',
    method: 'post',
    params: {
      projectIds: projectIds,
      newManagerId: newManagerId,
      changeReason: changeReason
    }
  })
}
```

---

## 六、技术实现要点

### 6.1 MyBatis XML 映射

#### ResultMap定义
```xml
<resultMap type="com.ruoyi.project.domain.vo.ProjectManagerChangeVO" 
           id="ProjectManagerChangeVOResult">
    <result property="projectId"           column="project_id"           />
    <result property="projectName"         column="project_name"         />
    <result property="projectCode"         column="project_code"         />
    <result property="projectCreateTime"   column="project_create_time"  />
    <result property="currentManagerId"    column="current_manager_id"   />
    <result property="currentManagerName"  column="current_manager_name" />
    <result property="oldManagerId"        column="old_manager_id"       />
    <result property="oldManagerName"      column="old_manager_name"     />
    <result property="changeReason"        column="change_reason"        />
    <result property="changeBy"            column="change_by"            />
    <result property="changeByName"        column="change_by_name"       />
    <result property="changeTime"          column="change_time"          />
</resultMap>
```

#### 动态SQL查询
```xml
<select id="selectProjectListWithLatestChange" 
        parameterType="ProjectManagerChangeVO" 
        resultMap="ProjectManagerChangeVOResult">
    SELECT ... (核心SQL见3.3节)
    WHERE p.del_flag = '0'
    <if test="projectName != null and projectName != ''">
        AND p.project_name LIKE CONCAT('%', #{projectName}, '%')
    </if>
    <if test="currentManagerId != null">
        AND p.project_manager_id = #{currentManagerId}
    </if>
    <if test="changeTime != null">
        AND latest_change.create_time BETWEEN 
            #{params.beginChangeTime} AND #{params.endChangeTime}
    </if>
    ORDER BY p.create_time DESC
</select>
```

### 6.2 事务管理

```java
@Service
public class ProjectManagerChangeServiceImpl 
    implements IProjectManagerChangeService {
    
    @Autowired
    private ProjectMapper projectMapper;
    
    @Autowired
    private ProjectManagerChangeMapper changeMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeProjectManager(Long projectId, Long newManagerId, 
                                     String changeReason) {
        // 1. 查询项目
        Project project = projectMapper.selectProjectByProjectId(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        
        Long oldManagerId = project.getProjectManagerId();
        
        // 2. 更新项目表
        project.setProjectManagerId(newManagerId);
        int rows = projectMapper.updateProject(project);
        
        // 3. 记录变更
        ProjectManagerChange change = new ProjectManagerChange();
        change.setProjectId(projectId);
        change.setOldManagerId(oldManagerId);
        change.setNewManagerId(newManagerId);
        change.setChangeReason(changeReason);
        change.setCreateBy(SecurityUtils.getUsername());
        change.setCreateTime(DateUtils.getNowDate());
        changeMapper.insertProjectManagerChange(change);
        
        return rows;
    }
}
```

**关键点**:
- `@Transactional` 确保原子性
- 任何异常触发回滚
- 记录操作人和时间

### 6.3 权限控制

```java
@RestController
@RequestMapping("/project/managerChange")
public class ProjectManagerChangeController {
    
    @PreAuthorize("@ss.hasPermi('project:managerChange:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProjectManagerChangeVO vo) {
        // ...
    }
    
    @PreAuthorize("@ss.hasPermi('project:managerChange:change')")
    @Log(title = "项目经理变更", businessType = BusinessType.UPDATE)
    @PostMapping("/change")
    public AjaxResult change(@RequestBody ProjectManagerChange data) {
        // ...
    }
    
    @PreAuthorize("@ss.hasPermi('project:managerChange:batchChange')")
    @Log(title = "项目经理批量变更", businessType = BusinessType.UPDATE)
    @PostMapping("/batchChange")
    public AjaxResult batchChange(...) {
        // ...
    }
}
```

### 6.4 操作日志

通过 `@Log` 注解自动记录操作：
- 操作标题
- 业务类型（INSERT/UPDATE/DELETE/GRANT等）
- 请求方法
- 请求参数
- 响应结果
- 操作人
- 操作时间
- 操作IP

日志存储在 `sys_oper_log` 表，可在系统管理 → 操作日志中查看。

### 6.5 Vue组件关键代码

#### 查询列表
```javascript
function getList() {
  loading.value = true
  queryParams.value.params = {}
  if (daterangeChangeTime.value && daterangeChangeTime.value.length === 2) {
    queryParams.value.params["beginChangeTime"] = daterangeChangeTime.value[0]
    queryParams.value.params["endChangeTime"] = daterangeChangeTime.value[1]
  }
  listManagerChange(queryParams.value).then(response => {
    projectList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
```

#### 项目搜索（防抖）
```javascript
function querySearchProject(queryString, cb) {
  if (!queryString || queryString.length < 2) {
    cb([])
    return
  }
  request({
    url: '/project/project/search',
    method: 'get',
    params: { projectName: queryString }
  }).then(response => {
    cb(response.data || [])
  }).catch(() => {
    cb([])
  })
}
```

#### 批量变更
```javascript
function submitChange() {
  proxy.$refs["changeFormRef"].validate(valid => {
    if (valid) {
      if (isBatchChange.value) {
        // 批量变更
        batchChangeManager(
          ids.value, 
          changeForm.value.newManagerId, 
          changeForm.value.changeReason
        ).then(response => {
          proxy.$modal.msgSuccess("批量变更成功")
          changeDialogOpen.value = false
          getList()
        })
      } else {
        // 单个变更
        changeManager(changeForm.value).then(response => {
          proxy.$modal.msgSuccess("变更成功")
          changeDialogOpen.value = false
          getList()
        })
      }
    }
  })
}
```

---

## 七、测试要点

### 7.1 功能测试

| 测试场景 | 测试步骤 | 预期结果 |
|---------|---------|---------|
| 查看项目列表 | 进入页面 | 显示所有项目，包含最新变更信息 |
| 项目名称搜索 | 输入项目名称关键字 | 实时显示匹配的项目 |
| 当前经理筛选 | 选择项目经理 | 只显示该经理负责的项目 |
| 变更时间筛选 | 选择时间范围 | 只显示该时间段内有变更的项目 |
| 单个变更 | 点击变更按钮 → 选择新经理 → 提交 | 成功提示，列表刷新，记录变更历史 |
| 批量变更 | 勾选多个项目 → 批量变更 → 选择新经理 → 提交 | 成功提示，所有项目更新，记录历史 |
| 查看详情 | 点击项目名称 | 显示项目信息和完整变更时间线 |
| 无变更记录 | 查看从未变更过的项目 | 变更相关列显示"-" |

### 7.2 性能测试

| 测试指标 | 测试数据量 | 目标 |
|---------|-----------|------|
| 列表查询响应时间 | 1000个项目 | <500ms |
| 列表查询响应时间 | 10000个项目 | <1s |
| 单个变更响应时间 | - | <200ms |
| 批量变更响应时间 | 100个项目 | <2s |
| 历史查询响应时间 | 100条变更记录 | <300ms |

### 7.3 边界测试

| 测试场景 | 测试方法 | 预期结果 |
|---------|---------|---------|
| 项目不存在 | 变更不存在的项目 | 提示"项目不存在" |
| 变更原因超长 | 输入>500字符 | 前端限制，无法提交 |
| 权限不足 | 无变更权限用户操作 | 提示权限不足，操作失败 |
| 并发变更 | 两个用户同时变更同一项目 | 后提交的覆盖前者，两条变更记录 |
| 事务回滚 | 变更过程中数据库异常 | 回滚，项目表和变更表都不更新 |

### 7.4 安全测试

| 测试点 | 测试方法 | 预期结果 |
|-------|---------|---------|
| XSS防护 | 在变更原因中输入脚本 | 自动转义，不执行 |
| SQL注入 | 在查询条件中注入SQL | 参数化查询，注入无效 |
| 越权访问 | 无权限用户直接调用API | 返回403 Forbidden |
| CSRF防护 | 跨站请求伪造 | Token验证失败 |

---

## 八、部署与上线

### 8.1 数据库变更

```sql
-- 1. 创建变更记录表（如不存在）
-- 见3.1节 SQL

-- 2. 添加索引（如不存在）
CREATE INDEX idx_project_id ON pm_project_manager_change(project_id);
CREATE INDEX idx_create_time ON pm_project_manager_change(create_time);

-- 3. 菜单权限数据
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, 
                      is_frame, is_cache, menu_type, visible, status, 
                      perms, icon, create_by, create_time, remark)
VALUES('项目经理变更', '2059', '4', 'managerChange', 'project/managerChange/index', 
       1, 0, 'C', '0', '0', 'project:managerChange:list', '#', 
       'admin', sysdate(), '项目经理变更菜单');

-- 获取父菜单ID
SET @parentId = LAST_INSERT_ID();

-- 按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, 
                      is_frame, is_cache, menu_type, visible, status, 
                      perms, icon, create_by, create_time)
VALUES
('项目经理变更查询', @parentId, '1', '#', '', 1, 0, 'F', '0', '0', 
 'project:managerChange:query', '#', 'admin', sysdate()),
('项目经理变更', @parentId, '2', '#', '', 1, 0, 'F', '0', '0', 
 'project:managerChange:change', '#', 'admin', sysdate()),
('批量变更项目经理', @parentId, '3', '#', '', 1, 0, 'F', '0', '0', 
 'project:managerChange:batchChange', '#', 'admin', sysdate());
```

### 8.2 后端部署

```bash
# 1. 编译打包
mvn clean package -Dmaven.test.skip=true

# 2. 停止旧服务
./ry.sh stop

# 3. 备份旧JAR
mv ruoyi-admin/target/ruoyi-admin.jar ruoyi-admin/target/ruoyi-admin.jar.bak

# 4. 启动新服务
./ry.sh start

# 5. 检查日志
tail -f logs/sys-info.log
```

### 8.3 前端部署

```bash
# 1. 编译生产版本
cd ruoyi-ui
npm run build:prod

# 2. 部署到Nginx
cp -r dist/* /usr/share/nginx/html/

# 3. 重启Nginx
nginx -s reload
```

### 8.4 回滚方案

如果上线后发现问题：

**数据库回滚**:
```sql
-- 删除变更记录表（慎重！会丢失变更历史）
DROP TABLE IF EXISTS pm_project_manager_change;

-- 删除菜单权限
DELETE FROM sys_menu WHERE perms LIKE 'project:managerChange:%';
```

**后端回滚**:
```bash
./ry.sh stop
mv ruoyi-admin/target/ruoyi-admin.jar.bak ruoyi-admin/target/ruoyi-admin.jar
./ry.sh start
```

**前端回滚**:
```bash
# 恢复旧版本前端文件
```

---

## 九、后续优化方向

### 9.1 功能增强

1. **变更审批流程**
   - 项目经理变更需要部门领导审批
   - 审批通过后才真正变更
   - 支持审批意见和驳回

2. **变更通知**
   - 变更完成后发送邮件/站内信通知相关人员
   - 通知新旧项目经理
   - 通知项目组成员

3. **批量导入导出**
   - 支持Excel批量导入变更计划
   - 导出变更历史报表

4. **统计分析**
   - 项目经理负责项目数量统计
   - 项目经理变更频率分析
   - 项目稳定性评估（变更次数）

### 9.2 性能优化

1. **索引优化**
   - 根据实际查询场景添加复合索引
   - 分析慢查询并优化

2. **缓存策略**
   - 项目经理列表缓存（Redis）
   - 项目列表分页缓存

3. **异步处理**
   - 批量变更异步执行
   - 变更通知异步发送

### 9.3 用户体验优化

1. **搜索增强**
   - 项目编号搜索
   - 历史经理搜索
   - 多条件组合搜索

2. **列表展示优化**
   - 自定义列显示
   - 列排序
   - 数据导出

3. **移动端适配**
   - 响应式布局
   - 移动端专属UI

---

## 十、附录

### 10.1 相关文档

- **需求文档**: `docs/pm/PM需求.md`
- **数据库设计**: `pm-sql/init/00_tables_ddl.sql`
- **菜单数据**: `pm-sql/init/02_menu_data.sql`
- **实施记录**: `docs/plans/2026-02-14-project-manager-change-implementation.md`

### 10.2 关键代码文件

**后端**:
- `ProjectManagerChangeVO.java` - 视图对象
- `ProjectManagerChangeMapper.java` - Mapper接口
- `ProjectManagerChangeMapper.xml` - SQL映射
- `IProjectManagerChangeService.java` - Service接口
- `ProjectManagerChangeServiceImpl.java` - Service实现
- `ProjectManagerChangeController.java` - Controller

**前端**:
- `ruoyi-ui/src/api/project/managerChange.js` - API函数
- `ruoyi-ui/src/views/project/managerChange/index.vue` - 页面组件

### 10.3 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.8 | 后端框架 |
| MyBatis | 3.5.x | ORM框架 |
| Vue | 3.5.x | 前端框架 |
| Element Plus | 2.13.x | UI组件库 |
| MySQL | 8.0.x | 数据库 |
| Java | 17 | JDK版本 |

---

**文档状态**: ✅ 已完成  
**最后更新**: 2026-02-14  
**维护人**: Claude
