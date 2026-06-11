---
description: "Task list for 批次版本管理 implementation"
---

# Tasks: 批次版本管理（出入库版本）

**Input**: Design documents from `/specs/007-batch-version-management/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/version-out-api.md, reference-version-algorithm.md

**Tests**: 含测试任务 —— 版本号算法特征测试是本功能命门（research.md D8 明确要求），CRUD service 测试遵循 newpm 特征测试约定。

**Organization**: 按用户故事分阶段，US1 为 MVP。US5（数据迁移）⏸️ 延后，本轮不生成实现任务。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件、无未完成依赖）
- **[Story]**: US1–US4

## Path Conventions

- 后端：`ruoyi-project/src/main/java/com/ruoyi/project/`，XML：`ruoyi-project/src/main/resources/mapper/project/`，测试：`ruoyi-project/src/test/java/com/ruoyi/project/service/impl/`
- 前端：`ruoyi-ui/src/views/project/versionOut/`，API：`ruoyi-ui/src/api/project/`
- SQL：`pm-sql/init/`

---

## Phase 1: Setup（共享基础设施）

**Purpose**: 建表、字典、菜单权限

- [x] T001 在 `pm-sql/init/00_tables_ddl.sql` 追加 `pm_version_out`、`pm_version_out_task`、`pm_sys_name` 三张表 DDL（utf8mb4_0900_ai_ci，含索引 + 唯一键 `uk_sys_type_outlib`）+ `ALTER TABLE pm_task ADD demand_name varchar(255) COMMENT '需求名称'`（字段见 data-model.md）
- [x] T002 [P] 在 `pm-sql/init/01_tables_data.sql` 追加字典：`sys_version_type`(1=SP升级包…6=SP包升级包)、`sys_package_mode`(1=A1本批次全量…6=C2多任务增量，值见 research.md D3)、`sys_version_status`(**建成空字典 type，不 seed 数据值** —— 枚举来自生产库，待迁移数据到位由管理员/迁移填充)
- [x] T003 [P] 在 `pm-sql/init/02_menu_data.sql` 追加一级菜单"出入库版本管理" + 二级菜单"批次版本管理"(`/project/versionOut`) + 6 个权限点 `project:versionOut:{list,query,add,edit,remove,export}`（菜单 parent_id 用子查询动态取，勿硬编码）
- [x] T004 应用 SQL 到本地：`cat` 管道 `docker exec -i newpm-mysql-1 mysql ...` 建表+字典+菜单，并 `docker exec -i newpm-redis-1 redis-cli DEL sys_dict:sys_version_type sys_dict:sys_package_mode sys_dict:sys_version_status` 刷缓存

**Checkpoint**: 表、字典、菜单就绪

---

## Phase 2: Foundational（阻塞性前置 —— 所有故事的公共基础）

**⚠️ CRITICAL**: 完成前任何用户故事都不能开工

- [x] T005 [P] 创建 `domain/VersionOut.java`（extends BaseEntity，全字段含 `sub_version_code`+冗余 `product` + `@Excel` 注解 + `List<VersionOutTask> taskList` + 非持久查询字段 + `@NotBlank/@NotNull` 必填校验；**version_status 不加必填**，本轮可选）
- [x] T006 [P] 创建 `domain/VersionOutTask.java`（持久字段 version_id/task_id；transient 回显字段 taskNo/taskName/prjName/demandName，由 JOIN 填充）
- [x] T007 [P] 创建 `domain/SysName.java`（sys_name/base_version_code/p_id 一级产品ID/product 产品名称，extends BaseEntity）
- [x] T008 创建 `mapper/VersionOutMapper.java` + `mapper/project/VersionOutMapper.xml`：resultMap 含 `<collection>` 装配 taskList；基础 CRUD（select list/by id、insert、update、软删除 update del_flag）；所有查询硬编码 `manual_input='0'`；JOIN sys_user/sys_dict_data 加 `COLLATE utf8mb4_unicode_ci`（依赖 T005/T006）
- [x] T009 [P] 创建 `mapper/SysNameMapper.java` + `mapper/project/SysNameMapper.xml`（按 product 查子系统列表、按 sys_name 取 base_version_code）
- [x] T010 创建 `service/IVersionOutService.java` + `service/ISysNameService.java` 接口（命名 select*List/select*ById/insert*/update*/delete*ByIds）
- [x] T011 创建 `service/impl/VersionOutServiceImpl.java` + `service/impl/SysNameServiceImpl.java` 骨架（注入 mapper，空实现占位）
- [x] T012 创建 `controller/VersionOutController.java` 骨架（extends BaseController，类级 `@RequestMapping("/project/versionOut")`，`@PreAuthorize` 占位）
- [x] T013 [P] 创建 `ruoyi-ui/src/api/project/versionOut.ts` 骨架（import request，导出空函数占位）

**Checkpoint**: 实体/Mapper/Service/Controller/API 骨架就绪，可并行开发各故事

---

## Phase 3: User Story 1 - 新增批次版本并自动生成出入库版本号 (P1) 🎯 MVP

**Goal**: 录入一条批次版本，系统按 6 种版本类型规则自动生成唯一出入库版本号并落库，支持级联选择与多任务关联。

**Independent Test**: 新增一条版本 → 确认版本号格式正确、投产日期自动带出、多任务可增删 → 落库成功。

### Tests for User Story 1 ⚠️（先写，先失败）

- [x] T014 [P] [US1] 创建 `service/impl/VersionNumberGeneratorTest.java`：覆盖 6 类型 + 边界（空 maxCode→01、类型3 续号 09→10、类型4 新增/编辑沿用、类型5 首个 02.01、类型5 已有 02.09→02.10 进位、类型6 回退基线）—— 用例见 quickstart.md（JUnit5 + Mockito，mock mapper 查询）

### Implementation for User Story 1

- [x] T015 [US1] 在 `VersionOutMapper.xml` 增加版本号生成所需查询：`getMaxVersionCode(sysName,versionType)`、`getMaxVersionCodeByYear(subVersionCode,versionType)`、`getCodeByOutVersion(sysName,versionType,outVersion)`、`getCodeByBaseVersion(sysName,baseVersionType,outVersion)`（空值 COALESCE 防御）
- [x] T016 [US1] 实现 `service/impl/VersionNumberGenerator.java`：端口 Scala `getOutLibVersion` 全部 6 类型逻辑（补零/进位/5→3、6→1 回退/编辑重算），使 T014 全绿（依赖 T015）
- [x] T017 [US1] 在 `VersionOutServiceImpl` 实现 `insertVersionOut`：调 generator 生成版本号 → 主表插入 → `pm_version_out_task` 级联插入（task_no 在 pm_task 找不到时 task_id 留空、仅存文本）→ 命中 `uk_sys_type_outlib` 冲突重试(≤3次)，`@Transactional`
- [x] T018 [US1] 在 `VersionOutController` 实现 `POST /project/versionOut`（`@Log(新增)` + `@PreAuthorize('project:versionOut:add')` + `@Validated`）与 `GET /generateOutLibVersion`（实时生成回填）
- [x] T019 [US1] 实现级联端点：`GET /sysNameByProduct`、`GET /outVersionOptions`（依赖 SysNameService / VersionOutMapper）
- [x] T020 [P] [US1] 实现级联端点：`GET /batchByYear`（查 pm_production_batch，按 production_year）、`GET /versionPDate`（批次→plan_production_date）、`GET /taskInfo`（按 software_demand_no 查 pm_task → 回显 task_name/demand_name，JOIN pm_project 取 project_name；JOIN 加 COLLATE）
- [x] T021 [US1] 创建 `ruoyi-ui/src/views/project/versionOut/add.vue`：级联下拉（年份→批次、产品→子系统→带出基准版本号/product、子系统+类型→初级版本号）、版本号只读实时回填、多任务行动态增删、必填校验（version_status 可选、空字典时下拉为空可不选）、提交人员默认当前用户（`<user-select post-code="pm">`，字典用 `<dict-select>`）
- [x] T022 [US1] 在 `versionOut.ts` 补全 `addVersionOut`/`generateOutLibVersion`/`batchByYear`/`sysNameByProduct`/`outVersionOptions`/`versionPDate`/`taskInfo` 函数

**Checkpoint**: 可独立新增批次版本并生成正确版本号 —— MVP 达成

---

## Phase 4: User Story 2 - 查询、筛选与查看列表 (P2)

**Goal**: 多条件分页查询批次版本，查看详情，返回列表保留筛选状态。

**Independent Test**: 在已有数据下按各条件查询、进详情、返回保留条件。

### Implementation for User Story 2

- [ ] T023 [US2] 在 `VersionOutServiceImpl` 实现 `selectVersionOutList`（多条件 + manual_input='0'）与 `selectVersionOutById`（含 taskList）
- [ ] T024 [US2] 在 `VersionOutController` 实现 `GET /list`（首行 `startPage()`，`@PreAuthorize('project:versionOut:list')`）与 `GET /{id}`
- [ ] T025 [US2] 创建 `ruoyi-ui/src/views/project/versionOut/index.vue`：查询表单（年份/批次/产品/任务号/版本类型/提交人员/基准版本号/组包方式/版本状态）+ el-table（服务端排序 sortable=custom）+ 分页 + 详情跳转 + 搜索状态缓存（sessionStorage，参考 subproject/index.vue）
- [ ] T026 [US2] 创建 `ruoyi-ui/src/views/project/versionOut/detail.vue`（展示全字段 + 关联任务列表）
- [ ] T027 [US2] 在 `versionOut.ts` 补全 `listVersionOut`/`getVersionOut`
- [ ] T028 [P] [US2] 创建 `service/impl/VersionOutServiceImplTest.java`：覆盖 list/getById 含 taskList 装配（Mockito）

**Checkpoint**: 查询/详情独立可用

---

## Phase 5: User Story 3 - 编辑与删除 (P3)

**Goal**: 修订（关键字段变更重算版本号）/ 软删除批次版本，留审计。

**Independent Test**: 改备注号不变 / 改子系统号重算；删除留审计日志。

### Implementation for User Story 3

- [ ] T029 [US3] 在 `VersionOutServiceImpl` 实现 `updateVersionOut`：判断 sys_name/version_type/sub_version_code 是否变更决定是否重算版本号 → 主表更新 → 任务行整体替换，`@Transactional`
- [ ] T030 [US3] 在 `VersionOutServiceImpl` 实现 `deleteVersionOutByIds`：软删除 `del_flag='1'` + 级联处理任务行，`@Transactional`
- [ ] T031 [US3] 在 `VersionOutController` 实现 `PUT /project/versionOut`（`@Log(修改)`）与 `DELETE /{ids}`（`@Log(删除)`）
- [ ] T032 [US3] 创建 `ruoyi-ui/src/views/project/versionOut/edit.vue`（复用 add 表单，回显 + 版本号重算逻辑）
- [ ] T033 [US3] 在 `versionOut.ts` 补全 `updateVersionOut`/`delVersionOut`
- [ ] T034 [P] [US3] 在 `VersionOutServiceImplTest` 增加 update（重算/不重算）/ delete 用例

**Checkpoint**: 完整 CRUD 可用

---

## Phase 6: User Story 4 - 导出 Excel (P3)

**Goal**: 当前查询结果导出为 Excel。

**Independent Test**: 有结果时导出，文件含全部列。

### Implementation for User Story 4

- [ ] T035 [US4] 在 `VersionOutServiceImpl` 增加 `enrichForExport`（填充字典/任务等显示字段），在 `VersionOutController` 实现 `POST /export`（`@Log(导出)` + POI `util.exportExcel`，依赖 T005 的 `@Excel` 注解）
- [ ] T036 [US4] 在 `index.vue` 增加导出按钮 + `versionOut.ts` 增加 `exportVersionOut`

**Checkpoint**: 导出可用

---

## Phase 7: Polish & Cross-Cutting

- [ ] T037 [P] 创建 E2E `e2e-version-out-crud.spec.js`：新增→生成版本号→查询→编辑→删除→导出主流程（跑前临时关验证码）
- [ ] T038 [P] 创建 `docs/gen-specs/pm_version_out.yml`（代码生成规格，便于后续维护）
- [ ] T039 [P] 更新 `docs/plans/2026-06-10-yadapm-migration-requirements.md`：标记"批次版本管理"已实现，记录与老系统的差异点（并发唯一键、软删除、子系统配置表）
- [ ] T040 运行 `mvn test -pl ruoyi-project -am -Dtest=VersionNumberGeneratorTest,VersionOutServiceImplTest` 全绿 + 按 quickstart.md 跑验收对照

---

## ⏸️ Phase X: User Story 5 - 数据迁移（延后，本轮不实现）

> 阻塞：等待旧库最新数据。待数据到位后单独补：Oracle→MySQL ETL，仅迁批次记录（manual_input=0），保留原 out_lib_version，引用关系映射到新系统年份/批次/产品/任务/用户。

---

## Dependencies & Execution Order

### Phase 依赖

- **Setup (P1)**: 立即开始
- **Foundational (P2)**: 依赖 Setup —— 阻塞所有故事
- **US1 (P3)**: 依赖 Foundational —— MVP
- **US2/US3/US4**: 依赖 Foundational；US3 编辑复用 US1 表单组件，US4 导出依赖 US2 列表查询；逻辑上 US2→US3→US4 顺序最顺，但各自可独立测试
- **Polish (P7)**: 依赖目标故事完成

### 用户故事内部

- 测试先于实现（T014 先于 T016）
- Model(P2) → Mapper(T015) → Generator/Service(T016/T017) → Controller(T018) → 前端(T021)

### 并行机会

- T002/T003（SQL 不同文件）、T005/T006/T007（不同 domain）、T009/T013、T020、T028/T034、T037/T038/T039 均可并行
- US2/US3/US4 后端方法虽在同文件（ServiceImpl/Controller/ts）→ 不可并行，需串行避免冲突

---

## Implementation Strategy

### MVP First

1. Phase 1 Setup → 2. Phase 2 Foundational → 3. Phase 3 US1 → **STOP & VALIDATE**（新增 + 版本号生成 6 类型全对）→ demo

### Incremental Delivery

Setup+Foundational → US1(MVP) → US2(查询) → US3(改删) → US4(导出) → Polish。每步独立可测、不破坏前序。

---

## Notes

- [P] = 不同文件无依赖；同文件任务（ServiceImpl/Controller/versionOut.ts/xml 多次追加）必须串行
- 版本号算法是最高风险，T014 测试务必先写先失败，T016 实现到全绿才算过
- 全程 `manual_input='0'` 隔离批次数据；JOIN 系统表加 COLLATE；提交人员/部门用代理 API
- 每个 task 或逻辑组完成后 commit
- 用户偏好：直接实现，不使用 TODO(human) 让用户写代码
