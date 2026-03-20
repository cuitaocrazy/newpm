# Tasks: 日报假期模块扩展

**Input**: Design documents from `/specs/004-daily-report-leave-types/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/api.md ✅

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 字典数据和新 DTO 类就位，所有后续任务的前提

- [x] T001 在 `pm-sql/init/01_tables_data.sql` 末尾追加婚假/产假/丧假三条 `sys_rbtype` 字典记录（dict_code 384/385/386，list_class 分别为 primary/warning/info）
- [x] T002 创建 `pm-sql/fix_004_leave_types_20260320.sql`，内容与 T001 相同，用于已部署数据库的增量更新
- [x] T003 创建新 DTO 类 `ruoyi-project/src/main/java/com/ruoyi/project/domain/request/BatchLeaveRequest.java`，包含字段：`entryType`、`startDate`、`endDate`、`leaveHoursPerDay`（默认 8）、`conflictStrategy`（默认 "skip"）及 getter/setter

**Checkpoint**: 字典数据就绪，DTO 类编译通过 → 后续开发可以开始

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 后端服务接口签名就位，前端 API 函数就位

**⚠️ CRITICAL**: US1 后端实现依赖此阶段完成

- [x] T004 在 `ruoyi-project/src/main/java/com/ruoyi/project/service/IDailyReportService.java` 中追加方法签名：`Map<String, Integer> batchSaveLeave(BatchLeaveRequest request)`（需 import BatchLeaveRequest 和 java.util.Map）
- [x] T005 [P] 在 `ruoyi-ui/src/api/project/dailyReport.js` 末尾追加 `batchSaveLeave(data)` 函数，POST 到 `/project/dailyReport/batchLeave`，设置 `headers: { repeatSubmit: false }`

**Checkpoint**: 接口签名和 API 函数就位 → 后端实现和前端弹窗可并行开发

---

## Phase 3: User Story 1 - 批量填写长假假期 (Priority: P1) 🎯 MVP

**Goal**: 用户通过「批量填假期」弹窗选择假期类型 + 日期范围，系统自动为范围内工作日生成假期记录

**Independent Test**: 打开日报填写页 → 点击「批量填假期」→ 选婚假 + 某周一到周五 → 提交 → 确认 5 天都出现婚假角标，周末跳过，且已有工时记录未被清除

### Implementation for User Story 1

- [x] T006 [US1] 在 `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java` 实现 `batchSaveLeave` 方法
- [x] T007 [US1] 在 `ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportController.java` 中追加 `batchLeave` endpoint
- [x] T008 [P] [US1] 在 `ruoyi-ui/src/views/project/dailyReport/write.vue` 的假期记录区块 header 处新增「批量填假期」按钮
- [x] T009 [US1] 在 `ruoyi-ui/src/views/project/dailyReport/write.vue` 底部新增批量填假期 `el-dialog`
- [x] T010 [US1] 在 `ruoyi-ui/src/views/project/dailyReport/write.vue` 的 `<script setup>` 区块中新增批量填写响应式状态和 `handleBatchLeave` 方法

**Checkpoint**: User Story 1 完整可测 — 批量填写功能端到端可用

---

## Phase 4: User Story 2 - 使用新假期类型记录单日假期 (Priority: P2)

**Goal**: 单日假期记录下拉中出现婚假/产假/丧假，保存后日历角标正确展示

**Independent Test**: 打开本周日期 → 假期记录区块「+ 添加假期」→ 下拉中看到婚假/产假/丧假 → 选产假 8h 保存 → 日历角标出现「产假8h」

### Implementation for User Story 2

- [ ] T011 [US2] 执行 `pm-sql/fix_004_leave_types_20260320.sql`（本地 Docker 或远端 K3s）向已运行的数据库写入三条新字典记录，验证写入后前端字典刷新（清除 Redis 字典缓存或重启后端）
- [ ] T012 [P] [US2] 验证 `ruoyi-ui/src/views/project/dailyReport/write.vue` 中假期类型下拉（`sys_rbtype.filter(d => d.value !== 'work')`）在新字典记录写入后是否自动出现婚假/产假/丧假 — 此为零代码变更的回归验证任务，若字典未更新则仅需检查 Redis 缓存
- [ ] T013 [P] [US2] 验证 `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java` 中 `saveDailyReport` 对新 entryType（marriage/maternity/bereavement）的处理是否与 leave/comp/annual 一致（leaveHours 复制到 workHours，workContent 默认空字符串），若有 entryType 白名单校验则需放开

**Checkpoint**: User Story 2 完整可测 — 单日新类型假期保存和展示正常

---

## Phase 5: User Story 3 - 假期颜色与字典同步 (Priority: P3)

**Goal**: 前端假期颜色由字典 `list_class` 动态驱动，删除硬编码 LEAVE_TYPE_COLOR

**Independent Test**: 检查日报填写页日历角标和颜色圆点颜色是否与字典 list_class 对应的 Element Plus 色值一致（leave=danger=#f56c6c，annual=success=#67c23a 等）

### Implementation for User Story 3

- [x] T014 [US3] 在 `ruoyi-ui/src/views/project/dailyReport/write.vue` 的 `<script setup>` 区块中，删除 `LEAVE_TYPE_COLOR` 常量，新增 `ELEMENT_PLUS_COLORS` 和 `leaveColorMap computed`
- [x] T015 [US3] 在 `ruoyi-ui/src/views/project/dailyReport/write.vue` 的模板中，将两处 `LEAVE_TYPE_COLOR` 引用替换为 `leaveColorMap`

**Checkpoint**: 所有 User Stories 功能完整，颜色完全由字典驱动

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 收尾和验证

- [x] T016 [P] 验证 leaveSummary 聚合：`entry_type != 'work'` 通用查询，新类型自动兼容，无需改动
- [x] T017 [P] 验证统计/团队日报页：无 entryType 硬编码，使用字典 label，新类型自动兼容
- [x] T018 检查 `docs/gen-specs/` 中无 daily_report 相关 spec.yml，无需更新
- [x] T019 [P] 代码走查：`write.vue` 中 `LEAVE_TYPE_COLOR` 已完全替换为 `leaveColorMap`，无残留

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 无依赖，立即开始
- **Phase 2 (Foundational)**: 依赖 Phase 1（T001-T003 完成后）
- **Phase 3 (US1)**: 依赖 Phase 2（T004-T005 完成后）
- **Phase 4 (US2)**: 依赖 Phase 1（字典 SQL 就绪），独立于 Phase 3
- **Phase 5 (US3)**: 依赖 Phase 1（字典就绪以验证颜色），独立于 Phase 3/4
- **Phase 6 (Polish)**: 依赖 Phase 3/4/5 完成

### User Story Dependencies

- **US1 (P1)**: 依赖 Phase 1+2 完成 → 后端 T006-T007 + 前端 T008-T010 可并行
- **US2 (P2)**: 依赖 T001/T002（字典 SQL），T011-T013 几乎零代码，主要是验证
- **US3 (P3)**: 依赖 T001（字典 list_class 值确认），T014-T015 纯前端改动

### Within Each User Story

- T006（服务实现）→ T007（Controller 注册）→ T008/T009/T010 可并行（前端层）
- T014（新增 computed）→ T015（模板替换）

---

## Parallel Opportunities

```bash
# Phase 1 全并行：
T001 (字典 SQL init) || T002 (字典 SQL fix) || T003 (BatchLeaveRequest DTO)

