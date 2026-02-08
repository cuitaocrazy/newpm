# 立项审核功能实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现立项审核工作台，让团队负责人审核待审核的项目立项申请，支持通过/拒绝操作并邮件通知项目创建人。

**Architecture:**
- 后端：新增 ProjectReviewController 和 Service，复用 ProjectMapper，实现岗位权限校验和部门数据权限过滤
- 前端：新增 review/index.vue 页面，包含查询表单、列表展示、审核对话框
- 邮件：异步发送审核结果通知，不阻塞审核流程

**Tech Stack:**
- 后端：Spring Boot 3.5.8, MyBatis, Spring Security, @DataScope 数据权限
- 前端：Vue 3.5, TypeScript 5.6, Element Plus 2.13, Pinia
- 邮件：Spring Mail, @Async 异步发送

---

## Task 1: 后端 - Service 接口定义

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/service/IProjectReviewService.java`

**Step 1: 创建 Service 接口**

创建 `IProjectReviewService.java` 文件，定义审核相关的业务方法。

```java
package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.Project;

/**
 * 项目审核Service接口
 *
 * @author ruoyi
 * @date 2026-02-08
 */
public interface IProjectReviewService
{
    /**
     * 查询待审核项目列表
     *
     * @param project 项目查询条件
     * @return 待审核项目列表
     */
    public List<Project> selectReviewList(Project project);

    /**
     * 查询项目详细信息（用于审核）
     *
     * @param projectId 项目ID
     * @return 项目详细信息
     */
    public Project selectProjectById(Long projectId);

    /**
     * 审核项目
     *
     * @param projectId 项目ID
     * @param approvalStatus 审核状态（1通过/2拒绝）
     * @param approvalReason 审核意见
     * @return 结果
     */
    public int approveProject(Long projectId, String approvalStatus, String approvalReason);
}
```

**Step 2: 提交代码**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/IProjectReviewService.java
git commit -m "feat(review): 添加项目审核 Service 接口定义"
```

---

## Task 2: 后端 - Mapper SQL 查询

**Files:**
- Modify: `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml`

**Step 1: 添加查询待审核项目列表的 SQL**

在 `ProjectMapper.xml` 的 `</mapper>` 标签前添加以下 SQL：

```xml
<!-- 查询待审核项目列表 -->
<select id="selectReviewList" parameterType="Project" resultMap="ProjectResult">
    SELECT
        p.project_id,
        p.project_code,
        p.project_name,
        p.project_full_name,
        p.industry,
        p.region,
        p.province_id,
        p.short_name,
        p.established_year,
        p.project_category,
        p.project_dept,
        p.project_status,
        p.acceptance_status,
        p.approval_status,
        p.approval_reason,
        p.approval_time,
        p.approver_id,
        p.project_manager_id,
        p.market_manager_id,
        p.create_time,
        d.dept_name,
        u1.nick_name as project_manager_name,
        u2.nick_name as market_manager_name,
        pr.province_name
    FROM pm_project p
    LEFT JOIN sys_dept d ON p.project_dept = d.dept_id
    LEFT JOIN sys_user u1 ON p.project_manager_id = u1.user_id
    LEFT JOIN sys_user u2 ON p.market_manager_id = u2.user_id
    LEFT JOIN pm_province pr ON p.province_id = pr.province_id
    WHERE p.approval_status = '0' AND p.del_flag = '0'
    ${params.dataScope}
    <if test="projectName != null and projectName != ''">
        AND p.project_name LIKE CONCAT('%', #{projectName}, '%')
    </if>
    <if test="projectDept != null and projectDept != ''">
        AND p.project_dept = #{projectDept}
    </if>
    <if test="projectCategory != null and projectCategory != ''">
        AND p.project_category = #{projectCategory}
    </if>
    <if test="region != null and region != ''">
        AND p.region = #{region}
    </if>
    <if test="provinceId != null">
        AND p.province_id = #{provinceId}
    </if>
    <if test="projectManagerId != null">
        AND p.project_manager_id = #{projectManagerId}
    </if>
    <if test="marketManagerId != null">
        AND p.market_manager_id = #{marketManagerId}
    </if>
    <if test="projectStatus != null and projectStatus != ''">
        AND p.project_status = #{projectStatus}
    </if>
    ORDER BY p.create_time DESC
</select>
```

**Step 2: 在 ProjectMapper 接口中添加方法声明**

