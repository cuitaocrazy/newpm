# Tasks: 日报手机端 H5 适配（一期）

**Input**: Design documents from `/specs/014-daily-report-mobile/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/reused-apis.md, quickstart.md

**Tests**: E2E 任务包含在内（spec SC-002 双端一致性 / SC-003 桌面回归 明确要求）；无后端改动故无单元测试任务。

**Organization**: 按 user story 分组；US1 为独立可交付 MVP，US2/US3 为同页面增量。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件、无未完成依赖）
- **[Story]**: US1=登录+填写保存 / US2=假期登记 / US3=本周补填与只读约束

## Path Conventions

前端增量特性，全部路径在 `ruoyi-ui/` 下（后端零改动，见 plan.md Technical Context）。

---

## Phase 1: Setup

**Purpose**: 依赖就绪

- [X] T001 安装移动 UI 依赖：`cd ruoyi-ui && npm i vant@^4.9`，确认 `ruoyi-ui/package.json` dependencies 新增 vant 且 `npm run dev` 正常启动

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 移动布局壳与登录守卫分流——所有 user story 的公共前置

**⚠️ CRITICAL**: 本阶段完成前不得开始任何 user story

- [X] T002 [P] 创建移动布局壳 `ruoyi-ui/src/views/m/layout/index.vue`：Vant NavBar（标题插槽）+ `<router-view/>` + 一次性引入 `vant/lib/index.css` + 移动基础样式（`env(safe-area-inset-bottom)` 底部安全区、禁用桌面全局样式干扰）
- [X] T003 [P] 修改 `ruoyi-ui/src/permission.ts`（最小增量，见 research R3）：① `whiteList` 增加 `'/m/login'`；② 无 token 分支按 `to.path.startsWith('/m')` 分流重定向至 `/m/login?redirect=...` 或原 `/login?redirect=...`；③ 有 token 访问 `/m/login` 重定向 `/m`。改完手动验证桌面 `/login` 流程不受影响

**Checkpoint**: 布局与守卫就绪，US1 可开始

---

## Phase 3: User Story 1 - 手机登录并填写保存当天日报 (Priority: P1) 🎯 MVP

**Goal**: 驻场人员手机浏览器登录 → 看到参与项目 → 填工时/内容/类别 → 保存成功且双端一致

**Independent Test**: 移动仿真（iPhone 13）访问 `/m/login` 登录 → 对一个普通项目和一个含子任务项目填写并保存 → 重新进入回显一致；`GET /project/dailyReport/my/{date}` 断言 payload 字段与 data-model.md §3 一致

### Implementation for User Story 1

- [X] T004 [P] [US1] 创建移动登录页 `ruoyi-ui/src/views/m/login/index.vue`：Vant Form/Field（用户名/密码）+ 验证码行（`getCodeImg()` base64 图、点击刷新、`captchaEnabled=false` 时隐藏）+ 记住我 Checkbox（沿用桌面 login.vue 的 Cookie+encrypt 方案）+ `useUserStore().login()` → 成功跳 `query.redirect || '/m'`（逻辑镜像 `src/views/login.vue`，UI 全 Vant）
- [X] T005 [US1] 创建移动填写页骨架与数据加载 `ruoyi-ui/src/views/m/dailyReport/write.vue`：进入页面**最先**调 `checkSelfInWhitelist()`（`src/api/project/whitelist.js`，异常按 false 兜底），白名单用户短路后续加载（对齐桌面 onMounted）；`useDict('sys_rbtype','sys_gzlb')`；`getMyProjects()` 构建 MobileProjectItem 列表（字段映射见 data-model.md §2.1）；`getMyReport(date)` 回显分发（按 `projectId + subProjectId + entryType` 回填，`workCategory` split(',') 转数组）；loading 态
- [X] T006 [US1] 实现项目卡片表单区（`write.vue` 内）：普通项目卡片 = Stepper（0–24 步 0.5）+ 工作内容 Field(textarea, 获焦 scrollIntoView) + 工作类别多选（Popup+Checkbox，数据源 sys_gzlb）；含子任务项目卡片 = Collapse 展开时经 `getTaskOptions(projectId)` 懒加载 TaskRow（data-model.md §2.2），每行同构表单；触控目标 ≥44px（SC-005）
- [X] T007 [US1] 实现保存逻辑（`write.vue` 内）：payload 组装**逐字段对齐** data-model.md §3（workCategory join(',')、普通项目需 workHours>0 且 workContent 非空白、subProjectId 语义）；校验 V1/V2/V3（Vant Toast 提示文案与桌面一致）；`saveDailyReport({reportDate, detailList})` → 成功 Toast → 重新 `getMyReport` 刷新 reportId；失败停留页面已填内容不丢（Edge Case）
- [X] T008 [US1] 实现汇总条 + 特殊状态（`write.vue` 内）：当日总工时实时汇总（≥8h 绿 / <8h 橙，FR-008）；白名单用户整页豁免提示不渲染表单（FR-009）；无项目空态提示；项目名关键字过滤框（FR-004）；底部固定保存栏（safe-area）
- [X] T009 [US1] 注册移动路由子树 `ruoyi-ui/src/router/index.ts`：constantRoutes 追加（全部懒加载 + hidden）——`/m/login` → `views/m/login/index.vue`；`/m` → MobileLayout，redirect `/m/daily-report/write`，children `daily-report/write` → `views/m/dailyReport/write.vue`（依赖 T002/T004/T005 文件存在）
- [X] T010 [US1] 新增移动 E2E `ruoyi-ui/e2e-mobile-daily-report.spec.js`：文件内 `test.use({ ...devices['iPhone 13'] })`；用例：① 登录页可登录（按 memory 流程临时关验证码）② 填写普通项目+含子任务项目并保存 → API 断言 detailList 与契约一致（contracts/reused-apis.md 锚点）③ 重进回显一致 ④ 清 token 访问 `/m/daily-report/write` 落 `/m/login?redirect=...`（FR-010）⑤ **桌面视角双端一致（SC-002/G2）**：移动保存后以桌面 viewport 打开 `/project/dailyReport/write` 断言同日工时/内容/类别 UI 回显一致（此用例不套 iPhone 仿真，用独立 `test.describe` + 默认桌面 viewport）；鉴权复用 `tests/helpers/api-client.js`

**Checkpoint**: US1 独立可用可测——此时即可交付 MVP 给驻场试用

---

## Phase 4: User Story 2 - 请假/倒休/年假登记 (Priority: P2)

**Goal**: 同一填写页添加/删除假期条目，与工时一并保存，双端一致

**Independent Test**: 移动端添加"年假 8 小时"保存 → API 断言 leaveDetail 结构（`projectId:null`、workHours/leaveHours 同值双写）→ 桌面 write.vue 同日回显一致

### Implementation for User Story 2

- [X] T011 [US2] 实现假期条目区 UI（`ruoyi-ui/src/views/m/dailyReport/write.vue` 内）：LeaveItem 列表（类型 Picker 数据源 = sys_rbtype 过滤 work + 小时 Stepper + 备注 Field + 删除按钮）+ "添加假期"按钮（FR-006）
- [X] T012 [US2] 假期参与保存与汇总（`write.vue` 内）：leaveDetails 组装对齐 data-model.md §3（V4 静默过滤 leaveHours≤0/未选类型）；总工时汇总纳入假期小时（US2-AS3）；回显时 `entryType != 'work'` 条目分发至假期区
- [X] T013 [US2] 扩展 `ruoyi-ui/e2e-mobile-daily-report.spec.js`：① 添加假期保存 → API 断言 leaveDetail 字段 ② 删除假期条目再保存 → 条目移除 ③ 工时+假期混合时总工时 = 两者之和

**Checkpoint**: US1+US2 均独立可用

---

## Phase 5: User Story 3 - 当周补填与编辑约束 (Priority: P3)

**Goal**: 本周 7 天可切换补填；越界日期只读，与桌面规则一致

**Independent Test**: 切到本周一可填写保存；URL 直达携带上周日期 → 表单只读 + 提示

### Implementation for User Story 3

- [X] T014 [US3] 实现本周日期切换条（`ruoyi-ui/src/views/m/dailyReport/write.vue` 内）：周一~周日 7 个 chip（当天高亮、有日报的日期加标记），点击切换 `selectedDate` 触发 `getMyReport` 重载回显（US3-AS3）
- [X] T015 [US3] 实现只读态（`write.vue` 内）：`isEditable = selectedDate ∈ [本周一, 本周日]`（对齐桌面 weekBounds 逻辑，防 URL query 直达越界日期）；只读时全部输入禁用 + 顶部"仅可填写本周日报"提示 + 保存栏禁用（FR-009）
- [X] T016 [US3] 扩展 `ruoyi-ui/e2e-mobile-daily-report.spec.js`：① 切换本周内昨天填写保存成功 ② 构造越界日期访问 → 只读断言 ③ 切换到已有日报日期 → 回显断言

**Checkpoint**: 全部 user story 完成

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 回归、性能验证与真机收尾（对应 SC-001/003/004/005）

- [X] T017 [P] 桌面日报 E2E 全量回归（SC-003）：`npx playwright test e2e-daily-report-*.spec.js` + 桌面登录流程用例（permission.ts 改动回归）；跑完按 memory 流程恢复验证码开关
- [X] T018 [P] 构建验证（FR-011）：`npm run build:prod` 后核对 ① 移动页面产出独立 chunk ② 入口 `index-*.js` 体积对比改动前无明显增长（vant 不得进入口 chunk），命令见 quickstart.md
- [ ] T019 真机联调清单执行（SC-001/SC-004/SC-005）：按 quickstart.md 清单过 iOS Safari / Android Chrome / 微信内置浏览器——软键盘遮挡、刘海屏安全区、验证码可读、弱网保存失败提示；**性能实测（SC-004）**：DevTools 4G 限速下测移动填写页首访可交互 ≤5s、二次访问（缓存命中）≤2s；发现问题回修 `write.vue`/`login/index.vue`
- [X] T020 [P] 文档收尾：`CLAUDE.md` Frontend Patterns 增补 `/m` 移动子树说明（路由位置、Vant 按需约定、守卫分流 3 行）；确认 `docs/gen-specs/` 无需变更（无表/无生成模块）

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 无依赖，立即可做
- **Phase 2 (Foundational)**: 依赖 T001；**阻塞所有 user story**
- **Phase 3 (US1)**: 依赖 Phase 2；US1 内 T009 依赖 T002/T004/T005（路由引用的文件须存在），T010 依赖 T004–T009
- **Phase 4 (US2)**: 依赖 US1 的 T005–T009（同页面增量），但验收独立
- **Phase 5 (US3)**: 依赖 US1 的 T005–T009；与 US2 无相互依赖（可先做 US3 后做 US2）
- **Phase 6 (Polish)**: 依赖全部所需 story 完成

### User Story Dependencies

- **US1 (P1)**: Foundational 后即可开始，无 story 依赖 —— MVP
- **US2 (P2)**: 页面宿主来自 US1，逻辑独立可测
- **US3 (P3)**: 页面宿主来自 US1，逻辑独立可测；与 US2 互不依赖

### Within Each User Story

- 数据加载（model）→ 表单 UI → 保存/校验 → 路由接入 → E2E
- 同文件任务（T005–T008、T011–T012、T014–T015）严格串行，不得并行

### Parallel Opportunities

- Phase 2: T002 ∥ T003（不同文件）
- US1: T004（登录页）∥ T005–T008（填写页，不同文件）
- US2 与 US3 整体可并行（不同开发者需注意同文件 write.vue 合并）
- Polish: T017 ∥ T018 ∥ T020

---

## Parallel Example: User Story 1

```bash
# 并行启动（不同文件）:
Task A: "T004 移动登录页 src/views/m/login/index.vue"
Task B: "T005→T006→T007→T008 移动填写页 src/views/m/dailyReport/write.vue（串行链）"
# 两者完成后:
Task: "T009 注册路由 → T010 E2E"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Phase 1 → Phase 2 → Phase 3（US1）
2. **STOP & VALIDATE**: 移动仿真 E2E 过 + 真机快测登录/填写/保存
3. 可直接发驻场小范围试用（假期/补填暂用桌面兜底）

### Incremental Delivery

1. US1 → 独立验证 → 试用（MVP）
2. US2（假期）→ 独立验证
3. US3（周补填/只读）→ 独立验证
4. Polish（回归 + 构建验证 + 真机清单）→ 按 memory「特性收尾 git 流程」合并 main 发版

---

## Notes

- 保存 payload 以 `data-model.md §3` 为唯一基准，任何字段偏差都会破坏 SC-002 双端一致性
- E2E 登录用例前临时关验证码、跑完恢复（memory: feedback_e2e_captcha_toggle）
- 提交范围克制：仅本特性文件（`src/views/m/**`、`router/index.ts`、`permission.ts`、`package.json`、E2E spec、specs/014 文档），勿裹挟工作区无关改动
