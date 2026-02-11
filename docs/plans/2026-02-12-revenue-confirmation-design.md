# 公司收入确认功能设计方案

**设计日期：** 2026-02-12
**业务模块：** 收入确认管理 > 公司收入确认
**设计目标：** 实现项目收入确认的完整流程，包括列表查询、收入确认、收入查看等功能

---

## 一、需求概述

### 1.1 业务背景

公司收入确认是项目管理的重要环节，用于记录和管理项目的收入确认信息，包括确认状态、确认年度、确认金额、税率及税后金额等。

### 1.2 核心需求

1. **独立菜单入口**：收入确认管理（一级菜单） > 公司收入确认（二级菜单）
2. **项目列表集成**：在项目管理列表中增加收入确认/查看入口
3. **状态驱动按钮**：根据收入确认状态显示不同操作按钮
4. **左右布局详情页**：左侧显示项目详情（只读），右侧显示收入确认信息（可编辑）
5. **自动计算税后金额**：根据确认金额和税率自动计算
6. **列表合计功能**：显示所有金额字段的合计

### 1.3 用户角色

- **财务人员**：负责收入确认，具有编辑权限
- **普通用户**：查看收入确认信息，仅有查看权限

---

## 二、整体架构

### 2.1 实现方案

采用**独立收入确认模块**方案，理由：
- 业务独立性强，收入确认是独立的业务流程
- 代码清晰，便于维护和扩展
- 不影响现有项目管理功能
- 支持后续增加收入确认特有功能（批量确认、统计报表等）

### 2.2 模块结构

**前端文件：**
```
ruoyi-ui/src/
├── views/revenue/company/
│   ├── index.vue          # 收入确认列表页
│   └── detail.vue         # 收入确认/查看详情页
└── api/revenue/
    └── company.js         # 收入确认API接口封装
```

**后端接口扩展：**
```
ProjectController 增加：
- GET  /project/project/revenue/list       # 收入确认列表
- GET  /project/project/revenue/{id}       # 收入确认详情
- PUT  /project/project/revenue            # 保存收入确认
- POST /project/project/revenue/export     # 导出收入确认
```

**数据库字段（pm_project 表）：**
- `revenue_confirm_status` VARCHAR(20) - 收入确认状态（字典：sys_srqrzt）
- `revenue_confirm_year` VARCHAR(20) - 收入确认年度（字典：sys_ndgl）
- `confirm_amount` DECIMAL(15,2) - 确认金额（含税）
- `tax_rate` DECIMAL(5,2) - 税率（%）
- `after_tax_amount` DECIMAL(15,2) - 税后金额（不含税）
- `company_revenue_confirmed_by` VARCHAR(64) - 确认人
- `company_revenue_confirmed_time` DATETIME - 确认时间

---

## 三、菜单和权限

### 3.1 现有菜单结构

```
收入确认管理（menu_id: 2074）
└── 公司收入确认（menu_id: 2075）
    ├── 公司收入确认查询（2076）- revenue:company:query
    ├── 公司收入确认查看（2077）- revenue:company:view
    ├── 公司收入确认编辑（2078）- revenue:company:edit
    └── 公司收入确认导出（2079）- revenue:company:export
```

**路由路径：** `/revenue/company`
**组件路径：** `revenue/company/index`

### 3.2 权限标识

- `revenue:company:list` - 收入确认列表查询
- `revenue:company:query` - 收入确认详情查询
- `revenue:company:view` - 收入确认查看
- `revenue:company:edit` - 收入确认编辑
- `revenue:company:export` - 收入确认导出

---

## 四、列表页设计

### 4.1 查询条件

与项目管理列表一致，并增加收入确认状态：

- 项目名称（支持自动补全）
- 项目部门（树形选择）
- 收入确认年度（sys_ndgl字典）
- 项目分类（sys_xmfl字典）
- 一级区域、二级区域
- 项目经理、市场经理
- 立项年度（sys_ndgl字典）
- 项目阶段（sys_xmjd字典）
- 审核状态（sys_spzt字典）
- 验收状态（sys_yszt字典）
- **收入确认状态（sys_srqrzt字典：未确认/已确认）** ← 新增

### 4.2 列表显示列

**基础列（复用项目管理）：**
- 序号
- 项目名称
- 项目编码
- 项目部门
- 项目经理
- 市场经理
- 项目分类
- 一级区域
- 二级区域
- 项目阶段
- 创建时间