修改 `ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java`，添加方法：

```java
/**
 * 查询待审核项目列表
 *
 * @param project 项目查询条件
 * @return 待审核项目列表
 */
public List<Project> selectReviewList(Project project);
```

**Step 3: 提交代码**

```bash
git add ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java
git commit -m "feat(review): 添加查询待审核项目列表的 SQL"
```

---

## Task 3: 后端 - Service 实现类

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectReviewServiceImpl.java`

**Step 1: 创建 Service 实现类**

创建完整的 Service 实现类，包含查询列表、查询详情、审核项目三个核心方法。

**Step 2: 提交代码**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectReviewServiceImpl.java
git commit -m "feat(review): 实现项目审核 Service 业务逻辑"
```

---

## Task 4: 后端 - Controller 控制器

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectReviewController.java`

**Step 1: 创建 Controller 类**

创建 REST API 控制器，提供三个接口：查询列表、查询详情、审核操作。

**Step 2: 提交代码**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectReviewController.java
git commit -m "feat(review): 添加项目审核 Controller 接口"
```

---

## Task 5: 后端 - 邮件通知服务

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/service/IProjectEmailService.java`
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectEmailServiceImpl.java`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectReviewServiceImpl.java`

**Step 1: 创建邮件服务接口和实现类**

实现异步邮件发送功能，使用 @Async 注解，不阻塞审核流程。

**Step 2: 在 Service 中集成邮件服务**

在 `ProjectReviewServiceImpl` 的 `approveProject` 方法中调用邮件服务。

**Step 3: 提交代码**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/IProjectEmailService.java ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectEmailServiceImpl.java ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectReviewServiceImpl.java
git commit -m "feat(review): 实现审核结果邮件通知功能"
```

---

## Task 6: 后端 - 编译测试

**Step 1: 编译后端代码**

```bash
cd /Users/kongli/ws-claude/PM/newpm
mvn clean compile -Dmaven.test.skip=true
```

预期：编译成功，无错误。

**Step 2: 如果有编译错误，修复后重新编译**

常见问题：
- 导入缺失：检查 import 语句
- 方法不存在：检查 Mapper 接口是否声明了方法
- 类型不匹配：检查参数类型

**Step 3: 提交修复（如果有）**

```bash
git add .
git commit -m "fix(review): 修复编译错误"
```

---

## Task 7: 前端 - API 接口文件

**Files:**
- Create: `ruoyi-ui/src/api/project/review.js`

**Step 1: 创建 API 文件**

创建 `review.js` 文件：

```javascript
import request from '@/utils/request'

// 查询待审核项目列表
export function listReview(query) {
  return request({
    url: '/project/review/list',
    method: 'get',
    params: query
  })
}

// 查询项目详细信息
export function getReview(projectId) {
  return request({
    url: '/project/review/' + projectId,
    method: 'get'
  })
}

// 审核项目
export function approveProject(data) {
  return request({
    url: '/project/review/approve',
    method: 'post',
    data: data
  })
}
```

**Step 2: 提交代码**

```bash
git add ruoyi-ui/src/api/project/review.js
git commit -m "feat(review): 添加项目审核 API 接口"
```

---

## Task 8: 前端 - 页面骨架（查询表单）

**Files:**
- Create: `ruoyi-ui/src/views/project/review/index.vue`

**Step 1: 创建页面文件，实现查询表单部分**

创建 `index.vue` 文件，先实现查询表单区域（约150行）：

