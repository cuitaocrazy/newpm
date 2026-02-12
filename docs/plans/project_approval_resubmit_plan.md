# 项目审核"原地修改重新提交"改造方案

## 📋 需求概述

**业务需求：** 实现审核拒绝后允许申请人修改项目信息并重新提交审核的功能

**核心流程：**
- 审核通过：项目状态变为"已通过"（1），审核意见可选
- 审核拒绝：项目状态变为"已拒绝"（2），拒绝原因必填
- 重新提交：编辑已拒绝的项目后，点击"重新提交审核"，状态自动变为"待审核"（0）

**方案选择：** 方案A - 单按钮模式（根据审核状态动态显示按钮）

---

## 🎯 改造目标

1. ✅ 保留完整的审核历史记录（pm_project_approval表）
2. ✅ 支持多次审核循环（拒绝→修改→重新提交→审核）
3. ✅ 编辑页面按钮根据审核状态智能切换
4. ✅ 审核通过后的项目仍可编辑，但不触发重新审核

---

## 📊 数据表结构分析

### 现有表结构（无需修改）

#### pm_project（项目表）
```sql
approval_status VARCHAR(20) DEFAULT '0' COMMENT '审核状态(0待审核/1已通过/2已拒绝)'
approval_reason VARCHAR(500) COMMENT '审核意见'
approval_time DATETIME COMMENT '审核时间'
approver_id VARCHAR(64) COMMENT '审核人ID'
```

#### pm_project_approval（审核历史表）
```sql
approval_id BIGINT PRIMARY KEY AUTO_INCREMENT
project_id BIGINT NOT NULL COMMENT '项目ID'
approval_status VARCHAR(50) NOT NULL DEFAULT '待审核' COMMENT '审核状态(0-待审核、1-通过、2-不通过)'
approval_reason TEXT COMMENT '审核原因/意见'
approver_id BIGINT COMMENT '审核人ID'
approval_time DATETIME COMMENT '审核时间'
create_time DATETIME COMMENT '创建时间'
```

**结论：** 现有表结构已满足需求，无需修改DDL

---

## 🔧 技术实现方案

### 一、后端改造

#### 1.1 Service层新增方法

**文件：** `IProjectApprovalService.java`

```java
/**
 * 审核项目（通过/拒绝）
 * @param projectId 项目ID
 * @param approvalStatus 审核状态（1-通过/2-拒绝）
 * @param approvalReason 审核意见（拒绝时必填）
 * @return 结果
 */
int approveProject(Long projectId, String approvalStatus, String approvalReason);

/**
 * 查询项目的审核历史
 * @param projectId 项目ID
 * @return 审核历史列表（按时间倒序）
 */
List<ProjectApproval> selectApprovalHistory(Long projectId);
```

**实现要点：**
- 使用 `@Transactional` 保证原子性
- 同时更新 `pm_project` 和 `pm_project_approval` 两张表
- 审核人ID自动获取当前登录用户（`SecurityUtils.getUserId()`）
- 审核时间自动设置为当前时间

#### 1.2 Controller层新增接口

**文件：** `ProjectApprovalController.java`

```java
/**
 * 审核项目
 * POST /project/approval/approve
 * 请求参数：{ projectId: 1, approvalStatus: "1", approvalReason: "审核通过" }
 */
@PreAuthorize("@ss.hasPermi('project:approval:approve')")
@Log(title = "项目审核", businessType = BusinessType.UPDATE)
@PostMapping("/approve")
public AjaxResult approve(@RequestBody Map<String, Object> params);

/**
 * 查询审核历史
 * GET /project/approval/history/{projectId}
 */
@PreAuthorize("@ss.hasPermi('project:approval:query')")
@GetMapping("/history/{projectId}")
public AjaxResult history(@PathVariable Long projectId);
```

**权限标识：**
- `project:approval:approve` - 审核权限（仅审核人角色）
- `project:approval:query` - 查看审核历史权限

#### 1.3 ProjectController修改逻辑

**文件：** `ProjectController.java`

**修改点：** `edit(@RequestBody Project project)` 方法

```java
@PutMapping
public AjaxResult edit(@RequestBody Project project)
{
    // 查询原项目状态
    Project oldProject = projectService.selectProjectByProjectId(project.getProjectId());

    // 如果原状态是"已拒绝"，修改后自动变为"待审核"并记录
    if ("2".equals(oldProject.getApprovalStatus())) {
        project.setApprovalStatus("0");

        // 新增重新提交审核记录
        ProjectApproval resubmitRecord = new ProjectApproval();
        resubmitRecord.setProjectId(project.getProjectId());
        resubmitRecord.setApprovalStatus("0");
        resubmitRecord.setApprovalReason("重新提交审核");
        resubmitRecord.setApprovalTime(new Date());
        projectApprovalService.insertProjectApproval(resubmitRecord);
    }

    return toAjax(projectService.updateProject(project));
}
```

