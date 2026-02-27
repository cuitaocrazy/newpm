# Permission Codes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复各业务页面因跨 Controller 调用引发的 403 权限问题，使每个页面能正常加载所需数据。

**Architecture:** 分两层处理：① 业务模块间互调用 `hasAnyPermi` 在接口上列出所有合法调用方；② 系统层接口（user/dept）前端改调已有的 project 模块代理接口，避免 system 层被业务模块权限污染。

**Tech Stack:** Java 17 / Spring Boot 3 `@PreAuthorize` / Vue 3 + TypeScript / RuoYi PermissionService

---

## 数据格式差异（必读）

前端接口替换时，以下差异需注意：

| 替换方向 | 旧响应字段 | 新响应字段 |
|---|---|---|
| `listUser()` → `getUsersByPost()` | `res.rows` | `res.data` |
| `listUserByPost(code)` → `getUsersByPost(code)` | `res.data` | `res.data`（一致）|
| `listDept()` → `getDeptTree()` | `res.data` 全字段 | `res.data` 含 `{deptId,parentId,deptName}` |
| `deptTreeSelect()` → `getDeptTree()` | `res.data` 已是树结构 | `res.data` 是平铺列表，需 handleTree |

---

## Task 1: AttachmentController — 5 个接口权限

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/AttachmentController.java`

**涉及页面调用关系：**
- `attachment:list` → 被项目详情（`project:project:query`）、合同列表（`project:contract:list`）、合同详情（`project:contract:query`）调用
- `attachment:download` → 同上
- `attachment:add` → 被合同列表内嵌上传（`project:contract:list`）调用
- `attachment:remove` → 被合同列表内嵌删除（`project:contract:list`）调用
- `attachment:log` → 被合同列表查看日志（`project:contract:list`）调用

**Step 1: 修改 5 个 @PreAuthorize 注解**

找到以下注解并替换（共 5 处）：

```java
// list — 第 39 行附近
// 原：
@PreAuthorize("@ss.hasPermi('project:attachment:list')")
// 改为：
@PreAuthorize("@ss.hasAnyPermi('project:attachment:list,project:project:query,project:contract:list,project:contract:query')")

// query (单条) — 第 52 行附近
// 原：
@PreAuthorize("@ss.hasPermi('project:attachment:query')")
// 改为：
@PreAuthorize("@ss.hasAnyPermi('project:attachment:query,project:project:query,project:contract:query')")

// add (上传) — 第 62 行附近
// 原：
@PreAuthorize("@ss.hasPermi('project:attachment:add')")
// 改为：
@PreAuthorize("@ss.hasAnyPermi('project:attachment:add,project:contract:list')")

// download — 第 78 行附近
// 原：
@PreAuthorize("@ss.hasPermi('project:attachment:download')")
// 改为：
@PreAuthorize("@ss.hasAnyPermi('project:attachment:download,project:project:query,project:contract:list,project:contract:query')")

// remove — 第 89 行附近
// 原：
@PreAuthorize("@ss.hasPermi('project:attachment:remove')")
// 改为：
@PreAuthorize("@ss.hasAnyPermi('project:attachment:remove,project:contract:list')")

// log — 第 100 行附近
// 原：
@PreAuthorize("@ss.hasPermi('project:attachment:log')")
// 改为：
@PreAuthorize("@ss.hasAnyPermi('project:attachment:log,project:contract:list')")
```

**Step 2: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/AttachmentController.java
git commit -m "feat(permission): 附件接口支持合同/项目权限码访问"
```

---

## Task 2: PaymentController — list 接口权限

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/PaymentController.java`

**涉及页面调用关系：**
- `payment:list` → 被项目详情（`project:project:query`）、合同详情（`project:contract:query`）调用

**Step 1: 修改 list 接口 @PreAuthorize**

```java
// 第 45 行附近
// 原：
@PreAuthorize("@ss.hasPermi('project:payment:list')")
@GetMapping("/list")
// 改为：
@PreAuthorize("@ss.hasAnyPermi('project:payment:list,project:project:query,project:contract:query')")
@GetMapping("/list")
```

**Step 2: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/PaymentController.java
git commit -m "feat(permission): 付款列表支持项目/合同详情权限码访问"
```

---

