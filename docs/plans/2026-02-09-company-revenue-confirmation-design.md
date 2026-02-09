# 公司收入确认管理功能设计文档

**创建日期：** 2026-02-09
**功能名称：** 公司收入确认管理
**设计版本：** v1.0

---

## 一、功能概述

### 1.1 业务背景

为项目管理系统新增收入确认管理功能，用于记录和管理项目的公司收入确认信息，包括确认金额、税率、税后金额等关键财务数据。

### 1.2 功能范围

- 一级菜单：收入确认管理
- 二级菜单：公司收入确认（独立列表页）
- 项目列表集成：在项目管理列表的操作列添加收入确认入口
- 收入确认抽屉：左侧展示项目信息，右侧编辑收入确认要素

---

## 二、数据库设计

### 2.1 表结构变更

**表名：** `pm_project`

**新增字段：**

```sql
ALTER TABLE pm_project
ADD COLUMN `company_revenue_confirmed_time` datetime DEFAULT NULL COMMENT '公司收入确认时间';
```

### 2.2 字段说明

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| `revenue_confirm_status` | char(1) | 收入确认状态 | 字典 sys_srqrzt：0=未确认，1=已确认 |
| `revenue_confirm_year` | varchar(20) | 收入确认年度 | 字典 sys_ndgl |
| `confirm_amount` | decimal(15,2) | 确认金额（含税） | 单位：元 |
| `tax_rate` | decimal(5,2) | 税率 | 单位：%，如 6.00 表示 6% |
| `after_tax_amount` | decimal(15,2) | 税后金额 | 自动计算：confirm_amount / (1 + tax_rate/100) |
| `company_revenue_confirmed_by` | varchar(64) | 公司收入确认人ID | 当前登录用户ID |
| `company_revenue_confirmed_time` | datetime | 公司收入确认时间 | 保存时自动记录 |
| `remark` | varchar(500) | 备注 | 可选 |

### 2.3 字典数据

需要确保以下字典类型存在：

- `sys_srqrzt` - 收入确认状态
  - 0 = 未确认
  - 1 = 已确认

- `sys_ndgl` - 年度管理
  - 2020、2021、2022、2023、2024、2025...

---

## 三、菜单结构

### 3.1 菜单层级

```
收入确认管理（一级菜单，order_num=6，图标=money）
└── 公司收入确认（二级菜单，路由=/revenue/company）
    ├── 公司收入确认查询（按钮权限：revenue:company:query）
    ├── 公司收入确认查看（按钮权限：revenue:company:view）
    ├── 公司收入确认编辑（按钮权限：revenue:company:edit）
    └── 公司收入确认导出（按钮权限：revenue:company:export）
```

### 3.2 菜单SQL

```sql
-- ========================================
-- 收入确认管理模块菜单数据
-- ========================================

-- 一级菜单：收入确认管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('收入确认管理', 0, 6, 'revenue', NULL, 1, 0, 'M', '0', '0', '', 'money', 'admin', sysdate(), '', NULL, '收入确认管理目录');

-- 获取收入确认管理菜单ID
SELECT @revenueMenuId := LAST_INSERT_ID();

-- 二级菜单：公司收入确认
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('公司收入确认', @revenueMenuId, 1, 'company', 'revenue/company/index', 1, 0, 'C', '0', '0', 'revenue:company:list', 'money', 'admin', sysdate(), '', NULL, '公司收入确认菜单');

-- 获取公司收入确认菜单ID
SELECT @companyRevenueMenuId := LAST_INSERT_ID();

-- 按钮权限：查询
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('公司收入确认查询', @companyRevenueMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:query', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：查看
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('公司收入确认查看', @companyRevenueMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:view', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：编辑
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('公司收入确认编辑', @companyRevenueMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:edit', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：导出
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('公司收入确认导出', @companyRevenueMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:export', '#', 'admin', sysdate(), '', NULL, '');
```

### 3.3 权限说明

