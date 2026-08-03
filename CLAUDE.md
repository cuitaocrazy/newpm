# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RuoYi-Vue v3.9.1 — Enterprise admin system customized for **Project Management (PM)** business.

- **Backend:** Java 17 / Spring Boot 3.5.8 / Spring Security / MyBatis / Redis / JWT
- **Frontend:** Vue 3.5 / TypeScript 5.6 / Vite 6.4 / Element Plus 2.13 / Pinia
- **Business Domain:** Project lifecycle management, customer/contract management, approval workflows, daily reports, revenue recognition

## Build & Run Commands

### Backend (from project root)

```bash
mvn clean package -Dmaven.test.skip=true
java -Xms512m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
# Or: ./ry.sh start|stop|restart|status

# Module-specific
mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true
mvn clean package -pl ruoyi-gen-cli -am -Dmaven.test.skip=true
mvn clean compile -pl ruoyi-project -am
```

### Frontend (from ruoyi-ui/ or project root)

```bash
npm run dev              # Vite dev server on port 80, proxies /dev-api → localhost:8080
npm run build:prod       # Production build → dist/
```

### Code Generation CLI

```bash
java -jar ruoyi-gen-cli/target/ruoyi-gen-cli-3.9.1.jar --sql=<ddl>.sql --config=<config>.yml --output=<output>.zip
```

Use the `/ruoyi-gen` skill for interactive CRUD generation.

### Unit Testing (backend)

Service-layer **characterization tests** (lock in current behavior to catch regressions during refactors) live in `ruoyi-project/src/test/java/com/ruoyi/project/service/impl/` — ~110 methods across `Contract`/`Customer`/`Project`/`ProjectApproval`/`ProjectStats`/`DailyReport` service tests. They use **JUnit 5 + Mockito** (`@ExtendWith(MockitoExtension.class)`, `@InjectMocks`/`@Mock`) — pure unit tests, so **no MySQL/Redis needed**.

```bash
mvn test -pl ruoyi-project -am                              # all project-module unit tests
mvn test -pl ruoyi-project -am -Dtest=ContractServiceImplTest   # single class
mvn test -pl ruoyi-project -am -Dtest=ContractServiceImplTest#methodName  # single method
```

Production builds skip tests (`-Dmaven.test.skip=true`); run `mvn test` explicitly to execute them. When changing service behavior, update the corresponding characterization test in the same commit.

### E2E Testing (from project root)

Playwright config (`playwright.config.js`): chromium only, `baseURL: http://localhost:80`, `retries: 2`, `workers: 1`, zh-CN locale. **Requires both frontend (port 80) and backend running** — tests hit the live app and log in as `admin` / `123456789` via `/dev-api/login`.

```bash
npx playwright install && npx playwright test
npx playwright test e2e-contract-crud.spec.js   # specific file
npx playwright test --ui && npx playwright show-report
```

API-driven specs (`e2e-*.spec.js`) share auth via `tests/helpers/api-client.js` (`setupApi()` returns an authed `APIRequestContext`; `await api.dispose()` in `afterAll`). Use this helper rather than re-implementing login per file. Per-module E2E suites: `e2e-{contract,customer,payment,task,daily-report,team-revenue,approval-workflow,manager-stage-change,auxiliary-modules}-*.spec.js`. Feature-specific: `contract-filter.spec.js`, `contract-add-from-project.spec.ts`, `project-create-audit-fields.spec.js`, `query-smoke.spec.js`. Debug/regression: `005-cleanup-verification.spec.js`, `006-code-review-fixes.spec.js`, `network-request-debug.spec.js`.

### Prerequisites

Java 17, Maven 3.6+, MySQL 8.x (`ry-vue`, `utf8mb4_unicode_ci`, port 3306), Redis 6.x+ (port 6379), Node.js 18+.

**First-time DB setup:**
```bash
mysql -u root -p ry-vue < pm-sql/init/00_tables_ddl.sql
mysql -u root -p ry-vue < pm-sql/init/01_tables_data.sql
mysql -u root -p ry-vue < pm-sql/init/02_menu_data.sql
```

## Module Architecture

```
ruoyi-admin       → Spring Boot entry, REST controllers (system/monitor/tool/common)
ruoyi-framework   → Security (JWT + Spring Security), AOP aspects, global exception handling
ruoyi-system      → Business services: user/role/menu/dept/dict/config/notice + MyBatis mappers
ruoyi-common      → Base classes, custom annotations, utilities (Excel, XSS filter, file ops)
ruoyi-project     → Project management business module (com.ruoyi.project)
ruoyi-gen-cli     → Standalone CLI code generator (no MySQL/Redis required)
ruoyi-ui          → Vue 3 + TypeScript + Vite frontend
```

Dependencies: admin → framework → system → common. Project/quartz/generator/gen-cli all depend on common.

### ruoyi-project Module — Business Entities

| Entity | Table | Description |
|--------|-------|-------------|
| `Project` | `pm_project` | 项目管理 - lifecycle, budget, workload, approval tracking |
| `ProjectApproval` | `pm_project_approval` | 项目审核 - approval workflow |
| `Customer` | `pm_customer` | 客户管理 |
| `CustomerContact` | `pm_customer_contact` | 客户联系人 |
| `Contract` | `pm_contract` | 合同管理 - amount tracking, project associations |
| `ProjectContractRel` | `pm_project_contract_rel` | 项目合同关联 (many-to-many) |
| `Payment` | `pm_payment` | 款项管理 - installment tracking |
| `Attachment` | `pm_attachment` | 附件管理 |
| `AttachmentLog` | `pm_attachment_log` | 附件操作审计日志 |
| `SecondaryRegion` | `pm_secondary_region` | 二级区域 |
| `ProjectReview` | (view-based) | 公司收入确认 |
| `ProjectManagerChange` | `pm_project_manager_change` | 项目经理变更记录 |
| `TeamRevenueConfirmation` | `pm_team_revenue_confirmation` | 团队收入确认 |
| `ProjectMember` | `pm_project_member` | 项目成员 |
| `DailyReport` | `pm_daily_report` | 工作日报 |
| `DailyReportDetail` | `pm_daily_report_detail` | 工作日报明细 |
| `WorkCalendar` | `pm_work_calendar` | 工作日历 |
| `ProjectStageChange` | `pm_project_stage_change` | 项目阶段变更记录 |
| `WorkloadCorrectLog` | `pm_workload_correct_log` | 人天补正审计日志 |
| `Task` | `pm_task` | 任务管理（独立表，迁移自 `pm_project.project_level=1`；**迁移已完成**，TaskController + 前端均已切换） |
| `ProductionBatch` | `pm_production_batch` | 投产批次（`Task.batchId` FK）。`ProductionBatchController` → `/project/productionBatch/**`. |