```vue
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="项目名称" prop="projectName">
        <el-input
          v-model="queryParams.projectName"
          placeholder="请输入项目名称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="项目部门" prop="projectDept">
        <el-tree-select
          v-model="queryParams.projectDept"
          :data="deptOptions"
          :props="{ value: 'id', label: 'label', children: 'children' }"
          value-key="id"
          placeholder="请选择项目部门"
          check-strictly
          clearable
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="项目分类" prop="projectCategory">
        <el-select v-model="queryParams.projectCategory" placeholder="请选择项目分类" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_xmfl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="一级区域" prop="region">
        <el-select v-model="queryParams.region" placeholder="请选择一级区域" clearable @change="handleRegionChange" style="width: 200px">
          <el-option
            v-for="dict in sys_yjqy"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="二级区域" prop="provinceId">
        <el-select v-model="queryParams.provinceId" placeholder="请选择二级区域" clearable :disabled="!queryParams.region" style="width: 200px">
          <el-option
            v-for="item in secondaryRegionOptions"
            :key="item.provinceId"
            :label="item.provinceName"
            :value="item.provinceId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目经理" prop="projectManagerId">
        <el-select v-model="queryParams.projectManagerId" placeholder="请选择项目经理" clearable filterable style="width: 200px">
          <el-option
            v-for="user in projectManagerOptions"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="市场经理" prop="marketManagerId">
        <el-select v-model="queryParams.marketManagerId" placeholder="请选择市场经理" clearable filterable style="width: 200px">
          <el-option
            v-for="user in marketManagerOptions"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目阶段" prop="projectStatus">
        <el-select v-model="queryParams.projectStatus" placeholder="请选择项目阶段" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_xmjd"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态" prop="approvalStatus">
        <el-select v-model="queryParams.approvalStatus" placeholder="请选择审核状态" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_shzt"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 列表和对话框将在后续步骤添加 -->
  </div>
</template>

<script setup name="Review">
import { listReview, getReview, approveProject } from "@/api/project/review"
import { listSecondaryRegion } from "@/api/project/secondaryRegion"
import { listUserByPost } from "@/api/system/user"
import { deptTreeSelect } from "@/api/system/dept"

const { proxy } = getCurrentInstance()
const { sys_xmfl, sys_yjqy, sys_xmjd, sys_shzt } = proxy.useDict('sys_xmfl', 'sys_yjqy', 'sys_xmjd', 'sys_shzt')

const reviewList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const deptOptions = ref([])
const secondaryRegionOptions = ref([])
const projectManagerOptions = ref([])
const marketManagerOptions = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    projectDept: null,
    projectCategory: null,
    region: null,
    provinceId: null,
    projectManagerId: null,
    marketManagerId: null,
    projectStatus: null,
    approvalStatus: '0' // 默认查询待审核
  }
})

const { queryParams } = toRefs(data)

/** 查询列表 */
function getList() {
  loading.value = true
  listReview(queryParams.value).then(response => {
    reviewList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  queryParams.value.approvalStatus = '0' // 重置后仍然默认查询待审核
  handleQuery()
}

/** 一级区域变化 */
function handleRegionChange(value) {
  queryParams.value.provinceId = null
  secondaryRegionOptions.value = []
  if (value) {
    listSecondaryRegion({ regionCode: value }).then(response => {
      secondaryRegionOptions.value = response.rows
    })
  }
}

/** 查询部门树 */
function getDeptTree() {
  deptTreeSelect().then(response => {
    deptOptions.value = response.data
  })
}

/** 查询用户列表 */
function getUserOptions() {
  // 查询项目经理（岗位代码：xmjl）
  listUserByPost({ postCode: 'xmjl' }).then(response => {
    projectManagerOptions.value = response.data || []
  })
  // 查询市场经理（岗位代码：scjl）
  listUserByPost({ postCode: 'scjl' }).then(response => {
    marketManagerOptions.value = response.data || []
  })
}

// 初始化
getDeptTree()
getUserOptions()
getList()
</script>
```

**Step 2: 提交代码**

```bash
git add ruoyi-ui/src/views/project/review/index.vue
git commit -m "feat(review): 添加立项审核页面骨架和查询表单"
```

---

## Task 9: 前端 - 列表展示

**Files:**
- Modify: `ruoyi-ui/src/views/project/review/index.vue`

**Step 1: 在查询表单后添加列表区域**

在 `<!-- 列表和对话框将在后续步骤添加 -->` 注释位置添加列表代码。

**Step 2: 提交代码**

```bash
git add ruoyi-ui/src/views/project/review/index.vue
git commit -m "feat(review): 添加项目列表展示"
```

---

## Task 10: 前端 - 审核对话框

**Files:**
- Modify: `ruoyi-ui/src/views/project/review/index.vue`

**Step 1: 添加审核对话框**

在列表后添加审核对话框，使用 el-collapse 展示项目详情，分7个模块折叠。

**Step 2: 添加审核相关方法**

添加 `handleReview`、`handleApprove`、`handleReject` 等方法。

**Step 3: 提交代码**

```bash
git add ruoyi-ui/src/views/project/review/index.vue
git commit -m "feat(review): 添加审核对话框和审核操作"
```

---

## Task 11: 菜单配置 - SQL 脚本

