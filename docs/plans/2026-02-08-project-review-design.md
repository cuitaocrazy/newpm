# 立项审核功能设计文档

**日期：** 2026-02-08
**功能：** 项目管理 - 立项审核
**描述：** 团队主管审核待审核的项目立项申请，可以通过或拒绝

---

## 1. 需求概述

### 1.1 功能定位
专门的审核工作台页面，用于团队负责人审核待审核的项目立项申请。

### 1.2 核心功能
- 查询和展示待审核项目列表
- 查看项目完整详情
- 审核操作（通过/拒绝）
- 邮件通知项目创建人

### 1.3 查询条件
- 项目名称
- 项目部门
- 项目分类
- 一级区域
- 二级区域
- 项目经理
- 市场经理
- 项目阶段
- 审核状态

### 1.4 列表显示字段
**基础信息：**
- 项目编号
- 项目名称
- 项目部门
- 项目分类
- 一级区域
- 二级区域

**状态信息：**
- 项目阶段
- 审核状态

**人员信息：**
- 项目经理
- 市场经理

**时间信息：**
- 立项年度
- 创建时间

**操作列：**
- 审核按钮

---

## 2. 整体架构

### 2.1 技术方案
复用现有的 `pm_project` 和 `pm_project_approval` 表，不需要新建表。

### 2.2 前端架构
- **新增页面：** `ruoyi-ui/src/views/project/review/index.vue`（立项审核列表页）
- **新增 API：** `ruoyi-ui/src/api/project/review.js`（审核相关接口）
- **复用组件：** DictTag、Pagination、部门树选择器、用户选择器等

### 2.3 后端架构
- **新增 Controller：** `ProjectReviewController`（`/project/review/**`）
- **新增 Service：** `IProjectReviewService` 和实现类
- **复用 Mapper：** `ProjectMapper` 和 `ProjectApprovalMapper`
- **新增邮件服务：** 审核结果邮件通知

### 2.4 核心特点
1. 只查询 `approval_status = '0'`（待审核）的项目
2. 使用 `@DataScope` 实现部门数据权限过滤
3. 审核操作同时更新项目表和插入审核记录表
4. 异步发送邮件通知，不阻塞审核流程

---

## 3. 后端设计

### 3.1 Controller 层

**类名：** `ProjectReviewController`
**路径：** `/project/review/**`

**主要接口：**

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询待审核项目列表 | GET | `/project/review/list` | 带分页和查询条件 |
| 获取项目详细信息 | GET | `/project/review/{projectId}` | 用于审核对话框 |
| 审核操作 | POST | `/project/review/approve` | 通过/拒绝 |

### 3.2 Service 层

**接口：** `IProjectReviewService`
**实现类：** `ProjectReviewServiceImpl`

**核心方法：**

#### selectReviewList(Project project)
- **功能：** 查询待审核项目列表
- **查询条件：** `approval_status = '0'`（待审核）
- **数据权限：** 使用 `@DataScope` 注解实现部门数据权限
- **关联查询：**
  - 用户表（获取项目经理、市场经理姓名）
  - 二级区域表（获取省份名称）
  - 部门表（获取部门名称）
- **支持条件：** 项目名称、项目部门、项目分类、区域、人员、阶段等

#### selectProjectById(Long projectId)
- **功能：** 获取项目详细信息
- **返回：** 完整的项目信息，包含关联的用户、客户、联系人等信息

#### approveProject(Long projectId, String approvalStatus, String approvalReason)
- **功能：** 审核项目
- **参数：**
  - `projectId`：项目ID
  - `approvalStatus`：审核状态（1通过/2拒绝）
  - `approvalReason`：审核意见
- **校验：**
  - 检查项目是否存在
  - 检查项目是否为待审核状态
  - 检查当前用户是否有 `tdfzr` 岗位
- **操作：**
  1. 更新项目表：`approval_status`、`approval_reason`、`approval_time`、`approver_id`
  2. 插入审核记录表：记录完整的审核信息
  3. 异步发送邮件：调用邮件服务通知项目创建人

### 3.3 Mapper 层

**复用现有 Mapper：**
- `ProjectMapper`：项目表操作
- `ProjectApprovalMapper`：审核记录表操作

**新增 SQL（在 ProjectMapper.xml 中）：**
```xml
<!-- 查询待审核项目列表 -->
<select id="selectReviewList" resultMap="ProjectResult">
    SELECT
        p.*,
        d.dept_name,
        u1.nick_name as project_manager_name,
        u2.nick_name as market_manager_name,
        pr.province_name
    FROM pm_project p
    LEFT JOIN sys_dept d ON p.project_dept = d.dept_id
    LEFT JOIN sys_user u1 ON p.project_manager_id = u1.user_id
    LEFT JOIN sys_user u2 ON p.market_manager_id = u2.user_id
    LEFT JOIN pm_province pr ON p.province_id = pr.province_id
    WHERE p.approval_status = '0'
    ${params.dataScope}
    <if test="projectName != null and projectName != ''">
        AND p.project_name LIKE CONCAT('%', #{projectName}, '%')
    </if>
    <!-- 其他查询条件 -->
    ORDER BY p.create_time DESC
</select>
```