### Business Workflows

Full details in `docs/pm/PM需求.md`. Key notes:

1. **Project Initiation:** Code format `{industry}-{region}-{shortName}-{year}`. Status starts at 待审核.

2. **Project Approval:** Status: 0=待审核, 1=审核通过, 2=审核拒绝, 3=退回待审核. Extra endpoints:
   - `POST /project/approval/approve` — `{ projectId, approvalStatus, approvalReason }` (reason required when rejecting)
   - `POST /project/approval/rollback` — `{ projectId, rollbackReason }`
   - `GET /project/approval/history/{projectId}`, `GET /project/approval/projectList`, `GET /project/approval/projectSummary`

3. **Contract Management:** Many-to-many via `pm_project_contract_rel`. Dicts: `sys_htlx`, `sys_htzt` (未签署/已签署), `sys_fkzt` for payment status, `sys_jdgl` for payment quarters. `Payment` supports multi-value filter fields (not persisted): `deptIds`, `paymentStatuses`, `expectedQuarters`, `actualQuarters` — passed as arrays in request body.

4. **Daily Reports:** Master-detail: `pm_daily_report` → `pm_daily_report_detail`. Three views: `write.vue`, `activity.vue`, `stats.vue`.

   **`write.vue` layout:**
   - Left (7/24): `MonthCalendar` — day badges from `total_work_hours` (green=≥8h, orange=<8h, grey=none). API: `GET /project/dailyReport/list?yearMonth=`
   - Right (17/24): project list from `pm_project_member`. API: `GET /project/dailyReport/myProjects` — returns `hasSubProject` flag per project. For projects with sub-tasks (`hasSubProject=true`), expand multi-task rows (one per sub-project); each row: hour slider + content textarea + `workCategory` (dict `sys_gzlb`). Plain projects get a single row (no `workCategory`).
   - Click date → load detail via `GET /project/dailyReport/my/{reportDate}`. Save → `POST /project/dailyReport`.
   - **Week constraint**: only current calendar week (Mon–Sun) is editable. Past/future weeks are read-only.
   - **`pm_daily_report_detail`** extra fields: `sub_project_id` (now FK to `pm_task.task_id`; kept as `sub_project_id` column name for backwards compat, NULL for plain projects), `work_category` (dict `sys_gzlb`, NULL for plain projects).
   - **Entry types**: `entryType` field on detail — `work`=项目工时, `leave`=请假, `comp`=倒休, `annual`=年假. Non-work entries use `leaveHours` instead of `workHours`. `pm_daily_report.leaveSummary` is a virtual field (subquery aggregating non-work entries as `entryType:hours` pairs).
   - **Workload rollup**: saving recalculates sub-project `actual_workload`, then rolls up to parent project `actual_workload`.

5. **Revenue Recognition:** Multi-dimensional filtering. Confirmation status: 未确认/待确认/已确认/无法确认. Batch operations supported. Revenue fields on `Project`: `confirmAmount`, `taxRate`, `afterTaxAmount` (= confirmAmount / (1 + taxRate/100)), `revenueConfirmStatus`, `revenueConfirmYear`.

6. **Team Revenue Confirmation:** Queries `pm_project` as main table (LEFT JOIN `pm_team_revenue_confirmation`). `confirmDeptId = -1` = projects with NO team confirmation.

7. **Project Manager Change:** DTOs: `ChangeRequest` (single), `BatchChangeRequest` (batch), `ProjectManagerChangeVO` (view).

8. **Project Stage Change:** Table uses `utf8mb4_0900_ai_ci` — add `COLLATE utf8mb4_unicode_ci` when joining system tables.

9. **Add Contract from Project:** List row shows "添加合同" / "查看合同" based on whether contract exists. Navigates to contract creation with pre-populated projectId, dept, customer.

10. **Task Decomposition:** Tasks live in `pm_task` (FK `project_id` → parent project). **Migration is complete**: `pm_task` is populated, `TaskController` serves all task CRUD, and `subproject/` frontend fully calls TaskController endpoints. Cleanup of legacy task-specific columns from `pm_project` is in progress (`docs/plans/2026-03-13-cleanup-project-task-fields.md`).

    Task endpoints (`/project/task/**`):
    - `GET /project/task/list` — paginated task list; supports `parentId` / `projectId` / `taskName` / `taskCode` / `taskStage` / `taskManagerId` / `productionYear` / `batchId` / `scheduleStatus` / `softwareDemandNo` / `product` / `projectDept` / `parentRevenueConfirmYear`
    - `GET /project/task/options?projectId=xxx` — lightweight options for daily report dropdowns
    - `GET /project/task/projectsHasTasks` — batch check which projects have tasks
    - Standard CRUD: `GET /{taskId}`, `POST`, `PUT`, `DELETE /{taskIds}`

    `TaskMapper.selectTaskList` queries `pm_project LEFT JOIN pm_task` — meaning tasks are always shown in the context of their parent project (project metadata is enriched onto Task). `pm_daily_report_detail.project_id` stores the **task ID** (from `pm_task`) for task-based entries.

    Frontend: `ruoyi-ui/src/views/project/subproject/` (index / add / edit / detail). Route is hidden level-2 under 项目管理 (`/project/subproject`).

    Task fields: `taskCode`, `taskName`, `taskStage` (dict `sys_xmjd`), `taskManagerId`, `product` (dict `sys_product`), `bankDemandNo`, `softwareDemandNo`, `taskBudget`, `estimatedWorkload`, `actualWorkload`, `productionYear` (dict `sys_ndgl`), `batchId` → `pm_production_batch`, `scheduleStatus` (dict `sys_pqzt`), `startDate`/`endDate`, `productionDate`, `productionVersionDate`, `actualProductionDate`, `internalClosureDate`, `functionalTestDate`, `functionDescription`, `implementationPlan`, `taskPlan`, `taskDescription`.

    Sub-project members are **not** inserted into `pm_project_member` — they inherit the parent project's member list. Legacy `ProjectController` sub-project proxies (`/project/project/subList`, `/project/project/subProjectOptions`) are deprecated; use TaskController instead.

