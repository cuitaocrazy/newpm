# Tasks: 日报白名单管理

**功能分支**：`002-daily-report-whitelist`
**设计文档**：`specs/002-daily-report-whitelist/`
**依赖**：本特性实现后，特性 001（日报统计报表）需在其 SQL 中追加白名单排除条件

## Format: `[ID] [P?] [Story] 描述`

- **[P]**：可并行（不同文件，无依赖）
- **[Story]**：对应哪个用户故事（US1–US5）
- 每个任务包含精确文件路径

---

## Phase 1：基础设施（数据库表）

**目的**：新建白名单表，是所有用户故事的前提

- [x] T001 新建 `pm_daily_report_whitelist` 表 DDL，追加到 `pm-sql/init/00_tables_ddl.sql` 末尾（见 plan.md Task 1 建表语句）
- [x] T002 创建增量 SQL 文件 `pm-sql/fix_whitelist_20260317.sql`（含建表 + 菜单，gitignored），在本地 Docker MySQL 执行建表
- [x] T003 提交：`git add pm-sql/init/00_tables_ddl.sql && git commit -m "feat: 新增 pm_daily_report_whitelist 表 DDL"`

---

## Phase 2：后端基础层（实体 / Mapper / Service）

**目的**：CRUD 底层支撑，所有用户故事均依赖

**⚠️ CRITICAL**：本阶段完成前任何用户故事不可开始实现

- [x] T004 [P] 新建实体类 `ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReportWhitelist.java`（继承 BaseEntity，含 id/userId/reason/delFlag/nickName/deptName/keyword 字段，完整 getters/setters）
- [x] T005 [P] 新建 Mapper 接口 `ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportWhitelistMapper.java`（5 个方法：selectWhitelistPage / countByUserId / insertWhitelist / deleteWhitelistById / checkSelfInWhitelist）
- [x] T006 新建 Mapper XML `ruoyi-project/src/main/resources/mapper/project/DailyReportWhitelistMapper.xml`（含 resultMap + 4 个 SQL：selectWhitelistPage JOIN sys_user/sys_dept，countByUserId，checkSelfInWhitelist，insertWhitelist，deleteWhitelistById 软删除）
- [x] T007 新建 Service 接口 `ruoyi-project/src/main/java/com/ruoyi/project/service/IDailyReportWhitelistService.java`（4 个方法：selectWhitelistPage / addToWhitelist / removeFromWhitelist / isInWhitelist）
- [x] T008 新建 Service 实现 `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportWhitelistServiceImpl.java`（注入 Mapper；addToWhitelist 中防重复添加校验 + 设置 createBy/createTime）
- [x] T009 编译验证：`mvn clean compile -pl ruoyi-project -am`
- [x] T010 提交后端基础层：`git add ruoyi-project/src/main/java/com/ruoyi/project/{domain,mapper,service}/ ruoyi-project/src/main/resources/mapper/project/DailyReportWhitelistMapper.xml && git commit -m "feat: DailyReportWhitelist 实体、Mapper、Service"`

**Checkpoint**：后端基础层就绪，可并行开展各用户故事实现

---

## Phase 3：用户故事 1 — 管理员维护白名单人员（P1）🎯 MVP

**目标**：管理员可查看白名单列表、添加人员（含原因）、移除人员

**独立测试**：启动后端，用 admin 账号调用 POST `/project/whitelist`（添加）、GET `/project/whitelist/list`（列表）、DELETE `/project/whitelist/{id}`（移除）三个接口均正常返回

### US1 实现

- [x] T011 [US1] 新建 Controller `ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportWhitelistController.java`（继承 BaseController；list 接口 startPage() + getDataTable；add 用 `@Validated @RequestBody`；delete 软删除；`@PreAuthorize("@ss.hasRole('admin')")` 保护 list/add/delete；checkSelf 无需 admin 权限）
- [x] T012 [P] [US1] 新建前端 API `ruoyi-ui/src/api/project/whitelist.js`（listWhitelist / addWhitelist / removeWhitelist / checkSelfInWhitelist 四个函数）
- [x] T013 [US1] 新建管理页面 `ruoyi-ui/src/views/system/whitelist/index.vue`（搜索栏：关键词输入 + 查询/重置 + 右侧添加按钮；表格：姓名/部门/加入原因/添加时间/操作人/移除操作；添加弹框：`<user-select post-code="pm">` + 原因文本框必填；移除二次确认 ElMessageBox.confirm）
- [x] T014 [US1] 追加菜单 SQL 到 `pm-sql/init/02_menu_data.sql` 和 `pm-sql/fix_whitelist_20260317.sql`（在系统管理目录下新增"日报白名单"菜单 + 查询/添加/移除三个按钮权限）
- [x] T015 [US1] 编译打包验证：`mvn clean compile -pl ruoyi-project -am`
- [x] T016 [US1] 提交 US1：`git add ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportWhitelistController.java ruoyi-ui/src/api/project/whitelist.js ruoyi-ui/src/views/system/whitelist/ pm-sql/init/02_menu_data.sql && git commit -m "feat: 白名单管理 Controller、前端页面和菜单 SQL"`

