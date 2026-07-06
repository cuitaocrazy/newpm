# Research: 日报手机端 H5 适配（一期）

**Date**: 2026-07-04
**Feature**: 014-daily-report-mobile

## R1. 应用形态：同 SPA 单入口 vs Vite MPA 双入口

**Decision**: 同 SPA 单入口。移动页面作为现有应用的懒加载路由子树（`/m/**`），不新建 HTML 入口。

**Rationale**:
- 实测现状：`main.ts` 全量引入 Element Plus（`import ElementPlus from 'element-plus'` + 全量 CSS），入口 chunk 实测 557KB + 336KB (gzip)。更关键的是 `src/utils/request.ts` 直接 import `ElMessage/ElMessageBox/ElNotification/ElLoading` —— 任何复用 request 层的入口都会带上 Element Plus 及其样式。
- MPA 双入口若要"纯净"，必须复制/抽象 request.ts（错误提示改 Vant Toast）、独立 router/guard、并修改生产静态托管（Docker 构建时注入的 SpaController 需增加 `/m/**` → `mobile.html` 映射）——三处新增复杂度，违背一期 MVP 与"后端/部署零改动"目标。
- 同 SPA 方案下移动端首访虽需下载现有入口 chunk，但生产已启用「SPA 入口 no-store + 哈希资源 immutable」缓存策略（见 memory: deploy_cache_strategy），驻场人员为每日高频用户，仅首次与发版后首次较慢。
- 桌面端零影响：移动路由 `component: () => import(...)` 懒加载，桌面用户不会下载移动 chunk（FR-011 满足）。

**Alternatives considered**:
- **Vite MPA 双入口（mobile.html + 独立 main）**：移动首包最小（仅 Vant + 业务代码，预估 <150KB gz），但需复制 request 层、改 Docker 构建的 SpaController、双守卫维护。留作二期性能优化选项，若驻场反馈首访过慢再启动。
- **独立新前端工程**：彻底隔离但 CI/CD、登录、依赖升级全部双份维护，为单一日报功能不值。

**影响 spec 修订**: SC-004 从"不加载桌面端组件库、TTI ≤3s"修订为"首次访问 4G 下可交互 ≤5s；二次访问（哈希资源缓存命中）≤2s"。

## R2. 移动 UI 组件库：Vant 4 按需引入

**Decision**: 引入 Vant 4（Vue 3 版本，当前 4.9.x），组件在移动页面内显式 import，`vant/lib/index.css` 在移动布局组件（MobileLayout）中一次性引入，随移动懒加载 chunk 下发。

**Rationale**:
- 触控体验是本特性的存在理由：Vant 的 Picker（底部滚轮选择）、Stepper（步进器）、Field（大触控输入）、Popup、Toast 专为手机设计；Element Plus 的 select 下拉/slider 在小屏触控目标过小。
- Vant 是 ESM tree-shakable；显式 import 的组件只进入移动路由 chunk，桌面构建产物不变（FR-011）。
- 全量 CSS（约 20KB gz）随移动 chunk 懒加载，避免逐组件样式引入的遗漏问题（Toast/Dialog 等函数式组件样式易漏）。
- 项目已有 `unplugin-auto-import`（仅 API 自动导入），无 `unplugin-vue-components`；不为 Vant 增加 resolver 配置，显式 import 更直观且零全局配置改动。

**Alternatives considered**:
- **继续用 Element Plus + 移动 CSS**：零新增依赖（EP 反正已在入口 chunk），但触控交互质量差，等于没解决核心问题。
- **手写移动组件**：无依赖但工作量大、细节坑多（滚轮选择器、软键盘适配），不划算。

## R3. 路由与登录守卫集成

**Decision**: 移动路由全部注册为 constantRoutes（hidden）；`permission.ts` 做最小增量修改：
1. `whiteList` 增加 `'/m/login'`
2. 无 token 重定向目标按路径分流：`to.path` 以 `/m` 开头 → `/m/login?redirect=...`，否则维持 `/login?redirect=...`
3. 已有 token 访问 `/m/login` → 重定向 `/m`

