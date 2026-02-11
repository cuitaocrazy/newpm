# 公司收入确认功能实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 实现公司收入确认的完整功能，包括列表查询（含合计行）、收入确认/查看详情页、自动税后金额计算，以及在项目管理列表中集成收入确认入口

**架构：** 采用独立收入确认模块方案，后端扩展 ProjectController 增加收入确认接口，前端新建 revenue/company 模块，包含列表页和详情页，复用项目查询逻辑并增加收入确认字段

**技术栈：**
- 后端：Java 17, Spring Boot 3.5.8, MyBatis, Spring Security
- 前端：Vue 3.5, TypeScript 5.6, Vite 6.4, Element Plus 2.13, Pinia
- 数据库：MySQL 8.x (pm_project 表已包含收入确认字段)

---

## 任务分解

### Task 1: 后端 - 扩展 ProjectController 添加收入确认列表接口

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java` (在文件末尾添加)

**Step 1: 添加收入确认列表接口**

在 `ProjectController.java` 文件末尾添加以下代码（在最后一个方法之后，类结束大括号之前）：

```java
/**
 * 查询公司收入确认列表
 */
@PreAuthorize("@ss.hasPermi('revenue:company:list')")
@GetMapping("/revenue/list")
public TableDataInfo revenueList(Project project)
{
    startPage();
    List<Project> list = projectService.selectProjectList(project);
    return getDataTable(list);
}
```

**Step 2: 编译测试**

```bash
cd /Users/kongli/ws-claude/PM/newpm
mvn clean compile -pl ruoyi-project -am
```

预期：编译成功，无错误

**Step 3: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java
git commit -m "feat(backend): 添加收入确认列表查询接口

- 新增 GET /project/project/revenue/list 接口
- 权限标识: revenue:company:list
- 复用 projectService.selectProjectList 查询逻辑

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 2: 后端 - 添加收入确认详情查询接口

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java`

**Step 1: 添加收入确认详情接口**

在 `ProjectController.java` 的 `revenueList` 方法后面添加：

```java
/**
 * 获取收入确认详情
 */
@PreAuthorize("@ss.hasPermi('revenue:company:query')")
@GetMapping("/revenue/{projectId}")
public AjaxResult getRevenueInfo(@PathVariable("projectId") Long projectId)
{
    return success(projectService.selectProjectById(projectId));
}
```

**Step 2: 编译测试**

```bash
mvn clean compile -pl ruoyi-project -am
```

预期：编译成功

**Step 3: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java
git commit -m "feat(backend): 添加收入确认详情查询接口

- 新增 GET /project/project/revenue/{projectId} 接口
- 权限标识: revenue:company:query
- 返回完整项目信息用于收入确认

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 3: 后端 - 添加收入确认保存接口

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java`

**Step 1: 添加必要的导入**

在文件顶部的导入部分添加（如果还没有）：

```java
import java.math.BigDecimal;
import java.math.RoundingMode;
```

**Step 2: 添加收入确认保存接口**

在 `ProjectController.java` 的 `getRevenueInfo` 方法后面添加：

```java
/**
 * 保存收入确认信息
 */
@PreAuthorize("@ss.hasPermi('revenue:company:edit')")
@Log(title = "收入确认", businessType = BusinessType.UPDATE)
@PutMapping("/revenue")
public AjaxResult updateRevenue(@RequestBody Project project)
{
    // 验证必填字段
    if (project.getRevenueConfirmStatus() == null || project.getRevenueConfirmStatus().trim().isEmpty()) {
        return error("收入确认状态不能为空");
    }
    if (project.getRevenueConfirmYear() == null || project.getRevenueConfirmYear().trim().isEmpty()) {
        return error("收入确认年度不能为空");
    }
    if (project.getConfirmAmount() == null) {
        return error("确认金额不能为空");
    }
    if (project.getTaxRate() == null) {
        return error("税率不能为空");
    }

    // 自动计算税后金额
    BigDecimal confirmAmount = project.getConfirmAmount();
    BigDecimal taxRate = project.getTaxRate();
    BigDecimal afterTaxAmount = confirmAmount.divide(
        BigDecimal.ONE.add(taxRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)),
        2,
        RoundingMode.HALF_UP
    );
    project.setAfterTaxAmount(afterTaxAmount);

    return toAjax(projectService.updateProject(project));
}
```

**Step 3: 编译测试**

```bash
mvn clean compile -pl ruoyi-project -am
```

预期：编译成功

**Step 4: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java
git commit -m "feat(backend): 添加收入确认保存接口

- 新增 PUT /project/project/revenue 接口
- 权限标识: revenue:company:edit
- 自动计算税后金额 = 确认金额 / (1 + 税率/100)
- 验证必填字段: 确认状态、年度、金额、税率

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 4: 后端 - 添加收入确认导出接口

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java`

**Step 1: 添加导出接口**

在 `ProjectController.java` 的 `updateRevenue` 方法后面添加：

```java
/**
 * 导出收入确认列表
 */
@PreAuthorize("@ss.hasPermi('revenue:company:export')")
@Log(title = "收入确认", businessType = BusinessType.EXPORT)
@PostMapping("/revenue/export")
public void exportRevenue(HttpServletResponse response, Project project)
{
    List<Project> list = projectService.selectProjectList(project);
    ExcelUtil<Project> util = new ExcelUtil<Project>(Project.class);
    util.exportExcel(response, list, "收入确认数据");
}
```

**Step 2: 编译测试**

```bash
mvn clean compile -pl ruoyi-project -am
```

预期：编译成功

**Step 3: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java
git commit -m "feat(backend): 添加收入确认导出接口

- 新增 POST /project/project/revenue/export 接口
- 权限标识: revenue:company:export
- 导出 Excel 包含收入确认相关字段

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 5: 后端 - 完整编译测试

**Files:**
- Test: 完整后端编译

**Step 1: 完整编译打包**

```bash
cd /Users/kongli/ws-claude/PM/newpm
mvn clean package -Dmaven.test.skip=true
```