**Checkpoint**：管理员可完整操作白名单 CRUD，用户故事 1 独立可验证

---

## Phase 4：用户故事 2 — 白名单人员禁止提交日报（P1）

**目标**：白名单中的用户尝试提交日报时被拦截（后端 + 前端双重保护）

**独立测试**：将某账号加入白名单，用该账号登录后打开日报填写页，确认提示卡片出现且填写区禁用；直接调用 POST `/project/dailyReport` 接口返回错误"您已被设置为无需填写日报"

### US2 实现

- [x] T017 [US2] 修改 `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java`：注入 `IDailyReportWhitelistService`；在 `saveDailyReport` 方法最开头调用 `isInWhitelist(userId)`，若 true 则 `throw new ServiceException("您已被设置为无需填写日报，如有疑问请联系管理员")`
- [x] T018 [US2] 修改 `ruoyi-ui/src/views/project/dailyReport/write.vue`：新增 `isWhitelisted` ref；`onMounted` 中调用 `checkSelfInWhitelist()`，若 true 设置 `isWhitelisted = true`；页面顶部添加 `<el-alert>` 提示卡片（`v-if="isWhitelisted"`）；右侧日报填写区域加 `v-if="!isWhitelisted"` 控制显示
- [x] T019 [US2] 提交 US2：`git add ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java ruoyi-ui/src/views/project/dailyReport/write.vue && git commit -m "feat: 日报提交拦截白名单用户（后端 + 前端）"`

**Checkpoint**：白名单用户既无法从前端提交，也无法绕过前端直接调接口提交

---

## Phase 5：用户故事 3 — 日报动态不统计白名单人员（P2）

**目标**：日报动态（activity.vue）的已填写/未填写人员统计自动排除白名单用户

**独立测试**：将某活跃用户加入白名单，查看日报动态页面，该用户不再出现在已填写或未填写名单中，总人数随之减少

### US3 实现

- [x] T020 [US3] 修改 `ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml`：在 `selectActivityUsers` 查询的 `<where>` 块末尾（`${params.dataScope}` 之后）追加排除子查询：`AND u.user_id NOT IN (SELECT user_id FROM pm_daily_report_whitelist WHERE del_flag = '0')`
- [x] T021 [US3] 编译验证：`mvn clean compile -pl ruoyi-project -am`
- [x] T022 [US3] 提交 US3：`git add ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml && git commit -m "feat: selectActivityUsers 排除白名单用户"`

**Checkpoint**：日报动态页面统计范围已自动排除白名单用户

---

## Phase 6：用户故事 4 — 日报统计报表不统计白名单人员（P2）

**目标**：日报统计报表中每日未提交人数计算排除白名单用户

> ⚠️ **注意**：本阶段依赖特性 001（日报统计报表）中新增的 `selectTotalUserCount` 和 `selectUnsubmittedUsersOnDate` SQL。这两个 SQL 当前尚未实现（属于 001 特性范畴）。**本 Phase 需等特性 001 的 Phase 2/3 完成后才能执行**，届时再追加白名单排除条件。

### US4 实现（在 001 特性 SQL 完成后执行）

- [ ] T023 [US4] 修改 `ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml`：在特性 001 新增的 `selectTotalUserCount` 的 `<where>` 块末尾追加相同的白名单排除子查询
- [ ] T024 [US4] 修改 `ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml`：在特性 001 新增的 `selectUnsubmittedUsersOnDate` 的 `<where>` 块末尾追加相同的白名单排除子查询
- [ ] T025 [US4] 提交 US4：`git commit -m "feat: 日报统计报表 SQL 排除白名单用户"`

**Checkpoint**：日报统计报表中白名单用户不会出现在未提交名单，总人数基数也正确

---