## Task 3: CustomerController — listAll 接口权限

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/CustomerController.java`

**涉及页面调用关系：**
- `/project/customer/listAll` → 被合同列表、合同详情、合同新增、合同编辑、付款列表调用

**Step 1: 修改 listAll 接口 @PreAuthorize**

```java
// 第 136 行附近
// 原：
@PreAuthorize("@ss.hasPermi('project:customer:list')")
@GetMapping("/listAll")
// 改为：
@PreAuthorize("@ss.hasAnyPermi('project:customer:list,project:contract:list,project:contract:add,project:contract:edit,project:contract:query,project:payment:list')")
@GetMapping("/listAll")
```

**Step 2: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/CustomerController.java
git commit -m "feat(permission): 客户下拉列表支持合同/付款权限码访问"
```

---

## Task 4: ProjectController — 支撑接口权限扩展

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java`

**需修改的 6 个接口：**

| 接口 | 新增消费方权限 |
|---|---|
| `GET /list` | `project:contract:query`（合同详情选项） |
| `GET /deptTree` | `project:payment:list,revenue:company:list,revenue:team:query` |
| `GET /users` | `project:project:query,project:contract:list,revenue:team:list` |
| `GET /listByDept` | `project:contract:add,project:contract:edit` |
| `GET /search` | `project:contract:list` |
| `GET /customers` | `project:payment:list` |

**Step 1: 修改 6 处注解**

```java
// list — 第 51 行附近
@PreAuthorize("@ss.hasAnyPermi('project:project:list,project:contract:query')")

// deptTree — 第 159 行附近（原 project:project:list）
@PreAuthorize("@ss.hasAnyPermi('project:project:list,project:payment:list,revenue:company:list,revenue:team:query')")

// users — 第 148 行附近
@PreAuthorize("@ss.hasAnyPermi('project:project:list,project:project:query,project:contract:list,revenue:team:list')")

// listByDept — 第 191 行附近（原 project:project:add）
// 注意：listByDept 被合同新增/编辑调用，原权限是 project:project:add，改为：
@PreAuthorize("@ss.hasAnyPermi('project:project:add,project:contract:add,project:contract:edit')")

// search — 第 318 行附近
@PreAuthorize("@ss.hasAnyPermi('project:project:list,project:contract:list')")

// customers — 第 170 行附近
@PreAuthorize("@ss.hasAnyPermi('project:project:list,project:payment:list')")
```

**Step 2: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java
git commit -m "feat(permission): 项目支撑接口扩展多模块权限码"
```

---

## Task 5: ProjectApprovalController — history 接口权限

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectApprovalController.java`

**涉及页面调用关系：**
- `GET /history/{projectId}` → 被项目列表页（`project:project:list`）调用查看审核记录

**Step 1: 修改 history 接口 @PreAuthorize**

```java
// 第 129 行附近
// 原：
@PreAuthorize("@ss.hasPermi('project:approval:query')")
@GetMapping("/history/{projectId}")
// 改为：
@PreAuthorize("@ss.hasAnyPermi('project:approval:query,project:project:list,project:project:query')")
@GetMapping("/history/{projectId}")
```

**Step 2: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectApprovalController.java
git commit -m "feat(permission): 审核历史接口支持项目列表权限码访问"
```

---

## Task 6: 前端 — 统一用户数据来源（5 个文件）

**目标：** 将 `/system/user/list` 和 `/system/user/listByPost` 替换为 `/project/project/users`

**数据格式差异：**
- 旧：`listUser().then(res => res.rows || [])`
- 新：`getUsersByPost().then(res => res.data || [])`

**Files:**
- Modify: `ruoyi-ui/src/views/project/project/index.vue`
- Modify: `ruoyi-ui/src/views/project/project/detail.vue`
- Modify: `ruoyi-ui/src/views/project/project/edit.vue`
- Modify: `ruoyi-ui/src/views/revenue/team/detail.vue`
- Modify: `ruoyi-ui/src/views/project/customer/index.vue`

**Step 1: project/index.vue**

```typescript
// 修改 import（第 357 行）
// 原：
import { listUser } from "@/api/system/user"
// 改为（删除该行，改从 project/project 引入）：
import { listProject, delProject, getDeptTree, getUsersByPost } from "@/api/project/project"

// 修改调用（第 717 行附近）
// 原：
listUser({ pageNum: 1, pageSize: 1000 }).then(res => {
  allUsersList.value = res.rows || []
})
// 改为：
getUsersByPost().then(res => {
  allUsersList.value = res.data || []
})
```

**Step 2: project/detail.vue**

