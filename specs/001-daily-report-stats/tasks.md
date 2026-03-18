# Tasks: 日报统计报表

**功能分支**：`001-daily-report-stats`
**设计文档**：`specs/001-daily-report-stats/`
**依赖**：`002-daily-report-whitelist` 已 merge（白名单表 + 服务已就绪）

## Format: `[ID] [P?] [Story] 描述`

- **[P]**：可并行（不同文件，无依赖）
- **[Story]**：对应哪个用户故事（US1–US5）
- 每个任务包含精确文件路径

---

## Phase 1：基础设施（领域对象 + Mapper）

**目的**：新增 VO 类和 4 条 SQL 查询，是所有用户故事的前提

**⚠️ CRITICAL**：本阶段完成前任何用户故事不可开始实现

- [x] T001 检查并补全 `DailyReport.java` 的查询参数字段（`yearMonth`、`startDate`、`endDate`、`reportDate`、`type` 均需存在；文件路径 `ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReport.java`）
- [x] T002 新建 VO 类 `ruoyi-project/src/main/java/com/ruoyi/project/domain/vo/DailySubmissionStat.java`（字段：`reportDate`/`dayOfWeek`/`isWorkday`/`submittedCount`/`unsubmittedCount`，均有 getter/setter）
- [x] T003 在 `ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportMapper.java` 接口末尾新增 4 个方法：`selectSubmittedCountByDate(DailyReport)`、`selectTotalUserCount(DailyReport)`、`selectSubmittedUsersOnDate(DailyReport)`、`selectUnsubmittedUsersOnDate(DailyReport)`
- [x] T004 在 `ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml` 末尾（`</mapper>` 之前）追加 4 条 SQL（按 plan.md Task 3 中的完整 SQL 编写）：
  - `selectSubmittedCountByDate`：JOIN sys_user/sys_dept，`BETWEEN #{startDate} AND #{endDate}`，可选 deptId 过滤 + `${params.dataScope}`
  - `selectTotalUserCount`：COUNT(*)，可选 deptId 过滤 + `${params.dataScope}`，**且必须加白名单排除**：`AND u.user_id NOT IN (SELECT user_id FROM pm_daily_report_whitelist WHERE del_flag = '0')`
  - `selectSubmittedUsersOnDate`：GROUP_CONCAT 工作内容摘要 LEFT 50 字，可选 deptId 过滤 + `${params.dataScope}`
  - `selectUnsubmittedUsersOnDate`：NOT IN 子查询排除已提交，可选 deptId 过滤 + `${params.dataScope}`，**且必须加白名单排除**：`AND u.user_id NOT IN (SELECT user_id FROM pm_daily_report_whitelist WHERE del_flag = '0')`
- [x] T005 编译验证：`mvn clean compile -pl ruoyi-project -am`
- [x] T006 提交基础层：`git add ruoyi-project/src/main/java/com/ruoyi/project/domain/ ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportMapper.java ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml && git commit -m "feat: 日报统计报表 VO、Mapper 方法和 SQL（含白名单排除）"`

**Checkpoint**：后端 Mapper 层就绪，各用户故事可并行开发

---

## Phase 2：用户故事 1 — 查看当月日报提交统计总览（P1）🎯 MVP

**目标**：管理人员可查看任意月份每天的日报已提交/未提交人数，按周分组展示，非工作日灰色标注

**独立测试**：启动后端，调用 `GET /project/dailyReport/weeklyStats?yearMonth=2026-03`，返回 31 天数组，每天含 submittedCount/unsubmittedCount/isWorkday；前端访问页面默认展示当月数据

### US1 实现

