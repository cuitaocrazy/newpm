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

### E2E Testing (from project root)

```bash
npx playwright install && npx playwright test
npx playwright test contract-add-from-project.spec.ts   # specific file
npx playwright test --ui && npx playwright show-report
```

Test files in `tests/` directory: `project-management.spec.js`, `contract-add-from-project.spec.ts`, `network-request-debug.spec.js`, `query-smoke.spec.js`

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

| 对象 | 频率 | 保留 | 存储路径 |
|---|---|---|---|
| 数据库 (`ry-vue`) | 每日 00:10 | 30天 | `/backup/newpm-mysql/newpm-YYYYMMDD.sql.gz` |
| 附件 (upload-pvc) | 每周日 01:20 | 30天（约4份） | `/backup/newpm-upload/newpm-upload-YYYYMMDD.tar.gz` |

脚本：`/usr/local/bin/backup-newpm-db.sh`、`/usr/local/bin/backup-newpm-upload.sh`（均在 root crontab）

备份均存储在 k3s001 本机 `/backup/` 目录（148G 磁盘，当前 55% 占用）。**无异地备份**，如需恢复直接 ssh 取文件。

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
- **Spec Files**: `docs/gen-specs/<table_name>.yml`
- **Default Config**: `ruoyi-generator/src/main/resources/generator.yml`

All generated Java code goes to `ruoyi-project` (not `ruoyi-admin`). Deploy: Java → `ruoyi-project/src/main/java/com/ruoyi/<module>/`, XML → `ruoyi-project/src/main/resources/mapper/<module>/`, Vue → `ruoyi-ui/src/views/<module>/`, API → `ruoyi-ui/src/api/<module>/`.

For master-detail generation: main table `tplCategory: sub`, sub table `tplCategory: crud`; set `subTableName`, `subTableFkName`, `subTableGenerateMenu` in `genInfo`.

**Spec file maintenance**: After business changes, check if `docs/gen-specs/<module>.yml` needs updating (field configs, business rules, schema changes need update; refactoring/renaming do not).

## Project Documentation

- **`docs/pm/PM需求.md`** — Complete business requirements (Chinese)
- **`docs/gen-specs/`** — Code generation specs (one YAML per table)
- **`docs/plans/`** — Implementation plans and design documents

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