**收入确认列（新增）：**
- 收入确认状态（dict-tag显示）
- 收入确认年度
- 确认金额（含税）
- 税后金额

**操作列：**
- 未确认(0) → "收入确认" 按钮（绿色，icon: Money）
- 已确认(1) → "收入查看" 按钮（蓝色，icon: View）

### 4.3 合计行设计

在列表第一行（表头和数据之间）显示合计行：

**合计字段：**
- 项目预算 (projectBudget)
- 预估工作量 (estimatedWorkload)
- 实际人天 (actualWorkload)
- 合同金额 (contractAmount)
- 确认金额/含税 (confirmAmount)
- 税后金额 (afterTaxAmount)

**显示规则：**
- 第一列（序号）显示"合计"文字，加粗
- 数值列显示合计值，加粗
- 其他非数值列为空
- 操作列不显示按钮

**实现方式：**
```javascript
// 计算合计行
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

// 获取列表数据后插入合计行
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

---

## 五、详情页设计

### 5.1 页面布局

**左右分栏布局（el-row + el-col）：**

```
┌─────────────────────────────────────────────────────────────┐
│  收入确认 - 项目名称                                        │
├──────────────────────────┬──────────────────────────────────┤
│                          │                                  │
│  左侧 (60%)              │  右侧 (40%)                      │
│  项目详情（只读）        │  收入确认信息（可编辑）          │
│                          │                                  │
│  【基本信息】            │  收入确认状态 *                  │
│  - 项目名称              │  [下拉选择]                      │
│  - 项目编码              │                                  │
│  - 项目部门              │  收入确认年度 *                  │
│  - 项目分类              │  [下拉选择]                      │
│  - 预估工作量            │                                  │
│  - 实际工作量            │  确认金额（含税）*               │
│  - ...                   │  [输入框] 元                     │
│                          │                                  │
│  【人员配置】            │  税率 *                          │
│  - 项目经理              │  [输入框] %                      │
│  - 市场经理              │  （默认根据项目分类设置）        │
│  - ...                   │                                  │
│                          │  税后金额（不含税）              │
│  【客户信息】            │  [自动计算，只读]                │
│  - 客户名称              │                                  │
│  - ...                   │  ─────────────────               │
│                          │  [保存] [取消]                   │
│  【时间规划】            │                                  │
│  - 启动日期              │                                  │
│  - ...                   │                                  │
│                          │                                  │
│  【成本预算】            │                                  │
│  - 项目预算              │                                  │
│  - ...                   │                                  │
└──────────────────────────┴──────────────────────────────────┘
```

### 5.2 左侧：项目详情（只读）

**展示方式：**
- 使用 `el-descriptions` 组件
- 全部展开显示（不使用折叠面板）
- 所有字段只读，纯展示

**信息分组：**
1. **基本信息**：项目名称、项目编码、项目部门、项目分类、预估工作量、实际工作量、项目阶段、验收状态、一级区域、二级区域、项目地址、项目计划
2. **人员配置**：项目经理、市场经理、销售负责人、销售联系方式、参与人员
3. **客户信息**：客户名称、客户联系人、客户联系方式、商户联系人、商户联系方式
4. **时间规划**：启动日期、结束日期、上线日期、验收日期
5. **成本预算**：项目预算、项目费用、成本预算、人工成本、采购成本

### 5.3 右侧：收入确认表单（可编辑）

**表单字段：**

| 字段 | 组件 | 必填 | 说明 |
|------|------|------|------|
| 收入确认状态 | el-select | 是 | 字典：sys_srqrzt（未确认/已确认） |
| 收入确认年度 | el-select | 是 | 字典：sys_ndgl（2025年、2026年...） |
| 确认金额（含税） | el-input-number | 是 | 单位：元，手动输入 |
| 税率 | el-input-number | 是 | 单位：%，根据项目分类初始化<br>软件开发类：6%<br>硬件销售类：13%<br>可手动修改 |
| 税后金额 | el-input | 否 | 只读，自动计算<br>公式：确认金额 / (1 + 税率/100) |

**按钮：**
- 保存：提交表单，验证通过后保存
- 取消：返回列表页

### 5.4 编辑模式与查看模式

**未确认时（收入确认模式）：**
- 右侧表单所有字段可编辑
- 按钮：保存、取消

**已确认时（收入查看模式）：**
- 右侧表单所有字段只读
- 增加"编辑"按钮，点击后切换为编辑模式
- 编辑模式下按钮：保存、取消

### 5.5 自动计算逻辑

**税率初始化：**
```javascript
// 根据项目分类初始化税率
function initTaxRate(projectCategory) {
  if (projectCategory === '1') { // 软件开发类
    form.value.taxRate = 6
  } else if (projectCategory === '2') { // 硬件销售类
    form.value.taxRate = 13
  }
}
```

**税后金额计算：**
```javascript
// 监听确认金额和税率变化，自动计算税后金额
watch(
  () => [form.value.confirmAmount, form.value.taxRate],
  ([amount, rate]) => {
    if (amount && rate != null) {
      const afterTax = amount / (1 + rate / 100)
      form.value.afterTaxAmount = afterTax.toFixed(2)
    }
  }
)
```

**计算示例：**
- 确认金额：100 元
- 税率：6%
- 税后金额：100 / (1 + 0.06) = 94.34 元

---

## 六、后端实现

### 6.1 Controller 接口

**1. 收入确认列表查询**
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

**2. 获取收入确认详情**
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

**3. 保存/更新收入确认信息**
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
    if (project.getRevenueConfirmStatus() == null) {
        return error("收入确认状态不能为空");
    }
    if (project.getRevenueConfirmYear() == null) {
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
        BigDecimal.ONE.add(taxRate.divide(new BigDecimal("100"))),
        2,
        RoundingMode.HALF_UP
    );
    project.setAfterTaxAmount(afterTaxAmount);

    return toAjax(projectService.updateProject(project));
}
```