---

### 二、前端改造

#### 2.1 API层新增函数

**文件：** `ruoyi-ui/src/api/project/approval.js`

```javascript
// 审核项目
export function approveProject(data) {
  return request({
    url: '/project/approval/approve',
    method: 'post',
    data: data
  })
}

// 查询审核历史
export function getApprovalHistory(projectId) {
  return request({
    url: '/project/approval/history/' + projectId,
    method: 'get'
  })
}
```

#### 2.2 项目列表页改造

**文件：** `ruoyi-ui/src/views/project/project/index.vue`

**改造点1：操作列增加审核按钮**

```vue
<el-table-column label="操作" align="center" fixed="right" width="350">
  <template #default="scope">
    <template v-if="!scope.row.isSummaryRow">
      <!-- 原有按钮：详情、编辑 -->
      <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
      <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>

      <!-- 新增：待审核状态显示审核按钮 -->
      <el-button
        v-if="scope.row.approvalStatus === '0'"
        link
        type="success"
        icon="CircleCheck"
        @click="handleApprove(scope.row)"
        v-hasPermi="['project:approval:approve']"
      >审核</el-button>

      <!-- 新增：查看审核历史 -->
      <el-button
        link
        type="info"
        icon="Document"
        @click="handleHistory(scope.row)"
      >审核历史</el-button>

      <!-- 原有按钮：收入确认、删除 -->
      <el-button link type="success" icon="Money" @click="handleRevenue(scope.row)">收入确认</el-button>
      <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
    </template>
  </template>
</el-table-column>
```

**改造点2：新增审核对话框**

```vue
<!-- 审核对话框 -->
<el-dialog title="项目审核" v-model="approvalDialogVisible" width="600px">
  <el-form :model="approvalForm" :rules="approvalRules" ref="approvalFormRef" label-width="100px">
    <el-form-item label="项目名称">
      <el-input v-model="currentProject.projectName" disabled />
    </el-form-item>
    <el-form-item label="审核结果" prop="approvalStatus">
      <el-radio-group v-model="approvalForm.approvalStatus">
        <el-radio label="1">通过</el-radio>
        <el-radio label="2">拒绝</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="审核意见" prop="approvalReason">
      <el-input
        v-model="approvalForm.approvalReason"
        type="textarea"
        :rows="4"
        :placeholder="approvalForm.approvalStatus === '2' ? '请填写拒绝原因（必填）' : '审核意见（可选）'"
      />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="approvalDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="submitApproval">确定</el-button>
  </template>
</el-dialog>

<!-- 审核历史对话框 -->
<el-dialog title="审核历史" v-model="historyDialogVisible" width="800px">
  <el-timeline>
    <el-timeline-item
      v-for="item in approvalHistory"
      :key="item.approvalId"
      :timestamp="item.approvalTime"
      placement="top"
    >
      <el-card>
        <template #header>
          <span>审核结果：</span>
          <dict-tag :options="sys_spzt" :value="item.approvalStatus" />
        </template>
        <p>审核人：{{ item.approverName || item.approverId }}</p>
        <p>审核意见：{{ item.approvalReason || '无' }}</p>
      </el-card>
    </el-timeline-item>
  </el-timeline>
</el-dialog>
```

**改造点3：新增方法**

```javascript
// 审核按钮点击
function handleApprove(row) {
  currentProject.value = { ...row }
  approvalForm.value = {
    projectId: row.projectId,
    approvalStatus: '1',
    approvalReason: ''
  }
  approvalDialogVisible.value = true
}

// 提交审核
function submitApproval() {
  proxy.$refs.approvalFormRef.validate(valid => {
    if (valid) {
      // 拒绝时必须填写原因
      if (approvalForm.value.approvalStatus === '2' && !approvalForm.value.approvalReason) {
        proxy.$modal.msgWarning('拒绝时必须填写拒绝原因')
        return
      }

      approveProject(approvalForm.value).then(response => {
        proxy.$modal.msgSuccess('审核成功')
        approvalDialogVisible.value = false
        getList()
      })
    }
  })
}

// 查看审核历史
function handleHistory(row) {
  getApprovalHistory(row.projectId).then(response => {
    approvalHistory.value = response.data
    historyDialogVisible.value = true
  })
}
```

#### 2.3 项目编辑页面改造

