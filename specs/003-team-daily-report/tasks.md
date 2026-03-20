# Tasks: 团队日报

**Input**: Design documents from `/specs/003-team-daily-report/`
**Prerequisites**: plan.md ✅ spec.md ✅ research.md ✅ data-model.md ✅ contracts/ ✅

**Organization**: 按用户故事分阶段，每个 Phase 结束后可独立演示验收。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件，无依赖）
- **[Story]**: 所属用户故事 US1/US2/US3

---

## Phase 1: Setup（共享基础）

**Purpose**: VO 类和 API 文件骨架，所有故事依赖

- [X] T001 [P] 创建 `TeamDailyReportVO.java`（项目层聚合 VO）在 `ruoyi-project/src/main/java/com/ruoyi/project/domain/vo/TeamDailyReportVO.java`
- [X] T002 [P] 创建 `TeamMemberDailyVO.java`（成员行 VO，含 dailyHours Map）在 `ruoyi-project/src/main/java/com/ruoyi/project/domain/vo/TeamMemberDailyVO.java`
- [X] T003 [P] 在 `ruoyi-ui/src/api/project/dailyReport.ts` 末尾新增 `getTeamMonthly` 和 `getTeamProjectOptions` 两个 API 函数
- [X] T004 [P] 创建前端页面骨架（空页面 + 查询栏占位）在 `ruoyi-ui/src/views/project/dailyReport/teamReport.vue`

**Checkpoint**: VO 类编译通过，API 函数存在（接口未实现），页面路由可访问

---

## Phase 2: Foundational（后端查询层）

**Purpose**: 两个新接口的 Mapper SQL + Service，是 Phase 3/4 前端渲染的前提

**⚠️ CRITICAL**: 前端数据展示全部依赖此 Phase 完成

- [X] T005 在 `DailyReportMapper.java` 新增 `selectTeamMonthly` 和 `selectTeamProjectOptions` 方法签名
- [X] T006 在 `DailyReportMapper.xml` 实现 `selectTeamProjectOptions` SQL（部门范围 + project_name LIKE + 最多20条）
- [X] T007 在 `DailyReportMapper.xml` 实现 `selectTeamMonthly` SQL（核心：pm_project JOIN pm_project_member JOIN sys_user LEFT JOIN pm_daily_report，含 hasContract EXISTS 子查询，含 COLLATE，含 `${params.dataScope}`）
- [X] T008 在 `IDailyReportService.java` 新增 `selectTeamMonthly` 和 `selectTeamProjectOptions` 方法签名
- [X] T009 在 `DailyReportServiceImpl.java` 实现 `selectTeamProjectOptions`（加 `@DataScope(deptAlias="d", userAlias="u")`，返回 List\<Map>）
- [X] T010 在 `DailyReportServiceImpl.java` 实现 `selectTeamMonthly`（加 `@DataScope(deptAlias="d", userAlias="u")`；查询原始行后在 Java 层按 projectId → userId 聚合，生成 `List<TeamDailyReportVO>`）
- [X] T011 在 `DailyReportController.java` 新增 `GET /project/dailyReport/teamMonthly` 和 `GET /project/dailyReport/teamProjectOptions` 两个端点（加 `@PreAuthorize("@ss.hasPermi('project:dailyReport:teamList')")`）

**Checkpoint**: Swagger 可调用 `teamMonthly`，返回正确聚合结构；`teamProjectOptions` 返回项目列表

---

## Phase 3: US1 — 按部门+月份查看团队日历总览 🎯 MVP

**Goal**: 选部门+年月后，渲染日历卡片区域，每个项目×成员一行，各日期格显示工时徽章

**Independent Test**: 选择三级部门 + 2026-03 → 页面展示日历卡，有合同项目行标绿色，无数据时显示空状态

- [X] T012 [US1] 在 `teamReport.vue` 实现查询栏：`<ProjectDeptSelect />` 部门树 + 月份选择器（el-date-picker type="month"）+ 查询按钮，绑定 `queryParams.deptId` 和 `queryParams.yearMonth`
- [X] T013 [US1] 在 `teamReport.vue` 实现日历卡表格主体：`el-table` 固定列（项目名+人员）+ 动态日期列（当月1~N日），格内显示工时数（0时显示空）
- [X] T014 [US1] 在 `teamReport.vue` 实现项目行有无合同的颜色区分：`row-class-name` 回调，`hasContract=true` 的项目行加 `contract-row` CSS class（浅绿背景 + 绿色项目名标识图标）
- [X] T015 [US1] 在 `teamReport.vue` 实现空状态：无数据时显示 `el-empty` 组件提示"暂无日报数据"
- [X] T016 [US1] 在 `teamReport.vue` 接入 `getTeamMonthly` API，调通数据流（deptId 必填校验：未选部门时 toast 提示）

**Checkpoint**: P1 故事完整可演示——日历卡片渲染、有合同项目绿色标识、空状态

---

## Phase 4: US2 — 项目名称 autocomplete 筛选

**Goal**: 输入项目名关键词，autocomplete 下拉候选，选中后列表只保留该项目成员行

