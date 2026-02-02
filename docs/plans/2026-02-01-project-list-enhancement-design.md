# 项目管理列表功能增强设计方案

**日期：** 2026-02-01
**作者：** Claude & 用户
**状态：** 已确认

## 一、背景

当前项目管理模块已有基础的CRUD功能，但查询条件和列表展示与需求文档存在差异。本次增强聚焦于：
1. 优化查询条件，提升用户体验
2. 完善列表字段展示
3. 添加金额汇总功能

## 二、查询条件设计

### 2.1 查询字段（9个）

| 序号 | 字段名称 | 组件类型 | 数据源 | 说明 |
|------|---------|---------|--------|------|
| 1 | 项目名称 | `el-autocomplete` | 远程搜索 | 智能提示已有项目名称 |
| 2 | 行业 | `el-select` | 字典：industry | 下拉选择 |
| 3 | 区域 | `el-select` | 字典：sys_yjqy | 下拉选择 |
| 4 | 项目状态 | `el-select` | 字典：sys_xmjd | 下拉选择 |
| 5 | 项目部门 | `el-tree-select` | sys_dept表 | 树形选择器 |
| 6 | 确认年度 | `el-select` | 字典：sys_jdgl | 默认当年 |
| 7 | 审核状态 | `el-select` | 字典：sys_shzt | 下拉选择 |
| 8 | 合同状态 | `el-select` | 字典：sys_htzt | 下拉选择（暂不可用，待合同表创建） |
| 9 | 立项时间 | `el-date-picker` | - | 日期范围，格式：YYYY-MM-DD |

### 2.2 删除的查询条件

以下字段从查询表单中移除，简化用户界面：
- 项目编号、简称、年份
- 项目分类、预估工作量、实际工作量
- 各类预算和费用字段
- 其他非核心查询条件

## 三、列表展示设计

### 3.1 金额汇总行

**显示位置：** 表头和第一行数据之间

**实现方式：** 后端返回数据时，第一条记录为汇总数据（标记 `isSummary: true`），前端用特殊样式渲染

**汇总字段：**
- 项目预算汇总（project_budget）
- 确认金额(含税)汇总（confirm_amount）

**样式：**
- 背景色：浅灰色
- 字体：加粗
- 第一列显示"汇总"文字

### 3.2 列表字段（21个）

| 序号 | 字段名称 | 数据库字段 | 说明 |
|------|---------|-----------|------|
| 1 | 序号 | - | 自动编号 |
| 2 | 项目名称 | project_name | - |
| 3 | 项目ID | project_code | - |
| 4 | 项目全称 | project_full_name | - |
| 5 | 审核状态 | approval_status | 字典翻译 |
| 6 | 预算人天 | estimated_workload | 数字格式化 |
| 7 | 实际人天 | actual_workload | 数字格式化 |
| 8 | 阶段状态 | project_status | 字典翻译 |
| 9 | 项目预算 | project_budget | 金额格式化 |
| 10 | 验收状态 | acceptance_status | 字典翻译 |
| 11 | 确认年度 | confirm_quarter | - |
| 12 | 确认金额(含税) | confirm_amount | 金额格式化 |
| 13 | 投产日期 | production_date | 日期格式化 |
| 14 | 项目部门 | project_dept | 关联查询部门名称 |
| 15 | 客户名称 | customer_id | 关联查询客户名称 |
| 16 | 项目经理 | project_manager_id | 关联查询用户名 |
| 17 | 市场经理 | market_manager_id | 关联查询用户名 |
| 18 | 创建日期 | create_time | 日期时间格式化 |
| 19 | 创建人 | create_by | - |
| 20 | 最后更新日期 | update_time | 日期时间格式化 |
| 21 | 最后更新人 | update_by | - |

### 3.3 暂不显示的字段

以下字段需要合同表支持，暂不显示：
- 合同状态
- 合同金额(含税)
- 合同金额(不含税)
- 合同签订日期

## 四、后端改动

### 4.1 新增接口

**1. 项目名称列表接口**
```
GET /project/project/nameList?projectName={keyword}
返回：List<String> 项目名称列表（最多20条）
```

**2. 金额汇总接口**
```
GET /project/project/summary?{查询条件}
返回：{
  projectBudgetSum: BigDecimal,
  confirmAmountSum: BigDecimal
}
```

### 4.2 修改接口

**列表查询接口增强**
```
GET /project/project/list
```

**改动点：**
1. 添加关联查询：
   - LEFT JOIN pm_customer 获取客户名称
   - LEFT JOIN sys_user (project_manager) 获取项目经理名
   - LEFT JOIN sys_user (market_manager) 获取市场经理名
   - LEFT JOIN sys_dept 获取部门名称

2. 返回数据第一条为汇总行（标记 `isSummary: true`）

3. 支持新的查询条件：
   - 项目名称模糊查询
   - 立项时间范围查询（startDate, endDate）
   - 确认年度查询
   - 审核状态查询
   - 合同状态查询（暂时无效）

### 4.3 Mapper改动

**ProjectMapper.xml 修改：**

1. `selectProjectList` 添加关联查询
2. `selectProjectSummary` 新增汇总查询SQL

## 五、前端改动

### 5.1 查询表单重构

**文件：** `ruoyi-ui/src/views/project/project/index.vue`

**改动：**
1. 删除多余的查询条件
2. 修改输入框为下拉框（行业、区域、项目状态、审核状态、合同状态、确认年度）
3. 项目名称改为 `el-autocomplete`
4. 项目部门改为 `el-tree-select`
5. 添加立项时间范围选择器

### 5.2 列表表格重构

**改动：**
1. 调整列定义，只保留21个字段
2. 添加汇总行渲染逻辑（第一行特殊样式）
3. 添加关联字段显示（客户名称、项目经理、市场经理、部门名称）
4. 金额和日期格式化

### 5.3 新增方法

1. `getProjectNameList(keyword)` - 获取项目名称列表
2. `getDeptTree()` - 获取部门树（调用系统接口）
3. `renderSummaryRow(row)` - 渲染汇总行样式

## 六、数据字典

需要确认以下字典类型存在：

| 字典类型 | 说明 | 示例值 |
|---------|------|--------|
| industry | 行业 | 金融、制造、零售 |
| sys_yjqy | 区域 | 华北、华东、华南 |
| sys_xmjd | 项目状态 | 立项、实施、验收 |
| sys_jdgl | 确认年度 | 2026年Q1、2026年Q2 |
| sys_shzt | 审核状态 | 待审核、已通过、已拒绝 |
| sys_htzt | 合同状态 | 待签订、已签订、已终止 |
| sys_yszt | 验收状态 | 未验收、已验收 |

## 七、实施步骤

1. **后端开发**
   - 修改 ProjectMapper.xml 添加关联查询
   - 新增 nameList 和 summary 接口
   - 修改 list 接口返回汇总行

2. **前端开发**
   - 重构查询表单
   - 重构列表表格
   - 添加汇总行渲染

3. **测试验证**
   - 查询条件功能测试
   - 列表展示测试
   - 金额汇总准确性测试

4. **数据库准备**
   - 确认字典数据完整性
   - 测试数据准备

## 八、后续规划

待合同表创建后，需要补充：
1. 合同状态查询条件启用
2. 列表中显示合同相关字段
3. 项目详情页展示合同信息