| 权限标识 | 功能 | 使用场景 |
|---------|------|---------|
| `revenue:company:query` | 列表查询 | 访问公司收入确认列表页 |
| `revenue:company:view` | 查看详情 | 点击"查看确认"按钮，查看收入确认信息 |
| `revenue:company:edit` | 编辑确认 | 点击"收入确认"按钮或"编辑"按钮，修改收入确认信息 |
| `revenue:company:export` | 导出数据 | 导出收入确认列表 |

---

## 四、公司收入确认列表页

### 4.1 页面路由

**路径：** `ruoyi-ui/src/views/revenue/company/index.vue`

**路由配置：**
```javascript
{
  path: '/revenue/company',
  component: Layout,
  hidden: false,
  children: [{
    path: 'index',
    component: () => import('@/views/revenue/company/index'),
    name: 'RevenueCompany',
    meta: { title: '公司收入确认', icon: 'money' }
  }]
}
```

### 4.2 查询条件

- 项目名称（模糊查询）
- 项目部门（下拉选择）
- 收入确认年度（字典 sys_ndgl）
- 收入确认状态（字典 sys_srqrzt）
- 项目分类（字典 sys_xmfl）
- 一级区域（字典 sys_yjqy）
- 项目经理（用户选择）

### 4.3 列表字段

| 字段 | 说明 | 宽度 |
|------|------|------|
| 项目编号 | project_code | 180px |
| 项目名称 | project_name | 200px |
| 项目部门 | project_dept | 120px |
| 确认金额（含税） | confirm_amount | 120px |
| 税率 | tax_rate | 80px |
| 税后金额 | after_tax_amount | 120px |
| 收入确认年度 | revenue_confirm_year（字典） | 100px |
| 收入确认状态 | revenue_confirm_status（字典标签） | 100px |
| 确认人 | company_revenue_confirmed_by（显示用户名） | 100px |
| 确认时间 | company_revenue_confirmed_time | 160px |
| 操作 | 按钮 | 150px |

### 4.4 操作列按钮

- **未确认**（revenue_confirm_status = '0'）：显示"收入确认"按钮（蓝色，权限：revenue:company:edit）
- **已确认**（revenue_confirm_status = '1'）：显示"查看确认"按钮（绿色，权限：revenue:company:view）

### 4.5 顶部操作按钮

- 导出按钮（权限：revenue:company:export）

---

## 五、收入确认抽屉页面

### 5.1 抽屉配置

- **打开方式：** Drawer（从右侧滑出）
- **宽度：** 80% 或 1200px
- **标题：**
  - 未确认时：`公司收入确认 - ${项目名称}`
  - 已确认时（查看模式）：`查看收入确认 - ${项目名称}`
  - 已确认时（编辑模式）：`编辑收入确认 - ${项目名称}`

### 5.2 布局结构

采用左右分栏布局（6:4 比例）：

```
┌─────────────────────────────────────────────────────┐
│  公司收入确认 - XXX项目                    [编辑] [×] │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────────┐  ┌────────────────┐         │
│  │                  │  │                │         │
│  │   项目信息       │  │  收入确认要素  │         │
│  │   (60%)          │  │  (40%)         │         │
│  │                  │  │                │         │
│  │   只读展示       │  │  可编辑表单    │         │
│  │                  │  │                │         │
│  └──────────────────┘  └────────────────┘         │
│                                                     │
│                          [取消] [保存]              │
└─────────────────────────────────────────────────────┘
```

### 5.3 左侧：项目信息（只读）

完整参照项目详情页面，使用 `el-descriptions` 展示：

**5.3.1 项目基本信息**
- 行业、区域、简称
- 立项年份、项目编号
- 项目名称
- 项目分类、项目部门、预估工作量
- 项目状态、验收状态、审核状态
- 项目地址
- 项目计划
- 项目描述
- 审核意见

**5.3.2 人员配置**
- 项目经理、市场经理、销售负责人
- 销售联系方式
- 参与人员

**5.3.3 客户信息**
- 客户名称、客户联系人、客户联系方式
- 商户联系人、商户联系方式