**Rationale**:
- 复用现有 JWT/Cookie（`utils/auth.ts`）与 user store，登录态桌面/移动互通（同域同 cookie）。
- 有 token 首次进入 `/m/**` 时守卫仍会走 `getInfo()` + `generateRoutes()`（拉取动态路由）——对移动页无用但无害（多一次 `/getRouters` 请求），换来守卫逻辑零分叉，接受。
- `permission.ts` 是桌面共享文件，改动仅 3 行分支；现有桌面登录 E2E 可回归验证（SC-003）。

**Alternatives considered**:
- 移动独立 router 实例：需独立守卫、独立 app 挂载，回到 MPA 复杂度，否决。
- 移动路由走后端动态菜单：日报填写是全员功能，无需菜单权限控制页面可见性（接口层后端 `@PreAuthorize` 依旧生效），constantRoutes 更简单。

## R4. write.vue 业务逻辑复用策略

**Decision**: 一期移动填写页**自带逻辑副本**（约 200–300 行 script），仅复用：API 层（`src/api/project/dailyReport.js`、`src/api/project/task.js`）、`useDict`、日期工具。**不改动** `write.vue`，不做共享 composable 抽取。

**Rationale**:
- SC-003 要求桌面 E2E 全部保持通过；从 1043 行的 write.vue 中抽取 composable 属于重构，回归风险与验证成本高，与"移动端尽快上线"目标冲突。
- 移动端交互形态不同（卡片/折叠/Picker vs 双栏/滑块/下拉），可直接共享的其实主要是：payload 组装、本周可编辑判断、总工时汇总——量小，复制成本低。
- 保存 payload 结构以桌面 `handleSave()` 为准（details 数组：`projectId/projectStage/workHours/workContent/entryType/subProjectId/workCategory(逗号拼接)`），移动端严格对齐即保证 SC-002 双端一致。

**已知代价（记录在案）**: 双份逻辑存在漂移风险。二期做历史回看时若两端逻辑再次重叠，届时评估抽 `useDailyReportForm()` composable 并为桌面补 E2E 后统一。

## R5. 移动登录页与验证码

**Decision**: 新建 `/m/login` 移动登录页，逻辑镜像桌面 `login.vue`：`getCodeImg()` 获取 base64 验证码图（`captchaEnabled=false` 时自动隐藏输入项）、`userStore.login()` 写 token、支持"记住我"（沿用桌面的 Cookie + encrypt 方案）、登录成功跳 `redirect` 参数或 `/m`。

**Rationale**: 认证链路（含 math 型验证码）后端零改动即可用；镜像桌面逻辑（而非复用组件）是因为 UI 结构完全不同而逻辑只有约 60 行。

## R6. 视口与移动适配基础

**Decision**: 无需新增配置。`index.html` 已有 `<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">`；布局使用 px + flex（Vant 默认 px 体系），不引入 postcss-px-to-viewport/rem 方案。软键盘遮挡用 `scrollIntoView` 处理（Vant Field 自带基础行为）。

**Rationale**: 一期只有 2 个页面，vw/rem 全局缩放体系收益为零；YAGNI。

## R7. 测试策略

**Decision**:
- **E2E（Playwright）**：新增 `e2e-mobile-daily-report.spec.js`，文件内 `test.use({ ...devices['iPhone 13'] })` 做移动仿真（现有 config 不改，chromium 支持 isMobile/touch）。数据准备与鉴权沿用 `tests/helpers/api-client.js`（API 登录注入 token），跑登录页用例前按既有流程临时关验证码（memory: feedback_e2e_captcha_toggle）。
- **双端一致性**：核心用例 = 移动端保存 → API 断言 detail 数据 → 桌面 write.vue 打开同日回显一致。
- **桌面回归**：现有日报相关 E2E 套件全量重跑（SC-003）。
- 后端零改动，无需新增单元测试。

## R8. 字典组件规范的移动端处理（Constitution VI 例外）

**Decision**: 移动端不使用 `<dict-select>`/`<dict-tag>`（Element Plus 实现，触控不适用），改为 `useDict('sys_rbtype', 'sys_gzlb')` 取数 + Vant Picker/Tag 渲染。**禁止硬编码选项**的底线不变——所有选项数据仍来自字典接口。

**Rationale**: Constitution VI 的意图是"字典数据由后台维护、前端不硬编码"；移动端换渲染载体但数据源合规。已在 plan.md Complexity Tracking 中登记此例外。