### Dictionary Dependencies

`industry` 行业, `sys_yjqy` 区域, `sys_xmfl` 项目分类, `sys_xmjd` 项目阶段(0-12，11=项目结项，12=技术投产), `sys_yszt` 验收状态, `sys_xmzt` 项目状态, `sys_htlx` 合同类型, `sys_htzt` 合同状态, `sys_fkzt` 付款状态, `sys_wdlx` 文档类型, `sys_spzt` 审核状态(0-3), `sys_qrzt` 确认状态(1-4), `sys_srqrzt` 收入确认状态(0-3), `sys_ndgl` 年度管理(for `establishedYear`/`revenueConfirmYear`/`productionYear`), `sys_gzlb` 工作任务类别(for `work_category` in `pm_daily_report_detail`), `sys_pqzt` 排期状态(for `scheduleStatus` on tasks), `sys_jdgl` 季度管理(for `expectedQuarter`/`actualQuarter` in `pm_payment`), `sys_product` 产品(for `product` on tasks)

### API URL Convention

| Controller | URL Prefix | Purpose |
|---|---|---|
| `ProjectController` | `/project/project/**` | Project CRUD + proxy endpoints. Extra: `GET /summary` (aggregation), `GET /checkCode`, `GET /listByDept`, `GET /listByName`, `GET /{id}/participantsWorkload`, `POST /{id}/bindContract`, `DELETE /{id}/unbindContract` (perm: `project:contract:unbind`). Print 立项申请书 is frontend-only (`project:project:print`). |
| `ProjectApprovalController` | `/project/approval/**` | Approval workflow |
| `ContractController` | `/project/contract/**` | Contract management |
| `PaymentController` | `/project/payment/**` | Payment management. Extra: `GET /listWithContracts` (contracts+payments grouped), `GET /sumPaymentAmount` (total), `GET /checkAttachments/{id}` |
| `CustomerController` | `/project/customer/**` | Customer + contacts |
| `AttachmentController` | `/project/attachment/**` | File attachments |
| `ProjectMemberController` | `/project/member/**` | Project members |
| `ProjectManagerChangeController` | `/project/managerChange/**` | Manager change records |
| `ProjectStageChangeController` | `/project/projectStageChange/**` | Stage change records |
| `SecondaryRegionController` | `/project/secondaryRegion/**` | Secondary regions |
| `WorkCalendarController` | `/project/workCalendar/**` | Work calendar |
| `DailyReportController` | `/project/dailyReport/**` | Daily reports. Extra: `GET /monthly` (month summary), `GET /activityUsers` (已填写/未填写人员统计) |
| `ProjectStatsController` | `/project/dailyReport/**` | Stats (shares prefix; `/projectStats`, `/projectNameSuggestions`, `POST /projectStats/{id}/correct`, `GET /projectStats/{id}/correctLog`) |
| `ProjectReviewController` | `/project/review/**` | Company revenue view |
| `TaskController` | `/project/task/**` | Task CRUD (reads `pm_project LEFT JOIN pm_task`). Extra: `GET /options?projectId=` (lightweight for daily report dropdown), `GET /projectsHasTasks` (batch check which projects have tasks), `GET /summary` (aggregation row for list page), `GET /searchTaskCode|searchTaskName|searchSoftwareDemandNo` (autocomplete suggestions) |
| `ProductionBatchController` | `/project/productionBatch/**` | 投产批次 CRUD |
| `TeamRevenueConfirmationController` | `/revenue/team/**` | Team revenue (different root) |

Company revenue endpoints: `/project/project/revenue/**`. Frontend routes: `/project/{entity}` + `/revenue/company` + `/revenue/team`.

## Backend Patterns

### Controller Convention

All controllers extend `BaseController`. Always call `startPage()` before list queries:

```java
@PreAuthorize("@ss.hasPermi('module:entity:list')")
@GetMapping("/list")
public TableDataInfo list(Entity entity) {
    startPage();  // MUST be first
    return getDataTable(service.selectEntityList(entity));
}

@Log(title = "实体名", businessType = BusinessType.INSERT)
@PreAuthorize("@ss.hasPermi('module:entity:add')")
@PostMapping
public AjaxResult add(@Validated @RequestBody Entity entity) {
    return toAjax(service.insertEntity(entity));
}
```

- `AjaxResult` → `{ code, msg, data }` for single objects
- `TableDataInfo` → `{ code, msg, total, rows }` for paginated lists

### Custom Annotations

| Annotation | Purpose |
|---|---|
| `@Log(title, businessType)` | Operation audit log (async) |
| `@DataScope(deptAlias, userAlias)` | Data permission SQL injection into `${params.dataScope}` (1=all, 2=custom, 3=own dept, 4=dept+children, 5=self) |
| `@DataSource` | Dynamic datasource switching |
| `@RateLimiter(time, count)` | Redis Lua rate limiting |
| `@RepeatSubmit` | Duplicate submission prevention |
| `@Excel(name)` | Excel import/export column config |
| `@Anonymous` | Bypass JWT authentication |

### Entity Hierarchy

