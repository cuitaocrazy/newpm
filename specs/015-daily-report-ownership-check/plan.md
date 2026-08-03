# Implementation Plan: 日报保存的工时保护与项目归属校验

**Branch**: `015-daily-report-ownership-check` | **Date**: 2026-07-30 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/015-daily-report-ownership-check/spec.md`

## Summary

日报保存当前采用「整天替换」（删该日全部明细 → 按提交内容重建），而前端只提交「填报人当前可见」的
工时行，两者对"完整"的定义不一致，导致不可见工时（主要是已结项项目的历史工时）被静默删除——
已确证造成 11 组 / 61 小时 / 7.63 人天的工时丢失，且现存 133 条日报仍在等着触发。同时保存路径
完全不校验工时所填项目是否与填报人相关，使 206 个有填报权限的账号均可篡改任意项目的实际人天。

**技术方案**：服务端在保存时自行计算「可填项目集合」（复用 `selectMyProjects()` 同一口径）作为
本次操作的**作用范围**——范围内的既有明细按提交内容替换，范围外的原样保留；同时对提交明细做三项
批量校验（曾是成员／项目未结项／任务归属正确），任一不通过即拒绝整次保存。**删除整条日报走同一
保护范围**：仅删范围内的明细，若有残留则保留日报主记录并重算其当日汇总工时（否则被保留的工时会
成为无主记录可归属的孤立数据）。前端零改动，所有写入入口自动获得保护。

## Technical Context

**Language/Version**: Java 17（后端）
**Primary Dependencies**: Spring Boot 3.5.8、MyBatis、Spring Security（均为既有依赖，本特性不引入新依赖）
**Storage**: MySQL 8.x（`ry-vue`）。涉及表：`pm_daily_report_detail`、`pm_project_member`、`pm_project`、`pm_task`（**均只读或既有写入，无 schema 变更**）
**Testing**: JUnit 5 + Mockito（服务层单元测试，无需 MySQL/Redis）；Playwright（API 驱动 e2e）
**Target Platform**: Linux 服务端（K3s 集群 `newpm` 命名空间）
**Project Type**: Web 应用后端模块（`ruoyi-project`），本特性为纯服务层改造
**Performance Goals**: 保存日报新增 3 次批量查询；生产实测单日报明细 1–6 条、涉及项目最多 6 个，
新增开销可忽略（目标：保存接口耗时增加 < 10ms）
**Constraints**: 前端零改动（spec Assumptions）；不得破坏填报人主动清零工时的能力（FR-002）；
不得引入新的人天对账偏差（SC-008）
**Scale/Scope**: 206 个有填报权限的账号；现存 12280 条日报、15661 条项目工时明细；
待保护的高风险日报 133 条

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原则 | 相关要求 | 本特性符合性 |
|---|---|---|
| **I. 业务完整性优先** | mutating 接口 MUST 加 `@Log` | ✅ `saveDailyReport` 已有 `@Log(title="日报管理", businessType=INSERT)`，本特性不新增接口。拒绝事件的可追溯性依托该注解写入 `sys_oper_log`（FR-012），无需新增审计表 |
| | `pm_daily_report_detail` 属硬删除例外表 | ✅ 本特性**收窄**硬删除的作用范围（从"该日全部"改为"作用范围内"），不改变硬删除这一性质，符合既有约定 |
| **II. 权限驱动访问控制** | Controller MUST 有 `@PreAuthorize` | ✅ 无新增 Controller；`save` 接口既有 `@ss.hasAnyPermi('project:dailyReport:add,project:dailyReport:edit')` 不变 |
| | 部门数据隔离用 `@DataScope` | ⚪ 不适用：本特性是「人 ↔ 项目」的身份关系校验，非部门维度数据隔离（团队日报的 `@DataScope` 口径问题属 Issue #6 问题一，另开 feature） |
| **III. API 与代码一致性** | 业务异常抛 `ServiceException` | ✅ 校验失败抛 `ServiceException`，由 `GlobalExceptionHandler` 统一转 `AjaxResult.error` |
| | Service 方法命名约定 | ✅ 新增 Mapper 方法遵循 `select*` / `delete*` 前缀 |
| **IV. 任务与项目解耦** | 禁止向 `pm_project` 加任务字段 | ✅ 无 schema 变更 |
| | `pm_project.update_by/update_time` 只能由项目编辑更新 | ✅ 工时重算走 `projectMapper.updateActualWorkload`（该 SQL 带 `update_time = update_time`，不触碰审计字段） |
| **V. 数据库规范** | PM 表与系统表 JOIN 须加 `COLLATE` | ✅ 新增查询均在 PM 表之间（`pm_project_member` / `pm_project` / `pm_task`），不跨字符集 |
| | 人天公式须含补正量 | ⚪ 不适用：本特性不新增人天展示 |
| | Schema 变更策略 | ✅ **无 schema 变更**，不需要 `fix_*.sql` 也不需改 `00_tables_ddl.sql` |
| **VI. 前端组件与字典规范** | 字典用 `<dict-select>`、禁止硬编码 | ⚪ 不适用：**前端零改动** |

**结论**：全部通过，无违规项，无需填写 Complexity Tracking。

**设计后复查（Phase 1 完成，含删除路径纳入范围后的复查）**：设计未引入新接口、新表、新依赖、
新前端代码；新增的 6 个 Mapper 方法均为同字符集内的简单查询；异常处理沿用框架统一路径；
删除路径保留主记录不改变 `pm_daily_report` 的软删除策略（宪法 I 允许该表硬删明细 + 软删主记录）。
**复查结论：仍全部通过。**

## Project Structure

### Documentation (this feature)

```text
specs/015-daily-report-ownership-check/
├── spec.md              # 需求规格（/speckit.specify 产出）
├── plan.md              # 本文件（/speckit.plan 产出）
├── research.md          # Phase 0：9 项技术决策与备选方案否决理由
├── data-model.md        # Phase 1：实体、判定规则、保存与删除流程、状态迁移矩阵
├── quickstart.md        # Phase 1：本地验证步骤（含造数与 e2e）
├── contracts/
│   ├── save-daily-report.md   # Phase 1：保存日报的行为契约（含拒绝语义）
│   └── delete-daily-report.md # Phase 1：删除日报的行为契约（含主记录保留规则）
├── bdd/
│   ├── save-daily-report.feature  # 28 个场景 + 1 场景大纲（覆盖评审用）
│   └── coverage.md                # 需求→场景覆盖矩阵
├── checklists/
│   └── requirements.md  # 规格质量检查清单（16 项已全部通过）
└── tasks.md             # Phase 2 产出（由 /speckit.tasks 生成，本命令不创建）
```

### Source Code (repository root)

```text
ruoyi-project/src/main/java/com/ruoyi/project/
├── service/impl/
│   └── DailyReportServiceImpl.java        # 核心改动：saveDailyReport 增加校验段 + 收窄删除范围
│                                          #          deleteDailyReportByIds 同样按范围删除 + 有残留则保主记录并重算汇总
├── mapper/
│   ├── DailyReportDetailMapper.java       # 新增 deleteByReportIdInScope、countByReportId
│   ├── DailyReportMapper.java             # 新增 updateTotalWorkHours
│   ├── ProjectMemberMapper.java           # 新增 selectEverMemberProjectIds
│   ├── ProjectMapper.java                 # 新增 selectProjectStatesIn（返回 id+name+stage）
│   └── TaskMapper.java                    # 新增 selectTaskProjectPairs
└── (无 Controller / Domain 改动)

