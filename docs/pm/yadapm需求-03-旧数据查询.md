# yadapm 需求梳理 ③ 出入库版本管理 → 旧数据查询

> 来源：逐文件精读老系统源码（OldStorageController/Service/Dao/Model/Query + oldStorage/list.html）
> 模块：出入库版本管理（一级）→ 旧数据查询（二级），老系统 URL 前缀 `/oldStorage`
> 整理日期：2026-06-11　｜　状态：需求梳理（不开发），供评审

---

## 一、模块概述

**纯只读的历史归档查询页**。展示从旧系统迁移过来的"出入库版本"历史数据，供检索查看。**没有新增/编辑/删除/导出，也没有任何级联联动**，是出入库三个菜单里最简单的一个。

- 权限（Shiro）：`oldStorageManagementQuery`（仅查询，单一权限）。
- 独立数据表 `T_B_OLD_VERSION_OUT`（**与 `T_B_VERSION_OUT` 不是同一张表**）。
- 数据来源：**100% 来自数据迁移**。新系统内无任何写入入口；不迁数据则此页为空。

## 二、页面与接口

| 页面 | URL | 说明 |
|---|---|---|
| 列表/查询 | `/oldStorage/list`、`/oldStorage/query` | 仅此两个端点，均为查询 |

无新增/编辑/删除/导出/AJAX。

## 三、查询条件（4 个）

| 条件 | 字段 | 控件 |
|---|---|---|
| 任务编号 | taskNo | 下拉（来自历史数据 distinct） |
| 投产批次号 | proBatchNo | 下拉（distinct） |
| 子产品 | product | 下拉 |
| 版本类型 | versionType | 下拉 |

## 四、列表列（18 列）

子系统名称、子产品名称、基准版本号、出入库版本号、版本类型、版本编号、提交人员、版本投产日期、版本说明、备注、任务编号、任务名称、投产年份、投产批次号、是否涉及TWS改造、数据库是否修改、接口是否修改、顺序号。

> 无"操作"列（只读，无行级操作）。

## 五、数据表 `T_B_OLD_VERSION_OUT` 字段（来自 OldStorage 模型）

id、sysName(子系统)、product(子产品)、baseVersioncode(基准版本号)、outLibVersion(出入库版本号)、versionType(版本类型)、versionCode(版本编号)、commName(提交人员)、versionPDate(版本投产日期)、versionDescr(版本说明)、remarks(备注)、taskNo(任务编号)、taskName(任务名称)、proYear(投产年份)、proBatchNo(投产批次号)、isInvolved(是否涉及TWS改造)、dbUpdate(数据库是否修改)、usbUpdate(接口是否修改)、sequenceNo(顺序号)。

**注意**：本表是**扁平快照**，所有字段都是迁移时定格的文本/值，不与任务库/项目库/字典做实时关联（连提交人员都是存的名字文本，非 user_id）。

## 六、核心业务规则

- 纯查询，无任何业务逻辑（无版本号生成、无校验、无审核、无级联）。
- 列表分页展示，按查询条件过滤。

## 七、落地 newpm 的设计考量（开发阶段，先记录）

1. **新建独立只读表 `pm_old_version_out`**（字段照搬上表），不复用 `pm_version_out`（结构与语义都不同，是历史扁平快照）。
2. **后端**：仅 `GET /project/oldVersionOut/list`（startPage + 4 条件查询），无写接口。
3. **前端**：仅 `index.vue`（查询表单 + 列表 + 分页），无新增/编辑/详情/导出按钮。
4. **菜单**：出入库版本管理一级菜单下二级"旧数据查询"，权限 `project:oldVersionOut:list`（单一查询权限）。
5. **强依赖数据迁移（US5）**：本功能价值完全取决于迁移脚本是否把 `T_B_OLD_VERSION_OUT` 导入 `pm_old_version_out`。功能可先建空表上线，数据待旧库到位后导入。
6. **字段保真**：提交人员等存文本快照即可，不强行映射到 sys_user；版本类型若老数据存的是名称文本，按文本展示（或迁移时映射成字典值，二选一，待定）。

## 八、出入库版本管理三件套 —— 收尾总览

| 二级菜单 | 表 | 写操作 | 任务联动 | 特点 |
|---|---|---|---|---|
| 批次版本管理 | pm_version_out (manual_input=0) | 增删改导 | 任务下拉+回显+项目审核校验 | 最复杂，✅ 已实现(spec 007) |
| 非批次版本管理 | pm_version_out (manual_input=1) | 增删改导 | 任务全手填 | 中等，砍掉组包/状态/简介/多任务 |
| 旧数据查询 | pm_old_version_out | **只读** | 无 | 最简单，纯历史归档 |