- `BaseEntity` → createBy/createTime/updateBy/updateTime/remark/params(Map)
- `TreeEntity extends BaseEntity` → parentId/ancestors

### Naming Conventions

- Packages: `com.ruoyi.{module}.controller|service|domain|mapper`
- Service methods: `select*List()`, `select*ById()`, `insert*()`, `update*()`, `delete*ByIds()`
- Permission strings: `{module}:{business}:{action}`

### Master-Detail Pattern

One-to-many via detail list on master entity. Service cascades insert/delete. MyBatis uses `<collection>` in resultMap. Operations must be `@Transactional`.

### Other Backend Patterns

- **Exception**: `throw new ServiceException("message")` → caught by `GlobalExceptionHandler`, returns `AjaxResult.error`
- **Logging**: `./logs/` (60-day rotation). `@Log` → async DB audit. SLF4J → file logs.
- **Async**: `@Async` on service methods (e.g., `ProjectEmailServiceImpl.sendNotificationEmail`)
- **Excel export enrichment**: `enrichForExport(list)` on service to populate non-DB display fields before `exportExcel()`
- **Spring Boot 3**: Jakarta EE namespace, `SecurityFilterChain` bean, springdoc-openapi at `/swagger-ui.html`
- **Global String trim**: All inbound String values are trimmed of leading/trailing whitespace at two entry points — **GET query params** via `StringTrimmerEditor(false)` in `BaseController.@InitBinder`, and **`@RequestBody` JSON** via `TrimStringJsonDeserializer` (a `ContextualDeserializer`) registered globally in `ApplicationConfig` (`deserializerByType(String.class, ...)`). Password-class fields (`password/oldPassword/newPassword/confirmPassword`) are skipped via the deserializer's field-name blacklist. **Do not add per-controller/per-field trim** — it's already global. To exempt a new sensitive field, add it to `TrimStringJsonDeserializer.SKIP_FIELDS`. (Design doc: `docs/plans/2026-05-25-global-string-trim.md`)

## Frontend Patterns

### API Layer (`src/api/`)

- `src/api/project/` — Project business APIs
- `src/api/revenue/company.ts` — use this (typed); `company.js` is older untyped version
- `src/api/project/managerChange.js` — project list with latest change info
- `src/api/project/projectManagerChange.js` — manager change CRUD
- `src/api/project/contact.js` — customer contact helper (CustomerController handles contacts)

```typescript
import request from '@/utils/request'
request({ url: '/project/project/list', method: 'get', params: query })
// Response: res.rows / res.total for lists, res.data for single objects
// DO NOT use proxy.$http or proxy.request — they don't exist
```

HTTP client auto-injects `Authorization: Bearer {token}`. Response codes: 200=success, 401=re-login, 500=error.

### Custom Business Components (`src/components/`)

| Component | Usage |
|---|---|
| `DictSelect` | `<dict-select dict-type="sys_xmfl" v-model="..." />` |
| `UserSelect` | `<user-select post-code="pm" v-model="..." />` |
| `SecondaryRegionSelect` | `<secondary-region-select :region-dict-value="primaryRegion" v-model="..." />` |
| `ProjectSelect` | Project picker with search |
| `ProjectDeptSelect` | Department tree picker |
| `MonthCalendar` | Monthly calendar (used in daily report) |

Global components registered in `main.ts`: DictTag, Pagination, FileUpload, ImageUpload, ImagePreview, RightToolbar, Editor.

### Permission

- Route-level: dynamic routes from backend based on roles/permissions
- Element-level: `v-hasPermi` directive
- Route meta: `permissions: ['a:b:c']`, `roles: ['admin']`

### File Upload Pattern

`AttachmentController` → `pm_attachment` + `pm_attachment_log`. Business types: `project`, `contract`, `payment`. API: `src/api/project/attachment.js`.

Storage path: `{业务类型}/{ID}_{名称}/{yyyyMMdd}/{UUID}.{ext}` inside `/app/uploadPath/` (K3s PVC).
Allowed: `doc, docx, xls, xlsx, pdf, csv, png, jpg, gif, txt, 7z, zip, gz`. Max: **30 MB**.

### Server-Side Sort Pattern

```typescript
// <el-table @sort-change="handleSortChange">
// <el-table-column sortable="custom" prop="projectBudget">
function handleSortChange({ prop, order }) {
  queryParams.value.orderByColumn = prop
  queryParams.value.isAsc = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : null
  handleQuery()
}
```

### Search State Caching Pattern

Used to preserve query conditions when navigating to detail/edit pages and returning. Uses `sessionStorage` (tab-scoped, not persistent across refreshes) with `onBeforeRouteLeave` to save and `onMounted` to restore. Use raw `sessionStorage` directly — `src/plugins/cache.ts` exports `cache.session.setJSON`/`getJSON`/`remove` as a cleaner wrapper.

```typescript
import { onBeforeRouteLeave } from 'vue-router'

const SEARCH_STATE_KEY = 'xxx_search_state'  // unique per list page

function saveSearchState() {
  sessionStorage.setItem(SEARCH_STATE_KEY, JSON.stringify({
    queryParams: { ...queryParams },
    someKeyword: someKeyword.value,         // any extra display-only refs
    asyncDropdownOptions: options.value     // cache async-loaded dropdown data too
  }))
}

function restoreSearchState(): boolean {
  try {
    const raw = sessionStorage.getItem(SEARCH_STATE_KEY)
    if (!raw) return false
    const state = JSON.parse(raw)
    Object.assign(queryParams, state.queryParams)
    someKeyword.value = state.someKeyword || ''
    options.value = state.asyncDropdownOptions || []
    sessionStorage.removeItem(SEARCH_STATE_KEY)  // one-time consume
    return true
  } catch { return false }
}

onBeforeRouteLeave(() => saveSearchState())

onMounted(() => {
  restoreSearchState()   // restore before first getList()
  // ... other init logic
  getList()
})

function resetQuery() {
  sessionStorage.removeItem(SEARCH_STATE_KEY)  // clear cache on manual reset
  // ... resetFields, getList
}
```