---

## 4. 前端设计

### 4.1 页面结构

**文件路径：** `ruoyi-ui/src/views/project/review/index.vue`

#### 4.1.1 查询表单区域
- 使用 `el-form` + `inline` 布局
- 查询条件组件：
  - 项目名称：`el-autocomplete`（支持自动补全）
  - 项目部门：`el-tree-select`（部门树选择器）
  - 项目分类：`el-select`（字典 sys_xmfl）
  - 一级区域：`el-select`（字典 sys_yjqy）
  - 二级区域：`el-select`（动态加载，依赖一级区域）
  - 项目经理：`el-select`（用户列表，支持搜索）
  - 市场经理：`el-select`（用户列表，支持搜索）
  - 项目阶段：`el-select`（字典 sys_xmjd）
  - 审核状态：`el-select`（字典 sys_shzt）
- 操作按钮：搜索、重置

#### 4.1.2 列表区域
- 使用 `el-table` 展示数据
- 列配置：
  - 项目编号（width: 180）
  - 项目名称（width: 200, show-overflow-tooltip）
  - 项目部门（width: 150）
  - 项目分类（width: 120, dict-tag）
  - 一级区域（width: 120, dict-tag）
  - 二级区域（width: 120）
  - 项目阶段（width: 120, dict-tag）
  - 审核状态（width: 120, dict-tag）
  - 项目经理（width: 120）
  - 市场经理（width: 120）
  - 立项年度（width: 100）
  - 创建时间（width: 180）
  - 操作列（width: 100, fixed: right）：审核按钮（蓝色 primary）

#### 4.1.3 审核对话框
- 使用 `el-dialog`，宽度 1200px
- 标题：项目审核 - {项目名称}
- 内容区域：
  - 使用 `el-collapse` 展示项目详情
  - 分模块折叠（默认第一个模块展开）：
    1. **基本信息**：项目编号、项目名称、项目全称、项目分类、行业、一级区域、二级区域、简称、立项年度、项目部门
    2. **人员信息**：项目经理、市场经理、销售负责人、团队负责人、参与人员
    3. **客户信息**：客户名称、客户联系人、商户联系人、商户联系方式
    4. **时间信息**：启动日期、结束日期、投产日期、验收日期
    5. **财务信息**：项目预算、项目费用、费用预算、成本预算、人力费用、采购成本、税率
    6. **收入确认信息**：确认状态、确认季度、确认金额、税后金额、确认人、确认时间
    7. **项目描述**：项目计划、项目描述、项目地址、备注
- 底部操作区域：
  - 审核意见输入框：`el-input type="textarea"`（rows: 4）
  - 通过按钮：绿色 success
  - 拒绝按钮：红色 danger
  - 取消按钮：默认样式

### 4.2 API 接口

**文件路径：** `ruoyi-ui/src/api/project/review.js`

```typescript
// 查询待审核项目列表
export function listReview(query: ReviewQueryParams): Promise<TableDataInfo<Project[]>> {
  return request({ url: '/project/review/list', method: 'get', params: query })
}

// 获取项目详细信息
export function getReview(projectId: number): Promise<AjaxResult<Project>> {
  return request({ url: `/project/review/${projectId}`, method: 'get' })
}

// 审核项目
export function approveProject(data: ApproveParams): Promise<AjaxResult<void>> {
  return request({ url: '/project/review/approve', method: 'post', data })
}
```

---

## 5. 数据流和交互流程

### 5.1 页面加载流程
1. 进入页面 → 调用 `getList()` 加载待审核项目列表
2. 同时加载字典数据：项目分类、一级区域、项目阶段、审核状态等
3. 加载部门树数据、用户列表（项目经理、市场经理选项）

### 5.2 审核操作流程
1. 用户点击"审核"按钮 → 调用 `GET /project/review/{projectId}` 获取项目完整信息
2. 打开审核对话框，展示项目详情（使用 el-collapse 分模块显示）
3. 用户选择"通过"或"拒绝"：
   - 如果选择"拒绝"，审核意见为必填项（前端校验）
   - 如果选择"通过"，审核意见为可选项
4. 点击提交 → 调用 `POST /project/review/approve` 接口
5. 后端处理：
   - 更新项目表（approval_status、approval_reason、approval_time、approver_id）
   - 插入审核记录表
   - 异步发送邮件
6. 前端处理：
   - 关闭对话框
   - 显示成功提示
   - 刷新列表（已审核项目自动消失）

### 5.3 错误处理
- 项目不存在或已被审核：提示"该项目已被审核或不存在"
- 用户无权限：提示"您没有审核权限"
- 邮件发送失败：记录日志，不影响审核流程

---

## 6. 权限控制

### 6.1 岗位权限校验