**文件：** 需要先找到编辑页面路径（可能是 `ruoyi-ui/src/views/project/project/edit.vue` 或独立路由组件）

**改造点1：动态按钮文字和图标**

```vue
<template>
  <el-form>
    <!-- 表单内容 -->
  </el-form>

  <div class="form-footer">
    <!-- 动态按钮 -->
    <el-button
      type="primary"
      :icon="submitButtonIcon"
      @click="handleSubmit"
    >
      {{ submitButtonText }}
    </el-button>
    <el-button @click="handleCancel">取消</el-button>
  </div>
</template>

<script setup>
const submitButtonText = computed(() => {
  return form.value.approvalStatus === '2' ? '重新提交审核' : '保存'
})

const submitButtonIcon = computed(() => {
  return form.value.approvalStatus === '2' ? 'RefreshRight' : 'Select'
})

function handleSubmit() {
  proxy.$refs.formRef.validate(valid => {
    if (valid) {
      updateProject(form.value).then(response => {
        proxy.$modal.msgSuccess(
          form.value.approvalStatus === '2'
            ? '重新提交审核成功，等待审核人审批'
            : '保存成功'
        )
        router.back()
      })
    }
  })
}
</script>
```

**改造点2：拒绝状态提示**

```vue
<!-- 已拒绝状态显示提示信息 -->
<el-alert
  v-if="form.approvalStatus === '2'"
  title="审核已拒绝"
  type="warning"
  :closable="false"
  show-icon
  class="mb-4"
>
  <template #default>
    <p>拒绝原因：{{ form.approvalReason }}</p>
    <p>请根据审核意见修改后重新提交</p>
  </template>
</el-alert>
```

---

## 📝 实施计划

### 阶段一：后端开发（预计2小时）

#### Step 1: Service层改造（30分钟）
- [ ] 修改 `IProjectApprovalService.java` 添加接口方法
- [ ] 修改 `ProjectApprovalServiceImpl.java` 实现审核方法
- [ ] 添加 `@Transactional` 事务控制
- [ ] 单元测试验证

**涉及文件：**
- `ruoyi-project/src/main/java/com/ruoyi/project/service/IProjectApprovalService.java`
- `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectApprovalServiceImpl.java`

#### Step 2: Controller层改造（30分钟）
- [ ] 修改 `ProjectApprovalController.java` 添加审核接口
- [ ] 修改 `ProjectController.java` 完善编辑逻辑
- [ ] 配置权限标识

**涉及文件：**
- `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectApprovalController.java`
- `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java`

#### Step 3: 后端测试（30分钟）
- [ ] 使用Postman测试审核接口
- [ ] 验证事务回滚（审核失败时数据不变）
- [ ] 验证审核历史记录正确生成

#### Step 4: 权限配置（30分钟）
- [ ] 在 `pm-sql/init/02_menu_data.sql` 添加审核权限菜单
- [ ] 分配审核权限给相应角色

---

### 阶段二：前端开发（预计3小时）

#### Step 5: API层开发（15分钟）
- [ ] 修改 `ruoyi-ui/src/api/project/approval.js` 添加API函数

**涉及文件：**
- `ruoyi-ui/src/api/project/approval.js`

#### Step 6: 项目列表页改造（1.5小时）
- [ ] 添加审核按钮（待审核状态可见）
- [ ] 添加审核历史按钮
- [ ] 实现审核对话框组件
- [ ] 实现审核历史时间轴组件
- [ ] 添加表单验证规则（拒绝时必填原因）

**涉及文件：**
- `ruoyi-ui/src/views/project/project/index.vue`

#### Step 7: 项目编辑页改造（1小时）
- [ ] 找到编辑页面文件路径
- [ ] 实现动态按钮逻辑
- [ ] 添加拒绝状态提示组件
- [ ] 测试编辑后状态切换

**涉及文件：**
- 待确认（可能是独立的edit.vue或详情页的编辑模式）

#### Step 8: 前端测试（30分钟）
- [ ] 测试审核通过流程
- [ ] 测试审核拒绝流程
- [ ] 测试重新提交流程
- [ ] 测试审核历史查看
- [ ] 测试按钮权限控制

---

### 阶段三：联调测试（预计1小时）

#### Step 9: 完整流程测试
- [ ] **场景1：** 新建项目 → 提交 → 审核通过
- [ ] **场景2：** 新建项目 → 提交 → 审核拒绝 → 修改 → 重新提交 → 审核通过
- [ ] **场景3：** 已通过项目修改信息（不触发审核）
- [ ] **场景4：** 多次拒绝重新提交循环
- [ ] **场景5：** 审核历史记录完整性