**Key rules:** Cache async-loaded dropdown data (e.g., batch options) alongside query params, or the restored dropdown will be empty. Clear cache in `resetQuery`. Implemented in `subproject/index.vue` as reference.


### Mobile H5 Pattern (`/m` 移动端子树)

日报移动端 H5（specs/014-daily-report-mobile）。驻场人员手机浏览器访问 `/m/login` 登录填日报。

- **路由**: `src/router/index.ts` constantRoutes 中的 `/m/login`（登录）与 `/m`（MobileLayout → `daily-report/write`），全部懒加载 + hidden——桌面用户不加载移动 chunk，移动端 vant 及其 CSS 随移动 chunk 下发（入口 chunk 零增长）
- **页面**: `src/views/m/`（layout / login / dailyReport/write），UI 用 **Vant 4**（显式 `import { X as VanX } from 'vant'`，样式在 layout 与 login 各 `import 'vant/lib/index.css'` 一次）
- **守卫**: `permission.ts` 三处增量——whiteList 含 `/m/login`；无 token 访问 `/m/**` → `/m/login?redirect=...`；有 token 访问 `/m/login` → `/m`
- **字典**: 移动端不用 `<dict-select>`（EP 触控不适配），用 `useDict()` 取数 + Vant Picker/Checkbox 渲染（Constitution VI 例外已在 specs/014 plan.md 登记；禁止硬编码选项的底线不变）
- **坑**: van-stepper 配 `decimal-length` 时 v-model 回写**字符串**（如 "2.0"），所有工时求和/比较必须 `Number()` 强转
- **坑**: 手机深色模式下微信/iOS WebView 会**强制改写未声明颜色的文字**（背景显式 #fff + 文字继承默认色 → 白底白字不可见）。防御：`index.html` 已加 `<meta name="color-scheme" content="light">`，且移动页所有文字**必须显式写 color**，不得依赖继承
- **业务逻辑**: 移动填写页自带逻辑副本（不动桌面 write.vue），保存 payload 以 `specs/014-daily-report-mobile/data-model.md §3` 为唯一基准
- **E2E**: `tests/e2e-mobile-daily-report.spec.js`（iPhone 13 仿真 + 桌面双端一致用例；需临时关验证码 + admin 挂项目成员造数）

## Configuration

- `ruoyi-admin/src/main/resources/application.yml` — server port, logging, file upload path, DB, Redis, JWT
- `ruoyi-admin/src/main/resources/application-druid.yml` — connection pool
- `ruoyi-ui/.env.development` — `VITE_APP_BASE_API`
- `ruoyi-ui/vite.config.ts` — Vite config, dev proxy

**Ports:** Backend 8080 | Frontend dev 80 (`/dev-api` → 8080) | MySQL 3306 | Redis 6379 | Swagger `/swagger-ui.html` | Druid `/druid` (`ruoyi/123456`)

## Database & SQL Management

Database: `ry-vue` (MySQL 8.x, `utf8mb4_unicode_ci`). Init scripts in `pm-sql/init/`:
- `00_tables_ddl.sql` — All DDL
- `01_tables_data.sql` — Initial data (dict, config)
- `02_menu_data.sql` — Menu and permission data

Ad-hoc fixes: `pm-sql/fix_*.sql` — **not committed to git** (added to `.gitignore`). New tables → modify `00_tables_ddl.sql`. Schema changes on deployed DBs → create `fix_<feature>_<date>.sql`.

### Running SQL on Remote K3s MySQL

```bash
# Remote (pipe files — never -e with Chinese text)
cat /tmp/migration.sql | ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"

# Local Docker
CONTAINER=$(docker ps --filter "name=mysql" -q | head -1)
cat fix_something.sql | docker exec -i $CONTAINER mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

### Backup Strategy (Production k3s001)

**架构：纯 OSS——本地一份不留，全部只在 OSS。** 脚本 `KEEP_COUNT=0`：备份生成→上传 OSS 成功→删本地这份，本地稳态为空。

| 对象 | 频率 | 本地保留 | 单份 | 临时路径（上传后即删） |
|---|---|---|---|---|
| 数据库 (`ry-vue`) | **每 6 小时**（00/06/12/18:10） | **0 份** | ~4.9M | `/backup/newpm-mysql/newpm-YYYYMMDD-HHMM.sql.gz` |
| 附件 (upload-pvc) | 每日 01:20 | **0 份** | ~2.4G，月增 ~20% | `/backup/newpm-upload/newpm-upload-YYYYMMDD.tar.gz` |

脚本：`/usr/local/bin/backup-newpm-{db,upload}.sh` + `check-backup-health.sh`（每日 08:00 巡检），均在 root crontab。版本管理副本在 `ops/backup/`（改脚本要同步两边）。

**四道安全闸门**：磁盘不足拒备 / 文件<1MB 拒传 / 完整性校验（DB 查 `Dump completed on`、附件 `tar -tzf`）/ **上传 OSS 失败则拒绝清理本地**（纯 OSS 模式下这是安全垫——上传失败时该份留本地兜底，绝不会本地+OSS 两头皆空）。恢复必走 OSS 归档解冻（有延迟，非秒级）。

**附件脚本硬编码了 PVC 宿主机目录**（`/var/lib/rancher/k3s/storage/pvc-01f86b13-..._newpm_upload-pvc`）。upload-pvc 一旦重建，UUID 变化会导致备份失败——脚本有「源目录不存在则中止」闸门，重建 PVC 后必须同步改脚本。

**异地备份：阿里云 OSS 归档存储**（2026-07-24 起）

k3s001 本身是阿里云 ECS（cn-beijing-k），走**内网 endpoint** 上传，零流量费、不受出网代理影响。

| 项 | 值 |
|---|---|
| Bucket | `yada-newpm-backup`（华北2·北京 / **归档** / **LRS 本地冗余** / 私有） |
| 前缀 | `newpm-mysql/`（DB，单目录）、`newpm-upload/`（附件，单目录） |
| 凭证 | `/root/.ossutilconfig`（600），RAM 用户 `li.kong`，**策略无 Delete 权限**（防勒索） |
| 工具 | `ossutil` v1.7.18 |
| 上传脚本 | `/usr/local/bin/sync-backup-to-oss.sh <文件> <前缀>`（大小 + 存储类型双校验） |
| 日志 | `/backup/oss-sync.log` |

```bash
# 手动上传某个备份到 OSS
ssh k3s001 "sudo /usr/local/bin/sync-backup-to-oss.sh /backup/newpm-mysql/newpm-YYYYMMDD.sql.gz newpm-mysql"

