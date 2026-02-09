# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RuoYi-Vue v3.9.1 — Enterprise admin system with separated frontend/backend.
- **Backend:** Java 17 / Spring Boot 3.5.8 / Spring Security / MyBatis / Redis / JWT
- **Frontend:** Vue 3.5 / TypeScript 5.6 / Vite 6.4 / Element Plus 2.13 / Pinia

## Build & Run Commands

### Backend (from project root)

```bash
mvn clean package -Dmaven.test.skip=true        # Build JAR
java -Xms256m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar  # Run
mvn clean                                        # Clean
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

```bash
npx playwright install                    # Install browsers (first time only)
npx playwright test                       # Run all E2E tests
npx playwright test --ui                  # Run tests in UI mode
npx playwright test --headed              # Run tests in headed mode
npx playwright show-report                # View test report
```

**Test Configuration**: `playwright.config.js` - Playwright E2E tests for project management features. Tests are in `tests/` directory.

### Prerequisites

- Java 17, Maven
- MySQL 8.x (database: `ry-vue`, init scripts in `pm-sql/init/`)
- Redis (localhost:6379)
- Node.js with npm

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

**Key Features:**

- **Project Management:** Full project lifecycle tracking including:
  - Project coding: `{industry}-{region}-{shortName}-{year}` format
  - Budget management: project budget, cost budget, labor cost, purchase cost
  - Workload tracking: estimated vs actual workload (in person-days)
  - Timeline: start/end/production/acceptance dates
  - Team assignment: project manager, market manager, sales manager, team leader, participants
  - Status tracking: project stage (字典: sys_xmjd), acceptance status (字典: sys_yszt)

- **Customer Management:** Customer database with:
  - Industry classification (字典: industry)
  - Region classification (字典: sys_yjqy)
  - Sales manager assignment
  - Multiple contact persons per customer

- **Approval Workflow:** Project approval process with:
  - Approval status: 0=pending, 1=approved, 2=rejected
  - Approval reason/comments
  - Approver tracking with timestamp

**Dictionary Dependencies:**

- `industry` - 行业分类
- `sys_yjqy` - 区域分类
- `sys_xmfl` - 项目分类
- `sys_xmjd` - 项目阶段
- `sys_yszt` - 验收状态

**Controllers:**

- `ProjectController` - `/project/project/**` - Project CRUD and approval operations
- `ProjectApprovalController` - `/project/approval/**` - Approval workflow management
- `CustomerController` - `/project/customer/**` - Customer management
- `CustomerContactController` - `/project/contact/**` - Contact management

**Frontend Routes:**

- `/project/project` - 项目列表 (Project list with approval actions)
- `/project/apply` - 项目立项申请 (Project initiation application)
- Customer and contact management pages

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

### Spring Boot 3 Specifics

- Security uses `SecurityFilterChain` bean + `@EnableMethodSecurity` (not WebSecurityConfigurerAdapter)
- Jakarta EE namespace (`jakarta.servlet.*` not `javax.servlet.*`)
- API docs via springdoc-openapi (`/v3/api-docs`, `/swagger-ui.html`)
- Druid starter: `druid-spring-boot-3-starter`
- MySQL driver: `mysql-connector-j`

## Frontend Patterns

### Tech Stack

Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router 4

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
- `00_tables_ddl.sql` - Table structure definitions
- `01_tables_data.sql` - Initial data and dictionary entries

When adding new business modules, place DDL files in `pm-sql/init/` for the `/ruoyi-gen` skill to discover.

## Code Generation Workflow

Use the `/ruoyi-gen` skill for interactive CRUD generation:

1. **Input modes**: DDL SQL, business description, or database query
2. **Smart inference**: Automatically infers field types, query types, and UI components from DDL
3. **Configuration**: Generates spec file in `docs/gen-specs/<table>.yml` for review
4. **Deployment**: Generates and deploys code to appropriate modules (backend + frontend)
5. **Customization**: Applies custom UI requirements from spec file

The skill handles menu generation, SQL file management, and database imports automatically.

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
