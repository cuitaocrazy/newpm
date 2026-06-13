# Implementation Plan: 旧数据查询

**Branch**: `009-old-version-query` | **Date**: 2026-06-14 | **Spec**: [spec.md](./spec.md)

## Summary
最简单的迁移功能：纯只读历史归档。新建独立表 `pm_old_version_out` + 一套只读后端(list + 3个下拉distinct) + 一个 index.vue。无增删改导、无级联、无版本号生成。

## Technical Context
Java 17 / TS 5.6 / Spring Boot 3.5.8 / MyBatis / Vue3 / Element Plus。新表 utf8mb4。无单元逻辑(纯查询转发)，测试以 E2E 为主。

## Constitution Check
- II 权限：`@PreAuthorize('project:oldVersionOut:list')` ✅
- III 一致性：BaseController + startPage + TableDataInfo ✅
- V 数据库：utf8mb4，无 JOIN（扁平表，无需 COLLATE）✅
- VI 前端：查询用文本框+下拉 ✅
无违规。

## Project Structure
```
后端 ruoyi-project：
- domain/OldVersionOut.java（18字段 + @Excel可选）
- mapper/OldVersionOutMapper.java(+xml)：selectList(4条件) + selectProBatchNoOptions/ProductOptions/VersionTypeOptions(distinct)
- service/IOldVersionOutService + impl（纯转发）
- controller/OldVersionOutController：/project/oldVersionOut（list + 3下拉端点）
前端 ruoyi-ui：
- views/project/oldVersionOut/index.vue（仅列表+查询，无四件套其余）
- api/project/oldVersionOut.ts
数据库：pm-sql/init/00_tables_ddl.sql 加表；02_menu_data.sql 加菜单；migration_009 可移植脚本
```

## Structure Decision
独立表、独立 Controller、仅 index.vue（无 add/edit/detail）。不碰 pm_version_out 体系。