# 查看 OSS 上的备份（stat 会 403——策略故意不含 GetObjectMeta，用 ls）
ssh k3s001 "sudo /usr/local/bin/ossutil ls oss://yada-newpm-backup/newpm-mysql/"

# 从 OSS 恢复：归档对象必须先解冻，之后才能下载
# ⚠️ 解冻收费(取回费~0.033元/GB + 下载流量,均不被2TB资源包抵扣)。纯OSS无本地兜底,任何恢复都触发。
ssh k3s001 "sudo /usr/local/bin/ossutil restore oss://yada-newpm-backup/newpm-mysql/newpm-YYYYMMDD.sql.gz"
```

**OSS 生命周期规则**（控制台已配 3 条，到期自动删——脚本无删权限，清理全靠这个）：
- `expire-db`：`newpm-mysql/` 30 天删除
- `expire-upload`：`newpm-upload/` 60 天删除（覆盖历史遗留的 daily/、monthly/ 子目录）
- `clean-parts`：整桶未完成分片 7 天清理

**归档存储三条硬约束**：① 最短计费 60 天（附件保留期不得低于 60 天；数据库 30 天虽触发但单份 5M 金额可忽略）；② 读取前必须解冻；③ 最小计量 64KB。资源包 2TB 到期 **2027-06-17**。

完整方案、决策调整与踩坑记录见 `docs/plans/2026-07-24-backup-to-oss-archive.md`（以 §11「最终落地参数」为准）。本地 Mac 第三副本在 `PM/PM-backups/`。

**宿主机磁盘**（148G，2026-07-24 实测 **77%** 已用 / 剩 33G）：大户是 `/var/lib/rancher` 36G（containerd 镜像层）、`/var/lib/docker` 24G、`/root` 18G、`/backup` 14G、`/data` 11G。备份只占 9.5%，不是吃盘主因。

```bash
# 手动触发数据库备份
ssh k3s001 "sudo /usr/local/bin/backup-newpm-db.sh"

# 手动触发附件备份
ssh k3s001 "sudo /usr/local/bin/backup-newpm-upload.sh"

# 查看备份状态
ssh k3s001 "ls -lah /backup/newpm-mysql/ && ls -lah /backup/newpm-upload/"

