# 公司收入确认管理功能实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现公司收入确认管理功能，包括独立列表页、收入确认抽屉、项目列表集成

**Architecture:** 基于 RuoYi-Vue 框架，后端在 ProjectController 中新增收入确认相关接口，前端创建独立的收入确认列表页和抽屉组件，通过左右分栏展示项目信息和收入确认表单

**Tech Stack:** Spring Boot 3.5.8, MyBatis, Vue 3.5, TypeScript 5.6, Element Plus 2.13

---

## 实施任务清单

### Task 1: 数据库变更 - 添加公司收入确认时间字段

**目标：** 在 pm_project 表中添加 company_revenue_confirmed_time 字段

**文件：**
- Create: `pm-sql/revenue/01_add_company_revenue_confirmed_time.sql`
- Modify: `pm-sql/init/00_tables_ddl.sql`

**步骤：**
1. 创建变更脚本 `pm-sql/revenue/01_add_company_revenue_confirmed_time.sql`
2. 执行 SQL：`mysql -u root -p ry-vue < pm-sql/revenue/01_add_company_revenue_confirmed_time.sql`
3. 验证字段是否添加成功
4. 更新 DDL 文档
5. 提交：`git commit -m "feat(db): 添加公司收入确认时间字段"`

**SQL内容：**
```sql
ALTER TABLE pm_project
ADD COLUMN `company_revenue_confirmed_time` datetime DEFAULT NULL COMMENT '公司收入确认时间';
```

---

### Task 2: 添加收入确认管理菜单和权限

**目标：** 添加一级菜单、二级菜单和4个按钮权限

**文件：**
- Modify: `pm-sql/init/02_menu_data.sql`

**步骤：**
1. 在文件末尾追加菜单SQL（见设计文档第三章）
2. 执行SQL或在数据库客户端执行
3. 验证菜单是否添加成功
4. 提交：`git commit -m "feat(menu): 添加公司收入确认管理菜单"`

**菜单结构：**
- 收入确认管理（一级）
  - 公司收入确认（二级）
    - 查询、查看、编辑、导出（按钮权限）

---

### Task 3: 后端实体类 - 添加公司收入确认时间字段

**目标：** 在 Project.java 中添加 companyRevenueConfirmedTime 字段

