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

### Prerequisites

- Java 17, Maven
- MySQL 8.x (database: `ry-vue`, init scripts in `sql/`)
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
ruoyi-ui          → Vue 3 + TypeScript + Vite frontend (RuoYi-Vue3 typescript branch)
```

Dependencies flow: admin → framework → system → common. Quartz and generator depend on common.

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

## Ports

- Backend: 8080
- Frontend dev: 80
- Druid monitor: http://localhost:8080/druid (ruoyi/123456)
- Swagger UI: http://localhost:8080/swagger-ui.html