# 恢复数据库（从备份文件）
ssh k3s001 "zcat /backup/newpm-mysql/newpm-YYYYMMDD.sql.gz | kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
```

## Code Generation Workflow

Use `/ruoyi-gen` skill for interactive generation. Key files:
- **CLI JAR**: `ruoyi-gen-cli/target/ruoyi-gen-cli-3.9.1.jar`
- **DDL Source**: `pm-sql/init/00_tables_ddl.sql`
- **业务规格文档**: `docs/gen-specs/<table_name>.yml`
- **Default Config**: `ruoyi-generator/src/main/resources/generator.yml`

### ⚠️ `docs/gen-specs/*.yml` 不是 CLI 的输入文件

这两者是**两种不同格式**，不能混用（Issue #21）：

| | `docs/gen-specs/<table>.yml` | `ruoyi-gen-cli --config=<file>` |
|---|---|---|
| 用途 | 业务规格文档，给人与 AI 阅读 | CLI 的生成配置 |
| 顶层结构 | `basicInfo` / `columns` / `genInfo`（按代码生成器 UI 的标签页组织） | `global` / `tables`（见 `GenTableConfig.java`） |
| 直接喂给 CLI | ❌ 报 `Unable to find property 'basicInfo'` | ✅ |

CLI 配置的最小可用示例（已实测跑通）：

```yaml
global:
  author: ruoyi
  packageName: com.ruoyi.project
  moduleName: project
tables:
  pm_payment:                 # key 是表名，需与 --sql 中的 CREATE TABLE 对应
    className: Payment
    businessName: payment
    functionName: 款项管理
    columns:                  # 只需列出要覆盖默认值的列
      payment_method_name:
        isRequired: true      # 必填 → update 语句保留 <if> 守卫
      actual_quarter:
        isRequired: false     # 可选且可编辑 → update 语句无条件更新
      expected_quarter:
        isInsert: false       # 不在表单上 → 保留守卫（不在请求体里，无条件更新会写成 null）
      confirm_year:
        isEdit: false         # 不可编辑 → 保留守卫
```

`ColumnConfig` 可用字段：`columnComment` / `javaType` / `javaField` / `isInsert` / `isEdit` / `isList` / `isQuery` / `isRequired` / `queryType` / `htmlType` / `dictType`。

`--sql` 需要**单表 DDL**，从 `pm-sql/init/00_tables_ddl.sql` 中截取对应的 `CREATE TABLE ... ;` 即可。

**YAML 语法**：`docs/gen-specs/*.yml` 中未加引号的标量值若含 `: `（冒号+空格）或嵌套双引号，会导致整个文件无法被解析（Issue #21 曾有 3 个文件因此损坏）。写业务描述时用引号包裹；外层引号与内容里的引号要错开（内容含 `"` 就用 `'` 包外层）。

All generated Java code goes to `ruoyi-project` (not `ruoyi-admin`). Deploy: Java → `ruoyi-project/src/main/java/com/ruoyi/<module>/`, XML → `ruoyi-project/src/main/resources/mapper/<module>/`, Vue → `ruoyi-ui/src/views/<module>/`, API → `ruoyi-ui/src/api/<module>/`.

For master-detail generation: main table `tplCategory: sub`, sub table `tplCategory: crud`; set `subTableName`, `subTableFkName`, `subTableGenerateMenu` in `genInfo`.

**Spec file maintenance**: After business changes, check if `docs/gen-specs/<module>.yml` needs updating (field configs, business rules, schema changes need update; refactoring/renaming do not).

## Project Documentation

- **`docs/pm/PM需求.md`** — Complete business requirements (Chinese)
- **`docs/gen-specs/`** — Code generation specs (one YAML per table)
- **`docs/plans/`** — Implementation plans and design documents
- **`specs/<NNN-feature-name>/`** — [Spec Kit](https://github.com/github/spec-kit) feature workspaces (`spec.md` → `plan.md` → `tasks.md` + `data-model.md`/`research.md`/`quickstart.md`). Driven by the `/speckit-*` skills (`specify`/`plan`/`tasks`/`implement`/`analyze`/`clarify`). Each numbered dir is one feature increment (e.g. `001-daily-report-stats`); the "Active Technologies" / "Recent Changes" sections below are auto-maintained by these skills.

## Common Pitfalls

### Collation Mismatch

New PM tables use `utf8mb4_0900_ai_ci`; system tables use `utf8mb4_unicode_ci`. Always add `COLLATE` when joining:

```xml
left join sys_dict_data d on t.type COLLATE utf8mb4_unicode_ci = d.dict_value
LEFT JOIN sys_user u ON p.update_by COLLATE utf8mb4_unicode_ci = u.user_name
```

Map resolved names to separate display fields (e.g., `updateByName`) — not to `updateBy` (managed by BaseEntity).

### Department Filter: Use ancestors Hierarchy

```xml
<!-- Correct — matches dept itself OR any descendant -->
<if test="projectDept != null and projectDept != ''">
    and (p.project_dept = #{projectDept}
         or p.project_dept in (select dept_id from sys_dept where find_in_set(#{projectDept}, ancestors) > 0))
</if>
```

Applies to all PM mapper queries with `projectDept` filter.

### Hard Delete Exceptions

Two tables use hard delete (not soft `del_flag = '1'`):
- `pm_project` — `DELETE FROM pm_project`
- `pm_task` — `DELETE FROM pm_task` (no `del_flag` column)
- `pm_daily_report` + `pm_daily_report_detail` — both hard-deleted in a transaction

All other PM tables use soft delete. Do not add unique constraint workarounds for daily reports.

### 全量表单提交的 update 不要加 `<if>` 守卫

`<if test="xxx != null">` 是**部分更新**语义。用在**全量表单提交**的 update 上，会让「用户主动清空」与「字段未提交」不可区分 —— 清空意图被静默丢弃，界面提示保存成功但值不变。此缺陷已复发四次（`pm_payment` 三次：`expected_quarter` → `actual_payment_date` → `actual_quarter`，Issue #7；随后 Issue #10 跨 5 个 mapper 收口 15 个字段），前三次都只修了当次报上来的那一个字段。

**源头已治**：代码生成器模板 `mapper.xml.vm` 现按四维度分流 —— 必填 / `isInsert=0` / `isEdit=0` / 审计列 → 保留守卫，其余可选字段 → 无条件更新。新生成的模块不再带病出生。存量模块仍需按下面的规则逐个排查。

判断规则：

1. **口径按前端 `required` 判定，不是控件是否带 `clearable`。** 任何 `el-input` 用户都能手动删空，`el-input-number` 清空后是 `undefined` —— Issue #10 因只扫 `clearable` 控件而漏掉了 `taskBudget`。另注意 `el-date-picker` 与所有自定义 `*-select` 组件（`dict-select` / `user-select` / `project-dept-select` / `secondary-region-select` / `project-select`）的 `clearable` **默认就是 true**，日期字段因此是重灾区。判据是前端 `rules` 里有没有 `required`（含 validator 里写「不能为空」的）。

2. **解放任何字段前，必须逐个排查该 mapper 主 CRUD 语句的全部 Java 调用方。**

   ```bash
   grep -rn '\.updateXxx\s*(' ruoyi-project/src/main/java/
   ```

   Service 自身的 `updateXxx` 透传是正常的；要找的是「`new` 一个只填几个字段的裸实体」那种部分更新调用。Issue #10 中 `ProjectReviewServiceImpl.approveProject/rollbackProject` 正是这种写法，守卫一去掉就会在**每次项目审核时把该项目的 5 个日期写成 NULL**（已改走专用语句 `updateProjectApprovalFields`）。

   **「该 update 只有一个全量表单调用方」这个前提必须逐个 mapper 验证，不可跨模块复用结论** —— 它对 `PaymentMapper` 成立，对 `ProjectMapper` 不成立。

3. **前端不需要改。** 已实测：`@RequestBody` 绑定 Java Bean 时，「payload 中 key 缺失」与「显式传 null」效果完全相同（字段都是 Java 默认值 null），**后端去掉守卫即充分**。Issue #10 中一度加过 `normalizeClearable` 工具 + 8 个前端文件改动，实测后全部撤销 —— 不要重复这个弯路。

4. 同一个 update 里「一半字段有守卫、一半没有」是缺陷的化石记录 —— 看到不对称就该查。同理，同一个 mapper 文件里有多个主 CRUD 语句时（如 `VersionOutMapper` 的 `updateVersionOut` 与 `updateVersionOutManual`）必须一并处理，只改一个是新的化石。

只更新审核字段这类**专用局部更新语句**（`updateProjectApprovalFields` / `updateActualWorkload` 等）无条件更新是正常设计，不要动它们。

参考实现：Issue #10 的 commit `f8f5956`（5 个 mapper + 生成器模板 + `ProjectReviewServiceImpl`）；回归用例 `tests/clear-field-guards-regression.spec.js`（跨 7 模块，锁定「key 缺失」与「显式 null」两条清空路径 + 必填字段不被误清）与 `tests/payment-clear-field-regression.spec.js`。剩余 36 个未解放字段见 Issue #14。

### Task Fields Belong to pm_task, Not pm_project

All task-specific fields (`taskCode`, `batchId`, `productionYear`, `scheduleStatus`, `bankDemandNo`, `softwareDemandNo`, `product`, `internalClosureDate`, `functionalTestDate`, etc.) now live in `pm_task`. The `pm_project` table is being cleaned of these 19 legacy columns (see `docs/plans/2026-03-13-cleanup-project-task-fields.md`). Do NOT add task-related fields to `pm_project`.

### Project Members Include All Managers

`projectManagerId`, `marketManagerId`, `salesManagerId`, `teamLeaderId`, and `participants` on `pm_project` are all inserted into `pm_project_member` via `syncProjectMembers()`. Sub-project members are **not** inserted — they inherit the parent project's member list. **Important**: `syncProjectMembers()` must NOT update `pm_project.update_by/update_time` — it only manages the `pm_project_member` table.

### Cross-module Permission

When a page calls endpoints from multiple controllers, use `@ss.hasAnyPermi()`:

```java
@PreAuthorize("@ss.hasAnyPermi('project:attachment:list,project:project:query,project:contract:list')")
```

### Project Module Proxy APIs

Use these instead of direct `system/` endpoints (avoids 403 for PM-only users):

| Endpoint | Returns | Response field |
|---|---|---|
| `GET /project/project/users?postCode=xxx` | Users | `res.data` |
| `GET /project/project/deptTree` | Flat dept list (data-scoped) | `res.data` — must call `handleTree()` |
| `GET /project/project/deptTreeAll` | Flat dept list (no scope) | `res.data` |
| `GET /project/project/customers` | Customers | `res.data` |
| `GET /project/project/search` | Projects | `res.data` |

### Person-Days Calculation

```xml
ROUND(p.actual_workload / 8, 3) + COALESCE(p.adjust_workload, 0) AS actual_workload
```

Never display raw `actual_workload` hours as person-days. Adjustments logged in `pm_workload_correct_log`.

## CI/CD Pipeline

**GitHub Actions** (`.github/workflows/deploy.yml`): Push to `main` → Docker build → push `cuitaocrazy/newpm:latest` → SSH → `kubectl rollout restart deployment/ruoyi-app -n newpm`.

Ignores: `k8s/`, `pm-sql/`, `*.md`, `.github/`, `docker-compose*.yml`.

## Deployment

### Docker (One-JAR)

3-stage multi-stage build. **Source modified at build time** (not in repo):
1. Node 20: Build Vue, set `VITE_APP_BASE_API=/`
2. Maven+JDK17: SecurityConfig GET permits, SpaController, Linux upload path, console-only logback, `mvn package`
3. JRE Alpine: `java -Xms256m -Xmx1024m -jar app.jar`

Local: `docker-compose up -d` (MySQL 8.0 + Redis 7)

### Kubernetes

Namespace: `newpm`. Config in `k8s/`: namespace, app deployment, ConfigMap (profiles: `druid,k8s`), MySQL StatefulSet, Redis, Traefik IngressRoute. Attachments persist via `upload-pvc` mounted at `/app/uploadPath`.

```bash
kubectl apply -f k8s/
kubectl get pods -n newpm
kubectl logs -f deployment/ruoyi-app -n newpm
```

## Troubleshooting

- **Backend won't start**: Check MySQL (3306, `ry-vue`), Redis (6379), Java 17, port 8080, DB init scripts
- **Frontend build errors**: Delete `node_modules/` + reinstall; port 80 needs sudo; delete `node_modules/.vite/` for cache
- **Code generation**: Build CLI first; ensure valid MySQL 8.0 DDL; check menu SQL imported
- **Collation mismatch**: Add `COLLATE utf8mb4_unicode_ci` when joining system tables

## Active Technologies
- Java 17 / TypeScript 5.6 + Spring Boot 3.5.8, MyBatis, Apache POI (Excel), Vue 3.5, Element Plus 2.13, dayjs (001-daily-report-stats)
- MySQL 8.x (`ry-vue`)，涉及表：`pm_daily_report`、`pm_daily_report_detail`、`sys_user`、`sys_dept`、`pm_work_calendar`、`pm_daily_report_whitelist` (001-daily-report-stats)
- Java 17 / TypeScript 5.6 + Spring Boot 3.5.8 + MyBatis / Vue 3.5 + Element Plus 2.13 (003-team-daily-report)
- MySQL 8.x（只读查询，无新表） (003-team-daily-report)
- Java 17 / TypeScript 5.6 + Spring Boot 3.5.8, MyBatis, Vue 3.5, Element Plus 2.13 (004-daily-report-leave-types)
- MySQL 8.x (`ry-vue`)，涉及表：`sys_dict_data`、`pm_daily_report`、`pm_daily_report_detail`、`pm_work_calendar` (004-daily-report-leave-types)
- TypeScript 5.6 / Vue 3.5（前端 only） + Element Plus 2.13（`el-input` / `el-select` / `el-option`），既有 `formList` 响应式数据 (013-daily-report-write-filter)
- N/A（无持久化；查询条件为会话内 reactive 视图状态） (013-daily-report-write-filter)
- TypeScript 5.6 / Vue 3.5（前端 only，后端零改动） + Vant 4.9.x（新增，移动 UI）、Vue Router 4.6（既有）、Pinia 3（既有）、现有 request/auth/useDict 基础设施 (014-daily-report-mobile)
- N/A（复用既有接口，无 schema 变更；表 `pm_daily_report` / `pm_daily_report_detail` 读写不变） (014-daily-report-mobile)

## Recent Changes
- 001-daily-report-stats: Added Java 17 / TypeScript 5.6 + Spring Boot 3.5.8, MyBatis, Apache POI (Excel), Vue 3.5, Element Plus 2.13, dayjs

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan:
`specs/007-batch-version-management/plan.md` (批次版本管理 / 出入库版本，迁移自 yadapm)
<!-- SPECKIT END -->