**5.3.4 时间规划**
- 启动日期、结束日期、投产日期
- 验收日期

**5.3.5 成本预算**
- 项目预算、项目费用、费用预算
- 成本预算、人力费用、采购成本

**5.3.6 备注**
- 备注信息

### 5.4 右侧：收入确认要素表单

**5.4.1 收入确认状态**
- 组件：`el-tag`（只读显示）
- 数据源：字典 `sys_srqrzt`
- 说明：
  - 查看模式：显示当前状态标签
  - 编辑模式：不显示此字段（保存时自动设为"已确认"）

**5.4.2 收入确认年度**（必填）
- 组件：`el-select`
- 数据源：字典 `sys_ndgl`
- 验证：必填
- 默认值：当前年份

**5.4.3 确认金额（含税）**（必填）
- 组件：`el-input-number`
- 单位：元
- 精度：2位小数
- 最小值：0
- 验证：必填，大于0

**5.4.4 税率**（必填）
- 组件：`el-input-number`
- 单位：%
- 精度：2位小数
- 范围：0-100
- 验证：必填
- **快捷按钮**：下方显示两个按钮
  - `6%` 按钮（蓝色）- 软件开发类
  - `13%` 按钮（蓝色）- 硬件类

**5.4.5 税后金额**（自动计算，只读）
- 组件：`el-input`（禁用）
- 单位：元
- 计算公式：`税后金额 = 确认金额（含税） / (1 + 税率/100)`
- 实时计算：当确认金额或税率变化时自动更新
- 显示格式：保留2位小数
- 示例：确认金额100元，税率6%，税后金额 = 100 / 1.06 = 94.34元

**5.4.6 备注**（可选）
- 组件：`el-input` type="textarea"
- 行数：4行
- 最大长度：500字符
- 验证：可选

---

## 六、交互逻辑

### 6.1 查看模式（已确认的项目）

**触发：** 点击列表中"查看确认"按钮

**状态：**
- 左侧项目信息：只读展示
- 右侧收入确认要素：所有字段只读
- 右上角显示"编辑"按钮（权限：revenue:company:edit）
- 底部只显示"关闭"按钮

**字段显示：**
- 收入确认状态：显示标签（已确认）
- 其他5个字段：显示已保存的值

### 6.2 编辑模式（未确认或点击编辑按钮）

**触发：**
- 点击列表中"收入确认"按钮（未确认项目）
- 或在查看模式点击"编辑"按钮

**状态：**
- 左侧项目信息：只读展示
- 右侧收入确认要素：可编辑
- 右上角"编辑"按钮变为"取消编辑"
- 底部显示"取消"和"保存"按钮

**字段显示：**
- 收入确认状态：不显示（保存时自动设为"已确认"）
- 其他5个字段：可编辑

### 6.3 实时计算逻辑

**税后金额自动计算：**
```javascript
// 监听确认金额和税率变化
watch([() => form.confirmAmount, () => form.taxRate], () => {
  if (form.confirmAmount && form.taxRate) {
    form.afterTaxAmount = (form.confirmAmount / (1 + form.taxRate / 100)).toFixed(2)
  } else {
    form.afterTaxAmount = null
  }
})
```

**快捷按钮：**
```javascript
// 点击6%按钮
const setTaxRate6 = () => {
  form.taxRate = 6
}

// 点击13%按钮
const setTaxRate13 = () => {
  form.taxRate = 13
}
```

### 6.4 保存逻辑

**点击"保存"按钮时：**
1. 表单验证（年度、确认金额、税率必填）
2. 计算税后金额
3. 设置 `revenue_confirm_status = '1'`（已确认）
4. 设置 `company_revenue_confirmed_by = 当前用户ID`
5. 设置 `company_revenue_confirmed_time = 当前时间`
6. 调用后端API保存
7. 成功后：
   - 提示"保存成功"
   - 关闭抽屉
   - 刷新列表

---

## 七、后端API设计