## Phase 7：用户故事 5 — 搜索和分页查看白名单列表（P3）

**目标**：管理员可按姓名或部门关键词搜索白名单，列表支持分页

> ℹ️ 本用户故事的后端能力（keyword 过滤、startPage 分页）已在 Phase 2 和 Phase 3 中随基础层一并实现（Mapper XML 中 keyword LIKE 条件、Controller 中 startPage()）。本阶段仅需验证前端搜索交互正常工作。

### US5 验证

- [x] T026 [US5] 验证前端搜索功能：在 `system/whitelist/index.vue` 确认搜索栏的 keyword 绑定到查询参数，重置按钮能清空搜索条件并重新加载列表，分页组件 `<pagination>` 正常显示和跳转

**Checkpoint**：白名单列表搜索和分页完整可用

---

## Phase 8：集成验证与收尾

**目的**：端到端验证所有用户故事协同工作，准备部署

- [x] T027 后端完整打包：`mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true`
- [x] T028 [P] 本地执行菜单 SQL（在本地 Docker MySQL 执行 `pm-sql/fix_whitelist_20260317.sql` 中的菜单插入部分）
- [ ] T029 [P] 端到端验证 US1：用 admin 账号在系统管理菜单下找到"日报白名单"，添加一名测试用户（填写原因），确认列表出现，再移除，确认软删除
- [ ] T030 端到端验证 US2：用被加入白名单的账号登录，打开日报填写页确认提示卡片出现且表单不可用；curl 直接调 POST `/project/dailyReport` 确认被拒绝
- [ ] T031 端到端验证 US3：加入白名单的用户在日报动态统计中不出现，总人数减少
- [ ] T032 [P] 推送分支到远端：`git push origin 002-daily-report-whitelist`（确认所有提交已完成后执行）

---

## 依赖关系与执行顺序

### Phase 依赖

- **Phase 1**（数据库表）：无依赖，立即可开始
- **Phase 2**（后端基础层）：依赖 Phase 1 完成（需要表存在才能测试 Mapper）
- **Phase 3/4/5**（US1/US2/US3）：均依赖 Phase 2 完成，完成后可并行
- **Phase 6**（US4）：依赖特性 001 SQL 完成 — **跨特性依赖，需延迟**
- **Phase 7**（US5）：已随 Phase 3 一并实现，仅需验证
- **Phase 8**（集成验证）：依赖 Phase 3/4/5 全部完成

### 用户故事依赖

- **US1（P1）**：Phase 2 完成即可开始
- **US2（P1）**：Phase 2 完成即可开始，与 US1 可并行
- **US3（P2）**：Phase 2 完成即可开始，可与 US1/US2 并行
- **US4（P2）**：必须等待特性 001 中 `selectTotalUserCount` / `selectUnsubmittedUsersOnDate` SQL 实现后才可执行
- **US5（P3）**：已内嵌于 US1 实现，无独立阻塞点

---

## 并行执行示例：Phase 2 完成后

```
同时开始（不同文件，无依赖冲突）：
- US1 T011: DailyReportWhitelistController.java
- US1 T012: whitelist.js（前端 API）
- US2 T017: DailyReportServiceImpl.java
- US3 T020: DailyReportMapper.xml
```

---

## 实现策略

### MVP（最小可验证版本）

1. 完成 Phase 1 + Phase 2（数据库 + 后端基础层）
2. 完成 Phase 3（US1：管理员 CRUD）
3. **停下来验证**：管理员可完整操作白名单
4. 完成 Phase 4（US2：禁止提交）
5. **停下来验证**：白名单用户被双重拦截

### 完整交付顺序

P1 stories（US1 + US2）→ P2 story US3 → 集成验证 → 推送分支 → US4 延迟到 001 特性中补充

---

## 备注

- US4（Phase 6）跨特性依赖：`selectTotalUserCount` 和 `selectUnsubmittedUsersOnDate` 是特性 001 新增的 SQL，需在实现 001 时同步追加白名单排除条件，不要遗漏
- `deleteWhitelistById` 是软删除：UPDATE del_flag='1'，而非 DELETE（与系统其他表保持一致）
- 防重复添加逻辑在 Service 层（不用 DB 唯一索引，因软删除会导致历史记录冲突）
- `checkSelf` 端点无需 admin 权限，任意登录用户均可调用
- fix SQL 文件（`pm-sql/fix_whitelist_20260317.sql`）已在 .gitignore 中，不提交到 git