- [x] T007 [US1] 在 `ruoyi-project/src/main/java/com/ruoyi/project/service/IDailyReportService.java` 末尾追加方法签名：`List<DailySubmissionStat> selectWeeklyStats(DailyReport query)`
- [x] T008 [US1] 在 `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java` 实现 `selectWeeklyStats`（按 plan.md Task 4 步骤：解析 yearMonth → 起止日期；调 `selectSubmittedCountByDate` → Map；调 `selectTotalUserCount` → total；通过 `workCalendarMapper`（autowire）查 `pm_work_calendar` 构建 calendarMap；遍历日期范围填充 DailySubmissionStat 列表；`@DataScope(deptAlias="d", userAlias="u")`）
- [x] T009 [US1] 在 `ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportController.java` 追加 `weeklyStats` 端点（`@GetMapping("/weeklyStats")`，`@PreAuthorize("@ss.hasPermi('project:dailyReport:weeklyStats')")`，返回 `success(list)`）
- [x] T010 [P] [US1] 在 `ruoyi-ui/src/api/project/dailyReport.js` 末尾追加 `getWeeklyStats(query)` 函数（GET `/project/dailyReport/weeklyStats`）
- [x] T011 [US1] 新建 `ruoyi-ui/src/views/project/dailyReport/weeklyStats.vue`（查询栏：仅月份 `<el-date-picker type="month">`；表格：按周分组（每周一个 `el-table` 或统一表格 + 分组行），列：日期、星期、已提交人数、未提交人数；非工作日行加 CSS 类 `.non-workday` 灰色背景；默认加载当月数据；**本阶段无部门筛选、无明细弹框、无导出、无周选择器**）
- [x] T012 [US1] 在 `pm-sql/init/02_menu_data.sql` 日报管理菜单段末尾追加日报统计报表菜单 SQL（按 plan.md Task 9：菜单 + 查询权限按钮 + 导出权限按钮；用子查询 `SELECT menu_id FROM sys_menu WHERE menu_name='日报管理' AND parent_id=0` 获取父菜单 ID）；同时创建 `pm-sql/fix_weekly_stats_menu_20260317.sql`（独立完整 SQL，gitignored，供已部署环境执行）
- [x] T013 [US1] 编译验证：`mvn clean compile -pl ruoyi-project -am`；将 fix SQL 在本地 Docker MySQL 执行建菜单：`cat pm-sql/fix_weekly_stats_menu_20260317.sql | docker exec -i 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue`
- [x] T014 [US1] 提交 US1：`git add ruoyi-project/src/main/java/com/ruoyi/project/service/ ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportController.java ruoyi-ui/src/api/project/dailyReport.js ruoyi-ui/src/views/project/dailyReport/weeklyStats.vue pm-sql/init/02_menu_data.sql && git commit -m "feat: 日报统计报表 US1 — 月度统计总览"`

**Checkpoint**：管理员可查看月度统计，US1 独立可验证

---

## Phase 3：用户故事 2 — 按部门筛选统计范围（P2）

**目标**：在统计报表页面增加部门筛选器，选中部门后数据更新为该部门（含下属部门）范围

**独立测试**：选择具体部门后，页面数据与不筛选时数字不同（且总数减小）

### US2 实现

- [x] T015 [US2] 修改 `ruoyi-ui/src/views/project/dailyReport/weeklyStats.vue`：查询栏新增 `<project-dept-select>` 组件绑定 `queryParams.deptId`；`handleQuery` 时将 `deptId` 带入请求参数
- [x] T016 [US2] 提交 US2：`git add ruoyi-ui/src/views/project/dailyReport/weeklyStats.vue && git commit -m "feat: 日报统计报表 US2 — 部门筛选"`

**Checkpoint**：部门筛选生效，US2 独立可验证

---

## Phase 4：用户故事 3 — 查看某天人员明细弹框（P2）

**目标**：点击某天"已提交 N人"或"未提交 N人"（N>0时可点击），弹出人员名单；已提交含工时和工作内容摘要

**独立测试**：点击任意一天的人数链接，弹出框人员数与数字一致

### US3 实现

- [x] T017 [P] [US3] 在 `IDailyReportService.java` 追加 `selectSubmittedDetail(DailyReport)` 和 `selectUnsubmittedDetail(DailyReport)` 方法签名
- [x] T018 [P] [US3] 在 `DailyReportServiceImpl.java` 实现两个 detail 方法（`@DataScope`；分别调 `selectSubmittedUsersOnDate` 和 `selectUnsubmittedUsersOnDate`）
- [x] T019 [US3] 在 `DailyReportController.java` 追加 `weeklyStatsDetail` 端点（`@GetMapping("/weeklyStatsDetail")`，`@PreAuthorize("@ss.hasPermi('project:dailyReport:weeklyStats')")`；根据 `dailyReport.getType()` 分发调 submitted/unsubmitted detail）
- [x] T020 [P] [US3] 在 `ruoyi-ui/src/api/project/dailyReport.js` 追加 `getWeeklyStatsDetail(query)` 函数
- [x] T021 [US3] 修改 `weeklyStats.vue`：已提交/未提交人数在 >0 时改为 `<el-button link>` 可点击；点击调 `getWeeklyStatsDetail`；新增 `<el-dialog>` 展示明细表（已提交列：姓名/部门/工时/工作内容摘要；未提交列：姓名/部门）
- [x] T022 [US3] 编译验证 + 提交：`mvn clean compile -pl ruoyi-project -am && git add ... && git commit -m "feat: 日报统计报表 US3 — 人员明细弹框"`