**4. 导出收入确认列表**
```java
/**
 * 导出收入确认列表
 */
@PreAuthorize("@ss.hasPermi('revenue:company:export')")
@Log(title = "收入确认", businessType = BusinessType.EXPORT)
@PostMapping("/revenue/export")
public void export(HttpServletResponse response, Project project)
{
    List<Project> list = projectService.selectProjectList(project);
    ExcelUtil<Project> util = new ExcelUtil<Project>(Project.class);
    util.exportExcel(response, list, "收入确认数据");
}
```

### 6.2 实体类扩展

**Project.java 增加 Excel 注解：**
```java
@Excel(name = "收入确认状态", readConverterExp = "0=未确认,1=已确认")
private String revenueConfirmStatus;

@Excel(name = "收入确认年度")
private String revenueConfirmYear;

@Excel(name = "确认金额(含税)")
private BigDecimal confirmAmount;

@Excel(name = "税率(%)")
private BigDecimal taxRate;

@Excel(name = "税后金额")
private BigDecimal afterTaxAmount;
```

---

## 七、前端实现

### 7.1 API 接口封装

**src/api/revenue/company.js：**
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
    method: 'get',
    params: query
  })
}
```

### 7.2 表单验证规则

**收入确认表单验证：**
```javascript
const rules = {
  revenueConfirmStatus: [
    { required: true, message: "收入确认状态不能为空", trigger: "change" }
  ],
  revenueConfirmYear: [
    { required: true, message: "收入确认年度不能为空", trigger: "change" }
  ],
  confirmAmount: [
    { required: true, message: "确认金额不能为空", trigger: "blur" },
    { type: 'number', min: 0, message: "确认金额必须大于等于0", trigger: "blur" }
  ],
  taxRate: [
    { required: true, message: "税率不能为空", trigger: "blur" },
    { type: 'number', min: 0, max: 100, message: "税率必须在0-100之间", trigger: "blur" }
  ]
}
```

---

## 八、项目管理列表集成

### 8.1 增加收入确认入口

**修改：ruoyi-ui/src/views/project/project/index.vue**

**操作列增加按钮：**
```vue
<el-table-column label="操作" align="center" fixed="right" width="300">
  <template #default="scope">
    <template v-if="!scope.row.isSummaryRow">
      <!-- 现有按钮 -->
      <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
      <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>

      <!-- 新增：收入确认/查看按钮 -->
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

      <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
    </template>
  </template>
</el-table-column>
```

**跳转方法：**
```javascript
import { useRouter } from 'vue-router'
const router = useRouter()

// 收入确认（跳转到详情页，编辑模式）
function handleRevenue(row) {
  router.push({
    path: '/revenue/company/detail/' + row.projectId,
    query: { mode: 'edit' }
  })
}

