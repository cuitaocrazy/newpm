# Implementation Plan: 日报手机端 H5 适配（一期）

**Branch**: `014-daily-report-mobile` | **Date**: 2026-07-04 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/014-daily-report-mobile/spec.md`

## Summary

驻场人员无电脑，需在手机浏览器完成日报填写。方案：在现有 `ruoyi-ui` SPA 中新增 `/m/**` 懒加载移动路由子树（constantRoutes），用 Vant 4 实现移动登录页与移动日报填写页（当天填写/保存 + 请假/倒休/年假登记 + 本周补填），复用现有 JWT 认证、request 层与 `/project/dailyReport/**` 接口，**后端与部署零改动，桌面端 write.vue 不动**。关键技术决策见 [research.md](./research.md)（R1 同 SPA 单入口、R2 Vant 4、R4 移动页自带逻辑副本）。

## Technical Context

**Language/Version**: TypeScript 5.6 / Vue 3.5（前端 only，后端零改动）
**Primary Dependencies**: Vant 4.9.x（新增，移动 UI）、Vue Router 4.6（既有）、Pinia 3（既有）、现有 request/auth/useDict 基础设施
**Storage**: N/A（复用既有接口，无 schema 变更；表 `pm_daily_report` / `pm_daily_report_detail` 读写不变）
**Testing**: Playwright E2E（新增移动仿真 spec，`devices['iPhone 13']`）+ 现有桌面日报 E2E 全量回归
**Target Platform**: iOS Safari、Android Chrome、微信内置浏览器（H5，无需安装）
**Project Type**: Web（现有前后端一体工程的前端增量）
**Performance Goals**: 移动填写页 4G 首访可交互 ≤5s、二次访问 ≤2s（SC-004）；桌面端构建产物加载不受影响（FR-011）
**Constraints**: 后端零改动（FR-012）；桌面 write.vue 零改动（SC-003）；permission.ts 仅 3 行分支增量
**Scale/Scope**: 2 个新页面（移动登录、移动填写）+ 1 个移动布局壳 + 路由/守卫增量；预估新增代码 ≤1200 行

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | 原则 | 结论 | 说明 |
|---|------|------|------|
| I | 业务完整性优先 | ✅ PASS | 无后端改动；保存走既有 `POST /project/dailyReport`（已带 `@Log`），审计链路不变 |
| II | 权限驱动访问控制 | ✅ PASS | 仅调用既有日报/任务/登录接口，后端 `@PreAuthorize` 继续生效；不调用任何 `system/` 直连接口 |
| III | API 与代码一致性 | ✅ PASS | 移动端统一 `import request from '@/utils/request'`；列表取 `res.rows`、单对象取 `res.data` |
| IV | 任务与项目解耦 | ✅ PASS | 纯前端特性，不触碰 pm_project/pm_task 字段 |
| V | 数据库规范 | ✅ PASS | 无 SQL、无 schema 变更 |
| VI | 前端组件与字典规范 | ⚠️ 例外已登记 | 移动端不用 `<dict-select>`（Element Plus 实现），改用 `useDict()` 取数 + Vant Picker 渲染；**无硬编码选项**，数据源合规。见 Complexity Tracking |

**Post-Phase-1 re-check**: 设计产物（data-model / contracts）未引入新违规；VI 例外维持原判定。GATE 通过。

## Project Structure

### Documentation (this feature)

```text
specs/014-daily-report-mobile/
├── plan.md              # 本文件
├── research.md          # Phase 0 输出（8 项决策 R1–R8）
├── data-model.md        # Phase 1 输出（视图模型 ↔ 既有实体映射）
├── quickstart.md        # Phase 1 输出（本地开发/真机联调指南）
├── contracts/
│   └── reused-apis.md   # Phase 1 输出（移动端消费的既有接口契约）
└── tasks.md             # Phase 2 输出（/speckit.tasks 生成，非本命令产物）
```

### Source Code (repository root)

```text
ruoyi-ui/
├── package.json                          # [改] 新增依赖 vant@^4.9
├── src/
│   ├── permission.ts                     # [改] whiteList += '/m/login'；无 token 时按 /m 前缀分流重定向
│   ├── router/index.ts                   # [改] constantRoutes 追加 /m 子树（全部懒加载、hidden）
│   ├── views/m/                          # [新] 移动端页面根目录
│   │   ├── layout/index.vue              # [新] MobileLayout：NavBar + router-view + 引入 vant/lib/index.css
│   │   ├── login/index.vue               # [新] 移动登录页（验证码/记住我，镜像 login.vue 逻辑）
│   │   └── dailyReport/write.vue         # [新] 移动日报填写页（核心，项目卡片 + 任务行 + 假期条目 + 周约束）
│   └── api/project/dailyReport.js        # [复用不改] myProjects / my/{date} / 保存 / task options
├── e2e-mobile-daily-report.spec.js       # [新] 移动仿真 E2E（iPhone 13 viewport）
└── vite.config.ts                        # [不改] 懒加载天然分 chunk，无需手动配置
```

**Structure Decision**: 移动端页面集中在 `src/views/m/` 独立子树，与桌面 `src/views/project/` 完全隔离；路由懒加载保证互不加载对方 chunk。共享层（request/auth/store/useDict/api）位于 `src/utils`、`src/api`、`src/store`，双端共用不复制。

## 页面设计要点（供 tasks 拆分参考）

### /m/login（移动登录页）
- Vant Form + Field（用户名/密码）+ 验证码行（Field + base64 图片，点击刷新）+ 记住我 Checkbox + 大按钮
- `captchaEnabled=false` 时隐藏验证码行；登录成功 `router.push(query.redirect || '/m')`

### /m/daily-report/write（移动填写页）
- **顶部**：NavBar（标题"今日日报" + 日期）+ 本周日期横向切换条（周一~周日 7 个可点 chip，非本周不提供入口 → 周约束天然满足；当天高亮）
- **汇总条**：当日总工时（工作+假期），≥8h 绿色 / <8h 橙色（对齐桌面 badge 语义）
- **项目区**：搜索框（项目名过滤）+ 项目卡片列表
  - 普通项目卡片：工时 Stepper（0–24，步进 0.5）+ 工作内容 Field(textarea)
  - 含子任务项目卡片：Collapse 展开任务行，每行 = 任务名 + Stepper + 内容 Field + 工作类别（Picker 弹层，必填校验与桌面一致：工时>0 时类别必选）
- **假期区**：条目列表（类型 Picker[sys_rbtype 非 work] + 小时 Stepper + 备注 Field + 删除）+ "添加假期"按钮
- **底部**：固定保存栏（SubmitBar 样式；白名单用户整页替换为豁免提示，只读日期显示只读提示）
- **保存 payload**：严格对齐桌面 `handleSave()` 组装结构（见 data-model.md），同接口同行为

## Complexity Tracking

> Constitution Check 例外的正式登记

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 移动端不使用 `<dict-select>`/`<dict-tag>`（Constitution VI 字面要求） | 二者基于 Element Plus，下拉触控目标小、无滚轮选择，移动可用性差（SC-005 要求 ≥44px 触控目标） | 直接复用 dict-select：触控体验不达标，违背本特性存在理由。替代实现仍经 `useDict()` 从字典接口取数，"禁止硬编码"底线不破 |
| `permission.ts`（桌面共享文件）3 行分支增量 | 未登录访问 `/m/**` 必须回移动登录页而非桌面登录页（FR-010） | 独立移动 router/守卫 = MPA 级复杂度（见 research R1/R3），为 3 行逻辑不值 |

## 风险与缓解

| 风险 | 等级 | 缓解 |
|------|------|------|
| 双份填写逻辑漂移（移动 vs write.vue） | 中 | payload 结构以 data-model.md 为唯一基准；E2E 含双端一致性用例（SC-002）；二期评估抽 composable |
| permission.ts 改动影响桌面登录 | 低 | 改动仅 else 分支 3 行；桌面登录 E2E 回归必跑 |
| 真机兼容坑（软键盘、iOS 100vh、微信内核） | 中 | quickstart.md 含真机联调步骤；固定底栏用 `env(safe-area-inset-bottom)`；预留 1–2 轮真机反馈 |
| 移动首访包体积（入口 chunk ~900KB gz） | 低 | 已由 SC-004 修订接受（R1）；发版缓存策略已在生产生效；二期可选 MPA 优化 |

## Phase 2 展望（非本命令产物）

`/speckit.tasks` 建议按 user story 切片：US1（登录+填写保存）→ US2（假期条目）→ US3（周切换/只读约束）→ E2E 与回归。US1 内部顺序：依赖安装 → 路由/守卫 → MobileLayout → 登录页 → 填写页（项目加载→表单→保存）。