**Checkpoint**：明细弹框可用，US3 独立可验证

---

## Phase 5：用户故事 4 — 导出统计数据为 Excel（P3）

**目标**：导出双 Sheet Excel：Sheet1 汇总（与页面相同），Sheet2 明细（所有人每天提交状态）

**独立测试**：点击导出，下载 xlsx，打开验证两个 Sheet 数据与页面一致

### US4 实现

- [x] T023 [P] [US4] 在 `IDailyReportService.java` 追加 `exportWeeklyStats(HttpServletResponse, List<DailySubmissionStat>, DailyReport)` 方法签名
- [x] T024 [US4] 在 `DailyReportServiceImpl.java` 实现 `exportWeeklyStats`（使用 Apache POI `XSSFWorkbook`：Sheet1 写 statList 汇总数据；Sheet2 遍历每天调 selectSubmittedDetail/selectUnsubmittedDetail 填写明细行；设置 response Content-Type 和 Content-Disposition；按 plan.md Task 6 完整实现）
- [x] T025 [US4] 在 `DailyReportController.java` 追加 `weeklyStatsExport` 端点（`@GetMapping("/weeklyStatsExport")`，`@PreAuthorize("@ss.hasPermi('project:dailyReport:weeklyStatsExport')")`，`@Log(title="日报统计报表", businessType=BusinessType.EXPORT)`，调 `exportWeeklyStats`）
- [x] T026 [P] [US4] 在 `ruoyi-ui/src/api/project/dailyReport.js` 追加 `exportWeeklyStats(query)` 函数（`responseType: 'blob'`）
- [x] T027 [US4] 修改 `weeklyStats.vue`：查询栏右侧新增"导出 Excel"按钮，点击调 `exportWeeklyStats` 并触发浏览器下载（使用 `URL.createObjectURL` + `<a>` 标签模拟下载）
- [x] T028 [US4] 编译验证 + 提交：`mvn clean compile -pl ruoyi-project -am && git add ... && git commit -m "feat: 日报统计报表 US4 — Excel 双Sheet导出"`

**Checkpoint**：导出功能可用，US4 独立可验证

---

## Phase 6：用户故事 5 — 周选择器快速定位（P3）

**目标**：页面新增周选择下拉，选择"第N周"后只展示该周数据，切换月份后自动重置

**独立测试**：选择月份第2周，表格只显示该周的行；切换月份后周选择器重置为"全部"

### US5 实现

- [x] T029 [US5] 修改 `ruoyi-ui/src/views/project/dailyReport/weeklyStats.vue`：新增 `computeWeekOptions(yearMonth)` 函数（按 plan.md Task 8 逻辑：用 dayjs 遍历月份，生成 `{label, value, startDate, endDate}` 数组，label 格式"第N周（MM-DD～MM-DD）"）；查询栏月份后新增周次下拉（`el-select`，选项含"全部"）；筛选逻辑：前端对后端返回的 statList 按选中周的 `startDate`/`endDate` 过滤展示（无需重新请求后端）；切换月份时重置周选为"全部"并重新请求
- [x] T030 [US5] 提交 US5：`git add ruoyi-ui/src/views/project/dailyReport/weeklyStats.vue && git commit -m "feat: 日报统计报表 US5 — 周次快速筛选"`

**Checkpoint**：周选择器可用，US5 独立可验证

---

## Phase 7：集成验证与收尾

**目的**：端到端验证所有用户故事，构建打包，推送分支