### 7.1 Controller层

**文件：** `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java`

在现有的 `ProjectController` 中新增方法：

**7.1.1 公司收入确认列表查询**
```java
/**
 * 查询公司收入确认列表
 */
@PreAuthorize("@ss.hasPermi('revenue:company:query')")
@GetMapping("/revenueList")
public TableDataInfo revenueList(Project project) {
    startPage();
    List<Project> list = projectService.selectProjectList(project);
    return getDataTable(list);
}
```

**7.1.2 获取项目详情（用于查看收入确认）**
```java
/**
 * 获取项目详情（用于查看收入确认）
 */
@PreAuthorize("@ss.hasPermi('revenue:company:view')")
@GetMapping(value = "/revenue/{projectId}")
public AjaxResult getRevenueInfo(@PathVariable("projectId") Long projectId) {
    return success(projectService.selectProjectByProjectId(projectId));
}
```

**7.1.3 更新收入确认信息**
```java
/**
 * 更新公司收入确认信息
 */
@PreAuthorize("@ss.hasPermi('revenue:company:edit')")
@Log(title = "公司收入确认", businessType = BusinessType.UPDATE)
@PutMapping("/revenueConfirm")
public AjaxResult updateRevenueConfirm(@Validated @RequestBody Project project) {
    // 设置收入确认状态为已确认
    project.setRevenueConfirmStatus("1");
    // 设置确认人
    project.setCompanyRevenueConfirmedBy(SecurityUtils.getUserId().toString());
    // 设置确认时间
    project.setCompanyRevenueConfirmedTime(new Date());
    // 计算税后金额
    if (project.getConfirmAmount() != null && project.getTaxRate() != null) {
        BigDecimal afterTax = project.getConfirmAmount()
            .divide(BigDecimal.ONE.add(project.getTaxRate().divide(new BigDecimal("100"))), 2, RoundingMode.HALF_UP);
        project.setAfterTaxAmount(afterTax);
    }
    return toAjax(projectService.updateProject(project));
}
```

**7.1.4 导出收入确认列表**
```java
/**
 * 导出公司收入确认列表
 */
@PreAuthorize("@ss.hasPermi('revenue:company:export')")
@Log(title = "公司收入确认", businessType = BusinessType.EXPORT)
@PostMapping("/revenueExport")
public void export(HttpServletResponse response, Project project) {
    List<Project> list = projectService.selectProjectList(project);
    ExcelUtil<Project> util = new ExcelUtil<>(Project.class);
    util.exportExcel(response, list, "公司收入确认数据");
}
```

---

## 八、前端API封装

### 8.1 API文件

**文件：** `ruoyi-ui/src/api/revenue/company.ts`

```typescript
import request from '@/utils/request'

// 查询公司收入确认列表
export function listRevenueCompany(query: any) {
  return request({
    url: '/project/project/revenueList',
    method: 'get',
    params: query
  })
}

// 获取项目详情（用于收入确认抽屉）
export function getRevenueCompany(projectId: number) {
  return request({
    url: '/project/project/revenue/' + projectId,
    method: 'get'
  })
}

// 更新公司收入确认信息
export function updateRevenueConfirm(data: any) {
  return request({
    url: '/project/project/revenueConfirm',
    method: 'put',
    data: data
  })
}

// 导出公司收入确认列表
export function exportRevenueCompany(query: any) {
  return request({
    url: '/project/project/revenueExport',
    method: 'post',
    params: query
  })
}
```

---

## 九、项目列表集成

### 9.1 在项目管理列表添加按钮

**文件：** `ruoyi-ui/src/views/project/project/index.vue`

**操作列修改：**