# Phase 2 并行：
T004 (服务接口签名) || T005 (前端 API 函数)

# Phase 3 后端/前端并行：
T006+T007 (后端实现) || T008+T009+T010 (前端弹窗)

# Phase 4 全并行：
T011 (执行 SQL) || T012 (下拉验证) || T013 (save 逻辑验证)

# Phase 5 顺序（T015 依赖 T014）：
T014 → T015

# Phase 6 全并行：
T016 || T017 || T018 || T019
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. 完成 Phase 1: Setup（T001-T003）
2. 完成 Phase 2: Foundational（T004-T005）
3. 完成 Phase 3: User Story 1（T006-T010）
4. **STOP and VALIDATE**: 批量填写端到端测试
5. 可独立部署演示

### Incremental Delivery

1. Phase 1+2 → 基础就绪
2. Phase 3 → 批量填写功能上线（MVP）
3. Phase 4 → 新字典类型可在单日填写中使用（字典数据已在 Phase 1 写入，此阶段主要是验证）
4. Phase 5 → 颜色动态化，可维护性提升
5. Phase 6 → 收尾确认兼容性

### Notes

- US2 (Phase 4) 的核心工作是执行 SQL + 验证，大部分功能由字典机制自动兼容，编码工作极少
- 批量填写（T006）最核心风险：调用 `saveDailyReport` 前**必须**先合并现有 work 条目，否则清除工时记录
- 字典缓存：新字典写入数据库后需清除 Redis 字典缓存（`sys:dict:*`）或重启后端，前端才能看到新选项