**Files:**
- Modify: `pm-sql/init/02_menu_data.sql`

**Step 1: 添加菜单 SQL**

在文件末尾添加立项审核菜单配置：

```sql
-- 立项审核菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '立项审核', menu_id, 3, 'review', 'project/review/index', 1, 0, 'C', '0', '0', 'project:review:list', 'edit', 'admin', sysdate(), '', null, '立项审核菜单'
FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0;

-- 立项审核按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '项目审核查询', menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'project:review:query', '#', 'admin', sysdate(), '', null, ''
FROM sys_menu WHERE menu_name = '立项审核' AND menu_type = 'C';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '项目审核操作', menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'project:review:approve', '#', 'admin', sysdate(), '', null, ''
FROM sys_menu WHERE menu_name = '立项审核' AND menu_type = 'C';
```

**Step 2: 提交代码**

```bash
git add pm-sql/init/02_menu_data.sql
git commit -m "feat(review): 添加立项审核菜单配置"
```

---

## Task 12: 集成测试 - 启动服务

**Step 1: 启动后端服务**

```bash
cd /Users/kongli/ws-claude/PM/newpm
mvn clean package -Dmaven.test.skip=true
java -Xms256m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
```

预期：服务启动成功，无错误日志。

**Step 2: 启动前端服务**

```bash
cd /Users/kongli/ws-claude/PM/newpm/ruoyi-ui
npm run dev
```

预期：前端启动成功，访问 http://localhost:80

---

## Task 13: 功能测试

**Step 1: 执行菜单 SQL**

在 MySQL 中执行 `pm-sql/init/02_menu_data.sql` 中的立项审核菜单 SQL。

**Step 2: 分配菜单权限**

登录系统，进入角色管理，给测试角色分配"立项审核"菜单权限。

**Step 3: 测试查询功能**

- 访问立项审核页面
- 测试各个查询条件
- 验证列表数据正确显示

**Step 4: 测试审核功能**

- 点击"审核"按钮
- 查看项目详情是否完整
- 测试"通过"操作（审核意见可选）
- 测试"拒绝"操作（审核意见必填）
- 验证审核后列表刷新，已审核项目消失

**Step 5: 测试权限控制**

- 使用非 tdfzr 岗位用户登录，验证无法审核
- 使用 tdfzr 岗位用户登录，验证只能看到自己部门的项目

**Step 6: 测试邮件通知**

- 配置邮件服务（application.yml）
- 审核项目后，检查项目创建人是否收到邮件
- 验证邮件内容正确

---

## Task 14: 最终提交

**Step 1: 检查所有文件**

```bash
git status
```

确保所有修改都已提交。

**Step 2: 创建功能完成标记提交**

```bash
git commit --allow-empty -m "feat(review): 立项审核功能开发完成

功能清单：
- 后端：Service、Controller、Mapper SQL、邮件通知
- 前端：查询表单、列表展示、审核对话框
- 权限：岗位权限校验、部门数据权限
- 菜单：立项审核菜单配置

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## 实施注意事项

1. **DRY 原则**：复用现有的组件和工具类，不重复造轮子
2. **YAGNI 原则**：只实现需求中明确的功能，不添加额外功能
3. **TDD 原则**：虽然本项目未强制要求单元测试，但建议在开发过程中进行手动测试
4. **频繁提交**：每完成一个小任务就提交，保持提交历史清晰
5. **错误处理**：前后端都要有完善的错误处理和友好的提示信息
6. **代码风格**：遵循项目现有的代码风格和命名规范

---

## 验收标准

- [ ] 后端编译通过，无错误
- [ ] 前端编译通过，无错误
- [ ] 服务启动成功，无错误日志
- [ ] 查询功能正常，支持所有查询条件
- [ ] 列表展示正确，字段完整
- [ ] 审核对话框显示项目完整信息
- [ ] 审核通过功能正常，状态更新正确
- [ ] 审核拒绝功能正常，必填审核意见
- [ ] 审核后列表自动刷新，已审核项目消失
- [ ] 邮件通知发送成功，内容正确
- [ ] 岗位权限校验生效，非 tdfzr 用户无法审核
- [ ] 部门数据权限生效，只能看到自己部门的项目
- [ ] 菜单配置正确，权限控制生效

---

**计划创建日期：** 2026-02-08
**预计工时：** 4-6 小时
**计划人：** Claude Sonnet 4.5