ruoyi-project/src/main/resources/mapper/project/
├── DailyReportDetailMapper.xml            # 新增 delete 语句（含 <if> 空集守卫）
├── ProjectMemberMapper.xml                # 新增批量成员查询
├── ProjectMapper.xml                      # 新增批量项目状态查询
└── TaskMapper.xml                         # 新增批量任务归属查询

ruoyi-project/src/test/java/com/ruoyi/project/service/impl/
└── DailyReportServiceImplTest.java        # 更新既有 rollup 测试 + 新增 4 类回归测试

tests/
└── e2e-daily-report-ownership.spec.js     # 新增：保存防丢失 / 删除防丢失 / 越权拒绝 / 结项拒绝 / 离场可维护

ruoyi-ui/                                   # 【零改动】
pm-sql/                                     # 【零改动，无 schema 变更】
```

**Structure Decision**: 纯后端服务层改造，落在既有 `ruoyi-project` 模块内（符合宪法「所有 Java 业务
代码放入 ruoyi-project」）。不新增包、不新增分层。前端与数据库均零改动——这是 Decision 1 选择
「服务端自算作用范围」而非「前端显式声明范围」的直接收益：改动面收敛到一个 Service 方法 +
四个 Mapper 查询，且所有写入入口（含移动端 H5）自动受保护。

## Complexity Tracking

> 无宪法违规项，本节不适用。