**Independent Test**: 已选部门，输入"项目"→ 下拉出现候选项；选中后日历区域只显示该项目的行

- [X] T017 [US2] 在 `teamReport.vue` 查询栏新增项目名称 `el-autocomplete` 组件，绑定 `queryParams.projectId` 和显示字段 `projectKeyword`；`fetchSuggestions` 调用 `getTeamProjectOptions`（防抖 300ms，`triggerOnFocus: false`）
- [X] T018 [US2] 在 `teamReport.vue` 实现清除项目名称时重置 `projectId` 并重新查询

**Checkpoint**: autocomplete 返回候选项，选中后列表正确过滤

---

## Phase 5: US3 — 实际人天 vs 预算人天对比预警

**Goal**: 每行末尾显示实际/预算人天，超 50% 时标红预警

**Independent Test**: 找实际人天 > 预算×0.5 的项目成员行，确认数值红色/警告图标；预算为0时不显示对比列

- [X] T019 [US3] 在 `teamReport.vue` 表格末尾新增两列「实际人天」和「预算人天」固定列（fixed="right"）；实际人天来自 VO 的 `actualPersonDays`，预算人天来自 `estimatedWorkload`
- [X] T020 [US3] 在 `teamReport.vue` 实现预警逻辑：`actualPersonDays > estimatedWorkload * 0.5` 时「实际人天」单元格文字变红 + 加 `el-icon` 警告图标；`estimatedWorkload` 为 null/0 时两列显示"—"

**Checkpoint**: 所有三个用户故事完整可用

---

## Phase 6: Polish & 菜单权限

**Purpose**: 收尾工作，上线前必须完成

- [X] T021 [P] 在 `pm-sql/init/02_menu_data.sql` 末尾追加团队日报菜单 SQL（parent_id 用子查询动态取，icon='date'，order_num=5，perms='project:dailyReport:teamList'）
- [X] T022 [P] 在前端路由中注册 `teamReport.vue`（在 `ruoyi-ui/src/router` 相关配置或确认由后端菜单动态生成，路径 `project/dailyReport/teamReport`）
- [X] T023 [P] 为数据量较大场景添加 loading 状态：查询期间表格区域显示 `v-loading`
- [X] T024 在本地 Docker 环境执行菜单 SQL，验证菜单出现在「日报管理」第5位

---

## Dependencies & Execution Order

### Phase 依赖

```
Phase 1 (Setup)      → 无依赖，立即开始（T001-T004 全部可并行）
Phase 2 (Foundational) → 依赖 T001/T002（VO 类）
Phase 3 (US1)        → 依赖 Phase 2 完成（需要 teamMonthly 接口）
Phase 4 (US2)        → 依赖 Phase 2 完成（需要 teamProjectOptions 接口）
Phase 5 (US3)        → 依赖 Phase 3（需要 actualPersonDays/estimatedWorkload 字段渲染）
Phase 6 (Polish)     → 依赖 Phase 3 完成（功能验证后再处理菜单权限）
```

### 用户故事依赖

- **US1 (P1)**: Phase 2 完成后可开始，无其他故事依赖
- **US2 (P2)**: Phase 2 完成后可与 US1 **并行**，接入不同组件（autocomplete vs 日历表格）
- **US3 (P3)**: 依赖 US1 完成（需要日历表格的行结构才能追加人天列）

### Phase 2 内部顺序

```
T005 (Mapper接口) → T006/T007 (SQL) → T008 (Service接口) → T009/T010 (Service实现) → T011 (Controller)
                   T006 ‖ T007 可并行
                                                            T009 ‖ T010 可并行
```

---

## Parallel Example: Phase 2

```
并行1: T006 teamProjectOptions SQL  ‖  T007 teamMonthly SQL
并行2: T009 selectTeamProjectOptions实现  ‖  T010 selectTeamMonthly实现
```

## Parallel Example: Phase 1

```
T001 TeamDailyReportVO  ‖  T002 TeamMemberDailyVO  ‖  T003 API函数  ‖  T004 页面骨架
```

---

## Implementation Strategy

### MVP（只做 US1）

1. Phase 1 Setup（全部可并行）
2. Phase 2 Foundational（按顺序）
3. Phase 3 US1（日历卡片主体）
4. **STOP & VALIDATE**: 演示日历卡片，确认数据正确、绿色标识生效
5. 视情况继续 US2/US3

### 完整交付顺序

```
Phase 1 → Phase 2 → Phase 3(US1) → Phase 4(US2) 并行 → Phase 5(US3) → Phase 6
```

---

## Notes

- T007 的 SQL 是最复杂任务：注意 `COLLATE utf8mb4_unicode_ci` on sys_user/sys_dept joins
- T010 的 Java 聚合逻辑：原始行是「项目×成员×日期」平铺，需按 projectId groupBy 后再按 userId groupBy，最后把 reportDate→workHours 写入 Map
- T013 动态列（31列）用 `v-for` 在 `el-table-column` 上动态生成，key 为日期字符串
- T021 菜单 SQL 的 parent_id 用 `SELECT menu_id FROM sys_menu WHERE menu_name='日报管理' AND parent_id=0 LIMIT 1`，不硬编码 ID