```vue
<el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
  <template #default="scope">
    <!-- 现有按钮：查看、编辑、删除等 -->

    <!-- 新增：收入确认按钮 -->
    <el-button
      v-if="scope.row.revenueConfirmStatus === '0'"
      link
      type="primary"
      icon="Money"
      @click="handleRevenueConfirm(scope.row)"
      v-hasPermi="['revenue:company:edit']"
    >收入确认</el-button>

    <el-button
      v-if="scope.row.revenueConfirmStatus === '1'"
      link
      type="success"
      icon="View"
      @click="handleViewRevenueConfirm(scope.row)"
      v-hasPermi="['revenue:company:view']"
    >查看确认</el-button>
  </template>
</el-table-column>
```

### 9.2 抽屉组件引入

```vue
<script setup>
import RevenueConfirmDrawer from './components/RevenueConfirmDrawer.vue'

// 抽屉状态
const revenueDrawerVisible = ref(false)
const revenueDrawerMode = ref('edit') // 'view' 或 'edit'
const currentProjectId = ref(null)

// 打开收入确认抽屉（编辑模式）
const handleRevenueConfirm = (row) => {
  currentProjectId.value = row.projectId
  revenueDrawerMode.value = 'edit'
  revenueDrawerVisible.value = true
}

// 打开收入确认抽屉（查看模式）
const handleViewRevenueConfirm = (row) => {
  currentProjectId.value = row.projectId
  revenueDrawerMode.value = 'view'
  revenueDrawerVisible.value = true
}

// 抽屉关闭后刷新列表
const handleRevenueDrawerClose = () => {
  revenueDrawerVisible.value = false
  getList() // 刷新列表
}
</script>

<template>
  <!-- 现有内容 -->

  <!-- 收入确认抽屉 -->
  <RevenueConfirmDrawer
    v-model="revenueDrawerVisible"
    :project-id="currentProjectId"
    :mode="revenueDrawerMode"
    @close="handleRevenueDrawerClose"
  />
</template>
```

---

## 十、文件清单

### 10.1 数据库文件

- `pm-sql/init/00_tables_ddl.sql` - 添加 `company_revenue_confirmed_time` 字段
- `pm-sql/init/02_menu_data.sql` - 添加收入确认管理菜单

### 10.2 后端文件

- `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java` - 新增4个方法
- `ruoyi-project/src/main/java/com/ruoyi/project/domain/Project.java` - 确认字段存在

### 10.3 前端文件

**新增文件：**
- `ruoyi-ui/src/views/revenue/company/index.vue` - 公司收入确认列表页
- `ruoyi-ui/src/views/project/project/components/RevenueConfirmDrawer.vue` - 收入确认抽屉组件
- `ruoyi-ui/src/api/revenue/company.ts` - API封装
- `ruoyi-ui/src/types/api/revenue/company.ts` - 类型定义

**修改文件：**
- `ruoyi-ui/src/views/project/project/index.vue` - 添加收入确认按钮

---

## 十一、开发顺序建议

1. **数据库变更**
   - 执行 ALTER TABLE 添加字段
   - 添加菜单SQL

2. **后端开发**
   - 在 ProjectController 添加4个方法
   - 测试API接口

3. **前端开发**
   - 创建 API 封装文件
   - 开发公司收入确认列表页
   - 开发收入确认抽屉组件
   - 在项目列表集成按钮

4. **联调测试**
   - 测试列表查询
   - 测试收入确认流程
   - 测试查看和编辑功能
   - 测试导出功能

5. **权限配置**
   - 为角色分配菜单权限
   - 测试权限控制

---

## 十二、注意事项

1. **税后金额计算精度**：使用 `BigDecimal` 进行计算，保留2位小数，采用四舍五入模式
2. **确认人记录**：保存时自动获取当前登录用户ID，不允许手动修改
3. **确认时间记录**：保存时自动记录当前时间，不允许手动修改
4. **状态自动更新**：保存时自动将 `revenue_confirm_status` 设置为 '1'（已确认）
5. **权限控制**：严格按照权限标识控制按钮和API访问
6. **数据验证**：前后端都要验证必填字段和数据格式
7. **列表刷新**：抽屉关闭后自动刷新列表，确保数据最新

---

**设计完成日期：** 2026-02-09
**设计人员：** Claude Code
**审核状态：** 待审核