// 收入查看（跳转到详情页，查看模式）
function handleRevenueView(row) {
  router.push({
    path: '/revenue/company/detail/' + row.projectId,
    query: { mode: 'view' }
  })
}
```

---

## 九、数据字典

### 9.1 收入确认状态（sys_srqrzt）

| 字典值 | 字典标签 |
|--------|----------|
| 0 | 未确认 |
| 1 | 已确认 |

### 9.2 收入确认年度（sys_ndgl）

| 字典值 | 字典标签 |
|--------|----------|
| dd | 待定 |
| 2025q | 2025年以前 |
| 2025 | 2025年 |
| 2026 | 2026年 |
| 2027 | 2027年 |
| 2028 | 2028年 |

---

## 十、实施步骤

### 阶段一：后端开发

1. 扩展 `ProjectController`，增加收入确认相关接口
2. 在 `Project` 实体类增加 Excel 注解
3. 测试后端接口（使用 Swagger 或 Postman）

### 阶段二：前端开发

1. 创建 API 接口文件：`src/api/revenue/company.js`
2. 创建列表页：`src/views/revenue/company/index.vue`
   - 查询条件表单
   - 列表表格（含合计行）
   - 操作按钮（收入确认/查看）
3. 创建详情页：`src/views/revenue/company/detail.vue`
   - 左侧项目详情（只读）
   - 右侧收入确认表单（可编辑）
   - 自动计算税后金额
4. 修改项目管理列表：`src/views/project/project/index.vue`
   - 增加收入确认/查看按钮

### 阶段三：测试与优化

1. 功能测试
   - 列表查询、合计行显示
   - 收入确认、收入查看
   - 税后金额自动计算
   - 项目列表跳转
2. 权限测试
   - 验证各权限标识是否生效
3. 数据验证
   - 表单验证规则
   - 必填项检查
4. 导出功能测试
   - Excel 导出内容完整性

---

## 十一、注意事项

### 11.1 开发注意事项

1. **权限标识统一**：使用 `revenue:company:*` 而非 `project:revenue:*`
2. **路由路径**：前端路由为 `/revenue/company`，不是 `/project/revenue`
3. **合计行标识**：通过 `isSummaryRow: true` 标记合计行
4. **税率精度**：使用 BigDecimal 进行计算，保留2位小数
5. **后端验证**：必填字段在后端也要验证，不能只依赖前端

### 11.2 业务规则

1. **税率设置**：
   - 软件开发类（项目分类=1）默认 6%
   - 硬件销售类（项目分类=2）默认 13%
   - 用户可手动修改税率
2. **税后金额计算**：
   - 公式：税后金额 = 确认金额 / (1 + 税率/100)
   - 实时计算，不需手动触发
3. **确认金额来源**：
   - 初始为空，用户手动输入
   - 可参考合同金额，但不自动填充

### 11.3 用户体验

1. **状态驱动按钮**：根据 `revenue_confirm_status` 显示不同按钮，用户体验更友好
2. **左右布局**：左侧项目详情全部展开，右侧表单紧凑，查看和编辑一目了然
3. **合计行**：第一行显示合计，方便财务统计
4. **自动计算**：税后金额自动计算，减少人工错误

---

## 十二、扩展性考虑

### 12.1 未来功能扩展

1. **批量确认**：支持批量选择项目进行收入确认
2. **统计报表**：按年度、部门、项目分类等维度统计收入
3. **审批流程**：收入确认需要财务主管审批
4. **历史记录**：记录收入确认的修改历史
5. **自动提醒**：未确认项目到期提醒

### 12.2 性能优化

1. **列表分页**：默认分页查询，避免一次加载过多数据
2. **合计缓存**：合计结果可考虑后端计算并缓存
3. **懒加载**：详情页左侧项目信息可按需加载

---

## 十三、总结

本设计方案采用**独立收入确认模块**架构，实现了：

✅ 收入确认列表查询（含合计行）
✅ 左右布局详情页（项目详情 + 收入确认表单）
✅ 状态驱动按钮显示（未确认→收入确认，已确认→收入查看）
✅ 自动计算税后金额
✅ 项目管理列表集成
✅ 权限控制和数据验证

该方案业务独立、代码清晰、便于维护和扩展，满足公司收入确认的全部核心需求。