#### Step 10: 数据库验证
- [ ] 检查 `pm_project` 表审核状态更新正确
- [ ] 检查 `pm_project_approval` 表历史记录完整
- [ ] 检查审核时间、审核人记录正确

---

## 🚨 风险点与注意事项

### 1. 事务一致性风险
**问题：** 审核时同时更新两张表，可能出现数据不一致

**解决方案：**
- 使用 `@Transactional` 注解确保原子性
- 捕获异常并回滚事务
- 添加日志记录异常情况

### 2. 权限控制风险
**问题：** 审核权限配置不当，普通用户可能审核自己的项目

**解决方案：**
- 严格权限标识配置（`project:approval:approve`）
- 后端验证审核人不能是申请人
- 角色分离：申请人角色 vs 审核人角色

### 3. 并发审核风险
**问题：** 两个审核人同时审核同一个项目

**解决方案：**
- 审核前检查当前状态是否为"待审核"
- 使用乐观锁（version字段）控制并发
- 提示"该项目已被他人审核"

### 4. 前端状态刷新风险
**问题：** 审核后列表状态未及时刷新

**解决方案：**
- 审核成功后调用 `getList()` 刷新列表
- 使用 `ElMessage` 提示操作结果

### 5. 审核历史排序问题
**问题：** 审核历史显示顺序混乱

**解决方案：**
- 查询时按 `create_time DESC` 排序
- 时间轴组件从上到下显示最新到最旧

---

## 📂 文件修改清单

### 后端文件（6个）

```
ruoyi-project/
├── src/main/java/com/ruoyi/project/
│   ├── controller/
│   │   ├── ProjectApprovalController.java         [修改] 新增审核接口
│   │   └── ProjectController.java                 [修改] 完善编辑逻辑
│   ├── service/
│   │   ├── IProjectApprovalService.java           [修改] 新增接口方法
│   │   └── impl/
│   │       └── ProjectApprovalServiceImpl.java    [修改] 实现审核方法

pm-sql/init/
└── 02_menu_data.sql                               [修改] 添加审核权限菜单
```

### 前端文件（2-3个）

```
ruoyi-ui/src/
├── api/project/
│   └── approval.js                                [修改] 新增API函数
├── views/project/
│   ├── project/
│   │   ├── index.vue                              [修改] 添加审核按钮和对话框
│   │   └── edit.vue (或detail.vue)                [修改] 动态按钮逻辑
```

---

## ✅ 验收标准

### 功能验收
- [ ] 审核通过后项目状态正确变为"已通过"
- [ ] 审核拒绝后项目状态正确变为"已拒绝"
- [ ] 已拒绝项目修改后自动变为"待审核"
- [ ] 审核历史记录完整且按时间倒序显示
- [ ] 审核意见在拒绝时必填，通过时可选
- [ ] 编辑页面按钮根据审核状态动态显示

### 性能验收
- [ ] 审核操作响应时间 < 500ms
- [ ] 审核历史查询响应时间 < 300ms
- [ ] 列表刷新时间 < 1s

### 安全验收
- [ ] 审核权限控制正常（无权限用户看不到审核按钮）
- [ ] 后端接口权限验证正常
- [ ] 申请人不能审核自己的项目

### 用户体验验收
- [ ] 操作提示信息清晰友好
- [ ] 拒绝状态显示明显的警告提示
- [ ] 审核历史时间轴直观易懂

---

## 📈 后续优化方向

### 1. 审核流程增强
- 支持多级审核（一审、二审）
- 审核委派功能
- 审核提醒通知（邮件/站内信）

### 2. 数据统计
- 审核通过率统计
- 审核时长统计
- 拒绝原因分类分析

### 3. 审核意见模板
- 常用审核意见快速选择
- 拒绝原因分类（预算不合理、信息不完整、不符合公司战略等）

---

## 📞 联系与支持

**开发人员：** Claude Code Assistant
**文档版本：** v1.0
**最后更新：** 2026-02-12

---

**附录：审核状态流转图**

```
┌─────────────┐
│  立项申请    │
└──────┬──────┘
       │ 提交
       ▼
┌─────────────┐      审核通过      ┌─────────────┐
│ 0-待审核     │ ───────────────▶  │ 1-已通过     │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │ 审核拒绝                          │ 可编辑
       ▼                                  │ 不触发审核
┌─────────────┐                          │
│ 2-已拒绝     │                          │
└──────┬──────┘                          │
       │                                  │
       │ 修改后重新提交                    │
       │                                  │
       └──────────────────────────────────┘
              (循环往复)
```
