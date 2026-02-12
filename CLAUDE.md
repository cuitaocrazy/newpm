# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Table of Contents

- [Project Overview](#project-overview)
- [Quick Start](#quick-start)
- [Build & Run Commands](#build--run-commands)
- [Module Architecture](#module-architecture)
- [Backend Patterns](#backend-patterns)
- [Frontend Patterns](#frontend-patterns)
- [Configuration Files](#configuration-files)
- [Database Schema](#database-schema)
- [Code Generation Workflow](#code-generation-workflow)
- [SQL Management](#sql-management)
- [Project Documentation](#project-documentation)
- [Development Workflow](#development-workflow)
- [Common Pitfalls](#common-pitfalls)
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
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ry_vue CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p ry_vue < pm-sql/init/00_tables_ddl.sql
mysql -u root -p ry_vue < pm-sql/init/01_tables_data.sql
mysql -u root -p ry_vue < pm-sql/init/02_menu_data.sql

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

### Code Generation CLI (ruoyi-gen-cli)

```bash
mvn clean package -Dmaven.test.skip=true        # Build CLI JAR
java -jar ruoyi-gen-cli/target/ruoyi-gen-cli.jar --sql=<ddl>.sql --config=<config>.yml --output=<output>.zip
```

The CLI generates CRUD scaffolding from DDL without requiring MySQL/Redis. Use the `/ruoyi-gen` skill for interactive code generation.

### E2E Testing (from project root)

**Test Strategy:** E2E testing infrastructure is configured with Playwright. Test coverage is planned for critical business workflows.

```bash
npx playwright install                    # Install browsers (first time only)
npx playwright test                       # Run all E2E tests
npx playwright test tests/project.spec.ts # Run specific test file
npx playwright test --ui                  # Run tests in UI mode
npx playwright test --headed              # Run tests in headed mode
npx playwright test --debug               # Run tests in debug mode
npx playwright show-report                # View test report
```

**Test Configuration:** `playwright.config.js` - Playwright E2E test configuration

**Planned Test Coverage:**
- Project creation and approval workflow (立项申请与审核)
- Customer and contact management (客户与联系人管理)
- Contract and payment tracking (合同与款项管理)
- Revenue recognition workflow (收入确认流程)

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
  - Database name: `ry_vue`
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
   - Track actual workload (person-days) per project
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

**Dictionary Dependencies:**

- `industry` - 行业分类
- `sys_yjqy` - 区域分类 (一级区域, 二级区域)
- `sys_xmfl` - 项目分类 (软件开发类, 硬件销售类, 系统集成类, 开发人力外包, 运维人力外包, 测试人力外包)
- `sys_xmjd` - 项目阶段 (需求分析, 设计阶段, 开发阶段, 部署阶段, 测试阶段, 运维阶段)
- `sys_yszt` - 验收状态
- `sys_xmzt` - 项目状态 (未启动, 已启动, 已测试, 已投产, 已结项, 已关闭)
- `sys_htlx` - 合同类型
- `sys_htzt` - 合同状态 (未签署, 已签署)
- `sys_fkzt` - 付款状态 (未开未付, 已提交验收材料, 验收材料已审核, 待通知开票, 已通知)
- `sys_wdlx` - 文档类型
- `sys_qrzt` - 确认状态 (未确认, 待确认, 已确认, 无法确认)

**Controllers:**

- `ProjectController` - `/project/project/**` - Project CRUD and approval operations
- `ProjectApprovalController` - `/project/approval/**` - Approval workflow management
- `ProjectReviewController` - `/project/review/**` - Revenue recognition and comprehensive project review
- `CustomerController` - `/project/customer/**` - Customer management
- `CustomerContactController` - `/project/contact/**` - Contact management
- `ContractController` - `/project/contract/**` - Contract CRUD with amount summary
- `PaymentController` - `/project/payment/**` - Payment management
- `AttachmentController` - `/project/attachment/**` - File upload/download/delete with audit logging
- `SecondaryRegionController` - `/project/secondaryRegion/**` - Secondary region management

**Frontend Routes:**

- `/project/project` - 项目列表 (Project list with approval actions)
- `/project/apply` - 项目立项申请 (Project initiation application)
- `/project/approval` - 项目审核 (Project approval workflow)
- `/project/review` - 公司收入确认 (Revenue recognition with comprehensive filtering)
- `/project/contract` - 合同列表 (Contract list with amount tracking)
- `/project/payment` - 款项列表 (Payment list with installment tracking)
- `/project/customer` - 客户管理 (Customer management)
- `/project/secondaryRegion` - 二级区域管理 (Secondary region management)

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

## Frontend Patterns

### Tech Stack

Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router 4

### Directory Structure (ruoyi-ui/)

```
src/
├── api/              → API functions (typed with interfaces from types/)
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
    └── ...
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

**Database name:** `ry_vue` (MySQL 8.x, charset: `utf8mb4`, collation: `utf8mb4_unicode_ci`)

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

**IMPORTANT**: When making changes to contract-related features, always check and confirm whether the configuration file needs to be updated:

- **File**: `docs/gen-specs/pm_contract.yml`
- **Purpose**: This file documents all customizations, business rules, and implementation details for the contract module
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

## Code Generation Workflow (ruoyi-gen-cli)

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

## Deployment

### Docker Deployment

```bash
# Build Docker image
docker build -t ruoyi-pm:latest .

# Run with docker-compose
docker-compose up -d
```

**Configuration files:**
- `Dockerfile` - Backend container configuration
- `docker-compose.yml` - Multi-container orchestration (backend + MySQL + Redis)

### Kubernetes Deployment

```bash
# Apply K8s configurations
kubectl apply -f k8s/

# Check deployment status
kubectl get pods -n ruoyi
```

**Configuration files in `k8s/`:**
- Deployment manifests for backend and frontend
- Service definitions
- ConfigMaps and Secrets
- Ingress rules

## Troubleshooting

### Backend Won't Start

1. **Check MySQL connection**: Ensure MySQL is running on port 3306 with database `ry_vue`
   ```bash
   mysql -u root -p -e "SHOW DATABASES LIKE 'ry_vue';"
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
   mysql -u root -p ry_vue < pm-sql/init/00_tables_ddl.sql
   mysql -u root -p ry_vue < pm-sql/init/01_tables_data.sql
   mysql -u root -p ry_vue < pm-sql/init/02_menu_data.sql
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
