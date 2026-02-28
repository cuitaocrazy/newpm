# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Table of Contents

- [Project Overview](#project-overview)
- [Quick Start](#quick-start)
- [Build & Run Commands](#build--run-commands)
- [Module Architecture](#module-architecture)
- [Backend Patterns](#backend-patterns)
  - [Controller Convention](#controller-convention)
  - [Response Types](#response-types)
  - [Custom Annotations](#custom-annotations)
  - [Data Permission](#data-permission)
  - [Entity Hierarchy](#entity-hierarchy)
  - [Naming Conventions](#naming-conventions)
  - [Master-Detail Pattern](#master-detail-pattern)
  - [Spring Boot 3 Specifics](#spring-boot-3-specifics)
  - [Logging Patterns](#logging-patterns)
  - [Exception Handling](#exception-handling)
  - [Transaction Management](#transaction-management)
  - [Async Processing](#async-processing)
- [Frontend Patterns](#frontend-patterns)
- [Configuration Files](#configuration-files)
- [Database Schema](#database-schema)
- [Code Generation Workflow](#code-generation-workflow)
- [SQL Management](#sql-management)
- [Project Documentation](#project-documentation)
- [Development Workflow](#development-workflow)
- [Common Pitfalls](#common-pitfalls)
- [CI/CD Pipeline](#cicd-pipeline)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

## Project Overview

RuoYi-Vue v3.9.1 — Enterprise admin system with separated frontend/backend, customized for **Project Management (PM)** business.

**Base Framework:** This project is built on [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue), an open-source rapid development platform. It uses the TypeScript branch of RuoYi-Vue3 for the frontend.
- **Official Documentation:** http://doc.ruoyi.vip
- **Demo Site:** http://vue.ruoyi.vip (credentials: admin/admin123)
- **Framework Features:** User/role/menu/dept/dict management, audit logs, scheduled tasks, code generation, API docs, monitoring tools

**Technology Stack:**
- **Backend:** Java 17 / Spring Boot 3.5.8 / Spring Security / MyBatis / Redis / JWT
- **Frontend:** Vue 3.5 / TypeScript 5.6 / Vite 6.4 / Element Plus 2.13 / Pinia
- **Business Domain:** Project lifecycle management, customer management, contract management, approval workflows, daily reports, and revenue recognition

## Quick Start

**First-time setup:**

```bash
# 1. Ensure MySQL 8.x and Redis are running
# 2. Create database and import schema
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS \`ry-vue\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p ry-vue < pm-sql/init/00_tables_ddl.sql
mysql -u root -p ry-vue < pm-sql/init/01_tables_data.sql
mysql -u root -p ry-vue < pm-sql/init/02_menu_data.sql

# 3. Build and run backend
mvn clean package -Dmaven.test.skip=true
java -Xms512m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
# Or use the helper script: ./ry.sh start

# 4. In another terminal, run frontend
cd ruoyi-ui && npm install && npm run dev
```

**Access:** http://localhost (default credentials: `admin/admin123`)

**Helper scripts:**
- `ry.sh` / `ry.bat` - Quick start/stop/restart scripts for backend server
  - `./ry.sh start` - Start backend service
  - `./ry.sh stop` - Stop backend service
  - `./ry.sh restart` - Restart backend service
  - `./ry.sh status` - Check service status

## Build & Run Commands

### Backend (from project root)

```bash
# Full build
mvn clean package -Dmaven.test.skip=true        # Build all modules
java -Xms512m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar  # Run
# Or use: ./ry.sh start

# Module-specific build
mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true     # Build admin module with dependencies
mvn clean package -pl ruoyi-gen-cli -am -Dmaven.test.skip=true   # Build CLI generator only
mvn clean compile -pl ruoyi-project -am                          # Compile project module only

# Clean
mvn clean                                        # Clean all
mvn clean -pl ruoyi-project                      # Clean specific module
```

### Frontend (from ruoyi-ui/)

```bash
npm install
npm run dev              # Vite dev server on port 80, proxies /dev-api → localhost:8080
npm run build:prod       # Production build → dist/
npm run build:stage      # Staging build
```

> **Shortcut**: From the project root, `npm run dev`, `npm run build`, and `npm run preview` are also available (npm workspaces proxy to `ruoyi-ui/`).

### Code Generation CLI (ruoyi-gen-cli)

```bash
mvn clean package -Dmaven.test.skip=true        # Build CLI JAR
java -jar ruoyi-gen-cli/target/ruoyi-gen-cli.jar --sql=<ddl>.sql --config=<config>.yml --output=<output>.zip
```

The CLI generates CRUD scaffolding from DDL without requiring MySQL/Redis. Use the `/ruoyi-gen` skill for interactive code generation.

### E2E Testing (from project root)

**Test Configuration:** `playwright.config.js` — Chromium, 30s timeout, 2 retries, screenshots/video/trace on failure, zh-CN locale, Asia/Shanghai timezone.

```bash
npx playwright install                    # Install browsers (first time only)
npx playwright test                       # Run all E2E tests
npx playwright test tests/project.spec.ts # Run specific test file
npx playwright test --ui                  # Run tests in UI mode
npx playwright test --headed              # Run tests in headed mode
npx playwright test --debug               # Run tests in debug mode
npx playwright show-report                # View test report
```

**Test Files (in `tests/`):**
- `project-management.spec.js` - Full project lifecycle (apply → list → edit → approval)
- `contract-add-from-project.spec.ts` - Contract creation from project list; validates auto-population of project info, dept, and customer
- `network-request-debug.spec.js` - Network debugging utilities

### Unit Testing (Backend)

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl ruoyi-project

# Run specific test class
mvn test -Dtest=ProjectServiceTest

# Skip tests during build
mvn clean package -Dmaven.test.skip=true
```

### Prerequisites

- **Java 17** - Required for Spring Boot 3.x
- **Maven 3.6+** - Build tool
- **MySQL 8.x** - Database (charset: `utf8mb4`, collation: `utf8mb4_unicode_ci`)
  - Database name: `ry-vue`
  - Default port: 3306
  - Init scripts: `pm-sql/init/00_tables_ddl.sql`, `01_tables_data.sql`, `02_menu_data.sql`
- **Redis 6.x+** - Cache and session storage
  - Default port: 6379
  - No password required for local development
- **Node.js 18+** with npm - Frontend development

## Module Architecture

```
ruoyi-admin       → Spring Boot entry, REST controllers (system/monitor/tool/common)
ruoyi-framework   → Security (JWT + Spring Security), AOP aspects, global exception handling
ruoyi-system      → Business services: user/role/menu/dept/dict/config/notice + MyBatis mappers
ruoyi-common      → Base classes, custom annotations, utilities (Excel, XSS filter, file ops)
ruoyi-quartz      → Quartz scheduled task management
ruoyi-generator   → Velocity-based CRUD code generator from DB tables
ruoyi-gen-cli     → Standalone CLI code generator (no MySQL/Redis required)
ruoyi-project     → Project management business module (see details below)
ruoyi-ui          → Vue 3 + TypeScript + Vite frontend (RuoYi-Vue3 typescript branch)
```

Dependencies flow: admin → framework → system → common. Quartz, generator, gen-cli, and project depend on common.

### ruoyi-project Module

**Purpose:** Project management business module for handling project lifecycle, customer management, and approval workflows.

**Package:** `com.ruoyi.project`

**Business Entities:**

| Entity | Table | Description |
|--------|-------|-------------|
| `Project` | `pm_project` | 项目管理 - Project lifecycle management with budget, workload, and approval tracking |
| `ProjectApproval` | `pm_project_approval` | 项目审核 - Project approval workflow (pending/approved/rejected) |
| `Customer` | `pm_customer` | 客户管理 - Customer information with industry and region classification |
| `CustomerContact` | `pm_customer_contact` | 客户联系人 - Customer contact persons |
| `Contract` | `pm_contract` | 合同管理 - Contract management with amount tracking and project associations |
| `ProjectContractRel` | `pm_project_contract_rel` | 项目合同关联 - Many-to-many relationship between projects and contracts |
| `Payment` | `pm_payment` | 款项管理 - Payment management with installment tracking and penalty handling |
| `Attachment` | `pm_attachment` | 附件管理 - File attachment management for projects, contracts, and payments |
| `AttachmentLog` | `pm_attachment_log` | 附件日志 - Audit log for attachment operations (upload/download/delete) |
| `SecondaryRegion` | `pm_secondary_region` | 二级区域 - Secondary region management for hierarchical region classification |
| `ProjectReview` | (view-based) | 公司收入确认 - Revenue recognition view with comprehensive project and contract data |
| `ProjectManagerChange` | `pm_project_manager_change` | 项目经理变更 - Track project manager changes with old/new manager, reason, and timestamp |
| `TeamRevenueConfirmation` | `pm_team_revenue_confirmation` | 团队收入确认 - Team-level revenue confirmation (separate from company-wide) |
| `ProjectMember` | `pm_project_member` | 项目成员 - Project team member management |
| `DailyReport` | `pm_daily_report` | 工作日报 - Daily work report management |
| `DailyReportDetail` | `pm_daily_report_detail` | 工作日报明细 - Daily report detail items linked to projects |
| `WorkCalendar` | `pm_work_calendar` | 工作日历 - Work calendar for tracking working days and holidays |

**Key Features:**

- **Project Management:** Full project lifecycle tracking including:
  - Project coding: `{industry}-{region}-{shortName}-{year}` format
  - Budget management: project budget, cost budget (万元), labor cost (万元), purchase cost (万元)
  - Workload tracking: estimated vs actual workload (in person-days)
  - Timeline: start/end/production/acceptance dates
  - Team assignment: project manager, market manager, sales manager, team leader, participants
  - Status tracking: project stage (字典: sys_xmjd), acceptance status (字典: sys_yszt)

- **Customer Management:** Customer database with:
  - Industry classification (字典: industry)
  - Region classification (字典: sys_yjqy)
  - Sales manager assignment
  - Multiple contact persons per customer

- **Contract Management:** Contract lifecycle tracking including:
  - Contract coding and naming
  - Contract type and status (字典: sys_htlx, sys_htzt)
  - Amount tracking: contract amount, received amount, remaining amount
  - Signing and effective dates
  - Project associations via `pm_project_contract_rel` (many-to-many)
  - Attachment support for contract documents

- **Payment Management:** Payment tracking with:
  - Payment method and installment tracking
  - Amount management: payment amount, penalty amount
  - Payment status (字典: sys_fkzt)
  - Expected vs actual payment dates
  - Quarterly tracking for expected payments
  - Penalty handling for contract violations
  - Attachment support for payment documents

- **Attachment Management:** Unified file management system:
  - Business type: project/contract/payment
  - Document type classification (字典: sys_wdlx)
  - File metadata: name, path, size, type
  - Upload/download/delete operations with audit logging
  - Version control support

- **Approval Workflow:** Project approval process with:
  - Approval status: 0=pending, 1=approved, 2=rejected
  - Approval reason/comments
  - Approver tracking with timestamp

**Business Workflows (from `docs/pm/PM需求.md`):**

1. **Project Initiation (立项申请):**
   - Project manager/market manager submits new project application
   - System generates project code: `{industry}-{region}-{shortName}-{year}`
   - Includes: basic info, personnel, customer, timeline, cost budget
   - Status: 待审核 (pending approval)

2. **Project Approval (项目审核):**
   - Project supervisor reviews and approves/rejects applications
   - Approval actions: 通过 (approve), 不通过 (reject)
   - Tracks approval history with comments

3. **Contract Management (合同管理):**
   - Link contracts to projects
   - Track contract status: 未签署/已签署
   - Payment tracking with multiple payment terms
   - Payment status: 未开未付, 已提交验收材料, 验收材料已审核, 待通知开票, 已通知

4. **Daily Reports (工作日报):**
   - Employees submit daily work reports linked to projects
   - Master-detail: `pm_daily_report` (header with date) → `pm_daily_report_detail` (items per project)
   - Track actual workload (person-days) per project per day
   - Two views: write/edit page (`write.vue`) and activity feed (`activity.vue`)
   - Work calendar integration (`pm_work_calendar`) for tracking working/non-working days
   - Calendar view for team daily reports
   - Statistics: project person-day aggregation, team reports

5. **Revenue Recognition (收入确认):**
   - Comprehensive revenue confirmation view with multi-dimensional filtering
   - Query conditions: project name, department, confirmation year, project category, primary/secondary region, project manager, market manager, initiation year, approval status, acceptance status, contract status, confirmation status
   - Track confirmation status: 未确认, 待确认, 已确认, 无法确认
   - Link to contract payments and project completion
   - Support batch revenue confirmation operations

6. **Secondary Region Management (二级区域管理):**
   - Hierarchical region classification (primary region → secondary region)
   - Used for detailed geographical organization of projects and customers

7. **Project Manager Change (项目经理变更):**
   - Record and track project manager changes with change reason
   - Fields: oldManagerId/Name, newManagerId/Name, changeReason, changeTime
   - Supports single and batch change operations
   - DTOs: `ChangeRequest` (single), `BatchChangeRequest` (batch), `ProjectManagerChangeVO` (view)

8. **Team Revenue Confirmation (团队收入确认):**
   - Department-level revenue confirmation (distinct from company-wide ProjectReview)
   - Fields: teamConfirmId, projectId, deptId, confirmAmount, confirmTime, confirmUserId

9. **Add Contract from Project (项目列表添加合同):**
   - Project list row shows a contextual button: "添加合同" (no contract yet) or "查看合同" (contract exists)
   - Clicking "添加合同" navigates to contract creation with auto-populated: projectId, project dept, customer info
   - Alert displays: "将为项目创建合同：{projectName} ({projectCode})"
   - Revenue confirmation fields on `Project`: `confirmAmount`, `taxRate`, `afterTaxAmount` (after-tax = confirmAmount / (1 + taxRate/100)), `revenueConfirmStatus`, `revenueConfirmYear`, `companyRevenueConfirmedBy`, `companyRevenueConfirmedTime`

**Dictionary Dependencies:**

- `industry` - 行业分类
- `sys_yjqy` - 区域分类 (一级区域, 二级区域)
- `sys_xmfl` - 项目分类 (软件开发类, 硬件销售类, 系统集成类, 开发人力外包, 运维人力外包, 测试人力外包)
- `sys_xmjd` - 项目阶段 (0=其他, 1=售前支持, 2=需求及设计, 3=开发及自测, 4=验收测试, 5=系统投产, 6=免维期维护, 7=项目结项)
- `sys_yszt` - 验收状态
- `sys_xmzt` - 项目状态 (未启动, 已启动, 已测试, 已投产, 已结项, 已关闭)
- `sys_htlx` - 合同类型
- `sys_htzt` - 合同状态 (未签署, 已签署)
- `sys_fkzt` - 付款状态 (未开未付, 已提交验收材料, 验收材料已审核, 待通知开票, 已通知)
- `sys_wdlx` - 文档类型
- `sys_qrzt` - 确认状态 (1=1-未确认, 2=2-待确认收入, 3=3-已确认收入, 4=4-无法确认)
- `sys_srqrzt` - 收入确认状态 (0=未确认, 1=已确认, 2=待确认, 3=已确认收入)

**Controllers:**

- `ProjectController` - `/project/project/**` - Project CRUD and approval operations
- `ProjectApprovalController` - `/project/approval/**` - Approval workflow management
- `ProjectReviewController` - `/project/review/**` - Company-wide revenue recognition
- `ProjectManagerChangeController` - `/project/managerChange/**` - Project manager change records
- `TeamRevenueConfirmationController` - `/project/teamRevenue/**` - Team-level revenue confirmation
- `CustomerController` - `/project/customer/**` - Customer management
- `CustomerContactController` - `/project/contact/**` - Contact management
- `ContractController` - `/project/contract/**` - Contract CRUD with amount summary
- `PaymentController` - `/project/payment/**` - Payment management
- `AttachmentController` - `/project/attachment/**` - File upload/download/delete with audit logging
- `SecondaryRegionController` - `/project/secondaryRegion/**` - Secondary region management
- `DailyReportController` - `/project/dailyReport/**` - Daily work report CRUD and monthly summary
- `WorkCalendarController` - `/project/workCalendar/**` - Work calendar management
- `ProjectMemberController` - `/project/member/**` - Project member management

**Frontend Routes:**

- `/project/project` - 项目列表 (Project list with approval actions and contract buttons)
- `/project/apply` - 项目立项申请 (Project initiation application)
- `/project/approval` - 项目审核 (Project approval workflow)
- `/project/review` - 公司收入确认 (Company-wide revenue recognition with comprehensive filtering)
- `/project/contract` - 合同列表 (Contract list with amount tracking)
- `/project/payment` - 款项列表 (Payment list with installment tracking)
- `/project/customer` - 客户管理 (Customer management)
- `/project/secondaryRegion` - 二级区域管理 (Secondary region management)
- `/project/managerChange` - 项目经理变更 (Project manager change records)
- `/project/dailyReport` - 工作日报 (Daily work report writing and activity feed)
- `/project/workCalendar` - 工作日历 (Work calendar management)
- `/project/projectMember` - 项目成员 (Project member management)
- `/revenue/company` - 公司收入确认详情 (Company revenue confirmation detail view)
- `/revenue/team` - 团队收入确认 (Team-level revenue confirmation)

## Backend Patterns

### Controller Convention

All controllers extend `BaseController`. Standard CRUD pattern:

```java
// List — startPage() MUST be called before the query
@PreAuthorize("@ss.hasPermi('module:entity:list')")
@GetMapping("/list")
public TableDataInfo list(Entity entity) {
    startPage();
    List<Entity> list = service.selectEntityList(entity);
    return getDataTable(list);
}

// Insert/Update/Delete — wrap with toAjax() and @Log
@Log(title = "实体名", businessType = BusinessType.INSERT)
@PreAuthorize("@ss.hasPermi('module:entity:add')")
@PostMapping
public AjaxResult add(@Validated @RequestBody Entity entity) {
    return toAjax(service.insertEntity(entity));
}
```

### Response Types

- `AjaxResult` → `{ code, msg, data }` for single-object responses
- `TableDataInfo` → `{ code, msg, total, rows }` for paginated lists

### Custom Annotations

| Annotation | Purpose |
|---|---|
| `@Log(title, businessType)` | Operation audit log via LogAspect (async) |
| `@DataScope(deptAlias, userAlias)` | Data permission SQL filter injection |
| `@DataSource` | Dynamic datasource switching |
| `@RateLimiter(time, count)` | Redis Lua-based rate limiting |
| `@RepeatSubmit` | Duplicate form submission prevention |
| `@Excel(name)` | Excel import/export column config |
| `@Anonymous` | Bypass JWT authentication |
| `@Sensitive` | JSON serialization data masking |

### Data Permission

`@DataScope(deptAlias = "d", userAlias = "u")` on service methods. The aspect injects SQL into `${params.dataScope}` in MyBatis XML based on the user's role scope (1=all, 2=custom depts, 3=own dept, 4=dept+children, 5=self only).

### Entity Hierarchy

- `BaseEntity` → createBy/createTime/updateBy/updateTime/remark/params(Map)
- `TreeEntity extends BaseEntity` → parentId/ancestors for hierarchical data

### Naming Conventions

- Packages: `com.ruoyi.{module}.controller|service|domain|mapper`
- Service methods: `select*List()`, `select*ById()`, `insert*()`, `update*()`, `delete*ByIds()`
- Permission strings: `{module}:{business}:{action}` (e.g. `system:user:list`)

### Master-Detail Pattern

The project uses master-detail (主子表) pattern for one-to-many relationships:

- **Project-Contract:** Projects can have multiple contracts via `pm_project_contract_rel` junction table
- **Contract-Payment:** Contracts can have multiple payment installments
- **Business-Attachment:** Projects, contracts, and payments can have multiple attachments

**Implementation pattern:**
```java
// Master entity includes detail list
public class Contract extends BaseEntity {
    private List<Payment> paymentList;  // Detail records
}

// Service handles cascading operations
public int insertContract(Contract contract) {
    int rows = contractMapper.insertContract(contract);
    insertPayment(contract);  // Insert detail records
    return rows;
}
```

**MyBatis XML uses collection mapping:**
```xml
<resultMap id="ContractPaymentResult" type="Contract">
    <collection property="paymentList" ofType="Payment" column="contract_id" select="selectPaymentList"/>
</resultMap>
```

### Spring Boot 3 Specifics

- Security uses `SecurityFilterChain` bean + `@EnableMethodSecurity` (not WebSecurityConfigurerAdapter)
- Jakarta EE namespace (`jakarta.servlet.*` not `javax.servlet.*`)
- API docs via springdoc-openapi (`/v3/api-docs`, `/swagger-ui.html`)
- Druid starter: `druid-spring-boot-3-starter`
- MySQL driver: `mysql-connector-j`

### Logging Patterns

**Log Files Location:** `./logs/` (60-day rotation)

- `sys-info.log` - INFO level system logs
- `sys-error.log` - ERROR level logs only
- `sys-user.log` - User operation logs

**Operation Audit Logging:**
```java
// Controller methods - audit logs to database
@Log(title = "项目管理", businessType = BusinessType.INSERT)
@PostMapping
public AjaxResult add(@RequestBody Project project) {
    return toAjax(projectService.insertProject(project));
}
```

**Standard Logging:**
```java
// Service layer - use SLF4J Logger
private static final Logger log = LoggerFactory.getLogger(YourClass.class);

log.info("Processing project: {}", projectId);
log.error("Failed to update contract", e);
```

**Important:**
- `@Log` annotation → async database audit via LogAspect (operation logs viewable in UI)
- SLF4J logger → file logs (sys-info.log, sys-error.log)
- Sensitive fields (password, oldPassword, newPassword, confirmPassword) are auto-excluded from logs
- Request params > 2000 chars are truncated

### Exception Handling

**Global Exception Handler:** `GlobalExceptionHandler` catches all exceptions with `@RestControllerAdvice`

**Business Exceptions - Use ServiceException:**
```java
// In service layer
if (project == null) {
    throw new ServiceException("项目不存在");
}

// With custom error code
throw new ServiceException("操作失败", 500);
```

**Exception Types:**
- `ServiceException` - Business logic exceptions (caught by GlobalExceptionHandler → AjaxResult.error)
- `AccessDeniedException` - Permission denied (returns 403)
- `BindException` / `MethodArgumentNotValidException` - Validation errors (returns field error message)

**Pattern:**
```java
// Service throws ServiceException
public int updateProject(Project project) {
    if (!checkPermission(project)) {
        throw new ServiceException("无权限修改此项目");
    }
    return projectMapper.updateProject(project);
}

// Controller auto-converts to AjaxResult
@PutMapping
public AjaxResult edit(@RequestBody Project project) {
    return toAjax(projectService.updateProject(project));  // Exceptions auto-handled
}
```

**All exceptions are logged and returned as JSON with appropriate HTTP status codes.**

### Transaction Management

**Use @Transactional on service methods that modify data:**

```java
@Service
public class ContractServiceImpl implements IContractService {

    @Transactional  // Rollback on any RuntimeException (including ServiceException)
    public int insertContract(Contract contract) {
        int rows = contractMapper.insertContract(contract);
        insertPayment(contract);  // Both operations in same transaction
        return rows;
    }
}
```

**Transaction Behavior:**
- Auto-rollback on `RuntimeException` (including `ServiceException`)
- No rollback on checked exceptions (unless configured)
- Transactions managed by Spring's `PlatformTransactionManager`

**Master-detail operations MUST be transactional to ensure data consistency.**

### Async Processing

**Operation Audit Logs:** Automatically async via `AsyncManager` (used by `@Log` annotation)

**Custom Async Methods (implemented in `ProjectEmailServiceImpl`):**
```java
@Service
public class ProjectEmailServiceImpl {

    @Async  // Runs in separate thread pool
    public void sendNotificationEmail(Project project) {
        // Sends approval result notification (approved/rejected with reason) to project creator
    }
}
```

**Usage:**
```java
// Fire and forget - no blocking
projectEmailService.sendNotificationEmail(project);
```

**Async operations run in Spring's task executor thread pool (configured in application.yml).**

## Frontend Patterns

### Tech Stack

Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router 4

### Directory Structure (ruoyi-ui/)

```
src/
├── api/              → API functions (typed with interfaces from types/)
│   ├── project/      → Project business APIs (project, contract, payment, customer, etc.)
│   └── revenue/      → Revenue module APIs (company.js/ts, team.js)
├── assets/           → Static assets (images, styles)
├── components/       → Reusable Vue components
├── directive/        → Custom directives (v-hasPermi, v-hasRole, etc.)
├── layout/           → Layout components (navbar, sidebar, tags-view)
├── plugins/          → Plugin configurations (modal, download, cache, etc.)
├── router/           → Vue Router configuration
├── store/            → Pinia stores (user, app, permission, settings, tagsView, dict)
├── types/            → TypeScript type definitions
│   └── api/          → API response types
├── utils/            → Utility functions (request.ts, auth.ts, etc.)
└── views/            → Page components
    ├── system/       → System management pages
    ├── project/      → Project management pages (custom business)
    └── revenue/      → Revenue confirmation module (company/ and team/ subdirectories)
```

### API Layer

API functions in `src/api/` are fully typed with interfaces from `src/types/`. Pattern:

```typescript
export function listUser(query: UserQueryParams): Promise<TableDataInfo<SysUser[]>> {
  return request({ url: '/system/user/list', method: 'get', params: query })
}
```

### HTTP Client (`src/utils/request.ts`)

- Axios with `VITE_APP_BASE_API` as baseURL
- Auto-injects `Authorization: Bearer {token}` header
- Built-in duplicate submission prevention (configurable via `headers.repeatSubmit` and `headers.interval`)
- Response codes: 200=success, 401=re-login prompt, 500=error, 601=warning

**Usage in Vue Components:**

```typescript
// Method 1: Use encapsulated API functions (Recommended)
import { listUser } from '@/api/system/user'
listUser(queryParams).then(response => {
  // response.rows, response.total
})

// Method 2: Direct request call (for custom endpoints)
import request from '@/utils/request'
request({
  url: '/system/user/listByPost',
  method: 'get',
  params: { postCode: 'xsfzr' }
}).then(response => {
  // response.data
})

// WRONG: Do NOT use proxy.$http or proxy.request
// const { proxy } = getCurrentInstance()
// proxy.$http.get(...)  ❌ Does not exist
// proxy.request(...)    ❌ Does not exist
```

### State Management (Pinia)

Stores in `src/store/modules/`: user, app, permission, settings, tagsView, dict

### Permission

- Route-level: dynamic routes loaded from backend based on user roles/permissions
- Element-level: `v-hasPermi` directive for component visibility
- Route meta: `permissions: ['a:b:c']` and `roles: ['admin']`

### Global Components

Registered in `main.ts`: DictTag, Pagination, FileUpload, ImageUpload, ImagePreview, RightToolbar, Editor

### Type Definitions

All API types in `src/types/api/`. Key types: `AjaxResult<T>`, `TableDataInfo<T>`, `BaseEntity`, `PageDomain`

### Development Tips

**Debugging Frontend:**
- Use Vue DevTools browser extension to inspect component state and Pinia stores
- Check Network tab for API calls (look for `/dev-api` prefix)
- Console errors often indicate missing permissions or incorrect API responses
- Use `console.log(proxy)` in setup() to see available global properties (but don't use `proxy.$http`)

**Hot Module Replacement (HMR):**
- Vite dev server supports HMR - changes to `.vue` files reflect immediately
- If HMR breaks, restart dev server: `Ctrl+C` then `npm run dev`
- Changes to `router/index.ts` or `store/` may require manual refresh

### File Upload Pattern

The project uses a unified attachment management system:

**Backend:**
- `AttachmentController` handles file operations with audit logging
- Files stored with metadata in `pm_attachment` table
- Operations logged in `pm_attachment_log` for compliance
- Business type discriminator: `project`, `contract`, `payment`

**Frontend:**
- `FileUpload` component for single/multiple file uploads
- `ImageUpload` component for image-specific uploads
- `ImagePreview` component for image viewing
- API functions in `src/api/project/attachment.js`

**Usage example:**
```vue
<file-upload
  v-model="form.attachmentIds"
  :business-type="'contract'"
  :business-id="form.contractId"
/>
```

## Configuration Files

### Backend
- `ruoyi-admin/src/main/resources/application.yml` - Main config (server port, logging, file upload path)
- `ruoyi-admin/src/main/resources/application-druid.yml` - Database connection pool config
- Database connection, Redis, JWT settings are in `application.yml`

### Frontend
- `ruoyi-ui/.env.development` - Dev environment variables (`VITE_APP_BASE_API`)
- `ruoyi-ui/.env.production` - Production environment variables
- `ruoyi-ui/vite.config.ts` - Vite build config and dev server proxy

### Redis Usage

**Redis is used for:**
- JWT token storage and validation
- Session management
- Dictionary cache (sys_dict_data)
- Rate limiting (via @RateLimiter annotation)

**Not used:** Spring Cache annotations (@Cacheable, @CachePut, @CacheEvict) - Redis is accessed directly via RedisTemplate/RedisCache utilities

## Ports & Monitoring

- **Backend**: 8080
- **Frontend dev**: 80 (proxies `/dev-api` → `http://localhost:8080`)
- **Druid monitor**: http://localhost:8080/druid (credentials: `ruoyi/123456`)
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **MySQL**: 3306 (database: `ry-vue`)
- **Redis**: 6379

## Database Schema

Database initialization scripts are in `pm-sql/init/`:
- `00_tables_ddl.sql` - Table structure definitions (all business tables)
- `01_tables_data.sql` - Initial data and dictionary entries
- `02_menu_data.sql` - Menu and permission data for UI navigation

**Database name:** `ry-vue` (MySQL 8.x, charset: `utf8mb4`, collation: `utf8mb4_unicode_ci`)

When adding new business modules, place DDL files in `pm-sql/init/00_tables_ddl.sql` for the `/ruoyi-gen` skill to discover.

**One-off migration scripts:** `pm-sql/fix_*.sql` - Ad-hoc fixes and data migrations (not part of init sequence)

## Code Generation Workflow

Use the `/ruoyi-gen` skill for interactive CRUD generation:

1. **Input modes**: DDL SQL, business description, or database query
2. **Smart inference**: Automatically infers field types, query types, and UI components from DDL
3. **Configuration**: Generates spec file in `docs/gen-specs/<table>.yml` for review
4. **Deployment**: Generates and deploys code to appropriate modules (backend + frontend)
5. **Customization**: Applies custom UI requirements from spec file

The skill handles menu generation, SQL file management, and database imports automatically.

### Configuration File Maintenance

**IMPORTANT**: When making changes to any business module, always check and confirm whether the corresponding spec file needs to be updated:

- **Files**: `docs/gen-specs/<module>.yml` (e.g., `pm_contract.yml`, `pm_project.yml`, `revenue_company.yml`)
- **Purpose**: Each spec file documents all customizations, business rules, and implementation details for that module
- **Special focus**: `pm_contract.yml` is the most actively maintained — always review it after contract changes
- **Workflow**:
  1. After any contract feature optimization or adjustment, review the changes
  2. Analyze which changes need to be documented in pm_contract.yml:
     - **NEED to update**: Field configurations, business rules, UI customizations, API endpoints, database schema changes, bug fixes with implementation details
     - **NO NEED to update**: Minor code refactoring without behavior changes, code comments, variable renaming, performance optimizations without logic changes
  3. Present a detailed recommendation to the user with two sections:
     - **Changes that NEED pm_contract.yml update**: List specific changes with reasons
     - **Changes that DO NOT NEED pm_contract.yml update**: List changes that are implementation details only
  4. Wait for user confirmation before updating the file
  5. If confirmed, update the relevant sections in the YAML file with clear notes and timestamps

**What to document**:
- ✅ Field configuration changes (validation rules, default values, display settings)
- ✅ Business logic modifications (calculation formulas, workflow rules)
- ✅ UI customizations (component changes, layout adjustments, interaction patterns)
- ✅ API endpoint changes (new endpoints, parameter modifications)
- ✅ Database schema updates (new fields, constraint changes)
- ✅ Bug fixes with root cause and solution details
- ❌ Code refactoring without behavior changes
- ❌ Internal implementation details (variable names, code structure)
- ❌ Performance optimizations without logic changes
- ❌ Code comments or documentation updates

**Example interaction**:
```
After optimization, I will present:

"Should we update pm_contract.yml to document these changes?

✅ NEED to update:
1. [Change description] - Reason: Affects field display/business logic/user interaction
2. [Change description] - Reason: New feature/bug fix that should be documented

❌ NO NEED to update:
1. [Change description] - Reason: Internal refactoring only
2. [Change description] - Reason: Code optimization without behavior change

Please confirm if you want me to update the configuration file."
```

This ensures the configuration file stays synchronized with the actual implementation and serves as accurate documentation for future code generation.

## Code Generation CLI Details (ruoyi-gen-cli)

This project includes a custom CLI-based code generator that generates CRUD scaffolding from DDL without requiring a running database.

### Key Files

- **CLI JAR**: `ruoyi-gen-cli/target/ruoyi-gen-cli-3.9.1.jar`
- **DDL Source**: `pm-sql/init/00_tables_ddl.sql` (all table DDLs)
- **Menu SQL**: `pm-sql/init/02_menu_data.sql` (menu permission SQL)
- **Spec Files**: `docs/gen-specs/<table_name>.yml` (generation config per table)
- **Default Config**: `ruoyi-generator/src/main/resources/generator.yml`

### Generation Process

1. **Find DDL**: Extract table DDL from `pm-sql/init/00_tables_ddl.sql`
2. **Generate Spec**: Create YAML config in `docs/gen-specs/` with field mappings, query types, HTML types
3. **Build CLI** (if needed): `mvn clean package -pl ruoyi-gen-cli -am -Dmaven.test.skip=true`
4. **Run CLI**: `java -jar ruoyi-gen-cli/target/ruoyi-gen-cli-3.9.1.jar --sql=<ddl>.sql --config=<spec>.yml --output=<out>.zip`
5. **Deploy**: Extract ZIP and copy to:
   - Java code → `ruoyi-project/src/main/java/com/ruoyi/<module>/`
   - MyBatis XML → `ruoyi-project/src/main/resources/mapper/<module>/`
   - Vue pages → `ruoyi-ui/src/views/<module>/<business>/`
   - API functions → `ruoyi-ui/src/api/<module>/`
6. **Menu SQL**: Append generated menu SQL to `pm-sql/init/02_menu_data.sql` (deduplicated)

**Important**: All generated Java code goes to `ruoyi-project` module (not `ruoyi-admin`). The CLI uses Velocity templates and supports master-detail tables.

### Master-Detail Table Support

When generating master-detail relationships:
- Set `tplCategory: sub` in main table config
- Include `subTableName`, `subTableFkName`, `subTableGenerateMenu` in `genInfo`
- Both main and sub table DDLs must be in the SQL file
- Both table configs must be in the YAML file
- Sub table's `tplCategory` must be `crud` (not `sub`)

### Skill Integration

Use the `ruoyi-gen` skill for interactive code generation. The skill automates the entire workflow with user confirmation at each step.

## SQL Management

### Database Initialization

- `sql/` - Original RuoYi system SQL (legacy)
- `pm-sql/init/` - Project-specific SQL (active):
  - `00_tables_ddl.sql` - All table DDL definitions
  - `01_tables_data.sql` - Initial data (dict, config, etc.)
  - `02_menu_data.sql` - Menu and permission data
- `pm-sql/fix_*.sql` - One-off migration scripts

**Pattern**: New tables go to `00_tables_ddl.sql`, menu changes go to `02_menu_data.sql`.

## Project Documentation

- **`docs/pm/PM需求.md`** - Complete business requirements document (Chinese)
  - Project management workflows
  - Customer and contract management
  - Daily reports and statistics
  - Revenue recognition
  - Dictionary definitions

- **`docs/gen-specs/`** - Code generation specification files (YAML)
  - One file per table: `<table_name>.yml`
  - Contains field mappings, query types, HTML types, and customizations
  - Used by `/ruoyi-gen` skill for interactive code generation

- **`docs/plans/`** - Implementation plans and design documents

## Development Workflow

### Adding a New Business Module

1. Design table schema and add DDL to `pm-sql/init/00_tables_ddl.sql`
2. Run DDL in MySQL to create table
3. Use `ruoyi-gen` skill or manual CLI to generate code
4. Deploy generated code to `ruoyi-project` and `ruoyi-ui`
5. Customize generated code as needed (add business logic, UI enhancements)
6. Add menu SQL to `pm-sql/init/02_menu_data.sql`
7. Test and commit

### Running Backend Locally

```bash
# Ensure MySQL and Redis are running
# Database: ry-vue (init with pm-sql/init/*.sql)
mvn clean package -Dmaven.test.skip=true
java -Xms256m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
```

### Running Frontend Locally

```bash
cd ruoyi-ui
npm install
npm run dev  # Vite dev server on port 80
```

Frontend proxies `/dev-api` to `http://localhost:8080` (see `vite.config.ts`).

## Common Pitfalls

### Collation Mismatch Error

**Error**: `Illegal mix of collations (utf8mb4_0900_ai_ci,IMPLICIT) and (utf8mb4_unicode_ci,IMPLICIT)`

**Cause**: New tables use MySQL 8.0 default collation `utf8mb4_0900_ai_ci`, but system tables use `utf8mb4_unicode_ci`.

**Fix**: In MyBatis XML, explicitly cast when joining system tables:

```xml
<!-- Wrong -->
left join sys_dict_data d on t.type = d.dict_value

<!-- Correct -->
left join sys_dict_data d on t.type COLLATE utf8mb4_unicode_ci = d.dict_value
```

### Frontend HTTP Request Pattern

**Correct**:
```typescript
import request from '@/utils/request'
request({ url: '/api/path', method: 'get' })
```

**Wrong**:
```typescript
const { proxy } = getCurrentInstance()
proxy.$http.get(...)  // Does not exist
```

### Pagination in Controller

Always call `startPage()` before query:

```java
@GetMapping("/list")
public TableDataInfo list(Entity entity) {
    startPage();  // MUST call this first
    List<Entity> list = service.selectList(entity);
    return getDataTable(list);
}
```

## CI/CD Pipeline

**GitHub Actions** (`.github/workflows/deploy.yml`): Pushes to `main` auto-deploy to K3s.

- **Trigger**: Push to `main` (ignores `k8s/`, `sql/`, `*.md`, `.github/`, `docker-compose*.yml`)
- **Steps**: Docker build → push to Docker Hub (`cuitaocrazy/newpm:latest`) → SSH to server → `kubectl rollout restart deployment/ruoyi-app -n newpm`
- **Secrets**: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `SERVER_HOST`, `SERVER_USER`, `SERVER_SSH_KEY`

## Deployment

### Docker One-JAR Deployment

The `Dockerfile` uses a 3-stage multi-stage build that bundles Vue frontend into the Spring Boot JAR. **Source code is modified at build time** (not in the repo):

1. **Stage 1 (Node 20)**: Build Vue frontend, change `VITE_APP_BASE_API` from `/prod-api` to `/` (same-origin)
2. **Stage 2 (Maven + JDK 17)**: Apply build-time patches, then `mvn clean package`:
   - `SecurityConfig`: Permit all GET requests (SPA routes reach `SpaController`; API data protected by `@PreAuthorize`)
   - Remove `SysIndexController` (its `/` mapping conflicts with `index.html`)
   - Create `SpaController` for Vue Router history mode fallback (`forward:/index.html`)
   - Fix upload path for Linux: `D:/ruoyi/uploadPath` → `/app/uploadPath`
   - Replace logback config with console-only output (K8s-friendly)
   - Copy Vue `dist/` into `src/main/resources/static/`
3. **Stage 3 (JRE Alpine)**: Runtime with `java -Xms256m -Xmx1024m -jar app.jar`

```bash
# Build and run locally with docker-compose (includes MySQL 8.0 + Redis 7)
docker-compose up -d
```

### Kubernetes Deployment

Namespace: `newpm`. Configuration in `k8s/`:

- `namespace.yml` - Namespace definition
- `app.yml` - Backend deployment (image: `cuitaocrazy/newpm:latest`)
- `config.yml` - ConfigMap with Spring application config (profiles: `druid,k8s`)
- `mysql.yml` - MySQL 8.0 StatefulSet
- `redis.yml` - Redis deployment
- `ingress.yml` - Traefik IngressRoute

```bash
kubectl apply -f k8s/
kubectl get pods -n newpm
kubectl logs -f deployment/ruoyi-app -n newpm
```

## Troubleshooting

### Backend Won't Start

1. **Check MySQL connection**: Ensure MySQL is running on port 3306 with database `ry-vue`
   ```bash
   mysql -u root -p -e "SHOW DATABASES LIKE 'ry-vue';"
   ```
2. **Check Redis connection**: Ensure Redis is running on port 6379
   ```bash
   redis-cli ping  # Should return PONG
   ```
3. **Port conflict**: If port 8080 is in use, change `server.port` in `application.yml`
   ```bash
   lsof -i :8080  # Check what's using port 8080
   ```
4. **Database not initialized**: Run SQL scripts in `pm-sql/init/` in order (00, 01, 02)
   ```bash
   mysql -u root -p ry-vue < pm-sql/init/00_tables_ddl.sql
   mysql -u root -p ry-vue < pm-sql/init/01_tables_data.sql
   mysql -u root -p ry-vue < pm-sql/init/02_menu_data.sql
   ```
5. **Java version mismatch**: Ensure Java 17 is installed
   ```bash
   java -version  # Should show version 17.x.x
   ```

### Frontend Build Errors

1. **Node modules issue**: Delete `node_modules/` and `package-lock.json`, then `npm install`
2. **Port 80 requires sudo**: On macOS/Linux, use `sudo npm run dev` or change port in `vite.config.ts`
3. **TypeScript errors**: Run `npm run type-check` to see all type errors
4. **Vite cache issue**: Delete `node_modules/.vite/` and restart dev server

### Code Generation Issues

1. **CLI JAR not found**: Build it first with `mvn clean package -pl ruoyi-gen-cli -am -Dmaven.test.skip=true`
2. **DDL parsing error**: Ensure DDL syntax is valid MySQL 8.0 syntax
3. **Generated code not working**: Check if menu SQL was imported to database
4. **Collation mismatch**: Add `COLLATE utf8mb4_unicode_ci` when joining system tables