后端在 `approveProject()` 方法中校验：
```java
// 获取当前用户的岗位列表
List<SysPost> posts = userService.selectPostsByUserId(userId);
boolean hasTdfzrPost = posts.stream()
    .anyMatch(post -> "tdfzr".equals(post.getPostCode()));
if (!hasTdfzrPost) {
    throw new ServiceException("您没有审核权限，只有团队负责人可以审核");
}
```

### 6.2 部门数据权限

在 Service 方法上使用 `@DataScope` 注解：
```java
@DataScope(deptAlias = "d", userAlias = "u")
public List<Project> selectReviewList(Project project)
```

在 MyBatis XML 中注入数据权限 SQL：
```xml
<select id="selectReviewList">
    SELECT p.*, d.dept_name, u1.nick_name as project_manager_name
    FROM pm_project p
    LEFT JOIN sys_dept d ON p.project_dept = d.dept_id
    LEFT JOIN sys_user u1 ON p.project_manager_id = u1.user_id
    WHERE p.approval_status = '0'
    ${params.dataScope}
    <!-- 其他查询条件 -->
</select>
```

### 6.3 菜单权限

**权限标识：**
- `project:review:list` - 查看列表
- `project:review:query` - 查看详情
- `project:review:approve` - 审核操作

**菜单配置：**
- 菜单名称：立项审核
- 父菜单：项目管理
- 路由地址：`review`
- 组件路径：`project/review/index`
- 权限标识：`project:review:list`

---

## 7. 邮件通知

### 7.1 邮件服务设计

复用 `ruoyi-common` 模块中的邮件工具类。

### 7.2 邮件内容模板

**主题：** `【立项审核通知】您的项目《{项目名称}》审核{结果}`

**内容：**
```
尊敬的 {创建人姓名}：

您提交的项目立项申请已完成审核，详情如下：

项目名称：{项目名称}
项目编号：{项目编号}
审核结果：{通过/拒绝}
审核意见：{审核意见}
审核人：{审核人姓名}
审核时间：{审核时间}

请登录系统查看详情。
```

### 7.3 异步发送实现

- 使用 `@Async` 注解实现异步发送
- 邮件发送失败不影响审核流程，只记录错误日志
- 获取项目创建人的邮箱地址（从 sys_user 表）

### 7.4 容错处理

- 创建人邮箱为空：跳过邮件发送，记录日志
- 邮件服务异常：捕获异常，记录日志，不抛出到上层

---

## 8. 数据库设计

### 8.1 复用现有表

**pm_project 表：**
- `approval_status`：审批状态（0待审核/1已通过/2已拒绝）
- `approval_reason`：审批意见
- `approval_time`：审批时间
- `approver_id`：审批人

**pm_project_approval 表：**
- 记录每次审核的历史记录
- 字段：approval_id、project_id、approval_status、approval_reason、approver_id、approval_time 等

### 8.2 数据流转

1. 项目创建时，`approval_status = '0'`（待审核）
2. 审核通过时，`approval_status = '1'`，同时更新 approval_reason、approval_time、approver_id
3. 审核拒绝时，`approval_status = '2'`，同时更新 approval_reason、approval_time、approver_id
4. 每次审核都在 pm_project_approval 表插入一条记录

---

## 9. 实施计划

### 9.1 后端开发
1. 创建 `ProjectReviewController`
2. 创建 `IProjectReviewService` 和实现类
3. 在 `ProjectMapper.xml` 中添加查询 SQL
4. 实现邮件通知服务
5. 添加单元测试

### 9.2 前端开发
1. 创建 `review/index.vue` 页面
2. 创建 `api/project/review.js` API 文件
3. 实现查询表单、列表、审核对话框
4. 集成字典、部门树、用户选择器等组件

### 9.3 菜单配置
1. 在 `pm-sql/init/02_menu_data.sql` 中添加菜单 SQL
2. 配置菜单权限和路由

### 9.4 测试
1. 功能测试：查询、审核、权限控制
2. 权限测试：岗位权限、部门数据权限
3. 邮件测试：邮件发送、异常处理
4. E2E 测试：完整的审核流程

---

## 10. 注意事项

1. **权限控制**：确保只有 `tdfzr` 岗位的用户可以审核，且只能审核自己部门的项目
2. **数据一致性**：审核操作需要同时更新项目表和审核记录表，使用事务保证一致性
3. **邮件异步**：邮件发送使用异步方式，避免阻塞审核流程
4. **前端校验**：拒绝时审核意见必填，前端需要做表单校验
5. **列表刷新**：审核后刷新列表，已审核项目自动消失（因为不再是待审核状态）
6. **错误提示**：提供友好的错误提示信息，帮助用户理解问题

---

## 11. 扩展性考虑

1. **多级审核**：当前设计支持单级审核，如需多级审核，可扩展审核流程
2. **审核历史**：pm_project_approval 表记录了所有审核历史，可用于审核记录查询
3. **通知方式**：当前只支持邮件通知，后续可扩展站内消息、短信等通知方式
4. **批量审核**：当前设计为单个审核，如需批量审核，可扩展批量操作接口

---

**设计完成日期：** 2026-02-08
**设计人：** Claude Sonnet 4.5
