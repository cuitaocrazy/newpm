---
description: "Task list for 旧数据查询"
---
# Tasks: 旧数据查询

最简单的迁移功能(纯只读)，任务少。

## Phase 1: Setup
- [ ] T001 `pm-sql/init/00_tables_ddl.sql` 加 `pm_old_version_out` 表(19字段:id+18展示列)；`pm-sql/migration_009_old_version_query.sql` 可移植脚本(建表+菜单)
- [ ] T002 `02_menu_data.sql` 出入库版本管理下加二级"旧数据查询"(`/storageVersion/oldVersionOut`,component `project/oldVersionOut/index`) + 权限点 `project:oldVersionOut:list`
- [ ] T003 应用 SQL 到本地

## Phase 2: 后端
- [ ] T004 domain/OldVersionOut.java(18字段)
- [ ] T005 mapper/OldVersionOutMapper.java + xml：selectOldVersionOutList(taskNo模糊+proBatchNo/product/versionType精确) + 3个 distinct 下拉查询
- [ ] T006 service/IOldVersionOutService + impl(纯转发)
- [ ] T007 controller/OldVersionOutController：GET /list(startPage) + GET /proBatchNoOptions /productOptions /versionTypeOptions

## Phase 3: 前端
- [ ] T008 api/project/oldVersionOut.ts
- [ ] T009 views/project/oldVersionOut/index.vue：查询(任务编号文本+3下拉) + 18列表格 + 分页，无操作列/无新增
- [ ] T010 菜单路由(index 是菜单直达，无需额外隐藏路由)

## Phase 4: 测试 + 收尾
- [ ] T011 E2E tests/e2e-old-version-query.spec.js：list + 3下拉端点 + 过滤(插测试数据验证)
- [ ] T012 Code Review + 前端build + 全量回归 + 提交

## Notes
- 纯只读，无单元逻辑测试(service纯转发)，测试以E2E为主。
- 数据迁移延后，E2E 用临时插入的测试数据验证查询。