预期：编译成功，生成 jar 包

**Step 2: 验证接口路由**

检查 Controller 中的接口路由是否正确：
- GET `/project/project/revenue/list`
- GET `/project/project/revenue/{projectId}`
- PUT `/project/project/revenue`
- POST `/project/project/revenue/export`

---

### Task 6: 前端 - 创建 API 接口文件

**Files:**
- Create: `ruoyi-ui/src/api/revenue/company.js`

**Step 1: 创建目录结构**

```bash
mkdir -p ruoyi-ui/src/api/revenue
```

**Step 2: 创建 API 文件**

创建 `ruoyi-ui/src/api/revenue/company.js` 文件，内容如下：

```javascript
import request from '@/utils/request'

// 查询收入确认列表
export function listRevenue(query) {
  return request({
    url: '/project/project/revenue/list',
    method: 'get',
    params: query
  })
}

// 获取收入确认详情
export function getRevenue(projectId) {
  return request({
    url: '/project/project/revenue/' + projectId,
    method: 'get'
  })
}

// 保存收入确认信息
export function updateRevenue(data) {
  return request({
    url: '/project/project/revenue',
    method: 'put',
    data: data
  })
}

// 导出收入确认
export function exportRevenue(query) {
  return request({
    url: '/project/project/revenue/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  })
}
```

**Step 3: Commit**

```bash
git add ruoyi-ui/src/api/revenue/company.js
git commit -m "feat(frontend): 创建收入确认 API 接口封装

- 新增 listRevenue - 查询列表
- 新增 getRevenue - 获取详情
- 新增 updateRevenue - 保存确认信息
- 新增 exportRevenue - 导出数据

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 7: 前端 - 创建收入确认列表页（基础结构）

**Files:**
- Create: `ruoyi-ui/src/views/revenue/company/index.vue`

**Step 1: 创建目录**

```bash
mkdir -p ruoyi-ui/src/views/revenue/company
```

**Step 2: 创建列表页基础结构**

创建 `ruoyi-ui/src/views/revenue/company/index.vue`，内容如下：

```vue
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="120px">
      <!-- 查询条件将在下一步添加 -->
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['revenue:company:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="revenueList">
      <!-- 表格列将在下一步添加 -->
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="RevenueCompany">
import { listRevenue, exportRevenue } from "@/api/revenue/company"

const { proxy } = getCurrentInstance()

const revenueList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10
  }
})

const { queryParams } = toRefs(data)

