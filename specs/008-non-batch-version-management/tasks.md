---
description: "Task list for 非批次版本管理 implementation"
---

# Tasks: 非批次版本管理（出入库版本·手动输入）

**Input**: spec.md + plan.md + research.md + data-model.md + contracts/
**蓝本**: spec 007 批次版本管理（复用 VersionNumberGenerator/pm_sys_name/字典）

**Tests**: 强制三件套（单元逻辑≥90% + E2E真跑 + JaCoCo）。

**Organization**: 按用户故事，US1 为 MVP。复用度高，多数任务是"裁剪批次代码"。

---

## Phase 1: Setup

- [ ] T001 `pm-sql/init/00_tables_ddl.sql`：`ALTER TABLE pm_version_out ADD manual_task_no varchar(64)` + `ADD manual_task_name varchar(255)`；同步建 `pm-sql/migration_008_non_batch_version_management.sql`（含 ALTER + 菜单）
- [ ] T002 `pm-sql/init/02_menu_data.sql`：在"出入库版本管理"一级菜单下加二级"非批次版本管理"(`/storageVersion/versionOutManual`, component `project/versionOutManual/index`) + 6 权限点 `project:versionOutManual:{list,query,add,edit,remove,export}`
- [ ] T003 应用 SQL 到本地（ALTER + 菜单）

## Phase 2: Foundational

- [ ] T004 `domain/VersionOut.java` 加 `manualTaskNo`/`manualTaskName` 字段 + getter/setter + `@Excel`（批次不填、非批次用）
- [ ] T005 `VersionOutMapper.xml`：resultMap 加 2 列；新增 `selectVersionOutManualList`(硬编码 manual_input='1')、insert/update 含 manual 2 列；或给现有 insert/update 加 manual 列、list 加 manualInput 区分
- [ ] T006 `IVersionOutService` + impl 加 manual 方法（或复用，传 manualInput）：insertManual(setManualInput '1' + 手填任务)、updateManual、selectManualList/ById、deleteManual
- [ ] T007 `controller/VersionOutManualController.java` 骨架：`@RequestMapping("/project/versionOutManual")`，`@PreAuthorize('project:versionOutManual:*')`
- [ ] T008 `ruoyi-ui/src/api/project/versionOutManual.ts` 骨架

## Phase 3: US1 - 新增非批次版本（MVP）

- [ ] T009 [US1] ServiceImpl `insertVersionOutManual`：manual_input='1' + 调 VersionNumberGenerator 生成版本号 + 存 manual_task_no/name + 唯一冲突重试
- [ ] T010 [US1] Controller `POST /` + `GET /generateOutLibVersion` + 级联端点(batchByYear/sysNameByProduct/outVersionOptions/versionPDate，复用批次逻辑)
- [ ] T011 [US1] `versionOutManual/add.vue`：裁剪批次 add.vue —— 去组包/状态/简介/多任务；软件中心任务号、任务名称改 `<el-input>` 手填；提交人员只读当前用户；版本投产日期批次带出；版本号实时生成
- [ ] T012 [US1] `versionOutManual.ts` 补全 add/generate/级联函数
- [ ] T013 [US1] 单元测试 `VersionOutManualServiceTest`（或并入 VersionOutServiceImplTest）：insertManual 设 manual_input、存手填任务、版本号生成、重试

## Phase 4: US2 - 查询/列表/详情

- [ ] T014 [US2] ServiceImpl `selectVersionOutManualList`(manual_input='1') + `selectById`
- [ ] T015 [US2] Controller `GET /list`(startPage) + `GET /{id}`
- [ ] T016 [US2] `versionOutManual/index.vue`：查询(年份/批次号/任务号/版本类型/提交人员/基准版本号/投产日期/产品/版本号) + 列表18列(对齐老 list.html，含手填任务号/任务名) + 分页+搜索缓存
- [ ] T017 [US2] `versionOutManual/detail.vue`：全字段+审计（含手填任务号/任务名）
- [ ] T018 [US2] `versionOutManual.ts` 补 list/get
- [ ] T019 [US2] 单元测试 list/getById 含 manual_input 过滤

## Phase 5: US3 - 编辑/删除

- [ ] T020 [US3] ServiceImpl `updateVersionOutManual`(关键字段变更重算) + `deleteVersionOutManualByIds`(软删)
- [ ] T021 [US3] Controller `PUT /` + `DELETE /{ids}`
- [ ] T022 [US3] `versionOutManual/edit.vue`(复用 add 表单，回显+重算)
- [ ] T023 [US3] `versionOutManual.ts` 补 update/del + index 加编辑/删除按钮 + 路由注册 add/edit/detail
- [ ] T024 [US3] 单元测试 update 重算/不重算、delete

## Phase 6: US4 - 导出

- [ ] T025 [US4] Controller `POST /export`(ExcelUtil) + index 导出按钮 + ts exportVersionOutManual

## Phase 7: 测试三件套 + 收尾

- [ ] T026 E2E `tests/e2e-version-out-manual-crud.spec.js`：覆盖 manual controller 全端点（列表/级联/生成/新增手填任务/详情/编辑重算/删除/导出/manual_input隔离），E2E_BASE_URL=8090 真跑
- [ ] T027 JaCoCo 确认逻辑类覆盖≥90%；全量 `mvn test` 零回归
- [ ] T028 浏览器验证关键页面（新增手填任务→生成版本号→列表只显非批次→详情），截图
- [ ] T029 前端 build:prod 退出码0；migration_008 SQL 产出；回写需求文档差异表；分段提交

---

## Dependencies
Setup → Foundational → US1(MVP) → US2 → US3 → US4 → 测试收尾。
版本号生成器/子系统表/字典/批次端点逻辑全复用，无需重建。

## Notes
- 全程 manual_input='1' 隔离，与批次零串扰。
- 复用 VersionNumberGenerator（已有10特征测试），不重写算法测试。
- 任务信息手填存 manual_task_no/name，不进 pm_version_out_task。