```typescript
// 修改 import（第 372 行）
// 原：
import { listUser } from '@/api/system/user'
// 改为：
import { getUsersByPost } from '@/api/project/project'

// 修改调用（第 472 行附近）
// 原：
listUser({ pageNum: 1, pageSize: 1000 }).then(response => {
  allUsers.value = response.rows || []
})
// 改为：
getUsersByPost().then(response => {
  allUsers.value = response.data || []
})
```

**Step 3: project/edit.vue**

```typescript
// 修改 import（第 482-483 行）
// 原：
import { listUser, listUserByPost } from '@/api/system/user'
import { listDept } from '@/api/system/dept'   // 此行直接删除（import 存在但未使用）
// 改为：
import { getUsersByPost } from '@/api/project/project'
// （listDept 一并删除，因为该文件只 import 未调用）

// 修改 listUser 调用（第 638 行附近）
// 原：
listUser({}).then(response => {
  allUsers.value = response.rows || []
})
// 改为：
getUsersByPost().then(response => {
  allUsers.value = response.data || []
})

// 修改 listUserByPost 调用（loadSalesManagers 函数）
// 原：
listUserByPost('xsfzr').then(response => {
  salesManagerList.value = response.data
})
// 改为：
getUsersByPost('xsfzr').then(response => {
  salesManagerList.value = response.data
})
```

**Step 4: revenue/team/detail.vue**

```typescript
// 修改 import（第 184 行）
// 原：
import { listUser } from '@/api/system/user'
// 改为：
import { getUsersByPost } from '@/api/project/project'

// 修改调用（第 223 行附近）
// 原：
listUser({}).then(response => {
  allUsers.value = response.rows || []
})
// 改为：
getUsersByPost().then(response => {
  allUsers.value = response.data || []
})
```

**Step 5: customer/index.vue**

```typescript
// 修改 import（第 277 行）
// 原：
import { listUserByPost } from "@/api/system/user"
// 改为：
import { getUsersByPost } from "@/api/project/project"

// 修改调用（第 529 行附近）
// 原：
listUserByPost('xsfzr').then(response => {
  salesManagerList.value = response.data
})
// 改为：
getUsersByPost('xsfzr').then(response => {
  salesManagerList.value = response.data
})
```

**Step 6: Commit**

```bash
git add ruoyi-ui/src/views/project/project/index.vue \
        ruoyi-ui/src/views/project/project/detail.vue \
        ruoyi-ui/src/views/project/project/edit.vue \
        ruoyi-ui/src/views/revenue/team/detail.vue \
        ruoyi-ui/src/views/project/customer/index.vue
git commit -m "feat(permission): 前端用户接口统一改调项目模块代理"
```

---

## Task 7: 前端 — 统一部门数据来源（3 个文件）

**目标：** 将 `/system/dept/list` 和 `/system/user/deptTree` 替换为 `/project/project/deptTree`

**数据格式说明：**
- `getDeptTree()` 返回平铺列表：`[{deptId, parentId, deptName, ancestors, orderNum}]`
- 需要用 `handleTree(data, 'deptId', 'parentId', 'children')` 构建树
- 如组件需要 `id`/`label` 字段，还需 map 转换

**Files:**
- Modify: `ruoyi-ui/src/views/project/contract/add.vue`
- Modify: `ruoyi-ui/src/views/project/contract/edit.vue`
- Modify: `ruoyi-ui/src/views/project/contract/detail.vue`

**Step 1: contract/add.vue**

```typescript
// 修改 import（第 275 行）
// 原：
import { listDept } from '@/api/system/dept'
// 改为（getDeptTree 从 project/project 引入）：
import { listProjectByDept, getProject, getDeptTree as getProjectDeptTree } from '@/api/project/project'
// 注意：用别名 getProjectDeptTree 避免与本地函数 getDeptTree 同名

// 修改本地 getDeptTree 函数（第 366 行附近）
// 原：
function getDeptTree() {
  listDept().then(response => {
    const deptData = response.data.map(dept => ({
      ...dept,
      id: dept.deptId,
      label: dept.deptName
    }))
    deptOptions.value = proxy.handleTree(deptData, "id")
  })
}
// 改为：
function getDeptTree() {
  getProjectDeptTree().then(response => {
    const deptData = response.data.map(dept => ({
      ...dept,
      id: dept.deptId,
      label: dept.deptName
    }))
    deptOptions.value = proxy.handleTree(deptData, "id")
  })
}
```

**Step 2: contract/edit.vue**（与 add.vue 完全一致的改法）