/** 查询列表 */
function getList() {
  loading.value = true
  listRevenue(queryParams.value).then(response => {
    revenueList.value = response.rows
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
  handleQuery()
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('revenue/export', {
    ...queryParams.value
  }, `收入确认_${new Date().getTime()}.xlsx`)
}

// 初始化
getList()
</script>
```

**Step 3: Commit**

```bash
git add ruoyi-ui/src/views/revenue/company/index.vue
git commit -m "feat(frontend): 创建收入确认列表页基础结构

- 创建 Vue 组件基础框架
- 实现基本的查询、重置、导出功能
- 集成分页组件

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 8: 前端 - 添加列表页查询条件

**Files:**
- Modify: `ruoyi-ui/src/views/revenue/company/index.vue`

**Step 1: 读取现有文件了解结构**

读取 `ruoyi-ui/src/views/project/project/index.vue` 第 1-130 行，参考查询条件的实现方式

**Step 2: 添加字典和下拉选项**

在 `<script setup>` 中的 `const { proxy } = getCurrentInstance()` 后面添加：

```javascript
const { sys_xmfl, sys_yjqy, sys_xmjd, sys_spzt, sys_yszt, sys_srqrzt, sys_ndgl, sys_htzt } = proxy.useDict(
  'sys_xmfl', 'sys_yjqy', 'sys_xmjd', 'sys_spzt', 'sys_yszt', 'sys_srqrzt', 'sys_ndgl', 'sys_htzt'
)
```

在 `data` 变量前添加下拉选项数据：

```javascript
const deptTree = ref([])
const provinceList = ref([])
const projectManagerList = ref([])
const marketManagerList = ref([])
```

**Step 3: 扩展查询参数**

修改 `queryParams` 对象，添加所有查询条件：

```javascript
const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    projectDept: null,
    revenueConfirmYear: null,
    projectCategory: null,
    region: null,
    regionId: null,
    projectManagerId: null,
    marketManagerId: null,
    establishedYear: null,
    projectStage: null,
    approvalStatus: null,
    acceptanceStatus: null,
    revenueConfirmStatus: null
  }
})
```

**Step 4: 添加查询条件表单**

将 `<el-form>` 中的注释 `<!-- 查询条件将在下一步添加 -->` 替换为：

```vue
<el-form-item label="项目名称" prop="projectName">
  <el-input
    v-model="queryParams.projectName"
    placeholder="请输入项目名称"
    clearable
    @keyup.enter="handleQuery"
    style="width: 240px"
  />
</el-form-item>
<el-form-item label="项目部门" prop="projectDept">
  <el-tree-select
    v-model="queryParams.projectDept"
    :data="deptTree"
    :props="{ label: 'label', value: 'value', children: 'children' }"
    placeholder="请选择项目部门"
    check-strictly
    clearable
    filterable
    style="width: 240px"
  />
</el-form-item>
<el-form-item label="收入确认年度" prop="revenueConfirmYear">
  <el-select v-model="queryParams.revenueConfirmYear" placeholder="请选择收入确认年度" clearable style="width: 240px">
    <el-option
      v-for="dict in sys_ndgl"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
    />
  </el-select>
</el-form-item>
<el-form-item label="项目分类" prop="projectCategory">
  <el-select v-model="queryParams.projectCategory" placeholder="请选择项目分类" clearable style="width: 240px">
    <el-option
      v-for="dict in sys_xmfl"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
    />
  </el-select>
</el-form-item>
<el-form-item label="一级区域" prop="region">
  <el-select v-model="queryParams.region" placeholder="请选择一级区域" clearable @change="handleRegionChange" style="width: 240px">
    <el-option
      v-for="dict in sys_yjqy"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
    />
  </el-select>
</el-form-item>
<el-form-item label="二级区域" prop="regionId">
  <el-select v-model="queryParams.regionId" placeholder="请选择二级区域" clearable filterable style="width: 240px">
    <el-option
      v-for="item in provinceList"
      :key="item.regionId"
      :label="item.regionName"
      :value="item.regionId"
    />
  </el-select>
</el-form-item>
<el-form-item label="项目经理" prop="projectManagerId">
  <el-select v-model="queryParams.projectManagerId" placeholder="请选择项目经理" clearable filterable style="width: 240px">
    <el-option
      v-for="user in projectManagerList"
      :key="user.userId"
      :label="user.nickName"
      :value="user.userId"
    />
  </el-select>
</el-form-item>
<el-form-item label="市场经理" prop="marketManagerId">
  <el-select v-model="queryParams.marketManagerId" placeholder="请选择市场经理" clearable filterable style="width: 240px">
    <el-option
      v-for="user in marketManagerList"
      :key="user.userId"
      :label="user.nickName"
      :value="user.userId"
    />
  </el-select>
</el-form-item>
<el-form-item label="立项年度" prop="establishedYear">
  <el-select v-model="queryParams.establishedYear" placeholder="请选择立项年度" clearable style="width: 240px">
    <el-option
      v-for="dict in sys_ndgl"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
    />
  </el-select>
</el-form-item>
<el-form-item label="项目阶段" prop="projectStage">
  <el-select v-model="queryParams.projectStage" placeholder="请选择项目阶段" clearable style="width: 240px">
    <el-option
      v-for="dict in sys_xmjd"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
    />
  </el-select>
</el-form-item>
<el-form-item label="审核状态" prop="approvalStatus">
  <el-select v-model="queryParams.approvalStatus" placeholder="请选择审核状态" clearable style="width: 240px">
    <el-option
      v-for="dict in sys_spzt"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
    />
  </el-select>
</el-form-item>
<el-form-item label="验收状态" prop="acceptanceStatus">
  <el-select v-model="queryParams.acceptanceStatus" placeholder="请选择验收状态" clearable style="width: 240px">
    <el-option
      v-for="dict in sys_yszt"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
    />
  </el-select>
</el-form-item>
<el-form-item label="收入确认状态" prop="revenueConfirmStatus">
  <el-select v-model="queryParams.revenueConfirmStatus" placeholder="请选择收入确认状态" clearable style="width: 240px">
    <el-option
      v-for="dict in sys_srqrzt"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
    />
  </el-select>
</el-form-item>
```

**Step 5: 添加辅助方法和初始化**

在 `handleExport` 函数后面添加：

```javascript
/** 一级区域变化 */
function handleRegionChange(value) {
  queryParams.value.regionId = null
  provinceList.value = []
  if (value) {
    // TODO: 加载二级区域列表
  }
}

/** 初始化下拉选项 */
function initOptions() {
  // TODO: 加载部门树、区域列表、项目经理、市场经理
}
```

修改初始化部分：

```javascript
// 初始化
initOptions()
getList()
```

**Step 6: Commit**

```bash
git add ruoyi-ui/src/views/revenue/company/index.vue
git commit -m "feat(frontend): 添加收入确认列表查询条件

- 添加所有查询条件表单字段
- 集成字典数据(项目分类、区域、阶段、状态等)
- 添加下拉选项变量(部门、区域、经理)
- 实现一级区域联动二级区域

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 9: 前端 - 添加列表页表格列（不含合计行）

**Files:**
- Modify: `ruoyi-ui/src/views/revenue/company/index.vue`

**Step 1: 添加表格列**

将 `<el-table>` 中的注释 `<!-- 表格列将在下一步添加 -->` 替换为：

```vue
<el-table-column label="序号" type="index" width="55" align="center" />
<el-table-column label="项目名称" align="center" prop="projectName" min-width="150" show-overflow-tooltip />
<el-table-column label="项目编码" align="center" prop="projectCode" min-width="150" show-overflow-tooltip />
<el-table-column label="项目部门" align="center" prop="projectDept" min-width="120" show-overflow-tooltip />
<el-table-column label="项目经理" align="center" prop="projectManagerName" min-width="100" />
<el-table-column label="市场经理" align="center" prop="marketManagerName" min-width="100" />
<el-table-column label="项目分类" align="center" prop="projectCategory" width="120">
  <template #default="scope">
    <dict-tag :options="sys_xmfl" :value="scope.row.projectCategory"/>
  </template>
</el-table-column>
<el-table-column label="一级区域" align="center" prop="region" width="100">
  <template #default="scope">
    <dict-tag :options="sys_yjqy" :value="scope.row.region"/>
  </template>
</el-table-column>
<el-table-column label="二级区域" align="center" prop="regionName" width="100" />
<el-table-column label="项目阶段" align="center" prop="projectStage" width="100">
  <template #default="scope">
    <dict-tag :options="sys_xmjd" :value="scope.row.projectStage"/>
  </template>
</el-table-column>
<el-table-column label="创建时间" align="center" prop="createTime" width="160">
  <template #default="scope">
    <span>{{ parseTime(scope.row.createTime) }}</span>
  </template>
</el-table-column>
<el-table-column label="收入确认状态" align="center" prop="revenueConfirmStatus" min-width="120">
  <template #default="scope">
    <dict-tag :options="sys_srqrzt" :value="scope.row.revenueConfirmStatus"/>
  </template>
</el-table-column>
<el-table-column label="收入确认年度" align="center" prop="revenueConfirmYear" min-width="120" />
<el-table-column label="确认金额(含税)" align="center" prop="confirmAmount" min-width="120" />
<el-table-column label="税后金额" align="center" prop="afterTaxAmount" min-width="120" />
<el-table-column label="项目预算" align="center" prop="projectBudget" min-width="120" />
<el-table-column label="预估工作量" align="center" prop="estimatedWorkload" min-width="100" />
<el-table-column label="实际人天" align="center" prop="actualWorkload" min-width="100" />
<el-table-column label="合同金额" align="center" prop="contractAmount" min-width="120" />
<el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150" fixed="right">
  <template #default="scope">
    <el-button
      v-if="scope.row.revenueConfirmStatus === '0' || !scope.row.revenueConfirmStatus"
      link
      type="success"
      icon="Money"
      @click="handleRevenue(scope.row)"
      v-hasPermi="['revenue:company:edit']"
    >收入确认</el-button>
    <el-button
      v-else-if="scope.row.revenueConfirmStatus === '1'"
      link
      type="primary"
      icon="View"
      @click="handleRevenueView(scope.row)"
      v-hasPermi="['revenue:company:view']"
    >收入查看</el-button>
  </template>
</el-table-column>
```

**Step 2: 添加操作方法**

在 `initOptions` 函数后面添加：

```javascript
/** 收入确认按钮 */
function handleRevenue(row) {
  // TODO: 跳转到详情页（编辑模式）
  console.log('收入确认', row)
}

/** 收入查看按钮 */
function handleRevenueView(row) {
  // TODO: 跳转到详情页（查看模式）
  console.log('收入查看', row)
}
```

**Step 3: Commit**

```bash
git add ruoyi-ui/src/views/revenue/company/index.vue
git commit -m "feat(frontend): 添加收入确认列表表格列

- 添加基础列: 项目名称、编码、部门、经理等
- 添加收入确认列: 状态、年度、确认金额、税后金额
- 添加金额列: 项目预算、工作量、合同金额
- 添加操作列: 根据状态显示收入确认/查看按钮

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 10: 前端 - 实现列表页合计行功能

**Files:**
- Modify: `ruoyi-ui/src/views/revenue/company/index.vue`

**Step 1: 修改序号列**

将序号列替换为：

```vue
<el-table-column label="序号" width="55" align="center">
  <template #default="scope">
    <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">合计</span>
    <span v-else>{{ scope.$index }}</span>
  </template>
</el-table-column>
```

**Step 2: 修改金额列支持合计显示**

将所有金额列（确认金额、税后金额、项目预算、预估工作量、实际人天、合同金额）的模板改为：

```vue
<el-table-column label="确认金额(含税)" align="center" prop="confirmAmount" min-width="120">
  <template #default="scope">
    <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.confirmAmount }}</span>
    <span v-else>{{ scope.row.confirmAmount }}</span>
  </template>
</el-table-column>

<el-table-column label="税后金额" align="center" prop="afterTaxAmount" min-width="120">
  <template #default="scope">
    <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.afterTaxAmount }}</span>
    <span v-else>{{ scope.row.afterTaxAmount }}</span>
  </template>
</el-table-column>

<el-table-column label="项目预算" align="center" prop="projectBudget" min-width="120">
  <template #default="scope">
    <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.projectBudget }}</span>
    <span v-else>{{ scope.row.projectBudget }}</span>
  </template>
</el-table-column>

<el-table-column label="预估工作量" align="center" prop="estimatedWorkload" min-width="100">
  <template #default="scope">
    <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.estimatedWorkload }}</span>
    <span v-else>{{ scope.row.estimatedWorkload }}</span>
  </template>
</el-table-column>

<el-table-column label="实际人天" align="center" prop="actualWorkload" min-width="100">
  <template #default="scope">
    <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.actualWorkload }}</span>
    <span v-else>{{ scope.row.actualWorkload }}</span>
  </template>
</el-table-column>

<el-table-column label="合同金额" align="center" prop="contractAmount" min-width="120">
  <template #default="scope">
    <span v-if="scope.row.isSummaryRow" style="font-weight: bold;">{{ scope.row.contractAmount }}</span>
    <span v-else>{{ scope.row.contractAmount }}</span>
  </template>
</el-table-column>
```

**Step 3: 修改操作列，合计行不显示按钮**

将操作列模板改为：

```vue
<el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150" fixed="right">
  <template #default="scope">
    <template v-if="!scope.row.isSummaryRow">
      <el-button
        v-if="scope.row.revenueConfirmStatus === '0' || !scope.row.revenueConfirmStatus"
        link
        type="success"
        icon="Money"
        @click="handleRevenue(scope.row)"
        v-hasPermi="['revenue:company:edit']"
      >收入确认</el-button>
      <el-button
        v-else-if="scope.row.revenueConfirmStatus === '1'"
        link
        type="primary"
        icon="View"
        @click="handleRevenueView(scope.row)"
        v-hasPermi="['revenue:company:view']"
      >收入查看</el-button>
    </template>
  </template>
</el-table-column>
```

**Step 4: 添加合计计算函数**

在 `handleRevenueView` 函数后面添加：

```javascript
/** 计算合计行 */
function calculateSummary(list) {
  const summary = {
    isSummaryRow: true,
    projectBudget: 0,
    estimatedWorkload: 0,
    actualWorkload: 0,
    contractAmount: 0,
    confirmAmount: 0,
    afterTaxAmount: 0
  }

  list.forEach(item => {
    summary.projectBudget += (item.projectBudget || 0)
    summary.estimatedWorkload += (item.estimatedWorkload || 0)
    summary.actualWorkload += (item.actualWorkload || 0)
    summary.contractAmount += (item.contractAmount || 0)
    summary.confirmAmount += (item.confirmAmount || 0)
    summary.afterTaxAmount += (item.afterTaxAmount || 0)
  })

  // 格式化为两位小数
  Object.keys(summary).forEach(key => {
    if (key !== 'isSummaryRow' && summary[key]) {
      summary[key] = summary[key].toFixed(2)
    }
  })

  return summary
}
```

**Step 5: 修改 getList 函数，插入合计行**

将 `getList` 函数修改为：

```javascript
/** 查询列表 */
function getList() {
  loading.value = true
  listRevenue(queryParams.value).then(response => {
    const list = response.rows
    const summary = calculateSummary(list)
    revenueList.value = [summary, ...list] // 合计行在第一行
    total.value = response.total
    loading.value = false
  })
}
```

**Step 6: Commit**

```bash
git add ruoyi-ui/src/views/revenue/company/index.vue
git commit -m "feat(frontend): 实现收入确认列表合计行功能

- 在第一行显示合计行
- 合计字段: 项目预算、工作量、合同金额、确认金额、税后金额
- 合计行序号列显示\"合计\"文字
- 合计行操作列不显示按钮
- 数值加粗显示，保留两位小数

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 11: 前端 - 完善列表页辅助功能

**Files:**
- Modify: `ruoyi-ui/src/views/revenue/company/index.vue`
- Read: `ruoyi-ui/src/api/project/secondaryRegion.js`
- Read: `ruoyi-ui/src/api/system/user.js`

**Step 1: 读取参考文件**

读取以下文件了解 API 使用方式：
- `ruoyi-ui/src/api/project/secondaryRegion.js`
- `ruoyi-ui/src/api/system/user.js`

**Step 2: 添加必要的 import**

在文件顶部添加：

```vue
<script setup name="RevenueCompany">
import { listRevenue, exportRevenue } from "@/api/revenue/company"
import { listSecondaryRegion } from "@/api/project/secondaryRegion"
import { listUserByPost } from "@/api/system/user"
import { listDept } from "@/api/system/dept"
```

**Step 3: 完善 initOptions 函数**

将 `initOptions` 函数修改为：

```javascript
/** 初始化下拉选项 */
function initOptions() {
  // 加载部门树
  listDept().then(response => {
    deptTree.value = proxy.handleTree(response.data, "deptId")
  })

  // 加载项目经理（岗位代码：pm）
  listUserByPost('pm').then(response => {
    projectManagerList.value = response.data || []
  })

  // 加载市场经理（岗位代码：scjl）
  listUserByPost('scjl').then(response => {
    marketManagerList.value = response.data || []
  })
}
```

**Step 4: 完善 handleRegionChange 函数**

将 `handleRegionChange` 函数修改为：

```javascript
/** 一级区域变化 */
function handleRegionChange(value) {
  queryParams.value.regionId = null
  provinceList.value = []
  if (value) {
    listSecondaryRegion({ regionCode: value }).then(response => {
      provinceList.value = response.rows || []
    })
  }
}
```

**Step 5: Commit**

```bash
git add ruoyi-ui/src/views/revenue/company/index.vue
git commit -m "feat(frontend): 完善收入确认列表辅助功能

- 实现部门树加载
- 实现项目经理、市场经理下拉选项加载
- 实现一级区域联动二级区域数据加载
- 集成必要的 API 接口

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 12: 前端 - 创建收入确认详情页（基础结构）

**Files:**
- Create: `ruoyi-ui/src/views/revenue/company/detail.vue`

**Step 1: 创建详情页基础结构**

创建 `ruoyi-ui/src/views/revenue/company/detail.vue`，内容如下：

```vue
<template>
  <div class="app-container">
    <el-page-header @back="goBack" :title="pageTitle" />

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 左侧：项目详情（60%） -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: bold;">项目详情</span>
          </template>
          <!-- 项目详情将在下一步添加 -->
        </el-card>
      </el-col>

      <!-- 右侧：收入确认表单（40%） -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: bold;">收入确认信息</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
            <!-- 表单字段将在下一步添加 -->

            <el-form-item style="margin-top: 20px;">
              <el-button type="primary" @click="submitForm">保存</el-button>
              <el-button @click="goBack">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="RevenueCompanyDetail">
import { getRevenue, updateRevenue } from "@/api/revenue/company"

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const projectId = ref(route.params.projectId)
const pageTitle = ref("收入确认")

const data = reactive({
  form: {},
  rules: {}
})

const { form, rules } = toRefs(data)

/** 获取详情 */
function getDetail() {
  getRevenue(projectId.value).then(response => {
    form.value = response.data
  })
}

/** 提交表单 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      updateRevenue(form.value).then(response => {
        proxy.$modal.msgSuccess("保存成功")
        goBack()
      })
    }
  })
}

/** 返回 */
function goBack() {
  router.push("/revenue/company")
}

// 初始化
getDetail()
</script>
```

**Step 2: Commit**

```bash
git add ruoyi-ui/src/views/revenue/company/detail.vue
git commit -m "feat(frontend): 创建收入确认详情页基础结构

- 创建左右布局（项目详情 + 收入确认表单）
- 实现基本的获取详情、保存、返回功能
- 添加页面头部和卡片容器

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 13: 前端 - 添加详情页左侧项目详情

**Files:**
- Modify: `ruoyi-ui/src/views/revenue/company/detail.vue`

**Step 1: 添加项目详情内容**

将左侧卡片中的注释 `<!-- 项目详情将在下一步添加 -->` 替换为：

```vue
<!-- 基本信息 -->
<div class="section-title">基本信息</div>
<el-descriptions :column="2" border>
  <el-descriptions-item label="项目名称">{{ form.projectName }}</el-descriptions-item>
  <el-descriptions-item label="项目编码">{{ form.projectCode }}</el-descriptions-item>
  <el-descriptions-item label="项目部门">{{ form.projectDeptName }}</el-descriptions-item>
  <el-descriptions-item label="项目分类">{{ form.projectCategory }}</el-descriptions-item>
  <el-descriptions-item label="预估工作量">{{ form.estimatedWorkload }} 人天</el-descriptions-item>
  <el-descriptions-item label="实际工作量">{{ form.actualWorkload }} 人天</el-descriptions-item>
  <el-descriptions-item label="项目阶段">{{ form.projectStage }}</el-descriptions-item>
  <el-descriptions-item label="验收状态">{{ form.acceptanceStatus }}</el-descriptions-item>
  <el-descriptions-item label="一级区域">{{ form.region }}</el-descriptions-item>
  <el-descriptions-item label="二级区域">{{ form.regionName }}</el-descriptions-item>
  <el-descriptions-item label="项目地址" :span="2">{{ form.projectAddress }}</el-descriptions-item>
  <el-descriptions-item label="项目计划" :span="2">{{ form.projectPlan }}</el-descriptions-item>
</el-descriptions>

<!-- 人员配置 -->
<div class="section-title">人员配置</div>
<el-descriptions :column="2" border>
  <el-descriptions-item label="项目经理">{{ form.projectManagerName }}</el-descriptions-item>
  <el-descriptions-item label="市场经理">{{ form.marketManagerName }}</el-descriptions-item>
  <el-descriptions-item label="销售负责人">{{ form.salesManagerName }}</el-descriptions-item>
  <el-descriptions-item label="销售联系方式">{{ form.salesContact }}</el-descriptions-item>
  <el-descriptions-item label="参与人员" :span="2">{{ form.participantsNames }}</el-descriptions-item>
</el-descriptions>

<!-- 客户信息 -->
<div class="section-title">客户信息</div>
<el-descriptions :column="2" border>
  <el-descriptions-item label="客户名称">{{ form.customerName }}</el-descriptions-item>
  <el-descriptions-item label="客户联系人">{{ form.customerContactName }}</el-descriptions-item>
  <el-descriptions-item label="客户联系方式">{{ form.customerContactPhone }}</el-descriptions-item>
  <el-descriptions-item label="商户联系人">{{ form.merchantContact }}</el-descriptions-item>
  <el-descriptions-item label="商户联系方式">{{ form.merchantPhone }}</el-descriptions-item>
</el-descriptions>

<!-- 时间规划 -->
<div class="section-title">时间规划</div>
<el-descriptions :column="2" border>
  <el-descriptions-item label="启动日期">{{ parseTime(form.startDate, '{y}-{m}-{d}') }}</el-descriptions-item>
  <el-descriptions-item label="结束日期">{{ parseTime(form.endDate, '{y}-{m}-{d}') }}</el-descriptions-item>
  <el-descriptions-item label="上线日期">{{ parseTime(form.productionDate, '{y}-{m}-{d}') }}</el-descriptions-item>
  <el-descriptions-item label="验收日期">{{ parseTime(form.acceptanceDate, '{y}-{m}-{d}') }}</el-descriptions-item>
</el-descriptions>

<!-- 成本预算 -->
<div class="section-title">成本预算</div>
<el-descriptions :column="2" border>
  <el-descriptions-item label="项目预算">{{ form.projectBudget }} 元</el-descriptions-item>
  <el-descriptions-item label="项目费用">{{ form.projectCost }} 元</el-descriptions-item>
  <el-descriptions-item label="成本预算">{{ form.costBudget }} 元</el-descriptions-item>
  <el-descriptions-item label="人工成本">{{ form.laborCost }} 元</el-descriptions-item>
  <el-descriptions-item label="采购成本">{{ form.purchaseCost }} 元</el-descriptions-item>
</el-descriptions>
```

**Step 2: 添加样式**

在 `</script>` 后面添加：

```vue
<style scoped>
.section-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-top: 20px;
  margin-bottom: 10px;
  padding-left: 10px;
  border-left: 3px solid #409EFF;
}

.section-title:first-child {
  margin-top: 0;
}
</style>
```

**Step 3: Commit**

```bash
git add ruoyi-ui/src/views/revenue/company/detail.vue
git commit -m "feat(frontend): 添加详情页左侧项目详情

- 展示基本信息、人员配置、客户信息
- 展示时间规划、成本预算
- 使用 el-descriptions 组件全部展开显示
- 添加分区标题样式

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 14: 前端 - 添加详情页右侧收入确认表单

**Files:**
- Modify: `ruoyi-ui/src/views/revenue/company/detail.vue`

**Step 1: 添加字典**

在 `const { proxy } = getCurrentInstance()` 后面添加：

```javascript
const { sys_srqrzt, sys_ndgl, sys_xmfl } = proxy.useDict('sys_srqrzt', 'sys_ndgl', 'sys_xmfl')
```

**Step 2: 添加表单字段**

将右侧表单中的注释 `<!-- 表单字段将在下一步添加 -->` 替换为：

```vue
<el-form-item label="收入确认状态" prop="revenueConfirmStatus">
  <el-select v-model="form.revenueConfirmStatus" placeholder="请选择收入确认状态">
    <el-option
      v-for="dict in sys_srqrzt"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
    />
  </el-select>
</el-form-item>

<el-form-item label="收入确认年度" prop="revenueConfirmYear">
  <el-select v-model="form.revenueConfirmYear" placeholder="请选择收入确认年度">
    <el-option
      v-for="dict in sys_ndgl"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
    />
  </el-select>
</el-form-item>

<el-form-item label="确认金额（含税）" prop="confirmAmount">
  <el-input-number
    v-model="form.confirmAmount"
    :precision="2"
    :step="100"
    :min="0"
    placeholder="请输入确认金额"
    style="width: 100%;"
  />
  <span style="margin-left: 8px;">元</span>
</el-form-item>

<el-form-item label="税率" prop="taxRate">
  <el-input-number
    v-model="form.taxRate"
    :precision="2"
    :step="0.1"
    :min="0"
    :max="100"
    placeholder="请输入税率"
    style="width: 100%;"
  />
  <span style="margin-left: 8px;">%</span>
</el-form-item>

<el-form-item label="税后金额（不含税）">
  <el-input v-model="form.afterTaxAmount" disabled style="width: 100%;">
    <template #append>元</template>
  </el-input>
</el-form-item>
```

**Step 3: 添加表单验证规则**

修改 `rules` 对象：

```javascript
const data = reactive({
  form: {},
  rules: {
    revenueConfirmStatus: [
      { required: true, message: "收入确认状态不能为空", trigger: "change" }
    ],
    revenueConfirmYear: [
      { required: true, message: "收入确认年度不能为空", trigger: "change" }
    ],
    confirmAmount: [
      { required: true, message: "确认金额不能为空", trigger: "blur" }
    ],
    taxRate: [
      { required: true, message: "税率不能为空", trigger: "blur" }
    ]
  }
})
```

**Step 4: Commit**

```bash
git add ruoyi-ui/src/views/revenue/company/detail.vue
git commit -m "feat(frontend): 添加详情页右侧收入确认表单

- 添加收入确认状态下拉选择
- 添加收入确认年度下拉选择
- 添加确认金额输入框（数字组件）
- 添加税率输入框（数字组件）
- 添加税后金额显示（只读）
- 添加表单验证规则

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 15: 前端 - 实现税后金额自动计算

**Files:**
- Modify: `ruoyi-ui/src/views/revenue/company/detail.vue`

**Step 1: 根据项目分类初始化税率**

修改 `getDetail` 函数：

```javascript
/** 获取详情 */
function getDetail() {
  getRevenue(projectId.value).then(response => {
    form.value = response.data
    // 初始化税率
    if (!form.value.taxRate) {
      initTaxRate(form.value.projectCategory)
    }
  })
}
```

**Step 2: 添加税率初始化函数**

在 `getDetail` 函数后面添加：

```javascript
/** 根据项目分类初始化税率 */
function initTaxRate(projectCategory) {
  if (projectCategory === '1') { // 软件开发类
    form.value.taxRate = 6
  } else if (projectCategory === '2') { // 硬件销售类
    form.value.taxRate = 13
  }
}
```

**Step 3: 添加税后金额自动计算**

在 `initTaxRate` 函数后面添加：

```javascript
/** 计算税后金额 */
function calculateAfterTax() {
  const amount = form.value.confirmAmount
  const rate = form.value.taxRate
  if (amount && rate != null) {
    const afterTax = amount / (1 + rate / 100)
    form.value.afterTaxAmount = afterTax.toFixed(2)
  } else {
    form.value.afterTaxAmount = null
  }
}

// 监听确认金额和税率变化，自动计算税后金额
watch(
  () => [form.value.confirmAmount, form.value.taxRate],
  () => {
    calculateAfterTax()
  }
)
```

**Step 4: Commit**

```bash
git add ruoyi-ui/src/views/revenue/company/detail.vue
git commit -m "feat(frontend): 实现税后金额自动计算

- 根据项目分类初始化税率(软件6%，硬件13%)
- 监听确认金额和税率变化
- 自动计算税后金额 = 确认金额 / (1 + 税率/100)
- 保留两位小数

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 16: 前端 - 完善列表页跳转到详情页

**Files:**
- Modify: `ruoyi-ui/src/views/revenue/company/index.vue`

**Step 1: 添加 router 导入**

在 `<script setup>` 开头添加：

```javascript
import { useRouter } from 'vue-router'
const router = useRouter()
```

**Step 2: 完善跳转方法**

将 `handleRevenue` 和 `handleRevenueView` 函数修改为：

```javascript
/** 收入确认按钮 */
function handleRevenue(row) {
  router.push({
    path: '/revenue/company/detail/' + row.projectId,
    query: { mode: 'edit' }
  })
}

/** 收入查看按钮 */
function handleRevenueView(row) {
  router.push({
    path: '/revenue/company/detail/' + row.projectId,
    query: { mode: 'view' }
  })
}
```

**Step 3: Commit**

```bash
git add ruoyi-ui/src/views/revenue/company/index.vue
git commit -m "feat(frontend): 完善列表页跳转到详情页

- 实现收入确认按钮跳转(编辑模式)
- 实现收入查看按钮跳转(查看模式)
- 通过 query 参数传递模式标识

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 17: 前端 - 实现详情页编辑/查看模式切换

**Files:**
- Modify: `ruoyi-ui/src/views/revenue/company/detail.vue`

**Step 1: 添加模式状态**

在 `const pageTitle = ref("收入确认")` 后面添加：

```javascript
const mode = ref(route.query.mode || 'edit') // edit | view
const isViewMode = computed(() => mode.value === 'view')
```

**Step 2: 修改表单为只读模式**

将所有表单字段添加 `:disabled` 属性：

```vue
<el-form-item label="收入确认状态" prop="revenueConfirmStatus">
  <el-select v-model="form.revenueConfirmStatus" placeholder="请选择收入确认状态" :disabled="isViewMode">
    <!-- options -->
  </el-select>
</el-form-item>

<el-form-item label="收入确认年度" prop="revenueConfirmYear">
  <el-select v-model="form.revenueConfirmYear" placeholder="请选择收入确认年度" :disabled="isViewMode">
    <!-- options -->
  </el-select>
</el-form-item>

<el-form-item label="确认金额（含税）" prop="confirmAmount">
  <el-input-number
    v-model="form.confirmAmount"
    :precision="2"
    :step="100"
    :min="0"
    placeholder="请输入确认金额"
    :disabled="isViewMode"
    style="width: 100%;"
  />
  <span style="margin-left: 8px;">元</span>
</el-form-item>

<el-form-item label="税率" prop="taxRate">
  <el-input-number
    v-model="form.taxRate"
    :precision="2"
    :step="0.1"
    :min="0"
    :max="100"
    placeholder="请输入税率"
    :disabled="isViewMode"
    style="width: 100%;"
  />
  <span style="margin-left: 8px;">%</span>
</el-form-item>
```

**Step 3: 修改按钮显示逻辑**

将表单底部的按钮修改为：

```vue
<el-form-item style="margin-top: 20px;">
  <el-button v-if="!isViewMode" type="primary" @click="submitForm">保存</el-button>
  <el-button v-if="isViewMode" type="primary" @click="toggleEditMode">编辑</el-button>
  <el-button @click="goBack">{{ isViewMode ? '关闭' : '取消' }}</el-button>
</el-form-item>
```

**Step 4: 添加切换编辑模式函数**

在 `goBack` 函数后面添加：

```javascript
/** 切换编辑模式 */
function toggleEditMode() {
  mode.value = 'edit'
}
```

**Step 5: 修改页面标题**

修改 `pageTitle` 的定义：

```javascript
const pageTitle = computed(() => {
  return mode.value === 'view' ? '收入查看' : '收入确认'
})
```

**Step 6: Commit**

```bash
git add ruoyi-ui/src/views/revenue/company/detail.vue
git commit -m "feat(frontend): 实现详情页编辑/查看模式切换

- 添加模式状态(edit/view)
- 查看模式下表单字段禁用
- 查看模式显示编辑按钮，点击切换为编辑模式
- 根据模式调整页面标题和按钮文字

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 18: 前端 - 集成到项目管理列表

**Files:**
- Modify: `ruoyi-ui/src/views/project/project/index.vue`

**Step 1: 读取现有文件找到操作列位置**

读取 `ruoyi-ui/src/views/project/project/index.vue` 文件，找到操作列（el-table-column label="操作"）的位置

**Step 2: 在操作列添加收入确认/查看按钮**

在操作列的现有按钮后面，删除按钮前面添加：

```vue
<!-- 收入确认/查看按钮 -->
<el-button
  v-if="scope.row.revenueConfirmStatus === '0' || !scope.row.revenueConfirmStatus"
  link
  type="success"
  icon="Money"
  @click="handleRevenue(scope.row)"
  v-hasPermi="['revenue:company:edit']"
>收入确认</el-button>

<el-button
  v-else-if="scope.row.revenueConfirmStatus === '1'"
  link
  type="primary"
  icon="View"
  @click="handleRevenueView(scope.row)"
  v-hasPermi="['revenue:company:view']"
>收入查看</el-button>
```

**Step 3: 添加 router 导入（如果还没有）**

在 `<script setup>` 中检查是否已导入 router，如果没有则添加：

```javascript
import { useRouter } from 'vue-router'
const router = useRouter()
```

**Step 4: 添加跳转方法**

在文件末尾的函数区域添加：

```javascript
/** 收入确认按钮 */
function handleRevenue(row) {
  router.push({
    path: '/revenue/company/detail/' + row.projectId,
    query: { mode: 'edit' }
  })
}

/** 收入查看按钮 */
function handleRevenueView(row) {
  router.push({
    path: '/revenue/company/detail/' + row.projectId,
    query: { mode: 'view' }
  })
}
```

**Step 5: Commit**

```bash
git add ruoyi-ui/src/views/project/project/index.vue
git commit -m "feat(frontend): 项目管理列表集成收入确认入口

- 在操作列添加收入确认/查看按钮
- 根据 revenueConfirmStatus 状态显示不同按钮
- 实现跳转到收入确认详情页

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 19: 前端 - 构建测试

**Files:**
- Test: 前端构建

**Step 1: 前端构建**

```bash
cd /Users/kongli/ws-claude/PM/newpm/ruoyi-ui
npm run build:prod
```

预期：构建成功，生成 dist 目录

**Step 2: 检查生成的文件**

```bash
ls -la dist/
```

预期：包含 index.html 和静态资源文件

---

### Task 20: 完整集成测试

**Files:**
- Test: 完整系统测试

**Step 1: 启动后端服务**

```bash
cd /Users/kongli/ws-claude/PM/newpm
./ry.sh start
```

或者：

```bash
java -Xms512m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
```

预期：后端启动成功，监听 8080 端口

**Step 2: 启动前端开发服务器**

在另一个终端：

```bash
cd /Users/kongli/ws-claude/PM/newpm/ruoyi-ui
npm run dev
```

预期：前端启动成功，访问 http://localhost

**Step 3: 功能测试清单**

测试以下功能：

1. **菜单访问**
   - 访问 收入确认管理 > 公司收入确认
   - 验证列表页正常显示

2. **列表查询**
   - 测试各种查询条件
   - 验证合计行计算正确
   - 验证分页功能

3. **收入确认**
   - 点击未确认项目的"收入确认"按钮
   - 验证详情页左侧项目信息显示完整
   - 填写收入确认信息
   - 验证税后金额自动计算
   - 保存并验证成功

4. **收入查看**
   - 点击已确认项目的"收入查看"按钮
   - 验证字段只读
   - 点击"编辑"按钮
   - 验证切换为编辑模式

5. **项目列表集成**
   - 访问 项目管理 > 项目管理
   - 验证操作列显示收入确认/查看按钮
   - 点击按钮跳转到收入确认详情页

6. **导出功能**
   - 点击导出按钮
   - 验证 Excel 文件下载成功
   - 验证数据完整性

**Step 4: 停止服务**

测试完成后停止服务：

```bash
# 停止后端
./ry.sh stop

# 停止前端（在前端终端按 Ctrl+C）
```

---

### Task 21: 最终提交

**Files:**
- Commit: 最终验证和提交

**Step 1: 检查所有更改**

```bash
git status
```

预期：所有更改已提交

**Step 2: 查看提交历史**

```bash
git log --oneline -21
```

预期：可以看到所有 21 个提交

**Step 3: 创建功能分支标签（可选）**

```bash
git tag -a v1.0.0-revenue-confirmation -m "公司收入确认功能 v1.0.0

功能特性:
- 收入确认列表查询（含合计行）
- 左右布局详情页（项目详情 + 收入确认表单）
- 自动计算税后金额
- 项目管理列表集成
- 导出功能

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

**Step 4: 推送到远程（如果需要）**

```bash
git push origin main
git push origin v1.0.0-revenue-confirmation
```

---

## 实施完成

所有任务已完成！公司收入确认功能已全部实现：

✅ **后端接口** - 列表查询、详情获取、保存更新、导出
✅ **前端列表页** - 查询条件、表格显示、合计行、操作按钮
✅ **前端详情页** - 左右布局、项目详情、收入确认表单、自动计算
✅ **项目列表集成** - 收入确认/查看入口
✅ **权限控制** - 完整的权限标识和验证
✅ **数据验证** - 前后端表单验证
✅ **模式切换** - 编辑/查看模式

**下一步建议：**
1. 进行充分的功能测试
2. 收集用户反馈
3. 根据实际使用情况优化体验
4. 考虑添加批量确认、统计报表等扩展功能