- [x] T031 后端完整打包：`mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true`
- [x] T032 [P] 验证菜单 SQL 已在本地 Docker MySQL 执行（检查"日报管理"下是否出现"日报统计报表"子菜单）：`docker exec -i 3523a41063b7 mysql -u root -ppassword ry-vue -e "SELECT menu_name FROM sys_menu WHERE menu_name='日报统计报表';" 2>/dev/null | grep -v Warning`
- [ ] T033 端到端验证 US1：启动后端，访问日报统计报表页面，切换月份验证数据刷新
- [ ] T034 端到端验证 US3：点击已提交/未提交人数弹出明细，人数与弹框行数一致
- [ ] T035 端到端验证 US4：点击导出，验证 xlsx 双 Sheet 数据正确
- [ ] T036 [P] 推送分支：`git push origin 001-daily-report-stats`

---

## 依赖关系与执行顺序

### Phase 依赖

- **Phase 1**（基础设施）：无依赖，立即可开始
- **Phase 2–6**（各用户故事）：均依赖 Phase 1 完成，之后可并行
- **Phase 7**（集成验证）：依赖 Phase 2–6 全部完成

### 用户故事依赖

- **US1 (P1)**：Phase 1 完成即可开始（最高优先级，核心功能）
- **US2 (P2)**：US1 前端框架就绪后即可开始（纯前端改动）
- **US3 (P2)**：Phase 1 完成即可开始（后端 + 前端独立）
- **US4 (P3)**：Phase 1 完成即可开始（后端 + 前端独立）
- **US5 (P3)**：US1 前端完成后即可开始（纯前端逻辑）

### Phase 1 完成后可并行执行的任务

```
可同时开始（不同文件，无依赖冲突）：
- US1 T007: IDailyReportService.java
- US3 T017: IDailyReportService.java 的 detail 方法（等 T007 完成后追加）
- US4 T023: IDailyReportService.java 的 export 方法（等 T017 完成后追加）
→ 上述 3 个均修改同一文件，需顺序追加

- US1 T010: ruoyi-ui/src/api/project/dailyReport.js（第一个 API 函数）
- US3 T020: 同文件追加（等 T010 完成后）
- US4 T026: 同文件追加（等 T020 完成后）
→ 同文件需顺序操作

- US1 T008: DailyReportServiceImpl.java（实现 selectWeeklyStats）
- US3 T018: DailyReportServiceImpl.java（实现 detail 方法）—— 可与 T008 并行（方法独立）
- US4 T024: DailyReportServiceImpl.java（实现 export）—— 可与 T008/T018 并行
```

---

## 白名单排除说明（与 002 的跨特性对接）

feature 002 的 deferred 任务（US4，T023-T025）要求在本特性实现时补充白名单排除。**本 tasks.md 的 T004 已将此要求内嵌**：
- `selectTotalUserCount` 的 `<where>` 块包含 `AND u.user_id NOT IN (SELECT user_id FROM pm_daily_report_whitelist WHERE del_flag = '0')`
- `selectUnsubmittedUsersOnDate` 的 `<where>` 块包含相同条件

实现 T004 时务必包含以上两处，实现后 002 的 T023-T025 即视为完成。

---

## 实现策略

### MVP（最小可验证版本）

1. 完成 Phase 1（基础设施）
2. 完成 Phase 2（US1：月度统计总览）
3. **停下来验证**：管理员可查看月度统计，数字准确
4. 继续 Phase 3/4（US2/US3）
5. **再次验证**：部门筛选 + 明细弹框

### 完整交付顺序

P1 story US1 → P2 stories US2/US3 → P3 stories US4/US5 → 集成验证 → 推送分支

---

## 备注

- `DailyReport` 实体已有 `yearMonth` 字段（日报活动查询使用），`startDate`/`endDate` 需确认是否已有；若无则在 Task 1 中加为 `@JsonIgnore` transient 字段或直接用 `params` Map 传递
- `WorkCalendarMapper` 已存在，`selectWeeklyStats` 实现中直接 `@Autowired WorkCalendarMapper workCalendarMapper` 即可
- Excel 导出 Sheet2 明细数据量可能较大（~31天 × N用户），建议 Sheet2 一次性构建而非逐行查询
- fix SQL 文件（`pm-sql/fix_weekly_stats_menu_20260317.sql`）gitignored，不提交