```typescript
// 修改 import（第 292 行）
// 原：
import { listDept } from '@/api/system/dept'
// 改为：
import { getContract, updateContract } from '@/api/project/contract'
import { listProjectByDept } from '@/api/project/project'
import { listAllCustomer } from '@/api/project/customer'
import { getDeptTree as getProjectDeptTree } from '@/api/project/project'

// 修改本地 getDeptTree 函数（第 358 行附近），与 add.vue 相同
function getDeptTree() {
  getProjectDeptTree().then(response => {
    const deptData = response.data.map(dept => ({
      ...dept,
      id: dept.deptId,
      label: dept.deptName
    }))
    deptOptions.value = proxy.handleTree(deptData, "id")
  })
}
```

**Step 3: contract/detail.vue**

```typescript
// 修改 import（第 247 行）
// 原：
import { deptTreeSelect } from "@/api/system/user"
// 改为：
import { getDeptTree as getProjectDeptTree } from "@/api/project/project"

// 修改本地 getDeptTree 函数（第 292 行附近）
// 原：
function getDeptTree() {
  deptTreeSelect().then(response => {
    deptOptions.value = response.data   // 旧接口返回已构建好的树
  })
}
// 改为：
function getDeptTree() {
  getProjectDeptTree().then(response => {
    const deptData = response.data.map((dept: any) => ({
      ...dept,
      id: dept.deptId,
      label: dept.deptName
    }))
    deptOptions.value = proxy.handleTree(deptData, "id")  // 新接口返回平铺列表，需手动构建
  })
}
```

**Step 4: Commit**

```bash
git add ruoyi-ui/src/views/project/contract/add.vue \
        ruoyi-ui/src/views/project/contract/edit.vue \
        ruoyi-ui/src/views/project/contract/detail.vue
git commit -m "feat(permission): 合同前端部门接口统一改调项目模块代理"
```

---

## 验证清单

所有 Task 完成后，启动前后端，按以下场景验证：

### 后端权限验证（使用一个只有合同权限的测试账号）

- [ ] 项目详情页 → 附件 Tab 正常加载
- [ ] 项目详情页 → 里程碑/付款 Tab 正常加载
- [ ] 合同列表页 → 附件上传/下载/删除/日志正常
- [ ] 合同列表页 → 项目搜索下拉正常
- [ ] 合同列表页 → 客户筛选下拉正常
- [ ] 合同详情页 → 付款列表正常加载
- [ ] 合同详情页 → 附件正常加载
- [ ] 付款列表页 → 客户下拉正常
- [ ] 项目列表页 → 审核历史弹窗正常

### 前端数据格式验证

- [ ] 项目列表 → 参与人员下拉 → 数据正常（非空）
- [ ] 项目详情 → 成员显示正常
- [ ] 项目编辑 → 人员选择器正常，销售负责人按岗位筛选正常
- [ ] 合同新增 → 部门树正常展示
- [ ] 合同编辑 → 部门树正常展示
- [ ] 合同详情 → 部门树正常展示
- [ ] 客户列表 → 销售负责人下拉正常

---

## 文件变更汇总

| 文件 | 变更类型 |
|---|---|
| `ruoyi-project/.../AttachmentController.java` | 6 处 @PreAuthorize 注解 |
| `ruoyi-project/.../PaymentController.java` | 1 处 @PreAuthorize 注解 |
| `ruoyi-project/.../CustomerController.java` | 1 处 @PreAuthorize 注解 |
| `ruoyi-project/.../ProjectController.java` | 6 处 @PreAuthorize 注解 |
| `ruoyi-project/.../ProjectApprovalController.java` | 1 处 @PreAuthorize 注解 |
| `ruoyi-ui/.../project/project/index.vue` | import 替换 + 调用替换 |
| `ruoyi-ui/.../project/project/detail.vue` | import 替换 + 调用替换 |
| `ruoyi-ui/.../project/project/edit.vue` | import 替换 + 调用替换 + 删除废弃 import |
| `ruoyi-ui/.../revenue/team/detail.vue` | import 替换 + 调用替换 |
| `ruoyi-ui/.../project/customer/index.vue` | import 替换 + 调用替换 |
| `ruoyi-ui/.../project/contract/add.vue` | import 替换 + 函数体替换 |
| `ruoyi-ui/.../project/contract/edit.vue` | import 替换 + 函数体替换 |
| `ruoyi-ui/.../project/contract/detail.vue` | import 替换 + 函数体替换 |
