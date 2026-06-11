# Implementation Plan: 批次版本管理（出入库版本）

**Branch**: `007-batch-version-management` | **Date**: 2026-06-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/007-batch-version-management/spec.md`
**参考**: [reference-version-algorithm.md](./reference-version-algorithm.md)（版本号算法源码提取）、`docs/plans/2026-06-10-yadapm-migration-requirements.md`（全量盘点）

## Summary

迁移老 yadapm 系统"出入库版本管理 → 批次版本管理"（`/storage/*`）到 newpm。核心是批次版本记录的 CRUD + Excel 导出 + **出入库版本号按 6 种版本类型自动生成**（最高复杂度），并复用 newpm 既有的 `pm_task`（任务）、`pm_production_batch`（投产批次）、字典体系。数据迁移本轮延后（待旧库数据到位）。

技术路线：标准 RuoYi 三层（Controller→Service→Mapper），新建 `pm_version_out` 主表 + `pm_version_out_task` 任务关联子表 + `pm_sys_name` 子系统配置表；新增 3 个字典；版本号生成逻辑端口为独立 Java service 方法并以特征测试逐类型锁定；前端 Vue3 + Element Plus 四件套页面。

## Technical Context

**Language/Version**: Java 17（后端）/ TypeScript 5.6（前端）
**Primary Dependencies**: Spring Boot 3.5.8、MyBatis、Spring Security、Apache POI（Excel）、Vue 3.5、Element Plus 2.13、Pinia
**Storage**: MySQL 8.x（`ry-vue`，utf8mb4_0900_ai_ci）、Redis（字典缓存）
**Testing**: JUnit 5 + Mockito（service 特征测试）、Playwright（E2E）
**Target Platform**: K3s 集群（namespace `newpm`），Web
**Project Type**: Web（前后端分离，RuoYi-Vue 单体）
**Performance Goals**: 常规数据量列表查询用户无感等待；版本号生成毫秒级
**Constraints**: 出入库版本号同子系统+版本类型唯一、并发新增不撞号；新表与系统表 JOIN 须加 COLLATE
**Scale/Scope**: 单模块、1 个主页面 + 4 个子页面（list/add/edit/detail）、约 6 个后端端点 + 5 个级联端点

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

对照 `.specify/memory/constitution.md` v1.0.0 六大原则：

| 原则 | 合规设计 | 结论 |
|---|---|---|
| I. 业务完整性优先 | 新增/编辑/删除均加 `@Log`；采用软删除 `del_flag='1'`（本表非硬删除例外） | ✅ |
| II. 权限驱动访问控制 | 所有接口 `@PreAuthorize("@ss.hasPermi('project:versionOut:*')")`；提交人员/部门用 `/project/project/users`、`deptTree` 代理 API，不直连 `system/` | ✅ |
| III. API 与代码一致性 | Controller 继承 `BaseController`，list 首行 `startPage()`；返回 `TableDataInfo`/`AjaxResult`；Service 命名 `select*List/insert*/update*/delete*ByIds`；前端 `import request from '@/utils/request'` | ✅ |
| IV. 任务与项目解耦 | 版本号/任务字段落新表 `pm_version_out`，**不向 `pm_project` 加任何字段**；任务关联 FK `pm_task.task_id` | ✅ |
| V. 数据库规范 | 新表 utf8mb4；与 `sys_user`/`sys_dict_data` JOIN 加 `COLLATE utf8mb4_unicode_ci`；若有部门过滤用 ancestors 层级 | ✅ |
| VI. 前端组件与字典规范 | 版本类型/组包方式/版本状态用 `<dict-select>`/`<dict-tag>`；提交人员用 `<user-select post-code="pm">`；服务端排序 `orderByColumn`+`isAsc` | ✅ |
| 安全与数据治理 | 无文件上传；JWT 框架自动；新字典改动后刷 Redis `sys_dict:*` 缓存 | ✅ |

**Gate 结论**：无违规，无需 Complexity Tracking。唯一需主动改进点 —— 老系统版本号生成存在并发竞态，newpm 以"`out_lib_version` 唯一约束 + 生成失败重试"消除（属增强，不违宪）。

## Project Structure

### Documentation (this feature)

```text
specs/007-batch-version-management/
├── plan.md                        # 本文件
├── spec.md                        # 需求规格
├── reference-version-algorithm.md # 版本号算法源码提取
├── research.md                    # Phase 0 输出
├── data-model.md                  # Phase 1 输出
├── quickstart.md                  # Phase 1 输出
├── contracts/                     # Phase 1 输出（REST 端点契约）
│   └── version-out-api.md
├── checklists/requirements.md     # specify 阶段质量清单
└── tasks.md                       # /speckit-tasks 输出（本命令不生成）
```

### Source Code (repository root)

```text
后端（ruoyi-project 模块，com.ruoyi.project）：
ruoyi-project/src/main/java/com/ruoyi/project/
├── domain/
│   ├── VersionOut.java            # 批次版本主实体（extends BaseEntity）
│   ├── VersionOutTask.java        # 版本-任务关联
│   └── SysName.java               # 子系统配置
├── controller/
│   └── VersionOutController.java  # /project/versionOut/**
├── service/
│   ├── IVersionOutService.java
│   ├── ISysNameService.java
│   └── impl/
│       ├── VersionOutServiceImpl.java
│       ├── VersionNumberGenerator.java   # 版本号生成算法（独立、可测）
│       └── SysNameServiceImpl.java
└── mapper/
    ├── VersionOutMapper.java
    └── SysNameMapper.java
ruoyi-project/src/main/resources/mapper/project/
├── VersionOutMapper.xml
└── SysNameMapper.xml
ruoyi-project/src/test/java/com/ruoyi/project/service/impl/
└── VersionNumberGeneratorTest.java       # 6 类型特征测试

前端（ruoyi-ui）：
ruoyi-ui/src/views/project/versionOut/
├── index.vue     # 列表 + 查询 + 导出
├── add.vue       # 新增（含级联 + 版本号实时生成 + 多任务行）
├── edit.vue      # 编辑
└── detail.vue    # 详情
ruoyi-ui/src/api/project/versionOut.ts

数据库（pm-sql）：
pm-sql/init/00_tables_ddl.sql   # 追加 pm_version_out / pm_version_out_task / pm_sys_name
pm-sql/init/02_menu_data.sql    # 追加菜单 + 权限点
pm-sql/init/01_tables_data.sql  # 追加字典 sys_version_type / sys_package_mode / sys_version_status
```

**Structure Decision**: 沿用 RuoYi 标准 Web 分层，全部业务代码入 `ruoyi-project`（不入 `ruoyi-admin`）。版本号算法抽为独立 `VersionNumberGenerator`，与 DAO 解耦，便于纯单元测试覆盖 6 种类型。

## Complexity Tracking

> 无宪法违规，本节留空。