**文件：**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/domain/Project.java`

**步骤：**
1. 添加字段声明（带注解）
2. 添加 getter/setter 方法
3. 更新 toString 方法
4. 编译验证：`mvn clean compile -pl ruoyi-project -am`
5. 提交：`git commit -m "feat(entity): Project实体类添加公司收入确认时间字段"`

**代码片段：**
```java
/** 公司收入确认时间 */
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
@Excel(name = "公司收入确认时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
private Date companyRevenueConfirmedTime;
```

---

### Task 4: 后端Mapper - 添加字段映射

**目标：** 在 ProjectMapper.xml 中添加字段映射

**文件：**
- Modify: `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml`

**步骤：**
1. 在 resultMap 中添加字段映射
2. 在查询字段列表中添加字段
3. 在 insert 语句中添加字段
4. 在 update 语句中添加字段
5. 提交：`git commit -m "feat(mapper): ProjectMapper添加公司收入确认时间字段映射"`

---

### Task 5: 后端Controller - 添加收入确认接口

**目标：** 在 ProjectController.java 中添加4个收入确认接口

**文件：**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/controller/ProjectController.java`

**步骤：**
1. 添加收入确认列表查询接口（`/revenueList`）
2. 添加获取收入确认详情接口（`/revenue/{projectId}`）
3. 添加更新收入确认信息接口（`/revenueConfirm`）
4. 添加导出收入确认列表接口（`/revenueExport`）
5. 添加必要的 import
6. 编译验证：`mvn clean compile -pl ruoyi-project -am`
7. 提交：`git commit -m "feat(controller): 添加公司收入确认相关接口"`

**关键逻辑：**
- 自动设置 `revenue_confirm_status = '1'`
- 自动设置 `company_revenue_confirmed_by = 当前用户ID`
- 自动设置 `company_revenue_confirmed_time = 当前时间`
- 自动计算 `after_tax_amount = confirm_amount / (1 + tax_rate/100)`

---

### Task 6: 前端API - 创建收入确认API封装

**目标：** 创建 API 封装文件

**文件：**
- Create: `ruoyi-ui/src/api/revenue/company.ts`

**步骤：**
1. 创建文件并编写4个API方法
2. 提交：`git commit -m "feat(api): 添加公司收入确认API封装"`

**API方法：**
- `listRevenueCompany()` - 列表查询
- `getRevenueCompany()` - 获取详情
- `updateRevenueConfirm()` - 更新确认
- `exportRevenueCompany()` - 导出数据

---

### Task 7: 前端列表页 - 公司收入确认列表

**目标：** 创建公司收入确认列表页

**文件：**
- Create: `ruoyi-ui/src/views/revenue/company/index.vue`

**步骤：**
1. 创建页面骨架（查询表单+工具栏+数据表格）
2. 实现查询功能
3. 实现列表展示（10个字段）
4. 实现导出功能
5. 实现操作列按钮（根据状态显示不同按钮）
6. 测试页面：`npm run dev`
7. 提交：`git commit -m "feat(frontend): 添加公司收入确认列表页"`

**查询条件：**
- 项目名称、项目部门、收入确认年度、收入确认状态、项目分类、一级区域、项目经理

**列表字段：**
- 项目编号、项目名称、项目部门、确认金额、税率、税后金额、确认年度、确认状态、确认人、确认时间

---

### Task 8: 前端抽屉组件 - 收入确认抽屉

**目标：** 创建收入确认抽屉组件（左右分栏布局）

**文件：**
- Create: `ruoyi-ui/src/views/revenue/company/components/RevenueConfirmDrawer.vue`

**步骤：**
1. 创建抽屉组件骨架
2. 实现左侧项目信息展示（6个卡片）
3. 实现右侧收入确认表单（6个字段）
4. 实现查看模式和编辑模式切换
5. 实现税后金额自动计算
6. 实现税率快捷按钮（6%、13%）
7. 实现保存逻辑
8. 测试抽屉功能
9. 提交：`git commit -m "feat(frontend): 添加收入确认抽屉组件"`

**布局比例：** 左侧60% + 右侧40%

**左侧卡片：**
1. 项目基本信息
2. 人员配置
3. 客户信息
4. 时间规划
5. 成本预算
6. 备注

**右侧表单：**
1. 收入确认状态（只读显示）
2. 收入确认年度（必填）
3. 确认金额（含税）（必填）
4. 税率（必填，带快捷按钮）
5. 税后金额（自动计算）
6. 备注（可选）

---

### Task 9: 前端集成 - 项目列表添加收入确认按钮

**目标：** 在项目管理列表的操作列中添加收入确认按钮

**文件：**
- Modify: `ruoyi-ui/src/views/project/project/index.vue`

**步骤：**
1. 引入 RevenueConfirmDrawer 组件
2. 在操作列添加按钮（根据 revenue_confirm_status 显示不同按钮）
3. 实现按钮点击事件
4. 实现抽屉关闭后刷新列表
5. 测试集成功能
6. 提交：`git commit -m "feat(frontend): 项目列表集成收入确认按钮"`

**按钮逻辑：**
- 未确认（status='0'）：显示"收入确认"按钮（蓝色）
- 已确认（status='1'）：显示"查看确认"按钮（绿色）

---

### Task 10: 功能测试 - 完整流程联调

**目标：** 测试完整功能流程

**步骤：**
1. 启动后端：`java -jar ruoyi-admin/target/ruoyi-admin.jar`
2. 启动前端：`cd ruoyi-ui && npm run dev`
3. 测试菜单访问（收入确认管理 > 公司收入确认）
4. 测试列表查询功能
5. 测试收入确认流程（未确认项目）
6. 测试查看和编辑功能（已确认项目）
7. 测试导出功能
8. 测试权限控制（不同角色）
9. 测试税后金额自动计算
10. 测试数据验证（必填项、格式校验）
11. 记录测试结果
12. 修复发现的问题
13. 最终提交：`git commit -m "test: 完成公司收入确认功能测试"`

**测试场景：**
- ✅ 列表页面正常显示
- ✅ 查询条件正常工作
- ✅ 未确认项目显示"收入确认"按钮
- ✅ 已确认项目显示"查看确认"按钮
- ✅ 抽屉正常打开和关闭
- ✅ 项目信息正确展示
- ✅ 收入确认表单正常编辑
- ✅ 税后金额自动计算正确
- ✅ 税率快捷按钮正常工作
- ✅ 保存后状态自动更新为"已确认"
- ✅ 确认人和确认时间自动记录
- ✅ 导出功能正常
- ✅ 权限控制正常

---

## 实施注意事项

1. **税后金额计算精度**：使用 `BigDecimal` 进行计算，保留2位小数，采用四舍五入模式
2. **确认人记录**：保存时自动获取当前登录用户ID，不允许手动修改
3. **确认时间记录**：保存时自动记录当前时间，不允许手动修改
4. **状态自动更新**：保存时自动将 `revenue_confirm_status` 设置为 '1'（已确认）
5. **权限控制**：严格按照权限标识控制按钮和API访问
6. **数据验证**：前后端都要验证必填字段和数据格式
7. **列表刷新**：抽屉关闭后自动刷新列表，确保数据最新
8. **字符集问题**：注意 MyBatis XML 中的字符集转换（参考 CLAUDE.md 常见陷阱）

---

## 参考文档

- 设计文档：`docs/plans/2026-02-09-company-revenue-confirmation-design.md`
- 项目文档：`CLAUDE.md`
- 代码生成规范：`.claude/skills/ruoyi-gen/SKILL.md`

---

**计划创建日期：** 2026-02-09
**预计工时：** 1-2天
**实施人员：** 开发团队
