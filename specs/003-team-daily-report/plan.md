# Implementation Plan: 团队日报

**Branch**: `003-team-daily-report` | **Date**: 2026-03-19 | **Spec**: spec.md

## Summary

为项目管理系统新增「团队日报」页面，供管理层从**团队视角**按部门+月份查看项目成员日报。采用日历卡片式布局（行=项目×成员，列=日期，格内显示工时），并展示实际人天 vs 预算人天对比、有无合同项目的收入标识。

后端新增 2 个接口（`teamMonthly`、`teamProjectOptions`），复用现有部门树和数据权限机制，不新建数据表。

## Technical Context

**Language/Version**: Java 17 / TypeScript 5.6
**Primary Dependencies**: Spring Boot 3.5.8 + MyBatis / Vue 3.5 + Element Plus 2.13
**Storage**: MySQL 8.x（只读查询，无新表）
**Testing**: 手动集成测试（Playwright 可扩展）
**Target Platform**: K3s（生产）/ 本地 Docker（开发）
**Project Type**: Web application（全栈）
**Performance Goals**: 月度数据查询 < 3s（正常部门规模 ≤ 50 人）
**Constraints**: 必须经过数据权限过滤；部门树只显三级及以下
**Scale/Scope**: 单次查询范围：1个部门+子部门，1个月，预估 ≤ 100 人行

## Constitution Check

| 原则 | 检查 | 状态 |
|------|------|------|
| I. 业务完整性 | 只读查询，无 mutating 操作，不需要 `@Log` 注解 | ✅ |
| II. 权限驱动 | Controller 加 `@PreAuthorize`；部门树用代理接口 `deptTree` | ✅ |
| III. API 一致性 | 继承 `BaseController`；单对象返回 `AjaxResult` | ✅ |
| IV. 关注点分离 | 不向 `pm_project` 添加新字段；任务字段在 `pm_task` | ✅ |
| V. 数据库规范 | JOIN sys_* 表时加 `COLLATE utf8mb4_unicode_ci`；部门用 ancestors 层级 | ✅ |
| VI. 前端规范 | 部门选择用 `<ProjectDeptSelect />`；字典用 `<dict-tag>` | ✅ |

**GATE PASS** — 无宪法违规，可进入实现。

## Project Structure

### Documentation (this feature)

```text
specs/003-team-daily-report/
├── plan.md              # 本文件
├── research.md          # Phase 0 研究结果
├── data-model.md        # VO设计 + SQL草稿
├── contracts/
│   └── api.md           # API 接口契约 + 菜单SQL
└── tasks.md             # Phase 2（/speckit.tasks 生成）
```

### Source Code

```text
# 后端（ruoyi-project 模块）
ruoyi-project/src/main/java/com/ruoyi/project/
├── controller/
│   └── DailyReportController.java          # 新增 2 个端点
├── service/
│   ├── IDailyReportService.java            # 新增 2 个方法签名
│   └── impl/DailyReportServiceImpl.java    # 实现聚合逻辑
├── domain/
│   └── vo/
│       ├── TeamDailyReportVO.java          # 新建 VO
│       └── TeamMemberDailyVO.java          # 新建 VO
└── mapper/
    └── DailyReportMapper.java              # 新增 2 个方法

ruoyi-project/src/main/resources/mapper/project/
└── DailyReportMapper.xml                   # 新增 teamMonthly + teamProjectOptions SQL

# 前端（ruoyi-ui）
ruoyi-ui/src/api/project/
└── dailyReport.ts                          # 新增 getTeamMonthly + getTeamProjectOptions

ruoyi-ui/src/views/project/dailyReport/
└── teamReport.vue                          # 新建页面
```

## Complexity Tracking

> 无宪法违规，本节不适用。
